package com.petclinic.backend.controller;

import com.petclinic.backend.dto.AuthRequest;
import com.petclinic.backend.dto.AuthResponse;
import com.petclinic.backend.dto.PasswordChangeRequest;
import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.model.User;
import com.petclinic.backend.service.AuditService;
import com.petclinic.backend.service.AuthenticationTokenService;
import com.petclinic.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * REST controller for authentication operations
 * Validates: Requirements 9.3, 9.4
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${pet-clinic.cors.allowed-origins}", maxAge = 3600)
@Tag(name = "Authentication", description = "User authentication and authorization operations")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationTokenService tokenService;

    @Autowired
    private AuditService auditService;

    /**
     * Authenticate user and return JWT token
     */
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user credentials and returns a JWT token for subsequent API requests. " +
                     "Includes audit logging and account lockout protection.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed - Invalid credentials, disabled account, or locked account",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Parameter(description = "User credentials for authentication", required = true)
            @Valid @RequestBody AuthRequest authRequest,
            HttpServletRequest request) {
        try {
            logger.info("Authentication attempt for user: {}", authRequest.getUsername());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getUsername(),
                    authRequest.getPassword()
                )
            );

            User user = (User) authentication.getPrincipal();

            // Check if user is enabled
            if (!user.isEnabled()) {
                logger.warn("Authentication failed - user disabled: {}", authRequest.getUsername());
                userService.handleFailedLogin(authRequest.getUsername());
                
                auditService.logDataAccess(authRequest.getUsername(), "LOGIN_FAILED", "Authentication", 
                                          user.getId(), "Login failed - user disabled",
                                          getClientIpAddress(request), request.getHeader("User-Agent"));
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "User account is disabled", false));
            }

            // Check if account is locked
            if (!user.isAccountNonLocked()) {
                logger.warn("Authentication failed - account locked: {}", authRequest.getUsername());
                
                auditService.logDataAccess(authRequest.getUsername(), "LOGIN_FAILED", "Authentication", 
                                          user.getId(), "Login failed - account locked",
                                          getClientIpAddress(request), request.getHeader("User-Agent"));
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "User account is locked", false));
            }

            // Generate JWT token
            String token = tokenService.generateToken(user);

            // Handle successful login
            userService.handleSuccessfulLogin(authRequest.getUsername());

            // Log successful authentication
            auditService.logDataAccess(authRequest.getUsername(), "LOGIN_SUCCESS", "Authentication", 
                                      user.getId(), "Successful login",
                                      getClientIpAddress(request), request.getHeader("User-Agent"));

            logger.info("Authentication successful for user: {}", authRequest.getUsername());

            return ResponseEntity.ok(new AuthResponse(token, "Authentication successful", true));

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed - bad credentials for user: {}", authRequest.getUsername());
            
            // Handle failed login
            userService.handleFailedLogin(authRequest.getUsername());
            
            // Log failed authentication
            auditService.logDataAccess(authRequest.getUsername(), "LOGIN_FAILED", "Authentication", 
                                      null, "Login failed - bad credentials",
                                      getClientIpAddress(request), request.getHeader("User-Agent"));

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse(null, "Invalid username or password", false));
        } catch (Exception e) {
            logger.error("Authentication error for user {}: {}", authRequest.getUsername(), e.getMessage());
            
            // Log authentication error
            auditService.logDataAccess(authRequest.getUsername(), "LOGIN_ERROR", "Authentication", 
                                      null, "Login error: " + e.getMessage(),
                                      getClientIpAddress(request), request.getHeader("User-Agent"));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(null, "Authentication failed", false));
        }
    }

    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "No token provided", false));
            }

            if (!tokenService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Invalid or expired token", false));
            }

            String newToken = tokenService.refreshToken(token);
            
            if (newToken != null) {
                String username = tokenService.getUsernameFromToken(newToken);
                logger.info("Token refreshed successfully for user: {}", username);
                
                return ResponseEntity.ok(new AuthResponse(newToken, "Token refreshed successfully", true));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Failed to refresh token", false));
            }

        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(null, "Token refresh failed", false));
        }
    }

    /**
     * Logout user (invalidate token)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                String username = tokenService.getUsernameFromToken(token);
                tokenService.invalidateToken(token);
                
                // Log logout
                auditService.logDataAccess(username, "LOGOUT", "Authentication", null, 
                                          "User logged out",
                                          getClientIpAddress(request), request.getHeader("User-Agent"));
                
                logger.info("User logged out successfully: {}", username);
            }

            // Clear security context
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(new AuthResponse(null, "Logout successful", true));

        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(null, "Logout failed", false));
        }
    }

    /**
     * Change user password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                          HttpServletRequest httpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "User not authenticated", false));
            }

            User user = (User) authentication.getPrincipal();
            
            // Change password
            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());

            // Log password change
            auditService.logDataAccess(user.getUsername(), "PASSWORD_CHANGED", "Authentication", 
                                      user.getId(), "Password changed by user",
                                      getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"));

            logger.info("Password changed successfully for user: {}", user.getUsername());

            return ResponseEntity.ok(new AuthResponse(null, "Password changed successfully", true));

        } catch (PetClinicException e) {
            logger.warn("Password change failed: {}", e.getMessage());
            return ResponseEntity.status(e.getHttpStatus())
                .body(new AuthResponse(null, e.getMessage(), false));
        } catch (Exception e) {
            logger.error("Password change error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(null, "Password change failed", false));
        }
    }

    /**
     * Validate token
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "No token provided", false));
            }

            boolean isValid = tokenService.validateToken(token);
            
            if (isValid) {
                String username = tokenService.getUsernameFromToken(token);
                return ResponseEntity.ok(new AuthResponse(null, "Token is valid for user: " + username, true));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Invalid or expired token", false));
            }

        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(null, "Token validation failed", false));
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}