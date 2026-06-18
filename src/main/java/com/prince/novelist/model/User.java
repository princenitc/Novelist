package com.prince.novelist.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Node
public class User {
	@Id
	private String userId;
	
	@NotBlank(message = "Name is required")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	private String name;
	
	@Email(message = "Email must be valid")
	private String email;
	
	@NotNull(message = "Age is required")
	@Min(value = 0, message = "Age must be a positive number")
	private Long age;
	
	@CreatedDate
	private Instant createdAt;
	
	@LastModifiedDate
	private Instant updatedAt;
	
	private Map<String, Object> preferences;

	@Relationship(type = "RATED", direction = Relationship.Direction.OUTGOING)
	private List<RatingRelation> ratedBooks = new ArrayList<>();

	public User() {
	}

	// Getters and Setters
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getAge() {
		return age;
	}

	public void setAge(Long age) {
		this.age = age;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Map<String, Object> getPreferences() {
		return preferences;
	}

	public void setPreferences(Map<String, Object> preferences) {
		this.preferences = preferences;
	}

	public List<RatingRelation> getRatedBooks() {
		return ratedBooks;
	}

	public void setRatedBooks(List<RatingRelation> ratedBooks) {
		this.ratedBooks = ratedBooks;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(userId, user.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId);
	}

	@Override
	public String toString() {
		return "User{" +
				"userId='" + userId + '\'' +
				", name='" + name + '\'' +
				", email='" + email + '\'' +
				", age=" + age +
				", createdAt=" + createdAt +
				", updatedAt=" + updatedAt +
				'}';
	}
}
