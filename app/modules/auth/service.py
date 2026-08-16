"""Authentication use cases."""
from datetime import datetime, timedelta, timezone

from app.core.config import get_settings
from app.core.http import error
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_refresh_token,
    hash_password,
    verify_password,
)
from .ports import AuthRepositoryPort


class AuthService:
    def __init__(self, repository: AuthRepositoryPort):
        self.repository = repository

    def register(self, name: str, email: str, age: int, password: str) -> dict:
        if self.repository.find_user_by_email(email):
            raise error(409, f"User already exists with identifier: {email}")
        return self.repository.create_user({"name": name, "email": email, "age": age, "passwordHash": hash_password(password)})

    def login(self, email: str, password: str) -> dict[str, str]:
        user = self.repository.find_user_by_email(email)
        if not user or not verify_password(password, user.get("passwordHash") or ""):
            raise error(401, "Invalid credentials")
        user_id = str(user["userId"])
        refresh_token, jti = create_refresh_token(user_id)
        settings = get_settings()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.jwt_refresh_expiry_days)
        self.repository.store_refresh_token(jti, user_id, expires_at)
        return {
            "access_token": create_access_token(user_id),
            "refresh_token": refresh_token,
            "token_type": "bearer",
        }

    def refresh(self, refresh_token: str) -> dict[str, str]:
        """Issue a new access token if the refresh token is valid and not revoked."""
        user_id, jti = decode_refresh_token(refresh_token)
        record = self.repository.find_refresh_token(jti)
        if not record or record.get("revoked"):
            raise error(401, "Refresh token has been revoked")
        return {"access_token": create_access_token(user_id), "token_type": "bearer"}

    def logout(self, refresh_token: str) -> None:
        """Revoke the refresh token so it cannot be reused."""
        _user_id, jti = decode_refresh_token(refresh_token)
        self.repository.revoke_refresh_token(jti)
