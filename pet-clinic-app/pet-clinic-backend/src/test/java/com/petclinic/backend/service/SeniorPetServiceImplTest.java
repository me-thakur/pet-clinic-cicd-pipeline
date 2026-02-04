package com.petclinic.backend.service;

import com.petclinic.backend.dto.SeniorPetInfo;
import com.petclinic.backend.dto.SeniorPetSearchCriteria;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.service.impl.SeniorPetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SeniorPetServiceImpl
 * Tests senior pet search functionality and age threshold management
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@ExtendWith(MockitoExtension.class)
class SeniorPetServiceImplTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private SeniorPetServiceImpl seniorPetService;

    private Pet testSeniorPet;
    private Owner testOwner;

    @BeforeEach
    void setUp() {
        // Create test owner
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Smith");
        testOwner.setEmail("john.smith@example.com");
        testOwner.setMobileNumber("123-456-7890");

        // Create test senior pet
        testSeniorPet = new Pet();
        testSeniorPet.setId(1L);
        testSeniorPet.setName("Max");
        testSeniorPet.setSpecies("Dog");
        testSeniorPet.setBreed("Labrador");
        testSeniorPet.setBirthDate(LocalDate.now().minusYears(10));
        testSeniorPet.setMedicalHistory("Arthritis, regular checkups needed");
        testSeniorPet.setOwner(testOwner);
    }

    @Test
    @DisplayName("Should return species age thresholds")
    void getSpeciesAgeThresholds_ShouldReturnConfiguredThresholds() {
        // When
        Map<String, Integer> thresholds = seniorPetService.getSpeciesAgeThresholds();

        // Then
        assertNotNull(thresholds);
        assertTrue(thresholds.containsKey("Dog"));
        assertTrue(thresholds.containsKey("Cat"));
        assertTrue(thresholds.containsKey("Bird"));
        assertEquals(7, thresholds.get("Dog"));
        assertEquals(7, thresholds.get("Cat"));
        assertEquals(5, thresholds.get("Bird"));
    }

    @Test
    @DisplayName("Should update species age threshold")
    void updateSpeciesAgeThreshold_ShouldUpdateThreshold() {
        // When
        seniorPetService.updateSpeciesAgeThreshold("Dog", 8);
        Map<String, Integer> thresholds = seniorPetService.getSpeciesAgeThresholds();

        // Then
        assertEquals(8, thresholds.get("Dog"));
    }

    @Test
    @DisplayName("Should get age threshold for species")
    void getAgeThresholdForSpecies_ShouldReturnCorrectThreshold() {
        // When
        Integer dogThreshold = seniorPetService.getAgeThresholdForSpecies("Dog");
        Integer birdThreshold = seniorPetService.getAgeThresholdForSpecies("Bird");
        Integer unknownThreshold = seniorPetService.getAgeThresholdForSpecies("Unknown");

        // Then
        assertEquals(7, dogThreshold);
        assertEquals(5, birdThreshold);
        assertEquals(7, unknownThreshold); // Default threshold
    }

    @Test
    @DisplayName("Should search senior pets with criteria")
    void searchSeniorPets_WithCriteria_ShouldReturnFilteredResults() {
        // Given
        List<Pet> mockPets = Arrays.asList(testSeniorPet);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pet> mockPage = new PageImpl<>(mockPets, pageable, mockPets.size());
        
        when(petRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(mockPage);

        SeniorPetSearchCriteria criteria = SeniorPetSearchCriteria.builder()
            .species("Dog")
            .minAge(8)
            .maxAge(15)
            .build();

        // When
        Page<SeniorPetInfo> results = seniorPetService.searchSeniorPets(criteria, pageable);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals(1, results.getContent().size());
        
        SeniorPetInfo seniorPetInfo = results.getContent().get(0);
        assertEquals("Max", seniorPetInfo.getPetName());
        assertEquals("Dog", seniorPetInfo.getSpecies());
        assertEquals(10, seniorPetInfo.getAge());
        assertTrue(seniorPetInfo.getIsSenior());
        assertEquals(7, seniorPetInfo.getSeniorThreshold());
        assertEquals("John Smith", seniorPetInfo.getOwnerName());
    }

    @Test
    @DisplayName("Should get all senior pets")
    void getAllSeniorPets_ShouldReturnAllSeniorPets() {
        // Given
        List<Pet> mockPets = Arrays.asList(testSeniorPet);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pet> mockPage = new PageImpl<>(mockPets, pageable, mockPets.size());
        
        when(petRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(mockPage);

        // When
        Page<SeniorPetInfo> results = seniorPetService.getAllSeniorPets(pageable);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals(1, results.getContent().size());
        
        SeniorPetInfo seniorPetInfo = results.getContent().get(0);
        assertEquals("Max", seniorPetInfo.getPetName());
        assertTrue(seniorPetInfo.getIsSenior());
    }

    @Test
    @DisplayName("Should get fallback senior pets when search fails")
    void getFallbackSeniorPets_ShouldReturnBasicList() {
        // Given
        List<Pet> mockPets = Arrays.asList(testSeniorPet);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(petRepository.findSeniorPets(any(LocalDate.class)))
            .thenReturn(mockPets);

        // When
        Page<SeniorPetInfo> results = seniorPetService.getFallbackSeniorPets(pageable);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals(1, results.getContent().size());
        
        SeniorPetInfo seniorPetInfo = results.getContent().get(0);
        assertEquals("Max", seniorPetInfo.getPetName());
        assertEquals("Dog", seniorPetInfo.getSpecies());
    }

    @Test
    @DisplayName("Should get senior pet statistics")
    void getSeniorPetStatistics_ShouldReturnStatistics() {
        // Given
        List<Pet> mockSeniorPets = Arrays.asList(testSeniorPet);
        when(petRepository.findSeniorPets(any(LocalDate.class)))
            .thenReturn(mockSeniorPets);
        when(petRepository.count()).thenReturn(10L);

        // When
        Map<String, Object> statistics = seniorPetService.getSeniorPetStatistics();

        // Then
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("totalSeniorPets"));
        assertTrue(statistics.containsKey("totalPets"));
        assertTrue(statistics.containsKey("seniorPercentage"));
        assertTrue(statistics.containsKey("speciesBreakdown"));
        assertTrue(statistics.containsKey("ageThresholds"));
        
        assertEquals(1L, statistics.get("totalSeniorPets"));
        assertEquals(10L, statistics.get("totalPets"));
        assertEquals(10.0, statistics.get("seniorPercentage"));
    }

    @Test
    @DisplayName("Should get available health conditions")
    void getAvailableHealthConditions_ShouldReturnConditions() {
        // Given
        List<Pet> mockSeniorPets = Arrays.asList(testSeniorPet);
        when(petRepository.findSeniorPets(any(LocalDate.class)))
            .thenReturn(mockSeniorPets);

        // When
        List<String> conditions = seniorPetService.getAvailableHealthConditions();

        // Then
        assertNotNull(conditions);
        assertTrue(conditions.contains("Arthritis"));
    }

    @Test
    @DisplayName("Should get available species")
    void getAvailableSpecies_ShouldReturnSpecies() {
        // Given
        List<Pet> mockSeniorPets = Arrays.asList(testSeniorPet);
        when(petRepository.findSeniorPets(any(LocalDate.class)))
            .thenReturn(mockSeniorPets);

        // When
        List<String> species = seniorPetService.getAvailableSpecies();

        // Then
        assertNotNull(species);
        assertTrue(species.contains("Dog"));
    }
}