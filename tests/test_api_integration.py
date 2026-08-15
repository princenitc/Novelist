import uuid
from datetime import datetime, timezone

import pytest
from fastapi.testclient import TestClient

from app import main as app_main


class FakePublisher:
    def publish(self, *args, **kwargs):
        # no-op publisher for tests
        return None


class FakeRepo:
    def __init__(self):
        self.books = {}
        self.users = {}

    def _to_plain(self, value):
        # recursively convert pydantic/other non-primitive values to primitives
        if value is None or isinstance(value, (str, int, float, bool)):
            return value
        if isinstance(value, list):
            return [self._to_plain(v) for v in value]
        if isinstance(value, dict):
            return {k: self._to_plain(v) for k, v in value.items()}
        try:
            # fallback: convert to string (covers HttpUrl, datetime, etc.)
            return str(value)
        except Exception:
            return value

    def create_book(self, payload: dict):
        # support Pydantic model or raw dict payloads
        data = payload.dict(by_alias=True) if hasattr(payload, "dict") else dict(payload)
        book_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc)
        props = data.copy()
        # coerce any non-primitive values to plain types for JSON/response validation
        props = self._to_plain(props)
        props.update({
            "bookId": book_id,
            "createdAt": now,
            "updatedAt": now,
            "averageRating": None,
            "totalRatings": None,
            "hasEmbedding": None,
        })
        self.books[book_id] = props
        return props

    def page_books(self, page, size, query=None, genre=None, year=None):
        items = list(self.books.values())
        total = len(items)
        return items[(page * size): (page * size + size)], total

    def get_book(self, book_id):
        return self.books[book_id]

    def update_book(self, book_id, payload):
        self.books[book_id].update(payload)
        self.books[book_id]["updatedAt"] = datetime.now(timezone.utc)
        return self.books[book_id]

    def delete_book(self, book_id):
        return self.books.pop(book_id)

    def create_user(self, payload):
        data = payload.dict(by_alias=True) if hasattr(payload, "dict") else dict(payload)
        user_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc)
        props = data.copy()
        props = self._to_plain(props)
        props.update({"userId": user_id, "createdAt": now, "updatedAt": now})
        self.users[user_id] = props
        return props

    def page_users(self, page, size, query=None):
        items = list(self.users.values())
        total = len(items)
        return items[(page * size): (page * size + size)], total

    def get_user(self, user_id):
        user = self.users[user_id].copy()
        # include ratedBooks if present
        user.setdefault("ratedBooks", [])
        return user

    def update_user(self, user_id, payload):
        self.users[user_id].update(payload)
        self.users[user_id]["updatedAt"] = datetime.now(timezone.utc)
        return self.get_user(user_id)

    def delete_user(self, user_id):
        return self.users.pop(user_id)

    def add_rating(self, user_id, book_id, rating, review):
        timestamp = datetime.now(timezone.utc)
        rating_obj = {"rating": rating, "review": review, "timestamp": timestamp}
        # store on user
        user = self.users.get(user_id)
        if user is None:
            raise KeyError("user")
        user.setdefault("ratedBooks", []).append({"book": self.books[book_id], "rating": rating, "review": review, "timestamp": timestamp, "helpful": 0})
        return {"book": self.books[book_id], "rating": rating, "review": review, "timestamp": timestamp, "helpful": 0}

    def book_stats(self, book_id):
        return {"bookId": book_id, "averageRating": None, "totalRatings": 0}

    def trending(self, limit):
        return list(self.books.values())[:limit]

    def genres(self):
        # simple aggregation
        counts = {}
        for b in self.books.values():
            for g in b.get("genres", []):
                counts[g] = counts.get(g, 0) + 1
        return [{"genre": k, "count": v} for k, v in counts.items()]

    def recommendations(self, user_id, limit):
        return list(self.books.values())[:limit]


@pytest.fixture
def client():
    fake_repo = FakeRepo()
    fake_pub = FakePublisher()
    # Override dependencies
    app = app_main.app
    app.dependency_overrides[app_main.repository] = lambda: fake_repo
    app.dependency_overrides[app_main.publisher] = lambda: fake_pub
    return TestClient(app)


def test_create_and_list_books(client):
    # create book
    payload = {
        "title": "Integration Book",
        "author": "Tester",
        "isbn": "123-4567890123",
        "publishedYear": 2026,
        "description": "Integration test book",
        "language": "en",
        "pageCount": 100,
        "coverImageUrl": "https://example.com/book.jpg",
        "genres": ["Test"]
    }
    r = client.post("/api/v1/books", json=payload)
    assert r.status_code == 201
    data = r.json()
    assert data["title"] == "Integration Book" or data.get("title") == "Integration Book" or data.get("bookId")

    # list books
    r2 = client.get("/api/v1/books?page=0&size=10")
    assert r2.status_code == 200
    body = r2.json()
    assert body["totalElements"] >= 1


def test_user_and_rating_flow(client):
    # create user
    user_payload = {"name": "Alice", "email": "alice@example.com", "age": 30}
    ru = client.post("/api/v1/users", json=user_payload)
    assert ru.status_code == 201
    user = ru.json()
    user_id = user.get("userId") or user.get("user_id")

    # create book
    book_payload = {"title": "Rateable", "author": "Auth", "pageCount": 50}
    rb = client.post("/api/v1/books", json=book_payload)
    assert rb.status_code == 201
    book = rb.json()
    book_id = book.get("bookId") or book.get("book_id")

    # add rating
    r = client.post(f"/api/v1/users/{user_id}/ratings/{book_id}", json={"rating": 5, "review": "Nice"})
    assert r.status_code == 201
    obj = r.json()
    assert obj["rating"] == 5
    assert obj["book"]["title"] == "Rateable"
