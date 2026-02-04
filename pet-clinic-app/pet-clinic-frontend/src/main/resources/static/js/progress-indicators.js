/**
 * Progress Indicators for Long-Running Operations
 * Provides visual feedback for operations that take more than a few seconds
 * Validates: Requirements 16.4, 16.5
 */

class ProgressIndicators {
    constructor() {
        this.activeIndicators = new Map();
        this.pollingIntervals = new Map();
        this.defaultOptions = {
            showPercentage: true,
            showTimeRemaining: true,
            showCancelButton: true,
            autoClose: true,
            autoCloseDelay: 3000,
            pollingInterval: 1000
        };
        
        this.init();
    }
    
    init() {
        this.createProgressContainer();
        this.setupStyles();
        
        console.log('Progress indicators initialized');
    }
    
    createProgressContainer() {
        // Create container for progress indicators
        const container = document.createElement('div');
        container.id = 'progress-indicators-container';
        container.className = 'progress-indicators-container';
        
        document.body.appendChild(container);
        this.container = container;
    }
    
    setupStyles() {
        // Add CSS styles for progress indicators
        const style = document.createElement('style');
        style.textContent = `
            .progress-indicators-container {
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 10000;
                max-width: 400px;
            }
            
            .progress-indicator {
                background: white;
                border: 1px solid #ddd;
                border-radius: 8px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                margin-bottom: 10px;
                padding: 16px;
                animation: slideIn 0.3s ease-out;
            }
            
            .progress-indicator.completed {
                border-color: #28a745;
                background: #f8fff9;
            }
            
            .progress-indicator.failed {
                border-color: #dc3545;
                background: #fff8f8;
            }
            
            .progress-indicator.cancelled {
                border-color: #ffc107;
                background: #fffdf5;
            }
            
            .progress-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 12px;
            }
            
            .progress-title {
                font-weight: 600;
                color: #333;
                font-size: 14px;
            }
            
            .progress-close {
                background: none;
                border: none;
                font-size: 18px;
                cursor: pointer;
                color: #999;
                padding: 0;
                width: 20px;
                height: 20px;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .progress-close:hover {
                color: #666;
            }
            
            .progress-bar-container {
                background: #f0f0f0;
                border-radius: 10px;
                height: 8px;
                margin-bottom: 8px;
                overflow: hidden;
            }
            
            .progress-bar {
                background: linear-gradient(90deg, #007bff, #0056b3);
                height: 100%;
                border-radius: 10px;
                transition: width 0.3s ease;
                position: relative;
            }
            
            .progress-bar.completed {
                background: linear-gradient(90deg, #28a745, #1e7e34);
            }
            
            .progress-bar.failed {
                background: linear-gradient(90deg, #dc3545, #c82333);
            }
            
            .progress-bar.indeterminate {
                background: linear-gradient(90deg, #007bff, #0056b3, #007bff);
                background-size: 200% 100%;
                animation: indeterminate 2s linear infinite;
            }
            
            @keyframes indeterminate {
                0% { background-position: 200% 0; }
                100% { background-position: -200% 0; }
            }
            
            .progress-info {
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-size: 12px;
                color: #666;
                margin-bottom: 8px;
            }
            
            .progress-percentage {
                font-weight: 600;
            }
            
            .progress-time {
                font-style: italic;
            }
            
            .progress-message {
                font-size: 13px;
                color: #555;
                margin-bottom: 8px;
                word-wrap: break-word;
            }
            
            .progress-actions {
                display: flex;
                gap: 8px;
                justify-content: flex-end;
            }
            
            .progress-cancel-btn {
                background: #dc3545;
                color: white;
                border: none;
                padding: 4px 12px;
                border-radius: 4px;
                font-size: 12px;
                cursor: pointer;
            }
            
            .progress-cancel-btn:hover {
                background: #c82333;
            }
            
            .progress-cancel-btn:disabled {
                background: #ccc;
                cursor: not-allowed;
            }
            
            @keyframes slideIn {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            
            @keyframes slideOut {
                from {
                    transform: translateX(0);
                    opacity: 1;
                }
                to {
                    transform: translateX(100%);
                    opacity: 0;
                }
            }
            
            .progress-indicator.removing {
                animation: slideOut 0.3s ease-in forwards;
            }
        `;
        
        document.head.appendChild(style);
    }
    
    /**
     * Show progress indicator for a long-running operation
     * @param {string} operationId - Unique identifier for the operation
     * @param {string} title - Display title for the operation
     * @param {Object} options - Configuration options
     * @returns {string} Progress indicator ID
     */
    showProgress(operationId, title, options = {}) {
        const config = { ...this.defaultOptions, ...options };
        const indicatorId = this.generateIndicatorId(operationId);
        
        // Create progress indicator element
        const indicator = this.createProgressElement(indicatorId, title, config);
        this.container.appendChild(indicator);
        
        // Store indicator info
        this.activeIndicators.set(indicatorId, {
            operationId,
            title,
            config,
            element: indicator,
            startTime: Date.now(),
            lastUpdate: Date.now()
        });
        
        // Start polling for progress updates
        if (config.pollForUpdates !== false) {
            this.startPolling(indicatorId);
        }
        
        console.log(`Progress indicator created: ${indicatorId} for operation: ${operationId}`);
        return indicatorId;
    }
    
    /**
     * Update progress indicator
     * @param {string} indicatorId - Progress indicator ID
     * @param {Object} progress - Progress information
     */
    updateProgress(indicatorId, progress) {
        const indicator = this.activeIndicators.get(indicatorId);
        if (!indicator) {
            console.warn(`Progress indicator not found: ${indicatorId}`);
            return;
        }
        
        const element = indicator.element;
        const progressBar = element.querySelector('.progress-bar');
        const percentageElement = element.querySelector('.progress-percentage');
        const messageElement = element.querySelector('.progress-message');
        const timeElement = element.querySelector('.progress-time');
        
        // Update percentage
        if (progress.percentage !== undefined) {
            const percentage = Math.min(100, Math.max(0, progress.percentage));
            progressBar.style.width = percentage + '%';
            
            if (percentageElement) {
                percentageElement.textContent = Math.round(percentage) + '%';
            }
        }
        
        // Update message
        if (progress.message) {
            messageElement.textContent = progress.message;
        }
        
        // Update estimated time remaining
        if (progress.estimatedRemainingMs && timeElement) {
            const timeRemaining = this.formatTime(progress.estimatedRemainingMs);
            timeElement.textContent = `~${timeRemaining} remaining`;
        }
        
        // Handle status changes
        if (progress.status) {
            this.handleStatusChange(indicatorId, progress.status, progress);
        }
        
        indicator.lastUpdate = Date.now();
    }
    
    /**
     * Complete progress indicator
     * @param {string} indicatorId - Progress indicator ID
     * @param {string} message - Completion message
     */
    completeProgress(indicatorId, message = 'Operation completed successfully') {
        this.updateProgress(indicatorId, {
            percentage: 100,
            status: 'COMPLETED',
            message: message
        });
    }
    
    /**
     * Fail progress indicator
     * @param {string} indicatorId - Progress indicator ID
     * @param {string} message - Error message
     */
    failProgress(indicatorId, message = 'Operation failed') {
        this.updateProgress(indicatorId, {
            status: 'FAILED',
            message: message
        });
    }
    
    /**
     * Cancel progress indicator
     * @param {string} indicatorId - Progress indicator ID
     */
    async cancelProgress(indicatorId) {
        const indicator = this.activeIndicators.get(indicatorId);
        if (!indicator) {
            console.warn(`Progress indicator not found: ${indicatorId}`);
            return false;
        }
        
        try {
            // Call backend to cancel operation
            const response = await fetch(`/api/progress/${indicatorId}/cancel`, {
                method: 'POST'
            });
            
            if (response.ok) {
                this.updateProgress(indicatorId, {
                    status: 'CANCELLED',
                    message: 'Operation cancelled'
                });
                return true;
            } else {
                console.error('Failed to cancel operation:', response.statusText);
                return false;
            }
            
        } catch (error) {
            console.error('Error cancelling operation:', error);
            return false;
        }
    }
    
    /**
     * Remove progress indicator
     * @param {string} indicatorId - Progress indicator ID
     */
    removeProgress(indicatorId) {
        const indicator = this.activeIndicators.get(indicatorId);
        if (!indicator) {
            return;
        }
        
        // Stop polling
        this.stopPolling(indicatorId);
        
        // Animate removal
        const element = indicator.element;
        element.classList.add('removing');
        
        setTimeout(() => {
            if (element.parentNode) {
                element.parentNode.removeChild(element);
            }
            this.activeIndicators.delete(indicatorId);
        }, 300);
    }
    
    /**
     * Track operation with automatic progress updates
     * @param {string} operationId - Operation identifier
     * @param {string} title - Display title
     * @param {Promise} operationPromise - Promise representing the operation
     * @param {Object} options - Configuration options
     * @returns {Promise} Promise that resolves when operation completes
     */
    async trackOperation(operationId, title, operationPromise, options = {}) {
        const indicatorId = this.showProgress(operationId, title, options);
        
        try {
            const result = await operationPromise;
            this.completeProgress(indicatorId, 'Operation completed successfully');
            return result;
            
        } catch (error) {
            this.failProgress(indicatorId, `Operation failed: ${error.message}`);
            throw error;
        }
    }
    
    /**
     * Track fetch request with progress indicator
     * @param {string} url - Request URL
     * @param {Object} options - Fetch options
     * @param {string} title - Progress title
     * @returns {Promise} Fetch promise
     */
    async trackFetch(url, options = {}, title = 'Loading...') {
        const operationId = `fetch_${Date.now()}`;
        const fetchPromise = fetch(url, options);
        
        return this.trackOperation(operationId, title, fetchPromise, {
            showCancelButton: false // Can't cancel fetch easily
        });
    }
    
    // Private methods
    
    generateIndicatorId(operationId) {
        return `progress_${operationId}_${Date.now()}`;
    }
    
    createProgressElement(indicatorId, title, config) {
        const element = document.createElement('div');
        element.className = 'progress-indicator';
        element.id = indicatorId;
        
        element.innerHTML = `
            <div class="progress-header">
                <div class="progress-title">${this.escapeHtml(title)}</div>
                <button class="progress-close" onclick="progressIndicators.removeProgress('${indicatorId}')">&times;</button>
            </div>
            <div class="progress-bar-container">
                <div class="progress-bar" style="width: 0%"></div>
            </div>
            <div class="progress-info">
                ${config.showPercentage ? '<span class="progress-percentage">0%</span>' : ''}
                ${config.showTimeRemaining ? '<span class="progress-time"></span>' : ''}
            </div>
            <div class="progress-message">Starting operation...</div>
            ${config.showCancelButton ? `
                <div class="progress-actions">
                    <button class="progress-cancel-btn" onclick="progressIndicators.cancelProgress('${indicatorId}')">Cancel</button>
                </div>
            ` : ''}
        `;
        
        return element;
    }
    
    startPolling(indicatorId) {
        const interval = setInterval(async () => {
            try {
                const response = await fetch(`/api/progress/${indicatorId}`);
                if (response.ok) {
                    const progress = await response.json();
                    this.updateProgress(indicatorId, progress);
                    
                    // Stop polling if operation is complete
                    if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.status)) {
                        this.stopPolling(indicatorId);
                    }
                } else if (response.status === 404) {
                    // Progress not found, stop polling
                    this.stopPolling(indicatorId);
                }
                
            } catch (error) {
                console.error(`Error polling progress for ${indicatorId}:`, error);
            }
        }, this.defaultOptions.pollingInterval);
        
        this.pollingIntervals.set(indicatorId, interval);
    }
    
    stopPolling(indicatorId) {
        const interval = this.pollingIntervals.get(indicatorId);
        if (interval) {
            clearInterval(interval);
            this.pollingIntervals.delete(indicatorId);
        }
    }
    
    handleStatusChange(indicatorId, status, progress) {
        const indicator = this.activeIndicators.get(indicatorId);
        if (!indicator) return;
        
        const element = indicator.element;
        const progressBar = element.querySelector('.progress-bar');
        const cancelButton = element.querySelector('.progress-cancel-btn');
        
        // Update visual state based on status
        element.className = `progress-indicator ${status.toLowerCase()}`;
        
        switch (status) {
            case 'COMPLETED':
                progressBar.classList.add('completed');
                progressBar.style.width = '100%';
                if (cancelButton) cancelButton.disabled = true;
                
                // Auto-close if configured
                if (indicator.config.autoClose) {
                    setTimeout(() => {
                        this.removeProgress(indicatorId);
                    }, indicator.config.autoCloseDelay);
                }
                break;
                
            case 'FAILED':
                progressBar.classList.add('failed');
                if (cancelButton) cancelButton.disabled = true;
                break;
                
            case 'CANCELLED':
                progressBar.classList.add('cancelled');
                if (cancelButton) cancelButton.disabled = true;
                
                // Auto-close cancelled operations
                setTimeout(() => {
                    this.removeProgress(indicatorId);
                }, 2000);
                break;
        }
    }
    
    formatTime(milliseconds) {
        const seconds = Math.floor(milliseconds / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        
        if (hours > 0) {
            return `${hours}h ${minutes % 60}m`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // Public API methods
    
    /**
     * Get all active progress indicators
     * @returns {Array} Array of active indicator information
     */
    getActiveIndicators() {
        return Array.from(this.activeIndicators.entries()).map(([id, indicator]) => ({
            id,
            operationId: indicator.operationId,
            title: indicator.title,
            startTime: indicator.startTime,
            lastUpdate: indicator.lastUpdate
        }));
    }
    
    /**
     * Clear all progress indicators
     */
    clearAll() {
        const indicatorIds = Array.from(this.activeIndicators.keys());
        indicatorIds.forEach(id => this.removeProgress(id));
    }
    
    /**
     * Set default options for new progress indicators
     * @param {Object} options - Default options
     */
    setDefaultOptions(options) {
        this.defaultOptions = { ...this.defaultOptions, ...options };
    }
}

// Initialize progress indicators when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.progressIndicators = new ProgressIndicators();
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ProgressIndicators;
}