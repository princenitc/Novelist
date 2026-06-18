package com.prince.novelist.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for book information.
 */
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

    public BookResponse() {
    }

    public BookResponse(String bookId, String title, String author, String isbn, Integer publishedYear,
                       String description, String language, Integer pageCount, String coverImageUrl,
                       List<String> genres, Instant createdAt, Instant updatedAt, Double averageRating,
                       Integer totalRatings, Boolean hasEmbedding) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.description = description;
        this.language = language;
        this.pageCount = pageCount;
        this.coverImageUrl = coverImageUrl;
        this.genres = genres;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
        this.hasEmbedding = hasEmbedding;
    }

    // Getters and Setters
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

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

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Boolean getHasEmbedding() {
        return hasEmbedding;
    }

    public void setHasEmbedding(Boolean hasEmbedding) {
        this.hasEmbedding = hasEmbedding;
    }
}

// Made with Bob
