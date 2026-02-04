package com.petclinic.backend.properties;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.InternationalValidationService;
import com.petclinic.backend.service.impl.InternationalValidationServiceImpl;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for international postal code validation
 * **Feature: critical-fixes-and-enhancements, Property 6: International Postal Code Validation**
 * **Validates: Requirements 3.3**
 * 
 * Tests that for any valid international postal code format (including letters, numbers, 
 * and common patterns), the validation system should accept it as valid.
 */
@DisplayName("International Postal Code Validation Properties")
class InternationalPostalCodeValidationProperties extends PropertyTestBase {
    
    private InternationalValidationService validationService;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        validationService = new InternationalValidationServiceImpl();
    }
    
    /**
     * Property 6: International Postal Code Validation
     * For any valid international postal code format (including letters, numbers, and common patterns),
     * the validation system should accept it as valid.
     * **Validates: Requirements 3.3**
     */
    @Test
    @DisplayName("Property 6: Valid international postal codes should always be accepted")
    void validInternationalPostalCodesShouldAlwaysBeAccepted() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate a valid international postal code
            String postalCode = validInternationalPostalCodes().next();
            
            // Test with flexible validation (no country code)
            FormatValidationResult flexibleResult = validationService.validatePostalCodeFlexible(postalCode);
            assertTrue(flexibleResult.isValid(), 
                "Valid international postal code '" + postalCode + "' should be accepted by flexible validation");
            
            // Test with country-specific validation if we can detect the country
            String detectedCountry = validationService.detectCountryFromPostalCode(postalCode);
            if (detectedCountry != null) {
                FormatValidationResult countryResult = validationService.validatePostalCode(postalCode, detectedCountry);
                assertTrue(countryResult.isValid(), 
                    "Valid postal code '" + postalCode + "' should be accepted for detected country '" + detectedCountry + "'");
            }
        });
    }
    
    /**
     * Property test for country-specific postal code validation
     * Tests that valid postal codes for each supported country are always accepted
     */
    @Test
    @DisplayName("Valid country-specific postal codes should always be accepted")
    void validCountrySpecificPostalCodesShouldAlwaysBeAccepted() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate a country and its valid postal code
            String country = supportedCountries().next();
            String postalCode = validPostalCodeForCountry(country).next();
            
            // Test country-specific validation
            FormatValidationResult result = validationService.validatePostalCode(postalCode, country);
            assertTrue(result.isValid(), 
                "Valid postal code '" + postalCode + "' should be accepted for country '" + country + "'");
            
            // Test flexible validation should also accept it
            FormatValidationResult flexibleResult = validationService.validatePostalCodeFlexible(postalCode);
            assertTrue(flexibleResult.isValid(), 
                "Valid postal code '" + postalCode + "' should also be accepted by flexible validation");
        });
    }
    
    /**
     * Property test for postal code normalization consistency
     * Tests that normalized postal codes are always valid for their country
     */
    @Test
    @DisplayName("Normalized postal codes should always be valid")
    void normalizedPostalCodesShouldAlwaysBeValid() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String country = supportedCountries().next();
            String originalPostalCode = validPostalCodeForCountry(country).next();
            
            // Normalize the postal code
            String normalizedPostalCode = validationService.normalizePostalCode(originalPostalCode, country);
            
            // The normalized postal code should be valid
            FormatValidationResult result = validationService.validatePostalCode(normalizedPostalCode, country);
            assertTrue(result.isValid(), 
                "Normalized postal code '" + normalizedPostalCode + "' (from '" + originalPostalCode + 
                "') should be valid for country '" + country + "'");
        });
    }
    
    /**
     * Property test for empty postal code handling
     * Tests that empty postal codes are always treated as valid (optional field)
     */
    @Test
    @DisplayName("Empty postal codes should always be treated as valid")
    void emptyPostalCodesShouldAlwaysBeValid() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String country = supportedCountries().next();
            String emptyPostalCode = emptyPostalCodes().next();
            
            // Test country-specific validation
            FormatValidationResult countryResult = validationService.validatePostalCode(emptyPostalCode, country);
            assertTrue(countryResult.isValid(), 
                "Empty postal code '" + emptyPostalCode + "' should be valid for country '" + country + "'");
            
            // Test flexible validation
            FormatValidationResult flexibleResult = validationService.validatePostalCodeFlexible(emptyPostalCode);
            assertTrue(flexibleResult.isValid(), 
                "Empty postal code '" + emptyPostalCode + "' should be valid for flexible validation");
        });
    }
    
    /**
     * Property test for alphanumeric postal code acceptance
     * Tests that postal codes with letters and numbers are accepted
     */
    @Test
    @DisplayName("Alphanumeric postal codes should be accepted by flexible validation")
    void alphanumericPostalCodesShouldBeAcceptedByFlexibleValidation() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String alphanumericPostalCode = validAlphanumericPostalCodes().next();
            
            FormatValidationResult result = validationService.validatePostalCodeFlexible(alphanumericPostalCode);
            assertTrue(result.isValid(), 
                "Alphanumeric postal code '" + alphanumericPostalCode + "' should be accepted by flexible validation");
        });
    }
    
    // Generator methods for property-based testing
    
    /**
     * Generator for valid international postal codes from all supported countries
     */
    protected Generator<String> validInternationalPostalCodes() {
        return () -> {
            String country = supportedCountries().next();
            return validPostalCodeForCountry(country).next();
        };
    }
    
    /**
     * Generator for supported country codes
     */
    protected Generator<String> supportedCountries() {
        String[] countries = {"US", "CA", "UK", "DE", "FR", "IN", "AU", "JP"};
        return () -> countries[random.nextInt(countries.length)];
    }
    
    /**
     * Generator for valid postal codes for a specific country
     */
    protected Generator<String> validPostalCodeForCountry(String country) {
        return () -> {
            switch (country) {
                case "US":
                    return generateUSPostalCode();
                case "CA":
                    return generateCanadianPostalCode();
                case "UK":
                    return generateUKPostalCode();
                case "DE":
                    return generateGermanPostalCode();
                case "FR":
                    return generateFrenchPostalCode();
                case "IN":
                    return generateIndianPostalCode();
                case "AU":
                    return generateAustralianPostalCode();
                case "JP":
                    return generateJapanesePostalCode();
                default:
                    return generateGenericPostalCode();
            }
        };
    }
    
    /**
     * Generator for empty postal codes (null, empty string, whitespace)
     */
    protected Generator<String> emptyPostalCodes() {
        String[] emptyValues = {null, "", "   ", "\t", "\n"};
        return () -> emptyValues[random.nextInt(emptyValues.length)];
    }
    
    /**
     * Generator for valid alphanumeric postal codes
     */
    protected Generator<String> validAlphanumericPostalCodes() {
        return () -> {
            StringBuilder postalCode = new StringBuilder();
            int baseLength = 3 + random.nextInt(6); // 3-8 base characters to leave room for separators
            
            for (int i = 0; i < baseLength; i++) {
                if (random.nextBoolean()) {
                    // Add letter
                    postalCode.append((char) ('A' + random.nextInt(26)));
                } else {
                    // Add digit
                    postalCode.append(random.nextInt(10));
                }
                
                // Occasionally add space or hyphen (but not at start/end, and ensure we don't exceed 10 chars)
                if (i > 0 && i < baseLength - 1 && random.nextInt(15) == 0 && postalCode.length() < 9) {
                    postalCode.append(random.nextBoolean() ? " " : "-");
                }
            }
            
            // Ensure we don't exceed 10 characters
            String result = postalCode.toString();
            if (result.length() > 10) {
                result = result.substring(0, 10);
            }
            
            return result;
        };
    }
    
    // Country-specific postal code generators
    
    private String generateUSPostalCode() {
        if (random.nextBoolean()) {
            // 5-digit ZIP code
            return String.format("%05d", random.nextInt(100000));
        } else {
            // ZIP+4 format
            return String.format("%05d-%04d", random.nextInt(100000), random.nextInt(10000));
        }
    }
    
    private String generateCanadianPostalCode() {
        // Format: A1A 1A1 or A1A1A1
        char[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        String pattern = String.format("%c%d%c%s%d%c%d",
            letters[random.nextInt(letters.length)],
            random.nextInt(10),
            letters[random.nextInt(letters.length)],
            random.nextBoolean() ? " " : "", // Sometimes with space, sometimes without
            random.nextInt(10),
            letters[random.nextInt(letters.length)],
            random.nextInt(10)
        );
        return pattern;
    }
    
    private String generateUKPostalCode() {
        char[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        
        if (random.nextBoolean()) {
            // Format: SW1A 1AA (2 letters + digit + letter + space + digit + 2 letters)
            return String.format("%c%c%d%c%s%d%c%c",
                letters[random.nextInt(letters.length)],
                letters[random.nextInt(letters.length)],
                random.nextInt(10),
                letters[random.nextInt(letters.length)],
                random.nextBoolean() ? " " : "", // Sometimes with space
                random.nextInt(10),
                letters[random.nextInt(letters.length)],
                letters[random.nextInt(letters.length)]
            );
        } else {
            // Format: M1 1AA (letter + digit + space + digit + 2 letters)
            return String.format("%c%d%s%d%c%c",
                letters[random.nextInt(letters.length)],
                random.nextInt(10),
                random.nextBoolean() ? " " : "", // Sometimes with space
                random.nextInt(10),
                letters[random.nextInt(letters.length)],
                letters[random.nextInt(letters.length)]
            );
        }
    }
    
    private String generateGermanPostalCode() {
        // 5-digit format
        return String.format("%05d", random.nextInt(100000));
    }
    
    private String generateFrenchPostalCode() {
        // 5-digit format
        return String.format("%05d", random.nextInt(100000));
    }
    
    private String generateIndianPostalCode() {
        // 6-digit format
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private String generateAustralianPostalCode() {
        // 4-digit format
        return String.format("%04d", random.nextInt(10000));
    }
    
    private String generateJapanesePostalCode() {
        if (random.nextBoolean()) {
            // Format: 100-0001
            return String.format("%03d-%04d", random.nextInt(1000), random.nextInt(10000));
        } else {
            // Format: 1000001 (7 digits, should be normalized to 100-0001)
            return String.format("%07d", random.nextInt(10000000));
        }
    }
    
    private String generateGenericPostalCode() {
        // Generic alphanumeric postal code (3-10 characters)
        StringBuilder postalCode = new StringBuilder();
        int length = 3 + random.nextInt(8); // 3-10 characters
        
        for (int i = 0; i < length; i++) {
            if (random.nextBoolean()) {
                postalCode.append((char) ('A' + random.nextInt(26)));
            } else {
                postalCode.append(random.nextInt(10));
            }
        }
        
        return postalCode.toString();
    }
}