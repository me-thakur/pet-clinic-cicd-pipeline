package com.petclinic.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO for export operation results
 * Contains the exported data and metadata
 * Validates: Requirements 4.1, 4.3
 */
public class ExportResult {
    
    private String exportId;
    private byte[] data;
    private String filename;
    private String contentType;
    private long fileSizeBytes;
    private LocalDateTime completionTime;
    private long executionTimeMs;
    private boolean success;
    private String errorMessage;
    
    public ExportResult() {}
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ExportResult result = new ExportResult();
        
        public Builder exportId(String exportId) {
            result.exportId = exportId;
            return this;
        }
        
        public Builder data(byte[] data) {
            result.data = data;
            result.fileSizeBytes = data != null ? data.length : 0;
            return this;
        }
        
        public Builder filename(String filename) {
            result.filename = filename;
            return this;
        }
        
        public Builder contentType(String contentType) {
            result.contentType = contentType;
            return this;
        }
        
        public Builder completionTime(LocalDateTime completionTime) {
            result.completionTime = completionTime;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            result.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder success(boolean success) {
            result.success = success;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            result.success = false;
            return this;
        }
        
        public ExportResult build() {
            if (result.completionTime == null) {
                result.completionTime = LocalDateTime.now();
            }
            return result;
        }
    }
    
    // Getters and setters
    public String getExportId() {
        return exportId;
    }
    
    public void setExportId(String exportId) {
        this.exportId = exportId;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        this.data = data;
        this.fileSizeBytes = data != null ? data.length : 0;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public LocalDateTime getCompletionTime() {
        return completionTime;
    }
    
    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
    }
    
    @Override
    public String toString() {
        return "ExportResult{" +
                "exportId='" + exportId + '\'' +
                ", filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", executionTimeMs=" + executionTimeMs +
                ", success=" + success +
                '}';
    }
}