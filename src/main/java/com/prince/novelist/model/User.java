package com.prince.novelist.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node
public class User {
	@Id
	private String userId;
	
	@NotBlank(message = "Name is required")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	private String name;
	
	@NotNull(message = "Age is required")
	@Min(value = 0, message = "Age must be a positive number")
	private Long age;

	@Relationship(type = "RATED", direction = Relationship.Direction.OUTGOING)
	private List<RatingRelation> ratedBooks = new ArrayList<>();

	public User() {
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getAge() {
		return age;
	}

	public void setAge(Long age) {
		this.age = age;
	}

	public List<RatingRelation> getRatedBooks() {
		return ratedBooks;
	}

	public void setRatedBooks(List<RatingRelation> ratedBooks) {
		this.ratedBooks = ratedBooks;
	}
}
