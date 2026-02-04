package com.petclinic.backend.service;

import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.impl.ValidationAuditServiceImpl;
import com.petclinic.backend.service.impl.ValidationMetricsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validation logging and monitoring functionality
 * Verifies that audit logging and metrics collection work correctly
 * Tests: Task 9.3 - Add validation logging and monitoring
 */
@ExtendWith(MockitoExtension.class)
class ValidationLoggingAndMonitoringTest {
    
    private ValidationMetricsService validationMetricsService;
    private ValidationAuditService validationAuditService;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private ListAppender<ILoggingEvent> listAppender;
    
    @BeforeEach
    void setUp() {
        // Set up meter registry for metrics testing
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        
        // Set up services
        validationMetricsService = new ValidationMetricsServiceImpl(meterRegistry);
        validationAuditService = new ValidationAuditServiceImpl(objectMapper);
        
        // Set up log capture for audit logging tests
        Logger auditLogger = (Logger) LoggerFactory.getLogger("VALIDATION_AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogger.addAppender(listAppender);
    }
    
    @Test
    void testValidationMetricsRecording() {
        // Given
        String validationType = "new_owner";
        ValidationResult successResult = ValidationResult.valid();
        ValidationResult failureResult = ValidationResult.invalid("Test failure");
        failureResult.addFieldError("testField", "TEST_ERROR", "Test error message", 
                                   "Test guidance", "testValue");
        
        // When - Record successful validation
        validationMetricsService.recordValidationAttempt(validationType, successResult, 150L);
        
        // When - Record failed validation
        validationMetricsService.recordValidationAttempt(validationType, failureResult, 200L);
        
        // Then - Verify metrics are recorded
        assertEquals(2, validationMetricsService.getTotalValidationAttempts(validationType));
        assertEquals(50.0, validationMetricsService.getValidationSuccessRate(validationType), 0.1);
        assertEquals(175.0, validationMetricsService.getAverageProcessingTime(validationType), 0.1);
        
        // Verify meter registry has the expected counters
        assertNotNull(meterRegistry.find("validation.attempts.total").counter());
        assertNotNull(meterRegistry.find("validation.success.total").counter());
        assertNotNull(meterRegistry.find("validation.failure.total").counter());
        assertNotNull(meterRegistry.find("validation.processing.time").timer());
    }
    
    @Test
    void testFieldValidationMetrics() {
        // Given
        String fieldName = "mobileNumber";
        String validationType = "format_validation";
        
        // When - Record field validations
        validationMetricsService.recordFieldValidation(fieldName, validationType, true);
        validationMetricsService.recordFieldValidation(fieldName, validationType, false);
        validationMetricsService.recordValidationError("MOBILE_NUMBER_FORMAT_INVALID", fieldName);
        
        // Then - Verify field validation metrics are recorded
        assertNotNull(meterRegistry.find("validation.field.total").counter());
        assertNotNull(meterRegistry.find("validation.error.total").counter());
    }
    
    @Test
    void testValidationAuditLogging() {
        // Given
        Owner testOwner = createTestOwner();
        ValidationResult result = ValidationResult.valid();
        String validationType = "new_owner";
        Long ownerId = null;
        long processingTime = 125L;
        String requestSource = "REST_API";
        
        // When
        validationAuditService.logValidationAttempt(validationType, testOwner, ownerId, 
                                                   result, processingTime, requestSource);
        
        // Then - Verify audit log was created
        assertEquals(2, listAppender.list.size()); // One for attempt, one for success
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("Validation attempt"));
        assertTrue(logEvent.getFormattedMessage().contains("auditId"));
        assertTrue(logEvent.getFormattedMessage().contains("new_owner"));
    }
    
    @Test
    void testValidationFailureAuditLogging() {
        // Given
        Owner testOwner = createTestOwner();
        ValidationResult failureResult = ValidationResult.invalid("Validation failed");
        failureResult.addFieldError("mobileNumber", "MOBILE_NUMBER_FORMAT_INVALID", 
                                   "Invalid mobile number format", 
                                   "Please provide a valid international mobile number", 
                                   "invalid-number");
        
        String validationType = "new_owner";
        Long ownerId = null;
        long processingTime = 200L;
        String requestSource = "REST_API";
        
        // When
        validationAuditService.logValidationFailure(validationType, testOwner, ownerId, 
                                                   failureResult, processingTime, requestSource);
        
        // Then - Verify failure audit log was created
        assertTrue(listAppender.list.size() >= 1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("Validation failure"));
        assertTrue(logEvent.getFormattedMessage().contains("VALIDATION_FAILURE"));
        assertTrue(logEvent.getFormattedMessage().contains("mobileNumber"));
    }
    
    @Test
    void testFieldValidationAuditLogging() {
        // Given
        String fieldName = "email";
        String fieldValue = "invalid-email";
        String validationType = "format_validation";
        boolean success = false;
        String errorMessage = "Invalid email format";
        String requestSource = "REST_API";
        
        // When
        validationAuditService.logFieldValidation(fieldName, fieldValue, validationType, 
                                                 success, errorMessage, requestSource);
        
        // Then - Verify field validation audit log was created
        assertTrue(listAppender.list.size() >= 1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("Field validation failure"));
        assertTrue(logEvent.getFormattedMessage().contains("FIELD_VALIDATION"));
        assertTrue(logEvent.getFormattedMessage().contains("email"));
    }
    
    @Test
    void testValidationSystemErrorLogging() {
        // Given
        Owner testOwner = createTestOwner();
        String validationType = "new_owner";
        Exception testError = new RuntimeException("Test system error");
        String requestSource = "REST_API";
        
        // When
        validationAuditService.logValidationSystemError(validationType, testOwner, testError, requestSource);
        
        // Then - Verify system error audit log was created
        assertTrue(listAppender.list.size() >= 1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("Validation system error"));
        assertTrue(logEvent.getFormattedMessage().contains("VALIDATION_SYSTEM_ERROR"));
        assertTrue(logEvent.getFormattedMessage().contains("RuntimeException"));
    }
    
    @Test
    void testValidationPerformanceLogging() {
        // Given
        String validationType = "new_owner";
        long processingTime = 1500L; // Slow operation
        double memoryUsage = 128.5;
        String requestSource = "REST_API";
        
        // When
        validationAuditService.logValidationPerformance(validationType, processingTime, 
                                                       memoryUsage, requestSource);
        
        // Then - Verify performance log was created
        assertTrue(listAppender.list.size() >= 1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("validation performance") || 
                  logEvent.getMessage().contains("Slow validation performance"));
        assertTrue(logEvent.getFormattedMessage().contains("VALIDATION_PERFORMANCE"));
        assertTrue(logEvent.getFormattedMessage().contains("1500"));
    }
    
    @Test
    void testValidationConfigurationChangeLogging() {
        // Given
        String configChange = "mobile-number.enforce-uniqueness";
        Object oldValue = false;
        Object newValue = true;
        String changedBy = "admin";
        
        // When
        validationAuditService.logValidationConfigurationChange(configChange, oldValue, newValue, changedBy);
        
        // Then - Verify configuration change log was created
        assertTrue(listAppender.list.size() >= 1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertTrue(logEvent.getMessage().contains("Validation configuration change"));
        assertTrue(logEvent.getFormattedMessage().contains("VALIDATION_CONFIG_CHANGE"));
        assertTrue(logEvent.getFormattedMessage().contains("mobile-number.enforce-uniqueness"));
    }
    
    @Test
    void testSensitiveDataMasking() {
        // Given
        Owner testOwner = createTestOwnerWithSensitiveData();
        ValidationResult result = ValidationResult.valid();
        String validationType = "new_owner";
        
        // When
        validationAuditService.logValidationAttempt(validationType, testOwner, null, 
                                                   result, 100L, "REST_API");
        
        // Then - Verify sensitive data is masked in logs
        assertTrue(listAppender.list.size() >= 1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        String logMessage = logEvent.getFormattedMessage();
        
        // Should not contain full sensitive values
        assertFalse(logMessage.contains("+1-555-123-4567"));
        assertFalse(logMessage.contains("john.doe@example.com"));
        
        // Should contain masked versions
        assertTrue(logMessage.contains("***") || logMessage.contains("**"));
    }
    
    @Test
    void testMetricsSuccessRateCalculation() {
        // Given
        String validationType = "owner_update";
        
        // When - Record multiple validation attempts
        for (int i = 0; i < 10; i++) {
            ValidationResult result = (i < 7) ? ValidationResult.valid() : ValidationResult.invalid("Test failure");
            validationMetricsService.recordValidationAttempt(validationType, result, 100L);
        }
        
        // Then - Verify success rate calculation
        assertEquals(10, validationMetricsService.getTotalValidationAttempts(validationType));
        assertEquals(70.0, validationMetricsService.getValidationSuccessRate(validationType), 0.1);
    }
    
    private Owner createTestOwner() {
        Owner owner = new Owner();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("j***e@example.com"); // Pre-masked for testing
        owner.setMobileNumber("+1***4567"); // Pre-masked for testing
        owner.setAddress("123 Main St");
        owner.setCity("Anytown");
        owner.setState("CA");
        owner.setZipCode("12345");
        return owner;
    }
    
    private Owner createTestOwnerWithSensitiveData() {
        Owner owner = new Owner();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com"); // Full sensitive data
        owner.setMobileNumber("+1-555-123-4567"); // Full sensitive data
        owner.setAddress("123 Main St");
        owner.setCity("Anytown");
        owner.setState("CA");
        owner.setZipCode("12345");
        return owner;
    }
}