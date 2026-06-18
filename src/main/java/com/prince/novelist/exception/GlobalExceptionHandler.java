package com.prince.novelist.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			ex.getMessage(),
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			ex.getMessage(),
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			ex.getMessage(),
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequestException(InvalidRequestException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			ex.getMessage(),
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> errors = new HashMap<>();
		
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("message", "Validation failed");
		response.put("errors", errors);
		response.put("timestamp", LocalDateTime.now());
		
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> errors = new HashMap<>();
		
		ex.getConstraintViolations().forEach(violation -> {
			String propertyPath = violation.getPropertyPath().toString();
			String message = violation.getMessage();
			errors.put(propertyPath, message);
		});
		
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("message", "Constraint violation");
		response.put("errors", errors);
		response.put("timestamp", LocalDateTime.now());
		
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		String message = "Data integrity violation";
		
		// Try to extract more specific message
		if (ex.getCause() != null && ex.getCause().getMessage() != null) {
			String causeMessage = ex.getCause().getMessage().toLowerCase();
			if (causeMessage.contains("unique") || causeMessage.contains("duplicate")) {
				message = "A record with this value already exists";
			} else if (causeMessage.contains("foreign key") || causeMessage.contains("constraint")) {
				message = "Cannot perform operation due to data constraints";
			}
		}
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			message,
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
		String message = "Malformed JSON request";
		
		if (ex.getCause() != null) {
			message = "Invalid request format: " + ex.getCause().getMessage();
		}
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			message,
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
			HttpRequestMethodNotSupportedException ex) {
		String message = String.format("Request method '%s' not supported. Supported methods are: %s",
			ex.getMethod(),
			ex.getSupportedHttpMethods() != null ?
				ex.getSupportedHttpMethods().stream()
					.map(Object::toString)
					.collect(Collectors.joining(", ")) : "");
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.METHOD_NOT_ALLOWED.value(),
			message,
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
			MissingServletRequestParameterException ex) {
		String message = String.format("Required parameter '%s' of type '%s' is missing",
			ex.getParameterName(), ex.getParameterType());
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			message,
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
			MethodArgumentTypeMismatchException ex) {
		String message = String.format("Parameter '%s' should be of type '%s'",
			ex.getName(),
			ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			message,
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
		// Log the exception for debugging
		ex.printStackTrace();
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			"An unexpected error occurred. Please try again later.",
			LocalDateTime.now()
		);
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	// Error response DTO
	public static class ErrorResponse {
		private int status;
		private String message;
		private LocalDateTime timestamp;

		public ErrorResponse(int status, String message, LocalDateTime timestamp) {
			this.status = status;
			this.message = message;
			this.timestamp = timestamp;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
		}
	}
}

// Made with Bob
