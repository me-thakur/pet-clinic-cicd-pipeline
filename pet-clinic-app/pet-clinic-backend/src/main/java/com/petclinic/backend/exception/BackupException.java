package com.petclinic.backend.exception;

/**
 * Exception thrown when backup or restore operations fail.
 */
public class BackupException extends PetClinicException {
    
    public BackupException(String message) {
        super(message, "BACKUP_ERROR", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public BackupException(String message, Throwable cause) {
        super(message, "BACKUP_ERROR", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
    
    public BackupException(String message, String errorCode) {
        super(message, errorCode, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public BackupException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}