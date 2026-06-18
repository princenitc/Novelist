package com.prince.novelist.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for user information.
 */
public class UserResponse {
    
    private String userId;
    private String name;
    private String email;
    private Long age;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RatingResponse> ratedBooks;
    private UserPreferencesResponse preferences;

    public UserResponse() {
    }

    public UserResponse(String userId, String name, String email, Long age, Instant createdAt,
                       Instant updatedAt, List<RatingResponse> ratedBooks, UserPreferencesResponse preferences) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.age = age;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.ratedBooks = ratedBooks;
        this.preferences = preferences;
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

    public List<RatingResponse> getRatedBooks() {
        return ratedBooks;
    }

    public void setRatedBooks(List<RatingResponse> ratedBooks) {
        this.ratedBooks = ratedBooks;
    }

    public UserPreferencesResponse getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferencesResponse preferences) {
        this.preferences = preferences;
    }
}

// Made with Bob
