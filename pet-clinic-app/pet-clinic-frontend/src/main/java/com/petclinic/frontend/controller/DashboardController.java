package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.DashboardMetrics;
import com.petclinic.frontend.model.ReportFilter;
import com.petclinic.frontend.service.DashboardService;
import com.petclinic.frontend.service.ReportService;
import com.petclinic.frontend.util.RoutingValidator;
import com.petclinic.frontend.util.ExportParameterValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

/**
 * Dashboard Controller
 * 
 * Handles web requests for dashboard and reporting operations.
 * Provides endpoints for viewing dashboard metrics, generating reports, and exporting data.
 * 
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final RoutingValidator routingValidator;
    private final ExportParameterValidator parameterValidator;

    @Autowired
    public DashboardController(DashboardService dashboardService, ReportService reportService, 
                             RoutingValidator routingValidator, ExportParameterValidator parameterValidator) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
        this.routingValidator = routingValidator;
        this.parameterValidator = parameterValidator;
        
        // Log routing configuration on startup
        routingValidator.logRoutingConfiguration();
    }

    /**
     * Show main dashboard
     */
    @GetMapping
    public String showDashboard(Model model) {
        try {
            DashboardMetrics metrics = null;
            Map<String, Object> weeklyTrends = new java.util.HashMap<>();
            Map<String, Long> petsBySpecies = new java.util.HashMap<>();
            
            try {
                metrics = dashboardService.getRealTimeDashboardMetrics().block();
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                if (e.getStatusCode().value() == 503) {
                    logger.warn("Backend service unavailable for user: {} - Status: {}", 
                              getCurrentUsername(), e.getStatusCode());
                    model.addAttribute("warning", "Some dashboard features are temporarily unavailable. " +
                                                "Core functionality is still accessible.");
                    // Create minimal metrics to prevent template errors
                    metrics = createFallbackMetrics();
                } else {
                    throw e;
                }
            } catch (Exception e) {
                logger.warn("Error loading dashboard metrics for user: {} - {}", 
                          getCurrentUsername(), e.getMessage());
                model.addAttribute("warning", "Unable to load real-time metrics. Showing cached data.");
                metrics = createFallbackMetrics();
            }
            
            // Get additional data for dashboard with fallback
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(7);
            
            try {
                weeklyTrends = dashboardService.getPerformanceIndicators(weekAgo, today).block();
            } catch (Exception e) {
                logger.warn("Could not load weekly trends for user: {} - {}", 
                          getCurrentUsername(), e.getMessage());
                weeklyTrends = createFallbackTrends();
            }
            
            try {
                petsBySpecies = dashboardService.getActivePetsBySpecies().block();
            } catch (Exception e) {
                logger.warn("Could not load pets by species for user: {} - {}", 
                          getCurrentUsername(), e.getMessage());
                petsBySpecies = createFallbackPetsBySpecies();
            }
            
            model.addAttribute("metrics", metrics);
            model.addAttribute("weeklyTrends", weeklyTrends);
            model.addAttribute("petsBySpecies", petsBySpecies);
            
            return "dashboard/index";
        } catch (Exception e) {
            logger.error("Error loading dashboard for user: {}", getCurrentUsername(), e);
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            model.addAttribute("metrics", createFallbackMetrics());
            return "dashboard/index";
        }
    }
    
    /**
     * Get current username for logging
     */
    private String getCurrentUsername() {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Create fallback metrics when backend is unavailable
     */
    private DashboardMetrics createFallbackMetrics() {
        DashboardMetrics fallback = new DashboardMetrics();
        fallback.setTodayAppointments(0);
        fallback.setTodayCompletedVisits(0);
        fallback.setTodayPendingVisits(0);
        fallback.setTodayRevenue(java.math.BigDecimal.ZERO);
        fallback.setTotalActivePets(0);
        fallback.setTotalActiveOwners(0);
        fallback.setTotalVeterinarians(0);
        fallback.setAppointmentCompletionRate(0.0);
        return fallback;
    }
    
    /**
     * Create fallback trends when backend is unavailable
     */
    private Map<String, Object> createFallbackTrends() {
        Map<String, Object> fallback = new java.util.HashMap<>();
        fallback.put("weeklyGrowth", 0.0);
        fallback.put("completionRate", 0.0);
        fallback.put("averageRevenue", 0.0);
        return fallback;
    }
    
    /**
     * Create fallback pets by species when backend is unavailable
     */
    private Map<String, Long> createFallbackPetsBySpecies() {
        Map<String, Long> fallback = new java.util.HashMap<>();
        fallback.put("Dogs", 0L);
        fallback.put("Cats", 0L);
        fallback.put("Birds", 0L);
        fallback.put("Other", 0L);
        return fallback;
    }

    /**
     * Show dashboard for specific date
     */
    @GetMapping("/date/{date}")
    public String showDashboardForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        
        try {
            DashboardMetrics metrics = dashboardService.getDashboardMetricsForDate(date).block();
            model.addAttribute("metrics", metrics);
            model.addAttribute("selectedDate", date);
            
            return "dashboard/index";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard for date " + date + ": " + e.getMessage());
            return "dashboard/index";
        }
    }

    /**
     * Show reports page - Admin only
     */
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String showReports(Model model) {
        try {
            // Set default date range (last 30 days)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            model.addAttribute("defaultStartDate", startDate);
            model.addAttribute("defaultEndDate", endDate);
            
            // Get available filter options
            ReportFilter filterOptions = reportService.getAvailableFilterOptions().block();
            model.addAttribute("filterOptions", filterOptions);
            
            return "dashboard/reports";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading reports page: " + e.getMessage());
            return "dashboard/reports";
        }
    }

    /**
     * Generate visit statistics report - Admin only
     */
    @GetMapping("/reports/visits")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateVisitStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String species,
            Model model) {
        
        try {
            Map<String, Object> report;
            
            if (veterinarianId != null || species != null) {
                // Use filtered report
                ReportFilter filter = new ReportFilter(startDate, endDate);
                filter.setVeterinarianId(veterinarianId);
                if (species != null && !species.trim().isEmpty()) {
                    filter.setSpecies(java.util.List.of(species));
                }
                report = reportService.getVisitStatisticsWithFilter(filter).block();
            } else {
                // Use simple date range report
                report = reportService.getVisitStatistics(startDate, endDate).block();
            }
            
            model.addAttribute("visitReport", report);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("veterinarianId", veterinarianId);
            model.addAttribute("species", species);
            
            return "dashboard/visit-report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating visit statistics: " + e.getMessage());
            return "dashboard/reports";
        }
    }

    /**
     * Generate revenue report - Admin only
     */
    @GetMapping("/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "true") boolean includeTrends,
            @RequestParam(required = false) Long veterinarianId,
            Model model) {
        
        try {
            Map<String, Object> report;
            
            if (veterinarianId != null) {
                // Use filtered report
                ReportFilter filter = new ReportFilter(startDate, endDate);
                filter.setVeterinarianId(veterinarianId);
                report = reportService.getRevenueReportWithFilter(filter).block();
            } else {
                // Use simple date range report
                report = reportService.getRevenueReport(startDate, endDate, includeTrends).block();
            }
            
            model.addAttribute("revenueReport", report);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("includeTrends", includeTrends);
            model.addAttribute("veterinarianId", veterinarianId);
            
            return "dashboard/revenue-report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating revenue report: " + e.getMessage());
            return "dashboard/reports";
        }
    }

    /**
     * Generate dashboard summary report - Admin only
     */
    @GetMapping("/reports/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateDashboardReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        try {
            // Get dashboard metrics for the date range
            DashboardMetrics metrics = null;
            try {
                metrics = dashboardService.getDashboardMetricsForDate(startDate).block();
            } catch (Exception e) {
                logger.warn("Could not load dashboard metrics for date {}: {}", startDate, e.getMessage());
                // Create empty metrics object to prevent template errors
                metrics = new DashboardMetrics();
            }
            
            // Get additional analytics data with error handling
            Map<String, Object> visitsByVet = new java.util.HashMap<>();
            Map<String, Object> visitsBySpecies = new java.util.HashMap<>();
            Map<String, Object> revenueByVet = new java.util.HashMap<>();
            Map<String, Object> utilization = new java.util.HashMap<>();
            
            try {
                visitsByVet = reportService.getVisitStatisticsByVeterinarian(startDate, endDate)
                    .onErrorReturn(new java.util.HashMap<>()).block();
            } catch (Exception e) {
                logger.warn("Could not load visit statistics by veterinarian: {}", e.getMessage());
            }
            
            try {
                visitsBySpecies = reportService.getVisitStatisticsBySpecies(startDate, endDate)
                    .onErrorReturn(new java.util.HashMap<>()).block();
            } catch (Exception e) {
                logger.warn("Could not load visit statistics by species: {}", e.getMessage());
            }
            
            try {
                revenueByVet = reportService.getRevenueReportByVeterinarian(startDate, endDate)
                    .onErrorReturn(new java.util.HashMap<>()).block();
            } catch (Exception e) {
                logger.warn("Could not load revenue by veterinarian: {}", e.getMessage());
            }
            
            try {
                utilization = dashboardService.getVeterinarianUtilization(startDate, endDate)
                    .onErrorReturn(new java.util.HashMap<>()).block();
            } catch (Exception e) {
                logger.warn("Could not load utilization data: {}", e.getMessage());
            }
            
            // Calculate summary statistics with null safety
            int totalVisits = 0;
            double totalRevenue = 0.0;
            int totalAnimals = 0;
            int activeVets = 0;
            
            if (visitsByVet != null && !visitsByVet.isEmpty()) {
                activeVets = visitsByVet.size();
                for (Object value : visitsByVet.values()) {
                    if (value instanceof Number) {
                        totalVisits += ((Number) value).intValue();
                    }
                }
            }
            
            if (revenueByVet != null && !revenueByVet.isEmpty()) {
                for (Object value : revenueByVet.values()) {
                    if (value instanceof Number) {
                        totalRevenue += ((Number) value).doubleValue();
                    }
                }
            }
            
            if (visitsBySpecies != null && !visitsBySpecies.isEmpty()) {
                for (Object value : visitsBySpecies.values()) {
                    if (value instanceof Number) {
                        totalAnimals += ((Number) value).intValue();
                    }
                }
            }
            
            // Add all attributes to model
            model.addAttribute("dashboardMetrics", metrics);
            model.addAttribute("visitsByVet", visitsByVet);
            model.addAttribute("visitsBySpecies", visitsBySpecies);
            model.addAttribute("revenueByVet", revenueByVet);
            model.addAttribute("utilization", utilization);
            model.addAttribute("totalVisits", totalVisits);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalAnimals", totalAnimals);
            model.addAttribute("activeVets", activeVets);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            
            logger.info("Dashboard report generated successfully for date range {} to {}", startDate, endDate);
            return "dashboard/dashboard-report";
            
        } catch (Exception e) {
            logger.error("Error generating dashboard report for date range {} to {}", startDate, endDate, e);
            model.addAttribute("error", "Error generating dashboard report: " + e.getMessage());
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            return "dashboard/reports";
        }
    }

    /**
     * Show analytics page - Admin only
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        try {
            // Set default date range if not provided
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            // Initialize with empty data to prevent template errors
            Map<String, Object> visitsByVet = new java.util.HashMap<>();
            Map<String, Object> visitsBySpecies = new java.util.HashMap<>();
            Map<String, Object> revenueByVet = new java.util.HashMap<>();
            Map<String, Object> monthlyTrends = new java.util.HashMap<>();
            Map<String, Object> utilization = new java.util.HashMap<>();
            
            // Try to get analytics data with timeout handling
            try {
                visitsByVet = reportService.getVisitStatisticsByVeterinarian(startDate, endDate)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorReturn(new java.util.HashMap<>())
                    .block();
            } catch (Exception e) {
                System.err.println("Error loading visit statistics by veterinarian: " + e.getMessage());
            }
            
            try {
                visitsBySpecies = reportService.getVisitStatisticsBySpecies(startDate, endDate)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorReturn(new java.util.HashMap<>())
                    .block();
            } catch (Exception e) {
                System.err.println("Error loading visit statistics by species: " + e.getMessage());
            }
            
            try {
                revenueByVet = reportService.getRevenueReportByVeterinarian(startDate, endDate)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorReturn(new java.util.HashMap<>())
                    .block();
            } catch (Exception e) {
                System.err.println("Error loading revenue by veterinarian: " + e.getMessage());
            }
            
            try {
                monthlyTrends = reportService.getMonthlyRevenueTrends(12)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorReturn(new java.util.HashMap<>())
                    .block();
            } catch (Exception e) {
                System.err.println("Error loading monthly trends: " + e.getMessage());
            }
            
            try {
                utilization = dashboardService.getVeterinarianUtilization(startDate, endDate)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .onErrorReturn(new java.util.HashMap<>())
                    .block();
            } catch (Exception e) {
                System.err.println("Error loading utilization data: " + e.getMessage());
            }
            
            // Calculate totals for KPIs
            int totalVisits = 0;
            double totalRevenue = 0.0;
            int totalAnimals = 0;
            int activeVets = 0;
            
            if (visitsByVet != null && !visitsByVet.isEmpty()) {
                activeVets = visitsByVet.size();
                for (Object value : visitsByVet.values()) {
                    if (value instanceof Number) {
                        totalVisits += ((Number) value).intValue();
                    }
                }
            }
            
            if (revenueByVet != null && !revenueByVet.isEmpty()) {
                for (Object value : revenueByVet.values()) {
                    if (value instanceof Number) {
                        totalRevenue += ((Number) value).doubleValue();
                    }
                }
            }
            
            if (visitsBySpecies != null && !visitsBySpecies.isEmpty()) {
                for (Object value : visitsBySpecies.values()) {
                    if (value instanceof Number) {
                        totalAnimals += ((Number) value).intValue();
                    }
                }
            }
            
            model.addAttribute("visitsByVet", visitsByVet);
            model.addAttribute("visitsBySpecies", visitsBySpecies);
            model.addAttribute("revenueByVet", revenueByVet);
            model.addAttribute("monthlyTrends", monthlyTrends);
            model.addAttribute("utilization", utilization);
            model.addAttribute("totalVisits", totalVisits);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalAnimals", totalAnimals);
            model.addAttribute("activeVets", activeVets);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            
            return "dashboard/analytics";
        } catch (Exception e) {
            System.err.println("Error in analytics controller: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading analytics: " + e.getMessage());
            model.addAttribute("startDate", startDate != null ? startDate : LocalDate.now().minusDays(30));
            model.addAttribute("endDate", endDate != null ? endDate : LocalDate.now());
            return "dashboard/analytics";
        }
    }

    /**
     * Export report with proper authentication flow and enhanced parameter validation
     * Supports both GET and POST methods for different use cases
     * Admin only access
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 3.2, 3.4, 3.5
     */
    @GetMapping("/export/{reportType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportReportGet(
            @PathVariable String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false") String includeTrends,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String species,
            jakarta.servlet.http.HttpServletRequest request) {
        
        return exportReport(reportType, startDate, endDate, format, includeTrends, 
                          veterinarianId, species, request, "GET");
    }

    /**
     * Export report via POST (requires CSRF token) - Admin only
     * Validates: Requirements 2.2, 2.3, 3.2, 3.4, 3.5
     */
    @PostMapping("/export/{reportType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportReportPost(
            @PathVariable String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "false") String includeTrends,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) String species,
            jakarta.servlet.http.HttpServletRequest request) {
        
        return exportReport(reportType, startDate, endDate, format, includeTrends, 
                          veterinarianId, species, request, "POST");
    }

    /**
     * Common export logic for both GET and POST requests with enhanced parameter validation
     */
    private ResponseEntity<byte[]> exportReport(
            String reportType,
            LocalDate startDate,
            LocalDate endDate,
            String format,
            String includeTrendsStr,
            Long veterinarianId,
            String species,
            jakarta.servlet.http.HttpServletRequest request,
            String method) {
        
        logger.info("Export request - User: {}, Method: {}, ReportType: {}, Format: {}, DateRange: {} to {}, VetId: {}, Species: {}", 
                   request.getRemoteUser(), method, reportType, format, startDate, endDate, veterinarianId, species);
        
        try {
            // Comprehensive parameter validation
            ExportParameterValidator.ValidationResult validation = 
                parameterValidator.validateExportParameters(reportType, format, startDate, endDate);
            
            if (!validation.isValid()) {
                logger.warn("Frontend export parameter validation failed: {}", validation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(validation).getBytes());
            }
            
            // Validate boolean parameter
            ExportParameterValidator.ValidationResult booleanValidation = 
                parameterValidator.validateBooleanParameter(includeTrendsStr, "includeTrends");
            
            if (!booleanValidation.isValid()) {
                logger.warn("Frontend boolean parameter validation failed: {}", booleanValidation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(booleanValidation).getBytes());
            }
            
            // Parse boolean parameter
            boolean includeTrends = parameterValidator.parseBooleanParameter(includeTrendsStr, false);
            
            // Validate optional filter parameters
            ExportParameterValidator.ValidationResult filterValidation = 
                parameterValidator.validateFilterParameters(veterinarianId, species);
            
            if (!filterValidation.isValid()) {
                logger.warn("Frontend filter validation failed: {}", filterValidation.getFormattedErrorMessage());
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(parameterValidator.createErrorResponseBody(filterValidation).getBytes());
            }
            
            // Validate routing configuration
            String normalizedReportType = parameterValidator.getNormalizedReportType(reportType);
            String normalizedFormat = parameterValidator.getNormalizedFormat(format);
            
            if (!routingValidator.validateExportEndpointRouting(normalizedReportType, normalizedFormat)) {
                logger.error("Export endpoint routing validation failed - reportType: {}, format: {}", normalizedReportType, normalizedFormat);
                return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Invalid routing configuration for report type and format\"}".getBytes());
            }
            
            // Validate user authentication
            if (request.getUserPrincipal() == null) {
                logger.warn("Export request without authentication - redirecting to login");
                return ResponseEntity.status(401)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Authentication required. Please log in.\",\"redirect\":\"/login\"}".getBytes());
            }
            
            // Validate session
            if (request.getSession(false) == null) {
                logger.warn("Export request with invalid session - User: {}", request.getRemoteUser());
                return ResponseEntity.status(401)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Session expired. Please log in again.\",\"redirect\":\"/login\"}".getBytes());
            }
            
            // Log authentication details for debugging
            logger.debug("Export authentication - User: {}, Method: {}, Session: {}, AuthType: {}", 
                        request.getRemoteUser(), method,
                        request.getSession(false).getId(),
                        request.getAuthType());
            
            ResponseEntity<byte[]> response;
            
            switch (normalizedReportType) {
                case "visits":
                    response = reportService.exportVisitStatistics(startDate, endDate, normalizedFormat).block();
                    break;
                case "revenue":
                    response = reportService.exportRevenueReport(startDate, endDate, normalizedFormat, includeTrends).block();
                    break;
                case "dashboard":
                    response = reportService.exportDashboardMetrics(normalizedFormat, startDate).block();
                    break;
                default:
                    logger.error("Unhandled report type: {} by user: {}", normalizedReportType, request.getRemoteUser());
                    return ResponseEntity.badRequest()
                            .header("Content-Type", "application/json")
                            .body("{\"error\":\"Invalid report type. Supported types: visits, revenue, dashboard\"}".getBytes());
            }
            
            if (response == null) {
                logger.error("Export service returned null response for report type: {} by user: {}", 
                           normalizedReportType, request.getRemoteUser());
                return ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Export service unavailable\"}".getBytes());
            }
            
            logger.info("Export successful - User: {}, Method: {}, ReportType: {}, Format: {}, Size: {} bytes", 
                       request.getRemoteUser(), method, normalizedReportType, normalizedFormat, 
                       response.getBody() != null ? response.getBody().length : 0);
            
            return response;
            
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            logger.error("Backend API error during export - User: {}, Status: {}, Response: {}", 
                        request.getRemoteUser(), e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode().value() == 401) {
                return ResponseEntity.status(401)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Backend authentication failed. Please log in again.\",\"redirect\":\"/login\"}".getBytes());
            } else if (e.getStatusCode().value() == 403) {
                return ResponseEntity.status(403)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Access denied. Insufficient permissions for export.\"}".getBytes());
            } else {
                return ResponseEntity.status(e.getStatusCode())
                        .header("Content-Type", "application/json")
                        .body(("{\"error\":\"Export failed: " + e.getMessage() + "\"}").getBytes());
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during export - User: {}", request.getRemoteUser(), e);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Internal server error during export\"}".getBytes());
        }
    }
    
    /**
     * Validate report type
     */
    private boolean isValidReportType(String reportType) {
        if (reportType == null || reportType.trim().isEmpty()) {
            return false;
        }
        String[] validTypes = {"visits", "revenue", "dashboard"};
        for (String validType : validTypes) {
            if (validType.equalsIgnoreCase(reportType.trim())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validate format
     */
    private boolean isValidFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return false;
        }
        String[] validFormats = {"pdf", "csv"};
        for (String validFormat : validFormats) {
            if (validFormat.equalsIgnoreCase(format.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refresh dashboard data
     */
    @PostMapping("/refresh")
    public String refreshDashboard(RedirectAttributes redirectAttributes) {
        try {
            dashboardService.refreshDashboardCache().block();
            redirectAttributes.addFlashAttribute("success", "Dashboard data refreshed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error refreshing dashboard: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    /**
     * AJAX endpoint for real-time metrics
     */
    @GetMapping("/api/metrics")
    @ResponseBody
    public DashboardMetrics getMetrics() {
        return dashboardService.getRealTimeDashboardMetrics().block();
    }

    /**
     * AJAX endpoint for daily appointment metrics
     */
    @GetMapping("/api/appointments/daily")
    @ResponseBody
    public Map<String, Long> getDailyAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return dashboardService.getDailyAppointmentMetrics(targetDate).block();
    }

    /**
     * AJAX endpoint for recent activities
     */
    @GetMapping("/api/activities")
    @ResponseBody
    public java.util.List<DashboardMetrics.RecentActivity> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        
        return dashboardService.getRecentActivities(limit).block();
    }

    /**
     * AJAX endpoint for upcoming appointments
     */
    @GetMapping("/api/appointments/upcoming")
    @ResponseBody
    public java.util.List<DashboardMetrics.UpcomingAppointment> getUpcomingAppointments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        
        return dashboardService.getUpcomingAppointments(days, limit).block();
    }
}