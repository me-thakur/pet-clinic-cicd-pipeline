package com.petclinic.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pet Clinic Frontend Application
 * 
 * This is the main entry point for the Pet Clinic Frontend web application.
 * It provides a Thymeleaf-based web interface for managing pet clinic operations
 * including pets, owners, visits, and veterinarians.
 * 
 * The frontend communicates with the backend REST API to perform CRUD operations
 * and provides search and filtering capabilities for clinic staff.
 * 
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5
 */
@SpringBootApplication
public class PetClinicFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetClinicFrontendApplication.class, args);
    }
}