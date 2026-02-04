/**
 * Frontend Performance Optimization
 * Implements client-side optimizations for better user experience
 * Validates: Requirements 16.1, 16.2, 16.3, 16.4, 16.5
 */

class FrontendOptimization {
    constructor() {
        this.optimizations = {
            imageOptimization: true,
            lazyLoading: true,
            resourcePreloading: true,
            caching: true,
            compression: true
        };
        
        this.performanceThresholds = {
            pageLoad: 2000,      // 2 seconds
            interaction: 100,    // 100ms
            imageLoad: 1000,     // 1 second
            apiCall: 3000        // 3 seconds
        };
        
        this.init();
    }
    
    init() {
        this.setupImageOptimization();
        this.setupLazyLoading();
        this.setupResourcePreloading();
        this.setupClientSideCaching();
        this.setupProgressIndicators();
        this.setupNetworkOptimization();
        
        console.log('Frontend optimization initialized');
    }
    
    setupImageOptimization() {
        if (!this.optimizations.imageOptimization) return;
        
        // Optimize images on load
        document.addEventListener('DOMContentLoaded', () => {
            this.optimizeImages();
        });
        
        // Monitor new images added dynamically
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const images = node.querySelectorAll ? node.querySelectorAll('img') : [];
                        images.forEach(img => this.optimizeImage(img));
                        
                        if (node.tagName === 'IMG') {
                            this.optimizeImage(node);
                        }
                    }
                });
            });
        });
        
        observer.observe(document.body, { childList: true, subtree: true });
    }
    
    setupLazyLoading() {
        if (!this.optimizations.lazyLoading) return;
        
        // Implement intersection observer for lazy loading
        if ('IntersectionObserver' in window) {
            const lazyImageObserver = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        this.loadImageLazily(img);
                        lazyImageObserver.unobserve(img);
                    }
                });
            }, {
                rootMargin: '50px 0px' // Start loading 50px before image comes into view
            });
            
            // Observe all images with data-src attribute
            document.addEventListener('DOMContentLoaded', () => {
                const lazyImages = document.querySelectorAll('img[data-src]');
                lazyImages.forEach(img => lazyImageObserver.observe(img));
            });
        }
        
        // Lazy load table content
        this.setupLazyTableLoading();
    }
    
    setupResourcePreloading() {
        if (!this.optimizations.resourcePreloading) return;
        
        // Preload critical resources
        this.preloadCriticalResources();
        
        // Preload next page resources on hover
        this.setupHoverPreloading();
        
        // Preload search results
        this.setupSearchPreloading();
    }
    
    setupClientSideCaching() {
        if (!this.optimizations.caching) return;
        
        // Implement client-side caching for API responses
        this.apiCache = new Map();
        this.cacheExpiry = new Map();
        
        // Override fetch to add caching
        this.setupFetchCaching();
        
        // Cache DOM elements for reuse
        this.setupDOMCaching();
    }
    
    setupProgressIndicators() {
        // Add progress indicators for long-running operations
        this.setupLoadingIndicators();
        this.setupProgressBars();
        this.setupSkeletonLoaders();
    }
    
    setupNetworkOptimization() {
        // Implement request batching
        this.setupRequestBatching();
        
        // Implement request debouncing
        this.setupRequestDebouncing();
        
        // Monitor network conditions
        this.setupNetworkMonitoring();
    }
    
    // Image optimization methods
    
    optimizeImages() {
        const images = document.querySelectorAll('img');
        images.forEach(img => this.optimizeImage(img));
    }
    
    optimizeImage(img) {
        const startTime = performance.now();
        
        // Add loading attribute for native lazy loading
        if (!img.hasAttribute('loading')) {
            img.setAttribute('loading', 'lazy');
        }
        
        // Optimize image format based on browser support
        this.optimizeImageFormat(img);
        
        // Add error handling
        img.addEventListener('error', () => {
            this.handleImageError(img);
        });
        
        // Monitor load time
        img.addEventListener('load', () => {
            const loadTime = performance.now() - startTime;
            if (loadTime > this.performanceThresholds.imageLoad) {
                console.warn(`Slow image load: ${img.src} took ${loadTime}ms`);
            }
        });
    }
    
    optimizeImageFormat(img) {
        if (!img.src) return;
        
        // Check for WebP support and optimize accordingly
        if (this.supportsWebP() && !img.src.includes('.webp')) {
            const webpSrc = img.src.replace(/\.(jpg|jpeg|png)$/i, '.webp');
            
            // Test if WebP version exists
            const testImg = new Image();
            testImg.onload = () => {
                img.src = webpSrc;
            };
            testImg.onerror = () => {
                // Keep original format
            };
            testImg.src = webpSrc;
        }
    }
    
    handleImageError(img) {
        // Provide fallback image or hide broken image
        img.style.display = 'none';
        
        // Try to load a fallback image
        const fallbackSrc = img.getAttribute('data-fallback');
        if (fallbackSrc && img.src !== fallbackSrc) {
            img.src = fallbackSrc;
            img.style.display = '';
        }
    }
    
    loadImageLazily(img) {
        const src = img.getAttribute('data-src');
        if (src) {
            img.src = src;
            img.removeAttribute('data-src');
            img.classList.add('loaded');
        }
    }
    
    // Lazy loading methods
    
    setupLazyTableLoading() {
        // Implement virtual scrolling for large tables
        const tables = document.querySelectorAll('.large-table');
        tables.forEach(table => {
            this.implementVirtualScrolling(table);
        });
    }
    
    implementVirtualScrolling(table) {
        const tbody = table.querySelector('tbody');
        if (!tbody) return;
        
        const rows = Array.from(tbody.querySelectorAll('tr'));
        const rowHeight = 40; // Estimated row height
        const visibleRows = Math.ceil(window.innerHeight / rowHeight) + 5; // Buffer
        
        let startIndex = 0;
        let endIndex = Math.min(visibleRows, rows.length);
        
        // Create virtual container
        const virtualContainer = document.createElement('div');
        virtualContainer.style.height = (rows.length * rowHeight) + 'px';
        virtualContainer.style.position = 'relative';
        
        // Replace tbody content
        tbody.innerHTML = '';
        tbody.appendChild(virtualContainer);
        
        const renderVisibleRows = () => {
            virtualContainer.innerHTML = '';
            
            for (let i = startIndex; i < endIndex; i++) {
                if (rows[i]) {
                    const row = rows[i].cloneNode(true);
                    row.style.position = 'absolute';
                    row.style.top = (i * rowHeight) + 'px';
                    row.style.width = '100%';
                    virtualContainer.appendChild(row);
                }
            }
        };
        
        // Handle scroll events
        table.addEventListener('scroll', () => {
            const scrollTop = table.scrollTop;
            const newStartIndex = Math.floor(scrollTop / rowHeight);
            const newEndIndex = Math.min(newStartIndex + visibleRows, rows.length);
            
            if (newStartIndex !== startIndex || newEndIndex !== endIndex) {
                startIndex = newStartIndex;
                endIndex = newEndIndex;
                renderVisibleRows();
            }
        });
        
        // Initial render
        renderVisibleRows();
    }
    
    // Resource preloading methods
    
    preloadCriticalResources() {
        const criticalResources = [
            '/css/main.css',
            '/js/main.js',
            '/js/enhanced-table.js',
            '/js/search.js'
        ];
        
        criticalResources.forEach(resource => {
            const link = document.createElement('link');
            link.rel = 'preload';
            link.href = resource;
            link.as = resource.endsWith('.css') ? 'style' : 'script';
            document.head.appendChild(link);
        });
    }
    
    setupHoverPreloading() {
        // Preload page resources on link hover
        document.addEventListener('mouseover', (event) => {
            if (event.target.tagName === 'A' && event.target.href) {
                this.preloadPage(event.target.href);
            }
        });
    }
    
    preloadPage(url) {
        // Only preload internal pages
        if (!url.startsWith(window.location.origin)) return;
        
        // Check if already preloaded
        if (this.preloadedPages && this.preloadedPages.has(url)) return;
        
        // Create prefetch link
        const link = document.createElement('link');
        link.rel = 'prefetch';
        link.href = url;
        document.head.appendChild(link);
        
        // Track preloaded pages
        if (!this.preloadedPages) this.preloadedPages = new Set();
        this.preloadedPages.add(url);
        
        // Remove link after 30 seconds to avoid memory leaks
        setTimeout(() => {
            if (link.parentNode) {
                link.parentNode.removeChild(link);
            }
        }, 30000);
    }
    
    setupSearchPreloading() {
        // Preload search results based on user input
        const searchInputs = document.querySelectorAll('input[type="search"], .search-input');
        
        searchInputs.forEach(input => {
            let preloadTimeout;
            
            input.addEventListener('input', (event) => {
                clearTimeout(preloadTimeout);
                
                const query = event.target.value;
                if (query.length >= 3) {
                    preloadTimeout = setTimeout(() => {
                        this.preloadSearchResults(query);
                    }, 500);
                }
            });
        });
    }
    
    preloadSearchResults(query) {
        // Preload search results in background
        const searchUrl = `/api/search?q=${encodeURIComponent(query)}`;
        
        fetch(searchUrl, {
            method: 'GET',
            headers: { 'X-Preload': 'true' }
        }).then(response => {
            if (response.ok) {
                return response.json();
            }
        }).then(data => {
            // Cache the results
            this.cacheSearchResults(query, data);
        }).catch(error => {
            console.debug('Search preload failed:', error);
        });
    }
    
    // Caching methods
    
    setupFetchCaching() {
        const originalFetch = window.fetch;
        const self = this;
        
        window.fetch = function(url, options = {}) {
            // Only cache GET requests
            if (options.method && options.method !== 'GET') {
                return originalFetch.apply(this, arguments);
            }
            
            // Check cache first
            const cacheKey = url + JSON.stringify(options);
            const cachedResponse = self.getCachedResponse(cacheKey);
            
            if (cachedResponse) {
                return Promise.resolve(cachedResponse.clone());
            }
            
            // Make request and cache response
            return originalFetch.apply(this, arguments).then(response => {
                if (response.ok && response.status === 200) {
                    self.cacheResponse(cacheKey, response.clone());
                }
                return response;
            });
        };
    }
    
    getCachedResponse(cacheKey) {
        const cached = this.apiCache.get(cacheKey);
        const expiry = this.cacheExpiry.get(cacheKey);
        
        if (cached && expiry && Date.now() < expiry) {
            return cached;
        }
        
        // Remove expired cache
        this.apiCache.delete(cacheKey);
        this.cacheExpiry.delete(cacheKey);
        
        return null;
    }
    
    cacheResponse(cacheKey, response) {
        // Cache for 5 minutes
        const expiryTime = Date.now() + (5 * 60 * 1000);
        
        this.apiCache.set(cacheKey, response);
        this.cacheExpiry.set(cacheKey, expiryTime);
        
        // Limit cache size
        if (this.apiCache.size > 100) {
            const firstKey = this.apiCache.keys().next().value;
            this.apiCache.delete(firstKey);
            this.cacheExpiry.delete(firstKey);
        }
    }
    
    cacheSearchResults(query, results) {
        const cacheKey = 'search_' + query;
        const expiryTime = Date.now() + (2 * 60 * 1000); // 2 minutes for search results
        
        this.apiCache.set(cacheKey, results);
        this.cacheExpiry.set(cacheKey, expiryTime);
    }
    
    setupDOMCaching() {
        // Cache frequently accessed DOM elements
        this.domCache = new Map();
        
        // Override querySelector to add caching
        const originalQuerySelector = document.querySelector;
        const self = this;
        
        document.querySelector = function(selector) {
            const cached = self.domCache.get(selector);
            if (cached && document.contains(cached)) {
                return cached;
            }
            
            const element = originalQuerySelector.call(this, selector);
            if (element) {
                self.domCache.set(selector, element);
            }
            
            return element;
        };
    }
    
    // Progress indicator methods
    
    setupLoadingIndicators() {
        // Add loading indicators to forms
        const forms = document.querySelectorAll('form');
        forms.forEach(form => {
            form.addEventListener('submit', () => {
                this.showLoadingIndicator(form);
            });
        });
        
        // Add loading indicators to AJAX requests
        this.setupAjaxLoadingIndicators();
    }
    
    setupAjaxLoadingIndicators() {
        // Monitor fetch requests and show loading indicators
        const originalFetch = window.fetch;
        const self = this;
        
        window.fetch = function(url, options = {}) {
            // Don't show indicators for very fast requests or progress polling
            if (url.includes('/api/progress/') || options.skipProgressIndicator) {
                return originalFetch.apply(this, arguments);
            }
            
            const operationId = `fetch_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
            const title = self.getFetchTitle(url, options);
            
            let indicatorId = null;
            
            // Show progress indicator for potentially long operations
            if (self.isLongRunningOperation(url, options)) {
                indicatorId = window.progressIndicators ? 
                    window.progressIndicators.showProgress(operationId, title, {
                        showCancelButton: false,
                        autoClose: true,
                        autoCloseDelay: 2000
                    }) : null;
            } else {
                indicatorId = self.showGlobalLoadingIndicator();
            }
            
            const startTime = performance.now();
            
            return originalFetch.apply(this, arguments)
                .then(response => {
                    const responseTime = performance.now() - startTime;
                    
                    if (window.progressIndicators && self.isLongRunningOperation(url, options)) {
                        if (response.ok) {
                            window.progressIndicators.completeProgress(indicatorId, 'Request completed successfully');
                        } else {
                            window.progressIndicators.failProgress(indicatorId, `Request failed: ${response.status}`);
                        }
                    } else {
                        self.hideGlobalLoadingIndicator(indicatorId);
                    }
                    
                    return response;
                })
                .catch(error => {
                    if (window.progressIndicators && self.isLongRunningOperation(url, options)) {
                        window.progressIndicators.failProgress(indicatorId, `Request failed: ${error.message}`);
                    } else {
                        self.hideGlobalLoadingIndicator(indicatorId);
                    }
                    throw error;
                });
        };
    }
    
    showLoadingIndicator(element) {
        const loader = document.createElement('div');
        loader.className = 'loading-indicator';
        loader.innerHTML = '<div class="spinner"></div><span>Loading...</span>';
        
        element.style.position = 'relative';
        element.appendChild(loader);
        
        return loader;
    }
    
    showGlobalLoadingIndicator() {
        const loadingId = 'loading_' + Date.now();
        const loader = document.createElement('div');
        loader.id = loadingId;
        loader.className = 'global-loading-indicator';
        loader.innerHTML = '<div class="spinner"></div>';
        
        document.body.appendChild(loader);
        
        return loadingId;
    }
    
    hideGlobalLoadingIndicator(loadingId) {
        const loader = document.getElementById(loadingId);
        if (loader) {
            loader.remove();
        }
    }
    
    setupProgressBars() {
        // Add progress bars for file uploads and long operations
        const fileInputs = document.querySelectorAll('input[type="file"]');
        fileInputs.forEach(input => {
            input.addEventListener('change', (event) => {
                if (event.target.files.length > 0) {
                    this.showUploadProgress(event.target);
                }
            });
        });
    }
    
    showUploadProgress(fileInput) {
        const progressBar = document.createElement('div');
        progressBar.className = 'upload-progress';
        progressBar.innerHTML = `
            <div class="progress-bar">
                <div class="progress-fill" style="width: 0%"></div>
            </div>
            <span class="progress-text">0%</span>
        `;
        
        fileInput.parentNode.insertBefore(progressBar, fileInput.nextSibling);
        
        // Simulate progress (in real implementation, this would track actual upload)
        let progress = 0;
        const interval = setInterval(() => {
            progress += Math.random() * 10;
            if (progress >= 100) {
                progress = 100;
                clearInterval(interval);
                setTimeout(() => progressBar.remove(), 1000);
            }
            
            const fill = progressBar.querySelector('.progress-fill');
            const text = progressBar.querySelector('.progress-text');
            
            fill.style.width = progress + '%';
            text.textContent = Math.round(progress) + '%';
        }, 200);
    }
    
    setupSkeletonLoaders() {
        // Add skeleton loaders for content that's loading
        const loadingContainers = document.querySelectorAll('.loading-container');
        loadingContainers.forEach(container => {
            this.showSkeletonLoader(container);
        });
    }
    
    showSkeletonLoader(container) {
        const skeleton = document.createElement('div');
        skeleton.className = 'skeleton-loader';
        skeleton.innerHTML = `
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
        `;
        
        container.appendChild(skeleton);
        
        return skeleton;
    }
    
    // Network optimization methods
    
    setupRequestBatching() {
        this.requestQueue = [];
        this.batchTimeout = null;
        
        // Override fetch to batch similar requests
        const originalFetch = window.fetch;
        const self = this;
        
        window.fetch = function(url, options = {}) {
            // Only batch GET requests to API endpoints
            if (url.startsWith('/api/') && (!options.method || options.method === 'GET')) {
                return self.batchRequest(url, options);
            }
            
            return originalFetch.apply(this, arguments);
        };
    }
    
    batchRequest(url, options) {
        return new Promise((resolve, reject) => {
            this.requestQueue.push({ url, options, resolve, reject });
            
            // Clear existing timeout
            if (this.batchTimeout) {
                clearTimeout(this.batchTimeout);
            }
            
            // Set new timeout to process batch
            this.batchTimeout = setTimeout(() => {
                this.processBatchedRequests();
            }, 50); // 50ms batch window
        });
    }
    
    processBatchedRequests() {
        if (this.requestQueue.length === 0) return;
        
        // Group requests by base URL
        const groupedRequests = new Map();
        
        this.requestQueue.forEach(request => {
            const baseUrl = request.url.split('?')[0];
            if (!groupedRequests.has(baseUrl)) {
                groupedRequests.set(baseUrl, []);
            }
            groupedRequests.get(baseUrl).push(request);
        });
        
        // Process each group
        groupedRequests.forEach((requests, baseUrl) => {
            if (requests.length === 1) {
                // Single request, process normally
                const request = requests[0];
                fetch(request.url, request.options)
                    .then(request.resolve)
                    .catch(request.reject);
            } else {
                // Multiple requests, try to batch them
                this.executeBatchedRequests(baseUrl, requests);
            }
        });
        
        // Clear queue
        this.requestQueue = [];
    }
    
    executeBatchedRequests(baseUrl, requests) {
        // For now, execute requests individually
        // In a real implementation, this would combine requests into a single batch request
        requests.forEach(request => {
            fetch(request.url, request.options)
                .then(request.resolve)
                .catch(request.reject);
        });
    }
    
    setupRequestDebouncing() {
        this.debouncedRequests = new Map();
        
        // Add debouncing for search requests
        const searchInputs = document.querySelectorAll('input[type="search"], .search-input');
        searchInputs.forEach(input => {
            input.addEventListener('input', (event) => {
                this.debounceSearchRequest(event.target);
            });
        });
    }
    
    debounceSearchRequest(input) {
        const query = input.value;
        const requestKey = 'search_' + input.name || input.id || 'default';
        
        // Clear existing timeout
        if (this.debouncedRequests.has(requestKey)) {
            clearTimeout(this.debouncedRequests.get(requestKey));
        }
        
        // Set new timeout
        const timeout = setTimeout(() => {
            if (query.length >= 2) {
                this.executeSearchRequest(query, input);
            }
            this.debouncedRequests.delete(requestKey);
        }, 300);
        
        this.debouncedRequests.set(requestKey, timeout);
    }
    
    executeSearchRequest(query, input) {
        const searchUrl = `/api/search?q=${encodeURIComponent(query)}`;
        
        fetch(searchUrl)
            .then(response => response.json())
            .then(data => {
                this.displaySearchResults(data, input);
            })
            .catch(error => {
                console.error('Search request failed:', error);
            });
    }
    
    displaySearchResults(data, input) {
        // Find or create results container
        let resultsContainer = input.parentNode.querySelector('.search-results');
        if (!resultsContainer) {
            resultsContainer = document.createElement('div');
            resultsContainer.className = 'search-results';
            input.parentNode.appendChild(resultsContainer);
        }
        
        // Display results
        if (data && data.length > 0) {
            resultsContainer.innerHTML = data.map(item => 
                `<div class="search-result-item">${item.title || item.name}</div>`
            ).join('');
            resultsContainer.style.display = 'block';
        } else {
            resultsContainer.style.display = 'none';
        }
    }
    
    setupNetworkMonitoring() {
        // Monitor network conditions and adjust behavior
        if ('connection' in navigator) {
            const connection = navigator.connection;
            
            const updateNetworkOptimizations = () => {
                if (connection.effectiveType === 'slow-2g' || connection.effectiveType === '2g') {
                    // Disable heavy optimizations on slow connections
                    this.optimizations.imageOptimization = false;
                    this.optimizations.resourcePreloading = false;
                } else {
                    // Enable all optimizations on fast connections
                    this.optimizations.imageOptimization = true;
                    this.optimizations.resourcePreloading = true;
                }
            };
            
            connection.addEventListener('change', updateNetworkOptimizations);
            updateNetworkOptimizations(); // Initial check
        }
    }
    
    // Utility methods
    
    supportsWebP() {
        const canvas = document.createElement('canvas');
        canvas.width = 1;
        canvas.height = 1;
        return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
    }
    
    // Public API methods
    
    enableOptimization(optimizationType) {
        if (this.optimizations.hasOwnProperty(optimizationType)) {
            this.optimizations[optimizationType] = true;
            console.log(`Enabled ${optimizationType} optimization`);
        }
    }
    
    disableOptimization(optimizationType) {
        if (this.optimizations.hasOwnProperty(optimizationType)) {
            this.optimizations[optimizationType] = false;
            console.log(`Disabled ${optimizationType} optimization`);
        }
    }
    
    getOptimizationStatus() {
        return { ...this.optimizations };
    }
    
    clearCache() {
        this.apiCache.clear();
        this.cacheExpiry.clear();
        this.domCache.clear();
        console.log('All caches cleared');
    }
    
    getPerformanceReport() {
        return {
            optimizations: this.optimizations,
            thresholds: this.performanceThresholds,
            cacheSize: this.apiCache.size,
            preloadedPages: this.preloadedPages ? this.preloadedPages.size : 0
        };
    }
    
    // Helper methods for progress indicators integration
    
    getFetchTitle(url, options) {
        if (url.includes('/api/reports/export')) {
            return 'Exporting report...';
        } else if (url.includes('/api/search')) {
            return 'Searching...';
        } else if (url.includes('/bulk')) {
            return 'Processing bulk operation...';
        } else if (options.method === 'POST' || options.method === 'PUT') {
            return 'Saving data...';
        } else if (options.method === 'DELETE') {
            return 'Deleting...';
        } else {
            return 'Loading...';
        }
    }
    
    isLongRunningOperation(url, options) {
        // Determine if this is likely to be a long-running operation
        return url.includes('/api/reports/export') ||
               url.includes('/bulk') ||
               url.includes('/api/performance/analyze') ||
               (options.method === 'POST' && url.includes('/import')) ||
               (options.body && options.body.length > 10000); // Large payloads
    }
}

// Initialize frontend optimization when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.frontendOptimization = new FrontendOptimization();
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FrontendOptimization;
}