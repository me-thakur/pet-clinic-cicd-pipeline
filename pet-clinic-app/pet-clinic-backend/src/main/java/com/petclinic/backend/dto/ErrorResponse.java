package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Enhanced error response with specific error information and recovery suggestions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Enhanced error response with specific details and recovery suggestions")
public class ErrorResponse {
    
    @Schema(description = "Specific error code for programmatic handling", example = "VALIDATION_FAILED")
    private String errorCode;
    
    @Schema(description = "Human-readable error message", example = "Please correct the following fields")
    private String message;
    
    @Schema(description = "Field-specific validation errors")
    private List<FieldError> fieldErrors;
    
    @Schema(description = "Suggestions for resolving the error")
    private List<String> suggestions;
    
    @Schema(description = "Time to wait before retrying (for service unavailable errors)")
    private String retryAfter;
    
    @Schema(description = "Available fallback options when primary service is unavailable")
    private List<String> fallbackOptions;
    
    @Schema(description = "Error timestamp")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request path where error occurred")
    private String path;
    
    @Schema(description = "Additional context information")
    private Map<String, Object> context;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }
    
    // Builder pattern for easy construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ErrorResponse errorResponse = new ErrorResponse();
        
        public Builder errorCode(String errorCode) {
            errorResponse.errorCode = errorCode;
            return this;
        }
        
        public Builder message(String message) {
            errorResponse.message = message;
            return this;
        }
        
        public Builder fieldErrors(List<FieldError> fieldErrors) {
            errorResponse.fieldErrors = fieldErrors;
            return this;
        }
        
        public Builder suggestions(List<String> suggestions) {
            errorResponse.suggestions = suggestions;
            return this;
        }
        
        public Builder retryAfter(String retryAfter) {
            errorResponse.retryAfter = retryAfter;
            return this;
        }
        
        public Builder fallbackOptions(List<String> fallbackOptions) {
            errorResponse.fallbackOptions = fallbackOptions;
            return this;
        }
        
        public Builder path(String path) {
            errorResponse.path = path;
            return this;
        }
        
        public Builder context(Map<String, Object> context) {
            errorResponse.context = context;
            return this;
        }
        
        public ErrorResponse build() {
            return errorResponse;
        }
    }
    
    // Getters and setters
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    
    public String getRetryAfter() {
        return retryAfter;
    }
    
    public void setRetryAfter(String retryAfter) {
        this.retryAfter = retryAfter;
    }
    
    public List<String> getFallbackOptions() {
        return fallbackOptions;
    }
    
    public void setFallbackOptions(List<String> fallbackOptions) {
        this.fallbackOptions = fallbackOptions;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}