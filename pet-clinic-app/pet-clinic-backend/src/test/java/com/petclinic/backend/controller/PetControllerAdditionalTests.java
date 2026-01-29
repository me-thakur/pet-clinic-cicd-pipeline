package com.petclinic.backend.controller;

import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.Owner;
import com.petclinic.backend.model.Pet;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.mockito.ArgumentMatchers;

/**
 * Additional comprehensive unit tests for PetController
 * Covers specialized endpoints, validation, error handling, and edge cases
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**
 */
@WebMvcTest(PetController.class)
@DisplayName("Pet Controller Additional Tests")
class PetControllerAdditionalTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    @Autowired
    private ObjectMapper objectMapper;

    private Pet testPet;
    private Pet testPet2;
    private Owner testOwner;

    @BeforeEach
    void setUp() {
        testOwner = new Owner();
        testOwner.setId(1L);
        testOwner.setFirstName("John");
        testOwner.setLastName("Doe");
        testOwner.setEmail("john.doe@example.com");

        testPet = new Pet();
        testPet.setId(1L);
        testPet.setName("Buddy");
        testPet.setSpecies("Dog");
        testPet.setBreed("Golden Retriever");
        testPet.setBirthDate(LocalDate.of(2020, 1, 15));
        testPet.setOwner(testOwner);

        testPet2 = new Pet();
        testPet2.setId(2L);
        testPet2.setName("Whiskers");
        testPet2.setSpecies("Cat");
        testPet2.setBreed("Persian");
        testPet2.setBirthDate(LocalDate.of(2019, 6, 10));
        testPet2.setOwner(testOwner);
    }
    @Nested
    @DisplayName("Specialized Search Endpoint Tests")
    class SpecializedSearchTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/young - Should return young pets")
        void getYoungPets_ShouldReturnYoungPets() throws Exception {
            // Given
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            List<Pet> youngPets = Arrays.asList(testPet);
            when(petServiceImpl.findYoungPets(2)).thenReturn(youngPets);

            // When & Then
            mockMvc.perform(get("/api/pets/young"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/senior - Should return senior pets")
        void getSeniorPets_ShouldReturnSeniorPets() throws Exception {
            // Given
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            List<Pet> seniorPets = Arrays.asList(testPet2);
            when(petServiceImpl.findSeniorPets(7)).thenReturn(seniorPets);

            // When & Then
            mockMvc.perform(get("/api/pets/senior")
                    .param("age", "7"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/senior - Should validate minimum age parameter")
        void getSeniorPets_WithInvalidAge_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/pets/senior")
                    .param("age", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/upcoming-visits - Should return pets with upcoming visits")
        void getPetsWithUpcomingVisits_ShouldReturnPetsWithVisits() throws Exception {
            // Given
            List<Pet> petsWithVisits = Arrays.asList(testPet);
            when(petService.findPetsWithUpcomingVisits()).thenReturn(petsWithVisits);

            // When & Then
            mockMvc.perform(get("/api/pets/upcoming-visits"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(petService).findPetsWithUpcomingVisits();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/no-visits - Should return pets without visits")
        void getPetsWithoutVisits_ShouldReturnPetsWithoutVisits() throws Exception {
            // Given
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            List<Pet> petsWithoutVisits = Arrays.asList(testPet2);
            when(petServiceImpl.findPetsWithoutVisits()).thenReturn(petsWithoutVisits);

            // When & Then
            mockMvc.perform(get("/api/pets/no-visits"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/medical-history - Should search pets by medical history")
        void getPetsByMedicalHistory_ShouldReturnMatchingPets() throws Exception {
            // Given
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            List<Pet> matchingPets = Arrays.asList(testPet);
            when(petServiceImpl.findByMedicalHistory("allergy")).thenReturn(matchingPets);

            // When & Then
            mockMvc.perform(get("/api/pets/medical-history")
                    .param("keywords", "allergy"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/medical-history - Should return 400 for blank keywords")
        void getPetsByMedicalHistory_WithBlankKeywords_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/pets/medical-history")
                    .param("keywords", " "))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Statistics and Analytics Tests")
    class StatisticsAndAnalyticsTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/statistics - Should return pet statistics")
        void getPetStatistics_ShouldReturnStatistics() throws Exception {
            // Given
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            Map<String, Object> stats = Map.of(
                "totalPets", 10L,
                "totalSpecies", 3L,
                "totalOwners", 5L
            );
            when(petServiceImpl.getPetStatistics()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/pets/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalPets").value(10))
                    .andExpect(jsonPath("$.totalSpecies").value(3))
                    .andExpect(jsonPath("$.totalOwners").value(5));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/species-count - Should return species count")
        void getPetsBySpeciesCount_ShouldReturnSpeciesCount() throws Exception {
            // Given
            PetServiceImpl petServiceImpl = mock(PetServiceImpl.class);
            List<Object[]> speciesCount = Arrays.asList(
                new Object[]{"Dog", 5L},
                new Object[]{"Cat", 3L}
            );
            when(petServiceImpl.countPetsBySpecies()).thenReturn(speciesCount);

            // When & Then
            mockMvc.perform(get("/api/pets/species-count"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].species").value("Dog"))
                    .andExpect(jsonPath("$[0].count").value(5))
                    .andExpect(jsonPath("$[1].species").value("Cat"))
                    .andExpect(jsonPath("$[1].count").value(3));
        }
    }

    @Nested
    @DisplayName("Utility Endpoint Tests")
    class UtilityEndpointTests {

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/{id}/can-delete - Should return true when pet can be deleted")
        void canDeletePet_WhenCanDelete_ShouldReturnTrue() throws Exception {
            // Given
            when(petService.canDeletePet(1L)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/pets/1/can-delete"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.canDelete").value(true));

            verify(petService).canDeletePet(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/{id}/can-delete - Should return false when pet cannot be deleted")
        void canDeletePet_WhenCannotDelete_ShouldReturnFalse() throws Exception {
            // Given
            when(petService.canDeletePet(1L)).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/pets/1/can-delete"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.canDelete").value(false));

            verify(petService).canDeletePet(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/count - Should return total pet count")
        void getPetCount_ShouldReturnTotalCount() throws Exception {
            // Given
            when(petService.count()).thenReturn(15L);

            // When & Then
            mockMvc.perform(get("/api/pets/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.count").value(15));

            verify(petService).count();
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/pets/health - Should return health status")
        void healthCheck_ShouldReturnHealthStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/pets/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("PetController"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @WithMockUser
        @DisplayName("POST /api/pets - Should return 400 for pet with name too long")
        void createPet_WithNameTooLong_ShouldReturn400() throws Exception {
            // Given - Pet with name exceeding 50 characters
            Pet invalidPet = new Pet();
            invalidPet.setName("A".repeat(51)); // 51 characters
            invalidPet.setSpecies("Dog");
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
        @DisplayName("POST /api/pets - Should return 400 for pet with species too long")
        void createPet_WithSpeciesTooLong_ShouldReturn400() throws Exception {
            // Given - Pet with species exceeding 30 characters
            Pet invalidPet = new Pet();
            invalidPet.setName("Valid Name");
            invalidPet.setSpecies("A".repeat(31)); // 31 characters
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
        @DisplayName("POST /api/pets - Should return 400 for pet with breed too long")
        void createPet_WithBreedTooLong_ShouldReturn400() throws Exception {
            // Given - Pet with breed exceeding 50 characters
            Pet invalidPet = new Pet();
            invalidPet.setName("Valid Name");
            invalidPet.setSpecies("Dog");
            invalidPet.setBreed("A".repeat(51)); // 51 characters
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
        @DisplayName("POST /api/pets - Should return 400 for pet with medical history too long")
        void createPet_WithMedicalHistoryTooLong_ShouldReturn400() throws Exception {
            // Given - Pet with medical history exceeding 1000 characters
            Pet invalidPet = new Pet();
            invalidPet.setName("Valid Name");
            invalidPet.setSpecies("Dog");
            invalidPet.setMedicalHistory("A".repeat(1001)); // 1001 characters
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
        @DisplayName("POST /api/pets - Should return 400 for pet without owner")
        void createPet_WithoutOwner_ShouldReturn400() throws Exception {
            // Given - Pet without owner
            Pet invalidPet = new Pet();
            invalidPet.setName("Valid Name");
            invalidPet.setSpecies("Dog");
            // No owner set

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidPet)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /api/pets/{id} - Should return 400 for invalid update data")
        void updatePet_WithInvalidData_ShouldReturn400() throws Exception {
            // Given - Invalid update data
            Pet invalidUpdate = new Pet();
            invalidUpdate.setName(""); // Empty name
            invalidUpdate.setSpecies("Dog");
            invalidUpdate.setOwner(testOwner);

            // When & Then
            mockMvc.perform(put("/api/pets/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidUpdate)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @WithMockUser
        @DisplayName("Should handle service exceptions properly")
        void handleServiceException_ShouldReturnProperErrorResponse() throws Exception {
            // Given
            when(petService.findById(1L))
                .thenThrow(new PetClinicException("Database error", "DB_ERROR", HttpStatus.INTERNAL_SERVER_ERROR));

            // When & Then
            mockMvc.perform(get("/api/pets/1"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle business rule violations")
        void handleBusinessRuleViolation_ShouldReturn422() throws Exception {
            // Given
            when(petService.create(ArgumentMatchers.any(Pet.class)))
                .thenThrow(new PetClinicException("Business rule violation", "BUSINESS_RULE_ERROR", HttpStatus.UNPROCESSABLE_ENTITY));

            Pet validPet = new Pet();
            validPet.setName("Test Pet");
            validPet.setSpecies("Dog");
            validPet.setOwner(testOwner);

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPet)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle malformed JSON requests")
        void handleMalformedJson_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle missing Content-Type header")
        void handleMissingContentType_ShouldReturn415() throws Exception {
            // Given
            Pet validPet = new Pet();
            validPet.setName("Test Pet");
            validPet.setSpecies("Dog");
            validPet.setOwner(testOwner);

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .content(objectMapper.writeValueAsString(validPet)))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("Security and CORS Tests")
    class SecurityAndCorsTests {

        @Test
        @DisplayName("Should require authentication for protected endpoints")
        void requireAuthentication_ForProtectedEndpoints() throws Exception {
            // When & Then - Without @WithMockUser
            mockMvc.perform(get("/api/pets"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should require CSRF token for state-changing operations")
        void requireCsrfToken_ForStateChangingOperations() throws Exception {
            // Given
            Pet newPet = new Pet();
            newPet.setName("Test Pet");
            newPet.setSpecies("Dog");
            newPet.setOwner(testOwner);

            // When & Then - Without CSRF token
            mockMvc.perform(post("/api/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newPet)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle CORS preflight requests")
        void handleCorsPreflightRequests() throws Exception {
            // When & Then
            mockMvc.perform(options("/api/pets")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @WithMockUser
        @DisplayName("Should handle very large page sizes gracefully")
        void handleLargePageSizes_ShouldLimitResults() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/pets")
                    .param("page", "0")
                    .param("size", "10000"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle special characters in search queries")
        void handleSpecialCharactersInSearch_ShouldNotFail() throws Exception {
            // Given
            when(petService.searchPets("@#$%^&*()")).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/pets/search")
                    .param("q", "@#$%^&*()"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle Unicode characters in pet names")
        void handleUnicodeCharacters_ShouldWork() throws Exception {
            // Given
            Pet unicodePet = new Pet();
            unicodePet.setName("Ñoño 🐕");
            unicodePet.setSpecies("Dog");
            unicodePet.setOwner(testOwner);

            Pet createdPet = new Pet();
            createdPet.setId(3L);
            createdPet.setName("Ñoño 🐕");
            createdPet.setSpecies("Dog");
            createdPet.setOwner(testOwner);

            when(petService.create(ArgumentMatchers.any(Pet.class))).thenReturn(createdPet);

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(unicodePet)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Ñoño 🐕"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle concurrent requests gracefully")
        void handleConcurrentRequests_ShouldNotFail() throws Exception {
            // Given
            when(petService.findById(1L)).thenReturn(Optional.of(testPet));

            // When & Then - Simulate concurrent requests
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/pets/1"))
                        .andExpect(status().isOk());
            }

            verify(petService, times(5)).findById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle empty request body gracefully")
        void handleEmptyRequestBody_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle null values in JSON gracefully")
        void handleNullValuesInJson_ShouldValidateCorrectly() throws Exception {
            // Given - JSON with explicit null values
            String jsonWithNulls = "{\n" +
                    "    \"name\": null,\n" +
                    "    \"species\": \"Dog\",\n" +
                    "    \"breed\": null,\n" +
                    "    \"birthDate\": null,\n" +
                    "    \"owner\": null\n" +
                    "}";

            // When & Then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonWithNulls))
                    .andExpect(status().isBadRequest());
        }
    }
}