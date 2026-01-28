package com.petclinic.backend.controller;

import com.petclinic.backend.model.Pet;
import com.petclinic.backend.repository.PetRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Pet management
 * Provides CRUD operations and search functionality for pets
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@RestController
@RequestMapping("/api/pets")
@CrossOrigin(origins = "*")
public class PetController {

    @Autowired
    private PetRepository petRepository;

    /**
     * Get all pets with pagination
     * GET /api/pets
     */
    @GetMapping
    public ResponseEntity<Page<Pet>> getAllPets(Pageable pageable) {
        try {
            Page<Pet> pets = petRepository.findAll(pageable);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pet by ID
     * GET /api/pets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pet> getPetById(@PathVariable Long id) {
        try {
            Optional<Pet> pet = petRepository.findById(id);
            return pet.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new pet
     * POST /api/pets
     */
    @PostMapping
    public ResponseEntity<Pet> createPet(@Valid @RequestBody Pet pet) {
        try {
            // Ensure ID is null for new entities
            pet.setId(null);
            Pet savedPet = petRepository.save(pet);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPet);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update existing pet
     * PUT /api/pets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(@PathVariable Long id, @Valid @RequestBody Pet petDetails) {
        try {
            Optional<Pet> optionalPet = petRepository.findById(id);
            if (optionalPet.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Pet pet = optionalPet.get();
            pet.setName(petDetails.getName());
            pet.setSpecies(petDetails.getSpecies());
            pet.setBreed(petDetails.getBreed());
            pet.setBirthDate(petDetails.getBirthDate());
            pet.setMedicalHistory(petDetails.getMedicalHistory());
            // Note: Owner should be updated through separate endpoint to maintain referential integrity

            Pet updatedPet = petRepository.save(pet);
            return ResponseEntity.ok(updatedPet);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete pet
     * DELETE /api/pets/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        try {
            if (!petRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            petRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pets by owner ID
     * GET /api/pets/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Pet>> getPetsByOwnerId(@PathVariable Long ownerId) {
        try {
            List<Pet> pets = petRepository.findByOwnerId(ownerId);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search pets by name (case-insensitive)
     * GET /api/pets/search/by-name?name={name}
     */
    @GetMapping("/search/by-name")
    public ResponseEntity<List<Pet>> searchByName(@RequestParam String name) {
        try {
            List<Pet> pets = petRepository.findByNameContainingIgnoreCase(name);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search pets by species (case-insensitive)
     * GET /api/pets/search/by-species?species={species}
     */
    @GetMapping("/search/by-species")
    public ResponseEntity<List<Pet>> searchBySpecies(@RequestParam String species) {
        try {
            List<Pet> pets = petRepository.findBySpeciesIgnoreCase(species);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search pets by breed (case-insensitive)
     * GET /api/pets/search/by-breed?breed={breed}
     */
    @GetMapping("/search/by-breed")
    public ResponseEntity<List<Pet>> searchByBreed(@RequestParam String breed) {
        try {
            List<Pet> pets = petRepository.findByBreedContainingIgnoreCase(breed);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pets by age range
     * GET /api/pets/search/by-age-range?minAge={minAge}&maxAge={maxAge}
     */
    @GetMapping("/search/by-age-range")
    public ResponseEntity<List<Pet>> searchByAgeRange(@RequestParam int minAge, @RequestParam int maxAge) {
        try {
            LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
            LocalDate minBirthDate = LocalDate.now().minusYears(maxAge + 1);
            List<Pet> pets = petRepository.findByBirthDateBetween(minBirthDate, maxBirthDate);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get senior pets (older than specified age)
     * GET /api/pets/senior?age={age}
     */
    @GetMapping("/senior")
    public ResponseEntity<List<Pet>> getSeniorPets(@RequestParam(defaultValue = "7") int age) {
        try {
            LocalDate cutoffDate = LocalDate.now().minusYears(age);
            List<Pet> pets = petRepository.findSeniorPets(cutoffDate);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pets with medical history containing specific text
     * GET /api/pets/search/by-medical-history?text={text}
     */
    @GetMapping("/search/by-medical-history")
    public ResponseEntity<List<Pet>> searchByMedicalHistory(@RequestParam String text) {
        try {
            List<Pet> pets = petRepository.findByMedicalHistoryContainingIgnoreCase(text);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pet statistics
     * GET /api/pets/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object[]> getPetStatistics() {
        try {
            Object[] stats = petRepository.getPetStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pets by species count
     * GET /api/pets/species-count
     */
    @GetMapping("/species-count")
    public ResponseEntity<List<Object[]>> getPetsBySpeciesCount() {
        try {
            List<Object[]> speciesCount = petRepository.countPetsBySpecies();
            return ResponseEntity.ok(speciesCount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}