/**
 * Secure Form Validation Utility
 * Provides client-side validation with security best practices
 * 
 * Validates: Requirements 17.3, 17.4
 */
class SecureFormValidator {
    constructor() {
        this.patterns = {
            email: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
            phone: /^[+]?[1-9]?[0-9]{7,15}$/,
            alphanumeric: /^[a-zA-Z0-9\s]+$/,
            name: /^[a-zA-Z\s'-]{1,50}$/,
            postalCode: /^[a-zA-Z0-9\s-]{3,10}$/,
            safeText: /^[a-zA-Z0-9\s.,!?'-]{1,500}$/
        };
        
        this.maliciousPatterns = [
            /<script[^>]*>.*?<\/script>/gi,
            /javascript:/gi,
            /on\w+\s*=/gi,
            /<iframe/gi,
            /<object/gi,
            /<embed/gi,
            /eval\s*\(/gi,
            /expression\s*\(/gi,
            /vbscript:/gi,
            /data:text\/html/gi
        ];
        
        this.init();
    }
    
    /**
     * Initialize the validator
     */
    init() {
        this.setupFormValidation();
        this.setupRealTimeValidation();
    }
    
    /**
     * Setup form validation on submit
     */
    setupFormValidation() {
        document.addEventListener('submit', (event) => {
            const form = event.target;
            if (form.tagName === 'FORM' && !this.validateForm(form)) {
                event.preventDefault();
                event.stopPropagation();
            }
        });
    }
    
    /**
     * Setup real-time validation on input
     */
    setupRealTimeValidation() {
        document.addEventListener('input', (event) => {
            const input = event.target;
            if (input.tagName === 'INPUT' || input.tagName === 'TEXTAREA') {
                this.validateField(input);
            }
        });
        
        document.addEventListener('blur', (event) => {
            const input = event.target;
            if (input.tagName === 'INPUT' || input.tagName === 'TEXTAREA') {
                this.validateField(input, true);
            }
        });
    }
    
    /**
     * Validate an entire form
     */
    validateForm(form) {
        let isValid = true;
        const inputs = form.querySelectorAll('input, textarea, select');
        
        inputs.forEach(input => {
            if (!this.validateField(input, true)) {
                isValid = false;
            }
        });
        
        return isValid;
    }
    
    /**
     * Validate a single field
     */
    validateField(field, showErrors = false) {
        const value = field.value;
        const fieldType = field.type || field.tagName.toLowerCase();
        const fieldName = field.name || field.id || 'field';
        
        // Clear previous validation state
        this.clearFieldErrors(field);
        
        let isValid = true;
        let errorMessage = '';
        
        // Check for malicious content first
        if (this.containsMaliciousContent(value)) {
            isValid = false;
            errorMessage = 'Invalid characters detected. Please remove any script tags or special characters.';
        }
        // Check required fields
        else if (field.required && (!value || value.trim() === '')) {
            isValid = false;
            errorMessage = `${this.getFieldDisplayName(fieldName)} is required.`;
        }
        // Validate based on field type
        else if (value && value.trim() !== '') {
            const validation = this.validateByType(fieldType, value, field);
            isValid = validation.isValid;
            errorMessage = validation.message;
        }
        
        // Apply validation state
        if (isValid) {
            this.markFieldValid(field);
        } else {
            this.markFieldInvalid(field, showErrors ? errorMessage : '');
        }
        
        return isValid;
    }
    
    /**
     * Validate field based on its type
     */
    validateByType(fieldType, value, field) {
        switch (fieldType) {
            case 'email':
                return this.validateEmail(value);
            case 'tel':
            case 'phone':
                return this.validatePhone(value);
            case 'text':
                return this.validateText(value, field);
            case 'textarea':
                return this.validateTextArea(value);
            case 'number':
                return this.validateNumber(value, field);
            case 'url':
                return this.validateUrl(value);
            default:
                return this.validateGeneric(value);
        }
    }
    
    /**
     * Validate email address
     */
    validateEmail(email) {
        if (!this.patterns.email.test(email)) {
            return { isValid: false, message: 'Please enter a valid email address.' };
        }
        
        if (email.length > 254) {
            return { isValid: false, message: 'Email address is too long.' };
        }
        
        return { isValid: true, message: '' };
    }
    
    /**
     * Validate phone number
     */
    validatePhone(phone) {
        // Remove formatting characters
        const cleanPhone = phone.replace(/[\s\-\(\)\.]/g, '');
        
        if (!this.patterns.phone.test(cleanPhone)) {
            return { isValid: false, message: 'Please enter a valid phone number.' };
        }
        
        return { isValid: true, message: '' };
    }
    
    /**
     * Validate text input
     */
    validateText(text, field) {
        const fieldName = field.name || field.id || '';
        
        // Validate based on field name/purpose
        if (fieldName.includes('name') || fieldName.includes('Name')) {
            if (!this.patterns.name.test(text)) {
                return { isValid: false, message: 'Name can only contain letters, spaces, hyphens, and apostrophes.' };
            }
        } else if (fieldName.includes('postal') || fieldName.includes('zip')) {
            if (!this.patterns.postalCode.test(text)) {
                return { isValid: false, message: 'Please enter a valid postal code.' };
            }
        } else {
            // Generic text validation
            if (!this.patterns.safeText.test(text)) {
                return { isValid: false, message: 'Text contains invalid characters.' };
            }
        }
        
        // Check length limits
        const maxLength = field.maxLength || 255;
        if (text.length > maxLength) {
            return { isValid: false, message: `Text is too long (maximum ${maxLength} characters).` };
        }
        
        return { isValid: true, message: '' };
    }
    
    /**
     * Validate textarea content
     */
    validateTextArea(text) {
        if (text.length > 2000) {
            return { isValid: false, message: 'Text is too long (maximum 2000 characters).' };
        }
        
        // Allow more characters in textarea but still check for malicious content
        const safeTextAreaPattern = /^[a-zA-Z0-9\s.,!?'"\-\n\r(){}[\]@#$%&*+=:;/\\]{1,2000}$/;
        if (!safeTextAreaPattern.test(text)) {
            return { isValid: false, message: 'Text contains invalid characters.' };
        }
        
        return { isValid: true, message: '' };
    }
    
    /**
     * Validate numeric input
     */
    validateNumber(value, field) {
        const num = parseFloat(value);
        
        if (isNaN(num)) {
            return { isValid: false, message: 'Please enter a valid number.' };
        }
        
        const min = field.min ? parseFloat(field.min) : null;
        const max = field.max ? parseFloat(field.max) : null;
        
        if (min !== null && num < min) {
            return { isValid: false, message: `Number must be at least ${min}.` };
        }
        
        if (max !== null && num > max) {
            return { isValid: false, message: `Number must be no more than ${max}.` };
        }
        
        return { isValid: true, message: '' };
    }
    
    /**
     * Validate URL
     */
    validateUrl(url) {
        try {
            new URL(url);
            
            // Only allow http and https protocols
            if (!url.startsWith('http://') && !url.startsWith('https://')) {
                return { isValid: false, message: 'URL must start with http:// or https://' };
            }
            
            return { isValid: true, message: '' };
        } catch {
            return { isValid: false, message: 'Please enter a valid URL.' };
        }
    }
    
    /**
     * Generic validation for unknown field types
     */
    validateGeneric(value) {
        if (value.length > 500) {
            return { isValid: false, message: 'Input is too long.' };
        }
        
        return { isValid: true, message: '' };
    }
    
    /**
     * Check if content contains malicious patterns
     */
    containsMaliciousContent(content) {
        if (!content) return false;
        
        return this.maliciousPatterns.some(pattern => pattern.test(content));
    }
    
    /**
     * Sanitize input value
     */
    sanitizeInput(value) {
        if (!value) return value;
        
        // Remove null bytes
        let sanitized = value.replace(/\0/g, '');
        
        // Escape HTML entities
        sanitized = sanitized
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#x27;');
        
        return sanitized;
    }
    
    /**
     * Mark field as valid
     */
    markFieldValid(field) {
        field.classList.remove('is-invalid', 'error');
        field.classList.add('is-valid', 'valid');
        
        // Update Bootstrap validation classes
        const feedback = field.parentNode.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.style.display = 'none';
        }
    }
    
    /**
     * Mark field as invalid
     */
    markFieldInvalid(field, message) {
        field.classList.remove('is-valid', 'valid');
        field.classList.add('is-invalid', 'error');
        
        // Show error message
        if (message) {
            this.showFieldError(field, message);
        }
    }
    
    /**
     * Clear field validation state
     */
    clearFieldErrors(field) {
        field.classList.remove('is-valid', 'is-invalid', 'valid', 'error');
        
        const feedback = field.parentNode.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.style.display = 'none';
        }
    }
    
    /**
     * Show field error message
     */
    showFieldError(field, message) {
        let feedback = field.parentNode.querySelector('.invalid-feedback');
        
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            field.parentNode.appendChild(feedback);
        }
        
        feedback.textContent = message;
        feedback.style.display = 'block';
    }
    
    /**
     * Get display name for field
     */
    getFieldDisplayName(fieldName) {
        const displayNames = {
            'firstName': 'First Name',
            'lastName': 'Last Name',
            'email': 'Email',
            'phone': 'Phone Number',
            'address': 'Address',
            'city': 'City',
            'postalCode': 'Postal Code',
            'notes': 'Notes'
        };
        
        return displayNames[fieldName] || fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
    }
    
    /**
     * Manually validate a form and return results
     */
    getFormValidationResults(form) {
        const results = {
            isValid: true,
            errors: [],
            fields: {}
        };
        
        const inputs = form.querySelectorAll('input, textarea, select');
        
        inputs.forEach(input => {
            const fieldName = input.name || input.id;
            const isFieldValid = this.validateField(input, true);
            
            results.fields[fieldName] = isFieldValid;
            
            if (!isFieldValid) {
                results.isValid = false;
                results.errors.push({
                    field: fieldName,
                    message: input.parentNode.querySelector('.invalid-feedback')?.textContent || 'Invalid input'
                });
            }
        });
        
        return results;
    }
}

// Initialize secure form validation when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.secureFormValidator = new SecureFormValidator();
});

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SecureFormValidator;
}