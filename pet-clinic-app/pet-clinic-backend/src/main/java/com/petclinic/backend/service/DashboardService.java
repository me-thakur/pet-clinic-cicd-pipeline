package com.petclinic.backend.service;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for dashboard metrics and real-time analytics
 * Provides specialized dashboard functionality with real-time metrics calculation
 * Validates: Requirements 5.4, 5.5
 */
public interface DashboardService {
    
    /**
     * Get real-time dashboard metrics for today
     * @return Dashboard metrics with current data
     */
    DashboardMetrics getRealTimeDashboardMetrics();
    
    /**
     * Get dashboard metrics for a specific date
     * @param date Date to generate metrics for
     * @return Dashboard metrics for the specified date
     */
    DashboardMetrics getDashboardMetrics(LocalDate date);
    
    /**
     * Get dashboard metrics with date range filtering
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Dashboard metrics for the date range
     */
    DashboardMetrics getDashboardMetrics(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get dashboard metrics with advanced filtering
     * @param filter Report filter criteria
     * @return Dashboard metrics with applied filters
     */
    DashboardMetrics getDashboardMetrics(ReportFilter filter);
    
    /**
     * Get daily appointment metrics
     * @param date Date to get appointments for
     * @return Map of appointment metrics (scheduled, completed, pending, cancelled)
     */
    Map<String, Long> getDailyAppointmentMetrics(LocalDate date);
    
    /**
     * Get active pets count
     * @return Total number of active pets in the system
     */
    long getActivePetsCount();
    
    /**
     * Get active pets count by species
     * @return Map of species to pet count
     */
    Map<String, Long> getActivePetsBySpecies();
    
    /**
     * Get active owners count
     * @return Total number of active owners in the system
     */
    long getActiveOwnersCount();
    
    /**
     * Get veterinarian utilization metrics
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of veterinarian utilization data
     */
    Map<String, Object> getVeterinarianUtilization(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get veterinarian utilization for a specific veterinarian
     * @param veterinarianId Veterinarian ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Utilization percentage for the veterinarian
     */
    double getVeterinarianUtilization(Long veterinarianId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get appointment completion rates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Completion rate percentage
     */
    double getAppointmentCompletionRate(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get appointment completion rates by veterinarian
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of veterinarian to completion rate
     */
    Map<String, Double> getAppointmentCompletionRatesByVeterinarian(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get recent activities for dashboard
     * @param limit Maximum number of activities to return
     * @return List of recent activities
     */
    List<DashboardMetrics.RecentActivity> getRecentActivities(int limit);
    
    /**
     * Get upcoming appointments for dashboard
     * @param days Number of days ahead to look for appointments
     * @param limit Maximum number of appointments to return
     * @return List of upcoming appointments
     */
    List<DashboardMetrics.UpcomingAppointment> getUpcomingAppointments(int days, int limit);
    
    /**
     * Get system alerts and notifications
     * @return List of current system alerts
     */
    List<DashboardMetrics.Alert> getSystemAlerts();
    
    /**
     * Get performance indicators for the dashboard
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of performance indicators
     */
    Map<String, Object> getPerformanceIndicators(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get revenue metrics for dashboard
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of revenue metrics (total, daily average, monthly total)
     */
    Map<String, Object> getRevenueMetrics(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get top performing veterinarians by visit count
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return Map of veterinarian names to visit counts
     */
    Map<String, Long> getTopVeterinariansByVisits(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Get top performing veterinarians by revenue
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return Map of veterinarian names to revenue amounts
     */
    Map<String, Object> getTopVeterinariansByRevenue(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Get most common visit types
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return Map of visit types to counts
     */
    Map<String, Long> getMostCommonVisitTypes(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Get visit statistics by species
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of species to visit counts
     */
    Map<String, Long> getVisitStatisticsBySpecies(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get filtered dashboard metrics with species filter
     * @param species Species to filter by
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Dashboard metrics filtered by species
     */
    DashboardMetrics getDashboardMetricsBySpecies(String species, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get filtered dashboard metrics with veterinarian filter
     * @param veterinarianId Veterinarian ID to filter by
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Dashboard metrics filtered by veterinarian
     */
    DashboardMetrics getDashboardMetricsByVeterinarian(Long veterinarianId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get monthly trends for dashboard
     * @param months Number of months to include
     * @return Map of monthly trend data
     */
    Map<String, Object> getMonthlyTrends(int months);
    
    /**
     * Get workload distribution across veterinarians
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of workload distribution data
     */
    Map<String, Object> getWorkloadDistribution(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get capacity utilization metrics
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Map of capacity utilization data
     */
    Map<String, Object> getCapacityUtilization(LocalDate startDate, LocalDate endDate);
    
    /**
     * Check if the dashboard data is healthy (no critical issues)
     * @return true if dashboard shows healthy metrics, false otherwise
     */
    boolean isDashboardHealthy();
    
    /**
     * Get dashboard health score (0-100)
     * @return Health score based on various metrics
     */
    double getDashboardHealthScore();
    
    /**
     * Refresh dashboard cache (if caching is implemented)
     */
    void refreshDashboardCache();
    
    /**
     * Get dashboard metrics summary for quick overview
     * @return Simplified dashboard metrics for overview display
     */
    Map<String, Object> getDashboardSummary();
}