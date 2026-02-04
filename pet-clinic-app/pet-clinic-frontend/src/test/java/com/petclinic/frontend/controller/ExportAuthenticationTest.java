package com.petclinic.frontend.controller;

import com.petclinic.frontend.config.SecurityConfig;
import com.petclinic.frontend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test authentication flow for export requests
 * Validates: Requirements 2.1, 2.4
 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
public class ExportAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private com.petclinic.frontend.service.DashboardService dashboardService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithValidAuthentication() throws Exception {
        // Mock successful export response
        byte[] mockPdfData = "Mock PDF content".getBytes();
        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"visits-report.pdf\"")
                .body(mockPdfData);

        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.just(mockResponse));

        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"visits-report.pdf\""))
                .andExpect(content().bytes(mockPdfData));
    }

    @Test
    public void testExportWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithBackendAuthenticationFailure() throws Exception {
        // Mock authentication failure from backend
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                        401, "Unauthorized", null, "Authentication failed".getBytes(), null)));

        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(content().json("{\"error\":\"Backend authentication failed. Please log in again.\",\"redirect\":\"/login\"}"));
    }

    @Test
    @WithMockUser(username = "staff1", roles = {"STAFF"})
    public void testExportWithInsufficientPermissions() throws Exception {
        // Mock access denied from backend
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                        403, "Forbidden", null, "Access denied".getBytes(), null)));

        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(content().json("{\"error\":\"Access denied. Insufficient permissions for export.\"}"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithInvalidReportType() throws Exception {
        mockMvc.perform(get("/dashboard/export/invalid")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(content().json("{\"error\":\"Invalid report type. Supported types: visits, revenue, dashboard\"}"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithServiceUnavailable() throws Exception {
        // Mock service unavailable - return null response
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.empty()); // Return empty Mono instead of null

        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(content().json("{\"error\":\"Export service unavailable\"}"));
    }

    @Test
    @WithMockUser(username = "vet1", roles = {"VET"})
    public void testExportWithVetRole() throws Exception {
        // Mock successful export response for vet user
        byte[] mockCsvData = "Mock CSV content".getBytes();
        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"revenue-report.csv\"")
                .body(mockCsvData);

        when(reportService.exportRevenueReport(any(LocalDate.class), any(LocalDate.class), eq("csv"), eq(false)))
                .thenReturn(Mono.just(mockResponse));

        mockMvc.perform(get("/dashboard/export/revenue")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "csv")
                        .param("includeTrends", "false")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(content().bytes(mockCsvData));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportDashboardMetrics() throws Exception {
        // Mock successful dashboard export
        byte[] mockPdfData = "Mock Dashboard PDF".getBytes();
        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"dashboard-report.pdf\"")
                .body(mockPdfData);

        when(reportService.exportDashboardMetrics(eq("pdf"), any(LocalDate.class)))
                .thenReturn(Mono.just(mockResponse));

        mockMvc.perform(get("/dashboard/export/dashboard")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf;charset=UTF-8"))
                .andExpect(content().bytes(mockPdfData));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithUnexpectedError() throws Exception {
        // Mock unexpected error
        when(reportService.exportVisitStatistics(any(LocalDate.class), any(LocalDate.class), eq("pdf")))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(content().json("{\"error\":\"Internal server error during export\"}"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithMissingParameters() throws Exception {
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testExportWithInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/dashboard/export/visits")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2024-01-31")
                        .param("format", "pdf")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}