package com.petclinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * DTO for owner validation requests
 * Contains owner data to be validated against business rules
 * Supports international postal code formats for global users
 * Validates: Requirements 1.1, 1.2, 1.3, 3.5
 */
public class OwnerValidationRequest {
    
    @JsonProperty("firstName")
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;
    
    @JsonProperty("lastName")
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;
    
    @JsonProperty("address")
    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;
    
    @JsonProperty("city")
    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;
    
    @JsonProperty("state")
    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;
    
    @JsonProperty("zipCode")
    @Pattern(regexp = "^$|^[A-Z0-9\\s-]{3,10}$", message = "Invalid postal code format. Please use only letters, numbers, spaces, and hyphens (3-10 characters). Examples: 12345 (US), K1A 0A6 (Canada), SW1A 1AA (UK), 110001 (India)")
    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    private String zipCode;
    
    @JsonProperty("telephone")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)\\.]{7,20}$", message = "Invalid telephone format")
    private String telephone;
    
    @JsonProperty("mobileNumber")
    private String mobileNumber;
    
    @JsonProperty("email")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    // Constructors
    public OwnerValidationRequest() {}
    
    public OwnerValidationRequest(String firstName, String lastName, String email, String mobileNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNumber = mobileNumber;
    }
    
    public OwnerValidationRequest(String firstName, String lastName, String address, String city, 
                                String state, String zipCode, String telephone, String mobileNumber, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.telephone = telephone;
        this.mobileNumber = mobileNumber;
        this.email = email;
    }
    
    // Getters and Setters
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    
    public String getMobileNumber() {
        return mobileNumber;
    }
    
    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    // Business Methods
    @JsonIgnore
    public boolean hasRequiredFields() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty();
    }
    
    @JsonIgnore
    public boolean hasMobileNumber() {
        return mobileNumber != null && !mobileNumber.trim().isEmpty();
    }
    
    @JsonIgnore
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }
    
    @JsonIgnore
    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }
    
    @JsonIgnore
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OwnerValidationRequest that = (OwnerValidationRequest) o;
        return Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(email, that.email) &&
               Objects.equals(mobileNumber, that.mobileNumber) &&
               Objects.equals(address, that.address) &&
               Objects.equals(city, that.city) &&
               Objects.equals(state, that.state) &&
               Objects.equals(zipCode, that.zipCode) &&
               Objects.equals(telephone, that.telephone);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, mobileNumber, address, city, state, zipCode, telephone);
    }
    
    @Override
    public String toString() {
        return "OwnerValidationRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", mobileNumber='" + (mobileNumber != null ? "[REDACTED]" : null) + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +
                '}';
    }
}