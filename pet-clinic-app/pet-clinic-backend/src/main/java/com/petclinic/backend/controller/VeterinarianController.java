package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.VeterinarianService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Veterinarian management
 * Provides comprehensive CRUD operations, availability tracking, and specialty-based filtering
 * Implements proper error handling, validation, and integration with VeterinarianService
 * 
 * API Endpoints:
 * - GET /api/veterinarians - List all veterinarians
 * - POST /api/veterinarians - Create new veterinarian
 * - GET /api/veterinarians/{id} - Get veterinarian by ID
 * - PUT /api/veterinarians/{id} - Update veterinarian
 * - DELETE /api/veterinarians/{id} - Delete veterinarian
 * - GET /api/veterinarians/specialty/{specialty} - Get veterinarians by specialty
 * - GET /api/veterinarians/available - Get available veterinarians
 * - GET /api/veterinarians/search - Search veterinarians
 * 
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@RestController
@RequestMapping("/api/veterinarians")
@CrossOrigin(origins = "*")
@Validated
@Tag(name = "Veterinarian Management", description = "Comprehensive veterinarian management operations including CRUD, availability tracking, and specialty-based filtering")
@SecurityRequirement(name = "bearerAuth")
public class VeterinarianController {

    private static final Logger logger = LoggerFactory.getLogger(VeterinarianController.class);

    @Autowired
    private VeterinarianService veterinarianService;

    // ========================================
    // CRUD Operations
    // ========================================

    /**
     * Get all veterinarians with pagination
     * GET /api/veterinarians?page=0&size=10&sort=lastName,asc
     * 
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of all veterinarians
     */
    @GetMapping
    public ResponseEntity<PagedResponse<Veterinarian>> getAllVeterinarians(Pageable pageable) {
        logger.debug("Getting all veterinarians with pagination: {}", pageable);
        
        // For now, get all veterinarians and create a page manually
        // In a real implementation, you'd want to add pagination to the service layer
        List<Veterinarian> allVeterinarians = veterinarianService.findAll();
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allVeterinarians.size());
        List<Veterinarian> pageContent = allVeterinarians.subList(start, end);
        
        Page<Veterinarian> veterinarians = new org.springframework.data.domain.PageImpl<>(
            pageContent, 
            pageable, 
            allVeterinarians.size()
        );
        
        PagedResponse<Veterinarian> response = new PagedResponse<>(veterinarians);
        
        logger.debug("Retrieved {} veterinarians", veterinarians.getTotalElements());
        return ResponseEntity.ok(response);
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