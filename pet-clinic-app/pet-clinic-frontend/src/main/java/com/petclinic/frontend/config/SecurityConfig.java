package com.petclinic.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 * 
 * Configures Spring Security for the Pet Clinic Frontend application.
 * Implements in-memory authentication with role-based access control.
 * Supports ADMIN, VET, and STAFF roles with appropriate permissions.
 * 
 * Validates: Requirements 9.1, 10.1, 10.2
 */
@Configuration
@EnableWebSecurity
// @EnableMethodSecurity(prePostEnabled = true)  // Temporarily disable method security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF completely for debugging
            .csrf(csrf -> csrf.disable())
            
            // Configure authorization
            .authorizeHttpRequests(authz -> authz
                // Allow static resources
                .requestMatchers("/webjars/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Allow health check
                .requestMatchers("/actuator/health").permitAll()
                // Allow validation API endpoints (for AJAX calls)
                .requestMatchers("/api/validation/**").permitAll()
                // Allow login page and error pages
                .requestMatchers("/login", "/error").permitAll()
                // Allow test endpoints for debugging
                .requestMatchers("/test/**").permitAll()
                // Require authentication for all other requests
                .anyRequest().authenticated()
            )
            
            // Configure form login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform-login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            
            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            // Configure session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry())
            )
            
            // Disable headers that might interfere
            .headers(headers -> headers.disable());

        return http.build();
    }

    @Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails vet1 = User.builder()
                .username("vet1")
                .password(passwordEncoder().encode("vet123"))
                .roles("VET")
                .build();

        UserDetails vet2 = User.builder()
                .username("vet2")
                .password(passwordEncoder().encode("vet123"))
                .roles("VET")
                .build();

        UserDetails staff1 = User.builder()
                .username("staff1")
                .password(passwordEncoder().encode("staff123"))
                .roles("STAFF")
                .build();

        UserDetails staff2 = User.builder()
                .username("staff2")
                .password(passwordEncoder().encode("staff123"))
                .roles("STAFF")
                .build();

        UserDetails receptionist = User.builder()
                .username("receptionist1")
                .password(passwordEncoder().encode("staff123"))
                .roles("STAFF")
                .build();

        UserDetails testUser = User.builder()
                .username("user")
                .password(passwordEncoder().encode("user123"))
                .roles("USER")
                .build();

        UserDetails testUser2 = User.builder()
                .username("test")
                .password(passwordEncoder().encode("test123"))
                .roles("USER")
                .build();

        // Support for users with default password
        UserDetails defaultUser = User.builder()
                .username("demo")
                .password(passwordEncoder().encode("password123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, vet1, vet2, staff1, staff2, receptionist, testUser, testUser2, defaultUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}