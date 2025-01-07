package com.prince.novelist.service;

import com.prince.novelist.TestSetup;
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
	void createBookIntegrationTest() throws Exception {

		// Step 1: Create a new book
		Book newBook = new Book("Gunaho ka Devta", "Dharmaveer Bharti");
		Book savedBook = bookService.addBook(newBook);
		assertNotNull(savedBook, "Saved book should not be null");
		String createdBookId = savedBook.getId();

		// Step 2: Validate the created book's data
		assertEquals(newBook.getAuthor(), bookService.getBookById(createdBookId).getAuthor());
		assertEquals(newBook.getTitle(), bookService.getBookById(createdBookId).getTitle());

		// Step 3: Update the book
		Book updatedBook = new Book("Three Idiots", "Chetan Bhagat");
		Book updatedBookResult = bookService.updateBookById(updatedBook, createdBookId);

		assertNotNull(updatedBookResult, "Updated book should not be null");
		assertEquals(updatedBook.getTitle(), bookService.getBookById(createdBookId).getTitle());
		assertEquals(updatedBook.getAuthor(), bookService.getBookById(createdBookId).getAuthor());

		// Step 4: Delete the book
		boolean isDeleted = bookService.deleteBookById(createdBookId);
		assertTrue(isDeleted, "The book should be deleted successfully");
	}
}
