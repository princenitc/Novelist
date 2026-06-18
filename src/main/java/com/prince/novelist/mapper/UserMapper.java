package com.prince.novelist.mapper;

import com.prince.novelist.dto.request.CreateUserRequest;
import com.prince.novelist.dto.request.UpdateUserRequest;
import com.prince.novelist.dto.request.UserPreferencesRequest;
import com.prince.novelist.dto.response.RatingResponse;
import com.prince.novelist.dto.response.UserPreferencesResponse;
import com.prince.novelist.dto.response.UserResponse;
import com.prince.novelist.model.RatingRelation;
import com.prince.novelist.model.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting between User entities and DTOs.
 */
@Component
public class UserMapper {

    private final BookMapper bookMapper;

    public UserMapper(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
    }

    /**
     * Convert CreateUserRequest to User entity.
     */
    public User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        
        if (request.getPreferences() != null) {
            user.setPreferences(toPreferencesMap(request.getPreferences()));
        }

        return user;
    }

    /**
     * Update User entity from UpdateUserRequest.
     * Only updates non-null fields.
     */
    public void updateEntity(User user, UpdateUserRequest request) {
        if (user == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getPreferences() != null) {
            user.setPreferences(toPreferencesMap(request.getPreferences()));
        }
        
        user.setUpdatedAt(Instant.now());
    }

    /**
     * Convert User entity to UserResponse DTO.
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setAge(user.getAge());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        // Convert rated books
        if (user.getRatedBooks() != null && !user.getRatedBooks().isEmpty()) {
            List<RatingResponse> ratingResponses = user.getRatedBooks().stream()
                    .map(this::toRatingResponse)
                    .collect(Collectors.toList());
            response.setRatedBooks(ratingResponses);
        }
        
        // Convert preferences
        if (user.getPreferences() != null) {
            response.setPreferences(toPreferencesResponse(user.getPreferences()));
        }

        return response;
    }

    /**
     * Convert UserPreferencesRequest to Map for storage.
     */
    private Map<String, Object> toPreferencesMap(UserPreferencesRequest request) {
        if (request == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        if (request.getFavoriteGenres() != null) {
            map.put("favoriteGenres", request.getFavoriteGenres());
        }
        if (request.getFavoriteAuthors() != null) {
            map.put("favoriteAuthors", request.getFavoriteAuthors());
        }
        if (request.getAnnualReadingGoal() != null) {
            map.put("annualReadingGoal", request.getAnnualReadingGoal());
        }
        if (request.getEmailNotifications() != null) {
            map.put("emailNotifications", request.getEmailNotifications());
        }
        if (request.getRecommendationNotifications() != null) {
            map.put("recommendationNotifications", request.getRecommendationNotifications());
        }
        return map;
    }

    /**
     * Convert preferences Map to UserPreferencesResponse.
     */
    @SuppressWarnings("unchecked")
    private UserPreferencesResponse toPreferencesResponse(Map<String, Object> preferences) {
        if (preferences == null) {
            return null;
        }

        UserPreferencesResponse response = new UserPreferencesResponse();
        
        if (preferences.containsKey("favoriteGenres")) {
            response.setFavoriteGenres((List<String>) preferences.get("favoriteGenres"));
        }
        if (preferences.containsKey("favoriteAuthors")) {
            response.setFavoriteAuthors((List<String>) preferences.get("favoriteAuthors"));
        }
        if (preferences.containsKey("annualReadingGoal")) {
            response.setAnnualReadingGoal((Integer) preferences.get("annualReadingGoal"));
        }
        if (preferences.containsKey("emailNotifications")) {
            response.setEmailNotifications((Boolean) preferences.get("emailNotifications"));
        }
        if (preferences.containsKey("recommendationNotifications")) {
            response.setRecommendationNotifications((Boolean) preferences.get("recommendationNotifications"));
        }
        
        return response;
    }

    /**
     * Convert RatingRelation to RatingResponse.
     */
    private RatingResponse toRatingResponse(RatingRelation relation) {
        if (relation == null) {
            return null;
        }

        RatingResponse response = new RatingResponse();
        response.setRating(relation.getRating());
        response.setReview(relation.getReview());
        response.setTimestamp(relation.getTimestamp());
        response.setHelpfulCount(relation.getHelpful());
        
        if (relation.getBook() != null) {
            response.setBook(bookMapper.toResponse(relation.getBook()));
        }
        
        return response;
    }
}

// Made with Bob
