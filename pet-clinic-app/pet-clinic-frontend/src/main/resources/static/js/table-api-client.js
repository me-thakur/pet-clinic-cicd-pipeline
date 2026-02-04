/**
 * Table API Client
 * Centralized API communication layer for enhanced table functionality
 * Handles sort, filter, and bulk operation requests with error handling and retry logic
 * 
 * Requirements: 2.5, 5.3
 */

(function() {
    'use strict';

    /**
     * Configuration constants for API client
     */
    const API_CONFIG = {
        BASE_URL: '/api',
        DEFAULT_TIMEOUT: 10000, // 10 seconds
        MAX_RETRIES: 3,
        RETRY_DELAY: 1000, // 1 second
        RETRY_BACKOFF_MULTIPLIER: 2,
        CACHE_TTL: 300000, // 5 minutes
        REQUEST_HEADERS: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        }
    };

    /**
     * API Error class for structured error handling
     */
    class ApiError extends Error {
        constructor(message, status, response, originalError) {
            super(message);
            this.name = 'ApiError';
            this.status = status;
            this.response = response;
            this.originalError = originalError;
            this.timestamp = new Date();
        }

        isNetworkError() {
            return this.status === 0 || this.originalError instanceof TypeError;
        }

        isServerError() {
            return this.status >= 500;
        }

        isClientError() {
            return this.status >= 400 && this.status < 500;
        }

        isRetryable() {
            return this.isNetworkError() || this.isServerError() || this.status === 429;
        }
    }

    /**
     * Request cache for performance optimization
     */
    class RequestCache {
        constructor() {
            this.cache = new Map();
            this.timestamps = new Map();
        }

        /**
         * Generate cache key from request parameters
         * @param {string} method - HTTP method
         * @param {string} url - Request URL
         * @param {Object} params - Request parameters
         * @returns {string} Cache key
         */
        generateKey(method, url, params = {}) {
            const sortedParams = Object.keys(params)
                .sort()
                .reduce((result, key) => {
                    result[key] = params[key];
                    return result;
                }, {});
            
            return `${method}:${url}:${JSON.stringify(sortedParams)}`;
        }

        /**
         * Get cached response if valid
         * @param {string} key - Cache key
         * @returns {Object|null} Cached response or null
         */
        get(key) {
            const timestamp = this.timestamps.get(key);
            if (!timestamp || Date.now() - timestamp > API_CONFIG.CACHE_TTL) {
                this.delete(key);
                return null;
            }
            return this.cache.get(key);
        }

        /**
         * Set cached response
         * @param {string} key - Cache key
         * @param {Object} response - Response to cache
         */
        set(key, response) {
            this.cache.set(key, response);
            this.timestamps.set(key, Date.now());
        }

        /**
         * Delete cached entry
         * @param {string} key - Cache key
         */
        delete(key) {
            this.cache.delete(key);
            this.timestamps.delete(key);
        }

        /**
         * Clear all cache entries
         */
        clear() {
            this.cache.clear();
            this.timestamps.clear();
        }

        /**
         * Clear cache entries for specific entity type
         * @param {string} entityType - Entity type to clear
         */
        clearForEntity(entityType) {
            const keysToDelete = [];
            for (const key of this.cache.keys()) {
                if (key.includes(`/${entityType}`)) {
                    keysToDelete.push(key);
                }
            }
            keysToDelete.forEach(key => this.delete(key));
        }
    }

    /**
     * Main Table API Client class
     * Provides centralized API communication with error handling and retry logic
     */
    class TableApiClient {
        constructor(options = {}) {
            this.config = { ...API_CONFIG, ...options };
            this.cache = new RequestCache();
            this.abortControllers = new Map();
        }

        /**
         * Execute HTTP request with retry logic and error handling
         * @param {string} method - HTTP method
         * @param {string} url - Request URL
         * @param {Object} options - Request options
         * @returns {Promise<Object>} Response data
         */
        async executeRequest(method, url, options = {}) {
            const requestId = this.generateRequestId();
            let lastError;

            // Check cache for GET requests
            if (method === 'GET' && options.useCache !== false) {
                const cacheKey = this.cache.generateKey(method, url, options.params);
                const cachedResponse = this.cache.get(cacheKey);
                if (cachedResponse) {
                    console.debug('TableApiClient: Cache hit for', url);
                    return cachedResponse;
                }
            }

            for (let attempt = 1; attempt <= this.config.MAX_RETRIES; attempt++) {
                try {
                    const response = await this.performRequest(method, url, options, requestId);
                    
                    // Cache successful GET responses
                    if (method === 'GET' && options.useCache !== false) {
                        const cacheKey = this.cache.generateKey(method, url, options.params);
                        this.cache.set(cacheKey, response);
                    }

                    return response;

                } catch (error) {
                    lastError = error;
                    console.warn(`TableApiClient: Request attempt ${attempt} failed:`, error.message);

                    // Don't retry if not retryable or on last attempt
                    if (!error.isRetryable() || attempt === this.config.MAX_RETRIES) {
                        break;
                    }

                    // Wait before retry with exponential backoff
                    const delay = this.config.RETRY_DELAY * Math.pow(this.config.RETRY_BACKOFF_MULTIPLIER, attempt - 1);
                    await this.sleep(delay);
                }
            }

            // All retries failed
            console.error('TableApiClient: All retry attempts failed for', url, lastError);
            throw lastError;
        }

        /**
         * Perform single HTTP request
         * @param {string} method - HTTP method
         * @param {string} url - Request URL
         * @param {Object} options - Request options
         * @param {string} requestId - Unique request ID
         * @returns {Promise<Object>} Response data
         */
        async performRequest(method, url, options, requestId) {
            const abortController = new AbortController();
            this.abortControllers.set(requestId, abortController);

            try {
                // Build full URL with query parameters
                const fullUrl = this.buildUrl(url, options.params);
                
                // Prepare fetch options
                const fetchOptions = {
                    method,
                    headers: { ...this.config.REQUEST_HEADERS, ...options.headers },
                    signal: abortController.signal
                };

                // Add body for non-GET requests
                if (method !== 'GET' && options.body) {
                    fetchOptions.body = JSON.stringify(options.body);
                }

                // Set timeout
                const timeoutId = setTimeout(() => {
                    abortController.abort();
                }, options.timeout || this.config.DEFAULT_TIMEOUT);

                console.debug(`TableApiClient: ${method} ${fullUrl}`);
                
                const response = await fetch(fullUrl, fetchOptions);
                clearTimeout(timeoutId);

                // Handle response
                return await this.handleResponse(response, fullUrl);

            } catch (error) {
                if (error.name === 'AbortError') {
                    throw new ApiError('Request timeout', 0, null, error);
                }
                
                if (error instanceof ApiError) {
                    throw error;
                }

                // Network or other errors
                throw new ApiError(
                    `Network error: ${error.message}`,
                    0,
                    null,
                    error
                );
            } finally {
                this.abortControllers.delete(requestId);
            }
        }

        /**
         * Handle HTTP response and extract data
         * @param {Response} response - Fetch response
         * @param {string} url - Request URL for error context
         * @returns {Promise<Object>} Response data
         */
        async handleResponse(response, url) {
            let responseData = null;
            let errorMessage = `HTTP ${response.status}`;

            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    responseData = await response.json();
                } else {
                    responseData = await response.text();
                }
            } catch (parseError) {
                console.warn('TableApiClient: Failed to parse response body:', parseError);
            }

            if (!response.ok) {
                // Extract error message from response if available
                if (responseData && typeof responseData === 'object') {
                    errorMessage = responseData.message || responseData.error || errorMessage;
                } else if (typeof responseData === 'string') {
                    errorMessage = responseData;
                }

                throw new ApiError(
                    `Request failed: ${errorMessage}`,
                    response.status,
                    responseData
                );
            }

            return responseData;
        }

        /**
         * Build full URL with query parameters
         * @param {string} path - URL path
         * @param {Object} params - Query parameters
         * @returns {string} Full URL
         */
        buildUrl(path, params = {}) {
            const url = new URL(path, window.location.origin);
            
            Object.entries(params).forEach(([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                    if (Array.isArray(value)) {
                        // Handle array parameters (like multiple sort parameters)
                        value.forEach(item => {
                            url.searchParams.append(key, item);
                        });
                    } else {
                        url.searchParams.append(key, value);
                    }
                }
            });

            return url.toString();
        }

        /**
         * Generate unique request ID
         * @returns {string} Request ID
         */
        generateRequestId() {
            return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        }

        /**
         * Sleep for specified duration
         * @param {number} ms - Milliseconds to sleep
         * @returns {Promise<void>}
         */
        sleep(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        // Public API Methods

        /**
         * Get entities with sort and filter support
         * @param {string} entityType - Entity type (visits, owners, pets, veterinarians)
         * @param {Object} options - Request options
         * @returns {Promise<Object>} Paginated response
         */
        async getEntities(entityType, options = {}) {
            const {
                page = 0,
                size = 10,
                sort = null,
                useCache = true,
                ...otherParams
            } = options;

            const params = {
                page: page.toString(),
                size: size.toString()
            };

            // Add sort parameters - handle both single sort and multi-sort
            if (sort) {
                if (Array.isArray(sort)) {
                    // Multi-column sort: add multiple sort parameters
                    sort.forEach(sortParam => {
                        if (!params.sort) {
                            params.sort = sortParam;
                        } else {
                            // Convert to array if not already
                            if (!Array.isArray(params.sort)) {
                                params.sort = [params.sort];
                            }
                            params.sort.push(sortParam);
                        }
                    });
                } else {
                    // Single sort parameter
                    params.sort = sort;
                }
            }

            // Add other parameters (filters, etc.)
            Object.entries(otherParams).forEach(([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                    params[key] = value;
                }
            });

            console.log('API request params:', params);

            return await this.executeRequest('GET', `${this.config.BASE_URL}/${entityType}`, {
                params,
                useCache
            });
        }

        /**
         * Execute bulk delete operation
         * @param {string} entityType - Entity type
         * @param {Object} request - Bulk delete request
         * @returns {Promise<Object>} Bulk operation result
         */
        async bulkDelete(entityType, request) {
            // Clear cache for this entity type since data will change
            this.cache.clearForEntity(entityType);

            return await this.executeRequest('DELETE', `${this.config.BASE_URL}/${entityType}/bulk`, {
                body: request,
                useCache: false
            });
        }

        /**
         * Get available filter values for a column
         * @param {string} entityType - Entity type
         * @param {string} column - Column name
         * @returns {Promise<Array>} Available filter values
         */
        async getFilterValues(entityType, column) {
            return await this.executeRequest('GET', `${this.config.BASE_URL}/${entityType}/filter-values/${column}`, {
                useCache: true
            });
        }

        /**
         * Validate sort parameters
         * @param {string} entityType - Entity type
         * @param {string} column - Sort column
         * @param {string} direction - Sort direction
         * @returns {Promise<Object>} Validation result
         */
        async validateSort(entityType, column, direction) {
            return await this.executeRequest('GET', `${this.config.BASE_URL}/${entityType}/validate-sort`, {
                params: { column, direction },
                useCache: true
            });
        }

        // Utility Methods

        /**
         * Transform frontend filter format to backend format
         * @param {Array} frontendFilters - Frontend filter array
         * @param {string} entityType - Entity type
         * @returns {Object} Backend filter object
         */
        transformFiltersToBackend(frontendFilters, entityType) {
            const backendFilters = {};
            
            frontendFilters.forEach(filter => {
                if (filter.column && filter.value !== null && filter.value !== undefined) {
                    // For simple filters, use column name as key
                    backendFilters[filter.column] = filter.value;
                }
            });

            return backendFilters;
        }

        /**
         * Transform backend response to frontend format
         * @param {Object} backendResponse - Backend response
         * @returns {Object} Frontend-compatible response
         */
        transformResponseToFrontend(backendResponse) {
            // The backend response is already in the expected format
            // This method exists for future customization if needed
            return backendResponse;
        }

        /**
         * Create bulk delete request object
         * @param {Array} selectedIds - Array of selected item IDs
         * @param {boolean} selectAll - Whether all items are selected
         * @param {Array} currentFilters - Current active filters
         * @param {string} entityType - Entity type
         * @returns {Object} Bulk delete request
         */
        createBulkDeleteRequest(selectedIds, selectAll, currentFilters, entityType) {
            return {
                selectedIds: selectedIds.map(id => parseInt(id)),
                selectAll: selectAll,
                currentFilters: currentFilters.map(filter => ({
                    field: filter.column,
                    operator: filter.operator || 'equals',
                    value: filter.value,
                    entityType: entityType
                })),
                entityType: entityType
            };
        }

        /**
         * Abort all pending requests
         */
        abortAllRequests() {
            this.abortControllers.forEach(controller => {
                controller.abort();
            });
            this.abortControllers.clear();
        }

        /**
         * Clear all cached responses
         */
        clearCache() {
            this.cache.clear();
        }

        /**
         * Clear cache for specific entity type
         * @param {string} entityType - Entity type
         */
        clearEntityCache(entityType) {
            this.cache.clearForEntity(entityType);
        }

        /**
         * Get cache statistics
         * @returns {Object} Cache statistics
         */
        getCacheStats() {
            return {
                size: this.cache.cache.size,
                keys: Array.from(this.cache.cache.keys())
            };
        }

        /**
         * Check if client is online
         * @returns {boolean} Online status
         */
        isOnline() {
            return navigator.onLine;
        }

        /**
         * Get client configuration
         * @returns {Object} Current configuration
         */
        getConfig() {
            return { ...this.config };
        }

        /**
         * Update client configuration
         * @param {Object} newConfig - New configuration options
         */
        updateConfig(newConfig) {
            this.config = { ...this.config, ...newConfig };
        }
    }

    // Export to global scope
    window.TableApiClient = TableApiClient;
    window.ApiError = ApiError;

    // Create default instance
    window.tableApiClient = new TableApiClient();

    console.log('TableApiClient: Initialized with configuration:', window.tableApiClient.getConfig());

})();