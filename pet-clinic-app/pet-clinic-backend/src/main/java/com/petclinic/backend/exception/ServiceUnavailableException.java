package com.petclinic.backend.exception;

import java.util.List;
import java.util.ArrayList;

/**
 * Exception thrown when a service is temporarily unavailable
 */
public class ServiceUnavailableException extends RuntimeException {
    
    private final String serviceName;
    private final String retryAfter;
    private final List<String> fallbackOptions;
    
    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.retryAfter = "30 seconds";
        this.fallbackOptions = new ArrayList<>();
    }
    
    public ServiceUnavailableException(String serviceName, String message, String retryAfter) {
        super(message);
        this.serviceName = serviceName;
        this.retryAfter = retryAfter;
        this.fallbackOptions = new ArrayList<>();
    }
    
    public ServiceUnavailableException(String serviceName, String message, String retryAfter, List<String> fallbackOptions) {
        super(message);
        this.serviceName = serviceName;
        this.retryAfter = retryAfter;
        this.fallbackOptions = fallbackOptions != null ? fallbackOptions : new ArrayList<>();
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getRetryAfter() {
        return retryAfter;
    }
    
    public List<String> getFallbackOptions() {
        return fallbackOptions;
    }
    
    public void addFallbackOption(String option) {
        this.fallbackOptions.add(option);
    }
}