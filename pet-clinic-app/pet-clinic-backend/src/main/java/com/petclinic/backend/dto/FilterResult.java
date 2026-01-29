package com.petclinic.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * DTO representing the result of applying filters
 * Validates: Requirements 4.2, 4.5
 */
public class FilterResult {
    
    private List<FilterCriteria> appliedFilters;
    private List<SearchResult> results;
    private Map<String, Integer> resultsByType;
    private int totalResults;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasMore;
    private LocalDateTime filterTime;
    private long executionTimeMs;
    private String sortBy;
    private String sortDirection;
    private Map<String, Object> aggregations;
    
    // Constructors
    public FilterResult() {
        this.filterTime = LocalDateTime.now();
    }
    
    public FilterResult(List<FilterCriteria> appliedFilters, List<SearchResult> results) {
        this();
        this.appliedFilters = appliedFilters;
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
    }
    
    public FilterResult(List<FilterCriteria> appliedFilters, List<SearchResult> results, 
                       Map<String, Integer> resultsByType, int page, int size, int totalPages) {
        this(appliedFilters, results);
        this.resultsByType = resultsByType;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.hasMore = page < totalPages - 1;
    }
    
    // Getters and Setters
    public List<FilterCriteria> getAppliedFilters() {
        return appliedFilters;
    }
    
    public void setAppliedFilters(List<FilterCriteria> appliedFilters) {
        this.appliedFilters = appliedFilters;
    }
    
    public List<SearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
    }
    
    public Map<String, Integer> getResultsByType() {
        return resultsByType;
    }
    
    public void setResultsByType(Map<String, Integer> resultsByType) {
        this.resultsByType = resultsByType;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        this.hasMore = page < totalPages - 1;
    }
    
    public boolean isHasMore() {
        return hasMore;
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    public LocalDateTime getFilterTime() {
        return filterTime;
    }
    
    public void setFilterTime(LocalDateTime filterTime) {
        this.filterTime = filterTime;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
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
    
    public Map<String, Object> getAggregations() {
        return aggregations;
    }
    
    public void setAggregations(Map<String, Object> aggregations) {
        this.aggregations = aggregations;
    }
    
    // Business Methods
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }
    
    public boolean hasFilters() {
        return appliedFilters != null && !appliedFilters.isEmpty();
    }
    
    public int getFilterCount() {
        return appliedFilters != null ? appliedFilters.size() : 0;
    }
    
    public int getResultCount(String entityType) {
        return resultsByType != null ? resultsByType.getOrDefault(entityType, 0) : 0;
    }
    
    public int getPetCount() {
        return getResultCount("Pet");
    }
    
    public int getVisitCount() {
        return getResultCount("Visit");
    }
    
    public int getVeterinarianCount() {
        return getResultCount("Veterinarian");
    }
    
    public int getOwnerCount() {
        return getResultCount("Owner");
    }
    
    public boolean isEmpty() {
        return totalResults == 0;
    }
    
    public boolean isPaginated() {
        return totalPages > 1;
    }
    
    public boolean isFirstPage() {
        return page == 0;
    }
    
    public boolean isLastPage() {
        return page >= totalPages - 1;
    }
    
    public int getNextPage() {
        return isLastPage() ? page : page + 1;
    }
    
    public int getPreviousPage() {
        return isFirstPage() ? page : page - 1;
    }
    
    public boolean hasAggregations() {
        return aggregations != null && !aggregations.isEmpty();
    }
    
    public Object getAggregation(String key) {
        return aggregations != null ? aggregations.get(key) : null;
    }
    
    public boolean isSorted() {
        return sortBy != null && !sortBy.trim().isEmpty();
    }
    
    public boolean isAscending() {
        return "asc".equalsIgnoreCase(sortDirection);
    }
    
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(sortDirection);
    }
    
    public List<FilterCriteria> getTextFilters() {
        return appliedFilters != null ? 
               appliedFilters.stream().filter(FilterCriteria::isTextFilter).collect(Collectors.toList()) : 
               new ArrayList<>();
    }
    
    public List<FilterCriteria> getNumericFilters() {
        return appliedFilters != null ? 
               appliedFilters.stream().filter(FilterCriteria::isNumericFilter).collect(Collectors.toList()) : 
               new ArrayList<>();
    }
    
    public List<FilterCriteria> getDateFilters() {
        return appliedFilters != null ? 
               appliedFilters.stream().filter(FilterCriteria::isDateFilter).collect(Collectors.toList()) : 
               new ArrayList<>();
    }
    
    public List<FilterCriteria> getFiltersByEntityType(String entityType) {
        return appliedFilters != null ? 
               appliedFilters.stream()
                   .filter(f -> entityType.equalsIgnoreCase(f.getEntityType()))
                   .collect(Collectors.toList()) : 
               new ArrayList<>();
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterResult that = (FilterResult) o;
        return Objects.equals(appliedFilters, that.appliedFilters) &&
               Objects.equals(filterTime, that.filterTime) &&
               totalResults == that.totalResults;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(appliedFilters, filterTime, totalResults);
    }
    
    @Override
    public String toString() {
        return "FilterResult{" +
                "appliedFilters=" + getFilterCount() +
                ", totalResults=" + totalResults +
                ", resultsByType=" + resultsByType +
                ", page=" + page +
                ", totalPages=" + totalPages +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}