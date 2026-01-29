package com.petclinic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.backend.exception.BackupException;
import com.petclinic.backend.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BackupController
 */
@WebMvcTest(BackupController.class)
class BackupControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private BackupService backupService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createBackup_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        File mockBackupFile = new File("backup_test_20240115_120000.sql");
        when(backupService.createBackup("test")).thenReturn(mockBackupFile);
        when(backupService.getBackupSize(mockBackupFile)).thenReturn(1024L);
        when(backupService.getBackupTimestamp(mockBackupFile)).thenReturn(LocalDateTime.of(2024, 1, 15, 12, 0));
        
        // When & Then
        mockMvc.perform(post("/api/backup/create")
                .param("backupName", "test")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Backup created successfully"))
                .andExpect(jsonPath("$.backupFile").value("backup_test_20240115_120000.sql"))
                .andExpect(jsonPath("$.backupSize").value(1024));
        
        verify(backupService).createBackup("test");
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createBackup_WithoutName_ShouldReturnSuccess() throws Exception {
        // Given
        File mockBackupFile = new File("backup_20240115_120000.sql");
        when(backupService.createBackup(null)).thenReturn(mockBackupFile);
        when(backupService.getBackupSize(mockBackupFile)).thenReturn(2048L);
        when(backupService.getBackupTimestamp(mockBackupFile)).thenReturn(LocalDateTime.of(2024, 1, 15, 12, 0));
        
        // When & Then
        mockMvc.perform(post("/api/backup/create")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        verify(backupService).createBackup(null);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createBackup_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        // Given
        when(backupService.createBackup(anyString())).thenThrow(new BackupException("Backup failed"));
        
        // When & Then
        mockMvc.perform(post("/api/backup/create")
                .param("backupName", "test")
                .with(csrf()))
                .andExpect(status().isInternalServerError());
        
        verify(backupService).createBackup("test");
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void createBackup_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/backup/create")
                .with(csrf()))
                .andExpect(status().isForbidden());
        
        verify(backupService, never()).createBackup(any());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void restoreFromUpload_WithValidFile_ShouldReturnSuccess() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "backup.sql", 
            "text/plain", 
            "CREATE TABLE test (id INT);".getBytes()
        );
        
        // When & Then
        mockMvc.perform(multipart("/api/backup/restore")
                .file(file)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Database restored successfully"))
                .andExpect(jsonPath("$.restoredFrom").value("backup.sql"));
        
        verify(backupService).restoreFromBackup(any(File.class));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void restoreFromExisting_WithValidBackupFile_ShouldReturnSuccess() throws Exception {
        // Given
        File mockBackupFile = new File("backup_test.sql");
        when(backupService.listBackups()).thenReturn(Arrays.asList(mockBackupFile));
        
        // When & Then
        mockMvc.perform(post("/api/backup/restore/backup_test.sql")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Database restored successfully"))
                .andExpect(jsonPath("$.restoredFrom").value("backup_test.sql"));
        
        verify(backupService).restoreFromBackup(mockBackupFile);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void restoreFromExisting_WithNonExistentFile_ShouldReturnNotFound() throws Exception {
        // Given
        when(backupService.listBackups()).thenReturn(Arrays.asList());
        
        // When & Then
        mockMvc.perform(post("/api/backup/restore/nonexistent.sql")
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Backup file not found: nonexistent.sql"));
        
        verify(backupService, never()).restoreFromBackup(any(File.class));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void listBackups_ShouldReturnBackupList() throws Exception {
        // Given
        File backup1 = new File("backup1.sql");
        File backup2 = new File("backup2.sql");
        List<File> backups = Arrays.asList(backup1, backup2);
        
        when(backupService.listBackups()).thenReturn(backups);
        when(backupService.getBackupSize(backup1)).thenReturn(1024L);
        when(backupService.getBackupSize(backup2)).thenReturn(2048L);
        when(backupService.getBackupTimestamp(backup1)).thenReturn(LocalDateTime.of(2024, 1, 15, 12, 0));
        when(backupService.getBackupTimestamp(backup2)).thenReturn(LocalDateTime.of(2024, 1, 16, 12, 0));
        when(backupService.validateBackup(backup1)).thenReturn(true);
        when(backupService.validateBackup(backup2)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/backup/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").value("backup1.sql"))
                .andExpect(jsonPath("$[0].size").value(1024))
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[1].fileName").value("backup2.sql"))
                .andExpect(jsonPath("$[1].size").value(2048))
                .andExpect(jsonPath("$[1].valid").value(true));
        
        verify(backupService).listBackups();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBackup_WithExistingFile_ShouldReturnSuccess() throws Exception {
        // Given
        File mockBackupFile = new File("backup_test.sql");
        when(backupService.listBackups()).thenReturn(Arrays.asList(mockBackupFile));
        when(backupService.deleteBackup(mockBackupFile)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(delete("/api/backup/backup_test.sql")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Backup file deleted successfully"));
        
        verify(backupService).deleteBackup(mockBackupFile);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBackup_WithNonExistentFile_ShouldReturnNotFound() throws Exception {
        // Given
        when(backupService.listBackups()).thenReturn(Arrays.asList());
        
        // When & Then
        mockMvc.perform(delete("/api/backup/nonexistent.sql")
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Backup file not found: nonexistent.sql"));
        
        verify(backupService, never()).deleteBackup(any(File.class));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void validateBackup_WithValidFile_ShouldReturnValidationResult() throws Exception {
        // Given
        File mockBackupFile = new File("backup_test.sql");
        when(backupService.listBackups()).thenReturn(Arrays.asList(mockBackupFile));
        when(backupService.validateBackup(mockBackupFile)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/backup/validate/backup_test.sql")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("backup_test.sql"))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Backup file is valid"));
        
        verify(backupService).validateBackup(mockBackupFile);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void validateBackup_WithInvalidFile_ShouldReturnValidationResult() throws Exception {
        // Given
        File mockBackupFile = new File("backup_test.sql");
        when(backupService.listBackups()).thenReturn(Arrays.asList(mockBackupFile));
        when(backupService.validateBackup(mockBackupFile)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/backup/validate/backup_test.sql")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("backup_test.sql"))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Backup file is invalid or corrupted"));
        
        verify(backupService).validateBackup(mockBackupFile);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void downloadBackup_WithExistingFile_ShouldReturnFile() throws Exception {
        // Given
        File mockBackupFile = new File("backup_test.sql");
        when(backupService.listBackups()).thenReturn(Arrays.asList(mockBackupFile));
        
        // When & Then
        mockMvc.perform(get("/api/backup/download/backup_test.sql"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"backup_test.sql\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void downloadBackup_WithNonExistentFile_ShouldReturnNotFound() throws Exception {
        // Given
        when(backupService.listBackups()).thenReturn(Arrays.asList());
        
        // When & Then
        mockMvc.perform(get("/api/backup/download/nonexistent.sql"))
                .andExpect(status().isNotFound());
    }
}