/**
 * Visit Search Component
 * Provides comprehensive search and filtering functionality for visits
 * Integrates with the new backend search endpoints from Task 7.1
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */

(function() {
    'use strict';

    /**
     * Visit Search Configuration
     */
    const SEARCH_CONFIG = {
        DEBOUNCE_DELAY: 300,
        MIN_SEARCH_LENGTH: 2,
        MAX_RESULTS_PER_DROPDOWN: 50,
        CACHE_TTL: 300000, // 5 minutes
        API_ENDPOINTS: {
            PETS: '/api/pets',
            VETERINARIANS: '/api/veterinarians',
            VISITS_BY_PET: '/api/visits/search/by-pet',
            VISITS_BY_VETERINARIAN: '/api/visits/search/by-veterinarian',
            VISITS_BY_STATUS: '/api/visits/search/by-status',
            VISITS_COMBINED: '/api/visits/search/combined'
        },
        STATUS_OPTIONS: [
            { value: 'completed', label: 'Completed', badge: 'bg-success' },
            { value: 'pending', label: 'Pending', badge: 'bg-warning' },
            { value: 'emergency', label: 'Emergency', badge: 'bg-danger' },
            { value: 'cancelled', label: 'Cancelled', badge: 'bg-secondary' }
        ]
    };

    /**
     * Visit Search Filter Manager
     * Manages all search filters and their interactions
     */
    class VisitSearchFilterManager {
        constructor(container, options = {}) {
            this.container = container;
            this.options = {
                enablePetFilter: true,
                enableVeterinarianFilter: true,
                enableStatusFilter: true,
                enableResultCounts: true,
                enableSearchHighlighting: true,
                ...options
            };

            this.activeFilters = new Map();
            this.searchCache = new Map();
            this.debounceTimers = new Map();
            this.apiClient = window.tableApiClient || new window.TableApiClient();

            this.init();
        }

        /**
         * Initialize the search filter manager
         */
        init() {
            try {
                this.createFilterUI();
                this.setupEventListeners();
                this.loadInitialData();
                console.log('VisitSearchFilterManager initialized successfully');
            } catch (error) {
                console.error('Failed to initialize VisitSearchFilterManager:', error);
                this.showError('Search filters are temporarily unavailable');
            }
        }

        /**
         * Create the filter UI components
         */
        createFilterUI() {
            const filterContainer = document.createElement('div');
            filterContainer.className = 'visit-search-filters mb-4';
            filterContainer.innerHTML = this.buildFilterHTML();

            // Insert before the existing search form or table
            const existingSearch = this.container.querySelector('.search-form');
            const table = this.container.querySelector('.table-responsive');
            const insertBefore = existingSearch || table;
            
            if (insertBefore) {
                insertBefore.parentNode.insertBefore(filterContainer, insertBefore);
            } else {
                this.container.appendChild(filterContainer);
            }

            this.filterContainer = filterContainer;
        }

        /**
         * Build the filter HTML structure
         * @returns {string} Filter HTML
         */
        buildFilterHTML() {
            return `
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">
                            <i class="fas fa-search me-2"></i>
                            Advanced Visit Search
                        </h5>
                    </div>
                    <div class="card-body">
                        <div class="row g-3">
                            ${this.options.enablePetFilter ? this.buildPetFilterHTML() : ''}
                            ${this.options.enableVeterinarianFilter ? this.buildVeterinarianFilterHTML() : ''}
                            ${this.options.enableStatusFilter ? this.buildStatusFilterHTML() : ''}
                        </div>
                        
                        <!-- Active Filters Display -->
                        <div class="active-filters-container mt-3" style="display: none;">
                            <div class="d-flex align-items-center">
                                <span class="text-muted me-2">Active filters:</span>
                                <div class="active-filters-list d-flex flex-wrap gap-2"></div>
                                <button type="button" class="btn btn-sm btn-outline-secondary ms-auto" id="clearAllFilters">
                                    <i class="fas fa-times me-1"></i>Clear All
                                </button>
                            </div>
                        </div>

                        <!-- Search Results Summary -->
                        <div class="search-results-summary mt-3" style="display: none;">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="result-counts">
                                        <span class="total-results badge bg-primary me-2">0 total</span>
                                        <span class="completed-count badge bg-success me-2">0 completed</span>
                                        <span class="pending-count badge bg-warning me-2">0 pending</span>
                                        <span class="emergency-count badge bg-danger me-2">0 emergency</span>
                                    </div>
                                </div>
                                <div class="col-md-6 text-end">
                                    <button type="button" class="btn btn-sm btn-outline-info" id="exportResults">
                                        <i class="fas fa-download me-1"></i>Export Results
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }

        /**
         * Build pet filter HTML
         * @returns {string} Pet filter HTML
         */
        buildPetFilterHTML() {
            return `
                <div class="col-md-4">
                    <label for="petFilter" class="form-label">
                        <i class="fas fa-paw me-1"></i>Filter by Pet
                    </label>
                    <div class="dropdown">
                        <input type="text" 
                               class="form-control dropdown-toggle" 
                               id="petFilter" 
                               placeholder="Search pets..." 
                               data-bs-toggle="dropdown" 
                               autocomplete="off">
                        <ul class="dropdown-menu w-100" id="petFilterDropdown">
                            <li><div class="dropdown-item-text text-muted">Start typing to search pets...</div></li>
                        </ul>
                    </div>
                </div>
            `;
        }

        /**
         * Build veterinarian filter HTML
         * @returns {string} Veterinarian filter HTML
         */
        buildVeterinarianFilterHTML() {
            return `
                <div class="col-md-4">
                    <label for="veterinarianFilter" class="form-label">
                        <i class="fas fa-user-md me-1"></i>Filter by Veterinarian
                    </label>
                    <div class="dropdown">
                        <input type="text" 
                               class="form-control dropdown-toggle" 
                               id="veterinarianFilter" 
                               placeholder="Search veterinarians..." 
                               data-bs-toggle="dropdown" 
                               autocomplete="off">
                        <ul class="dropdown-menu w-100" id="veterinarianFilterDropdown">
                            <li><div class="dropdown-item-text text-muted">Start typing to search veterinarians...</div></li>
                        </ul>
                    </div>
                </div>
            `;
        }

        /**
         * Build status filter HTML
         * @returns {string} Status filter HTML
         */
        buildStatusFilterHTML() {
            const statusOptions = SEARCH_CONFIG.STATUS_OPTIONS.map(status => `
                <li>
                    <div class="dropdown-item">
                        <div class="form-check">
                            <input class="form-check-input" 
                                   type="checkbox" 
                                   value="${status.value}" 
                                   id="status_${status.value}">
                            <label class="form-check-label d-flex align-items-center" for="status_${status.value}">
                                <span class="badge ${status.badge} me-2">${status.label}</span>
                                <span class="status-count text-muted">(0)</span>
                            </label>
                        </div>
                    </div>
                </li>
            `).join('');

            return `
                <div class="col-md-4">
                    <label for="statusFilter" class="form-label">
                        <i class="fas fa-tags me-1"></i>Filter by Status
                    </label>
                    <div class="dropdown">
                        <button class="btn btn-outline-secondary dropdown-toggle w-100 text-start" 
                                type="button" 
                                id="statusFilter" 
                                data-bs-toggle="dropdown">
                            <span class="filter-text">Select status...</span>
                        </button>
                        <ul class="dropdown-menu w-100" id="statusFilterDropdown">
                            <li><h6 class="dropdown-header">Select Status(es)</h6></li>
                            <li><hr class="dropdown-divider"></li>
                            ${statusOptions}
                            <li><hr class="dropdown-divider"></li>
                            <li>
                                <div class="dropdown-item">
                                    <button type="button" class="btn btn-sm btn-outline-secondary w-100" id="clearStatusFilter">
                                        <i class="fas fa-times me-1"></i>Clear Status Filter
                                    </button>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            `;
        }

        /**
         * Setup event listeners for all filter components
         */
        setupEventListeners() {
            // Pet filter search
            if (this.options.enablePetFilter) {
                this.setupPetFilterListeners();
            }

            // Veterinarian filter search
            if (this.options.enableVeterinarianFilter) {
                this.setupVeterinarianFilterListeners();
            }

            // Status filter
            if (this.options.enableStatusFilter) {
                this.setupStatusFilterListeners();
            }

            // Clear all filters
            const clearAllBtn = this.filterContainer.querySelector('#clearAllFilters');
            if (clearAllBtn) {
                clearAllBtn.addEventListener('click', () => this.clearAllFilters());
            }

            // Export results
            const exportBtn = this.filterContainer.querySelector('#exportResults');
            if (exportBtn) {
                exportBtn.addEventListener('click', () => this.exportResults());
            }
        }

        /**
         * Setup pet filter event listeners
         */
        setupPetFilterListeners() {
            const petInput = this.filterContainer.querySelector('#petFilter');
            const petDropdown = this.filterContainer.querySelector('#petFilterDropdown');

            if (petInput && petDropdown) {
                // Search input with debouncing
                petInput.addEventListener('input', (e) => {
                    this.debounceSearch('pet', e.target.value, () => {
                        this.searchPets(e.target.value);
                    });
                });

                // Handle dropdown selection
                petDropdown.addEventListener('click', (e) => {
                    if (e.target.classList.contains('pet-option')) {
                        e.preventDefault();
                        const petId = e.target.getAttribute('data-pet-id');
                        const petName = e.target.getAttribute('data-pet-name');
                        this.selectPet(petId, petName);
                        
                        // Close dropdown
                        const dropdown = bootstrap.Dropdown.getInstance(petInput);
                        if (dropdown) dropdown.hide();
                    }
                });

                // Clear selection on focus
                petInput.addEventListener('focus', () => {
                    if (this.activeFilters.has('pet')) {
                        petInput.value = '';
                        this.searchPets('');
                    }
                });
            }
        }

        /**
         * Setup veterinarian filter event listeners
         */
        setupVeterinarianFilterListeners() {
            const vetInput = this.filterContainer.querySelector('#veterinarianFilter');
            const vetDropdown = this.filterContainer.querySelector('#veterinarianFilterDropdown');

            if (vetInput && vetDropdown) {
                // Search input with debouncing
                vetInput.addEventListener('input', (e) => {
                    this.debounceSearch('veterinarian', e.target.value, () => {
                        this.searchVeterinarians(e.target.value);
                    });
                });

                // Handle dropdown selection
                vetDropdown.addEventListener('click', (e) => {
                    if (e.target.classList.contains('vet-option')) {
                        e.preventDefault();
                        const vetId = e.target.getAttribute('data-vet-id');
                        const vetName = e.target.getAttribute('data-vet-name');
                        this.selectVeterinarian(vetId, vetName);
                        
                        // Close dropdown
                        const dropdown = bootstrap.Dropdown.getInstance(vetInput);
                        if (dropdown) dropdown.hide();
                    }
                });

                // Clear selection on focus
                vetInput.addEventListener('focus', () => {
                    if (this.activeFilters.has('veterinarian')) {
                        vetInput.value = '';
                        this.searchVeterinarians('');
                    }
                });
            }
        }

        /**
         * Setup status filter event listeners
         */
        setupStatusFilterListeners() {
            const statusDropdown = this.filterContainer.querySelector('#statusFilterDropdown');
            const clearStatusBtn = this.filterContainer.querySelector('#clearStatusFilter');

            if (statusDropdown) {
                // Handle status checkbox changes
                statusDropdown.addEventListener('change', (e) => {
                    if (e.target.type === 'checkbox') {
                        this.updateStatusFilter();
                    }
                });

                // Prevent dropdown from closing when clicking checkboxes
                statusDropdown.addEventListener('click', (e) => {
                    if (e.target.type === 'checkbox' || e.target.classList.contains('form-check-label')) {
                        e.stopPropagation();
                    }
                });
            }

            if (clearStatusBtn) {
                clearStatusBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.clearStatusFilter();
                });
            }
        }

        /**
         * Debounce search input to avoid excessive API calls
         * @param {string} type - Search type (pet, veterinarian)
         * @param {string} query - Search query
         * @param {Function} callback - Callback function
         */
        debounceSearch(type, query, callback) {
            // Clear existing timer
            if (this.debounceTimers.has(type)) {
                clearTimeout(this.debounceTimers.get(type));
            }

            // Set new timer
            const timer = setTimeout(() => {
                callback();
                this.debounceTimers.delete(type);
            }, SEARCH_CONFIG.DEBOUNCE_DELAY);

            this.debounceTimers.set(type, timer);
        }

        /**
         * Search for pets based on query
         * @param {string} query - Search query
         */
        async searchPets(query) {
            const dropdown = this.filterContainer.querySelector('#petFilterDropdown');
            if (!dropdown) return;

            try {
                if (query.length < SEARCH_CONFIG.MIN_SEARCH_LENGTH) {
                    dropdown.innerHTML = '<li><div class="dropdown-item-text text-muted">Start typing to search pets...</div></li>';
                    return;
                }

                // Show loading
                dropdown.innerHTML = '<li><div class="dropdown-item-text"><i class="fas fa-spinner fa-spin me-2"></i>Searching pets...</div></li>';

                // Check cache first
                const cacheKey = `pets_${query}`;
                let pets = this.searchCache.get(cacheKey);

                if (!pets) {
                    // Make API call
                    const response = await this.apiClient.getEntities('pets', {
                        page: 0,
                        size: SEARCH_CONFIG.MAX_RESULTS_PER_DROPDOWN,
                        search: query,
                        useCache: true
                    });

                    pets = response.content || response;
                    this.searchCache.set(cacheKey, pets);
                }

                // Update dropdown
                this.updatePetDropdown(pets, query);

            } catch (error) {
                console.error('Pet search failed:', error);
                dropdown.innerHTML = '<li><div class="dropdown-item-text text-danger">Search failed. Please try again.</div></li>';
            }
        }

        /**
         * Update pet dropdown with search results
         * @param {Array} pets - Pet search results
         * @param {string} query - Search query for highlighting
         */
        updatePetDropdown(pets, query) {
            const dropdown = this.filterContainer.querySelector('#petFilterDropdown');
            if (!dropdown) return;

            if (pets.length === 0) {
                dropdown.innerHTML = '<li><div class="dropdown-item-text text-muted">No pets found</div></li>';
                return;
            }

            const petItems = pets.map(pet => {
                const highlightedName = this.highlightText(pet.name, query);
                const ownerInfo = pet.owner ? `Owner: ${pet.owner.firstName} ${pet.owner.lastName}` : 'No owner';
                
                return `
                    <li>
                        <a class="dropdown-item pet-option" 
                           href="#" 
                           data-pet-id="${pet.id}" 
                           data-pet-name="${pet.name}">
                            <div class="d-flex align-items-center">
                                <div class="pet-avatar me-2">
                                    <i class="fas fa-paw text-primary"></i>
                                </div>
                                <div class="flex-grow-1">
                                    <div class="pet-name">${highlightedName}</div>
                                    <small class="text-muted">${pet.species || 'Unknown species'} • ${ownerInfo}</small>
                                </div>
                            </div>
                        </a>
                    </li>
                `;
            }).join('');

            dropdown.innerHTML = petItems;
        }

        /**
         * Search for veterinarians based on query
         * @param {string} query - Search query
         */
        async searchVeterinarians(query) {
            const dropdown = this.filterContainer.querySelector('#veterinarianFilterDropdown');
            if (!dropdown) return;

            try {
                if (query.length < SEARCH_CONFIG.MIN_SEARCH_LENGTH) {
                    dropdown.innerHTML = '<li><div class="dropdown-item-text text-muted">Start typing to search veterinarians...</div></li>';
                    return;
                }

                // Show loading
                dropdown.innerHTML = '<li><div class="dropdown-item-text"><i class="fas fa-spinner fa-spin me-2"></i>Searching veterinarians...</div></li>';

                // Check cache first
                const cacheKey = `veterinarians_${query}`;
                let veterinarians = this.searchCache.get(cacheKey);

                if (!veterinarians) {
                    // Make API call
                    const response = await this.apiClient.getEntities('veterinarians', {
                        page: 0,
                        size: SEARCH_CONFIG.MAX_RESULTS_PER_DROPDOWN,
                        search: query,
                        useCache: true
                    });

                    veterinarians = response.content || response;
                    this.searchCache.set(cacheKey, veterinarians);
                }

                // Update dropdown
                this.updateVeterinarianDropdown(veterinarians, query);

            } catch (error) {
                console.error('Veterinarian search failed:', error);
                dropdown.innerHTML = '<li><div class="dropdown-item-text text-danger">Search failed. Please try again.</div></li>';
            }
        }

        /**
         * Update veterinarian dropdown with search results
         * @param {Array} veterinarians - Veterinarian search results
         * @param {string} query - Search query for highlighting
         */
        updateVeterinarianDropdown(veterinarians, query) {
            const dropdown = this.filterContainer.querySelector('#veterinarianFilterDropdown');
            if (!dropdown) return;

            if (veterinarians.length === 0) {
                dropdown.innerHTML = '<li><div class="dropdown-item-text text-muted">No veterinarians found</div></li>';
                return;
            }

            const vetItems = veterinarians.map(vet => {
                const fullName = `Dr. ${vet.firstName} ${vet.lastName}`;
                const highlightedName = this.highlightText(fullName, query);
                const specialties = vet.specialties && vet.specialties.length > 0 ? 
                    vet.specialties.join(', ') : 'General Practice';
                
                return `
                    <li>
                        <a class="dropdown-item vet-option" 
                           href="#" 
                           data-vet-id="${vet.id}" 
                           data-vet-name="${fullName}">
                            <div class="d-flex align-items-center">
                                <div class="vet-avatar me-2">
                                    <i class="fas fa-user-md text-success"></i>
                                </div>
                                <div class="flex-grow-1">
                                    <div class="vet-name">${highlightedName}</div>
                                    <small class="text-muted">${specialties}</small>
                                </div>
                            </div>
                        </a>
                    </li>
                `;
            }).join('');

            dropdown.innerHTML = vetItems;
        }

        /**
         * Select a pet filter
         * @param {string} petId - Pet ID
         * @param {string} petName - Pet name
         */
        selectPet(petId, petName) {
            const petInput = this.filterContainer.querySelector('#petFilter');
            if (petInput) {
                petInput.value = petName;
            }

            this.activeFilters.set('pet', {
                type: 'pet',
                id: petId,
                name: petName,
                displayName: `Pet: ${petName}`
            });

            this.updateActiveFiltersDisplay();
            this.executeSearch();
        }

        /**
         * Select a veterinarian filter
         * @param {string} vetId - Veterinarian ID
         * @param {string} vetName - Veterinarian name
         */
        selectVeterinarian(vetId, vetName) {
            const vetInput = this.filterContainer.querySelector('#veterinarianFilter');
            if (vetInput) {
                vetInput.value = vetName;
            }

            this.activeFilters.set('veterinarian', {
                type: 'veterinarian',
                id: vetId,
                name: vetName,
                displayName: `Veterinarian: ${vetName}`
            });

            this.updateActiveFiltersDisplay();
            this.executeSearch();
        }

        /**
         * Update status filter based on checkbox selections
         */
        updateStatusFilter() {
            const checkboxes = this.filterContainer.querySelectorAll('#statusFilterDropdown input[type="checkbox"]:checked');
            const selectedStatuses = Array.from(checkboxes).map(cb => cb.value);

            if (selectedStatuses.length > 0) {
                const statusLabels = selectedStatuses.map(status => {
                    const statusConfig = SEARCH_CONFIG.STATUS_OPTIONS.find(s => s.value === status);
                    return statusConfig ? statusConfig.label : status;
                });

                this.activeFilters.set('status', {
                    type: 'status',
                    values: selectedStatuses,
                    displayName: `Status: ${statusLabels.join(', ')}`
                });

                // Update button text
                const statusButton = this.filterContainer.querySelector('#statusFilter .filter-text');
                if (statusButton) {
                    statusButton.textContent = `${selectedStatuses.length} status(es) selected`;
                }
            } else {
                this.activeFilters.delete('status');
                
                // Reset button text
                const statusButton = this.filterContainer.querySelector('#statusFilter .filter-text');
                if (statusButton) {
                    statusButton.textContent = 'Select status...';
                }
            }

            this.updateActiveFiltersDisplay();
            this.executeSearch();
        }

        /**
         * Clear status filter
         */
        clearStatusFilter() {
            const checkboxes = this.filterContainer.querySelectorAll('#statusFilterDropdown input[type="checkbox"]');
            checkboxes.forEach(cb => cb.checked = false);
            
            this.activeFilters.delete('status');
            
            // Reset button text
            const statusButton = this.filterContainer.querySelector('#statusFilter .filter-text');
            if (statusButton) {
                statusButton.textContent = 'Select status...';
            }

            this.updateActiveFiltersDisplay();
            this.executeSearch();
        }

        /**
         * Clear all active filters
         */
        clearAllFilters() {
            // Clear pet filter
            const petInput = this.filterContainer.querySelector('#petFilter');
            if (petInput) {
                petInput.value = '';
            }

            // Clear veterinarian filter
            const vetInput = this.filterContainer.querySelector('#veterinarianFilter');
            if (vetInput) {
                vetInput.value = '';
            }

            // Clear status filter
            this.clearStatusFilter();

            // Clear active filters
            this.activeFilters.clear();

            this.updateActiveFiltersDisplay();
            this.executeSearch();
        }

        /**
         * Update the active filters display
         */
        updateActiveFiltersDisplay() {
            const container = this.filterContainer.querySelector('.active-filters-container');
            const filtersList = this.filterContainer.querySelector('.active-filters-list');

            if (!container || !filtersList) return;

            if (this.activeFilters.size === 0) {
                container.style.display = 'none';
                return;
            }

            container.style.display = 'block';

            const filterBadges = Array.from(this.activeFilters.values()).map(filter => `
                <span class="badge bg-info d-flex align-items-center">
                    ${filter.displayName}
                    <button type="button" 
                            class="btn-close btn-close-white ms-2" 
                            data-filter-type="${filter.type}"
                            title="Remove filter"></button>
                </span>
            `).join('');

            filtersList.innerHTML = filterBadges;

            // Add event listeners for filter removal
            filtersList.querySelectorAll('.btn-close').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const filterType = e.target.getAttribute('data-filter-type');
                    this.removeFilter(filterType);
                });
            });
        }

        /**
         * Remove a specific filter
         * @param {string} filterType - Type of filter to remove
         */
        removeFilter(filterType) {
            this.activeFilters.delete(filterType);

            // Clear UI elements
            switch (filterType) {
                case 'pet':
                    const petInput = this.filterContainer.querySelector('#petFilter');
                    if (petInput) petInput.value = '';
                    break;
                case 'veterinarian':
                    const vetInput = this.filterContainer.querySelector('#veterinarianFilter');
                    if (vetInput) vetInput.value = '';
                    break;
                case 'status':
                    this.clearStatusFilter();
                    return; // clearStatusFilter already calls updateActiveFiltersDisplay and executeSearch
            }

            this.updateActiveFiltersDisplay();
            this.executeSearch();
        }

        /**
         * Execute search with current filters
         */
        async executeSearch() {
            try {
                this.showSearchLoading();

                let searchResults;

                if (this.activeFilters.size === 0) {
                    // No filters - get all visits
                    searchResults = await this.apiClient.getEntities('visits', {
                        page: 0,
                        size: 50,
                        useCache: false
                    });
                } else if (this.activeFilters.size === 1) {
                    // Single filter - use specific endpoint
                    const filter = Array.from(this.activeFilters.values())[0];
                    searchResults = await this.executeSingleFilterSearch(filter);
                } else {
                    // Multiple filters - use combined endpoint
                    searchResults = await this.executeCombinedSearch();
                }

                this.displaySearchResults(searchResults);
                this.updateResultCounts(searchResults);

            } catch (error) {
                console.error('Search execution failed:', error);
                this.showSearchError('Search failed. Please try again.');
            } finally {
                this.hideSearchLoading();
            }
        }

        /**
         * Execute single filter search
         * @param {Object} filter - Filter object
         * @returns {Promise<Object>} Search results
         */
        async executeSingleFilterSearch(filter) {
            const params = {
                page: 0,
                size: 50,
                useCache: false
            };

            switch (filter.type) {
                case 'pet':
                    params.petId = filter.id;
                    return await fetch(SEARCH_CONFIG.API_ENDPOINTS.VISITS_BY_PET + '?' + new URLSearchParams(params))
                        .then(response => response.json());

                case 'veterinarian':
                    params.veterinarianId = filter.id;
                    return await fetch(SEARCH_CONFIG.API_ENDPOINTS.VISITS_BY_VETERINARIAN + '?' + new URLSearchParams(params))
                        .then(response => response.json());

                case 'status':
                    // For multiple status values, use combined search
                    if (filter.values.length === 1) {
                        params.status = filter.values[0];
                        return await fetch(SEARCH_CONFIG.API_ENDPOINTS.VISITS_BY_STATUS + '?' + new URLSearchParams(params))
                            .then(response => response.json());
                    } else {
                        return await this.executeCombinedSearch();
                    }

                default:
                    throw new Error(`Unknown filter type: ${filter.type}`);
            }
        }

        /**
         * Execute combined search with multiple filters
         * @returns {Promise<Object>} Search results
         */
        async executeCombinedSearch() {
            const params = {
                page: 0,
                size: 50,
                useCache: false
            };

            // Add filter parameters
            this.activeFilters.forEach(filter => {
                switch (filter.type) {
                    case 'pet':
                        params.petId = filter.id;
                        break;
                    case 'veterinarian':
                        params.veterinarianId = filter.id;
                        break;
                    case 'status':
                        // For multiple status values, join with comma
                        params.status = filter.values.join(',');
                        break;
                }
            });

            return await fetch(SEARCH_CONFIG.API_ENDPOINTS.VISITS_COMBINED + '?' + new URLSearchParams(params))
                .then(response => response.json());
        }

        /**
         * Display search results in the table
         * @param {Object} results - Search results
         */
        displaySearchResults(results) {
            const table = this.container.querySelector('#visitsTable tbody');
            if (!table) return;

            const visits = results.visits || [];

            if (visits.length === 0) {
                table.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center py-5">
                            <i class="fas fa-search fa-3x text-muted mb-3"></i>
                            <h5 class="text-muted">No visits found</h5>
                            <p class="text-muted">Try adjusting your search filters.</p>
                        </td>
                    </tr>
                `;
                return;
            }

            const tableRows = visits.map(visit => this.buildVisitTableRow(visit)).join('');
            table.innerHTML = tableRows;

            // Apply search highlighting if enabled
            if (this.options.enableSearchHighlighting) {
                this.applySearchHighlighting(table);
            }

            // Update enhanced table if available
            const enhancedTable = this.container.querySelector('#visitsTable').enhancedTable;
            if (enhancedTable) {
                enhancedTable.bulkController.clearSelection();
                enhancedTable.bulkController.updateSelectionUI();
            }
        }

        /**
         * Build table row HTML for a visit
         * @param {Object} visit - Visit object
         * @returns {string} Table row HTML
         */
        buildVisitTableRow(visit) {
            const visitDate = new Date(visit.visitDate);
            const formattedDate = visitDate.toLocaleDateString();
            const formattedTime = visitDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            
            const petName = visit.pet ? visit.pet.name : 'Unknown Pet';
            const petId = visit.pet ? visit.pet.id : null;
            
            const vetName = visit.veterinarian ? `Dr. ${visit.veterinarian.lastName}` : 'Not assigned';
            
            const status = this.determineVisitStatus(visit);
            const statusBadge = this.getStatusBadge(status);
            
            const cost = visit.cost ? `$${visit.cost.toFixed(2)}` : '-';

            return `
                <tr data-id="${visit.id}">
                    <td class="select-column">
                        <input type="checkbox" class="form-check-input" data-id="${visit.id}">
                    </td>
                    <td>
                        <div>${formattedDate}</div>
                        <small class="text-muted">${formattedTime}</small>
                    </td>
                    <td>
                        ${petId ? `<a href="/pets/${petId}">${petName}</a>` : `<span class="text-muted">${petName}</span>`}
                    </td>
                    <td>
                        <div>${visit.description || 'No description'}</div>
                        ${visit.emergencyVisit ? '<span class="badge bg-danger">Emergency</span>' : ''}
                    </td>
                    <td>${vetName}</td>
                    <td>${statusBadge}</td>
                    <td>${cost}</td>
                    <td>
                        <div class="btn-group btn-group-sm">
                            <a href="/visits/${visit.id}" class="btn btn-outline-info btn-sm">
                                <i class="fas fa-eye"></i>
                            </a>
                            <a href="/visits/${visit.id}/edit" class="btn btn-outline-primary btn-sm">
                                <i class="fas fa-edit"></i>
                            </a>
                        </div>
                    </td>
                </tr>
            `;
        }

        /**
         * Determine visit status based on visit data
         * @param {Object} visit - Visit object
         * @returns {string} Status
         */
        determineVisitStatus(visit) {
            if (visit.emergencyVisit) return 'emergency';
            if (visit.diagnosis && visit.treatment) return 'completed';
            return 'pending';
        }

        /**
         * Get status badge HTML
         * @param {string} status - Status value
         * @returns {string} Badge HTML
         */
        getStatusBadge(status) {
            const statusConfig = SEARCH_CONFIG.STATUS_OPTIONS.find(s => s.value === status);
            if (statusConfig) {
                return `<span class="badge ${statusConfig.badge}">${statusConfig.label}</span>`;
            }
            return `<span class="badge bg-secondary">${status}</span>`;
        }

        /**
         * Update result counts display
         * @param {Object} results - Search results
         */
        updateResultCounts(results) {
            const summaryContainer = this.filterContainer.querySelector('.search-results-summary');
            if (!summaryContainer) return;

            summaryContainer.style.display = 'block';

            // Update count badges
            const totalCount = results.totalCount || 0;
            const completedCount = results.completedCount || 0;
            const pendingCount = results.pendingCount || 0;
            const emergencyCount = results.emergencyCount || 0;

            const totalBadge = summaryContainer.querySelector('.total-results');
            const completedBadge = summaryContainer.querySelector('.completed-count');
            const pendingBadge = summaryContainer.querySelector('.pending-count');
            const emergencyBadge = summaryContainer.querySelector('.emergency-count');

            if (totalBadge) totalBadge.textContent = `${totalCount} total`;
            if (completedBadge) completedBadge.textContent = `${completedCount} completed`;
            if (pendingBadge) pendingBadge.textContent = `${pendingCount} pending`;
            if (emergencyBadge) emergencyBadge.textContent = `${emergencyCount} emergency`;

            // Update status filter counts
            this.updateStatusFilterCounts(results);
        }

        /**
         * Update status filter dropdown counts
         * @param {Object} results - Search results
         */
        updateStatusFilterCounts(results) {
            const statusDropdown = this.filterContainer.querySelector('#statusFilterDropdown');
            if (!statusDropdown) return;

            const counts = {
                completed: results.completedCount || 0,
                pending: results.pendingCount || 0,
                emergency: results.emergencyCount || 0,
                cancelled: results.cancelledCount || 0
            };

            Object.entries(counts).forEach(([status, count]) => {
                const countElement = statusDropdown.querySelector(`#status_${status} + label .status-count`);
                if (countElement) {
                    countElement.textContent = `(${count})`;
                }
            });
        }

        /**
         * Apply search highlighting to table content
         * @param {HTMLElement} table - Table element
         */
        applySearchHighlighting(table) {
            // Get search terms from active filters
            const searchTerms = [];
            
            this.activeFilters.forEach(filter => {
                if (filter.type === 'pet' || filter.type === 'veterinarian') {
                    searchTerms.push(filter.name);
                }
            });

            if (searchTerms.length === 0) return;

            // Apply highlighting to table cells
            const cells = table.querySelectorAll('td');
            cells.forEach(cell => {
                let cellHTML = cell.innerHTML;
                
                searchTerms.forEach(term => {
                    const regex = new RegExp(`(${this.escapeRegExp(term)})`, 'gi');
                    cellHTML = cellHTML.replace(regex, '<mark class="search-highlight">$1</mark>');
                });
                
                cell.innerHTML = cellHTML;
            });
        }

        /**
         * Highlight text with search query
         * @param {string} text - Text to highlight
         * @param {string} query - Search query
         * @returns {string} Highlighted text
         */
        highlightText(text, query) {
            if (!query || query.length < 2) return text;
            
            const regex = new RegExp(`(${this.escapeRegExp(query)})`, 'gi');
            return text.replace(regex, '<mark class="search-highlight">$1</mark>');
        }

        /**
         * Escape special regex characters
         * @param {string} string - String to escape
         * @returns {string} Escaped string
         */
        escapeRegExp(string) {
            return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        }

        /**
         * Show search loading state
         */
        showSearchLoading() {
            const table = this.container.querySelector('#visitsTable tbody');
            if (table) {
                table.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center py-5">
                            <div class="spinner-border text-primary" role="status">
                                <span class="visually-hidden">Searching...</span>
                            </div>
                            <div class="mt-2">Searching visits...</div>
                        </td>
                    </tr>
                `;
            }
        }

        /**
         * Hide search loading state
         */
        hideSearchLoading() {
            // Loading state is replaced by results or error message
        }

        /**
         * Show search error
         * @param {string} message - Error message
         */
        showSearchError(message) {
            const table = this.container.querySelector('#visitsTable tbody');
            if (table) {
                table.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center py-5">
                            <i class="fas fa-exclamation-triangle fa-3x text-danger mb-3"></i>
                            <h5 class="text-danger">Search Error</h5>
                            <p class="text-muted">${message}</p>
                            <button type="button" class="btn btn-outline-primary" onclick="this.closest('.visit-search-filters').visitSearchManager.executeSearch()">
                                <i class="fas fa-redo me-1"></i>Retry Search
                            </button>
                        </td>
                    </tr>
                `;
            }
        }

        /**
         * Show general error message
         * @param {string} message - Error message
         */
        showError(message) {
            // Create or update error container
            let errorContainer = this.container.querySelector('.visit-search-error');
            if (!errorContainer) {
                errorContainer = document.createElement('div');
                errorContainer.className = 'visit-search-error alert alert-warning alert-dismissible fade show';
                this.container.insertBefore(errorContainer, this.container.firstChild);
            }

            errorContainer.innerHTML = `
                <i class="fas fa-exclamation-triangle me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
        }

        /**
         * Load initial data (pets and veterinarians for dropdowns)
         */
        async loadInitialData() {
            try {
                // Pre-load some pets and veterinarians for better UX
                const [petsResponse, vetsResponse] = await Promise.all([
                    this.apiClient.getEntities('pets', { page: 0, size: 20, useCache: true }),
                    this.apiClient.getEntities('veterinarians', { page: 0, size: 20, useCache: true })
                ]);

                // Cache the results
                this.searchCache.set('pets_initial', petsResponse.content || petsResponse);
                this.searchCache.set('veterinarians_initial', vetsResponse.content || vetsResponse);

            } catch (error) {
                console.warn('Failed to load initial data:', error);
                // Non-critical error - search will still work
            }
        }

        /**
         * Export search results
         */
        async exportResults() {
            try {
                // Get current search results
                const results = await this.executeSearch();
                
                // Create CSV content
                const csvContent = this.generateCSV(results.visits || []);
                
                // Download CSV file
                const blob = new Blob([csvContent], { type: 'text/csv' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `visit-search-results-${new Date().toISOString().split('T')[0]}.csv`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);

            } catch (error) {
                console.error('Export failed:', error);
                this.showError('Export failed. Please try again.');
            }
        }

        /**
         * Generate CSV content from visits data
         * @param {Array} visits - Visits array
         * @returns {string} CSV content
         */
        generateCSV(visits) {
            const headers = ['Date', 'Time', 'Pet', 'Description', 'Veterinarian', 'Status', 'Cost'];
            const csvRows = [headers.join(',')];

            visits.forEach(visit => {
                const visitDate = new Date(visit.visitDate);
                const row = [
                    visitDate.toLocaleDateString(),
                    visitDate.toLocaleTimeString(),
                    visit.pet ? visit.pet.name : 'Unknown Pet',
                    `"${(visit.description || '').replace(/"/g, '""')}"`,
                    visit.veterinarian ? `Dr. ${visit.veterinarian.lastName}` : 'Not assigned',
                    this.determineVisitStatus(visit),
                    visit.cost ? visit.cost.toFixed(2) : '0.00'
                ];
                csvRows.push(row.join(','));
            });

            return csvRows.join('\n');
        }

        /**
         * Destroy the search filter manager
         */
        destroy() {
            // Clear timers
            this.debounceTimers.forEach(timer => clearTimeout(timer));
            this.debounceTimers.clear();

            // Clear cache
            this.searchCache.clear();

            // Remove UI
            if (this.filterContainer) {
                this.filterContainer.remove();
            }

            // Clear references
            this.activeFilters.clear();
        }
    }

    // Export to global scope
    window.VisitSearchFilterManager = VisitSearchFilterManager;

    // Auto-initialize on visits list page
    document.addEventListener('DOMContentLoaded', function() {
        const visitsContainer = document.querySelector('.visits-container, [data-page="visits"]');
        const visitsTable = document.getElementById('visitsTable');
        
        if (visitsTable && window.location.pathname.includes('/visits')) {
            try {
                const container = visitsContainer || visitsTable.closest('.container, .container-fluid') || document.body;
                const searchManager = new VisitSearchFilterManager(container);
                
                // Store reference for potential later access
                container.visitSearchManager = searchManager;
                
                console.log('VisitSearchFilterManager auto-initialized successfully');
            } catch (error) {
                console.error('Failed to auto-initialize VisitSearchFilterManager:', error);
            }
        }
    });

})();