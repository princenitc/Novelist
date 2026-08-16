"""Routes for the currently authenticated user's profile."""
from fastapi import APIRouter, Depends

from app.core.dependencies import ProfileServiceDep
from app.core.security import get_current_user_id
from app.modules.users.schemas import UserOut

router = APIRouter(prefix="/api/v1", tags=["profile"])


@router.get("/me", response_model=UserOut)
def get_current_profile(
    service: ProfileServiceDep,
    user_id: str = Depends(get_current_user_id),
):
    return service.get_current_profile(user_id)
