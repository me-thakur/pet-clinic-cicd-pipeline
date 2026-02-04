package com.petclinic.backend.service;

import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SortMetadata;
import com.petclinic.backend.service.impl.EnhancedTableCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;

/**
 * Unit tests for EnhancedTableCacheService
 * Tests caching functionality for table operations
 * Validates: Requirements 5.3
 */
@ExtendWith(MockitoExtension.class)
class EnhancedTableCacheServiceTest {
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private Cache cache;
    
    @Mock
    private Cache.ValueWrapper valueWrapper;
    
    private EnhancedTableCacheService cacheService;
    
    @BeforeEach
    void setUp() {
        cacheService = new EnhancedTableCacheServiceImpl();
        // Use reflection to set the cacheManager field
        try {
            var field = EnhancedTableCacheServiceImpl.class.getDeclaredField("cacheManager");
            field.setAccessible(true);
            field.set(cacheService, cacheManager);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }
    }
    
    @Test
    void testCachingEnabledByDefault() {
        // Test that caching is enabled by default for all entity types
        assertTrue(cacheService.isCachingEnabled("visits"));
        assertTrue(cacheService.isCachingEnabled("owners"));
        assertTrue(cacheService.isCachingEnabled("pets"));
        assertTrue(cacheService.isCachingEnabled("veterinarians"));
    }
    
    @Test
    void testSetCachingEnabled() {
        // Test enabling/disabling caching for specific entity types
        cacheService.setCachingEnabled("visits", false);
        assertFalse(cacheService.isCachingEnabled("visits"));
        
        cacheService.setCachingEnabled("visits", true);
        assertTrue(cacheService.isCachingEnabled("visits"));
    }
    
    @Test
    void testGetCachedResultsWhenCachingDisabled() {
        // Setup cache mocks for invalidation that happens when disabling caching
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        
        // Disable caching for visits (this will trigger cache invalidation)
        cacheService.setCachingEnabled("visits", false);
        
        // Reset mock interactions after the invalidation
        reset(cacheManager);
        
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        // Should return null when caching is disabled
        PagedResponse<Object> result = cacheService.getCachedResults("visits", pageable, sortMetadata, filters);
        assertNull(result);
        
        // Verify cache was not accessed for the actual query
        verify(cacheManager, never()).getCache(anyString());
    }
    
    @Test
    void testGetCachedResultsCacheMiss() {
        // Setup cache mock to return null (cache miss)
        when(cacheManager.getCache(CacheConfig.TABLE_RESULTS_CACHE)).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null);
        
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        PagedResponse<Object> result = cacheService.getCachedResults("visits", pageable, sortMetadata, filters);
        assertNull(result);
        
        // Verify cache was accessed
        verify(cacheManager).getCache(CacheConfig.TABLE_RESULTS_CACHE);
        verify(cache).get(anyString());
    }
    
    @Test
    void testGetCachedResultsCacheHit() {
        // Create mock response
        PagedResponse<Object> mockResponse = createMockPagedResponse();
        
        // Setup cache mock to return cached result
        when(cacheManager.getCache(CacheConfig.TABLE_RESULTS_CACHE)).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(mockResponse);
        
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        
        PagedResponse<Object> result = cacheService.getCachedResults("visits", pageable, sortMetadata, filters);
        assertNotNull(result);
        assertEquals(mockResponse, result);
        
        // Verify cache was accessed
        verify(cacheManager).getCache(CacheConfig.TABLE_RESULTS_CACHE);
        verify(cache).get(anyString());
        verify(valueWrapper).get();
    }
    
    @Test
    void testCacheResults() {
        // Setup cache mock
        when(cacheManager.getCache(CacheConfig.TABLE_RESULTS_CACHE)).thenReturn(cache);
        
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        PagedResponse<Object> response = createMockPagedResponse();
        
        cacheService.cacheResults("visits", pageable, sortMetadata, filters, response);
        
        // Verify cache was accessed and result was stored
        verify(cacheManager).getCache(CacheConfig.TABLE_RESULTS_CACHE);
        verify(cache).put(anyString(), eq(response));
    }
    
    @Test
    void testCacheResultsWhenCachingDisabled() {
        // Setup cache mocks for invalidation that happens when disabling caching
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        
        // Disable caching for visits (this will trigger cache invalidation)
        cacheService.setCachingEnabled("visits", false);
        
        // Reset mock interactions after the invalidation
        reset(cacheManager);
        
        Pageable pageable = PageRequest.of(0, 10);
        SortMetadata sortMetadata = new SortMetadata("id", "asc", true);
        List<FilterCriteria> filters = Collections.emptyList();
        PagedResponse<Object> response = createMockPagedResponse();
        
        cacheService.cacheResults("visits", pageable, sortMetadata, filters, response);
        
        // Verify cache was not accessed when caching is disabled
        verify(cacheManager, never()).getCache(anyString());
    }
    
    @Test
    void testGetCachedFilterValues() {
        // Setup cache mock
        List<String> mockValues = Arrays.asList("value1", "value2", "value3");
        when(cacheManager.getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE)).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(mockValues);
        
        List<String> result = cacheService.getCachedFilterValues("visits", "visitType");
        assertNotNull(result);
        assertEquals(mockValues, result);
        
        // Verify cache was accessed
        verify(cacheManager).getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE);
        verify(cache).get(anyString());
        verify(valueWrapper).get();
    }
    
    @Test
    void testCacheFilterValues() {
        // Setup cache mock
        when(cacheManager.getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE)).thenReturn(cache);
        
        List<String> values = Arrays.asList("value1", "value2", "value3");
        cacheService.cacheFilterValues("visits", "visitType", values);
        
        // Verify cache was accessed and values were stored
        verify(cacheManager).getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE);
        verify(cache).put(anyString(), any(List.class));
    }
    
    @Test
    void testGetTableCacheStatistics() {
        Map<String, Object> statistics = cacheService.getTableCacheStatistics();
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("entityStatistics"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> entityStats = (Map<String, Object>) statistics.get("entityStatistics");
        assertTrue(entityStats.containsKey("visits"));
        assertTrue(entityStats.containsKey("owners"));
        assertTrue(entityStats.containsKey("pets"));
        assertTrue(entityStats.containsKey("veterinarians"));
    }
    
    @Test
    void testGetTableCacheHitRateWithNoRequests() {
        double hitRate = cacheService.getTableCacheHitRate();
        assertEquals(0.0, hitRate, 0.001);
    }
    
    @Test
    void testWarmUpEntityCache() {
        // This should not throw any exceptions
        assertDoesNotThrow(() -> cacheService.warmUpEntityCache("visits"));
    }
    
    @Test
    void testInvalidateAllTableCaches() {
        // Setup cache mocks
        when(cacheManager.getCache(CacheConfig.TABLE_RESULTS_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(CacheConfig.TABLE_COUNTS_CACHE)).thenReturn(cache);
        when(cacheManager.getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE)).thenReturn(cache);
        
        cacheService.invalidateAllTableCaches();
        
        // Verify all caches were cleared
        verify(cacheManager, times(3)).getCache(anyString());
        verify(cache, times(3)).clear();
    }
    
    // Helper methods
    
    private PagedResponse<Object> createMockPagedResponse() {
        List<Object> content = Arrays.asList("item1", "item2", "item3");
        PagedResponse.PageInfo pageInfo = new PagedResponse.PageInfo(0, 10, 3, 1, true, true, false, false);
        return new PagedResponse<>(content, pageInfo);
    }
}