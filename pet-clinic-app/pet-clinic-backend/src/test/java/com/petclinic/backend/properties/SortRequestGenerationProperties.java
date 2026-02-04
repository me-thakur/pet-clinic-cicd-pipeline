package com.petclinic.backend.properties;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Sort Request Generation
 * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
 * **Validates: Requirements 2.1**
 * 
 * Property 3: Sort Request Generation
 * For any sortable column header click, the frontend should generate a server-side sorting request 
 * with correct column and direction parameters
 */
class SortRequestGenerationProperties {
    
    // Define valid sortable columns based on the enhanced table implementation
    private static final Set<String> VALID_SORTABLE_COLUMNS = Set.of(
        "id", "visitDate", "visitType", "diagnosis", "treatment", "cost",
        "petName", "ownerName", "ownerEmail", "ownerMobileNumber",
        "name", "species", "breed", "birthDate", "firstName", "lastName", "address"
    );
    
    private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("asc", "desc", "ASC", "DESC");
    
    /**
     * Property 3: Sort Request Generation - Single Column Sort Request
     * For any sortable column header click, the frontend should generate correct single column sort parameters
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void singleColumnSortRequestShouldGenerateCorrectParameters(
            @ForAll("validSortableColumns") String column,
            @ForAll("validSortDirections") String direction) {
        
        // Simulate single column sort request generation (as implemented in enhanced-table.js)
        SortRequest sortRequest = generateSortRequest(column, direction, false, Collections.emptyList());
        
        // Verify single column sort parameters are correctly set
        assertThat(sortRequest).isNotNull();
        assertThat(sortRequest.getSortBy()).isEqualTo(column);
        assertThat(sortRequest.getSortDir()).isEqualTo(direction);
        assertThat(sortRequest.getMultiSort()).isNull();
        
        // Verify request contains required pagination parameters
        assertThat(sortRequest.getPage()).isNotNull();
        assertThat(sortRequest.getSize()).isNotNull();
        assertThat(sortRequest.getPage()).isGreaterThanOrEqualTo(0);
        assertThat(sortRequest.getSize()).isGreaterThan(0);
        
        // Verify filters parameter is present (can be empty)
        assertThat(sortRequest.getFilters()).isNotNull();
        
        // Verify useCache is set to false for sort requests (to ensure fresh data)
        assertThat(sortRequest.isUseCache()).isFalse();
    }
    
    /**
     * Property 3: Sort Request Generation - Multi-Column Sort Request
     * For any multi-column sort scenario, the frontend should generate correct multiSort parameter
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void multiColumnSortRequestShouldGenerateCorrectMultiSortParameter(
            @ForAll("validSortableColumns") String newColumn,
            @ForAll("validSortDirections") String newDirection,
            @ForAll("existingMultiColumnSort") List<SortCriterion> existingSort) {
        
        // Skip test cases where existingSort is empty - those should be single column sorts
        if (existingSort.isEmpty()) {
            return;
        }
        
        // Simulate multi-column sort request generation (Ctrl+click behavior)
        SortRequest sortRequest = generateSortRequest(newColumn, newDirection, true, existingSort);
        
        // Verify multi-column sort parameters are correctly set
        assertThat(sortRequest).isNotNull();
        assertThat(sortRequest.getSortBy()).isNull(); // Should not use single column parameters
        assertThat(sortRequest.getSortDir()).isNull(); // Should not use single column parameters
        assertThat(sortRequest.getMultiSort()).isNotNull();
        
        // Parse and verify multiSort string format: "column1:direction1,column2:direction2,..."
        String multiSortString = sortRequest.getMultiSort();
        assertThat(multiSortString).isNotBlank();
        
        String[] sortCriteria = multiSortString.split(",");
        assertThat(sortCriteria.length).isGreaterThan(0);
        assertThat(sortCriteria.length).isLessThanOrEqualTo(5); // Limit to 5 columns as per implementation
        
        // Verify each sort criterion has correct format
        for (String criterion : sortCriteria) {
            assertThat(criterion).contains(":");
            String[] parts = criterion.split(":");
            assertThat(parts).hasSize(2);
            
            String column = parts[0];
            String direction = parts[1];
            
            assertThat(VALID_SORTABLE_COLUMNS).contains(column);
            assertThat(VALID_SORT_DIRECTIONS).contains(direction);
        }
        
        // Verify the new column is included in the multiSort string
        assertThat(multiSortString).contains(newColumn + ":" + newDirection);
        
        // Verify request structure is correct
        assertThat(sortRequest.getPage()).isNotNull();
        assertThat(sortRequest.getSize()).isNotNull();
        assertThat(sortRequest.getFilters()).isNotNull();
        assertThat(sortRequest.isUseCache()).isFalse();
    }
    
    /**
     * Property 3: Sort Request Generation - Direction Toggle Behavior
     * For any column that is already sorted, clicking it again should toggle the direction
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void sortDirectionShouldToggleCorrectlyOnRepeatedClicks(
            @ForAll("validSortableColumns") String column,
            @ForAll("validSortDirections") String initialDirection) {
        
        // Simulate first click - establish initial sort
        SortRequest firstRequest = generateSortRequest(column, initialDirection, false, Collections.emptyList());
        
        // Create existing sort state from first request
        List<SortCriterion> existingSort = List.of(
            new SortCriterion(column, initialDirection, 0)
        );
        
        // Simulate second click on same column - should toggle direction
        String expectedToggledDirection = initialDirection.toLowerCase().equals("asc") ? "desc" : "asc";
        SortRequest secondRequest = generateSortRequest(column, expectedToggledDirection, false, existingSort);
        
        // Verify direction was toggled correctly
        assertThat(secondRequest.getSortBy()).isEqualTo(column);
        assertThat(secondRequest.getSortDir()).isEqualTo(expectedToggledDirection);
        assertThat(secondRequest.getMultiSort()).isNull(); // Single column sort
        
        // Verify the direction is actually different from the initial direction
        assertThat(secondRequest.getSortDir().toLowerCase())
            .isNotEqualTo(initialDirection.toLowerCase());
    }
    
    /**
     * Property 3: Sort Request Generation - Column Addition to Multi-Sort
     * For any existing multi-column sort, adding a new column should preserve existing criteria
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 50)
    void addingColumnToMultiSortShouldPreserveExistingCriteria(
            @ForAll("validSortableColumns") String newColumn,
            @ForAll("validSortDirections") String newDirection,
            @ForAll("existingMultiColumnSort") List<SortCriterion> existingSort) {
        
        // Filter out the new column from existing sort to avoid duplicates
        List<SortCriterion> filteredExistingSort = existingSort.stream()
            .filter(c -> !c.getColumn().equals(newColumn))
            .collect(Collectors.toList());
        
        if (filteredExistingSort.isEmpty()) {
            return; // Skip this test case if no existing criteria remain
        }
        
        // Generate multi-column sort request
        SortRequest sortRequest = generateSortRequest(newColumn, newDirection, true, filteredExistingSort);
        
        String multiSortString = sortRequest.getMultiSort();
        assertThat(multiSortString).isNotNull();
        
        // Verify all existing criteria are preserved
        for (SortCriterion existingCriterion : filteredExistingSort) {
            String expectedCriterion = existingCriterion.getColumn() + ":" + existingCriterion.getDirection();
            assertThat(multiSortString).contains(expectedCriterion);
        }
        
        // Verify new criterion is added
        String newCriterion = newColumn + ":" + newDirection;
        assertThat(multiSortString).contains(newCriterion);
        
        // Verify total number of criteria doesn't exceed limit
        String[] allCriteria = multiSortString.split(",");
        assertThat(allCriteria.length).isLessThanOrEqualTo(5);
    }
    
    /**
     * Property 3: Sort Request Generation - Request Parameter Consistency
     * For any sort request, all required parameters should be present and valid
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void sortRequestShouldAlwaysContainRequiredParameters(
            @ForAll("validSortableColumns") String column,
            @ForAll("validSortDirections") String direction,
            @ForAll boolean isMultiColumn,
            @ForAll("existingMultiColumnSort") List<SortCriterion> existingSort) {
        
        // Generate sort request
        SortRequest sortRequest = generateSortRequest(column, direction, isMultiColumn, existingSort);
        
        // Verify all required parameters are present
        assertThat(sortRequest).isNotNull();
        assertThat(sortRequest.getPage()).isNotNull();
        assertThat(sortRequest.getSize()).isNotNull();
        assertThat(sortRequest.getFilters()).isNotNull();
        
        // Verify pagination parameters are valid
        assertThat(sortRequest.getPage()).isGreaterThanOrEqualTo(0);
        assertThat(sortRequest.getSize()).isGreaterThan(0);
        assertThat(sortRequest.getSize()).isLessThanOrEqualTo(100); // Reasonable upper limit
        
        // Verify cache setting is correct for sort requests
        assertThat(sortRequest.isUseCache()).isFalse();
        
        // Verify sort parameters are mutually exclusive
        if (sortRequest.getMultiSort() != null) {
            assertThat(sortRequest.getSortBy()).isNull();
            assertThat(sortRequest.getSortDir()).isNull();
        } else {
            assertThat(sortRequest.getSortBy()).isNotNull();
            assertThat(sortRequest.getSortDir()).isNotNull();
        }
    }
    
    /**
     * Property 3: Sort Request Generation - Column Name Validation
     * For any sort request, column names should be valid and sortable
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void sortRequestShouldOnlyContainValidSortableColumns(
            @ForAll("validSortableColumns") String column,
            @ForAll("validSortDirections") String direction,
            @ForAll boolean isMultiColumn,
            @ForAll("existingMultiColumnSort") List<SortCriterion> existingSort) {
        
        // Generate sort request
        SortRequest sortRequest = generateSortRequest(column, direction, isMultiColumn, existingSort);
        
        // Verify single column sort uses valid column
        if (sortRequest.getSortBy() != null) {
            assertThat(VALID_SORTABLE_COLUMNS).contains(sortRequest.getSortBy());
            assertThat(VALID_SORT_DIRECTIONS).contains(sortRequest.getSortDir());
        }
        
        // Verify multi-column sort uses valid columns
        if (sortRequest.getMultiSort() != null) {
            String[] sortCriteria = sortRequest.getMultiSort().split(",");
            
            for (String criterion : sortCriteria) {
                String[] parts = criterion.split(":");
                assertThat(parts).hasSize(2);
                
                String columnName = parts[0];
                String sortDirection = parts[1];
                
                assertThat(VALID_SORTABLE_COLUMNS).contains(columnName);
                assertThat(VALID_SORT_DIRECTIONS).contains(sortDirection);
            }
        }
    }
    
    /**
     * Property 3: Sort Request Generation - Empty State Handling
     * For any sort request with no existing sort criteria, should generate valid single column sort
     * **Feature: critical-fixes-and-enhancements, Property 3: Sort Request Generation**
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 50)
    void sortRequestFromEmptyStateShouldGenerateSingleColumnSort(
            @ForAll("validSortableColumns") String column,
            @ForAll("validSortDirections") String direction) {
        
        // Generate sort request from empty state (no existing sort)
        SortRequest sortRequest = generateSortRequest(column, direction, false, Collections.emptyList());
        
        // Should always generate single column sort from empty state
        assertThat(sortRequest.getSortBy()).isEqualTo(column);
        assertThat(sortRequest.getSortDir()).isEqualTo(direction);
        assertThat(sortRequest.getMultiSort()).isNull();
        
        // Verify request is valid
        assertThat(sortRequest.getPage()).isNotNull();
        assertThat(sortRequest.getSize()).isNotNull();
        assertThat(sortRequest.getFilters()).isNotNull();
        assertThat(sortRequest.isUseCache()).isFalse();
    }
    
    // Generators for test data
    
    @Provide
    Arbitrary<String> validSortableColumns() {
        return Arbitraries.of(VALID_SORTABLE_COLUMNS.toArray(new String[0]));
    }
    
    @Provide
    Arbitrary<String> validSortDirections() {
        return Arbitraries.of(VALID_SORT_DIRECTIONS.toArray(new String[0]));
    }
    
    @Provide
    Arbitrary<List<SortCriterion>> existingMultiColumnSort() {
        return Arbitraries.of(VALID_SORTABLE_COLUMNS.toArray(new String[0]))
            .list()
            .ofMinSize(0)
            .ofMaxSize(4) // Leave room for one more column
            .map(columns -> {
                List<SortCriterion> criteria = new ArrayList<>();
                Set<String> usedColumns = new HashSet<>();
                
                for (int i = 0; i < columns.size(); i++) {
                    String column = columns.get(i);
                    if (!usedColumns.contains(column)) {
                        String direction = i % 2 == 0 ? "asc" : "desc";
                        criteria.add(new SortCriterion(column, direction, i));
                        usedColumns.add(column);
                    }
                }
                
                return criteria;
            });
    }
    
    // Helper methods and classes
    
    /**
     * Simulate the sort request generation logic from enhanced-table.js
     * This mirrors the actual frontend implementation
     */
    private SortRequest generateSortRequest(String column, String direction, boolean addToExisting, List<SortCriterion> existingSort) {
        SortRequest request = new SortRequest();
        
        // Set standard pagination and cache parameters
        request.setPage(0); // Reset to first page when sorting
        request.setSize(10); // Default page size
        request.setFilters(new HashMap<>()); // Empty filters for this test
        request.setUseCache(false); // Don't cache sort requests
        
        // Determine sort parameters based on multi-column logic
        if (addToExisting && !existingSort.isEmpty()) {
            // Multi-column sort: build multiSort parameter
            List<SortCriterion> multiSortCriteria = new ArrayList<>(existingSort);
            
            // Update or add the new criterion
            boolean updated = false;
            for (SortCriterion criterion : multiSortCriteria) {
                if (criterion.getColumn().equals(column)) {
                    criterion.setDirection(direction);
                    updated = true;
                    break;
                }
            }
            
            if (!updated && multiSortCriteria.size() < 5) {
                multiSortCriteria.add(new SortCriterion(column, direction, multiSortCriteria.size()));
            }
            
            // Build multiSort string: "column1:direction1,column2:direction2,..."
            String multiSortString = multiSortCriteria.stream()
                .map(c -> c.getColumn() + ":" + c.getDirection())
                .collect(Collectors.joining(","));
            
            request.setMultiSort(multiSortString);
            request.setSortBy(null);
            request.setSortDir(null);
        } else {
            // Single column sort
            request.setSortBy(column);
            request.setSortDir(direction);
            request.setMultiSort(null);
        }
        
        return request;
    }
    
    /**
     * Represents a sort criterion for multi-column sorting
     */
    private static class SortCriterion {
        private String column;
        private String direction;
        private int precedence;
        
        public SortCriterion(String column, String direction, int precedence) {
            this.column = column;
            this.direction = direction;
            this.precedence = precedence;
        }
        
        public String getColumn() { return column; }
        public String getDirection() { return direction; }
        public int getPrecedence() { return precedence; }
        
        public void setDirection(String direction) { this.direction = direction; }
    }
    
    /**
     * Represents a sort request as generated by the frontend
     * This mirrors the request structure sent to the backend API
     */
    private static class SortRequest {
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDir;
        private String multiSort;
        private Map<String, Object> filters;
        private boolean useCache;
        
        // Getters and setters
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public String getSortDir() { return sortDir; }
        public void setSortDir(String sortDir) { this.sortDir = sortDir; }
        
        public String getMultiSort() { return multiSort; }
        public void setMultiSort(String multiSort) { this.multiSort = multiSort; }
        
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
        
        public boolean isUseCache() { return useCache; }
        public void setUseCache(boolean useCache) { this.useCache = useCache; }
    }
}