package com.prince.novelist.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class Book {
	@Id
	private String bookId;
	
	@NotBlank(message = "Title is required")
	@Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
	private String title;
	
	@NotBlank(message = "Author is required")
	@Size(min = 1, max = 100, message = "Author must be between 1 and 100 characters")
	private String author;

	public Book() {
	}
	
	public Book(String title, String author) {
		this.author = author;
		this.title = title;
	}

	public String getBookId() {
		return bookId;
	}

	public void setBookId(String bookId) {
		this.bookId = bookId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}
}
