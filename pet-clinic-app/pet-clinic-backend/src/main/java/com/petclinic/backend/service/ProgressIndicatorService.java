package com.petclinic.backend.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing progress indicators for long-running operations
 * Validates: Requirements 16.4, 16.5
 */
public interface ProgressIndicatorService {
    
    /**
     * Create a new progress indicator for a long-running operation
     * @param operationId Unique identifier for the operation
     * @param operationType Type of operation (export, import, bulk_delete, etc.)
     * @param totalSteps Total number of steps in the operation
     * @param description Human-readable description of the operation
     * @return Progress indicator ID
     */
    String createProgressIndicator(String operationId, String operationType, int totalSteps, String description);
    
    /**
     * Update progress for an ongoing operation
     * @param progressId Progress indicator ID
     * @param currentStep Current step number
     * @param message Current status message
     */
    void updateProgress(String progressId, int currentStep, String message);
    
    /**
     * Update progress with percentage
     * @param progressId Progress indicator ID
     * @param percentage Completion percentage (0-100)
     * @param message Current status message
     */
    void updateProgress(String progressId, double percentage, String message);
    
    /**
     * Mark operation as completed
     * @param progressId Progress indicator ID
     * @param result Final result or summary
     */
    void completeProgress(String progressId, String result);
    
    /**
     * Mark operation as failed
     * @param progressId Progress indicator ID
     * @param error Error message or exception details
     */
    void failProgress(String progressId, String error);
    
    /**
     * Get current progress status
     * @param progressId Progress indicator ID
     * @return Map containing progress information
     */
    Map<String, Object> getProgress(String progressId);
    
    /**
     * Get all active progress indicators
     * @return Map of progress ID to progress information
     */
    Map<String, Map<String, Object>> getAllActiveProgress();
    
    /**
     * Cancel a running operation
     * @param progressId Progress indicator ID
     * @return true if operation was successfully cancelled
     */
    boolean cancelOperation(String progressId);
    
    /**
     * Clean up completed or failed progress indicators older than specified minutes
     * @param retentionMinutes Number of minutes to retain completed operations
     */
    void cleanupOldProgress(int retentionMinutes);
    
    /**
     * Scheduled cleanup method for automatic maintenance
     */
    void scheduledCleanup();
    
    /**
     * Execute operation with automatic progress tracking
     * @param operationId Unique identifier for the operation
     * @param operationType Type of operation
     * @param description Operation description
     * @param operation The operation to execute
     * @return CompletableFuture with operation result
     */
    <T> CompletableFuture<T> executeWithProgress(String operationId, String operationType, 
                                                String description, ProgressAwareOperation<T> operation);
    
    /**
     * Functional interface for operations that can report progress
     */
    @FunctionalInterface
    interface ProgressAwareOperation<T> {
        T execute(ProgressCallback callback) throws Exception;
    }
    
    /**
     * Callback interface for reporting progress during operation execution
     */
    interface ProgressCallback {
        void updateProgress(int currentStep, int totalSteps, String message);
        void updateProgress(double percentage, String message);
        boolean isCancelled();
    }
}