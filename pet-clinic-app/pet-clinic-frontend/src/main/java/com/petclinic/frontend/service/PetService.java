package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Owner;
import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.PetWithOwnerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
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
                .map(this::convertBackendPageToPetPage);
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
        // Convert Pet object to Map to avoid content type issues
        Map<String, Object> petData = new HashMap<>();
        
        if (pet.getName() != null) {
            petData.put("name", pet.getName());
        }
        
        if (pet.getSpecies() != null) {
            petData.put("species", pet.getSpecies());
        }
        
        if (pet.getBreed() != null) {
            petData.put("breed", pet.getBreed());
        }
        
        if (pet.getBirthDate() != null) {
            petData.put("birthDate", pet.getBirthDate().toString());
        }
        
        if (pet.getMedicalHistory() != null) {
            petData.put("medicalHistory", pet.getMedicalHistory());
        }
        
        // Set owner
        if (pet.getOwner() != null) {
            Map<String, Object> ownerData = new HashMap<>();
            ownerData.put("id", pet.getOwner().getId());
            petData.put("owner", ownerData);
        }
        
        return webClient.post()
                .uri("/pets")
                .bodyValue(petData)
                .retrieve()
                .bodyToMono(Pet.class);
    }

    /**
     * Update existing pet
     */
    public Mono<Pet> updatePet(Long id, Pet pet) {
        // Convert Pet object to Map to avoid content type issues
        Map<String, Object> petData = new HashMap<>();
        
        if (pet.getName() != null) {
            petData.put("name", pet.getName());
        }
        
        if (pet.getSpecies() != null) {
            petData.put("species", pet.getSpecies());
        }
        
        if (pet.getBreed() != null) {
            petData.put("breed", pet.getBreed());
        }
        
        if (pet.getBirthDate() != null) {
            petData.put("birthDate", pet.getBirthDate().toString());
        }
        
        if (pet.getMedicalHistory() != null) {
            petData.put("medicalHistory", pet.getMedicalHistory());
        }
        
        // Set owner
        if (pet.getOwner() != null) {
            Map<String, Object> ownerData = new HashMap<>();
            ownerData.put("id", pet.getOwner().getId());
            petData.put("owner", ownerData);
        }
        
        return webClient.put()
                .uri("/pets/{id}", id)
                .bodyValue(petData)
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

    // ========================================
    // Enhanced Methods with Owner Information
    // ========================================

    /**
     * Get all pets with owner information using pagination
     */
    public Mono<Page<PetWithOwnerInfo>> getAllPetsWithOwnerInfo(Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/with-owner-info")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertBackendPageToPetWithOwnerInfoPage)
                .onErrorResume(error -> {
                    // Fallback to regular pets endpoint
                    return getAllPets(pageable)
                            .map(this::convertPetPageToPetWithOwnerInfoPage);
                });
    }

    /**
     * Get pet with owner information by ID
     */
    public Mono<PetWithOwnerInfo> getPetWithOwnerInfoById(Long id) {
        return webClient.get()
                .uri("/pets/{id}/with-owner-info", id)
                .retrieve()
                .bodyToMono(PetWithOwnerInfo.class)
                .onErrorResume(error -> {
                    // Fallback to regular pet endpoint
                    return getPetById(id)
                            .map(this::convertPetToPetWithOwnerInfo);
                });
    }

    /**
     * Search pets with owner information
     */
    public Mono<Page<PetWithOwnerInfo>> searchPetsWithOwnerInfo(String searchTerm, Pageable pageable) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pets/search/with-owner-info")
                        .queryParam("q", searchTerm)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertBackendPageToPetWithOwnerInfoPage)
                .onErrorResume(error -> {
                    // Fallback to regular search
                    return searchByName(searchTerm)
                            .map(pets -> convertPetListToPetWithOwnerInfoPage(pets, pageable));
                });
    }

    /**
     * Get pets with owner information by owner ID
     */
    public Mono<List<PetWithOwnerInfo>> getPetsWithOwnerInfoByOwnerId(Long ownerId) {
        return webClient.get()
                .uri("/pets/owner/{ownerId}/with-owner-info", ownerId)
                .retrieve()
                .bodyToFlux(PetWithOwnerInfo.class)
                .collectList()
                .onErrorResume(error -> {
                    // Fallback to regular pets by owner
                    return getPetsByOwnerId(ownerId)
                            .map(pets -> pets.stream()
                                    .map(this::convertPetToPetWithOwnerInfo)
                                    .collect(Collectors.toList()));
                });
    }

    /**
     * Refresh pet-owner information for a specific pet
     */
    public Mono<PetWithOwnerInfo> refreshPetOwnerInfo(Long petId) {
        return webClient.post()
                .uri("/pets/{id}/refresh-owner-info", petId)
                .retrieve()
                .bodyToMono(PetWithOwnerInfo.class)
                .onErrorResume(error -> {
                    // Fallback to regular pet lookup
                    return getPetById(petId)
                            .map(this::convertPetToPetWithOwnerInfo);
                });
    }

    /**
     * Get pet ownership statistics
     */
    public Mono<Map<String, Long>> getPetOwnershipStatistics() {
        return webClient.get()
                .uri("/pets/ownership-statistics")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
                .onErrorResume(error -> {
                    // Fallback to basic statistics
                    return getPetStatistics()
                            .map(stats -> {
                                Map<String, Long> fallbackStats = new HashMap<>();
                                Object totalPets = stats.get("totalPets");
                                long total = totalPets instanceof Number ? ((Number) totalPets).longValue() : 0L;
                                fallbackStats.put("petsWithOwners", total);
                                fallbackStats.put("petsWithoutOwners", 0L);
                                fallbackStats.put("totalPets", total);
                                return fallbackStats;
                            });
                });
    }

    /**
     * Convert backend page response to Spring Data Page
     */
    @SuppressWarnings("unchecked")
    private Page<Pet> convertBackendPageToPetPage(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        List<Pet> pets = content.stream()
                .map(this::mapToPet)
                .collect(Collectors.toList());

        Map<String, Object> pageInfo = (Map<String, Object>) pageResponse.get("page");
        int pageNumber = (Integer) pageInfo.get("number");
        int pageSize = (Integer) pageInfo.get("size");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        return new PageImpl<>(pets, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Convert backend page response to Spring Data Page (legacy method)
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
        
        // Handle owner if present
        if (petMap.get("owner") != null) {
            Map<String, Object> ownerMap = (Map<String, Object>) petMap.get("owner");
            Owner owner = new Owner();
            owner.setId(((Number) ownerMap.get("id")).longValue());
            owner.setFirstName((String) ownerMap.get("firstName"));
            owner.setLastName((String) ownerMap.get("lastName"));
            owner.setEmail((String) ownerMap.get("email"));
            owner.setCity((String) ownerMap.get("city"));
            owner.setTelephone((String) ownerMap.get("telephone"));
            owner.setAddress((String) ownerMap.get("address"));
            pet.setOwner(owner);
        }
        
        return pet;
    }

    // ========================================
    // Helper Methods for PetWithOwnerInfo
    // ========================================

    /**
     * Convert backend page response to PetWithOwnerInfo Page
     */
    @SuppressWarnings("unchecked")
    private Page<PetWithOwnerInfo> convertBackendPageToPetWithOwnerInfoPage(Map<String, Object> pageResponse) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");
        List<PetWithOwnerInfo> pets = content.stream()
                .map(this::mapToPetWithOwnerInfo)
                .collect(Collectors.toList());

        Map<String, Object> pageInfo = (Map<String, Object>) pageResponse.get("page");
        int pageNumber = (Integer) pageInfo.get("number");
        int pageSize = (Integer) pageInfo.get("size");
        long totalElements = ((Number) pageInfo.get("totalElements")).longValue();

        return new PageImpl<>(pets, 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize), 
                totalElements);
    }

    /**
     * Convert Pet Page to PetWithOwnerInfo Page
     */
    private Page<PetWithOwnerInfo> convertPetPageToPetWithOwnerInfoPage(Page<Pet> petPage) {
        List<PetWithOwnerInfo> petsWithOwnerInfo = petPage.getContent().stream()
                .map(this::convertPetToPetWithOwnerInfo)
                .collect(Collectors.toList());

        return new PageImpl<>(petsWithOwnerInfo, petPage.getPageable(), petPage.getTotalElements());
    }

    /**
     * Convert Pet List to PetWithOwnerInfo Page
     */
    private Page<PetWithOwnerInfo> convertPetListToPetWithOwnerInfoPage(List<Pet> pets, Pageable pageable) {
        List<PetWithOwnerInfo> petsWithOwnerInfo = pets.stream()
                .map(this::convertPetToPetWithOwnerInfo)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), petsWithOwnerInfo.size());
        List<PetWithOwnerInfo> pageContent = petsWithOwnerInfo.subList(start, end);

        return new PageImpl<>(pageContent, pageable, petsWithOwnerInfo.size());
    }

    /**
     * Convert Pet to PetWithOwnerInfo
     */
    private PetWithOwnerInfo convertPetToPetWithOwnerInfo(Pet pet) {
        PetWithOwnerInfo petWithOwnerInfo = new PetWithOwnerInfo();
        
        // Set pet information
        petWithOwnerInfo.setPetId(pet.getId());
        petWithOwnerInfo.setPetName(pet.getName());
        petWithOwnerInfo.setPetSpecies(pet.getSpecies());
        petWithOwnerInfo.setPetBreed(pet.getBreed());
        petWithOwnerInfo.setBirthDate(pet.getBirthDate());
        petWithOwnerInfo.setMedicalHistory(pet.getMedicalHistory());
        petWithOwnerInfo.setCreatedAt(pet.getCreatedAt());
        petWithOwnerInfo.setUpdatedAt(pet.getUpdatedAt());
        
        // Set owner information if available
        if (pet.getOwner() != null) {
            petWithOwnerInfo.setOwnerId(pet.getOwner().getId());
            petWithOwnerInfo.setOwnerFirstName(pet.getOwner().getFirstName());
            petWithOwnerInfo.setOwnerLastName(pet.getOwner().getLastName());
            petWithOwnerInfo.setOwnerEmail(pet.getOwner().getEmail());
            petWithOwnerInfo.setOwnerMobileNumber(pet.getOwner().getTelephone());
            petWithOwnerInfo.setOwnerTelephone(pet.getOwner().getTelephone());
            petWithOwnerInfo.setOwnerAddress(pet.getOwner().getAddress());
            petWithOwnerInfo.setOwnerCity(pet.getOwner().getCity());
            petWithOwnerInfo.setOwnerState(pet.getOwner().getState());
            petWithOwnerInfo.setOwnerZipCode(pet.getOwner().getZipCode());
        }
        
        return petWithOwnerInfo;
    }

    /**
     * Map backend PetWithOwnerInfo response to frontend model
     */
    @SuppressWarnings("unchecked")
    private PetWithOwnerInfo mapToPetWithOwnerInfo(Map<String, Object> petMap) {
        PetWithOwnerInfo petWithOwnerInfo = new PetWithOwnerInfo();
        
        // Set pet information
        if (petMap.get("petId") != null) {
            petWithOwnerInfo.setPetId(((Number) petMap.get("petId")).longValue());
        }
        petWithOwnerInfo.setPetName((String) petMap.get("petName"));
        petWithOwnerInfo.setPetSpecies((String) petMap.get("petSpecies"));
        petWithOwnerInfo.setPetBreed((String) petMap.get("petBreed"));
        petWithOwnerInfo.setMedicalHistory((String) petMap.get("medicalHistory"));
        
        // Handle date fields
        if (petMap.get("birthDate") != null) {
            petWithOwnerInfo.setBirthDate(LocalDate.parse((String) petMap.get("birthDate")));
        }
        if (petMap.get("createdAt") != null) {
            petWithOwnerInfo.setCreatedAt(LocalDate.parse((String) petMap.get("createdAt")));
        }
        if (petMap.get("updatedAt") != null) {
            petWithOwnerInfo.setUpdatedAt(LocalDate.parse((String) petMap.get("updatedAt")));
        }
        
        // Set owner information
        if (petMap.get("ownerId") != null) {
            petWithOwnerInfo.setOwnerId(((Number) petMap.get("ownerId")).longValue());
        }
        petWithOwnerInfo.setOwnerFirstName((String) petMap.get("ownerFirstName"));
        petWithOwnerInfo.setOwnerLastName((String) petMap.get("ownerLastName"));
        petWithOwnerInfo.setOwnerEmail((String) petMap.get("ownerEmail"));
        petWithOwnerInfo.setOwnerMobileNumber((String) petMap.get("ownerMobileNumber"));
        petWithOwnerInfo.setOwnerTelephone((String) petMap.get("ownerTelephone"));
        petWithOwnerInfo.setOwnerAddress((String) petMap.get("ownerAddress"));
        petWithOwnerInfo.setOwnerCity((String) petMap.get("ownerCity"));
        petWithOwnerInfo.setOwnerState((String) petMap.get("ownerState"));
        petWithOwnerInfo.setOwnerZipCode((String) petMap.get("ownerZipCode"));
        
        return petWithOwnerInfo;
    }
}