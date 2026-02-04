package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Visit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisitService error handling
 * Tests error handling robustness for API failures
 * Validates: Requirements 6.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Visit Service Error Handling Tests")
class VisitServiceErrorHandlingTest {
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    private VisitService visitService;
    
    @BeforeEach
    void setUp() {
        visitService = new VisitService(webClient);
    }
    
    @Test
    @DisplayName("Should handle API failure when getting visit by ID")
    void testGetVisitById_ApiFailure() {
        // Given
        Long visitId = 1L;
        WebClientResponseException exception = WebClientResponseException.create(500, "Internal Server Error", null, null, null);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/visits/{id}", visitId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.error(exception));
        
        // When
        Visit result = visitService.getVisitById(visitId).block();
        
        // Then
        assertNotNull(result);
        assertEquals(visitId, result.getId());
        assertFalse(result.getCompleted()); // Should default to pending
        assertEquals("Error loading visit data - please try again", result.getDescription());
    }
    
    @Test
    @DisplayName("Should handle API failure when updating visit")
    void testUpdateVisit_ApiFailure() {
        // Given
        Long visitId = 1L;
        Visit visit = new Visit();
        visit.setId(visitId);
        visit.setDiagnosis("Test diagnosis");
        visit.setTreatment("Test treatment");
        
        WebClientResponseException exception = WebClientResponseException.create(500, "Internal Server Error", null, null, null);
        
        when(webClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/visits/{id}", visitId)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(Map.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.error(exception));
        
        // When
        Visit result = visitService.updateVisit(visitId, visit).block();
        
        // Then
        assertNotNull(result);
        assertEquals(visitId, result.getId());
        assertFalse(result.getCompleted()); // Should default to pending on error
        assertEquals("Update failed - please try again", result.getDescription());
        assertEquals("Test diagnosis", result.getDiagnosis());
        assertEquals("Test treatment", result.getTreatment());
    }
    
    @Test
    @DisplayName("Should handle API failure when getting all visits")
    void testGetAllVisits_ApiFailure() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        WebClientResponseException exception = WebClientResponseException.create(500, "Internal Server Error", null, null, null);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.error(exception));
        
        // When
        Page<Visit> result = visitService.getAllVisits(pageable).block();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Should return empty page as fallback
        assertEquals(0, result.getTotalElements());
    }
    
    @Test
    @DisplayName("Should handle API failure when getting completed visits")
    void testGetCompletedVisits_ApiFailure() {
        // Given
        WebClientResponseException exception = WebClientResponseException.create(500, "Internal Server Error", null, null, null);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/visits/completed")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(reactor.core.publisher.Flux.error(exception));
        
        // When
        List<Visit> result = visitService.getCompletedVisits().block();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Should return empty list as fallback
    }
    
    @Test
    @DisplayName("Should handle API failure when getting incomplete visits")
    void testGetIncompleteVisits_ApiFailure() {
        // Given
        WebClientResponseException exception = WebClientResponseException.create(500, "Internal Server Error", null, null, null);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/visits/incomplete")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Visit.class)).thenReturn(reactor.core.publisher.Flux.error(exception));
        
        // When
        List<Visit> result = visitService.getIncompleteVisits().block();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Should return empty list as fallback
    }
    
    @Test
    @DisplayName("Should handle network timeout gracefully")
    void testGetVisitById_NetworkTimeout() {
        // Given
        Long visitId = 1L;
        RuntimeException timeoutException = new RuntimeException("Connection timeout");
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/visits/{id}", visitId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.error(timeoutException));
        
        // When
        Visit result = visitService.getVisitById(visitId).block();
        
        // Then
        assertNotNull(result);
        assertEquals(visitId, result.getId());
        assertFalse(result.getCompleted()); // Should default to pending
        assertEquals("Error loading visit data - please try again", result.getDescription());
    }
    
    @Test
    @DisplayName("Should handle malformed response gracefully")
    void testGetVisitById_MalformedResponse() {
        // Given
        Long visitId = 1L;
        RuntimeException parseException = new RuntimeException("JSON parse error");
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/visits/{id}", visitId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Visit.class)).thenReturn(Mono.error(parseException));
        
        // When
        Visit result = visitService.getVisitById(visitId).block();
        
        // Then
        assertNotNull(result);
        assertEquals(visitId, result.getId());
        assertFalse(result.getCompleted()); // Should default to pending
        assertEquals("Error loading visit data - please try again", result.getDescription());
    }
}