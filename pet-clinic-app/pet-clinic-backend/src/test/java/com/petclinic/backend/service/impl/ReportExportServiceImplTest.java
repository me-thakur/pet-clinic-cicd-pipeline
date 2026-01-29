package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportExportServiceImpl
 * Tests PDF and CSV export functionality for all report types
 * Validates: Requirements 5.3
 */
@ExtendWith(MockitoExtension.class)
class ReportExportServiceImplTest {
    
    @Mock
    private ReportService reportService;
    
    @InjectMocks
    private ReportExportServiceImpl reportExportService;
    
    private VisitStatisticsReport visitStatsReport;
    private RevenueReport revenueReport;
    private DashboardMetrics dashboardMetrics;
    private ReportFilter reportFilter;
    
    @BeforeEach
    void setUp() {
        // Set up test data
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        // Visit statistics report
        visitStatsReport = new VisitStatisticsReport(startDate, endDate);
        visitStatsReport.setTotalVisits(100);
        visitStatsReport.setCompletedVisits(80);
        visitStatsReport.setScheduledVisits(15);
        visitStatsReport.setCancelledVisits(5);
        
        Map<String, Long> visitsByVet = new HashMap<>();
        visitsByVet.put("Dr. Smith", 50L);
        visitsByVet.put("Dr. Johnson", 30L);
        visitsByVet.put("Dr. Brown", 20L);
        visitStatsReport.setVisitsByVeterinarian(visitsByVet);
        
        Map<VisitType, Long> visitsByType = new HashMap<>();
        visitsByType.put(VisitType.WELLNESS_EXAM, 40L);
        visitsByType.put(VisitType.VACCINATION, 30L);
        visitsByType.put(VisitType.SURGERY, 20L);
        visitsByType.put(VisitType.EMERGENCY, 10L);
        visitStatsReport.setVisitsByType(visitsByType);
        
        Map<String, Long> visitsBySpecies = new HashMap<>();
        visitsBySpecies.put("Dog", 60L);
        visitsBySpecies.put("Cat", 30L);
        visitsBySpecies.put("Bird", 10L);
        visitStatsReport.setVisitsBySpecies(visitsBySpecies);
        
        Map<LocalDate, Long> visitsByDate = new HashMap<>();
        visitsByDate.put(LocalDate.of(2024, 1, 15), 5L);
        visitsByDate.put(LocalDate.of(2024, 1, 16), 8L);
        visitsByDate.put(LocalDate.of(2024, 1, 17), 3L);
        visitStatsReport.setVisitsByDate(visitsByDate);
        
        // Revenue report
        revenueReport = new RevenueReport(startDate, endDate);
        revenueReport.setTotalRevenue(new BigDecimal("15000.00"));
        revenueReport.setTotalPaidVisits(80);
        revenueReport.setTotalUnpaidVisits(20);
        
        Map<String, BigDecimal> revenueByVet = new HashMap<>();
        revenueByVet.put("Dr. Smith", new BigDecimal("8000.00"));
        revenueByVet.put("Dr. Johnson", new BigDecimal("4500.00"));
        revenueByVet.put("Dr. Brown", new BigDecimal("2500.00"));
        revenueReport.setRevenueByVeterinarian(revenueByVet);
        
        Map<String, BigDecimal> revenueBySpecies = new HashMap<>();
        revenueBySpecies.put("Dog", new BigDecimal("9000.00"));
        revenueBySpecies.put("Cat", new BigDecimal("4500.00"));
        revenueBySpecies.put("Bird", new BigDecimal("1500.00"));
        revenueReport.setRevenueBySpecies(revenueBySpecies);
        
        Map<String, BigDecimal> revenueByType = new HashMap<>();
        revenueByType.put("Checkup", new BigDecimal("6000.00"));
        revenueByType.put("Vaccination", new BigDecimal("3000.00"));
        revenueByType.put("Surgery", new BigDecimal("5000.00"));
        revenueByType.put("Emergency", new BigDecimal("1000.00"));
        revenueReport.setRevenueByVisitType(revenueByType);
        
        Map<LocalDate, BigDecimal> dailyRevenue = new HashMap<>();
        dailyRevenue.put(LocalDate.of(2024, 1, 15), new BigDecimal("500.00"));
        dailyRevenue.put(LocalDate.of(2024, 1, 16), new BigDecimal("800.00"));
        dailyRevenue.put(LocalDate.of(2024, 1, 17), new BigDecimal("300.00"));
        revenueReport.setDailyRevenue(dailyRevenue);
        
        // Add trend data
        RevenueReport.TrendData trendData = new RevenueReport.TrendData(
                new BigDecimal("12000.00"), new BigDecimal("15000.00"));
        revenueReport.setTrendData(trendData);
        
        // Dashboard metrics
        dashboardMetrics = new DashboardMetrics();
        dashboardMetrics.setTodayAppointments(12);
        dashboardMetrics.setTodayCompletedVisits(8);
        dashboardMetrics.setTodayPendingVisits(4);
        dashboardMetrics.setTodayRevenue(new BigDecimal("1200.00"));
        dashboardMetrics.setTotalActivePets(250);
        dashboardMetrics.setTotalVeterinarians(5);
        dashboardMetrics.setTotalVisitsThisMonth(100);
        dashboardMetrics.setMonthlyRevenue(new BigDecimal("15000.00"));
        dashboardMetrics.setVeterinarianUtilization(75.0);
        dashboardMetrics.setAppointmentCompletionRate(85.0);
        dashboardMetrics.setAverageVisitDuration(45.0);
        
        // Add upcoming appointments
        List<DashboardMetrics.UpcomingAppointment> upcomingAppointments = new ArrayList<>();
        DashboardMetrics.UpcomingAppointment appointment = new DashboardMetrics.UpcomingAppointment();
        appointment.setVisitId(1L);
        appointment.setPetName("Buddy");
        appointment.setOwnerName("John Doe");
        appointment.setVeterinarianName("Dr. Smith");
        appointment.setAppointmentTime(LocalDateTime.now().plusHours(2));
        appointment.setVisitType("Checkup");
        appointment.setDuration(30);
        upcomingAppointments.add(appointment);
        dashboardMetrics.setUpcomingAppointments(upcomingAppointments);
        
        // Add recent activities
        List<DashboardMetrics.RecentActivity> recentActivities = new ArrayList<>();
        DashboardMetrics.RecentActivity activity = new DashboardMetrics.RecentActivity();
        activity.setType("VISIT_COMPLETED");
        activity.setDescription("Checkup visit for Max");
        activity.setTimestamp(LocalDateTime.now().minusHours(1));
        activity.setEntityId("123");
        activity.setEntityType("VISIT");
        recentActivities.add(activity);
        dashboardMetrics.setRecentActivities(recentActivities);
        
        // Add alerts
        List<DashboardMetrics.Alert> alerts = new ArrayList<>();
        DashboardMetrics.Alert alert = new DashboardMetrics.Alert();
        alert.setType("WARNING");
        alert.setTitle("High Utilization");
        alert.setMessage("Veterinarian utilization is above 90%");
        alert.setSeverity("MEDIUM");
        alerts.add(alert);
        dashboardMetrics.setAlerts(alerts);
        
        // Report filter
        reportFilter = new ReportFilter(startDate, endDate);
        reportFilter.setVeterinarianId(1L);
        reportFilter.setSpecies(Arrays.asList("Dog", "Cat"));
        reportFilter.setVisitTypes(Arrays.asList(VisitType.WELLNESS_EXAM, VisitType.VACCINATION));
    }
    
    @Test
    void testExportVisitStatisticsToPdf() {
        // When
        byte[] pdfData = reportExportService.exportVisitStatisticsToPdf(visitStatsReport);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        
        // Verify PDF header (PDF files start with %PDF)
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportVisitStatisticsToCSV() {
        // When
        byte[] csvData = reportExportService.exportVisitStatisticsToCSV(visitStatsReport);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Visit Statistics Report"));
        assertTrue(csvContent.contains("Total Visits") && csvContent.contains("100"));
        assertTrue(csvContent.contains("Completed Visits") && csvContent.contains("80"));
        assertTrue(csvContent.contains("Dr. Smith") && csvContent.contains("50"));
        assertTrue(csvContent.contains("Dog") && csvContent.contains("60"));
    }
    
    @Test
    void testExportRevenueReportToPdf() {
        // When
        byte[] pdfData = reportExportService.exportRevenueReportToPdf(revenueReport);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        
        // Verify PDF header
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportRevenueReportToCSV() {
        // When
        byte[] csvData = reportExportService.exportRevenueReportToCSV(revenueReport);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Revenue Report"));
        assertTrue(csvContent.contains("Total Revenue") && csvContent.contains("$15000.00"));
        assertTrue(csvContent.contains("Total Paid Visits") && csvContent.contains("80"));
        assertTrue(csvContent.contains("Dr. Smith") && csvContent.contains("$8000.00"));
        assertTrue(csvContent.contains("Dog") && csvContent.contains("$9000.00"));
    }
    
    @Test
    void testExportDashboardMetricsToPdf() {
        // When
        byte[] pdfData = reportExportService.exportDashboardMetricsToPdf(dashboardMetrics);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        
        // Verify PDF header
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportDashboardMetricsToCSV() {
        // When
        byte[] csvData = reportExportService.exportDashboardMetricsToCSV(dashboardMetrics);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Dashboard Metrics Report"));
        assertTrue(csvContent.contains("Today's Appointments") && csvContent.contains("12"));
        assertTrue(csvContent.contains("Completed Visits") && csvContent.contains("8"));
        assertTrue(csvContent.contains("Total Active Pets") && csvContent.contains("250"));
        assertTrue(csvContent.contains("Buddy") && csvContent.contains("John Doe"));
        assertTrue(csvContent.contains("High Utilization"));
    }
    
    @Test
    void testExportMultipleVisitStatisticsToPdf() {
        // Given
        List<VisitStatisticsReport> reports = Arrays.asList(visitStatsReport, visitStatsReport);
        
        // When
        byte[] pdfData = reportExportService.exportMultipleVisitStatisticsToPdf(reports, "Multiple Reports");
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        
        // Verify PDF header
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportMultipleVisitStatisticsToCSV() {
        // Given
        List<VisitStatisticsReport> reports = Arrays.asList(visitStatsReport, visitStatsReport);
        
        // When
        byte[] csvData = reportExportService.exportMultipleVisitStatisticsToCSV(reports);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Multiple Visit Statistics Reports"));
        assertTrue(csvContent.contains("2024-01-01 to 2024-01-31") && csvContent.contains("100"));
    }
    
    @Test
    void testExportMultipleRevenueReportsToPdf() {
        // Given
        List<RevenueReport> reports = Arrays.asList(revenueReport, revenueReport);
        
        // When
        byte[] pdfData = reportExportService.exportMultipleRevenueReportsToPdf(reports, "Multiple Revenue Reports");
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        
        // Verify PDF header
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportMultipleRevenueReportsToCSV() {
        // Given
        List<RevenueReport> reports = Arrays.asList(revenueReport, revenueReport);
        
        // When
        byte[] csvData = reportExportService.exportMultipleRevenueReportsToCSV(reports);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Multiple Revenue Reports"));
        assertTrue(csvContent.contains("2024-01-01 to 2024-01-31") && csvContent.contains("$15000.00"));
    }
    
    @Test
    void testExportFilteredVisitStatisticsToPdf() {
        // Given
        when(reportService.generateVisitStatistics(any(ReportFilter.class))).thenReturn(visitStatsReport);
        
        // When
        byte[] pdfData = reportExportService.exportFilteredVisitStatisticsToPdf(reportFilter);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        verify(reportService).generateVisitStatistics(reportFilter);
        
        // Verify PDF header
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportFilteredVisitStatisticsToCSV() {
        // Given
        when(reportService.generateVisitStatistics(any(ReportFilter.class))).thenReturn(visitStatsReport);
        
        // When
        byte[] csvData = reportExportService.exportFilteredVisitStatisticsToCSV(reportFilter);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        verify(reportService).generateVisitStatistics(reportFilter);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Visit Statistics Report"));
    }
    
    @Test
    void testExportFilteredRevenueReportToPdf() {
        // Given
        when(reportService.generateRevenueReport(any(ReportFilter.class))).thenReturn(revenueReport);
        
        // When
        byte[] pdfData = reportExportService.exportFilteredRevenueReportToPdf(reportFilter);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        verify(reportService).generateRevenueReport(reportFilter);
        
        // Verify PDF header
        String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, 4));
        assertEquals("%PDF", pdfHeader);
    }
    
    @Test
    void testExportFilteredRevenueReportToCSV() {
        // Given
        when(reportService.generateRevenueReport(any(ReportFilter.class))).thenReturn(revenueReport);
        
        // When
        byte[] csvData = reportExportService.exportFilteredRevenueReportToCSV(reportFilter);
        
        // Then
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        verify(reportService).generateRevenueReport(reportFilter);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Revenue Report"));
    }
    
    @Test
    void testGetExportFilename() {
        // Test with date range
        String filename1 = reportExportService.getExportFilename("visit-statistics", "pdf", 
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertEquals("visit-statistics_2024-01-01_to_2024-01-31.pdf", filename1);
        
        // Test with single date
        String filename2 = reportExportService.getExportFilename("revenue", "csv", 
                LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15));
        assertEquals("revenue_2024-01-15.csv", filename2);
        
        // Test with no dates
        String filename3 = reportExportService.getExportFilename("dashboard", "pdf", null, null);
        assertTrue(filename3.startsWith("dashboard_"));
        assertTrue(filename3.endsWith(".pdf"));
        
        // Test with start date only
        String filename4 = reportExportService.getExportFilename("visit-statistics", "csv", 
                LocalDate.of(2024, 1, 1), null);
        assertEquals("visit-statistics_from_2024-01-01.csv", filename4);
        
        // Test with end date only
        String filename5 = reportExportService.getExportFilename("revenue", "pdf", 
                null, LocalDate.of(2024, 1, 31));
        assertEquals("revenue_until_2024-01-31.pdf", filename5);
    }
    
    @Test
    void testGetMimeType() {
        assertEquals("application/pdf", reportExportService.getMimeType("pdf"));
        assertEquals("application/pdf", reportExportService.getMimeType("PDF"));
        assertEquals("text/csv", reportExportService.getMimeType("csv"));
        assertEquals("text/csv", reportExportService.getMimeType("CSV"));
        assertEquals("application/octet-stream", reportExportService.getMimeType("unknown"));
    }
    
    @Test
    void testValidateExportParameters() {
        // Valid parameters
        assertTrue(reportExportService.validateExportParameters("visit-statistics", "pdf"));
        assertTrue(reportExportService.validateExportParameters("revenue", "csv"));
        assertTrue(reportExportService.validateExportParameters("dashboard", "PDF"));
        assertTrue(reportExportService.validateExportParameters("visit-statistics-by-veterinarian", "CSV"));
        
        // Invalid report types
        assertFalse(reportExportService.validateExportParameters("invalid-report", "pdf"));
        assertFalse(reportExportService.validateExportParameters("", "pdf"));
        assertFalse(reportExportService.validateExportParameters(null, "pdf"));
        
        // Invalid formats
        assertFalse(reportExportService.validateExportParameters("visit-statistics", "invalid"));
        assertFalse(reportExportService.validateExportParameters("visit-statistics", ""));
        assertFalse(reportExportService.validateExportParameters("visit-statistics", null));
    }
    
    @Test
    void testExportWithEmptyData() {
        // Given - empty report
        VisitStatisticsReport emptyReport = new VisitStatisticsReport(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        emptyReport.setTotalVisits(0);
        emptyReport.setCompletedVisits(0);
        emptyReport.setScheduledVisits(0);
        emptyReport.setCancelledVisits(0);
        
        // When
        byte[] pdfData = reportExportService.exportVisitStatisticsToPdf(emptyReport);
        byte[] csvData = reportExportService.exportVisitStatisticsToCSV(emptyReport);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Total Visits") && csvContent.contains("0"));
    }
    
    @Test
    void testExportWithNullValues() {
        // Given - report with null values
        VisitStatisticsReport reportWithNulls = new VisitStatisticsReport(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        reportWithNulls.setTotalVisits(10);
        reportWithNulls.setCompletedVisits(5);
        reportWithNulls.setScheduledVisits(3);
        reportWithNulls.setCancelledVisits(2);
        // Leave other fields null
        
        // When
        byte[] pdfData = reportExportService.exportVisitStatisticsToPdf(reportWithNulls);
        byte[] csvData = reportExportService.exportVisitStatisticsToCSV(reportWithNulls);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
    }
    
    @Test
    void testExportRevenueReportWithNullRevenue() {
        // Given - revenue report with null revenue
        RevenueReport reportWithNullRevenue = new RevenueReport(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        reportWithNullRevenue.setTotalRevenue(null);
        reportWithNullRevenue.setTotalPaidVisits(0);
        reportWithNullRevenue.setTotalUnpaidVisits(10);
        
        // When
        byte[] pdfData = reportExportService.exportRevenueReportToPdf(reportWithNullRevenue);
        byte[] csvData = reportExportService.exportRevenueReportToCSV(reportWithNullRevenue);
        
        // Then
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);
        
        String csvContent = new String(csvData);
        assertTrue(csvContent.contains("Total Revenue") && csvContent.contains("$0.00"));
    }
}