package com.petclinic.backend.service;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.impl.InternationalValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InternationalValidationService
 * Tests international postal code validation patterns and flexible validation
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@DisplayName("InternationalValidationService Tests")
class InternationalValidationServiceTest {
    
    private InternationalValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new InternationalValidationServiceImpl();
    }
    
    @Nested
    @DisplayName("Country-Specific Postal Code Validation")
    class CountrySpecificValidation {
        
        @Test
        @DisplayName("Should validate US postal codes correctly")
        void shouldValidateUSPostalCodes() {
            // Valid US postal codes
            assertTrue(validationService.validatePostalCode("12345", "US").isValid());
            assertTrue(validationService.validatePostalCode("12345-6789", "US").isValid());
            assertTrue(validationService.validatePostalCode("90210", "US").isValid());
            
            // Invalid US postal codes
            assertFalse(validationService.validatePostalCode("1234", "US").isValid());
            assertFalse(validationService.validatePostalCode("123456", "US").isValid());
            assertFalse(validationService.validatePostalCode("ABCDE", "US").isValid());
            assertFalse(validationService.validatePostalCode("12345-67890", "US").isValid());
        }
        
        @Test
        @DisplayName("Should validate Canadian postal codes correctly")
        void shouldValidateCanadianPostalCodes() {
            // Valid Canadian postal codes
            assertTrue(validationService.validatePostalCode("K1A 0A6", "CA").isValid());
            assertTrue(validationService.validatePostalCode("K1A0A6", "CA").isValid());
            assertTrue(validationService.validatePostalCode("M5V 3L9", "CA").isValid());
            
            // Invalid Canadian postal codes
            assertFalse(validationService.validatePostalCode("12345", "CA").isValid());
            assertFalse(validationService.validatePostalCode("K1A", "CA").isValid());
            assertFalse(validationService.validatePostalCode("K1A 0A", "CA").isValid());
        }
        
        @Test
        @DisplayName("Should validate UK postal codes correctly")
        void shouldValidateUKPostalCodes() {
            // Valid UK postal codes
            assertTrue(validationService.validatePostalCode("SW1A 1AA", "UK").isValid());
            assertTrue(validationService.validatePostalCode("M1 1AA", "UK").isValid());
            assertTrue(validationService.validatePostalCode("B33 8TH", "UK").isValid());
            assertTrue(validationService.validatePostalCode("W1A 0AX", "UK").isValid());
            
            // Invalid UK postal codes
            assertFalse(validationService.validatePostalCode("12345", "UK").isValid());
            assertFalse(validationService.validatePostalCode("SW1A", "UK").isValid());
        }
        
        @Test
        @DisplayName("Should validate German postal codes correctly")
        void shouldValidateGermanPostalCodes() {
            // Valid German postal codes
            assertTrue(validationService.validatePostalCode("12345", "DE").isValid());
            assertTrue(validationService.validatePostalCode("01067", "DE").isValid());
            
            // Invalid German postal codes
            assertFalse(validationService.validatePostalCode("1234", "DE").isValid());
            assertFalse(validationService.validatePostalCode("123456", "DE").isValid());
            assertFalse(validationService.validatePostalCode("ABCDE", "DE").isValid());
        }
        
        @Test
        @DisplayName("Should validate French postal codes correctly")
        void shouldValidateFrenchPostalCodes() {
            // Valid French postal codes
            assertTrue(validationService.validatePostalCode("75001", "FR").isValid());
            assertTrue(validationService.validatePostalCode("13008", "FR").isValid());
            
            // Invalid French postal codes
            assertFalse(validationService.validatePostalCode("7500", "FR").isValid());
            assertFalse(validationService.validatePostalCode("750001", "FR").isValid());
        }
        
        @Test
        @DisplayName("Should validate Indian postal codes correctly")
        void shouldValidateIndianPostalCodes() {
            // Valid Indian postal codes
            assertTrue(validationService.validatePostalCode("110001", "IN").isValid());
            assertTrue(validationService.validatePostalCode("400001", "IN").isValid());
            
            // Invalid Indian postal codes
            assertFalse(validationService.validatePostalCode("11000", "IN").isValid());
            assertFalse(validationService.validatePostalCode("1100001", "IN").isValid());
        }
        
        @Test
        @DisplayName("Should validate Australian postal codes correctly")
        void shouldValidateAustralianPostalCodes() {
            // Valid Australian postal codes
            assertTrue(validationService.validatePostalCode("2000", "AU").isValid());
            assertTrue(validationService.validatePostalCode("3000", "AU").isValid());
            
            // Invalid Australian postal codes
            assertFalse(validationService.validatePostalCode("200", "AU").isValid());
            assertFalse(validationService.validatePostalCode("20000", "AU").isValid());
        }
        
        @Test
        @DisplayName("Should validate Japanese postal codes correctly")
        void shouldValidateJapanesePostalCodes() {
            // Valid Japanese postal codes
            assertTrue(validationService.validatePostalCode("100-0001", "JP").isValid());
            assertTrue(validationService.validatePostalCode("150-0002", "JP").isValid());
            assertTrue(validationService.validatePostalCode("1000001", "JP").isValid()); // Should normalize to 100-0001
            
            // Invalid Japanese postal codes
            assertFalse(validationService.validatePostalCode("100-001", "JP").isValid());
            assertFalse(validationService.validatePostalCode("100001", "JP").isValid()); // Only 6 digits
            assertFalse(validationService.validatePostalCode("100-00001", "JP").isValid());
            assertFalse(validationService.validatePostalCode("10000001", "JP").isValid()); // 8 digits
        }
    }
    
    @Nested
    @DisplayName("Flexible Postal Code Validation")
    class FlexibleValidation {
        
        @Test
        @DisplayName("Should accept empty postal codes as valid (optional field)")
        void shouldAcceptEmptyPostalCodes() {
            assertTrue(validationService.validatePostalCodeFlexible(null).isValid());
            assertTrue(validationService.validatePostalCodeFlexible("").isValid());
            assertTrue(validationService.validatePostalCodeFlexible("   ").isValid());
        }
        
        @Test
        @DisplayName("Should accept various international formats")
        void shouldAcceptVariousInternationalFormats() {
            // Various valid international formats
            assertTrue(validationService.validatePostalCodeFlexible("12345").isValid());
            assertTrue(validationService.validatePostalCodeFlexible("K1A 0A6").isValid());
            assertTrue(validationService.validatePostalCodeFlexible("SW1A 1AA").isValid());
            assertTrue(validationService.validatePostalCodeFlexible("100-0001").isValid());
            assertTrue(validationService.validatePostalCodeFlexible("ABC123").isValid());
            assertTrue(validationService.validatePostalCodeFlexible("12-345").isValid());
        }
        
        @Test
        @DisplayName("Should reject postal codes that are too short")
        void shouldRejectTooShortPostalCodes() {
            FormatValidationResult result = validationService.validatePostalCodeFlexible("12");
            assertFalse(result.isValid());
            assertEquals("POSTAL_CODE_TOO_SHORT", result.getErrorCode());
            assertNotNull(result.getErrorMessage());
            assertNotNull(result.getCorrectionGuidance());
        }
        
        @Test
        @DisplayName("Should reject postal codes that are too long")
        void shouldRejectTooLongPostalCodes() {
            FormatValidationResult result = validationService.validatePostalCodeFlexible("12345678901");
            assertFalse(result.isValid());
            assertEquals("POSTAL_CODE_TOO_LONG", result.getErrorCode());
            assertNotNull(result.getErrorMessage());
            assertNotNull(result.getCorrectionGuidance());
        }
        
        @Test
        @DisplayName("Should reject postal codes with invalid characters")
        void shouldRejectInvalidCharacters() {
            FormatValidationResult result = validationService.validatePostalCodeFlexible("12@45");
            assertFalse(result.isValid());
            assertEquals("POSTAL_CODE_INVALID_CHARACTERS", result.getErrorCode());
            
            assertFalse(validationService.validatePostalCodeFlexible("12#45").isValid());
            assertFalse(validationService.validatePostalCodeFlexible("12$45").isValid());
            assertFalse(validationService.validatePostalCodeFlexible("12%45").isValid());
        }
    }
    
    @Nested
    @DisplayName("Postal Code Normalization")
    class PostalCodeNormalization {
        
        @Test
        @DisplayName("Should normalize Canadian postal codes")
        void shouldNormalizeCanadianPostalCodes() {
            assertEquals("K1A 0A6", validationService.normalizePostalCode("K1A0A6", "CA"));
            assertEquals("K1A 0A6", validationService.normalizePostalCode("k1a0a6", "CA"));
            assertEquals("K1A 0A6", validationService.normalizePostalCode("K1A  0A6", "CA"));
        }
        
        @Test
        @DisplayName("Should normalize UK postal codes")
        void shouldNormalizeUKPostalCodes() {
            assertEquals("SW1A 1AA", validationService.normalizePostalCode("SW1A1AA", "UK"));
            assertEquals("SW1A 1AA", validationService.normalizePostalCode("sw1a1aa", "UK"));
            assertEquals("M1 1AA", validationService.normalizePostalCode("M11AA", "UK"));
        }
        
        @Test
        @DisplayName("Should normalize Japanese postal codes")
        void shouldNormalizeJapanesePostalCodes() {
            assertEquals("100-0001", validationService.normalizePostalCode("1000001", "JP"));
            assertEquals("150-0002", validationService.normalizePostalCode("1500002", "JP"));
        }
        
        @Test
        @DisplayName("Should return original for unsupported countries")
        void shouldReturnOriginalForUnsupportedCountries() {
            String original = "12345";
            assertEquals(original, validationService.normalizePostalCode(original, "XX"));
            assertEquals(original, validationService.normalizePostalCode(original, null));
        }
    }
    
    @Nested
    @DisplayName("Country Detection")
    class CountryDetection {
        
        @Test
        @DisplayName("Should detect countries from postal code patterns")
        void shouldDetectCountriesFromPatterns() {
            assertEquals("IN", validationService.detectCountryFromPostalCode("110001"));
            assertEquals("AU", validationService.detectCountryFromPostalCode("2000"));
            assertEquals("CA", validationService.detectCountryFromPostalCode("K1A 0A6"));
            assertEquals("UK", validationService.detectCountryFromPostalCode("SW1A 1AA"));
            assertEquals("JP", validationService.detectCountryFromPostalCode("100-0001"));
        }
        
        @Test
        @DisplayName("Should return null for ambiguous patterns")
        void shouldReturnNullForAmbiguousPatterns() {
            // 5-digit codes could be US, DE, or FR
            assertNull(validationService.detectCountryFromPostalCode("12345"));
        }
        
        @Test
        @DisplayName("Should return null for invalid postal codes")
        void shouldReturnNullForInvalidPostalCodes() {
            assertNull(validationService.detectCountryFromPostalCode(null));
            assertNull(validationService.detectCountryFromPostalCode(""));
            assertNull(validationService.detectCountryFromPostalCode("INVALID"));
        }
    }
    
    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {
        
        @Test
        @DisplayName("Should return supported postal code formats")
        void shouldReturnSupportedFormats() {
            List<String> formats = validationService.getSupportedPostalCodeFormats();
            assertNotNull(formats);
            assertFalse(formats.isEmpty());
            assertEquals(8, formats.size()); // US, CA, UK, DE, FR, IN, AU, JP
            
            // Check that all expected countries are included
            String formatsString = String.join(", ", formats);
            assertTrue(formatsString.contains("United States"));
            assertTrue(formatsString.contains("Canada"));
            assertTrue(formatsString.contains("United Kingdom"));
            assertTrue(formatsString.contains("Germany"));
            assertTrue(formatsString.contains("France"));
            assertTrue(formatsString.contains("India"));
            assertTrue(formatsString.contains("Australia"));
            assertTrue(formatsString.contains("Japan"));
        }
        
        @Test
        @DisplayName("Should return postal code examples for countries")
        void shouldReturnPostalCodeExamples() {
            assertEquals("12345 or 12345-6789", validationService.getPostalCodeExample("US"));
            assertEquals("K1A 0A6 or K1A0A6", validationService.getPostalCodeExample("CA"));
            assertEquals("SW1A 1AA or M1 1AA", validationService.getPostalCodeExample("UK"));
            assertEquals("12345", validationService.getPostalCodeExample("DE"));
            assertEquals("75001", validationService.getPostalCodeExample("FR"));
            assertEquals("110001", validationService.getPostalCodeExample("IN"));
            assertEquals("2000", validationService.getPostalCodeExample("AU"));
            assertEquals("100-0001", validationService.getPostalCodeExample("JP"));
            
            assertNull(validationService.getPostalCodeExample("XX"));
            assertNull(validationService.getPostalCodeExample(null));
        }
        
        @Test
        @DisplayName("Should return all postal code examples")
        void shouldReturnAllPostalCodeExamples() {
            String allExamples = validationService.getAllPostalCodeExamples();
            assertNotNull(allExamples);
            assertFalse(allExamples.isEmpty());
            
            // Check that all countries are included
            assertTrue(allExamples.contains("United States"));
            assertTrue(allExamples.contains("Canada"));
            assertTrue(allExamples.contains("United Kingdom"));
            assertTrue(allExamples.contains("Germany"));
            assertTrue(allExamples.contains("France"));
            assertTrue(allExamples.contains("India"));
            assertTrue(allExamples.contains("Australia"));
            assertTrue(allExamples.contains("Japan"));
        }
    }
    
    @Nested
    @DisplayName("Unsupported Country Handling")
    class UnsupportedCountryHandling {
        
        @Test
        @DisplayName("Should fall back to flexible validation for unsupported countries")
        void shouldFallBackToFlexibleValidation() {
            // Unsupported country should use flexible validation
            FormatValidationResult result = validationService.validatePostalCode("12345", "XX");
            assertTrue(result.isValid()); // Should pass flexible validation
            
            // Invalid format should still fail
            FormatValidationResult invalidResult = validationService.validatePostalCode("12@45", "XX");
            assertFalse(invalidResult.isValid());
        }
        
        @Test
        @DisplayName("Should use flexible validation when country code is null")
        void shouldUseFlexibleValidationWhenCountryIsNull() {
            FormatValidationResult result = validationService.validatePostalCode("12345", null);
            assertTrue(result.isValid());
            
            FormatValidationResult invalidResult = validationService.validatePostalCode("12@45", null);
            assertFalse(invalidResult.isValid());
        }
    }
}