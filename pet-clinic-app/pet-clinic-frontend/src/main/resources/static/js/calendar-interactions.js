/**
 * Calendar Interaction Features
 * Enhanced interaction features for the visit calendar
 * Requirements: 8.2, 8.3, 8.5
 */

(function() {
    'use strict';

    /**
     * Calendar Interaction Manager
     * Handles day cell clicks, veterinarian filtering, and popup displays
     */
    class CalendarInteractionManager {
        constructor() {
            this.currentVeterinarianFilter = null;
            this.dayDetailsCache = new Map();
            this.visitDetailsCache = new Map();
            
            this.init();
        }

        /**
         * Initialize calendar interactions
         */
        init() {
            this.setupDayClickHandlers();
            this.setupVeterinarianFiltering();
            this.setupVisitPopups();
            this.setupModalEnhancements();
            this.setupStateManagement();
            
            console.log('Calendar interactions initialized');
        }

        /**
         * Setup day cell click handlers with popup display
         */
        setupDayClickHandlers() {
            const calendarDays = document.querySelectorAll('.calendar-day');
            
            calendarDays.forEach(day => {
                day.addEventListener('click', (e) => {
                    // Don't trigger if clicking on a visit item
                    if (e.target.classList.contains('visit-item')) {
                        return;
                    }
                    
                    const dayNumber = day.dataset.dayNumber;
                    const dateString = day.dataset.dateString;
                    
                    if (dayNumber && dateString) {
                        this.showDayDetailsPopup(dayNumber, dateString, day);
                    }
                });
                
                // Add keyboard support
                day.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        day.click();
                    }
                });
            });
        }

        /**
         * Show day details popup with enhanced functionality
         */
        async showDayDetailsPopup(dayNumber, dateString, dayElement) {
            const modal = this.getOrCreateDayModal();
            const modalInstance = new bootstrap.Modal(modal);
            
            // Update modal title
            const selectedDateSpan = modal.querySelector('#selectedDate');
            if (selectedDateSpan) {
                selectedDateSpan.textContent = this.formatDateString(dateString);
            }
            
            // Show loading state
            const dayVisitsList = modal.querySelector('#dayVisitsList');
            if (dayVisitsList) {
                dayVisitsList.innerHTML = this.getLoadingHTML();
            }
            
            // Update schedule link
            const scheduleLink = modal.querySelector('#scheduleVisitForDay');
            if (scheduleLink) {
                scheduleLink.href = `/visits/new?date=${dateString}`;
            }
            
            modalInstance.show();
            
            try {
                // Load day details
                const dayDetails = await this.loadDayDetails(dateString);
                this.displayDayDetails(dayDetails, dayVisitsList);
                
            } catch (error) {
                console.error('Error loading day details:', error);
                this.displayDayError(dayVisitsList, dayNumber, dateString);
            }
        }

        /**
         * Setup veterinarian filtering with state management
         */
        setupVeterinarianFiltering() {
            const veterinarianFilter = document.getElementById('veterinarianFilter');
            if (!veterinarianFilter) return;

            veterinarianFilter.addEventListener('change', (e) => {
                const selectedVetId = e.target.value;
                this.applyVeterinarianFilter(selectedVetId);
            });

            // Initialize current filter state
            this.currentVeterinarianFilter = veterinarianFilter.value || null;
        }

        /**
         * Apply veterinarian filter with enhanced UX
         */
        async applyVeterinarianFilter(veterinarianId) {
            const filterCard = document.querySelector('.filter-card');
            const calendarGrid = document.querySelector('.calendar-grid');
            
            try {
                // Show loading state
                this.showFilterLoadingState(filterCard, calendarGrid);
                
                // Build URL parameters
                const params = new URLSearchParams(window.location.search);
                if (veterinarianId) {
                    params.set('veterinarianId', veterinarianId);
                } else {
                    params.delete('veterinarianId');
                }
                
                // Maintain current month/year
                const currentYear = this.getCurrentYear();
                const currentMonth = this.getCurrentMonth();
                if (currentYear) params.set('year', currentYear);
                if (currentMonth !== null) params.set('month', currentMonth);
                
                // Navigate with smooth transition
                await this.smoothNavigate(`/visits/calendar?${params.toString()}`);
                
            } catch (error) {
                console.error('Error applying veterinarian filter:', error);
                this.showFilterError(filterCard);
            }
        }

        /**
         * Setup visit popup interactions
         */
        setupVisitPopups() {
            // Delegate event handling for dynamically loaded visit items
            document.addEventListener('click', (e) => {
                if (e.target.classList.contains('visit-item')) {
                    e.stopPropagation();
                    const visitId = e.target.dataset.visitId;
                    if (visitId) {
                        this.showVisitQuickView(visitId);
                    }
                }
            });
        }

        /**
         * Show visit quick view popup
         */
        async showVisitQuickView(visitId) {
            const modal = this.getOrCreateVisitModal();
            const modalInstance = new bootstrap.Modal(modal);
            
            // Show loading state
            const visitContent = modal.querySelector('#visitQuickContent');
            if (visitContent) {
                visitContent.innerHTML = this.getLoadingHTML();
            }
            
            // Update action links
            const viewFullLink = modal.querySelector('#viewFullVisit');
            const editLink = modal.querySelector('#editVisit');
            if (viewFullLink) viewFullLink.href = `/visits/${visitId}`;
            if (editLink) editLink.href = `/visits/${visitId}/edit`;
            
            modalInstance.show();
            
            try {
                // Load visit details
                const visitDetails = await this.loadVisitDetails(visitId);
                this.displayVisitDetails(visitDetails, visitContent);
                
            } catch (error) {
                console.error('Error loading visit details:', error);
                this.displayVisitError(visitContent, visitId);
            }
        }

        /**
         * Setup modal enhancements
         */
        setupModalEnhancements() {
            // Add modal backdrop click handling
            document.addEventListener('click', (e) => {
                if (e.target.classList.contains('modal')) {
                    const modalInstance = bootstrap.Modal.getInstance(e.target);
                    if (modalInstance) {
                        modalInstance.hide();
                    }
                }
            });

            // Add escape key handling
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    const openModals = document.querySelectorAll('.modal.show');
                    openModals.forEach(modal => {
                        const modalInstance = bootstrap.Modal.getInstance(modal);
                        if (modalInstance) {
                            modalInstance.hide();
                        }
                    });
                }
            });
        }

        /**
         * Setup state management for navigation
         */
        setupStateManagement() {
            // Handle browser back/forward buttons
            window.addEventListener('popstate', (e) => {
                if (e.state && e.state.calendarState) {
                    this.restoreCalendarState(e.state.calendarState);
                }
            });

            // Save initial state
            const initialState = {
                calendarState: {
                    year: this.getCurrentYear(),
                    month: this.getCurrentMonth(),
                    veterinarianId: this.currentVeterinarianFilter
                }
            };
            history.replaceState(initialState, '', window.location.href);
        }

        /**
         * Load day details from server
         */
        async loadDayDetails(dateString) {
            // Check cache first
            if (this.dayDetailsCache.has(dateString)) {
                return this.dayDetailsCache.get(dateString);
            }

            const response = await fetch(`/visits/calendar/day?date=${dateString}`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const html = await response.text();
            const dayDetails = { html, dateString };
            
            // Cache the result
            this.dayDetailsCache.set(dateString, dayDetails);
            
            return dayDetails;
        }

        /**
         * Load visit details from server
         */
        async loadVisitDetails(visitId) {
            // Check cache first
            if (this.visitDetailsCache.has(visitId)) {
                return this.visitDetailsCache.get(visitId);
            }

            const response = await fetch(`/visits/${visitId}/quick`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const html = await response.text();
            const visitDetails = { html, visitId };
            
            // Cache the result
            this.visitDetailsCache.set(visitId, visitDetails);
            
            return visitDetails;
        }

        /**
         * Display day details in modal
         */
        displayDayDetails(dayDetails, container) {
            if (!container) return;

            if (dayDetails.html && dayDetails.html.trim()) {
                container.innerHTML = dayDetails.html;
            } else {
                container.innerHTML = this.getEmptyDayHTML();
            }

            // Add interaction handlers to loaded content
            this.setupLoadedContentHandlers(container);
        }

        /**
         * Display visit details in modal
         */
        displayVisitDetails(visitDetails, container) {
            if (!container) return;

            if (visitDetails.html && visitDetails.html.trim()) {
                container.innerHTML = visitDetails.html;
            } else {
                container.innerHTML = this.getEmptyVisitHTML();
            }

            // Add interaction handlers to loaded content
            this.setupLoadedContentHandlers(container);
        }

        /**
         * Display day loading error
         */
        displayDayError(container, dayNumber, dateString) {
            if (!container) return;

            container.innerHTML = `
                <div class="text-center py-4">
                    <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                    <h5 class="text-warning">Unable to load visits</h5>
                    <p class="text-muted">Please try again or refresh the page.</p>
                    <div class="mt-3">
                        <button class="btn btn-outline-primary me-2" onclick="this.closest('.modal').querySelector('.btn-close').click(); document.querySelector('[data-date-string=&quot;${dateString}&quot;]').click();">
                            <i class="fas fa-redo"></i> Retry
                        </button>
                        <a href="/visits/new?date=${dateString}" class="btn btn-primary">
                            <i class="fas fa-plus"></i> Schedule Visit
                        </a>
                    </div>
                </div>
            `;
        }

        /**
         * Display visit loading error
         */
        displayVisitError(container, visitId) {
            if (!container) return;

            container.innerHTML = `
                <div class="text-center py-4">
                    <i class="fas fa-exclamation-triangle fa-3x text-warning mb-3"></i>
                    <h5 class="text-warning">Unable to load visit details</h5>
                    <p class="text-muted">Please try again or view the full visit page.</p>
                    <div class="mt-3">
                        <button class="btn btn-outline-primary me-2" onclick="window.calendarInteractions.showVisitQuickView('${visitId}')">
                            <i class="fas fa-redo"></i> Retry
                        </button>
                        <a href="/visits/${visitId}" class="btn btn-primary">
                            <i class="fas fa-external-link-alt"></i> View Full Details
                        </a>
                    </div>
                </div>
            `;
        }

        /**
         * Get or create day details modal
         */
        getOrCreateDayModal() {
            let modal = document.getElementById('dayDetailsModal');
            if (!modal) {
                modal = this.createDayModal();
                document.body.appendChild(modal);
            }
            return modal;
        }

        /**
         * Get or create visit quick view modal
         */
        getOrCreateVisitModal() {
            let modal = document.getElementById('visitQuickModal');
            if (!modal) {
                modal = this.createVisitModal();
                document.body.appendChild(modal);
            }
            return modal;
        }

        /**
         * Create day details modal
         */
        createDayModal() {
            const modal = document.createElement('div');
            modal.className = 'modal fade';
            modal.id = 'dayDetailsModal';
            modal.setAttribute('tabindex', '-1');
            modal.innerHTML = `
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-calendar-day"></i> 
                                Visits for <span id="selectedDate">Date</span>
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <div id="dayVisitsList">
                                ${this.getLoadingHTML()}
                            </div>
                            <div class="text-center mt-4">
                                <a href="#" id="scheduleVisitForDay" class="btn btn-primary">
                                    <i class="fas fa-plus"></i> Schedule Visit for This Day
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            return modal;
        }

        /**
         * Create visit quick view modal
         */
        createVisitModal() {
            const modal = document.createElement('div');
            modal.className = 'modal fade';
            modal.id = 'visitQuickModal';
            modal.setAttribute('tabindex', '-1');
            modal.innerHTML = `
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-eye"></i> Visit Quick View
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body" id="visitQuickContent">
                            ${this.getLoadingHTML()}
                        </div>
                        <div class="modal-footer">
                            <a href="#" id="viewFullVisit" class="btn btn-primary">
                                <i class="fas fa-eye"></i> View Full Details
                            </a>
                            <a href="#" id="editVisit" class="btn btn-outline-secondary">
                                <i class="fas fa-edit"></i> Edit Visit
                            </a>
                        </div>
                    </div>
                </div>
            `;
            return modal;
        }

        /**
         * Get loading HTML
         */
        getLoadingHTML() {
            return `
                <div class="text-center py-4">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <p class="mt-3 text-muted">Loading...</p>
                </div>
            `;
        }

        /**
         * Get empty day HTML
         */
        getEmptyDayHTML() {
            return `
                <div class="text-center py-4">
                    <i class="fas fa-calendar-plus fa-3x text-muted mb-3"></i>
                    <h5 class="text-muted">No visits scheduled</h5>
                    <p class="text-muted">This day is available for new appointments.</p>
                </div>
            `;
        }

        /**
         * Get empty visit HTML
         */
        getEmptyVisitHTML() {
            return `
                <div class="text-center py-4">
                    <i class="fas fa-exclamation-circle fa-3x text-muted mb-3"></i>
                    <h5 class="text-muted">Visit details not available</h5>
                    <p class="text-muted">Please try viewing the full visit page.</p>
                </div>
            `;
        }

        /**
         * Setup handlers for loaded content
         */
        setupLoadedContentHandlers(container) {
            // Add click handlers for any visit items in loaded content
            const visitItems = container.querySelectorAll('.visit-item');
            visitItems.forEach(item => {
                item.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const visitId = item.dataset.visitId;
                    if (visitId) {
                        this.showVisitQuickView(visitId);
                    }
                });
            });

            // Add any other necessary handlers for loaded content
            const actionButtons = container.querySelectorAll('[data-action]');
            actionButtons.forEach(button => {
                button.addEventListener('click', (e) => {
                    const action = button.dataset.action;
                    this.handleActionButton(action, button, e);
                });
            });
        }

        /**
         * Handle action button clicks
         */
        handleActionButton(action, button, event) {
            switch (action) {
                case 'refresh':
                    event.preventDefault();
                    window.location.reload();
                    break;
                case 'close-modal':
                    event.preventDefault();
                    const modal = button.closest('.modal');
                    if (modal) {
                        const modalInstance = bootstrap.Modal.getInstance(modal);
                        if (modalInstance) modalInstance.hide();
                    }
                    break;
                // Add more action handlers as needed
            }
        }

        /**
         * Show filter loading state
         */
        showFilterLoadingState(filterCard, calendarGrid) {
            if (filterCard) {
                filterCard.style.opacity = '0.7';
                const select = filterCard.querySelector('select');
                if (select) select.disabled = true;
            }
            
            if (calendarGrid) {
                calendarGrid.style.opacity = '0.5';
                calendarGrid.style.pointerEvents = 'none';
            }
        }

        /**
         * Show filter error
         */
        showFilterError(filterCard) {
            if (filterCard) {
                filterCard.style.opacity = '1';
                const select = filterCard.querySelector('select');
                if (select) select.disabled = false;
                
                // Show error message
                let errorMsg = filterCard.querySelector('.filter-error');
                if (!errorMsg) {
                    errorMsg = document.createElement('div');
                    errorMsg.className = 'filter-error alert alert-warning mt-2';
                    filterCard.appendChild(errorMsg);
                }
                errorMsg.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Filter failed to apply. Please try again.';
                
                // Auto-hide error after 5 seconds
                setTimeout(() => {
                    if (errorMsg.parentNode) {
                        errorMsg.parentNode.removeChild(errorMsg);
                    }
                }, 5000);
            }
        }

        /**
         * Smooth navigation with transition
         */
        async smoothNavigate(url) {
            return new Promise((resolve) => {
                const calendarGrid = document.querySelector('.calendar-grid');
                if (calendarGrid) {
                    calendarGrid.style.transition = 'opacity 0.3s ease';
                    calendarGrid.style.opacity = '0.5';
                }
                
                setTimeout(() => {
                    window.location.href = url;
                    resolve();
                }, 300);
            });
        }

        /**
         * Get current year from page or URL
         */
        getCurrentYear() {
            const params = new URLSearchParams(window.location.search);
            return params.get('year') || new Date().getFullYear();
        }

        /**
         * Get current month from page or URL
         */
        getCurrentMonth() {
            const params = new URLSearchParams(window.location.search);
            const month = params.get('month');
            return month !== null ? parseInt(month) : new Date().getMonth();
        }

        /**
         * Format date string for display
         */
        formatDateString(dateString) {
            try {
                const date = new Date(dateString);
                return date.toLocaleDateString('en-US', { 
                    weekday: 'long', 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric' 
                });
            } catch (error) {
                return dateString;
            }
        }

        /**
         * Restore calendar state from history
         */
        restoreCalendarState(state) {
            if (state.veterinarianId !== this.currentVeterinarianFilter) {
                const veterinarianFilter = document.getElementById('veterinarianFilter');
                if (veterinarianFilter) {
                    veterinarianFilter.value = state.veterinarianId || '';
                    this.currentVeterinarianFilter = state.veterinarianId;
                }
            }
        }

        /**
         * Clear caches
         */
        clearCaches() {
            this.dayDetailsCache.clear();
            this.visitDetailsCache.clear();
        }

        /**
         * Destroy and cleanup
         */
        destroy() {
            this.clearCaches();
            
            // Remove event listeners if needed
            const modals = document.querySelectorAll('#dayDetailsModal, #visitQuickModal');
            modals.forEach(modal => {
                if (modal.parentNode) {
                    modal.parentNode.removeChild(modal);
                }
            });
        }
    }

    // Make available globally for error handlers
    window.calendarInteractions = null;

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            window.calendarInteractions = new CalendarInteractionManager();
        });
    } else {
        window.calendarInteractions = new CalendarInteractionManager();
    }

})();

        /**
         * Create visit quick view modal
         */
        createVisitModal() {
            const modal = document.createElement('div');
            modal.className = 'modal fade';
            modal.id = 'visitQuickModal';
            modal.setAttribute('tabindex', '-1');
            modal.innerHTML = `
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-eye"></i> Visit Quick View
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body" id="visitQuickContent">
                            ${this.getLoadingHTML()}
                        </div>
                        <div class="modal-footer">
                            <a href="#" id="viewFullVisit" class="btn btn-primary">
                                <i class="fas fa-eye"></i> View Full Details
                            </a>
                            <a href="#" id="editVisit" class="btn btn-outline-secondary">
                                <i class="fas fa-edit"></i> Edit Visit
                            </a>
                        </div>
                    </div>
                </div>
            `;
            return modal;
        }

        /**
         * Get loading HTML
         */
        getLoadingHTML() {
            return `
                <div class="text-center py-4">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <p class="mt-3 text-muted">Loading...</p>
                </div>
            `;
        }

        /**
         * Get empty day HTML
         */
        getEmptyDayHTML() {
            return `
                <div class="text-center py-4">
                    <i class="fas fa-calendar-plus fa-3x text-muted mb-3"></i>
                    <h5 class="text-muted">No visits scheduled</h5>
                    <p class="text-muted">This day is available for new appointments.</p>
                </div>
            `;
        }

        /**
         * Get empty visit HTML
         */
        getEmptyVisitHTML() {
            return `
                <div class="text-center py-4">
                    <i class="fas fa-exclamation-circle fa-3x text-muted mb-3"></i>
                    <h5 class="text-muted">Visit details not available</h5>
                    <p class="text-muted">Please try viewing the full visit page.</p>
                </div>
            `;
        }

        /**
         * Setup handlers for loaded content
         */
        setupLoadedContentHandlers(container) {
            // Add click handlers for any visit items in loaded content
            const visitItems = container.querySelectorAll('.visit-item');
            visitItems.forEach(item => {
                item.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const visitId = item.dataset.visitId;
                    if (visitId) {
                        this.showVisitQuickView(visitId);
                    }
                });
            });

            // Add any other necessary handlers for loaded content
            const actionButtons = container.querySelectorAll('[data-action]');
            actionButtons.forEach(button => {
                button.addEventListener('click', (e) => {
                    const action = button.dataset.action;
                    this.handleActionButton(action, button, e);
                });
            });
        }

        /**
         * Handle action button clicks
         */
        handleActionButton(action, button, event) {
            switch (action) {
                case 'refresh':
                    event.preventDefault();
                    window.location.reload();
                    break;
                case 'close-modal':
                    event.preventDefault();
                    const modal = button.closest('.modal');
                    if (modal) {
                        const modalInstance = bootstrap.Modal.getInstance(modal);
                        if (modalInstance) modalInstance.hide();
                    }
                    break;
                // Add more action handlers as needed
            }
        }

        /**
         * Show filter loading state
         */
        showFilterLoadingState(filterCard, calendarGrid) {
            if (filterCard) {
                filterCard.style.opacity = '0.7';
                const select = filterCard.querySelector('select');
                if (select) select.disabled = true;
            }
            
            if (calendarGrid) {
                calendarGrid.style.opacity = '0.5';
                calendarGrid.style.pointerEvents = 'none';
            }
        }

        /**
         * Show filter error
         */
        showFilterError(filterCard) {
            if (filterCard) {
                filterCard.style.opacity = '1';
                const select = filterCard.querySelector('select');
                if (select) select.disabled = false;
                
                // Show error message
                let errorMsg = filterCard.querySelector('.filter-error');
                if (!errorMsg) {
                    errorMsg = document.createElement('div');
                    errorMsg.className = 'filter-error alert alert-warning mt-2';
                    filterCard.appendChild(errorMsg);
                }
                errorMsg.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Filter failed to apply. Please try again.';
                
                // Auto-hide error after 5 seconds
                setTimeout(() => {
                    if (errorMsg.parentNode) {
                        errorMsg.parentNode.removeChild(errorMsg);
                    }
                }, 5000);
            }
        }

        /**
         * Smooth navigation with transition
         */
        async smoothNavigate(url) {
            return new Promise((resolve) => {
                const calendarGrid = document.querySelector('.calendar-grid');
                if (calendarGrid) {
                    calendarGrid.style.transition = 'opacity 0.3s ease';
                    calendarGrid.style.opacity = '0.5';
                }
                
                setTimeout(() => {
                    window.location.href = url;
                    resolve();
                }, 300);
            });
        }

        /**
         * Get current year from page or URL
         */
        getCurrentYear() {
            const params = new URLSearchParams(window.location.search);
            return params.get('year') || new Date().getFullYear();
        }

        /**
         * Get current month from page or URL
         */
        getCurrentMonth() {
            const params = new URLSearchParams(window.location.search);
            const month = params.get('month');
            return month !== null ? parseInt(month) : new Date().getMonth();
        }

        /**
         * Format date string for display
         */
        formatDateString(dateString) {
            try {
                const date = new Date(dateString);
                return date.toLocaleDateString('en-US', { 
                    weekday: 'long', 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric' 
                });
            } catch (error) {
                return dateString;
            }
        }

        /**
         * Restore calendar state from history
         */
        restoreCalendarState(state) {
            if (state.veterinarianId !== this.currentVeterinarianFilter) {
                const veterinarianFilter = document.getElementById('veterinarianFilter');
                if (veterinarianFilter) {
                    veterinarianFilter.value = state.veterinarianId || '';
                    this.currentVeterinarianFilter = state.veterinarianId;
                }
            }
        }

        /**
         * Clear caches
         */
        clearCaches() {
            this.dayDetailsCache.clear();
            this.visitDetailsCache.clear();
        }

        /**
         * Destroy and cleanup
         */
        destroy() {
            this.clearCaches();
            
            // Remove event listeners if needed
            const modals = document.querySelectorAll('#dayDetailsModal, #visitQuickModal');
            modals.forEach(modal => {
                if (modal.parentNode) {
                    modal.parentNode.removeChild(modal);
                }
            });
        }
    }

    // Make available globally for error handlers
    window.calendarInteractions = null;

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            window.calendarInteractions = new CalendarInteractionManager();
        });
    } else {
        window.calendarInteractions = new CalendarInteractionManager();
    }

})();