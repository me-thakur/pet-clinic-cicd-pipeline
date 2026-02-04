package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.PetWithOwnerInfo;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.service.PetService;
import com.petclinic.backend.service.EnhancedPetService;
import com.petclinic.backend.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Pet management
 * Provides comprehensive CRUD operations, search functionality, and filtering for pets
 * Implements proper error handling, validation, and integration with PetService
 * 
 * API Endpoints:
 * - GET /api/pets - List all pets with pagination
 * - POST /api/pets - Create new pet
 * - GET /api/pets/{id} - Get pet by ID
 * - PUT /api/pets/{id} - Update pet
 * - DELETE /api/pets/{id} - Delete pet
 * - GET /api/pets/search - Search pets by criteria
 * - GET /api/owners/{id}/pets - Get pets by owner
 * - GET /api/pets/species/{species} - Get pets by species
 * - GET /api/pets/breed/{breed} - Get pets by breed
 * - GET /api/pets/age-range - Get pets by age range
 * 
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4
 */
@RestController
@RequestMapping("/api/pets")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
@Tag(name = "Pet Management", description = "Comprehensive pet management operations including CRUD, search, and filtering")
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private static final Logger logger = LoggerFactory.getLogger(PetController.class);

    @Autowired
    private PetService petService;
    
    @Autowired
    private EnhancedPetService enhancedPetService;
    
    @Autowired
    private SecurityAuditService securityAuditService;

    // ========================================
    // CRUD Operations
    // ========================================

    /**
     * Get all pets with server-side pagination and sorting
     * GET /api/pets?page=0&size=10&sort=name,asc
     * 
     * Supports sorting by: id, name, species, breed, birthDate, owner.lastName
     * Default sort: name ascending
     * 
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of pets with server-side sorting applied
     */
    @Operation(
        summary = "Get all pets with server-side pagination and sorting",
        description = "Retrieves a paginated list of all pets in the system with server-side sorting applied. " +
                     "Supports sorting by name, species, breed, birthDate, and owner.lastName. " +
                     "Sorting is performed at the database level for optimal performance.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved pets with server-side sorting",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content
        )
    })
    @GetMapping
    public ResponseEntity<PagedResponse<Pet>> getAllPets(Pageable pageable) {
        logger.debug("Getting all pets with server-side pagination and sorting: {}", pageable);
        
        try {
            // Apply default sorting if none specified
            if (pageable.getSort().isUnsorted()) {
                pageable = PageRequest.of(
                    pageable.getPageNumber(), 
                    pageable.getPageSize(), 
                    Sort.by(Sort.Direction.ASC, "name")
                );
            }
            
            // Use server-side sorting and pagination - sorting is applied at database level
            Page<Pet> pets = petService.findAllWithPagination(pageable);
            PagedResponse<Pet> response = new PagedResponse<>(pets);
            
            logger.debug("Retrieved {} pets with server-side sorting", pets.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting pets with server-side sorting: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pet by ID
     * GET /api/pets/{id}
     * 
     * @param id Pet ID
     * @return Pet details or 404 if not found
     */
    @Operation(
        summary = "Get pet by ID",
        description = "Retrieves detailed information about a specific pet including owner details and visit history.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pet found and returned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Pet.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pet not found with the specified ID",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Pet> getPetById(
            @Parameter(description = "Unique identifier of the pet", required = true, example = "1")
            @PathVariable Long id) {
        logger.debug("Getting pet by ID: {}", id);
        
        Optional<Pet> pet = petService.findById(id);
        if (pet.isPresent()) {
            logger.debug("Found pet: {}", pet.get().getName());
            return ResponseEntity.ok(pet.get());
        } else {
            logger.debug("Pet not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new pet
     * POST /api/pets
     * 
     * @param pet Pet data to create
     * @return Created pet with 201 status
     */
    @Operation(
        summary = "Create a new pet",
        description = "Creates a new pet record in the system. Requires valid pet data including name, species, and owner association.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Pet created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Pet.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pet data provided",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error - Pet data does not meet business rules",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<Pet> createPet(
            @Parameter(description = "Pet data to create", required = true)
            @RequestBody Map<String, Object> petData) {
        logger.debug("Creating new pet with data: {}", petData);
        
        // Convert Map to Pet object
        Pet pet = new Pet();
        
        // Set basic fields
        if (petData.containsKey("name")) {
            pet.setName(petData.get("name").toString());
        }
        
        if (petData.containsKey("species")) {
            pet.setSpecies(petData.get("species").toString());
        }
        
        if (petData.containsKey("breed")) {
            pet.setBreed(petData.get("breed").toString());
        }
        
        if (petData.containsKey("birthDate")) {
            String birthDateStr = petData.get("birthDate").toString();
            try {
                pet.setBirthDate(java.time.LocalDate.parse(birthDateStr));
            } catch (Exception e) {
                logger.warn("Invalid birth date format: {}", birthDateStr);
            }
        }
        
        if (petData.containsKey("medicalHistory")) {
            pet.setMedicalHistory(petData.get("medicalHistory").toString());
        }
        
        // Set owner
        if (petData.containsKey("owner") && petData.get("owner") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ownerData = (Map<String, Object>) petData.get("owner");
            if (ownerData.containsKey("id")) {
                com.petclinic.backend.model.Owner owner = new com.petclinic.backend.model.Owner();
                owner.setId(Long.valueOf(ownerData.get("id").toString()));
                pet.setOwner(owner);
            }
        }
        
        // Ensure ID is null for new entities
        pet.setId(null);
        Pet createdPet = petService.create(pet);
        
        logger.info("Created pet with ID: {} and name: {}", createdPet.getId(), createdPet.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPet);
    }

    /**
     * Update existing pet
     * PUT /api/pets/{id}
     * 
     * @param id Pet ID to update
     * @param petData Updated pet data
     * @return Updated pet or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(@PathVariable Long id, @RequestBody Map<String, Object> petData) {
        logger.debug("Updating pet with ID: {} with data: {}", id, petData);
        
        // Convert Map to Pet object
        Pet pet = new Pet();
        pet.setId(id); // Ensure the ID is set for update
        
        // Set basic fields
        if (petData.containsKey("name")) {
            pet.setName(petData.get("name").toString());
        }
        
        if (petData.containsKey("species")) {
            pet.setSpecies(petData.get("species").toString());
        }
        
        if (petData.containsKey("breed")) {
            pet.setBreed(petData.get("breed").toString());
        }
        
        if (petData.containsKey("birthDate")) {
            String birthDateStr = petData.get("birthDate").toString();
            try {
                pet.setBirthDate(java.time.LocalDate.parse(birthDateStr));
            } catch (Exception e) {
                logger.warn("Invalid birth date format: {}", birthDateStr);
            }
        }
        
        if (petData.containsKey("medicalHistory")) {
            pet.setMedicalHistory(petData.get("medicalHistory").toString());
        }
        
        // Set owner
        if (petData.containsKey("owner") && petData.get("owner") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ownerData = (Map<String, Object>) petData.get("owner");
            if (ownerData.containsKey("id")) {
                com.petclinic.backend.model.Owner owner = new com.petclinic.backend.model.Owner();
                owner.setId(Long.valueOf(ownerData.get("id").toString()));
                pet.setOwner(owner);
            }
        }
        
        Pet updatedPet = petService.update(id, pet);
        logger.info("Updated pet with ID: {} and name: {}", updatedPet.getId(), updatedPet.getName());
        return ResponseEntity.ok(updatedPet);
    }

    /**
     * Delete pet
     * DELETE /api/pets/{id}
     * 
     * @param id Pet ID to delete
     * @return 204 No Content if successful, 404 if not found, 422 if has visits
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        logger.debug("Deleting pet with ID: {}", id);
        
        petService.deleteById(id);
        logger.info("Deleted pet with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk delete pets
     * DELETE /api/pets/bulk
     * 
     * @param request Bulk delete request containing pet IDs
     * @return Bulk operation result
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> bulkDeletePets(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            // Validate user can perform bulk operations
            if (!securityAuditService.canPerformBulkOperation(authentication, "pets", "DELETE")) {
                securityAuditService.logAccessDenied("BULK_DELETE", "pets", authentication, 
                    "User does not have permission for bulk delete operations");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: Insufficient permissions for bulk delete operations", "success", false));
            }
            
            logger.debug("Bulk delete pets request: {}", request);
            
            // Extract IDs from request
            @SuppressWarnings("unchecked")
            List<Object> idObjects = (List<Object>) request.get("ids");
            if (idObjects == null || idObjects.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No pet IDs provided", "success", false));
            }
            
            List<Long> ids = idObjects.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            
            // Log the bulk delete operation attempt
            Map<String, Object> auditContext = new HashMap<>();
            auditContext.put("totalRequested", ids.size());
            auditContext.put("requestedIds", ids);
            
            securityAuditService.logSecurityOperation("BULK_DELETE_ATTEMPT", "pets", ids, authentication, auditContext);
            
            int totalRequested = ids.size();
            int deletedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long id : ids) {
                try {
                    // Check if pet exists
                    if (!petService.existsById(id)) {
                        errors.add("Pet with ID " + id + " not found");
                        continue;
                    }
                    
                    // Check if pet can be deleted (no associated visits)
                    if (!petService.canDeletePet(id)) {
                        errors.add("Cannot delete pet with ID " + id + " - has associated visits");
                        continue;
                    }
                    
                    // Delete the pet
                    petService.deleteById(id);
                    deletedCount++;
                    
                    logger.debug("Successfully deleted pet with ID: {}", id);
                    
                } catch (Exception e) {
                    String errorMsg = securityAuditService.sanitizeErrorMessage(e.getMessage(), authentication);
                    logger.error("Error deleting pet with ID {}: {}", id, e.getMessage(), e);
                    errors.add("Failed to delete pet with ID " + id + ": " + errorMsg);
                }
            }
            
            // Log the completion of bulk delete operation
            Map<String, Object> completionContext = new HashMap<>();
            completionContext.put("totalRequested", totalRequested);
            completionContext.put("deletedCount", deletedCount);
            completionContext.put("failedCount", totalRequested - deletedCount);
            completionContext.put("hasErrors", !errors.isEmpty());
            
            securityAuditService.logSecurityOperation("BULK_DELETE_COMPLETED", "pets", ids, authentication, completionContext);
            
            boolean success = deletedCount > 0;
            Map<String, Object> result = Map.of(
                "success", success,
                "deletedCount", deletedCount,
                "totalRequested", totalRequested,
                "failedCount", totalRequested - deletedCount,
                "errors", errors,
                "message", success ? 
                    "Successfully deleted " + deletedCount + " of " + totalRequested + " pets" :
                    "Failed to delete any pets"
            );
            
            logger.info("Bulk delete pets result: deleted {}/{} pets", deletedCount, totalRequested);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("General error in bulk delete pets: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Bulk delete failed", "details", e.getMessage(), "success", false));
        }
    }

    // ========================================
    // Search and Filtering Endpoints
    // ========================================

    /**
     * Search pets by multiple criteria
     * GET /api/pets/search?q=searchTerm&name=petName&species=dog&breed=labrador&ownerId=1&page=0&size=10
     * 
     * @param q General search term (searches name, species, breed, owner name)
     * @param name Pet name filter
     * @param species Pet species filter
     * @param breed Pet breed filter
     * @param ownerId Owner ID filter
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<Pet>> searchPets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.debug("Searching pets with criteria - q: {}, name: {}, species: {}, breed: {}, ownerId: {}", 
                    q, name, species, breed, ownerId);
        
        Page<Pet> results;
        
        if (q != null && !q.trim().isEmpty()) {
            // General search using service method
            List<Pet> searchResults = petService.searchPets(q);
            // Convert to page (simplified pagination for search results)
            int start = page * size;
            int end = Math.min(start + size, searchResults.size());
            List<Pet> pageContent = searchResults.subList(start, end);
            results = new org.springframework.data.domain.PageImpl<>(
                pageContent, 
                PageRequest.of(page, size), 
                searchResults.size()
            );
        } else {
            // Advanced search with specific criteria
            results = petService.searchPetsAdvanced(name, species, breed, ownerId, page, size);
        }
        
        PagedResponse<Pet> response = new PagedResponse<>(results);
        
        logger.debug("Found {} pets matching search criteria", results.getTotalElements());
        return ResponseEntity.ok(response);
    }

    /**
     * Get pets by species
     * GET /api/pets/species/{species}
     * 
     * @param species Pet species
     * @return List of pets of the specified species
     */
    @GetMapping("/species/{species}")
    public ResponseEntity<List<Pet>> getPetsBySpecies(@PathVariable @NotBlank String species) {
        logger.debug("Getting pets by species: {}", species);
        
        List<Pet> pets = petService.findBySpecies(species);
        logger.debug("Found {} pets of species: {}", pets.size(), species);
        return ResponseEntity.ok(pets);
    }

    /**
     * Get pets by breed
     * GET /api/pets/breed/{breed}
     * 
     * @param breed Pet breed
     * @return List of pets of the specified breed
     */
    @GetMapping("/breed/{breed}")
    public ResponseEntity<List<Pet>> getPetsByBreed(@PathVariable @NotBlank String breed) {
        logger.debug("Getting pets by breed: {}", breed);
        
        List<Pet> pets = petService.findByBreed(breed);
        logger.debug("Found {} pets of breed: {}", pets.size(), breed);
        return ResponseEntity.ok(pets);
    }

    /**
     * Get pets by age range
     * GET /api/pets/age-range?minAge=1&maxAge=5
     * 
     * @param minAge Minimum age in years
     * @param maxAge Maximum age in years
     * @return List of pets within the age range
     */
    @GetMapping("/age-range")
    public ResponseEntity<List<Pet>> getPetsByAgeRange(
            @RequestParam @Min(0) int minAge,
            @RequestParam @Min(0) int maxAge) {
        
        logger.debug("Getting pets by age range: {} to {}", minAge, maxAge);
        
        List<Pet> pets = petService.findByAgeRange(minAge, maxAge);
        logger.debug("Found {} pets in age range {} to {}", pets.size(), minAge, maxAge);
        return ResponseEntity.ok(pets);
    }

    // ========================================
    // Owner-Related Endpoints
    // ========================================

    /**
     * Get pets by owner ID
     * GET /api/owners/{id}/pets
     * 
     * @param id Owner ID
     * @return List of pets belonging to the owner
     */
    @GetMapping("/owners/{id}/pets")
    public ResponseEntity<List<Pet>> getPetsByOwner(@PathVariable Long id) {
        logger.debug("Getting pets by owner ID: {}", id);
        
        List<Pet> pets = petService.findByOwner(id);
        logger.debug("Found {} pets for owner ID: {}", pets.size(), id);
        return ResponseEntity.ok(pets);
    }

    // Alternative endpoint for consistency with other patterns
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Pet>> getPetsByOwnerId(@PathVariable Long ownerId) {
        return getPetsByOwner(ownerId);
    }

    // ========================================
    // Specialized Search Endpoints
    // ========================================

    /**
     * Get young pets (less than 2 years old)
     * GET /api/pets/young
     * 
     * @return List of young pets
     */
    @GetMapping("/young")
    public ResponseEntity<List<Pet>> getYoungPets(@RequestParam(defaultValue = "2") int age) {
        logger.debug("Getting young pets younger than {} years", age);
        
        List<Pet> pets = petService.findYoungPets(age);
        logger.debug("Found {} young pets", pets.size());
        return ResponseEntity.ok(pets);
    }

    /**
     * Get senior pets (more than 7 years old)
     * GET /api/pets/senior?age=7
     * 
     * @param age Minimum age to be considered senior (default: 7)
     * @return List of senior pets
     */
    @GetMapping("/senior")
    public ResponseEntity<List<Pet>> getSeniorPets(@RequestParam(defaultValue = "7") @Min(1) int age) {
        logger.debug("Getting senior pets older than {} years", age);
        
        List<Pet> pets = petService.findSeniorPets(age);
        logger.debug("Found {} senior pets", pets.size());
        return ResponseEntity.ok(pets);
    }

    /**
     * Get pets with upcoming visits
     * GET /api/pets/upcoming-visits
     * 
     * @return List of pets with scheduled visits
     */
    @GetMapping("/upcoming-visits")
    public ResponseEntity<List<Pet>> getPetsWithUpcomingVisits() {
        logger.debug("Getting pets with upcoming visits");
        
        List<Pet> pets = petService.findPetsWithUpcomingVisits();
        logger.debug("Found {} pets with upcoming visits", pets.size());
        return ResponseEntity.ok(pets);
    }

    /**
     * Get pets without any visits
     * GET /api/pets/no-visits
     * 
     * @return List of pets without visits
     */
    @GetMapping("/no-visits")
    public ResponseEntity<List<Pet>> getPetsWithoutVisits() {
        logger.debug("Getting pets without visits");
        
        List<Pet> pets = petService.findPetsWithoutVisits();
        logger.debug("Found {} pets without visits", pets.size());
        return ResponseEntity.ok(pets);
    }

    /**
     * Search pets by medical history
     * GET /api/pets/medical-history?keywords=allergy
     * 
     * @param keywords Keywords to search in medical history
     * @return List of pets with matching medical history
     */
    @GetMapping("/medical-history")
    public ResponseEntity<List<Pet>> getPetsByMedicalHistory(@RequestParam @NotBlank String keywords) {
        logger.debug("Searching pets by medical history keywords: {}", keywords);
        
        List<Pet> pets = petService.findByMedicalHistory(keywords);
        logger.debug("Found {} pets with medical history containing: {}", pets.size(), keywords);
        return ResponseEntity.ok(pets);
    }

    // ========================================
    // Statistics and Analytics Endpoints
    // ========================================

    /**
     * Get pet statistics
     * GET /api/pets/statistics
     * 
     * @return Statistics array [totalPets, totalSpecies, totalOwners]
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getPetStatistics() {
        logger.debug("Getting pet statistics");
        
        Map<String, Object> statisticsMap = petService.getPetStatistics();
        
        logger.debug("Pet statistics: {}", statisticsMap);
        return ResponseEntity.ok(statisticsMap);
    }

    /**
     * Get count of pets by species
     * GET /api/pets/species-count
     * 
     * @return List of species counts
     */
    @GetMapping("/species-count")
    public ResponseEntity<List<Map<String, Object>>> getPetsBySpeciesCount() {
        logger.debug("Getting pets count by species");
        
        Map<String, Long> speciesCount = petService.getPetCountBySpecies();
        
        List<Map<String, Object>> result = speciesCount.entrySet().stream()
            .map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("species", entry.getKey());
                map.put("count", entry.getValue());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Species count: {}", result);
        return ResponseEntity.ok(result);
    }

    // ========================================
    // Utility Endpoints
    // ========================================

    /**
     * Check if pet can be deleted
     * GET /api/pets/{id}/can-delete
     * 
     * @param id Pet ID
     * @return Boolean indicating if pet can be deleted
     */
    @GetMapping("/{id}/can-delete")
    public ResponseEntity<Map<String, Boolean>> canDeletePet(@PathVariable Long id) {
        logger.debug("Checking if pet can be deleted: {}", id);
        
        boolean canDelete = petService.canDeletePet(id);
        Map<String, Boolean> result = Map.of("canDelete", canDelete);
        
        logger.debug("Pet {} can be deleted: {}", id, canDelete);
        return ResponseEntity.ok(result);
    }

    /**
     * Get total count of pets
     * GET /api/pets/count
     * 
     * @return Total number of pets
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPetCount() {
        logger.debug("Getting total pet count");
        
        long count = petService.count();
        Map<String, Long> result = Map.of("count", count);
        
        logger.debug("Total pet count: {}", count);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     * GET /api/pets/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "PetController",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    // ========================================
    // Enhanced Endpoints with Owner Information
    // ========================================

    /**
     * Get all pets with owner information using pagination
     * GET /api/pets/with-owner-info?page=0&size=10
     * 
     * @param pageable Pagination parameters
     * @return Paginated list of pets with owner information
     */
    @Operation(
        summary = "Get all pets with owner information",
        description = "Retrieves a paginated list of all pets with their complete owner information. " +
                     "This endpoint provides comprehensive data for displaying pets table with owner details.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved pets with owner information",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content
        )
    })
    @GetMapping("/with-owner-info")
    public ResponseEntity<PagedResponse<PetWithOwnerInfo>> getPetsWithOwnerInfo(Pageable pageable) {
        logger.debug("Getting pets with owner info, page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<PetWithOwnerInfo> pets = enhancedPetService.findPetsWithOwnerInfo(pageable);
            PagedResponse<PetWithOwnerInfo> response = new PagedResponse<>(pets);
            
            logger.debug("Retrieved {} pets with owner info", pets.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving pets with owner info", e);
            // Fallback to regular pets endpoint without owner info
            logger.warn("Falling back to regular pets endpoint due to error");
            Page<Pet> regularPets = petService.searchPetsAdvanced(
                null, null, null, null, 
                pageable.getPageNumber(), 
                pageable.getPageSize()
            );
            
            // Convert Pet to PetWithOwnerInfo with placeholder owner data
            List<PetWithOwnerInfo> fallbackPets = regularPets.getContent().stream()
                .map(this::convertPetToPetWithOwnerInfo)
                .collect(java.util.stream.Collectors.toList());
            
            Page<PetWithOwnerInfo> fallbackPage = new PageImpl<>(
                fallbackPets, pageable, regularPets.getTotalElements()
            );
            
            PagedResponse<PetWithOwnerInfo> response = new PagedResponse<>(fallbackPage);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get pet with owner information by ID
     * GET /api/pets/{id}/with-owner-info
     * 
     * @param id Pet ID
     * @return Pet with owner information or 404 if not found
     */
    @Operation(
        summary = "Get pet with owner information by ID",
        description = "Retrieves detailed information about a specific pet including complete owner details.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pet with owner information found and returned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PetWithOwnerInfo.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pet not found with the specified ID",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        )
    })
    @GetMapping("/{id}/with-owner-info")
    public ResponseEntity<PetWithOwnerInfo> getPetWithOwnerInfoById(
            @Parameter(description = "Unique identifier of the pet", required = true, example = "1")
            @PathVariable Long id) {
        logger.debug("Getting pet with owner info by ID: {}", id);
        
        try {
            Optional<PetWithOwnerInfo> petWithOwnerInfo = enhancedPetService.findPetWithOwnerInfo(id);
            if (petWithOwnerInfo.isPresent()) {
                logger.debug("Found pet with owner info: {}", petWithOwnerInfo.get().getPetName());
                return ResponseEntity.ok(petWithOwnerInfo.get());
            } else {
                logger.debug("Pet not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving pet with owner info for ID: {}", id, e);
            // Fallback to regular pet endpoint
            Optional<Pet> pet = petService.findById(id);
            if (pet.isPresent()) {
                PetWithOwnerInfo fallbackPet = convertPetToPetWithOwnerInfo(pet.get());
                return ResponseEntity.ok(fallbackPet);
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }

    /**
     * Search pets with owner information
     * GET /api/pets/search/with-owner-info?q=searchTerm&page=0&size=10
     * 
     * @param q Search term to match against pet name, species, breed, or owner name
     * @param pageable Pagination parameters
     * @return Paginated search results with owner information
     */
    @Operation(
        summary = "Search pets with owner information",
        description = "Searches pets by various criteria and returns results with complete owner information. " +
                     "Searches across pet name, species, breed, and owner name.",
        tags = {"Pet Management"}
    )
    @GetMapping("/search/with-owner-info")
    public ResponseEntity<PagedResponse<PetWithOwnerInfo>> searchPetsWithOwnerInfo(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        
        logger.debug("Searching pets with owner info for term: '{}', page: {}, size: {}", 
                    q, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<PetWithOwnerInfo> results = enhancedPetService.searchPetsWithOwnerInfo(q, pageable);
            PagedResponse<PetWithOwnerInfo> response = new PagedResponse<>(results);
            
            logger.debug("Found {} pets with owner info matching search term '{}'", 
                        results.getTotalElements(), q);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching pets with owner info for term: {}", q, e);
            // Fallback to regular search
            List<Pet> searchResults = petService.searchPets(q != null ? q : "");
            int start = pageable.getPageNumber() * pageable.getPageSize();
            int end = Math.min(start + pageable.getPageSize(), searchResults.size());
            List<Pet> pageContent = searchResults.subList(start, end);
            
            List<PetWithOwnerInfo> fallbackResults = pageContent.stream()
                .map(this::convertPetToPetWithOwnerInfo)
                .collect(java.util.stream.Collectors.toList());
            
            Page<PetWithOwnerInfo> fallbackPage = new PageImpl<>(
                fallbackResults, pageable, searchResults.size()
            );
            
            PagedResponse<PetWithOwnerInfo> response = new PagedResponse<>(fallbackPage);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get pets with owner information by owner ID
     * GET /api/pets/owner/{ownerId}/with-owner-info
     * 
     * @param ownerId Owner ID
     * @return List of pets with owner information for the specified owner
     */
    @Operation(
        summary = "Get pets with owner information by owner ID",
        description = "Retrieves all pets belonging to a specific owner with complete owner information.",
        tags = {"Pet Management"}
    )
    @GetMapping("/owner/{ownerId}/with-owner-info")
    public ResponseEntity<List<PetWithOwnerInfo>> getPetsWithOwnerInfoByOwner(
            @Parameter(description = "Unique identifier of the owner", required = true, example = "1")
            @PathVariable Long ownerId) {
        logger.debug("Getting pets with owner info for owner ID: {}", ownerId);
        
        try {
            List<PetWithOwnerInfo> pets = enhancedPetService.findPetsWithOwnerInfoByOwner(ownerId);
            logger.debug("Found {} pets with owner info for owner ID: {}", pets.size(), ownerId);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            logger.error("Error retrieving pets with owner info for owner ID: {}", ownerId, e);
            // Fallback to regular pets by owner
            List<Pet> regularPets = petService.findByOwner(ownerId);
            List<PetWithOwnerInfo> fallbackPets = regularPets.stream()
                .map(this::convertPetToPetWithOwnerInfo)
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(fallbackPets);
        }
    }

    /**
     * Refresh pet-owner information for a specific pet
     * POST /api/pets/{id}/refresh-owner-info
     * 
     * @param id Pet ID
     * @return Updated pet with owner information
     */
    @Operation(
        summary = "Refresh pet-owner information",
        description = "Refreshes the pet-owner relationship data for immediate updates when owner information changes.",
        tags = {"Pet Management"}
    )
    @PostMapping("/{id}/refresh-owner-info")
    public ResponseEntity<PetWithOwnerInfo> refreshPetOwnerInfo(
            @Parameter(description = "Unique identifier of the pet", required = true, example = "1")
            @PathVariable Long id) {
        logger.debug("Refreshing pet-owner info for pet ID: {}", id);
        
        try {
            Optional<PetWithOwnerInfo> refreshedPet = enhancedPetService.refreshPetOwnerInfo(id);
            if (refreshedPet.isPresent()) {
                logger.debug("Refreshed pet-owner info for pet: {}", refreshedPet.get().getPetName());
                return ResponseEntity.ok(refreshedPet.get());
            } else {
                logger.debug("Pet not found for refresh with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error refreshing pet-owner info for ID: {}", id, e);
            // Fallback to regular pet lookup
            Optional<Pet> pet = petService.findById(id);
            if (pet.isPresent()) {
                PetWithOwnerInfo fallbackPet = convertPetToPetWithOwnerInfo(pet.get());
                return ResponseEntity.ok(fallbackPet);
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }

    /**
     * Get pet ownership statistics
     * GET /api/pets/ownership-statistics
     * 
     * @return Statistics about pets with and without owners
     */
    @Operation(
        summary = "Get pet ownership statistics",
        description = "Returns statistics about how many pets have owners vs how many are orphaned.",
        tags = {"Pet Management"}
    )
    @GetMapping("/ownership-statistics")
    public ResponseEntity<Map<String, Long>> getPetOwnershipStatistics() {
        logger.debug("Getting pet ownership statistics");
        
        try {
            long[] stats = enhancedPetService.getPetOwnershipStatistics();
            Map<String, Long> result = Map.of(
                "petsWithOwners", stats[0],
                "petsWithoutOwners", stats[1],
                "totalPets", stats[0] + stats[1]
            );
            
            logger.debug("Pet ownership statistics: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting pet ownership statistics", e);
            // Fallback to basic count
            long totalPets = petService.count();
            Map<String, Long> fallbackResult = Map.of(
                "petsWithOwners", totalPets, // Assume all have owners as fallback
                "petsWithoutOwners", 0L,
                "totalPets", totalPets
            );
            return ResponseEntity.ok(fallbackResult);
        }
    }

    // ========================================
    // Owner Lookup Endpoints for Pet Form
    // ========================================

    /**
     * Search owners for pet form dropdown
     * GET /api/pets/owners/search?q=searchTerm&page=0&size=10
     * 
     * @param q Search term to match against owner name, email, or phone
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Paginated list of owners with contact information
     */
    @Operation(
        summary = "Search owners for pet form",
        description = "Searches owners by name, email, or phone number for use in pet form owner selection dropdown. " +
                     "Returns owner information including contact details for confirmation.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved matching owners",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        )
    })
    @GetMapping("/owners/search")
    public ResponseEntity<PagedResponse<Map<String, Object>>> searchOwnersForPetForm(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        logger.debug("Searching owners for pet form with query: '{}', page: {}, size: {}", q, page, size);
        
        try {
            // Use owner service to search owners
            Page<com.petclinic.backend.model.Owner> owners = petService.searchOwners(q, page, size);
            
            // Convert to simplified owner info for dropdown
            List<Map<String, Object>> ownerInfoList = owners.getContent().stream()
                .map(owner -> {
                    Map<String, Object> ownerInfo = new HashMap<>();
                    ownerInfo.put("id", owner.getId());
                    ownerInfo.put("firstName", owner.getFirstName());
                    ownerInfo.put("lastName", owner.getLastName());
                    ownerInfo.put("fullName", owner.getFullName());
                    ownerInfo.put("email", owner.getEmail());
                    ownerInfo.put("mobileNumber", owner.getMobileNumber());
                    ownerInfo.put("telephone", owner.getTelephone());
                    ownerInfo.put("address", owner.getAddress());
                    ownerInfo.put("city", owner.getCity());
                    ownerInfo.put("state", owner.getState());
                    ownerInfo.put("zipCode", owner.getZipCode());
                    ownerInfo.put("petCount", owner.getPetCount());
                    return ownerInfo;
                })
                .collect(java.util.stream.Collectors.toList());
            
            Page<Map<String, Object>> ownerInfoPage = new PageImpl<>(
                ownerInfoList, 
                PageRequest.of(page, size), 
                owners.getTotalElements()
            );
            
            PagedResponse<Map<String, Object>> response = new PagedResponse<>(ownerInfoPage);
            
            logger.debug("Found {} owners matching search query '{}'", owners.getTotalElements(), q);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching owners for pet form with query: {}", q, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get owner information for existing pet
     * GET /api/pets/{id}/owner-info
     * 
     * @param id Pet ID
     * @return Owner information for the pet or 404 if pet not found
     */
    @Operation(
        summary = "Get owner information for existing pet",
        description = "Retrieves complete owner information for an existing pet to display in the pet form.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved owner information",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pet not found or pet has no owner",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authentication token",
            content = @Content
        )
    })
    @GetMapping("/{id}/owner-info")
    public ResponseEntity<Map<String, Object>> getOwnerInfoForPet(
            @Parameter(description = "Unique identifier of the pet", required = true, example = "1")
            @PathVariable Long id) {
        logger.debug("Getting owner info for pet ID: {}", id);
        
        try {
            Optional<Pet> petOpt = petService.findById(id);
            if (petOpt.isEmpty()) {
                logger.debug("Pet not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Pet pet = petOpt.get();
            if (pet.getOwner() == null) {
                logger.debug("Pet with ID {} has no owner", id);
                return ResponseEntity.notFound().build();
            }
            
            com.petclinic.backend.model.Owner owner = pet.getOwner();
            Map<String, Object> ownerInfo = new HashMap<>();
            ownerInfo.put("id", owner.getId());
            ownerInfo.put("firstName", owner.getFirstName());
            ownerInfo.put("lastName", owner.getLastName());
            ownerInfo.put("fullName", owner.getFullName());
            ownerInfo.put("email", owner.getEmail());
            ownerInfo.put("mobileNumber", owner.getMobileNumber());
            ownerInfo.put("telephone", owner.getTelephone());
            ownerInfo.put("address", owner.getAddress());
            ownerInfo.put("city", owner.getCity());
            ownerInfo.put("state", owner.getState());
            ownerInfo.put("zipCode", owner.getZipCode());
            ownerInfo.put("petCount", owner.getPetCount());
            
            logger.debug("Retrieved owner info for pet: {} - Owner: {}", pet.getName(), owner.getFullName());
            return ResponseEntity.ok(ownerInfo);
            
        } catch (Exception e) {
            logger.error("Error retrieving owner info for pet ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate owner assignment for pet
     * POST /api/pets/validate-owner-assignment
     * 
     * @param request Request containing pet ID and owner ID
     * @return Validation result with any warnings or conflicts
     */
    @Operation(
        summary = "Validate owner assignment for pet",
        description = "Validates that an owner can be assigned to a pet, checking for any business rule violations.",
        tags = {"Pet Management"}
    )
    @PostMapping("/validate-owner-assignment")
    public ResponseEntity<Map<String, Object>> validateOwnerAssignment(
            @RequestBody Map<String, Object> request) {
        
        logger.debug("Validating owner assignment: {}", request);
        
        try {
            Long petId = request.containsKey("petId") ? 
                Long.valueOf(request.get("petId").toString()) : null;
            Long ownerId = request.containsKey("ownerId") ? 
                Long.valueOf(request.get("ownerId").toString()) : null;
            
            if (ownerId == null) {
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Pet will be created without an owner assignment",
                    "warnings", List.of("Consider assigning an owner for better record keeping")
                ));
            }
            
            // Check if owner exists
            if (!petService.ownerExists(ownerId)) {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Selected owner does not exist",
                    "errors", List.of("Owner with ID " + ownerId + " not found")
                ));
            }
            
            // Get owner information for validation
            com.petclinic.backend.model.Owner owner = petService.getOwnerById(ownerId);
            List<String> warnings = new ArrayList<>();
            
            // Check if owner has many pets (warning, not error)
            if (owner.getPetCount() >= 5) {
                warnings.add("This owner already has " + owner.getPetCount() + " pets registered");
            }
            
            // For existing pets, check if owner is changing
            if (petId != null) {
                Optional<Pet> existingPet = petService.findById(petId);
                if (existingPet.isPresent() && existingPet.get().getOwner() != null) {
                    if (!existingPet.get().getOwner().getId().equals(ownerId)) {
                        warnings.add("Changing pet owner from " + 
                            existingPet.get().getOwner().getFullName() + " to " + owner.getFullName());
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("message", "Owner assignment is valid");
            result.put("ownerInfo", Map.of(
                "id", owner.getId(),
                "fullName", owner.getFullName(),
                "email", owner.getEmail(),
                "petCount", owner.getPetCount()
            ));
            
            if (!warnings.isEmpty()) {
                result.put("warnings", warnings);
            }
            
            logger.debug("Owner assignment validation result: {}", result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error validating owner assignment: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "message", "Validation failed due to system error",
                "errors", List.of("Unable to validate owner assignment: " + e.getMessage())
            ));
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Convert Pet entity to PetWithOwnerInfo DTO
     * Used as fallback when enhanced service is unavailable
     * 
     * @param pet Pet entity
     * @return PetWithOwnerInfo DTO with placeholder owner data if owner is missing
     */
    private PetWithOwnerInfo convertPetToPetWithOwnerInfo(Pet pet) {
        PetWithOwnerInfo petWithOwnerInfo = new PetWithOwnerInfo();
        
        // Set pet information
        petWithOwnerInfo.setPetId(pet.getId());
        petWithOwnerInfo.setPetName(pet.getName());
        petWithOwnerInfo.setPetSpecies(pet.getSpecies());
        petWithOwnerInfo.setPetBreed(pet.getBreed());
        petWithOwnerInfo.setBirthDate(pet.getBirthDate());
        petWithOwnerInfo.setMedicalHistory(pet.getMedicalHistory());
        petWithOwnerInfo.setCreatedAt(pet.getCreatedAt());
        petWithOwnerInfo.setUpdatedAt(pet.getUpdatedAt());
        
        // Set owner information if available
        if (pet.getOwner() != null) {
            petWithOwnerInfo.setOwnerId(pet.getOwner().getId());
            petWithOwnerInfo.setOwnerFirstName(pet.getOwner().getFirstName());
            petWithOwnerInfo.setOwnerLastName(pet.getOwner().getLastName());
            petWithOwnerInfo.setOwnerEmail(pet.getOwner().getEmail());
            petWithOwnerInfo.setOwnerMobileNumber(pet.getOwner().getMobileNumber());
            petWithOwnerInfo.setOwnerTelephone(pet.getOwner().getTelephone());
            petWithOwnerInfo.setOwnerAddress(pet.getOwner().getAddress());
            petWithOwnerInfo.setOwnerCity(pet.getOwner().getCity());
            petWithOwnerInfo.setOwnerState(pet.getOwner().getState());
            petWithOwnerInfo.setOwnerZipCode(pet.getOwner().getZipCode());
        } else {
            // Set placeholder values for pets without owners
            petWithOwnerInfo.setOwnerId(null);
            petWithOwnerInfo.setOwnerFirstName(null);
            petWithOwnerInfo.setOwnerLastName(null);
            petWithOwnerInfo.setOwnerEmail(null);
            petWithOwnerInfo.setOwnerMobileNumber(null);
            petWithOwnerInfo.setOwnerTelephone(null);
            petWithOwnerInfo.setOwnerAddress(null);
            petWithOwnerInfo.setOwnerCity(null);
            petWithOwnerInfo.setOwnerState(null);
            petWithOwnerInfo.setOwnerZipCode(null);
        }
        
        return petWithOwnerInfo;
    }
}