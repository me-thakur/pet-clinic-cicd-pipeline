package com.petclinic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for data seeding
 * Allows customization of data seeding behavior through application properties
 * 
 * Validates: Requirements 8.3, 8.4
 */
@Configuration
@ConfigurationProperties(prefix = "pet-clinic.data-seeding")
public class DataSeedingConfig {
    
    /**
     * Whether data seeding is enabled
     */
    private boolean enabled = true;
    
    /**
     * Whether to skip seeding if data already exists
     */
    private boolean skipIfDataExists = true;
    
    /**
     * Number of owners to seed
     */
    private int ownerCount = 15;
    
    /**
     * Number of veterinarians to seed
     */
    private int veterinarianCount = 10;
    
    /**
     * Maximum number of pets per owner
     */
    private int maxPetsPerOwner = 3;
    
    /**
     * Maximum number of visits per pet
     */
    private int maxVisitsPerPet = 5;
    
    /**
     * Number of months in the past to generate visits
     */
    private int pastMonths = 6;
    
    /**
     * Number of months in the future to generate visits
     */
    private int futureMonths = 3;
    
    /**
     * Whether to generate detailed medical histories
     */
    private boolean generateMedicalHistory = true;
    
    /**
     * Whether to seed user accounts
     */
    private boolean seedUsers = true;
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isSkipIfDataExists() {
        return skipIfDataExists;
    }
    
    public void setSkipIfDataExists(boolean skipIfDataExists) {
        this.skipIfDataExists = skipIfDataExists;
    }
    
    public int getOwnerCount() {
        return ownerCount;
    }
    
    public void setOwnerCount(int ownerCount) {
        this.ownerCount = ownerCount;
    }
    
    public int getVeterinarianCount() {
        return veterinarianCount;
    }
    
    public void setVeterinarianCount(int veterinarianCount) {
        this.veterinarianCount = veterinarianCount;
    }
    
    public int getMaxPetsPerOwner() {
        return maxPetsPerOwner;
    }
    
    public void setMaxPetsPerOwner(int maxPetsPerOwner) {
        this.maxPetsPerOwner = maxPetsPerOwner;
    }
    
    public int getMaxVisitsPerPet() {
        return maxVisitsPerPet;
    }
    
    public void setMaxVisitsPerPet(int maxVisitsPerPet) {
        this.maxVisitsPerPet = maxVisitsPerPet;
    }
    
    public int getPastMonths() {
        return pastMonths;
    }
    
    public void setPastMonths(int pastMonths) {
        this.pastMonths = pastMonths;
    }
    
    public int getFutureMonths() {
        return futureMonths;
    }
    
    public void setFutureMonths(int futureMonths) {
        this.futureMonths = futureMonths;
    }
    
    public boolean isGenerateMedicalHistory() {
        return generateMedicalHistory;
    }
    
    public void setGenerateMedicalHistory(boolean generateMedicalHistory) {
        this.generateMedicalHistory = generateMedicalHistory;
    }
    
    public boolean isSeedUsers() {
        return seedUsers;
    }
    
    public void setSeedUsers(boolean seedUsers) {
        this.seedUsers = seedUsers;
    }
}