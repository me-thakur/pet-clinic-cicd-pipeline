package com.petclinic.frontend.service;

import com.petclinic.frontend.dto.PagedResponse;
import com.petclinic.frontend.dto.SeniorPetInfo;
import com.petclinic.frontend.dto.SeniorPetSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Frontend service for senior pet operations
 * Communicates with the backend SeniorPetController API
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@Service
public class SeniorPetService {

    private final WebClient webClient;

    @Autowired
    public SeniorPetService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Search senior pets with comprehensive criteria
     */
    public Mono<PagedResponse<SeniorPetInfo>> searchSeniorPets(
            Integer minAge, Integer maxAge, String species, String breed,
            String healthCondition, String ownerName, String searchTerm,
            String sortBy, String sortDirection, Pageable pageable) {
        
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/pets/senior/search")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .queryParam("sortBy", sortBy != null ? sortBy : "age")
                            .queryParam("sortDirection", sortDirection != null ? sortDirection : "desc");
                    
                    if (minAge != null) builder.queryParam("minAge", minAge);
                    if (maxAge != null) builder.queryParam("maxAge", maxAge);
                    if (species != null && !species.trim().isEmpty()) builder.queryParam("species", species);
                    if (breed != null && !breed.trim().isEmpty()) builder.queryParam("breed", breed);
                    if (healthCondition != null && !healthCondition.trim().isEmpty()) builder.queryParam("healthCondition", healthCondition);
                    if (ownerName != null && !ownerName.trim().isEmpty()) builder.queryParam("ownerName", ownerName);
                    if (searchTerm != null && !searchTerm.trim().isEmpty()) builder.queryParam("searchTerm", searchTerm);
                    
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<SeniorPetInfo>>() {});
    }

    /**
     * Get all senior pets with pagination
     */
    public Mono<PagedResponse<SeniorPetInfo>> getAllSeniorPets(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/senior")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<SeniorPetInfo>>() {});
    }

    /**
     * Get senior pets by species
     */
    public Mono<PagedResponse<SeniorPetInfo>> getSeniorPetsBySpecies(String species, Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/senior/species/{species}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(species))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<SeniorPetInfo>>() {});
    }

    /**
     * Get senior pets with health condition
     */
    public Mono<PagedResponse<SeniorPetInfo>> getSeniorPetsWithHealthCondition(String condition, Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/senior/health-condition")
                        .queryParam("condition", condition)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<SeniorPetInfo>>() {});
    }

    /**
     * Get senior pets needing special care
     */
    public Mono<PagedResponse<SeniorPetInfo>> getSeniorPetsNeedingSpecialCare(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/senior/special-care")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<SeniorPetInfo>>() {});
    }

    /**
     * Get species age thresholds
     */
    public Mono<Map<String, Integer>> getSpeciesAgeThresholds() {
        return webClient.get()
                .uri("/pets/senior/age-thresholds")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Integer>>() {});
    }

    /**
     * Update species age threshold
     */
    public Mono<Map<String, Integer>> updateSpeciesAgeThreshold(String species, Integer threshold) {
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/senior/age-thresholds/{species}")
                        .queryParam("threshold", threshold)
                        .build(species))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Integer>>() {});
    }

    /**
     * Get senior pet statistics
     */
    public Mono<Map<String, Object>> getSeniorPetStatistics() {
        return webClient.get()
                .uri("/pets/senior/statistics")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get available health conditions
     */
    public Mono<List<String>> getAvailableHealthConditions() {
        return webClient.get()
                .uri("/pets/senior/health-conditions")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * Get available species
     */
    public Mono<List<String>> getAvailableSpecies() {
        return webClient.get()
                .uri("/pets/senior/species")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * Check if pet is senior
     */
    public Mono<Map<String, Object>> checkIfPetIsSenior(Long petId) {
        return webClient.get()
                .uri("/pets/senior/check/{petId}", petId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}