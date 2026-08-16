"""Analytics routes: trending books, genre popularity, book stats."""
from fastapi import APIRouter, Depends, Query

from app.core.dependencies import AnalyticsServiceDep
from app.core.security import get_current_user_id
from app.modules.books.schemas import BookOut

router = APIRouter(prefix="/api/v1/analytics", tags=["analytics"])


@router.get("/books/{book_id}/stats")
def book_stats(book_id: str, service: AnalyticsServiceDep, _: str = Depends(get_current_user_id)):
    return service.book_stats(book_id)


@router.get("/books/trending", response_model=list[BookOut])
def trending_books(service: AnalyticsServiceDep, limit: int = Query(default=10, ge=1, le=100), _: str = Depends(get_current_user_id)):
    return service.trending_books(limit)


@router.get("/genres")
def genre_popularity(service: AnalyticsServiceDep, _: str = Depends(get_current_user_id)):
    return service.genre_popularity()
