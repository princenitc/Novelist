"""Outbound dependencies required by analytics use cases."""
from typing import Any, Protocol


class AnalyticsRepositoryPort(Protocol):
    def book_stats(self, book_id: str) -> dict[str, Any]: ...

    def trending(self, limit: int) -> list[dict[str, Any]]: ...

    def genres(self) -> list[dict[str, Any]]: ...
