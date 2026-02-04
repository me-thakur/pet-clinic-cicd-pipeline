/**
 * Unit tests for enhanced owner form validation functionality
 * Tests graceful degradation, warning messages, and enhanced error display
 * Validates: Requirements 3.2, 3.3, 3.4
 */

describe('Enhanced Owner Form Validation', () => {
    let validator;
    let mockForm;
    let mockField;
    
    beforeEach(() => {
        // Setup DOM
        document.body.innerHTML = `
            <form th:object="\${owner}" action="/owners/new" method="post">
                <input type="text" name="firstName" id="firstName" required>
                <input type="text" name="lastName" id="lastName" required>
                <input type="email" name="email" id="email">
                <input type="text" name="zipCode" id="zipCode">
            </form>
        `;
        
        mockForm = document.querySelector('form');
        mockField = document.getElementById('firstName');
        
        // Mock fetch globally
        global.fetch = jest.fn();
        
        // Create validator instance
        const OwnerFormValidator = require('../../main/resources/static/js/owner-form-validation.js');
        validator = new OwnerFormValidator();
    });
    
    afterEach(() => {
        jest.clearAllMocks();
        document.body.innerHTML = '';
    });
    
    describe('Service Availability Handling', () => {
        test('should handle service unavailable (503) gracefully', async () => {
            // Mock service unavailable response
            global.fetch.mockResolvedValueOnce({
                status: 503,
                json: () => Promise.resolve({
                    available: false,
                    status: 'Validation service is temporarily unavailable'
                })
            });
            
            const result = await validator.validateFieldServerSide(mockField);
            
            expect(result.fallback).toBe(true);
            expect(result.fallbackReason).toBe('SERVICE_UNAVAILABLE');
            expect(result.metadata.validationMode).toBe('CLIENT_SIDE_FALLBACK');
        });
        
        test('should handle connection errors gracefully', async () => {
            // Mock connection error
            global.fetch.mockRejectedValueOnce(new Error('Network error'));
            
            const result = await validator.validateFieldServerSide(mockField);
            
            expect(result.fallback).toBe(true);
            expect(result.fallbackReason).toBe('CONNECTION_ERROR');
            expect(result.metadata.validationMode).toBe('CLIENT_SIDE_FALLBACK');
        });
        
        test('should handle service degraded (202) response', async () => {
            // Mock degraded service response
            global.fetch
                .mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    json: () => Promise.resolve({ available: true })
                })
                .mockResolvedValueOnce({
                    ok: true,
                    status: 202,
                    json: () => Promise.resolve({
                        valid: true,
                        warning: true,
                        warningMessage: 'Validation service is operating in reduced mode'
                    })
                });
            
            const result = await validator.validateFieldServerSide(mockField);
            
            expect(result.valid).toBe(true);
            expect(result.metadata).toBeDefined();
        });
    });
    
    describe('Warning Message Display', () => {
        test('should display warning message for service unavailable', () => {
            validator.showValidationWarning('Service unavailable', 'warning');
            
            const warning = document.querySelector('.validation-warning');
            expect(warning).toBeTruthy();
            expect(warning.classList.contains('alert-warning')).toBe(true);
            expect(warning.textContent).toContain('Service unavailable');
        });
        
        test('should display info message for service degraded', () => {
            validator.showValidationWarning('Service degraded', 'info');
            
            const warning = document.querySelector('.validation-warning');
            expect(warning).toBeTruthy();
            expect(warning.classList.contains('alert-info')).toBe(true);
        });
        
        test('should display success message for service recovered', () => {
            validator.showValidationWarning('Service recovered', 'success');
            
            const warning = document.querySelector('.validation-warning');
            expect(warning).toBeTruthy();
            expect(warning.classList.contains('alert-success')).toBe(true);
        });
        
        test('should include retry button for warning messages', () => {
            validator.showValidationWarning('Service unavailable', 'warning');
            
            const retryButton = document.querySelector('.validation-warning button[onclick*="retryValidation"]');
            expect(retryButton).toBeTruthy();
            expect(retryButton.textContent).toContain('Retry');
        });
    });
    
    describe('Field-Specific Error Display', () => {
        test('should display field-specific error with format example', () => {
            const error = {
                fieldName: 'zipCode',
                errorMessage: 'Invalid postal code format',
                correctionGuidance: 'Please enter a valid postal code',
                formatExample: '12345 or K1A 0A6',
                errorCode: 'ZIP_FORMAT_INVALID'
            };
            
            validator.displayFieldError(mockField, error);
            
            const errorElement = document.querySelector('.invalid-feedback');
            expect(errorElement).toBeTruthy();
            expect(errorElement.textContent).toContain('Invalid postal code format');
            expect(errorElement.textContent).toContain('Please enter a valid postal code');
            expect(errorElement.textContent).toContain('12345 or K1A 0A6');
        });
        
        test('should display multiple errors for same field', () => {
            const errors = [
                {
                    fieldName: 'email',
                    errorMessage: 'Invalid email format',
                    correctionGuidance: 'Please enter a valid email',
                    errorCode: 'EMAIL_FORMAT_INVALID'
                },
                {
                    fieldName: 'email',
                    errorMessage: 'Email already exists',
                    correctionGuidance: 'Please use a different email',
                    errorCode: 'EMAIL_NOT_UNIQUE'
                }
            ];
            
            validator.displayMultipleFieldErrors(mockField, errors);
            
            const errorElement = document.querySelector('.invalid-feedback.multiple-errors');
            expect(errorElement).toBeTruthy();
            expect(errorElement.textContent).toContain('Invalid email format');
            expect(errorElement.textContent).toContain('Email already exists');
        });
    });
    
    describe('Immediate Validation Feedback', () => {
        test('should clear errors immediately when user starts typing', () => {
            // Setup initial error
            mockField.classList.add('is-invalid');
            const errorDiv = document.createElement('div');
            errorDiv.className = 'invalid-feedback';
            mockField.parentNode.appendChild(errorDiv);
            
            // Simulate user input
            validator.onFieldInput(mockField);
            
            expect(mockField.classList.contains('is-invalid')).toBe(false);
            expect(document.querySelector('.invalid-feedback')).toBeFalsy();
        });
        
        test('should show success indicator for valid field', () => {
            validator.showFieldSuccess(mockField, 'Validation passed');
            
            const successIndicator = document.querySelector('.field-success-indicator');
            expect(successIndicator).toBeTruthy();
            expect(successIndicator.classList.contains('text-success')).toBe(true);
            expect(mockField.classList.contains('is-valid')).toBe(true);
        });
        
        test('should show warning indicator for degraded validation', () => {
            validator.showFieldWarning(mockField, 'Limited validation');
            
            const warningIndicator = document.querySelector('.field-warning-indicator');
            expect(warningIndicator).toBeTruthy();
            expect(warningIndicator.classList.contains('text-warning')).toBe(true);
            expect(mockField.classList.contains('field-warning')).toBe(true);
        });
    });
    
    describe('Service Status Indicator', () => {
        test('should add service status indicator to form', () => {
            validator.addServiceStatusIndicator(mockForm);
            
            const statusIndicator = document.querySelector('.validation-service-status');
            expect(statusIndicator).toBeTruthy();
            expect(statusIndicator.textContent).toContain('Checking validation service status');
        });
        
        test('should update status indicator for available service', async () => {
            validator.addServiceStatusIndicator(mockForm);
            
            // Mock successful health check
            global.fetch.mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: () => Promise.resolve({
                    available: true,
                    status: 'Service is operating normally'
                })
            });
            
            await validator.checkServiceStatus();
            
            const statusIndicator = document.querySelector('.validation-service-status');
            expect(statusIndicator.classList.contains('alert-success')).toBe(true);
            expect(statusIndicator.textContent).toContain('Full validation service is available');
        });
        
        test('should update status indicator for unavailable service', async () => {
            validator.addServiceStatusIndicator(mockForm);
            
            // Mock service unavailable
            global.fetch.mockResolvedValueOnce({
                ok: false,
                status: 503,
                json: () => Promise.resolve({
                    available: false,
                    status: 'Service is temporarily unavailable'
                })
            });
            
            await validator.checkServiceStatus();
            
            const statusIndicator = document.querySelector('.validation-service-status');
            expect(statusIndicator.classList.contains('alert-warning')).toBe(true);
            expect(statusIndicator.textContent).toContain('temporarily unavailable');
        });
    });
    
    describe('Form Submission with Fallback', () => {
        test('should show confirmation dialog for fallback validation', async () => {
            // Mock confirm dialog
            global.confirm = jest.fn().mockReturnValue(true);
            
            const result = await validator.showFallbackConfirmation('SERVICE_UNAVAILABLE');
            
            expect(global.confirm).toHaveBeenCalled();
            expect(result).toBe(true);
            
            const confirmMessage = global.confirm.mock.calls[0][0];
            expect(confirmMessage).toContain('validation service is currently unavailable');
        });
        
        test('should add fallback metadata to form on submission', async () => {
            // Mock server validation returning fallback result
            global.fetch.mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: () => Promise.resolve({
                    valid: true,
                    fallback: true,
                    fallbackReason: 'SERVICE_UNAVAILABLE'
                })
            });
            
            global.confirm = jest.fn().mockReturnValue(true);
            
            const result = await validator.validateForm(mockForm);
            
            expect(result).toBe(true);
            
            const fallbackInput = mockForm.querySelector('input[name="_validationFallback"]');
            const fallbackReasonInput = mockForm.querySelector('input[name="_validationFallbackReason"]');
            
            expect(fallbackInput).toBeTruthy();
            expect(fallbackInput.value).toBe('true');
            expect(fallbackReasonInput).toBeTruthy();
            expect(fallbackReasonInput.value).toBe('SERVICE_UNAVAILABLE');
        });
    });
    
    describe('Retry Validation', () => {
        test('should retry validation for all fields', async () => {
            // Setup form with values
            document.getElementById('firstName').value = 'John';
            document.getElementById('lastName').value = 'Doe';
            document.getElementById('email').value = 'john@example.com';
            
            // Mock successful retry
            global.fetch.mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ valid: true })
            });
            
            await validator.retryValidation();
            
            // Should have called fetch for each field with value
            expect(global.fetch).toHaveBeenCalledTimes(3);
            
            const successMessage = document.querySelector('.validation-warning.alert-success');
            expect(successMessage).toBeTruthy();
            expect(successMessage.textContent).toContain('retry successful');
        });
    });
    
    describe('Client-Side Validation Fallback', () => {
        test('should perform comprehensive client-side validation', () => {
            const owner = {
                firstName: '',
                lastName: 'Doe',
                email: 'invalid-email',
                zipCode: 'INVALID'
            };
            
            const result = validator.performClientSideValidation(owner, new Set(['firstName', 'lastName', 'email', 'zipCode']));
            
            expect(result.valid).toBe(false);
            expect(result.fieldErrors).toHaveLength(2); // firstName required, email invalid format
            expect(result.metadata.validationMode).toBe('CLIENT_SIDE_FALLBACK');
        });
        
        test('should provide specific error messages with guidance', () => {
            const owner = {
                firstName: '',
                email: 'invalid-email'
            };
            
            const result = validator.performClientSideValidation(owner, new Set(['firstName', 'email']));
            
            const firstNameError = result.fieldErrors.find(e => e.fieldName === 'firstName');
            const emailError = result.fieldErrors.find(e => e.fieldName === 'email');
            
            expect(firstNameError.errorMessage).toContain('required');
            expect(firstNameError.correctionGuidance).toContain('Please enter');
            
            expect(emailError.errorMessage).toContain('Invalid email format');
            expect(emailError.formatExample).toContain('user@example.com');
        });
    });
});