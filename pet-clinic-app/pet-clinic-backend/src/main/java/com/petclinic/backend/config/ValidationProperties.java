package com.petclinic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for validation settings
 * Allows customization of validation behavior through application properties
 * Validates: Requirements 1.1, 2.1, 4.1
 */
@Configuration
@ConfigurationProperties(prefix = "petclinic.validation")
public class ValidationProperties {

    /**
     * Mobile number validation settings
     */
    private MobileNumber mobileNumber = new MobileNumber();

    /**
     * Email validation settings
     */
    private Email email = new Email();

    /**
     * Name validation settings
     */
    private Name name = new Name();

    /**
     * General validation settings
     */
    private General general = new General();

    // Getters and Setters
    public MobileNumber getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(MobileNumber mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public General getGeneral() {
        return general;
    }

    public void setGeneral(General general) {
        this.general = general;
    }

    /**
     * Mobile number validation configuration
     */
    public static class MobileNumber {
        private boolean required = false;
        private boolean enforceUniqueness = true;
        private String defaultCountryCode = "US";
        private boolean allowInternational = true;

        // Getters and Setters
        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public boolean isEnforceUniqueness() {
            return enforceUniqueness;
        }

        public void setEnforceUniqueness(boolean enforceUniqueness) {
            this.enforceUniqueness = enforceUniqueness;
        }

        public String getDefaultCountryCode() {
            return defaultCountryCode;
        }

        public void setDefaultCountryCode(String defaultCountryCode) {
            this.defaultCountryCode = defaultCountryCode;
        }

        public boolean isAllowInternational() {
            return allowInternational;
        }

        public void setAllowInternational(boolean allowInternational) {
            this.allowInternational = allowInternational;
        }
    }

    /**
     * Email validation configuration
     */
    public static class Email {
        private boolean required = false;
        private boolean enforceUniqueness = true;
        private int maxLength = 100;

        // Getters and Setters
        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public boolean isEnforceUniqueness() {
            return enforceUniqueness;
        }

        public void setEnforceUniqueness(boolean enforceUniqueness) {
            this.enforceUniqueness = enforceUniqueness;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }

    /**
     * Name validation configuration
     */
    public static class Name {
        private int maxLength = 50;
        private boolean allowSpecialCharacters = true;

        // Getters and Setters
        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public boolean isAllowSpecialCharacters() {
            return allowSpecialCharacters;
        }

        public void setAllowSpecialCharacters(boolean allowSpecialCharacters) {
            this.allowSpecialCharacters = allowSpecialCharacters;
        }
    }

    /**
     * General validation configuration
     */
    public static class General {
        private boolean failFast = false;
        private boolean enableAuditLogging = true;

        // Getters and Setters
        public boolean isFailFast() {
            return failFast;
        }

        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }

        public boolean isEnableAuditLogging() {
            return enableAuditLogging;
        }

        public void setEnableAuditLogging(boolean enableAuditLogging) {
            this.enableAuditLogging = enableAuditLogging;
        }
    }
}