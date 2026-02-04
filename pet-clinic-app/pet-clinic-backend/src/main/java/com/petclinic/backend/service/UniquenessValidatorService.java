package com.petclinic.backend.service;

import com.petclinic.backend.dto.UniquenessValidationResult;

/**
 * Service interface for validating uniqueness constraints
 * Validates: Requirements 2.2, 2.3, 2.4
 */
public interface UniquenessValidatorService {
    
    /**
     * Validates mobile number uniqueness for new owner creation
     * @param mobileNumber the mobile number to check for uniqueness
     * @return validation result indicating if the mobile number is unique
     */
    UniquenessValidationResult validateMobileNumberUniqueness(String mobileNumber);
    
    /**
     * Validates mobile number uniqueness for owner updates
     * Allows the same owner to keep their existing mobile number
     * @param mobileNumber the mobile number to check for uniqueness
     * @param excludeOwnerId the owner ID to exclude from uniqueness check (for updates)
     * @return validation result indicating if the mobile number is unique
     */
    UniquenessValidationResult validateMobileNumberUniqueness(String mobileNumber, Long excludeOwnerId);
    
    /**
     * Validates email uniqueness for new owner creation
     * @param email the email address to check for uniqueness
     * @return validation result indicating if the email is unique
     */
    UniquenessValidationResult validateEmailUniqueness(String email);
    
    /**
     * Validates email uniqueness for owner updates
     * Allows the same owner to keep their existing email
     * @param email the email address to check for uniqueness
     * @param excludeOwnerId the owner ID to exclude from uniqueness check (for updates)
     * @return validation result indicating if the email is unique
     */
    UniquenessValidationResult validateEmailUniqueness(String email, Long excludeOwnerId);
}