package com.petclinic.backend.integration;

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
 * Simple integration test to verify all components are properly wired together
 * Tests basic service availability and integration without complex property tests
 * Validates: All requirements - ensures complete system integration
 */
@SpringBootTest
@ActiveProfiles("test")
public class SimpleIntegrationValidationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleIntegrationValidationTest.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    public void testCoreServicesAreAvailable() {
        logger.info("Testing core services availability...");
        
        // Test core services
        assertTrue(applicationContext.containsBean("petServiceImpl"), "PetServiceImpl should be available");
        assertTrue(applicationContext.containsBean("enhancedPetServiceImpl"), "EnhancedPetServiceImpl should be available");
        assertTrue(applicationContext.containsBean("visitServiceImpl"), "VisitServiceImpl should be available");
        assertTrue(applicationContext.containsBean("visitSearchServiceImpl"), "VisitSearchServiceImpl should be available");
        assertTrue(applicationContext.containsBean("veterinarianServiceImpl"), "VeterinarianServiceImpl should be available");
        
        logger.info("Core services availability test passed");
    }
    
    @Test
    public void testValidationServicesAreAvailable() {
        logger.info("Testing validation services availability...");
        
        assertTrue(applicationContext.containsBean("ownerValidationServiceImpl"), "OwnerValidationServiceImpl should be available");
        assertTrue(applicationContext.containsBean("internationalValidationServiceImpl"), "InternationalValidationServiceImpl should be available");
        assertTrue(applicationContext.containsBean("validationErrorHandlingServiceImpl"), "ValidationErrorHandlingServiceImpl should be available");
        
        logger.info("Validation services availability test passed");
    }
    
    @Test
    public void testErrorHandlingServicesAreAvailable() {
        logger.info("Testing error handling services availability...");
        
        assertTrue(applicationContext.containsBean("searchErrorHandlingServiceImpl"), "SearchErrorHandlingServiceImpl should be available");
        assertTrue(applicationContext.containsBean("tableSortingErrorHandlingServiceImpl"), "TableSortingErrorHandlingServiceImpl should be available");
        assertTrue(applicationContext.containsBean("petOwnerDataErrorHandlingServiceImpl"), "PetOwnerDataErrorHandlingServiceImpl should be available");
        
        logger.info("Error handling services availability test passed");
    }
    
    @Test
    public void testEnhancedTableServicesAreAvailable() {
        logger.info("Testing enhanced table services availability...");
        
        assertTrue(applicationContext.containsBean("enhancedTableServiceImpl"), "EnhancedTableServiceImpl should be available");
        assertTrue(applicationContext.containsBean("enhancedTableCacheServiceImpl"), "EnhancedTableCacheServiceImpl should be available");
        
        logger.info("Enhanced table services availability test passed");
    }
    
    @Test
    public void testDataIntegrityServicesAreAvailable() {
        logger.info("Testing data integrity services availability...");
        
        assertTrue(applicationContext.containsBean("searchResultConsistencyServiceImpl"), "SearchResultConsistencyServiceImpl should be available");
        assertTrue(applicationContext.containsBean("dataIntegrityServiceImpl"), "DataIntegrityServiceImpl should be available");
        assertTrue(applicationContext.containsBean("concurrentOperationSafetyServiceImpl"), "ConcurrentOperationSafetyServiceImpl should be available");
        
        logger.info("Data integrity services availability test passed");
    }
    
    @Test
    public void testIntegrationCoordinatorsAreAvailable() {
        logger.info("Testing integration coordinators availability...");
        
        assertTrue(applicationContext.containsBean("serviceIntegrationCoordinator"), "ServiceIntegrationCoordinator should be available");
        assertTrue(applicationContext.containsBean("errorHandlingCoordinator"), "ErrorHandlingCoordinator should be available");
        
        logger.info("Integration coordinators availability test passed");
    }
    
    @Test
    public void testServiceDependencyInjection() {
        logger.info("Testing service dependency injection...");
        
        // Test that services can be injected
        PetService petService = applicationContext.getBean(PetService.class);
        assertNotNull(petService, "PetService should be injectable");
        
        EnhancedPetService enhancedPetService = applicationContext.getBean(EnhancedPetService.class);
        assertNotNull(enhancedPetService, "EnhancedPetService should be injectable");
        
        VisitService visitService = applicationContext.getBean(VisitService.class);
        assertNotNull(visitService, "VisitService should be injectable");
        
        VisitSearchService visitSearchService = applicationContext.getBean(VisitSearchService.class);
        assertNotNull(visitSearchService, "VisitSearchService should be injectable");
        
        InternationalValidationService internationalValidationService = applicationContext.getBean(InternationalValidationService.class);
        assertNotNull(internationalValidationService, "InternationalValidationService should be injectable");
        
        logger.info("Service dependency injection test passed");
    }
    
    @Test
    public void testBasicServiceFunctionality() {
        logger.info("Testing basic service functionality...");
        
        try {
            // Test that services can perform basic operations
            PetService petService = applicationContext.getBean(PetService.class);
            long petCount = petService.count();
            assertTrue(petCount >= 0, "Pet count should be non-negative");
            
            VisitService visitService = applicationContext.getBean(VisitService.class);
            long visitCount = visitService.count();
            assertTrue(visitCount >= 0, "Visit count should be non-negative");
            
            VeterinarianService veterinarianService = applicationContext.getBean(VeterinarianService.class);
            long vetCount = veterinarianService.count();
            assertTrue(vetCount >= 0, "Veterinarian count should be non-negative");
            
            logger.info("Basic service functionality test passed");
            
        } catch (Exception e) {
            logger.error("Basic service functionality test failed: {}", e.getMessage(), e);
            fail("Basic service functionality test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testApplicationStartupIntegration() {
        logger.info("Testing application startup integration...");
        
        // Test that the application context is fully loaded
        assertNotNull(applicationContext, "Application context should be loaded");
        
        // Test that all configuration classes are loaded
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        assertTrue(beanNames.length > 0, "Application context should contain beans");
        
        // Test that critical configuration beans exist
        assertTrue(applicationContext.containsBean("integrationConfig"), "IntegrationConfig should be loaded");
        assertTrue(applicationContext.containsBean("serviceIntegrationConfig"), "ServiceIntegrationConfig should be loaded");
        assertTrue(applicationContext.containsBean("errorHandlingIntegrationConfig"), "ErrorHandlingIntegrationConfig should be loaded");
        
        logger.info("Application startup integration test passed");
    }
}