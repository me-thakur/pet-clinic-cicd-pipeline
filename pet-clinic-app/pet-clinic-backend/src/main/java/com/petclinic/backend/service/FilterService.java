package com.petclinic.backend.service;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.FilterResult;
import com.petclinic.backend.dto.SearchResult;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Service interface for advanced filtering functionality
 * Provides multi-criteria filtering, search history, and recent queries
 * Validates: Requirements 4.2, 4.5
 */
public interface FilterService {
    
    /**
     * Apply multiple filters with logical AND operations
     * @param filters List of filter criteria to apply
     * @return FilterResult with filtered data
     */
    FilterResult applyFilters(List<FilterCriteria> filters);
    
    /**
     * Apply multiple filters with pagination
     * @param filters List of filter criteria to apply
     * @param page Page number (0-based)
     * @param size Page size
     * @return FilterResult with paginated filtered data
     */
    FilterResult applyFilters(List<FilterCriteria> filters, int page, int size);
    
    /**
     * Apply multiple filters with pagination and sorting
     * @param filters List of filter criteria to apply
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (asc, desc)
     * @return FilterResult with sorted and paginated filtered data
     */
    FilterResult applyFilters(List<FilterCriteria> filters, int page, int size, String sortBy, String sortDirection);
    
    /**
     * Apply filters to a specific entity type
     * @param filters List of filter criteria to apply
     * @param entityType Entity type to filter (Pet, Visit, Veterinarian, Owner)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of SearchResult objects for the specified entity type
     */
    Page<SearchResult> applyFiltersToEntityType(List<FilterCriteria> filters, String entityType, int page, int size);
    
    /**
     * Filter pets based on criteria
     * @param filters List of filter criteria for pets
     * @return List of SearchResult objects for pets
     */
    List<SearchResult> filterPets(List<FilterCriteria> filters);
    
    /**
     * Filter visits based on criteria
     * @param filters List of filter criteria for visits
     * @return List of SearchResult objects for visits
     */
    List<SearchResult> filterVisits(List<FilterCriteria> filters);
    
    /**
     * Filter veterinarians based on criteria
     * @param filters List of filter criteria for veterinarians
     * @return List of SearchResult objects for veterinarians
     */
    List<SearchResult> filterVeterinarians(List<FilterCriteria> filters);
    
    /**
     * Filter owners based on criteria
     * @param filters List of filter criteria for owners
     * @return List of SearchResult objects for owners
     */
    List<SearchResult> filterOwners(List<FilterCriteria> filters);
    
    /**
     * Validate filter criteria
     * @param filters List of filter criteria to validate
     * @return List of validation error messages (empty if valid)
     */
    List<String> validateFilters(List<FilterCriteria> filters);
    
    /**
     * Sanitize filter criteria to prevent injection attacks
     * @param filters List of filter criteria to sanitize
     * @return List of sanitized filter criteria
     */
    List<FilterCriteria> sanitizeFilters(List<FilterCriteria> filters);
    
    /**
     * Get available filter fields for an entity type
     * @param entityType Entity type (Pet, Visit, Veterinarian, Owner)
     * @return Map of field names to field types
     */
    Map<String, String> getAvailableFields(String entityType);
    
    /**
     * Get available filter operators for a field type
     * @param fieldType Field type (text, numeric, date, boolean, enum)
     * @return List of available operators
     */
    List<String> getAvailableOperators(String fieldType);
    
    /**
     * Save filter combination for reuse
     * @param userId User ID (optional, can be null for anonymous)
     * @param filterName Name for the saved filter combination
     * @param filters List of filter criteria to save
     * @return ID of the saved filter combination
     */
    Long saveFilterCombination(Long userId, String filterName, List<FilterCriteria> filters);
    
    /**
     * Get saved filter combinations for a user
     * @param userId User ID (optional, can be null for anonymous)
     * @return Map of filter combination ID to filter name
     */
    Map<Long, String> getSavedFilterCombinations(Long userId);
    
    /**
     * Load saved filter combination
     * @param filterId Filter combination ID
     * @return List of filter criteria
     */
    List<FilterCriteria> loadFilterCombination(Long filterId);
    
    /**
     * Delete saved filter combination
     * @param filterId Filter combination ID
     * @param userId User ID (for authorization)
     * @return true if deleted successfully
     */
    boolean deleteFilterCombination(Long filterId, Long userId);
    
    /**
     * Get recent filter combinations used by a user
     * @param userId User ID (optional, can be null for anonymous)
     * @param maxResults Maximum number of recent combinations to return
     * @return List of recent filter combinations
     */
    List<List<FilterCriteria>> getRecentFilterCombinations(Long userId, int maxResults);
    
    /**
     * Save filter combination to history
     * @param userId User ID (optional, can be null for anonymous)
     * @param filters List of filter criteria to save to history
     */
    void saveFilterToHistory(Long userId, List<FilterCriteria> filters);
    
    /**
     * Get popular filter combinations
     * @param maxResults Maximum number of popular combinations to return
     * @return List of popular filter combinations with usage counts
     */
    List<Map<String, Object>> getPopularFilterCombinations(int maxResults);
    
    /**
     * Clear filter history for a user
     * @param userId User ID
     */
    void clearFilterHistory(Long userId);
    
    /**
     * Get filter analytics and metrics
     * @return Map containing filter analytics data
     */
    Map<String, Object> getFilterAnalytics();
    
    /**
     * Get filter suggestions based on partial criteria
     * @param partialFilter Partial filter criteria
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested filter criteria
     */
    List<FilterCriteria> getFilterSuggestions(FilterCriteria partialFilter, int maxSuggestions);
    
    /**
     * Combine search query with filters
     * @param searchQuery Search query string
     * @param filters List of filter criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @return FilterResult with combined search and filter results
     */
    FilterResult combineSearchAndFilters(String searchQuery, List<FilterCriteria> filters, int page, int size);
    
    /**
     * Get filter result aggregations (counts, averages, etc.)
     * @param filters List of filter criteria
     * @param aggregationFields Fields to aggregate
     * @return Map of aggregation results
     */
    Map<String, Object> getFilterAggregations(List<FilterCriteria> filters, List<String> aggregationFields);
    
    /**
     * Export filtered results
     * @param filters List of filter criteria
     * @param format Export format (csv, pdf, json)
     * @return Byte array of exported data
     */
    byte[] exportFilteredResults(List<FilterCriteria> filters, String format);
}