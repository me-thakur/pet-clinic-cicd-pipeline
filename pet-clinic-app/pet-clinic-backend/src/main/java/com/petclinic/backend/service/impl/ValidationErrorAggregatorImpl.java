package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.dto.UniquenessValidationResult;
import com.petclinic.backend.service.ValidationErrorAggregator;
import com.petclinic.backend.service.ErrorMessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ValidationErrorAggregator for collecting and formatting validation errors
 * Provides comprehensive error aggregation with field-specific identification and guidance
 * Enhanced with detailed error message templates and categorization
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Service
public class ValidationErrorAggregatorImpl implements ValidationErrorAggregator {
    
    private final ErrorMessageFormatter errorMessageFormatter;
    
    @Autowired
    public ValidationErrorAggregatorImpl(ErrorMessageFormatter errorMessageFormatter) {
        this.errorMessageFormatter = errorMessageFormatter;
    }
    
    // Error message templates for consistent formatting
    private static final Map<String, String> ERROR_MESSAGE_TEMPLATES = Map.of(
        "MOBILE_NUMBER_FORMAT_INVALID", "Mobile number '%s' is not in valid international format",
        "EMAIL_FORMAT_INVALID", "Email address '%s' is not in valid format",
        "MOBILE_NUMBER_NOT_UNIQUE", "Mobile number '%s' already exists in the system",
        "EMAIL_NOT_UNIQUE", "Email address '%s' already exists in the system",
        "FIRST_NAME_REQUIRED", "First name is required and cannot be empty",
        "LAST_NAME_REQUIRED", "Last name is required and cannot be empty",
        "FIELD_TOO_LONG", "%s exceeds maximum allowed length"
    );
    
    // Correction guidance templates
    private static final Map<String, String> CORRECTION_GUIDANCE_TEMPLATES = Map.of(
        "MOBILE_NUMBER_FORMAT_INVALID", "Please provide a mobile number in international format (e.g., +1-555-123-4567)",
        "EMAIL_FORMAT_INVALID", "Please provide a valid email address (e.g., user@example.com)",
        "MOBILE_NUMBER_NOT_UNIQUE", "Please use a different mobile number or contact support if this is your number",
        "EMAIL_NOT_UNIQUE", "Please use a different email address or contact support if this is your email",
        "FIRST_NAME_REQUIRED", "Please enter a valid first name",
        "LAST_NAME_REQUIRED", "Please enter a valid last name",
        "FIELD_TOO_LONG", "Please shorten the %s to fit within the allowed character limit"
    );
    
    // Format examples for different field types with comprehensive international examples
    private static final Map<String, String> FORMAT_EXAMPLES = Map.of(
        "mobileNumber", "+1-555-123-4567 (US), +44-7911-123456 (UK), +81-90-1234-5678 (Japan), +49-151-12345678 (Germany), +33-6-12-34-56-78 (France)",
        "email", "user@example.com, firstname.lastname@company.org, user+tag@domain.co.uk",
        "firstName", "John, Mary, José, François, 李明, محمد",
        "lastName", "Smith, Johnson, García, O'Connor, van der Berg, Al-Rashid"
    );
    
    // Enhanced correction guidance with specific suggestions for common errors
    private static final Map<String, List<String>> ENHANCED_CORRECTION_SUGGESTIONS = Map.of(
        "MOBILE_NUMBER_FORMAT_INVALID", Arrays.asList(
            "Ensure the number starts with a country code (e.g., +1 for US, +44 for UK)",
            "Remove any letters or special characters except +, -, (, ), and spaces",
            "Check that the number has the correct length for your country",
            "Try formatting as: +[country code]-[area code]-[number]"
        ),
        "MOBILE_NUMBER_NOT_UNIQUE", Arrays.asList(
            "Use a different mobile number if you have multiple phones",
            "Contact support at support@petclinic.com if this is your correct number",
            "Check if you already have an account with this number",
            "Consider using a landline number if mobile is not available"
        ),
        "EMAIL_FORMAT_INVALID", Arrays.asList(
            "Ensure the email contains exactly one @ symbol",
            "Check that the domain part has at least one dot (e.g., .com, .org)",
            "Remove any spaces or invalid characters",
            "Use only letters, numbers, dots, hyphens, and underscores"
        ),
        "EMAIL_NOT_UNIQUE", Arrays.asList(
            "Use a different email address if you have multiple accounts",
            "Contact support at support@petclinic.com if this is your correct email",
            "Check if you already have an account with this email",
            "Try adding a number or modifier to your email (e.g., john.doe2@example.com)"
        ),
        "NAME_INVALID_FORMAT", Arrays.asList(
            "Use only letters, spaces, hyphens, and apostrophes",
            "Remove any numbers or special characters",
            "Ensure the name is between 1 and 50 characters",
            "Examples: John, Mary-Jane, O'Connor, José"
        )
    );
    
    // Context-specific guidance based on rejected values
    private static final Map<String, String> CONTEXT_SPECIFIC_GUIDANCE = Map.of(
        "missing_country_code", "Add a country code at the beginning (e.g., +1 for US numbers)",
        "contains_letters", "Remove any letters from the phone number",
        "too_short", "Ensure you've included the full number including area code",
        "missing_at_symbol", "Add an @ symbol between the username and domain",
        "multiple_at_symbols", "Use only one @ symbol in the email address",
        "missing_domain", "Add a domain after the @ symbol (e.g., @example.com)"
    );
    
    @Override
    public ValidationResult aggregateErrors(List<FieldValidationError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return ValidationResult.valid();
        }
        
        // Filter out invalid errors and enhance valid ones
        List<FieldValidationError> validErrors = fieldErrors.stream()
            .filter(this::isValidFieldError)
            .map(this::enhanceFieldError)
            .collect(Collectors.toList());
        
        if (validErrors.isEmpty()) {
            return ValidationResult.valid();
        }
        
        ValidationResult result = ValidationResult.invalid(validErrors);
        result.setOverallMessage(createOverallMessage(validErrors));
        
        // Add metadata about error aggregation
        result.addMetadata("errorCount", validErrors.size());
        result.addMetadata("errorsByField", groupErrorsByField(validErrors));
        result.addMetadata("aggregationTimestamp", System.currentTimeMillis());
        
        return result;
    }
    
    @Override
    public ValidationResult aggregateValidationResults(Map<String, FormatValidationResult> formatResults,
                                                      Map<String, UniquenessValidationResult> uniquenessResults,
                                                      List<FieldValidationError> businessRuleErrors) {
        List<FieldValidationError> allErrors = new ArrayList<>();
        
        // Process format validation results
        if (formatResults != null) {
            for (Map.Entry<String, FormatValidationResult> entry : formatResults.entrySet()) {
                if (!entry.getValue().isValid()) {
                    allErrors.add(createFormatError(entry.getKey(), entry.getValue()));
                }
            }
        }
        
        // Process uniqueness validation results
        if (uniquenessResults != null) {
            for (Map.Entry<String, UniquenessValidationResult> entry : uniquenessResults.entrySet()) {
                if (!entry.getValue().isUnique()) {
                    allErrors.add(createUniquenessError(entry.getKey(), entry.getValue()));
                }
            }
        }
        
        // Add business rule errors
        if (businessRuleErrors != null) {
            allErrors.addAll(businessRuleErrors.stream()
                .filter(this::isValidFieldError)
                .map(this::enhanceFieldError)
                .collect(Collectors.toList()));
        }
        
        return aggregateErrors(allErrors);
    }
    
    @Override
    public FieldValidationError createFieldError(String fieldName, String errorCode, String errorMessage,
                                                String correctionGuidance, Object rejectedValue) {
        // Use ErrorMessageFormatter for enhanced message formatting
        String enhancedMessage = errorMessage;
        if (errorMessage == null || errorMessage.isEmpty()) {
            enhancedMessage = errorMessageFormatter.formatErrorMessage(fieldName, errorCode, rejectedValue);
        } else {
            // Enhance existing message to ensure it includes field context and rejected value
            enhancedMessage = errorMessageFormatter.formatErrorMessage(fieldName, errorCode, rejectedValue, errorMessage);
        }
        
        // Generate enhanced guidance if not provided
        String enhancedGuidance = correctionGuidance;
        if (correctionGuidance == null || correctionGuidance.isEmpty()) {
            enhancedGuidance = errorMessageFormatter.generateDetailedGuidance(fieldName, errorCode, rejectedValue);
        }
        
        FieldValidationError error = new FieldValidationError(
            fieldName, errorCode, enhancedMessage, enhancedGuidance, rejectedValue
        );
        
        // Add format example using ErrorMessageFormatter
        String formatExample = errorMessageFormatter.getFormatExamples(fieldName);
        if (formatExample != null) {
            error.setFormatExample(formatExample);
        }
        
        return error;
    }
    
    @Override
    public FieldValidationError createFormatError(String fieldName, FormatValidationResult formatResult) {
        FieldValidationError error = FieldValidationError.formatError(
            fieldName,
            formatResult.getErrorMessage(),
            formatResult.getCorrectionGuidance(),
            formatResult.getRejectedValue(),
            formatResult.getFormatExample(),
            formatResult.getErrorCode()
        );
        
        // Enhance with additional guidance if needed
        if (error.getCorrectionGuidance() == null || error.getCorrectionGuidance().isEmpty()) {
            error.setCorrectionGuidance(generateCorrectionGuidance(fieldName, formatResult.getErrorCode(), 
                formatResult.getRejectedValue()));
        }
        
        // Add format example if not present
        if (error.getFormatExample() == null && FORMAT_EXAMPLES.containsKey(fieldName)) {
            error.setFormatExample(FORMAT_EXAMPLES.get(fieldName));
        }
        
        return error;
    }
    
    @Override
    public FieldValidationError createUniquenessError(String fieldName, UniquenessValidationResult uniquenessResult) {
        // Use ErrorMessageFormatter for enhanced uniqueness error formatting
        String enhancedMessage = uniquenessResult.getErrorMessage();
        if (enhancedMessage == null || enhancedMessage.isEmpty()) {
            enhancedMessage = errorMessageFormatter.formatUniquenessError(
                fieldName, 
                uniquenessResult.getRejectedValue(), 
                uniquenessResult.getConflictingEntityId()
            );
        }
        
        FieldValidationError error = FieldValidationError.uniquenessError(
            fieldName,
            enhancedMessage,
            uniquenessResult.getCorrectionGuidance(),
            uniquenessResult.getRejectedValue(),
            uniquenessResult.getErrorCode(),
            uniquenessResult.getConflictingEntityId()
        );
        
        // Enhance with detailed guidance if needed
        if (error.getCorrectionGuidance() == null || error.getCorrectionGuidance().isEmpty()) {
            String enhancedGuidance = errorMessageFormatter.generateDetailedGuidance(
                fieldName, 
                uniquenessResult.getErrorCode(), 
                uniquenessResult.getRejectedValue()
            );
            error.setCorrectionGuidance(enhancedGuidance);
        }
        
        return error;
    }
    
    @Override
    public String formatErrorMessage(String fieldName, String baseMessage, String errorType) {
        // Use ErrorMessageFormatter for consistent message formatting
        if (baseMessage == null || baseMessage.isEmpty()) {
            return errorMessageFormatter.formatErrorMessage(fieldName, errorType, null);
        }
        
        // If message doesn't already include field context, enhance it
        if (!baseMessage.toLowerCase().contains(fieldName.toLowerCase())) {
            return errorMessageFormatter.formatErrorMessage(fieldName, errorType, null, baseMessage);
        }
        
        return baseMessage;
    }
    
    @Override
    public String generateCorrectionGuidance(String fieldName, String errorCode, Object rejectedValue) {
        // Use ErrorMessageFormatter for enhanced guidance generation
        return errorMessageFormatter.generateDetailedGuidance(fieldName, errorCode, rejectedValue);
    }
    
    @Override
    public String createOverallMessage(List<FieldValidationError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "All validation checks passed";
        }
        
        int errorCount = fieldErrors.size();
        Map<String, List<FieldValidationError>> errorsByField = groupErrorsByField(fieldErrors);
        int fieldCount = errorsByField.size();
        
        if (fieldCount == 1) {
            String fieldName = errorsByField.keySet().iterator().next();
            if (errorCount == 1) {
                return String.format("Validation failed for %s", fieldName);
            } else {
                return String.format("Validation failed for %s with %d errors", fieldName, errorCount);
            }
        } else {
            return String.format("Validation failed for %d field%s with %d total error%s",
                fieldCount, fieldCount > 1 ? "s" : "",
                errorCount, errorCount > 1 ? "s" : "");
        }
    }
    
    @Override
    public Map<String, List<FieldValidationError>> groupErrorsByField(List<FieldValidationError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return new HashMap<>();
        }
        
        return fieldErrors.stream()
            .filter(error -> error.getFieldName() != null)
            .collect(Collectors.groupingBy(FieldValidationError::getFieldName));
    }
    
    @Override
    public boolean isValidFieldError(FieldValidationError fieldError) {
        return fieldError != null &&
               fieldError.getFieldName() != null && !fieldError.getFieldName().trim().isEmpty() &&
               fieldError.getErrorCode() != null && !fieldError.getErrorCode().trim().isEmpty() &&
               fieldError.getErrorMessage() != null && !fieldError.getErrorMessage().trim().isEmpty();
    }
    
    /**
     * Enhances a field error with additional context and formatting using the ErrorMessageFormatter
     */
    private FieldValidationError enhanceFieldError(FieldValidationError error) {
        if (error == null) {
            return null;
        }
        
        // Use the ErrorMessageFormatter for enhanced formatting
        return errorMessageFormatter.enhanceFieldError(error);
    }
    
    /**
     * Generates format-specific guidance for validation errors
     */
    private String generateFormatGuidance(String fieldName, Object rejectedValue) {
        switch (fieldName.toLowerCase()) {
            case "mobilenumber":
                return "Please provide a mobile number in international format starting with country code (e.g., +1-555-123-4567)";
            case "email":
                return "Please provide a valid email address with @ symbol and domain (e.g., user@example.com)";
            case "firstname":
            case "lastname":
                return "Please provide a name using only letters, spaces, hyphens, and apostrophes";
            default:
                return String.format("Please provide a valid %s in the correct format", 
                    fieldName.replaceAll("([A-Z])", " $1").toLowerCase());
        }
    }
    
    /**
     * Generates enhanced format-specific guidance with context analysis
     */
    private String generateEnhancedFormatGuidance(String fieldName, Object rejectedValue) {
        String baseGuidance = generateFormatGuidance(fieldName, rejectedValue);
        String contextGuidance = analyzeFormatContext(fieldName, rejectedValue);
        
        if (contextGuidance != null) {
            return baseGuidance + " " + contextGuidance;
        }
        return baseGuidance;
    }
    
    /**
     * Generates format guidance with specific context based on rejected value
     */
    private String generateFormatGuidanceWithContext(String fieldName, Object rejectedValue, List<String> suggestions) {
        String contextGuidance = analyzeFormatContext(fieldName, rejectedValue);
        
        if (contextGuidance != null) {
            // Find the most relevant suggestion based on context
            for (String suggestion : suggestions) {
                if (isRelevantSuggestion(suggestion, contextGuidance)) {
                    return suggestion + " " + contextGuidance;
                }
            }
        }
        
        // Return the first suggestion if no context-specific match
        return suggestions.get(0);
    }
    
    /**
     * Analyzes the rejected value to provide context-specific guidance
     */
    private String analyzeFormatContext(String fieldName, Object rejectedValue) {
        if (rejectedValue == null) {
            return null;
        }
        
        String value = rejectedValue.toString().trim();
        
        switch (fieldName.toLowerCase()) {
            case "mobilenumber":
                return analyzeMobileNumberContext(value);
            case "email":
                return analyzeEmailContext(value);
            case "firstname":
            case "lastname":
                return analyzeNameContext(value);
            default:
                return null;
        }
    }
    
    /**
     * Analyzes mobile number context for specific guidance
     */
    private String analyzeMobileNumberContext(String value) {
        if (value.isEmpty()) {
            return null;
        }
        
        if (!value.startsWith("+")) {
            return CONTEXT_SPECIFIC_GUIDANCE.get("missing_country_code");
        }
        
        if (value.matches(".*[a-zA-Z].*")) {
            return CONTEXT_SPECIFIC_GUIDANCE.get("contains_letters");
        }
        
        if (value.length() < 8) {
            return CONTEXT_SPECIFIC_GUIDANCE.get("too_short");
        }
        
        return null;
    }
    
    /**
     * Analyzes email context for specific guidance
     */
    private String analyzeEmailContext(String value) {
        if (value.isEmpty()) {
            return null;
        }
        
        if (!value.contains("@")) {
            return CONTEXT_SPECIFIC_GUIDANCE.get("missing_at_symbol");
        }
        
        long atCount = value.chars().filter(ch -> ch == '@').count();
        if (atCount > 1) {
            return CONTEXT_SPECIFIC_GUIDANCE.get("multiple_at_symbols");
        }
        
        if (value.endsWith("@") || !value.substring(value.indexOf("@")).contains(".")) {
            return CONTEXT_SPECIFIC_GUIDANCE.get("missing_domain");
        }
        
        return null;
    }
    
    /**
     * Analyzes name context for specific guidance
     */
    private String analyzeNameContext(String value) {
        if (value.isEmpty()) {
            return null;
        }
        
        if (value.matches(".*\\d.*")) {
            return "Remove any numbers from the name";
        }
        
        if (value.matches(".*[^a-zA-Z\\s\\-'].*")) {
            return "Remove special characters except hyphens and apostrophes";
        }
        
        return null;
    }
    
    /**
     * Checks if a suggestion is relevant to the given context
     */
    private boolean isRelevantSuggestion(String suggestion, String context) {
        if (context == null || suggestion == null) {
            return false;
        }
        
        String lowerSuggestion = suggestion.toLowerCase();
        String lowerContext = context.toLowerCase();
        
        // Check for keyword matches
        if (lowerContext.contains("country code") && lowerSuggestion.contains("country code")) {
            return true;
        }
        if (lowerContext.contains("letters") && lowerSuggestion.contains("letters")) {
            return true;
        }
        if (lowerContext.contains("@") && lowerSuggestion.contains("@")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Generates uniqueness-specific guidance for validation errors
     */
    private String generateUniquenessGuidance(String fieldName, Object rejectedValue) {
        switch (fieldName.toLowerCase()) {
            case "mobilenumber":
                return "This mobile number is already registered. Please use a different number or contact support if this is your number.";
            case "email":
                return "This email address is already registered. Please use a different email or contact support if this is your email.";
            default:
                return String.format("This %s already exists in the system. Please use a different value.", 
                    fieldName.replaceAll("([A-Z])", " $1").toLowerCase());
        }
    }
    
    /**
     * Generates enhanced uniqueness guidance with alternatives
     */
    private String generateEnhancedUniquenessGuidance(String fieldName, Object rejectedValue) {
        String baseGuidance = generateUniquenessGuidance(fieldName, rejectedValue);
        List<String> alternatives = generateUniquenessAlternatives(fieldName, rejectedValue);
        
        if (!alternatives.isEmpty()) {
            return baseGuidance + " Alternatives: " + String.join(", ", alternatives);
        }
        
        return baseGuidance;
    }
    
    /**
     * Generates uniqueness guidance with specific alternatives
     */
    private String generateUniquenessGuidanceWithAlternatives(String fieldName, Object rejectedValue, List<String> suggestions) {
        StringBuilder guidance = new StringBuilder(suggestions.get(0));
        
        // Add specific alternatives based on field type
        List<String> alternatives = generateUniquenessAlternatives(fieldName, rejectedValue);
        if (!alternatives.isEmpty()) {
            guidance.append(" Suggestions: ").append(String.join("; ", alternatives));
        }
        
        return guidance.toString();
    }
    
    /**
     * Generates specific alternatives for uniqueness errors
     */
    private List<String> generateUniquenessAlternatives(String fieldName, Object rejectedValue) {
        List<String> alternatives = new ArrayList<>();
        
        if (rejectedValue == null) {
            return alternatives;
        }
        
        String value = rejectedValue.toString();
        
        switch (fieldName.toLowerCase()) {
            case "mobilenumber":
                alternatives.add("Try a different phone number if you have multiple");
                alternatives.add("Use a landline number instead");
                alternatives.add("Contact support at support@petclinic.com for assistance");
                break;
            case "email":
                alternatives.addAll(generateEmailAlternatives(value));
                break;
            default:
                alternatives.add("Try a variation of the current value");
                alternatives.add("Contact support for assistance");
                break;
        }
        
        return alternatives;
    }
    
    /**
     * Generates specific email alternatives
     */
    private List<String> generateEmailAlternatives(String email) {
        List<String> alternatives = new ArrayList<>();
        
        if (email.contains("@")) {
            String[] parts = email.split("@");
            if (parts.length == 2) {
                String username = parts[0];
                String domain = parts[1];
                
                // Suggest variations
                alternatives.add("Try " + username + "2@" + domain);
                alternatives.add("Try " + username + ".alt@" + domain);
                alternatives.add("Try " + username + "+clinic@" + domain);
                
                // Suggest different domains if using common ones
                if (domain.equals("gmail.com")) {
                    alternatives.add("Try using a different email provider (e.g., " + username + "@yahoo.com)");
                } else if (domain.equals("yahoo.com")) {
                    alternatives.add("Try using a different email provider (e.g., " + username + "@gmail.com)");
                }
            }
        }
        
        alternatives.add("Contact support at support@petclinic.com for assistance");
        return alternatives;
    }
    
    /**
     * Generates length-specific guidance
     */
    private String generateLengthGuidance(String fieldName, Object rejectedValue) {
        String baseMessage = String.format("Please shorten the %s to fit within the allowed character limit", 
            fieldName.replaceAll("([A-Z])", " $1").toLowerCase());
        
        if (rejectedValue != null) {
            int currentLength = rejectedValue.toString().length();
            int maxLength = getMaxLengthForField(fieldName);
            
            if (maxLength > 0) {
                int excess = currentLength - maxLength;
                return baseMessage + String.format(" (currently %d characters, need to remove %d)", 
                    currentLength, excess);
            }
        }
        
        return baseMessage;
    }
    
    /**
     * Gets the maximum allowed length for a field
     */
    private int getMaxLengthForField(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "firstname":
            case "lastname":
                return 50;
            case "email":
                return 100;
            case "address":
                return 200;
            default:
                return 0;
        }
    }
}