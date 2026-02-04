package com.petclinic.backend.util;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.SortMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheKeyGenerator
 * Tests cache key generation for different sort/filter combinations
 * Validates: Requirements 5.3
 */
class CacheKeyGeneratorTest {
    
    @Test
    void testGenerateSortFilterKeyBasic() {
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        String key = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters);
        
        assertNotNull(key);
        assertTrue(key.contains("visits"));
        assertTrue(key.contains("page0"));
        assertTrue(key.contains("size10"));
        assertTrue(key.contains("sortid"));
        assertTrue(key.contains("asc"));
    }
    
    @Test
    void testGenerateSortFilterKeyWithFilters() {
        Pageable pageable = PageRequest.of(1, 20);
        SortMetadata sortMetadata = new SortMetadata("visitDate", "desc", true);
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("visitType", "equals", "CHECKUP", "visits"),
            new FilterCriteria("pet.name", "contains", "Max", "visits")
        );
        
        String key = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters);
        
        assertNotNull(key);
        assertTrue(key.contains("visits"));
        assertTrue(key.contains("page1"));
        assertTrue(key.contains("size20"));
        assertTrue(key.contains("sortvisitdate"));
        assertTrue(key.contains("desc"));
        assertTrue(key.contains("filters"));
    }
    
    @Test
    void testGenerateSortFilterKeyConsistency() {
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("visitType", "equals", "CHECKUP", "visits"),
            new FilterCriteria("pet.name", "contains", "Max", "visits")
        );
        
        String key1 = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters);
        String key2 = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters);
        
        assertEquals(key1, key2, "Same parameters should generate same cache key");
    }
    
    @Test
    void testGenerateSortFilterKeyDifferentOrder() {
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        
        // Same filters in different order
        List<FilterCriteria> filters1 = Arrays.asList(
            new FilterCriteria("visitType", "equals", "CHECKUP", "visits"),
            new FilterCriteria("pet.name", "contains", "Max", "visits")
        );
        List<FilterCriteria> filters2 = Arrays.asList(
            new FilterCriteria("pet.name", "contains", "Max", "visits"),
            new FilterCriteria("visitType", "equals", "CHECKUP", "visits")
        );
        
        String key1 = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters1);
        String key2 = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters2);
        
        assertEquals(key1, key2, "Same filters in different order should generate same cache key");
    }
    
    @Test
    void testGenerateCountKey() {
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("visitType", "equals", "CHECKUP", "visits")
        );
        
        String key = CacheKeyGenerator.generateCountKey("visits", filters);
        
        assertNotNull(key);
        assertTrue(key.contains("count"));
        assertTrue(key.contains("visits"));
        assertTrue(key.contains("filters"));
    }
    
    @Test
    void testGenerateFilterValuesKey() {
        String key = CacheKeyGenerator.generateFilterValuesKey("visits", "visitType");
        
        assertNotNull(key);
        assertTrue(key.contains("filterValues"));
        assertTrue(key.contains("visits"));
        assertTrue(key.contains("visittype"));
    }
    
    @Test
    void testGenerateEntityPattern() {
        String pattern = CacheKeyGenerator.generateEntityPattern("visits");
        
        assertNotNull(pattern);
        assertTrue(pattern.contains("visits"));
        assertTrue(pattern.startsWith("*"));
        assertTrue(pattern.endsWith("*"));
    }
    
    @Test
    void testExtractEntityType() {
        String cacheKey = "table:visits:page0:size10:sortid:asc";
        String entityType = CacheKeyGenerator.extractEntityType(cacheKey);
        
        assertEquals("visits", entityType);
    }
    
    @Test
    void testExtractEntityTypeInvalid() {
        String entityType1 = CacheKeyGenerator.extractEntityType(null);
        String entityType2 = CacheKeyGenerator.extractEntityType("invalid");
        
        assertNull(entityType1);
        assertNull(entityType2);
    }
    
    @Test
    void testMatchesEntityType() {
        String cacheKey = "table:visits:page0:size10:sortid:asc";
        
        assertTrue(CacheKeyGenerator.matchesEntityType(cacheKey, "visits"));
        assertFalse(CacheKeyGenerator.matchesEntityType(cacheKey, "owners"));
        assertFalse(CacheKeyGenerator.matchesEntityType(null, "visits"));
        assertFalse(CacheKeyGenerator.matchesEntityType(cacheKey, null));
    }
    
    @Test
    void testLongKeyHashing() {
        // Create a very long key that should trigger hashing
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("veryLongColumnNameThatExceedsNormalLimits", "asc", true);
        
        // Create many filters to make the key very long
        List<FilterCriteria> filters = Arrays.asList(
            new FilterCriteria("veryLongFieldName1", "equals", "veryLongValueThatMakesTheKeyExtremelyLong", "visits"),
            new FilterCriteria("veryLongFieldName2", "contains", "anotherVeryLongValueThatAddsToTheLength", "visits"),
            new FilterCriteria("veryLongFieldName3", "startswith", "yetAnotherLongValueToMakeItEvenLonger", "visits"),
            new FilterCriteria("veryLongFieldName4", "endswith", "finalLongValueToEnsureHashingIsTriggered", "visits")
        );
        
        String key = CacheKeyGenerator.generateSortFilterKey("visits", pageable, sortMetadata, filters);
        
        assertNotNull(key);
        // Hashed keys should be shorter and contain the entity type
        assertTrue(key.length() <= 200);
        assertTrue(key.contains("visits"));
    }
}