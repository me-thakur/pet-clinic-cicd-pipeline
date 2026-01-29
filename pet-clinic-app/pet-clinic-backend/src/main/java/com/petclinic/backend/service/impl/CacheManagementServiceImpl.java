package com.petclinic.backend.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.service.CacheManagementService;
import com.petclinic.backend.service.PetService;
import com.petclinic.backend.service.VeterinarianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of CacheManagementService providing cache management operations
 * Handles cache invalidation strategies and provides cache statistics
 * Validates: Requirements 10.3
 */
@Service
public class CacheManagementServiceImpl implements CacheManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManagementServiceImpl.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private PetService petService;
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    @Override
    public void clearAllCaches() {
        logger.info("Clearing all caches");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            }
        });
        
        logger.info("All caches cleared successfully");
    }
    
    @Override
    public void clearCache(String cacheName) {
        logger.info("Clearing cache: {}", cacheName);
        
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            logger.info("Cache {} cleared successfully", cacheName);
        } else {
            logger.warn("Cache {} not found", cacheName);
        }
    }
    
    @Override
    public void clearEntityCaches(String entityType) {
        logger.info("Clearing caches for entity type: {}", entityType);
        
        switch (entityType.toLowerCase()) {
            case "pet":
                clearCache(CacheConfig.PETS_CACHE);
                clearCache(CacheConfig.SEARCH_RESULTS_CACHE);
                clearCache(CacheConfig.STATISTICS_CACHE);
                break;
            case "veterinarian":
                clearCache(CacheConfig.VETERINARIANS_CACHE);
                clearCache(CacheConfig.STATISTICS_CACHE);
                break;
            case "visit":
                clearCache(CacheConfig.VISITS_CACHE);
                clearCache(CacheConfig.STATISTICS_CACHE);
                clearCache(CacheConfig.DASHBOARD_METRICS_CACHE);
                break;
            case "owner":
                clearCache(CacheConfig.OWNERS_CACHE);
                clearCache(CacheConfig.PETS_CACHE); // Pets are related to owners
                clearCache(CacheConfig.SEARCH_RESULTS_CACHE);
                break;
            default:
                logger.warn("Unknown entity type: {}", entityType);
        }
        
        logger.info("Entity caches cleared for: {}", entityType);
    }
    
    @Override
    public Map<String, Object> getCacheStatistics() {
        logger.debug("Getting cache statistics");
        
        Map<String, Object> statistics = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
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
                
                statistics.put(cacheName, cacheStats);
            }
        });
        
        return statistics;
    }
    
    @Override
    public Map<String, Double> getCacheHitRates() {
        logger.debug("Getting cache hit rates");
        
        Map<String, Double> hitRates = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();
                
                hitRates.put(cacheName, stats.hitRate());
            }
        });
        
        return hitRates;
    }
    
    @Override
    public void warmUpCaches() {
        logger.info("Warming up caches with frequently accessed data");
        
        try {
            // Warm up pets cache
            logger.debug("Warming up pets cache");
            petService.findAll();
            petService.getPetStatistics();
            petService.getPetCountBySpecies();
            
            // Warm up veterinarians cache
            logger.debug("Warming up veterinarians cache");
            veterinarianService.findAll();
            
            // Warm up common search results
            logger.debug("Warming up search caches");
            petService.findBySpecies("Dog");
            petService.findBySpecies("Cat");
            
            logger.info("Cache warm-up completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during cache warm-up", e);
        }
    }
    
    @Override
    public void invalidateSearchCaches() {
        logger.info("Invalidating search result caches");
        clearCache(CacheConfig.SEARCH_RESULTS_CACHE);
    }
    
    @Override
    public void invalidateStatisticsCaches() {
        logger.info("Invalidating statistics caches");
        clearCache(CacheConfig.STATISTICS_CACHE);
    }
    
    @Override
    public void invalidateDashboardCaches() {
        logger.info("Invalidating dashboard metrics caches");
        clearCache(CacheConfig.DASHBOARD_METRICS_CACHE);
    }
}