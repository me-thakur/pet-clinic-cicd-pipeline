package com.petclinic.backend.service.impl;

import com.petclinic.backend.exception.BackupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BackupServiceImpl
 */
class BackupServiceImplTest {
    
    @Mock
    private DataSource dataSource;
    
    @Mock
    private Connection connection;
    
    @Mock
    private Statement statement;
    
    @TempDir
    Path tempDir;
    
    private BackupServiceImpl backupService;
    
    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        
        // Initialize service with H2 database URL for testing
        backupService = new BackupServiceImpl(
            dataSource, 
            tempDir.toString(), 
            "jdbc:h2:mem:testdb"
        );
    }
    
    @Test
    void createBackup_WithCustomName_ShouldCreateBackupFile() throws SQLException {
        // Given
        String backupName = "test_backup";
        
        // When
        File backupFile = backupService.createBackup(backupName);
        
        // Then
        assertNotNull(backupFile);
        assertTrue(backupFile.getName().contains(backupName));
        assertTrue(backupFile.getName().endsWith(".sql"));
        verify(statement).execute(contains("SCRIPT TO"));
    }
    
    @Test
    void createBackup_WithoutName_ShouldCreateTimestampedBackupFile() throws SQLException {
        // When
        File backupFile = backupService.createBackup(null);
        
        // Then
        assertNotNull(backupFile);
        assertTrue(backupFile.getName().startsWith("backup_"));
        assertTrue(backupFile.getName().endsWith(".sql"));
        verify(statement).execute(contains("SCRIPT TO"));
    }
    
    @Test
    void createBackup_WhenSQLExceptionOccurs_ShouldThrowBackupException() throws SQLException {
        // Given
        when(statement.execute(anyString())).thenThrow(new SQLException("Database error"));
        
        // When & Then
        assertThrows(BackupException.class, () -> backupService.createBackup("test"));
    }
    
    @Test
    void restoreFromBackup_WithValidBackupFile_ShouldRestoreSuccessfully() throws SQLException, IOException {
        // Given
        File backupFile = createValidBackupFile();
        
        // When
        backupService.restoreFromBackup(backupFile);
        
        // Then
        verify(statement).execute("DROP ALL OBJECTS");
        verify(statement).execute(contains("RUNSCRIPT FROM"));
    }
    
    @Test
    void restoreFromBackup_WithNonExistentFile_ShouldThrowBackupException() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.sql");
        
        // When & Then
        assertThrows(BackupException.class, () -> backupService.restoreFromBackup(nonExistentFile));
    }
    
    @Test
    void restoreFromBackup_WithInvalidBackupFile_ShouldThrowBackupException() throws IOException {
        // Given
        File invalidBackupFile = createInvalidBackupFile();
        
        // When & Then
        assertThrows(BackupException.class, () -> backupService.restoreFromBackup(invalidBackupFile));
    }
    
    @Test
    void listBackups_WithMultipleBackupFiles_ShouldReturnSortedList() throws IOException, InterruptedException {
        // Given
        createBackupFile("backup_20240101_120000.sql");
        Thread.sleep(10); // Ensure different timestamps
        createBackupFile("backup_20240102_120000.sql");
        Thread.sleep(10);
        createBackupFile("backup_20240103_120000.sql");
        
        // When
        List<File> backups = backupService.listBackups();
        
        // Then
        assertEquals(3, backups.size());
        // Should be sorted by modification time (newest first)
        assertTrue(backups.get(0).lastModified() >= backups.get(1).lastModified());
        assertTrue(backups.get(1).lastModified() >= backups.get(2).lastModified());
    }
    
    @Test
    void listBackups_WithEmptyDirectory_ShouldReturnEmptyList() {
        // When
        List<File> backups = backupService.listBackups();
        
        // Then
        assertTrue(backups.isEmpty());
    }
    
    @Test
    void validateBackup_WithValidBackupFile_ShouldReturnTrue() throws IOException {
        // Given
        File validBackupFile = createValidBackupFile();
        
        // When
        boolean isValid = backupService.validateBackup(validBackupFile);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void validateBackup_WithInvalidBackupFile_ShouldReturnFalse() throws IOException {
        // Given
        File invalidBackupFile = createInvalidBackupFile();
        
        // When
        boolean isValid = backupService.validateBackup(invalidBackupFile);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateBackup_WithEmptyFile_ShouldReturnFalse() throws IOException {
        // Given
        File emptyFile = new File(tempDir.toFile(), "empty.sql");
        emptyFile.createNewFile();
        
        // When
        boolean isValid = backupService.validateBackup(emptyFile);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateBackup_WithNonExistentFile_ShouldReturnFalse() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.sql");
        
        // When
        boolean isValid = backupService.validateBackup(nonExistentFile);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void deleteBackup_WithExistingFile_ShouldReturnTrue() throws IOException {
        // Given
        File backupFile = createBackupFile("test_backup.sql");
        assertTrue(backupFile.exists());
        
        // When
        boolean deleted = backupService.deleteBackup(backupFile);
        
        // Then
        assertTrue(deleted);
        assertFalse(backupFile.exists());
    }
    
    @Test
    void deleteBackup_WithNonExistentFile_ShouldReturnFalse() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.sql");
        
        // When
        boolean deleted = backupService.deleteBackup(nonExistentFile);
        
        // Then
        assertFalse(deleted);
    }
    
    @Test
    void getBackupSize_WithExistingFile_ShouldReturnCorrectSize() throws IOException {
        // Given
        File backupFile = createBackupFile("test_backup.sql");
        long expectedSize = backupFile.length();
        
        // When
        long actualSize = backupService.getBackupSize(backupFile);
        
        // Then
        assertEquals(expectedSize, actualSize);
    }
    
    @Test
    void getBackupSize_WithNonExistentFile_ShouldReturnMinusOne() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.sql");
        
        // When
        long size = backupService.getBackupSize(nonExistentFile);
        
        // Then
        assertEquals(-1, size);
    }
    
    @Test
    void getBackupTimestamp_WithTimestampInFilename_ShouldParseCorrectly() throws IOException {
        // Given
        File backupFile = createBackupFile("backup_test_20240115_143000.sql");
        
        // When
        LocalDateTime timestamp = backupService.getBackupTimestamp(backupFile);
        
        // Then
        assertNotNull(timestamp);
        assertEquals(2024, timestamp.getYear());
        assertEquals(1, timestamp.getMonthValue());
        assertEquals(15, timestamp.getDayOfMonth());
        assertEquals(14, timestamp.getHour());
        assertEquals(30, timestamp.getMinute());
        assertEquals(0, timestamp.getSecond());
    }
    
    @Test
    void getBackupTimestamp_WithoutTimestampInFilename_ShouldFallbackToFileModificationTime() throws IOException {
        // Given
        File backupFile = createBackupFile("backup_without_timestamp.sql");
        
        // When
        LocalDateTime timestamp = backupService.getBackupTimestamp(backupFile);
        
        // Then
        assertNotNull(timestamp);
        // Should be close to current time (within last minute)
        assertTrue(timestamp.isAfter(LocalDateTime.now().minusMinutes(1)));
    }
    
    @Test
    void getBackupTimestamp_WithNonExistentFile_ShouldReturnNull() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.sql");
        
        // When
        LocalDateTime timestamp = backupService.getBackupTimestamp(nonExistentFile);
        
        // Then
        assertNull(timestamp);
    }
    
    // Helper methods
    
    private File createValidBackupFile() throws IOException {
        File backupFile = new File(tempDir.toFile(), "valid_backup.sql");
        try (FileWriter writer = new FileWriter(backupFile)) {
            writer.write("CREATE TABLE test_table (id INT PRIMARY KEY);\n");
            writer.write("INSERT INTO test_table VALUES (1);\n");
        }
        return backupFile;
    }
    
    private File createInvalidBackupFile() throws IOException {
        File backupFile = new File(tempDir.toFile(), "invalid_backup.sql");
        try (FileWriter writer = new FileWriter(backupFile)) {
            writer.write("This is not a valid SQL backup file\n");
            writer.write("It contains no SQL statements\n");
        }
        return backupFile;
    }
    
    private File createBackupFile(String fileName) throws IOException {
        File backupFile = new File(tempDir.toFile(), fileName);
        try (FileWriter writer = new FileWriter(backupFile)) {
            writer.write("CREATE TABLE test (id INT);\n");
        }
        return backupFile;
    }
}