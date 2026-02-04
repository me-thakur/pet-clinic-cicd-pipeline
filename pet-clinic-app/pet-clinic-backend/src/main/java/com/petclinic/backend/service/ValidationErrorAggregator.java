package com.petclinic.backend.service;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.dto.UniquenessValidationResult;

import java.util.List;
import java.util.Map;

/**
 * Service interface for aggregating validation errors from multiple sources
 * Provides structured error collection and formatting capabilities
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
public interface ValidationErrorAggregator {
    
    /**
     * Aggregates multiple field validation errors into a comprehensive validation result
     * 
     * @param fieldErrors List of field validation errors to aggregate
     * @return ValidationResult containing all aggregated errors with structured formatting
     */
    ValidationResult aggregateErrors(List<FieldValidationError> fieldErrors);
    
    /**
     * Aggregates validation errors from multiple validation result sources
     * 
     * @param formatResults Map of field names to format validation results
     * @param uniquenessResults Map of field names to uniqueness validation results
     * @param businessRuleErrors List of business rule validation errors
     * @return ValidationResult containing all aggregated errors
     */
    ValidationResult aggregateValidationResults(Map<String, FormatValidationResult> formatResults,
                                               Map<String, UniquenessValidationResult> uniquenessResults,
                                               List<FieldValidationError> businessRuleErrors);
    
    /**
     * Creates a structured field error with enhanced formatting and guidance
     * 
     * @param fieldName Name of the field that failed validation
     * @param errorCode Specific error code for categorization
     * @param errorMessage Descriptive error message
     * @param correctionGuidance Specific guidance for correcting the error
     * @param rejectedValue The value that was rejected
     * @return FieldValidationError with structured formatting
     */
    FieldValidationError createFieldError(String fieldName, String errorCode, String errorMessage,
                                         String correctionGuidance, Object rejectedValue);
    
    /**
     * Creates a format validation error with examples and guidance
     * 
     * @param fieldName Name of the field that failed format validation
     * @param formatResult The format validation result containing error details
     * @return FieldValidationError with format-specific guidance and examples
     */
    FieldValidationError createFormatError(String fieldName, FormatValidationResult formatResult);
    
    /**
     * Creates a uniqueness validation error with conflict information
     * 
     * @param fieldName Name of the field that failed uniqueness validation
     * @param uniquenessResult The uniqueness validation result containing conflict details
     * @return FieldValidationError with uniqueness-specific guidance and conflict information
     */
    FieldValidationError createUniquenessError(String fieldName, UniquenessValidationResult uniquenessResult);
    
    /**
     * Formats error messages with field-specific context and guidance
     * 
     * @param fieldName Name of the field
     * @param baseMessage Base error message
     * @param errorType Type of validation error
     * @return Formatted error message with field context
     */
    String formatErrorMessage(String fieldName, String baseMessage, String errorType);
    
    /**
     * Generates corrective guidance based on error type and field context
     * 
     * @param fieldName Name of the field that failed validation
     * @param errorCode Specific error code
     * @param rejectedValue The value that was rejected (optional)
     * @return Specific corrective guidance for the error
     */
    String generateCorrectionGuidance(String fieldName, String errorCode, Object rejectedValue);
    
    /**
     * Creates an overall validation message summarizing all errors
     * 
     * @param fieldErrors List of field validation errors
     * @return Overall message describing the validation outcome
     */
    String createOverallMessage(List<FieldValidationError> fieldErrors);
    
    /**
     * Groups errors by field name for better organization
     * 
     * @param fieldErrors List of field validation errors
     * @return Map of field names to their associated errors
     */
    Map<String, List<FieldValidationError>> groupErrorsByField(List<FieldValidationError> fieldErrors);
    
    /**
     * Validates that all required error information is present
     * 
     * @param fieldError The field validation error to validate
     * @return true if the error has all required information, false otherwise
     */
    boolean isValidFieldError(FieldValidationError fieldError);
}