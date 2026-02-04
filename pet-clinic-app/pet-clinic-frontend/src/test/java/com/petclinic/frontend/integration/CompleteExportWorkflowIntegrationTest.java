package com.petclinic.frontend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Complete Export Workflow Integration Test
 * 
 * Comprehensive integration tests for the complete export workflow covering:
 * - PDF and CSV generation for all report types (visits, revenue, dashboard)
 * - End-to-end export flow from frontend button click to file download
 * - Various date ranges and filter combinations
 * - Authentication and security validation
 * - HTTP response header validation
 * - File content validation
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompleteExportWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static GenericContainer<?> mockBackend = new GenericContainer<>("wiremock/wiremock:2.35.0")
            .withExposedPorts(8080)
            .withCommand("--port", "8080", "--global-response-templating")
            .waitingFor(Wait.forHttp("/").forStatusCode(404));

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // Test data constants
    private static final LocalDate DEFAULT_START_DATE = LocalDate.now().minusDays(30);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.now();
    private static final String[] REPORT_TYPES = {"visits", "revenue", "dashboard"};
    private static final String[] FORMATS = {"pdf", "csv"};

    @BeforeEach
    void setUp() {
        // Configure mock backend URL for tests
        System.setProperty("pet-clinic.backend.url", 
            "http://localhost:" + mockBackend.getMappedPort(8080));
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
     * Test PDF and CSV generation for all report types
     * **Validates: Requirements 5.1, 5.2**
     */
    @ParameterizedTest(name = "Export {0} report as {1}")
    @MethodSource("reportTypeAndFormatProvider")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportAllReportTypesAndFormats(String reportType, String format) throws Exception {
        // Arrange
        String expectedContentType = format.equals("pdf") ? "application/pdf" : "text/csv";
        String expectedFilename = reportType + "_report_" + DEFAULT_START_DATE + "_to_" + DEFAULT_END_DATE + "." + format;

        // Act & Assert
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

        // Validate file content is not empty
        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 0, "Export file should not be empty");

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

        // POST with CSRF token should succeed
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
        int contentLength = Integer.parseInt(pdfResult.getResponse().getHeader("Content-Length"));
        int actualLength = pdfResult.getResponse().getContentAsByteArray().length;
        assertEquals(contentLength, actualLength, "Content-Length header should match actual content size");

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
        int csvContentLength = Integer.parseInt(csvResult.getResponse().getHeader("Content-Length"));
        int csvActualLength = csvResult.getResponse().getContentAsByteArray().length;
        assertEquals(csvContentLength, csvActualLength, "CSV Content-Length header should match actual content size");
    }

    /**
     * Test error handling for various failure scenarios
     * **Validates: Requirements 5.5**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testErrorHandling() throws Exception {
        // Test with future date range (should handle gracefully)
        LocalDate futureStart = LocalDate.now().plusDays(1);
        LocalDate futureEnd = LocalDate.now().plusDays(30);
        
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", futureStart.format(DATE_FORMAT))
                .param("endDate", futureEnd.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk()); // Should handle gracefully, possibly with empty report

        // Test with very large date range
        LocalDate veryOldStart = LocalDate.now().minusYears(10);
        
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", veryOldStart.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .with(csrf()))
                .andExpect(status().isOk()); // Should handle gracefully

        // Test with invalid veterinarian ID
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .param("veterinarianId", "-1")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test concurrent export requests
     * **Validates: System stability under load**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testConcurrentExportRequests() throws Exception {
        // Simulate multiple concurrent export requests
        Thread[] threads = new Thread[5];
        Exception[] exceptions = new Exception[5];

        for (int i = 0; i < 5; i++) {
            final int threadIndex = i;
            final String format = (i % 2 == 0) ? "pdf" : "csv";
            final String reportType = REPORT_TYPES[i % REPORT_TYPES.length];

            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/dashboard/export/" + reportType)
                            .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                            .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                            .param("format", format)
                            .with(csrf()))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000); // 10 second timeout
        }

        // Check for exceptions
        for (int i = 0; i < exceptions.length; i++) {
            assertNull(exceptions[i], "Thread " + i + " should not have thrown an exception: " + 
                (exceptions[i] != null ? exceptions[i].getMessage() : ""));
        }
    }

    /**
     * Test export with edge case data scenarios
     * **Validates: Data integrity and edge case handling**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportWithEdgeCaseData() throws Exception {
        // Test with single day range
        LocalDate singleDay = LocalDate.now().minusDays(1);
        
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", singleDay.format(DATE_FORMAT))
                .param("endDate", singleDay.format(DATE_FORMAT))
                .param("format", "csv")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // Test with weekend dates
        LocalDate saturday = LocalDate.now().with(java.time.DayOfWeek.SATURDAY);
        LocalDate sunday = saturday.plusDays(1);
        
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", saturday.format(DATE_FORMAT))
                .param("endDate", sunday.format(DATE_FORMAT))
                .param("format", "pdf")
                .param("includeTrends", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
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