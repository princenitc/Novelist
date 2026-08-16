"""Persistence operations for ratings."""
from typing import Any

from app.infrastructure.neo4j.base import now, to_native


class RatingRepositoryMixin:
    def add_rating(self, user_id: str, book_id: str, rating: int, review: str | None) -> dict[str, Any]:
        self.get_user(user_id)
        book = self.get_book(book_id)
        record = self._one(
            "MATCH (u:User {userId: $user_id}), (b:Book {bookId: $book_id}) MERGE (u)-[r:RATED]->(b) "
            "ON CREATE SET r.timestamp = $timestamp, r.helpful = 0 SET r.rating = $rating, r.review = $review RETURN r",
            user_id=user_id, book_id=book_id, rating=rating, review=review, timestamp=now(),
        )
        relation = to_native(dict(record["r"]))
        return {"book": book, "rating": relation["rating"], "review": relation.get("review"), "timestamp": relation.get("timestamp"), "helpfulCount": relation.get("helpful", 0)}
