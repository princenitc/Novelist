import json
from datetime import date, datetime
from uuid import uuid4

import pika
import structlog

from app.core.config import Settings

logger = structlog.get_logger(__name__)


def _json_default(value: object) -> str:
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    raise TypeError(f"Cannot serialize {type(value).__name__}")


class EventPublisher:
    """Publishes domain events over a persistent RabbitMQ channel.

    A single connection + channel is created on first use and reused across
    all subsequent publishes.  If the broker closes the connection (network
    blip, restart, etc.) the next ``publish`` call transparently reconnects
    before retrying once, so transient outages do not surface as errors to
    the caller.
    """

    exchange = "novelist.domain.exchange"

    def __init__(self, settings: Settings):
        self.settings = settings
        self._connection: pika.BlockingConnection | None = None
        self._channel: pika.adapters.blocking_connection.BlockingChannel | None = None

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _connect(self) -> None:
        credentials = pika.PlainCredentials(
            self.settings.rabbitmq_user, self.settings.rabbitmq_password
        )
        self._connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=self.settings.rabbitmq_host,
                port=self.settings.rabbitmq_port,
                credentials=credentials,
            )
        )
        self._channel = self._connection.channel()
        self._channel.exchange_declare(
            exchange=self.exchange, exchange_type="topic", durable=True
        )

    def _is_open(self) -> bool:
        return (
            self._connection is not None
            and self._connection.is_open
            and self._channel is not None
            and self._channel.is_open
        )

    def _ensure_connected(self) -> None:
        if not self._is_open():
            self._connect()

    def close(self) -> None:
        """Gracefully close the connection (called during app shutdown)."""
        try:
            if self._connection and self._connection.is_open:
                self._connection.close()
        except Exception:
            pass
        finally:
            self._connection = None
            self._channel = None

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def publish(self, routing_key: str, payload: dict) -> None:
        if not self.settings.rabbitmq_enabled:
            return

        body = json.dumps(
            {
                "eventId": str(uuid4()),
                "timestamp": int(datetime.now().timestamp() * 1000),
                **payload,
            },
            default=_json_default,
        )

        try:
            self._ensure_connected()
            self._channel.basic_publish(  # type: ignore[union-attr]
                exchange=self.exchange,
                routing_key=routing_key,
                body=body,
                properties=pika.BasicProperties(
                    content_type="application/json", delivery_mode=2
                ),
            )
        except pika.exceptions.AMQPError:
            # Connection may have dropped — attempt one reconnect then retry.
            logger.warning("RabbitMQ connection lost; reconnecting", routing_key=routing_key)
            self.close()
            try:
                self._connect()
                self._channel.basic_publish(  # type: ignore[union-attr]
                    exchange=self.exchange,
                    routing_key=routing_key,
                    body=body,
                    properties=pika.BasicProperties(
                        content_type="application/json", delivery_mode=2
                    ),
                )
            except pika.exceptions.AMQPError:
                # Event delivery must not turn a successful database write into a failed API call.
                logger.exception("Could not publish event", routing_key=routing_key)
