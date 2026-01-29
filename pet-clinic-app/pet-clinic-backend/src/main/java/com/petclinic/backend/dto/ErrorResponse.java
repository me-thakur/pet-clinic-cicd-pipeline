package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response format for all API errors
 * Provides consistent structure across all error conditions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    private int status;
    private String error;
    private String message;
    private String path;
    private String errorCode;
    private String apiVersion;
    private Map<String, String> fieldErrors;
    private List<String> details;
    private String traceId;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(int status, String error, String message, String path, String errorCode) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errorCode = errorCode;
    }
    
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, String errorCode) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errorCode = errorCode;
    }
    
    // Builder pattern for easier construction
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
    
    public static class ErrorResponseBuilder {
        private ErrorResponse errorResponse = new ErrorResponse();
        
        public ErrorResponseBuilder status(int status) {
            errorResponse.status = status;
            return this;
        }
        
        public ErrorResponseBuilder error(String error) {
            errorResponse.error = error;
            return this;
        }
        
        public ErrorResponseBuilder message(String message) {
            errorResponse.message = message;
            return this;
        }
        
        public ErrorResponseBuilder path(String path) {
            errorResponse.path = path;
            return this;
        }
        
        public ErrorResponseBuilder errorCode(String errorCode) {
            errorResponse.errorCode = errorCode;
            return this;
        }
        
        public ErrorResponseBuilder apiVersion(String apiVersion) {
            errorResponse.apiVersion = apiVersion;
            return this;
        }
        
        public ErrorResponseBuilder fieldErrors(Map<String, String> fieldErrors) {
            errorResponse.fieldErrors = fieldErrors;
            return this;
        }
        
        public ErrorResponseBuilder details(List<String> details) {
            errorResponse.details = details;
            return this;
        }
        
        public ErrorResponseBuilder traceId(String traceId) {
            errorResponse.traceId = traceId;
            return this;
        }
        
        public ErrorResponse build() {
            return errorResponse;
        }
    }
    
    // Getters and setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
    
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
    
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}