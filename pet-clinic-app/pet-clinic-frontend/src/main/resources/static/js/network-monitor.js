/**
 * Frontend Network Connectivity Monitor
 * Provides network connectivity detection and retry mechanisms for frontend
 * Requirements: 15.1, 15.2, 15.3
 */

(function(window) {
    'use strict';

    /**
     * Network connectivity monitor
     */
    class NetworkMonitor {
        constructor(options = {}) {
            this.options = {
                enableAutoRetry: options.enableAutoRetry !== false,
                enableNotifications: options.enableNotifications !== false,
                checkInterval: options.checkInterval || 30000, // 30 seconds
                timeoutDuration: options.timeoutDuration || 5000, // 5 seconds
                maxRetries: options.maxRetries || 3,
                retryDelay: options.retryDelay || 1000,
                ...options
            };

            this.isOnline = navigator.onLine;
            this.lastOnlineTime = new Date();
            this.lastOfflineTime = null;
            this.connectivityHistory = [];
            this.pendingRequests = new Map();
            this.retryQueue = [];
            this.checkTimer = null;

            this.init();
        }

        /**
         * Initialize network monitoring
         */
        init() {
            this.setupEventListeners();
            this.startConnectivityChecks();
            this.setupRequestInterception();
        }

        /**
         * Setup event listeners for network changes
         */
        setupEventListeners() {
            window.addEventListener('online', () => {
                this.handleOnline();
            });

            window.addEventListener('offline', () => {
                this.handleOffline();
            });

            // Listen for page visibility changes
            document.addEventListener('visibilitychange', () => {
                if (!document.hidden) {
                    this.checkConnectivity();
                }
            });
        }

        /**
         * Start periodic connectivity checks
         */
        startConnectivityChecks() {
            this.checkTimer = setInterval(() => {
                this.checkConnectivity();
            }, this.options.checkInterval);

            // Initial check
            this.checkConnectivity();
        }

        /**
         * Setup request interception for automatic retry
         */
        setupRequestInterception() {
            const originalFetch = window.fetch;
            
            window.fetch = async (...args) => {
                const requestId = this.generateRequestId();
                
                try {
                    // Store pending request
                    this.pendingRequests.set(requestId, {
                        args: args,
                        timestamp: new Date(),
                        retries: 0
                    });

                    const response = await originalFetch(...args);
                    
                    // Remove from pending requests
                    this.pendingRequests.delete(requestId);
                    
                    return response;
                } catch (error) {
                    // Handle network errors
                    if (this.isNetworkError(error)) {
                        return await this.handleNetworkError(requestId, error, args);
                    } else {
                        this.pendingRequests.delete(requestId);
                        throw error;
                    }
                }
            };
        }

        /**
         * Check network connectivity
         */
        async checkConnectivity() {
            const wasOnline = this.isOnline;
            
            try {
                // Test connectivity with multiple methods
                const isConnected = await this.performConnectivityTest();
                this.updateConnectivityStatus(isConnected);
                
                // Record connectivity change
                if (wasOnline !== isConnected) {
                    this.recordConnectivityChange(isConnected);
                }
                
            } catch (error) {
                console.warn('Connectivity check failed:', error);
                this.updateConnectivityStatus(false);
            }
        }

        /**
         * Perform comprehensive connectivity test
         */
        async performConnectivityTest() {
            // Test 1: Navigator online status
            if (!navigator.onLine) {
                return false;
            }

            // Test 2: Fetch a small resource from our server
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), this.options.timeoutDuration);
                
                const response = await fetch('/api/health/ping', {
                    method: 'HEAD',
                    cache: 'no-cache',
                    signal: controller.signal
                });
                
                clearTimeout(timeoutId);
                return response.ok;
            } catch (error) {
                // Test 3: Try external resource as fallback
                try {
                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), this.options.timeoutDuration);
                    
                    const response = await fetch('https://httpbin.org/status/200', {
                        method: 'HEAD',
                        mode: 'no-cors',
                        cache: 'no-cache',
                        signal: controller.signal
                    });
                    
                    clearTimeout(timeoutId);
                    return true; // If we get here, we have connectivity
                } catch (fallbackError) {
                    return false;
                }
            }
        }

        /**
         * Update connectivity status
         */
        updateConnectivityStatus(isOnline) {
            const wasOnline = this.isOnline;
            this.isOnline = isOnline;

            if (wasOnline && !isOnline) {
                this.lastOfflineTime = new Date();
                this.handleConnectivityLoss();
            } else if (!wasOnline && isOnline) {
                this.lastOnlineTime = new Date();
                this.handleConnectivityRestore();
            }
        }

        /**
         * Handle online event
         */
        handleOnline() {
            console.log('Network: Online event detected');
            this.checkConnectivity();
        }

        /**
         * Handle offline event
         */
        handleOffline() {
            console.log('Network: Offline event detected');
            this.updateConnectivityStatus(false);
        }

        /**
         * Handle connectivity loss
         */
        handleConnectivityLoss() {
            console.warn('Network connectivity lost');
            
            // Notify user
            if (this.options.enableNotifications) {
                this.showConnectivityNotification(false);
            }

            // Trigger offline mode
            window.dispatchEvent(new CustomEvent('networkOffline', {
                detail: { timestamp: this.lastOfflineTime }
            }));

            // Queue pending requests for retry
            this.queuePendingRequests();
        }

        /**
         * Handle connectivity restoration
         */
        handleConnectivityRestore() {
            console.log('Network connectivity restored');
            
            // Notify user
            if (this.options.enableNotifications) {
                this.showConnectivityNotification(true);
            }

            // Trigger online mode
            window.dispatchEvent(new CustomEvent('networkOnline', {
                detail: { 
                    timestamp: this.lastOnlineTime,
                    downtime: this.getDowntime()
                }
            }));

            // Process retry queue
            this.processRetryQueue();
        }

        /**
         * Record connectivity change
         */
        recordConnectivityChange(isOnline) {
            this.connectivityHistory.push({
                timestamp: new Date(),
                status: isOnline ? 'online' : 'offline',
                duration: isOnline ? this.getDowntime() : this.getUptime()
            });

            // Keep only last 100 records
            if (this.connectivityHistory.length > 100) {
                this.connectivityHistory.shift();
            }
        }

        /**
         * Handle network error during fetch
         */
        async handleNetworkError(requestId, error, args) {
            const requestData = this.pendingRequests.get(requestId);
            if (!requestData) {
                throw error;
            }

            // Check if we should retry
            if (this.options.enableAutoRetry && requestData.retries < this.options.maxRetries) {
                return await this.retryRequest(requestId, args);
            } else {
                // Add to retry queue for later
                this.addToRetryQueue(requestId, args);
                this.pendingRequests.delete(requestId);
                throw error;
            }
        }

        /**
         * Retry a failed request
         */
        async retryRequest(requestId, args) {
            const requestData = this.pendingRequests.get(requestId);
            if (!requestData) {
                throw new Error('Request data not found');
            }

            requestData.retries++;
            
            // Calculate delay with exponential backoff
            const delay = this.options.retryDelay * Math.pow(2, requestData.retries - 1);
            
            console.log(`Retrying request (attempt ${requestData.retries}/${this.options.maxRetries}) after ${delay}ms`);
            
            // Wait before retry
            await this.delay(delay);
            
            // Check connectivity before retry
            if (!this.isOnline) {
                await this.waitForConnectivity(10000); // Wait up to 10 seconds
            }

            try {
                const response = await fetch(...args);
                this.pendingRequests.delete(requestId);
                return response;
            } catch (retryError) {
                if (this.isNetworkError(retryError) && requestData.retries < this.options.maxRetries) {
                    return await this.retryRequest(requestId, args);
                } else {
                    this.pendingRequests.delete(requestId);
                    throw retryError;
                }
            }
        }

        /**
         * Add request to retry queue
         */
        addToRetryQueue(requestId, args) {
            this.retryQueue.push({
                id: requestId,
                args: args,
                timestamp: new Date()
            });

            // Limit queue size
            if (this.retryQueue.length > 50) {
                this.retryQueue.shift();
            }
        }

        /**
         * Queue pending requests for retry when connectivity is restored
         */
        queuePendingRequests() {
            for (const [requestId, requestData] of this.pendingRequests.entries()) {
                this.addToRetryQueue(requestId, requestData.args);
            }
            this.pendingRequests.clear();
        }

        /**
         * Process retry queue when connectivity is restored
         */
        async processRetryQueue() {
            if (this.retryQueue.length === 0) return;

            console.log(`Processing ${this.retryQueue.length} queued requests`);

            const queue = [...this.retryQueue];
            this.retryQueue = [];

            for (const queuedRequest of queue) {
                try {
                    // Small delay between requests to avoid overwhelming the server
                    await this.delay(100);
                    
                    // Retry the request
                    await fetch(...queuedRequest.args);
                    console.log('Successfully retried queued request');
                } catch (error) {
                    console.warn('Failed to retry queued request:', error);
                    // Could add back to queue or handle differently
                }
            }
        }

        /**
         * Wait for connectivity to be restored
         */
        async waitForConnectivity(timeout = 30000) {
            const startTime = Date.now();
            
            while (!this.isOnline && (Date.now() - startTime) < timeout) {
                await this.delay(1000);
                await this.checkConnectivity();
            }
            
            return this.isOnline;
        }

        /**
         * Show connectivity notification
         */
        showConnectivityNotification(isOnline) {
            // Remove existing connectivity notifications
            const existingNotifications = document.querySelectorAll('.connectivity-notification');
            existingNotifications.forEach(notification => notification.remove());

            // Create notification
            const notification = document.createElement('div');
            notification.className = `alert alert-${isOnline ? 'success' : 'warning'} connectivity-notification`;
            notification.style.cssText = `
                position: fixed;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                z-index: 10001;
                min-width: 300px;
                text-align: center;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                border-radius: 8px;
            `;

            const icon = isOnline ? 'fas fa-wifi' : 'fas fa-wifi-slash';
            const message = isOnline ? 
                'Connection restored - queued requests are being processed' : 
                'Connection lost - requests will be queued for retry';

            notification.innerHTML = `
                <div class="d-flex align-items-center justify-content-center">
                    <i class="${icon} me-2"></i>
                    <span>${message}</span>
                    <button type="button" class="btn-close ms-3" onclick="this.closest('.connectivity-notification').remove()"></button>
                </div>
            `;

            document.body.appendChild(notification);

            // Auto-hide after delay
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.remove();
                }
            }, isOnline ? 3000 : 10000);
        }

        /**
         * Check if error is a network error
         */
        isNetworkError(error) {
            return error.name === 'TypeError' && 
                   (error.message.includes('fetch') || 
                    error.message.includes('network') ||
                    error.message.includes('Failed to fetch'));
        }

        /**
         * Get current connectivity status
         */
        getConnectivityStatus() {
            return {
                isOnline: this.isOnline,
                lastOnlineTime: this.lastOnlineTime,
                lastOfflineTime: this.lastOfflineTime,
                uptime: this.getUptime(),
                downtime: this.getDowntime(),
                pendingRequests: this.pendingRequests.size,
                queuedRequests: this.retryQueue.length
            };
        }

        /**
         * Get uptime in milliseconds
         */
        getUptime() {
            if (!this.isOnline || !this.lastOnlineTime) return 0;
            return Date.now() - this.lastOnlineTime.getTime();
        }

        /**
         * Get downtime in milliseconds
         */
        getDowntime() {
            if (this.isOnline || !this.lastOfflineTime) return 0;
            return Date.now() - this.lastOfflineTime.getTime();
        }

        /**
         * Get connectivity history
         */
        getConnectivityHistory() {
            return [...this.connectivityHistory];
        }

        /**
         * Get connectivity statistics
         */
        getConnectivityStats() {
            const history = this.connectivityHistory;
            if (history.length === 0) {
                return {
                    totalEvents: 0,
                    onlineEvents: 0,
                    offlineEvents: 0,
                    averageUptime: 0,
                    averageDowntime: 0,
                    uptimePercentage: 100
                };
            }

            const onlineEvents = history.filter(event => event.status === 'online');
            const offlineEvents = history.filter(event => event.status === 'offline');
            
            const totalUptime = onlineEvents.reduce((sum, event) => sum + (event.duration || 0), 0);
            const totalDowntime = offlineEvents.reduce((sum, event) => sum + (event.duration || 0), 0);
            const totalTime = totalUptime + totalDowntime;

            return {
                totalEvents: history.length,
                onlineEvents: onlineEvents.length,
                offlineEvents: offlineEvents.length,
                averageUptime: onlineEvents.length > 0 ? totalUptime / onlineEvents.length : 0,
                averageDowntime: offlineEvents.length > 0 ? totalDowntime / offlineEvents.length : 0,
                uptimePercentage: totalTime > 0 ? (totalUptime / totalTime) * 100 : 100
            };
        }

        /**
         * Test network latency
         */
        async testLatency() {
            const startTime = performance.now();
            
            try {
                await fetch('/api/health/ping', {
                    method: 'HEAD',
                    cache: 'no-cache'
                });
                
                return performance.now() - startTime;
            } catch (error) {
                return -1; // Indicates failure
            }
        }

        /**
         * Generate unique request ID
         */
        generateRequestId() {
            return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        }

        /**
         * Delay utility
         */
        delay(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        /**
         * Cleanup resources
         */
        destroy() {
            if (this.checkTimer) {
                clearInterval(this.checkTimer);
                this.checkTimer = null;
            }

            this.pendingRequests.clear();
            this.retryQueue = [];
            this.connectivityHistory = [];
        }
    }

    // Create global network monitor instance
    window.networkMonitor = new NetworkMonitor();

    // Export for use by other modules
    window.NetworkMonitor = NetworkMonitor;

})(window);