package com.prince.novelist.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node
public class User {
	@Id
	private String userId;
	private String name;
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
