"""Outbound dependencies required by book use cases."""
from typing import Any, Protocol


class BookRepositoryPort(Protocol):
    def create_book(self, payload: dict[str, Any]) -> dict[str, Any]: ...

    def update_book(self, book_id: str, payload: dict[str, Any]) -> dict[str, Any]: ...

    def delete_book(self, book_id: str) -> dict[str, Any]: ...
