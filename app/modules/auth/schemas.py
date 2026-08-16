"""Authentication request and response models."""
from pydantic import ConfigDict, EmailStr, Field

from app.modules.shared.schemas import APIModel


class AuthRegister(APIModel):
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr
    age: int = Field(ge=0, le=150)
    password: str = Field(min_length=8, max_length=128)


class AuthLogin(APIModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class RefreshRequest(APIModel):
    refresh_token: str


class TokenOut(APIModel):
    model_config = ConfigDict(alias_generator=lambda value: value, populate_by_name=True)
    access_token: str
    refresh_token: str | None = None
    token_type: str = "bearer"


class AccessTokenOut(APIModel):
    """Returned by /auth/refresh — access token only, no new refresh token."""
    model_config = ConfigDict(alias_generator=lambda value: value, populate_by_name=True)
    access_token: str
    token_type: str = "bearer"
