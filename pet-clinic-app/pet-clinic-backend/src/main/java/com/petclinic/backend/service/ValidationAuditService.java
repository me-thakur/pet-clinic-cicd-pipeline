package com.petclinic.backend.service;

import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;

/**
 * Service interface for validation audit logging
 * Provides comprehensive audit trail for all validation operations
 * Validates: Requirements 4.5
 */
public interface ValidationAuditService {
    
    /**
     * Log a validation attempt with full context
     * 
     * @param validationType Type of validation performed
     * @param ownerData Owner data being validated (may be partial for field validation)
     * @param ownerId Owner ID for update operations (null for new owners)
     * @param result Validation result
     * @param processingTimeMs Time taken to process the validation
     * @param requestSource Source of the validation request (e.g., "REST_API", "BATCH_IMPORT")
     */
    void logValidationAttempt(String validationType, Owner ownerData, Long ownerId, 
                             ValidationResult result, long processingTimeMs, String requestSource);
    
    /**
     * Log a successful validation
     * 
     * @param validationType Type of validation performed
     * @param ownerData Owner data that was validated
     * @param ownerId Owner ID for update operations (null for new owners)
     * @param processingTimeMs Time taken to process the validation
     * @param requestSource Source of the validation request
     */
    void logValidationSuccess(String validationType, Owner ownerData, Long ownerId, 
                             long processingTimeMs, String requestSource);
    
    /**
     * Log a failed validation with detailed error information
     * 
     * @param validationType Type of validation performed
     * @param ownerData Owner data that failed validation
     * @param ownerId Owner ID for update operations (null for new owners)
     * @param result Validation result containing error details
     * @param processingTimeMs Time taken to process the validation
     * @param requestSource Source of the validation request
     */
    void logValidationFailure(String validationType, Owner ownerData, Long ownerId, 
                             ValidationResult result, long processingTimeMs, String requestSource);
    
    /**
     * Log field-specific validation events
     * 
     * @param fieldName Name of the field being validated
     * @param fieldValue Value being validated (may be masked for sensitive data)
     * @param validationType Type of validation (format, uniqueness, business_rule)
     * @param success Whether the field validation succeeded
     * @param errorMessage Error message if validation failed
     * @param requestSource Source of the validation request
     */
    void logFieldValidation(String fieldName, Object fieldValue, String validationType, 
                           boolean success, String errorMessage, String requestSource);
    
    /**
     * Log validation system errors for troubleshooting
     * 
     * @param validationType Type of validation that encountered an error
     * @param ownerData Owner data being processed when error occurred
     * @param error Exception or error that occurred
     * @param requestSource Source of the validation request
     */
    void logValidationSystemError(String validationType, Owner ownerData, 
                                 Throwable error, String requestSource);
    
    /**
     * Log validation configuration changes
     * 
     * @param configurationChange Description of the configuration change
     * @param oldValue Previous configuration value
     * @param newValue New configuration value
     * @param changedBy User or system that made the change
     */
    void logValidationConfigurationChange(String configurationChange, Object oldValue, 
                                         Object newValue, String changedBy);
    
    /**
     * Log validation performance metrics for monitoring
     * 
     * @param validationType Type of validation
     * @param processingTimeMs Processing time in milliseconds
     * @param memoryUsageMB Memory usage during validation
     * @param requestSource Source of the validation request
     */
    void logValidationPerformance(String validationType, long processingTimeMs, 
                                 double memoryUsageMB, String requestSource);
}