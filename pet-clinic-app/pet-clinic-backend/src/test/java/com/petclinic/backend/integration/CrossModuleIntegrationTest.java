package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.AuthRequest;
import com.petclinic.backend.dto.AuthResponse;
import com.petclinic.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-Module Integration Test
 * 
 * This test validates the integration between frontend and backend modules,
 * testing data consistency, API contract compliance, and cross-module functionality.
 * It simulates realistic scenarios where frontend components interact with backend services.
 * 
 * **Validates: Requirements All (Cross-Module Integration)**
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CrossModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate for API access
        AuthRequest authRequest = new AuthRequest("admin", "admin123");
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        authToken = "Bearer " + authResponse.getToken();
    }

    @Test
    void testFrontendBackendDataConsistency() throws Exception {
        // **Scenario: Frontend creates data through API and verifies consistency**
        
        // 1. Frontend creates owner through API
        Owner owner = new Owner();
        owner.setFirstName("Frontend");
        owner.setLastName("User");
        owner.setAddress("123 Frontend St");
        owner.setCity("Springfield");
        owner.setTelephone("555-FRONT");

        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Frontend"))
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);

        // 2. Frontend retrieves owner data and verifies consistency
        mockMvc.perform(get("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdOwner.getId()))
                .andExpect(jsonPath("$.firstName").value("Frontend"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.address").value("123 Frontend St"))
                .andExpect(jsonPath("$.city").value("Springfield"))
                .andExpect(jsonPath("$.telephone").value("555-FRONT"));

        // 3. Frontend searches for owner and verifies search results
        mockMvc.perform(get("/api/search/global")
                .header("Authorization", authToken)
                .param("query", "Frontend User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners").isArray())
                .andExpect(jsonPath("$.owners[0].firstName").value("Frontend"))
                .andExpect(jsonPath("$.owners[0].lastName").value("User"));

        // 4. Frontend updates owner data
        createdOwner.setTelephone("555-UPDATED");
        
        mockMvc.perform(put("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("555-UPDATED"));

        // 5. Verify update is immediately visible in subsequent requests
        mockMvc.perform(get("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("555-UPDATED"));
    }

    @Test
    void testPaginationAndFilteringIntegration() throws Exception {
        // **Scenario: Frontend uses pagination and filtering features**
        
        // 1. Create test data for pagination
        List<Owner> owners = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Owner owner = new Owner();
            owner.setFirstName("Owner" + i);
            owner.setLastName("Test");
            owner.setAddress(i + " Test St");
            owner.setCity("Springfield");
            owner.setTelephone("555-" + String.format("%04d", i));

            MvcResult result = mockMvc.perform(post("/api/owners")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(owner)))
                    .andExpect(status().isCreated())
                    .andReturn();

            owners.add(objectMapper.readValue(result.getResponse().getContentAsString(), Owner.class));
        }

        // 2. Test pagination - first page
        mockMvc.perform(get("/api/owners")
                .header("Authorization", authToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));

        // 3. Test pagination - second page
        mockMvc.perform(get("/api/owners")
                .header("Authorization", authToken)
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.pageable.pageNumber").value(1));

        // 4. Test pagination - last page
        mockMvc.perform(get("/api/owners")
                .header("Authorization", authToken)
                .param("page", "2")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.pageable.pageNumber").value(2));

        // 5. Test filtering by name
        mockMvc.perform(get("/api/filters/owners")
                .header("Authorization", authToken)
                .param("firstName", "Owner1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[?(@.firstName == 'Owner1')]").exists());

        // 6. Test combined filtering and pagination
        mockMvc.perform(get("/api/filters/owners")
                .header("Authorization", authToken)
                .param("lastName", "Test")
                .param("city", "Springfield")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(25));
    }

    @Test
    void testConcurrentUserOperations() throws Exception {
        // **Scenario: Multiple frontend users perform operations simultaneously**
        
        // Create test data
        Owner owner = new Owner();
        owner.setFirstName("Concurrent");
        owner.setLastName("Test");
        owner.setAddress("123 Concurrent St");
        owner.setCity("Springfield");
        owner.setTelephone("555-CONC");

        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);

        // Create multiple pets concurrently
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int petNumber = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Pet pet = new Pet();
                    pet.setName("ConcurrentPet" + petNumber);
                    pet.setSpecies("Dog");
                    pet.setBreed("TestBreed" + petNumber);
                    pet.setBirthDate(LocalDate.of(2020, petNumber, 1));
                    pet.setOwner(createdOwner);

                    mockMvc.perform(post("/api/pets")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(pet)))
                            .andExpect(status().isCreated());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Verify all pets were created successfully
        mockMvc.perform(get("/api/owners/{id}/pets", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void testAPIContractCompliance() throws Exception {
        // **Scenario: Verify API responses match expected contract for frontend consumption**
        
        // 1. Test owner creation response structure
        Owner owner = new Owner();
        owner.setFirstName("Contract");
        owner.setLastName("Test");
        owner.setAddress("123 Contract St");
        owner.setCity("Springfield");
        owner.setTelephone("555-CONTRACT");

        mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").isString())
                .andExpect(jsonPath("$.lastName").isString())
                .andExpect(jsonPath("$.address").isString())
                .andExpect(jsonPath("$.city").isString())
                .andExpect(jsonPath("$.telephone").isString())
                .andExpect(jsonPath("$.pets").doesNotExist()); // Should not include pets in creation response

        // 2. Test error response structure
        Owner invalidOwner = new Owner();
        // Missing required fields

        mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOwner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.errorCode").exists());

        // 3. Test pagination response structure
        mockMvc.perform(get("/api/owners")
                .header("Authorization", authToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.pageable.pageNumber").isNumber())
                .andExpect(jsonPath("$.pageable.pageSize").isNumber())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").isBoolean())
                .andExpect(jsonPath("$.last").isBoolean());

        // 4. Test search response structure
        mockMvc.perform(get("/api/search/global")
                .header("Authorization", authToken)
                .param("query", "Contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners").isArray())
                .andExpect(jsonPath("$.pets").isArray())
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.totalResults").isNumber())
                .andExpect(jsonPath("$.searchTerm").isString());
    }

    @Test
    void testCachingBehaviorAcrossModules() throws Exception {
        // **Scenario: Test caching behavior between frontend and backend**
        
        // 1. Create test data
        Owner owner = new Owner();
        owner.setFirstName("Cache");
        owner.setLastName("Test");
        owner.setAddress("123 Cache St");
        owner.setCity("Springfield");
        owner.setTelephone("555-CACHE");

        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);

        // 2. First request - should hit database
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(get("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk());
        long duration1 = System.currentTimeMillis() - startTime1;

        // 3. Second request - should hit cache (faster)
        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(get("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk());
        long duration2 = System.currentTimeMillis() - startTime2;

        // Cache should make second request faster (though this is timing-dependent)
        assertTrue(duration2 <= duration1 + 50, "Second request should be as fast or faster due to caching");

        // 4. Update owner - should invalidate cache
        createdOwner.setTelephone("555-UPDATED");
        mockMvc.perform(put("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdOwner)))
                .andExpect(status().isOk());

        // 5. Next request should reflect updated data
        mockMvc.perform(get("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("555-UPDATED"));
    }

    @Test
    void testSecurityIntegrationAcrossModules() throws Exception {
        // **Scenario: Test security enforcement across frontend-backend integration**
        
        // 1. Test unauthenticated access is denied
        mockMvc.perform(get("/api/owners"))
                .andExpect(status().isUnauthorized());

        // 2. Test invalid token is rejected
        mockMvc.perform(get("/api/owners")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        // 3. Test expired token handling (simulate with malformed token)
        mockMvc.perform(get("/api/owners")
                .header("Authorization", "Bearer expired.token.here"))
                .andExpect(status().isUnauthorized());

        // 4. Test role-based access control
        // Admin can access admin endpoints
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", authToken))
                .andExpect(status().isOk());

        // 5. Test CORS headers for frontend integration
        mockMvc.perform(options("/api/owners")
                .header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    void testRealTimeDataSynchronization() throws Exception {
        // **Scenario: Test real-time data synchronization between modules**
        
        // 1. Create veterinarian and pets for scheduling
        Veterinarian vet = new Veterinarian();
        vet.setFirstName("Dr. Sync");
        vet.setLastName("Test");
        vet.setLicenseNumber("VETSYNC1");
        vet.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));

        MvcResult vetResult = mockMvc.perform(post("/api/veterinarians")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vet)))
                .andExpect(status().isCreated())
                .andReturn();

        Veterinarian createdVet = objectMapper.readValue(
                vetResult.getResponse().getContentAsString(), Veterinarian.class);

        Owner owner = new Owner();
        owner.setFirstName("Sync");
        owner.setLastName("Owner");
        owner.setAddress("123 Sync St");
        owner.setCity("Springfield");
        owner.setTelephone("555-SYNC");

        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);

        Pet pet = new Pet();
        pet.setName("SyncPet");
        pet.setSpecies("Dog");
        pet.setBreed("Synchronizer");
        pet.setBirthDate(LocalDate.of(2020, 1, 1));
        pet.setOwner(createdOwner);

        MvcResult petResult = mockMvc.perform(post("/api/pets")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pet)))
                .andExpect(status().isCreated())
                .andReturn();

        Pet createdPet = objectMapper.readValue(
                petResult.getResponse().getContentAsString(), Pet.class);

        // 2. Schedule a visit
        Visit visit = new Visit();
        visit.setVisitDate(LocalDateTime.now().plusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setPet(createdPet);
        visit.setVeterinarian(createdVet);

        MvcResult visitResult = mockMvc.perform(post("/api/visits")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated())
                .andReturn();

        Visit createdVisit = objectMapper.readValue(
                visitResult.getResponse().getContentAsString(), Visit.class);

        // 3. Verify visit appears in veterinarian's schedule immediately
        mockMvc.perform(get("/api/veterinarians/{id}/visits", createdVet.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(createdVisit.getId()));

        // 4. Verify visit appears in pet's history immediately
        mockMvc.perform(get("/api/pets/{id}/visits", createdPet.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(createdVisit.getId()));

        // 5. Update visit and verify changes are immediately visible
        createdVisit.setNotes("Updated notes for synchronization test");
        
        mockMvc.perform(put("/api/visits/{id}", createdVisit.getId())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdVisit)))
                .andExpect(status().isOk());

        // 6. Verify update is immediately visible in all related views
        mockMvc.perform(get("/api/visits/{id}", createdVisit.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes for synchronization test"));

        mockMvc.perform(get("/api/pets/{id}/visits", createdPet.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notes").value("Updated notes for synchronization test"));
    }

    @Test
    void testErrorHandlingConsistencyAcrossModules() throws Exception {
        // **Scenario: Test consistent error handling between frontend and backend**
        
        // 1. Test validation errors
        Owner invalidOwner = new Owner();
        // Missing required fields
        
        mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOwner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        // 2. Test not found errors
        mockMvc.perform(get("/api/owners/99999")
                .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("OWNER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        // 3. Test business rule violations
        // Create owner and pet first
        Owner owner = new Owner();
        owner.setFirstName("Error");
        owner.setLastName("Test");
        owner.setAddress("123 Error St");
        owner.setCity("Springfield");
        owner.setTelephone("555-ERROR");

        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);

        Pet pet = new Pet();
        pet.setName("ErrorPet");
        pet.setSpecies("Dog");
        pet.setBreed("TestBreed");
        pet.setBirthDate(LocalDate.of(2020, 1, 1));
        pet.setOwner(createdOwner);

        mockMvc.perform(post("/api/pets")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pet)))
                .andExpect(status().isCreated());

        // Try to delete owner with pets (should fail)
        mockMvc.perform(delete("/api/owners/{id}", createdOwner.getId())
                .header("Authorization", authToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REFERENTIAL_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").exists());

        // 4. Test server errors are handled gracefully
        // This would typically involve mocking a service to throw an exception
        // For now, we test that the error response structure is consistent
    }
}