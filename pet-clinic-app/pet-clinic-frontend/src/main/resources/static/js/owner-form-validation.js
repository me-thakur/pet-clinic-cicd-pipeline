/**
 * Owner Form Validation with International Postal Code Support
 * Provides client-side validation with enhanced error display and format examples
 * Validates: Requirements 3.5, 4.1, 4.2, 4.3, 4.4, 4.5
 */

class OwnerFormValidator {
    constructor() {
        this.validationEndpoint = '/api/validation/owners';
        this.fieldValidationCache = new Map();
        this.validationTimeout = null;
        this.validationDelay = 500; // ms delay for debounced validation
        
        // International postal code patterns for client-side validation
        this.postalCodePatterns = {
            'US': {
                pattern: /^\d{5}(-\d{4})?$/,
                example: '12345 or 12345-6789',
                name: 'United States'
            },
            'CA': {
                pattern: /^[A-Z]\d[A-Z]\s?\d[A-Z]\d$/i,
                example: 'K1A 0A6 or K1A0A6',
                name: 'Canada'
            },
            'UK': {
                pattern: /^[A-Z]{1,2}\d[A-Z\d]?\s?\d[A-Z]{2}$/i,
                example: 'SW1A 1AA or M1 1AA',
                name: 'United Kingdom'
            },
            'DE': {
                pattern: /^\d{5}$/,
                example: '12345',
                name: 'Germany'
            },
            'FR': {
                pattern: /^\d{5}$/,
                example: '75001',
                name: 'France'
            },
            'IN': {
                pattern: /^\d{6}$/,
                example: '110001',
                name: 'India'
            },
            'AU': {
                pattern: /^\d{4}$/,
                example: '2000',
                name: 'Australia'
            },
            'JP': {
                pattern: /^\d{3}-\d{4}$/,
                example: '100-0001',
                name: 'Japan'
            }
        };
        
        // Flexible pattern for unknown formats
        this.flexiblePattern = /^[A-Z0-9\s-]{3,10}$/i;
        
        this.init();
    }
    
    init() {
        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.setupValidation());
        } else {
            this.setupValidation();
        }
    }
    
    setupValidation() {
        const form = document.querySelector('form[th\\:object="${owner}"], form[action*="/owners"]');
        if (!form) {
            console.warn('Owner form not found, validation setup skipped');
            return;
        }
        
        console.log('Setting up owner form validation');
        
        // Add service status indicator
        this.addServiceStatusIndicator(form);
        
        // Setup field validation listeners
        this.setupFieldValidation(form);
        
        // Setup form submission validation
        this.setupFormSubmissionValidation(form);
        
        // Setup postal code specific validation
        this.setupPostalCodeValidation(form);
        
        // Add validation styles
        this.addValidationStyles();
        
        // Check initial service status
        this.checkServiceStatus();
    }
    
    /**
     * Add service status indicator to the form
     */
    addServiceStatusIndicator(form) {
        // Check if indicator already exists
        if (document.querySelector('.validation-service-status')) return;
        
        const statusIndicator = document.createElement('div');
        statusIndicator.className = 'validation-service-status alert alert-info d-flex align-items-center';
        statusIndicator.style.cssText = 'margin-bottom: 1rem; padding: 0.5rem 0.75rem; font-size: 0.875rem;';
        statusIndicator.innerHTML = `
            <div class="spinner-border spinner-border-sm me-2" role="status" style="width: 1rem; height: 1rem;">
                <span class="visually-hidden">Loading...</span>
            </div>
            <span class="status-text">Checking validation service status...</span>
        `;
        
        // Insert at the top of the form
        form.insertBefore(statusIndicator, form.firstChild);
    }
    
    /**
     * Check validation service status and update indicator
     */
    async checkServiceStatus() {
        const statusIndicator = document.querySelector('.validation-service-status');
        if (!statusIndicator) return;
        
        try {
            const response = await fetch(`${this.validationEndpoint}/health`, {
                method: 'GET',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            
            const statusText = statusIndicator.querySelector('.status-text');
            const spinner = statusIndicator.querySelector('.spinner-border');
            
            if (response.ok) {
                const healthData = await response.json();
                
                // Service is available
                statusIndicator.className = 'validation-service-status alert alert-success d-flex align-items-center';
                spinner.innerHTML = '<i class="bi bi-check-circle-fill"></i>';
                spinner.className = 'me-2 text-success';
                statusText.textContent = 'Full validation service is available';
                
                // Auto-hide success status after 3 seconds
                setTimeout(() => {
                    if (statusIndicator.parentNode) {
                        statusIndicator.style.transition = 'opacity 0.3s ease-out';
                        statusIndicator.style.opacity = '0';
                        setTimeout(() => statusIndicator.remove(), 300);
                    }
                }, 3000);
                
            } else if (response.status === 503) {
                const healthData = await response.json();
                
                // Service is unavailable
                statusIndicator.className = 'validation-service-status alert alert-warning d-flex align-items-center';
                spinner.innerHTML = '<i class="bi bi-exclamation-triangle-fill"></i>';
                spinner.className = 'me-2 text-warning';
                statusText.innerHTML = `
                    Validation service is temporarily unavailable. Using basic validation mode.
                    <button type="button" class="btn btn-sm btn-outline-warning ms-2" onclick="window.ownerFormValidator.checkServiceStatus()">
                        <i class="bi bi-arrow-clockwise"></i> Retry
                    </button>
                `;
            }
            
        } catch (error) {
            console.warn('Failed to check service status:', error);
            
            const statusText = statusIndicator.querySelector('.status-text');
            const spinner = statusIndicator.querySelector('.spinner-border');
            
            // Connection error
            statusIndicator.className = 'validation-service-status alert alert-warning d-flex align-items-center';
            spinner.innerHTML = '<i class="bi bi-wifi-off"></i>';
            spinner.className = 'me-2 text-warning';
            statusText.innerHTML = `
                Unable to connect to validation service. Using offline validation mode.
                <button type="button" class="btn btn-sm btn-outline-warning ms-2" onclick="window.ownerFormValidator.checkServiceStatus()">
                    <i class="bi bi-arrow-clockwise"></i> Retry
                </button>
            `;
        }
    }
    
    setupFieldValidation(form) {
        const fields = form.querySelectorAll('input, textarea, select');
        
        fields.forEach(field => {
            // Add real-time validation on blur and input
            field.addEventListener('blur', (e) => this.validateField(e.target));
            field.addEventListener('input', (e) => this.debouncedValidateField(e.target));
            
            // Clear errors when user starts typing (immediate error clearing)
            field.addEventListener('focus', (e) => this.onFieldFocus(e.target));
            field.addEventListener('input', (e) => this.onFieldInput(e.target));
            
            // Track field state for better UX
            field.addEventListener('change', (e) => this.onFieldChange(e.target));
        });
    }
    
    onFieldFocus(field) {
        // Mark field as focused for better error handling
        field.dataset.focused = 'true';
        
        // Don't clear errors immediately on focus, wait for input
    }
    
    onFieldInput(field) {
        // If field was invalid and user is typing, clear error immediately
        if (field.classList.contains('is-invalid')) {
            field.dataset.wasInvalid = 'true';
            this.clearFieldError(field);
        }
        
        // Debounced validation will run after user stops typing
        this.debouncedValidateField(field);
    }
    
    onFieldChange(field) {
        // Field has been changed, validate it
        this.validateField(field);
        delete field.dataset.focused;
    }
    
    setupFormSubmissionValidation(form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const isValid = await this.validateForm(form);
            if (isValid) {
                // Allow form submission
                form.removeEventListener('submit', arguments.callee);
                form.submit();
            }
        });
    }
    
    setupPostalCodeValidation(form) {
        const zipCodeField = form.querySelector('#zipCode, input[name="zipCode"]');
        if (!zipCodeField) return;
        
        // Add format hint
        this.addFormatHint(zipCodeField);
        
        // Enhanced postal code validation
        zipCodeField.addEventListener('input', (e) => {
            this.validatePostalCodeFormat(e.target);
        });
        
        zipCodeField.addEventListener('blur', (e) => {
            this.validatePostalCodeFormat(e.target, true);
        });
    }
    
    addFormatHint(zipCodeField) {
        const existingHint = zipCodeField.parentNode.querySelector('.postal-code-hint');
        if (existingHint) return;
        
        const hint = document.createElement('div');
        hint.className = 'form-text postal-code-hint';
        hint.innerHTML = `
            <small class="text-muted">
                <i class="fas fa-info-circle"></i> 
                Supports international formats: US (12345), Canada (K1A 0A6), UK (SW1A 1AA), etc.
                <a href="#" class="postal-examples-link" onclick="event.preventDefault(); this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'block' : 'none';">
                    View examples
                </a>
                <div class="postal-examples" style="display: none; margin-top: 5px; padding: 8px; background: #f8f9fa; border-radius: 4px; font-size: 0.85em;">
                    ${this.getPostalCodeExamples()}
                </div>
            </small>
        `;
        
        zipCodeField.parentNode.appendChild(hint);
    }
    
    getPostalCodeExamples() {
        const examples = Object.entries(this.postalCodePatterns)
            .map(([code, info]) => `<strong>${info.name}:</strong> ${info.example}`)
            .join('<br>');
        return examples;
    }
    
    debouncedValidateField(field) {
        clearTimeout(this.validationTimeout);
        this.validationTimeout = setTimeout(() => {
            this.validateField(field);
        }, this.validationDelay);
    }
    
    async validateField(field) {
        if (!field.name || field.name === '_csrf') return;
        
        // Clear previous error
        this.clearFieldError(field);
        
        // Skip validation for empty optional fields
        if (!field.required && (!field.value || field.value.trim() === '')) {
            return true;
        }
        
        // Client-side validation first
        const clientValidation = this.validateFieldClientSide(field);
        if (!clientValidation.valid) {
            this.displayFieldError(field, clientValidation.error);
            return false;
        }
        
        // Server-side validation for critical fields
        if (this.shouldValidateServerSide(field)) {
            try {
                const serverValidation = await this.validateFieldServerSide(field);
                if (!serverValidation.valid) {
                    this.displayFieldError(field, serverValidation.error);
                    return false;
                }
            } catch (error) {
                console.warn('Server-side validation failed:', error);
                // Continue with client-side validation only
            }
        }
        
        return true;
    }
    
    validateFieldClientSide(field) {
        const value = field.value.trim();
        const fieldName = field.name;
        
        // Required field validation
        if (field.required && !value) {
            return {
                valid: false,
                error: {
                    fieldName: fieldName,
                    errorMessage: `${this.getFieldDisplayName(fieldName)} is required`,
                    correctionGuidance: `Please enter a ${this.getFieldDisplayName(fieldName).toLowerCase()}`,
                    errorCode: 'REQUIRED_FIELD'
                }
            };
        }
        
        // Field-specific validation
        switch (fieldName) {
            case 'zipCode':
                return this.validatePostalCodeClientSide(value);
            case 'email':
                return this.validateEmailClientSide(value);
            case 'telephone':
            case 'mobileNumber':
                return this.validatePhoneClientSide(value);
            case 'state':
                return this.validateStateClientSide(value);
            default:
                return { valid: true };
        }
    }
    
    validatePostalCodeClientSide(value) {
        if (!value) return { valid: true }; // Optional field
        
        // Check length constraints
        if (value.length < 3) {
            return {
                valid: false,
                error: {
                    fieldName: 'zipCode',
                    errorMessage: 'Postal code is too short',
                    correctionGuidance: 'Please provide a postal code with at least 3 characters',
                    formatExample: this.getPostalCodeExamples(),
                    errorCode: 'POSTAL_CODE_TOO_SHORT'
                }
            };
        }
        
        if (value.length > 10) {
            return {
                valid: false,
                error: {
                    fieldName: 'zipCode',
                    errorMessage: 'Postal code is too long',
                    correctionGuidance: 'Please provide a postal code with at most 10 characters',
                    formatExample: this.getPostalCodeExamples(),
                    errorCode: 'POSTAL_CODE_TOO_LONG'
                }
            };
        }
        
        // Try to match against known patterns
        const detectedCountry = this.detectCountryFromPostalCode(value);
        if (detectedCountry) {
            const pattern = this.postalCodePatterns[detectedCountry];
            if (!pattern.pattern.test(value)) {
                return {
                    valid: false,
                    error: {
                        fieldName: 'zipCode',
                        errorMessage: `Invalid postal code format for ${pattern.name}`,
                        correctionGuidance: `Please provide a valid ${pattern.name} postal code`,
                        formatExample: `Expected format: ${pattern.example}`,
                        errorCode: `POSTAL_CODE_INVALID_FORMAT_${detectedCountry}`
                    }
                };
            }
        } else {
            // Use flexible validation
            if (!this.flexiblePattern.test(value)) {
                return {
                    valid: false,
                    error: {
                        fieldName: 'zipCode',
                        errorMessage: 'Invalid postal code format',
                        correctionGuidance: 'Please use only letters, numbers, spaces, and hyphens',
                        formatExample: this.getPostalCodeExamples(),
                        errorCode: 'POSTAL_CODE_INVALID_CHARACTERS'
                    }
                };
            }
        }
        
        return { valid: true };
    }
    
    validateEmailClientSide(value) {
        if (!value) return { valid: true }; // Optional field
        
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(value)) {
            return {
                valid: false,
                error: {
                    fieldName: 'email',
                    errorMessage: 'Invalid email format',
                    correctionGuidance: 'Please enter a valid email address',
                    formatExample: 'example@domain.com',
                    errorCode: 'EMAIL_INVALID_FORMAT'
                }
            };
        }
        
        return { valid: true };
    }
    
    validatePhoneClientSide(value) {
        if (!value) return { valid: true }; // Optional field
        
        // Basic phone validation - allow various formats
        const phonePattern = /^[\d\s\-\+\(\)\.]{7,20}$/;
        if (!phonePattern.test(value)) {
            return {
                valid: false,
                error: {
                    fieldName: 'telephone',
                    errorMessage: 'Invalid phone number format',
                    correctionGuidance: 'Please enter a valid phone number',
                    formatExample: '(555) 123-4567 or +1-555-123-4567',
                    errorCode: 'PHONE_INVALID_FORMAT'
                }
            };
        }
        
        return { valid: true };
    }
    
    validateStateClientSide(value) {
        if (!value) return { valid: true }; // Optional field
        
        if (value.length > 2) {
            return {
                valid: false,
                error: {
                    fieldName: 'state',
                    errorMessage: 'State code is too long',
                    correctionGuidance: 'Please use a 2-letter state code',
                    formatExample: 'CA, NY, TX',
                    errorCode: 'STATE_CODE_TOO_LONG'
                }
            };
        }
        
        return { valid: true };
    }
    
    detectCountryFromPostalCode(postalCode) {
        const trimmed = postalCode.trim();
        
        // Check for unique patterns
        if (/^\d{6}$/.test(trimmed)) return 'IN'; // India
        if (/^\d{4}$/.test(trimmed)) return 'AU'; // Australia
        if (/^[A-Z]\d[A-Z]/i.test(trimmed)) return 'CA'; // Canada
        if (/^[A-Z]{1,2}\d/i.test(trimmed)) return 'UK'; // UK
        if (/^\d{3}-\d{4}$/.test(trimmed)) return 'JP'; // Japan
        
        // For ambiguous patterns, don't guess
        return null;
    }
    
    shouldValidateServerSide(field) {
        // Validate server-side for fields that need uniqueness checks
        return ['email', 'mobileNumber'].includes(field.name);
    }
    
    async validateFieldServerSide(field) {
        const formData = this.getFormData();
        const fieldName = field.name;
        
        try {
            // Check service health first
            const healthResponse = await fetch(`${this.validationEndpoint}/health`, {
                method: 'GET',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            
            // If service is unavailable (503), immediately use fallback
            if (healthResponse.status === 503) {
                const healthData = await healthResponse.json();
                console.warn('Validation service unavailable:', healthData.status);
                return this.handleServiceUnavailable(field, 'SERVICE_UNAVAILABLE', healthData.status);
            }
            
            const response = await fetch(`${this.validationEndpoint}/validate-fields?fields=${fieldName}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(formData)
            });
            
            if (!response.ok) {
                if (response.status === 503) {
                    // Service degraded
                    const errorData = await response.json();
                    return this.handleServiceUnavailable(field, 'SERVICE_DEGRADED', errorData.message);
                }
                throw new Error(`Server validation failed: ${response.status}`);
            }
            
            const result = await response.json();
            
            // Check for degraded validation warning (202 Accepted)
            if (response.status === 202 || result.warning || result.warningMessage) {
                this.showValidationWarning(
                    result.warningMessage || 
                    'Validation service is operating in reduced mode. Basic validation performed.',
                    'warning'
                );
            }
            
            // Check for successful validation with metadata
            if (result.valid) {
                // Clear any previous warnings for this field
                this.clearFieldWarning(field);
                
                // Show success feedback if configured
                if (result.metadata && result.metadata.validationMode === 'FULL') {
                    this.showFieldSuccess(field, 'Full validation passed');
                }
                
                return { valid: true, metadata: result.metadata };
            }
            
            // Handle validation errors
            if (!result.valid && result.fieldErrors) {
                const fieldError = result.fieldErrors.find(error => error.fieldName === fieldName);
                if (fieldError) {
                    return {
                        valid: false,
                        error: fieldError,
                        metadata: result.metadata
                    };
                }
            }
            
            return { valid: true };
            
        } catch (error) {
            console.warn('Server-side validation error, using client-side validation:', error);
            return this.handleServiceUnavailable(field, 'CONNECTION_ERROR', error.message);
        }
    }
    
    /**
     * Handle service unavailable scenarios with graceful degradation
     */
    handleServiceUnavailable(field, reason, details) {
        // Perform client-side validation as fallback
        const clientValidation = this.validateFieldClientSide(field);
        
        // Show appropriate warning based on reason
        let warningMessage;
        let warningType = 'warning';
        
        switch (reason) {
            case 'SERVICE_UNAVAILABLE':
                warningMessage = 'Validation service is temporarily unavailable. Basic validation is being performed. Please retry later for full validation.';
                warningType = 'warning';
                break;
            case 'SERVICE_DEGRADED':
                warningMessage = 'Validation service is operating in reduced mode. Some validation checks may be limited.';
                warningType = 'info';
                break;
            case 'CONNECTION_ERROR':
                warningMessage = 'Unable to connect to validation service. Using offline validation. Please check your connection and retry.';
                warningType = 'warning';
                break;
            default:
                warningMessage = 'Full validation temporarily unavailable. Basic validation performed.';
                warningType = 'warning';
        }
        
        // Show warning with specific guidance
        this.showValidationWarning(warningMessage, warningType);
        
        // Add field-specific warning indicator
        this.showFieldWarning(field, 'Limited validation - ' + reason.toLowerCase().replace('_', ' '));
        
        // Add metadata to indicate fallback mode
        if (clientValidation.valid) {
            return {
                valid: true,
                fallback: true,
                fallbackReason: reason,
                metadata: {
                    validationMode: 'CLIENT_SIDE_FALLBACK',
                    fallbackReason: reason,
                    details: details
                }
            };
        } else {
            return {
                valid: false,
                error: clientValidation.error,
                fallback: true,
                fallbackReason: reason,
                metadata: {
                    validationMode: 'CLIENT_SIDE_FALLBACK',
                    fallbackReason: reason,
                    details: details
                }
            };
        }
    }
    
    async validateForm(form) {
        const fields = form.querySelectorAll('input, textarea, select');
        let isValid = true;
        const validationPromises = [];
        let hasFallbackValidation = false;
        
        // Clear all previous errors and warnings
        this.clearAllErrors(form);
        
        // Validate all fields
        for (const field of fields) {
            if (field.name && field.name !== '_csrf') {
                validationPromises.push(this.validateField(field));
            }
        }
        
        const results = await Promise.all(validationPromises);
        isValid = results.every(result => result);
        
        // Check if any field used fallback validation
        hasFallbackValidation = results.some(result => result && result.fallback);
        
        // If client-side validation passes, do full server-side validation
        if (isValid) {
            try {
                const serverValidation = await this.validateFormServerSide(form);
                
                // Handle fallback validation results
                if (serverValidation.fallback) {
                    hasFallbackValidation = true;
                    
                    // Add form-level metadata about fallback
                    const fallbackInfo = document.createElement('input');
                    fallbackInfo.type = 'hidden';
                    fallbackInfo.name = '_validationFallback';
                    fallbackInfo.value = 'true';
                    form.appendChild(fallbackInfo);
                    
                    const fallbackReason = document.createElement('input');
                    fallbackReason.type = 'hidden';
                    fallbackReason.name = '_validationFallbackReason';
                    fallbackReason.value = serverValidation.fallbackReason || 'UNKNOWN';
                    form.appendChild(fallbackReason);
                    
                    // Show final warning before submission
                    const proceedConfirm = await this.showFallbackConfirmation(serverValidation.fallbackReason);
                    if (!proceedConfirm) {
                        return false; // User chose not to proceed
                    }
                }
                
                if (!serverValidation.valid) {
                    this.displayServerErrors(serverValidation.fieldErrors);
                    isValid = false;
                }
                
            } catch (error) {
                console.warn('Server-side form validation failed:', error);
                hasFallbackValidation = true;
                
                // Show final fallback warning
                this.showValidationWarning(
                    'Final validation check failed. Your form has been validated using basic rules only. ' +
                    'You may proceed, but consider retrying later for complete validation.',
                    'warning'
                );
            }
        }
        
        // Add validation summary to form
        if (isValid) {
            const validationSummary = document.createElement('input');
            validationSummary.type = 'hidden';
            validationSummary.name = '_validationSummary';
            validationSummary.value = hasFallbackValidation ? 'FALLBACK_VALIDATION' : 'FULL_VALIDATION';
            form.appendChild(validationSummary);
        }
        
        return isValid;
    }
    
    /**
     * Show confirmation dialog for fallback validation
     */
    async showFallbackConfirmation(fallbackReason) {
        let message;
        switch (fallbackReason) {
            case 'SERVICE_UNAVAILABLE':
                message = 'The validation service is currently unavailable. Your form has been validated using basic rules only.\n\n' +
                         'Do you want to proceed with submission, or wait and retry for full validation?';
                break;
            case 'SERVICE_DEGRADED':
                message = 'The validation service is operating with limited functionality. Some validation checks may have been skipped.\n\n' +
                         'Do you want to proceed with submission?';
                break;
            case 'CONNECTION_ERROR':
                message = 'Unable to connect to the validation service. Your form has been validated offline using basic rules.\n\n' +
                         'Do you want to proceed with submission, or check your connection and retry?';
                break;
            default:
                message = 'Full validation was not available. Your form has been validated using basic rules only.\n\n' +
                         'Do you want to proceed with submission?';
        }
        
        return confirm(message);
    }
    
    async validateFormServerSide(form) {
        const formData = this.getFormData();
        const ownerId = this.getOwnerId();
        
        const endpoint = ownerId ? 
            `${this.validationEndpoint}/validate/${ownerId}` : 
            `${this.validationEndpoint}/validate`;
        
        try {
            // Check service health first for form submission
            const healthResponse = await fetch(`${this.validationEndpoint}/health`, {
                method: 'GET',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            
            // If service is unavailable, show comprehensive warning
            if (healthResponse.status === 503) {
                const healthData = await healthResponse.json();
                this.showValidationWarning(
                    'Validation service is currently unavailable. Your form data has been validated using basic client-side rules only. ' +
                    'Consider waiting for the service to recover for full validation, or proceed with caution.',
                    'warning'
                );
                
                // Return a result indicating fallback validation was used
                return {
                    valid: true,
                    fallback: true,
                    fallbackReason: 'SERVICE_UNAVAILABLE',
                    message: 'Form validated using basic rules only due to service unavailability'
                };
            }
            
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(formData)
            });
            
            if (!response.ok) {
                if (response.status === 503) {
                    // Service degraded during form submission
                    const errorData = await response.json();
                    this.showValidationWarning(
                        'Validation service became unavailable during form submission. ' +
                        'Your data has been validated using basic rules. You may proceed or retry later for full validation.',
                        'warning'
                    );
                    
                    return {
                        valid: true,
                        fallback: true,
                        fallbackReason: 'SERVICE_DEGRADED',
                        message: errorData.message || 'Service became unavailable during validation'
                    };
                }
                throw new Error(`Server validation failed: ${response.status}`);
            }
            
            const result = await response.json();
            
            // Handle degraded validation (202 Accepted)
            if (response.status === 202 || result.warning || result.warningMessage) {
                this.showValidationWarning(
                    result.warningMessage || 
                    'Validation completed with limited checks. Some advanced validations may not have been performed.',
                    'info'
                );
            }
            
            // Show success message for full validation
            if (result.valid && response.status === 200 && !result.warning) {
                this.showValidationWarning('Form validation completed successfully with full checks.', 'success');
            }
            
            return result;
            
        } catch (error) {
            console.error('Server-side form validation error:', error);
            
            // Show comprehensive error message
            this.showValidationWarning(
                'Unable to connect to validation service. Your form has been validated using basic offline rules only. ' +
                'Please check your internet connection and consider retrying for full validation.',
                'warning'
            );
            
            // Return fallback result
            return {
                valid: true,
                fallback: true,
                fallbackReason: 'CONNECTION_ERROR',
                message: 'Form validated offline due to connection error: ' + error.message
            };
        }
    }
    
    getFormData() {
        const form = document.querySelector('form[th\\:object="${owner}"], form[action*="/owners"]');
        const formData = {};
        
        const fields = form.querySelectorAll('input, textarea, select');
        fields.forEach(field => {
            if (field.name && field.name !== '_csrf') {
                formData[field.name] = field.value;
            }
        });
        
        return formData;
    }
    
    getOwnerId() {
        const form = document.querySelector('form[th\\:object="${owner}"], form[action*="/owners"]');
        const action = form.getAttribute('action') || form.getAttribute('th:action');
        
        // Extract owner ID from action URL if it's an edit form
        const match = action && action.match(/\/owners\/(\d+)\/edit/);
        return match ? parseInt(match[1]) : null;
    }
    
    displayFieldError(field, error) {
        // Add error class to field
        field.classList.add('is-invalid');
        
        // Remove existing error message
        this.clearFieldError(field);
        
        // Create error message element with enhanced styling
        const errorElement = document.createElement('div');
        errorElement.className = 'invalid-feedback field-specific-error';
        errorElement.setAttribute('data-field', error.fieldName || field.name);
        errorElement.innerHTML = this.formatErrorMessage(error);
        
        // Insert error message after the field
        field.parentNode.appendChild(errorElement);
        
        // Add visual indicator
        this.addErrorIndicator(field);
        
        // Add field-specific styling
        this.addFieldSpecificStyling(field, error);
        
        // Log error for debugging
        console.debug(`Field error displayed for ${error.fieldName || field.name}:`, error);
    }
    
    displayServerErrors(fieldErrors) {
        if (!fieldErrors || !Array.isArray(fieldErrors)) return;
        
        // Clear all previous errors first
        this.clearAllErrors();
        
        // Group errors by field to handle multiple errors per field
        const errorsByField = this.groupErrorsByField(fieldErrors);
        
        // Display errors for each field
        Object.entries(errorsByField).forEach(([fieldName, errors]) => {
            const field = document.querySelector(`[name="${fieldName}"]`);
            if (field) {
                // If multiple errors for same field, combine them
                if (errors.length === 1) {
                    this.displayFieldError(field, errors[0]);
                } else {
                    this.displayMultipleFieldErrors(field, errors);
                }
            } else {
                console.warn(`Field not found for error: ${fieldName}`, errors);
            }
        });
        
        // Scroll to first error field
        this.scrollToFirstError();
    }
    
    displayMultipleFieldErrors(field, errors) {
        // Add error class to field
        field.classList.add('is-invalid');
        
        // Remove existing error message
        this.clearFieldError(field);
        
        // Create combined error message element
        const errorElement = document.createElement('div');
        errorElement.className = 'invalid-feedback field-specific-error multiple-errors';
        errorElement.setAttribute('data-field', field.name);
        
        // Format multiple errors
        const errorMessages = errors.map(error => this.formatErrorMessage(error)).join('<hr class="error-separator">');
        errorElement.innerHTML = errorMessages;
        
        // Insert error message after the field
        field.parentNode.appendChild(errorElement);
        
        // Add visual indicator
        this.addErrorIndicator(field);
        
        // Add field-specific styling for multiple errors
        this.addFieldSpecificStyling(field, errors[0], true);
        
        console.debug(`Multiple field errors displayed for ${field.name}:`, errors);
    }
    
    groupErrorsByField(fieldErrors) {
        const grouped = {};
        fieldErrors.forEach(error => {
            const fieldName = error.fieldName;
            if (!grouped[fieldName]) {
                grouped[fieldName] = [];
            }
            grouped[fieldName].push(error);
        });
        return grouped;
    }
    
    addFieldSpecificStyling(field, error, isMultiple = false) {
        // Add error type specific classes
        if (error.errorCode) {
            if (error.errorCode.includes('REQUIRED')) {
                field.classList.add('error-required');
            } else if (error.errorCode.includes('FORMAT') || error.errorCode.includes('INVALID')) {
                field.classList.add('error-format');
            } else if (error.errorCode.includes('UNIQUE') || error.errorCode.includes('DUPLICATE')) {
                field.classList.add('error-uniqueness');
            }
        }
        
        if (isMultiple) {
            field.classList.add('error-multiple');
        }
        
        // Add field-specific error class
        field.classList.add(`error-field-${field.name}`);
    }
    
    formatErrorMessage(error) {
        // Use field display name from server if available, otherwise generate it
        const fieldDisplayName = error.fieldDisplayName || this.getFieldDisplayName(error.fieldName);
        
        let message = `<div class="error-main">`;
        
        // Main error message
        if (error.errorMessage) {
            // If error message already includes field name, use as-is, otherwise add field name
            if (error.errorMessage.includes(fieldDisplayName)) {
                message += `<strong>${error.errorMessage}</strong>`;
            } else {
                message += `<strong>${fieldDisplayName}: ${error.errorMessage}</strong>`;
            }
        } else {
            message += `<strong>${fieldDisplayName}: Validation error</strong>`;
        }
        
        message += `</div>`;
        
        // Corrective guidance
        if (error.correctionGuidance) {
            message += `<div class="error-guidance">
                <small class="text-muted">
                    <i class="fas fa-info-circle"></i> ${error.correctionGuidance}
                </small>
            </div>`;
        }
        
        // Format example
        if (error.formatExample) {
            message += `<div class="error-example">
                <small class="text-info">
                    <i class="fas fa-lightbulb"></i> Example: ${error.formatExample}
                </small>
            </div>`;
        }
        
        // Error code for debugging (only in development)
        if (this.isDebugMode() && error.errorCode) {
            message += `<div class="error-debug">
                <small class="text-muted" style="font-size: 0.75em;">
                    Code: ${error.errorCode}
                </small>
            </div>`;
        }
        
        return message;
    }
    
    isDebugMode() {
        // Check if we're in development mode
        return window.location.hostname === 'localhost' || 
               window.location.hostname === '127.0.0.1' ||
               window.location.search.includes('debug=true');
    }
    
    clearFieldError(field) {
        // Remove all error classes
        field.classList.remove('is-invalid', 'error-required', 'error-format', 'error-uniqueness', 'error-multiple');
        field.classList.remove(`error-field-${field.name}`);
        
        // Remove error message
        const errorElement = field.parentNode.querySelector('.invalid-feedback');
        if (errorElement) {
            errorElement.remove();
        }
        
        // Remove error indicator
        this.removeErrorIndicator(field);
        
        // Add success indicator briefly if field has value and was previously invalid
        if (field.value.trim() && field.dataset.wasInvalid === 'true') {
            this.showSuccessIndicator(field);
            delete field.dataset.wasInvalid;
        }
    }
    
    clearAllErrors(form) {
        const formElement = form || document.querySelector('form[th\\:object="${owner}"], form[action*="/owners"]');
        if (!formElement) return;
        
        const errorFields = formElement.querySelectorAll('.is-invalid');
        errorFields.forEach(field => {
            this.clearFieldError(field);
        });
        
        // Remove any orphaned error messages
        const orphanedErrors = formElement.querySelectorAll('.invalid-feedback');
        orphanedErrors.forEach(error => error.remove());
        
        // Remove any orphaned error indicators
        const orphanedIndicators = formElement.querySelectorAll('.field-error-indicator');
        orphanedIndicators.forEach(indicator => indicator.remove());
    }
    
    scrollToFirstError() {
        const firstErrorField = document.querySelector('.is-invalid');
        if (firstErrorField) {
            firstErrorField.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'center' 
            });
            
            // Focus the field after scrolling
            setTimeout(() => {
                firstErrorField.focus();
            }, 500);
        }
    }
    
    addErrorIndicator(field) {
        // Remove existing indicator first
        this.removeErrorIndicator(field);
        
        const indicator = document.createElement('i');
        indicator.className = 'fas fa-exclamation-triangle text-danger field-error-indicator';
        indicator.style.cssText = `
            position: absolute; 
            right: 10px; 
            top: 50%; 
            transform: translateY(-50%); 
            z-index: 10;
            pointer-events: none;
            animation: errorPulse 0.5s ease-in-out;
        `;
        
        // Add tooltip with error summary
        const errorElement = field.parentNode.querySelector('.invalid-feedback');
        if (errorElement) {
            const errorText = errorElement.textContent.trim();
            indicator.title = errorText.length > 100 ? errorText.substring(0, 100) + '...' : errorText;
        }
        
        // Make parent position relative if not already
        const parent = field.parentNode;
        if (getComputedStyle(parent).position === 'static') {
            parent.style.position = 'relative';
        }
        
        parent.appendChild(indicator);
    }
    
    removeErrorIndicator(field) {
        const indicator = field.parentNode.querySelector('.field-error-indicator');
        if (indicator) {
            indicator.remove();
        }
    }
    
    showSuccessIndicator(field) {
        // Remove any existing indicators
        this.removeErrorIndicator(field);
        this.removeSuccessIndicator(field);
        
        const indicator = document.createElement('i');
        indicator.className = 'fas fa-check-circle text-success field-success-indicator';
        indicator.style.cssText = `
            position: absolute; 
            right: 10px; 
            top: 50%; 
            transform: translateY(-50%); 
            z-index: 10;
            pointer-events: none;
            animation: successPulse 0.5s ease-in-out;
        `;
        
        // Make parent position relative if not already
        const parent = field.parentNode;
        if (getComputedStyle(parent).position === 'static') {
            parent.style.position = 'relative';
        }
        
        parent.appendChild(indicator);
        
        // Add success class to field
        field.classList.add('is-valid');
        
        // Remove success indicator after a delay
        setTimeout(() => {
            this.removeSuccessIndicator(field);
            field.classList.remove('is-valid');
        }, 2000);
    }
    
    removeSuccessIndicator(field) {
        const indicator = field.parentNode.querySelector('.field-success-indicator');
        if (indicator) {
            indicator.remove();
        }
    }
    
    getFieldDisplayName(fieldName) {
        const displayNames = {
            'firstName': 'First Name',
            'lastName': 'Last Name',
            'email': 'Email Address',
            'telephone': 'Telephone',
            'mobileNumber': 'Mobile Number',
            'address': 'Address',
            'city': 'City',
            'state': 'State',
            'zipCode': 'ZIP Code'
        };
        
        return displayNames[fieldName] || fieldName;
    }
    
    validatePostalCodeFormat(field, showSuccess = false) {
        const validation = this.validatePostalCodeClientSide(field.value);
        
        if (!validation.valid) {
            this.displayFieldError(field, validation.error);
        } else if (showSuccess && field.value.trim()) {
            // Show success indicator for valid postal codes
            this.showSuccessIndicator(field);
        }
    }
    
    showValidationWarning(message, type = 'warning') {
        // Remove any existing warning
        const existingWarning = document.querySelector('.validation-warning');
        if (existingWarning) {
            existingWarning.remove();
        }
        
        // Determine alert class and icon based on type
        let alertClass, iconClass, dismissClass;
        switch (type) {
            case 'info':
                alertClass = 'alert-info';
                iconClass = 'bi-info-circle-fill';
                dismissClass = 'btn-outline-info';
                break;
            case 'success':
                alertClass = 'alert-success';
                iconClass = 'bi-check-circle-fill';
                dismissClass = 'btn-outline-success';
                break;
            case 'danger':
                alertClass = 'alert-danger';
                iconClass = 'bi-exclamation-triangle-fill';
                dismissClass = 'btn-outline-danger';
                break;
            default: // warning
                alertClass = 'alert-warning';
                iconClass = 'bi-exclamation-triangle-fill';
                dismissClass = 'btn-outline-warning';
        }
        
        // Create warning element with enhanced styling
        const warningElement = document.createElement('div');
        warningElement.className = `alert ${alertClass} alert-dismissible fade show validation-warning`;
        warningElement.setAttribute('role', 'alert');
        warningElement.innerHTML = `
            <div class="d-flex align-items-start">
                <i class="bi ${iconClass} me-2 flex-shrink-0" style="font-size: 1.1em; margin-top: 2px;"></i>
                <div class="flex-grow-1">
                    <strong>${type.charAt(0).toUpperCase() + type.slice(1)}:</strong> ${message}
                    ${type === 'warning' ? '<br><small class="text-muted">You can still submit the form, but full validation is recommended.</small>' : ''}
                </div>
                <div class="ms-2">
                    <button type="button" class="btn btn-sm ${dismissClass} me-2" onclick="this.closest('.validation-warning').remove()" title="Dismiss">
                        <i class="bi bi-x"></i>
                    </button>
                    ${type === 'warning' ? `
                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="window.ownerFormValidator.retryValidation()" title="Retry validation">
                        <i class="bi bi-arrow-clockwise"></i> Retry
                    </button>
                    ` : ''}
                </div>
            </div>
        `;
        
        // Insert warning at the top of the form
        const form = document.querySelector('form[th\\:object="${owner}"], form[action*="/owners"]');
        if (form) {
            form.insertBefore(warningElement, form.firstChild);
        } else {
            // Fallback: insert at top of page content
            const container = document.querySelector('.container');
            if (container) {
                container.insertBefore(warningElement, container.firstChild);
            }
        }
        
        // Auto-dismiss based on type (longer for warnings)
        const dismissTime = type === 'warning' ? 20000 : type === 'info' ? 10000 : 5000;
        setTimeout(() => {
            if (warningElement.parentNode) {
                warningElement.classList.remove('show');
                setTimeout(() => warningElement.remove(), 150);
            }
        }, dismissTime);
        
        // Scroll warning into view if not visible
        setTimeout(() => {
            if (warningElement.getBoundingClientRect().top < 0) {
                warningElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }, 100);
    }
    
    /**
     * Show field-specific warning indicator
     */
    showFieldWarning(field, message) {
        // Remove existing warning indicator
        this.clearFieldWarning(field);
        
        const indicator = document.createElement('i');
        indicator.className = 'bi bi-exclamation-triangle text-warning field-warning-indicator';
        indicator.style.cssText = `
            position: absolute; 
            right: 30px; 
            top: 50%; 
            transform: translateY(-50%); 
            z-index: 10;
            pointer-events: none;
            animation: warningPulse 1s ease-in-out;
        `;
        indicator.title = message;
        
        // Make parent position relative if not already
        const parent = field.parentNode;
        if (getComputedStyle(parent).position === 'static') {
            parent.style.position = 'relative';
        }
        
        parent.appendChild(indicator);
        
        // Add warning class to field
        field.classList.add('field-warning');
    }
    
    /**
     * Clear field-specific warning indicator
     */
    clearFieldWarning(field) {
        const indicator = field.parentNode.querySelector('.field-warning-indicator');
        if (indicator) {
            indicator.remove();
        }
        field.classList.remove('field-warning');
    }
    
    /**
     * Show field success indicator
     */
    showFieldSuccess(field, message) {
        // Remove any existing indicators
        this.removeErrorIndicator(field);
        this.removeSuccessIndicator(field);
        this.clearFieldWarning(field);
        
        const indicator = document.createElement('i');
        indicator.className = 'bi bi-check-circle text-success field-success-indicator';
        indicator.style.cssText = `
            position: absolute; 
            right: 10px; 
            top: 50%; 
            transform: translateY(-50%); 
            z-index: 10;
            pointer-events: none;
            animation: successPulse 0.5s ease-in-out;
        `;
        indicator.title = message;
        
        // Make parent position relative if not already
        const parent = field.parentNode;
        if (getComputedStyle(parent).position === 'static') {
            parent.style.position = 'relative';
        }
        
        parent.appendChild(indicator);
        
        // Add success class to field
        field.classList.add('is-valid');
        
        // Remove success indicator after a delay
        setTimeout(() => {
            this.removeSuccessIndicator(field);
            field.classList.remove('is-valid');
        }, 3000);
    }
    
    /**
     * Retry validation for all fields
     */
    async retryValidation() {
        const form = document.querySelector('form[th\\:object="${owner}"], form[action*="/owners"]');
        if (!form) return;
        
        // Remove existing warning
        const existingWarning = document.querySelector('.validation-warning');
        if (existingWarning) {
            existingWarning.remove();
        }
        
        // Show retry in progress
        this.showValidationWarning('Retrying validation...', 'info');
        
        // Validate all fields
        const fields = form.querySelectorAll('input, textarea, select');
        const validationPromises = [];
        
        for (const field of fields) {
            if (field.name && field.name !== '_csrf' && field.value.trim()) {
                validationPromises.push(this.validateField(field));
            }
        }
        
        try {
            const results = await Promise.all(validationPromises);
            const allValid = results.every(result => result);
            
            // Remove retry message
            const retryWarning = document.querySelector('.validation-warning');
            if (retryWarning) {
                retryWarning.remove();
            }
            
            if (allValid) {
                this.showValidationWarning('Validation retry successful! Full validation is now available.', 'success');
            } else {
                this.showValidationWarning('Some validation issues remain. Please check the highlighted fields.', 'warning');
            }
            
        } catch (error) {
            console.error('Retry validation failed:', error);
            this.showValidationWarning('Retry failed. Validation service may still be unavailable.', 'danger');
        }
    }
    
    addValidationStyles() {
        if (document.getElementById('owner-form-validation-styles')) return;
        
        const styles = document.createElement('style');
        styles.id = 'owner-form-validation-styles';
        styles.textContent = `
            /* Field indicators */
            .field-error-indicator, .field-success-indicator, .field-warning-indicator {
                pointer-events: none;
            }
            
            /* Animations */
            @keyframes errorPulse {
                0% { opacity: 0; transform: translateY(-50%) scale(0.8); }
                50% { opacity: 1; transform: translateY(-50%) scale(1.1); }
                100% { opacity: 1; transform: translateY(-50%) scale(1); }
            }
            
            @keyframes successPulse {
                0% { opacity: 0; transform: translateY(-50%) scale(0.8); }
                50% { opacity: 1; transform: translateY(-50%) scale(1.1); }
                100% { opacity: 1; transform: translateY(-50%) scale(1); }
            }
            
            @keyframes warningPulse {
                0% { opacity: 0; transform: translateY(-50%) scale(0.8); }
                50% { opacity: 1; transform: translateY(-50%) scale(1.05); }
                100% { opacity: 0.8; transform: translateY(-50%) scale(1); }
            }
            
            /* Postal code examples */
            .postal-examples {
                line-height: 1.4;
            }
            
            .postal-examples-link {
                text-decoration: none;
                color: #007bff;
            }
            
            .postal-examples-link:hover {
                text-decoration: underline;
            }
            
            /* Field states */
            .is-valid {
                border-color: #28a745 !important;
                box-shadow: 0 0 0 0.2rem rgba(40, 167, 69, 0.25) !important;
            }
            
            .field-warning {
                border-color: #ffc107 !important;
                box-shadow: 0 0 0 0.2rem rgba(255, 193, 7, 0.25) !important;
            }
            
            /* Enhanced validation warning styling */
            .validation-warning {
                border-left: 4px solid;
                margin-bottom: 1rem;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            
            .validation-warning.alert-warning {
                border-left-color: #ffc107;
                background-color: #fff3cd;
            }
            
            .validation-warning.alert-info {
                border-left-color: #17a2b8;
                background-color: #d1ecf1;
            }
            
            .validation-warning.alert-success {
                border-left-color: #28a745;
                background-color: #d4edda;
            }
            
            .validation-warning.alert-danger {
                border-left-color: #dc3545;
                background-color: #f8d7da;
            }
            
            /* Error message styling */
            .invalid-feedback {
                display: block !important;
                margin-top: 0.25rem;
                font-size: 0.875em;
                line-height: 1.4;
            }
            
            .field-specific-error {
                border-left: 3px solid #dc3545;
                padding-left: 0.75rem;
                background-color: #f8f9fa;
                border-radius: 0.25rem;
                padding: 0.5rem 0.75rem;
            }
            
            .field-specific-error.multiple-errors {
                border-left-color: #fd7e14;
            }
            
            .error-main strong {
                color: #dc3545;
                display: block;
                margin-bottom: 0.25rem;
            }
            
            .error-guidance {
                margin-top: 0.25rem;
            }
            
            .error-guidance .text-muted {
                color: #6c757d !important;
            }
            
            .error-example {
                margin-top: 0.25rem;
            }
            
            .error-example .text-info {
                color: #17a2b8 !important;
            }
            
            .error-debug {
                margin-top: 0.25rem;
                opacity: 0.7;
            }
            
            .error-separator {
                margin: 0.5rem 0;
                border-color: #dee2e6;
            }
            
            /* Field-specific error types */
            .error-required {
                border-left-color: #dc3545 !important;
            }
            
            .error-format {
                border-left-color: #fd7e14 !important;
            }
            
            .error-uniqueness {
                border-left-color: #6f42c1 !important;
            }
            
            .error-multiple {
                border-left-color: #e83e8c !important;
            }
            
            /* Focus states for error fields */
            .is-invalid:focus {
                border-color: #dc3545;
                box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25);
            }
            
            .field-warning:focus {
                border-color: #ffc107;
                box-shadow: 0 0 0 0.2rem rgba(255, 193, 7, 0.25);
            }
            
            /* Smooth transitions */
            input, textarea, select {
                transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
            }
            
            .invalid-feedback {
                transition: opacity 0.15s ease-in-out;
            }
            
            .validation-warning {
                transition: opacity 0.15s ease-in-out;
            }
            
            /* Enhanced button styling for validation warnings */
            .validation-warning .btn {
                font-size: 0.875rem;
                padding: 0.25rem 0.5rem;
            }
            
            .validation-warning .btn i {
                font-size: 0.875rem;
            }
            
            /* Responsive adjustments */
            @media (max-width: 768px) {
                .validation-warning .d-flex {
                    flex-direction: column;
                    align-items: stretch;
                }
                
                .validation-warning .ms-2 {
                    margin-left: 0 !important;
                    margin-top: 0.5rem;
                    text-align: right;
                }
                
                .field-error-indicator, .field-success-indicator, .field-warning-indicator {
                    right: 5px;
                    font-size: 0.875rem;
                }
            }
        `;
        
        document.head.appendChild(styles);
    }
}

// Initialize validation when script loads
const ownerFormValidator = new OwnerFormValidator();

// Export for testing purposes
if (typeof module !== 'undefined' && module.exports) {
    module.exports = OwnerFormValidator;
}