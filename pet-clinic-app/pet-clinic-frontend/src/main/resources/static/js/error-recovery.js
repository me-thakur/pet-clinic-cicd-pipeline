/**
 * Error Recovery Component
 * Provides error recovery suggestions and retry mechanisms for transient errors
 * Requirements: 1.4, 1.5
 */

(function(window) {
    'use strict';

    /**
     * Error recovery strategies
     */
    const RecoveryStrategy = {
        RETRY: 'retry',
        FALLBACK: 'fallback',
        REFRESH: 'refresh',
        REDIRECT: 'redirect',
        USER_ACTION: 'user_action',
        CONTACT_SUPPORT: 'contact_support'
    };

    /**
     * Error recovery component
     */
    class ErrorRecovery {
        constructor(options = {}) {
            this.options = {
                enableAutoRecovery: options.enableAutoRecovery !== false,
                maxAutoRetries: options.maxAutoRetries || 3,
                retryDelay: options.retryDelay || 1000,
                showRecoveryUI: options.showRecoveryUI !== false,
                ...options
            };

            this.recoveryAttempts = new Map();
            this.recoveryStrategies = new Map();
            this.lastFailedOperation = null;
            
            this.init();
        }

        /**
         * Initialize recovery system
         */
        init() {
            this.setupRecoveryStrategies();
            this.setupRecoveryUI();
        }

        /**
         * Setup recovery strategies for different error types
         */
        setupRecoveryStrategies() {
            // Network errors - retry with exponential backoff
            this.recoveryStrategies.set('NETWORK_ERROR', {
                strategy: RecoveryStrategy.RETRY,
                maxAttempts: 3,
                delay: 1000,
                backoff: true,
                suggestions: [
                    'Check your internet connection',
                    'Try again in a few moments',
                    'Switch to a different network if available'
                ]
            });

            // Service unavailable - retry with longer delays
            this.recoveryStrategies.set('SERVICE_UNAVAILABLE', {
                strategy: RecoveryStrategy.RETRY,
                maxAttempts: 5,
                delay: 5000,
                backoff: true,
                fallback: true,
                suggestions: [
                    'Service is temporarily unavailable',
                    'Please wait while we retry your request',
                    'Try using offline mode if available'
                ]
            });

            // Server errors - retry with fallback
            this.recoveryStrategies.set('SERVER_ERROR', {
                strategy: RecoveryStrategy.RETRY,
                maxAttempts: 2,
                delay: 2000,
                fallback: true,
                suggestions: [
                    'Server encountered an error',
                    'Retrying your request automatically',
                    'Contact support if the problem persists'
                ]
            });

            // Authentication errors - redirect to login
            this.recoveryStrategies.set('AUTHENTICATION_FAILED', {
                strategy: RecoveryStrategy.REDIRECT,
                redirectUrl: '/login',
                delay: 3000,
                suggestions: [
                    'Your session has expired',
                    'Please log in again to continue',
                    'You will be redirected to the login page'
                ]
            });

            // Authorization errors - show contact support
            this.recoveryStrategies.set('ACCESS_DENIED', {
                strategy: RecoveryStrategy.CONTACT_SUPPORT,
                suggestions: [
                    'You don\'t have permission to perform this action',
                    'Contact your administrator for access',
                    'Make sure you\'re logged in with the correct account'
                ]
            });

            // Validation errors - user action required
            this.recoveryStrategies.set('VALIDATION_FAILED', {
                strategy: RecoveryStrategy.USER_ACTION,
                suggestions: [
                    'Please correct the highlighted fields',
                    'All required fields must be completed',
                    'Check for typos and formatting errors'
                ]
            });

            // Connection timeout - retry with longer timeout
            this.recoveryStrategies.set('REQUEST_TIMEOUT', {
                strategy: RecoveryStrategy.RETRY,
                maxAttempts: 2,
                delay: 3000,
                suggestions: [
                    'Request timed out',
                    'Retrying with extended timeout',
                    'Try using fewer filters to reduce data size'
                ]
            });
        }

        /**
         * Setup recovery UI components
         */
        setupRecoveryUI() {
            if (!this.options.showRecoveryUI) return;

            // Create recovery modal
            this.createRecoveryModal();
            
            // Create recovery notification area
            this.createRecoveryNotificationArea();
        }

        /**
         * Create recovery modal for complex recovery scenarios
         */
        createRecoveryModal() {
            if (document.getElementById('error-recovery-modal')) return;

            const modal = document.createElement('div');
            modal.id = 'error-recovery-modal';
            modal.className = 'modal fade';
            modal.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-exclamation-triangle text-warning"></i>
                                Error Recovery
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div id="recovery-content">
                                <!-- Recovery content will be inserted here -->
                            </div>
                        </div>
                        <div class="modal-footer">
                            <div id="recovery-actions">
                                <!-- Recovery actions will be inserted here -->
                            </div>
                        </div>
                    </div>
                </div>
            `;

            document.body.appendChild(modal);
        }

        /**
         * Create recovery notification area
         */
        createRecoveryNotificationArea() {
            if (document.getElementById('recovery-notifications')) return;

            const container = document.createElement('div');
            container.id = 'recovery-notifications';
            container.className = 'recovery-notifications-container';
            container.style.cssText = `
                position: fixed;
                bottom: 20px;
                left: 20px;
                z-index: 9998;
                max-width: 400px;
                pointer-events: none;
            `;

            document.body.appendChild(container);
        }

        /**
         * Attempt error recovery
         */
        async attemptRecovery(error, context = {}) {
            const errorCode = error.errorCode || error.name || 'UNKNOWN_ERROR';
            const strategy = this.recoveryStrategies.get(errorCode);

            if (!strategy) {
                return this.handleUnknownError(error, context);
            }

            // Store the failed operation for potential retry
            this.lastFailedOperation = { error, context, timestamp: new Date() };

            // Track recovery attempts
            const attemptKey = this.getAttemptKey(error, context);
            const currentAttempts = this.recoveryAttempts.get(attemptKey) || 0;

            switch (strategy.strategy) {
                case RecoveryStrategy.RETRY:
                    return await this.attemptRetry(error, context, strategy, currentAttempts);
                case RecoveryStrategy.FALLBACK:
                    return await this.attemptFallback(error, context, strategy);
                case RecoveryStrategy.REFRESH:
                    return await this.attemptRefresh(error, context, strategy);
                case RecoveryStrategy.REDIRECT:
                    return await this.attemptRedirect(error, context, strategy);
                case RecoveryStrategy.USER_ACTION:
                    return await this.requestUserAction(error, context, strategy);
                case RecoveryStrategy.CONTACT_SUPPORT:
                    return await this.showContactSupport(error, context, strategy);
                default:
                    return this.handleUnknownError(error, context);
            }
        }

        /**
         * Attempt retry recovery
         */
        async attemptRetry(error, context, strategy, currentAttempts) {
            if (currentAttempts >= strategy.maxAttempts) {
                return {
                    success: false,
                    reason: 'max_attempts_exceeded',
                    nextStrategy: strategy.fallback ? RecoveryStrategy.FALLBACK : RecoveryStrategy.CONTACT_SUPPORT
                };
            }

            const attemptKey = this.getAttemptKey(error, context);
            this.recoveryAttempts.set(attemptKey, currentAttempts + 1);

            // Calculate delay with optional exponential backoff
            let delay = strategy.delay;
            if (strategy.backoff) {
                delay = strategy.delay * Math.pow(2, currentAttempts);
            }

            // Show retry notification
            this.showRetryNotification(error, currentAttempts + 1, strategy.maxAttempts, delay);

            // Wait for delay
            await this.delay(delay);

            // Attempt to retry the original operation
            try {
                const result = await this.retryOperation(context);
                
                // Success - clear retry count
                this.recoveryAttempts.delete(attemptKey);
                this.showRecoverySuccessNotification('Operation completed successfully');
                
                return {
                    success: true,
                    result: result,
                    attempts: currentAttempts + 1
                };
            } catch (retryError) {
                // Retry failed - try again or move to next strategy
                if (currentAttempts + 1 >= strategy.maxAttempts) {
                    if (strategy.fallback) {
                        return await this.attemptFallback(error, context, strategy);
                    } else {
                        return {
                            success: false,
                            reason: 'retry_failed',
                            error: retryError,
                            nextStrategy: RecoveryStrategy.CONTACT_SUPPORT
                        };
                    }
                } else {
                    // Try again
                    return await this.attemptRetry(error, context, strategy, currentAttempts + 1);
                }
            }
        }

        /**
         * Attempt fallback recovery
         */
        async attemptFallback(error, context, strategy) {
            this.showFallbackNotification(error, strategy);

            // Trigger fallback mode
            window.dispatchEvent(new CustomEvent('errorRecoveryFallback', {
                detail: { error, context, strategy }
            }));

            return {
                success: true,
                fallback: true,
                message: 'Switched to fallback mode'
            };
        }

        /**
         * Attempt refresh recovery
         */
        async attemptRefresh(error, context, strategy) {
            this.showRefreshNotification(strategy);

            // Wait for delay if specified
            if (strategy.delay) {
                await this.delay(strategy.delay);
            }

            // Refresh the page
            window.location.reload();

            return {
                success: true,
                refresh: true,
                message: 'Page will be refreshed'
            };
        }

        /**
         * Attempt redirect recovery
         */
        async attemptRedirect(error, context, strategy) {
            this.showRedirectNotification(strategy);

            // Wait for delay if specified
            if (strategy.delay) {
                await this.delay(strategy.delay);
            }

            // Redirect to specified URL
            window.location.href = strategy.redirectUrl;

            return {
                success: true,
                redirect: true,
                url: strategy.redirectUrl,
                message: 'Redirecting...'
            };
        }

        /**
         * Request user action
         */
        async requestUserAction(error, context, strategy) {
            this.showUserActionModal(error, strategy);

            return {
                success: true,
                userAction: true,
                message: 'User action required'
            };
        }

        /**
         * Show contact support
         */
        async showContactSupport(error, context, strategy) {
            this.showContactSupportModal(error, strategy);

            return {
                success: true,
                contactSupport: true,
                message: 'Please contact support'
            };
        }

        /**
         * Handle unknown error
         */
        handleUnknownError(error, context) {
            this.showGenericRecoveryModal(error);

            return {
                success: false,
                reason: 'unknown_error',
                message: 'Unknown error - manual intervention required'
            };
        }

        /**
         * Retry the original operation
         */
        async retryOperation(context) {
            if (context.retryFunction && typeof context.retryFunction === 'function') {
                return await context.retryFunction();
            }

            if (context.url && context.options) {
                return await fetch(context.url, context.options);
            }

            throw new Error('No retry mechanism available');
        }

        /**
         * Show retry notification
         */
        showRetryNotification(error, attempt, maxAttempts, delay) {
            const notification = this.createRecoveryNotification({
                type: 'info',
                title: 'Retrying Operation',
                message: `Attempt ${attempt} of ${maxAttempts} - retrying in ${Math.round(delay / 1000)} seconds`,
                icon: 'fas fa-sync-alt fa-spin',
                autoHide: true,
                hideDelay: delay + 1000
            });

            this.showRecoveryNotification(notification);
        }

        /**
         * Show fallback notification
         */
        showFallbackNotification(error, strategy) {
            const notification = this.createRecoveryNotification({
                type: 'warning',
                title: 'Fallback Mode Active',
                message: 'Some features may be limited while in fallback mode',
                icon: 'fas fa-shield-alt',
                suggestions: strategy.suggestions,
                autoHide: false,
                actions: [
                    {
                        label: 'Try Again',
                        class: 'btn-outline-primary',
                        onclick: () => this.retryLastOperation()
                    }
                ]
            });

            this.showRecoveryNotification(notification);
        }

        /**
         * Show refresh notification
         */
        showRefreshNotification(strategy) {
            const notification = this.createRecoveryNotification({
                type: 'info',
                title: 'Refreshing Page',
                message: 'The page will be refreshed to resolve the issue',
                icon: 'fas fa-refresh fa-spin',
                suggestions: strategy.suggestions,
                autoHide: true,
                hideDelay: strategy.delay || 3000
            });

            this.showRecoveryNotification(notification);
        }

        /**
         * Show redirect notification
         */
        showRedirectNotification(strategy) {
            const notification = this.createRecoveryNotification({
                type: 'info',
                title: 'Redirecting',
                message: `You will be redirected to ${strategy.redirectUrl}`,
                icon: 'fas fa-external-link-alt',
                suggestions: strategy.suggestions,
                autoHide: true,
                hideDelay: strategy.delay || 3000
            });

            this.showRecoveryNotification(notification);
        }

        /**
         * Show recovery success notification
         */
        showRecoverySuccessNotification(message) {
            const notification = this.createRecoveryNotification({
                type: 'success',
                title: 'Recovery Successful',
                message: message,
                icon: 'fas fa-check-circle',
                autoHide: true,
                hideDelay: 3000
            });

            this.showRecoveryNotification(notification);
        }

        /**
         * Show user action modal
         */
        showUserActionModal(error, strategy) {
            const modal = document.getElementById('error-recovery-modal');
            const content = modal.querySelector('#recovery-content');
            const actions = modal.querySelector('#recovery-actions');

            content.innerHTML = `
                <div class="alert alert-warning">
                    <h6><i class="fas fa-user-edit"></i> Action Required</h6>
                    <p>${error.message}</p>
                    ${this.formatSuggestions(strategy.suggestions)}
                </div>
            `;

            actions.innerHTML = `
                <button type="button" class="btn btn-primary" data-bs-dismiss="modal">
                    I'll Fix This
                </button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    Cancel
                </button>
            `;

            // Show modal
            const bootstrapModal = new bootstrap.Modal(modal);
            bootstrapModal.show();
        }

        /**
         * Show contact support modal
         */
        showContactSupportModal(error, strategy) {
            const modal = document.getElementById('error-recovery-modal');
            const content = modal.querySelector('#recovery-content');
            const actions = modal.querySelector('#recovery-actions');

            content.innerHTML = `
                <div class="alert alert-info">
                    <h6><i class="fas fa-life-ring"></i> Support Needed</h6>
                    <p>${error.message}</p>
                    ${this.formatSuggestions(strategy.suggestions)}
                    
                    <div class="mt-3">
                        <strong>Error Details:</strong>
                        <ul class="mb-0">
                            <li><strong>Error Code:</strong> ${error.errorCode || 'N/A'}</li>
                            <li><strong>Time:</strong> ${new Date().toLocaleString()}</li>
                            <li><strong>Page:</strong> ${window.location.pathname}</li>
                        </ul>
                    </div>
                </div>
            `;

            actions.innerHTML = `
                <button type="button" class="btn btn-primary" onclick="window.open('mailto:support@petclinic.com?subject=Error Report&body=Error Code: ${error.errorCode}%0ATime: ${new Date().toISOString()}%0APage: ${window.location.href}')">
                    <i class="fas fa-envelope"></i> Contact Support
                </button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    Close
                </button>
            `;

            // Show modal
            const bootstrapModal = new bootstrap.Modal(modal);
            bootstrapModal.show();
        }

        /**
         * Show generic recovery modal
         */
        showGenericRecoveryModal(error) {
            const modal = document.getElementById('error-recovery-modal');
            const content = modal.querySelector('#recovery-content');
            const actions = modal.querySelector('#recovery-actions');

            content.innerHTML = `
                <div class="alert alert-danger">
                    <h6><i class="fas fa-exclamation-triangle"></i> Unexpected Error</h6>
                    <p>${error.message}</p>
                    
                    <div class="mt-3">
                        <strong>What you can try:</strong>
                        <ul>
                            <li>Refresh the page</li>
                            <li>Clear your browser cache</li>
                            <li>Try a different browser</li>
                            <li>Contact support if the problem persists</li>
                        </ul>
                    </div>
                </div>
            `;

            actions.innerHTML = `
                <button type="button" class="btn btn-primary" onclick="location.reload()">
                    <i class="fas fa-refresh"></i> Refresh Page
                </button>
                <button type="button" class="btn btn-outline-primary" onclick="window.open('mailto:support@petclinic.com')">
                    <i class="fas fa-envelope"></i> Contact Support
                </button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    Close
                </button>
            `;

            // Show modal
            const bootstrapModal = new bootstrap.Modal(modal);
            bootstrapModal.show();
        }

        /**
         * Create recovery notification
         */
        createRecoveryNotification(options) {
            const notification = document.createElement('div');
            notification.className = `alert alert-${options.type} recovery-notification`;
            notification.style.cssText = `
                pointer-events: auto;
                margin-bottom: 10px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                border-radius: 8px;
            `;

            let html = `
                <div class="d-flex align-items-start">
                    <div class="me-3">
                        <i class="${options.icon}"></i>
                    </div>
                    <div class="flex-grow-1">
                        <strong>${options.title}</strong><br>
                        <span>${options.message}</span>
            `;

            if (options.suggestions && options.suggestions.length > 0) {
                html += this.formatSuggestions(options.suggestions);
            }

            html += '</div>';

            if (options.actions && options.actions.length > 0) {
                html += '<div class="ms-3">';
                options.actions.forEach(action => {
                    html += `<button type="button" class="btn btn-sm ${action.class}" onclick="${action.onclick}">${action.label}</button>`;
                });
                html += '</div>';
            }

            if (!options.autoHide) {
                html += `<button type="button" class="btn-close" onclick="this.closest('.recovery-notification').remove()"></button>`;
            }

            html += '</div>';

            notification.innerHTML = html;

            // Auto-hide if specified
            if (options.autoHide) {
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.remove();
                    }
                }, options.hideDelay || 5000);
            }

            return notification;
        }

        /**
         * Show recovery notification
         */
        showRecoveryNotification(notification) {
            const container = document.getElementById('recovery-notifications');
            if (container) {
                container.appendChild(notification);
            }
        }

        /**
         * Format suggestions as HTML
         */
        formatSuggestions(suggestions) {
            if (!suggestions || suggestions.length === 0) return '';

            let html = '<div class="mt-2"><small><strong>Suggestions:</strong><ul class="mb-0">';
            suggestions.forEach(suggestion => {
                html += `<li>${suggestion}</li>`;
            });
            html += '</ul></small></div>';

            return html;
        }

        /**
         * Retry last operation
         */
        async retryLastOperation() {
            if (!this.lastFailedOperation) {
                console.warn('No failed operation to retry');
                return;
            }

            try {
                const result = await this.retryOperation(this.lastFailedOperation.context);
                this.showRecoverySuccessNotification('Operation completed successfully');
                return result;
            } catch (error) {
                console.error('Retry failed:', error);
                await this.attemptRecovery(error, this.lastFailedOperation.context);
            }
        }

        /**
         * Get attempt key for tracking
         */
        getAttemptKey(error, context) {
            return `${error.errorCode || error.name}_${context.url || 'unknown'}_${JSON.stringify(context.options || {})}`;
        }

        /**
         * Delay utility
         */
        delay(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        /**
         * Clear recovery attempts
         */
        clearRecoveryAttempts() {
            this.recoveryAttempts.clear();
            this.lastFailedOperation = null;
        }

        /**
         * Get recovery statistics
         */
        getRecoveryStatistics() {
            return {
                totalAttempts: Array.from(this.recoveryAttempts.values()).reduce((sum, count) => sum + count, 0),
                activeAttempts: this.recoveryAttempts.size,
                lastFailedOperation: this.lastFailedOperation,
                strategies: Array.from(this.recoveryStrategies.keys())
            };
        }
    }

    // Create global error recovery instance
    window.errorRecovery = new ErrorRecovery();

    // Export for use by other modules
    window.ErrorRecovery = ErrorRecovery;
    window.RecoveryStrategy = RecoveryStrategy;

})(window);