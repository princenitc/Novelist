"""Pagination response construction shared by collection endpoints."""
import math
from typing import Any


def make_page(items: list[Any], total: int, page: int, size: int) -> dict[str, Any]:
    total_pages = math.ceil(total / size) if total else 0
    return {"content": items, "page": page, "size": size, "totalElements": total, "totalPages": total_pages,
            "first": page == 0, "last": total_pages == 0 or page >= total_pages - 1,
            "hasNext": page + 1 < total_pages, "hasPrevious": page > 0}
