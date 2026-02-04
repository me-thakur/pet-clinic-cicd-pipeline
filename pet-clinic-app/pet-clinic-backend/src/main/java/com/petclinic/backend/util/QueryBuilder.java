package com.petclinic.backend.util;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.SortMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dynamic query builder for constructing SQL queries with ORDER BY and WHERE clauses
 * Supports safe parameter binding to prevent SQL injection
 * Validates: Requirements 2.2, 2.3
 */
public class QueryBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
    
    private StringBuilder query;
    private List<Object> parameters;
    private String entityType;
    private boolean hasWhere;
    
    // Entity to table mapping
    private static final Map<String, String> ENTITY_TABLE_MAP = Map.of(
        "visits", "visits",
        "owners", "owners", 
        "pets", "pets",
        "veterinarians", "veterinarians"
    );
    
    // Column mapping for complex fields
    private static final Map<String, String> COLUMN_MAPPING = Map.of(
        "pet.name", "p.name",
        "pet.id", "p.id",
        "owner.lastname", "o.last_name",
        "owner.firstname", "o.first_name",
        "veterinarian.lastname", "v.last_name",
        "veterinarian.firstname", "v.first_name"
    );
    
    public QueryBuilder(String entityType) {
        this.query = new StringBuilder();
        this.parameters = new ArrayList<>();
        this.entityType = entityType.toLowerCase();
        this.hasWhere = false;
        
        initializeBaseQuery();
    }
    
    /**
     * Initialize base query for entity type
     */
    private void initializeBaseQuery() {
        String tableName = ENTITY_TABLE_MAP.get(entityType);
        if (tableName == null) {
            throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        }
        
        switch (entityType) {
            case "visits":
                query.append("SELECT v.*, p.name as pet_name, p.id as pet_id, ")
                     .append("vet.first_name as vet_first_name, vet.last_name as vet_last_name ")
                     .append("FROM visits v ")
                     .append("LEFT JOIN pets p ON v.pet_id = p.id ")
                     .append("LEFT JOIN owners o ON p.owner_id = o.id ")
                     .append("LEFT JOIN veterinarians vet ON v.veterinarian_id = vet.id");
                break;
            case "owners":
                query.append("SELECT o.* FROM owners o");
                break;
            case "pets":
                query.append("SELECT p.*, o.first_name as owner_first_name, o.last_name as owner_last_name ")
                     .append("FROM pets p ")
                     .append("LEFT JOIN owners o ON p.owner_id = o.id");
                break;
            case "veterinarians":
                query.append("SELECT v.* FROM veterinarians v");
                break;
            default:
                throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        }
    }
    
    /**
     * Add filter criteria to query
     * 
     * @param filters List of filter criteria
     * @return This QueryBuilder for method chaining
     */
    public QueryBuilder addFilters(List<FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            return this;
        }
        
        for (FilterCriteria filter : filters) {
            addFilter(filter);
        }
        
        return this;
    }
    
    /**
     * Add single filter to query
     * 
     * @param filter Filter criteria
     * @return This QueryBuilder for method chaining
     */
    public QueryBuilder addFilter(FilterCriteria filter) {
        if (filter == null || !filter.isValid()) {
            logger.warn("Invalid filter criteria: {}", filter);
            return this;
        }
        
        String column = mapColumn(filter.getField());
        String operator = filter.getOperator().toLowerCase();
        Object value = filter.getValue();
        
        // Add WHERE or AND
        if (!hasWhere) {
            query.append(" WHERE ");
            hasWhere = true;
        } else {
            String logicalOp = filter.isAndOperator() ? " AND " : " OR ";
            query.append(logicalOp);
        }
        
        // Add filter condition based on operator
        switch (operator) {
            case "equals":
            case "eq":
                query.append(column).append(" = ?");
                parameters.add(value);
                break;
            case "ne":
            case "notequals":
                query.append(column).append(" != ?");
                parameters.add(value);
                break;
            case "contains":
                query.append(column).append(" LIKE ?");
                parameters.add("%" + value + "%");
                break;
            case "startswith":
                query.append(column).append(" LIKE ?");
                parameters.add(value + "%");
                break;
            case "endswith":
                query.append(column).append(" LIKE ?");
                parameters.add("%" + value);
                break;
            case "gt":
                query.append(column).append(" > ?");
                parameters.add(value);
                break;
            case "gte":
                query.append(column).append(" >= ?");
                parameters.add(value);
                break;
            case "lt":
                query.append(column).append(" < ?");
                parameters.add(value);
                break;
            case "lte":
                query.append(column).append(" <= ?");
                parameters.add(value);
                break;
            case "in":
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    if (!values.isEmpty()) {
                        query.append(column).append(" IN (");
                        for (int i = 0; i < values.size(); i++) {
                            if (i > 0) query.append(", ");
                            query.append("?");
                            parameters.add(values.get(i));
                        }
                        query.append(")");
                    }
                } else {
                    query.append(column).append(" = ?");
                    parameters.add(value);
                }
                break;
            case "notin":
                if (value instanceof List) {
                    List<?> values = (List<?>) value;
                    if (!values.isEmpty()) {
                        query.append(column).append(" NOT IN (");
                        for (int i = 0; i < values.size(); i++) {
                            if (i > 0) query.append(", ");
                            query.append("?");
                            parameters.add(values.get(i));
                        }
                        query.append(")");
                    }
                } else {
                    query.append(column).append(" != ?");
                    parameters.add(value);
                }
                break;
            case "isnull":
                query.append(column).append(" IS NULL");
                break;
            case "isnotnull":
                query.append(column).append(" IS NOT NULL");
                break;
            default:
                logger.warn("Unsupported filter operator: {}", operator);
                // Remove the WHERE/AND/OR we just added
                if (hasWhere && query.toString().endsWith(" WHERE ")) {
                    query.setLength(query.length() - 7);
                    hasWhere = false;
                } else if (query.toString().endsWith(" AND ")) {
                    query.setLength(query.length() - 5);
                } else if (query.toString().endsWith(" OR ")) {
                    query.setLength(query.length() - 4);
                }
                break;
        }
        
        return this;
    }
    
    /**
     * Add sort criteria to query
     * 
     * @param sortMetadata Sort metadata
     * @return This QueryBuilder for method chaining
     */
    public QueryBuilder addSort(SortMetadata sortMetadata) {
        if (sortMetadata == null || sortMetadata.getColumn() == null) {
            return this;
        }
        
        String column = mapColumn(sortMetadata.getColumn());
        String direction = sortMetadata.getDirection();
        
        if (!isValidSortDirection(direction)) {
            logger.warn("Invalid sort direction: {}", direction);
            return this;
        }
        
        query.append(" ORDER BY ").append(column).append(" ").append(direction.toUpperCase());
        
        return this;
    }
    
    /**
     * Add pagination to query
     * 
     * @param offset Offset for pagination
     * @param limit Limit for pagination
     * @return This QueryBuilder for method chaining
     */
    public QueryBuilder addPagination(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            logger.warn("Invalid pagination parameters: offset={}, limit={}", offset, limit);
            return this;
        }
        
        query.append(" LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);
        
        return this;
    }
    
    /**
     * Build the final query string
     * 
     * @return Complete SQL query
     */
    public String buildQuery() {
        return query.toString();
    }
    
    /**
     * Get query parameters
     * 
     * @return List of parameters for prepared statement
     */
    public List<Object> getParameters() {
        return new ArrayList<>(parameters);
    }
    
    /**
     * Build count query for pagination
     * 
     * @return Count query string
     */
    public String buildCountQuery() {
        String baseQuery = query.toString();
        
        // Remove ORDER BY and LIMIT clauses for count query
        int orderByIndex = baseQuery.toUpperCase().lastIndexOf(" ORDER BY");
        if (orderByIndex > 0) {
            baseQuery = baseQuery.substring(0, orderByIndex);
        }
        
        int limitIndex = baseQuery.toUpperCase().lastIndexOf(" LIMIT");
        if (limitIndex > 0) {
            baseQuery = baseQuery.substring(0, limitIndex);
        }
        
        // Replace SELECT clause with COUNT
        int fromIndex = baseQuery.toUpperCase().indexOf(" FROM");
        if (fromIndex > 0) {
            return "SELECT COUNT(*)" + baseQuery.substring(fromIndex);
        }
        
        return baseQuery;
    }
    
    /**
     * Get count query parameters (excluding pagination parameters)
     * 
     * @return List of parameters for count query
     */
    public List<Object> getCountParameters() {
        List<Object> countParams = new ArrayList<>(parameters);
        
        // Remove pagination parameters (last 2 parameters if they exist)
        String queryStr = query.toString();
        if (queryStr.contains(" LIMIT ? OFFSET ?") && countParams.size() >= 2) {
            countParams.remove(countParams.size() - 1); // Remove offset
            countParams.remove(countParams.size() - 1); // Remove limit
        }
        
        return countParams;
    }
    
    /**
     * Map logical column names to actual database column names
     */
    private String mapColumn(String column) {
        if (column == null) {
            return "id"; // Default fallback
        }
        
        // Check if it's a mapped column
        String mapped = COLUMN_MAPPING.get(column.toLowerCase());
        if (mapped != null) {
            return mapped;
        }
        
        // Convert camelCase to snake_case for database columns
        String dbColumn = column.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        
        // Add table alias based on entity type and column
        switch (entityType) {
            case "visits":
                if (dbColumn.startsWith("pet_")) {
                    return "p." + dbColumn.substring(4);
                } else if (dbColumn.startsWith("veterinarian_")) {
                    return "vet." + dbColumn.substring(13);
                } else {
                    return "v." + dbColumn;
                }
            case "owners":
                return "o." + dbColumn;
            case "pets":
                if (dbColumn.startsWith("owner_")) {
                    return "o." + dbColumn.substring(6);
                } else {
                    return "p." + dbColumn;
                }
            case "veterinarians":
                return "v." + dbColumn;
            default:
                return dbColumn;
        }
    }
    
    /**
     * Validate sort direction
     */
    private boolean isValidSortDirection(String direction) {
        return direction != null && 
               (direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"));
    }
    
    @Override
    public String toString() {
        return "QueryBuilder{" +
                "query='" + query + '\'' +
                ", parameters=" + parameters +
                ", entityType='" + entityType + '\'' +
                '}';
    }
}