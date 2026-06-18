package com.prince.novelist.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.Instant;
import java.util.Objects;

@RelationshipProperties
public class RatingRelation {
	
	@RelationshipId
	@GeneratedValue
	private Long id;
	
	@NotNull(message = "Rating is required")
	@Min(value = 1, message = "Rating must be between 1 and 5")
	@Max(value = 5, message = "Rating must be between 1 and 5")
	private Integer rating;
	
	@Size(max = 1000, message = "Review must not exceed 1000 characters")
	private String review;
	
	@CreatedDate
	private Instant timestamp;
	
	private Integer helpful;
	
	@TargetNode
	private Book book;
	
	public RatingRelation() {
	}
	
	public RatingRelation(Book book, Integer rating) {
		this.rating = rating;
		this.book = book;
		this.timestamp = Instant.now();
	}
	
	public RatingRelation(Book book, Integer rating, String review) {
		this.rating = rating;
		this.book = book;
		this.review = review;
		this.timestamp = Instant.now();
	}
	
	// Getters and Setters
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Integer getRating() {
		return rating;
	}
	
	public void setRating(Integer rating) {
		this.rating = rating;
	}
	
	public String getReview() {
		return review;
	}
	
	public void setReview(String review) {
		this.review = review;
	}
	
	public Instant getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
	
	public Integer getHelpful() {
		return helpful;
	}
	
	public void setHelpful(Integer helpful) {
		this.helpful = helpful;
	}
	
	public Book getBook() {
		return book;
	}
	
	public void setBook(Book book) {
		this.book = book;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RatingRelation that = (RatingRelation) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RatingRelation{" +
				"id=" + id +
				", rating=" + rating +
				", review='" + review + '\'' +
				", timestamp=" + timestamp +
				", helpful=" + helpful +
				'}';
	}
}

