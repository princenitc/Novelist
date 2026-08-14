from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, EmailStr, Field, HttpUrl, field_validator


def to_camel(value: str) -> str:
    first, *rest = value.split("_")
    return first + "".join(word.capitalize() for word in rest)


class APIModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class Preferences(APIModel):
    favorite_genres: list[str] | None = None
    favorite_authors: list[str] | None = None
    annual_reading_goal: int | None = Field(default=None, ge=1)
    email_notifications: bool | None = None
    recommendation_notifications: bool | None = None


class BookCreate(APIModel):
    title: str = Field(min_length=1, max_length=200)
    author: str = Field(min_length=1, max_length=100)
    isbn: str | None = Field(default=None, pattern=r"^[0-9X-]{10,17}$")
    published_year: int | None = Field(default=None, ge=1000, le=2100)
    description: str | None = Field(default=None, max_length=2000)
    content: str | None = Field(default=None, max_length=1_000_000)
    language: str | None = Field(default=None, pattern=r"^[a-z]{2}$")
    page_count: int | None = Field(default=None, ge=1)
    cover_image_url: HttpUrl | None = None
    genres: list[str] | None = None

    @field_validator("title", "author")
    @classmethod
    def strip_required_text(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("must not be blank")
        return value


class BookUpdate(APIModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, extra="forbid")
    title: str | None = Field(default=None, min_length=1, max_length=200)
    author: str | None = Field(default=None, min_length=1, max_length=100)
    isbn: str | None = Field(default=None, pattern=r"^[0-9X-]{10,17}$")
    published_year: int | None = Field(default=None, ge=1000, le=2100)
    description: str | None = Field(default=None, max_length=2000)
    content: str | None = Field(default=None, max_length=1_000_000)
    language: str | None = Field(default=None, pattern=r"^[a-z]{2}$")
    page_count: int | None = Field(default=None, ge=1)
    cover_image_url: HttpUrl | None = None
    genres: list[str] | None = None


class UserCreate(APIModel):
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr | None = Field(default=None, max_length=255)
    age: int = Field(ge=0, le=150)
    preferences: Preferences | None = None

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


class PreferencesUpdate(APIModel):
    preferences: Preferences


class RatingCreate(APIModel):
    rating: int = Field(ge=1, le=5)
    review: str | None = Field(default=None, max_length=1000)


class BookOut(APIModel):
    book_id: str
    title: str
    author: str
    isbn: str | None = None
    published_year: int | None = None
    description: str | None = None
    language: str | None = None
    page_count: int | None = None
    cover_image_url: str | None = None
    genres: list[str] | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None
    average_rating: float | None = None
    total_ratings: int | None = None
    has_embedding: bool | None = None


class RatingOut(APIModel):
    book: BookOut | None
    rating: int
    review: str | None = None
    timestamp: datetime | None = None
    helpful_count: int | None = None


class UserOut(APIModel):
    user_id: str
    name: str
    email: str | None = None
    age: int
    preferences: Preferences | None = None
    rated_books: list[RatingOut] = []
    created_at: datetime | None = None
    updated_at: datetime | None = None


class PageOut(APIModel):
    content: list[Any]
    page: int
    size: int
    total_elements: int
    total_pages: int
    first: bool
    last: bool
    has_next: bool
    has_previous: bool
