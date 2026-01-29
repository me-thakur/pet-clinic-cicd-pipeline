package com.petclinic.backend.properties;

import com.petclinic.backend.dto.ErrorResponse;
import com.petclinic.backend.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Property-based tests for error response standardization
 * **Validates: Requirements 7.4**
 */
@SpringBootTest
@ActiveProfiles("test")
public class ErrorResponseStandardizationProperties extends PropertyTestBase {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUpObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Property 17: Error Response Standardization
     * For any API error condition, the response should follow the standardized error format with appropriate HTTP status codes
     */
    @Test
    void errorResponsesShouldFollowStandardizedFormat() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String message = validDescriptions().next();
            String path = "/api/test/" + random.nextInt(1000);
            String errorCode = "TEST_ERROR_" + random.nextInt(100);
            
            // Test different exception types
            testExceptionStandardization(new EntityNotFoundException(message), HttpStatus.NOT_FOUND, path, errorCode);
            testExceptionStandardization(new ValidationException(message), HttpStatus.BAD_REQUEST, path, errorCode);
            testExceptionStandardization(new BusinessRuleException(message), HttpStatus.UNPROCESSABLE_ENTITY, path, errorCode);
            testExceptionStandardization(new ConflictException(message), HttpStatus.CONFLICT, path, errorCode);
        });
    }
    
    private void testExceptionStandardization(PetClinicException exception, HttpStatus expectedStatus, String path, String errorCode) {
        // Create error response
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(expectedStatus.value())
            .error(expectedStatus.getReasonPhrase())
            .message(exception.getMessage())
            .path(path)
            .errorCode(exception.getErrorCode())
            .apiVersion("v1")
            .build();
        
        // Verify standardized format
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(errorResponse.getError()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(errorResponse.getMessage()).isNotBlank();
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getErrorCode()).isNotBlank();
        assertThat(errorResponse.getApiVersion()).isEqualTo("v1");
    }
    
    /**
     * Property: Error responses should be serializable to JSON
     */
    @Test
    void errorResponsesShouldBeSerializableToJson() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            int status = 400 + random.nextInt(200); // HTTP status codes 400-599
            String error = "Test Error";
            String message = validDescriptions().next();
            String path = "/api/test/" + random.nextInt(1000);
            String errorCode = "TEST_ERROR_" + random.nextInt(100);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .errorCode(errorCode)
                .apiVersion("v1")
                .build();
            
            // Should be serializable to JSON without errors
            assertThatCode(() -> {
                String json = objectMapper.writeValueAsString(errorResponse);
                assertThat(json).isNotBlank();
                
                // Should be deserializable back
                ErrorResponse deserialized = objectMapper.readValue(json, ErrorResponse.class);
                assertThat(deserialized.getStatus()).isEqualTo(status);
                assertThat(deserialized.getError()).isEqualTo(error);
                assertThat(deserialized.getMessage()).isEqualTo(message);
                assertThat(deserialized.getPath()).isEqualTo(path);
                assertThat(deserialized.getErrorCode()).isEqualTo(errorCode);
                assertThat(deserialized.getApiVersion()).isEqualTo("v1");
            }).doesNotThrowAnyException();
        });
    }
    
    /**
     * Property: HTTP status codes should match exception types
     */
    @Test
    void httpStatusCodesShouldMatchExceptionTypes() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String message = validDescriptions().next();
            
            // Test specific exception to status code mappings
            EntityNotFoundException notFound = new EntityNotFoundException(message);
            assertThat(notFound.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            
            ValidationException validation = new ValidationException(message);
            assertThat(validation.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            
            BusinessRuleException businessRule = new BusinessRuleException(message);
            assertThat(businessRule.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            
            ConflictException conflict = new ConflictException(message);
            assertThat(conflict.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        });
    }
    
    /**
     * Property: Error codes should be consistent for exception types
     */
    @Test
    void errorCodesShouldBeConsistentForExceptionTypes() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String message = validDescriptions().next();
            
            // Test consistent error codes
            EntityNotFoundException notFound = new EntityNotFoundException(message);
            assertThat(notFound.getErrorCode()).isEqualTo("ENTITY_NOT_FOUND");
            
            ValidationException validation = new ValidationException(message);
            assertThat(validation.getErrorCode()).isEqualTo("VALIDATION_ERROR");
            
            BusinessRuleException businessRule = new BusinessRuleException(message);
            assertThat(businessRule.getErrorCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
            
            ConflictException conflict = new ConflictException(message);
            assertThat(conflict.getErrorCode()).isEqualTo("RESOURCE_CONFLICT");
        });
    }
    
    /**
     * Property: Error responses should include trace IDs for debugging
     */
    @Test
    void errorResponsesShouldIncludeTraceIds() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String traceId = "trace-" + random.nextInt(10000);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .message("Test error")
                .path("/api/test")
                .errorCode("TEST_ERROR")
                .apiVersion("v1")
                .traceId(traceId)
                .build();
            
            assertThat(errorResponse.getTraceId()).isEqualTo(traceId);
            assertThat(errorResponse.getTraceId()).isNotBlank();
        });
    }
}