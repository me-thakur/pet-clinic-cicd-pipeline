package com.petclinic.frontend.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Frontend model for report filtering criteria
 */
public class ReportFilter {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private Long veterinarianId;
    private String veterinarianName;
    private List<VeterinarianOption> availableVeterinarians; // Available veterinarians for dropdown
    private List<String> species;
    private List<String> visitTypes;
    private List<Long> petIds;
    private List<Long> ownerIds;
    private Boolean completedOnly;
    private Boolean paidOnly;
    private String groupBy;
    private String sortBy;
    private String sortDirection;
    private Integer limit;
    
    // Constructors
    public ReportFilter() {}
    
    public ReportFilter(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
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
    
    public List<VeterinarianOption> getAvailableVeterinarians() {
        return availableVeterinarians;
    }
    
    public void setAvailableVeterinarians(List<VeterinarianOption> availableVeterinarians) {
        this.availableVeterinarians = availableVeterinarians;
    }
    
    public List<String> getSpecies() {
        return species;
    }
    
    public void setSpecies(List<String> species) {
        this.species = species;
    }
    
    public List<String> getVisitTypes() {
        return visitTypes;
    }
    
    public void setVisitTypes(List<String> visitTypes) {
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
        
        return desc.length() > 0 ? desc.toString() : "No filters applied";
    }
}