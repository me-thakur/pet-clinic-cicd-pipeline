package com.petclinic.backend.service;

/**
 * Service interface for data encryption and decryption operations
 * Validates: Requirements 9.5
 */
public interface EncryptionService {
    
    /**
     * Encrypt sensitive data
     * @param plainText the data to encrypt
     * @return encrypted data as Base64 string
     */
    String encrypt(String plainText);
    
    /**
     * Decrypt sensitive data
     * @param encryptedText the encrypted data as Base64 string
     * @return decrypted plain text
     */
    String decrypt(String encryptedText);
    
    /**
     * Hash sensitive data (one-way)
     * @param plainText the data to hash
     * @return hashed data as hex string
     */
    String hash(String plainText);
    
    /**
     * Verify hashed data
     * @param plainText the plain text to verify
     * @param hashedText the hashed text to compare against
     * @return true if the plain text matches the hash
     */
    boolean verifyHash(String plainText, String hashedText);
}