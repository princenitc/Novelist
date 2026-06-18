package com.prince.novelist.resource;

import com.prince.novelist.dto.request.CreateBookRequest;
import com.prince.novelist.dto.request.UpdateBookRequest;
import com.prince.novelist.dto.response.BookResponse;
import com.prince.novelist.dto.response.PageResponse;
import com.prince.novelist.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
public class BookResource {

	private final BookService bookService;

	public BookResource(BookService bookService) {
		this.bookService = bookService;
	}

	@Operation(summary = "Create a book", description = "Create a new book resource")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Book created successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request payload"),
		@ApiResponse(responseCode = "409", description = "Book with ISBN already exists")
	})
	@PostMapping
	public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
	}

	@Operation(summary = "Get all books", description = "Retrieve paginated list of all books")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved books"),
		@ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
	})
	@GetMapping
	public ResponseEntity<PageResponse<BookResponse>> getAllBooks(
		@PageableDefault(size = 20, sort = "title") Pageable pageable
	) {
		return ResponseEntity.ok(bookService.getAllBooks(pageable));
	}

	@Operation(summary = "Get book by id", description = "Retrieve a single book by its identifier")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Book retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Book not found")
	})
	@GetMapping("/{bookId}")
	public ResponseEntity<BookResponse> getBookById(@PathVariable("bookId") String bookId) {
		return ResponseEntity.ok(bookService.getBookById(bookId));
	}

	@Operation(summary = "Update a book", description = "Update an existing book by its identifier")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Book updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request payload"),
		@ApiResponse(responseCode = "404", description = "Book not found"),
		@ApiResponse(responseCode = "409", description = "Book with ISBN already exists")
	})
	@PutMapping("/{bookId}")
	public ResponseEntity<BookResponse> updateBook(
			@PathVariable("bookId") String bookId,
			@Valid @RequestBody UpdateBookRequest request) {
		return ResponseEntity.ok(bookService.updateBook(bookId, request));
	}

	@Operation(summary = "Delete a book", description = "Delete a book by its identifier")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Book deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Book not found")
	})
	@DeleteMapping("/{bookId}")
	public ResponseEntity<Void> deleteBook(@PathVariable("bookId") String bookId) {
		bookService.deleteBook(bookId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Search books", description = "Search books by query with pagination")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Books retrieved successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid search or pagination parameters")
	})
	@GetMapping("/search")
	public ResponseEntity<PageResponse<BookResponse>> searchBooks(
		@RequestParam("query") String query,
		@PageableDefault(size = 20, sort = "title") Pageable pageable
	) {
		return ResponseEntity.ok(bookService.searchBooks(query, pageable));
	}
}
