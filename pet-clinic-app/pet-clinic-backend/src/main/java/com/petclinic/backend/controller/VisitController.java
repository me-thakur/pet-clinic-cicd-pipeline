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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Visit management and scheduling
 * Provides comprehensive CRUD operations, scheduling functionality, and visit management
 * Implements proper error handling, validation, and integration with VisitService
 * 
 * API Endpoints:
 * - GET /api/visits - List all visits with pagination
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
 * Validates: Requirements 2.1, 2.2, 2.3
 */
@RestController
@RequestMapping("/api/visits")
@CrossOrigin(origins = "*")
@Validated
public class VisitController {

    private static final Logger logger = LoggerFactory.getLogger(VisitController.class);

    @Autowired
    private VisitService visitService;

    // ========================================
    // Core CRUD Operations
    // ========================================

    /**
     * Get all visits with pagination
     * GET /api/visits?page=0&size=10&sort=visitDate,desc
     * 
     * @param pageable Pagination and sorting parameters
     * @return Paginated list of all visits
     */
    @GetMapping
    public ResponseEntity<PagedResponse<Visit>> getAllVisits(Pageable pageable) {
        logger.debug("Getting all visits with pagination: {}", pageable);
        
        // For now, get all visits and create a page manually
        // In a real implementation, you'd want to add pagination to the service layer
        List<Visit> allVisits = visitService.findAll();
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allVisits.size());
        List<Visit> pageContent = allVisits.subList(start, end);
        
        Page<Visit> visits = new org.springframework.data.domain.PageImpl<>(
            pageContent, 
            pageable, 
            allVisits.size()
        );
        
        PagedResponse<Visit> response = new PagedResponse<>(visits);
        
        logger.debug("Retrieved {} visits", visits.getTotalElements());
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Visit> createVisit(@Valid @RequestBody Visit visit) {
        logger.debug("Creating new visit for pet {} on {}", 
                    visit.getPet() != null ? visit.getPet().getId() : "unknown", 
                    visit.getVisitDate());
        
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
     * @param visit Updated visit data
     * @return Updated visit or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Visit> updateVisit(@PathVariable Long id, @Valid @RequestBody Visit visit) {
        logger.debug("Updating visit with ID: {}", id);
        
        Visit updatedVisit = visitService.update(id, visit);
        logger.info("Updated visit with ID: {} on {}", updatedVisit.getId(), updatedVisit.getVisitDate());
        return ResponseEntity.ok(updatedVisit);
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
        
        String diagnosis = completionData.get("diagnosis");
        String treatment = completionData.get("treatment");
        String notes = completionData.get("notes");
        
        Visit completedVisit = visitService.completeVisit(id, diagnosis, treatment, notes);
        
        logger.info("Completed visit with ID: {} - Diagnosis: {}, Treatment: {}", 
                   completedVisit.getId(), diagnosis, treatment);
        return ResponseEntity.ok(completedVisit);
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
        
        List<Visit> allVisits = visitService.findAll();
        List<Visit> incompleteVisits = allVisits.stream()
                .filter(visit -> !visit.isCompleted())
                .collect(java.util.stream.Collectors.toList());
        
        logger.debug("Found {} incomplete visits", incompleteVisits.size());
        return ResponseEntity.ok(incompleteVisits);
    }

    // ========================================
    // Search and Filtering Endpoints
    // ========================================

    /**
     * Search visits by multiple criteria
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
        
        logger.debug("Searching visits with criteria - petId: {}, vetId: {}, startDate: {}, endDate: {}, visitType: {}, completed: {}", 
                    petId, vetId, startDate, endDate, visitType, completed);
        
        List<Visit> allVisits = visitService.findAll();
        List<Visit> filteredVisits = allVisits.stream()
                .filter(visit -> petId == null || (visit.getPet() != null && visit.getPet().getId().equals(petId)))
                .filter(visit -> vetId == null || (visit.getVeterinarian() != null && visit.getVeterinarian().getId().equals(vetId)))
                .filter(visit -> startDate == null || !visit.getVisitDate().toLocalDate().isBefore(startDate))
                .filter(visit -> endDate == null || !visit.getVisitDate().toLocalDate().isAfter(endDate))
                .filter(visit -> visitType == null || visitType.equals(visit.getVisitType()))
                .filter(visit -> completed == null || completed.equals(visit.isCompleted()))
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