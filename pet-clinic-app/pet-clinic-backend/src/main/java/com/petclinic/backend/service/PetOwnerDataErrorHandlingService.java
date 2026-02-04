package com.petclinic.backend.service;

import com.petclinic.backend.dto.PetWithOwnerInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for handling pet-owner data loading errors and recovery
 * Provides fallback mechanisms and retry logic for pet-owner data operations
 * 
 * Validates: Requirements 6.4
 */
public interface PetOwnerDataErrorHandlingService {

    /**
     * Execute pet-owner data loading operation with error recovery
     * 
     * @param dataOperation The data loading operation to execute
     * @param operationType Type of operation (findAll, findById, search, etc.)
     * @param context Additional context for the operation
     * @return Result with data or fallback information
     */
    <T> T executeDataOperationWithRecovery(
            DataOperation<T> dataOperation,
            String operationType,
            DataOperationContext context);

    /**
     * Check if pet-owner data service is available
     * 
     * @return true if service is available, false otherwise
     */
    boolean isPetOwnerDataServiceAvailable();

    /**
     * Get partial pet information when owner data loading fails
     * 
     * @param petId Pet ID
     * @return PetWithOwnerInfo with pet data and placeholder owner info
     */
    Optional<PetWithOwnerInfo> getPartialPetInfo(Long petId);

    /**
     * Get partial pets information when owner data loading fails
     * 
     * @param pageable Pagination parameters
     * @return Page of PetWithOwnerInfo with pet data and placeholder owner info
     */
    Page<PetWithOwnerInfo> getPartialPetsInfo(Pageable pageable);

    /**
     * Refresh pet-owner data with retry mechanism
     * 
     * @param petId Pet ID to refresh
     * @return Updated pet with owner information or partial data
     */
    Optional<PetWithOwnerInfo> refreshPetOwnerDataWithRetry(Long petId);

    /**
     * Get user-friendly error message for data loading failures
     * 
     * @param operationType Type of operation that failed
     * @param error The original error
     * @return User-friendly error message
     */
    String getDataLoadingErrorMessage(String operationType, Throwable error);

    /**
     * Log detailed data loading error information
     * 
     * @param operationType Type of operation
     * @param context Operation context
     * @param error The error that occurred
     * @param attemptNumber The retry attempt number
     */
    void logDataLoadingError(String operationType, DataOperationContext context, 
                            Throwable error, int attemptNumber);

    /**
     * Create placeholder owner info for pets without owner data
     * 
     * @param petId Pet ID
     * @param petName Pet name
     * @return PetWithOwnerInfo with placeholder owner data
     */
    PetWithOwnerInfo createPetWithPlaceholderOwner(Long petId, String petName, String species, String breed);

    /**
     * Functional interface for data operations
     */
    @FunctionalInterface
    interface DataOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Context information for data operations
     */
    class DataOperationContext {
        private Long petId;
        private Long ownerId;
        private String searchTerm;
        private Pageable pageable;
        private String requestId;
        private boolean allowPartialData;

        public DataOperationContext() {}

        public DataOperationContext(Long petId, Long ownerId, String searchTerm, 
                                   Pageable pageable, String requestId, boolean allowPartialData) {
            this.petId = petId;
            this.ownerId = ownerId;
            this.searchTerm = searchTerm;
            this.pageable = pageable;
            this.requestId = requestId;
            this.allowPartialData = allowPartialData;
        }

        // Getters and setters
        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }

        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

        public String getSearchTerm() { return searchTerm; }
        public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

        public Pageable getPageable() { return pageable; }
        public void setPageable(Pageable pageable) { this.pageable = pageable; }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public boolean isAllowPartialData() { return allowPartialData; }
        public void setAllowPartialData(boolean allowPartialData) { this.allowPartialData = allowPartialData; }
    }
}