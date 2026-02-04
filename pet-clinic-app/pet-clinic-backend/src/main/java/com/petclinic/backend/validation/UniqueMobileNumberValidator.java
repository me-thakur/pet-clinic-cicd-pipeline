package com.petclinic.backend.validation;

import com.petclinic.backend.dto.UniquenessValidationResult;
import com.petclinic.backend.service.UniquenessValidatorService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Validator implementation for @UniqueMobileNumber annotation
 * Uses UniquenessValidatorService to check mobile number uniqueness in the system
 * Validates: Requirements 2.2, 2.3, 2.4
 */
@Component
public class UniqueMobileNumberValidator implements ConstraintValidator<UniqueMobileNumber, String> {
    
    @Autowired
    private UniquenessValidatorService uniquenessValidatorService;
    
    private boolean allowNull;
    private String ownerIdField;
    
    @Override
    public void initialize(UniqueMobileNumber constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.ownerIdField = constraintAnnotation.ownerIdField();
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
        
        // If uniquenessValidatorService is not available (e.g., during testing or startup),
        // skip uniqueness validation and return true
        if (uniquenessValidatorService == null) {
            return true; // Skip uniqueness check when service is not available
        }
        
        // Get owner ID for update scenarios
        Long excludeOwnerId = getOwnerIdFromContext(context);
        
        // Validate mobile number uniqueness
        UniquenessValidationResult result;
        if (excludeOwnerId != null) {
            result = uniquenessValidatorService.validateMobileNumberUniqueness(mobileNumber, excludeOwnerId);
        } else {
            result = uniquenessValidatorService.validateMobileNumberUniqueness(mobileNumber);
        }
        
        // If validation fails, customize the error message
        if (!result.isUnique()) {
            context.disableDefaultConstraintViolation();
            String errorMessage = String.format("Mobile number %s already exists in the system", mobileNumber);
            context.buildConstraintViolationWithTemplate(errorMessage)
                   .addConstraintViolation();
        }
        
        return result.isUnique();
    }
    
    /**
     * Attempts to extract owner ID from the validation context for update scenarios
     * This is a simplified implementation - in practice, you might need to pass
     * the owner ID through a different mechanism (e.g., validation groups or context)
     */
    private Long getOwnerIdFromContext(ConstraintValidatorContext context) {
        // For now, return null - this will be enhanced when we implement
        // the full validation service that can handle update scenarios
        // The proper implementation would involve passing the owner ID
        // through the validation context or using a different approach
        return null;
    }
}