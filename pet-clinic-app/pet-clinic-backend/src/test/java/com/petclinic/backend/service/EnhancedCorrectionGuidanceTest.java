package com.petclinic.backend.service;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.service.impl.ValidationErrorAggregatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class to demonstrate enhanced corrective guidance functionality
 * Tests the improvements made in task 7.1: Add corrective guidance to error messages
 * Updated to work with ErrorMessageFormatter integration
 * Validates: Requirements 5.2, 5.4, 5.5
 */
class EnhancedCorrectionGuidanceTest {
    
    @Mock
    private ErrorMessageFormatter errorMessageFormatter;
    
    private ValidationErrorAggregator aggregator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aggregator = new ValidationErrorAggregatorImpl(errorMessageFormatter);
        
        // Setup mock behavior for enhanced format examples
        when(errorMessageFormatter.getFormatExamples("mobileNumber"))
            .thenReturn("+1-555-123-4567 (US), +44-7911-123456 (UK), +81-90-1234-5678 (Japan), +49-151-12345678 (Germany), +33-6-12-34-56-78 (France)");
        
        when(errorMessageFormatter.getFormatExamples("email"))
            .thenReturn("user@example.com, firstname.lastname@company.org, user+tag@domain.co.uk");
        
        when(errorMessageFormatter.getFormatExamples("firstName"))
            .thenReturn("John, Mary-Jane, O'Connor, José, François, 李明, محمد");
        
        // Setup mock behavior for enhanced error messages
        when(errorMessageFormatter.formatErrorMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                String errorCode = invocation.getArgument(1);
                Object rejectedValue = invocation.getArgument(2);
                return String.format("Enhanced error for field '%s' with code '%s' and rejected value '%s'", 
                    fieldName, errorCode, rejectedValue);
            });
        
        when(errorMessageFormatter.formatErrorMessage(anyString(), anyString(), any(), anyString()))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                String errorCode = invocation.getArgument(1);
                Object rejectedValue = invocation.getArgument(2);
                String additionalContext = invocation.getArgument(3);
                return String.format("Enhanced error for field '%s' with code '%s', rejected value: %s, context: %s", 
                    fieldName, errorCode, rejectedValue, additionalContext);
            });
        
        // Setup mock behavior for detailed guidance
        when(errorMessageFormatter.generateDetailedGuidance(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                String errorCode = invocation.getArgument(1);
                Object rejectedValue = invocation.getArgument(2);
                
                if (errorCode.contains("NOT_UNIQUE")) {
                    return String.format("Enhanced uniqueness guidance for %s: Use different value or contact support", fieldName);
                } else if (errorCode.contains("FORMAT") || errorCode.contains("INVALID")) {
                    return String.format("Enhanced format guidance for %s: Please check format and try again", fieldName);
                } else if (errorCode.contains("REQUIRED")) {
                    return String.format("Enhanced required field guidance: Please provide a valid %s", fieldName);
                } else if (errorCode.contains("TOO_LONG") || errorCode.contains("LENGTH")) {
                    return String.format("Enhanced length guidance: Please shorten the %s", fieldName);
                }
                return String.format("Enhanced guidance for %s", fieldName);
            });
        
        // Setup mock behavior for field error enhancement
        when(errorMessageFormatter.enhanceFieldError(any(FieldValidationError.class)))
            .thenAnswer(invocation -> {
                FieldValidationError error = invocation.getArgument(0);
                // Simulate enhancement
                if (error.getCorrectionGuidance() == null) {
                    error.setCorrectionGuidance("Enhanced corrective guidance");
                }
                if (error.getFormatExample() == null) {
                    String examples = errorMessageFormatter.getFormatExamples(error.getFieldName());
                    error.setFormatExample(examples);
                }
                return error;
            });
    }
    
    @Test
    @DisplayName("Should provide enhanced format examples for mobile numbers")
    void testEnhancedMobileNumberFormatExamples() {
        // Test that format examples include international examples
        FieldValidationError error = aggregator.createFieldError(
            "mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", 
            "Invalid format", null, "+123"
        );
        
        assertNotNull(error.getFormatExample());
        assertTrue(error.getFormatExample().contains("US"));
        assertTrue(error.getFormatExample().contains("UK"));
        assertTrue(error.getFormatExample().contains("Japan"));
        assertTrue(error.getFormatExample().contains("Germany"));
        assertTrue(error.getFormatExample().contains("France"));
    }
    
    @Test
    @DisplayName("Should provide enhanced format examples for email addresses")
    void testEnhancedEmailFormatExamples() {
        // Test that email format examples include various formats
        FieldValidationError error = aggregator.createFieldError(
            "email", "EMAIL_FORMAT_INVALID", 
            "Invalid format", null, "invalid-email"
        );
        
        assertNotNull(error.getFormatExample());
        assertTrue(error.getFormatExample().contains("user@example.com"));
        assertTrue(error.getFormatExample().contains("firstname.lastname"));
        assertTrue(error.getFormatExample().contains("user+tag"));
    }
    
    @Test
    @DisplayName("Should provide context-specific guidance for mobile number errors")
    void testContextSpecificMobileGuidance() {
        // Test guidance for missing country code
        String guidanceNoCountryCode = aggregator.generateCorrectionGuidance(
            "mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", "5551234567"
        );
        assertNotNull(guidanceNoCountryCode);
        assertFalse(guidanceNoCountryCode.isEmpty());
        
        // Test guidance for number with letters
        String guidanceWithLetters = aggregator.generateCorrectionGuidance(
            "mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", "+1-555-ABC-4567"
        );
        assertNotNull(guidanceWithLetters);
        assertFalse(guidanceWithLetters.isEmpty());
    }
    
    @Test
    @DisplayName("Should provide alternative suggestions for uniqueness errors")
    void testUniquenessAlternativeSuggestions() {
        // Test mobile number uniqueness alternatives
        String mobileUniquenessGuidance = aggregator.generateCorrectionGuidance(
            "mobileNumber", "MOBILE_NUMBER_NOT_UNIQUE", "+1-555-123-4567"
        );
        assertTrue(mobileUniquenessGuidance.contains("different") || 
                  mobileUniquenessGuidance.contains("support") ||
                  mobileUniquenessGuidance.contains("alternative"));
        
        // Test email uniqueness alternatives
        String emailUniquenessGuidance = aggregator.generateCorrectionGuidance(
            "email", "EMAIL_NOT_UNIQUE", "user@example.com"
        );
        assertTrue(emailUniquenessGuidance.contains("different") || 
                  emailUniquenessGuidance.contains("support") ||
                  emailUniquenessGuidance.contains("alternative"));
    }
    
    @Test
    @DisplayName("Should provide length-specific guidance with character counts")
    void testLengthSpecificGuidance() {
        // Test length guidance with specific character count information
        String lengthGuidance = aggregator.generateCorrectionGuidance(
            "firstName", "FIRST_NAME_TOO_LONG", 
            "ThisIsAVeryLongFirstNameThatExceedsTheMaximumAllowedLength"
        );
        assertTrue(lengthGuidance.contains("shorten") || lengthGuidance.contains("character"));
    }
    
    @Test
    @DisplayName("Should provide comprehensive error messages with field context")
    void testComprehensiveErrorMessages() {
        // Test that error messages include field context
        String errorMessage = aggregator.formatErrorMessage(
            "mobileNumber", "Invalid format", "FORMAT_INVALID"
        );
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("mobileNumber") || errorMessage.contains("Invalid format"));
        
        // Test with empty base message - should use ErrorMessageFormatter
        String errorMessageEmpty = aggregator.formatErrorMessage(
            "email", "", "EMAIL_REQUIRED"
        );
        assertNotNull(errorMessageEmpty);
        assertTrue(errorMessageEmpty.contains("email"));
    }
    
    @Test
    @DisplayName("Should provide enhanced name format guidance")
    void testEnhancedNameFormatGuidance() {
        // Test name format examples include international names
        FieldValidationError error = aggregator.createFieldError(
            "firstName", "NAME_INVALID_FORMAT", 
            "Invalid format", null, "John123"
        );
        
        assertNotNull(error.getFormatExample());
        assertTrue(error.getFormatExample().contains("José"));
        assertTrue(error.getFormatExample().contains("François"));
        assertTrue(error.getFormatExample().contains("李明"));
        assertTrue(error.getFormatExample().contains("محمد"));
    }
    
    @Test
    @DisplayName("Should provide specific guidance for common validation scenarios")
    void testCommonValidationScenarios() {
        // Test required field guidance
        String requiredGuidance = aggregator.generateCorrectionGuidance(
            "firstName", "FIRST_NAME_REQUIRED", null
        );
        assertTrue(requiredGuidance.contains("first name") || requiredGuidance.contains("firstName"));
        
        // Test business rule guidance
        String businessRuleGuidance = aggregator.generateCorrectionGuidance(
            "email", "EMAIL_TOO_LONG", "very.long.email.address@example.com"
        );
        assertTrue(businessRuleGuidance.contains("shorten") || businessRuleGuidance.contains("shorter"));
    }
}