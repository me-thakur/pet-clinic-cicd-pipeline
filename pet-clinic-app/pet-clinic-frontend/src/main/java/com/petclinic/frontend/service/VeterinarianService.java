package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Veterinarian;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Veterinarian Service
 * 
 * Service class for managing veterinarian operations through the backend REST API.
 * Provides methods for CRUD operations and search functionality for veterinarians.
 * 
 * Validates: Requirements 8.1, 8.3, 8.4
 */
@Service
public class VeterinarianService {

    private final WebClient webClient;

    @Autowired
    public VeterinarianService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get all veterinarians with pagination
     */
    public Mono<Page<Veterinarian>> getAllVeterinarians(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertToVeterinarianPage);
    }

    /**
     * Get veterinarian by ID
     */
    public Mono<Veterinarian> getVeterinarianById(Long id) {
        return webClient.get()
                .uri("/veterinarians/{id}", id)
                .retrieve()
                .bodyToMono(Veterinarian.class);
    }

    /**
     * Create new veterinarian
     */
    public Mono<Veterinarian> createVeterinarian(Veterinarian veterinarian) {
        return webClient.post()
                .uri("/veterinarians")
                .bodyValue(veterinarian)
                .retrieve()
                .bodyToMono(Veterinarian.class);
    }

    /**
     * Update existing veterinarian
     */
    public Mono<Veterinarian> updateVeterinarian(Long id, Veterinarian veterinarian) {
        return webClient.put()
                .uri("/veterinarians/{id}", id)
                .bodyValue(veterinarian)
                .retrieve()
                .bodyToMono(Veterinarian.class);
    }

    /**
     * Delete veterinarian
     */
    public Mono<Void> deleteVeterinarian(Long id) {
        return webClient.delete()
                .uri("/veterinarians/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Search veterinarians by first name
     */
    public Mono<List<Veterinarian>> searchByFirstName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians/search/by-first-name")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToFlux(Veterinarian.class)
                .collectList();
    }

    /**
     * Search veterinarians by last name
     */
    public Mono<List<Veterinarian>> searchByLastName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians/search/by-last-name")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToFlux(Veterinarian.class)
                .collectList();
    }

    /**
     * Search veterinarians by specialty
     */
    public Mono<List<Veterinarian>> searchBySpecialty(String specialty) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians/search/by-specialty")
                        .queryParam("specialty", specialty)
                        .build())
                .retrieve()
                .bodyToFlux(Veterinarian.class)
                .collectList();
    }

    /**
     * Search veterinarian by license number
     */
    public Mono<Veterinarian> searchByLicenseNumber(String license) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians/search/by-license")
                        .queryParam("license", license)
                        .build())
                .retrieve()
                .bodyToMono(Veterinarian.class);
    }

    /**
     * Get veterinarians with multiple specialties
     */
    public Mono<List<Veterinarian>> getVeterinariansWithMultipleSpecialties() {
        return webClient.get()
                .uri("/veterinarians/multi-specialty")
                .retrieve()
                .bodyToFlux(Veterinarian.class)
                .collectList();
    }

    /**
     * Get available veterinarians
     */
    public Mono<List<Veterinarian>> getAvailableVeterinarians(int maxVisits) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians/available")
                        .queryParam("maxVisits", maxVisits)
                        .build())
                .retrieve()
                .bodyToFlux(Veterinarian.class)
                .collectList();
    }

    /**
     * Get veterinarian statistics
     */
    public Mono<Object[]> getVeterinarianStatistics() {
        return webClient.get()
                .uri("/veterinarians/statistics")
                .retrieve()
                .bodyToMono(Object[].class);
    }

    /**
     * Get most common specialties
     */
    public Mono<List<Object[]>> getMostCommonSpecialties(int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/veterinarians/common-specialties")
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToFlux(Object[].class)
                .collectList();
    }

    /**
     * Advanced search veterinarians
     */
    public Mono<Page<Veterinarian>> searchVeterinarians(
            String firstName, String lastName, String specialty, Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/veterinarians/search")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize());
                    
                    if (firstName != null && !firstName.trim().isEmpty()) {
                        builder.queryParam("firstName", firstName.trim());
                    }
                    if (lastName != null && !lastName.trim().isEmpty()) {
                        builder.queryParam("lastName", lastName.trim());
                    }
                    if (specialty != null && !specialty.trim().isEmpty()) {
                        builder.queryParam("specialty", specialty.trim());
                    }
                    
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertToVeterinarianPage);
    }

    /**
     * Convert backend page response to Spring Data Page
     */
    @SuppressWarnings("unchecked")
    private Page<Veterinarian> convertToVeterinarianPage(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        List<Veterinarian> veterinarians = content.stream()
                .map(this::mapToVeterinarian)
                .collect(Collectors.toList());

        Map<String, Object> pageInfo = (Map<String, Object>) pageResponse.get("page");
        int pageNumber = (Integer) pageInfo.get("number");
        int pageSize = (Integer) pageInfo.get("size");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        return new PageImpl<>(veterinarians, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Map backend veterinarian response to Veterinarian model
     */
    @SuppressWarnings("unchecked")
    private Veterinarian mapToVeterinarian(Map<String, Object> vetMap) {
        Veterinarian veterinarian = new Veterinarian();
        veterinarian.setId(((Number) vetMap.get("id")).longValue());
        veterinarian.setFirstName((String) vetMap.get("firstName"));
        veterinarian.setLastName((String) vetMap.get("lastName"));
        veterinarian.setSpecialties((String) vetMap.get("specialties"));
        veterinarian.setLicenseNumber((String) vetMap.get("licenseNumber"));
        
        // Map contact information
        veterinarian.setTelephone((String) vetMap.get("telephone"));
        veterinarian.setEmail((String) vetMap.get("email"));
        veterinarian.setAddress((String) vetMap.get("address"));
        
        // Handle date fields if present
        if (vetMap.get("createdAt") != null) {
            veterinarian.setCreatedAt(java.time.LocalDate.parse((String) vetMap.get("createdAt")));
        }
        if (vetMap.get("updatedAt") != null) {
            veterinarian.setUpdatedAt(java.time.LocalDate.parse((String) vetMap.get("updatedAt")));
        }
        
        return veterinarian;
    }
}