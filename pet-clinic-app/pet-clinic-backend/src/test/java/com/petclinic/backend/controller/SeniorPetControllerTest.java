package com.petclinic.backend.controller;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.dto.SeniorPetInfo;
import com.petclinic.backend.service.SeniorPetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SeniorPetController
 * Tests all endpoints for senior pet search functionality
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@WebMvcTest(SeniorPetController.class)
class SeniorPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeniorPetService seniorPetService;

    @Autowired
    private ObjectMapper objectMapper;

    private SeniorPetInfo testSeniorPet1;
    private SeniorPetInfo testSeniorPet2;
    private List<SeniorPetInfo> testSeniorPets;

    @BeforeEach
    void setUp() {
        // Create test senior pet data
        testSeniorPet1 = new SeniorPetInfo();
        testSeniorPet1.setPetId(1L);
        testSeniorPet1.setPetName("Max");
        testSeniorPet1.setSpecies("Dog");
        testSeniorPet1.setBreed("Labrador");
        testSeniorPet1.setBirthDate(LocalDate.now().minusYears(10));
        testSeniorPet1.setAge(10);
        testSeniorPet1.setIsSenior(true);
        testSeniorPet1.setSeniorThreshold(7);
        testSeniorPet1.setOwnerId(1L);
        testSeniorPet1.setOwnerName("John Smith");
        testSeniorPet1.setMedicalHistory("Arthritis, regular checkups needed");
        testSeniorPet1.setHealthConditions(Arrays.asList("Arthritis"));
        testSeniorPet1.setSpecialCareNeeded(true);
        testSeniorPet1.setAgeHighlighted(false);
        testSeniorPet1.setHealthHighlighted(false);

        testSeniorPet2 = new SeniorPetInfo();
        testSeniorPet2.setPetId(2L);
        testSeniorPet2.setPetName("Whiskers");
        testSeniorPet2.setSpecies("Cat");
        testSeniorPet2.setBreed("Persian");
        testSeniorPet2.setBirthDate(LocalDate.now().minusYears(12));
        testSeniorPet2.setAge(12);
        testSeniorPet2.setIsSenior(true);
        testSeniorPet2.setSeniorThreshold(7);
        testSeniorPet2.setOwnerId(2L);
        testSeniorPet2.setOwnerName("Jane Doe");
        testSeniorPet2.setMedicalHistory("Diabetes, kidney issues");
        testSeniorPet2.setHealthConditions(Arrays.asList("Diabetes", "Kidney Disease"));
        testSeniorPet2.setSpecialCareNeeded(true);
        testSeniorPet2.setAgeHighlighted(false);
        testSeniorPet2.setHealthHighlighted(false);

        testSeniorPets = Arrays.asList(testSeniorPet1, testSeniorPet2);
    }

    // ========================================
    // Search Endpoint Tests - Requirement 2.1
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/search - Should return senior pets with search criteria")
    void searchSeniorPets_WithCriteria_ShouldReturnFilteredResults() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(testSeniorPets, pageable, testSeniorPets.size());
        
        when(seniorPetService.searchSeniorPets(any(), any(Pageable.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/search")
                .param("minAge", "8")
                .param("maxAge", "15")
                .param("species", "Dog")
                .param("healthCondition", "arthritis")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].petName").value("Max"))
                .andExpect(jsonPath("$.content[0].isSenior").value(true))
                .andExpect(jsonPath("$.content[0].seniorThreshold").value(7))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        verify(seniorPetService).searchSeniorPets(any(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/search - Should handle search failure with fallback")
    void searchSeniorPets_WithError_ShouldReturnFallback() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> fallbackPage = new PageImpl<>(testSeniorPets, pageable, testSeniorPets.size());
        
        when(seniorPetService.searchSeniorPets(any(), any(Pageable.class)))
            .thenThrow(new RuntimeException("Search service unavailable"));
        when(seniorPetService.getFallbackSeniorPets(any(Pageable.class)))
            .thenReturn(fallbackPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/search")
                .param("species", "Dog")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.errorMessage").value("Search temporarily unavailable. Showing basic senior pets list sorted by age."))
                .andExpect(jsonPath("$.retryable").value(true));

        verify(seniorPetService).searchSeniorPets(any(), any(Pageable.class));
        verify(seniorPetService).getFallbackSeniorPets(any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/search - Should validate age parameters")
    void searchSeniorPets_WithInvalidAge_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/pets/senior/search")
                .param("minAge", "-1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior - Should return all senior pets")
    void getAllSeniorPets_ShouldReturnAllSeniorPets() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(testSeniorPets, pageable, testSeniorPets.size());
        
        when(seniorPetService.getAllSeniorPets(any(Pageable.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        verify(seniorPetService).getAllSeniorPets(any(Pageable.class));
    }

    // ========================================
    // Species-Specific Tests - Requirement 2.2
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/species/{species} - Should return senior pets by species")
    void getSeniorPetsBySpecies_ShouldReturnSpeciesSpecificResults() throws Exception {
        // Given
        List<SeniorPetInfo> dogSeniorPets = Arrays.asList(testSeniorPet1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(dogSeniorPets, pageable, dogSeniorPets.size());
        
        when(seniorPetService.getSeniorPetsBySpecies(eq("Dog"), any(Pageable.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/species/Dog")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].species").value("Dog"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(seniorPetService).getSeniorPetsBySpecies("Dog", pageable);
    }

    // ========================================
    // Health Condition Tests - Requirement 2.1
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/health-condition - Should return pets with health condition")
    void getSeniorPetsWithHealthCondition_ShouldReturnFilteredResults() throws Exception {
        // Given
        List<SeniorPetInfo> petsWithArthritis = Arrays.asList(testSeniorPet1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(petsWithArthritis, pageable, petsWithArthritis.size());
        
        when(seniorPetService.getSeniorPetsWithHealthCondition(eq("arthritis"), any(Pageable.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/health-condition")
                .param("condition", "arthritis")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].healthConditions").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(seniorPetService).getSeniorPetsWithHealthCondition("arthritis", pageable);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/special-care - Should return pets needing special care")
    void getSeniorPetsNeedingSpecialCare_ShouldReturnSpecialCarePets() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(testSeniorPets, pageable, testSeniorPets.size());
        
        when(seniorPetService.getSeniorPetsNeedingSpecialCare(any(Pageable.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/special-care")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        verify(seniorPetService).getSeniorPetsNeedingSpecialCare(any(Pageable.class));
    }

    // ========================================
    // Configuration Tests - Requirement 2.2
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/age-thresholds - Should return age thresholds")
    void getSpeciesAgeThresholds_ShouldReturnThresholds() throws Exception {
        // Given
        Map<String, Integer> thresholds = Map.of(
            "Dog", 7,
            "Cat", 7,
            "Bird", 5,
            "Rabbit", 5
        );
        
        when(seniorPetService.getSpeciesAgeThresholds()).thenReturn(thresholds);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/age-thresholds"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Dog").value(7))
                .andExpect(jsonPath("$.Cat").value(7))
                .andExpect(jsonPath("$.Bird").value(5))
                .andExpect(jsonPath("$.Rabbit").value(5));

        verify(seniorPetService).getSpeciesAgeThresholds();
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/pets/senior/age-thresholds/{species} - Should update age threshold")
    void updateSpeciesAgeThreshold_ShouldUpdateThreshold() throws Exception {
        // Given
        Map<String, Integer> updatedThresholds = Map.of(
            "Dog", 8,
            "Cat", 7,
            "Bird", 5,
            "Rabbit", 5
        );
        
        doNothing().when(seniorPetService).updateSpeciesAgeThreshold("Dog", 8);
        when(seniorPetService.getSpeciesAgeThresholds()).thenReturn(updatedThresholds);

        // When & Then
        mockMvc.perform(put("/api/pets/senior/age-thresholds/Dog")
                .param("threshold", "8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Dog").value(8));

        verify(seniorPetService).updateSpeciesAgeThreshold("Dog", 8);
        verify(seniorPetService).getSpeciesAgeThresholds();
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/pets/senior/age-thresholds/{species} - Should validate threshold parameter")
    void updateSpeciesAgeThreshold_WithInvalidThreshold_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/pets/senior/age-thresholds/Dog")
                .param("threshold", "0"))
                .andExpect(status().isBadRequest());
    }

    // ========================================
    // Statistics Tests
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/statistics - Should return statistics")
    void getSeniorPetStatistics_ShouldReturnStatistics() throws Exception {
        // Given
        Map<String, Object> statistics = Map.of(
            "totalSeniorPets", 25L,
            "totalPets", 100L,
            "seniorPercentage", 25.0,
            "speciesBreakdown", Map.of("Dog", 15L, "Cat", 10L)
        );
        
        when(seniorPetService.getSeniorPetStatistics()).thenReturn(statistics);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalSeniorPets").value(25))
                .andExpect(jsonPath("$.totalPets").value(100))
                .andExpect(jsonPath("$.seniorPercentage").value(25.0));

        verify(seniorPetService).getSeniorPetStatistics();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/health-conditions - Should return available health conditions")
    void getAvailableHealthConditions_ShouldReturnConditions() throws Exception {
        // Given
        List<String> conditions = Arrays.asList("Arthritis", "Diabetes", "Heart Disease", "Kidney Disease");
        
        when(seniorPetService.getAvailableHealthConditions()).thenReturn(conditions);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/health-conditions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("Arthritis"));

        verify(seniorPetService).getAvailableHealthConditions();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/species - Should return available species")
    void getAvailableSpecies_ShouldReturnSpecies() throws Exception {
        // Given
        List<String> species = Arrays.asList("Dog", "Cat", "Bird", "Rabbit");
        
        when(seniorPetService.getAvailableSpecies()).thenReturn(species);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/species"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("Dog"));

        verify(seniorPetService).getAvailableSpecies();
    }

    // ========================================
    // Utility Tests
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/check/{petId} - Should check if pet is senior")
    void checkIfPetIsSenior_ShouldReturnSeniorStatus() throws Exception {
        // Given
        when(seniorPetService.isPetSenior(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/check/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.petId").value(1))
                .andExpect(jsonPath("$.isSenior").value(true));

        verify(seniorPetService).isPetSenior(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/health - Should return health status")
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/pets/senior/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("SeniorPetController"));
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior - Should handle service errors gracefully")
    void getAllSeniorPets_WithServiceError_ShouldReturnFallback() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> fallbackPage = new PageImpl<>(testSeniorPets, pageable, testSeniorPets.size());
        
        when(seniorPetService.getAllSeniorPets(any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));
        when(seniorPetService.getFallbackSeniorPets(any(Pageable.class)))
            .thenReturn(fallbackPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.errorMessage").value("Error loading senior pets. Showing fallback list."));

        verify(seniorPetService).getAllSeniorPets(any(Pageable.class));
        verify(seniorPetService).getFallbackSeniorPets(any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/pets/senior/statistics - Should handle statistics error gracefully")
    void getSeniorPetStatistics_WithError_ShouldReturnFallback() throws Exception {
        // Given
        when(seniorPetService.getSeniorPetStatistics())
            .thenThrow(new RuntimeException("Statistics service error"));

        // When & Then
        mockMvc.perform(get("/api/pets/senior/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalSeniorPets").value(0))
                .andExpect(jsonPath("$.totalPets").value(0))
                .andExpect(jsonPath("$.seniorPercentage").value(0.0))
                .andExpect(jsonPath("$.error").value("Statistics temporarily unavailable"));

        verify(seniorPetService).getSeniorPetStatistics();
    }
}