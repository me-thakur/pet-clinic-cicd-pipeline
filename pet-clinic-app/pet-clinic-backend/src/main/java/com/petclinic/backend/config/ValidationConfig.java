package com.petclinic.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import jakarta.validation.Validator;

/**
 * Configuration class for Bean Validation (JSR-303) setup
 * Configures Hibernate Validator as the validation provider
 * Validates: Requirements 1.1, 2.1, 4.1
 */
@Configuration
@EnableConfigurationProperties(ValidationProperties.class)
public class ValidationConfig {

    /**
     * Creates a LocalValidatorFactoryBean that serves as the JSR-303 Validator
     * Uses Hibernate Validator as the implementation
     * 
     * @return configured validator factory bean
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        
        // Configure validation message interpolation
        validatorFactoryBean.setValidationMessageSource(messageSource());
        
        return validatorFactoryBean;
    }

    /**
     * Creates a MethodValidationPostProcessor to enable method-level validation
     * This allows validation of method parameters and return values
     * 
     * @return configured method validation post processor
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }

    /**
     * Provides the Validator instance for programmatic validation
     * 
     * @return the configured validator
     */
    @Bean
    public Validator validatorInstance() {
        return validator();
    }

    /**
     * Creates a message source for validation error messages
     * Supports internationalization of validation messages
     * 
     * @return configured message source
     */
    @Bean
    public org.springframework.context.MessageSource messageSource() {
        org.springframework.context.support.ReloadableResourceBundleMessageSource messageSource = 
            new org.springframework.context.support.ReloadableResourceBundleMessageSource();
        
        messageSource.setBasename("classpath:messages/validation");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(300); // Cache for 5 minutes
        messageSource.setFallbackToSystemLocale(false);
        
        return messageSource;
    }
}