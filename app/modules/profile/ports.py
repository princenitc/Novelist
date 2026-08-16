"""Outbound dependencies required by profile use cases."""
from typing import Any, Protocol


class ProfileRepositoryPort(Protocol):
    def get_user(self, user_id: str) -> dict[str, Any]: ...
