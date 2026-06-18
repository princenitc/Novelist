package com.prince.novelist.dto.response;

import java.util.List;

/**
 * Response DTO for user preferences.
 */
public class UserPreferencesResponse {
    
    private List<String> favoriteGenres;
    private List<String> favoriteAuthors;
    private Integer annualReadingGoal;
    private Boolean emailNotifications;
    private Boolean recommendationNotifications;

    public UserPreferencesResponse() {
    }

    public UserPreferencesResponse(List<String> favoriteGenres, List<String> favoriteAuthors,
                                  Integer annualReadingGoal, Boolean emailNotifications,
                                  Boolean recommendationNotifications) {
        this.favoriteGenres = favoriteGenres;
        this.favoriteAuthors = favoriteAuthors;
        this.annualReadingGoal = annualReadingGoal;
        this.emailNotifications = emailNotifications;
        this.recommendationNotifications = recommendationNotifications;
    }

    // Getters and Setters
    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public List<String> getFavoriteAuthors() {
        return favoriteAuthors;
    }

    public void setFavoriteAuthors(List<String> favoriteAuthors) {
        this.favoriteAuthors = favoriteAuthors;
    }

    public Integer getAnnualReadingGoal() {
        return annualReadingGoal;
    }

    public void setAnnualReadingGoal(Integer annualReadingGoal) {
        this.annualReadingGoal = annualReadingGoal;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getRecommendationNotifications() {
        return recommendationNotifications;
    }

    public void setRecommendationNotifications(Boolean recommendationNotifications) {
        this.recommendationNotifications = recommendationNotifications;
    }
}

// Made with Bob
