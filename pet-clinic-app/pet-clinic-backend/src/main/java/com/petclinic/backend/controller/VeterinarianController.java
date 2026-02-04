package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.VeterinarianFilterCriteria;
import com.petclinic.backend.dto.VeterinarianFilterResult;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.VeterinarianService;
import com.petclinic.backend.service.VeterinarianFilterService;
import com.petclinic.backend.service.SecurityAuditService;
import com.petclinic.backend.service.impl.VeterinarianServiceImpl;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Veterinarian management
 * Provides comprehensive CRUD operations, availability tracking, and specialty-based filtering
 * Implements server-side sorting and pagination for optimal performance
 * 
 * API Endpoints:
 * - GET /api/veterinarians - List all veterinarians with server-side sorting
 * - POST /api/veterinarians - Create new veterinarian
 * - GET /api/veterinarians/{id} - Get veterinarian by ID
 * - PUT /api/veterinarians/{id} - Update veterinarian
 * - DELETE /api/veterinarians/{id} - Delete veterinarian
 * - GET /api/veterinarians/specialty/{specialty} - Get veterinarians by specialty
 * - GET /api/veterinarians/available - Get available veterinarians
 * - GET /api/veterinarians/search - Search veterinarians
 * 
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 */
@RestController
@RequestMapping("/api/veterinarians")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
@Tag(name = "Veterinarian Management", description = "Comprehensive veterinarian management operations including CRUD, availability tracking, and specialty-based filtering")
@SecurityRequirement(name = "bearerAuth")
public class VeterinarianController {

    private static final Logger logger = LoggerFactory.getLogger(VeterinarianController.class);

    @Autowired
    private VeterinarianService veterinarianService;
    
    @Autowired
    private VeterinarianFilterService veterinarianFilterService;
    
    @Autowired
    private SecurityAuditService securityAuditService;

    // ========================================
    // Enhanced Filtering Endpoints
    // ========================================

    /**
     * Filter veterinarians with comprehensive criteria
     * GET /api/veterinarians/filter?specialties=Surgery,Emergency&availableOnly=true&page=0&size=10
     * 
     * @param specialties List of specialties to filter by
     * @param availableOnly Filter only available veterinarians
     * @param minExperienceYears Minimum experience in years
     * @param maxExperienceYears Maximum experience in years
     * @param activeLicenseOnly Filter only active licenses
     * @param maxVisits Maximum visits for light workload filter
     * @param emergencyCapable Filter emergency-capable veterinarians
     * @param surgicalCapable Filter surgical-capable veterinarians
     * @param searchText Text search across name and license
     * @param pageable Pagination and sorting parameters
     * @return Filtered veterinarians with metadata
     */
    @GetMapping("/filter")
    @Operation(summary = "Filter veterinarians with comprehensive criteria", 
               description = "Filter veterinarians by specialties, availability, experience, and other criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filtered veterinarians retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid filter criteria"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<VeterinarianFilterResult> filterVeterinarians(
            @RequestParam(required = false) List<String> specialties,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) Integer minExperienceYears,
            @RequestParam(required = false) Integer maxExperienceYears,
            @RequestParam(required = false) Boolean activeLicenseOnly,
            @RequestParam(required = false) Integer maxVisits,
            @RequestParam(required = false) Boolean emergencyCapable,
            @RequestParam(required = false) Boolean surgicalCapable,
            @RequestParam(required = false) String searchText,
            Pageable pageable) {
        
        logger.debug("Filtering veterinarians with criteria - specialties: {}, availableOnly: {}, searchText: {}", 
                    specialties, availableOnly, searchText);
        
        try {
            VeterinarianFilterCriteria criteria = VeterinarianFilterCriteria.builder()
                .specialties(specialties)
                .availableOnly(availableOnly)
                .minExperienceYears(minExperienceYears)
                .maxExperienceYears(maxExperienceYears)
                .activeLicenseOnly(activeLicenseOnly)
                .maxVisits(maxVisits)
                .emergencyCapable(emergencyCapable)
                .surgicalCapable(surgicalCapable)
                .searchText(searchText)
                .build();
            
            VeterinarianFilterResult result = veterinarianFilterService.filterVeterinarians(criteria, pageable);
            
            logger.debug("Filter completed - found {} veterinarians in {}ms", 
                        result.getTotalResults(), result.getExecutionTimeMs());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error filtering veterinarians: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VeterinarianFilterResult.builder()
                    .veterinarians(org.springframework.data.domain.Page.empty(pageable))
                    .suggestions(java.util.Arrays.asList("Filter temporarily unavailable. Please try again later."))
                    .build());
        }
    }

    /**
     * Get available filter options for dropdowns
     * GET /api/veterinarians/filter-options
     * 
     * @return Available filter options
     */
    @GetMapping("/filter-options")
    @Operation(summary = "Get available filter options", 
               description = "Get available options for filter dropdowns")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions() {
        logger.debug("Getting veterinarian filter options");
        
        try {
            Map<String, List<String>> options = veterinarianFilterService.getAvailableFilterOptions();
            logger.debug("Retrieved filter options: {}", options.keySet());
            return ResponseEntity.ok(options);
            
        } catch (Exception e) {
            logger.error("Error getting filter options: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Collections.emptyMap());
        }
    }

    /**
     * Get result counts for filter categories
     * GET /api/veterinarians/filter-counts?specialties=Surgery&availableOnly=true
     * 
     * @param specialties List of specialties
     * @param availableOnly Available only filter
     * @param minExperienceYears Minimum experience
     * @param maxExperienceYears Maximum experience
     * @param activeLicenseOnly Active license filter
     * @param maxVisits Maximum visits filter
     * @param emergencyCapable Emergency capable filter
     * @param surgicalCapable Surgical capable filter
     * @param searchText Search text
     * @return Result counts by category
     */
    @GetMapping("/filter-counts")
    @Operation(summary = "Get result counts for filter categories", 
               description = "Get counts for each filter category to help users understand filter impact")
    public ResponseEntity<Map<String, Long>> getFilterCounts(
            @RequestParam(required = false) List<String> specialties,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) Integer minExperienceYears,
            @RequestParam(required = false) Integer maxExperienceYears,
            @RequestParam(required = false) Boolean activeLicenseOnly,
            @RequestParam(required = false) Integer maxVisits,
            @RequestParam(required = false) Boolean emergencyCapable,
            @RequestParam(required = false) Boolean surgicalCapable,
            @RequestParam(required = false) String searchText) {
        
        logger.debug("Getting filter counts for criteria");
        
        try {
            VeterinarianFilterCriteria criteria = VeterinarianFilterCriteria.builder()
                .specialties(specialties)
                .availableOnly(availableOnly)
                .minExperienceYears(minExperienceYears)
                .maxExperienceYears(maxExperienceYears)
                .activeLicenseOnly(activeLicenseOnly)
                .maxVisits(maxVisits)
                .emergencyCapable(emergencyCapable)
                .surgicalCapable(surgicalCapable)
                .searchText(searchText)
                .build();
            
            Map<String, Long> counts = veterinarianFilterService.getResultCounts(criteria);
            logger.debug("Retrieved filter counts: {}", counts);
            return ResponseEntity.ok(counts);
            
        } catch (Exception e) {
            logger.error("Error getting filter counts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Collections.emptyMap());
        }
    }

    // ========================================
    // CRUD Operations
    // ========================================

    /**
     * Get all veterinarians with server-side pagination and sorting
     * GET /api/veterinarians?page=0&size=10&sort=lastName,asc
     * 
     * Supports sorting by: id, firstName, lastName, specialties, licenseNumber
     * Default sort: lastName ascending
     * 
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of all veterinarians with server-side sorting applied
     */
    @GetMapping
    public ResponseEntity<PagedResponse<Veterinarian>> getAllVeterinarians(Pageable pageable) {
        logger.debug("Getting all veterinarians with server-side pagination and sorting: {}", pageable);
        
        try {
            // Apply default sorting if none specified
            if (pageable.getSort().isUnsorted()) {
                pageable = PageRequest.of(
                    pageable.getPageNumber(), 
                    pageable.getPageSize(), 
                    Sort.by(Sort.Direction.ASC, "lastName")
                );
            }
            
            // Use server-side sorting and pagination - sorting is applied at database level
            Page<Veterinarian> veterinarians = veterinarianService.findAllWithPagination(pageable);
            PagedResponse<Veterinarian> response = new PagedResponse<>(veterinarians);
            
            logger.debug("Retrieved {} veterinarians with server-side sorting", veterinarians.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting veterinarians with server-side sorting: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get veterinarian by ID
     * GET /api/veterinarians/{id}
     * 
     * @param id Veterinarian ID
     * @return Veterinarian details or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Veterinarian> getVeterinarianById(@PathVariable Long id) {
        logger.debug("Getting veterinarian by ID: {}", id);
        
        Optional<Veterinarian> veterinarian = veterinarianService.findById(id);
        if (veterinarian.isPresent()) {
            logger.debug("Found veterinarian: Dr. {}", veterinarian.get().getFullName());
            return ResponseEntity.ok(veterinarian.get());
        } else {
            logger.debug("Veterinarian not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new veterinarian
     * POST /api/veterinarians
     * 
     * @param veterinarian Veterinarian data to create
     * @return Created veterinarian with 201 status
     */
    @PostMapping
    public ResponseEntity<Veterinarian> createVeterinarian(@Valid @RequestBody Veterinarian veterinarian) {
        logger.debug("Creating new veterinarian: Dr. {} {}", veterinarian.getFirstName(), veterinarian.getLastName());
        
        // Ensure ID is null for new entities
        veterinarian.setId(null);
        Veterinarian createdVeterinarian = veterinarianService.create(veterinarian);
        
        logger.info("Created veterinarian with ID: {} and license: {}", 
                   createdVeterinarian.getId(), createdVeterinarian.getLicenseNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVeterinarian);
    }

    /**
     * Update existing veterinarian
     * PUT /api/veterinarians/{id}
     * 
     * @param id Veterinarian ID to update
     * @param veterinarian Updated veterinarian data
     * @return Updated veterinarian or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Veterinarian> updateVeterinarian(@PathVariable Long id, @Valid @RequestBody Veterinarian veterinarian) {
        logger.debug("Updating veterinarian with ID: {}", id);
        
        Veterinarian updatedVeterinarian = veterinarianService.update(id, veterinarian);
        logger.info("Updated veterinarian with ID: {} and license: {}", 
                   updatedVeterinarian.getId(), updatedVeterinarian.getLicenseNumber());
        return ResponseEntity.ok(updatedVeterinarian);
    }

    /**
     * Delete veterinarian
     * DELETE /api/veterinarians/{id}
     * 
     * @param id Veterinarian ID to delete
     * @return 204 No Content if successful, 404 if not found, 422 if has visits
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeterinarian(@PathVariable Long id) {
        logger.debug("Deleting veterinarian with ID: {}", id);
        
        veterinarianService.deleteById(id);
        logger.info("Deleted veterinarian with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk delete veterinarians
     * DELETE /api/veterinarians/bulk
     * 
     * @param request Bulk delete request containing veterinarian IDs
     * @return Bulk operation result
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> bulkDeleteVeterinarians(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            logger.debug("Bulk delete veterinarians request: {}", request);
            
            // Extract IDs from request
            @SuppressWarnings("unchecked")
            List<Object> idObjects = (List<Object>) request.get("ids");
            if (idObjects == null || idObjects.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No veterinarian IDs provided", "success", false));
            }
            
            List<Long> ids = idObjects.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            
            int totalRequested = ids.size();
            int deletedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long id : ids) {
                try {
                    // Check if veterinarian exists
                    if (!veterinarianService.existsById(id)) {
                        errors.add("Veterinarian with ID " + id + " not found");
                        continue;
                    }
                    
                    // Check if veterinarian can be deleted (no associated visits)
                    if (!veterinarianService.canDeleteVeterinarian(id)) {
                        errors.add("Cannot delete veterinarian with ID " + id + " - has associated visits");
                        continue;
                    }
                    
                    // Delete the veterinarian
                    veterinarianService.deleteById(id);
                    deletedCount++;
                    
                    logger.debug("Successfully deleted veterinarian with ID: {}", id);
                    
                } catch (Exception e) {
                    logger.error("Error deleting veterinarian with ID {}: {}", id, e.getMessage(), e);
                    errors.add("Failed to delete veterinarian with ID " + id + ": " + e.getMessage());
                }
            }
            
            boolean success = deletedCount > 0;
            Map<String, Object> result = Map.of(
                "success", success,
                "deletedCount", deletedCount,
                "totalRequested", totalRequested,
                "failedCount", totalRequested - deletedCount,
                "errors", errors,
                "message", success ? 
                    "Successfully deleted " + deletedCount + " of " + totalRequested + " veterinarians" :
                    "Failed to delete any veterinarians"
            );
            
            logger.info("Bulk delete veterinarians result: deleted {}/{} veterinarians", deletedCount, totalRequested);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("General error in bulk delete veterinarians: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Bulk delete failed", "details", e.getMessage(), "success", false));
        }
    }

    // ========================================
    // Specialty-Based Filtering Endpoints
    // ========================================

    /**
     * Find veterinarians by specialty
     * GET /api/veterinarians/specialty/{specialty}
     * 
     * @param specialty Specialty name
     * @return List of veterinarians with the specified specialty
     */
    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<List<Veterinarian>> getVeterinariansBySpecialty(@PathVariable @NotBlank String specialty) {
        logger.debug("Getting veterinarians by specialty: {}", specialty);
        
        List<Veterinarian> veterinarians = veterinarianService.findBySpecialty(specialty);
        logger.debug("Found {} veterinarians with specialty: {}", veterinarians.size(), specialty);
        return ResponseEntity.ok(veterinarians);
    }

    /**
     * Find veterinarians available for specific visit type
     * GET /api/veterinarians/available-for-visit?visitType={visitType}&dateTime={dateTime}
     * 
     * @param visitType Visit type requiring specific specialties
     * @param dateTime Desired appointment time
     * @return List of available veterinarians with appropriate specialties
     */
    @GetMapping("/available-for-visit")
    public ResponseEntity<List<Veterinarian>> getVeterinariansAvailableForVisitType(
            @RequestParam VisitType visitType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        logger.debug("Getting veterinarians available for visit type {} at {}", visitType, dateTime);
        
        List<Veterinarian> veterinarians = veterinarianService.findAvailableForVisitType(visitType, dateTime);
        
        logger.debug("Found {} veterinarians available for visit type: {}", veterinarians.size(), visitType);
        return ResponseEntity.ok(veterinarians);
    }

    /**
     * Find emergency veterinarians
     * GET /api/veterinarians/emergency?dateTime={dateTime}
     * 
     * @param dateTime Emergency time
     * @return List of emergency-capable veterinarians
     */
    @GetMapping("/emergency")
    public ResponseEntity<List<Veterinarian>> getEmergencyVeterinarians(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        logger.debug("Getting emergency veterinarians at {}", dateTime);
        
        List<Veterinarian> veterinarians = veterinarianService.findEmergencyVeterinarians(dateTime);
        
        logger.debug("Found {} emergency veterinarians", veterinarians.size());
        return ResponseEntity.ok(veterinarians);
    }

    /**
     * Find surgical veterinarians
     * GET /api/veterinarians/surgical?dateTime={dateTime}
     * 
     * @param dateTime Surgery time
     * @return List of surgical veterinarians
     */
    @GetMapping("/surgical")
    public ResponseEntity<List<Veterinarian>> getSurgicalVeterinarians(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        logger.debug("Getting surgical veterinarians at {}", dateTime);
        
        List<Veterinarian> veterinarians = veterinarianService.findSurgicalVeterinarians(dateTime);
        
        logger.debug("Found {} surgical veterinarians", veterinarians.size());
        return ResponseEntity.ok(veterinarians);
    }

    // ========================================
    // Availability Tracking Endpoints
    // ========================================

    /**
     * Find available veterinarians at specific time
     * GET /api/veterinarians/available?dateTime={dateTime}
     * 
     * @param dateTime Date and time to check availability
     * @return List of available veterinarians
     */
    @GetMapping("/available")
    public ResponseEntity<List<Veterinarian>> getAvailableVeterinarians(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        logger.debug("Getting available veterinarians at {}", dateTime);
        
        List<Veterinarian> veterinarians = veterinarianService.findAvailable(dateTime);
        logger.debug("Found {} available veterinarians", veterinarians.size());
        return ResponseEntity.ok(veterinarians);
    }

    /**
     * Check if veterinarian is available at specific time
     * GET /api/veterinarians/{id}/availability?dateTime={dateTime}
     * 
     * @param id Veterinarian ID
     * @param dateTime Date and time to check
     * @return Boolean indicating availability
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<Map<String, Boolean>> checkVeterinarianAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        logger.debug("Checking availability for veterinarian {} at {}", id, dateTime);
        
        boolean available = veterinarianService.isVeterinarianAvailable(id, dateTime);
        Map<String, Boolean> result = Map.of("available", available);
        
        logger.debug("Veterinarian {} availability at {}: {}", id, dateTime, available);
        return ResponseEntity.ok(result);
    }

    /**
     * Get veterinarian workload statistics
     * GET /api/veterinarians/{id}/workload
     * 
     * @param id Veterinarian ID
     * @return Workload statistics
     */
    @GetMapping("/{id}/workload")
    public ResponseEntity<Map<String, Object>> getVeterinarianWorkload(@PathVariable Long id) {
        logger.debug("Getting workload for veterinarian {}", id);
        
        Map<String, Object> workload = veterinarianService.getVeterinarianWorkload(id);
        
        logger.debug("Retrieved workload for veterinarian {}", id);
        return ResponseEntity.ok(workload);
    }

    /**
     * Find veterinarians with light workload
     * GET /api/veterinarians/light-workload?maxVisits={maxVisits}
     * 
     * @param maxVisits Maximum number of visits to consider as light workload
     * @return List of veterinarians with light workload
     */
    @GetMapping("/light-workload")
    public ResponseEntity<List<Veterinarian>> getVeterinariansWithLightWorkload(
            @RequestParam(defaultValue = "20") @Min(0) int maxVisits) {
        
        logger.debug("Getting veterinarians with light workload (max {} visits)", maxVisits);
        
        List<Veterinarian> veterinarians = veterinarianService.findVeterinariansWithLightWorkload(maxVisits);
        
        logger.debug("Found {} veterinarians with light workload", veterinarians.size());
        return ResponseEntity.ok(veterinarians);
    }

    // ========================================
    // Search and Filtering Endpoints
    // ========================================

    /**
     * Search veterinarians by name
     * GET /api/veterinarians/search/by-name?name={name}
     * 
     * @param name Name to search for
     * @return List of matching veterinarians
     */
    @GetMapping("/search/by-name")
    public ResponseEntity<List<Veterinarian>> searchByName(@RequestParam @NotBlank String name) {
        logger.debug("Searching veterinarians by name: {}", name);
        
        List<Veterinarian> veterinarians = veterinarianService.searchByName(name);
        logger.debug("Found {} veterinarians matching name: {}", veterinarians.size(), name);
        return ResponseEntity.ok(veterinarians);
    }

    /**
     * Search veterinarians by first name (case-insensitive)
     * GET /api/veterinarians/search/by-first-name?name={name}
     * 
     * @param name First name to search for
     * @return List of matching veterinarians
     */
    @GetMapping("/search/by-first-name")
    public ResponseEntity<List<Veterinarian>> searchByFirstName(@RequestParam @NotBlank String name) {
        logger.debug("Searching veterinarians by first name: {}", name);
        
        try {
            List<Veterinarian> veterinarians = veterinarianService.searchByFirstName(name);
            logger.debug("Found {} veterinarians matching first name: {}", veterinarians.size(), name);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            logger.error("Error searching veterinarians by first name: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Find veterinarian by license number
     * GET /api/veterinarians/search/by-license?license={license}
     * 
     * @param license License number to search for
     * @return Veterinarian with the specified license number
     */
    @GetMapping("/search/by-license")
    public ResponseEntity<Veterinarian> searchByLicenseNumber(@RequestParam @NotBlank String license) {
        logger.debug("Searching veterinarian by license number: {}", license);
        
        Veterinarian veterinarian = veterinarianService.findByLicenseNumber(license);
        logger.debug("Found veterinarian with license: {}", license);
        return ResponseEntity.ok(veterinarian);
    }

    /**
     * Get most active veterinarians
     * GET /api/veterinarians/most-active?limit={limit}
     * 
     * @param limit Maximum number of results
     * @return List of most active veterinarians
     */
    @GetMapping("/most-active")
    public ResponseEntity<List<Veterinarian>> getMostActiveVeterinarians(
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        
        logger.debug("Getting most active veterinarians, limit: {}", limit);
        
        List<Veterinarian> veterinarians = veterinarianService.findMostActiveVeterinarians(limit);
        logger.debug("Found {} most active veterinarians", veterinarians.size());
        return ResponseEntity.ok(veterinarians);
    }

    // ========================================
    // Analytics and Reporting Endpoints
    // ========================================

    /**
     * Get specialty distribution
     * GET /api/veterinarians/specialty-distribution
     * 
     * @return Map of specialty to count
     */
    @GetMapping("/specialty-distribution")
    public ResponseEntity<Map<String, Long>> getSpecialtyDistribution() {
        logger.debug("Getting specialty distribution");
        
        Map<String, Long> distribution = veterinarianService.getSpecialtyDistribution();
        
        logger.debug("Retrieved specialty distribution: {}", distribution);
        return ResponseEntity.ok(distribution);
    }

    // ========================================
    // Utility Endpoints
    // ========================================

    /**
     * Check if veterinarian can be deleted
     * GET /api/veterinarians/{id}/can-delete
     * 
     * @param id Veterinarian ID
     * @return Boolean indicating if veterinarian can be deleted
     */
    @GetMapping("/{id}/can-delete")
    public ResponseEntity<Map<String, Boolean>> canDeleteVeterinarian(@PathVariable Long id) {
        logger.debug("Checking if veterinarian can be deleted: {}", id);
        
        boolean canDelete = veterinarianService.canDeleteVeterinarian(id);
        Map<String, Boolean> result = Map.of("canDelete", canDelete);
        
        logger.debug("Veterinarian {} can be deleted: {}", id, canDelete);
        return ResponseEntity.ok(result);
    }

    /**
     * Get total count of veterinarians
     * GET /api/veterinarians/count
     * 
     * @return Total number of veterinarians
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getVeterinarianCount() {
        logger.debug("Getting total veterinarian count");
        
        long count = veterinarianService.count();
        Map<String, Long> result = Map.of("count", count);
        
        logger.debug("Total veterinarian count: {}", count);
        return ResponseEntity.ok(result);
    }

    /**
     * Check if veterinarian exists
     * GET /api/veterinarians/{id}/exists
     * 
     * @param id Veterinarian ID
     * @return Boolean indicating if veterinarian exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> checkVeterinarianExists(@PathVariable Long id) {
        logger.debug("Checking if veterinarian exists: {}", id);
        
        boolean exists = veterinarianService.existsById(id);
        Map<String, Boolean> result = Map.of("exists", exists);
        
        logger.debug("Veterinarian {} exists: {}", id, exists);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint
     * GET /api/veterinarians/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "VeterinarianController",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}