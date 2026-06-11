package com.prince.novelist.service;

import com.prince.novelist.TestSetup;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestSetup.class)
public class BookIntegrationTest {

	@Autowired
	private BookService bookService;

	@Test
	void shouldPerformCompleteBookLifecycle() {
		// Step 1: Create a new book
		Book newBook = new Book("Gunaho ka Devta", "Dharmaveer Bharti");
		Book savedBook = bookService.createBook(newBook);
		assertNotNull(savedBook, "Saved book should not be null");
		String createdBookId = savedBook.getBookId();
		assertNotNull(createdBookId, "Book ID should be generated");

		// Step 2: Validate the created book's data
		Book retrievedBook = bookService.getBookById(createdBookId);
		assertEquals(newBook.getAuthor(), retrievedBook.getAuthor());
		assertEquals(newBook.getTitle(), retrievedBook.getTitle());

		// Step 3: Update the book
		Book updatedBook = new Book("Three Idiots", "Chetan Bhagat");
		Book updatedBookResult = bookService.updateBook(updatedBook, createdBookId);

		assertNotNull(updatedBookResult, "Updated book should not be null");
		assertEquals(updatedBook.getTitle(), updatedBookResult.getTitle());
		assertEquals(updatedBook.getAuthor(), updatedBookResult.getAuthor());

		// Verify the update persisted
		Book verifyUpdate = bookService.getBookById(createdBookId);
		assertEquals(updatedBook.getTitle(), verifyUpdate.getTitle());
		assertEquals(updatedBook.getAuthor(), verifyUpdate.getAuthor());

		// Step 4: Delete the book
		bookService.deleteBook(createdBookId);

		// Step 5: Verify deletion - should throw ResourceNotFoundException
		assertThrows(ResourceNotFoundException.class, () -> {
			bookService.getBookById(createdBookId);
		}, "Getting deleted book should throw ResourceNotFoundException");
	}
}
