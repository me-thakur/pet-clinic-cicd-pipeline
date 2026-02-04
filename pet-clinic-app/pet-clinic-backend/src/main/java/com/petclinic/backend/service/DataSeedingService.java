package com.petclinic.backend.service;

/**
 * Service interface for seeding the database with dummy data for development and testing
 */
public interface DataSeedingService {
    
    /**
     * Seeds all data types in the correct order
     */
    void seedAllData() throws Exception;
    
    /**
     * Checks if data seeding is needed based on configuration and existing data
     */
    boolean isSeedingNeeded();
    
    /**
     * Clears all seeded data from the database
     */
    void clearAllData() throws Exception;
    
    /**
     * Gets statistics about current data in the database
     */
    DataSeedingStatistics getStatistics();
    
    /**
     * Statistics class for data seeding information
     */
    class DataSeedingStatistics {
        private final long ownerCount;
        private final long petCount;
        private final long veterinarianCount;
        private final long visitCount;
        private final long userCount;
        
        public DataSeedingStatistics(long ownerCount, long petCount, long veterinarianCount, long visitCount, long userCount) {
            this.ownerCount = ownerCount;
            this.petCount = petCount;
            this.veterinarianCount = veterinarianCount;
            this.visitCount = visitCount;
            this.userCount = userCount;
        }
        
        public long getOwnerCount() { return ownerCount; }
        public long getPetCount() { return petCount; }
        public long getVeterinarianCount() { return veterinarianCount; }
        public long getVisitCount() { return visitCount; }
        public long getUserCount() { return userCount; }
    }
}