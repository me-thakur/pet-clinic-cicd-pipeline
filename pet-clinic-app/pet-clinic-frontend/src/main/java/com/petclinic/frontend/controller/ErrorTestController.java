package com.petclinic.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for error handling test page
 * Used to test the enhanced error handling components
 */
@Controller
@RequestMapping("/error-test")
public class ErrorTestController {

    @GetMapping
    public String errorTestPage() {
        return "error-test";
    }
}