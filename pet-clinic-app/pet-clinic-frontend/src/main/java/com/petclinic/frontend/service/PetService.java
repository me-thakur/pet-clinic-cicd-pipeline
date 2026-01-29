package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pet Service
 * 
 * Service class for managing pet operations through the backend REST API.
 * Provides methods for CRUD operations and search functionality for pets.
 * 
 * Validates: Requirements 8.1, 8.2, 8.4
 */
@Service
public class PetService {

    private final WebClient webClient;

    @Autowired
    public PetService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get all pets with pagination
     */
    public Mono<Page<Pet>> getAllPets(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertToPetPage);
    }

    /**
     * Get pet by ID
     */
    public Mono<Pet> getPetById(Long id) {
        return webClient.get()
                .uri("/pets/{id}", id)
                .retrieve()
                .bodyToMono(Pet.class);
    }

    /**
     * Create new pet
     */
    public Mono<Pet> createPet(Pet pet) {
        return webClient.post()
                .uri("/pets")
                .bodyValue(pet)
                .retrieve()
                .bodyToMono(Pet.class);
    }

    /**
     * Update existing pet
     */
    public Mono<Pet> updatePet(Long id, Pet pet) {
        return webClient.put()
                .uri("/pets/{id}", id)
                .bodyValue(pet)
                .retrieve()
                .bodyToMono(Pet.class);
    }

    /**
     * Delete pet
     */
    public Mono<Void> deletePet(Long id) {
        return webClient.delete()
                .uri("/pets/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Get pets by owner ID
     */
    public Mono<List<Pet>> getPetsByOwnerId(Long ownerId) {
        return webClient.get()
                .uri("/pets/owner/{ownerId}", ownerId)
                .retrieve()
                .bodyToFlux(Pet.class)
                .collectList();
    }

    /**
     * Search pets by name
     */
    public Mono<List<Pet>> searchByName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/search")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::extractPetListFromPagedResponse);
    }

    /**
     * Search pets by species
     */
    public Mono<List<Pet>> searchBySpecies(String species) {
        return webClient.get()
                .uri("/pets/species/{species}", species)
                .retrieve()
                .bodyToFlux(Pet.class)
                .collectList();
    }

    /**
     * Search pets by breed
     */
    public Mono<List<Pet>> searchByBreed(String breed) {
        return webClient.get()
                .uri("/pets/breed/{breed}", breed)
                .retrieve()
                .bodyToFlux(Pet.class)
                .collectList();
    }

    /**
     * Search pets by age range
     */
    public Mono<List<Pet>> searchByAgeRange(int minAge, int maxAge) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/age-range")
                        .queryParam("minAge", minAge)
                        .queryParam("maxAge", maxAge)
                        .build())
                .retrieve()
                .bodyToFlux(Pet.class)
                .collectList();
    }

    /**
     * Get senior pets
     */
    public Mono<List<Pet>> getSeniorPets(int age) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/senior")
                        .queryParam("age", age)
                        .build())
                .retrieve()
                .bodyToFlux(Pet.class)
                .collectList();
    }

    /**
     * Search pets by medical history
     */
    public Mono<List<Pet>> searchByMedicalHistory(String keywords) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/medical-history")
                        .queryParam("keywords", keywords)
                        .build())
                .retrieve()
                .bodyToFlux(Pet.class)
                .collectList();
    }

    /**
     * Get pet statistics
     */
    public Mono<Map<String, Object>> getPetStatistics() {
        return webClient.get()
                .uri("/pets/statistics")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get pets by species count
     */
    public Mono<List<Map<String, Object>>> getPetsBySpeciesCount() {
        return webClient.get()
                .uri("/pets/species-count")
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList();
    }

    /**
     * Convert backend page response to Spring Data Page
     */
    @SuppressWarnings("unchecked")
    private Page<Pet> convertToPetPage(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        List<Pet> pets = content.stream()
                .map(this::mapToPet)
                .collect(Collectors.toList());

        Map<String, Object> pageInfo = (Map<String, Object>) pageResponse.get("pageInfo");
        int pageNumber = (Integer) pageInfo.get("pageNumber");
        int pageSize = (Integer) pageInfo.get("pageSize");
        long totalElements = ((Number) pageResponse.get("totalElements")).longValue();

        return new PageImpl<>(pets, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Extract pet list from paged response
     */
    @SuppressWarnings("unchecked")
    private List<Pet> extractPetListFromPagedResponse(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        return content.stream()
                .map(this::mapToPet)
                .collect(Collectors.toList());
    }

    /**
     * Map backend pet response to Pet model
     */
    @SuppressWarnings("unchecked")
    private Pet mapToPet(Map<String, Object> petMap) {
        Pet pet = new Pet();
        pet.setId(((Number) petMap.get("id")).longValue());
        pet.setName((String) petMap.get("name"));
        pet.setSpecies((String) petMap.get("species"));
        pet.setBreed((String) petMap.get("breed"));
        pet.setMedicalHistory((String) petMap.get("medicalHistory"));
        
        // Handle date fields
        if (petMap.get("birthDate") != null) {
            pet.setBirthDate(LocalDate.parse((String) petMap.get("birthDate")));
        }
        if (petMap.get("createdAt") != null) {
            pet.setCreatedAt(LocalDate.parse((String) petMap.get("createdAt")));
        }
        if (petMap.get("updatedAt") != null) {
            pet.setUpdatedAt(LocalDate.parse((String) petMap.get("updatedAt")));
        }
        
        return pet;
    }
}