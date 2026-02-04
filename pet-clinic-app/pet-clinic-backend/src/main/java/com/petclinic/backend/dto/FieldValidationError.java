package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Data Transfer Object for individual field validation errors
 * Contains detailed error information for specific fields
 * Validates: Requirements 3.1, 3.2, 3.4, 3.5
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldValidationError {
    
    @JsonProperty("fieldName")
    private String fieldName;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("correctionGuidance")
    private String correctionGuidance;
    
    @JsonProperty("rejectedValue")
    private Object rejectedValue;
    
    @JsonProperty("fieldDisplayName")
    private String fieldDisplayName;
    
    @JsonProperty("formatExample")
    private String formatExample;
    
    @JsonProperty("conflictingEntityId")
    private Long conflictingEntityId;
    
    // Constructors
    public FieldValidationError() {}
    
    public FieldValidationError(String fieldName, String errorCode, String errorMessage) {
        this.fieldName = fieldName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public FieldValidationError(String fieldName, String errorCode, String errorMessage, 
                               String correctionGuidance, Object rejectedValue) {
        this.fieldName = fieldName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.correctionGuidance = correctionGuidance;
        this.rejectedValue = rejectedValue;
        this.fieldDisplayName = generateFieldDisplayName(fieldName);
    }
    
    public FieldValidationError(String fieldName, String errorCode, String errorMessage,
                               String correctionGuidance, Object rejectedValue, 
                               String formatExample, Long conflictingEntityId) {
        this.fieldName = fieldName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.correctionGuidance = correctionGuidance;
        this.rejectedValue = rejectedValue;
        this.fieldDisplayName = generateFieldDisplayName(fieldName);
        this.formatExample = formatExample;
        this.conflictingEntityId = conflictingEntityId;
    }
    
    // Static factory methods for common error types
    public static FieldValidationError formatError(String fieldName, String errorMessage, 
                                                  String correctionGuidance, Object rejectedValue, 
                                                  String formatExample, String errorCode) {
        FieldValidationError error = new FieldValidationError(fieldName, errorCode, errorMessage, 
                                      correctionGuidance, rejectedValue, formatExample, null);
        error.setFieldDisplayName(generateFieldDisplayName(fieldName));
        return error;
    }
    
    public static FieldValidationError uniquenessError(String fieldName, String errorMessage,
                                                      String correctionGuidance, Object rejectedValue,
                                                      String errorCode, Long conflictingEntityId) {
        FieldValidationError error = new FieldValidationError(fieldName, errorCode, errorMessage,
                                      correctionGuidance, rejectedValue, null, conflictingEntityId);
        error.setFieldDisplayName(generateFieldDisplayName(fieldName));
        return error;
    }
    
    public static FieldValidationError requiredFieldError(String fieldName, String errorMessage,
                                                         String correctionGuidance, String errorCode) {
        FieldValidationError error = new FieldValidationError(fieldName, errorCode, errorMessage, correctionGuidance, null);
        error.setFieldDisplayName(generateFieldDisplayName(fieldName));
        return error;
    }
    
    public static FieldValidationError businessRuleError(String fieldName, String errorMessage,
                                                        String correctionGuidance, Object rejectedValue,
                                                        String errorCode) {
        FieldValidationError error = new FieldValidationError(fieldName, errorCode, errorMessage, 
                                      correctionGuidance, rejectedValue);
        error.setFieldDisplayName(generateFieldDisplayName(fieldName));
        return error;
    }
    
    /**
     * Generate user-friendly display name for field
     */
    private static String generateFieldDisplayName(String fieldName) {
        if (fieldName == null) return null;
        
        java.util.Map<String, String> displayNames = java.util.Map.of(
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
        return displayNames.getOrDefault(fieldName, fieldName);
    }
    
    // Utility methods
    @JsonIgnore
    public boolean isFormatError() {
        return errorCode != null && (errorCode.contains("FORMAT") || errorCode.contains("INVALID"));
    }
    
    @JsonIgnore
    public boolean isUniquenessError() {
        return errorCode != null && (errorCode.contains("UNIQUE") || errorCode.contains("DUPLICATE"));
    }
    
    @JsonIgnore
    public boolean isRequiredFieldError() {
        return errorCode != null && errorCode.contains("REQUIRED");
    }
    
    @JsonIgnore
    public boolean hasFormatExample() {
        return formatExample != null && !formatExample.trim().isEmpty();
    }
    
    @JsonIgnore
    public boolean hasConflictingEntity() {
        return conflictingEntityId != null;
    }
    
    // Getters and Setters
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getCorrectionGuidance() {
        return correctionGuidance;
    }
    
    public void setCorrectionGuidance(String correctionGuidance) {
        this.correctionGuidance = correctionGuidance;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
    
    public String getFieldDisplayName() {
        return fieldDisplayName;
    }
    
    public void setFieldDisplayName(String fieldDisplayName) {
        this.fieldDisplayName = fieldDisplayName;
    }
    
    public String getFormatExample() {
        return formatExample;
    }
    
    public void setFormatExample(String formatExample) {
        this.formatExample = formatExample;
    }
    
    public Long getConflictingEntityId() {
        return conflictingEntityId;
    }
    
    public void setConflictingEntityId(Long conflictingEntityId) {
        this.conflictingEntityId = conflictingEntityId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldValidationError that = (FieldValidationError) o;
        return java.util.Objects.equals(fieldName, that.fieldName) &&
               java.util.Objects.equals(errorCode, that.errorCode) &&
               java.util.Objects.equals(errorMessage, that.errorMessage) &&
               java.util.Objects.equals(correctionGuidance, that.correctionGuidance) &&
               java.util.Objects.equals(rejectedValue, that.rejectedValue) &&
               java.util.Objects.equals(fieldDisplayName, that.fieldDisplayName) &&
               java.util.Objects.equals(formatExample, that.formatExample) &&
               java.util.Objects.equals(conflictingEntityId, that.conflictingEntityId);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(fieldName, errorCode, errorMessage, correctionGuidance, 
                                     rejectedValue, fieldDisplayName, formatExample, conflictingEntityId);
    }
    
    @Override
    public String toString() {
        return "FieldValidationError{" +
                "fieldName='" + fieldName + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", rejectedValue=" + rejectedValue +
                '}';
    }
}