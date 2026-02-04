package com.petclinic.backend.dto;

/**
 * Data Transfer Object for uniqueness validation results
 * Contains validation outcome and detailed error information for uniqueness constraints
 * Validates: Requirements 2.2, 2.3, 2.4, 3.1, 3.5
 */
public class UniquenessValidationResult {
    
    private boolean unique;
    private String errorMessage;
    private String correctionGuidance;
    private String rejectedValue;
    private String errorCode;
    private Long conflictingEntityId;
    
    // Constructors
    public UniquenessValidationResult() {}
    
    public UniquenessValidationResult(boolean unique) {
        this.unique = unique;
    }
    
    public UniquenessValidationResult(boolean unique, String errorMessage) {
        this.unique = unique;
        this.errorMessage = errorMessage;
    }
    
    public UniquenessValidationResult(boolean unique, String errorMessage, String correctionGuidance,
                                    String rejectedValue, String errorCode, Long conflictingEntityId) {
        this.unique = unique;
        this.errorMessage = errorMessage;
        this.correctionGuidance = correctionGuidance;
        this.rejectedValue = rejectedValue;
        this.errorCode = errorCode;
        this.conflictingEntityId = conflictingEntityId;
    }
    
    // Static factory methods for common scenarios
    public static UniquenessValidationResult unique() {
        return new UniquenessValidationResult(true);
    }
    
    public static UniquenessValidationResult notUnique(String errorMessage, String correctionGuidance,
                                                     String rejectedValue, String errorCode, Long conflictingEntityId) {
        return new UniquenessValidationResult(false, errorMessage, correctionGuidance,
                                            rejectedValue, errorCode, conflictingEntityId);
    }
    
    public static UniquenessValidationResult notUnique(String errorMessage, String rejectedValue, String errorCode) {
        return new UniquenessValidationResult(false, errorMessage, null, rejectedValue, errorCode, null);
    }
    
    // Getters and Setters
    public boolean isUnique() {
        return unique;
    }
    
    public void setUnique(boolean unique) {
        this.unique = unique;
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
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public Long getConflictingEntityId() {
        return conflictingEntityId;
    }
    
    public void setConflictingEntityId(Long conflictingEntityId) {
        this.conflictingEntityId = conflictingEntityId;
    }
    
    @Override
    public String toString() {
        return "UniquenessValidationResult{" +
                "unique=" + unique +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", rejectedValue='" + rejectedValue + '\'' +
                ", conflictingEntityId=" + conflictingEntityId +
                '}';
    }
}