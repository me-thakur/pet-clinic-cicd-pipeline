package com.petclinic.backend.service;

import com.petclinic.backend.dto.FormatValidationResult;

/**
 * Service interface for validating data formats against international standards
 * Validates: Requirements 1.1, 2.1, 2.5
 */
public interface FormatValidatorService {
    
    /**
     * Validates mobile number format using international E.164 standard
     * @param mobileNumber the mobile number to validate
     * @return validation result with format compliance information
     */
    FormatValidationResult validateMobileNumber(String mobileNumber);
    
    /**
     * Validates mobile number format for a specific country
     * @param mobileNumber the mobile number to validate
     * @param countryCode the ISO country code (e.g., "US", "GB", "DE")
     * @return validation result with format compliance information
     */
    FormatValidationResult validateMobileNumber(String mobileNumber, String countryCode);
    
    /**
     * Validates email format using RFC 5322 standard
     * @param email the email address to validate
     * @return validation result with format compliance information
     */
    FormatValidationResult validateEmail(String email);
    
    /**
     * Validates name format (first name, last name)
     * @param name the name to validate
     * @return validation result with format compliance information
     */
    FormatValidationResult validateName(String name);
    
    /**
     * Validates postal code format using international validation
     * @param postalCode the postal code to validate
     * @return validation result with format compliance information
     */
    FormatValidationResult validatePostalCode(String postalCode);
    
    /**
     * Validates postal code format for a specific country
     * @param postalCode the postal code to validate
     * @param countryCode the ISO country code (e.g., "US", "CA", "UK")
     * @return validation result with format compliance information
     */
    FormatValidationResult validatePostalCode(String postalCode, String countryCode);
}