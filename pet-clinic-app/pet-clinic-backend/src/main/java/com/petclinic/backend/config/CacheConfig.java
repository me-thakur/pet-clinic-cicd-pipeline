package com.petclinic.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Pet Clinic application
 * Configures Caffeine cache manager with different cache policies for different data types
 * Validates: Requirements 10.3
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    // Cache names
    public static final String PETS_CACHE = "pets";
    public static final String VETERINARIANS_CACHE = "veterinarians";
    public static final String VISITS_CACHE = "visits";
    public static final String OWNERS_CACHE = "owners";
    public static final String SEARCH_RESULTS_CACHE = "searchResults";
    public static final String REPORTS_CACHE = "reports";
    public static final String DASHBOARD_METRICS_CACHE = "dashboardMetrics";
    public static final String STATISTICS_CACHE = "statistics";
    
    // Enhanced table caching
    public static final String TABLE_RESULTS_CACHE = "tableResults";
    public static final String TABLE_COUNTS_CACHE = "tableCounts";
    public static final String TABLE_FILTER_VALUES_CACHE = "tableFilterValues";
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure default cache settings
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats());
        
        // Set cache names
        cacheManager.setCacheNames(Arrays.asList(
            PETS_CACHE,
            VETERINARIANS_CACHE,
            VISITS_CACHE,
            OWNERS_CACHE,
            SEARCH_RESULTS_CACHE,
            REPORTS_CACHE,
            DASHBOARD_METRICS_CACHE,
            STATISTICS_CACHE,
            TABLE_RESULTS_CACHE,
            TABLE_COUNTS_CACHE,
            TABLE_FILTER_VALUES_CACHE
        ));
        
        return cacheManager;
    }
    
    /**
     * Cache configuration for frequently accessed entity data
     * - Pets, Veterinarians, Owners: 1 hour TTL, max 500 entries
     */
    @Bean("entityCacheBuilder")
    public Caffeine<Object, Object> entityCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats();
    }
    
    /**
     * Cache configuration for search results
     * - Search results: 15 minutes TTL, max 200 entries
     */
    @Bean("searchCacheBuilder")
    public Caffeine<Object, Object> searchCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .recordStats();
    }
    
    /**
     * Cache configuration for reports and statistics
     * - Reports: 5 minutes TTL, max 100 entries
     */
    @Bean("reportsCacheBuilder")
    public Caffeine<Object, Object> reportsCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats();
    }
    
    /**
     * Cache configuration for dashboard metrics
     * - Dashboard metrics: 2 minutes TTL, max 50 entries
     */
    @Bean("dashboardCacheBuilder")
    public Caffeine<Object, Object> dashboardCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .recordStats();
    }
    
    /**
     * Cache configuration for table results
     * - Table results: 10 minutes TTL, max 1000 entries
     * - Optimized for frequent sort/filter operations
     */
    @Bean("tableCacheBuilder")
    public Caffeine<Object, Object> tableCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES) // Evict if not accessed for 5 minutes
            .recordStats();
    }
    
    /**
     * Cache configuration for table counts
     * - Count queries: 15 minutes TTL, max 200 entries
     * - Longer TTL since counts change less frequently
     */
    @Bean("tableCountCacheBuilder")
    public Caffeine<Object, Object> tableCountCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .recordStats();
    }
    
    /**
     * Cache configuration for filter values
     * - Filter values: 30 minutes TTL, max 100 entries
     * - Longer TTL since filter options change infrequently
     */
    @Bean("filterValuesCacheBuilder")
    public Caffeine<Object, Object> filterValuesCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats();
    }
}