package com.prince.novelist.repository;

import com.prince.novelist.model.Book;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import java.util.Collection;
import java.util.Optional;

public interface BookRepository extends Neo4jRepository<Book, String> {

	@Query("CREATE (book : Book {title : $title, author: $author, bookId: $bookId}) RETURN book")
	Optional<Book> addBook(String title, String author,String bookId);

	@Query("MATCH(b: Book) return b")
	Collection<Book> getAllBooks();

	@Query("MATCH(b: Book{bookId:$id}) RETURN b")
	Optional<Book> getBookById(String id);

	@Query("MATCH (b:Book {bookId: $id}) SET b.title=$title, b.author=$author RETURN b")
	Optional<Book> updateBookById(String id, String title, String author);

	@Query("MATCH(b: Book {bookId:$id}) DELETE b")
	void deleteBookById(String id);

}
