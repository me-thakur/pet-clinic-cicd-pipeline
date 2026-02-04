package com.petclinic.backend.service.impl;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.petclinic.backend.dto.FormatValidationResult;
import com.petclinic.backend.service.FormatValidatorService;
import com.petclinic.backend.service.InternationalValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Implementation of FormatValidatorService for validating data formats
 * Uses Google libphonenumber for mobile number validation and standard patterns for other formats
 * Validates: Requirements 1.1, 2.1, 2.5
 */
@Service
public class FormatValidatorServiceImpl implements FormatValidatorService {
    
    private final PhoneNumberUtil phoneNumberUtil;
    private final InternationalValidationService internationalValidationService;
    
    // Email validation pattern based on RFC 5322
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    // Name validation pattern (letters, spaces, hyphens, apostrophes)
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z\\s\\-']{1,50}$"
    );
    
    @Autowired
    public FormatValidatorServiceImpl(InternationalValidationService internationalValidationService) {
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
        this.internationalValidationService = internationalValidationService;
    }
    
    @Override
    public FormatValidationResult validateMobileNumber(String mobileNumber) {
        return validateMobileNumber(mobileNumber, null);
    }
    
    @Override
    public FormatValidationResult validateMobileNumber(String mobileNumber, String countryCode) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return FormatValidationResult.invalid(
                "Mobile number is required",
                "Please provide a valid mobile number in international format",
                mobileNumber,
                "+1-555-123-4567 (US), +44-7911-123456 (UK), +81-90-1234-5678 (Japan)",
                "MOBILE_REQUIRED"
            );
        }
        
        String trimmedNumber = mobileNumber.trim();
        
        try {
            // Parse the phone number
            Phonenumber.PhoneNumber phoneNumber;
            if (countryCode != null && !countryCode.trim().isEmpty()) {
                phoneNumber = phoneNumberUtil.parse(trimmedNumber, countryCode.toUpperCase());
            } else {
                // Try to parse without country code (number should include country code)
                phoneNumber = phoneNumberUtil.parse(trimmedNumber, null);
            }
            
            // Check if it's a valid number
            if (!phoneNumberUtil.isValidNumber(phoneNumber)) {
                return FormatValidationResult.invalid(
                    "Invalid mobile number format",
                    "Please provide a valid mobile number with country code. Ensure the number starts with + followed by country code",
                    trimmedNumber,
                    "+1-555-123-4567 (US), +44-7911-123456 (UK), +49-151-12345678 (Germany)",
                    "MOBILE_INVALID_FORMAT"
                );
            }
            
            // Check if it's a mobile number type
            PhoneNumberUtil.PhoneNumberType numberType = phoneNumberUtil.getNumberType(phoneNumber);
            if (numberType != PhoneNumberUtil.PhoneNumberType.MOBILE && 
                numberType != PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE) {
                return FormatValidationResult.invalid(
                    "Number is not a mobile phone number",
                    "Please provide a mobile phone number, not a landline. Mobile numbers typically start with specific prefixes in each country",
                    trimmedNumber,
                    "+1-555-123-4567 (US mobile), +44-7911-123456 (UK mobile), +49-151-12345678 (German mobile)",
                    "MOBILE_NOT_MOBILE_TYPE"
                );
            }
            
            return FormatValidationResult.valid();
            
        } catch (NumberParseException e) {
            String errorMessage;
            String guidance;
            String example;
            String errorCode;
            
            switch (e.getErrorType()) {
                case INVALID_COUNTRY_CODE:
                    errorMessage = "Invalid country code in mobile number";
                    guidance = "Please use a valid country code. Common codes: +1 (US/Canada), +44 (UK), +49 (Germany), +33 (France), +81 (Japan)";
                    example = "+1-555-123-4567, +44-7911-123456, +49-151-12345678";
                    errorCode = "MOBILE_INVALID_COUNTRY_CODE";
                    break;
                case NOT_A_NUMBER:
                    errorMessage = "Mobile number contains invalid characters";
                    guidance = "Please use only numbers, spaces, hyphens, parentheses, and the + symbol for country code";
                    example = "+1-555-123-4567, +1 (555) 123-4567, +44 7911 123456";
                    errorCode = "MOBILE_INVALID_CHARACTERS";
                    break;
                case TOO_SHORT_NSN:
                    errorMessage = "Mobile number is too short";
                    guidance = "Please provide the complete mobile number including country code and area code. Most mobile numbers are 10-15 digits total";
                    example = "+1-555-123-4567 (complete), not +1-555-123 (incomplete)";
                    errorCode = "MOBILE_TOO_SHORT";
                    break;
                case TOO_LONG:
                    errorMessage = "Mobile number is too long";
                    guidance = "Please check the mobile number format. Remove any extra digits or characters";
                    example = "+1-555-123-4567 (correct length), not +1-555-123-4567890 (too long)";
                    errorCode = "MOBILE_TOO_LONG";
                    break;
                default:
                    errorMessage = "Invalid mobile number format";
                    guidance = "Please provide a valid international mobile number starting with country code";
                    example = "+1-555-123-4567 (US), +44-7911-123456 (UK), +81-90-1234-5678 (Japan)";
                    errorCode = "MOBILE_PARSE_ERROR";
                    break;
            }
            
            return FormatValidationResult.invalid(
                errorMessage,
                guidance,
                trimmedNumber,
                example,
                errorCode
            );
        }
    }
    
    @Override
    public FormatValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return FormatValidationResult.invalid(
                "Email address is required",
                "Please provide a valid email address in the format: username@domain.com",
                email,
                "user@example.com, firstname.lastname@company.org",
                "EMAIL_REQUIRED"
            );
        }
        
        String trimmedEmail = email.trim();
        
        // Check length
        if (trimmedEmail.length() > 100) {
            return FormatValidationResult.invalid(
                "Email address is too long (maximum 100 characters)",
                "Please use a shorter email address. Consider using a shorter username or domain name",
                trimmedEmail,
                "user@example.com (shorter alternative)",
                "EMAIL_TOO_LONG"
            );
        }
        
        // Check format using regex
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return FormatValidationResult.invalid(
                "Invalid email address format",
                "Please provide a valid email address with exactly one @ symbol and a valid domain (e.g., .com, .org, .net)",
                trimmedEmail,
                "user@example.com, firstname.lastname@company.org, user+tag@domain.co.uk",
                "EMAIL_INVALID_FORMAT"
            );
        }
        
        return FormatValidationResult.valid();
    }
    
    @Override
    public FormatValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return FormatValidationResult.invalid(
                "Name is required",
                "Please provide a valid name using letters, spaces, hyphens, and apostrophes only",
                name,
                "John, Mary-Jane, O'Connor, José, François",
                "NAME_REQUIRED"
            );
        }
        
        String trimmedName = name.trim();
        
        // Check length
        if (trimmedName.length() > 50) {
            return FormatValidationResult.invalid(
                "Name is too long (maximum 50 characters)",
                "Please use a shorter name. Consider using initials or abbreviations if necessary",
                trimmedName,
                "John Smith (shorter), not John Christopher Alexander Smith (too long)",
                "NAME_TOO_LONG"
            );
        }
        
        // Check format using regex
        if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            return FormatValidationResult.invalid(
                "Invalid name format",
                "Please use only letters, spaces, hyphens, and apostrophes. Remove any numbers or special characters",
                trimmedName,
                "John, Mary-Jane, O'Connor, José (valid) vs John123, Mary@Jane (invalid)",
                "NAME_INVALID_FORMAT"
            );
        }
        
        return FormatValidationResult.valid();
    }
    
    @Override
    public FormatValidationResult validatePostalCode(String postalCode) {
        return internationalValidationService.validatePostalCodeFlexible(postalCode);
    }
    
    @Override
    public FormatValidationResult validatePostalCode(String postalCode, String countryCode) {
        return internationalValidationService.validatePostalCode(postalCode, countryCode);
    }
}