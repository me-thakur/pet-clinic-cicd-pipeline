package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.FieldValidationError;
import com.petclinic.backend.dto.ValidationResult;
import com.petclinic.backend.service.ValidationMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Implementation of ValidationMetricsService using Micrometer metrics
 * Provides comprehensive metrics collection for validation operations with Prometheus integration
 * Validates: Requirements 4.5
 */
@Service
public class ValidationMetricsServiceImpl implements ValidationMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationMetricsServiceImpl.class);
    
    private final MeterRegistry meterRegistry;
    
    // In-memory tracking for success rate calculations
    private final ConcurrentHashMap<String, AtomicLong> validationAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> validationSuccesses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> processingTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> processingCounts = new ConcurrentHashMap<>();
    
    @Autowired
    public ValidationMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        logger.info("ValidationMetricsService initialized with Micrometer metrics");
    }
    
    @Override
    public void recordValidationAttempt(String validationType, ValidationResult result, long processingTimeMs) {
        try {
            // Record basic attempt using tags
            Counter.builder("validation.attempts.total")
                .description("Total number of validation attempts")
                .tag("component", "owner-validation")
                .tag("validation_type", validationType)
                .tag("outcome", result.isValid() ? "success" : "failure")
                .register(meterRegistry)
                .increment();
            
            // Update in-memory counters
            validationAttempts.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
            
            if (result.isValid()) {
                recordValidationSuccess(validationType, processingTimeMs);
            } else {
                recordValidationFailure(validationType, result.getErrorCount(), processingTimeMs);
                
                // Record individual field errors
                if (result.getFieldErrors() != null) {
                    for (FieldValidationError error : result.getFieldErrors()) {
                        recordValidationError(error.getErrorCode(), error.getFieldName());
                    }
                }
            }
            
            // Record processing time
            recordValidationProcessingTime(validationType, processingTimeMs);
            
            logger.debug("Recorded validation attempt: type={}, success={}, processingTime={}ms, errorCount={}", 
                        validationType, result.isValid(), processingTimeMs, result.getErrorCount());
                        
        } catch (Exception e) {
            logger.error("Error recording validation attempt metrics: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void recordValidationSuccess(String validationType, long processingTimeMs) {
        try {
            Counter.builder("validation.success.total")
                .description("Total number of successful validations")
                .tag("component", "owner-validation")
                .tag("validation_type", validationType)
                .register(meterRegistry)
                .increment();
            
            // Update in-memory success counter
            validationSuccesses.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
            
            logger.debug("Recorded validation success: type={}, processingTime={}ms", validationType, processingTimeMs);
            
        } catch (Exception e) {
            logger.error("Error recording validation success metrics: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void recordValidationFailure(String validationType, int errorCount, long processingTimeMs) {
        try {
            Counter.builder("validation.failure.total")
                .description("Total number of failed validations")
                .tag("component", "owner-validation")
                .tag("validation_type", validationType)
                .tag("error_count", String.valueOf(errorCount))
                .register(meterRegistry)
                .increment();
            
            logger.debug("Recorded validation failure: type={}, errorCount={}, processingTime={}ms", 
                        validationType, errorCount, processingTimeMs);
                        
        } catch (Exception e) {
            logger.error("Error recording validation failure metrics: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void recordFieldValidation(String fieldName, String validationType, boolean success) {
        try {
            Counter.builder("validation.field.total")
                .description("Total number of field validations")
                .tag("component", "owner-validation")
                .tag("field_name", fieldName)
                .tag("validation_type", validationType)
                .tag("outcome", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
            
            logger.debug("Recorded field validation: field={}, type={}, success={}", fieldName, validationType, success);
            
        } catch (Exception e) {
            logger.error("Error recording field validation metrics: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void recordValidationError(String errorType, String fieldName) {
        try {
            Counter.builder("validation.error.total")
                .description("Total number of validation errors by type")
                .tag("component", "owner-validation")
                .tag("error_type", errorType)
                .tag("field_name", fieldName)
                .register(meterRegistry)
                .increment();
            
            logger.debug("Recorded validation error: type={}, field={}", errorType, fieldName);
            
        } catch (Exception e) {
            logger.error("Error recording validation error metrics: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void recordValidationProcessingTime(String validationType, long processingTimeMs) {
        try {
            Timer.builder("validation.processing.time")
                .description("Time taken to process validations")
                .tag("component", "owner-validation")
                .tag("validation_type", validationType)
                .register(meterRegistry)
                .record(Duration.ofMillis(processingTimeMs));
            
            // Update in-memory processing time tracking
            processingTimes.computeIfAbsent(validationType, k -> new DoubleAdder()).add(processingTimeMs);
            processingCounts.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
            
            logger.debug("Recorded validation processing time: type={}, time={}ms", validationType, processingTimeMs);
            
        } catch (Exception e) {
            logger.error("Error recording validation processing time metrics: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public double getValidationSuccessRate(String validationType) {
        try {
            AtomicLong attempts = validationAttempts.get(validationType);
            AtomicLong successes = validationSuccesses.get(validationType);
            
            if (attempts == null || attempts.get() == 0) {
                return 0.0;
            }
            
            long totalAttempts = attempts.get();
            long totalSuccesses = successes != null ? successes.get() : 0;
            
            double successRate = (double) totalSuccesses / totalAttempts * 100.0;
            
            logger.debug("Calculated success rate for {}: {:.2f}% ({}/{} attempts)", 
                        validationType, successRate, totalSuccesses, totalAttempts);
            
            return successRate;
            
        } catch (Exception e) {
            logger.error("Error calculating validation success rate: {}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    @Override
    public long getTotalValidationAttempts(String validationType) {
        try {
            AtomicLong attempts = validationAttempts.get(validationType);
            long total = attempts != null ? attempts.get() : 0;
            
            logger.debug("Total validation attempts for {}: {}", validationType, total);
            return total;
            
        } catch (Exception e) {
            logger.error("Error getting total validation attempts: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public double getAverageProcessingTime(String validationType) {
        try {
            DoubleAdder totalTime = processingTimes.get(validationType);
            AtomicLong count = processingCounts.get(validationType);
            
            if (totalTime == null || count == null || count.get() == 0) {
                return 0.0;
            }
            
            double average = totalTime.sum() / count.get();
            
            logger.debug("Average processing time for {}: {:.2f}ms", validationType, average);
            return average;
            
        } catch (Exception e) {
            logger.error("Error calculating average processing time: {}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Get metrics summary for debugging and monitoring
     */
    public String getMetricsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Validation Metrics Summary:\n");
        
        for (String validationType : validationAttempts.keySet()) {
            long attempts = getTotalValidationAttempts(validationType);
            double successRate = getValidationSuccessRate(validationType);
            double avgTime = getAverageProcessingTime(validationType);
            
            summary.append(String.format("  %s: %d attempts, %.2f%% success rate, %.2fms avg time\n",
                validationType, attempts, successRate, avgTime));
        }
        
        return summary.toString();
    }
}