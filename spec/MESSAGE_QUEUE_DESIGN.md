# Message Queue Design Specification

## Table of Contents
- [Overview](#overview)
- [Current Implementation](#current-implementation)
- [Exchange and Routing Key Topology](#exchange-and-routing-key-topology)
- [Event Payload Schema](#event-payload-schema)
- [Event Types](#event-types)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Planned Enhancements](#planned-enhancements)

## Overview

The Novelist API publishes domain events to **RabbitMQ** using the [pika](https://pika.readthedocs.io/) Python client. Event publishing is **optional** — the API operates fully without RabbitMQ when `RABBITMQ_ENABLED=false`. The publisher is implemented in [`app/infrastructure/messaging/publisher.py`](../app/infrastructure/messaging/publisher.py).

## Current Implementation

The `EventPublisher` class:
- Opens a new blocking connection per publish call (simple, low-event-volume approach)
- Declares the exchange if it doesn't exist before publishing
- Serializes events as JSON with `content-type: application/json` and `delivery_mode: 2` (persistent)
- Catches all `AMQPError` exceptions, logs them, and does **not** re-raise — a messaging failure must never fail a successful database write

```python
class EventPublisher:
    exchange = "novelist.domain.exchange"   # topic exchange, durable

    def publish(self, routing_key: str, payload: dict) -> None:
        if not self.settings.rabbitmq_enabled:
            return
        body = json.dumps({
            "eventId":   str(uuid4()),
            "timestamp": int(datetime.now().timestamp() * 1000),
            **payload
        })
        # ... open connection, declare exchange, basic_publish, close
```

## Exchange and Routing Key Topology

| Exchange | Type | Durable |
|----------|------|---------|
| `novelist.domain.exchange` | topic | ✅ |

| Routing Key | Published by |
|-------------|-------------|
| `book.created` | `BooksService.create_book` |
| `book.updated` | `BooksService.update_book` |
| `book.deleted` | `BooksService.delete_book` |
| `user.created` | `UsersService.create_user` |
| `rating.added` | `RatingsService.add_rating` |

Consumers can bind queues with `book.*` or `#` patterns as needed.

## Event Payload Schema

All events share these envelope fields:

```json
{
  "eventId":   "uuid-v4",
  "timestamp": 1720000000000,
  "<domain-specific fields>": "..."
}
```

`eventId` and `timestamp` are added automatically by the publisher. The rest of the payload is passed by the service.

## Event Types

### book.created

```json
{
  "eventId":   "...",
  "timestamp": 1720000000000,
  "bookId":    "...",
  "title":     "The Great Gatsby",
  "author":    "F. Scott Fitzgerald"
}
```

### book.updated

```json
{
  "eventId":   "...",
  "timestamp": 1720000000000,
  "bookId":    "...",
  "changes":   { "title": "New Title" }
}
```

### book.deleted

```json
{
  "eventId":   "...",
  "timestamp": 1720000000000,
  "bookId":    "..."
}
```

### user.created

```json
{
  "eventId":   "...",
  "timestamp": 1720000000000,
  "userId":    "...",
  "name":      "Alice"
}
```

### rating.added

```json
{
  "eventId":   "...",
  "timestamp": 1720000000000,
  "userId":    "...",
  "bookId":    "...",
  "rating":    5
}
```

## Configuration

Set via environment variables or `.env`:

| Variable | Default | Description |
|----------|---------|-------------|
| `RABBITMQ_ENABLED` | `true` | Set to `false` to disable all publishing |
| `RABBITMQ_HOST` | `localhost` | Broker hostname |
| `RABBITMQ_PORT` | `5672` | AMQP port |
| `RABBITMQ_USER` | `novelist` | Credentials |
| `RABBITMQ_PASSWORD` | `password` | Credentials |

In `docker-compose.yml` the broker is `novelist-rabbitmq` with user `novelist` / `password`, management UI at port `15672`.

## Error Handling

The publisher uses a fire-and-forget pattern:

```python
try:
    # connect → declare → publish → close
except pika.exceptions.AMQPError:
    logger.exception("Could not publish event with routing key %s", routing_key)
    # intentionally not re-raised
```

This guarantees that a RabbitMQ outage does not turn a successful Neo4j write into a failed API response. Events may be lost during an outage; this is acceptable at current scale.

**Future improvement**: use an outbox pattern (store pending events in Neo4j, publish via a background worker) for at-least-once delivery guarantees.

## Planned Enhancements

When RAG integration is implemented, new routing keys will be added:

| Routing Key | Purpose |
|-------------|---------|
| `rag.index.requested` | Trigger embedding of a book's content |
| `rag.index.completed` | RAG service signals embedding is done |
| `rag.search.requested` | Async semantic search request |
| `rag.search.completed` | RAG service returns search results |

---

**Last Updated**: 2026-08-16  
**Status**: Current
