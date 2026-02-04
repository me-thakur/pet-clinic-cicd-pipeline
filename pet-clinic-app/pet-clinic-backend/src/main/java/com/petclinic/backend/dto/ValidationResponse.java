package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * DTO for validation responses
 * Contains comprehensive validation results with field-specific errors
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 5.1
 */
public class ValidationResponse {
    
    @JsonProperty("valid")
    private boolean valid;
    
    @JsonProperty("fieldErrors")
    private List<FieldValidationError> fieldErrors;
    
    @JsonProperty("overallMessage")
    private String overallMessage;
    
    @JsonProperty("validationTimestamp")
    private LocalDateTime validationTimestamp;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("validationContext")
    private String validationContext;
    
    @JsonProperty("warning")
    private boolean warning = false;
    
    @JsonProperty("warningMessage")
    private String warningMessage;
    
    // Constructors
    public ValidationResponse() {
        this.fieldErrors = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.validationTimestamp = LocalDateTime.now();
    }
    
    public ValidationResponse(boolean valid) {
        this();
        this.valid = valid;
    }
    
    public ValidationResponse(boolean valid, String overallMessage) {
        this();
        this.valid = valid;
        this.overallMessage = overallMessage;
    }
    
    public ValidationResponse(boolean valid, List<FieldValidationError> fieldErrors, String overallMessage) {
        this();
        this.valid = valid;
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
        this.overallMessage = overallMessage;
    }
    
    // Static factory methods
    public static ValidationResponse valid() {
        ValidationResponse response = new ValidationResponse(true);
        response.setOverallMessage("All validation checks passed");
        return response;
    }
    
    public static ValidationResponse valid(String message) {
        ValidationResponse response = new ValidationResponse(true);
        response.setOverallMessage(message);
        return response;
    }
    
    public static ValidationResponse invalid(String overallMessage) {
        return new ValidationResponse(false, overallMessage);
    }
    
    public static ValidationResponse invalid(List<FieldValidationError> fieldErrors) {
        ValidationResponse response = new ValidationResponse(false);
        response.setFieldErrors(fieldErrors);
        response.setOverallMessage("Validation failed with " + fieldErrors.size() + " field error(s)");
        return response;
    }
    
    public static ValidationResponse invalid(List<FieldValidationError> fieldErrors, String overallMessage) {
        return new ValidationResponse(false, fieldErrors, overallMessage);
    }
    
    public static ValidationResponse fromValidationResult(com.petclinic.backend.dto.ValidationResult result) {
        if (result == null) {
            return ValidationResponse.invalid("Validation service returned null result");
        }
        
        ValidationResponse response = new ValidationResponse();
        response.setValid(result.isValid());
        response.setFieldErrors(result.getFieldErrors());
        response.setOverallMessage(result.getOverallMessage());
        if (result.getMetadata() != null) {
            response.setMetadata(result.getMetadata());
        }
        
        // Enhance field errors with display names if missing
        response.enhanceFieldErrorsWithDisplayNames();
        
        return response;
    }
    
    /**
     * Enhance field errors with display names and ensure corrective guidance is present
     */
    public void enhanceFieldErrorsWithDisplayNames() {
        if (fieldErrors == null) return;
        
        Map<String, String> displayNames = Map.of(
            "firstName", "First Name",
            "lastName", "Last Name", 
            "email", "Email Address",
            "telephone", "Telephone",
            "mobileNumber", "Mobile Number",
            "address", "Address",
            "city", "City",
            "state", "State",
            "zipCode", "ZIP Code"
        );
        
        for (FieldValidationError error : fieldErrors) {
            // Set field display name
            String fieldDisplayName = displayNames.getOrDefault(error.getFieldName(), error.getFieldName());
            error.setFieldDisplayName(fieldDisplayName);
            
            // Enhance error message to include field display name if it doesn't already
            if (error.getErrorMessage() != null && !error.getErrorMessage().contains(fieldDisplayName)) {
                String enhancedMessage = fieldDisplayName + ": " + error.getErrorMessage();
                error.setErrorMessage(enhancedMessage);
            }
            
            // Ensure corrective guidance is present
            if (error.getCorrectionGuidance() == null || error.getCorrectionGuidance().trim().isEmpty()) {
                String guidance = generateCorrectionGuidance(error.getFieldName(), error.getErrorCode());
                error.setCorrectionGuidance(guidance);
            }
        }
    }
    
    /**
     * Generate corrective guidance for field errors
     */
    private String generateCorrectionGuidance(String fieldName, String errorCode) {
        Map<String, String> displayNames = Map.of(
            "firstName", "First Name",
            "lastName", "Last Name", 
            "email", "Email Address",
            "telephone", "Telephone",
            "mobileNumber", "Mobile Number",
            "address", "Address",
            "city", "City",
            "state", "State",
            "zipCode", "ZIP Code"
        );
        
        String fieldDisplayName = displayNames.getOrDefault(fieldName, fieldName);
        
        if (errorCode == null) {
            return "Please check the " + fieldDisplayName.toLowerCase() + " and try again";
        }
        
        // Generate specific guidance based on error code
        if (errorCode.contains("REQUIRED")) {
            return "Please enter a " + fieldDisplayName.toLowerCase();
        } else if (errorCode.contains("FORMAT") || errorCode.contains("INVALID")) {
            return "Please enter a valid " + fieldDisplayName.toLowerCase() + " in the correct format";
        } else if (errorCode.contains("UNIQUE") || errorCode.contains("DUPLICATE")) {
            return "This " + fieldDisplayName.toLowerCase() + " is already in use. Please choose a different one";
        } else if (errorCode.contains("LENGTH") || errorCode.contains("TOO_LONG") || errorCode.contains("TOO_SHORT")) {
            return "Please check the length of the " + fieldDisplayName.toLowerCase();
        } else {
            return "Please check the " + fieldDisplayName.toLowerCase() + " and try again";
        }
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
    @JsonIgnore
    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
    
    @JsonIgnore
    public int getErrorCount() {
        return fieldErrors != null ? fieldErrors.size() : 0;
    }
    
    @JsonIgnore
    public boolean hasErrorForField(String fieldName) {
        return fieldErrors != null && fieldErrors.stream()
            .anyMatch(error -> fieldName.equals(error.getFieldName()));
    }
    
    @JsonIgnore
    public List<FieldValidationError> getErrorsForField(String fieldName) {
        if (fieldErrors == null) {
            return new ArrayList<>();
        }
        return fieldErrors.stream()
            .filter(error -> fieldName.equals(error.getFieldName()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    @JsonIgnore
    public boolean hasFormatErrors() {
        return fieldErrors != null && fieldErrors.stream()
            .anyMatch(FieldValidationError::isFormatError);
    }
    
    @JsonIgnore
    public boolean hasUniquenessErrors() {
        return fieldErrors != null && fieldErrors.stream()
            .anyMatch(FieldValidationError::isUniquenessError);
    }
    
    @JsonIgnore
    public List<String> getFieldsWithErrors() {
        if (fieldErrors == null) {
            return new ArrayList<>();
        }
        return fieldErrors.stream()
            .map(FieldValidationError::getFieldName)
            .distinct()
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
    
    public LocalDateTime getValidationTimestamp() {
        return validationTimestamp;
    }
    
    public void setValidationTimestamp(LocalDateTime validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
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
    
    public String getValidationContext() {
        return validationContext;
    }
    
    public void setValidationContext(String validationContext) {
        this.validationContext = validationContext;
    }
    
    public boolean isWarning() {
        return warning;
    }
    
    public void setWarning(boolean warning) {
        this.warning = warning;
    }
    
    public String getWarningMessage() {
        return warningMessage;
    }
    
    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResponse that = (ValidationResponse) o;
        return valid == that.valid &&
               Objects.equals(fieldErrors, that.fieldErrors) &&
               Objects.equals(overallMessage, that.overallMessage) &&
               Objects.equals(validationContext, that.validationContext);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, fieldErrors, overallMessage, validationContext);
    }
    
    @Override
    public String toString() {
        return "ValidationResponse{" +
                "valid=" + valid +
                ", errorCount=" + getErrorCount() +
                ", overallMessage='" + overallMessage + '\'' +
                ", validationContext='" + validationContext + '\'' +
                ", validationTimestamp=" + validationTimestamp +
                '}';
    }
}