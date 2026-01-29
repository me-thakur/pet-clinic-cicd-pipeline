package com.petclinic.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Configuration for pagination settings across the application.
 * Sets default page size and maximum page size limits.
 */
@Configuration
public class PaginationConfig {
    
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * Customizes the default pagination settings for Spring Data Web.
     * Sets reasonable defaults for page size and maximum page size.
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return pageableResolver -> {
            pageableResolver.setOneIndexedParameters(false); // Use 0-based page indexing
            pageableResolver.setPageParameterName("page");
            pageableResolver.setSizeParameterName("size");
            pageableResolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
            pageableResolver.setMaxPageSize(MAX_PAGE_SIZE);
        };
    }
}