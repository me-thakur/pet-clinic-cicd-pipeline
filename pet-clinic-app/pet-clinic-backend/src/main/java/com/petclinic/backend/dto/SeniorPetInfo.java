package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for senior pet information with enhanced details
 * Used for senior pet search results with highlighting and health indicators
 * Validates: Requirements 2.1, 2.2, 2.3
 */
public class SeniorPetInfo {
    
    @JsonProperty("petId")
    private Long petId;
    
    @JsonProperty("petName")
    private String petName;
    
    @JsonProperty("species")
    private String species;
    
    @JsonProperty("breed")
    private String breed;
    
    @JsonProperty("birthDate")
    private LocalDate birthDate;
    
    @JsonProperty("age")
    private Integer age;
    
    @JsonProperty("isSenior")
    private Boolean isSenior;
    
    @JsonProperty("seniorThreshold")
    private Integer seniorThreshold;
    
    @JsonProperty("medicalHistory")
    private String medicalHistory;
    
    @JsonProperty("healthConditions")
    private List<String> healthConditions;
    
    @JsonProperty("ownerId")
    private Long ownerId;
    
    @JsonProperty("ownerName")
    private String ownerName;
    
    @JsonProperty("ownerEmail")
    private String ownerEmail;
    
    @JsonProperty("ownerPhone")
    private String ownerPhone;
    
    @JsonProperty("lastVisitDate")
    private LocalDate lastVisitDate;
    
    @JsonProperty("upcomingVisitDate")
    private LocalDate upcomingVisitDate;
    
    @JsonProperty("visitCount")
    private Integer visitCount;
    
    @JsonProperty("ageHighlighted")
    private Boolean ageHighlighted = false;
    
    @JsonProperty("healthHighlighted")
    private Boolean healthHighlighted = false;
    
    @JsonProperty("specialCareNeeded")
    private Boolean specialCareNeeded = false;
    
    @JsonProperty("createdAt")
    private LocalDate createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDate updatedAt;
    
    // Constructors
    public SeniorPetInfo() {
    }
    
    public SeniorPetInfo(Long petId, String petName, String species, String breed, 
                        LocalDate birthDate, Integer age, Boolean isSenior) {
        this.petId = petId;
        this.petName = petName;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.age = age;
        this.isSenior = isSenior;
    }
    
    // Getters and Setters
    public Long getPetId() {
        return petId;
    }
    
    public void setPetId(Long petId) {
        this.petId = petId;
    }
    
    public String getPetName() {
        return petName;
    }
    
    public void setPetName(String petName) {
        this.petName = petName;
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
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public Boolean getIsSenior() {
        return isSenior;
    }
    
    public void setIsSenior(Boolean isSenior) {
        this.isSenior = isSenior;
    }
    
    public Integer getSeniorThreshold() {
        return seniorThreshold;
    }
    
    public void setSeniorThreshold(Integer seniorThreshold) {
        this.seniorThreshold = seniorThreshold;
    }
    
    public String getMedicalHistory() {
        return medicalHistory;
    }
    
    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
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
    
    public String getOwnerEmail() {
        return ownerEmail;
    }
    
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
    
    public String getOwnerPhone() {
        return ownerPhone;
    }
    
    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }
    
    public LocalDate getLastVisitDate() {
        return lastVisitDate;
    }
    
    public void setLastVisitDate(LocalDate lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }
    
    public LocalDate getUpcomingVisitDate() {
        return upcomingVisitDate;
    }
    
    public void setUpcomingVisitDate(LocalDate upcomingVisitDate) {
        this.upcomingVisitDate = upcomingVisitDate;
    }
    
    public Integer getVisitCount() {
        return visitCount;
    }
    
    public void setVisitCount(Integer visitCount) {
        this.visitCount = visitCount;
    }
    
    public Boolean getAgeHighlighted() {
        return ageHighlighted;
    }
    
    public void setAgeHighlighted(Boolean ageHighlighted) {
        this.ageHighlighted = ageHighlighted;
    }
    
    public Boolean getHealthHighlighted() {
        return healthHighlighted;
    }
    
    public void setHealthHighlighted(Boolean healthHighlighted) {
        this.healthHighlighted = healthHighlighted;
    }
    
    public Boolean getSpecialCareNeeded() {
        return specialCareNeeded;
    }
    
    public void setSpecialCareNeeded(Boolean specialCareNeeded) {
        this.specialCareNeeded = specialCareNeeded;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "SeniorPetInfo{" +
                "petId=" + petId +
                ", petName='" + petName + '\'' +
                ", species='" + species + '\'' +
                ", age=" + age +
                ", isSenior=" + isSenior +
                ", ownerName='" + ownerName + '\'' +
                '}';
    }
}