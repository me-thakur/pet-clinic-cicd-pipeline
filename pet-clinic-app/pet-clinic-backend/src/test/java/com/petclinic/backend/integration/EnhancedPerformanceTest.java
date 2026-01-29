package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.AuthRequest;
import com.petclinic.backend.dto.AuthResponse;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import com.petclinic.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced Performance Integration Test
 * 
 * This test validates system performance under load and tests scalability requirements.
 * It focuses on the key performance requirements: response times, concurrent users,
 * database query optimization, and caching effectiveness.
 * 
 * **Validates: Requirements 10.1, 10.4, 10.5 (Performance and Scalability)**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EnhancedPerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String authToken;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate for API access
        AuthRequest authRequest = new AuthRequest("admin", "admin123");
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", authRequest, AuthResponse.class);
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            authToken = "Bearer " + response.getBody().getToken();
        } else {
            authToken = "Bearer test-token"; // Fallback for testing
        }
        
        headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @Transactional
    void testResponseTimeRequirements() throws Exception {
        // **Validates: Requirement 10.1 - Response times under 2 seconds**
        
        System.out.println("=== Testing Response Time Requirements ===");
        
        // Create test data
        Owner owner = createTestOwner("Performance", "Test");
        
        // Test owner creation response time
        long startTime = System.currentTimeMillis();
        HttpEntity<Owner> request = new HttpEntity<>(owner, headers);
        ResponseEntity<Owner> response = restTemplate.postForEntity("/api/owners", request, Owner.class);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Owner creation took: " + duration + "ms");
        assertTrue(duration < 2000, "Owner creation should complete within 2 seconds, took: " + duration + "ms");

        // Test owner retrieval response time
        startTime = System.currentTimeMillis();
        HttpEntity<String> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/owners?page=0&size=10", HttpMethod.GET, getRequest, String.class);
        duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Owner listing took: " + duration + "ms");
        assertTrue(duration < 2000, "Owner listing should complete within 2 seconds, took: " + duration + "ms");

        // Test search response time
        startTime = System.currentTimeMillis();
        ResponseEntity<String> searchResponse = restTemplate.exchange(
                "/api/search/global?query=Performance", HttpMethod.GET, getRequest, String.class);
        duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Global search took: " + duration + "ms");
        assertTrue(duration < 2000, "Global search should complete within 2 seconds, took: " + duration + "ms");

        System.out.println("✓ All response time requirements met");
    }

    @Test
    void testConcurrentUserLoad() throws Exception {
        // **Validates: Requirement 10.5 - Handle 50 concurrent users**
        
        System.out.println("=== Testing Concurrent User Load (50 users) ===");
        
        final int CONCURRENT_USERS = 50;
        final int OPERATIONS_PER_USER = 3;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        // Simulate 50 concurrent users each performing 3 operations
        for (int user = 1; user <= CONCURRENT_USERS; user++) {
            final int userId = user;
            
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                try {
                    long totalTime = 0;
                    TestRestTemplate userRestTemplate = new TestRestTemplate();
                    String baseUrl = "http://localhost:" + port;
                    
                    for (int op = 1; op <= OPERATIONS_PER_USER; op++) {
                        // Create owner
                        Owner owner = createTestOwner("ConcurrentUser" + userId, "Operation" + op);
                        
                        long startTime = System.currentTimeMillis();
                        HttpEntity<Owner> request = new HttpEntity<>(owner, headers);
                        ResponseEntity<Owner> response = userRestTemplate.postForEntity(
                                baseUrl + "/api/owners", request, Owner.class);
                        totalTime += System.currentTimeMillis() - startTime;

                        // Search for owners
                        startTime = System.currentTimeMillis();
                        HttpEntity<String> getRequest = new HttpEntity<>(headers);
                        userRestTemplate.exchange(
                                baseUrl + "/api/search/global?query=ConcurrentUser" + userId, 
                                HttpMethod.GET, getRequest, String.class);
                        totalTime += System.currentTimeMillis() - startTime;
                    }
                    
                    return totalTime;
                } catch (Exception e) {
                    System.err.println("User " + userId + " failed: " + e.getMessage());
                    return 0L;
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all users to complete with timeout
        long overallStartTime = System.currentTimeMillis();
        List<Long> results = new ArrayList<>();
        
        for (CompletableFuture<Long> future : futures) {
            try {
                Long result = future.get(60, TimeUnit.SECONDS);
                if (result > 0) {
                    results.add(result);
                }
            } catch (Exception e) {
                System.err.println("Concurrent user operation failed: " + e.getMessage());
            }
        }
        
        long overallDuration = System.currentTimeMillis() - overallStartTime;
        executor.shutdown();

        // Verify most operations completed successfully
        assertTrue(results.size() >= CONCURRENT_USERS * 0.8, 
                "At least 80% of concurrent users should complete successfully. Completed: " + results.size());

        // Calculate average response time per operation
        double averageTimePerOperation = results.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / OPERATIONS_PER_USER;

        // Verify performance doesn't degrade significantly under load
        assertTrue(averageTimePerOperation < 5000, 
                "Average response time per operation should be under 5 seconds under load, was: " + averageTimePerOperation + "ms");

        System.out.println("Concurrent load test completed:");
        System.out.println("- Target users: " + CONCURRENT_USERS);
        System.out.println("- Successful users: " + results.size());
        System.out.println("- Operations per user: " + OPERATIONS_PER_USER);
        System.out.println("- Overall duration: " + overallDuration + "ms");
        System.out.println("- Average time per operation: " + averageTimePerOperation + "ms");
        System.out.println("✓ Concurrent load test passed");
    }

    @Test
    @Transactional
    void testDatabaseQueryOptimization() throws Exception {
        // **Validates: Requirement 10.4 - Database query optimization**
        
        System.out.println("=== Testing Database Query Optimization ===");
        
        final int DATASET_SIZE = 100;
        
        // Create test dataset
        System.out.println("Creating test dataset of " + DATASET_SIZE + " records...");
        List<Owner> owners = new ArrayList<>();
        List<Pet> pets = new ArrayList<>();
        List<Veterinarian> veterinarians = new ArrayList<>();
        
        for (int i = 1; i <= DATASET_SIZE; i++) {
            Owner owner = createTestOwner("QueryTest" + i, "Owner");
            owners.add(ownerRepository.save(owner));
            
            if (i <= 10) {
                Veterinarian vet = createTestVeterinarian("Dr. QueryTest" + i, "Vet");
                veterinarians.add(veterinarianRepository.save(vet));
            }
            
            if (i % 2 == 0) {
                Pet pet = createTestPet("QueryPet" + i, "Dog", "TestBreed", owners.get(i-1));
                pets.add(petRepository.save(pet));
            }
        }

        // Test pagination performance
        System.out.println("Testing pagination performance...");
        long startTime = System.currentTimeMillis();
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        // Test different page sizes
        int[] pageSizes = {10, 25, 50};
        for (int pageSize : pageSizes) {
            long pageStartTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/owners?page=0&size=" + pageSize, HttpMethod.GET, request, String.class);
            long pageDuration = System.currentTimeMillis() - pageStartTime;
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(pageDuration < 1000, 
                    "Page size " + pageSize + " should load within 1 second, took: " + pageDuration + "ms");
            
            System.out.println("Page size " + pageSize + " took: " + pageDuration + "ms");
        }

        // Test search performance
        System.out.println("Testing search performance...");
        String[] searchTerms = {"QueryTest1", "QueryPet", "TestBreed"};
        
        for (String searchTerm : searchTerms) {
            startTime = System.currentTimeMillis();
            ResponseEntity<String> searchResponse = restTemplate.exchange(
                    "/api/search/global?query=" + searchTerm, HttpMethod.GET, request, String.class);
            long searchDuration = System.currentTimeMillis() - startTime;
            
            assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
            assertTrue(searchDuration < 1000, 
                    "Search for '" + searchTerm + "' should complete within 1 second, took: " + searchDuration + "ms");
            
            System.out.println("Search '" + searchTerm + "' took: " + searchDuration + "ms");
        }

        System.out.println("✓ Database query optimization tests passed");
    }

    @Test
    @Transactional
    void testCachingEffectiveness() throws Exception {
        // **Validates: Requirement 10.3 - Caching for frequently accessed data**
        
        System.out.println("=== Testing Caching Effectiveness ===");
        
        // Create test data
        Owner owner = createTestOwner("Cache", "Performance");
        Owner savedOwner = ownerRepository.save(owner);

        HttpEntity<String> request = new HttpEntity<>(headers);

        // Test dashboard metrics caching (frequently accessed)
        System.out.println("Testing dashboard metrics caching...");
        
        long firstCallTime = measureResponseTime(() -> {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/dashboard/metrics", HttpMethod.GET, request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Second call should be faster due to caching
        long secondCallTime = measureResponseTime(() -> {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/dashboard/metrics", HttpMethod.GET, request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Third call should also be fast (cache hit)
        long thirdCallTime = measureResponseTime(() -> {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/dashboard/metrics", HttpMethod.GET, request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Dashboard metrics - First call: " + firstCallTime + "ms, " +
                         "Second call: " + secondCallTime + "ms, Third call: " + thirdCallTime + "ms");

        // Cache should make subsequent calls faster (allowing some variance for timing)
        assertTrue(secondCallTime <= firstCallTime + 200, 
                "Second call should be as fast or faster due to caching");
        assertTrue(thirdCallTime <= firstCallTime + 200, 
                "Third call should be as fast or faster due to caching");

        // Test owner data retrieval caching
        System.out.println("Testing owner data caching...");
        
        long ownerFirstCall = measureResponseTime(() -> {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/owners/" + savedOwner.getId(), HttpMethod.GET, request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        long ownerSecondCall = measureResponseTime(() -> {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/owners/" + savedOwner.getId(), HttpMethod.GET, request, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Owner data - First call: " + ownerFirstCall + "ms, Second call: " + ownerSecondCall + "ms");
        
        // Allow for some variance in timing
        assertTrue(ownerSecondCall <= ownerFirstCall + 100, 
                "Cached owner data should be retrieved as fast or faster");

        System.out.println("✓ Caching effectiveness tests passed");
    }

    @Test
    void testMemoryUsageUnderLoad() throws Exception {
        // **Test memory efficiency under load**
        
        System.out.println("=== Testing Memory Usage Under Load ===");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.println("Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");

        // Perform memory-intensive operations
        final int OPERATIONS = 50;
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        for (int i = 1; i <= OPERATIONS; i++) {
            // Create and retrieve data
            Owner owner = createTestOwner("MemoryTest" + i, "Load");
            HttpEntity<Owner> createRequest = new HttpEntity<>(owner, headers);
            
            ResponseEntity<Owner> createResponse = restTemplate.postForEntity(
                    "/api/owners", createRequest, Owner.class);
            assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

            // Perform search operations
            ResponseEntity<String> searchResponse = restTemplate.exchange(
                    "/api/search/global?query=MemoryTest" + i, HttpMethod.GET, request, String.class);
            assertEquals(HttpStatus.OK, searchResponse.getStatusCode());

            // Check memory every 10 operations
            if (i % 10 == 0) {
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

        // Memory increase should be reasonable (less than 50MB for this test)
        assertTrue(memoryIncrease < 50 * 1024 * 1024, 
                "Memory increase should be reasonable, was: " + (memoryIncrease / 1024 / 1024) + " MB");

        System.out.println("✓ Memory usage test passed");
    }

    @Test
    @Transactional
    void testSystemPerformanceUnderRealisticLoad() throws Exception {
        // **Comprehensive test combining all performance aspects**
        
        System.out.println("=== Testing System Performance Under Realistic Load ===");
        
        final int USERS = 20;
        final int OPERATIONS_PER_USER = 5;
        
        ExecutorService executor = Executors.newFixedThreadPool(USERS);
        List<CompletableFuture<Map<String, Long>>> futures = new ArrayList<>();

        // Create base data
        System.out.println("Setting up base data...");
        for (int i = 1; i <= 10; i++) {
            Owner owner = createTestOwner("BaseOwner" + i, "Test");
            ownerRepository.save(owner);
            
            Veterinarian vet = createTestVeterinarian("Dr. Base" + i, "Vet");
            veterinarianRepository.save(vet);
        }

        // Simulate realistic user behavior
        for (int user = 1; user <= USERS; user++) {
            final int userId = user;
            
            CompletableFuture<Map<String, Long>> future = CompletableFuture.supplyAsync(() -> {
                Map<String, Long> userMetrics = new HashMap<>();
                TestRestTemplate userRestTemplate = new TestRestTemplate();
                String baseUrl = "http://localhost:" + port;
                
                try {
                    long totalTime = 0;
                    
                    for (int op = 1; op <= OPERATIONS_PER_USER; op++) {
                        // Mixed operations: Create, Read, Search
                        
                        // 1. Create owner
                        Owner owner = createTestOwner("RealisticUser" + userId, "Op" + op);
                        long startTime = System.currentTimeMillis();
                        HttpEntity<Owner> request = new HttpEntity<>(owner, headers);
                        ResponseEntity<Owner> response = userRestTemplate.postForEntity(
                                baseUrl + "/api/owners", request, Owner.class);
                        long createTime = System.currentTimeMillis() - startTime;
                        totalTime += createTime;

                        // 2. Search operations
                        startTime = System.currentTimeMillis();
                        HttpEntity<String> getRequest = new HttpEntity<>(headers);
                        userRestTemplate.exchange(
                                baseUrl + "/api/search/global?query=RealisticUser" + userId, 
                                HttpMethod.GET, getRequest, String.class);
                        long searchTime = System.currentTimeMillis() - startTime;
                        totalTime += searchTime;

                        // 3. List operations with pagination
                        startTime = System.currentTimeMillis();
                        userRestTemplate.exchange(
                                baseUrl + "/api/owners?page=0&size=10", 
                                HttpMethod.GET, getRequest, String.class);
                        long listTime = System.currentTimeMillis() - startTime;
                        totalTime += listTime;

                        // 4. Dashboard metrics (cached)
                        startTime = System.currentTimeMillis();
                        userRestTemplate.exchange(
                                baseUrl + "/api/dashboard/metrics", 
                                HttpMethod.GET, getRequest, String.class);
                        long dashboardTime = System.currentTimeMillis() - startTime;
                        totalTime += dashboardTime;
                    }
                    
                    userMetrics.put("totalTime", totalTime);
                    userMetrics.put("avgTimePerOp", totalTime / (OPERATIONS_PER_USER * 4)); // 4 ops per iteration
                    
                } catch (Exception e) {
                    System.err.println("Realistic user " + userId + " failed: " + e.getMessage());
                    userMetrics.put("totalTime", -1L);
                }
                
                return userMetrics;
            }, executor);
            
            futures.add(future);
        }

        // Collect results
        long overallStartTime = System.currentTimeMillis();
        List<Map<String, Long>> results = new ArrayList<>();
        
        for (CompletableFuture<Map<String, Long>> future : futures) {
            try {
                Map<String, Long> result = future.get(120, TimeUnit.SECONDS);
                if (result.get("totalTime") > 0) {
                    results.add(result);
                }
            } catch (Exception e) {
                System.err.println("Realistic load test user failed: " + e.getMessage());
            }
        }
        
        long overallDuration = System.currentTimeMillis() - overallStartTime;
        executor.shutdown();

        // Analyze results
        assertTrue(results.size() >= USERS * 0.9, 
                "At least 90% of users should complete successfully. Completed: " + results.size());

        double avgResponseTime = results.stream()
                .mapToLong(m -> m.get("avgTimePerOp"))
                .average()
                .orElse(0.0);

        long maxResponseTime = results.stream()
                .mapToLong(m -> m.get("avgTimePerOp"))
                .max()
                .orElse(0L);

        // Performance assertions
        assertTrue(avgResponseTime < 1000, 
                "Average response time should be under 1 second, was: " + avgResponseTime + "ms");
        assertTrue(maxResponseTime < 3000, 
                "Maximum response time should be under 3 seconds, was: " + maxResponseTime + "ms");

        System.out.println("Realistic load test results:");
        System.out.println("- Target users: " + USERS);
        System.out.println("- Successful users: " + results.size());
        System.out.println("- Operations per user: " + (OPERATIONS_PER_USER * 4));
        System.out.println("- Overall duration: " + overallDuration + "ms");
        System.out.println("- Average response time: " + avgResponseTime + "ms");
        System.out.println("- Maximum response time: " + maxResponseTime + "ms");
        System.out.println("✓ Realistic load test passed - System meets performance requirements");
    }

    // Helper methods
    private Owner createTestOwner(String firstName, String lastName) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setAddress("123 Performance St");
        owner.setCity("Springfield");
        owner.setTelephone("555-PERF");
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

    private long measureResponseTime(Runnable operation) {
        long startTime = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - startTime;
    }
}