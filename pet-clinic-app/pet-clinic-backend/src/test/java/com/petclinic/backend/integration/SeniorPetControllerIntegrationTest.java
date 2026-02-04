package com.petclinic.backend.integration;

import com.petclinic.backend.dto.SeniorPetInfo;
import com.petclinic.backend.service.SeniorPetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SeniorPetController
 * Tests the complete flow from controller to service
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class SeniorPetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeniorPetService seniorPetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("Integration Test: GET /api/pets/senior/search - Should return senior pets with search criteria")
    void searchSeniorPets_IntegrationTest_ShouldReturnResults() throws Exception {
        // Given
        SeniorPetInfo seniorPet = new SeniorPetInfo();
        seniorPet.setPetId(1L);
        seniorPet.setPetName("Max");
        seniorPet.setSpecies("Dog");
        seniorPet.setAge(10);
        seniorPet.setIsSenior(true);
        seniorPet.setSeniorThreshold(7);
        seniorPet.setOwnerName("John Smith");
        seniorPet.setAgeHighlighted(true);

        List<SeniorPetInfo> seniorPets = Arrays.asList(seniorPet);
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(seniorPets, pageable, seniorPets.size());
        
        when(seniorPetService.searchSeniorPets(any(), any(Pageable.class)))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/search")
                .param("minAge", "8")
                .param("species", "Dog")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].petName").value("Max"))
                .andExpect(jsonPath("$.content[0].species").value("Dog"))
                .andExpect(jsonPath("$.content[0].isSenior").value(true))
                .andExpect(jsonPath("$.content[0].seniorThreshold").value(7))
                .andExpect(jsonPath("$.content[0].ageHighlighted").value(true))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Integration Test: GET /api/pets/senior/age-thresholds - Should return age thresholds")
    void getSpeciesAgeThresholds_IntegrationTest_ShouldReturnThresholds() throws Exception {
        // Given
        Map<String, Integer> thresholds = Map.of(
            "Dog", 7,
            "Cat", 7,
            "Bird", 5,
            "Rabbit", 5
        );
        
        when(seniorPetService.getSpeciesAgeThresholds()).thenReturn(thresholds);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/age-thresholds")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Dog").value(7))
                .andExpect(jsonPath("$.Cat").value(7))
                .andExpect(jsonPath("$.Bird").value(5))
                .andExpect(jsonPath("$.Rabbit").value(5));
    }

    @Test
    @WithMockUser
    @DisplayName("Integration Test: GET /api/pets/senior/health - Should return health status")
    void healthCheck_IntegrationTest_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/pets/senior/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("SeniorPetController"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Integration Test: GET /api/pets/senior/species/Dog - Should return senior dogs")
    void getSeniorPetsBySpecies_IntegrationTest_ShouldReturnSpeciesResults() throws Exception {
        // Given
        SeniorPetInfo seniorDog = new SeniorPetInfo();
        seniorDog.setPetId(1L);
        seniorDog.setPetName("Max");
        seniorDog.setSpecies("Dog");
        seniorDog.setAge(10);
        seniorDog.setIsSenior(true);
        seniorDog.setSeniorThreshold(7);

        List<SeniorPetInfo> seniorDogs = Arrays.asList(seniorDog);
        Pageable pageable = PageRequest.of(0, 10);
        Page<SeniorPetInfo> mockPage = new PageImpl<>(seniorDogs, pageable, seniorDogs.size());
        
        when(seniorPetService.getSeniorPetsBySpecies("Dog", pageable))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/pets/senior/species/Dog")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].species").value("Dog"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
}