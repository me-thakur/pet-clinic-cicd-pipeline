package com.petclinic.backend.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.dto.FilterCriteria;
import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SortMetadata;
import com.petclinic.backend.service.EnhancedTableCacheService;
import com.petclinic.backend.util.CacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of EnhancedTableCacheService providing caching for table operations
 * Uses Caffeine cache for high-performance in-memory caching of sort/filter results
 * Validates: Requirements 5.3
 */
@Service
public class EnhancedTableCacheServiceImpl implements EnhancedTableCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTableCacheServiceImpl.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    // Track which entity types have caching enabled
    private final Map<String, Boolean> cachingEnabled = new ConcurrentHashMap<>();
    
    // Cache hit/miss counters for monitoring
    private final Map<String, Long> cacheHits = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheMisses = new ConcurrentHashMap<>();
    
    public EnhancedTableCacheServiceImpl() {
        // Enable caching for all entity types by default
        cachingEnabled.put("visits", true);
        cachingEnabled.put("owners", true);
        cachingEnabled.put("pets", true);
        cachingEnabled.put("veterinarians", true);
    }
    
    @Override
    public PagedResponse<Object> getCachedResults(String entityType, Pageable pageable, 
                                                SortMetadata sortMetadata, List<FilterCriteria> filters) {
        if (!isCachingEnabled(entityType)) {
            return null;
        }
        
        String cacheKey = CacheKeyGenerator.generateSortFilterKey(entityType, pageable, sortMetadata, filters);
        logger.debug("Looking for cached results with key: {}", cacheKey);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.TABLE_RESULTS_CACHE);
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                incrementCacheHits(entityType);
                logger.debug("Cache hit for key: {}", cacheKey);
                return (PagedResponse<Object>) wrapper.get();
            }
        }
        
        incrementCacheMisses(entityType);
        logger.debug("Cache miss for key: {}", cacheKey);
        return null;
    }
    
    @Override
    public void cacheResults(String entityType, Pageable pageable, SortMetadata sortMetadata, 
                           List<FilterCriteria> filters, PagedResponse<Object> results) {
        if (!isCachingEnabled(entityType) || results == null) {
            return;
        }
        
        String cacheKey = CacheKeyGenerator.generateSortFilterKey(entityType, pageable, sortMetadata, filters);
        logger.debug("Caching results with key: {}", cacheKey);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.TABLE_RESULTS_CACHE);
        if (cache != null) {
            cache.put(cacheKey, results);
            logger.debug("Cached {} results for key: {}", results.getContent().size(), cacheKey);
        }
    }
    
    @Override
    public Long getCachedCount(String entityType, List<FilterCriteria> filters) {
        if (!isCachingEnabled(entityType)) {
            return null;
        }
        
        String cacheKey = CacheKeyGenerator.generateCountKey(entityType, filters);
        logger.debug("Looking for cached count with key: {}", cacheKey);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.TABLE_COUNTS_CACHE);
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                incrementCacheHits(entityType + "_count");
                logger.debug("Cache hit for count key: {}", cacheKey);
                return (Long) wrapper.get();
            }
        }
        
        incrementCacheMisses(entityType + "_count");
        logger.debug("Cache miss for count key: {}", cacheKey);
        return null;
    }
    
    @Override
    public void cacheCount(String entityType, List<FilterCriteria> filters, Long count) {
        if (!isCachingEnabled(entityType) || count == null) {
            return;
        }
        
        String cacheKey = CacheKeyGenerator.generateCountKey(entityType, filters);
        logger.debug("Caching count {} with key: {}", count, cacheKey);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.TABLE_COUNTS_CACHE);
        if (cache != null) {
            cache.put(cacheKey, count);
            logger.debug("Cached count {} for key: {}", count, cacheKey);
        }
    }
    
    @Override
    public List<String> getCachedFilterValues(String entityType, String column) {
        if (!isCachingEnabled(entityType)) {
            return null;
        }
        
        String cacheKey = CacheKeyGenerator.generateFilterValuesKey(entityType, column);
        logger.debug("Looking for cached filter values with key: {}", cacheKey);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE);
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                incrementCacheHits(entityType + "_filter");
                logger.debug("Cache hit for filter values key: {}", cacheKey);
                return (List<String>) wrapper.get();
            }
        }
        
        incrementCacheMisses(entityType + "_filter");
        logger.debug("Cache miss for filter values key: {}", cacheKey);
        return null;
    }
    
    @Override
    public void cacheFilterValues(String entityType, String column, List<String> values) {
        if (!isCachingEnabled(entityType) || values == null) {
            return;
        }
        
        String cacheKey = CacheKeyGenerator.generateFilterValuesKey(entityType, column);
        logger.debug("Caching {} filter values with key: {}", values.size(), cacheKey);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.TABLE_FILTER_VALUES_CACHE);
        if (cache != null) {
            cache.put(cacheKey, new ArrayList<>(values)); // Create defensive copy
            logger.debug("Cached {} filter values for key: {}", values.size(), cacheKey);
        }
    }
    
    @Override
    public void invalidateEntityCaches(String entityType) {
        logger.info("Invalidating all caches for entity type: {}", entityType);
        
        // Clear all table-related caches for this entity type
        clearCacheByPattern(CacheConfig.TABLE_RESULTS_CACHE, entityType);
        clearCacheByPattern(CacheConfig.TABLE_COUNTS_CACHE, entityType);
        clearCacheByPattern(CacheConfig.TABLE_FILTER_VALUES_CACHE, entityType);
        
        // Reset hit/miss counters
        resetCounters(entityType);
        
        logger.info("Invalidated all caches for entity type: {}", entityType);
    }
    
    @Override
    public void invalidateAllTableCaches() {
        logger.info("Invalidating all table caches");
        
        // Clear all table-related caches
        clearCache(CacheConfig.TABLE_RESULTS_CACHE);
        clearCache(CacheConfig.TABLE_COUNTS_CACHE);
        clearCache(CacheConfig.TABLE_FILTER_VALUES_CACHE);
        
        // Reset all counters
        cacheHits.clear();
        cacheMisses.clear();
        
        logger.info("Invalidated all table caches");
    }
    
    @Override
    public Map<String, Object> getTableCacheStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Get statistics for each table cache
        addCacheStatistics(statistics, CacheConfig.TABLE_RESULTS_CACHE, "results");
        addCacheStatistics(statistics, CacheConfig.TABLE_COUNTS_CACHE, "counts");
        addCacheStatistics(statistics, CacheConfig.TABLE_FILTER_VALUES_CACHE, "filterValues");
        
        // Add custom hit/miss statistics
        Map<String, Object> customStats = new HashMap<>();
        for (String entityType : Arrays.asList("visits", "owners", "pets", "veterinarians")) {
            Map<String, Object> entityStats = new HashMap<>();
            entityStats.put("hits", cacheHits.getOrDefault(entityType, 0L));
            entityStats.put("misses", cacheMisses.getOrDefault(entityType, 0L));
            entityStats.put("countHits", cacheHits.getOrDefault(entityType + "_count", 0L));
            entityStats.put("countMisses", cacheMisses.getOrDefault(entityType + "_count", 0L));
            entityStats.put("filterHits", cacheHits.getOrDefault(entityType + "_filter", 0L));
            entityStats.put("filterMisses", cacheMisses.getOrDefault(entityType + "_filter", 0L));
            entityStats.put("cachingEnabled", isCachingEnabled(entityType));
            
            customStats.put(entityType, entityStats);
        }
        statistics.put("entityStatistics", customStats);
        
        return statistics;
    }
    
    @Override
    public double getTableCacheHitRate() {
        long totalHits = cacheHits.values().stream().mapToLong(Long::longValue).sum();
        long totalMisses = cacheMisses.values().stream().mapToLong(Long::longValue).sum();
        long totalRequests = totalHits + totalMisses;
        
        if (totalRequests == 0) {
            return 0.0;
        }
        
        return (double) totalHits / totalRequests;
    }
    
    @Override
    public void warmUpEntityCache(String entityType) {
        logger.info("Warming up cache for entity type: {}", entityType);
        
        // This would typically pre-load common queries
        // For now, just log the operation
        logger.debug("Cache warm-up for {} would pre-load common sort/filter combinations", entityType);
        
        // In a real implementation, this might:
        // 1. Load first page with default sort
        // 2. Load common filter combinations
        // 3. Load filter values for dropdown fields
        
        logger.info("Cache warm-up completed for entity type: {}", entityType);
    }
    
    @Override
    public boolean isCachingEnabled(String entityType) {
        return cachingEnabled.getOrDefault(entityType.toLowerCase(), false);
    }
    
    @Override
    public void setCachingEnabled(String entityType, boolean enabled) {
        logger.info("Setting caching {} for entity type: {}", enabled ? "enabled" : "disabled", entityType);
        cachingEnabled.put(entityType.toLowerCase(), enabled);
        
        if (!enabled) {
            // Clear caches when disabling
            invalidateEntityCaches(entityType);
        }
    }
    
    // Private helper methods
    
    private void incrementCacheHits(String key) {
        cacheHits.merge(key, 1L, Long::sum);
    }
    
    private void incrementCacheMisses(String key) {
        cacheMisses.merge(key, 1L, Long::sum);
    }
    
    private void resetCounters(String entityType) {
        cacheHits.remove(entityType);
        cacheMisses.remove(entityType);
        cacheHits.remove(entityType + "_count");
        cacheMisses.remove(entityType + "_count");
        cacheHits.remove(entityType + "_filter");
        cacheMisses.remove(entityType + "_filter");
    }
    
    private void clearCache(String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            logger.debug("Cleared cache: {}", cacheName);
        }
    }
    
    private void clearCacheByPattern(String cacheName, String entityType) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            
            // Remove entries that match the entity type
            Set<Object> keysToRemove = new HashSet<>();
            nativeCache.asMap().keySet().forEach(key -> {
                if (key instanceof String && CacheKeyGenerator.matchesEntityType((String) key, entityType)) {
                    keysToRemove.add(key);
                }
            });
            
            nativeCache.invalidateAll(keysToRemove);
            logger.debug("Cleared {} entries from cache {} for entity type {}", 
                        keysToRemove.size(), cacheName, entityType);
        }
    }
    
    private void addCacheStatistics(Map<String, Object> statistics, String cacheName, String prefix) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats stats = nativeCache.stats();
            
            Map<String, Object> cacheStats = new HashMap<>();
            cacheStats.put("hitCount", stats.hitCount());
            cacheStats.put("missCount", stats.missCount());
            cacheStats.put("hitRate", stats.hitRate());
            cacheStats.put("missRate", stats.missRate());
            cacheStats.put("requestCount", stats.requestCount());
            cacheStats.put("evictionCount", stats.evictionCount());
            cacheStats.put("estimatedSize", nativeCache.estimatedSize());
            
            statistics.put(prefix, cacheStats);
        }
    }
}