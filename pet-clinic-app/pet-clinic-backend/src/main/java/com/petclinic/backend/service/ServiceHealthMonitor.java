package com.petclinic.backend.service;

import com.petclinic.backend.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

/**
 * Service health monitoring with fallback systems and availability detection
 * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5
 */
@Service
public class ServiceHealthMonitor implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthMonitor.class);
    
    @Autowired
    private DataSource dataSource;
    
    // Service health status tracking
    private final Map<String, ServiceHealth> serviceHealthMap = new ConcurrentHashMap<>();
    private final AtomicBoolean systemHealthy = new AtomicBoolean(true);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    
    // Health check configuration
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
    private static final long SERVICE_TIMEOUT = 5000; // 5 seconds
    
    /**
     * Service health information
     */
    public static class ServiceHealth {
        private final String serviceName;
        private boolean available;
        private LocalDateTime lastCheck;
        private LocalDateTime lastFailure;
        private int consecutiveFailures;
        private String lastError;
        private List<String> fallbackOptions;
        
        public ServiceHealth(String serviceName) {
            this.serviceName = serviceName;
            this.available = true;
            this.lastCheck = LocalDateTime.now();
            this.consecutiveFailures = 0;
            this.fallbackOptions = new ArrayList<>();
        }
        
        // Getters and setters
        public String getServiceName() { return serviceName; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public LocalDateTime getLastCheck() { return lastCheck; }
        public void setLastCheck(LocalDateTime lastCheck) { this.lastCheck = lastCheck; }
        public LocalDateTime getLastFailure() { return lastFailure; }
        public void setLastFailure(LocalDateTime lastFailure) { this.lastFailure = lastFailure; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
        public List<String> getFallbackOptions() { return fallbackOptions; }
        public void setFallbackOptions(List<String> fallbackOptions) { this.fallbackOptions = fallbackOptions; }
    }
    
    /**
     * Initialize service monitoring
     */
    public void initializeServiceMonitoring() {
        // Register core services
        registerService("database", List.of("Use cached data when available", "Enable read-only mode"));
        registerService("validation", List.of("Use client-side validation", "Allow basic validation only"));
        registerService("export", List.of("Queue export for later", "Use simplified export format"));
        registerService("search", List.of("Use basic search", "Search individual entities"));
        
        logger.info("Service health monitoring initialized for {} services", serviceHealthMap.size());
    }
    
    /**
     * Register a service for monitoring
     */
    public void registerService(String serviceName, List<String> fallbackOptions) {
        ServiceHealth health = new ServiceHealth(serviceName);
        health.setFallbackOptions(fallbackOptions);
        serviceHealthMap.put(serviceName, health);
        logger.debug("Registered service for monitoring: {}", serviceName);
    }
    
    /**
     * Check if a service is available
     */
    public boolean isServiceAvailable(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health == null) {
            logger.warn("Unknown service requested: {}", serviceName);
            return true; // Assume available if not monitored
        }
        
        return health.isAvailable();
    }
    
    /**
     * Ensure service is available or throw exception
     */
    public void ensureServiceAvailable(String serviceName) throws ServiceUnavailableException {
        if (!isServiceAvailable(serviceName)) {
            ServiceHealth health = serviceHealthMap.get(serviceName);
            String retryAfter = calculateRetryAfter(health);
            
            throw new ServiceUnavailableException(
                serviceName,
                "Service " + serviceName + " is currently unavailable",
                retryAfter,
                health != null ? health.getFallbackOptions() : new ArrayList<>()
            );
        }
    }
    
    /**
     * Calculate retry after time based on failure history
     */
    private String calculateRetryAfter(ServiceHealth health) {
        if (health == null) return "30 seconds";
        
        int failures = health.getConsecutiveFailures();
        if (failures <= 1) return "30 seconds";
        if (failures <= 3) return "1 minute";
        if (failures <= 5) return "2 minutes";
        return "5 minutes";
    }
    
    /**
     * Record service failure
     */
    public void recordServiceFailure(String serviceName, String error) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health == null) {
            logger.warn("Attempted to record failure for unknown service: {}", serviceName);
            return;
        }
        
        health.setAvailable(false);
        health.setLastFailure(LocalDateTime.now());
        health.setLastError(error);
        health.setConsecutiveFailures(health.getConsecutiveFailures() + 1);
        
        logger.warn("Service {} failed (consecutive failures: {}): {}", 
                   serviceName, health.getConsecutiveFailures(), error);
        
        // Update system health
        updateSystemHealth();
    }
    
    /**
     * Record service recovery
     */
    public void recordServiceRecovery(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health == null) {
            logger.warn("Attempted to record recovery for unknown service: {}", serviceName);
            return;
        }
        
        boolean wasUnavailable = !health.isAvailable();
        health.setAvailable(true);
        health.setConsecutiveFailures(0);
        health.setLastError(null);
        health.setLastCheck(LocalDateTime.now());
        
        if (wasUnavailable) {
            logger.info("Service {} recovered", serviceName);
        }
        
        // Update system health
        updateSystemHealth();
    }
    
    /**
     * Update overall system health status
     */
    private void updateSystemHealth() {
        long unavailableServices = serviceHealthMap.values().stream()
            .mapToLong(health -> health.isAvailable() ? 0 : 1)
            .sum();
        
        boolean wasHealthy = systemHealthy.get();
        boolean isHealthy = unavailableServices == 0;
        
        systemHealthy.set(isHealthy);
        
        if (wasHealthy && !isHealthy) {
            logger.warn("System health degraded - {} services unavailable", unavailableServices);
        } else if (!wasHealthy && isHealthy) {
            logger.info("System health restored - all services available");
        }
    }
    
    /**
     * Scheduled health checks
     */
    @Scheduled(fixedRate = HEALTH_CHECK_INTERVAL)
    public void performHealthChecks() {
        logger.debug("Performing scheduled health checks for {} services", serviceHealthMap.size());
        
        for (ServiceHealth health : serviceHealthMap.values()) {
            try {
                boolean isHealthy = checkServiceHealth(health.getServiceName());
                
                if (isHealthy) {
                    recordServiceRecovery(health.getServiceName());
                } else {
                    recordServiceFailure(health.getServiceName(), "Health check failed");
                }
                
            } catch (Exception e) {
                recordServiceFailure(health.getServiceName(), "Health check error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check health of specific service
     */
    private boolean checkServiceHealth(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "database":
                return checkDatabaseHealth();
            case "validation":
                return checkValidationServiceHealth();
            case "export":
                return checkExportServiceHealth();
            case "search":
                return checkSearchServiceHealth();
            default:
                logger.debug("No specific health check for service: {}", serviceName);
                return true;
        }
    }
    
    /**
     * Check database connectivity
     */
    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid((int) (SERVICE_TIMEOUT / 1000));
        } catch (SQLException e) {
            logger.debug("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check validation service health
     */
    private boolean checkValidationServiceHealth() {
        // Implement validation service health check
        // For now, assume it's healthy
        return true;
    }
    
    /**
     * Check export service health
     */
    private boolean checkExportServiceHealth() {
        // Check if export directory is writable and has space
        try {
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            return tempDir.canWrite() && tempDir.getFreeSpace() > 100 * 1024 * 1024; // 100MB
        } catch (Exception e) {
            logger.debug("Export service health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check search service health
     */
    private boolean checkSearchServiceHealth() {
        // Implement search service health check
        // For now, assume it's healthy
        return true;
    }
    
    /**
     * Get health status for all services
     */
    public Map<String, ServiceHealth> getAllServiceHealth() {
        return new ConcurrentHashMap<>(serviceHealthMap);
    }
    
    /**
     * Get system health summary
     */
    public Health getSystemHealth() {
        Health.Builder builder = systemHealthy.get() ? Health.up() : Health.down();
        
        for (Map.Entry<String, ServiceHealth> entry : serviceHealthMap.entrySet()) {
            ServiceHealth health = entry.getValue();
            builder.withDetail(entry.getKey(), Map.of(
                "available", health.isAvailable(),
                "lastCheck", health.getLastCheck(),
                "consecutiveFailures", health.getConsecutiveFailures(),
                "lastError", health.getLastError() != null ? health.getLastError() : "None"
            ));
        }
        
        return builder.build();
    }
    
    /**
     * Spring Boot Actuator health indicator
     */
    @Override
    public Health health() {
        return getSystemHealth();
    }
    
    /**
     * Get fallback options for a service
     */
    public List<String> getFallbackOptions(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        return health != null ? health.getFallbackOptions() : new ArrayList<>();
    }
    
    /**
     * Reset service health (for testing)
     */
    public void resetServiceHealth(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health != null) {
            health.setAvailable(true);
            health.setConsecutiveFailures(0);
            health.setLastError(null);
            health.setLastCheck(LocalDateTime.now());
            updateSystemHealth();
        }
    }
    
    /**
     * Get service uptime percentage
     */
    public double getServiceUptime(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        if (health == null) return 100.0;
        
        // Simple calculation based on consecutive failures
        int failures = health.getConsecutiveFailures();
        if (failures == 0) return 100.0;
        if (failures >= 10) return 0.0;
        
        return Math.max(0.0, 100.0 - (failures * 10.0));
    }
}