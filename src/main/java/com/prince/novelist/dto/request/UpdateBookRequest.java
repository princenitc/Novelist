package com.prince.novelist.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Request DTO for updating an existing book.
 * All fields are optional - only provided fields will be updated.
 */
public class UpdateBookRequest {
    
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @Size(min = 1, max = 100, message = "Author must be between 1 and 100 characters")
    private String author;
    
    @Pattern(regexp = "^[0-9X-]{10,17}$",
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
    
    @Pattern(regexp = "^https?://.+", message = "Cover image URL must be valid")
    private String coverImageUrl;
    
    private List<String> genres;

    public UpdateBookRequest() {
    }

    public UpdateBookRequest(String title, String author, String isbn, Integer publishedYear,
                           String description, String content, String language,
                           Integer pageCount, String coverImageUrl, List<String> genres) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.description = description;
        this.content = content;
        this.language = language;
        this.pageCount = pageCount;
        this.coverImageUrl = coverImageUrl;
        this.genres = genres;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
}

// Made with Bob
