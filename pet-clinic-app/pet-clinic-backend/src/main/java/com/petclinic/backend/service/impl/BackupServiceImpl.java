package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.BackupException;
import com.petclinic.backend.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of BackupService for database backup and restore operations.
 * Supports both H2 and MySQL databases with appropriate backup strategies.
 */
@Service
public class BackupServiceImpl implements BackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupServiceImpl.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final DataSource dataSource;
    private final String backupDirectory;
    private final String databaseUrl;
    
    public BackupServiceImpl(DataSource dataSource,
                           @Value("${app.backup.directory:./backups}") String backupDirectory,
                           @Value("${spring.datasource.url}") String databaseUrl) {
        this.dataSource = dataSource;
        this.backupDirectory = backupDirectory;
        this.databaseUrl = databaseUrl;
        
        // Create backup directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(backupDirectory));
        } catch (IOException e) {
            logger.error("Failed to create backup directory: {}", backupDirectory, e);
            throw new BackupException("Failed to create backup directory", e);
        }
    }
    
    @Override
    public File createBackup(String backupName) {
        logger.info("Starting database backup process");
        
        String fileName = generateBackupFileName(backupName);
        File backupFile = new File(backupDirectory, fileName);
        
        try {
            if (isH2Database()) {
                createH2Backup(backupFile);
            } else if (isMySQLDatabase()) {
                createMySQLBackup(backupFile);
            } else {
                throw new BackupException("Unsupported database type for backup");
            }
            
            logger.info("Database backup completed successfully: {}", backupFile.getAbsolutePath());
            return backupFile;
            
        } catch (Exception e) {
            logger.error("Failed to create database backup", e);
            // Clean up partial backup file
            if (backupFile.exists()) {
                backupFile.delete();
            }
            throw new BackupException("Failed to create database backup", e);
        }
    }
    
    @Override
    public void restoreFromBackup(File backupFile) {
        if (!backupFile.exists()) {
            throw new BackupException("Backup file does not exist: " + backupFile.getAbsolutePath());
        }
        
        if (!validateBackup(backupFile)) {
            throw new BackupException("Backup file is invalid or corrupted: " + backupFile.getAbsolutePath());
        }
        
        logger.info("Starting database restore from backup: {}", backupFile.getAbsolutePath());
        
        try {
            if (isH2Database()) {
                restoreH2Backup(backupFile);
            } else if (isMySQLDatabase()) {
                restoreMySQLBackup(backupFile);
            } else {
                throw new BackupException("Unsupported database type for restore");
            }
            
            logger.info("Database restore completed successfully from: {}", backupFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Failed to restore database from backup", e);
            throw new BackupException("Failed to restore database from backup", e);
        }
    }
    
    @Override
    public List<File> listBackups() {
        File backupDir = new File(backupDirectory);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return Collections.emptyList();
        }
        
        File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.endsWith(".sql") || name.endsWith(".zip"));
        
        if (backupFiles == null) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(backupFiles)
                .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean validateBackup(File backupFile) {
        if (!backupFile.exists() || !backupFile.isFile()) {
            return false;
        }
        
        // Check if file is not empty
        if (backupFile.length() == 0) {
            return false;
        }
        
        // Basic validation - check if file contains SQL statements
        try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
            String line;
            boolean foundSqlStatement = false;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null && lineCount < 100) {
                line = line.trim().toUpperCase();
                if (line.startsWith("CREATE") || line.startsWith("INSERT") || 
                    line.startsWith("DROP") || line.startsWith("ALTER")) {
                    foundSqlStatement = true;
                    break;
                }
                lineCount++;
            }
            
            return foundSqlStatement;
            
        } catch (IOException e) {
            logger.error("Failed to validate backup file: {}", backupFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    @Override
    public boolean deleteBackup(File backupFile) {
        if (!backupFile.exists()) {
            return false;
        }
        
        try {
            boolean deleted = backupFile.delete();
            if (deleted) {
                logger.info("Backup file deleted: {}", backupFile.getAbsolutePath());
            } else {
                logger.warn("Failed to delete backup file: {}", backupFile.getAbsolutePath());
            }
            return deleted;
        } catch (SecurityException e) {
            logger.error("Security exception while deleting backup file: {}", backupFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    @Override
    public long getBackupSize(File backupFile) {
        return backupFile.exists() ? backupFile.length() : -1;
    }
    
    @Override
    public LocalDateTime getBackupTimestamp(File backupFile) {
        if (!backupFile.exists()) {
            return null;
        }
        
        // Try to extract timestamp from filename
        String fileName = backupFile.getName();
        try {
            // Look for timestamp pattern in filename
            String[] parts = fileName.split("_");
            if (parts.length >= 3) {
                String datePart = parts[parts.length - 2];
                String timePart = parts[parts.length - 1].split("\\.")[0];
                String timestamp = datePart + "_" + timePart;
                return LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT);
            }
        } catch (Exception e) {
            logger.debug("Could not parse timestamp from filename: {}", fileName);
        }
        
        // Fall back to file modification time
        return LocalDateTime.ofEpochSecond(backupFile.lastModified() / 1000, 0, 
                                         java.time.ZoneOffset.systemDefault().getRules()
                                         .getOffset(java.time.Instant.ofEpochMilli(backupFile.lastModified())));
    }
    
    private String generateBackupFileName(String backupName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        if (backupName != null && !backupName.trim().isEmpty()) {
            return String.format("backup_%s_%s.sql", backupName.replaceAll("[^a-zA-Z0-9]", "_"), timestamp);
        } else {
            return String.format("backup_%s.sql", timestamp);
        }
    }
    
    private boolean isH2Database() {
        return databaseUrl.contains("h2");
    }
    
    private boolean isMySQLDatabase() {
        return databaseUrl.contains("mysql");
    }
    
    private void createH2Backup(File backupFile) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // H2 SCRIPT command creates a SQL script with all data
            String scriptCommand = String.format("SCRIPT TO '%s'", backupFile.getAbsolutePath());
            statement.execute(scriptCommand);
        }
    }
    
    private void createMySQLBackup(File backupFile) throws IOException, InterruptedException {
        // For MySQL, use mysqldump command
        // This is a simplified implementation - in production, you'd want more robust handling
        ProcessBuilder processBuilder = new ProcessBuilder(
            "mysqldump",
            "--single-transaction",
            "--routines",
            "--triggers",
            "--all-databases"
        );
        
        processBuilder.redirectOutput(backupFile);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new BackupException("mysqldump process failed with exit code: " + exitCode);
        }
    }
    
    private void restoreH2Backup(File backupFile) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // First, drop all existing tables (be careful in production!)
            statement.execute("DROP ALL OBJECTS");
            
            // Run the backup script
            String runScriptCommand = String.format("RUNSCRIPT FROM '%s'", backupFile.getAbsolutePath());
            statement.execute(runScriptCommand);
        }
    }
    
    private void restoreMySQLBackup(File backupFile) throws IOException, InterruptedException {
        // For MySQL, use mysql command to restore
        ProcessBuilder processBuilder = new ProcessBuilder(
            "mysql"
        );
        
        processBuilder.redirectInput(backupFile);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new BackupException("mysql restore process failed with exit code: " + exitCode);
        }
    }
}