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
import java.util.HashMap;
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
                .map(this::convertToVisitPage)
                .onErrorResume(throwable -> {
                    // Handle API communication failures gracefully
                    System.err.println("Error retrieving visits: " + throwable.getMessage());
                    
                    // Return empty page as fallback
                    return Mono.just(new PageImpl<>(List.of(), pageable, 0));
                });
    }

    /**
     * Get visit by ID
     * 
     * Retrieves visit data from the backend including the computed completion status.
     * The backend automatically calculates completion status based on diagnosis and treatment fields.
     * Handles cases where backend completion status is unavailable and provides fallback display.
     * 
     * @param id The visit ID to retrieve
     * @return Mono<Visit> containing the visit with backend-computed completion status
     */
    public Mono<Visit> getVisitById(Long id) {
        return webClient.get()
                .uri("/visits/{id}", id)
                .retrieve()
                .bodyToMono(Visit.class)
                .onErrorResume(throwable -> {
                    // Handle cases where backend completion status is unavailable
                    // Provide fallback display when API communication fails (Requirements: 6.1)
                    Visit fallbackVisit = new Visit();
                    fallbackVisit.setId(id);
                    fallbackVisit.setCompleted(false); // Default to pending status
                    fallbackVisit.setDescription("Error loading visit data - please try again");
                    
                    // Log the error for debugging
                    System.err.println("Error retrieving visit " + id + ": " + throwable.getMessage());
                    
                    return Mono.just(fallbackVisit);
                });
    }

    /**
     * Create new visit
     */
    public Mono<Visit> createVisit(Visit visit) {
        // Convert Visit object to Map to avoid content type issues
        Map<String, Object> visitData = new HashMap<>();
        
        if (visit.getPet() != null) {
            Map<String, Object> petData = new HashMap<>();
            petData.put("id", visit.getPet().getId());
            visitData.put("pet", petData);
        }
        
        if (visit.getVisitDate() != null) {
            visitData.put("visitDate", visit.getVisitDate().toString());
        }
        
        // Frontend uses "description", backend expects "notes"
        if (visit.getDescription() != null) {
            visitData.put("notes", visit.getDescription());
        }
        
        if (visit.getCost() != null) {
            visitData.put("cost", visit.getCost().toString());
        }
        
        // Add visitType if available (frontend model might not have this)
        visitData.put("visitType", "WELLNESS_EXAM"); // Default value
        
        return webClient.post()
                .uri("/visits")
                .bodyValue(visitData)
                .retrieve()
                .bodyToMono(Visit.class);
    }

    /**
     * Update existing visit
     * 
     * Sends visit data to the backend and processes the response to extract
     * the computed completion status. The backend automatically calculates
     * completion status based on diagnosis and treatment fields.
     * Handles API failures gracefully and provides fallback behavior.
     * 
     * @param id The visit ID to update
     * @param visit The visit data to update
     * @return Mono<Visit> containing the updated visit with backend-computed completion status
     */
    public Mono<Visit> updateVisit(Long id, Visit visit) {
        // Convert Visit object to Map to avoid content type issues
        Map<String, Object> visitData = new HashMap<>();
        
        if (visit.getPet() != null) {
            Map<String, Object> petData = new HashMap<>();
            petData.put("id", visit.getPet().getId());
            visitData.put("pet", petData);
        }
        
        if (visit.getVisitDate() != null) {
            visitData.put("visitDate", visit.getVisitDate().toString());
        }
        
        // Frontend uses "description", backend expects "notes"
        if (visit.getDescription() != null) {
            visitData.put("notes", visit.getDescription());
        }
        
        // Add diagnosis and treatment for completion logic
        if (visit.getDiagnosis() != null) {
            visitData.put("diagnosis", visit.getDiagnosis());
        }
        
        if (visit.getTreatment() != null) {
            visitData.put("treatment", visit.getTreatment());
        }
        
        if (visit.getCost() != null) {
            visitData.put("cost", visit.getCost().toString());
        }
        
        // Add visitType if available (frontend model might not have this)
        visitData.put("visitType", "WELLNESS_EXAM"); // Default value
        
        return webClient.put()
                .uri("/visits/{id}", id)
                .bodyValue(visitData)
                .retrieve()
                .bodyToMono(Visit.class)
                .onErrorResume(throwable -> {
                    // Handle cases where backend completion status is unavailable
                    // Provide fallback display when API communication fails (Requirements: 6.1)
                    System.err.println("Error updating visit " + id + ": " + throwable.getMessage());
                    
                    // Return the original visit with a warning message and default completion status
                    Visit fallbackVisit = new Visit();
                    fallbackVisit.setId(id);
                    fallbackVisit.setCompleted(false); // Default to pending status on error
                    fallbackVisit.setDescription("Update failed - please try again");
                    fallbackVisit.setDiagnosis(visit.getDiagnosis());
                    fallbackVisit.setTreatment(visit.getTreatment());
                    fallbackVisit.setPet(visit.getPet());
                    fallbackVisit.setVisitDate(visit.getVisitDate());
                    fallbackVisit.setCost(visit.getCost());
                    
                    return Mono.just(fallbackVisit);
                });
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
                        .queryParam("startDate", start.toLocalDate().toString())
                        .queryParam("endDate", end.toLocalDate().toString())
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
                .collectList()
                .onErrorResume(throwable -> {
                    // Handle API communication failures gracefully
                    System.err.println("Error retrieving completed visits: " + throwable.getMessage());
                    
                    // Return empty list as fallback
                    return Mono.just(List.of());
                });
    }

    /**
     * Get incomplete visits
     */
    public Mono<List<Visit>> getIncompleteVisits() {
        return webClient.get()
                .uri("/visits/incomplete")
                .retrieve()
                .bodyToFlux(Visit.class)
                .collectList()
                .onErrorResume(throwable -> {
                    // Handle API communication failures gracefully
                    System.err.println("Error retrieving incomplete visits: " + throwable.getMessage());
                    
                    // Return empty list as fallback
                    return Mono.just(List.of());
                });
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

        Map<String, Object> pageInfo = (Map<String, Object>) pageResponse.get("page");
        int pageNumber = (Integer) pageInfo.get("number");
        int pageSize = (Integer) pageInfo.get("size");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        return new PageImpl<>(visits, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Map backend visit response to Visit model
     * Handles cases where completion status might be missing from backend response
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
        
        // Handle boolean fields with error handling
        if (visitMap.get("emergencyVisit") != null) {
            visit.setEmergencyVisit((Boolean) visitMap.get("emergencyVisit"));
        }
        
        // Handle completion status with fallback for API failures (Requirements: 6.1)
        if (visitMap.get("completed") != null) {
            try {
                visit.setCompleted((Boolean) visitMap.get("completed"));
            } catch (Exception e) {
                // If completion status is corrupted or invalid, default to pending
                System.err.println("Error parsing completion status for visit " + visitMap.get("id") + ": " + e.getMessage());
                visit.setCompleted(false); // Default to pending status
            }
        } else {
            // If completion status is missing from backend response, calculate locally as fallback
            String diagnosis = (String) visitMap.get("diagnosis");
            String treatment = (String) visitMap.get("treatment");
            boolean localCompletion = diagnosis != null && !diagnosis.trim().isEmpty() &&
                                    treatment != null && !treatment.trim().isEmpty();
            visit.setCompleted(localCompletion);
            
            System.err.println("Warning: Completion status missing from backend for visit " + visitMap.get("id") + 
                             ". Using local calculation: " + localCompletion);
        }
        
        // Handle date fields with error handling
        try {
            if (visitMap.get("visitDate") != null) {
                visit.setVisitDate(LocalDateTime.parse((String) visitMap.get("visitDate")));
            }
        } catch (Exception e) {
            System.err.println("Error parsing visitDate for visit " + visitMap.get("id") + ": " + e.getMessage());
        }
        
        try {
            if (visitMap.get("createdAt") != null) {
                visit.setCreatedAt(LocalDateTime.parse((String) visitMap.get("createdAt")));
            }
        } catch (Exception e) {
            System.err.println("Error parsing createdAt for visit " + visitMap.get("id") + ": " + e.getMessage());
        }
        
        try {
            if (visitMap.get("updatedAt") != null) {
                visit.setUpdatedAt(LocalDateTime.parse((String) visitMap.get("updatedAt")));
            }
        } catch (Exception e) {
            System.err.println("Error parsing updatedAt for visit " + visitMap.get("id") + ": " + e.getMessage());
        }
        
        // Handle nested objects with error handling
        try {
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
        } catch (Exception e) {
            System.err.println("Error parsing pet data for visit " + visitMap.get("id") + ": " + e.getMessage());
        }
        
        try {
            if (visitMap.get("veterinarian") != null) {
                Map<String, Object> vetMap = (Map<String, Object>) visitMap.get("veterinarian");
                Veterinarian veterinarian = new Veterinarian();
                veterinarian.setId(((Number) vetMap.get("id")).longValue());
                veterinarian.setFirstName((String) vetMap.get("firstName"));
                veterinarian.setLastName((String) vetMap.get("lastName"));
                veterinarian.setSpecialties((String) vetMap.get("specialty"));
                visit.setVeterinarian(veterinarian);
            }
        } catch (Exception e) {
            System.err.println("Error parsing veterinarian data for visit " + visitMap.get("id") + ": " + e.getMessage());
        }
        
        return visit;
    }
}