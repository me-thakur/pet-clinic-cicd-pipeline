package com.petclinic.backend.model;

import com.petclinic.backend.validation.ValidMobileNumber;
import com.petclinic.backend.validation.UniqueMobileNumber;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Owner entity validation annotations
 * Validates: Requirements 1.1, 2.1, 4.1
 */
@SpringBootTest
@ActiveProfiles("test")
class OwnerValidationTest {

    @Autowired
    private Validator validator;

    @Test
    void testOwnerWithValidMobileNumber_ShouldPassValidation() {
        // Arrange
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber("+12025551234"); // Valid US mobile number in E.164 format

        // Act
        Set<ConstraintViolation<Owner>> violations = validator.validate(owner);

        // Assert - Print violations for debugging
        if (!violations.isEmpty()) {
            System.out.println("Validation violations:");
            for (ConstraintViolation<Owner> violation : violations) {
                System.out.println("Field: " + violation.getPropertyPath() + 
                                 ", Message: " + violation.getMessage() + 
                                 ", Value: " + violation.getInvalidValue());
            }
        }
        
        assertTrue(violations.isEmpty(), "Owner with valid mobile number should pass validation");
    }

    @Test
    void testOwnerWithInvalidMobileNumber_ShouldFailValidation() {
        // Arrange
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber("invalid-mobile"); // Invalid format

        // Act
        Set<ConstraintViolation<Owner>> violations = validator.validate(owner);

        // Assert
        assertFalse(violations.isEmpty(), "Owner with invalid mobile number should fail validation");
        
        boolean hasMobileNumberViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("mobileNumber"));
        assertTrue(hasMobileNumberViolation, "Should have mobile number validation violation");
    }

    @Test
    void testOwnerWithNullMobileNumber_ShouldPassValidation() {
        // Arrange
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber(null); // Null should be allowed

        // Act
        Set<ConstraintViolation<Owner>> violations = validator.validate(owner);

        // Assert
        assertTrue(violations.isEmpty(), "Owner with null mobile number should pass validation");
    }

    @Test
    void testOwnerWithEmptyMobileNumber_ShouldPassValidation() {
        // Arrange
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john.doe@example.com");
        owner.setMobileNumber(""); // Empty string should be allowed

        // Act
        Set<ConstraintViolation<Owner>> violations = validator.validate(owner);

        // Assert
        assertTrue(violations.isEmpty(), "Owner with empty mobile number should pass validation");
    }

    @Test
    void testOwnerValidationAnnotationsPresent() {
        // Arrange & Act
        try {
            var mobileNumberField = Owner.class.getDeclaredField("mobileNumber");
            
            // Assert
            assertTrue(mobileNumberField.isAnnotationPresent(ValidMobileNumber.class), 
                    "Mobile number field should have @ValidMobileNumber annotation");
            assertTrue(mobileNumberField.isAnnotationPresent(UniqueMobileNumber.class), 
                    "Mobile number field should have @UniqueMobileNumber annotation");
                    
        } catch (NoSuchFieldException e) {
            fail("Mobile number field should exist in Owner entity");
        }
    }
}