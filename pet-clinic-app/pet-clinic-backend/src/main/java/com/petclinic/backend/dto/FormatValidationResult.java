package com.petclinic.backend.dto;

/**
 * Data Transfer Object for format validation results
 * Contains validation outcome and detailed error information
 * Validates: Requirements 1.1, 2.1, 2.5, 3.1, 3.2
 */
public class FormatValidationResult {
    
    private boolean valid;
    private String errorMessage;
    private String correctionGuidance;
    private String rejectedValue;
    private String formatExample;
    private String errorCode;
    
    // Constructors
    public FormatValidationResult() {}
    
    public FormatValidationResult(boolean valid) {
        this.valid = valid;
    }
    
    public FormatValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    public FormatValidationResult(boolean valid, String errorMessage, String correctionGuidance, 
                                String rejectedValue, String formatExample, String errorCode) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.correctionGuidance = correctionGuidance;
        this.rejectedValue = rejectedValue;
        this.formatExample = formatExample;
        this.errorCode = errorCode;
    }
    
    // Static factory methods for common scenarios
    public static FormatValidationResult valid() {
        return new FormatValidationResult(true);
    }
    
    public static FormatValidationResult invalid(String errorMessage, String correctionGuidance, 
                                               String rejectedValue, String formatExample, String errorCode) {
        return new FormatValidationResult(false, errorMessage, correctionGuidance, 
                                        rejectedValue, formatExample, errorCode);
    }
    
    public static FormatValidationResult invalid(String errorMessage, String errorCode) {
        return new FormatValidationResult(false, errorMessage, null, null, null, errorCode);
    }
    
    // Getters and Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
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
    
    public String getRejectedValue() {
        return rejectedValue;
    }
    
    public void setRejectedValue(String rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
    
    public String getFormatExample() {
        return formatExample;
    }
    
    public void setFormatExample(String formatExample) {
        this.formatExample = formatExample;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    @Override
    public String toString() {
        return "FormatValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", rejectedValue='" + rejectedValue + '\'' +
                '}';
    }
}