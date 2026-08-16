"""Rating routes: add ratings and review books."""
from fastapi import APIRouter, Depends

from app.core.dependencies import RatingServiceDep
from app.core.security import get_current_user_id, require_self
from app.modules.ratings.schemas import RatingCreate, RatingOut

router = APIRouter(prefix="/api/v1/users", tags=["ratings"])


@router.post("/{user_id}/ratings/{book_id}", status_code=201, response_model=RatingOut)
def add_rating(user_id: str, book_id: str, body: RatingCreate, service: RatingServiceDep, caller_id: str = Depends(get_current_user_id)):
    require_self(user_id, caller_id)
    return service.add(user_id, book_id, body.rating, body.review)
