package com.petclinic.backend.controller;

import com.petclinic.backend.service.ExportDiagnosticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for export diagnostic endpoints
 * Provides comprehensive testing and analysis of export request flow
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@RestController
@RequestMapping("/api/diagnostics/export")
public class ExportDiagnosticController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportDiagnosticController.class);
    
    @Autowired
    private ExportDiagnosticService exportDiagnosticService;
    
    /**
     * Trace complete export request flow
     * GET /api/diagnostics/export/trace?reportType=visits&format=pdf&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/trace")
    public ResponseEntity<ExportDiagnosticService.ExportDiagnosticInfo> traceExportRequest(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        
        logger.info("Export trace request - ReportType: {}, Format: {}, DateRange: {} to {}", 
                   reportType, format, startDate, endDate);
        
        try {
            // Extract request headers
            Map<String, String> requestHeaders = extractRequestHeaders(request);
            
            ExportDiagnosticService.ExportDiagnosticInfo diagnostic = 
                exportDiagnosticService.traceExportRequest(reportType, format, startDate, endDate, requestHeaders);
            
            return ResponseEntity.ok(diagnostic);
            
        } catch (Exception e) {
            logger.error("Error tracing export request", e);
            
            ExportDiagnosticService.ExportDiagnosticInfo errorDiagnostic = 
                new ExportDiagnosticService.ExportDiagnosticInfo("ERROR", reportType, format);
            errorDiagnostic.setErrorMessage("Trace failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorDiagnostic);
        }
    }
    
    /**
     * Test authentication flow
     * GET /api/diagnostics/export/test/authentication
     */
    @GetMapping("/test/authentication")
    public ResponseEntity<Map<String, Object>> testAuthentication(HttpServletRequest request) {
        logger.info("Testing authentication flow for export requests");
        
        try {
            Map<String, String> requestHeaders = extractRequestHeaders(request);
            Map<String, Object> result = exportDiagnosticService.testAuthenticationFlow(requestHeaders);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing authentication flow", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Authentication test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Test CSRF token handling
     * GET /api/diagnostics/export/test/csrf
     */
    @GetMapping("/test/csrf")
    public ResponseEntity<Map<String, Object>> testCsrfToken(HttpServletRequest request) {
        logger.info("Testing CSRF token handling for export requests");
        
        try {
            Map<String, String> requestHeaders = extractRequestHeaders(request);
            Map<String, Object> result = exportDiagnosticService.testCsrfTokenHandling(requestHeaders);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing CSRF token handling", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "CSRF test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Test routing configuration
     * GET /api/diagnostics/export/test/routing?reportType=visits
     */
    @GetMapping("/test/routing")
    public ResponseEntity<Map<String, Object>> testRouting(@RequestParam String reportType) {
        logger.info("Testing routing configuration for report type: {}", reportType);
        
        try {
            Map<String, Object> result = exportDiagnosticService.testRoutingConfiguration(reportType);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing routing configuration", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Routing test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Test response header configuration
     * GET /api/diagnostics/export/test/headers?format=pdf
     */
    @GetMapping("/test/headers")
    public ResponseEntity<Map<String, Object>> testResponseHeaders(@RequestParam String format) {
        logger.info("Testing response header configuration for format: {}", format);
        
        try {
            Map<String, Object> result = exportDiagnosticService.testResponseHeaderConfiguration(format);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing response headers", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Response header test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Test complete export flow end-to-end
     * GET /api/diagnostics/export/test/complete?reportType=visits&format=pdf&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/test/complete")
    public ResponseEntity<Map<String, Object>> testCompleteFlow(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Testing complete export flow - ReportType: {}, Format: {}, DateRange: {} to {}", 
                   reportType, format, startDate, endDate);
        
        try {
            Map<String, Object> result = exportDiagnosticService.testCompleteExportFlow(
                reportType, format, startDate, endDate);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing complete export flow", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Complete flow test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Validate export service dependencies
     * GET /api/diagnostics/export/dependencies
     */
    @GetMapping("/dependencies")
    public ResponseEntity<Map<String, Object>> validateDependencies() {
        logger.info("Validating export service dependencies");
        
        try {
            Map<String, Object> result = exportDiagnosticService.validateExportDependencies();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error validating dependencies", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Dependency validation failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Get error analysis for a specific request
     * GET /api/diagnostics/export/analysis/{requestId}
     */
    @GetMapping("/analysis/{requestId}")
    public ResponseEntity<Map<String, Object>> getErrorAnalysis(@PathVariable String requestId) {
        logger.info("Getting error analysis for request ID: {}", requestId);
        
        try {
            Map<String, Object> result = exportDiagnosticService.getErrorAnalysis(requestId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting error analysis", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Error analysis failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Generate system health report
     * GET /api/diagnostics/export/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        logger.info("Generating export system health report");
        
        try {
            Map<String, Object> result = exportDiagnosticService.generateSystemHealthReport();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error generating system health report", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "Health report generation failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Test all export endpoints with sample data
     * GET /api/diagnostics/export/test/all
     */
    @GetMapping("/test/all")
    public ResponseEntity<Map<String, Object>> testAllExportEndpoints() {
        logger.info("Testing all export endpoints with sample data");
        
        try {
            Map<String, Object> results = new HashMap<>();
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            
            String[] reportTypes = {"visits", "revenue", "dashboard"};
            String[] formats = {"pdf", "csv"};
            
            for (String reportType : reportTypes) {
                Map<String, Object> reportResults = new HashMap<>();
                
                for (String format : formats) {
                    try {
                        Map<String, Object> testResult = exportDiagnosticService.testCompleteExportFlow(
                            reportType, format, startDate, endDate);
                        reportResults.put(format, testResult);
                        
                    } catch (Exception e) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("status", "ERROR");
                        errorResult.put("error", e.getMessage());
                        reportResults.put(format, errorResult);
                    }
                }
                
                results.put(reportType, reportResults);
            }
            
            results.put("testDate", LocalDate.now());
            results.put("dateRange", startDate + " to " + endDate);
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error testing all export endpoints", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("error", "All endpoints test failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * Extract request headers into a map
     */
    private Map<String, String> extractRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        
        return headers;
    }
}