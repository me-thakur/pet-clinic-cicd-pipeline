/**
 * Error State Management System
 * Manages error states and recovery workflows across all components
 * Requirements: 15.1, 15.2, 15.4
 */

(function(window) {
    'use strict';

    /**
     * Error state manager
     */
    class ErrorStateManager {
        constructor(options = {}) {
            this.options = {
                enablePersistence: options.enablePersistence !== false,
                enableRecoveryWorkflows: options.enableRecoveryWorkflows !== false,
                maxErrorHistory: options.maxErrorHistory || 100,
                autoCleanupInterval: options.autoCleanupInterval || 300000, // 5 minutes
                ...options
            };

            this.errorStates = new Map();
            this.recoveryWorkflows = new Map();
            this.errorHistory = [];
            this.componentStates = new Map();
            this.globalState = {
                hasErrors: false,
                errorCount: 0,
                criticalErrors: 0,
                lastError: null,
                systemDegraded: false
            };

            this.init();
        }

        /**
         * Initialize error state management
         */
        init() {
            this.setupEventListeners();
            this.setupRecoveryWorkflows();
            this.startAutoCleanup();
            this.loadPersistedState();
        }

        /**
         * Setup event listeners
         */
        setupEventListeners() {
            // Listen for error events
            window.addEventListener('errorHandlerError', (event) => {
                this.handleError(event.detail.error, event.detail.context);
            });

            // Listen for recovery events
            window.addEventListener('errorRecoveryAttempt', (event) => {
                this.handleRecoveryAttempt(event.detail.error, event.detail.recovery);
            });

            window.addEventListener('errorRecoverySuccess', (event) => {
                this.handleRecoverySuccess(event.detail.errorId);
            });

            window.addEventListener('errorRecoveryFailure', (event) => {
                this.handleRecoveryFailure(event.detail.errorId, event.detail.reason);
            });

            // Listen for network events
            window.addEventListener('networkOffline', () => {
                this.handleNetworkOffline();
            });

            window.addEventListener('networkOnline', () => {
                this.handleNetworkOnline();
            });

            // Listen for page unload to persist state
            window.addEventListener('beforeunload', () => {
                this.persistState();
            });
        }

        /**
         * Setup recovery workflows
         */
        setupRecoveryWorkflows() {
            // Network error recovery workflow
            this.recoveryWorkflows.set('NETWORK_ERROR', {
                steps: [
                    { action: 'checkConnectivity', timeout: 5000 },
                    { action: 'retryRequest', maxAttempts: 3, delay: 1000 },
                    { action: 'enableOfflineMode', fallback: true }
                ],
                onSuccess: 'clearNetworkErrors',
                onFailure: 'showNetworkErrorDialog'
            });

            // Service unavailable recovery workflow
            this.recoveryWorkflows.set('SERVICE_UNAVAILABLE', {
                steps: [
                    { action: 'checkServiceHealth', timeout: 10000 },
                    { action: 'retryWithBackoff', maxAttempts: 5, delay: 2000 },
                    { action: 'enableDegradedMode', fallback: true }
                ],
                onSuccess: 'clearServiceErrors',
                onFailure: 'showServiceErrorDialog'
            });

            // Validation error recovery workflow
            this.recoveryWorkflows.set('VALIDATION_FAILED', {
                steps: [
                    { action: 'highlightFields', immediate: true },
                    { action: 'showFieldGuidance', immediate: true },
                    { action: 'enableClientSideValidation', fallback: true }
                ],
                onSuccess: 'clearValidationErrors',
                onFailure: 'showValidationHelp'
            });

            // Authentication error recovery workflow
            this.recoveryWorkflows.set('AUTHENTICATION_FAILED', {
                steps: [
                    { action: 'checkSession', timeout: 3000 },
                    { action: 'refreshToken', maxAttempts: 1 },
                    { action: 'redirectToLogin', fallback: true }
                ],
                onSuccess: 'clearAuthErrors',
                onFailure: 'forceLogin'
            });
        }

        /**
         * Handle error occurrence
         */
        handleError(error, context = {}) {
            const errorId = this.generateErrorId();
            const timestamp = new Date();
            
            // Create error state
            const errorState = {
                id: errorId,
                error: error,
                context: context,
                timestamp: timestamp,
                status: 'active',
                recoveryAttempts: 0,
                lastRecoveryAttempt: null,
                recoveryHistory: [],
                componentId: context.componentId || 'unknown',
                severity: this.determineSeverity(error),
                category: this.categorizeError(error)
            };

            // Store error state
            this.errorStates.set(errorId, errorState);

            // Add to history
            this.addToHistory(errorState);

            // Update component state
            this.updateComponentState(errorState.componentId, errorState);

            // Update global state
            this.updateGlobalState();

            // Start recovery workflow if enabled
            if (this.options.enableRecoveryWorkflows) {
                this.startRecoveryWorkflow(errorId);
            }

            // Persist state if enabled
            if (this.options.enablePersistence) {
                this.persistState();
            }

            return errorId;
        }

        /**
         * Handle recovery attempt
         */
        handleRecoveryAttempt(error, recovery) {
            const errorState = this.findErrorState(error);
            if (!errorState) return;

            errorState.recoveryAttempts++;
            errorState.lastRecoveryAttempt = new Date();
            errorState.status = 'recovering';
            
            // Add to recovery history
            errorState.recoveryHistory.push({
                timestamp: new Date(),
                strategy: recovery.strategy,
                attempt: errorState.recoveryAttempts,
                status: 'in_progress'
            });

            this.updateGlobalState();
            this.persistState();
        }

        /**
         * Handle recovery success
         */
        handleRecoverySuccess(errorId) {
            const errorState = this.errorStates.get(errorId);
            if (!errorState) return;

            errorState.status = 'resolved';
            
            // Update recovery history
            const lastRecovery = errorState.recoveryHistory[errorState.recoveryHistory.length - 1];
            if (lastRecovery) {
                lastRecovery.status = 'success';
                lastRecovery.completedAt = new Date();
            }

            // Remove from active errors
            this.errorStates.delete(errorId);

            // Update component state
            this.updateComponentState(errorState.componentId, null, errorId);

            // Update global state
            this.updateGlobalState();

            // Trigger success workflow
            this.executeSuccessWorkflow(errorState);

            this.persistState();
        }

        /**
         * Handle recovery failure
         */
        handleRecoveryFailure(errorId, reason) {
            const errorState = this.errorStates.get(errorId);
            if (!errorState) return;

            errorState.status = 'failed';
            
            // Update recovery history
            const lastRecovery = errorState.recoveryHistory[errorState.recoveryHistory.length - 1];
            if (lastRecovery) {
                lastRecovery.status = 'failed';
                lastRecovery.reason = reason;
                lastRecovery.completedAt = new Date();
            }

            // Check if we should continue with workflow
            const workflow = this.recoveryWorkflows.get(errorState.category);
            if (workflow && errorState.recoveryAttempts < this.getMaxRecoveryAttempts(workflow)) {
                // Continue recovery workflow
                setTimeout(() => {
                    this.continueRecoveryWorkflow(errorId);
                }, this.getRecoveryDelay(errorState.recoveryAttempts));
            } else {
                // Execute failure workflow
                this.executeFailureWorkflow(errorState);
            }

            this.updateGlobalState();
            this.persistState();
        }

        /**
         * Handle network offline
         */
        handleNetworkOffline() {
            // Mark all network-related errors as potentially recoverable
            for (const [errorId, errorState] of this.errorStates.entries()) {
                if (errorState.category === 'NETWORK_ERROR' && errorState.status === 'failed') {
                    errorState.status = 'pending_network';
                }
            }

            // Update global state
            this.globalState.systemDegraded = true;
            this.updateGlobalState();
        }

        /**
         * Handle network online
         */
        handleNetworkOnline() {
            // Retry network-related errors
            for (const [errorId, errorState] of this.errorStates.entries()) {
                if (errorState.status === 'pending_network') {
                    this.startRecoveryWorkflow(errorId);
                }
            }

            // Update global state
            this.globalState.systemDegraded = false;
            this.updateGlobalState();
        }

        /**
         * Start recovery workflow for an error
         */
        async startRecoveryWorkflow(errorId) {
            const errorState = this.errorStates.get(errorId);
            if (!errorState) return;

            const workflow = this.recoveryWorkflows.get(errorState.category);
            if (!workflow) return;

            try {
                await this.executeRecoveryWorkflow(errorId, workflow);
            } catch (error) {
                console.error('Recovery workflow failed:', error);
                this.handleRecoveryFailure(errorId, error.message);
            }
        }

        /**
         * Execute recovery workflow
         */
        async executeRecoveryWorkflow(errorId, workflow) {
            const errorState = this.errorStates.get(errorId);
            if (!errorState) return;

            for (const step of workflow.steps) {
                try {
                    const result = await this.executeRecoveryStep(errorId, step);
                    
                    if (result.success) {
                        // Step succeeded, continue to next step or complete
                        if (step.fallback) {
                            // This was a fallback step, workflow complete
                            this.handleRecoverySuccess(errorId);
                            return;
                        }
                        continue;
                    } else {
                        // Step failed, try next step or fail
                        if (step.required) {
                            throw new Error(`Required step failed: ${step.action}`);
                        }
                        continue;
                    }
                } catch (stepError) {
                    console.error(`Recovery step ${step.action} failed:`, stepError);
                    
                    if (step.required) {
                        throw stepError;
                    }
                }
            }

            // All steps completed successfully
            this.handleRecoverySuccess(errorId);
        }

        /**
         * Execute individual recovery step
         */
        async executeRecoveryStep(errorId, step) {
            const errorState = this.errorStates.get(errorId);
            if (!errorState) return { success: false };

            switch (step.action) {
                case 'checkConnectivity':
                    return await this.checkConnectivity(step.timeout);
                
                case 'retryRequest':
                    return await this.retryRequest(errorState, step);
                
                case 'enableOfflineMode':
                    return this.enableOfflineMode(errorState.componentId);
                
                case 'checkServiceHealth':
                    return await this.checkServiceHealth(errorState, step.timeout);
                
                case 'retryWithBackoff':
                    return await this.retryWithBackoff(errorState, step);
                
                case 'enableDegradedMode':
                    return this.enableDegradedMode(errorState.componentId);
                
                case 'highlightFields':
                    return this.highlightFields(errorState);
                
                case 'showFieldGuidance':
                    return this.showFieldGuidance(errorState);
                
                case 'enableClientSideValidation':
                    return this.enableClientSideValidation(errorState.componentId);
                
                case 'checkSession':
                    return await this.checkSession(step.timeout);
                
                case 'refreshToken':
                    return await this.refreshToken();
                
                case 'redirectToLogin':
                    return this.redirectToLogin();
                
                default:
                    console.warn(`Unknown recovery step: ${step.action}`);
                    return { success: false };
            }
        }

        /**
         * Recovery step implementations
         */
        async checkConnectivity(timeout) {
            try {
                const isOnline = await window.networkMonitor.waitForConnectivity(timeout);
                return { success: isOnline };
            } catch (error) {
                return { success: false, error };
            }
        }

        async retryRequest(errorState, step) {
            if (!errorState.context.retryFunction) {
                return { success: false, reason: 'No retry function available' };
            }

            try {
                const result = await errorState.context.retryFunction();
                return { success: true, result };
            } catch (error) {
                return { success: false, error };
            }
        }

        enableOfflineMode(componentId) {
            window.dispatchEvent(new CustomEvent('enableOfflineMode', {
                detail: { componentId }
            }));
            return { success: true };
        }

        async checkServiceHealth(errorState, timeout) {
            try {
                const response = await fetch('/api/health/check', {
                    method: 'GET',
                    timeout: timeout
                });
                return { success: response.ok };
            } catch (error) {
                return { success: false, error };
            }
        }

        async retryWithBackoff(errorState, step) {
            const delay = step.delay * Math.pow(2, errorState.recoveryAttempts - 1);
            await this.delay(delay);
            return await this.retryRequest(errorState, step);
        }

        enableDegradedMode(componentId) {
            window.dispatchEvent(new CustomEvent('enableDegradedMode', {
                detail: { componentId }
            }));
            return { success: true };
        }

        highlightFields(errorState) {
            if (errorState.error.fieldErrors) {
                errorState.error.fieldErrors.forEach(fieldError => {
                    const field = document.querySelector(`[name="${fieldError.field}"]`);
                    if (field) {
                        field.classList.add('is-invalid');
                    }
                });
            }
            return { success: true };
        }

        showFieldGuidance(errorState) {
            if (errorState.error.fieldErrors) {
                errorState.error.fieldErrors.forEach(fieldError => {
                    window.errorDisplay.displayFieldError(
                        document.querySelector(`[name="${fieldError.field}"]`),
                        fieldError
                    );
                });
            }
            return { success: true };
        }

        enableClientSideValidation(componentId) {
            window.dispatchEvent(new CustomEvent('enableClientSideValidation', {
                detail: { componentId }
            }));
            return { success: true };
        }

        async checkSession(timeout) {
            try {
                const response = await fetch('/api/auth/session', {
                    method: 'GET',
                    timeout: timeout
                });
                return { success: response.ok };
            } catch (error) {
                return { success: false, error };
            }
        }

        async refreshToken() {
            try {
                const response = await fetch('/api/auth/refresh', {
                    method: 'POST'
                });
                return { success: response.ok };
            } catch (error) {
                return { success: false, error };
            }
        }

        redirectToLogin() {
            setTimeout(() => {
                window.location.href = '/login';
            }, 3000);
            return { success: true };
        }

        /**
         * Helper methods
         */
        findErrorState(error) {
            for (const errorState of this.errorStates.values()) {
                if (errorState.error === error || errorState.error.errorCode === error.errorCode) {
                    return errorState;
                }
            }
            return null;
        }

        generateErrorId() {
            return `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        }

        determineSeverity(error) {
            if (error.severity) return error.severity;
            if (error.status >= 500) return 'critical';
            if (error.status >= 400) return 'error';
            return 'warning';
        }

        categorizeError(error) {
            if (error.errorCode) return error.errorCode;
            if (error.name === 'TypeError' && error.message.includes('fetch')) return 'NETWORK_ERROR';
            if (error.status === 503) return 'SERVICE_UNAVAILABLE';
            if (error.status === 401) return 'AUTHENTICATION_FAILED';
            if (error.status === 400) return 'VALIDATION_FAILED';
            return 'UNKNOWN_ERROR';
        }

        addToHistory(errorState) {
            this.errorHistory.push({
                id: errorState.id,
                timestamp: errorState.timestamp,
                category: errorState.category,
                severity: errorState.severity,
                componentId: errorState.componentId,
                message: errorState.error.message
            });

            // Limit history size
            if (this.errorHistory.length > this.options.maxErrorHistory) {
                this.errorHistory.shift();
            }
        }

        updateComponentState(componentId, errorState, resolvedErrorId = null) {
            if (!this.componentStates.has(componentId)) {
                this.componentStates.set(componentId, {
                    hasErrors: false,
                    errorCount: 0,
                    errors: [],
                    lastError: null
                });
            }

            const componentState = this.componentStates.get(componentId);

            if (resolvedErrorId) {
                // Remove resolved error
                componentState.errors = componentState.errors.filter(e => e.id !== resolvedErrorId);
            } else if (errorState) {
                // Add new error
                componentState.errors.push(errorState);
                componentState.lastError = errorState;
            }

            componentState.errorCount = componentState.errors.length;
            componentState.hasErrors = componentState.errorCount > 0;
        }

        updateGlobalState() {
            const activeErrors = Array.from(this.errorStates.values());
            
            this.globalState.hasErrors = activeErrors.length > 0;
            this.globalState.errorCount = activeErrors.length;
            this.globalState.criticalErrors = activeErrors.filter(e => e.severity === 'critical').length;
            this.globalState.lastError = activeErrors.length > 0 ? 
                activeErrors[activeErrors.length - 1] : null;

            // Broadcast state change
            window.dispatchEvent(new CustomEvent('errorStateChanged', {
                detail: { globalState: this.globalState }
            }));
        }

        getMaxRecoveryAttempts(workflow) {
            return workflow.steps.reduce((max, step) => 
                Math.max(max, step.maxAttempts || 1), 1);
        }

        getRecoveryDelay(attemptNumber) {
            return Math.min(1000 * Math.pow(2, attemptNumber), 30000); // Max 30 seconds
        }

        executeSuccessWorkflow(errorState) {
            const workflow = this.recoveryWorkflows.get(errorState.category);
            if (workflow && workflow.onSuccess) {
                this.executeWorkflowAction(workflow.onSuccess, errorState);
            }
        }

        executeFailureWorkflow(errorState) {
            const workflow = this.recoveryWorkflows.get(errorState.category);
            if (workflow && workflow.onFailure) {
                this.executeWorkflowAction(workflow.onFailure, errorState);
            }
        }

        executeWorkflowAction(action, errorState) {
            switch (action) {
                case 'clearNetworkErrors':
                    this.clearErrorsByCategory('NETWORK_ERROR');
                    break;
                case 'clearServiceErrors':
                    this.clearErrorsByCategory('SERVICE_UNAVAILABLE');
                    break;
                case 'clearValidationErrors':
                    this.clearErrorsByCategory('VALIDATION_FAILED');
                    break;
                case 'clearAuthErrors':
                    this.clearErrorsByCategory('AUTHENTICATION_FAILED');
                    break;
                default:
                    console.warn(`Unknown workflow action: ${action}`);
            }
        }

        clearErrorsByCategory(category) {
            const errorsToRemove = [];
            for (const [errorId, errorState] of this.errorStates.entries()) {
                if (errorState.category === category) {
                    errorsToRemove.push(errorId);
                }
            }

            errorsToRemove.forEach(errorId => {
                this.errorStates.delete(errorId);
            });

            this.updateGlobalState();
        }

        startAutoCleanup() {
            setInterval(() => {
                this.cleanupOldErrors();
            }, this.options.autoCleanupInterval);
        }

        cleanupOldErrors() {
            const now = Date.now();
            const maxAge = 24 * 60 * 60 * 1000; // 24 hours

            const errorsToRemove = [];
            for (const [errorId, errorState] of this.errorStates.entries()) {
                if (now - errorState.timestamp.getTime() > maxAge && 
                    errorState.status === 'resolved') {
                    errorsToRemove.push(errorId);
                }
            }

            errorsToRemove.forEach(errorId => {
                this.errorStates.delete(errorId);
            });

            if (errorsToRemove.length > 0) {
                this.updateGlobalState();
                this.persistState();
            }
        }

        persistState() {
            if (!this.options.enablePersistence) return;

            try {
                const stateToSave = {
                    errorHistory: this.errorHistory.slice(-50), // Save last 50 errors
                    globalState: this.globalState,
                    timestamp: new Date().toISOString()
                };

                localStorage.setItem('errorStateManager', JSON.stringify(stateToSave));
            } catch (error) {
                console.warn('Failed to persist error state:', error);
            }
        }

        loadPersistedState() {
            if (!this.options.enablePersistence) return;

            try {
                const savedState = localStorage.getItem('errorStateManager');
                if (savedState) {
                    const state = JSON.parse(savedState);
                    this.errorHistory = state.errorHistory || [];
                    // Don't restore active errors, only history
                }
            } catch (error) {
                console.warn('Failed to load persisted error state:', error);
            }
        }

        delay(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        /**
         * Public API
         */
        getGlobalState() {
            return { ...this.globalState };
        }

        getComponentState(componentId) {
            return this.componentStates.get(componentId) || {
                hasErrors: false,
                errorCount: 0,
                errors: [],
                lastError: null
            };
        }

        getErrorHistory() {
            return [...this.errorHistory];
        }

        getActiveErrors() {
            return Array.from(this.errorStates.values());
        }

        clearAllErrors() {
            this.errorStates.clear();
            this.componentStates.clear();
            this.updateGlobalState();
            this.persistState();
        }

        getErrorStatistics() {
            const history = this.errorHistory;
            const stats = {
                total: history.length,
                byCategory: {},
                bySeverity: {},
                byComponent: {},
                recoveryRate: 0
            };

            history.forEach(error => {
                stats.byCategory[error.category] = (stats.byCategory[error.category] || 0) + 1;
                stats.bySeverity[error.severity] = (stats.bySeverity[error.severity] || 0) + 1;
                stats.byComponent[error.componentId] = (stats.byComponent[error.componentId] || 0) + 1;
            });

            // Calculate recovery rate
            const resolvedErrors = history.filter(e => e.status === 'resolved').length;
            stats.recoveryRate = history.length > 0 ? (resolvedErrors / history.length) * 100 : 0;

            return stats;
        }
    }

    // Create global error state manager instance
    window.errorStateManager = new ErrorStateManager();

    // Export for use by other modules
    window.ErrorStateManager = ErrorStateManager;

})(window);