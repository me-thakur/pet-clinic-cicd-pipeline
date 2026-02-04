package com.petclinic.backend.config;

import com.petclinic.backend.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Configuration for integrating all error handling services
 * Ensures proper error handling throughout the application
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 */
@Configuration
@DependsOn({"validationConfig", "cacheConfig"})
public class ErrorHandlingIntegrationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingIntegrationConfig.class);
    
    @Autowired
    private SearchErrorHandlingService searchErrorHandlingService;
    
    @Autowired
    private TableSortingErrorHandlingService tableSortingErrorHandlingService;
    
    @Autowired
    private ValidationErrorHandlingService validationErrorHandlingService;
    
    @Autowired
    private PetOwnerDataErrorHandlingService petOwnerDataErrorHandlingService;
    
    /**
     * Creates a comprehensive error handling coordinator
     * Integrates all error handling services for unified error management
     */
    @Bean
    public ErrorHandlingCoordinator errorHandlingCoordinator() {
        logger.info("Creating error handling coordinator with integrated services");
        
        return new ErrorHandlingCoordinator(
            searchErrorHandlingService,
            tableSortingErrorHandlingService,
            validationErrorHandlingService,
            petOwnerDataErrorHandlingService
        );
    }
    
    /**
     * Coordinator class for managing all error handling services
     */
    public static class ErrorHandlingCoordinator {
        
        private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingCoordinator.class);
        
        private final SearchErrorHandlingService searchErrorHandlingService;
        private final TableSortingErrorHandlingService tableSortingErrorHandlingService;
        private final ValidationErrorHandlingService validationErrorHandlingService;
        private final PetOwnerDataErrorHandlingService petOwnerDataErrorHandlingService;
        
        public ErrorHandlingCoordinator(
                SearchErrorHandlingService searchErrorHandlingService,
                TableSortingErrorHandlingService tableSortingErrorHandlingService,
                ValidationErrorHandlingService validationErrorHandlingService,
                PetOwnerDataErrorHandlingService petOwnerDataErrorHandlingService) {
            
            this.searchErrorHandlingService = searchErrorHandlingService;
            this.tableSortingErrorHandlingService = tableSortingErrorHandlingService;
            this.validationErrorHandlingService = validationErrorHandlingService;
            this.petOwnerDataErrorHandlingService = petOwnerDataErrorHandlingService;
            
            logger.info("Error handling coordinator initialized with all services");
        }
        
        /**
         * Validates that all error handling services are properly integrated
         */
        public boolean validateIntegration() {
            try {
                boolean allValid = true;
                
                if (searchErrorHandlingService == null) {
                    logger.error("SearchErrorHandlingService is not integrated");
                    allValid = false;
                }
                
                if (tableSortingErrorHandlingService == null) {
                    logger.error("TableSortingErrorHandlingService is not integrated");
                    allValid = false;
                }
                
                if (validationErrorHandlingService == null) {
                    logger.error("ValidationErrorHandlingService is not integrated");
                    allValid = false;
                }
                
                if (petOwnerDataErrorHandlingService == null) {
                    logger.error("PetOwnerDataErrorHandlingService is not integrated");
                    allValid = false;
                }
                
                if (allValid) {
                    logger.info("All error handling services are properly integrated");
                }
                
                return allValid;
                
            } catch (Exception e) {
                logger.error("Error during error handling integration validation: {}", e.getMessage(), e);
                return false;
            }
        }
        
        // Getters for accessing individual services
        public SearchErrorHandlingService getSearchErrorHandlingService() {
            return searchErrorHandlingService;
        }
        
        public TableSortingErrorHandlingService getTableSortingErrorHandlingService() {
            return tableSortingErrorHandlingService;
        }
        
        public ValidationErrorHandlingService getValidationErrorHandlingService() {
            return validationErrorHandlingService;
        }
        
        public PetOwnerDataErrorHandlingService getPetOwnerDataErrorHandlingService() {
            return petOwnerDataErrorHandlingService;
        }
    }
}