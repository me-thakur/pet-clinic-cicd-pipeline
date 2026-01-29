package com.petclinic.backend.service.impl;

import com.petclinic.backend.model.User;
import com.petclinic.backend.service.AuditService;
import com.petclinic.backend.service.AuthenticationTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of AuthenticationTokenService for JWT token management
 * Validates: Requirements 9.3, 9.4
 */
@Service
public class AuthenticationTokenServiceImpl implements AuthenticationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationTokenServiceImpl.class);

    @Value("${app.jwt.secret:petclinic-secret-key-that-is-at-least-256-bits-long-for-security}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600000}") // 1 hour in milliseconds
    private long jwtExpirationTime;

    @Value("${app.session.timeout:1800000}") // 30 minutes in milliseconds
    private long sessionTimeout;

    @Autowired
    private AuditService auditService;

    // Store for invalidated tokens (in production, use Redis or database)
    private final Map<String, Date> invalidatedTokens = new ConcurrentHashMap<>();

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public String generateToken(User user) {
        logger.debug("Generating token for user: {}", user.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("enabled", user.isEnabled());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationTime);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Log token generation
        auditService.logDataAccess(user.getUsername(), "TOKEN_GENERATED", "Authentication", user.getId(),
                "Authentication token generated");

        logger.info("Token generated successfully for user: {}", user.getUsername());
        return token;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // Check if token is invalidated
            if (invalidatedTokens.containsKey(token)) {
                logger.debug("Token is invalidated");
                return false;
            }

            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            // Check if token is expired
            Date expiration = claims.getBody().getExpiration();
            if (expiration.before(new Date())) {
                logger.debug("Token is expired");
                return false;
            }

            // Check session timeout
            Date issuedAt = claims.getBody().getIssuedAt();
            if (issuedAt.before(new Date(System.currentTimeMillis() - sessionTimeout))) {
                logger.debug("Token session has timed out");
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Token parsing failed: {}", e.getMessage());
            return true; // Consider invalid tokens as expired
        }
    }

    @Override
    public String refreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);
            Boolean enabled = claims.get("enabled", Boolean.class);

            // Create new token with extended expiration
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("userId", userId);
            newClaims.put("role", role);
            newClaims.put("enabled", enabled);
            newClaims.put("refreshed", true); // Add flag to indicate this is a refreshed token

            // Ensure new timestamp is different from original
            Date now = new Date();
            Date originalIssuedAt = claims.getIssuedAt();
            if (now.getTime() <= originalIssuedAt.getTime()) {
                // Add 1 millisecond to ensure different timestamp
                now = new Date(originalIssuedAt.getTime() + 1);
            }
            
            Date expiryDate = new Date(now.getTime() + jwtExpirationTime);

            String newToken = Jwts.builder()
                    .setClaims(newClaims)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

            // Invalidate old token
            invalidatedTokens.put(token, new Date());

            // Log token refresh
            auditService.logDataAccess(username, "TOKEN_REFRESHED", "Authentication", userId,
                    "Authentication token refreshed");

            logger.info("Token refreshed successfully for user: {}", username);
            return newToken;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Failed to refresh token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void invalidateToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            invalidatedTokens.put(token, new Date());

            // Log token invalidation
            if (username != null) {
                auditService.logDataAccess(username, "TOKEN_INVALIDATED", "Authentication", null,
                        "Authentication token invalidated (logout)");
            }

            logger.info("Token invalidated successfully for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to invalidate token: {}", e.getMessage());
        }
    }

    @Override
    public long getTokenExpirationTime() {
        return jwtExpirationTime;
    }

    /**
     * Clean up expired invalidated tokens (should be called periodically)
     */
    public void cleanupExpiredTokens() {
        Date now = new Date();
        invalidatedTokens.entrySet().removeIf(entry -> {
            Date invalidationDate = entry.getValue();
            return invalidationDate.before(new Date(now.getTime() - jwtExpirationTime));
        });
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("userId", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get user role from token
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }
}