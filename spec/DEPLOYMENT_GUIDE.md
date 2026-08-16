# Deployment Guide

## Table of Contents
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Local Development (without containers)](#local-development-without-containers)
- [Docker Compose (recommended)](#docker-compose-recommended)
- [Environment Variables](#environment-variables)
- [Health and Readiness Checks](#health-and-readiness-checks)
- [Monitoring](#monitoring)
- [Neo4j Backup](#neo4j-backup)
- [Troubleshooting](#troubleshooting)

## Overview

The Novelist API is a Python 3.13 / FastAPI application. The canonical way to run the full stack is `docker compose up --build`. For local development without containers, you need Python 3.13+, a reachable Neo4j 5 instance, and optionally RabbitMQ.

### Services Started by Docker Compose

| Service | Image | Port(s) |
|---------|-------|---------|
| `novelist-app` | Built from `Dockerfile` | 8081 |
| `novelist-neo4j` | neo4j:5.15.0 | 7474 (HTTP), 7687 (Bolt) |
| `novelist-rabbitmq` | rabbitmq:3.12-management | 5672 (AMQP), 15672 (UI) |
| `novelist-prometheus` | prom/prometheus:v2.45.0 | 9090 |
| `novelist-grafana` | grafana/grafana:10.0.3 | 3000 |

## Prerequisites

- **Docker** ≥24 with **Docker Compose** plugin
- **Python 3.13+** (for local dev only)
- **git**

## Local Development (without containers)

```bash
# 1. Clone and enter the repo
git clone <repo-url>
cd Novelist

# 2. Create and activate a virtual environment
python3.13 -m venv .venv
source .venv/bin/activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Configure environment
cp .env.example .env
# Edit .env with your Neo4j and RabbitMQ credentials

# 5. Start the API
uvicorn app.main:app --reload --port 8081
```

The API will be available at `http://localhost:8081`. Interactive docs at `http://localhost:8081/docs`.

> Set `RABBITMQ_ENABLED=false` in `.env` to skip RabbitMQ if you don't have a broker running locally.

## Docker Compose (recommended)

```bash
# Build and start all services (detached)
docker compose up --build -d

# View logs
docker compose logs -f novelist-app

# Stop all services
docker compose down

# Stop and remove volumes (full reset)
docker compose down -v
```

The app waits for Neo4j and RabbitMQ health checks to pass before starting.

### Service URLs

| Service | URL |
|---------|-----|
| API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/docs |
| Neo4j Browser | http://localhost:7474 |
| RabbitMQ Management | http://localhost:15672 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/password) |

### Scaling the app

```bash
docker compose up --scale novelist-app=2
```

## Environment Variables

All variables are read by `app/core/config.py` via `pydantic-settings`. Copy `.env.example` to `.env` for local dev.

| Variable | Default | Description |
|----------|---------|-------------|
| `NEO4J_URI` | `bolt://localhost:7687` | Neo4j Bolt URI |
| `NEO4J_USER` | `neo4j` | Neo4j username |
| `NEO4J_PASSWORD` | `password` | Neo4j password |
| `RABBITMQ_ENABLED` | `true` | Set `false` to disable event publishing |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USER` | `novelist` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `password` | RabbitMQ password |
| `JWT_SECRET_KEY` | `change-this-secret` | **Change in production** — signs JWT tokens |
| `JWT_ALGORITHM` | `HS256` | JWT signing algorithm |
| `JWT_EXPIRY_MINUTES` | `60` | Access token lifetime |

> **Production checklist**: rotate `JWT_SECRET_KEY`, use strong Neo4j/RabbitMQ credentials, restrict network access to internal services.

## Health and Readiness Checks

```bash
# API liveness / Neo4j connectivity
curl http://localhost:8081/actuator/health
# → {"status": "UP"}  or 503 if Neo4j is unreachable

# Prometheus metrics scrape endpoint
curl http://localhost:8081/actuator/prometheus
```

Docker Compose health checks:
- **Neo4j**: HTTP GET to port 7474 every 10 s, 5 retries, 30 s start period
- **RabbitMQ**: `rabbitmq-diagnostics -q ping` every 10 s, 5 retries, 30 s start period
- **novelist-app**: depends on both above passing before starting

## Monitoring

### Prometheus

`prometheus.yml` should be present in the repo root and mounted at `/etc/prometheus/prometheus.yml`. Minimal scrape config:

```yaml
scrape_configs:
  - job_name: novelist
    static_configs:
      - targets: ['novelist-app:8081']
    metrics_path: /actuator/prometheus
    scrape_interval: 15s
```

### Grafana

- Default admin credentials: `admin` / `password` (set via `GF_SECURITY_ADMIN_*` env vars in compose)
- Add a Prometheus data source pointing to `http://novelist-prometheus:9090`
- Import a FastAPI dashboard (e.g. Grafana dashboard ID 14282) for HTTP latency and throughput

## Neo4j Backup

```bash
# One-off backup (while container is running)
docker exec novelist-neo4j neo4j-admin database dump neo4j --to-path=/tmp/backup

# Copy backup out of container
docker cp novelist-neo4j:/tmp/backup/neo4j.dump ./neo4j-backup-$(date +%F).dump

# Restore
docker exec novelist-neo4j neo4j-admin database load neo4j \
  --from-path=/tmp/backup --overwrite-destination=true
```

## Troubleshooting

### App can't connect to Neo4j

```bash
# Check Neo4j is healthy
docker compose ps novelist-neo4j
docker compose logs novelist-neo4j | tail -30

# Test connectivity from within the network
docker compose run --rm novelist-app python -c \
  "from neo4j import GraphDatabase; GraphDatabase.driver('bolt://neo4j:7687', auth=('neo4j','password')).verify_connectivity(); print('OK')"
```

### RabbitMQ publish failures

The publisher logs errors but does not surface them to callers. Look for log lines like:
```
ERROR  Could not publish event with routing key book.created
```

Set `RABBITMQ_ENABLED=false` to silence these if a broker isn't available.

### Port conflicts

If port 8081, 7474, 7687, 5672, or 15672 is already in use:
```bash
# Find the process
lsof -i :8081
# Or change the host port in docker-compose.yml
```

### Rebuild after dependency changes

```bash
docker compose build --no-cache novelist-app
docker compose up -d novelist-app
```

---

**Last Updated**: 2026-08-16  
**Status**: Current
