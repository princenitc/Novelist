"""Recommendation query use cases."""
from .ports import RecommendationRepositoryPort


class RecommendationService:
    def __init__(self, repository: RecommendationRepositoryPort):
        self.repository = repository

    def for_user(self, user_id: str, limit: int) -> list[dict]:
        return self.repository.recommendations(user_id, limit)
