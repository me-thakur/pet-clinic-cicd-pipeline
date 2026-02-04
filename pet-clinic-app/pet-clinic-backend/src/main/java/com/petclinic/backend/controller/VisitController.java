package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.VisitService;
import com.petclinic.backend.service.VisitService.VisitStatistics;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Visit management and scheduling
 * Provides comprehensive CRUD operations, scheduling functionality, and visit management
 * Implements server-side sorting and pagination for optimal performance
 * 
 * API Endpoints:
 * - GET /api/visits - List all visits with server-side sorting and pagination
 * - POST /api/visits - Create/schedule new visit
 * - GET /api/visits/{id} - Get visit by ID
 * - PUT /api/visits/{id} - Update visit
 * - DELETE /api/visits/{id} - Delete visit
 * - POST /api/visits/schedule - Schedule a new visit with conflict detection
 * - PUT /api/visits/{id}/reschedule - Reschedule existing visit
 * - DELETE /api/visits/{id}/cancel - Cancel visit
 * - PUT /api/visits/{id}/complete - Complete visit with diagnosis and treatment
 * - GET /api/visits/search - Search visits by multiple criteria
 * - GET /api/visits/schedule/daily - Daily schedule view
 * - GET /api/visits/statistics - Get visit statistics
 * 
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 */
@RestController
@RequestMapping("/api/visits")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
@Validated
public class VisitController {

    private static final Logger logger = LoggerFactory.getLogger(VisitController.class);

    @Autowired
    private VisitService visitService;

    // ========================================
    // Core CRUD Operations
    // ========================================

    /**
     * Get all visits with server-side pagination and sorting
     * GET /api/visits?page=0&size=10&sort=visitDate,desc
     * 
     * Supports sorting by: id, visitDate, visitType, diagnosis, treatment, cost, pet.name, veterinarian.lastName
     * Default sort: visitDate descending (most recent first)
     * 
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of all visits with server-side sorting applied
     */
    @GetMapping
    public ResponseEntity<PagedResponse<Visit>> getAllVisits(Pageable pageable) {
        logger.debug("Getting all visits with server-side pagination and sorting: {}", pageable);
        
        try {
            // Apply default sorting if none specified
            if (pageable.getSort().isUnsorted()) {
                pageable = PageRequest.of(
                    pageable.getPageNumber(), 
                    pageable.getPageSize(), 
                    Sort.by(Sort.Direction.DESC, "visitDate")
                );
            }
            
            // Use server-side sorting and pagination - sorting is applied at database level
            Page<Visit> visits = visitService.findAllWithPagination(pageable);
            PagedResponse<Visit> response = new PagedResponse<>(visits);
            
            logger.debug("Retrieved {} visits with server-side sorting", visits.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting visits with server-side sorting: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test endpoint for debugging content type issues
     */
    @PostMapping("/test")
    public ResponseEntity<String> testEndpoint(@RequestBody String body) {
        return ResponseEntity.ok("Test successful: " + body);
    }

    /**
     * Test endpoint for debugging content type issues with Visit model
     */
    @PostMapping("/test-visit")
    public ResponseEntity<Visit> testVisitEndpoint(@RequestBody Visit visit) {
        return ResponseEntity.ok(visit);
    }

    /**
     * Test endpoint for debugging content type issues with simple POJO
     */
    @PostMapping("/test-simple")
    public ResponseEntity<Map<String, Object>> testSimpleEndpoint(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(data);
    }

    /**
     * Get visit by ID
     * GET /api/visits/{id}
     * 
     * @param id Visit ID
     * @return Visit details or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Visit> getVisitById(@PathVariable Long id) {
        logger.debug("Getting visit by ID: {}", id);
        
        Optional<Visit> visit = visitService.findById(id);
        if (visit.isPresent()) {
            logger.debug("Found visit: {} on {}", visit.get().getId(), visit.get().getVisitDate());
            return ResponseEntity.ok(visit.get());
        } else {
            logger.debug("Visit not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new visit
     * POST /api/visits
     * 
     * @param visit Visit data to create
     * @return Created visit with 201 status
     */
    @PostMapping
    public ResponseEntity<Visit> createVisit(@RequestBody Map<String, Object> visitData) {
        logger.debug("Creating new visit with data: {}", visitData);
        
        // Convert Map to Visit object
        Visit visit = new Visit();
        
        // Set pet
        if (visitData.containsKey("pet") && visitData.get("pet") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> petData = (Map<String, Object>) visitData.get("pet");
            if (petData.containsKey("id")) {
                com.petclinic.backend.model.Pet pet = new com.petclinic.backend.model.Pet();
                pet.setId(Long.valueOf(petData.get("id").toString()));
                visit.setPet(pet);
            }
        }
        
        // Set visit date
        if (visitData.containsKey("visitDate") && visitData.get("visitDate") != null) {
            String visitDateStr = visitData.get("visitDate").toString();
            visit.setVisitDate(LocalDateTime.parse(visitDateStr));
        }
        
        // Set visit type
        if (visitData.containsKey("visitType") && visitData.get("visitType") != null) {
            String visitTypeStr = visitData.get("visitType").toString();
            visit.setVisitType(VisitType.valueOf(visitTypeStr));
        }
        
        // Set notes
        if (visitData.containsKey("notes") && visitData.get("notes") != null) {
            visit.setNotes(visitData.get("notes").toString());
        }
        
        // Set cost if provided
        if (visitData.containsKey("cost") && visitData.get("cost") != null) {
            visit.setCost(new BigDecimal(visitData.get("cost").toString()));
        }
        
        // Ensure ID is null for new entities
        visit.setId(null);
        Visit createdVisit = visitService.create(visit);
        
        logger.info("Created visit with ID: {} for pet {} on {}", 
                   createdVisit.getId(), 
                   createdVisit.getPet().getId(),
                   createdVisit.getVisitDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVisit);
    }

    /**
     * Update existing visit
     * PUT /api/visits/{id}
     * 
     * @param id Visit ID to update
     * @param visitData Updated visit data
     * @return Updated visit or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Visit> updateVisit(@PathVariable Long id, @RequestBody Map<String, Object> visitData) {
        logger.debug("Updating visit with ID: {} with data: {}", id, visitData);
        
        try {
            // Convert Map to Visit object
            Visit visit = new Visit();
            visit.setId(id); // Ensure the ID is set for update
            
            // Set pet
            if (visitData.containsKey("pet") && visitData.get("pet") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> petData = (Map<String, Object>) visitData.get("pet");
                if (petData.containsKey("id")) {
                    com.petclinic.backend.model.Pet pet = new com.petclinic.backend.model.Pet();
                    pet.setId(Long.valueOf(petData.get("id").toString()));
                    visit.setPet(pet);
                }
            }
            
            // Set visit date
            if (visitData.containsKey("visitDate") && visitData.get("visitDate") != null) {
                String visitDateStr = visitData.get("visitDate").toString();
                visit.setVisitDate(LocalDateTime.parse(visitDateStr));
            }
            
            // Set visit type
            if (visitData.containsKey("visitType") && visitData.get("visitType") != null) {
                String visitTypeStr = visitData.get("visitType").toString();
                visit.setVisitType(VisitType.valueOf(visitTypeStr));
            }
            
            // Set notes
            if (visitData.containsKey("notes") && visitData.get("notes") != null) {
                visit.setNotes(visitData.get("notes").toString());
            }
            
            // Set diagnosis with error handling for completion status
            if (visitData.containsKey("diagnosis")) {
                Object diagnosisObj = visitData.get("diagnosis");
                if (diagnosisObj != null) {
                    String diagnosis = diagnosisObj.toString().trim();
                    visit.setDiagnosis(diagnosis.isEmpty() ? null : diagnosis);
                } else {
                    visit.setDiagnosis(null);
                }
            }
            
            // Set treatment with error handling for completion status
            if (visitData.containsKey("treatment")) {
                Object treatmentObj = visitData.get("treatment");
                if (treatmentObj != null) {
                    String treatment = treatmentObj.toString().trim();
                    visit.setTreatment(treatment.isEmpty() ? null : treatment);
                } else {
                    visit.setTreatment(null);
                }
            }
            
            // Set cost if provided
            if (visitData.containsKey("cost") && visitData.get("cost") != null) {
                visit.setCost(new BigDecimal(visitData.get("cost").toString()));
            }
            
            Visit updatedVisit = visitService.update(id, visit);
            
            // Validate completion status calculation after update
            try {
                boolean completionStatus = updatedVisit.isCompleted();
                logger.debug("Visit {} completion status after update: {}", id, completionStatus);
            } catch (Exception e) {
                logger.warn("Error calculating completion status for updated visit {}: {}. Status defaulted to pending.", 
                           id, e.getMessage());
                // The visit is still updated successfully, just log the completion status issue
            }
            
            logger.info("Updated visit with ID: {} on {}", updatedVisit.getId(), updatedVisit.getVisitDate());
            return ResponseEntity.ok(updatedVisit);
            
        } catch (Exception e) {
            logger.error("Error updating visit with ID {}: {}", id, e.getMessage(), e);
            // Return a generic error response while maintaining data consistency
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete visit
     * DELETE /api/visits/{id}
     * 
     * @param id Visit ID to delete
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long id) {
        logger.debug("Deleting visit with ID: {}", id);
        
        visitService.deleteById(id);
        logger.info("Deleted visit with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get total count of visits
     * GET /api/visits/count
     * 
     * @return Total number of visits
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getVisitCount() {
        logger.debug("Getting total visit count");
        
        long count = visitService.count();
        Map<String, Long> result = Map.of("count", count);
        
        logger.debug("Total visit count: {}", count);
        return ResponseEntity.ok(result);
    }

    // ========================================
    // Scheduling Endpoints
    // ========================================

    /**
     * Schedule a new visit with conflict detection
     * POST /api/visits/schedule
     * 
     * @param visit Visit to schedule
     * @return Scheduled visit with 201 status
     */
    @PostMapping("/schedule")
    public ResponseEntity<Visit> scheduleVisit(@Valid @RequestBody Visit visit) {
        logger.debug("Scheduling visit for pet {} with veterinarian {} on {}", 
                    visit.getPet() != null ? visit.getPet().getId() : "unknown",
                    visit.getVeterinarian() != null ? visit.getVeterinarian().getId() : "unknown",
                    visit.getVisitDate());
        
        // Ensure ID is null for new entities
        visit.setId(null);
        Visit scheduledVisit = visitService.scheduleVisit(visit);
        
        logger.info("Scheduled visit with ID: {} for pet {} with veterinarian {} on {}", 
                   scheduledVisit.getId(),
                   scheduledVisit.getPet().getId(),
                   scheduledVisit.getVeterinarian() != null ? scheduledVisit.getVeterinarian().getId() : "TBD",
                   scheduledVisit.getVisitDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduledVisit);
    }

    /**
     * Reschedule existing visit
     * PUT /api/visits/{id}/reschedule
     * 
     * @param id Visit ID to reschedule
     * @param newDateTime New date and time for the visit
     * @return Rescheduled visit
     */
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Visit> rescheduleVisit(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime) {
        
        logger.debug("Rescheduling visit {} to {}", id, newDateTime);
        
        Visit rescheduledVisit = ((com.petclinic.backend.service.impl.VisitServiceImpl) visitService)
                .rescheduleVisit(id, newDateTime);
        
        logger.info("Rescheduled visit {} to {}", id, newDateTime);
        return ResponseEntity.ok(rescheduledVisit);
    }

    /**
     * Cancel visit
     * DELETE /api/visits/{id}/cancel
     * 
     * @param id Visit ID to cancel
     * @param reason Cancellation reason (optional)
     * @return Cancelled visit
     */
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Visit> cancelVisit(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        
        logger.debug("Cancelling visit {} with reason: {}", id, reason);
        
        Visit cancelledVisit = ((com.petclinic.backend.service.impl.VisitServiceImpl) visitService)
                .cancelVisit(id, reason);
        
        logger.info("Cancelled visit {} with reason: {}", id, reason);
        return ResponseEntity.ok(cancelledVisit);
    }

    /**
     * Check for scheduling conflicts
     * GET /api/visits/conflicts?vetId={vetId}&dateTime={dateTime}&duration={duration}
     * 
     * @param vetId Veterinarian ID
     * @param dateTime Proposed visit date and time
     * @param duration Expected duration in minutes (default: 30)
     * @return Boolean indicating if there's a conflict
     */
    @GetMapping("/conflicts")
    public ResponseEntity<Map<String, Boolean>> checkSchedulingConflicts(
            @RequestParam Long vetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(defaultValue = "30") @Min(1) int duration) {
        
        logger.debug("Checking scheduling conflicts for veterinarian {} at {} for {} minutes", 
                    vetId, dateTime, duration);
        
        boolean hasConflict = visitService.hasSchedulingConflict(vetId, dateTime, duration);
        Map<String, Boolean> result = Map.of("hasConflict", hasConflict);
        
        logger.debug("Scheduling conflict check for veterinarian {} at {}: {}", vetId, dateTime, hasConflict);
        return ResponseEntity.ok(result);
    }

    /**
     * Get upcoming visits
     * GET /api/visits/upcoming?days={days}&vetId={vetId}
     * 
     * @param days Number of days to look ahead (default: 7)
     * @param vetId Veterinarian ID (optional, if not provided returns all upcoming visits)
     * @return List of upcoming visits
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Visit>> getUpcomingVisits(
            @RequestParam(defaultValue = "7") @Min(1) int days,
            @RequestParam(required = false) Long vetId) {
        
        logger.debug("Getting upcoming visits for {} days, veterinarian: {}", days, vetId);
        
        List<Visit> upcomingVisits = visitService.getUpcomingVisits(vetId, days);
        logger.debug("Found {} upcoming visits", upcomingVisits.size());
        return ResponseEntity.ok(upcomingVisits);
    }

    /**
     * Get today's visits
     * GET /api/visits/today
     * 
     * @return List of today's visits
     */
    @GetMapping("/today")
    public ResponseEntity<List<Visit>> getTodaysVisits() {
        logger.debug("Getting today's visits");
        
        List<Visit> todaysVisits = ((com.petclinic.backend.service.impl.VisitServiceImpl) visitService)
                .findTodaysVisits();
        
        logger.debug("Found {} visits for today", todaysVisits.size());
        return ResponseEntity.ok(todaysVisits);
    }

    // ========================================
    // Visit Completion Endpoints
    // ========================================

    /**
     * Complete visit with diagnosis and treatment
     * PUT /api/visits/{id}/complete
     * 
     * @param id Visit ID to complete
     * @param completionData Map containing diagnosis, treatment, and notes
     * @return Completed visit
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Visit> completeVisit(
            @PathVariable Long id,
            @RequestBody Map<String, String> completionData) {
        
        logger.debug("Completing visit with ID: {}", id);
        
        try {
            // Extract and validate completion data with error handling
            String diagnosis = completionData.get("diagnosis");
            String treatment = completionData.get("treatment");
            String notes = completionData.get("notes");
            
            // Handle null/missing diagnosis and treatment fields gracefully
            // Ensure default "Pending" status for corrupted data (Requirements: 6.1, 6.3)
            if (diagnosis != null) {
                diagnosis = diagnosis.trim();
                if (diagnosis.isEmpty()) {
                    diagnosis = null; // Normalize empty strings to null
                }
            }
            
            if (treatment != null) {
                treatment = treatment.trim();
                if (treatment.isEmpty()) {
                    treatment = null; // Normalize empty strings to null
                }
            }
            
            // Validate that both diagnosis and treatment are provided for completion
            if (diagnosis == null || treatment == null) {
                logger.warn("Attempted to complete visit {} with missing diagnosis or treatment. " +
                           "Diagnosis: '{}', Treatment: '{}'", id, diagnosis, treatment);
                return ResponseEntity.badRequest().build();
            }
            
            Visit completedVisit = visitService.completeVisit(id, diagnosis, treatment, notes);
            
            // Validate completion status after the operation
            try {
                boolean isCompleted = completedVisit.isCompleted();
                if (!isCompleted) {
                    logger.warn("Visit {} was processed for completion but status shows as pending. " +
                               "This may indicate a data consistency issue.", id);
                }
                logger.info("Completed visit with ID: {} - Status: {}, Diagnosis: {}, Treatment: {}", 
                           completedVisit.getId(), isCompleted ? "Completed" : "Pending", diagnosis, treatment);
            } catch (Exception e) {
                logger.error("Error validating completion status for visit {}: {}. " +
                            "Visit was updated but status calculation failed.", id, e.getMessage());
            }
            
            return ResponseEntity.ok(completedVisit);
            
        } catch (Exception e) {
            logger.error("Error completing visit with ID {}: {}", id, e.getMessage(), e);
            // Maintain data consistency - don't leave visit in inconsistent state
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get completed visits
     * GET /api/visits/completed
     * 
     * @return List of completed visits
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Visit>> getCompletedVisits() {
        logger.debug("Getting completed visits");
        
        List<Visit> completedVisits = visitService.findCompletedVisits();
        logger.debug("Found {} completed visits", completedVisits.size());
        return ResponseEntity.ok(completedVisits);
    }

    /**
     * Get incomplete visits
     * GET /api/visits/incomplete
     * 
     * @return List of incomplete visits
     */
    @GetMapping("/incomplete")
    public ResponseEntity<List<Visit>> getIncompleteVisits() {
        logger.debug("Getting incomplete visits");
        
        try {
            List<Visit> allVisits = visitService.findAll();
            List<Visit> incompleteVisits = allVisits.stream()
                    .filter(visit -> {
                        try {
                            return !visit.isCompleted();
                        } catch (Exception e) {
                            // If completion status calculation fails, default to incomplete (pending)
                            logger.warn("Error calculating completion status for visit {}: {}. " +
                                       "Defaulting to incomplete status.", 
                                       visit.getId() != null ? visit.getId() : "unknown", e.getMessage());
                            return true; // Default to incomplete for error cases
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            logger.debug("Found {} incomplete visits", incompleteVisits.size());
            return ResponseEntity.ok(incompleteVisits);
            
        } catch (Exception e) {
            logger.error("Error retrieving incomplete visits: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================
    // Search and Filtering Endpoints
    // ========================================

    /**
     * Search visits by pet with pagination and result highlighting
     * GET /api/visits/search/by-pet?petId={petId}&page=0&size=10
     * 
     * Validates: Requirements 7.1, 7.5
     * 
     * @param petId Pet ID to search for
     * @param pageable Pagination parameters
     * @return Paginated visits for the specified pet with result counts
     */
    @GetMapping("/search/by-pet")
    public ResponseEntity<Map<String, Object>> searchVisitsByPet(
            @RequestParam Long petId,
            Pageable pageable) {
        
        logger.debug("Searching visits by pet ID: {} with pagination: {}", petId, pageable);
        
        try {
            List<Visit> allPetVisits = visitService.findByPet(petId);
            
            // Apply pagination manually since we need to filter by completion status
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allPetVisits.size());
            List<Visit> paginatedVisits = allPetVisits.subList(start, end);
            
            // Calculate result counts for highlighting
            long totalCount = allPetVisits.size();
            long completedCount = allPetVisits.stream()
                    .mapToLong(visit -> {
                        try {
                            return visit.isCompleted() ? 1 : 0;
                        } catch (Exception e) {
                            logger.warn("Error calculating completion status for visit {}: {}. Defaulting to incomplete.", 
                                       visit.getId(), e.getMessage());
                            return 0;
                        }
                    })
                    .sum();
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("visits", paginatedVisits);
            response.put("totalCount", totalCount);
            response.put("completedCount", completedCount);
            response.put("pendingCount", totalCount - completedCount);
            response.put("currentPage", pageable.getPageNumber());
            response.put("pageSize", pageable.getPageSize());
            response.put("totalPages", (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            response.put("searchCriteria", Map.of("petId", petId));
            
            logger.debug("Found {} visits for pet ID: {} (page {}/{})", 
                        paginatedVisits.size(), petId, pageable.getPageNumber() + 1, 
                        (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching visits by pet ID {}: {}", petId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search visits by veterinarian with pagination and result highlighting
     * GET /api/visits/search/by-veterinarian?veterinarianId={veterinarianId}&page=0&size=10
     * 
     * Validates: Requirements 7.2, 7.5
     * 
     * @param veterinarianId Veterinarian ID to search for
     * @param pageable Pagination parameters
     * @return Paginated visits for the specified veterinarian with result counts
     */
    @GetMapping("/search/by-veterinarian")
    public ResponseEntity<Map<String, Object>> searchVisitsByVeterinarian(
            @RequestParam Long veterinarianId,
            Pageable pageable) {
        
        logger.debug("Searching visits by veterinarian ID: {} with pagination: {}", veterinarianId, pageable);
        
        try {
            List<Visit> allVetVisits = visitService.findByVeterinarian(veterinarianId);
            
            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allVetVisits.size());
            List<Visit> paginatedVisits = allVetVisits.subList(start, end);
            
            // Calculate result counts for highlighting
            long totalCount = allVetVisits.size();
            long completedCount = allVetVisits.stream()
                    .mapToLong(visit -> {
                        try {
                            return visit.isCompleted() ? 1 : 0;
                        } catch (Exception e) {
                            logger.warn("Error calculating completion status for visit {}: {}. Defaulting to incomplete.", 
                                       visit.getId(), e.getMessage());
                            return 0;
                        }
                    })
                    .sum();
            
            // Count by visit types for additional highlighting
            Map<String, Long> visitTypeCounts = allVetVisits.stream()
                    .filter(visit -> visit.getVisitType() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                        visit -> visit.getVisitType().getDisplayName(),
                        java.util.stream.Collectors.counting()
                    ));
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("visits", paginatedVisits);
            response.put("totalCount", totalCount);
            response.put("completedCount", completedCount);
            response.put("pendingCount", totalCount - completedCount);
            response.put("visitTypeCounts", visitTypeCounts);
            response.put("currentPage", pageable.getPageNumber());
            response.put("pageSize", pageable.getPageSize());
            response.put("totalPages", (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            response.put("searchCriteria", Map.of("veterinarianId", veterinarianId));
            
            logger.debug("Found {} visits for veterinarian ID: {} (page {}/{})", 
                        paginatedVisits.size(), veterinarianId, pageable.getPageNumber() + 1,
                        (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching visits by veterinarian ID {}: {}", veterinarianId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search visits by status with pagination and result highlighting
     * GET /api/visits/search/by-status?status={status}&page=0&size=10
     * 
     * Validates: Requirements 7.3, 7.5
     * 
     * @param status Status to search for (completed, pending, emergency)
     * @param pageable Pagination parameters
     * @return Paginated visits with the specified status and result counts
     */
    @GetMapping("/search/by-status")
    public ResponseEntity<Map<String, Object>> searchVisitsByStatus(
            @RequestParam String status,
            Pageable pageable) {
        
        logger.debug("Searching visits by status: {} with pagination: {}", status, pageable);
        
        try {
            List<Visit> allVisits = visitService.findAll();
            List<Visit> filteredVisits = new ArrayList<>();
            
            // Filter by status
            String normalizedStatus = status.toLowerCase().trim();
            switch (normalizedStatus) {
                case "completed":
                    filteredVisits = allVisits.stream()
                            .filter(visit -> {
                                try {
                                    return visit.isCompleted();
                                } catch (Exception e) {
                                    logger.warn("Error calculating completion status for visit {}: {}. Defaulting to incomplete.", 
                                               visit.getId(), e.getMessage());
                                    return false;
                                }
                            })
                            .collect(java.util.stream.Collectors.toList());
                    break;
                case "pending":
                    filteredVisits = allVisits.stream()
                            .filter(visit -> {
                                try {
                                    return !visit.isCompleted();
                                } catch (Exception e) {
                                    logger.warn("Error calculating completion status for visit {}: {}. Defaulting to incomplete.", 
                                               visit.getId(), e.getMessage());
                                    return true; // Default to pending for error cases
                                }
                            })
                            .collect(java.util.stream.Collectors.toList());
                    break;
                case "emergency":
                    filteredVisits = allVisits.stream()
                            .filter(visit -> visit.getVisitType() != null && visit.getVisitType().isEmergency())
                            .collect(java.util.stream.Collectors.toList());
                    break;
                case "cancelled":
                    // For now, we don't have a cancelled status in the model
                    // This could be extended in the future with a proper status field
                    filteredVisits = new ArrayList<>();
                    break;
                default:
                    logger.warn("Unknown status filter: {}", status);
                    return ResponseEntity.badRequest().build();
            }
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filteredVisits.size());
            List<Visit> paginatedVisits = filteredVisits.subList(start, end);
            
            // Calculate additional statistics for highlighting
            long totalCount = filteredVisits.size();
            Map<String, Long> additionalStats = new java.util.HashMap<>();
            
            if ("emergency".equals(normalizedStatus)) {
                // For emergency visits, show breakdown by emergency types
                additionalStats = filteredVisits.stream()
                        .filter(visit -> visit.getVisitType() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                            visit -> visit.getVisitType().getDisplayName(),
                            java.util.stream.Collectors.counting()
                        ));
            } else {
                // For completed/pending, show breakdown by visit types
                additionalStats = filteredVisits.stream()
                        .filter(visit -> visit.getVisitType() != null)
                        .collect(java.util.stream.Collectors.groupingBy(
                            visit -> visit.getVisitType().getDisplayName(),
                            java.util.stream.Collectors.counting()
                        ));
            }
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("visits", paginatedVisits);
            response.put("totalCount", totalCount);
            response.put("status", normalizedStatus);
            response.put("statusBreakdown", additionalStats);
            response.put("currentPage", pageable.getPageNumber());
            response.put("pageSize", pageable.getPageSize());
            response.put("totalPages", (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            response.put("searchCriteria", Map.of("status", status));
            
            logger.debug("Found {} visits with status: {} (page {}/{})", 
                        paginatedVisits.size(), status, pageable.getPageNumber() + 1,
                        (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching visits by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Combined filter endpoint for multiple criteria with result highlighting
     * GET /api/visits/search/combined?petId={petId}&veterinarianId={veterinarianId}&status={status}&visitType={visitType}&startDate={startDate}&endDate={endDate}
     * 
     * Validates: Requirements 7.4, 7.5
     * 
     * @param petId Pet ID filter (optional)
     * @param veterinarianId Veterinarian ID filter (optional)
     * @param status Status filter (optional: completed, pending, emergency)
     * @param visitType Visit type filter (optional)
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @param pageable Pagination parameters
     * @return Paginated visits matching all criteria with comprehensive result highlighting
     */
    @GetMapping("/search/combined")
    public ResponseEntity<Map<String, Object>> searchVisitsCombined(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) VisitType visitType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        
        logger.debug("Combined search with criteria - petId: {}, veterinarianId: {}, status: {}, visitType: {}, startDate: {}, endDate: {}", 
                    petId, veterinarianId, status, visitType, startDate, endDate);
        
        try {
            List<Visit> allVisits = visitService.findAll();
            List<Visit> filteredVisits = allVisits.stream()
                    .filter(visit -> petId == null || (visit.getPet() != null && visit.getPet().getId().equals(petId)))
                    .filter(visit -> veterinarianId == null || (visit.getVeterinarian() != null && visit.getVeterinarian().getId().equals(veterinarianId)))
                    .filter(visit -> startDate == null || !visit.getVisitDate().toLocalDate().isBefore(startDate))
                    .filter(visit -> endDate == null || !visit.getVisitDate().toLocalDate().isAfter(endDate))
                    .filter(visit -> visitType == null || visitType.equals(visit.getVisitType()))
                    .filter(visit -> {
                        if (status == null) {
                            return true;
                        }
                        String normalizedStatus = status.toLowerCase().trim();
                        switch (normalizedStatus) {
                            case "completed":
                                try {
                                    return visit.isCompleted();
                                } catch (Exception e) {
                                    logger.warn("Error calculating completion status for visit {}: {}. Defaulting to incomplete.", 
                                               visit.getId(), e.getMessage());
                                    return false;
                                }
                            case "pending":
                                try {
                                    return !visit.isCompleted();
                                } catch (Exception e) {
                                    logger.warn("Error calculating completion status for visit {}: {}. Defaulting to incomplete.", 
                                               visit.getId(), e.getMessage());
                                    return true;
                                }
                            case "emergency":
                                return visit.getVisitType() != null && visit.getVisitType().isEmergency();
                            case "cancelled":
                                return false; // Not implemented yet
                            default:
                                return true;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filteredVisits.size());
            List<Visit> paginatedVisits = filteredVisits.subList(start, end);
            
            // Calculate comprehensive result highlighting
            long totalCount = filteredVisits.size();
            long completedCount = filteredVisits.stream()
                    .mapToLong(visit -> {
                        try {
                            return visit.isCompleted() ? 1 : 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
            
            Map<String, Long> visitTypeCounts = filteredVisits.stream()
                    .filter(visit -> visit.getVisitType() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                        visit -> visit.getVisitType().getDisplayName(),
                        java.util.stream.Collectors.counting()
                    ));
            
            Map<String, Long> veterinarianCounts = filteredVisits.stream()
                    .filter(visit -> visit.getVeterinarian() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                        visit -> visit.getVeterinarian().getFirstName() + " " + visit.getVeterinarian().getLastName(),
                        java.util.stream.Collectors.counting()
                    ));
            
            // Build active filters indicator
            Map<String, Object> activeFilters = new java.util.HashMap<>();
            if (petId != null) activeFilters.put("petId", petId);
            if (veterinarianId != null) activeFilters.put("veterinarianId", veterinarianId);
            if (status != null) activeFilters.put("status", status);
            if (visitType != null) activeFilters.put("visitType", visitType.getDisplayName());
            if (startDate != null) activeFilters.put("startDate", startDate);
            if (endDate != null) activeFilters.put("endDate", endDate);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("visits", paginatedVisits);
            response.put("totalCount", totalCount);
            response.put("completedCount", completedCount);
            response.put("pendingCount", totalCount - completedCount);
            response.put("visitTypeCounts", visitTypeCounts);
            response.put("veterinarianCounts", veterinarianCounts);
            response.put("activeFilters", activeFilters);
            response.put("currentPage", pageable.getPageNumber());
            response.put("pageSize", pageable.getPageSize());
            response.put("totalPages", (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            response.put("searchCriteria", activeFilters);
            
            logger.debug("Combined search found {} visits matching criteria (page {}/{})", 
                        paginatedVisits.size(), pageable.getPageNumber() + 1,
                        (int) Math.ceil((double) totalCount / pageable.getPageSize()));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in combined visit search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search visits by multiple criteria (legacy endpoint - maintained for backward compatibility)
     * GET /api/visits/search?petId={petId}&vetId={vetId}&startDate={startDate}&endDate={endDate}&visitType={visitType}&completed={completed}
     * 
     * @param petId Pet ID filter (optional)
     * @param vetId Veterinarian ID filter (optional)
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @param visitType Visit type filter (optional)
     * @param completed Completion status filter (optional)
     * @return List of visits matching the criteria
     */
    @GetMapping("/search")
    public ResponseEntity<List<Visit>> searchVisits(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long vetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) VisitType visitType,
            @RequestParam(required = false) Boolean completed) {
        
        logger.debug("Legacy search visits with criteria - petId: {}, vetId: {}, startDate: {}, endDate: {}, visitType: {}, completed: {}", 
                    petId, vetId, startDate, endDate, visitType, completed);
        
        List<Visit> allVisits = visitService.findAll();
        List<Visit> filteredVisits = allVisits.stream()
                .filter(visit -> petId == null || (visit.getPet() != null && visit.getPet().getId().equals(petId)))
                .filter(visit -> vetId == null || (visit.getVeterinarian() != null && visit.getVeterinarian().getId().equals(vetId)))
                .filter(visit -> startDate == null || !visit.getVisitDate().toLocalDate().isBefore(startDate))
                .filter(visit -> endDate == null || !visit.getVisitDate().toLocalDate().isAfter(endDate))
                .filter(visit -> visitType == null || visitType.equals(visit.getVisitType()))
                .filter(visit -> {
                    if (completed == null) {
                        return true;
                    }
                    try {
                        return completed.equals(visit.isCompleted());
                    } catch (Exception e) {
                        // If completion status calculation fails, default to incomplete (pending)
                        logger.warn("Error calculating completion status for visit {} during search: {}. " +
                                   "Defaulting to incomplete status.", 
                                   visit.getId() != null ? visit.getId() : "unknown", e.getMessage());
                        return completed.equals(false); // Default to incomplete for error cases
                    }
                })
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} visits matching search criteria", filteredVisits.size());
        return ResponseEntity.ok(filteredVisits);
    }

    /**
     * Get visits by pet
     * GET /api/visits/pet/{petId}
     * 
     * @param petId Pet ID
     * @return List of visits for the specified pet
     */
    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<Visit>> getVisitsByPet(@PathVariable Long petId) {
        logger.debug("Getting visits for pet ID: {}", petId);
        
        List<Visit> visits = visitService.findByPet(petId);
        logger.debug("Found {} visits for pet ID: {}", visits.size(), petId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get visits by veterinarian
     * GET /api/visits/veterinarian/{vetId}
     * 
     * @param vetId Veterinarian ID
     * @return List of visits for the specified veterinarian
     */
    @GetMapping("/veterinarian/{vetId}")
    public ResponseEntity<List<Visit>> getVisitsByVeterinarian(@PathVariable Long vetId) {
        logger.debug("Getting visits for veterinarian ID: {}", vetId);
        
        List<Visit> visits = visitService.findByVeterinarian(vetId);
        logger.debug("Found {} visits for veterinarian ID: {}", visits.size(), vetId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get visits by owner
     * GET /api/visits/owner/{ownerId}
     * 
     * @param ownerId Owner ID
     * @return List of visits for pets belonging to the specified owner
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Visit>> getVisitsByOwner(@PathVariable Long ownerId) {
        logger.debug("Getting visits for owner ID: {}", ownerId);
        
        List<Visit> visits = ((com.petclinic.backend.service.impl.VisitServiceImpl) visitService)
                .findVisitsByOwner(ownerId);
        
        logger.debug("Found {} visits for owner ID: {}", visits.size(), ownerId);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get visits by date range
     * GET /api/visits/date-range?startDate={startDate}&endDate={endDate}
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of visits within the date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Visit>> getVisitsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Getting visits between {} and {}", startDate, endDate);
        
        List<Visit> visits = visitService.findByDateRange(startDate, endDate);
        logger.debug("Found {} visits between {} and {}", visits.size(), startDate, endDate);
        return ResponseEntity.ok(visits);
    }

    /**
     * Get visits by type
     * GET /api/visits/type/{visitType}
     * 
     * @param visitType Visit type
     * @return List of visits of the specified type
     */
    @GetMapping("/type/{visitType}")
    public ResponseEntity<List<Visit>> getVisitsByType(@PathVariable VisitType visitType) {
        logger.debug("Getting visits by type: {}", visitType);
        
        List<Visit> allVisits = visitService.findAll();
        List<Visit> visitsByType = allVisits.stream()
                .filter(visit -> visitType.equals(visit.getVisitType()))
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} visits of type: {}", visitsByType.size(), visitType);
        return ResponseEntity.ok(visitsByType);
    }

    // ========================================
    // Schedule View Endpoints
    // ========================================

    /**
     * Daily schedule view
     * GET /api/visits/schedule/daily?date={date}
     * 
     * @param date Date for the schedule (default: today)
     * @return List of visits for the specified date
     */
    @GetMapping("/schedule/daily")
    public ResponseEntity<List<Visit>> getDailySchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate scheduleDate = date != null ? date : LocalDate.now();
        logger.debug("Getting daily schedule for: {}", scheduleDate);
        
        List<Visit> dailyVisits = visitService.findByDate(scheduleDate);
        logger.debug("Found {} visits for {}", dailyVisits.size(), scheduleDate);
        return ResponseEntity.ok(dailyVisits);
    }

    /**
     * Weekly schedule view
     * GET /api/visits/schedule/weekly?startDate={startDate}
     * 
     * @param startDate Start date of the week (default: current week start)
     * @return List of visits for the week
     */
    @GetMapping("/schedule/weekly")
    public ResponseEntity<List<Visit>> getWeeklySchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        
        LocalDate weekStart = startDate != null ? startDate : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        logger.debug("Getting weekly schedule from {} to {}", weekStart, weekEnd);
        
        List<Visit> weeklyVisits = visitService.findByDateRange(weekStart, weekEnd);
        logger.debug("Found {} visits for week {} to {}", weeklyVisits.size(), weekStart, weekEnd);
        return ResponseEntity.ok(weeklyVisits);
    }

    /**
     * Veterinarian schedule
     * GET /api/visits/schedule/veterinarian/{vetId}?startDate={startDate}&endDate={endDate}
     * 
     * @param vetId Veterinarian ID
     * @param startDate Start date (optional, default: today)
     * @param endDate End date (optional, default: 7 days from start)
     * @return List of visits for the veterinarian within the date range
     */
    @GetMapping("/schedule/veterinarian/{vetId}")
    public ResponseEntity<List<Visit>> getVeterinarianSchedule(
            @PathVariable Long vetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(7);
        
        logger.debug("Getting schedule for veterinarian {} from {} to {}", vetId, start, end);
        
        List<Visit> vetVisits = visitService.findByVeterinarian(vetId);
        List<Visit> filteredVisits = vetVisits.stream()
                .filter(visit -> !visit.getVisitDate().toLocalDate().isBefore(start))
                .filter(visit -> !visit.getVisitDate().toLocalDate().isAfter(end))
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} visits for veterinarian {} from {} to {}", 
                    filteredVisits.size(), vetId, start, end);
        return ResponseEntity.ok(filteredVisits);
    }

    /**
     * Schedule for specific date
     * GET /api/visits/schedule/date/{date}
     * 
     * @param date Specific date
     * @return List of visits for the specified date
     */
    @GetMapping("/schedule/date/{date}")
    public ResponseEntity<List<Visit>> getScheduleForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.debug("Getting schedule for date: {}", date);
        
        List<Visit> dateVisits = visitService.findByDate(date);
        logger.debug("Found {} visits for date: {}", dateVisits.size(), date);
        return ResponseEntity.ok(dateVisits);
    }

    // ========================================
    // Statistics and Analytics Endpoints
    // ========================================

    /**
     * Get visit statistics
     * GET /api/visits/statistics?startDate={startDate}&endDate={endDate}
     * 
     * @param startDate Start date for statistics (optional)
     * @param endDate End date for statistics (optional)
     * @return Visit statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<VisitStatistics> getVisitStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Getting visit statistics from {} to {}", startDate, endDate);
        
        VisitStatistics statistics;
        if (startDate != null && endDate != null) {
            statistics = visitService.getVisitStatistics(startDate, endDate);
        } else {
            statistics = ((com.petclinic.backend.service.impl.VisitServiceImpl) visitService)
                    .getVisitStatistics();
        }
        
        logger.debug("Visit statistics: {} total visits, {} completed", 
                    statistics.getTotalVisits(), statistics.getCompletedVisits());
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get revenue statistics
     * GET /api/visits/revenue?startDate={startDate}&endDate={endDate}
     * 
     * @param startDate Start date for revenue calculation (optional)
     * @param endDate End date for revenue calculation (optional)
     * @return Revenue statistics
     */
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Getting revenue statistics from {} to {}", startDate, endDate);
        
        VisitStatistics statistics;
        if (startDate != null && endDate != null) {
            statistics = visitService.getVisitStatistics(startDate, endDate);
        } else {
            statistics = ((com.petclinic.backend.service.impl.VisitServiceImpl) visitService)
                    .getVisitStatistics();
        }
        
        Map<String, Object> revenueStats = Map.of(
            "totalRevenue", statistics.getTotalRevenue(),
            "averageCost", statistics.getAverageCost(),
            "totalVisits", statistics.getTotalVisits(),
            "completedVisits", statistics.getCompletedVisits()
        );
        
        logger.debug("Revenue statistics: {} total revenue, {} average cost", 
                    statistics.getTotalRevenue(), statistics.getAverageCost());
        return ResponseEntity.ok(revenueStats);
    }

    /**
     * Get monthly visit analytics
     * GET /api/visits/analytics/monthly?year={year}&month={month}
     * 
     * @param year Year for analytics (optional, default: current year)
     * @param month Month for analytics (optional, default: current month)
     * @return Monthly visit analytics
     */
    @GetMapping("/analytics/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyAnalytics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        int analyticsYear = year != null ? year : LocalDate.now().getYear();
        int analyticsMonth = month != null ? month : LocalDate.now().getMonthValue();
        
        logger.debug("Getting monthly analytics for {}/{}", analyticsMonth, analyticsYear);
        
        LocalDate startDate = LocalDate.of(analyticsYear, analyticsMonth, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        VisitStatistics statistics = visitService.getVisitStatistics(startDate, endDate);
        
        Map<String, Object> monthlyAnalytics = Map.of(
            "year", analyticsYear,
            "month", analyticsMonth,
            "totalVisits", statistics.getTotalVisits(),
            "completedVisits", statistics.getCompletedVisits(),
            "scheduledVisits", statistics.getScheduledVisits(),
            "totalRevenue", statistics.getTotalRevenue(),
            "averageCost", statistics.getAverageCost()
        );
        
        logger.debug("Monthly analytics for {}/{}: {} visits, {} revenue", 
                    analyticsMonth, analyticsYear, statistics.getTotalVisits(), statistics.getTotalRevenue());
        return ResponseEntity.ok(monthlyAnalytics);
    }

    // ========================================
    // Utility Endpoints
    // ========================================

    /**
     * Check if visit exists
     * GET /api/visits/{id}/exists
     * 
     * @param id Visit ID
     * @return Boolean indicating if visit exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> checkVisitExists(@PathVariable Long id) {
        logger.debug("Checking if visit exists: {}", id);
        
        boolean exists = visitService.existsById(id);
        Map<String, Boolean> result = Map.of("exists", exists);
        
        logger.debug("Visit {} exists: {}", id, exists);
        return ResponseEntity.ok(result);
    }

    // ========================================
    // Administrative Endpoints
    // ========================================

    /**
     * Batch recalculate visit completion statuses
     * POST /api/admin/visits/recalculate-status
     * 
     * @return Recalculation result
     */
    @PostMapping("/admin/visits/recalculate-status")
    public ResponseEntity<Map<String, Object>> recalculateVisitStatuses() {
        logger.info("Received request to recalculate all visit statuses");
        
        try {
            List<Visit> allVisits = visitService.findAll();
            int processedCount = allVisits.size();
            
            // Note: In a real implementation, this would update the database
            // For now, we just return the count as the completion status is calculated dynamically
            
            logger.info("Recalculation completed - processed {} visits", processedCount);
            
            return ResponseEntity.ok(Map.of(
                "message", "Visit status recalculation completed",
                "processedCount", processedCount,
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            logger.error("Error during visit status recalculation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Recalculation failed: " + e.getMessage()));
        }
    }

    /**
     * Bulk delete visits
     * DELETE /api/visits/bulk
     * 
     * @param request Bulk delete request containing visit IDs
     * @return Bulk operation result
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkDeleteVisits(@RequestBody Map<String, Object> request) {
        try {
            logger.debug("Bulk delete visits request: {}", request);
            
            // Extract IDs from request
            @SuppressWarnings("unchecked")
            List<Object> idObjects = (List<Object>) request.get("ids");
            if (idObjects == null || idObjects.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No visit IDs provided", "success", false));
            }
            
            List<Long> ids = idObjects.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            
            int totalRequested = ids.size();
            int deletedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long id : ids) {
                try {
                    // Check if visit exists
                    if (!visitService.existsById(id)) {
                        errors.add("Visit with ID " + id + " not found");
                        continue;
                    }
                    
                    // Delete the visit
                    visitService.deleteById(id);
                    deletedCount++;
                    
                    logger.debug("Successfully deleted visit with ID: {}", id);
                    
                } catch (Exception e) {
                    logger.error("Error deleting visit with ID {}: {}", id, e.getMessage(), e);
                    errors.add("Failed to delete visit with ID " + id + ": " + e.getMessage());
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
                    "Successfully deleted " + deletedCount + " of " + totalRequested + " visits" :
                    "Failed to delete any visits"
            );
            
            logger.info("Bulk delete visits result: deleted {}/{} visits", deletedCount, totalRequested);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("General error in bulk delete visits: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Bulk delete failed", "details", e.getMessage(), "success", false));
        }
    }

    /**
     * Health check endpoint
     * GET /api/visits/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "VisitController",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}