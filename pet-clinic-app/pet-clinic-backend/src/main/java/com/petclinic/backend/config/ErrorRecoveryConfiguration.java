package com.petclinic.backend.config;

import com.petclinic.backend.service.ServiceHealthMonitor;
import com.petclinic.backend.service.GracefulDegradationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for error recovery system initialization
 * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5
 */
@Configuration
@EnableScheduling
public class ErrorRecoveryConfiguration {
    
    @Autowired
    private ServiceHealthMonitor serviceHealthMonitor;
    
    @Autowired
    private GracefulDegradationService gracefulDegradationService;
    
    /**
     * Initialize error recovery system when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeErrorRecoverySystem() {
        // Initialize service health monitoring
        serviceHealthMonitor.initializeServiceMonitoring();
        
        // Initialize graceful degradation strategies
        gracefulDegradationService.initializeDegradationStrategies();
        
        // Log initialization
        org.slf4j.LoggerFactory.getLogger(ErrorRecoveryConfiguration.class)
            .info("Error recovery system initialized successfully");
    }
}