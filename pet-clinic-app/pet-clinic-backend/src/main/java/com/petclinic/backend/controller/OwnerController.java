package com.petclinic.backend.controller;

import com.petclinic.backend.model.Owner;
import com.petclinic.backend.repository.OwnerRepository;
import com.petclinic.backend.service.SecurityAuditService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Owner management
 * Provides CRUD operations and search functionality for pet owners
 * Implements server-side sorting and pagination for optimal performance
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 */
@RestController
@RequestMapping("/api/owners")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}")
public class OwnerController {

    private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);

    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * Simple test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Backend is working!");
    }

    /**
     * Get all owners with pagination and server-side sorting
     * GET /owners?page=0&size=10&sort=lastName,asc
     * 
     * Supports sorting by: id, firstName, lastName, address, city, telephone, email
     * Default sort: lastName ascending
     */
    @GetMapping
    public ResponseEntity<Page<Owner>> getAllOwners(Pageable pageable) {
        logger.debug("Getting all owners with server-side pagination and sorting: {}", pageable);
        try {
            // Apply default sorting if none specified
            if (pageable.getSort().isUnsorted()) {
                pageable = PageRequest.of(
                    pageable.getPageNumber(), 
                    pageable.getPageSize(), 
                    Sort.by(Sort.Direction.ASC, "lastName")
                );
            }
            
            // Use server-side sorting and pagination - sorting is applied at database level
            Page<Owner> owners = ownerRepository.findAllWithPets(pageable);
            logger.debug("Retrieved {} owners with server-side sorting", owners.getTotalElements());
            return ResponseEntity.ok(owners);
        } catch (Exception e) {
            logger.error("Error in getAllOwners with server-side sorting: {}", e.getMessage(), e);
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
    public ResponseEntity<?> createOwner(@RequestBody Map<String, Object> ownerData) {
        try {
            System.out.println("DEBUG: Creating owner with data: " + ownerData);
            
            // Convert Map to Owner object
            Owner owner = new Owner();
            
            if (ownerData.containsKey("firstName")) {
                owner.setFirstName(ownerData.get("firstName").toString());
            }
            
            if (ownerData.containsKey("lastName")) {
                owner.setLastName(ownerData.get("lastName").toString());
            }
            
            if (ownerData.containsKey("address")) {
                owner.setAddress(ownerData.get("address").toString());
            }
            
            if (ownerData.containsKey("city")) {
                owner.setCity(ownerData.get("city").toString());
            }
            
            if (ownerData.containsKey("state")) {
                owner.setState(ownerData.get("state").toString());
            }
            
            if (ownerData.containsKey("zipCode")) {
                String zipCode = ownerData.get("zipCode").toString();
                System.out.println("DEBUG: ZIP code: " + zipCode);
                owner.setZipCode(zipCode);
            }
            
            if (ownerData.containsKey("telephone")) {
                String telephone = ownerData.get("telephone").toString();
                System.out.println("DEBUG: Telephone: " + telephone);
                owner.setTelephone(telephone);
            }
            
            if (ownerData.containsKey("email")) {
                owner.setEmail(ownerData.get("email").toString());
            }
            
            // Ensure ID is null for new entities
            owner.setId(null);
            
            System.out.println("DEBUG: About to save owner: " + owner);
            Owner savedOwner = ownerRepository.save(owner);
            System.out.println("DEBUG: Successfully saved owner with ID: " + savedOwner.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedOwner);
        } catch (jakarta.validation.ConstraintViolationException e) {
            System.out.println("DEBUG: Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation failed", "details", e.getMessage()));
        } catch (Exception e) {
            System.out.println("DEBUG: General error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to create owner", "details", e.getMessage()));
        }
    }

    /**
     * Update existing owner
     * PUT /owners/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Owner> updateOwner(@PathVariable Long id, @RequestBody Map<String, Object> ownerData) {
        try {
            Optional<Owner> optionalOwner = ownerRepository.findById(id);
            if (optionalOwner.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Owner owner = optionalOwner.get();
            
            // Update fields from Map
            if (ownerData.containsKey("firstName")) {
                owner.setFirstName(ownerData.get("firstName").toString());
            }
            
            if (ownerData.containsKey("lastName")) {
                owner.setLastName(ownerData.get("lastName").toString());
            }
            
            if (ownerData.containsKey("address")) {
                owner.setAddress(ownerData.get("address").toString());
            }
            
            if (ownerData.containsKey("city")) {
                owner.setCity(ownerData.get("city").toString());
            }
            
            if (ownerData.containsKey("state")) {
                owner.setState(ownerData.get("state").toString());
            }
            
            if (ownerData.containsKey("zipCode")) {
                owner.setZipCode(ownerData.get("zipCode").toString());
            }
            
            if (ownerData.containsKey("telephone")) {
                owner.setTelephone(ownerData.get("telephone").toString());
            }
            
            if (ownerData.containsKey("email")) {
                owner.setEmail(ownerData.get("email").toString());
            }

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
     * Bulk delete owners
     * DELETE /owners/bulk
     * 
     * @param request Bulk delete request containing owner IDs
     * @return Bulk operation result
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> bulkDeleteOwners(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            // Validate user can perform bulk operations
            if (!securityAuditService.canPerformBulkOperation(authentication, "owners", "DELETE")) {
                securityAuditService.logAccessDenied("BULK_DELETE", "owners", authentication, 
                    "User does not have permission for bulk delete operations");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: Insufficient permissions for bulk delete operations", "success", false));
            }
            
            logger.debug("Bulk delete owners request: {}", request);
            
            // Extract IDs from request
            @SuppressWarnings("unchecked")
            List<Object> idObjects = (List<Object>) request.get("ids");
            if (idObjects == null || idObjects.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No owner IDs provided", "success", false));
            }
            
            List<Long> ids = idObjects.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(java.util.stream.Collectors.toList());
            
            // Log the bulk delete operation attempt
            Map<String, Object> auditContext = new HashMap<>();
            auditContext.put("totalRequested", ids.size());
            auditContext.put("requestedIds", ids);
            
            securityAuditService.logSecurityOperation("BULK_DELETE_ATTEMPT", "owners", ids, authentication, auditContext);
            
            int totalRequested = ids.size();
            int deletedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long id : ids) {
                try {
                    // Check if owner exists
                    if (!ownerRepository.existsById(id)) {
                        errors.add("Owner with ID " + id + " not found");
                        continue;
                    }
                    
                    // Check if owner has pets - prevent deletion if they do
                    // Note: This assumes there's a method to find pets by owner ID
                    // If not available, we'll skip this check for now
                    
                    // Delete the owner
                    ownerRepository.deleteById(id);
                    deletedCount++;
                    
                    logger.debug("Successfully deleted owner with ID: {}", id);
                    
                } catch (Exception e) {
                    String errorMsg = securityAuditService.sanitizeErrorMessage(e.getMessage(), authentication);
                    logger.error("Error deleting owner with ID {}: {}", id, e.getMessage());
                    errors.add("Failed to delete owner with ID " + id + ": " + errorMsg);
                }
            }
            
            // Log the completion of bulk delete operation
            Map<String, Object> completionContext = new HashMap<>();
            completionContext.put("totalRequested", totalRequested);
            completionContext.put("deletedCount", deletedCount);
            completionContext.put("failedCount", totalRequested - deletedCount);
            completionContext.put("hasErrors", !errors.isEmpty());
            
            securityAuditService.logSecurityOperation("BULK_DELETE_COMPLETED", "owners", ids, authentication, completionContext);
            
            boolean success = deletedCount > 0;
            Map<String, Object> result = Map.of(
                "success", success,
                "deletedCount", deletedCount,
                "totalRequested", totalRequested,
                "failedCount", totalRequested - deletedCount,
                "errors", errors,
                "message", success ? 
                    "Successfully deleted " + deletedCount + " of " + totalRequested + " owners" :
                    "Failed to delete any owners"
            );
            
            System.out.println("DEBUG: Bulk delete owners result: " + result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.out.println("DEBUG: General error in bulk delete owners: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Bulk delete failed", "details", e.getMessage(), "success", false));
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