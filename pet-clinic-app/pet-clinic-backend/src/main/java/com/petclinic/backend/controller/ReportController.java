package com.petclinic.backend.controller;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.service.ReportService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.util.ExportParameterValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
public class ReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ReportExportService reportExportService;
    
    @Autowired
    private ExportParameterValidator parameterValidator;
    
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
     * POST /api/reports/export/visits (with form data or JSON body)
     */
    @GetMapping("/export/visits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportVisitStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String species) {
        
        return handleExportVisitStatistics("visits", startDate, endDate, format, veterinarianId, species, "GET");
    }
    
    /**
     * Export visit statistics report via POST
     * POST /api/reports/export/visits?format=pdf
     */
    @PostMapping("/export/visits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportVisitStatisticsPost(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String species) {
        
        return handleExportVisitStatistics("visits", startDate, endDate, format, veterinarianId, species, "POST");
    }
    
    /**
     * Common handler for visit statistics export
     */
    private ResponseEntity<byte[]> handleExportVisitStatistics(String reportType, LocalDate startDate, 
                                                             LocalDate endDate, String format, 
                                                             Long veterinarianId, String species, 
                                                             String method) {
        
        logger.info("Exporting visit statistics report - Method: {}, DateRange: {} to {}, Format: {}, VetId: {}, Species: {}", 
                   method, startDate, endDate, format, veterinarianId, species);
        
        try {
            // Comprehensive parameter validation
            ExportParameterValidator.ValidationResult validation = 
                parameterValidator.validateExportParameters(reportType, format, startDate, endDate);
            
            if (!validation.isValid()) {
                logger.warn("Export parameter validation failed: {}", validation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(validation).getBytes());
            }
            
            // Validate optional filter parameters
            ExportParameterValidator.ValidationResult filterValidation = 
                parameterValidator.validateFilterParameters(veterinarianId, species);
            
            if (!filterValidation.isValid()) {
                logger.warn("Export filter validation failed: {}", filterValidation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(filterValidation).getBytes());
            }
            
            // Generate report data
            VisitStatisticsReport report;
            if (veterinarianId != null || (species != null && !species.trim().isEmpty())) {
                // Use filtered report
                ReportFilter filter = new ReportFilter(startDate, endDate);
                filter.setVeterinarianId(veterinarianId);
                if (species != null && !species.trim().isEmpty()) {
                    filter.setSpecies(java.util.List.of(species.trim()));
                }
                report = reportService.generateVisitStatistics(filter);
            } else {
                // Use simple date range report
                report = reportService.generateVisitStatistics(startDate, endDate);
            }
            
            if (report == null) {
                logger.warn("No visit statistics data found for date range: {} to {}", startDate, endDate);
                return ResponseEntity.status(404)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"No data found for the specified date range and filters\"}".getBytes());
            }
            
            // Generate export data
            byte[] exportData;
            String normalizedFormat = parameterValidator.getNormalizedFormat(format);
            
            if ("csv".equals(normalizedFormat)) {
                exportData = reportExportService.exportVisitStatisticsToCSV(report);
            } else {
                exportData = reportExportService.exportVisitStatisticsToPdf(report);
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for visit statistics report");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            // Generate response headers
            String filename = reportExportService.getExportFilename("visit-statistics", normalizedFormat, startDate, endDate);
            String mimeType = reportExportService.getMimeType(normalizedFormat);
            
            logger.info("Successfully exported visit statistics report - Method: {}, Size: {} bytes, Filename: {}", 
                       method, exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (org.springframework.security.access.AccessDeniedException e) {
            logger.error("Access denied for visit statistics export", e);
            return ResponseEntity.status(403)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Access denied. Insufficient permissions for export.\"}".getBytes());
                    
        } catch (Exception e) {
            logger.error("Error exporting visit statistics report", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
    
    /**
     * Export visit statistics report with filters
     * POST /api/reports/export/visits/filtered?format=pdf
     */
    @PostMapping("/export/visits/filtered")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportVisitStatisticsWithFilter(
            @RequestBody ReportFilter filter,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting visit statistics report with filter: {}, format: {}", filter, format);
        
        try {
            if (!reportExportService.validateExportParameters("visit-statistics", format)) {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Invalid export parameters\"}".getBytes());
            }
            
            byte[] exportData;
            
            if ("csv".equalsIgnoreCase(format)) {
                exportData = reportExportService.exportFilteredVisitStatisticsToCSV(filter);
            } else {
                exportData = reportExportService.exportFilteredVisitStatisticsToPdf(filter);
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for filtered visit statistics");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            String filename = reportExportService.getExportFilename("visit-statistics", format, 
                    filter.getStartDate(), filter.getEndDate());
            String mimeType = reportExportService.getMimeType(format);
            
            logger.info("Successfully exported filtered visit statistics - Size: {} bytes, Filename: {}", 
                       exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (Exception e) {
            logger.error("Error exporting filtered visit statistics", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
    
    /**
     * Export revenue report
     * GET /api/reports/export/revenue?startDate=2024-01-01&endDate=2024-01-31&format=pdf&includeTrends=true
     * POST /api/reports/export/revenue (with form data)
     */
    @GetMapping("/export/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false") String includeTrends,
            @RequestParam(required = false) Long veterinarianId) {
        
        return handleExportRevenueReport("revenue", startDate, endDate, format, includeTrends, veterinarianId, "GET");
    }
    
    /**
     * Export revenue report via POST
     * POST /api/reports/export/revenue?format=pdf
     */
    @PostMapping("/export/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRevenueReportPost(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false") String includeTrends,
            @RequestParam(required = false) Long veterinarianId) {
        
        return handleExportRevenueReport("revenue", startDate, endDate, format, includeTrends, veterinarianId, "POST");
    }
    
    /**
     * Common handler for revenue report export
     */
    private ResponseEntity<byte[]> handleExportRevenueReport(String reportType, LocalDate startDate, 
                                                           LocalDate endDate, String format, 
                                                           String includeTrendsStr, Long veterinarianId, 
                                                           String method) {
        
        logger.info("Exporting revenue report - Method: {}, DateRange: {} to {}, Format: {}, IncludeTrends: {}, VetId: {}", 
                   method, startDate, endDate, format, includeTrendsStr, veterinarianId);
        
        try {
            // Comprehensive parameter validation
            ExportParameterValidator.ValidationResult validation = 
                parameterValidator.validateExportParameters(reportType, format, startDate, endDate);
            
            if (!validation.isValid()) {
                logger.warn("Export parameter validation failed: {}", validation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(validation).getBytes());
            }
            
            // Validate boolean parameter
            ExportParameterValidator.ValidationResult booleanValidation = 
                parameterValidator.validateBooleanParameter(includeTrendsStr, "includeTrends");
            
            if (!booleanValidation.isValid()) {
                logger.warn("Boolean parameter validation failed: {}", booleanValidation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(booleanValidation).getBytes());
            }
            
            // Validate optional filter parameters
            ExportParameterValidator.ValidationResult filterValidation = 
                parameterValidator.validateFilterParameters(veterinarianId, null);
            
            if (!filterValidation.isValid()) {
                logger.warn("Export filter validation failed: {}", filterValidation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(filterValidation).getBytes());
            }
            
            // Parse boolean parameter
            boolean includeTrends = parameterValidator.parseBooleanParameter(includeTrendsStr, false);
            
            // Generate report data
            RevenueReport report;
            if (veterinarianId != null) {
                // Use filtered report
                ReportFilter filter = new ReportFilter(startDate, endDate);
                filter.setVeterinarianId(veterinarianId);
                report = reportService.generateRevenueReport(filter);
            } else {
                // Use simple date range report
                report = reportService.generateRevenueReport(startDate, endDate, includeTrends);
            }
            
            if (report == null) {
                logger.warn("No revenue data found for date range: {} to {}", startDate, endDate);
                return ResponseEntity.status(404)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"No data found for the specified date range and filters\"}".getBytes());
            }
            
            // Generate export data
            byte[] exportData;
            String normalizedFormat = parameterValidator.getNormalizedFormat(format);
            
            if ("csv".equals(normalizedFormat)) {
                exportData = reportExportService.exportRevenueReportToCSV(report);
            } else {
                exportData = reportExportService.exportRevenueReportToPdf(report);
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for revenue report");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            // Generate response headers
            String filename = reportExportService.getExportFilename("revenue", normalizedFormat, startDate, endDate);
            String mimeType = reportExportService.getMimeType(normalizedFormat);
            
            logger.info("Successfully exported revenue report - Method: {}, Size: {} bytes, Filename: {}", 
                       method, exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (org.springframework.security.access.AccessDeniedException e) {
            logger.error("Access denied for revenue report export", e);
            return ResponseEntity.status(403)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Access denied. Insufficient permissions for export.\"}".getBytes());
                    
        } catch (Exception e) {
            logger.error("Error exporting revenue report", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
    
    /**
     * Export revenue report with filters
     * POST /api/reports/export/revenue/filtered?format=pdf
     */
    @PostMapping("/export/revenue/filtered")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRevenueReportWithFilter(
            @RequestBody ReportFilter filter,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting revenue report with filter: {}, format: {}", filter, format);
        
        try {
            if (!reportExportService.validateExportParameters("revenue", format)) {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Invalid export parameters\"}".getBytes());
            }
            
            byte[] exportData;
            
            if ("csv".equalsIgnoreCase(format)) {
                exportData = reportExportService.exportFilteredRevenueReportToCSV(filter);
            } else {
                exportData = reportExportService.exportFilteredRevenueReportToPdf(filter);
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for filtered revenue report");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            String filename = reportExportService.getExportFilename("revenue", format, 
                    filter.getStartDate(), filter.getEndDate());
            String mimeType = reportExportService.getMimeType(format);
            
            logger.info("Successfully exported filtered revenue report - Size: {} bytes, Filename: {}", 
                       exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (Exception e) {
            logger.error("Error exporting filtered revenue report", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
    
    /**
     * Export dashboard metrics
     * GET /api/reports/export/dashboard?format=pdf&date=2024-01-15
     * POST /api/reports/export/dashboard (with form data)
     */
    @GetMapping("/export/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDashboardMetrics(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return handleExportDashboardMetrics("dashboard", format, date, "GET");
    }
    
    /**
     * Export dashboard metrics via POST
     * POST /api/reports/export/dashboard?format=pdf
     */
    @PostMapping("/export/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDashboardMetricsPost(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        return handleExportDashboardMetrics("dashboard", format, date, "POST");
    }
    
    /**
     * Common handler for dashboard metrics export
     */
    private ResponseEntity<byte[]> handleExportDashboardMetrics(String reportType, String format, 
                                                              LocalDate date, String method) {
        
        logger.info("Exporting dashboard metrics - Method: {}, Format: {}, Date: {}", method, format, date);
        
        try {
            // Use current date if not provided
            LocalDate targetDate = date != null ? date : LocalDate.now();
            
            // Comprehensive parameter validation
            ExportParameterValidator.ValidationResult validation = 
                parameterValidator.validateExportParameters(reportType, format, targetDate, targetDate);
            
            if (!validation.isValid()) {
                logger.warn("Export parameter validation failed: {}", validation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(validation).getBytes());
            }
            
            // Generate report data
            DashboardMetrics metrics = date != null ? 
                    reportService.getDashboardMetrics(date) : 
                    reportService.getDashboardMetrics();
                    
            if (metrics == null) {
                logger.warn("No dashboard metrics data found for date: {}", date);
                return ResponseEntity.status(404)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"No dashboard data found for the specified date\"}".getBytes());
            }
            
            // Generate export data
            byte[] exportData;
            String normalizedFormat = parameterValidator.getNormalizedFormat(format);
            
            if ("csv".equals(normalizedFormat)) {
                exportData = reportExportService.exportDashboardMetricsToCSV(metrics);
            } else {
                exportData = reportExportService.exportDashboardMetricsToPdf(metrics);
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for dashboard metrics");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            // Generate response headers
            String filename = reportExportService.getExportFilename("dashboard", normalizedFormat, targetDate, targetDate);
            String mimeType = reportExportService.getMimeType(normalizedFormat);
            
            logger.info("Successfully exported dashboard metrics - Method: {}, Size: {} bytes, Filename: {}", 
                       method, exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (org.springframework.security.access.AccessDeniedException e) {
            logger.error("Access denied for dashboard metrics export", e);
            return ResponseEntity.status(403)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Access denied. Insufficient permissions for export.\"}".getBytes());
                    
        } catch (Exception e) {
            logger.error("Error exporting dashboard metrics", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
    
    /**
     * Export visit statistics by veterinarian
     * GET /api/reports/export/visits/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31&format=pdf
     */
    @GetMapping("/export/visits/by-veterinarian")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportVisitStatisticsByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting visit statistics by veterinarian for date range: {} to {}, format: {}", 
                   startDate, endDate, format);
        
        try {
            if (!reportExportService.validateExportParameters("visit-statistics-by-veterinarian", format)) {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Invalid export parameters\"}".getBytes());
            }
            
            List<VisitStatisticsReport> reports = reportService.generateVisitStatisticsByVeterinarian(startDate, endDate);
            byte[] exportData;
            
            if ("csv".equalsIgnoreCase(format)) {
                exportData = reportExportService.exportMultipleVisitStatisticsToCSV(reports);
            } else {
                exportData = reportExportService.exportMultipleVisitStatisticsToPdf(reports, "Visit Statistics by Veterinarian");
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for visit statistics by veterinarian");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            String filename = reportExportService.getExportFilename("visit-statistics-by-veterinarian", format, startDate, endDate);
            String mimeType = reportExportService.getMimeType(format);
            
            logger.info("Successfully exported visit statistics by veterinarian - Size: {} bytes, Filename: {}", 
                       exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (Exception e) {
            logger.error("Error exporting visit statistics by veterinarian", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
    
    /**
     * Export revenue report by veterinarian
     * GET /api/reports/export/revenue/by-veterinarian?startDate=2024-01-01&endDate=2024-01-31&format=pdf
     */
    @GetMapping("/export/revenue/by-veterinarian")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRevenueReportByVeterinarian(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {
        
        logger.info("Exporting revenue report by veterinarian for date range: {} to {}, format: {}", 
                   startDate, endDate, format);
        
        try {
            if (!reportExportService.validateExportParameters("revenue-by-veterinarian", format)) {
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Invalid export parameters\"}".getBytes());
            }
            
            List<RevenueReport> reports = reportService.generateRevenueReportByVeterinarian(startDate, endDate);
            byte[] exportData;
            
            if ("csv".equalsIgnoreCase(format)) {
                exportData = reportExportService.exportMultipleRevenueReportsToCSV(reports);
            } else {
                exportData = reportExportService.exportMultipleRevenueReportsToPdf(reports, "Revenue Report by Veterinarian");
            }
            
            if (exportData == null || exportData.length == 0) {
                logger.error("Export service returned empty data for revenue report by veterinarian");
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export generation failed - no data produced\"}".getBytes());
            }
            
            String filename = reportExportService.getExportFilename("revenue-by-veterinarian", format, startDate, endDate);
            String mimeType = reportExportService.getMimeType(format);
            
            logger.info("Successfully exported revenue report by veterinarian - Size: {} bytes, Filename: {}", 
                       exportData.length, filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate, private")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Download-Options", "noopen")
                    .body(exportData);
                    
        } catch (Exception e) {
            logger.error("Error exporting revenue report by veterinarian", e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
        }
    }
}