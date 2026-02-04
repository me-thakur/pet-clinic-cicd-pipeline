package com.petclinic.backend.interceptor;

import com.petclinic.backend.config.ApiVersionConfig;
import com.petclinic.backend.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Interceptor to handle API versioning validation
 * Ensures requested API versions are supported and provides appropriate responses
 */
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    private final ObjectMapper objectMapper;
    
    public ApiVersionInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only check API endpoints
        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }
        
        String requestedVersion = ApiVersionConfig.getApiVersion(request);
        
        // Check if the requested version is supported
        if (!ApiVersionConfig.isSupportedVersion(requestedVersion)) {
            handleUnsupportedVersion(request, response, requestedVersion);
            return false;
        }
        
        // Add version header to response
        response.setHeader(ApiVersionConfig.API_VERSION_HEADER, ApiVersionConfig.CURRENT_API_VERSION);
        
        return true;
    }
    
    /**
     * Handle unsupported API version requests
     */
    private void handleUnsupportedVersion(HttpServletRequest request, HttpServletResponse response, String requestedVersion) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("UNSUPPORTED_API_VERSION")
            .message(String.format("API version '%s' is not supported. Current version: %s", 
                requestedVersion, ApiVersionConfig.CURRENT_API_VERSION))
            .suggestions(List.of(
                "Please use a supported API version",
                "Check the API documentation for supported versions",
                "Update your client to use version " + ApiVersionConfig.CURRENT_API_VERSION
            ))
            .path(request.getRequestURI())
            .build();
        
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}