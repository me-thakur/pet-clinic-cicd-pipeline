package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.Owner;
import com.petclinic.frontend.service.PetService;
import com.petclinic.frontend.service.OwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

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

    @Autowired
    public PetController(PetService petService, OwnerService ownerService) {
        this.petService = petService;
        this.ownerService = ownerService;
    }

    /**
     * List all pets with pagination
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
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading pets: " + e.getMessage());
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
        
        // Load owners for dropdown
        try {
            Pageable pageable = PageRequest.of(0, 100); // Get first 100 owners
            Page<Owner> owners = ownerService.getAllOwners(pageable).block();
            model.addAttribute("owners", owners.getContent());
        } catch (Exception e) {
            model.addAttribute("error", "Error loading owners: " + e.getMessage());
        }
        
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
            // Reload owners for dropdown
            try {
                Pageable pageable = PageRequest.of(0, 100);
                Page<Owner> owners = ownerService.getAllOwners(pageable).block();
                model.addAttribute("owners", owners.getContent());
            } catch (Exception e) {
                model.addAttribute("error", "Error loading owners: " + e.getMessage());
            }
            return "pets/form";
        }

        try {
            Pet savedPet = petService.createPet(pet).block();
            redirectAttributes.addFlashAttribute("success", "Pet created successfully!");
            return "redirect:/pets/" + savedPet.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Error creating pet: " + e.getMessage());
            // Reload owners for dropdown
            try {
                Pageable pageable = PageRequest.of(0, 100);
                Page<Owner> owners = ownerService.getAllOwners(pageable).block();
                model.addAttribute("owners", owners.getContent());
            } catch (Exception ex) {
                model.addAttribute("error", "Error loading owners: " + ex.getMessage());
            }
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
        
        // Load owners for dropdown
        try {
            Pageable pageable = PageRequest.of(0, 100);
            Page<Owner> owners = ownerService.getAllOwners(pageable).block();
            model.addAttribute("owners", owners.getContent());
        } catch (Exception e) {
            model.addAttribute("error", "Error loading owners: " + e.getMessage());
        }
        
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
            // Reload owners for dropdown
            try {
                Pageable pageable = PageRequest.of(0, 100);
                Page<Owner> owners = ownerService.getAllOwners(pageable).block();
                model.addAttribute("owners", owners.getContent());
            } catch (Exception e) {
                model.addAttribute("error", "Error loading owners: " + e.getMessage());
            }
            return "pets/form";
        }

        try {
            petService.updatePet(id, pet).block();
            redirectAttributes.addFlashAttribute("success", "Pet updated successfully!");
            return "redirect:/pets/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Error updating pet: " + e.getMessage());
            pet.setId(id);
            // Reload owners for dropdown
            try {
                Pageable pageable = PageRequest.of(0, 100);
                Page<Owner> owners = ownerService.getAllOwners(pageable).block();
                model.addAttribute("owners", owners.getContent());
            } catch (Exception ex) {
                model.addAttribute("error", "Error loading owners: " + ex.getMessage());
            }
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
     * Search pets
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
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching pets: " + e.getMessage());
            return "pets/list";
        }
    }

    /**
     * Show pets by owner
     */
    @GetMapping("/owner/{ownerId}")
    public String petsByOwner(@PathVariable Long ownerId, Model model) {
        try {
            List<Pet> pets = petService.getPetsByOwnerId(ownerId).block();
            Owner owner = ownerService.getOwnerById(ownerId).block();
            
            model.addAttribute("pets", pets);
            model.addAttribute("owner", owner);
            model.addAttribute("filterByOwner", true);
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading pets for owner: " + e.getMessage());
            return "redirect:/pets";
        }
    }

    /**
     * Show senior pets
     */
    @GetMapping("/senior")
    public String seniorPets(
            @RequestParam(defaultValue = "7") int age,
            Model model) {
        
        try {
            List<Pet> pets = petService.getSeniorPets(age).block();
            
            model.addAttribute("pets", pets);
            model.addAttribute("seniorAge", age);
            model.addAttribute("filterBySenior", true);
            
            return "pets/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading senior pets: " + e.getMessage());
            return "pets/list";
        }
    }
}