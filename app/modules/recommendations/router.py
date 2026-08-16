"""Recommendation routes: personalized book recommendations."""
from fastapi import APIRouter, Depends, Query

from app.core.dependencies import RecommendationServiceDep
from app.core.security import get_current_user_id
from app.modules.books.schemas import BookOut

router = APIRouter(prefix="/api/v1/recommendations", tags=["recommendations"])


@router.get("/users/{user_id}", response_model=list[BookOut])
def recommendations(user_id: str, service: RecommendationServiceDep, limit: int = Query(default=10, ge=1, le=100), _: str = Depends(get_current_user_id)):
    return service.for_user(user_id, limit)
