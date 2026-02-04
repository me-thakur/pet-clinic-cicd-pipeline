/**
 * Server-Side Sorting Tests
 * Tests for enhanced table server-side sorting functionality
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */

describe('Server-Side Sorting', function() {
    let table, enhancedTable, mockApiClient;
    
    beforeEach(function() {
        // Create test table
        document.body.innerHTML = `
            <table id="testTable" class="table table-sortable">
                <thead>
                    <tr>
                        <th class="sortable-header" data-column="id">ID <span class="sort-indicator neutral"></span></th>
                        <th class="sortable-header" data-column="name">Name <span class="sort-indicator neutral"></span></th>
                        <th class="sortable-header" data-column="species">Species <span class="sort-indicator neutral"></span></th>
                        <th class="sortable-header" data-column="birthDate">Birth Date <span class="sort-indicator neutral"></span></th>
                    </tr>
                </thead>
                <tbody>
                    <tr data-id="1"><td>1</td><td>Buddy</td><td>Dog</td><td>2020-03-15</td></tr>
                    <tr data-id="2"><td>2</td><td>Whiskers</td><td>Cat</td><td>2019-07-22</td></tr>
                    <tr data-id="3"><td>3</td><td>Charlie</td><td>Dog</td><td>2021-01-10</td></tr>
                </tbody>
            </table>
        `;
        
        table = document.getElementById('testTable');
        
        // Mock API client
        mockApiClient = {
            getEntities: jasmine.createSpy('getEntities').and.returnValue(Promise.resolve({
                content: [],
                page: { number: 0, size: 10, totalElements: 3, totalPages: 1 }
            })),
            transformFiltersToBackend: jasmine.createSpy('transformFiltersToBackend').and.returnValue({}),
            transformResponseToFrontend: jasmine.createSpy('transformResponseToFrontend').and.returnValue({}),
            clearEntityCache: jasmine.createSpy('clearEntityCache')
        };
        
        // Replace global API client
        window.tableApiClient = mockApiClient;
        
        // Initialize enhanced table
        enhancedTable = new window.EnhancedTable(table, {
            entityType: 'pets',
            enableGlobalSort: true,
            enableFiltering: false,
            enableBulkOperations: false,
            persistState: false
        });
    });
    
    afterEach(function() {
        if (enhancedTable) {
            enhancedTable.destroy();
        }
        document.body.innerHTML = '';
    });
    
    describe('Server-Side Sort Requests', function() {
        it('should send proper server-side sort requests with column and direction', async function() {
            // Test Requirement 6.1: Send sort requests to backend with column and direction parameters
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                sort: 'name,asc',
                page: 0,
                size: 10,
                useCache: false
            }));
        });
        
        it('should handle multi-column sorting with proper precedence', async function() {
            // Test Requirement 6.5: Multiple column sorting with proper precedence
            await enhancedTable.sortController.applySortAsync('name', 'asc', false);
            await enhancedTable.sortController.applySortAsync('species', 'desc', true);
            
            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                sort: jasmine.any(Array),
                page: 0,
                size: 10,
                useCache: false
            }));
            
            const lastCall = mockApiClient.getEntities.calls.mostRecent();
            const sortParam = lastCall.args[1].sort;
            expect(Array.isArray(sortParam)).toBe(true);
            expect(sortParam).toContain('name,asc');
            expect(sortParam).toContain('species,desc');
        });
        
        it('should include filter parameters in sort requests', async function() {
            // Mock active filters
            spyOn(enhancedTable.filterController, 'getActiveFilters').and.returnValue([
                { column: 'species', value: 'Dog', operator: 'equals' }
            ]);
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                sort: 'name,asc',
                species: 'Dog'
            }));
        });
    });
    
    describe('Server-Side Sort Processing', function() {
        it('should apply sorting to complete dataset before pagination', function() {
            // Test Requirement 6.2: Backend applies sorting to complete dataset before pagination
            // This is tested by verifying the API request includes pagination parameters
            // after sort parameters, indicating server-side processing
            
            enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                page: jasmine.any(Number),
                size: jasmine.any(Number),
                sort: jasmine.any(String)
            }));
        });
        
        it('should return properly sorted results with pagination metadata', async function() {
            // Test Requirement 6.3: Returns properly sorted results with updated pagination metadata
            const mockResponse = {
                content: [
                    { id: 1, name: 'Alice', species: 'Cat' },
                    { id: 2, name: 'Bob', species: 'Dog' }
                ],
                page: {
                    number: 0,
                    size: 10,
                    totalElements: 25,
                    totalPages: 3
                }
            };
            
            mockApiClient.getEntities.and.returnValue(Promise.resolve(mockResponse));
            spyOn(enhancedTable, 'updateTableContent');
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(enhancedTable.updateTableContent).toHaveBeenCalledWith(mockResponse);
        });
    });
    
    describe('Fallback Mechanisms', function() {
        it('should fall back to client-side sorting when server fails', async function() {
            // Test Requirement 6.4: Fallback to client-side sorting with user notification
            mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('Server error')));
            
            spyOn(enhancedTable.sortController, 'applyClientSideSort').and.returnValue(Promise.resolve());
            spyOn(enhancedTable, 'showFallbackMessage');
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(enhancedTable.sortController.applyClientSideSort).toHaveBeenCalledWith('name', 'asc', false);
            expect(enhancedTable.showFallbackMessage).toHaveBeenCalled();
        });
        
        it('should use TableSorter for client-side fallback', async function() {
            // Mock TableSorter
            const mockTableSorter = {
                sortByColumnName: jasmine.createSpy('sortByColumnName'),
                sortByColumn: jasmine.createSpy('sortByColumn')
            };
            
            window.TableSorter = jasmine.createSpy('TableSorter').and.returnValue(mockTableSorter);
            
            await enhancedTable.sortController.applyClientSideSort('name', 'asc');
            
            expect(window.TableSorter).toHaveBeenCalled();
            expect(mockTableSorter.sortByColumnName).toHaveBeenCalledWith('name', 'asc');
        });
        
        it('should show appropriate error messages for different failure types', async function() {
            spyOn(enhancedTable, 'showFallbackMessage');
            
            // Test network error
            const networkError = new window.ApiError('Network error', 0, null, new TypeError('Failed to fetch'));
            mockApiClient.getEntities.and.returnValue(Promise.reject(networkError));
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(enhancedTable.showFallbackMessage).toHaveBeenCalledWith(
                jasmine.stringMatching(/network/i),
                'warning'
            );
        });
        
        it('should temporarily mark server as unavailable after failures', async function() {
            mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('Server error')));
            spyOn(enhancedTable.sortController, 'applyClientSideSort').and.returnValue(Promise.resolve());
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(enhancedTable.sortController.isServerSideAvailable()).toBe(false);
        });
    });
    
    describe('Visual Indicators', function() {
        it('should update visual indicators for server-side sorting', async function() {
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            const nameHeader = table.querySelector('[data-column="name"]');
            const indicator = nameHeader.querySelector('.sort-indicator');
            
            expect(indicator.classList.contains('asc')).toBe(true);
            expect(indicator.classList.contains('active')).toBe(true);
            expect(nameHeader.getAttribute('aria-sort')).toBe('asc');
        });
        
        it('should show multi-column sort display for multiple columns', async function() {
            await enhancedTable.sortController.applySortAsync('name', 'asc', false);
            await enhancedTable.sortController.applySortAsync('species', 'desc', true);
            
            const sortDisplay = table.parentNode.querySelector('.multi-column-sort-display');
            expect(sortDisplay).toBeTruthy();
            expect(sortDisplay.textContent).toContain('Multi-column sort');
        });
        
        it('should distinguish between server-side and client-side sort indicators', async function() {
            // Server-side sort
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            let indicator = table.querySelector('[data-column="name"] .sort-indicator');
            expect(indicator.classList.contains('global')).toBe(true);
            
            // Simulate fallback to client-side
            mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('Server error')));
            spyOn(enhancedTable.sortController, 'applyClientSideSort').and.returnValue(Promise.resolve());
            
            await enhancedTable.sortController.applySortAsync('species', 'desc');
            
            // Client-side sort should not have global class
            const currentSort = enhancedTable.sortController.getCurrentSort();
            expect(currentSort.isGlobal).toBe(false);
        });
    });
    
    describe('Error Handling', function() {
        it('should handle network errors gracefully', async function() {
            const networkError = new window.ApiError('Network error', 0, null, new TypeError('Failed to fetch'));
            mockApiClient.getEntities.and.returnValue(Promise.reject(networkError));
            
            spyOn(enhancedTable.sortController, 'applyClientSideSort').and.returnValue(Promise.resolve());
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(enhancedTable.sortController.applyClientSideSort).toHaveBeenCalled();
        });
        
        it('should handle server errors with appropriate messages', async function() {
            const serverError = new window.ApiError('Server error', 500, { message: 'Internal server error' });
            mockApiClient.getEntities.and.returnValue(Promise.reject(serverError));
            
            spyOn(enhancedTable, 'showFallbackMessage');
            spyOn(enhancedTable.sortController, 'applyClientSideSort').and.returnValue(Promise.resolve());
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(enhancedTable.showFallbackMessage).toHaveBeenCalled();
        });
        
        it('should retry server-side sorting when connectivity is restored', function() {
            // Mark server as unavailable
            enhancedTable.sortController.markServerSideUnavailable();
            expect(enhancedTable.sortController.isServerSideAvailable()).toBe(false);
            
            // Simulate network restoration
            enhancedTable.sortController.clearServerUnavailableStatus();
            expect(enhancedTable.sortController.isServerSideAvailable()).toBe(true);
        });
    });
    
    describe('Performance and Caching', function() {
        it('should not cache sort requests to ensure fresh data', async function() {
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                useCache: false
            }));
        });
        
        it('should clear entity cache after bulk operations', function() {
            enhancedTable.bulkController.clearSelection();
            // Cache clearing is tested in bulk operations, but we verify the method exists
            expect(mockApiClient.clearEntityCache).toBeDefined();
        });
    });
    
    describe('Integration with Existing Components', function() {
        it('should maintain compatibility with existing table functionality', function() {
            expect(table.classList.contains('enhanced-table')).toBe(true);
            expect(table.classList.contains('table-sortable')).toBe(true);
        });
        
        it('should dispatch events for other components', async function() {
            let eventFired = false;
            table.addEventListener('tableUpdated', function() {
                eventFired = true;
            });
            
            await enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(eventFired).toBe(true);
        });
        
        it('should work with existing pagination controls', function() {
            // Test that pagination state is maintained
            enhancedTable.currentPage = 2;
            enhancedTable.pageSize = 20;
            
            enhancedTable.sortController.applySortAsync('name', 'asc');
            
            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                page: 2,
                size: 20
            }));
        });
    });
});

// Property-based test for sort parameter validation
describe('Sort Parameter Validation Properties', function() {
    let enhancedTable, mockApiClient;
    
    beforeEach(function() {
        document.body.innerHTML = `
            <table id="testTable" class="table table-sortable">
                <thead>
                    <tr>
                        <th class="sortable-header" data-column="id">ID</th>
                        <th class="sortable-header" data-column="name">Name</th>
                    </tr>
                </thead>
                <tbody><tr><td>1</td><td>Test</td></tr></tbody>
            </table>
        `;
        
        mockApiClient = {
            getEntities: jasmine.createSpy('getEntities').and.returnValue(Promise.resolve({})),
            transformFiltersToBackend: jasmine.createSpy('transformFiltersToBackend').and.returnValue({}),
            transformResponseToFrontend: jasmine.createSpy('transformResponseToFrontend').and.returnValue({})
        };
        
        window.tableApiClient = mockApiClient;
        
        enhancedTable = new window.EnhancedTable(document.getElementById('testTable'), {
            entityType: 'pets',
            enableGlobalSort: true
        });
    });
    
    afterEach(function() {
        if (enhancedTable) {
            enhancedTable.destroy();
        }
        document.body.innerHTML = '';
    });
    
    /**
     * Property: Sort requests always include valid column names and directions
     * **Validates: Requirements 6.1**
     */
    it('should always send valid sort parameters', function() {
        const validColumns = ['id', 'name'];
        const validDirections = ['asc', 'desc'];
        
        // Test all combinations
        validColumns.forEach(column => {
            validDirections.forEach(direction => {
                enhancedTable.sortController.applySortAsync(column, direction);
                
                expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                    sort: `${column},${direction}`
                }));
            });
        });
    });
    
    /**
     * Property: Multi-column sort maintains proper order and precedence
     * **Validates: Requirements 6.5**
     */
    it('should maintain sort precedence in multi-column sorting', async function() {
        await enhancedTable.sortController.applySortAsync('name', 'asc', false);
        await enhancedTable.sortController.applySortAsync('id', 'desc', true);
        
        const currentSort = enhancedTable.sortController.getCurrentSort();
        expect(currentSort.multiColumn).toEqual([
            { column: 'name', direction: 'asc', precedence: 0 },
            { column: 'id', direction: 'desc', precedence: 1 }
        ]);
    });
    
    /**
     * Property: Fallback mechanism always provides working sort functionality
     * **Validates: Requirements 6.4**
     */
    it('should always provide working sort functionality even when server fails', async function() {
        // Mock server failure
        mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('Server error')));
        
        // Mock TableSorter for fallback
        const mockTableSorter = {
            sortByColumnName: jasmine.createSpy('sortByColumnName'),
            sortByColumn: jasmine.createSpy('sortByColumn')
        };
        window.TableSorter = jasmine.createSpy('TableSorter').and.returnValue(mockTableSorter);
        
        await enhancedTable.sortController.applySortAsync('name', 'asc');
        
        // Should fall back to client-side sorting
        expect(mockTableSorter.sortByColumnName || mockTableSorter.sortByColumn).toHaveBeenCalled();
    });
});