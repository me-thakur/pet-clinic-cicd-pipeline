package com.petclinic.backend.service;

import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;

/**
 * Service interface for orchestrating comprehensive owner data validation
 * Coordinates multiple validation types and aggregates results
 * Validates: Requirements 1.2, 1.3, 1.5, 4.4
 */
public interface OwnerValidationService {
    
    /**
     * Validates a new owner entry against all validation rules
     * Performs format validation, business rule validation, and uniqueness checks
     * @param owner the owner data to validate
     * @return comprehensive validation result with all errors aggregated
     */
    ValidationResult validateNewOwner(Owner owner);
    
    /**
     * Validates an owner update against all validation rules
     * Allows the same owner to keep their existing unique values
     * @param ownerId the ID of the owner being updated
     * @param owner the updated owner data to validate
     * @return comprehensive validation result with all errors aggregated
     */
    ValidationResult validateOwnerUpdate(Long ownerId, Owner owner);
    
    /**
     * Validates specific fields of an owner entry
     * Useful for partial validation during form input
     * @param owner the owner data to validate
     * @param fieldNames the specific fields to validate
     * @return validation result for the specified fields only
     */
    ValidationResult validateOwnerFields(Owner owner, String... fieldNames);
    
    /**
     * Validates an owner update for specific fields only
     * @param ownerId the ID of the owner being updated
     * @param owner the updated owner data to validate
     * @param fieldNames the specific fields to validate
     * @return validation result for the specified fields only
     */
    ValidationResult validateOwnerFieldsUpdate(Long ownerId, Owner owner, String... fieldNames);
    
    /**
     * Validates an owner update with enhanced optimization for partial updates
     * This method allows for more granular control over which fields are validated
     * while still ensuring complete validation of the specified fields
     * 
     * @param ownerId The ID of the owner being updated
     * @param owner The owner data to validate
     * @param validateOnlyChangedFields If true, only validates non-null fields
     * @param fieldNames Specific fields to validate (optional)
     * @return ValidationResult containing validation outcome
     */
    ValidationResult validateOwnerUpdateOptimized(Long ownerId, Owner owner, 
                                                 boolean validateOnlyChangedFields, 
                                                 String... fieldNames);
    
    /**
     * Check if the validation service is currently available
     * Uses circuit breaker pattern to determine service health
     * 
     * @return true if service is available, false if using fallback mode
     */
    boolean isValidationServiceAvailable();
    
    /**
     * Get the current circuit breaker state for monitoring
     * 
     * @return Current circuit breaker state (CLOSED, OPEN, HALF_OPEN)
     */
    String getCircuitBreakerState();
    
    /**
     * Get the current failure count for monitoring
     * 
     * @return Number of consecutive failures
     */
    int getCurrentFailureCount();
}