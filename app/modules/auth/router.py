"""Authentication routes: register, login, token refresh, and logout."""
from fastapi import APIRouter, Request

from app.core.dependencies import AuthServiceDep
from app.core.limiter import limiter
from app.modules.auth.schemas import AccessTokenOut, AuthLogin, AuthRegister, RefreshRequest, TokenOut
from app.modules.users.schemas import UserOut

router = APIRouter(prefix="/auth", tags=["authentication"])

_AUTH_LIMIT = "10/minute"


@router.post("/register", status_code=201, response_model=UserOut)
@limiter.limit(_AUTH_LIMIT)
def register_user(request: Request, body: AuthRegister, service: AuthServiceDep):
    return service.register(body.name, str(body.email), body.age, body.password)


@router.post("/login", response_model=TokenOut)
@limiter.limit(_AUTH_LIMIT)
def login_user(request: Request, body: AuthLogin, service: AuthServiceDep):
    return service.login(str(body.email), body.password)


@router.post("/refresh", response_model=AccessTokenOut)
@limiter.limit(_AUTH_LIMIT)
def refresh_token(request: Request, body: RefreshRequest, service: AuthServiceDep):
    """Exchange a valid refresh token for a new short-lived access token."""
    return service.refresh(body.refresh_token)


@router.post("/logout", status_code=204)
def logout(body: RefreshRequest, service: AuthServiceDep):
    """Revoke the supplied refresh token — not rate-limited (low-risk operation)."""
    service.logout(body.refresh_token)
