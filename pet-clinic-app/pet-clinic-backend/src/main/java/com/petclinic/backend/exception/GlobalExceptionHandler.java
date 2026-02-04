package com.petclinic.backend.exception;

import com.petclinic.backend.dto.ErrorResponse;
import com.petclinic.backend.dto.FieldError;
import com.petclinic.backend.dto.StandardApiResponse;
import com.petclinic.backend.service.ErrorSuggestionService;
import com.petclinic.backend.service.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for standardized error responses with specific, actionable error messages
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private ErrorSuggestionService errorSuggestionService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    /**
     * Handle custom validation exceptions with specific field-level feedback
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("VALIDATION_FAILED")
            .message("Please correct the following fields to continue:")
            .fieldErrors(ex.getFieldErrors())
            .suggestions(errorSuggestionService.generateSuggestions(ex))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle service unavailable exceptions with fallback options
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex, HttpServletRequest request) {
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("SERVICE_UNAVAILABLE")
            .message("Service temporarily unavailable: " + ex.getServiceName())
            .retryAfter(ex.getRetryAfter())
            .fallbackOptions(ex.getFallbackOptions())
            .suggestions(errorSuggestionService.generateServiceUnavailableSuggestions(ex.getServiceName()))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Service unavailable: {} - {}", ex.getServiceName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * Handle business logic exceptions with specific guidance
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(
            BusinessLogicException ex, HttpServletRequest request) {
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .suggestions(errorSuggestionService.generateSuggestions(ex.getErrorCode()))
            .path(request.getRequestURI())
            .context(ex.getContext())
            .build();
        
        logger.warn("Business logic error on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle validation errors from @Valid annotations with specific field guidance
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<FieldError> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((org.springframework.validation.FieldError) error).getRejectedValue();
            
            List<String> fieldSuggestions = errorSuggestionService.generateFieldSuggestions(
                fieldName, "VALIDATION_ERROR"
            );
            
            fieldErrors.add(FieldError.builder()
                .field(fieldName)
                .message(errorMessage)
                .rejectedValue(rejectedValue != null ? rejectedValue.toString() : null)
                .suggestions(fieldSuggestions)
                .build());
        });
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("FIELD_VALIDATION_FAILED")
            .message("Please correct the highlighted fields below:")
            .fieldErrors(fieldErrors)
            .suggestions(List.of(
                "Review each field for specific formatting requirements",
                "All required fields must be completed",
                "Check for typos and ensure data matches expected formats"
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Field validation failed for {}: {} errors", request.getRequestURI(), fieldErrors.size());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle constraint violation exceptions with specific field guidance
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        List<FieldError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                Object invalidValue = violation.getInvalidValue();
                
                List<String> fieldSuggestions = errorSuggestionService.generateFieldSuggestions(
                    fieldName, "CONSTRAINT_VIOLATION"
                );
                
                return FieldError.builder()
                    .field(fieldName)
                    .message(message)
                    .rejectedValue(invalidValue != null ? invalidValue.toString() : null)
                    .suggestions(fieldSuggestions)
                    .build();
            })
            .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("CONSTRAINT_VIOLATION")
            .message("Data constraints were not met for the following fields:")
            .fieldErrors(fieldErrors)
            .suggestions(List.of(
                "Ensure all data meets the required constraints",
                "Check for duplicate values where uniqueness is required",
                "Verify data types and formats match requirements"
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Constraint violations for {}: {} violations", request.getRequestURI(), fieldErrors.size());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle entity not found exceptions with specific guidance
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {
        
        String entityType = extractEntityTypeFromMessage(ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("ENTITY_NOT_FOUND")
            .message("The requested " + entityType + " could not be found")
            .suggestions(List.of(
                "Verify the " + entityType + " ID is correct",
                "The " + entityType + " may have been deleted by another user",
                "Try refreshing the page to see updated data",
                "Use the search function to find the correct " + entityType
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Entity not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Extract entity type from exception message for better error context
     */
    private String extractEntityTypeFromMessage(String message) {
        if (message == null) return "resource";
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("owner")) return "owner";
        if (lowerMessage.contains("pet")) return "pet";
        if (lowerMessage.contains("visit")) return "visit";
        if (lowerMessage.contains("veterinarian") || lowerMessage.contains("vet")) return "veterinarian";
        
        return "resource";
    }
    
    /**
     * Handle authentication errors with specific guidance
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("AUTHENTICATION_FAILED")
            .message("Login failed - please check your credentials")
            .suggestions(errorSuggestionService.generateAuthenticationSuggestions())
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Authentication failed for {}: Invalid credentials", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Handle authorization errors with specific guidance
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String resource = extractResourceFromPath(request.getRequestURI());
        
        // Log the access denied event for security monitoring
        securityAuditService.logAccessDenied("ACCESS_DENIED", resource, authentication, 
            "Insufficient permissions for " + request.getMethod() + " " + request.getRequestURI());
        
        // Sanitize the error message based on user's role
        String sanitizedMessage = securityAuditService.sanitizeErrorMessage(
            "You don't have permission to access this " + resource, authentication);
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("ACCESS_DENIED")
            .message(sanitizedMessage)
            .suggestions(errorSuggestionService.generateAuthorizationSuggestions(resource))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Access denied for {} on {}: Insufficient permissions", 
                   request.getRemoteUser(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * Extract resource type from request path for better error context
     */
    private String extractResourceFromPath(String path) {
        if (path == null) return "resource";
        
        String lowerPath = path.toLowerCase();
        if (lowerPath.contains("/owners")) return "owner information";
        if (lowerPath.contains("/pets")) return "pet information";
        if (lowerPath.contains("/visits")) return "visit records";
        if (lowerPath.contains("/veterinarians")) return "veterinarian information";
        if (lowerPath.contains("/reports")) return "reports";
        if (lowerPath.contains("/admin")) return "administrative function";
        
        return "resource";
    }
    
    /**
     * Handle network connectivity exceptions
     */
    @ExceptionHandler({ConnectException.class, SocketTimeoutException.class})
    public ResponseEntity<ErrorResponse> handleNetworkException(
            Exception ex, HttpServletRequest request) {
        
        String errorCode = ex instanceof ConnectException ? "CONNECTION_FAILED" : "REQUEST_TIMEOUT";
        String message = ex instanceof ConnectException ? 
            "Unable to connect to the service" : 
            "Request timed out - the service is taking too long to respond";
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .suggestions(errorSuggestionService.generateNetworkErrorSuggestions())
            .retryAfter("30 seconds")
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Network error on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * Handle illegal argument exceptions with specific guidance
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("INVALID_ARGUMENT")
            .message("Invalid data provided: " + ex.getMessage())
            .suggestions(List.of(
                "Check that all required parameters are provided",
                "Verify data types and formats are correct",
                "Ensure numeric values are within valid ranges",
                "Check that dates are in the correct format"
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Invalid argument on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle illegal state exceptions with specific guidance
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("INVALID_OPERATION_STATE")
            .message("Operation cannot be performed in current state: " + ex.getMessage())
            .suggestions(List.of(
                "Ensure all prerequisites are met before performing this operation",
                "Check that the resource is in the correct state",
                "Try refreshing the page to get the latest data",
                "Contact support if the problem persists"
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.warn("Invalid state on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * Handle runtime exceptions with recovery guidance
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Sanitize the error message to prevent information leakage
        String sanitizedMessage = securityAuditService.sanitizeErrorMessage(ex.getMessage(), authentication);
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("RUNTIME_ERROR")
            .message("An unexpected error occurred while processing your request")
            .suggestions(List.of(
                "Please try your request again",
                "If the problem persists, try refreshing the page",
                "Contact support if you continue to experience issues",
                "Include the timestamp and error details when contacting support"
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.error("Runtime error on {}: {}", request.getRequestURI(), sanitizedMessage, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handle all other exceptions with general recovery guidance
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Sanitize the error message to prevent information leakage
        String sanitizedMessage = securityAuditService.sanitizeErrorMessage(ex.getMessage(), authentication);
        
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("INTERNAL_ERROR")
            .message("An internal server error occurred")
            .suggestions(List.of(
                "This is a temporary issue - please try again",
                "If the problem continues, contact technical support",
                "Include the error timestamp when reporting the issue",
                "Try using a different browser if the problem persists"
            ))
            .path(request.getRequestURI())
            .build();
        
        logger.error("Unexpected error on {}: {}", request.getRequestURI(), sanitizedMessage, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}