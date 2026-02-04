package com.petclinic.backend.service;

import com.petclinic.backend.model.Visit;
import java.util.List;

/**
 * Service interface for validating visit completion status consistency
 * Validates: Requirements 5.1, 5.2
 */
public interface VisitStatusValidationService {
    
    /**
     * Validation result containing inconsistency information
     */
    class ValidationResult {
        private final boolean hasInconsistencies;
        private final List<InconsistencyReport> inconsistencies;
        private final int totalVisitsChecked;
        private final int inconsistentVisitsCount;
        
        public ValidationResult(boolean hasInconsistencies, List<InconsistencyReport> inconsistencies, 
                              int totalVisitsChecked, int inconsistentVisitsCount) {
            this.hasInconsistencies = hasInconsistencies;
            this.inconsistencies = inconsistencies;
            this.totalVisitsChecked = totalVisitsChecked;
            this.inconsistentVisitsCount = inconsistentVisitsCount;
        }
        
        public boolean hasInconsistencies() { return hasInconsistencies; }
        public List<InconsistencyReport> getInconsistencies() { return inconsistencies; }
        public int getTotalVisitsChecked() { return totalVisitsChecked; }
        public int getInconsistentVisitsCount() { return inconsistentVisitsCount; }
    }
    
    /**
     * Report of a specific inconsistency found
     */
    class InconsistencyReport {
        private final Long visitId;
        private final String petName;
        private final String ownerName;
        private final String diagnosis;
        private final String treatment;
        private final boolean expectedCompletionStatus;
        private final String inconsistencyDescription;
        
        public InconsistencyReport(Long visitId, String petName, String ownerName, 
                                 String diagnosis, String treatment, boolean expectedCompletionStatus, 
                                 String inconsistencyDescription) {
            this.visitId = visitId;
            this.petName = petName;
            this.ownerName = ownerName;
            this.diagnosis = diagnosis;
            this.treatment = treatment;
            this.expectedCompletionStatus = expectedCompletionStatus;
            this.inconsistencyDescription = inconsistencyDescription;
        }
        
        public Long getVisitId() { return visitId; }
        public String getPetName() { return petName; }
        public String getOwnerName() { return ownerName; }
        public String getDiagnosis() { return diagnosis; }
        public String getTreatment() { return treatment; }
        public boolean getExpectedCompletionStatus() { return expectedCompletionStatus; }
        public String getInconsistencyDescription() { return inconsistencyDescription; }
    }
    
    /**
     * Validate completion status consistency for all visits
     * @return ValidationResult containing any inconsistencies found
     */
    ValidationResult validateAllVisitStatuses();
    
    /**
     * Validate completion status consistency for visits in a date range
     * @param startDate Start date for validation range
     * @param endDate End date for validation range
     * @return ValidationResult containing any inconsistencies found
     */
    ValidationResult validateVisitStatusesByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    /**
     * Validate completion status consistency for a specific visit
     * @param visitId Visit ID to validate
     * @return ValidationResult containing any inconsistencies found
     */
    ValidationResult validateVisitStatus(Long visitId);
    
    /**
     * Validate completion status consistency for visits by pet
     * @param petId Pet ID to validate visits for
     * @return ValidationResult containing any inconsistencies found
     */
    ValidationResult validateVisitStatusesByPet(Long petId);
    
    /**
     * Check if a visit's completion status is consistent with its data
     * @param visit Visit to check
     * @return true if consistent, false if inconsistent
     */
    boolean isVisitStatusConsistent(Visit visit);
    
    /**
     * Get the expected completion status for a visit based on its diagnosis and treatment
     * @param visit Visit to evaluate
     * @return Expected completion status
     */
    boolean getExpectedCompletionStatus(Visit visit);
    
    /**
     * Recalculation result containing information about the batch operation
     */
    class RecalculationResult {
        private final int totalVisitsProcessed;
        private final int visitsUpdated;
        private final List<Long> updatedVisitIds;
        private final List<String> errors;
        private final long processingTimeMs;
        
        public RecalculationResult(int totalVisitsProcessed, int visitsUpdated, 
                                 List<Long> updatedVisitIds, List<String> errors, long processingTimeMs) {
            this.totalVisitsProcessed = totalVisitsProcessed;
            this.visitsUpdated = visitsUpdated;
            this.updatedVisitIds = updatedVisitIds;
            this.errors = errors;
            this.processingTimeMs = processingTimeMs;
        }
        
        public int getTotalVisitsProcessed() { return totalVisitsProcessed; }
        public int getVisitsUpdated() { return visitsUpdated; }
        public List<Long> getUpdatedVisitIds() { return updatedVisitIds; }
        public List<String> getErrors() { return errors; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
    
    /**
     * Recalculate completion status for all visits in the system
     * Uses the same logic as isCompleted() method to ensure consistency
     * @return RecalculationResult containing operation details
     */
    RecalculationResult recalculateAllVisitStatuses();
    
    /**
     * Recalculate completion status for visits in a specific date range
     * @param startDate Start date for recalculation range
     * @param endDate End date for recalculation range
     * @return RecalculationResult containing operation details
     */
    RecalculationResult recalculateVisitStatusesByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    /**
     * Recalculate completion status for visits by pet
     * @param petId Pet ID to recalculate visits for
     * @return RecalculationResult containing operation details
     */
    RecalculationResult recalculateVisitStatusesByPet(Long petId);
}