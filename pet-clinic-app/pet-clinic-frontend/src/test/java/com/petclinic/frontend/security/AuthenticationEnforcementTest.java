package com.petclinic.frontend.security;

import com.petclinic.frontend.config.SecurityConfig;
import com.petclinic.frontend.controller.HomeController;
import com.petclinic.frontend.controller.OwnerController;
import com.petclinic.frontend.service.OwnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authentication Enforcement Tests
 * **Property 13: Authentication Enforcement**
 * **Validates: Requirements 10.2**
 * 
 * Tests that for any attempt to access the Pet Clinic System,
 * the system should require valid authentication credentials
 * before allowing access to any functionality.
 */
@WebMvcTest(controllers = {HomeController.class, OwnerController.class})
@Import(SecurityConfig.class)
public class AuthenticationEnforcementTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OwnerService ownerService;

    /**
     * Test that unauthenticated users are redirected to login page
     * for protected endpoints
     */
    @Test
    @WithAnonymousUser
    void unauthenticatedAccessToProtectedEndpointsRedirectsToLogin() throws Exception {
        // Test main dashboard
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Test owners list
        mockMvc.perform(get("/owners"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Test owner creation form
        mockMvc.perform(get("/owners/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * Test that unauthenticated POST requests are blocked
     */
    @Test
    @WithAnonymousUser
    void unauthenticatedPostRequestsAreBlocked() throws Exception {
        // Test owner creation
        mockMvc.perform(post("/owners/new")
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "john@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Test owner deletion
        mockMvc.perform(post("/owners/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * Test that login page is accessible to anonymous users
     */
    @Test
    @WithAnonymousUser
    void loginPageIsAccessibleToAnonymousUsers() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    /**
     * Test that static resources are accessible without authentication
     */
    @Test
    @WithAnonymousUser
    void staticResourcesAreAccessibleWithoutAuthentication() throws Exception {
        // Test WebJars resources
        mockMvc.perform(get("/webjars/bootstrap/css/bootstrap.min.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/webjars/jquery/jquery.min.js"))
                .andExpect(status().isOk());
    }

    /**
     * Test that health endpoint is accessible without authentication
     */
    @Test
    @WithAnonymousUser
    void healthEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    /**
     * Test different user roles can access the system
     */
    @Test
    @WithMockUser(username = "vet", roles = {"VET"})
    void veterinarianRoleCanAccessSystem() throws Exception {
        // Should not redirect to login
        mockMvc.perform(get("/"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 300 && status < 400) {
                        throw new AssertionError("Expected non-redirect status but got: " + status);
                    }
                });
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void staffRoleCanAccessSystem() throws Exception {
        // Should not redirect to login
        mockMvc.perform(get("/"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 300 && status < 400) {
                        throw new AssertionError("Expected non-redirect status but got: " + status);
                    }
                });
    }

    /**
     * Test that authentication is enforced consistently across all endpoints
     * This test validates the core property: any attempt to access functionality
     * requires authentication
     */
    @Test
    @WithAnonymousUser
    void authenticationIsEnforcedConsistentlyAcrossAllEndpoints() throws Exception {
        String[] protectedEndpoints = {
            "/",
            "/dashboard",
            "/owners",
            "/owners/new",
            "/owners/search"
        };

        for (String endpoint : protectedEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    /**
     * Test that logout functionality works correctly
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void logoutFunctionalityWorksCorrectly() throws Exception {
        // Perform logout
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    /**
     * Test that invalid authentication attempts are handled properly
     */
    @Test
    void invalidAuthenticationAttemptsAreHandledProperly() throws Exception {
        // Test login with invalid credentials
        mockMvc.perform(post("/login")
                .param("username", "invalid")
                .param("password", "invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}