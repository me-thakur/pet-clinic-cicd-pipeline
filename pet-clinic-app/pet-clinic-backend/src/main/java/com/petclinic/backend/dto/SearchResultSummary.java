package com.petclinic.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DTO representing a summary of search results with counts and aggregations
 * Validates: Requirements 4.1, 4.3
 */
public class SearchResultSummary {
    
    private String query;
    private int totalResults;
    private Map<String, Integer> resultsByType;
    private List<SearchResult> results;
    private int page;
    private int size;
    private int totalPages;
    private LocalDateTime searchTime;
    private long executionTimeMs;
    private boolean hasMore;
    private String sortBy;
    private String sortDirection;
    
    // Constructors
    public SearchResultSummary() {
        this.searchTime = LocalDateTime.now();
    }
    
    public SearchResultSummary(String query, List<SearchResult> results, Map<String, Integer> resultsByType) {
        this();
        this.query = query;
        this.results = results;
        this.resultsByType = resultsByType;
        this.totalResults = results != null ? results.size() : 0;
    }
    
    public SearchResultSummary(String query, List<SearchResult> results, Map<String, Integer> resultsByType,
                              int page, int size, int totalPages) {
        this(query, results, resultsByType);
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.hasMore = page < totalPages - 1;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public Map<String, Integer> getResultsByType() {
        return resultsByType;
    }
    
    public void setResultsByType(Map<String, Integer> resultsByType) {
        this.resultsByType = resultsByType;
    }
    
    public List<SearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
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
    
    public LocalDateTime getSearchTime() {
        return searchTime;
    }
    
    public void setSearchTime(LocalDateTime searchTime) {
        this.searchTime = searchTime;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public boolean isHasMore() {
        return hasMore;
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
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
    
    // Business Methods
    public boolean hasResults() {
        return results != null && !results.isEmpty();
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
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResultSummary that = (SearchResultSummary) o;
        return Objects.equals(query, that.query) &&
               Objects.equals(searchTime, that.searchTime) &&
               totalResults == that.totalResults;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(query, searchTime, totalResults);
    }
    
    @Override
    public String toString() {
        return "SearchResultSummary{" +
                "query='" + query + '\'' +
                ", totalResults=" + totalResults +
                ", resultsByType=" + resultsByType +
                ", page=" + page +
                ", totalPages=" + totalPages +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}