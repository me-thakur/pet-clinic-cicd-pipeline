package com.petclinic.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for Pet Clinic Management System API documentation.
 * Configures Swagger UI with comprehensive API information, security schemes,
 * and server definitions for interactive API testing.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:9090}")
    private String serverPort;

    @Value("${spring.application.name:Pet Clinic Backend}")
    private String applicationName;

    /**
     * Configures OpenAPI specification with comprehensive API documentation.
     * 
     * @return OpenAPI configuration with API info, security, and servers
     */
    @Bean
    public OpenAPI petClinicOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServerList())
                .addSecurityItem(createSecurityRequirement())
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", createSecurityScheme()));
    }

    /**
     * Creates comprehensive API information for documentation.
     * 
     * @return API info with title, description, version, and contact details
     */
    private Info createApiInfo() {
        return new Info()
                .title("Pet Clinic Management System API")
                .description("Comprehensive REST API for veterinary practice management including " +
                           "pet management, visit scheduling, veterinarian management, and reporting capabilities. " +
                           "Built with Spring Boot and designed for scalability and security.")
                .version("1.0.0")
                .contact(createContactInfo())
                .license(createLicenseInfo());
    }

    /**
     * Creates contact information for API documentation.
     * 
     * @return Contact information for API support
     */
    private Contact createContactInfo() {
        return new Contact()
                .name("Pet Clinic Development Team")
                .email("support@petclinic.com")
                .url("https://petclinic.com/support");
    }

    /**
     * Creates license information for API documentation.
     * 
     * @return License information
     */
    private License createLicenseInfo() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    /**
     * Creates server list for different environments.
     * 
     * @return List of servers for API testing
     */
    private List<Server> createServerList() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");

        Server productionServer = new Server()
                .url("https://api.petclinic.com")
                .description("Production server");

        return List.of(localServer, productionServer);
    }

    /**
     * Creates security requirement for JWT authentication.
     * 
     * @return Security requirement configuration
     */
    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }

    /**
     * Creates JWT security scheme configuration.
     * 
     * @return Security scheme for JWT bearer token authentication
     */
    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT authentication token. Obtain token from /api/auth/login endpoint.");
    }
}