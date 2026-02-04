package com.petclinic.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Test for Visit Status Fix
 * 
 * This test validates the complete visit update flow from frontend to backend
 * to ensure status synchronization across the entire system.
 * Tests complete visit update flow from frontend to backend and verifies 
 * status synchronization across the entire system.
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Visit Status Fix End-to-End Integration Tests")
class VisitStatusFixEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Owner testOwner;
    private Pet testPet;
    private Veterinarian testVet;

    @BeforeEach
    void setUp() {
        // Clean up data before each test
        visitRepository.deleteAll();
        petRepository.deleteAll();
        ownerRepository.deleteAll();
        veterinarianRepository.deleteAll();

        // Create test data
        testOwner = createAndSaveOwner("John", "Doe", "123 Main St", "Springfield", "555-1234");
        testPet = createAndSavePet("Buddy", "Dog", "Golden Retriever", LocalDate.of(2020, 1, 15), testOwner);
        testVet = createAndSaveVeterinarian("Dr. Jane", "Smith", "VET123", Set.of(Specialty.GENERAL_PRACTICE));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "USER")
    @DisplayName("Complete visit lifecycle with automatic status calculation")
    void testCompleteVisitLifecycleWithStatusCalculation() throws Exception {
        // **Scenario: Test complete visit lifecycle from creation to completion with automatic status calculation**
        
        // 1. Create a new visit (should start as pending)
        Map<String, Object> visitData = new HashMap<>();
        visitData.put("visitDate", LocalDateTime.now().plusDays(1).toString());
        visitData.put("visitType", "WELLNESS_EXAM");
        visitData.put("pet", Map.of("id", testPet.getId()));
        
        MvcResult createResult = mockMvc.perform(post("/api/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visitData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false)) // Should be pending initially
                .andReturn();

        Visit createdVisit = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Visit.class);
        Long visitId = createdVisit.getId();

        // 2. Retrieve visit and verify it's pending
        mockMvc.perform(get("/api/visits/{id}", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visitId))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.treatment").doesNotExist());

        // 3. Update visit with only diagnosis (should remain pending)
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("diagnosis", "Routine checkup - healthy");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)) // Still pending
                .andExpect(jsonPath("$.diagnosis").value("Routine checkup - healthy"))
                .andExpect(jsonPath("$.treatment").doesNotExist());

        // 4. Update visit with only treatment (should remain pending)
        partialUpdate.clear();
        partialUpdate.put("treatment", "Vaccinations administered");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)) // Still pending (diagnosis was cleared)
                .andExpect(jsonPath("$.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.treatment").value("Vaccinations administered"));

        // 5. Update visit with both diagnosis and treatment (should become completed)
        partialUpdate.clear();
        partialUpdate.put("diagnosis", "Routine checkup - healthy");
        partialUpdate.put("treatment", "Vaccinations administered");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true)) // Now completed!
                .andExpect(jsonPath("$.diagnosis").value("Routine checkup - healthy"))
                .andExpect(jsonPath("$.treatment").value("Vaccinations administered"));

        // 6. Verify status persists on subsequent retrieval
        mockMvc.perform(get("/api/visits/{id}", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.diagnosis").value("Routine checkup - healthy"))
                .andExpect(jsonPath("$.treatment").value("Vaccinations administered"));

        // 7. Test removing diagnosis (should become pending again)
        partialUpdate.clear();
        partialUpdate.put("diagnosis", "");
        partialUpdate.put("treatment", "Vaccinations administered");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)) // Back to pending
                .andExpect(jsonPath("$.treatment").value("Vaccinations administered"));

        // 8. Test removing treatment (should remain pending)
        partialUpdate.clear();
        partialUpdate.put("diagnosis", "Routine checkup - healthy");
        partialUpdate.put("treatment", "");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)) // Still pending
                .andExpect(jsonPath("$.diagnosis").value("Routine checkup - healthy"));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "USER")
    @DisplayName("Test whitespace handling in completion logic")
    void testWhitespaceHandlingInCompletionLogic() throws Exception {
        // **Scenario: Test completion logic edge cases with whitespace-only fields**
        
        // Create a visit
        Map<String, Object> visitData = new HashMap<>();
        visitData.put("visitDate", LocalDateTime.now().plusDays(1).toString());
        visitData.put("visitType", "WELLNESS_EXAM");
        visitData.put("pet", Map.of("id", testPet.getId()));
        
        MvcResult createResult = mockMvc.perform(post("/api/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visitData)))
                .andExpect(status().isCreated())
                .andReturn();

        Visit createdVisit = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Visit.class);
        Long visitId = createdVisit.getId();

        // Test whitespace-only diagnosis
        Map<String, Object> update = new HashMap<>();
        update.put("diagnosis", "   ");
        update.put("treatment", "Valid treatment");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)); // Should be pending

        // Test whitespace-only treatment
        update.clear();
        update.put("diagnosis", "Valid diagnosis");
        update.put("treatment", "\t\n  ");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)); // Should be pending

        // Test both fields with whitespace only
        update.clear();
        update.put("diagnosis", "  \t  ");
        update.put("treatment", "  \n  ");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)); // Should be pending

        // Test valid content with surrounding whitespace (should be completed)
        update.clear();
        update.put("diagnosis", "  Valid diagnosis  ");
        update.put("treatment", "  Valid treatment  ");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true)); // Should be completed
    }

    @Test
    @Transactional
    @WithMockUser(roles = "USER")
    @DisplayName("Test visit list endpoints include completion status")
    void testVisitListEndpointsIncludeCompletionStatus() throws Exception {
        // **Scenario: Verify all visit list endpoints return consistent completion status**
        
        // Create multiple visits with different completion states
        Visit pendingVisit = createAndSaveVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, testPet, testVet);
        
        Visit completedVisit = createTestVisit(LocalDateTime.now().plusDays(2), VisitType.SURGERY, testPet, testVet);
        completedVisit.setDiagnosis("Surgery completed successfully");
        completedVisit.setTreatment("Post-operative care provided");
        completedVisit = visitRepository.save(completedVisit);

        // Test GET /api/visits (all visits)
        mockMvc.perform(get("/api/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].completed").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + pendingVisit.getId() + ")].completed").value(false))
                .andExpect(jsonPath("$.content[?(@.id == " + completedVisit.getId() + ")].completed").value(true));

        // Test GET /api/visits/completed
        mockMvc.perform(get("/api/visits/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].completed").value(true))
                .andExpect(jsonPath("$[?(@.id == " + completedVisit.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + pendingVisit.getId() + ")]").doesNotExist());

        // Test GET /api/visits/incomplete
        mockMvc.perform(get("/api/visits/incomplete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].completed").value(false))
                .andExpect(jsonPath("$[?(@.id == " + pendingVisit.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + completedVisit.getId() + ")]").doesNotExist());

        // Test GET /api/pets/{id}/visits
        mockMvc.perform(get("/api/pets/{id}/visits", testPet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].completed").exists())
                .andExpect(jsonPath("$[?(@.id == " + pendingVisit.getId() + ")].completed").value(false))
                .andExpect(jsonPath("$[?(@.id == " + completedVisit.getId() + ")].completed").value(true));

        // Test GET /api/veterinarians/{id}/visits
        mockMvc.perform(get("/api/veterinarians/{id}/visits", testVet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].completed").exists())
                .andExpect(jsonPath("$[?(@.id == " + pendingVisit.getId() + ")].completed").value(false))
                .andExpect(jsonPath("$[?(@.id == " + completedVisit.getId() + ")].completed").value(true));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "USER")
    @DisplayName("Test concurrent visit updates maintain consistency")
    void testConcurrentVisitUpdatesMaintainConsistency() throws Exception {
        // **Scenario: Test that concurrent updates maintain completion status consistency**
        
        // Create a visit
        Visit newVisit = createAndSaveVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, testPet, testVet);
        Long visitId = newVisit.getId();

        // Simulate concurrent updates
        Map<String, Object> update1 = new HashMap<>();
        update1.put("diagnosis", "First diagnosis");
        
        Map<String, Object> update2 = new HashMap<>();
        update2.put("treatment", "First treatment");

        // Apply both updates
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update1)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update2)))
                .andExpect(status().isOk());

        // Verify final state is consistent
        MvcResult finalResult = mockMvc.perform(get("/api/visits/{id}", visitId))
                .andExpect(status().isOk())
                .andReturn();

        Visit finalVisit = objectMapper.readValue(
                finalResult.getResponse().getContentAsString(), Visit.class);

        // Check completion status matches the actual field values
        boolean expectedCompletion = finalVisit.getDiagnosis() != null && !finalVisit.getDiagnosis().trim().isEmpty() &&
                                   finalVisit.getTreatment() != null && !finalVisit.getTreatment().trim().isEmpty();
        
        assertEquals(expectedCompletion, finalVisit.isCompleted(),
                "Completion status should match the actual diagnosis and treatment field values");
    }

    @Test
    @Transactional
    @WithMockUser(roles = "USER")
    @DisplayName("Test error handling maintains data consistency")
    void testErrorHandlingMaintainsDataConsistency() throws Exception {
        // **Scenario: Test that error conditions don't leave visits in inconsistent state**
        
        // Create a visit
        Visit newVisit = createAndSaveVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, testPet, testVet);
        Long visitId = newVisit.getId();

        // Test invalid JSON (should not affect visit)
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());

        // Verify visit is unchanged
        mockMvc.perform(get("/api/visits/{id}", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        // Test update with invalid visit ID (should return 404)
        Map<String, Object> update = new HashMap<>();
        update.put("diagnosis", "Test diagnosis");
        
        mockMvc.perform(put("/api/visits/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        // Test successful update after error
        update.put("diagnosis", "Valid diagnosis");
        update.put("treatment", "Valid treatment");
        
        mockMvc.perform(put("/api/visits/{id}", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.diagnosis").value("Valid diagnosis"))
                .andExpect(jsonPath("$.treatment").value("Valid treatment"));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "USER")
    @DisplayName("Test batch recalculation functionality")
    void testBatchRecalculationFunctionality() throws Exception {
        // **Scenario: Test the batch recalculation functionality for data consistency**
        
        // Create visits with different states
        Visit visit1 = createAndSaveVisit(LocalDateTime.now().plusDays(1), VisitType.WELLNESS_EXAM, testPet, testVet);
        visit1.setDiagnosis("Diagnosis 1");
        visit1.setTreatment("Treatment 1");
        visit1 = visitRepository.save(visit1);

        Visit visit2 = createAndSaveVisit(LocalDateTime.now().plusDays(2), VisitType.SURGERY, testPet, testVet);
        visit2.setDiagnosis("Diagnosis 2");
        // No treatment - should be pending
        visit2 = visitRepository.save(visit2);

        Visit visit3 = createAndSaveVisit(LocalDateTime.now().plusDays(3), VisitType.VACCINATION, testPet, testVet);
        // No diagnosis or treatment - should be pending
        visit3 = visitRepository.save(visit3);

        // Test batch recalculation endpoint
        mockMvc.perform(post("/api/admin/visits/recalculate-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Visit status recalculation completed"))
                .andExpect(jsonPath("$.processedCount").value(3));

        // Verify each visit has correct completion status
        mockMvc.perform(get("/api/visits/{id}", visit1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true)); // Has both diagnosis and treatment

        mockMvc.perform(get("/api/visits/{id}", visit2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)); // Missing treatment

        mockMvc.perform(get("/api/visits/{id}", visit3.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false)); // Missing both
    }

    // Helper methods for creating test data
    private Owner createAndSaveOwner(String firstName, String lastName, String address, String city, String telephone) {
        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setAddress(address);
        owner.setCity(city);
        owner.setTelephone(telephone);
        return ownerRepository.save(owner);
    }

    private Pet createAndSavePet(String name, String species, String breed, LocalDate birthDate, Owner owner) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setBirthDate(birthDate);
        pet.setOwner(owner);
        return petRepository.save(pet);
    }

    private Veterinarian createAndSaveVeterinarian(String firstName, String lastName, String licenseNumber, Set<Specialty> specialties) {
        Veterinarian vet = new Veterinarian();
        vet.setFirstName(firstName);
        vet.setLastName(lastName);
        vet.setLicenseNumber(licenseNumber);
        vet.setSpecialtySet(specialties);
        return veterinarianRepository.save(vet);
    }

    private Visit createTestVisit(LocalDateTime visitDate, VisitType visitType, Pet pet, Veterinarian veterinarian) {
        Visit visit = new Visit();
        visit.setVisitDate(visitDate);
        visit.setVisitType(visitType);
        visit.setPet(pet);
        visit.setVeterinarian(veterinarian);
        return visit;
    }

    private Visit createAndSaveVisit(LocalDateTime visitDate, VisitType visitType, Pet pet, Veterinarian veterinarian) {
        Visit visit = createTestVisit(visitDate, visitType, pet, veterinarian);
        return visitRepository.save(visit);
    }
}