package com.petclinic.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OwnerValidationRequest DTO Bean Validation
 * Tests that the DTO now supports international postal code formats
 * Validates: Requirements 3.5
 */
@DisplayName("OwnerValidationRequest DTO Tests")
class OwnerValidationRequestTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Nested
    @DisplayName("International Postal Code Validation")
    class InternationalPostalCodeValidation {
        
        @Test
        @DisplayName("Should accept US postal codes")
        void shouldAcceptUSPostalCodes() {
            OwnerValidationRequest request = createValidRequest();
            
            // Test 5-digit ZIP code
            request.setZipCode("12345");
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "5-digit US ZIP code should be valid");
            
            // Test ZIP+4 format
            request.setZipCode("12345-6789");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "ZIP+4 format should be valid");
        }
        
        @Test
        @DisplayName("Should accept Canadian postal codes")
        void shouldAcceptCanadianPostalCodes() {
            OwnerValidationRequest request = createValidRequest();
            
            // Test Canadian postal code with space
            request.setZipCode("K1A 0A6");
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Canadian postal code with space should be valid");
            
            // Test Canadian postal code without space
            request.setZipCode("K1A0A6");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Canadian postal code without space should be valid");
        }
        
        @Test
        @DisplayName("Should accept UK postal codes")
        void shouldAcceptUKPostalCodes() {
            OwnerValidationRequest request = createValidRequest();
            
            // Test UK postal code
            request.setZipCode("SW1A 1AA");
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "UK postal code should be valid");
            
            // Test shorter UK postal code
            request.setZipCode("M1 1AA");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Shorter UK postal code should be valid");
        }
        
        @Test
        @DisplayName("Should accept other international postal codes")
        void shouldAcceptOtherInternationalPostalCodes() {
            OwnerValidationRequest request = createValidRequest();
            
            // Test German postal code
            request.setZipCode("12345");
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "German postal code should be valid");
            
            // Test Indian postal code
            request.setZipCode("110001");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Indian postal code should be valid");
            
            // Test Australian postal code
            request.setZipCode("2000");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Australian postal code should be valid");
            
            // Test Japanese postal code
            request.setZipCode("100-0001");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Japanese postal code should be valid");
        }
        
        @Test
        @DisplayName("Should accept empty postal code")
        void shouldAcceptEmptyPostalCode() {
            OwnerValidationRequest request = createValidRequest();
            
            // Test null postal code
            request.setZipCode(null);
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Null postal code should be valid (optional field)");
            
            // Test empty postal code
            request.setZipCode("");
            violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Empty postal code should be valid (optional field)");
        }
        
        @Test
        @DisplayName("Should reject invalid postal code formats")
        void shouldRejectInvalidPostalCodeFormats() {
            OwnerValidationRequest request = createValidRequest();
            
            // Test postal code with special characters
            request.setZipCode("12@45");
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Postal code with special characters should be invalid");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("zipCode")));
            
            // Test postal code that's too short
            request.setZipCode("12");
            violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Postal code that's too short should be invalid");
            
            // Test postal code that's too long
            request.setZipCode("12345678901");
            violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Postal code that's too long should be invalid");
        }
        
        @Test
        @DisplayName("Should provide helpful error message for invalid postal codes")
        void shouldProvideHelpfulErrorMessageForInvalidPostalCodes() {
            OwnerValidationRequest request = createValidRequest();
            request.setZipCode("12@45");
            
            Set<ConstraintViolation<OwnerValidationRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            
            ConstraintViolation<OwnerValidationRequest> violation = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("zipCode"))
                .findFirst()
                .orElse(null);
            
            assertNotNull(violation);
            String message = violation.getMessage();
            assertTrue(message.contains("Invalid postal code format"), "Error message should mention invalid format");
            assertTrue(message.contains("Examples:"), "Error message should contain examples");
            assertTrue(message.contains("12345 (US)"), "Error message should contain US example");
            assertTrue(message.contains("K1A 0A6 (Canada)"), "Error message should contain Canadian example");
            assertTrue(message.contains("SW1A 1AA (UK)"), "Error message should contain UK example");
        }
    }
    
    private OwnerValidationRequest createValidRequest() {
        OwnerValidationRequest request = new OwnerValidationRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setMobileNumber("+1-555-123-4567");
        request.setAddress("123 Main St");
        request.setCity("Anytown");
        request.setState("CA");
        request.setTelephone("555-123-4567");
        return request;
    }
}