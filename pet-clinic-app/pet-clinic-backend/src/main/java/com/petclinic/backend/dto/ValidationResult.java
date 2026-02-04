package com.petclinic.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Transfer Object for comprehensive validation results
 * Aggregates validation outcomes from multiple validators
 * Validates: Requirements 1.2, 1.3, 1.5, 3.1, 3.2, 3.3
 */
public class ValidationResult {
    
    private boolean valid;
    private List<FieldValidationError> fieldErrors;
    private String overallMessage;
    private Map<String, Object> metadata;
    
    // Constructors
    public ValidationResult() {
        this.fieldErrors = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public ValidationResult(boolean valid) {
        this();
        this.valid = valid;
    }
    
    public ValidationResult(boolean valid, String overallMessage) {
        this();
        this.valid = valid;
        this.overallMessage = overallMessage;
    }
    
    // Static factory methods
    public static ValidationResult valid() {
        ValidationResult result = new ValidationResult(true);
        result.setOverallMessage("All validation checks passed");
        return result;
    }
    
    public static ValidationResult invalid(String overallMessage) {
        return new ValidationResult(false, overallMessage);
    }
    
    public static ValidationResult invalid(List<FieldValidationError> fieldErrors) {
        ValidationResult result = new ValidationResult(false);
        result.setFieldErrors(fieldErrors);
        result.setOverallMessage("Validation failed with " + fieldErrors.size() + " field error(s)");
        return result;
    }
    
    // Methods for adding field errors
    public void addFieldError(FieldValidationError fieldError) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new ArrayList<>();
        }
        this.fieldErrors.add(fieldError);
        this.valid = false;
        updateOverallMessage();
    }
    
    public void addFieldError(String fieldName, String errorCode, String errorMessage, 
                             String correctionGuidance, Object rejectedValue) {
        FieldValidationError error = new FieldValidationError(
            fieldName, errorCode, errorMessage, correctionGuidance, rejectedValue
        );
        addFieldError(error);
    }
    
    // Convenience method for adding errors with just field name, error code, and message
    public void addError(String fieldName, String errorCode, String errorMessage) {
        addFieldError(fieldName, errorCode, errorMessage, null, null);
    }
    
    public void addFieldErrors(List<FieldValidationError> errors) {
        if (errors != null && !errors.isEmpty()) {
            if (this.fieldErrors == null) {
                this.fieldErrors = new ArrayList<>();
            }
            this.fieldErrors.addAll(errors);
            this.valid = false;
            updateOverallMessage();
        }
    }
    
    // Utility methods
    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
    
    public int getErrorCount() {
        return fieldErrors != null ? fieldErrors.size() : 0;
    }
    
    public boolean hasErrorForField(String fieldName) {
        return fieldErrors != null && fieldErrors.stream()
            .anyMatch(error -> fieldName.equals(error.getFieldName()));
    }
    
    public List<FieldValidationError> getErrorsForField(String fieldName) {
        if (fieldErrors == null) {
            return new ArrayList<>();
        }
        return fieldErrors.stream()
            .filter(error -> fieldName.equals(error.getFieldName()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private void updateOverallMessage() {
        if (hasFieldErrors()) {
            int errorCount = getErrorCount();
            this.overallMessage = "Validation failed with " + errorCount + 
                                " field error" + (errorCount > 1 ? "s" : "");
        }
    }
    
    // Getters and Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(List<FieldValidationError> fieldErrors) {
        this.fieldErrors = fieldErrors;
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            this.valid = false;
            updateOverallMessage();
        }
    }
    
    public String getOverallMessage() {
        return overallMessage;
    }
    
    public void setOverallMessage(String overallMessage) {
        this.overallMessage = overallMessage;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errorCount=" + getErrorCount() +
                ", overallMessage='" + overallMessage + '\'' +
                '}';
    }
}