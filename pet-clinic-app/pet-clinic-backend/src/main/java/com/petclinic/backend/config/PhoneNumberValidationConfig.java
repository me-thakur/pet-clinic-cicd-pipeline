package com.petclinic.backend.config;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Google libphonenumber library
 * Provides PhoneNumberUtil bean for mobile number validation
 * Validates: Requirements 2.1, 2.5
 */
@Configuration
public class PhoneNumberValidationConfig {

    /**
     * Creates a PhoneNumberUtil instance for mobile number validation
     * This utility provides comprehensive phone number parsing and validation
     * according to international standards (E.164)
     * 
     * @return PhoneNumberUtil instance
     */
    @Bean
    public PhoneNumberUtil phoneNumberUtil() {
        return PhoneNumberUtil.getInstance();
    }
}