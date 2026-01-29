package com.petclinic.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when business logic rules are violated
 * Maps to HTTP 422 Unprocessable Entity status
 */
public class BusinessRuleException extends PetClinicException {
    
    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    public BusinessRuleException(String operation, String reason) {
        super(String.format("Cannot %s: %s", operation, reason), 
              "BUSINESS_RULE_VIOLATION", 
              HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    public BusinessRuleException(String message, Throwable cause) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}