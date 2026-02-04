package com.petclinic.backend.properties;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.InternationalValidationService;
import com.petclinic.backend.service.impl.InternationalValidationServiceImpl;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for invalid postal code error messages
 * **Feature: critical-fixes-and-enhancements, Property 7: Invalid Postal Code Error Messages**
 * **Validates: Requirements 3.5**
 * 
 * Tests that for any invalid postal code format, the validation system should provide 
 * clear error messages with format examples and corrective guidance.
 */
@DisplayName("Invalid Postal Code Error Message Properties")
class InvalidPostalCodeErrorMessageProperties extends PropertyTestBase {
    
    private InternationalValidationService validationService;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        validationService = new InternationalValidationServiceImpl();
    }
    
    /**
     * Property 7: Invalid Postal Code Error Messages
     * For any invalid postal code format, the validation system should provide clear error messages 
     * with format examples and corrective guidance.
     * **Validates: Requirements 3.5**
     */
    @Test
    @DisplayName("Property 7: Invalid postal codes should provide clear error messages with examples and guidance")
    void invalidPostalCodesShouldProvideErrorMessagesWithExamplesAndGuidance() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate an invalid postal code
            String invalidPostalCode = invalidPostalCodes().next();
            
            // Test with flexible validation
            FormatValidationResult flexibleResult = validationService.validatePostalCodeFlexible(invalidPostalCode);
            
            // Should be invalid
            assertFalse(flexibleResult.isValid(), 
                "Invalid postal code '" + invalidPostalCode + "' should be rejected");
            
            // Should have clear error message
            assertNotNull(flexibleResult.getErrorMessage(), 
                "Invalid postal code '" + invalidPostalCode + "' should have an error message");
            assertFalse(flexibleResult.getErrorMessage().trim().isEmpty(), 
                "Error message for invalid postal code '" + invalidPostalCode + "' should not be empty");
            
            // Should have corrective guidance
            assertNotNull(flexibleResult.getCorrectionGuidance(), 
                "Invalid postal code '" + invalidPostalCode + "' should have corrective guidance");
            assertFalse(flexibleResult.getCorrectionGuidance().trim().isEmpty(), 
                "Corrective guidance for invalid postal code '" + invalidPostalCode + "' should not be empty");
            
            // Should have format examples
            assertNotNull(flexibleResult.getFormatExample(), 
                "Invalid postal code '" + invalidPostalCode + "' should have format examples");
            assertFalse(flexibleResult.getFormatExample().trim().isEmpty(), 
                "Format examples for invalid postal code '" + invalidPostalCode + "' should not be empty");
            
            // Should have error code for categorization
            assertNotNull(flexibleResult.getErrorCode(), 
                "Invalid postal code '" + invalidPostalCode + "' should have an error code");
            assertFalse(flexibleResult.getErrorCode().trim().isEmpty(), 
                "Error code for invalid postal code '" + invalidPostalCode + "' should not be empty");
            
            // Should capture the rejected value
            assertNotNull(flexibleResult.getRejectedValue(), 
                "Invalid postal code '" + invalidPostalCode + "' should have the rejected value captured");
        });
    }
    
    /**
     * Property test for country-specific invalid postal code error messages
     * Tests that invalid postal codes for specific countries provide country-specific guidance
     */
    @Test
    @DisplayName("Country-specific invalid postal codes should provide country-specific error messages")
    void countrySpecificInvalidPostalCodesShouldProvideCountrySpecificErrorMessages() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate a country and an invalid postal code for that country
            String country = supportedCountries().next();
            String invalidPostalCode = invalidPostalCodeForCountry(country).next();
            
            // Test country-specific validation
            FormatValidationResult result = validationService.validatePostalCode(invalidPostalCode, country);
            
            // Should be invalid
            assertFalse(result.isValid(), 
                "Invalid postal code '" + invalidPostalCode + "' should be rejected for country '" + country + "'");
            
            // Should have clear error message mentioning the country
            assertNotNull(result.getErrorMessage(), 
                "Invalid postal code '" + invalidPostalCode + "' should have an error message for country '" + country + "'");
            String errorMessage = result.getErrorMessage().toLowerCase();
            assertTrue(errorMessage.contains(getCountryName(country).toLowerCase()) || errorMessage.contains(country.toLowerCase()), 
                "Error message should mention the country name or code for invalid postal code '" + invalidPostalCode + "' and country '" + country + "'");
            
            // Should have corrective guidance with country-specific format
            assertNotNull(result.getCorrectionGuidance(), 
                "Invalid postal code '" + invalidPostalCode + "' should have corrective guidance for country '" + country + "'");
            String guidance = result.getCorrectionGuidance().toLowerCase();
            assertTrue(guidance.contains(getCountryName(country).toLowerCase()) || guidance.contains("format"), 
                "Corrective guidance should mention the country or format for invalid postal code '" + invalidPostalCode + "' and country '" + country + "'");
            
            // Should have country-specific format examples
            assertNotNull(result.getFormatExample(), 
                "Invalid postal code '" + invalidPostalCode + "' should have format examples for country '" + country + "'");
            String expectedExample = getExpectedExampleForCountry(country);
            if (expectedExample != null) {
                assertTrue(result.getFormatExample().contains(expectedExample), 
                    "Format example should contain expected format '" + expectedExample + "' for country '" + country + "'");
            }
            
            // Should have country-specific error code
            assertNotNull(result.getErrorCode(), 
                "Invalid postal code '" + invalidPostalCode + "' should have an error code for country '" + country + "'");
            assertTrue(result.getErrorCode().contains(country) || result.getErrorCode().contains("POSTAL_CODE"), 
                "Error code should be country-specific or postal-code-specific for invalid postal code '" + invalidPostalCode + "' and country '" + country + "'");
        });
    }
    
    /**
     * Property test for too short postal codes
     * Tests that postal codes that are too short provide specific guidance about length
     */
    @Test
    @DisplayName("Too short postal codes should provide length-specific error messages")
    void tooShortPostalCodesShouldProvideLengthSpecificErrorMessages() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String tooShortPostalCode = tooShortPostalCodes().next();
            
            FormatValidationResult result = validationService.validatePostalCodeFlexible(tooShortPostalCode);
            
            // Should be invalid
            assertFalse(result.isValid(), 
                "Too short postal code '" + tooShortPostalCode + "' should be rejected");
            
            // Error message should mention length issue
            assertNotNull(result.getErrorMessage(), 
                "Too short postal code '" + tooShortPostalCode + "' should have an error message");
            String errorMessage = result.getErrorMessage().toLowerCase();
            assertTrue(errorMessage.contains("short") || errorMessage.contains("length") || errorMessage.contains("character"), 
                "Error message should mention length issue for too short postal code '" + tooShortPostalCode + "'");
            
            // Corrective guidance should mention minimum length
            assertNotNull(result.getCorrectionGuidance(), 
                "Too short postal code '" + tooShortPostalCode + "' should have corrective guidance");
            String guidance = result.getCorrectionGuidance().toLowerCase();
            assertTrue(guidance.contains("3") || guidance.contains("character") || guidance.contains("least"), 
                "Corrective guidance should mention minimum length for too short postal code '" + tooShortPostalCode + "'");
            
            // Should have error code indicating length issue
            assertNotNull(result.getErrorCode(), 
                "Too short postal code '" + tooShortPostalCode + "' should have an error code");
            assertTrue(result.getErrorCode().contains("SHORT") || result.getErrorCode().contains("LENGTH"), 
                "Error code should indicate length issue for too short postal code '" + tooShortPostalCode + "'");
        });
    }
    
    /**
     * Property test for too long postal codes
     * Tests that postal codes that are too long provide specific guidance about length
     */
    @Test
    @DisplayName("Too long postal codes should provide length-specific error messages")
    void tooLongPostalCodesShouldProvideLengthSpecificErrorMessages() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String tooLongPostalCode = tooLongPostalCodes().next();
            
            FormatValidationResult result = validationService.validatePostalCodeFlexible(tooLongPostalCode);
            
            // Should be invalid
            assertFalse(result.isValid(), 
                "Too long postal code '" + tooLongPostalCode + "' should be rejected");
            
            // Error message should mention length issue
            assertNotNull(result.getErrorMessage(), 
                "Too long postal code '" + tooLongPostalCode + "' should have an error message");
            String errorMessage = result.getErrorMessage().toLowerCase();
            assertTrue(errorMessage.contains("long") || errorMessage.contains("length") || errorMessage.contains("character"), 
                "Error message should mention length issue for too long postal code '" + tooLongPostalCode + "'");
            
            // Corrective guidance should mention maximum length
            assertNotNull(result.getCorrectionGuidance(), 
                "Too long postal code '" + tooLongPostalCode + "' should have corrective guidance");
            String guidance = result.getCorrectionGuidance().toLowerCase();
            assertTrue(guidance.contains("10") || guidance.contains("character") || guidance.contains("most"), 
                "Corrective guidance should mention maximum length for too long postal code '" + tooLongPostalCode + "'");
            
            // Should have error code indicating length issue
            assertNotNull(result.getErrorCode(), 
                "Too long postal code '" + tooLongPostalCode + "' should have an error code");
            assertTrue(result.getErrorCode().contains("LONG") || result.getErrorCode().contains("LENGTH"), 
                "Error code should indicate length issue for too long postal code '" + tooLongPostalCode + "'");
        });
    }
    
    /**
     * Property test for postal codes with invalid characters
     * Tests that postal codes with invalid characters provide specific guidance about allowed characters
     */
    @Test
    @DisplayName("Postal codes with invalid characters should provide character-specific error messages")
    void postalCodesWithInvalidCharactersShouldProvideCharacterSpecificErrorMessages() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String invalidCharacterPostalCode = invalidCharacterPostalCodes().next();
            
            FormatValidationResult result = validationService.validatePostalCodeFlexible(invalidCharacterPostalCode);
            
            // Should be invalid
            assertFalse(result.isValid(), 
                "Postal code with invalid characters '" + invalidCharacterPostalCode + "' should be rejected");
            
            // Error message should mention character issue
            assertNotNull(result.getErrorMessage(), 
                "Postal code with invalid characters '" + invalidCharacterPostalCode + "' should have an error message");
            String errorMessage = result.getErrorMessage().toLowerCase();
            assertTrue(errorMessage.contains("character") || errorMessage.contains("format") || errorMessage.contains("invalid"), 
                "Error message should mention character issue for postal code '" + invalidCharacterPostalCode + "'");
            
            // Corrective guidance should mention allowed characters
            assertNotNull(result.getCorrectionGuidance(), 
                "Postal code with invalid characters '" + invalidCharacterPostalCode + "' should have corrective guidance");
            String guidance = result.getCorrectionGuidance().toLowerCase();
            assertTrue(guidance.contains("letter") || guidance.contains("number") || guidance.contains("character") || guidance.contains("remove"), 
                "Corrective guidance should mention allowed characters for postal code '" + invalidCharacterPostalCode + "'");
            
            // Should have error code indicating character issue
            assertNotNull(result.getErrorCode(), 
                "Postal code with invalid characters '" + invalidCharacterPostalCode + "' should have an error code");
            assertTrue(result.getErrorCode().contains("CHARACTER") || result.getErrorCode().contains("INVALID"), 
                "Error code should indicate character issue for postal code '" + invalidCharacterPostalCode + "'");
        });
    }
    
    // Generator methods for invalid postal codes
    
    /**
     * Generator for various types of invalid postal codes (excluding empty ones since they're valid)
     */
    protected Generator<String> invalidPostalCodes() {
        return () -> {
            int type = random.nextInt(3);
            switch (type) {
                case 0:
                    return tooShortPostalCodes().next();
                case 1:
                    return tooLongPostalCodes().next();
                default:
                    return invalidCharacterPostalCodes().next();
            }
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
     * Generator for invalid postal codes for specific countries
     */
    protected Generator<String> invalidPostalCodeForCountry(String country) {
        return () -> {
            switch (country) {
                case "US":
                    return generateInvalidUSPostalCode();
                case "CA":
                    return generateInvalidCanadianPostalCode();
                case "UK":
                    return generateInvalidUKPostalCode();
                case "DE":
                    return generateInvalidGermanPostalCode();
                case "FR":
                    return generateInvalidFrenchPostalCode();
                case "IN":
                    return generateInvalidIndianPostalCode();
                case "AU":
                    return generateInvalidAustralianPostalCode();
                case "JP":
                    return generateInvalidJapanesePostalCode();
                default:
                    return invalidCharacterPostalCodes().next();
            }
        };
    }
    
    /**
     * Generator for postal codes that are too short (less than 3 characters, but not empty)
     */
    protected Generator<String> tooShortPostalCodes() {
        return () -> {
            int length = 1 + random.nextInt(2); // 1-2 characters (not empty since empty is valid)
            
            StringBuilder postalCode = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    postalCode.append((char) ('A' + random.nextInt(26)));
                } else {
                    postalCode.append(random.nextInt(10));
                }
            }
            return postalCode.toString();
        };
    }
    
    /**
     * Generator for postal codes that are too long (more than 10 characters)
     */
    protected Generator<String> tooLongPostalCodes() {
        return () -> {
            int length = 11 + random.nextInt(10); // 11-20 characters
            StringBuilder postalCode = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    postalCode.append((char) ('A' + random.nextInt(26)));
                } else {
                    postalCode.append(random.nextInt(10));
                }
            }
            return postalCode.toString();
        };
    }
    
    /**
     * Generator for postal codes with invalid characters (special characters not allowed)
     */
    protected Generator<String> invalidCharacterPostalCodes() {
        return () -> {
            StringBuilder postalCode = new StringBuilder();
            int length = 3 + random.nextInt(5); // 3-7 base characters
            
            // Add some valid characters first
            for (int i = 0; i < length - 1; i++) {
                if (random.nextBoolean()) {
                    postalCode.append((char) ('A' + random.nextInt(26)));
                } else {
                    postalCode.append(random.nextInt(10));
                }
            }
            
            // Add at least one invalid character
            char[] invalidChars = {'@', '#', '$', '%', '^', '&', '*', '(', ')', '=', '+', '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.', '?', '/', '~', '`'};
            postalCode.append(invalidChars[random.nextInt(invalidChars.length)]);
            
            return postalCode.toString();
        };
    }
    
    /**
     * Generator for malformed postal codes (valid characters but wrong patterns)
     */
    protected Generator<String> malformedPostalCodes() {
        return () -> {
            // Create postal codes that have valid characters but don't match any country pattern
            StringBuilder postalCode = new StringBuilder();
            int length = 3 + random.nextInt(5); // 3-7 characters
            
            // Mix letters and numbers in patterns that don't match any country
            for (int i = 0; i < length; i++) {
                if (i % 2 == 0) {
                    postalCode.append((char) ('A' + random.nextInt(26)));
                } else {
                    postalCode.append(random.nextInt(10));
                }
            }
            
            return postalCode.toString();
        };
    }
    
    // Country-specific invalid postal code generators
    
    private String generateInvalidUSPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Too few digits
                return String.format("%d", random.nextInt(1000)); // 1-3 digits
            case 1:
                // Too many digits
                return String.format("%06d", random.nextInt(1000000)); // 6 digits
            default:
                // Invalid format with letters
                return String.format("%c%04d", 'A' + random.nextInt(26), random.nextInt(10000));
        }
    }
    
    private String generateInvalidCanadianPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Wrong pattern (should be A1A 1A1)
                return String.format("%d%c%d %c%d%c", 
                    random.nextInt(10), 'A' + random.nextInt(26), random.nextInt(10),
                    'A' + random.nextInt(26), random.nextInt(10), 'A' + random.nextInt(26));
            case 1:
                // Too short
                return String.format("%c%d%c", 'A' + random.nextInt(26), random.nextInt(10), 'A' + random.nextInt(26));
            default:
                // Invalid characters
                return String.format("%c%d%c@%d%c%d", 
                    'A' + random.nextInt(26), random.nextInt(10), 'A' + random.nextInt(26),
                    random.nextInt(10), 'A' + random.nextInt(26), random.nextInt(10));
        }
    }
    
    private String generateInvalidUKPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Wrong pattern - start with digits instead of letters
                return String.format("%d%d%d %d%c%c", 
                    random.nextInt(10), random.nextInt(10), random.nextInt(10),
                    random.nextInt(10), 'A' + random.nextInt(26), 'A' + random.nextInt(26));
            case 1:
                // Too many characters
                return String.format("%c%c%d%c%c %d%c%c%c", 
                    'A' + random.nextInt(26), 'A' + random.nextInt(26), random.nextInt(10), 'A' + random.nextInt(26), 'A' + random.nextInt(26),
                    random.nextInt(10), 'A' + random.nextInt(26), 'A' + random.nextInt(26), 'A' + random.nextInt(26));
            default:
                // Invalid characters
                return String.format("%c%c%d%c@%d%c%c", 
                    'A' + random.nextInt(26), 'A' + random.nextInt(26), random.nextInt(10), 'A' + random.nextInt(26),
                    random.nextInt(10), 'A' + random.nextInt(26), 'A' + random.nextInt(26));
        }
    }
    
    private String generateInvalidGermanPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Too few digits
                return String.format("%04d", random.nextInt(10000)); // 4 digits
            case 1:
                // Too many digits
                return String.format("%06d", random.nextInt(1000000)); // 6 digits
            default:
                // Letters in postal code
                return String.format("%c%04d", 'A' + random.nextInt(26), random.nextInt(10000));
        }
    }
    
    private String generateInvalidFrenchPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Too few digits
                return String.format("%04d", random.nextInt(10000)); // 4 digits
            case 1:
                // Too many digits
                return String.format("%06d", random.nextInt(1000000)); // 6 digits
            default:
                // Letters in postal code
                return String.format("%04d%c", random.nextInt(10000), 'A' + random.nextInt(26));
        }
    }
    
    private String generateInvalidIndianPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Too few digits
                return String.format("%05d", random.nextInt(100000)); // 5 digits
            case 1:
                // Too many digits
                return String.format("%07d", random.nextInt(10000000)); // 7 digits
            default:
                // Letters in postal code
                return String.format("%05d%c", random.nextInt(100000), 'A' + random.nextInt(26));
        }
    }
    
    private String generateInvalidAustralianPostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Too few digits
                return String.format("%03d", random.nextInt(1000)); // 3 digits
            case 1:
                // Too many digits
                return String.format("%05d", random.nextInt(100000)); // 5 digits
            default:
                // Letters in postal code
                return String.format("%03d%c", random.nextInt(1000), 'A' + random.nextInt(26));
        }
    }
    
    private String generateInvalidJapanesePostalCode() {
        int type = random.nextInt(3);
        switch (type) {
            case 0:
                // Wrong format (should be XXX-XXXX)
                return String.format("%04d-%03d", random.nextInt(10000), random.nextInt(1000));
            case 1:
                // Missing hyphen but wrong length
                return String.format("%06d", random.nextInt(1000000)); // 6 digits instead of 7
            default:
                // Letters in postal code
                return String.format("%03d-%03d%c", random.nextInt(1000), random.nextInt(1000), 'A' + random.nextInt(26));
        }
    }
    
    // Helper methods
    
    private String getCountryName(String countryCode) {
        switch (countryCode) {
            case "US": return "United States";
            case "CA": return "Canada";
            case "UK": return "United Kingdom";
            case "DE": return "Germany";
            case "FR": return "France";
            case "IN": return "India";
            case "AU": return "Australia";
            case "JP": return "Japan";
            default: return countryCode;
        }
    }
    
    private String getExpectedExampleForCountry(String countryCode) {
        switch (countryCode) {
            case "US": return "12345";
            case "CA": return "K1A";
            case "UK": return "SW1A";
            case "DE": return "12345";
            case "FR": return "75001";
            case "IN": return "110001";
            case "AU": return "2000";
            case "JP": return "100-0001";
            default: return null;
        }
    }
}