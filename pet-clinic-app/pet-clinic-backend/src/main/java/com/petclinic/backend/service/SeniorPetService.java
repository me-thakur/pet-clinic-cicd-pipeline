package com.petclinic.backend.service;

import com.petclinic.backend.dto.SeniorPetInfo;
import com.petclinic.backend.dto.SeniorPetSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

/**
 * Service interface for senior pet management and search operations
 * Provides specialized functionality for senior pet care and monitoring
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
public interface SeniorPetService {
    
    /**
     * Search senior pets with comprehensive criteria
     * @param criteria Search criteria including age, species, and health filters
     * @param pageable Pagination parameters
     * @return Page of senior pet information with highlighting
     */
    Page<SeniorPetInfo> searchSeniorPets(SeniorPetSearchCriteria criteria, Pageable pageable);
    
    /**
     * Get all senior pets with basic age threshold
     * @param pageable Pagination parameters
     * @return Page of senior pet information
     */
    Page<SeniorPetInfo> getAllSeniorPets(Pageable pageable);
    
    /**
     * Get senior pets by species with species-specific age threshold
     * @param species Pet species
     * @param pageable Pagination parameters
     * @return Page of senior pet information
     */
    Page<SeniorPetInfo> getSeniorPetsBySpecies(String species, Pageable pageable);
    
    /**
     * Get senior pets with health conditions
     * @param healthCondition Health condition to filter by
     * @param pageable Pagination parameters
     * @return Page of senior pet information
     */
    Page<SeniorPetInfo> getSeniorPetsWithHealthCondition(String healthCondition, Pageable pageable);
    
    /**
     * Get senior pets needing special care
     * @param pageable Pagination parameters
     * @return Page of senior pet information
     */
    Page<SeniorPetInfo> getSeniorPetsNeedingSpecialCare(Pageable pageable);
    
    /**
     * Get fallback senior pets list with age sorting
     * Used when search fails - Requirement 2.5
     * @param pageable Pagination parameters
     * @return Page of senior pet information sorted by age
     */
    Page<SeniorPetInfo> getFallbackSeniorPets(Pageable pageable);
    
    /**
     * Get configurable age thresholds per species
     * @return Map of species to age threshold
     */
    Map<String, Integer> getSpeciesAgeThresholds();
    
    /**
     * Update age threshold for a species
     * @param species Pet species
     * @param threshold New age threshold
     */
    void updateSpeciesAgeThreshold(String species, Integer threshold);
    
    /**
     * Get senior pet statistics
     * @return Map containing senior pet statistics
     */
    Map<String, Object> getSeniorPetStatistics();
    
    /**
     * Get available health conditions for filtering
     * @return List of health conditions found in senior pets
     */
    List<String> getAvailableHealthConditions();
    
    /**
     * Get available species for filtering
     * @return List of species with senior pets
     */
    List<String> getAvailableSpecies();
    
    /**
     * Check if a pet qualifies as senior based on species-specific threshold
     * @param petId Pet ID
     * @return true if pet is senior, false otherwise
     */
    boolean isPetSenior(Long petId);
    
    /**
     * Get age threshold for specific species
     * @param species Pet species
     * @return Age threshold for the species
     */
    Integer getAgeThresholdForSpecies(String species);
}