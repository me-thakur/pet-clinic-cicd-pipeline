package com.petclinic.backend.interceptor;

import com.petclinic.backend.service.DataSanitizationService;
import com.petclinic.backend.service.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Security validation interceptor to check all incoming requests for malicious content
 * and ensure proper data sanitization
 * 
 * Validates: Requirements 17.3, 17.4
 */
@Component
public class SecurityValidationInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityValidationInterceptor.class);
    
    @Autowired
    private DataSanitizationService dataSanitizationService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Skip validation for public endpoints
        if (isPublicEndpoint(request.getRequestURI())) {
            return true;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Validate request parameters
        if (!validateRequestParameters(request, authentication)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid request parameters detected\",\"success\":false}");
            return false;
        }
        
        // Validate request headers
        if (!validateRequestHeaders(request, authentication)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid request headers detected\",\"success\":false}");
            return false;
        }
        
        // Check for suspicious patterns in the request
        if (containsSuspiciousPatterns(request)) {
            securityAuditService.logAccessDenied("SUSPICIOUS_REQUEST", request.getRequestURI(), 
                authentication, "Request contains suspicious patterns");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Request blocked for security reasons\",\"success\":false}");
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if the endpoint is public and doesn't require validation
     */
    private boolean isPublicEndpoint(String uri) {
        if (uri == null) return false;
        
        return uri.startsWith("/api/auth/") || 
               uri.startsWith("/api/test/") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/swagger-ui");
    }
    
    /**
     * Validate all request parameters for malicious content
     */
    private boolean validateRequestParameters(HttpServletRequest request, Authentication authentication) {
        Enumeration<String> parameterNames = request.getParameterNames();
        
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            
            // Validate parameter name
            if (dataSanitizationService.containsMaliciousContent(paramName)) {
                logger.warn("Malicious parameter name detected: {}", paramName);
                securityAuditService.logAccessDenied("MALICIOUS_PARAMETER_NAME", request.getRequestURI(), 
                    authentication, "Parameter name contains malicious content: " + paramName);
                return false;
            }
            
            // Validate parameter values
            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (dataSanitizationService.containsMaliciousContent(paramValue)) {
                        logger.warn("Malicious parameter value detected for {}: {}", paramName, 
                                   paramValue.substring(0, Math.min(50, paramValue.length())));
                        securityAuditService.logAccessDenied("MALICIOUS_PARAMETER_VALUE", request.getRequestURI(), 
                            authentication, "Parameter value contains malicious content");
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Validate request headers for malicious content
     */
    private boolean validateRequestHeaders(HttpServletRequest request, Authentication authentication) {
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            // Skip standard security headers
            if (isStandardSecurityHeader(headerName)) {
                continue;
            }
            
            // Validate header name
            if (dataSanitizationService.containsMaliciousContent(headerName)) {
                logger.warn("Malicious header name detected: {}", headerName);
                securityAuditService.logAccessDenied("MALICIOUS_HEADER_NAME", request.getRequestURI(), 
                    authentication, "Header name contains malicious content: " + headerName);
                return false;
            }
            
            // Validate header value
            if (headerValue != null && dataSanitizationService.containsMaliciousContent(headerValue)) {
                logger.warn("Malicious header value detected for {}: {}", headerName, 
                           headerValue.substring(0, Math.min(50, headerValue.length())));
                securityAuditService.logAccessDenied("MALICIOUS_HEADER_VALUE", request.getRequestURI(), 
                    authentication, "Header value contains malicious content");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if header is a standard security header that should be allowed
     */
    private boolean isStandardSecurityHeader(String headerName) {
        if (headerName == null) return false;
        
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.equals("authorization") ||
               lowerHeaderName.equals("content-type") ||
               lowerHeaderName.equals("accept") ||
               lowerHeaderName.equals("user-agent") ||
               lowerHeaderName.equals("referer") ||
               lowerHeaderName.equals("origin") ||
               lowerHeaderName.equals("x-requested-with") ||
               lowerHeaderName.equals("x-csrf-token") ||
               lowerHeaderName.startsWith("x-forwarded-");
    }
    
    /**
     * Check for suspicious patterns in the entire request
     */
    private boolean containsSuspiciousPatterns(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // Check URI for suspicious patterns
        if (uri != null && dataSanitizationService.containsMaliciousContent(uri)) {
            logger.warn("Suspicious URI pattern detected: {}", uri);
            return true;
        }
        
        // Check query string for suspicious patterns
        if (queryString != null && dataSanitizationService.containsMaliciousContent(queryString)) {
            logger.warn("Suspicious query string pattern detected: {}", queryString);
            return true;
        }
        
        // Check for directory traversal attempts
        if (uri != null && (uri.contains("../") || uri.contains("..\\") || uri.contains("%2e%2e"))) {
            logger.warn("Directory traversal attempt detected: {}", uri);
            return true;
        }
        
        // Check for excessive parameter count (potential DoS)
        if (request.getParameterMap().size() > 100) {
            logger.warn("Excessive parameter count detected: {}", request.getParameterMap().size());
            return true;
        }
        
        return false;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Log security-relevant response information
        if (response.getStatus() >= 400) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            Map<String, Object> context = new HashMap<>();
            context.put("responseStatus", response.getStatus());
            context.put("requestMethod", request.getMethod());
            context.put("requestURI", request.getRequestURI());
            
            if (ex != null) {
                context.put("exception", ex.getClass().getSimpleName());
            }
            
            securityAuditService.logSecurityOperation("REQUEST_COMPLETED_WITH_ERROR", 
                request.getRequestURI(), null, authentication, context);
        }
    }
}