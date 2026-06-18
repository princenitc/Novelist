package com.prince.novelist.util;

import com.prince.novelist.dto.response.PageResponse;
import com.prince.novelist.dto.response.PaginationMetadata;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for pagination operations.
 */
public class PaginationUtil {

    private PaginationUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Create a PageResponse from a Spring Data Page.
     *
     * @param page The Spring Data Page
     * @param <T> The type of content
     * @return PageResponse with content and pagination metadata
     */
    public static <T> PageResponse<T> createPageResponse(Page<T> page) {
        PaginationMetadata metadata = createPaginationMetadata(page);
        return new PageResponse<>(page.getContent(), metadata);
    }

    /**
     * Create a PageResponse with mapped content.
     *
     * @param page The Spring Data Page
     * @param mapper Function to map from source type to target type
     * @param <S> Source type
     * @param <T> Target type
     * @return PageResponse with mapped content and pagination metadata
     */
    public static <S, T> PageResponse<T> createPageResponse(Page<S> page, Function<S, T> mapper) {
        List<T> mappedContent = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        
        PaginationMetadata metadata = createPaginationMetadata(page);
        return new PageResponse<>(mappedContent, metadata);
    }

    /**
     * Create pagination metadata from a Spring Data Page.
     *
     * @param page The Spring Data Page
     * @return PaginationMetadata
     */
    private static PaginationMetadata createPaginationMetadata(Page<?> page) {
        return new PaginationMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.isFirst(),
                page.isLast()
        );
    }
}

// Made with Bob
