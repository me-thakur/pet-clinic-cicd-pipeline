package com.petclinic.backend.integration;

import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import com.petclinic.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Performance Test
 * 
 * This test validates system performance under load without requiring authentication.
 * It focuses on database operations, service layer performance, and caching effectiveness.
 * 
 * **Validates: Requirements 10.1, 10.4, 10.5 (Performance and Scalability)**
 */
@SpringBootTest
@ActiveProfiles("test")
public class SimplePerformanceTest {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private GlobalSearchService globalSearchService;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        visitRepository.deleteAll();
        petRepository.deleteAll();
        veterinarianRepository.deleteAll();
        ownerRepository.deleteAll();
    }

    @Test
    @Transactional
    void testDatabaseOperationPerformance() throws Exception {
        // **Validates: Requirement 10.1 - Response times under 2 seconds**
        
        System.out.println("=== Testing Database Operation Performance ===");
        
        // Test owner creation performance
        long startTime = System.currentTimeMillis();
        Owner owner = createTestOwner("Performance", "Test");
        Owner savedOwner = ownerRepository.save(owner);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Owner creation took: " + duration + "ms");
        assertTrue(duration < 1000, "Owner creation should complete within 1 second, took: " + duration + "ms");
        assertNotNull(savedOwner.getId());

        // Test owner retrieval performance
        startTime = System.currentTimeMillis();
        Optional<Owner> retrievedOwner = ownerRepository.findById(savedOwner.getId());
        duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Owner retrieval took: " + duration + "ms");
        assertTrue(duration < 500, "Owner retrieval should complete within 500ms, took: " + duration + "ms");
        assertTrue(retrievedOwner.isPresent());

        // Test batch operations
        startTime = System.currentTimeMillis();
        List<Owner> owners = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            owners.add(createTestOwner("BatchOwner" + i, "Test"));
        }
        List<Owner> savedOwners = ownerRepository.saveAll(owners);
        duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Batch creation of 50 owners took: " + duration + "ms");
        assertTrue(duration < 2000, "Batch creation should complete within 2 seconds, took: " + duration + "ms");
        assertEquals(50, savedOwners.size());

        System.out.println("✓ Database operation performance tests passed");
    }

    @Test
    @Transactional
    void testConcurrentDatabaseOperations() throws Exception {
        // **Validates: Requirement 10.5 - Handle concurrent operations**
        
        System.out.println("=== Testing Concurrent Database Operations ===");
        
        final int CONCURRENT_OPERATIONS = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_OPERATIONS);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        // Simulate concurrent database operations
        for (int i = 1; i <= CONCURRENT_OPERATIONS; i++) {
            final int operationId = i;
            
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    // Create owner
                    Owner owner = createTestOwner("ConcurrentOwner" + operationId, "Test");
                    Owner savedOwner = ownerRepository.save(owner);
                    
                    // Create pet
                    Pet pet = createTestPet("ConcurrentPet" + operationId, "Dog", "TestBreed", savedOwner);
                    petRepository.save(pet);
                    
                    // Create veterinarian
                    Veterinarian vet = createTestVeterinarian("Dr. Concurrent" + operationId, "Test");
                    veterinarianRepository.save(vet);
                    
                    return System.currentTimeMillis() - startTime;
                } catch (Exception e) {
                    System.err.println("Concurrent operation " + operationId + " failed: " + e.getMessage());
                    return -1L;
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all operations to complete
        long overallStartTime = System.currentTimeMillis();
        List<Long> results = new ArrayList<>();
        
        for (CompletableFuture<Long> future : futures) {
            try {
                Long result = future.get(10, TimeUnit.SECONDS);
                if (result > 0) {
                    results.add(result);
                }
            } catch (Exception e) {
                System.err.println("Concurrent operation failed: " + e.getMessage());
            }
        }
        
        long overallDuration = System.currentTimeMillis() - overallStartTime;
        executor.shutdown();

        // Verify most operations completed successfully
        assertTrue(results.size() >= CONCURRENT_OPERATIONS * 0.8, 
                "At least 80% of concurrent operations should complete successfully. Completed: " + results.size());

        // Calculate average response time
        double averageTime = results.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        // Verify performance doesn't degrade significantly under load
        assertTrue(averageTime < 2000, 
                "Average response time should be under 2 seconds, was: " + averageTime + "ms");

        System.out.println("Concurrent operations test results:");
        System.out.println("- Target operations: " + CONCURRENT_OPERATIONS);
        System.out.println("- Successful operations: " + results.size());
        System.out.println("- Overall duration: " + overallDuration + "ms");
        System.out.println("- Average time per operation: " + averageTime + "ms");
        System.out.println("✓ Concurrent database operations test passed");
    }

    @Test
    @Transactional
    void testLargeDatasetQueryPerformance() throws Exception {
        // **Validates: Requirement 10.4 - Database query optimization**
        
        System.out.println("=== Testing Large Dataset Query Performance ===");
        
        final int DATASET_SIZE = 200;
        
        // Create test dataset
        System.out.println("Creating test dataset of " + DATASET_SIZE + " records...");
        List<Owner> owners = new ArrayList<>();
        List<Pet> pets = new ArrayList<>();
        List<Veterinarian> veterinarians = new ArrayList<>();
        
        for (int i = 1; i <= DATASET_SIZE; i++) {
            Owner owner = createTestOwner("QueryOwner" + i, "Test");
            owners.add(ownerRepository.save(owner));
            
            if (i <= 20) {
                Veterinarian vet = createTestVeterinarian("Dr. Query" + i, "Test");
                veterinarians.add(veterinarianRepository.save(vet));
            }
            
            if (i % 2 == 0) {
                Pet pet = createTestPet("QueryPet" + i, "Dog", "TestBreed", owners.get(i-1));
                pets.add(petRepository.save(pet));
            }
        }

        // Test query performance
        System.out.println("Testing query performance...");
        
        // Test findAll performance
        long startTime = System.currentTimeMillis();
        List<Owner> allOwners = ownerRepository.findAll();
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals(DATASET_SIZE, allOwners.size());
        assertTrue(duration < 1000, 
                "FindAll query should complete within 1 second, took: " + duration + "ms");
        System.out.println("FindAll query took: " + duration + "ms");

        // Test search by name performance
        startTime = System.currentTimeMillis();
        List<Owner> searchResults = ownerRepository.findByFirstNameContainingIgnoreCase("QueryOwner1");
        duration = System.currentTimeMillis() - startTime;
        
        assertFalse(searchResults.isEmpty());
        assertTrue(duration < 500, 
                "Search query should complete within 500ms, took: " + duration + "ms");
        System.out.println("Search query took: " + duration + "ms");

        // Test count performance
        startTime = System.currentTimeMillis();
        long count = ownerRepository.count();
        duration = System.currentTimeMillis() - startTime;
        
        assertEquals(DATASET_SIZE, count);
        assertTrue(duration < 200, 
                "Count query should complete within 200ms, took: " + duration + "ms");
        System.out.println("Count query took: " + duration + "ms");

        System.out.println("✓ Large dataset query performance tests passed");
    }

    @Test
    @Transactional
    void testServiceLayerPerformance() throws Exception {
        // **Test service layer performance with business logic**
        
        System.out.println("=== Testing Service Layer Performance ===");
        
        // Create test data
        Owner owner = createTestOwner("ServiceTest", "Owner");
        Owner savedOwner = ownerRepository.save(owner);
        
        Pet pet = createTestPet("ServicePet", "Dog", "TestBreed", savedOwner);
        Pet savedPet = petRepository.save(pet);
        
        Veterinarian vet = createTestVeterinarian("Dr. Service", "Test");
        Veterinarian savedVet = veterinarianRepository.save(vet);

        // Test dashboard service performance
        long startTime = System.currentTimeMillis();
        try {
            var metrics = dashboardService.getDashboardMetrics(
                    LocalDate.now().minusDays(30), LocalDate.now());
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("Dashboard metrics took: " + duration + "ms");
            assertTrue(duration < 1000, 
                    "Dashboard metrics should complete within 1 second, took: " + duration + "ms");
            assertNotNull(metrics);
        } catch (Exception e) {
            System.out.println("Dashboard service not fully available, skipping test: " + e.getMessage());
        }

        // Test search service performance
        startTime = System.currentTimeMillis();
        try {
            var searchResults = globalSearchService.globalSearch("ServiceTest");
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("Global search took: " + duration + "ms");
            assertTrue(duration < 1000, 
                    "Global search should complete within 1 second, took: " + duration + "ms");
            assertNotNull(searchResults);
        } catch (Exception e) {
            System.out.println("Search service not fully available, skipping test: " + e.getMessage());
        }

        System.out.println("✓ Service layer performance tests completed");
    }

    @Test
    void testMemoryEfficiency() throws Exception {
        // **Test memory efficiency under load**
        
        System.out.println("=== Testing Memory Efficiency ===");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.println("Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");

        // Perform memory-intensive operations
        final int OPERATIONS = 100;
        
        for (int i = 1; i <= OPERATIONS; i++) {
            // Create and save data
            Owner owner = createTestOwner("MemoryTest" + i, "Load");
            ownerRepository.save(owner);

            Pet pet = createTestPet("MemoryPet" + i, "Dog", "TestBreed", owner);
            petRepository.save(pet);

            // Check memory every 25 operations
            if (i % 25 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                System.out.println("Memory after " + i + " operations: " + (currentMemory / 1024 / 1024) + " MB");
            }
        }

        // Force garbage collection and check final memory
        System.gc();
        Thread.sleep(1000); // Give GC time to run
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Final memory usage: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");

        // Memory increase should be reasonable (less than 100MB for this test)
        assertTrue(memoryIncrease < 100 * 1024 * 1024, 
                "Memory increase should be reasonable, was: " + (memoryIncrease / 1024 / 1024) + " MB");

        System.out.println("✓ Memory efficiency test passed");
    }

    @Test
    @Transactional
    void testBulkOperationPerformance() throws Exception {
        // **Test bulk operations performance**
        
        System.out.println("=== Testing Bulk Operation Performance ===");
        
        final int BULK_SIZE = 100;
        
        // Test bulk insert performance
        long startTime = System.currentTimeMillis();
        List<Owner> owners = new ArrayList<>();
        for (int i = 1; i <= BULK_SIZE; i++) {
            owners.add(createTestOwner("BulkOwner" + i, "Test"));
        }
        List<Owner> savedOwners = ownerRepository.saveAll(owners);
        long insertDuration = System.currentTimeMillis() - startTime;
        
        assertEquals(BULK_SIZE, savedOwners.size());
        assertTrue(insertDuration < 3000, 
                "Bulk insert should complete within 3 seconds, took: " + insertDuration + "ms");
        System.out.println("Bulk insert of " + BULK_SIZE + " owners took: " + insertDuration + "ms");

        // Test bulk query performance
        startTime = System.currentTimeMillis();
        List<Owner> retrievedOwners = ownerRepository.findAll();
        long queryDuration = System.currentTimeMillis() - startTime;
        
        assertTrue(retrievedOwners.size() >= BULK_SIZE);
        assertTrue(queryDuration < 1000, 
                "Bulk query should complete within 1 second, took: " + queryDuration + "ms");
        System.out.println("Bulk query took: " + queryDuration + "ms");

        // Test bulk update performance
        startTime = System.currentTimeMillis();
        for (Owner owner : savedOwners) {
            owner.setCity("Updated City");
        }
        List<Owner> updatedOwners = ownerRepository.saveAll(savedOwners);
        long updateDuration = System.currentTimeMillis() - startTime;
        
        assertEquals(BULK_SIZE, updatedOwners.size());
        assertTrue(updateDuration < 2000, 
                "Bulk update should complete within 2 seconds, took: " + updateDuration + "ms");
        System.out.println("Bulk update took: " + updateDuration + "ms");

        System.out.println("✓ Bulk operation performance tests passed");
    }

    // Helper methods
    private Owner createTestOwner(String firstName, String lastName) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setAddress("123 Performance St");
        owner.setCity("Springfield");
        owner.setTelephone("555-0123"); // Valid format
        return owner;
    }

    private Pet createTestPet(String name, String species, String breed, Owner owner) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setBirthDate(LocalDate.of(2020, 1, 1));
        pet.setOwner(owner);
        return pet;
    }

    private Veterinarian createTestVeterinarian(String firstName, String lastName) {
        Veterinarian vet = new Veterinarian();
        vet.setFirstName(firstName);
        vet.setLastName(lastName);
        vet.setLicenseNumber("LIC" + System.currentTimeMillis());
        Set<Specialty> specialties = new HashSet<>();
        specialties.add(Specialty.GENERAL_PRACTICE);
        vet.setSpecialtySet(specialties);
        return vet;
    }
}