package com.petclinic.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Pet Clinic Backend
 * Enables comprehensive component scanning and integration
 * Validates: Requirements 8.1, 8.2, 8.3, 8.5
 */
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.petclinic.backend.repository")
@ComponentScan(basePackages = {
    "com.petclinic.backend.config",
    "com.petclinic.backend.controller",
    "com.petclinic.backend.service",
    "com.petclinic.backend.repository",
    "com.petclinic.backend.security",
    "com.petclinic.backend.validation",
    "com.petclinic.backend.interceptor",
    "com.petclinic.backend.exception",
    "com.petclinic.backend.util"
})
public class PetClinicBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetClinicBackendApplication.class, args);
    }
}