package com.petclinic.backend.config;

import com.petclinic.backend.interceptor.SecurityValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web security configuration for registering security interceptors
 * 
 * Validates: Requirements 17.3, 17.4
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {
    
    @Autowired
    private SecurityValidationInterceptor securityValidationInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityValidationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/test/**");
    }
}