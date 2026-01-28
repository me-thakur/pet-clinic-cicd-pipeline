package com.petclinic.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Web Client Configuration
 * 
 * Configures WebClient for communication with the backend REST API.
 * This enables the frontend to make HTTP requests to the backend services
 * for CRUD operations on pets, owners, visits, and veterinarians.
 */
@Configuration
public class WebClientConfig {

    @Value("${pet-clinic.backend.url}")
    private String backendUrl;

    @Value("${pet-clinic.backend.api-path}")
    private String apiPath;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(backendUrl + apiPath)
                .build();
    }

    @Bean
    public String backendBaseUrl() {
        return backendUrl + apiPath;
    }
}