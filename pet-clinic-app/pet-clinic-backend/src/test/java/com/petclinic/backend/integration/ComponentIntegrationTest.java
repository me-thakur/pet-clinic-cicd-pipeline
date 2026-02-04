package com.petclinic.backend.integration;

import com.petclinic.backend.config.ErrorHandlingIntegrationConfig;
import com.petclinic.backend.config.ServiceIntegrationConfig;
import com.petclinic.backend.service.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify all components are properly wired together
 * Tests the complete integration of all services, controllers, and configurations
 * Validates: All requirements - ensures complete system integration
 */
@SpringBootTest
@ActiveProfiles("test")
public class ComponentIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ComponentIntegrationTest.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
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
    
    // Integration coordinators
    @Autowired
    private ServiceIntegrationConfig.ServiceIntegrationCoordinator serviceIntegrationCoordinator;
    
    @Autowired
    private ErrorHandlingIntegrationConfig.ErrorHandlingCoordinator errorHandlingCoordinator;
    
    @Test
    public void testAllCoreServicesAreWired() {
        logger.info("Testing core services integration...");
        
        assertNotNull(petService, "PetService should be wired");
        assertNotNull(enhancedPetService, "EnhancedPetService should be wired");
        assertNotNull(veterinarianService, "VeterinarianService should be wired");
        assertNotNull(visitService, "VisitService should be wired");
        assertNotNull(visitSearchService, "VisitSearchService should be wired");
        
        logger.info("All core services are properly wired");
    }
    
    @Test
    public void testAllValidationServicesAreWired() {
        logger.info("Testing validation services integration...");
        
        assertNotNull(ownerValidationService, "OwnerValidationService should be wired");
        assertNotNull(internationalValidationService, "InternationalValidationService should be wired");
        assertNotNull(validationErrorHandlingService, "ValidationErrorHandlingService should be wired");
        
        logger.info("All validation services are properly wired");
    }
    
    @Test
    public void testAllErrorHandlingServicesAreWired() {
        logger.info("Testing error handling services integration...");
        
        assertNotNull(petOwnerDataErrorHandlingService, "PetOwnerDataErrorHandlingService should be wired");
        assertNotNull(tableSortingErrorHandlingService, "TableSortingErrorHandlingService should be wired");
        assertNotNull(searchErrorHandlingService, "SearchErrorHandlingService should be wired");
        
        logger.info("All error handling services are properly wired");
    }
    
    @Test
    public void testAllEnhancedTableServicesAreWired() {
        logger.info("Testing enhanced table services integration...");
        
        assertNotNull(enhancedTableService, "EnhancedTableService should be wired");
        assertNotNull(enhancedTableCacheService, "EnhancedTableCacheService should be wired");
        
        logger.info("All enhanced table services are properly wired");
    }
    
    @Test
    public void testAllDataIntegrityServicesAreWired() {
        logger.info("Testing data integrity services integration...");
        
        assertNotNull(searchResultConsistencyService, "SearchResultConsistencyService should be wired");
        assertNotNull(dataIntegrityService, "DataIntegrityService should be wired");
        assertNotNull(concurrentOperationSafetyService, "ConcurrentOperationSafetyService should be wired");
        
        logger.info("All data integrity services are properly wired");
    }
    
    @Test
    public void testIntegrationCoordinatorsAreWired() {
        logger.info("Testing integration coordinators...");
        
        assertNotNull(serviceIntegrationCoordinator, "ServiceIntegrationCoordinator should be wired");
        assertNotNull(errorHandlingCoordinator, "ErrorHandlingCoordinator should be wired");
        
        logger.info("All integration coordinators are properly wired");
    }
    
    @Test
    public void testServiceIntegrationCoordinatorValidation() {
        logger.info("Testing service integration coordinator validation...");
        
        boolean isValid = serviceIntegrationCoordinator.validateServiceIntegration();
        assertTrue(isValid, "Service integration should be valid");
        
        logger.info("Service integration coordinator validation passed");
    }
    
    @Test
    public void testErrorHandlingCoordinatorValidation() {
        logger.info("Testing error handling coordinator validation...");
        
        boolean isValid = errorHandlingCoordinator.validateIntegration();
        assertTrue(isValid, "Error handling integration should be valid");
        
        logger.info("Error handling coordinator validation passed");
    }
    
    @Test
    public void testServiceDependencies() {
        logger.info("Testing service dependencies...");
        
        // Test that EnhancedPetService can access PetService functionality
        assertNotNull(serviceIntegrationCoordinator.getPetService(), "PetService should be accessible");
        assertNotNull(serviceIntegrationCoordinator.getEnhancedPetService(), "EnhancedPetService should be accessible");
        
        // Test that services can interact properly
        assertNotNull(serviceIntegrationCoordinator.getVisitService(), "VisitService should be accessible");
        assertNotNull(serviceIntegrationCoordinator.getVisitSearchService(), "VisitSearchService should be accessible");
        
        // Test validation services
        assertNotNull(serviceIntegrationCoordinator.getInternationalValidationService(), "InternationalValidationService should be accessible");
        
        // Test enhanced table service
        assertNotNull(serviceIntegrationCoordinator.getEnhancedTableService(), "EnhancedTableService should be accessible");
        
        logger.info("Service dependencies validation passed");
    }
    
    @Test
    public void testErrorHandlingServiceDependencies() {
        logger.info("Testing error handling service dependencies...");
        
        assertNotNull(errorHandlingCoordinator.getSearchErrorHandlingService(), "SearchErrorHandlingService should be accessible");
        assertNotNull(errorHandlingCoordinator.getTableSortingErrorHandlingService(), "TableSortingErrorHandlingService should be accessible");
        assertNotNull(errorHandlingCoordinator.getValidationErrorHandlingService(), "ValidationErrorHandlingService should be accessible");
        assertNotNull(errorHandlingCoordinator.getPetOwnerDataErrorHandlingService(), "PetOwnerDataErrorHandlingService should be accessible");
        
        logger.info("Error handling service dependencies validation passed");
    }
    
    @Test
    public void testApplicationContextContainsAllRequiredBeans() {
        logger.info("Testing application context for all required beans...");
        
        // Test core service beans
        assertTrue(applicationContext.containsBean("petServiceImpl"), "PetServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("enhancedPetServiceImpl"), "EnhancedPetServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("visitServiceImpl"), "VisitServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("visitSearchServiceImpl"), "VisitSearchServiceImpl bean should exist");
        
        // Test validation service beans
        assertTrue(applicationContext.containsBean("ownerValidationServiceImpl"), "OwnerValidationServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("internationalValidationServiceImpl"), "InternationalValidationServiceImpl bean should exist");
        
        // Test error handling service beans
        assertTrue(applicationContext.containsBean("searchErrorHandlingServiceImpl"), "SearchErrorHandlingServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("tableSortingErrorHandlingServiceImpl"), "TableSortingErrorHandlingServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("validationErrorHandlingServiceImpl"), "ValidationErrorHandlingServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("petOwnerDataErrorHandlingServiceImpl"), "PetOwnerDataErrorHandlingServiceImpl bean should exist");
        
        // Test enhanced table service beans
        assertTrue(applicationContext.containsBean("enhancedTableServiceImpl"), "EnhancedTableServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("enhancedTableCacheServiceImpl"), "EnhancedTableCacheServiceImpl bean should exist");
        
        // Test data integrity service beans
        assertTrue(applicationContext.containsBean("searchResultConsistencyServiceImpl"), "SearchResultConsistencyServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("dataIntegrityServiceImpl"), "DataIntegrityServiceImpl bean should exist");
        assertTrue(applicationContext.containsBean("concurrentOperationSafetyServiceImpl"), "ConcurrentOperationSafetyServiceImpl bean should exist");
        
        logger.info("Application context contains all required beans");
    }
    
    @Test
    public void testConfigurationBeansAreLoaded() {
        logger.info("Testing configuration beans...");
        
        assertTrue(applicationContext.containsBean("serviceIntegrationCoordinator"), "ServiceIntegrationCoordinator bean should exist");
        assertTrue(applicationContext.containsBean("errorHandlingCoordinator"), "ErrorHandlingCoordinator bean should exist");
        
        logger.info("All configuration beans are loaded");
    }
    
    @Test
    public void testCompleteSystemIntegration() {
        logger.info("Testing complete system integration...");
        
        // This test verifies that all components can work together
        // by testing a simple operation that involves multiple services
        
        try {
            // Test that we can access basic functionality from each service layer
            long petCount = petService.count();
            logger.debug("Pet count: {}", petCount);
            
            long visitCount = visitService.count();
            logger.debug("Visit count: {}", visitCount);
            
            long vetCount = veterinarianService.count();
            logger.debug("Veterinarian count: {}", vetCount);
            
            // Test that validation services are working
            assertNotNull(internationalValidationService, "International validation should be available");
            
            // Test that error handling is integrated
            assertNotNull(searchErrorHandlingService, "Search error handling should be available");
            
            logger.info("Complete system integration test passed");
            
        } catch (Exception e) {
            logger.error("System integration test failed: {}", e.getMessage(), e);
            fail("Complete system integration failed: " + e.getMessage());
        }
    }
}