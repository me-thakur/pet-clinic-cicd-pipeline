package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Visit;
import com.petclinic.frontend.model.Pet;
import com.petclinic.frontend.model.Veterinarian;
import com.petclinic.frontend.service.VisitService;
import com.petclinic.frontend.service.PetService;
import com.petclinic.frontend.service.VeterinarianService;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Visit Controller
 * 
 * Handles web requests for visit management operations.
 * Provides endpoints for listing, creating, editing, and deleting visits.
 * 
 * Validates: Requirements 8.1, 8.3, 8.4
 */
@Controller
@RequestMapping("/visits")
public class VisitController {

    private final VisitService visitService;
    private final PetService petService;
    private final VeterinarianService veterinarianService;

    @Autowired
    public VisitController(VisitService visitService, PetService petService, VeterinarianService veterinarianService) {
        this.visitService = visitService;
        this.petService = petService;
        this.veterinarianService = veterinarianService;
    }

    /**
     * List all visits with pagination
     */
    @GetMapping
    public String listVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Visit> visits = visitService.getAllVisits(pageable).block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", visits.getTotalPages());
            model.addAttribute("totalElements", visits.getTotalElements());
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Show visit details
     */
    @GetMapping("/{id}")
    public String showVisit(@PathVariable Long id, Model model) {
        Visit visit = visitService.getVisitById(id).block();
        if (visit == null) {
            model.addAttribute("error", "Visit not found");
            return "redirect:/visits";
        }
        
        model.addAttribute("visit", visit);
        return "visits/details";
    }

    /**
     * Show create visit form
     */
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long veterinarianId,
            Model model) {
        
        Visit visit = new Visit();
        if (petId != null) {
            // Create a temporary pet object with just the ID
            Pet pet = new Pet();
            pet.setId(petId);
            visit.setPet(pet);
        }
        if (veterinarianId != null) {
            // Create a temporary veterinarian object with just the ID
            Veterinarian veterinarian = new Veterinarian();
            veterinarian.setId(veterinarianId);
            visit.setVeterinarian(veterinarian);
        }
        
        model.addAttribute("visit", visit);
        
        // Load pets and veterinarians for dropdowns
        loadFormData(model);
        
        return "visits/form";
    }

    /**
     * Process create visit form
     */
    @PostMapping("/new")
    public String createVisit(
            @Valid @ModelAttribute Visit visit,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            loadFormData(model);
            return "visits/form";
        }

        try {
            Visit savedVisit = visitService.createVisit(visit).block();
            redirectAttributes.addFlashAttribute("success", "Visit created successfully!");
            return "redirect:/visits/" + savedVisit.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Error creating visit: " + e.getMessage());
            loadFormData(model);
            return "visits/form";
        }
    }

    /**
     * Show edit visit form
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Visit visit = visitService.getVisitById(id).block();
        if (visit == null) {
            model.addAttribute("error", "Visit not found");
            return "redirect:/visits";
        }
        
        model.addAttribute("visit", visit);
        loadFormData(model);
        
        return "visits/form";
    }

    /**
     * Process edit visit form
     */
    @PostMapping("/{id}/edit")
    public String updateVisit(
            @PathVariable Long id,
            @Valid @ModelAttribute Visit visit,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            visit.setId(id);
            loadFormData(model);
            return "visits/form";
        }

        try {
            visitService.updateVisit(id, visit).block();
            redirectAttributes.addFlashAttribute("success", "Visit updated successfully!");
            return "redirect:/visits/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Error updating visit: " + e.getMessage());
            visit.setId(id);
            loadFormData(model);
            return "visits/form";
        }
    }

    /**
     * Delete visit
     */
    @PostMapping("/{id}/delete")
    public String deleteVisit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            visitService.deleteVisit(id).block();
            redirectAttributes.addFlashAttribute("success", "Visit deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting visit: " + e.getMessage());
        }
        return "redirect:/visits";
    }

    /**
     * Search visits
     */
    @GetMapping("/search")
    public String searchVisits(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) String treatment,
            @RequestParam(required = false) BigDecimal minCost,
            @RequestParam(required = false) BigDecimal maxCost,
            Model model) {
        
        try {
            List<Visit> visits = null;
            
            if (description != null && !description.trim().isEmpty()) {
                visits = visitService.searchByDescription(description.trim()).block();
            } else if (diagnosis != null && !diagnosis.trim().isEmpty()) {
                visits = visitService.searchByDiagnosis(diagnosis.trim()).block();
            } else if (treatment != null && !treatment.trim().isEmpty()) {
                visits = visitService.searchByTreatment(treatment.trim()).block();
            } else if (minCost != null && maxCost != null) {
                visits = visitService.getVisitsByCostRange(minCost, maxCost).block();
            } else {
                // No search criteria, redirect to list
                return "redirect:/visits";
            }
            
            model.addAttribute("visits", visits);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("searchDescription", description);
            model.addAttribute("searchDiagnosis", diagnosis);
            model.addAttribute("searchTreatment", treatment);
            model.addAttribute("searchMinCost", minCost);
            model.addAttribute("searchMaxCost", maxCost);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Show visits by pet
     */
    @GetMapping("/pet/{petId}")
    public String visitsByPet(@PathVariable Long petId, Model model) {
        try {
            List<Visit> visits = visitService.getVisitsByPetId(petId).block();
            Pet pet = petService.getPetById(petId).block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("pet", pet);
            model.addAttribute("filterByPet", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading visits for pet: " + e.getMessage());
            return "redirect:/visits";
        }
    }

    /**
     * Show visits by veterinarian
     */
    @GetMapping("/veterinarian/{veterinarianId}")
    public String visitsByVeterinarian(@PathVariable Long veterinarianId, Model model) {
        try {
            List<Visit> visits = visitService.getVisitsByVeterinarianId(veterinarianId).block();
            Veterinarian veterinarian = veterinarianService.getVeterinarianById(veterinarianId).block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("veterinarian", veterinarian);
            model.addAttribute("filterByVeterinarian", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading visits for veterinarian: " + e.getMessage());
            return "redirect:/visits";
        }
    }

    /**
     * Show today's visits
     */
    @GetMapping("/today")
    public String todaysVisits(Model model) {
        try {
            List<Visit> visits = visitService.getTodaysVisits().block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("filterByToday", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading today's visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Show upcoming visits
     */
    @GetMapping("/upcoming")
    public String upcomingVisits(Model model) {
        try {
            List<Visit> visits = visitService.getUpcomingVisits().block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("filterByUpcoming", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading upcoming visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Show emergency visits
     */
    @GetMapping("/emergency")
    public String emergencyVisits(Model model) {
        try {
            List<Visit> visits = visitService.getEmergencyVisits().block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("filterByEmergency", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading emergency visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Show completed visits
     */
    @GetMapping("/completed")
    public String completedVisits(Model model) {
        try {
            List<Visit> visits = visitService.getCompletedVisits().block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("filterByCompleted", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading completed visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Show incomplete visits
     */
    @GetMapping("/incomplete")
    public String incompleteVisits(Model model) {
        try {
            List<Visit> visits = visitService.getIncompleteVisits().block();
            
            model.addAttribute("visits", visits);
            model.addAttribute("filterByIncomplete", true);
            
            return "visits/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading incomplete visits: " + e.getMessage());
            return "visits/list";
        }
    }

    /**
     * Load form data (pets and veterinarians for dropdowns)
     */
    private void loadFormData(Model model) {
        try {
            // Load pets for dropdown
            Pageable pageable = PageRequest.of(0, 100); // Get first 100 pets
            Page<Pet> pets = petService.getAllPets(pageable).block();
            model.addAttribute("pets", pets.getContent());
            
            // Load veterinarians for dropdown
            Page<Veterinarian> veterinarians = veterinarianService.getAllVeterinarians(pageable).block();
            model.addAttribute("veterinarians", veterinarians.getContent());
        } catch (Exception e) {
            model.addAttribute("error", "Error loading form data: " + e.getMessage());
        }
    }
}