package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.model.*;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.repository.VeterinarianRepository;
import com.petclinic.backend.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VisitController
 * Tests the complete flow with real database and services
 * Validates end-to-end functionality including:
 * - Database persistence and retrieval
 * - Transaction behavior
 * - Real business logic execution
 * - Data integrity constraints
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("VisitController Integration Tests")
class VisitControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    private MockMvc mockMvc;
    private Owner testOwner;
    private Pet testPet;
    private Veterinarian testVeterinarian;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up any existing data
        visitRepository.deleteAll();
        petRepository.deleteAll();
        veterinarianRepository.deleteAll();
        ownerRepository.deleteAll();

        // Create test owner
        testOwner = new Owner();
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Springfield");
        testOwner.setTelephone("555-1234");
        testOwner = ownerRepository.save(testOwner);

        // Create test pet
        testPet = new Pet();
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 15));
        testPet.setOwner(testOwner);
        testPet = petRepository.save(testPet);

        // Create test veterinarian
        testVeterinarian = new Veterinarian();
        testVeterinarian.setFirstName("Dr. Jane");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        testVeterinarian.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));
        testVeterinarian = veterinarianRepository.save(testVeterinarian);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should create and retrieve visit successfully")
    void shouldCreateAndRetrieveVisit() throws Exception {
        // Given
        Visit visit = new Visit();
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setVisitDate(LocalDateTime.now().plusDays(7));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setDiagnosis("Regular wellness check");
        visit.setCost(new BigDecimal("75.00"));

        // When - Create visit
        String response = mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.visitType").value("WELLNESS_EXAM"))
                .andExpect(jsonPath("$.diagnosis").value("Regular wellness check"))
                .andExpect(jsonPath("$.cost").value(75.00))
                .andReturn().getResponse().getContentAsString();

        // Extract ID from response
        Visit createdVisit = objectMapper.readValue(response, Visit.class);
        Long visitId = createdVisit.getId();

        // Then - Retrieve visit
        mockMvc.perform(get("/api/visits/" + visitId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(visitId))
                .andExpect(jsonPath("$.visitType").value("WELLNESS_EXAM"))
                .andExpect(jsonPath("$.diagnosis").value("Regular wellness check"))
                .andExpect(jsonPath("$.cost").value(75.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should schedule visit with conflict detection")
    void shouldScheduleVisitWithConflictDetection() throws Exception {
        // Given - Create first visit
        Visit firstVisit = new Visit();
        firstVisit.setPet(testPet);
        firstVisit.setVeterinarian(testVeterinarian);
        firstVisit.setVisitDate(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        firstVisit.setVisitType(VisitType.WELLNESS_EXAM);
        firstVisit.setDiagnosis("First visit");
        firstVisit.setCost(new BigDecimal("75.00"));

        // Schedule first visit
        mockMvc.perform(post("/api/visits/schedule")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstVisit)))
                .andExpect(status().isCreated());

        // When - Check for conflicts
        LocalDateTime conflictDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(15);
        mockMvc.perform(get("/api/visits/conflicts")
                .param("vetId", testVeterinarian.getId().toString())
                .param("dateTime", conflictDateTime.toString())
                .param("duration", "30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasConflict").value(true));

        // Then - Check for no conflicts at different time
        LocalDateTime noConflictDateTime = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        mockMvc.perform(get("/api/visits/conflicts")
                .param("vetId", testVeterinarian.getId().toString())
                .param("dateTime", noConflictDateTime.toString())
                .param("duration", "30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasConflict").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should complete visit with diagnosis and treatment")
    void shouldCompleteVisitWithDiagnosisAndTreatment() throws Exception {
        // Given - Create visit
        Visit visit = new Visit();
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setVisitDate(LocalDateTime.now().minusHours(2)); // Past visit
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setDiagnosis("Wellness examination");
        visit.setCost(new BigDecimal("75.00"));

        String response = mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Visit createdVisit = objectMapper.readValue(response, Visit.class);
        Long visitId = createdVisit.getId();

        // When - Complete visit
        Map<String, String> completionData = Map.of(
            "diagnosis", "Healthy, no issues found",
            "treatment", "Routine vaccination administered",
            "notes", "Pet was cooperative during examination"
        );

        mockMvc.perform(put("/api/visits/" + visitId + "/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completionData)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(visitId))
                .andExpect(jsonPath("$.diagnosis").value("Healthy, no issues found"))
                .andExpect(jsonPath("$.treatment").value("Routine vaccination administered"))
                .andExpect(jsonPath("$.notes").value("Pet was cooperative during examination"));

        // Then - Verify completion persisted
        mockMvc.perform(get("/api/visits/" + visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Healthy, no issues found"))
                .andExpect(jsonPath("$.treatment").value("Routine vaccination administered"))
                .andExpect(jsonPath("$.notes").value("Pet was cooperative during examination"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should search visits by multiple criteria")
    void shouldSearchVisitsByMultipleCriteria() throws Exception {
        // Given - Create multiple visits
        Visit visit1 = new Visit();
        visit1.setPet(testPet);
        visit1.setVeterinarian(testVeterinarian);
        visit1.setVisitDate(LocalDateTime.now().plusDays(1));
        visit1.setVisitType(VisitType.WELLNESS_EXAM);
        visit1.setDiagnosis("Wellness check");
        visit1.setCost(new BigDecimal("75.00"));

        Visit visit2 = new Visit();
        visit2.setPet(testPet);
        visit2.setVeterinarian(testVeterinarian);
        visit2.setVisitDate(LocalDateTime.now().plusDays(2));
        visit2.setVisitType(VisitType.VACCINATION);
        visit2.setDiagnosis("Annual vaccination");
        visit2.setCost(new BigDecimal("45.00"));

        // Create visits
        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit2)))
                .andExpect(status().isCreated());

        // When & Then - Search by pet ID
        mockMvc.perform(get("/api/visits/search")
                .param("petId", testPet.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));

        // Search by veterinarian ID
        mockMvc.perform(get("/api/visits/search")
                .param("vetId", testVeterinarian.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));

        // Search by visit type
        mockMvc.perform(get("/api/visits/search")
                .param("visitType", "WELLNESS_EXAM"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].visitType").value("WELLNESS_EXAM"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get visits by pet")
    void shouldGetVisitsByPet() throws Exception {
        // Given - Create visits for the pet
        Visit visit1 = new Visit();
        visit1.setPet(testPet);
        visit1.setVeterinarian(testVeterinarian);
        visit1.setVisitDate(LocalDateTime.now().plusDays(1));
        visit1.setVisitType(VisitType.WELLNESS_EXAM);
        visit1.setCost(new BigDecimal("75.00"));

        Visit visit2 = new Visit();
        visit2.setPet(testPet);
        visit2.setVeterinarian(testVeterinarian);
        visit2.setVisitDate(LocalDateTime.now().plusDays(2));
        visit2.setVisitType(VisitType.VACCINATION);
        visit2.setCost(new BigDecimal("45.00"));

        // Create visits
        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit2)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/visits/pet/" + testPet.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get visits by veterinarian")
    void shouldGetVisitsByVeterinarian() throws Exception {
        // Given - Create visits for the veterinarian
        Visit visit = new Visit();
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setVisitDate(LocalDateTime.now().plusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setCost(new BigDecimal("75.00"));

        // Create visit
        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/visits/veterinarian/" + testVeterinarian.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].visitType").value("WELLNESS_EXAM"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get daily schedule")
    void shouldGetDailySchedule() throws Exception {
        // Given - Create visit for today
        Visit todayVisit = new Visit();
        todayVisit.setPet(testPet);
        todayVisit.setVeterinarian(testVeterinarian);
        todayVisit.setVisitDate(LocalDateTime.now().withHour(10).withMinute(0));
        todayVisit.setVisitType(VisitType.WELLNESS_EXAM);
        todayVisit.setCost(new BigDecimal("75.00"));

        // Create visit
        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(todayVisit)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/visits/schedule/daily"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].visitType").value("WELLNESS_EXAM"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get visit statistics")
    void shouldGetVisitStatistics() throws Exception {
        // Given - Create completed and scheduled visits
        Visit completedVisit = new Visit();
        completedVisit.setPet(testPet);
        completedVisit.setVeterinarian(testVeterinarian);
        completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
        completedVisit.setDiagnosis("Healthy");
        completedVisit.setTreatment("Vaccination");
        completedVisit.setCost(new BigDecimal("75.00"));

        Visit scheduledVisit = new Visit();
        scheduledVisit.setPet(testPet);
        scheduledVisit.setVeterinarian(testVeterinarian);
        scheduledVisit.setVisitDate(LocalDateTime.now().plusDays(1));
        scheduledVisit.setVisitType(VisitType.VACCINATION);
        scheduledVisit.setCost(new BigDecimal("45.00"));

        // Create visits
        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completedVisit)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduledVisit)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/visits/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalVisits").value(2))
                .andExpect(jsonPath("$.totalRevenue").value(120.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should get visit count")
    void shouldGetVisitCount() throws Exception {
        // Given - Create a visit
        Visit visit = new Visit();
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setVisitDate(LocalDateTime.now().plusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setCost(new BigDecimal("75.00"));

        // Create visit
        mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/visits/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should check health endpoint")
    void shouldCheckHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/visits/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("VisitController"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should update visit successfully")
    void shouldUpdateVisitSuccessfully() throws Exception {
        // Given - Create visit
        Visit visit = new Visit();
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setVisitDate(LocalDateTime.now().plusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setDiagnosis("Original diagnosis");
        visit.setCost(new BigDecimal("75.00"));

        String response = mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Visit createdVisit = objectMapper.readValue(response, Visit.class);
        Long visitId = createdVisit.getId();

        // When - Update visit
        Visit updatedVisit = new Visit();
        updatedVisit.setPet(testPet);
        updatedVisit.setVeterinarian(testVeterinarian);
        updatedVisit.setVisitDate(visit.getVisitDate());
        updatedVisit.setVisitType(VisitType.WELLNESS_EXAM);
        updatedVisit.setDiagnosis("Updated diagnosis");
        updatedVisit.setCost(new BigDecimal("85.00"));

        mockMvc.perform(put("/api/visits/" + visitId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedVisit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(visitId))
                .andExpect(jsonPath("$.diagnosis").value("Updated diagnosis"))
                .andExpect(jsonPath("$.cost").value(85.00));

        // Then - Verify update persisted
        mockMvc.perform(get("/api/visits/" + visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Updated diagnosis"))
                .andExpect(jsonPath("$.cost").value(85.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should delete visit successfully")
    void shouldDeleteVisitSuccessfully() throws Exception {
        // Given - Create visit
        Visit visit = new Visit();
        visit.setPet(testPet);
        visit.setVeterinarian(testVeterinarian);
        visit.setVisitDate(LocalDateTime.now().plusDays(1));
        visit.setVisitType(VisitType.WELLNESS_EXAM);
        visit.setCost(new BigDecimal("75.00"));

        String response = mockMvc.perform(post("/api/visits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Visit createdVisit = objectMapper.readValue(response, Visit.class);
        Long visitId = createdVisit.getId();

        // When - Delete visit
        mockMvc.perform(delete("/api/visits/" + visitId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Then - Verify deletion
        mockMvc.perform(get("/api/visits/" + visitId))
                .andExpect(status().isNotFound());
    }
}