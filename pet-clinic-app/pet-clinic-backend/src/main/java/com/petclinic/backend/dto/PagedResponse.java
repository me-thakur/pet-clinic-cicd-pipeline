package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper for API endpoints.
 * Provides consistent pagination metadata across all list endpoints.
 * Enhanced with sort and filter metadata for enhanced table functionality.
 * 
 * @param <T> The type of content being paginated
 */
public class PagedResponse<T> {
    
    @JsonProperty("content")
    private List<T> content;
    
    @JsonProperty("page")
    private PageInfo page;
    
    @JsonProperty("sortMetadata")
    private SortMetadata sortMetadata;
    
    @JsonProperty("multiColumnSortMetadata")
    private MultiColumnSortMetadata multiColumnSortMetadata;
    
    @JsonProperty("activeFilters")
    private List<FilterMetadata> activeFilters;
    
    @JsonProperty("filterStatistics")
    private java.util.Map<String, Object> filterStatistics;
    
    @JsonProperty("activeFilterSummary")
    private java.util.Map<String, Object> activeFilterSummary;
    
    @JsonProperty("error")
    private boolean error = false;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("errorType")
    private String errorType;
    
    @JsonProperty("retryable")
    private boolean retryable = false;
    
    public PagedResponse() {
    }
    
    public PagedResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = new PageInfo(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
    
    public PagedResponse(List<T> content, PageInfo page) {
        this.content = content;
        this.page = page;
    }
    
    public PagedResponse(Page<T> page, SortMetadata sortMetadata, List<FilterMetadata> activeFilters) {
        this(page);
        this.sortMetadata = sortMetadata;
        this.activeFilters = activeFilters;
    }
    
    public PagedResponse(List<T> content, PageInfo page, SortMetadata sortMetadata, List<FilterMetadata> activeFilters) {
        this.content = content;
        this.page = page;
        this.sortMetadata = sortMetadata;
        this.activeFilters = activeFilters;
    }
    
    // Getters and Setters
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public PageInfo getPage() {
        return page;
    }
    
    public void setPage(PageInfo page) {
        this.page = page;
    }
    
    public SortMetadata getSortMetadata() {
        return sortMetadata;
    }
    
    public void setSortMetadata(SortMetadata sortMetadata) {
        this.sortMetadata = sortMetadata;
    }
    
    public MultiColumnSortMetadata getMultiColumnSortMetadata() {
        return multiColumnSortMetadata;
    }
    
    public void setMultiColumnSortMetadata(MultiColumnSortMetadata multiColumnSortMetadata) {
        this.multiColumnSortMetadata = multiColumnSortMetadata;
    }
    
    public List<FilterMetadata> getActiveFilters() {
        return activeFilters;
    }
    
    public void setActiveFilters(List<FilterMetadata> activeFilters) {
        this.activeFilters = activeFilters;
    }
    
    public boolean isError() {
        return error;
    }
    
    public void setError(boolean error) {
        this.error = error;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
    
    public java.util.Map<String, Object> getFilterStatistics() {
        return filterStatistics;
    }
    
    public void setFilterStatistics(java.util.Map<String, Object> filterStatistics) {
        this.filterStatistics = filterStatistics;
    }
    
    public java.util.Map<String, Object> getActiveFilterSummary() {
        return activeFilterSummary;
    }
    
    public void setActiveFilterSummary(java.util.Map<String, Object> activeFilterSummary) {
        this.activeFilterSummary = activeFilterSummary;
    }
    
    /**
     * Page metadata information
     */
    public static class PageInfo {
        @JsonProperty("number")
        private int number;
        
        @JsonProperty("size")
        private int size;
        
        @JsonProperty("totalElements")
        private long totalElements;
        
        @JsonProperty("totalPages")
        private int totalPages;
        
        @JsonProperty("first")
        private boolean first;
        
        @JsonProperty("last")
        private boolean last;
        
        @JsonProperty("hasNext")
        private boolean hasNext;
        
        @JsonProperty("hasPrevious")
        private boolean hasPrevious;
        
        public PageInfo() {
        }
        
        public PageInfo(int number, int size, long totalElements, int totalPages, 
                       boolean first, boolean last, boolean hasNext, boolean hasPrevious) {
            this.number = number;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.first = first;
            this.last = last;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }
        
        // Getters and Setters
        public int getNumber() {
            return number;
        }
        
        public void setNumber(int number) {
            this.number = number;
        }
        
        public int getSize() {
            return size;
        }
        
        public void setSize(int size) {
            this.size = size;
        }
        
        public long getTotalElements() {
            return totalElements;
        }
        
        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public boolean isFirst() {
            return first;
        }
        
        public void setFirst(boolean first) {
            this.first = first;
        }
        
        public boolean isLast() {
            return last;
        }
        
        public void setLast(boolean last) {
            this.last = last;
        }
        
        public boolean isHasNext() {
            return hasNext;
        }
        
        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }
        
        public boolean isHasPrevious() {
            return hasPrevious;
        }
        
        public void setHasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
        }
    }
}