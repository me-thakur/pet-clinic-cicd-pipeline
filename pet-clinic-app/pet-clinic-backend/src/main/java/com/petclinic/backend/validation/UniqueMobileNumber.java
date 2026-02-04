package com.petclinic.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for mobile number uniqueness validation
 * Ensures mobile numbers are unique within the clinic system
 * Validates: Requirements 2.2, 2.3, 2.4
 */
@Documented
@Constraint(validatedBy = UniqueMobileNumberValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueMobileNumber {
    
    /**
     * Default validation error message
     */
    String message() default "Mobile number already exists in the system";
    
    /**
     * Validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for validation metadata
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow null values (default: true)
     */
    boolean allowNull() default true;
    
    /**
     * Field name that contains the owner ID for update scenarios
     * When specified, the validator will exclude the owner with this ID from uniqueness check
     * This allows owners to keep their existing mobile number during updates
     */
    String ownerIdField() default "";
}