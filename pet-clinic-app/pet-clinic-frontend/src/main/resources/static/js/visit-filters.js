/**
 * Visit Filter Manager
 * Comprehensive filtering functionality for visits page
 * 
 * Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5
 */

(function() {
    'use strict';

    /**
     * Visit Filter Manager Class
     * Manages all visit filtering functionality including main filters and status filters
     */
    class VisitFilterManager {
        constructor(container, options = {}) {
            this.container = container;
            this.options = {
                apiBaseUrl: '/api/visits/filter',
                enableDateRangeFilter: true,
                enableVisitTypeFilter: true,
                enableStatusFilter: true,
                enablePetFilter: true,
                enableVeterinarianFilter: true,
                enableImmediateApplication: true,
                persistFilters: true,
                ...options
            };

            // State management
            this.activeFilters = new Map();
            this.filterValues = new Map();
            this.isLoading = false;
            this.lastAppliedFilters = null;

            // UI elements
            this.mainFilterDropdown = null;
            this.statusFilterDropdown = null;
            this.activeFiltersContainer = null;
            this.resultsContainer = null;

            this.init();
        }

        async init() {
            try {
                this.createFilterUI();
                this.setupEventListeners();
                await this.loadFilterValues();
                this.restorePersistedFilters();
                
                console.log('VisitFilterManager initialized successfully');
            } catch (error) {
                console.error('Failed to initialize VisitFilterManager:', error);
                this.showError('Filter functionality is temporarily unavailable');
            }
        }

        /**
         * Create the filter UI components
         */
        createFilterUI() {
            // Find or create main filter dropdown
            this.mainFilterDropdown = this.container.querySelector('#mainFilterDropdown') || 
                                     this.createMainFilterDropdown();

            // Find or create status filter dropdown
            this.statusFilterDropdown = this.container.querySelector('#statusFilterDropdown') || 
                                       this.createStatusFilterDropdown();

            // Find or create active filters container
            this.activeFiltersContainer = this.container.querySelector('#activeFilters') || 
                                         this.createActiveFiltersContainer();

            // Find results container
            this.resultsContainer = this.container.querySelector('#visitsTable') || 
                                   this.container.querySelector('.table-responsive');
        }

        /**
         * Create main filter dropdown
         */
        createMainFilterDropdown() {
            const filterContainer = this.container.querySelector('.d-flex.justify-content-between.align-items-center') ||
                                   this.container.querySelector('.visits-container > div:first-child');

            if (!filterContainer) {
                console.warn('Could not find container for main filter dropdown');
                return null;
            }

            const buttonGroup = filterContainer.querySelector('.btn-group') || 
                               document.createElement('div');
            
            if (!buttonGroup.classList.contains('btn-group')) {
                buttonGroup.className = 'btn-group ms-2';
                buttonGroup.setAttribute('role', 'group');
                filterContainer.appendChild(buttonGroup);
            }

            const mainFilterHTML = `
                <button type="button" class="btn btn-outline-primary dropdown-toggle" 
                        data-bs-toggle="dropdown" id="mainFilterDropdown">
                    <i class="fas fa-filter"></i> Main Filters
                </button>
                <ul class="dropdown-menu main-filter-dropdown">
                    <li><h6 class="dropdown-header">Filter by Date Range</h6></li>
                    <li class="px-3 py-2">
                        <div class="row g-2">
                            <div class="col-6">
                                <label for="startDateFilter" class="form-label small">Start Date</label>
                                <input type="date" class="form-control form-control-sm" id="startDateFilter">
                            </div>
                            <div class="col-6">
                                <label for="endDateFilter" class="form-label small">End Date</label>
                                <input type="date" class="form-control form-control-sm" id="endDateFilter">
                            </div>
                        </div>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li><h6 class="dropdown-header">Filter by Visit Type</h6></li>
                    <li class="px-3 py-2">
                        <select class="form-select form-select-sm" id="visitTypeFilter">
                            <option value="">All Visit Types</option>
                        </select>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li><h6 class="dropdown-header">Filter by Pet/Veterinarian</h6></li>
                    <li class="px-3 py-2">
                        <div class="row g-2">
                            <div class="col-6">
                                <label for="petFilter" class="form-label small">Pet</label>
                                <select class="form-select form-select-sm" id="petFilter">
                                    <option value="">All Pets</option>
                                </select>
                            </div>
                            <div class="col-6">
                                <label for="veterinarianFilter" class="form-label small">Veterinarian</label>
                                <select class="form-select form-select-sm" id="veterinarianFilter">
                                    <option value="">All Veterinarians</option>
                                </select>
                            </div>
                        </div>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li class="px-3 py-2">
                        <div class="d-flex gap-2">
                            <button type="button" class="btn btn-primary btn-sm" id="applyMainFilters">
                                <i class="fas fa-check"></i> Apply Filters
                            </button>
                            <button type="button" class="btn btn-outline-secondary btn-sm" id="clearMainFilters">
                                <i class="fas fa-times"></i> Clear All
                            </button>
                        </div>
                    </li>
                </ul>
            `;

            buttonGroup.insertAdjacentHTML('beforeend', mainFilterHTML);
            return buttonGroup.querySelector('#mainFilterDropdown');
        }

        /**
         * Create status filter dropdown
         */
        createStatusFilterDropdown() {
            const existingStatusFilter = this.container.querySelector('#statusFilterDropdown');
            if (existingStatusFilter) {
                return existingStatusFilter;
            }

            const filterContainer = this.container.querySelector('.d-flex.justify-content-between.align-items-center') ||
                                   this.container.querySelector('.visits-container > div:first-child');

            if (!filterContainer) {
                console.warn('Could not find container for status filter dropdown');
                return null;
            }

            const buttonGroup = document.createElement('div');
            buttonGroup.className = 'btn-group ms-2';
            buttonGroup.setAttribute('role', 'group');

            const statusFilterHTML = `
                <button type="button" class="btn btn-outline-secondary dropdown-toggle" 
                        data-bs-toggle="dropdown" id="statusFilterDropdown">
                    <i class="fas fa-tags"></i> Status Filter
                </button>
                <ul class="dropdown-menu status-filter-dropdown">
                    <li><h6 class="dropdown-header">Filter by Status</h6></li>
                    <li>
                        <a class="dropdown-item status-filter-item" href="#" data-status="">
                            <span class="status-badge badge bg-light text-dark">All</span>
                            All Statuses
                        </a>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                        <a class="dropdown-item status-filter-item" href="#" data-status="completed">
                            <span class="status-badge badge bg-success">Completed</span>
                            Completed Visits
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item status-filter-item" href="#" data-status="pending">
                            <span class="status-badge badge bg-warning">Pending</span>
                            Pending Visits
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item status-filter-item" href="#" data-status="emergency">
                            <span class="status-badge badge bg-danger">Emergency</span>
                            Emergency Visits
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item status-filter-item" href="#" data-status="cancelled">
                            <span class="status-badge badge bg-secondary">Cancelled</span>
                            Cancelled Visits
                        </a>
                    </li>
                </ul>
            `;

            buttonGroup.innerHTML = statusFilterHTML;
            filterContainer.appendChild(buttonGroup);
            
            return buttonGroup.querySelector('#statusFilterDropdown');
        }

        /**
         * Create active filters container
         */
        createActiveFiltersContainer() {
            const existingContainer = this.container.querySelector('#activeFilters');
            if (existingContainer) {
                return existingContainer;
            }

            const container = document.createElement('div');
            container.id = 'activeFilters';
            container.className = 'active-filters mb-3';
            container.style.display = 'none';

            // Insert after the header section
            const headerSection = this.container.querySelector('.d-flex.justify-content-between.align-items-center');
            if (headerSection) {
                headerSection.insertAdjacentElement('afterend', container);
            } else {
                this.container.insertBefore(container, this.container.firstChild);
            }

            return container;
        }

        /**
         * Setup event listeners
         */
        setupEventListeners() {
            // Main filter dropdown events
            this.setupMainFilterEvents();

            // Status filter dropdown events
            this.setupStatusFilterEvents();

            // Active filter removal events
            this.setupActiveFilterEvents();

            // Form submission prevention
            this.preventFormSubmission();
        }

        /**
         * Setup main filter dropdown events
         */
        setupMainFilterEvents() {
            if (!this.mainFilterDropdown) return;

            const applyButton = this.container.querySelector('#applyMainFilters');
            const clearButton = this.container.querySelector('#clearMainFilters');

            if (applyButton) {
                applyButton.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.applyMainFilters();
                });
            }

            if (clearButton) {
                clearButton.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.clearMainFilters();
                });
            }

            // Immediate application for certain filters if enabled
            if (this.options.enableImmediateApplication) {
                const immediateFilters = ['#startDateFilter', '#endDateFilter', '#visitTypeFilter'];
                immediateFilters.forEach(selector => {
                    const element = this.container.querySelector(selector);
                    if (element) {
                        element.addEventListener('change', () => {
                            this.debounce(() => this.applyMainFilters(), 500)();
                        });
                    }
                });
            }
        }

        /**
         * Setup status filter dropdown events
         */
        setupStatusFilterEvents() {
            if (!this.statusFilterDropdown) return;

            const statusFilterItems = this.container.querySelectorAll('.status-filter-item');
            
            statusFilterItems.forEach(item => {
                item.addEventListener('click', (e) => {
                    e.preventDefault();
                    
                    const status = item.getAttribute('data-status');
                    const statusText = item.textContent.trim();
                    
                    this.applyStatusFilter(status, statusText);
                });
            });
        }

        /**
         * Setup active filter removal events
         */
        setupActiveFilterEvents() {
            if (!this.activeFiltersContainer) return;

            // Use event delegation for dynamically created filter badges
            this.activeFiltersContainer.addEventListener('click', (e) => {
                if (e.target.classList.contains('filter-remove-btn') || 
                    e.target.closest('.filter-remove-btn')) {
                    
                    const filterBadge = e.target.closest('.filter-badge');
                    if (filterBadge) {
                        const filterKey = filterBadge.getAttribute('data-filter-key');
                        this.removeFilter(filterKey);
                    }
                }

                if (e.target.classList.contains('clear-all-filters-btn')) {
                    this.clearAllFilters();
                }
            });
        }

        /**
         * Prevent form submission to avoid page reload
         */
        preventFormSubmission() {
            const forms = this.container.querySelectorAll('form');
            forms.forEach(form => {
                form.addEventListener('submit', (e) => {
                    e.preventDefault();
                    // Handle form data as filter application instead
                    this.applyMainFilters();
                });
            });
        }

        /**
         * Load available filter values from the backend
         */
        async loadFilterValues() {
            try {
                const filterTypes = ['status', 'visitType', 'pets', 'veterinarians'];
                
                for (const filterType of filterTypes) {
                    try {
                        const response = await fetch(`${this.options.apiBaseUrl}/values/${filterType}`);
                        if (response.ok) {
                            const values = await response.json();
                            this.filterValues.set(filterType, values);
                            this.populateFilterDropdown(filterType, values);
                        }
                    } catch (error) {
                        console.warn(`Failed to load ${filterType} filter values:`, error);
                    }
                }

            } catch (error) {
                console.error('Error loading filter values:', error);
            }
        }

        /**
         * Populate filter dropdown with values
         */
        populateFilterDropdown(filterType, values) {
            let selector;
            
            switch (filterType) {
                case 'visitType':
                    selector = '#visitTypeFilter';
                    break;
                case 'pets':
                    selector = '#petFilter';
                    break;
                case 'veterinarians':
                    selector = '#veterinarianFilter';
                    break;
                default:
                    return;
            }

            const dropdown = this.container.querySelector(selector);
            if (!dropdown) return;

            // Clear existing options (except the first "All" option)
            while (dropdown.children.length > 1) {
                dropdown.removeChild(dropdown.lastChild);
            }

            // Add new options
            values.forEach(value => {
                if (value && value !== 'all') {
                    const option = document.createElement('option');
                    option.value = value;
                    option.textContent = value;
                    dropdown.appendChild(option);
                }
            });
        }

        /**
         * Apply main filters
         */
        async applyMainFilters() {
            try {
                this.showLoading(true);

                const filters = this.collectMainFilterValues();
                
                // Update active filters
                Object.entries(filters).forEach(([key, value]) => {
                    if (value) {
                        this.activeFilters.set(key, {
                            value: value,
                            displayName: this.getFilterDisplayName(key, value)
                        });
                    } else {
                        this.activeFilters.delete(key);
                    }
                });

                // Apply filters
                await this.executeFilters();
                
                // Update UI
                this.updateActiveFiltersDisplay();
                this.updateMainFilterButton();
                this.persistFilters();

                // Close dropdown
                this.closeDropdown(this.mainFilterDropdown);

            } catch (error) {
                console.error('Error applying main filters:', error);
                this.showError('Failed to apply filters. Please try again.');
            } finally {
                this.showLoading(false);
            }
        }

        /**
         * Apply status filter
         */
        async applyStatusFilter(status, statusText) {
            try {
                this.showLoading(true);

                if (status) {
                    this.activeFilters.set('status', {
                        value: status,
                        displayName: statusText
                    });
                } else {
                    this.activeFilters.delete('status');
                }

                // Apply filters
                await this.executeFilters();
                
                // Update UI
                this.updateActiveFiltersDisplay();
                this.updateStatusFilterButton(status, statusText);
                this.persistFilters();

                // Close dropdown
                this.closeDropdown(this.statusFilterDropdown);

            } catch (error) {
                console.error('Error applying status filter:', error);
                this.showError('Failed to apply status filter. Please try again.');
            } finally {
                this.showLoading(false);
            }
        }

        /**
         * Collect main filter values from form inputs
         */
        collectMainFilterValues() {
            const filters = {};

            const startDate = this.container.querySelector('#startDateFilter')?.value;
            const endDate = this.container.querySelector('#endDateFilter')?.value;
            const visitType = this.container.querySelector('#visitTypeFilter')?.value;
            const petId = this.container.querySelector('#petFilter')?.value;
            const veterinarianId = this.container.querySelector('#veterinarianFilter')?.value;

            if (startDate) filters.startDate = startDate;
            if (endDate) filters.endDate = endDate;
            if (visitType) filters.visitType = visitType;
            if (petId) filters.petId = petId;
            if (veterinarianId) filters.veterinarianId = veterinarianId;

            return filters;
        }

        /**
         * Execute filters by calling the backend API
         */
        async executeFilters() {
            try {
                const filterData = {
                    page: 0,
                    size: 20,
                    sortBy: 'visitDate',
                    sortDir: 'desc'
                };

                // Add active filters to request
                this.activeFilters.forEach((filterInfo, key) => {
                    filterData[key] = filterInfo.value;
                });

                const response = await fetch(`${this.options.apiBaseUrl}/apply`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: JSON.stringify(filterData)
                });

                if (!response.ok) {
                    throw new Error(`Filter request failed: ${response.status}`);
                }

                const result = await response.json();
                this.updateResultsDisplay(result);
                this.lastAppliedFilters = new Map(this.activeFilters);

            } catch (error) {
                console.error('Error executing filters:', error);
                throw error;
            }
        }

        /**
         * Update results display with filtered data
         */
        updateResultsDisplay(result) {
            if (!this.resultsContainer) return;

            try {
                // Update table body with new results
                const tbody = this.resultsContainer.querySelector('tbody');
                if (tbody && result.content) {
                    this.updateTableRows(tbody, result.content);
                }

                // Update pagination if present
                this.updatePagination(result.page);

                // Update result count
                this.updateResultCount(result.page?.totalElements || result.content?.length || 0);

                // Show success message
                this.showSuccess(`Found ${result.page?.totalElements || result.content?.length || 0} visits`);

            } catch (error) {
                console.error('Error updating results display:', error);
            }
        }

        /**
         * Update table rows with new data
         */
        updateTableRows(tbody, visits) {
            tbody.innerHTML = '';

            if (!visits || visits.length === 0) {
                const emptyRow = document.createElement('tr');
                emptyRow.innerHTML = `
                    <td colspan="8" class="text-center py-4">
                        <i class="fas fa-search fa-2x text-muted mb-2"></i>
                        <p class="text-muted mb-0">No visits found matching your filters</p>
                    </td>
                `;
                tbody.appendChild(emptyRow);
                return;
            }

            visits.forEach(visit => {
                const row = this.createVisitRow(visit);
                tbody.appendChild(row);
            });
        }

        /**
         * Create a table row for a visit
         */
        createVisitRow(visit) {
            const row = document.createElement('tr');
            row.setAttribute('data-id', visit.id);

            const visitDate = new Date(visit.visitDate);
            const formattedDate = visitDate.toLocaleDateString('en-US', { 
                month: 'short', day: 'numeric', year: 'numeric' 
            });
            const formattedTime = visitDate.toLocaleTimeString('en-US', { 
                hour: '2-digit', minute: '2-digit' 
            });

            const petName = visit.pet?.name || 'Unknown Pet';
            const vetName = visit.veterinarian ? `Dr. ${visit.veterinarian.lastName}` : 'Not assigned';
            const status = this.getVisitStatus(visit);
            const cost = visit.cost ? `$${visit.cost.toFixed(2)}` : '-';

            row.innerHTML = `
                <td class="select-column">
                    <input type="checkbox" class="form-check-input" data-id="${visit.id}" 
                           aria-label="Select visit for ${petName}">
                </td>
                <td>
                    <div>${formattedDate}</div>
                    <small class="text-muted">${formattedTime}</small>
                </td>
                <td>
                    <a href="/pets/${visit.pet?.id || '#'}">${petName}</a>
                </td>
                <td>
                    <div>${visit.description || 'No description'}</div>
                    ${visit.emergencyVisit ? '<span class="badge bg-danger">Emergency</span>' : ''}
                </td>
                <td>${vetName}</td>
                <td>
                    <span class="badge ${status.class}" data-status="${status.value}">${status.text}</span>
                </td>
                <td>${cost}</td>
                <td>
                    <div class="btn-group btn-group-sm" role="group">
                        <a href="/visits/${visit.id}" class="btn btn-outline-info btn-action" title="View Details">
                            <i class="fas fa-eye"></i> <span class="d-none d-md-inline">View</span>
                        </a>
                        <a href="/visits/${visit.id}/edit" class="btn btn-outline-primary btn-action" title="Edit">
                            <i class="fas fa-edit"></i> <span class="d-none d-md-inline">Edit</span>
                        </a>
                        <button type="button" class="btn btn-outline-danger btn-action btn-delete" 
                                title="Delete" data-visit-id="${visit.id}">
                            <i class="fas fa-trash"></i> <span class="d-none d-md-inline">Delete</span>
                        </button>
                    </div>
                </td>
            `;

            return row;
        }

        /**
         * Get visit status information
         */
        getVisitStatus(visit) {
            try {
                if (visit.completed === true) {
                    return { value: 'completed', text: 'Completed', class: 'bg-success' };
                } else if (visit.completed === false) {
                    return { value: 'pending', text: 'Pending', class: 'bg-warning' };
                } else if (visit.visitType && visit.visitType.emergency) {
                    return { value: 'emergency', text: 'Emergency', class: 'bg-danger' };
                } else {
                    return { value: 'unknown', text: 'Unknown', class: 'bg-secondary' };
                }
            } catch (error) {
                return { value: 'unknown', text: 'Unknown', class: 'bg-secondary' };
            }
        }

        /**
         * Update active filters display
         */
        updateActiveFiltersDisplay() {
            if (!this.activeFiltersContainer) return;

            if (this.activeFilters.size === 0) {
                this.activeFiltersContainer.style.display = 'none';
                return;
            }

            this.activeFiltersContainer.style.display = 'block';

            const filtersHTML = Array.from(this.activeFilters.entries())
                .map(([key, filterInfo]) => `
                    <span class="badge bg-primary filter-badge me-2 mb-2" data-filter-key="${key}">
                        ${filterInfo.displayName}
                        <button type="button" class="btn-close btn-close-white ms-1 filter-remove-btn" 
                                aria-label="Remove ${filterInfo.displayName} filter"></button>
                    </span>
                `).join('');

            const clearAllButton = this.activeFilters.size > 1 ? `
                <button type="button" class="btn btn-outline-secondary btn-sm clear-all-filters-btn">
                    <i class="fas fa-times"></i> Clear All Filters
                </button>
            ` : '';

            this.activeFiltersContainer.innerHTML = `
                <div class="d-flex align-items-center flex-wrap">
                    <span class="me-2 text-muted">Active Filters:</span>
                    ${filtersHTML}
                    ${clearAllButton}
                </div>
            `;
        }

        /**
         * Update main filter button text
         */
        updateMainFilterButton() {
            if (!this.mainFilterDropdown) return;

            const mainFilterCount = Array.from(this.activeFilters.keys())
                .filter(key => key !== 'status').length;

            const buttonText = mainFilterCount > 0 ? 
                `Main Filters (${mainFilterCount})` : 'Main Filters';

            this.mainFilterDropdown.innerHTML = `<i class="fas fa-filter"></i> ${buttonText}`;
        }

        /**
         * Update status filter button text
         */
        updateStatusFilterButton(status, statusText) {
            if (!this.statusFilterDropdown) return;

            const buttonText = status ? `Status: ${statusText}` : 'Status Filter';
            this.statusFilterDropdown.innerHTML = `<i class="fas fa-tags"></i> ${buttonText}`;
        }

        /**
         * Remove a specific filter
         */
        async removeFilter(filterKey) {
            try {
                this.showLoading(true);

                this.activeFilters.delete(filterKey);

                // Clear the corresponding form input
                this.clearFilterInput(filterKey);

                // Re-apply remaining filters
                await this.executeFilters();

                // Update UI
                this.updateActiveFiltersDisplay();
                this.updateMainFilterButton();
                
                if (filterKey === 'status') {
                    this.updateStatusFilterButton('', '');
                }

                this.persistFilters();

            } catch (error) {
                console.error('Error removing filter:', error);
                this.showError('Failed to remove filter. Please try again.');
            } finally {
                this.showLoading(false);
            }
        }

        /**
         * Clear all filters
         */
        async clearAllFilters() {
            try {
                this.showLoading(true);

                this.activeFilters.clear();
                this.clearAllFilterInputs();

                // Load all visits without filters
                await this.executeFilters();

                // Update UI
                this.updateActiveFiltersDisplay();
                this.updateMainFilterButton();
                this.updateStatusFilterButton('', '');
                this.persistFilters();

            } catch (error) {
                console.error('Error clearing all filters:', error);
                this.showError('Failed to clear filters. Please try again.');
            } finally {
                this.showLoading(false);
            }
        }

        /**
         * Clear main filters only
         */
        async clearMainFilters() {
            try {
                this.showLoading(true);

                // Remove all filters except status
                const statusFilter = this.activeFilters.get('status');
                this.activeFilters.clear();
                if (statusFilter) {
                    this.activeFilters.set('status', statusFilter);
                }

                this.clearMainFilterInputs();

                // Re-apply remaining filters
                await this.executeFilters();

                // Update UI
                this.updateActiveFiltersDisplay();
                this.updateMainFilterButton();
                this.persistFilters();

                // Close dropdown
                this.closeDropdown(this.mainFilterDropdown);

            } catch (error) {
                console.error('Error clearing main filters:', error);
                this.showError('Failed to clear main filters. Please try again.');
            } finally {
                this.showLoading(false);
            }
        }

        /**
         * Clear filter input for specific key
         */
        clearFilterInput(filterKey) {
            const inputMap = {
                'startDate': '#startDateFilter',
                'endDate': '#endDateFilter',
                'visitType': '#visitTypeFilter',
                'petId': '#petFilter',
                'veterinarianId': '#veterinarianFilter'
            };

            const selector = inputMap[filterKey];
            if (selector) {
                const input = this.container.querySelector(selector);
                if (input) {
                    input.value = '';
                }
            }
        }

        /**
         * Clear all filter inputs
         */
        clearAllFilterInputs() {
            this.clearMainFilterInputs();
        }

        /**
         * Clear main filter inputs
         */
        clearMainFilterInputs() {
            const inputs = [
                '#startDateFilter', '#endDateFilter', '#visitTypeFilter', 
                '#petFilter', '#veterinarianFilter'
            ];

            inputs.forEach(selector => {
                const input = this.container.querySelector(selector);
                if (input) {
                    input.value = '';
                }
            });
        }

        /**
         * Get filter display name
         */
        getFilterDisplayName(key, value) {
            switch (key) {
                case 'status':
                    return `Status: ${value.charAt(0).toUpperCase() + value.slice(1)}`;
                case 'visitType':
                    return `Type: ${value}`;
                case 'startDate':
                    return `From: ${new Date(value).toLocaleDateString()}`;
                case 'endDate':
                    return `To: ${new Date(value).toLocaleDateString()}`;
                case 'petId':
                    return `Pet: ${value}`;
                case 'veterinarianId':
                    return `Vet: ${value}`;
                default:
                    return `${key}: ${value}`;
            }
        }

        /**
         * Persist filters to localStorage
         */
        persistFilters() {
            if (!this.options.persistFilters) return;

            try {
                const filtersData = {};
                this.activeFilters.forEach((filterInfo, key) => {
                    filtersData[key] = filterInfo.value;
                });

                localStorage.setItem('visitFilters', JSON.stringify(filtersData));
            } catch (error) {
                console.warn('Failed to persist filters:', error);
            }
        }

        /**
         * Restore persisted filters
         */
        restorePersistedFilters() {
            if (!this.options.persistFilters) return;

            try {
                const filtersData = localStorage.getItem('visitFilters');
                if (filtersData) {
                    const filters = JSON.parse(filtersData);
                    
                    Object.entries(filters).forEach(([key, value]) => {
                        if (value) {
                            this.activeFilters.set(key, {
                                value: value,
                                displayName: this.getFilterDisplayName(key, value)
                            });

                            // Set form input values
                            this.setFilterInputValue(key, value);
                        }
                    });

                    // Apply restored filters
                    if (this.activeFilters.size > 0) {
                        setTimeout(() => this.executeFilters(), 100);
                    }
                }
            } catch (error) {
                console.warn('Failed to restore persisted filters:', error);
            }
        }

        /**
         * Set filter input value
         */
        setFilterInputValue(key, value) {
            const inputMap = {
                'startDate': '#startDateFilter',
                'endDate': '#endDateFilter',
                'visitType': '#visitTypeFilter',
                'petId': '#petFilter',
                'veterinarianId': '#veterinarianFilter'
            };

            const selector = inputMap[key];
            if (selector) {
                const input = this.container.querySelector(selector);
                if (input) {
                    input.value = value;
                }
            }
        }

        /**
         * Update pagination display
         */
        updatePagination(pageInfo) {
            if (!pageInfo) return;

            const paginationContainer = this.container.querySelector('.pagination');
            if (!paginationContainer) return;

            // Update pagination based on pageInfo
            // This would be implemented based on the specific pagination structure
        }

        /**
         * Update result count display
         */
        updateResultCount(count) {
            const countElements = this.container.querySelectorAll('.result-count, .visit-count');
            countElements.forEach(element => {
                element.textContent = count;
            });

            // Update the results info paragraph
            const resultsInfo = this.container.querySelector('.text-muted');
            if (resultsInfo && resultsInfo.textContent.includes('visit(s) found')) {
                resultsInfo.innerHTML = `${count} visit(s) found`;
            }
        }

        /**
         * Close dropdown
         */
        closeDropdown(button) {
            if (!button) return;

            try {
                const dropdown = bootstrap.Dropdown.getInstance(button);
                if (dropdown) {
                    dropdown.hide();
                }
            } catch (error) {
                console.warn('Failed to close dropdown:', error);
            }
        }

        /**
         * Show loading state
         */
        showLoading(show) {
            this.isLoading = show;

            // Update button states
            const buttons = this.container.querySelectorAll('button[id*="Filter"]');
            buttons.forEach(button => {
                button.disabled = show;
                if (show) {
                    button.classList.add('loading');
                } else {
                    button.classList.remove('loading');
                }
            });

            // Show/hide loading indicator
            if (show) {
                this.showMessage('Applying filters...', 'info');
            }
        }

        /**
         * Show success message
         */
        showSuccess(message) {
            this.showMessage(message, 'success');
        }

        /**
         * Show error message
         */
        showError(message) {
            this.showMessage(message, 'error');
        }

        /**
         * Show message
         */
        showMessage(message, type) {
            // Remove existing messages
            const existingMessages = this.container.querySelectorAll('.filter-message');
            existingMessages.forEach(msg => msg.remove());

            // Create new message
            const messageDiv = document.createElement('div');
            messageDiv.className = `alert alert-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'info'} alert-dismissible fade show filter-message`;
            messageDiv.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;

            // Insert message
            const insertPoint = this.activeFiltersContainer || 
                               this.container.querySelector('.card') || 
                               this.container.firstElementChild;
            
            if (insertPoint) {
                insertPoint.insertAdjacentElement('beforebegin', messageDiv);
            }

            // Auto-remove success messages
            if (type === 'success') {
                setTimeout(() => {
                    if (messageDiv.parentNode) {
                        messageDiv.remove();
                    }
                }, 3000);
            }
        }

        /**
         * Debounce function
         */
        debounce(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        }

        /**
         * Get current active filters
         */
        getActiveFilters() {
            return new Map(this.activeFilters);
        }

        /**
         * Set filters programmatically
         */
        async setFilters(filters) {
            try {
                this.activeFilters.clear();
                
                Object.entries(filters).forEach(([key, value]) => {
                    if (value) {
                        this.activeFilters.set(key, {
                            value: value,
                            displayName: this.getFilterDisplayName(key, value)
                        });
                        this.setFilterInputValue(key, value);
                    }
                });

                await this.executeFilters();
                this.updateActiveFiltersDisplay();
                this.updateMainFilterButton();
                
                if (filters.status) {
                    this.updateStatusFilterButton(filters.status, this.getFilterDisplayName('status', filters.status));
                }

            } catch (error) {
                console.error('Error setting filters:', error);
                throw error;
            }
        }

        /**
         * Refresh filters (re-apply current filters)
         */
        async refreshFilters() {
            try {
                await this.executeFilters();
                this.showSuccess('Filters refreshed');
            } catch (error) {
                console.error('Error refreshing filters:', error);
                this.showError('Failed to refresh filters');
            }
        }

        /**
         * Destroy the filter manager
         */
        destroy() {
            // Remove event listeners
            // Clear persisted data if needed
            // Clean up UI elements
            
            if (this.options.persistFilters) {
                localStorage.removeItem('visitFilters');
            }
            
            console.log('VisitFilterManager destroyed');
        }
    }

    // Export to global scope
    window.VisitFilterManager = VisitFilterManager;

    // Auto-initialize on visits page
    document.addEventListener('DOMContentLoaded', function() {
        const visitsContainer = document.querySelector('.visits-container, [data-page="visits"]');
        
        if (visitsContainer) {
            try {
                const filterManager = new VisitFilterManager(visitsContainer);
                
                // Store reference for potential later access
                visitsContainer.visitFilterManager = filterManager;
                window.visitFilterManager = filterManager;
                
                console.log('VisitFilterManager auto-initialized successfully');
                
            } catch (error) {
                console.error('Failed to auto-initialize VisitFilterManager:', error);
            }
        }
    });

})();