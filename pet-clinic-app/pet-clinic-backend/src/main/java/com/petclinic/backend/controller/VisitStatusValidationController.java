package com.petclinic.backend.controller;

import com.petclinic.backend.service.VisitStatusValidationService;
import com.petclinic.backend.service.VisitStatusValidationService.ValidationResult;
import com.petclinic.backend.service.VisitStatusValidationService.RecalculationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for visit status validation operations
 * Provides administrative endpoints for validating visit completion status consistency
 * Validates: Requirements 5.1, 5.2
 */
@RestController
@RequestMapping("/api/admin/visit-validation")
@Tag(name = "Visit Status Validation", description = "Administrative endpoints for validating visit completion status consistency")
public class VisitStatusValidationController {
    
    private static final Logger logger = LoggerFactory.getLogger(VisitStatusValidationController.class);
    
    @Autowired
    private VisitStatusValidationService validationService;
    
    @Operation(summary = "Validate all visit completion statuses", 
               description = "Validates completion status consistency for all visits in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @GetMapping("/all")
    public ResponseEntity<ValidationResult> validateAllVisits() {
        logger.info("Received request to validate all visit statuses");
        
        try {
            ValidationResult result = validationService.validateAllVisitStatuses();
            
            if (result.hasInconsistencies()) {
                logger.warn("Validation completed with {} inconsistencies found out of {} visits", 
                           result.getInconsistentVisitsCount(), result.getTotalVisitsChecked());
            } else {
                logger.info("Validation completed successfully - no inconsistencies found in {} visits", 
                           result.getTotalVisitsChecked());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during visit status validation: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Operation(summary = "Validate visit completion statuses by date range", 
               description = "Validates completion status consistency for visits within a specified date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range provided"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @GetMapping("/date-range")
    public ResponseEntity<ValidationResult> validateVisitsByDateRange(
            @Parameter(description = "Start date for validation range (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for validation range (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Received request to validate visit statuses for date range: {} to {}", startDate, endDate);
        
        try {
            ValidationResult result = validationService.validateVisitStatusesByDateRange(startDate, endDate);
            
            if (result.hasInconsistencies()) {
                logger.warn("Date range validation completed with {} inconsistencies found out of {} visits", 
                           result.getInconsistentVisitsCount(), result.getTotalVisitsChecked());
            } else {
                logger.info("Date range validation completed successfully - no inconsistencies found in {} visits", 
                           result.getTotalVisitsChecked());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during date range visit status validation: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Operation(summary = "Validate specific visit completion status", 
               description = "Validates completion status consistency for a specific visit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "404", description = "Visit not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @GetMapping("/visit/{visitId}")
    public ResponseEntity<ValidationResult> validateVisit(
            @Parameter(description = "Visit ID to validate", required = true)
            @PathVariable Long visitId) {
        
        logger.info("Received request to validate visit status for visit ID: {}", visitId);
        
        try {
            ValidationResult result = validationService.validateVisitStatus(visitId);
            
            if (result.hasInconsistencies()) {
                logger.warn("Visit {} validation found inconsistency", visitId);
            } else {
                logger.info("Visit {} validation completed successfully - no inconsistencies found", visitId);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during visit {} status validation: {}", visitId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Operation(summary = "Validate visit completion statuses by pet", 
               description = "Validates completion status consistency for all visits of a specific pet")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "404", description = "Pet not found or no visits found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during validation")
    })
    @GetMapping("/pet/{petId}")
    public ResponseEntity<ValidationResult> validateVisitsByPet(
            @Parameter(description = "Pet ID to validate visits for", required = true)
            @PathVariable Long petId) {
        
        logger.info("Received request to validate visit statuses for pet ID: {}", petId);
        
        try {
            ValidationResult result = validationService.validateVisitStatusesByPet(petId);
            
            if (result.hasInconsistencies()) {
                logger.warn("Pet {} visit validation completed with {} inconsistencies found out of {} visits", 
                           petId, result.getInconsistentVisitsCount(), result.getTotalVisitsChecked());
            } else {
                logger.info("Pet {} visit validation completed successfully - no inconsistencies found in {} visits", 
                           petId, result.getTotalVisitsChecked());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during pet {} visit status validation: {}", petId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Operation(summary = "Get validation summary", 
               description = "Get a summary of the validation capabilities and recent validation statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    })
    @GetMapping("/summary")
    public ResponseEntity<ValidationSummary> getValidationSummary() {
        logger.info("Received request for validation summary");
        
        ValidationSummary summary = new ValidationSummary(
            "Visit Status Validation Service",
            "Validates that visit completion status is consistent with diagnosis and treatment data",
            new String[]{
                "Validate all visits",
                "Validate visits by date range", 
                "Validate specific visit",
                "Validate visits by pet",
                "Recalculate all visit statuses",
                "Recalculate visit statuses by date range",
                "Recalculate visit statuses by pet"
            },
            "A visit is considered completed if both diagnosis and treatment fields contain non-empty, non-whitespace text"
        );
        
        return ResponseEntity.ok(summary);
    }
    
    @Operation(summary = "Recalculate all visit completion statuses", 
               description = "Performs batch recalculation of completion status for all visits in the system. " +
                           "Uses the same logic as isCompleted() method to ensure consistency.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recalculation completed successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error during recalculation")
    })
    @PostMapping("/recalculate/all")
    public ResponseEntity<RecalculationResult> recalculateAllVisitStatuses() {
        logger.info("Received request to recalculate all visit statuses");
        
        try {
            RecalculationResult result = validationService.recalculateAllVisitStatuses();
            
            if (result.hasErrors()) {
                logger.warn("Recalculation completed with {} errors out of {} visits processed in {} ms", 
                           result.getErrors().size(), result.getTotalVisitsProcessed(), result.getProcessingTimeMs());
            } else {
                logger.info("Recalculation completed successfully - processed {} visits, updated {} in {} ms", 
                           result.getTotalVisitsProcessed(), result.getVisitsUpdated(), result.getProcessingTimeMs());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during visit status recalculation: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Operation(summary = "Recalculate visit completion statuses by date range", 
               description = "Performs batch recalculation of completion status for visits within a specified date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recalculation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range provided"),
        @ApiResponse(responseCode = "500", description = "Internal server error during recalculation")
    })
    @PostMapping("/recalculate/date-range")
    public ResponseEntity<RecalculationResult> recalculateVisitStatusesByDateRange(
            @Parameter(description = "Start date for recalculation range (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for recalculation range (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Received request to recalculate visit statuses for date range: {} to {}", startDate, endDate);
        
        try {
            RecalculationResult result = validationService.recalculateVisitStatusesByDateRange(startDate, endDate);
            
            if (result.hasErrors()) {
                logger.warn("Date range recalculation completed with {} errors out of {} visits processed in {} ms", 
                           result.getErrors().size(), result.getTotalVisitsProcessed(), result.getProcessingTimeMs());
            } else {
                logger.info("Date range recalculation completed successfully - processed {} visits, updated {} in {} ms", 
                           result.getTotalVisitsProcessed(), result.getVisitsUpdated(), result.getProcessingTimeMs());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during date range visit status recalculation: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Operation(summary = "Recalculate visit completion statuses by pet", 
               description = "Performs batch recalculation of completion status for all visits of a specific pet")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recalculation completed successfully"),
        @ApiResponse(responseCode = "404", description = "Pet not found or no visits found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during recalculation")
    })
    @PostMapping("/recalculate/pet/{petId}")
    public ResponseEntity<RecalculationResult> recalculateVisitStatusesByPet(
            @Parameter(description = "Pet ID to recalculate visits for", required = true)
            @PathVariable Long petId) {
        
        logger.info("Received request to recalculate visit statuses for pet ID: {}", petId);
        
        try {
            RecalculationResult result = validationService.recalculateVisitStatusesByPet(petId);
            
            if (result.hasErrors()) {
                logger.warn("Pet {} recalculation completed with {} errors out of {} visits processed in {} ms", 
                           petId, result.getErrors().size(), result.getTotalVisitsProcessed(), result.getProcessingTimeMs());
            } else {
                logger.info("Pet {} recalculation completed successfully - processed {} visits, updated {} in {} ms", 
                           petId, result.getTotalVisitsProcessed(), result.getVisitsUpdated(), result.getProcessingTimeMs());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during pet {} visit status recalculation: {}", petId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Summary information about the validation service
     */
    public static class ValidationSummary {
        private final String serviceName;
        private final String description;
        private final String[] availableOperations;
        private final String completionLogic;
        
        public ValidationSummary(String serviceName, String description, String[] availableOperations, String completionLogic) {
            this.serviceName = serviceName;
            this.description = description;
            this.availableOperations = availableOperations;
            this.completionLogic = completionLogic;
        }
        
        public String getServiceName() { return serviceName; }
        public String getDescription() { return description; }
        public String[] getAvailableOperations() { return availableOperations; }
        public String getCompletionLogic() { return completionLogic; }
    }
}