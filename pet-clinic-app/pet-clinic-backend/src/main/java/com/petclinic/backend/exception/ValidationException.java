package com.petclinic.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when input validation fails
 * Maps to HTTP 400 Bad Request status
 */
public class ValidationException extends PetClinicException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    public ValidationException(String field, String value, String reason) {
        super(String.format("Invalid value '%s' for field '%s': %s", value, field, reason), 
              "VALIDATION_ERROR", 
              HttpStatus.BAD_REQUEST);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}