package com.petclinic.backend.config;

import com.petclinic.backend.interceptor.PerformanceInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for performance monitoring and optimization
 * Sets up interceptors, metrics, caching, and performance-related beans
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableCaching
public class PerformanceConfig implements WebMvcConfigurer {
    
    private final PerformanceInterceptor performanceInterceptor;
    
    @Autowired
    public PerformanceConfig(PerformanceInterceptor performanceInterceptor) {
        this.performanceInterceptor = performanceInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register performance interceptor for all requests
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/actuator/**",     // Exclude actuator endpoints
                    "/swagger-ui/**",   // Exclude Swagger UI
                    "/api-docs/**",     // Exclude API docs
                    "/webjars/**",      // Exclude static resources
                    "/css/**",          // Exclude CSS files
                    "/js/**",           // Exclude JS files
                    "/images/**",       // Exclude images
                    "/favicon.ico"      // Exclude favicon
                );
    }
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", "pet-clinic-backend",
            "version", "1.0.0"
        );
    }
}