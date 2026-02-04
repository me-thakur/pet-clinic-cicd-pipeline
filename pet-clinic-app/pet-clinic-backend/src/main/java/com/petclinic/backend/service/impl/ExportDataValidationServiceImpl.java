package com.petclinic.backend.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.opencsv.CSVReader;
import com.petclinic.backend.dto.DashboardMetrics;
import com.petclinic.backend.dto.ReportFilter;
import com.petclinic.backend.dto.RevenueReport;
import com.petclinic.backend.dto.VisitStatisticsReport;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.ExportDataValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Implementation of ExportDataValidationService
 * Provides comprehensive validation for export data content and consistency
 * 
 * Validates: Requirements 6.1, 6.2, 6.3
 */
@Service
public class ExportDataValidationServiceImpl implements ExportDataValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportDataValidationServiceImpl.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern CSV_DELIMITER_PATTERN = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    
    @Override
    public ValidationResult validateVisitStatisticsData(VisitStatisticsReport report, ReportFilter filter) {
        logger.debug("Validating visit statistics data: {}", report);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate required fields
        if (report == null) {
            errors.add("Visit statistics report is null");
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validate basic metrics
        if (report.getTotalVisits() < 0) {
            errors.add("Total visits cannot be negative: " + report.getTotalVisits());
        }
        
        if (report.getCompletedVisits() < 0) {
            errors.add("Completed visits cannot be negative: " + report.getCompletedVisits());
        }
        
        if (report.getScheduledVisits() < 0) {
            errors.add("Scheduled visits cannot be negative: " + report.getScheduledVisits());
        }
        
        if (report.getCancelledVisits() < 0) {
            errors.add("Cancelled visits cannot be negative: " + report.getCancelledVisits());
        }
        
        // Validate totals consistency
        long calculatedTotal = report.getCompletedVisits() + report.getScheduledVisits() + report.getCancelledVisits();
        if (calculatedTotal != report.getTotalVisits()) {
            errors.add(String.format("Visit totals inconsistent: completed(%d) + scheduled(%d) + cancelled(%d) = %d, but total is %d",
                    report.getCompletedVisits(), report.getScheduledVisits(), report.getCancelledVisits(), 
                    calculatedTotal, report.getTotalVisits()));
        }
        
        // Validate completion rate
        if (report.getCompletionRate() < 0 || report.getCompletionRate() > 100) {
            errors.add("Completion rate must be between 0 and 100: " + report.getCompletionRate());
        }
        
        double expectedCompletionRate = report.getTotalVisits() > 0 ? 
                (double) report.getCompletedVisits() / report.getTotalVisits() * 100.0 : 0.0;
        if (Math.abs(report.getCompletionRate() - expectedCompletionRate) > 0.1) {
            errors.add(String.format("Completion rate calculation incorrect: expected %.1f%%, got %.1f%%",
                    expectedCompletionRate, report.getCompletionRate()));
        }
        
        // Validate average visits per day
        if (report.getAverageVisitsPerDay() < 0) {
            errors.add("Average visits per day cannot be negative: " + report.getAverageVisitsPerDay());
        }
        
        // Validate date range
        if (report.getStartDate() != null && report.getEndDate() != null) {
            if (report.getStartDate().isAfter(report.getEndDate())) {
                errors.add("Start date cannot be after end date: " + report.getStartDate() + " > " + report.getEndDate());
            }
            
            // Validate average calculation
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(report.getStartDate(), report.getEndDate()) + 1;
            double expectedAverage = daysBetween > 0 ? (double) report.getTotalVisits() / daysBetween : 0.0;
            if (Math.abs(report.getAverageVisitsPerDay() - expectedAverage) > 0.1) {
                errors.add(String.format("Average visits per day calculation incorrect: expected %.1f, got %.1f",
                        expectedAverage, report.getAverageVisitsPerDay()));
            }
        }
        
        // Validate breakdown data consistency
        if (report.getVisitsByVeterinarian() != null) {
            long vetTotal = report.getVisitsByVeterinarian().values().stream().mapToLong(Long::longValue).sum();
            if (vetTotal != report.getTotalVisits() && vetTotal > 0) {
                warnings.add(String.format("Visits by veterinarian total (%d) doesn't match total visits (%d)", 
                        vetTotal, report.getTotalVisits()));
            }
        }
        
        if (report.getVisitsByType() != null) {
            long typeTotal = report.getVisitsByType().values().stream().mapToLong(Long::longValue).sum();
            if (typeTotal != report.getTotalVisits() && typeTotal > 0) {
                warnings.add(String.format("Visits by type total (%d) doesn't match total visits (%d)", 
                        typeTotal, report.getTotalVisits()));
            }
        }
        
        if (report.getVisitsBySpecies() != null) {
            long speciesTotal = report.getVisitsBySpecies().values().stream().mapToLong(Long::longValue).sum();
            if (speciesTotal != report.getTotalVisits() && speciesTotal > 0) {
                warnings.add(String.format("Visits by species total (%d) doesn't match total visits (%d)", 
                        speciesTotal, report.getTotalVisits()));
            }
        }
        
        // Validate filtering consistency
        if (filter != null) {
            ValidationResult filterResult = validateFilteringConsistency(report, filter);
            errors.addAll(filterResult.getErrors());
            warnings.addAll(filterResult.getWarnings());
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public ValidationResult validateRevenueReportData(RevenueReport report, ReportFilter filter) {
        logger.debug("Validating revenue report data: {}", report);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate required fields
        if (report == null) {
            errors.add("Revenue report is null");
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validate basic metrics
        if (report.getTotalRevenue() == null) {
            errors.add("Total revenue cannot be null");
        } else if (report.getTotalRevenue().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total revenue cannot be negative: " + report.getTotalRevenue());
        }
        
        if (report.getTotalPaidVisits() < 0) {
            errors.add("Total paid visits cannot be negative: " + report.getTotalPaidVisits());
        }
        
        if (report.getTotalUnpaidVisits() < 0) {
            errors.add("Total unpaid visits cannot be negative: " + report.getTotalUnpaidVisits());
        }
        
        // Validate payment rate
        double expectedPaymentRate = report.getTotalVisits() > 0 ? 
                (double) report.getTotalPaidVisits() / report.getTotalVisits() * 100.0 : 0.0;
        if (Math.abs(report.getPaymentRate() - expectedPaymentRate) > 0.1) {
            errors.add(String.format("Payment rate calculation incorrect: expected %.1f%%, got %.1f%%",
                    expectedPaymentRate, report.getPaymentRate()));
        }
        
        // Validate average revenue calculations
        if (report.getAverageRevenuePerVisit() == null) {
            errors.add("Average revenue per visit cannot be null");
        } else if (report.getTotalPaidVisits() > 0 && report.getTotalRevenue() != null) {
            BigDecimal expectedAvgPerVisit = report.getTotalRevenue().divide(
                    BigDecimal.valueOf(report.getTotalPaidVisits()), 2, BigDecimal.ROUND_HALF_UP);
            if (report.getAverageRevenuePerVisit().subtract(expectedAvgPerVisit).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                errors.add(String.format("Average revenue per visit calculation incorrect: expected %s, got %s",
                        expectedAvgPerVisit, report.getAverageRevenuePerVisit()));
            }
        }
        
        // Validate date range
        if (report.getStartDate() != null && report.getEndDate() != null) {
            if (report.getStartDate().isAfter(report.getEndDate())) {
                errors.add("Start date cannot be after end date: " + report.getStartDate() + " > " + report.getEndDate());
            }
        }
        
        // Validate breakdown data consistency
        if (report.getRevenueByVeterinarian() != null) {
            BigDecimal vetTotal = report.getRevenueByVeterinarian().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (report.getTotalRevenue() != null && vetTotal.subtract(report.getTotalRevenue()).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                warnings.add(String.format("Revenue by veterinarian total (%s) doesn't match total revenue (%s)", 
                        vetTotal, report.getTotalRevenue()));
            }
        }
        
        if (report.getRevenueBySpecies() != null) {
            BigDecimal speciesTotal = report.getRevenueBySpecies().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (report.getTotalRevenue() != null && speciesTotal.subtract(report.getTotalRevenue()).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                warnings.add(String.format("Revenue by species total (%s) doesn't match total revenue (%s)", 
                        speciesTotal, report.getTotalRevenue()));
            }
        }
        
        if (report.getRevenueByVisitType() != null) {
            BigDecimal typeTotal = report.getRevenueByVisitType().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (report.getTotalRevenue() != null && typeTotal.subtract(report.getTotalRevenue()).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                warnings.add(String.format("Revenue by visit type total (%s) doesn't match total revenue (%s)", 
                        typeTotal, report.getTotalRevenue()));
            }
        }
        
        // Validate trend data if present
        if (report.getTrendData() != null) {
            RevenueReport.TrendData trend = report.getTrendData();
            if (trend.getPreviousPeriodRevenue() != null && trend.getPreviousPeriodRevenue().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Previous period revenue cannot be negative: " + trend.getPreviousPeriodRevenue());
            }
            
            if (Math.abs(trend.getGrowthRate()) > 1000) {
                warnings.add("Growth rate seems unusually high: " + trend.getGrowthRate() + "%");
            }
        }
        
        // Validate filtering consistency
        if (filter != null) {
            ValidationResult filterResult = validateFilteringConsistency(report, filter);
            errors.addAll(filterResult.getErrors());
            warnings.addAll(filterResult.getWarnings());
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public ValidationResult validateDashboardMetricsData(DashboardMetrics metrics) {
        logger.debug("Validating dashboard metrics data: {}", metrics);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate required fields
        if (metrics == null) {
            errors.add("Dashboard metrics is null");
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validate basic metrics
        if (metrics.getTodayAppointments() < 0) {
            errors.add("Today's appointments cannot be negative: " + metrics.getTodayAppointments());
        }
        
        if (metrics.getTodayCompletedVisits() < 0) {
            errors.add("Today's completed visits cannot be negative: " + metrics.getTodayCompletedVisits());
        }
        
        if (metrics.getTodayPendingVisits() < 0) {
            errors.add("Today's pending visits cannot be negative: " + metrics.getTodayPendingVisits());
        }
        
        if (metrics.getTotalActivePets() < 0) {
            errors.add("Total active pets cannot be negative: " + metrics.getTotalActivePets());
        }
        
        if (metrics.getTotalVeterinarians() < 0) {
            errors.add("Total veterinarians cannot be negative: " + metrics.getTotalVeterinarians());
        }
        
        // Validate revenue fields
        if (metrics.getTodayRevenue() == null) {
            errors.add("Today's revenue cannot be null");
        } else if (metrics.getTodayRevenue().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Today's revenue cannot be negative: " + metrics.getTodayRevenue());
        }
        
        if (metrics.getMonthlyRevenue() == null) {
            errors.add("Monthly revenue cannot be null");
        } else if (metrics.getMonthlyRevenue().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Monthly revenue cannot be negative: " + metrics.getMonthlyRevenue());
        }
        
        // Validate percentages
        if (metrics.getVeterinarianUtilization() < 0 || metrics.getVeterinarianUtilization() > 100) {
            errors.add("Veterinarian utilization must be between 0 and 100: " + metrics.getVeterinarianUtilization());
        }
        
        if (metrics.getAppointmentCompletionRate() < 0 || metrics.getAppointmentCompletionRate() > 100) {
            errors.add("Appointment completion rate must be between 0 and 100: " + metrics.getAppointmentCompletionRate());
        }
        
        // Validate today's completion rate calculation
        double expectedTodayCompletionRate = metrics.getTodayAppointments() > 0 ? 
                (double) metrics.getTodayCompletedVisits() / metrics.getTodayAppointments() * 100.0 : 0.0;
        if (Math.abs(metrics.getTodayCompletionRate() - expectedTodayCompletionRate) > 0.1) {
            errors.add(String.format("Today's completion rate calculation incorrect: expected %.1f%%, got %.1f%%",
                    expectedTodayCompletionRate, metrics.getTodayCompletionRate()));
        }
        
        // Validate visit duration
        if (metrics.getAverageVisitDuration() < 0) {
            errors.add("Average visit duration cannot be negative: " + metrics.getAverageVisitDuration());
        }
        
        if (metrics.getAverageVisitDuration() > 480) { // 8 hours seems excessive
            warnings.add("Average visit duration seems unusually high: " + metrics.getAverageVisitDuration() + " minutes");
        }
        
        // Validate generation timestamp
        if (metrics.getGeneratedAt() == null) {
            errors.add("Generation timestamp cannot be null");
        }
        
        // Validate upcoming appointments
        if (metrics.getUpcomingAppointments() != null) {
            for (DashboardMetrics.UpcomingAppointment appointment : metrics.getUpcomingAppointments()) {
                if (appointment.getPetName() == null || appointment.getPetName().trim().isEmpty()) {
                    warnings.add("Upcoming appointment missing pet name");
                }
                if (appointment.getOwnerName() == null || appointment.getOwnerName().trim().isEmpty()) {
                    warnings.add("Upcoming appointment missing owner name");
                }
                if (appointment.getVeterinarianName() == null || appointment.getVeterinarianName().trim().isEmpty()) {
                    warnings.add("Upcoming appointment missing veterinarian name");
                }
                if (appointment.getAppointmentTime() == null) {
                    errors.add("Upcoming appointment missing appointment time");
                }
            }
        }
        
        // Validate alerts
        if (metrics.getAlerts() != null) {
            for (DashboardMetrics.Alert alert : metrics.getAlerts()) {
                if (alert.getTitle() == null || alert.getTitle().trim().isEmpty()) {
                    warnings.add("Alert missing title");
                }
                if (alert.getMessage() == null || alert.getMessage().trim().isEmpty()) {
                    warnings.add("Alert missing message");
                }
                if (alert.getSeverity() == null || alert.getSeverity().trim().isEmpty()) {
                    warnings.add("Alert missing severity");
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public ValidationResult validateCSVFormatting(byte[] csvContent, List<String> expectedHeaders, String reportType) {
        logger.debug("Validating CSV formatting for report type: {}", reportType);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (csvContent == null || csvContent.length == 0) {
            errors.add("CSV content is empty");
            return new ValidationResult(false, errors, warnings);
        }
        
        try {
            String csvString = new String(csvContent);
            CSVReader reader = new CSVReader(new StringReader(csvString));
            
            List<String[]> records = reader.readAll();
            reader.close();
            
            if (records.isEmpty()) {
                errors.add("CSV contains no records");
                return new ValidationResult(false, errors, warnings);
            }
            
            // Find the header row (may not be the first row due to title/metadata)
            String[] headers = null;
            int headerRowIndex = -1;
            
            for (int i = 0; i < records.size(); i++) {
                String[] row = records.get(i);
                if (row.length > 1 && containsExpectedHeaders(row, expectedHeaders)) {
                    headers = row;
                    headerRowIndex = i;
                    break;
                }
            }
            
            if (headers == null) {
                errors.add("Could not find header row with expected headers: " + expectedHeaders);
                return new ValidationResult(false, errors, warnings);
            }
            
            // Validate header completeness
            List<String> missingHeaders = new ArrayList<>();
            for (String expectedHeader : expectedHeaders) {
                boolean found = false;
                for (String actualHeader : headers) {
                    if (actualHeader != null && actualHeader.trim().equalsIgnoreCase(expectedHeader.trim())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missingHeaders.add(expectedHeader);
                }
            }
            
            if (!missingHeaders.isEmpty()) {
                errors.add("Missing expected headers: " + missingHeaders);
            }
            
            // Validate data rows
            int dataRowCount = 0;
            for (int i = headerRowIndex + 1; i < records.size(); i++) {
                String[] row = records.get(i);
                
                // Skip empty rows
                if (isEmptyRow(row)) {
                    continue;
                }
                
                dataRowCount++;
                
                // Validate row length matches header length
                if (row.length != headers.length) {
                    warnings.add(String.format("Row %d has %d columns, expected %d", i + 1, row.length, headers.length));
                }
                
                // Validate data types based on report type
                validateCSVDataTypes(row, headers, reportType, i + 1, errors, warnings);
            }
            
            if (dataRowCount == 0) {
                warnings.add("CSV contains headers but no data rows");
            }
            
            // Validate CSV structure
            validateCSVStructure(csvString, errors, warnings);
            
        } catch (IOException e) {
            errors.add("Failed to parse CSV content: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Unexpected error validating CSV: " + e.getMessage());
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public ValidationResult validatePDFContent(byte[] pdfContent, String reportType, List<String> expectedSections) {
        logger.debug("Validating PDF content for report type: {}", reportType);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (pdfContent == null || pdfContent.length == 0) {
            errors.add("PDF content is empty");
            return new ValidationResult(false, errors, warnings);
        }
        
        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfContent));
            PdfDocument pdfDoc = new PdfDocument(reader);
            
            StringBuilder fullText = new StringBuilder();
            int pageCount = pdfDoc.getNumberOfPages();
            
            if (pageCount == 0) {
                errors.add("PDF contains no pages");
                pdfDoc.close();
                return new ValidationResult(false, errors, warnings);
            }
            
            // Extract text from all pages
            for (int i = 1; i <= pageCount; i++) {
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                fullText.append(pageText).append("\n");
            }
            
            pdfDoc.close();
            
            String pdfText = fullText.toString();
            
            // Validate expected sections are present
            List<String> missingSections = new ArrayList<>();
            for (String section : expectedSections) {
                if (!pdfText.toLowerCase().contains(section.toLowerCase())) {
                    missingSections.add(section);
                }
            }
            
            if (!missingSections.isEmpty()) {
                errors.add("Missing expected sections in PDF: " + missingSections);
            }
            
            // Validate report-specific content
            validatePDFReportContent(pdfText, reportType, errors, warnings);
            
            // Validate PDF structure
            if (pdfText.trim().isEmpty()) {
                errors.add("PDF appears to contain no readable text");
            }
            
            if (pdfText.length() < 100) {
                warnings.add("PDF content seems unusually short: " + pdfText.length() + " characters");
            }
            
        } catch (IOException e) {
            errors.add("Failed to read PDF content: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Unexpected error validating PDF: " + e.getMessage());
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public ValidationResult validateCrossFormatConsistency(byte[] pdfContent, byte[] csvContent, String reportType) {
        logger.debug("Validating cross-format consistency for report type: {}", reportType);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Validate input parameters
            if (pdfContent == null || pdfContent.length == 0) {
                errors.add("PDF content is null or empty");
                return new ValidationResult(false, errors, warnings);
            }
            
            if (csvContent == null || csvContent.length == 0) {
                errors.add("CSV content is null or empty");
                return new ValidationResult(false, errors, warnings);
            }
            
            // Extract comprehensive metrics from both formats
            Map<String, String> pdfMetrics = extractMetricsFromPDF(pdfContent);
            Map<String, String> csvMetrics = extractMetricsFromCSV(csvContent, reportType);
            
            // Extract detailed data for comprehensive comparison
            Map<String, Object> pdfData = extractDetailedDataFromPDF(pdfContent, reportType);
            Map<String, Object> csvData = extractDetailedDataFromCSV(csvContent, reportType);
            
            // Validate basic metric consistency
            validateBasicMetricConsistency(pdfMetrics, csvMetrics, errors, warnings);
            
            // Validate detailed data consistency
            validateDetailedDataConsistency(pdfData, csvData, reportType, errors, warnings);
            
            // Validate calculations and aggregations
            validateCalculationConsistency(pdfMetrics, csvMetrics, pdfData, csvData, reportType, errors, warnings);
            
            // Validate totals consistency
            validateTotalsConsistency(pdfMetrics, csvMetrics, pdfData, csvData, reportType, errors, warnings);
            
            // Validate breakdown data consistency (by veterinarian, species, type)
            validateBreakdownConsistency(pdfData, csvData, reportType, errors, warnings);
            
            // Validate date range consistency
            validateDateRangeConsistency(pdfData, csvData, errors, warnings);
            
            // Check for data completeness across formats
            validateDataCompleteness(pdfData, csvData, reportType, errors, warnings);
            
        } catch (Exception e) {
            errors.add("Failed to compare formats: " + e.getMessage());
            logger.error("Cross-format consistency validation failed", e);
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public ValidationResult validateFilteringConsistency(Object reportData, ReportFilter filter) {
        logger.debug("Validating filtering consistency for filter: {}", filter);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (filter == null) {
            return new ValidationResult(true, errors, warnings);
        }
        
        // Validate date range consistency
        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            LocalDate reportStartDate = null;
            LocalDate reportEndDate = null;
            
            if (reportData instanceof VisitStatisticsReport) {
                VisitStatisticsReport report = (VisitStatisticsReport) reportData;
                reportStartDate = report.getStartDate();
                reportEndDate = report.getEndDate();
            } else if (reportData instanceof RevenueReport) {
                RevenueReport report = (RevenueReport) reportData;
                reportStartDate = report.getStartDate();
                reportEndDate = report.getEndDate();
            }
            
            if (filter.getStartDate() != null && reportStartDate != null && 
                !filter.getStartDate().equals(reportStartDate)) {
                errors.add(String.format("Start date mismatch: filter=%s, report=%s", 
                        filter.getStartDate(), reportStartDate));
            }
            
            if (filter.getEndDate() != null && reportEndDate != null && 
                !filter.getEndDate().equals(reportEndDate)) {
                errors.add(String.format("End date mismatch: filter=%s, report=%s", 
                        filter.getEndDate(), reportEndDate));
            }
        }
        
        // Validate veterinarian filter consistency
        if (filter.getVeterinarianId() != null && reportData instanceof VisitStatisticsReport) {
            VisitStatisticsReport report = (VisitStatisticsReport) reportData;
            if (report.getVisitsByVeterinarian() != null && report.getVisitsByVeterinarian().size() > 1) {
                warnings.add("Veterinarian filter applied but report contains multiple veterinarians");
            }
        }
        
        // Validate species filter consistency
        if (filter.getSpecies() != null && !filter.getSpecies().isEmpty()) {
            if (reportData instanceof VisitStatisticsReport) {
                VisitStatisticsReport report = (VisitStatisticsReport) reportData;
                if (report.getVisitsBySpecies() != null) {
                    Set<String> reportSpecies = report.getVisitsBySpecies().keySet();
                    boolean hasFilteredSpecies = filter.getSpecies().stream()
                            .anyMatch(species -> reportSpecies.stream()
                                    .anyMatch(reportSpecies1 -> reportSpecies1.equalsIgnoreCase(species)));
                    
                    if (!hasFilteredSpecies && !reportSpecies.isEmpty()) {
                        warnings.add("Species filter applied but report contains different species: " + reportSpecies);
                    }
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    @Override
    public List<String> getExpectedDataFields(String reportType) {
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                return Arrays.asList(
                    "totalVisits", "completedVisits", "scheduledVisits", "cancelledVisits",
                    "completionRate", "averageVisitsPerDay", "visitsByVeterinarian", 
                    "visitsByType", "visitsBySpecies", "visitsByDate", "dateRangeDescription"
                );
            
            case "revenue":
                return Arrays.asList(
                    "totalRevenue", "totalPaidVisits", "totalUnpaidVisits", "averageRevenuePerVisit",
                    "averageRevenuePerDay", "paymentRate", "revenueByVeterinarian", 
                    "revenueBySpecies", "revenueByVisitType", "dailyRevenue", "trendData"
                );
            
            case "dashboard":
                return Arrays.asList(
                    "todayAppointments", "todayCompletedVisits", "todayPendingVisits", "todayRevenue",
                    "totalActivePets", "totalVeterinarians", "totalVisitsThisMonth", "monthlyRevenue",
                    "veterinarianUtilization", "appointmentCompletionRate", "averageVisitDuration",
                    "upcomingAppointments", "recentActivities", "alerts", "generatedAt"
                );
            
            default:
                return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getExpectedCSVHeaders(String reportType) {
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                return Arrays.asList(
                    "Metric", "Value"
                );
            
            case "revenue":
                return Arrays.asList(
                    "Metric", "Value"
                );
            
            case "dashboard":
                return Arrays.asList(
                    "Metric", "Value"
                );
            
            default:
                return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getExpectedPDFSections(String reportType) {
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                return Arrays.asList(
                    "Visit Statistics Report", "Summary Statistics", "Completion Rate",
                    "Average Visits per Day", "Visits by Veterinarian", "Visits by Type", 
                    "Visits by Species", "Daily Breakdown"
                );
            
            case "revenue":
                return Arrays.asList(
                    "Revenue Report", "Summary Statistics", "Trend Analysis",
                    "Revenue by Veterinarian", "Revenue by Species", "Revenue by Visit Type",
                    "Daily Revenue Breakdown"
                );
            
            case "dashboard":
                return Arrays.asList(
                    "Dashboard Metrics Report", "Today's Metrics", "Overall Metrics",
                    "Performance Metrics", "Upcoming Appointments", "Recent Activities", "Alerts"
                );
            
            default:
                return new ArrayList<>();
        }
    }
    
    // Helper methods for cross-format consistency validation
    
    /**
     * Validates basic metric consistency between PDF and CSV formats
     */
    private void validateBasicMetricConsistency(Map<String, String> pdfMetrics, Map<String, String> csvMetrics, 
                                              List<String> errors, List<String> warnings) {
        // Compare common metrics
        Set<String> commonKeys = new HashSet<>(pdfMetrics.keySet());
        commonKeys.retainAll(csvMetrics.keySet());
        
        for (String key : commonKeys) {
            String pdfValue = pdfMetrics.get(key);
            String csvValue = csvMetrics.get(key);
            
            if (!areValuesConsistent(pdfValue, csvValue)) {
                errors.add(String.format("Inconsistent %s: PDF='%s', CSV='%s'", key, pdfValue, csvValue));
            }
        }
        
        // Check for missing metrics
        Set<String> pdfOnlyKeys = new HashSet<>(pdfMetrics.keySet());
        pdfOnlyKeys.removeAll(csvMetrics.keySet());
        
        Set<String> csvOnlyKeys = new HashSet<>(csvMetrics.keySet());
        csvOnlyKeys.removeAll(pdfMetrics.keySet());
        
        if (!pdfOnlyKeys.isEmpty()) {
            warnings.add("Metrics only in PDF: " + pdfOnlyKeys);
        }
        
        if (!csvOnlyKeys.isEmpty()) {
            warnings.add("Metrics only in CSV: " + csvOnlyKeys);
        }
    }
    
    /**
     * Validates detailed data consistency between formats
     */
    private void validateDetailedDataConsistency(Map<String, Object> pdfData, Map<String, Object> csvData, 
                                               String reportType, List<String> errors, List<String> warnings) {
        // Compare data tables/lists
        for (String key : pdfData.keySet()) {
            if (csvData.containsKey(key)) {
                Object pdfValue = pdfData.get(key);
                Object csvValue = csvData.get(key);
                
                if (pdfValue instanceof Map && csvValue instanceof Map) {
                    validateMapConsistency(key, (Map<String, Object>) pdfValue, (Map<String, Object>) csvValue, errors, warnings);
                } else if (pdfValue instanceof List && csvValue instanceof List) {
                    validateListConsistency(key, (List<?>) pdfValue, (List<?>) csvValue, errors, warnings);
                } else if (!Objects.equals(pdfValue, csvValue)) {
                    if (!areObjectValuesConsistent(pdfValue, csvValue)) {
                        errors.add(String.format("Inconsistent %s data: PDF='%s', CSV='%s'", key, pdfValue, csvValue));
                    }
                }
            }
        }
    }
    
    /**
     * Validates calculation consistency between formats
     */
    private void validateCalculationConsistency(Map<String, String> pdfMetrics, Map<String, String> csvMetrics,
                                              Map<String, Object> pdfData, Map<String, Object> csvData,
                                              String reportType, List<String> errors, List<String> warnings) {
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                validateVisitCalculations(pdfMetrics, csvMetrics, pdfData, csvData, errors, warnings);
                break;
            case "revenue":
                validateRevenueCalculations(pdfMetrics, csvMetrics, pdfData, csvData, errors, warnings);
                break;
            case "dashboard":
                validateDashboardCalculations(pdfMetrics, csvMetrics, pdfData, csvData, errors, warnings);
                break;
        }
    }
    
    /**
     * Validates totals consistency between formats
     */
    private void validateTotalsConsistency(Map<String, String> pdfMetrics, Map<String, String> csvMetrics,
                                         Map<String, Object> pdfData, Map<String, Object> csvData,
                                         String reportType, List<String> errors, List<String> warnings) {
        // Validate that breakdown totals match overall totals
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                validateVisitTotals(pdfData, csvData, errors, warnings);
                break;
            case "revenue":
                validateRevenueTotals(pdfData, csvData, errors, warnings);
                break;
            case "dashboard":
                validateDashboardTotals(pdfData, csvData, errors, warnings);
                break;
        }
    }
    
    /**
     * Validates breakdown data consistency (by veterinarian, species, type)
     */
    private void validateBreakdownConsistency(Map<String, Object> pdfData, Map<String, Object> csvData,
                                            String reportType, List<String> errors, List<String> warnings) {
        String[] breakdownKeys = getBreakdownKeys(reportType);
        
        for (String key : breakdownKeys) {
            if (pdfData.containsKey(key) && csvData.containsKey(key)) {
                Object pdfBreakdown = pdfData.get(key);
                Object csvBreakdown = csvData.get(key);
                
                if (pdfBreakdown instanceof Map && csvBreakdown instanceof Map) {
                    validateMapConsistency(key, (Map<String, Object>) pdfBreakdown, 
                                         (Map<String, Object>) csvBreakdown, errors, warnings);
                }
            } else if (pdfData.containsKey(key) || csvData.containsKey(key)) {
                warnings.add(String.format("Breakdown data '%s' missing in one format", key));
            }
        }
    }
    
    /**
     * Validates date range consistency between formats
     */
    private void validateDateRangeConsistency(Map<String, Object> pdfData, Map<String, Object> csvData,
                                            List<String> errors, List<String> warnings) {
        String[] dateKeys = {"startDate", "endDate", "dateRange", "reportPeriod"};
        
        for (String key : dateKeys) {
            if (pdfData.containsKey(key) && csvData.containsKey(key)) {
                Object pdfDate = pdfData.get(key);
                Object csvDate = csvData.get(key);
                
                if (!areObjectValuesConsistent(pdfDate, csvDate)) {
                    errors.add(String.format("Inconsistent %s: PDF='%s', CSV='%s'", key, pdfDate, csvDate));
                }
            }
        }
    }
    
    /**
     * Validates data completeness across formats
     */
    private void validateDataCompleteness(Map<String, Object> pdfData, Map<String, Object> csvData,
                                        String reportType, List<String> errors, List<String> warnings) {
        List<String> expectedFields = getExpectedDataFields(reportType);
        
        for (String field : expectedFields) {
            boolean inPdf = pdfData.containsKey(field) && pdfData.get(field) != null;
            boolean inCsv = csvData.containsKey(field) && csvData.get(field) != null;
            
            if (inPdf && !inCsv) {
                warnings.add(String.format("Field '%s' present in PDF but missing in CSV", field));
            } else if (!inPdf && inCsv) {
                warnings.add(String.format("Field '%s' present in CSV but missing in PDF", field));
            } else if (!inPdf && !inCsv) {
                warnings.add(String.format("Field '%s' missing in both formats", field));
            }
        }
    }
    
    /**
     * Extracts detailed data from PDF content for comprehensive comparison
     */
    private Map<String, Object> extractDetailedDataFromPDF(byte[] pdfContent, String reportType) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            String text;
            
            // Try to parse as actual PDF first
            try {
                PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfContent));
                PdfDocument pdfDoc = new PdfDocument(reader);
                
                StringBuilder fullText = new StringBuilder();
                for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                    fullText.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i)));
                }
                pdfDoc.close();
                text = fullText.toString();
            } catch (Exception pdfException) {
                // If PDF parsing fails, treat as plain text (for testing purposes)
                text = new String(pdfContent);
                logger.debug("PDF parsing failed, treating as plain text: {}", pdfException.getMessage());
            }
            
            // Extract breakdown data based on report type
            switch (reportType.toLowerCase()) {
                case "visits":
                case "visit-statistics":
                    extractVisitBreakdownFromPDF(text, data);
                    break;
                case "revenue":
                    extractRevenueBreakdownFromPDF(text, data);
                    break;
                case "dashboard":
                    extractDashboardDataFromPDF(text, data);
                    break;
            }
            
            // Extract common data elements
            extractCommonDataFromPDF(text, data);
            
        } catch (Exception e) {
            logger.warn("Failed to extract detailed data from PDF: {}", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * Extracts detailed data from CSV content for comprehensive comparison
     */
    private Map<String, Object> extractDetailedDataFromCSV(byte[] csvContent, String reportType) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            String csvString = new String(csvContent);
            CSVReader reader = new CSVReader(new StringReader(csvString));
            List<String[]> records = reader.readAll();
            reader.close();
            
            // Extract breakdown data based on report type
            switch (reportType.toLowerCase()) {
                case "visits":
                case "visit-statistics":
                    extractVisitBreakdownFromCSV(records, data);
                    break;
                case "revenue":
                    extractRevenueBreakdownFromCSV(records, data);
                    break;
                case "dashboard":
                    extractDashboardDataFromCSV(records, data);
                    break;
            }
            
            // Extract common data elements
            extractCommonDataFromCSV(records, data);
            
        } catch (Exception e) {
            logger.warn("Failed to extract detailed data from CSV: {}", e.getMessage());
        }
        
        return data;
    }
    
    // Helper methods
    
    private void validateMapConsistency(String key, Map<String, Object> pdfMap, Map<String, Object> csvMap,
                                      List<String> errors, List<String> warnings) {
        for (String mapKey : pdfMap.keySet()) {
            if (csvMap.containsKey(mapKey)) {
                Object pdfValue = pdfMap.get(mapKey);
                Object csvValue = csvMap.get(mapKey);
                
                if (!areObjectValuesConsistent(pdfValue, csvValue)) {
                    errors.add(String.format("Inconsistent %s[%s]: PDF='%s', CSV='%s'", key, mapKey, pdfValue, csvValue));
                }
            } else {
                warnings.add(String.format("%s[%s] missing in CSV", key, mapKey));
            }
        }
        
        for (String mapKey : csvMap.keySet()) {
            if (!pdfMap.containsKey(mapKey)) {
                warnings.add(String.format("%s[%s] missing in PDF", key, mapKey));
            }
        }
    }
    
    private void validateListConsistency(String key, List<?> pdfList, List<?> csvList,
                                       List<String> errors, List<String> warnings) {
        if (pdfList.size() != csvList.size()) {
            warnings.add(String.format("%s list size mismatch: PDF=%d, CSV=%d", key, pdfList.size(), csvList.size()));
        }
        
        int minSize = Math.min(pdfList.size(), csvList.size());
        for (int i = 0; i < minSize; i++) {
            Object pdfItem = pdfList.get(i);
            Object csvItem = csvList.get(i);
            
            if (!areObjectValuesConsistent(pdfItem, csvItem)) {
                errors.add(String.format("Inconsistent %s[%d]: PDF='%s', CSV='%s'", key, i, pdfItem, csvItem));
            }
        }
    }
    
    private boolean areObjectValuesConsistent(Object value1, Object value2) {
        if (value1 == null || value2 == null) {
            return value1 == value2;
        }
        
        // Convert to strings for comparison
        String str1 = value1.toString();
        String str2 = value2.toString();
        
        return areValuesConsistent(str1, str2);
    }
    
    private String[] getBreakdownKeys(String reportType) {
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                return new String[]{"visitsByVeterinarian", "visitsBySpecies", "visitsByType", "dailyVisits"};
            case "revenue":
                return new String[]{"revenueByVeterinarian", "revenueBySpecies", "revenueByVisitType", "dailyRevenue"};
            case "dashboard":
                return new String[]{"upcomingAppointments", "recentActivities", "alerts"};
            default:
                return new String[0];
        }
    }
    
    // Report-specific validation methods
    
    private void validateVisitCalculations(Map<String, String> pdfMetrics, Map<String, String> csvMetrics,
                                         Map<String, Object> pdfData, Map<String, Object> csvData,
                                         List<String> errors, List<String> warnings) {
        // Validate completion rate calculation
        if (pdfMetrics.containsKey("totalVisits") && pdfMetrics.containsKey("completedVisits") && 
            pdfMetrics.containsKey("completionRate")) {
            
            try {
                long totalVisits = Long.parseLong(pdfMetrics.get("totalVisits").replaceAll("[^0-9]", ""));
                long completedVisits = Long.parseLong(pdfMetrics.get("completedVisits").replaceAll("[^0-9]", ""));
                double completionRate = Double.parseDouble(pdfMetrics.get("completionRate").replaceAll("[^0-9.]", ""));
                
                double expectedRate = totalVisits > 0 ? (double) completedVisits / totalVisits * 100.0 : 0.0;
                if (Math.abs(completionRate - expectedRate) > 0.1) {
                    errors.add(String.format("PDF completion rate calculation incorrect: expected %.1f%%, got %.1f%%",
                            expectedRate, completionRate));
                }
            } catch (NumberFormatException e) {
                warnings.add("Could not validate completion rate calculation in PDF");
            }
        }
        
        // Validate the same for CSV
        if (csvMetrics.containsKey("totalVisits") && csvMetrics.containsKey("completedVisits") && 
            csvMetrics.containsKey("completionRate")) {
            
            try {
                long totalVisits = Long.parseLong(csvMetrics.get("totalVisits").replaceAll("[^0-9]", ""));
                long completedVisits = Long.parseLong(csvMetrics.get("completedVisits").replaceAll("[^0-9]", ""));
                double completionRate = Double.parseDouble(csvMetrics.get("completionRate").replaceAll("[^0-9.]", ""));
                
                double expectedRate = totalVisits > 0 ? (double) completedVisits / totalVisits * 100.0 : 0.0;
                if (Math.abs(completionRate - expectedRate) > 0.1) {
                    errors.add(String.format("CSV completion rate calculation incorrect: expected %.1f%%, got %.1f%%",
                            expectedRate, completionRate));
                }
            } catch (NumberFormatException e) {
                warnings.add("Could not validate completion rate calculation in CSV");
            }
        }
    }
    
    private void validateRevenueCalculations(Map<String, String> pdfMetrics, Map<String, String> csvMetrics,
                                           Map<String, Object> pdfData, Map<String, Object> csvData,
                                           List<String> errors, List<String> warnings) {
        // Validate payment rate calculation
        if (pdfMetrics.containsKey("totalPaidVisits") && pdfMetrics.containsKey("totalUnpaidVisits") && 
            pdfMetrics.containsKey("paymentRate")) {
            
            try {
                long paidVisits = Long.parseLong(pdfMetrics.get("totalPaidVisits").replaceAll("[^0-9]", ""));
                long unpaidVisits = Long.parseLong(pdfMetrics.get("totalUnpaidVisits").replaceAll("[^0-9]", ""));
                double paymentRate = Double.parseDouble(pdfMetrics.get("paymentRate").replaceAll("[^0-9.]", ""));
                
                long totalVisits = paidVisits + unpaidVisits;
                double expectedRate = totalVisits > 0 ? (double) paidVisits / totalVisits * 100.0 : 0.0;
                if (Math.abs(paymentRate - expectedRate) > 0.1) {
                    errors.add(String.format("PDF payment rate calculation incorrect: expected %.1f%%, got %.1f%%",
                            expectedRate, paymentRate));
                }
            } catch (NumberFormatException e) {
                warnings.add("Could not validate payment rate calculation in PDF");
            }
        }
        
        // Validate average revenue per visit
        if (pdfMetrics.containsKey("totalRevenue") && pdfMetrics.containsKey("totalPaidVisits") && 
            pdfMetrics.containsKey("averageRevenuePerVisit")) {
            
            try {
                double totalRevenue = Double.parseDouble(pdfMetrics.get("totalRevenue").replaceAll("[^0-9.]", ""));
                long paidVisits = Long.parseLong(pdfMetrics.get("totalPaidVisits").replaceAll("[^0-9]", ""));
                double avgRevenue = Double.parseDouble(pdfMetrics.get("averageRevenuePerVisit").replaceAll("[^0-9.]", ""));
                
                double expectedAvg = paidVisits > 0 ? totalRevenue / paidVisits : 0.0;
                if (Math.abs(avgRevenue - expectedAvg) > 0.01) {
                    errors.add(String.format("PDF average revenue per visit calculation incorrect: expected %.2f, got %.2f",
                            expectedAvg, avgRevenue));
                }
            } catch (NumberFormatException e) {
                warnings.add("Could not validate average revenue per visit calculation in PDF");
            }
        }
    }
    
    private void validateDashboardCalculations(Map<String, String> pdfMetrics, Map<String, String> csvMetrics,
                                             Map<String, Object> pdfData, Map<String, Object> csvData,
                                             List<String> errors, List<String> warnings) {
        // Validate today's completion rate
        if (pdfMetrics.containsKey("todayAppointments") && pdfMetrics.containsKey("todayCompletedVisits") && 
            pdfMetrics.containsKey("todayCompletionRate")) {
            
            try {
                long todayAppointments = Long.parseLong(pdfMetrics.get("todayAppointments").replaceAll("[^0-9]", ""));
                long todayCompleted = Long.parseLong(pdfMetrics.get("todayCompletedVisits").replaceAll("[^0-9]", ""));
                double completionRate = Double.parseDouble(pdfMetrics.get("todayCompletionRate").replaceAll("[^0-9.]", ""));
                
                double expectedRate = todayAppointments > 0 ? (double) todayCompleted / todayAppointments * 100.0 : 0.0;
                if (Math.abs(completionRate - expectedRate) > 0.1) {
                    errors.add(String.format("PDF today's completion rate calculation incorrect: expected %.1f%%, got %.1f%%",
                            expectedRate, completionRate));
                }
            } catch (NumberFormatException e) {
                warnings.add("Could not validate today's completion rate calculation in PDF");
            }
        }
    }
    
    private void validateVisitTotals(Map<String, Object> pdfData, Map<String, Object> csvData,
                                   List<String> errors, List<String> warnings) {
        // Validate that breakdown totals match overall totals
        if (pdfData.containsKey("visitsByVeterinarian") && pdfData.containsKey("totalVisits")) {
            Object breakdownObj = pdfData.get("visitsByVeterinarian");
            if (breakdownObj instanceof Map) {
                Map<String, Object> breakdown = (Map<String, Object>) breakdownObj;
                long breakdownTotal = breakdown.values().stream()
                        .mapToLong(v -> Long.parseLong(v.toString().replaceAll("[^0-9]", "")))
                        .sum();
                
                Object totalObj = pdfData.get("totalVisits");
                if (totalObj != null) {
                    long reportedTotal = Long.parseLong(totalObj.toString().replaceAll("[^0-9]", ""));
                    if (breakdownTotal != reportedTotal) {
                        errors.add(String.format("PDF visits by veterinarian total (%d) doesn't match reported total (%d)",
                                breakdownTotal, reportedTotal));
                    }
                }
            }
        }
    }
    
    private void validateRevenueTotals(Map<String, Object> pdfData, Map<String, Object> csvData,
                                     List<String> errors, List<String> warnings) {
        // Validate that breakdown totals match overall totals
        if (pdfData.containsKey("revenueByVeterinarian") && pdfData.containsKey("totalRevenue")) {
            Object breakdownObj = pdfData.get("revenueByVeterinarian");
            if (breakdownObj instanceof Map) {
                Map<String, Object> breakdown = (Map<String, Object>) breakdownObj;
                double breakdownTotal = breakdown.values().stream()
                        .mapToDouble(v -> Double.parseDouble(v.toString().replaceAll("[^0-9.]", "")))
                        .sum();
                
                Object totalObj = pdfData.get("totalRevenue");
                if (totalObj != null) {
                    double reportedTotal = Double.parseDouble(totalObj.toString().replaceAll("[^0-9.]", ""));
                    if (Math.abs(breakdownTotal - reportedTotal) > 0.01) {
                        errors.add(String.format("PDF revenue by veterinarian total (%.2f) doesn't match reported total (%.2f)",
                                breakdownTotal, reportedTotal));
                    }
                }
            }
        }
    }
    
    private void validateDashboardTotals(Map<String, Object> pdfData, Map<String, Object> csvData,
                                       List<String> errors, List<String> warnings) {
        // Validate pending + completed = total appointments
        if (pdfData.containsKey("todayAppointments") && pdfData.containsKey("todayCompletedVisits") && 
            pdfData.containsKey("todayPendingVisits")) {
            
            try {
                long totalAppointments = Long.parseLong(pdfData.get("todayAppointments").toString().replaceAll("[^0-9]", ""));
                long completedVisits = Long.parseLong(pdfData.get("todayCompletedVisits").toString().replaceAll("[^0-9]", ""));
                long pendingVisits = Long.parseLong(pdfData.get("todayPendingVisits").toString().replaceAll("[^0-9]", ""));
                
                if (completedVisits + pendingVisits != totalAppointments) {
                    errors.add(String.format("PDF today's appointments total inconsistent: completed(%d) + pending(%d) = %d, but total is %d",
                            completedVisits, pendingVisits, completedVisits + pendingVisits, totalAppointments));
                }
            } catch (NumberFormatException e) {
                warnings.add("Could not validate today's appointments total in PDF");
            }
        }
    }
    
    // Data extraction methods for specific report types
    
    private void extractVisitBreakdownFromPDF(String text, Map<String, Object> data) {
        // Extract visits by veterinarian
        Map<String, Object> vetBreakdown = new HashMap<>();
        java.util.regex.Pattern vetPattern = java.util.regex.Pattern.compile("(Dr\\.[\\s\\w]+)[:\\s]+(\\d+)\\s*visits?", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher vetMatcher = vetPattern.matcher(text);
        while (vetMatcher.find()) {
            vetBreakdown.put(vetMatcher.group(1).trim(), vetMatcher.group(2));
        }
        
        // Also try pattern without "visits" suffix
        java.util.regex.Pattern vetPattern2 = java.util.regex.Pattern.compile("(Dr\\.[\\s\\w]+)[,\\s]+(\\d+)(?!.*visits)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher vetMatcher2 = vetPattern2.matcher(text);
        while (vetMatcher2.find()) {
            String vetName = vetMatcher2.group(1).trim();
            if (!vetBreakdown.containsKey(vetName)) {
                vetBreakdown.put(vetName, vetMatcher2.group(2));
            }
        }
        
        if (!vetBreakdown.isEmpty()) {
            data.put("visitsByVeterinarian", vetBreakdown);
        }
        
        // Extract visits by species
        Map<String, Object> speciesBreakdown = new HashMap<>();
        java.util.regex.Pattern speciesPattern = java.util.regex.Pattern.compile("(Dog|Cat|Bird|Rabbit|Other)[:\\s]+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher speciesMatcher = speciesPattern.matcher(text);
        while (speciesMatcher.find()) {
            speciesBreakdown.put(speciesMatcher.group(1), speciesMatcher.group(2));
        }
        if (!speciesBreakdown.isEmpty()) {
            data.put("visitsBySpecies", speciesBreakdown);
        }
        
        // Extract total visits for validation
        java.util.regex.Pattern totalPattern = java.util.regex.Pattern.compile("Total Visits[:\\s]+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher totalMatcher = totalPattern.matcher(text);
        if (totalMatcher.find()) {
            data.put("totalVisits", totalMatcher.group(1));
        }
    }
    
    private void extractRevenueBreakdownFromPDF(String text, Map<String, Object> data) {
        // Extract revenue by veterinarian
        Map<String, Object> vetRevenue = new HashMap<>();
        java.util.regex.Pattern vetPattern = java.util.regex.Pattern.compile("(Dr\\.[\\s\\w]+)[:\\s]+\\$?([\\d,]+\\.?\\d*)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher vetMatcher = vetPattern.matcher(text);
        while (vetMatcher.find()) {
            vetRevenue.put(vetMatcher.group(1).trim(), vetMatcher.group(2));
        }
        if (!vetRevenue.isEmpty()) {
            data.put("revenueByVeterinarian", vetRevenue);
        }
        
        // Extract total revenue for validation
        java.util.regex.Pattern totalPattern = java.util.regex.Pattern.compile("Total Revenue[:\\s]+\\$?([\\d,]+\\.?\\d*)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher totalMatcher = totalPattern.matcher(text);
        if (totalMatcher.find()) {
            data.put("totalRevenue", totalMatcher.group(1));
        }
    }
    
    private void extractDashboardDataFromPDF(String text, Map<String, Object> data) {
        // Extract upcoming appointments count
        java.util.regex.Pattern appointmentPattern = java.util.regex.Pattern.compile("Upcoming Appointments\\s+(\\d+)");
        java.util.regex.Matcher appointmentMatcher = appointmentPattern.matcher(text);
        if (appointmentMatcher.find()) {
            data.put("upcomingAppointmentsCount", appointmentMatcher.group(1));
        }
        
        // Extract alerts count
        java.util.regex.Pattern alertPattern = java.util.regex.Pattern.compile("Alerts\\s+(\\d+)");
        java.util.regex.Matcher alertMatcher = alertPattern.matcher(text);
        if (alertMatcher.find()) {
            data.put("alertsCount", alertMatcher.group(1));
        }
    }
    
    private void extractCommonDataFromPDF(String text, Map<String, Object> data) {
        // Extract date range - multiple patterns
        java.util.regex.Pattern datePattern1 = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher dateMatcher1 = datePattern1.matcher(text);
        if (dateMatcher1.find()) {
            data.put("startDate", dateMatcher1.group(1));
            data.put("endDate", dateMatcher1.group(2));
        }
        
        // Try alternative date pattern
        java.util.regex.Pattern datePattern2 = java.util.regex.Pattern.compile("Report Period[:\\s]+(\\d{4}-\\d{2}-\\d{2})\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher dateMatcher2 = datePattern2.matcher(text);
        if (dateMatcher2.find()) {
            data.put("startDate", dateMatcher2.group(1));
            data.put("endDate", dateMatcher2.group(2));
        }
        
        // Extract generation timestamp
        java.util.regex.Pattern timestampPattern = java.util.regex.Pattern.compile("Generated\\s+on\\s+([\\d\\-\\s:]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher timestampMatcher = timestampPattern.matcher(text);
        if (timestampMatcher.find()) {
            data.put("generatedAt", timestampMatcher.group(1).trim());
        }
    }
    
    private void extractVisitBreakdownFromCSV(List<String[]> records, Map<String, Object> data) {
        Map<String, Object> vetBreakdown = new HashMap<>();
        Map<String, Object> speciesBreakdown = new HashMap<>();
        
        for (String[] row : records) {
            if (row.length >= 2) {
                String key = row[0] != null ? row[0].trim() : "";
                String value = row[1] != null ? row[1].trim() : "";
                
                // Extract veterinarian data
                if (key.toLowerCase().contains("dr.") || key.toLowerCase().contains("veterinarian")) {
                    if (!value.isEmpty()) {
                        vetBreakdown.put(key, value);
                    }
                } else if (key.matches("(?i)Dog|Cat|Bird|Rabbit|Other")) {
                    if (!value.isEmpty()) {
                        speciesBreakdown.put(key, value);
                    }
                } else if (key.toLowerCase().contains("total visits")) {
                    data.put("totalVisits", value);
                }
            }
        }
        
        if (!vetBreakdown.isEmpty()) {
            data.put("visitsByVeterinarian", vetBreakdown);
        }
        if (!speciesBreakdown.isEmpty()) {
            data.put("visitsBySpecies", speciesBreakdown);
        }
    }
    
    private void extractRevenueBreakdownFromCSV(List<String[]> records, Map<String, Object> data) {
        Map<String, Object> vetRevenue = new HashMap<>();
        
        for (String[] row : records) {
            if (row.length >= 2) {
                String key = row[0] != null ? row[0].trim() : "";
                String value = row[1] != null ? row[1].trim() : "";
                
                if (key.toLowerCase().contains("dr.") || key.toLowerCase().contains("veterinarian")) {
                    if (!value.isEmpty()) {
                        vetRevenue.put(key, value);
                    }
                } else if (key.toLowerCase().contains("total revenue")) {
                    data.put("totalRevenue", value);
                }
            }
        }
        
        if (!vetRevenue.isEmpty()) {
            data.put("revenueByVeterinarian", vetRevenue);
        }
    }
    
    private void extractDashboardDataFromCSV(List<String[]> records, Map<String, Object> data) {
        for (String[] row : records) {
            if (row.length >= 2) {
                String key = row[0] != null ? row[0].trim() : "";
                String value = row[1] != null ? row[1].trim() : "";
                
                if (key.toLowerCase().contains("upcoming appointments")) {
                    data.put("upcomingAppointmentsCount", value);
                } else if (key.toLowerCase().contains("alerts")) {
                    data.put("alertsCount", value);
                }
            }
        }
    }
    
    private void extractCommonDataFromCSV(List<String[]> records, Map<String, Object> data) {
        for (String[] row : records) {
            if (row.length >= 2) {
                String key = row[0] != null ? row[0].trim() : "";
                String value = row[1] != null ? row[1].trim() : "";
                
                if (key.toLowerCase().contains("start date")) {
                    data.put("startDate", value);
                } else if (key.toLowerCase().contains("end date")) {
                    data.put("endDate", value);
                } else if (key.toLowerCase().contains("generated")) {
                    data.put("generatedAt", value);
                }
            }
        }
    }
    
    // Helper methods
    
    private boolean containsExpectedHeaders(String[] row, List<String> expectedHeaders) {
        if (row.length < 2) return false;
        
        // For CSV reports with section-based structure, look for the basic "Metric, Value" pattern
        // which is the most common header structure in our CSV exports
        if (expectedHeaders.contains("Metric") && expectedHeaders.contains("Value")) {
            boolean hasMetric = false;
            boolean hasValue = false;
            
            for (String cell : row) {
                if (cell != null) {
                    String trimmed = cell.trim();
                    if (trimmed.equalsIgnoreCase("Metric")) {
                        hasMetric = true;
                    } else if (trimmed.equalsIgnoreCase("Value")) {
                        hasValue = true;
                    }
                }
            }
            
            return hasMetric && hasValue;
        }
        
        // Fallback to original logic for other header patterns
        int matchCount = 0;
        for (String expected : expectedHeaders) {
            for (String actual : row) {
                if (actual != null && actual.trim().equalsIgnoreCase(expected.trim())) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return matchCount >= Math.min(2, expectedHeaders.size());
    }
    
    private boolean isEmptyRow(String[] row) {
        if (row == null || row.length == 0) return true;
        
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    private void validateCSVDataTypes(String[] row, String[] headers, String reportType, 
                                    int rowNumber, List<String> errors, List<String> warnings) {
        for (int i = 0; i < Math.min(row.length, headers.length); i++) {
            String header = headers[i];
            String value = row[i];
            
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            
            // Validate numeric fields
            if (header.toLowerCase().contains("visits") || header.toLowerCase().contains("count") ||
                header.toLowerCase().contains("total") || header.toLowerCase().contains("percentage")) {
                
                try {
                    if (header.toLowerCase().contains("percentage") || header.toLowerCase().contains("rate")) {
                        double percentage = Double.parseDouble(value.replace("%", ""));
                        if (percentage < 0 || percentage > 100) {
                            warnings.add(String.format("Row %d: %s value out of range: %s", rowNumber, header, value));
                        }
                    } else {
                        long number = Long.parseLong(value);
                        if (number < 0) {
                            warnings.add(String.format("Row %d: %s cannot be negative: %s", rowNumber, header, value));
                        }
                    }
                } catch (NumberFormatException e) {
                    errors.add(String.format("Row %d: %s should be numeric: %s", rowNumber, header, value));
                }
            }
            
            // Validate currency fields
            if (header.toLowerCase().contains("revenue") || header.toLowerCase().contains("$")) {
                if (!value.matches("^\\$?[0-9,]+\\.?[0-9]*$")) {
                    warnings.add(String.format("Row %d: %s should be currency format: %s", rowNumber, header, value));
                }
            }
            
            // Validate date fields
            if (header.toLowerCase().contains("date") || header.toLowerCase().contains("time")) {
                try {
                    if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        LocalDate.parse(value, DATE_FORMATTER);
                    }
                } catch (Exception e) {
                    warnings.add(String.format("Row %d: %s should be valid date: %s", rowNumber, header, value));
                }
            }
        }
    }
    
    private void validateCSVStructure(String csvString, List<String> errors, List<String> warnings) {
        String[] lines = csvString.split("\n");
        
        // Check for consistent delimiter usage
        int expectedCommas = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            String[] parts = CSV_DELIMITER_PATTERN.split(line);
            int commaCount = parts.length - 1;
            
            if (expectedCommas == -1) {
                expectedCommas = commaCount;
            } else if (commaCount != expectedCommas && commaCount > 0) {
                warnings.add(String.format("Line %d has inconsistent delimiter count: expected %d, got %d", 
                        i + 1, expectedCommas, commaCount));
            }
        }
        
        // Check for proper escaping
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("\"")) {
                // Basic check for balanced quotes
                long quoteCount = line.chars().filter(ch -> ch == '"').count();
                if (quoteCount % 2 != 0) {
                    errors.add(String.format("Line %d has unbalanced quotes", i + 1));
                }
            }
        }
    }
    
    private void validatePDFReportContent(String pdfText, String reportType, 
                                        List<String> errors, List<String> warnings) {
        String lowerText = pdfText.toLowerCase();
        
        switch (reportType.toLowerCase()) {
            case "visits":
            case "visit-statistics":
                if (!lowerText.contains("total visits")) {
                    errors.add("PDF missing 'Total Visits' metric");
                }
                if (!lowerText.contains("completed visits")) {
                    errors.add("PDF missing 'Completed Visits' metric");
                }
                if (!lowerText.contains("completion rate")) {
                    errors.add("PDF missing 'Completion Rate' metric");
                }
                break;
                
            case "revenue":
                if (!lowerText.contains("total revenue")) {
                    errors.add("PDF missing 'Total Revenue' metric");
                }
                if (!lowerText.contains("payment rate")) {
                    errors.add("PDF missing 'Payment Rate' metric");
                }
                break;
                
            case "dashboard":
                if (!lowerText.contains("today's appointments")) {
                    errors.add("PDF missing 'Today's Appointments' metric");
                }
                if (!lowerText.contains("total active pets")) {
                    errors.add("PDF missing 'Total Active Pets' metric");
                }
                break;
        }
        
        // Check for generation timestamp
        if (!lowerText.contains("generated")) {
            warnings.add("PDF missing generation timestamp");
        }
    }
    
    private Map<String, String> extractMetricsFromPDF(byte[] pdfContent) {
        Map<String, String> metrics = new HashMap<>();
        
        try {
            String text;
            
            // Try to parse as actual PDF first
            try {
                PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfContent));
                PdfDocument pdfDoc = new PdfDocument(reader);
                
                StringBuilder fullText = new StringBuilder();
                for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                    fullText.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i)));
                }
                pdfDoc.close();
                text = fullText.toString();
            } catch (Exception pdfException) {
                // If PDF parsing fails, treat as plain text (for testing purposes)
                text = new String(pdfContent);
                logger.debug("PDF parsing failed, treating as plain text: {}", pdfException.getMessage());
            }
            
            // Extract common metrics using regex patterns
            extractMetricFromText(text, "Total Visits[:\\s]+(\\d+)", "totalVisits", metrics);
            extractMetricFromText(text, "Completed Visits[:\\s]+(\\d+)", "completedVisits", metrics);
            extractMetricFromText(text, "Total Revenue[:\\s]+\\$?([\\d,]+\\.?\\d*)", "totalRevenue", metrics);
            extractMetricFromText(text, "Completion Rate[:\\s]+([\\d.]+)%?", "completionRate", metrics);
            extractMetricFromText(text, "Payment Rate[:\\s]+([\\d.]+)%?", "paymentRate", metrics);
            extractMetricFromText(text, "Average Revenue per Visit[:\\s]+\\$?([\\d,]+\\.?\\d*)", "averageRevenuePerVisit", metrics);
            extractMetricFromText(text, "Total Paid Visits[:\\s]+(\\d+)", "totalPaidVisits", metrics);
            extractMetricFromText(text, "Total Unpaid Visits[:\\s]+(\\d+)", "totalUnpaidVisits", metrics);
            extractMetricFromText(text, "Today's Appointments[:\\s]+(\\d+)", "todayAppointments", metrics);
            extractMetricFromText(text, "Today's Completed Visits[:\\s]+(\\d+)", "todayCompletedVisits", metrics);
            extractMetricFromText(text, "Today's Pending Visits[:\\s]+(\\d+)", "todayPendingVisits", metrics);
            extractMetricFromText(text, "Today's Revenue[:\\s]+\\$?([\\d,]+\\.?\\d*)", "todayRevenue", metrics);
            extractMetricFromText(text, "Veterinarian Utilization[:\\s]+([\\d.]+)%?", "veterinarianUtilization", metrics);
            
        } catch (Exception e) {
            logger.warn("Failed to extract metrics from PDF: {}", e.getMessage());
        }
        
        return metrics;
    }
    
    private Map<String, String> extractMetricsFromCSV(byte[] csvContent, String reportType) {
        Map<String, String> metrics = new HashMap<>();
        
        try {
            String csvString = new String(csvContent);
            CSVReader reader = new CSVReader(new StringReader(csvString));
            List<String[]> records = reader.readAll();
            reader.close();
            
            // Look for metric-value pairs
            for (String[] row : records) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    String metric = row[0].trim().toLowerCase();
                    String value = row[1].trim();
                    
                    if (metric.contains("total visits")) {
                        metrics.put("totalVisits", value);
                    } else if (metric.contains("completed visits")) {
                        metrics.put("completedVisits", value);
                    } else if (metric.contains("total revenue")) {
                        metrics.put("totalRevenue", value);
                    } else if (metric.contains("completion rate")) {
                        metrics.put("completionRate", value);
                    } else if (metric.contains("payment rate")) {
                        metrics.put("paymentRate", value);
                    } else if (metric.contains("average revenue per visit")) {
                        metrics.put("averageRevenuePerVisit", value);
                    } else if (metric.contains("total paid visits")) {
                        metrics.put("totalPaidVisits", value);
                    } else if (metric.contains("total unpaid visits")) {
                        metrics.put("totalUnpaidVisits", value);
                    } else if (metric.contains("today's appointments")) {
                        metrics.put("todayAppointments", value);
                    } else if (metric.contains("today's completed visits")) {
                        metrics.put("todayCompletedVisits", value);
                    } else if (metric.contains("today's pending visits")) {
                        metrics.put("todayPendingVisits", value);
                    } else if (metric.contains("today's revenue")) {
                        metrics.put("todayRevenue", value);
                    } else if (metric.contains("veterinarian utilization")) {
                        metrics.put("veterinarianUtilization", value);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to extract metrics from CSV: {}", e.getMessage());
        }
        
        return metrics;
    }
    
    private void extractMetricFromText(String text, String pattern, String key, Map<String, String> metrics) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            metrics.put(key, m.group(1));
        }
    }
    
    private boolean areValuesConsistent(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return value1 == value2;
        }
        
        // Normalize values for comparison
        String normalized1 = value1.replaceAll("[,$%]", "").trim();
        String normalized2 = value2.replaceAll("[,$%]", "").trim();
        
        // Try numeric comparison
        try {
            double num1 = Double.parseDouble(normalized1);
            double num2 = Double.parseDouble(normalized2);
            return Math.abs(num1 - num2) < 0.01;
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return normalized1.equalsIgnoreCase(normalized2);
        }
    }
}