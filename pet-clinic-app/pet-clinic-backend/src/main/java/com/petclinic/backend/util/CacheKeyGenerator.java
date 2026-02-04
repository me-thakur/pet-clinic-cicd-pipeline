package com.petclinic.backend.util;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.SortMetadata;
import org.springframework.data.domain.Pageable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for generating cache keys for sort and filter operations
 * Creates consistent, unique keys for caching table query results
 * Validates: Requirements 5.3
 */
public class CacheKeyGenerator {
    
    private static final String SEPARATOR = ":";
    private static final String LIST_SEPARATOR = ",";
    
    /**
     * Generate cache key for sort and filter combination
     * 
     * @param entityType Entity type (visits, owners, etc.)
     * @param pageable Pagination information
     * @param sortMetadata Sort criteria
     * @param filters Filter criteria list
     * @return Unique cache key string
     */
    public static String generateSortFilterKey(String entityType, Pageable pageable, 
                                             SortMetadata sortMetadata, List<FilterCriteria> filters) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Entity type
        keyBuilder.append("table").append(SEPARATOR).append(entityType.toLowerCase());
        
        // Pagination
        keyBuilder.append(SEPARATOR).append("page").append(pageable.getPageNumber());
        keyBuilder.append(SEPARATOR).append("size").append(pageable.getPageSize());
        
        // Sort metadata
        if (sortMetadata != null && sortMetadata.getColumn() != null) {
            keyBuilder.append(SEPARATOR).append("sort")
                     .append(sortMetadata.getColumn().toLowerCase())
                     .append(SEPARATOR).append(sortMetadata.getDirection().toLowerCase());
        }
        
        // Filters
        if (filters != null && !filters.isEmpty()) {
            String filterString = filters.stream()
                .filter(FilterCriteria::isValid)
                .sorted((f1, f2) -> f1.getField().compareToIgnoreCase(f2.getField())) // Sort for consistency
                .map(filter -> filter.getField().toLowerCase() + "=" + 
                              filter.getOperator().toLowerCase() + "=" + 
                              (filter.getValue() != null ? filter.getValue().toString().toLowerCase() : "null"))
                .collect(Collectors.joining(LIST_SEPARATOR));
            
            if (!filterString.isEmpty()) {
                keyBuilder.append(SEPARATOR).append("filters").append(filterString);
            }
        }
        
        String key = keyBuilder.toString();
        
        // If key is too long, hash it to ensure consistent length
        if (key.length() > 200) {
            return "table:" + entityType.toLowerCase() + ":" + hashKey(key);
        }
        
        return key;
    }
    
    /**
     * Generate cache key for entity count with filters
     * 
     * @param entityType Entity type
     * @param filters Filter criteria list
     * @return Cache key for count query
     */
    public static String generateCountKey(String entityType, List<FilterCriteria> filters) {
        StringBuilder keyBuilder = new StringBuilder();
        
        keyBuilder.append("count").append(SEPARATOR).append(entityType.toLowerCase());
        
        if (filters != null && !filters.isEmpty()) {
            String filterString = filters.stream()
                .filter(FilterCriteria::isValid)
                .sorted((f1, f2) -> f1.getField().compareToIgnoreCase(f2.getField()))
                .map(filter -> filter.getField().toLowerCase() + "=" + 
                              filter.getOperator().toLowerCase() + "=" + 
                              (filter.getValue() != null ? filter.getValue().toString().toLowerCase() : "null"))
                .collect(Collectors.joining(LIST_SEPARATOR));
            
            if (!filterString.isEmpty()) {
                keyBuilder.append(SEPARATOR).append("filters").append(filterString);
            }
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generate cache key for filter values
     * 
     * @param entityType Entity type
     * @param column Column name
     * @return Cache key for filter values
     */
    public static String generateFilterValuesKey(String entityType, String column) {
        return "filterValues" + SEPARATOR + entityType.toLowerCase() + SEPARATOR + column.toLowerCase();
    }
    
    /**
     * Generate cache key pattern for entity type (for invalidation)
     * 
     * @param entityType Entity type
     * @return Cache key pattern for matching
     */
    public static String generateEntityPattern(String entityType) {
        return "*" + entityType.toLowerCase() + "*";
    }
    
    /**
     * Hash a key using SHA-256 to ensure consistent length
     * 
     * @param key Original key
     * @return Hashed key
     */
    private static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 32); // Use first 32 characters
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 not available
            return String.valueOf(key.hashCode());
        }
    }
    
    /**
     * Extract entity type from cache key
     * 
     * @param cacheKey Cache key
     * @return Entity type or null if not found
     */
    public static String extractEntityType(String cacheKey) {
        if (cacheKey == null || !cacheKey.contains(SEPARATOR)) {
            return null;
        }
        
        String[] parts = cacheKey.split(SEPARATOR);
        if (parts.length >= 2) {
            return parts[1]; // Entity type is typically the second part
        }
        
        return null;
    }
    
    /**
     * Check if cache key matches entity type pattern
     * 
     * @param cacheKey Cache key to check
     * @param entityType Entity type to match
     * @return True if key matches entity type
     */
    public static boolean matchesEntityType(String cacheKey, String entityType) {
        if (cacheKey == null || entityType == null) {
            return false;
        }
        
        return cacheKey.toLowerCase().contains(entityType.toLowerCase());
    }
}