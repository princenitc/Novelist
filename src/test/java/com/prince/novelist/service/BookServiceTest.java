package com.prince.novelist.service;

import com.prince.novelist.exception.InvalidRequestException;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.model.Book;
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
	void shouldCreateBook() {
		// Given
		Book newBook = new Book();
		newBook.setTitle("Karmabhoomi");
		newBook.setBookId("1");
		newBook.setAuthor("Munshi Premchandra");

		Mockito.when(bookRepository.addBook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(Optional.of(newBook));

		// When
		Book createdBook = bookService.createBook(newBook);

		// Then
		assertNotNull(createdBook);
		assertEquals("Karmabhoomi", createdBook.getTitle());
		assertEquals("1", createdBook.getBookId());
		assertEquals("Munshi Premchandra", createdBook.getAuthor());
	}

	@Test
	void shouldThrowInvalidRequestExceptionWhenBookCreationFails() {
		// Given
		Book newBook = new Book();
		newBook.setTitle("Test Book");
		newBook.setAuthor("Test Author");

		Mockito.when(bookRepository.addBook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(Optional.empty());

		// When & Then
		assertThrows(InvalidRequestException.class, () -> bookService.createBook(newBook));
	}

	@Test
	void shouldFindBookById() {
		// Given
		String bookId = "existing-book-id";
		Book existingBook = new Book();
		existingBook.setBookId(bookId);
		existingBook.setAuthor("Premchandra");
		existingBook.setTitle("Prema");

		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.of(existingBook));

		// When
		Book foundBook = bookService.getBookById(bookId);

		// Then
		assertNotNull(foundBook);
		assertEquals(bookId, foundBook.getBookId());
		assertEquals("Premchandra", foundBook.getAuthor());
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenBookNotFound() {
		// Given
		String bookId = "non-existent-id";
		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(bookId));
	}

	@Test
	void shouldUpdateBook() {
		// Given
		String bookId = "existing-book-id";
		Book existingBook = new Book();
		existingBook.setBookId(bookId);
		existingBook.setTitle("Nirmala");
		existingBook.setAuthor("Premchandra");

		Book updatedBook = new Book();
		updatedBook.setAuthor("Premchandra Updated");
		updatedBook.setTitle("Nirmala");

		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.of(existingBook));
		Mockito.when(bookRepository.updateBookById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(Optional.of(updatedBook));

		// When
		Book result = bookService.updateBook(updatedBook, bookId);

		// Then
		assertNotNull(result);
		assertEquals("Premchandra Updated", result.getAuthor());
		assertEquals("Nirmala", result.getTitle());
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentBook() {
		// Given
		String bookId = "non-existent-id";
		Book updatedBook = new Book();
		updatedBook.setTitle("Test");
		updatedBook.setAuthor("Test Author");

		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(updatedBook, bookId));
	}

	@Test
	void shouldDeleteBook() {
		// Given
		String bookId = "existing-book-id";
		Book existingBook = new Book();
		existingBook.setBookId(bookId);

		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.of(existingBook));

		// When
		bookService.deleteBook(bookId);

		// Then
		Mockito.verify(bookRepository).deleteBookById(bookId);
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentBook() {
		// Given
		String bookId = "non-existent-id";
		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(bookId));
	}
}
