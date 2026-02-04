package com.petclinic.backend.service;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.impl.OwnerValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for OwnerValidationService
 * Tests validation orchestration and error aggregation
 * Validates: Requirements 1.2, 1.3, 1.5, 4.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerValidationService Tests")
class OwnerValidationServiceTest {
    
    @Mock
    private FormatValidatorService formatValidatorService;
    
    @Mock
    private UniquenessValidatorService uniquenessValidatorService;
    
    private OwnerValidationService ownerValidationService;
    
    @Mock
    private ValidationMetricsService validationMetricsService;
    
    @Mock
    private ValidationAuditService validationAuditService;
    
    @BeforeEach
    void setUp() {
        ownerValidationService = new OwnerValidationServiceImpl(
            formatValidatorService, uniquenessValidatorService, 
            validationMetricsService, validationAuditService);
    }
    
    @Nested
    @DisplayName("New Owner Validation Tests")
    class NewOwnerValidationTests {
        
        @Test
        @DisplayName("Should return valid result for completely valid owner")
        void shouldReturnValidResultForValidOwner() {
            // Given
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            
            // When
            ValidationResult result = ownerValidationService.validateNewOwner(owner);
            
            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getFieldErrors()).isEmpty();
            assertThat(result.getOverallMessage()).isEqualTo("All validation checks passed");
            assertThat(result.getMetadata()).containsKey("validatedFields");
            assertThat(result.getMetadata()).containsEntry("isUpdate", false);
        }
        
        @Test
        @DisplayName("Should return invalid result for null owner")
        void shouldReturnInvalidResultForNullOwner() {
            // When
            ValidationResult result = ownerValidationService.validateNewOwner(null);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getFieldErrors()).hasSize(1);
            assertThat(result.getFieldErrors().get(0).getFieldName()).isEqualTo("owner");
            assertThat(result.getFieldErrors().get(0).getErrorCode()).isEqualTo("OWNER_NULL");
        }
        
        @Test
        @DisplayName("Should aggregate multiple validation errors")
        void shouldAggregateMultipleValidationErrors() {
            // Given
            Owner owner = createOwnerWithMultipleErrors();
            setupMocksForMultipleErrors();
            
            // When
            ValidationResult result = ownerValidationService.validateNewOwner(owner);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getFieldErrors()).hasSize(4); // Format, business rule, and uniqueness errors
            assertThat(result.getOverallMessage()).contains("4 error(s)");
            
            // Verify all error types are present
            assertThat(result.getFieldErrors()).extracting("fieldName")
                .contains("mobileNumber", "email", "firstName", "lastName");
        }
        
        @Test
        @DisplayName("Should not perform uniqueness validation when format validation fails")
        void shouldSkipUniquenessValidationWhenFormatValidationFails() {
            // Given
            Owner owner = createOwnerWithInvalidMobileFormat();
            when(formatValidatorService.validateMobileNumber(anyString()))
                .thenReturn(FormatValidationResult.invalid("Invalid format", "Fix format", 
                          "invalid", "example", "MOBILE_INVALID"));
            when(formatValidatorService.validateEmail(anyString()))
                .thenReturn(FormatValidationResult.valid());
            when(formatValidatorService.validateName(anyString()))
                .thenReturn(FormatValidationResult.valid());
            
            // When
            ValidationResult result = ownerValidationService.validateNewOwner(owner);
            
            // Then
            assertThat(result.isValid()).isFalse();
            // Verify uniqueness validation was not called for mobile number
            verify(uniquenessValidatorService, never())
                .validateMobileNumberUniqueness(eq(owner.getMobileNumber()), isNull());
            // But should still be called for email since email format is valid
            verify(uniquenessValidatorService)
                .validateEmailUniqueness(eq(owner.getEmail()), isNull());
        }
    }
    
    @Nested
    @DisplayName("Owner Update Validation Tests")
    class OwnerUpdateValidationTests {
        
        @Test
        @DisplayName("Should return valid result for valid owner update")
        void shouldReturnValidResultForValidOwnerUpdate() {
            // Given
            Long ownerId = 1L;
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            
            // When
            ValidationResult result = ownerValidationService.validateOwnerUpdate(ownerId, owner);
            
            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getFieldErrors()).isEmpty();
            assertThat(result.getMetadata()).containsEntry("isUpdate", true);
            
            // Verify uniqueness validation was called with owner ID
            verify(uniquenessValidatorService)
                .validateMobileNumberUniqueness(eq(owner.getMobileNumber()), eq(ownerId));
            verify(uniquenessValidatorService)
                .validateEmailUniqueness(eq(owner.getEmail()), eq(ownerId));
        }
        
        @Test
        @DisplayName("Should return invalid result for null owner ID")
        void shouldReturnInvalidResultForNullOwnerId() {
            // Given
            Owner owner = createValidOwner();
            
            // When
            ValidationResult result = ownerValidationService.validateOwnerUpdate(null, owner);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getFieldErrors()).hasSize(1);
            assertThat(result.getFieldErrors().get(0).getFieldName()).isEqualTo("ownerId");
            assertThat(result.getFieldErrors().get(0).getErrorCode()).isEqualTo("OWNER_ID_INVALID");
        }
        
        @Test
        @DisplayName("Should return invalid result for invalid owner ID")
        void shouldReturnInvalidResultForInvalidOwnerId() {
            // Given
            Owner owner = createValidOwner();
            
            // When - test with zero and negative IDs
            ValidationResult result1 = ownerValidationService.validateOwnerUpdate(0L, owner);
            ValidationResult result2 = ownerValidationService.validateOwnerUpdate(-1L, owner);
            
            // Then
            assertThat(result1.isValid()).isFalse();
            assertThat(result1.getFieldErrors().get(0).getErrorCode()).isEqualTo("OWNER_ID_INVALID");
            
            assertThat(result2.isValid()).isFalse();
            assertThat(result2.getFieldErrors().get(0).getErrorCode()).isEqualTo("OWNER_ID_INVALID");
        }
        
        @Test
        @DisplayName("Should return invalid result for null owner in update")
        void shouldReturnInvalidResultForNullOwnerInUpdate() {
            // When
            ValidationResult result = ownerValidationService.validateOwnerUpdate(1L, null);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getFieldErrors()).hasSize(1);
            assertThat(result.getFieldErrors().get(0).getFieldName()).isEqualTo("owner");
            assertThat(result.getFieldErrors().get(0).getErrorCode()).isEqualTo("OWNER_NULL");
        }
        
        @Test
        @DisplayName("Should validate complete record on update per requirement 4.4")
        void shouldValidateCompleteRecordOnUpdate() {
            // Given
            Long ownerId = 1L;
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            
            // When
            ValidationResult result = ownerValidationService.validateOwnerUpdate(ownerId, owner);
            
            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMetadata()).containsEntry("isUpdate", true);
            assertThat(result.getMetadata()).containsEntry("ownerId", ownerId);
            assertThat(result.getMetadata()).containsEntry("updateValidationType", "complete_record");
            
            // Verify all validation types were called for complete record validation
            verify(formatValidatorService).validateMobileNumber(owner.getMobileNumber());
            verify(formatValidatorService).validateEmail(owner.getEmail());
            verify(formatValidatorService).validateName(owner.getFirstName());
            verify(formatValidatorService).validateName(owner.getLastName());
            verify(uniquenessValidatorService).validateMobileNumberUniqueness(owner.getMobileNumber(), ownerId);
            verify(uniquenessValidatorService).validateEmailUniqueness(owner.getEmail(), ownerId);
        }
    }
    
    @Nested
    @DisplayName("Optimized Update Validation Tests")
    class OptimizedUpdateValidationTests {
        
        @Test
        @DisplayName("Should validate only changed fields when optimized for partial updates")
        void shouldValidateOnlyChangedFieldsWhenOptimized() {
            // Given
            Long ownerId = 1L;
            Owner partialOwner = new Owner();
            partialOwner.setMobileNumber("+1-555-123-4567");
            partialOwner.setEmail("john.doe@example.com");
            // firstName and lastName are null (not changed)
            
            when(formatValidatorService.validateMobileNumber(anyString()))
                .thenReturn(FormatValidationResult.valid());
            when(formatValidatorService.validateEmail(anyString()))
                .thenReturn(FormatValidationResult.valid());
            when(uniquenessValidatorService.validateMobileNumberUniqueness(anyString(), eq(ownerId)))
                .thenReturn(UniquenessValidationResult.unique());
            when(uniquenessValidatorService.validateEmailUniqueness(anyString(), eq(ownerId)))
                .thenReturn(UniquenessValidationResult.unique());
            
            // When - validate only changed fields
            ValidationResult result = ownerValidationService.validateOwnerUpdateOptimized(
                ownerId, partialOwner, true);
            
            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMetadata()).containsEntry("updateValidationType", "partial_fields");
            assertThat(result.getMetadata()).containsKey("skippedFields");
            
            // Verify only mobile number and email validation were called
            verify(formatValidatorService).validateMobileNumber(partialOwner.getMobileNumber());
            verify(formatValidatorService).validateEmail(partialOwner.getEmail());
            verify(formatValidatorService, never()).validateName(anyString());
            
            verify(uniquenessValidatorService).validateMobileNumberUniqueness(partialOwner.getMobileNumber(), ownerId);
            verify(uniquenessValidatorService).validateEmailUniqueness(partialOwner.getEmail(), ownerId);
        }
        
        @Test
        @DisplayName("Should validate specific fields when field names are provided")
        void shouldValidateSpecificFieldsWhenProvided() {
            // Given
            Long ownerId = 1L;
            Owner owner = createValidOwner();
            when(formatValidatorService.validateMobileNumber(anyString()))
                .thenReturn(FormatValidationResult.valid());
            when(uniquenessValidatorService.validateMobileNumberUniqueness(anyString(), eq(ownerId)))
                .thenReturn(UniquenessValidationResult.unique());
            
            // When - validate only mobile number field
            ValidationResult result = ownerValidationService.validateOwnerUpdateOptimized(
                ownerId, owner, false, "mobileNumber");
            
            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMetadata()).containsEntry("updateValidationType", "partial_fields");
            
            // Verify only mobile number validation was called
            verify(formatValidatorService).validateMobileNumber(owner.getMobileNumber());
            verify(formatValidatorService, never()).validateEmail(anyString());
            verify(formatValidatorService, never()).validateName(anyString());
            
            verify(uniquenessValidatorService).validateMobileNumberUniqueness(owner.getMobileNumber(), ownerId);
            verify(uniquenessValidatorService, never()).validateEmailUniqueness(anyString(), any());
        }
        
        @Test
        @DisplayName("Should validate complete record when no optimization flags are set")
        void shouldValidateCompleteRecordWhenNoOptimizationFlags() {
            // Given
            Long ownerId = 1L;
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            
            // When - no optimization flags set
            ValidationResult result = ownerValidationService.validateOwnerUpdateOptimized(
                ownerId, owner, false);
            
            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMetadata()).containsEntry("updateValidationType", "complete_record");
            
            // Verify all validation types were called
            verify(formatValidatorService).validateMobileNumber(owner.getMobileNumber());
            verify(formatValidatorService).validateEmail(owner.getEmail());
            verify(formatValidatorService).validateName(owner.getFirstName());
            verify(formatValidatorService).validateName(owner.getLastName());
        }
        
        @Test
        @DisplayName("Should handle invalid owner ID in optimized validation")
        void shouldHandleInvalidOwnerIdInOptimizedValidation() {
            // Given
            Owner owner = createValidOwner();
            
            // When
            ValidationResult result = ownerValidationService.validateOwnerUpdateOptimized(
                -1L, owner, false);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getFieldErrors()).hasSize(1);
            assertThat(result.getFieldErrors().get(0).getFieldName()).isEqualTo("ownerId");
            assertThat(result.getFieldErrors().get(0).getErrorCode()).isEqualTo("OWNER_ID_INVALID");
        }
    }
    
    @Nested
    @DisplayName("Field-Specific Validation Tests")
    class FieldSpecificValidationTests {
        
        @Test
        @DisplayName("Should validate only specified fields")
        void shouldValidateOnlySpecifiedFields() {
            // Given
            Owner owner = createOwnerWithMultipleErrors();
            when(formatValidatorService.validateMobileNumber(anyString()))
                .thenReturn(FormatValidationResult.invalid("Invalid mobile", "Fix it", 
                          "invalid", "example", "MOBILE_INVALID"));
            
            // When - validate only mobile number field
            ValidationResult result = ownerValidationService.validateOwnerFields(owner, "mobileNumber");
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getFieldErrors()).hasSize(1);
            assertThat(result.getFieldErrors().get(0).getFieldName()).isEqualTo("mobileNumber");
            
            // Verify other field validations were not called
            verify(formatValidatorService, never()).validateEmail(anyString());
            verify(formatValidatorService, never()).validateName(anyString());
        }
        
        @Test
        @DisplayName("Should validate all fields when no specific fields provided")
        void shouldValidateAllFieldsWhenNoSpecificFieldsProvided() {
            // Given
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            
            // When - no specific fields provided
            ValidationResult result = ownerValidationService.validateOwnerFields(owner);
            
            // Then
            assertThat(result.isValid()).isTrue();
            
            // Verify all relevant validations were called
            verify(formatValidatorService).validateMobileNumber(owner.getMobileNumber());
            verify(formatValidatorService).validateEmail(owner.getEmail());
            verify(formatValidatorService).validateName(owner.getFirstName());
            verify(formatValidatorService).validateName(owner.getLastName());
        }
        
        @Test
        @DisplayName("Should ignore invalid field names")
        void shouldIgnoreInvalidFieldNames() {
            // Given
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            
            // When - provide mix of valid and invalid field names
            ValidationResult result = ownerValidationService.validateOwnerFields(
                owner, "mobileNumber", "invalidField", "email", "anotherInvalidField");
            
            // Then
            assertThat(result.isValid()).isTrue();
            
            // Verify only valid fields were validated
            verify(formatValidatorService).validateMobileNumber(owner.getMobileNumber());
            verify(formatValidatorService).validateEmail(owner.getEmail());
            verify(formatValidatorService, never()).validateName(anyString());
        }
    }
    
    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("Should handle format validation service exceptions gracefully")
        void shouldHandleFormatValidationServiceExceptions() {
            // Given
            Owner owner = createValidOwner();
            when(formatValidatorService.validateMobileNumber(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));
            
            // When
            ValidationResult result = ownerValidationService.validateNewOwner(owner);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getOverallMessage()).contains("Validation failed due to system error");
            assertThat(result.getFieldErrors()).hasSize(1);
            assertThat(result.getFieldErrors().get(0).getFieldName()).isEqualTo("system");
            assertThat(result.getFieldErrors().get(0).getErrorCode()).isEqualTo("VALIDATION_SYSTEM_ERROR");
        }
        
        @Test
        @DisplayName("Should handle uniqueness validation service exceptions gracefully")
        void shouldHandleUniquenessValidationServiceExceptions() {
            // Given
            Owner owner = createValidOwner();
            setupMocksForValidOwner();
            when(uniquenessValidatorService.validateMobileNumberUniqueness(anyString(), isNull()))
                .thenThrow(new RuntimeException("Database connection failed"));
            
            // When
            ValidationResult result = ownerValidationService.validateNewOwner(owner);
            
            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getOverallMessage()).contains("Validation failed due to system error");
        }
    }
    
    // Helper methods
    private Owner createValidOwner() {
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber("+1-555-123-4567");
        owner.setAddress("123 Main St");
        return owner;
    }
    
    private Owner createOwnerWithMultipleErrors() {
        Owner owner = new Owner();
        owner.setFirstName(""); // Required field error
        owner.setLastName(""); // Required field error
        owner.setEmail("invalid-email"); // Format error
        owner.setMobileNumber("invalid-mobile"); // Format error
        return owner;
    }
    
    private Owner createOwnerWithInvalidMobileFormat() {
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber("invalid-mobile");
        return owner;
    }
    
    private void setupMocksForValidOwner() {
        lenient().when(formatValidatorService.validateMobileNumber(anyString()))
            .thenReturn(FormatValidationResult.valid());
        lenient().when(formatValidatorService.validateEmail(anyString()))
            .thenReturn(FormatValidationResult.valid());
        lenient().when(formatValidatorService.validateName(anyString()))
            .thenReturn(FormatValidationResult.valid());
        lenient().when(uniquenessValidatorService.validateMobileNumberUniqueness(anyString(), any()))
            .thenReturn(UniquenessValidationResult.unique());
        lenient().when(uniquenessValidatorService.validateEmailUniqueness(anyString(), any()))
            .thenReturn(UniquenessValidationResult.unique());
    }
    
    private void setupMocksForMultipleErrors() {
        // Format validation errors
        lenient().when(formatValidatorService.validateMobileNumber(anyString()))
            .thenReturn(FormatValidationResult.invalid("Invalid mobile format", "Fix format", 
                      "invalid-mobile", "+1-555-123-4567", "MOBILE_INVALID"));
        lenient().when(formatValidatorService.validateEmail(anyString()))
            .thenReturn(FormatValidationResult.invalid("Invalid email format", "Fix format", 
                      "invalid-email", "user@example.com", "EMAIL_INVALID"));
        lenient().when(formatValidatorService.validateName(anyString()))
            .thenReturn(FormatValidationResult.valid());
        
        // Uniqueness validation (won't be called due to format errors)
        lenient().when(uniquenessValidatorService.validateMobileNumberUniqueness(anyString(), any()))
            .thenReturn(UniquenessValidationResult.unique());
        lenient().when(uniquenessValidatorService.validateEmailUniqueness(anyString(), any()))
            .thenReturn(UniquenessValidationResult.unique());
    }
}