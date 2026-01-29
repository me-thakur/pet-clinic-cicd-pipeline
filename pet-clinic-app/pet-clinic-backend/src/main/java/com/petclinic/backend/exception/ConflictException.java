package com.petclinic.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a resource conflict occurs
 * Maps to HTTP 409 Conflict status
 */
public class ConflictException extends PetClinicException {
    
    public ConflictException(String message) {
        super(message, "RESOURCE_CONFLICT", HttpStatus.CONFLICT);
    }
    
    public ConflictException(String resource, String conflictReason) {
        super(String.format("Conflict with %s: %s", resource, conflictReason), 
              "RESOURCE_CONFLICT", 
              HttpStatus.CONFLICT);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, "RESOURCE_CONFLICT", HttpStatus.CONFLICT, cause);
    }
}