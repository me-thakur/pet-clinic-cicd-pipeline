package com.petclinic.frontend.model;

/**
 * Simple model for veterinarian dropdown options
 */
public class VeterinarianOption {
    
    private Long id;
    private String firstName;
    private String lastName;
    
    // Constructors
    public VeterinarianOption() {}
    
    public VeterinarianOption(Long id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    @Override
    public String toString() {
        return getFullName();
    }
}