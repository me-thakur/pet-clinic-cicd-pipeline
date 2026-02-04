package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Field-specific error information with suggestions for correction
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Field-specific error with correction suggestions")
public class FieldError {
    
    @Schema(description = "Field name that has the error", example = "email")
    private String field;
    
    @Schema(description = "Human-readable error message", example = "Email address is already in use")
    private String message;
    
    @Schema(description = "The value that was rejected", example = "john@example.com")
    private String rejectedValue;
    
    @Schema(description = "Suggestions for correcting the error")
    private List<String> suggestions;
    
    @Schema(description = "Error code specific to this field", example = "EMAIL_DUPLICATE")
    private String errorCode;
    
    @Schema(description = "Display name for the field", example = "Email Address")
    private String displayName;
    
    public FieldError() {}
    
    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }
    
    public FieldError(String field, String message, String rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }
    
    // Builder pattern for easy construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private FieldError fieldError = new FieldError();
        
        public Builder field(String field) {
            fieldError.field = field;
            return this;
        }
        
        public Builder message(String message) {
            fieldError.message = message;
            return this;
        }
        
        public Builder rejectedValue(String rejectedValue) {
            fieldError.rejectedValue = rejectedValue;
            return this;
        }
        
        public Builder suggestions(List<String> suggestions) {
            fieldError.suggestions = suggestions;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            fieldError.errorCode = errorCode;
            return this;
        }
        
        public Builder displayName(String displayName) {
            fieldError.displayName = displayName;
            return this;
        }
        
        public FieldError build() {
            return fieldError;
        }
    }
    
    // Getters and setters
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRejectedValue() {
        return rejectedValue;
    }
    
    public void setRejectedValue(String rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
    
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}