package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.PetClinicException;
import com.petclinic.backend.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementation of EncryptionService for data encryption and decryption
 * Validates: Requirements 9.5
 */
@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String HASH_ALGORITHM = "SHA-256";
    
    @Value("${app.encryption.key:petclinic-encryption-key-32-chars}")
    private String encryptionKey;
    
    private SecretKey getSecretKey() {
        // Ensure key is exactly 32 characters for AES-256
        String key = encryptionKey;
        if (key.length() < 32) {
            key = key + "0".repeat(32 - key.length());
        } else if (key.length() > 32) {
            key = key.substring(0, 32);
        }
        
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            
            logger.debug("Data encrypted successfully");
            return encrypted;
        } catch (Exception e) {
            logger.error("Failed to encrypt data: {}", e.getMessage());
            throw new PetClinicException("ENCRYPTION_FAILED", "Failed to encrypt data", 
                                       HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            logger.debug("Data decrypted successfully");
            return decrypted;
        } catch (Exception e) {
            logger.error("Failed to decrypt data: {}", e.getMessage());
            throw new PetClinicException("DECRYPTION_FAILED", "Failed to decrypt data", 
                                       HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public String hash(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            
            // Add salt for better security
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            digest.update(salt);
            byte[] hashedBytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedBytes.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedBytes, 0, combined, salt.length, hashedBytes.length);
            
            String hashed = Base64.getEncoder().encodeToString(combined);
            
            logger.debug("Data hashed successfully");
            return hashed;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to hash data: {}", e.getMessage());
            throw new PetClinicException("HASHING_FAILED", "Failed to hash data", 
                                       HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public boolean verifyHash(String plainText, String hashedText) {
        if (plainText == null || hashedText == null) {
            return false;
        }
        
        try {
            byte[] combined = Base64.getDecoder().decode(hashedText);
            
            // Extract salt (first 16 bytes)
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, 16);
            
            // Extract hash (remaining bytes)
            byte[] originalHash = new byte[combined.length - 16];
            System.arraycopy(combined, 16, originalHash, 0, originalHash.length);
            
            // Hash the plain text with the same salt
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            byte[] newHash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Compare hashes
            boolean matches = MessageDigest.isEqual(originalHash, newHash);
            
            logger.debug("Hash verification completed: {}", matches);
            return matches;
        } catch (Exception e) {
            logger.error("Failed to verify hash: {}", e.getMessage());
            return false;
        }
    }
}