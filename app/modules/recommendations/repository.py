"""Persistence operations for personalized recommendations."""
from typing import Any

from app.infrastructure.neo4j.base import node


class RecommendationRepositoryMixin:
    def recommendations(self, user_id: str, limit: int) -> list[dict[str, Any]]:
        rows = self._all(
            "MATCH (u:User {userId: $user_id})-[r1:RATED]->(b1:Book)<-[r2:RATED]-(other:User)-[r3:RATED]->(b2:Book) "
            "WHERE r1.rating >= 4 AND r2.rating >= 4 AND r3.rating >= 4 AND NOT (u)-[:RATED]->(b2) RETURN b2 AS entity, count(other) AS score ORDER BY score DESC LIMIT $limit",
            user_id=user_id, limit=limit,
        )
        return [node(row) for row in rows]
