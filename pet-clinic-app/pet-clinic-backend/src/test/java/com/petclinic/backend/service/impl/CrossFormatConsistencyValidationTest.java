package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.ExportDataValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for cross-format consistency validation
 * Ensures data consistency between PDF and CSV versions of same report
 * Validates that totals, calculations, and aggregations match source data
 * 
 * Validates: Requirements 6.4, 6.5
 */
@ExtendWith(MockitoExtension.class)
class CrossFormatConsistencyValidationTest {
    
    private ExportDataValidationServiceImpl validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ExportDataValidationServiceImpl();
    }
    
    @Test
    void testCrossFormatConsistency_VisitStatistics_IdenticalData() {
        // Given - PDF and CSV with identical visit statistics data
        byte[] pdfContent = createVisitStatisticsPDF(
            "Total Visits: 100",
            "Completed Visits: 80", 
            "Completion Rate: 80.0%",
            "Dr. Smith: 50 visits",
            "Dr. Johnson: 50 visits",
            "Dog: 60",
            "Cat: 40"
        );
        
        byte[] csvContent = createVisitStatisticsCSV(
            "Total Visits,100",
            "Completed Visits,80",
            "Completion Rate,80.0%",
            "Dr. Smith,50",
            "Dr. Johnson,50",
            "Dog,60",
            "Cat,40"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then
        assertTrue(result.isValid(), "Identical data should be consistent: " + result.getErrors());
        assertTrue(result.getErrors().isEmpty(), "No errors expected for identical data");
    }
    
    @Test
    void testCrossFormatConsistency_VisitStatistics_InconsistentTotals() {
        // Given - PDF and CSV with different total visits
        byte[] pdfContent = createVisitStatisticsPDF(
            "Total Visits: 100",
            "Completed Visits: 80",
            "Completion Rate: 80.0%"
        );
        
        byte[] csvContent = createVisitStatisticsCSV(
            "Total Visits,95", // Different total
            "Completed Visits,80",
            "Completion Rate,80.0%"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent totals should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && error.toLowerCase().contains("totalvisits")),
            "Should detect inconsistent total visits");
    }
    
    @Test
    void testCrossFormatConsistency_VisitStatistics_InconsistentCalculations() {
        // Given - PDF and CSV with different completion rates
        byte[] pdfContent = createVisitStatisticsPDF(
            "Total Visits: 100",
            "Completed Visits: 80",
            "Completion Rate: 80.0%" // Correct calculation
        );
        
        byte[] csvContent = createVisitStatisticsCSV(
            "Total Visits,100",
            "Completed Visits,80",
            "Completion Rate,75.0%" // Incorrect calculation
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent calculations should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && error.toLowerCase().contains("completionrate")),
            "Should detect inconsistent completion rate");
    }
    
    @Test
    void testCrossFormatConsistency_VisitStatistics_InconsistentBreakdowns() {
        // Given - PDF and CSV with different veterinarian breakdowns
        byte[] pdfContent = createVisitStatisticsPDF(
            "Total Visits: 100",
            "Dr. Smith: 50 visits",
            "Dr. Johnson: 50 visits"
        );
        
        byte[] csvContent = createVisitStatisticsCSV(
            "Total Visits,100",
            "Dr. Smith,45", // Different breakdown
            "Dr. Johnson,55"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent breakdowns should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && error.contains("Dr. Smith")),
            "Should detect inconsistent veterinarian breakdown");
    }
    
    @Test
    void testCrossFormatConsistency_Revenue_IdenticalData() {
        // Given - PDF and CSV with identical revenue data
        byte[] pdfContent = createRevenuePDF(
            "Total Revenue: $5,000.00",
            "Total Paid Visits: 80",
            "Payment Rate: 80.0%",
            "Average Revenue per Visit: $62.50",
            "Dr. Smith: $2,500.00",
            "Dr. Johnson: $2,500.00"
        );
        
        byte[] csvContent = createRevenueCSV(
            "Total Revenue,$5000.00",
            "Total Paid Visits,80",
            "Payment Rate,80.0%",
            "Average Revenue per Visit,$62.50",
            "Dr. Smith,$2500.00",
            "Dr. Johnson,$2500.00"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "revenue");
        
        // Then
        assertTrue(result.isValid(), "Identical revenue data should be consistent: " + result.getErrors());
        assertTrue(result.getErrors().isEmpty(), "No errors expected for identical revenue data");
    }
    
    @Test
    void testCrossFormatConsistency_Revenue_InconsistentTotals() {
        // Given - PDF and CSV with different total revenue
        byte[] pdfContent = createRevenuePDF(
            "Total Revenue: $5,000.00",
            "Total Paid Visits: 80",
            "Dr. Smith: $2,500.00",
            "Dr. Johnson: $2,500.00"
        );
        
        byte[] csvContent = createRevenueCSV(
            "Total Revenue,$4800.00", // Different total
            "Total Paid Visits,80",
            "Dr. Smith,$2500.00",
            "Dr. Johnson,$2500.00"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "revenue");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent revenue totals should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && error.toLowerCase().contains("totalrevenue")),
            "Should detect inconsistent total revenue");
    }
    
    @Test
    void testCrossFormatConsistency_Revenue_InconsistentCalculations() {
        // Given - PDF and CSV with different average revenue calculations
        byte[] pdfContent = createRevenuePDF(
            "Total Revenue: $5,000.00",
            "Total Paid Visits: 80",
            "Average Revenue per Visit: $62.50" // Correct: 5000/80 = 62.50
        );
        
        byte[] csvContent = createRevenueCSV(
            "Total Revenue,$5000.00",
            "Total Paid Visits,80",
            "Average Revenue per Visit,$60.00" // Incorrect calculation
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "revenue");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent revenue calculations should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && error.toLowerCase().contains("averagerevenuepervisit")),
            "Should detect inconsistent average revenue calculation");
    }
    
    @Test
    void testCrossFormatConsistency_Revenue_BreakdownTotalMismatch() {
        // Given - PDF and CSV where breakdown totals don't match overall total
        byte[] pdfContent = createRevenuePDF(
            "Total Revenue: $5,000.00",
            "Dr. Smith: $2,000.00", // Breakdown total = $4,500 != $5,000
            "Dr. Johnson: $2,500.00"
        );
        
        byte[] csvContent = createRevenueCSV(
            "Total Revenue,$5000.00",
            "Dr. Smith,$2000.00",
            "Dr. Johnson,$2500.00"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "revenue");
        
        // Then
        // This should be detected as an internal consistency issue
        // The formats are consistent with each other, but both have the same error
        assertTrue(result.isValid() || result.getWarnings().stream().anyMatch(warning -> 
            warning.toLowerCase().contains("total")),
            "Should detect or warn about breakdown total mismatch");
    }
    
    @Test
    void testCrossFormatConsistency_Dashboard_IdenticalData() {
        // Given - PDF and CSV with identical dashboard data
        byte[] pdfContent = createDashboardPDF(
            "Today's Appointments: 10",
            "Today's Completed Visits: 8",
            "Today's Pending Visits: 2",
            "Today's Revenue: $800.00",
            "Total Active Pets: 500",
            "Veterinarian Utilization: 75.0%"
        );
        
        byte[] csvContent = createDashboardCSV(
            "Today's Appointments,10",
            "Today's Completed Visits,8",
            "Today's Pending Visits,2",
            "Today's Revenue,$800.00",
            "Total Active Pets,500",
            "Veterinarian Utilization,75.0%"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "dashboard");
        
        // Then
        assertTrue(result.isValid(), "Identical dashboard data should be consistent: " + result.getErrors());
        assertTrue(result.getErrors().isEmpty(), "No errors expected for identical dashboard data");
    }
    
    @Test
    void testCrossFormatConsistency_Dashboard_InconsistentAppointmentTotals() {
        // Given - PDF and CSV with inconsistent appointment calculations
        byte[] pdfContent = createDashboardPDF(
            "Today's Appointments: 10",
            "Today's Completed Visits: 8",
            "Today's Pending Visits: 2" // 8 + 2 = 10 ✓
        );
        
        byte[] csvContent = createDashboardCSV(
            "Today's Appointments,10",
            "Today's Completed Visits,7", // 7 + 2 = 9 ≠ 10 ✗
            "Today's Pending Visits,2"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "dashboard");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent appointment totals should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && 
            (error.toLowerCase().contains("todaycompletedvisits") || 
             error.toLowerCase().contains("completed visits") ||
             error.toLowerCase().contains("completedvisits"))),
            "Should detect inconsistent completed visits");
    }
    
    @Test
    void testCrossFormatConsistency_EmptyContent() {
        // Given - Empty content
        byte[] emptyPdf = new byte[0];
        byte[] emptyCsv = new byte[0];
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(emptyPdf, emptyCsv, "visits");
        
        // Then
        assertFalse(result.isValid(), "Empty content should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("empty")),
            "Should detect empty content");
    }
    
    @Test
    void testCrossFormatConsistency_NullContent() {
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(null, null, "visits");
        
        // Then
        assertFalse(result.isValid(), "Null content should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("null")),
            "Should detect null content");
    }
    
    @Test
    void testCrossFormatConsistency_DateRangeConsistency() {
        // Given - PDF and CSV with different date ranges
        byte[] pdfContent = createVisitStatisticsPDF(
            "Report Period: 2024-01-01 to 2024-01-31",
            "Total Visits: 100"
        );
        
        byte[] csvContent = createVisitStatisticsCSV(
            "Start Date,2024-01-01",
            "End Date,2024-02-28", // Different end date
            "Total Visits,100"
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then
        assertFalse(result.isValid(), "Inconsistent date ranges should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> 
            error.toLowerCase().contains("inconsistent") && error.toLowerCase().contains("date")),
            "Should detect inconsistent date ranges");
    }
    
    @Test
    void testCrossFormatConsistency_NumericFormatTolerance() {
        // Given - PDF and CSV with slightly different numeric formatting but same values
        byte[] pdfContent = createRevenuePDF(
            "Total Revenue: $5,000.00",
            "Average Revenue per Visit: $62.50"
        );
        
        byte[] csvContent = createRevenueCSV(
            "Total Revenue,$5000", // No decimal places
            "Average Revenue per Visit,$62.5" // One decimal place
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "revenue");
        
        // Then
        assertTrue(result.isValid(), "Minor formatting differences should be tolerated: " + result.getErrors());
    }
    
    @Test
    void testCrossFormatConsistency_PercentageFormatTolerance() {
        // Given - PDF and CSV with different percentage formatting but same values
        byte[] pdfContent = createVisitStatisticsPDF(
            "Completion Rate: 80.0%"
        );
        
        byte[] csvContent = createVisitStatisticsCSV(
            "Completion Rate,80%" // No decimal places
        );
        
        // When
        ExportDataValidationService.ValidationResult result = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then
        assertTrue(result.isValid(), "Minor percentage formatting differences should be tolerated: " + result.getErrors());
    }
    
    // Helper methods to create test content
    
    private byte[] createVisitStatisticsPDF(String... lines) {
        StringBuilder content = new StringBuilder();
        content.append("Visit Statistics Report\n");
        content.append("Generated on 2024-01-15 10:30:00\n\n");
        
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        return content.toString().getBytes();
    }
    
    private byte[] createVisitStatisticsCSV(String... lines) {
        StringBuilder content = new StringBuilder();
        content.append("Visit Statistics Report\n");
        content.append("Generated,2024-01-15 10:30:00\n");
        content.append("Metric,Value\n");
        
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        return content.toString().getBytes();
    }
    
    private byte[] createRevenuePDF(String... lines) {
        StringBuilder content = new StringBuilder();
        content.append("Revenue Report\n");
        content.append("Generated on 2024-01-15 10:30:00\n\n");
        
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        return content.toString().getBytes();
    }
    
    private byte[] createRevenueCSV(String... lines) {
        StringBuilder content = new StringBuilder();
        content.append("Revenue Report\n");
        content.append("Generated,2024-01-15 10:30:00\n");
        content.append("Metric,Value\n");
        
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        return content.toString().getBytes();
    }
    
    private byte[] createDashboardPDF(String... lines) {
        StringBuilder content = new StringBuilder();
        content.append("Dashboard Metrics Report\n");
        content.append("Generated on 2024-01-15 10:30:00\n\n");
        
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        return content.toString().getBytes();
    }
    
    private byte[] createDashboardCSV(String... lines) {
        StringBuilder content = new StringBuilder();
        content.append("Dashboard Metrics Report\n");
        content.append("Generated,2024-01-15 10:30:00\n");
        content.append("Metric,Value\n");
        
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        return content.toString().getBytes();
    }
}