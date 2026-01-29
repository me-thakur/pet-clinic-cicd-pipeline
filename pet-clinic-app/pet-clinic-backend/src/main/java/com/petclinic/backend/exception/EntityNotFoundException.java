package com.petclinic.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested entity is not found
 * Maps to HTTP 404 Not Found status
 */
public class EntityNotFoundException extends PetClinicException {
    
    public EntityNotFoundException(String entityType, Long id) {
        super(String.format("%s with ID %d not found", entityType, id), 
              "ENTITY_NOT_FOUND", 
              HttpStatus.NOT_FOUND);
    }
    
    public EntityNotFoundException(String entityType, String identifier) {
        super(String.format("%s with identifier '%s' not found", entityType, identifier), 
              "ENTITY_NOT_FOUND", 
              HttpStatus.NOT_FOUND);
    }
    
    public EntityNotFoundException(String message) {
        super(message, "ENTITY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}