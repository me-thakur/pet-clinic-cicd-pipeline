package com.petclinic.backend.service;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;

import java.util.List;
import java.util.Map;

/**
 * Service interface for validating export data content and consistency
 * Ensures PDF reports include all data fields from web report views
 * Validates CSV formatting with proper headers, escaping, and delimiters
 * Verifies filtering logic consistency between web reports and exports
 * 
 * Validates: Requirements 6.1, 6.2, 6.3
 */
public interface ExportDataValidationService {
    
    /**
     * Validation result for export data
     */
    class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private Map<String, Object> metadata;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * Validate visit statistics report data completeness and accuracy
     * Ensures all fields displayed in web view are included in export
     * 
     * @param report Visit statistics report to validate
     * @param filter Original filter used to generate the report
     * @return Validation result with any issues found
     */
    ValidationResult validateVisitStatisticsData(VisitStatisticsReport report, ReportFilter filter);
    
    /**
     * Validate revenue report data completeness and accuracy
     * Ensures all fields displayed in web view are included in export
     * 
     * @param report Revenue report to validate
     * @param filter Original filter used to generate the report
     * @return Validation result with any issues found
     */
    ValidationResult validateRevenueReportData(RevenueReport report, ReportFilter filter);
    
    /**
     * Validate dashboard metrics data completeness and accuracy
     * Ensures all fields displayed in web view are included in export
     * 
     * @param metrics Dashboard metrics to validate
     * @return Validation result with any issues found
     */
    ValidationResult validateDashboardMetricsData(DashboardMetrics metrics);
    
    /**
     * Validate CSV content formatting
     * Checks headers, escaping, delimiters, and data integrity
     * 
     * @param csvContent CSV content as byte array
     * @param expectedHeaders List of expected column headers
     * @param reportType Type of report ("visits", "revenue", "dashboard")
     * @return Validation result with formatting issues
     */
    ValidationResult validateCSVFormatting(byte[] csvContent, List<String> expectedHeaders, String reportType);
    
    /**
     * Validate PDF content completeness
     * Ensures all required sections and data fields are present
     * 
     * @param pdfContent PDF content as byte array
     * @param reportType Type of report ("visits", "revenue", "dashboard")
     * @param expectedSections List of expected sections in the PDF
     * @return Validation result with content issues
     */
    ValidationResult validatePDFContent(byte[] pdfContent, String reportType, List<String> expectedSections);
    
    /**
     * Validate cross-format data consistency
     * Ensures PDF and CSV versions contain identical data
     * 
     * @param pdfContent PDF content as byte array
     * @param csvContent CSV content as byte array
     * @param reportType Type of report
     * @return Validation result with consistency issues
     */
    ValidationResult validateCrossFormatConsistency(byte[] pdfContent, byte[] csvContent, String reportType);
    
    /**
     * Validate filtering logic consistency
     * Ensures exported data matches the applied filters
     * 
     * @param reportData Report data object (VisitStatisticsReport, RevenueReport, or DashboardMetrics)
     * @param filter Applied filter criteria
     * @return Validation result with filtering issues
     */
    ValidationResult validateFilteringConsistency(Object reportData, ReportFilter filter);
    
    /**
     * Get expected data fields for a report type
     * Returns list of all fields that should be present in exports
     * 
     * @param reportType Type of report ("visits", "revenue", "dashboard")
     * @return List of expected field names
     */
    List<String> getExpectedDataFields(String reportType);
    
    /**
     * Get expected CSV headers for a report type
     * Returns properly formatted headers with correct ordering
     * 
     * @param reportType Type of report ("visits", "revenue", "dashboard")
     * @return List of CSV headers in correct order
     */
    List<String> getExpectedCSVHeaders(String reportType);
    
    /**
     * Get expected PDF sections for a report type
     * Returns list of sections that should be present in PDF exports
     * 
     * @param reportType Type of report ("visits", "revenue", "dashboard")
     * @return List of expected PDF sections
     */
    List<String> getExpectedPDFSections(String reportType);
}