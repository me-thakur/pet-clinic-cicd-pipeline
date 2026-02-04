package com.petclinic.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Frontend search criteria for senior pet search functionality
 * Supports filtering by age, species, and health conditions
 * Validates: Requirements 2.1, 2.2
 */
public class SeniorPetSearchCriteria {
    
    @JsonProperty("minAge")
    private Integer minAge;
    
    @JsonProperty("maxAge")
    private Integer maxAge;
    
    @JsonProperty("species")
    private String species;
    
    @JsonProperty("breed")
    private String breed;
    
    @JsonProperty("healthCondition")
    private String healthCondition;
    
    @JsonProperty("healthConditions")
    private List<String> healthConditions;
    
    @JsonProperty("ownerId")
    private Long ownerId;
    
    @JsonProperty("ownerName")
    private String ownerName;
    
    @JsonProperty("hasRecentVisit")
    private Boolean hasRecentVisit;
    
    @JsonProperty("needsSpecialCare")
    private Boolean needsSpecialCare;
    
    @JsonProperty("searchTerm")
    private String searchTerm;
    
    @JsonProperty("sortBy")
    private String sortBy = "age";
    
    @JsonProperty("sortDirection")
    private String sortDirection = "desc";
    
    // Constructors
    public SeniorPetSearchCriteria() {
    }
    
    public SeniorPetSearchCriteria(Integer minAge, Integer maxAge, String species, String healthCondition) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.species = species;
        this.healthCondition = healthCondition;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private SeniorPetSearchCriteria criteria = new SeniorPetSearchCriteria();
        
        public Builder minAge(Integer minAge) {
            criteria.minAge = minAge;
            return this;
        }
        
        public Builder maxAge(Integer maxAge) {
            criteria.maxAge = maxAge;
            return this;
        }
        
        public Builder species(String species) {
            criteria.species = species;
            return this;
        }
        
        public Builder breed(String breed) {
            criteria.breed = breed;
            return this;
        }
        
        public Builder healthCondition(String healthCondition) {
            criteria.healthCondition = healthCondition;
            return this;
        }
        
        public Builder healthConditions(List<String> healthConditions) {
            criteria.healthConditions = healthConditions;
            return this;
        }
        
        public Builder ownerId(Long ownerId) {
            criteria.ownerId = ownerId;
            return this;
        }
        
        public Builder ownerName(String ownerName) {
            criteria.ownerName = ownerName;
            return this;
        }
        
        public Builder hasRecentVisit(Boolean hasRecentVisit) {
            criteria.hasRecentVisit = hasRecentVisit;
            return this;
        }
        
        public Builder needsSpecialCare(Boolean needsSpecialCare) {
            criteria.needsSpecialCare = needsSpecialCare;
            return this;
        }
        
        public Builder searchTerm(String searchTerm) {
            criteria.searchTerm = searchTerm;
            return this;
        }
        
        public Builder sortBy(String sortBy) {
            criteria.sortBy = sortBy;
            return this;
        }
        
        public Builder sortDirection(String sortDirection) {
            criteria.sortDirection = sortDirection;
            return this;
        }
        
        public SeniorPetSearchCriteria build() {
            return criteria;
        }
    }
    
    // Getters and Setters
    public Integer getMinAge() {
        return minAge;
    }
    
    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }
    
    public Integer getMaxAge() {
        return maxAge;
    }
    
    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }
    
    public String getSpecies() {
        return species;
    }
    
    public void setSpecies(String species) {
        this.species = species;
    }
    
    public String getBreed() {
        return breed;
    }
    
    public void setBreed(String breed) {
        this.breed = breed;
    }
    
    public String getHealthCondition() {
        return healthCondition;
    }
    
    public void setHealthCondition(String healthCondition) {
        this.healthCondition = healthCondition;
    }
    
    public List<String> getHealthConditions() {
        return healthConditions;
    }
    
    public void setHealthConditions(List<String> healthConditions) {
        this.healthConditions = healthConditions;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public Boolean getHasRecentVisit() {
        return hasRecentVisit;
    }
    
    public void setHasRecentVisit(Boolean hasRecentVisit) {
        this.hasRecentVisit = hasRecentVisit;
    }
    
    public Boolean getNeedsSpecialCare() {
        return needsSpecialCare;
    }
    
    public void setNeedsSpecialCare(Boolean needsSpecialCare) {
        this.needsSpecialCare = needsSpecialCare;
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }
    
    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
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
    
    @Override
    public String toString() {
        return "SeniorPetSearchCriteria{" +
                "minAge=" + minAge +
                ", maxAge=" + maxAge +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", healthCondition='" + healthCondition + '\'' +
                ", searchTerm='" + searchTerm + '\'' +
                '}';
    }
}