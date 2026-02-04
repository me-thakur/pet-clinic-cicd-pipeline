/**
 * Enhanced Report Export with Progress Indicators and Retry Mechanisms
 * Provides async export functionality with real-time progress tracking
 * Validates: Requirements 4.2, 4.3, 4.4
 */

class EnhancedReportExporter {
    constructor() {
        this.activeExports = new Map();
        this.progressCheckInterval = 2000; // Check progress every 2 seconds
        this.maxRetries = 3;
        this.init();
    }
    
    init() {
        console.log('Enhanced Report Exporter initialized');
        this.setupEventListeners();
        this.checkUserPermissions();
    }
    
    async checkUserPermissions() {
        try {
            const response = await fetch('/api/user/permissions');
            if (response.ok) {
                const permissions = await response.json();
                this.userPermissions = permissions;
                this.isAdmin = permissions.roles && permissions.roles.includes('ADMIN');
            } else {
                // Fallback: assume no admin permissions if check fails
                this.isAdmin = false;
                this.userPermissions = { roles: [] };
            }
        } catch (error) {
            console.warn('Failed to check user permissions:', error);
            this.isAdmin = false;
            this.userPermissions = { roles: [] };
        }
        
        // Update UI based on permissions
        this.updateUIForPermissions();
    }
    
    updateUIForPermissions() {
        const exportButtons = document.querySelectorAll('[data-export-async]');
        exportButtons.forEach(button => {
            if (!this.isAdmin) {
                button.disabled = true;
                button.title = 'Admin privileges required for report export';
                button.innerHTML = '<i class="bi bi-lock"></i> Admin Only';
                button.classList.add('btn-outline-secondary');
                button.classList.remove('btn-outline-primary', 'btn-outline-success');
            }
        });
    }
    
    async checkAdminPermissions() {
        // If we haven't checked permissions yet, do it now
        if (this.isAdmin === undefined) {
            await this.checkUserPermissions();
        }
        return this.isAdmin;
    }
    
    showPermissionError(button) {
        const errorAlert = document.createElement('div');
        errorAlert.className = 'alert alert-warning alert-dismissible fade show mt-2';
        errorAlert.innerHTML = `
            <i class="bi bi-shield-exclamation"></i>
            <strong>Access Denied:</strong> Report export functionality requires administrator privileges. 
            Please contact your system administrator if you need access to this feature.
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        button.parentNode.insertBefore(errorAlert, button.nextSibling);
        
        // Auto-dismiss after 15 seconds
        setTimeout(() => {
            if (errorAlert.parentNode) {
                errorAlert.remove();
            }
        }, 15000);
        
        this.showNotification('Admin privileges required for report export', 'warning');
    }
    
    setupEventListeners() {
        // Listen for export button clicks
        document.addEventListener('click', (event) => {
            if (event.target.matches('[data-export-async]')) {
                event.preventDefault();
                this.handleAsyncExportClick(event.target);
            }
            
            if (event.target.matches('[data-retry-export]')) {
                event.preventDefault();
                this.retryExport(event.target.dataset.exportId);
            }
            
            if (event.target.matches('[data-cancel-export]')) {
                event.preventDefault();
                this.cancelExport(event.target.dataset.exportId);
            }
        });
    }
    
    async handleAsyncExportClick(button) {
        const reportType = button.dataset.reportType;
        const format = button.dataset.format || 'pdf';
        
        console.log('Starting async export:', reportType, format);
        
        try {
            // Check admin permissions first
            const hasPermission = await this.checkAdminPermissions();
            if (!hasPermission) {
                this.showPermissionError(button);
                return;
            }
            
            // Disable button and show loading state
            button.disabled = true;
            button.innerHTML = '<i class="bi bi-hourglass-split"></i> Starting Export...';
            
            // Collect form parameters
            const params = this.collectExportParameters(reportType);
            params.format = format;
            
            // Add CSRF token
            const csrfToken = document.querySelector('meta[name="_csrf"]');
            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded',
            };
            if (csrfToken && csrfToken.getAttribute('content')) {
                headers['X-CSRF-TOKEN'] = csrfToken.getAttribute('content');
            }
            
            // Start async export
            const response = await fetch(`/api/reports/async/export/${reportType}`, {
                method: 'POST',
                headers: headers,
                body: new URLSearchParams(params)
            });
            
            if (!response.ok) {
                if (response.status === 403) {
                    throw new Error('Access denied. Admin privileges required for report export.');
                } else if (response.status === 401) {
                    throw new Error('Authentication required. Please log in and try again.');
                } else {
                    const errorText = await response.text();
                    throw new Error(`Export failed: ${response.status} ${response.statusText}${errorText ? ' - ' + errorText : ''}`);
                }
            }
            
            const progress = await response.json();
            
            // Start tracking progress
            this.trackExportProgress(progress.exportId, reportType, format, button);
            
        } catch (error) {
            console.error('Failed to start export:', error);
            this.showExportError(button, error.message);
            button.disabled = false;
            button.innerHTML = button.dataset.originalText || 'Export';
        }
    }
    
    collectExportParameters(reportType) {
        const params = {};
        
        // Handle "current" report type by determining actual report type from context
        if (reportType === 'current') {
            reportType = this.getCurrentReportType();
        }
        
        // Common parameters
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        
        if (startDateInput && startDateInput.value) {
            params.startDate = startDateInput.value;
        }
        
        if (endDateInput && endDateInput.value) {
            params.endDate = endDateInput.value;
        }
        
        // Report-specific parameters
        switch (reportType) {
            case 'visits':
                const veterinarianSelect = document.getElementById('veterinarianId');
                const speciesSelect = document.getElementById('species');
                
                if (veterinarianSelect && veterinarianSelect.value) {
                    params.veterinarianId = veterinarianSelect.value;
                }
                
                if (speciesSelect && speciesSelect.value) {
                    params.species = speciesSelect.value;
                }
                break;
                
            case 'revenue':
                const includeTrendsCheckbox = document.getElementById('includeTrends');
                const revenueVetSelect = document.getElementById('veterinarianId');
                
                if (includeTrendsCheckbox) {
                    params.includeTrends = includeTrendsCheckbox.checked;
                }
                
                if (revenueVetSelect && revenueVetSelect.value) {
                    params.veterinarianId = revenueVetSelect.value;
                }
                break;
                
            case 'dashboard':
                const dateInput = document.getElementById('date');
                if (dateInput && dateInput.value) {
                    params.date = dateInput.value;
                }
                break;
        }
        
        return params;
    }
    
    getCurrentReportType() {
        // Try to determine report type from URL
        const path = window.location.pathname;
        if (path.includes('/revenue')) {
            return 'revenue';
        } else if (path.includes('/dashboard') && !path.includes('/reports/')) {
            return 'dashboard';
        } else if (path.includes('/visits')) {
            return 'visits';
        }
        
        // Try to determine from page title or other context
        const title = document.title.toLowerCase();
        if (title.includes('revenue')) {
            return 'revenue';
        } else if (title.includes('dashboard')) {
            return 'dashboard';
        } else if (title.includes('visit')) {
            return 'visits';
        }
        
        // Default fallback
        return 'visits';
    }
    
    async trackExportProgress(exportId, reportType, format, button) {
        console.log('Tracking export progress:', exportId);
        
        // Create progress UI
        const progressContainer = this.createProgressUI(exportId, reportType, format, button);
        
        // Store export info
        this.activeExports.set(exportId, {
            reportType,
            format,
            button,
            progressContainer,
            intervalId: null
        });
        
        // Start progress polling
        const intervalId = setInterval(async () => {
            try {
                await this.checkExportProgress(exportId);
            } catch (error) {
                console.error('Error checking export progress:', error);
                this.handleExportError(exportId, error.message);
            }
        }, this.progressCheckInterval);
        
        this.activeExports.get(exportId).intervalId = intervalId;
    }
    
    async checkExportProgress(exportId) {
        const response = await fetch(`/api/reports/async/progress/${exportId}`);
        
        if (!response.ok) {
            if (response.status === 404) {
                this.handleExportError(exportId, 'Export not found');
                return;
            }
            throw new Error(`Failed to get progress: ${response.status}`);
        }
        
        const progress = await response.json();
        this.updateProgressUI(exportId, progress);
        
        // Handle completion or failure
        if (progress.status === 'COMPLETED') {
            this.handleExportComplete(exportId);
        } else if (progress.status === 'FAILED') {
            this.handleExportError(exportId, progress.errorMessage);
        } else if (progress.status === 'CANCELLED') {
            this.handleExportCancelled(exportId);
        }
    }
    
    createProgressUI(exportId, reportType, format, button) {
        // Store original button text
        if (!button.dataset.originalText) {
            button.dataset.originalText = button.innerHTML;
        }
        
        // Create progress container
        const progressContainer = document.createElement('div');
        progressContainer.className = 'export-progress-container mt-3';
        progressContainer.innerHTML = `
            <div class="card border-primary">
                <div class="card-header bg-light">
                    <h6 class="card-title mb-0">
                        <i class="bi bi-file-earmark-arrow-down text-primary"></i>
                        Exporting ${this.formatReportTypeName(reportType)} Report (${format.toUpperCase()})
                        <span class="badge bg-secondary ms-2">ID: ${exportId.substring(0, 8)}</span>
                    </h6>
                </div>
                <div class="card-body">
                    <div class="progress mb-3" style="height: 25px;">
                        <div class="progress-bar progress-bar-striped progress-bar-animated bg-primary" 
                             role="progressbar" 
                             style="width: 0%" 
                             aria-valuenow="0" 
                             aria-valuemin="0" 
                             aria-valuemax="100">
                            <span class="progress-text">0%</span>
                        </div>
                    </div>
                    <div class="export-status mb-3">
                        <div class="row">
                            <div class="col-md-6">
                                <small class="text-muted">
                                    <strong>Status:</strong> <span class="current-step">Initializing...</span>
                                </small>
                            </div>
                            <div class="col-md-6 text-end">
                                <small class="text-muted">
                                    <strong>ETA:</strong> <span class="estimated-time">Calculating...</span>
                                </small>
                            </div>
                        </div>
                        <div class="row mt-1">
                            <div class="col-md-6">
                                <small class="text-muted">
                                    <strong>Started:</strong> <span class="start-time">${new Date().toLocaleTimeString()}</span>
                                </small>
                            </div>
                            <div class="col-md-6 text-end">
                                <small class="text-muted">
                                    <strong>Elapsed:</strong> <span class="elapsed-time">0s</span>
                                </small>
                            </div>
                        </div>
                    </div>
                    <div class="export-actions">
                        <button type="button" 
                                class="btn btn-sm btn-outline-danger" 
                                data-cancel-export 
                                data-export-id="${exportId}">
                            <i class="bi bi-x-circle"></i> Cancel Export
                        </button>
                        <button type="button" 
                                class="btn btn-sm btn-outline-secondary ms-2" 
                                onclick="window.enhancedReportExporter.minimizeProgress('${exportId}')">
                            <i class="bi bi-dash"></i> Minimize
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        // Insert after the button
        button.parentNode.insertBefore(progressContainer, button.nextSibling);
        
        // Store start time for elapsed time calculation
        progressContainer.dataset.startTime = Date.now();
        
        return progressContainer;
    }
    
    formatReportTypeName(reportType) {
        switch (reportType) {
            case 'visits': return 'Visit Statistics';
            case 'revenue': return 'Revenue Analysis';
            case 'dashboard': return 'Dashboard Summary';
            default: return reportType.charAt(0).toUpperCase() + reportType.slice(1);
        }
    }
    
    minimizeProgress(exportId) {
        const exportInfo = this.activeExports.get(exportId);
        if (!exportInfo) return;
        
        const container = exportInfo.progressContainer;
        const cardBody = container.querySelector('.card-body');
        const minimizeBtn = container.querySelector('[onclick*="minimizeProgress"]');
        
        if (cardBody.style.display === 'none') {
            // Restore
            cardBody.style.display = 'block';
            minimizeBtn.innerHTML = '<i class="bi bi-dash"></i> Minimize';
        } else {
            // Minimize
            cardBody.style.display = 'none';
            minimizeBtn.innerHTML = '<i class="bi bi-plus"></i> Expand';
        }
    }
    
    updateProgressUI(exportId, progress) {
        const exportInfo = this.activeExports.get(exportId);
        if (!exportInfo) return;
        
        const container = exportInfo.progressContainer;
        const progressBar = container.querySelector('.progress-bar');
        const progressText = container.querySelector('.progress-text');
        const currentStep = container.querySelector('.current-step');
        const estimatedTime = container.querySelector('.estimated-time');
        const elapsedTime = container.querySelector('.elapsed-time');
        
        // Update progress bar
        const percentage = Math.min(100, Math.max(0, progress.progressPercentage || 0));
        progressBar.style.width = `${percentage}%`;
        progressBar.setAttribute('aria-valuenow', percentage);
        if (progressText) {
            progressText.textContent = `${percentage}%`;
        }
        
        // Update current step
        if (progress.currentStep && currentStep) {
            currentStep.textContent = progress.currentStep;
        }
        
        // Update estimated time
        if (progress.estimatedTimeRemaining && estimatedTime) {
            estimatedTime.textContent = progress.estimatedTimeRemaining;
        } else if (estimatedTime) {
            estimatedTime.textContent = 'Calculating...';
        }
        
        // Update elapsed time
        if (elapsedTime) {
            const startTime = parseInt(container.dataset.startTime);
            const elapsed = Math.floor((Date.now() - startTime) / 1000);
            elapsedTime.textContent = this.formatElapsedTime(elapsed);
        }
        
        // Update progress bar color and animation based on status
        progressBar.className = 'progress-bar progress-bar-striped';
        if (progress.status === 'FAILED') {
            progressBar.classList.add('bg-danger');
            progressBar.classList.remove('progress-bar-animated');
        } else if (progress.status === 'COMPLETED') {
            progressBar.classList.add('bg-success');
            progressBar.classList.remove('progress-bar-animated');
        } else if (progress.status === 'CANCELLED') {
            progressBar.classList.add('bg-warning');
            progressBar.classList.remove('progress-bar-animated');
        } else {
            progressBar.classList.add('bg-primary', 'progress-bar-animated');
        }
        
        // Update card border color based on status
        const card = container.querySelector('.card');
        card.className = 'card';
        if (progress.status === 'FAILED') {
            card.classList.add('border-danger');
        } else if (progress.status === 'COMPLETED') {
            card.classList.add('border-success');
        } else if (progress.status === 'CANCELLED') {
            card.classList.add('border-warning');
        } else {
            card.classList.add('border-primary');
        }
    }
    
    formatElapsedTime(seconds) {
        if (seconds < 60) {
            return `${seconds}s`;
        } else if (seconds < 3600) {
            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = seconds % 60;
            return `${minutes}m ${remainingSeconds}s`;
        } else {
            const hours = Math.floor(seconds / 3600);
            const minutes = Math.floor((seconds % 3600) / 60);
            return `${hours}h ${minutes}m`;
        }
    }
    
    async handleExportComplete(exportId) {
        console.log('Export completed:', exportId);
        
        const exportInfo = this.activeExports.get(exportId);
        if (!exportInfo) return;
        
        // Stop progress polling
        if (exportInfo.intervalId) {
            clearInterval(exportInfo.intervalId);
        }
        
        // Update UI
        const container = exportInfo.progressContainer;
        const actionsDiv = container.querySelector('.export-actions');
        const currentStep = container.querySelector('.current-step');
        
        if (currentStep) {
            currentStep.textContent = 'Export completed successfully!';
        }
        
        actionsDiv.innerHTML = `
            <button type="button" 
                    class="btn btn-sm btn-success" 
                    onclick="window.enhancedReportExporter.downloadExport('${exportId}')">
                <i class="bi bi-download"></i> Download Report
            </button>
            <button type="button" 
                    class="btn btn-sm btn-outline-info ms-2" 
                    onclick="window.enhancedReportExporter.getDownloadUrl('${exportId}')">
                <i class="bi bi-link-45deg"></i> Get Link
            </button>
            <button type="button" 
                    class="btn btn-sm btn-outline-secondary ms-2" 
                    onclick="window.enhancedReportExporter.cleanupExport('${exportId}')">
                <i class="bi bi-trash"></i> Clean Up
            </button>
        `;
        
        // Re-enable original button
        exportInfo.button.disabled = false;
        exportInfo.button.innerHTML = exportInfo.button.dataset.originalText || 'Export';
        
        // Show success notification with download option
        this.showNotification('Export completed successfully! Click Download to get your report.', 'success');
        
        // Auto-download if user preference is set
        if (this.shouldAutoDownload()) {
            setTimeout(() => this.downloadExport(exportId), 1000);
        }
    }
    
    shouldAutoDownload() {
        // Check user preference from localStorage
        return localStorage.getItem('autoDownloadReports') === 'true';
    }
    
    async getDownloadUrl(exportId) {
        try {
            const response = await fetch(`/api/reports/async/download-url/${exportId}`);
            
            if (!response.ok) {
                throw new Error(`Failed to get download URL: ${response.status}`);
            }
            
            const downloadUrl = await response.text();
            
            // Copy to clipboard and show notification
            if (navigator.clipboard) {
                await navigator.clipboard.writeText(downloadUrl);
                this.showNotification('Download URL copied to clipboard!', 'success');
            } else {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = downloadUrl;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                this.showNotification('Download URL copied to clipboard!', 'success');
            }
            
        } catch (error) {
            console.error('Failed to get download URL:', error);
            this.showNotification('Failed to get download URL: ' + error.message, 'error');
        }
    }
    
    handleExportError(exportId, errorMessage) {
        console.error('Export failed:', exportId, errorMessage);
        
        const exportInfo = this.activeExports.get(exportId);
        if (!exportInfo) return;
        
        // Stop progress polling
        if (exportInfo.intervalId) {
            clearInterval(exportInfo.intervalId);
        }
        
        // Update UI
        const container = exportInfo.progressContainer;
        const actionsDiv = container.querySelector('.export-actions');
        const currentStep = container.querySelector('.current-step');
        
        if (currentStep) {
            currentStep.textContent = 'Export failed';
        }
        
        // Categorize error for better user guidance
        const errorCategory = this.categorizeError(errorMessage);
        const userFriendlyMessage = this.getUserFriendlyErrorMessage(errorMessage, errorCategory);
        const troubleshootingTips = this.getTroubleshootingTips(errorCategory);
        
        actionsDiv.innerHTML = `
            <div class="alert alert-danger alert-sm mb-3">
                <div class="d-flex align-items-start">
                    <i class="bi bi-exclamation-triangle me-2 mt-1"></i>
                    <div class="flex-grow-1">
                        <strong>Export Failed:</strong> ${userFriendlyMessage}
                        ${troubleshootingTips ? `<br><small class="text-muted mt-1">${troubleshootingTips}</small>` : ''}
                    </div>
                </div>
            </div>
            <div class="btn-group" role="group">
                <button type="button" 
                        class="btn btn-sm btn-warning" 
                        data-retry-export 
                        data-export-id="${exportId}">
                    <i class="bi bi-arrow-clockwise"></i> Retry Export
                </button>
                <button type="button" 
                        class="btn btn-sm btn-outline-info" 
                        onclick="window.enhancedReportExporter.showErrorDetails('${exportId}', '${errorMessage.replace(/'/g, "\\'")}')">
                    <i class="bi bi-info-circle"></i> Details
                </button>
                <button type="button" 
                        class="btn btn-sm btn-outline-secondary" 
                        onclick="window.enhancedReportExporter.cleanupExport('${exportId}')">
                    <i class="bi bi-trash"></i> Clean Up
                </button>
            </div>
        `;
        
        // Re-enable original button
        exportInfo.button.disabled = false;
        exportInfo.button.innerHTML = exportInfo.button.dataset.originalText || 'Export';
        
        // Show error notification
        this.showNotification('Export failed: ' + userFriendlyMessage, 'error');
        
        // Log detailed error for debugging
        console.error('Detailed export error:', {
            exportId,
            errorMessage,
            errorCategory,
            timestamp: new Date().toISOString(),
            reportType: exportInfo.reportType,
            format: exportInfo.format
        });
    }
    
    categorizeError(errorMessage) {
        const message = errorMessage.toLowerCase();
        
        if (message.includes('permission') || message.includes('access denied') || message.includes('403')) {
            return 'PERMISSION';
        } else if (message.includes('authentication') || message.includes('401')) {
            return 'AUTHENTICATION';
        } else if (message.includes('timeout') || message.includes('took too long')) {
            return 'TIMEOUT';
        } else if (message.includes('memory') || message.includes('out of space')) {
            return 'RESOURCE';
        } else if (message.includes('network') || message.includes('connection')) {
            return 'NETWORK';
        } else if (message.includes('data') || message.includes('query')) {
            return 'DATA';
        } else {
            return 'UNKNOWN';
        }
    }
    
    getUserFriendlyErrorMessage(errorMessage, category) {
        switch (category) {
            case 'PERMISSION':
                return 'You do not have permission to export reports. Please contact your administrator.';
            case 'AUTHENTICATION':
                return 'Your session has expired. Please log in again and retry.';
            case 'TIMEOUT':
                return 'The export took too long to complete. Try reducing the date range or filters.';
            case 'RESOURCE':
                return 'The server is currently overloaded. Please try again in a few minutes.';
            case 'NETWORK':
                return 'Network connection issue. Please check your connection and retry.';
            case 'DATA':
                return 'There was an issue processing the report data. Please check your filters and try again.';
            default:
                return errorMessage.length > 100 ? 'An unexpected error occurred during export.' : errorMessage;
        }
    }
    
    getTroubleshootingTips(category) {
        switch (category) {
            case 'PERMISSION':
                return 'Contact your system administrator to request report export permissions.';
            case 'AUTHENTICATION':
                return 'Refresh the page and log in again before retrying the export.';
            case 'TIMEOUT':
                return 'Try exporting a smaller date range or removing some filters to reduce processing time.';
            case 'RESOURCE':
                return 'The server may be busy. Wait a few minutes before trying again.';
            case 'NETWORK':
                return 'Check your internet connection and ensure the server is accessible.';
            case 'DATA':
                return 'Verify your date range and filter selections are valid.';
            default:
                return 'If the problem persists, please contact technical support.';
        }
    }
    
    showErrorDetails(exportId, errorMessage) {
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.innerHTML = `
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <i class="bi bi-exclamation-triangle text-danger"></i>
                            Export Error Details
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label"><strong>Export ID:</strong></label>
                            <div class="form-control-plaintext">${exportId}</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label"><strong>Error Message:</strong></label>
                            <div class="form-control-plaintext text-danger">${errorMessage}</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label"><strong>Timestamp:</strong></label>
                            <div class="form-control-plaintext">${new Date().toLocaleString()}</div>
                        </div>
                        <div class="alert alert-info">
                            <i class="bi bi-info-circle"></i>
                            <strong>Next Steps:</strong>
                            <ul class="mb-0 mt-2">
                                <li>Try clicking "Retry Export" to attempt the export again</li>
                                <li>Check your internet connection and try again</li>
                                <li>If the problem persists, contact technical support with the Export ID above</li>
                            </ul>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="btn btn-primary" onclick="window.enhancedReportExporter.copyErrorDetails('${exportId}', '${errorMessage.replace(/'/g, "\\'")}')">
                            <i class="bi bi-clipboard"></i> Copy Details
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
        
        // Clean up modal after hiding
        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
        });
    }
    
    async copyErrorDetails(exportId, errorMessage) {
        const details = `Export Error Details:
Export ID: ${exportId}
Error: ${errorMessage}
Timestamp: ${new Date().toISOString()}
User Agent: ${navigator.userAgent}
URL: ${window.location.href}`;
        
        try {
            if (navigator.clipboard) {
                await navigator.clipboard.writeText(details);
            } else {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = details;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
            }
            this.showNotification('Error details copied to clipboard', 'success');
        } catch (error) {
            console.error('Failed to copy error details:', error);
            this.showNotification('Failed to copy error details', 'error');
        }
    }
    
    handleExportCancelled(exportId) {
        console.log('Export cancelled:', exportId);
        
        const exportInfo = this.activeExports.get(exportId);
        if (!exportInfo) return;
        
        // Stop progress polling
        if (exportInfo.intervalId) {
            clearInterval(exportInfo.intervalId);
        }
        
        // Remove progress UI
        exportInfo.progressContainer.remove();
        
        // Re-enable original button
        exportInfo.button.disabled = false;
        exportInfo.button.innerHTML = exportInfo.button.dataset.originalText || 'Export';
        
        // Clean up
        this.activeExports.delete(exportId);
        
        // Show cancellation notification
        this.showNotification('Export cancelled', 'info');
    }
    
    async retryExport(exportId) {
        console.log('Retrying export:', exportId);
        
        try {
            const response = await fetch(`/api/reports/async/retry/${exportId}`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error(`Retry failed: ${response.status}`);
            }
            
            // Resume progress tracking
            const exportInfo = this.activeExports.get(exportId);
            if (exportInfo) {
                // Start progress polling again
                const intervalId = setInterval(async () => {
                    try {
                        await this.checkExportProgress(exportId);
                    } catch (error) {
                        console.error('Error checking export progress:', error);
                        this.handleExportError(exportId, error.message);
                    }
                }, this.progressCheckInterval);
                
                exportInfo.intervalId = intervalId;
                
                // Update UI
                const container = exportInfo.progressContainer;
                const actionsDiv = container.querySelector('.export-actions');
                actionsDiv.innerHTML = `
                    <button type="button" 
                            class="btn btn-sm btn-outline-danger" 
                            data-cancel-export 
                            data-export-id="${exportId}">
                        <i class="bi bi-x-circle"></i> Cancel
                    </button>
                `;
            }
            
            this.showNotification('Export retry started', 'info');
            
        } catch (error) {
            console.error('Failed to retry export:', error);
            this.showNotification('Failed to retry export: ' + error.message, 'error');
        }
    }
    
    async cancelExport(exportId) {
        console.log('Cancelling export:', exportId);
        
        try {
            const response = await fetch(`/api/reports/async/cancel/${exportId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                this.handleExportCancelled(exportId);
            } else {
                throw new Error(`Cancel failed: ${response.status}`);
            }
            
        } catch (error) {
            console.error('Failed to cancel export:', error);
            this.showNotification('Failed to cancel export: ' + error.message, 'error');
        }
    }
    
    async downloadExport(exportId) {
        console.log('Downloading export:', exportId);
        
        try {
            const response = await fetch(`/api/reports/async/download/${exportId}`);
            
            if (!response.ok) {
                throw new Error(`Download failed: ${response.status}`);
            }
            
            // Get filename from Content-Disposition header
            const contentDisposition = response.headers.get('Content-Disposition');
            let filename = 'export.pdf';
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="(.+)"/);
                if (filenameMatch) {
                    filename = filenameMatch[1];
                }
            }
            
            // Create download link
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
            
            this.showNotification('Download started', 'success');
            
        } catch (error) {
            console.error('Failed to download export:', error);
            this.showNotification('Failed to download export: ' + error.message, 'error');
        }
    }
    
    async cleanupExport(exportId) {
        console.log('Cleaning up export:', exportId);
        
        try {
            const response = await fetch(`/api/reports/async/cleanup/${exportId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                const exportInfo = this.activeExports.get(exportId);
                if (exportInfo) {
                    exportInfo.progressContainer.remove();
                    this.activeExports.delete(exportId);
                }
                
                this.showNotification('Export cleaned up', 'info');
            } else {
                throw new Error(`Cleanup failed: ${response.status}`);
            }
            
        } catch (error) {
            console.error('Failed to cleanup export:', error);
            this.showNotification('Failed to cleanup export: ' + error.message, 'error');
        }
    }
    
    showExportError(button, message) {
        // Create error alert
        const errorAlert = document.createElement('div');
        errorAlert.className = 'alert alert-danger alert-dismissible fade show mt-2';
        errorAlert.innerHTML = `
            <i class="bi bi-exclamation-triangle"></i>
            <strong>Export Failed:</strong> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        button.parentNode.insertBefore(errorAlert, button.nextSibling);
        
        // Auto-dismiss after 10 seconds
        setTimeout(() => {
            if (errorAlert.parentNode) {
                errorAlert.remove();
            }
        }, 10000);
    }
    
    showNotification(message, type = 'info') {
        // Create notification toast
        const toastContainer = document.getElementById('toast-container') || this.createToastContainer();
        
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${this.getBootstrapColor(type)} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <i class="bi bi-${this.getIcon(type)}"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        toastContainer.appendChild(toast);
        
        // Show toast
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        // Remove from DOM after hiding
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
    
    createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1055';
        document.body.appendChild(container);
        return container;
    }
    
    getBootstrapColor(type) {
        switch (type) {
            case 'success': return 'success';
            case 'error': return 'danger';
            case 'warning': return 'warning';
            case 'info': return 'info';
            default: return 'secondary';
        }
    }
    
    getIcon(type) {
        switch (type) {
            case 'success': return 'check-circle';
            case 'error': return 'exclamation-triangle';
            case 'warning': return 'exclamation-triangle';
            case 'info': return 'info-circle';
            default: return 'info-circle';
        }
    }
}

// Initialize enhanced report exporter
window.enhancedReportExporter = new EnhancedReportExporter();

// Export functions for global access
window.startAsyncExport = function(reportType, format) {
    const button = document.createElement('button');
    button.dataset.reportType = reportType;
    button.dataset.format = format;
    button.dataset.exportAsync = 'true';
    
    window.enhancedReportExporter.handleAsyncExportClick(button);
};

console.log('Enhanced Report Export JavaScript loaded');