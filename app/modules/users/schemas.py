"""User request and response models."""
from datetime import datetime

from pydantic import EmailStr, Field, field_validator

from app.modules.ratings.schemas import RatingOut
from app.modules.shared.schemas import APIModel, Preferences


class UserCreate(APIModel):
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr | None = Field(default=None, max_length=255)
    age: int = Field(ge=0, le=150)
    preferences: Preferences | None = None
    password: str | None = Field(default=None, min_length=8, max_length=128)

    @field_validator("name")
    @classmethod
    def strip_name(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("must not be blank")
        return value


class UserUpdate(APIModel):
    name: str | None = Field(default=None, min_length=1, max_length=100)
    email: EmailStr | None = Field(default=None, max_length=255)
    age: int | None = Field(default=None, ge=0, le=150)
    preferences: Preferences | None = None


class UserOut(APIModel):
    user_id: str
    name: str
    email: str | None = None
    age: int
    preferences: Preferences | None = None
    rated_books: list[RatingOut] = []
    created_at: datetime | None = None
    updated_at: datetime | None = None
