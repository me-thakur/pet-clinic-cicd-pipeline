package com.petclinic.backend.exception;

import com.petclinic.backend.dto.FieldError;

import java.util.List;
import java.util.ArrayList;

/**
 * Custom validation exception with field-specific error details
 */
public class ValidationException extends RuntimeException {
    
    private final List<FieldError> fieldErrors;
    private final String errorCode;
    
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new ArrayList<>();
        this.errorCode = "VALIDATION_FAILED";
    }
    
    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
        this.errorCode = "VALIDATION_FAILED";
    }
    
    public ValidationException(String message, String errorCode, List<FieldError> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
        this.errorCode = errorCode;
    }
    
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void addFieldError(String field, String message) {
        this.fieldErrors.add(FieldError.builder()
            .field(field)
            .message(message)
            .build());
    }
    
    public void addFieldError(String field, String message, String rejectedValue) {
        this.fieldErrors.add(FieldError.builder()
            .field(field)
            .message(message)
            .rejectedValue(rejectedValue)
            .build());
    }
}