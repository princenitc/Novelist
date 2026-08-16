"""Book use cases and domain-event orchestration."""
from app.modules.shared.ports import EventPublisherPort
from .ports import BookRepositoryPort


class BookService:
    def __init__(self, repository: BookRepositoryPort, publisher: EventPublisherPort):
        self.repository, self.publisher = repository, publisher

    def create(self, payload: dict) -> dict:
        book = self.repository.create_book(payload)
        self.publisher.publish("book.created", {"action": "CREATED", "book": book})
        return book

    def update(self, book_id: str, payload: dict) -> dict:
        book = self.repository.update_book(book_id, payload)
        self.publisher.publish("book.updated", {"action": "UPDATED", "book": book})
        return book

    def delete(self, book_id: str) -> None:
        book = self.repository.delete_book(book_id)
        self.publisher.publish("book.deleted", {"action": "DELETED", "book": book})
