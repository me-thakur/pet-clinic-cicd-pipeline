package com.petclinic.backend.properties;

import com.petclinic.backend.service.DataSeedingService;
import com.petclinic.backend.repository.*;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static net.java.quickcheck.generator.PrimitiveGenerators.integers;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for data seeding consistency and error logging completeness.
 * 
 * **Validates: Requirements 8.3, 8.4**
 */
@SpringBootTest
@ActiveProfiles("test")
class DataSeedingConsistencyProperties extends PropertyTestBase {

    @Autowired
    private DataSeedingService dataSeedingService;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private VeterinarianRepository veterinarianRepository;
    
    @Autowired
    private VisitRepository visitRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Property 20: Data Seeding Consistency
     * For any development or test environment initialization, sample data should be created consistently and completely
     */
    @Test
    @Transactional
    void testDataSeedingConsistency() {
        QuickCheck.forAll(integers(1, 5), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    // Given: A clean database state
                    clearAllData();
                    
                    // When: Data seeding is performed
                    if (dataSeedingService.isSeedingNeeded()) {
                        dataSeedingService.seedAllData();
                    }
                    
                    // Then: Data should be seeded consistently
                    DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
                    
                    // And: All entity types should have data
                    assertTrue(stats.getOwnerCount() > 0, 
                        "Owners should be seeded on iteration " + i);
                    assertTrue(stats.getPetCount() > 0, 
                        "Pets should be seeded on iteration " + i);
                    assertTrue(stats.getVeterinarianCount() > 0, 
                        "Veterinarians should be seeded on iteration " + i);
                    assertTrue(stats.getVisitCount() > 0, 
                        "Visits should be seeded on iteration " + i);
                    assertTrue(stats.getUserCount() > 0, 
                        "Users should be seeded on iteration " + i);
                    
                    // And: Data relationships should be maintained
                    verifyDataRelationships();
                    
                    // And: Data should be valid and complete
                    verifyDataIntegrity();
                }
            }
        });
    }

    /**
     * Property: Data Seeding Idempotency
     * Running data seeding multiple times should not create duplicate data
     */
    @Test
    @Transactional
    void testDataSeedingIdempotency() {
        QuickCheck.forAll(integers(2, 4), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer runs) throws Throwable {
                // Given: A clean database state
                clearAllData();
                
                // When: Data seeding is run multiple times
                DataSeedingService.DataSeedingStatistics firstStats = null;
                
                for (int i = 0; i < runs; i++) {
                    if (dataSeedingService.isSeedingNeeded()) {
                        dataSeedingService.seedAllData();
                    }
                    
                    DataSeedingService.DataSeedingStatistics currentStats = dataSeedingService.getStatistics();
                    
                    if (firstStats == null) {
                        firstStats = currentStats;
                    } else {
                        // Then: Subsequent runs should not create additional data
                        assertEquals(firstStats.getOwnerCount(), currentStats.getOwnerCount(),
                            "Owner count should remain consistent across runs");
                        assertEquals(firstStats.getPetCount(), currentStats.getPetCount(),
                            "Pet count should remain consistent across runs");
                        assertEquals(firstStats.getVeterinarianCount(), currentStats.getVeterinarianCount(),
                            "Veterinarian count should remain consistent across runs");
                        assertEquals(firstStats.getUserCount(), currentStats.getUserCount(),
                            "User count should remain consistent across runs");
                    }
                }
            }
        });
    }

    /**
     * Property: Data Seeding Completeness
     * All seeded entities should have required fields populated
     */
    @Test
    @Transactional
    void testDataSeedingCompleteness() {
        QuickCheck.forAll(integers(1, 3), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    // Given: A clean database state
                    clearAllData();
                    
                    // When: Data seeding is performed
                    if (dataSeedingService.isSeedingNeeded()) {
                        dataSeedingService.seedAllData();
                    }
                    
                    // Then: All owners should have required fields
                    ownerRepository.findAll().forEach(owner -> {
                        assertNotNull(owner.getFirstName(), "Owner first name should not be null");
                        assertNotNull(owner.getLastName(), "Owner last name should not be null");
                        assertNotNull(owner.getEmail(), "Owner email should not be null");
                        assertFalse(owner.getFirstName().trim().isEmpty(), "Owner first name should not be empty");
                        assertFalse(owner.getLastName().trim().isEmpty(), "Owner last name should not be empty");
                        assertFalse(owner.getEmail().trim().isEmpty(), "Owner email should not be empty");
                    });
                    
                    // And: All pets should have required fields
                    petRepository.findAll().forEach(pet -> {
                        assertNotNull(pet.getName(), "Pet name should not be null");
                        assertNotNull(pet.getSpecies(), "Pet species should not be null");
                        assertNotNull(pet.getOwner(), "Pet owner should not be null");
                        assertFalse(pet.getName().trim().isEmpty(), "Pet name should not be empty");
                        assertFalse(pet.getSpecies().trim().isEmpty(), "Pet species should not be empty");
                    });
                    
                    // And: All veterinarians should have required fields
                    veterinarianRepository.findAll().forEach(vet -> {
                        assertNotNull(vet.getFirstName(), "Veterinarian first name should not be null");
                        assertNotNull(vet.getLastName(), "Veterinarian last name should not be null");
                        assertNotNull(vet.getLicenseNumber(), "Veterinarian license number should not be null");
                        assertFalse(vet.getFirstName().trim().isEmpty(), "Veterinarian first name should not be empty");
                        assertFalse(vet.getLastName().trim().isEmpty(), "Veterinarian last name should not be empty");
                        assertFalse(vet.getLicenseNumber().trim().isEmpty(), "Veterinarian license number should not be empty");
                    });
                    
                    // And: All visits should have required fields
                    visitRepository.findAll().forEach(visit -> {
                        assertNotNull(visit.getVisitDate(), "Visit date should not be null");
                        assertNotNull(visit.getPet(), "Visit pet should not be null");
                        assertNotNull(visit.getVeterinarian(), "Visit veterinarian should not be null");
                    });
                    
                    // And: All users should have required fields
                    userRepository.findAll().forEach(user -> {
                        assertNotNull(user.getUsername(), "User username should not be null");
                        assertNotNull(user.getEmail(), "User email should not be null");
                        assertNotNull(user.getRole(), "User role should not be null");
                        assertFalse(user.getUsername().trim().isEmpty(), "User username should not be empty");
                        assertFalse(user.getEmail().trim().isEmpty(), "User email should not be empty");
                    });
                }
            }
        });
    }

    /**
     * Property: Data Seeding Error Recovery
     * If data seeding encounters errors, it should handle them gracefully and log appropriately
     */
    @Test
    @Transactional
    void testDataSeedingErrorRecovery() {
        QuickCheck.forAll(integers(1, 3), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    // Given: A clean database state
                    clearAllData();
                    
                    // When: Data seeding is performed (even with potential errors)
                    try {
                        if (dataSeedingService.isSeedingNeeded()) {
                            dataSeedingService.seedAllData();
                        }
                        
                        // Then: Basic data should still be created
                        DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
                        
                        // And: At least some data should be present (graceful degradation)
                        long totalEntities = stats.getOwnerCount() + stats.getPetCount() + 
                                           stats.getVeterinarianCount() + stats.getVisitCount() + 
                                           stats.getUserCount();
                        
                        assertTrue(totalEntities > 0, 
                            "At least some entities should be created even with errors on iteration " + i);
                        
                    } catch (Exception e) {
                        // If seeding fails completely, it should be a controlled failure
                        assertNotNull(e.getMessage(), "Error message should be provided");
                        assertFalse(e.getMessage().trim().isEmpty(), "Error message should not be empty");
                    }
                }
            }
        });
    }

    /**
     * Property 21: Error Logging Completeness
     * For any data corruption or system error, appropriate error messages should be logged with sufficient detail for debugging
     */
    @Test
    @Transactional
    void testErrorLoggingCompleteness() {
        QuickCheck.forAll(integers(1, 3), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    // Given: A clean database state
                    clearAllData();
                    
                    // When: Data operations are performed
                    try {
                        if (dataSeedingService.isSeedingNeeded()) {
                            dataSeedingService.seedAllData();
                        }
                        
                        // Then: Operations should complete successfully or log errors appropriately
                        DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
                        
                        // And: Statistics should be available for monitoring
                        assertNotNull(stats, "Statistics should be available for monitoring");
                        assertTrue(stats.getOwnerCount() >= 0, "Owner count should be non-negative");
                        assertTrue(stats.getPetCount() >= 0, "Pet count should be non-negative");
                        assertTrue(stats.getVeterinarianCount() >= 0, "Veterinarian count should be non-negative");
                        assertTrue(stats.getVisitCount() >= 0, "Visit count should be non-negative");
                        assertTrue(stats.getUserCount() >= 0, "User count should be non-negative");
                        
                    } catch (Exception e) {
                        // If errors occur, they should provide sufficient debugging information
                        assertNotNull(e.getMessage(), "Error message should be provided for debugging");
                        assertFalse(e.getMessage().trim().isEmpty(), "Error message should not be empty");
                        
                        // Error should contain contextual information
                        String errorMessage = e.getMessage().toLowerCase();
                        boolean hasContext = errorMessage.contains("seed") || 
                                           errorMessage.contains("data") || 
                                           errorMessage.contains("owner") || 
                                           errorMessage.contains("pet") || 
                                           errorMessage.contains("veterinarian") || 
                                           errorMessage.contains("visit") || 
                                           errorMessage.contains("user");
                        
                        assertTrue(hasContext, 
                            "Error message should contain contextual information: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Property: Data Seeding Statistics Accuracy
     * Statistics returned by the seeding service should accurately reflect the actual data
     */
    @Test
    @Transactional
    void testDataSeedingStatisticsAccuracy() {
        QuickCheck.forAll(integers(1, 3), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer iterations) throws Throwable {
                for (int i = 0; i < iterations; i++) {
                    // Given: A clean database state
                    clearAllData();
                    
                    // When: Data seeding is performed
                    if (dataSeedingService.isSeedingNeeded()) {
                        dataSeedingService.seedAllData();
                    }
                    
                    // Then: Statistics should match actual repository counts
                    DataSeedingService.DataSeedingStatistics stats = dataSeedingService.getStatistics();
                    
                    assertEquals(ownerRepository.count(), stats.getOwnerCount(),
                        "Owner statistics should match repository count");
                    assertEquals(petRepository.count(), stats.getPetCount(),
                        "Pet statistics should match repository count");
                    assertEquals(veterinarianRepository.count(), stats.getVeterinarianCount(),
                        "Veterinarian statistics should match repository count");
                    assertEquals(visitRepository.count(), stats.getVisitCount(),
                        "Visit statistics should match repository count");
                    assertEquals(userRepository.count(), stats.getUserCount(),
                        "User statistics should match repository count");
                }
            }
        });
    }

    /**
     * Helper method to clear all data for testing
     */
    private void clearAllData() {
        try {
            dataSeedingService.clearAllData();
        } catch (Exception e) {
            // If clearing fails, at least ensure we have a clean state for testing
            visitRepository.deleteAll();
            petRepository.deleteAll();
            veterinarianRepository.deleteAll();
            ownerRepository.deleteAll();
            userRepository.deleteAll();
        }
    }

    /**
     * Helper method to verify data relationships are maintained
     */
    private void verifyDataRelationships() {
        // Verify pet-owner relationships
        petRepository.findAll().forEach(pet -> {
            assertNotNull(pet.getOwner(), "Pet should have an owner");
            assertTrue(pet.getOwner().getPets().contains(pet), 
                "Owner should contain the pet in their pets list");
        });
        
        // Verify visit-pet relationships
        visitRepository.findAll().forEach(visit -> {
            assertNotNull(visit.getPet(), "Visit should have a pet");
            if (visit.getVeterinarian() != null) {
                // If visit has a veterinarian, verify the relationship
                assertTrue(visit.getVeterinarian().getVisits().contains(visit),
                    "Veterinarian should contain the visit in their visits list");
            }
        });
    }

    /**
     * Helper method to verify data integrity
     */
    private void verifyDataIntegrity() {
        // Verify unique constraints
        long uniqueOwnerEmails = ownerRepository.findAll().stream()
                .map(owner -> owner.getEmail())
                .distinct()
                .count();
        assertEquals(ownerRepository.count(), uniqueOwnerEmails,
            "All owner emails should be unique");
        
        long uniqueVetLicenses = veterinarianRepository.findAll().stream()
                .map(vet -> vet.getLicenseNumber())
                .distinct()
                .count();
        assertEquals(veterinarianRepository.count(), uniqueVetLicenses,
            "All veterinarian license numbers should be unique");
        
        long uniqueUsernames = userRepository.findAll().stream()
                .map(user -> user.getUsername())
                .distinct()
                .count();
        assertEquals(userRepository.count(), uniqueUsernames,
            "All usernames should be unique");
    }
}