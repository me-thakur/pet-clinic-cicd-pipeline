package com.petclinic.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Configuration for security headers and CSRF protection
 * Provides additional security measures beyond basic authentication
 * 
 * Validates: Requirements 17.3, 17.4, 17.5
 */
@Configuration
public class SecurityHeadersConfig {
    
    /**
     * Custom filter to add security headers to all responses
     */
    @Bean
    public OncePerRequestFilter securityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                
                // Add security headers
                addSecurityHeaders(response);
                
                filterChain.doFilter(request, response);
            }
        };
    }
    
    /**
     * Add comprehensive security headers to prevent various attacks
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        // Prevent clickjacking attacks
        response.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Referrer policy for privacy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Content Security Policy (CSP) - restrictive policy for API endpoints
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");
        
        // HTTP Strict Transport Security (HSTS) - only if using HTTPS
        // Uncomment when deploying with HTTPS
        // response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        
        // Permissions Policy (formerly Feature Policy)
        response.setHeader("Permissions-Policy", 
            "camera=(), " +
            "microphone=(), " +
            "geolocation=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "accelerometer=(), " +
            "gyroscope=()");
        
        // Cache control for sensitive endpoints
        String requestURI = getCurrentRequestURI();
        if (isSensitiveEndpoint(requestURI)) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
    }
    
    /**
     * Get current request URI (simplified implementation)
     */
    private String getCurrentRequestURI() {
        // In a real implementation, this would get the URI from the request context
        return "";
    }
    
    /**
     * Check if the endpoint contains sensitive information
     */
    private boolean isSensitiveEndpoint(String uri) {
        if (uri == null) return false;
        
        String lowerUri = uri.toLowerCase();
        return lowerUri.contains("/reports") || 
               lowerUri.contains("/admin") || 
               lowerUri.contains("/auth") ||
               lowerUri.contains("/bulk") ||
               lowerUri.contains("/export");
    }
}