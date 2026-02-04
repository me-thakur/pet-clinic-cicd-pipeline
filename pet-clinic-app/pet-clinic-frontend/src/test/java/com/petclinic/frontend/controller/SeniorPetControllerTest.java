package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.service.PetService;
import com.petclinic.frontend.service.OwnerService;
import com.petclinic.frontend.service.SeniorPetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Senior Pet functionality in PetController
 * Tests the senior pets search page and related endpoints
 * Validates: Requirements 2.1, 2.3, 2.4
 */
@ExtendWith(MockitoExtension.class)
class SeniorPetControllerTest {

    @Mock
    private PetService petService;

    @Mock
    private OwnerService ownerService;

    @Mock
    private SeniorPetService seniorPetService;

    @Mock
    private Model model;

    private PetController petController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        petController = new PetController(petService, ownerService, seniorPetService);
        mockMvc = MockMvcBuilders.standaloneSetup(petController).build();
    }

    @Test
    @DisplayName("Should display senior pets page with default parameters")
    void seniorPets_WithDefaultParameters_ShouldReturnSeniorPetsView() {
        // Given
        List<Pet> mockSeniorPets = createMockSeniorPets();
        when(petService.getSeniorPets(7)).thenReturn(Mono.just(mockSeniorPets));

        // When
        String viewName = petController.seniorPets(null, null, null, null, null, null, null, 
                                                  "age", "desc", 0, 10, model);

        // Then
        assertEquals("pets/senior", viewName);
        verify(model).addAttribute("pets", mockSeniorPets);
        verify(model).addAttribute("seniorAge", 7);
        verify(model).addAttribute("filterBySenior", true);
        verify(model).addAttribute("searchPerformed", false);
    }

    @Test
    @DisplayName("Should display senior pets page with search criteria")
    void seniorPets_WithSearchCriteria_ShouldReturnFilteredResults() {
        // Given
        List<Pet> mockSeniorPets = createMockSeniorPets();
        when(petService.getSeniorPets(8)).thenReturn(Mono.just(mockSeniorPets));

        // When
        String viewName = petController.seniorPets(8, 15, "Dog", "Labrador", "Arthritis", 
                                                  "John Smith", "Max", "age", "desc", 0, 10, model);

        // Then
        assertEquals("pets/senior", viewName);
        verify(model).addAttribute("pets", mockSeniorPets);
        verify(model).addAttribute("seniorAge", 8);
        verify(model).addAttribute("searchPerformed", true);
        verify(model).addAttribute("searchMinAge", 8);
        verify(model).addAttribute("searchMaxAge", 15);
        verify(model).addAttribute("searchSpecies", "Dog");
        verify(model).addAttribute("searchBreed", "Labrador");
        verify(model).addAttribute("searchHealthCondition", "Arthritis");
        verify(model).addAttribute("searchOwnerName", "John Smith");
        verify(model).addAttribute("searchTerm", "Max");
    }

    @Test
    @DisplayName("Should handle service errors gracefully")
    void seniorPets_WithServiceError_ShouldReturnErrorView() {
        // Given
        when(petService.getSeniorPets(anyInt())).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        String viewName = petController.seniorPets(null, null, null, null, null, null, null, 
                                                  "age", "desc", 0, 10, model);

        // Then
        assertEquals("pets/senior", viewName);
        verify(model).addAttribute(eq("error"), contains("Error loading senior pets"));
    }

    @Test
    @DisplayName("Should return available species list")
    void getAvailableSpecies_ShouldReturnSpeciesList() throws Exception {
        // When & Then
        mockMvc.perform(get("/pets/senior/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("Dog"))
                .andExpect(jsonPath("$[1]").value("Cat"))
                .andExpect(jsonPath("$[2]").value("Bird"))
                .andExpect(jsonPath("$[3]").value("Rabbit"));
    }

    @Test
    @DisplayName("Should return available health conditions list")
    void getAvailableHealthConditions_ShouldReturnConditionsList() throws Exception {
        // When & Then
        mockMvc.perform(get("/pets/senior/health-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("Arthritis"))
                .andExpect(jsonPath("$[1]").value("Diabetes"))
                .andExpect(jsonPath("$[2]").value("Heart Disease"))
                .andExpect(jsonPath("$[3]").value("Kidney Disease"));
    }

    @Test
    @DisplayName("Should return senior pet statistics")
    void getSeniorPetStatistics_ShouldReturnStatistics() throws Exception {
        // When & Then
        mockMvc.perform(get("/pets/senior/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeniorPets").value(45))
                .andExpect(jsonPath("$.totalPets").value(120))
                .andExpect(jsonPath("$.seniorPercentage").value(37.5))
                .andExpect(jsonPath("$.averageAge").value(9.2))
                .andExpect(jsonPath("$.speciesBreakdown").exists());
    }

    @Test
    @DisplayName("Should handle empty search criteria correctly")
    void seniorPets_WithEmptySearchCriteria_ShouldUseDefaults() {
        // Given
        List<Pet> mockSeniorPets = createMockSeniorPets();
        when(petService.getSeniorPets(7)).thenReturn(Mono.just(mockSeniorPets));

        // When
        String viewName = petController.seniorPets(null, null, "", "", "", "", "", 
                                                  "age", "desc", 0, 10, model);

        // Then
        assertEquals("pets/senior", viewName);
        verify(model).addAttribute("searchPerformed", false);
        verify(petService).getSeniorPets(7);
    }

    @Test
    @DisplayName("Should validate search criteria properly")
    void seniorPets_WithValidSearchCriteria_ShouldDetectSearchPerformed() {
        // Given
        List<Pet> mockSeniorPets = createMockSeniorPets();
        when(petService.getSeniorPets(anyInt())).thenReturn(Mono.just(mockSeniorPets));

        // When - Test with species filter
        petController.seniorPets(null, null, "Dog", null, null, null, null, 
                                "age", "desc", 0, 10, model);

        // Then
        verify(model).addAttribute("searchPerformed", true);

        // Reset mock
        reset(model);
        when(petService.getSeniorPets(anyInt())).thenReturn(Mono.just(mockSeniorPets));

        // When - Test with health condition filter
        petController.seniorPets(null, null, null, null, "Arthritis", null, null, 
                                "age", "desc", 0, 10, model);

        // Then
        verify(model).addAttribute("searchPerformed", true);
    }

    @Test
    @DisplayName("Should handle senior pets page request via MockMvc")
    void seniorPetsPage_ShouldRenderCorrectly() throws Exception {
        // Given
        List<Pet> mockSeniorPets = createMockSeniorPets();
        when(petService.getSeniorPets(anyInt())).thenReturn(Mono.just(mockSeniorPets));

        // When & Then
        mockMvc.perform(get("/pets/senior")
                        .param("species", "Dog")
                        .param("healthCondition", "Arthritis")
                        .param("minAge", "8")
                        .param("maxAge", "15"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/senior"))
                .andExpect(model().attributeExists("pets"))
                .andExpect(model().attributeExists("searchPerformed"))
                .andExpect(model().attribute("searchSpecies", "Dog"))
                .andExpect(model().attribute("searchHealthCondition", "Arthritis"))
                .andExpect(model().attribute("searchMinAge", 8))
                .andExpect(model().attribute("searchMaxAge", 15));
    }

    @Test
    @DisplayName("Should handle pagination parameters correctly")
    void seniorPets_WithPaginationParameters_ShouldPassToModel() {
        // Given
        List<Pet> mockSeniorPets = createMockSeniorPets();
        when(petService.getSeniorPets(anyInt())).thenReturn(Mono.just(mockSeniorPets));

        // When
        String viewName = petController.seniorPets(null, null, null, null, null, null, null, 
                                                  "name", "asc", 2, 20, model);

        // Then
        assertEquals("pets/senior", viewName);
        verify(model).addAttribute("sortBy", "name");
        verify(model).addAttribute("sortDirection", "asc");
        verify(model).addAttribute("currentPage", 2);
    }

    private List<Pet> createMockSeniorPets() {
        Pet seniorDog = new Pet();
        seniorDog.setId(1L);
        seniorDog.setName("Max");
        seniorDog.setSpecies("Dog");
        seniorDog.setBreed("Labrador");
        seniorDog.setBirthDate(LocalDate.now().minusYears(10));
        seniorDog.setMedicalHistory("Arthritis, regular checkups needed");

        Pet seniorCat = new Pet();
        seniorCat.setId(2L);
        seniorCat.setName("Whiskers");
        seniorCat.setSpecies("Cat");
        seniorCat.setBreed("Persian");
        seniorCat.setBirthDate(LocalDate.now().minusYears(12));
        seniorCat.setMedicalHistory("Diabetes, kidney issues");

        return Arrays.asList(seniorDog, seniorCat);
    }
}