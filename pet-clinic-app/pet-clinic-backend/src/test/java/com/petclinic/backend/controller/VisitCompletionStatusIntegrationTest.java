package com.petclinic.backend.controller;

import com.petclinic.backend.model.*;
import com.petclinic.backend.service.VisitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests specifically for verifying that all GET endpoints include completion status
 * **Validates: Requirements 3.2, 3.3**
 */
@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("Visit Completion Status Integration Tests")
class VisitCompletionStatusIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private VisitService visitService;

    @Autowired
    private ObjectMapper objectMapper;

    private Visit completedVisit;
    private Visit incompleteVisit;
    private Pet testPet;
    private Veterinarian testVeterinarian;
    private Owner testOwner;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        // Setup test data
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");
        testOwner.setAddress("123 Main St");
        testOwner.setCity("Springfield");
        testOwner.setTelephone("555-1234");

        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 15));
        testPet.setOwner(testOwner);

        testVeterinarian = new Veterinarian();
        testVeterinarian.setId(1L);
        testVeterinarian.setFirstName("Dr. Jane");
        testVeterinarian.setLastName("Smith");
        testVeterinarian.setLicenseNumber("VET123456");
        testVeterinarian.setSpecialtySet(Set.of(Specialty.GENERAL_PRACTICE));

        // Completed visit - has both diagnosis and treatment
        completedVisit = new Visit();
        completedVisit.setId(1L);
        completedVisit.setPet(testPet);
        completedVisit.setVeterinarian(testVeterinarian);
        completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
        completedVisit.setDiagnosis("Healthy pet - no issues found");
        completedVisit.setTreatment("Vaccination administered");
        completedVisit.setCost(new BigDecimal("75.00"));

        // Incomplete visit - has diagnosis but no treatment
        incompleteVisit = new Visit();
        incompleteVisit.setId(2L);
        incompleteVisit.setPet(testPet);
        incompleteVisit.setVeterinarian(testVeterinarian);
        incompleteVisit.setVisitDate(LocalDateTime.now().plusDays(1));
        incompleteVisit.setVisitType(VisitType.VACCINATION);
        incompleteVisit.setDiagnosis("Scheduled for vaccination");
        // No treatment set - should be incomplete
        incompleteVisit.setCost(new BigDecimal("45.00"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/{id} - Should include completion status for completed visit")
    void getVisitById_CompletedVisit_ShouldIncludeCompletionStatus() throws Exception {
        // Given
        when(visitService.findById(1L)).thenReturn(Optional.of(completedVisit));

        // When & Then
        mockMvc.perform(get("/api/visits/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.diagnosis").value("Healthy pet - no issues found"))
                .andExpect(jsonPath("$.treatment").value("Vaccination administered"))
                .andExpect(jsonPath("$.completed").value(true)); // Verify completion status is true

        verify(visitService).findById(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/{id} - Should include completion status for incomplete visit")
    void getVisitById_IncompleteVisit_ShouldIncludeCompletionStatus() throws Exception {
        // Given
        when(visitService.findById(2L)).thenReturn(Optional.of(incompleteVisit));

        // When & Then
        mockMvc.perform(get("/api/visits/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.diagnosis").value("Scheduled for vaccination"))
                .andExpect(jsonPath("$.treatment").doesNotExist())
                .andExpect(jsonPath("$.completed").value(false)); // Verify completion status is false

        verify(visitService).findById(2L);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/pet/{petId} - Should include completion status for pet visits")
    void getVisitsByPet_ShouldIncludeCompletionStatusForAllVisits() throws Exception {
        // Given
        List<Visit> petVisits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitService.findByPet(1L)).thenReturn(petVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/pet/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

        verify(visitService).findByPet(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/veterinarian/{vetId} - Should include completion status for veterinarian visits")
    void getVisitsByVeterinarian_ShouldIncludeCompletionStatusForAllVisits() throws Exception {
        // Given
        List<Visit> vetVisits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitService.findByVeterinarian(1L)).thenReturn(vetVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/veterinarian/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

        verify(visitService).findByVeterinarian(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/date-range - Should include completion status for date range visits")
    void getVisitsByDateRange_ShouldIncludeCompletionStatusForAllVisits() throws Exception {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now().plusDays(7);
        List<Visit> dateRangeVisits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitService.findByDateRange(startDate, endDate)).thenReturn(dateRangeVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

        verify(visitService).findByDateRange(startDate, endDate);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search - Should include completion status in search results")
    void searchVisits_ShouldIncludeCompletionStatusInResults() throws Exception {
        // Given
        List<Visit> searchResults = Arrays.asList(completedVisit, incompleteVisit);
        when(visitService.findAll()).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/visits/search")
                .param("petId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

        verify(visitService).findAll();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/completed - Should include completion status for completed visits")
    void getCompletedVisits_ShouldIncludeCompletionStatusForAllVisits() throws Exception {
        // Given - Only completed visits should be returned
        List<Visit> completedVisits = Arrays.asList(completedVisit);
        when(visitService.findCompletedVisits()).thenReturn(completedVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/completed"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].completed").value(true)); // All should be completed

        verify(visitService).findCompletedVisits();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/schedule/daily - Should include completion status in daily schedule")
    void getDailySchedule_ShouldIncludeCompletionStatusForAllVisits() throws Exception {
        // Given
        LocalDate today = LocalDate.now();
        List<Visit> dailyVisits = Arrays.asList(completedVisit, incompleteVisit);
        when(visitService.findByDate(today)).thenReturn(dailyVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/schedule/daily"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].completed").value(true))  // Completed visit
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].completed").value(false)); // Incomplete visit

        verify(visitService).findByDate(today);
    }

    @Test
    @WithMockUser
    @DisplayName("Verify completion logic - Visit with whitespace-only treatment should be incomplete")
    void getVisitById_WithWhitespaceOnlyTreatment_ShouldBeIncomplete() throws Exception {
        // Given - Visit with diagnosis but whitespace-only treatment
        Visit whitespaceVisit = new Visit();
        whitespaceVisit.setId(3L);
        whitespaceVisit.setPet(testPet);
        whitespaceVisit.setVeterinarian(testVeterinarian);
        whitespaceVisit.setVisitDate(LocalDateTime.now());
        whitespaceVisit.setVisitType(VisitType.WELLNESS_EXAM);
        whitespaceVisit.setDiagnosis("Valid diagnosis");
        whitespaceVisit.setTreatment("   "); // Whitespace-only treatment
        whitespaceVisit.setCost(new BigDecimal("50.00"));

        when(visitService.findById(3L)).thenReturn(Optional.of(whitespaceVisit));

        // When & Then
        mockMvc.perform(get("/api/visits/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.diagnosis").value("Valid diagnosis"))
                .andExpect(jsonPath("$.treatment").value("   "))
                .andExpect(jsonPath("$.completed").value(false)); // Should be false due to whitespace-only treatment

        verify(visitService).findById(3L);
    }

    @Test
    @WithMockUser
    @DisplayName("Verify completion logic - Visit with empty diagnosis should be incomplete")
    void getVisitById_WithEmptyDiagnosis_ShouldBeIncomplete() throws Exception {
        // Given - Visit with treatment but empty diagnosis
        Visit emptyDiagnosisVisit = new Visit();
        emptyDiagnosisVisit.setId(4L);
        emptyDiagnosisVisit.setPet(testPet);
        emptyDiagnosisVisit.setVeterinarian(testVeterinarian);
        emptyDiagnosisVisit.setVisitDate(LocalDateTime.now());
        emptyDiagnosisVisit.setVisitType(VisitType.WELLNESS_EXAM);
        emptyDiagnosisVisit.setDiagnosis(""); // Empty diagnosis
        emptyDiagnosisVisit.setTreatment("Valid treatment");
        emptyDiagnosisVisit.setCost(new BigDecimal("50.00"));

        when(visitService.findById(4L)).thenReturn(Optional.of(emptyDiagnosisVisit));

        // When & Then
        mockMvc.perform(get("/api/visits/4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.diagnosis").value(""))
                .andExpect(jsonPath("$.treatment").value("Valid treatment"))
                .andExpect(jsonPath("$.completed").value(false)); // Should be false due to empty diagnosis

        verify(visitService).findById(4L);
    }
}