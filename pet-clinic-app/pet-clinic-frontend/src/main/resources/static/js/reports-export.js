/**
 * Reports Export Functionality
 * Handles PDF and CSV export functionality for Pet Clinic reports
 */

let currentReportType = '';

function generateReport(reportType) {
    console.log('generateReport called with:', reportType);
    currentReportType = reportType;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const veterinarianId = document.getElementById('veterinarianId').value;
    const species = document.getElementById('species').value;

    if (!startDate || !endDate) {
        alert('Please select both start and end dates.');
        return;
    }

    showLoading();

    let url = `/dashboard/reports/${reportType}?startDate=${startDate}&endDate=${endDate}`;
    if (veterinarianId) url += `&veterinarianId=${veterinarianId}`;
    if (species) url += `&species=${species}`;

    console.log('Navigating to:', url);
    window.location.href = url;
}

function exportReport(reportType, format) {
    console.log('exportReport called with:', reportType, format);
    
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const veterinarianId = document.getElementById('veterinarianId').value;
    const species = document.getElementById('species').value;
    
    if (!startDate || !endDate) {
        alert('Please select both start and end dates.');
        return;
    }

    const includeTrends = reportType === 'revenue' ? 'true' : 'false';
    
    // Build URL with parameters
    let url = `/dashboard/export/${reportType}?startDate=${startDate}&endDate=${endDate}&format=${format}&includeTrends=${includeTrends}`;
    
    // Add optional filters
    if (veterinarianId) {
        url += `&veterinarianId=${veterinarianId}`;
    }
    if (species) {
        url += `&species=${species}`;
    }
    
    // Add CSRF token if available
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken && csrfToken.getAttribute('content')) {
        url += `&_csrf=${encodeURIComponent(csrfToken.getAttribute('content'))}`;
    }
    
    console.log('Export URL:', url);
    
    // Create a temporary link and click it to trigger download
    const link = document.createElement('a');
    link.href = url;
    link.download = `${reportType}-report-${startDate}-to-${endDate}.${format}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    console.log('Export initiated for:', reportType, format);
}

function generateQuickReport(period) {
    console.log('generateQuickReport called with:', period);
    
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

    document.getElementById('startDate').value = startDate;
    document.getElementById('endDate').value = endDate;
    
    // Generate visit statistics for quick reports
    generateReport('visits');
}

function showLoading() {
    const loadingSpinner = document.getElementById('loadingSpinner');
    const reportResults = document.getElementById('reportResults');
    
    if (loadingSpinner) loadingSpinner.style.display = 'block';
    if (reportResults) reportResults.style.display = 'none';
}

function hideLoading() {
    const loadingSpinner = document.getElementById('loadingSpinner');
    if (loadingSpinner) loadingSpinner.style.display = 'none';
}

// Debug function to test export
function testExport() {
    console.log('Testing export functionality...');
    exportReport('visits', 'pdf');
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('Reports export JavaScript loaded');
    
    // Initialize date inputs with default values
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
    
    // Debug: Check if veterinarian dropdown is populated
    setTimeout(function() {
        const dropdown = document.getElementById('veterinarianId');
        if (dropdown) {
            console.log('Veterinarian dropdown options:', dropdown.options.length);
            for (let i = 0; i < dropdown.options.length; i++) {
                console.log(`Option ${i}:`, dropdown.options[i].value, dropdown.options[i].text);
            }
        }
    }, 1000);
    
    // Make functions globally available
    window.generateReport = generateReport;
    window.exportReport = exportReport;
    window.generateQuickReport = generateQuickReport;
    window.testExport = testExport;
    
    console.log('Export functions made globally available');
});