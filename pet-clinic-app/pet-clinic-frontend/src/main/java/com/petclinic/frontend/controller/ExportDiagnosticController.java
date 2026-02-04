package com.petclinic.frontend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Frontend controller for export diagnostics
 * Provides web interface and endpoints for testing export functionality
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@Controller
@RequestMapping("/dashboard/diagnostics")
public class ExportDiagnosticController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportDiagnosticController.class);
    
    @Autowired
    private WebClient webClient;
    
    /**
     * Show export diagnostics page
     */
    @GetMapping("/export")
    public String showExportDiagnostics(Model model) {
        try {
            // Set default test parameters
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            model.addAttribute("defaultStartDate", startDate);
            model.addAttribute("defaultEndDate", endDate);
            model.addAttribute("reportTypes", new String[]{"visits", "revenue", "dashboard"});
            model.addAttribute("formats", new String[]{"pdf", "csv"});
            
            return "dashboard/diagnostics/export";
            
        } catch (Exception e) {
            logger.error("Error loading export diagnostics page", e);
            model.addAttribute("error", "Error loading diagnostics page: " + e.getMessage());
            return "dashboard/diagnostics/export";
        }
    }
    
    /**
     * Run export trace diagnostic
     * GET /dashboard/diagnostics/export/trace
     */
    @GetMapping("/export/trace")
    @ResponseBody
    public Mono<Map<String, Object>> traceExportRequest(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        
        logger.info("Frontend export trace request - ReportType: {}, Format: {}, DateRange: {} to {}", 
                   reportType, format, startDate, endDate);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/diagnostics/export/trace")
                        .queryParam("reportType", reportType)
                        .queryParam("format", format)
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("diagnostic", response);
                    result.put("frontendRequestInfo", extractFrontendRequestInfo(request));
                    return result;
                })
                .onErrorReturn(createErrorResponse("Frontend trace failed"));
    }
    
    /**
     * Test authentication flow
     * GET /dashboard/diagnostics/export/test/authentication
     */
    @GetMapping("/export/test/authentication")
    @ResponseBody
    public Mono<Map<String, Object>> testAuthentication(HttpServletRequest request) {
        logger.info("Frontend authentication test request");
        
        return webClient.get()
                .uri("/diagnostics/export/test/authentication")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("backendTest", response);
                    result.put("frontendAuth", extractFrontendAuthInfo(request));
                    return result;
                })
                .onErrorReturn(createErrorResponse("Frontend auth test failed"));
    }
    
    /**
     * Test CSRF token handling
     * GET /dashboard/diagnostics/export/test/csrf
     */
    @GetMapping("/export/test/csrf")
    @ResponseBody
    public Mono<Map<String, Object>> testCsrfToken(HttpServletRequest request) {
        logger.info("Frontend CSRF test request");
        
        return webClient.get()
                .uri("/diagnostics/export/test/csrf")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("backendTest", response);
                    result.put("frontendCsrf", extractFrontendCsrfInfo(request));
                    return result;
                })
                .onErrorReturn(createErrorResponse("Frontend CSRF test failed"));
    }
    
    /**
     * Test complete export flow from frontend perspective
     * GET /dashboard/diagnostics/export/test/complete
     */
    @GetMapping("/export/test/complete")
    @ResponseBody
    public Mono<Map<String, Object>> testCompleteFlow(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        
        logger.info("Frontend complete flow test - ReportType: {}, Format: {}, DateRange: {} to {}", 
                   reportType, format, startDate, endDate);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/diagnostics/export/test/complete")
                        .queryParam("reportType", reportType)
                        .queryParam("format", format)
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("backendTest", response);
                    result.put("frontendToBackendFlow", analyzeFrontendToBackendFlow(request, reportType, format));
                    return result;
                })
                .onErrorReturn(createErrorResponse("Frontend complete flow test failed"));
    }
    
    /**
     * Get system health report
     * GET /dashboard/diagnostics/export/health
     */
    @GetMapping("/export/health")
    @ResponseBody
    public Mono<Map<String, Object>> getSystemHealth() {
        logger.info("Frontend system health request");
        
        return webClient.get()
                .uri("/diagnostics/export/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("systemHealth", response);
                    result.put("frontendHealth", getFrontendHealthInfo());
                    return result;
                })
                .onErrorReturn(createErrorResponse("System health check failed"));
    }
    
    /**
     * Test all export endpoints
     * GET /dashboard/diagnostics/export/test/all
     */
    @GetMapping("/export/test/all")
    @ResponseBody
    public Mono<Map<String, Object>> testAllEndpoints() {
        logger.info("Frontend test all endpoints request");
        
        return webClient.get()
                .uri("/diagnostics/export/test/all")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("allEndpointsTest", response);
                    result.put("frontendEndpoints", getFrontendEndpointInfo());
                    return result;
                })
                .onErrorReturn(createErrorResponse("All endpoints test failed"));
    }
    
    /**
     * Simulate actual export request to test the real flow
     * GET /dashboard/diagnostics/export/simulate
     */
    @GetMapping("/export/simulate")
    @ResponseBody
    public Mono<Map<String, Object>> simulateExportRequest(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        
        logger.info("Simulating actual export request - ReportType: {}, Format: {}, DateRange: {} to {}", 
                   reportType, format, startDate, endDate);
        
        // This simulates the actual export flow that would happen when a user clicks export
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/export/" + reportType)
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .queryParam("format", format)
                        .build())
                .retrieve()
                .bodyToMono(byte[].class)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "SUCCESS");
                    result.put("message", "Export simulation successful");
                    result.put("responseSize", response != null ? response.length : 0);
                    result.put("simulationTime", System.currentTimeMillis());
                    return result;
                })
                .onErrorReturn(createErrorResponse("Export simulation failed"));
    }
    
    /**
     * Create error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "ERROR");
        errorResult.put("error", message);
        errorResult.put("timestamp", System.currentTimeMillis());
        return errorResult;
    }
    
    /**
     * Extract frontend request information
     */
    private Map<String, Object> extractFrontendRequestInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        
        info.put("requestURI", request.getRequestURI());
        info.put("method", request.getMethod());
        info.put("remoteAddr", request.getRemoteAddr());
        info.put("userAgent", request.getHeader("User-Agent"));
        info.put("referer", request.getHeader("Referer"));
        info.put("sessionId", request.getSession(false) != null ? request.getSession(false).getId() : "NO_SESSION");
        
        return info;
    }
    
    /**
     * Extract frontend authentication information
     */
    private Map<String, Object> extractFrontendAuthInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        
        info.put("sessionExists", request.getSession(false) != null);
        info.put("sessionId", request.getSession(false) != null ? request.getSession(false).getId() : null);
        info.put("remoteUser", request.getRemoteUser());
        info.put("userPrincipal", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null);
        info.put("authType", request.getAuthType());
        info.put("secure", request.isSecure());
        
        return info;
    }
    
    /**
     * Extract frontend CSRF information
     */
    private Map<String, Object> extractFrontendCsrfInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        
        // Check for CSRF token in request
        String csrfHeader = request.getHeader("X-CSRF-TOKEN");
        String csrfParam = request.getParameter("_csrf");
        
        info.put("csrfHeader", csrfHeader);
        info.put("csrfParam", csrfParam);
        info.put("csrfPresent", csrfHeader != null || csrfParam != null);
        
        return info;
    }
    
    /**
     * Analyze frontend to backend flow
     */
    private Map<String, Object> analyzeFrontendToBackendFlow(HttpServletRequest request, String reportType, String format) {
        Map<String, Object> analysis = new HashMap<>();
        
        analysis.put("frontendEndpoint", "/dashboard/export/" + reportType);
        analysis.put("backendEndpoint", "/api/reports/export/" + reportType);
        analysis.put("expectedFlow", "Frontend Controller -> WebClient -> Backend API -> Export Service");
        analysis.put("requestMethod", request.getMethod());
        analysis.put("contentType", request.getContentType());
        analysis.put("acceptHeader", request.getHeader("Accept"));
        
        return analysis;
    }
    
    /**
     * Get frontend health information
     */
    private Map<String, Object> getFrontendHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("webClientAvailable", webClient != null);
        health.put("timestamp", System.currentTimeMillis());
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("springProfile", System.getProperty("spring.profiles.active"));
        
        return health;
    }
    
    /**
     * Get frontend endpoint information
     */
    private Map<String, Object> getFrontendEndpointInfo() {
        Map<String, Object> endpoints = new HashMap<>();
        
        endpoints.put("dashboardExportVisits", "/dashboard/export/visits");
        endpoints.put("dashboardExportRevenue", "/dashboard/export/revenue");
        endpoints.put("dashboardExportDashboard", "/dashboard/export/dashboard");
        endpoints.put("diagnosticsBase", "/dashboard/diagnostics/export");
        
        return endpoints;
    }
}