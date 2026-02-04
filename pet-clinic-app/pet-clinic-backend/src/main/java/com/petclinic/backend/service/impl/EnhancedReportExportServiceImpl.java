package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.*;
import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.service.EnhancedReportExportService;
import com.petclinic.backend.service.ReportExportService;
import com.petclinic.backend.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced implementation of ReportExportService with async support and progress tracking
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Service
public class EnhancedReportExportServiceImpl implements EnhancedReportExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedReportExportServiceImpl.class);
    
    @Autowired
    private ReportExportService reportExportService;
    
    @Autowired
    private ReportService reportService;
    
    // In-memory storage for export progress and results
    // In production, this should be replaced with Redis or database storage
    private final Map<String, ExportProgress> exportProgressMap = new ConcurrentHashMap<>();
    private final Map<String, ExportResult> exportResultMap = new ConcurrentHashMap<>();
    
    @Override
    @Async
    public CompletableFuture<ExportResult> exportReportAsync(ExportRequest request) {
        String exportId = UUID.randomUUID().toString();
        logger.info("Starting async export operation: {} for report type: {}", exportId, request.getReportType());
        
        // Initialize progress tracking
        ExportProgress progress = new ExportProgress(exportId, request.getReportType(), request.getFormat());
        progress.setCurrentStep("Initializing export");
        progress.setProgressPercentage(0);
        exportProgressMap.put(exportId, progress);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate request
            if (!validateExportRequest(request)) {
                throw new PetClinicException("Invalid export request parameters", "INVALID_REQUEST", HttpStatus.BAD_REQUEST);
            }
            
            // Update progress
            progress.setStatus(ExportProgress.ExportStatus.IN_PROGRESS);
            progress.setCurrentStep("Validating parameters");
            progress.setProgressPercentage(10);
            
            // Estimate completion time
            long estimatedDuration = estimateExportTime(request);
            progress.setEstimatedCompletionTime(LocalDateTime.now().plusSeconds(estimatedDuration / 1000));
            progress.setEstimatedTimeRemaining(formatDuration(estimatedDuration));
            
            // Generate report data
            progress.setCurrentStep("Generating report data");
            progress.setProgressPercentage(30);
            
            byte[] exportData = generateExportData(request, progress);
            
            // Create result
            progress.setCurrentStep("Finalizing export");
            progress.setProgressPercentage(90);
            
            String filename = reportExportService.getExportFilename(
                request.getReportType(), 
                request.getFormat(), 
                request.getStartDate(), 
                request.getEndDate()
            );
            
            String contentType = reportExportService.getMimeType(request.getFormat());
            
            ExportResult result = ExportResult.builder()
                .exportId(exportId)
                .data(exportData)
                .filename(filename)
                .contentType(contentType)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .success(true)
                .build();
            
            // Complete progress
            progress.setStatus(ExportProgress.ExportStatus.COMPLETED);
            progress.setCurrentStep("Export completed");
            progress.setProgressPercentage(100);
            progress.setEstimatedTimeRemaining("0 seconds");
            
            // Store result
            exportResultMap.put(exportId, result);
            
            logger.info("Export operation completed successfully: {} in {}ms", exportId, result.getExecutionTimeMs());
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("Export operation failed: {}", exportId, e);
            
            // Update progress with error
            progress.setStatus(ExportProgress.ExportStatus.FAILED);
            progress.setErrorMessage(e.getMessage());
            progress.setCurrentStep("Export failed");
            
            ExportResult result = ExportResult.builder()
                .exportId(exportId)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .success(false)
                .errorMessage(e.getMessage())
                .build();
            
            exportResultMap.put(exportId, result);
            
            return CompletableFuture.completedFuture(result);
        }
    }
    
    @Override
    public ExportProgress getExportProgress(String exportId) {
        return exportProgressMap.get(exportId);
    }
    
    @Override
    public boolean cancelExport(String exportId) {
        ExportProgress progress = exportProgressMap.get(exportId);
        if (progress != null && progress.getStatus() == ExportProgress.ExportStatus.IN_PROGRESS) {
            progress.setStatus(ExportProgress.ExportStatus.CANCELLED);
            progress.setCurrentStep("Export cancelled");
            logger.info("Export operation cancelled: {}", exportId);
            return true;
        }
        return false;
    }
    
    @Override
    @Async
    public CompletableFuture<ExportResult> retryExport(String exportId) {
        ExportProgress progress = exportProgressMap.get(exportId);
        if (progress == null || !progress.canRetry()) {
            logger.warn("Cannot retry export: {} (progress: {})", exportId, progress);
            return CompletableFuture.completedFuture(null);
        }
        
        logger.info("Retrying export operation: {} (attempt {})", exportId, progress.getRetryCount() + 1);
        progress.incrementRetryCount();
        
        // Create new request from stored progress data
        ExportRequest retryRequest = new ExportRequest();
        retryRequest.setReportType(progress.getReportType());
        retryRequest.setFormat(progress.getFormat());
        
        // Reset progress for retry
        progress.setStatus(ExportProgress.ExportStatus.IN_PROGRESS);
        progress.setProgressPercentage(0);
        progress.setCurrentStep("Retrying export");
        progress.setErrorMessage(null);
        
        return exportReportAsync(retryRequest);
    }
    
    @Override
    public String getDownloadUrl(String exportId) {
        ExportResult result = exportResultMap.get(exportId);
        if (result != null && result.isSuccess()) {
            return "/api/reports/download/" + exportId;
        }
        return null;
    }
    
    @Override
    public void cleanupExport(String exportId) {
        exportProgressMap.remove(exportId);
        exportResultMap.remove(exportId);
        logger.debug("Cleaned up export data for: {}", exportId);
    }
    
    @Override
    public boolean validateExportRequest(ExportRequest request) {
        if (request == null) {
            return false;
        }
        
        if (request.getReportType() == null || request.getReportType().trim().isEmpty()) {
            return false;
        }
        
        if (request.getFormat() == null || request.getFormat().trim().isEmpty()) {
            return false;
        }
        
        return reportExportService.validateExportParameters(request.getReportType(), request.getFormat());
    }
    
    @Override
    public long estimateExportTime(ExportRequest request) {
        // Base time estimates in milliseconds
        long baseTime = 2000; // 2 seconds base
        
        // Adjust based on report type
        switch (request.getReportType().toLowerCase()) {
            case "visits":
            case "visit-statistics":
                baseTime += 3000; // 3 additional seconds
                break;
            case "revenue":
                baseTime += 5000; // 5 additional seconds
                break;
            case "dashboard":
                baseTime += 1000; // 1 additional second
                break;
            default:
                baseTime += 2000; // 2 additional seconds
        }
        
        // Adjust based on format
        if ("pdf".equalsIgnoreCase(request.getFormat())) {
            baseTime += 2000; // PDF takes longer than CSV
        }
        
        // Adjust based on date range
        if (request.getStartDate() != null && request.getEndDate() != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            baseTime += daysBetween * 100; // 100ms per day
        }
        
        return baseTime;
    }
    
    private byte[] generateExportData(ExportRequest request, ExportProgress progress) {
        progress.setCurrentStep("Fetching report data");
        progress.setProgressPercentage(40);
        
        switch (request.getReportType().toLowerCase()) {
            case "visits":
            case "visit-statistics":
                return generateVisitStatisticsExport(request, progress);
            case "revenue":
                return generateRevenueReportExport(request, progress);
            case "dashboard":
                return generateDashboardExport(request, progress);
            default:
                throw new PetClinicException("Unsupported report type: " + request.getReportType(), "UNSUPPORTED_REPORT_TYPE", HttpStatus.BAD_REQUEST);
        }
    }
    
    private byte[] generateVisitStatisticsExport(ExportRequest request, ExportProgress progress) {
        progress.setCurrentStep("Generating visit statistics");
        progress.setProgressPercentage(50);
        
        VisitStatisticsReport report;
        if (request.getVeterinarianId() != null || (request.getSpecies() != null && !request.getSpecies().trim().isEmpty())) {
            ReportFilter filter = new ReportFilter(request.getStartDate(), request.getEndDate());
            filter.setVeterinarianId(request.getVeterinarianId());
            if (request.getSpecies() != null && !request.getSpecies().trim().isEmpty()) {
                filter.setSpecies(java.util.List.of(request.getSpecies().trim()));
            }
            report = reportService.generateVisitStatistics(filter);
        } else {
            report = reportService.generateVisitStatistics(request.getStartDate(), request.getEndDate());
        }
        
        progress.setCurrentStep("Exporting to " + request.getFormat().toUpperCase());
        progress.setProgressPercentage(70);
        
        if ("csv".equalsIgnoreCase(request.getFormat())) {
            return reportExportService.exportVisitStatisticsToCSV(report);
        } else {
            return reportExportService.exportVisitStatisticsToPdf(report);
        }
    }
    
    private byte[] generateRevenueReportExport(ExportRequest request, ExportProgress progress) {
        progress.setCurrentStep("Generating revenue report");
        progress.setProgressPercentage(50);
        
        RevenueReport report;
        if (request.getVeterinarianId() != null) {
            ReportFilter filter = new ReportFilter(request.getStartDate(), request.getEndDate());
            filter.setVeterinarianId(request.getVeterinarianId());
            report = reportService.generateRevenueReport(filter);
        } else {
            report = reportService.generateRevenueReport(request.getStartDate(), request.getEndDate(), request.isIncludeTrends());
        }
        
        progress.setCurrentStep("Exporting to " + request.getFormat().toUpperCase());
        progress.setProgressPercentage(70);
        
        if ("csv".equalsIgnoreCase(request.getFormat())) {
            return reportExportService.exportRevenueReportToCSV(report);
        } else {
            return reportExportService.exportRevenueReportToPdf(report);
        }
    }
    
    private byte[] generateDashboardExport(ExportRequest request, ExportProgress progress) {
        progress.setCurrentStep("Generating dashboard metrics");
        progress.setProgressPercentage(50);
        
        DashboardMetrics metrics = request.getStartDate() != null ? 
                reportService.getDashboardMetrics(request.getStartDate()) : 
                reportService.getDashboardMetrics();
        
        progress.setCurrentStep("Exporting to " + request.getFormat().toUpperCase());
        progress.setProgressPercentage(70);
        
        if ("csv".equalsIgnoreCase(request.getFormat())) {
            return reportExportService.exportDashboardMetricsToCSV(metrics);
        } else {
            return reportExportService.exportDashboardMetricsToPdf(metrics);
        }
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else {
            return (seconds / 3600) + " hours";
        }
    }
    
    // Method to get export result (for download endpoint)
    public ExportResult getExportResult(String exportId) {
        return exportResultMap.get(exportId);
    }
}