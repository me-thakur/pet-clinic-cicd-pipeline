package com.petclinic.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * DTO representing filter criteria for advanced filtering
 * Validates: Requirements 4.2, 4.5
 */
public class FilterCriteria {
    
    @NotBlank(message = "Field is required")
    private String field;
    
    @NotBlank(message = "Operator is required")
    private String operator;
    
    @NotNull(message = "Value is required")
    private Object value;
    
    @NotBlank(message = "Entity type is required")
    private String entityType;
    
    private String logicalOperator = "AND"; // Default to AND
    
    // Constructors
    public FilterCriteria() {}
    
    public FilterCriteria(String field, String operator, Object value, String entityType) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.entityType = entityType;
        this.logicalOperator = "AND";
    }
    
    public FilterCriteria(String field, String operator, Object value, String entityType, String logicalOperator) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.entityType = entityType;
        this.logicalOperator = logicalOperator != null ? logicalOperator : "AND";
    }
    
    // Getters and Setters
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public String getLogicalOperator() {
        return logicalOperator;
    }
    
    public void setLogicalOperator(String logicalOperator) {
        this.logicalOperator = logicalOperator != null ? logicalOperator : "AND";
    }
    
    // Business Methods
    public boolean isTextFilter() {
        return "contains".equalsIgnoreCase(operator) || 
               "startsWith".equalsIgnoreCase(operator) || 
               "endsWith".equalsIgnoreCase(operator) ||
               "eq".equalsIgnoreCase(operator) ||
               "ne".equalsIgnoreCase(operator);
    }
    
    public boolean isNumericFilter() {
        return "gt".equalsIgnoreCase(operator) || 
               "lt".equalsIgnoreCase(operator) || 
               "gte".equalsIgnoreCase(operator) || 
               "lte".equalsIgnoreCase(operator) ||
               "eq".equalsIgnoreCase(operator) ||
               "ne".equalsIgnoreCase(operator);
    }
    
    public boolean isDateFilter() {
        return "before".equalsIgnoreCase(operator) || 
               "after".equalsIgnoreCase(operator) || 
               "between".equalsIgnoreCase(operator) ||
               "eq".equalsIgnoreCase(operator) ||
               "ne".equalsIgnoreCase(operator);
    }
    
    public boolean isBooleanFilter() {
        return "eq".equalsIgnoreCase(operator) || 
               "ne".equalsIgnoreCase(operator);
    }
    
    public boolean isEnumFilter() {
        return "eq".equalsIgnoreCase(operator) || 
               "ne".equalsIgnoreCase(operator) ||
               "in".equalsIgnoreCase(operator) ||
               "notIn".equalsIgnoreCase(operator);
    }
    
    public boolean isRelationshipFilter() {
        return field != null && (field.contains(".") || field.endsWith("Id"));
    }
    
    public boolean isAndOperator() {
        return "AND".equalsIgnoreCase(logicalOperator);
    }
    
    public boolean isOrOperator() {
        return "OR".equalsIgnoreCase(logicalOperator);
    }
    
    public String getValueAsString() {
        return value != null ? value.toString() : "";
    }
    
    public boolean hasValidOperator() {
        if (operator == null) return false;
        
        String op = operator.toLowerCase();
        return op.equals("eq") || op.equals("ne") || op.equals("gt") || op.equals("lt") ||
               op.equals("gte") || op.equals("lte") || op.equals("contains") || 
               op.equals("startswith") || op.equals("endswith") || op.equals("before") ||
               op.equals("after") || op.equals("between") || op.equals("in") || op.equals("notin") ||
               op.equals("equals") || op.equals("notequals");
    }
    
    public boolean hasValidEntityType() {
        if (entityType == null) return false;
        
        String type = entityType.toLowerCase();
        return type.equals("pet") || type.equals("visit") || type.equals("veterinarian") || type.equals("owner") ||
               type.equals("pets") || type.equals("visits") || type.equals("veterinarians") || type.equals("owners");
    }
    
    public boolean isValid() {
        return field != null && !field.trim().isEmpty() &&
               hasValidOperator() &&
               value != null &&
               hasValidEntityType();
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterCriteria that = (FilterCriteria) o;
        return Objects.equals(field, that.field) &&
               Objects.equals(operator, that.operator) &&
               Objects.equals(value, that.value) &&
               Objects.equals(entityType, that.entityType) &&
               Objects.equals(logicalOperator, that.logicalOperator);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value, entityType, logicalOperator);
    }
    
    @Override
    public String toString() {
        return "FilterCriteria{" +
                "field='" + field + '\'' +
                ", operator='" + operator + '\'' +
                ", value=" + value +
                ", entityType='" + entityType + '\'' +
                ", logicalOperator='" + logicalOperator + '\'' +
                '}';
    }
}