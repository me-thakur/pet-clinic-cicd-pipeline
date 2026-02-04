package com.petclinic.backend.service;

import com.petclinic.backend.dto.FieldError;
import com.petclinic.backend.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating helpful error suggestions and recovery guidance
 */
@Service
public class ErrorSuggestionService {
    
    private static final Map<String, List<String>> FIELD_SUGGESTIONS = Map.of(
        "email", List.of(
            "Ensure the email address is in the format: user@domain.com",
            "Check for typos in the email address",
            "Make sure the email address is unique (not already registered)"
        ),
        "telephone", List.of(
            "Enter a valid phone number with area code",
            "Use format: (123) 456-7890 or 123-456-7890",
            "Include country code for international numbers"
        ),
        "postalCode", List.of(
            "Enter a valid postal code for your country",
            "US: 12345 or 12345-6789",
            "Canada: A1A 1A1",
            "UK: SW1A 1AA"
        ),
        "firstName", List.of(
            "First name is required",
            "Use only letters and spaces",
            "Must be between 2 and 50 characters"
        ),
        "lastName", List.of(
            "Last name is required",
            "Use only letters and spaces",
            "Must be between 2 and 50 characters"
        ),
        "address", List.of(
            "Enter a complete street address",
            "Include street number and name",
            "Apartment/unit number is optional"
        ),
        "city", List.of(
            "City name is required",
            "Use only letters and spaces",
            "Check spelling of city name"
        )
    );
    
    private static final Map<String, List<String>> ERROR_CODE_SUGGESTIONS = Map.of(
        "EMAIL_DUPLICATE", List.of(
            "This email address is already registered",
            "Try using a different email address",
            "If this is your email, try logging in instead"
        ),
        "VALIDATION_SERVICE_UNAVAILABLE", List.of(
            "Validation service is temporarily unavailable",
            "Basic validation is being performed",
            "You can submit with caution and retry later for full validation"
        ),
        "NETWORK_ERROR", List.of(
            "Check your internet connection",
            "Try refreshing the page",
            "Contact support if the problem persists"
        ),
        "SERVER_ERROR", List.of(
            "An unexpected server error occurred",
            "Please try again in a few moments",
            "Contact support if the problem continues"
        ),
        "AUTHENTICATION_FAILED", List.of(
            "Check your username and password",
            "Ensure caps lock is not enabled",
            "Try resetting your password if needed"
        ),
        "ACCESS_DENIED", List.of(
            "You don't have permission to perform this action",
            "Contact an administrator for access",
            "Make sure you're logged in with the correct account"
        )
    );
    
    /**
     * Generate suggestions for validation exceptions
     */
    public List<String> generateSuggestions(ValidationException ex) {
        List<String> suggestions = new ArrayList<>();
        
        // Add general validation suggestions
        suggestions.add("Please review and correct the highlighted fields");
        suggestions.add("All required fields must be filled out");
        
        // Add field-specific suggestions
        if (ex.getFieldErrors() != null) {
            for (FieldError fieldError : ex.getFieldErrors()) {
                List<String> fieldSuggestions = FIELD_SUGGESTIONS.get(fieldError.getField());
                if (fieldSuggestions != null) {
                    suggestions.addAll(fieldSuggestions);
                }
            }
        }
        
        // Add error code specific suggestions
        List<String> codeSuggestions = ERROR_CODE_SUGGESTIONS.get(ex.getErrorCode());
        if (codeSuggestions != null) {
            suggestions.addAll(codeSuggestions);
        }
        
        return suggestions;
    }
    
    /**
     * Generate suggestions for specific error codes
     */
    public List<String> generateSuggestions(String errorCode) {
        return ERROR_CODE_SUGGESTIONS.getOrDefault(errorCode, List.of(
            "Please try again",
            "Contact support if the problem persists"
        ));
    }
    
    /**
     * Generate suggestions for field-specific errors
     */
    public List<String> generateFieldSuggestions(String fieldName, String errorCode) {
        List<String> suggestions = new ArrayList<>();
        
        // Add field-specific suggestions
        List<String> fieldSuggestions = FIELD_SUGGESTIONS.get(fieldName);
        if (fieldSuggestions != null) {
            suggestions.addAll(fieldSuggestions);
        }
        
        // Add error code specific suggestions
        List<String> codeSuggestions = ERROR_CODE_SUGGESTIONS.get(errorCode);
        if (codeSuggestions != null) {
            suggestions.addAll(codeSuggestions);
        }
        
        return suggestions;
    }
    
    /**
     * Generate network error suggestions
     */
    public List<String> generateNetworkErrorSuggestions() {
        return List.of(
            "Check your internet connection",
            "Try refreshing the page",
            "Clear your browser cache and cookies",
            "Try using a different browser",
            "Contact support if the problem persists"
        );
    }
    
    /**
     * Generate service unavailable suggestions
     */
    public List<String> generateServiceUnavailableSuggestions(String serviceName) {
        return List.of(
            serviceName + " is temporarily unavailable",
            "Please try again in a few minutes",
            "Some features may work with reduced functionality",
            "Contact support if the service remains unavailable"
        );
    }
    
    /**
     * Generate authentication error suggestions
     */
    public List<String> generateAuthenticationSuggestions() {
        return List.of(
            "Verify your username and password are correct",
            "Check that caps lock is not enabled",
            "Try clearing your browser cookies",
            "Use the 'Forgot Password' link if needed",
            "Contact support if you continue having trouble logging in"
        );
    }
    
    /**
     * Generate authorization error suggestions
     */
    public List<String> generateAuthorizationSuggestions(String resource) {
        return List.of(
            "You don't have permission to access " + resource,
            "Contact your administrator to request access",
            "Make sure you're logged in with the correct account",
            "Some features may require elevated privileges"
        );
    }
}