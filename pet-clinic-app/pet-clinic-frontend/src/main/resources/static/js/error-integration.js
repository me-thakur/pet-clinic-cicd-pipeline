/**
 * Error Integration Script
 * Integrates all error handling components for comprehensive error management
 * Requirements: 1.3, 1.4, 1.5
 */

(function(window) {
    'use strict';

    /**
     * Error integration manager
     */
    class ErrorIntegration {
        constructor(options = {}) {
            this.options = {
                enableGlobalHandling: options.enableGlobalHandling !== false,
                enableFormIntegration: options.enableFormIntegration !== false,
                enableTableIntegration: options.enableTableIntegration !== false,
                enableApiIntegration: options.enableApiIntegration !== false,
                enableRetryMechanisms: options.enableRetryMechanisms !== false,
                ...options
            };

            this.initialized = false;
            this.integrations = new Map();
            
            this.init();
        }

        /**
         * Initialize error integration
         */
        init() {
            if (this.initialized) return;

            // Wait for all error handling components to be available
            this.waitForComponents().then(() => {
                this.setupIntegrations();
                this.setupGlobalHandling();
                this.setupFormIntegration();
                this.setupTableIntegration();
                this.setupApiIntegration();
                this.setupRetryMechanisms();
                
                this.initialized = true;
                this.dispatchReadyEvent();
            });
        }

        /**
         * Wait for all required components to be available
         */
        async waitForComponents() {
            const requiredComponents = [
                'errorHandler',
                'errorRecovery', 
                'errorCategorization',
                'errorDisplay'
            ];

            const checkComponents = () => {
                return requiredComponents.every(component => window[component]);
            };

            // Wait up to 5 seconds for components
            let attempts = 0;
            const maxAttempts = 50;

            while (!checkComponents() && attempts < maxAttempts) {
                await this.delay(100);
                attempts++;
            }

            if (!checkComponents()) {
                console.warn('Some error handling components are not available:', 
                    requiredComponents.filter(component => !window[component]));
            }
        }

        /**
         * Setup component integrations
         */
        setupIntegrations() {
            // Integrate error handler with display
            this.integrations.set('handler-display', {
                source: window.errorHandler,
                target: window.errorDisplay,
                events: ['error']
            });

            // Integrate error recovery with display
            this.integrations.set('recovery-display', {
                source: window.errorRecovery,
                target: window.errorDisplay,
                events: ['recovery-attempt', 'recovery-success', 'recovery-failure']
            });

            // Integrate categorization with recovery
            this.integrations.set('categorization-recovery', {
                source: window.errorCategorization,
                target: window.errorRecovery,
                events: ['error-categorized']
            });

            // Setup event forwarding between components
            this.setupEventForwarding();
        }

        /**
         * Setup event forwarding between components
         */
        setupEventForwarding() {
            // Forward error handler events to display
            window.addEventListener('errorHandlerError', (event) => {
                const { error, context } = event.detail;
                window.errorDisplay.displayError(error, context);
            });

            // Forward recovery events to display
            window.addEventListener('errorRecoveryAttempt', (event) => {
                window.errorDisplay.updateErrorWithRecovery(
                    event.detail.error, 
                    event.detail.recovery
                );
            });

            window.addEventListener('errorRecoverySuccess', (event) => {
                window.errorDisplay.removeError(event.detail.errorId);
            });

            // Forward display events to analytics
            window.addEventListener('errorDisplayShown', (event) => {
                this.trackErrorDisplay(event.detail);
            });
        }

        /**
         * Setup global error handling
         */
        setupGlobalHandling() {
            if (!this.options.enableGlobalHandling) return;

            // Override console.error to capture application errors
            const originalConsoleError = console.error;
            console.error = (...args) => {
                originalConsoleError.apply(console, args);
                
                // Create error from console arguments
                const error = new Error(args.join(' '));
                error.source = 'console';
                
                this.handleError(error, { source: 'console' });
            };

            // Handle unhandled promise rejections
            window.addEventListener('unhandledrejection', (event) => {
                this.handleError(event.reason, { 
                    source: 'unhandledrejection',
                    promise: event.promise 
                });
            });

            // Handle JavaScript errors
            window.addEventListener('error', (event) => {
                this.handleError(event.error, {
                    source: 'javascript',
                    filename: event.filename,
                    lineno: event.lineno,
                    colno: event.colno
                });
            });

            // Handle resource loading errors
            window.addEventListener('error', (event) => {
                if (event.target !== window) {
                    const error = new Error(`Failed to load resource: ${event.target.src || event.target.href}`);
                    error.resourceType = event.target.tagName.toLowerCase();
                    
                    this.handleError(error, {
                        source: 'resource',
                        element: event.target
                    });
                }
            }, true);
        }

        /**
         * Setup form integration
         */
        setupFormIntegration() {
            if (!this.options.enableFormIntegration) return;

            // Integrate with existing form validation
            document.addEventListener('DOMContentLoaded', () => {
                this.integrateWithForms();
            });

            // Handle dynamic form creation
            const observer = new MutationObserver((mutations) => {
                mutations.forEach((mutation) => {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            const forms = node.querySelectorAll ? node.querySelectorAll('form') : [];
                            forms.forEach(form => this.integrateWithForm(form));
                        }
                    });
                });
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
        }

        /**
         * Integrate with all forms on the page
         */
        integrateWithForms() {
            const forms = document.querySelectorAll('form');
            forms.forEach(form => this.integrateWithForm(form));
        }

        /**
         * Integrate with a specific form
         */
        integrateWithForm(form) {
            if (form.hasAttribute('data-error-integrated')) return;
            form.setAttribute('data-error-integrated', 'true');

            // Override form submission to handle errors
            form.addEventListener('submit', async (event) => {
                event.preventDefault();
                
                try {
                    await this.handleFormSubmission(form);
                } catch (error) {
                    this.handleError(error, {
                        source: 'form',
                        form: form,
                        formData: new FormData(form)
                    });
                }
            });

            // Integrate field validation
            const fields = form.querySelectorAll('input, textarea, select');
            fields.forEach(field => this.integrateWithField(field));
        }

        /**
         * Integrate with a form field
         */
        integrateWithField(field) {
            if (field.hasAttribute('data-error-integrated')) return;
            field.setAttribute('data-error-integrated', 'true');

            // Clear errors when user starts typing
            field.addEventListener('input', () => {
                this.clearFieldErrors(field);
            });

            // Validate on blur
            field.addEventListener('blur', async () => {
                try {
                    await this.validateField(field);
                } catch (error) {
                    this.handleFieldError(field, error);
                }
            });
        }

        /**
         * Handle form submission with error handling
         */
        async handleFormSubmission(form) {
            const formData = new FormData(form);
            const action = form.action || window.location.href;
            const method = form.method || 'POST';

            try {
                const response = await fetch(action, {
                    method: method,
                    body: formData,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw this.createFormError(errorData, response.status);
                }

                // Success - handle redirect or show success message
                const result = await response.json();
                this.handleFormSuccess(form, result);

            } catch (error) {
                throw error;
            }
        }

        /**
         * Create form error from response
         */
        createFormError(errorData, status) {
            const error = new Error(errorData.message || 'Form submission failed');
            error.status = status;
            error.errorCode = errorData.errorCode;
            error.fieldErrors = errorData.fieldErrors;
            error.suggestions = errorData.suggestions;
            error.isFormError = true;
            
            return error;
        }

        /**
         * Handle form success
         */
        handleFormSuccess(form, result) {
            // Show success message
            this.showSuccessMessage('Form submitted successfully');

            // Handle redirect if specified
            if (result.redirectUrl) {
                setTimeout(() => {
                    window.location.href = result.redirectUrl;
                }, 1000);
            }
        }

        /**
         * Validate individual field
         */
        async validateField(field) {
            if (!field.name || field.name === '_csrf') return;

            // Use existing validation if available
            if (window.ownerFormValidator && typeof window.ownerFormValidator.validateField === 'function') {
                return await window.ownerFormValidator.validateField(field);
            }

            // Basic validation
            if (field.required && !field.value.trim()) {
                throw new Error(`${this.getFieldDisplayName(field)} is required`);
            }
        }

        /**
         * Handle field error
         */
        handleFieldError(field, error) {
            const fieldError = {
                field: field.name,
                message: error.message,
                suggestions: error.suggestions || []
            };

            window.errorDisplay.displayFieldError(field, fieldError);
        }

        /**
         * Clear field errors
         */
        clearFieldErrors(field) {
            window.errorDisplay.clearFieldError(field);
        }

        /**
         * Setup table integration
         */
        setupTableIntegration() {
            if (!this.options.enableTableIntegration) return;

            // Integrate with enhanced tables
            document.addEventListener('DOMContentLoaded', () => {
                this.integrateWithTables();
            });

            // Listen for table error events
            window.addEventListener('enhancedTableError', (event) => {
                this.handleError(event.detail.error, {
                    source: 'table',
                    table: event.detail.table,
                    operation: event.detail.operation
                });
            });
        }

        /**
         * Integrate with all tables on the page
         */
        integrateWithTables() {
            const tables = document.querySelectorAll('.enhanced-table, [data-enhanced-table]');
            tables.forEach(table => this.integrateWithTable(table));
        }

        /**
         * Integrate with a specific table
         */
        integrateWithTable(table) {
            if (table.hasAttribute('data-error-integrated')) return;
            table.setAttribute('data-error-integrated', 'true');

            // Override table operations to handle errors
            const originalFetch = table.fetch || (() => {});
            table.fetch = async (...args) => {
                try {
                    return await originalFetch.apply(table, args);
                } catch (error) {
                    this.handleError(error, {
                        source: 'table',
                        table: table,
                        operation: 'fetch'
                    });
                    throw error;
                }
            };
        }

        /**
         * Setup API integration
         */
        setupApiIntegration() {
            if (!this.options.enableApiIntegration) return;

            // Integrate with fetch API
            this.integrateFetchAPI();

            // Integrate with XMLHttpRequest
            this.integrateXHR();
        }

        /**
         * Integrate with fetch API
         */
        integrateFetchAPI() {
            const originalFetch = window.fetch;
            
            window.fetch = async (...args) => {
                try {
                    const response = await originalFetch.apply(window, args);
                    
                    if (!response.ok) {
                        const error = await this.createAPIError(response);
                        this.handleError(error, {
                            source: 'api',
                            url: args[0],
                            options: args[1],
                            response: response
                        });
                        throw error;
                    }
                    
                    return response;
                } catch (error) {
                    if (!error.isAPIError) {
                        this.handleError(error, {
                            source: 'api',
                            url: args[0],
                            options: args[1]
                        });
                    }
                    throw error;
                }
            };
        }

        /**
         * Create API error from response
         */
        async createAPIError(response) {
            let errorData = {};
            
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    errorData = await response.json();
                } else {
                    errorData.message = await response.text();
                }
            } catch (e) {
                errorData.message = `HTTP ${response.status} ${response.statusText}`;
            }

            const error = new Error(errorData.message || `HTTP ${response.status}`);
            error.status = response.status;
            error.statusText = response.statusText;
            error.errorCode = errorData.errorCode;
            error.fieldErrors = errorData.fieldErrors;
            error.suggestions = errorData.suggestions;
            error.retryAfter = errorData.retryAfter;
            error.fallbackOptions = errorData.fallbackOptions;
            error.isAPIError = true;
            
            return error;
        }

        /**
         * Integrate with XMLHttpRequest
         */
        integrateXHR() {
            const originalXHR = window.XMLHttpRequest;
            
            window.XMLHttpRequest = function() {
                const xhr = new originalXHR();
                const originalSend = xhr.send;
                
                xhr.send = function(...args) {
                    xhr.addEventListener('error', () => {
                        const error = new Error('Network error');
                        error.isNetworkError = true;
                        
                        window.errorIntegration.handleError(error, {
                            source: 'xhr',
                            xhr: xhr
                        });
                    });
                    
                    xhr.addEventListener('load', () => {
                        if (xhr.status >= 400) {
                            let errorData = {};
                            
                            try {
                                errorData = JSON.parse(xhr.responseText);
                            } catch (e) {
                                errorData.message = xhr.responseText || `HTTP ${xhr.status}`;
                            }
                            
                            const error = new Error(errorData.message || `HTTP ${xhr.status}`);
                            error.status = xhr.status;
                            error.errorCode = errorData.errorCode;
                            error.isAPIError = true;
                            
                            window.errorIntegration.handleError(error, {
                                source: 'xhr',
                                xhr: xhr
                            });
                        }
                    });
                    
                    return originalSend.apply(xhr, args);
                };
                
                return xhr;
            };
        }

        /**
         * Setup retry mechanisms
         */
        setupRetryMechanisms() {
            if (!this.options.enableRetryMechanisms) return;

            // Setup automatic retry for transient errors
            window.addEventListener('errorHandlerError', async (event) => {
                const { error, context } = event.detail;
                
                if (this.isTransientError(error)) {
                    try {
                        const result = await window.errorRecovery.attemptRecovery(error, context);
                        
                        if (result.success) {
                            this.showSuccessMessage('Operation completed successfully after retry');
                        }
                    } catch (recoveryError) {
                        console.warn('Automatic recovery failed:', recoveryError);
                    }
                }
            });
        }

        /**
         * Check if error is transient (retryable)
         */
        isTransientError(error) {
            // Network errors
            if (error.isNetworkError || error.name === 'TypeError') {
                return true;
            }

            // Server errors (5xx)
            if (error.status >= 500) {
                return true;
            }

            // Service unavailable
            if (error.status === 503) {
                return true;
            }

            // Timeout errors
            if (error.status === 408 || error.status === 504) {
                return true;
            }

            return false;
        }

        /**
         * Main error handling method
         */
        async handleError(error, context = {}) {
            try {
                // Use error handler if available
                if (window.errorHandler) {
                    return await window.errorHandler.handleError(error, context);
                }

                // Fallback handling
                console.error('Error (fallback handling):', error, context);
                this.showErrorMessage(error.message || 'An error occurred');

            } catch (handlingError) {
                console.error('Error handling failed:', handlingError);
                this.showErrorMessage('An unexpected error occurred');
            }
        }

        /**
         * Show success message
         */
        showSuccessMessage(message) {
            // Create success notification
            const notification = document.createElement('div');
            notification.className = 'alert alert-success alert-dismissible fade show';
            notification.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            `;
            notification.innerHTML = `
                <i class="fas fa-check-circle me-2"></i>
                ${message}
                <button type="button" class="btn-close" onclick="this.closest('.alert').remove()"></button>
            `;

            document.body.appendChild(notification);

            // Auto-hide after 3 seconds
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.remove();
                }
            }, 3000);
        }

        /**
         * Show error message (fallback)
         */
        showErrorMessage(message) {
            // Create error notification
            const notification = document.createElement('div');
            notification.className = 'alert alert-danger alert-dismissible fade show';
            notification.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            `;
            notification.innerHTML = `
                <i class="fas fa-exclamation-triangle me-2"></i>
                ${message}
                <button type="button" class="btn-close" onclick="this.closest('.alert').remove()"></button>
            `;

            document.body.appendChild(notification);

            // Auto-hide after 5 seconds
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.remove();
                }
            }, 5000);
        }

        /**
         * Track error display for analytics
         */
        trackErrorDisplay(errorData) {
            // Send to analytics if available
            if (window.errorDisplay && typeof window.errorDisplay.trackError === 'function') {
                window.errorDisplay.trackError(errorData);
            }
        }

        /**
         * Dispatch ready event
         */
        dispatchReadyEvent() {
            window.dispatchEvent(new CustomEvent('errorIntegrationReady', {
                detail: { integration: this }
            }));
        }

        /**
         * Helper methods
         */
        getFieldDisplayName(field) {
            const displayNames = {
                'firstName': 'First Name',
                'lastName': 'Last Name',
                'email': 'Email Address',
                'telephone': 'Telephone',
                'address': 'Address',
                'city': 'City',
                'state': 'State',
                'zipCode': 'ZIP Code'
            };
            
            return displayNames[field.name] || field.name;
        }

        delay(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        /**
         * Public API methods
         */
        isInitialized() {
            return this.initialized;
        }

        getIntegrations() {
            return Array.from(this.integrations.keys());
        }

        enableIntegration(name) {
            const integration = this.integrations.get(name);
            if (integration) {
                integration.enabled = true;
            }
        }

        disableIntegration(name) {
            const integration = this.integrations.get(name);
            if (integration) {
                integration.enabled = false;
            }
        }
    }

    // Create global error integration instance
    window.errorIntegration = new ErrorIntegration();

    // Export for use by other modules
    window.ErrorIntegration = ErrorIntegration;

})(window);