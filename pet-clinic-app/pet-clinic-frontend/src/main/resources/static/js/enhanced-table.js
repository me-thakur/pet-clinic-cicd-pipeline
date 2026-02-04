/**
 * Enhanced Table Component
 * Provides server-side sorting, filtering, and bulk operations for HTML tables
 * Builds upon the existing table-sorting.js functionality
 * 
 * Requirements: 1.1, 1.5, 2.5
 */

(function() {
    'use strict';

    /**
     * Sort Controller - Manages sort state and UI interactions
     * Handles both client-side and server-side sorting
     */
    class SortController {
        constructor(enhancedTable) {
            this.enhancedTable = enhancedTable;
            this.currentSort = { 
                column: null, 
                direction: null, 
                isGlobal: false, 
                multiColumn: [] // Array of {column, direction, precedence}
            };
            this.fallbackSorter = null; // Will hold TableSorter instance for fallback
            this.apiClient = window.tableApiClient || new window.TableApiClient();
        }

        /**
         * Apply sort to a column with multi-column support
         * @param {string} column - Column name
         * @param {string} direction - Sort direction ('asc' or 'desc')
         * @param {boolean} addToExisting - Add to existing sort criteria (Ctrl+click)
         * @param {boolean} forceClientSide - Force client-side sorting (for fallback testing)
         * @returns {Promise<void>}
         */
        async applySortAsync(column, direction, addToExisting = false, forceClientSide = false) {
            try {
                // Show loading state
                this.enhancedTable.showLoadingState();

                // Check if we should attempt server-side sorting
                const shouldTryServerSide = this.enhancedTable.options.enableGlobalSort && 
                                          !forceClientSide && 
                                          this.isServerSideAvailable();

                if (shouldTryServerSide) {
                    try {
                        await this.applyServerSideSort(column, direction, addToExisting);
                        this.updateCurrentSort(column, direction, addToExisting, true);
                        this.updateVisualIndicators();
                        
                        // Clear any previous fallback warnings
                        this.enhancedTable.clearFallbackWarning();
                        return;
                    } catch (error) {
                        // Use enhanced error handler
                        const errorHandler = this.enhancedTable.options.errorHandler;
                        if (errorHandler) {
                            const result = await errorHandler.handleError(error, {
                                operation: 'sort',
                                column,
                                direction,
                                addToExisting,
                                entityType: this.enhancedTable.options.entityType
                            });
                            
                            if (result.fallback) {
                                // Fallback to client-side sorting
                                console.log('Falling back to client-side sorting due to server error');
                                await this.applyClientSideSort(column, direction, addToExisting);
                                this.enhancedTable.showFallbackMessage(
                                    'Server sorting temporarily unavailable. Using local sorting instead.',
                                    'warning'
                                );
                                return;
                            }
                        } else {
                            // Legacy error handling
                            console.warn('Server-side sorting failed, falling back to client-side:', error);
                            
                            // Mark server-side as temporarily unavailable
                            this.markServerSideUnavailable();
                            
                            // Show user-friendly fallback notification
                            this.showFallbackNotification(error);
                            
                            // Fallback to client-side sorting
                            await this.applyClientSideSort(column, direction, addToExisting);
                            return;
                        }
                    }
                }

                // Fallback to client-side sorting
                await this.applyClientSideSort(column, direction, addToExisting);
                this.updateCurrentSort(column, direction, addToExisting, false);
                this.updateVisualIndicators();

            } catch (error) {
                // Use enhanced error handler for complete failures
                const errorHandler = this.enhancedTable.options.errorHandler;
                if (errorHandler) {
                    await errorHandler.handleError(error, {
                        operation: 'sort_complete_failure',
                        column,
                        direction,
                        addToExisting,
                        entityType: this.enhancedTable.options.entityType
                    });
                } else {
                    // Legacy error handling
                    console.error('Sort operation failed completely:', error);
                    this.enhancedTable.showError('Unable to sort data. Please refresh the page and try again.');
                }
                
                // Reset sort state on complete failure
                this.currentSort = { column: null, direction: null, isGlobal: false, multiColumn: [] };
                this.updateVisualIndicators();
            } finally {
                this.enhancedTable.hideLoadingState();
            }
        }

        /**
         * Update current sort state with multi-column support
         * @param {string} column - Column name
         * @param {string} direction - Sort direction
         * @param {boolean} addToExisting - Whether to add to existing sort criteria
         * @param {boolean} isGlobal - Whether this is server-side sorting
         */
        updateCurrentSort(column, direction, addToExisting, isGlobal) {
            if (addToExisting && this.currentSort.multiColumn.length > 0) {
                // Multi-column sorting: add or update criterion
                const existingIndex = this.currentSort.multiColumn.findIndex(c => c.column === column);
                
                if (existingIndex !== -1) {
                    // Update existing criterion
                    this.currentSort.multiColumn[existingIndex].direction = direction;
                } else {
                    // Add new criterion (limit to 5 columns)
                    if (this.currentSort.multiColumn.length < 5) {
                        this.currentSort.multiColumn.push({
                            column,
                            direction,
                            precedence: this.currentSort.multiColumn.length
                        });
                    }
                }
                
                // Update primary sort to first criterion
                if (this.currentSort.multiColumn.length > 0) {
                    const primary = this.currentSort.multiColumn[0];
                    this.currentSort.column = primary.column;
                    this.currentSort.direction = primary.direction;
                }
            } else {
                // Single column sorting or new multi-column sort
                this.currentSort.column = column;
                this.currentSort.direction = direction;
                this.currentSort.multiColumn = [{
                    column,
                    direction,
                    precedence: 0
                }];
            }
            
            this.currentSort.isGlobal = isGlobal;
        }

        /**
         * Apply server-side sorting with multi-column support
         * @param {string} column - Column name
         * @param {string} direction - Sort direction
         * @param {boolean} addToExisting - Whether to add to existing sort criteria
         * @returns {Promise<void>}
         */
        async applyServerSideSort(column, direction, addToExisting = false) {
            try {
                // Get current filters
                const activeFilters = this.enhancedTable.filterController.getActiveFilters();
                const filters = this.apiClient.transformFiltersToBackend(activeFilters, this.enhancedTable.entityType);

                let requestParams = {
                    page: this.enhancedTable.currentPage,
                    size: this.enhancedTable.pageSize,
                    useCache: false // Don't cache sort requests to ensure fresh data
                };

                // Add filter parameters to request
                Object.entries(filters).forEach(([key, value]) => {
                    if (value !== null && value !== undefined && value !== '') {
                        requestParams[key] = value;
                    }
                });

                // Build multi-column sort specification if needed
                if (addToExisting && this.currentSort.multiColumn.length > 0) {
                    // Build multi-column sort string
                    const multiSortCriteria = [...this.currentSort.multiColumn];
                    
                    // Update or add the new criterion
                    const existingIndex = multiSortCriteria.findIndex(c => c.column === column);
                    if (existingIndex !== -1) {
                        multiSortCriteria[existingIndex].direction = direction;
                    } else if (multiSortCriteria.length < 5) {
                        multiSortCriteria.push({
                            column,
                            direction,
                            precedence: multiSortCriteria.length
                        });
                    }
                    
                    // Build sort parameter using Spring Data format: "column1,direction1&sort=column2,direction2"
                    multiSortCriteria.forEach(c => {
                        if (!requestParams.sort) {
                            requestParams.sort = [];
                        }
                        if (Array.isArray(requestParams.sort)) {
                            requestParams.sort.push(`${c.column},${c.direction}`);
                        } else {
                            // Convert single sort to array
                            const existingSort = requestParams.sort;
                            requestParams.sort = [existingSort, `${c.column},${c.direction}`];
                        }
                    });
                } else {
                    // Single column sort using Spring Data format
                    requestParams.sort = `${column},${direction}`;
                }

                console.log('Server-side sort request:', requestParams);

                // Make API request using TableApiClient
                const response = await this.apiClient.getEntities(this.enhancedTable.entityType, requestParams);

                // Transform response if needed
                const data = this.apiClient.transformResponseToFrontend(response);
                this.enhancedTable.updateTableContent(data);

                console.log('Server-side sort completed successfully');

            } catch (error) {
                console.error('Server-side sort failed:', error);
                
                if (error instanceof window.ApiError) {
                    if (error.isNetworkError()) {
                        throw new Error('Network connection failed. Please check your internet connection.');
                    } else if (error.isServerError()) {
                        throw new Error('Server error occurred. Please try again later.');
                    } else if (error.isClientError()) {
                        throw new Error('Invalid sort parameters. Please try a different column.');
                    }
                }
                throw error;
            }
        }

        /**
         * Apply client-side sorting using existing TableSorter
         * @param {string} column - Column name
         * @param {string} direction - Sort direction
         * @returns {Promise<void>}
         */
        async applyClientSideSort(column, direction) {
            try {
                // Initialize fallback sorter if not already done
                if (!this.fallbackSorter) {
                    // Check if TableSorter is available
                    if (typeof window.TableSorter !== 'function') {
                        throw new Error('TableSorter not available for client-side fallback');
                    }

                    this.fallbackSorter = new window.TableSorter(this.enhancedTable.table, {
                        persistState: false,
                        showIndicators: false, // We'll handle indicators ourselves
                        autoDetectTypes: true
                    });
                }

                // Use the new sortByColumnName method for better integration
                if (typeof this.fallbackSorter.sortByColumnName === 'function') {
                    this.fallbackSorter.sortByColumnName(column, direction);
                } else {
                    // Fallback to index-based sorting
                    const columnIndex = this.enhancedTable.getColumnIndex(column);
                    if (columnIndex === -1) {
                        throw new Error(`Column not found: ${column}`);
                    }
                    this.fallbackSorter.sortByColumn(columnIndex, direction);
                }

                // Ensure visual consistency with enhanced table
                this.ensureBackwardCompatibility();

            } catch (error) {
                // Use enhanced error handler for client-side failures
                const errorHandler = this.enhancedTable.options.errorHandler;
                if (errorHandler) {
                    await errorHandler.handleError(error, {
                        operation: 'client_side_sort',
                        column,
                        direction,
                        entityType: this.enhancedTable.options.entityType
                    });
                } else {
                    // Legacy error handling
                    console.error('Client-side sorting failed:', error);
                }
                
                // Try basic DOM-based sorting as last resort
                await this.applyBasicDOMSort(column, direction);
            }
        }

        /**
         * Ensure backward compatibility with existing table functionality
         */
        ensureBackwardCompatibility() {
            // Ensure the table maintains its enhanced-table class
            if (!this.enhancedTable.table.classList.contains('enhanced-table')) {
                this.enhancedTable.table.classList.add('enhanced-table');
            }

            // Ensure sortable headers maintain their classes
            const headers = this.enhancedTable.table.querySelectorAll('thead th');
            headers.forEach((header, index) => {
                const columnName = this.enhancedTable.getColumnName(index);
                if (columnName && !header.classList.contains('no-sort')) {
                    header.classList.add('sortable-header');
                }
            });

            // Dispatch compatibility event for other components
            const event = new CustomEvent('tableSortedClientSide', {
                detail: {
                    column: this.currentSort.column,
                    direction: this.currentSort.direction,
                    isGlobal: false
                }
            });
            this.enhancedTable.table.dispatchEvent(event);
        }

        /**
         * Basic DOM-based sorting as last resort fallback
         * @param {string} column - Column name
         * @param {string} direction - Sort direction
         * @returns {Promise<void>}
         */
        async applyBasicDOMSort(column, direction) {
            const columnIndex = this.enhancedTable.getColumnIndex(column);
            if (columnIndex === -1) {
                throw new Error(`Column not found for basic sort: ${column}`);
            }

            const tbody = this.enhancedTable.table.querySelector('tbody');
            if (!tbody) {
                throw new Error('Table body not found for basic sort');
            }

            const rows = Array.from(tbody.querySelectorAll('tr'));
            if (rows.length <= 1) {
                return; // Nothing to sort
            }

            // Extract and sort row data
            const rowData = rows.map(row => ({
                row: row,
                value: row.cells[columnIndex] ? row.cells[columnIndex].textContent.trim() : ''
            }));

            // Simple text-based sorting
            rowData.sort((a, b) => {
                const aVal = a.value.toLowerCase();
                const bVal = b.value.toLowerCase();
                const result = aVal.localeCompare(bVal);
                return direction === 'desc' ? -result : result;
            });

            // Update DOM
            const fragment = document.createDocumentFragment();
            rowData.forEach(item => {
                fragment.appendChild(item.row);
            });
            tbody.appendChild(fragment);

            console.log('Applied basic DOM sorting as fallback');
        }

        /**
         * Clear current sort
         */
        clearSort() {
            this.currentSort = { column: null, direction: null, isGlobal: false };
            this.updateVisualIndicators(null, null);
            
            if (this.fallbackSorter) {
                this.fallbackSorter.clearSort();
            }
        }

        /**
         * Check if server-side sorting is available
         * @returns {boolean} True if server-side sorting should be attempted
         */
        isServerSideAvailable() {
            // Check if we're online
            if (!navigator.onLine) {
                return false;
            }

            // Check if server-side was recently marked as unavailable
            const unavailableUntil = this.getServerUnavailableTime();
            if (unavailableUntil && Date.now() < unavailableUntil) {
                return false;
            }

            // Check if API client is available
            if (!this.apiClient || typeof this.apiClient.getEntities !== 'function') {
                return false;
            }

            return true;
        }

        /**
         * Mark server-side sorting as temporarily unavailable
         */
        markServerSideUnavailable() {
            // Mark as unavailable for 30 seconds to avoid repeated failed attempts
            const unavailableUntil = Date.now() + (30 * 1000);
            try {
                sessionStorage.setItem('serverSortUnavailable', unavailableUntil.toString());
            } catch (e) {
                // Fallback to in-memory storage if sessionStorage is not available
                this._serverUnavailableUntil = unavailableUntil;
            }
        }

        /**
         * Get the time until which server-side sorting is marked as unavailable
         * @returns {number|null} Timestamp or null if not marked as unavailable
         */
        getServerUnavailableTime() {
            try {
                const stored = sessionStorage.getItem('serverSortUnavailable');
                return stored ? parseInt(stored) : null;
            } catch (e) {
                // Fallback to in-memory storage
                return this._serverUnavailableUntil || null;
            }
        }

        /**
         * Clear server unavailable status
         */
        clearServerUnavailableStatus() {
            try {
                sessionStorage.removeItem('serverSortUnavailable');
            } catch (e) {
                this._serverUnavailableUntil = null;
            }
        }

        /**
         * Show fallback notification to user
         * @param {Error} error - The error that caused the fallback
         */
        showFallbackNotification(error) {
            let message = 'Server sorting is temporarily unavailable. Using local sorting instead.';
            let type = 'warning';

            // Customize message based on error type
            if (error instanceof window.ApiError) {
                if (error.isNetworkError()) {
                    message = 'Network connection issue detected. Sorting data locally instead.';
                } else if (error.isServerError()) {
                    message = 'Server is experiencing issues. Using local sorting for now.';
                } else if (error.isClientError()) {
                    message = 'Invalid sort request. Falling back to local sorting.';
                    type = 'info';
                }
            } else if (!navigator.onLine) {
                message = 'You appear to be offline. Sorting data locally.';
                type = 'info';
            }

            // Show persistent notification with option to retry
            this.enhancedTable.showFallbackMessage(message, type);
        }

        /**
         * Get sort indicator for a column
         * @param {string} column - Column name
         * @returns {Object} Sort indicator info
         */
        getSortIndicator(column) {
            if (this.currentSort.column === column) {
                return {
                    active: true,
                    direction: this.currentSort.direction,
                    isGlobal: this.currentSort.isGlobal
                };
            }
            return { active: false, direction: null, isGlobal: false };
        }

        /**
         * Update visual indicators for sort state with multi-column support
         * @param {string} column - Active column
         * @param {string} direction - Sort direction
         */
        updateVisualIndicators(column, direction) {
            const headers = this.enhancedTable.table.querySelectorAll('thead th');
            
            headers.forEach((header, index) => {
                const headerColumn = this.enhancedTable.getColumnName(index);
                const indicator = header.querySelector('.sort-indicator');
                
                if (!indicator) return;

                // Check if this column is in multi-column sort
                const multiColumnCriterion = this.currentSort.multiColumn.find(c => c.column === headerColumn);
                
                if (multiColumnCriterion) {
                    // This column is part of multi-column sort
                    indicator.className = `sort-indicator ${multiColumnCriterion.direction} active`;
                    
                    if (this.currentSort.isGlobal) {
                        indicator.classList.add('global');
                    }
                    
                    // Add multi-column indicator
                    if (this.currentSort.multiColumn.length > 1) {
                        indicator.classList.add('multi-column');
                        
                        // Add precedence number
                        let precedenceSpan = header.querySelector('.sort-precedence');
                        if (!precedenceSpan) {
                            precedenceSpan = document.createElement('span');
                            precedenceSpan.className = 'sort-precedence';
                            indicator.appendChild(precedenceSpan);
                        }
                        precedenceSpan.textContent = (multiColumnCriterion.precedence + 1).toString();
                    } else {
                        // Remove precedence indicator for single column
                        const precedenceSpan = header.querySelector('.sort-precedence');
                        if (precedenceSpan) {
                            precedenceSpan.remove();
                        }
                        indicator.classList.remove('multi-column');
                    }
                    
                    header.classList.add('sorted');
                    header.setAttribute('aria-sort', multiColumnCriterion.direction);
                } else {
                    // This column is not part of current sort
                    indicator.className = 'sort-indicator neutral';
                    header.classList.remove('sorted');
                    header.setAttribute('aria-sort', 'none');
                    
                    // Remove precedence indicator
                    const precedenceSpan = header.querySelector('.sort-precedence');
                    if (precedenceSpan) {
                        precedenceSpan.remove();
                    }
                }
            });
            
            // Update multi-column sort display
            this.updateMultiColumnSortDisplay();
        }

        /**
         * Update multi-column sort display
         */
        updateMultiColumnSortDisplay() {
            // Find or create multi-column sort display
            let sortDisplay = this.enhancedTable.table.parentNode.querySelector('.multi-column-sort-display');
            
            if (this.currentSort.multiColumn.length > 1) {
                if (!sortDisplay) {
                    sortDisplay = document.createElement('div');
                    sortDisplay.className = 'multi-column-sort-display alert alert-info';
                    this.enhancedTable.table.parentNode.insertBefore(sortDisplay, this.enhancedTable.table);
                }
                
                const sortText = this.currentSort.multiColumn
                    .map((c, index) => `${index + 1}. ${this.formatColumnName(c.column)} (${c.direction.toUpperCase()})`)
                    .join(', ');
                
                sortDisplay.innerHTML = `
                    <div class="d-flex align-items-center">
                        <i class="fas fa-sort me-2"></i>
                        <span class="flex-grow-1">
                            <strong>Multi-column sort:</strong> ${sortText}
                        </span>
                        <button type="button" class="btn btn-sm btn-outline-info me-2" onclick="this.closest('.enhanced-table').enhancedTable.sortController.clearSort()">
                            Clear Sort
                        </button>
                        <button type="button" class="btn-close" onclick="this.parentElement.parentElement.style.display='none'"></button>
                    </div>
                `;
            } else if (sortDisplay) {
                sortDisplay.remove();
            }
        }

        /**
         * Format column name for display
         * @param {string} columnName - Column name
         * @returns {string} Formatted column name
         */
        formatColumnName(columnName) {
            return columnName
                .replace(/([a-z])([A-Z])/g, '$1 $2')
                .replace(/_/g, ' ')
                .replace(/\b\w/g, l => l.toUpperCase());
        }

        /**
         * Get current sort state
         * @returns {Object} Current sort state
         */
        getCurrentSort() {
            return { ...this.currentSort };
        }
    }

    /**
     * Filter Controller - Manages filter state and operations
     */
    class FilterController {
        constructor(enhancedTable) {
            this.enhancedTable = enhancedTable;
            this.activeFilters = new Map();
            this.availableFilters = new Map();
            this.apiClient = window.tableApiClient || new window.TableApiClient();
        }

        /**
         * Apply a filter
         * @param {Object} filter - Filter criteria
         * @returns {Promise<void>}
         */
        async applyFilterAsync(filter) {
            try {
                this.enhancedTable.showLoadingState();
                
                // Add to active filters
                this.activeFilters.set(filter.column, filter);
                
                // Apply filter (server-side if enabled)
                if (this.enhancedTable.options.enableGlobalSort) {
                    await this.applyServerSideFilter();
                } else {
                    await this.applyClientSideFilter();
                }
                
                this.updateFilterUI();
                
            } catch (error) {
                console.error('Filter operation failed:', error);
                this.enhancedTable.showError('Filter operation failed');
            } finally {
                this.enhancedTable.hideLoadingState();
            }
        }

        /**
         * Clear a specific filter
         * @param {string} filterKey - Filter key to clear
         * @returns {Promise<void>}
         */
        async clearFilter(filterKey) {
            this.activeFilters.delete(filterKey);
            await this.refreshData();
            this.updateFilterUI();
        }

        /**
         * Clear all filters
         * @returns {Promise<void>}
         */
        async clearAllFilters() {
            this.activeFilters.clear();
            await this.refreshData();
            this.updateFilterUI();
        }

        /**
         * Get active filters as array
         * @returns {Array} Active filters
         */
        getActiveFilters() {
            return Array.from(this.activeFilters.values());
        }

        /**
         * Apply server-side filtering
         * @returns {Promise<void>}
         */
        async applyServerSideFilter() {
            try {
                // Get current sort
                const currentSort = this.enhancedTable.sortController.getCurrentSort();
                
                // Transform filters to backend format
                const activeFilters = this.getActiveFilters();
                const filters = this.apiClient.transformFiltersToBackend(activeFilters, this.enhancedTable.entityType);

                // Make API request using TableApiClient
                const response = await this.apiClient.getEntities(this.enhancedTable.entityType, {
                    page: 0, // Reset to first page when filtering
                    size: this.enhancedTable.pageSize,
                    sortBy: currentSort.column,
                    sortDir: currentSort.direction,
                    filters: filters,
                    useCache: false // Don't cache filter requests to ensure fresh data
                });

                // Transform response if needed
                const data = this.apiClient.transformResponseToFrontend(response);
                this.enhancedTable.updateTableContent(data);
                this.enhancedTable.currentPage = 0; // Reset page

            } catch (error) {
                if (error instanceof window.ApiError) {
                    console.error('Server-side filter failed:', error.message, 'Status:', error.status);
                    
                    if (error.isNetworkError()) {
                        throw new Error('Network connection failed. Please check your internet connection.');
                    } else if (error.isServerError()) {
                        throw new Error('Server error occurred. Please try again later.');
                    } else if (error.isClientError()) {
                        throw new Error('Invalid filter parameters. Please check your filter values.');
                    }
                }
                throw error;
            }
        }

        /**
         * Apply client-side filtering
         * @returns {Promise<void>}
         */
        async applyClientSideFilter() {
            // For client-side filtering, we would need to implement row filtering logic
            // This is a simplified implementation
            const rows = this.enhancedTable.table.querySelectorAll('tbody tr');
            
            rows.forEach(row => {
                let visible = true;
                
                this.activeFilters.forEach(filter => {
                    const columnIndex = this.enhancedTable.getColumnIndex(filter.column);
                    if (columnIndex !== -1) {
                        const cell = row.cells[columnIndex];
                        if (cell) {
                            const cellText = cell.textContent.toLowerCase().trim();
                            const filterValue = filter.value.toLowerCase().trim();
                            
                            switch (filter.operator) {
                                case 'equals':
                                    if (cellText !== filterValue) visible = false;
                                    break;
                                case 'contains':
                                    if (!cellText.includes(filterValue)) visible = false;
                                    break;
                                default:
                                    if (!cellText.includes(filterValue)) visible = false;
                            }
                        }
                    }
                });
                
                row.style.display = visible ? '' : 'none';
            });
        }

        /**
         * Refresh data with current filters and sort
         * @returns {Promise<void>}
         */
        async refreshData() {
            if (this.enhancedTable.options.enableGlobalSort) {
                await this.applyServerSideFilter();
            } else {
                await this.applyClientSideFilter();
            }
        }

        /**
         * Update filter UI elements
         */
        updateFilterUI() {
            // Update filter badges/indicators
            const filterContainer = this.enhancedTable.table.parentNode.querySelector('.active-filters');
            if (filterContainer) {
                filterContainer.innerHTML = '';
                
                this.activeFilters.forEach(filter => {
                    const badge = document.createElement('span');
                    badge.className = 'badge bg-info me-2';
                    badge.innerHTML = `${filter.column}: ${filter.value} <button type="button" class="btn-close btn-close-white ms-1" data-filter="${filter.column}"></button>`;
                    filterContainer.appendChild(badge);
                });
            }
        }

        /**
         * Load available filter values for a column
         * @param {string} column - Column name
         * @returns {Promise<Array>} Available values
         */
        async loadFilterValues(column) {
            try {
                const values = await this.apiClient.getFilterValues(this.enhancedTable.entityType, column);
                this.availableFilters.set(column, values);
                return values;
            } catch (error) {
                console.warn('Failed to load filter values for', column, error);
                return [];
            }
        }
    }

    /**
     * Bulk Operations Controller - Manages selection and bulk operations
     */
    class BulkController {
        constructor(enhancedTable) {
            this.enhancedTable = enhancedTable;
            this.selectedItems = new Set();
            this.selectAll = false;
            this.totalSelectedCount = 0;
            this.apiClient = window.tableApiClient || new window.TableApiClient();
        }

        /**
         * Toggle selection for an item
         * @param {string} itemId - Item ID
         */
        toggleSelection(itemId) {
            if (this.selectedItems.has(itemId)) {
                this.selectedItems.delete(itemId);
            } else {
                this.selectedItems.add(itemId);
            }
            this.updateSelectionUI();
        }

        /**
         * Toggle select all
         */
        toggleSelectAll() {
            this.selectAll = !this.selectAll;
            
            if (this.selectAll) {
                // Select all visible items
                const checkboxes = this.enhancedTable.table.querySelectorAll('tbody input[type="checkbox"][data-id]');
                checkboxes.forEach(checkbox => {
                    const itemId = checkbox.getAttribute('data-id');
                    this.selectedItems.add(itemId);
                    checkbox.checked = true;
                });
            } else {
                // Deselect all
                this.selectedItems.clear();
                const checkboxes = this.enhancedTable.table.querySelectorAll('tbody input[type="checkbox"][data-id]');
                checkboxes.forEach(checkbox => {
                    checkbox.checked = false;
                });
            }
            
            this.updateSelectionUI();
        }

        /**
         * Clear all selections
         */
        clearSelection() {
            this.selectedItems.clear();
            this.selectAll = false;
            this.totalSelectedCount = 0;
            this.updateSelectionUI();
        }

        /**
         * Execute bulk delete operation with progress feedback
         * @returns {Promise<Object>} Operation result
         */
        async bulkDeleteAsync() {
            if (this.selectedItems.size === 0) {
                throw new Error('No items selected for deletion');
            }

            // Show confirmation dialog
            const confirmed = await this.showConfirmationDialog();
            if (!confirmed) {
                return { cancelled: true };
            }

            let progressModal = null;
            
            try {
                // Show progress modal
                progressModal = this.showProgressModal();
                
                // Create bulk delete request using API client
                const activeFilters = this.enhancedTable.filterController.getActiveFilters();
                const request = this.apiClient.createBulkDeleteRequest(
                    Array.from(this.selectedItems),
                    this.selectAll,
                    activeFilters,
                    this.enhancedTable.entityType
                );

                // Update progress: Starting operation
                this.updateProgressModal(progressModal, {
                    stage: 'starting',
                    message: 'Preparing bulk delete operation...',
                    percentage: 0
                });

                // Execute bulk delete using API client
                const result = await this.apiClient.bulkDelete(this.enhancedTable.entityType, request);
                
                // Update progress: Processing
                this.updateProgressModal(progressModal, {
                    stage: 'processing',
                    message: 'Processing delete operation...',
                    percentage: 50
                });

                // Simulate progress for user feedback (since backend operation is atomic)
                await this.simulateProgress(progressModal, result);
                
                // Update progress: Refreshing
                this.updateProgressModal(progressModal, {
                    stage: 'refreshing',
                    message: 'Refreshing table data...',
                    percentage: 90
                });

                // Clear selections and refresh table
                this.clearSelection();
                await this.enhancedTable.refreshTableAfterBulkDelete(result);
                
                // Update progress: Complete
                this.updateProgressModal(progressModal, {
                    stage: 'complete',
                    message: result.getSummaryMessage ? result.getSummaryMessage() : 
                        `Successfully deleted ${result.deletedCount} items`,
                    percentage: 100
                });

                // Close progress modal after a brief delay
                setTimeout(() => {
                    this.closeProgressModal(progressModal);
                }, 1500);
                
                // Show success message
                const message = result.getSummaryMessage ? result.getSummaryMessage() : 
                    `Successfully deleted ${result.deletedCount} items`;
                this.enhancedTable.showSuccess(message);

                return result;

            } catch (error) {
                console.error('Bulk delete failed:', error);
                
                // Update progress modal with error
                if (progressModal) {
                    this.updateProgressModal(progressModal, {
                        stage: 'error',
                        message: 'Bulk delete operation failed',
                        percentage: 0,
                        error: true
                    });
                    
                    // Close progress modal after showing error
                    setTimeout(() => {
                        this.closeProgressModal(progressModal);
                    }, 3000);
                }
                
                let errorMessage = 'Bulk delete operation failed';
                if (error instanceof window.ApiError) {
                    if (error.isNetworkError()) {
                        errorMessage = 'Network connection failed. Please check your internet connection.';
                    } else if (error.isServerError()) {
                        errorMessage = 'Server error occurred. Please try again later.';
                    } else if (error.isClientError()) {
                        errorMessage = 'Invalid delete request. Please refresh the page and try again.';
                    } else {
                        errorMessage = error.message;
                    }
                }
                
                this.enhancedTable.showError(errorMessage);
                throw error;
            }
        }

        /**
         * Show progress modal for bulk operations
         * @returns {HTMLElement} Progress modal element
         */
        showProgressModal() {
            const modal = document.createElement('div');
            modal.className = 'modal fade';
            modal.setAttribute('data-bs-backdrop', 'static');
            modal.setAttribute('data-bs-keyboard', 'false');
            modal.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header border-0">
                            <h5 class="modal-title">
                                <i class="fas fa-trash me-2"></i>
                                Bulk Delete Progress
                            </h5>
                        </div>
                        <div class="modal-body text-center">
                            <div class="progress-container">
                                <div class="mb-3">
                                    <div class="spinner-border text-primary" role="status">
                                        <span class="visually-hidden">Loading...</span>
                                    </div>
                                </div>
                                
                                <div class="progress mb-3" style="height: 8px;">
                                    <div class="progress-bar progress-bar-striped progress-bar-animated" 
                                         role="progressbar" style="width: 0%"></div>
                                </div>
                                
                                <div class="progress-message text-muted">
                                    Initializing...
                                </div>
                                
                                <div class="progress-details mt-2 small text-muted">
                                    <div class="stage-indicator"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            document.body.appendChild(modal);
            const bootstrapModal = new bootstrap.Modal(modal);
            bootstrapModal.show();
            
            // Store modal reference
            modal.bootstrapModal = bootstrapModal;
            
            return modal;
        }

        /**
         * Update progress modal with current status
         * @param {HTMLElement} modal - Progress modal element
         * @param {Object} progress - Progress information
         */
        updateProgressModal(modal, progress) {
            if (!modal) return;
            
            const progressBar = modal.querySelector('.progress-bar');
            const messageElement = modal.querySelector('.progress-message');
            const stageElement = modal.querySelector('.stage-indicator');
            const spinner = modal.querySelector('.spinner-border');
            
            if (progressBar) {
                progressBar.style.width = `${progress.percentage}%`;
                progressBar.setAttribute('aria-valuenow', progress.percentage);
                
                // Update progress bar color based on stage
                progressBar.className = 'progress-bar progress-bar-striped';
                if (progress.error) {
                    progressBar.classList.add('bg-danger');
                } else if (progress.stage === 'complete') {
                    progressBar.classList.add('bg-success');
                    progressBar.classList.remove('progress-bar-animated');
                } else {
                    progressBar.classList.add('progress-bar-animated', 'bg-primary');
                }
            }
            
            if (messageElement) {
                messageElement.textContent = progress.message;
                if (progress.error) {
                    messageElement.className = 'progress-message text-danger';
                } else if (progress.stage === 'complete') {
                    messageElement.className = 'progress-message text-success';
                } else {
                    messageElement.className = 'progress-message text-muted';
                }
            }
            
            if (stageElement) {
                const stageText = this.getStageText(progress.stage);
                stageElement.textContent = stageText;
            }
            
            // Hide spinner when complete or error
            if (spinner && (progress.stage === 'complete' || progress.error)) {
                spinner.style.display = 'none';
            }
        }

        /**
         * Get user-friendly stage text
         * @param {string} stage - Current stage
         * @returns {string} Stage description
         */
        getStageText(stage) {
            const stageTexts = {
                'starting': 'Preparing operation...',
                'processing': 'Deleting items...',
                'refreshing': 'Updating table...',
                'complete': 'Operation completed successfully',
                'error': 'Operation failed'
            };
            return stageTexts[stage] || 'Processing...';
        }

        /**
         * Simulate progress for better user experience
         * @param {HTMLElement} modal - Progress modal
         * @param {Object} result - Operation result
         */
        async simulateProgress(modal, result) {
            const steps = [
                { percentage: 60, message: 'Validating delete permissions...' },
                { percentage: 70, message: 'Executing database operations...' },
                { percentage: 80, message: 'Cleaning up related data...' }
            ];
            
            for (const step of steps) {
                this.updateProgressModal(modal, {
                    stage: 'processing',
                    message: step.message,
                    percentage: step.percentage
                });
                
                // Small delay for visual feedback
                await new Promise(resolve => setTimeout(resolve, 300));
            }
        }

        /**
         * Close progress modal
         * @param {HTMLElement} modal - Progress modal element
         */
        closeProgressModal(modal) {
            if (modal && modal.bootstrapModal) {
                modal.bootstrapModal.hide();
                
                // Clean up after modal is hidden
                modal.addEventListener('hidden.bs.modal', () => {
                    if (modal.parentNode) {
                        document.body.removeChild(modal);
                    }
                });
            }
        }

        /**
         * Show confirmation dialog for bulk delete with accurate count display
         * @returns {Promise<boolean>} User confirmation
         */
        async showConfirmationDialog() {
            return new Promise(async (resolve) => {
                try {
                    // Calculate accurate count for confirmation
                    const countInfo = await this.getAccurateDeleteCount();
                    
                    const modal = document.createElement('div');
                    modal.className = 'modal fade';
                    modal.innerHTML = `
                        <div class="modal-dialog">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">
                                        <i class="fas fa-exclamation-triangle text-warning me-2"></i>
                                        Confirm Bulk Delete
                                    </h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="alert alert-warning" role="alert">
                                        <i class="fas fa-exclamation-triangle me-2"></i>
                                        <strong>Warning:</strong> This action cannot be undone.
                                    </div>
                                    
                                    <div class="delete-summary">
                                        <h6 class="mb-3">Delete Summary:</h6>
                                        <div class="row">
                                            <div class="col-sm-6">
                                                <div class="card bg-light">
                                                    <div class="card-body text-center py-2">
                                                        <div class="h4 mb-1 text-danger">${countInfo.totalCount}</div>
                                                        <div class="small text-muted">
                                                            ${countInfo.totalCount === 1 ? 'Item' : 'Items'} to Delete
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="col-sm-6">
                                                <div class="card bg-light">
                                                    <div class="card-body text-center py-2">
                                                        <div class="h4 mb-1 text-info">${countInfo.entityType}</div>
                                                        <div class="small text-muted">Entity Type</div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <div class="mt-3">
                                        <p class="mb-2">
                                            ${countInfo.isSelectAll ? 
                                                `<i class="fas fa-globe me-1"></i> You are deleting <strong>all ${countInfo.totalCount} items</strong> that match the current filters.` :
                                                `<i class="fas fa-check-square me-1"></i> You are deleting <strong>${countInfo.totalCount} selected items</strong>.`
                                            }
                                        </p>
                                        
                                        ${countInfo.hasActiveFilters ? 
                                            `<p class="small text-muted">
                                                <i class="fas fa-filter me-1"></i>
                                                Active filters: ${countInfo.activeFiltersText}
                                            </p>` : ''
                                        }
                                    </div>
                                    
                                    <div class="form-check mt-3">
                                        <input class="form-check-input" type="checkbox" id="confirmUnderstand">
                                        <label class="form-check-label" for="confirmUnderstand">
                                            I understand this action cannot be undone
                                        </label>
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                                        <i class="fas fa-times me-1"></i> Cancel
                                    </button>
                                    <button type="button" class="btn btn-danger" id="confirmDelete" disabled>
                                        <i class="fas fa-trash me-1"></i> Delete ${countInfo.totalCount} ${countInfo.totalCount === 1 ? 'Item' : 'Items'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;

                    document.body.appendChild(modal);
                    const bootstrapModal = new bootstrap.Modal(modal);
                    
                    // Enable delete button only when checkbox is checked
                    const confirmCheckbox = modal.querySelector('#confirmUnderstand');
                    const deleteButton = modal.querySelector('#confirmDelete');
                    
                    confirmCheckbox.addEventListener('change', () => {
                        deleteButton.disabled = !confirmCheckbox.checked;
                    });
                    
                    deleteButton.addEventListener('click', () => {
                        bootstrapModal.hide();
                        resolve(true);
                    });

                    modal.addEventListener('hidden.bs.modal', () => {
                        document.body.removeChild(modal);
                        resolve(false);
                    });

                    bootstrapModal.show();
                    
                } catch (error) {
                    console.error('Error showing confirmation dialog:', error);
                    // Fallback to simple confirmation
                    const confirmed = confirm(`Are you sure you want to delete ${this.selectedItems.size} selected items? This action cannot be undone.`);
                    resolve(confirmed);
                }
            });
        }

        /**
         * Get accurate count for delete operation
         * @returns {Promise<Object>} Count information
         */
        async getAccurateDeleteCount() {
            const activeFilters = this.enhancedTable.filterController.getActiveFilters();
            const entityType = this.enhancedTable.entityType || 'items';
            
            let totalCount = this.selectedItems.size;
            let isSelectAll = this.selectAll;
            
            // If select all is enabled, get the total count from server
            if (this.selectAll) {
                try {
                    // Get total count with current filters
                    const response = await this.apiClient.getEntities(this.enhancedTable.entityType, {
                        page: 0,
                        size: 1, // We only need the count
                        filters: this.apiClient.transformFiltersToBackend(activeFilters, entityType),
                        useCache: false
                    });
                    
                    if (response && response.totalElements !== undefined) {
                        totalCount = response.totalElements;
                    }
                } catch (error) {
                    console.warn('Could not get accurate total count, using selected items count:', error);
                    totalCount = this.selectedItems.size;
                    isSelectAll = false;
                }
            }
            
            // Build active filters text
            const activeFiltersText = activeFilters.length > 0 ? 
                activeFilters.map(f => `${f.column}=${f.value}`).join(', ') : 
                'none';
            
            return {
                totalCount,
                isSelectAll,
                entityType: entityType.charAt(0).toUpperCase() + entityType.slice(1),
                hasActiveFilters: activeFilters.length > 0,
                activeFiltersText
            };
        }

        /**
         * Update selection UI elements
         */
        updateSelectionUI() {
            // Update select all checkbox
            const selectAllCheckbox = this.enhancedTable.table.querySelector('thead input[type="checkbox"]');
            if (selectAllCheckbox) {
                selectAllCheckbox.checked = this.selectAll;
                selectAllCheckbox.indeterminate = !this.selectAll && this.selectedItems.size > 0;
            }

            // Update individual checkboxes
            const checkboxes = this.enhancedTable.table.querySelectorAll('tbody input[type="checkbox"][data-id]');
            checkboxes.forEach(checkbox => {
                const itemId = checkbox.getAttribute('data-id');
                checkbox.checked = this.selectedItems.has(itemId);
            });

            // Update bulk action buttons
            const bulkActions = document.querySelector('.bulk-actions');
            if (bulkActions) {
                bulkActions.style.display = this.selectedItems.size > 0 ? 'block' : 'none';
                
                const countElement = bulkActions.querySelector('.selected-count');
                if (countElement) {
                    countElement.textContent = this.selectedItems.size;
                }
            }
        }

        /**
         * Get current selection state
         * @returns {Object} Selection state
         */
        getSelectionState() {
            return {
                selectedItems: Array.from(this.selectedItems),
                selectAll: this.selectAll,
                totalSelectedCount: this.selectedItems.size
            };
        }
    }

    /**
     * Main Enhanced Table Component
     * Coordinates all controllers and manages table state
     */
    class EnhancedTable {
        constructor(tableElement, options = {}) {
            this.table = tableElement;
            this.options = this.mergeDefaultOptions(options);
            
            // Initialize API client
            this.apiClient = window.tableApiClient || new window.TableApiClient();
            
            // Initialize controllers
            this.sortController = new SortController(this);
            this.filterController = new FilterController(this);
            this.bulkController = new BulkController(this);
            
            // Table state
            this.entityType = this.options.entityType || this.detectEntityType();
            this.currentPage = 0;
            this.pageSize = this.options.pageSize || 10;
            this.totalElements = 0;
            
            // Initialize
            this.init();
        }

        /**
         * Merge default options with provided options
         * @param {Object} options - User options
         * @returns {Object} Merged options
         */
        mergeDefaultOptions(options) {
            return Object.assign({
                entityType: null,
                enableGlobalSort: true,
                enableFiltering: true,
                enableBulkOperations: true,
                pageSize: 10,
                showIndicators: true,
                persistState: true
            }, options);
        }

        /**
         * Initialize the enhanced table
         */
        init() {
            try {
                this.setupTableStructure();
                this.setupEventListeners();
                this.setupAccessibility();
                this.setupNetworkMonitoring();
                
                if (this.options.persistState) {
                    this.restoreState();
                }
                
                console.log('EnhancedTable initialized for:', this.entityType);
            } catch (error) {
                console.error('EnhancedTable initialization failed:', error);
                this.showError('Enhanced table functionality unavailable. Using basic table features.');
                
                // Ensure basic functionality still works
                this.ensureBasicFunctionality();
            }
        }

        /**
         * Setup network connectivity monitoring
         */
        setupNetworkMonitoring() {
            // Monitor online/offline status
            window.addEventListener('online', () => {
                this.sortController.clearServerUnavailableStatus();
                this.clearFallbackWarning();
                this.showSuccess('Connection restored. Server sorting is now available.');
            });

            window.addEventListener('offline', () => {
                this.showFallbackMessage('You are now offline. Using local sorting only.', 'info');
            });
        }

        /**
         * Ensure basic functionality works even if enhanced features fail
         */
        ensureBasicFunctionality() {
            try {
                // Ensure basic table sorting works
                if (typeof window.TableSorter === 'function') {
                    const basicSorter = new window.TableSorter(this.table, {
                        persistState: false,
                        showIndicators: true
                    });
                    this.table.basicSorter = basicSorter;
                    console.log('Basic table sorting enabled as fallback');
                }
            } catch (error) {
                console.warn('Basic table sorting also failed:', error);
            }
        }

        /**
         * Setup table structure and classes
         */
        setupTableStructure() {
            this.table.classList.add('enhanced-table');
            
            // Add sort indicators to headers
            const headers = this.table.querySelectorAll('thead th.sortable-header');
            headers.forEach(header => {
                if (!header.querySelector('.sort-indicator')) {
                    const indicator = document.createElement('span');
                    indicator.className = 'sort-indicator neutral';
                    indicator.setAttribute('aria-hidden', 'true');
                    header.appendChild(indicator);
                }
            });

            // Add bulk selection checkboxes if enabled
            if (this.options.enableBulkOperations) {
                this.addBulkSelectionUI();
            }
        }

        /**
         * Add bulk selection UI elements
         */
        addBulkSelectionUI() {
            // Add select all checkbox to header
            const headerRow = this.table.querySelector('thead tr');
            if (headerRow && !headerRow.querySelector('.select-column')) {
                const selectHeader = document.createElement('th');
                selectHeader.className = 'select-column';
                selectHeader.innerHTML = '<input type="checkbox" class="form-check-input" title="Select all">';
                headerRow.insertBefore(selectHeader, headerRow.firstChild);
            }

            // Add individual checkboxes to rows
            const bodyRows = this.table.querySelectorAll('tbody tr');
            bodyRows.forEach(row => {
                if (!row.querySelector('.select-column')) {
                    const selectCell = document.createElement('td');
                    selectCell.className = 'select-column';
                    const itemId = this.extractItemId(row);
                    selectCell.innerHTML = `<input type="checkbox" class="form-check-input" data-id="${itemId}">`;
                    row.insertBefore(selectCell, row.firstChild);
                }
            });
        }

        /**
         * Setup event listeners
         */
        setupEventListeners() {
            // Sort event listeners with multi-column support
            const sortableHeaders = this.table.querySelectorAll('thead th.sortable-header');
            sortableHeaders.forEach((header, index) => {
                header.addEventListener('click', (e) => {
                    e.preventDefault();
                    const column = this.getColumnName(index);
                    const currentSort = this.sortController.getCurrentSort();
                    
                    // Determine direction
                    let direction = 'asc';
                    const multiColumnCriterion = currentSort.multiColumn.find(c => c.column === column);
                    if (multiColumnCriterion) {
                        direction = multiColumnCriterion.direction === 'asc' ? 'desc' : 'asc';
                    }
                    
                    // Check for multi-column sort (Ctrl+click or Cmd+click)
                    const addToExisting = e.ctrlKey || e.metaKey;
                    
                    this.sortController.applySortAsync(column, direction, addToExisting);
                });
                
                // Add keyboard support for multi-column sorting
                header.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        
                        // Simulate click with Ctrl key if Shift is held
                        const simulatedEvent = {
                            preventDefault: () => {},
                            ctrlKey: e.shiftKey,
                            metaKey: false
                        };
                        
                        header.dispatchEvent(new MouseEvent('click', simulatedEvent));
                    }
                });
            });

            // Bulk selection event listeners
            if (this.options.enableBulkOperations) {
                // Select all checkbox
                const selectAllCheckbox = this.table.querySelector('thead input[type="checkbox"]');
                if (selectAllCheckbox) {
                    selectAllCheckbox.addEventListener('change', () => {
                        this.bulkController.toggleSelectAll();
                    });
                }

                // Individual checkboxes
                this.table.addEventListener('change', (e) => {
                    if (e.target.type === 'checkbox' && e.target.hasAttribute('data-id')) {
                        const itemId = e.target.getAttribute('data-id');
                        this.bulkController.toggleSelection(itemId);
                    }
                });
            }
        }

        /**
         * Setup accessibility features
         */
        setupAccessibility() {
            // Add ARIA labels and keyboard navigation
            const headers = this.table.querySelectorAll('thead th.sortable-header');
            headers.forEach(header => {
                header.setAttribute('tabindex', '0');
                header.setAttribute('role', 'columnheader');
                header.setAttribute('aria-sort', 'none');
                
                header.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        header.click();
                    }
                });
            });
        }

        /**
         * Detect entity type from table or URL
         * @returns {string} Entity type
         */
        detectEntityType() {
            // Try to detect from table ID or class
            if (this.table.id.includes('visits')) return 'visits';
            if (this.table.id.includes('owners')) return 'owners';
            if (this.table.id.includes('pets')) return 'pets';
            if (this.table.id.includes('veterinarians')) return 'veterinarians';
            
            // Try to detect from URL
            const path = window.location.pathname;
            if (path.includes('/visits')) return 'visits';
            if (path.includes('/owners')) return 'owners';
            if (path.includes('/pets')) return 'pets';
            if (path.includes('/veterinarians')) return 'veterinarians';
            
            return 'visits'; // Default fallback
        }

        /**
         * Get column name by index
         * @param {number} index - Column index
         * @returns {string} Column name
         */
        getColumnName(index) {
            const header = this.table.querySelector(`thead th:nth-child(${index + 1})`);
            return header ? header.getAttribute('data-column') || header.textContent.trim().toLowerCase() : '';
        }

        /**
         * Get column index by name
         * @param {string} columnName - Column name
         * @returns {number} Column index (-1 if not found)
         */
        getColumnIndex(columnName) {
            const headers = this.table.querySelectorAll('thead th');
            for (let i = 0; i < headers.length; i++) {
                const header = headers[i];
                const name = header.getAttribute('data-column') || header.textContent.trim().toLowerCase();
                if (name === columnName) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Extract item ID from table row
         * @param {HTMLElement} row - Table row
         * @returns {string} Item ID
         */
        extractItemId(row) {
            // Try to find ID in data attribute or action links
            const dataId = row.getAttribute('data-id');
            if (dataId) return dataId;
            
            const actionLink = row.querySelector('a[href*="/"]');
            if (actionLink) {
                const href = actionLink.getAttribute('href');
                const match = href.match(/\/(\d+)(?:\/|$)/);
                if (match) return match[1];
            }
            
            return row.rowIndex.toString(); // Fallback to row index
        }

        /**
         * Update table content with new data
         * @param {Object} data - Server response data
         */
        updateTableContent(data) {
            // This would update the table with new data from server
            // For now, we'll trigger a page refresh or update via existing mechanisms
            console.log('Updating table content:', data);
            
            // Update pagination info
            if (data.page) {
                this.currentPage = data.page.number;
                this.totalElements = data.page.totalElements;
                this.updatePaginationUI();
            }
            
            // Dispatch custom event for other components
            const event = new CustomEvent('tableUpdated', { 
                detail: { data, entityType: this.entityType } 
            });
            this.table.dispatchEvent(event);
        }

        /**
         * Update pagination UI
         */
        updatePaginationUI() {
            const paginationContainer = document.querySelector('.pagination');
            if (paginationContainer && this.totalElements > this.pageSize) {
                // Update pagination controls
                // This would be implemented based on the existing pagination structure
            }
        }

        /**
         * Refresh table data after bulk delete operation
         * @param {Object} result - Bulk operation result
         * @returns {Promise<void>}
         */
        async refreshTableAfterBulkDelete(result) {
            try {
                // Clear any cached data since items were deleted
                if (this.apiClient) {
                    this.apiClient.clearEntityCache(this.entityType);
                }
                
                // If all items on current page were deleted, go to previous page
                const currentPageItems = this.table.querySelectorAll('tbody tr').length;
                const deletedFromCurrentPage = Math.min(result.deletedCount, currentPageItems);
                
                if (deletedFromCurrentPage === currentPageItems && this.currentPage > 0) {
                    this.currentPage = Math.max(0, this.currentPage - 1);
                }
                
                // Refresh the table data
                await this.refreshData();
                
                // Update pagination if needed
                this.updatePaginationAfterDelete(result);
                
                // Dispatch custom event for other components
                const event = new CustomEvent('tableBulkDeleted', { 
                    detail: { 
                        result, 
                        entityType: this.entityType,
                        currentPage: this.currentPage
                    } 
                });
                this.table.dispatchEvent(event);
                
            } catch (error) {
                console.error('Failed to refresh table after bulk delete:', error);
                
                // Fallback: reload the page if refresh fails
                this.showWarning('Table refresh failed. The page will reload to show updated data.');
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            }
        }

        /**
         * Update pagination after delete operation
         * @param {Object} result - Bulk operation result
         */
        updatePaginationAfterDelete(result) {
            // Update total elements count
            if (this.totalElements > 0) {
                this.totalElements = Math.max(0, this.totalElements - result.deletedCount);
            }
            
            // Update pagination UI
            this.updatePaginationUI();
            
            // If current page is now empty and we're not on the first page, go back
            const totalPages = Math.ceil(this.totalElements / this.pageSize);
            if (this.currentPage >= totalPages && totalPages > 0) {
                this.currentPage = totalPages - 1;
            }
        }

        /**
         * Refresh table data
         * @returns {Promise<void>}
         */
        async refreshData() {
            await this.filterController.refreshData();
        }

        /**
         * Show loading state
         */
        showLoadingState() {
            this.table.classList.add('loading');
            
            // Add loading overlay if it doesn't exist
            let overlay = this.table.parentNode.querySelector('.loading-overlay');
            if (!overlay) {
                overlay = document.createElement('div');
                overlay.className = 'loading-overlay';
                overlay.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
                this.table.parentNode.style.position = 'relative';
                this.table.parentNode.appendChild(overlay);
            }
            overlay.style.display = 'flex';
        }

        /**
         * Hide loading state
         */
        hideLoadingState() {
            this.table.classList.remove('loading');
            
            const overlay = this.table.parentNode.querySelector('.loading-overlay');
            if (overlay) {
                overlay.style.display = 'none';
            }
        }

        /**
         * Show success message
         * @param {string} message - Success message
         */
        showSuccess(message) {
            this.showMessage(message, 'success');
        }

        /**
         * Show warning message
         * @param {string} message - Warning message
         */
        showWarning(message) {
            this.showMessage(message, 'warning');
        }

        /**
         * Show error message
         * @param {string} message - Error message
         */
        showError(message) {
            this.showMessage(message, 'danger');
        }

        /**
         * Show message with specified type
         * @param {string} message - Message text
         * @param {string} type - Message type (success, warning, danger, info)
         */
        showMessage(message, type = 'info') {
            // Create or update message container
            let messageContainer = this.table.parentNode.querySelector('.table-messages');
            if (!messageContainer) {
                messageContainer = document.createElement('div');
                messageContainer.className = 'table-messages';
                this.table.parentNode.insertBefore(messageContainer, this.table);
            }

            const alert = document.createElement('div');
            alert.className = `alert alert-${type} alert-dismissible fade show`;
            alert.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;

            messageContainer.appendChild(alert);

            // Auto-dismiss after 5 seconds
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.remove();
                }
            }, 5000);
        }

        /**
         * Show fallback message with retry option
         * @param {string} message - Message text
         * @param {string} type - Message type (warning, info)
         */
        showFallbackMessage(message, type = 'warning') {
            // Create or update message container
            let messageContainer = this.table.parentNode.querySelector('.table-messages');
            if (!messageContainer) {
                messageContainer = document.createElement('div');
                messageContainer.className = 'table-messages';
                this.table.parentNode.insertBefore(messageContainer, this.table);
            }

            // Remove any existing fallback messages
            const existingFallback = messageContainer.querySelector('.fallback-message');
            if (existingFallback) {
                existingFallback.remove();
            }

            const alert = document.createElement('div');
            alert.className = `alert alert-${type} alert-dismissible fade show fallback-message`;
            alert.innerHTML = `
                <div class="d-flex align-items-center">
                    <div class="flex-grow-1">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-${type === 'warning' ? 'warning' : 'primary'} me-2" onclick="this.closest('.enhanced-table').enhancedTable.retryServerSideSort()">
                        Retry Server Sort
                    </button>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `;

            messageContainer.appendChild(alert);

            // Auto-dismiss after 10 seconds (longer for fallback messages)
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.remove();
                }
            }, 10000);
        }

        /**
         * Clear fallback warning messages
         */
        clearFallbackWarning() {
            const messageContainer = this.table.parentNode.querySelector('.table-messages');
            if (messageContainer) {
                const fallbackMessages = messageContainer.querySelectorAll('.fallback-message');
                fallbackMessages.forEach(msg => msg.remove());
            }
        }

        /**
         * Retry server-side sorting
         */
        retryServerSideSort() {
            // Clear the server unavailable status
            this.sortController.clearServerUnavailableStatus();
            
            // Clear fallback warnings
            this.clearFallbackWarning();
            
            // Re-apply current sort if any
            const currentSort = this.sortController.getCurrentSort();
            if (currentSort.column && currentSort.direction) {
                this.sortController.applySortAsync(currentSort.column, currentSort.direction);
            } else {
                this.showSuccess('Server sorting is now available. Click any column header to sort.');
            }
        }

        /**
         * Restore saved state
         */
        restoreState() {
            // Restore sort state
            const savedSort = localStorage.getItem(`enhancedTable_${this.entityType}_sort`);
            if (savedSort) {
                try {
                    const sortState = JSON.parse(savedSort);
                    this.sortController.applySortAsync(sortState.column, sortState.direction);
                } catch (error) {
                    console.warn('Failed to restore sort state:', error);
                }
            }
        }

        /**
         * Save current state
         */
        saveState() {
            if (!this.options.persistState) return;
            
            const sortState = this.sortController.getCurrentSort();
            if (sortState.column) {
                localStorage.setItem(`enhancedTable_${this.entityType}_sort`, JSON.stringify(sortState));
            }
        }

        /**
         * Destroy the enhanced table instance
         */
        destroy() {
            // Abort any pending API requests
            if (this.apiClient) {
                this.apiClient.abortAllRequests();
            }
            
            // Clean up event listeners and DOM modifications
            this.table.classList.remove('enhanced-table', 'loading');
            
            // Remove added elements
            const indicators = this.table.querySelectorAll('.sort-indicator');
            indicators.forEach(indicator => indicator.remove());
            
            const selectColumns = this.table.querySelectorAll('.select-column');
            selectColumns.forEach(column => column.remove());
            
            // Clean up network event listeners
            window.removeEventListener('online', this.onlineHandler);
            window.removeEventListener('offline', this.offlineHandler);
            
            // Clear state
            this.saveState();
            
            // Clear entity-specific cache
            if (this.apiClient) {
                this.apiClient.clearEntityCache(this.entityType);
            }
        }

        /**
         * Test server connectivity and capabilities
         */
        async testServerConnectivity() {
            if (!this.options.enableGlobalSort) {
                return; // Skip if server-side sorting is disabled
            }

            try {
                // Test with a simple request
                await this.apiClient.getEntities(this.entityType, {
                    page: 0,
                    size: 1,
                    useCache: false
                });
                
                console.log('Server connectivity test passed for', this.entityType);
            } catch (error) {
                console.warn('Server connectivity test failed:', error);
                this.sortController.markServerSideUnavailable();
                
                // Show initial fallback notification if server is unavailable
                this.showFallbackMessage(
                    'Server sorting is currently unavailable. Using local sorting instead.',
                    'info'
                );
            }
        }

        /**
         * Get current fallback status
         * @returns {Object} Fallback status information
         */
        getFallbackStatus() {
            return {
                isServerSideAvailable: this.sortController.isServerSideAvailable(),
                isOnline: navigator.onLine,
                hasTableSorter: typeof window.TableSorter === 'function',
                currentSortMode: this.sortController.currentSort.isGlobal ? 'server' : 'client',
                serverUnavailableUntil: this.sortController.getServerUnavailableTime()
            };
        }
    }

    // Auto-initialization with enhanced error handling
    document.addEventListener('DOMContentLoaded', function() {
        console.log('EnhancedTable: Auto-initializing enhanced tables...');
        
        const enhancedTables = document.querySelectorAll('.table-sortable');
        const initializedTables = [];
        const failedTables = [];
        
        enhancedTables.forEach(table => {
            try {
                const options = {
                    enableGlobalSort: true,
                    enableFiltering: true,
                    enableBulkOperations: true,
                    persistState: true
                };
                
                const enhancedTable = new EnhancedTable(table, options);
                
                // Store reference for potential later access
                table.enhancedTable = enhancedTable;
                initializedTables.push(table.id || 'unnamed');
                
                // Test server connectivity on initialization
                enhancedTable.testServerConnectivity();
                
            } catch (error) {
                console.error('Failed to initialize EnhancedTable for table:', table, error);
                failedTables.push(table.id || 'unnamed');
                
                // Try to initialize basic sorting as fallback
                try {
                    if (typeof window.TableSorter === 'function') {
                        const basicSorter = new window.TableSorter(table, {
                            persistState: true,
                            showIndicators: true
                        });
                        table.tableSorter = basicSorter;
                        console.log('Initialized basic TableSorter as fallback for:', table.id || 'unnamed');
                    }
                } catch (fallbackError) {
                    console.error('Even basic table sorting failed for:', table, fallbackError);
                }
            }
        });
        
        console.log(`EnhancedTable: Successfully initialized ${initializedTables.length} tables:`, initializedTables);
        if (failedTables.length > 0) {
            console.warn(`EnhancedTable: Failed to initialize ${failedTables.length} tables:`, failedTables);
        }
    });

    // Export to global scope
    window.EnhancedTable = EnhancedTable;
    window.EnhancedTableControllers = {
        SortController,
        FilterController,
        BulkController
    };

})();