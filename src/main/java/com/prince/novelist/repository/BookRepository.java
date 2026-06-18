package com.prince.novelist.repository;

import com.prince.novelist.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import java.util.Collection;
import java.util.Optional;

public interface BookRepository extends Neo4jRepository<Book, String> {

	@Query("CREATE (book : Book {title : $title, author: $author, bookId: $bookId}) RETURN book")
	Optional<Book> addBook(String title, String author, String bookId);

	@Query("MATCH(b: Book) return b")
	Collection<Book> getAllBooks();
	
	/**
	 * Find all books with pagination support.
	 * Uses Spring Data's built-in pagination.
	 */
	Page<Book> findAll(Pageable pageable);

	@Query("MATCH(b: Book{bookId:$id}) RETURN b")
	Optional<Book> getBookById(String id);

	@Query("MATCH (b:Book {bookId: $id}) SET b.title=$title, b.author=$author RETURN b")
	Optional<Book> updateBookById(String id, String title, String author);

	@Query("MATCH(b: Book {bookId:$id}) DELETE b")
	void deleteBookById(String id);
	
	/**
	 * Find books by title containing the search term (case-insensitive).
	 */
	@Query(value = "MATCH (b:Book) WHERE toLower(b.title) CONTAINS toLower($searchTerm) RETURN b",
	       countQuery = "MATCH (b:Book) WHERE toLower(b.title) CONTAINS toLower($searchTerm) RETURN count(b)")
	Page<Book> findByTitleContaining(String searchTerm, Pageable pageable);
	
	/**
	 * Find books by author containing the search term (case-insensitive).
	 */
	@Query(value = "MATCH (b:Book) WHERE toLower(b.author) CONTAINS toLower($searchTerm) RETURN b",
	       countQuery = "MATCH (b:Book) WHERE toLower(b.author) CONTAINS toLower($searchTerm) RETURN count(b)")
	Page<Book> findByAuthorContaining(String searchTerm, Pageable pageable);
	
	/**
	 * Check if a book exists by ISBN.
	 */
	@Query("MATCH (b:Book {isbn: $isbn}) RETURN count(b) > 0")
	boolean existsByIsbn(String isbn);

}
