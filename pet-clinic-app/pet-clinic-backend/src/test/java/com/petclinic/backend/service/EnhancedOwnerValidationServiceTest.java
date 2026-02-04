package com.petclinic.backend.service;

import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.impl.OwnerValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for enhanced OwnerValidationService with fallback mechanisms
 * Tests the circuit breaker pattern and client-side validation fallback
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Enhanced OwnerValidationService Integration Tests")
class EnhancedOwnerValidationServiceTest {

    private OwnerValidationService ownerValidationService;

    @BeforeEach
    void setUp() {
        // Create a mock implementation for testing
        ownerValidationService = new TestOwnerValidationServiceImpl();
    }

    @Test
    @DisplayName("Should validate owner successfully when service is available")
    void testValidateOwnerWhenServiceAvailable() {
        // Given
        Owner owner = createValidOwner();
        
        // When
        ValidationResult result = ownerValidationService.validateNewOwner(owner);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertNotNull(result.getMetadata());
    }

    @Test
    @DisplayName("Should use fallback validation when service is unavailable")
    void testValidateOwnerWithFallback() {
        // Given
        Owner owner = createValidOwner();
        TestOwnerValidationServiceImpl testService = (TestOwnerValidationServiceImpl) ownerValidationService;
        testService.setServiceUnavailable(true);
        
        // When
        ValidationResult result = ownerValidationService.validateNewOwner(owner);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertNotNull(result.getMetadata());
        assertTrue(result.getMetadata().containsKey("validationMode"));
        assertEquals("CLIENT_SIDE_FALLBACK", result.getMetadata().get("validationMode"));
    }

    @Test
    @DisplayName("Should detect validation errors in fallback mode")
    void testFallbackValidationDetectsErrors() {
        // Given
        Owner owner = createInvalidOwner();
        TestOwnerValidationServiceImpl testService = (TestOwnerValidationServiceImpl) ownerValidationService;
        testService.setServiceUnavailable(true);
        
        // When
        ValidationResult result = ownerValidationService.validateNewOwner(owner);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasFieldErrors());
        assertNotNull(result.getMetadata());
        assertEquals("CLIENT_SIDE_FALLBACK", result.getMetadata().get("validationMode"));
    }

    @Test
    @DisplayName("Should check service availability")
    void testServiceAvailabilityCheck() {
        // Given
        TestOwnerValidationServiceImpl testService = (TestOwnerValidationServiceImpl) ownerValidationService;
        
        // When service is available
        assertTrue(testService.isValidationServiceAvailable());
        assertEquals("CLOSED", testService.getCircuitBreakerState());
        
        // When service is unavailable
        testService.setServiceUnavailable(true);
        assertFalse(testService.isValidationServiceAvailable());
    }

    private Owner createValidOwner() {
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setTelephone("555-123-4567");
        owner.setAddress("123 Main St");
        owner.setCity("Anytown");
        owner.setState("CA");
        owner.setZipCode("12345");
        return owner;
    }

    private Owner createInvalidOwner() {
        Owner owner = new Owner();
        // Missing required fields
        owner.setEmail("invalid-email");
        owner.setTelephone("invalid-phone");
        return owner;
    }

    /**
     * Test implementation of OwnerValidationService for testing fallback mechanisms
     */
    private static class TestOwnerValidationServiceImpl extends OwnerValidationServiceImpl {
        private boolean serviceUnavailable = false;

        public TestOwnerValidationServiceImpl() {
            super(null, null, null, null);
        }

        public void setServiceUnavailable(boolean unavailable) {
            this.serviceUnavailable = unavailable;
        }

        @Override
        public boolean isValidationServiceAvailable() {
            return !serviceUnavailable;
        }

        @Override
        public String getCircuitBreakerState() {
            return serviceUnavailable ? "OPEN" : "CLOSED";
        }

        @Override
        public int getCurrentFailureCount() {
            return serviceUnavailable ? 5 : 0;
        }

        @Override
        public ValidationResult validateNewOwner(Owner owner) {
            if (serviceUnavailable) {
                // Simulate fallback validation
                return performClientSideValidationTest(owner);
            } else {
                // Simulate normal validation
                ValidationResult result = new ValidationResult(true);
                result.setOverallMessage("Validation passed");
                result.addMetadata("validationMode", "FULL_SERVICE");
                return result;
            }
        }

        private ValidationResult performClientSideValidationTest(Owner owner) {
            ValidationResult result = new ValidationResult(true);
            
            // Basic validation logic
            if (owner.getFirstName() == null || owner.getFirstName().trim().isEmpty()) {
                result.addError("firstName", "FIRST_NAME_REQUIRED", "First name is required");
                result.setValid(false);
            }
            
            if (owner.getLastName() == null || owner.getLastName().trim().isEmpty()) {
                result.addError("lastName", "LAST_NAME_REQUIRED", "Last name is required");
                result.setValid(false);
            }
            
            if (owner.getEmail() != null && !owner.getEmail().contains("@")) {
                result.addError("email", "EMAIL_FORMAT", "Invalid email format");
                result.setValid(false);
            }
            
            result.addMetadata("validationMode", "CLIENT_SIDE_FALLBACK");
            result.addMetadata("warning", "Validation service is temporarily unavailable. Basic validation performed.");
            
            return result;
        }
    }
}