package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Visit;
import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.Veterinarian;
import com.petclinic.frontend.model.Owner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Visit Service
 * 
 * Service class for managing visit operations through the backend REST API.
 * Provides methods for CRUD operations and search functionality for visits.
 * 
 * Validates: Requirements 8.1, 8.3, 8.4
 */
@Service
public class VisitService {

    private final WebClient webClient;

    @Autowired
    public VisitService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get all visits with pagination
     */
    public Mono<Page<Visit>> getAllVisits(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertToVisitPage);
    }

    /**
     * Get visit by ID
     */
    public Mono<Visit> getVisitById(Long id) {
        return webClient.get()
                .uri("/visits/{id}", id)
                .retrieve()
                .bodyToMono(Visit.class);
    }

    /**
     * Create new visit
     */
    public Mono<Visit> createVisit(Visit visit) {
        return webClient.post()
                .uri("/visits")
                .bodyValue(visit)
                .retrieve()
                .bodyToMono(Visit.class);
    }

    /**
     * Update existing visit
     */
    public Mono<Visit> updateVisit(Long id, Visit visit) {
        return webClient.put()
                .uri("/visits/{id}", id)
                .bodyValue(visit)
                .retrieve()
                .bodyToMono(Visit.class);
    }

    /**
     * Delete visit
     */
    public Mono<Void> deleteVisit(Long id) {
        return webClient.delete()
                .uri("/visits/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Get visits by pet ID
     */
    public Mono<List<Visit>> getVisitsByPetId(Long petId) {
        return webClient.get()
                .uri("/visits/pet/{petId}", petId)
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get visits by owner ID
     */
    public Mono<List<Visit>> getVisitsByOwnerId(Long ownerId) {
        return webClient.get()
                .uri("/visits/owner/{ownerId}", ownerId)
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get visits by veterinarian ID
     */
    public Mono<List<Visit>> getVisitsByVeterinarianId(Long veterinarianId) {
        return webClient.get()
                .uri("/visits/veterinarian/{veterinarianId}", veterinarianId)
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get visits by date range
     */
    public Mono<List<Visit>> getVisitsByDateRange(LocalDateTime start, LocalDateTime end) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits/date-range")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .build())
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Search visits by description
     */
    public Mono<List<Visit>> searchByDescription(String text) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits/search/by-description")
                        .queryParam("text", text)
                        .build())
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Search visits by diagnosis
     */
    public Mono<List<Visit>> searchByDiagnosis(String text) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits/search/by-diagnosis")
                        .queryParam("text", text)
                        .build())
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Search visits by treatment
     */
    public Mono<List<Visit>> searchByTreatment(String text) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits/search/by-treatment")
                        .queryParam("text", text)
                        .build())
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get visits by cost range
     */
    public Mono<List<Visit>> getVisitsByCostRange(BigDecimal min, BigDecimal max) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits/cost-range")
                        .queryParam("min", min.toString())
                        .queryParam("max", max.toString())
                        .build())
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get emergency visits
     */
    public Mono<List<Visit>> getEmergencyVisits() {
        return webClient.get()
                .uri("/visits/emergency")
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get completed visits
     */
    public Mono<List<Visit>> getCompletedVisits() {
        return webClient.get()
                .uri("/visits/completed")
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get incomplete visits
     */
    public Mono<List<Visit>> getIncompleteVisits() {
        return webClient.get()
                .uri("/visits/incomplete")
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get today's visits
     */
    public Mono<List<Visit>> getTodaysVisits() {
        return webClient.get()
                .uri("/visits/today")
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get upcoming visits
     */
    public Mono<List<Visit>> getUpcomingVisits() {
        return webClient.get()
                .uri("/visits/upcoming")
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList();
    }

    /**
     * Get visit statistics
     */
    public Mono<Object[]> getVisitStatistics() {
        return webClient.get()
                .uri("/visits/statistics")
                .retrieve()
                .bodyToMono(Object[].class);
    }

    /**
     * Calculate revenue for date range
     */
    public Mono<BigDecimal> calculateRevenue(LocalDateTime start, LocalDateTime end) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/visits/revenue")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .build())
                .retrieve()
                .bodyToMono(BigDecimal.class);
    }

    /**
     * Convert backend page response to Spring Data Page
     */
    @SuppressWarnings("unchecked")
    private Page<Visit> convertToVisitPage(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        List<Visit> visits = content.stream()
                .map(this::mapToVisit)
                .collect(Collectors.toList());

        Map<String, Object> pageable = (Map<String, Object>) pageResponse.get("pageable");
        int pageNumber = (Integer) pageable.get("pageNumber");
        int pageSize = (Integer) pageable.get("pageSize");
        long totalElements = ((Number) pageResponse.get("totalElements")).longValue();

        return new PageImpl<>(visits, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Map backend visit response to Visit model
     */
    @SuppressWarnings("unchecked")
    private Visit mapToVisit(Map<String, Object> visitMap) {
        Visit visit = new Visit();
        visit.setId(((Number) visitMap.get("id")).longValue());
        visit.setDescription((String) visitMap.get("description"));
        visit.setDiagnosis((String) visitMap.get("diagnosis"));
        visit.setTreatment((String) visitMap.get("treatment"));
        
        if (visitMap.get("cost") != null) {
            visit.setCost(new BigDecimal(visitMap.get("cost").toString()));
        }
        
        // Handle boolean fields
        if (visitMap.get("emergencyVisit") != null) {
            visit.setEmergencyVisit((Boolean) visitMap.get("emergencyVisit"));
        }
        if (visitMap.get("completed") != null) {
            visit.setCompleted((Boolean) visitMap.get("completed"));
        }
        
        // Handle date fields
        if (visitMap.get("visitDate") != null) {
            visit.setVisitDate(LocalDateTime.parse((String) visitMap.get("visitDate")));
        }
        if (visitMap.get("createdAt") != null) {
            visit.setCreatedAt(LocalDateTime.parse((String) visitMap.get("createdAt")));
        }
        if (visitMap.get("updatedAt") != null) {
            visit.setUpdatedAt(LocalDateTime.parse((String) visitMap.get("updatedAt")));
        }
        
        // Handle nested objects
        if (visitMap.get("pet") != null) {
            Map<String, Object> petMap = (Map<String, Object>) visitMap.get("pet");
            Pet pet = new Pet();
            pet.setId(((Number) petMap.get("id")).longValue());
            pet.setName((String) petMap.get("name"));
            pet.setSpecies((String) petMap.get("species"));
            pet.setBreed((String) petMap.get("breed"));
            
            // Handle pet owner if present
            if (petMap.get("owner") != null) {
                Map<String, Object> ownerMap = (Map<String, Object>) petMap.get("owner");
                Owner owner = new Owner();
                owner.setId(((Number) ownerMap.get("id")).longValue());
                owner.setFirstName((String) ownerMap.get("firstName"));
                owner.setLastName((String) ownerMap.get("lastName"));
                pet.setOwner(owner);
            }
            
            visit.setPet(pet);
        }
        
        if (visitMap.get("veterinarian") != null) {
            Map<String, Object> vetMap = (Map<String, Object>) visitMap.get("veterinarian");
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setId(((Number) vetMap.get("id")).longValue());
            veterinarian.setFirstName((String) vetMap.get("firstName"));
            veterinarian.setLastName((String) vetMap.get("lastName"));
            veterinarian.setSpecialties((String) vetMap.get("specialty"));
            visit.setVeterinarian(veterinarian);
        }
        
        return visit;
    }
}