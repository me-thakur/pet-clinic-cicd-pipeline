package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.PerformanceMonitoringService;
import com.petclinic.backend.service.ProgressIndicatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of progress indicator service
 * Provides progress tracking for long-running operations
 * Validates: Requirements 16.4, 16.5
 */
@Service
public class ProgressIndicatorServiceImpl implements ProgressIndicatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressIndicatorServiceImpl.class);
    
    private final PerformanceMonitoringService performanceMonitoringService;
    
    // Storage for progress indicators
    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> cancellationFlags = new ConcurrentHashMap<>();
    
    @Autowired
    public ProgressIndicatorServiceImpl(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }
    
    @Override
    public String createProgressIndicator(String operationId, String operationType, int totalSteps, String description) {
        try {
            String progressId = generateProgressId(operationId);
            
            ProgressInfo progressInfo = new ProgressInfo();
            progressInfo.setProgressId(progressId);
            progressInfo.setOperationId(operationId);
            progressInfo.setOperationType(operationType);
            progressInfo.setDescription(description);
            progressInfo.setTotalSteps(totalSteps);
            progressInfo.setCurrentStep(0);
            progressInfo.setPercentage(0.0);
            progressInfo.setStatus("STARTED");
            progressInfo.setStartTime(LocalDateTime.now());
            progressInfo.setLastUpdated(LocalDateTime.now());
            progressInfo.setMessage("Operation started");
            
            progressMap.put(progressId, progressInfo);
            cancellationFlags.put(progressId, new AtomicBoolean(false));
            
            logger.info("Created progress indicator: {} for operation: {} ({})", 
                progressId, operationId, operationType);
            
            return progressId;
            
        } catch (Exception e) {
            logger.error("Error creating progress indicator for operation {}: {}", operationId, e.getMessage(), e);
            throw new RuntimeException("Failed to create progress indicator", e);
        }
    }
    
    @Override
    public void updateProgress(String progressId, int currentStep, String message) {
        try {
            ProgressInfo progressInfo = progressMap.get(progressId);
            if (progressInfo == null) {
                logger.warn("Progress indicator not found: {}", progressId);
                return;
            }
            
            progressInfo.setCurrentStep(currentStep);
            progressInfo.setMessage(message);
            progressInfo.setLastUpdated(LocalDateTime.now());
            
            // Calculate percentage
            if (progressInfo.getTotalSteps() > 0) {
                double percentage = (double) currentStep / progressInfo.getTotalSteps() * 100;
                progressInfo.setPercentage(Math.min(100.0, percentage));
            }
            
            // Estimate remaining time
            updateEstimatedTime(progressInfo);
            
            logger.debug("Updated progress {}: step {}/{} - {}", 
                progressId, currentStep, progressInfo.getTotalSteps(), message);
            
        } catch (Exception e) {
            logger.error("Error updating progress {}: {}", progressId, e.getMessage(), e);
        }
    }
    
    @Override
    public void updateProgress(String progressId, double percentage, String message) {
        try {
            ProgressInfo progressInfo = progressMap.get(progressId);
            if (progressInfo == null) {
                logger.warn("Progress indicator not found: {}", progressId);
                return;
            }
            
            progressInfo.setPercentage(Math.min(100.0, Math.max(0.0, percentage)));
            progressInfo.setMessage(message);
            progressInfo.setLastUpdated(LocalDateTime.now());
            
            // Update current step based on percentage
            if (progressInfo.getTotalSteps() > 0) {
                int currentStep = (int) (percentage / 100.0 * progressInfo.getTotalSteps());
                progressInfo.setCurrentStep(currentStep);
            }
            
            // Estimate remaining time
            updateEstimatedTime(progressInfo);
            
            logger.debug("Updated progress {}: {:.1f}% - {}", progressId, percentage, message);
            
        } catch (Exception e) {
            logger.error("Error updating progress {}: {}", progressId, e.getMessage(), e);
        }
    }
    
    @Override
    public void completeProgress(String progressId, String result) {
        try {
            ProgressInfo progressInfo = progressMap.get(progressId);
            if (progressInfo == null) {
                logger.warn("Progress indicator not found: {}", progressId);
                return;
            }
            
            progressInfo.setStatus("COMPLETED");
            progressInfo.setPercentage(100.0);
            progressInfo.setMessage("Operation completed successfully");
            progressInfo.setResult(result);
            progressInfo.setEndTime(LocalDateTime.now());
            progressInfo.setLastUpdated(LocalDateTime.now());
            
            // Calculate total execution time
            if (progressInfo.getStartTime() != null) {
                long executionTime = java.time.Duration.between(
                    progressInfo.getStartTime(), progressInfo.getEndTime()).toMillis();
                progressInfo.setExecutionTimeMs(executionTime);
                
                // Record performance metrics
                performanceMonitoringService.recordApiPerformance(
                    progressInfo.getOperationType(), "OPERATION", 
                    executionTime, 200, null, null);
            }
            
            // Clean up cancellation flag
            cancellationFlags.remove(progressId);
            
            logger.info("Completed progress {}: {} in {}ms", 
                progressId, progressInfo.getOperationType(), progressInfo.getExecutionTimeMs());
            
        } catch (Exception e) {
            logger.error("Error completing progress {}: {}", progressId, e.getMessage(), e);
        }
    }
    
    @Override
    public void failProgress(String progressId, String error) {
        try {
            ProgressInfo progressInfo = progressMap.get(progressId);
            if (progressInfo == null) {
                logger.warn("Progress indicator not found: {}", progressId);
                return;
            }
            
            progressInfo.setStatus("FAILED");
            progressInfo.setMessage("Operation failed: " + error);
            progressInfo.setError(error);
            progressInfo.setEndTime(LocalDateTime.now());
            progressInfo.setLastUpdated(LocalDateTime.now());
            
            // Calculate total execution time
            if (progressInfo.getStartTime() != null) {
                long executionTime = java.time.Duration.between(
                    progressInfo.getStartTime(), progressInfo.getEndTime()).toMillis();
                progressInfo.setExecutionTimeMs(executionTime);
                
                // Record performance metrics with error status
                performanceMonitoringService.recordApiPerformance(
                    progressInfo.getOperationType(), "OPERATION", 
                    executionTime, 500, null, null);
            }
            
            // Clean up cancellation flag
            cancellationFlags.remove(progressId);
            
            logger.error("Failed progress {}: {} - {}", progressId, progressInfo.getOperationType(), error);
            
        } catch (Exception e) {
            logger.error("Error failing progress {}: {}", progressId, e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getProgress(String progressId) {
        try {
            ProgressInfo progressInfo = progressMap.get(progressId);
            if (progressInfo == null) {
                return Map.of("error", "Progress indicator not found: " + progressId);
            }
            
            return progressInfoToMap(progressInfo);
            
        } catch (Exception e) {
            logger.error("Error getting progress {}: {}", progressId, e.getMessage(), e);
            return Map.of("error", "Failed to get progress: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Map<String, Object>> getAllActiveProgress() {
        Map<String, Map<String, Object>> activeProgress = new HashMap<>();
        
        try {
            for (Map.Entry<String, ProgressInfo> entry : progressMap.entrySet()) {
                ProgressInfo progressInfo = entry.getValue();
                if ("STARTED".equals(progressInfo.getStatus()) || "IN_PROGRESS".equals(progressInfo.getStatus())) {
                    activeProgress.put(entry.getKey(), progressInfoToMap(progressInfo));
                }
            }
            
        } catch (Exception e) {
            logger.error("Error getting all active progress: {}", e.getMessage(), e);
        }
        
        return activeProgress;
    }
    
    @Override
    public boolean cancelOperation(String progressId) {
        try {
            ProgressInfo progressInfo = progressMap.get(progressId);
            if (progressInfo == null) {
                logger.warn("Progress indicator not found for cancellation: {}", progressId);
                return false;
            }
            
            if ("COMPLETED".equals(progressInfo.getStatus()) || "FAILED".equals(progressInfo.getStatus())) {
                logger.warn("Cannot cancel already finished operation: {}", progressId);
                return false;
            }
            
            // Set cancellation flag
            AtomicBoolean cancellationFlag = cancellationFlags.get(progressId);
            if (cancellationFlag != null) {
                cancellationFlag.set(true);
            }
            
            // Update progress info
            progressInfo.setStatus("CANCELLED");
            progressInfo.setMessage("Operation cancelled by user");
            progressInfo.setEndTime(LocalDateTime.now());
            progressInfo.setLastUpdated(LocalDateTime.now());
            
            logger.info("Cancelled operation: {}", progressId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error cancelling operation {}: {}", progressId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void cleanupOldProgress(int retentionMinutes) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(retentionMinutes);
            List<String> toRemove = new ArrayList<>();
            
            for (Map.Entry<String, ProgressInfo> entry : progressMap.entrySet()) {
                ProgressInfo progressInfo = entry.getValue();
                
                // Remove completed, failed, or cancelled operations older than retention time
                if (("COMPLETED".equals(progressInfo.getStatus()) || 
                     "FAILED".equals(progressInfo.getStatus()) || 
                     "CANCELLED".equals(progressInfo.getStatus())) &&
                    progressInfo.getEndTime() != null &&
                    progressInfo.getEndTime().isBefore(cutoffTime)) {
                    
                    toRemove.add(entry.getKey());
                }
            }
            
            for (String progressId : toRemove) {
                progressMap.remove(progressId);
                cancellationFlags.remove(progressId);
            }
            
            if (!toRemove.isEmpty()) {
                logger.info("Cleaned up {} old progress indicators", toRemove.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during progress cleanup: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public <T> CompletableFuture<T> executeWithProgress(String operationId, String operationType, 
                                                       String description, ProgressAwareOperation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            String progressId = createProgressIndicator(operationId, operationType, 100, description);
            
            try {
                ProgressCallback callback = new ProgressCallbackImpl(progressId);
                T result = operation.execute(callback);
                
                completeProgress(progressId, "Operation completed successfully");
                return result;
                
            } catch (Exception e) {
                failProgress(progressId, e.getMessage());
                throw new RuntimeException("Operation failed", e);
            }
        });
    }
    
    // Scheduled cleanup with default retention
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void scheduledCleanup() {
        cleanupOldProgress(30); // 30 minutes default retention
    }
    
    // Private helper methods
    
    private String generateProgressId(String operationId) {
        return operationId + "_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(new Random().nextInt());
    }
    
    private void updateEstimatedTime(ProgressInfo progressInfo) {
        try {
            if (progressInfo.getStartTime() != null && progressInfo.getPercentage() > 0) {
                long elapsedMs = java.time.Duration.between(
                    progressInfo.getStartTime(), LocalDateTime.now()).toMillis();
                
                if (progressInfo.getPercentage() < 100.0) {
                    long estimatedTotalMs = (long) (elapsedMs / (progressInfo.getPercentage() / 100.0));
                    long estimatedRemainingMs = estimatedTotalMs - elapsedMs;
                    
                    progressInfo.setEstimatedRemainingMs(Math.max(0, estimatedRemainingMs));
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error updating estimated time for {}: {}", progressInfo.getProgressId(), e.getMessage());
        }
    }
    
    private Map<String, Object> progressInfoToMap(ProgressInfo progressInfo) {
        Map<String, Object> map = new HashMap<>();
        
        map.put("progressId", progressInfo.getProgressId());
        map.put("operationId", progressInfo.getOperationId());
        map.put("operationType", progressInfo.getOperationType());
        map.put("description", progressInfo.getDescription());
        map.put("status", progressInfo.getStatus());
        map.put("percentage", progressInfo.getPercentage());
        map.put("currentStep", progressInfo.getCurrentStep());
        map.put("totalSteps", progressInfo.getTotalSteps());
        map.put("message", progressInfo.getMessage());
        map.put("startTime", progressInfo.getStartTime());
        map.put("endTime", progressInfo.getEndTime());
        map.put("lastUpdated", progressInfo.getLastUpdated());
        map.put("executionTimeMs", progressInfo.getExecutionTimeMs());
        map.put("estimatedRemainingMs", progressInfo.getEstimatedRemainingMs());
        
        if (progressInfo.getResult() != null) {
            map.put("result", progressInfo.getResult());
        }
        
        if (progressInfo.getError() != null) {
            map.put("error", progressInfo.getError());
        }
        
        return map;
    }
    
    // Inner classes
    
    private class ProgressCallbackImpl implements ProgressCallback {
        private final String progressId;
        
        public ProgressCallbackImpl(String progressId) {
            this.progressId = progressId;
        }
        
        @Override
        public void updateProgress(int currentStep, int totalSteps, String message) {
            ProgressIndicatorServiceImpl.this.updateProgress(progressId, currentStep, message);
        }
        
        @Override
        public void updateProgress(double percentage, String message) {
            ProgressIndicatorServiceImpl.this.updateProgress(progressId, percentage, message);
        }
        
        @Override
        public boolean isCancelled() {
            AtomicBoolean cancellationFlag = cancellationFlags.get(progressId);
            return cancellationFlag != null && cancellationFlag.get();
        }
    }
    
    private static class ProgressInfo {
        private String progressId;
        private String operationId;
        private String operationType;
        private String description;
        private String status;
        private double percentage;
        private int currentStep;
        private int totalSteps;
        private String message;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime lastUpdated;
        private long executionTimeMs;
        private long estimatedRemainingMs;
        private String result;
        private String error;
        
        // Getters and setters
        public String getProgressId() { return progressId; }
        public void setProgressId(String progressId) { this.progressId = progressId; }
        
        public String getOperationId() { return operationId; }
        public void setOperationId(String operationId) { this.operationId = operationId; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        
        public int getCurrentStep() { return currentStep; }
        public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
        
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        
        public long getEstimatedRemainingMs() { return estimatedRemainingMs; }
        public void setEstimatedRemainingMs(long estimatedRemainingMs) { this.estimatedRemainingMs = estimatedRemainingMs; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}