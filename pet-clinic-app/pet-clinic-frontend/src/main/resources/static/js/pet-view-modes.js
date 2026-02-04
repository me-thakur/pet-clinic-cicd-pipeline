/**
 * Pet View Modes JavaScript
 * 
 * Handles view mode switching functionality for the pets page.
 * Implements proper state management, loading indicators, error handling,
 * and view preference persistence.
 * 
 * Validates: Requirements 11.1, 11.2, 11.3, 11.5
 */

class PetViewModeManager {
    constructor() {
        this.currentView = this.getCurrentViewFromUrl();
        this.isLoading = false;
        this.preferences = this.loadPreferences();
        
        this.init();
    }

    /**
     * Initialize the view mode manager
     */
    init() {
        this.setupViewToggleButtons();
        this.setupLoadingStates();
        this.setupErrorHandling();
        this.applyStoredPreferences();
        this.setupRefreshButtons();
        
        console.log('Pet View Mode Manager initialized', {
            currentView: this.currentView,
            preferences: this.preferences
        });
    }

    /**
     * Setup view toggle buttons with proper state indicators
     */
    setupViewToggleButtons() {
        const standardButton = document.querySelector('a[href="/pets"]');
        const enhancedButton = document.querySelector('a[href="/pets/enhanced"]');
        
        if (!standardButton || !enhancedButton) {
            console.warn('View toggle buttons not found');
            return;
        }

        // Add click handlers with loading states
        standardButton.addEventListener('click', (e) => {
            if (this.currentView === 'standard') {
                e.preventDefault();
                return;
            }
            
            this.handleViewSwitch(e, 'standard', standardButton);
        });

        enhancedButton.addEventListener('click', (e) => {
            if (this.currentView === 'enhanced') {
                e.preventDefault();
                return;
            }
            
            this.handleViewSwitch(e, 'enhanced', enhancedButton);
        });

        // Update button states
        this.updateButtonStates(standardButton, enhancedButton);
    }

    /**
     * Handle view switching with loading states and error handling
     */
    async handleViewSwitch(event, targetView, button) {
        event.preventDefault();
        
        if (this.isLoading) {
            return;
        }

        try {
            this.setLoadingState(true, button);
            
            // Store the current page and filters before switching
            const currentParams = this.getCurrentPageParams();
            
            // Build target URL with current parameters
            const baseUrl = targetView === 'standard' ? '/pets' : '/pets/enhanced';
            const targetUrl = this.buildUrlWithParams(baseUrl, currentParams);
            
            // Store preference
            this.saveViewPreference(targetView);
            
            // Navigate to new view
            window.location.href = targetUrl;
            
        } catch (error) {
            console.error('Error switching view:', error);
            this.handleViewSwitchError(error, targetView);
            this.setLoadingState(false, button);
        }
    }

    /**
     * Setup loading states for view switching
     */
    setupLoadingStates() {
        // Add loading overlay container if it doesn't exist
        if (!document.getElementById('viewSwitchLoadingOverlay')) {
            const overlay = document.createElement('div');
            overlay.id = 'viewSwitchLoadingOverlay';
            overlay.className = 'view-switch-loading-overlay';
            overlay.innerHTML = `
                <div class="loading-content">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <div class="loading-text mt-2">Switching view...</div>
                </div>
            `;
            document.body.appendChild(overlay);
        }
    }

    /**
     * Setup error handling for view modes
     */
    setupErrorHandling() {
        // Check for error messages in the page
        const errorAlert = document.querySelector('.alert-danger');
        const fallbackAlert = document.querySelector('.alert-warning');
        
        if (errorAlert && errorAlert.textContent.includes('enhanced')) {
            this.handleEnhancedViewError();
        }
        
        if (fallbackAlert && fallbackAlert.textContent.includes('Enhanced view temporarily unavailable')) {
            this.handleEnhancedViewFallback();
        }
    }

    /**
     * Setup refresh owner info buttons for enhanced view
     */
    setupRefreshButtons() {
        const refreshButtons = document.querySelectorAll('.refresh-owner-info');
        
        refreshButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                this.handleRefreshOwnerInfo(e, button);
            });
        });
    }

    /**
     * Handle refresh owner info functionality
     */
    async handleRefreshOwnerInfo(event, button) {
        event.preventDefault();
        
        const petId = button.getAttribute('data-pet-id');
        if (!petId) {
            console.error('Pet ID not found for refresh button');
            return;
        }

        const originalIcon = button.querySelector('i');
        const originalText = button.querySelector('span');
        const originalIconClass = originalIcon.className;
        const originalTextContent = originalText ? originalText.textContent : '';

        try {
            // Show loading state
            originalIcon.className = 'fas fa-spinner fa-spin';
            if (originalText) originalText.textContent = 'Refreshing...';
            button.disabled = true;
            button.classList.add('loading');

            // Make AJAX request to refresh owner info
            const response = await fetch(`/pets/${petId}/refresh-owner-info`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            const result = await response.text();

            if (result === 'success') {
                // Show success state briefly
                originalIcon.className = 'fas fa-check';
                if (originalText) originalText.textContent = 'Updated';
                button.classList.add('btn-success');
                button.classList.remove('btn-outline-warning');

                // Reload the page after a short delay to show updated information
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                throw new Error(result.startsWith('error:') ? result.substring(7) : 'Unknown error');
            }

        } catch (error) {
            console.error('Error refreshing owner info:', error);
            
            // Show error state
            originalIcon.className = 'fas fa-exclamation-triangle';
            if (originalText) originalText.textContent = 'Error';
            button.classList.add('btn-danger');
            button.classList.remove('btn-outline-warning');

            // Show error message
            this.showErrorMessage(`Failed to refresh owner info: ${error.message}`);

            // Reset button after delay
            setTimeout(() => {
                originalIcon.className = originalIconClass;
                if (originalText) originalText.textContent = originalTextContent;
                button.disabled = false;
                button.classList.remove('loading', 'btn-success', 'btn-danger');
                button.classList.add('btn-outline-warning');
            }, 3000);
        }
    }

    /**
     * Set loading state for view switching
     */
    setLoadingState(loading, button = null) {
        this.isLoading = loading;
        
        const overlay = document.getElementById('viewSwitchLoadingOverlay');
        if (overlay) {
            overlay.style.display = loading ? 'flex' : 'none';
        }

        if (button) {
            const icon = button.querySelector('i');
            const text = button.querySelector('span');
            
            if (loading) {
                button.classList.add('loading');
                if (icon) {
                    icon.dataset.originalClass = icon.className;
                    icon.className = 'fas fa-spinner fa-spin';
                }
                if (text) {
                    text.dataset.originalText = text.textContent;
                    text.textContent = 'Loading...';
                }
            } else {
                button.classList.remove('loading');
                if (icon && icon.dataset.originalClass) {
                    icon.className = icon.dataset.originalClass;
                }
                if (text && text.dataset.originalText) {
                    text.textContent = text.dataset.originalText;
                }
            }
        }
    }

    /**
     * Update button states to reflect current view
     */
    updateButtonStates(standardButton, enhancedButton) {
        // Remove active states
        standardButton.classList.remove('btn-secondary', 'active');
        standardButton.classList.add('btn-outline-secondary');
        enhancedButton.classList.remove('btn-secondary', 'active');
        enhancedButton.classList.add('btn-outline-secondary');

        // Add active state to current view
        if (this.currentView === 'standard') {
            standardButton.classList.remove('btn-outline-secondary');
            standardButton.classList.add('btn-secondary', 'active');
        } else if (this.currentView === 'enhanced') {
            enhancedButton.classList.remove('btn-outline-secondary');
            enhancedButton.classList.add('btn-secondary', 'active');
        }

        // Add state indicators
        this.addStateIndicators(standardButton, enhancedButton);
    }

    /**
     * Add visual state indicators to buttons
     */
    addStateIndicators(standardButton, enhancedButton) {
        // Remove existing indicators
        const existingIndicators = document.querySelectorAll('.view-state-indicator');
        existingIndicators.forEach(indicator => indicator.remove());

        // Add indicator to active button
        const activeButton = this.currentView === 'standard' ? standardButton : enhancedButton;
        const indicator = document.createElement('span');
        indicator.className = 'view-state-indicator badge bg-light text-dark ms-1';
        indicator.textContent = 'Active';
        indicator.style.fontSize = '0.7em';
        activeButton.appendChild(indicator);
    }

    /**
     * Get current view from URL
     */
    getCurrentViewFromUrl() {
        const path = window.location.pathname;
        if (path.includes('/enhanced')) {
            return 'enhanced';
        } else if (path.startsWith('/pets')) {
            return 'standard';
        }
        return 'standard';
    }

    /**
     * Get current page parameters (page, filters, etc.)
     */
    getCurrentPageParams() {
        const urlParams = new URLSearchParams(window.location.search);
        const params = {};
        
        // Preserve pagination
        if (urlParams.has('page')) {
            params.page = urlParams.get('page');
        }
        if (urlParams.has('size')) {
            params.size = urlParams.get('size');
        }
        
        // Preserve search parameters
        const searchParams = ['name', 'species', 'breed', 'minAge', 'maxAge', 'medicalHistory', 'q'];
        searchParams.forEach(param => {
            if (urlParams.has(param)) {
                params[param] = urlParams.get(param);
            }
        });
        
        return params;
    }

    /**
     * Build URL with parameters
     */
    buildUrlWithParams(baseUrl, params) {
        const url = new URL(baseUrl, window.location.origin);
        Object.keys(params).forEach(key => {
            if (params[key]) {
                url.searchParams.set(key, params[key]);
            }
        });
        return url.toString();
    }

    /**
     * Load user preferences from localStorage
     */
    loadPreferences() {
        try {
            const stored = localStorage.getItem('petViewPreferences');
            return stored ? JSON.parse(stored) : { defaultView: 'standard' };
        } catch (error) {
            console.warn('Error loading view preferences:', error);
            return { defaultView: 'standard' };
        }
    }

    /**
     * Save view preference to localStorage
     */
    saveViewPreference(view) {
        try {
            this.preferences.defaultView = view;
            this.preferences.lastChanged = new Date().toISOString();
            localStorage.setItem('petViewPreferences', JSON.stringify(this.preferences));
            console.log('View preference saved:', view);
        } catch (error) {
            console.warn('Error saving view preference:', error);
        }
    }

    /**
     * Apply stored preferences if user hasn't explicitly chosen a view
     */
    applyStoredPreferences() {
        // Only apply stored preferences if we're on the default pets page
        // and the user hasn't explicitly chosen a view
        const isDefaultPage = window.location.pathname === '/pets' && !window.location.search;
        const hasStoredPreference = this.preferences.defaultView && this.preferences.defaultView !== 'standard';
        
        if (isDefaultPage && hasStoredPreference && this.preferences.defaultView === 'enhanced') {
            // Redirect to enhanced view if that's the user's preference
            console.log('Applying stored preference: enhanced view');
            window.location.href = '/pets/enhanced';
        }
    }

    /**
     * Handle enhanced view error
     */
    handleEnhancedViewError() {
        console.warn('Enhanced view error detected');
        
        // Add retry button to error message
        const errorAlert = document.querySelector('.alert-danger');
        if (errorAlert && !errorAlert.querySelector('.retry-enhanced-view')) {
            const retryButton = document.createElement('button');
            retryButton.className = 'btn btn-sm btn-outline-danger retry-enhanced-view ms-2';
            retryButton.innerHTML = '<i class="fas fa-redo"></i> Retry Enhanced View';
            retryButton.addEventListener('click', () => {
                window.location.reload();
            });
            errorAlert.appendChild(retryButton);
        }
    }

    /**
     * Handle enhanced view fallback
     */
    handleEnhancedViewFallback() {
        console.info('Enhanced view fallback detected');
        
        // Add option to try enhanced view again
        const fallbackAlert = document.querySelector('.alert-warning');
        if (fallbackAlert && !fallbackAlert.querySelector('.try-enhanced-again')) {
            const tryAgainButton = document.createElement('button');
            tryAgainButton.className = 'btn btn-sm btn-outline-warning try-enhanced-again ms-2';
            tryAgainButton.innerHTML = '<i class="fas fa-redo"></i> Try Enhanced View Again';
            tryAgainButton.addEventListener('click', () => {
                window.location.href = '/pets/enhanced';
            });
            fallbackAlert.appendChild(tryAgainButton);
        }
    }

    /**
     * Show error message to user
     */
    showErrorMessage(message) {
        // Create or update error alert
        let errorAlert = document.querySelector('.pet-view-error-alert');
        
        if (!errorAlert) {
            errorAlert = document.createElement('div');
            errorAlert.className = 'alert alert-danger alert-dismissible fade show pet-view-error-alert';
            errorAlert.innerHTML = `
                <i class="fas fa-exclamation-triangle"></i>
                <span class="error-message"></span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            `;
            
            // Insert at the top of the content
            const content = document.querySelector('[th\\:fragment="content"], .container, main');
            if (content) {
                content.insertBefore(errorAlert, content.firstChild);
            }
        }
        
        const messageSpan = errorAlert.querySelector('.error-message');
        if (messageSpan) {
            messageSpan.textContent = message;
        }
        
        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            if (errorAlert && errorAlert.parentNode) {
                errorAlert.remove();
            }
        }, 5000);
    }

    /**
     * Get current view mode
     */
    getCurrentView() {
        return this.currentView;
    }

    /**
     * Check if currently in enhanced view
     */
    isEnhancedView() {
        return this.currentView === 'enhanced';
    }

    /**
     * Check if currently in standard view
     */
    isStandardView() {
        return this.currentView === 'standard';
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Only initialize on pets pages
    if (window.location.pathname.startsWith('/pets')) {
        window.petViewModeManager = new PetViewModeManager();
        console.log('Pet View Mode Manager initialized');
    }
});

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PetViewModeManager;
}