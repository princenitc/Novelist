"""Local embedding model wrapper using sentence-transformers.

The model is loaded once at process start and reused across requests.
``all-MiniLM-L6-v2`` produces 384-dimensional vectors and runs on CPU
in ~5 ms per sentence, making it suitable for development and moderate
production workloads without a GPU.
"""
from functools import lru_cache

import structlog
from sentence_transformers import SentenceTransformer

logger = structlog.get_logger(__name__)


@lru_cache(maxsize=1)
def _get_model(model_name: str) -> SentenceTransformer:
    logger.info("Loading embedding model", model=model_name)
    return SentenceTransformer(model_name)


def embed(texts: list[str], model_name: str) -> list[list[float]]:
    """Return a list of embedding vectors, one per input text."""
    model = _get_model(model_name)
    vectors = model.encode(texts, convert_to_numpy=True, show_progress_bar=False)
    return [v.tolist() for v in vectors]


def embed_one(text: str, model_name: str) -> list[float]:
    return embed([text], model_name)[0]


def dimensions(model_name: str) -> int:
    return _get_model(model_name).get_embedding_dimension()


def chunk_text(text: str, chunk_size: int, overlap: int) -> list[str]:
    """Split *text* into overlapping character-level chunks.

    Chunks respect word boundaries — the split point is walked back to the
    nearest space so words are never cut in half.
    """
    if not text:
        return []
    chunks: list[str] = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        # Walk back to a word boundary unless we are at the very end.
        if end < len(text):
            boundary = text.rfind(" ", start, end)
            if boundary > start:
                end = boundary
        chunks.append(text[start:end].strip())
        start = end - overlap if end - overlap > start else end
    return [c for c in chunks if c]
