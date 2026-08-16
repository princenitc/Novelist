"""Shared API models and response envelopes."""
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


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


class PreferencesUpdate(APIModel):
    preferences: Preferences


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
