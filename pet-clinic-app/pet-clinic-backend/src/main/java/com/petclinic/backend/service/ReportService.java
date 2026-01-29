package com.petclinic.backend.service;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for reporting and analytics operations
 * Provides visit statistics, revenue analysis, and dashboard metrics
 * Validates: Requirements 5.1, 5.2, 5.4, 5.5
 */
public interface ReportService {
    
    /**
     * Generate visit statistics report for a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Visit statistics report
     */
    VisitStatisticsReport generateVisitStatistics(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate visit statistics report with filters
     * @param filter Report filter criteria
     * @return Visit statistics report
     */
    VisitStatisticsReport generateVisitStatistics(ReportFilter filter);
    
    /**
     * Generate revenue report for a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Revenue report with totals and trends
     */
    RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate revenue report with filters
     * @param filter Report filter criteria
     * @return Revenue report with totals and trends
     */
    RevenueReport generateRevenueReport(ReportFilter filter);
    
    /**
     * Generate revenue report with trend analysis
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param includeTrends Whether to include trend analysis
     * @return Revenue report with optional trend data
     */
    RevenueReport generateRevenueReport(LocalDate startDate, LocalDate endDate, boolean includeTrends);
    
    /**
     * Get dashboard metrics with key performance indicators
     * @return Dashboard metrics
     */
    DashboardMetrics getDashboardMetrics();
    
    /**
     * Get dashboard metrics for a specific date
     * @param date Date to generate metrics for
     * @return Dashboard metrics for the specified date
     */
    DashboardMetrics getDashboardMetrics(LocalDate date);
    
    /**
     * Generate visit statistics by veterinarian
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of visit statistics grouped by veterinarian
     */
    List<VisitStatisticsReport> generateVisitStatisticsByVeterinarian(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate visit statistics by visit type
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of visit statistics grouped by visit type
     */
    List<VisitStatisticsReport> generateVisitStatisticsByType(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate visit statistics by species
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of visit statistics grouped by species
     */
    List<VisitStatisticsReport> generateVisitStatisticsBySpecies(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate revenue report by veterinarian
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of revenue reports grouped by veterinarian
     */
    List<RevenueReport> generateRevenueReportByVeterinarian(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate revenue report by species
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of revenue reports grouped by species
     */
    List<RevenueReport> generateRevenueReportBySpecies(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get monthly revenue trends
     * @param months Number of months to include in trend analysis
     * @return Revenue report with monthly trend data
     */
    RevenueReport getMonthlyRevenueTrends(int months);
    
    /**
     * Get veterinarian utilization report
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Dashboard metrics with veterinarian utilization data
     */
    DashboardMetrics getVeterinarianUtilization(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get appointment completion rates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Dashboard metrics with completion rate data
     */
    DashboardMetrics getAppointmentCompletionRates(LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate comprehensive clinic performance report
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Dashboard metrics with comprehensive performance data
     */
    DashboardMetrics generatePerformanceReport(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get top performing veterinarians by visit count
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return List of visit statistics for top veterinarians
     */
    List<VisitStatisticsReport> getTopVeterinariansByVisits(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Get top performing veterinarians by revenue
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return List of revenue reports for top veterinarians
     */
    List<RevenueReport> getTopVeterinariansByRevenue(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Get most common visit types
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return List of visit statistics for most common visit types
     */
    List<VisitStatisticsReport> getMostCommonVisitTypes(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Get most profitable visit types
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param limit Maximum number of results
     * @return List of revenue reports for most profitable visit types
     */
    List<RevenueReport> getMostProfitableVisitTypes(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * Validate report filter
     * @param filter Report filter to validate
     * @return true if filter is valid, false otherwise
     */
    boolean validateReportFilter(ReportFilter filter);
    
    /**
     * Get available report filter options
     * @return Report filter with available options populated
     */
    ReportFilter getAvailableFilterOptions();
}