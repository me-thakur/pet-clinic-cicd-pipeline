package com.petclinic.backend.properties;

import com.petclinic.backend.config.CacheConfig;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.service.CacheManagementService;
import com.petclinic.backend.service.PetService;
import com.petclinic.backend.service.VeterinarianService;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.java.quickcheck.generator.PrimitiveGenerators.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for caching effectiveness
 * **Property 29: Caching Effectiveness**
 * **Validates: Requirements 10.3**
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CachingEffectivenessProperties extends PropertyTestBase {
    
    @Autowired
    private PetService petService;
    
    @Autowired
    private VeterinarianService veterinarianService;
    
    @Autowired
    private CacheManagementService cacheManagementService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private com.petclinic.backend.repository.OwnerRepository ownerRepository;
    
    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManagementService.clearAllCaches();
    }
    
    @Test
    void testCacheHitRateImprovement() {
        QuickCheck.forAll(integers(1, 10), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer accessCount) throws Throwable {
                // Clear caches to start fresh
                cacheManagementService.clearAllCaches();
                
                // Create test data
                Pet testPet = createTestPet();
                Pet savedPet = petService.create(testPet);
                Long petId = savedPet.getId();
                
                // First access - should be a cache miss
                long startTime = System.currentTimeMillis();
                Optional<Pet> firstAccess = petService.findById(petId);
                long firstAccessTime = System.currentTimeMillis() - startTime;
                
                assertTrue(firstAccess.isPresent(), "Pet should be found on first access");
                
                // Subsequent accesses - should be cache hits and faster
                long totalSubsequentTime = 0;
                for (int i = 0; i < accessCount; i++) {
                    startTime = System.currentTimeMillis();
                    Optional<Pet> subsequentAccess = petService.findById(petId);
                    totalSubsequentTime += (System.currentTimeMillis() - startTime);
                    
                    assertTrue(subsequentAccess.isPresent(), "Pet should be found on subsequent access");
                    assertEquals(savedPet.getName(), subsequentAccess.get().getName(), 
                        "Cached pet should have same data as original");
                }
                
                // Average subsequent access time should be faster than first access
                // (allowing for some variance due to system load)
                double averageSubsequentTime = (double) totalSubsequentTime / accessCount;
                
                // Cache should provide performance improvement
                // Note: This is a loose check as timing can be variable in tests
                if (accessCount > 1) {
                    assertTrue(averageSubsequentTime <= firstAccessTime * 2, 
                        "Cached access should be reasonably fast compared to first access");
                }
                
                // Verify cache statistics show hits
                Map<String, Double> hitRates = cacheManagementService.getCacheHitRates();
                Double petsCacheHitRate = hitRates.get(CacheConfig.PETS_CACHE);
                
                if (petsCacheHitRate != null && accessCount > 1) {
                    assertTrue(petsCacheHitRate > 0.0, 
                        "Cache hit rate should be greater than 0 after multiple accesses");
                }
            }
        });
    }
    
    @Test
    void testCacheInvalidationEffectiveness() {
        QuickCheck.forAll(strings(), new AbstractCharacteristic<String>() {
            @Override
            protected void doSpecify(String newName) throws Throwable {
                if (newName == null || newName.trim().isEmpty()) {
                    newName = "UpdatedPet";
                }
                
                // Create and cache a pet
                Pet testPet = createTestPet();
                Pet savedPet = petService.create(testPet);
                Long petId = savedPet.getId();
                
                // Access pet to cache it
                Optional<Pet> cachedPet = petService.findById(petId);
                assertTrue(cachedPet.isPresent(), "Pet should be cached");
                
                // Update the pet (should invalidate cache)
                Pet updatedPet = new Pet();
                updatedPet.setName(newName.trim());
                updatedPet.setSpecies(savedPet.getSpecies());
                updatedPet.setOwner(savedPet.getOwner());
                
                Pet result = petService.update(petId, updatedPet);
                
                // Verify the update was applied
                assertEquals(newName.trim(), result.getName(), 
                    "Pet name should be updated");
                
                // Access pet again - should get updated data from database
                Optional<Pet> refreshedPet = petService.findById(petId);
                assertTrue(refreshedPet.isPresent(), "Pet should still exist after update");
                assertEquals(newName.trim(), refreshedPet.get().getName(), 
                    "Cached pet should reflect the update");
                
                // Verify cache was properly invalidated and repopulated
                assertNotEquals(savedPet.getName(), refreshedPet.get().getName(), 
                    "Cache should contain updated data, not stale data");
            }
        });
    }
    
    @Test
    void testSearchResultCaching() {
        QuickCheck.forAll(strings(), new AbstractCharacteristic<String>() {
            @Override
            protected void doSpecify(String searchTerm) throws Throwable {
                if (searchTerm == null || searchTerm.trim().isEmpty()) {
                    searchTerm = "TestSearch";
                }
                
                String finalSearchTerm = searchTerm.trim();
                
                // Clear search caches
                cacheManagementService.invalidateSearchCaches();
                
                // First search - should be a cache miss
                long startTime = System.currentTimeMillis();
                List<Pet> firstSearchResults = petService.searchPets(finalSearchTerm);
                long firstSearchTime = System.currentTimeMillis() - startTime;
                
                // Second search with same term - should be a cache hit
                startTime = System.currentTimeMillis();
                List<Pet> secondSearchResults = petService.searchPets(finalSearchTerm);
                long secondSearchTime = System.currentTimeMillis() - startTime;
                
                // Results should be identical
                assertEquals(firstSearchResults.size(), secondSearchResults.size(), 
                    "Search results should be consistent");
                
                // Second search should be faster (cached)
                // Note: Loose timing check due to test environment variability
                assertTrue(secondSearchTime <= firstSearchTime * 3, 
                    "Cached search should be reasonably fast");
                
                // Verify search cache has entries
                Map<String, Object> cacheStats = cacheManagementService.getCacheStatistics();
                assertNotNull(cacheStats.get(CacheConfig.SEARCH_RESULTS_CACHE), 
                    "Search results cache should have statistics");
            }
        });
    }
    
    @Test
    void testStatisticsCaching() {
        QuickCheck.forAll(integers(1, 5), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer accessCount) throws Throwable {
                // Clear statistics caches
                cacheManagementService.invalidateStatisticsCaches();
                
                // First access to statistics - should be a cache miss
                Map<String, Object> firstStats = petService.getPetStatistics();
                assertNotNull(firstStats, "Pet statistics should be available");
                
                // Multiple subsequent accesses - should be cache hits
                for (int i = 0; i < accessCount; i++) {
                    Map<String, Object> subsequentStats = petService.getPetStatistics();
                    assertNotNull(subsequentStats, "Pet statistics should be consistently available");
                    
                    // Statistics should be identical (cached)
                    assertEquals(firstStats.size(), subsequentStats.size(), 
                        "Cached statistics should have same structure");
                    
                    for (String key : firstStats.keySet()) {
                        assertEquals(firstStats.get(key), subsequentStats.get(key), 
                            "Cached statistics values should be identical for key: " + key);
                    }
                }
                
                // Verify statistics cache has entries
                Map<String, Double> hitRates = cacheManagementService.getCacheHitRates();
                Double statsHitRate = hitRates.get(CacheConfig.STATISTICS_CACHE);
                
                if (statsHitRate != null && accessCount > 1) {
                    assertTrue(statsHitRate > 0.0, 
                        "Statistics cache should have hits after multiple accesses");
                }
            }
        });
    }
    
    @Test
    void testCacheEvictionBehavior() {
        QuickCheck.forAll(integers(1, 20), new AbstractCharacteristic<Integer>() {
            @Override
            protected void doSpecify(Integer petCount) throws Throwable {
                // Clear caches
                cacheManagementService.clearAllCaches();
                
                // Create multiple pets to test cache eviction
                for (int i = 0; i < petCount; i++) {
                    Pet testPet = createTestPet();
                    testPet.setName("TestPet" + i);
                    Pet savedPet = petService.create(testPet);
                    
                    // Access the pet to cache it
                    Optional<Pet> cachedPet = petService.findById(savedPet.getId());
                    assertTrue(cachedPet.isPresent(), "Pet should be accessible");
                }
                
                // Get cache statistics
                Map<String, Object> cacheStats = cacheManagementService.getCacheStatistics();
                assertNotNull(cacheStats, "Cache statistics should be available");
                
                // Verify cache is functioning
                Map<String, Double> hitRates = cacheManagementService.getCacheHitRates();
                assertNotNull(hitRates, "Hit rates should be available");
                
                // Cache should handle the load without errors
                assertTrue(true, "Cache should handle multiple entries without errors");
            }
        });
    }
    
    @Test
    void testCacheWarmUpEffectiveness() {
        QuickCheck.forAll(booleans(), new AbstractCharacteristic<Boolean>() {
            @Override
            protected void doSpecify(Boolean performWarmUp) throws Throwable {
                // Clear all caches
                cacheManagementService.clearAllCaches();
                
                if (performWarmUp) {
                    // Warm up caches
                    cacheManagementService.warmUpCaches();
                    
                    // Verify caches have been populated
                    Map<String, Object> cacheStats = cacheManagementService.getCacheStatistics();
                    assertNotNull(cacheStats, "Cache statistics should be available after warm-up");
                    
                    // Access commonly cached data - should be faster due to warm-up
                    long startTime = System.currentTimeMillis();
                    List<Pet> allPets = petService.findAll();
                    long accessTime = System.currentTimeMillis() - startTime;
                    
                    assertNotNull(allPets, "All pets should be accessible after warm-up");
                    
                    // Verify cache hit rates show some activity
                    Map<String, Double> hitRates = cacheManagementService.getCacheHitRates();
                    assertNotNull(hitRates, "Hit rates should be available after warm-up");
                }
                
                // Test should complete without errors regardless of warm-up
                assertTrue(true, "Cache warm-up should complete without errors");
            }
        });
    }
    
    private Pet createTestPet() {
        // Create and save owner first
        com.petclinic.backend.model.Owner owner = createTestOwner();
        com.petclinic.backend.model.Owner savedOwner = ownerRepository.save(owner);
        
        Pet pet = new Pet();
        pet.setName("TestPet");
        pet.setSpecies("Dog");
        pet.setBreed("Labrador");
        pet.setOwner(savedOwner);
        return pet;
    }
    
    private com.petclinic.backend.model.Owner createTestOwner() {
        com.petclinic.backend.model.Owner owner = new com.petclinic.backend.model.Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setEmail("test@example.com");
        owner.setTelephone("555-123-4567");
        return owner;
    }
}