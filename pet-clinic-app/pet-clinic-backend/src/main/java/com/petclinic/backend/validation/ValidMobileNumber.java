package com.petclinic.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for mobile number format validation
 * Validates mobile numbers against international E.164 standard
 * Validates: Requirements 2.1, 2.5
 */
@Documented
@Constraint(validatedBy = ValidMobileNumberValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMobileNumber {
    
    /**
     * Default validation error message
     */
    String message() default "Mobile number must be in valid international format";
    
    /**
     * Validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for validation metadata
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Country code for region-specific validation (optional)
     * If not specified, validates against global E.164 format
     */
    String countryCode() default "";
    
    /**
     * Whether to allow null values (default: true)
     */
    boolean allowNull() default true;
}