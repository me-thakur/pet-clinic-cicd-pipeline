package com.petclinic.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller
 * 
 * Handles the main home page and navigation for the Pet Clinic application.
 * Provides the main entry point after user authentication.
 * 
 * Validates: Requirements 8.5
 */
@Controller
public class HomeController {

    /**
     * Home page - redirects to dashboard
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    /**
     * Index page - redirects to dashboard
     */
    @GetMapping("/index")
    public String index() {
        return "redirect:/dashboard";
    }
}