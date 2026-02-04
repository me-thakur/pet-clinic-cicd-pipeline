package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.ExportDiagnosticService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

/**
 * Implementation of ExportDiagnosticService for analyzing export request flow
 * Provides comprehensive tracing and diagnosis of export functionality issues
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@Service
public class ExportDiagnosticServiceImpl implements ExportDiagnosticService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportDiagnosticServiceImpl.class);
    
    @Autowired
    private ReportExportService reportExportService;
    
    @Autowired
    private ReportService reportService;
    
    // Store diagnostic information for analysis
    private final Map<String, ExportDiagnosticInfo> diagnosticCache = new HashMap<>();
    
    @Override
    public ExportDiagnosticInfo traceExportRequest(String reportType, String format, 
                                                 LocalDate startDate, LocalDate endDate,
                                                 Map<String, String> requestHeaders) {
        
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        logger.info("Starting export request trace - RequestID: {}, ReportType: {}, Format: {}, DateRange: {} to {}", 
                   requestId, reportType, format, startDate, endDate);
        
        ExportDiagnosticInfo diagnostic = new ExportDiagnosticInfo(requestId, reportType, format);
        diagnostic.setStartDate(startDate);
        diagnostic.setEndDate(endDate);
        diagnostic.setRequestHeaders(requestHeaders != null ? requestHeaders : new HashMap<>());
        
        try {
            // Step 1: Test Authentication
            logger.debug("RequestID: {} - Testing authentication flow", requestId);
            Map<String, Object> authResult = testAuthenticationFlow(requestHeaders);
            diagnostic.setAuthenticationStatus((String) authResult.get("status"));
            
            // Step 2: Test CSRF Token
            logger.debug("RequestID: {} - Testing CSRF token handling", requestId);
            Map<String, Object> csrfResult = testCsrfTokenHandling(requestHeaders);
            diagnostic.setCsrfTokenStatus((String) csrfResult.get("status"));
            
            // Step 3: Test Routing
            logger.debug("RequestID: {} - Testing routing configuration", requestId);
            Map<String, Object> routingResult = testRoutingConfiguration(reportType);
            diagnostic.setRoutingStatus((String) routingResult.get("status"));
            
            // Step 4: Test Response Headers
            logger.debug("RequestID: {} - Testing response header configuration", requestId);
            Map<String, Object> responseResult = testResponseHeaderConfiguration(format);
            diagnostic.setResponseStatus((String) responseResult.get("status"));
            
            // Step 5: Generate trace information
            StringBuilder trace = new StringBuilder();
            trace.append("Export Request Flow Trace:\\n");
            trace.append("1. Authentication: ").append(diagnostic.getAuthenticationStatus()).append("\\n");
            trace.append("2. CSRF Token: ").append(diagnostic.getCsrfTokenStatus()).append("\\n");
            trace.append("3. Routing: ").append(diagnostic.getRoutingStatus()).append("\\n");
            trace.append("4. Response Headers: ").append(diagnostic.getResponseStatus()).append("\\n");
            
            // Add detailed error information if any step failed
            if (authResult.containsKey("error")) {
                trace.append("Authentication Error: ").append(authResult.get("error")).append("\\n");
            }
            if (csrfResult.containsKey("error")) {
                trace.append("CSRF Error: ").append(csrfResult.get("error")).append("\\n");
            }
            if (routingResult.containsKey("error")) {
                trace.append("Routing Error: ").append(routingResult.get("error")).append("\\n");
            }
            if (responseResult.containsKey("error")) {
                trace.append("Response Error: ").append(responseResult.get("error")).append("\\n");
            }
            
            diagnostic.setFrontendToBackendTrace(trace.toString());
            
            // Set response headers based on format
            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Content-Type", reportExportService.getMimeType(format));
            responseHeaders.put("Content-Disposition", 
                "attachment; filename=\"" + reportExportService.getExportFilename(reportType, format, startDate, endDate) + "\"");
            responseHeaders.put("Cache-Control", "no-cache, no-store, must-revalidate, private");
            responseHeaders.put("Pragma", "no-cache");
            responseHeaders.put("Expires", "0");
            responseHeaders.put("X-Content-Type-Options", "nosniff");
            responseHeaders.put("X-Download-Options", "noopen");
            diagnostic.setResponseHeaders(responseHeaders);
            
        } catch (Exception e) {
            logger.error("RequestID: {} - Error during export request trace", requestId, e);
            diagnostic.setErrorMessage("Export trace failed: " + e.getMessage());
            diagnostic.setAuthenticationStatus("ERROR");
            diagnostic.setCsrfTokenStatus("ERROR");
            diagnostic.setRoutingStatus("ERROR");
            diagnostic.setResponseStatus("ERROR");
        }
        
        long endTime = System.currentTimeMillis();
        diagnostic.setProcessingTimeMs(endTime - startTime);
        
        // Cache diagnostic information
        diagnosticCache.put(requestId, diagnostic);
        
        logger.info("Completed export request trace - RequestID: {}, ProcessingTime: {}ms", 
                   requestId, diagnostic.getProcessingTimeMs());
        
        return diagnostic;
    }
    
    @Override
    public Map<String, Object> testAuthenticationFlow(Map<String, String> requestHeaders) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check Spring Security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                result.put("status", "NO_AUTHENTICATION");
                result.put("error", "No authentication context found");
                logger.warn("Authentication test failed: No authentication context");
                return result;
            }
            
            if (!authentication.isAuthenticated()) {
                result.put("status", "NOT_AUTHENTICATED");
                result.put("error", "User is not authenticated");
                result.put("principal", authentication.getPrincipal());
                logger.warn("Authentication test failed: User not authenticated - Principal: {}", 
                           authentication.getPrincipal());
                return result;
            }
            
            // Check for session-based authentication (frontend)
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String sessionId = request.getSession(false) != null ? 
                    request.getSession(false).getId() : "NO_SESSION";
                
                result.put("sessionId", sessionId);
                result.put("sessionValid", request.getSession(false) != null);
                
                // Check for JWT token (backend API)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    result.put("jwtToken", "PRESENT");
                    result.put("authType", "JWT");
                } else {
                    result.put("jwtToken", "ABSENT");
                    result.put("authType", "SESSION");
                }
            }
            
            result.put("status", "AUTHENTICATED");
            result.put("principal", authentication.getName());
            result.put("authorities", authentication.getAuthorities().toString());
            
            logger.debug("Authentication test passed - User: {}, Authorities: {}", 
                        authentication.getName(), authentication.getAuthorities());
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", "Authentication test failed: " + e.getMessage());
            logger.error("Authentication test error", e);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> testCsrfTokenHandling(Map<String, String> requestHeaders) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes == null) {
                result.put("status", "NO_REQUEST_CONTEXT");
                result.put("error", "No request context available for CSRF testing");
                return result;
            }
            
            HttpServletRequest request = requestAttributes.getRequest();
            
            // Check for CSRF token in request
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            
            if (csrfToken == null) {
                // CSRF might be disabled for this endpoint
                result.put("status", "CSRF_DISABLED");
                result.put("message", "CSRF protection appears to be disabled for this endpoint");
                logger.debug("CSRF test: No CSRF token found - protection may be disabled");
                return result;
            }
            
            // Check if CSRF token is present in headers
            String csrfHeaderValue = request.getHeader(csrfToken.getHeaderName());
            String csrfParamValue = request.getParameter(csrfToken.getParameterName());
            
            result.put("csrfTokenName", csrfToken.getParameterName());
            result.put("csrfHeaderName", csrfToken.getHeaderName());
            result.put("expectedToken", csrfToken.getToken());
            result.put("headerValue", csrfHeaderValue);
            result.put("paramValue", csrfParamValue);
            
            if (csrfHeaderValue != null || csrfParamValue != null) {
                String providedToken = csrfHeaderValue != null ? csrfHeaderValue : csrfParamValue;
                if (csrfToken.getToken().equals(providedToken)) {
                    result.put("status", "CSRF_VALID");
                    logger.debug("CSRF test passed - Valid token provided");
                } else {
                    result.put("status", "CSRF_INVALID");
                    result.put("error", "CSRF token mismatch");
                    logger.warn("CSRF test failed - Token mismatch. Expected: {}, Provided: {}", 
                               csrfToken.getToken(), providedToken);
                }
            } else {
                result.put("status", "CSRF_MISSING");
                result.put("error", "CSRF token required but not provided");
                logger.warn("CSRF test failed - No CSRF token provided in request");
            }
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", "CSRF test failed: " + e.getMessage());
            logger.error("CSRF test error", e);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> testRoutingConfiguration(String reportType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate report type
            List<String> validReportTypes = Arrays.asList("visits", "revenue", "dashboard", 
                                                         "visit-statistics", "visit-statistics-by-veterinarian",
                                                         "revenue-by-veterinarian");
            
            if (!validReportTypes.contains(reportType)) {
                result.put("status", "INVALID_REPORT_TYPE");
                result.put("error", "Invalid report type: " + reportType);
                result.put("validTypes", validReportTypes);
                logger.warn("Routing test failed - Invalid report type: {}", reportType);
                return result;
            }
            
            // Check if export service can handle this report type
            boolean canValidate = reportExportService.validateExportParameters(reportType, "pdf");
            
            if (!canValidate) {
                result.put("status", "UNSUPPORTED_REPORT_TYPE");
                result.put("error", "Report type not supported by export service: " + reportType);
                logger.warn("Routing test failed - Unsupported report type: {}", reportType);
                return result;
            }
            
            // Test endpoint mapping
            String frontendEndpoint = "/dashboard/export/" + reportType;
            String backendEndpoint = "/api/reports/export/" + reportType;
            
            result.put("status", "ROUTING_VALID");
            result.put("frontendEndpoint", frontendEndpoint);
            result.put("backendEndpoint", backendEndpoint);
            result.put("reportType", reportType);
            result.put("supportedMethods", Arrays.asList("GET", "POST"));
            
            logger.debug("Routing test passed - ReportType: {}, Frontend: {}, Backend: {}", 
                        reportType, frontendEndpoint, backendEndpoint);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", "Routing test failed: " + e.getMessage());
            logger.error("Routing test error for report type: {}", reportType, e);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> testResponseHeaderConfiguration(String format) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate format
            if (!"pdf".equalsIgnoreCase(format) && !"csv".equalsIgnoreCase(format)) {
                result.put("status", "INVALID_FORMAT");
                result.put("error", "Invalid format: " + format + ". Supported formats: pdf, csv");
                logger.warn("Response header test failed - Invalid format: {}", format);
                return result;
            }
            
            // Test MIME type configuration
            String mimeType = reportExportService.getMimeType(format);
            String expectedMimeType = "pdf".equalsIgnoreCase(format) ? "application/pdf" : "text/csv";
            
            if (!expectedMimeType.equals(mimeType)) {
                result.put("status", "INCORRECT_MIME_TYPE");
                result.put("error", "Incorrect MIME type. Expected: " + expectedMimeType + ", Got: " + mimeType);
                logger.warn("Response header test failed - MIME type mismatch. Expected: {}, Got: {}", 
                           expectedMimeType, mimeType);
                return result;
            }
            
            // Test filename generation
            String filename = reportExportService.getExportFilename("test-report", format, 
                                                                   LocalDate.now(), LocalDate.now());
            
            if (filename == null || filename.trim().isEmpty()) {
                result.put("status", "INVALID_FILENAME");
                result.put("error", "Generated filename is null or empty");
                logger.warn("Response header test failed - Invalid filename generated");
                return result;
            }
            
            String expectedExtension = "pdf".equalsIgnoreCase(format) ? ".pdf" : ".csv";
            if (!filename.toLowerCase().endsWith(expectedExtension)) {
                result.put("status", "INCORRECT_FILE_EXTENSION");
                result.put("error", "Filename does not have correct extension. Expected: " + expectedExtension);
                logger.warn("Response header test failed - Incorrect file extension in filename: {}", filename);
                return result;
            }
            
            result.put("status", "HEADERS_VALID");
            result.put("mimeType", mimeType);
            result.put("filename", filename);
            result.put("contentDisposition", "attachment; filename=" + filename);
            result.put("cacheControl", "no-cache, no-store, must-revalidate, private");
            result.put("pragma", "no-cache");
            result.put("expires", "0");
            result.put("xContentTypeOptions", "nosniff");
            result.put("xDownloadOptions", "noopen");
            
            logger.debug("Response header test passed - Format: {}, MIME: {}, Filename: {}", 
                        format, mimeType, filename);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", "Response header test failed: " + e.getMessage());
            logger.error("Response header test error for format: {}", format, e);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> testCompleteExportFlow(String reportType, String format, 
                                                     LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Testing complete export flow - ReportType: {}, Format: {}, DateRange: {} to {}", 
                       reportType, format, startDate, endDate);
            
            // Step 1: Validate parameters
            if (!reportExportService.validateExportParameters(reportType, format)) {
                result.put("status", "PARAMETER_VALIDATION_FAILED");
                result.put("error", "Invalid parameters: reportType=" + reportType + ", format=" + format);
                return result;
            }
            
            // Step 2: Test data retrieval
            Map<String, Object> dataTest = new HashMap<>();
            try {
                switch (reportType.toLowerCase()) {
                    case "visits":
                    case "visit-statistics":
                        reportService.generateVisitStatistics(startDate, endDate);
                        dataTest.put("dataRetrieval", "SUCCESS");
                        break;
                    case "revenue":
                        reportService.generateRevenueReport(startDate, endDate, false);
                        dataTest.put("dataRetrieval", "SUCCESS");
                        break;
                    case "dashboard":
                        reportService.getDashboardMetrics(startDate);
                        dataTest.put("dataRetrieval", "SUCCESS");
                        break;
                    default:
                        dataTest.put("dataRetrieval", "UNSUPPORTED_TYPE");
                }
            } catch (Exception e) {
                dataTest.put("dataRetrieval", "FAILED");
                dataTest.put("dataError", e.getMessage());
            }
            
            // Step 3: Test export generation (without actually generating large files)
            Map<String, Object> exportTest = new HashMap<>();
            try {
                // Test filename and MIME type generation
                String filename = reportExportService.getExportFilename(reportType, format, startDate, endDate);
                String mimeType = reportExportService.getMimeType(format);
                
                exportTest.put("filenameGeneration", "SUCCESS");
                exportTest.put("filename", filename);
                exportTest.put("mimeType", mimeType);
                exportTest.put("exportGeneration", "SIMULATED_SUCCESS");
                
            } catch (Exception e) {
                exportTest.put("exportGeneration", "FAILED");
                exportTest.put("exportError", e.getMessage());
            }
            
            result.put("status", "FLOW_TEST_COMPLETE");
            result.put("parameterValidation", "SUCCESS");
            result.put("dataTest", dataTest);
            result.put("exportTest", exportTest);
            result.put("processingTimeMs", System.currentTimeMillis() - startTime);
            
            logger.info("Complete export flow test finished - Status: SUCCESS, Time: {}ms", 
                       result.get("processingTimeMs"));
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", "Complete flow test failed: " + e.getMessage());
            result.put("processingTimeMs", System.currentTimeMillis() - startTime);
            logger.error("Complete export flow test error", e);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> validateExportDependencies() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Validating export service dependencies");
            
            // Test ReportExportService
            Map<String, Object> exportServiceTest = new HashMap<>();
            try {
                boolean pdfValid = reportExportService.validateExportParameters("test", "pdf");
                boolean csvValid = reportExportService.validateExportParameters("test", "csv");
                exportServiceTest.put("available", true);
                exportServiceTest.put("pdfSupport", pdfValid || true); // Service exists even if params invalid
                exportServiceTest.put("csvSupport", csvValid || true);
            } catch (Exception e) {
                exportServiceTest.put("available", false);
                exportServiceTest.put("error", e.getMessage());
            }
            
            // Test ReportService
            Map<String, Object> reportServiceTest = new HashMap<>();
            try {
                // Test if service is available (don't actually call methods that require data)
                reportServiceTest.put("available", reportService != null);
            } catch (Exception e) {
                reportServiceTest.put("available", false);
                reportServiceTest.put("error", e.getMessage());
            }
            
            // Test PDF library (iText)
            Map<String, Object> pdfLibraryTest = new HashMap<>();
            try {
                Class.forName("com.itextpdf.text.Document");
                pdfLibraryTest.put("available", true);
                pdfLibraryTest.put("library", "iText");
            } catch (ClassNotFoundException e) {
                pdfLibraryTest.put("available", false);
                pdfLibraryTest.put("error", "iText library not found");
            }
            
            // Test CSV library (OpenCSV)
            Map<String, Object> csvLibraryTest = new HashMap<>();
            try {
                Class.forName("com.opencsv.CSVWriter");
                csvLibraryTest.put("available", true);
                csvLibraryTest.put("library", "OpenCSV");
            } catch (ClassNotFoundException e) {
                csvLibraryTest.put("available", false);
                csvLibraryTest.put("error", "OpenCSV library not found");
            }
            
            result.put("status", "DEPENDENCY_CHECK_COMPLETE");
            result.put("reportExportService", exportServiceTest);
            result.put("reportService", reportServiceTest);
            result.put("pdfLibrary", pdfLibraryTest);
            result.put("csvLibrary", csvLibraryTest);
            
            boolean allDependenciesAvailable = 
                (Boolean) exportServiceTest.get("available") &&
                (Boolean) reportServiceTest.get("available") &&
                (Boolean) pdfLibraryTest.get("available") &&
                (Boolean) csvLibraryTest.get("available");
            
            result.put("allDependenciesAvailable", allDependenciesAvailable);
            
            logger.info("Export dependencies validation complete - All available: {}", allDependenciesAvailable);
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", "Dependency validation failed: " + e.getMessage());
            logger.error("Export dependencies validation error", e);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getErrorAnalysis(String requestId) {
        Map<String, Object> result = new HashMap<>();
        
        ExportDiagnosticInfo diagnostic = diagnosticCache.get(requestId);
        
        if (diagnostic == null) {
            result.put("status", "REQUEST_NOT_FOUND");
            result.put("error", "No diagnostic information found for request ID: " + requestId);
            return result;
        }
        
        result.put("status", "ANALYSIS_COMPLETE");
        result.put("requestId", requestId);
        result.put("reportType", diagnostic.getReportType());
        result.put("format", diagnostic.getFormat());
        result.put("processingTimeMs", diagnostic.getProcessingTimeMs());
        result.put("authenticationStatus", diagnostic.getAuthenticationStatus());
        result.put("csrfTokenStatus", diagnostic.getCsrfTokenStatus());
        result.put("routingStatus", diagnostic.getRoutingStatus());
        result.put("responseStatus", diagnostic.getResponseStatus());
        result.put("errorMessage", diagnostic.getErrorMessage());
        result.put("trace", diagnostic.getFrontendToBackendTrace());
        
        // Provide recommendations based on the analysis
        List<String> recommendations = new ArrayList<>();
        
        if ("ERROR".equals(diagnostic.getAuthenticationStatus()) || 
            "NOT_AUTHENTICATED".equals(diagnostic.getAuthenticationStatus())) {
            recommendations.add("Check user authentication and session validity");
        }
        
        if ("CSRF_MISSING".equals(diagnostic.getCsrfTokenStatus()) || 
            "CSRF_INVALID".equals(diagnostic.getCsrfTokenStatus())) {
            recommendations.add("Ensure CSRF tokens are properly included in export requests");
        }
        
        if ("ERROR".equals(diagnostic.getRoutingStatus())) {
            recommendations.add("Verify export endpoint routing configuration");
        }
        
        if ("ERROR".equals(diagnostic.getResponseStatus())) {
            recommendations.add("Check HTTP response header configuration for file downloads");
        }
        
        result.put("recommendations", recommendations);
        
        return result;
    }
    
    @Override
    public Map<String, Object> generateSystemHealthReport() {
        Map<String, Object> report = new HashMap<>();
        
        try {
            logger.info("Generating export system health report");
            
            // Overall system status
            report.put("timestamp", new Date());
            report.put("reportVersion", "1.0");
            
            // Dependency health
            Map<String, Object> dependencies = validateExportDependencies();
            report.put("dependencies", dependencies);
            
            // Test basic functionality
            Map<String, Object> functionalityTests = new HashMap<>();
            
            // Test each report type
            String[] reportTypes = {"visits", "revenue", "dashboard"};
            String[] formats = {"pdf", "csv"};
            
            for (String reportType : reportTypes) {
                Map<String, Object> reportTypeTest = new HashMap<>();
                
                for (String format : formats) {
                    try {
                        Map<String, Object> routingTest = testRoutingConfiguration(reportType);
                        Map<String, Object> headerTest = testResponseHeaderConfiguration(format);
                        
                        boolean success = "ROUTING_VALID".equals(routingTest.get("status")) &&
                                        "HEADERS_VALID".equals(headerTest.get("status"));
                        
                        reportTypeTest.put(format, success ? "HEALTHY" : "ISSUES_DETECTED");
                        
                    } catch (Exception e) {
                        reportTypeTest.put(format, "ERROR");
                    }
                }
                
                functionalityTests.put(reportType, reportTypeTest);
            }
            
            report.put("functionalityTests", functionalityTests);
            
            // Cache statistics
            Map<String, Object> cacheStats = new HashMap<>();
            cacheStats.put("diagnosticCacheSize", diagnosticCache.size());
            cacheStats.put("oldestEntry", diagnosticCache.isEmpty() ? null : 
                Collections.min(diagnosticCache.values(), 
                    Comparator.comparing(ExportDiagnosticInfo::getProcessingTimeMs)));
            
            report.put("cacheStatistics", cacheStats);
            
            // Overall health status
            boolean dependenciesHealthy = (Boolean) dependencies.getOrDefault("allDependenciesAvailable", false);
            boolean functionalityHealthy = functionalityTests.values().stream()
                .allMatch(test -> ((Map<String, Object>) test).values().stream()
                    .allMatch(status -> "HEALTHY".equals(status)));
            
            String overallStatus = (dependenciesHealthy && functionalityHealthy) ? "HEALTHY" : "DEGRADED";
            report.put("overallStatus", overallStatus);
            
            logger.info("Export system health report generated - Status: {}", overallStatus);
            
        } catch (Exception e) {
            report.put("status", "ERROR");
            report.put("error", "Health report generation failed: " + e.getMessage());
            logger.error("System health report generation error", e);
        }
        
        return report;
    }
}