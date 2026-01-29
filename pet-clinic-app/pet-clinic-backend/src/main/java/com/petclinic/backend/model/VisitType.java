package com.petclinic.backend.model;

/**
 * Enumeration of visit types with duration and specialty requirements
 */
public enum VisitType {
    WELLNESS_EXAM("Wellness Examination", 30, false, Specialty.GENERAL_PRACTICE),
    VACCINATION("Vaccination", 15, false, Specialty.GENERAL_PRACTICE),
    EMERGENCY("Emergency Visit", 60, true, Specialty.EMERGENCY_MEDICINE),
    SURGERY_CONSULTATION("Surgery Consultation", 45, false, Specialty.SURGERY),
    SURGERY("Surgery", 120, true, Specialty.SURGERY),
    DENTAL_CLEANING("Dental Cleaning", 90, false, Specialty.DENTISTRY),
    DENTAL_SURGERY("Dental Surgery", 120, true, Specialty.DENTISTRY),
    CARDIOLOGY_EXAM("Cardiology Examination", 45, false, Specialty.CARDIOLOGY),
    DERMATOLOGY_EXAM("Dermatology Examination", 30, false, Specialty.DERMATOLOGY),
    ORTHOPEDIC_EXAM("Orthopedic Examination", 45, false, Specialty.ORTHOPEDICS),
    ONCOLOGY_CONSULTATION("Oncology Consultation", 60, false, Specialty.ONCOLOGY),
    OPHTHALMOLOGY_EXAM("Ophthalmology Examination", 30, false, Specialty.OPHTHALMOLOGY),
    FOLLOW_UP("Follow-up Visit", 20, false, Specialty.GENERAL_PRACTICE),
    GROOMING("Grooming", 60, false, Specialty.GENERAL_PRACTICE),
    BEHAVIORAL_CONSULTATION("Behavioral Consultation", 60, false, Specialty.GENERAL_PRACTICE);
    
    private final String displayName;
    private final int defaultDurationMinutes;
    private final boolean requiresSpecialist;
    private final Specialty recommendedSpecialty;
    
    VisitType(String displayName, int defaultDurationMinutes, boolean requiresSpecialist, Specialty recommendedSpecialty) {
        this.displayName = displayName;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.requiresSpecialist = requiresSpecialist;
        this.recommendedSpecialty = recommendedSpecialty;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }
    
    public boolean requiresSpecialist() {
        return requiresSpecialist;
    }
    
    public Specialty getRecommendedSpecialty() {
        return recommendedSpecialty;
    }
    
    /**
     * Find visit type by display name (case-insensitive)
     * @param displayName Display name to search for
     * @return Matching visit type, or null if not found
     */
    public static VisitType findByDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        
        for (VisitType visitType : values()) {
            if (visitType.displayName.equalsIgnoreCase(displayName.trim())) {
                return visitType;
            }
        }
        return null;
    }
    
    /**
     * Check if this visit type is an emergency
     * @return true if emergency visit type, false otherwise
     */
    public boolean isEmergency() {
        return this == EMERGENCY || this == SURGERY;
    }
    
    /**
     * Check if this visit type is preventive care
     * @return true if preventive care, false otherwise
     */
    public boolean isPreventiveCare() {
        return this == WELLNESS_EXAM || this == VACCINATION || this == DENTAL_CLEANING;
    }
    
    /**
     * Check if this visit type requires anesthesia
     * @return true if requires anesthesia, false otherwise
     */
    public boolean requiresAnesthesia() {
        return this == SURGERY || this == DENTAL_SURGERY || this == DENTAL_CLEANING;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}