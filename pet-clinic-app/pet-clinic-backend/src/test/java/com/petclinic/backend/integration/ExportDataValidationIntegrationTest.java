package com.petclinic.backend.integration;

import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.service.ExportDataValidationService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for export data validation
 * Tests the complete workflow from data generation to export validation
 * 
 * Validates: Requirements 6.1, 6.2, 6.3
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExportDataValidationIntegrationTest {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ReportExportService exportService;
    
    @Autowired
    private ExportDataValidationService validationService;
    
    @Test
    void testCompleteExportValidationWorkflow_VisitStatistics() {
        // Given
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(LocalDate.of(2024, 1, 1));
        filter.setEndDate(LocalDate.of(2024, 1, 31));
        
        // When - Generate report data
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        
        // Then - Validate report data
        ExportDataValidationService.ValidationResult dataValidation = 
                validationService.validateVisitStatisticsData(report, filter);
        
        assertTrue(dataValidation.isValid(), 
                "Generated report data should be valid: " + dataValidation.getErrors());
        
        // When - Export to PDF
        byte[] pdfContent = exportService.exportVisitStatisticsToPdf(report);
        
        // Then - Validate PDF content
        List<String> expectedPDFSections = validationService.getExpectedPDFSections("visits");
        ExportDataValidationService.ValidationResult pdfValidation = 
                validationService.validatePDFContent(pdfContent, "visits", expectedPDFSections);
        
        assertTrue(pdfValidation.isValid(), 
                "Generated PDF should be valid: " + pdfValidation.getErrors());
        assertFalse(pdfContent.length == 0, "PDF content should not be empty");
        
        // When - Export to CSV
        byte[] csvContent = exportService.exportVisitStatisticsToCSV(report);
        
        // Then - Validate CSV content
        List<String> expectedCSVHeaders = validationService.getExpectedCSVHeaders("visits");
        ExportDataValidationService.ValidationResult csvValidation = 
                validationService.validateCSVFormatting(csvContent, expectedCSVHeaders, "visits");
        
        assertTrue(csvValidation.isValid(), 
                "Generated CSV should be valid: " + csvValidation.getErrors());
        assertFalse(csvContent.length == 0, "CSV content should not be empty");
        
        // When - Validate cross-format consistency
        ExportDataValidationService.ValidationResult consistencyValidation = 
                validationService.validateCrossFormatConsistency(pdfContent, csvContent, "visits");
        
        // Then - Formats should be consistent
        assertTrue(consistencyValidation.isValid(), 
                "PDF and CSV should be consistent: " + consistencyValidation.getErrors());
        
        // Verify filtering consistency
        ExportDataValidationService.ValidationResult filterValidation = 
                validationService.validateFilteringConsistency(report, filter);
        
        assertTrue(filterValidation.isValid(), 
                "Report should match applied filters: " + filterValidation.getErrors());
    }
    
    @Test
    void testExportDataFieldCompleteness_VisitStatistics() {
        // Given
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(LocalDate.of(2024, 1, 1));
        filter.setEndDate(LocalDate.of(2024, 1, 31));
        
        // When
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        byte[] pdfContent = exportService.exportVisitStatisticsToPdf(report);
        byte[] csvContent = exportService.exportVisitStatisticsToCSV(report);
        
        // Then - Verify all expected data fields are present
        List<String> expectedFields = validationService.getExpectedDataFields("visits");
        
        // Check PDF contains key metrics using proper PDF text extraction
        String pdfText = extractTextFromPDF(pdfContent);
        assertTrue(pdfText.toLowerCase().contains("total visits"), 
                "PDF should contain 'Total Visits' field");
        assertTrue(pdfText.toLowerCase().contains("completed visits"), 
                "PDF should contain 'Completed Visits' field");
        assertTrue(pdfText.toLowerCase().contains("completion rate"), 
                "PDF should contain 'Completion Rate' field");
        
        // Check CSV contains structured data
        String csvText = new String(csvContent);
        assertTrue(csvText.contains("Total Visits"), 
                "CSV should contain 'Total Visits' data");
        assertTrue(csvText.contains("Completed Visits"), 
                "CSV should contain 'Completed Visits' data");
        assertTrue(csvText.contains("Completion Rate"), 
                "CSV should contain 'Completion Rate' data");
        
        // Verify data consistency between formats
        assertTrue(pdfText.contains(String.valueOf(report.getTotalVisits())), 
                "PDF should contain correct total visits count");
        assertTrue(csvText.contains(String.valueOf(report.getTotalVisits())), 
                "CSV should contain correct total visits count");
    }
    
    @Test
    void testCSVFormattingValidation() {
        // Given
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(LocalDate.of(2024, 1, 1));
        filter.setEndDate(LocalDate.of(2024, 1, 31));
        
        // When
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        byte[] csvContent = exportService.exportVisitStatisticsToCSV(report);
        
        // Then - Validate CSV structure
        String csvText = new String(csvContent);
        String[] lines = csvText.split("\n");
        
        assertTrue(lines.length > 1, "CSV should have multiple lines");
        
        // Check for proper CSV formatting
        boolean hasCommaDelimiters = false;
        for (String line : lines) {
            if (line.contains(",") && !line.trim().isEmpty()) {
                hasCommaDelimiters = true;
                break;
            }
        }
        assertTrue(hasCommaDelimiters, "CSV should use comma delimiters");
        
        // Validate headers are present
        List<String> expectedHeaders = validationService.getExpectedCSVHeaders("visits");
        ExportDataValidationService.ValidationResult validation = 
                validationService.validateCSVFormatting(csvContent, expectedHeaders, "visits");
        
        if (!validation.isValid()) {
            fail("CSV formatting validation failed: " + validation.getErrors());
        }
        
        // Check for proper escaping (no unbalanced quotes)
        for (String line : lines) {
            long quoteCount = line.chars().filter(ch -> ch == '"').count();
            if (quoteCount % 2 != 0) {
                fail("CSV line has unbalanced quotes: " + line);
            }
        }
    }
    
    @Test
    void testFilteringConsistencyValidation() {
        // Given - Filter for specific date range
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(LocalDate.of(2024, 1, 1));
        filter.setEndDate(LocalDate.of(2024, 1, 31));
        
        // When
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        
        // Then - Report should match filter criteria
        assertEquals(filter.getStartDate(), report.getStartDate(), 
                "Report start date should match filter");
        assertEquals(filter.getEndDate(), report.getEndDate(), 
                "Report end date should match filter");
        
        // Validate filtering consistency
        ExportDataValidationService.ValidationResult validation = 
                validationService.validateFilteringConsistency(report, filter);
        
        assertTrue(validation.isValid(), 
                "Filtering should be consistent: " + validation.getErrors());
        
        // Export and verify consistency is maintained
        byte[] pdfContent = exportService.exportFilteredVisitStatisticsToPdf(filter);
        byte[] csvContent = exportService.exportFilteredVisitStatisticsToCSV(filter);
        
        assertNotNull(pdfContent, "Filtered PDF export should not be null");
        assertNotNull(csvContent, "Filtered CSV export should not be null");
        assertTrue(pdfContent.length > 0, "Filtered PDF should have content");
        assertTrue(csvContent.length > 0, "Filtered CSV should have content");
        
        // Verify date range is reflected in exports using proper PDF text extraction
        String pdfText = extractTextFromPDF(pdfContent);
        assertTrue(pdfText.contains("2024-01-01") || pdfText.contains("Jan 01, 2024"), 
                "PDF should contain start date");
        assertTrue(pdfText.contains("2024-01-31") || pdfText.contains("Jan 31, 2024"), 
                "PDF should contain end date");
    }
    
    @Test
    void testExportValidationWithEmptyData() {
        // Given - Filter that should return no data
        ReportFilter filter = new ReportFilter();
        filter.setStartDate(LocalDate.of(2025, 12, 1)); // Future date
        filter.setEndDate(LocalDate.of(2025, 12, 31));
        
        // When
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        
        // Then - Report should be valid even with no data
        ExportDataValidationService.ValidationResult validation = 
                validationService.validateVisitStatisticsData(report, filter);
        
        assertTrue(validation.isValid(), 
                "Empty report should still be valid: " + validation.getErrors());
        
        // Exports should work with empty data
        byte[] pdfContent = exportService.exportVisitStatisticsToPdf(report);
        byte[] csvContent = exportService.exportVisitStatisticsToCSV(report);
        
        assertNotNull(pdfContent, "PDF export should work with empty data");
        assertNotNull(csvContent, "CSV export should work with empty data");
        assertTrue(pdfContent.length > 0, "PDF should have basic structure even with no data");
        assertTrue(csvContent.length > 0, "CSV should have headers even with no data");
        
        // Validate empty data exports
        List<String> expectedPDFSections = validationService.getExpectedPDFSections("visits");
        ExportDataValidationService.ValidationResult pdfValidation = 
                validationService.validatePDFContent(pdfContent, "visits", expectedPDFSections);
        
        assertTrue(pdfValidation.isValid(), 
                "Empty PDF should be valid: " + pdfValidation.getErrors());
    }
    
    /**
     * Helper method to extract text from PDF content using iText
     */
    private String extractTextFromPDF(byte[] pdfContent) {
        try {
            com.itextpdf.kernel.pdf.PdfReader reader = new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(pdfContent));
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(reader);
            
            StringBuilder fullText = new StringBuilder();
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                String pageText = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                fullText.append(pageText).append("\n");
            }
            pdfDoc.close();
            
            return fullText.toString();
        } catch (Exception e) {
            // Fallback to treating as plain text for testing purposes
            return new String(pdfContent);
        }
    }
}