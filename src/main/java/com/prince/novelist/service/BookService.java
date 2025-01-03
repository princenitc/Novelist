package com.prince.novelist.service;

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

	public Book addBook(Book book) {
		String title = book.getTitle();
		String author = book.getAuthor();
		String bookId = UUID.randomUUID().toString();
		Optional<Book> createdBook =  bookRepository.addBook(title,author,bookId);
		return createdBook.orElseThrow(() -> new IllegalArgumentException("Invalid arguments"));
	}

	public Collection<Book> getAllBooks() {
		return bookRepository.getAllBooks();
	}

	public Book getBookById(String id) {
		Optional<Book> book = bookRepository.getBookById(id);
		return book.orElseThrow(()-> new IllegalArgumentException("Invalid arguments"));
	}

	public Book updateBookById(Book book, String id) throws Exception {
		String title = book.getTitle();
		String author = book.getAuthor();
		if(bookRepository.updateBookById(id, title, author).isPresent()) {
			return bookRepository.updateBookById(id,title,author).orElseThrow(()-> new IllegalArgumentException("Invalid arguments"));
		} else {
			throw new Exception("update error");
		}
	}

	public Boolean deleteBookById(String id) {
		if(bookRepository.getBookById(id).isPresent()) {
			bookRepository.deleteBookById(id);
			return true;
		}
		return false;
	}
}
