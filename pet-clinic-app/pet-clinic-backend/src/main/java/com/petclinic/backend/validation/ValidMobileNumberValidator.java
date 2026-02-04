package com.petclinic.backend.validation;

import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.FormatValidatorService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validator implementation for @ValidMobileNumber annotation
 * Uses FormatValidatorService to validate mobile numbers against international standards
 * Validates: Requirements 2.1, 2.5
 */
@Component
public class ValidMobileNumberValidator implements ConstraintValidator<ValidMobileNumber, String> {
    
    @Autowired
    private FormatValidatorService formatValidatorService;
    
    private String countryCode;
    private boolean allowNull;
    
    @Override
    public void initialize(ValidMobileNumber constraintAnnotation) {
        this.countryCode = constraintAnnotation.countryCode();
        this.allowNull = constraintAnnotation.allowNull();
    }
    
    @Override
    public boolean isValid(String mobileNumber, ConstraintValidatorContext context) {
        // Allow null values if configured to do so
        if (mobileNumber == null) {
            return allowNull;
        }
        
        // Allow empty strings if null is allowed
        if (mobileNumber.trim().isEmpty()) {
            return allowNull;
        }
        
        // If formatValidatorService is not available (e.g., during testing or startup),
        // perform basic validation
        if (formatValidatorService == null) {
            return performBasicMobileValidation(mobileNumber);
        }
        
        // Validate mobile number format
        FormatValidationResult result;
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            result = formatValidatorService.validateMobileNumber(mobileNumber, countryCode);
        } else {
            result = formatValidatorService.validateMobileNumber(mobileNumber);
        }
        
        // If validation fails, customize the error message
        if (!result.isValid()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(result.getErrorMessage())
                   .addConstraintViolation();
        }
        
        return result.isValid();
    }
    
    /**
     * Basic mobile number validation when FormatValidatorService is not available
     */
    private boolean performBasicMobileValidation(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return allowNull;
        }
        
        // Basic validation: check if it contains only digits, spaces, hyphens, parentheses, and plus sign
        String cleanNumber = mobileNumber.replaceAll("[\\s\\-\\(\\)\\+]", "");
        
        // Must contain only digits after cleaning
        if (!cleanNumber.matches("\\d+")) {
            return false;
        }
        
        // Must be between 7 and 15 digits (international standard)
        return cleanNumber.length() >= 7 && cleanNumber.length() <= 15;
    }
}