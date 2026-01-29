package com.petclinic.backend.service;

import com.petclinic.backend.model.User;

/**
 * Service interface for authentication token management
 */
public interface AuthenticationTokenService {

    /**
     * Generate an authentication token for a user
     */
    String generateToken(User user);

    /**
     * Validate an authentication token
     */
    boolean validateToken(String token);

    /**
     * Extract username from token
     */
    String getUsernameFromToken(String token);

    /**
     * Check if token is expired
     */
    boolean isTokenExpired(String token);

    /**
     * Refresh an authentication token
     */
    String refreshToken(String token);

    /**
     * Invalidate a token (logout)
     */
    void invalidateToken(String token);

    /**
     * Get token expiration time in milliseconds
     */
    long getTokenExpirationTime();
}