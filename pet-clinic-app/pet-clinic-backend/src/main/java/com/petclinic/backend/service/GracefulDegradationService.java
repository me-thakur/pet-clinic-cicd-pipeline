package com.petclinic.backend.service;

import com.petclinic.backend.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;

/**
 * Graceful degradation for partial service failures
 * Requirements: 15.1, 15.4, 15.5
 */
@Service
public class GracefulDegradationService {
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulDegradationService.class);
    
    @Autowired
    private ServiceHealthMonitor serviceHealthMonitor;
    
    // Degradation modes for different services
    private final Map<String, DegradationMode> serviceDegradationModes = new ConcurrentHashMap<>();
    
    /**
     * Degradation modes
     */
    public enum DegradationMode {
        FULL_SERVICE,      // All features available
        REDUCED_FEATURES,  // Some features disabled
        READ_ONLY,         // Only read operations
        CACHED_DATA,       // Use cached/stale data
        BASIC_FALLBACK,    // Minimal functionality
        SERVICE_UNAVAILABLE // Service completely down
    }
    
    /**
     * Degradation strategy for a service
     */
    public static class DegradationStrategy {
        private final String serviceName;
        private final DegradationMode mode;
        private final List<String> disabledFeatures;
        private final List<String> fallbackOptions;
        private final String userMessage;
        
        public DegradationStrategy(String serviceName, DegradationMode mode, 
                                 List<String> disabledFeatures, List<String> fallbackOptions, 
                                 String userMessage) {
            this.serviceName = serviceName;
            this.mode = mode;
            this.disabledFeatures = disabledFeatures != null ? disabledFeatures : new ArrayList<>();
            this.fallbackOptions = fallbackOptions != null ? fallbackOptions : new ArrayList<>();
            this.userMessage = userMessage;
        }
        
        // Getters
        public String getServiceName() { return serviceName; }
        public DegradationMode getMode() { return mode; }
        public List<String> getDisabledFeatures() { return disabledFeatures; }
        public List<String> getFallbackOptions() { return fallbackOptions; }
        public String getUserMessage() { return userMessage; }
    }
    
    /**
     * Initialize degradation strategies
     */
    public void initializeDegradationStrategies() {
        // Database degradation
        setDegradationMode("database", DegradationMode.FULL_SERVICE);
        
        // Validation service degradation
        setDegradationMode("validation", DegradationMode.FULL_SERVICE);
        
        // Export service degradation
        setDegradationMode("export", DegradationMode.FULL_SERVICE);
        
        // Search service degradation
        setDegradationMode("search", DegradationMode.FULL_SERVICE);
        
        logger.info("Degradation strategies initialized for {} services", serviceDegradationModes.size());
    }
    
    /**
     * Set degradation mode for a service
     */
    public void setDegradationMode(String serviceName, DegradationMode mode) {
        serviceDegradationModes.put(serviceName, mode);
        logger.debug("Set degradation mode for {}: {}", serviceName, mode);
    }
    
    /**
     * Get current degradation mode for a service
     */
    public DegradationMode getDegradationMode(String serviceName) {
        return serviceDegradationModes.getOrDefault(serviceName, DegradationMode.FULL_SERVICE);
    }
    
    /**
     * Execute operation with graceful degradation
     */
    public <T> T executeWithDegradation(String serviceName, Supplier<T> primaryOperation, 
                                       Supplier<T> fallbackOperation) {
        try {
            // Check service availability
            if (serviceHealthMonitor.isServiceAvailable(serviceName)) {
                return primaryOperation.get();
            } else {
                logger.warn("Service {} unavailable, using fallback", serviceName);
                return executeFallback(serviceName, fallbackOperation);
            }
        } catch (Exception e) {
            logger.error("Primary operation failed for service {}, attempting fallback", serviceName, e);
            return executeFallback(serviceName, fallbackOperation);
        }
    }
    
    /**
     * Execute fallback operation
     */
    private <T> T executeFallback(String serviceName, Supplier<T> fallbackOperation) {
        try {
            DegradationMode mode = getDegradationMode(serviceName);
            
            if (mode == DegradationMode.SERVICE_UNAVAILABLE) {
                throw new ServiceUnavailableException(serviceName, 
                    "Service is completely unavailable", "5 minutes");
            }
            
            T result = fallbackOperation.get();
            logger.info("Fallback operation successful for service {}", serviceName);
            return result;
            
        } catch (Exception e) {
            logger.error("Fallback operation also failed for service {}", serviceName, e);
            throw new ServiceUnavailableException(serviceName, 
                "Both primary and fallback operations failed", "2 minutes");
        }
    }
    
    /**
     * Get degradation strategy for database operations
     */
    public DegradationStrategy getDatabaseDegradationStrategy() {
        DegradationMode mode = getDegradationMode("database");
        
        switch (mode) {
            case READ_ONLY:
                return new DegradationStrategy("database", mode,
                    List.of("create", "update", "delete"),
                    List.of("View existing data", "Export current data"),
                    "Database is in read-only mode. You can view data but cannot make changes.");
                    
            case CACHED_DATA:
                return new DegradationStrategy("database", mode,
                    List.of("real-time updates", "create", "update", "delete"),
                    List.of("View cached data", "Refresh when available"),
                    "Showing cached data. Some information may not be current.");
                    
            case BASIC_FALLBACK:
                return new DegradationStrategy("database", mode,
                    List.of("advanced queries", "reporting", "bulk operations"),
                    List.of("Basic data access", "Simple searches"),
                    "Database operating in basic mode. Advanced features temporarily unavailable.");
                    
            case SERVICE_UNAVAILABLE:
                return new DegradationStrategy("database", mode,
                    List.of("all database operations"),
                    List.of("Try again later", "Contact support"),
                    "Database is temporarily unavailable. Please try again later.");
                    
            default:
                return new DegradationStrategy("database", DegradationMode.FULL_SERVICE,
                    List.of(), List.of(), "All database features available.");
        }
    }
    
    /**
     * Get degradation strategy for validation service
     */
    public DegradationStrategy getValidationDegradationStrategy() {
        DegradationMode mode = getDegradationMode("validation");
        
        switch (mode) {
            case REDUCED_FEATURES:
                return new DegradationStrategy("validation", mode,
                    List.of("advanced validation", "external validation"),
                    List.of("Basic client-side validation", "Manual verification"),
                    "Using basic validation. Some advanced checks are temporarily unavailable.");
                    
            case BASIC_FALLBACK:
                return new DegradationStrategy("validation", mode,
                    List.of("server-side validation", "complex rules"),
                    List.of("Client-side validation only", "Submit with warnings"),
                    "Validation service degraded. Please verify your data carefully.");
                    
            case SERVICE_UNAVAILABLE:
                return new DegradationStrategy("validation", mode,
                    List.of("all validation"),
                    List.of("Manual verification", "Submit at your own risk"),
                    "Validation service unavailable. Please verify data manually.");
                    
            default:
                return new DegradationStrategy("validation", DegradationMode.FULL_SERVICE,
                    List.of(), List.of(), "Full validation available.");
        }
    }
    
    /**
     * Get degradation strategy for export service
     */
    public DegradationStrategy getExportDegradationStrategy() {
        DegradationMode mode = getDegradationMode("export");
        
        switch (mode) {
            case REDUCED_FEATURES:
                return new DegradationStrategy("export", mode,
                    List.of("PDF export", "advanced formatting"),
                    List.of("CSV export", "Basic data export"),
                    "PDF export temporarily unavailable. CSV export is available.");
                    
            case BASIC_FALLBACK:
                return new DegradationStrategy("export", mode,
                    List.of("formatted exports", "large datasets"),
                    List.of("Simple CSV export", "Small dataset export"),
                    "Export service in basic mode. Only simple exports available.");
                    
            case SERVICE_UNAVAILABLE:
                return new DegradationStrategy("export", mode,
                    List.of("all exports"),
                    List.of("Copy data manually", "Try again later"),
                    "Export service unavailable. Please try again later.");
                    
            default:
                return new DegradationStrategy("export", DegradationMode.FULL_SERVICE,
                    List.of(), List.of(), "All export features available.");
        }
    }
    
    /**
     * Get degradation strategy for search service
     */
    public DegradationStrategy getSearchDegradationStrategy() {
        DegradationMode mode = getDegradationMode("search");
        
        switch (mode) {
            case REDUCED_FEATURES:
                return new DegradationStrategy("search", mode,
                    List.of("global search", "advanced filters"),
                    List.of("Basic search", "Individual entity search"),
                    "Advanced search temporarily unavailable. Basic search is available.");
                    
            case BASIC_FALLBACK:
                return new DegradationStrategy("search", mode,
                    List.of("complex queries", "sorting", "filtering"),
                    List.of("Simple text search", "Browse by category"),
                    "Search service in basic mode. Use simple search terms.");
                    
            case SERVICE_UNAVAILABLE:
                return new DegradationStrategy("search", mode,
                    List.of("all search"),
                    List.of("Browse manually", "Use navigation menu"),
                    "Search service unavailable. Please browse manually.");
                    
            default:
                return new DegradationStrategy("search", DegradationMode.FULL_SERVICE,
                    List.of(), List.of(), "Full search capabilities available.");
        }
    }
    
    /**
     * Check if feature is available in current degradation mode
     */
    public boolean isFeatureAvailable(String serviceName, String feature) {
        DegradationStrategy strategy = getDegradationStrategyForService(serviceName);
        return !strategy.getDisabledFeatures().contains(feature);
    }
    
    /**
     * Get degradation strategy for any service
     */
    public DegradationStrategy getDegradationStrategyForService(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "database":
                return getDatabaseDegradationStrategy();
            case "validation":
                return getValidationDegradationStrategy();
            case "export":
                return getExportDegradationStrategy();
            case "search":
                return getSearchDegradationStrategy();
            default:
                DegradationMode mode = getDegradationMode(serviceName);
                return new DegradationStrategy(serviceName, mode, List.of(), List.of(),
                    "Service operating in " + mode.name().toLowerCase().replace('_', ' ') + " mode.");
        }
    }
    
    /**
     * Update degradation mode based on service health
     */
    public void updateDegradationBasedOnHealth(String serviceName) {
        if (!serviceHealthMonitor.isServiceAvailable(serviceName)) {
            ServiceHealthMonitor.ServiceHealth health = 
                serviceHealthMonitor.getAllServiceHealth().get(serviceName);
            
            if (health != null) {
                int failures = health.getConsecutiveFailures();
                DegradationMode newMode;
                
                if (failures >= 5) {
                    newMode = DegradationMode.SERVICE_UNAVAILABLE;
                } else if (failures >= 3) {
                    newMode = DegradationMode.BASIC_FALLBACK;
                } else if (failures >= 2) {
                    newMode = DegradationMode.REDUCED_FEATURES;
                } else {
                    newMode = DegradationMode.CACHED_DATA;
                }
                
                DegradationMode currentMode = getDegradationMode(serviceName);
                if (currentMode != newMode) {
                    setDegradationMode(serviceName, newMode);
                    logger.warn("Updated degradation mode for {} from {} to {} due to {} consecutive failures",
                               serviceName, currentMode, newMode, failures);
                }
            }
        } else {
            // Service is healthy, restore full service
            DegradationMode currentMode = getDegradationMode(serviceName);
            if (currentMode != DegradationMode.FULL_SERVICE) {
                setDegradationMode(serviceName, DegradationMode.FULL_SERVICE);
                logger.info("Restored full service for {} (was in {} mode)", serviceName, currentMode);
            }
        }
    }
    
    /**
     * Get system-wide degradation status
     */
    public Map<String, DegradationStrategy> getSystemDegradationStatus() {
        Map<String, DegradationStrategy> status = new ConcurrentHashMap<>();
        
        for (String serviceName : serviceDegradationModes.keySet()) {
            status.put(serviceName, getDegradationStrategyForService(serviceName));
        }
        
        return status;
    }
    
    /**
     * Check if system is in degraded mode
     */
    public boolean isSystemDegraded() {
        return serviceDegradationModes.values().stream()
            .anyMatch(mode -> mode != DegradationMode.FULL_SERVICE);
    }
    
    /**
     * Get user-friendly system status message
     */
    public String getSystemStatusMessage() {
        if (!isSystemDegraded()) {
            return "All systems operational";
        }
        
        long degradedServices = serviceDegradationModes.values().stream()
            .mapToLong(mode -> mode != DegradationMode.FULL_SERVICE ? 1 : 0)
            .sum();
        
        if (degradedServices == 1) {
            return "One service is experiencing issues - some features may be limited";
        } else {
            return degradedServices + " services are experiencing issues - some features may be limited";
        }
    }
}