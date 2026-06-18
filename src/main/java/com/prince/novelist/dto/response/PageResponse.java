package com.prince.novelist.dto.response;

import java.util.List;

/**
 * Generic response DTO for paginated results.
 * @param <T> The type of content in the page
 */
public class PageResponse<T> {
    
    private List<T> content;
    private PaginationMetadata pagination;

    public PageResponse() {
    }

    public PageResponse(List<T> content, PaginationMetadata pagination) {
        this.content = content;
        this.pagination = pagination;
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMetadata pagination) {
        this.pagination = pagination;
    }
}

// Made with Bob
