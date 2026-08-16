"""RAG request and response models."""
from pydantic import Field

from app.modules.shared.schemas import APIModel


class RagIndexRequest(APIModel):
    book_id: str


class RagChunkOut(APIModel):
    book_id: str
    title: str
    author: str
    chunk_index: int
    text: str
    score: float


class RagSearchRequest(APIModel):
    query: str = Field(min_length=1, max_length=500)
    top_k: int = Field(default=5, ge=1, le=50)


class RagSearchOut(APIModel):
    query: str
    results: list[RagChunkOut]


class RagIndexOut(APIModel):
    book_id: str
    chunks_indexed: int
    model: str
