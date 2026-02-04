package com.petclinic.backend.service;

import com.petclinic.backend.dto.VeterinarianFilterCriteria;
import com.petclinic.backend.dto.VeterinarianFilterResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for veterinarian filtering operations
 * Provides comprehensive filtering by specialties, availability, and experience
 * 
 * Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5
 */
public interface VeterinarianFilterService {
    
    /**
     * Filter veterinarians based on criteria with pagination and sorting
     * @param criteria Filter criteria
     * @param pageable Pagination and sorting parameters
     * @return Filtered results with metadata
     */
    VeterinarianFilterResult filterVeterinarians(VeterinarianFilterCriteria criteria, Pageable pageable);
    
    /**
     * Get available filter options for dropdowns
     * @return Map of filter type to available options
     */
    Map<String, List<String>> getAvailableFilterOptions();
    
    /**
     * Generate suggestions for broadening criteria when no results found
     * @param criteria Current filter criteria
     * @return List of suggestions
     */
    List<String> generateSuggestions(VeterinarianFilterCriteria criteria);
    
    /**
     * Get result counts for each filter category
     * @param criteria Filter criteria
     * @return Map of filter category to count
     */
    Map<String, Long> getResultCounts(VeterinarianFilterCriteria criteria);
    
    /**
     * Persist filter state for user session
     * @param criteria Filter criteria to persist
     * @param userId User ID (optional)
     * @return Filter state data
     */
    Map<String, Object> persistFilterState(VeterinarianFilterCriteria criteria, String userId);
    
    /**
     * Restore filter state from user session
     * @param userId User ID (optional)
     * @return Restored filter criteria
     */
    VeterinarianFilterCriteria restoreFilterState(String userId);
}