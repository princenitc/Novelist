"""Cross-feature outbound port contracts."""
from typing import Any, Protocol


class EventPublisherPort(Protocol):
    """Publishes a domain event without exposing a broker implementation."""

    def publish(self, routing_key: str, payload: dict[str, Any]) -> None: ...
