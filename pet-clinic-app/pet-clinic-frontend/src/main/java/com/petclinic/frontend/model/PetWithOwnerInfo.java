package com.petclinic.frontend.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Frontend model for Pet with complete Owner information
 * Used for displaying pets table with owner details
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4
 */
public class PetWithOwnerInfo {
    
    private Long petId;
    private String petName;
    private String petSpecies;
    private String petBreed;
    private LocalDate birthDate;
    private String medicalHistory;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    
    // Owner information
    private Long ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerEmail;
    private String ownerMobileNumber;
    private String ownerTelephone;
    private String ownerAddress;
    private String ownerCity;
    private String ownerState;
    private String ownerZipCode;
    
    // Constructors
    public PetWithOwnerInfo() {}
    
    public PetWithOwnerInfo(Long petId, String petName, String petSpecies, String petBreed, 
                           LocalDate birthDate, String medicalHistory, LocalDate createdAt, LocalDate updatedAt,
                           Long ownerId, String ownerFirstName, String ownerLastName, 
                           String ownerEmail, String ownerMobileNumber, String ownerTelephone,
                           String ownerAddress, String ownerCity, String ownerState, String ownerZipCode) {
        this.petId = petId;
        this.petName = petName;
        this.petSpecies = petSpecies;
        this.petBreed = petBreed;
        this.birthDate = birthDate;
        this.medicalHistory = medicalHistory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.ownerId = ownerId;
        this.ownerFirstName = ownerFirstName;
        this.ownerLastName = ownerLastName;
        this.ownerEmail = ownerEmail;
        this.ownerMobileNumber = ownerMobileNumber;
        this.ownerTelephone = ownerTelephone;
        this.ownerAddress = ownerAddress;
        this.ownerCity = ownerCity;
        this.ownerState = ownerState;
        this.ownerZipCode = ownerZipCode;
    }
    
    // Convenience methods
    public String getOwnerFullName() {
        if (ownerFirstName == null && ownerLastName == null) {
            return "No Owner Assigned";
        }
        if (ownerFirstName == null) {
            return ownerLastName;
        }
        if (ownerLastName == null) {
            return ownerFirstName;
        }
        return ownerFirstName + " " + ownerLastName;
    }
    
    public String getOwnerContactInfo() {
        StringBuilder contact = new StringBuilder();
        
        if (ownerEmail != null && !ownerEmail.trim().isEmpty()) {
            contact.append(ownerEmail);
        }
        
        if (ownerMobileNumber != null && !ownerMobileNumber.trim().isEmpty()) {
            if (contact.length() > 0) {
                contact.append(" | ");
            }
            contact.append(ownerMobileNumber);
        }
        
        if (ownerTelephone != null && !ownerTelephone.trim().isEmpty() && 
            !Objects.equals(ownerTelephone, ownerMobileNumber)) {
            if (contact.length() > 0) {
                contact.append(" | ");
            }
            contact.append(ownerTelephone);
        }
        
        return contact.length() > 0 ? contact.toString() : "No Contact Info";
    }
    
    public String getOwnerFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (ownerAddress != null && !ownerAddress.trim().isEmpty()) {
            address.append(ownerAddress);
        }
        
        if (ownerCity != null && !ownerCity.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(ownerCity);
        }
        
        if (ownerState != null && !ownerState.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(ownerState);
        }
        
        if (ownerZipCode != null && !ownerZipCode.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(ownerZipCode);
        }
        
        return address.length() > 0 ? address.toString() : "No Address";
    }
    
    public boolean hasOwner() {
        return ownerId != null;
    }
    
    public int getPetAge() {
        if (birthDate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - birthDate.getYear();
    }
    
    public boolean isYoungPet() {
        return getPetAge() < 2;
    }
    
    public boolean isSeniorPet() {
        return getPetAge() > 7;
    }
    
    // Getters and Setters
    public Long getPetId() {
        return petId;
    }
    
    public void setPetId(Long petId) {
        this.petId = petId;
    }
    
    public String getPetName() {
        return petName;
    }
    
    public void setPetName(String petName) {
        this.petName = petName;
    }
    
    public String getPetSpecies() {
        return petSpecies;
    }
    
    public void setPetSpecies(String petSpecies) {
        this.petSpecies = petSpecies;
    }
    
    public String getPetBreed() {
        return petBreed;
    }
    
    public void setPetBreed(String petBreed) {
        this.petBreed = petBreed;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public String getMedicalHistory() {
        return medicalHistory;
    }
    
    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getOwnerFirstName() {
        return ownerFirstName;
    }
    
    public void setOwnerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
    }
    
    public String getOwnerLastName() {
        return ownerLastName;
    }
    
    public void setOwnerLastName(String ownerLastName) {
        this.ownerLastName = ownerLastName;
    }
    
    public String getOwnerEmail() {
        return ownerEmail;
    }
    
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
    
    public String getOwnerMobileNumber() {
        return ownerMobileNumber;
    }
    
    public void setOwnerMobileNumber(String ownerMobileNumber) {
        this.ownerMobileNumber = ownerMobileNumber;
    }
    
    public String getOwnerTelephone() {
        return ownerTelephone;
    }
    
    public void setOwnerTelephone(String ownerTelephone) {
        this.ownerTelephone = ownerTelephone;
    }
    
    public String getOwnerAddress() {
        return ownerAddress;
    }
    
    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }
    
    public String getOwnerCity() {
        return ownerCity;
    }
    
    public void setOwnerCity(String ownerCity) {
        this.ownerCity = ownerCity;
    }
    
    public String getOwnerState() {
        return ownerState;
    }
    
    public void setOwnerState(String ownerState) {
        this.ownerState = ownerState;
    }
    
    public String getOwnerZipCode() {
        return ownerZipCode;
    }
    
    public void setOwnerZipCode(String ownerZipCode) {
        this.ownerZipCode = ownerZipCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PetWithOwnerInfo that = (PetWithOwnerInfo) o;
        return Objects.equals(petId, that.petId) &&
               Objects.equals(ownerId, that.ownerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(petId, ownerId);
    }
    
    @Override
    public String toString() {
        return "PetWithOwnerInfo{" +
                "petId=" + petId +
                ", petName='" + petName + '\'' +
                ", petSpecies='" + petSpecies + '\'' +
                ", ownerFullName='" + getOwnerFullName() + '\'' +
                ", ownerContactInfo='" + getOwnerContactInfo() + '\'' +
                '}';
    }
}