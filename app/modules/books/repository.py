"""Persistence operations for books."""
from typing import Any
from uuid import uuid4

from app.infrastructure.neo4j.base import ConflictError, NotFoundError, node, now


class BookRepositoryMixin:
    @staticmethod
    def _book_properties(payload: dict[str, Any]) -> dict[str, Any]:
        fields = {
            "title": payload.get("title"), "author": payload.get("author"),
            "isbn": payload.get("isbn"), "publishedYear": payload.get("published_year"),
            "description": payload.get("description"), "content": payload.get("content"),
            "language": payload.get("language"), "pageCount": payload.get("page_count"),
            "coverImageUrl": str(payload["cover_image_url"]) if payload.get("cover_image_url") else None,
            "genres": payload.get("genres"),
        }
        return {key: value for key, value in fields.items() if value is not None}

    def create_book(self, payload: dict[str, Any]) -> dict[str, Any]:
        props = self._book_properties(payload)
        isbn = props.get("isbn")
        if isbn and self._one("MATCH (b:Book {isbn: $isbn}) RETURN b", isbn=isbn):
            raise ConflictError(f"Book already exists with identifier: {isbn}")
        props.update(bookId=str(uuid4()), createdAt=now(), updatedAt=now())
        return node(self._one("CREATE (b:Book) SET b = $props RETURN b AS entity", props=props))

    def get_book(self, book_id: str) -> dict[str, Any]:
        record = self._one("MATCH (b:Book {bookId: $book_id}) RETURN b AS entity", book_id=book_id)
        if not record:
            raise NotFoundError(f"Book not found with id: {book_id}")
        return node(record)

    def update_book(self, book_id: str, payload: dict[str, Any]) -> dict[str, Any]:
        self.get_book(book_id)
        props = self._book_properties(payload)
        if "isbn" in props:
            duplicate = self._one("MATCH (b:Book {isbn: $isbn}) WHERE b.bookId <> $book_id RETURN b", isbn=props["isbn"], book_id=book_id)
            if duplicate:
                raise ConflictError(f"Book already exists with identifier: {props['isbn']}")
        props["updatedAt"] = now()
        return node(self._one("MATCH (b:Book {bookId: $book_id}) SET b += $props RETURN b AS entity", book_id=book_id, props=props))

    def delete_book(self, book_id: str) -> dict[str, Any]:
        book = self.get_book(book_id)
        self._one("MATCH (b:Book {bookId: $book_id}) DETACH DELETE b", book_id=book_id)
        return book

    def page_books(
        self,
        page: int,
        size: int,
        query: str | None = None,
        genre: str | None = None,
        year: int | None = None,
        sort_by: str = "title",
        sort_order: str = "asc",
        min_rating: float | None = None,
        max_page_count: int | None = None,
    ) -> tuple[list[dict[str, Any]], int]:
        # When filtering or sorting by rating we need to aggregate RATED edges.
        needs_rating = min_rating is not None or sort_by == "rating"

        match = "MATCH (b:Book)"
        if needs_rating:
            match += " OPTIONAL MATCH (b)<-[r:RATED]-()"

        with_clause = "WITH b" + (", avg(r.rating) AS avgRating" if needs_rating else "")

        filters = [
            "($search_query IS NULL OR toLower(b.title) CONTAINS toLower($search_query) OR toLower(b.author) CONTAINS toLower($search_query))",
            "($genre IS NULL OR $genre IN b.genres)",
            "($year IS NULL OR b.publishedYear = $year)",
            "($max_page_count IS NULL OR b.pageCount <= $max_page_count)",
        ]
        if min_rating is not None:
            filters.append("avgRating >= $min_rating")

        where = "WHERE " + " AND ".join(filters)

        _sort_field = {
            "title": "toLower(b.title)",
            "createdAt": "b.createdAt",
            "rating": "avgRating",
        }.get(sort_by, "toLower(b.title)")
        _order = "DESC" if sort_order == "desc" else "ASC"
        order_by = f"ORDER BY {_sort_field} {_order}"

        params: dict[str, Any] = {
            "search_query": query,
            "genre": genre,
            "year": year,
            "max_page_count": max_page_count,
            "min_rating": min_rating,
            "skip": page * size,
            "size": size,
        }

        count_q = f"{match} {with_clause} {where} RETURN count(b) AS total"
        data_q  = f"{match} {with_clause} {where} RETURN b AS entity {order_by} SKIP $skip LIMIT $size"

        total = self._one(count_q, **params)["total"]
        rows  = self._all(data_q, **params)
        return [node(row) for row in rows], total
