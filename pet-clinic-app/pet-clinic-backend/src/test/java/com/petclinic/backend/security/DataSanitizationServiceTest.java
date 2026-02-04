package com.petclinic.backend.security;

import com.petclinic.backend.service.DataSanitizationService;
import com.petclinic.backend.service.impl.DataSanitizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DataSanitizationService
 * Validates data sanitization and security validation functionality
 */
public class DataSanitizationServiceTest {
    
    private DataSanitizationService dataSanitizationService;
    
    @BeforeEach
    void setUp() {
        dataSanitizationService = new DataSanitizationServiceImpl();
    }
    
    @Test
    void testSanitizeString_WithNormalText_ReturnsCleanText() {
        // Arrange
        String input = "Hello World";
        
        // Act
        String result = dataSanitizationService.sanitizeString(input);
        
        // Assert
        assertEquals("Hello World", result);
    }
    
    @Test
    void testSanitizeString_WithScriptTag_RemovesScript() {
        // Arrange
        String input = "Hello <script>alert('xss')</script> World";
        
        // Act
        String result = dataSanitizationService.sanitizeString(input);
        
        // Assert
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("alert"));
    }
    
    @Test
    void testContainsMaliciousContent_WithScriptTag_ReturnsTrue() {
        // Arrange
        String input = "<script>alert('xss')</script>";
        
        // Act
        boolean result = dataSanitizationService.containsMaliciousContent(input);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void testContainsMaliciousContent_WithNormalText_ReturnsFalse() {
        // Arrange
        String input = "This is normal text";
        
        // Act
        boolean result = dataSanitizationService.containsMaliciousContent(input);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testSanitizeEmail_WithValidEmail_ReturnsEmail() {
        // Arrange
        String input = "test@example.com";
        
        // Act
        String result = dataSanitizationService.sanitizeEmail(input);
        
        // Assert
        assertEquals("test@example.com", result);
    }
    
    @Test
    void testSanitizeEmail_WithInvalidEmail_ReturnsNull() {
        // Arrange
        String input = "invalid-email";
        
        // Act
        String result = dataSanitizationService.sanitizeEmail(input);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testSanitizePhoneNumber_WithValidPhone_ReturnsCleanPhone() {
        // Arrange
        String input = "+1 (555) 123-4567";
        
        // Act
        String result = dataSanitizationService.sanitizePhoneNumber(input);
        
        // Assert
        assertEquals("+15551234567", result);
    }
    
    @Test
    void testSanitizePhoneNumber_WithInvalidPhone_ReturnsNull() {
        // Arrange
        String input = "abc123";
        
        // Act
        String result = dataSanitizationService.sanitizePhoneNumber(input);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testSanitizeFileName_WithDangerousPath_RemovesTraversal() {
        // Arrange
        String input = "../../../etc/passwd";
        
        // Act
        String result = dataSanitizationService.sanitizeFileName(input);
        
        // Assert
        assertFalse(result.contains("../"));
        assertEquals("etcpasswd", result);
    }
    
    @Test
    void testSanitizeFileName_WithReservedName_AddsPrefix() {
        // Arrange
        String input = "CON.txt";
        
        // Act
        String result = dataSanitizationService.sanitizeFileName(input);
        
        // Assert
        assertTrue(result.startsWith("_"));
    }
    
    @Test
    void testSanitizeNumericInput_WithValidNumber_ReturnsNumber() {
        // Arrange
        String input = "123";
        
        // Act
        Long result = dataSanitizationService.sanitizeNumericInput(input, 0L, 1000L);
        
        // Assert
        assertEquals(Long.valueOf(123), result);
    }
    
    @Test
    void testSanitizeNumericInput_WithOutOfRangeNumber_ReturnsNull() {
        // Arrange
        String input = "2000";
        
        // Act
        Long result = dataSanitizationService.sanitizeNumericInput(input, 0L, 1000L);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testSanitizeParameters_WithMaliciousContent_SanitizesValues() {
        // Arrange
        Map<String, Object> input = new HashMap<>();
        input.put("name", "John <script>alert('xss')</script>");
        input.put("age", "25");
        
        // Act
        Map<String, Object> result = dataSanitizationService.sanitizeParameters(input);
        
        // Assert
        assertNotNull(result);
        String sanitizedName = (String) result.get("name");
        assertFalse(sanitizedName.contains("<script>"));
    }
    
    @Test
    void testEscapeSpecialCharacters_WithHtmlChars_EscapesCorrectly() {
        // Arrange
        String input = "<div>Hello & \"World\"</div>";
        
        // Act
        String result = dataSanitizationService.escapeSpecialCharacters(input);
        
        // Assert
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&amp;"));
        assertTrue(result.contains("&quot;"));
    }
    
    @Test
    void testSanitizeHtml_WithDangerousHtml_RemovesDangerousElements() {
        // Arrange
        String input = "<p>Safe content</p><script>alert('xss')</script><iframe src='evil.com'></iframe>";
        
        // Act
        String result = dataSanitizationService.sanitizeHtml(input);
        
        // Assert
        assertTrue(result.contains("<p>Safe content</p>"));
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("<iframe>"));
    }
    
    @Test
    void testSanitizeSql_WithSqlInjection_ReturnsNull() {
        // Arrange
        String input = "'; DROP TABLE users; --";
        
        // Act
        String result = dataSanitizationService.sanitizeSql(input);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testSanitizeSql_WithSafeInput_EscapesQuotes() {
        // Arrange
        String input = "John's Pet";
        
        // Act
        String result = dataSanitizationService.sanitizeSql(input);
        
        // Assert
        assertEquals("John''s Pet", result);
    }
}