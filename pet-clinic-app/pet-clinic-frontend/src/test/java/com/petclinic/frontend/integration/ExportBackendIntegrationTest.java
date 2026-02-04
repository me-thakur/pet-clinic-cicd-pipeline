package com.petclinic.frontend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.frontend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Export Backend Integration Test
 * 
 * Tests the integration between frontend ReportService and backend export APIs.
 * Validates the complete export workflow from service layer perspective.
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class ExportBackendIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static GenericContainer<?> mockBackend = new GenericContainer<>("wiremock/wiremock:2.35.0")
            .withExposedPorts(8080)
            .withCommand("--port", "8080", "--global-response-templating")
            .waitingFor(Wait.forHttp("/").forStatusCode(404));

    private static final LocalDate DEFAULT_START_DATE = LocalDate.now().minusDays(30);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.now();

    @BeforeEach
    void setUp() {
        // Configure mock backend URL for tests
        System.setProperty("pet-clinic.backend.url", 
            "http://localhost:" + mockBackend.getMappedPort(8080));
    }

    /**
     * Test data provider for report types and formats
     */
    private static Stream<Arguments> exportParametersProvider() {
        return Stream.of(
                Arguments.of("visits", "pdf"),
                Arguments.of("visits", "csv"),
                Arguments.of("revenue", "pdf"),
                Arguments.of("revenue", "csv"),
                Arguments.of("dashboard", "pdf"),
                Arguments.of("dashboard", "csv")
        );
    }

    /**
     * Test export service methods for all report types and formats
     * **Validates: Requirements 5.1, 5.2**
     */
    @ParameterizedTest(name = "Export {0} report as {1} via service")
    @MethodSource("exportParametersProvider")
    void testExportServiceMethods(String reportType, String format) {
        Mono<ResponseEntity<byte[]>> exportMono;

        // Call appropriate export method based on report type
        switch (reportType) {
            case "visits":
                exportMono = reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, format);
                break;
            case "revenue":
                exportMono = reportService.exportRevenueReport(DEFAULT_START_DATE, DEFAULT_END_DATE, format, true);
                break;
            case "dashboard":
                exportMono = reportService.exportDashboardMetrics(format, DEFAULT_START_DATE);
                break;
            default:
                fail("Unknown report type: " + reportType);
                return;
        }

        // Verify the export request
        StepVerifier.create(exportMono)
                .expectNextMatches(response -> {
                    // Validate response status
                    assertTrue(response.getStatusCode().is2xxSuccessful(), 
                        "Export should return successful status");

                    // Validate content type
                    String expectedContentType = format.equals("pdf") ? "application/pdf" : "text/csv";
                    String actualContentType = response.getHeaders().getContentType().toString();
                    assertTrue(actualContentType.contains(expectedContentType), 
                        "Content type should be " + expectedContentType + " but was " + actualContentType);

                    // Validate content is not empty
                    byte[] content = response.getBody();
                    assertNotNull(content, "Export content should not be null");
                    assertTrue(content.length > 0, "Export content should not be empty");

                    // Validate content disposition header
                    String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
                    if (contentDisposition != null) {
                        assertTrue(contentDisposition.contains("attachment"), 
                            "Content-Disposition should indicate attachment");
                        assertTrue(contentDisposition.contains("filename"), 
                            "Content-Disposition should contain filename");
                    }

                    return true;
                })
                .verifyComplete();
    }

    /**
     * Test export with various date ranges
     * **Validates: Requirements 5.3**
     */
    @Test
    void testExportWithVariousDateRanges() {
        LocalDate today = LocalDate.now();
        
        // Test different date ranges
        LocalDate[][] dateRanges = {
                {today.minusDays(7), today},           // Last week
                {today.minusDays(30), today},          // Last month
                {today.minusDays(90), today},          // Last quarter
                {today.withDayOfMonth(1), today},      // This month
                {today.minusMonths(1).withDayOfMonth(1), 
                 today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth())} // Previous month
        };

        for (LocalDate[] dateRange : dateRanges) {
            LocalDate startDate = dateRange[0];
            LocalDate endDate = dateRange[1];

            StepVerifier.create(reportService.exportVisitStatistics(startDate, endDate, "pdf"))
                    .expectNextMatches(response -> {
                        assertTrue(response.getStatusCode().is2xxSuccessful(), 
                            "Export should succeed for date range " + startDate + " to " + endDate);
                        assertNotNull(response.getBody(), "Export content should not be null");
                        assertTrue(response.getBody().length > 0, "Export content should not be empty");
                        return true;
                    })
                    .verifyComplete();
        }
    }

    /**
     * Test export with filter combinations
     * **Validates: Requirements 5.4**
     */
    @Test
    void testExportWithFilters() {
        // Test visits export (no additional filters in current implementation)
        StepVerifier.create(reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, "csv"))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    assertTrue(response.getHeaders().getContentType().toString().contains("text/csv"));
                    return true;
                })
                .verifyComplete();

        // Test revenue export with trends
        StepVerifier.create(reportService.exportRevenueReport(DEFAULT_START_DATE, DEFAULT_END_DATE, "pdf", true))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    assertTrue(response.getHeaders().getContentType().toString().contains("application/pdf"));
                    return true;
                })
                .verifyComplete();

        // Test revenue export without trends
        StepVerifier.create(reportService.exportRevenueReport(DEFAULT_START_DATE, DEFAULT_END_DATE, "csv", false))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    assertTrue(response.getHeaders().getContentType().toString().contains("text/csv"));
                    return true;
                })
                .verifyComplete();

        // Test dashboard export with specific date
        StepVerifier.create(reportService.exportDashboardMetrics("pdf", DEFAULT_START_DATE))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    assertTrue(response.getHeaders().getContentType().toString().contains("application/pdf"));
                    return true;
                })
                .verifyComplete();

        // Test dashboard export without specific date
        StepVerifier.create(reportService.exportDashboardMetrics("csv", null))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    assertTrue(response.getHeaders().getContentType().toString().contains("text/csv"));
                    return true;
                })
                .verifyComplete();
    }

    /**
     * Test error handling for invalid parameters
     * **Validates: Requirements 5.5**
     */
    @Test
    void testErrorHandlingForInvalidParameters() {
        // Test with invalid date range (end before start)
        LocalDate invalidStart = LocalDate.now();
        LocalDate invalidEnd = LocalDate.now().minusDays(1);

        StepVerifier.create(reportService.exportVisitStatistics(invalidStart, invalidEnd, "pdf"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException)
                .verify();

        // Test with null dates
        StepVerifier.create(reportService.exportVisitStatistics(null, DEFAULT_END_DATE, "pdf"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException || 
                    throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException)
                .verify();

        // Test with invalid format (should be handled by backend)
        StepVerifier.create(reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, "invalid"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException)
                .verify();
    }

    /**
     * Test timeout handling for long-running exports
     * **Validates: System resilience**
     */
    @Test
    void testTimeoutHandling() {
        // Test with a very large date range that might timeout
        LocalDate veryOldStart = LocalDate.now().minusYears(5);
        
        StepVerifier.create(reportService.exportVisitStatistics(veryOldStart, DEFAULT_END_DATE, "pdf")
                .timeout(Duration.ofSeconds(30))) // Set reasonable timeout
                .expectNextMatches(response -> {
                    // Should either succeed or timeout gracefully
                    return response.getStatusCode().is2xxSuccessful();
                })
                .verifyComplete();
    }

    /**
     * Test concurrent export requests at service level
     * **Validates: Service layer concurrency handling**
     */
    @Test
    void testConcurrentServiceRequests() {
        // Create multiple concurrent export requests
        Mono<ResponseEntity<byte[]>>[] exportMonos = new Mono[6];
        
        exportMonos[0] = reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, "pdf");
        exportMonos[1] = reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, "csv");
        exportMonos[2] = reportService.exportRevenueReport(DEFAULT_START_DATE, DEFAULT_END_DATE, "pdf", true);
        exportMonos[3] = reportService.exportRevenueReport(DEFAULT_START_DATE, DEFAULT_END_DATE, "csv", false);
        exportMonos[4] = reportService.exportDashboardMetrics("pdf", DEFAULT_START_DATE);
        exportMonos[5] = reportService.exportDashboardMetrics("csv", null);

        // Execute all requests concurrently
        Mono<ResponseEntity<byte[]>> combinedMono = Mono.zip(
                exportMonos[0], exportMonos[1], exportMonos[2], 
                exportMonos[3], exportMonos[4], exportMonos[5])
                .map(tuple -> {
                    // Validate all responses are successful
                    ResponseEntity<byte[]>[] responses = new ResponseEntity[]{
                        tuple.getT1(), tuple.getT2(), tuple.getT3(),
                        tuple.getT4(), tuple.getT5(), tuple.getT6()
                    };
                    
                    for (int i = 0; i < responses.length; i++) {
                        assertTrue(responses[i].getStatusCode().is2xxSuccessful(), 
                            "Response " + i + " should be successful");
                        assertNotNull(responses[i].getBody(), 
                            "Response " + i + " body should not be null");
                        assertTrue(responses[i].getBody().length > 0, 
                            "Response " + i + " body should not be empty");
                    }
                    
                    return responses[0]; // Return first response for verification
                });

        StepVerifier.create(combinedMono)
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();
    }

    /**
     * Test export service with edge case dates
     * **Validates: Edge case handling**
     */
    @Test
    void testExportWithEdgeCaseDates() {
        LocalDate today = LocalDate.now();
        
        // Test with same start and end date
        StepVerifier.create(reportService.exportVisitStatistics(today, today, "csv"))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    return true;
                })
                .verifyComplete();

        // Test with leap year date
        LocalDate leapYearDate = LocalDate.of(2024, 2, 29);
        if (leapYearDate.isBefore(today)) {
            StepVerifier.create(reportService.exportRevenueReport(leapYearDate, leapYearDate, "pdf", false))
                    .expectNextMatches(response -> {
                        assertTrue(response.getStatusCode().is2xxSuccessful());
                        return true;
                    })
                    .verifyComplete();
        }

        // Test with year boundary dates
        LocalDate yearEnd = LocalDate.of(today.getYear() - 1, 12, 31);
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        
        StepVerifier.create(reportService.exportDashboardMetrics("csv", yearEnd))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    return true;
                })
                .verifyComplete();
    }

    /**
     * Test response header validation at service level
     * **Validates: Requirements 4.1, 4.2, 4.3, 4.4**
     */
    @Test
    void testResponseHeaderValidation() {
        // Test PDF export headers
        StepVerifier.create(reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, "pdf"))
                .expectNextMatches(response -> {
                    // Validate Content-Type
                    String contentType = response.getHeaders().getContentType().toString();
                    assertTrue(contentType.contains("application/pdf"), 
                        "PDF export should have application/pdf content type");

                    // Validate Content-Length if present
                    if (response.getHeaders().getContentLength() > 0) {
                        assertEquals(response.getHeaders().getContentLength(), 
                                   response.getBody().length,
                                   "Content-Length should match actual content size");
                    }

                    // Validate cache control headers if present
                    String cacheControl = response.getHeaders().getCacheControl();
                    if (cacheControl != null) {
                        assertTrue(cacheControl.contains("no-cache") || cacheControl.contains("no-store"),
                            "Cache control should prevent caching");
                    }

                    return true;
                })
                .verifyComplete();

        // Test CSV export headers
        StepVerifier.create(reportService.exportRevenueReport(DEFAULT_START_DATE, DEFAULT_END_DATE, "csv", true))
                .expectNextMatches(response -> {
                    // Validate Content-Type
                    String contentType = response.getHeaders().getContentType().toString();
                    assertTrue(contentType.contains("text/csv"), 
                        "CSV export should have text/csv content type");

                    // Validate content is not empty
                    assertNotNull(response.getBody(), "CSV content should not be null");
                    assertTrue(response.getBody().length > 0, "CSV content should not be empty");

                    return true;
                })
                .verifyComplete();
    }

    /**
     * Test export service resilience to backend failures
     * **Validates: Error handling and resilience**
     */
    @Test
    void testServiceResilienceToBackendFailures() {
        // This test would typically involve configuring the mock backend to return errors
        // For now, we test that the service properly propagates WebClient exceptions
        
        // Test with a backend URL that doesn't exist (simulating backend down)
        System.setProperty("pet-clinic.backend.url", "http://localhost:99999");
        
        StepVerifier.create(reportService.exportVisitStatistics(DEFAULT_START_DATE, DEFAULT_END_DATE, "pdf"))
                .expectError(org.springframework.web.reactive.function.client.WebClientRequestException.class)
                .verify();

        // Restore proper backend URL
        System.setProperty("pet-clinic.backend.url", 
            "http://localhost:" + mockBackend.getMappedPort(8080));
    }
}