package com.petclinic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for caching settings
 * Allows external configuration of cache behavior
 * Validates: Requirements 5.3
 */
@Component
@ConfigurationProperties(prefix = "petclinic.cache")
public class CacheProperties {
    
    /**
     * Global cache settings
     */
    private boolean enabled = true;
    private boolean statisticsEnabled = true;
    
    /**
     * Table cache settings
     */
    private TableCache table = new TableCache();
    
    /**
     * Entity-specific cache settings
     */
    private Map<String, EntityCache> entities = new HashMap<>();
    
    public CacheProperties() {
        // Initialize default entity cache settings
        entities.put("visits", new EntityCache(true, Duration.ofMinutes(10)));
        entities.put("owners", new EntityCache(true, Duration.ofMinutes(15)));
        entities.put("pets", new EntityCache(true, Duration.ofMinutes(15)));
        entities.put("veterinarians", new EntityCache(true, Duration.ofMinutes(30)));
    }
    
    // Getters and setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
    
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }
    
    public TableCache getTable() {
        return table;
    }
    
    public void setTable(TableCache table) {
        this.table = table;
    }
    
    public Map<String, EntityCache> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, EntityCache> entities) {
        this.entities = entities;
    }
    
    /**
     * Get cache settings for specific entity type
     */
    public EntityCache getEntityCache(String entityType) {
        return entities.getOrDefault(entityType.toLowerCase(), new EntityCache());
    }
    
    /**
     * Table cache configuration
     */
    public static class TableCache {
        private Duration resultsTtl = Duration.ofMinutes(10);
        private Duration countsttl = Duration.ofMinutes(15);
        private Duration filterValuesTtl = Duration.ofMinutes(30);
        private int resultsMaxSize = 1000;
        private int countsMaxSize = 200;
        private int filterValuesMaxSize = 100;
        private Duration accessTtl = Duration.ofMinutes(5);
        
        // Getters and setters
        
        public Duration getResultsTtl() {
            return resultsTtl;
        }
        
        public void setResultsTtl(Duration resultsTtl) {
            this.resultsTtl = resultsTtl;
        }
        
        public Duration getCountsttl() {
            return countsttl;
        }
        
        public void setCountsttl(Duration countsttl) {
            this.countsttl = countsttl;
        }
        
        public Duration getFilterValuesTtl() {
            return filterValuesTtl;
        }
        
        public void setFilterValuesTtl(Duration filterValuesTtl) {
            this.filterValuesTtl = filterValuesTtl;
        }
        
        public int getResultsMaxSize() {
            return resultsMaxSize;
        }
        
        public void setResultsMaxSize(int resultsMaxSize) {
            this.resultsMaxSize = resultsMaxSize;
        }
        
        public int getCountsMaxSize() {
            return countsMaxSize;
        }
        
        public void setCountsMaxSize(int countsMaxSize) {
            this.countsMaxSize = countsMaxSize;
        }
        
        public int getFilterValuesMaxSize() {
            return filterValuesMaxSize;
        }
        
        public void setFilterValuesMaxSize(int filterValuesMaxSize) {
            this.filterValuesMaxSize = filterValuesMaxSize;
        }
        
        public Duration getAccessTtl() {
            return accessTtl;
        }
        
        public void setAccessTtl(Duration accessTtl) {
            this.accessTtl = accessTtl;
        }
    }
    
    /**
     * Entity-specific cache configuration
     */
    public static class EntityCache {
        private boolean enabled = true;
        private Duration ttl = Duration.ofMinutes(10);
        
        public EntityCache() {}
        
        public EntityCache(boolean enabled, Duration ttl) {
            this.enabled = enabled;
            this.ttl = ttl;
        }
        
        // Getters and setters
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Duration getTtl() {
            return ttl;
        }
        
        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}