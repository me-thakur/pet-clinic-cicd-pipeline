package com.petclinic.backend.dto;

import java.util.List;

/**
 * DTO for veterinarian filter criteria
 * Supports filtering by specialties, availability, and experience level
 * 
 * Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5
 */
public class VeterinarianFilterCriteria {
    
    /**
     * Filter by specialties (multiple selection allowed)
     */
    private List<String> specialties;
    
    /**
     * Filter by availability status
     */
    private Boolean availableOnly;
    
    /**
     * Filter by experience level (in years)
     */
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    
    /**
     * Filter by license status
     */
    private Boolean activeLicenseOnly;
    
    /**
     * Filter by workload (maximum number of visits)
     */
    private Integer maxVisits;
    
    /**
     * Filter by emergency capability
     */
    private Boolean emergencyCapable;
    
    /**
     * Filter by surgical capability
     */
    private Boolean surgicalCapable;
    
    /**
     * Text search across name and license number
     */
    private String searchText;
    
    // Constructors
    public VeterinarianFilterCriteria() {}
    
    public VeterinarianFilterCriteria(List<String> specialties, Boolean availableOnly, 
                                     Integer minExperienceYears, Integer maxExperienceYears,
                                     Boolean activeLicenseOnly, Integer maxVisits,
                                     Boolean emergencyCapable, Boolean surgicalCapable, String searchText) {
        this.specialties = specialties;
        this.availableOnly = availableOnly;
        this.minExperienceYears = minExperienceYears;
        this.maxExperienceYears = maxExperienceYears;
        this.activeLicenseOnly = activeLicenseOnly;
        this.maxVisits = maxVisits;
        this.emergencyCapable = emergencyCapable;
        this.surgicalCapable = surgicalCapable;
        this.searchText = searchText;
    }
    
    // Builder pattern
    public static VeterinarianFilterCriteriaBuilder builder() {
        return new VeterinarianFilterCriteriaBuilder();
    }
    
    public static class VeterinarianFilterCriteriaBuilder {
        private List<String> specialties;
        private Boolean availableOnly;
        private Integer minExperienceYears;
        private Integer maxExperienceYears;
        private Boolean activeLicenseOnly;
        private Integer maxVisits;
        private Boolean emergencyCapable;
        private Boolean surgicalCapable;
        private String searchText;
        
        public VeterinarianFilterCriteriaBuilder specialties(List<String> specialties) {
            this.specialties = specialties;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder availableOnly(Boolean availableOnly) {
            this.availableOnly = availableOnly;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder minExperienceYears(Integer minExperienceYears) {
            this.minExperienceYears = minExperienceYears;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder maxExperienceYears(Integer maxExperienceYears) {
            this.maxExperienceYears = maxExperienceYears;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder activeLicenseOnly(Boolean activeLicenseOnly) {
            this.activeLicenseOnly = activeLicenseOnly;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder maxVisits(Integer maxVisits) {
            this.maxVisits = maxVisits;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder emergencyCapable(Boolean emergencyCapable) {
            this.emergencyCapable = emergencyCapable;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder surgicalCapable(Boolean surgicalCapable) {
            this.surgicalCapable = surgicalCapable;
            return this;
        }
        
        public VeterinarianFilterCriteriaBuilder searchText(String searchText) {
            this.searchText = searchText;
            return this;
        }
        
        public VeterinarianFilterCriteria build() {
            return new VeterinarianFilterCriteria(specialties, availableOnly, minExperienceYears,
                maxExperienceYears, activeLicenseOnly, maxVisits, emergencyCapable, surgicalCapable, searchText);
        }
    }
    
    // Getters and Setters
    public List<String> getSpecialties() { return specialties; }
    public void setSpecialties(List<String> specialties) { this.specialties = specialties; }
    
    public Boolean getAvailableOnly() { return availableOnly; }
    public void setAvailableOnly(Boolean availableOnly) { this.availableOnly = availableOnly; }
    
    public Integer getMinExperienceYears() { return minExperienceYears; }
    public void setMinExperienceYears(Integer minExperienceYears) { this.minExperienceYears = minExperienceYears; }
    
    public Integer getMaxExperienceYears() { return maxExperienceYears; }
    public void setMaxExperienceYears(Integer maxExperienceYears) { this.maxExperienceYears = maxExperienceYears; }
    
    public Boolean getActiveLicenseOnly() { return activeLicenseOnly; }
    public void setActiveLicenseOnly(Boolean activeLicenseOnly) { this.activeLicenseOnly = activeLicenseOnly; }
    
    public Integer getMaxVisits() { return maxVisits; }
    public void setMaxVisits(Integer maxVisits) { this.maxVisits = maxVisits; }
    
    public Boolean getEmergencyCapable() { return emergencyCapable; }
    public void setEmergencyCapable(Boolean emergencyCapable) { this.emergencyCapable = emergencyCapable; }
    
    public Boolean getSurgicalCapable() { return surgicalCapable; }
    public void setSurgicalCapable(Boolean surgicalCapable) { this.surgicalCapable = surgicalCapable; }
    
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    
    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return (specialties != null && !specialties.isEmpty()) ||
               availableOnly != null ||
               minExperienceYears != null ||
               maxExperienceYears != null ||
               activeLicenseOnly != null ||
               maxVisits != null ||
               emergencyCapable != null ||
               surgicalCapable != null ||
               (searchText != null && !searchText.trim().isEmpty());
    }
    
    /**
     * Get active filter count for UI display
     */
    public int getActiveFilterCount() {
        int count = 0;
        if (specialties != null && !specialties.isEmpty()) count++;
        if (availableOnly != null) count++;
        if (minExperienceYears != null || maxExperienceYears != null) count++;
        if (activeLicenseOnly != null) count++;
        if (maxVisits != null) count++;
        if (emergencyCapable != null) count++;
        if (surgicalCapable != null) count++;
        if (searchText != null && !searchText.trim().isEmpty()) count++;
        return count;
    }
}