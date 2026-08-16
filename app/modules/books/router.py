"""Book management routes: create, list, search, update, delete."""
from typing import Literal

from fastapi import APIRouter, Depends, Query, Response

from app.core.dependencies import BookServiceDep, Repo
from app.core.security import get_current_user_id
from app.core.http import check_paging
from app.core.pagination import make_page
from app.modules.books.schemas import BookCreate, BookOut, BookUpdate
from app.modules.shared.schemas import PageOut

router = APIRouter(prefix="/api/v1/books", tags=["books"])


@router.post("", status_code=201, response_model=BookOut)
def create_book(body: BookCreate, service: BookServiceDep, _: str = Depends(get_current_user_id)):
    return service.create(body.model_dump())


@router.get("", response_model=PageOut)
def list_books(repo: Repo, page: int = 0, size: int = 20, _: str = Depends(get_current_user_id)):
    check_paging(page, size)
    books, total = repo.page_books(page, size)
    return make_page(books, total, page, size)


@router.get("/search", response_model=PageOut)
def search_books(
    repo: Repo,
    query: str | None = None,
    genre: str | None = None,
    year: int | None = None,
    sort_by: Literal["title", "createdAt", "rating"] = Query(default="title", alias="sortBy"),
    sort_order: Literal["asc", "desc"] = Query(default="asc", alias="sortOrder"),
    min_rating: float | None = Query(default=None, alias="minRating", ge=1.0, le=5.0, description="Minimum average rating (1–5)"),
    max_page_count: int | None = Query(default=None, alias="maxPageCount", ge=1, description="Maximum page count"),
    page: int = 0,
    size: int = 20,
    _: str = Depends(get_current_user_id),
):
    check_paging(page, size)
    books, total = repo.page_books(
        page, size,
        query=query.strip() if query and query.strip() else None,
        genre=genre,
        year=year,
        sort_by=sort_by,
        sort_order=sort_order,
        min_rating=min_rating,
        max_page_count=max_page_count,
    )
    return make_page(books, total, page, size)


@router.get("/{book_id}", response_model=BookOut)
def get_book(book_id: str, repo: Repo, _: str = Depends(get_current_user_id)):
    return repo.get_book(book_id)


@router.put("/{book_id}", response_model=BookOut)
def update_book(book_id: str, body: BookUpdate, service: BookServiceDep, _: str = Depends(get_current_user_id)):
    return service.update(book_id, body.model_dump(exclude_unset=True))


@router.delete("/{book_id}", status_code=204)
def delete_book(book_id: str, service: BookServiceDep, _: str = Depends(get_current_user_id)) -> Response:
    service.delete(book_id)
    return Response(status_code=204)
