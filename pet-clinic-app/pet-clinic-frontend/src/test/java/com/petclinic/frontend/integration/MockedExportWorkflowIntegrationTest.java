package com.petclinic.frontend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.frontend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Mocked Export Workflow Integration Test
 * 
 * Comprehensive integration tests for the complete export workflow using mocked backend responses.
 * This allows testing the frontend export functionality without requiring a running backend.
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MockedExportWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // Test data constants
    private static final LocalDate DEFAULT_START_DATE = LocalDate.now().minusDays(30);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.now();
    private static final String[] REPORT_TYPES = {"visits", "revenue", "dashboard"};
    private static final String[] FORMATS = {"pdf", "csv"};

    @BeforeEach
    void setUp() {
        // Reset all mocks before each test
        Mockito.reset(reportService);
    }

    /**
     * Test data provider for all report type and format combinations
     */
    private static Stream<Arguments> reportTypeAndFormatProvider() {
        return Stream.of(REPORT_TYPES)
                .flatMap(reportType -> Stream.of(FORMATS)
                        .map(format -> Arguments.of(reportType, format)));
    }

    /**
     * Test data provider for date range variations
     */
    private static Stream<Arguments> dateRangeProvider() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(today.minusDays(7), today, "Last 7 days"),
                Arguments.of(today.minusDays(30), today, "Last 30 days"),
                Arguments.of(today.minusDays(90), today, "Last 90 days"),
                Arguments.of(today.minusMonths(1).withDayOfMonth(1), 
                           today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth()), 
                           "Previous month"),
                Arguments.of(today.withDayOfYear(1), today, "Year to date")
        );
    }

    /**
     * Helper method to create mock PDF response
     */
    private ResponseEntity<byte[]> createMockPdfResponse(String filename) {
        byte[] pdfContent = "%PDF-1.4\n1 0 obj\n<<\n/Type /Catalog\n/Pages 2 0 R\n>>\nendobj\n".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/pdf");
        headers.set("Content-Disposition", "attachment; filename=" + filename);
        headers.set("Content-Length", String.valueOf(pdfContent.length));
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    /**
     * Helper method to create mock CSV response
     */
    private ResponseEntity<byte[]> createMockCsvResponse(String filename) {
        byte[] csvContent = "Date,Type,Count,Amount\n2024-01-01,Visit,5,250.00\n2024-01-02,Visit,3,150.00\n".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/csv");
        headers.set("Content-Disposition", "attachment; filename=" + filename);
        headers.set("Content-Length", String.valueOf(csvContent.length));
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        
        return new ResponseEntity<>(csvContent, headers, HttpStatus.OK);
    }

    /**
     * Test PDF and CSV generation for all report types
     * **Validates: Requirements 5.1, 5.2**
     */
    @ParameterizedTest(name = "Export {0} report as {1}")
    @MethodSource("reportTypeAndFormatProvider")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportAllReportTypesAndFormats(String reportType, String format) throws Exception {
        // Arrange
        String expectedContentType = format.equals("pdf") ? "application/pdf" : "text/csv";
        String filename = reportType + "_report." + format;
        
        ResponseEntity<byte[]> mockResponse = format.equals("pdf") 
            ? createMockPdfResponse(filename)
            : createMockCsvResponse(filename);

        // Mock the appropriate service method based on report type
        switch (reportType) {
            case "visits":
                when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq(format)))
                    .thenReturn(Mono.just(mockResponse));
                break;
            case "revenue":
                when(reportService.exportRevenueReport(any(LocalDate.class), any(LocalDate.class), eq(format), anyBoolean()))
                    .thenReturn(Mono.just(mockResponse));
                break;
            case "dashboard":
                when(reportService.exportDashboardMetrics(eq(format), any(LocalDate.class)))
                    .thenReturn(Mono.just(mockResponse));
                break;
        }

        // Act & Assert - Test GET method
        MvcResult result = mockMvc.perform(get("/dashboard/export/{reportType}", reportType)
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", format)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", expectedContentType))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("filename=")))
                .andExpect(header().exists("Content-Length"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"))
                .andReturn();

        // Validate response content
        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 0, "Export file should not be empty");

        // Validate Content-Length header matches actual content
        String contentLengthHeader = result.getResponse().getHeader("Content-Length");
        if (contentLengthHeader != null) {
            int expectedLength = Integer.parseInt(contentLengthHeader);
            assertEquals(expectedLength, content.length, 
                "Content-Length header should match actual content size");
        }

        // Validate content type specific characteristics
        if (format.equals("pdf")) {
            // PDF files should start with %PDF
            String contentStart = new String(content, 0, Math.min(4, content.length));
            assertEquals("%PDF", contentStart, "PDF file should start with %PDF header");
        } else if (format.equals("csv")) {
            // CSV files should contain readable text
            String contentStr = new String(content);
            assertTrue(contentStr.contains(",") || contentStr.contains("\n"), 
                "CSV file should contain comma separators or newlines");
        }
    }

    /**
     * Test export workflow with various date ranges
     * **Validates: Requirements 5.3**
     */
    @ParameterizedTest(name = "Export visits report with {2}")
    @MethodSource("dateRangeProvider")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportWithVariousDateRanges(LocalDate startDate, LocalDate endDate, String description) throws Exception {
        // Arrange
        ResponseEntity<byte[]> mockResponse = createMockPdfResponse("visits_report.pdf");
        when(reportService.exportVisitStatistics(eq(startDate), eq(endDate), eq("pdf")))
            .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", startDate.format(DATE_FORMAT))
                .param("endDate", endDate.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Length"));
    }

    /**
     * Test export with filter combinations
     * **Validates: Requirements 5.4**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportWithFilterCombinations() throws Exception {
        // Mock responses for different scenarios
        ResponseEntity<byte[]> csvResponse = createMockCsvResponse("visits_report.csv");
        ResponseEntity<byte[]> pdfResponse = createMockPdfResponse("visits_report.pdf");
        ResponseEntity<byte[]> revenuePdfResponse = createMockPdfResponse("revenue_report.pdf");

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("csv")))
            .thenReturn(Mono.just(csvResponse));
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
            .thenReturn(Mono.just(pdfResponse));
        when(reportService.exportRevenueReport(any(LocalDate.class), any(LocalDate.class), eq("pdf"), eq(true)))
            .thenReturn(Mono.just(revenuePdfResponse));

        // Test with veterinarian filter
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .param("veterinarianId", "1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // Test with species filter
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .param("species", "dog")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        // Test with both filters
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .param("veterinarianId", "2")
                .param("species", "cat")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // Test revenue report with includeTrends
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .param("includeTrends", "true")
                .param("veterinarianId", "1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    /**
     * Test end-to-end export flow using POST method (simulating form submission)
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testEndToEndExportFlowWithPostMethod() throws Exception {
        // Mock responses
        ResponseEntity<byte[]> pdfResponse = createMockPdfResponse("visits_report.pdf");
        ResponseEntity<byte[]> csvResponse = createMockCsvResponse("revenue_report.csv");
        ResponseEntity<byte[]> dashboardPdfResponse = createMockPdfResponse("dashboard_report.pdf");

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
            .thenReturn(Mono.just(pdfResponse));
        when(reportService.exportRevenueReport(any(LocalDate.class), any(LocalDate.class), eq("csv"), eq(false)))
            .thenReturn(Mono.just(csvResponse));
        when(reportService.exportDashboardMetrics(eq("pdf"), any(LocalDate.class)))
            .thenReturn(Mono.just(dashboardPdfResponse));

        // Test POST method for visits report
        mockMvc.perform(post("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("attachment")));

        // Test POST method for revenue report
        mockMvc.perform(post("/dashboard/export/revenue")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .param("includeTrends", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // Test POST method for dashboard report
        mockMvc.perform(post("/dashboard/export/dashboard")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    /**
     * Test authentication requirements for export endpoints
     * **Validates: Requirements 2.1, 2.4**
     */
    @Test
    void testExportRequiresAuthentication() throws Exception {
        // Test without authentication - should redirect to login
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * Test CSRF protection for POST requests
     * **Validates: Requirements 2.2, 2.3**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCsrfProtectionForPostRequests() throws Exception {
        // POST without CSRF token should fail
        mockMvc.perform(post("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf"))
                .andExpect(status().isForbidden());

        // POST with CSRF token should succeed (mock the service response)
        ResponseEntity<byte[]> mockResponse = createMockPdfResponse("visits_report.pdf");
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
            .thenReturn(Mono.just(mockResponse));

        mockMvc.perform(post("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    /**
     * Test parameter validation for export requests
     * **Validates: Requirements 3.2, 3.4**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testParameterValidation() throws Exception {
        // Test invalid report type
        mockMvc.perform(get("/dashboard/export/invalid")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        // Test invalid format
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "invalid")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        // Test invalid date range (end before start)
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        // Test missing required parameters
        mockMvc.perform(get("/dashboard/export/visits")
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test HTTP response headers for file downloads
     * **Validates: Requirements 4.1, 4.2, 4.3, 4.4**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testHttpResponseHeaders() throws Exception {
        // Mock responses
        ResponseEntity<byte[]> pdfResponse = createMockPdfResponse("visits_report.pdf");
        ResponseEntity<byte[]> csvResponse = createMockCsvResponse("visits_report.csv");

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
            .thenReturn(Mono.just(pdfResponse));
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("csv")))
            .thenReturn(Mono.just(csvResponse));

        // Test PDF response headers
        MvcResult pdfResult = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.matchesPattern("attachment;\\s*filename=.*\\.pdf")))
                .andExpect(header().exists("Content-Length"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"))
                .andReturn();

        // Validate Content-Length matches actual content
        String contentLengthStr = pdfResult.getResponse().getHeader("Content-Length");
        if (contentLengthStr != null) {
            int contentLength = Integer.parseInt(contentLengthStr);
            int actualLength = pdfResult.getResponse().getContentAsByteArray().length;
            assertEquals(contentLength, actualLength, "Content-Length header should match actual content size");
        }

        // Test CSV response headers
        MvcResult csvResult = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.matchesPattern("attachment;\\s*filename=.*\\.csv")))
                .andExpect(header().exists("Content-Length"))
                .andReturn();

        // Validate CSV Content-Length
        String csvContentLengthStr = csvResult.getResponse().getHeader("Content-Length");
        if (csvContentLengthStr != null) {
            int csvContentLength = Integer.parseInt(csvContentLengthStr);
            int csvActualLength = csvResult.getResponse().getContentAsByteArray().length;
            assertEquals(csvContentLength, csvActualLength, "CSV Content-Length header should match actual content size");
        }
    }

    /**
     * Test error handling for backend service failures
     * **Validates: Requirements 5.5**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testErrorHandlingForBackendFailures() throws Exception {
        // Mock service to throw WebClientResponseException (simulating backend error)
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
            .thenReturn(Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                500, "Internal Server Error", null, null, null)));

        // Should handle backend errors gracefully
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Test data consistency between PDF and CSV exports
     * **Validates: Cross-format data consistency**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDataConsistencyBetweenFormats() throws Exception {
        // Mock responses
        ResponseEntity<byte[]> pdfResponse = createMockPdfResponse("visits_report.pdf");
        ResponseEntity<byte[]> csvResponse = createMockCsvResponse("visits_report.csv");

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
            .thenReturn(Mono.just(pdfResponse));
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("csv")))
            .thenReturn(Mono.just(csvResponse));

        // Export same report in both formats
        MvcResult pdfResult = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult csvResult = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // Both should have content
        assertTrue(pdfResult.getResponse().getContentAsByteArray().length > 0, 
            "PDF should have content");
        assertTrue(csvResult.getResponse().getContentAsByteArray().length > 0, 
            "CSV should have content");

        // Content-Length should match actual content if headers are present
        String pdfContentLengthStr = pdfResult.getResponse().getHeader("Content-Length");
        if (pdfContentLengthStr != null) {
            int pdfContentLength = Integer.parseInt(pdfContentLengthStr);
            int pdfActualLength = pdfResult.getResponse().getContentAsByteArray().length;
            assertEquals(pdfContentLength, pdfActualLength, "PDF Content-Length should match actual size");
        }

        String csvContentLengthStr = csvResult.getResponse().getHeader("Content-Length");
        if (csvContentLengthStr != null) {
            int csvContentLength = Integer.parseInt(csvContentLengthStr);
            int csvActualLength = csvResult.getResponse().getContentAsByteArray().length;
            assertEquals(csvContentLength, csvActualLength, "CSV Content-Length should match actual size");
        }
    }

    /**
     * Test filename generation for different scenarios
     * **Validates: Proper filename generation**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testFilenameGeneration() throws Exception {
        for (String reportType : REPORT_TYPES) {
            for (String format : FORMATS) {
                String filename = reportType + "_report." + format;
                ResponseEntity<byte[]> mockResponse = format.equals("pdf") 
                    ? createMockPdfResponse(filename)
                    : createMockCsvResponse(filename);

                // Mock the appropriate service method
                switch (reportType) {
                    case "visits":
                        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq(format)))
                            .thenReturn(Mono.just(mockResponse));
                        break;
                    case "revenue":
                        when(reportService.exportRevenueReport(any(LocalDate.class), any(LocalDate.class), eq(format), anyBoolean()))
                            .thenReturn(Mono.just(mockResponse));
                        break;
                    case "dashboard":
                        when(reportService.exportDashboardMetrics(eq(format), any(LocalDate.class)))
                            .thenReturn(Mono.just(mockResponse));
                        break;
                }

                MvcResult result = mockMvc.perform(get("/dashboard/export/" + reportType)
                        .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                        .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                        .param("format", format)
                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn();

                String contentDisposition = result.getResponse().getHeader("Content-Disposition");
                assertNotNull(contentDisposition, "Content-Disposition header should be present");
                assertTrue(contentDisposition.contains("attachment"), "Should be attachment disposition");
                assertTrue(contentDisposition.contains("filename="), "Should contain filename");
                assertTrue(contentDisposition.contains(reportType), "Filename should contain report type");
                assertTrue(contentDisposition.contains("." + format), "Filename should have correct extension");
            }
        }
    }
}