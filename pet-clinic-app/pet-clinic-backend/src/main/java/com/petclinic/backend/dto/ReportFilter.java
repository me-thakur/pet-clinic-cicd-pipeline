package com.petclinic.backend.dto;

import com.petclinic.backend.model.VisitType;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Data model for report filtering criteria
 * Supports filtering by date ranges, veterinarian, species, and other criteria
 * Validates: Requirements 5.5
 */
public class ReportFilter {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private Long veterinarianId;
    private String veterinarianName;
    private List<String> species;
    private List<VisitType> visitTypes;
    private List<Long> petIds;
    private List<Long> ownerIds;
    private Boolean completedOnly;
    private Boolean paidOnly;
    private String groupBy; // "DATE", "VETERINARIAN", "SPECIES", "VISIT_TYPE"
    private String sortBy; // "DATE", "REVENUE", "COUNT"
    private String sortDirection; // "ASC", "DESC"
    private Integer limit;
    
    // Constructors
    public ReportFilter() {}
    
    public ReportFilter(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public ReportFilter(LocalDate startDate, LocalDate endDate, Long veterinarianId) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.veterinarianId = veterinarianId;
    }
    
    // Getters and Setters
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
    
    public Long getVeterinarianId() {
        return veterinarianId;
    }
    
    public void setVeterinarianId(Long veterinarianId) {
        this.veterinarianId = veterinarianId;
    }
    
    public String getVeterinarianName() {
        return veterinarianName;
    }
    
    public void setVeterinarianName(String veterinarianName) {
        this.veterinarianName = veterinarianName;
    }
    
    public List<String> getSpecies() {
        return species;
    }
    
    public void setSpecies(List<String> species) {
        this.species = species;
    }
    
    public List<VisitType> getVisitTypes() {
        return visitTypes;
    }
    
    public void setVisitTypes(List<VisitType> visitTypes) {
        this.visitTypes = visitTypes;
    }
    
    public List<Long> getPetIds() {
        return petIds;
    }
    
    public void setPetIds(List<Long> petIds) {
        this.petIds = petIds;
    }
    
    public List<Long> getOwnerIds() {
        return ownerIds;
    }
    
    public void setOwnerIds(List<Long> ownerIds) {
        this.ownerIds = ownerIds;
    }
    
    public Boolean getCompletedOnly() {
        return completedOnly;
    }
    
    public void setCompletedOnly(Boolean completedOnly) {
        this.completedOnly = completedOnly;
    }
    
    public Boolean getPaidOnly() {
        return paidOnly;
    }
    
    public void setPaidOnly(Boolean paidOnly) {
        this.paidOnly = paidOnly;
    }
    
    public String getGroupBy() {
        return groupBy;
    }
    
    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    // Business Methods
    public boolean hasDateRange() {
        return startDate != null && endDate != null;
    }
    
    public boolean hasVeterinarianFilter() {
        return veterinarianId != null || (veterinarianName != null && !veterinarianName.trim().isEmpty());
    }
    
    public boolean hasSpeciesFilter() {
        return species != null && !species.isEmpty();
    }
    
    public boolean hasVisitTypeFilter() {
        return visitTypes != null && !visitTypes.isEmpty();
    }
    
    public boolean hasPetFilter() {
        return petIds != null && !petIds.isEmpty();
    }
    
    public boolean hasOwnerFilter() {
        return ownerIds != null && !ownerIds.isEmpty();
    }
    
    public boolean hasCompletionFilter() {
        return completedOnly != null;
    }
    
    public boolean hasPaymentFilter() {
        return paidOnly != null;
    }
    
    public boolean hasGrouping() {
        return groupBy != null && !groupBy.trim().isEmpty();
    }
    
    public boolean hasSorting() {
        return sortBy != null && !sortBy.trim().isEmpty();
    }
    
    public boolean hasLimit() {
        return limit != null && limit > 0;
    }
    
    public boolean isEmpty() {
        return !hasDateRange() && !hasVeterinarianFilter() && !hasSpeciesFilter() && 
               !hasVisitTypeFilter() && !hasPetFilter() && !hasOwnerFilter() && 
               !hasCompletionFilter() && !hasPaymentFilter();
    }
    
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (hasDateRange()) {
            desc.append("Date: ").append(startDate).append(" to ").append(endDate);
        }
        
        if (hasVeterinarianFilter()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Veterinarian: ");
            if (veterinarianName != null) {
                desc.append(veterinarianName);
            } else {
                desc.append("ID ").append(veterinarianId);
            }
        }
        
        if (hasSpeciesFilter()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Species: ").append(String.join(", ", species));
        }
        
        if (hasVisitTypeFilter()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Visit Types: ").append(visitTypes.size()).append(" selected");
        }
        
        if (hasCompletionFilter()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Completed: ").append(completedOnly ? "Yes" : "No");
        }
        
        if (hasPaymentFilter()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("Paid: ").append(paidOnly ? "Yes" : "No");
        }
        
        return desc.length() > 0 ? desc.toString() : "No filters applied";
    }
    
    // Validation methods
    public boolean isValid() {
        // Check date range validity
        if (hasDateRange() && startDate.isAfter(endDate)) {
            return false;
        }
        
        // Check limit validity
        if (hasLimit() && limit <= 0) {
            return false;
        }
        
        // Check sort direction validity
        if (sortDirection != null && !sortDirection.equals("ASC") && !sortDirection.equals("DESC")) {
            return false;
        }
        
        return true;
    }
    
    public String getValidationError() {
        if (hasDateRange() && startDate.isAfter(endDate)) {
            return "Start date must be before or equal to end date";
        }
        
        if (hasLimit() && limit <= 0) {
            return "Limit must be greater than 0";
        }
        
        if (sortDirection != null && !sortDirection.equals("ASC") && !sortDirection.equals("DESC")) {
            return "Sort direction must be 'ASC' or 'DESC'";
        }
        
        return null;
    }
    
    // Builder pattern methods for fluent API
    public ReportFilter withDateRange(LocalDate start, LocalDate end) {
        this.startDate = start;
        this.endDate = end;
        return this;
    }
    
    public ReportFilter withVeterinarian(Long veterinarianId) {
        this.veterinarianId = veterinarianId;
        return this;
    }
    
    public ReportFilter withSpecies(List<String> species) {
        this.species = species;
        return this;
    }
    
    public ReportFilter withVisitTypes(List<VisitType> visitTypes) {
        this.visitTypes = visitTypes;
        return this;
    }
    
    public ReportFilter withCompletedOnly(boolean completedOnly) {
        this.completedOnly = completedOnly;
        return this;
    }
    
    public ReportFilter withPaidOnly(boolean paidOnly) {
        this.paidOnly = paidOnly;
        return this;
    }
    
    public ReportFilter groupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }
    
    public ReportFilter sortBy(String sortBy, String direction) {
        this.sortBy = sortBy;
        this.sortDirection = direction;
        return this;
    }
    
    public ReportFilter limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportFilter that = (ReportFilter) o;
        return Objects.equals(startDate, that.startDate) &&
               Objects.equals(endDate, that.endDate) &&
               Objects.equals(veterinarianId, that.veterinarianId) &&
               Objects.equals(species, that.species) &&
               Objects.equals(visitTypes, that.visitTypes) &&
               Objects.equals(completedOnly, that.completedOnly) &&
               Objects.equals(paidOnly, that.paidOnly);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, veterinarianId, species, visitTypes, completedOnly, paidOnly);
    }
    
    @Override
    public String toString() {
        return "ReportFilter{" +
                "description='" + getDescription() + '\'' +
                ", groupBy='" + groupBy + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", limit=" + limit +
                '}';
    }
}