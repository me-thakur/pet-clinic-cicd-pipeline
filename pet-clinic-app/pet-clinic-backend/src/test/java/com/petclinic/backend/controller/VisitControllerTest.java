package com.petclinic.backend.controller;

import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.*;
import com.petclinic.backend.service.VisitService;
import com.petclinic.backend.service.VisitService.VisitStatistics;
import com.petclinic.backend.service.impl.VisitServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for VisitController
 * Tests all REST API endpoints for visit management including:
 * - CRUD operations with success and error scenarios
 * - Scheduling operations and conflict detection
 * - Visit completion and data persistence
 * - Search functionality and filtering
 * - Schedule views and analytics
 * - Input validation and error responses
 * - Business rule enforcement
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 */
@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("Visit Controller Tests")
class VisitControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private VisitService visitService;

    @Autowired
    private ObjectMapper objectMapper;

    private Visit testVisit;
    private Visit testVisit2;
    private Pet testPet;
    private Veterinarian testVeterinarian;
    private Owner testOwner;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
        // Setup test owner
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Springfield");
        testOwner.setTelephone("555-1234");

        // Setup test pet
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 15));
        testPet.setOwner(testOwner);

        // Setup test veterinarian
        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("Dr. Jane");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        testVeterinarian.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));

        // Setup test visits
        testVisit = new Visit();
        testVisit.setId(1L);
        testVisit.setPet(testPet);
        testVisit.setVeterinarian(testVeterinarian);
        testVisit.setVisitDate(LocalDateTime.now().plusDays(7));
        testVisit.setVisitType(VisitType.WELLNESS_EXAM);
        testVisit.setDiagnosis("Regular wellness check");
        testVisit.setCost(new BigDecimal("75.00"));

        testVisit2 = new Visit();
        testVisit2.setId(2L);
        testVisit2.setPet(testPet);
        testVisit2.setVeterinarian(testVeterinarian);
        testVisit2.setVisitDate(LocalDateTime.now().plusDays(14));
        testVisit2.setVisitType(VisitType.VACCINATION);
        testVisit2.setDiagnosis("Annual vaccination");
        testVisit2.setCost(new BigDecimal("45.00"));
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits - Should return all visits successfully")
        void getAllVisits_ShouldReturnAllVisits() throws Exception {
            // Given
            List<Visit> visits = Arrays.asList(testVisit, testVisit2);
            when(visitService.findAll()).thenReturn(visits);

            // When & Then
            mockMvc.perform(get("/api/visits"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].visitType").value("WELLNESS_EXAM"))
                    .andExpect(jsonPath("$.content[1].id").value(2))
                    .andExpect(jsonPath("$.content[1].visitType").value("VACCINATION"));

            verify(visitService).findAll();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/{id} - Should return visit when exists")
        void getVisitById_WhenVisitExists_ShouldReturnVisit() throws Exception {
            // Given
            when(visitService.findById(1L)).thenReturn(Optional.of(testVisit));

            // When & Then
            mockMvc.perform(get("/api/visits/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.visitType").value("WELLNESS_EXAM"))
                    .andExpect(jsonPath("$.diagnosis").value("Regular wellness check"))
                    .andExpect(jsonPath("$.cost").value(75.00));

            verify(visitService).findById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/{id} - Should include computed completion status in JSON response")
        void getVisitById_ShouldIncludeCompletionStatusInResponse() throws Exception {
            // Given - Visit with both diagnosis and treatment (should be completed)
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            when(visitService.findById(1L)).thenReturn(Optional.of(completedVisit));

            // When & Then
            mockMvc.perform(get("/api/visits/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diagnosis").value("Healthy pet"))
                    .andExpect(jsonPath("$.treatment").value("Vaccination administered"))
                    .andExpect(jsonPath("$.completed").value(true)); // Verify completion status is included

            verify(visitService).findById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/{id} - Should return false completion status for incomplete visit")
        void getVisitById_ShouldReturnFalseForIncompleteVisit() throws Exception {
            // Given - Visit with only diagnosis (should be incomplete)
            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            incompleteVisit.setVisitType(VisitType.WELLNESS_EXAM);
            incompleteVisit.setDiagnosis("Examination in progress");
            // No treatment set
            incompleteVisit.setCost(new BigDecimal("75.00"));

            when(visitService.findById(2L)).thenReturn(Optional.of(incompleteVisit));

            // When & Then
            mockMvc.perform(get("/api/visits/2"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.diagnosis").value("Examination in progress"))
                    .andExpect(jsonPath("$.completed").value(false)); // Verify completion status is false

            verify(visitService).findById(2L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/{id} - Should return 404 when visit not found")
        void getVisitById_WhenVisitNotExists_ShouldReturn404() throws Exception {
            // Given
            when(visitService.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/visits/999"))
                    .andExpect(status().isNotFound());

            verify(visitService).findById(999L);
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/visits - Should create visit with valid data")
        void createVisit_WithValidData_ShouldReturnCreatedVisit() throws Exception {
            // Given
            Visit newVisit = new Visit();
            newVisit.setPet(testPet);
            newVisit.setVeterinarian(testVeterinarian);
            newVisit.setVisitDate(LocalDateTime.now().plusDays(3));
            newVisit.setVisitType(VisitType.EMERGENCY);
            newVisit.setDiagnosis("Emergency visit");
            newVisit.setCost(new BigDecimal("150.00"));

            Visit createdVisit = new Visit();
            createdVisit.setId(3L);
            createdVisit.setPet(testPet);
            createdVisit.setVeterinarian(testVeterinarian);
            createdVisit.setVisitDate(newVisit.getVisitDate());
            createdVisit.setVisitType(VisitType.EMERGENCY);
            createdVisit.setDiagnosis("Emergency visit");
            createdVisit.setCost(new BigDecimal("150.00"));

            when(visitService.create(any(Visit.class))).thenReturn(createdVisit);

            // When & Then
            mockMvc.perform(post("/api/visits")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newVisit)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.visitType").value("EMERGENCY"))
                    .andExpect(jsonPath("$.diagnosis").value("Emergency visit"))
                    .andExpect(jsonPath("$.cost").value(150.00));

            verify(visitService).create(any(Visit.class));
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/visits - Should return 400 for invalid data")
        void createVisit_WithInvalidData_ShouldReturn400() throws Exception {
            // Given - Visit with missing required fields
            Visit invalidVisit = new Visit();
            // Missing pet, veterinarian, and visit date

            // When & Then
            mockMvc.perform(post("/api/visits")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidVisit)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /api/visits/{id} - Should update visit with valid data")
        void updateVisit_WithValidData_ShouldReturnUpdatedVisit() throws Exception {
            // Given
            Visit updatedVisit = new Visit();
            updatedVisit.setId(1L);
            updatedVisit.setPet(testPet);
            updatedVisit.setVeterinarian(testVeterinarian);
            updatedVisit.setVisitDate(testVisit.getVisitDate());
            updatedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            updatedVisit.setDiagnosis("Updated wellness check");
            updatedVisit.setCost(new BigDecimal("85.00"));

            when(visitService.update(eq(1L), any(Visit.class))).thenReturn(updatedVisit);

            // When & Then
            mockMvc.perform(put("/api/visits/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedVisit)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diagnosis").value("Updated wellness check"))
                    .andExpect(jsonPath("$.cost").value(85.00));

            verify(visitService).update(eq(1L), any(Visit.class));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /api/visits/{id} - Should return updated completion status in response")
        void updateVisit_ShouldReturnUpdatedCompletionStatus() throws Exception {
            // Given - Update visit to include both diagnosis and treatment
            Visit updatedVisit = new Visit();
            updatedVisit.setId(1L);
            updatedVisit.setPet(testPet);
            updatedVisit.setVeterinarian(testVeterinarian);
            updatedVisit.setVisitDate(testVisit.getVisitDate());
            updatedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            updatedVisit.setDiagnosis("Complete wellness check");
            updatedVisit.setTreatment("Vaccination and deworming completed");
            updatedVisit.setCost(new BigDecimal("95.00"));

            when(visitService.update(eq(1L), any(Visit.class))).thenReturn(updatedVisit);

            // When & Then
            mockMvc.perform(put("/api/visits/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedVisit)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diagnosis").value("Complete wellness check"))
                    .andExpect(jsonPath("$.treatment").value("Vaccination and deworming completed"))
                    .andExpect(jsonPath("$.completed").value(true)) // Verify completion status is true
                    .andExpect(jsonPath("$.cost").value(95.00));

            verify(visitService).update(eq(1L), any(Visit.class));
        }

        @Test
        @WithMockUser
        @DisplayName("DELETE /api/visits/{id} - Should delete visit successfully")
        void deleteVisit_WhenVisitExists_ShouldReturn204() throws Exception {
            // Given
            doNothing().when(visitService).deleteById(1L);

            // When & Then
            mockMvc.perform(delete("/api/visits/1")
                    .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(visitService).deleteById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/count - Should return visit count")
        void getVisitCount_ShouldReturnCount() throws Exception {
            // Given
            when(visitService.count()).thenReturn(5L);

            // When & Then
            mockMvc.perform(get("/api/visits/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.count").value(5));

            verify(visitService).count();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits - Should include completion status for all visits in list")
        void getAllVisits_ShouldIncludeCompletionStatusForAllVisits() throws Exception {
            // Given - Mix of completed and incomplete visits
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(14));
            incompleteVisit.setVisitType(VisitType.VACCINATION);
            incompleteVisit.setDiagnosis("Examination in progress");
            // No treatment set - should be incomplete
            incompleteVisit.setCost(new BigDecimal("45.00"));

            List<Visit> visits = Arrays.asList(completedVisit, incompleteVisit);
            when(visitService.findAll()).thenReturn(visits);

            // When & Then
            mockMvc.perform(get("/api/visits"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].completed").value(true))  // Completed visit
                    .andExpect(jsonPath("$.content[1].id").value(2))
                    .andExpect(jsonPath("$.content[1].completed").value(false)); // Incomplete visit

            verify(visitService).findAll();
        }
    }

    @Nested
    @DisplayName("Scheduling Operations Tests")
    class SchedulingOperationsTests {

        @Test
        @WithMockUser
        @DisplayName("POST /api/visits/schedule - Should schedule visit successfully")
        void scheduleVisit_WithValidData_ShouldReturnScheduledVisit() throws Exception {
            // Given
            Visit visitToSchedule = new Visit();
            visitToSchedule.setPet(testPet);
            visitToSchedule.setVeterinarian(testVeterinarian);
            visitToSchedule.setVisitDate(LocalDateTime.now().plusDays(5));
            visitToSchedule.setVisitType(VisitType.WELLNESS_EXAM);

            Visit scheduledVisit = new Visit();
            scheduledVisit.setId(4L);
            scheduledVisit.setPet(testPet);
            scheduledVisit.setVeterinarian(testVeterinarian);
            scheduledVisit.setVisitDate(visitToSchedule.getVisitDate());
            scheduledVisit.setVisitType(VisitType.WELLNESS_EXAM);

            when(visitService.scheduleVisit(any(Visit.class))).thenReturn(scheduledVisit);

            // When & Then
            mockMvc.perform(post("/api/visits/schedule")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(visitToSchedule)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.visitType").value("WELLNESS_EXAM"));

            verify(visitService).scheduleVisit(any(Visit.class));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/conflicts - Should check scheduling conflicts")
        void checkSchedulingConflicts_ShouldReturnConflictStatus() throws Exception {
            // Given
            Long vetId = 1L;
            LocalDateTime dateTime = LocalDateTime.now().plusDays(1);
            int duration = 30;
            
            when(visitService.hasSchedulingConflict(vetId, dateTime, duration)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/visits/conflicts")
                    .param("vetId", vetId.toString())
                    .param("dateTime", dateTime.toString())
                    .param("duration", String.valueOf(duration)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.hasConflict").value(true));

            verify(visitService).hasSchedulingConflict(vetId, dateTime, duration);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/upcoming - Should return upcoming visits")
        void getUpcomingVisits_ShouldReturnUpcomingVisits() throws Exception {
            // Given
            List<Visit> upcomingVisits = Arrays.asList(testVisit, testVisit2);
            when(visitService.getUpcomingVisits(1L, 7)).thenReturn(upcomingVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/upcoming")
                    .param("days", "7")
                    .param("vetId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(visitService).getUpcomingVisits(1L, 7);
        }
    }

    @Nested
    @DisplayName("Visit Completion Tests")
    class VisitCompletionTests {

        @Test
        @WithMockUser
        @DisplayName("PUT /api/visits/{id}/complete - Should complete visit with diagnosis and treatment")
        void completeVisit_WithValidData_ShouldReturnCompletedVisit() throws Exception {
            // Given
            String diagnosis = "Healthy, no issues found";
            String treatment = "Routine vaccination administered";
            String notes = "Pet was cooperative during examination";
            
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(testVisit.getVisitDate());
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis(diagnosis);
            completedVisit.setTreatment(treatment);
            completedVisit.setNotes(notes);

            when(visitService.completeVisit(1L, diagnosis, treatment, notes)).thenReturn(completedVisit);

            Map<String, String> completionData = Map.of(
                "diagnosis", diagnosis,
                "treatment", treatment,
                "notes", notes
            );

            // When & Then
            mockMvc.perform(put("/api/visits/1/complete")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completionData)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.diagnosis").value(diagnosis))
                    .andExpect(jsonPath("$.treatment").value(treatment))
                    .andExpect(jsonPath("$.notes").value(notes));

            verify(visitService).completeVisit(1L, diagnosis, treatment, notes);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/completed - Should return completed visits")
        void getCompletedVisits_ShouldReturnCompletedVisits() throws Exception {
            // Given
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy");
            completedVisit.setTreatment("Vaccination");

            List<Visit> completedVisits = Arrays.asList(completedVisit);
            when(visitService.findCompletedVisits()).thenReturn(completedVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/completed"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].diagnosis").value("Healthy"));

            verify(visitService).findCompletedVisits();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/completed - Should include completion status for completed visits")
        void getCompletedVisits_ShouldIncludeCompletionStatusForCompletedVisits() throws Exception {
            // Given - Completed visits should all have completion status true
            Visit completedVisit1 = new Visit();
            completedVisit1.setId(1L);
            completedVisit1.setPet(testPet);
            completedVisit1.setVeterinarian(testVeterinarian);
            completedVisit1.setVisitDate(LocalDateTime.now().minusDays(1));
            completedVisit1.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit1.setDiagnosis("Healthy pet");
            completedVisit1.setTreatment("Vaccination administered");

            Visit completedVisit2 = new Visit();
            completedVisit2.setId(2L);
            completedVisit2.setPet(testPet);
            completedVisit2.setVeterinarian(testVeterinarian);
            completedVisit2.setVisitDate(LocalDateTime.now().minusDays(7));
            completedVisit2.setVisitType(VisitType.SURGERY);
            completedVisit2.setDiagnosis("Successful surgery");
            completedVisit2.setTreatment("Post-operative care completed");

            List<Visit> completedVisits = Arrays.asList(completedVisit1, completedVisit2);
            when(visitService.findCompletedVisits()).thenReturn(completedVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/completed"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // All completed visits should be true
                    .andExpect(jsonPath("$[1].completed").value(true));

            verify(visitService).findCompletedVisits();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/incomplete - Should include completion status for incomplete visits")
        void getIncompleteVisits_ShouldIncludeCompletionStatusForIncompleteVisits() throws Exception {
            // Given - Mock the service to return all visits, then filter in controller
            Visit incompleteVisit1 = new Visit();
            incompleteVisit1.setId(1L);
            incompleteVisit1.setPet(testPet);
            incompleteVisit1.setVeterinarian(testVeterinarian);
            incompleteVisit1.setVisitDate(LocalDateTime.now().plusDays(1));
            incompleteVisit1.setVisitType(VisitType.WELLNESS_EXAM);
            incompleteVisit1.setDiagnosis("Examination in progress");
            // No treatment set - should be incomplete

            Visit incompleteVisit2 = new Visit();
            incompleteVisit2.setId(2L);
            incompleteVisit2.setPet(testPet);
            incompleteVisit2.setVeterinarian(testVeterinarian);
            incompleteVisit2.setVisitDate(LocalDateTime.now().plusDays(7));
            incompleteVisit2.setVisitType(VisitType.VACCINATION);
            // No diagnosis or treatment set - should be incomplete

            List<Visit> allVisits = Arrays.asList(incompleteVisit1, incompleteVisit2);
            when(visitService.findAll()).thenReturn(allVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/incomplete"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(false))  // All incomplete visits should be false
                    .andExpect(jsonPath("$[1].completed").value(false));

            verify(visitService).findAll();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/upcoming - Should include completion status for upcoming visits")
        void getUpcomingVisits_ShouldIncludeCompletionStatusForUpcomingVisits() throws Exception {
            // Given - Upcoming visits with different completion statuses
            Visit completedUpcomingVisit = new Visit();
            completedUpcomingVisit.setId(1L);
            completedUpcomingVisit.setPet(testPet);
            completedUpcomingVisit.setVeterinarian(testVeterinarian);
            completedUpcomingVisit.setVisitDate(LocalDateTime.now().plusDays(1));
            completedUpcomingVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedUpcomingVisit.setDiagnosis("Pre-scheduled examination completed");
            completedUpcomingVisit.setTreatment("Vaccination administered");

            Visit incompleteUpcomingVisit = new Visit();
            incompleteUpcomingVisit.setId(2L);
            incompleteUpcomingVisit.setPet(testPet);
            incompleteUpcomingVisit.setVeterinarian(testVeterinarian);
            incompleteUpcomingVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            incompleteUpcomingVisit.setVisitType(VisitType.VACCINATION);
            incompleteUpcomingVisit.setDiagnosis("Scheduled for vaccination");
            // No treatment set - should be incomplete

            List<Visit> upcomingVisits = Arrays.asList(completedUpcomingVisit, incompleteUpcomingVisit);
            when(visitService.getUpcomingVisits(1L, 7)).thenReturn(upcomingVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/upcoming")
                    .param("days", "7")
                    .param("vetId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // Completed upcoming visit
                    .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete upcoming visit

            verify(visitService).getUpcomingVisits(1L, 7);
        }
    }

    @Nested
    @DisplayName("Search and Filtering Tests")
    class SearchAndFilteringTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/search - Should search visits by pet ID")
        void searchVisits_ByPetId_ShouldReturnMatchingVisits() throws Exception {
            // Given
            List<Visit> petVisits = Arrays.asList(testVisit, testVisit2);
            when(visitService.findAll()).thenReturn(petVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/search")
                    .param("petId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(visitService).findAll();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/search - Should include completion status in search results")
        void searchVisits_ShouldIncludeCompletionStatusInResults() throws Exception {
            // Given - Mix of completed and incomplete visits
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(14));
            incompleteVisit.setVisitType(VisitType.VACCINATION);
            incompleteVisit.setDiagnosis("Examination in progress");
            // No treatment set - should be incomplete
            incompleteVisit.setCost(new BigDecimal("45.00"));

            List<Visit> searchResults = Arrays.asList(completedVisit, incompleteVisit);
            when(visitService.findAll()).thenReturn(searchResults);

            // When & Then
            mockMvc.perform(get("/api/visits/search")
                    .param("petId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                    .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

            verify(visitService).findAll();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/pet/{petId} - Should return visits for specific pet")
        void getVisitsByPet_ShouldReturnPetVisits() throws Exception {
            // Given
            List<Visit> petVisits = Arrays.asList(testVisit, testVisit2);
            when(visitService.findByPet(1L)).thenReturn(petVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/pet/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(visitService).findByPet(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/pet/{petId} - Should include completion status for pet visits")
        void getVisitsByPet_ShouldIncludeCompletionStatusForPetVisits() throws Exception {
            // Given - Pet visits with different completion statuses
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().minusDays(7));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            incompleteVisit.setVisitType(VisitType.VACCINATION);
            incompleteVisit.setDiagnosis("Scheduled for vaccination");
            // No treatment set - should be incomplete
            incompleteVisit.setCost(new BigDecimal("45.00"));

            List<Visit> petVisits = Arrays.asList(completedVisit, incompleteVisit);
            when(visitService.findByPet(1L)).thenReturn(petVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/pet/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                    .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

            verify(visitService).findByPet(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/veterinarian/{vetId} - Should return visits for specific veterinarian")
        void getVisitsByVeterinarian_ShouldReturnVetVisits() throws Exception {
            // Given
            List<Visit> vetVisits = Arrays.asList(testVisit);
            when(visitService.findByVeterinarian(1L)).thenReturn(vetVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/veterinarian/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(visitService).findByVeterinarian(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/veterinarian/{vetId} - Should include completion status for veterinarian visits")
        void getVisitsByVeterinarian_ShouldIncludeCompletionStatusForVetVisits() throws Exception {
            // Given - Veterinarian visits with different completion statuses
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().minusDays(7));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            List<Visit> vetVisits = Arrays.asList(completedVisit);
            when(visitService.findByVeterinarian(1L)).thenReturn(vetVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/veterinarian/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].completed").value(true)); // Completed visit

            verify(visitService).findByVeterinarian(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/date-range - Should return visits within date range")
        void getVisitsByDateRange_ShouldReturnDateRangeVisits() throws Exception {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(30);
            List<Visit> dateRangeVisits = Arrays.asList(testVisit, testVisit2);
            when(visitService.findByDateRange(startDate, endDate)).thenReturn(dateRangeVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/date-range")
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(visitService).findByDateRange(startDate, endDate);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/date-range - Should include completion status for date range visits")
        void getVisitsByDateRange_ShouldIncludeCompletionStatusForDateRangeVisits() throws Exception {
            // Given - Date range visits with different completion statuses
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().plusDays(7));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(14));
            incompleteVisit.setVisitType(VisitType.VACCINATION);
            incompleteVisit.setDiagnosis("Scheduled for vaccination");
            // No treatment set - should be incomplete
            incompleteVisit.setCost(new BigDecimal("45.00"));

            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(30);
            List<Visit> dateRangeVisits = Arrays.asList(completedVisit, incompleteVisit);
            when(visitService.findByDateRange(startDate, endDate)).thenReturn(dateRangeVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/date-range")
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                    .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

            verify(visitService).findByDateRange(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Schedule View Tests")
    class ScheduleViewTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/schedule/daily - Should return daily schedule")
        void getDailySchedule_ShouldReturnDailyVisits() throws Exception {
            // Given
            LocalDate today = LocalDate.now();
            List<Visit> dailyVisits = Arrays.asList(testVisit);
            when(visitService.findByDate(today)).thenReturn(dailyVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/schedule/daily"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(visitService).findByDate(today);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/schedule/daily - Should include completion status in daily schedule")
        void getDailySchedule_ShouldIncludeCompletionStatusInDailySchedule() throws Exception {
            // Given - Daily visits with different completion statuses
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().withHour(10).withMinute(0));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().withHour(14).withMinute(0));
            incompleteVisit.setVisitType(VisitType.VACCINATION);
            incompleteVisit.setDiagnosis("Scheduled for vaccination");
            // No treatment set - should be incomplete
            incompleteVisit.setCost(new BigDecimal("45.00"));

            LocalDate today = LocalDate.now();
            List<Visit> dailyVisits = Arrays.asList(completedVisit, incompleteVisit);
            when(visitService.findByDate(today)).thenReturn(dailyVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/schedule/daily"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                    .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

            verify(visitService).findByDate(today);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/schedule/weekly - Should return weekly schedule")
        void getWeeklySchedule_ShouldReturnWeeklyVisits() throws Exception {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate weekEnd = weekStart.plusDays(6);
            List<Visit> weeklyVisits = Arrays.asList(testVisit, testVisit2);
            when(visitService.findByDateRange(weekStart, weekEnd)).thenReturn(weeklyVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/schedule/weekly"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(visitService).findByDateRange(weekStart, weekEnd);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/schedule/weekly - Should include completion status in weekly schedule")
        void getWeeklySchedule_ShouldIncludeCompletionStatusInWeeklySchedule() throws Exception {
            // Given - Weekly visits with different completion statuses
            Visit completedVisit = new Visit();
            completedVisit.setId(1L);
            completedVisit.setPet(testPet);
            completedVisit.setVeterinarian(testVeterinarian);
            completedVisit.setVisitDate(LocalDateTime.now().minusDays(2));
            completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
            completedVisit.setDiagnosis("Healthy pet");
            completedVisit.setTreatment("Vaccination administered");
            completedVisit.setCost(new BigDecimal("75.00"));

            Visit incompleteVisit = new Visit();
            incompleteVisit.setId(2L);
            incompleteVisit.setPet(testPet);
            incompleteVisit.setVeterinarian(testVeterinarian);
            incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(2));
            incompleteVisit.setVisitType(VisitType.VACCINATION);
            incompleteVisit.setDiagnosis("Scheduled for vaccination");
            // No treatment set - should be incomplete
            incompleteVisit.setCost(new BigDecimal("45.00"));

            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate weekEnd = weekStart.plusDays(6);
            List<Visit> weeklyVisits = Arrays.asList(completedVisit, incompleteVisit);
            when(visitService.findByDateRange(weekStart, weekEnd)).thenReturn(weeklyVisits);

            // When & Then
            mockMvc.perform(get("/api/visits/schedule/weekly"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                    .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

            verify(visitService).findByDateRange(weekStart, weekEnd);
        }
    }

    @Nested
    @DisplayName("Statistics and Analytics Tests")
    class StatisticsAndAnalyticsTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/statistics - Should return visit statistics")
        void getVisitStatistics_ShouldReturnStatistics() throws Exception {
            // Given
            VisitStatistics statistics = new VisitStatistics(
                10L, 8L, 2L, new BigDecimal("750.00"), 75.0
            );
            VisitServiceImpl visitServiceImpl = mock(VisitServiceImpl.class);
            when(visitServiceImpl.getVisitStatistics()).thenReturn(statistics);

            // When & Then
            mockMvc.perform(get("/api/visits/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalVisits").value(10))
                    .andExpect(jsonPath("$.completedVisits").value(8))
                    .andExpect(jsonPath("$.scheduledVisits").value(2))
                    .andExpect(jsonPath("$.totalRevenue").value(750.00))
                    .andExpect(jsonPath("$.averageCost").value(75.0));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/revenue - Should return revenue statistics")
        void getRevenueStatistics_ShouldReturnRevenueStats() throws Exception {
            // Given
            VisitStatistics statistics = new VisitStatistics(
                10L, 8L, 2L, new BigDecimal("750.00"), 75.0
            );
            VisitServiceImpl visitServiceImpl = mock(VisitServiceImpl.class);
            when(visitServiceImpl.getVisitStatistics()).thenReturn(statistics);

            // When & Then
            mockMvc.perform(get("/api/visits/revenue"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalRevenue").value(750.00))
                    .andExpect(jsonPath("$.averageCost").value(75.0))
                    .andExpect(jsonPath("$.totalVisits").value(10))
                    .andExpect(jsonPath("$.completedVisits").value(8));
        }
    }

    @Nested
    @DisplayName("Utility Endpoints Tests")
    class UtilityEndpointsTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/{id}/exists - Should check if visit exists")
        void checkVisitExists_WhenVisitExists_ShouldReturnTrue() throws Exception {
            // Given
            when(visitService.existsById(1L)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/visits/1/exists"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.exists").value(true));

            verify(visitService).existsById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/visits/health - Should return health status")
        void healthCheck_ShouldReturnHealthStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/visits/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("VisitController"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @WithMockUser
        @DisplayName("POST /api/visits/schedule - Should return 409 for scheduling conflict")
        void scheduleVisit_WithConflict_ShouldReturn409() throws Exception {
            // Given
            Visit conflictingVisit = new Visit();
            conflictingVisit.setPet(testPet);
            conflictingVisit.setVeterinarian(testVeterinarian);
            conflictingVisit.setVisitDate(LocalDateTime.now().plusDays(1));
            conflictingVisit.setVisitType(VisitType.WELLNESS_EXAM);

            when(visitService.scheduleVisit(any(Visit.class)))
                .thenThrow(new PetClinicException("Scheduling conflict", "SCHEDULING_CONFLICT", HttpStatus.CONFLICT));

            // When & Then
            mockMvc.perform(post("/api/visits/schedule")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(conflictingVisit)))
                    .andExpect(status().isConflict());

            verify(visitService).scheduleVisit(any(Visit.class));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /api/visits/{id}/complete - Should return 404 for non-existent visit")
        void completeVisit_WhenVisitNotFound_ShouldReturn404() throws Exception {
            // Given
            when(visitService.completeVisit(eq(999L), anyString(), anyString(), anyString()))
                .thenThrow(new PetClinicException("Visit not found", "VISIT_NOT_FOUND", HttpStatus.NOT_FOUND));

            Map<String, String> completionData = Map.of("diagnosis", "Test diagnosis");

            // When & Then
            mockMvc.perform(put("/api/visits/999/complete")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completionData)))
                    .andExpect(status().isNotFound());

            verify(visitService).completeVisit(eq(999L), anyString(), isNull(), isNull());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/visits - Should return 422 for business rule violation")
        void createVisit_WithBusinessRuleViolation_ShouldReturn422() throws Exception {
            // Given
            Visit invalidVisit = new Visit();
            invalidVisit.setPet(testPet);
            invalidVisit.setVeterinarian(testVeterinarian);
            invalidVisit.setVisitDate(LocalDateTime.now().minusDays(1)); // Past date
            invalidVisit.setVisitType(VisitType.WELLNESS_EXAM);

            when(visitService.create(any(Visit.class)))
                .thenThrow(new PetClinicException("Cannot schedule visit in the past", "INVALID_VISIT_DATE", HttpStatus.UNPROCESSABLE_ENTITY));

            // When & Then
            mockMvc.perform(post("/api/visits")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidVisit)))
                    .andExpect(status().isUnprocessableEntity());

            verify(visitService).create(any(Visit.class));
        }
    }
}