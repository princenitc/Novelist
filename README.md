# Novelist

Novelist is a Python REST API for managing books, users, ratings, recommendations, and reading analytics. It uses FastAPI, Neo4j, optional RabbitMQ domain events, and a local RAG (Retrieval-Augmented Generation) semantic search pipeline.

## Project structure

The application uses feature modules with explicit application and infrastructure layers.
Each feature owns its API contract, router, service, and persistence concern; routers do not contain business logic.
Services depend on feature-owned `Protocol` ports, while the dependency container supplies the concrete Neo4j and RabbitMQ adapters.

```
app/
├── main.py                         # Thin application bootstrap and router registration
├── core/                           # Configuration, dependencies, security, rate limiting, logging
├── infrastructure/                 # Concrete external-system adapters
│   ├── neo4j/                      # Driver lifecycle and composed repository
│   └── messaging/                  # RabbitMQ publisher
└── modules/                        # Feature modules
    ├── auth/                       # register, login, refresh, logout
    ├── books/                      # CRUD + paginated search with filters
    ├── users/                      # CRUD + preferences + search
    ├── ratings/                    # Per-user book ratings
    ├── analytics/                  # Stats, trending, genre breakdown
    ├── recommendations/            # Graph-based recommendations
    ├── profile/                    # Current-user profile (/me)
    ├── rag/                        # Semantic search: index + vector query
    └── shared/                     # Cross-feature API contracts
```

## Requirements

- Python 3.13+
- Neo4j 5+
- RabbitMQ (optional — disable with `RABBITMQ_ENABLED=false`)

## Run locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8081
```

Interactive API docs: `http://localhost:8081/docs`

## Test

```bash
pytest
```

58 tests across `tests/test_api_integration.py`, `tests/test_api_coverage.py`, and `tests/unit/test_services.py`.

## Run with containers

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Novelist API | `http://localhost:8081` |
| Neo4j Browser | `http://localhost:7474` |
| RabbitMQ management | `http://localhost:15672` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

## Endpoints

| Resource | Endpoints |
|---|---|
| **Auth** | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| **Books** | `POST /api/v1/books`, `GET /api/v1/books`, `GET /api/v1/books/{bookId}`, `PUT /api/v1/books/{bookId}`, `DELETE /api/v1/books/{bookId}`, `GET /api/v1/books/search` |
| **Users** | `POST /api/v1/users`, `GET /api/v1/users`, `GET /api/v1/users/{userId}`, `PUT /api/v1/users/{userId}`, `DELETE /api/v1/users/{userId}`, `GET /api/v1/users/search`, `PUT /api/v1/users/{userId}/preferences` |
| **Ratings** | `POST /api/v1/users/{userId}/ratings/{bookId}` |
| **Profile** | `GET /api/v1/me` |
| **Analytics** | `GET /api/v1/analytics/books/{bookId}/stats`, `GET /api/v1/analytics/books/trending`, `GET /api/v1/analytics/genres` |
| **Recommendations** | `GET /api/v1/recommendations/users/{userId}` |
| **RAG** | `POST /api/v1/rag/index`, `POST /api/v1/rag/search` |
| **Actuator** | `GET /actuator/health`, `GET /actuator/prometheus` |

### Book search filters

`GET /api/v1/books/search` accepts:

| Param | Type | Description |
|---|---|---|
| `query` | string | Full-text match on title or author |
| `genre` | string | Exact genre match |
| `year` | int | Published year |
| `sortBy` | `title` \| `rating` \| `createdAt` | Sort field (default `title`) |
| `sortOrder` | `asc` \| `desc` | Sort direction (default `asc`) |
| `minRating` | float | Minimum average rating |
| `maxPageCount` | int | Maximum page count |

## Configuration

Copy `.env.example` to `.env`. Key variables:

| Variable | Default | Description |
|---|---|---|
| `NEO4J_URI` | `bolt://localhost:7687` | Neo4j connection |
| `RABBITMQ_ENABLED` | `true` | Set `false` to disable event publishing |
| `JWT_SECRET_KEY` | — | **Change in production** |
| `JWT_EXPIRY_MINUTES` | `60` | Access token lifetime |
| `JWT_REFRESH_EXPIRY_DAYS` | `7` | Refresh token lifetime |
| `LOG_LEVEL` | `INFO` | `DEBUG` (pretty) or `INFO`/`WARNING`/`ERROR` (JSON) |
| `CORS_ORIGINS` | `*` | Comma-separated allowed origins |
| `RAG_EMBEDDING_MODEL` | `all-MiniLM-L6-v2` | Local sentence-transformers model |
| `RAG_TOP_K` | `5` | Default number of RAG search results |

See `.env.example` for the full list and `spec/` for architecture and deployment documentation.
