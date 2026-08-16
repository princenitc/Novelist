"""RAG use cases: index a book's content, search by semantic query."""
import structlog

from app.core.config import Settings
from app.infrastructure.neo4j.base import NotFoundError
from .embedding_service import chunk_text, dimensions, embed, embed_one
from .ports import RagRepositoryPort

logger = structlog.get_logger(__name__)


class RagService:
    # Class-level flag so the check survives across request-scoped instances.
    # The vector index creation is idempotent (IF NOT EXISTS), so a one-time
    # call per process is sufficient.
    _index_ready: bool = False

    def __init__(self, repository: RagRepositoryPort, settings: Settings):
        self.repo = repository
        self.settings = settings

    def _ensure_index(self) -> None:
        if not RagService._index_ready:
            dims = dimensions(self.settings.rag_embedding_model)
            self.repo.ensure_vector_index(dims)
            RagService._index_ready = True

    def index_book(self, book: dict) -> dict:
        """Chunk and embed ``book['content']``, store vectors in Neo4j.

        Raises ``NotFoundError`` if the book has no content to index.
        Returns a summary dict with ``book_id``, ``chunks_indexed``, ``model``.
        """
        content: str = book.get("content") or ""
        if not content.strip():
            raise NotFoundError(
                f"Book {book.get('bookId')} has no content — add content before indexing"
            )

        self._ensure_index()
        model = self.settings.rag_embedding_model
        texts = chunk_text(content, self.settings.rag_chunk_size, self.settings.rag_chunk_overlap)

        logger.info("Embedding chunks", count=len(texts), book_id=book.get("bookId"))
        vectors = embed(texts, model)

        chunks = [
            {"index": i, "text": text, "embedding": vector}
            for i, (text, vector) in enumerate(zip(texts, vectors))
        ]
        self.repo.store_chunks(book["bookId"], chunks)
        logger.info("Stored chunks", count=len(chunks), book_id=book.get("bookId"))

        return {
            "book_id": book["bookId"],
            "chunks_indexed": len(chunks),
            "model": model,
        }

    def search(self, query: str, top_k: int | None = None) -> list[dict]:
        """Embed *query* and return the closest chunks from the vector index."""
        self._ensure_index()
        k = top_k if top_k is not None else self.settings.rag_top_k
        model = self.settings.rag_embedding_model
        query_vector = embed_one(query, model)
        return self.repo.vector_search(query_vector, k)
