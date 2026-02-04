package com.petclinic.backend.config;

import com.petclinic.backend.service.*;
import com.petclinic.backend.service.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Integration configuration to ensure all components are properly wired together
 * Validates that all critical services are available and properly integrated
 * Validates: All requirements - ensures complete system integration
 */
@Configuration
public class IntegrationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrationConfig.class);
    
    // Core services
    @Autowired
    private PetService petService;
    
    @Autowired
    private EnhancedPetService enhancedPetService;
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    @Autowired
    private VisitService visitService;
    
    @Autowired
    private VisitSearchService visitSearchService;
    
    // Validation services
    @Autowired
    private OwnerValidationService ownerValidationService;
    
    @Autowired
    private InternationalValidationService internationalValidationService;
    
    @Autowired
    private ValidationErrorHandlingService validationErrorHandlingService;
    
    // Error handling services
    @Autowired
    private PetOwnerDataErrorHandlingService petOwnerDataErrorHandlingService;
    
    @Autowired
    private TableSortingErrorHandlingService tableSortingErrorHandlingService;
    
    @Autowired
    private SearchErrorHandlingService searchErrorHandlingService;
    
    // Enhanced table services
    @Autowired
    private EnhancedTableService enhancedTableService;
    
    @Autowired
    private EnhancedTableCacheService enhancedTableCacheService;
    
    // Data integrity services
    @Autowired
    private SearchResultConsistencyService searchResultConsistencyService;
    
    @Autowired
    private DataIntegrityService dataIntegrityService;
    
    @Autowired
    private ConcurrentOperationSafetyService concurrentOperationSafetyService;
    
    /**
     * Validates that all critical services are properly wired and available
     * This method is called after the application context is fully initialized
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateIntegration() {
        logger.info("Starting integration validation...");
        
        try {
            // Validate core services
            validateCoreServices();
            
            // Validate validation services
            validateValidationServices();
            
            // Validate error handling services
            validateErrorHandlingServices();
            
            // Validate enhanced table services
            validateEnhancedTableServices();
            
            // Validate data integrity services
            validateDataIntegrityServices();
            
            logger.info("Integration validation completed successfully - all components are properly wired");
            
        } catch (Exception e) {
            logger.error("Integration validation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Critical services are not properly integrated", e);
        }
    }
    
    private void validateCoreServices() {
        logger.debug("Validating core services...");
        
        if (petService == null) {
            throw new IllegalStateException("PetService is not properly wired");
        }
        
        if (enhancedPetService == null) {
            throw new IllegalStateException("EnhancedPetService is not properly wired");
        }
        
        if (veterinarianService == null) {
            throw new IllegalStateException("VeterinarianService is not properly wired");
        }
        
        if (visitService == null) {
            throw new IllegalStateException("VisitService is not properly wired");
        }
        
        if (visitSearchService == null) {
            throw new IllegalStateException("VisitSearchService is not properly wired");
        }
        
        // Validate that EnhancedPetService is the primary implementation
        if (!(enhancedPetService instanceof EnhancedPetServiceImpl)) {
            logger.warn("EnhancedPetService is not using the expected implementation");
        }
        
        logger.debug("Core services validation completed");
    }
    
    private void validateValidationServices() {
        logger.debug("Validating validation services...");
        
        if (ownerValidationService == null) {
            throw new IllegalStateException("OwnerValidationService is not properly wired");
        }
        
        if (internationalValidationService == null) {
            throw new IllegalStateException("InternationalValidationService is not properly wired");
        }
        
        if (validationErrorHandlingService == null) {
            throw new IllegalStateException("ValidationErrorHandlingService is not properly wired");
        }
        
        logger.debug("Validation services validation completed");
    }
    
    private void validateErrorHandlingServices() {
        logger.debug("Validating error handling services...");
        
        if (petOwnerDataErrorHandlingService == null) {
            throw new IllegalStateException("PetOwnerDataErrorHandlingService is not properly wired");
        }
        
        if (tableSortingErrorHandlingService == null) {
            throw new IllegalStateException("TableSortingErrorHandlingService is not properly wired");
        }
        
        if (searchErrorHandlingService == null) {
            throw new IllegalStateException("SearchErrorHandlingService is not properly wired");
        }
        
        logger.debug("Error handling services validation completed");
    }
    
    private void validateEnhancedTableServices() {
        logger.debug("Validating enhanced table services...");
        
        if (enhancedTableService == null) {
            throw new IllegalStateException("EnhancedTableService is not properly wired");
        }
        
        if (enhancedTableCacheService == null) {
            throw new IllegalStateException("EnhancedTableCacheService is not properly wired");
        }
        
        logger.debug("Enhanced table services validation completed");
    }
    
    private void validateDataIntegrityServices() {
        logger.debug("Validating data integrity services...");
        
        if (searchResultConsistencyService == null) {
            throw new IllegalStateException("SearchResultConsistencyService is not properly wired");
        }
        
        if (dataIntegrityService == null) {
            throw new IllegalStateException("DataIntegrityService is not properly wired");
        }
        
        if (concurrentOperationSafetyService == null) {
            throw new IllegalStateException("ConcurrentOperationSafetyService is not properly wired");
        }
        
        logger.debug("Data integrity services validation completed");
    }
}