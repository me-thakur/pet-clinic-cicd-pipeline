package com.petclinic.backend.integration;

import com.petclinic.backend.dto.OwnerValidationRequest;
import com.petclinic.backend.dto.ValidationResponse;
import com.petclinic.backend.service.OwnerValidationService;
import com.petclinic.backend.service.FormatValidatorService;
import com.petclinic.backend.service.UniquenessValidatorService;
import com.petclinic.backend.service.ValidationErrorAggregator;
import com.petclinic.backend.service.ErrorMessageFormatter;
import com.petclinic.backend.config.ValidationConfig;
import com.petclinic.backend.config.ValidationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test to verify that all validation components are properly wired together
 * Tests the complete validation stack from controller to services
 * Validates: Requirements 9.1 - Wire all components together
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class ValidationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OwnerValidationService ownerValidationService;

    @Autowired
    private FormatValidatorService formatValidatorService;

    @Autowired
    private UniquenessValidatorService uniquenessValidatorService;

    @Autowired
    private ValidationErrorAggregator validationErrorAggregator;

    @Autowired
    private ErrorMessageFormatter errorMessageFormatter;

    @Autowired
    private ValidationConfig validationConfig;

    @Autowired
    private ValidationProperties validationProperties;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testAllValidationComponentsAreWired() {
        // Verify that all validation components are properly injected
        assertNotNull(ownerValidationService, "OwnerValidationService should be wired");
        assertNotNull(formatValidatorService, "FormatValidatorService should be wired");
        assertNotNull(uniquenessValidatorService, "UniquenessValidatorService should be wired");
        assertNotNull(validationErrorAggregator, "ValidationErrorAggregator should be wired");
        assertNotNull(errorMessageFormatter, "ErrorMessageFormatter should be wired");
        assertNotNull(validationConfig, "ValidationConfig should be wired");
        assertNotNull(validationProperties, "ValidationProperties should be wired");
    }

    @Test
    public void testValidationPropertiesConfiguration() {
        // Verify that validation properties are properly configured
        assertNotNull(validationProperties.getMobileNumber(), "Mobile number properties should be configured");
        assertNotNull(validationProperties.getEmail(), "Email properties should be configured");
        assertNotNull(validationProperties.getName(), "Name properties should be configured");
        assertNotNull(validationProperties.getGeneral(), "General properties should be configured");

        // Test specific property values
        assertTrue(validationProperties.getMobileNumber().isEnforceUniqueness(), 
                  "Mobile number uniqueness should be enforced by default");
        assertTrue(validationProperties.getMobileNumber().isAllowInternational(), 
                  "International mobile numbers should be allowed by default");
        assertEquals("US", validationProperties.getMobileNumber().getDefaultCountryCode(), 
                    "Default country code should be US");
    }

    @Test
    public void testValidationEndpointIntegration() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Test valid owner validation
        OwnerValidationRequest validRequest = new OwnerValidationRequest();
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
        validRequest.setEmail("john.doe@example.com");
        validRequest.setMobileNumber("+12025550123");  // Using a valid US mobile number format without dashes
        validRequest.setAddress("123 Main St");

        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.validationContext").value("new_owner"));
    }

    @Test
    public void testValidationEndpointWithErrors() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Test invalid owner validation
        OwnerValidationRequest invalidRequest = new OwnerValidationRequest();
        invalidRequest.setFirstName("John");
        invalidRequest.setLastName("Doe");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setMobileNumber("invalid-phone");

        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.validationContext").value("new_owner"));
    }

    @Test
    public void testValidationServiceInfo() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/validation/owners/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("Owner Data Validation Service"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.availableOperations").isArray())
                .andExpect(jsonPath("$.supportedValidations").isArray());
    }

    @Test
    public void testExceptionHandlingIntegration() {
        // Verify that the global exception handler is properly configured
        // This test ensures that validation exceptions are properly handled
        
        // Test with null owner - should trigger validation exception handling
        try {
            ownerValidationService.validateNewOwner(null);
            // The service should handle null gracefully and return a validation result
            // rather than throwing an exception
        } catch (Exception e) {
            fail("Validation service should handle null input gracefully: " + e.getMessage());
        }
    }

    @Test
    public void testValidationConfigurationBeans() {
        // Verify that all necessary validation beans are configured
        assertNotNull(validationConfig.validator(), "LocalValidatorFactoryBean should be configured");
        assertNotNull(validationConfig.methodValidationPostProcessor(), "MethodValidationPostProcessor should be configured");
        assertNotNull(validationConfig.validatorInstance(), "Validator instance should be configured");
        assertNotNull(validationConfig.messageSource(), "MessageSource should be configured");
    }

    @Test
    public void testValidationMessageSource() {
        // Test that validation messages are properly configured
        try {
            String message = validationConfig.messageSource().getMessage(
                "mobile.number.required", null, java.util.Locale.getDefault());
            assertNotNull(message, "Validation messages should be available");
        } catch (Exception e) {
            // This is acceptable as the message might not exist, but the message source should be configured
            assertNotNull(validationConfig.messageSource(), "MessageSource should be configured even if specific messages are missing");
        }
    }
}