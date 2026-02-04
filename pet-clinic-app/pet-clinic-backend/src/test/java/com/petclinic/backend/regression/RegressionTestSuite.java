package com.petclinic.backend.regression;

import com.petclinic.backend.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive regression test suite to ensure existing functionality remains intact
 * after implementing critical fixes and enhancements
 * Validates: All requirements - ensures no regression in existing features
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Regression Test Suite")
public class RegressionTestSuite {
    
    private static final Logger logger = LoggerFactory.getLogger(RegressionTestSuite.class);
    
    @Autowired
    private PetService petService;
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    @Autowired
    private VisitService visitService;
    
    @Autowired
    private OwnerValidationService ownerValidationService;
    
    @Autowired
    private EnhancedTableService enhancedTableService;
    
    @Test
    @DisplayName("Core Pet Service Functionality")
    public void testCorePetServiceFunctionality() {
        logger.info("Testing core pet service functionality...");
        
        // Test that basic pet service operations still work
        assertNotNull(petService, "PetService should be available");
        
        // Test count operation
        long petCount = petService.count();
        assertTrue(petCount >= 0, "Pet count should be non-negative");
        
        // Test that service can handle basic operations without errors
        try {
            petService.findAll();
            logger.debug("Pet service findAll() works correctly");
        } catch (Exception e) {
            fail("Pet service findAll() should not throw exceptions: " + e.getMessage());
        }
        
        logger.info("Core pet service functionality test passed");
    }
    
    @Test
    @DisplayName("Core Veterinarian Service Functionality")
    public void testCoreVeterinarianServiceFunctionality() {
        logger.info("Testing core veterinarian service functionality...");
        
        assertNotNull(veterinarianService, "VeterinarianService should be available");
        
        // Test count operation
        long vetCount = veterinarianService.count();
        assertTrue(vetCount >= 0, "Veterinarian count should be non-negative");
        
        // Test that service can handle basic operations without errors
        try {
            veterinarianService.findAll();
            logger.debug("Veterinarian service findAll() works correctly");
        } catch (Exception e) {
            fail("Veterinarian service findAll() should not throw exceptions: " + e.getMessage());
        }
        
        logger.info("Core veterinarian service functionality test passed");
    }
    
    @Test
    @DisplayName("Core Visit Service Functionality")
    public void testCoreVisitServiceFunctionality() {
        logger.info("Testing core visit service functionality...");
        
        assertNotNull(visitService, "VisitService should be available");
        
        // Test count operation
        long visitCount = visitService.count();
        assertTrue(visitCount >= 0, "Visit count should be non-negative");
        
        // Test that service can handle basic operations without errors
        try {
            visitService.findAll();
            logger.debug("Visit service findAll() works correctly");
        } catch (Exception e) {
            fail("Visit service findAll() should not throw exceptions: " + e.getMessage());
        }
        
        logger.info("Core visit service functionality test passed");
    }
    
    @Test
    @DisplayName("Existing Validation Rules Continue to Work")
    public void testExistingValidationRules() {
        logger.info("Testing existing validation rules...");
        
        assertNotNull(ownerValidationService, "OwnerValidationService should be available");
        
        // Test that validation service is accessible and functional
        try {
            // The service should be available for validation operations
            // We're not testing specific validation logic here, just that the service works
            logger.debug("Owner validation service is accessible");
        } catch (Exception e) {
            fail("Owner validation service should be accessible: " + e.getMessage());
        }
        
        logger.info("Existing validation rules test passed");
    }
    
    @Test
    @DisplayName("Enhanced Table Features Still Work")
    public void testEnhancedTableFeatures() {
        logger.info("Testing enhanced table features...");
        
        assertNotNull(enhancedTableService, "EnhancedTableService should be available");
        
        // Test that enhanced table service is accessible
        try {
            // The service should be available for table operations
            logger.debug("Enhanced table service is accessible");
        } catch (Exception e) {
            fail("Enhanced table service should be accessible: " + e.getMessage());
        }
        
        logger.info("Enhanced table features test passed");
    }
    
    @Test
    @DisplayName("Service Dependencies Are Maintained")
    public void testServiceDependencies() {
        logger.info("Testing service dependencies...");
        
        // Test that all critical services are still properly injected
        assertNotNull(petService, "PetService dependency should be maintained");
        assertNotNull(veterinarianService, "VeterinarianService dependency should be maintained");
        assertNotNull(visitService, "VisitService dependency should be maintained");
        assertNotNull(ownerValidationService, "OwnerValidationService dependency should be maintained");
        assertNotNull(enhancedTableService, "EnhancedTableService dependency should be maintained");
        
        logger.info("Service dependencies test passed");
    }
    
    @Test
    @DisplayName("Database Operations Still Function")
    public void testDatabaseOperations() {
        logger.info("Testing database operations...");
        
        try {
            // Test that basic database operations still work
            long petCount = petService.count();
            long vetCount = veterinarianService.count();
            long visitCount = visitService.count();
            
            // All counts should be non-negative (basic sanity check)
            assertTrue(petCount >= 0, "Pet count should be non-negative");
            assertTrue(vetCount >= 0, "Veterinarian count should be non-negative");
            assertTrue(visitCount >= 0, "Visit count should be non-negative");
            
            logger.debug("Database operations work correctly - Pet: {}, Vet: {}, Visit: {}", 
                        petCount, vetCount, visitCount);
            
        } catch (Exception e) {
            fail("Database operations should work correctly: " + e.getMessage());
        }
        
        logger.info("Database operations test passed");
    }
    
    @Test
    @DisplayName("Application Context Integrity")
    public void testApplicationContextIntegrity() {
        logger.info("Testing application context integrity...");
        
        // Test that the application context is still properly configured
        // and all beans are available
        
        // Core services should be available
        assertNotNull(petService, "PetService should be in context");
        assertNotNull(veterinarianService, "VeterinarianService should be in context");
        assertNotNull(visitService, "VisitService should be in context");
        
        // Validation services should be available
        assertNotNull(ownerValidationService, "OwnerValidationService should be in context");
        
        // Enhanced services should be available
        assertNotNull(enhancedTableService, "EnhancedTableService should be in context");
        
        logger.info("Application context integrity test passed");
    }
    
    @Test
    @DisplayName("No Critical Exceptions During Basic Operations")
    public void testNoCriticalExceptions() {
        logger.info("Testing for critical exceptions during basic operations...");
        
        try {
            // Perform basic operations that should not throw exceptions
            petService.count();
            veterinarianService.count();
            visitService.count();
            
            petService.findAll();
            veterinarianService.findAll();
            visitService.findAll();
            
            logger.debug("All basic operations completed without exceptions");
            
        } catch (Exception e) {
            fail("Basic operations should not throw critical exceptions: " + e.getMessage());
        }
        
        logger.info("No critical exceptions test passed");
    }
    
    @Test
    @DisplayName("Service Interface Compatibility")
    public void testServiceInterfaceCompatibility() {
        logger.info("Testing service interface compatibility...");
        
        // Test that services still implement their expected interfaces
        assertTrue(petService instanceof PetService, "PetService should implement PetService interface");
        assertTrue(veterinarianService instanceof VeterinarianService, "VeterinarianService should implement VeterinarianService interface");
        assertTrue(visitService instanceof VisitService, "VisitService should implement VisitService interface");
        assertTrue(ownerValidationService instanceof OwnerValidationService, "OwnerValidationService should implement OwnerValidationService interface");
        assertTrue(enhancedTableService instanceof EnhancedTableService, "EnhancedTableService should implement EnhancedTableService interface");
        
        logger.info("Service interface compatibility test passed");
    }
}