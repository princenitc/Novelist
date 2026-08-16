"""Persistence operations for analytics."""
from typing import Any

from app.infrastructure.neo4j.base import NotFoundError, node


class AnalyticsRepositoryMixin:
    def book_stats(self, book_id: str) -> dict[str, Any]:
        row = self._one("MATCH (b:Book {bookId: $book_id}) OPTIONAL MATCH (b)<-[r:RATED]-() RETURN b.bookId AS bookId, avg(r.rating) AS averageRating, count(r) AS totalRatings", book_id=book_id)
        if not row:
            raise NotFoundError(f"Book not found with id: {book_id}")
        return dict(row)

    def trending(self, limit: int) -> list[dict[str, Any]]:
        rows = self._all("MATCH (b:Book)<-[r:RATED]-() WITH b, count(r) AS count, avg(r.rating) AS average WHERE count > 0 RETURN b AS entity ORDER BY count * average DESC LIMIT $limit", limit=limit)
        return [node(row) for row in rows]

    def genres(self) -> list[dict[str, Any]]:
        return [dict(row) for row in self._all("MATCH (b:Book) UNWIND b.genres AS genre RETURN genre, count(b) AS count ORDER BY count DESC")]
