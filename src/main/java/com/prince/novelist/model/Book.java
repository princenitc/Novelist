package com.prince.novelist.model;

import com.prince.novelist.repository.BookRepository;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class Book {
	@Id
	private String bookId;
	private String title;
	private String author;

	public Book() {
	}
	public Book(String title, String author) {
		this.author = author;
		this.title = title;
	}

	public String getId() {
		return bookId;
	}

	public void setId(String bookId) {
		this.bookId = bookId;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}
}
