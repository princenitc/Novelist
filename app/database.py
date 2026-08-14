import json
import math
from collections.abc import Callable
from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from neo4j import Driver, GraphDatabase

from .config import Settings


class NotFoundError(Exception):
    pass


class ConflictError(Exception):
    pass


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _node(record: Any, key: str = "entity") -> dict[str, Any]:
    return dict(record[key])


class NovelistRepository:
    def __init__(self, settings: Settings):
        self.driver: Driver = GraphDatabase.driver(
            settings.neo4j_uri, auth=(settings.neo4j_user, settings.neo4j_password)
        )

    def close(self) -> None:
        self.driver.close()

    def verify(self) -> None:
        self.driver.verify_connectivity()

    def _one(self, query: str, **params: Any) -> Any | None:
        with self.driver.session() as session:
            return session.run(query, **params).single()

    def _all(self, query: str, **params: Any) -> list[Any]:
        with self.driver.session() as session:
            return list(session.run(query, **params))

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

    @staticmethod
    def _user_properties(payload: dict[str, Any]) -> dict[str, Any]:
        fields = {"name": payload.get("name"), "email": payload.get("email"), "age": payload.get("age")}
        if payload.get("preferences") is not None:
            fields["preferencesJson"] = json.dumps(payload["preferences"], separators=(",", ":"))
        return {key: value for key, value in fields.items() if value is not None}

    @staticmethod
    def _book_out(node: dict[str, Any]) -> dict[str, Any]:
        return node

    @staticmethod
    def _user_out(node: dict[str, Any], ratings: list[dict[str, Any]] | None = None) -> dict[str, Any]:
        preferences = node.pop("preferencesJson", None)
        if preferences:
            node["preferences"] = json.loads(preferences)
        if ratings is not None:
            node["ratedBooks"] = ratings
        return node

    def create_book(self, payload: dict[str, Any]) -> dict[str, Any]:
        props = self._book_properties(payload)
        isbn = props.get("isbn")
        if isbn and self._one("MATCH (b:Book {isbn: $isbn}) RETURN b", isbn=isbn):
            raise ConflictError(f"Book already exists with identifier: {isbn}")
        props.update(bookId=str(uuid4()), createdAt=_now(), updatedAt=_now())
        record = self._one("CREATE (b:Book) SET b = $props RETURN b AS entity", props=props)
        return self._book_out(_node(record))

    def get_book(self, book_id: str) -> dict[str, Any]:
        record = self._one("MATCH (b:Book {bookId: $book_id}) RETURN b AS entity", book_id=book_id)
        if not record:
            raise NotFoundError(f"Book not found with id: {book_id}")
        return self._book_out(_node(record))

    def update_book(self, book_id: str, payload: dict[str, Any]) -> dict[str, Any]:
        self.get_book(book_id)
        props = self._book_properties(payload)
        if "isbn" in props:
            duplicate = self._one(
                "MATCH (b:Book {isbn: $isbn}) WHERE b.bookId <> $book_id RETURN b",
                isbn=props["isbn"], book_id=book_id,
            )
            if duplicate:
                raise ConflictError(f"Book already exists with identifier: {props['isbn']}")
        props["updatedAt"] = _now()
        record = self._one(
            "MATCH (b:Book {bookId: $book_id}) SET b += $props RETURN b AS entity",
            book_id=book_id, props=props,
        )
        return self._book_out(_node(record))

    def delete_book(self, book_id: str) -> dict[str, Any]:
        book = self.get_book(book_id)
        self._one("MATCH (b:Book {bookId: $book_id}) DETACH DELETE b", book_id=book_id)
        return book

    def create_user(self, payload: dict[str, Any]) -> dict[str, Any]:
        props = self._user_properties(payload)
        email = props.get("email")
        if email and self._one("MATCH (u:User) WHERE toLower(u.email) = toLower($email) RETURN u", email=email):
            raise ConflictError(f"User already exists with identifier: {email}")
        props.update(userId=str(uuid4()), createdAt=_now(), updatedAt=_now())
        record = self._one("CREATE (u:User) SET u = $props RETURN u AS entity", props=props)
        return self._user_out(_node(record), [])

    def get_user(self, user_id: str) -> dict[str, Any]:
        record = self._one("MATCH (u:User {userId: $user_id}) RETURN u AS entity", user_id=user_id)
        if not record:
            raise NotFoundError(f"User not found with id: {user_id}")
        ratings = []
        for row in self._all(
            "MATCH (u:User {userId: $user_id})-[r:RATED]->(b:Book) RETURN r, b", user_id=user_id
        ):
            relation, book = dict(row["r"]), dict(row["b"])
            ratings.append({"book": book, "rating": relation["rating"], "review": relation.get("review"),
                            "timestamp": relation.get("timestamp"), "helpfulCount": relation.get("helpful", 0)})
        return self._user_out(_node(record), ratings)

    def update_user(self, user_id: str, payload: dict[str, Any]) -> dict[str, Any]:
        self.get_user(user_id)
        props = self._user_properties(payload)
        if "email" in props:
            duplicate = self._one(
                "MATCH (u:User) WHERE toLower(u.email) = toLower($email) AND u.userId <> $user_id RETURN u",
                email=props["email"], user_id=user_id,
            )
            if duplicate:
                raise ConflictError(f"User already exists with identifier: {props['email']}")
        props["updatedAt"] = _now()
        self._one("MATCH (u:User {userId: $user_id}) SET u += $props RETURN u", user_id=user_id, props=props)
        return self.get_user(user_id)

    def delete_user(self, user_id: str) -> None:
        self.get_user(user_id)
        self._one("MATCH (u:User {userId: $user_id}) DETACH DELETE u", user_id=user_id)

    def add_rating(self, user_id: str, book_id: str, rating: int, review: str | None) -> dict[str, Any]:
        self.get_user(user_id)
        book = self.get_book(book_id)
        record = self._one(
            "MATCH (u:User {userId: $user_id}), (b:Book {bookId: $book_id}) "
            "MERGE (u)-[r:RATED]->(b) "
            "ON CREATE SET r.timestamp = $timestamp, r.helpful = 0 "
            "SET r.rating = $rating, r.review = $review RETURN r",
            user_id=user_id, book_id=book_id, rating=rating, review=review, timestamp=_now(),
        )
        relation = dict(record["r"])
        return {"book": book, "rating": relation["rating"], "review": relation.get("review"),
                "timestamp": relation.get("timestamp"), "helpfulCount": relation.get("helpful", 0)}

    def page_books(self, page: int, size: int, query: str | None = None, genre: str | None = None, year: int | None = None) -> tuple[list[dict[str, Any]], int]:
        where = "WHERE ($query IS NULL OR toLower(b.title) CONTAINS toLower($query) OR toLower(b.author) CONTAINS toLower($query)) AND ($genre IS NULL OR $genre IN b.genres) AND ($year IS NULL OR b.publishedYear = $year)"
        params = {"query": query, "genre": genre, "year": year, "skip": page * size, "size": size}
        total = self._one(f"MATCH (b:Book) {where} RETURN count(b) AS total", **params)["total"]
        rows = self._all(f"MATCH (b:Book) {where} RETURN b AS entity ORDER BY b.title SKIP $skip LIMIT $size", **params)
        return [self._book_out(_node(row)) for row in rows], total

    def page_users(self, page: int, size: int, query: str | None = None) -> tuple[list[dict[str, Any]], int]:
        where = "WHERE ($query IS NULL OR toLower(u.name) CONTAINS toLower($query))"
        params = {"query": query, "skip": page * size, "size": size}
        total = self._one(f"MATCH (u:User) {where} RETURN count(u) AS total", **params)["total"]
        rows = self._all(f"MATCH (u:User) {where} RETURN u AS entity ORDER BY u.name SKIP $skip LIMIT $size", **params)
        return [self._user_out(_node(row), []) for row in rows], total

    def book_stats(self, book_id: str) -> dict[str, Any]:
        row = self._one("MATCH (b:Book {bookId: $book_id}) OPTIONAL MATCH (b)<-[r:RATED]-() RETURN b.bookId AS bookId, avg(r.rating) AS averageRating, count(r) AS totalRatings", book_id=book_id)
        if not row:
            raise NotFoundError(f"Book not found with id: {book_id}")
        return dict(row)

    def trending(self, limit: int) -> list[dict[str, Any]]:
        rows = self._all("MATCH (b:Book)<-[r:RATED]-() WITH b, count(r) AS count, avg(r.rating) AS average WHERE count > 0 RETURN b AS entity ORDER BY count * average DESC LIMIT $limit", limit=limit)
        return [self._book_out(_node(row)) for row in rows]

    def genres(self) -> list[dict[str, Any]]:
        return [dict(row) for row in self._all("MATCH (b:Book) UNWIND b.genres AS genre RETURN genre, count(b) AS count ORDER BY count DESC")]

    def recommendations(self, user_id: str, limit: int) -> list[dict[str, Any]]:
        rows = self._all(
            "MATCH (u:User {userId: $user_id})-[r1:RATED]->(b1:Book)<-[r2:RATED]-(other:User)-[r3:RATED]->(b2:Book) "
            "WHERE r1.rating >= 4 AND r2.rating >= 4 AND r3.rating >= 4 AND NOT (u)-[:RATED]->(b2) "
            "RETURN b2 AS entity, count(other) AS score ORDER BY score DESC LIMIT $limit", user_id=user_id, limit=limit,
        )
        return [self._book_out(_node(row)) for row in rows]


def make_page(items: list[Any], total: int, page: int, size: int) -> dict[str, Any]:
    total_pages = math.ceil(total / size) if total else 0
    return {"content": items, "page": page, "size": size, "totalElements": total, "totalPages": total_pages,
            "first": page == 0, "last": total_pages == 0 or page >= total_pages - 1,
            "hasNext": page + 1 < total_pages, "hasPrevious": page > 0}
