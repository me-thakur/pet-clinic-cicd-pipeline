package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Owner;
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
 * Owner Service
 * 
 * Service class for managing owner operations through the backend REST API.
 * Provides methods for CRUD operations and search functionality for owners.
 * 
 * Validates: Requirements 8.2, 8.3, 8.4
 */
@Service
public class OwnerService {

    private final WebClient webClient;

    @Autowired
    public OwnerService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get all owners with pagination
     */
    public Mono<Page<Owner>> getAllOwners(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/owners")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertToOwnerPage);
    }

    /**
     * Get owner by ID
     */
    public Mono<Owner> getOwnerById(Long id) {
        return webClient.get()
                .uri("/owners/{id}", id)
                .retrieve()
                .bodyToMono(Owner.class);
    }

    /**
     * Create new owner
     */
    public Mono<Owner> createOwner(Owner owner) {
        return webClient.post()
                .uri("/owners")
                .bodyValue(owner)
                .retrieve()
                .bodyToMono(Owner.class);
    }

    /**
     * Update existing owner
     */
    public Mono<Owner> updateOwner(Long id, Owner owner) {
        return webClient.put()
                .uri("/owners/{id}", id)
                .bodyValue(owner)
                .retrieve()
                .bodyToMono(Owner.class);
    }

    /**
     * Delete owner
     */
    public Mono<Void> deleteOwner(Long id) {
        return webClient.delete()
                .uri("/owners/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Search owners by first name
     */
    public Mono<List<Owner>> searchByFirstName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/owners/search/by-first-name")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToFlux(Owner.class)
                .collectList();
    }

    /**
     * Search owners by last name
     */
    public Mono<List<Owner>> searchByLastName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/owners/search/by-last-name")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToFlux(Owner.class)
                .collectList();
    }

    /**
     * Search owners by email
     */
    public Mono<Owner> searchByEmail(String email) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/owners/search/by-email")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .bodyToMono(Owner.class);
    }

    /**
     * Search owners by city
     */
    public Mono<List<Owner>> searchByCity(String city) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/owners/search/by-city")
                        .queryParam("city", city)
                        .build())
                .retrieve()
                .bodyToFlux(Owner.class)
                .collectList();
    }

    /**
     * Get owners with multiple pets
     */
    public Mono<List<Owner>> getOwnersWithMultiplePets() {
        return webClient.get()
                .uri("/owners/with-multiple-pets")
                .retrieve()
                .bodyToFlux(Owner.class)
                .collectList();
    }

    /**
     * Get owner statistics
     */
    public Mono<Object[]> getOwnerStatistics() {
        return webClient.get()
                .uri("/owners/statistics")
                .retrieve()
                .bodyToMono(Object[].class);
    }

    /**
     * Convert backend page response to Spring Data Page
     */
    @SuppressWarnings("unchecked")
    private Page<Owner> convertToOwnerPage(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        List<Owner> owners = content.stream()
                .map(this::mapToOwner)
                .collect(Collectors.toList());

        Map<String, Object> pageable = (Map<String, Object>) pageResponse.get("pageable");
        int pageNumber = (Integer) pageable.get("pageNumber");
        int pageSize = (Integer) pageable.get("pageSize");
        long totalElements = ((Number) pageResponse.get("totalElements")).longValue();

        return new PageImpl<>(owners, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Map backend owner response to Owner model
     */
    @SuppressWarnings("unchecked")
    private Owner mapToOwner(Map<String, Object> ownerMap) {
        Owner owner = new Owner();
        owner.setId(((Number) ownerMap.get("id")).longValue());
        owner.setFirstName((String) ownerMap.get("firstName"));
        owner.setLastName((String) ownerMap.get("lastName"));
        owner.setAddress((String) ownerMap.get("address"));
        owner.setCity((String) ownerMap.get("city"));
        owner.setTelephone((String) ownerMap.get("telephone"));
        owner.setEmail((String) ownerMap.get("email"));
        
        // Handle date fields if present
        if (ownerMap.get("createdAt") != null) {
            owner.setCreatedAt(java.time.LocalDate.parse((String) ownerMap.get("createdAt")));
        }
        if (ownerMap.get("updatedAt") != null) {
            owner.setUpdatedAt(java.time.LocalDate.parse((String) ownerMap.get("updatedAt")));
        }
        
        return owner;
    }
}