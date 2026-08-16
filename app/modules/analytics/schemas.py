"""Analytics response models."""
from app.modules.shared.schemas import APIModel


class BookStatsOut(APIModel):
    book_id: str
    average_rating: float | None = None
    total_ratings: int


class GenreCountOut(APIModel):
    genre: str
    count: int
