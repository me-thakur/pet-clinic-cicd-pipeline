/**
 * Unit tests for Enhanced Report Export functionality
 * Tests the frontend report export interface with progress indicators,
 * error handling, and admin permission checks
 */

describe('Enhanced Report Export', function() {
    let enhancedReportExporter;
    let mockFetch;
    
    beforeEach(function() {
        // Reset DOM
        document.body.innerHTML = '';
        
        // Create mock elements
        const mockElements = `
            <meta name="_csrf" content="test-csrf-token">
            <input type="date" id="startDate" value="2024-01-01">
            <input type="date" id="endDate" value="2024-12-31">
            <select id="veterinarianId"><option value="1" selected>Dr. Smith</option></select>
            <select id="species"><option value="Dog" selected>Dog</option></select>
            <div id="toast-container"></div>
        `;
        document.body.innerHTML = mockElements;
        
        // Mock fetch
        mockFetch = jest.fn();
        global.fetch = mockFetch;
        
        // Mock bootstrap
        global.bootstrap = {
            Toast: jest.fn().mockImplementation(() => ({
                show: jest.fn(),
                hide: jest.fn()
            })),
            Modal: jest.fn().mockImplementation(() => ({
                show: jest.fn(),
                hide: jest.fn()
            }))
        };
        
        // Load the enhanced report exporter
        require('../../../main/resources/static/js/enhanced-report-export.js');
        enhancedReportExporter = window.enhancedReportExporter;
    });
    
    afterEach(function() {
        jest.clearAllMocks();
    });
    
    describe('Initialization', function() {
        test('should initialize with default values', function() {
            expect(enhancedReportExporter).toBeDefined();
            expect(enhancedReportExporter.activeExports).toBeInstanceOf(Map);
            expect(enhancedReportExporter.progressCheckInterval).toBe(2000);
            expect(enhancedReportExporter.maxRetries).toBe(3);
        });
        
        test('should check user permissions on init', async function() {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    roles: ['ADMIN'],
                    isAdmin: true,
                    canExportReports: true
                })
            });
            
            await enhancedReportExporter.checkUserPermissions();
            
            expect(mockFetch).toHaveBeenCalledWith('/api/user/permissions');
            expect(enhancedReportExporter.isAdmin).toBe(true);
        });
    });
    
    describe('Permission Checking', function() {
        test('should correctly identify admin users', async function() {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    roles: ['ADMIN'],
                    isAdmin: true,
                    canExportReports: true
                })
            });
            
            const hasPermission = await enhancedReportExporter.checkAdminPermissions();
            expect(hasPermission).toBe(true);
        });
        
        test('should correctly identify non-admin users', async function() {
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    roles: ['USER'],
                    isAdmin: false,
                    canExportReports: false
                })
            });
            
            const hasPermission = await enhancedReportExporter.checkAdminPermissions();
            expect(hasPermission).toBe(false);
        });
        
        test('should handle permission check failures gracefully', async function() {
            mockFetch.mockRejectedValueOnce(new Error('Network error'));
            
            const hasPermission = await enhancedReportExporter.checkAdminPermissions();
            expect(hasPermission).toBe(false);
        });
    });
    
    describe('Parameter Collection', function() {
        test('should collect visit report parameters correctly', function() {
            const params = enhancedReportExporter.collectExportParameters('visits');
            
            expect(params).toEqual({
                startDate: '2024-01-01',
                endDate: '2024-12-31',
                veterinarianId: '1',
                species: 'Dog'
            });
        });
        
        test('should collect revenue report parameters correctly', function() {
            // Add revenue-specific elements
            const includeTrendsCheckbox = document.createElement('input');
            includeTrendsCheckbox.type = 'checkbox';
            includeTrendsCheckbox.id = 'includeTrends';
            includeTrendsCheckbox.checked = true;
            document.body.appendChild(includeTrendsCheckbox);
            
            const params = enhancedReportExporter.collectExportParameters('revenue');
            
            expect(params.includeTrends).toBe(true);
            expect(params.startDate).toBe('2024-01-01');
            expect(params.endDate).toBe('2024-12-31');
        });
        
        test('should handle current report type detection', function() {
            // Mock window.location
            Object.defineProperty(window, 'location', {
                value: { pathname: '/dashboard/visits' },
                writable: true
            });
            
            const reportType = enhancedReportExporter.getCurrentReportType();
            expect(reportType).toBe('visits');
        });
    });
    
    describe('Export Process', function() {
        test('should start export with admin permissions', async function() {
            // Mock admin permissions
            enhancedReportExporter.isAdmin = true;
            
            // Mock successful export start
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    exportId: 'test-export-123',
                    status: 'STARTED',
                    progressPercentage: 0
                })
            });
            
            const button = document.createElement('button');
            button.dataset.reportType = 'visits';
            button.dataset.format = 'pdf';
            document.body.appendChild(button);
            
            await enhancedReportExporter.handleAsyncExportClick(button);
            
            expect(mockFetch).toHaveBeenCalledWith(
                '/api/reports/async/export/visits',
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        'Content-Type': 'application/x-www-form-urlencoded'
                    })
                })
            );
        });
        
        test('should prevent export without admin permissions', async function() {
            // Mock non-admin permissions
            enhancedReportExporter.isAdmin = false;
            
            const button = document.createElement('button');
            button.dataset.reportType = 'visits';
            button.dataset.format = 'pdf';
            document.body.appendChild(button);
            
            await enhancedReportExporter.handleAsyncExportClick(button);
            
            // Should not make API call
            expect(mockFetch).not.toHaveBeenCalled();
            
            // Should show permission error
            const errorAlert = document.querySelector('.alert-warning');
            expect(errorAlert).toBeTruthy();
            expect(errorAlert.textContent).toContain('Access Denied');
        });
    });
    
    describe('Progress Tracking', function() {
        test('should create progress UI correctly', function() {
            const button = document.createElement('button');
            button.innerHTML = 'Export';
            document.body.appendChild(button);
            
            const progressContainer = enhancedReportExporter.createProgressUI(
                'test-export-123', 
                'visits', 
                'pdf', 
                button
            );
            
            expect(progressContainer).toBeTruthy();
            expect(progressContainer.querySelector('.progress-bar')).toBeTruthy();
            expect(progressContainer.querySelector('.current-step')).toBeTruthy();
            expect(progressContainer.querySelector('.estimated-time')).toBeTruthy();
        });
        
        test('should update progress UI correctly', function() {
            const button = document.createElement('button');
            document.body.appendChild(button);
            
            const progressContainer = enhancedReportExporter.createProgressUI(
                'test-export-123', 
                'visits', 
                'pdf', 
                button
            );
            
            enhancedReportExporter.activeExports.set('test-export-123', {
                progressContainer: progressContainer
            });
            
            const mockProgress = {
                progressPercentage: 75,
                currentStep: 'Generating PDF...',
                estimatedTimeRemaining: '30 seconds',
                status: 'IN_PROGRESS'
            };
            
            enhancedReportExporter.updateProgressUI('test-export-123', mockProgress);
            
            const progressBar = progressContainer.querySelector('.progress-bar');
            expect(progressBar.style.width).toBe('75%');
            expect(progressBar.getAttribute('aria-valuenow')).toBe('75');
            
            const currentStep = progressContainer.querySelector('.current-step');
            expect(currentStep.textContent).toBe('Generating PDF...');
        });
    });
    
    describe('Error Handling', function() {
        test('should categorize errors correctly', function() {
            expect(enhancedReportExporter.categorizeError('Access denied')).toBe('PERMISSION');
            expect(enhancedReportExporter.categorizeError('Authentication failed')).toBe('AUTHENTICATION');
            expect(enhancedReportExporter.categorizeError('Request timeout')).toBe('TIMEOUT');
            expect(enhancedReportExporter.categorizeError('Network connection failed')).toBe('NETWORK');
            expect(enhancedReportExporter.categorizeError('Unknown error')).toBe('UNKNOWN');
        });
        
        test('should provide user-friendly error messages', function() {
            const permissionMsg = enhancedReportExporter.getUserFriendlyErrorMessage(
                'Access denied', 'PERMISSION'
            );
            expect(permissionMsg).toContain('permission');
            
            const timeoutMsg = enhancedReportExporter.getUserFriendlyErrorMessage(
                'Timeout', 'TIMEOUT'
            );
            expect(timeoutMsg).toContain('too long');
        });
        
        test('should provide troubleshooting tips', function() {
            const tips = enhancedReportExporter.getTroubleshootingTips('PERMISSION');
            expect(tips).toContain('administrator');
            
            const networkTips = enhancedReportExporter.getTroubleshootingTips('NETWORK');
            expect(networkTips).toContain('connection');
        });
    });
    
    describe('Utility Functions', function() {
        test('should format elapsed time correctly', function() {
            expect(enhancedReportExporter.formatElapsedTime(30)).toBe('30s');
            expect(enhancedReportExporter.formatElapsedTime(90)).toBe('1m 30s');
            expect(enhancedReportExporter.formatElapsedTime(3661)).toBe('1h 1m');
        });
        
        test('should format report type names correctly', function() {
            expect(enhancedReportExporter.formatReportTypeName('visits')).toBe('Visit Statistics');
            expect(enhancedReportExporter.formatReportTypeName('revenue')).toBe('Revenue Analysis');
            expect(enhancedReportExporter.formatReportTypeName('dashboard')).toBe('Dashboard Summary');
            expect(enhancedReportExporter.formatReportTypeName('custom')).toBe('Custom');
        });
    });
    
    describe('Integration Tests', function() {
        test('should handle complete export workflow', async function() {
            // Mock admin permissions
            enhancedReportExporter.isAdmin = true;
            
            // Mock export start
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    exportId: 'test-export-123',
                    status: 'STARTED'
                })
            });
            
            // Mock progress updates
            mockFetch.mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve({
                    status: 'COMPLETED',
                    progressPercentage: 100
                })
            });
            
            const button = document.createElement('button');
            button.dataset.reportType = 'visits';
            button.dataset.format = 'pdf';
            button.innerHTML = 'Export';
            document.body.appendChild(button);
            
            // Start export
            await enhancedReportExporter.handleAsyncExportClick(button);
            
            // Verify export was started
            expect(enhancedReportExporter.activeExports.has('test-export-123')).toBe(true);
            
            // Simulate progress check
            await enhancedReportExporter.checkExportProgress('test-export-123');
            
            // Verify completion was handled
            const downloadButton = document.querySelector('[onclick*="downloadExport"]');
            expect(downloadButton).toBeTruthy();
        });
    });
});

// Test helper functions
function createMockButton(reportType, format) {
    const button = document.createElement('button');
    button.dataset.reportType = reportType;
    button.dataset.format = format;
    button.dataset.exportAsync = 'true';
    button.innerHTML = `Export ${reportType} ${format}`;
    return button;
}

function createMockProgressResponse(status, percentage = 0) {
    return {
        status: status,
        progressPercentage: percentage,
        currentStep: `Processing ${status.toLowerCase()}...`,
        estimatedTimeRemaining: '1 minute'
    };
}