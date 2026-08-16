# Observability Design

## Table of Contents
- [Overview](#overview)
- [Metrics (Prometheus)](#metrics-prometheus)
- [Logging](#logging)
- [Health Checks](#health-checks)
- [Grafana Dashboards](#grafana-dashboards)
- [Alerting](#alerting)
- [Planned Improvements](#planned-improvements)

## Overview

The Novelist API exposes observability data through three channels:

| Signal | Technology | Endpoint / Location |
|--------|-----------|---------------------|
| Metrics | Prometheus (via `prometheus-fastapi-instrumentator`) | `GET /actuator/prometheus` |
| Health | FastAPI custom endpoint | `GET /actuator/health` |
| Logs | Python `logging` to stdout | Docker log driver / `docker compose logs` |

`docker-compose.yml` starts a Prometheus scraper (`novelist-prometheus:9090`) and a Grafana instance (`novelist-grafana:3000`) alongside the API.

## Metrics (Prometheus)

### Instrumentation

[`prometheus-fastapi-instrumentator`](https://github.com/trallnag/prometheus-fastapi-instrumentator) is registered in `app/main.py`:

```python
Instrumentator().instrument(app).expose(app, endpoint="/actuator/prometheus", include_in_schema=False)
```

This automatically instruments all routes and exposes the following metric families:

| Metric | Type | Description |
|--------|------|-------------|
| `http_requests_total` | Counter | Total requests by method, handler, status |
| `http_request_duration_seconds` | Histogram | Request latency by handler |
| `http_request_size_bytes` | Histogram | Request body size |
| `http_response_size_bytes` | Histogram | Response body size |
| `http_requests_in_progress` | Gauge | Concurrent requests in flight |

### Prometheus Scrape Config

`prometheus.yml` (mounted into the `novelist-prometheus` container):

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: novelist-api
    static_configs:
      - targets: ['novelist-app:8081']
    metrics_path: /actuator/prometheus
```

### Useful PromQL Queries

```promql
# Request rate (req/s) across all routes
rate(http_requests_total[1m])

# 99th percentile latency per route
histogram_quantile(0.99,
  sum(rate(http_request_duration_seconds_bucket[5m])) by (handler, le)
)

# Error rate (4xx + 5xx) per route
sum(rate(http_requests_total{status=~"[45].."}[5m])) by (handler)
  /
sum(rate(http_requests_total[5m])) by (handler)

# Requests in progress
http_requests_in_progress
```

## Logging

### Current Setup

`app/main.py` configures root logging at `INFO` level:

```python
logging.basicConfig(level=logging.INFO)
```

All application modules use `logging.getLogger(__name__)`. The `EventPublisher` logs AMQP errors at `ERROR` level. Uvicorn access logs are written to stdout.

### Log Format (current — unstructured)

```
INFO:     uvicorn.access - 127.0.0.1:52410 - "POST /api/v1/books HTTP/1.1" 201
ERROR:    app.infrastructure.messaging.publisher - Could not publish event with routing key book.created
```

### Viewing Logs

```bash
# Docker Compose
docker compose logs -f novelist-app

# Filter errors only
docker compose logs novelist-app | grep ERROR
```

### Planned: Structured Logging

Replace `basicConfig` with `structlog` or `python-json-logger` to emit JSON log lines with:
- `timestamp`
- `level`
- `logger`
- `message`
- `request_id` (correlation ID from a middleware)

This makes logs directly queryable in any log aggregation stack (e.g. Loki + Grafana, ELK).

## Health Checks

### `/actuator/health`

```python
@app.get("/actuator/health", include_in_schema=False)
def health(repo: Repo) -> dict[str, str]:
    try:
        repo.verify()          # calls driver.verify_connectivity()
    except Exception as exc:
        raise error(503, "Neo4j is unavailable") from exc
    return {"status": "UP"}
```

| Response | Meaning |
|----------|---------|
| `200 {"status": "UP"}` | API running, Neo4j reachable |
| `503 {"status": 503, "message": "Neo4j is unavailable"}` | Neo4j unreachable |

RabbitMQ availability is not checked here — it is optional infrastructure.

## Grafana Dashboards

Grafana is available at `http://localhost:3000` (admin/password when running via Docker Compose).

### Setup

1. Add a Prometheus data source: `http://novelist-prometheus:9090`
2. Import a FastAPI community dashboard (e.g. [ID 14282](https://grafana.com/grafana/dashboards/14282)) for out-of-the-box HTTP metrics
3. Neo4j plugin: optionally install the [Neo4j Grafana plugin](https://grafana.com/grafana/plugins/neo4j-datasource/) to query graph data directly

### Recommended Panels

| Panel | Metric / Query |
|-------|---------------|
| Request rate | `rate(http_requests_total[1m])` |
| P99 latency | `histogram_quantile(0.99, ...)` |
| Error rate | `(4xx + 5xx) / total` |
| Active requests | `http_requests_in_progress` |
| Top slow routes | P99 latency by `handler` |

## Alerting

Prometheus alerting rules (`prometheus.yml` → `rule_files`):

```yaml
groups:
  - name: novelist
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_requests_total{status=~"5.."}[5m])) /
          sum(rate(http_requests_total[5m])) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "5xx error rate above 5% for 2 minutes"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.99,
            sum(rate(http_request_duration_seconds_bucket[5m])) by (le)
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P99 latency above 2 s"

      - alert: APIDown
        expr: up{job="novelist-api"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Novelist API is down"
```

## Planned Improvements

| Item | Priority | Notes |
|------|----------|-------|
| Structured JSON logging | High | Use `structlog`; add request-ID middleware |
| Request correlation ID | High | Middleware injects `X-Request-ID` into logs |
| Log to file | Medium | Rotate with `logging.handlers.RotatingFileHandler` or forward to Loki |
| Distributed tracing | Low | OpenTelemetry → Jaeger or Grafana Tempo |
| Custom business metrics | Low | e.g. `novels_rated_total`, `registrations_total` |

---

**Last Updated**: 2026-08-16  
**Status**: Current
