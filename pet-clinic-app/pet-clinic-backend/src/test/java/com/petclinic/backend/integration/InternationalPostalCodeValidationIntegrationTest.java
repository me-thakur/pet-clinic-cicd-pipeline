package com.petclinic.backend.integration;

import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.OwnerValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for international postal code validation
 * Tests the complete validation flow from Owner model through validation services
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("International Postal Code Validation Integration Tests")
class InternationalPostalCodeValidationIntegrationTest {
    
    @Autowired
    private OwnerValidationService ownerValidationService;
    
    @Test
    @DisplayName("Should accept various international postal code formats")
    void shouldAcceptInternationalPostalCodeFormats() {
        // Test US postal codes
        Owner usOwner = createOwnerWithPostalCode("12345");
        ValidationResult usResult = ownerValidationService.validateNewOwner(usOwner);
        assertTrue(usResult.isValid(), "US postal code should be valid");
        
        Owner usExtendedOwner = createOwnerWithPostalCode("12345-6789");
        ValidationResult usExtendedResult = ownerValidationService.validateNewOwner(usExtendedOwner);
        assertTrue(usExtendedResult.isValid(), "US extended postal code should be valid");
        
        // Test Canadian postal codes
        Owner canadianOwner = createOwnerWithPostalCode("K1A 0A6");
        ValidationResult canadianResult = ownerValidationService.validateNewOwner(canadianOwner);
        assertTrue(canadianResult.isValid(), "Canadian postal code should be valid");
        
        // Test UK postal codes
        Owner ukOwner = createOwnerWithPostalCode("SW1A 1AA");
        ValidationResult ukResult = ownerValidationService.validateNewOwner(ukOwner);
        assertTrue(ukResult.isValid(), "UK postal code should be valid");
        
        // Test German postal codes
        Owner germanOwner = createOwnerWithPostalCode("12345");
        ValidationResult germanResult = ownerValidationService.validateNewOwner(germanOwner);
        assertTrue(germanResult.isValid(), "German postal code should be valid");
        
        // Test Indian postal codes
        Owner indianOwner = createOwnerWithPostalCode("110001");
        ValidationResult indianResult = ownerValidationService.validateNewOwner(indianOwner);
        assertTrue(indianResult.isValid(), "Indian postal code should be valid");
        
        // Test Australian postal codes
        Owner australianOwner = createOwnerWithPostalCode("2000");
        ValidationResult australianResult = ownerValidationService.validateNewOwner(australianOwner);
        assertTrue(australianResult.isValid(), "Australian postal code should be valid");
        
        // Test Japanese postal codes
        Owner japaneseOwner = createOwnerWithPostalCode("100-0001");
        ValidationResult japaneseResult = ownerValidationService.validateNewOwner(japaneseOwner);
        assertTrue(japaneseResult.isValid(), "Japanese postal code should be valid");
    }
    
    @Test
    @DisplayName("Should accept empty postal codes as optional")
    void shouldAcceptEmptyPostalCodes() {
        Owner ownerWithoutPostalCode = createOwnerWithPostalCode(null);
        ValidationResult result = ownerValidationService.validateNewOwner(ownerWithoutPostalCode);
        assertTrue(result.isValid(), "Owner without postal code should be valid");
        
        Owner ownerWithEmptyPostalCode = createOwnerWithPostalCode("");
        ValidationResult emptyResult = ownerValidationService.validateNewOwner(ownerWithEmptyPostalCode);
        assertTrue(emptyResult.isValid(), "Owner with empty postal code should be valid");
    }
    
    @Test
    @DisplayName("Should reject invalid postal code formats with helpful error messages")
    void shouldRejectInvalidPostalCodeFormats() {
        // Test postal code with invalid characters
        Owner ownerWithInvalidChars = createOwnerWithPostalCode("12@45");
        ValidationResult invalidCharsResult = ownerValidationService.validateNewOwner(ownerWithInvalidChars);
        assertFalse(invalidCharsResult.isValid(), "Postal code with invalid characters should be rejected");
        assertTrue(invalidCharsResult.hasErrorForField("zipCode"), "Should have error for zipCode field");
        
        // Test postal code that's too short
        Owner ownerWithShortCode = createOwnerWithPostalCode("12");
        ValidationResult shortCodeResult = ownerValidationService.validateNewOwner(ownerWithShortCode);
        assertFalse(shortCodeResult.isValid(), "Too short postal code should be rejected");
        assertTrue(shortCodeResult.hasErrorForField("zipCode"), "Should have error for zipCode field");
        
        // Test postal code that's too long
        Owner ownerWithLongCode = createOwnerWithPostalCode("12345678901");
        ValidationResult longCodeResult = ownerValidationService.validateNewOwner(ownerWithLongCode);
        assertFalse(longCodeResult.isValid(), "Too long postal code should be rejected");
        assertTrue(longCodeResult.hasErrorForField("zipCode"), "Should have error for zipCode field");
        
        // Verify error messages contain helpful guidance
        var zipCodeErrors = invalidCharsResult.getErrorsForField("zipCode");
        assertFalse(zipCodeErrors.isEmpty(), "Should have zipCode errors");
        assertNotNull(zipCodeErrors.get(0).getCorrectionGuidance(), "Should have correction guidance");
        assertNotNull(zipCodeErrors.get(0).getFormatExample(), "Should have format examples");
    }
    
    @Test
    @DisplayName("Should validate postal codes in field-specific validation")
    void shouldValidatePostalCodesInFieldSpecificValidation() {
        Owner owner = createOwnerWithPostalCode("K1A 0A6"); // Valid Canadian postal code
        
        // Test field-specific validation for zipCode only
        ValidationResult result = ownerValidationService.validateOwnerFields(owner, "zipCode");
        assertTrue(result.isValid(), "Valid postal code should pass field-specific validation");
        
        // Test with invalid postal code
        Owner invalidOwner = createOwnerWithPostalCode("12@45");
        ValidationResult invalidResult = ownerValidationService.validateOwnerFields(invalidOwner, "zipCode");
        assertFalse(invalidResult.isValid(), "Invalid postal code should fail field-specific validation");
        assertTrue(invalidResult.hasErrorForField("zipCode"), "Should have error for zipCode field");
    }
    
    @Test
    @DisplayName("Should maintain backward compatibility with existing US postal codes")
    void shouldMaintainBackwardCompatibility() {
        // These were valid under the old restrictive validation and should still be valid
        String[] validUSCodes = {"12345", "90210", "10001", "12345-6789", "90210-1234"};
        
        for (String postalCode : validUSCodes) {
            Owner owner = createOwnerWithPostalCode(postalCode);
            ValidationResult result = ownerValidationService.validateNewOwner(owner);
            assertTrue(result.isValid(), 
                      String.format("US postal code '%s' should remain valid for backward compatibility", postalCode));
        }
    }
    
    private Owner createOwnerWithPostalCode(String postalCode) {
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber("+1-201-555-0123"); // Valid US mobile number
        owner.setAddress("123 Main St");
        owner.setCity("Anytown");
        owner.setState("CA");
        owner.setZipCode(postalCode);
        return owner;
    }
}