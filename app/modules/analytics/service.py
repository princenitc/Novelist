"""Analytics query use cases."""
from .ports import AnalyticsRepositoryPort


class AnalyticsService:
    def __init__(self, repository: AnalyticsRepositoryPort):
        self.repository = repository

    def book_stats(self, book_id: str) -> dict:
        return self.repository.book_stats(book_id)

    def trending_books(self, limit: int) -> list[dict]:
        return self.repository.trending(limit)

    def genre_popularity(self) -> list[dict]:
        return self.repository.genres()
