package com.petclinic.frontend.service;

import com.petclinic.frontend.model.Owner;
import com.petclinic.frontend.model.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
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
        // Convert Owner object to Map to avoid content type issues
        Map<String, Object> ownerData = new HashMap<>();
        
        if (owner.getFirstName() != null) {
            ownerData.put("firstName", owner.getFirstName());
        }
        
        if (owner.getLastName() != null) {
            ownerData.put("lastName", owner.getLastName());
        }
        
        if (owner.getAddress() != null) {
            ownerData.put("address", owner.getAddress());
        }
        
        if (owner.getCity() != null) {
            ownerData.put("city", owner.getCity());
        }
        
        if (owner.getState() != null) {
            ownerData.put("state", owner.getState());
        }
        
        if (owner.getZipCode() != null) {
            ownerData.put("zipCode", owner.getZipCode());
        }
        
        if (owner.getTelephone() != null) {
            ownerData.put("telephone", owner.getTelephone());
        }
        
        if (owner.getEmail() != null) {
            ownerData.put("email", owner.getEmail());
        }
        
        System.out.println("DEBUG: Creating owner with data: " + ownerData);
        
        return webClient.post()
                .uri("/owners")
                .bodyValue(ownerData)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), 
                         clientResponse -> {
                             System.out.println("DEBUG: Client error response status: " + clientResponse.statusCode());
                             return clientResponse.bodyToMono(String.class)
                                 .defaultIfEmpty("") // Handle empty response body
                                 .map(body -> {
                                     System.out.println("DEBUG: Error response body: '" + body + "'");
                                     
                                     // Handle specific error cases
                                     if (clientResponse.statusCode().value() == 400) {
                                         if (body.contains("email") || body.contains("unique") || body.contains("duplicate")) {
                                             return new RuntimeException("EMAIL_DUPLICATE");
                                         } else if (body.contains("validation") || body.contains("Validation")) {
                                             return new RuntimeException("VALIDATION_ERROR");
                                         } else {
                                             // For 400 with empty body, likely a constraint violation
                                             return new RuntimeException("EMAIL_DUPLICATE");
                                         }
                                     } else if (clientResponse.statusCode().value() == 401) {
                                         return new RuntimeException("AUTHENTICATION_ERROR");
                                     } else if (clientResponse.statusCode().value() == 403) {
                                         return new RuntimeException("AUTHORIZATION_ERROR");
                                     } else {
                                         return new RuntimeException("CLIENT_ERROR: " + clientResponse.statusCode());
                                     }
                                 });
                         })
                .onStatus(status -> status.is5xxServerError(), 
                         clientResponse -> {
                             System.out.println("DEBUG: Server error response status: " + clientResponse.statusCode());
                             return clientResponse.bodyToMono(String.class)
                                 .defaultIfEmpty("")
                                 .map(body -> {
                                     System.out.println("DEBUG: Server error response body: '" + body + "'");
                                     return new RuntimeException("SERVER_ERROR");
                                 });
                         })
                .bodyToMono(Owner.class)
                .doOnSuccess(savedOwner -> System.out.println("DEBUG: Successfully created owner: " + savedOwner))
                .doOnError(error -> System.out.println("DEBUG: Error creating owner: " + error.getMessage()));
    }

    /**
     * Update existing owner
     */
    public Mono<Owner> updateOwner(Long id, Owner owner) {
        // Convert Owner object to Map to avoid content type issues
        Map<String, Object> ownerData = new HashMap<>();
        
        if (owner.getFirstName() != null) {
            ownerData.put("firstName", owner.getFirstName());
        }
        
        if (owner.getLastName() != null) {
            ownerData.put("lastName", owner.getLastName());
        }
        
        if (owner.getAddress() != null) {
            ownerData.put("address", owner.getAddress());
        }
        
        if (owner.getCity() != null) {
            ownerData.put("city", owner.getCity());
        }
        
        if (owner.getState() != null) {
            ownerData.put("state", owner.getState());
        }
        
        if (owner.getZipCode() != null) {
            ownerData.put("zipCode", owner.getZipCode());
        }
        
        if (owner.getTelephone() != null) {
            ownerData.put("telephone", owner.getTelephone());
        }
        
        if (owner.getEmail() != null) {
            ownerData.put("email", owner.getEmail());
        }
        
        return webClient.put()
                .uri("/owners/{id}", id)
                .bodyValue(ownerData)
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
        owner.setState((String) ownerMap.get("state"));
        owner.setZipCode((String) ownerMap.get("zipCode"));
        owner.setTelephone((String) ownerMap.get("telephone"));
        owner.setEmail((String) ownerMap.get("email"));
        
        // Map pets if present
        if (ownerMap.get("pets") != null) {
            List<Map<String, Object>> petsData = (List<Map<String, Object>>) ownerMap.get("pets");
            List<Pet> pets = petsData.stream()
                    .map(this::mapToPet)
                    .collect(Collectors.toList());
            owner.setPets(pets);
        }
        
        // Handle date fields if present
        if (ownerMap.get("createdAt") != null) {
            owner.setCreatedAt(java.time.LocalDate.parse((String) ownerMap.get("createdAt")));
        }
        if (ownerMap.get("updatedAt") != null) {
            owner.setUpdatedAt(java.time.LocalDate.parse((String) ownerMap.get("updatedAt")));
        }
        
        return owner;
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
        
        if (petMap.get("birthDate") != null) {
            pet.setBirthDate(java.time.LocalDate.parse((String) petMap.get("birthDate")));
        }
        
        return pet;
    }
}