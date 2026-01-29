package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for authentication operations
 * Validates: Requirements 9.3, 9.4
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    private String token;
    private String message;
    private boolean success;
    private Long expiresIn;
    private String tokenType = "Bearer";

    public AuthResponse() {}

    public AuthResponse(String token, String message, boolean success) {
        this.token = token;
        this.message = message;
        this.success = success;
    }

    public AuthResponse(String token, String message, boolean success, Long expiresIn) {
        this.token = token;
        this.message = message;
        this.success = success;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}