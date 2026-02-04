package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SeniorPetInfo;
import com.petclinic.backend.dto.SeniorPetSearchCriteria;
import com.petclinic.backend.service.SeniorPetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Senior Pet management and search operations
 * Provides specialized functionality for senior pet care and monitoring
 * Implements configurable age thresholds per species and search result highlighting
 * 
 * API Endpoints:
 * - GET /api/pets/senior/search - Search senior pets with comprehensive filters
 * - GET /api/pets/senior - Get all senior pets with pagination
 * - GET /api/pets/senior/species/{species} - Get senior pets by species
 * - GET /api/pets/senior/health-condition - Get senior pets with health conditions
 * - GET /api/pets/senior/special-care - Get senior pets needing special care
 * - GET /api/pets/senior/statistics - Get senior pet statistics
 * - GET /api/pets/senior/age-thresholds - Get configurable age thresholds
 * - PUT /api/pets/senior/age-thresholds/{species} - Update age threshold for species
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@RestController
@RequestMapping("/api/senior-pets")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
@Tag(name = "Senior Pet Management", description = "Specialized senior pet search and management operations")
@SecurityRequirement(name = "bearerAuth")
public class SeniorPetController {

    private static final Logger logger = LoggerFactory.getLogger(SeniorPetController.class);

    @Autowired
    private SeniorPetService seniorPetService;

    // ========================================
    // Search Endpoints - Requirement 2.1
    // ========================================

    /**
     * Search senior pets with comprehensive filters
     * GET /api/pets/senior/search?minAge=8&maxAge=15&species=Dog&healthCondition=arthritis&page=0&size=10
     * 
     * @param minAge Minimum age filter
     * @param maxAge Maximum age filter
     * @param species Species filter
     * @param breed Breed filter
     * @param healthCondition Health condition filter
     * @param ownerId Owner ID filter
     * @param ownerName Owner name filter
     * @param searchTerm General search term
     * @param sortBy Sort field (age, name, species, breed, owner)
     * @param sortDirection Sort direction (asc, desc)
     * @param pageable Pagination parameters
     * @return Paginated search results with highlighting
     */
    @Operation(
        summary = "Search senior pets with comprehensive filters",
        description = "Searches senior pets using age, species, and health filters with configurable age thresholds per species. " +
                     "Results include highlighting for age-related information and health indicators. " +
                     "Supports fallback to basic pet listing with age sorting when search fails.",
        tags = {"Senior Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved senior pets matching search criteria",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search parameters provided",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Falls back to basic pet listing",
            content = @Content
        )
    })
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<SeniorPetInfo>> searchSeniorPets(
            @Parameter(description = "Minimum age in years", example = "8")
            @RequestParam(required = false) @Min(0) Integer minAge,
            
            @Parameter(description = "Maximum age in years", example = "15")
            @RequestParam(required = false) @Min(0) Integer maxAge,
            
            @Parameter(description = "Pet species (Dog, Cat, Bird, etc.)", example = "Dog")
            @RequestParam(required = false) String species,
            
            @Parameter(description = "Pet breed", example = "Labrador")
            @RequestParam(required = false) String breed,
            
            @Parameter(description = "Health condition to filter by", example = "arthritis")
            @RequestParam(required = false) String healthCondition,
            
            @Parameter(description = "Owner ID", example = "1")
            @RequestParam(required = false) Long ownerId,
            
            @Parameter(description = "Owner name", example = "John Smith")
            @RequestParam(required = false) String ownerName,
            
            @Parameter(description = "General search term", example = "Max")
            @RequestParam(required = false) String searchTerm,
            
            @Parameter(description = "Sort field", example = "age")
            @RequestParam(defaultValue = "age") String sortBy,
            
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection,
            
            Pageable pageable) {
        
        logger.debug("Searching senior pets with criteria - minAge: {}, maxAge: {}, species: {}, healthCondition: {}", 
                    minAge, maxAge, species, healthCondition);
        
        try {
            SeniorPetSearchCriteria criteria = SeniorPetSearchCriteria.builder()
                .minAge(minAge)
                .maxAge(maxAge)
                .species(species)
                .breed(breed)
                .healthCondition(healthCondition)
                .ownerId(ownerId)
                .ownerName(ownerName)
                .searchTerm(searchTerm)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
            
            Page<SeniorPetInfo> results = seniorPetService.searchSeniorPets(criteria, pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(results);
            
            logger.debug("Found {} senior pets matching search criteria", results.getTotalElements());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching senior pets", e);
            // Fallback to basic senior pets list - Requirement 2.5
            Page<SeniorPetInfo> fallbackResults = seniorPetService.getFallbackSeniorPets(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(fallbackResults);
            response.setError(true);
            response.setErrorMessage("Search temporarily unavailable. Showing basic senior pets list sorted by age.");
            response.setRetryable(true);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get all senior pets with pagination
     * GET /api/pets/senior?page=0&size=10
     * 
     * @param pageable Pagination parameters
     * @return Paginated list of senior pets
     */
    @Operation(
        summary = "Get all senior pets",
        description = "Retrieves all senior pets using default age threshold of 7 years, with pagination support.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping
    public ResponseEntity<PagedResponse<SeniorPetInfo>> getAllSeniorPets(Pageable pageable) {
        logger.debug("Getting all senior pets with pagination: {}", pageable);
        
        try {
            Page<SeniorPetInfo> results = seniorPetService.getAllSeniorPets(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(results);
            
            logger.debug("Retrieved {} senior pets", results.getTotalElements());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting all senior pets", e);
            Page<SeniorPetInfo> fallbackResults = seniorPetService.getFallbackSeniorPets(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(fallbackResults);
            response.setError(true);
            response.setErrorMessage("Error loading senior pets. Showing fallback list.");
            return ResponseEntity.ok(response);
        }
    }

    // ========================================
    // Species-Specific Endpoints - Requirement 2.2
    // ========================================

    /**
     * Get senior pets by species with species-specific age threshold
     * GET /api/pets/senior/species/Dog?page=0&size=10
     * 
     * @param species Pet species
     * @param pageable Pagination parameters
     * @return Paginated list of senior pets for the species
     */
    @Operation(
        summary = "Get senior pets by species",
        description = "Retrieves senior pets for a specific species using configurable age thresholds. " +
                     "Different species have different age thresholds (e.g., Dogs: 7 years, Birds: 5 years).",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/species/{species}")
    public ResponseEntity<PagedResponse<SeniorPetInfo>> getSeniorPetsBySpecies(
            @Parameter(description = "Pet species", required = true, example = "Dog")
            @PathVariable String species,
            Pageable pageable) {
        
        logger.debug("Getting senior pets by species: {}", species);
        
        try {
            Page<SeniorPetInfo> results = seniorPetService.getSeniorPetsBySpecies(species, pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(results);
            
            logger.debug("Found {} senior {} pets", results.getTotalElements(), species);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting senior pets by species: {}", species, e);
            Page<SeniorPetInfo> fallbackResults = seniorPetService.getFallbackSeniorPets(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(fallbackResults);
            response.setError(true);
            response.setErrorMessage("Error loading senior pets for species. Showing all senior pets.");
            return ResponseEntity.ok(response);
        }
    }

    // ========================================
    // Health Condition Endpoints - Requirement 2.1
    // ========================================

    /**
     * Get senior pets with specific health condition
     * GET /api/pets/senior/health-condition?condition=arthritis&page=0&size=10
     * 
     * @param condition Health condition to filter by
     * @param pageable Pagination parameters
     * @return Paginated list of senior pets with the health condition
     */
    @Operation(
        summary = "Get senior pets with health condition",
        description = "Retrieves senior pets that have a specific health condition in their medical history. " +
                     "Results include health highlighting for matched conditions.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/health-condition")
    public ResponseEntity<PagedResponse<SeniorPetInfo>> getSeniorPetsWithHealthCondition(
            @Parameter(description = "Health condition to filter by", required = true, example = "arthritis")
            @RequestParam String condition,
            Pageable pageable) {
        
        logger.debug("Getting senior pets with health condition: {}", condition);
        
        try {
            Page<SeniorPetInfo> results = seniorPetService.getSeniorPetsWithHealthCondition(condition, pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(results);
            
            logger.debug("Found {} senior pets with health condition: {}", results.getTotalElements(), condition);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting senior pets with health condition: {}", condition, e);
            Page<SeniorPetInfo> fallbackResults = seniorPetService.getFallbackSeniorPets(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(fallbackResults);
            response.setError(true);
            response.setErrorMessage("Error filtering by health condition. Showing all senior pets.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get senior pets needing special care
     * GET /api/pets/senior/special-care?page=0&size=10
     * 
     * @param pageable Pagination parameters
     * @return Paginated list of senior pets needing special care
     */
    @Operation(
        summary = "Get senior pets needing special care",
        description = "Retrieves senior pets that need special care based on medical history and visit patterns.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/special-care")
    public ResponseEntity<PagedResponse<SeniorPetInfo>> getSeniorPetsNeedingSpecialCare(Pageable pageable) {
        logger.debug("Getting senior pets needing special care");
        
        try {
            Page<SeniorPetInfo> results = seniorPetService.getSeniorPetsNeedingSpecialCare(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(results);
            
            logger.debug("Found {} senior pets needing special care", results.getTotalElements());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting senior pets needing special care", e);
            Page<SeniorPetInfo> fallbackResults = seniorPetService.getFallbackSeniorPets(pageable);
            PagedResponse<SeniorPetInfo> response = new PagedResponse<>(fallbackResults);
            response.setError(true);
            response.setErrorMessage("Error identifying pets needing special care. Showing all senior pets.");
            return ResponseEntity.ok(response);
        }
    }

    // ========================================
    // Configuration Endpoints - Requirement 2.2
    // ========================================

    /**
     * Get configurable age thresholds per species
     * GET /api/pets/senior/age-thresholds
     * 
     * @return Map of species to age threshold
     */
    @Operation(
        summary = "Get age thresholds per species",
        description = "Retrieves the configurable age thresholds used to determine senior status for different species.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/age-thresholds")
    public ResponseEntity<Map<String, Integer>> getSpeciesAgeThresholds() {
        logger.debug("Getting species age thresholds");
        
        try {
            Map<String, Integer> thresholds = seniorPetService.getSpeciesAgeThresholds();
            logger.debug("Retrieved age thresholds for {} species", thresholds.size());
            return ResponseEntity.ok(thresholds);
            
        } catch (Exception e) {
            logger.error("Error getting species age thresholds", e);
            return ResponseEntity.ok(Map.of("Dog", 7, "Cat", 7, "Bird", 5, "Rabbit", 5));
        }
    }

    /**
     * Update age threshold for a specific species
     * PUT /api/pets/senior/age-thresholds/Dog?threshold=8
     * 
     * @param species Pet species
     * @param threshold New age threshold
     * @return Updated age thresholds map
     */
    @Operation(
        summary = "Update age threshold for species",
        description = "Updates the age threshold used to determine senior status for a specific species.",
        tags = {"Senior Pet Management"}
    )
    @PutMapping("/age-thresholds/{species}")
    public ResponseEntity<Map<String, Integer>> updateSpeciesAgeThreshold(
            @Parameter(description = "Pet species", required = true, example = "Dog")
            @PathVariable String species,
            
            @Parameter(description = "New age threshold in years", required = true, example = "8")
            @RequestParam @Min(1) Integer threshold) {
        
        logger.debug("Updating age threshold for species {} to {}", species, threshold);
        
        try {
            seniorPetService.updateSpeciesAgeThreshold(species, threshold);
            Map<String, Integer> updatedThresholds = seniorPetService.getSpeciesAgeThresholds();
            
            logger.info("Updated age threshold for {} to {} years", species, threshold);
            return ResponseEntity.ok(updatedThresholds);
            
        } catch (Exception e) {
            logger.error("Error updating age threshold for species: {}", species, e);
            return ResponseEntity.ok(seniorPetService.getSpeciesAgeThresholds());
        }
    }

    // ========================================
    // Statistics and Metadata Endpoints
    // ========================================

    /**
     * Get senior pet statistics
     * GET /api/pets/senior/statistics
     * 
     * @return Statistics about senior pets
     */
    @Operation(
        summary = "Get senior pet statistics",
        description = "Retrieves comprehensive statistics about senior pets including counts by species and percentages.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSeniorPetStatistics() {
        logger.debug("Getting senior pet statistics");
        
        try {
            Map<String, Object> statistics = seniorPetService.getSeniorPetStatistics();
            logger.debug("Retrieved senior pet statistics: {}", statistics);
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting senior pet statistics", e);
            return ResponseEntity.ok(Map.of(
                "totalSeniorPets", 0,
                "totalPets", 0,
                "seniorPercentage", 0.0,
                "error", "Statistics temporarily unavailable"
            ));
        }
    }

    /**
     * Get available health conditions for filtering
     * GET /api/pets/senior/health-conditions
     * 
     * @return List of health conditions found in senior pets
     */
    @Operation(
        summary = "Get available health conditions",
        description = "Retrieves a list of health conditions found in senior pet medical histories for filtering purposes.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/health-conditions")
    public ResponseEntity<List<String>> getAvailableHealthConditions() {
        logger.debug("Getting available health conditions");
        
        try {
            List<String> conditions = seniorPetService.getAvailableHealthConditions();
            logger.debug("Found {} health conditions", conditions.size());
            return ResponseEntity.ok(conditions);
            
        } catch (Exception e) {
            logger.error("Error getting available health conditions", e);
            return ResponseEntity.ok(List.of("Arthritis", "Diabetes", "Heart Disease", "Kidney Disease"));
        }
    }

    /**
     * Get available species with senior pets
     * GET /api/pets/senior/species
     * 
     * @return List of species that have senior pets
     */
    @Operation(
        summary = "Get available species",
        description = "Retrieves a list of species that have senior pets for filtering purposes.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/species")
    public ResponseEntity<List<String>> getAvailableSpecies() {
        logger.debug("Getting available species with senior pets");
        
        try {
            List<String> species = seniorPetService.getAvailableSpecies();
            logger.debug("Found {} species with senior pets", species.size());
            return ResponseEntity.ok(species);
            
        } catch (Exception e) {
            logger.error("Error getting available species", e);
            return ResponseEntity.ok(List.of("Dog", "Cat", "Bird", "Rabbit"));
        }
    }

    // ========================================
    // Utility Endpoints
    // ========================================

    /**
     * Check if a specific pet is considered senior
     * GET /api/pets/senior/check/{petId}
     * 
     * @param petId Pet ID to check
     * @return Boolean indicating if pet is senior
     */
    @Operation(
        summary = "Check if pet is senior",
        description = "Checks if a specific pet is considered senior based on species-specific age thresholds.",
        tags = {"Senior Pet Management"}
    )
    @GetMapping("/check/{petId}")
    public ResponseEntity<Map<String, Object>> checkIfPetIsSenior(
            @Parameter(description = "Pet ID to check", required = true, example = "1")
            @PathVariable Long petId) {
        
        logger.debug("Checking if pet {} is senior", petId);
        
        try {
            boolean isSenior = seniorPetService.isPetSenior(petId);
            Map<String, Object> result = Map.of(
                "petId", petId,
                "isSenior", isSenior
            );
            
            logger.debug("Pet {} is senior: {}", petId, isSenior);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error checking if pet {} is senior", petId, e);
            return ResponseEntity.ok(Map.of(
                "petId", petId,
                "isSenior", false,
                "error", "Unable to determine senior status"
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/pets/senior/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "SeniorPetController",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}