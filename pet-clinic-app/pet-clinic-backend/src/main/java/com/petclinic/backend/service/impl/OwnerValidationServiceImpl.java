package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.FormatValidatorService;
import com.petclinic.backend.service.OwnerValidationService;
import com.petclinic.backend.service.UniquenessValidatorService;
import com.petclinic.backend.service.ValidationMetricsService;
import com.petclinic.backend.service.ValidationAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Implementation of OwnerValidationService for orchestrating comprehensive owner validation
 * Coordinates multiple validation types and aggregates results into structured responses
 * Enhanced with comprehensive logging and metrics collection for monitoring and audit
 * 
 * NEW ENHANCEMENTS (Requirements 3.1, 3.2, 3.3, 3.4, 3.5):
 * - Service availability checking with circuit breaker pattern
 * - Client-side validation fallback when service unavailable
 * - Comprehensive validation with specific field-level feedback
 * - Automatic retry mechanisms for validation service recovery
 * - Enhanced error messages with corrective guidance
 * 
 * Validates: Requirements 1.2, 1.3, 1.5, 4.4, 4.5, 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Service
public class OwnerValidationServiceImpl implements OwnerValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(OwnerValidationServiceImpl.class);
    
    // Circuit breaker configuration
    @Value("${petclinic.validation.circuit-breaker.failure-threshold:5}")
    private int circuitBreakerFailureThreshold;
    
    @Value("${petclinic.validation.circuit-breaker.recovery-timeout:30000}")
    private long circuitBreakerRecoveryTimeout; // 30 seconds
    
    @Value("${petclinic.validation.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${petclinic.validation.retry.delay-ms:1000}")
    private long retryDelayMs;
    
    // Circuit breaker state
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile CircuitBreakerState circuitBreakerState = CircuitBreakerState.CLOSED;
    
    // Client-side validation patterns for fallback
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[\\+]?[1-9]?[0-9]{7,15}$");
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z\\s\\-\\']{1,50}$");
    private static final Pattern ZIP_PATTERN = Pattern.compile(
        "^[A-Z0-9\\s\\-]{3,10}$", Pattern.CASE_INSENSITIVE);
    
    private final FormatValidatorService formatValidatorService;
    private final UniquenessValidatorService uniquenessValidatorService;
    private final ValidationMetricsService validationMetricsService;
    private final ValidationAuditService validationAuditService;
    
    // Define all validatable fields
    private static final Set<String> VALIDATABLE_FIELDS = Set.of(
        "firstName", "lastName", "email", "mobileNumber", "address", 
        "city", "state", "zipCode", "telephone"
    );
    
    /**
     * Circuit breaker states for service availability management
     */
    private enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Service unavailable, using fallback
        HALF_OPEN  // Testing if service has recovered
    }
    
    @Autowired
    public OwnerValidationServiceImpl(FormatValidatorService formatValidatorService,
                                    UniquenessValidatorService uniquenessValidatorService,
                                    ValidationMetricsService validationMetricsService,
                                    ValidationAuditService validationAuditService) {
        this.formatValidatorService = formatValidatorService;
        this.uniquenessValidatorService = uniquenessValidatorService;
        this.validationMetricsService = validationMetricsService;
        this.validationAuditService = validationAuditService;
    }
    
    @Override
    public ValidationResult validateNewOwner(Owner owner) {
        long startTime = System.currentTimeMillis();
        String validationType = "new_owner";
        
        try {
            logger.debug("Starting validation for new owner: {}", 
                        owner != null ? owner.getFirstName() + " " + owner.getLastName() : "null");
            
            if (owner == null) {
                ValidationResult result = ValidationResult.invalid("Owner data is required");
                result.addFieldError("owner", "OWNER_NULL", "Owner data is required", 
                                   "Please provide valid owner information", null);
                
                long processingTime = System.currentTimeMillis() - startTime;
                recordValidationMetricsAndAudit(validationType, owner, null, result, processingTime);
                return result;
            }
            
            // Check circuit breaker state and attempt validation with fallback
            ValidationResult result = executeValidationWithCircuitBreaker(
                () -> performValidation(owner, null, VALIDATABLE_FIELDS),
                owner, validationType
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record metrics and audit logs
            recordValidationMetricsAndAudit(validationType, owner, null, result, processingTime);
            
            logger.debug("Completed validation for new owner in {}ms: success={}, errorCount={}", 
                        processingTime, result.isValid(), result.getErrorCount());
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Error during new owner validation: {}", e.getMessage(), e);
            
            // Log system error
            validationAuditService.logValidationSystemError(validationType, owner, e, "REST_API");
            
            // Fallback to client-side validation on system error
            ValidationResult fallbackResult = performClientSideValidation(owner, VALIDATABLE_FIELDS);
            fallbackResult.addMetadata("warning", "Validation service encountered an error. Basic validation performed. Please retry later for full validation.");
            fallbackResult.addMetadata("fallbackReason", "SYSTEM_ERROR");
            fallbackResult.addMetadata("originalError", e.getMessage());
            
            recordValidationMetricsAndAudit(validationType, owner, null, fallbackResult, processingTime);
            return fallbackResult;
        }
    }
    
    @Override
    public ValidationResult validateOwnerUpdate(Long ownerId, Owner owner) {
        long startTime = System.currentTimeMillis();
        String validationType = "owner_update";
        
        try {
            logger.debug("Starting validation for owner update: ownerId={}, owner={}", 
                        ownerId, owner != null ? owner.getFirstName() + " " + owner.getLastName() : "null");
            
            if (owner == null) {
                ValidationResult result = ValidationResult.invalid("Owner data is required");
                result.addFieldError("owner", "OWNER_NULL", "Owner data is required", 
                                   "Please provide valid owner information", null);
                
                long processingTime = System.currentTimeMillis() - startTime;
                recordValidationMetricsAndAudit(validationType, owner, ownerId, result, processingTime);
                return result;
            }
            
            if (ownerId == null || ownerId <= 0) {
                ValidationResult result = ValidationResult.invalid("Valid owner ID is required for updates");
                result.addFieldError("ownerId", "OWNER_ID_INVALID", "Valid owner ID is required for updates", 
                                   "Please provide a valid positive owner ID", ownerId);
                
                long processingTime = System.currentTimeMillis() - startTime;
                recordValidationMetricsAndAudit(validationType, owner, ownerId, result, processingTime);
                return result;
            }
            
            // Check circuit breaker state and attempt validation with fallback
            ValidationResult result = executeValidationWithCircuitBreaker(
                () -> performValidation(owner, ownerId, VALIDATABLE_FIELDS),
                owner, validationType
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record metrics and audit logs
            recordValidationMetricsAndAudit(validationType, owner, ownerId, result, processingTime);
            
            logger.debug("Completed validation for owner update in {}ms: ownerId={}, success={}, errorCount={}", 
                        processingTime, ownerId, result.isValid(), result.getErrorCount());
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Error during owner update validation for ownerId {}: {}", ownerId, e.getMessage(), e);
            
            // Log system error
            validationAuditService.logValidationSystemError(validationType, owner, e, "REST_API");
            
            // Fallback to client-side validation on system error
            ValidationResult fallbackResult = performClientSideValidation(owner, VALIDATABLE_FIELDS);
            fallbackResult.addMetadata("warning", "Validation service encountered an error. Basic validation performed. Please retry later for full validation.");
            fallbackResult.addMetadata("fallbackReason", "SYSTEM_ERROR");
            fallbackResult.addMetadata("originalError", e.getMessage());
            fallbackResult.addMetadata("ownerId", ownerId);
            
            recordValidationMetricsAndAudit(validationType, owner, ownerId, fallbackResult, processingTime);
            return fallbackResult;
        }
    }
    
    @Override
    public ValidationResult validateOwnerFields(Owner owner, String... fieldNames) {
        if (owner == null) {
            ValidationResult result = ValidationResult.invalid("Owner data is required");
            result.addFieldError("owner", "OWNER_NULL", "Owner data is required", 
                               "Please provide valid owner information", null);
            return result;
        }
        
        Set<String> fieldsToValidate = getValidFieldNames(fieldNames);
        return performValidation(owner, null, fieldsToValidate);
    }
    
    @Override
    public ValidationResult validateOwnerFieldsUpdate(Long ownerId, Owner owner, String... fieldNames) {
        if (owner == null) {
            ValidationResult result = ValidationResult.invalid("Owner data is required");
            result.addFieldError("owner", "OWNER_NULL", "Owner data is required", 
                               "Please provide valid owner information", null);
            return result;
        }
        
        if (ownerId == null || ownerId <= 0) {
            ValidationResult result = ValidationResult.invalid("Valid owner ID is required for updates");
            result.addFieldError("ownerId", "OWNER_ID_INVALID", "Valid owner ID is required for updates", 
                               "Please provide a valid positive owner ID", ownerId);
            return result;
        }
        
        Set<String> fieldsToValidate = getValidFieldNames(fieldNames);
        
        // For partial field updates, we still need to ensure the fields being updated
        // are validated against current rules, but we can optimize by only validating
        // the specified fields (Requirement 2.4 - optimize validation for partial updates)
        return performValidation(owner, ownerId, fieldsToValidate);
    }
    
    /**
     * Validates an owner update with enhanced optimization for partial updates
     * This method allows for more granular control over which fields are validated
     * while still ensuring complete validation of the specified fields
     * 
     * @param ownerId The ID of the owner being updated
     * @param owner The owner data to validate
     * @param validateOnlyChangedFields If true, only validates non-null fields
     * @param fieldNames Specific fields to validate (optional)
     * @return ValidationResult containing validation outcome
     */
    public ValidationResult validateOwnerUpdateOptimized(Long ownerId, Owner owner, 
                                                        boolean validateOnlyChangedFields, 
                                                        String... fieldNames) {
        if (owner == null) {
            ValidationResult result = ValidationResult.invalid("Owner data is required");
            result.addFieldError("owner", "OWNER_NULL", "Owner data is required", 
                               "Please provide valid owner information", null);
            return result;
        }
        
        if (ownerId == null || ownerId <= 0) {
            ValidationResult result = ValidationResult.invalid("Valid owner ID is required for updates");
            result.addFieldError("ownerId", "OWNER_ID_INVALID", "Valid owner ID is required for updates", 
                               "Please provide a valid positive owner ID", ownerId);
            return result;
        }
        
        Set<String> fieldsToValidate;
        
        if (validateOnlyChangedFields) {
            // Optimize for partial updates by only validating non-null fields
            fieldsToValidate = getChangedFields(owner);
        } else if (fieldNames != null && fieldNames.length > 0) {
            // Validate specific fields
            fieldsToValidate = getValidFieldNames(fieldNames);
        } else {
            // Default: validate entire record (Requirement 4.4)
            fieldsToValidate = VALIDATABLE_FIELDS;
        }
        
        return performValidation(owner, ownerId, fieldsToValidate);
    }
    
    /**
     * Execute validation with circuit breaker pattern and automatic retry
     * Implements Requirements 3.1, 3.2, 3.5
     */
    private ValidationResult executeValidationWithCircuitBreaker(
            ValidationOperation operation, Owner owner, String validationType) {
        
        // Check circuit breaker state
        if (circuitBreakerState == CircuitBreakerState.OPEN) {
            if (shouldAttemptRecovery()) {
                circuitBreakerState = CircuitBreakerState.HALF_OPEN;
                logger.info("Circuit breaker transitioning to HALF_OPEN for recovery attempt");
            } else {
                logger.warn("Circuit breaker is OPEN, using client-side validation fallback");
                ValidationResult fallbackResult = performClientSideValidation(owner, VALIDATABLE_FIELDS);
                fallbackResult.addMetadata("warning", "Validation service is temporarily unavailable. Basic validation performed. Please retry later for full validation.");
                fallbackResult.addMetadata("fallbackReason", "CIRCUIT_BREAKER_OPEN");
                return fallbackResult;
            }
        }
        
        // Attempt validation with retry mechanism
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                logger.debug("Validation attempt {} of {} for type: {}", attempt, maxRetryAttempts, validationType);
                
                ValidationResult result = operation.execute();
                
                // Success - reset circuit breaker
                if (circuitBreakerState != CircuitBreakerState.CLOSED) {
                    logger.info("Validation service recovered, closing circuit breaker");
                    circuitBreakerState = CircuitBreakerState.CLOSED;
                    failureCount.set(0);
                }
                
                if (attempt > 1) {
                    logger.info("Validation succeeded on attempt {} for type: {}", attempt, validationType);
                    result.addMetadata("retryAttempt", attempt);
                    result.addMetadata("recoveredFromFailure", true);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Validation attempt {} failed for type: {} - {}", attempt, validationType, e.getMessage());
                
                // Don't retry on the last attempt
                if (attempt == maxRetryAttempts) {
                    break;
                }
                
                // Check if error is retryable
                if (!isRetryableError(e)) {
                    logger.warn("Non-retryable error encountered for {} validation, stopping retries", validationType);
                    break;
                }
                
                // Wait before retry
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry delay interrupted for {} validation", validationType);
                    break;
                }
            }
        }
        
        // All attempts failed - update circuit breaker
        recordValidationFailure();
        
        // Fallback to client-side validation
        logger.warn("All validation attempts failed for type: {}, using client-side fallback", validationType);
        ValidationResult fallbackResult = performClientSideValidation(owner, VALIDATABLE_FIELDS);
        fallbackResult.addMetadata("warning", "Validation service is temporarily unavailable. Basic validation performed. Please retry later for full validation.");
        fallbackResult.addMetadata("fallbackReason", "SERVICE_FAILURE");
        fallbackResult.addMetadata("lastError", lastException != null ? lastException.getMessage() : "Unknown error");
        fallbackResult.addMetadata("retryAttempts", maxRetryAttempts);
        
        return fallbackResult;
    }
    
    /**
     * Perform client-side validation as fallback when service is unavailable
     * Implements Requirements 3.2, 3.3
     */
    private ValidationResult performClientSideValidation(Owner owner, Set<String> fieldsToValidate) {
        logger.debug("Performing client-side validation fallback");
        
        ValidationResult result = new ValidationResult(true);
        List<FieldValidationError> errors = new ArrayList<>();
        
        // Basic required field validation
        if (fieldsToValidate.contains("firstName") && isNullOrEmpty(owner.getFirstName())) {
            errors.add(FieldValidationError.requiredFieldError(
                "firstName", "First name is required", 
                "Please enter your first name", "FIRST_NAME_REQUIRED"));
        }
        
        if (fieldsToValidate.contains("lastName") && isNullOrEmpty(owner.getLastName())) {
            errors.add(FieldValidationError.requiredFieldError(
                "lastName", "Last name is required", 
                "Please enter your last name", "LAST_NAME_REQUIRED"));
        }
        
        // Format validation with enhanced error messages
        if (fieldsToValidate.contains("firstName") && owner.getFirstName() != null) {
            if (!NAME_PATTERN.matcher(owner.getFirstName()).matches()) {
                errors.add(FieldValidationError.formatError(
                    "firstName", "Invalid first name format", 
                    "First name should contain only letters, spaces, hyphens, and apostrophes (1-50 characters)",
                    owner.getFirstName(), "Example: John, Mary-Jane, O'Connor", "FIRST_NAME_FORMAT"));
            }
        }
        
        if (fieldsToValidate.contains("lastName") && owner.getLastName() != null) {
            if (!NAME_PATTERN.matcher(owner.getLastName()).matches()) {
                errors.add(FieldValidationError.formatError(
                    "lastName", "Invalid last name format", 
                    "Last name should contain only letters, spaces, hyphens, and apostrophes (1-50 characters)",
                    owner.getLastName(), "Example: Smith, Van Der Berg, O'Neil", "LAST_NAME_FORMAT"));
            }
        }
        
        if (fieldsToValidate.contains("email") && owner.getEmail() != null && !owner.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(owner.getEmail()).matches()) {
                errors.add(FieldValidationError.formatError(
                    "email", "Invalid email format", 
                    "Please enter a valid email address",
                    owner.getEmail(), "Example: user@example.com", "EMAIL_FORMAT"));
            } else if (owner.getEmail().length() > 100) {
                errors.add(FieldValidationError.businessRuleError(
                    "email", "Email address is too long", 
                    "Email address must be 100 characters or less",
                    owner.getEmail(), "EMAIL_TOO_LONG"));
            }
        }
        
        if (fieldsToValidate.contains("telephone") && owner.getTelephone() != null && !owner.getTelephone().trim().isEmpty()) {
            String cleanPhone = owner.getTelephone().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                errors.add(FieldValidationError.formatError(
                    "telephone", "Invalid telephone format", 
                    "Please enter a valid phone number (7-15 digits)",
                    owner.getTelephone(), "Example: +1-555-123-4567, (555) 123-4567", "TELEPHONE_FORMAT"));
            }
        }
        
        if (fieldsToValidate.contains("mobileNumber") && owner.getMobileNumber() != null && !owner.getMobileNumber().trim().isEmpty()) {
            String cleanMobile = owner.getMobileNumber().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanMobile).matches()) {
                errors.add(FieldValidationError.formatError(
                    "mobileNumber", "Invalid mobile number format", 
                    "Please enter a valid mobile number (7-15 digits)",
                    owner.getMobileNumber(), "Example: +1-555-987-6543, (555) 987-6543", "MOBILE_FORMAT"));
            }
        }
        
        if (fieldsToValidate.contains("zipCode") && owner.getZipCode() != null && !owner.getZipCode().trim().isEmpty()) {
            if (!ZIP_PATTERN.matcher(owner.getZipCode()).matches()) {
                errors.add(FieldValidationError.formatError(
                    "zipCode", "Invalid ZIP/postal code format", 
                    "Please enter a valid ZIP or postal code (3-10 characters)",
                    owner.getZipCode(), "Example: 12345, K1A 0A6, SW1A 1AA", "ZIP_FORMAT"));
            }
        }
        
        // Length validation with specific guidance
        if (fieldsToValidate.contains("address") && owner.getAddress() != null && owner.getAddress().length() > 200) {
            errors.add(FieldValidationError.businessRuleError(
                "address", "Address is too long", 
                "Address must be 200 characters or less. Consider abbreviating street types (St, Ave, Blvd)",
                owner.getAddress(), "ADDRESS_TOO_LONG"));
        }
        
        if (fieldsToValidate.contains("city") && owner.getCity() != null && owner.getCity().length() > 100) {
            errors.add(FieldValidationError.businessRuleError(
                "city", "City name is too long", 
                "City name must be 100 characters or less",
                owner.getCity(), "CITY_TOO_LONG"));
        }
        
        if (fieldsToValidate.contains("state") && owner.getState() != null && owner.getState().length() > 50) {
            errors.add(FieldValidationError.businessRuleError(
                "state", "State name is too long", 
                "State name must be 50 characters or less. Consider using abbreviations (CA, NY, TX)",
                owner.getState(), "STATE_TOO_LONG"));
        }
        
        // Set result
        if (!errors.isEmpty()) {
            result.setFieldErrors(errors);
            result.setValid(false);
            result.setOverallMessage("Basic validation failed with " + errors.size() + " error(s). Full validation unavailable.");
        } else {
            result.setValid(true);
            result.setOverallMessage("Basic validation passed. Full validation unavailable - please retry later.");
        }
        
        // Add metadata
        result.addMetadata("validationMode", "CLIENT_SIDE_FALLBACK");
        result.addMetadata("validatedFields", fieldsToValidate);
        result.addMetadata("fullValidationAvailable", false);
        result.addMetadata("validationTimestamp", System.currentTimeMillis());
        
        logger.debug("Client-side validation completed with {} errors", errors.size());
        return result;
    }
    
    /**
     * Check if the circuit breaker should attempt recovery
     */
    private boolean shouldAttemptRecovery() {
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        return timeSinceLastFailure >= circuitBreakerRecoveryTimeout;
    }
    
    /**
     * Record a validation failure and update circuit breaker state
     */
    private void recordValidationFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= circuitBreakerFailureThreshold && circuitBreakerState == CircuitBreakerState.CLOSED) {
            circuitBreakerState = CircuitBreakerState.OPEN;
            logger.warn("Circuit breaker opened after {} failures. Switching to fallback validation.", failures);
        } else if (circuitBreakerState == CircuitBreakerState.HALF_OPEN) {
            circuitBreakerState = CircuitBreakerState.OPEN;
            logger.warn("Circuit breaker returned to OPEN state after failed recovery attempt");
        }
    }
    
    /**
     * Check if an error is retryable
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
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("connection") || 
                   lowerMessage.contains("timeout") || 
                   lowerMessage.contains("temporary") ||
                   lowerMessage.contains("unavailable");
        }
        
        // Default to retrying unknown exceptions
        return true;
    }
    
    /**
     * Functional interface for validation operations
     */
    @FunctionalInterface
    private interface ValidationOperation {
        ValidationResult execute() throws Exception;
    }
    
    /**
     * Check if validation service is currently available
     * Implements Requirement 3.1
     */
    public boolean isValidationServiceAvailable() {
        return circuitBreakerState == CircuitBreakerState.CLOSED;
    }
    
    /**
     * Get current circuit breaker state for monitoring
     */
    public String getCircuitBreakerState() {
        return circuitBreakerState.name();
    }
    
    /**
     * Get current failure count for monitoring
     */
    public int getCurrentFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Performs the actual validation by coordinating different validation types
     */
    private ValidationResult performValidation(Owner owner, Long ownerId, Set<String> fieldsToValidate) {
        ValidationResult result = new ValidationResult(true);
        List<FieldValidationError> allErrors = new ArrayList<>();
        
        try {
            // 1. Perform format validation
            List<FieldValidationError> formatErrors = performFormatValidation(owner, fieldsToValidate);
            allErrors.addAll(formatErrors);
            
            // 2. Perform business rule validation
            List<FieldValidationError> businessRuleErrors = performBusinessRuleValidation(owner, fieldsToValidate);
            allErrors.addAll(businessRuleErrors);
            
            // 3. Perform uniqueness validation (only if format validation passed for relevant fields)
            List<FieldValidationError> uniquenessErrors = performUniquenessValidation(owner, ownerId, fieldsToValidate, formatErrors);
            allErrors.addAll(uniquenessErrors);
            
            // 4. Aggregate all errors
            if (!allErrors.isEmpty()) {
                result.setFieldErrors(allErrors);
                result.setValid(false);
                result.setOverallMessage("Validation failed with " + allErrors.size() + " error(s)");
            } else {
                result.setValid(true);
                result.setOverallMessage("All validation checks passed");
            }
            
            // Add metadata
            result.addMetadata("validatedFields", fieldsToValidate);
            result.addMetadata("isUpdate", ownerId != null);
            result.addMetadata("validationTimestamp", System.currentTimeMillis());
            
            // Add update-specific metadata
            if (ownerId != null) {
                result.addMetadata("ownerId", ownerId);
                result.addMetadata("updateValidationType", "complete_record");
                result.addMetadata("fieldsValidatedCount", fieldsToValidate.size());
                result.addMetadata("totalValidatableFields", VALIDATABLE_FIELDS.size());
                
                // Indicate if this is a partial field validation
                if (fieldsToValidate.size() < VALIDATABLE_FIELDS.size()) {
                    result.addMetadata("updateValidationType", "partial_fields");
                    result.addMetadata("skippedFields", 
                        VALIDATABLE_FIELDS.stream()
                            .filter(field -> !fieldsToValidate.contains(field))
                            .toArray(String[]::new));
                }
            }
            
        } catch (Exception e) {
            // Handle unexpected validation exceptions
            result.setValid(false);
            result.addFieldError("system", "VALIDATION_SYSTEM_ERROR", 
                               "Validation system encountered an error", 
                               "Please try again or contact support", e.getMessage());
            result.setOverallMessage("Validation failed due to system error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Performs format validation for specified fields
     */
    private List<FieldValidationError> performFormatValidation(Owner owner, Set<String> fieldsToValidate) {
        List<FieldValidationError> errors = new ArrayList<>();
        
        // Validate mobile number format
        if (fieldsToValidate.contains("mobileNumber") && owner.getMobileNumber() != null) {
            FormatValidationResult mobileResult = formatValidatorService.validateMobileNumber(owner.getMobileNumber());
            if (!mobileResult.isValid()) {
                errors.add(FieldValidationError.formatError(
                    "mobileNumber",
                    mobileResult.getErrorMessage(),
                    mobileResult.getCorrectionGuidance(),
                    mobileResult.getRejectedValue(),
                    mobileResult.getFormatExample(),
                    mobileResult.getErrorCode()
                ));
            }
        }
        
        // Validate email format
        if (fieldsToValidate.contains("email") && owner.getEmail() != null) {
            FormatValidationResult emailResult = formatValidatorService.validateEmail(owner.getEmail());
            if (!emailResult.isValid()) {
                errors.add(FieldValidationError.formatError(
                    "email",
                    emailResult.getErrorMessage(),
                    emailResult.getCorrectionGuidance(),
                    emailResult.getRejectedValue(),
                    emailResult.getFormatExample(),
                    emailResult.getErrorCode()
                ));
            }
        }
        
        // Validate first name format
        if (fieldsToValidate.contains("firstName") && owner.getFirstName() != null) {
            FormatValidationResult firstNameResult = formatValidatorService.validateName(owner.getFirstName());
            if (!firstNameResult.isValid()) {
                errors.add(FieldValidationError.formatError(
                    "firstName",
                    firstNameResult.getErrorMessage(),
                    firstNameResult.getCorrectionGuidance(),
                    firstNameResult.getRejectedValue(),
                    firstNameResult.getFormatExample(),
                    firstNameResult.getErrorCode()
                ));
            }
        }
        
        // Validate last name format
        if (fieldsToValidate.contains("lastName") && owner.getLastName() != null) {
            FormatValidationResult lastNameResult = formatValidatorService.validateName(owner.getLastName());
            if (!lastNameResult.isValid()) {
                errors.add(FieldValidationError.formatError(
                    "lastName",
                    lastNameResult.getErrorMessage(),
                    lastNameResult.getCorrectionGuidance(),
                    lastNameResult.getRejectedValue(),
                    lastNameResult.getFormatExample(),
                    lastNameResult.getErrorCode()
                ));
            }
        }
        
        // Validate postal code format
        if (fieldsToValidate.contains("zipCode") && owner.getZipCode() != null) {
            FormatValidationResult postalCodeResult = formatValidatorService.validatePostalCode(owner.getZipCode());
            if (!postalCodeResult.isValid()) {
                errors.add(FieldValidationError.formatError(
                    "zipCode",
                    postalCodeResult.getErrorMessage(),
                    postalCodeResult.getCorrectionGuidance(),
                    postalCodeResult.getRejectedValue(),
                    postalCodeResult.getFormatExample(),
                    postalCodeResult.getErrorCode()
                ));
            }
        }
        
        return errors;
    }
    
    /**
     * Performs business rule validation for specified fields
     */
    private List<FieldValidationError> performBusinessRuleValidation(Owner owner, Set<String> fieldsToValidate) {
        List<FieldValidationError> errors = new ArrayList<>();
        
        // Validate required fields
        if (fieldsToValidate.contains("firstName") && isNullOrEmpty(owner.getFirstName())) {
            errors.add(FieldValidationError.requiredFieldError(
                "firstName",
                "First name is required",
                "Please provide a valid first name",
                "FIRST_NAME_REQUIRED"
            ));
        }
        
        if (fieldsToValidate.contains("lastName") && isNullOrEmpty(owner.getLastName())) {
            errors.add(FieldValidationError.requiredFieldError(
                "lastName",
                "Last name is required",
                "Please provide a valid last name",
                "LAST_NAME_REQUIRED"
            ));
        }
        
        // Validate field lengths
        if (fieldsToValidate.contains("firstName") && owner.getFirstName() != null && owner.getFirstName().length() > 50) {
            errors.add(FieldValidationError.businessRuleError(
                "firstName",
                "First name must not exceed 50 characters",
                "Please use a shorter first name",
                owner.getFirstName(),
                "FIRST_NAME_TOO_LONG"
            ));
        }
        
        if (fieldsToValidate.contains("lastName") && owner.getLastName() != null && owner.getLastName().length() > 50) {
            errors.add(FieldValidationError.businessRuleError(
                "lastName",
                "Last name must not exceed 50 characters",
                "Please use a shorter last name",
                owner.getLastName(),
                "LAST_NAME_TOO_LONG"
            ));
        }
        
        if (fieldsToValidate.contains("email") && owner.getEmail() != null && owner.getEmail().length() > 100) {
            errors.add(FieldValidationError.businessRuleError(
                "email",
                "Email must not exceed 100 characters",
                "Please use a shorter email address",
                owner.getEmail(),
                "EMAIL_TOO_LONG"
            ));
        }
        
        if (fieldsToValidate.contains("address") && owner.getAddress() != null && owner.getAddress().length() > 200) {
            errors.add(FieldValidationError.businessRuleError(
                "address",
                "Address must not exceed 200 characters",
                "Please use a shorter address",
                owner.getAddress(),
                "ADDRESS_TOO_LONG"
            ));
        }
        
        return errors;
    }
    
    /**
     * Performs uniqueness validation for specified fields
     * Only validates fields that passed format validation
     */
    private List<FieldValidationError> performUniquenessValidation(Owner owner, Long ownerId, 
                                                                  Set<String> fieldsToValidate, 
                                                                  List<FieldValidationError> formatErrors) {
        List<FieldValidationError> errors = new ArrayList<>();
        
        // Check if mobile number had format errors
        boolean mobileNumberHasFormatError = formatErrors.stream()
            .anyMatch(error -> "mobileNumber".equals(error.getFieldName()));
        
        // Validate mobile number uniqueness (only if format validation passed)
        if (fieldsToValidate.contains("mobileNumber") && owner.getMobileNumber() != null && 
            !mobileNumberHasFormatError) {
            UniquenessValidationResult mobileResult = uniquenessValidatorService
                .validateMobileNumberUniqueness(owner.getMobileNumber(), ownerId);
            if (!mobileResult.isUnique()) {
                errors.add(FieldValidationError.uniquenessError(
                    "mobileNumber",
                    mobileResult.getErrorMessage(),
                    mobileResult.getCorrectionGuidance(),
                    mobileResult.getRejectedValue(),
                    mobileResult.getErrorCode(),
                    mobileResult.getConflictingEntityId()
                ));
            }
        }
        
        // Check if email had format errors
        boolean emailHasFormatError = formatErrors.stream()
            .anyMatch(error -> "email".equals(error.getFieldName()));
        
        // Validate email uniqueness (only if format validation passed)
        if (fieldsToValidate.contains("email") && owner.getEmail() != null && 
            !emailHasFormatError) {
            UniquenessValidationResult emailResult = uniquenessValidatorService
                .validateEmailUniqueness(owner.getEmail(), ownerId);
            if (!emailResult.isUnique()) {
                errors.add(FieldValidationError.uniquenessError(
                    "email",
                    emailResult.getErrorMessage(),
                    emailResult.getCorrectionGuidance(),
                    emailResult.getRejectedValue(),
                    emailResult.getErrorCode(),
                    emailResult.getConflictingEntityId()
                ));
            }
        }
        
        return errors;
    }
    
    /**
     * Filters and validates field names, returning only valid field names
     */
    private Set<String> getValidFieldNames(String... fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            return VALIDATABLE_FIELDS;
        }
        
        Set<String> validFields = new HashSet<>();
        for (String fieldName : fieldNames) {
            if (fieldName != null && VALIDATABLE_FIELDS.contains(fieldName)) {
                validFields.add(fieldName);
            }
        }
        
        // If no valid fields were specified, validate all fields
        return validFields.isEmpty() ? VALIDATABLE_FIELDS : validFields;
    }
    
    /**
     * Identifies fields that have non-null values (indicating they may have changed)
     * This optimization helps with partial updates by only validating fields that are present
     */
    private Set<String> getChangedFields(Owner owner) {
        Set<String> changedFields = new HashSet<>();
        
        if (owner.getFirstName() != null) changedFields.add("firstName");
        if (owner.getLastName() != null) changedFields.add("lastName");
        if (owner.getEmail() != null) changedFields.add("email");
        if (owner.getMobileNumber() != null) changedFields.add("mobileNumber");
        if (owner.getAddress() != null) changedFields.add("address");
        if (owner.getCity() != null) changedFields.add("city");
        if (owner.getState() != null) changedFields.add("state");
        if (owner.getZipCode() != null) changedFields.add("zipCode");
        if (owner.getTelephone() != null) changedFields.add("telephone");
        
        // If no fields are present, validate all fields to ensure complete validation
        return changedFields.isEmpty() ? VALIDATABLE_FIELDS : changedFields;
    }
    
    /**
     * Utility method to check if a string is null or empty
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Record validation metrics and audit logs for a validation attempt
     */
    private void recordValidationMetricsAndAudit(String validationType, Owner owner, Long ownerId, 
                                               ValidationResult result, long processingTimeMs) {
        try {
            // Record metrics
            validationMetricsService.recordValidationAttempt(validationType, result, processingTimeMs);
            
            // Record audit log
            validationAuditService.logValidationAttempt(validationType, owner, ownerId, 
                                                      result, processingTimeMs, "REST_API");
            
            // Record performance metrics if processing time is significant
            if (processingTimeMs > 100) { // More than 100ms
                double memoryUsage = getMemoryUsage();
                validationAuditService.logValidationPerformance(validationType, processingTimeMs, 
                                                              memoryUsage, "REST_API");
            }
            
        } catch (Exception e) {
            logger.error("Error recording validation metrics and audit: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get current memory usage in MB for performance monitoring
     */
    private double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return usedMemory / (1024.0 * 1024.0); // Convert to MB
    }
}