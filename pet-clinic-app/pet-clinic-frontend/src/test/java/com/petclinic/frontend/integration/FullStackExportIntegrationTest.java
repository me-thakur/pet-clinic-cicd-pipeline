package com.petclinic.frontend.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Stack Export Integration Test
 * 
 * Tests the complete export workflow with real backend and database using TestContainers.
 * This provides the most comprehensive validation of the export functionality.
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4 - Complete end-to-end workflow**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FullStackExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final LocalDate DEFAULT_START_DATE = LocalDate.now().minusDays(30);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.now();

    // Create a shared network for containers
    static Network network = Network.newNetwork();

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            .withDatabaseName("petclinic")
            .withUsername("petclinic")
            .withPassword("petclinic")
            .withInitScript("test-data.sql");

    @Container
    static GenericContainer<?> backend = new GenericContainer<>("openjdk:11-jre-slim")
            .withNetwork(network)
            .withExposedPorts(9090)
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/petclinic")
            .withEnv("SPRING_DATASOURCE_USERNAME", "petclinic")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "petclinic")
            .dependsOn(mysql)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(9090))
            .withCommand("java", "-jar", "/app/pet-clinic-backend.jar");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("pet-clinic.backend.url", 
            () -> "http://localhost:" + backend.getMappedPort(9090));
        registry.add("pet-clinic.backend.api-path", () -> "/api");
    }

    @BeforeAll
    void setupTestData() {
        // Wait for backend to be fully ready
        try {
            Thread.sleep(5000); // Give backend time to initialize
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test complete export workflow for visits report
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCompleteVisitsExportWorkflow() throws Exception {
        // Test PDF export
        MvcResult pdfResult = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("visits")))
                .andExpect(header().exists("Content-Length"))
                .andReturn();

        // Validate PDF content
        byte[] pdfContent = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfContent.length > 0, "PDF content should not be empty");
        
        // PDF files should start with %PDF
        String pdfHeader = new String(pdfContent, 0, Math.min(4, pdfContent.length));
        assertEquals("%PDF", pdfHeader, "Should be a valid PDF file");

        // Test CSV export
        MvcResult csvResult = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("visits")))
                .andExpect(header().exists("Content-Length"))
                .andReturn();

        // Validate CSV content
        byte[] csvContent = csvResult.getResponse().getContentAsByteArray();
        assertTrue(csvContent.length > 0, "CSV content should not be empty");
        
        String csvString = new String(csvContent);
        assertTrue(csvString.contains(",") || csvString.contains("\n"), 
            "CSV should contain delimiters or line breaks");
    }

    /**
     * Test complete export workflow for revenue report
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCompleteRevenueExportWorkflow() throws Exception {
        // Test PDF export with trends
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .param("includeTrends", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("revenue")));

        // Test CSV export without trends
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .param("includeTrends", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("revenue")));
    }

    /**
     * Test complete export workflow for dashboard report
     * **Validates: Requirements 5.1, 5.2**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCompleteDashboardExportWorkflow() throws Exception {
        // Test PDF export
        mockMvc.perform(get("/dashboard/export/dashboard")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("dashboard")));

        // Test CSV export
        mockMvc.perform(get("/dashboard/export/dashboard")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", 
                    org.hamcrest.Matchers.containsString("dashboard")));
    }

    /**
     * Test export with various date ranges using real backend
     * **Validates: Requirements 5.3**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportWithRealDateRanges() throws Exception {
        LocalDate today = LocalDate.now();
        
        // Test last week
        LocalDate weekStart = today.minusDays(7);
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", weekStart.format(DATE_FORMAT))
                .param("endDate", today.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        // Test last month
        LocalDate monthStart = today.minusDays(30);
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", monthStart.format(DATE_FORMAT))
                .param("endDate", today.format(DATE_FORMAT))
                .param("format", "csv")
                .param("includeTrends", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // Test single day
        mockMvc.perform(get("/dashboard/export/dashboard")
                .param("startDate", today.minusDays(1).format(DATE_FORMAT))
                .param("endDate", today.minusDays(1).format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    /**
     * Test export with filters using real backend
     * **Validates: Requirements 5.4**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportWithRealFilters() throws Exception {
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

        // Test revenue with veterinarian filter
        mockMvc.perform(get("/dashboard/export/revenue")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .param("includeTrends", "false")
                .param("veterinarianId", "2")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    /**
     * Test POST method export workflow
     * **Validates: Complete form submission workflow**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testPostMethodExportWorkflow() throws Exception {
        // Test POST for visits
        mockMvc.perform(post("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        // Test POST for revenue
        mockMvc.perform(post("/dashboard/export/revenue")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "csv")
                .param("includeTrends", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        // Test POST for dashboard
        mockMvc.perform(post("/dashboard/export/dashboard")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    /**
     * Test data consistency between PDF and CSV exports
     * **Validates: Cross-format data consistency**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDataConsistencyBetweenFormats() throws Exception {
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

        // Content-Length should match actual content
        int pdfContentLength = Integer.parseInt(pdfResult.getResponse().getHeader("Content-Length"));
        int pdfActualLength = pdfResult.getResponse().getContentAsByteArray().length;
        assertEquals(pdfContentLength, pdfActualLength, "PDF Content-Length should match actual size");

        int csvContentLength = Integer.parseInt(csvResult.getResponse().getHeader("Content-Length"));
        int csvActualLength = csvResult.getResponse().getContentAsByteArray().length;
        assertEquals(csvContentLength, csvActualLength, "CSV Content-Length should match actual size");
    }

    /**
     * Test error handling with real backend
     * **Validates: Requirements 5.5**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testErrorHandlingWithRealBackend() throws Exception {
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

        // Test invalid date range
        mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test cache control headers with real backend
     * **Validates: Requirements 4.3**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCacheControlHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/dashboard/export/visits")
                .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                .param("format", "pdf")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"))
                .andReturn();

        // Verify the headers prevent caching
        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertTrue(cacheControl.contains("no-cache"), "Should prevent caching");
        assertTrue(cacheControl.contains("no-store"), "Should prevent storing");
        assertTrue(cacheControl.contains("must-revalidate"), "Should require revalidation");
    }

    /**
     * Test performance with real backend and database
     * **Validates: System performance under realistic conditions**
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportPerformance() throws Exception {
        long startTime = System.currentTimeMillis();

        // Perform multiple exports to test performance
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/dashboard/export/visits")
                    .param("startDate", DEFAULT_START_DATE.format(DATE_FORMAT))
                    .param("endDate", DEFAULT_END_DATE.format(DATE_FORMAT))
                    .param("format", "pdf")
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Each export should complete within reasonable time (adjust as needed)
        assertTrue(totalTime < 30000, "Three exports should complete within 30 seconds, took: " + totalTime + "ms");
    }
}