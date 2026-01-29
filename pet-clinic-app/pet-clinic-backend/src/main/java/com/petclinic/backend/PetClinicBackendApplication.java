package com.petclinic.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Pet Clinic Backend
 * Validates: Requirements 8.1, 8.2, 8.3, 8.5
 */
@SpringBootApplication
@EnableScheduling
public class PetClinicBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetClinicBackendApplication.class, args);
    }
}