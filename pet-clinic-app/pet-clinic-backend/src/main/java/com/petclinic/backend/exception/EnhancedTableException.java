package com.petclinic.backend.exception;

/**
 * Custom exception for enhanced table operations
 * Provides structured error handling with error codes and user-friendly messages
 * Requirements: 2.4, 4.2
 */
public class EnhancedTableException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String userMessage;
    private final Object details;
    
    public EnhancedTableException(ErrorCode errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.details = null;
    }
    
    public EnhancedTableException(ErrorCode errorCode, String userMessage, String technicalMessage) {
        super(technicalMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.details = null;
    }
    
    public EnhancedTableException(ErrorCode errorCode, String userMessage, String technicalMessage, Object details) {
        super(technicalMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.details = details;
    }
    
    public EnhancedTableException(ErrorCode errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.details = null;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public Object getDetails() {
        return details;
    }
    
    public boolean isRetryable() {
        return errorCode.isRetryable();
    }
    
    public boolean requiresFallback() {
        return errorCode.requiresFallback();
    }
    
    /**
     * Error codes for enhanced table operations
     */
    public enum ErrorCode {
        // Validation Errors (4xx)
        INVALID_ENTITY_TYPE("INVALID_ENTITY_TYPE", "Invalid table type", false, false),
        INVALID_SORT_COLUMN("INVALID_SORT_COLUMN", "Invalid sort column", false, false),
        INVALID_SORT_DIRECTION("INVALID_SORT_DIRECTION", "Invalid sort direction", false, false),
        INVALID_FILTER_CRITERIA("INVALID_FILTER_CRITERIA", "Invalid filter criteria", false, false),
        INVALID_PAGE_PARAMETERS("INVALID_PAGE_PARAMETERS", "Invalid pagination parameters", false, false),
        INVALID_BULK_REQUEST("INVALID_BULK_REQUEST", "Invalid bulk operation request", false, false),
        
        // Server Errors (5xx)
        DATABASE_CONNECTION_FAILED("DATABASE_CONNECTION_FAILED", "Database connection failed", true, true),
        DATABASE_QUERY_FAILED("DATABASE_QUERY_FAILED", "Database query failed", true, true),
        BULK_OPERATION_FAILED("BULK_OPERATION_FAILED", "Bulk operation failed", true, false),
        CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION", "Data was modified by another user", true, false),
        CACHE_OPERATION_FAILED("CACHE_OPERATION_FAILED", "Cache operation failed", true, false),
        
        // System Errors
        INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", true, true),
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service temporarily unavailable", true, true),
        TIMEOUT_ERROR("TIMEOUT_ERROR", "Operation timed out", true, true);
        
        private final String code;
        private final String defaultMessage;
        private final boolean retryable;
        private final boolean requiresFallback;
        
        ErrorCode(String code, String defaultMessage, boolean retryable, boolean requiresFallback) {
            this.code = code;
            this.defaultMessage = defaultMessage;
            this.retryable = retryable;
            this.requiresFallback = requiresFallback;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDefaultMessage() {
            return defaultMessage;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
        
        public boolean requiresFallback() {
            return requiresFallback;
        }
    }
}