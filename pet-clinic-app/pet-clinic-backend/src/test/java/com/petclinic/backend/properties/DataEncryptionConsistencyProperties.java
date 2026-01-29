package com.petclinic.backend.properties;

import com.petclinic.backend.service.EncryptionService;
import com.petclinic.backend.service.impl.EncryptionServiceImpl;
import net.java.quickcheck.Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for data encryption consistency
 * **Property 26: Data Encryption Consistency**
 * **Validates: Requirements 9.5**
 * 
 * For any sensitive data, encryption should be applied consistently at rest and in transit
 */
public class DataEncryptionConsistencyProperties extends PropertyTestBase {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Initialize the random field from PropertyTestBase
        encryptionService = new EncryptionServiceImpl();
        // Set test encryption key
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "test-encryption-key-32-characters");
    }

    /**
     * Generator for sensitive text data
     */
    protected Generator<String> sensitiveTextData() {
        return () -> {
            StringBuilder text = new StringBuilder();
            int length = 10 + random.nextInt(100); // 10-110 characters
            
            // Include various characters that might be in sensitive data
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!@#$%^&*()-_=+[]{}|;:'\",.<>?/";
            
            for (int i = 0; i < length; i++) {
                text.append(chars.charAt(random.nextInt(chars.length())));
            }
            
            return text.toString();
        };
    }

    /**
     * Property: Encryption service should consistently encrypt and decrypt data
     */
    @Test
    public void encryptionServiceShouldConsistentlyEncryptAndDecrypt() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String originalData = sensitiveTextData().next();
            
            // Property: Encryption should produce different output than input
            String encryptedData = encryptionService.encrypt(originalData);
            assertNotEquals(originalData, encryptedData, "Encrypted data should be different from original");
            
            // Property: Decryption should restore original data
            String decryptedData = encryptionService.decrypt(encryptedData);
            assertEquals(originalData, decryptedData, "Decrypted data should match original");
            
            // Property: Multiple encryptions of same data should produce same result
            String encryptedData2 = encryptionService.encrypt(originalData);
            assertEquals(encryptedData, encryptedData2, "Multiple encryptions should produce consistent results");
        });
    }

    /**
     * Property: Hash service should consistently hash and verify data
     */
    @Test
    public void hashServiceShouldConsistentlyHashAndVerify() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String originalData = sensitiveTextData().next();
            
            // Property: Hash should produce different output than input
            String hashedData = encryptionService.hash(originalData);
            assertNotEquals(originalData, hashedData, "Hashed data should be different from original");
            
            // Property: Hash verification should work correctly
            boolean isValid = encryptionService.verifyHash(originalData, hashedData);
            assertTrue(isValid, "Hash verification should succeed for correct data");
            
            // Property: Hash verification should fail for incorrect data
            String wrongData = sensitiveTextData().next();
            if (!wrongData.equals(originalData)) {
                boolean isInvalid = encryptionService.verifyHash(wrongData, hashedData);
                assertFalse(isInvalid, "Hash verification should fail for incorrect data");
            }
        });
    }

    /**
     * Property: Null and empty values should be handled correctly
     */
    @Test
    public void nullAndEmptyValuesShouldBeHandledCorrectly() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Property: Null values should remain null
            String encryptedNull = encryptionService.encrypt(null);
            assertNull(encryptedNull, "Encrypted null should remain null");
            
            String decryptedNull = encryptionService.decrypt(null);
            assertNull(decryptedNull, "Decrypted null should remain null");
            
            // Property: Empty strings should remain empty
            String encryptedEmpty = encryptionService.encrypt("");
            assertEquals("", encryptedEmpty, "Encrypted empty string should remain empty");
            
            String decryptedEmpty = encryptionService.decrypt("");
            assertEquals("", decryptedEmpty, "Decrypted empty string should remain empty");
            
            // Property: Hash of null should remain null
            String hashedNull = encryptionService.hash(null);
            assertNull(hashedNull, "Hashed null should remain null");
            
            // Property: Hash verification with null should return false
            boolean verifyNull = encryptionService.verifyHash("test", null);
            assertFalse(verifyNull, "Hash verification with null hash should return false");
            
            boolean verifyNullPlain = encryptionService.verifyHash(null, "hash");
            assertFalse(verifyNullPlain, "Hash verification with null plain text should return false");
        });
    }

    /**
     * Property: Encryption should be deterministic for same input
     */
    @Test
    public void encryptionShouldBeDeterministicForSameInput() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String originalData = sensitiveTextData().next();
            
            // Property: Multiple encryptions of same data should produce same result
            String encrypted1 = encryptionService.encrypt(originalData);
            String encrypted2 = encryptionService.encrypt(originalData);
            assertEquals(encrypted1, encrypted2, "Encryption should be deterministic");
            
            // Property: Both encrypted values should decrypt to same original
            String decrypted1 = encryptionService.decrypt(encrypted1);
            String decrypted2 = encryptionService.decrypt(encrypted2);
            assertEquals(originalData, decrypted1, "First decryption should match original");
            assertEquals(originalData, decrypted2, "Second decryption should match original");
            assertEquals(decrypted1, decrypted2, "Both decryptions should be identical");
        });
    }

    /**
     * Property: Encryption should handle various character sets correctly
     */
    @Test
    public void encryptionShouldHandleVariousCharacterSets() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            // Test with different character sets
            String[] testStrings = {
                "Simple ASCII text",
                "Text with numbers 123456",
                "Special chars: !@#$%^&*()",
                "Unicode: αβγδε ñáéíóú",
                "Mixed: Hello123!@# αβγ",
                "Whitespace and\ttabs\nand\rcarriage returns"
            };
            
            String testString = testStrings[random.nextInt(testStrings.length)];
            
            // Property: All character sets should encrypt and decrypt correctly
            String encrypted = encryptionService.encrypt(testString);
            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals(testString, decrypted, "All character sets should encrypt/decrypt correctly");
        });
    }

    /**
     * Property: Hash should be non-reversible
     */
    @Test
    public void hashShouldBeNonReversible() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String originalData = sensitiveTextData().next();
            String hashedData = encryptionService.hash(originalData);
            
            // Property: Hash should be different from original
            assertNotEquals(originalData, hashedData, "Hash should be different from original");
            
            // Property: Hash should be longer than typical input (due to salt and encoding)
            assertTrue(hashedData.length() > 20, "Hash should be reasonably long due to salt and encoding");
            
            // Property: Same input should produce different hashes (due to salt)
            String hashedData2 = encryptionService.hash(originalData);
            assertNotEquals(hashedData, hashedData2, "Same input should produce different hashes due to salt");
            
            // Property: Both hashes should verify correctly
            assertTrue(encryptionService.verifyHash(originalData, hashedData), "First hash should verify");
            assertTrue(encryptionService.verifyHash(originalData, hashedData2), "Second hash should verify");
        });
    }
}