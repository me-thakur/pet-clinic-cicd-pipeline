/**
 * Senior Pets Page JavaScript
 * Handles search functionality, view switching, and responsive behavior
 * Validates: Requirements 2.1, 2.3, 2.4
 */

class SeniorPetsManager {
    constructor() {
        this.currentView = 'list';
        this.searchForm = document.getElementById('senior-search-form');
        this.advancedSearchToggle = document.getElementById('toggle-advanced-search');
        this.advancedSearchSection = document.querySelector('.advanced-search');
        this.activeFiltersSection = document.getElementById('active-filters');
        this.filterTagsContainer = document.getElementById('filter-tags');
        this.listView = document.getElementById('list-view');
        this.gridView = document.getElementById('grid-view');
        this.viewListBtn = document.getElementById('view-list');
        this.viewGridBtn = document.getElementById('view-grid');
        
        this.init();
    }
    
    init() {
        this.loadStatistics();
        this.setupEventListeners();
        this.setupViewToggle();
        this.setupAdvancedSearch();
        this.displayActiveFilters();
        this.setupFormValidation();
        this.setupResponsiveFeatures();
        this.loadDropdownOptions();
        
        console.log('Senior Pets Manager initialized');
    }
    
    setupEventListeners() {
        // Advanced search toggle
        if (this.advancedSearchToggle) {
            this.advancedSearchToggle.addEventListener('click', () => {
                this.toggleAdvancedSearch();
            });
        }
        
        // View toggle buttons
        if (this.viewListBtn) {
            this.viewListBtn.addEventListener('click', () => {
                this.switchView('list');
            });
        }
        
        if (this.viewGridBtn) {
            this.viewGridBtn.addEventListener('click', () => {
                this.switchView('grid');
            });
        }
        
        // Form submission with loading state
        if (this.searchForm) {
            this.searchForm.addEventListener('submit', (e) => {
                this.handleFormSubmission(e);
            });
        }
        
        // Real-time search suggestions
        const searchTermInput = document.getElementById('searchTerm');
        if (searchTermInput) {
            let searchTimeout;
            searchTermInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.handleSearchSuggestions(e.target.value);
                }, 300);
            });
        }
        
        // Species and health condition change handlers
        const speciesSelect = document.getElementById('species');
        const healthConditionSelect = document.getElementById('healthCondition');
        
        if (speciesSelect) {
            speciesSelect.addEventListener('change', () => {
                this.updateHealthConditionOptions();
            });
        }
        
        // Age range validation
        const minAgeInput = document.getElementById('minAge');
        const maxAgeInput = document.getElementById('maxAge');
        
        if (minAgeInput && maxAgeInput) {
            minAgeInput.addEventListener('change', () => {
                this.validateAgeRange();
            });
            maxAgeInput.addEventListener('change', () => {
                this.validateAgeRange();
            });
        }
    }
    
    setupViewToggle() {
        // Set initial view state
        if (this.viewListBtn && this.viewGridBtn) {
            this.viewListBtn.classList.add('btn-secondary');
            this.viewListBtn.classList.remove('btn-outline-secondary');
            this.viewGridBtn.classList.add('btn-outline-secondary');
            this.viewGridBtn.classList.remove('btn-secondary');
        }
        
        // Load saved view preference
        const savedView = localStorage.getItem('seniorPetsView');
        if (savedView && ['list', 'grid'].includes(savedView)) {
            this.switchView(savedView);
        }
    }
    
    switchView(viewType) {
        this.currentView = viewType;
        
        if (viewType === 'list') {
            if (this.listView) this.listView.style.display = 'block';
            if (this.gridView) this.gridView.style.display = 'none';
            
            if (this.viewListBtn) {
                this.viewListBtn.classList.add('btn-secondary');
                this.viewListBtn.classList.remove('btn-outline-secondary');
            }
            if (this.viewGridBtn) {
                this.viewGridBtn.classList.add('btn-outline-secondary');
                this.viewGridBtn.classList.remove('btn-secondary');
            }
        } else if (viewType === 'grid') {
            if (this.listView) this.listView.style.display = 'none';
            if (this.gridView) this.gridView.style.display = 'block';
            
            if (this.viewGridBtn) {
                this.viewGridBtn.classList.add('btn-secondary');
                this.viewGridBtn.classList.remove('btn-outline-secondary');
            }
            if (this.viewListBtn) {
                this.viewListBtn.classList.add('btn-outline-secondary');
                this.viewListBtn.classList.remove('btn-secondary');
            }
        }
        
        // Save preference
        localStorage.setItem('seniorPetsView', viewType);
        
        console.log(`Switched to ${viewType} view`);
    }
    
    setupAdvancedSearch() {
        // Check if advanced search should be shown based on current filters
        const hasAdvancedFilters = this.hasAdvancedFilters();
        if (hasAdvancedFilters && this.advancedSearchSection) {
            this.advancedSearchSection.style.display = 'block';
            if (this.advancedSearchToggle) {
                this.advancedSearchToggle.innerHTML = '<i class="fas fa-cog"></i> Basic';
            }
        }
    }
    
    toggleAdvancedSearch() {
        if (!this.advancedSearchSection) return;
        
        const isVisible = this.advancedSearchSection.style.display !== 'none';
        
        if (isVisible) {
            this.advancedSearchSection.style.display = 'none';
            if (this.advancedSearchToggle) {
                this.advancedSearchToggle.innerHTML = '<i class="fas fa-cog"></i> Advanced';
            }
        } else {
            this.advancedSearchSection.style.display = 'block';
            if (this.advancedSearchToggle) {
                this.advancedSearchToggle.innerHTML = '<i class="fas fa-cog"></i> Basic';
            }
        }
        
        console.log(`Advanced search ${isVisible ? 'hidden' : 'shown'}`);
    }
    
    hasAdvancedFilters() {
        const minAge = document.getElementById('minAge')?.value;
        const maxAge = document.getElementById('maxAge')?.value;
        const breed = document.getElementById('breed')?.value;
        const ownerName = document.getElementById('ownerName')?.value;
        
        return minAge || maxAge || breed || ownerName;
    }
    
    displayActiveFilters() {
        if (!this.filterTagsContainer) return;
        
        const filters = this.getActiveFilters();
        
        if (filters.length === 0) {
            if (this.activeFiltersSection) {
                this.activeFiltersSection.style.display = 'none';
            }
            return;
        }
        
        // Show active filters section
        if (this.activeFiltersSection) {
            this.activeFiltersSection.style.display = 'block';
        }
        
        // Clear existing tags
        this.filterTagsContainer.innerHTML = '';
        
        // Add filter tags
        filters.forEach(filter => {
            const tag = this.createFilterTag(filter.label, filter.value, filter.param);
            this.filterTagsContainer.appendChild(tag);
        });
        
        console.log(`Displaying ${filters.length} active filters`);
    }
    
    getActiveFilters() {
        const filters = [];
        
        const searchTerm = document.getElementById('searchTerm')?.value;
        const species = document.getElementById('species')?.value;
        const healthCondition = document.getElementById('healthCondition')?.value;
        const minAge = document.getElementById('minAge')?.value;
        const maxAge = document.getElementById('maxAge')?.value;
        const breed = document.getElementById('breed')?.value;
        const ownerName = document.getElementById('ownerName')?.value;
        
        if (searchTerm) {
            filters.push({ label: 'Search', value: searchTerm, param: 'searchTerm' });
        }
        if (species) {
            filters.push({ label: 'Species', value: species, param: 'species' });
        }
        if (healthCondition) {
            filters.push({ label: 'Health', value: healthCondition, param: 'healthCondition' });
        }
        if (minAge) {
            filters.push({ label: 'Min Age', value: `${minAge} years`, param: 'minAge' });
        }
        if (maxAge) {
            filters.push({ label: 'Max Age', value: `${maxAge} years`, param: 'maxAge' });
        }
        if (breed) {
            filters.push({ label: 'Breed', value: breed, param: 'breed' });
        }
        if (ownerName) {
            filters.push({ label: 'Owner', value: ownerName, param: 'ownerName' });
        }
        
        return filters;
    }
    
    createFilterTag(label, value, param) {
        const tag = document.createElement('span');
        tag.className = 'filter-tag';
        tag.innerHTML = `
            ${label}: ${value}
            <button type="button" class="remove-filter" data-param="${param}" title="Remove filter">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        // Add click handler for remove button
        const removeBtn = tag.querySelector('.remove-filter');
        removeBtn.addEventListener('click', () => {
            this.removeFilter(param);
        });
        
        return tag;
    }
    
    removeFilter(param) {
        const input = document.getElementById(param);
        if (input) {
            if (input.type === 'select-one') {
                input.selectedIndex = 0;
            } else {
                input.value = '';
            }
        }
        
        // Refresh the page with updated filters
        this.searchForm.submit();
    }
    
    handleFormSubmission(e) {
        // Show loading state
        const submitBtn = this.searchForm.querySelector('button[type="submit"]');
        if (submitBtn) {
            const originalContent = submitBtn.innerHTML;
            submitBtn.innerHTML = '<span class="loading-spinner"></span> Searching...';
            submitBtn.disabled = true;
            
            // Re-enable after a delay (in case of errors)
            setTimeout(() => {
                submitBtn.innerHTML = originalContent;
                submitBtn.disabled = false;
            }, 5000);
        }
        
        console.log('Form submitted with loading state');
    }
    
    setupFormValidation() {
        const minAgeInput = document.getElementById('minAge');
        const maxAgeInput = document.getElementById('maxAge');
        
        if (minAgeInput && maxAgeInput) {
            // Add validation styling
            [minAgeInput, maxAgeInput].forEach(input => {
                input.addEventListener('blur', () => {
                    this.validateAgeRange();
                });
            });
        }
    }
    
    validateAgeRange() {
        const minAgeInput = document.getElementById('minAge');
        const maxAgeInput = document.getElementById('maxAge');
        
        if (!minAgeInput || !maxAgeInput) return;
        
        const minAge = parseInt(minAgeInput.value);
        const maxAge = parseInt(maxAgeInput.value);
        
        // Clear previous validation states
        minAgeInput.classList.remove('is-invalid');
        maxAgeInput.classList.remove('is-invalid');
        
        // Remove existing error messages
        const existingErrors = document.querySelectorAll('.age-validation-error');
        existingErrors.forEach(error => error.remove());
        
        if (minAge && maxAge && minAge > maxAge) {
            // Add error styling
            minAgeInput.classList.add('is-invalid');
            maxAgeInput.classList.add('is-invalid');
            
            // Add error message
            const errorMsg = document.createElement('div');
            errorMsg.className = 'invalid-feedback age-validation-error';
            errorMsg.textContent = 'Minimum age cannot be greater than maximum age';
            maxAgeInput.parentNode.appendChild(errorMsg);
            
            return false;
        }
        
        return true;
    }
    
    handleSearchSuggestions(searchTerm) {
        if (!searchTerm || searchTerm.length < 2) {
            this.hideSuggestions();
            return;
        }
        
        // TODO: Implement real-time search suggestions
        // This would call the backend API for suggestions
        console.log(`Getting suggestions for: ${searchTerm}`);
    }
    
    hideSuggestions() {
        const suggestionsContainer = document.getElementById('search-suggestions');
        if (suggestionsContainer) {
            suggestionsContainer.style.display = 'none';
        }
    }
    
    updateHealthConditionOptions() {
        const speciesSelect = document.getElementById('species');
        const healthConditionSelect = document.getElementById('healthCondition');
        
        if (!speciesSelect || !healthConditionSelect) return;
        
        const selectedSpecies = speciesSelect.value;
        
        // TODO: Update health conditions based on species
        // This would call the backend API for species-specific conditions
        console.log(`Updating health conditions for species: ${selectedSpecies}`);
    }
    
    setupResponsiveFeatures() {
        // Handle mobile-specific features
        if (window.innerWidth <= 768) {
            this.setupMobileFeatures();
        }
        
        // Listen for window resize
        window.addEventListener('resize', () => {
            if (window.innerWidth <= 768) {
                this.setupMobileFeatures();
            } else {
                this.setupDesktopFeatures();
            }
        });
    }
    
    setupMobileFeatures() {
        // Auto-collapse advanced search on mobile
        if (this.advancedSearchSection && window.innerWidth <= 576) {
            this.advancedSearchSection.style.display = 'none';
            if (this.advancedSearchToggle) {
                this.advancedSearchToggle.innerHTML = '<i class="fas fa-cog"></i> Advanced';
            }
        }
        
        // Switch to grid view on mobile for better touch interaction
        if (window.innerWidth <= 576 && this.currentView === 'list') {
            this.switchView('grid');
        }
        
        console.log('Mobile features activated');
    }
    
    setupDesktopFeatures() {
        // Desktop-specific features can be added here
        console.log('Desktop features activated');
    }
    
    loadDropdownOptions() {
        // Load species options
        this.loadSpeciesOptions();
        
        // Load health condition options
        this.loadHealthConditionOptions();
    }
    
    async loadSpeciesOptions() {
        try {
            const response = await fetch('/pets/senior/species');
            if (response.ok) {
                const species = await response.json();
                this.updateSpeciesDropdown(species);
            }
        } catch (error) {
            console.error('Error loading species options:', error);
        }
    }
    
    async loadHealthConditionOptions() {
        try {
            const response = await fetch('/pets/senior/health-conditions');
            if (response.ok) {
                const conditions = await response.json();
                this.updateHealthConditionDropdown(conditions);
            }
        } catch (error) {
            console.error('Error loading health condition options:', error);
        }
    }
    
    updateSpeciesDropdown(species) {
        const speciesSelect = document.getElementById('species');
        if (!speciesSelect) return;
        
        const currentValue = speciesSelect.value;
        
        // Clear existing options (except the first "All Species" option)
        while (speciesSelect.children.length > 1) {
            speciesSelect.removeChild(speciesSelect.lastChild);
        }
        
        // Add new options
        species.forEach(speciesName => {
            const option = document.createElement('option');
            option.value = speciesName;
            option.textContent = speciesName;
            if (speciesName === currentValue) {
                option.selected = true;
            }
            speciesSelect.appendChild(option);
        });
        
        console.log(`Updated species dropdown with ${species.length} options`);
    }
    
    updateHealthConditionDropdown(conditions) {
        const healthConditionSelect = document.getElementById('healthCondition');
        if (!healthConditionSelect) return;
        
        const currentValue = healthConditionSelect.value;
        
        // Clear existing options (except the first "Any Condition" option)
        while (healthConditionSelect.children.length > 1) {
            healthConditionSelect.removeChild(healthConditionSelect.lastChild);
        }
        
        // Add new options
        conditions.forEach(condition => {
            const option = document.createElement('option');
            option.value = condition;
            option.textContent = condition;
            if (condition === currentValue) {
                option.selected = true;
            }
            healthConditionSelect.appendChild(option);
        });
        
        console.log(`Updated health condition dropdown with ${conditions.length} options`);
    }
    
    async loadStatistics() {
        try {
            const response = await fetch('/pets/senior/statistics');
            if (response.ok) {
                const stats = await response.json();
                this.updateStatistics(stats);
            }
        } catch (error) {
            console.error('Error loading statistics:', error);
            this.showStatisticsError();
        }
    }
    
    updateStatistics(stats) {
        // Update statistics cards
        const totalSeniorPetsEl = document.getElementById('total-senior-pets');
        const seniorPercentageEl = document.getElementById('senior-percentage');
        const averageAgeEl = document.getElementById('average-age');
        const specialCareCountEl = document.getElementById('special-care-count');
        const seniorCountEl = document.getElementById('senior-count');
        
        if (totalSeniorPetsEl) {
            totalSeniorPetsEl.textContent = stats.totalSeniorPets || '0';
        }
        
        if (seniorPercentageEl) {
            seniorPercentageEl.textContent = `${stats.seniorPercentage || 0}%`;
        }
        
        if (averageAgeEl) {
            averageAgeEl.textContent = `${stats.averageAge || 0} yrs`;
        }
        
        if (specialCareCountEl) {
            // Calculate pets needing special care (example logic)
            const specialCareCount = Math.floor((stats.totalSeniorPets || 0) * 0.3);
            specialCareCountEl.textContent = specialCareCount;
        }
        
        if (seniorCountEl) {
            seniorCountEl.textContent = stats.totalSeniorPets || '0';
        }
        
        console.log('Statistics updated successfully');
    }
    
    showStatisticsError() {
        // Show error state for statistics
        const statsElements = [
            'total-senior-pets',
            'senior-percentage', 
            'average-age',
            'special-care-count',
            'senior-count'
        ];
        
        statsElements.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = 'N/A';
                element.title = 'Statistics temporarily unavailable';
            }
        });
        
        console.log('Statistics error state displayed');
    }
}

// Utility functions
function showNoResultsSuggestions() {
    const suggestionsContainer = document.querySelector('.search-suggestions');
    if (suggestionsContainer) {
        suggestionsContainer.style.display = 'block';
    }
}

function hideNoResultsSuggestions() {
    const suggestionsContainer = document.querySelector('.search-suggestions');
    if (suggestionsContainer) {
        suggestionsContainer.style.display = 'none';
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize senior pets manager
    window.seniorPetsManager = new SeniorPetsManager();
    
    // Add any additional initialization here
    console.log('Senior Pets page loaded successfully');
});

// Export for testing purposes
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { SeniorPetsManager };
}