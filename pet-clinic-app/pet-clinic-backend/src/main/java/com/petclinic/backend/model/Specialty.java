package com.petclinic.backend.model;

/**
 * Enumeration of veterinary specialties
 * Provides predefined specialty codes for veterinarians
 */
public enum Specialty {
    GENERAL_PRACTICE("General Practice", "GP"),
    SURGERY("Surgery", "SURG"),
    CARDIOLOGY("Cardiology", "CARD"),
    DERMATOLOGY("Dermatology", "DERM"),
    ORTHOPEDICS("Orthopedics", "ORTHO"),
    ONCOLOGY("Oncology", "ONCO"),
    OPHTHALMOLOGY("Ophthalmology", "OPHT"),
    DENTISTRY("Dentistry", "DENT"),
    EMERGENCY("Emergency", "EMRG"),
    EMERGENCY_MEDICINE("Emergency Medicine", "EMRG_MED"),
    INTERNAL_MEDICINE("Internal Medicine", "INTMED"),
    NEUROLOGY("Neurology", "NEURO"),
    RADIOLOGY("Radiology", "RAD"),
    ANESTHESIOLOGY("Anesthesiology", "ANES"),
    PATHOLOGY("Pathology", "PATH"),
    EXOTIC_ANIMALS("Exotic Animals", "EXOTIC");
    
    private final String displayName;
    private final String code;
    
    Specialty(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * Find specialty by display name (case-insensitive)
     * @param displayName Display name to search for
     * @return Matching specialty, or null if not found
     */
    public static Specialty findByDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        
        for (Specialty specialty : values()) {
            if (specialty.displayName.equalsIgnoreCase(displayName.trim())) {
                return specialty;
            }
        }
        return null;
    }
    
    /**
     * Find specialty by code (case-insensitive)
     * @param code Code to search for
     * @return Matching specialty, or null if not found
     */
    public static Specialty findByCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (Specialty specialty : values()) {
            if (specialty.code.equalsIgnoreCase(code.trim())) {
                return specialty;
            }
        }
        return null;
    }
    
    /**
     * Check if a specialty is a surgical specialty
     * @return true if surgical specialty, false otherwise
     */
    public boolean isSurgical() {
        return this == SURGERY || this == ORTHOPEDICS || this == OPHTHALMOLOGY || this == NEUROLOGY;
    }
    
    /**
     * Check if a specialty is an emergency specialty
     * @return true if emergency specialty, false otherwise
     */
    public boolean isEmergency() {
        return this == EMERGENCY || this == EMERGENCY_MEDICINE || this == SURGERY || this == CARDIOLOGY;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}