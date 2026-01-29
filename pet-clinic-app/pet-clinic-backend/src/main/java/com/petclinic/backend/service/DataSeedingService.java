package com.petclinic.backend.service;

/**
 * Service interface for data seeding operations
 * Provides methods for seeding development and test data
 * 
 * Validates: Requirements 8.3, 8.4
 */
public interface DataSeedingService {
    
    /**
     * Seed all data for development environment
     * @throws Exception if seeding fails
     */
    void seedAllData() throws Exception;
    
    /**
     * Check if data seeding is needed
     * @return true if seeding should be performed, false otherwise
     */
    boolean isSeedingNeeded();
    
    /**
     * Clear all seeded data (for testing purposes)
     * @throws Exception if clearing fails
     */
    void clearAllData() throws Exception;
    
    /**
     * Get data seeding statistics
     * @return statistics about seeded data
     */
    DataSeedingStatistics getStatistics();
    
    /**
     * Data seeding statistics holder
     */
    class DataSeedingStatistics {
        private long ownerCount;
        private long petCount;
        private long veterinarianCount;
        private long visitCount;
        private long userCount;
        
        public DataSeedingStatistics(long ownerCount, long petCount, long veterinarianCount, long visitCount, long userCount) {
            this.ownerCount = ownerCount;
            this.petCount = petCount;
            this.veterinarianCount = veterinarianCount;
            this.visitCount = visitCount;
            this.userCount = userCount;
        }
        
        // Getters
        public long getOwnerCount() { return ownerCount; }
        public long getPetCount() { return petCount; }
        public long getVeterinarianCount() { return veterinarianCount; }
        public long getVisitCount() { return visitCount; }
        public long getUserCount() { return userCount; }
        
        @Override
        public String toString() {
            return String.format("DataSeedingStatistics{owners=%d, pets=%d, veterinarians=%d, visits=%d, users=%d}", 
                    ownerCount, petCount, veterinarianCount, visitCount, userCount);
        }
    }
}