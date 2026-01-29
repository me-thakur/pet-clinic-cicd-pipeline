package com.petclinic.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for Pet Clinic application
 * Provides common error handling functionality with HTTP status codes
 */
public class PetClinicException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public PetClinicException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public PetClinicException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}