package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.UniquenessValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.service.UniquenessValidatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of UniquenessValidatorService for validating uniqueness constraints
 * Uses optimized database queries to check for duplicate values
 * Validates: Requirements 2.2, 2.3, 2.4
 */
@Service
public class UniquenessValidatorServiceImpl implements UniquenessValidatorService {
    
    private final OwnerRepository ownerRepository;
    
    @Autowired
    public UniquenessValidatorServiceImpl(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }
    
    @Override
    public UniquenessValidationResult validateMobileNumberUniqueness(String mobileNumber) {
        return validateMobileNumberUniqueness(mobileNumber, null);
    }
    
    @Override
    public UniquenessValidationResult validateMobileNumberUniqueness(String mobileNumber, Long excludeOwnerId) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            // Empty mobile numbers are considered unique (handled by format validation)
            return UniquenessValidationResult.unique();
        }
        
        String trimmedMobileNumber = mobileNumber.trim();
        
        try {
            // Check if mobile number exists
            Optional<Owner> existingOwner = ownerRepository.findByTelephone(trimmedMobileNumber);
            
            if (existingOwner.isPresent()) {
                Owner owner = existingOwner.get();
                
                // If this is an update and the mobile number belongs to the same owner, it's allowed
                if (excludeOwnerId != null && owner.getId().equals(excludeOwnerId)) {
                    return UniquenessValidationResult.unique();
                }
                
                // Mobile number already exists for a different owner
                return UniquenessValidationResult.notUnique(
                    "Mobile number " + trimmedMobileNumber + " already exists in the system",
                    "This mobile number is already registered to another owner. Please use a different mobile number, or contact support at support@petclinic.com if this is your correct number and you believe there's an error",
                    trimmedMobileNumber,
                    "MOBILE_NOT_UNIQUE",
                    owner.getId()
                );
            }
            
            return UniquenessValidationResult.unique();
            
        } catch (Exception e) {
            // Log the error and return a generic validation failure
            return UniquenessValidationResult.notUnique(
                "Unable to validate mobile number uniqueness due to system error",
                "Please try again in a moment. If the problem persists, contact support at support@petclinic.com",
                trimmedMobileNumber,
                "MOBILE_UNIQUENESS_CHECK_FAILED",
                null
            );
        }
    }
    
    @Override
    public UniquenessValidationResult validateEmailUniqueness(String email) {
        return validateEmailUniqueness(email, null);
    }
    
    @Override
    public UniquenessValidationResult validateEmailUniqueness(String email, Long excludeOwnerId) {
        if (email == null || email.trim().isEmpty()) {
            // Empty emails are considered unique (handled by format validation)
            return UniquenessValidationResult.unique();
        }
        
        String trimmedEmail = email.trim().toLowerCase(); // Normalize email to lowercase
        
        try {
            // Check if email exists
            Optional<Owner> existingOwner = ownerRepository.findByEmail(trimmedEmail);
            
            if (existingOwner.isPresent()) {
                Owner owner = existingOwner.get();
                
                // If this is an update and the email belongs to the same owner, it's allowed
                if (excludeOwnerId != null && owner.getId().equals(excludeOwnerId)) {
                    return UniquenessValidationResult.unique();
                }
                
                // Email already exists for a different owner
                return UniquenessValidationResult.notUnique(
                    "Email address " + trimmedEmail + " already exists in the system",
                    "This email address is already registered to another owner. Please use a different email address, or contact support at support@petclinic.com if this is your correct email and you believe there's an error",
                    trimmedEmail,
                    "EMAIL_NOT_UNIQUE",
                    owner.getId()
                );
            }
            
            return UniquenessValidationResult.unique();
            
        } catch (Exception e) {
            // Log the error and return a generic validation failure
            return UniquenessValidationResult.notUnique(
                "Unable to validate email uniqueness due to system error",
                "Please try again in a moment. If the problem persists, contact support at support@petclinic.com",
                trimmedEmail,
                "EMAIL_UNIQUENESS_CHECK_FAILED",
                null
            );
        }
    }
}