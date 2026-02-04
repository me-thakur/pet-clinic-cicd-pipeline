package com.petclinic.backend.service;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.dto.UniquenessValidationResult;
import com.petclinic.backend.service.impl.ValidationErrorAggregatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidationErrorAggregator
 * Tests error collection, formatting, and structured response generation
 * Updated to work with ErrorMessageFormatter integration
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
class ValidationErrorAggregatorTest {
    
    @Mock
    private ErrorMessageFormatter errorMessageFormatter;
    
    private ValidationErrorAggregator aggregator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aggregator = new ValidationErrorAggregatorImpl(errorMessageFormatter);
        
        // Setup default mock behavior for ErrorMessageFormatter
        when(errorMessageFormatter.enhanceFieldError(any(FieldValidationError.class)))
            .thenAnswer(invocation -> {
                FieldValidationError error = invocation.getArgument(0);
                // Simulate enhancement by ensuring all required fields are present
                if (error.getCorrectionGuidance() == null) {
                    error.setCorrectionGuidance("Please correct the field and try again");
                }
                if (error.getFormatExample() == null && "mobileNumber".equals(error.getFieldName())) {
                    error.setFormatExample("+1-555-123-4567 (US), +44-7911-123456 (UK)");
                }
                return error;
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
        
        when(errorMessageFormatter.formatErrorMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                String errorCode = invocation.getArgument(1);
                Object rejectedValue = invocation.getArgument(2);
                return String.format("Enhanced error for field '%s' with code '%s', rejected value: %s", 
                    fieldName, errorCode, rejectedValue);
            });
        
        when(errorMessageFormatter.generateDetailedGuidance(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                return String.format("Please correct the %s and try again", fieldName);
            });
        
        when(errorMessageFormatter.getFormatExamples(anyString()))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                if ("mobileNumber".equals(fieldName)) {
                    return "+1-555-123-4567 (US), +44-7911-123456 (UK)";
                } else if ("email".equals(fieldName)) {
                    return "user@example.com, firstname.lastname@company.org";
                }
                return null;
            });
        
        when(errorMessageFormatter.formatUniquenessError(anyString(), any(), any(Long.class)))
            .thenAnswer(invocation -> {
                String fieldName = invocation.getArgument(0);
                Object rejectedValue = invocation.getArgument(1);
                Long entityId = invocation.getArgument(2);
                return String.format("Field '%s' value '%s' already exists (Entity ID: %s)", 
                    fieldName, rejectedValue, entityId);
            });
    }
    
    @Test
    @DisplayName("Should return valid result when no errors provided")
    void testAggregateErrors_NoErrors() {
        // Given
        List<FieldValidationError> errors = new ArrayList<>();
        
        // When
        ValidationResult result = aggregator.aggregateErrors(errors);
        
        // Then
        assertTrue(result.isValid());
        assertEquals("All validation checks passed", result.getOverallMessage());
        assertTrue(result.getFieldErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should return valid result when null errors provided")
    void testAggregateErrors_NullErrors() {
        // When
        ValidationResult result = aggregator.aggregateErrors(null);
        
        // Then
        assertTrue(result.isValid());
        assertEquals("All validation checks passed", result.getOverallMessage());
    }
    
    @Test
    @DisplayName("Should aggregate single field error correctly")
    void testAggregateErrors_SingleError() {
        // Given
        FieldValidationError error = new FieldValidationError(
            "mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", 
            "Invalid mobile number format", "Please use international format", "+123456"
        );
        List<FieldValidationError> errors = Arrays.asList(error);
        
        // When
        ValidationResult result = aggregator.aggregateErrors(errors);
        
        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getFieldErrors().size());
        assertEquals("Validation failed for mobileNumber", result.getOverallMessage());
        assertEquals(1, result.getMetadata().get("errorCount"));
    }
    
    @Test
    @DisplayName("Should aggregate multiple field errors correctly")
    void testAggregateErrors_MultipleErrors() {
        // Given
        FieldValidationError error1 = new FieldValidationError(
            "mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", 
            "Invalid mobile number format", "Please use international format", "+123456"
        );
        FieldValidationError error2 = new FieldValidationError(
            "email", "EMAIL_FORMAT_INVALID", 
            "Invalid email format", "Please use valid email format", "invalid-email"
        );
        List<FieldValidationError> errors = Arrays.asList(error1, error2);
        
        // When
        ValidationResult result = aggregator.aggregateErrors(errors);
        
        // Then
        assertFalse(result.isValid());
        assertEquals(2, result.getFieldErrors().size());
        assertEquals("Validation failed for 2 fields with 2 total errors", result.getOverallMessage());
        assertEquals(2, result.getMetadata().get("errorCount"));
    }
    
    @Test
    @DisplayName("Should create format error with enhanced details")
    void testCreateFormatError() {
        // Given
        FormatValidationResult formatResult = FormatValidationResult.invalid(
            "Mobile number format is invalid", "Please use international format",
            "+123456", "+1-555-123-4567", "MOBILE_NUMBER_FORMAT_INVALID"
        );
        
        // When
        FieldValidationError error = aggregator.createFormatError("mobileNumber", formatResult);
        
        // Then
        assertEquals("mobileNumber", error.getFieldName());
        assertEquals("MOBILE_NUMBER_FORMAT_INVALID", error.getErrorCode());
        assertEquals("Mobile number format is invalid", error.getErrorMessage());
        assertEquals("Please use international format", error.getCorrectionGuidance());
        assertEquals("+123456", error.getRejectedValue());
        assertEquals("+1-555-123-4567", error.getFormatExample());
    }
    
    @Test
    @DisplayName("Should create uniqueness error with conflict information")
    void testCreateUniquenessError() {
        // Given
        UniquenessValidationResult uniquenessResult = UniquenessValidationResult.notUnique(
            "Mobile number already exists", "Please use different number",
            "+1-555-123-4567", "MOBILE_NUMBER_NOT_UNIQUE", 123L
        );
        
        // When
        FieldValidationError error = aggregator.createUniquenessError("mobileNumber", uniquenessResult);
        
        // Then
        assertEquals("mobileNumber", error.getFieldName());
        assertEquals("MOBILE_NUMBER_NOT_UNIQUE", error.getErrorCode());
        assertEquals("Mobile number already exists", error.getErrorMessage());
        assertEquals("Please use different number", error.getCorrectionGuidance());
        assertEquals("+1-555-123-4567", error.getRejectedValue());
        assertEquals(123L, error.getConflictingEntityId());
    }
    
    @Test
    @DisplayName("Should aggregate validation results from multiple sources")
    void testAggregateValidationResults() {
        // Given
        Map<String, FormatValidationResult> formatResults = new HashMap<>();
        formatResults.put("mobileNumber", FormatValidationResult.invalid(
            "Invalid format", "Use international format", "+123", "+1-555-123-4567", "FORMAT_INVALID"
        ));
        
        Map<String, UniquenessValidationResult> uniquenessResults = new HashMap<>();
        uniquenessResults.put("email", UniquenessValidationResult.notUnique(
            "Email exists", "Use different email", "test@example.com", "EMAIL_NOT_UNIQUE", 456L
        ));
        
        List<FieldValidationError> businessRuleErrors = Arrays.asList(
            new FieldValidationError("firstName", "REQUIRED", "First name required", "Enter first name", null)
        );
        
        // When
        ValidationResult result = aggregator.aggregateValidationResults(
            formatResults, uniquenessResults, businessRuleErrors
        );
        
        // Then
        assertFalse(result.isValid());
        assertEquals(3, result.getFieldErrors().size());
        
        // Verify each error type is present
        Map<String, List<FieldValidationError>> errorsByField = aggregator.groupErrorsByField(result.getFieldErrors());
        assertTrue(errorsByField.containsKey("mobileNumber"));
        assertTrue(errorsByField.containsKey("email"));
        assertTrue(errorsByField.containsKey("firstName"));
    }
    
    @Test
    @DisplayName("Should format error message with field context")
    void testFormatErrorMessage() {
        // Test with message that doesn't include field name
        String formatted1 = aggregator.formatErrorMessage("mobileNumber", "Invalid format", "FORMAT_INVALID");
        assertNotNull(formatted1);
        assertTrue(formatted1.contains("mobileNumber"));
        
        // Test with message that already includes field name
        String formatted2 = aggregator.formatErrorMessage("email", "Email format is invalid", "FORMAT_INVALID");
        assertEquals("Email format is invalid", formatted2);
        
        // Test with empty message - should use ErrorMessageFormatter
        String formatted3 = aggregator.formatErrorMessage("firstName", "", "REQUIRED");
        assertNotNull(formatted3);
        assertTrue(formatted3.contains("firstName"));
    }
    
    @Test
    @DisplayName("Should generate appropriate correction guidance")
    void testGenerateCorrectionGuidance() {
        // All guidance generation is now handled by ErrorMessageFormatter
        String mobileGuidance = aggregator.generateCorrectionGuidance("mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", "+123");
        assertNotNull(mobileGuidance);
        assertTrue(mobileGuidance.contains("mobileNumber"));
        
        String emailGuidance = aggregator.generateCorrectionGuidance("email", "EMAIL_FORMAT_INVALID", "invalid-email");
        assertNotNull(emailGuidance);
        assertTrue(emailGuidance.contains("email"));
        
        String requiredGuidance = aggregator.generateCorrectionGuidance("firstName", "FIRST_NAME_REQUIRED", null);
        assertNotNull(requiredGuidance);
        assertTrue(requiredGuidance.contains("firstName"));
        
        String uniquenessGuidance = aggregator.generateCorrectionGuidance("mobileNumber", "MOBILE_NUMBER_NOT_UNIQUE", "+1-555-123-4567");
        assertNotNull(uniquenessGuidance);
        assertTrue(uniquenessGuidance.contains("mobileNumber"));
    }
    
    @Test
    @DisplayName("Should create appropriate overall messages")
    void testCreateOverallMessage() {
        // Test single error
        List<FieldValidationError> singleError = Arrays.asList(
            new FieldValidationError("mobileNumber", "FORMAT_INVALID", "Invalid format", "Fix format", "+123")
        );
        String message1 = aggregator.createOverallMessage(singleError);
        assertEquals("Validation failed for mobileNumber", message1);
        
        // Test multiple errors for same field
        List<FieldValidationError> multipleErrorsSameField = Arrays.asList(
            new FieldValidationError("mobileNumber", "FORMAT_INVALID", "Invalid format", "Fix format", "+123"),
            new FieldValidationError("mobileNumber", "TOO_LONG", "Too long", "Shorten", "+123456789012345")
        );
        String message2 = aggregator.createOverallMessage(multipleErrorsSameField);
        assertTrue(message2.contains("mobileNumber") && message2.contains("2 errors"));
        
        // Test multiple errors for different fields
        List<FieldValidationError> multipleErrorsDifferentFields = Arrays.asList(
            new FieldValidationError("mobileNumber", "FORMAT_INVALID", "Invalid format", "Fix format", "+123"),
            new FieldValidationError("email", "FORMAT_INVALID", "Invalid email", "Fix email", "invalid")
        );
        String message3 = aggregator.createOverallMessage(multipleErrorsDifferentFields);
        assertTrue(message3.contains("2 fields") && message3.contains("2 total errors"));
    }
    
    @Test
    @DisplayName("Should group errors by field correctly")
    void testGroupErrorsByField() {
        // Given
        List<FieldValidationError> errors = Arrays.asList(
            new FieldValidationError("mobileNumber", "FORMAT_INVALID", "Invalid format", "Fix format", "+123"),
            new FieldValidationError("mobileNumber", "TOO_LONG", "Too long", "Shorten", "+123456789012345"),
            new FieldValidationError("email", "FORMAT_INVALID", "Invalid email", "Fix email", "invalid")
        );
        
        // When
        Map<String, List<FieldValidationError>> grouped = aggregator.groupErrorsByField(errors);
        
        // Then
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("mobileNumber").size());
        assertEquals(1, grouped.get("email").size());
    }
    
    @Test
    @DisplayName("Should validate field error completeness")
    void testIsValidFieldError() {
        // Valid error
        FieldValidationError validError = new FieldValidationError(
            "mobileNumber", "FORMAT_INVALID", "Invalid format", "Fix format", "+123"
        );
        assertTrue(aggregator.isValidFieldError(validError));
        
        // Invalid errors
        assertFalse(aggregator.isValidFieldError(null));
        assertFalse(aggregator.isValidFieldError(new FieldValidationError(null, "CODE", "Message", "Guidance", "Value")));
        assertFalse(aggregator.isValidFieldError(new FieldValidationError("field", null, "Message", "Guidance", "Value")));
        assertFalse(aggregator.isValidFieldError(new FieldValidationError("field", "CODE", null, "Guidance", "Value")));
        assertFalse(aggregator.isValidFieldError(new FieldValidationError("", "CODE", "Message", "Guidance", "Value")));
        assertFalse(aggregator.isValidFieldError(new FieldValidationError("field", "", "Message", "Guidance", "Value")));
        assertFalse(aggregator.isValidFieldError(new FieldValidationError("field", "CODE", "", "Guidance", "Value")));
    }
    
    @Test
    @DisplayName("Should create field error with enhanced formatting")
    void testCreateFieldError() {
        // When
        FieldValidationError error = aggregator.createFieldError(
            "mobileNumber", "FORMAT_INVALID", "Invalid format", null, "+123"
        );
        
        // Then
        assertEquals("mobileNumber", error.getFieldName());
        assertEquals("FORMAT_INVALID", error.getErrorCode());
        assertNotNull(error.getErrorMessage());
        assertNotNull(error.getCorrectionGuidance());
        assertEquals("+123", error.getRejectedValue());
        assertNotNull(error.getFormatExample()); // Should be added for mobileNumber by mock
    }
    
    @Test
    @DisplayName("Should handle empty validation results gracefully")
    void testAggregateValidationResults_EmptyResults() {
        // Given
        Map<String, FormatValidationResult> formatResults = new HashMap<>();
        Map<String, UniquenessValidationResult> uniquenessResults = new HashMap<>();
        List<FieldValidationError> businessRuleErrors = new ArrayList<>();
        
        // When
        ValidationResult result = aggregator.aggregateValidationResults(
            formatResults, uniquenessResults, businessRuleErrors
        );
        
        // Then
        assertTrue(result.isValid());
        assertEquals("All validation checks passed", result.getOverallMessage());
    }
    
    @Test
    @DisplayName("Should handle null validation results gracefully")
    void testAggregateValidationResults_NullResults() {
        // When
        ValidationResult result = aggregator.aggregateValidationResults(null, null, null);
        
        // Then
        assertTrue(result.isValid());
        assertEquals("All validation checks passed", result.getOverallMessage());
    }
}