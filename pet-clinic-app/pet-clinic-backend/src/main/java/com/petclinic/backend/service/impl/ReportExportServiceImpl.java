package com.petclinic.backend.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.opencsv.CSVWriter;
import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ReportExportService providing PDF and CSV export functionality
 * Validates: Requirements 5.3
 */
@Service
public class ReportExportServiceImpl implements ReportExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportExportServiceImpl.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private ReportService reportService;
    
    @Override
    public byte[] exportVisitStatisticsToPdf(VisitStatisticsReport report) {
        logger.debug("Exporting visit statistics report to PDF: {}", report);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Add title
            document.add(new Paragraph("Visit Statistics Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add date range
            document.add(new Paragraph("Period: " + report.getDateRangeDescription())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add generation timestamp
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("\n"));
            
            // Summary statistics table
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(summaryTable, "Metric", "Value");
            addTableRow(summaryTable, "Total Visits", String.valueOf(report.getTotalVisits()));
            addTableRow(summaryTable, "Completed Visits", String.valueOf(report.getCompletedVisits()));
            addTableRow(summaryTable, "Scheduled Visits", String.valueOf(report.getScheduledVisits()));
            addTableRow(summaryTable, "Cancelled Visits", String.valueOf(report.getCancelledVisits()));
            addTableRow(summaryTable, "Completion Rate", String.format("%.1f%%", report.getCompletionRate()));
            addTableRow(summaryTable, "Average Visits/Day", String.format("%.1f", report.getAverageVisitsPerDay()));
            
            document.add(summaryTable);
            document.add(new Paragraph("\n"));
            
            // Visits by veterinarian
            if (report.getVisitsByVeterinarian() != null && !report.getVisitsByVeterinarian().isEmpty()) {
                document.add(new Paragraph("Visits by Veterinarian").setFontSize(14).setBold());
                Table vetTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                vetTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(vetTable, "Veterinarian", "Visits");
                for (Map.Entry<String, Long> entry : report.getVisitsByVeterinarian().entrySet()) {
                    addTableRow(vetTable, entry.getKey(), String.valueOf(entry.getValue()));
                }
                document.add(vetTable);
                document.add(new Paragraph("\n"));
            }
            
            // Visits by type
            if (report.getVisitsByType() != null && !report.getVisitsByType().isEmpty()) {
                document.add(new Paragraph("Visits by Type").setFontSize(14).setBold());
                Table typeTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                typeTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(typeTable, "Visit Type", "Count");
                for (Map.Entry<VisitType, Long> entry : report.getVisitsByType().entrySet()) {
                    addTableRow(typeTable, entry.getKey().getDisplayName(), String.valueOf(entry.getValue()));
                }
                document.add(typeTable);
                document.add(new Paragraph("\n"));
            }
            
            // Visits by species
            if (report.getVisitsBySpecies() != null && !report.getVisitsBySpecies().isEmpty()) {
                document.add(new Paragraph("Visits by Species").setFontSize(14).setBold());
                Table speciesTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                speciesTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(speciesTable, "Species", "Visits");
                for (Map.Entry<String, Long> entry : report.getVisitsBySpecies().entrySet()) {
                    addTableRow(speciesTable, entry.getKey(), String.valueOf(entry.getValue()));
                }
                document.add(speciesTable);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting visit statistics to PDF", e);
            throw new PetClinicException("Failed to export visit statistics to PDF", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportVisitStatisticsToCSV(VisitStatisticsReport report) {
        logger.debug("Exporting visit statistics report to CSV: {}", report);
        
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            
            // Header information
            csvWriter.writeNext(new String[]{"Visit Statistics Report"});
            csvWriter.writeNext(new String[]{"Period", report.getDateRangeDescription()});
            csvWriter.writeNext(new String[]{"Generated", LocalDateTime.now().format(DATETIME_FORMATTER)});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Summary statistics
            csvWriter.writeNext(new String[]{"Summary Statistics"});
            csvWriter.writeNext(new String[]{"Metric", "Value"});
            csvWriter.writeNext(new String[]{"Total Visits", String.valueOf(report.getTotalVisits())});
            csvWriter.writeNext(new String[]{"Completed Visits", String.valueOf(report.getCompletedVisits())});
            csvWriter.writeNext(new String[]{"Scheduled Visits", String.valueOf(report.getScheduledVisits())});
            csvWriter.writeNext(new String[]{"Cancelled Visits", String.valueOf(report.getCancelledVisits())});
            csvWriter.writeNext(new String[]{"Completion Rate", String.format("%.1f%%", report.getCompletionRate())});
            csvWriter.writeNext(new String[]{"Average Visits/Day", String.format("%.1f", report.getAverageVisitsPerDay())});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Visits by veterinarian
            if (report.getVisitsByVeterinarian() != null && !report.getVisitsByVeterinarian().isEmpty()) {
                csvWriter.writeNext(new String[]{"Visits by Veterinarian"});
                csvWriter.writeNext(new String[]{"Veterinarian", "Visits"});
                for (Map.Entry<String, Long> entry : report.getVisitsByVeterinarian().entrySet()) {
                    csvWriter.writeNext(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Visits by type
            if (report.getVisitsByType() != null && !report.getVisitsByType().isEmpty()) {
                csvWriter.writeNext(new String[]{"Visits by Type"});
                csvWriter.writeNext(new String[]{"Visit Type", "Count"});
                for (Map.Entry<VisitType, Long> entry : report.getVisitsByType().entrySet()) {
                    csvWriter.writeNext(new String[]{entry.getKey().getDisplayName(), String.valueOf(entry.getValue())});
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Visits by species
            if (report.getVisitsBySpecies() != null && !report.getVisitsBySpecies().isEmpty()) {
                csvWriter.writeNext(new String[]{"Visits by Species"});
                csvWriter.writeNext(new String[]{"Species", "Visits"});
                for (Map.Entry<String, Long> entry : report.getVisitsBySpecies().entrySet()) {
                    csvWriter.writeNext(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
                }
            }
            
            // Daily breakdown if available
            if (report.getVisitsByDate() != null && !report.getVisitsByDate().isEmpty()) {
                csvWriter.writeNext(new String[]{}); // Empty line
                csvWriter.writeNext(new String[]{"Daily Breakdown"});
                csvWriter.writeNext(new String[]{"Date", "Visits"});
                report.getVisitsByDate().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> csvWriter.writeNext(new String[]{
                                entry.getKey().format(DATE_FORMATTER),
                                String.valueOf(entry.getValue())
                        }));
            }
            
            return stringWriter.toString().getBytes();
            
        } catch (IOException e) {
            logger.error("Error exporting visit statistics to CSV", e);
            throw new PetClinicException("Failed to export visit statistics to CSV", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportRevenueReportToPdf(RevenueReport report) {
        logger.debug("Exporting revenue report to PDF: {}", report);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Add title
            document.add(new Paragraph("Revenue Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add date range
            document.add(new Paragraph("Period: " + report.getDateRangeDescription())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add generation timestamp
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("\n"));
            
            // Summary statistics table
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(summaryTable, "Metric", "Value");
            addTableRow(summaryTable, "Total Revenue", formatCurrency(report.getTotalRevenue()));
            addTableRow(summaryTable, "Total Paid Visits", String.valueOf(report.getTotalPaidVisits()));
            addTableRow(summaryTable, "Total Unpaid Visits", String.valueOf(report.getTotalUnpaidVisits()));
            addTableRow(summaryTable, "Average Revenue/Visit", formatCurrency(report.getAverageRevenuePerVisit()));
            addTableRow(summaryTable, "Average Revenue/Day", formatCurrency(report.getAverageRevenuePerDay()));
            addTableRow(summaryTable, "Payment Rate", String.format("%.1f%%", report.getPaymentRate()));
            
            document.add(summaryTable);
            document.add(new Paragraph("\n"));
            
            // Trend data if available
            if (report.getTrendData() != null) {
                document.add(new Paragraph("Trend Analysis").setFontSize(14).setBold());
                Table trendTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                trendTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(trendTable, "Metric", "Value");
                addTableRow(trendTable, "Previous Period Revenue", formatCurrency(report.getTrendData().getPreviousPeriodRevenue()));
                addTableRow(trendTable, "Growth Rate", String.format("%.1f%%", report.getTrendData().getGrowthRate()));
                addTableRow(trendTable, "Trend Direction", report.getTrendData().getTrendDirection());
                addTableRow(trendTable, "Description", report.getTrendData().getTrendDescription());
                
                document.add(trendTable);
                document.add(new Paragraph("\n"));
            }
            
            // Revenue by veterinarian
            if (report.getRevenueByVeterinarian() != null && !report.getRevenueByVeterinarian().isEmpty()) {
                document.add(new Paragraph("Revenue by Veterinarian").setFontSize(14).setBold());
                Table vetTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                vetTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(vetTable, "Veterinarian", "Revenue");
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueByVeterinarian().entrySet()) {
                    addTableRow(vetTable, entry.getKey(), formatCurrency(entry.getValue()));
                }
                document.add(vetTable);
                document.add(new Paragraph("\n"));
            }
            
            // Revenue by species
            if (report.getRevenueBySpecies() != null && !report.getRevenueBySpecies().isEmpty()) {
                document.add(new Paragraph("Revenue by Species").setFontSize(14).setBold());
                Table speciesTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                speciesTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(speciesTable, "Species", "Revenue");
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueBySpecies().entrySet()) {
                    addTableRow(speciesTable, entry.getKey(), formatCurrency(entry.getValue()));
                }
                document.add(speciesTable);
                document.add(new Paragraph("\n"));
            }
            
            // Revenue by visit type
            if (report.getRevenueByVisitType() != null && !report.getRevenueByVisitType().isEmpty()) {
                document.add(new Paragraph("Revenue by Visit Type").setFontSize(14).setBold());
                Table typeTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
                typeTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(typeTable, "Visit Type", "Revenue");
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueByVisitType().entrySet()) {
                    addTableRow(typeTable, entry.getKey(), formatCurrency(entry.getValue()));
                }
                document.add(typeTable);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting revenue report to PDF", e);
            throw new PetClinicException("Failed to export revenue report to PDF", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportRevenueReportToCSV(RevenueReport report) {
        logger.debug("Exporting revenue report to CSV: {}", report);
        
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            
            // Header information
            csvWriter.writeNext(new String[]{"Revenue Report"});
            csvWriter.writeNext(new String[]{"Period", report.getDateRangeDescription()});
            csvWriter.writeNext(new String[]{"Generated", LocalDateTime.now().format(DATETIME_FORMATTER)});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Summary statistics
            csvWriter.writeNext(new String[]{"Summary Statistics"});
            csvWriter.writeNext(new String[]{"Metric", "Value"});
            csvWriter.writeNext(new String[]{"Total Revenue", formatCurrency(report.getTotalRevenue())});
            csvWriter.writeNext(new String[]{"Total Paid Visits", String.valueOf(report.getTotalPaidVisits())});
            csvWriter.writeNext(new String[]{"Total Unpaid Visits", String.valueOf(report.getTotalUnpaidVisits())});
            csvWriter.writeNext(new String[]{"Average Revenue/Visit", formatCurrency(report.getAverageRevenuePerVisit())});
            csvWriter.writeNext(new String[]{"Average Revenue/Day", formatCurrency(report.getAverageRevenuePerDay())});
            csvWriter.writeNext(new String[]{"Payment Rate", String.format("%.1f%%", report.getPaymentRate())});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Trend data if available
            if (report.getTrendData() != null) {
                csvWriter.writeNext(new String[]{"Trend Analysis"});
                csvWriter.writeNext(new String[]{"Metric", "Value"});
                csvWriter.writeNext(new String[]{"Previous Period Revenue", formatCurrency(report.getTrendData().getPreviousPeriodRevenue())});
                csvWriter.writeNext(new String[]{"Growth Rate", String.format("%.1f%%", report.getTrendData().getGrowthRate())});
                csvWriter.writeNext(new String[]{"Trend Direction", report.getTrendData().getTrendDirection()});
                csvWriter.writeNext(new String[]{"Description", report.getTrendData().getTrendDescription()});
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Revenue by veterinarian
            if (report.getRevenueByVeterinarian() != null && !report.getRevenueByVeterinarian().isEmpty()) {
                csvWriter.writeNext(new String[]{"Revenue by Veterinarian"});
                csvWriter.writeNext(new String[]{"Veterinarian", "Revenue"});
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueByVeterinarian().entrySet()) {
                    csvWriter.writeNext(new String[]{entry.getKey(), formatCurrency(entry.getValue())});
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Revenue by species
            if (report.getRevenueBySpecies() != null && !report.getRevenueBySpecies().isEmpty()) {
                csvWriter.writeNext(new String[]{"Revenue by Species"});
                csvWriter.writeNext(new String[]{"Species", "Revenue"});
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueBySpecies().entrySet()) {
                    csvWriter.writeNext(new String[]{entry.getKey(), formatCurrency(entry.getValue())});
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Revenue by visit type
            if (report.getRevenueByVisitType() != null && !report.getRevenueByVisitType().isEmpty()) {
                csvWriter.writeNext(new String[]{"Revenue by Visit Type"});
                csvWriter.writeNext(new String[]{"Visit Type", "Revenue"});
                for (Map.Entry<String, BigDecimal> entry : report.getRevenueByVisitType().entrySet()) {
                    csvWriter.writeNext(new String[]{entry.getKey(), formatCurrency(entry.getValue())});
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Daily breakdown if available
            if (report.getDailyRevenue() != null && !report.getDailyRevenue().isEmpty()) {
                csvWriter.writeNext(new String[]{"Daily Revenue Breakdown"});
                csvWriter.writeNext(new String[]{"Date", "Revenue"});
                report.getDailyRevenue().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> csvWriter.writeNext(new String[]{
                                entry.getKey().format(DATE_FORMATTER),
                                formatCurrency(entry.getValue())
                        }));
            }
            
            return stringWriter.toString().getBytes();
            
        } catch (IOException e) {
            logger.error("Error exporting revenue report to CSV", e);
            throw new PetClinicException("Failed to export revenue report to CSV", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportDashboardMetricsToPdf(DashboardMetrics metrics) {
        logger.debug("Exporting dashboard metrics to PDF: {}", metrics);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Add title
            document.add(new Paragraph("Dashboard Metrics Report")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add generation timestamp
            document.add(new Paragraph("Generated: " + metrics.getGeneratedAt().format(DATETIME_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("\n"));
            
            // Today's metrics
            document.add(new Paragraph("Today's Metrics").setFontSize(14).setBold());
            Table todayTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            todayTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(todayTable, "Metric", "Value");
            addTableRow(todayTable, "Today's Appointments", String.valueOf(metrics.getTodayAppointments()));
            addTableRow(todayTable, "Completed Visits", String.valueOf(metrics.getTodayCompletedVisits()));
            addTableRow(todayTable, "Pending Visits", String.valueOf(metrics.getTodayPendingVisits()));
            addTableRow(todayTable, "Today's Revenue", formatCurrency(metrics.getTodayRevenue()));
            addTableRow(todayTable, "Completion Rate", String.format("%.1f%%", metrics.getTodayCompletionRate()));
            
            document.add(todayTable);
            document.add(new Paragraph("\n"));
            
            // Overall metrics
            document.add(new Paragraph("Overall Metrics").setFontSize(14).setBold());
            Table overallTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            overallTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(overallTable, "Metric", "Value");
            addTableRow(overallTable, "Total Active Pets", String.valueOf(metrics.getTotalActivePets()));
            addTableRow(overallTable, "Total Veterinarians", String.valueOf(metrics.getTotalVeterinarians()));
            addTableRow(overallTable, "Visits This Month", String.valueOf(metrics.getTotalVisitsThisMonth()));
            addTableRow(overallTable, "Monthly Revenue", formatCurrency(metrics.getMonthlyRevenue()));
            
            document.add(overallTable);
            document.add(new Paragraph("\n"));
            
            // Performance metrics
            document.add(new Paragraph("Performance Metrics").setFontSize(14).setBold());
            Table perfTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            perfTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(perfTable, "Metric", "Value");
            addTableRow(perfTable, "Veterinarian Utilization", String.format("%.1f%%", metrics.getVeterinarianUtilization()));
            addTableRow(perfTable, "Appointment Completion Rate", String.format("%.1f%%", metrics.getAppointmentCompletionRate()));
            addTableRow(perfTable, "Average Visit Duration", String.format("%.1f minutes", metrics.getAverageVisitDuration()));
            addTableRow(perfTable, "System Health", metrics.isHealthy() ? "Healthy" : "Needs Attention");
            
            document.add(perfTable);
            document.add(new Paragraph("\n"));
            
            // Upcoming appointments
            if (metrics.getUpcomingAppointments() != null && !metrics.getUpcomingAppointments().isEmpty()) {
                document.add(new Paragraph("Upcoming Appointments").setFontSize(14).setBold());
                Table appointmentTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 1}));
                appointmentTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(appointmentTable, "Pet", "Owner", "Veterinarian", "Time");
                for (DashboardMetrics.UpcomingAppointment appointment : metrics.getUpcomingAppointments()) {
                    addTableRow(appointmentTable, 
                            appointment.getPetName(),
                            appointment.getOwnerName(),
                            appointment.getVeterinarianName(),
                            appointment.getAppointmentTime().format(DATETIME_FORMATTER));
                }
                document.add(appointmentTable);
                document.add(new Paragraph("\n"));
            }
            
            // Alerts
            if (metrics.getAlerts() != null && !metrics.getAlerts().isEmpty()) {
                document.add(new Paragraph("Alerts").setFontSize(14).setBold());
                Table alertTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 3}));
                alertTable.setWidth(UnitValue.createPercentValue(100));
                
                addTableHeader(alertTable, "Severity", "Title", "Message");
                for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                    addTableRow(alertTable, alert.getSeverity(), alert.getTitle(), alert.getMessage());
                }
                document.add(alertTable);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting dashboard metrics to PDF", e);
            throw new PetClinicException("Failed to export dashboard metrics to PDF", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportDashboardMetricsToCSV(DashboardMetrics metrics) {
        logger.debug("Exporting dashboard metrics to CSV: {}", metrics);
        
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            
            // Header information
            csvWriter.writeNext(new String[]{"Dashboard Metrics Report"});
            csvWriter.writeNext(new String[]{"Generated", metrics.getGeneratedAt().format(DATETIME_FORMATTER)});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Today's metrics
            csvWriter.writeNext(new String[]{"Today's Metrics"});
            csvWriter.writeNext(new String[]{"Metric", "Value"});
            csvWriter.writeNext(new String[]{"Today's Appointments", String.valueOf(metrics.getTodayAppointments())});
            csvWriter.writeNext(new String[]{"Completed Visits", String.valueOf(metrics.getTodayCompletedVisits())});
            csvWriter.writeNext(new String[]{"Pending Visits", String.valueOf(metrics.getTodayPendingVisits())});
            csvWriter.writeNext(new String[]{"Today's Revenue", formatCurrency(metrics.getTodayRevenue())});
            csvWriter.writeNext(new String[]{"Completion Rate", String.format("%.1f%%", metrics.getTodayCompletionRate())});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Overall metrics
            csvWriter.writeNext(new String[]{"Overall Metrics"});
            csvWriter.writeNext(new String[]{"Metric", "Value"});
            csvWriter.writeNext(new String[]{"Total Active Pets", String.valueOf(metrics.getTotalActivePets())});
            csvWriter.writeNext(new String[]{"Total Veterinarians", String.valueOf(metrics.getTotalVeterinarians())});
            csvWriter.writeNext(new String[]{"Visits This Month", String.valueOf(metrics.getTotalVisitsThisMonth())});
            csvWriter.writeNext(new String[]{"Monthly Revenue", formatCurrency(metrics.getMonthlyRevenue())});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Performance metrics
            csvWriter.writeNext(new String[]{"Performance Metrics"});
            csvWriter.writeNext(new String[]{"Metric", "Value"});
            csvWriter.writeNext(new String[]{"Veterinarian Utilization", String.format("%.1f%%", metrics.getVeterinarianUtilization())});
            csvWriter.writeNext(new String[]{"Appointment Completion Rate", String.format("%.1f%%", metrics.getAppointmentCompletionRate())});
            csvWriter.writeNext(new String[]{"Average Visit Duration", String.format("%.1f minutes", metrics.getAverageVisitDuration())});
            csvWriter.writeNext(new String[]{"System Health", metrics.isHealthy() ? "Healthy" : "Needs Attention"});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Upcoming appointments
            if (metrics.getUpcomingAppointments() != null && !metrics.getUpcomingAppointments().isEmpty()) {
                csvWriter.writeNext(new String[]{"Upcoming Appointments"});
                csvWriter.writeNext(new String[]{"Pet", "Owner", "Veterinarian", "Time", "Visit Type", "Duration"});
                for (DashboardMetrics.UpcomingAppointment appointment : metrics.getUpcomingAppointments()) {
                    csvWriter.writeNext(new String[]{
                            appointment.getPetName(),
                            appointment.getOwnerName(),
                            appointment.getVeterinarianName(),
                            appointment.getAppointmentTime().format(DATETIME_FORMATTER),
                            appointment.getVisitType(),
                            String.valueOf(appointment.getDuration())
                    });
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Recent activities
            if (metrics.getRecentActivities() != null && !metrics.getRecentActivities().isEmpty()) {
                csvWriter.writeNext(new String[]{"Recent Activities"});
                csvWriter.writeNext(new String[]{"Type", "Description", "Timestamp"});
                for (DashboardMetrics.RecentActivity activity : metrics.getRecentActivities()) {
                    csvWriter.writeNext(new String[]{
                            activity.getType(),
                            activity.getDescription(),
                            activity.getTimestamp().format(DATETIME_FORMATTER)
                    });
                }
                csvWriter.writeNext(new String[]{}); // Empty line
            }
            
            // Alerts
            if (metrics.getAlerts() != null && !metrics.getAlerts().isEmpty()) {
                csvWriter.writeNext(new String[]{"Alerts"});
                csvWriter.writeNext(new String[]{"Severity", "Type", "Title", "Message", "Created"});
                for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                    csvWriter.writeNext(new String[]{
                            alert.getSeverity(),
                            alert.getType(),
                            alert.getTitle(),
                            alert.getMessage(),
                            alert.getCreatedAt().format(DATETIME_FORMATTER)
                    });
                }
            }
            
            return stringWriter.toString().getBytes();
            
        } catch (IOException e) {
            logger.error("Error exporting dashboard metrics to CSV", e);
            throw new PetClinicException("Failed to export dashboard metrics to CSV", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportMultipleVisitStatisticsToPdf(List<VisitStatisticsReport> reports, String title) {
        logger.debug("Exporting multiple visit statistics reports to PDF: {} reports", reports.size());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Add title
            document.add(new Paragraph(title != null ? title : "Multiple Visit Statistics Reports")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add generation timestamp
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("\n"));
            
            // Summary table
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1, 1}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(summaryTable, "Period", "Total", "Completed", "Scheduled", "Rate");
            
            for (VisitStatisticsReport report : reports) {
                addTableRow(summaryTable,
                        report.getDateRangeDescription(),
                        String.valueOf(report.getTotalVisits()),
                        String.valueOf(report.getCompletedVisits()),
                        String.valueOf(report.getScheduledVisits()),
                        String.format("%.1f%%", report.getCompletionRate()));
            }
            
            document.add(summaryTable);
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting multiple visit statistics to PDF", e);
            throw new PetClinicException("Failed to export multiple visit statistics to PDF", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportMultipleVisitStatisticsToCSV(List<VisitStatisticsReport> reports) {
        logger.debug("Exporting multiple visit statistics reports to CSV: {} reports", reports.size());
        
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            
            // Header information
            csvWriter.writeNext(new String[]{"Multiple Visit Statistics Reports"});
            csvWriter.writeNext(new String[]{"Generated", LocalDateTime.now().format(DATETIME_FORMATTER)});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Summary data
            csvWriter.writeNext(new String[]{"Period", "Total Visits", "Completed", "Scheduled", "Cancelled", "Completion Rate", "Avg/Day"});
            
            for (VisitStatisticsReport report : reports) {
                csvWriter.writeNext(new String[]{
                        report.getDateRangeDescription(),
                        String.valueOf(report.getTotalVisits()),
                        String.valueOf(report.getCompletedVisits()),
                        String.valueOf(report.getScheduledVisits()),
                        String.valueOf(report.getCancelledVisits()),
                        String.format("%.1f%%", report.getCompletionRate()),
                        String.format("%.1f", report.getAverageVisitsPerDay())
                });
            }
            
            return stringWriter.toString().getBytes();
            
        } catch (IOException e) {
            logger.error("Error exporting multiple visit statistics to CSV", e);
            throw new PetClinicException("Failed to export multiple visit statistics to CSV", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportMultipleRevenueReportsToPdf(List<RevenueReport> reports, String title) {
        logger.debug("Exporting multiple revenue reports to PDF: {} reports", reports.size());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Add title
            document.add(new Paragraph(title != null ? title : "Multiple Revenue Reports")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            
            // Add generation timestamp
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("\n"));
            
            // Summary table
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 1, 1, 2}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(summaryTable, "Period", "Revenue", "Paid", "Unpaid", "Avg/Visit");
            
            for (RevenueReport report : reports) {
                addTableRow(summaryTable,
                        report.getDateRangeDescription(),
                        formatCurrency(report.getTotalRevenue()),
                        String.valueOf(report.getTotalPaidVisits()),
                        String.valueOf(report.getTotalUnpaidVisits()),
                        formatCurrency(report.getAverageRevenuePerVisit()));
            }
            
            document.add(summaryTable);
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error exporting multiple revenue reports to PDF", e);
            throw new PetClinicException("Failed to export multiple revenue reports to PDF", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportMultipleRevenueReportsToCSV(List<RevenueReport> reports) {
        logger.debug("Exporting multiple revenue reports to CSV: {} reports", reports.size());
        
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            
            // Header information
            csvWriter.writeNext(new String[]{"Multiple Revenue Reports"});
            csvWriter.writeNext(new String[]{"Generated", LocalDateTime.now().format(DATETIME_FORMATTER)});
            csvWriter.writeNext(new String[]{}); // Empty line
            
            // Summary data
            csvWriter.writeNext(new String[]{"Period", "Total Revenue", "Paid Visits", "Unpaid Visits", "Avg Revenue/Visit", "Avg Revenue/Day", "Payment Rate"});
            
            for (RevenueReport report : reports) {
                csvWriter.writeNext(new String[]{
                        report.getDateRangeDescription(),
                        formatCurrency(report.getTotalRevenue()),
                        String.valueOf(report.getTotalPaidVisits()),
                        String.valueOf(report.getTotalUnpaidVisits()),
                        formatCurrency(report.getAverageRevenuePerVisit()),
                        formatCurrency(report.getAverageRevenuePerDay()),
                        String.format("%.1f%%", report.getPaymentRate())
                });
            }
            
            return stringWriter.toString().getBytes();
            
        } catch (IOException e) {
            logger.error("Error exporting multiple revenue reports to CSV", e);
            throw new PetClinicException("Failed to export multiple revenue reports to CSV", "EXPORT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public byte[] exportFilteredVisitStatisticsToPdf(ReportFilter filter) {
        logger.debug("Exporting filtered visit statistics to PDF with filter: {}", filter);
        
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        return exportVisitStatisticsToPdf(report);
    }
    
    @Override
    public byte[] exportFilteredVisitStatisticsToCSV(ReportFilter filter) {
        logger.debug("Exporting filtered visit statistics to CSV with filter: {}", filter);
        
        VisitStatisticsReport report = reportService.generateVisitStatistics(filter);
        return exportVisitStatisticsToCSV(report);
    }
    
    @Override
    public byte[] exportFilteredRevenueReportToPdf(ReportFilter filter) {
        logger.debug("Exporting filtered revenue report to PDF with filter: {}", filter);
        
        RevenueReport report = reportService.generateRevenueReport(filter);
        return exportRevenueReportToPdf(report);
    }
    
    @Override
    public byte[] exportFilteredRevenueReportToCSV(ReportFilter filter) {
        logger.debug("Exporting filtered revenue report to CSV with filter: {}", filter);
        
        RevenueReport report = reportService.generateRevenueReport(filter);
        return exportRevenueReportToCSV(report);
    }
    
    @Override
    public String getExportFilename(String reportType, String format, LocalDate startDate, LocalDate endDate) {
        StringBuilder filename = new StringBuilder();
        
        // Add report type
        filename.append(reportType.toLowerCase().replace(" ", "-"));
        
        // Add date range if provided
        if (startDate != null && endDate != null) {
            filename.append("_").append(startDate.format(DATE_FORMATTER));
            if (!startDate.equals(endDate)) {
                filename.append("_to_").append(endDate.format(DATE_FORMATTER));
            }
        } else if (startDate != null) {
            filename.append("_from_").append(startDate.format(DATE_FORMATTER));
        } else if (endDate != null) {
            filename.append("_until_").append(endDate.format(DATE_FORMATTER));
        } else {
            filename.append("_").append(LocalDate.now().format(DATE_FORMATTER));
        }
        
        // Add format extension
        filename.append(".").append(format.toLowerCase());
        
        return filename.toString();
    }
    
    @Override
    public String getMimeType(String format) {
        switch (format.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "csv":
                return "text/csv";
            default:
                return "application/octet-stream";
        }
    }
    
    @Override
    public boolean validateExportParameters(String reportType, String format) {
        if (reportType == null || reportType.trim().isEmpty()) {
            return false;
        }
        
        if (format == null || format.trim().isEmpty()) {
            return false;
        }
        
        // Validate report type
        List<String> validReportTypes = Arrays.asList(
                "visit-statistics", "revenue", "dashboard", 
                "visit-statistics-by-veterinarian", "visit-statistics-by-type", "visit-statistics-by-species",
                "revenue-by-veterinarian", "revenue-by-species", "revenue-by-visit-type"
        );
        
        if (!validReportTypes.contains(reportType.toLowerCase())) {
            return false;
        }
        
        // Validate format
        List<String> validFormats = Arrays.asList("pdf", "csv");
        return validFormats.contains(format.toLowerCase());
    }
    
    // Helper methods
    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold());
            cell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
            table.addHeaderCell(cell);
        }
    }
    
    private void addTableRow(Table table, String... values) {
        for (String value : values) {
            table.addCell(new Cell().add(new Paragraph(value != null ? value : "")));
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }
}