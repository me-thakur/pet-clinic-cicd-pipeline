package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.dto.AuthRequest;
import com.petclinic.backend.dto.AuthResponse;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.Specialty;
import com.petclinic.backend.model.VisitType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * System Integration Test
 * 
 * This test validates that all components of the Pet Clinic system work together correctly.
 * It tests the complete workflow from authentication through CRUD operations on all entities.
 * 
 * Validates: Requirements All
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate to get JWT token
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
    void testCompleteWorkflow() throws Exception {
        // 1. Create an Owner
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setAddress("123 Main St");
        owner.setCity("Springfield");
        owner.setTelephone("555-1234");

        MvcResult ownerResult = mockMvc.perform(post("/api/owners")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(owner)))
                .andExpect(status().isCreated())
                .andReturn();

        Owner createdOwner = objectMapper.readValue(
                ownerResult.getResponse().getContentAsString(), Owner.class);
        assertNotNull(createdOwner.getId());
        assertEquals("John", createdOwner.getFirstName());

        // 2. Create a Pet for the Owner
        Pet pet = new Pet();
        pet.setName("Buddy");
        pet.setSpecies("Dog");
        pet.setBreed("Golden Retriever");
        pet.setBirthDate(LocalDate.of(2020, 5, 15));
        pet.setOwner(createdOwner);

        MvcResult petResult = mockMvc.perform(post("/api/pets")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pet)))
                .andExpect(status().isCreated())
                .andReturn();

        Pet createdPet = objectMapper.readValue(
                petResult.getResponse().getContentAsString(), Pet.class);
        assertNotNull(createdPet.getId());
        assertEquals("Buddy", createdPet.getName());

        // 3. Create a Veterinarian
        Veterinarian vet = new Veterinarian();
        vet.setFirstName("Dr. Jane");
        vet.setLastName("Smith");
        vet.setLicenseNumber("VET12345");
        vet.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));

        MvcResult vetResult = mockMvc.perform(post("/api/veterinarians")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vet)))
                .andExpect(status().isCreated())
                .andReturn();

        Veterinarian createdVet = objectMapper.readValue(
                vetResult.getResponse().getContentAsString(), Veterinarian.class);
        assertNotNull(createdVet.getId());
        assertEquals("Dr. Jane", createdVet.getFirstName());

        // 4. Schedule a Visit
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
        assertNotNull(createdVisit.getId());
        assertEquals(VisitType.WELLNESS_EXAM, createdVisit.getVisitType());

        // 5. Test Search Functionality
        mockMvc.perform(get("/api/search/global")
                .header("Authorization", authToken)
                .param("query", "Buddy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pets").isArray())
                .andExpect(jsonPath("$.pets[0].name").value("Buddy"));

        // 6. Test Dashboard Metrics
        mockMvc.perform(get("/api/dashboard/metrics")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPets").exists())
                .andExpect(jsonPath("$.totalOwners").exists())
                .andExpect(jsonPath("$.totalVeterinarians").exists());

        // 7. Test Report Generation
        mockMvc.perform(get("/api/reports/visits")
                .header("Authorization", authToken)
                .param("startDate", LocalDate.now().minusDays(30).toString())
                .param("endDate", LocalDate.now().plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").exists());

        // 8. Test API Documentation
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        // 9. Test Health Check
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testErrorHandling() throws Exception {
        // Test invalid authentication
        mockMvc.perform(get("/api/pets")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        // Test validation errors
        Pet invalidPet = new Pet();
        // Missing required fields

        mockMvc.perform(post("/api/pets")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPet)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        // Test not found error
        mockMvc.perform(get("/api/pets/99999")
                .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PET_NOT_FOUND"));
    }

    @Test
    void testCrossOriginRequests() throws Exception {
        // Test CORS headers are present
        mockMvc.perform(options("/api/pets")
                .header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void testPaginationAndFiltering() throws Exception {
        // Test pagination
        mockMvc.perform(get("/api/pets")
                .header("Authorization", authToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists());

        // Test filtering
        mockMvc.perform(get("/api/filters/pets")
                .header("Authorization", authToken)
                .param("species", "Dog")
                .param("minAge", "1")
                .param("maxAge", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testCachingBehavior() throws Exception {
        // First request - should hit database
        MvcResult result1 = mockMvc.perform(get("/api/dashboard/metrics")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andReturn();

        // Second request - should hit cache (faster response)
        MvcResult result2 = mockMvc.perform(get("/api/dashboard/metrics")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andReturn();

        // Results should be identical
        assertEquals(result1.getResponse().getContentAsString(),
                    result2.getResponse().getContentAsString());
    }

    @Test
    void testAuditLogging() throws Exception {
        // Create a pet (should generate audit log)
        Pet pet = new Pet();
        pet.setName("Test Pet");
        pet.setSpecies("Cat");
        pet.setBirthDate(LocalDate.of(2021, 1, 1));

        mockMvc.perform(post("/api/pets")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pet)))
                .andExpect(status().isCreated());

        // Check audit logs (admin only)
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", authToken)
                .param("action", "CREATE")
                .param("entityType", "Pet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}