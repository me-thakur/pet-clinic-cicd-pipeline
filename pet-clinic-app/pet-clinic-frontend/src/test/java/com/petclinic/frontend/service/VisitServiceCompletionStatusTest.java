package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Visit;
import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.Veterinarian;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for VisitService completion status handling
 * Validates that all retrieval methods properly handle completion status from backend responses
 * Validates: Requirements 2.1, 2.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Visit Service Completion Status Tests")
class VisitServiceCompletionStatusTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private VisitService visitService;

    private Visit completedVisit;
    private Visit incompleteVisit;

    @BeforeEach
    void setUp() {
        // Setup common mock behavior
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Create test visits
        completedVisit = createTestVisit(1L, "Completed visit", "Full diagnosis", "Complete treatment", true);
        incompleteVisit = createTestVisit(2L, "Incomplete visit", "Partial diagnosis", null, false);
    }

    private Visit createTestVisit(Long id, String description, String diagnosis, String treatment, boolean completed) {
        Visit visit = new Visit();
        visit.setId(id);
        visit.setVisitDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        visit.setDescription(description);
        visit.setDiagnosis(diagnosis);
        visit.setTreatment(treatment);
        visit.setCost(BigDecimal.valueOf(75.00));
        visit.setCompleted(completed);

        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Buddy");
        visit.setPet(pet);

        Veterinarian vet = new Veterinarian();
        vet.setId(1L);
        vet.setFirstName("Dr. Jane");
        vet.setLastName("Smith");
        visit.setVeterinarian(vet);

        return visit;
    }

    @Test
    @DisplayName("getVisitById should handle completion status correctly")
    void getVisitById_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.just(completedVisit));

        // When
        Visit result = visitService.getVisitById(1L).block();

        // Then
        assertNotNull(result);
        assertTrue(result.getCompleted(), "Completion status should be preserved from backend response");
        assertEquals("Complete treatment", result.getTreatment());
        assertEquals("Full diagnosis", result.getDiagnosis());
    }

    @Test
    @DisplayName("getVisitsByPetId should handle completion status correctly")
    void getVisitsByPetId_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(completedVisit, incompleteVisit));

        // When
        List<Visit> results = visitService.getVisitsByPetId(1L).block();

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        Visit completed = results.stream().filter(v -> v.getId().equals(1L)).findFirst().orElse(null);
        Visit incomplete = results.stream().filter(v -> v.getId().equals(2L)).findFirst().orElse(null);
        
        assertNotNull(completed);
        assertNotNull(incomplete);
        assertTrue(completed.getCompleted(), "Completed visit should have completion status true");
        assertFalse(incomplete.getCompleted(), "Incomplete visit should have completion status false");
    }

    @Test
    @DisplayName("getVisitsByOwnerId should handle completion status correctly")
    void getVisitsByOwnerId_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(completedVisit, incompleteVisit));

        // When
        List<Visit> results = visitService.getVisitsByOwnerId(1L).block();

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        Visit completed = results.stream().filter(v -> v.getId().equals(1L)).findFirst().orElse(null);
        Visit incomplete = results.stream().filter(v -> v.getId().equals(2L)).findFirst().orElse(null);
        
        assertNotNull(completed);
        assertNotNull(incomplete);
        assertTrue(completed.getCompleted(), "Completed visit should have completion status true");
        assertFalse(incomplete.getCompleted(), "Incomplete visit should have completion status false");
    }

    @Test
    @DisplayName("getVisitsByVeterinarianId should handle completion status correctly")
    void getVisitsByVeterinarianId_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(completedVisit, incompleteVisit));

        // When
        List<Visit> results = visitService.getVisitsByVeterinarianId(1L).block();

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        Visit completed = results.stream().filter(v -> v.getId().equals(1L)).findFirst().orElse(null);
        Visit incomplete = results.stream().filter(v -> v.getId().equals(2L)).findFirst().orElse(null);
        
        assertNotNull(completed);
        assertNotNull(incomplete);
        assertTrue(completed.getCompleted(), "Completed visit should have completion status true");
        assertFalse(incomplete.getCompleted(), "Incomplete visit should have completion status false");
    }

    @Test
    @DisplayName("getAllVisits should handle completion status correctly in paginated response")
    void getAllVisits_ShouldHandleCompletionStatusCorrectlyInPaginatedResponse() {
        // Given - Create a mock paginated response
        Map<String, Object> visitMap1 = createVisitMap(1L, "Completed visit", "Full diagnosis", "Complete treatment", true);
        Map<String, Object> visitMap2 = createVisitMap(2L, "Incomplete visit", "Partial diagnosis", null, false);
        
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("number", 0);
        pageInfo.put("size", 10);
        pageInfo.put("totalElements", 2L);
        
        Map<String, Object> pageResponse = new HashMap<>();
        pageResponse.put("content", List.of(visitMap1, visitMap2));
        pageResponse.put("page", pageInfo);

        when(responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .thenReturn(Mono.just(pageResponse));

        // When
        Page<Visit> result = visitService.getAllVisits(PageRequest.of(0, 10)).block();

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        
        Visit completed = result.getContent().stream().filter(v -> v.getId().equals(1L)).findFirst().orElse(null);
        Visit incomplete = result.getContent().stream().filter(v -> v.getId().equals(2L)).findFirst().orElse(null);
        
        assertNotNull(completed);
        assertNotNull(incomplete);
        assertTrue(completed.getCompleted(), "Completed visit should have completion status true");
        assertFalse(incomplete.getCompleted(), "Incomplete visit should have completion status false");
        assertEquals("Complete treatment", completed.getTreatment());
        assertNull(incomplete.getTreatment());
    }

    @Test
    @DisplayName("getCompletedVisits should handle completion status correctly")
    void getCompletedVisits_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(completedVisit));

        // When
        List<Visit> results = visitService.getCompletedVisits().block();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getCompleted(), "All visits from getCompletedVisits should have completion status true");
    }

    @Test
    @DisplayName("getIncompleteVisits should handle completion status correctly")
    void getIncompleteVisits_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(incompleteVisit));

        // When
        List<Visit> results = visitService.getIncompleteVisits().block();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertFalse(results.get(0).getCompleted(), "All visits from getIncompleteVisits should have completion status false");
    }

    @Test
    @DisplayName("searchByDiagnosis should handle completion status correctly")
    void searchByDiagnosis_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(completedVisit, incompleteVisit));

        // When
        List<Visit> results = visitService.searchByDiagnosis("diagnosis").block();

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        Visit completed = results.stream().filter(v -> v.getId().equals(1L)).findFirst().orElse(null);
        Visit incomplete = results.stream().filter(v -> v.getId().equals(2L)).findFirst().orElse(null);
        
        assertNotNull(completed);
        assertNotNull(incomplete);
        assertTrue(completed.getCompleted(), "Completed visit should have completion status true");
        assertFalse(incomplete.getCompleted(), "Incomplete visit should have completion status false");
    }

    @Test
    @DisplayName("searchByTreatment should handle completion status correctly")
    void searchByTreatment_ShouldHandleCompletionStatusCorrectly() {
        // Given
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(Flux.just(completedVisit));

        // When
        List<Visit> results = visitService.searchByTreatment("treatment").block();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getCompleted(), "Visit should have completion status preserved from backend");
    }

    private Map<String, Object> createVisitMap(Long id, String description, String diagnosis, String treatment, boolean completed) {
        Map<String, Object> visitMap = new HashMap<>();
        visitMap.put("id", id);
        visitMap.put("description", description);
        visitMap.put("diagnosis", diagnosis);
        visitMap.put("treatment", treatment);
        visitMap.put("cost", "75.00");
        visitMap.put("completed", completed);
        visitMap.put("emergencyVisit", false);
        visitMap.put("visitDate", "2024-01-15T10:30:00");
        visitMap.put("createdAt", "2024-01-15T10:30:00");
        visitMap.put("updatedAt", "2024-01-15T10:30:00");

        Map<String, Object> petMap = new HashMap<>();
        petMap.put("id", 1L);
        petMap.put("name", "Buddy");
        petMap.put("species", "Dog");
        petMap.put("breed", "Golden Retriever");
        visitMap.put("pet", petMap);

        Map<String, Object> vetMap = new HashMap<>();
        vetMap.put("id", 1L);
        vetMap.put("firstName", "Dr. Jane");
        vetMap.put("lastName", "Smith");
        vetMap.put("specialty", "General Practice");
        visitMap.put("veterinarian", vetMap);

        return visitMap;
    }
}