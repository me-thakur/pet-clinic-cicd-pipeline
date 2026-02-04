package com.petclinic.backend.service;

import com.petclinic.backend.dto.FieldValidationError;

/**
 * Service interface for formatting validation error messages with enhanced templates
 * Provides detailed error message formatting with categorization and context
 * Validates: Requirements 3.4, 3.5
 */
public interface ErrorMessageFormatter {
    
    /**
     * Formats an error message using enhanced templates with field context
     * 
     * @param fieldName the name of the field that failed validation
     * @param errorCode the specific error code for categorization
     * @param rejectedValue the value that was rejected during validation
     * @param additionalContext optional additional context for the error
     * @return formatted error message with field name, error type, and rejected value
     */
    String formatErrorMessage(String fieldName, String errorCode, Object rejectedValue, String additionalContext);
    
    /**
     * Formats an error message using enhanced templates
     * 
     * @param fieldName the name of the field that failed validation
     * @param errorCode the specific error code for categorization
     * @param rejectedValue the value that was rejected during validation
     * @return formatted error message with field name, error type, and rejected value
     */
    String formatErrorMessage(String fieldName, String errorCode, Object rejectedValue);
    
    /**
     * Generates detailed corrective guidance based on error type and context
     * 
     * @param fieldName the name of the field that failed validation
     * @param errorCode the specific error code
     * @param rejectedValue the value that was rejected
     * @return detailed corrective guidance with specific steps
     */
    String generateDetailedGuidance(String fieldName, String errorCode, Object rejectedValue);
    
    /**
     * Gets format examples for a specific field type
     * 
     * @param fieldName the name of the field
     * @return format examples appropriate for the field type
     */
    String getFormatExamples(String fieldName);
    
    /**
     * Categorizes an error code into a hierarchical category
     * 
     * @param errorCode the error code to categorize
     * @return the error category (FORMAT, UNIQUENESS, REQUIRED, LENGTH, BUSINESS_RULE)
     */
    String categorizeErrorCode(String errorCode);
    
    /**
     * Enhances a field validation error with detailed formatting and context
     * 
     * @param error the field validation error to enhance
     * @return enhanced field validation error with detailed messages and guidance
     */
    FieldValidationError enhanceFieldError(FieldValidationError error);
    
    /**
     * Creates a formatted uniqueness error with specific entity information
     * 
     * @param fieldName the name of the field
     * @param rejectedValue the value that already exists
     * @param conflictingEntityId the ID of the entity that has the conflicting value
     * @return formatted uniqueness error message
     */
    String formatUniquenessError(String fieldName, Object rejectedValue, Long conflictingEntityId);
    
    /**
     * Analyzes the rejected value to provide context-specific guidance
     * 
     * @param fieldName the name of the field
     * @param rejectedValue the rejected value to analyze
     * @return context-specific guidance based on the rejected value
     */
    String analyzeRejectedValueContext(String fieldName, Object rejectedValue);
}