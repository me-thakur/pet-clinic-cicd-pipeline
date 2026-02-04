package com.petclinic.backend.validation;

import com.petclinic.backend.dto.UniquenessValidationResult;
import com.petclinic.backend.service.UniquenessValidatorService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UniqueMobileNumberValidator
 * Tests the custom validation annotation for mobile number uniqueness validation
 */
@ExtendWith(MockitoExtension.class)
class UniqueMobileNumberValidatorTest {
    
    @Mock
    private UniquenessValidatorService uniquenessValidatorService;
    
    @Mock
    private UniqueMobileNumber uniqueMobileNumber;
    
    @Mock
    private ConstraintValidatorContext context;
    
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    
    @InjectMocks
    private UniqueMobileNumberValidator validator;
    
    @BeforeEach
    void setUp() {
        when(uniqueMobileNumber.allowNull()).thenReturn(true);
        when(uniqueMobileNumber.ownerIdField()).thenReturn("");
        validator.initialize(uniqueMobileNumber);
    }
    
    @Test
    void testUniqueMobileNumber() {
        // Given
        String uniqueMobileNumber = "+1-555-123-4567";
        UniquenessValidationResult uniqueResult = UniquenessValidationResult.unique();
        when(uniquenessValidatorService.validateMobileNumberUniqueness(uniqueMobileNumber)).thenReturn(uniqueResult);
        
        // When
        boolean result = validator.isValid(uniqueMobileNumber, context);
        
        // Then
        assertTrue(result);
        verify(uniquenessValidatorService).validateMobileNumberUniqueness(uniqueMobileNumber);
    }
    
    @Test
    void testDuplicateMobileNumber() {
        // Given
        String duplicateMobileNumber = "+1-555-123-4567";
        UniquenessValidationResult duplicateResult = UniquenessValidationResult.notUnique("Mobile number already exists", duplicateMobileNumber, "UNIQUENESS_VIOLATION");
        
        when(uniquenessValidatorService.validateMobileNumberUniqueness(duplicateMobileNumber)).thenReturn(duplicateResult);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
        
        // When
        boolean result = validator.isValid(duplicateMobileNumber, context);
        
        // Then
        assertFalse(result);
        verify(uniquenessValidatorService).validateMobileNumberUniqueness(duplicateMobileNumber);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Mobile number +1-555-123-4567 already exists in the system");
    }
    
    @Test
    void testNullMobileNumberWithAllowNull() {
        // Given
        when(uniqueMobileNumber.allowNull()).thenReturn(true);
        validator.initialize(uniqueMobileNumber);
        
        // When
        boolean result = validator.isValid(null, context);
        
        // Then
        assertTrue(result);
        verifyNoInteractions(uniquenessValidatorService);
    }
    
    @Test
    void testNullMobileNumberWithoutAllowNull() {
        // Given
        when(uniqueMobileNumber.allowNull()).thenReturn(false);
        validator.initialize(uniqueMobileNumber);
        
        // When
        boolean result = validator.isValid(null, context);
        
        // Then
        assertFalse(result);
        verifyNoInteractions(uniquenessValidatorService);
    }
    
    @Test
    void testEmptyMobileNumberWithAllowNull() {
        // Given
        when(uniqueMobileNumber.allowNull()).thenReturn(true);
        validator.initialize(uniqueMobileNumber);
        
        // When
        boolean result = validator.isValid("", context);
        
        // Then
        assertTrue(result);
        verifyNoInteractions(uniquenessValidatorService);
    }
    
    @Test
    void testEmptyMobileNumberWithoutAllowNull() {
        // Given
        when(uniqueMobileNumber.allowNull()).thenReturn(false);
        validator.initialize(uniqueMobileNumber);
        
        // When
        boolean result = validator.isValid("", context);
        
        // Then
        assertFalse(result);
        verifyNoInteractions(uniquenessValidatorService);
    }
    
    @Test
    void testWhitespaceMobileNumber() {
        // Given
        when(uniqueMobileNumber.allowNull()).thenReturn(true);
        validator.initialize(uniqueMobileNumber);
        
        // When
        boolean result = validator.isValid("   ", context);
        
        // Then
        assertTrue(result);
        verifyNoInteractions(uniquenessValidatorService);
    }
    
    @Test
    void testMobileNumberValidationWithExcludeOwner() {
        // Given - This test demonstrates the intended behavior, though the current
        // implementation doesn't fully support owner ID extraction from context
        String mobileNumber = "+1-555-123-4567";
        UniquenessValidationResult uniqueResult = UniquenessValidationResult.unique();
        when(uniquenessValidatorService.validateMobileNumberUniqueness(mobileNumber)).thenReturn(uniqueResult);
        
        // When
        boolean result = validator.isValid(mobileNumber, context);
        
        // Then
        assertTrue(result);
        verify(uniquenessValidatorService).validateMobileNumberUniqueness(mobileNumber);
    }
}