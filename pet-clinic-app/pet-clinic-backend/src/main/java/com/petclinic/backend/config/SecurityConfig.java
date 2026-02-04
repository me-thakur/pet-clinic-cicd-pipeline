package com.petclinic.backend.config;

import com.petclinic.backend.security.JwtAuthenticationFilter;
import com.petclinic.backend.service.UserService;
import com.petclinic.backend.config.SecurityHeadersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;

/**
 * Security configuration for Pet Clinic Backend API
 * 
 * This configuration:
 * - Enables method-level security with role-based access control
 * - Disables CSRF protection for REST API endpoints
 * - Enables CORS for cross-origin requests from frontend
 * - Uses stateless session management for REST API
 * - Integrates JWT authentication filter
 * - Integrates with database-backed user authentication
 * 
 * Validates: Requirements 9.1, 9.3, 9.4
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${pet-clinic.cors.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SecurityHeadersConfig securityHeadersConfig;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configure CSRF protection for state-changing operations
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Disable CSRF for API endpoints that use JWT authentication or are public
                .ignoringRequestMatchers("/api/auth/**", "/api/test/**", "/api/validation/**")
                // Enable CSRF for sensitive operations
                .requireCsrfProtectionMatcher(request -> {
                    String uri = request.getRequestURI();
                    String method = request.getMethod();
                    // Require CSRF for bulk operations and admin functions
                    return ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) &&
                           (uri.contains("/bulk") || uri.contains("/reports") || uri.contains("/admin"));
                })
            )
            
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management for stateless REST API
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization with role-based access
            .authorizeHttpRequests(authz -> authz
                // Allow public access to authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // Allow public access to test endpoint
                .requestMatchers("/api/test/**").permitAll()
                // Allow public access to validation endpoints (for frontend AJAX calls)
                .requestMatchers("/api/validation/**").permitAll()
                // Allow public access to health check and documentation
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                // Require authentication for all other API endpoints
                .requestMatchers("/api/**").authenticated()
                // Allow public access for other endpoints (handled by frontend)
                .anyRequest().permitAll()
            )
            
            // Add security headers filter
            .addFilterBefore(securityHeadersConfig.securityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (frontend URL)
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}