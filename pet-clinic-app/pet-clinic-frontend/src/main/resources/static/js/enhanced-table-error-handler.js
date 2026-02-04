/**
 * Enhanced Table Error Handler
 * Comprehensive error handling system for enhanced table operations
 * Provides user-friendly error messages, recovery options, and fallback mechanisms
 * Requirements: 2.4, 4.2
 */

(function(window) {
    'use strict';

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
     * Error categories for different handling strategies
     */
    const ErrorCategory = {
        NETWORK: 'network',
        VALIDATION: 'validation',
        SERVER: 'server',
        CLIENT: 'client',
        TIMEOUT: 'timeout',
        PERMISSION: 'permission'
    };

    /**
     * Enhanced error class with recovery options
     */
    class EnhancedTableError extends Error {
        constructor(message, options = {}) {
            super(message);
            this.name = 'EnhancedTableError';
            this.code = options.code || 'UNKNOWN_ERROR';
            this.severity = options.severity || ErrorSeverity.ERROR;
            this.category = options.category || ErrorCategory.CLIENT;
            this.userMessage = options.userMessage || message;
            this.recoveryOptions = options.recoveryOptions || [];
            this.retryable = options.retryable !== false;
            this.fallbackAvailable = options.fallbackAvailable || false;
            this.details = options.details || null;
            this.timestamp = new Date();
            this.context = options.context || {};
        }

        /**
         * Check if error is retryable
         */
        isRetryable() {
            return this.retryable && this.category !== ErrorCategory.VALIDATION;
        }

        /**
         * Check if fallback is available
         */
        hasFallback() {
            return this.fallbackAvailable;
        }

        /**
         * Get user-friendly error message
         */
        getUserMessage() {
            return this.userMessage || this.message;
        }

        /**
         * Get recovery options
         */
        getRecoveryOptions() {
            return this.recoveryOptions || [];
        }
    }

    /**
     * Error handler class for enhanced table operations
     */
    class EnhancedTableErrorHandler {
        constructor(options = {}) {
            this.options = {
                showNotifications: options.showNotifications !== false,
                enableFallback: options.enableFallback !== false,
                maxRetries: options.maxRetries || 3,
                retryDelay: options.retryDelay || 1000,
                logErrors: options.logErrors !== false,
                ...options
            };

            this.errorHistory = [];
            this.fallbackActive = false;
            this.retryCount = new Map();
            
            this.initializeErrorHandling();
        }

        /**
         * Initialize error handling system
         */
        initializeErrorHandling() {
            // Set up global error handlers
            window.addEventListener('unhandledrejection', (event) => {
                if (this.isEnhancedTableError(event.reason)) {
                    this.handleError(event.reason);
                    event.preventDefault();
                }
            });

            // Set up network error detection
            window.addEventListener('online', () => {
                this.handleNetworkRestore();
            });

            window.addEventListener('offline', () => {
                this.handleNetworkLoss();
            });
        }

        /**
         * Main error handling method
         */
        async handleError(error, context = {}) {
            if (!error) return;

            // Use the new integrated error handling system if available
            if (window.errorIntegration && window.errorIntegration.isInitialized()) {
                return await window.errorIntegration.handleError(error, context);
            }

            // Fallback to existing error handling
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
         * Enhance error with additional information
         */
        enhanceError(error, context = {}) {
            if (error instanceof EnhancedTableError) {
                error.context = { ...error.context, ...context };
                return error;
            }

            // Convert API errors
            if (window.ApiError && error instanceof window.ApiError) {
                return this.convertApiError(error, context);
            }

            // Convert generic errors
            return this.convertGenericError(error, context);
        }

        /**
         * Convert API error to enhanced error
         */
        convertApiError(apiError, context = {}) {
            const errorData = apiError.response || {};
            
            let category = ErrorCategory.SERVER;
            let severity = ErrorSeverity.ERROR;
            let fallbackAvailable = false;
            let recoveryOptions = [];

            // Determine category based on status code
            if (apiError.isNetworkError()) {
                category = ErrorCategory.NETWORK;
                fallbackAvailable = true;
                recoveryOptions = [
                    'Check your internet connection',
                    'Try again in a few moments',
                    'Use offline mode if available'
                ];
            } else if (apiError.isClientError()) {
                category = ErrorCategory.VALIDATION;
                severity = ErrorSeverity.WARNING;
                recoveryOptions = [
                    'Check your input parameters',
                    'Try a different approach',
                    'Clear filters and try again'
                ];
            } else if (apiError.isServerError()) {
                category = ErrorCategory.SERVER;
                fallbackAvailable = true;
                recoveryOptions = [
                    'Try again in a few moments',
                    'Use simplified view if available',
                    'Contact support if problem persists'
                ];
            }

            // Extract user message from response
            let userMessage = 'An error occurred while processing your request';
            if (errorData.userMessage) {
                userMessage = errorData.userMessage;
            } else if (errorData.message) {
                userMessage = errorData.message;
            }

            // Extract recovery options from response
            if (errorData.recoveryOptions && Array.isArray(errorData.recoveryOptions)) {
                recoveryOptions = [...recoveryOptions, ...errorData.recoveryOptions];
            }

            return new EnhancedTableError(apiError.message, {
                code: errorData.errorCode || 'API_ERROR',
                severity,
                category,
                userMessage,
                recoveryOptions,
                retryable: apiError.isRetryable(),
                fallbackAvailable,
                details: errorData,
                context
            });
        }

        /**
         * Convert generic error to enhanced error
         */
        convertGenericError(error, context = {}) {
            let category = ErrorCategory.CLIENT;
            let severity = ErrorSeverity.ERROR;
            let recoveryOptions = ['Refresh the page and try again'];

            // Detect specific error types
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                category = ErrorCategory.NETWORK;
                recoveryOptions = [
                    'Check your internet connection',
                    'Try again in a few moments'
                ];
            } else if (error.name === 'TimeoutError') {
                category = ErrorCategory.TIMEOUT;
                recoveryOptions = [
                    'Try again with a smaller dataset',
                    'Use more specific filters'
                ];
            }

            return new EnhancedTableError(error.message, {
                code: error.name || 'GENERIC_ERROR',
                severity,
                category,
                userMessage: 'An unexpected error occurred',
                recoveryOptions,
                retryable: category !== ErrorCategory.CLIENT,
                fallbackAvailable: category === ErrorCategory.NETWORK,
                context
            });
        }

        /**
         * Handle error based on category
         */
        async handleByCategory(error) {
            switch (error.category) {
                case ErrorCategory.NETWORK:
                    return await this.handleNetworkError(error);
                case ErrorCategory.VALIDATION:
                    return await this.handleValidationError(error);
                case ErrorCategory.SERVER:
                    return await this.handleServerError(error);
                case ErrorCategory.TIMEOUT:
                    return await this.handleTimeoutError(error);
                default:
                    return await this.handleGenericError(error);
            }
        }

        /**
         * Handle network errors
         */
        async handleNetworkError(error) {
            if (this.options.enableFallback && error.hasFallback()) {
                return await this.activateFallback(error);
            }

            if (error.isRetryable()) {
                return await this.attemptRetry(error);
            }

            return { handled: true, fallback: false, retry: false };
        }

        /**
         * Handle validation errors
         */
        async handleValidationError(error) {
            // Validation errors are not retryable
            // Focus on providing clear feedback to user
            return { 
                handled: true, 
                fallback: false, 
                retry: false,
                userAction: 'correct_input'
            };
        }

        /**
         * Handle server errors
         */
        async handleServerError(error) {
            if (error.isRetryable()) {
                const retryResult = await this.attemptRetry(error);
                if (retryResult.success) {
                    return retryResult;
                }
            }

            if (this.options.enableFallback && error.hasFallback()) {
                return await this.activateFallback(error);
            }

            return { handled: true, fallback: false, retry: false };
        }

        /**
         * Handle timeout errors
         */
        async handleTimeoutError(error) {
            // Suggest reducing dataset size
            return { 
                handled: true, 
                fallback: false, 
                retry: true,
                suggestion: 'reduce_dataset'
            };
        }

        /**
         * Handle generic errors
         */
        async handleGenericError(error) {
            return { handled: true, fallback: false, retry: false };
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

            // Wait before retry
            await this.delay(this.options.retryDelay * Math.pow(2, currentRetries));

            return { success: true, attempt: currentRetries + 1 };
        }

        /**
         * Activate fallback mechanism
         */
        async activateFallback(error) {
            if (this.fallbackActive) {
                return { success: false, reason: 'fallback_already_active' };
            }

            this.fallbackActive = true;

            // Trigger fallback event
            window.dispatchEvent(new CustomEvent('enhancedTableFallback', {
                detail: { error, timestamp: new Date() }
            }));

            return { success: true, fallback: true };
        }

        /**
         * Handle network restoration
         */
        handleNetworkRestore() {
            if (this.fallbackActive) {
                this.fallbackActive = false;
                
                window.dispatchEvent(new CustomEvent('enhancedTableNetworkRestore', {
                    detail: { timestamp: new Date() }
                }));
            }

            // Clear network-related retry counts
            for (const [key, value] of this.retryCount.entries()) {
                if (key.includes('NETWORK')) {
                    this.retryCount.delete(key);
                }
            }
        }

        /**
         * Handle network loss
         */
        handleNetworkLoss() {
            if (this.options.enableFallback) {
                this.activateFallback(new EnhancedTableError('Network connection lost', {
                    category: ErrorCategory.NETWORK,
                    fallbackAvailable: true
                }));
            }
        }

        /**
         * Show error notification to user
         */
        showErrorNotification(error, result) {
            const notification = {
                type: this.getSeverityClass(error.severity),
                title: this.getErrorTitle(error),
                message: error.getUserMessage(),
                recoveryOptions: error.getRecoveryOptions(),
                actions: this.getNotificationActions(error, result),
                persistent: error.severity === ErrorSeverity.CRITICAL
            };

            // Dispatch notification event
            window.dispatchEvent(new CustomEvent('enhancedTableNotification', {
                detail: notification
            }));
        }

        /**
         * Get error title based on category
         */
        getErrorTitle(error) {
            switch (error.category) {
                case ErrorCategory.NETWORK:
                    return 'Connection Problem';
                case ErrorCategory.VALIDATION:
                    return 'Input Error';
                case ErrorCategory.SERVER:
                    return 'Server Error';
                case ErrorCategory.TIMEOUT:
                    return 'Operation Timeout';
                default:
                    return 'Error';
            }
        }

        /**
         * Get notification actions based on error and result
         */
        getNotificationActions(error, result) {
            const actions = [];

            if (result.retry) {
                actions.push({
                    label: 'Try Again',
                    action: 'retry',
                    primary: true
                });
            }

            if (result.fallback) {
                actions.push({
                    label: 'Use Offline Mode',
                    action: 'fallback',
                    secondary: true
                });
            }

            actions.push({
                label: 'Dismiss',
                action: 'dismiss',
                secondary: true
            });

            return actions;
        }

        /**
         * Get CSS class for severity
         */
        getSeverityClass(severity) {
            switch (severity) {
                case ErrorSeverity.INFO:
                    return 'alert-info';
                case ErrorSeverity.WARNING:
                    return 'alert-warning';
                case ErrorSeverity.ERROR:
                    return 'alert-danger';
                case ErrorSeverity.CRITICAL:
                    return 'alert-danger';
                default:
                    return 'alert-secondary';
            }
        }

        /**
         * Log error for debugging
         */
        logError(error) {
            const logData = {
                timestamp: error.timestamp,
                code: error.code,
                category: error.category,
                severity: error.severity,
                message: error.message,
                userMessage: error.userMessage,
                context: error.context,
                stack: error.stack
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
                timestamp: error.timestamp,
                code: error.code,
                category: error.category,
                severity: error.severity,
                message: error.message,
                context: error.context
            });

            // Keep only last 50 errors
            if (this.errorHistory.length > 50) {
                this.errorHistory.shift();
            }
        }

        /**
         * Get error key for retry tracking
         */
        getErrorKey(error) {
            return `${error.category}_${error.code}_${JSON.stringify(error.context)}`;
        }

        /**
         * Check if error is enhanced table error
         */
        isEnhancedTableError(error) {
            return error instanceof EnhancedTableError || 
                   error instanceof window.ApiError ||
                   (error && error.message && error.message.includes('table'));
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

        /**
         * Reset fallback state
         */
        resetFallback() {
            this.fallbackActive = false;
        }
    }

    // Export to global scope
    window.EnhancedTableError = EnhancedTableError;
    window.EnhancedTableErrorHandler = EnhancedTableErrorHandler;
    window.ErrorSeverity = ErrorSeverity;
    window.ErrorCategory = ErrorCategory;

})(window);