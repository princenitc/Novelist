import logging
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Annotated

from fastapi import Depends, FastAPI, HTTPException, Query, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator

from .config import Settings, get_settings
from .database import ConflictError, NotFoundError, NovelistRepository, make_page
from .events import EventPublisher
from .schemas import (BookCreate, BookOut, BookUpdate, PageOut, PreferencesUpdate,
                      RatingCreate, RatingOut, UserCreate, UserOut, UserUpdate)

logging.basicConfig(level=logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    app.state.repository = NovelistRepository(settings)
    app.state.publisher = EventPublisher(settings)
    try:
        app.state.repository.verify()
    except Exception:
        logging.getLogger(__name__).warning("Neo4j is not reachable at startup; requests will retry the connection.")
    yield
    app.state.repository.close()


app = FastAPI(title="Novelist API", version="1.0", lifespan=lifespan)
Instrumentator().instrument(app).expose(app, endpoint="/actuator/prometheus", include_in_schema=False)


def repository(request: Request) -> NovelistRepository:
    return request.app.state.repository


def publisher(request: Request) -> EventPublisher:
    return request.app.state.publisher


Repo = Annotated[NovelistRepository, Depends(repository)]
Publisher = Annotated[EventPublisher, Depends(publisher)]


def error(status: int, message: str) -> HTTPException:
    return HTTPException(status_code=status, detail=message)


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    errors = {".".join(str(part) for part in item["loc"] if part != "body"): item["msg"] for item in exc.errors()}
    return JSONResponse(status_code=400, content={"status": 400, "message": "Validation failed", "errors": errors,
                                                   "timestamp": datetime.now(timezone.utc).isoformat()})


@app.exception_handler(NotFoundError)
async def not_found_handler(request: Request, exc: NotFoundError) -> JSONResponse:
    return JSONResponse(status_code=404, content={"status": 404, "message": str(exc), "timestamp": datetime.now(timezone.utc).isoformat()})


@app.exception_handler(ConflictError)
async def conflict_handler(request: Request, exc: ConflictError) -> JSONResponse:
    return JSONResponse(status_code=409, content={"status": 409, "message": str(exc), "timestamp": datetime.now(timezone.utc).isoformat()})


def check_paging(page: int, size: int) -> None:
    if page < 0 or not 1 <= size <= 100:
        raise error(400, "page must be non-negative and size must be between 1 and 100")


@app.get("/actuator/health", include_in_schema=False)
def health(repo: Repo) -> dict[str, str]:
    try:
        repo.verify()
    except Exception as exc:
        raise error(503, "Neo4j is unavailable") from exc
    return {"status": "UP"}


@app.post("/api/v1/books", status_code=201, response_model=BookOut)
def create_book(body: BookCreate, repo: Repo, events: Publisher):
    book = repo.create_book(body.model_dump())
    events.publish("book.created", {"action": "CREATED", "book": book})
    return book


@app.get("/api/v1/books", response_model=PageOut)
def list_books(repo: Repo, page: int = 0, size: int = 20):
    check_paging(page, size)
    books, total = repo.page_books(page, size)
    return make_page(books, total, page, size)


@app.get("/api/v1/books/search", response_model=PageOut)
def search_books(repo: Repo, query: str | None = None, genre: str | None = None, year: int | None = None, page: int = 0, size: int = 20):
    check_paging(page, size)
    books, total = repo.page_books(page, size, query.strip() if query and query.strip() else None, genre, year)
    return make_page(books, total, page, size)


@app.get("/api/v1/books/{book_id}", response_model=BookOut)
def get_book(book_id: str, repo: Repo):
    return repo.get_book(book_id)


@app.put("/api/v1/books/{book_id}", response_model=BookOut)
def update_book(book_id: str, body: BookUpdate, repo: Repo, events: Publisher):
    book = repo.update_book(book_id, body.model_dump(exclude_unset=True))
    events.publish("book.updated", {"action": "UPDATED", "book": book})
    return book


@app.delete("/api/v1/books/{book_id}", status_code=204)
def delete_book(book_id: str, repo: Repo, events: Publisher) -> Response:
    book = repo.delete_book(book_id)
    events.publish("book.deleted", {"action": "DELETED", "book": book})
    return Response(status_code=204)


@app.post("/api/v1/users", status_code=201, response_model=UserOut)
def create_user(body: UserCreate, repo: Repo):
    return repo.create_user(body.model_dump())


@app.get("/api/v1/users", response_model=PageOut)
def list_users(repo: Repo, page: int = 0, size: int = 20):
    check_paging(page, size)
    users, total = repo.page_users(page, size)
    return make_page(users, total, page, size)


@app.get("/api/v1/users/search", response_model=PageOut)
def search_users(query: str, repo: Repo, page: int = 0, size: int = 20):
    check_paging(page, size)
    if not query.strip():
        raise error(400, "Search query must not be blank")
    users, total = repo.page_users(page, size, query.strip())
    return make_page(users, total, page, size)


@app.get("/api/v1/users/{user_id}", response_model=UserOut)
def get_user(user_id: str, repo: Repo):
    return repo.get_user(user_id)


@app.put("/api/v1/users/{user_id}", response_model=UserOut)
def update_user(user_id: str, body: UserUpdate, repo: Repo):
    return repo.update_user(user_id, body.model_dump(exclude_unset=True))


@app.put("/api/v1/users/{user_id}/preferences", response_model=UserOut)
def update_preferences(user_id: str, body: PreferencesUpdate, repo: Repo):
    return repo.update_user(user_id, {"preferences": body.preferences.model_dump()})


@app.delete("/api/v1/users/{user_id}", status_code=204)
def delete_user(user_id: str, repo: Repo) -> Response:
    repo.delete_user(user_id)
    return Response(status_code=204)


@app.post("/api/v1/users/{user_id}/ratings/{book_id}", status_code=201, response_model=RatingOut)
def add_rating(user_id: str, book_id: str, body: RatingCreate, repo: Repo, events: Publisher):
    rating = repo.add_rating(user_id, book_id, body.rating, body.review)
    events.publish("rating.added", {"userId": user_id, "bookId": book_id, "rating": body.rating, "review": body.review})
    return rating


@app.get("/api/v1/analytics/books/{book_id}/stats")
def book_stats(book_id: str, repo: Repo):
    return repo.book_stats(book_id)


@app.get("/api/v1/analytics/books/trending", response_model=list[BookOut])
def trending_books(repo: Repo, limit: int = Query(default=10, ge=1, le=100)):
    return repo.trending(limit)


@app.get("/api/v1/analytics/genres")
def genre_popularity(repo: Repo):
    return repo.genres()


@app.get("/api/v1/recommendations/users/{user_id}", response_model=list[BookOut])
def recommendations(user_id: str, repo: Repo, limit: int = Query(default=10, ge=1, le=100)):
    return repo.recommendations(user_id, limit)
