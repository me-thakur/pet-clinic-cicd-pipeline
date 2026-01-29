package com.petclinic.backend.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for database backup and restore operations.
 * Provides functionality to create backups, restore from backups,
 * and manage backup files.
 */
public interface BackupService {
    
    /**
     * Creates a backup of the current database.
     * 
     * @param backupName Optional name for the backup file. If null, generates timestamp-based name
     * @return File object representing the created backup file
     * @throws BackupException if backup creation fails
     */
    File createBackup(String backupName);
    
    /**
     * Restores the database from a backup file.
     * 
     * @param backupFile The backup file to restore from
     * @throws BackupException if restore operation fails
     */
    void restoreFromBackup(File backupFile);
    
    /**
     * Lists all available backup files.
     * 
     * @return List of backup files sorted by creation date (newest first)
     */
    List<File> listBackups();
    
    /**
     * Validates a backup file to ensure it's complete and not corrupted.
     * 
     * @param backupFile The backup file to validate
     * @return true if backup is valid, false otherwise
     */
    boolean validateBackup(File backupFile);
    
    /**
     * Deletes a backup file.
     * 
     * @param backupFile The backup file to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteBackup(File backupFile);
    
    /**
     * Gets the size of a backup file in bytes.
     * 
     * @param backupFile The backup file
     * @return Size in bytes, or -1 if file doesn't exist
     */
    long getBackupSize(File backupFile);
    
    /**
     * Gets the creation timestamp of a backup file.
     * 
     * @param backupFile The backup file
     * @return Creation timestamp, or null if cannot be determined
     */
    LocalDateTime getBackupTimestamp(File backupFile);
}