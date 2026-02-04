package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * DTO representing a bulk delete request
 * Contains the IDs to delete and current filter context
 * Validates: Requirements 6.4, 6.5
 */
public class BulkDeleteRequest {
    
    @JsonProperty("selectedIds")
    @NotEmpty(message = "At least one ID must be selected for deletion")
    private List<Long> selectedIds;
    
    @JsonProperty("selectAll")
    private boolean selectAll;
    
    @JsonProperty("currentFilters")
    private List<FilterCriteria> currentFilters;
    
    @JsonProperty("entityType")
    @NotNull(message = "Entity type is required")
    private String entityType;
    
    @JsonProperty("confirmationToken")
    private String confirmationToken;
    
    // Constructors
    public BulkDeleteRequest() {
    }
    
    public BulkDeleteRequest(List<Long> selectedIds, String entityType) {
        this.selectedIds = selectedIds;
        this.entityType = entityType;
        this.selectAll = false;
    }
    
    public BulkDeleteRequest(List<Long> selectedIds, boolean selectAll, List<FilterCriteria> currentFilters, String entityType) {
        this.selectedIds = selectedIds;
        this.selectAll = selectAll;
        this.currentFilters = currentFilters;
        this.entityType = entityType;
    }
    
    // Getters and Setters
    public List<Long> getSelectedIds() {
        return selectedIds;
    }
    
    public void setSelectedIds(List<Long> selectedIds) {
        this.selectedIds = selectedIds;
    }
    
    public boolean isSelectAll() {
        return selectAll;
    }
    
    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }
    
    public List<FilterCriteria> getCurrentFilters() {
        return currentFilters;
    }
    
    public void setCurrentFilters(List<FilterCriteria> currentFilters) {
        this.currentFilters = currentFilters;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getConfirmationToken() {
        return confirmationToken;
    }
    
    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }
    
    // Business Methods
    public boolean hasSelectedIds() {
        return selectedIds != null && !selectedIds.isEmpty();
    }
    
    public int getSelectedCount() {
        return selectedIds != null ? selectedIds.size() : 0;
    }
    
    public boolean hasFilters() {
        return currentFilters != null && !currentFilters.isEmpty();
    }
    
    public boolean isValidEntityType() {
        if (entityType == null) return false;
        
        String type = entityType.toLowerCase();
        return type.equals("visits") || type.equals("owners") || 
               type.equals("pets") || type.equals("veterinarians");
    }
    
    public boolean isValid() {
        return hasSelectedIds() && 
               entityType != null && 
               isValidEntityType();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkDeleteRequest that = (BulkDeleteRequest) o;
        return selectAll == that.selectAll &&
               Objects.equals(selectedIds, that.selectedIds) &&
               Objects.equals(currentFilters, that.currentFilters) &&
               Objects.equals(entityType, that.entityType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(selectedIds, selectAll, currentFilters, entityType);
    }
    
    @Override
    public String toString() {
        return "BulkDeleteRequest{" +
                "selectedIds=" + selectedIds +
                ", selectAll=" + selectAll +
                ", currentFilters=" + currentFilters +
                ", entityType='" + entityType + '\'' +
                ", confirmationToken='" + (confirmationToken != null ? "[REDACTED]" : null) + '\'' +
                '}';
    }
}