package com.prince.novelist.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node
public class User {
	@Id
	private String userId ;
	private String name;
	private Long age;

	@Relationship(type = "RATED", direction = Relationship.Direction.INCOMING)
	private List<Book> books;
	public List<Book> getBooks() {
		return books;
	}
	public User() {
	}

	public void setId(String userId) {
		this.userId = userId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAge(Long age) {
		this.age = age;
	}

	public String getId() {
		return this.userId;
	}

	public String getName() {
		return name;
	}

	public Long getAge() {
		return age;
	}
}
