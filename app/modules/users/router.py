"""User management routes: create, list, search, update, delete, preferences."""
from fastapi import APIRouter, Depends, Response

from app.core.dependencies import Repo, UserServiceDep
from app.core.security import get_current_user_id, require_self
from app.core.http import check_paging, error
from app.core.pagination import make_page
from app.modules.shared.schemas import PageOut, PreferencesUpdate
from app.modules.users.schemas import UserCreate, UserOut, UserUpdate

router = APIRouter(prefix="/api/v1/users", tags=["users"])


@router.post("", status_code=201, response_model=UserOut)
def create_user(body: UserCreate, repo: Repo, _: str = Depends(get_current_user_id)):
    return repo.create_user(body.model_dump())


@router.get("", response_model=PageOut)
def list_users(repo: Repo, page: int = 0, size: int = 20, _: str = Depends(get_current_user_id)):
    check_paging(page, size)
    users, total = repo.page_users(page, size)
    return make_page(users, total, page, size)


@router.get("/search", response_model=PageOut)
def search_users(query: str, repo: Repo, page: int = 0, size: int = 20, _: str = Depends(get_current_user_id)):
    check_paging(page, size)
    if not query.strip():
        raise error(400, "Search query must not be blank")
    users, total = repo.page_users(page, size, query.strip())
    return make_page(users, total, page, size)


@router.get("/{user_id}", response_model=UserOut)
def get_user(user_id: str, repo: Repo, _: str = Depends(get_current_user_id)):
    return repo.get_user(user_id)


@router.put("/{user_id}", response_model=UserOut)
def update_user(user_id: str, body: UserUpdate, repo: Repo, caller_id: str = Depends(get_current_user_id)):
    require_self(user_id, caller_id)
    return repo.update_user(user_id, body.model_dump(exclude_unset=True))


@router.put("/{user_id}/preferences", response_model=UserOut)
def update_preferences(user_id: str, body: PreferencesUpdate, service: UserServiceDep, caller_id: str = Depends(get_current_user_id)):
    require_self(user_id, caller_id)
    return service.update_preferences(user_id, body.preferences.model_dump())


@router.delete("/{user_id}", status_code=204)
def delete_user(user_id: str, repo: Repo, caller_id: str = Depends(get_current_user_id)) -> Response:
    require_self(user_id, caller_id)
    repo.delete_user(user_id)
    return Response(status_code=204)
