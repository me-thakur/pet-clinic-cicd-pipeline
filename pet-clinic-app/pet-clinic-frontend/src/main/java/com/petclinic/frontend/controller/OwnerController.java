package com.petclinic.frontend.controller;

import com.petclinic.frontend.model.Owner;
import com.petclinic.frontend.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Owner Controller
 * 
 * Handles web requests for owner management operations.
 * Provides CRUD operations and search functionality through Thymeleaf templates.
 * 
 * Validates: Requirements 8.2, 8.3, 8.4, 8.5
 */
@Controller
@RequestMapping("/owners")
public class OwnerController {

    private final OwnerService ownerService;

    @Autowired
    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * List all owners with pagination
     */
    @GetMapping
    public String listOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Owner> ownersPage = ownerService.getAllOwners(pageable).block();
        
        model.addAttribute("owners", ownersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ownersPage.getTotalPages());
        model.addAttribute("totalElements", ownersPage.getTotalElements());
        model.addAttribute("size", size);
        
        return "owners/list";
    }

    /**
     * Show owner details
     */
    @GetMapping("/{id}")
    public String showOwner(@PathVariable Long id, Model model) {
        Owner owner = ownerService.getOwnerById(id).block();
        if (owner == null) {
            return "redirect:/owners?error=notfound";
        }
        
        model.addAttribute("owner", owner);
        return "owners/details";
    }

    /**
     * Show create owner form
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("owner", new Owner());
        return "owners/form";
    }

    /**
     * Process create owner form
     */
    @PostMapping("/new")
    public String createOwner(
            @Valid @ModelAttribute Owner owner,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "owners/form";
        }
        
        try {
            System.out.println("DEBUG: Frontend attempting to create owner: " + owner);
            Owner savedOwner = ownerService.createOwner(owner).block();
            
            if (savedOwner == null) {
                System.out.println("DEBUG: savedOwner is null - this should not happen with proper error handling");
                bindingResult.rejectValue("email", "error.owner", "Failed to create owner. Please try again.");
                return "owners/form";
            }
            
            System.out.println("DEBUG: Frontend successfully created owner: " + savedOwner);
            redirectAttributes.addFlashAttribute("success", "Owner created successfully!");
            return "redirect:/owners/" + savedOwner.getId();
        } catch (Exception e) {
            System.out.println("DEBUG: Frontend error creating owner: " + e.getMessage());
            e.printStackTrace();
            
            // Handle specific error types
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("EMAIL_DUPLICATE")) {
                    bindingResult.rejectValue("email", "error.owner", "An owner with this email address already exists. Please use a different email.");
                } else if (errorMessage.contains("VALIDATION_ERROR")) {
                    bindingResult.rejectValue("email", "error.owner", "Invalid data provided. Please check your input and try again.");
                } else if (errorMessage.contains("AUTHENTICATION_ERROR")) {
                    bindingResult.rejectValue("email", "error.owner", "Authentication failed. Please log in again.");
                } else if (errorMessage.contains("AUTHORIZATION_ERROR")) {
                    bindingResult.rejectValue("email", "error.owner", "You don't have permission to create owners.");
                } else if (errorMessage.contains("SERVER_ERROR")) {
                    bindingResult.rejectValue("email", "error.owner", "Server error occurred. Please try again later.");
                } else if (errorMessage.contains("400") || errorMessage.contains("validation") || errorMessage.contains("Validation")) {
                    bindingResult.rejectValue("email", "error.owner", "Invalid data provided. Please check your input and try again.");
                } else if (errorMessage.contains("401") || errorMessage.contains("403")) {
                    bindingResult.rejectValue("email", "error.owner", "Authentication failed. Please log in again.");
                } else if (errorMessage.contains("503")) {
                    bindingResult.rejectValue("email", "error.owner", "Backend service is temporarily unavailable. Please try again later.");
                } else {
                    bindingResult.rejectValue("email", "error.owner", "Failed to create owner. Please try again. Error: " + errorMessage);
                }
            } else {
                bindingResult.rejectValue("email", "error.owner", "Failed to create owner. Please try again.");
            }
            return "owners/form";
        }
    }

    /**
     * Show edit owner form
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Owner owner = ownerService.getOwnerById(id).block();
        if (owner == null) {
            return "redirect:/owners?error=notfound";
        }
        
        model.addAttribute("owner", owner);
        return "owners/form";
    }

    /**
     * Process edit owner form
     */
    @PostMapping("/{id}/edit")
    public String updateOwner(
            @PathVariable Long id,
            @Valid @ModelAttribute Owner owner,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "owners/form";
        }
        
        try {
            ownerService.updateOwner(id, owner).block();
            redirectAttributes.addFlashAttribute("success", "Owner updated successfully!");
            return "redirect:/owners/" + id;
        } catch (Exception e) {
            bindingResult.rejectValue("email", "error.owner", "Failed to update owner. Please try again.");
            return "owners/form";
        }
    }

    /**
     * Delete owner
     */
    @PostMapping("/{id}/delete")
    public String deleteOwner(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ownerService.deleteOwner(id).block();
            redirectAttributes.addFlashAttribute("success", "Owner deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete owner. Please try again.");
        }
        
        return "redirect:/owners";
    }

    /**
     * Search owners
     */
    @GetMapping("/search")
    public String searchOwners(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String city,
            Model model) {
        
        List<Owner> owners = null;
        String searchType = "";
        String searchTerm = "";
        
        try {
            if (firstName != null && !firstName.trim().isEmpty()) {
                owners = ownerService.searchByFirstName(firstName.trim()).block();
                searchType = "first name";
                searchTerm = firstName.trim();
            } else if (lastName != null && !lastName.trim().isEmpty()) {
                owners = ownerService.searchByLastName(lastName.trim()).block();
                searchType = "last name";
                searchTerm = lastName.trim();
            } else if (email != null && !email.trim().isEmpty()) {
                Owner owner = ownerService.searchByEmail(email.trim()).block();
                owners = owner != null ? List.of(owner) : List.of();
                searchType = "email";
                searchTerm = email.trim();
            } else if (city != null && !city.trim().isEmpty()) {
                owners = ownerService.searchByCity(city.trim()).block();
                searchType = "city";
                searchTerm = city.trim();
            }
        } catch (Exception e) {
            model.addAttribute("error", "Search failed. Please try again.");
            owners = List.of();
        }
        
        model.addAttribute("owners", owners != null ? owners : List.of());
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("email", email);
        model.addAttribute("city", city);
        
        return "owners/search";
    }

    /**
     * Show owners with multiple pets
     */
    @GetMapping("/multiple-pets")
    public String ownersWithMultiplePets(Model model) {
        try {
            List<Owner> owners = ownerService.getOwnersWithMultiplePets().block();
            model.addAttribute("owners", owners != null ? owners : List.of());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load owners with multiple pets.");
            model.addAttribute("owners", List.of());
        }
        
        return "owners/multiple-pets";
    }
}