package com.petclinic.backend.controller;

import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.service.VisitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the new visit search endpoints
 * Tests the implementation of Task 7.1: Add missing visit search endpoints and filters
 */
@WebMvcTest(VisitController.class)
class VisitSearchEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitService visitService;

    @Autowired
    private ObjectMapper objectMapper;

    private Visit completedVisit;
    private Visit pendingVisit;
    private Visit emergencyVisit;
    private Pet testPet;
    private Veterinarian testVet;

    @BeforeEach
    void setUp() {
        // Create test pet
        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");

        // Create test veterinarian
        testVet = new Veterinarian();
        testVet.setId(1L);
        testVet.setFirstName("Dr. John");
        testVet.setLastName("Smith");

        // Create completed visit
        completedVisit = new Visit();
        completedVisit.setId(1L);
        completedVisit.setVisitDate(LocalDateTime.now().minusDays(1));
        completedVisit.setVisitType(VisitType.WELLNESS_EXAM);
        completedVisit.setDiagnosis("Healthy");
        completedVisit.setTreatment("Routine checkup");
        completedVisit.setCost(new BigDecimal("75.00"));
        completedVisit.setPet(testPet);
        completedVisit.setVeterinarian(testVet);

        // Create pending visit
        pendingVisit = new Visit();
        pendingVisit.setId(2L);
        pendingVisit.setVisitDate(LocalDateTime.now().plusDays(1));
        pendingVisit.setVisitType(VisitType.VACCINATION);
        pendingVisit.setPet(testPet);
        pendingVisit.setVeterinarian(testVet);
        // No diagnosis/treatment = pending

        // Create emergency visit
        emergencyVisit = new Visit();
        emergencyVisit.setId(3L);
        emergencyVisit.setVisitDate(LocalDateTime.now());
        emergencyVisit.setVisitType(VisitType.EMERGENCY);
        emergencyVisit.setDiagnosis("Emergency condition");
        emergencyVisit.setTreatment("Emergency treatment");
        emergencyVisit.setCost(new BigDecimal("200.00"));
        emergencyVisit.setPet(testPet);
        emergencyVisit.setVeterinarian(testVet);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-pet - Should return visits for specific pet with result highlighting")
    void searchByPet_ShouldReturnVisitsWithHighlighting() throws Exception {
        // Given
        List<Visit> petVisits = Arrays.asList(completedVisit, pendingVisit);
        when(visitService.findByPet(1L)).thenReturn(petVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/by-pet")
                .param("petId", "1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.searchCriteria.petId").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-veterinarian - Should return visits for specific veterinarian with result highlighting")
    void searchByVeterinarian_ShouldReturnVisitsWithHighlighting() throws Exception {
        // Given
        List<Visit> vetVisits = Arrays.asList(completedVisit, emergencyVisit);
        when(visitService.findByVeterinarian(1L)).thenReturn(vetVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/by-veterinarian")
                .param("veterinarianId", "1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.completedCount").value(2))
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.visitTypeCounts").exists())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.searchCriteria.veterinarianId").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-status - Should return visits filtered by completed status")
    void searchByStatus_Completed_ShouldReturnCompletedVisits() throws Exception {
        // Given
        List<Visit> allVisits = Arrays.asList(completedVisit, pendingVisit, emergencyVisit);
        when(visitService.findAll()).thenReturn(allVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/by-status")
                .param("status", "completed")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(2)) // completedVisit and emergencyVisit are completed
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.statusBreakdown").exists())
                .andExpect(jsonPath("$.searchCriteria.status").value("completed"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-status - Should return visits filtered by pending status")
    void searchByStatus_Pending_ShouldReturnPendingVisits() throws Exception {
        // Given
        List<Visit> allVisits = Arrays.asList(completedVisit, pendingVisit, emergencyVisit);
        when(visitService.findAll()).thenReturn(allVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/by-status")
                .param("status", "pending")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(1)) // Only pendingVisit
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.searchCriteria.status").value("pending"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-status - Should return visits filtered by emergency status")
    void searchByStatus_Emergency_ShouldReturnEmergencyVisits() throws Exception {
        // Given
        List<Visit> allVisits = Arrays.asList(completedVisit, pendingVisit, emergencyVisit);
        when(visitService.findAll()).thenReturn(allVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/by-status")
                .param("status", "emergency")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(1)) // Only emergencyVisit
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.status").value("emergency"))
                .andExpect(jsonPath("$.searchCriteria.status").value("emergency"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/combined - Should return visits matching multiple criteria")
    void searchCombined_ShouldReturnFilteredVisits() throws Exception {
        // Given
        List<Visit> allVisits = Arrays.asList(completedVisit, pendingVisit, emergencyVisit);
        when(visitService.findAll()).thenReturn(allVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/combined")
                .param("petId", "1")
                .param("veterinarianId", "1")
                .param("status", "completed")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.totalCount").exists())
                .andExpect(jsonPath("$.completedCount").exists())
                .andExpect(jsonPath("$.pendingCount").exists())
                .andExpect(jsonPath("$.visitTypeCounts").exists())
                .andExpect(jsonPath("$.veterinarianCounts").exists())
                .andExpect(jsonPath("$.activeFilters").exists())
                .andExpect(jsonPath("$.activeFilters.petId").value(1))
                .andExpect(jsonPath("$.activeFilters.veterinarianId").value(1))
                .andExpect(jsonPath("$.activeFilters.status").value("completed"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-status - Should return bad request for invalid status")
    void searchByStatus_InvalidStatus_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/visits/search/by-status")
                .param("status", "invalid_status")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/by-pet - Should handle empty results gracefully")
    void searchByPet_EmptyResults_ShouldReturnEmptyResponse() throws Exception {
        // Given
        when(visitService.findByPet(999L)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/visits/search/by-pet")
                .param("petId", "999")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(0))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.completedCount").value(0))
                .andExpect(jsonPath("$.pendingCount").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/visits/search/combined - Should handle no filters gracefully")
    void searchCombined_NoFilters_ShouldReturnAllVisits() throws Exception {
        // Given
        List<Visit> allVisits = Arrays.asList(completedVisit, pendingVisit, emergencyVisit);
        when(visitService.findAll()).thenReturn(allVisits);

        // When & Then
        mockMvc.perform(get("/api/visits/search/combined")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visits").isArray())
                .andExpect(jsonPath("$.visits.length()").value(3))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.activeFilters").exists());
    }
}