package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.PerformanceMonitoringService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementation of performance monitoring service
 * Provides comprehensive performance metrics collection and analysis
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5
 */
@Service
public class PerformanceMonitoringServiceImpl implements PerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringServiceImpl.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    
    // Performance thresholds (in milliseconds)
    private static final long PAGE_LOAD_THRESHOLD = 2000; // 2 seconds
    private static final long SEARCH_THRESHOLD = 3000; // 3 seconds
    private static final long FILTER_THRESHOLD = 1000; // 1 second
    private static final long API_THRESHOLD = 5000; // 5 seconds
    
    private final MeterRegistry meterRegistry;
    
    // In-memory storage for recent metrics (for quick access)
    private final Map<String, List<PerformanceMetric>> recentMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Double> averages = new ConcurrentHashMap<>();
    
    @Autowired
    public PerformanceMonitoringServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }
    
    private void initializeMetrics() {
        // Initialize Micrometer metrics with proper builder syntax
        Gauge.builder("petclinic.performance.page_load.average", this, PerformanceMonitoringServiceImpl::getAveragePageLoadTime)
            .description("Average page load time")
            .register(meterRegistry);
        
        Gauge.builder("petclinic.performance.search.average", this, PerformanceMonitoringServiceImpl::getAverageSearchTime)
            .description("Average search response time")
            .register(meterRegistry);
        
        Gauge.builder("petclinic.performance.filter.average", this, PerformanceMonitoringServiceImpl::getAverageFilterTime)
            .description("Average filter response time")
            .register(meterRegistry);
    }
    
    @Override
    public void recordPageLoadTime(String pageName, long loadTimeMs, String userId) {
        try {
            // Record in Micrometer
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("petclinic.page.load.time")
                .description("Page load time")
                .tag("page", pageName)
                .tag("user", userId != null ? userId : "anonymous")
                .register(meterRegistry));
            
            // Store in recent metrics
            PerformanceMetric metric = new PerformanceMetric(
                "page_load", pageName, loadTimeMs, LocalDateTime.now(), 
                Map.of("userId", userId != null ? userId : "anonymous")
            );
            addToRecentMetrics("page_load", metric);
            
            // Update counters and averages
            updateCounterAndAverage("page_load_" + pageName, loadTimeMs);
            
            // Log performance issue if threshold exceeded
            if (loadTimeMs > PAGE_LOAD_THRESHOLD) {
                performanceLogger.warn("Page load time exceeded threshold: {} took {}ms (threshold: {}ms)", 
                    pageName, loadTimeMs, PAGE_LOAD_THRESHOLD);
            }
            
            logger.debug("Recorded page load time: {} = {}ms", pageName, loadTimeMs);
            
        } catch (Exception e) {
            logger.error("Error recording page load time for {}: {}", pageName, e.getMessage());
        }
    }
    
    @Override
    public void recordSearchPerformance(String searchType, String queryComplexity, 
                                       int resultCount, long executionTimeMs) {
        try {
            // Record in Micrometer
            Timer.builder("petclinic.search.execution.time")
                .description("Search execution time")
                .tag("type", searchType)
                .tag("complexity", queryComplexity)
                .register(meterRegistry)
                .record(Duration.ofMillis(executionTimeMs));
            
            Counter.builder("petclinic.search.results.count")
                .description("Search results count")
                .tag("type", searchType)
                .register(meterRegistry)
                .increment(resultCount);
            
            // Store in recent metrics
            PerformanceMetric metric = new PerformanceMetric(
                "search", searchType, executionTimeMs, LocalDateTime.now(),
                Map.of("complexity", queryComplexity, "resultCount", String.valueOf(resultCount))
            );
            addToRecentMetrics("search", metric);
            
            // Update counters and averages
            updateCounterAndAverage("search_" + searchType, executionTimeMs);
            
            // Log performance issue if threshold exceeded
            if (executionTimeMs > SEARCH_THRESHOLD) {
                performanceLogger.warn("Search execution time exceeded threshold: {} took {}ms (threshold: {}ms)", 
                    searchType, executionTimeMs, SEARCH_THRESHOLD);
            }
            
            logger.debug("Recorded search performance: {} = {}ms, {} results", 
                searchType, executionTimeMs, resultCount);
            
        } catch (Exception e) {
            logger.error("Error recording search performance for {}: {}", searchType, e.getMessage());
        }
    }
    
    @Override
    public void recordFilterPerformance(String entityType, int filterCount, 
                                       int resultCount, long executionTimeMs) {
        try {
            // Record in Micrometer
            Timer.builder("petclinic.filter.execution.time")
                .description("Filter execution time")
                .tag("entity", entityType)
                .tag("filter_count", String.valueOf(filterCount))
                .register(meterRegistry)
                .record(Duration.ofMillis(executionTimeMs));
            
            // Store in recent metrics
            PerformanceMetric metric = new PerformanceMetric(
                "filter", entityType, executionTimeMs, LocalDateTime.now(),
                Map.of("filterCount", String.valueOf(filterCount), "resultCount", String.valueOf(resultCount))
            );
            addToRecentMetrics("filter", metric);
            
            // Update counters and averages
            updateCounterAndAverage("filter_" + entityType, executionTimeMs);
            
            // Log performance issue if threshold exceeded
            if (executionTimeMs > FILTER_THRESHOLD) {
                performanceLogger.warn("Filter execution time exceeded threshold: {} took {}ms (threshold: {}ms)", 
                    entityType, executionTimeMs, FILTER_THRESHOLD);
            }
            
            logger.debug("Recorded filter performance: {} = {}ms, {} filters, {} results", 
                entityType, executionTimeMs, filterCount, resultCount);
            
        } catch (Exception e) {
            logger.error("Error recording filter performance for {}: {}", entityType, e.getMessage());
        }
    }
    
    @Override
    public void recordDatabaseQueryPerformance(String queryType, String entityType, 
                                              long executionTimeMs, int recordCount) {
        try {
            // Record in Micrometer
            Timer.builder("petclinic.database.query.time")
                .description("Database query execution time")
                .tag("query_type", queryType)
                .tag("entity", entityType)
                .register(meterRegistry)
                .record(Duration.ofMillis(executionTimeMs));
            
            Counter.builder("petclinic.database.records.processed")
                .description("Database records processed")
                .tag("query_type", queryType)
                .tag("entity", entityType)
                .register(meterRegistry)
                .increment(recordCount);
            
            // Store in recent metrics
            PerformanceMetric metric = new PerformanceMetric(
                "database", queryType + "_" + entityType, executionTimeMs, LocalDateTime.now(),
                Map.of("queryType", queryType, "entityType", entityType, "recordCount", String.valueOf(recordCount))
            );
            addToRecentMetrics("database", metric);
            
            // Update counters and averages
            updateCounterAndAverage("db_" + queryType + "_" + entityType, executionTimeMs);
            
            logger.debug("Recorded database query performance: {} {} = {}ms, {} records", 
                queryType, entityType, executionTimeMs, recordCount);
            
        } catch (Exception e) {
            logger.error("Error recording database query performance for {} {}: {}", 
                queryType, entityType, e.getMessage());
        }
    }
    
    @Override
    public void recordApiPerformance(String endpoint, String httpMethod, long responseTimeMs, 
                                    int statusCode, Long requestSize, Long responseSize) {
        try {
            // Record in Micrometer
            Timer.builder("petclinic.api.response.time")
                .description("API response time")
                .tag("endpoint", endpoint)
                .tag("method", httpMethod)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(Duration.ofMillis(responseTimeMs));
            
            if (requestSize != null) {
                Counter.builder("petclinic.api.request.size")
                    .description("API request size")
                    .tag("endpoint", endpoint)
                    .register(meterRegistry)
                    .increment(requestSize);
            }
            
            if (responseSize != null) {
                Counter.builder("petclinic.api.response.size")
                    .description("API response size")
                    .tag("endpoint", endpoint)
                    .register(meterRegistry)
                    .increment(responseSize);
            }
            
            // Store in recent metrics
            Map<String, String> metadata = new HashMap<>();
            metadata.put("httpMethod", httpMethod);
            metadata.put("statusCode", String.valueOf(statusCode));
            if (requestSize != null) metadata.put("requestSize", String.valueOf(requestSize));
            if (responseSize != null) metadata.put("responseSize", String.valueOf(responseSize));
            
            PerformanceMetric metric = new PerformanceMetric(
                "api", endpoint, responseTimeMs, LocalDateTime.now(), metadata
            );
            addToRecentMetrics("api", metric);
            
            // Update counters and averages
            updateCounterAndAverage("api_" + endpoint.replaceAll("/", "_"), responseTimeMs);
            
            // Log performance issue if threshold exceeded
            if (responseTimeMs > API_THRESHOLD) {
                performanceLogger.warn("API response time exceeded threshold: {} {} took {}ms (threshold: {}ms)", 
                    httpMethod, endpoint, responseTimeMs, API_THRESHOLD);
            }
            
            logger.debug("Recorded API performance: {} {} = {}ms, status {}", 
                httpMethod, endpoint, responseTimeMs, statusCode);
            
        } catch (Exception e) {
            logger.error("Error recording API performance for {} {}: {}", httpMethod, endpoint, e.getMessage());
        }
    }
    
    @Override
    public void recordCachePerformance(String cacheName, String operation, long executionTimeMs) {
        try {
            // Record in Micrometer
            Timer.builder("petclinic.cache.operation.time")
                .description("Cache operation time")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .register(meterRegistry)
                .record(Duration.ofMillis(executionTimeMs));
            
            Counter.builder("petclinic.cache.operations")
                .description("Cache operations count")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
            
            // Store in recent metrics
            PerformanceMetric metric = new PerformanceMetric(
                "cache", cacheName + "_" + operation, executionTimeMs, LocalDateTime.now(),
                Map.of("operation", operation)
            );
            addToRecentMetrics("cache", metric);
            
            logger.debug("Recorded cache performance: {} {} = {}ms", cacheName, operation, executionTimeMs);
            
        } catch (Exception e) {
            logger.error("Error recording cache performance for {} {}: {}", cacheName, operation, e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getCurrentPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Page load metrics
            metrics.put("pageLoadAverage", getAveragePageLoadTime());
            metrics.put("pageLoadAcceptable", isPagePerformanceAcceptable("overall"));
            
            // Search metrics
            metrics.put("searchAverage", getAverageSearchTime());
            metrics.put("searchAcceptable", isSearchPerformanceAcceptable("overall"));
            
            // Filter metrics
            metrics.put("filterAverage", getAverageFilterTime());
            
            // Recent performance issues
            metrics.put("recentIssues", getRecentPerformanceIssues());
            
            // Cache hit rates
            metrics.put("cacheMetrics", getCacheMetrics());
            
            // System resource usage
            metrics.put("systemMetrics", getSystemMetrics());
            
            metrics.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting current performance metrics: {}", e.getMessage());
            metrics.put("error", "Failed to retrieve metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    @Override
    public Map<String, Object> getPerformanceMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Filter recent metrics by time range
            for (Map.Entry<String, List<PerformanceMetric>> entry : recentMetrics.entrySet()) {
                String category = entry.getKey();
                List<PerformanceMetric> filteredMetrics = entry.getValue().stream()
                    .filter(metric -> metric.getTimestamp().isAfter(startTime) && 
                                     metric.getTimestamp().isBefore(endTime))
                    .collect(Collectors.toList());
                
                if (!filteredMetrics.isEmpty()) {
                    double average = filteredMetrics.stream()
                        .mapToLong(PerformanceMetric::getExecutionTimeMs)
                        .average()
                        .orElse(0.0);
                    
                    long max = filteredMetrics.stream()
                        .mapToLong(PerformanceMetric::getExecutionTimeMs)
                        .max()
                        .orElse(0L);
                    
                    long min = filteredMetrics.stream()
                        .mapToLong(PerformanceMetric::getExecutionTimeMs)
                        .min()
                        .orElse(0L);
                    
                    Map<String, Object> categoryMetrics = new HashMap<>();
                    categoryMetrics.put("average", average);
                    categoryMetrics.put("max", max);
                    categoryMetrics.put("min", min);
                    categoryMetrics.put("count", filteredMetrics.size());
                    
                    metrics.put(category, categoryMetrics);
                }
            }
            
            metrics.put("startTime", startTime);
            metrics.put("endTime", endTime);
            
        } catch (Exception e) {
            logger.error("Error getting performance metrics for time range: {}", e.getMessage());
            metrics.put("error", "Failed to retrieve metrics: " + e.getMessage());
        }
        
        return metrics;
    }
    
    @Override
    public Map<String, Object> getPerformanceAlerts() {
        Map<String, Object> alerts = new HashMap<>();
        List<Map<String, Object>> activeAlerts = new ArrayList<>();
        
        try {
            // Check page load performance
            if (!isPagePerformanceAcceptable("overall")) {
                activeAlerts.add(createAlert("HIGH", "Page Load Performance", 
                    "Average page load time exceeds 2 second threshold", 
                    "Optimize frontend assets and server response times"));
            }
            
            // Check search performance
            if (!isSearchPerformanceAcceptable("overall")) {
                activeAlerts.add(createAlert("HIGH", "Search Performance", 
                    "Average search time exceeds 3 second threshold", 
                    "Optimize database queries and add proper indexing"));
            }
            
            // Check for recent performance spikes
            List<PerformanceMetric> recentApiMetrics = recentMetrics.getOrDefault("api", new ArrayList<>());
            long recentSlowApis = recentApiMetrics.stream()
                .filter(metric -> metric.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(5)))
                .filter(metric -> metric.getExecutionTimeMs() > API_THRESHOLD)
                .count();
            
            if (recentSlowApis > 5) {
                activeAlerts.add(createAlert("MEDIUM", "API Performance Spike", 
                    "Multiple API endpoints showing slow response times", 
                    "Check server resources and database performance"));
            }
            
            alerts.put("alerts", activeAlerts);
            alerts.put("alertCount", activeAlerts.size());
            alerts.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting performance alerts: {}", e.getMessage());
            alerts.put("error", "Failed to retrieve alerts: " + e.getMessage());
        }
        
        return alerts;
    }
    
    @Override
    public boolean isPagePerformanceAcceptable(String pageName) {
        try {
            if ("overall".equals(pageName)) {
                return getAveragePageLoadTime() <= PAGE_LOAD_THRESHOLD;
            }
            
            Double average = averages.get("page_load_" + pageName);
            return average == null || average <= PAGE_LOAD_THRESHOLD;
            
        } catch (Exception e) {
            logger.error("Error checking page performance for {}: {}", pageName, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isSearchPerformanceAcceptable(String searchType) {
        try {
            if ("overall".equals(searchType)) {
                return getAverageSearchTime() <= SEARCH_THRESHOLD;
            }
            
            Double average = averages.get("search_" + searchType);
            return average == null || average <= SEARCH_THRESHOLD;
            
        } catch (Exception e) {
            logger.error("Error checking search performance for {}: {}", searchType, e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getOptimizationRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        try {
            // Analyze page load performance
            if (!isPagePerformanceAcceptable("overall")) {
                suggestions.add(createRecommendation("HIGH", "Page Load Optimization",
                    "Implement browser caching, compress assets, optimize images",
                    "Frontend"));
            }
            
            // Analyze search performance
            if (!isSearchPerformanceAcceptable("overall")) {
                suggestions.add(createRecommendation("HIGH", "Search Optimization",
                    "Add database indexes, implement result caching, optimize queries",
                    "Backend"));
            }
            
            // Analyze cache performance
            Map<String, Object> cacheMetrics = getCacheMetrics();
            if (cacheMetrics.containsKey("hitRate")) {
                Double hitRate = (Double) cacheMetrics.get("hitRate");
                if (hitRate != null && hitRate < 0.8) {
                    suggestions.add(createRecommendation("MEDIUM", "Cache Optimization",
                        "Improve cache hit rate by adjusting cache size and TTL settings",
                        "Configuration"));
                }
            }
            
            // Check for database performance issues
            List<PerformanceMetric> dbMetrics = recentMetrics.getOrDefault("database", new ArrayList<>());
            long slowQueries = dbMetrics.stream()
                .filter(metric -> metric.getExecutionTimeMs() > 1000)
                .count();
            
            if (slowQueries > 10) {
                suggestions.add(createRecommendation("HIGH", "Database Optimization",
                    "Optimize slow queries, add missing indexes, consider query result caching",
                    "Database"));
            }
            
            recommendations.put("recommendations", suggestions);
            recommendations.put("recommendationCount", suggestions.size());
            recommendations.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting optimization recommendations: {}", e.getMessage());
            recommendations.put("error", "Failed to generate recommendations: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    @Override
    @Async
    public CompletableFuture<Map<String, Object>> analyzePerformanceAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> analysis = new HashMap<>();
                
                // Perform comprehensive analysis
                analysis.put("currentMetrics", getCurrentPerformanceMetrics());
                analysis.put("alerts", getPerformanceAlerts());
                analysis.put("recommendations", getOptimizationRecommendations());
                analysis.put("trends", analyzePerformanceTrends());
                
                analysis.put("analysisTimestamp", LocalDateTime.now());
                
                return analysis;
                
            } catch (Exception e) {
                logger.error("Error during async performance analysis: {}", e.getMessage());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Analysis failed: " + e.getMessage());
                return errorResult;
            }
        });
    }
    
    @Override
    public void cleanupOldMetrics(int retentionDays) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            for (Map.Entry<String, List<PerformanceMetric>> entry : recentMetrics.entrySet()) {
                List<PerformanceMetric> metrics = entry.getValue();
                metrics.removeIf(metric -> metric.getTimestamp().isBefore(cutoffTime));
            }
            
            logger.info("Cleaned up performance metrics older than {} days", retentionDays);
            
        } catch (Exception e) {
            logger.error("Error cleaning up old metrics: {}", e.getMessage());
        }
    }
    
    @Override
    public String exportMetrics(String exportFormat) {
        try {
            Map<String, Object> allMetrics = getCurrentPerformanceMetrics();
            
            switch (exportFormat.toLowerCase()) {
                case "json":
                    return exportAsJson(allMetrics);
                case "csv":
                    return exportAsCsv(allMetrics);
                case "prometheus":
                    return exportAsPrometheus(allMetrics);
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + exportFormat);
            }
            
        } catch (Exception e) {
            logger.error("Error exporting metrics in format {}: {}", exportFormat, e.getMessage());
            return "Error exporting metrics: " + e.getMessage();
        }
    }
    
    // Helper methods
    
    private void addToRecentMetrics(String category, PerformanceMetric metric) {
        recentMetrics.computeIfAbsent(category, k -> new ArrayList<>()).add(metric);
        
        // Keep only recent metrics (last 1000 entries per category)
        List<PerformanceMetric> metrics = recentMetrics.get(category);
        if (metrics.size() > 1000) {
            metrics.subList(0, metrics.size() - 1000).clear();
        }
    }
    
    private void updateCounterAndAverage(String key, long value) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        long count = counter.incrementAndGet();
        
        Double currentAverage = averages.get(key);
        if (currentAverage == null) {
            averages.put(key, (double) value);
        } else {
            // Calculate running average
            double newAverage = (currentAverage * (count - 1) + value) / count;
            averages.put(key, newAverage);
        }
    }
    
    private double getAveragePageLoadTime() {
        return averages.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("page_load_"))
            .mapToDouble(Map.Entry::getValue)
            .average()
            .orElse(0.0);
    }
    
    private double getAverageSearchTime() {
        return averages.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("search_"))
            .mapToDouble(Map.Entry::getValue)
            .average()
            .orElse(0.0);
    }
    
    private double getAverageFilterTime() {
        return averages.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("filter_"))
            .mapToDouble(Map.Entry::getValue)
            .average()
            .orElse(0.0);
    }
    
    private List<Map<String, Object>> getRecentPerformanceIssues() {
        List<Map<String, Object>> issues = new ArrayList<>();
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(10);
        
        for (Map.Entry<String, List<PerformanceMetric>> entry : recentMetrics.entrySet()) {
            String category = entry.getKey();
            List<PerformanceMetric> metrics = entry.getValue();
            
            long threshold = getThresholdForCategory(category);
            
            List<PerformanceMetric> recentIssues = metrics.stream()
                .filter(metric -> metric.getTimestamp().isAfter(recentTime))
                .filter(metric -> metric.getExecutionTimeMs() > threshold)
                .collect(Collectors.toList());
            
            for (PerformanceMetric metric : recentIssues) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("category", category);
                issue.put("operation", metric.getOperation());
                issue.put("executionTime", metric.getExecutionTimeMs());
                issue.put("threshold", threshold);
                issue.put("timestamp", metric.getTimestamp());
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    private long getThresholdForCategory(String category) {
        switch (category) {
            case "page_load": return PAGE_LOAD_THRESHOLD;
            case "search": return SEARCH_THRESHOLD;
            case "filter": return FILTER_THRESHOLD;
            case "api": return API_THRESHOLD;
            default: return 1000L;
        }
    }
    
    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> cacheMetrics = new HashMap<>();
        
        // This would typically integrate with actual cache statistics
        // For now, return placeholder metrics
        cacheMetrics.put("hitRate", 0.85);
        cacheMetrics.put("missRate", 0.15);
        cacheMetrics.put("evictionRate", 0.02);
        
        return cacheMetrics;
    }
    
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        // Get JVM metrics
        Runtime runtime = Runtime.getRuntime();
        systemMetrics.put("memoryUsed", runtime.totalMemory() - runtime.freeMemory());
        systemMetrics.put("memoryTotal", runtime.totalMemory());
        systemMetrics.put("memoryMax", runtime.maxMemory());
        systemMetrics.put("processors", runtime.availableProcessors());
        
        return systemMetrics;
    }
    
    private Map<String, Object> createAlert(String severity, String title, String description, String recommendation) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("severity", severity);
        alert.put("title", title);
        alert.put("description", description);
        alert.put("recommendation", recommendation);
        alert.put("timestamp", LocalDateTime.now());
        return alert;
    }
    
    private Map<String, Object> createRecommendation(String priority, String title, String description, String category) {
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("priority", priority);
        recommendation.put("title", title);
        recommendation.put("description", description);
        recommendation.put("category", category);
        return recommendation;
    }
    
    private Map<String, Object> analyzePerformanceTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        // Analyze trends over the last hour vs previous hour
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        
        Map<String, Object> currentHour = getPerformanceMetrics(oneHourAgo, now);
        Map<String, Object> previousHour = getPerformanceMetrics(twoHoursAgo, oneHourAgo);
        
        trends.put("currentHour", currentHour);
        trends.put("previousHour", previousHour);
        trends.put("trend", calculateTrend(currentHour, previousHour));
        
        return trends;
    }
    
    private String calculateTrend(Map<String, Object> current, Map<String, Object> previous) {
        // Simple trend calculation - could be more sophisticated
        if (current.size() > previous.size()) {
            return "INCREASING_ACTIVITY";
        } else if (current.size() < previous.size()) {
            return "DECREASING_ACTIVITY";
        } else {
            return "STABLE";
        }
    }
    
    private String exportAsJson(Map<String, Object> metrics) {
        // Simple JSON export - in production, use proper JSON library
        return metrics.toString();
    }
    
    private String exportAsCsv(Map<String, Object> metrics) {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value,Timestamp\n");
        
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            csv.append(entry.getKey()).append(",")
               .append(entry.getValue()).append(",")
               .append(LocalDateTime.now()).append("\n");
        }
        
        return csv.toString();
    }
    
    private String exportAsPrometheus(Map<String, Object> metrics) {
        StringBuilder prometheus = new StringBuilder();
        
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            if (entry.getValue() instanceof Number) {
                prometheus.append("petclinic_").append(entry.getKey().replaceAll("[^a-zA-Z0-9_]", "_"))
                          .append(" ").append(entry.getValue()).append("\n");
            }
        }
        
        return prometheus.toString();
    }
    
    // Inner class for performance metrics
    private static class PerformanceMetric {
        private final String category;
        private final String operation;
        private final long executionTimeMs;
        private final LocalDateTime timestamp;
        private final Map<String, String> metadata;
        
        public PerformanceMetric(String category, String operation, long executionTimeMs, 
                               LocalDateTime timestamp, Map<String, String> metadata) {
            this.category = category;
            this.operation = operation;
            this.executionTimeMs = executionTimeMs;
            this.timestamp = timestamp;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters
        public String getCategory() { return category; }
        public String getOperation() { return operation; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}