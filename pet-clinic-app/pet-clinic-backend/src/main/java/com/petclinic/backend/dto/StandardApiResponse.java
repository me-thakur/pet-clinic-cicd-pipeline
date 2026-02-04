package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for consistent response format across all endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class StandardApiResponse<T> {
    
    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "Response data")
    private T data;
    
    @Schema(description = "Error details (only present when success is false)")
    private ErrorDetails error;
    
    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;
    
    @Schema(description = "API version", example = "v1")
    private String version = "v1";
    
    // Constructors
    public StandardApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public StandardApiResponse(boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    // Static factory methods for success responses
    public static <T> StandardApiResponse<T> success(T data) {
        return new StandardApiResponse<>(true, "Success", data);
    }
    
    public static <T> StandardApiResponse<T> success(String message, T data) {
        return new StandardApiResponse<>(true, message, data);
    }
    
    public static StandardApiResponse<Void> success(String message) {
        return new StandardApiResponse<>(true, message, null);
    }
    
    // Static factory methods for error responses
    public static <T> StandardApiResponse<T> error(String message) {
        StandardApiResponse<T> response = new StandardApiResponse<>(false, message, null);
        response.error = new ErrorDetails("GENERAL_ERROR", message);
        return response;
    }
    
    public static <T> StandardApiResponse<T> error(String code, String message) {
        StandardApiResponse<T> response = new StandardApiResponse<>(false, message, null);
        response.error = new ErrorDetails(code, message);
        return response;
    }
    
    public static <T> StandardApiResponse<T> error(String code, String message, String details) {
        StandardApiResponse<T> response = new StandardApiResponse<>(false, message, null);
        response.error = new ErrorDetails(code, message, details);
        return response;
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public ErrorDetails getError() {
        return error;
    }
    
    public void setError(ErrorDetails error) {
        this.error = error;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Error details for failed responses
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error details")
    public static class ErrorDetails {
        @Schema(description = "Error code", example = "VALIDATION_ERROR")
        private String code;
        
        @Schema(description = "Error message", example = "Invalid input data")
        private String message;
        
        @Schema(description = "Detailed error information")
        private String details;
        
        public ErrorDetails() {}
        
        public ErrorDetails(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public ErrorDetails(String code, String message, String details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
        
        // Getters and setters
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getDetails() {
            return details;
        }
        
        public void setDetails(String details) {
            this.details = details;
        }
    }
}