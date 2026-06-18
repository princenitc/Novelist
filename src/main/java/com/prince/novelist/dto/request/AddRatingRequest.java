package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for adding a book rating.
 */
public class AddRatingRequest {
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;
    
    @Size(max = 1000, message = "Review must not exceed 1000 characters")
    private String review;

    public AddRatingRequest() {
    }

    public AddRatingRequest(Integer rating, String review) {
        this.rating = rating;
        this.review = review;
    }

    // Getters and Setters
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
}

// Made with Bob
