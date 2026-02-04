/**
 * Integration tests for TableApiClient with EnhancedTable
 * Tests the complete integration between API client and table components
 * 
 * Requirements: 2.5, 5.3
 */

const { JSDOM } = require('jsdom');
const fs = require('fs');
const path = require('path');

// Setup DOM environment
const dom = new JSDOM(`
<!DOCTYPE html>
<html>
<head><title>Test</title></head>
<body>
    <table id="test-table" class="table-sortable">
        <thead>
            <tr>
                <th class="sortable-header" data-column="id">ID</th>
                <th class="sortable-header" data-column="name">Name</th>
                <th class="sortable-header" data-column="status">Status</th>
            </tr>
        </thead>
        <tbody>
            <tr data-id="1">
                <td>1</td>
                <td>Test Item 1</td>
                <td>ACTIVE</td>
            </tr>
            <tr data-id="2">
                <td>2</td>
                <td>Test Item 2</td>
                <td>INACTIVE</td>
            </tr>
        </tbody>
    </table>
</body>
</html>
`, { 
    url: 'http://localhost',
    pretendToBeVisual: true,
    resources: 'usable'
});

global.window = dom.window;
global.document = dom.window.document;
global.navigator = dom.window.navigator;
global.fetch = jest.fn();

// Load the TableApiClient
const tableApiClientCode = fs.readFileSync(
    path.join(__dirname, 'src/main/resources/static/js/table-api-client.js'), 
    'utf8'
);
eval(tableApiClientCode);

describe('TableApiClient Integration Tests', () => {
    let apiClient;
    let mockFetch;

    beforeEach(() => {
        apiClient = new window.TableApiClient();
        mockFetch = global.fetch;
        mockFetch.mockClear();
    });

    afterEach(() => {
        apiClient.abortAllRequests();
        apiClient.clearCache();
    });

    describe('API Client Initialization', () => {
        test('should create instance with default configuration', () => {
            expect(apiClient).toBeDefined();
            expect(apiClient.getConfig().BASE_URL).toBe('/api/v1');
            expect(apiClient.getConfig().MAX_RETRIES).toBe(3);
        });

        test('should create instance with custom configuration', () => {
            const customClient = new window.TableApiClient({
                MAX_RETRIES: 5,
                BASE_URL: '/custom/api'
            });
            
            expect(customClient.getConfig().MAX_RETRIES).toBe(5);
            expect(customClient.getConfig().BASE_URL).toBe('/custom/api');
        });
    });

    describe('Entity Requests', () => {
        test('should make GET request for entities with correct parameters', async () => {
            const mockResponse = {
                content: [
                    { id: 1, name: 'Visit 1', status: 'SCHEDULED' },
                    { id: 2, name: 'Visit 2', status: 'COMPLETED' }
                ],
                page: { number: 0, size: 10, totalElements: 2 },
                sortMetadata: { column: 'id', direction: 'asc', isGlobal: true },
                activeFilters: []
            };

            mockFetch.mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: () => Promise.resolve(mockResponse),
                headers: new Map([['content-type', 'application/json']])
            });

            const result = await apiClient.getEntities('visits', {
                page: 0,
                size: 10,
                sortBy: 'id',
                sortDir: 'asc',
                filters: { status: 'SCHEDULED' }
            });

            expect(mockFetch).toHaveBeenCalledTimes(1);
            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/v1/visits'),
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    })
                })
            );

            expect(result.content).toHaveLength(2);
            expect(result.page.totalElements).toBe(2);
        });

        test('should handle API errors gracefully', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 500,
                json: () => Promise.resolve({ message: 'Internal server error' }),
                headers: new Map([['content-type', 'application/json']])
            });

            await expect(apiClient.getEntities('visits')).rejects.toThrow(window.ApiError);
        });
    });

    describe('Bulk Operations', () => {
        test('should create bulk delete request with correct format', () => {
            const selectedIds = ['1', '2', '3'];
            const filters = [
                { column: 'status', value: 'ACTIVE', operator: 'equals' }
            ];

            const request = apiClient.createBulkDeleteRequest(
                selectedIds, 
                false, 
                filters, 
                'visits'
            );

            expect(request.selectedIds).toEqual([1, 2, 3]);
            expect(request.selectAll).toBe(false);
            expect(request.entityType).toBe('visits');
            expect(request.currentFilters).toHaveLength(1);
            expect(request.currentFilters[0].field).toBe('status');
            expect(request.currentFilters[0].value).toBe('ACTIVE');
            expect(request.currentFilters[0].entityType).toBe('visits');
        });

        test('should execute bulk delete with correct API call', async () => {
            const mockResponse = {
                success: true,
                deletedCount: 2,
                failedCount: 0,
                totalRequested: 2,
                errors: [],
                warnings: []
            };

            mockFetch.mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: () => Promise.resolve(mockResponse),
                headers: new Map([['content-type', 'application/json']])
            });

            const request = {
                selectedIds: [1, 2],
                selectAll: false,
                currentFilters: [],
                entityType: 'visits'
            };

            const result = await apiClient.bulkDelete('visits', request);

            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/v1/visits/bulk'),
                expect.objectContaining({
                    method: 'DELETE',
                    headers: expect.objectContaining({
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    }),
                    body: JSON.stringify(request)
                })
            );

            expect(result.success).toBe(true);
            expect(result.deletedCount).toBe(2);
        });
    });

    describe('Filter Values', () => {
        test('should fetch filter values for column', async () => {
            const mockValues = ['SCHEDULED', 'COMPLETED', 'CANCELLED'];

            mockFetch.mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: () => Promise.resolve(mockValues),
                headers: new Map([['content-type', 'application/json']])
            });

            const result = await apiClient.getFilterValues('visits', 'status');

            expect(mockFetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/v1/visits/filter-values/status'),
                expect.objectContaining({
                    method: 'GET'
                })
            );

            expect(result).toEqual(mockValues);
        });
    });

    describe('Caching', () => {
        test('should cache GET requests', async () => {
            const mockResponse = { content: [], page: { totalElements: 0 } };

            mockFetch.mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve(mockResponse),
                headers: new Map([['content-type', 'application/json']])
            });

            // First request
            await apiClient.getEntities('visits', { page: 0, size: 10 });
            expect(mockFetch).toHaveBeenCalledTimes(1);

            // Second identical request should use cache
            await apiClient.getEntities('visits', { page: 0, size: 10 });
            expect(mockFetch).toHaveBeenCalledTimes(1); // Still only 1 call
        });

        test('should clear cache for entity type', async () => {
            const mockResponse = { content: [], page: { totalElements: 0 } };

            mockFetch.mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve(mockResponse),
                headers: new Map([['content-type', 'application/json']])
            });

            // Make request to cache it
            await apiClient.getEntities('visits', { page: 0, size: 10 });
            expect(mockFetch).toHaveBeenCalledTimes(1);

            // Clear cache for visits
            apiClient.clearEntityCache('visits');

            // Next request should hit API again
            await apiClient.getEntities('visits', { page: 0, size: 10 });
            expect(mockFetch).toHaveBeenCalledTimes(2);
        });
    });

    describe('Error Handling and Retry Logic', () => {
        test('should retry on network errors', async () => {
            // First two calls fail, third succeeds
            mockFetch
                .mockRejectedValueOnce(new TypeError('Network error'))
                .mockRejectedValueOnce(new TypeError('Network error'))
                .mockResolvedValueOnce({
                    ok: true,
                    status: 200,
                    json: () => Promise.resolve({ content: [] }),
                    headers: new Map([['content-type', 'application/json']])
                });

            const result = await apiClient.getEntities('visits');

            expect(mockFetch).toHaveBeenCalledTimes(3);
            expect(result).toBeDefined();
        });

        test('should not retry on client errors', async () => {
            mockFetch.mockResolvedValueOnce({
                ok: false,
                status: 400,
                json: () => Promise.resolve({ message: 'Bad request' }),
                headers: new Map([['content-type', 'application/json']])
            });

            await expect(apiClient.getEntities('visits')).rejects.toThrow(window.ApiError);
            expect(mockFetch).toHaveBeenCalledTimes(1); // No retry for 400 errors
        });

        test('should abort requests on timeout', async () => {
            const shortTimeoutClient = new window.TableApiClient({ DEFAULT_TIMEOUT: 100 });

            mockFetch.mockImplementation(() => 
                new Promise(resolve => setTimeout(resolve, 200))
            );

            await expect(shortTimeoutClient.getEntities('visits')).rejects.toThrow('timeout');
        });
    });

    describe('Request Transformation', () => {
        test('should transform frontend filters to backend format', () => {
            const frontendFilters = [
                { column: 'status', value: 'ACTIVE', operator: 'equals' },
                { column: 'name', value: 'John', operator: 'contains' },
                { column: 'date', value: null } // Should be filtered out
            ];

            const backendFilters = apiClient.transformFiltersToBackend(frontendFilters, 'visits');

            expect(backendFilters).toEqual({
                status: 'ACTIVE',
                name: 'John'
            });
            expect(backendFilters.date).toBeUndefined();
        });

        test('should build URLs with query parameters correctly', () => {
            const url = apiClient.buildUrl('/api/v1/visits', {
                page: 0,
                size: 10,
                sortBy: 'id',
                sortDir: 'asc',
                status: 'ACTIVE',
                emptyParam: '',
                nullParam: null
            });

            expect(url).toContain('page=0');
            expect(url).toContain('size=10');
            expect(url).toContain('sortBy=id');
            expect(url).toContain('sortDir=asc');
            expect(url).toContain('status=ACTIVE');
            expect(url).not.toContain('emptyParam');
            expect(url).not.toContain('nullParam');
        });
    });
});

// Export for potential use in other test files
module.exports = {
    setupTableApiClientTests: () => {
        // Setup function for other test files
        return { apiClient: new window.TableApiClient() };
    }
};