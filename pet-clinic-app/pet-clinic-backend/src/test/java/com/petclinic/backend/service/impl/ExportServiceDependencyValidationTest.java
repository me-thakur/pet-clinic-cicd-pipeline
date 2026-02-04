package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.ExportDiagnosticService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to validate export service dependencies and configuration
 * Validates: Requirements 1.4, 1.5
 */
@SpringBootTest
@ActiveProfiles("test")
class ExportServiceDependencyValidationTest {
    
    @Autowired
    private ExportDiagnosticService exportDiagnosticService;
    
    @Autowired
    private ReportExportService reportExportService;
    
    @Autowired
    private ReportService reportService;
    
    @Test
    void testExportServiceDependenciesValidation() {
        // Test that all required services are properly injected
        assertNotNull(exportDiagnosticService, "ExportDiagnosticService should be available");
        assertNotNull(reportExportService, "ReportExportService should be available");
        assertNotNull(reportService, "ReportService should be available");
        
        // Validate export dependencies using the diagnostic service
        Map<String, Object> result = exportDiagnosticService.validateExportDependencies();
        
        assertNotNull(result, "Dependency validation result should not be null");
        assertEquals("DEPENDENCY_CHECK_COMPLETE", result.get("status"), "Dependency check should complete successfully");
        
        // Check individual dependency results
        Map<String, Object> reportExportServiceTest = (Map<String, Object>) result.get("reportExportService");
        assertNotNull(reportExportServiceTest, "ReportExportService test result should be available");
        assertTrue((Boolean) reportExportServiceTest.get("available"), "ReportExportService should be available");
        
        Map<String, Object> reportServiceTest = (Map<String, Object>) result.get("reportService");
        assertNotNull(reportServiceTest, "ReportService test result should be available");
        assertTrue((Boolean) reportServiceTest.get("available"), "ReportService should be available");
        
        Map<String, Object> pdfLibraryTest = (Map<String, Object>) result.get("pdfLibrary");
        assertNotNull(pdfLibraryTest, "PDF library test result should be available");
        assertTrue((Boolean) pdfLibraryTest.get("available"), "PDF library should be available");
        assertEquals("iText", pdfLibraryTest.get("library"), "Should use iText PDF library");
        
        Map<String, Object> csvLibraryTest = (Map<String, Object>) result.get("csvLibrary");
        assertNotNull(csvLibraryTest, "CSV library test result should be available");
        assertTrue((Boolean) csvLibraryTest.get("available"), "CSV library should be available");
        assertEquals("OpenCSV", csvLibraryTest.get("library"), "Should use OpenCSV library");
        
        // Overall dependencies should be available
        assertTrue((Boolean) result.get("allDependenciesAvailable"), "All dependencies should be available");
    }
    
    @Test
    void testReportExportServiceConfiguration() {
        // Test MIME type configuration
        String pdfMimeType = reportExportService.getMimeType("pdf");
        assertEquals("application/pdf", pdfMimeType, "PDF MIME type should be correct");
        
        String csvMimeType = reportExportService.getMimeType("csv");
        assertEquals("text/csv", csvMimeType, "CSV MIME type should be correct");
        
        // Test filename generation
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        String pdfFilename = reportExportService.getExportFilename("visits", "pdf", startDate, endDate);
        assertNotNull(pdfFilename, "PDF filename should be generated");
        assertTrue(pdfFilename.endsWith(".pdf"), "PDF filename should have .pdf extension");
        assertTrue(pdfFilename.contains("visits"), "PDF filename should contain report type");
        
        String csvFilename = reportExportService.getExportFilename("revenue", "csv", startDate, endDate);
        assertNotNull(csvFilename, "CSV filename should be generated");
        assertTrue(csvFilename.endsWith(".csv"), "CSV filename should have .csv extension");
        assertTrue(csvFilename.contains("revenue"), "CSV filename should contain report type");
        
        // Test parameter validation
        assertTrue(reportExportService.validateExportParameters("visit-statistics", "pdf"), "Valid parameters should be accepted");
        assertTrue(reportExportService.validateExportParameters("revenue", "csv"), "Valid parameters should be accepted");
        assertTrue(reportExportService.validateExportParameters("dashboard", "pdf"), "Valid parameters should be accepted");
    }
    
    @Test
    void testReportDataRetrievalServices() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // Test that report service methods are available (don't call them as they require database data)
        assertDoesNotThrow(() -> {
            // These methods should exist and be callable
            reportService.getClass().getMethod("generateVisitStatistics", LocalDate.class, LocalDate.class);
            reportService.getClass().getMethod("generateRevenueReport", LocalDate.class, LocalDate.class, boolean.class);
            reportService.getClass().getMethod("getDashboardMetrics", LocalDate.class);
        }, "Report service methods should be available");
    }
    
    @Test
    void testExportDiagnosticServiceConfiguration() {
        // Test routing configuration for different report types
        String[] reportTypes = {"visits", "revenue", "dashboard"};
        
        for (String reportType : reportTypes) {
            Map<String, Object> routingResult = exportDiagnosticService.testRoutingConfiguration(reportType);
            assertNotNull(routingResult, "Routing result should not be null for " + reportType);
            assertEquals("ROUTING_VALID", routingResult.get("status"), "Routing should be valid for " + reportType);
            assertEquals("/dashboard/export/" + reportType, routingResult.get("frontendEndpoint"), 
                        "Frontend endpoint should be correct for " + reportType);
            assertEquals("/api/reports/export/" + reportType, routingResult.get("backendEndpoint"), 
                        "Backend endpoint should be correct for " + reportType);
        }
        
        // Test response header configuration for different formats
        String[] formats = {"pdf", "csv"};
        
        for (String format : formats) {
            Map<String, Object> headerResult = exportDiagnosticService.testResponseHeaderConfiguration(format);
            assertNotNull(headerResult, "Header result should not be null for " + format);
            assertEquals("HEADERS_VALID", headerResult.get("status"), "Headers should be valid for " + format);
            
            String expectedMimeType = "pdf".equals(format) ? "application/pdf" : "text/csv";
            assertEquals(expectedMimeType, headerResult.get("mimeType"), "MIME type should be correct for " + format);
            
            String filename = (String) headerResult.get("filename");
            assertNotNull(filename, "Filename should be generated for " + format);
            assertTrue(filename.endsWith("." + format), "Filename should have correct extension for " + format);
        }
    }
    
    @Test
    void testCompleteExportFlowValidation() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // Test complete flow for different report types and formats
        String[] reportTypes = {"visits", "revenue", "dashboard"};
        String[] formats = {"pdf", "csv"};
        
        for (String reportType : reportTypes) {
            for (String format : formats) {
                Map<String, Object> flowResult = exportDiagnosticService.testCompleteExportFlow(
                    reportType, format, startDate, endDate);
                
                assertNotNull(flowResult, "Flow result should not be null for " + reportType + "/" + format);
                assertEquals("FLOW_TEST_COMPLETE", flowResult.get("status"), 
                           "Flow test should complete for " + reportType + "/" + format);
                assertEquals("SUCCESS", flowResult.get("parameterValidation"), 
                           "Parameter validation should succeed for " + reportType + "/" + format);
                
                assertTrue(flowResult.containsKey("processingTimeMs"), 
                          "Processing time should be recorded for " + reportType + "/" + format);
                assertTrue(flowResult.containsKey("dataTest"), 
                          "Data test should be included for " + reportType + "/" + format);
                assertTrue(flowResult.containsKey("exportTest"), 
                          "Export test should be included for " + reportType + "/" + format);
            }
        }
    }
    
    @Test
    void testSystemHealthReport() {
        Map<String, Object> healthReport = exportDiagnosticService.generateSystemHealthReport();
        
        assertNotNull(healthReport, "Health report should not be null");
        assertTrue(healthReport.containsKey("timestamp"), "Health report should include timestamp");
        assertTrue(healthReport.containsKey("dependencies"), "Health report should include dependencies");
        assertTrue(healthReport.containsKey("functionalityTests"), "Health report should include functionality tests");
        assertTrue(healthReport.containsKey("overallStatus"), "Health report should include overall status");
        
        // Check that functionality tests cover all report types and formats
        Map<String, Object> functionalityTests = (Map<String, Object>) healthReport.get("functionalityTests");
        assertNotNull(functionalityTests, "Functionality tests should not be null");
        
        String[] reportTypes = {"visits", "revenue", "dashboard"};
        for (String reportType : reportTypes) {
            assertTrue(functionalityTests.containsKey(reportType), 
                      "Functionality tests should include " + reportType);
            
            Map<String, Object> reportTypeTests = (Map<String, Object>) functionalityTests.get(reportType);
            assertTrue(reportTypeTests.containsKey("pdf"), 
                      "Functionality tests should include PDF for " + reportType);
            assertTrue(reportTypeTests.containsKey("csv"), 
                      "Functionality tests should include CSV for " + reportType);
        }
    }
}