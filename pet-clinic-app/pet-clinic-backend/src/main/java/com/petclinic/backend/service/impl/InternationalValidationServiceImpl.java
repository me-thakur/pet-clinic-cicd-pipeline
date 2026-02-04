package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.InternationalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Implementation of InternationalValidationService for international postal code validation
 * Supports common international formats with flexible fallback validation
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Service
public class InternationalValidationServiceImpl implements InternationalValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InternationalValidationServiceImpl.class);
    
    // Country-specific postal code patterns
    private static final Map<String, Pattern> COUNTRY_PATTERNS = Map.of(
        "US", Pattern.compile("^\\d{5}(-\\d{4})?$"),                    // 12345 or 12345-6789
        "CA", Pattern.compile("^[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d$"),       // K1A 0A6 or K1A0A6
        "UK", Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}$"), // SW1A 1AA or M1 1AA
        "DE", Pattern.compile("^\\d{5}$"),                              // 12345
        "FR", Pattern.compile("^\\d{5}$"),                              // 75001
        "IN", Pattern.compile("^\\d{6}$"),                              // 110001
        "AU", Pattern.compile("^\\d{4}$"),                              // 2000
        "JP", Pattern.compile("^\\d{3}-\\d{4}$")                        // 100-0001
    );
    
    // Country-specific examples
    private static final Map<String, String> COUNTRY_EXAMPLES = Map.of(
        "US", "12345 or 12345-6789",
        "CA", "K1A 0A6 or K1A0A6", 
        "UK", "SW1A 1AA or M1 1AA",
        "DE", "12345",
        "FR", "75001", 
        "IN", "110001",
        "AU", "2000",
        "JP", "100-0001"
    );
    
    // Country names for user-friendly messages
    private static final Map<String, String> COUNTRY_NAMES = Map.of(
        "US", "United States",
        "CA", "Canada",
        "UK", "United Kingdom", 
        "DE", "Germany",
        "FR", "France",
        "IN", "India",
        "AU", "Australia",
        "JP", "Japan"
    );
    
    // Flexible pattern for unknown formats - accepts alphanumeric with spaces and hyphens
    private static final Pattern FLEXIBLE_PATTERN = 
        Pattern.compile("^[A-Z0-9\\s-]{3,10}$", Pattern.CASE_INSENSITIVE);
    
    @Override
    public FormatValidationResult validatePostalCode(String postalCode, String countryCode) {
        logger.debug("Validating postal code '{}' for country '{}'", postalCode, countryCode);
        
        if (postalCode == null || postalCode.trim().isEmpty()) {
            // Postal code is optional - return valid for empty values
            logger.debug("Postal code is empty - treating as valid (optional field)");
            return FormatValidationResult.valid();
        }
        
        if (countryCode == null || countryCode.trim().isEmpty()) {
            logger.debug("No country code provided - using flexible validation");
            return validatePostalCodeFlexible(postalCode);
        }
        
        String trimmedPostalCode = postalCode.trim();
        String upperCountryCode = countryCode.trim().toUpperCase();
        
        Pattern pattern = COUNTRY_PATTERNS.get(upperCountryCode);
        if (pattern == null) {
            logger.debug("Country code '{}' not supported - using flexible validation", upperCountryCode);
            return validatePostalCodeFlexible(postalCode);
        }
        
        // Normalize postal code for validation (handle case and spacing)
        String normalizedPostalCode = normalizePostalCode(trimmedPostalCode, upperCountryCode);
        
        if (pattern.matcher(normalizedPostalCode).matches()) {
            logger.debug("Postal code '{}' is valid for country '{}'", normalizedPostalCode, upperCountryCode);
            return FormatValidationResult.valid();
        } else {
            String countryName = COUNTRY_NAMES.get(upperCountryCode);
            String example = COUNTRY_EXAMPLES.get(upperCountryCode);
            
            logger.debug("Postal code '{}' is invalid for country '{}' ({})", 
                        normalizedPostalCode, upperCountryCode, countryName);
            
            return FormatValidationResult.invalid(
                String.format("Invalid postal code format for %s", countryName),
                String.format("Please provide a valid %s postal code. Expected format: %s", 
                             countryName, example),
                trimmedPostalCode,
                example,
                "POSTAL_CODE_INVALID_FORMAT_" + upperCountryCode
            );
        }
    }
    
    @Override
    public FormatValidationResult validatePostalCodeFlexible(String postalCode) {
        logger.debug("Performing flexible validation for postal code '{}'", postalCode);
        
        if (postalCode == null || postalCode.trim().isEmpty()) {
            // Postal code is optional - return valid for empty values
            logger.debug("Postal code is empty - treating as valid (optional field)");
            return FormatValidationResult.valid();
        }
        
        String trimmedPostalCode = postalCode.trim();
        
        // Check length constraints
        if (trimmedPostalCode.length() < 3) {
            logger.debug("Postal code '{}' is too short (minimum 3 characters)", trimmedPostalCode);
            return FormatValidationResult.invalid(
                "Postal code is too short",
                "Please provide a postal code with at least 3 characters. Most postal codes are between 3-10 characters",
                trimmedPostalCode,
                getAllPostalCodeExamples(),
                "POSTAL_CODE_TOO_SHORT"
            );
        }
        
        if (trimmedPostalCode.length() > 10) {
            logger.debug("Postal code '{}' is too long (maximum 10 characters)", trimmedPostalCode);
            return FormatValidationResult.invalid(
                "Postal code is too long",
                "Please provide a postal code with at most 10 characters. Most postal codes are between 3-10 characters",
                trimmedPostalCode,
                getAllPostalCodeExamples(),
                "POSTAL_CODE_TOO_LONG"
            );
        }
        
        // Check against flexible pattern
        if (FLEXIBLE_PATTERN.matcher(trimmedPostalCode).matches()) {
            logger.debug("Postal code '{}' passes flexible validation", trimmedPostalCode);
            
            // Try to detect country for additional context
            String detectedCountry = detectCountryFromPostalCode(trimmedPostalCode);
            if (detectedCountry != null) {
                logger.debug("Detected likely country '{}' for postal code '{}'", detectedCountry, trimmedPostalCode);
            }
            
            return FormatValidationResult.valid();
        } else {
            logger.debug("Postal code '{}' fails flexible validation", trimmedPostalCode);
            return FormatValidationResult.invalid(
                "Invalid postal code format",
                "Please provide a valid postal code using only letters, numbers, spaces, and hyphens. Remove any special characters",
                trimmedPostalCode,
                getAllPostalCodeExamples(),
                "POSTAL_CODE_INVALID_CHARACTERS"
            );
        }
    }
    
    @Override
    public List<String> getSupportedPostalCodeFormats() {
        List<String> formats = new ArrayList<>();
        for (Map.Entry<String, String> entry : COUNTRY_EXAMPLES.entrySet()) {
            String countryCode = entry.getKey();
            String example = entry.getValue();
            String countryName = COUNTRY_NAMES.get(countryCode);
            formats.add(String.format("%s (%s): %s", countryName, countryCode, example));
        }
        return formats;
    }
    
    @Override
    public String getPostalCodeExample(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        return COUNTRY_EXAMPLES.get(countryCode.toUpperCase());
    }
    
    @Override
    public String getAllPostalCodeExamples() {
        StringBuilder examples = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, String> entry : COUNTRY_EXAMPLES.entrySet()) {
            if (!first) {
                examples.append(", ");
            }
            String countryCode = entry.getKey();
            String example = entry.getValue();
            String countryName = COUNTRY_NAMES.get(countryCode);
            examples.append(String.format("%s: %s", countryName, example));
            first = false;
        }
        
        return examples.toString();
    }
    
    @Override
    public String normalizePostalCode(String postalCode, String countryCode) {
        if (postalCode == null || countryCode == null) {
            return postalCode;
        }
        
        String trimmed = postalCode.trim();
        String upperCountryCode = countryCode.toUpperCase();
        
        switch (upperCountryCode) {
            case "CA":
                // Canadian postal codes: normalize to uppercase and ensure space
                String canadianNormalized = trimmed.toUpperCase().replaceAll("\\s+", "");
                if (canadianNormalized.length() == 6) {
                    return canadianNormalized.substring(0, 3) + " " + canadianNormalized.substring(3);
                }
                return trimmed.toUpperCase();
                
            case "UK":
                // UK postal codes: normalize to uppercase and ensure proper spacing
                String ukNormalized = trimmed.toUpperCase().replaceAll("\\s+", " ");
                // Add space before last 3 characters if not present
                if (ukNormalized.length() >= 5 && !ukNormalized.contains(" ")) {
                    int spacePos = ukNormalized.length() - 3;
                    return ukNormalized.substring(0, spacePos) + " " + ukNormalized.substring(spacePos);
                }
                return ukNormalized;
                
            case "JP":
                // Japanese postal codes: only normalize if it already has hyphen or is exactly 7 digits
                if (trimmed.contains("-")) {
                    // Already has hyphen, just return as-is
                    return trimmed;
                } else if (trimmed.matches("^\\d{7}$")) {
                    // Exactly 7 digits, add hyphen
                    return trimmed.substring(0, 3) + "-" + trimmed.substring(3);
                }
                // Otherwise, return original to let validation handle it
                return trimmed;
                
            default:
                // For other countries, just trim and return
                return trimmed;
        }
    }
    
    @Override
    public String detectCountryFromPostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = postalCode.trim();
        
        // First check for unique patterns that can definitively identify a country
        if (trimmed.matches("^\\d{6}$")) {
            // Likely India (6 digits is unique to India in our supported countries)
            return "IN";
        }
        
        if (trimmed.matches("^\\d{4}$")) {
            // Likely Australia (4 digits is unique to Australia in our supported countries)
            return "AU";
        }
        
        if (trimmed.matches("^[A-Z]\\d[A-Z].*")) {
            // Likely Canada (starts with letter-digit-letter pattern)
            return "CA";
        }
        
        if (trimmed.matches("^[A-Z]{1,2}\\d.*")) {
            // Likely UK (starts with 1-2 letters followed by digit)
            return "UK";
        }
        
        if (trimmed.matches("^\\d{3}-\\d{4}$")) {
            // Likely Japan (XXX-XXXX format is unique to Japan)
            return "JP";
        }
        
        // For ambiguous patterns (like 5-digit codes), don't guess
        if (trimmed.matches("^\\d{5}$")) {
            // Could be US (without extension), DE, or FR - can't determine definitively
            return null;
        }
        
        logger.debug("Could not detect country for postal code '{}'", trimmed);
        return null;
    }
}