package com.petclinic.backend.interceptor;

import com.petclinic.backend.service.PerformanceMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor for automatic performance monitoring of HTTP requests
 * Tracks response times, request sizes, and other performance metrics
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceInterceptor.class);
    
    private final PerformanceMonitoringService performanceMonitoringService;
    
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String REQUEST_SIZE_ATTRIBUTE = "requestSize";
    
    @Autowired
    public PerformanceInterceptor(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Record start time
            long startTime = System.currentTimeMillis();
            request.setAttribute(START_TIME_ATTRIBUTE, startTime);
            
            // Record request size if available
            String contentLength = request.getHeader("Content-Length");
            if (contentLength != null) {
                try {
                    long requestSize = Long.parseLong(contentLength);
                    request.setAttribute(REQUEST_SIZE_ATTRIBUTE, requestSize);
                } catch (NumberFormatException e) {
                    // Ignore invalid content length
                }
            }
            
            // Log request start for debugging
            logger.debug("Started processing request: {} {}", request.getMethod(), request.getRequestURI());
            
        } catch (Exception e) {
            logger.error("Error in performance interceptor preHandle: {}", e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        try {
            // Calculate response time
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            if (startTime != null) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                // Get request details
                String method = request.getMethod();
                String uri = request.getRequestURI();
                int statusCode = response.getStatus();
                
                // Get request and response sizes
                Long requestSize = (Long) request.getAttribute(REQUEST_SIZE_ATTRIBUTE);
                Long responseSize = getResponseSize(response);
                
                // Record API performance
                performanceMonitoringService.recordApiPerformance(
                    uri, method, responseTime, statusCode, requestSize, responseSize
                );
                
                // Log slow requests
                if (responseTime > 5000) { // 5 seconds threshold
                    logger.warn("Slow request detected: {} {} took {}ms (status: {})", 
                        method, uri, responseTime, statusCode);
                }
                
                // Log request completion for debugging
                logger.debug("Completed processing request: {} {} in {}ms (status: {})", 
                    method, uri, responseTime, statusCode);
                
                // Record specific performance metrics based on endpoint type
                recordSpecificMetrics(uri, method, responseTime, statusCode);
            }
            
        } catch (Exception e) {
            logger.error("Error in performance interceptor afterCompletion: {}", e.getMessage());
        }
    }
    
    private Long getResponseSize(HttpServletResponse response) {
        try {
            // Try to get response size from Content-Length header
            String contentLength = response.getHeader("Content-Length");
            if (contentLength != null) {
                return Long.parseLong(contentLength);
            }
            
            // If not available, return null (size unknown)
            return null;
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private void recordSpecificMetrics(String uri, String method, long responseTime, int statusCode) {
        try {
            // Record page load metrics for frontend pages
            if (isPageRequest(uri)) {
                String pageName = extractPageName(uri);
                performanceMonitoringService.recordPageLoadTime(pageName, responseTime, null);
            }
            
            // Record search metrics for search endpoints
            if (isSearchRequest(uri)) {
                String searchType = extractSearchType(uri);
                // Note: We don't have result count here, so we pass 0
                performanceMonitoringService.recordSearchPerformance(searchType, "unknown", 0, responseTime);
            }
            
            // Record filter metrics for filter endpoints
            if (isFilterRequest(uri)) {
                String entityType = extractEntityType(uri);
                // Note: We don't have filter count here, so we pass 0
                performanceMonitoringService.recordFilterPerformance(entityType, 0, 0, responseTime);
            }
            
        } catch (Exception e) {
            logger.error("Error recording specific metrics for {} {}: {}", method, uri, e.getMessage());
        }
    }
    
    private boolean isPageRequest(String uri) {
        // Check if this is a page request (not API)
        return !uri.startsWith("/api/") && 
               (uri.endsWith(".html") || uri.contains("/pets") || uri.contains("/owners") || 
                uri.contains("/visits") || uri.contains("/veterinarians") || uri.equals("/"));
    }
    
    private boolean isSearchRequest(String uri) {
        return uri.contains("/search") || uri.contains("/find");
    }
    
    private boolean isFilterRequest(String uri) {
        return uri.contains("/filter") || uri.contains("?filter") || uri.contains("&filter");
    }
    
    private String extractPageName(String uri) {
        try {
            if (uri.equals("/") || uri.equals("")) {
                return "home";
            }
            
            // Extract page name from URI
            String[] parts = uri.split("/");
            if (parts.length > 1) {
                String lastPart = parts[parts.length - 1];
                if (lastPart.contains(".")) {
                    return lastPart.substring(0, lastPart.lastIndexOf('.'));
                }
                return lastPart;
            }
            
            return "unknown";
            
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String extractSearchType(String uri) {
        try {
            if (uri.contains("/pets/search") || uri.contains("/pets/find")) {
                return "pet_search";
            } else if (uri.contains("/owners/search") || uri.contains("/owners/find")) {
                return "owner_search";
            } else if (uri.contains("/visits/search") || uri.contains("/visits/find")) {
                return "visit_search";
            } else if (uri.contains("/veterinarians/search") || uri.contains("/veterinarians/find")) {
                return "veterinarian_search";
            } else if (uri.contains("/search/global")) {
                return "global_search";
            } else {
                return "general_search";
            }
            
        } catch (Exception e) {
            return "unknown_search";
        }
    }
    
    private String extractEntityType(String uri) {
        try {
            if (uri.contains("/pets")) {
                return "pets";
            } else if (uri.contains("/owners")) {
                return "owners";
            } else if (uri.contains("/visits")) {
                return "visits";
            } else if (uri.contains("/veterinarians")) {
                return "veterinarians";
            } else {
                return "unknown";
            }
            
        } catch (Exception e) {
            return "unknown";
        }
    }
}