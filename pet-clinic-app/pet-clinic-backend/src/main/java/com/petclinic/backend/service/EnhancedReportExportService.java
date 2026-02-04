package com.petclinic.backend.service;

import com.petclinic.backend.dto.ExportProgress;
import com.petclinic.backend.dto.ExportRequest;
import com.petclinic.backend.dto.ExportResult;

import java.util.concurrent.CompletableFuture;

/**
 * Enhanced service interface for report export functionality with async support
 * Provides PDF and CSV export capabilities with progress indicators and retry mechanisms
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4
 */
public interface EnhancedReportExportService {
    
    /**
     * Start an asynchronous export operation
     * @param request Export request parameters
     * @return CompletableFuture with export result
     */
    CompletableFuture<ExportResult> exportReportAsync(ExportRequest request);
    
    /**
     * Get the progress of an ongoing export operation
     * @param exportId Export operation ID
     * @return Export progress information
     */
    ExportProgress getExportProgress(String exportId);
    
    /**
     * Cancel an ongoing export operation
     * @param exportId Export operation ID
     * @return true if cancellation was successful
     */
    boolean cancelExport(String exportId);
    
    /**
     * Retry a failed export operation
     * @param exportId Export operation ID
     * @return CompletableFuture with retry result
     */
    CompletableFuture<ExportResult> retryExport(String exportId);
    
    /**
     * Get download URL for completed export
     * @param exportId Export operation ID
     * @return Download URL or null if not available
     */
    String getDownloadUrl(String exportId);
    
    /**
     * Clean up completed or failed export data
     * @param exportId Export operation ID
     */
    void cleanupExport(String exportId);
    
    /**
     * Validate export request parameters
     * @param request Export request to validate
     * @return true if request is valid
     */
    boolean validateExportRequest(ExportRequest request);
    
    /**
     * Estimate export completion time
     * @param request Export request parameters
     * @return Estimated completion time in milliseconds
     */
    long estimateExportTime(ExportRequest request);
    
    /**
     * Get export result for download
     * @param exportId Export operation ID
     * @return Export result or null if not available
     */
    ExportResult getExportResult(String exportId);
}