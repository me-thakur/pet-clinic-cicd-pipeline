package com.petclinic.backend.controller;

import com.petclinic.backend.model.Veterinarian;
import com.petclinic.backend.repository.VeterinarianRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Veterinarian management
 * Provides CRUD operations and search functionality for veterinarians
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@RestController
@RequestMapping("/veterinarians")
@CrossOrigin(origins = "*")
public class VeterinarianController {

    @Autowired
    private VeterinarianRepository veterinarianRepository;

    /**
     * Get all veterinarians with pagination
     * GET /veterinarians
     */
    @GetMapping
    public ResponseEntity<Page<Veterinarian>> getAllVeterinarians(Pageable pageable) {
        try {
            Page<Veterinarian> veterinarians = veterinarianRepository.findAll(pageable);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get veterinarian by ID
     * GET /veterinarians/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Veterinarian> getVeterinarianById(@PathVariable Long id) {
        try {
            Optional<Veterinarian> veterinarian = veterinarianRepository.findById(id);
            return veterinarian.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new veterinarian
     * POST /veterinarians
     */
    @PostMapping
    public ResponseEntity<Veterinarian> createVeterinarian(@Valid @RequestBody Veterinarian veterinarian) {
        try {
            // Ensure ID is null for new entities
            veterinarian.setId(null);
            Veterinarian savedVeterinarian = veterinarianRepository.save(veterinarian);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedVeterinarian);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update existing veterinarian
     * PUT /veterinarians/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Veterinarian> updateVeterinarian(@PathVariable Long id, @Valid @RequestBody Veterinarian veterinarianDetails) {
        try {
            Optional<Veterinarian> optionalVeterinarian = veterinarianRepository.findById(id);
            if (optionalVeterinarian.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Veterinarian veterinarian = optionalVeterinarian.get();
            veterinarian.setFirstName(veterinarianDetails.getFirstName());
            veterinarian.setLastName(veterinarianDetails.getLastName());
            veterinarian.setSpecialties(veterinarianDetails.getSpecialties());
            veterinarian.setLicenseNumber(veterinarianDetails.getLicenseNumber());

            Veterinarian updatedVeterinarian = veterinarianRepository.save(veterinarian);
            return ResponseEntity.ok(updatedVeterinarian);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete veterinarian
     * DELETE /veterinarians/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeterinarian(@PathVariable Long id) {
        try {
            if (!veterinarianRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            veterinarianRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search veterinarians by first name (case-insensitive)
     * GET /veterinarians/search/by-first-name?name={name}
     */
    @GetMapping("/search/by-first-name")
    public ResponseEntity<List<Veterinarian>> searchByFirstName(@RequestParam String name) {
        try {
            List<Veterinarian> veterinarians = veterinarianRepository.findByFirstNameContainingIgnoreCase(name);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search veterinarians by last name (case-insensitive)
     * GET /veterinarians/search/by-last-name?name={name}
     */
    @GetMapping("/search/by-last-name")
    public ResponseEntity<List<Veterinarian>> searchByLastName(@RequestParam String name) {
        try {
            List<Veterinarian> veterinarians = veterinarianRepository.findByLastNameContainingIgnoreCase(name);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search veterinarians by specialty
     * GET /veterinarians/search/by-specialty?specialty={specialty}
     */
    @GetMapping("/search/by-specialty")
    public ResponseEntity<List<Veterinarian>> searchBySpecialty(@RequestParam String specialty) {
        try {
            List<Veterinarian> veterinarians = veterinarianRepository.findBySpecialtiesContainingIgnoreCase(specialty);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Find veterinarian by license number
     * GET /veterinarians/search/by-license?license={license}
     */
    @GetMapping("/search/by-license")
    public ResponseEntity<Veterinarian> searchByLicenseNumber(@RequestParam String license) {
        try {
            Optional<Veterinarian> veterinarian = veterinarianRepository.findByLicenseNumber(license);
            return veterinarian.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get veterinarians with multiple specialties
     * GET /veterinarians/multi-specialty
     */
    @GetMapping("/multi-specialty")
    public ResponseEntity<List<Veterinarian>> getVeterinariansWithMultipleSpecialties() {
        try {
            List<Veterinarian> veterinarians = veterinarianRepository.findVeterinariansWithMultipleSpecialties();
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available veterinarians (those with fewer visits)
     * GET /veterinarians/available?maxVisits={maxVisits}
     */
    @GetMapping("/available")
    public ResponseEntity<List<Veterinarian>> getAvailableVeterinarians(@RequestParam(defaultValue = "50") int maxVisits) {
        try {
            List<Veterinarian> veterinarians = veterinarianRepository.findAvailableVeterinarians(maxVisits);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get veterinarian statistics
     * GET /veterinarians/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object[]> getVeterinarianStatistics() {
        try {
            Object[] stats = veterinarianRepository.getVeterinarianStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get most common specialties
     * GET /veterinarians/common-specialties?limit={limit}
     */
    @GetMapping("/common-specialties")
    public ResponseEntity<List<Object[]>> getMostCommonSpecialties(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Object[]> specialties = veterinarianRepository.findMostCommonSpecialties(
                org.springframework.data.domain.PageRequest.of(0, limit));
            return ResponseEntity.ok(specialties);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Advanced search with multiple criteria
     * GET /veterinarians/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Veterinarian>> searchVeterinarians(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String licenseNumber,
            Pageable pageable) {
        try {
            Page<Veterinarian> veterinarians = veterinarianRepository.searchVeterinarians(
                firstName, lastName, specialty, licenseNumber, pageable);
            return ResponseEntity.ok(veterinarians);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}