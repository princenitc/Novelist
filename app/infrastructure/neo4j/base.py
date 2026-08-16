"""Shared Neo4j repository primitives."""
from datetime import datetime, timezone
from typing import Any

from neo4j import Driver, GraphDatabase

from app.core.config import Settings


class NotFoundError(Exception):
    """Raised when a requested domain entity does not exist."""


class ConflictError(Exception):
    """Raised when a write would violate a domain uniqueness rule."""


def now() -> datetime:
    return datetime.now(timezone.utc)


def to_native(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: to_native(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [to_native(item) for item in value]
    if hasattr(value, "to_native"):
        try:
            return to_native(value.to_native())
        except Exception:
            pass
    return value


def node(record: Any, key: str = "entity") -> dict[str, Any]:
    return to_native(dict(record[key]))


class Neo4jRepository:
    """Connection lifecycle and query helpers shared by all repositories."""

    def __init__(self, settings: Settings):
        self.driver: Driver = GraphDatabase.driver(
            settings.neo4j_uri, auth=(settings.neo4j_user, settings.neo4j_password)
        )

    def close(self) -> None:
        self.driver.close()

    def verify(self) -> None:
        self.driver.verify_connectivity()

    def _one(self, query: str, **params: Any) -> Any | None:
        with self.driver.session() as session:
            return session.run(query, **params).single()

    def _all(self, query: str, **params: Any) -> list[Any]:
        with self.driver.session() as session:
            return list(session.run(query, **params))
