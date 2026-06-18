package com.prince.novelist.exception;

/**
 * Exception thrown when a request is malformed or contains invalid data.
 * Results in HTTP 400 Bad Request status.
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Made with Bob
