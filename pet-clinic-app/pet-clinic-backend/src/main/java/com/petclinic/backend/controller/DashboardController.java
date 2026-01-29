package com.petclinic.backend.controller;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for dashboard metrics and real-time analytics endpoints
 * Provides specialized dashboard functionality with real-time metrics
 * Validates: Requirements 5.4, 5.5
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private DashboardService dashboardService;
    
    /**
     * Get real-time dashboard metrics
     * GET /api/dashboard/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetrics> getRealTimeDashboardMetrics() {
        logger.info("Getting real-time dashboard metrics");
        
        DashboardMetrics metrics = dashboardService.getRealTimeDashboardMetrics();
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get dashboard metrics for specific date
     * GET /api/dashboard/metrics/{date}
     */
    @GetMapping("/metrics/{date}")
    public ResponseEntity<DashboardMetrics> getDashboardMetricsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.info("Getting dashboard metrics for date: {}", date);
        
        DashboardMetrics metrics = dashboardService.getDashboardMetrics(date);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get dashboard metrics for date range
     * GET /api/dashboard/metrics?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/metrics/range")
    public ResponseEntity<DashboardMetrics> getDashboardMetricsForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting dashboard metrics for date range: {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = dashboardService.getDashboardMetrics(startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get dashboard metrics with filters
     * POST /api/dashboard/metrics/filtered
     */
    @PostMapping("/metrics/filtered")
    public ResponseEntity<DashboardMetrics> getDashboardMetricsWithFilter(
            @RequestBody ReportFilter filter) {
        
        logger.info("Getting dashboard metrics with filter: {}", filter);
        
        DashboardMetrics metrics = dashboardService.getDashboardMetrics(filter);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get daily appointment metrics
     * GET /api/dashboard/appointments/daily?date=2024-01-15
     */
    @GetMapping("/appointments/daily")
    public ResponseEntity<Map<String, Long>> getDailyAppointmentMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        logger.info("Getting daily appointment metrics for date: {}", targetDate);
        
        Map<String, Long> metrics = dashboardService.getDailyAppointmentMetrics(targetDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get active pets count
     * GET /api/dashboard/pets/count
     */
    @GetMapping("/pets/count")
    public ResponseEntity<Long> getActivePetsCount() {
        logger.info("Getting active pets count");
        
        long count = dashboardService.getActivePetsCount();
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * Get active pets by species
     * GET /api/dashboard/pets/by-species
     */
    @GetMapping("/pets/by-species")
    public ResponseEntity<Map<String, Long>> getActivePetsBySpecies() {
        logger.info("Getting active pets by species");
        
        Map<String, Long> petsBySpecies = dashboardService.getActivePetsBySpecies();
        
        return ResponseEntity.ok(petsBySpecies);
    }
    
    /**
     * Get active owners count
     * GET /api/dashboard/owners/count
     */
    @GetMapping("/owners/count")
    public ResponseEntity<Long> getActiveOwnersCount() {
        logger.info("Getting active owners count");
        
        long count = dashboardService.getActiveOwnersCount();
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * Get veterinarian utilization
     * GET /api/dashboard/utilization?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/utilization")
    public ResponseEntity<Map<String, Object>> getVeterinarianUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting veterinarian utilization for date range: {} to {}", startDate, endDate);
        
        Map<String, Object> utilization = dashboardService.getVeterinarianUtilization(startDate, endDate);
        
        return ResponseEntity.ok(utilization);
    }
    
    /**
     * Get veterinarian utilization for specific veterinarian
     * GET /api/dashboard/utilization/{veterinarianId}?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/utilization/{veterinarianId}")
    public ResponseEntity<Double> getVeterinarianUtilization(
            @PathVariable Long veterinarianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting utilization for veterinarian {} for date range: {} to {}", veterinarianId, startDate, endDate);
        
        double utilization = dashboardService.getVeterinarianUtilization(veterinarianId, startDate, endDate);
        
        return ResponseEntity.ok(utilization);
    }
    
    /**
     * Get appointment completion rate
     * GET /api/dashboard/completion-rate?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/completion-rate")
    public ResponseEntity<Double> getAppointmentCompletionRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting appointment completion rate for date range: {} to {}", startDate, endDate);
        
        double completionRate = dashboardService.getAppointmentCompletionRate(startDate, endDate);
        
        return ResponseEntity.ok(completionRate);
    }
    
    /**
     * Get appointment completion rates by veterinarian
     * GET /api/dashboard/completion-rate/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/completion-rate/by-veterinarian")
    public ResponseEntity<Map<String, Double>> getAppointmentCompletionRatesByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting appointment completion rates by veterinarian for date range: {} to {}", startDate, endDate);
        
        Map<String, Double> completionRates = dashboardService.getAppointmentCompletionRatesByVeterinarian(startDate, endDate);
        
        return ResponseEntity.ok(completionRates);
    }
    
    /**
     * Get recent activities
     * GET /api/dashboard/activities/recent?limit=10
     */
    @GetMapping("/activities/recent")
    public ResponseEntity<List<DashboardMetrics.RecentActivity>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting recent activities with limit: {}", limit);
        
        List<DashboardMetrics.RecentActivity> activities = dashboardService.getRecentActivities(limit);
        
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get upcoming appointments
     * GET /api/dashboard/appointments/upcoming?days=7&limit=10
     */
    @GetMapping("/appointments/upcoming")
    public ResponseEntity<List<DashboardMetrics.UpcomingAppointment>> getUpcomingAppointments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting upcoming appointments for {} days with limit: {}", days, limit);
        
        List<DashboardMetrics.UpcomingAppointment> appointments = dashboardService.getUpcomingAppointments(days, limit);
        
        return ResponseEntity.ok(appointments);
    }
    
    /**
     * Get system alerts
     * GET /api/dashboard/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<DashboardMetrics.Alert>> getSystemAlerts() {
        logger.info("Getting system alerts");
        
        List<DashboardMetrics.Alert> alerts = dashboardService.getSystemAlerts();
        
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Get performance indicators
     * GET /api/dashboard/performance?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceIndicators(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting performance indicators for date range: {} to {}", startDate, endDate);
        
        Map<String, Object> indicators = dashboardService.getPerformanceIndicators(startDate, endDate);
        
        return ResponseEntity.ok(indicators);
    }
    
    /**
     * Get revenue metrics
     * GET /api/dashboard/revenue?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting revenue metrics for date range: {} to {}", startDate, endDate);
        
        Map<String, Object> metrics = dashboardService.getRevenueMetrics(startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get top veterinarians by visits
     * GET /api/dashboard/top-veterinarians/visits?startDate=2024-01-01&endDate=2024-01-31&limit=5
     */
    @GetMapping("/top-veterinarians/visits")
    public ResponseEntity<Map<String, Long>> getTopVeterinariansByVisits(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("Getting top {} veterinarians by visits for date range: {} to {}", limit, startDate, endDate);
        
        Map<String, Long> topVets = dashboardService.getTopVeterinariansByVisits(startDate, endDate, limit);
        
        return ResponseEntity.ok(topVets);
    }
    
    /**
     * Get top veterinarians by revenue
     * GET /api/dashboard/top-veterinarians/revenue?startDate=2024-01-01&endDate=2024-01-31&limit=5
     */
    @GetMapping("/top-veterinarians/revenue")
    public ResponseEntity<Map<String, Object>> getTopVeterinariansByRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("Getting top {} veterinarians by revenue for date range: {} to {}", limit, startDate, endDate);
        
        Map<String, Object> topVets = dashboardService.getTopVeterinariansByRevenue(startDate, endDate, limit);
        
        return ResponseEntity.ok(topVets);
    }
    
    /**
     * Get most common visit types
     * GET /api/dashboard/visit-types/common?startDate=2024-01-01&endDate=2024-01-31&limit=10
     */
    @GetMapping("/visit-types/common")
    public ResponseEntity<Map<String, Long>> getMostCommonVisitTypes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting most common visit types for date range: {} to {} with limit: {}", startDate, endDate, limit);
        
        Map<String, Long> visitTypes = dashboardService.getMostCommonVisitTypes(startDate, endDate, limit);
        
        return ResponseEntity.ok(visitTypes);
    }
    
    /**
     * Get visit statistics by species
     * GET /api/dashboard/visits/by-species?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/visits/by-species")
    public ResponseEntity<Map<String, Long>> getVisitStatisticsBySpecies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting visit statistics by species for date range: {} to {}", startDate, endDate);
        
        Map<String, Long> visitsBySpecies = dashboardService.getVisitStatisticsBySpecies(startDate, endDate);
        
        return ResponseEntity.ok(visitsBySpecies);
    }
    
    /**
     * Get dashboard metrics filtered by species
     * GET /api/dashboard/metrics/by-species/{species}?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/metrics/by-species/{species}")
    public ResponseEntity<DashboardMetrics> getDashboardMetricsBySpecies(
            @PathVariable String species,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting dashboard metrics filtered by species: {} for date range: {} to {}", species, startDate, endDate);
        
        DashboardMetrics metrics = dashboardService.getDashboardMetricsBySpecies(species, startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get dashboard metrics filtered by veterinarian
     * GET /api/dashboard/metrics/by-veterinarian/{veterinarianId}?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/metrics/by-veterinarian/{veterinarianId}")
    public ResponseEntity<DashboardMetrics> getDashboardMetricsByVeterinarian(
            @PathVariable Long veterinarianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting dashboard metrics filtered by veterinarian: {} for date range: {} to {}", veterinarianId, startDate, endDate);
        
        DashboardMetrics metrics = dashboardService.getDashboardMetricsByVeterinarian(veterinarianId, startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get monthly trends
     * GET /api/dashboard/trends/monthly?months=12
     */
    @GetMapping("/trends/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends(
            @RequestParam(defaultValue = "12") int months) {
        
        logger.info("Getting monthly trends for {} months", months);
        
        Map<String, Object> trends = dashboardService.getMonthlyTrends(months);
        
        return ResponseEntity.ok(trends);
    }
    
    /**
     * Get workload distribution
     * GET /api/dashboard/workload?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/workload")
    public ResponseEntity<Map<String, Object>> getWorkloadDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting workload distribution for date range: {} to {}", startDate, endDate);
        
        Map<String, Object> distribution = dashboardService.getWorkloadDistribution(startDate, endDate);
        
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * Get capacity utilization
     * GET /api/dashboard/capacity?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/capacity")
    public ResponseEntity<Map<String, Object>> getCapacityUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting capacity utilization for date range: {} to {}", startDate, endDate);
        
        Map<String, Object> capacity = dashboardService.getCapacityUtilization(startDate, endDate);
        
        return ResponseEntity.ok(capacity);
    }
    
    /**
     * Check dashboard health
     * GET /api/dashboard/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDashboardHealth() {
        logger.info("Checking dashboard health");
        
        Map<String, Object> health = Map.of(
                "isHealthy", dashboardService.isDashboardHealthy(),
                "healthScore", dashboardService.getDashboardHealthScore()
        );
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get dashboard summary
     * GET /api/dashboard/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        logger.info("Getting dashboard summary");
        
        Map<String, Object> summary = dashboardService.getDashboardSummary();
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Refresh dashboard cache
     * POST /api/dashboard/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshDashboardCache() {
        logger.info("Refreshing dashboard cache");
        
        dashboardService.refreshDashboardCache();
        
        Map<String, String> response = Map.of(
                "status", "success",
                "message", "Dashboard cache refreshed successfully"
        );
        
        return ResponseEntity.ok(response);
    }
}