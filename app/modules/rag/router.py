"""RAG routes: index a book's content, semantic search."""
from fastapi import APIRouter, Depends

from app.core.dependencies import RagServiceDep, Repo
from app.core.security import get_current_user_id
from app.modules.rag.schemas import RagIndexOut, RagIndexRequest, RagSearchOut, RagSearchRequest

router = APIRouter(prefix="/api/v1/rag", tags=["rag"])


@router.post("/index", response_model=RagIndexOut, status_code=201)
def index_book(body: RagIndexRequest, service: RagServiceDep, repo: Repo, _: str = Depends(get_current_user_id)):
    """Chunk and embed a book's ``content`` field, storing vectors in Neo4j."""
    book = repo.get_book(body.book_id)
    result = service.index_book(book)
    return result


@router.post("/search", response_model=RagSearchOut)
def semantic_search(body: RagSearchRequest, service: RagServiceDep, _: str = Depends(get_current_user_id)):
    """Return the *top_k* most semantically similar book chunks for a free-text query."""
    raw = service.search(body.query, body.top_k)
    results = [
        {
            "book_id": r["bookId"],
            "title": r["title"],
            "author": r["author"],
            "chunk_index": r["chunkIndex"],
            "text": r["text"],
            "score": r["score"],
        }
        for r in raw
    ]
    return {"query": body.query, "results": results}
