package com.prince.novelist.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Results in HTTP 409 Conflict status.
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DuplicateResourceException(String resourceType, String identifier) {
        super(String.format("%s with identifier '%s' already exists", resourceType, identifier));
    }
}

// Made with Bob
