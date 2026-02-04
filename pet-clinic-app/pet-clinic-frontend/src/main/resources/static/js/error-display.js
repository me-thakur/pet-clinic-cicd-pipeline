/**
 * Enhanced Error Display Component
 * Integrates error handling, categorization, and recovery for comprehensive error display
 * Requirements: 1.3, 1.4, 1.5
 */

(function(window) {
    'use strict';

    /**
     * Enhanced error display component
     */
    class ErrorDisplay {
        constructor(options = {}) {
            this.options = {
                enableAnimations: options.enableAnimations !== false,
                enableAccessibility: options.enableAccessibility !== false,
                enableAnalytics: options.enableAnalytics !== false,
                theme: options.theme || 'default',
                position: options.position || 'top-right',
                maxDisplayedErrors: options.maxDisplayedErrors || 5,
                ...options
            };

            this.activeErrors = new Map();
            this.errorQueue = [];
            this.displayedCount = 0;
            this.analytics = {
                totalErrors: 0,
                errorsByType: {},
                errorsByPage: {},
                recoveryAttempts: 0,
                successfulRecoveries: 0
            };

            this.init();
        }

        /**
         * Initialize error display system
         */
        init() {
            this.setupStyles();
            this.setupEventListeners();
            this.setupAccessibility();
            this.setupAnalytics();
        }

        /**
         * Setup error display styles
         */
        setupStyles() {
            if (document.getElementById('error-display-styles')) return;

            const styles = document.createElement('style');
            styles.id = 'error-display-styles';
            styles.textContent = `
                /* Error display container */
                .error-display-container {
                    position: fixed;
                    z-index: 10000;
                    pointer-events: none;
                    max-width: 400px;
                }

                .error-display-container.top-right {
                    top: 20px;
                    right: 20px;
                }

                .error-display-container.top-left {
                    top: 20px;
                    left: 20px;
                }

                .error-display-container.bottom-right {
                    bottom: 20px;
                    right: 20px;
                }

                .error-display-container.bottom-left {
                    bottom: 20px;
                    left: 20px;
                }

                .error-display-container.center {
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    max-width: 600px;
                }

                /* Error display item */
                .error-display-item {
                    pointer-events: auto;
                    margin-bottom: 10px;
                    border-radius: 8px;
                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                    overflow: hidden;
                    transition: all 0.3s ease;
                    position: relative;
                }

                .error-display-item.entering {
                    animation: errorSlideIn 0.3s ease-out;
                }

                .error-display-item.exiting {
                    animation: errorSlideOut 0.3s ease-in;
                }

                @keyframes errorSlideIn {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }

                @keyframes errorSlideOut {
                    from {
                        transform: translateX(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                }

                /* Error severity indicators */
                .error-display-item::before {
                    content: '';
                    position: absolute;
                    left: 0;
                    top: 0;
                    bottom: 0;
                    width: 4px;
                    background: var(--error-severity-color);
                }

                .error-display-item.severity-info::before {
                    background: #17a2b8;
                }

                .error-display-item.severity-warning::before {
                    background: #ffc107;
                }

                .error-display-item.severity-error::before {
                    background: #dc3545;
                }

                .error-display-item.severity-critical::before {
                    background: #721c24;
                }

                /* Error content */
                .error-display-content {
                    padding: 16px;
                    padding-left: 20px;
                }

                .error-display-header {
                    display: flex;
                    align-items: flex-start;
                    margin-bottom: 8px;
                }

                .error-display-icon {
                    margin-right: 12px;
                    font-size: 18px;
                    flex-shrink: 0;
                    margin-top: 2px;
                }

                .error-display-title {
                    font-weight: 600;
                    font-size: 14px;
                    line-height: 1.4;
                    margin: 0;
                    flex-grow: 1;
                }

                .error-display-message {
                    font-size: 13px;
                    line-height: 1.4;
                    margin-bottom: 12px;
                    color: #666;
                }

                .error-display-suggestions {
                    font-size: 12px;
                    margin-bottom: 12px;
                }

                .error-display-suggestions ul {
                    margin: 4px 0 0 0;
                    padding-left: 16px;
                }

                .error-display-suggestions li {
                    margin-bottom: 2px;
                }

                /* Error actions */
                .error-display-actions {
                    display: flex;
                    gap: 8px;
                    margin-top: 12px;
                }

                .error-display-action {
                    padding: 4px 12px;
                    font-size: 12px;
                    border-radius: 4px;
                    border: 1px solid #ddd;
                    background: #fff;
                    cursor: pointer;
                    transition: all 0.2s ease;
                }

                .error-display-action:hover {
                    background: #f8f9fa;
                    border-color: #adb5bd;
                }

                .error-display-action.primary {
                    background: #007bff;
                    border-color: #007bff;
                    color: #fff;
                }

                .error-display-action.primary:hover {
                    background: #0056b3;
                    border-color: #0056b3;
                }

                /* Progress indicator */
                .error-display-progress {
                    position: absolute;
                    bottom: 0;
                    left: 0;
                    height: 2px;
                    background: rgba(0, 0, 0, 0.1);
                    transition: width linear;
                }

                .error-display-progress.active {
                    background: #007bff;
                }

                /* Close button */
                .error-display-close {
                    position: absolute;
                    top: 8px;
                    right: 8px;
                    background: none;
                    border: none;
                    font-size: 16px;
                    cursor: pointer;
                    color: #999;
                    padding: 4px;
                    border-radius: 4px;
                    transition: all 0.2s ease;
                }

                .error-display-close:hover {
                    background: rgba(0, 0, 0, 0.1);
                    color: #666;
                }

                /* Field error integration */
                .field-error-enhanced {
                    position: relative;
                    border-left: 3px solid #dc3545;
                    background: #f8f9fa;
                    padding: 12px;
                    border-radius: 4px;
                    margin-top: 4px;
                }

                .field-error-enhanced .error-icon {
                    color: #dc3545;
                    margin-right: 8px;
                }

                .field-error-enhanced .error-message {
                    font-weight: 600;
                    color: #dc3545;
                    margin-bottom: 4px;
                }

                .field-error-enhanced .error-guidance {
                    font-size: 12px;
                    color: #666;
                    margin-bottom: 8px;
                }

                .field-error-enhanced .error-example {
                    font-size: 12px;
                    color: #17a2b8;
                    font-style: italic;
                }

                /* Accessibility enhancements */
                .error-display-item[role="alert"] {
                    /* Screen reader will announce this */
                }

                .error-display-item:focus {
                    outline: 2px solid #007bff;
                    outline-offset: 2px;
                }

                /* Responsive design */
                @media (max-width: 768px) {
                    .error-display-container {
                        left: 10px !important;
                        right: 10px !important;
                        max-width: none;
                    }

                    .error-display-item {
                        margin-bottom: 8px;
                    }

                    .error-display-content {
                        padding: 12px;
                        padding-left: 16px;
                    }
                }

                /* Dark theme support */
                @media (prefers-color-scheme: dark) {
                    .error-display-item {
                        background: #2d3748;
                        color: #e2e8f0;
                    }

                    .error-display-message {
                        color: #a0aec0;
                    }

                    .error-display-action {
                        background: #4a5568;
                        border-color: #4a5568;
                        color: #e2e8f0;
                    }

                    .error-display-action:hover {
                        background: #2d3748;
                        border-color: #2d3748;
                    }

                    .field-error-enhanced {
                        background: #2d3748;
                        color: #e2e8f0;
                    }

                    .field-error-enhanced .error-guidance {
                        color: #a0aec0;
                    }
                }
            `;

            document.head.appendChild(styles);
        }

        /**
         * Setup event listeners
         */
        setupEventListeners() {
            // Listen for error events from other components
            window.addEventListener('errorHandlerError', (event) => {
                this.displayError(event.detail.error, event.detail.context);
            });

            window.addEventListener('errorRecoveryAttempt', (event) => {
                this.updateErrorWithRecovery(event.detail.error, event.detail.recovery);
            });

            window.addEventListener('errorRecoverySuccess', (event) => {
                this.removeError(event.detail.errorId);
                this.trackRecoverySuccess();
            });

            // Listen for page visibility changes
            document.addEventListener('visibilitychange', () => {
                if (document.hidden) {
                    this.pauseAutoHide();
                } else {
                    this.resumeAutoHide();
                }
            });
        }

        /**
         * Setup accessibility features
         */
        setupAccessibility() {
            if (!this.options.enableAccessibility) return;

            // Create live region for screen readers
            const liveRegion = document.createElement('div');
            liveRegion.id = 'error-live-region';
            liveRegion.setAttribute('aria-live', 'polite');
            liveRegion.setAttribute('aria-atomic', 'true');
            liveRegion.style.cssText = `
                position: absolute;
                left: -10000px;
                width: 1px;
                height: 1px;
                overflow: hidden;
            `;
            document.body.appendChild(liveRegion);
        }

        /**
         * Setup analytics tracking
         */
        setupAnalytics() {
            if (!this.options.enableAnalytics) return;

            // Track page for error analytics
            this.analytics.errorsByPage[window.location.pathname] = 0;

            // Send analytics periodically
            setInterval(() => {
                this.sendAnalytics();
            }, 60000); // Every minute
        }

        /**
         * Display error with enhanced UI
         */
        displayError(error, context = {}) {
            // Categorize error
            const categorizedError = window.errorCategorization.categorizeError(error, context);
            
            // Create error display item
            const errorItem = this.createErrorDisplayItem(categorizedError, context);
            
            // Add to active errors
            const errorId = this.generateErrorId();
            this.activeErrors.set(errorId, {
                item: errorItem,
                error: categorizedError,
                context: context,
                timestamp: new Date(),
                autoHideTimer: null
            });

            // Display the error
            this.showErrorItem(errorId);

            // Track analytics
            this.trackError(categorizedError);

            // Announce to screen readers
            this.announceError(categorizedError);

            return errorId;
        }

        /**
         * Create error display item
         */
        createErrorDisplayItem(categorizedError, context) {
            const { errorType, originalError, uiResponse } = categorizedError;
            
            const item = document.createElement('div');
            item.className = `error-display-item severity-${this.getSeverityClass(originalError)} ${errorType}`;
            item.setAttribute('role', 'alert');
            item.setAttribute('tabindex', '0');

            const content = document.createElement('div');
            content.className = 'error-display-content';

            // Header with icon and title
            const header = document.createElement('div');
            header.className = 'error-display-header';
            
            const icon = document.createElement('div');
            icon.className = 'error-display-icon';
            icon.innerHTML = this.getErrorIcon(errorType);
            
            const title = document.createElement('h4');
            title.className = 'error-display-title';
            title.textContent = this.getErrorTitle(errorType);
            
            header.appendChild(icon);
            header.appendChild(title);

            // Message
            const message = document.createElement('div');
            message.className = 'error-display-message';
            message.textContent = originalError.message;

            // Suggestions
            const suggestions = this.createSuggestionsElement(originalError.suggestions);

            // Actions
            const actions = this.createActionsElement(categorizedError, context);

            // Close button
            const closeButton = document.createElement('button');
            closeButton.className = 'error-display-close';
            closeButton.innerHTML = '&times;';
            closeButton.setAttribute('aria-label', 'Close error message');
            closeButton.onclick = () => this.removeError(item.errorId);

            // Progress bar for auto-hide
            const progress = document.createElement('div');
            progress.className = 'error-display-progress';

            // Assemble content
            content.appendChild(header);
            content.appendChild(message);
            if (suggestions) content.appendChild(suggestions);
            if (actions) content.appendChild(actions);

            item.appendChild(content);
            item.appendChild(closeButton);
            item.appendChild(progress);

            return item;
        }

        /**
         * Create suggestions element
         */
        createSuggestionsElement(suggestions) {
            if (!suggestions || suggestions.length === 0) return null;

            const suggestionsDiv = document.createElement('div');
            suggestionsDiv.className = 'error-display-suggestions';

            const title = document.createElement('strong');
            title.textContent = 'Suggestions:';

            const list = document.createElement('ul');
            suggestions.forEach(suggestion => {
                const item = document.createElement('li');
                item.textContent = suggestion;
                list.appendChild(item);
            });

            suggestionsDiv.appendChild(title);
            suggestionsDiv.appendChild(list);

            return suggestionsDiv;
        }

        /**
         * Create actions element
         */
        createActionsElement(categorizedError, context) {
            const { uiResponse } = categorizedError;
            const actions = [];

            if (uiResponse.showRetry) {
                actions.push({
                    label: 'Retry',
                    class: 'primary',
                    onclick: () => this.retryOperation(categorizedError, context)
                });
            }

            if (uiResponse.showFallback) {
                actions.push({
                    label: 'Offline Mode',
                    class: 'secondary',
                    onclick: () => this.activateFallback(categorizedError, context)
                });
            }

            if (uiResponse.showContactSupport) {
                actions.push({
                    label: 'Contact Support',
                    class: 'secondary',
                    onclick: () => this.contactSupport(categorizedError)
                });
            }

            if (actions.length === 0) return null;

            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'error-display-actions';

            actions.forEach(action => {
                const button = document.createElement('button');
                button.className = `error-display-action ${action.class}`;
                button.textContent = action.label;
                button.onclick = action.onclick;
                actionsDiv.appendChild(button);
            });

            return actionsDiv;
        }

        /**
         * Show error item
         */
        showErrorItem(errorId) {
            const errorData = this.activeErrors.get(errorId);
            if (!errorData) return;

            const { item, error } = errorData;
            item.errorId = errorId;

            // Get or create container
            const container = this.getOrCreateContainer();

            // Add entering animation
            if (this.options.enableAnimations) {
                item.classList.add('entering');
                setTimeout(() => {
                    item.classList.remove('entering');
                }, 300);
            }

            // Add to container
            container.appendChild(item);
            this.displayedCount++;

            // Setup auto-hide if specified
            if (error.uiResponse.autoHide) {
                this.setupAutoHide(errorId, error.uiResponse.autoHideDelay || 5000);
            }

            // Manage queue if too many errors
            this.manageErrorQueue();
        }

        /**
         * Get or create error container
         */
        getOrCreateContainer() {
            let container = document.getElementById('error-display-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'error-display-container';
                container.className = `error-display-container ${this.options.position}`;
                document.body.appendChild(container);
            }
            return container;
        }

        /**
         * Setup auto-hide timer
         */
        setupAutoHide(errorId, delay) {
            const errorData = this.activeErrors.get(errorId);
            if (!errorData) return;

            const { item } = errorData;
            const progress = item.querySelector('.error-display-progress');

            // Animate progress bar
            if (progress) {
                progress.classList.add('active');
                progress.style.width = '100%';
                progress.style.transition = `width ${delay}ms linear`;
                
                // Start countdown
                setTimeout(() => {
                    progress.style.width = '0%';
                }, 100);
            }

            // Set auto-hide timer
            errorData.autoHideTimer = setTimeout(() => {
                this.removeError(errorId);
            }, delay);
        }

        /**
         * Remove error
         */
        removeError(errorId) {
            const errorData = this.activeErrors.get(errorId);
            if (!errorData) return;

            const { item, autoHideTimer } = errorData;

            // Clear auto-hide timer
            if (autoHideTimer) {
                clearTimeout(autoHideTimer);
            }

            // Add exiting animation
            if (this.options.enableAnimations) {
                item.classList.add('exiting');
                setTimeout(() => {
                    this.finalizeRemoval(errorId);
                }, 300);
            } else {
                this.finalizeRemoval(errorId);
            }
        }

        /**
         * Finalize error removal
         */
        finalizeRemoval(errorId) {
            const errorData = this.activeErrors.get(errorId);
            if (!errorData) return;

            const { item } = errorData;

            // Remove from DOM
            if (item.parentNode) {
                item.parentNode.removeChild(item);
            }

            // Remove from active errors
            this.activeErrors.delete(errorId);
            this.displayedCount--;

            // Process queue
            this.processErrorQueue();
        }

        /**
         * Update error with recovery information
         */
        updateErrorWithRecovery(error, recovery) {
            // Find matching error
            for (const [errorId, errorData] of this.activeErrors.entries()) {
                if (errorData.error.originalError === error) {
                    this.updateErrorDisplay(errorId, recovery);
                    break;
                }
            }
        }

        /**
         * Update error display with recovery info
         */
        updateErrorDisplay(errorId, recovery) {
            const errorData = this.activeErrors.get(errorId);
            if (!errorData) return;

            const { item } = errorData;
            const content = item.querySelector('.error-display-content');

            // Add recovery status
            const recoveryStatus = document.createElement('div');
            recoveryStatus.className = 'error-recovery-status';
            recoveryStatus.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                    <span>${recovery.message || 'Attempting recovery...'}</span>
                </div>
            `;

            content.appendChild(recoveryStatus);

            // Track recovery attempt
            this.trackRecoveryAttempt();
        }

        /**
         * Display field error with enhanced styling
         */
        displayFieldError(field, fieldError) {
            // Remove existing error
            this.clearFieldError(field);

            // Add error class
            field.classList.add('is-invalid');

            // Create enhanced error element
            const errorElement = document.createElement('div');
            errorElement.className = 'field-error-enhanced';
            errorElement.innerHTML = `
                <div class="d-flex align-items-start">
                    <i class="fas fa-exclamation-triangle error-icon"></i>
                    <div class="flex-grow-1">
                        <div class="error-message">${fieldError.message}</div>
                        ${fieldError.correctionGuidance ? `<div class="error-guidance">${fieldError.correctionGuidance}</div>` : ''}
                        ${fieldError.formatExample ? `<div class="error-example">Example: ${fieldError.formatExample}</div>` : ''}
                    </div>
                </div>
            `;

            // Insert after field
            field.parentNode.appendChild(errorElement);

            // Announce to screen readers
            this.announceFieldError(fieldError);
        }

        /**
         * Clear field error
         */
        clearFieldError(field) {
            field.classList.remove('is-invalid');
            const errorElement = field.parentNode.querySelector('.field-error-enhanced');
            if (errorElement) {
                errorElement.remove();
            }
        }

        /**
         * Retry operation
         */
        async retryOperation(categorizedError, context) {
            try {
                this.trackRecoveryAttempt();
                
                const result = await window.errorRecovery.attemptRecovery(
                    categorizedError.originalError, 
                    context
                );

                if (result.success) {
                    this.trackRecoverySuccess();
                }

                return result;
            } catch (error) {
                console.error('Retry operation failed:', error);
                return { success: false, error };
            }
        }

        /**
         * Activate fallback mode
         */
        activateFallback(categorizedError, context) {
            window.dispatchEvent(new CustomEvent('errorDisplayFallback', {
                detail: { error: categorizedError, context }
            }));
        }

        /**
         * Contact support
         */
        contactSupport(categorizedError) {
            const { originalError } = categorizedError;
            const subject = encodeURIComponent(`Error Report: ${originalError.errorCode || 'Unknown'}`);
            const body = encodeURIComponent(`
Error Details:
- Code: ${originalError.errorCode || 'N/A'}
- Message: ${originalError.message}
- Time: ${new Date().toISOString()}
- Page: ${window.location.href}
- User Agent: ${navigator.userAgent}
            `);

            window.open(`mailto:support@petclinic.com?subject=${subject}&body=${body}`);
        }

        /**
         * Announce error to screen readers
         */
        announceError(categorizedError) {
            if (!this.options.enableAccessibility) return;

            const liveRegion = document.getElementById('error-live-region');
            if (liveRegion) {
                const { errorType, originalError } = categorizedError;
                const announcement = `${this.getErrorTitle(errorType)}: ${originalError.message}`;
                liveRegion.textContent = announcement;

                // Clear after announcement
                setTimeout(() => {
                    liveRegion.textContent = '';
                }, 1000);
            }
        }

        /**
         * Announce field error to screen readers
         */
        announceFieldError(fieldError) {
            if (!this.options.enableAccessibility) return;

            const liveRegion = document.getElementById('error-live-region');
            if (liveRegion) {
                liveRegion.textContent = `Field error: ${fieldError.message}`;
                setTimeout(() => {
                    liveRegion.textContent = '';
                }, 1000);
            }
        }

        /**
         * Manage error queue when too many errors are displayed
         */
        manageErrorQueue() {
            if (this.displayedCount > this.options.maxDisplayedErrors) {
                // Remove oldest error
                const oldestErrorId = Array.from(this.activeErrors.keys())[0];
                this.removeError(oldestErrorId);
            }
        }

        /**
         * Process error queue
         */
        processErrorQueue() {
            if (this.errorQueue.length > 0 && this.displayedCount < this.options.maxDisplayedErrors) {
                const queuedError = this.errorQueue.shift();
                this.displayError(queuedError.error, queuedError.context);
            }
        }

        /**
         * Pause auto-hide timers
         */
        pauseAutoHide() {
            for (const [errorId, errorData] of this.activeErrors.entries()) {
                if (errorData.autoHideTimer) {
                    clearTimeout(errorData.autoHideTimer);
                    errorData.autoHideTimer = null;
                }
            }
        }

        /**
         * Resume auto-hide timers
         */
        resumeAutoHide() {
            for (const [errorId, errorData] of this.activeErrors.entries()) {
                const { error, timestamp } = errorData;
                if (error.uiResponse.autoHide && !errorData.autoHideTimer) {
                    const elapsed = Date.now() - timestamp.getTime();
                    const remaining = (error.uiResponse.autoHideDelay || 5000) - elapsed;
                    
                    if (remaining > 0) {
                        this.setupAutoHide(errorId, remaining);
                    } else {
                        this.removeError(errorId);
                    }
                }
            }
        }

        /**
         * Helper methods
         */
        generateErrorId() {
            return `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        }

        getSeverityClass(error) {
            if (error.severity) return error.severity;
            if (error.status >= 500) return 'error';
            if (error.status >= 400) return 'warning';
            return 'info';
        }

        getErrorIcon(errorType) {
            return window.errorCategorization.getErrorIcon(errorType);
        }

        getErrorTitle(errorType) {
            return window.errorCategorization.getErrorTitle(errorType);
        }

        /**
         * Analytics methods
         */
        trackError(categorizedError) {
            if (!this.options.enableAnalytics) return;

            this.analytics.totalErrors++;
            
            const errorType = categorizedError.errorType;
            this.analytics.errorsByType[errorType] = (this.analytics.errorsByType[errorType] || 0) + 1;
            
            const page = window.location.pathname;
            this.analytics.errorsByPage[page] = (this.analytics.errorsByPage[page] || 0) + 1;
        }

        trackRecoveryAttempt() {
            if (!this.options.enableAnalytics) return;
            this.analytics.recoveryAttempts++;
        }

        trackRecoverySuccess() {
            if (!this.options.enableAnalytics) return;
            this.analytics.successfulRecoveries++;
        }

        sendAnalytics() {
            if (!this.options.enableAnalytics) return;

            // Send analytics data to server
            fetch('/api/analytics/errors', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    ...this.analytics,
                    timestamp: new Date().toISOString(),
                    page: window.location.pathname,
                    userAgent: navigator.userAgent
                })
            }).catch(error => {
                console.warn('Failed to send error analytics:', error);
            });
        }

        /**
         * Public API methods
         */
        clearAllErrors() {
            for (const errorId of this.activeErrors.keys()) {
                this.removeError(errorId);
            }
        }

        getActiveErrors() {
            return Array.from(this.activeErrors.values()).map(errorData => ({
                error: errorData.error,
                context: errorData.context,
                timestamp: errorData.timestamp
            }));
        }

        getAnalytics() {
            return { ...this.analytics };
        }
    }

    // Create global error display instance
    window.errorDisplay = new ErrorDisplay();

    // Export for use by other modules
    window.ErrorDisplay = ErrorDisplay;

})(window);