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
    """Publishes the same topic routing keys as the Spring application."""

    exchange = "novelist.domain.exchange"

    def __init__(self, settings: Settings):
        self.settings = settings

    def publish(self, routing_key: str, payload: dict) -> None:
        if not self.settings.rabbitmq_enabled:
            return
        body = json.dumps({"eventId": str(uuid4()), "timestamp": int(datetime.now().timestamp() * 1000), **payload}, default=_json_default)
        try:
            credentials = pika.PlainCredentials(self.settings.rabbitmq_user, self.settings.rabbitmq_password)
            connection = pika.BlockingConnection(pika.ConnectionParameters(
                host=self.settings.rabbitmq_host, port=self.settings.rabbitmq_port, credentials=credentials
            ))
            channel = connection.channel()
            channel.exchange_declare(exchange=self.exchange, exchange_type="topic", durable=True)
            channel.basic_publish(exchange=self.exchange, routing_key=routing_key, body=body,
                                  properties=pika.BasicProperties(content_type="application/json", delivery_mode=2))
            connection.close()
        except pika.exceptions.AMQPError:
            # Event delivery must not turn a successful database write into a failed API call.
            logger.exception("Could not publish event", routing_key=routing_key)
