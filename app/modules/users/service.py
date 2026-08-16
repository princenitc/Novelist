"""User use cases."""
from .ports import UserRepositoryPort


class UserService:
    def __init__(self, repository: UserRepositoryPort):
        self.repository = repository

    def update_preferences(self, user_id: str, preferences: dict) -> dict:
        return self.repository.update_user(user_id, {"preferences": preferences})
