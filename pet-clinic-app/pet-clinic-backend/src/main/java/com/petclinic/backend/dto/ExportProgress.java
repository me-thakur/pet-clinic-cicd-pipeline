package com.petclinic.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO for tracking export progress
 * Provides progress indicators for long-running export operations
 * Validates: Requirements 4.2
 */
public class ExportProgress {
    
    private String exportId;
    private String reportType;
    private String format;
    private ExportStatus status;
    private int progressPercentage;
    private String currentStep;
    private String estimatedTimeRemaining;
    private LocalDateTime startTime;
    private LocalDateTime estimatedCompletionTime;
    private String errorMessage;
    private int retryCount;
    private int maxRetries;
    
    public enum ExportStatus {
        QUEUED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public ExportProgress() {}
    
    public ExportProgress(String exportId, String reportType, String format) {
        this.exportId = exportId;
        this.reportType = reportType;
        this.format = format;
        this.status = ExportStatus.QUEUED;
        this.progressPercentage = 0;
        this.startTime = LocalDateTime.now();
        this.retryCount = 0;
        this.maxRetries = 3;
    }
    
    // Getters and setters
    public String getExportId() {
        return exportId;
    }
    
    public void setExportId(String exportId) {
        this.exportId = exportId;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public ExportStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExportStatus status) {
        this.status = status;
    }
    
    public int getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
    
    public String getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }
    
    public void setEstimatedTimeRemaining(String estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }
    
    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public boolean canRetry() {
        return retryCount < maxRetries && status == ExportStatus.FAILED;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    @Override
    public String toString() {
        return "ExportProgress{" +
                "exportId='" + exportId + '\'' +
                ", reportType='" + reportType + '\'' +
                ", format='" + format + '\'' +
                ", status=" + status +
                ", progressPercentage=" + progressPercentage +
                ", currentStep='" + currentStep + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}