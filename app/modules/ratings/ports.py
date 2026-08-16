"""Outbound dependencies required by rating use cases."""
from typing import Any, Protocol


class RatingRepositoryPort(Protocol):
    def add_rating(self, user_id: str, book_id: str, rating: int, review: str | None) -> dict[str, Any]: ...
