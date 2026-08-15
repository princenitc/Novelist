# DTO Design Specification

## Table of Contents
- [Overview](#overview)
- [Design Principles](#design-principles)
- [Request DTOs](#request-dtos)
- [Response DTOs](#response-dtos)
- [Event DTOs](#event-dtos)
- [Mapping Strategy](#mapping-strategy)
- [Validation Rules](#validation-rules)
- [Best Practices](#best-practices)

## Overview

Data Transfer Objects (DTOs) provide a clean separation between the API layer and domain model. This document defines all DTOs used in the Novelist application for requests, responses, and event messaging.

### Benefits of DTOs

1. **Decoupling**: API contracts independent of domain model
2. **Security**: Control what data is exposed
3. **Versioning**: Support multiple API versions
4. **Validation**: Centralized input validation
5. **Documentation**: Clear API contracts

## Design Principles

1. **Immutability**: Use immutable DTOs where possible
2. **Validation**: Validate at DTO level, not domain level
3. **Null Safety**: Use Optional for nullable fields
4. **Clear Naming**: Descriptive names (CreateBookRequest, BookResponse)
5. **Flat Structure**: Avoid deep nesting
6. **Documentation**: JavaDoc for all public DTOs

## Request DTOs

### Book Request DTOs

#### CreateBookRequest

```java
package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new book.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @NotBlank(message = "Author is required")
    @Size(min = 1, max = 100, message = "Author must be between 1 and 100 characters")
    private String author;
    
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
            message = "Invalid ISBN format")
    private String isbn;
    
    @Min(value = 1000, message = "Published year must be after 1000")
    @Max(value = 2100, message = "Published year must be before 2100")
    private Integer publishedYear;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Size(max = 1000000, message = "Content must not exceed 1,000,000 characters")
    private String content;
    
    @Pattern(regexp = "^[a-z]{2}$", message = "Language must be ISO 639-1 code (e.g., 'en', 'es')")
    private String language;
    
    @Min(value = 1, message = "Page count must be positive")
    private Integer pageCount;
    
    @URL(message = "Cover image URL must be valid")
    private String coverImageUrl;
    
    private List<String> genres;
}
```

#### UpdateBookRequest

```java
package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing book.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequest {
    
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @Size(min = 1, max = 100, message = "Author must be between 1 and 100 characters")
    private String author;
    
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
            message = "Invalid ISBN format")
    private String isbn;
    
    @Min(value = 1000, message = "Published year must be after 1000")
    @Max(value = 2100, message = "Published year must be before 2100")
    private Integer publishedYear;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Size(max = 1000000, message = "Content must not exceed 1,000,000 characters")
    private String content;
    
    @Pattern(regexp = "^[a-z]{2}$", message = "Language must be ISO 639-1 code")
    private String language;
    
    @Min(value = 1, message = "Page count must be positive")
    private Integer pageCount;
    
    @URL(message = "Cover image URL must be valid")
    private String coverImageUrl;
    
    private List<String> genres;
}
```

---

### User Request DTOs

#### CreateUserRequest

```java
package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
```

#### UpdateUserRequest

```java
package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Min(value = 0, message = "Age must be a positive number")
    @Max(value = 150, message = "Age must be realistic")
    private Long age;
    
    private UserPreferencesRequest preferences;
}
```

#### UserPreferencesRequest

```java
package com.prince.novelist.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for user preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {
    
    private List<String> favoriteGenres;
    private List<String> favoriteAuthors;
    
    @Min(value = 1, message = "Reading goal must be at least 1")
    private Integer annualReadingGoal;
    
    private Boolean emailNotifications;
    private Boolean recommendationNotifications;
}
```

---

### Rating Request DTOs

#### AddRatingRequest

```java
package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a book rating.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRatingRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Book ID is required")
    private String bookId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;
    
    @Size(max = 1000, message = "Review must not exceed 1000 characters")
    private String review;
}
```

---

### Search Request DTOs

#### SearchBooksRequest

```java
package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for semantic book search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchBooksRequest {
    
    @NotBlank(message = "Search query is required")
    @Size(min = 1, max = 500, message = "Query must be between 1 and 500 characters")
    private String query;
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 50, message = "Limit must not exceed 50")
    private Integer limit = 10;
    
    @Min(value = 0, message = "Minimum score must be between 0 and 1")
    @Max(value = 1, message = "Minimum score must be between 0 and 1")
    private Double minScore = 0.0;
    
    private List<String> genres;
    
    @Min(value = 0, message = "Minimum rating must be between 0 and 5")
    @Max(value = 5, message = "Minimum rating must be between 0 and 5")
    private Double minRating;
    
    @Min(value = 1000, message = "Published after must be after 1000")
    private Integer publishedAfter;
    
    private String language;
}
```

---

## Response DTOs

### Book Response DTOs

#### BookResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for book information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    
    private String bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;
    private String description;
    private String language;
    private Integer pageCount;
    private String coverImageUrl;
    private List<String> genres;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Optional fields for enriched responses
    private Double averageRating;
    private Integer totalRatings;
    private Boolean hasEmbedding;
}
```

#### BookDetailResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Detailed response DTO for book information including statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailResponse {
    
    private String bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;
    private String description;
    private String language;
    private Integer pageCount;
    private String coverImageUrl;
    private List<String> genres;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Statistics
    private BookStatisticsResponse statistics;
    
    // Similar books
    private List<SimilarBookResponse> similarBooks;
}
```

#### BookStatisticsResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for book statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookStatisticsResponse {
    
    private Integer totalRatings;
    private Double averageRating;
    private Map<Integer, Integer> ratingDistribution; // rating -> count
    private Integer totalReviews;
    private Integer readCount;
    private Integer wishlistCount;
    private Double popularityScore;
}
```

#### SimilarBookResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for similar book information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarBookResponse {
    
    private String bookId;
    private String title;
    private String author;
    private Double similarityScore;
    private String similarityReason;
    private Double averageRating;
}
```

---

### User Response DTOs

#### UserResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private String userId;
    private String name;
    private String email;
    private Long age;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RatingResponse> ratedBooks;
    private UserPreferencesResponse preferences;
}
```

#### UserPreferencesResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for user preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {
    
    private List<String> favoriteGenres;
    private List<String> favoriteAuthors;
    private Integer annualReadingGoal;
    private Boolean emailNotifications;
    private Boolean recommendationNotifications;
}
```

---

### Rating Response DTOs

#### RatingResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for rating information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    
    private BookResponse book;
    private Integer rating;
    private String review;
    private Instant timestamp;
    private Integer helpfulCount;
}
```

---

### Search Response DTOs

#### SearchResultResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {
    
    private String queryId;
    private String query;
    private List<SearchResultItemResponse> results;
    private Integer totalResults;
    private Long processingTimeMs;
    private SearchMetadataResponse metadata;
}
```

#### SearchResultItemResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for individual search result item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItemResponse {
    
    private String bookId;
    private String title;
    private String author;
    private Double score;
    private String relevanceReason;
    private String excerpt;
    private List<String> genres;
    private Double averageRating;
    private Integer publishedYear;
}
```

#### SearchMetadataResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for search metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMetadataResponse {
    
    private Instant timestamp;
    private String searchType; // "semantic", "fulltext", "hybrid"
    private String model; // "text-embedding-ada-002"
    private Integer vectorDimensions;
}
```

---

### Recommendation Response DTOs

#### RecommendationResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for book recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    
    private String userId;
    private List<RecommendationItemResponse> recommendations;
    private Integer totalRecommendations;
    private String algorithm; // "collaborative", "content-based", "hybrid"
    private RecommendationBasisResponse basedOn;
    private Instant timestamp;
    private String model;
}
```

#### RecommendationItemResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for individual recommendation item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemResponse {
    
    private String bookId;
    private String title;
    private String author;
    private Double score;
    private String reason;
    private List<String> genres;
    private Double averageRating;
    private Double predictedRating;
}
```

#### RecommendationBasisResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for recommendation basis information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationBasisResponse {
    
    private Integer ratedBooks;
    private List<String> preferredGenres;
    private Integer similarUsers;
}
```

---

### Pagination Response

#### PageResponse

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic response DTO for paginated results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    private List<T> content;
    private PaginationMetadata pagination;
}
```

#### PaginationMetadata

```java
package com.prince.novelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMetadata {
    
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Boolean first;
    private Boolean last;
}
```

---

## Event DTOs

### Book Event DTOs

#### BookCreatedEvent

```java
package com.prince.novelist.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event DTO for book creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreatedEvent extends BaseEvent {
    
    private String bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;
    private String description;
    private String content; // Full text for embedding
    private String language;
    private Integer pageCount;
    private List<String> genres;
}
```

#### BookUpdatedEvent

```java
package com.prince.novelist.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Event DTO for book update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdatedEvent extends BaseEvent {
    
    private String bookId;
    private String title;
    private String author;
    private String content;
    private List<String> updatedFields;
    private Map<String, Object> previousValues;
}
```

#### BookDeletedEvent

```java
package com.prince.novelist.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event DTO for book deletion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDeletedEvent extends BaseEvent {
    
    private String bookId;
    private Instant deletedAt;
    private String reason;
}
```

---

### Embedding Event DTOs

#### EmbeddingGeneratedEvent

```java
package com.prince.novelist.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event DTO for successful embedding generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingGeneratedEvent extends BaseEvent {
    
    private String bookId;
    private String embeddingId;
    private String model;
    private Integer vectorDimensions;
    private Integer numChunks;
    private Long processingTimeMs;
    private String status; // "success"
    private String vectorStoreId;
}
```

#### EmbeddingFailedEvent

```java
package com.prince.novelist.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event DTO for failed embedding generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingFailedEvent extends BaseEvent {
    
    private String bookId;
    private String errorCode;
    private String errorMessage;
    private Boolean retryable;
    private Integer retryAfterSeconds;
}
```

---

### Base Event DTO

#### BaseEvent

```java
package com.prince.novelist.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Base event DTO with common fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    private String eventId;
    private String eventType;
    private Integer version;
    private Instant timestamp;
    private String correlationId;
    private String source;
    private Map<String, Object> metadata;
}
```

---

## Mapping Strategy

### MapStruct Configuration

```java
package com.prince.novelist.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface MapStructConfig {
}
```

### Book Mapper

```java
package com.prince.novelist.mapper;

import com.prince.novelist.dto.request.CreateBookRequest;
import com.prince.novelist.dto.request.UpdateBookRequest;
import com.prince.novelist.dto.response.BookResponse;
import com.prince.novelist.model.Book;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface BookMapper {
    
    /**
     * Map CreateBookRequest to Book entity.
     */
    @Mapping(target = "bookId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Book toEntity(CreateBookRequest request);
    
    /**
     * Map Book entity to BookResponse.
     */
    BookResponse toResponse(Book book);
    
    /**
     * Map list of Book entities to list of BookResponse.
     */
    List<BookResponse> toResponseList(List<Book> books);
    
    /**
     * Update Book entity from UpdateBookRequest.
     * Only updates non-null fields.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bookId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    void updateEntityFromRequest(UpdateBookRequest request, @MappingTarget Book book);
}
```

### User Mapper

```java
package com.prince.novelist.mapper;

import com.prince.novelist.dto.request.CreateUserRequest;
import com.prince.novelist.dto.request.UpdateUserRequest;
import com.prince.novelist.dto.response.UserResponse;
import com.prince.novelist.model.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = MapStructConfig.class, uses = {RatingMapper.class})
public interface UserMapper {
    
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ratedBooks", ignore = true)
    User toEntity(CreateUserRequest request);
    
    UserResponse toResponse(User user);
    
    List<UserResponse> toResponseList(List<User> users);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "ratedBooks", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
```

---

## Validation Rules

### Common Validation Annotations

```java
// Required fields
@NotNull
@NotBlank
@NotEmpty

// String constraints
@Size(min = 1, max = 100)
@Pattern(regexp = "...")

// Numeric constraints
@Min(value = 0)
@Max(value = 100)
@Positive
@PositiveOrZero

// Email and URL
@Email
@URL

// Custom validators
@ValidISBN
@ValidLanguageCode
```

### Custom Validators

#### ISBN Validator

```java
package com.prince.novelist.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ISBNValidator.class)
@Documented
public @interface ValidISBN {
    String message() default "Invalid ISBN format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
package com.prince.novelist.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {
    
    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        if (isbn == null || isbn.isBlank()) {
            return true; // Use @NotBlank for required validation
        }
        
        // Remove hyphens and spaces
        String cleanISBN = isbn.replaceAll("[- ]", "");
        
        // Validate ISBN-10 or ISBN-13
        return isValidISBN10(cleanISBN) || isValidISBN13(cleanISBN);
    }
    
    private boolean isValidISBN10(String isbn) {
        // Implementation
        return isbn.length() == 10; // Simplified
    }
    
    private boolean isValidISBN13(String isbn) {
        // Implementation
        return isbn.length() == 13; // Simplified
    }
}
```

---

## Best Practices

### 1. Use Builder Pattern

```java
BookResponse response = BookResponse.builder()
    .bookId(book.getBookId())
    .title(book.getTitle())
    .author(book.getAuthor())
    .build();
```

### 2. Validate at Controller Level

```java
@PostMapping
public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
    // Validation happens automatically
    Book book = bookService.createBook(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(bookMapper.toResponse(book));
}
```

### 3. Use Optional for Nullable Fields

```java
public class BookResponse {
    private String bookId; // Required
    private Optional<String> isbn; // Optional
    private Optional<Integer> publishedYear; // Optional
}
```

### 4. Document DTOs with JavaDoc

```java
/**
 * Request DTO for creating a new book.
 * 
 * @author Bob
 * @version 1.0
 * @since 1.0
 */
@Data
public class CreateBookRequest {
    // ...
}
```

### 5. Version DTOs

```java
// v1
package com.prince.novelist.dto.v1.request;
public class CreateBookRequest { }

// v2
package com.prince.novelist.dto.v2.request;
public class CreateBookRequest { }
```

### 6. Use Composition Over Inheritance

```java
// Good
public class BookDetailResponse {
    private BookResponse book;
    private BookStatisticsResponse statistics;
}

// Avoid
public class BookDetailResponse extends BookResponse {
    private BookStatisticsResponse statistics;
}
```

---

## References

- [MapStruct Documentation](https://mapstruct.org/)
- [Jakarta Bean Validation](https://beanvalidation.org/)
- [Spring Validation](https://docs.spring.io/spring-framework/reference/core/validation.html)
- [DTO Pattern](https://martinfowler.com/eaaCatalog/dataTransferObject.html)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-18  
**Author**: Bob (AI Assistant)  
**Status**: Draft