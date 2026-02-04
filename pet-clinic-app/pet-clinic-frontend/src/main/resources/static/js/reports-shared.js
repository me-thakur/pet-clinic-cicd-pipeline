/**
 * Shared JavaScript functions for all report pages
 * This file provides consistent export and report generation functionality
 * across all report templates in the Pet Clinic application.
 */

// Ensure functions are available globally
window.PetClinicReports = window.PetClinicReports || {};

/**
 * Generate a report by navigating to the appropriate report page
 * @param {string} reportType - Type of report (visits, revenue, dashboard)
 */
window.PetClinicReports.generateReport = function(reportType) {
    console.log('PetClinicReports.generateReport called with:', reportType);
    
    try {
        const startDate = document.getElementById('startDate')?.value;
        const endDate = document.getElementById('endDate')?.value;
        const veterinarianId = document.getElementById('veterinarianId')?.value;
        const species = document.getElementById('species')?.value;

        if (!startDate || !endDate) {
            alert('Please select both start and end dates.');
            return;
        }

        let url = `/dashboard/reports/${reportType}?startDate=${startDate}&endDate=${endDate}`;
        if (veterinarianId) url += `&veterinarianId=${veterinarianId}`;
        if (species) url += `&species=${species}`;

        console.log('Navigating to:', url);
        
        // Add a small delay to ensure any pending operations complete
        setTimeout(() => {
            window.location.href = url;
        }, 100);
        
    } catch (error) {
        console.error('Error in generateReport:', error);
        alert('Error generating report. Please try again.');
    }
};

/**
 * Export a report in the specified format
 * @param {string} reportType - Type of report (visits, revenue, dashboard)
 * @param {string} format - Export format (pdf, csv)
 * @param {Object} options - Additional options for export
 */
window.PetClinicReports.exportReport = function(reportType, format, options = {}) {
    console.log('PetClinicReports.exportReport called with:', reportType, format, options);
    
    // Check if enhanced async export is available and preferred
    if (window.enhancedReportExporter && options.async !== false) {
        console.log('Using enhanced async export');
        return window.startAsyncExport(reportType, format);
    }
    
    // Fallback to synchronous export
    console.log('Using synchronous export fallback');
    
    // Get parameters from form inputs or options
    const startDate = options.startDate || document.getElementById('startDate')?.value;
    const endDate = options.endDate || document.getElementById('endDate')?.value;
    const veterinarianId = options.veterinarianId || document.getElementById('veterinarianId')?.value;
    const species = options.species || document.getElementById('species')?.value;
    const includeTrends = options.includeTrends || (reportType === 'revenue' ? 'true' : 'false');
    
    if (!startDate || !endDate) {
        alert('Please select both start and end dates.');
        return;
    }

    // Use form submission for proper authentication and CSRF handling
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/dashboard/export/${reportType}`;
    form.style.display = 'none';
    
    // Add all parameters as hidden inputs
    const params = {
        'startDate': startDate,
        'endDate': endDate,
        'format': format,
        'includeTrends': includeTrends
    };
    
    if (veterinarianId) {
        params['veterinarianId'] = veterinarianId;
    }
    if (species) {
        params['species'] = species;
    }
    
    // Add CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken && csrfToken.getAttribute('content')) {
        params['_csrf'] = csrfToken.getAttribute('content');
    }
    
    // Create hidden inputs for all parameters
    for (const [key, value] of Object.entries(params)) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = value;
        form.appendChild(input);
    }
    
    // Add form to document and submit
    document.body.appendChild(form);
    
    console.log('Submitting export form for:', reportType, format);
    console.log('Form action:', form.action);
    console.log('Form method:', form.method);
    
    form.submit();
    
    // Clean up
    setTimeout(() => {
        if (document.body.contains(form)) {
            document.body.removeChild(form);
        }
    }, 1000);
};

/**
 * Show loading toast notification
 * @param {string} message - Loading message
 * @returns {Object} Toast instance
 */
window.PetClinicReports.showLoadingToast = function(message) {
    const toastContainer = document.getElementById('toast-container') || this.createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white bg-info border-0';
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                <i class="bi bi-hourglass-split"></i>
                ${message}
            </div>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    
    const bsToast = new bootstrap.Toast(toast, { autohide: false });
    bsToast.show();
    
    return bsToast;
};

/**
 * Show error toast notification
 * @param {string} message - Error message
 */
window.PetClinicReports.showErrorToast = function(message) {
    const toastContainer = document.getElementById('toast-container') || this.createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white bg-danger border-0';
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                <i class="bi bi-exclamation-triangle"></i>
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', () => {
        toast.remove();
    });
};

/**
 * Create toast container if it doesn't exist
 * @returns {HTMLElement} Toast container
 */
window.PetClinicReports.createToastContainer = function() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container position-fixed top-0 end-0 p-3';
    container.style.zIndex = '1055';
    document.body.appendChild(container);
    return container;
};

/**
 * Generate a quick report for a specific time period
 * @param {string} period - Time period (today, week, month, quarter)
 */
window.PetClinicReports.generateQuickReport = function(period) {
    console.log('PetClinicReports.generateQuickReport called with:', period);
    
    const today = new Date();
    let startDate, endDate;

    switch (period) {
        case 'today':
            startDate = endDate = today.toISOString().split('T')[0];
            break;
        case 'week':
            const weekStart = new Date(today.setDate(today.getDate() - today.getDay()));
            startDate = weekStart.toISOString().split('T')[0];
            endDate = new Date().toISOString().split('T')[0];
            break;
        case 'month':
            startDate = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
            endDate = new Date().toISOString().split('T')[0];
            break;
        case 'quarter':
            const quarter = Math.floor(today.getMonth() / 3);
            startDate = new Date(today.getFullYear(), quarter * 3, 1).toISOString().split('T')[0];
            endDate = new Date().toISOString().split('T')[0];
            break;
    }

    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    
    if (startDateInput) startDateInput.value = startDate;
    if (endDateInput) endDateInput.value = endDate;
    
    // Generate visit statistics for quick reports
    window.PetClinicReports.generateReport('visits');
};

/**
 * Export report from individual report pages (simplified version)
 * This function is used on individual report pages where parameters are already known
 * @param {string} format - Export format (pdf, csv)
 * @param {Object} pageData - Data from the current page (injected by Thymeleaf)
 */
window.PetClinicReports.exportFromPage = function(format, pageData = {}) {
    console.log('PetClinicReports.exportFromPage called with:', format, pageData);
    
    // Determine report type from current URL
    let reportType = 'visits'; // default
    const path = window.location.pathname;
    if (path.includes('/revenue')) {
        reportType = 'revenue';
    } else if (path.includes('/dashboard') && !path.includes('/reports/')) {
        reportType = 'dashboard';
    }
    
    // Use the main export function with page data
    window.PetClinicReports.exportReport(reportType, format, pageData);
};

// Create global aliases for backward compatibility
window.generateReport = function(reportType) {
    return window.PetClinicReports.generateReport(reportType);
};

window.exportReport = function(reportTypeOrFormat, format) {
    // Handle different calling patterns
    if (format) {
        // Called with reportType and format (from main reports page)
        return window.PetClinicReports.exportReport(reportTypeOrFormat, format);
    } else {
        // Called with just format (from individual report pages)
        return window.PetClinicReports.exportFromPage(reportTypeOrFormat);
    }
};

window.generateQuickReport = function(period) {
    return window.PetClinicReports.generateQuickReport(period);
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('PetClinic Reports JavaScript loaded - functions available globally');
    
    // Initialize date inputs with default values if they exist and are empty
    const today = new Date();
    const thirtyDaysAgo = new Date(today.getTime() - (30 * 24 * 60 * 60 * 1000));
    
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    
    if (startDateInput && !startDateInput.value) {
        startDateInput.value = thirtyDaysAgo.toISOString().split('T')[0];
    }
    if (endDateInput && !endDateInput.value) {
        endDateInput.value = today.toISOString().split('T')[0];
    }
    
    // Debug: Check if CSRF token is available
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken) {
        console.log('CSRF token found:', csrfToken.getAttribute('content').substring(0, 10) + '...');
    } else {
        console.warn('CSRF token not found in page');
    }
    
    console.log('Functions available:');
    console.log('- window.exportReport:', typeof window.exportReport);
    console.log('- window.generateReport:', typeof window.generateReport);
    console.log('- window.generateQuickReport:', typeof window.generateQuickReport);
    console.log('- PetClinicReports.exportReport:', typeof window.PetClinicReports.exportReport);
    console.log('- PetClinicReports.generateReport:', typeof window.PetClinicReports.generateReport);
});

// Immediate verification
console.log('PetClinic Reports JavaScript loaded immediately');
console.log('Functions defined in global scope:');
console.log('- window.exportReport:', typeof window.exportReport);
console.log('- window.generateReport:', typeof window.generateReport);
console.log('- window.generateQuickReport:', typeof window.generateQuickReport);