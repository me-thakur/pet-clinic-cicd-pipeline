package com.petclinic.backend.service;

import com.petclinic.backend.dto.FormatValidationResult;

import java.util.List;

/**
 * Service interface for international validation including postal codes
 * Provides flexible validation for various international formats
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
public interface InternationalValidationService {
    
    /**
     * Validates postal code format for a specific country
     * @param postalCode the postal code to validate
     * @param countryCode the ISO country code (e.g., "US", "CA", "UK", "DE", "FR", "IN", "AU", "JP")
     * @return validation result with format compliance information
     */
    FormatValidationResult validatePostalCode(String postalCode, String countryCode);
    
    /**
     * Validates postal code format using flexible validation for unknown formats
     * Accepts common international postal code patterns when country is not specified
     * @param postalCode the postal code to validate
     * @return validation result with format compliance information
     */
    FormatValidationResult validatePostalCodeFlexible(String postalCode);
    
    /**
     * Gets list of supported postal code formats with examples
     * @return list of supported country codes with format descriptions
     */
    List<String> getSupportedPostalCodeFormats();
    
    /**
     * Gets postal code format example for a specific country
     * @param countryCode the ISO country code
     * @return example postal code format for the country, or null if not supported
     */
    String getPostalCodeExample(String countryCode);
    
    /**
     * Gets all postal code format examples as a formatted string
     * @return formatted string with examples for all supported countries
     */
    String getAllPostalCodeExamples();
    
    /**
     * Normalizes postal code format for a specific country
     * @param postalCode the postal code to normalize
     * @param countryCode the ISO country code
     * @return normalized postal code, or original if normalization not applicable
     */
    String normalizePostalCode(String postalCode, String countryCode);
    
    /**
     * Detects likely country from postal code format
     * @param postalCode the postal code to analyze
     * @return likely country code, or null if cannot be determined
     */
    String detectCountryFromPostalCode(String postalCode);
}