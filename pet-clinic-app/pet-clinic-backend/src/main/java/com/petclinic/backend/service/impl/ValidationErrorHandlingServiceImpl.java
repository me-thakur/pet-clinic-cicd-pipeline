package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.ValidationResponse;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.OwnerValidationService;
import com.petclinic.backend.service.ValidationErrorHandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Implementation of ValidationErrorHandlingService
 * Provides comprehensive error handling, graceful degradation, and offline validation caching
 * for validation operations
 * 
 * Validates: Requirements 6.3
 */
@Service
public class ValidationErrorHandlingServiceImpl implements ValidationErrorHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationErrorHandlingServiceImpl.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 500; // 0.5 seconds
    private static final long CACHE_EXPIRY_MS = 300000; // 5 minutes
    
    // Cache for validation results
    private final Map<String, CachedValidationResult> validationCache = new ConcurrentHashMap<>();
    
    // Basic validation patterns for offline validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[\\+]?[1-9]?[0-9]{7,15}$");
    private static final Pattern ZIP_PATTERN = Pattern.compile(
        "^[A-Z0-9\\s-]{3,10}$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private OwnerValidationService ownerValidationService;

    @Override
    public ValidationResponse executeValidationWithGracefulDegradation(
            ValidationOperation validationOperation,
            Owner owner,
            String validationType,
            ValidationContext context) {
        
        logger.debug("Executing validation with graceful degradation for type: {}", validationType);
        
        Exception lastException = null;
        
        // Try full validation service with retry
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("Attempt {} of {} for {} validation", attempt, MAX_RETRY_ATTEMPTS, validationType);
                
                ValidationResult result = validationOperation.execute();
                
                if (attempt > 1) {
                    logger.info("Validation succeeded on attempt {} for type: {}", attempt, validationType);
                }
                
                // Cache successful result
                cacheValidationResult(owner, validationType, result);
                
                // Return successful response
                ValidationResponse response = ValidationResponse.fromValidationResult(result);
                response.setValidationContext(validationType);
                return response;
                
            } catch (Exception e) {
                lastException = e;
                logValidationError(validationType, owner, e, attempt);
                
                // Don't retry on the last attempt
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    break;
                }
                
                // Check if error is retryable
                if (!isRetryableError(e)) {
                    logger.warn("Non-retryable error encountered for {} validation, stopping retries", validationType);
                    break;
                }
                
                // Wait before retry
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry delay interrupted for {} validation", validationType);
                    break;
                }
            }
        }
        
        // Full validation failed, try cached result
        logger.warn("Full validation service failed for {}, checking cache", validationType);
        ValidationResult cachedResult = getCachedValidationResult(owner, validationType);
        if (cachedResult != null) {
            logger.info("Using cached validation result for type: {}", validationType);
            return createWarningResponse(cachedResult, validationType, 
                "Using cached validation result due to service unavailability. Please retry later for full validation.");
        }
        
        // No cache available, perform basic offline validation
        logger.info("Performing offline validation for type: {}", validationType);
        ValidationResult offlineResult = performOfflineValidation(owner, validationType);
        
        return createWarningResponse(offlineResult, validationType,
            "Validation service is temporarily unavailable. Basic validation performed. Please submit with caution and retry later for full validation.");
    }

    @Override
    public boolean isValidationServiceAvailable() {
        try {
            // Simple health check - try to validate a minimal owner
            Owner testOwner = new Owner();
            testOwner.setFirstName("Test");
            testOwner.setLastName("User");
            testOwner.setEmail("test@example.com");
            
            ownerValidationService.validateNewOwner(testOwner);
            return true;
        } catch (Exception e) {
            logger.warn("Validation service availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ValidationResult getCachedValidationResult(Owner owner, String validationType) {
        String cacheKey = generateCacheKey(owner, validationType);
        CachedValidationResult cached = validationCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            logger.debug("Found valid cached result for {} validation", validationType);
            return cached.getResult();
        }
        
        // Remove expired cache entry
        if (cached != null) {
            validationCache.remove(cacheKey);
            logger.debug("Removed expired cache entry for {} validation", validationType);
        }
        
        return null;
    }

    @Override
    public void cacheValidationResult(Owner owner, String validationType, ValidationResult result) {
        try {
            String cacheKey = generateCacheKey(owner, validationType);
            CachedValidationResult cached = new CachedValidationResult(result, System.currentTimeMillis() + CACHE_EXPIRY_MS);
            validationCache.put(cacheKey, cached);
            logger.debug("Cached validation result for {} validation", validationType);
        } catch (Exception e) {
            logger.warn("Failed to cache validation result for {}: {}", validationType, e.getMessage());
        }
    }

    @Override
    public ValidationResult performOfflineValidation(Owner owner, String validationType) {
        logger.debug("Performing offline validation for type: {}", validationType);
        
        ValidationResult result = new ValidationResult();
        result.setValid(true); // Start optimistic
        
        // Basic field validation
        if (owner.getFirstName() == null || owner.getFirstName().trim().isEmpty()) {
            result.addError("firstName", "First name is required", "Please enter a first name");
            result.setValid(false);
        } else if (owner.getFirstName().length() > 50) {
            result.addError("firstName", "First name is too long", "Please enter a first name with 50 characters or less");
            result.setValid(false);
        }
        
        if (owner.getLastName() == null || owner.getLastName().trim().isEmpty()) {
            result.addError("lastName", "Last name is required", "Please enter a last name");
            result.setValid(false);
        } else if (owner.getLastName().length() > 50) {
            result.addError("lastName", "Last name is too long", "Please enter a last name with 50 characters or less");
            result.setValid(false);
        }
        
        // Email validation
        if (owner.getEmail() != null && !owner.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(owner.getEmail()).matches()) {
                result.addError("email", "Invalid email format", "Please enter a valid email address (e.g., user@example.com)");
                result.setValid(false);
            }
        }
        
        // Phone validation
        if (owner.getTelephone() != null && !owner.getTelephone().trim().isEmpty()) {
            String cleanPhone = owner.getTelephone().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                result.addError("telephone", "Invalid phone format", "Please enter a valid phone number");
                result.setValid(false);
            }
        }
        
        if (owner.getMobileNumber() != null && !owner.getMobileNumber().trim().isEmpty()) {
            String cleanMobile = owner.getMobileNumber().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanMobile).matches()) {
                result.addError("mobileNumber", "Invalid mobile format", "Please enter a valid mobile number");
                result.setValid(false);
            }
        }
        
        // ZIP code validation
        if (owner.getZipCode() != null && !owner.getZipCode().trim().isEmpty()) {
            if (!ZIP_PATTERN.matcher(owner.getZipCode()).matches()) {
                result.addError("zipCode", "Invalid ZIP code format", "Please enter a valid ZIP/postal code");
                result.setValid(false);
            }
        }
        
        // Address validation
        if (owner.getAddress() != null && owner.getAddress().length() > 255) {
            result.addError("address", "Address is too long", "Please enter an address with 255 characters or less");
            result.setValid(false);
        }
        
        if (owner.getCity() != null && owner.getCity().length() > 100) {
            result.addError("city", "City name is too long", "Please enter a city name with 100 characters or less");
            result.setValid(false);
        }
        
        if (owner.getState() != null && owner.getState().length() > 50) {
            result.addError("state", "State name is too long", "Please enter a state name with 50 characters or less");
            result.setValid(false);
        }
        
        logger.debug("Offline validation completed for type: {} with {} errors", validationType, result.getErrorCount());
        return result;
    }

    @Override
    public String getValidationErrorMessage(String validationType, Throwable error) {
        if (error == null) {
            return String.format("Validation service is temporarily unavailable for %s validation. Basic validation will be performed.", validationType);
        }
        
        // Categorize errors and provide appropriate messages
        if (error instanceof TimeoutException) {
            return String.format("%s validation is taking longer than expected. We'll perform basic validation instead.", 
                                capitalize(validationType));
        }
        
        if (error instanceof SQLException) {
            return "We're experiencing database connectivity issues. Basic validation will be performed while we resolve this.";
        }
        
        if (error instanceof IllegalArgumentException) {
            return String.format("Invalid data provided for %s validation. Please check your input and try again.", validationType);
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("connection")) {
            return "We're experiencing connectivity issues. Basic validation will be performed.";
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("timeout")) {
            return String.format("%s validation timed out. We'll perform basic validation instead.", capitalize(validationType));
        }
        
        // Generic error message
        return String.format("We encountered an issue with %s validation. Basic validation will be performed.", validationType);
    }

    @Override
    public void logValidationError(String validationType, Owner owner, Throwable error, int attemptNumber) {
        logger.error("Validation error details - Type: {}, Owner: {} {}, Attempt: {}, Error: {}, Message: {}", 
                    validationType, owner.getFirstName(), owner.getLastName(), attemptNumber, 
                    error.getClass().getSimpleName(), error.getMessage());
        
        // Log stack trace for debugging (only on first attempt to avoid spam)
        if (attemptNumber == 1) {
            logger.debug("Full stack trace for {} validation error:", validationType, error);
        }
        
        // Log additional context
        logger.error("Validation context - Service available: {}, Thread: {}, Timestamp: {}", 
                    isValidationServiceAvailable(), Thread.currentThread().getName(), 
                    System.currentTimeMillis());
    }

    @Override
    public ValidationResponse createWarningResponse(ValidationResult result, String validationType, String warningMessage) {
        ValidationResponse response = ValidationResponse.fromValidationResult(result);
        response.setValidationContext(validationType);
        response.setWarning(true);
        response.setWarningMessage(warningMessage);
        response.addMetadata("degradedValidation", true);
        response.addMetadata("validationMode", "offline");
        response.addMetadata("retryRecommended", true);
        
        return response;
    }

    /**
     * Determine if an error is retryable
     */
    private boolean isRetryableError(Exception e) {
        // Don't retry validation errors or illegal arguments
        if (e instanceof IllegalArgumentException) {
            return false;
        }
        
        // Don't retry security-related errors
        if (e instanceof SecurityException) {
            return false;
        }
        
        // Retry database connection issues, timeouts, and temporary failures
        if (e instanceof SQLException || 
            e instanceof TimeoutException ||
            (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection") ||
                e.getMessage().toLowerCase().contains("timeout") ||
                e.getMessage().toLowerCase().contains("temporary")
            ))) {
            return true;
        }
        
        // Default to retrying unknown exceptions
        return true;
    }

    /**
     * Generate cache key for validation result
     */
    private String generateCacheKey(Owner owner, String validationType) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(validationType).append(":");
        keyBuilder.append(owner.getFirstName()).append(":");
        keyBuilder.append(owner.getLastName()).append(":");
        keyBuilder.append(owner.getEmail()).append(":");
        keyBuilder.append(owner.getTelephone()).append(":");
        keyBuilder.append(owner.getMobileNumber());
        
        return keyBuilder.toString().hashCode() + "";
    }

    /**
     * Capitalize first letter of string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Cached validation result with expiry
     */
    private static class CachedValidationResult {
        private final ValidationResult result;
        private final long expiryTime;

        public CachedValidationResult(ValidationResult result, long expiryTime) {
            this.result = result;
            this.expiryTime = expiryTime;
        }

        public ValidationResult getResult() {
            return result;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}