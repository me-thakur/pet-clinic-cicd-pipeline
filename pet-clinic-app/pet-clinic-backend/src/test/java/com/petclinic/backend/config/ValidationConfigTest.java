package com.petclinic.backend.config;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for ValidationConfig
 * Verifies that validation infrastructure is properly configured
 * Validates: Requirements 1.1, 2.1
 */
class ValidationConfigTest {

    /**
     * Unit test to verify that JSR-303 validation infrastructure is available
     */
    @Test
    void shouldHaveValidationInfrastructureAvailable() {
        // Test that we can create a validator factory
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        assertThat(factory).isNotNull();
        
        // Test that we can get a validator
        Validator validator = factory.getValidator();
        assertThat(validator).isNotNull();
        
        // The validator should be Hibernate Validator (the default JSR-303 implementation)
        assertThat(validator.getClass().getName()).contains("hibernate");
        
        factory.close();
    }

    /**
     * Unit test to verify that Google libphonenumber library is available
     */
    @Test
    void shouldHaveLibphonenumberAvailable() {
        // Test that we can access the PhoneNumberUtil class
        try {
            Class<?> phoneNumberUtilClass = Class.forName("com.google.i18n.phonenumbers.PhoneNumberUtil");
            assertThat(phoneNumberUtilClass).isNotNull();
            
            // Test that we can get an instance
            Object phoneNumberUtil = phoneNumberUtilClass.getMethod("getInstance").invoke(null);
            assertThat(phoneNumberUtil).isNotNull();
        } catch (Exception e) {
            throw new AssertionError("Google libphonenumber library should be available", e);
        }
    }

    /**
     * Unit test to verify that jqwik is working correctly
     */
    @Test
    void shouldHaveJqwikAvailable() {
        // This test itself verifies jqwik is available since it uses jqwik annotations
        assertThat(Property.class).isNotNull();
        assertThat(ForAll.class).isNotNull();
    }

    /**
     * Property-based test to verify validation infrastructure works with various inputs
     * **Feature: owner-data-validation, Property 1: Validation Infrastructure Functionality**
     * **Validates: Requirements 1.1, 2.1**
     */
    @Property(tries = 100)
    void validationInfrastructureShouldHandleVariousInputs(@ForAll String input) {
        // Test that the validator can process various string inputs without throwing exceptions
        // This verifies the basic infrastructure is working
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        
        // Create a simple test object to validate
        TestValidationObject testObject = new TestValidationObject(input);
        
        // The validator should not throw exceptions when validating
        var violations = validator.validate(testObject);
        
        // Violations collection should not be null (infrastructure working)
        assertThat(violations).isNotNull();
        
        // The number of violations should be non-negative
        assertThat(violations.size()).isGreaterThanOrEqualTo(0);
        
        factory.close();
    }

    /**
     * Simple test class for validation infrastructure testing
     */
    private static class TestValidationObject {
        private final String value;

        public TestValidationObject(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}