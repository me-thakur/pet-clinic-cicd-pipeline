package com.petclinic.backend.validation;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.FormatValidatorService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidMobileNumberValidator
 * Tests the custom validation annotation for mobile number format validation
 */
@ExtendWith(MockitoExtension.class)
class ValidMobileNumberValidatorTest {
    
    @Mock
    private FormatValidatorService formatValidatorService;
    
    @Mock
    private ValidMobileNumber validMobileNumber;
    
    @Mock
    private ConstraintValidatorContext context;
    
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    
    @InjectMocks
    private ValidMobileNumberValidator validator;
    
    @BeforeEach
    void setUp() {
        when(validMobileNumber.allowNull()).thenReturn(true);
        when(validMobileNumber.countryCode()).thenReturn("");
        validator.initialize(validMobileNumber);
    }
    
    @Test
    void testValidMobileNumber() {
        // Given
        String validMobileNumber = "+1-555-123-4567";
        FormatValidationResult validResult = FormatValidationResult.valid();
        when(formatValidatorService.validateMobileNumber(validMobileNumber)).thenReturn(validResult);
        
        // When
        boolean result = validator.isValid(validMobileNumber, context);
        
        // Then
        assertTrue(result);
        verify(formatValidatorService).validateMobileNumber(validMobileNumber);
    }
    
    @Test
    void testInvalidMobileNumber() {
        // Given
        String invalidMobileNumber = "invalid-number";
        String errorMessage = "Invalid mobile number format";
        FormatValidationResult invalidResult = FormatValidationResult.invalid(errorMessage, "FORMAT_ERROR");
        
        when(formatValidatorService.validateMobileNumber(invalidMobileNumber)).thenReturn(invalidResult);
        when(context.buildConstraintViolationWithTemplate(errorMessage)).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
        
        // When
        boolean result = validator.isValid(invalidMobileNumber, context);
        
        // Then
        assertFalse(result);
        verify(formatValidatorService).validateMobileNumber(invalidMobileNumber);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(errorMessage);
    }
    
    @Test
    void testNullMobileNumberWithAllowNull() {
        // Given
        when(validMobileNumber.allowNull()).thenReturn(true);
        validator.initialize(validMobileNumber);
        
        // When
        boolean result = validator.isValid(null, context);
        
        // Then
        assertTrue(result);
        verifyNoInteractions(formatValidatorService);
    }
    
    @Test
    void testNullMobileNumberWithoutAllowNull() {
        // Given
        when(validMobileNumber.allowNull()).thenReturn(false);
        validator.initialize(validMobileNumber);
        
        // When
        boolean result = validator.isValid(null, context);
        
        // Then
        assertFalse(result);
        verifyNoInteractions(formatValidatorService);
    }
    
    @Test
    void testEmptyMobileNumberWithAllowNull() {
        // Given
        when(validMobileNumber.allowNull()).thenReturn(true);
        validator.initialize(validMobileNumber);
        
        // When
        boolean result = validator.isValid("", context);
        
        // Then
        assertTrue(result);
        verifyNoInteractions(formatValidatorService);
    }
    
    @Test
    void testEmptyMobileNumberWithoutAllowNull() {
        // Given
        when(validMobileNumber.allowNull()).thenReturn(false);
        validator.initialize(validMobileNumber);
        
        // When
        boolean result = validator.isValid("", context);
        
        // Then
        assertFalse(result);
        verifyNoInteractions(formatValidatorService);
    }
    
    @Test
    void testMobileNumberWithCountryCode() {
        // Given
        String mobileNumber = "555-123-4567";
        String countryCode = "US";
        FormatValidationResult validResult = FormatValidationResult.valid();
        
        when(validMobileNumber.countryCode()).thenReturn(countryCode);
        when(formatValidatorService.validateMobileNumber(mobileNumber, countryCode)).thenReturn(validResult);
        validator.initialize(validMobileNumber);
        
        // When
        boolean result = validator.isValid(mobileNumber, context);
        
        // Then
        assertTrue(result);
        verify(formatValidatorService).validateMobileNumber(mobileNumber, countryCode);
    }
    
    @Test
    void testWhitespaceMobileNumber() {
        // Given
        when(validMobileNumber.allowNull()).thenReturn(true);
        validator.initialize(validMobileNumber);
        
        // When
        boolean result = validator.isValid("   ", context);
        
        // Then
        assertTrue(result);
        verifyNoInteractions(formatValidatorService);
    }
}