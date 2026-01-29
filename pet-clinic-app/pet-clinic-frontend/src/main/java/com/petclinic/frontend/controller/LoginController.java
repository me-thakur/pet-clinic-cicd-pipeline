package com.petclinic.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Login Controller
 * 
 * Handles user authentication and login page display.
 * Provides login form and handles authentication errors.
 * 
 * Validates: Requirements 9.1, 9.3
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }
        
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully");
        }
        
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}