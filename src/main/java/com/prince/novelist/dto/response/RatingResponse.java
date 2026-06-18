package com.prince.novelist.dto.response;

import java.time.Instant;

/**
 * Response DTO for rating information.
 */
public class RatingResponse {
    
    private BookResponse book;
    private Integer rating;
    private String review;
    private Instant timestamp;
    private Integer helpfulCount;

    public RatingResponse() {
    }

    public RatingResponse(BookResponse book, Integer rating, String review, Instant timestamp, Integer helpfulCount) {
        this.book = book;
        this.rating = rating;
        this.review = review;
        this.timestamp = timestamp;
        this.helpfulCount = helpfulCount;
    }

    // Getters and Setters
    public BookResponse getBook() {
        return book;
    }

    public void setBook(BookResponse book) {
        this.book = book;
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

    public Integer getHelpfulCount() {
        return helpfulCount;
    }

    public void setHelpfulCount(Integer helpfulCount) {
        this.helpfulCount = helpfulCount;
    }
}

// Made with Bob
