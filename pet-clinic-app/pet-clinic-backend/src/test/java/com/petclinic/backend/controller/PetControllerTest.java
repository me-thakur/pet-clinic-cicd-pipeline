package com.petclinic.backend.controller;

import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.model.VisitType;
import com.petclinic.backend.service.PetService;
import com.petclinic.backend.service.impl.PetServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.mockito.ArgumentMatchers;

/**
 * Comprehensive unit tests for PetController
 * Tests all REST API endpoints for pet management including:
 * - CRUD operations with success and error scenarios
 * - Search functionality and edge cases
 * - Input validation and error responses
 * - Business rule enforcement
 * - Proper HTTP status codes and response formats
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**
 */
@WebMvcTest(PetController.class)
@DisplayName("Pet Controller Tests")
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    @Autowired
    private ObjectMapper objectMapper;

    private Pet testPet;
    private Pet testPet2;
    private Owner testOwner;
    private Visit testVisit;

    @BeforeEach
    void setUp() {
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
        testPet.setMedicalHistory("Vaccinated, healthy");

        testPet2 = new Pet();
        testPet2.setId(2L);
        testPet2.setName("Whiskers");
        testPet2.setSpecies("Cat");
        testPet2.setBreed("Persian");
        testPet2.setBirthDate(LocalDate.of(2019, 6, 10));
        testPet2.setOwner(testOwner);

        testVisit = new Visit();
        testVisit.setId(1L);
        testVisit.setPet(testPet);
        testVisit.setVisitDate(LocalDateTime.now().plusDays(7));
        testVisit.setVisitType(VisitType.WELLNESS_EXAM);
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets - Should return paginated pets successfully")
        void getAllPets_ShouldReturnPaginatedPets() throws Exception {
            // Given
            List<Pet> pets = Arrays.asList(testPet, testPet2);
            when(petService.findAll()).thenReturn(pets);

            // When & Then
            mockMvc.perform(get("/api/pets")
                    .param("page", "0")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/{id} - Should return pet when exists")
        void getPetById_WhenPetExists_ShouldReturnPet() throws Exception {
            // Given
            when(petService.findById(1L)).thenReturn(Optional.of(testPet));

            // When & Then
            mockMvc.perform(get("/api/pets/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Buddy"))
                    .andExpect(jsonPath("$.species").value("Dog"))
                    .andExpect(jsonPath("$.breed").value("Golden Retriever"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/{id} - Should return 404 when pet not found")
        void getPetById_WhenPetNotExists_ShouldReturn404() throws Exception {
            // Given
            when(petService.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/pets/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/pets - Should create pet with valid data")
        void createPet_WithValidData_ShouldReturnCreatedPet() throws Exception {
            // Given
            Pet newPet = new Pet();
            newPet.setName("Max");
            newPet.setSpecies("Dog");
            newPet.setBreed("Labrador");
            newPet.setBirthDate(LocalDate.of(2021, 6, 10));
            newPet.setOwner(testOwner);

            Pet createdPet = new Pet();
            createdPet.setId(2L);
            createdPet.setName("Max");
            createdPet.setSpecies("Dog");
            createdPet.setBreed("Labrador");
            createdPet.setBirthDate(LocalDate.of(2021, 6, 10));
            createdPet.setOwner(testOwner);

            when(petService.create(ArgumentMatchers.any(Pet.class))).thenReturn(createdPet);

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newPet)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("Max"))
                    .andExpect(jsonPath("$.species").value("Dog"))
                    .andExpect(jsonPath("$.breed").value("Labrador"));

            verify(petService).create(ArgumentMatchers.any(Pet.class));
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/pets - Should return 400 for invalid data")
        void createPet_WithInvalidData_ShouldReturn400() throws Exception {
            // Given - Pet with missing required fields
            Pet invalidPet = new Pet();
            // Missing name and species

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPet)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/pets - Should return 400 for future birth date")
        void createPet_WithFutureBirthDate_ShouldReturn400() throws Exception {
            // Given - Pet with future birth date
            Pet invalidPet = new Pet();
            invalidPet.setName("Future Pet");
            invalidPet.setSpecies("Dog");
            invalidPet.setBirthDate(LocalDate.now().plusDays(1));
            invalidPet.setOwner(testOwner);

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPet)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /api/pets/{id} - Should update pet with valid data")
        void updatePet_WithValidData_ShouldReturnUpdatedPet() throws Exception {
            // Given
            Pet updatedPet = new Pet();
            updatedPet.setId(1L);
            updatedPet.setName("Buddy Updated");
            updatedPet.setSpecies("Dog");
            updatedPet.setBreed("Golden Retriever");
            updatedPet.setBirthDate(LocalDate.of(2020, 1, 15));
            updatedPet.setOwner(testOwner);

            when(petService.update(eq(1L), ArgumentMatchers.any(Pet.class))).thenReturn(updatedPet);

            // When & Then
            mockMvc.perform(put("/api/pets/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedPet)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Buddy Updated"));

            verify(petService).update(eq(1L), ArgumentMatchers.any(Pet.class));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /api/pets/{id} - Should return 404 when pet not found")
        void updatePet_WhenPetNotFound_ShouldReturn404() throws Exception {
            // Given
            Pet updateData = new Pet();
            updateData.setName("Updated Name");
            updateData.setSpecies("Dog");
            updateData.setOwner(testOwner);

            when(petService.update(eq(999L), ArgumentMatchers.any(Pet.class)))
                .thenThrow(new PetClinicException("Pet not found", "PET_NOT_FOUND", HttpStatus.NOT_FOUND));

            // When & Then
            mockMvc.perform(put("/api/pets/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("DELETE /api/pets/{id} - Should delete pet successfully")
        void deletePet_WhenPetExists_ShouldReturn204() throws Exception {
            // Given
            doNothing().when(petService).deleteById(1L);

            // When & Then
            mockMvc.perform(delete("/api/pets/1")
                    .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(petService).deleteById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("DELETE /api/pets/{id} - Should return 404 when pet not found")
        void deletePet_WhenPetNotFound_ShouldReturn404() throws Exception {
            // Given
            doThrow(new PetClinicException("Pet not found", "PET_NOT_FOUND", HttpStatus.NOT_FOUND))
                .when(petService).deleteById(999L);

            // When & Then
            mockMvc.perform(delete("/api/pets/999")
                    .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("DELETE /api/pets/{id} - Should return 422 when pet has visits")
        void deletePet_WhenPetHasVisits_ShouldReturn422() throws Exception {
            // Given
            doThrow(new PetClinicException("Cannot delete pet with visits", "PET_HAS_VISITS", HttpStatus.UNPROCESSABLE_ENTITY))
                .when(petService).deleteById(1L);

            // When & Then
            mockMvc.perform(delete("/api/pets/1")
                    .with(csrf()))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("Search and Filtering Tests")
    class SearchAndFilteringTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/search - Should search pets by general query")
        void searchPets_WithGeneralQuery_ShouldReturnMatchingPets() throws Exception {
            // Given
            List<Pet> searchResults = Arrays.asList(testPet);
            when(petService.searchPets("Buddy")).thenReturn(searchResults);

            // When & Then
            mockMvc.perform(get("/api/pets/search")
                    .param("q", "Buddy"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Buddy"));

            verify(petService).searchPets("Buddy");
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/search - Should handle empty search results")
        void searchPets_WithNoResults_ShouldReturnEmptyPage() throws Exception {
            // Given
            when(petService.searchPets("NonExistent")).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/pets/search")
                    .param("q", "NonExistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/search - Should search with advanced criteria")
        void searchPets_WithAdvancedCriteria_ShouldReturnFilteredResults() throws Exception {
            // Given
            List<Pet> results = Arrays.asList(testPet);
            Page<Pet> resultPage = new PageImpl<>(results, PageRequest.of(0, 10), 1);
            
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            when(petServiceImpl.searchPetsAdvanced("Buddy", "Dog", "Golden Retriever", 1L, 0, 10))
                .thenReturn(resultPage);

            // When & Then
            mockMvc.perform(get("/api/pets/search")
                    .param("name", "Buddy")
                    .param("species", "Dog")
                    .param("breed", "Golden Retriever")
                    .param("ownerId", "1")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/search - Should validate pagination parameters")
        void searchPets_WithInvalidPagination_ShouldReturn400() throws Exception {
            // When & Then - negative page number
            mockMvc.perform(get("/api/pets/search")
                    .param("page", "-1")
                    .param("size", "10"))
                    .andExpect(status().isBadRequest());

            // When & Then - zero page size
            mockMvc.perform(get("/api/pets/search")
                    .param("page", "0")
                    .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/species/{species} - Should return pets of specified species")
        void getPetsBySpecies_ShouldReturnPetsOfSpecies() throws Exception {
            // Given
            List<Pet> dogs = Arrays.asList(testPet);
            when(petService.findBySpecies("Dog")).thenReturn(dogs);

            // When & Then
            mockMvc.perform(get("/api/pets/species/Dog"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].species").value("Dog"));

            verify(petService).findBySpecies("Dog");
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/species/{species} - Should return 400 for blank species")
        void getPetsBySpecies_WithBlankSpecies_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/pets/species/ "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/breed/{breed} - Should return pets of specified breed")
        void getPetsByBreed_ShouldReturnPetsOfBreed() throws Exception {
            // Given
            List<Pet> goldenRetrievers = Arrays.asList(testPet);
            when(petService.findByBreed("Golden Retriever")).thenReturn(goldenRetrievers);

            // When & Then
            mockMvc.perform(get("/api/pets/breed/Golden Retriever"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].breed").value("Golden Retriever"));

            verify(petService).findByBreed("Golden Retriever");
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/age-range - Should return pets within age range")
        void getPetsByAgeRange_ShouldReturnPetsInRange() throws Exception {
            // Given
            List<Pet> youngPets = Arrays.asList(testPet);
            when(petService.findByAgeRange(1, 5)).thenReturn(youngPets);

            // When & Then
            mockMvc.perform(get("/api/pets/age-range")
                    .param("minAge", "1")
                    .param("maxAge", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Buddy"));

            verify(petService).findByAgeRange(1, 5);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/age-range - Should return 400 for negative age values")
        void getPetsByAgeRange_WithNegativeAge_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/pets/age-range")
                    .param("minAge", "-1")
                    .param("maxAge", "5"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Owner-Related Endpoint Tests")
    class OwnerRelatedTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/owners/{id}/pets - Should return owner's pets")
        void getPetsByOwner_ShouldReturnOwnersPets() throws Exception {
            // Given
            List<Pet> ownerPets = Arrays.asList(testPet, testPet2);
            when(petService.findByOwner(1L)).thenReturn(ownerPets);

            // When & Then
            mockMvc.perform(get("/api/pets/owners/1/pets"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Buddy"))
                    .andExpect(jsonPath("$[1].name").value("Whiskers"));

            verify(petService).findByOwner(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/owner/{ownerId} - Should return owner's pets (alternative endpoint)")
        void getPetsByOwnerId_ShouldReturnOwnersPets() throws Exception {
            // Given
            List<Pet> ownerPets = Arrays.asList(testPet);
            when(petService.findByOwner(1L)).thenReturn(ownerPets);

            // When & Then
            mockMvc.perform(get("/api/pets/owner/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(petService).findByOwner(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/owners/{id}/pets - Should return empty list for owner with no pets")
        void getPetsByOwner_WithNoPets_ShouldReturnEmptyList() throws Exception {
            // Given
            when(petService.findByOwner(999L)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/pets/owners/999/pets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}