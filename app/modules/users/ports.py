"""Outbound dependencies required by user use cases."""
from typing import Any, Protocol


class UserRepositoryPort(Protocol):
    def update_user(self, user_id: str, payload: dict[str, Any]) -> dict[str, Any]: ...
