"""Shared utility functions for the API."""
from fastapi import HTTPException


def error(status: int, message: str) -> HTTPException:
    """Create an HTTP exception with the given status code and message."""
    return HTTPException(status_code=status, detail=message)


def check_paging(page: int, size: int) -> None:
    """Validate pagination parameters."""
    if page < 0 or not 1 <= size <= 100:
        raise error(400, "page must be non-negative and size must be between 1 and 100")
