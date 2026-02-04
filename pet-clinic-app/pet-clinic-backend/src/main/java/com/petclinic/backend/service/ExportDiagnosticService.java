package com.petclinic.backend.service;

import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service interface for diagnosing export request flow issues
 * Provides comprehensive tracing and analysis of export requests from frontend to backend
 * Validates: Requirements 1.1, 1.2, 1.3
 */
public interface ExportDiagnosticService {
    
    /**
     * Diagnostic information for export request flow
     */
    class ExportDiagnosticInfo {
        private String requestId;
        private String reportType;
        private String format;
        private LocalDate startDate;
        private LocalDate endDate;
        private String authenticationStatus;
        private String csrfTokenStatus;
        private String routingStatus;
        private String responseStatus;
        private Map<String, String> requestHeaders;
        private Map<String, String> responseHeaders;
        private String errorMessage;
        private long processingTimeMs;
        private String frontendToBackendTrace;
        
        // Constructors
        public ExportDiagnosticInfo() {}
        
        public ExportDiagnosticInfo(String requestId, String reportType, String format) {
            this.requestId = requestId;
            this.reportType = reportType;
            this.format = format;
        }
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public String getAuthenticationStatus() { return authenticationStatus; }
        public void setAuthenticationStatus(String authenticationStatus) { this.authenticationStatus = authenticationStatus; }
        
        public String getCsrfTokenStatus() { return csrfTokenStatus; }
        public void setCsrfTokenStatus(String csrfTokenStatus) { this.csrfTokenStatus = csrfTokenStatus; }
        
        public String getRoutingStatus() { return routingStatus; }
        public void setRoutingStatus(String routingStatus) { this.routingStatus = routingStatus; }
        
        public String getResponseStatus() { return responseStatus; }
        public void setResponseStatus(String responseStatus) { this.responseStatus = responseStatus; }
        
        public Map<String, String> getRequestHeaders() { return requestHeaders; }
        public void setRequestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; }
        
        public Map<String, String> getResponseHeaders() { return responseHeaders; }
        public void setResponseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public String getFrontendToBackendTrace() { return frontendToBackendTrace; }
        public void setFrontendToBackendTrace(String frontendToBackendTrace) { this.frontendToBackendTrace = frontendToBackendTrace; }
    }
    
    /**
     * Trace and analyze an export request from start to finish
     * @param reportType Type of report being exported
     * @param format Export format (pdf/csv)
     * @param startDate Start date for the report
     * @param endDate End date for the report
     * @param requestHeaders HTTP request headers
     * @return Diagnostic information about the request flow
     */
    ExportDiagnosticInfo traceExportRequest(String reportType, String format, 
                                          LocalDate startDate, LocalDate endDate,
                                          Map<String, String> requestHeaders);
    
    /**
     * Test authentication flow for export requests
     * @param requestHeaders HTTP request headers
     * @return Authentication diagnostic information
     */
    Map<String, Object> testAuthenticationFlow(Map<String, String> requestHeaders);
    
    /**
     * Test CSRF token handling for export requests
     * @param requestHeaders HTTP request headers
     * @return CSRF diagnostic information
     */
    Map<String, Object> testCsrfTokenHandling(Map<String, String> requestHeaders);
    
    /**
     * Test routing configuration for export endpoints
     * @param reportType Type of report
     * @return Routing diagnostic information
     */
    Map<String, Object> testRoutingConfiguration(String reportType);
    
    /**
     * Test response header configuration for file downloads
     * @param format Export format (pdf/csv)
     * @return Response header diagnostic information
     */
    Map<String, Object> testResponseHeaderConfiguration(String format);
    
    /**
     * Test complete export flow end-to-end
     * @param reportType Type of report
     * @param format Export format
     * @param startDate Start date
     * @param endDate End date
     * @return Complete diagnostic information
     */
    Map<String, Object> testCompleteExportFlow(String reportType, String format, 
                                             LocalDate startDate, LocalDate endDate);
    
    /**
     * Validate export service dependencies
     * @return Dependency validation results
     */
    Map<String, Object> validateExportDependencies();
    
    /**
     * Get detailed error analysis for failed export requests
     * @param requestId Request ID to analyze
     * @return Error analysis information
     */
    Map<String, Object> getErrorAnalysis(String requestId);
    
    /**
     * Generate diagnostic report for export system health
     * @return System health diagnostic report
     */
    Map<String, Object> generateSystemHealthReport();
}