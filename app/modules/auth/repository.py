"""Persistence operations for refresh tokens."""
from datetime import datetime
from typing import Any

from app.infrastructure.neo4j.base import node, now


class AuthRepositoryMixin:
    def store_refresh_token(self, jti: str, user_id: str, expires_at: datetime) -> None:
        self._one(
            "CREATE (t:RefreshToken {jti: $jti, userId: $user_id, expiresAt: $expires_at, revoked: false, createdAt: $created_at})",
            jti=jti,
            user_id=user_id,
            expires_at=expires_at.isoformat(),
            created_at=now(),
        )

    def find_refresh_token(self, jti: str) -> dict[str, Any] | None:
        record = self._one(
            "MATCH (t:RefreshToken {jti: $jti}) RETURN t AS entity",
            jti=jti,
        )
        return node(record) if record else None

    def revoke_refresh_token(self, jti: str) -> None:
        self._one(
            "MATCH (t:RefreshToken {jti: $jti}) SET t.revoked = true",
            jti=jti,
        )
