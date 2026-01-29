package com.petclinic.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Configuration for API versioning support
 * Enables version-aware request handling and backward compatibility
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {
    
    public static final String CURRENT_API_VERSION = "v1";
    public static final String API_VERSION_HEADER = "X-API-Version";
    public static final String DEFAULT_MEDIA_TYPE = "application/json";
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)
            .favorPathExtension(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    }
    
    /**
     * Get the API version from request headers or return default
     */
    public static String getApiVersion(HttpServletRequest request) {
        String version = request.getHeader(API_VERSION_HEADER);
        return version != null ? version : CURRENT_API_VERSION;
    }
    
    /**
     * Check if the requested API version is supported
     */
    public static boolean isSupportedVersion(String version) {
        return CURRENT_API_VERSION.equals(version) || version == null;
    }
    
    /**
     * Get versioned endpoint path
     */
    public static String getVersionedPath(String basePath, String version) {
        if (version == null || CURRENT_API_VERSION.equals(version)) {
            return basePath;
        }
        return "/" + version + basePath;
    }
}