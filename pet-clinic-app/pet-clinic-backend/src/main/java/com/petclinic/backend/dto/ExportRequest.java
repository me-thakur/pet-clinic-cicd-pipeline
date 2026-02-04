package com.petclinic.backend.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for export requests
 * Contains all parameters needed for report export
 * Validates: Requirements 4.1, 4.2
 */
public class ExportRequest {
    
    private String reportType;
    private String format;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> filters;
    private String requestedBy;
    private boolean includeTrends;
    private Long veterinarianId;
    private String species;
    
    public ExportRequest() {}
    
    public ExportRequest(String reportType, String format, LocalDate startDate, LocalDate endDate) {
        this.reportType = reportType;
        this.format = format;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and setters
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
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Map<String, Object> getFilters() {
        return filters;
    }
    
    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
    
    public String getRequestedBy() {
        return requestedBy;
    }
    
    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    public boolean isIncludeTrends() {
        return includeTrends;
    }
    
    public void setIncludeTrends(boolean includeTrends) {
        this.includeTrends = includeTrends;
    }
    
    public Long getVeterinarianId() {
        return veterinarianId;
    }
    
    public void setVeterinarianId(Long veterinarianId) {
        this.veterinarianId = veterinarianId;
    }
    
    public String getSpecies() {
        return species;
    }
    
    public void setSpecies(String species) {
        this.species = species;
    }
    
    @Override
    public String toString() {
        return "ExportRequest{" +
                "reportType='" + reportType + '\'' +
                ", format='" + format + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", requestedBy='" + requestedBy + '\'' +
                ", includeTrends=" + includeTrends +
                ", veterinarianId=" + veterinarianId +
                ", species='" + species + '\'' +
                '}';
    }
}