/**
 * Table Sorting Functionality
 * Provides client-side sorting capabilities for HTML tables
 * 
 * Requirements: 1.1, 2.1, 2.3
 */

(function() {
    'use strict';

    /**
     * Base class for data type handlers
     */
    class DataTypeHandler {
        constructor(name) {
            this.name = name;
        }

        /**
         * Detect if this handler can process the given values
         * @param {Array} values - Sample values from the column
         * @returns {boolean} - True if this handler can process the data
         */
        detect(values) {
            throw new Error('Must implement detect method');
        }

        /**
         * Compare two values for sorting
         * @param {*} a - First value
         * @param {*} b - Second value
         * @returns {number} - Comparison result (-1, 0, 1)
         */
        compare(a, b) {
            throw new Error('Must implement compare method');
        }

        /**
         * Preprocess a value before comparison
         * @param {*} value - Raw value
         * @returns {*} - Processed value
         */
        preprocess(value) {
            return value;
        }
    }

    /**
     * Text data type handler
     */
    class TextDataTypeHandler extends DataTypeHandler {
        constructor() {
            super('text');
        }

        detect(values) {
            // Text handler is the default fallback
            return true;
        }

        compare(a, b) {
            const aStr = String(a || '').toLowerCase().trim();
            const bStr = String(b || '').toLowerCase().trim();
            
            // Empty values go to the end
            if (!aStr && !bStr) return 0;
            if (!aStr) return 1;
            if (!bStr) return -1;
            
            return aStr.localeCompare(bStr);
        }
    }

    /**
     * Numeric data type handler
     */
    class NumericDataTypeHandler extends DataTypeHandler {
        constructor() {
            super('number');
        }

        detect(values) {
            const numericValues = values.filter(v => this.isNumeric(v));
            return numericValues.length / values.length > 0.6; // 60% threshold
        }

        isNumeric(value) {
            if (value === null || value === undefined || value === '') return false;
            const cleaned = String(value).replace(/[$,\s%]/g, '');
            return !isNaN(cleaned) && !isNaN(parseFloat(cleaned));
        }

        preprocess(value) {
            if (value === null || value === undefined || value === '') return Infinity; // Empty values at end
            const cleaned = String(value).replace(/[$,\s%]/g, '');
            const num = parseFloat(cleaned);
            return isNaN(num) ? Infinity : num; // Invalid numbers at end too
        }

        compare(a, b) {
            const numA = this.preprocess(a);
            const numB = this.preprocess(b);
            return numA - numB;
        }
    }

    /**
     * Date/Time data type handler
     */
    class DateTimeDataTypeHandler extends DataTypeHandler {
        constructor() {
            super('datetime');
        }

        detect(values) {
            const dateValues = values.filter(v => this.isDate(v));
            return dateValues.length / values.length > 0.6; // 60% threshold
        }

        isDate(value) {
            if (!value) return false;
            const date = new Date(value);
            return !isNaN(date.getTime());
        }

        preprocess(value) {
            if (!value) return new Date('9999-12-31'); // Empty values at end
            const date = new Date(value);
            return isNaN(date.getTime()) ? new Date('9999-12-31') : date; // Invalid dates at end too
        }

        compare(a, b) {
            const dateA = this.preprocess(a);
            const dateB = this.preprocess(b);
            return dateA.getTime() - dateB.getTime();
        }
    }

    /**
     * Status/Enum data type handler
     */
    class StatusDataTypeHandler extends DataTypeHandler {
        constructor() {
            super('status');
            this.statusOrder = {
                'pending': 1,
                'in progress': 2,
                'completed': 3,
                'cancelled': 4,
                'unknown': 5
            };
        }

        detect(values) {
            const statusValues = values.filter(v => this.isStatus(v));
            return statusValues.length / values.length > 0.5; // 50% threshold
        }

        isStatus(value) {
            if (!value) return false;
            const lowerValue = String(value).toLowerCase().trim();
            return Object.keys(this.statusOrder).includes(lowerValue);
        }

        preprocess(value) {
            if (!value) return 999; // Empty values at end
            const lowerValue = String(value).toLowerCase().trim();
            return this.statusOrder[lowerValue] || 999;
        }

        compare(a, b) {
            const orderA = this.preprocess(a);
            const orderB = this.preprocess(b);
            return orderA - orderB;
        }
    }

    /**
     * State Manager for persistence
     */
    class StateManager {
        constructor(tableId) {
            this.tableId = tableId;
            this.storageKey = `tableSortState_${tableId}`;
        }

        save(columnIndex, direction) {
            if (!this.isSupported()) return;
            
            const state = {
                column: columnIndex,
                direction: direction,
                timestamp: Date.now()
            };
            
            try {
                localStorage.setItem(this.storageKey, JSON.stringify(state));
            } catch (e) {
                console.warn('Failed to save sort state:', e);
            }
        }

        load() {
            if (!this.isSupported()) return null;
            
            try {
                const stored = localStorage.getItem(this.storageKey);
                return stored ? JSON.parse(stored) : null;
            } catch (e) {
                console.warn('Failed to load sort state:', e);
                return null;
            }
        }

        clear() {
            if (!this.isSupported()) return;
            
            try {
                localStorage.removeItem(this.storageKey);
            } catch (e) {
                console.warn('Failed to clear sort state:', e);
            }
        }

        isSupported() {
            try {
                return typeof Storage !== 'undefined' && localStorage;
            } catch (e) {
                return false;
            }
        }
    }

    /**
     * Accessibility Manager
     */
    class AccessibilityManager {
        constructor(table) {
            this.table = table;
        }

        setupKeyboardNavigation() {
            const headers = this.table.querySelectorAll('thead th.sortable-header');
            
            headers.forEach(header => {
                // Make headers focusable
                if (!header.hasAttribute('tabindex')) {
                    header.setAttribute('tabindex', '0');
                }
                
                // Add keyboard event listeners
                header.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        header.click();
                    }
                });
            });
        }

        updateAriaAttributes(columnIndex, direction) {
            const headers = this.table.querySelectorAll('thead th');
            
            headers.forEach((header, index) => {
                if (index === columnIndex) {
                    header.setAttribute('aria-sort', direction || 'none');
                } else {
                    header.setAttribute('aria-sort', 'none');
                }
            });
        }

        announceSort(columnName, direction) {
            const announcement = `Table sorted by ${columnName} in ${direction === 'asc' ? 'ascending' : 'descending'} order`;
            
            // Create or update live region
            let liveRegion = document.getElementById('sort-announcements');
            if (!liveRegion) {
                liveRegion = document.createElement('div');
                liveRegion.id = 'sort-announcements';
                liveRegion.setAttribute('aria-live', 'polite');
                liveRegion.setAttribute('aria-atomic', 'true');
                liveRegion.className = 'sr-only';
                document.body.appendChild(liveRegion);
            }
            
            liveRegion.textContent = announcement;
        }
    }

    /**
     * Main TableSorter class
     */
    class TableSorter {
        constructor(tableElement, options = {}) {
            this.table = tableElement;
            this.tbody = tableElement.querySelector('tbody');
            this.headers = Array.from(tableElement.querySelectorAll('thead th'));
            
            if (!this.tbody || this.headers.length === 0) {
                console.warn('TableSorter: Invalid table structure');
                return;
            }

            // Configuration
            this.options = this.mergeDefaultOptions(options);
            this.currentSort = { column: null, direction: null };
            
            // Initialize components
            this.dataTypeHandlers = this.initializeDataTypeHandlers();
            this.stateManager = new StateManager(this.table.id || 'table-' + Date.now());
            this.accessibilityManager = new AccessibilityManager(this.table);
            
            // Column metadata
            this.columnMetadata = this.analyzeColumns();
            
            // Initialize
            this.init();
        }

        mergeDefaultOptions(options) {
            return Object.assign({
                persistState: true,
                showIndicators: true,
                multiSort: false,
                autoDetectTypes: true
            }, options);
        }

        initializeDataTypeHandlers() {
            return [
                new StatusDataTypeHandler(),
                new DateTimeDataTypeHandler(),
                new NumericDataTypeHandler(),
                new TextDataTypeHandler() // Fallback handler
            ];
        }

        init() {
            try {
                this.setupTableClasses();
                this.setupEventListeners();
                this.accessibilityManager.setupKeyboardNavigation();
                
                if (this.options.persistState) {
                    this.restoreState();
                }
                
                console.log('TableSorter initialized for table:', this.table.id || 'unnamed');
            } catch (error) {
                console.error('TableSorter initialization failed:', error);
                this.showErrorMessage('Sorting functionality unavailable');
            }
        }

        setupTableClasses() {
            this.table.classList.add('table-sortable');
            
            this.headers.forEach((header, index) => {
                const metadata = this.columnMetadata[index];
                if (metadata && metadata.sortable) {
                    header.classList.add('sortable-header');
                    
                    // Add sort indicator
                    if (this.options.showIndicators) {
                        const indicator = document.createElement('span');
                        indicator.className = 'sort-indicator neutral';
                        indicator.setAttribute('aria-hidden', 'true');
                        header.appendChild(indicator);
                    }
                }
            });
        }

        analyzeColumns() {
            return this.headers.map((header, index) => {
                const isNoSort = header.classList.contains('no-sort');
                const dataType = header.getAttribute('data-sort-type') || 
                               (this.options.autoDetectTypes ? this.detectDataType(index) : 'text');
                
                return {
                    index,
                    name: header.textContent.trim(),
                    dataType,
                    sortable: !isNoSort,
                    element: header
                };
            });
        }

        detectDataType(columnIndex) {
            const sampleData = this.getSampleDataForColumn(columnIndex);
            
            for (const handler of this.dataTypeHandlers) {
                if (handler.name !== 'text' && handler.detect(sampleData)) {
                    return handler.name;
                }
            }
            
            return 'text'; // Default fallback
        }

        getSampleDataForColumn(columnIndex) {
            const rows = Array.from(this.tbody.querySelectorAll('tr'));
            const sampleSize = Math.min(10, rows.length);
            const sample = [];
            
            for (let i = 0; i < sampleSize; i++) {
                const cell = rows[i].cells[columnIndex];
                if (cell) {
                    sample.push(cell.textContent.trim());
                }
            }
            
            return sample;
        }

        setupEventListeners() {
            this.headers.forEach((header, index) => {
                const metadata = this.columnMetadata[index];
                if (metadata && metadata.sortable) {
                    // Add click event listener for sorting
                    header.addEventListener('click', (e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        
                        // Ensure header is focusable and provide visual feedback
                        header.focus();
                        
                        // Trigger sort
                        this.sortByColumn(index);
                    });
                    
                    // Add visual feedback for sortable headers
                    header.style.cursor = 'pointer';
                    header.setAttribute('title', `Click to sort by ${metadata.name}`);
                }
            });
        }

        sortByColumn(columnIndex, direction = null) {
            try {
                const column = this.columnMetadata[columnIndex];
                if (!column || !column.sortable) return;

                // Determine sort direction
                if (direction === null) {
                    direction = this.getNextSortDirection(columnIndex);
                }

                // Clear previous sort indicators when switching columns
                if (this.currentSort.column !== null && this.currentSort.column !== columnIndex) {
                    this.clearPreviousSortIndicators();
                }

                // Show loading state
                this.table.classList.add('sorting');

                // Perform sort asynchronously to avoid blocking UI
                setTimeout(() => {
                    try {
                        this.performSort(columnIndex, direction);
                        this.updateCurrentSort(columnIndex, direction);
                        this.updateVisualIndicators(columnIndex, direction);
                        this.accessibilityManager.updateAriaAttributes(columnIndex, direction);
                        this.accessibilityManager.announceSort(column.name, direction);
                        
                        if (this.options.persistState) {
                            this.stateManager.save(columnIndex, direction);
                        }
                        
                        // Dispatch event for enhanced table integration
                        const event = new CustomEvent('tableSortedClientSide', {
                            detail: {
                                column: column.name,
                                columnIndex: columnIndex,
                                direction: direction,
                                isGlobal: false
                            }
                        });
                        this.table.dispatchEvent(event);
                        
                    } catch (error) {
                        console.error('Sort operation failed:', error);
                        this.showErrorMessage('Sort operation failed');
                    } finally {
                        this.table.classList.remove('sorting');
                    }
                }, 10);

            } catch (error) {
                console.error('Sort initiation failed:', error);
                this.showErrorMessage('Unable to sort column');
            }
        }

        getNextSortDirection(columnIndex) {
            if (this.currentSort.column === columnIndex) {
                // Cycle through: asc → desc → asc (Requirements 1.1, 1.2, 1.3)
                return this.currentSort.direction === 'asc' ? 'desc' : 'asc';
            }
            // New column always starts with ascending (Requirement 1.4)
            return 'asc';
        }

        performSort(columnIndex, direction) {
            const rows = Array.from(this.tbody.querySelectorAll('tr'));
            const column = this.columnMetadata[columnIndex];
            const handler = this.getDataTypeHandler(column.dataType);

            // Performance optimization: skip sorting if only one row
            if (rows.length <= 1) return;

            // Extract row data with original row reference
            const rowData = rows.map(row => ({
                row: row,
                value: row.cells[columnIndex] ? row.cells[columnIndex].textContent.trim() : ''
            }));

            // Use efficient sorting algorithm (JavaScript's native sort is Timsort - very efficient)
            rowData.sort((a, b) => {
                const result = handler.compare(a.value, b.value);
                return direction === 'desc' ? -result : result;
            });

            // Efficient DOM manipulation using DocumentFragment
            const fragment = document.createDocumentFragment();
            rowData.forEach(item => {
                fragment.appendChild(item.row);
            });
            
            // Single DOM operation for better performance
            this.tbody.appendChild(fragment);
        }

        getDataTypeHandler(dataType) {
            return this.dataTypeHandlers.find(handler => handler.name === dataType) ||
                   this.dataTypeHandlers.find(handler => handler.name === 'text');
        }

        updateCurrentSort(columnIndex, direction) {
            this.currentSort = { column: columnIndex, direction: direction };
        }

        updateVisualIndicators(columnIndex, direction) {
            if (!this.options.showIndicators) return;

            this.headers.forEach((header, index) => {
                const indicator = header.querySelector('.sort-indicator');
                if (!indicator) return;

                if (index === columnIndex) {
                    indicator.className = `sort-indicator ${direction} active`;
                    header.classList.add('sorted');
                } else {
                    indicator.className = 'sort-indicator neutral';
                    header.classList.remove('sorted');
                }
            });
        }

        clearPreviousSortIndicators() {
            if (!this.options.showIndicators) return;

            this.headers.forEach(header => {
                const indicator = header.querySelector('.sort-indicator');
                if (indicator) {
                    indicator.className = 'sort-indicator neutral';
                }
                header.classList.remove('sorted');
            });
        }

        restoreState() {
            const state = this.stateManager.load();
            if (state && state.column !== null && state.direction) {
                // Restore sort after a brief delay to ensure DOM is ready
                setTimeout(() => {
                    this.sortByColumn(state.column, state.direction);
                }, 100);
            }
        }

        clearSort() {
            this.currentSort = { column: null, direction: null };
            this.updateVisualIndicators(-1, null);
            this.accessibilityManager.updateAriaAttributes(-1, null);
            
            if (this.options.persistState) {
                this.stateManager.clear();
            }
        }

        showErrorMessage(message) {
            let errorDiv = this.table.parentNode.querySelector('.sort-error-message');
            if (!errorDiv) {
                errorDiv = document.createElement('div');
                errorDiv.className = 'sort-error-message';
                this.table.parentNode.appendChild(errorDiv);
            }
            
            errorDiv.textContent = message;
            errorDiv.classList.add('show');
            
            setTimeout(() => {
                errorDiv.classList.remove('show');
            }, 5000);
        }

        // Public API methods
        getSortState() {
            return { ...this.currentSort };
        }

        setSortState(columnIndex, direction) {
            this.sortByColumn(columnIndex, direction);
        }
        
        /**
         * Sort by column name (for enhanced table integration)
         * @param {string} columnName - Column name
         * @param {string} direction - Sort direction
         */
        sortByColumnName(columnName, direction) {
            const columnIndex = this.getColumnIndexByName(columnName);
            if (columnIndex !== -1) {
                this.sortByColumn(columnIndex, direction);
            } else {
                console.warn('Column not found for sorting:', columnName);
            }
        }
        
        /**
         * Get column index by name
         * @param {string} columnName - Column name
         * @returns {number} Column index (-1 if not found)
         */
        getColumnIndexByName(columnName) {
            return this.columnMetadata.findIndex(col => 
                col.name.toLowerCase() === columnName.toLowerCase() ||
                col.element.getAttribute('data-column') === columnName
            );
        }

        refreshSort() {
            // Re-apply current sort after filter changes
            if (this.currentSort.column !== null && this.currentSort.direction) {
                this.sortByColumn(this.currentSort.column, this.currentSort.direction);
            }
        }

        onFilterChange() {
            // Called when filters are applied/removed to maintain sort order
            setTimeout(() => {
                this.refreshSort();
            }, 100); // Small delay to ensure DOM is updated
        }

        destroy() {
            // Clean up event listeners and DOM modifications
            this.headers.forEach(header => {
                header.classList.remove('sortable-header', 'sorted');
                const indicator = header.querySelector('.sort-indicator');
                if (indicator) {
                    indicator.remove();
                }
            });
            
            this.table.classList.remove('table-sortable', 'sorting');
        }
    }

    // Auto-initialization
    document.addEventListener('DOMContentLoaded', function() {
        console.log('TableSorter: Auto-initializing sortable tables...');
        
        const sortableTables = document.querySelectorAll('.table-sortable');
        const initializedTables = [];
        
        sortableTables.forEach(table => {
            try {
                const sorter = new TableSorter(table, {
                    persistState: true,
                    showIndicators: true,
                    multiSort: false
                });
                
                // Store reference for potential later access
                table.tableSorter = sorter;
                initializedTables.push(table.id || 'unnamed');
            } catch (error) {
                console.error('Failed to initialize TableSorter for table:', table, error);
            }
        });
        
        console.log(`TableSorter: Initialized ${initializedTables.length} tables:`, initializedTables);
        
        // Listen for filter changes to maintain sort order
        document.addEventListener('filterApplied', function(event) {
            const table = event.target.closest('.table-sortable');
            if (table && table.tableSorter) {
                table.tableSorter.onFilterChange();
            }
        });
        
        // Listen for table content updates (e.g., after AJAX requests)
        document.addEventListener('tableUpdated', function(event) {
            const table = event.target.closest('.table-sortable');
            if (table && table.tableSorter) {
                table.tableSorter.onFilterChange();
            }
        });
    });

    // Export to global scope
    window.TableSorter = TableSorter;
    window.TableSortingHandlers = {
        TextDataTypeHandler,
        NumericDataTypeHandler,
        DateTimeDataTypeHandler,
        StatusDataTypeHandler
    };

})();