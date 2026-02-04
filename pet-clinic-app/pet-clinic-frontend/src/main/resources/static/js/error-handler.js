/**
 * Central Error Handler for Pet Clinic Frontend
 * Implements specific error message display logic, error categorization, 
 * recovery suggestions, and retry mechanisms for transient errors
 * Requirements: 1.3, 1.4, 1.5
 */

(function(window) {
    'use strict';

    /**
     * Error categories for different handling strategies
     */
    const ErrorCategory = {
        VALIDATION: 'validation',
        SERVICE_UNAVAILABLE: 'service_unavailable',
        NETWORK: 'network',
        AUTHENTICATION: 'authentication',
        AUTHORIZATION: 'authorization',
        NOT_FOUND: 'not_found',
        SERVER_ERROR: 'server_error',
        CLIENT_ERROR: 'client_error'
    };

    /**
     * Error severity levels
     */
    const ErrorSeverity = {
        INFO: 'info',
        WARNING: 'warning',
        ERROR: 'error',
        CRITICAL: 'critical'
    };

    /**
     * Central error handler class
     */
    class ErrorHandler {
        constructor(options = {}) {
            this.options = {
                showNotifications: options.showNotifications !== false,
                enableRetry: options.enableRetry !== false,
                maxRetries: options.maxRetries || 3,
                retryDelay: options.retryDelay || 1000,
                logErrors: options.logErrors !== false,
                ...options
            };

            this.retryCount = new Map();
            this.errorHistory = [];
            this.notificationContainer = null;
            
            this.init();
        }

        /**
         * Initialize error handling system
         */
        init() {
            this.createNotificationContainer();
            this.setupGlobalErrorHandlers();
            this.setupNetworkMonitoring();
        }

        /**
         * Create notification container for error messages
         */
        createNotificationContainer() {
            if (document.getElementById('error-notifications')) return;

            const container = document.createElement('div');
            container.id = 'error-notifications';
            container.className = 'error-notifications-container';
            container.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
                pointer-events: none;
            `;

            document.body.appendChild(container);
            this.notificationContainer = container;
        }

        /**
         * Setup global error handlers
         */
        setupGlobalErrorHandlers() {
            // Handle unhandled promise rejections
            window.addEventListener('unhandledrejection', (event) => {
                this.handleError(event.reason, { source: 'unhandledrejection' });
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

            // Handle fetch errors globally
            this.interceptFetch();
        }

        /**
         * Setup network monitoring
         */
        setupNetworkMonitoring() {
            window.addEventListener('online', () => {
                this.handleNetworkRestore();
            });

            window.addEventListener('offline', () => {
                this.handleNetworkLoss();
            });
        }

        /**
         * Intercept fetch requests to handle API errors
         */
        interceptFetch() {
            const originalFetch = window.fetch;
            
            window.fetch = async (...args) => {
                try {
                    const response = await originalFetch(...args);
                    
                    if (!response.ok) {
                        const errorData = await this.extractErrorData(response);
                        const error = this.createApiError(response, errorData);
                        throw error;
                    }
                    
                    return response;
                } catch (error) {
                    // Only handle if it's our API error or network error
                    if (error.isApiError || error.name === 'TypeError') {
                        this.handleError(error, { 
                            source: 'fetch',
                            url: args[0],
                            options: args[1]
                        });
                    }
                    throw error;
                }
            };
        }

        /**
         * Extract error data from response
         */
        async extractErrorData(response) {
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return await response.json();
                } else {
                    return { message: await response.text() };
                }
            } catch (e) {
                return { message: `HTTP ${response.status} ${response.statusText}` };
            }
        }

        /**
         * Create API error object
         */
        createApiError(response, errorData) {
            const error = new Error(errorData.message || `HTTP ${response.status}`);
            error.isApiError = true;
            error.status = response.status;
            error.statusText = response.statusText;
            error.errorCode = errorData.errorCode;
            error.fieldErrors = errorData.fieldErrors;
            error.suggestions = errorData.suggestions;
            error.retryAfter = errorData.retryAfter;
            error.fallbackOptions = errorData.fallbackOptions;
            error.path = errorData.path;
            error.context = errorData.context;
            
            return error;
        }

        /**
         * Main error handling method
         */
        async handleError(error, context = {}) {
            if (!error) return;

            const categorizedError = this.categorizeError(error, context);
            
            // Log error
            if (this.options.logErrors) {
                this.logError(categorizedError);
            }

            // Add to history
            this.addToHistory(categorizedError);

            // Handle based on category
            const result = await this.handleByCategory(categorizedError);

            // Show notification if enabled
            if (this.options.showNotifications) {
                this.showErrorNotification(categorizedError, result);
            }

            return result;
        }

        /**
         * Categorize error based on type and content
         */
        categorizeError(error, context = {}) {
            let category = ErrorCategory.CLIENT_ERROR;
            let severity = ErrorSeverity.ERROR;
            let retryable = false;
            let suggestions = [];

            if (error.isApiError) {
                // API errors from backend
                if (error.status >= 500) {
                    category = ErrorCategory.SERVER_ERROR;
                    retryable = true;
                    suggestions = error.suggestions || [
                        'Try again in a few moments',
                        'Contact support if the problem persists'
                    ];
                } else if (error.status === 503) {
                    category = ErrorCategory.SERVICE_UNAVAILABLE;
                    severity = ErrorSeverity.WARNING;
                    retryable = true;
                    suggestions = error.suggestions || [
                        'Service is temporarily unavailable',
                        'Please try again in a few minutes'
                    ];
                } else if (error.status === 401) {
                    category = ErrorCategory.AUTHENTICATION;
                    suggestions = error.suggestions || [
                        'Please log in again',
                        'Check your credentials'
                    ];
                } else if (error.status === 403) {
                    category = ErrorCategory.AUTHORIZATION;
                    suggestions = error.suggestions || [
                        'You don\'t have permission for this action',
                        'Contact an administrator for access'
                    ];
                } else if (error.status === 404) {
                    category = ErrorCategory.NOT_FOUND;
                    suggestions = error.suggestions || [
                        'The requested resource was not found',
                        'Check the URL and try again'
                    ];
                } else if (error.status >= 400 && error.status < 500) {
                    category = ErrorCategory.VALIDATION;
                    severity = ErrorSeverity.WARNING;
                    suggestions = error.suggestions || [
                        'Please check your input and try again'
                    ];
                }
            } else if (error.name === 'TypeError' && error.message.includes('fetch')) {
                // Network errors
                category = ErrorCategory.NETWORK;
                retryable = true;
                suggestions = [
                    'Check your internet connection',
                    'Try again in a few moments'
                ];
            }

            return {
                originalError: error,
                category,
                severity,
                retryable,
                suggestions: error.suggestions || suggestions,
                errorCode: error.errorCode || error.name,
                message: error.message,
                fieldErrors: error.fieldErrors,
                retryAfter: error.retryAfter,
                fallbackOptions: error.fallbackOptions,
                context: { ...context, timestamp: new Date() }
            };
        }

        /**
         * Handle error based on category
         */
        async handleByCategory(categorizedError) {
            switch (categorizedError.category) {
                case ErrorCategory.VALIDATION:
                    return await this.handleValidationError(categorizedError);
                case ErrorCategory.SERVICE_UNAVAILABLE:
                    return await this.handleServiceUnavailableError(categorizedError);
                case ErrorCategory.NETWORK:
                    return await this.handleNetworkError(categorizedError);
                case ErrorCategory.AUTHENTICATION:
                    return await this.handleAuthenticationError(categorizedError);
                case ErrorCategory.AUTHORIZATION:
                    return await this.handleAuthorizationError(categorizedError);
                case ErrorCategory.NOT_FOUND:
                    return await this.handleNotFoundError(categorizedError);
                case ErrorCategory.SERVER_ERROR:
                    return await this.handleServerError(categorizedError);
                default:
                    return await this.handleGenericError(categorizedError);
            }
        }

        /**
         * Handle validation errors
         */
        async handleValidationError(error) {
            // Display field-specific errors if available
            if (error.fieldErrors && Array.isArray(error.fieldErrors)) {
                this.displayFieldErrors(error.fieldErrors);
            }

            return { 
                handled: true, 
                retry: false, 
                userAction: 'correct_input',
                message: 'Please correct the highlighted fields'
            };
        }

        /**
         * Handle service unavailable errors
         */
        async handleServiceUnavailableError(error) {
            if (error.retryable && this.options.enableRetry) {
                const retryResult = await this.attemptRetry(error);
                if (retryResult.success) {
                    return { 
                        handled: true, 
                        retry: true, 
                        attempt: retryResult.attempt,
                        message: 'Retrying request...'
                    };
                }
            }

            // Show fallback options if available
            if (error.fallbackOptions && error.fallbackOptions.length > 0) {
                return {
                    handled: true,
                    retry: false,
                    fallback: true,
                    fallbackOptions: error.fallbackOptions,
                    message: 'Service unavailable - fallback options available'
                };
            }

            return { 
                handled: true, 
                retry: false,
                message: 'Service is temporarily unavailable'
            };
        }

        /**
         * Handle network errors
         */
        async handleNetworkError(error) {
            if (error.retryable && this.options.enableRetry) {
                const retryResult = await this.attemptRetry(error);
                if (retryResult.success) {
                    return { 
                        handled: true, 
                        retry: true, 
                        attempt: retryResult.attempt,
                        message: 'Retrying request...'
                    };
                }
            }

            return { 
                handled: true, 
                retry: false,
                message: 'Network connection problem'
            };
        }

        /**
         * Handle authentication errors
         */
        async handleAuthenticationError(error) {
            // Redirect to login page after showing error
            setTimeout(() => {
                window.location.href = '/login';
            }, 3000);

            return { 
                handled: true, 
                retry: false,
                redirect: '/login',
                message: 'Authentication required - redirecting to login...'
            };
        }

        /**
         * Handle authorization errors
         */
        async handleAuthorizationError(error) {
            return { 
                handled: true, 
                retry: false,
                message: 'You don\'t have permission to perform this action'
            };
        }

        /**
         * Handle not found errors
         */
        async handleNotFoundError(error) {
            return { 
                handled: true, 
                retry: false,
                message: 'The requested resource was not found'
            };
        }

        /**
         * Handle server errors
         */
        async handleServerError(error) {
            if (error.retryable && this.options.enableRetry) {
                const retryResult = await this.attemptRetry(error);
                if (retryResult.success) {
                    return { 
                        handled: true, 
                        retry: true, 
                        attempt: retryResult.attempt,
                        message: 'Retrying request...'
                    };
                }
            }

            return { 
                handled: true, 
                retry: false,
                message: 'A server error occurred'
            };
        }

        /**
         * Handle generic errors
         */
        async handleGenericError(error) {
            return { 
                handled: true, 
                retry: false,
                message: 'An unexpected error occurred'
            };
        }

        /**
         * Attempt to retry operation
         */
        async attemptRetry(error) {
            const errorKey = this.getErrorKey(error);
            const currentRetries = this.retryCount.get(errorKey) || 0;

            if (currentRetries >= this.options.maxRetries) {
                return { success: false, reason: 'max_retries_exceeded' };
            }

            this.retryCount.set(errorKey, currentRetries + 1);

            // Calculate delay with exponential backoff
            const delay = this.options.retryDelay * Math.pow(2, currentRetries);
            
            // Use retryAfter from server if available
            const serverDelay = error.retryAfter ? this.parseRetryAfter(error.retryAfter) : null;
            const finalDelay = serverDelay || delay;

            await this.delay(finalDelay);

            return { success: true, attempt: currentRetries + 1, delay: finalDelay };
        }

        /**
         * Parse retry-after header value
         */
        parseRetryAfter(retryAfter) {
            if (typeof retryAfter === 'number') {
                return retryAfter * 1000; // Convert seconds to milliseconds
            }
            
            if (typeof retryAfter === 'string') {
                if (retryAfter.includes('second')) {
                    const match = retryAfter.match(/(\d+)/);
                    return match ? parseInt(match[1]) * 1000 : null;
                }
                if (retryAfter.includes('minute')) {
                    const match = retryAfter.match(/(\d+)/);
                    return match ? parseInt(match[1]) * 60 * 1000 : null;
                }
            }
            
            return null;
        }

        /**
         * Display field-specific errors
         */
        displayFieldErrors(fieldErrors) {
            fieldErrors.forEach(fieldError => {
                const field = document.querySelector(`[name="${fieldError.field}"]`);
                if (field) {
                    this.displayFieldError(field, fieldError);
                }
            });
        }

        /**
         * Display error for specific field
         */
        displayFieldError(field, fieldError) {
            // Remove existing error
            this.clearFieldError(field);

            // Add error class
            field.classList.add('is-invalid');

            // Create error message
            const errorElement = document.createElement('div');
            errorElement.className = 'invalid-feedback';
            errorElement.innerHTML = this.formatFieldError(fieldError);

            // Insert after field
            field.parentNode.appendChild(errorElement);
        }

        /**
         * Format field error message
         */
        formatFieldError(fieldError) {
            let html = `<strong>${fieldError.message}</strong>`;
            
            if (fieldError.suggestions && fieldError.suggestions.length > 0) {
                html += '<ul class="mt-1 mb-0">';
                fieldError.suggestions.forEach(suggestion => {
                    html += `<li><small>${suggestion}</small></li>`;
                });
                html += '</ul>';
            }

            return html;
        }

        /**
         * Clear field error
         */
        clearFieldError(field) {
            field.classList.remove('is-invalid');
            const errorElement = field.parentNode.querySelector('.invalid-feedback');
            if (errorElement) {
                errorElement.remove();
            }
        }

        /**
         * Show error notification
         */
        showErrorNotification(error, result) {
            const notification = this.createNotification(error, result);
            this.notificationContainer.appendChild(notification);

            // Auto-dismiss after delay
            const dismissDelay = this.getDismissDelay(error.severity);
            setTimeout(() => {
                this.dismissNotification(notification);
            }, dismissDelay);
        }

        /**
         * Create notification element
         */
        createNotification(error, result) {
            const notification = document.createElement('div');
            notification.className = `alert alert-${this.getAlertClass(error.severity)} alert-dismissible fade show error-notification`;
            notification.style.cssText = `
                pointer-events: auto;
                margin-bottom: 10px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                border-left: 4px solid ${this.getBorderColor(error.severity)};
            `;

            const title = this.getErrorTitle(error.category);
            const message = result.message || error.message;

            let html = `
                <div class="d-flex align-items-start">
                    <div class="flex-grow-1">
                        <strong>${title}</strong><br>
                        <span>${message}</span>
            `;

            // Add suggestions
            if (error.suggestions && error.suggestions.length > 0) {
                html += '<div class="mt-2"><small>';
                html += '<strong>Suggestions:</strong><ul class="mb-0">';
                error.suggestions.forEach(suggestion => {
                    html += `<li>${suggestion}</li>`;
                });
                html += '</ul></small></div>';
            }

            html += '</div>';

            // Add action buttons
            const actions = this.getNotificationActions(error, result);
            if (actions.length > 0) {
                html += '<div class="ms-3">';
                actions.forEach(action => {
                    html += `<button type="button" class="btn btn-sm ${action.class}" onclick="${action.onclick}">${action.label}</button>`;
                });
                html += '</div>';
            }

            html += `
                    <button type="button" class="btn-close" onclick="errorHandler.dismissNotification(this.closest('.error-notification'))"></button>
                </div>
            `;

            notification.innerHTML = html;
            return notification;
        }

        /**
         * Get notification actions
         */
        getNotificationActions(error, result) {
            const actions = [];

            if (result.retry) {
                actions.push({
                    label: 'Retry',
                    class: 'btn-outline-primary',
                    onclick: 'errorHandler.retryLastOperation()'
                });
            }

            if (result.fallback && result.fallbackOptions) {
                actions.push({
                    label: 'Options',
                    class: 'btn-outline-secondary',
                    onclick: 'errorHandler.showFallbackOptions()'
                });
            }

            return actions;
        }

        /**
         * Get error title based on category
         */
        getErrorTitle(category) {
            const titles = {
                [ErrorCategory.VALIDATION]: 'Validation Error',
                [ErrorCategory.SERVICE_UNAVAILABLE]: 'Service Unavailable',
                [ErrorCategory.NETWORK]: 'Connection Problem',
                [ErrorCategory.AUTHENTICATION]: 'Authentication Required',
                [ErrorCategory.AUTHORIZATION]: 'Access Denied',
                [ErrorCategory.NOT_FOUND]: 'Not Found',
                [ErrorCategory.SERVER_ERROR]: 'Server Error',
                [ErrorCategory.CLIENT_ERROR]: 'Error'
            };
            return titles[category] || 'Error';
        }

        /**
         * Get alert class for severity
         */
        getAlertClass(severity) {
            const classes = {
                [ErrorSeverity.INFO]: 'info',
                [ErrorSeverity.WARNING]: 'warning',
                [ErrorSeverity.ERROR]: 'danger',
                [ErrorSeverity.CRITICAL]: 'danger'
            };
            return classes[severity] || 'secondary';
        }

        /**
         * Get border color for severity
         */
        getBorderColor(severity) {
            const colors = {
                [ErrorSeverity.INFO]: '#17a2b8',
                [ErrorSeverity.WARNING]: '#ffc107',
                [ErrorSeverity.ERROR]: '#dc3545',
                [ErrorSeverity.CRITICAL]: '#dc3545'
            };
            return colors[severity] || '#6c757d';
        }

        /**
         * Get dismiss delay based on severity
         */
        getDismissDelay(severity) {
            const delays = {
                [ErrorSeverity.INFO]: 3000,
                [ErrorSeverity.WARNING]: 5000,
                [ErrorSeverity.ERROR]: 7000,
                [ErrorSeverity.CRITICAL]: 10000
            };
            return delays[severity] || 5000;
        }

        /**
         * Dismiss notification
         */
        dismissNotification(notification) {
            if (notification && notification.parentNode) {
                notification.classList.remove('show');
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.parentNode.removeChild(notification);
                    }
                }, 150);
            }
        }

        /**
         * Handle network restoration
         */
        handleNetworkRestore() {
            // Clear network-related retry counts
            for (const [key, value] of this.retryCount.entries()) {
                if (key.includes('network')) {
                    this.retryCount.delete(key);
                }
            }

            // Show restoration notification
            this.showInfoNotification('Network connection restored');
        }

        /**
         * Handle network loss
         */
        handleNetworkLoss() {
            this.showWarningNotification('Network connection lost - some features may not work');
        }

        /**
         * Show info notification
         */
        showInfoNotification(message) {
            const error = {
                category: ErrorCategory.CLIENT_ERROR,
                severity: ErrorSeverity.INFO,
                message: message,
                suggestions: []
            };
            const result = { handled: true, retry: false };
            this.showErrorNotification(error, result);
        }

        /**
         * Show warning notification
         */
        showWarningNotification(message) {
            const error = {
                category: ErrorCategory.CLIENT_ERROR,
                severity: ErrorSeverity.WARNING,
                message: message,
                suggestions: []
            };
            const result = { handled: true, retry: false };
            this.showErrorNotification(error, result);
        }

        /**
         * Get error key for retry tracking
         */
        getErrorKey(error) {
            return `${error.category}_${error.errorCode}_${JSON.stringify(error.context)}`;
        }

        /**
         * Log error
         */
        logError(error) {
            const logData = {
                timestamp: error.context.timestamp,
                category: error.category,
                severity: error.severity,
                errorCode: error.errorCode,
                message: error.message,
                context: error.context
            };

            if (error.severity === ErrorSeverity.CRITICAL) {
                console.error('CRITICAL ERROR:', logData);
            } else if (error.severity === ErrorSeverity.ERROR) {
                console.error('ERROR:', logData);
            } else {
                console.warn('WARNING:', logData);
            }
        }

        /**
         * Add error to history
         */
        addToHistory(error) {
            this.errorHistory.push({
                timestamp: error.context.timestamp,
                category: error.category,
                severity: error.severity,
                errorCode: error.errorCode,
                message: error.message,
                context: error.context
            });

            // Keep only last 50 errors
            if (this.errorHistory.length > 50) {
                this.errorHistory.shift();
            }
        }

        /**
         * Delay utility
         */
        delay(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        /**
         * Get error statistics
         */
        getErrorStatistics() {
            const stats = {
                total: this.errorHistory.length,
                byCategory: {},
                bySeverity: {},
                recent: this.errorHistory.slice(-10)
            };

            this.errorHistory.forEach(error => {
                stats.byCategory[error.category] = (stats.byCategory[error.category] || 0) + 1;
                stats.bySeverity[error.severity] = (stats.bySeverity[error.severity] || 0) + 1;
            });

            return stats;
        }

        /**
         * Clear error history
         */
        clearHistory() {
            this.errorHistory = [];
            this.retryCount.clear();
        }
    }

    // Create global error handler instance
    window.errorHandler = new ErrorHandler();

    // Export classes for use by other modules
    window.ErrorHandler = ErrorHandler;
    window.ErrorCategory = ErrorCategory;
    window.ErrorSeverity = ErrorSeverity;

})(window);