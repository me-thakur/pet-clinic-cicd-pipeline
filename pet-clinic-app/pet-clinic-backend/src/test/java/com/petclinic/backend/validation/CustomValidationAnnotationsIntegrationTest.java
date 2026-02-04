package com.petclinic.backend.validation;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.dto.UniquenessValidationResult;
import com.petclinic.backend.service.FormatValidatorService;
import com.petclinic.backend.service.UniquenessValidatorService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test for custom validation annotations
 * Tests the annotations when applied to actual model classes
 */
@SpringBootTest
@ActiveProfiles("test")
class CustomValidationAnnotationsIntegrationTest {
    
    @Autowired
    private Validator validator;
    
    @MockBean
    private FormatValidatorService formatValidatorService;
    
    @MockBean
    private UniquenessValidatorService uniquenessValidatorService;
    
    /**
     * Test model class with custom validation annotations
     */
    static class TestOwnerModel {
        @ValidMobileNumber
        @UniqueMobileNumber
        private String mobileNumber;
        
        public TestOwnerModel(String mobileNumber) {
            this.mobileNumber = mobileNumber;
        }
        
        public String getMobileNumber() {
            return mobileNumber;
        }
        
        public void setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
        }
    }
    
    @BeforeEach
    void setUp() {
        // Default mock behavior - valid format and unique
        when(formatValidatorService.validateMobileNumber(anyString()))
            .thenReturn(FormatValidationResult.valid());
        when(uniquenessValidatorService.validateMobileNumberUniqueness(anyString()))
            .thenReturn(UniquenessValidationResult.unique());
    }
    
    @Test
    void testValidMobileNumberPassesValidation() {
        // Given
        String validMobileNumber = "+1-555-123-4567";
        TestOwnerModel owner = new TestOwnerModel(validMobileNumber);
        
        // When
        Set<ConstraintViolation<TestOwnerModel>> violations = validator.validate(owner);
        
        // Then
        assertTrue(violations.isEmpty(), "Valid mobile number should pass validation");
    }
    
    @Test
    void testInvalidFormatMobileNumberFailsValidation() {
        // Given
        String invalidMobileNumber = "invalid-number";
        String errorMessage = "Invalid mobile number format";
        TestOwnerModel owner = new TestOwnerModel(invalidMobileNumber);
        
        when(formatValidatorService.validateMobileNumber(invalidMobileNumber))
            .thenReturn(FormatValidationResult.invalid(errorMessage, "FORMAT_ERROR"));
        
        // When
        Set<ConstraintViolation<TestOwnerModel>> violations = validator.validate(owner);
        
        // Then
        assertFalse(violations.isEmpty(), "Invalid format should fail validation");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains(errorMessage)),
            "Should contain format error message");
    }
    
    @Test
    void testDuplicateMobileNumberFailsValidation() {
        // Given
        String duplicateMobileNumber = "+1-555-123-4567";
        TestOwnerModel owner = new TestOwnerModel(duplicateMobileNumber);
        
        when(uniquenessValidatorService.validateMobileNumberUniqueness(duplicateMobileNumber))
            .thenReturn(UniquenessValidationResult.notUnique("Mobile number already exists", 
                duplicateMobileNumber, "UNIQUENESS_VIOLATION"));
        
        // When
        Set<ConstraintViolation<TestOwnerModel>> violations = validator.validate(owner);
        
        // Then
        assertFalse(violations.isEmpty(), "Duplicate mobile number should fail validation");
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("already exists in the system")),
            "Should contain uniqueness error message");
    }
    
    @Test
    void testNullMobileNumberPassesValidation() {
        // Given
        TestOwnerModel owner = new TestOwnerModel(null);
        
        // When
        Set<ConstraintViolation<TestOwnerModel>> violations = validator.validate(owner);
        
        // Then
        assertTrue(violations.isEmpty(), "Null mobile number should pass validation (allowNull=true)");
    }
    
    @Test
    void testEmptyMobileNumberPassesValidation() {
        // Given
        TestOwnerModel owner = new TestOwnerModel("");
        
        // When
        Set<ConstraintViolation<TestOwnerModel>> violations = validator.validate(owner);
        
        // Then
        assertTrue(violations.isEmpty(), "Empty mobile number should pass validation (allowNull=true)");
    }
    
    @Test
    void testBothValidationsFailSimultaneously() {
        // Given
        String invalidMobileNumber = "invalid-duplicate";
        TestOwnerModel owner = new TestOwnerModel(invalidMobileNumber);
        
        when(formatValidatorService.validateMobileNumber(invalidMobileNumber))
            .thenReturn(FormatValidationResult.invalid("Invalid format", "FORMAT_ERROR"));
        when(uniquenessValidatorService.validateMobileNumberUniqueness(invalidMobileNumber))
            .thenReturn(UniquenessValidationResult.notUnique("Already exists", 
                invalidMobileNumber, "UNIQUENESS_VIOLATION"));
        
        // When
        Set<ConstraintViolation<TestOwnerModel>> violations = validator.validate(owner);
        
        // Then
        assertEquals(2, violations.size(), "Both format and uniqueness validations should fail");
        
        boolean hasFormatError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Invalid format"));
        boolean hasUniquenessError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("already exists in the system"));
        
        assertTrue(hasFormatError, "Should have format validation error");
        assertTrue(hasUniquenessError, "Should have uniqueness validation error");
    }
}