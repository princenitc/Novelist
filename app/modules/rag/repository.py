"""Neo4j persistence for RAG chunks and vector search."""
from typing import Any

from app.infrastructure.neo4j.base import node


_INDEX_NAME = "book_chunk_embeddings"


class RagRepositoryMixin:
    """Mixin for NovelistRepository — owns all RAG Cypher queries."""

    # ------------------------------------------------------------------
    # Schema
    # ------------------------------------------------------------------

    def ensure_vector_index(self, dimensions: int) -> None:
        """Create the vector index if it does not already exist.

        Neo4j 5.11+ supports native vector indexes via CREATE VECTOR INDEX.
        The call is idempotent thanks to IF NOT EXISTS.
        """
        self._one(
            f"""
            CREATE VECTOR INDEX {_INDEX_NAME} IF NOT EXISTS
            FOR (c:BookChunk) ON (c.embedding)
            OPTIONS {{
                indexConfig: {{
                    `vector.dimensions`: $dims,
                    `vector.similarity_function`: 'cosine'
                }}
            }}
            """,
            dims=dimensions,
        )

    # ------------------------------------------------------------------
    # Write
    # ------------------------------------------------------------------

    def store_chunks(self, book_id: str, chunks: list[dict[str, Any]]) -> None:
        """Replace all existing chunks for a book, then write new ones.

        Each element of *chunks* must have keys: ``index``, ``text``, ``embedding``.
        The book node is looked up by bookId so metadata (title, author) is
        always current even if the book was updated after indexing.
        """
        self.delete_chunks(book_id)
        self._one(
            """
            MATCH (b:Book {bookId: $book_id})
            WITH b
            UNWIND $chunks AS c
            CREATE (chunk:BookChunk {
                bookId:     $book_id,
                chunkIndex: c.index,
                text:       c.text,
                embedding:  c.embedding
            })
            CREATE (b)-[:HAS_CHUNK]->(chunk)
            """,
            book_id=book_id,
            chunks=chunks,
        )

    def delete_chunks(self, book_id: str) -> None:
        self._one(
            "MATCH (b:Book {bookId: $book_id})-[:HAS_CHUNK]->(c:BookChunk) DETACH DELETE c",
            book_id=book_id,
        )

    # ------------------------------------------------------------------
    # Read
    # ------------------------------------------------------------------

    def vector_search(self, query_vector: list[float], top_k: int) -> list[dict[str, Any]]:
        """Return the *top_k* most similar chunks with their book metadata."""
        records = self._all(
            f"""
            CALL db.index.vector.queryNodes('{_INDEX_NAME}', $top_k, $query_vector)
            YIELD node AS chunk, score
            MATCH (b:Book)-[:HAS_CHUNK]->(chunk)
            RETURN b.bookId  AS bookId,
                   b.title   AS title,
                   b.author  AS author,
                   chunk.chunkIndex AS chunkIndex,
                   chunk.text       AS text,
                   score
            ORDER BY score DESC
            """,
            top_k=top_k,
            query_vector=query_vector,
        )
        return [dict(r) for r in records]

    def get_indexed_book_ids(self) -> list[str]:
        """Return the distinct book IDs that have at least one indexed chunk."""
        records = self._all(
            "MATCH (b:Book)-[:HAS_CHUNK]->(:BookChunk) RETURN DISTINCT b.bookId AS bookId"
        )
        return [r["bookId"] for r in records]
