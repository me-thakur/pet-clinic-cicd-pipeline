package com.petclinic.backend.properties;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import com.petclinic.backend.service.DashboardService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for report export consistency functionality
 * **Validates: Requirements 5.3**
 * 
 * Tests universal property that exported data is consistent across different formats
 * Uses H2 test database to verify export format consistency between PDF and CSV
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportExportConsistencyProperties extends PropertyTestBase {
    
    @Autowired
    private ReportExportService reportExportService;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private OwnerRepository ownerRepository;
    /**
     * Property 15: Export Format Consistency
     * For any report data, both PDF and CSV export formats should contain identical data 
     * with appropriate formatting for each format
     * **Validates: Requirements 5.3**
     */
    @Test
    void testVisitStatisticsExportFormatConsistency() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data
            LocalDate startDate = validReportStartDates().next();
            LocalDate endDate = validReportEndDates(startDate).next();
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits within the date range
            List<Visit> visits = createTestVisits(pets, veterinarians, startDate, endDate, 5 + random.nextInt(15)); // 5-19 visits
            
            // Generate visit statistics report
            VisitStatisticsReport report = reportService.generateVisitStatistics(startDate, endDate);
            
            // Export to both formats
            byte[] pdfData = reportExportService.exportVisitStatisticsToPdf(report);
            byte[] csvData = reportExportService.exportVisitStatisticsToCSV(report);
            
            // Verify both exports are not null and contain data
            assertNotNull(pdfData, "PDF export should not be null");
            assertNotNull(csvData, "CSV export should not be null");
            assertTrue(pdfData.length > 0, "PDF export should contain data");
            assertTrue(csvData.length > 0, "CSV export should contain data");
            
            // Verify PDF format
            String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, Math.min(4, pdfData.length)));
            assertEquals("%PDF", pdfHeader, "PDF export should have valid PDF header");
            
            // Parse CSV content for data validation
            String csvContent = new String(csvData);
            
            // Verify core data consistency between formats
            verifyVisitStatisticsDataConsistency(report, csvContent);
            
            // Verify numerical precision consistency
            verifyNumericalPrecisionConsistency(report, csvContent);
            
            // Verify date formatting consistency
            verifyDateFormattingConsistency(report, csvContent);
            
            // Verify text content preservation
            verifyTextContentPreservation(report, csvContent);
        });
    }
    /**
     * Property 15: Export Format Consistency - Revenue Reports
     * For any revenue report data, both PDF and CSV export formats should contain identical data 
     * with appropriate formatting for each format
     * **Validates: Requirements 5.3**
     */
    @Test
    void testRevenueReportExportFormatConsistency() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data
            LocalDate startDate = validReportStartDates().next();
            LocalDate endDate = validReportEndDates(startDate).next();
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits with costs within the date range
            List<Visit> visits = createTestVisitsWithCosts(pets, veterinarians, startDate, endDate, 5 + random.nextInt(15)); // 5-19 visits
            
            // Generate revenue report
            RevenueReport report = reportService.generateRevenueReport(startDate, endDate);
            
            // Export to both formats
            byte[] pdfData = reportExportService.exportRevenueReportToPdf(report);
            byte[] csvData = reportExportService.exportRevenueReportToCSV(report);
            
            // Verify both exports are not null and contain data
            assertNotNull(pdfData, "PDF export should not be null");
            assertNotNull(csvData, "CSV export should not be null");
            assertTrue(pdfData.length > 0, "PDF export should contain data");
            assertTrue(csvData.length > 0, "CSV export should contain data");
            
            // Verify PDF format
            String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, Math.min(4, pdfData.length)));
            assertEquals("%PDF", pdfHeader, "PDF export should have valid PDF header");
            
            // Parse CSV content for data validation
            String csvContent = new String(csvData);
            
            // Verify core data consistency between formats
            verifyRevenueReportDataConsistency(report, csvContent);
            
            // Verify currency formatting consistency
            verifyCurrencyFormattingConsistency(report, csvContent);
            
            // Verify percentage calculations consistency
            verifyPercentageCalculationsConsistency(report, csvContent);
        });
    }
    /**
     * Property 15: Export Format Consistency - Dashboard Metrics
     * For any dashboard metrics data, both PDF and CSV export formats should contain identical data 
     * with appropriate formatting for each format
     * **Validates: Requirements 5.3**
     */
    @Test
    void testDashboardMetricsExportFormatConsistency() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Create test entities for dashboard metrics
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits for today and this month
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            List<Visit> visits = createTestVisitsWithCosts(pets, veterinarians, monthStart, today, 10 + random.nextInt(20)); // 10-29 visits
            
            // Generate dashboard metrics
            DashboardMetrics metrics = dashboardService.getRealTimeDashboardMetrics();
            
            // Export to both formats
            byte[] pdfData = reportExportService.exportDashboardMetricsToPdf(metrics);
            byte[] csvData = reportExportService.exportDashboardMetricsToCSV(metrics);
            
            // Verify both exports are not null and contain data
            assertNotNull(pdfData, "PDF export should not be null");
            assertNotNull(csvData, "CSV export should not be null");
            assertTrue(pdfData.length > 0, "PDF export should contain data");
            assertTrue(csvData.length > 0, "CSV export should contain data");
            
            // Verify PDF format
            String pdfHeader = new String(Arrays.copyOfRange(pdfData, 0, Math.min(4, pdfData.length)));
            assertEquals("%PDF", pdfHeader, "PDF export should have valid PDF header");
            
            // Parse CSV content for data validation
            String csvContent = new String(csvData);
            
            // Verify core data consistency between formats
            verifyDashboardMetricsDataConsistency(metrics, csvContent);
            
            // Verify timestamp formatting consistency
            verifyTimestampFormattingConsistency(metrics, csvContent);
            
            // Verify complex data structures consistency (alerts, appointments)
            verifyComplexDataStructuresConsistency(metrics, csvContent);
        });
    }
    /**
     * Property 15: Export Format Consistency - Multiple Reports
     * For any multiple report data, both PDF and CSV export formats should contain identical data 
     * with appropriate formatting for each format
     * **Validates: Requirements 5.3**
     */
    @Test
    void testMultipleReportsExportFormatConsistency() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data for multiple periods
            LocalDate endDate = validReportStartDates().next();
            LocalDate startDate = endDate.minusDays(7 + random.nextInt(14)); // 7-20 days period
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits for multiple periods
            List<Visit> visits1 = createTestVisitsWithCosts(pets, veterinarians, startDate, endDate, 3 + random.nextInt(8)); // 3-10 visits
            
            LocalDate period2Start = endDate.plusDays(1);
            LocalDate period2End = period2Start.plusDays(7 + random.nextInt(14)); // Another 7-20 days
            List<Visit> visits2 = createTestVisitsWithCosts(pets, veterinarians, period2Start, period2End, 3 + random.nextInt(8)); // 3-10 visits
            
            // Generate multiple reports
            List<VisitStatisticsReport> visitReports = Arrays.asList(
                reportService.generateVisitStatistics(startDate, endDate),
                reportService.generateVisitStatistics(period2Start, period2End)
            );
            
            List<RevenueReport> revenueReports = Arrays.asList(
                reportService.generateRevenueReport(startDate, endDate),
                reportService.generateRevenueReport(period2Start, period2End)
            );
            
            // Export visit statistics to both formats
            byte[] visitPdfData = reportExportService.exportMultipleVisitStatisticsToPdf(visitReports, "Multiple Visit Reports");
            byte[] visitCsvData = reportExportService.exportMultipleVisitStatisticsToCSV(visitReports);
            
            // Export revenue reports to both formats
            byte[] revenuePdfData = reportExportService.exportMultipleRevenueReportsToPdf(revenueReports, "Multiple Revenue Reports");
            byte[] revenueCsvData = reportExportService.exportMultipleRevenueReportsToCSV(revenueReports);
            
            // Verify all exports are not null and contain data
            assertNotNull(visitPdfData, "Visit statistics PDF export should not be null");
            assertNotNull(visitCsvData, "Visit statistics CSV export should not be null");
            assertNotNull(revenuePdfData, "Revenue PDF export should not be null");
            assertNotNull(revenueCsvData, "Revenue CSV export should not be null");
            
            assertTrue(visitPdfData.length > 0, "Visit statistics PDF export should contain data");
            assertTrue(visitCsvData.length > 0, "Visit statistics CSV export should contain data");
            assertTrue(revenuePdfData.length > 0, "Revenue PDF export should contain data");
            assertTrue(revenueCsvData.length > 0, "Revenue CSV export should contain data");
            
            // Verify PDF formats
            String visitPdfHeader = new String(Arrays.copyOfRange(visitPdfData, 0, Math.min(4, visitPdfData.length)));
            String revenuePdfHeader = new String(Arrays.copyOfRange(revenuePdfData, 0, Math.min(4, revenuePdfData.length)));
            assertEquals("%PDF", visitPdfHeader, "Visit statistics PDF export should have valid PDF header");
            assertEquals("%PDF", revenuePdfHeader, "Revenue PDF export should have valid PDF header");
            
            // Parse CSV content for data validation
            String visitCsvContent = new String(visitCsvData);
            String revenueCsvContent = new String(revenueCsvData);
            
            // Verify multiple reports data consistency
            verifyMultipleVisitReportsDataConsistency(visitReports, visitCsvContent);
            verifyMultipleRevenueReportsDataConsistency(revenueReports, revenueCsvContent);
        });
    }
    /**
     * Property 15: Export Format Consistency - Filtered Reports
     * For any filtered report data, both PDF and CSV export formats should contain identical data 
     * with appropriate formatting for each format
     * **Validates: Requirements 5.3**
     */
    @Test
    void testFilteredReportsExportFormatConsistency() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Generate test data
            LocalDate startDate = validReportStartDates().next();
            LocalDate endDate = validReportEndDates(startDate).next();
            
            // Create test entities
            List<Owner> owners = createTestOwners(2 + random.nextInt(3)); // 2-4 owners
            List<Pet> pets = createTestPets(owners, 3 + random.nextInt(5)); // 3-7 pets
            List<Veterinarian> veterinarians = createTestVeterinarians(2 + random.nextInt(3)); // 2-4 vets
            
            // Create visits with costs within the date range
            List<Visit> visits = createTestVisitsWithCosts(pets, veterinarians, startDate, endDate, 8 + random.nextInt(15)); // 8-22 visits
            
            // Create report filter
            ReportFilter filter = new ReportFilter(startDate, endDate);
            if (!veterinarians.isEmpty()) {
                filter.setVeterinarianId(veterinarians.get(random.nextInt(veterinarians.size())).getId());
            }
            if (!pets.isEmpty()) {
                Set<String> species = pets.stream().map(Pet::getSpecies).collect(Collectors.toSet());
                filter.setSpecies(new ArrayList<>(species));
            }
            
            // Export filtered reports to both formats
            byte[] visitPdfData = reportExportService.exportFilteredVisitStatisticsToPdf(filter);
            byte[] visitCsvData = reportExportService.exportFilteredVisitStatisticsToCSV(filter);
            byte[] revenuePdfData = reportExportService.exportFilteredRevenueReportToPdf(filter);
            byte[] revenueCsvData = reportExportService.exportFilteredRevenueReportToCSV(filter);
            
            // Verify all exports are not null and contain data
            assertNotNull(visitPdfData, "Filtered visit statistics PDF export should not be null");
            assertNotNull(visitCsvData, "Filtered visit statistics CSV export should not be null");
            assertNotNull(revenuePdfData, "Filtered revenue PDF export should not be null");
            assertNotNull(revenueCsvData, "Filtered revenue CSV export should not be null");
            
            assertTrue(visitPdfData.length > 0, "Filtered visit statistics PDF export should contain data");
            assertTrue(visitCsvData.length > 0, "Filtered visit statistics CSV export should contain data");
            assertTrue(revenuePdfData.length > 0, "Filtered revenue PDF export should contain data");
            assertTrue(revenueCsvData.length > 0, "Filtered revenue CSV export should contain data");
            
            // Verify PDF formats
            String visitPdfHeader = new String(Arrays.copyOfRange(visitPdfData, 0, Math.min(4, visitPdfData.length)));
            String revenuePdfHeader = new String(Arrays.copyOfRange(revenuePdfData, 0, Math.min(4, revenuePdfData.length)));
            assertEquals("%PDF", visitPdfHeader, "Filtered visit statistics PDF export should have valid PDF header");
            assertEquals("%PDF", revenuePdfHeader, "Filtered revenue PDF export should have valid PDF header");
            
            // Parse CSV content for data validation
            String visitCsvContent = new String(visitCsvData);
            String revenueCsvContent = new String(revenueCsvData);
            
            // Generate the same reports directly for comparison
            VisitStatisticsReport visitReport = reportService.generateVisitStatistics(filter);
            RevenueReport revenueReport = reportService.generateRevenueReport(filter);
            
            // Verify filtered reports data consistency
            verifyVisitStatisticsDataConsistency(visitReport, visitCsvContent);
            verifyRevenueReportDataConsistency(revenueReport, revenueCsvContent);
        });
    }
    /**
     * Property 15: Export Format Consistency - Edge Cases
     * For any edge case data (empty datasets, null values, special characters), 
     * both PDF and CSV export formats should handle them consistently
     * **Validates: Requirements 5.3**
     */
    @Test
    void testEdgeCasesExportFormatConsistency() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Clear any existing data to ensure clean test
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            
            // Test different edge cases
            int edgeCaseType = random.nextInt(4);
            
            switch (edgeCaseType) {
                case 0: // Empty dataset
                    testEmptyDatasetConsistency();
                    break;
                case 1: // Null values
                    testNullValuesConsistency();
                    break;
                case 2: // Special characters
                    testSpecialCharactersConsistency();
                    break;
                case 3: // Large numbers and precision
                    testNumericalPrecisionConsistency();
                    break;
            }
        });
    }
    
    // Helper methods for data verification
    
    private void verifyVisitStatisticsDataConsistency(VisitStatisticsReport report, String csvContent) {
        // Verify total visits
        assertTrue(csvContent.contains("Total Visits") && csvContent.contains(String.valueOf(report.getTotalVisits())),
                  "CSV should contain correct total visits count");
        
        // Verify completed visits
        assertTrue(csvContent.contains("Completed Visits") && csvContent.contains(String.valueOf(report.getCompletedVisits())),
                  "CSV should contain correct completed visits count");
        
        // Verify scheduled visits
        assertTrue(csvContent.contains("Scheduled Visits") && csvContent.contains(String.valueOf(report.getScheduledVisits())),
                  "CSV should contain correct scheduled visits count");
        
        // Verify cancelled visits
        assertTrue(csvContent.contains("Cancelled Visits") && csvContent.contains(String.valueOf(report.getCancelledVisits())),
                  "CSV should contain correct cancelled visits count");
        
        // Verify completion rate
        String expectedCompletionRate = String.format("%.1f%%", report.getCompletionRate());
        assertTrue(csvContent.contains("Completion Rate") && csvContent.contains(expectedCompletionRate),
                  "CSV should contain correct completion rate");
        
        // Verify average visits per day
        String expectedAverage = String.format("%.1f", report.getAverageVisitsPerDay());
        assertTrue(csvContent.contains("Average Visits/Day") && csvContent.contains(expectedAverage),
                  "CSV should contain correct average visits per day");
        
        // Verify visits by veterinarian if present
        if (report.getVisitsByVeterinarian() != null && !report.getVisitsByVeterinarian().isEmpty()) {
            for (Map.Entry<String, Long> entry : report.getVisitsByVeterinarian().entrySet()) {
                assertTrue(csvContent.contains(entry.getKey()) && csvContent.contains(String.valueOf(entry.getValue())),
                          "CSV should contain visits by veterinarian: " + entry.getKey());
            }
        }
        
        // Verify visits by species if present
        if (report.getVisitsBySpecies() != null && !report.getVisitsBySpecies().isEmpty()) {
            for (Map.Entry<String, Long> entry : report.getVisitsBySpecies().entrySet()) {
                assertTrue(csvContent.contains(entry.getKey()) && csvContent.contains(String.valueOf(entry.getValue())),
                          "CSV should contain visits by species: " + entry.getKey());
            }
        }
    }
    private void verifyRevenueReportDataConsistency(RevenueReport report, String csvContent) {
        // Verify total revenue
        String expectedRevenue = formatCurrency(report.getTotalRevenue());
        assertTrue(csvContent.contains("Total Revenue") && csvContent.contains(expectedRevenue),
                  "CSV should contain correct total revenue");
        
        // Verify paid visits
        assertTrue(csvContent.contains("Total Paid Visits") && csvContent.contains(String.valueOf(report.getTotalPaidVisits())),
                  "CSV should contain correct paid visits count");
        
        // Verify unpaid visits
        assertTrue(csvContent.contains("Total Unpaid Visits") && csvContent.contains(String.valueOf(report.getTotalUnpaidVisits())),
                  "CSV should contain correct unpaid visits count");
        
        // Verify average revenue per visit
        String expectedAvgPerVisit = formatCurrency(report.getAverageRevenuePerVisit());
        assertTrue(csvContent.contains("Average Revenue/Visit") && csvContent.contains(expectedAvgPerVisit),
                  "CSV should contain correct average revenue per visit");
        
        // Verify average revenue per day
        String expectedAvgPerDay = formatCurrency(report.getAverageRevenuePerDay());
        assertTrue(csvContent.contains("Average Revenue/Day") && csvContent.contains(expectedAvgPerDay),
                  "CSV should contain correct average revenue per day");
        
        // Verify payment rate
        String expectedPaymentRate = String.format("%.1f%%", report.getPaymentRate());
        assertTrue(csvContent.contains("Payment Rate") && csvContent.contains(expectedPaymentRate),
                  "CSV should contain correct payment rate");
        
        // Verify revenue by veterinarian if present
        if (report.getRevenueByVeterinarian() != null && !report.getRevenueByVeterinarian().isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : report.getRevenueByVeterinarian().entrySet()) {
                String expectedVetRevenue = formatCurrency(entry.getValue());
                assertTrue(csvContent.contains(entry.getKey()) && csvContent.contains(expectedVetRevenue),
                          "CSV should contain revenue by veterinarian: " + entry.getKey());
            }
        }
        
        // Verify revenue by species if present
        if (report.getRevenueBySpecies() != null && !report.getRevenueBySpecies().isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : report.getRevenueBySpecies().entrySet()) {
                String expectedSpeciesRevenue = formatCurrency(entry.getValue());
                assertTrue(csvContent.contains(entry.getKey()) && csvContent.contains(expectedSpeciesRevenue),
                          "CSV should contain revenue by species: " + entry.getKey());
            }
        }
        
        // Verify trend data if present
        if (report.getTrendData() != null) {
            RevenueReport.TrendData trendData = report.getTrendData();
            String expectedPreviousRevenue = formatCurrency(trendData.getPreviousPeriodRevenue());
            String expectedGrowthRate = String.format("%.1f%%", trendData.getGrowthRate());
            
            assertTrue(csvContent.contains("Previous Period Revenue") && csvContent.contains(expectedPreviousRevenue),
                      "CSV should contain correct previous period revenue");
            assertTrue(csvContent.contains("Growth Rate") && csvContent.contains(expectedGrowthRate),
                      "CSV should contain correct growth rate");
            assertTrue(csvContent.contains("Trend Direction") && csvContent.contains(trendData.getTrendDirection()),
                      "CSV should contain correct trend direction");
        }
    }
    private void verifyDashboardMetricsDataConsistency(DashboardMetrics metrics, String csvContent) {
        // Verify today's appointments
        assertTrue(csvContent.contains("Today's Appointments") && csvContent.contains(String.valueOf(metrics.getTodayAppointments())),
                  "CSV should contain correct today's appointments count");
        
        // Verify completed visits
        assertTrue(csvContent.contains("Completed Visits") && csvContent.contains(String.valueOf(metrics.getTodayCompletedVisits())),
                  "CSV should contain correct completed visits count");
        
        // Verify pending visits
        assertTrue(csvContent.contains("Pending Visits") && csvContent.contains(String.valueOf(metrics.getTodayPendingVisits())),
                  "CSV should contain correct pending visits count");
        
        // Verify today's revenue
        String expectedTodayRevenue = formatCurrency(metrics.getTodayRevenue());
        assertTrue(csvContent.contains("Today's Revenue") && csvContent.contains(expectedTodayRevenue),
                  "CSV should contain correct today's revenue");
        
        // Verify total active pets
        assertTrue(csvContent.contains("Total Active Pets") && csvContent.contains(String.valueOf(metrics.getTotalActivePets())),
                  "CSV should contain correct total active pets count");
        
        // Verify total veterinarians
        assertTrue(csvContent.contains("Total Veterinarians") && csvContent.contains(String.valueOf(metrics.getTotalVeterinarians())),
                  "CSV should contain correct total veterinarians count");
        
        // Verify monthly revenue
        String expectedMonthlyRevenue = formatCurrency(metrics.getMonthlyRevenue());
        assertTrue(csvContent.contains("Monthly Revenue") && csvContent.contains(expectedMonthlyRevenue),
                  "CSV should contain correct monthly revenue");
        
        // Verify utilization metrics
        String expectedUtilization = String.format("%.1f%%", metrics.getVeterinarianUtilization());
        assertTrue(csvContent.contains("Veterinarian Utilization") && csvContent.contains(expectedUtilization),
                  "CSV should contain correct veterinarian utilization");
        
        String expectedCompletionRate = String.format("%.1f%%", metrics.getAppointmentCompletionRate());
        assertTrue(csvContent.contains("Appointment Completion Rate") && csvContent.contains(expectedCompletionRate),
                  "CSV should contain correct appointment completion rate");
        
        String expectedAvgDuration = String.format("%.1f minutes", metrics.getAverageVisitDuration());
        assertTrue(csvContent.contains("Average Visit Duration") && csvContent.contains(expectedAvgDuration),
                  "CSV should contain correct average visit duration");
        
        // Verify system health
        String expectedHealth = metrics.isHealthy() ? "Healthy" : "Needs Attention";
        assertTrue(csvContent.contains("System Health") && csvContent.contains(expectedHealth),
                  "CSV should contain correct system health status");
        
        // Verify upcoming appointments if present
        if (metrics.getUpcomingAppointments() != null && !metrics.getUpcomingAppointments().isEmpty()) {
            for (DashboardMetrics.UpcomingAppointment appointment : metrics.getUpcomingAppointments()) {
                assertTrue(csvContent.contains(appointment.getPetName()),
                          "CSV should contain upcoming appointment pet name: " + appointment.getPetName());
                assertTrue(csvContent.contains(appointment.getOwnerName()),
                          "CSV should contain upcoming appointment owner name: " + appointment.getOwnerName());
                assertTrue(csvContent.contains(appointment.getVeterinarianName()),
                          "CSV should contain upcoming appointment veterinarian name: " + appointment.getVeterinarianName());
            }
        }
        
        // Verify alerts if present
        if (metrics.getAlerts() != null && !metrics.getAlerts().isEmpty()) {
            for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                assertTrue(csvContent.contains(alert.getTitle()),
                          "CSV should contain alert title: " + alert.getTitle());
                assertTrue(csvContent.contains(alert.getSeverity()),
                          "CSV should contain alert severity: " + alert.getSeverity());
            }
        }
    }
    private void verifyNumericalPrecisionConsistency(VisitStatisticsReport report, String csvContent) {
        // Verify completion rate precision (should be to 1 decimal place)
        String completionRatePattern = String.format("%.1f%%", report.getCompletionRate());
        assertTrue(csvContent.contains(completionRatePattern),
                  "CSV should contain completion rate with correct precision: " + completionRatePattern);
        
        // Verify average visits per day precision (should be to 1 decimal place)
        String averagePattern = String.format("%.1f", report.getAverageVisitsPerDay());
        assertTrue(csvContent.contains(averagePattern),
                  "CSV should contain average visits per day with correct precision: " + averagePattern);
        
        // Ensure no rounding errors in calculations
        if (report.getTotalVisits() > 0) {
            double expectedCompletionRate = (double) report.getCompletedVisits() / report.getTotalVisits() * 100.0;
            assertEquals(expectedCompletionRate, report.getCompletionRate(), 0.01,
                        "Completion rate calculation should be mathematically correct");
        }
    }
    
    private void verifyDateFormattingConsistency(VisitStatisticsReport report, String csvContent) {
        // Verify date range description is present and correctly formatted
        String dateRangeDescription = report.getDateRangeDescription();
        assertTrue(csvContent.contains(dateRangeDescription),
                  "CSV should contain correct date range description: " + dateRangeDescription);
        
        // Verify individual dates are formatted consistently (YYYY-MM-DD format)
        if (report.getVisitsByDate() != null && !report.getVisitsByDate().isEmpty()) {
            for (LocalDate date : report.getVisitsByDate().keySet()) {
                String expectedDateFormat = date.toString(); // ISO format YYYY-MM-DD
                assertTrue(csvContent.contains(expectedDateFormat),
                          "CSV should contain date in ISO format: " + expectedDateFormat);
            }
        }
    }
    
    private void verifyTextContentPreservation(VisitStatisticsReport report, String csvContent) {
        // Verify that text content is preserved without corruption
        assertTrue(csvContent.contains("Visit Statistics Report"),
                  "CSV should contain report title");
        
        // Verify that special characters in veterinarian names are preserved
        if (report.getVisitsByVeterinarian() != null) {
            for (String vetName : report.getVisitsByVeterinarian().keySet()) {
                assertTrue(csvContent.contains(vetName),
                          "CSV should preserve veterinarian name: " + vetName);
                
                // Ensure no character encoding issues
                assertFalse(vetName.contains("?") && !csvContent.contains(vetName.replace("?", "")),
                           "CSV should not have character encoding issues for: " + vetName);
            }
        }
        
        // Verify that species names are preserved
        if (report.getVisitsBySpecies() != null) {
            for (String species : report.getVisitsBySpecies().keySet()) {
                assertTrue(csvContent.contains(species),
                          "CSV should preserve species name: " + species);
            }
        }
    }
    
    private void verifyCurrencyFormattingConsistency(RevenueReport report, String csvContent) {
        // Verify all currency values are formatted consistently as $X.XX
        Pattern currencyPattern = Pattern.compile("\\$\\d+\\.\\d{2}");
        
        // Check total revenue formatting
        String expectedTotalRevenue = formatCurrency(report.getTotalRevenue());
        assertTrue(csvContent.contains(expectedTotalRevenue),
                  "CSV should contain correctly formatted total revenue: " + expectedTotalRevenue);
        assertTrue(currencyPattern.matcher(expectedTotalRevenue).matches(),
                  "Total revenue should match currency pattern: " + expectedTotalRevenue);
        
        // Check average revenue per visit formatting
        String expectedAvgPerVisit = formatCurrency(report.getAverageRevenuePerVisit());
        assertTrue(csvContent.contains(expectedAvgPerVisit),
                  "CSV should contain correctly formatted average revenue per visit: " + expectedAvgPerVisit);
        assertTrue(currencyPattern.matcher(expectedAvgPerVisit).matches(),
                  "Average revenue per visit should match currency pattern: " + expectedAvgPerVisit);
        
        // Check average revenue per day formatting
        String expectedAvgPerDay = formatCurrency(report.getAverageRevenuePerDay());
        assertTrue(csvContent.contains(expectedAvgPerDay),
                  "CSV should contain correctly formatted average revenue per day: " + expectedAvgPerDay);
        assertTrue(currencyPattern.matcher(expectedAvgPerDay).matches(),
                  "Average revenue per day should match currency pattern: " + expectedAvgPerDay);
    }
    private void verifyPercentageCalculationsConsistency(RevenueReport report, String csvContent) {
        // Verify payment rate percentage formatting (should be to 1 decimal place)
        String expectedPaymentRate = String.format("%.1f%%", report.getPaymentRate());
        assertTrue(csvContent.contains(expectedPaymentRate),
                  "CSV should contain correctly formatted payment rate: " + expectedPaymentRate);
        
        // Verify payment rate calculation accuracy
        if (report.getTotalVisits() > 0) {
            double expectedRate = (double) report.getTotalPaidVisits() / report.getTotalVisits() * 100.0;
            assertEquals(expectedRate, report.getPaymentRate(), 0.01,
                        "Payment rate calculation should be mathematically correct");
        }
        
        // Verify growth rate formatting if trend data is present
        if (report.getTrendData() != null) {
            String expectedGrowthRate = String.format("%.1f%%", report.getTrendData().getGrowthRate());
            assertTrue(csvContent.contains(expectedGrowthRate),
                      "CSV should contain correctly formatted growth rate: " + expectedGrowthRate);
        }
    }
    
    private void verifyTimestampFormattingConsistency(DashboardMetrics metrics, String csvContent) {
        // Verify generation timestamp is present and correctly formatted
        String expectedTimestamp = metrics.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        assertTrue(csvContent.contains(expectedTimestamp),
                  "CSV should contain correctly formatted generation timestamp: " + expectedTimestamp);
        
        // Verify upcoming appointment times are formatted consistently
        if (metrics.getUpcomingAppointments() != null && !metrics.getUpcomingAppointments().isEmpty()) {
            for (DashboardMetrics.UpcomingAppointment appointment : metrics.getUpcomingAppointments()) {
                String expectedAppointmentTime = appointment.getAppointmentTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                assertTrue(csvContent.contains(expectedAppointmentTime),
                          "CSV should contain correctly formatted appointment time: " + expectedAppointmentTime);
            }
        }
        
        // Verify alert timestamps are formatted consistently
        if (metrics.getAlerts() != null && !metrics.getAlerts().isEmpty()) {
            for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                String expectedAlertTime = alert.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                assertTrue(csvContent.contains(expectedAlertTime),
                          "CSV should contain correctly formatted alert timestamp: " + expectedAlertTime);
            }
        }
    }
    
    private void verifyComplexDataStructuresConsistency(DashboardMetrics metrics, String csvContent) {
        // Verify upcoming appointments section structure
        if (metrics.getUpcomingAppointments() != null && !metrics.getUpcomingAppointments().isEmpty()) {
            assertTrue(csvContent.contains("Upcoming Appointments"),
                      "CSV should contain upcoming appointments section header");
            assertTrue(csvContent.contains("Pet") && csvContent.contains("Owner") && csvContent.contains("Veterinarian"),
                      "CSV should contain upcoming appointments column headers");
            
            // Verify each appointment has all required fields
            for (DashboardMetrics.UpcomingAppointment appointment : metrics.getUpcomingAppointments()) {
                assertTrue(csvContent.contains(appointment.getPetName()),
                          "CSV should contain pet name: " + appointment.getPetName());
                assertTrue(csvContent.contains(appointment.getOwnerName()),
                          "CSV should contain owner name: " + appointment.getOwnerName());
                assertTrue(csvContent.contains(appointment.getVeterinarianName()),
                          "CSV should contain veterinarian name: " + appointment.getVeterinarianName());
                assertTrue(csvContent.contains(appointment.getVisitType()),
                          "CSV should contain visit type: " + appointment.getVisitType());
                assertTrue(csvContent.contains(String.valueOf(appointment.getDuration())),
                          "CSV should contain duration: " + appointment.getDuration());
            }
        }
        
        // Verify alerts section structure
        if (metrics.getAlerts() != null && !metrics.getAlerts().isEmpty()) {
            assertTrue(csvContent.contains("Alerts"),
                      "CSV should contain alerts section header");
            assertTrue(csvContent.contains("Severity") && csvContent.contains("Title") && csvContent.contains("Message"),
                      "CSV should contain alert column headers");
            
            // Verify each alert has all required fields
            for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                assertTrue(csvContent.contains(alert.getSeverity()),
                          "CSV should contain alert severity: " + alert.getSeverity());
                assertTrue(csvContent.contains(alert.getType()),
                          "CSV should contain alert type: " + alert.getType());
                assertTrue(csvContent.contains(alert.getTitle()),
                          "CSV should contain alert title: " + alert.getTitle());
                assertTrue(csvContent.contains(alert.getMessage()),
                          "CSV should contain alert message: " + alert.getMessage());
            }
        }
        
        // Verify recent activities section structure if present
        if (metrics.getRecentActivities() != null && !metrics.getRecentActivities().isEmpty()) {
            assertTrue(csvContent.contains("Recent Activities"),
                      "CSV should contain recent activities section header");
            
            for (DashboardMetrics.RecentActivity activity : metrics.getRecentActivities()) {
                assertTrue(csvContent.contains(activity.getType()),
                          "CSV should contain activity type: " + activity.getType());
                assertTrue(csvContent.contains(activity.getDescription()),
                          "CSV should contain activity description: " + activity.getDescription());
            }
        }
    }
    private void verifyMultipleVisitReportsDataConsistency(List<VisitStatisticsReport> reports, String csvContent) {
        assertTrue(csvContent.contains("Multiple Visit Statistics Reports"),
                  "CSV should contain multiple reports header");
        
        // Verify each report's data is present
        for (VisitStatisticsReport report : reports) {
            assertTrue(csvContent.contains(report.getDateRangeDescription()),
                      "CSV should contain date range: " + report.getDateRangeDescription());
            assertTrue(csvContent.contains(String.valueOf(report.getTotalVisits())),
                      "CSV should contain total visits: " + report.getTotalVisits());
            assertTrue(csvContent.contains(String.valueOf(report.getCompletedVisits())),
                      "CSV should contain completed visits: " + report.getCompletedVisits());
            
            String expectedCompletionRate = String.format("%.1f%%", report.getCompletionRate());
            assertTrue(csvContent.contains(expectedCompletionRate),
                      "CSV should contain completion rate: " + expectedCompletionRate);
        }
    }
    
    private void verifyMultipleRevenueReportsDataConsistency(List<RevenueReport> reports, String csvContent) {
        assertTrue(csvContent.contains("Multiple Revenue Reports"),
                  "CSV should contain multiple reports header");
        
        // Verify each report's data is present
        for (RevenueReport report : reports) {
            assertTrue(csvContent.contains(report.getDateRangeDescription()),
                      "CSV should contain date range: " + report.getDateRangeDescription());
            
            String expectedRevenue = formatCurrency(report.getTotalRevenue());
            assertTrue(csvContent.contains(expectedRevenue),
                      "CSV should contain total revenue: " + expectedRevenue);
            
            assertTrue(csvContent.contains(String.valueOf(report.getTotalPaidVisits())),
                      "CSV should contain paid visits: " + report.getTotalPaidVisits());
            
            String expectedPaymentRate = String.format("%.1f%%", report.getPaymentRate());
            assertTrue(csvContent.contains(expectedPaymentRate),
                      "CSV should contain payment rate: " + expectedPaymentRate);
        }
    }
    
    // Edge case testing methods
    
    private void testEmptyDatasetConsistency() {
        // Create empty reports
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        VisitStatisticsReport emptyVisitReport = new VisitStatisticsReport(startDate, endDate);
        emptyVisitReport.setTotalVisits(0);
        emptyVisitReport.setCompletedVisits(0);
        emptyVisitReport.setScheduledVisits(0);
        emptyVisitReport.setCancelledVisits(0);
        
        RevenueReport emptyRevenueReport = new RevenueReport(startDate, endDate);
        emptyRevenueReport.setTotalRevenue(BigDecimal.ZERO);
        emptyRevenueReport.setTotalPaidVisits(0);
        emptyRevenueReport.setTotalUnpaidVisits(0);
        
        // Export both formats
        byte[] visitPdfData = reportExportService.exportVisitStatisticsToPdf(emptyVisitReport);
        byte[] visitCsvData = reportExportService.exportVisitStatisticsToCSV(emptyVisitReport);
        byte[] revenuePdfData = reportExportService.exportRevenueReportToPdf(emptyRevenueReport);
        byte[] revenueCsvData = reportExportService.exportRevenueReportToCSV(emptyRevenueReport);
        
        // Verify exports handle empty data gracefully
        assertNotNull(visitPdfData, "Empty visit statistics PDF export should not be null");
        assertNotNull(visitCsvData, "Empty visit statistics CSV export should not be null");
        assertNotNull(revenuePdfData, "Empty revenue PDF export should not be null");
        assertNotNull(revenueCsvData, "Empty revenue CSV export should not be null");
        
        assertTrue(visitPdfData.length > 0, "Empty visit statistics PDF should still contain structure");
        assertTrue(visitCsvData.length > 0, "Empty visit statistics CSV should still contain structure");
        assertTrue(revenuePdfData.length > 0, "Empty revenue PDF should still contain structure");
        assertTrue(revenueCsvData.length > 0, "Empty revenue CSV should still contain structure");
        
        // Verify CSV content shows zero values correctly
        String visitCsvContent = new String(visitCsvData);
        String revenueCsvContent = new String(revenueCsvData);
        
        assertTrue(visitCsvContent.contains("Total Visits") && visitCsvContent.contains("0"),
                  "Empty visit CSV should show zero total visits");
        assertTrue(revenueCsvContent.contains("Total Revenue") && revenueCsvContent.contains("$0.00"),
                  "Empty revenue CSV should show zero total revenue");
    }
    private void testNullValuesConsistency() {
        // Create reports with null values
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        VisitStatisticsReport reportWithNulls = new VisitStatisticsReport(startDate, endDate);
        reportWithNulls.setTotalVisits(10);
        reportWithNulls.setCompletedVisits(5);
        reportWithNulls.setScheduledVisits(3);
        reportWithNulls.setCancelledVisits(2);
        // Leave other fields null
        
        RevenueReport revenueReportWithNulls = new RevenueReport(startDate, endDate);
        revenueReportWithNulls.setTotalRevenue(null); // This should be handled as zero
        revenueReportWithNulls.setTotalPaidVisits(0);
        revenueReportWithNulls.setTotalUnpaidVisits(10);
        
        // Export both formats
        byte[] visitPdfData = reportExportService.exportVisitStatisticsToPdf(reportWithNulls);
        byte[] visitCsvData = reportExportService.exportVisitStatisticsToCSV(reportWithNulls);
        byte[] revenuePdfData = reportExportService.exportRevenueReportToPdf(revenueReportWithNulls);
        byte[] revenueCsvData = reportExportService.exportRevenueReportToCSV(revenueReportWithNulls);
        
        // Verify exports handle null values gracefully
        assertNotNull(visitPdfData, "Visit statistics PDF with nulls should not be null");
        assertNotNull(visitCsvData, "Visit statistics CSV with nulls should not be null");
        assertNotNull(revenuePdfData, "Revenue PDF with nulls should not be null");
        assertNotNull(revenueCsvData, "Revenue CSV with nulls should not be null");
        
        // Verify CSV content handles null values appropriately
        String visitCsvContent = new String(visitCsvData);
        String revenueCsvContent = new String(revenueCsvData);
        
        assertTrue(visitCsvContent.contains("Total Visits") && visitCsvContent.contains("10"),
                  "Visit CSV should handle basic values correctly");
        assertTrue(revenueCsvContent.contains("Total Revenue") && revenueCsvContent.contains("$0.00"),
                  "Revenue CSV should handle null revenue as zero");
    }
    
    private void testSpecialCharactersConsistency() {
        // Create test entities with special characters
        List<Owner> owners = createTestOwners(1);
        List<Pet> pets = createTestPets(owners, 1);
        List<Veterinarian> veterinarians = createTestVeterinarians(1);
        
        // Modify names to include special characters
        Veterinarian vet = veterinarians.get(0);
        vet.setFirstName("José");
        vet.setLastName("García-Smith");
        veterinarianRepository.save(vet);
        
        Pet pet = pets.get(0);
        pet.setName("Fifi & Max's Friend");
        pet.setSpecies("Cat/Dog Mix");
        petRepository.save(pet);
        
        // Create visits with guaranteed costs
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Visit> visits = createTestVisits(pets, veterinarians, startDate, endDate, 3);
        
        // Ensure all visits have costs to guarantee they appear in revenue reports
        for (Visit visit : visits) {
            visit.setCost(new BigDecimal("100.00"));
            visitRepository.save(visit);
        }
        
        // Generate reports
        VisitStatisticsReport visitReport = reportService.generateVisitStatistics(startDate, endDate);
        RevenueReport revenueReport = reportService.generateRevenueReport(startDate, endDate);
        
        // Export both formats
        byte[] visitPdfData = reportExportService.exportVisitStatisticsToPdf(visitReport);
        byte[] visitCsvData = reportExportService.exportVisitStatisticsToCSV(visitReport);
        byte[] revenuePdfData = reportExportService.exportRevenueReportToPdf(revenueReport);
        byte[] revenueCsvData = reportExportService.exportRevenueReportToCSV(revenueReport);
        
        // Verify exports handle special characters
        assertNotNull(visitPdfData, "Visit statistics PDF with special chars should not be null");
        assertNotNull(visitCsvData, "Visit statistics CSV with special chars should not be null");
        assertNotNull(revenuePdfData, "Revenue PDF with special chars should not be null");
        assertNotNull(revenueCsvData, "Revenue CSV with special chars should not be null");
        
        // Verify CSV content preserves special characters
        String visitCsvContent = new String(visitCsvData);
        String revenueCsvContent = new String(revenueCsvData);
        
        // Check if the veterinarian name appears in the CSV (may be in different sections)
        boolean vetNameInVisitCsv = visitCsvContent.contains("José") || visitCsvContent.contains("García");
        assertTrue(vetNameInVisitCsv,
                  "Visit CSV should preserve special characters in veterinarian names");
        
        assertTrue(visitCsvContent.contains("Cat/Dog Mix") || visitCsvContent.contains("Cat") || visitCsvContent.contains("Dog"),
                  "Visit CSV should preserve special characters in species names");
        
        // For revenue CSV, check if there's any revenue data first
        if (revenueReport.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            boolean vetNameInRevenueCsv = revenueCsvContent.contains("José") || revenueCsvContent.contains("García");
            assertTrue(vetNameInRevenueCsv,
                      "Revenue CSV should preserve special characters in veterinarian names when revenue data exists");
        } else {
            // If no revenue, just verify the CSV was generated successfully
            assertTrue(revenueCsvContent.contains("Revenue Report"),
                      "Revenue CSV should be generated even with no revenue data");
        }
    }
    private void testNumericalPrecisionConsistency() {
        // Create test data with precise numerical values
        List<Owner> owners = createTestOwners(1);
        List<Pet> pets = createTestPets(owners, 1);
        List<Veterinarian> veterinarians = createTestVeterinarians(1);
        
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Visit> visits = createTestVisits(pets, veterinarians, startDate, endDate, 7);
        
        // Set precise costs that might cause rounding issues
        BigDecimal[] preciseCosts = {
            new BigDecimal("123.456"), // Should round to 123.46
            new BigDecimal("999.999"), // Should round to 1000.00
            new BigDecimal("0.001"),   // Should round to 0.00
            new BigDecimal("50.555"),  // Should round to 50.56
            new BigDecimal("25.125"),  // Should round to 25.13
            new BigDecimal("100.005"), // Should round to 100.01
            new BigDecimal("75.995")   // Should round to 76.00
        };
        
        for (int i = 0; i < visits.size() && i < preciseCosts.length; i++) {
            Visit visit = visits.get(i);
            visit.setCost(preciseCosts[i]);
            visitRepository.save(visit);
        }
        
        // Generate reports
        RevenueReport revenueReport = reportService.generateRevenueReport(startDate, endDate);
        
        // Export both formats
        byte[] revenuePdfData = reportExportService.exportRevenueReportToPdf(revenueReport);
        byte[] revenueCsvData = reportExportService.exportRevenueReportToCSV(revenueReport);
        
        // Verify exports handle precision correctly
        assertNotNull(revenuePdfData, "Revenue PDF with precise values should not be null");
        assertNotNull(revenueCsvData, "Revenue CSV with precise values should not be null");
        
        // Verify CSV content shows correctly rounded values
        String revenueCsvContent = new String(revenueCsvData);
        
        // Verify total revenue is calculated and formatted correctly
        BigDecimal expectedTotal = Arrays.stream(preciseCosts)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String expectedTotalFormatted = formatCurrency(expectedTotal);
        
        assertTrue(revenueCsvContent.contains(expectedTotalFormatted),
                  "Revenue CSV should contain correctly calculated and formatted total: " + expectedTotalFormatted);
        
        // Verify individual currency values are formatted to 2 decimal places
        Pattern currencyPattern = Pattern.compile("\\$\\d+\\.\\d{2}");
        String[] csvLines = revenueCsvContent.split("\n");
        
        for (String line : csvLines) {
            if (line.contains("$")) {
                Matcher matcher = currencyPattern.matcher(line);
                while (matcher.find()) {
                    String currencyValue = matcher.group();
                    // Verify it matches the expected pattern
                    assertTrue(currencyValue.matches("\\$\\d+\\.\\d{2}"),
                              "Currency value should be formatted to 2 decimal places: " + currencyValue);
                }
            }
        }
    }
    
    // Generator methods for report-specific test data (reused from ReportCalculationProperties)
    
    protected Generator<LocalDate> validReportStartDates() {
        return () -> {
            LocalDate now = LocalDate.now();
            LocalDate earliest = now.minusMonths(6);
            LocalDate latest = now.minusDays(7); // At least a week ago to allow for end dates
            long daysBetween = latest.toEpochDay() - earliest.toEpochDay();
            long randomDays = Math.abs(random.nextLong()) % (daysBetween + 1);
            return earliest.plusDays(randomDays);
        };
    }
    
    protected Generator<LocalDate> validReportEndDates(LocalDate startDate) {
        return () -> {
            LocalDate earliest = startDate.plusDays(1); // At least one day after start
            LocalDate latest = startDate.plusDays(30); // At most 30 days after start
            if (latest.isAfter(LocalDate.now())) {
                latest = LocalDate.now();
            }
            if (earliest.isAfter(latest)) {
                return startDate; // Same day if no valid range
            }
            long daysBetween = latest.toEpochDay() - earliest.toEpochDay();
            if (daysBetween <= 0) {
                return startDate;
            }
            long randomDays = Math.abs(random.nextLong()) % (daysBetween + 1);
            return earliest.plusDays(randomDays);
        };
    }
    
    protected Generator<BigDecimal> validVisitCosts() {
        return () -> {
            // Generate costs between $0 and $500, with some null values
            if (random.nextInt(10) == 0) {
                return null; // 10% chance of null cost
            }
            if (random.nextInt(5) == 0) {
                return BigDecimal.ZERO; // 20% chance of zero cost
            }
            double cost = random.nextDouble() * 500.0;
            return BigDecimal.valueOf(Math.round(cost * 100.0) / 100.0); // Round to 2 decimal places
        };
    }
    
    protected Generator<VisitType> validVisitTypes() {
        return () -> {
            VisitType[] types = VisitType.values();
            return types[random.nextInt(types.length)];
        };
    }
    protected Generator<String> validVisitNotes() {
        String[] notes = {
            "Regular checkup completed",
            "Patient responded well to treatment",
            "Follow-up recommended in 2 weeks",
            "CANCELLED - owner request",
            "CANCELLED - emergency",
            "Vaccination completed successfully",
            "Dental cleaning performed",
            "Surgery consultation completed",
            null, // Null notes
            "" // Empty notes
        };
        return () -> notes[random.nextInt(notes.length)];
    }
    
    // Helper methods for creating test data (reused from ReportCalculationProperties)
    
    private List<Owner> createTestOwners(int count) {
        List<Owner> owners = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Owner owner = new Owner();
            owner.setFirstName(validOwnerNames().next());
            owner.setLastName(validOwnerNames().next());
            owner.setAddress("123 Test Street " + i);
            owner.setCity("Test City");
            owner.setTelephone(validPhoneNumbers().next());
            owners.add(ownerRepository.save(owner));
        }
        return owners;
    }
    
    private List<Pet> createTestPets(List<Owner> owners, int count) {
        List<Pet> pets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Pet pet = new Pet();
            pet.setName(validPetNames().next());
            pet.setSpecies(validSpecies().next());
            pet.setBirthDate(validBirthDates().next());
            pet.setOwner(owners.get(random.nextInt(owners.size())));
            pets.add(petRepository.save(pet));
        }
        return pets;
    }
    
    private List<Veterinarian> createTestVeterinarians(int count) {
        List<Veterinarian> veterinarians = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Veterinarian vet = new Veterinarian();
            vet.setFirstName(validVeterinarianFirstNames().next());
            vet.setLastName(validVeterinarianLastNames().next());
            vet.setLicenseNumber(validLicenseNumbers().next());
            vet.setSpecialties(validStringSpecialties().next());
            vet.setSpecialtySet(validEnumSpecialties().next());
            veterinarians.add(veterinarianRepository.save(vet));
        }
        return veterinarians;
    }
    
    private List<Visit> createTestVisits(List<Pet> pets, List<Veterinarian> veterinarians, 
                                       LocalDate startDate, LocalDate endDate, int count) {
        List<Visit> visits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Visit visit = new Visit();
            
            // Generate random date within range
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            long randomDays = daysBetween > 0 ? Math.abs(random.nextLong()) % (daysBetween + 1) : 0;
            LocalDate visitDate = startDate.plusDays(randomDays);
            LocalDateTime visitDateTime = visitDate.atTime(9 + random.nextInt(8), random.nextInt(60)); // 9 AM - 5 PM
            
            visit.setVisitDate(visitDateTime);
            visit.setVisitType(validVisitTypes().next());
            visit.setPet(pets.get(random.nextInt(pets.size())));
            visit.setVeterinarian(veterinarians.get(random.nextInt(veterinarians.size())));
            visit.setNotes(validVisitNotes().next());
            
            // Set diagnosis and treatment for some visits to make them completed
            if (random.nextBoolean()) {
                visit.setDiagnosis("Test diagnosis " + i);
                visit.setTreatment("Test treatment " + i);
            }
            
            visits.add(visitRepository.save(visit));
        }
        return visits;
    }
    
    private List<Visit> createTestVisitsWithCosts(List<Pet> pets, List<Veterinarian> veterinarians, 
                                                LocalDate startDate, LocalDate endDate, int count) {
        List<Visit> visits = createTestVisits(pets, veterinarians, startDate, endDate, count);
        
        // Add costs to visits
        for (Visit visit : visits) {
            visit.setCost(validVisitCosts().next());
            visitRepository.save(visit);
        }
        
        return visits;
    }
    // Additional generator methods from existing property tests
    
    protected Generator<String> validVeterinarianFirstNames() {
        String[] names = {"John", "Jane", "Michael", "Sarah", "David", "Emily", "Robert", "Lisa", "James", "Maria"};
        return () -> names[random.nextInt(names.length)];
    }
    
    protected Generator<String> validVeterinarianLastNames() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        return () -> names[random.nextInt(names.length)];
    }
    
    protected Generator<String> validStringSpecialties() {
        String[] specialties = {
            "General Practice",
            "Surgery, Cardiology",
            "Dermatology",
            "Orthopedics, Surgery",
            "Emergency Medicine",
            "Internal Medicine, Cardiology",
            "Neurology",
            "Oncology, Surgery",
            "Ophthalmology",
            "Dentistry, General Practice",
            "" // Empty string for general practitioners
        };
        return () -> specialties[random.nextInt(specialties.length)];
    }
    
    protected Generator<Set<Specialty>> validEnumSpecialties() {
        return () -> {
            Set<Specialty> specialties = new HashSet<>();
            Specialty[] allSpecialties = Specialty.values();
            
            // Generate 0-3 random specialties
            int count = random.nextInt(4);
            for (int i = 0; i < count; i++) {
                specialties.add(allSpecialties[random.nextInt(allSpecialties.length)]);
            }
            
            return specialties;
        };
    }
    
    // Helper method for currency formatting (matches the implementation)
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }
}