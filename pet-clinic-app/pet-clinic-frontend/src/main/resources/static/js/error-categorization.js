/**
 * Error Categorization Component
 * Provides error categorization and appropriate UI responses for different error types
 * Requirements: 1.3, 1.4, 1.5
 */

(function(window) {
    'use strict';

    /**
     * Error types with specific handling rules
     */
    const ErrorType = {
        // Network-related errors
        NETWORK_TIMEOUT: 'network_timeout',
        NETWORK_UNAVAILABLE: 'network_unavailable',
        CONNECTION_REFUSED: 'connection_refused',
        DNS_ERROR: 'dns_error',

        // Service-related errors
        SERVICE_UNAVAILABLE: 'service_unavailable',
        SERVICE_OVERLOADED: 'service_overloaded',
        SERVICE_MAINTENANCE: 'service_maintenance',

        // Authentication/Authorization errors
        AUTHENTICATION_EXPIRED: 'authentication_expired',
        AUTHENTICATION_INVALID: 'authentication_invalid',
        AUTHORIZATION_INSUFFICIENT: 'authorization_insufficient',
        AUTHORIZATION_FORBIDDEN: 'authorization_forbidden',

        // Validation errors
        VALIDATION_FIELD_REQUIRED: 'validation_field_required',
        VALIDATION_FIELD_FORMAT: 'validation_field_format',
        VALIDATION_FIELD_DUPLICATE: 'validation_field_duplicate',
        VALIDATION_BUSINESS_RULE: 'validation_business_rule',

        // Data errors
        DATA_NOT_FOUND: 'data_not_found',
        DATA_CONFLICT: 'data_conflict',
        DATA_CORRUPTED: 'data_corrupted',

        // Server errors
        SERVER_INTERNAL: 'server_internal',
        SERVER_DATABASE: 'server_database',
        SERVER_CONFIGURATION: 'server_configuration',

        // Client errors
        CLIENT_BROWSER: 'client_browser',
        CLIENT_JAVASCRIPT: 'client_javascript',
        CLIENT_STORAGE: 'client_storage'
    };

    /**
     * UI Response types for different error categories
     */
    const UIResponseType = {
        INLINE_MESSAGE: 'inline_message',
        MODAL_DIALOG: 'modal_dialog',
        NOTIFICATION_TOAST: 'notification_toast',
        PAGE_OVERLAY: 'page_overlay',
        FIELD_VALIDATION: 'field_validation',
        STATUS_BAR: 'status_bar',
        REDIRECT: 'redirect'
    };

    /**
     * Error categorization and UI response component
     */
    class ErrorCategorization {
        constructor(options = {}) {
            this.options = {
                enableAutoDetection: options.enableAutoDetection !== false,
                enableContextualResponses: options.enableContextualResponses !== false,
                enableProgressiveDisclosure: options.enableProgressiveDisclosure !== false,
                ...options
            };

            this.errorPatterns = new Map();
            this.uiResponseRules = new Map();
            this.contextualHandlers = new Map();
            
            this.init();
        }

        /**
         * Initialize categorization system
         */
        init() {
            this.setupErrorPatterns();
            this.setupUIResponseRules();
            this.setupContextualHandlers();
        }

        /**
         * Setup error detection patterns
         */
        setupErrorPatterns() {
            // Network error patterns
            this.errorPatterns.set(ErrorType.NETWORK_TIMEOUT, {
                statusCodes: [408, 504],
                messagePatterns: [/timeout/i, /timed out/i],
                errorNames: ['TimeoutError'],
                contextClues: ['slow network', 'connection timeout']
            });

            this.errorPatterns.set(ErrorType.NETWORK_UNAVAILABLE, {
                statusCodes: [0],
                messagePatterns: [/network error/i, /failed to fetch/i],
                errorNames: ['TypeError', 'NetworkError'],
                contextClues: ['offline', 'no internet']
            });

            this.errorPatterns.set(ErrorType.CONNECTION_REFUSED, {
                statusCodes: [502, 503],
                messagePatterns: [/connection refused/i, /service unavailable/i],
                errorNames: ['ConnectionError'],
                contextClues: ['server down', 'maintenance']
            });

            // Service error patterns
            this.errorPatterns.set(ErrorType.SERVICE_UNAVAILABLE, {
                statusCodes: [503],
                errorCodes: ['SERVICE_UNAVAILABLE', 'SERVICE_DOWN'],
                messagePatterns: [/service.*unavailable/i, /temporarily unavailable/i],
                contextClues: ['maintenance', 'overload']
            });

            this.errorPatterns.set(ErrorType.SERVICE_OVERLOADED, {
                statusCodes: [429, 503],
                errorCodes: ['RATE_LIMIT_EXCEEDED', 'TOO_MANY_REQUESTS'],
                messagePatterns: [/rate limit/i, /too many requests/i],
                contextClues: ['high load', 'busy']
            });

            // Authentication error patterns
            this.errorPatterns.set(ErrorType.AUTHENTICATION_EXPIRED, {
                statusCodes: [401],
                errorCodes: ['TOKEN_EXPIRED', 'SESSION_EXPIRED'],
                messagePatterns: [/session.*expired/i, /token.*expired/i],
                contextClues: ['login again', 'session timeout']
            });

            this.errorPatterns.set(ErrorType.AUTHENTICATION_INVALID, {
                statusCodes: [401],
                errorCodes: ['INVALID_CREDENTIALS', 'AUTHENTICATION_FAILED'],
                messagePatterns: [/invalid.*credentials/i, /authentication.*failed/i],
                contextClues: ['wrong password', 'invalid login']
            });

            // Authorization error patterns
            this.errorPatterns.set(ErrorType.AUTHORIZATION_INSUFFICIENT, {
                statusCodes: [403],
                errorCodes: ['INSUFFICIENT_PRIVILEGES', 'ACCESS_DENIED'],
                messagePatterns: [/insufficient.*privileges/i, /access.*denied/i],
                contextClues: ['permission', 'role']
            });

            // Validation error patterns
            this.errorPatterns.set(ErrorType.VALIDATION_FIELD_REQUIRED, {
                statusCodes: [400],
                errorCodes: ['REQUIRED_FIELD', 'FIELD_REQUIRED'],
                messagePatterns: [/required/i, /mandatory/i],
                contextClues: ['missing field', 'empty field']
            });

            this.errorPatterns.set(ErrorType.VALIDATION_FIELD_FORMAT, {
                statusCodes: [400],
                errorCodes: ['INVALID_FORMAT', 'FORMAT_ERROR'],
                messagePatterns: [/invalid.*format/i, /format.*error/i],
                contextClues: ['email format', 'phone format', 'date format']
            });

            this.errorPatterns.set(ErrorType.VALIDATION_FIELD_DUPLICATE, {
                statusCodes: [409],
                errorCodes: ['DUPLICATE_VALUE', 'ALREADY_EXISTS'],
                messagePatterns: [/already.*exists/i, /duplicate/i],
                contextClues: ['unique', 'already registered']
            });

            // Data error patterns
            this.errorPatterns.set(ErrorType.DATA_NOT_FOUND, {
                statusCodes: [404],
                errorCodes: ['NOT_FOUND', 'ENTITY_NOT_FOUND'],
                messagePatterns: [/not found/i, /does not exist/i],
                contextClues: ['missing', 'deleted']
            });

            this.errorPatterns.set(ErrorType.DATA_CONFLICT, {
                statusCodes: [409],
                errorCodes: ['CONFLICT', 'DATA_CONFLICT'],
                messagePatterns: [/conflict/i, /concurrent modification/i],
                contextClues: ['version', 'modified by another user']
            });

            // Server error patterns
            this.errorPatterns.set(ErrorType.SERVER_INTERNAL, {
                statusCodes: [500],
                errorCodes: ['INTERNAL_ERROR', 'SERVER_ERROR'],
                messagePatterns: [/internal.*error/i, /server.*error/i],
                contextClues: ['unexpected error', 'system error']
            });

            this.errorPatterns.set(ErrorType.SERVER_DATABASE, {
                statusCodes: [500],
                errorCodes: ['DATABASE_ERROR', 'SQL_ERROR'],
                messagePatterns: [/database.*error/i, /sql.*error/i],
                contextClues: ['database', 'query failed']
            });
        }

        /**
         * Setup UI response rules for different error types
         */
        setupUIResponseRules() {
            // Network errors - show status bar with retry option
            this.uiResponseRules.set(ErrorType.NETWORK_TIMEOUT, {
                primaryResponse: UIResponseType.NOTIFICATION_TOAST,
                secondaryResponse: UIResponseType.STATUS_BAR,
                showRetry: true,
                showFallback: false,
                autoHide: false,
                priority: 'high'
            });

            this.uiResponseRules.set(ErrorType.NETWORK_UNAVAILABLE, {
                primaryResponse: UIResponseType.PAGE_OVERLAY,
                secondaryResponse: UIResponseType.STATUS_BAR,
                showRetry: true,
                showFallback: true,
                autoHide: false,
                priority: 'critical'
            });

            // Service errors - show notification with fallback options
            this.uiResponseRules.set(ErrorType.SERVICE_UNAVAILABLE, {
                primaryResponse: UIResponseType.NOTIFICATION_TOAST,
                secondaryResponse: UIResponseType.STATUS_BAR,
                showRetry: true,
                showFallback: true,
                autoHide: false,
                priority: 'high'
            });

            this.uiResponseRules.set(ErrorType.SERVICE_OVERLOADED, {
                primaryResponse: UIResponseType.NOTIFICATION_TOAST,
                showRetry: true,
                showFallback: false,
                autoHide: true,
                autoHideDelay: 10000,
                priority: 'medium'
            });

            // Authentication errors - show modal with redirect
            this.uiResponseRules.set(ErrorType.AUTHENTICATION_EXPIRED, {
                primaryResponse: UIResponseType.MODAL_DIALOG,
                secondaryResponse: UIResponseType.REDIRECT,
                showRetry: false,
                showFallback: false,
                autoHide: false,
                priority: 'critical',
                redirectUrl: '/login',
                redirectDelay: 5000
            });

            this.uiResponseRules.set(ErrorType.AUTHENTICATION_INVALID, {
                primaryResponse: UIResponseType.INLINE_MESSAGE,
                showRetry: true,
                showFallback: false,
                autoHide: false,
                priority: 'high'
            });

            // Authorization errors - show modal with contact info
            this.uiResponseRules.set(ErrorType.AUTHORIZATION_INSUFFICIENT, {
                primaryResponse: UIResponseType.MODAL_DIALOG,
                showRetry: false,
                showFallback: false,
                autoHide: false,
                priority: 'high',
                showContactSupport: true
            });

            // Validation errors - show field-specific messages
            this.uiResponseRules.set(ErrorType.VALIDATION_FIELD_REQUIRED, {
                primaryResponse: UIResponseType.FIELD_VALIDATION,
                secondaryResponse: UIResponseType.INLINE_MESSAGE,
                showRetry: false,
                showFallback: false,
                autoHide: false,
                priority: 'medium'
            });

            this.uiResponseRules.set(ErrorType.VALIDATION_FIELD_FORMAT, {
                primaryResponse: UIResponseType.FIELD_VALIDATION,
                secondaryResponse: UIResponseType.INLINE_MESSAGE,
                showRetry: false,
                showFallback: false,
                autoHide: false,
                priority: 'medium',
                showFormatExample: true
            });

            this.uiResponseRules.set(ErrorType.VALIDATION_FIELD_DUPLICATE, {
                primaryResponse: UIResponseType.FIELD_VALIDATION,
                secondaryResponse: UIResponseType.INLINE_MESSAGE,
                showRetry: false,
                showFallback: false,
                autoHide: false,
                priority: 'medium',
                showAlternatives: true
            });

            // Data errors - show appropriate messages
            this.uiResponseRules.set(ErrorType.DATA_NOT_FOUND, {
                primaryResponse: UIResponseType.INLINE_MESSAGE,
                secondaryResponse: UIResponseType.NOTIFICATION_TOAST,
                showRetry: true,
                showFallback: false,
                autoHide: true,
                autoHideDelay: 5000,
                priority: 'medium'
            });

            this.uiResponseRules.set(ErrorType.DATA_CONFLICT, {
                primaryResponse: UIResponseType.MODAL_DIALOG,
                showRetry: true,
                showFallback: false,
                autoHide: false,
                priority: 'high',
                showConflictResolution: true
            });

            // Server errors - show notification with support contact
            this.uiResponseRules.set(ErrorType.SERVER_INTERNAL, {
                primaryResponse: UIResponseType.NOTIFICATION_TOAST,
                secondaryResponse: UIResponseType.STATUS_BAR,
                showRetry: true,
                showFallback: false,
                autoHide: false,
                priority: 'high',
                showContactSupport: true
            });
        }

        /**
         * Setup contextual handlers for different page contexts
         */
        setupContextualHandlers() {
            // Form context - focus on field validation
            this.contextualHandlers.set('form', {
                preferredResponses: [UIResponseType.FIELD_VALIDATION, UIResponseType.INLINE_MESSAGE],
                enhanceFieldErrors: true,
                showProgressiveHelp: true,
                scrollToError: true
            });

            // Table/list context - focus on notifications
            this.contextualHandlers.set('table', {
                preferredResponses: [UIResponseType.NOTIFICATION_TOAST, UIResponseType.STATUS_BAR],
                showBulkErrorSummary: true,
                enablePartialSuccess: true
            });

            // Modal context - use inline messages
            this.contextualHandlers.set('modal', {
                preferredResponses: [UIResponseType.INLINE_MESSAGE],
                preventModalStacking: true,
                showInModalHeader: true
            });

            // Dashboard context - use status bar
            this.contextualHandlers.set('dashboard', {
                preferredResponses: [UIResponseType.STATUS_BAR, UIResponseType.NOTIFICATION_TOAST],
                showSystemStatus: true,
                aggregateErrors: true
            });
        }

        /**
         * Categorize error and determine appropriate UI response
         */
        categorizeError(error, context = {}) {
            const errorType = this.detectErrorType(error);
            const uiResponse = this.determineUIResponse(errorType, context);
            const contextualEnhancements = this.getContextualEnhancements(context);

            return {
                errorType,
                originalError: error,
                uiResponse: {
                    ...uiResponse,
                    ...contextualEnhancements
                },
                categorization: {
                    confidence: this.calculateConfidence(error, errorType),
                    alternativeTypes: this.getAlternativeTypes(error),
                    context: context
                }
            };
        }

        /**
         * Detect error type based on patterns
         */
        detectErrorType(error) {
            let bestMatch = null;
            let highestScore = 0;

            for (const [errorType, pattern] of this.errorPatterns.entries()) {
                const score = this.calculatePatternScore(error, pattern);
                if (score > highestScore) {
                    highestScore = score;
                    bestMatch = errorType;
                }
            }

            return bestMatch || ErrorType.CLIENT_JAVASCRIPT;
        }

        /**
         * Calculate pattern matching score
         */
        calculatePatternScore(error, pattern) {
            let score = 0;

            // Check status code
            if (pattern.statusCodes && error.status) {
                if (pattern.statusCodes.includes(error.status)) {
                    score += 30;
                }
            }

            // Check error code
            if (pattern.errorCodes && error.errorCode) {
                if (pattern.errorCodes.includes(error.errorCode)) {
                    score += 25;
                }
            }

            // Check message patterns
            if (pattern.messagePatterns && error.message) {
                for (const messagePattern of pattern.messagePatterns) {
                    if (messagePattern.test(error.message)) {
                        score += 20;
                        break;
                    }
                }
            }

            // Check error name
            if (pattern.errorNames && error.name) {
                if (pattern.errorNames.includes(error.name)) {
                    score += 15;
                }
            }

            // Check context clues
            if (pattern.contextClues && error.message) {
                for (const clue of pattern.contextClues) {
                    if (error.message.toLowerCase().includes(clue.toLowerCase())) {
                        score += 10;
                        break;
                    }
                }
            }

            return score;
        }

        /**
         * Determine appropriate UI response
         */
        determineUIResponse(errorType, context) {
            const rule = this.uiResponseRules.get(errorType);
            if (!rule) {
                return this.getDefaultUIResponse();
            }

            // Apply contextual modifications
            const contextualHandler = this.getContextualHandler(context);
            if (contextualHandler && contextualHandler.preferredResponses) {
                // Check if primary response is preferred in this context
                if (!contextualHandler.preferredResponses.includes(rule.primaryResponse)) {
                    // Use first preferred response that's available
                    const preferredResponse = contextualHandler.preferredResponses[0];
                    return {
                        ...rule,
                        primaryResponse: preferredResponse,
                        contextuallyModified: true
                    };
                }
            }

            return rule;
        }

        /**
         * Get contextual enhancements
         */
        getContextualEnhancements(context) {
            const handler = this.getContextualHandler(context);
            if (!handler) return {};

            const enhancements = {};

            if (handler.enhanceFieldErrors) {
                enhancements.enhanceFieldErrors = true;
            }

            if (handler.showProgressiveHelp) {
                enhancements.showProgressiveHelp = true;
            }

            if (handler.scrollToError) {
                enhancements.scrollToError = true;
            }

            if (handler.showBulkErrorSummary) {
                enhancements.showBulkErrorSummary = true;
            }

            if (handler.preventModalStacking) {
                enhancements.preventModalStacking = true;
            }

            return enhancements;
        }

        /**
         * Get contextual handler based on context
         */
        getContextualHandler(context) {
            if (context.formElement || context.isForm) {
                return this.contextualHandlers.get('form');
            }

            if (context.tableElement || context.isTable) {
                return this.contextualHandlers.get('table');
            }

            if (context.modalElement || context.isModal) {
                return this.contextualHandlers.get('modal');
            }

            if (context.isDashboard) {
                return this.contextualHandlers.get('dashboard');
            }

            return null;
        }

        /**
         * Calculate confidence in error type detection
         */
        calculateConfidence(error, errorType) {
            if (!errorType) return 0;

            const pattern = this.errorPatterns.get(errorType);
            if (!pattern) return 0;

            const score = this.calculatePatternScore(error, pattern);
            
            // Convert score to confidence percentage
            const maxPossibleScore = 100; // 30 + 25 + 20 + 15 + 10
            return Math.min(100, (score / maxPossibleScore) * 100);
        }

        /**
         * Get alternative error types that also match
         */
        getAlternativeTypes(error) {
            const alternatives = [];

            for (const [errorType, pattern] of this.errorPatterns.entries()) {
                const score = this.calculatePatternScore(error, pattern);
                if (score > 20) { // Threshold for alternative consideration
                    alternatives.push({
                        errorType,
                        score,
                        confidence: Math.min(100, (score / 100) * 100)
                    });
                }
            }

            // Sort by score and return top 3
            return alternatives
                .sort((a, b) => b.score - a.score)
                .slice(0, 3);
        }

        /**
         * Get default UI response for unknown errors
         */
        getDefaultUIResponse() {
            return {
                primaryResponse: UIResponseType.NOTIFICATION_TOAST,
                showRetry: true,
                showFallback: false,
                autoHide: true,
                autoHideDelay: 5000,
                priority: 'medium'
            };
        }

        /**
         * Create appropriate UI response based on categorization
         */
        createUIResponse(categorizedError) {
            const { errorType, uiResponse, originalError } = categorizedError;

            switch (uiResponse.primaryResponse) {
                case UIResponseType.FIELD_VALIDATION:
                    return this.createFieldValidationResponse(categorizedError);
                case UIResponseType.MODAL_DIALOG:
                    return this.createModalDialogResponse(categorizedError);
                case UIResponseType.NOTIFICATION_TOAST:
                    return this.createNotificationToastResponse(categorizedError);
                case UIResponseType.PAGE_OVERLAY:
                    return this.createPageOverlayResponse(categorizedError);
                case UIResponseType.INLINE_MESSAGE:
                    return this.createInlineMessageResponse(categorizedError);
                case UIResponseType.STATUS_BAR:
                    return this.createStatusBarResponse(categorizedError);
                case UIResponseType.REDIRECT:
                    return this.createRedirectResponse(categorizedError);
                default:
                    return this.createNotificationToastResponse(categorizedError);
            }
        }

        /**
         * Create field validation response
         */
        createFieldValidationResponse(categorizedError) {
            const { originalError, uiResponse } = categorizedError;

            return {
                type: 'field_validation',
                render: (container) => {
                    if (originalError.fieldErrors) {
                        originalError.fieldErrors.forEach(fieldError => {
                            this.renderFieldError(fieldError, uiResponse);
                        });
                    }
                },
                cleanup: () => {
                    document.querySelectorAll('.field-error-message').forEach(el => el.remove());
                }
            };
        }

        /**
         * Create modal dialog response
         */
        createModalDialogResponse(categorizedError) {
            const { errorType, originalError, uiResponse } = categorizedError;

            return {
                type: 'modal_dialog',
                render: (container) => {
                    const modal = this.createErrorModal(errorType, originalError, uiResponse);
                    document.body.appendChild(modal);
                    
                    // Show modal
                    const bootstrapModal = new bootstrap.Modal(modal);
                    bootstrapModal.show();

                    return modal;
                },
                cleanup: (modal) => {
                    if (modal && modal.parentNode) {
                        modal.parentNode.removeChild(modal);
                    }
                }
            };
        }

        /**
         * Create notification toast response
         */
        createNotificationToastResponse(categorizedError) {
            const { errorType, originalError, uiResponse } = categorizedError;

            return {
                type: 'notification_toast',
                render: (container) => {
                    const toast = this.createErrorToast(errorType, originalError, uiResponse);
                    
                    // Use existing notification container or create one
                    const notificationContainer = document.getElementById('error-notifications') || container;
                    notificationContainer.appendChild(toast);

                    // Auto-hide if specified
                    if (uiResponse.autoHide) {
                        setTimeout(() => {
                            if (toast.parentNode) {
                                toast.remove();
                            }
                        }, uiResponse.autoHideDelay || 5000);
                    }

                    return toast;
                },
                cleanup: (toast) => {
                    if (toast && toast.parentNode) {
                        toast.remove();
                    }
                }
            };
        }

        /**
         * Create page overlay response
         */
        createPageOverlayResponse(categorizedError) {
            const { errorType, originalError, uiResponse } = categorizedError;

            return {
                type: 'page_overlay',
                render: (container) => {
                    const overlay = this.createErrorOverlay(errorType, originalError, uiResponse);
                    document.body.appendChild(overlay);
                    return overlay;
                },
                cleanup: (overlay) => {
                    if (overlay && overlay.parentNode) {
                        overlay.parentNode.removeChild(overlay);
                    }
                }
            };
        }

        /**
         * Create inline message response
         */
        createInlineMessageResponse(categorizedError) {
            const { errorType, originalError, uiResponse } = categorizedError;

            return {
                type: 'inline_message',
                render: (container) => {
                    const message = this.createInlineErrorMessage(errorType, originalError, uiResponse);
                    container.appendChild(message);
                    return message;
                },
                cleanup: (message) => {
                    if (message && message.parentNode) {
                        message.remove();
                    }
                }
            };
        }

        /**
         * Create status bar response
         */
        createStatusBarResponse(categorizedError) {
            const { errorType, originalError, uiResponse } = categorizedError;

            return {
                type: 'status_bar',
                render: (container) => {
                    const statusBar = this.createErrorStatusBar(errorType, originalError, uiResponse);
                    
                    // Add to top of page
                    document.body.insertBefore(statusBar, document.body.firstChild);
                    return statusBar;
                },
                cleanup: (statusBar) => {
                    if (statusBar && statusBar.parentNode) {
                        statusBar.remove();
                    }
                }
            };
        }

        /**
         * Create redirect response
         */
        createRedirectResponse(categorizedError) {
            const { uiResponse } = categorizedError;

            return {
                type: 'redirect',
                render: () => {
                    // Show countdown message
                    const message = this.createRedirectMessage(uiResponse);
                    document.body.appendChild(message);

                    // Redirect after delay
                    setTimeout(() => {
                        window.location.href = uiResponse.redirectUrl;
                    }, uiResponse.redirectDelay || 3000);

                    return message;
                },
                cleanup: (message) => {
                    if (message && message.parentNode) {
                        message.remove();
                    }
                }
            };
        }

        /**
         * Helper methods for creating UI elements
         */
        createErrorModal(errorType, error, uiResponse) {
            const modal = document.createElement('div');
            modal.className = 'modal fade error-modal';
            modal.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                ${this.getErrorIcon(errorType)} ${this.getErrorTitle(errorType)}
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="alert alert-${this.getAlertClass(errorType)}">
                                ${error.message}
                            </div>
                            ${this.createSuggestionsList(error.suggestions)}
                        </div>
                        <div class="modal-footer">
                            ${this.createModalActions(uiResponse)}
                        </div>
                    </div>
                </div>
            `;
            return modal;
        }

        createErrorToast(errorType, error, uiResponse) {
            const toast = document.createElement('div');
            toast.className = `alert alert-${this.getAlertClass(errorType)} alert-dismissible fade show error-toast`;
            toast.style.cssText = `
                margin-bottom: 10px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                border-left: 4px solid ${this.getBorderColor(errorType)};
            `;
            
            toast.innerHTML = `
                <div class="d-flex align-items-start">
                    <div class="me-3">
                        ${this.getErrorIcon(errorType)}
                    </div>
                    <div class="flex-grow-1">
                        <strong>${this.getErrorTitle(errorType)}</strong><br>
                        ${error.message}
                        ${this.createSuggestionsList(error.suggestions)}
                    </div>
                    ${this.createToastActions(uiResponse)}
                    <button type="button" class="btn-close" onclick="this.closest('.error-toast').remove()"></button>
                </div>
            `;
            
            return toast;
        }

        createErrorOverlay(errorType, error, uiResponse) {
            const overlay = document.createElement('div');
            overlay.className = 'error-overlay';
            overlay.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.8);
                z-index: 9999;
                display: flex;
                align-items: center;
                justify-content: center;
            `;
            
            overlay.innerHTML = `
                <div class="card" style="max-width: 500px; margin: 20px;">
                    <div class="card-header bg-${this.getAlertClass(errorType)} text-white">
                        <h5 class="mb-0">
                            ${this.getErrorIcon(errorType)} ${this.getErrorTitle(errorType)}
                        </h5>
                    </div>
                    <div class="card-body">
                        <p>${error.message}</p>
                        ${this.createSuggestionsList(error.suggestions)}
                        <div class="mt-3">
                            ${this.createOverlayActions(uiResponse)}
                        </div>
                    </div>
                </div>
            `;
            
            return overlay;
        }

        // Additional helper methods...
        getErrorIcon(errorType) {
            const icons = {
                [ErrorType.NETWORK_TIMEOUT]: '<i class="fas fa-clock text-warning"></i>',
                [ErrorType.NETWORK_UNAVAILABLE]: '<i class="fas fa-wifi text-danger"></i>',
                [ErrorType.SERVICE_UNAVAILABLE]: '<i class="fas fa-server text-warning"></i>',
                [ErrorType.AUTHENTICATION_EXPIRED]: '<i class="fas fa-user-clock text-warning"></i>',
                [ErrorType.AUTHORIZATION_INSUFFICIENT]: '<i class="fas fa-lock text-danger"></i>',
                [ErrorType.VALIDATION_FIELD_REQUIRED]: '<i class="fas fa-exclamation-triangle text-warning"></i>',
                [ErrorType.DATA_NOT_FOUND]: '<i class="fas fa-search text-info"></i>',
                [ErrorType.SERVER_INTERNAL]: '<i class="fas fa-server text-danger"></i>'
            };
            return icons[errorType] || '<i class="fas fa-exclamation-circle text-danger"></i>';
        }

        getErrorTitle(errorType) {
            const titles = {
                [ErrorType.NETWORK_TIMEOUT]: 'Connection Timeout',
                [ErrorType.NETWORK_UNAVAILABLE]: 'Network Unavailable',
                [ErrorType.SERVICE_UNAVAILABLE]: 'Service Unavailable',
                [ErrorType.AUTHENTICATION_EXPIRED]: 'Session Expired',
                [ErrorType.AUTHORIZATION_INSUFFICIENT]: 'Access Denied',
                [ErrorType.VALIDATION_FIELD_REQUIRED]: 'Required Field Missing',
                [ErrorType.DATA_NOT_FOUND]: 'Not Found',
                [ErrorType.SERVER_INTERNAL]: 'Server Error'
            };
            return titles[errorType] || 'Error';
        }

        getAlertClass(errorType) {
            const classes = {
                [ErrorType.NETWORK_TIMEOUT]: 'warning',
                [ErrorType.NETWORK_UNAVAILABLE]: 'danger',
                [ErrorType.SERVICE_UNAVAILABLE]: 'warning',
                [ErrorType.AUTHENTICATION_EXPIRED]: 'warning',
                [ErrorType.AUTHORIZATION_INSUFFICIENT]: 'danger',
                [ErrorType.VALIDATION_FIELD_REQUIRED]: 'warning',
                [ErrorType.DATA_NOT_FOUND]: 'info',
                [ErrorType.SERVER_INTERNAL]: 'danger'
            };
            return classes[errorType] || 'secondary';
        }

        getBorderColor(errorType) {
            const colors = {
                [ErrorType.NETWORK_TIMEOUT]: '#ffc107',
                [ErrorType.NETWORK_UNAVAILABLE]: '#dc3545',
                [ErrorType.SERVICE_UNAVAILABLE]: '#ffc107',
                [ErrorType.AUTHENTICATION_EXPIRED]: '#ffc107',
                [ErrorType.AUTHORIZATION_INSUFFICIENT]: '#dc3545',
                [ErrorType.VALIDATION_FIELD_REQUIRED]: '#ffc107',
                [ErrorType.DATA_NOT_FOUND]: '#17a2b8',
                [ErrorType.SERVER_INTERNAL]: '#dc3545'
            };
            return colors[errorType] || '#6c757d';
        }

        createSuggestionsList(suggestions) {
            if (!suggestions || suggestions.length === 0) return '';
            
            let html = '<div class="mt-2"><small><strong>Suggestions:</strong><ul class="mb-0">';
            suggestions.forEach(suggestion => {
                html += `<li>${suggestion}</li>`;
            });
            html += '</ul></small></div>';
            
            return html;
        }

        createModalActions(uiResponse) {
            let actions = '';
            
            if (uiResponse.showRetry) {
                actions += '<button type="button" class="btn btn-primary" onclick="errorRecovery.retryLastOperation()">Retry</button>';
            }
            
            if (uiResponse.showContactSupport) {
                actions += '<button type="button" class="btn btn-outline-primary" onclick="window.open(\'mailto:support@petclinic.com\')">Contact Support</button>';
            }
            
            actions += '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>';
            
            return actions;
        }

        createToastActions(uiResponse) {
            if (!uiResponse.showRetry && !uiResponse.showFallback) return '';
            
            let actions = '<div class="ms-3">';
            
            if (uiResponse.showRetry) {
                actions += '<button type="button" class="btn btn-sm btn-outline-primary" onclick="errorRecovery.retryLastOperation()">Retry</button>';
            }
            
            if (uiResponse.showFallback) {
                actions += '<button type="button" class="btn btn-sm btn-outline-secondary" onclick="errorRecovery.activateFallback()">Offline Mode</button>';
            }
            
            actions += '</div>';
            
            return actions;
        }

        createOverlayActions(uiResponse) {
            let actions = '';
            
            if (uiResponse.showRetry) {
                actions += '<button type="button" class="btn btn-primary me-2" onclick="errorRecovery.retryLastOperation()">Retry</button>';
            }
            
            if (uiResponse.showFallback) {
                actions += '<button type="button" class="btn btn-outline-primary me-2" onclick="errorRecovery.activateFallback()">Use Offline Mode</button>';
            }
            
            actions += '<button type="button" class="btn btn-secondary" onclick="this.closest(\'.error-overlay\').remove()">Close</button>';
            
            return actions;
        }
    }

    // Create global error categorization instance
    window.errorCategorization = new ErrorCategorization();

    // Export for use by other modules
    window.ErrorCategorization = ErrorCategorization;
    window.ErrorType = ErrorType;
    window.UIResponseType = UIResponseType;

})(window);