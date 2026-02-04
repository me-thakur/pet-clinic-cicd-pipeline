/**
 * CSRF Protection Utility
 * Provides CSRF token management for secure form submissions
 * 
 * Validates: Requirements 17.3, 17.5
 */
class CSRFProtection {
    constructor() {
        this.token = null;
        this.headerName = 'X-CSRF-TOKEN';
        this.init();
    }
    
    /**
     * Initialize CSRF protection by fetching the token
     */
    init() {
        this.fetchCSRFToken();
        this.setupAjaxDefaults();
        this.setupFormSubmissionHandlers();
    }
    
    /**
     * Fetch CSRF token from the server
     */
    async fetchCSRFToken() {
        try {
            const response = await fetch('/api/csrf-token', {
                method: 'GET',
                credentials: 'same-origin'
            });
            
            if (response.ok) {
                const data = await response.json();
                this.token = data.token;
                this.updateTokenInForms();
            } else {
                console.warn('Failed to fetch CSRF token');
            }
        } catch (error) {
            console.error('Error fetching CSRF token:', error);
        }
    }
    
    /**
     * Get the current CSRF token
     */
    getToken() {
        return this.token;
    }
    
    /**
     * Get CSRF token from cookie (fallback method)
     */
    getTokenFromCookie() {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'XSRF-TOKEN') {
                return decodeURIComponent(value);
            }
        }
        return null;
    }
    
    /**
     * Setup default AJAX headers to include CSRF token
     */
    setupAjaxDefaults() {
        // Setup for jQuery if available
        if (typeof $ !== 'undefined') {
            $.ajaxSetup({
                beforeSend: (xhr, settings) => {
                    if (this.requiresCSRFToken(settings.type, settings.url)) {
                        const token = this.token || this.getTokenFromCookie();
                        if (token) {
                            xhr.setRequestHeader(this.headerName, token);
                        }
                    }
                }
            });
        }
        
        // Setup for fetch API
        const originalFetch = window.fetch;
        window.fetch = (url, options = {}) => {
            if (this.requiresCSRFToken(options.method, url)) {
                const token = this.token || this.getTokenFromCookie();
                if (token) {
                    options.headers = {
                        ...options.headers,
                        [this.headerName]: token
                    };
                }
            }
            return originalFetch(url, options);
        };
    }
    
    /**
     * Check if request requires CSRF token
     */
    requiresCSRFToken(method, url) {
        if (!method) method = 'GET';
        method = method.toUpperCase();
        
        // CSRF required for state-changing operations
        const requiresCSRF = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method);
        
        // Skip CSRF for authentication endpoints
        const isAuthEndpoint = url && (url.includes('/api/auth/') || url.includes('/api/test/'));
        
        return requiresCSRF && !isAuthEndpoint;
    }
    
    /**
     * Setup form submission handlers to include CSRF token
     */
    setupFormSubmissionHandlers() {
        document.addEventListener('submit', (event) => {
            const form = event.target;
            if (form.tagName === 'FORM') {
                this.addCSRFTokenToForm(form);
            }
        });
    }
    
    /**
     * Add CSRF token to a form
     */
    addCSRFTokenToForm(form) {
        const method = (form.method || 'GET').toUpperCase();
        const action = form.action || window.location.href;
        
        if (this.requiresCSRFToken(method, action)) {
            const token = this.token || this.getTokenFromCookie();
            if (token) {
                // Remove existing CSRF token input
                const existingToken = form.querySelector('input[name="_csrf"]');
                if (existingToken) {
                    existingToken.remove();
                }
                
                // Add new CSRF token input
                const tokenInput = document.createElement('input');
                tokenInput.type = 'hidden';
                tokenInput.name = '_csrf';
                tokenInput.value = token;
                form.appendChild(tokenInput);
            }
        }
    }
    
    /**
     * Update CSRF token in all existing forms
     */
    updateTokenInForms() {
        const forms = document.querySelectorAll('form');
        forms.forEach(form => {
            this.addCSRFTokenToForm(form);
        });
    }
    
    /**
     * Manually add CSRF token to AJAX request headers
     */
    addTokenToHeaders(headers = {}) {
        const token = this.token || this.getTokenFromCookie();
        if (token) {
            headers[this.headerName] = token;
        }
        return headers;
    }
    
    /**
     * Create a secure AJAX request with CSRF protection
     */
    async secureRequest(url, options = {}) {
        const token = this.token || this.getTokenFromCookie();
        
        if (this.requiresCSRFToken(options.method, url) && token) {
            options.headers = {
                ...options.headers,
                [this.headerName]: token
            };
        }
        
        // Add credentials for cookie-based authentication
        options.credentials = options.credentials || 'same-origin';
        
        try {
            const response = await fetch(url, options);
            
            // Handle CSRF token refresh if needed
            if (response.status === 403) {
                const errorText = await response.text();
                if (errorText.includes('CSRF') || errorText.includes('csrf')) {
                    console.warn('CSRF token may be expired, refreshing...');
                    await this.fetchCSRFToken();
                    
                    // Retry the request with new token
                    if (this.token) {
                        options.headers[this.headerName] = this.token;
                        return fetch(url, options);
                    }
                }
            }
            
            return response;
        } catch (error) {
            console.error('Secure request failed:', error);
            throw error;
        }
    }
    
    /**
     * Validate that CSRF protection is working
     */
    async validateProtection() {
        try {
            // Try to make a request without CSRF token
            const response = await fetch('/api/test/csrf-validation', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ test: true }),
                credentials: 'same-origin'
            });
            
            if (response.status === 403) {
                console.log('CSRF protection is working correctly');
                return true;
            } else {
                console.warn('CSRF protection may not be working properly');
                return false;
            }
        } catch (error) {
            console.error('Error validating CSRF protection:', error);
            return false;
        }
    }
}

// Initialize CSRF protection when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.csrfProtection = new CSRFProtection();
});

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CSRFProtection;
}