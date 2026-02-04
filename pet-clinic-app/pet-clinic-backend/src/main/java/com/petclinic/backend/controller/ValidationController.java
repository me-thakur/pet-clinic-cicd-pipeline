package com.petclinic.backend.controller;

import com.petclinic.backend.dto.OwnerValidationRequest;
import com.petclinic.backend.dto.ValidationResponse;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.service.OwnerValidationService;
import com.petclinic.backend.service.ValidationErrorHandlingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Owner Data Validation
 * Provides comprehensive validation endpoints for owner information
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@RestController
@RequestMapping("/api/validation/owners")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins:http://localhost:8080}")
@Tag(name = "Owner Validation", description = "Endpoints for validating owner data against business rules and format standards")
public class ValidationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);
    
    @Autowired
    private OwnerValidationService ownerValidationService;
    
    @Autowired
    private ValidationErrorHandlingService validationErrorHandlingService;
    
    @Operation(summary = "Validate new owner data", 
               description = "Validates owner data for new owner creation against all business rules, format standards, and uniqueness constraints")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateOwner(
            @Parameter(description = "Owner data to validate", required = true)
            @RequestBody OwnerValidationRequest request) {
        
        logger.info("Received request to validate new owner: {} [auditId={}]", 
                   request.getFullName(), generateRequestId());
        
        try {
            // Convert request DTO to Owner entity
            Owner owner = convertToOwner(request);
            
            // Create validation context
            ValidationErrorHandlingService.ValidationContext context = 
                new ValidationErrorHandlingService.ValidationContext();
            context.setRequestId(generateRequestId());
            
            // Perform validation with graceful degradation
            ValidationResponse response = validationErrorHandlingService.executeValidationWithGracefulDegradation(
                () -> ownerValidationService.validateNewOwner(owner),
                owner,
                "new_owner",
                context
            );
            
            // Add additional metadata
            response.addMetadata("requestedFields", getRequestedFieldsCount(request));
            
            if (response.isValid()) {
                logger.info("Owner validation passed for: {}", request.getFullName());
                response.addMetadata("validationStatus", "PASSED");
            } else {
                logger.warn("Owner validation failed for: {} with {} errors", 
                           request.getFullName(), response.getErrorCount());
                response.addMetadata("validationStatus", "FAILED");
                response.addMetadata("errorFields", response.getFieldsWithErrors());
            }
            
            // Check if this was a degraded validation
            if (response.isWarning()) {
                logger.warn("Degraded validation performed for: {} - {}", 
                           request.getFullName(), response.getWarningMessage());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response); // 202 Accepted for degraded validation
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request data for owner validation: {}", e.getMessage());
            ValidationResponse errorResponse = ValidationResponse.invalid("Invalid request data: " + e.getMessage());
            errorResponse.setValidationContext("new_owner");
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error during owner validation: {}", e.getMessage(), e);
            ValidationResponse errorResponse = ValidationResponse.invalid("Validation service error: " + e.getMessage());
            errorResponse.setValidationContext("new_owner");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(summary = "Validate owner update data", 
               description = "Validates owner data for updating an existing owner, allowing the same owner to keep their unique values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or owner ID"),
        @ApiResponse(responseCode = "404", description = "Owner not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @PostMapping("/validate/{ownerId}")
    public ResponseEntity<ValidationResponse> validateOwnerUpdate(
            @Parameter(description = "ID of the owner being updated", required = true)
            @PathVariable Long ownerId,
            @Parameter(description = "Updated owner data to validate", required = true)
            @RequestBody OwnerValidationRequest request) {
        
        logger.info("Received request to validate owner update for ID: {} with data: {}", ownerId, request.getFullName());
        
        try {
            // Validate owner ID
            if (ownerId == null || ownerId <= 0) {
                logger.error("Invalid owner ID provided: {}", ownerId);
                ValidationResponse errorResponse = ValidationResponse.invalid("Invalid owner ID: " + ownerId);
                errorResponse.setValidationContext("owner_update");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert request DTO to Owner entity
            Owner owner = convertToOwner(request);
            
            // Perform validation for update
            ValidationResult result = ownerValidationService.validateOwnerUpdate(ownerId, owner);
            
            // Convert result to response DTO
            ValidationResponse response = ValidationResponse.fromValidationResult(result);
            response.setValidationContext("owner_update");
            response.addMetadata("ownerId", ownerId);
            response.addMetadata("requestedFields", getRequestedFieldsCount(request));
            
            if (result.isValid()) {
                logger.info("Owner update validation passed for ID: {} with data: {}", ownerId, request.getFullName());
                response.addMetadata("validationStatus", "PASSED");
            } else {
                logger.warn("Owner update validation failed for ID: {} with {} errors", ownerId, result.getErrorCount());
                response.addMetadata("validationStatus", "FAILED");
                response.addMetadata("errorFields", response.getFieldsWithErrors());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request data for owner update validation: {}", e.getMessage());
            ValidationResponse errorResponse = ValidationResponse.invalid("Invalid request data: " + e.getMessage());
            errorResponse.setValidationContext("owner_update");
            errorResponse.addMetadata("ownerId", ownerId);
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error during owner update validation for ID {}: {}", ownerId, e.getMessage(), e);
            ValidationResponse errorResponse = ValidationResponse.invalid("Validation service error: " + e.getMessage());
            errorResponse.setValidationContext("owner_update");
            errorResponse.addMetadata("ownerId", ownerId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(summary = "Validate specific owner fields", 
               description = "Validates only specific fields of owner data, useful for partial validation during form input")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Field validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or field names"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @PostMapping("/validate-fields")
    public ResponseEntity<ValidationResponse> validateOwnerFields(
            @Parameter(description = "Owner data to validate", required = true)
            @RequestBody OwnerValidationRequest request,
            @Parameter(description = "Comma-separated list of field names to validate", required = true)
            @RequestParam String fields) {
        
        logger.info("Received request to validate specific fields: {} for owner: {}", fields, request.getFullName());
        
        try {
            // Parse field names
            String[] fieldNames = parseFieldNames(fields);
            if (fieldNames.length == 0) {
                ValidationResponse errorResponse = ValidationResponse.invalid("No valid field names provided");
                errorResponse.setValidationContext("field_validation");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert request DTO to Owner entity
            Owner owner = convertToOwner(request);
            
            // Perform field-specific validation
            ValidationResult result = ownerValidationService.validateOwnerFields(owner, fieldNames);
            
            // Convert result to response DTO
            ValidationResponse response = ValidationResponse.fromValidationResult(result);
            response.setValidationContext("field_validation");
            response.addMetadata("validatedFields", fieldNames);
            response.addMetadata("fieldCount", fieldNames.length);
            
            if (result.isValid()) {
                logger.info("Field validation passed for fields: {} on owner: {}", fields, request.getFullName());
                response.addMetadata("validationStatus", "PASSED");
            } else {
                logger.warn("Field validation failed for fields: {} on owner: {} with {} errors", 
                           fields, request.getFullName(), result.getErrorCount());
                response.addMetadata("validationStatus", "FAILED");
                response.addMetadata("errorFields", response.getFieldsWithErrors());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request data for field validation: {}", e.getMessage());
            ValidationResponse errorResponse = ValidationResponse.invalid("Invalid request data: " + e.getMessage());
            errorResponse.setValidationContext("field_validation");
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error during field validation: {}", e.getMessage(), e);
            ValidationResponse errorResponse = ValidationResponse.invalid("Validation service error: " + e.getMessage());
            errorResponse.setValidationContext("field_validation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(summary = "Validate specific fields for owner update", 
               description = "Validates only specific fields for an owner update, allowing the same owner to keep their unique values")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Field validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data, owner ID, or field names"),
        @ApiResponse(responseCode = "404", description = "Owner not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @PostMapping("/validate-fields/{ownerId}")
    public ResponseEntity<ValidationResponse> validateOwnerFieldsUpdate(
            @Parameter(description = "ID of the owner being updated", required = true)
            @PathVariable Long ownerId,
            @Parameter(description = "Updated owner data to validate", required = true)
            @RequestBody OwnerValidationRequest request,
            @Parameter(description = "Comma-separated list of field names to validate", required = true)
            @RequestParam String fields) {
        
        logger.info("Received request to validate specific fields: {} for owner update ID: {} with data: {}", 
                   fields, ownerId, request.getFullName());
        
        try {
            // Validate owner ID
            if (ownerId == null || ownerId <= 0) {
                logger.error("Invalid owner ID provided: {}", ownerId);
                ValidationResponse errorResponse = ValidationResponse.invalid("Invalid owner ID: " + ownerId);
                errorResponse.setValidationContext("field_update_validation");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Parse field names
            String[] fieldNames = parseFieldNames(fields);
            if (fieldNames.length == 0) {
                ValidationResponse errorResponse = ValidationResponse.invalid("No valid field names provided");
                errorResponse.setValidationContext("field_update_validation");
                errorResponse.addMetadata("ownerId", ownerId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert request DTO to Owner entity
            Owner owner = convertToOwner(request);
            
            // Perform field-specific validation for update
            ValidationResult result = ownerValidationService.validateOwnerFieldsUpdate(ownerId, owner, fieldNames);
            
            // Convert result to response DTO
            ValidationResponse response = ValidationResponse.fromValidationResult(result);
            response.setValidationContext("field_update_validation");
            response.addMetadata("ownerId", ownerId);
            response.addMetadata("validatedFields", fieldNames);
            response.addMetadata("fieldCount", fieldNames.length);
            
            if (result.isValid()) {
                logger.info("Field update validation passed for fields: {} on owner ID: {} with data: {}", 
                           fields, ownerId, request.getFullName());
                response.addMetadata("validationStatus", "PASSED");
            } else {
                logger.warn("Field update validation failed for fields: {} on owner ID: {} with {} errors", 
                           fields, ownerId, result.getErrorCount());
                response.addMetadata("validationStatus", "FAILED");
                response.addMetadata("errorFields", response.getFieldsWithErrors());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request data for field update validation: {}", e.getMessage());
            ValidationResponse errorResponse = ValidationResponse.invalid("Invalid request data: " + e.getMessage());
            errorResponse.setValidationContext("field_update_validation");
            errorResponse.addMetadata("ownerId", ownerId);
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error during field update validation for owner ID {}: {}", ownerId, e.getMessage(), e);
            ValidationResponse errorResponse = ValidationResponse.invalid("Validation service error: " + e.getMessage());
            errorResponse.setValidationContext("field_update_validation");
            errorResponse.addMetadata("ownerId", ownerId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(summary = "Validate owner update data with optimization", 
               description = "Validates owner data for updating with optimization options for partial updates")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or owner ID"),
        @ApiResponse(responseCode = "404", description = "Owner not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @PostMapping("/validate-optimized/{ownerId}")
    public ResponseEntity<ValidationResponse> validateOwnerUpdateOptimized(
            @Parameter(description = "ID of the owner being updated", required = true)
            @PathVariable Long ownerId,
            @Parameter(description = "Updated owner data to validate", required = true)
            @RequestBody OwnerValidationRequest request,
            @Parameter(description = "Whether to validate only changed (non-null) fields", required = false)
            @RequestParam(defaultValue = "false") boolean validateOnlyChangedFields,
            @Parameter(description = "Comma-separated list of specific field names to validate", required = false)
            @RequestParam(required = false) String fields) {
        
        logger.info("Received optimized validation request for owner update ID: {} with validateOnlyChangedFields: {} and fields: {}", 
                   ownerId, validateOnlyChangedFields, fields);
        
        try {
            // Validate owner ID
            if (ownerId == null || ownerId <= 0) {
                logger.error("Invalid owner ID provided: {}", ownerId);
                ValidationResponse errorResponse = ValidationResponse.invalid("Invalid owner ID: " + ownerId);
                errorResponse.setValidationContext("optimized_owner_update");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Convert request DTO to Owner entity
            Owner owner = convertToOwner(request);
            
            // Parse field names if provided
            String[] fieldNames = null;
            if (fields != null && !fields.trim().isEmpty()) {
                fieldNames = parseFieldNames(fields);
            }
            
            // Perform optimized validation for update
            ValidationResult result = ownerValidationService.validateOwnerUpdateOptimized(
                ownerId, owner, validateOnlyChangedFields, fieldNames);
            
            // Convert result to response DTO
            ValidationResponse response = ValidationResponse.fromValidationResult(result);
            response.setValidationContext("optimized_owner_update");
            response.addMetadata("ownerId", ownerId);
            response.addMetadata("validateOnlyChangedFields", validateOnlyChangedFields);
            response.addMetadata("requestedFields", getRequestedFieldsCount(request));
            
            if (fields != null) {
                response.addMetadata("specifiedFields", fieldNames);
            }
            
            if (result.isValid()) {
                logger.info("Optimized owner update validation passed for ID: {} with data: {}", ownerId, request.getFullName());
                response.addMetadata("validationStatus", "PASSED");
            } else {
                logger.warn("Optimized owner update validation failed for ID: {} with {} errors", ownerId, result.getErrorCount());
                response.addMetadata("validationStatus", "FAILED");
                response.addMetadata("errorFields", response.getFieldsWithErrors());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request data for optimized owner update validation: {}", e.getMessage());
            ValidationResponse errorResponse = ValidationResponse.invalid("Invalid request data: " + e.getMessage());
            errorResponse.setValidationContext("optimized_owner_update");
            errorResponse.addMetadata("ownerId", ownerId);
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error during optimized owner update validation for ID {}: {}", ownerId, e.getMessage(), e);
            ValidationResponse errorResponse = ValidationResponse.invalid("Validation service error: " + e.getMessage());
            errorResponse.setValidationContext("optimized_owner_update");
            errorResponse.addMetadata("ownerId", ownerId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(summary = "Get validation service information", 
               description = "Get information about the validation service capabilities and supported operations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service information retrieved successfully")
    })
    @GetMapping("/info")
    public ResponseEntity<ValidationServiceInfo> getValidationInfo() {
        logger.info("Received request for validation service information");
        
        
        ValidationServiceInfo info = new ValidationServiceInfo(
            "Owner Data Validation Service",
            "Provides comprehensive validation of owner information with international format validation and system-wide uniqueness checks",
            new String[]{
                "Validate new owner data",
                "Validate owner update data", 
                "Validate owner update data with optimization",
                "Validate specific owner fields",
                "Validate specific fields for owner update"
            },
            new String[]{
                "International mobile number format validation (E.164)",
                "Email format validation (RFC 5322)",
                "Mobile number uniqueness validation",
                "Email uniqueness validation",
                "Business rule validation",
                "Field-specific error reporting",
                "Corrective guidance for validation errors",
                "Optimized partial update validation"
            },
            "1.0.0"
        );
        
        return ResponseEntity.ok(info);
    }
    
    @Operation(summary = "Check validation service availability", 
               description = "Check if the validation service is currently available or in fallback mode")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service status retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "Service is in fallback mode")
    })
    @GetMapping("/health")
    public ResponseEntity<ValidationServiceHealth> getValidationHealth() {
        logger.debug("Received request for validation service health check");
        
        boolean isAvailable = ownerValidationService.isValidationServiceAvailable();
        String circuitBreakerState = ownerValidationService.getCircuitBreakerState();
        int failureCount = ownerValidationService.getCurrentFailureCount();
        
        ValidationServiceHealth health = new ValidationServiceHealth(
            isAvailable,
            circuitBreakerState,
            failureCount,
            isAvailable ? "Service is operating normally" : "Service is in fallback mode",
            System.currentTimeMillis()
        );
        
        if (isAvailable) {
            logger.debug("Validation service health check: AVAILABLE");
            return ResponseEntity.ok(health);
        } else {
            logger.warn("Validation service health check: UNAVAILABLE (fallback mode)");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    // Helper methods
    private Owner convertToOwner(OwnerValidationRequest request) {
        Owner owner = new Owner();
        owner.setFirstName(request.getFirstName());
        owner.setLastName(request.getLastName());
        owner.setAddress(request.getAddress());
        owner.setCity(request.getCity());
        owner.setState(request.getState());
        owner.setZipCode(request.getZipCode());
        owner.setTelephone(request.getTelephone());
        owner.setMobileNumber(request.getMobileNumber());
        owner.setEmail(request.getEmail());
        return owner;
    }
    
    private String[] parseFieldNames(String fields) {
        if (fields == null || fields.trim().isEmpty()) {
            return new String[0];
        }
        
        return java.util.Arrays.stream(fields.split(","))
            .map(String::trim)
            .filter(field -> !field.isEmpty())
            .toArray(String[]::new);
    }
    
    private int getRequestedFieldsCount(OwnerValidationRequest request) {
        int count = 0;
        if (request.getFirstName() != null) count++;
        if (request.getLastName() != null) count++;
        if (request.getAddress() != null) count++;
        if (request.getCity() != null) count++;
        if (request.getState() != null) count++;
        if (request.getZipCode() != null) count++;
        if (request.getTelephone() != null) count++;
        if (request.getMobileNumber() != null) count++;
        if (request.getEmail() != null) count++;
        return count;
    }
    
    /**
     * Information about the validation service
     */
    public static class ValidationServiceInfo {
        private final String serviceName;
        private final String description;
        private final String[] availableOperations;
        private final String[] supportedValidations;
        private final String version;
        
        public ValidationServiceInfo(String serviceName, String description, String[] availableOperations, 
                                   String[] supportedValidations, String version) {
            this.serviceName = serviceName;
            this.description = description;
            this.availableOperations = availableOperations;
            this.supportedValidations = supportedValidations;
            this.version = version;
        }
        
        public String getServiceName() { return serviceName; }
        public String getDescription() { return description; }
        public String[] getAvailableOperations() { return availableOperations; }
        public String[] getSupportedValidations() { return supportedValidations; }
        public String getVersion() { return version; }
    }
    
    /**
     * Health information about the validation service
     */
    public static class ValidationServiceHealth {
        private final boolean available;
        private final String circuitBreakerState;
        private final int failureCount;
        private final String status;
        private final long timestamp;
        
        public ValidationServiceHealth(boolean available, String circuitBreakerState, int failureCount, 
                                     String status, long timestamp) {
            this.available = available;
            this.circuitBreakerState = circuitBreakerState;
            this.failureCount = failureCount;
            this.status = status;
            this.timestamp = timestamp;
        }
        
        public boolean isAvailable() { return available; }
        public String getCircuitBreakerState() { return circuitBreakerState; }
        public int getFailureCount() { return failureCount; }
        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Get user-friendly display name for field
     */
    private String getFieldDisplayName(String fieldName) {
        Map<String, String> displayNames = Map.of(
            "firstName", "First Name",
            "lastName", "Last Name", 
            "email", "Email Address",
            "telephone", "Telephone",
            "mobileNumber", "Mobile Number",
            "address", "Address",
            "city", "City",
            "state", "State",
            "zipCode", "ZIP Code"
        );
        return displayNames.getOrDefault(fieldName, fieldName);
    }
    
    /**
     * Generate a unique request ID for tracking validation requests
     */
    private String generateRequestId() {
        return "REQ-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}