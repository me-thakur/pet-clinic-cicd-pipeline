package com.petclinic.backend.exception;

import com.petclinic.backend.dto.ErrorResponse;
import com.petclinic.backend.dto.FieldError;
import com.petclinic.backend.service.ErrorSuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for enhanced GlobalExceptionHandler with specific error messages
 */
@ExtendWith(MockitoExtension.class)
class EnhancedGlobalExceptionHandlerTest {

    @Mock
    private ErrorSuggestionService errorSuggestionService;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/owners");
    }

    @Test
    void handleValidationException_ShouldReturnSpecificErrorResponse() {
        // Given
        List<FieldError> fieldErrors = List.of(
            FieldError.builder()
                .field("email")
                .message("Email is required")
                .build()
        );
        ValidationException exception = new ValidationException("Validation failed", fieldErrors);
        
        List<String> suggestions = List.of("Please provide a valid email address");
        when(errorSuggestionService.generateSuggestions(any(ValidationException.class)))
            .thenReturn(suggestions);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationException(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("VALIDATION_FAILED", errorResponse.getErrorCode());
        assertEquals("Please correct the following fields to continue:", errorResponse.getMessage());
        assertEquals(fieldErrors, errorResponse.getFieldErrors());
        assertEquals(suggestions, errorResponse.getSuggestions());
        assertEquals("/api/owners", errorResponse.getPath());
    }

    @Test
    void handleServiceUnavailableException_ShouldReturnServiceSpecificError() {
        // Given
        ServiceUnavailableException exception = new ServiceUnavailableException(
            "ValidationService", 
            "Service is temporarily down",
            "60 seconds"
        );
        
        List<String> suggestions = List.of("Try again in a few minutes");
        when(errorSuggestionService.generateServiceUnavailableSuggestions("ValidationService"))
            .thenReturn(suggestions);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleServiceUnavailableException(exception, request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("SERVICE_UNAVAILABLE", errorResponse.getErrorCode());
        assertEquals("Service temporarily unavailable: ValidationService", errorResponse.getMessage());
        assertEquals("60 seconds", errorResponse.getRetryAfter());
        assertEquals(suggestions, errorResponse.getSuggestions());
    }

    @Test
    void handleEntityNotFoundException_ShouldReturnSpecificEntityError() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Owner with ID 123 not found");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleEntityNotFoundException(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("ENTITY_NOT_FOUND", errorResponse.getErrorCode());
        assertEquals("The requested owner could not be found", errorResponse.getMessage());
        assertNotNull(errorResponse.getSuggestions());
        assertTrue(errorResponse.getSuggestions().contains("Verify the owner ID is correct"));
    }

    @Test
    void handleBadCredentialsException_ShouldReturnAuthenticationGuidance() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");
        
        List<String> suggestions = List.of("Check your username and password");
        when(errorSuggestionService.generateAuthenticationSuggestions())
            .thenReturn(suggestions);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleBadCredentialsException(exception, request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("AUTHENTICATION_FAILED", errorResponse.getErrorCode());
        assertEquals("Login failed - please check your credentials", errorResponse.getMessage());
        assertEquals(suggestions, errorResponse.getSuggestions());
    }

    @Test
    void handleAccessDeniedException_ShouldReturnResourceSpecificError() {
        // Given
        request.setRequestURI("/api/reports/export");
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        List<String> suggestions = List.of("Contact administrator for access");
        when(errorSuggestionService.generateAuthorizationSuggestions("reports"))
            .thenReturn(suggestions);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAccessDeniedException(exception, request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("ACCESS_DENIED", errorResponse.getErrorCode());
        assertEquals("You don't have permission to access this reports", errorResponse.getMessage());
        assertEquals(suggestions, errorResponse.getSuggestions());
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnSpecificGuidance() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid pet age: -5");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleIllegalArgumentException(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
        assertEquals("Invalid data provided: Invalid pet age: -5", errorResponse.getMessage());
        assertNotNull(errorResponse.getSuggestions());
        assertTrue(errorResponse.getSuggestions().contains("Ensure numeric values are within valid ranges"));
    }

    @Test
    void handleRuntimeException_ShouldReturnRecoveryGuidance() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleRuntimeException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("RUNTIME_ERROR", errorResponse.getErrorCode());
        assertEquals("An unexpected error occurred while processing your request", errorResponse.getMessage());
        assertNotNull(errorResponse.getSuggestions());
        assertTrue(errorResponse.getSuggestions().contains("Please try your request again"));
    }

    @Test
    void extractEntityTypeFromMessage_ShouldIdentifyCorrectEntityType() {
        // Test owner entity detection
        EntityNotFoundException ownerException = new EntityNotFoundException("Owner with ID 123 not found");
        ResponseEntity<ErrorResponse> ownerResponse = globalExceptionHandler
            .handleEntityNotFoundException(ownerException, request);
        assertTrue(ownerResponse.getBody().getMessage().contains("owner"));

        // Test pet entity detection
        EntityNotFoundException petException = new EntityNotFoundException("Pet not found");
        ResponseEntity<ErrorResponse> petResponse = globalExceptionHandler
            .handleEntityNotFoundException(petException, request);
        assertTrue(petResponse.getBody().getMessage().contains("pet"));

        // Test veterinarian entity detection
        EntityNotFoundException vetException = new EntityNotFoundException("Veterinarian not available");
        ResponseEntity<ErrorResponse> vetResponse = globalExceptionHandler
            .handleEntityNotFoundException(vetException, request);
        assertTrue(vetResponse.getBody().getMessage().contains("veterinarian"));
    }

    @Test
    void extractResourceFromPath_ShouldIdentifyCorrectResource() {
        // Test reports resource
        request.setRequestURI("/api/reports/export");
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        when(errorSuggestionService.generateAuthorizationSuggestions(anyString()))
            .thenReturn(List.of("Contact administrator"));

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAccessDeniedException(exception, request);
        
        assertTrue(response.getBody().getMessage().contains("reports"));
    }
}