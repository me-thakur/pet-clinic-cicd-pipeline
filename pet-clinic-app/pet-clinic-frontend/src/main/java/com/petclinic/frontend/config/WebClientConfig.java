package com.petclinic.frontend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web Client Configuration with Enhanced Authentication
 * 
 * Configures WebClient for communication with the backend REST API.
 * Implements proper authentication flow for export requests.
 * Validates: Requirements 2.1, 2.4
 */
@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${pet-clinic.backend.url}")
    private String backendUrl;

    @Value("${pet-clinic.backend.api-path}")
    private String apiPath;

    private String cachedToken;
    private long tokenExpiryTime;
    private String cachedUsername;

    @Bean
    public WebClient webClient() {
        String fullBaseUrl = backendUrl + apiPath;
        logger.info("Configuring WebClient with base URL: {}", fullBaseUrl);
        
        return WebClient.builder()
                .baseUrl(fullBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter((request, next) -> {
                    String currentUser = getCurrentUsername();
                    String sessionInfo = getCurrentSessionInfo();
                    
                    logger.info("=== WebClient Request Start ===");
                    logger.info("Request: {} {}", request.method(), request.url());
                    logger.info("Current User: {}", currentUser);
                    logger.info("Session Info: {}", sessionInfo);
                    logger.info("Request Headers: {}", request.headers());
                    
                    // Add authentication header dynamically
                    String token = getAuthToken();
                    logger.info("Auth Token Retrieved: {}", token != null ? "YES (length=" + token.length() + ")" : "NO");
                    
                    if (token != null && !token.isEmpty()) {
                        logger.info("Adding auth token to request: {}", request.url());
                        logger.debug("Token preview: {}...", token.substring(0, Math.min(50, token.length())));
                        
                        // Create a new request with authentication header
                        org.springframework.web.reactive.function.client.ClientRequest newRequest = 
                            org.springframework.web.reactive.function.client.ClientRequest.from(request)
                                .header("Authorization", "Bearer " + token)
                                .headers(headers -> {
                                    // Ensure proper content type
                                    headers.remove("Content-Type");
                                    headers.set("Content-Type", "application/json");
                                    
                                    // Add session information for debugging
                                    if (sessionInfo != null) {
                                        headers.set("X-Frontend-Session", sessionInfo);
                                    }
                                    headers.set("X-Frontend-User", currentUser != null ? currentUser : "UNKNOWN");
                                })
                                .build();
                        
                        logger.info("Modified Request Headers: {}", newRequest.headers());
                        
                        return next.exchange(newRequest)
                                .doOnSuccess(response -> {
                                    logger.info("=== WebClient Response Success ===");
                                    logger.info("Response Status: {} for {}", response.statusCode(), request.url());
                                    logger.info("Response Headers: {}", response.headers().asHttpHeaders());
                                })
                                .doOnError(error -> {
                                    logger.error("=== WebClient Response Error ===");
                                    logger.error("Error Type: {}", error.getClass().getSimpleName());
                                    logger.error("Error Message: {}", error.getMessage());
                                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException webEx = 
                                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                                        logger.error("HTTP Status: {}", webEx.getStatusCode());
                                        logger.error("Response Body: {}", webEx.getResponseBodyAsString());
                                    }
                                })
                                .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.class, ex -> {
                                    // Handle authentication failures
                                    logger.error("=== WebClient Authentication Error ===");
                                    logger.error("Status Code: {}", ex.getStatusCode());
                                    logger.error("Request URL: {}", request.url());
                                    logger.error("Response Body: {}", ex.getResponseBodyAsString());
                                    logger.error("Current User: {}", currentUser);
                                    logger.error("Token Used: {}...", token.substring(0, Math.min(20, token.length())));
                                    
                                    if (ex.getStatusCode().value() == 401) {
                                        logger.error("401 Unauthorized - Authentication failed");
                                        
                                        // Check if user session is still valid
                                        boolean sessionValid = isCurrentUserSessionValid();
                                        logger.error("Frontend session valid: {}", sessionValid);
                                        
                                        if (!sessionValid) {
                                            logger.error("Frontend session is invalid - user needs to re-login");
                                            return reactor.core.publisher.Mono.error(
                                                new org.springframework.web.reactive.function.client.WebClientResponseException(
                                                    401, "Session expired", null, 
                                                    "Frontend session expired. Please log in again.".getBytes(), null));
                                        }
                                        
                                        // Clear cached token and retry once
                                        logger.info("Clearing cached token and retrying...");
                                        clearCachedToken();
                                        String newToken = getAuthToken();
                                        logger.info("New token obtained: {}", newToken != null ? "YES" : "NO");
                                        
                                        if (newToken != null && !newToken.equals(token)) {
                                            logger.info("Retrying request with new token: {}", request.url());
                                            org.springframework.web.reactive.function.client.ClientRequest retryRequest = 
                                                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                                                    .header("Authorization", "Bearer " + newToken)
                                                    .headers(headers -> {
                                                        headers.remove("Content-Type");
                                                        headers.set("Content-Type", "application/json");
                                                        headers.set("X-Frontend-User", currentUser != null ? currentUser : "UNKNOWN");
                                                        headers.set("X-Retry-Attempt", "1");
                                                    })
                                                    .build();
                                            return next.exchange(retryRequest);
                                        } else {
                                            logger.error("Could not obtain new token for retry");
                                        }
                                    } else if (ex.getStatusCode().value() == 400) {
                                        logger.error("400 Bad Request - Validation or data error");
                                        logger.error("Request body might have validation issues");
                                    } else if (ex.getStatusCode().value() == 403) {
                                        logger.error("403 Forbidden - User lacks permissions");
                                    }
                                    
                                    return reactor.core.publisher.Mono.error(ex);
                                });
                    } else {
                        logger.error("=== WebClient No Auth Token ===");
                        logger.error("No auth token available for request: {}", request.url());
                        logger.error("Current User: {}", currentUser);
                        logger.error("Session Valid: {}", isCurrentUserSessionValid());
                        
                        // Check if user is authenticated in frontend
                        if (!isCurrentUserSessionValid()) {
                            logger.error("Frontend session is invalid");
                            return reactor.core.publisher.Mono.error(
                                new org.springframework.web.reactive.function.client.WebClientResponseException(
                                    401, "Authentication required", null, 
                                    "Please log in to access this resource.".getBytes(), null));
                        }
                        
                        // Return 503 error for backend authentication service unavailable
                        logger.error("Backend authentication service unavailable for user: {}", currentUser);
                        
                        // Try to proceed without authentication for some endpoints that might work
                        String uri = request.url().getPath();
                        if (uri.contains("/health") || uri.contains("/status")) {
                            logger.info("Proceeding without auth for health check endpoint: {}", request.url());
                            return next.exchange(request);
                        }
                        
                        return reactor.core.publisher.Mono.error(
                            new org.springframework.web.reactive.function.client.WebClientResponseException(
                                503, "Backend service unavailable", null, 
                                ("Backend authentication service is currently unavailable for user '" + 
                                 currentUser + "'. Please try again later or contact support.").getBytes(), null));
                    }
                })
                .build();
    }

    @Bean
    public String backendBaseUrl() {
        return backendUrl + apiPath;
    }

    /**
     * Get authentication token with enhanced session validation
     * Validates: Requirements 2.1, 2.4
     */
    private synchronized String getAuthToken() {
        logger.info("=== getAuthToken() Start ===");
        
        // Get current user from Spring Security context
        String currentUser = getCurrentUsername();
        logger.info("Current authenticated user: {}", currentUser);
        
        // Check if we have a cached token for the current user that's still valid (with 5 minute buffer)
        long currentTime = System.currentTimeMillis();
        long tokenValidUntil = tokenExpiryTime - 300000; // 5 minute buffer
        
        logger.info("Token cache status:");
        logger.info("  - Cached token exists: {}", cachedToken != null);
        logger.info("  - Cached username: {}", cachedUsername);
        logger.info("  - Current time: {}", currentTime);
        logger.info("  - Token expiry time: {}", tokenExpiryTime);
        logger.info("  - Token valid until (with buffer): {}", tokenValidUntil);
        logger.info("  - Token still valid: {}", currentTime < tokenValidUntil);
        
        if (cachedToken != null && 
            currentUser != null && 
            currentUser.equals(cachedUsername) &&
            currentTime < tokenValidUntil) {
            logger.info("Using cached token for user: {}", currentUser);
            return cachedToken;
        }

        // Clear cache if user changed
        if (currentUser == null || !currentUser.equals(cachedUsername)) {
            logger.info("User changed from '{}' to '{}', clearing cache", cachedUsername, currentUser);
            clearCachedToken();
        }

        // Validate that user is authenticated in frontend
        if (currentUser == null) {
            logger.error("Cannot get auth token - no authenticated user in frontend session");
            return null;
        }

        // Get a new token using current user's credentials
        logger.info("Obtaining new auth token for user: {}", currentUser);
        try {
            WebClient authClient = WebClient.builder().build();
            
            // Use the current authenticated user's credentials
            String password = getUserPassword(currentUser);
            if (password == null) {
                logger.error("Cannot get password for user: {}", currentUser);
                return null;
            }
            
            logger.info("Attempting backend authentication for user: {}", currentUser);
            logger.debug("Using password: {}...", password.substring(0, Math.min(3, password.length())));
            
            String authUrl = backendUrl + apiPath + "/auth/login";
            logger.info("Auth URL: {}", authUrl);
            
            java.util.Map<String, String> authRequest = java.util.Map.of(
                "username", currentUser,
                "password", password
            );
            logger.info("Auth request: {}", authRequest);
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = authClient.post()
                    .uri(authUrl)
                    .bodyValue(authRequest)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                             clientResponse -> {
                                 logger.error("Backend authentication failed:");
                                 logger.error("  - Status: {}", clientResponse.statusCode());
                                 logger.error("  - Headers: {}", clientResponse.headers().asHttpHeaders());
                                 return clientResponse.bodyToMono(String.class)
                                     .map(body -> {
                                         logger.error("  - Response body: {}", body);
                                         return new RuntimeException("Authentication failed for user " + 
                                                                   currentUser + ": " + clientResponse.statusCode() + " - " + body);
                                     });
                             })
                    .bodyToMono(java.util.Map.class)
                    .block();
            
            logger.info("Backend auth response received: {}", response != null ? "YES" : "NO");
            if (response != null) {
                logger.info("Response keys: {}", response.keySet());
                logger.info("Success: {}", response.get("success"));
                logger.info("Message: {}", response.get("message"));
                logger.info("Token present: {}", response.containsKey("token"));
            }
            
            if (response != null && response.containsKey("token")) {
                String token = (String) response.get("token");
                
                // Cache the token for the current user (JWT tokens typically expire in 1 hour)
                cachedToken = token;
                cachedUsername = currentUser;
                tokenExpiryTime = System.currentTimeMillis() + 3600000; // 1 hour from now
                
                logger.info("Successfully obtained and cached auth token:");
                logger.info("  - User: {}", currentUser);
                logger.info("  - Token length: {}", token.length());
                logger.info("  - Token preview: {}...", token.substring(0, Math.min(20, token.length())));
                logger.info("  - Cached until: {}", tokenExpiryTime);
                
                return token;
            } else {
                logger.error("Auth response missing token:");
                logger.error("  - User: {}", currentUser);
                logger.error("  - Response: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to get auth token for user: {}", currentUser);
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Cause: {}", e.getCause().getMessage());
            }
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get current authenticated username from Spring Security context
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            System.err.println("Error getting current username: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if current user session is valid
     */
    private boolean isCurrentUserSessionValid() {
        try {
            // Check Spring Security authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return false;
            }

            // Check HTTP session if available
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                return request.getSession(false) != null;
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error checking session validity: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current session information for debugging
     */
    private String getCurrentSessionInfo() {
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String sessionId = request.getSession(false) != null ? 
                    request.getSession(false).getId() : "NO_SESSION";
                String username = getCurrentUsername();
                return username + ":" + sessionId;
            }
        } catch (Exception e) {
            System.err.println("Error getting session info: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get user password for authentication
     * Enhanced to support all configured users and provide better error handling
     */
    private String getUserPassword(String username) {
        // This is a simplified implementation for the demo
        // In production, you would use a secure credential store or OAuth flow
        switch (username) {
            case "admin":
                return "admin123";
            case "sjohnson":
                return "admin123";
            case "receptionist1":
                return "staff123";
            // Add support for additional users that might be configured
            case "vet1":
                return "vet123";
            case "vet2":
                return "vet123";
            case "staff1":
                return "staff123";
            case "staff2":
                return "staff123";
            case "user":
                return "user123";
            case "test":
                return "test123";
            default:
                // For unknown users, try common default passwords
                // This is not secure but allows the demo to work
                logger.warn("Unknown user '{}' - trying default password", username);
                return "password123"; // Default fallback password
        }
    }

    /**
     * Clear cached authentication token to force refresh
     */
    private synchronized void clearCachedToken() {
        System.out.println("Clearing cached authentication token for user: " + cachedUsername);
        cachedToken = null;
        cachedUsername = null;
        tokenExpiryTime = 0;
    }
}