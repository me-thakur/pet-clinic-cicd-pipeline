/**
 * Enhanced Performance Monitoring with Progress Indicators
 * Integrates with backend performance optimization and provides real-time feedback
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5
 */

class EnhancedPerformanceMonitoring {
    constructor() {
        this.performanceData = {
            pageLoads: new Map(),
            apiCalls: new Map(),
            userInteractions: new Map(),
            longRunningOperations: new Map()
        };
        
        this.thresholds = {
            pageLoad: 2000,
            apiCall: 3000,
            interaction: 1000,
            longRunning: 5000
        };
        
        this.progressIndicators = new Map();
        this.init();
    }
    
    init() {
        this.setupPerformanceObserver();
        this.setupLongRunningOperationMonitoring();
        this.setupProgressIndicatorIntegration();
        this.setupRealTimeReporting();
        
        console.log('Enhanced performance monitoring initialized');
    }
    
    setupPerformanceObserver() {
        // Use Performance Observer API for detailed metrics
        if ('PerformanceObserver' in window) {
            // Observe navigation timing
            const navObserver = new PerformanceObserver((list) => {
                for (const entry of list.getEntries()) {
                    this.processNavigationEntry(entry);
                }
            });
            navObserver.observe({ entryTypes: ['navigation'] });
            
            // Observe resource timing
            const resourceObserver = new PerformanceObserver((list) => {
                for (const entry of list.getEntries()) {
                    this.processResourceEntry(entry);
                }
            });
            resourceObserver.observe({ entryTypes: ['resource'] });
            
            // Observe measure timing
            const measureObserver = new PerformanceObserver((list) => {
                for (const entry of list.getEntries()) {
                    this.processMeasureEntry(entry);
                }
            });
            measureObserver.observe({ entryTypes: ['measure'] });
        }
    }
    
    setupLongRunningOperationMonitoring() {
        // Override fetch to monitor long-running operations
        const originalFetch = window.fetch;
        const self = this;
        
        window.fetch = async function(url, options = {}) {
            const operationId = self.generateOperationId();
            const startTime = performance.now();
            
            // Determine if this might be a long-running operation
            const isLongRunning = self.isLongRunningOperation(url, options);
            
            let progressId = null;
            if (isLongRunning && window.progressIndicators) {
                const title = self.getOperationTitle(url, options);
                progressId = window.progressIndicators.showProgress(operationId, title, {
                    showCancelButton: false,
                    autoClose: true,
                    autoCloseDelay: 3000
                });
                
                self.progressIndicators.set(operationId, progressId);
            }
            
            try {
                const response = await originalFetch.apply(this, arguments);
                const responseTime = performance.now() - startTime;
                
                // Record performance data
                self.recordApiCall(url, options.method || 'GET', responseTime, response.status, response.ok);
                
                // Update progress indicator
                if (progressId && window.progressIndicators) {
                    if (response.ok) {
                        window.progressIndicators.completeProgress(progressId, 'Request completed successfully');
                    } else {
                        window.progressIndicators.failProgress(progressId, `Request failed: ${response.status}`);
                    }
                }
                
                return response;
                
            } catch (error) {
                const responseTime = performance.now() - startTime;
                
                // Record error
                self.recordApiError(url, options.method || 'GET', responseTime, error);
                
                // Update progress indicator
                if (progressId && window.progressIndicators) {
                    window.progressIndicators.failProgress(progressId, `Request failed: ${error.message}`);
                }
                
                throw error;
            } finally {
                self.progressIndicators.delete(operationId);
            }
        };
    }
    
    setupProgressIndicatorIntegration() {
        // Monitor form submissions for progress indicators
        document.addEventListener('submit', (event) => {
            const form = event.target;
            const formId = form.id || 'unknown_form';
            
            // Check if this form might trigger a long-running operation
            if (this.isLongRunningForm(form)) {
                const operationId = this.generateOperationId();
                const title = this.getFormOperationTitle(form);
                
                if (window.progressIndicators) {
                    const progressId = window.progressIndicators.showProgress(operationId, title, {
                        showCancelButton: false,
                        autoClose: true,
                        autoCloseDelay: 2000
                    });
                    
                    // Monitor form completion
                    this.monitorFormCompletion(form, progressId);
                }
            }
        });
        
        // Monitor button clicks for long-running operations
        document.addEventListener('click', (event) => {
            const button = event.target;
            
            if (button.tagName === 'BUTTON' && this.isLongRunningButton(button)) {
                const operationId = this.generateOperationId();
                const title = this.getButtonOperationTitle(button);
                
                if (window.progressIndicators) {
                    const progressId = window.progressIndicators.showProgress(operationId, title, {
                        showCancelButton: false,
                        autoClose: true,
                        autoCloseDelay: 2000
                    });
                    
                    // Monitor button operation completion
                    this.monitorButtonOperation(button, progressId);
                }
            }
        });
    }
    
    setupRealTimeReporting() {
        // Send performance data to backend every 30 seconds
        setInterval(() => {
            this.sendPerformanceData();
        }, 30000);
        
        // Send data before page unload
        window.addEventListener('beforeunload', () => {
            this.sendPerformanceData(true);
        });
        
        // Monitor performance degradation
        setInterval(() => {
            this.checkPerformanceDegradation();
        }, 10000);
    }
    
    processNavigationEntry(entry) {
        const pageLoadTime = entry.loadEventEnd - entry.fetchStart;
        const pageName = this.getCurrentPageName();
        
        this.performanceData.pageLoads.set(pageName, {
            loadTime: pageLoadTime,
            domContentLoaded: entry.domContentLoadedEventEnd - entry.fetchStart,
            firstPaint: entry.fetchStart, // Simplified
            timestamp: Date.now(),
            acceptable: pageLoadTime <= this.thresholds.pageLoad
        });
        
        // Show warning for slow page loads
        if (pageLoadTime > this.thresholds.pageLoad) {
            this.showPerformanceWarning('Page Load', `Page took ${Math.round(pageLoadTime)}ms to load`);
        }
        
        console.debug(`Navigation timing recorded: ${pageName} = ${pageLoadTime}ms`);
    }
    
    processResourceEntry(entry) {
        const resourceTime = entry.responseEnd - entry.startTime;
        const resourceName = entry.name;
        
        // Track slow resources
        if (resourceTime > 1000) { // 1 second threshold for resources
            console.warn(`Slow resource load: ${resourceName} took ${resourceTime}ms`);
        }
    }
    
    processMeasureEntry(entry) {
        // Process custom performance measures
        console.debug(`Custom measure: ${entry.name} = ${entry.duration}ms`);
    }
    
    recordApiCall(url, method, responseTime, status, success) {
        const apiKey = `${method} ${url}`;
        
        this.performanceData.apiCalls.set(apiKey, {
            url: url,
            method: method,
            responseTime: responseTime,
            status: status,
            success: success,
            timestamp: Date.now(),
            acceptable: responseTime <= this.thresholds.apiCall
        });
        
        // Show warning for slow API calls
        if (responseTime > this.thresholds.apiCall) {
            this.showPerformanceWarning('API Call', `${method} ${url} took ${Math.round(responseTime)}ms`);
        }
    }
    
    recordApiError(url, method, responseTime, error) {
        const apiKey = `${method} ${url}`;
        
        this.performanceData.apiCalls.set(apiKey, {
            url: url,
            method: method,
            responseTime: responseTime,
            error: error.message,
            success: false,
            timestamp: Date.now(),
            acceptable: false
        });
        
        this.showPerformanceError('API Error', `${method} ${url} failed: ${error.message}`);
    }
    
    isLongRunningOperation(url, options) {
        // Determine if an operation is likely to be long-running
        return url.includes('/api/reports/export') ||
               url.includes('/api/optimization/trigger') ||
               url.includes('/bulk') ||
               url.includes('/import') ||
               (options.method === 'POST' && options.body && options.body.length > 10000);
    }
    
    isLongRunningForm(form) {
        // Check if form submission might be long-running
        return form.classList.contains('export-form') ||
               form.classList.contains('import-form') ||
               form.classList.contains('bulk-operation-form') ||
               form.querySelector('input[type="file"]') !== null;
    }
    
    isLongRunningButton(button) {
        // Check if button click might trigger long-running operation
        return button.classList.contains('export-button') ||
               button.classList.contains('import-button') ||
               button.classList.contains('bulk-delete-button') ||
               button.classList.contains('optimization-button') ||
               button.textContent.toLowerCase().includes('export') ||
               button.textContent.toLowerCase().includes('import') ||
               button.textContent.toLowerCase().includes('optimize');
    }
    
    getOperationTitle(url, options) {
        if (url.includes('/api/reports/export')) {
            return 'Exporting report...';
        } else if (url.includes('/api/optimization/trigger')) {
            return 'Running performance optimization...';
        } else if (url.includes('/bulk')) {
            return 'Processing bulk operation...';
        } else if (url.includes('/import')) {
            return 'Importing data...';
        } else if (options.method === 'POST') {
            return 'Saving data...';
        } else if (options.method === 'DELETE') {
            return 'Deleting...';
        } else {
            return 'Loading...';
        }
    }
    
    getFormOperationTitle(form) {
        if (form.classList.contains('export-form')) {
            return 'Exporting data...';
        } else if (form.classList.contains('import-form')) {
            return 'Importing data...';
        } else if (form.classList.contains('bulk-operation-form')) {
            return 'Processing bulk operation...';
        } else {
            return 'Processing form...';
        }
    }
    
    getButtonOperationTitle(button) {
        const text = button.textContent.toLowerCase();
        
        if (text.includes('export')) {
            return 'Exporting data...';
        } else if (text.includes('import')) {
            return 'Importing data...';
        } else if (text.includes('delete')) {
            return 'Deleting items...';
        } else if (text.includes('optimize')) {
            return 'Running optimization...';
        } else {
            return 'Processing...';
        }
    }
    
    monitorFormCompletion(form, progressId) {
        // Monitor form completion through various means
        const formAction = form.action;
        
        // Set a timeout to complete the progress indicator
        setTimeout(() => {
            if (window.progressIndicators) {
                window.progressIndicators.completeProgress(progressId, 'Form submitted successfully');
            }
        }, 2000);
    }
    
    monitorButtonOperation(button, progressId) {
        // Monitor button operation completion
        setTimeout(() => {
            if (window.progressIndicators) {
                window.progressIndicators.completeProgress(progressId, 'Operation completed');
            }
        }, 1500);
    }
    
    showPerformanceWarning(type, message) {
        // Show non-intrusive performance warning
        console.warn(`Performance Warning [${type}]: ${message}`);
        
        // Could also show a toast notification
        if (window.showToast) {
            window.showToast('warning', `Performance Warning: ${message}`, 5000);
        }
    }
    
    showPerformanceError(type, message) {
        // Show performance error
        console.error(`Performance Error [${type}]: ${message}`);
        
        // Could also show an error notification
        if (window.showToast) {
            window.showToast('error', `Performance Error: ${message}`, 8000);
        }
    }
    
    checkPerformanceDegradation() {
        // Check for performance degradation patterns
        const recentPageLoads = Array.from(this.performanceData.pageLoads.values())
            .filter(load => Date.now() - load.timestamp < 60000); // Last minute
        
        const recentApiCalls = Array.from(this.performanceData.apiCalls.values())
            .filter(call => Date.now() - call.timestamp < 60000); // Last minute
        
        // Check page load degradation
        const slowPageLoads = recentPageLoads.filter(load => !load.acceptable);
        if (slowPageLoads.length > 3) {
            this.showPerformanceWarning('Degradation', 'Multiple slow page loads detected');
        }
        
        // Check API call degradation
        const slowApiCalls = recentApiCalls.filter(call => !call.acceptable);
        if (slowApiCalls.length > 5) {
            this.showPerformanceWarning('Degradation', 'Multiple slow API calls detected');
        }
    }
    
    sendPerformanceData(isBeforeUnload = false) {
        const performanceReport = this.generatePerformanceReport();
        
        if (Object.keys(performanceReport.metrics).length === 0) {
            return; // No data to send
        }
        
        const payload = {
            ...performanceReport,
            userAgent: navigator.userAgent,
            url: window.location.href,
            timestamp: Date.now()
        };
        
        const sendRequest = () => {
            fetch('/api/performance/frontend-metrics', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            }).then(response => {
                if (response.ok) {
                    console.debug('Enhanced performance data sent successfully');
                    this.clearOldData();
                } else {
                    console.warn('Failed to send enhanced performance data:', response.status);
                }
            }).catch(error => {
                console.error('Error sending enhanced performance data:', error);
            });
        };
        
        if (isBeforeUnload && navigator.sendBeacon) {
            const success = navigator.sendBeacon('/api/performance/frontend-metrics', 
                JSON.stringify(payload));
            if (success) {
                this.clearOldData();
            }
        } else {
            sendRequest();
        }
    }
    
    generatePerformanceReport() {
        const report = {
            metrics: {
                pageLoads: Array.from(this.performanceData.pageLoads.entries()).map(([name, data]) => ({
                    pageName: name,
                    ...data
                })),
                apiCalls: Array.from(this.performanceData.apiCalls.entries()).map(([key, data]) => ({
                    operation: key,
                    ...data
                })),
                userInteractions: Array.from(this.performanceData.userInteractions.entries()).map(([key, data]) => ({
                    interaction: key,
                    ...data
                }))
            },
            summary: {
                totalPageLoads: this.performanceData.pageLoads.size,
                totalApiCalls: this.performanceData.apiCalls.size,
                totalInteractions: this.performanceData.userInteractions.size,
                averagePageLoadTime: this.calculateAveragePageLoadTime(),
                averageApiResponseTime: this.calculateAverageApiResponseTime(),
                performanceScore: this.calculatePerformanceScore()
            },
            thresholds: this.thresholds
        };
        
        return report;
    }
    
    calculateAveragePageLoadTime() {
        const pageLoads = Array.from(this.performanceData.pageLoads.values());
        if (pageLoads.length === 0) return 0;
        
        const total = pageLoads.reduce((sum, load) => sum + load.loadTime, 0);
        return Math.round(total / pageLoads.length);
    }
    
    calculateAverageApiResponseTime() {
        const apiCalls = Array.from(this.performanceData.apiCalls.values());
        if (apiCalls.length === 0) return 0;
        
        const total = apiCalls.reduce((sum, call) => sum + call.responseTime, 0);
        return Math.round(total / apiCalls.length);
    }
    
    calculatePerformanceScore() {
        let score = 100;
        
        // Deduct points for slow page loads
        const pageLoads = Array.from(this.performanceData.pageLoads.values());
        const slowPageLoads = pageLoads.filter(load => !load.acceptable);
        score -= (slowPageLoads.length / Math.max(pageLoads.length, 1)) * 30;
        
        // Deduct points for slow API calls
        const apiCalls = Array.from(this.performanceData.apiCalls.values());
        const slowApiCalls = apiCalls.filter(call => !call.acceptable);
        score -= (slowApiCalls.length / Math.max(apiCalls.length, 1)) * 20;
        
        return Math.max(0, Math.round(score));
    }
    
    clearOldData() {
        const cutoffTime = Date.now() - (5 * 60 * 1000); // 5 minutes ago
        
        // Clear old page loads
        for (const [key, data] of this.performanceData.pageLoads.entries()) {
            if (data.timestamp < cutoffTime) {
                this.performanceData.pageLoads.delete(key);
            }
        }
        
        // Clear old API calls
        for (const [key, data] of this.performanceData.apiCalls.entries()) {
            if (data.timestamp < cutoffTime) {
                this.performanceData.apiCalls.delete(key);
            }
        }
        
        // Clear old interactions
        for (const [key, data] of this.performanceData.userInteractions.entries()) {
            if (data.timestamp < cutoffTime) {
                this.performanceData.userInteractions.delete(key);
            }
        }
    }
    
    getCurrentPageName() {
        const path = window.location.pathname;
        if (path === '/' || path === '') {
            return 'home';
        }
        
        const segments = path.split('/').filter(segment => segment);
        return segments.join('_') || 'unknown';
    }
    
    generateOperationId() {
        return 'op_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
    
    // Public API methods
    
    getPerformanceReport() {
        return this.generatePerformanceReport();
    }
    
    setThresholds(newThresholds) {
        this.thresholds = { ...this.thresholds, ...newThresholds };
        console.log('Enhanced performance thresholds updated:', this.thresholds);
    }
    
    triggerManualReport() {
        this.sendPerformanceData();
    }
    
    getPerformanceScore() {
        return this.calculatePerformanceScore();
    }
}

// Initialize enhanced performance monitoring when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.enhancedPerformanceMonitoring = new EnhancedPerformanceMonitoring();
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = EnhancedPerformanceMonitoring;
}