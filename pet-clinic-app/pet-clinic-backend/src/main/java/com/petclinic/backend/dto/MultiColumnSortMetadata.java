package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO representing multi-column sort metadata for enhanced table functionality
 * Supports multiple sort criteria with precedence order
 * Validates: Requirements 2.4
 */
public class MultiColumnSortMetadata {
    
    @JsonProperty("sortCriteria")
    @NotEmpty(message = "At least one sort criterion is required")
    @Size(max = 5, message = "Maximum 5 sort criteria allowed")
    @Valid
    private List<SortCriterion> sortCriteria;
    
    @JsonProperty("isGlobal")
    private boolean isGlobal;
    
    // Constructors
    public MultiColumnSortMetadata() {
        this.sortCriteria = new ArrayList<>();
        this.isGlobal = true;
    }
    
    public MultiColumnSortMetadata(List<SortCriterion> sortCriteria) {
        this.sortCriteria = sortCriteria != null ? new ArrayList<>(sortCriteria) : new ArrayList<>();
        this.isGlobal = true;
    }
    
    public MultiColumnSortMetadata(List<SortCriterion> sortCriteria, boolean isGlobal) {
        this.sortCriteria = sortCriteria != null ? new ArrayList<>(sortCriteria) : new ArrayList<>();
        this.isGlobal = isGlobal;
    }
    
    // Getters and Setters
    public List<SortCriterion> getSortCriteria() {
        return sortCriteria;
    }
    
    public void setSortCriteria(List<SortCriterion> sortCriteria) {
        this.sortCriteria = sortCriteria != null ? new ArrayList<>(sortCriteria) : new ArrayList<>();
    }
    
    public boolean isGlobal() {
        return isGlobal;
    }
    
    public void setGlobal(boolean global) {
        isGlobal = global;
    }
    
    // Business Methods
    
    /**
     * Add a sort criterion with the highest precedence (first in order)
     * @param column Column name
     * @param direction Sort direction
     */
    public void addPrimarySortCriterion(String column, String direction) {
        SortCriterion criterion = new SortCriterion(column, direction, 0);
        
        // Remove existing criterion for the same column
        sortCriteria.removeIf(c -> c.getColumn().equals(column));
        
        // Increment precedence of existing criteria
        sortCriteria.forEach(c -> c.setPrecedence(c.getPrecedence() + 1));
        
        // Add new criterion at the beginning
        sortCriteria.add(0, criterion);
        
        // Limit to maximum 5 criteria
        if (sortCriteria.size() > 5) {
            sortCriteria = sortCriteria.subList(0, 5);
        }
    }
    
    /**
     * Add a sort criterion with the lowest precedence (last in order)
     * @param column Column name
     * @param direction Sort direction
     */
    public void addSecondarySortCriterion(String column, String direction) {
        // Remove existing criterion for the same column
        sortCriteria.removeIf(c -> c.getColumn().equals(column));
        
        int precedence = sortCriteria.size();
        SortCriterion criterion = new SortCriterion(column, direction, precedence);
        sortCriteria.add(criterion);
        
        // Limit to maximum 5 criteria
        if (sortCriteria.size() > 5) {
            sortCriteria.remove(sortCriteria.size() - 1);
        }
    }
    
    /**
     * Remove sort criterion for a specific column
     * @param column Column name
     */
    public void removeSortCriterion(String column) {
        sortCriteria.removeIf(c -> c.getColumn().equals(column));
        
        // Reorder precedence
        for (int i = 0; i < sortCriteria.size(); i++) {
            sortCriteria.get(i).setPrecedence(i);
        }
    }
    
    /**
     * Clear all sort criteria
     */
    public void clearSortCriteria() {
        sortCriteria.clear();
    }
    
    /**
     * Get the primary (first) sort criterion
     * @return Primary sort criterion or null if none
     */
    public SortCriterion getPrimarySortCriterion() {
        return sortCriteria.isEmpty() ? null : sortCriteria.get(0);
    }
    
    /**
     * Check if a column is being sorted
     * @param column Column name
     * @return true if column is in sort criteria
     */
    public boolean isColumnSorted(String column) {
        return sortCriteria.stream().anyMatch(c -> c.getColumn().equals(column));
    }
    
    /**
     * Get sort criterion for a specific column
     * @param column Column name
     * @return Sort criterion or null if not found
     */
    public SortCriterion getSortCriterionForColumn(String column) {
        return sortCriteria.stream()
                .filter(c -> c.getColumn().equals(column))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Convert to single-column SortMetadata (using primary criterion)
     * @return SortMetadata or null if no criteria
     */
    public SortMetadata toPrimarySortMetadata() {
        SortCriterion primary = getPrimarySortCriterion();
        if (primary == null) {
            return null;
        }
        return new SortMetadata(primary.getColumn(), primary.getDirection(), isGlobal);
    }
    
    /**
     * Create from single SortMetadata
     * @param sortMetadata Single sort metadata
     * @return MultiColumnSortMetadata
     */
    public static MultiColumnSortMetadata fromSortMetadata(SortMetadata sortMetadata) {
        MultiColumnSortMetadata multiSort = new MultiColumnSortMetadata();
        if (sortMetadata != null && sortMetadata.getColumn() != null) {
            multiSort.addPrimarySortCriterion(sortMetadata.getColumn(), sortMetadata.getDirection());
            multiSort.setGlobal(sortMetadata.isGlobal());
        }
        return multiSort;
    }
    
    /**
     * Get display text for all sort criteria
     * @return Human-readable sort description
     */
    public String getDisplayText() {
        if (sortCriteria.isEmpty()) {
            return "No sorting";
        }
        
        return sortCriteria.stream()
                .map(c -> c.getDisplayName() + " (" + c.getDirection().toUpperCase() + ")")
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Check if this multi-column sort is valid
     * @return true if valid
     */
    public boolean isValid() {
        return !sortCriteria.isEmpty() && 
               sortCriteria.stream().allMatch(SortCriterion::isValid);
    }
    
    @Override
    public String toString() {
        return "MultiColumnSortMetadata{" +
                "sortCriteria=" + sortCriteria +
                ", isGlobal=" + isGlobal +
                '}';
    }
    
    /**
     * Inner class representing a single sort criterion
     */
    public static class SortCriterion {
        
        @JsonProperty("column")
        private String column;
        
        @JsonProperty("direction")
        private String direction;
        
        @JsonProperty("precedence")
        private int precedence;
        
        @JsonProperty("displayName")
        private String displayName;
        
        // Constructors
        public SortCriterion() {}
        
        public SortCriterion(String column, String direction, int precedence) {
            this.column = column;
            this.direction = direction;
            this.precedence = precedence;
            this.displayName = generateDisplayName(column);
        }
        
        // Getters and Setters
        public String getColumn() {
            return column;
        }
        
        public void setColumn(String column) {
            this.column = column;
            this.displayName = generateDisplayName(column);
        }
        
        public String getDirection() {
            return direction;
        }
        
        public void setDirection(String direction) {
            this.direction = direction;
        }
        
        public int getPrecedence() {
            return precedence;
        }
        
        public void setPrecedence(int precedence) {
            this.precedence = precedence;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        // Business Methods
        public boolean isAscending() {
            return "asc".equalsIgnoreCase(direction);
        }
        
        public boolean isDescending() {
            return "desc".equalsIgnoreCase(direction);
        }
        
        public boolean isValid() {
            return column != null && !column.trim().isEmpty() &&
                   direction != null && (direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"));
        }
        
        /**
         * Generate display name from column name
         * @param column Column name
         * @return Display name
         */
        private String generateDisplayName(String column) {
            if (column == null || column.trim().isEmpty()) {
                return "";
            }
            
            // Convert camelCase or snake_case to Title Case
            String displayName = column.replaceAll("([a-z])([A-Z])", "$1 $2")
                                      .replaceAll("_", " ")
                                      .toLowerCase();
            
            // Capitalize first letter of each word
            String[] words = displayName.split(" ");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1))
                          .append(" ");
                }
            }
            
            return result.toString().trim();
        }
        
        @Override
        public String toString() {
            return "SortCriterion{" +
                    "column='" + column + '\'' +
                    ", direction='" + direction + '\'' +
                    ", precedence=" + precedence +
                    ", displayName='" + displayName + '\'' +
                    '}';
        }
    }
}