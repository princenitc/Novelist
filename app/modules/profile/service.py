"""Current-user profile use cases."""
from .ports import ProfileRepositoryPort


class ProfileService:
    def __init__(self, repository: ProfileRepositoryPort):
        self.repository = repository

    def get_current_profile(self, user_id: str) -> dict:
        return self.repository.get_user(user_id)
