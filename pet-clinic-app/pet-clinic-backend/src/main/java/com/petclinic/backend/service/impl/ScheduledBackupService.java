package com.petclinic.backend.service.impl;

import com.petclinic.backend.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for automatic scheduled backups and backup retention management.
 */
@Service
@ConditionalOnProperty(name = "app.backup.auto-backup.enabled", havingValue = "true")
public class ScheduledBackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledBackupService.class);
    
    private final BackupService backupService;
    private final int retentionDays;
    
    public ScheduledBackupService(BackupService backupService,
                                @Value("${app.backup.retention-days:30}") int retentionDays) {
        this.backupService = backupService;
        this.retentionDays = retentionDays;
    }
    
    /**
     * Scheduled method to create automatic backups.
     * Runs according to the cron expression defined in app.backup.auto-backup.schedule
     */
    @Scheduled(cron = "${app.backup.auto-backup.schedule:0 0 2 * * ?}")
    public void createScheduledBackup() {
        logger.info("Starting scheduled backup process");
        
        try {
            String backupName = "scheduled_" + LocalDateTime.now().toLocalDate();
            File backupFile = backupService.createBackup(backupName);
            
            logger.info("Scheduled backup completed successfully: {}", backupFile.getName());
            
            // Clean up old backups after successful backup
            cleanupOldBackups();
            
        } catch (Exception e) {
            logger.error("Scheduled backup failed", e);
        }
    }
    
    /**
     * Cleans up backup files older than the retention period.
     */
    @Scheduled(cron = "0 30 2 * * ?") // Run 30 minutes after backup
    public void cleanupOldBackups() {
        logger.info("Starting backup cleanup process (retention: {} days)", retentionDays);
        
        try {
            List<File> backups = backupService.listBackups();
            LocalDateTime cutoffDate = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
            
            int deletedCount = 0;
            for (File backup : backups) {
                LocalDateTime backupTimestamp = backupService.getBackupTimestamp(backup);
                
                if (backupTimestamp != null && backupTimestamp.isBefore(cutoffDate)) {
                    if (backupService.deleteBackup(backup)) {
                        deletedCount++;
                        logger.debug("Deleted old backup: {}", backup.getName());
                    } else {
                        logger.warn("Failed to delete old backup: {}", backup.getName());
                    }
                }
            }
            
            if (deletedCount > 0) {
                logger.info("Backup cleanup completed. Deleted {} old backup files", deletedCount);
            } else {
                logger.info("Backup cleanup completed. No old backups to delete");
            }
            
        } catch (Exception e) {
            logger.error("Backup cleanup failed", e);
        }
    }
}