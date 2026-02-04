/**
 * Enhanced Search and Navigation JavaScript
 * Provides global search functionality with highlighting, filtering, and responsive navigation
 */

class SearchNavigationManager {
    constructor() {
        this.searchInput = null;
        this.suggestionsContainer = null;
        this.resultsContainer = null;
        this.filtersContainer = null;
        this.currentQuery = '';
        this.currentPage = 0;
        this.currentSortBy = 'relevance';
        this.currentSortDirection = 'desc';
        this.searchTimeout = null;
        this.activeFilters = new Map();
        this.entityCounts = new Map();
        this.recentSearches = this.loadRecentSearches();
        
        this.init();
    }
    
    init() {
        this.setupSearchInterface();
        this.setupFilters();
        this.setupMobileNavigation();
        this.setupKeyboardNavigation();
        this.setupEventListeners();
        
        // Load popular search terms on page load
        this.loadPopularSearchTerms();
    }
    
    setupSearchInterface() {
        this.searchInput = document.getElementById('globalSearchInput');
        this.suggestionsContainer = document.getElementById('searchSuggestions');
        this.resultsContainer = document.getElementById('searchResults');
        
        if (!this.searchInput) {
            // Search input not found on this page, skip search setup
            return;
        }
        
        // Create suggestions container if it doesn't exist
        if (!this.suggestionsContainer) {
            this.suggestionsContainer = document.createElement('div');
            this.suggestionsContainer.id = 'searchSuggestions';
            this.suggestionsContainer.className = 'search-suggestions';
            this.searchInput.parentNode.appendChild(this.suggestionsContainer);
        }
        
        // Setup search input events
        this.searchInput.addEventListener('input', (e) => {
            this.handleSearchInput(e.target.value);
        });
        
        this.searchInput.addEventListener('keydown', (e) => {
            this.handleSearchKeydown(e);
        });
        
        this.searchInput.addEventListener('focus', () => {
            this.showRecentSearches();
        });
        
        // Hide suggestions when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-input-group')) {
                this.hideSuggestions();
            }
        });
    }
    
    setupFilters() {
        this.filtersContainer = document.getElementById('advancedFilters');
        
        if (!this.filtersContainer) {
            // Filters not available on this page, skip filter setup
            return;
        }
        
        // Setup filter toggle
        const filterToggle = document.getElementById('filtersToggle');
        if (filterToggle) {
            filterToggle.addEventListener('click', () => {
                this.toggleFilters();
            });
        }
        
        // Setup entity type filters
        this.setupEntityTypeFilters();
        
        // Setup advanced filter controls
        this.setupAdvancedFilterControls();
    }
    
    setupEntityTypeFilters() {
        const entityFilters = document.querySelectorAll('.entity-filter-btn');
        
        entityFilters.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const entityType = btn.dataset.entityType;
                this.toggleEntityFilter(entityType, btn);
            });
        });
    }
    
    setupAdvancedFilterControls() {
        // Date range filters
        const dateFromInput = document.getElementById('dateFrom');
        const dateToInput = document.getElementById('dateTo');
        
        if (dateFromInput && dateToInput) {
            [dateFromInput, dateToInput].forEach(input => {
                input.addEventListener('change', () => {
                    this.updateDateRangeFilter();
                });
            });
        }
        
        // Species filter
        const speciesSelect = document.getElementById('speciesFilter');
        if (speciesSelect) {
            speciesSelect.addEventListener('change', (e) => {
                this.updateSpeciesFilter(e.target.value);
            });
        }
        
        // Status filter
        const statusCheckboxes = document.querySelectorAll('input[name="statusFilter"]');
        statusCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.updateStatusFilter();
            });
        });
        
        // Clear filters button
        const clearFiltersBtn = document.getElementById('clearFilters');
        if (clearFiltersBtn) {
            clearFiltersBtn.addEventListener('click', () => {
                this.clearAllFilters();
            });
        }
        
        // Save filters button
        const saveFiltersBtn = document.getElementById('saveFilters');
        if (saveFiltersBtn) {
            saveFiltersBtn.addEventListener('click', () => {
                this.showSaveFiltersModal();
            });
        }
    }
    
    setupMobileNavigation() {
        // Enhanced mobile menu behavior
        const navbarToggler = document.querySelector('.navbar-toggler');
        const navbarCollapse = document.querySelector('.navbar-collapse');
        
        if (navbarToggler && navbarCollapse) {
            navbarToggler.addEventListener('click', () => {
                // Add smooth animation for mobile menu
                navbarCollapse.style.transition = 'height 0.3s ease';
            });
        }
        
        // Mobile search behavior
        const mobileSearchInput = document.getElementById('mobileSearchInput');
        if (mobileSearchInput) {
            mobileSearchInput.addEventListener('focus', () => {
                // Expand search on mobile
                mobileSearchInput.parentNode.classList.add('expanded');
            });
            
            mobileSearchInput.addEventListener('blur', () => {
                if (!mobileSearchInput.value) {
                    mobileSearchInput.parentNode.classList.remove('expanded');
                }
            });
        }
    }
    
    setupKeyboardNavigation() {
        let selectedSuggestionIndex = -1;
        
        document.addEventListener('keydown', (e) => {
            if (!this.suggestionsContainer || !this.suggestionsContainer.children.length) {
                return;
            }
            
            const suggestions = this.suggestionsContainer.children;
            
            switch (e.key) {
                case 'ArrowDown':
                    e.preventDefault();
                    selectedSuggestionIndex = Math.min(selectedSuggestionIndex + 1, suggestions.length - 1);
                    this.highlightSuggestion(selectedSuggestionIndex);
                    break;
                    
                case 'ArrowUp':
                    e.preventDefault();
                    selectedSuggestionIndex = Math.max(selectedSuggestionIndex - 1, -1);
                    this.highlightSuggestion(selectedSuggestionIndex);
                    break;
                    
                case 'Enter':
                    if (selectedSuggestionIndex >= 0) {
                        e.preventDefault();
                        const selectedSuggestion = suggestions[selectedSuggestionIndex];
                        this.selectSuggestion(selectedSuggestion.textContent);
                    }
                    break;
                    
                case 'Escape':
                    this.hideSuggestions();
                    selectedSuggestionIndex = -1;
                    break;
            }
        });
    }
    
    setupEventListeners() {
        // Search form submission
        const searchForm = document.getElementById('globalSearchForm');
        if (searchForm) {
            searchForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.performSearch();
            });
        }
        
        // Search button click
        const searchBtn = document.getElementById('searchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.performSearch();
            });
        }
        
        // Window resize for responsive behavior
        window.addEventListener('resize', () => {
            this.handleWindowResize();
        });
    }
    
    handleSearchInput(query) {
        this.currentQuery = query.trim();
        
        // Clear previous timeout
        if (this.searchTimeout) {
            clearTimeout(this.searchTimeout);
        }
        
        // Debounce search suggestions
        this.searchTimeout = setTimeout(() => {
            if (this.currentQuery.length >= 2) {
                this.loadSearchSuggestions(this.currentQuery);
            } else if (this.currentQuery.length === 0) {
                this.showRecentSearches();
            } else {
                this.hideSuggestions();
            }
        }, 300);
    }
    
    handleSearchKeydown(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            this.performSearch();
        }
    }
    
    async loadSearchSuggestions(query) {
        try {
            const response = await fetch(`/api/search/suggestions?partialQuery=${encodeURIComponent(query)}&maxSuggestions=8`);
            
            if (response.ok) {
                const suggestions = await response.json();
                this.displaySuggestions(suggestions, 'suggestions');
            }
        } catch (error) {
            console.error('Error loading search suggestions:', error);
        }
    }
    
    async loadPopularSearchTerms() {
        try {
            const response = await fetch('/api/search/popular?maxResults=5');
            
            if (response.ok) {
                const popularTerms = await response.json();
                this.displayPopularTerms(popularTerms);
            }
        } catch (error) {
            console.error('Error loading popular search terms:', error);
        }
    }
    
    showRecentSearches() {
        if (this.recentSearches.length > 0) {
            this.displaySuggestions(this.recentSearches, 'recent');
        }
    }
    
    displaySuggestions(suggestions, type) {
        if (!this.suggestionsContainer) return;
        
        this.suggestionsContainer.innerHTML = '';
        
        if (suggestions.length === 0) {
            this.hideSuggestions();
            return;
        }
        
        const title = type === 'recent' ? 'Recent Searches' : 'Suggestions';
        const titleElement = document.createElement('div');
        titleElement.className = 'suggestion-title';
        titleElement.innerHTML = `<small class="text-muted px-3 py-2 d-block border-bottom">${title}</small>`;
        this.suggestionsContainer.appendChild(titleElement);
        
        suggestions.forEach((suggestion, index) => {
            const suggestionElement = document.createElement('div');
            suggestionElement.className = 'suggestion-item';
            suggestionElement.innerHTML = `
                <span class="suggestion-text">${this.highlightMatch(suggestion, this.currentQuery)}</span>
                ${type === 'recent' ? '<i class="fas fa-history suggestion-type"></i>' : ''}
            `;
            
            suggestionElement.addEventListener('click', () => {
                this.selectSuggestion(suggestion);
            });
            
            this.suggestionsContainer.appendChild(suggestionElement);
        });
        
        this.suggestionsContainer.style.display = 'block';
        this.suggestionsContainer.classList.add('slide-down');
    }
    
    displayPopularTerms(terms) {
        const popularTermsContainer = document.getElementById('popularTerms');
        if (!popularTermsContainer || terms.length === 0) return;
        
        popularTermsContainer.innerHTML = '';
        
        terms.forEach(term => {
            const termElement = document.createElement('span');
            termElement.className = 'badge bg-light text-dark me-2 mb-2 popular-term';
            termElement.textContent = term;
            termElement.style.cursor = 'pointer';
            
            termElement.addEventListener('click', () => {
                this.searchInput.value = term;
                this.currentQuery = term;
                this.performSearch();
            });
            
            popularTermsContainer.appendChild(termElement);
        });
    }
    
    highlightMatch(text, query) {
        if (!query) return text;
        
        const regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, '<span class="search-highlight">$1</span>');
    }
    
    highlightSuggestion(index) {
        const suggestions = this.suggestionsContainer.children;
        
        // Remove previous highlights
        Array.from(suggestions).forEach(suggestion => {
            suggestion.classList.remove('active');
        });
        
        // Highlight current suggestion
        if (index >= 0 && index < suggestions.length) {
            suggestions[index].classList.add('active');
        }
    }
    
    selectSuggestion(suggestion) {
        this.searchInput.value = suggestion;
        this.currentQuery = suggestion;
        this.hideSuggestions();
        this.performSearch();
    }
    
    hideSuggestions() {
        if (this.suggestionsContainer) {
            this.suggestionsContainer.style.display = 'none';
            this.suggestionsContainer.classList.remove('slide-down');
        }
    }
    
    async performSearch() {
        if (!this.currentQuery) return;
        
        // Add to recent searches
        this.addToRecentSearches(this.currentQuery);
        
        // Show loading state
        this.showSearchLoading();
        
        try {
            // Get search results with current filters, pagination, and sorting
            const searchParams = new URLSearchParams({
                query: this.currentQuery,
                page: this.currentPage || 0,
                size: 20,
                sortBy: this.currentSortBy || 'relevance',
                sortDirection: this.currentSortDirection || 'desc'
            });
            
            // Add active filters to search params
            this.activeFilters.forEach((value, key) => {
                searchParams.append(key, value);
            });
            
            const response = await fetch(`/api/search/advanced?${searchParams}`);
            
            if (response.ok) {
                const results = await response.json();
                this.displaySearchResults(results);
                this.updateEntityCounts(results);
            } else {
                this.showSearchError('Failed to perform search. Please try again.');
            }
        } catch (error) {
            console.error('Search error:', error);
            this.showSearchError('An error occurred while searching. Please check your connection and try again.');
        }
        
        this.hideSuggestions();
    }
    
    showSearchLoading() {
        if (!this.resultsContainer) return;
        
        this.resultsContainer.innerHTML = `
            <div class="search-loading">
                <div class="spinner-border spinner-border-sm" role="status">
                    <span class="sr-only">Loading...</span>
                </div>
                Searching...
            </div>
        `;
    }
    
    displaySearchResults(results) {
        if (!this.resultsContainer) return;
        
        if (results.totalResults === 0) {
            this.showEmptyResults();
            return;
        }
        
        let html = `
            <div class="search-results-header">
                <div class="search-results-info">
                    <span class="search-results-count">${results.totalResults}</span> results found for 
                    "<strong>${this.currentQuery}</strong>"
                    ${results.executionTimeMs ? `in ${results.executionTimeMs}ms` : ''}
                </div>
                <div class="search-results-actions">
                    <div class="btn-group" role="group">
                        <button class="btn btn-sm btn-outline-secondary" onclick="searchManager.exportResults()">
                            <i class="fas fa-download"></i> Export
                        </button>
                        <button class="btn btn-sm btn-outline-secondary" onclick="searchManager.toggleSortOptions()">
                            <i class="fas fa-sort"></i> Sort
                        </button>
                    </div>
                </div>
            </div>
            
            <div class="sort-options" id="sortOptions" style="display: none;">
                <div class="d-flex align-items-center gap-2 mb-3 p-2 bg-light rounded">
                    <label class="form-label mb-0">Sort by:</label>
                    <select id="sortBySelect" class="form-select form-select-sm" style="width: auto;">
                        <option value="relevance">Relevance</option>
                        <option value="date">Date</option>
                        <option value="name">Name</option>
                        <option value="type">Type</option>
                    </select>
                    <select id="sortDirectionSelect" class="form-select form-select-sm" style="width: auto;">
                        <option value="desc">Descending</option>
                        <option value="asc">Ascending</option>
                    </select>
                    <button class="btn btn-sm btn-primary" onclick="searchManager.applySorting()">
                        Apply
                    </button>
                </div>
            </div>
        `;
        
        results.results.forEach(result => {
            html += this.createResultCard(result);
        });
        
        // Add pagination if needed
        if (results.totalResults > 20) {
            html += this.createPagination(results);
        }
        
        this.resultsContainer.innerHTML = html;
        this.resultsContainer.classList.add('fade-in');
        
        // Set current sort values
        if (results.sortBy) {
            const sortBySelect = document.getElementById('sortBySelect');
            const sortDirectionSelect = document.getElementById('sortDirectionSelect');
            if (sortBySelect) sortBySelect.value = results.sortBy;
            if (sortDirectionSelect) sortDirectionSelect.value = results.sortDirection || 'desc';
        }
    }
    
    createPagination(results) {
        const currentPage = results.page || 0;
        const totalPages = results.totalPages || 1;
        
        if (totalPages <= 1) return '';
        
        let paginationHtml = `
            <nav aria-label="Search results pagination" class="mt-4">
                <ul class="pagination justify-content-center">
        `;
        
        // Previous button
        paginationHtml += `
            <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="searchManager.goToPage(${currentPage - 1})">
                    <i class="fas fa-chevron-left"></i> Previous
                </a>
            </li>
        `;
        
        // Page numbers
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);
        
        if (startPage > 0) {
            paginationHtml += `
                <li class="page-item">
                    <a class="page-link" href="#" onclick="searchManager.goToPage(0)">1</a>
                </li>
            `;
            if (startPage > 1) {
                paginationHtml += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }
        
        for (let i = startPage; i <= endPage; i++) {
            paginationHtml += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="searchManager.goToPage(${i})">${i + 1}</a>
                </li>
            `;
        }
        
        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                paginationHtml += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            paginationHtml += `
                <li class="page-item">
                    <a class="page-link" href="#" onclick="searchManager.goToPage(${totalPages - 1})">${totalPages}</a>
                </li>
            `;
        }
        
        // Next button
        paginationHtml += `
            <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="searchManager.goToPage(${currentPage + 1})">
                    Next <i class="fas fa-chevron-right"></i>
                </a>
            </li>
        `;
        
        paginationHtml += `
                </ul>
            </nav>
        `;
        
        return paginationHtml;
    }
    
    async goToPage(page) {
        if (page < 0) return;
        
        this.currentPage = page;
        await this.performSearch();
    }
    
    toggleSortOptions() {
        const sortOptions = document.getElementById('sortOptions');
        if (sortOptions) {
            sortOptions.style.display = sortOptions.style.display === 'none' ? 'block' : 'none';
        }
    }
    
    async applySorting() {
        const sortBySelect = document.getElementById('sortBySelect');
        const sortDirectionSelect = document.getElementById('sortDirectionSelect');
        
        if (sortBySelect && sortDirectionSelect) {
            this.currentSortBy = sortBySelect.value;
            this.currentSortDirection = sortDirectionSelect.value;
            this.currentPage = 0; // Reset to first page when sorting
            await this.performSearch();
        }
    }
    
    createResultCard(result) {
        const typeClass = `result-type-${result.entityType.toLowerCase()}`;
        const icon = this.getEntityIcon(result.entityType);
        
        // Enhanced relevance indicator
        const relevanceStars = this.getRelevanceStars(result.relevanceScore);
        
        // Enhanced highlighting - use server-side highlighted content if available
        const title = result.highlightedTitle || this.highlightMatch(result.title, this.currentQuery);
        const description = result.highlightedDescription || this.highlightMatch(result.description, this.currentQuery);
        
        // Show matched fields if available
        let matchedFieldsHtml = '';
        if (result.matchedFields && result.matchedFields.length > 0) {
            matchedFieldsHtml = `
                <div class="matched-fields">
                    <small class="text-muted">
                        <i class="fas fa-search"></i> 
                        Matches: ${result.matchedFields.join(', ')}
                    </small>
                </div>
            `;
        }
        
        return `
            <div class="result-card">
                <div class="result-card-header">
                    <div class="result-title">
                        <i class="${icon}"></i>
                        ${title}
                        <span class="relevance-indicator ms-2">${relevanceStars}</span>
                    </div>
                    <span class="result-type-badge ${typeClass}">
                        ${result.entityType}
                    </span>
                </div>
                <div class="result-card-body">
                    <div class="result-description">
                        ${description}
                    </div>
                    ${matchedFieldsHtml}
                    <div class="result-metadata">
                        ${result.metadata ? Object.entries(result.metadata).map(([key, value]) => 
                            `<span><strong>${key}:</strong> ${value}</span>`
                        ).join('') : ''}
                        ${result.lastModified ? `<span><strong>Last updated:</strong> ${new Date(result.lastModified).toLocaleDateString()}</span>` : ''}
                    </div>
                    <div class="result-actions mt-2">
                        <a href="${result.url}" class="btn btn-sm btn-primary">
                            <i class="fas fa-eye"></i> View Details
                        </a>
                        ${result.editUrl ? `
                            <a href="${result.editUrl}" class="btn btn-sm btn-outline-secondary">
                                <i class="fas fa-edit"></i> Edit
                            </a>
                        ` : ''}
                    </div>
                </div>
            </div>
        `;
    }
    
    getRelevanceStars(score) {
        if (!score || score <= 0) return '';
        
        const normalizedScore = Math.min(Math.max(score / 3, 0), 1); // Normalize to 0-1 range
        const starCount = Math.round(normalizedScore * 5);
        
        let stars = '';
        for (let i = 0; i < 5; i++) {
            if (i < starCount) {
                stars += '<i class="fas fa-star text-warning"></i>';
            } else {
                stars += '<i class="far fa-star text-muted"></i>';
            }
        }
        
        return `<span class="relevance-stars" title="Relevance: ${score.toFixed(1)}">${stars}</span>`;
    }
    
    getEntityIcon(entityType) {
        const icons = {
            'Pet': 'fas fa-paw',
            'Owner': 'fas fa-user',
            'Visit': 'fas fa-calendar-check',
            'Veterinarian': 'fas fa-user-md'
        };
        return icons[entityType] || 'fas fa-file';
    }
    
    showEmptyResults() {
        if (!this.resultsContainer) return;
        
        // Get suggestions for no results
        this.getNoResultsSuggestions(this.currentQuery).then(suggestions => {
            let suggestionsHtml = '';
            if (suggestions && suggestions.length > 0) {
                suggestionsHtml = `
                    <div class="search-suggestions-help">
                        <p class="mb-2"><strong>Try these suggestions:</strong></p>
                        <ul class="list-unstyled">
                            ${suggestions.map(suggestion => 
                                `<li>• <a href="#" onclick="performGlobalSearch('${suggestion}')" class="text-primary">${suggestion}</a></li>`
                            ).join('')}
                        </ul>
                    </div>
                `;
            }
            
            this.resultsContainer.innerHTML = `
                <div class="search-empty">
                    <i class="fas fa-search"></i>
                    <h5>No results found</h5>
                    <p>We couldn't find anything matching "<strong>${this.currentQuery}</strong>"</p>
                    <div class="search-suggestions-help">
                        <p class="mb-2">Try:</p>
                        <ul class="list-unstyled">
                            <li>• Checking your spelling</li>
                            <li>• Using different keywords</li>
                            <li>• Removing some filters</li>
                            <li>• Using more general terms</li>
                        </ul>
                    </div>
                    ${suggestionsHtml}
                </div>
            `;
        }).catch(error => {
            console.error('Error getting no-results suggestions:', error);
            // Fallback to basic empty results display
            this.resultsContainer.innerHTML = `
                <div class="search-empty">
                    <i class="fas fa-search"></i>
                    <h5>No results found</h5>
                    <p>We couldn't find anything matching "<strong>${this.currentQuery}</strong>"</p>
                    <div class="search-suggestions-help">
                        <p class="mb-2">Try:</p>
                        <ul class="list-unstyled">
                            <li>• Checking your spelling</li>
                            <li>• Using different keywords</li>
                            <li>• Removing some filters</li>
                            <li>• Using more general terms</li>
                        </ul>
                    </div>
                </div>
            `;
        });
    }
    
    showSearchError(message) {
        if (!this.resultsContainer) return;
        
        this.resultsContainer.innerHTML = `
            <div class="alert alert-danger">
                <div class="d-flex align-items-center">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    <div>
                        <strong>Search Error</strong><br>
                        ${message}
                    </div>
                </div>
                <div class="mt-3">
                    <button class="btn btn-outline-danger btn-sm" onclick="searchManager.performSearch()">
                        <i class="fas fa-redo"></i> Try Again
                    </button>
                    <button class="btn btn-outline-secondary btn-sm ms-2" onclick="searchManager.clearSearch()">
                        <i class="fas fa-times"></i> Clear Search
                    </button>
                </div>
            </div>
        `;
    }
    
    clearSearch() {
        this.searchInput.value = '';
        this.currentQuery = '';
        this.resultsContainer.innerHTML = '';
        this.hideSuggestions();
        this.clearAllFilters();
    }
    
    updateEntityCounts(results) {
        // Update entity filter buttons with result counts
        const entityCounts = results.entityCounts || {};
        
        Object.entries(entityCounts).forEach(([entityType, count]) => {
            const filterBtn = document.querySelector(`[data-entity-type="${entityType.toLowerCase()}"]`);
            if (filterBtn) {
                const countElement = filterBtn.querySelector('.entity-count');
                if (countElement) {
                    countElement.textContent = count;
                }
            }
        });
    }
    
    toggleEntityFilter(entityType, button) {
        const isActive = button.classList.contains('active');
        
        if (isActive) {
            button.classList.remove('active');
            this.activeFilters.delete('entityType');
        } else {
            // Remove active state from other entity filters
            document.querySelectorAll('.entity-filter-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            
            button.classList.add('active');
            this.activeFilters.set('entityType', entityType);
        }
        
        // Refresh search results if there's a current query
        if (this.currentQuery) {
            this.performSearch();
        }
    }
    
    toggleFilters() {
        const filtersBody = document.getElementById('filtersBody');
        const filtersToggle = document.getElementById('filtersToggle');
        
        if (filtersBody && filtersToggle) {
            const isVisible = filtersBody.classList.contains('show');
            
            if (isVisible) {
                filtersBody.classList.remove('show');
                filtersToggle.innerHTML = '<i class="fas fa-filter"></i> Show Filters';
            } else {
                filtersBody.classList.add('show');
                filtersToggle.innerHTML = '<i class="fas fa-filter"></i> Hide Filters';
            }
        }
    }
    
    updateDateRangeFilter() {
        const dateFrom = document.getElementById('dateFrom')?.value;
        const dateTo = document.getElementById('dateTo')?.value;
        
        if (dateFrom || dateTo) {
            this.activeFilters.set('dateFrom', dateFrom || '');
            this.activeFilters.set('dateTo', dateTo || '');
        } else {
            this.activeFilters.delete('dateFrom');
            this.activeFilters.delete('dateTo');
        }
        
        this.updateFilterDisplay();
    }
    
    updateSpeciesFilter(species) {
        if (species) {
            this.activeFilters.set('species', species);
        } else {
            this.activeFilters.delete('species');
        }
        
        this.updateFilterDisplay();
    }
    
    updateStatusFilter() {
        const checkedStatuses = Array.from(document.querySelectorAll('input[name="statusFilter"]:checked'))
            .map(cb => cb.value);
        
        if (checkedStatuses.length > 0) {
            this.activeFilters.set('status', checkedStatuses.join(','));
        } else {
            this.activeFilters.delete('status');
        }
        
        this.updateFilterDisplay();
    }
    
    updateFilterDisplay() {
        const activeFiltersContainer = document.getElementById('activeFilters');
        if (!activeFiltersContainer) return;
        
        activeFiltersContainer.innerHTML = '';
        
        this.activeFilters.forEach((value, key) => {
            if (value) {
                const filterChip = document.createElement('span');
                filterChip.className = 'filter-chip active';
                filterChip.innerHTML = `
                    ${key}: ${value}
                    <button type="button" class="remove-filter" onclick="searchManager.removeFilter('${key}')">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                activeFiltersContainer.appendChild(filterChip);
            }
        });
    }
    
    removeFilter(filterKey) {
        this.activeFilters.delete(filterKey);
        
        // Update UI controls
        switch (filterKey) {
            case 'entityType':
                document.querySelectorAll('.entity-filter-btn').forEach(btn => {
                    btn.classList.remove('active');
                });
                break;
            case 'dateFrom':
                document.getElementById('dateFrom').value = '';
                break;
            case 'dateTo':
                document.getElementById('dateTo').value = '';
                break;
            case 'species':
                document.getElementById('speciesFilter').value = '';
                break;
            case 'status':
                document.querySelectorAll('input[name="statusFilter"]').forEach(cb => {
                    cb.checked = false;
                });
                break;
        }
        
        this.updateFilterDisplay();
        
        // Refresh search if there's a current query
        if (this.currentQuery) {
            this.performSearch();
        }
    }
    
    clearAllFilters() {
        this.activeFilters.clear();
        
        // Reset all filter controls
        document.querySelectorAll('.entity-filter-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        
        document.getElementById('dateFrom').value = '';
        document.getElementById('dateTo').value = '';
        document.getElementById('speciesFilter').value = '';
        
        document.querySelectorAll('input[name="statusFilter"]').forEach(cb => {
            cb.checked = false;
        });
        
        this.updateFilterDisplay();
        
        // Refresh search if there's a current query
        if (this.currentQuery) {
            this.performSearch();
        }
    }
    
    handleWindowResize() {
        // Adjust search interface for different screen sizes
        const isMobile = window.innerWidth < 768;
        
        if (isMobile) {
            // Mobile-specific adjustments
            this.hideSuggestions();
        }
    }
    
    addToRecentSearches(query) {
        // Remove if already exists
        const index = this.recentSearches.indexOf(query);
        if (index > -1) {
            this.recentSearches.splice(index, 1);
        }
        
        // Add to beginning
        this.recentSearches.unshift(query);
        
        // Keep only last 10 searches
        this.recentSearches = this.recentSearches.slice(0, 10);
        
        // Save to localStorage
        this.saveRecentSearches();
    }
    
    loadRecentSearches() {
        try {
            const saved = localStorage.getItem('petClinicRecentSearches');
            return saved ? JSON.parse(saved) : [];
        } catch (error) {
            console.error('Error loading recent searches:', error);
            return [];
        }
    }
    
    saveRecentSearches() {
        try {
            localStorage.setItem('petClinicRecentSearches', JSON.stringify(this.recentSearches));
        } catch (error) {
            console.error('Error saving recent searches:', error);
        }
    }
    
    async getNoResultsSuggestions(query) {
        try {
            const response = await fetch(`/api/search/no-results-suggestions?originalQuery=${encodeURIComponent(query)}&maxSuggestions=5`);
            
            if (response.ok) {
                return await response.json();
            }
        } catch (error) {
            console.error('Error getting no-results suggestions:', error);
        }
        
        return [];
    }
    
    async exportResults() {
        try {
            const searchParams = new URLSearchParams({
                query: this.currentQuery,
                format: 'csv'
            });
            
            this.activeFilters.forEach((value, key) => {
                searchParams.append(key, value);
            });
            
            const response = await fetch(`/api/search/export?${searchParams}`);
            
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `search-results-${new Date().toISOString().split('T')[0]}.csv`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            }
        } catch (error) {
            console.error('Error exporting results:', error);
            alert('Failed to export results. Please try again.');
        }
    }
    
    showSaveFiltersModal() {
        // This would typically open a modal dialog
        const filterName = prompt('Enter a name for this filter combination:');
        if (filterName) {
            this.saveFilterCombination(filterName);
        }
    }
    
    async saveFilterCombination(name) {
        try {
            const filters = Array.from(this.activeFilters.entries()).map(([key, value]) => ({
                field: key,
                operator: 'equals',
                value: value
            }));
            
            const response = await fetch('/api/filters/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(filters),
                params: new URLSearchParams({ filterName: name })
            });
            
            if (response.ok) {
                alert('Filter combination saved successfully!');
            }
        } catch (error) {
            console.error('Error saving filter combination:', error);
            alert('Failed to save filter combination. Please try again.');
        }
    }
}

// Initialize search manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.searchManager = new SearchNavigationManager();
});

// Utility functions for global access
function performGlobalSearch(query) {
    if (window.searchManager) {
        window.searchManager.searchInput.value = query;
        window.searchManager.currentQuery = query;
        window.searchManager.performSearch();
    }
}

function clearSearch() {
    if (window.searchManager) {
        window.searchManager.clearSearch();
    }
}