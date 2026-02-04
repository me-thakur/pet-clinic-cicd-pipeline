package com.petclinic.backend.service;

import com.petclinic.backend.dto.ValidationResponse;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;

/**
 * Service interface for handling validation service errors and graceful degradation
 * Provides fallback mechanisms and offline validation caching for validation operations
 * 
 * Validates: Requirements 6.3
 */
public interface ValidationErrorHandlingService {

    /**
     * Execute validation operation with graceful degradation
     * 
     * @param validationOperation The validation operation to execute
     * @param owner Owner data being validated
     * @param validationType Type of validation (new, update, field)
     * @param context Additional context for validation
     * @return ValidationResponse with results or fallback information
     */
    ValidationResponse executeValidationWithGracefulDegradation(
            ValidationOperation validationOperation,
            Owner owner,
            String validationType,
            ValidationContext context);

    /**
     * Check if validation service is available
     * 
     * @return true if service is available, false otherwise
     */
    boolean isValidationServiceAvailable();

    /**
     * Get cached validation result if available
     * 
     * @param owner Owner data
     * @param validationType Type of validation
     * @return Cached validation result or null if not available
     */
    ValidationResult getCachedValidationResult(Owner owner, String validationType);

    /**
     * Cache validation result for offline use
     * 
     * @param owner Owner data
     * @param validationType Type of validation
     * @param result Validation result to cache
     */
    void cacheValidationResult(Owner owner, String validationType, ValidationResult result);

    /**
     * Perform basic offline validation
     * 
     * @param owner Owner data
     * @param validationType Type of validation
     * @return Basic validation result
     */
    ValidationResult performOfflineValidation(Owner owner, String validationType);

    /**
     * Get user-friendly error message for validation failures
     * 
     * @param validationType Type of validation that failed
     * @param error The original error
     * @return User-friendly error message
     */
    String getValidationErrorMessage(String validationType, Throwable error);

    /**
     * Log detailed validation error information
     * 
     * @param validationType Type of validation
     * @param owner Owner data
     * @param error The error that occurred
     * @param attemptNumber The retry attempt number
     */
    void logValidationError(String validationType, Owner owner, Throwable error, int attemptNumber);

    /**
     * Create warning response for degraded validation
     * 
     * @param result Basic validation result
     * @param validationType Type of validation
     * @param warningMessage Warning message to include
     * @return ValidationResponse with warning
     */
    ValidationResponse createWarningResponse(ValidationResult result, String validationType, String warningMessage);

    /**
     * Functional interface for validation operations
     */
    @FunctionalInterface
    interface ValidationOperation {
        ValidationResult execute() throws Exception;
    }

    /**
     * Context information for validation operations
     */
    class ValidationContext {
        private Long ownerId;
        private String[] fieldNames;
        private boolean validateOnlyChangedFields;
        private String requestId;

        public ValidationContext() {}

        public ValidationContext(Long ownerId, String[] fieldNames, boolean validateOnlyChangedFields, String requestId) {
            this.ownerId = ownerId;
            this.fieldNames = fieldNames;
            this.validateOnlyChangedFields = validateOnlyChangedFields;
            this.requestId = requestId;
        }

        // Getters and setters
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

        public String[] getFieldNames() { return fieldNames; }
        public void setFieldNames(String[] fieldNames) { this.fieldNames = fieldNames; }

        public boolean isValidateOnlyChangedFields() { return validateOnlyChangedFields; }
        public void setValidateOnlyChangedFields(boolean validateOnlyChangedFields) { this.validateOnlyChangedFields = validateOnlyChangedFields; }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
    }
}