/**
 * Unit Tests for Visit Search Frontend
 * Tests the comprehensive visit search and filtering functionality
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */

describe('VisitSearchFilterManager', function() {
    let container;
    let searchManager;
    let mockApiClient;

    beforeEach(function() {
        // Create test container
        container = document.createElement('div');
        container.className = 'visits-container';
        container.innerHTML = `
            <table class="table" id="visitsTable">
                <thead>
                    <tr>
                        <th class="select-column">Select</th>
                        <th data-column="visitDate">Date</th>
                        <th data-column="petName">Pet</th>
                        <th data-column="description">Description</th>
                        <th data-column="veterinarianName">Veterinarian</th>
                        <th data-column="status">Status</th>
                        <th data-column="cost">Cost</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody></tbody>
            </table>
        `;
        document.body.appendChild(container);

        // Mock API client
        mockApiClient = {
            getEntities: jasmine.createSpy('getEntities').and.returnValue(Promise.resolve({
                content: [],
                totalElements: 0
            }))
        };
        window.tableApiClient = mockApiClient;

        // Initialize search manager
        searchManager = new VisitSearchFilterManager(container);
    });

    afterEach(function() {
        if (searchManager) {
            searchManager.destroy();
        }
        if (container && container.parentNode) {
            document.body.removeChild(container);
        }
    });

    describe('Initialization', function() {
        it('should create filter UI components', function() {
            expect(container.querySelector('.visit-search-filters')).toBeTruthy();
            expect(container.querySelector('#petFilter')).toBeTruthy();
            expect(container.querySelector('#veterinarianFilter')).toBeTruthy();
            expect(container.querySelector('#statusFilter')).toBeTruthy();
        });

        it('should setup event listeners', function() {
            const petInput = container.querySelector('#petFilter');
            const vetInput = container.querySelector('#veterinarianFilter');
            const clearAllBtn = container.querySelector('#clearAllFilters');

            expect(petInput).toBeTruthy();
            expect(vetInput).toBeTruthy();
            expect(clearAllBtn).toBeTruthy();
        });

        it('should initialize with no active filters', function() {
            expect(searchManager.activeFilters.size).toBe(0);
        });
    });

    describe('Pet Filter', function() {
        it('should search pets when input changes', async function() {
            const petInput = container.querySelector('#petFilter');
            
            // Mock pets response
            mockApiClient.getEntities.and.returnValue(Promise.resolve({
                content: [
                    { id: 1, name: 'Buddy', species: 'Dog', owner: { firstName: 'John', lastName: 'Smith' } }
                ]
            }));

            petInput.value = 'Buddy';
            petInput.dispatchEvent(new Event('input'));

            // Wait for debounce
            await new Promise(resolve => setTimeout(resolve, 400));

            expect(mockApiClient.getEntities).toHaveBeenCalledWith('pets', jasmine.objectContaining({
                search: 'Buddy'
            }));
        });

        it('should update dropdown with search results', async function() {
            const mockPets = [
                { id: 1, name: 'Buddy', species: 'Dog', owner: { firstName: 'John', lastName: 'Smith' } },
                { id: 2, name: 'Max', species: 'Dog', owner: { firstName: 'Jane', lastName: 'Doe' } }
            ];

            searchManager.updatePetDropdown(mockPets, 'Bu');

            const dropdown = container.querySelector('#petFilterDropdown');
            const petOptions = dropdown.querySelectorAll('.pet-option');

            expect(petOptions.length).toBe(2);
            expect(petOptions[0].getAttribute('data-pet-id')).toBe('1');
            expect(petOptions[0].getAttribute('data-pet-name')).toBe('Buddy');
        });

        it('should select pet and add to active filters', function() {
            searchManager.selectPet('1', 'Buddy');

            expect(searchManager.activeFilters.has('pet')).toBe(true);
            expect(searchManager.activeFilters.get('pet').id).toBe('1');
            expect(searchManager.activeFilters.get('pet').name).toBe('Buddy');
        });
    });

    describe('Veterinarian Filter', function() {
        it('should search veterinarians when input changes', async function() {
            const vetInput = container.querySelector('#veterinarianFilter');
            
            // Mock veterinarians response
            mockApiClient.getEntities.and.returnValue(Promise.resolve({
                content: [
                    { id: 1, firstName: 'Sarah', lastName: 'Johnson', specialties: ['General Practice'] }
                ]
            }));

            vetInput.value = 'Johnson';
            vetInput.dispatchEvent(new Event('input'));

            // Wait for debounce
            await new Promise(resolve => setTimeout(resolve, 400));

            expect(mockApiClient.getEntities).toHaveBeenCalledWith('veterinarians', jasmine.objectContaining({
                search: 'Johnson'
            }));
        });

        it('should update dropdown with search results', function() {
            const mockVets = [
                { id: 1, firstName: 'Sarah', lastName: 'Johnson', specialties: ['General Practice'] },
                { id: 2, firstName: 'Michael', lastName: 'Brown', specialties: ['Surgery'] }
            ];

            searchManager.updateVeterinarianDropdown(mockVets, 'Jo');

            const dropdown = container.querySelector('#veterinarianFilterDropdown');
            const vetOptions = dropdown.querySelectorAll('.vet-option');

            expect(vetOptions.length).toBe(2);
            expect(vetOptions[0].getAttribute('data-vet-id')).toBe('1');
            expect(vetOptions[0].getAttribute('data-vet-name')).toBe('Dr. Sarah Johnson');
        });

        it('should select veterinarian and add to active filters', function() {
            searchManager.selectVeterinarian('1', 'Dr. Sarah Johnson');

            expect(searchManager.activeFilters.has('veterinarian')).toBe(true);
            expect(searchManager.activeFilters.get('veterinarian').id).toBe('1');
            expect(searchManager.activeFilters.get('veterinarian').name).toBe('Dr. Sarah Johnson');
        });
    });

    describe('Status Filter', function() {
        it('should update status filter when checkboxes change', function() {
            const completedCheckbox = container.querySelector('#status_completed');
            const pendingCheckbox = container.querySelector('#status_pending');

            completedCheckbox.checked = true;
            pendingCheckbox.checked = true;

            searchManager.updateStatusFilter();

            expect(searchManager.activeFilters.has('status')).toBe(true);
            expect(searchManager.activeFilters.get('status').values).toEqual(['completed', 'pending']);
        });

        it('should clear status filter', function() {
            // First set some status filters
            const completedCheckbox = container.querySelector('#status_completed');
            completedCheckbox.checked = true;
            searchManager.updateStatusFilter();

            expect(searchManager.activeFilters.has('status')).toBe(true);

            // Then clear them
            searchManager.clearStatusFilter();

            expect(searchManager.activeFilters.has('status')).toBe(false);
            expect(completedCheckbox.checked).toBe(false);
        });
    });

    describe('Active Filters Display', function() {
        it('should show active filters container when filters are active', function() {
            searchManager.selectPet('1', 'Buddy');

            const container = searchManager.filterContainer.querySelector('.active-filters-container');
            expect(container.style.display).toBe('block');
        });

        it('should hide active filters container when no filters are active', function() {
            const container = searchManager.filterContainer.querySelector('.active-filters-container');
            expect(container.style.display).toBe('none');
        });

        it('should display filter badges correctly', function() {
            searchManager.selectPet('1', 'Buddy');
            searchManager.selectVeterinarian('2', 'Dr. Sarah Johnson');

            const filtersList = searchManager.filterContainer.querySelector('.active-filters-list');
            const badges = filtersList.querySelectorAll('.badge');

            expect(badges.length).toBe(2);
            expect(badges[0].textContent).toContain('Pet: Buddy');
            expect(badges[1].textContent).toContain('Veterinarian: Dr. Sarah Johnson');
        });
    });

    describe('Filter Removal', function() {
        it('should remove specific filter', function() {
            searchManager.selectPet('1', 'Buddy');
            searchManager.selectVeterinarian('2', 'Dr. Sarah Johnson');

            expect(searchManager.activeFilters.size).toBe(2);

            searchManager.removeFilter('pet');

            expect(searchManager.activeFilters.size).toBe(1);
            expect(searchManager.activeFilters.has('pet')).toBe(false);
            expect(searchManager.activeFilters.has('veterinarian')).toBe(true);
        });

        it('should clear all filters', function() {
            searchManager.selectPet('1', 'Buddy');
            searchManager.selectVeterinarian('2', 'Dr. Sarah Johnson');

            expect(searchManager.activeFilters.size).toBe(2);

            searchManager.clearAllFilters();

            expect(searchManager.activeFilters.size).toBe(0);
        });
    });

    describe('Search Execution', function() {
        it('should execute search with no filters', async function() {
            spyOn(searchManager, 'displaySearchResults');
            spyOn(searchManager, 'updateResultCounts');

            await searchManager.executeSearch();

            expect(mockApiClient.getEntities).toHaveBeenCalledWith('visits', jasmine.objectContaining({
                page: 0,
                size: 50,
                useCache: false
            }));
        });

        it('should execute single filter search', async function() {
            searchManager.selectPet('1', 'Buddy');
            
            spyOn(searchManager, 'executeSingleFilterSearch').and.returnValue(Promise.resolve({
                visits: [],
                totalCount: 0
            }));
            spyOn(searchManager, 'displaySearchResults');
            spyOn(searchManager, 'updateResultCounts');

            await searchManager.executeSearch();

            expect(searchManager.executeSingleFilterSearch).toHaveBeenCalled();
        });

        it('should execute combined search with multiple filters', async function() {
            searchManager.selectPet('1', 'Buddy');
            searchManager.selectVeterinarian('2', 'Dr. Sarah Johnson');
            
            spyOn(searchManager, 'executeCombinedSearch').and.returnValue(Promise.resolve({
                visits: [],
                totalCount: 0
            }));
            spyOn(searchManager, 'displaySearchResults');
            spyOn(searchManager, 'updateResultCounts');

            await searchManager.executeSearch();

            expect(searchManager.executeCombinedSearch).toHaveBeenCalled();
        });
    });

    describe('Search Highlighting', function() {
        it('should highlight search terms in text', function() {
            const text = 'Buddy the dog';
            const query = 'Buddy';
            const highlighted = searchManager.highlightText(text, query);

            expect(highlighted).toBe('<mark class="search-highlight">Buddy</mark> the dog');
        });

        it('should escape regex characters', function() {
            const text = 'Cost: $150.00';
            const query = '$150';
            const highlighted = searchManager.highlightText(text, query);

            expect(highlighted).toBe('Cost: <mark class="search-highlight">$150</mark>.00');
        });
    });

    describe('Result Display', function() {
        it('should display search results in table', function() {
            const mockResults = {
                visits: [
                    {
                        id: 1,
                        visitDate: '2024-01-15T10:00:00',
                        pet: { id: 1, name: 'Buddy' },
                        veterinarian: { id: 1, lastName: 'Johnson' },
                        description: 'Annual checkup',
                        diagnosis: 'Healthy',
                        treatment: 'Vaccinations',
                        cost: 150.00,
                        emergencyVisit: false
                    }
                ]
            };

            searchManager.displaySearchResults(mockResults);

            const tbody = container.querySelector('#visitsTable tbody');
            const rows = tbody.querySelectorAll('tr');

            expect(rows.length).toBe(1);
            expect(rows[0].getAttribute('data-id')).toBe('1');
        });

        it('should show no results message when empty', function() {
            const mockResults = { visits: [] };

            searchManager.displaySearchResults(mockResults);

            const tbody = container.querySelector('#visitsTable tbody');
            const noResultsMessage = tbody.querySelector('td[colspan="8"]');

            expect(noResultsMessage).toBeTruthy();
            expect(noResultsMessage.textContent).toContain('No visits found');
        });
    });

    describe('Status Determination', function() {
        it('should determine emergency status', function() {
            const visit = { emergencyVisit: true };
            const status = searchManager.determineVisitStatus(visit);
            expect(status).toBe('emergency');
        });

        it('should determine completed status', function() {
            const visit = { 
                emergencyVisit: false,
                diagnosis: 'Healthy',
                treatment: 'Vaccinations'
            };
            const status = searchManager.determineVisitStatus(visit);
            expect(status).toBe('completed');
        });

        it('should determine pending status', function() {
            const visit = { 
                emergencyVisit: false,
                diagnosis: null,
                treatment: null
            };
            const status = searchManager.determineVisitStatus(visit);
            expect(status).toBe('pending');
        });
    });

    describe('Export Functionality', function() {
        it('should generate CSV content', function() {
            const visits = [
                {
                    visitDate: '2024-01-15T10:00:00',
                    pet: { name: 'Buddy' },
                    veterinarian: { lastName: 'Johnson' },
                    description: 'Annual checkup',
                    cost: 150.00,
                    emergencyVisit: false
                }
            ];

            const csv = searchManager.generateCSV(visits);
            const lines = csv.split('\n');

            expect(lines[0]).toBe('Date,Time,Pet,Description,Veterinarian,Status,Cost');
            expect(lines[1]).toContain('Buddy');
            expect(lines[1]).toContain('Dr. Johnson');
            expect(lines[1]).toContain('150.00');
        });
    });

    describe('Error Handling', function() {
        it('should handle search errors gracefully', async function() {
            mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('Network error')));
            spyOn(searchManager, 'showSearchError');

            await searchManager.executeSearch();

            expect(searchManager.showSearchError).toHaveBeenCalledWith('Search failed. Please try again.');
        });

        it('should handle pet search errors', async function() {
            mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('API error')));

            await searchManager.searchPets('Buddy');

            const dropdown = container.querySelector('#petFilterDropdown');
            expect(dropdown.textContent).toContain('Search failed. Please try again.');
        });

        it('should handle veterinarian search errors', async function() {
            mockApiClient.getEntities.and.returnValue(Promise.reject(new Error('API error')));

            await searchManager.searchVeterinarians('Johnson');

            const dropdown = container.querySelector('#veterinarianFilterDropdown');
            expect(dropdown.textContent).toContain('Search failed. Please try again.');
        });
    });

    describe('Cleanup', function() {
        it('should clean up resources on destroy', function() {
            searchManager.selectPet('1', 'Buddy');
            
            expect(searchManager.activeFilters.size).toBe(1);
            expect(searchManager.searchCache.size).toBeGreaterThanOrEqual(0);

            searchManager.destroy();

            expect(searchManager.activeFilters.size).toBe(0);
            expect(searchManager.searchCache.size).toBe(0);
        });
    });
});

// Test helper functions
function createMockVisit(overrides = {}) {
    return {
        id: 1,
        visitDate: '2024-01-15T10:00:00',
        pet: { id: 1, name: 'Buddy' },
        veterinarian: { id: 1, lastName: 'Johnson' },
        description: 'Test visit',
        diagnosis: null,
        treatment: null,
        cost: 100.00,
        emergencyVisit: false,
        ...overrides
    };
}

function createMockPet(overrides = {}) {
    return {
        id: 1,
        name: 'Buddy',
        species: 'Dog',
        owner: { firstName: 'John', lastName: 'Smith' },
        ...overrides
    };
}

function createMockVeterinarian(overrides = {}) {
    return {
        id: 1,
        firstName: 'Sarah',
        lastName: 'Johnson',
        specialties: ['General Practice'],
        ...overrides
    };
}