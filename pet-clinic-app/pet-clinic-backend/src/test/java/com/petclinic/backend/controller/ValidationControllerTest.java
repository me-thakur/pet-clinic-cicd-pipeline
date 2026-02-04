package com.petclinic.backend.controller;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.dto.OwnerValidationRequest;
import com.petclinic.backend.dto.ValidationResponse;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.OwnerValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ValidationController
 * Tests REST endpoints for owner data validation
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@ExtendWith(MockitoExtension.class)
class ValidationControllerTest {
    
    @Mock
    private OwnerValidationService ownerValidationService;
    
    @InjectMocks
    private ValidationController validationController;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(validationController).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testValidateOwner_ValidData_ReturnsSuccess() throws Exception {
        // Arrange
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        ValidationResult validResult = ValidationResult.valid();
        when(ownerValidationService.validateNewOwner(any(Owner.class))).thenReturn(validResult);
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.overallMessage").value("All validation checks passed"))
                .andExpect(jsonPath("$.validationContext").value("new_owner"))
                .andExpect(jsonPath("$.metadata.validationStatus").value("PASSED"));
    }
    
    @Test
    void testValidateOwner_InvalidData_ReturnsValidationErrors() throws Exception {
        // Arrange - Use valid format but business logic invalid data
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        List<FieldValidationError> errors = Arrays.asList(
            new FieldValidationError("mobileNumber", "UNIQUENESS_VIOLATION", "Mobile number already exists", 
                                   "Please use a different mobile number", "+1-555-123-4567"),
            new FieldValidationError("email", "UNIQUENESS_VIOLATION", "Email already exists",
                                   "Please use a different email address", "john.doe@example.com")
        );
        
        ValidationResult invalidResult = ValidationResult.invalid(errors);
        when(ownerValidationService.validateNewOwner(any(Owner.class))).thenReturn(invalidResult);
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.validationContext").value("new_owner"))
                .andExpect(jsonPath("$.metadata.validationStatus").value("FAILED"));
    }
    
    @Test
    void testValidateOwnerUpdate_ValidData_ReturnsSuccess() throws Exception {
        // Arrange
        Long ownerId = 1L;
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        ValidationResult validResult = ValidationResult.valid();
        when(ownerValidationService.validateOwnerUpdate(eq(ownerId), any(Owner.class))).thenReturn(validResult);
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate/{ownerId}", ownerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.validationContext").value("owner_update"))
                .andExpect(jsonPath("$.metadata.ownerId").value(ownerId))
                .andExpect(jsonPath("$.metadata.validationStatus").value("PASSED"));
    }
    
    @Test
    void testValidateOwnerUpdate_InvalidOwnerId_ReturnsBadRequest() throws Exception {
        // Arrange
        Long invalidOwnerId = -1L;
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate/{ownerId}", invalidOwnerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.validationContext").value("owner_update"))
                .andExpect(jsonPath("$.overallMessage").value("Invalid owner ID: -1"));
    }
    
    @Test
    void testValidateOwnerFields_ValidFields_ReturnsSuccess() throws Exception {
        // Arrange
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        ValidationResult validResult = ValidationResult.valid();
        when(ownerValidationService.validateOwnerFields(any(Owner.class), eq("firstName"), eq("lastName")))
            .thenReturn(validResult);
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate-fields")
                .param("fields", "firstName,lastName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.validationContext").value("field_validation"))
                .andExpect(jsonPath("$.metadata.fieldCount").value(2))
                .andExpect(jsonPath("$.metadata.validationStatus").value("PASSED"));
    }
    
    @Test
    void testValidateOwnerFields_EmptyFields_ReturnsBadRequest() throws Exception {
        // Arrange
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate-fields")
                .param("fields", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.validationContext").value("field_validation"))
                .andExpect(jsonPath("$.overallMessage").value("No valid field names provided"));
    }
    
    @Test
    void testValidateOwnerFieldsUpdate_ValidData_ReturnsSuccess() throws Exception {
        // Arrange
        Long ownerId = 1L;
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        ValidationResult validResult = ValidationResult.valid();
        when(ownerValidationService.validateOwnerFieldsUpdate(eq(ownerId), any(Owner.class), eq("email")))
            .thenReturn(validResult);
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate-fields/{ownerId}", ownerId)
                .param("fields", "email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.validationContext").value("field_update_validation"))
                .andExpect(jsonPath("$.metadata.ownerId").value(ownerId))
                .andExpect(jsonPath("$.metadata.fieldCount").value(1))
                .andExpect(jsonPath("$.metadata.validationStatus").value("PASSED"));
    }
    
    @Test
    void testGetValidationInfo_ReturnsServiceInformation() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/validation/owners/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("Owner Data Validation Service"))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.availableOperations").isArray())
                .andExpect(jsonPath("$.supportedValidations").isArray())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }
    
    @Test
    void testValidateOwner_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        // Arrange - Request with missing required fields
        OwnerValidationRequest request = new OwnerValidationRequest();
        request.setEmail("john.doe@example.com");
        // firstName and lastName are missing (required fields)
        
        // Mock the validation service to return validation errors for missing fields
        ValidationResult invalidResult = ValidationResult.invalid("Missing required fields");
        invalidResult.addFieldError("firstName", "REQUIRED_FIELD_MISSING", "First name is required", 
                                   "Please provide a first name", null);
        invalidResult.addFieldError("lastName", "REQUIRED_FIELD_MISSING", "Last name is required", 
                                   "Please provide a last name", null);
        when(ownerValidationService.validateNewOwner(any(Owner.class))).thenReturn(invalidResult);
        
        // Act & Assert - Should return 200 OK with validation errors (not 400)
        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }
    
    @Test
    void testValidateOwner_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Arrange - Request with invalid email format
        OwnerValidationRequest request = new OwnerValidationRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("invalid-email-format");
        
        // Mock the validation service to return validation errors for invalid email
        ValidationResult invalidResult = ValidationResult.invalid("Invalid email format");
        invalidResult.addFieldError("email", "EMAIL_INVALID_FORMAT", "Invalid email address format", 
                                   "Please provide a valid email address", "invalid-email-format");
        when(ownerValidationService.validateNewOwner(any(Owner.class))).thenReturn(invalidResult);
        
        // Act & Assert - Should return 200 OK with validation errors (not 400)
        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].fieldName").value("email"));
    }
    
    @Test
    void testValidateOwner_ServiceException_ReturnsInternalServerError() throws Exception {
        // Arrange
        OwnerValidationRequest request = new OwnerValidationRequest(
            "John", "Doe", "john.doe@example.com", "+1-555-123-4567"
        );
        
        when(ownerValidationService.validateNewOwner(any(Owner.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act & Assert
        mockMvc.perform(post("/api/validation/owners/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.validationContext").value("new_owner"))
                .andExpect(jsonPath("$.overallMessage").value("Validation service error: Database connection failed"));
    }
}