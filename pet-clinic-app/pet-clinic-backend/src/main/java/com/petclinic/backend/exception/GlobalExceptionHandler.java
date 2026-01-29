package com.petclinic.backend.exception;

import com.petclinic.backend.config.ApiVersionConfig;
import com.petclinic.backend.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for Pet Clinic application
 * Provides centralized exception handling with consistent error response format
 * Supports API versioning and comprehensive error logging
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle Pet Clinic specific exceptions
     */
    @ExceptionHandler(PetClinicException.class)
    public ResponseEntity<ErrorResponse> handlePetClinicException(PetClinicException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            ex.getHttpStatus().value(),
            ex.getHttpStatus().getReasonPhrase(),
            ex.getMessage(),
            getPath(request),
            ex.getErrorCode(),
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }
    
    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            getPath(request),
            ex.getErrorCode(),
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            getPath(request),
            ex.getErrorCode(),
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle business rule exceptions
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
            ex.getMessage(),
            getPath(request),
            ex.getErrorCode(),
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    /**
     * Handle conflict exceptions
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            ex.getMessage(),
            getPath(request),
            ex.getErrorCode(),
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle method argument validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        String message = "Validation failed for " + fieldErrors.size() + " field(s)";
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            getPath(request),
            "VALIDATION_ERROR",
            getApiVersion(request),
            traceId
        );
        errorResponse.setFieldErrors(fieldErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            fieldErrors.put(fieldName, errorMessage);
        }
        
        String message = "Constraint validation failed for " + fieldErrors.size() + " field(s)";
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            getPath(request),
            "CONSTRAINT_VIOLATION",
            getApiVersion(request),
            traceId
        );
        errorResponse.setFieldErrors(fieldErrors);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle data integrity violation exceptions
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        String message = "Data integrity constraint violation";
        if (ex.getMessage() != null && ex.getMessage().contains("foreign key constraint")) {
            message = "Cannot delete entity due to existing references";
        } else if (ex.getMessage() != null && ex.getMessage().contains("unique constraint")) {
            message = "Duplicate value violates uniqueness constraint";
        }
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            message,
            getPath(request),
            "DATA_INTEGRITY_VIOLATION",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle HTTP message not readable exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        String message = "Malformed JSON request";
        if (ex.getMessage() != null && ex.getMessage().contains("JSON parse error")) {
            message = "Invalid JSON format in request body";
        }
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            getPath(request),
            "MALFORMED_REQUEST",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            getPath(request),
            "INVALID_PARAMETER_TYPE",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            getPath(request),
            "MISSING_PARAMETER",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle HTTP request method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());
        List<String> details = new ArrayList<>();
        if (ex.getSupportedMethods() != null) {
            details.add("Supported methods: " + String.join(", ", ex.getSupportedMethods()));
        }
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
            message,
            getPath(request),
            "METHOD_NOT_ALLOWED",
            getApiVersion(request),
            traceId
        );
        errorResponse.setDetails(details);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "Access denied: insufficient permissions",
            getPath(request),
            "ACCESS_DENIED",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handle bad credentials exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            "Invalid credentials provided",
            getPath(request),
            "INVALID_CREDENTIALS",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String traceId = generateTraceId();
        logError(ex, traceId);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred",
            getPath(request),
            "INTERNAL_ERROR",
            getApiVersion(request),
            traceId
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Create standardized error response
     */
    private ErrorResponse createErrorResponse(int status, String error, String message, 
                                            String path, String errorCode, String apiVersion, String traceId) {
        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .errorCode(errorCode)
            .apiVersion(apiVersion)
            .traceId(traceId)
            .build();
    }
    
    /**
     * Extract path from web request
     */
    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }
    
    /**
     * Get API version from request
     */
    private String getApiVersion(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            return ApiVersionConfig.getApiVersion(httpRequest);
        }
        return ApiVersionConfig.CURRENT_API_VERSION;
    }
    
    /**
     * Generate unique trace ID for error tracking
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Log error with trace ID
     */
    private void logError(Exception ex, String traceId) {
        MDC.put("traceId", traceId);
        try {
            if (ex instanceof PetClinicException) {
                logger.warn("Pet Clinic Exception [{}]: {}", traceId, ex.getMessage());
            } else {
                logger.error("Unexpected Exception [{}]: {}", traceId, ex.getMessage(), ex);
            }
        } finally {
            MDC.remove("traceId");
        }
    }
}