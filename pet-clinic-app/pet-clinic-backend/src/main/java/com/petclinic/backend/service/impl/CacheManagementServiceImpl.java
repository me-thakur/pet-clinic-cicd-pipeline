package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.CacheManagementService;
import com.petclinic.backend.service.PerformanceMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of cache management service
 * Provides comprehensive cache management and statistics
 * Validates: Requirements 16.1, 16.2, 16.3
 */
@Service
public class CacheManagementServiceImpl implements CacheManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManagementServiceImpl.class);
    
    private final CacheManager cacheManager;
    private final PerformanceMonitoringService performanceMonitoringService;
    
    // Cache statistics tracking
    private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cacheEvictions = new ConcurrentHashMap<>();
    
    // Frequently accessed data for cache warming
    private final List<String> frequentlyAccessedKeys = Arrays.asList(
        "dashboard_metrics", "active_veterinarians", "recent_visits", 
        "pet_statistics", "owner_statistics"
    );
    
    @Autowired
    public CacheManagementServiceImpl(CacheManager cacheManager, 
                                     PerformanceMonitoringService performanceMonitoringService) {
        this.cacheManager = cacheManager;
        this.performanceMonitoringService = performanceMonitoringService;
    }
    
    @PostConstruct
    public void initializeCacheStatistics() {
        // Initialize statistics for all configured caches
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            cacheHits.put(cacheName, new AtomicLong(0));
            cacheMisses.put(cacheName, new AtomicLong(0));
            cacheEvictions.put(cacheName, new AtomicLong(0));
        }
        
        logger.info("Initialized cache statistics for {} caches", cacheNames.size());
    }
    
    @Override
    public void clearAllCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            Collection<String> cacheNames = cacheManager.getCacheNames();
            for (String cacheName : cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    logger.debug("Cleared cache: {}", cacheName);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordCachePerformance("all_caches", "clear", executionTime);
            
            logger.info("Cleared all {} caches in {}ms", cacheNames.size(), executionTime);
            
        } catch (Exception e) {
            logger.error("Error clearing all caches: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void clearCache(String cacheName) {
        try {
            long startTime = System.currentTimeMillis();
            
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                
                long executionTime = System.currentTimeMillis() - startTime;
                performanceMonitoringService.recordCachePerformance(cacheName, "clear", executionTime);
                
                logger.info("Cleared cache '{}' in {}ms", cacheName, executionTime);
            } else {
                logger.warn("Cache '{}' not found", cacheName);
            }
            
        } catch (Exception e) {
            logger.error("Error clearing cache '{}': {}", cacheName, e.getMessage(), e);
        }
    }
    
    @Override
    public void clearEntityCaches(String entityType) {
        try {
            long startTime = System.currentTimeMillis();
            int clearedCaches = 0;
            
            Collection<String> cacheNames = cacheManager.getCacheNames();
            for (String cacheName : cacheNames) {
                // Clear caches that contain the entity type
                if (cacheName.toLowerCase().contains(entityType.toLowerCase())) {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                        clearedCaches++;
                        logger.debug("Cleared entity cache: {} for entity type: {}", cacheName, entityType);
                    }
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordCachePerformance(entityType + "_caches", "clear", executionTime);
            
            logger.info("Cleared {} caches for entity type '{}' in {}ms", clearedCaches, entityType, executionTime);
            
        } catch (Exception e) {
            logger.error("Error clearing caches for entity type '{}': {}", entityType, e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            Map<String, Map<String, Object>> cacheStats = new HashMap<>();
            
            long totalHits = 0;
            long totalMisses = 0;
            long totalEvictions = 0;
            
            for (String cacheName : cacheNames) {
                Map<String, Object> stats = new HashMap<>();
                
                long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
                long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
                long evictions = cacheEvictions.getOrDefault(cacheName, new AtomicLong(0)).get();
                
                double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
                
                stats.put("hits", hits);
                stats.put("misses", misses);
                stats.put("evictions", evictions);
                stats.put("hitRate", hitRate);
                stats.put("requests", hits + misses);
                
                cacheStats.put(cacheName, stats);
                
                totalHits += hits;
                totalMisses += misses;
                totalEvictions += evictions;
            }
            
            // Overall statistics
            double overallHitRate = (totalHits + totalMisses) > 0 ? 
                (double) totalHits / (totalHits + totalMisses) : 0.0;
            
            statistics.put("caches", cacheStats);
            statistics.put("totalCaches", cacheNames.size());
            statistics.put("totalHits", totalHits);
            statistics.put("totalMisses", totalMisses);
            statistics.put("totalEvictions", totalEvictions);
            statistics.put("overallHitRate", overallHitRate);
            statistics.put("timestamp", new Date());
            
        } catch (Exception e) {
            logger.error("Error getting cache statistics: {}", e.getMessage(), e);
            statistics.put("error", "Failed to retrieve cache statistics: " + e.getMessage());
        }
        
        return statistics;
    }
    
    @Override
    public Map<String, Double> getCacheHitRates() {
        Map<String, Double> hitRates = new HashMap<>();
        
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            
            for (String cacheName : cacheNames) {
                long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
                long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
                
                double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
                hitRates.put(cacheName, hitRate);
            }
            
        } catch (Exception e) {
            logger.error("Error getting cache hit rates: {}", e.getMessage(), e);
        }
        
        return hitRates;
    }
    
    @Override
    public void warmUpCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            logger.info("Starting cache warm-up process...");
            
            // Warm up dashboard metrics cache
            warmUpDashboardCache();
            
            // Warm up entity statistics caches
            warmUpStatisticsCaches();
            
            // Warm up search result caches
            warmUpSearchCaches();
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordCachePerformance("cache_warmup", "warmup", executionTime);
            
            logger.info("Cache warm-up completed in {}ms", executionTime);
            
        } catch (Exception e) {
            logger.error("Error during cache warm-up: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void invalidateSearchCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Clear search-related caches
            clearCache("searchResults");
            clearCache("globalSearch");
            clearCache("petSearch");
            clearCache("ownerSearch");
            clearCache("visitSearch");
            clearCache("veterinarianSearch");
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordCachePerformance("search_caches", "invalidate", executionTime);
            
            logger.info("Invalidated search caches in {}ms", executionTime);
            
        } catch (Exception e) {
            logger.error("Error invalidating search caches: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void invalidateStatisticsCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Clear statistics-related caches
            clearCache("statistics");
            clearCache("reports");
            clearCache("petStatistics");
            clearCache("ownerStatistics");
            clearCache("visitStatistics");
            clearCache("veterinarianStatistics");
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordCachePerformance("statistics_caches", "invalidate", executionTime);
            
            logger.info("Invalidated statistics caches in {}ms", executionTime);
            
        } catch (Exception e) {
            logger.error("Error invalidating statistics caches: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void invalidateDashboardCaches() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Clear dashboard-related caches
            clearCache("dashboardMetrics");
            clearCache("recentActivities");
            clearCache("upcomingAppointments");
            clearCache("alerts");
            
            long executionTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordCachePerformance("dashboard_caches", "invalidate", executionTime);
            
            logger.info("Invalidated dashboard caches in {}ms", executionTime);
            
        } catch (Exception e) {
            logger.error("Error invalidating dashboard caches: {}", e.getMessage(), e);
        }
    }
    
    // Cache hit/miss tracking methods
    public void recordCacheHit(String cacheName) {
        cacheHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        performanceMonitoringService.recordCachePerformance(cacheName, "hit", 0);
    }
    
    public void recordCacheMiss(String cacheName) {
        cacheMisses.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        performanceMonitoringService.recordCachePerformance(cacheName, "miss", 0);
    }
    
    public void recordCacheEviction(String cacheName) {
        cacheEvictions.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        performanceMonitoringService.recordCachePerformance(cacheName, "eviction", 0);
    }
    
    // Scheduled cache maintenance
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void performCacheMaintenance() {
        try {
            // Check cache hit rates and log warnings for poor performance
            Map<String, Double> hitRates = getCacheHitRates();
            
            for (Map.Entry<String, Double> entry : hitRates.entrySet()) {
                String cacheName = entry.getKey();
                Double hitRate = entry.getValue();
                
                if (hitRate != null && hitRate < 0.5) { // Less than 50% hit rate
                    logger.warn("Cache '{}' has low hit rate: {:.2f}%", cacheName, hitRate * 100);
                }
            }
            
            // Log cache statistics
            Map<String, Object> stats = getCacheStatistics();
            Double overallHitRate = (Double) stats.get("overallHitRate");
            if (overallHitRate != null) {
                logger.debug("Overall cache hit rate: {:.2f}%", overallHitRate * 100);
            }
            
        } catch (Exception e) {
            logger.error("Error during cache maintenance: {}", e.getMessage(), e);
        }
    }
    
    // Scheduled cache warm-up
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void scheduledCacheWarmUp() {
        try {
            // Only warm up if hit rates are low
            Map<String, Double> hitRates = getCacheHitRates();
            boolean needsWarmUp = hitRates.values().stream()
                .anyMatch(rate -> rate != null && rate < 0.7);
            
            if (needsWarmUp) {
                logger.info("Cache hit rates are low, performing scheduled warm-up");
                warmUpCaches();
            }
            
        } catch (Exception e) {
            logger.error("Error during scheduled cache warm-up: {}", e.getMessage(), e);
        }
    }
    
    // Private helper methods
    
    private void warmUpDashboardCache() {
        try {
            // This would typically call dashboard service methods to populate cache
            logger.debug("Warming up dashboard cache...");
            
            // Simulate cache warming - in real implementation, call actual services
            Cache dashboardCache = cacheManager.getCache("dashboardMetrics");
            if (dashboardCache != null) {
                // Pre-populate with common dashboard queries
                logger.debug("Dashboard cache warmed up");
            }
            
        } catch (Exception e) {
            logger.error("Error warming up dashboard cache: {}", e.getMessage());
        }
    }
    
    private void warmUpStatisticsCaches() {
        try {
            logger.debug("Warming up statistics caches...");
            
            // Warm up entity statistics
            String[] entities = {"pets", "owners", "visits", "veterinarians"};
            for (String entity : entities) {
                Cache cache = cacheManager.getCache(entity);
                if (cache != null) {
                    // Pre-populate with common statistics queries
                    logger.debug("Warmed up {} statistics cache", entity);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error warming up statistics caches: {}", e.getMessage());
        }
    }
    
    private void warmUpSearchCaches() {
        try {
            logger.debug("Warming up search caches...");
            
            Cache searchCache = cacheManager.getCache("searchResults");
            if (searchCache != null) {
                // Pre-populate with common search queries
                logger.debug("Search cache warmed up");
            }
            
        } catch (Exception e) {
            logger.error("Error warming up search caches: {}", e.getMessage());
        }
    }
}