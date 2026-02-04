package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.service.ExportDiagnosticService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExportDiagnosticServiceImpl
 * Tests diagnostic functionality for export request flow analysis
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@ExtendWith(MockitoExtension.class)
class ExportDiagnosticServiceImplTest {
    
    @Mock
    private ReportExportService reportExportService;
    
    @Mock
    private ReportService reportService;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private ExportDiagnosticServiceImpl exportDiagnosticService;
    
    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    void testTraceExportRequest_Success() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("User-Agent", "Test Browser");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(reportExportService.validateExportParameters(reportType, format)).thenReturn(true);
        when(reportExportService.getMimeType(format)).thenReturn("application/pdf");
        when(reportExportService.getExportFilename(reportType, format, startDate, endDate))
            .thenReturn("visits_2024-01-01_2024-01-31.pdf");
        
        // When
        ExportDiagnosticService.ExportDiagnosticInfo result = 
            exportDiagnosticService.traceExportRequest(reportType, format, startDate, endDate, requestHeaders);
        
        // Then
        assertNotNull(result);
        assertEquals(reportType, result.getReportType());
        assertEquals(format, result.getFormat());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertNotNull(result.getRequestId());
        assertTrue(result.getProcessingTimeMs() >= 0);
        assertNotNull(result.getFrontendToBackendTrace());
        
        // Verify response headers are set correctly
        Map<String, String> responseHeaders = result.getResponseHeaders();
        assertNotNull(responseHeaders);
        assertEquals("application/pdf", responseHeaders.get("Content-Type"));
        assertTrue(responseHeaders.get("Content-Disposition").contains("visits_2024-01-01_2024-01-31.pdf"));
    }
    
    @Test
    void testTraceExportRequest_WithError() {
        // Given
        String reportType = "invalid";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Map<String, String> requestHeaders = new HashMap<>();
        
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // When
        ExportDiagnosticService.ExportDiagnosticInfo result = 
            exportDiagnosticService.traceExportRequest(reportType, format, startDate, endDate, requestHeaders);
        
        // Then
        assertNotNull(result);
        assertEquals(reportType, result.getReportType());
        assertEquals(format, result.getFormat());
        assertEquals("NO_AUTHENTICATION", result.getAuthenticationStatus());
        assertNotNull(result.getFrontendToBackendTrace());
    }
    
    @Test
    void testTestAuthenticationFlow_Authenticated() {
        // Given
        Map<String, String> requestHeaders = new HashMap<>();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        
        // When
        Map<String, Object> result = exportDiagnosticService.testAuthenticationFlow(requestHeaders);
        
        // Then
        assertNotNull(result);
        assertEquals("AUTHENTICATED", result.get("status"));
        assertEquals("testuser", result.get("principal"));
    }
    
    @Test
    void testTestAuthenticationFlow_NotAuthenticated() {
        // Given
        Map<String, String> requestHeaders = new HashMap<>();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        
        // When
        Map<String, Object> result = exportDiagnosticService.testAuthenticationFlow(requestHeaders);
        
        // Then
        assertNotNull(result);
        assertEquals("NOT_AUTHENTICATED", result.get("status"));
        assertTrue(result.containsKey("error"));
    }
    
    @Test
    void testTestAuthenticationFlow_NoAuthentication() {
        // Given
        Map<String, String> requestHeaders = new HashMap<>();
        
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // When
        Map<String, Object> result = exportDiagnosticService.testAuthenticationFlow(requestHeaders);
        
        // Then
        assertNotNull(result);
        assertEquals("NO_AUTHENTICATION", result.get("status"));
        assertTrue(result.containsKey("error"));
    }
    
    @Test
    void testTestRoutingConfiguration_ValidReportType() {
        // Given
        String reportType = "visits";
        
        when(reportExportService.validateExportParameters(reportType, "pdf")).thenReturn(true);
        
        // When
        Map<String, Object> result = exportDiagnosticService.testRoutingConfiguration(reportType);
        
        // Then
        assertNotNull(result);
        assertEquals("ROUTING_VALID", result.get("status"));
        assertEquals("/dashboard/export/" + reportType, result.get("frontendEndpoint"));
        assertEquals("/api/reports/export/" + reportType, result.get("backendEndpoint"));
    }
    
    @Test
    void testTestRoutingConfiguration_InvalidReportType() {
        // Given
        String reportType = "invalid-type";
        
        // When
        Map<String, Object> result = exportDiagnosticService.testRoutingConfiguration(reportType);
        
        // Then
        assertNotNull(result);
        assertEquals("INVALID_REPORT_TYPE", result.get("status"));
        assertTrue(result.containsKey("error"));
        assertTrue(result.containsKey("validTypes"));
    }
    
    @Test
    void testTestResponseHeaderConfiguration_ValidPdfFormat() {
        // Given
        String format = "pdf";
        
        when(reportExportService.getMimeType(format)).thenReturn("application/pdf");
        when(reportExportService.getExportFilename(anyString(), eq(format), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn("test-report.pdf");
        
        // When
        Map<String, Object> result = exportDiagnosticService.testResponseHeaderConfiguration(format);
        
        // Then
        assertNotNull(result);
        assertEquals("HEADERS_VALID", result.get("status"));
        assertEquals("application/pdf", result.get("mimeType"));
        assertEquals("test-report.pdf", result.get("filename"));
    }
    
    @Test
    void testTestResponseHeaderConfiguration_ValidCsvFormat() {
        // Given
        String format = "csv";
        
        when(reportExportService.getMimeType(format)).thenReturn("text/csv");
        when(reportExportService.getExportFilename(anyString(), eq(format), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn("test-report.csv");
        
        // When
        Map<String, Object> result = exportDiagnosticService.testResponseHeaderConfiguration(format);
        
        // Then
        assertNotNull(result);
        assertEquals("HEADERS_VALID", result.get("status"));
        assertEquals("text/csv", result.get("mimeType"));
        assertEquals("test-report.csv", result.get("filename"));
    }
    
    @Test
    void testTestResponseHeaderConfiguration_InvalidFormat() {
        // Given
        String format = "invalid";
        
        // When
        Map<String, Object> result = exportDiagnosticService.testResponseHeaderConfiguration(format);
        
        // Then
        assertNotNull(result);
        assertEquals("INVALID_FORMAT", result.get("status"));
        assertTrue(result.containsKey("error"));
    }
    
    @Test
    void testTestCompleteExportFlow_Success() {
        // Given
        String reportType = "visits";
        String format = "pdf";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        when(reportExportService.validateExportParameters(reportType, format)).thenReturn(true);
        when(reportExportService.getExportFilename(reportType, format, startDate, endDate))
            .thenReturn("visits_2024-01-01_2024-01-31.pdf");
        when(reportExportService.getMimeType(format)).thenReturn("application/pdf");
        when(reportService.generateVisitStatistics(startDate, endDate)).thenReturn(new VisitStatisticsReport());
        
        // When
        Map<String, Object> result = exportDiagnosticService.testCompleteExportFlow(
            reportType, format, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals("FLOW_TEST_COMPLETE", result.get("status"));
        assertEquals("SUCCESS", result.get("parameterValidation"));
        assertTrue(result.containsKey("processingTimeMs"));
        assertTrue(result.containsKey("dataTest"));
        assertTrue(result.containsKey("exportTest"));
    }
    
    @Test
    void testValidateExportDependencies() {
        // Given
        when(reportExportService.validateExportParameters(anyString(), anyString())).thenReturn(true);
        
        // When
        Map<String, Object> result = exportDiagnosticService.validateExportDependencies();
        
        // Then
        assertNotNull(result);
        assertEquals("DEPENDENCY_CHECK_COMPLETE", result.get("status"));
        assertTrue(result.containsKey("reportExportService"));
        assertTrue(result.containsKey("reportService"));
        assertTrue(result.containsKey("pdfLibrary"));
        assertTrue(result.containsKey("csvLibrary"));
        assertTrue(result.containsKey("allDependenciesAvailable"));
    }
    
    @Test
    void testGenerateSystemHealthReport() {
        // Given
        when(reportExportService.validateExportParameters(anyString(), anyString())).thenReturn(true);
        when(reportExportService.getMimeType(anyString())).thenReturn("application/pdf");
        when(reportExportService.getExportFilename(anyString(), anyString(), any(), any()))
            .thenReturn("test.pdf");
        
        // When
        Map<String, Object> result = exportDiagnosticService.generateSystemHealthReport();
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("timestamp"));
        assertTrue(result.containsKey("dependencies"));
        assertTrue(result.containsKey("functionalityTests"));
        assertTrue(result.containsKey("overallStatus"));
    }
    
    @Test
    void testGetErrorAnalysis_RequestNotFound() {
        // Given
        String requestId = "non-existent-id";
        
        // When
        Map<String, Object> result = exportDiagnosticService.getErrorAnalysis(requestId);
        
        // Then
        assertNotNull(result);
        assertEquals("REQUEST_NOT_FOUND", result.get("status"));
        assertTrue(result.containsKey("error"));
    }
}