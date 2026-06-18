package com.prince.novelist.mapper;

import com.prince.novelist.dto.request.CreateBookRequest;
import com.prince.novelist.dto.request.UpdateBookRequest;
import com.prince.novelist.dto.response.BookResponse;
import com.prince.novelist.model.Book;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapper for converting between Book entities and DTOs.
 */
@Component
public class BookMapper {

    /**
     * Convert CreateBookRequest to Book entity.
     */
    public Book toEntity(CreateBookRequest request) {
        if (request == null) {
            return null;
        }

        Book book = new Book();
        book.setBookId(UUID.randomUUID().toString());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublishedYear(request.getPublishedYear());
        book.setDescription(request.getDescription());
        book.setContent(request.getContent());
        book.setLanguage(request.getLanguage());
        book.setPageCount(request.getPageCount());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setGenres(request.getGenres());
        book.setCreatedAt(Instant.now());
        book.setUpdatedAt(Instant.now());

        return book;
    }

    /**
     * Update Book entity from UpdateBookRequest.
     * Only updates non-null fields.
     */
    public void updateEntity(Book book, UpdateBookRequest request) {
        if (book == null || request == null) {
            return;
        }

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }
        if (request.getPublishedYear() != null) {
            book.setPublishedYear(request.getPublishedYear());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getContent() != null) {
            book.setContent(request.getContent());
        }
        if (request.getLanguage() != null) {
            book.setLanguage(request.getLanguage());
        }
        if (request.getPageCount() != null) {
            book.setPageCount(request.getPageCount());
        }
        if (request.getCoverImageUrl() != null) {
            book.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getGenres() != null) {
            book.setGenres(request.getGenres());
        }
        
        book.setUpdatedAt(Instant.now());
    }

    /**
     * Convert Book entity to BookResponse DTO.
     */
    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }

        BookResponse response = new BookResponse();
        response.setBookId(book.getBookId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setIsbn(book.getIsbn());
        response.setPublishedYear(book.getPublishedYear());
        response.setDescription(book.getDescription());
        response.setLanguage(book.getLanguage());
        response.setPageCount(book.getPageCount());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setGenres(book.getGenres());
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());
        
        // Optional fields - will be set by service layer if needed
        response.setAverageRating(book.getAverageRating());
        response.setTotalRatings(book.getTotalRatings());
        response.setHasEmbedding(false); // Default, can be updated by service

        return response;
    }
}

// Made with Bob
