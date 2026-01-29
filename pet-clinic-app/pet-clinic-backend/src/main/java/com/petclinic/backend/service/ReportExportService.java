package com.petclinic.backend.service;

import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for report export functionality
 * Provides PDF and CSV export capabilities for all report types
 * Validates: Requirements 5.3
 */
public interface ReportExportService {
    
    /**
     * Export visit statistics report to PDF format
     * @param report Visit statistics report to export
     * @return PDF content as byte array
     */
    byte[] exportVisitStatisticsToPdf(VisitStatisticsReport report);
    
    /**
     * Export visit statistics report to CSV format
     * @param report Visit statistics report to export
     * @return CSV content as byte array
     */
    byte[] exportVisitStatisticsToCSV(VisitStatisticsReport report);
    
    /**
     * Export revenue report to PDF format
     * @param report Revenue report to export
     * @return PDF content as byte array
     */
    byte[] exportRevenueReportToPdf(RevenueReport report);
    
    /**
     * Export revenue report to CSV format
     * @param report Revenue report to export
     * @return CSV content as byte array
     */
    byte[] exportRevenueReportToCSV(RevenueReport report);
    
    /**
     * Export dashboard metrics to PDF format
     * @param metrics Dashboard metrics to export
     * @return PDF content as byte array
     */
    byte[] exportDashboardMetricsToPdf(DashboardMetrics metrics);
    
    /**
     * Export dashboard metrics to CSV format
     * @param metrics Dashboard metrics to export
     * @return CSV content as byte array
     */
    byte[] exportDashboardMetricsToCSV(DashboardMetrics metrics);
    
    /**
     * Export multiple visit statistics reports to PDF format
     * @param reports List of visit statistics reports to export
     * @param title Title for the combined report
     * @return PDF content as byte array
     */
    byte[] exportMultipleVisitStatisticsToPdf(List<VisitStatisticsReport> reports, String title);
    
    /**
     * Export multiple visit statistics reports to CSV format
     * @param reports List of visit statistics reports to export
     * @return CSV content as byte array
     */
    byte[] exportMultipleVisitStatisticsToCSV(List<VisitStatisticsReport> reports);
    
    /**
     * Export multiple revenue reports to PDF format
     * @param reports List of revenue reports to export
     * @param title Title for the combined report
     * @return PDF content as byte array
     */
    byte[] exportMultipleRevenueReportsToPdf(List<RevenueReport> reports, String title);
    
    /**
     * Export multiple revenue reports to CSV format
     * @param reports List of revenue reports to export
     * @return CSV content as byte array
     */
    byte[] exportMultipleRevenueReportsToCSV(List<RevenueReport> reports);
    
    /**
     * Export filtered visit statistics to PDF format
     * @param filter Report filter criteria
     * @return PDF content as byte array
     */
    byte[] exportFilteredVisitStatisticsToPdf(ReportFilter filter);
    
    /**
     * Export filtered visit statistics to CSV format
     * @param filter Report filter criteria
     * @return CSV content as byte array
     */
    byte[] exportFilteredVisitStatisticsToCSV(ReportFilter filter);
    
    /**
     * Export filtered revenue report to PDF format
     * @param filter Report filter criteria
     * @return PDF content as byte array
     */
    byte[] exportFilteredRevenueReportToPdf(ReportFilter filter);
    
    /**
     * Export filtered revenue report to CSV format
     * @param filter Report filter criteria
     * @return CSV content as byte array
     */
    byte[] exportFilteredRevenueReportToCSV(ReportFilter filter);
    
    /**
     * Get appropriate filename for export
     * @param reportType Type of report ("visit-statistics", "revenue", "dashboard")
     * @param format Export format ("pdf", "csv")
     * @param startDate Start date for the report (optional)
     * @param endDate End date for the report (optional)
     * @return Suggested filename
     */
    String getExportFilename(String reportType, String format, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get MIME type for export format
     * @param format Export format ("pdf", "csv")
     * @return MIME type string
     */
    String getMimeType(String format);
    
    /**
     * Validate export parameters
     * @param reportType Type of report
     * @param format Export format
     * @return true if parameters are valid, false otherwise
     */
    boolean validateExportParameters(String reportType, String format);
}