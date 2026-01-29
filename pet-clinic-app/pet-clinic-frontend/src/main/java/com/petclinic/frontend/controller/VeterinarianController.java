package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Veterinarian;
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
import java.util.List;

/**
 * Veterinarian Controller
 * 
 * Handles web requests for veterinarian management operations.
 * Provides endpoints for listing, creating, editing, and deleting veterinarians.
 * 
 * Validates: Requirements 8.1, 8.3, 8.4
 */
@Controller
@RequestMapping("/veterinarians")
public class VeterinarianController {

    private final VeterinarianService veterinarianService;

    @Autowired
    public VeterinarianController(VeterinarianService veterinarianService) {
        this.veterinarianService = veterinarianService;
    }

    /**
     * List all veterinarians with pagination
     */
    @GetMapping
    public String listVeterinarians(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Veterinarian> veterinarians = veterinarianService.getAllVeterinarians(pageable).block();
            
            model.addAttribute("veterinarians", veterinarians);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", veterinarians.getTotalPages());
            model.addAttribute("totalElements", veterinarians.getTotalElements());
            
            return "veterinarians/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading veterinarians: " + e.getMessage());
            return "veterinarians/list";
        }
    }

    /**
     * Show veterinarian details
     */
    @GetMapping("/{id}")
    public String showVeterinarian(@PathVariable Long id, Model model) {
        Veterinarian veterinarian = veterinarianService.getVeterinarianById(id).block();
        if (veterinarian == null) {
            model.addAttribute("error", "Veterinarian not found");
            return "redirect:/veterinarians";
        }
        
        model.addAttribute("veterinarian", veterinarian);
        return "veterinarians/details";
    }

    /**
     * Show create veterinarian form
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("veterinarian", new Veterinarian());
        return "veterinarians/form";
    }

    /**
     * Process create veterinarian form
     */
    @PostMapping("/new")
    public String createVeterinarian(
            @Valid @ModelAttribute Veterinarian veterinarian,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "veterinarians/form";
        }

        try {
            Veterinarian savedVeterinarian = veterinarianService.createVeterinarian(veterinarian).block();
            redirectAttributes.addFlashAttribute("success", "Veterinarian created successfully!");
            return "redirect:/veterinarians/" + savedVeterinarian.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Error creating veterinarian: " + e.getMessage());
            return "veterinarians/form";
        }
    }

    /**
     * Show edit veterinarian form
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Veterinarian veterinarian = veterinarianService.getVeterinarianById(id).block();
        if (veterinarian == null) {
            model.addAttribute("error", "Veterinarian not found");
            return "redirect:/veterinarians";
        }
        
        model.addAttribute("veterinarian", veterinarian);
        return "veterinarians/form";
    }

    /**
     * Process edit veterinarian form
     */
    @PostMapping("/{id}/edit")
    public String updateVeterinarian(
            @PathVariable Long id,
            @Valid @ModelAttribute Veterinarian veterinarian,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            veterinarian.setId(id);
            return "veterinarians/form";
        }

        try {
            veterinarianService.updateVeterinarian(id, veterinarian).block();
            redirectAttributes.addFlashAttribute("success", "Veterinarian updated successfully!");
            return "redirect:/veterinarians/" + id;
        } catch (Exception e) {
            model.addAttribute("error", "Error updating veterinarian: " + e.getMessage());
            veterinarian.setId(id);
            return "veterinarians/form";
        }
    }

    /**
     * Delete veterinarian
     */
    @PostMapping("/{id}/delete")
    public String deleteVeterinarian(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            veterinarianService.deleteVeterinarian(id).block();
            redirectAttributes.addFlashAttribute("success", "Veterinarian deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting veterinarian: " + e.getMessage());
        }
        return "redirect:/veterinarians";
    }

    /**
     * Search veterinarians
     */
    @GetMapping("/search")
    public String searchVeterinarians(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String licenseNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            if (licenseNumber != null && !licenseNumber.trim().isEmpty()) {
                // Search by license number (single result)
                Veterinarian veterinarian = veterinarianService.searchByLicenseNumber(licenseNumber.trim()).block();
                List<Veterinarian> veterinarians = veterinarian != null ? List.of(veterinarian) : List.of();
                model.addAttribute("veterinarians", veterinarians);
                model.addAttribute("searchPerformed", true);
                model.addAttribute("searchLicenseNumber", licenseNumber);
                return "veterinarians/list";
            }
            
            List<Veterinarian> veterinarians = null;
            
            if (firstName != null && !firstName.trim().isEmpty()) {
                veterinarians = veterinarianService.searchByFirstName(firstName.trim()).block();
            } else if (lastName != null && !lastName.trim().isEmpty()) {
                veterinarians = veterinarianService.searchByLastName(lastName.trim()).block();
            } else if (specialty != null && !specialty.trim().isEmpty()) {
                veterinarians = veterinarianService.searchBySpecialty(specialty.trim()).block();
            } else if ((firstName != null && !firstName.trim().isEmpty()) || 
                      (lastName != null && !lastName.trim().isEmpty()) || 
                      (specialty != null && !specialty.trim().isEmpty())) {
                // Advanced search
                Pageable pageable = PageRequest.of(page, size);
                Page<Veterinarian> vetPage = veterinarianService.searchVeterinarians(
                    firstName, lastName, specialty, pageable).block();
                
                model.addAttribute("veterinarians", vetPage);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", vetPage.getTotalPages());
                model.addAttribute("totalElements", vetPage.getTotalElements());
                model.addAttribute("searchPerformed", true);
                model.addAttribute("searchFirstName", firstName);
                model.addAttribute("searchLastName", lastName);
                model.addAttribute("searchSpecialty", specialty);
                
                return "veterinarians/list";
            } else {
                // No search criteria, redirect to list
                return "redirect:/veterinarians";
            }
            
            model.addAttribute("veterinarians", veterinarians);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("searchFirstName", firstName);
            model.addAttribute("searchLastName", lastName);
            model.addAttribute("searchSpecialty", specialty);
            
            return "veterinarians/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error searching veterinarians: " + e.getMessage());
            return "veterinarians/list";
        }
    }

    /**
     * Show veterinarians with multiple specialties
     */
    @GetMapping("/multi-specialty")
    public String veterinariansWithMultipleSpecialties(Model model) {
        try {
            List<Veterinarian> veterinarians = veterinarianService.getVeterinariansWithMultipleSpecialties().block();
            
            model.addAttribute("veterinarians", veterinarians);
            model.addAttribute("filterByMultiSpecialty", true);
            
            return "veterinarians/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading veterinarians with multiple specialties: " + e.getMessage());
            return "veterinarians/list";
        }
    }

    /**
     * Show available veterinarians
     */
    @GetMapping("/available")
    public String availableVeterinarians(
            @RequestParam(defaultValue = "50") int maxVisits,
            Model model) {
        
        try {
            List<Veterinarian> veterinarians = veterinarianService.getAvailableVeterinarians(maxVisits).block();
            
            model.addAttribute("veterinarians", veterinarians);
            model.addAttribute("maxVisits", maxVisits);
            model.addAttribute("filterByAvailable", true);
            
            return "veterinarians/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading available veterinarians: " + e.getMessage());
            return "veterinarians/list";
        }
    }

    /**
     * Show veterinarian statistics
     */
    @GetMapping("/statistics")
    public String veterinarianStatistics(Model model) {
        try {
            Object[] statistics = veterinarianService.getVeterinarianStatistics().block();
            List<Object[]> commonSpecialties = veterinarianService.getMostCommonSpecialties(10).block();
            
            model.addAttribute("statistics", statistics);
            model.addAttribute("commonSpecialties", commonSpecialties);
            
            return "veterinarians/statistics";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading veterinarian statistics: " + e.getMessage());
            return "veterinarians/statistics";
        }
    }
}