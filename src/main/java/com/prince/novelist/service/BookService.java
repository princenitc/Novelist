package com.prince.novelist.service;

import com.prince.novelist.dto.request.CreateBookRequest;
import com.prince.novelist.dto.request.UpdateBookRequest;
import com.prince.novelist.dto.response.BookResponse;
import com.prince.novelist.dto.response.PageResponse;
import com.prince.novelist.exception.BadRequestException;
import com.prince.novelist.exception.DuplicateResourceException;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.mapper.BookMapper;
import com.prince.novelist.model.Book;
import com.prince.novelist.repository.BookRepository;
import com.prince.novelist.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BookService {

	private static final Logger log = LoggerFactory.getLogger(BookService.class);

	private final BookRepository bookRepository;
	private final BookMapper bookMapper;

	public BookService(BookRepository bookRepository, BookMapper bookMapper) {
		this.bookRepository = bookRepository;
		this.bookMapper = bookMapper;
	}

	public PageResponse<BookResponse> getAllBooks(Pageable pageable) {
		log.debug("Fetching all books with pagination: page={}, size={}, sort={}",
			pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

		Page<Book> booksPage = bookRepository.findAll(pageable);
		return PaginationUtil.createPageResponse(booksPage, bookMapper::toResponse);
	}

	public BookResponse getBookById(String bookId) {
		validateBookId(bookId);
		log.debug("Fetching book by id: {}", bookId);

		Book book = findBookById(bookId);
		return bookMapper.toResponse(book);
	}

	@Transactional
	public BookResponse createBook(CreateBookRequest request) {
		if (request == null) {
			throw new BadRequestException("Create book request must not be null");
		}

		log.info("Creating book with title='{}', author='{}', isbn='{}'",
			request.getTitle(), request.getAuthor(), request.getIsbn());

		validateDuplicateIsbn(request.getIsbn(), null);

		Book book = bookMapper.toEntity(request);
		Book savedBook = bookRepository.save(book);

		log.info("Created book with id={}", savedBook.getBookId());
		return bookMapper.toResponse(savedBook);
	}

	@Transactional
	public BookResponse updateBook(String bookId, UpdateBookRequest request) {
		validateBookId(bookId);

		if (request == null) {
			throw new BadRequestException("Update book request must not be null");
		}

		log.info("Updating book with id={}", bookId);

		Book existingBook = findBookById(bookId);
		validateDuplicateIsbn(request.getIsbn(), existingBook.getIsbn());

		bookMapper.updateEntity(existingBook, request);
		Book updatedBook = bookRepository.save(existingBook);

		log.info("Updated book with id={}", bookId);
		return bookMapper.toResponse(updatedBook);
	}

	@Transactional
	public void deleteBook(String bookId) {
		validateBookId(bookId);
		log.info("Deleting book with id={}", bookId);

		Book existingBook = findBookById(bookId);
		bookRepository.delete(existingBook);

		log.info("Deleted book with id={}", bookId);
	}

	public PageResponse<BookResponse> searchBooks(String query, Pageable pageable) {
		if (query == null || query.trim().isEmpty()) {
			throw new BadRequestException("Search query must not be blank");
		}

		String normalizedQuery = query.trim();
		log.debug("Searching books with query='{}', page={}, size={}, sort={}",
			normalizedQuery, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

		Page<Book> booksPage = bookRepository.findByTitleContaining(normalizedQuery, pageable);
		return PaginationUtil.createPageResponse(booksPage, bookMapper::toResponse);
	}

	private Book findBookById(String bookId) {
		Optional<Book> book = bookRepository.getBookById(bookId);
		return book.orElseThrow(() -> {
			log.warn("Book not found with id={}", bookId);
			return new ResourceNotFoundException("Book not found with id: " + bookId);
		});
	}

	private void validateBookId(String bookId) {
		if (bookId == null || bookId.trim().isEmpty()) {
			throw new BadRequestException("Book id must not be blank");
		}
	}

	private void validateDuplicateIsbn(String isbn, String currentIsbn) {
		if (isbn == null || isbn.trim().isEmpty()) {
			return;
		}

		String normalizedIsbn = isbn.trim();
		if (normalizedIsbn.equals(currentIsbn)) {
			return;
		}

		if (bookRepository.existsByIsbn(normalizedIsbn)) {
			log.warn("Duplicate ISBN detected: {}", normalizedIsbn);
			throw new DuplicateResourceException("Book", normalizedIsbn);
		}
	}
}
