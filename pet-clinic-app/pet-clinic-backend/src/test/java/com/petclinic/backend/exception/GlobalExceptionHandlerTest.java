package com.petclinic.backend.exception;

import com.petclinic.backend.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for GlobalExceptionHandler
 * Tests comprehensive error handling with standardized API responses
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    
    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private MethodArgumentNotValidException validationException;
    
    @Mock
    private BindingResult bindingResult;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
    }
    
    @Test
    void handleValidationExceptions_ShouldReturnBadRequest() {
        // Given
        org.springframework.validation.FieldError fieldError = 
            new org.springframework.validation.FieldError("owner", "firstName", "First name is required");
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleValidationExceptions(validationException, mockRequest);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FIELD_VALIDATION_FAILED", response.getBody().getErrorCode());
        assertNotNull(response.getBody().getFieldErrors());
    }
    
    @Test
    void handleConstraintViolationException_ShouldReturnBadRequest() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getPropertyPath().toString()).thenReturn("email");
        when(violation.getMessage()).thenReturn("Invalid email format");
        violations.add(violation);
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleConstraintViolationException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONSTRAINT_VIOLATION", response.getBody().getErrorCode());
    }
    
    @Test
    void handleEntityNotFoundException_ShouldReturnNotFound() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Pet not found");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleEntityNotFoundException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ENTITY_NOT_FOUND", response.getBody().getErrorCode());
    }
    
    @Test
    void handleBadCredentialsException_ShouldReturnUnauthorized() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleBadCredentialsException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHENTICATION_FAILED", response.getBody().getErrorCode());
    }
    
    @Test
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleAccessDeniedException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().getErrorCode());
    }
    
    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleIllegalArgumentException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getErrorCode());
    }
    
    @Test
    void handleIllegalStateException_ShouldReturnConflict() {
        // Given
        IllegalStateException exception = new IllegalStateException("Invalid state");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleIllegalStateException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_OPERATION_STATE", response.getBody().getErrorCode());
    }
    
    @Test
    void handleRuntimeException_ShouldReturnInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Runtime error");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleRuntimeException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RUNTIME_ERROR", response.getBody().getErrorCode());
    }
    
    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new Exception("Generic error");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleGenericException(exception, mockRequest);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
    }
    
    @Test
    void errorResponse_ShouldHaveCorrectStructure() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Test error");
        
        // When
        ResponseEntity<ErrorResponse> response = 
            exceptionHandler.handleIllegalArgumentException(exception, mockRequest);
        
        // Then
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getMessage());
        assertNotNull(body.getErrorCode());
        assertNotNull(body.getTimestamp());
        assertNotNull(body.getPath());
    }
}