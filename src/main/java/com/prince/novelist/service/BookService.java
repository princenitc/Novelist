package com.prince.novelist.service;

import com.prince.novelist.exception.InvalidRequestException;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.model.Book;
import com.prince.novelist.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {

	@Autowired
	BookRepository bookRepository;

	public Book createBook(Book book) {
		String title = book.getTitle();
		String author = book.getAuthor();
		String bookId = UUID.randomUUID().toString();
		Optional<Book> createdBook = bookRepository.addBook(title, author, bookId);
		return createdBook.orElseThrow(() ->
			new InvalidRequestException("Failed to create book with provided data"));
	}

	public Collection<Book> getAllBooks() {
		return bookRepository.getAllBooks();
	}

	public Book getBookById(String id) {
		Optional<Book> book = bookRepository.getBookById(id);
		return book.orElseThrow(() ->
			new ResourceNotFoundException("Book not found with id: " + id));
	}

	public Book updateBook(Book book, String id) {
		String title = book.getTitle();
		String author = book.getAuthor();
		
		// Check if book exists first
		if (!bookRepository.getBookById(id).isPresent()) {
			throw new ResourceNotFoundException("Book not found with id: " + id);
		}
		
		Optional<Book> updatedBook = bookRepository.updateBookById(id, title, author);
		return updatedBook.orElseThrow(() ->
			new InvalidRequestException("Failed to update book with provided data"));
	}

	public void deleteBook(String id) {
		if (!bookRepository.getBookById(id).isPresent()) {
			throw new ResourceNotFoundException("Book not found with id: " + id);
		}
		bookRepository.deleteBookById(id);
	}
}
