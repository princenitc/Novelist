# Novelist

Novelist is a Python REST API for managing books, users, ratings, recommendations, and reading analytics. It uses FastAPI, Neo4j, and optional RabbitMQ domain events.

## Requirements

- Python 3.13+
- Neo4j 5+
- RabbitMQ (optional; disable with `RABBITMQ_ENABLED=false`)

## Run locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8081
```

The interactive API documentation is at `http://localhost:8081/docs`.

## Run with containers

```bash
docker compose up --build
```

This starts the API at `http://localhost:8081`, Neo4j at `http://localhost:7474`, RabbitMQ management at `http://localhost:15672`, Prometheus at `http://localhost:9090`, and Grafana at `http://localhost:3000`.

## Main endpoints

| Resource | Endpoints |
| --- | --- |
| Books | `POST /api/v1/books`, `GET /api/v1/books`, `GET /api/v1/books/{bookId}`, `PUT`/`DELETE /api/v1/books/{bookId}`, `GET /api/v1/books/search` |
| Users | `POST /api/v1/users`, `GET /api/v1/users`, `GET /api/v1/users/{userId}`, `PUT`/`DELETE /api/v1/users/{userId}`, `GET /api/v1/users/search` |
| Ratings | `POST /api/v1/users/{userId}/ratings/{bookId}` |
| Analytics | `GET /api/v1/analytics/books/{bookId}/stats`, `GET /api/v1/analytics/books/trending`, `GET /api/v1/analytics/genres` |
| Recommendations | `GET /api/v1/recommendations/users/{userId}` |

Health and Prometheus metrics are available at `/actuator/health` and `/actuator/prometheus`.

## Configuration

Copy `.env.example` to `.env` and adjust the Neo4j and RabbitMQ connection values as needed. The application reads the same values as environment variables, which is how `docker-compose.yml` configures it.
