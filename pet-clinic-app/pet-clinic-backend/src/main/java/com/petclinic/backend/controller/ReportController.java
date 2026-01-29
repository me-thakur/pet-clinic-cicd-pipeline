package com.petclinic.backend.controller;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.service.ReportService;
import com.petclinic.backend.service.ReportExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for reporting and analytics endpoints
 * Provides visit statistics, revenue reports, and dashboard metrics
 * Validates: Requirements 5.1, 5.2, 5.4, 5.5
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ReportExportService reportExportService;
    
    /**
     * Generate visit statistics report
     * GET /api/reports/visits?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/visits")
    public ResponseEntity<VisitStatisticsReport> getVisitStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Generating visit statistics report for date range: {} to {}", startDate, endDate);
        
        VisitStatisticsReport report = reportService.generateVisitStatistics(startDate, endDate);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Generate visit statistics report with filters
     * POST /api/reports/visits
     */
    @PostMapping("/visits")
    public ResponseEntity<VisitStatisticsReport> getVisitStatisticsWithFilter(
            @RequestBody ReportFilter filter) {
        
        logger.info("Generating visit statistics report with filter: {}", filter);
        
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Generate revenue report
     * GET /api/reports/revenue?startDate=2024-01-01&endDate=2024-01-31&includeTrends=true
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueReport> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean includeTrends) {
        
        logger.info("Generating revenue report for date range: {} to {}, includeTrends: {}", 
                   startDate, endDate, includeTrends);
        
        RevenueReport report = reportService.generateRevenueReport(startDate, endDate, includeTrends);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Generate revenue report with filters
     * POST /api/reports/revenue
     */
    @PostMapping("/revenue")
    public ResponseEntity<RevenueReport> getRevenueReportWithFilter(
            @RequestBody ReportFilter filter) {
        
        logger.info("Generating revenue report with filter: {}", filter);
        
        RevenueReport report = reportService.generateRevenueReport(filter);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get dashboard metrics
     * GET /api/reports/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics() {
        
        logger.info("Getting dashboard metrics");
        
        DashboardMetrics metrics = reportService.getDashboardMetrics();
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get dashboard metrics for specific date
     * GET /api/reports/dashboard?date=2024-01-15
     */
    @GetMapping("/dashboard/{date}")
    public ResponseEntity<DashboardMetrics> getDashboardMetricsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.info("Getting dashboard metrics for date: {}", date);
        
        DashboardMetrics metrics = reportService.getDashboardMetrics(date);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get visit statistics by veterinarian
     * GET /api/reports/visits/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/visits/by-veterinarian")
    public ResponseEntity<List<VisitStatisticsReport>> getVisitStatisticsByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting visit statistics by veterinarian for date range: {} to {}", startDate, endDate);
        
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsByVeterinarian(startDate, endDate);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get visit statistics by visit type
     * GET /api/reports/visits/by-type?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/visits/by-type")
    public ResponseEntity<List<VisitStatisticsReport>> getVisitStatisticsByType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting visit statistics by type for date range: {} to {}", startDate, endDate);
        
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsByType(startDate, endDate);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get visit statistics by species
     * GET /api/reports/visits/by-species?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/visits/by-species")
    public ResponseEntity<List<VisitStatisticsReport>> getVisitStatisticsBySpecies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting visit statistics by species for date range: {} to {}", startDate, endDate);
        
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsBySpecies(startDate, endDate);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get revenue report by veterinarian
     * GET /api/reports/revenue/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/revenue/by-veterinarian")
    public ResponseEntity<List<RevenueReport>> getRevenueReportByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting revenue report by veterinarian for date range: {} to {}", startDate, endDate);
        
        List<RevenueReport> reports = reportService.generateRevenueReportByVeterinarian(startDate, endDate);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get revenue report by species
     * GET /api/reports/revenue/by-species?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/revenue/by-species")
    public ResponseEntity<List<RevenueReport>> getRevenueReportBySpecies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting revenue report by species for date range: {} to {}", startDate, endDate);
        
        List<RevenueReport> reports = reportService.generateRevenueReportBySpecies(startDate, endDate);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get monthly revenue trends
     * GET /api/reports/revenue/trends?months=12
     */
    @GetMapping("/revenue/trends")
    public ResponseEntity<RevenueReport> getMonthlyRevenueTrends(
            @RequestParam(defaultValue = "12") int months) {
        
        logger.info("Getting monthly revenue trends for {} months", months);
        
        RevenueReport report = reportService.getMonthlyRevenueTrends(months);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get veterinarian utilization report
     * GET /api/reports/utilization?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/utilization")
    public ResponseEntity<DashboardMetrics> getVeterinarianUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting veterinarian utilization for date range: {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = reportService.getVeterinarianUtilization(startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get appointment completion rates
     * GET /api/reports/completion-rates?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/completion-rates")
    public ResponseEntity<DashboardMetrics> getAppointmentCompletionRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Getting appointment completion rates for date range: {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = reportService.getAppointmentCompletionRates(startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Generate comprehensive performance report
     * GET /api/reports/performance?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/performance")
    public ResponseEntity<DashboardMetrics> getPerformanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Generating performance report for date range: {} to {}", startDate, endDate);
        
        DashboardMetrics metrics = reportService.generatePerformanceReport(startDate, endDate);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get top veterinarians by visit count
     * GET /api/reports/top-veterinarians/visits?startDate=2024-01-01&endDate=2024-01-31&limit=5
     */
    @GetMapping("/top-veterinarians/visits")
    public ResponseEntity<List<VisitStatisticsReport>> getTopVeterinariansByVisits(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("Getting top {} veterinarians by visits for date range: {} to {}", limit, startDate, endDate);
        
        List<VisitStatisticsReport> reports = reportService.getTopVeterinariansByVisits(startDate, endDate, limit);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get top veterinarians by revenue
     * GET /api/reports/top-veterinarians/revenue?startDate=2024-01-01&endDate=2024-01-31&limit=5
     */
    @GetMapping("/top-veterinarians/revenue")
    public ResponseEntity<List<RevenueReport>> getTopVeterinariansByRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("Getting top {} veterinarians by revenue for date range: {} to {}", limit, startDate, endDate);
        
        List<RevenueReport> reports = reportService.getTopVeterinariansByRevenue(startDate, endDate, limit);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get most common visit types
     * GET /api/reports/top-visit-types/visits?startDate=2024-01-01&endDate=2024-01-31&limit=10
     */
    @GetMapping("/top-visit-types/visits")
    public ResponseEntity<List<VisitStatisticsReport>> getMostCommonVisitTypes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting top {} visit types by count for date range: {} to {}", limit, startDate, endDate);
        
        List<VisitStatisticsReport> reports = reportService.getMostCommonVisitTypes(startDate, endDate, limit);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get most profitable visit types
     * GET /api/reports/top-visit-types/revenue?startDate=2024-01-01&endDate=2024-01-31&limit=10
     */
    @GetMapping("/top-visit-types/revenue")
    public ResponseEntity<List<RevenueReport>> getMostProfitableVisitTypes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting top {} visit types by revenue for date range: {} to {}", limit, startDate, endDate);
        
        List<RevenueReport> reports = reportService.getMostProfitableVisitTypes(startDate, endDate, limit);
        
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get available filter options
     * GET /api/reports/filter-options
     */
    @GetMapping("/filter-options")
    public ResponseEntity<ReportFilter> getAvailableFilterOptions() {
        
        logger.info("Getting available filter options");
        
        ReportFilter filter = reportService.getAvailableFilterOptions();
        
        return ResponseEntity.ok(filter);
    }
    
    /**
     * Validate report filter
     * POST /api/reports/validate-filter
     */
    @PostMapping("/validate-filter")
    public ResponseEntity<Boolean> validateReportFilter(@RequestBody ReportFilter filter) {
        
        logger.info("Validating report filter: {}", filter);
        
        boolean isValid = reportService.validateReportFilter(filter);
        
        return ResponseEntity.ok(isValid);
    }
    
    // ========== EXPORT ENDPOINTS ==========
    
    /**
     * Export visit statistics report
     * GET /api/reports/export/visits?startDate=2024-01-01&endDate=2024-01-31&format=pdf
     */
    @GetMapping("/export/visits")
    public ResponseEntity<byte[]> exportVisitStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting visit statistics report for date range: {} to {}, format: {}", 
                   startDate, endDate, format);
        
        if (!reportExportService.validateExportParameters("visit-statistics", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        VisitStatisticsReport report = reportService.generateVisitStatistics(startDate, endDate);
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportVisitStatisticsToCSV(report);
        } else {
            exportData = reportExportService.exportVisitStatisticsToPdf(report);
        }
        
        String filename = reportExportService.getExportFilename("visit-statistics", format, startDate, endDate);
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
    
    /**
     * Export visit statistics report with filters
     * POST /api/reports/export/visits?format=pdf
     */
    @PostMapping("/export/visits")
    public ResponseEntity<byte[]> exportVisitStatisticsWithFilter(
            @RequestBody ReportFilter filter,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting visit statistics report with filter: {}, format: {}", filter, format);
        
        if (!reportExportService.validateExportParameters("visit-statistics", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportFilteredVisitStatisticsToCSV(filter);
        } else {
            exportData = reportExportService.exportFilteredVisitStatisticsToPdf(filter);
        }
        
        String filename = reportExportService.getExportFilename("visit-statistics", format, 
                filter.getStartDate(), filter.getEndDate());
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
    
    /**
     * Export revenue report
     * GET /api/reports/export/revenue?startDate=2024-01-01&endDate=2024-01-31&format=pdf
     */
    @GetMapping("/export/revenue")
    public ResponseEntity<byte[]> exportRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false") boolean includeTrends) {
        
        logger.info("Exporting revenue report for date range: {} to {}, format: {}, includeTrends: {}", 
                   startDate, endDate, format, includeTrends);
        
        if (!reportExportService.validateExportParameters("revenue", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        RevenueReport report = reportService.generateRevenueReport(startDate, endDate, includeTrends);
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportRevenueReportToCSV(report);
        } else {
            exportData = reportExportService.exportRevenueReportToPdf(report);
        }
        
        String filename = reportExportService.getExportFilename("revenue", format, startDate, endDate);
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
    
    /**
     * Export revenue report with filters
     * POST /api/reports/export/revenue?format=pdf
     */
    @PostMapping("/export/revenue")
    public ResponseEntity<byte[]> exportRevenueReportWithFilter(
            @RequestBody ReportFilter filter,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting revenue report with filter: {}, format: {}", filter, format);
        
        if (!reportExportService.validateExportParameters("revenue", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportFilteredRevenueReportToCSV(filter);
        } else {
            exportData = reportExportService.exportFilteredRevenueReportToPdf(filter);
        }
        
        String filename = reportExportService.getExportFilename("revenue", format, 
                filter.getStartDate(), filter.getEndDate());
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
    
    /**
     * Export dashboard metrics
     * GET /api/reports/export/dashboard?format=pdf&date=2024-01-15
     */
    @GetMapping("/export/dashboard")
    public ResponseEntity<byte[]> exportDashboardMetrics(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.info("Exporting dashboard metrics, format: {}, date: {}", format, date);
        
        if (!reportExportService.validateExportParameters("dashboard", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        DashboardMetrics metrics = date != null ? 
                reportService.getDashboardMetrics(date) : 
                reportService.getDashboardMetrics();
        
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportDashboardMetricsToCSV(metrics);
        } else {
            exportData = reportExportService.exportDashboardMetricsToPdf(metrics);
        }
        
        String filename = reportExportService.getExportFilename("dashboard", format, date, date);
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
    
    /**
     * Export visit statistics by veterinarian
     * GET /api/reports/export/visits/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31&format=pdf
     */
    @GetMapping("/export/visits/by-veterinarian")
    public ResponseEntity<byte[]> exportVisitStatisticsByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting visit statistics by veterinarian for date range: {} to {}, format: {}", 
                   startDate, endDate, format);
        
        if (!reportExportService.validateExportParameters("visit-statistics-by-veterinarian", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsByVeterinarian(startDate, endDate);
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportMultipleVisitStatisticsToCSV(reports);
        } else {
            exportData = reportExportService.exportMultipleVisitStatisticsToPdf(reports, "Visit Statistics by Veterinarian");
        }
        
        String filename = reportExportService.getExportFilename("visit-statistics-by-veterinarian", format, startDate, endDate);
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
    
    /**
     * Export revenue report by veterinarian
     * GET /api/reports/export/revenue/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31&format=pdf
     */
    @GetMapping("/export/revenue/by-veterinarian")
    public ResponseEntity<byte[]> exportRevenueReportByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting revenue report by veterinarian for date range: {} to {}, format: {}", 
                   startDate, endDate, format);
        
        if (!reportExportService.validateExportParameters("revenue-by-veterinarian", format)) {
            return ResponseEntity.badRequest().build();
        }
        
        List<RevenueReport> reports = reportService.generateRevenueReportByVeterinarian(startDate, endDate);
        byte[] exportData;
        
        if ("csv".equalsIgnoreCase(format)) {
            exportData = reportExportService.exportMultipleRevenueReportsToCSV(reports);
        } else {
            exportData = reportExportService.exportMultipleRevenueReportsToPdf(reports, "Revenue Report by Veterinarian");
        }
        
        String filename = reportExportService.getExportFilename("revenue-by-veterinarian", format, startDate, endDate);
        String mimeType = reportExportService.getMimeType(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(exportData);
    }
}