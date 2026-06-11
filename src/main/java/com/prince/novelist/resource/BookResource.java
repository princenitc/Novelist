package com.prince.novelist.resource;

import com.prince.novelist.model.Book;
import com.prince.novelist.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/books")
public class BookResource {

	@Autowired
	BookService bookService;

	@PostMapping
	public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
		Book createdBook = bookService.createBook(book);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
	}

	@GetMapping
	public ResponseEntity<Collection<Book>> getAllBooks() {
		Collection<Book> books = bookService.getAllBooks();
		return ResponseEntity.ok(books);
	}

	@GetMapping("/{bookId}")
	public ResponseEntity<Book> getBookById(@PathVariable("bookId") String bookId) {
		Book book = bookService.getBookById(bookId);
		return ResponseEntity.ok(book);
	}

	@PutMapping("/{bookId}")
	public ResponseEntity<Book> updateBook(
			@PathVariable("bookId") String bookId,
			@Valid @RequestBody Book book) {
		Book updatedBook = bookService.updateBook(book, bookId);
		return ResponseEntity.ok(updatedBook);
	}

	@DeleteMapping("/{bookId}")
	public ResponseEntity<Void> deleteBook(@PathVariable("bookId") String bookId) {
		bookService.deleteBook(bookId);
		return ResponseEntity.noContent().build();
	}
}
