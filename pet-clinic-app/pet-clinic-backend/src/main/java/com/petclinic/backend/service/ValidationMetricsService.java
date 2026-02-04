package com.petclinic.backend.service;

import com.petclinic.backend.dto.ValidationResult;

/**
 * Service interface for validation metrics collection and monitoring
 * Provides comprehensive metrics tracking for validation operations
 * Validates: Requirements 4.5
 */
public interface ValidationMetricsService {
    
    /**
     * Record a validation attempt with its outcome
     * 
     * @param validationType Type of validation (new_owner, owner_update, field_validation, etc.)
     * @param result Validation result containing success/failure information
     * @param processingTimeMs Time taken to process the validation in milliseconds
     */
    void recordValidationAttempt(String validationType, ValidationResult result, long processingTimeMs);
    
    /**
     * Record a validation success
     * 
     * @param validationType Type of validation performed
     * @param processingTimeMs Time taken to process the validation
     */
    void recordValidationSuccess(String validationType, long processingTimeMs);
    
    /**
     * Record a validation failure
     * 
     * @param validationType Type of validation performed
     * @param errorCount Number of validation errors
     * @param processingTimeMs Time taken to process the validation
     */
    void recordValidationFailure(String validationType, int errorCount, long processingTimeMs);
    
    /**
     * Record field-specific validation metrics
     * 
     * @param fieldName Name of the field being validated
     * @param validationType Type of validation (format, uniqueness, business_rule)
     * @param success Whether the field validation succeeded
     */
    void recordFieldValidation(String fieldName, String validationType, boolean success);
    
    /**
     * Record validation error by type
     * 
     * @param errorType Type of validation error (FORMAT_INVALID, UNIQUENESS_VIOLATION, etc.)
     * @param fieldName Field that caused the error
     */
    void recordValidationError(String errorType, String fieldName);
    
    /**
     * Record validation processing time for performance monitoring
     * 
     * @param validationType Type of validation performed
     * @param processingTimeMs Processing time in milliseconds
     */
    void recordValidationProcessingTime(String validationType, long processingTimeMs);
    
    /**
     * Get current validation success rate for a specific validation type
     * 
     * @param validationType Type of validation
     * @return Success rate as a percentage (0.0 to 100.0)
     */
    double getValidationSuccessRate(String validationType);
    
    /**
     * Get total number of validation attempts for a specific type
     * 
     * @param validationType Type of validation
     * @return Total number of attempts
     */
    long getTotalValidationAttempts(String validationType);
    
    /**
     * Get average processing time for a specific validation type
     * 
     * @param validationType Type of validation
     * @return Average processing time in milliseconds
     */
    double getAverageProcessingTime(String validationType);
}