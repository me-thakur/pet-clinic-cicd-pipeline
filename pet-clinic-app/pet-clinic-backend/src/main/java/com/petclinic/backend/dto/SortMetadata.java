package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO representing sort metadata for enhanced table functionality
 * Provides information about current sort state and configuration
 * Validates: Requirements 2.1, 2.4
 */
public class SortMetadata {
    
    @JsonProperty("column")
    @NotBlank(message = "Sort column is required")
    private String column;
    
    @JsonProperty("direction")
    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
    private String direction;
    
    @JsonProperty("isGlobal")
    private boolean isGlobal;
    
    @JsonProperty("displayName")
    private String displayName;
    
    @JsonProperty("clientSideFallback")
    private boolean clientSideFallback = false;
    
    @JsonProperty("fallbackMessage")
    private String fallbackMessage;
    
    // Constructors
    public SortMetadata() {
        this.isGlobal = true; // Default to global sorting
    }
    
    public SortMetadata(String column, String direction) {
        this.column = column;
        this.direction = direction;
        this.isGlobal = true;
        this.displayName = generateDisplayName(column);
    }
    
    public SortMetadata(String column, String direction, boolean isGlobal) {
        this.column = column;
        this.direction = direction;
        this.isGlobal = isGlobal;
        this.displayName = generateDisplayName(column);
    }
    
    public SortMetadata(String column, String direction, boolean isGlobal, String displayName) {
        this.column = column;
        this.direction = direction;
        this.isGlobal = isGlobal;
        this.displayName = displayName;
    }
    
    // Getters and Setters
    public String getColumn() {
        return column;
    }
    
    public void setColumn(String column) {
        this.column = column;
        if (this.displayName == null) {
            this.displayName = generateDisplayName(column);
        }
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public boolean isGlobal() {
        return isGlobal;
    }
    
    public void setGlobal(boolean global) {
        isGlobal = global;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isClientSideFallback() {
        return clientSideFallback;
    }
    
    public void setClientSideFallback(boolean clientSideFallback) {
        this.clientSideFallback = clientSideFallback;
    }
    
    public String getFallbackMessage() {
        return fallbackMessage;
    }
    
    public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }
    
    // Business Methods
    public boolean isAscending() {
        return "asc".equalsIgnoreCase(direction);
    }
    
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(direction);
    }
    
    public boolean isValidDirection() {
        return direction != null && (direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"));
    }
    
    public String getOppositeDirection() {
        return isAscending() ? "desc" : "asc";
    }
    
    /**
     * Generate a human-readable display name from column name
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
        return "SortMetadata{" +
                "column='" + column + '\'' +
                ", direction='" + direction + '\'' +
                ", isGlobal=" + isGlobal +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}