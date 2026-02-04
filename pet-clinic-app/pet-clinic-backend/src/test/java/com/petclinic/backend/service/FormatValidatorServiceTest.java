package com.petclinic.backend.service;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.impl.FormatValidatorServiceImpl;
import com.petclinic.backend.service.impl.InternationalValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FormatValidatorService
 * Tests format validation for mobile numbers, emails, and names
 */
@DisplayName("Format Validator Service Tests")
class FormatValidatorServiceTest {
    
    private FormatValidatorService formatValidatorService;
    
    @BeforeEach
    void setUp() {
        formatValidatorService = new FormatValidatorServiceImpl(new InternationalValidationServiceImpl());
    }
    
    @Test
    @DisplayName("Should validate valid US mobile number")
    void shouldValidateValidUSMobileNumber() {
        // Given - Using a proper US mobile number (area codes 201-999 are mobile in US)
        String validUSMobile = "+1-201-555-0123";
        
        // When
        FormatValidationResult result = formatValidatorService.validateMobileNumber(validUSMobile);
        
        // Then
        System.out.println("Result: " + result);
        assertTrue(result.isValid(), "Valid US mobile number should pass validation");
    }
    
    @Test
    @DisplayName("Should validate valid UK mobile number")
    void shouldValidateValidUKMobileNumber() {
        // Given
        String validUKMobile = "+44-7911-123456";
        
        // When
        FormatValidationResult result = formatValidatorService.validateMobileNumber(validUKMobile);
        
        // Then
        assertTrue(result.isValid(), "Valid UK mobile number should pass validation");
    }
    
    @Test
    @DisplayName("Should reject invalid mobile number format")
    void shouldRejectInvalidMobileNumberFormat() {
        // Given
        String invalidMobile = "123-456";
        
        // When
        FormatValidationResult result = formatValidatorService.validateMobileNumber(invalidMobile);
        
        // Then
        assertFalse(result.isValid(), "Invalid mobile number should fail validation");
        assertNotNull(result.getErrorMessage(), "Error message should be provided");
        assertNotNull(result.getErrorCode(), "Error code should be provided");
    }
    
    @Test
    @DisplayName("Should validate valid email address")
    void shouldValidateValidEmailAddress() {
        // Given
        String validEmail = "user@example.com";
        
        // When
        FormatValidationResult result = formatValidatorService.validateEmail(validEmail);
        
        // Then
        assertTrue(result.isValid(), "Valid email should pass validation");
    }
    
    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmailFormat() {
        // Given
        String invalidEmail = "invalid-email";
        
        // When
        FormatValidationResult result = formatValidatorService.validateEmail(invalidEmail);
        
        // Then
        assertFalse(result.isValid(), "Invalid email should fail validation");
        assertEquals("EMAIL_INVALID_FORMAT", result.getErrorCode());
    }
    
    @Test
    @DisplayName("Should validate valid name")
    void shouldValidateValidName() {
        // Given
        String validName = "John";
        
        // When
        FormatValidationResult result = formatValidatorService.validateName(validName);
        
        // Then
        assertTrue(result.isValid(), "Valid name should pass validation");
    }
    
    @Test
    @DisplayName("Should validate name with hyphen and apostrophe")
    void shouldValidateNameWithSpecialCharacters() {
        // Given
        String validName = "Mary-Jane O'Connor";
        
        // When
        FormatValidationResult result = formatValidatorService.validateName(validName);
        
        // Then
        assertTrue(result.isValid(), "Name with hyphen and apostrophe should pass validation");
    }
    
    @Test
    @DisplayName("Should reject name with invalid characters")
    void shouldRejectNameWithInvalidCharacters() {
        // Given
        String invalidName = "John123";
        
        // When
        FormatValidationResult result = formatValidatorService.validateName(invalidName);
        
        // Then
        assertFalse(result.isValid(), "Name with numbers should fail validation");
        assertEquals("NAME_INVALID_FORMAT", result.getErrorCode());
    }
}