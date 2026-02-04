package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * DTO representing filter metadata for enhanced table functionality
 * Provides information about active filters and their display properties
 * Validates: Requirements 3.2, 3.4
 */
public class FilterMetadata {
    
    @JsonProperty("column")
    @NotBlank(message = "Filter column is required")
    private String column;
    
    @JsonProperty("operator")
    @NotBlank(message = "Filter operator is required")
    private String operator;
    
    @JsonProperty("value")
    private Object value;
    
    @JsonProperty("displayName")
    private String displayName;
    
    @JsonProperty("displayValue")
    private String displayValue;
    
    @JsonProperty("filterType")
    private String filterType;
    
    // Constructors
    public FilterMetadata() {
    }
    
    public FilterMetadata(String column, String operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.displayName = generateDisplayName(column);
        this.displayValue = generateDisplayValue(value);
        this.filterType = determineFilterType(operator);
    }
    
    public FilterMetadata(String column, String operator, Object value, String displayName) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.displayName = displayName;
        this.displayValue = generateDisplayValue(value);
        this.filterType = determineFilterType(operator);
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
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
        this.filterType = determineFilterType(operator);
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
        this.displayValue = generateDisplayValue(value);
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
    
    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getFilterType() {
        return filterType;
    }
    
    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }
    
    // Business Methods
    public boolean isTextFilter() {
        return "text".equals(filterType);
    }
    
    public boolean isSelectFilter() {
        return "select".equals(filterType);
    }
    
    public boolean isDateFilter() {
        return "date".equals(filterType);
    }
    
    public boolean isNumericFilter() {
        return "numeric".equals(filterType);
    }
    
    public boolean isEqualsOperator() {
        return "equals".equalsIgnoreCase(operator) || "eq".equalsIgnoreCase(operator);
    }
    
    public boolean isContainsOperator() {
        return "contains".equalsIgnoreCase(operator);
    }
    
    public boolean isInOperator() {
        return "in".equalsIgnoreCase(operator);
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
    
    /**
     * Generate a human-readable display value
     * @param value Filter value
     * @return Display value
     */
    private String generateDisplayValue(Object value) {
        if (value == null) {
            return "";
        }
        
        String stringValue = value.toString();
        
        // Handle enum values - convert to display format
        if (stringValue.contains("_")) {
            String result = stringValue.replaceAll("_", " ").toLowerCase();
            // Capitalize first letter of each word
            String[] words = result.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
                }
            }
            return sb.toString().trim();
        }
        
        return stringValue;
    }
    
    /**
     * Determine filter type based on operator
     * @param operator Filter operator
     * @return Filter type
     */
    private String determineFilterType(String operator) {
        if (operator == null) {
            return "text";
        }
        
        switch (operator.toLowerCase()) {
            case "equals":
            case "eq":
            case "in":
                return "select";
            case "contains":
            case "startswith":
            case "endswith":
                return "text";
            case "before":
            case "after":
            case "between":
                return "date";
            case "gt":
            case "lt":
            case "gte":
            case "lte":
                return "numeric";
            default:
                return "text";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterMetadata that = (FilterMetadata) o;
        return Objects.equals(column, that.column) &&
               Objects.equals(operator, that.operator) &&
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(column, operator, value);
    }
    
    @Override
    public String toString() {
        return "FilterMetadata{" +
                "column='" + column + '\'' +
                ", operator='" + operator + '\'' +
                ", value=" + value +
                ", displayName='" + displayName + '\'' +
                ", displayValue='" + displayValue + '\'' +
                ", filterType='" + filterType + '\'' +
                '}';
    }
}