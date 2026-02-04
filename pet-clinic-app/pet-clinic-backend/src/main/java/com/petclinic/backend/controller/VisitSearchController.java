package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.SearchErrorHandlingService;
import com.petclinic.backend.service.VisitSearchService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Visit search functionality
 * Provides specific search endpoints for visits by treatment, diagnosis, and description
 * Implements proper error handling, validation, and pagination support
 * 
 * API Endpoints:
 * - GET /api/visits/search/by-treatment - Search visits by treatment text
 * - GET /api/visits/search/by-diagnosis - Search visits by diagnosis text
 * - GET /api/visits/search/by-description - Search visits by description/notes text
 * 
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@RestController
@RequestMapping("/api/visits/search")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
public class VisitSearchController {

    private static final Logger logger = LoggerFactory.getLogger(VisitSearchController.class);

    @Autowired
    private VisitSearchService visitSearchService;

    @Autowired
    private SearchErrorHandlingService searchErrorHandlingService;

    /**
     * Search visits by treatment text
     * GET /api/visits/search/by-treatment?text={searchText}&page={page}&size={size}
     * 
     * @param text Search text for treatment field
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 10, max: 100)
     * @return PagedResponse of visits matching the treatment search criteria
     */
    @GetMapping("/by-treatment")
    public ResponseEntity<PagedResponse<Visit>> searchByTreatment(
            @RequestParam @NotBlank(message = "Search text cannot be empty") String text,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.debug("Searching visits by treatment: '{}', page: {}, size: {}", text, page, size);
        
        try {
            // Validate page size
            if (size > 100) {
                logger.warn("Page size {} exceeds maximum allowed (100), using 100", size);
                size = 100;
            }
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Use error handling service with retry mechanism
            PagedResponse<Visit> response = searchErrorHandlingService.executeSearchWithRetry(
                () -> visitSearchService.searchByTreatment(text, pageable),
                "treatment",
                text,
                pageable
            );
            
            // Check if there was an error
            if (response.isError()) {
                logger.warn("Search by treatment failed with error: {}", response.getErrorMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            logger.debug("Found {} total visits matching treatment '{}', returning page {} with {} results", 
                        response.getPage().getTotalElements(), text, page, response.getContent().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid search parameters for treatment search: {}", e.getMessage());
            
            // Create error response for validation errors
            PagedResponse<Visit> errorResponse = new PagedResponse<>();
            errorResponse.setError(true);
            errorResponse.setErrorMessage("Invalid search parameters: " + e.getMessage());
            errorResponse.setErrorType("VALIDATION_ERROR");
            errorResponse.setRetryable(false);
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error in treatment search controller: {}", e.getMessage(), e);
            
            // Create generic error response
            PagedResponse<Visit> errorResponse = new PagedResponse<>();
            errorResponse.setError(true);
            errorResponse.setErrorMessage(searchErrorHandlingService.getUserFriendlyErrorMessage("treatment", e));
            errorResponse.setErrorType("INTERNAL_ERROR");
            errorResponse.setRetryable(true);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search visits by diagnosis text
     * GET /api/visits/search/by-diagnosis?text={searchText}&page={page}&size={size}
     * 
     * @param text Search text for diagnosis field
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 10, max: 100)
     * @return PagedResponse of visits matching the diagnosis search criteria
     */
    @GetMapping("/by-diagnosis")
    public ResponseEntity<PagedResponse<Visit>> searchByDiagnosis(
            @RequestParam @NotBlank(message = "Search text cannot be empty") String text,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.debug("Searching visits by diagnosis: '{}', page: {}, size: {}", text, page, size);
        
        try {
            // Validate page size
            if (size > 100) {
                logger.warn("Page size {} exceeds maximum allowed (100), using 100", size);
                size = 100;
            }
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Use error handling service with retry mechanism
            PagedResponse<Visit> response = searchErrorHandlingService.executeSearchWithRetry(
                () -> visitSearchService.searchByDiagnosis(text, pageable),
                "diagnosis",
                text,
                pageable
            );
            
            // Check if there was an error
            if (response.isError()) {
                logger.warn("Search by diagnosis failed with error: {}", response.getErrorMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            logger.debug("Found {} total visits matching diagnosis '{}', returning page {} with {} results", 
                        response.getPage().getTotalElements(), text, page, response.getContent().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid search parameters for diagnosis search: {}", e.getMessage());
            
            // Create error response for validation errors
            PagedResponse<Visit> errorResponse = new PagedResponse<>();
            errorResponse.setError(true);
            errorResponse.setErrorMessage("Invalid search parameters: " + e.getMessage());
            errorResponse.setErrorType("VALIDATION_ERROR");
            errorResponse.setRetryable(false);
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error in diagnosis search controller: {}", e.getMessage(), e);
            
            // Create generic error response
            PagedResponse<Visit> errorResponse = new PagedResponse<>();
            errorResponse.setError(true);
            errorResponse.setErrorMessage(searchErrorHandlingService.getUserFriendlyErrorMessage("diagnosis", e));
            errorResponse.setErrorType("INTERNAL_ERROR");
            errorResponse.setRetryable(true);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search visits by description/notes text
     * GET /api/visits/search/by-description?text={searchText}&page={page}&size={size}
     * 
     * @param text Search text for description/notes field
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 10, max: 100)
     * @return PagedResponse of visits matching the description search criteria
     */
    @GetMapping("/by-description")
    public ResponseEntity<PagedResponse<Visit>> searchByDescription(
            @RequestParam @NotBlank(message = "Search text cannot be empty") String text,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.debug("Searching visits by description: '{}', page: {}, size: {}", text, page, size);
        
        try {
            // Validate page size
            if (size > 100) {
                logger.warn("Page size {} exceeds maximum allowed (100), using 100", size);
                size = 100;
            }
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Use error handling service with retry mechanism
            PagedResponse<Visit> response = searchErrorHandlingService.executeSearchWithRetry(
                () -> visitSearchService.searchByDescription(text, pageable),
                "description",
                text,
                pageable
            );
            
            // Check if there was an error
            if (response.isError()) {
                logger.warn("Search by description failed with error: {}", response.getErrorMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            logger.debug("Found {} total visits matching description '{}', returning page {} with {} results", 
                        response.getPage().getTotalElements(), text, page, response.getContent().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid search parameters for description search: {}", e.getMessage());
            
            // Create error response for validation errors
            PagedResponse<Visit> errorResponse = new PagedResponse<>();
            errorResponse.setError(true);
            errorResponse.setErrorMessage("Invalid search parameters: " + e.getMessage());
            errorResponse.setErrorType("VALIDATION_ERROR");
            errorResponse.setRetryable(false);
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error in description search controller: {}", e.getMessage(), e);
            
            // Create generic error response
            PagedResponse<Visit> errorResponse = new PagedResponse<>();
            errorResponse.setError(true);
            errorResponse.setErrorMessage(searchErrorHandlingService.getUserFriendlyErrorMessage("description", e));
            errorResponse.setErrorType("INTERNAL_ERROR");
            errorResponse.setRetryable(true);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}