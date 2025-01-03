package com.prince.novelist.service;

import com.prince.novelist.model.Book;
import com.prince.novelist.model.Review;
import com.prince.novelist.model.User;
import com.prince.novelist.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BookServiceTest {

	@InjectMocks
	BookService bookService;

	@Mock
	BookRepository bookRepository;

	@BeforeEach
	void setUpMocks() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void createdBook()  {
		Book newBook = new Book();
		newBook.setTitle("Karmabhoomi");
		newBook.setId("1");
		newBook.setAuthor("Munshi Premchandra");
		Mockito.when(bookRepository.addBook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(Optional.of(newBook));

		Book createdBook = bookService.addBook(newBook);

		assertNotNull(createdBook);
		assertEquals("Karmabhoomi", createdBook.getTitle());
		assertEquals("1", createdBook.getId());
		assertEquals("Munshi Premchandra", createdBook.getAuthor());

	}

	@Test
	void testFindBookById() {
		// Given
		String userId = "existing-book-id";
		Book existingBook = new Book();
		existingBook.setId(userId);
		existingBook.setAuthor("Premchandra");
		existingBook.setTitle("Prema");

		Mockito.when(bookRepository.getBookById(userId)).thenReturn(Optional.of(existingBook));

		// When
		Book foundBook = bookService.getBookById(userId);

		// Then
		assertNotNull(foundBook);
		assertEquals(userId, foundBook.getId());
		assertEquals("Premchandra", foundBook.getAuthor());
	}

	// Test case for updating a user
	@Test
	void testUpdateBook() throws Exception {
		// Given
		String bookId = "existing-user-id";
		Book existingBook = new Book();
		existingBook.setId(bookId);
		existingBook.setTitle("Nirmala");
		existingBook.setAuthor("Premchandra");

		Book updatedBook = new Book();
		updatedBook.setAuthor("Premchandra Updated");
		updatedBook.setTitle("Nirmala");

		Mockito.when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));
		Mockito.when(bookRepository.updateBookById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(Optional.of(updatedBook));

		// When
		Book result = bookService.updateBookById(updatedBook, bookId);

		// Then
		assertNotNull(result);
		assertEquals("Premchandra Updated", result.getAuthor());
		assertEquals("Nirmala", result.getTitle());
	}

	@Test
	void testDeleteUser() {
		// Given
		String bookId = "existing-book-id";
		Book existingBook = new Book();
		existingBook.setId(bookId);

		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.of(existingBook));
		// When
		boolean isDeleted = bookService.deleteBookById(bookId);

		// Then
		assertTrue(isDeleted);
		Mockito.verify(bookRepository).deleteBookById(bookId);
	}


}
