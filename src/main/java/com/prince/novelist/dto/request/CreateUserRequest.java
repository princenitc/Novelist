package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new user.
 */
public class CreateUserRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be a positive number")
    @Max(value = 150, message = "Age must be realistic")
    private Long age;
    
    private UserPreferencesRequest preferences;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String name, String email, Long age, UserPreferencesRequest preferences) {
        this.name = name;
        this.email = email;
        this.age = age;
        this.preferences = preferences;
    }

    // Getters and Setters
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

    public UserPreferencesRequest getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferencesRequest preferences) {
        this.preferences = preferences;
    }
}

// Made with Bob
