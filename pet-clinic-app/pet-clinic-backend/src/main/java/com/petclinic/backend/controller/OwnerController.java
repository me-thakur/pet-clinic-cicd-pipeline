package com.petclinic.backend.controller;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.repository.OwnerRepository;
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
 * REST Controller for Owner management
 * Provides CRUD operations and search functionality for pet owners
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
@RestController
@RequestMapping("/owners")
@CrossOrigin(origins = "*")
public class OwnerController {

    @Autowired
    private OwnerRepository ownerRepository;

    /**
     * Get all owners with pagination
     * GET /owners
     */
    @GetMapping
    public ResponseEntity<Page<Owner>> getAllOwners(Pageable pageable) {
        try {
            Page<Owner> owners = ownerRepository.findAll(pageable);
            return ResponseEntity.ok(owners);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get owner by ID
     * GET /owners/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Owner> getOwnerById(@PathVariable Long id) {
        try {
            Optional<Owner> owner = ownerRepository.findById(id);
            return owner.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new owner
     * POST /owners
     */
    @PostMapping
    public ResponseEntity<Owner> createOwner(@Valid @RequestBody Owner owner) {
        try {
            // Ensure ID is null for new entities
            owner.setId(null);
            Owner savedOwner = ownerRepository.save(owner);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedOwner);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update existing owner
     * PUT /owners/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Owner> updateOwner(@PathVariable Long id, @Valid @RequestBody Owner ownerDetails) {
        try {
            Optional<Owner> optionalOwner = ownerRepository.findById(id);
            if (optionalOwner.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Owner owner = optionalOwner.get();
            owner.setFirstName(ownerDetails.getFirstName());
            owner.setLastName(ownerDetails.getLastName());
            owner.setAddress(ownerDetails.getAddress());
            owner.setCity(ownerDetails.getCity());
            owner.setTelephone(ownerDetails.getTelephone());
            owner.setEmail(ownerDetails.getEmail());

            Owner updatedOwner = ownerRepository.save(owner);
            return ResponseEntity.ok(updatedOwner);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Delete owner
     * DELETE /owners/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwner(@PathVariable Long id) {
        try {
            if (!ownerRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            ownerRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search owners by first name (case-insensitive)
     * GET /owners/search/by-first-name?name={name}
     */
    @GetMapping("/search/by-first-name")
    public ResponseEntity<List<Owner>> searchByFirstName(@RequestParam String name) {
        try {
            List<Owner> owners = ownerRepository.findByFirstNameContainingIgnoreCase(name);
            return ResponseEntity.ok(owners);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search owners by last name (case-insensitive)
     * GET /owners/search/by-last-name?name={name}
     */
    @GetMapping("/search/by-last-name")
    public ResponseEntity<List<Owner>> searchByLastName(@RequestParam String name) {
        try {
            List<Owner> owners = ownerRepository.findByLastNameContainingIgnoreCase(name);
            return ResponseEntity.ok(owners);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search owners by email
     * GET /owners/search/by-email?email={email}
     */
    @GetMapping("/search/by-email")
    public ResponseEntity<Owner> searchByEmail(@RequestParam String email) {
        try {
            Optional<Owner> owner = ownerRepository.findByEmail(email);
            return owner.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search owners by city
     * GET /owners/search/by-city?city={city}
     */
    @GetMapping("/search/by-city")
    public ResponseEntity<List<Owner>> searchByCity(@RequestParam String city) {
        try {
            List<Owner> owners = ownerRepository.findByCityIgnoreCase(city);
            return ResponseEntity.ok(owners);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get owners with multiple pets
     * GET /owners/with-multiple-pets
     */
    @GetMapping("/with-multiple-pets")
    public ResponseEntity<List<Owner>> getOwnersWithMultiplePets() {
        try {
            List<Owner> owners = ownerRepository.findOwnersWithMultiplePets();
            return ResponseEntity.ok(owners);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get owner statistics
     * GET /owners/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object[]> getOwnerStatistics() {
        try {
            Object[] stats = ownerRepository.getOwnerStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}