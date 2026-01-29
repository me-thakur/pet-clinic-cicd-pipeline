package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.service.PetService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(origins = "*")
@Validated
@Tag(name = "Pet Management", description = "Comprehensive pet management operations including CRUD, search, and filtering")
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private static final Logger logger = LoggerFactory.getLogger(PetController.class);

    @Autowired
    private PetService petService;

    // ========================================
    // CRUD Operations
    // ========================================

    /**
     * Get all pets with pagination and sorting
     * GET /api/pets?page=0&size=10&sort=name,asc
     * 
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of pets
     */
    @Operation(
        summary = "Get all pets with pagination",
        description = "Retrieves a paginated list of all pets in the system with optional sorting. " +
                     "Supports sorting by name, species, breed, and birth date.",
        tags = {"Pet Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved pets",
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
        logger.debug("Getting all pets with pagination: {}", pageable);
        
        // Use advanced search method for pagination
        Page<Pet> pets = petService.searchPetsAdvanced(
            null, null, null, null, 
            pageable.getPageNumber(), 
            pageable.getPageSize()
        );
        
        PagedResponse<Pet> response = new PagedResponse<>(pets);
        
        logger.debug("Retrieved {} pets", pets.getTotalElements());
        return ResponseEntity.ok(response);
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
            @Valid @RequestBody Pet pet) {
        logger.debug("Creating new pet: {}", pet.getName());
        
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
     * @param pet Updated pet data
     * @return Updated pet or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(@PathVariable Long id, @Valid @RequestBody Pet pet) {
        logger.debug("Updating pet with ID: {}", id);
        
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
}