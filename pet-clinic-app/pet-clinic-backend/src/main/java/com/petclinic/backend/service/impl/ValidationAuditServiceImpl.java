package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.ValidationAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ValidationAuditService for comprehensive audit logging
 * Provides structured logging with contextual information for all validation operations
 * Uses SLF4J with MDC for structured logging and audit trail
 * Validates: Requirements 4.5
 */
@Service
public class ValidationAuditServiceImpl implements ValidationAuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("VALIDATION_AUDIT");
    private static final Logger performanceLogger = LoggerFactory.getLogger("VALIDATION_PERFORMANCE");
    private static final Logger configLogger = LoggerFactory.getLogger("VALIDATION_CONFIG");
    private static final Logger errorLogger = LoggerFactory.getLogger("VALIDATION_ERROR");
    
    private final ObjectMapper objectMapper;
    
    // Sensitive fields that should be masked in logs
    private static final String[] SENSITIVE_FIELDS = {
        "mobileNumber", "telephone", "email"
    };
    
    @Autowired
    public ValidationAuditServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void logValidationAttempt(String validationType, Owner ownerData, Long ownerId, 
                                   ValidationResult result, long processingTimeMs, String requestSource) {
        try {
            String auditId = generateAuditId();
            
            // Set up MDC context for structured logging
            MDC.put("auditId", auditId);
            MDC.put("validationType", validationType);
            MDC.put("ownerId", ownerId != null ? ownerId.toString() : "NEW");
            MDC.put("requestSource", requestSource);
            MDC.put("processingTime", String.valueOf(processingTimeMs));
            MDC.put("validationOutcome", result.isValid() ? "SUCCESS" : "FAILURE");
            MDC.put("errorCount", String.valueOf(result.getErrorCount()));
            MDC.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Create audit log entry
            Map<String, Object> auditEntry = new HashMap<>();
            auditEntry.put("auditId", auditId);
            auditEntry.put("eventType", "VALIDATION_ATTEMPT");
            auditEntry.put("validationType", validationType);
            auditEntry.put("ownerId", ownerId);
            auditEntry.put("ownerData", maskSensitiveData(ownerData));
            auditEntry.put("validationResult", createValidationResultSummary(result));
            auditEntry.put("processingTimeMs", processingTimeMs);
            auditEntry.put("requestSource", requestSource);
            auditEntry.put("timestamp", LocalDateTime.now());
            
            // Log the audit entry
            auditLogger.info("Validation attempt: {}", toJsonString(auditEntry));
            
            // Log specific outcome
            if (result.isValid()) {
                logValidationSuccess(validationType, ownerData, ownerId, processingTimeMs, requestSource);
            } else {
                logValidationFailure(validationType, ownerData, ownerId, result, processingTimeMs, requestSource);
            }
            
        } catch (Exception e) {
            errorLogger.error("Error logging validation attempt: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public void logValidationSuccess(String validationType, Owner ownerData, Long ownerId, 
                                   long processingTimeMs, String requestSource) {
        try {
            String auditId = generateAuditId();
            
            MDC.put("auditId", auditId);
            MDC.put("validationType", validationType);
            MDC.put("ownerId", ownerId != null ? ownerId.toString() : "NEW");
            MDC.put("requestSource", requestSource);
            MDC.put("processingTime", String.valueOf(processingTimeMs));
            MDC.put("validationOutcome", "SUCCESS");
            
            Map<String, Object> successEntry = new HashMap<>();
            successEntry.put("auditId", auditId);
            successEntry.put("eventType", "VALIDATION_SUCCESS");
            successEntry.put("validationType", validationType);
            successEntry.put("ownerId", ownerId);
            successEntry.put("ownerSummary", createOwnerSummary(ownerData));
            successEntry.put("processingTimeMs", processingTimeMs);
            successEntry.put("requestSource", requestSource);
            successEntry.put("timestamp", LocalDateTime.now());
            
            auditLogger.info("Validation success: {}", toJsonString(successEntry));
            
        } catch (Exception e) {
            errorLogger.error("Error logging validation success: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public void logValidationFailure(String validationType, Owner ownerData, Long ownerId, 
                                   ValidationResult result, long processingTimeMs, String requestSource) {
        try {
            String auditId = generateAuditId();
            
            MDC.put("auditId", auditId);
            MDC.put("validationType", validationType);
            MDC.put("ownerId", ownerId != null ? ownerId.toString() : "NEW");
            MDC.put("requestSource", requestSource);
            MDC.put("processingTime", String.valueOf(processingTimeMs));
            MDC.put("validationOutcome", "FAILURE");
            MDC.put("errorCount", String.valueOf(result.getErrorCount()));
            
            Map<String, Object> failureEntry = new HashMap<>();
            failureEntry.put("auditId", auditId);
            failureEntry.put("eventType", "VALIDATION_FAILURE");
            failureEntry.put("validationType", validationType);
            failureEntry.put("ownerId", ownerId);
            failureEntry.put("ownerSummary", createOwnerSummary(ownerData));
            failureEntry.put("validationErrors", createErrorSummary(result.getFieldErrors()));
            failureEntry.put("errorCount", result.getErrorCount());
            failureEntry.put("processingTimeMs", processingTimeMs);
            failureEntry.put("requestSource", requestSource);
            failureEntry.put("timestamp", LocalDateTime.now());
            
            auditLogger.warn("Validation failure: {}", toJsonString(failureEntry));
            
            // Log individual field errors for detailed analysis
            if (result.getFieldErrors() != null) {
                for (FieldValidationError error : result.getFieldErrors()) {
                    logFieldValidation(error.getFieldName(), error.getRejectedValue(), 
                                     error.getErrorCode(), false, error.getErrorMessage(), requestSource);
                }
            }
            
        } catch (Exception e) {
            errorLogger.error("Error logging validation failure: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public void logFieldValidation(String fieldName, Object fieldValue, String validationType, 
                                 boolean success, String errorMessage, String requestSource) {
        try {
            String auditId = generateAuditId();
            
            MDC.put("auditId", auditId);
            MDC.put("fieldName", fieldName);
            MDC.put("validationType", validationType);
            MDC.put("requestSource", requestSource);
            MDC.put("validationOutcome", success ? "SUCCESS" : "FAILURE");
            
            Map<String, Object> fieldEntry = new HashMap<>();
            fieldEntry.put("auditId", auditId);
            fieldEntry.put("eventType", "FIELD_VALIDATION");
            fieldEntry.put("fieldName", fieldName);
            fieldEntry.put("fieldValue", maskSensitiveValue(fieldName, fieldValue));
            fieldEntry.put("validationType", validationType);
            fieldEntry.put("success", success);
            fieldEntry.put("errorMessage", errorMessage);
            fieldEntry.put("requestSource", requestSource);
            fieldEntry.put("timestamp", LocalDateTime.now());
            
            if (success) {
                auditLogger.debug("Field validation success: {}", toJsonString(fieldEntry));
            } else {
                auditLogger.warn("Field validation failure: {}", toJsonString(fieldEntry));
            }
            
        } catch (Exception e) {
            errorLogger.error("Error logging field validation: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public void logValidationSystemError(String validationType, Owner ownerData, 
                                       Throwable error, String requestSource) {
        try {
            String auditId = generateAuditId();
            
            MDC.put("auditId", auditId);
            MDC.put("validationType", validationType);
            MDC.put("requestSource", requestSource);
            MDC.put("errorType", error.getClass().getSimpleName());
            MDC.put("validationOutcome", "SYSTEM_ERROR");
            
            Map<String, Object> errorEntry = new HashMap<>();
            errorEntry.put("auditId", auditId);
            errorEntry.put("eventType", "VALIDATION_SYSTEM_ERROR");
            errorEntry.put("validationType", validationType);
            errorEntry.put("ownerSummary", createOwnerSummary(ownerData));
            errorEntry.put("errorType", error.getClass().getSimpleName());
            errorEntry.put("errorMessage", error.getMessage());
            errorEntry.put("requestSource", requestSource);
            errorEntry.put("timestamp", LocalDateTime.now());
            
            errorLogger.error("Validation system error: {}", toJsonString(errorEntry), error);
            
        } catch (Exception e) {
            errorLogger.error("Error logging validation system error: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public void logValidationConfigurationChange(String configurationChange, Object oldValue, 
                                               Object newValue, String changedBy) {
        try {
            String auditId = generateAuditId();
            
            MDC.put("auditId", auditId);
            MDC.put("configurationChange", configurationChange);
            MDC.put("changedBy", changedBy);
            
            Map<String, Object> configEntry = new HashMap<>();
            configEntry.put("auditId", auditId);
            configEntry.put("eventType", "VALIDATION_CONFIG_CHANGE");
            configEntry.put("configurationChange", configurationChange);
            configEntry.put("oldValue", oldValue);
            configEntry.put("newValue", newValue);
            configEntry.put("changedBy", changedBy);
            configEntry.put("timestamp", LocalDateTime.now());
            
            configLogger.info("Validation configuration change: {}", toJsonString(configEntry));
            
        } catch (Exception e) {
            errorLogger.error("Error logging validation configuration change: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    @Override
    public void logValidationPerformance(String validationType, long processingTimeMs, 
                                       double memoryUsageMB, String requestSource) {
        try {
            String auditId = generateAuditId();
            
            MDC.put("auditId", auditId);
            MDC.put("validationType", validationType);
            MDC.put("processingTime", String.valueOf(processingTimeMs));
            MDC.put("memoryUsage", String.valueOf(memoryUsageMB));
            MDC.put("requestSource", requestSource);
            
            Map<String, Object> performanceEntry = new HashMap<>();
            performanceEntry.put("auditId", auditId);
            performanceEntry.put("eventType", "VALIDATION_PERFORMANCE");
            performanceEntry.put("validationType", validationType);
            performanceEntry.put("processingTimeMs", processingTimeMs);
            performanceEntry.put("memoryUsageMB", memoryUsageMB);
            performanceEntry.put("requestSource", requestSource);
            performanceEntry.put("timestamp", LocalDateTime.now());
            
            // Log as INFO for normal performance, WARN for slow operations
            if (processingTimeMs > 1000) { // More than 1 second
                performanceLogger.warn("Slow validation performance: {}", toJsonString(performanceEntry));
            } else {
                performanceLogger.debug("Validation performance: {}", toJsonString(performanceEntry));
            }
            
        } catch (Exception e) {
            errorLogger.error("Error logging validation performance: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Generate a unique audit ID for tracking related log entries
     */
    private String generateAuditId() {
        return "VAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Mask sensitive data in owner information for logging
     */
    private Map<String, Object> maskSensitiveData(Owner owner) {
        if (owner == null) {
            return null;
        }
        
        Map<String, Object> maskedData = new HashMap<>();
        maskedData.put("id", owner.getId());
        maskedData.put("firstName", owner.getFirstName());
        maskedData.put("lastName", owner.getLastName());
        maskedData.put("address", owner.getAddress());
        maskedData.put("city", owner.getCity());
        maskedData.put("state", owner.getState());
        maskedData.put("zipCode", owner.getZipCode());
        
        // Mask sensitive fields
        maskedData.put("mobileNumber", maskValue(owner.getMobileNumber()));
        maskedData.put("telephone", maskValue(owner.getTelephone()));
        maskedData.put("email", maskValue(owner.getEmail()));
        
        return maskedData;
    }
    
    /**
     * Create a summary of owner information for logging
     */
    private Map<String, Object> createOwnerSummary(Owner owner) {
        if (owner == null) {
            return null;
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", owner.getId());
        summary.put("fullName", (owner.getFirstName() != null ? owner.getFirstName() : "") + " " + 
                                (owner.getLastName() != null ? owner.getLastName() : ""));
        summary.put("hasEmail", owner.getEmail() != null && !owner.getEmail().isEmpty());
        summary.put("hasMobileNumber", owner.getMobileNumber() != null && !owner.getMobileNumber().isEmpty());
        summary.put("hasAddress", owner.getAddress() != null && !owner.getAddress().isEmpty());
        
        return summary;
    }
    
    /**
     * Create a summary of validation results for logging
     */
    private Map<String, Object> createValidationResultSummary(ValidationResult result) {
        if (result == null) {
            return null;
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("valid", result.isValid());
        summary.put("errorCount", result.getErrorCount());
        summary.put("overallMessage", result.getOverallMessage());
        
        if (result.getFieldErrors() != null && !result.getFieldErrors().isEmpty()) {
            summary.put("errorFields", result.getFieldErrors().stream()
                .map(FieldValidationError::getFieldName)
                .collect(Collectors.toList()));
            summary.put("errorTypes", result.getFieldErrors().stream()
                .map(FieldValidationError::getErrorCode)
                .collect(Collectors.toList()));
        }
        
        return summary;
    }
    
    /**
     * Create a summary of validation errors for logging
     */
    private Map<String, Object> createErrorSummary(java.util.List<FieldValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalErrors", errors.size());
        
        Map<String, Long> errorsByField = errors.stream()
            .collect(Collectors.groupingBy(FieldValidationError::getFieldName, Collectors.counting()));
        summary.put("errorsByField", errorsByField);
        
        Map<String, Long> errorsByType = errors.stream()
            .collect(Collectors.groupingBy(FieldValidationError::getErrorCode, Collectors.counting()));
        summary.put("errorsByType", errorsByType);
        
        return summary;
    }
    
    /**
     * Mask sensitive field values for logging
     */
    private Object maskSensitiveValue(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        
        for (String sensitiveField : SENSITIVE_FIELDS) {
            if (fieldName.toLowerCase().contains(sensitiveField.toLowerCase())) {
                return maskValue(value.toString());
            }
        }
        
        return value;
    }
    
    /**
     * Mask a value for logging (show first and last characters, mask middle)
     */
    private String maskValue(String value) {
        if (value == null || value.length() <= 2) {
            return "***";
        }
        
        if (value.length() <= 4) {
            return value.charAt(0) + "**" + value.charAt(value.length() - 1);
        }
        
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
    
    /**
     * Convert object to JSON string for logging
     */
    private String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return object.toString();
        }
    }
}