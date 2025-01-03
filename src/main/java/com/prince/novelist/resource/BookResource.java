package com.prince.novelist.resource;

import com.prince.novelist.model.Book;
import com.prince.novelist.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping("/books")
public class BookResource {

	@Autowired
	BookService bookService;


	@PostMapping("/create")
	public Book addBook(@RequestBody Book book) {
		return bookService.addBook(book);
	}

	@GetMapping("/all")
	public Collection<Book> getAllBooks() {
		return bookService.getAllBooks();
	}


	@GetMapping("/{bookId}")
	public Book getBookById(@PathVariable("bookId") String bookId) {
		return bookService.getBookById(bookId);
	}

	@PutMapping("/update/{bookId}")
	public Book updateBook(@PathVariable("bookId") String bookId, @RequestBody Book book) throws Exception {
		return bookService.updateBookById(book,bookId);
	}

	@DeleteMapping("/delete/{bookId}")
	public boolean deleteBook(@PathVariable("bookId") String bookId) {
		return bookService.deleteBookById(bookId);
	}
}
