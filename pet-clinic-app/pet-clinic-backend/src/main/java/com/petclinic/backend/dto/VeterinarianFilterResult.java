package com.petclinic.backend.dto;

import com.petclinic.backend.model.Veterinarian;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * DTO for veterinarian filter results
 * Contains filtered veterinarians and metadata for UI display
 * 
 * Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5
 */
public class VeterinarianFilterResult {
    
    /**
     * Filtered veterinarians
     */
    private Page<Veterinarian> veterinarians;
    
    /**
     * Applied filter criteria
     */
    private VeterinarianFilterCriteria appliedFilters;
    
    /**
     * Available filter options for dropdowns
     */
    private Map<String, List<String>> availableFilterOptions;
    
    /**
     * Result counts by filter category
     */
    private Map<String, Long> resultCounts;
    
    /**
     * Suggestions for broadening criteria when no results found
     */
    private List<String> suggestions;
    
    /**
     * Filter state persistence data
     */
    private Map<String, Object> filterState;
    
    /**
     * Total results before pagination
     */
    private long totalResults;
    
    /**
     * Execution time in milliseconds
     */
    private long executionTimeMs;
    
    // Constructors
    public VeterinarianFilterResult() {}
    
    public VeterinarianFilterResult(Page<Veterinarian> veterinarians, VeterinarianFilterCriteria appliedFilters,
                                   Map<String, List<String>> availableFilterOptions, Map<String, Long> resultCounts,
                                   List<String> suggestions, Map<String, Object> filterState, 
                                   long totalResults, long executionTimeMs) {
        this.veterinarians = veterinarians;
        this.appliedFilters = appliedFilters;
        this.availableFilterOptions = availableFilterOptions;
        this.resultCounts = resultCounts;
        this.suggestions = suggestions;
        this.filterState = filterState;
        this.totalResults = totalResults;
        this.executionTimeMs = executionTimeMs;
    }
    
    // Builder pattern
    public static VeterinarianFilterResultBuilder builder() {
        return new VeterinarianFilterResultBuilder();
    }
    
    public static class VeterinarianFilterResultBuilder {
        private Page<Veterinarian> veterinarians;
        private VeterinarianFilterCriteria appliedFilters;
        private Map<String, List<String>> availableFilterOptions;
        private Map<String, Long> resultCounts;
        private List<String> suggestions;
        private Map<String, Object> filterState;
        private long totalResults;
        private long executionTimeMs;
        
        public VeterinarianFilterResultBuilder veterinarians(Page<Veterinarian> veterinarians) {
            this.veterinarians = veterinarians;
            return this;
        }
        
        public VeterinarianFilterResultBuilder appliedFilters(VeterinarianFilterCriteria appliedFilters) {
            this.appliedFilters = appliedFilters;
            return this;
        }
        
        public VeterinarianFilterResultBuilder availableFilterOptions(Map<String, List<String>> availableFilterOptions) {
            this.availableFilterOptions = availableFilterOptions;
            return this;
        }
        
        public VeterinarianFilterResultBuilder resultCounts(Map<String, Long> resultCounts) {
            this.resultCounts = resultCounts;
            return this;
        }
        
        public VeterinarianFilterResultBuilder suggestions(List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }
        
        public VeterinarianFilterResultBuilder filterState(Map<String, Object> filterState) {
            this.filterState = filterState;
            return this;
        }
        
        public VeterinarianFilterResultBuilder totalResults(long totalResults) {
            this.totalResults = totalResults;
            return this;
        }
        
        public VeterinarianFilterResultBuilder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public VeterinarianFilterResult build() {
            return new VeterinarianFilterResult(veterinarians, appliedFilters, availableFilterOptions,
                resultCounts, suggestions, filterState, totalResults, executionTimeMs);
        }
    }
    
    // Getters and Setters
    public Page<Veterinarian> getVeterinarians() { return veterinarians; }
    public void setVeterinarians(Page<Veterinarian> veterinarians) { this.veterinarians = veterinarians; }
    
    public VeterinarianFilterCriteria getAppliedFilters() { return appliedFilters; }
    public void setAppliedFilters(VeterinarianFilterCriteria appliedFilters) { this.appliedFilters = appliedFilters; }
    
    public Map<String, List<String>> getAvailableFilterOptions() { return availableFilterOptions; }
    public void setAvailableFilterOptions(Map<String, List<String>> availableFilterOptions) { this.availableFilterOptions = availableFilterOptions; }
    
    public Map<String, Long> getResultCounts() { return resultCounts; }
    public void setResultCounts(Map<String, Long> resultCounts) { this.resultCounts = resultCounts; }
    
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    
    public Map<String, Object> getFilterState() { return filterState; }
    public void setFilterState(Map<String, Object> filterState) { this.filterState = filterState; }
    
    public long getTotalResults() { return totalResults; }
    public void setTotalResults(long totalResults) { this.totalResults = totalResults; }
    
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    
    /**
     * Check if results are empty
     */
    public boolean isEmpty() {
        return veterinarians == null || veterinarians.isEmpty();
    }
    
    /**
     * Check if suggestions should be shown
     */
    public boolean shouldShowSuggestions() {
        return isEmpty() && appliedFilters != null && appliedFilters.hasFilters();
    }
}