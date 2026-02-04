/**
 * Veterinarian Filter Management
 * Handles comprehensive filtering by specialties, availability, and experience
 * 
 * Validates: Requirements 12.1, 12.2, 12.3, 12.5
 */
class VeterinarianFilterManager {
    constructor() {
        this.filterCriteria = {
            specialties: [],
            availableOnly: null,
            minExperienceYears: null,
            maxExperienceYears: null,
            activeLicenseOnly: null,
            maxVisits: null,
            emergencyCapable: null,
            surgicalCapable: null,
            searchText: null
        };
        
        this.filterOptions = {};
        this.activeFilters = new Map();
        this.isLoading = false;
        
        this.init();
    }
    
    async init() {
        try {
            console.log('Initializing veterinarian filter manager');
            
            // Load filter options
            await this.loadFilterOptions();
            
            // Setup UI components
            this.setupFilterUI();
            this.setupEventListeners();
            
            // Restore filter state if available
            this.restoreFilterState();
            
            console.log('Veterinarian filter manager initialized successfully');
            
        } catch (error) {
            console.error('Failed to initialize veterinarian filter manager:', error);
            this.showError('Filter initialization failed. Some features may not work properly.');
        }
    }
    
    async loadFilterOptions() {
        try {
            const response = await fetch('/api/veterinarians/filter-options');
            if (response.ok) {
                this.filterOptions = await response.json();
                console.log('Loaded filter options:', this.filterOptions);
            } else {
                throw new Error(`Failed to load filter options: ${response.status}`);
            }
        } catch (error) {
            console.error('Error loading filter options:', error);
            // Provide fallback options
            this.filterOptions = {
                specialties: ['General Practice', 'Surgery', 'Emergency', 'Cardiology', 'Dermatology'],
                experienceLevels: ['0-2 years', '3-5 years', '6-10 years', '11-15 years', '16+ years'],
                availability: ['Available Now', 'Light Workload'],
                capabilities: ['Emergency', 'Surgery', 'General Practice']
            };
        }
    }
    
    setupFilterUI() {
        // Create filter panel if it doesn't exist
        let filterPanel = document.getElementById('veterinarianFilterPanel');
        if (!filterPanel) {
            filterPanel = this.createFilterPanel();
            this.insertFilterPanel(filterPanel);
        }
        
        // Populate filter options
        this.populateFilterOptions();
        
        // Setup active filters display
        this.setupActiveFiltersDisplay();
    }
    
    createFilterPanel() {
        const panel = document.createElement('div');
        panel.id = 'veterinarianFilterPanel';
        panel.className = 'filter-panel card mb-4';
        panel.style.display = 'none';
        
        panel.innerHTML = `
            <div class="card-header">
                <h5 class="mb-0">
                    <i class="fas fa-filter"></i> Filter Veterinarians
                    <button type="button" class="btn btn-sm btn-outline-secondary float-end" id="clearAllFilters">
                        <i class="fas fa-times"></i> Clear All
                    </button>
                </h5>
            </div>
            <div class="card-body">
                <div class="row g-3">
                    <!-- Specialty Filter -->
                    <div class="col-md-3">
                        <label for="specialtyFilter" class="form-label">Specialties</label>
                        <select class="form-select" id="specialtyFilter" multiple>
                            <!-- Options populated dynamically -->
                        </select>
                        <div class="form-text">Hold Ctrl/Cmd to select multiple</div>
                    </div>
                    
                    <!-- Availability Filter -->
                    <div class="col-md-3">
                        <label for="availabilityFilter" class="form-label">Availability</label>
                        <select class="form-select" id="availabilityFilter">
                            <option value="">All Veterinarians</option>
                            <option value="true">Available Only</option>
                            <option value="false">Include Busy</option>
                        </select>
                    </div>
                    
                    <!-- Experience Filter -->
                    <div class="col-md-3">
                        <label for="experienceFilter" class="form-label">Experience Level</label>
                        <select class="form-select" id="experienceFilter">
                            <option value="">Any Experience</option>
                            <option value="0-2">0-2 years</option>
                            <option value="3-5">3-5 years</option>
                            <option value="6-10">6-10 years</option>
                            <option value="11-15">11-15 years</option>
                            <option value="16+">16+ years</option>
                        </select>
                    </div>
                    
                    <!-- Capabilities Filter -->
                    <div class="col-md-3">
                        <label class="form-label">Capabilities</label>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="emergencyCapable">
                            <label class="form-check-label" for="emergencyCapable">
                                Emergency Care
                            </label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="surgicalCapable">
                            <label class="form-check-label" for="surgicalCapable">
                                Surgical Procedures
                            </label>
                        </div>
                    </div>
                    
                    <!-- Search Text -->
                    <div class="col-md-6">
                        <label for="searchTextFilter" class="form-label">Search</label>
                        <input type="text" class="form-control" id="searchTextFilter" 
                               placeholder="Search by name or license number...">
                    </div>
                    
                    <!-- Max Visits Filter -->
                    <div class="col-md-3">
                        <label for="maxVisitsFilter" class="form-label">Max Visits (Light Workload)</label>
                        <input type="number" class="form-control" id="maxVisitsFilter" 
                               placeholder="e.g., 20" min="0" max="100">
                    </div>
                    
                    <!-- Filter Actions -->
                    <div class="col-md-3 d-flex align-items-end">
                        <button type="button" class="btn btn-primary me-2" id="applyFilters">
                            <i class="fas fa-search"></i> Apply Filters
                        </button>
                        <button type="button" class="btn btn-outline-secondary" id="resetFilters">
                            <i class="fas fa-undo"></i> Reset
                        </button>
                    </div>
                </div>
                
                <!-- Filter Results Info -->
                <div class="row mt-3">
                    <div class="col-12">
                        <div id="filterResultsInfo" class="alert alert-info" style="display: none;">
                            <!-- Results info populated dynamically -->
                        </div>
                        <div id="filterSuggestions" class="alert alert-warning" style="display: none;">
                            <!-- Suggestions populated dynamically -->
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        return panel;
    }
    
    insertFilterPanel(panel) {
        // Insert after the header but before the search form
        const header = document.querySelector('.d-flex.justify-content-between.align-items-center.mb-4');
        if (header) {
            header.parentNode.insertBefore(panel, header.nextSibling);
        } else {
            // Fallback: insert at the beginning of content
            const content = document.querySelector('[th\\:fragment="content"]') || document.body;
            content.insertBefore(panel, content.firstChild);
        }
    }
    
    populateFilterOptions() {
        // Populate specialty options
        const specialtySelect = document.getElementById('specialtyFilter');
        if (specialtySelect && this.filterOptions.specialties) {
            specialtySelect.innerHTML = '';
            this.filterOptions.specialties.forEach(specialty => {
                const option = document.createElement('option');
                option.value = specialty;
                option.textContent = specialty;
                specialtySelect.appendChild(option);
            });
        }
    }
    
    setupActiveFiltersDisplay() {
        let activeFiltersContainer = document.getElementById('activeFilters');
        if (!activeFiltersContainer) {
            activeFiltersContainer = document.createElement('div');
            activeFiltersContainer.id = 'activeFilters';
            activeFiltersContainer.className = 'active-filters mb-3';
            
            // Insert before the table
            const tableContainer = document.querySelector('.card');
            if (tableContainer) {
                tableContainer.parentNode.insertBefore(activeFiltersContainer, tableContainer);
            }
        }
    }
    
    setupEventListeners() {
        // Filter button toggle
        const filterButton = document.querySelector('.btn-outline-info.dropdown-toggle');
        if (filterButton) {
            // Replace dropdown functionality with filter panel toggle
            filterButton.removeAttribute('data-bs-toggle');
            filterButton.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleFilterPanel();
            });
        }
        
        // Apply filters button
        const applyButton = document.getElementById('applyFilters');
        if (applyButton) {
            applyButton.addEventListener('click', () => this.applyFilters());
        }
        
        // Reset filters button
        const resetButton = document.getElementById('resetFilters');
        if (resetButton) {
            resetButton.addEventListener('click', () => this.resetFilters());
        }
        
        // Clear all filters button
        const clearAllButton = document.getElementById('clearAllFilters');
        if (clearAllButton) {
            clearAllButton.addEventListener('click', () => this.clearAllFilters());
        }
        
        // Real-time search
        const searchInput = document.getElementById('searchTextFilter');
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.filterCriteria.searchText = e.target.value.trim() || null;
                    this.applyFilters();
                }, 500);
            });
        }
        
        // Filter change listeners
        this.setupFilterChangeListeners();
    }
    
    setupFilterChangeListeners() {
        // Specialty filter
        const specialtySelect = document.getElementById('specialtyFilter');
        if (specialtySelect) {
            specialtySelect.addEventListener('change', () => {
                this.filterCriteria.specialties = Array.from(specialtySelect.selectedOptions)
                    .map(option => option.value);
            });
        }
        
        // Availability filter
        const availabilitySelect = document.getElementById('availabilityFilter');
        if (availabilitySelect) {
            availabilitySelect.addEventListener('change', () => {
                const value = availabilitySelect.value;
                this.filterCriteria.availableOnly = value === '' ? null : value === 'true';
            });
        }
        
        // Experience filter
        const experienceSelect = document.getElementById('experienceFilter');
        if (experienceSelect) {
            experienceSelect.addEventListener('change', () => {
                const value = experienceSelect.value;
                if (value === '') {
                    this.filterCriteria.minExperienceYears = null;
                    this.filterCriteria.maxExperienceYears = null;
                } else if (value === '16+') {
                    this.filterCriteria.minExperienceYears = 16;
                    this.filterCriteria.maxExperienceYears = null;
                } else {
                    const [min, max] = value.split('-').map(Number);
                    this.filterCriteria.minExperienceYears = min;
                    this.filterCriteria.maxExperienceYears = max;
                }
            });
        }
        
        // Capability checkboxes
        const emergencyCheck = document.getElementById('emergencyCapable');
        if (emergencyCheck) {
            emergencyCheck.addEventListener('change', () => {
                this.filterCriteria.emergencyCapable = emergencyCheck.checked || null;
            });
        }
        
        const surgicalCheck = document.getElementById('surgicalCapable');
        if (surgicalCheck) {
            surgicalCheck.addEventListener('change', () => {
                this.filterCriteria.surgicalCapable = surgicalCheck.checked || null;
            });
        }
        
        // Max visits filter
        const maxVisitsInput = document.getElementById('maxVisitsFilter');
        if (maxVisitsInput) {
            maxVisitsInput.addEventListener('change', () => {
                const value = parseInt(maxVisitsInput.value);
                this.filterCriteria.maxVisits = isNaN(value) ? null : value;
            });
        }
    }
    
    toggleFilterPanel() {
        const panel = document.getElementById('veterinarianFilterPanel');
        if (panel) {
            const isVisible = panel.style.display !== 'none';
            panel.style.display = isVisible ? 'none' : 'block';
            
            // Update button text
            const button = document.querySelector('.btn-outline-info.dropdown-toggle');
            if (button) {
                const icon = button.querySelector('i');
                const text = button.querySelector('.d-none.d-sm-inline') || button;
                if (isVisible) {
                    if (icon) icon.className = 'fas fa-filter';
                    if (text) text.textContent = text.textContent.replace('Hide Filters', 'Filter');
                } else {
                    if (icon) icon.className = 'fas fa-times';
                    if (text) text.textContent = text.textContent.replace('Filter', 'Hide Filters');
                }
            }
        }
    }
    
    async applyFilters() {
        if (this.isLoading) return;
        
        try {
            this.isLoading = true;
            this.showLoading(true);
            
            console.log('Applying filters:', this.filterCriteria);
            
            // Build query parameters
            const params = new URLSearchParams();
            
            if (this.filterCriteria.specialties && this.filterCriteria.specialties.length > 0) {
                this.filterCriteria.specialties.forEach(specialty => {
                    params.append('specialties', specialty);
                });
            }
            
            if (this.filterCriteria.availableOnly !== null) {
                params.append('availableOnly', this.filterCriteria.availableOnly);
            }
            
            if (this.filterCriteria.minExperienceYears !== null) {
                params.append('minExperienceYears', this.filterCriteria.minExperienceYears);
            }
            
            if (this.filterCriteria.maxExperienceYears !== null) {
                params.append('maxExperienceYears', this.filterCriteria.maxExperienceYears);
            }
            
            if (this.filterCriteria.activeLicenseOnly !== null) {
                params.append('activeLicenseOnly', this.filterCriteria.activeLicenseOnly);
            }
            
            if (this.filterCriteria.maxVisits !== null) {
                params.append('maxVisits', this.filterCriteria.maxVisits);
            }
            
            if (this.filterCriteria.emergencyCapable !== null) {
                params.append('emergencyCapable', this.filterCriteria.emergencyCapable);
            }
            
            if (this.filterCriteria.surgicalCapable !== null) {
                params.append('surgicalCapable', this.filterCriteria.surgicalCapable);
            }
            
            if (this.filterCriteria.searchText) {
                params.append('searchText', this.filterCriteria.searchText);
            }
            
            // Add pagination
            params.append('page', '0');
            params.append('size', '20');
            params.append('sort', 'lastName,asc');
            
            // Make API call
            const response = await fetch(`/api/veterinarians/filter?${params.toString()}`);
            
            if (response.ok) {
                const result = await response.json();
                console.log('Filter results:', result);
                
                // Update UI with results
                this.updateResultsDisplay(result);
                this.updateActiveFiltersDisplay();
                this.persistFilterState();
                
            } else {
                throw new Error(`Filter request failed: ${response.status}`);
            }
            
        } catch (error) {
            console.error('Error applying filters:', error);
            this.showError('Failed to apply filters. Please try again.');
        } finally {
            this.isLoading = false;
            this.showLoading(false);
        }
    }
    
    updateResultsDisplay(result) {
        // Update results info
        const resultsInfo = document.getElementById('filterResultsInfo');
        if (resultsInfo) {
            if (result.totalResults > 0) {
                resultsInfo.innerHTML = `
                    <i class="fas fa-info-circle"></i>
                    Found ${result.totalResults} veterinarian${result.totalResults !== 1 ? 's' : ''} 
                    matching your criteria (${result.executionTimeMs}ms)
                `;
                resultsInfo.style.display = 'block';
                resultsInfo.className = 'alert alert-info';
            } else {
                resultsInfo.innerHTML = `
                    <i class="fas fa-exclamation-triangle"></i>
                    No veterinarians found matching your criteria
                `;
                resultsInfo.style.display = 'block';
                resultsInfo.className = 'alert alert-warning';
            }
        }
        
        // Show suggestions if no results
        const suggestionsDiv = document.getElementById('filterSuggestions');
        if (suggestionsDiv && result.suggestions && result.suggestions.length > 0) {
            suggestionsDiv.innerHTML = `
                <i class="fas fa-lightbulb"></i>
                <strong>Suggestions:</strong>
                <ul class="mb-0 mt-2">
                    ${result.suggestions.map(suggestion => `<li>${suggestion}</li>`).join('')}
                </ul>
            `;
            suggestionsDiv.style.display = 'block';
        } else if (suggestionsDiv) {
            suggestionsDiv.style.display = 'none';
        }
        
        // Update table with results (simplified - in real implementation would update table rows)
        this.updateTableWithResults(result.veterinarians);
    }
    
    updateTableWithResults(veterinarians) {
        // This is a simplified implementation
        // In a real application, you would update the table rows with the filtered results
        console.log('Updating table with', veterinarians.totalElements, 'veterinarians');
        
        // For now, just log the results
        // The actual table update would depend on your table implementation
    }
    
    updateActiveFiltersDisplay() {
        const container = document.getElementById('activeFilters');
        if (!container) return;
        
        const activeFilters = this.getActiveFilters();
        
        if (activeFilters.length === 0) {
            container.innerHTML = '';
            container.style.display = 'none';
            return;
        }
        
        container.innerHTML = `
            <div class="d-flex flex-wrap align-items-center gap-2">
                <span class="text-muted">Active filters:</span>
                ${activeFilters.map(filter => `
                    <span class="badge bg-primary">
                        ${filter.label}
                        <button type="button" class="btn-close btn-close-white ms-1" 
                                onclick="veterinarianFilterManager.removeFilter('${filter.key}')"
                                aria-label="Remove ${filter.label} filter"></button>
                    </span>
                `).join('')}
                <button type="button" class="btn btn-sm btn-outline-secondary" 
                        onclick="veterinarianFilterManager.clearAllFilters()">
                    <i class="fas fa-times"></i> Clear All
                </button>
            </div>
        `;
        container.style.display = 'block';
    }
    
    getActiveFilters() {
        const filters = [];
        
        if (this.filterCriteria.specialties && this.filterCriteria.specialties.length > 0) {
            filters.push({
                key: 'specialties',
                label: `Specialties: ${this.filterCriteria.specialties.join(', ')}`
            });
        }
        
        if (this.filterCriteria.availableOnly !== null) {
            filters.push({
                key: 'availableOnly',
                label: this.filterCriteria.availableOnly ? 'Available Only' : 'Include Busy'
            });
        }
        
        if (this.filterCriteria.minExperienceYears !== null || this.filterCriteria.maxExperienceYears !== null) {
            let label = 'Experience: ';
            if (this.filterCriteria.minExperienceYears !== null && this.filterCriteria.maxExperienceYears !== null) {
                label += `${this.filterCriteria.minExperienceYears}-${this.filterCriteria.maxExperienceYears} years`;
            } else if (this.filterCriteria.minExperienceYears !== null) {
                label += `${this.filterCriteria.minExperienceYears}+ years`;
            } else {
                label += `≤${this.filterCriteria.maxExperienceYears} years`;
            }
            filters.push({ key: 'experience', label });
        }
        
        if (this.filterCriteria.emergencyCapable) {
            filters.push({ key: 'emergencyCapable', label: 'Emergency Capable' });
        }
        
        if (this.filterCriteria.surgicalCapable) {
            filters.push({ key: 'surgicalCapable', label: 'Surgical Capable' });
        }
        
        if (this.filterCriteria.maxVisits !== null) {
            filters.push({ key: 'maxVisits', label: `Max ${this.filterCriteria.maxVisits} visits` });
        }
        
        if (this.filterCriteria.searchText) {
            filters.push({ key: 'searchText', label: `Search: "${this.filterCriteria.searchText}"` });
        }
        
        return filters;
    }
    
    removeFilter(key) {
        switch (key) {
            case 'specialties':
                this.filterCriteria.specialties = [];
                const specialtySelect = document.getElementById('specialtyFilter');
                if (specialtySelect) {
                    Array.from(specialtySelect.options).forEach(option => option.selected = false);
                }
                break;
            case 'availableOnly':
                this.filterCriteria.availableOnly = null;
                const availabilitySelect = document.getElementById('availabilityFilter');
                if (availabilitySelect) availabilitySelect.value = '';
                break;
            case 'experience':
                this.filterCriteria.minExperienceYears = null;
                this.filterCriteria.maxExperienceYears = null;
                const experienceSelect = document.getElementById('experienceFilter');
                if (experienceSelect) experienceSelect.value = '';
                break;
            case 'emergencyCapable':
                this.filterCriteria.emergencyCapable = null;
                const emergencyCheck = document.getElementById('emergencyCapable');
                if (emergencyCheck) emergencyCheck.checked = false;
                break;
            case 'surgicalCapable':
                this.filterCriteria.surgicalCapable = null;
                const surgicalCheck = document.getElementById('surgicalCapable');
                if (surgicalCheck) surgicalCheck.checked = false;
                break;
            case 'maxVisits':
                this.filterCriteria.maxVisits = null;
                const maxVisitsInput = document.getElementById('maxVisitsFilter');
                if (maxVisitsInput) maxVisitsInput.value = '';
                break;
            case 'searchText':
                this.filterCriteria.searchText = null;
                const searchInput = document.getElementById('searchTextFilter');
                if (searchInput) searchInput.value = '';
                break;
        }
        
        this.applyFilters();
    }
    
    resetFilters() {
        // Reset all filter criteria
        this.filterCriteria = {
            specialties: [],
            availableOnly: null,
            minExperienceYears: null,
            maxExperienceYears: null,
            activeLicenseOnly: null,
            maxVisits: null,
            emergencyCapable: null,
            surgicalCapable: null,
            searchText: null
        };
        
        // Reset UI elements
        this.resetFilterUI();
        
        // Apply empty filters (show all)
        this.applyFilters();
    }
    
    clearAllFilters() {
        this.resetFilters();
        
        // Hide filter panel
        const panel = document.getElementById('veterinarianFilterPanel');
        if (panel) {
            panel.style.display = 'none';
        }
    }
    
    resetFilterUI() {
        const specialtySelect = document.getElementById('specialtyFilter');
        if (specialtySelect) {
            Array.from(specialtySelect.options).forEach(option => option.selected = false);
        }
        
        const availabilitySelect = document.getElementById('availabilityFilter');
        if (availabilitySelect) availabilitySelect.value = '';
        
        const experienceSelect = document.getElementById('experienceFilter');
        if (experienceSelect) experienceSelect.value = '';
        
        const emergencyCheck = document.getElementById('emergencyCapable');
        if (emergencyCheck) emergencyCheck.checked = false;
        
        const surgicalCheck = document.getElementById('surgicalCapable');
        if (surgicalCheck) surgicalCheck.checked = false;
        
        const maxVisitsInput = document.getElementById('maxVisitsFilter');
        if (maxVisitsInput) maxVisitsInput.value = '';
        
        const searchInput = document.getElementById('searchTextFilter');
        if (searchInput) searchInput.value = '';
    }
    
    persistFilterState() {
        try {
            localStorage.setItem('veterinarianFilters', JSON.stringify(this.filterCriteria));
            console.log('Filter state persisted');
        } catch (error) {
            console.error('Failed to persist filter state:', error);
        }
    }
    
    restoreFilterState() {
        try {
            const saved = localStorage.getItem('veterinarianFilters');
            if (saved) {
                const criteria = JSON.parse(saved);
                this.filterCriteria = { ...this.filterCriteria, ...criteria };
                this.updateUIFromCriteria();
                console.log('Filter state restored:', this.filterCriteria);
            }
        } catch (error) {
            console.error('Failed to restore filter state:', error);
        }
    }
    
    updateUIFromCriteria() {
        // Update UI elements to match restored criteria
        const specialtySelect = document.getElementById('specialtyFilter');
        if (specialtySelect && this.filterCriteria.specialties) {
            Array.from(specialtySelect.options).forEach(option => {
                option.selected = this.filterCriteria.specialties.includes(option.value);
            });
        }
        
        const availabilitySelect = document.getElementById('availabilityFilter');
        if (availabilitySelect && this.filterCriteria.availableOnly !== null) {
            availabilitySelect.value = this.filterCriteria.availableOnly.toString();
        }
        
        // Update other UI elements similarly...
    }
    
    showLoading(show) {
        const applyButton = document.getElementById('applyFilters');
        if (applyButton) {
            if (show) {
                applyButton.disabled = true;
                applyButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Applying...';
            } else {
                applyButton.disabled = false;
                applyButton.innerHTML = '<i class="fas fa-search"></i> Apply Filters';
            }
        }
    }
    
    showError(message) {
        // Create or update error alert
        let errorAlert = document.getElementById('filterErrorAlert');
        if (!errorAlert) {
            errorAlert = document.createElement('div');
            errorAlert.id = 'filterErrorAlert';
            errorAlert.className = 'alert alert-danger alert-dismissible fade show';
            
            const filterPanel = document.getElementById('veterinarianFilterPanel');
            if (filterPanel) {
                filterPanel.parentNode.insertBefore(errorAlert, filterPanel);
            }
        }
        
        errorAlert.innerHTML = `
            <i class="fas fa-exclamation-triangle"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            if (errorAlert && errorAlert.parentNode) {
                errorAlert.remove();
            }
        }, 5000);
    }
}

// Initialize filter manager when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Only initialize on veterinarian pages
    if (window.location.pathname.includes('/veterinarians') || 
        document.getElementById('veterinariansTable') || 
        document.getElementById('veterinariansListTable')) {
        
        window.veterinarianFilterManager = new VeterinarianFilterManager();
    }
});