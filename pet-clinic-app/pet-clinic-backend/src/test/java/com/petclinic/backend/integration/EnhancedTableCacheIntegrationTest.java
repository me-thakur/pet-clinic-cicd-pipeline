package com.petclinic.backend.integration;

import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SortMetadata;
import com.petclinic.backend.service.EnhancedTableCacheService;
import com.petclinic.backend.service.EnhancedTableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Enhanced Table Caching functionality
 * Tests the complete caching workflow with real Spring context
 * Validates: Requirements 5.3
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnhancedTableCacheIntegrationTest {
    
    @Autowired
    private EnhancedTableService<Object> enhancedTableService;
    
    @Autowired
    private EnhancedTableCacheService cacheService;
    
    @Test
    void testCacheServiceIsAvailable() {
        assertNotNull(cacheService);
        assertNotNull(enhancedTableService);
    }
    
    @Test
    void testCachingEnabledByDefault() {
        assertTrue(cacheService.isCachingEnabled("visits"));
        assertTrue(cacheService.isCachingEnabled("owners"));
        assertTrue(cacheService.isCachingEnabled("pets"));
        assertTrue(cacheService.isCachingEnabled("veterinarians"));
    }
    
    @Test
    void testCacheToggling() {
        // Test disabling cache
        cacheService.setCachingEnabled("visits", false);
        assertFalse(cacheService.isCachingEnabled("visits"));
        
        // Test re-enabling cache
        cacheService.setCachingEnabled("visits", true);
        assertTrue(cacheService.isCachingEnabled("visits"));
    }
    
    @Test
    void testCacheStatistics() {
        Map<String, Object> statistics = cacheService.getTableCacheStatistics();
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("entityStatistics"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> entityStats = (Map<String, Object>) statistics.get("entityStatistics");
        assertNotNull(entityStats);
        assertTrue(entityStats.containsKey("visits"));
        assertTrue(entityStats.containsKey("owners"));
        assertTrue(entityStats.containsKey("pets"));
        assertTrue(entityStats.containsKey("veterinarians"));
    }
    
    @Test
    void testCacheHitRate() {
        double hitRate = cacheService.getTableCacheHitRate();
        assertTrue(hitRate >= 0.0 && hitRate <= 1.0);
    }
    
    @Test
    void testCacheInvalidation() {
        // This should not throw any exceptions
        assertDoesNotThrow(() -> cacheService.invalidateEntityCaches("visits"));
        assertDoesNotThrow(() -> cacheService.invalidateAllTableCaches());
    }
    
    @Test
    void testCacheWarmUp() {
        // This should not throw any exceptions
        assertDoesNotThrow(() -> cacheService.warmUpEntityCache("visits"));
        assertDoesNotThrow(() -> cacheService.warmUpEntityCache("owners"));
        assertDoesNotThrow(() -> cacheService.warmUpEntityCache("pets"));
        assertDoesNotThrow(() -> cacheService.warmUpEntityCache("veterinarians"));
    }
    
    @Test
    void testFilterValuesCaching() {
        // Test caching filter values
        List<String> testValues = List.of("value1", "value2", "value3");
        cacheService.cacheFilterValues("visits", "visitType", testValues);
        
        // Retrieve cached values
        List<String> cachedValues = cacheService.getCachedFilterValues("visits", "visitType");
        assertNotNull(cachedValues);
        assertEquals(testValues.size(), cachedValues.size());
        assertTrue(cachedValues.containsAll(testValues));
    }
    
    @Test
    void testCountCaching() {
        // Test caching count values
        List<FilterCriteria> filters = Collections.emptyList();
        Long testCount = 42L;
        
        cacheService.cacheCount("visits", filters, testCount);
        
        // Retrieve cached count
        Long cachedCount = cacheService.getCachedCount("visits", filters);
        assertNotNull(cachedCount);
        assertEquals(testCount, cachedCount);
    }
    
    @Test
    void testResultsCaching() {
        // Create test data
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        // Create mock response
        List<Object> content = List.of("item1", "item2", "item3");
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(0, 10, 3, 1, true, true, false, false);
        PagedResponse<Object> testResponse = new PagedResponse<>(content, pageInfo);
        
        // Cache the results
        cacheService.cacheResults("visits", pageable, sortMetadata, filters, testResponse);
        
        // Retrieve cached results
        PagedResponse<Object> cachedResults = cacheService.getCachedResults("visits", pageable, sortMetadata, filters);
        assertNotNull(cachedResults);
        assertEquals(testResponse.getContent().size(), cachedResults.getContent().size());
        assertEquals(testResponse.getPage().getTotalElements(), cachedResults.getPage().getTotalElements());
    }
    
    @Test
    void testCachingWithDisabledEntity() {
        // Disable caching for visits
        cacheService.setCachingEnabled("visits", false);
        
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        // Should return null when caching is disabled
        PagedResponse<Object> result = cacheService.getCachedResults("visits", pageable, sortMetadata, filters);
        assertNull(result);
        
        // Should return null for filter values when caching is disabled
        List<String> filterValues = cacheService.getCachedFilterValues("visits", "visitType");
        assertNull(filterValues);
        
        // Should return null for count when caching is disabled
        Long count = cacheService.getCachedCount("visits", filters);
        assertNull(count);
    }
    
    @Test
    void testEnhancedTableServiceIntegration() {
        // Test that the enhanced table service can be called without errors
        // This verifies that the caching integration doesn't break the service
        Pageable pageable = PageRequest.of(0, 5);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        assertDoesNotThrow(() -> {
            PagedResponse<Object> result = enhancedTableService.findWithSortAndFilter(
                "visits", pageable, sortMetadata, filters);
            assertNotNull(result);
        });
    }
    
    @Test
    void testFilterValuesServiceIntegration() {
        // Test that filter values can be retrieved without errors
        assertDoesNotThrow(() -> {
            List<String> values = enhancedTableService.getAvailableFilterValues("visits", "visitType");
            assertNotNull(values);
        });
    }
}