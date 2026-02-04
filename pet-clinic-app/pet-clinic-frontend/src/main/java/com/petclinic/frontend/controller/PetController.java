package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.PetWithOwnerInfo;
import com.petclinic.frontend.model.Owner;
import com.petclinic.frontend.service.PetService;
import com.petclinic.frontend.service.OwnerService;
import com.petclinic.frontend.service.SeniorPetService;
import com.petclinic.frontend.dto.PagedResponse;
import com.petclinic.frontend.dto.SeniorPetInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pet Controller
 * 
 * Handles web requests for pet management operations.
 * Provides endpoints for listing, creating, editing, and deleting pets.
 * 
 * Validates: Requirements 8.1, 8.2, 8.4
 */
@Controller
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;
    private final OwnerService ownerService;
    private final SeniorPetService seniorPetService;

    @Autowired
    public PetController(PetService petService, OwnerService ownerService, SeniorPetService seniorPetService) {
        this.petService = petService;
        this.ownerService = ownerService;
        this.seniorPetService = seniorPetService;
    }

    /**
     * List all pets in standard view (basic pet information only)
     */
    @GetMapping
    public String listPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Pet> pets = petService.getAllPets(pageable).block();
            
            model.addAttribute("pets", pets);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pets.getTotalPages());
            model.addAttribute("totalElements", pets.getTotalElements());
            model.addAttribute("enhancedView", false); // Standard view - basic pet info only
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading pets: " + e.getMessage());
            // Create empty page for error state
            model.addAttribute("pets", Page.empty());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("enhancedView", false);
            return "pets/list";
        }
    }

    /**
     * Show pet details
     */
    @GetMapping("/{id}")
    public String showPet(@PathVariable Long id, Model model) {
        Pet pet = petService.getPetById(id).block();
        if (pet == null) {
            model.addAttribute("error", "Pet not found");
            return "redirect:/pets";
        }
        
        model.addAttribute("pet", pet);
        return "pets/details";
    }

    /**
     * Show create pet form
     */
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) Long ownerId,
            Model model) {
        
        Pet pet = new Pet();
        if (ownerId != null) {
            pet.setOwnerId(ownerId);
        }
        
        model.addAttribute("pet", pet);
        
        // No need to load all owners - using searchable dropdown
        return "pets/form";
    }

    /**
     * Process create pet form
     */
    @PostMapping("/new")
    public String createPet(
            @Valid @ModelAttribute Pet pet,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            // No need to reload owners - using searchable dropdown
            return "pets/form";
        }

        try {
            Pet savedPet = petService.createPet(pet).block();
            redirectAttributes.addFlashAttribute("success", "Pet created successfully!");
            return "redirect:/pets/" + savedPet.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Error creating pet: " + e.getMessage());
            return "pets/form";
        }
    }

    /**
     * Show edit pet form
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Pet pet = petService.getPetById(id).block();
        if (pet == null) {
            model.addAttribute("error", "Pet not found");
            return "redirect:/pets";
        }
        
        // Set ownerId for form binding
        if (pet.getOwner() != null) {
            pet.setOwnerId(pet.getOwner().getId());
        }
        
        model.addAttribute("pet", pet);
        
        // No need to load all owners - using searchable dropdown
        return "pets/form";
    }

    /**
     * Process edit pet form
     */
    @PostMapping("/{id}/edit")
    public String updatePet(
            @PathVariable Long id,
            @Valid @ModelAttribute Pet pet,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            pet.setId(id);
            // No need to reload owners - using searchable dropdown
            return "pets/form";
        }

        try {
            petService.updatePet(id, pet).block();
            redirectAttributes.addFlashAttribute("success", "Pet updated successfully!");
            return "redirect:/pets/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Error updating pet: " + e.getMessage());
            pet.setId(id);
            return "pets/form";
        }
    }

    /**
     * Delete pet
     */
    @PostMapping("/{id}/delete")
    public String deletePet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            petService.deletePet(id).block();
            redirectAttributes.addFlashAttribute("success", "Pet deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting pet: " + e.getMessage());
        }
        return "redirect:/pets";
    }

    /**
     * Search pets in standard view
     */
    @GetMapping("/search")
    public String searchPets(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String medicalHistory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            List<Pet> pets = null;
            
            if (name != null && !name.trim().isEmpty()) {
                pets = petService.searchByName(name.trim()).block();
            } else if (species != null && !species.trim().isEmpty()) {
                pets = petService.searchBySpecies(species.trim()).block();
            } else if (breed != null && !breed.trim().isEmpty()) {
                pets = petService.searchByBreed(breed.trim()).block();
            } else if (minAge != null && maxAge != null) {
                pets = petService.searchByAgeRange(minAge, maxAge).block();
            } else if (medicalHistory != null && !medicalHistory.trim().isEmpty()) {
                pets = petService.searchByMedicalHistory(medicalHistory.trim()).block();
            } else {
                // No search criteria, redirect to list
                return "redirect:/pets";
            }
            
            model.addAttribute("pets", pets);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("searchName", name);
            model.addAttribute("searchSpecies", species);
            model.addAttribute("searchBreed", breed);
            model.addAttribute("searchMinAge", minAge);
            model.addAttribute("searchMaxAge", maxAge);
            model.addAttribute("searchMedicalHistory", medicalHistory);
            model.addAttribute("enhancedView", false); // Standard view for search
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching pets: " + e.getMessage());
            model.addAttribute("enhancedView", false);
            return "pets/list";
        }
    }

    /**
     * Show pets by owner in standard view
     */
    @GetMapping("/owner/{ownerId}")
    public String petsByOwner(@PathVariable Long ownerId, Model model) {
        try {
            List<Pet> pets = petService.getPetsByOwnerId(ownerId).block();
            Owner owner = ownerService.getOwnerById(ownerId).block();
            
            model.addAttribute("pets", pets);
            model.addAttribute("owner", owner);
            model.addAttribute("filterByOwner", true);
            model.addAttribute("enhancedView", false); // Standard view
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading pets for owner: " + e.getMessage());
            return "redirect:/pets";
        }
    }

    /**
     * Show senior pets with comprehensive search functionality
     */
    @GetMapping("/senior")
    public String seniorPets(
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String healthCondition,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "age") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            
            // Use the new SeniorPetService to call backend API
            PagedResponse<SeniorPetInfo> seniorPetsResponse = seniorPetService.searchSeniorPets(
                minAge, maxAge, species, breed, healthCondition, ownerName, searchTerm,
                sortBy, sortDirection, pageable
            ).block();
            
            model.addAttribute("pets", seniorPetsResponse.getContent());
            model.addAttribute("pageInfo", seniorPetsResponse.getPage());
            model.addAttribute("searchPerformed", hasSearchCriteria(minAge, maxAge, species, breed, healthCondition, ownerName, searchTerm));
            
            // Add search parameters to model for form persistence
            model.addAttribute("searchMinAge", minAge);
            model.addAttribute("searchMaxAge", maxAge);
            model.addAttribute("searchSpecies", species);
            model.addAttribute("searchBreed", breed);
            model.addAttribute("searchHealthCondition", healthCondition);
            model.addAttribute("searchOwnerName", ownerName);
            model.addAttribute("searchTerm", searchTerm);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("currentPage", page);
            
            // Handle error state from backend
            if (seniorPetsResponse.isError()) {
                model.addAttribute("error", seniorPetsResponse.getErrorMessage());
                model.addAttribute("retryable", seniorPetsResponse.isRetryable());
            }
            
            return "pets/senior";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading senior pets: " + e.getMessage());
            return "pets/senior";
        }
    }
    
    /**
     * Get available species for senior pet filters
     */
    @GetMapping("/senior/species")
    @ResponseBody
    public List<String> getAvailableSpecies() {
        try {
            return seniorPetService.getAvailableSpecies().block();
        } catch (Exception e) {
            return List.of("Dog", "Cat", "Bird", "Rabbit");
        }
    }
    
    /**
     * Get available health conditions for senior pet filters
     */
    @GetMapping("/senior/health-conditions")
    @ResponseBody
    public List<String> getAvailableHealthConditions() {
        try {
            return seniorPetService.getAvailableHealthConditions().block();
        } catch (Exception e) {
            return List.of("Arthritis", "Diabetes", "Heart Disease", "Kidney Disease");
        }
    }
    
    /**
     * Get senior pet statistics
     */
    @GetMapping("/senior/statistics")
    @ResponseBody
    public Map<String, Object> getSeniorPetStatistics() {
        try {
            return seniorPetService.getSeniorPetStatistics().block();
        } catch (Exception e) {
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Statistics temporarily unavailable");
            return errorStats;
        }
    }
    
    private boolean hasSearchCriteria(Integer minAge, Integer maxAge, String species, String breed, 
                                    String healthCondition, String ownerName, String searchTerm) {
        return (minAge != null) || (maxAge != null) || 
               (species != null && !species.trim().isEmpty()) ||
               (breed != null && !breed.trim().isEmpty()) ||
               (healthCondition != null && !healthCondition.trim().isEmpty()) ||
               (ownerName != null && !ownerName.trim().isEmpty()) ||
               (searchTerm != null && !searchTerm.trim().isEmpty());
    }

    // ========================================
    // Enhanced Endpoints with Owner Information
    // ========================================

    /**
     * List pets with enhanced owner information (Enhanced View)
     */
    @GetMapping("/enhanced")
    public String listPetsEnhanced(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PetWithOwnerInfo> pets = petService.getAllPetsWithOwnerInfo(pageable).block();
            
            model.addAttribute("pets", pets);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pets.getTotalPages());
            model.addAttribute("totalElements", pets.getTotalElements());
            model.addAttribute("enhancedView", true); // Enhanced view with owner details
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading enhanced pets view: " + e.getMessage());
            // Fallback to standard view
            try {
                Pageable pageable = PageRequest.of(page, size);
                Page<Pet> regularPets = petService.getAllPets(pageable).block();
                model.addAttribute("pets", regularPets);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", regularPets.getTotalPages());
                model.addAttribute("totalElements", regularPets.getTotalElements());
                model.addAttribute("enhancedView", false);
                model.addAttribute("fallbackMessage", "Enhanced view temporarily unavailable. Showing standard view.");
            } catch (Exception fallbackError) {
                model.addAttribute("error", "Error loading pets: " + fallbackError.getMessage());
                model.addAttribute("pets", Page.empty());
                model.addAttribute("currentPage", 0);
                model.addAttribute("totalPages", 0);
                model.addAttribute("totalElements", 0L);
                model.addAttribute("enhancedView", false);
            }
            return "pets/list";
        }
    }

    /**
     * Show pets by owner with enhanced information
     */
    @GetMapping("/owner/{ownerId}/enhanced")
    public String petsByOwnerEnhanced(@PathVariable Long ownerId, Model model) {
        try {
            List<PetWithOwnerInfo> pets = petService.getPetsWithOwnerInfoByOwnerId(ownerId).block();
            
            // Get owner info from the first pet (if available) or fetch separately
            Owner owner = null;
            if (!pets.isEmpty() && pets.get(0).hasOwner()) {
                owner = new Owner();
                owner.setId(pets.get(0).getOwnerId());
                owner.setFirstName(pets.get(0).getOwnerFirstName());
                owner.setLastName(pets.get(0).getOwnerLastName());
            } else {
                owner = ownerService.getOwnerById(ownerId).block();
            }
            
            model.addAttribute("pets", pets);
            model.addAttribute("owner", owner);
            model.addAttribute("filterByOwner", true);
            model.addAttribute("enhancedView", true);
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading enhanced pets for owner: " + e.getMessage());
            return "redirect:/pets/owner/" + ownerId;
        }
    }

    /**
     * Search pets with enhanced owner information
     */
    @GetMapping("/search/enhanced")
    public String searchPetsEnhanced(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PetWithOwnerInfo> pets = petService.searchPetsWithOwnerInfo(q, pageable).block();
            
            model.addAttribute("pets", pets);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pets.getTotalPages());
            model.addAttribute("totalElements", pets.getTotalElements());
            model.addAttribute("searchPerformed", true);
            model.addAttribute("searchTerm", q);
            model.addAttribute("enhancedView", true);
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching enhanced pets: " + e.getMessage());
            return "redirect:/pets/search?q=" + (q != null ? q : "");
        }
    }

    /**
     * Refresh pet-owner information
     */
    @PostMapping("/{id}/refresh-owner-info")
    @ResponseBody
    public String refreshPetOwnerInfo(@PathVariable Long id) {
        try {
            PetWithOwnerInfo refreshedPet = petService.refreshPetOwnerInfo(id).block();
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /**
     * Get pet ownership statistics
     */
    @GetMapping("/ownership-stats")
    @ResponseBody
    public Map<String, Long> getPetOwnershipStats() {
        try {
            return petService.getPetOwnershipStatistics().block();
        } catch (Exception e) {
            Map<String, Long> errorStats = new HashMap<>();
            errorStats.put("error", 1L);
            return errorStats;
        }
    }
}