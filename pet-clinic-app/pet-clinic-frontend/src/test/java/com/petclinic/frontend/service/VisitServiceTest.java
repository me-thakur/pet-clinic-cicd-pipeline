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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for VisitService
 * Tests the response handling for completion status from backend API
 * Validates: Requirements 2.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Visit Service Tests")
class VisitServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private VisitService visitService;

    @BeforeEach
    void setUp() {
        // Setup common mock behavior with lenient stubbing to avoid unnecessary stubbing errors
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(webClient.put()).thenReturn(requestBodyUriSpec);
        
        lenient().when(requestHeadersUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestBodyUriSpec.uri(any(String.class), any(Object.class))).thenReturn(requestBodySpec);
        
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("updateVisit should process backend response with completion status")
    void updateVisit_ShouldProcessBackendResponseWithCompletionStatus() {
        // Given - Create a completed visit response
        Visit completedVisit = new Visit();
        completedVisit.setId(1L);
        completedVisit.setVisitDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        completedVisit.setDescription("Routine checkup");
        completedVisit.setDiagnosis("Healthy pet");
        completedVisit.setTreatment("Vaccination administered");
        completedVisit.setCost(BigDecimal.valueOf(75.00));
        completedVisit.setCompleted(true); // Backend sets this to true

        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Buddy");
        completedVisit.setPet(pet);

        Veterinarian vet = new Veterinarian();
        vet.setId(1L);
        vet.setFirstName("Dr. Jane");
        vet.setLastName("Smith");
        completedVisit.setVeterinarian(vet);

        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.just(completedVisit));

        // Create visit to update
        Visit visitToUpdate = new Visit();
        visitToUpdate.setVisitDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        visitToUpdate.setDescription("Routine checkup");
        visitToUpdate.setDiagnosis("Healthy pet");
        visitToUpdate.setTreatment("Vaccination administered");
        visitToUpdate.setCost(BigDecimal.valueOf(75.00));
        
        Pet updatePet = new Pet();
        updatePet.setId(1L);
        visitToUpdate.setPet(updatePet);

        // When - Update visit
        Mono<Visit> result = visitService.updateVisit(1L, visitToUpdate);
        Visit updatedVisit = result.block();

        // Then - Verify completion status is processed from backend response
        assertNotNull(updatedVisit, "Updated visit should not be null");
        assertEquals(1L, updatedVisit.getId(), "Visit ID should match");
        assertEquals("Healthy pet", updatedVisit.getDiagnosis(), "Diagnosis should match");
        assertEquals("Vaccination administered", updatedVisit.getTreatment(), "Treatment should match");
        assertTrue(updatedVisit.getCompleted(), "Completion status should be true from backend response");
        assertEquals(0, BigDecimal.valueOf(75.00).compareTo(updatedVisit.getCost()), "Cost should match");

        // Verify the correct API call was made
        verify(webClient).put();
        verify(requestBodyUriSpec).uri("/visits/{id}", 1L);
        verify(requestBodySpec).bodyValue(any());
        verify(responseSpec).bodyToMono(Visit.class);
    }

    @Test
    @DisplayName("updateVisit should handle incomplete visit response correctly")
    void updateVisit_ShouldHandleIncompleteVisitResponseCorrectly() {
        // Given - Create an incomplete visit response
        Visit incompleteVisit = new Visit();
        incompleteVisit.setId(2L);
        incompleteVisit.setVisitDate(LocalDateTime.of(2024, 1, 16, 14, 0));
        incompleteVisit.setDescription("Initial examination");
        incompleteVisit.setDiagnosis("Examination in progress");
        incompleteVisit.setTreatment(null); // No treatment - should be incomplete
        incompleteVisit.setCost(BigDecimal.valueOf(50.00));
        incompleteVisit.setCompleted(false); // Backend sets this to false

        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Buddy");
        incompleteVisit.setPet(pet);

        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.just(incompleteVisit));

        // Create visit to update
        Visit visitToUpdate = new Visit();
        visitToUpdate.setVisitDate(LocalDateTime.of(2024, 1, 16, 14, 0));
        visitToUpdate.setDescription("Initial examination");
        visitToUpdate.setDiagnosis("Examination in progress");
        visitToUpdate.setCost(BigDecimal.valueOf(50.00));
        
        Pet updatePet = new Pet();
        updatePet.setId(1L);
        visitToUpdate.setPet(updatePet);

        // When - Update visit
        Mono<Visit> result = visitService.updateVisit(2L, visitToUpdate);
        Visit updatedVisit = result.block();

        // Then - Verify completion status is false from backend response
        assertNotNull(updatedVisit, "Updated visit should not be null");
        assertEquals(2L, updatedVisit.getId(), "Visit ID should match");
        assertEquals("Examination in progress", updatedVisit.getDiagnosis(), "Diagnosis should match");
        assertNull(updatedVisit.getTreatment(), "Treatment should be null");
        assertFalse(updatedVisit.getCompleted(), "Completion status should be false from backend response");
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(updatedVisit.getCost()), "Cost should match");

        // Verify the correct API call was made
        verify(webClient).put();
        verify(requestBodyUriSpec).uri("/visits/{id}", 2L);
        verify(requestBodySpec).bodyValue(any());
        verify(responseSpec).bodyToMono(Visit.class);
    }

    @Test
    @DisplayName("updateVisit should handle missing completion status in response")
    void updateVisit_ShouldHandleMissingCompletionStatusInResponse() {
        // Given - Create a visit response without explicit completion status (defaults to false)
        Visit visitWithoutStatus = new Visit();
        visitWithoutStatus.setId(3L);
        visitWithoutStatus.setVisitDate(LocalDateTime.of(2024, 1, 17, 9, 0));
        visitWithoutStatus.setDescription("Follow-up visit");
        visitWithoutStatus.setDiagnosis("Recovery progressing well");
        visitWithoutStatus.setTreatment("Continue medication");
        visitWithoutStatus.setCost(BigDecimal.valueOf(40.00));
        // Don't set completion status - should default to false

        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Buddy");
        visitWithoutStatus.setPet(pet);

        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.just(visitWithoutStatus));

        // Create visit to update
        Visit visitToUpdate = new Visit();
        visitToUpdate.setVisitDate(LocalDateTime.of(2024, 1, 17, 9, 0));
        visitToUpdate.setDescription("Follow-up visit");
        visitToUpdate.setDiagnosis("Recovery progressing well");
        visitToUpdate.setTreatment("Continue medication");
        visitToUpdate.setCost(BigDecimal.valueOf(40.00));
        
        Pet updatePet = new Pet();
        updatePet.setId(1L);
        visitToUpdate.setPet(updatePet);

        // When - Update visit
        Mono<Visit> result = visitService.updateVisit(3L, visitToUpdate);
        Visit updatedVisit = result.block();

        // Then - Should default to false when completion status is missing
        assertNotNull(updatedVisit, "Updated visit should not be null");
        assertEquals(3L, updatedVisit.getId(), "Visit ID should match");
        assertEquals("Recovery progressing well", updatedVisit.getDiagnosis(), "Diagnosis should match");
        assertEquals("Continue medication", updatedVisit.getTreatment(), "Treatment should match");
        assertFalse(updatedVisit.getCompleted(), "Completion status should default to false when missing");
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(updatedVisit.getCost()), "Cost should match");

        // Verify the correct API call was made
        verify(webClient).put();
        verify(requestBodyUriSpec).uri("/visits/{id}", 3L);
        verify(requestBodySpec).bodyValue(any());
        verify(responseSpec).bodyToMono(Visit.class);
    }

    @Test
    @DisplayName("getVisitById should process backend response with completion status")
    void getVisitById_ShouldProcessBackendResponseWithCompletionStatus() {
        // Given - Create a completed visit response
        Visit completedVisit = new Visit();
        completedVisit.setId(1L);
        completedVisit.setVisitDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        completedVisit.setDescription("Routine checkup");
        completedVisit.setDiagnosis("Healthy pet");
        completedVisit.setTreatment("Vaccination administered");
        completedVisit.setCost(BigDecimal.valueOf(75.00));
        completedVisit.setCompleted(true); // Backend sets this to true

        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Buddy");
        completedVisit.setPet(pet);

        Veterinarian vet = new Veterinarian();
        vet.setId(1L);
        vet.setFirstName("Dr. Jane");
        vet.setLastName("Smith");
        completedVisit.setVeterinarian(vet);

        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.just(completedVisit));

        // When - Get visit by ID
        Mono<Visit> result = visitService.getVisitById(1L);
        Visit visit = result.block();

        // Then - Verify completion status is processed from backend response
        assertNotNull(visit, "Visit should not be null");
        assertEquals(1L, visit.getId(), "Visit ID should match");
        assertEquals("Healthy pet", visit.getDiagnosis(), "Diagnosis should match");
        assertEquals("Vaccination administered", visit.getTreatment(), "Treatment should match");
        assertTrue(visit.getCompleted(), "Completion status should be true from backend response");
        assertEquals(0, BigDecimal.valueOf(75.00).compareTo(visit.getCost()), "Cost should match");

        // Verify the correct API call was made
        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/visits/{id}", 1L);
        verify(responseSpec).bodyToMono(Visit.class);
    }
}