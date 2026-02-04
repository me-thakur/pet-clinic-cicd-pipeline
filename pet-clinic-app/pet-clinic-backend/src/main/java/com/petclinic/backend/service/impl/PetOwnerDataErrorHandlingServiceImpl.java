package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.PetWithOwnerInfo;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.service.EnhancedPetService;
import com.petclinic.backend.service.PetOwnerDataErrorHandlingService;
import com.petclinic.backend.service.PetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Implementation of PetOwnerDataErrorHandlingService
 * Provides comprehensive error handling, retry mechanisms, and partial data display
 * for pet-owner data operations
 * 
 * Validates: Requirements 6.4
 */
@Service
public class PetOwnerDataErrorHandlingServiceImpl implements PetOwnerDataErrorHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(PetOwnerDataErrorHandlingServiceImpl.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second
    private static final long BACKOFF_MULTIPLIER = 2;

    @Autowired
    @Lazy
    private EnhancedPetService enhancedPetService;
    
    @Autowired
    @Lazy
    private PetService petService;

    @Override
    public <T> T executeDataOperationWithRecovery(
            DataOperation<T> dataOperation,
            String operationType,
            DataOperationContext context) {
        
        logger.debug("Executing data operation with recovery for type: {}", operationType);
        
        Exception lastException = null;
        long delay = RETRY_DELAY_MS;
        
        // Try full data operation with retry
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("Attempt {} of {} for {} operation", attempt, MAX_RETRY_ATTEMPTS, operationType);
                
                T result = dataOperation.execute();
                
                if (attempt > 1) {
                    logger.info("Data operation succeeded on attempt {} for type: {}", attempt, operationType);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                logDataLoadingError(operationType, context, e, attempt);
                
                // Don't retry on the last attempt
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    break;
                }
                
                // Check if error is retryable
                if (!isRetryableError(e)) {
                    logger.warn("Non-retryable error encountered for {} operation, stopping retries", operationType);
                    break;
                }
                
                // Wait before retry with exponential backoff
                try {
                    logger.debug("Waiting {}ms before retry attempt {} for {} operation", delay, attempt + 1, operationType);
                    Thread.sleep(delay);
                    delay *= BACKOFF_MULTIPLIER;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry delay interrupted for {} operation", operationType);
                    break;
                }
            }
        }
        
        // Full operation failed, try partial data recovery if allowed
        logger.warn("Full data operation failed for {}, attempting partial data recovery", operationType);
        
        if (context != null && context.isAllowPartialData()) {
            try {
                return attemptPartialDataRecovery(operationType, context, lastException);
            } catch (Exception e) {
                logger.error("Partial data recovery also failed for {}: {}", operationType, e.getMessage());
            }
        }
        
        // Complete failure - throw the last exception
        if (lastException instanceof RuntimeException) {
            throw (RuntimeException) lastException;
        } else {
            throw new RuntimeException("Data operation failed: " + lastException.getMessage(), lastException);
        }
    }

    @Override
    public boolean isPetOwnerDataServiceAvailable() {
        try {
            // Simple health check - try to get a small page of pets
            enhancedPetService.findPetsWithOwnerInfo(org.springframework.data.domain.PageRequest.of(0, 1));
            return true;
        } catch (Exception e) {
            logger.warn("Pet-owner data service availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<PetWithOwnerInfo> getPartialPetInfo(Long petId) {
        logger.debug("Getting partial pet info for pet ID: {}", petId);
        
        try {
            // Try to get basic pet information without owner join
            Optional<Pet> petOpt = petService.findById(petId);
            
            if (petOpt.isPresent()) {
                Pet pet = petOpt.get();
                PetWithOwnerInfo partialInfo = createPetWithPlaceholderOwner(
                    pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed());
                
                logger.debug("Created partial pet info for pet ID: {}", petId);
                return Optional.of(partialInfo);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to get partial pet info for pet ID {}: {}", petId, e.getMessage());
        }
        
        return Optional.empty();
    }

    @Override
    public Page<PetWithOwnerInfo> getPartialPetsInfo(Pageable pageable) {
        logger.debug("Getting partial pets info with pagination: page={}, size={}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // Try to get basic pet information without owner joins using advanced search
            Page<Pet> petsPage = petService.searchPetsAdvanced(null, null, null, null, 
                                                              pageable.getPageNumber(), pageable.getPageSize());
            
            List<PetWithOwnerInfo> partialInfoList = petsPage.getContent().stream()
                .map(pet -> createPetWithPlaceholderOwner(pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed()))
                .collect(Collectors.toList());
            
            logger.debug("Created partial info for {} pets", partialInfoList.size());
            return new PageImpl<>(partialInfoList, pageable, petsPage.getTotalElements());
            
        } catch (Exception e) {
            logger.error("Failed to get partial pets info: {}", e.getMessage(), e);
            // Return empty page as last resort
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    @Override
    public Optional<PetWithOwnerInfo> refreshPetOwnerDataWithRetry(Long petId) {
        logger.debug("Refreshing pet-owner data with retry for pet ID: {}", petId);
        
        DataOperationContext context = new DataOperationContext();
        context.setPetId(petId);
        context.setAllowPartialData(true);
        context.setRequestId("REFRESH-" + System.currentTimeMillis());
        
        try {
            return executeDataOperationWithRecovery(
                () -> enhancedPetService.refreshPetOwnerInfo(petId),
                "refresh_pet_owner_data",
                context
            );
        } catch (Exception e) {
            logger.error("Failed to refresh pet-owner data for pet ID {}: {}", petId, e.getMessage());
            return getPartialPetInfo(petId);
        }
    }

    @Override
    public String getDataLoadingErrorMessage(String operationType, Throwable error) {
        if (error == null) {
            return String.format("Pet and owner data is temporarily unavailable for %s operation. Showing available information.", operationType);
        }
        
        // Categorize errors and provide appropriate messages
        if (error instanceof TimeoutException) {
            return String.format("Loading pet and owner data is taking longer than expected for %s. Showing available information.", operationType);
        }
        
        if (error instanceof SQLException) {
            return "We're experiencing database connectivity issues. Showing available pet information while we resolve this.";
        }
        
        if (error instanceof IllegalArgumentException) {
            return String.format("Invalid parameters provided for %s operation. Please check your request.", operationType);
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("connection")) {
            return "We're experiencing connectivity issues. Showing available pet information.";
        }
        
        if (error.getMessage() != null && error.getMessage().toLowerCase().contains("timeout")) {
            return String.format("Data loading timed out for %s. Showing available information.", operationType);
        }
        
        // Generic error message
        return String.format("We encountered an issue loading complete pet and owner data for %s. Showing available information.", operationType);
    }

    @Override
    public void logDataLoadingError(String operationType, DataOperationContext context, 
                                   Throwable error, int attemptNumber) {
        logger.error("Data loading error details - Operation: {}, Context: {}, Attempt: {}, Error: {}, Message: {}", 
                    operationType, contextToString(context), attemptNumber, 
                    error.getClass().getSimpleName(), error.getMessage());
        
        // Log stack trace for debugging (only on first attempt to avoid spam)
        if (attemptNumber == 1) {
            logger.debug("Full stack trace for {} data loading error:", operationType, error);
        }
        
        // Log additional context
        logger.error("Data loading context - Service available: {}, Thread: {}, Timestamp: {}", 
                    isPetOwnerDataServiceAvailable(), Thread.currentThread().getName(), 
                    System.currentTimeMillis());
    }

    @Override
    public PetWithOwnerInfo createPetWithPlaceholderOwner(Long petId, String petName, String species, String breed) {
        logger.debug("Creating pet with placeholder owner for pet ID: {}", petId);
        
        PetWithOwnerInfo petInfo = new PetWithOwnerInfo();
        
        // Set pet information
        petInfo.setPetId(petId);
        petInfo.setPetName(petName != null ? petName : "Unknown Pet");
        petInfo.setPetSpecies(species != null ? species : "Unknown Species");
        petInfo.setPetBreed(breed != null ? breed : "Unknown Breed");
        petInfo.setBirthDate(LocalDate.now().minusYears(1)); // Default age
        petInfo.setCreatedAt(LocalDate.now());
        petInfo.setUpdatedAt(LocalDate.now());
        
        // Set placeholder owner information
        petInfo.setOwnerId(null);
        petInfo.setOwnerFirstName("Owner information");
        petInfo.setOwnerLastName("temporarily unavailable");
        petInfo.setOwnerEmail("Please refresh to try again");
        petInfo.setOwnerMobileNumber("");
        petInfo.setOwnerTelephone("");
        petInfo.setOwnerAddress("");
        petInfo.setOwnerCity("");
        petInfo.setOwnerState("");
        petInfo.setOwnerZipCode("");
        
        // Note: ownerFullName and ownerContactInfo are computed properties
        // They will return appropriate default values when owner data is missing
        
        logger.debug("Created placeholder pet info for pet ID: {}", petId);
        return petInfo;
    }

    /**
     * Attempt partial data recovery based on operation type
     */
    @SuppressWarnings("unchecked")
    private <T> T attemptPartialDataRecovery(String operationType, DataOperationContext context, Exception originalError) {
        logger.info("Attempting partial data recovery for operation: {}", operationType);
        
        try {
            switch (operationType.toLowerCase()) {
                case "find_pet_with_owner_info":
                case "refresh_pet_owner_data":
                    if (context.getPetId() != null) {
                        Optional<PetWithOwnerInfo> partialInfo = getPartialPetInfo(context.getPetId());
                        return (T) partialInfo;
                    }
                    break;
                    
                case "find_pets_with_owner_info":
                case "search_pets_with_owner_info":
                    if (context.getPageable() != null) {
                        Page<PetWithOwnerInfo> partialPage = getPartialPetsInfo(context.getPageable());
                        return (T) partialPage;
                    }
                    break;
                    
                default:
                    logger.warn("No partial recovery strategy available for operation: {}", operationType);
                    break;
            }
        } catch (Exception e) {
            logger.error("Partial data recovery failed for {}: {}", operationType, e.getMessage());
        }
        
        throw new RuntimeException("Complete data operation failure: " + originalError.getMessage(), originalError);
    }

    /**
     * Determine if an error is retryable
     */
    private boolean isRetryableError(Exception e) {
        // Don't retry validation errors or illegal arguments
        if (e instanceof IllegalArgumentException) {
            return false;
        }
        
        // Don't retry security-related errors
        if (e instanceof SecurityException) {
            return false;
        }
        
        // Retry database connection issues, timeouts, and temporary failures
        if (e instanceof SQLException || 
            e instanceof TimeoutException ||
            (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection") ||
                e.getMessage().toLowerCase().contains("timeout") ||
                e.getMessage().toLowerCase().contains("temporary")
            ))) {
            return true;
        }
        
        // Default to retrying unknown exceptions
        return true;
    }

    /**
     * Convert context to string for logging
     */
    private String contextToString(DataOperationContext context) {
        if (context == null) {
            return "null";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("DataOperationContext{");
        sb.append("petId=").append(context.getPetId());
        sb.append(", ownerId=").append(context.getOwnerId());
        sb.append(", searchTerm='").append(context.getSearchTerm()).append("'");
        sb.append(", allowPartialData=").append(context.isAllowPartialData());
        sb.append(", requestId='").append(context.getRequestId()).append("'");
        sb.append("}");
        
        return sb.toString();
    }
}