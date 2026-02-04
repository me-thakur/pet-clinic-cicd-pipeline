package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DTO representing the result of a bulk operation
 * Contains success status, counts, and error information
 * Validates: Requirements 6.5, 6.6
 */
public class BulkOperationResult {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("deletedCount")
    private int deletedCount;
    
    @JsonProperty("failedCount")
    private int failedCount;
    
    @JsonProperty("totalRequested")
    private int totalRequested;
    
    @JsonProperty("errors")
    private List<String> errors;
    
    @JsonProperty("warnings")
    private List<String> warnings;
    
    @JsonProperty("operationType")
    private String operationType;
    
    @JsonProperty("entityType")
    private String entityType;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("duration")
    private Long duration; // Duration in milliseconds
    
    @JsonProperty("progressPercentage")
    private Integer progressPercentage;
    
    @JsonProperty("currentItem")
    private Integer currentItem;
    
    @JsonProperty("estimatedTimeRemaining")
    private Long estimatedTimeRemaining; // Estimated time remaining in milliseconds
    
    // Constructors
    public BulkOperationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
        this.operationType = "DELETE";
    }
    
    public BulkOperationResult(boolean success, int deletedCount, int totalRequested) {
        this();
        this.success = success;
        this.deletedCount = deletedCount;
        this.totalRequested = totalRequested;
        this.failedCount = totalRequested - deletedCount;
    }
    
    public BulkOperationResult(boolean success, int deletedCount, int totalRequested, String entityType) {
        this(success, deletedCount, totalRequested);
        this.entityType = entityType;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getDeletedCount() {
        return deletedCount;
    }
    
    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
        updateFailedCount();
    }
    
    public int getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
    
    public int getTotalRequested() {
        return totalRequested;
    }
    
    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
        updateFailedCount();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Long getDuration() {
        return duration;
    }
    
    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    public Integer getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public Integer getCurrentItem() {
        return currentItem;
    }
    
    public void setCurrentItem(Integer currentItem) {
        this.currentItem = currentItem;
    }
    
    public Long getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }
    
    public void setEstimatedTimeRemaining(Long estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }
    
    // Business Methods
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.success = false; // Any error makes the operation unsuccessful
    }
    
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    public boolean isPartialSuccess() {
        return deletedCount > 0 && deletedCount < totalRequested;
    }
    
    public boolean isCompleteSuccess() {
        return success && deletedCount == totalRequested && !hasErrors();
    }
    
    public boolean isCompleteFailure() {
        return !success && deletedCount == 0;
    }
    
    public double getSuccessRate() {
        if (totalRequested == 0) {
            return 0.0;
        }
        return (double) deletedCount / totalRequested * 100.0;
    }
    
    public void updateProgress(int currentItem, long startTime) {
        this.currentItem = currentItem;
        if (totalRequested > 0) {
            this.progressPercentage = (int) ((double) currentItem / totalRequested * 100);
            
            // Calculate estimated time remaining
            if (currentItem > 0) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long avgTimePerItem = elapsedTime / currentItem;
                int remainingItems = totalRequested - currentItem;
                this.estimatedTimeRemaining = avgTimePerItem * remainingItems;
            }
        }
    }
    
    public boolean isInProgress() {
        return currentItem != null && currentItem < totalRequested;
    }
    
    public String getSummaryMessage() {
        if (isCompleteSuccess()) {
            return String.format("Successfully deleted %d %s", deletedCount, entityType != null ? entityType : "items");
        } else if (isPartialSuccess()) {
            return String.format("Partially successful: deleted %d of %d %s", 
                               deletedCount, totalRequested, entityType != null ? entityType : "items");
        } else if (isCompleteFailure()) {
            return String.format("Failed to delete any %s", entityType != null ? entityType : "items");
        } else {
            return String.format("Operation completed with mixed results: %d deleted, %d failed", 
                               deletedCount, failedCount);
        }
    }
    
    private void updateFailedCount() {
        this.failedCount = Math.max(0, totalRequested - deletedCount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkOperationResult that = (BulkOperationResult) o;
        return success == that.success &&
               deletedCount == that.deletedCount &&
               failedCount == that.failedCount &&
               totalRequested == that.totalRequested &&
               Objects.equals(operationType, that.operationType) &&
               Objects.equals(entityType, that.entityType) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, deletedCount, failedCount, totalRequested, 
                          operationType, entityType, timestamp);
    }
    
    @Override
    public String toString() {
        return "BulkOperationResult{" +
                "success=" + success +
                ", deletedCount=" + deletedCount +
                ", failedCount=" + failedCount +
                ", totalRequested=" + totalRequested +
                ", errors=" + errors +
                ", warnings=" + warnings +
                ", operationType='" + operationType + '\'' +
                ", entityType='" + entityType + '\'' +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                ", progressPercentage=" + progressPercentage +
                ", currentItem=" + currentItem +
                ", estimatedTimeRemaining=" + estimatedTimeRemaining +
                '}';
    }
}