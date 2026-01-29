package com.petclinic.backend.properties;

import com.petclinic.backend.model.Role;
import com.petclinic.backend.model.User;
import com.petclinic.backend.service.AuthenticationTokenService;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for authentication token validation
 * **Property 25: Authentication Token Validation**
 * **Validates: Requirements 9.4**
 * 
 * For any API request, invalid or expired authentication tokens should be rejected with appropriate error responses
 */
@SpringBootTest
@ActiveProfiles("test")
public class AuthenticationTokenValidationProperties extends PropertyTestBase {

    @Autowired
    private AuthenticationTokenService tokenService;

    /**
     * Generator for valid usernames
     */
    protected Generator<String> validUsernames() {
        return () -> {
            StringBuilder username = new StringBuilder();
            int length = 5 + random.nextInt(15); // 5-20 characters
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    username.append((char) ('a' + random.nextInt(26)));
                } else {
                    username.append(random.nextInt(10));
                }
            }
            return username.toString();
        };
    }

    /**
     * Generator for all roles
     */
    protected Generator<Role> allRoles() {
        return () -> {
            Role[] roles = Role.values();
            return roles[random.nextInt(roles.length)];
        };
    }

    /**
     * Generator for invalid tokens
     */
    protected Generator<String> invalidTokens() {
        return () -> {
            StringBuilder token = new StringBuilder();
            int length = 10 + random.nextInt(90); // 10-100 characters
            for (int i = 0; i < length; i++) {
                if (random.nextBoolean()) {
                    token.append((char) ('a' + random.nextInt(26)));
                } else {
                    token.append(random.nextInt(10));
                }
            }
            // Ensure it doesn't accidentally look like a JWT
            String result = token.toString();
            if (result.contains(".") && result.split("\\.").length == 3) {
                result = result.replace(".", "x");
            }
            return result;
        };
    }

    /**
     * Property: Valid tokens generated for users should always validate successfully
     */
    @Test
    public void validTokensShouldAlwaysValidate() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String username = validUsernames().next();
            Role role = allRoles().next();
            
            // Create a test user
            User user = createTestUser(username, role);
            
            // Generate token
            String token = tokenService.generateToken(user);
            
            // Property: Valid token should always validate
            boolean isValid = tokenService.validateToken(token);
            assertTrue(isValid, "Valid token should always validate");
            
            // Property: Username should be extractable from valid token
            String extractedUsername = tokenService.getUsernameFromToken(token);
            assertEquals(username, extractedUsername, "Username should be extractable from valid token");
            
            // Property: Valid token should not be expired
            boolean isExpired = tokenService.isTokenExpired(token);
            assertFalse(isExpired, "Newly generated token should not be expired");
        });
    }

    /**
     * Property: Invalid tokens should always be rejected
     */
    @Test
    public void invalidTokensShouldAlwaysBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String invalidToken = invalidTokens().next();
            
            // Property: Invalid token should always fail validation
            boolean isValid = tokenService.validateToken(invalidToken);
            assertFalse(isValid, "Invalid token should always fail validation");
            
            // Property: Username extraction should fail for invalid token
            String username = tokenService.getUsernameFromToken(invalidToken);
            assertNull(username, "Username extraction should fail for invalid token");
            
            // Property: Invalid token should be considered expired
            boolean isExpired = tokenService.isTokenExpired(invalidToken);
            assertTrue(isExpired, "Invalid token should be considered expired");
        });
    }

    /**
     * Property: Invalidated tokens should always be rejected
     */
    @Test
    public void invalidatedTokensShouldAlwaysBeRejected() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String username = validUsernames().next();
            Role role = allRoles().next();
            
            // Create a test user
            User user = createTestUser(username, role);
            
            // Generate and validate token
            String token = tokenService.generateToken(user);
            assertTrue(tokenService.validateToken(token), "Token should be valid before invalidation");
            
            // Invalidate token
            tokenService.invalidateToken(token);
            
            // Property: Invalidated token should always fail validation
            boolean isValid = tokenService.validateToken(token);
            assertFalse(isValid, "Invalidated token should always fail validation");
        });
    }

    /**
     * Property: Token refresh should generate new valid token and invalidate old one
     */
    @Test
    public void tokenRefreshShouldGenerateNewValidToken() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String username = validUsernames().next();
            Role role = allRoles().next();
            
            // Create a test user
            User user = createTestUser(username, role);
            
            // Generate original token
            String originalToken = tokenService.generateToken(user);
            assertTrue(tokenService.validateToken(originalToken), "Original token should be valid");
            
            // Refresh token
            String newToken = tokenService.refreshToken(originalToken);
            assertNotNull(newToken, "Refresh should generate new token");
            assertNotEquals(originalToken, newToken, "New token should be different from original");
            
            // Property: New token should be valid
            boolean newTokenValid = tokenService.validateToken(newToken);
            assertTrue(newTokenValid, "New token should be valid");
            
            // Property: Original token should be invalidated after refresh
            boolean originalTokenValid = tokenService.validateToken(originalToken);
            assertFalse(originalTokenValid, "Original token should be invalidated after refresh");
            
            // Property: Username should be preserved in new token
            String extractedUsername = tokenService.getUsernameFromToken(newToken);
            assertEquals(username, extractedUsername, "Username should be preserved in new token");
        });
    }

    /**
     * Property: Token expiration time should be consistent
     */
    @Test
    public void tokenExpirationTimeShouldBeConsistent() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Generate test data
            String username = validUsernames().next();
            Role role = allRoles().next();
            
            // Create a test user
            User user = createTestUser(username, role);
            
            // Generate token
            String token = tokenService.generateToken(user);
            
            // Property: Token expiration time should be positive and consistent
            long expirationTime = tokenService.getTokenExpirationTime();
            assertTrue(expirationTime > 0, "Token expiration time should be positive");
            
            // Property: Multiple calls should return same expiration time
            long expirationTime2 = tokenService.getTokenExpirationTime();
            assertEquals(expirationTime, expirationTime2, "Token expiration time should be consistent");
        });
    }

    private User createTestUser(String username, Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("hashedPassword");
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        return user;
    }
}