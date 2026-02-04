package com.petclinic.backend.exception;

import java.util.Map;
import java.util.HashMap;

/**
 * Exception for business logic violations with specific error codes and context
 */
public class BusinessLogicException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> context;
    
    public BusinessLogicException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public BusinessLogicException(String message, String errorCode, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : new HashMap<>();
    }
    
    public BusinessLogicException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }
}