"""Outbound dependencies required by recommendation use cases."""
from typing import Any, Protocol


class RecommendationRepositoryPort(Protocol):
    def recommendations(self, user_id: str, limit: int) -> list[dict[str, Any]]: ...
