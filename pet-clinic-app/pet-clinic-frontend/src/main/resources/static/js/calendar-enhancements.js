/**
 * Calendar Enhancements
 * Additional functionality for the visit calendar view
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */

(function() {
    'use strict';

    /**
     * Calendar Enhancement Manager
     * Provides additional functionality for the calendar view
     */
    class CalendarEnhancementManager {
        constructor() {
            this.currentDate = new Date();
            this.isLoading = false;
            this.touchStartX = 0;
            this.touchStartY = 0;
            
            this.init();
        }

        /**
         * Initialize calendar enhancements
         */
        init() {
            this.setupGridLayoutFix();
            this.setupTouchNavigation();
            this.setupKeyboardShortcuts();
            this.setupVisitCountIndicators();
            this.setupLoadingStates();
            this.setupAccessibility();
            this.setupPerformanceOptimizations();
            
            console.log('Calendar enhancements initialized');
        }

        /**
         * Ensure proper grid layout on all browsers
         */
        setupGridLayoutFix() {
            const calendarGrid = document.querySelector('.calendar-grid');
            if (!calendarGrid) return;

            // Force grid layout for older browsers
            const observer = new MutationObserver(() => {
                this.enforceGridLayout(calendarGrid);
            });

            observer.observe(calendarGrid, { 
                childList: true, 
                subtree: true 
            });

            // Initial layout fix
            this.enforceGridLayout(calendarGrid);

            // Handle window resize
            let resizeTimeout;
            window.addEventListener('resize', () => {
                clearTimeout(resizeTimeout);
                resizeTimeout = setTimeout(() => {
                    this.enforceGridLayout(calendarGrid);
                }, 100);
            });
        }

        /**
         * Enforce proper grid layout
         * @param {HTMLElement} grid - Calendar grid element
         */
        enforceGridLayout(grid) {
            if (!grid) return;

            const computedStyle = window.getComputedStyle(grid);
            
            // Ensure grid display
            if (computedStyle.display !== 'grid') {
                grid.style.display = 'grid';
                grid.style.gridTemplateColumns = 'repeat(7, 1fr)';
                grid.style.gap = '1px';
                grid.style.width = '100%';
            }

            // Ensure proper row sizing
            const isMobile = window.innerWidth < 768;
            const minHeight = isMobile ? '100px' : '140px';
            grid.style.gridAutoRows = `minmax(${minHeight}, auto)`;

            // Count children and ensure proper layout
            const children = grid.children;
            const expectedChildren = 49; // 7 headers + 42 days

            if (children.length !== expectedChildren) {
                console.warn(`Calendar grid has ${children.length} children, expected ${expectedChildren}`);
            }

            // Ensure day cells have proper classes
            Array.from(children).forEach((child, index) => {
                if (index >= 7) { // Skip headers
                    if (!child.classList.contains('calendar-day')) {
                        child.classList.add('calendar-day');
                    }
                }
            });
        }

        /**
         * Setup touch navigation for mobile devices
         */
        setupTouchNavigation() {
            const calendarGrid = document.querySelector('.calendar-grid');
            if (!calendarGrid) return;

            calendarGrid.addEventListener('touchstart', (e) => {
                this.touchStartX = e.touches[0].clientX;
                this.touchStartY = e.touches[0].clientY;
            }, { passive: true });

            calendarGrid.addEventListener('touchend', (e) => {
                if (!this.touchStartX || !this.touchStartY) return;

                const touchEndX = e.changedTouches[0].clientX;
                const touchEndY = e.changedTouches[0].clientY;

                const deltaX = this.touchStartX - touchEndX;
                const deltaY = this.touchStartY - touchEndY;

                // Only handle horizontal swipes
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 50) {
                    if (deltaX > 0) {
                        // Swipe left - next month
                        if (typeof navigateMonth === 'function') {
                            navigateMonth(1);
                        }
                    } else {
                        // Swipe right - previous month
                        if (typeof navigateMonth === 'function') {
                            navigateMonth(-1);
                        }
                    }
                }

                this.touchStartX = 0;
                this.touchStartY = 0;
            }, { passive: true });
        }

        /**
         * Setup enhanced keyboard shortcuts
         */
        setupKeyboardShortcuts() {
            document.addEventListener('keydown', (e) => {
                // Skip if user is typing in an input
                if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT' || e.target.tagName === 'TEXTAREA') {
                    return;
                }

                switch(e.key) {
                    case 'h':
                    case 'H':
                        if (!e.ctrlKey && !e.metaKey) {
                            e.preventDefault();
                            this.showKeyboardHelp();
                        }
                        break;
                    case 'n':
                    case 'N':
                        if (!e.ctrlKey && !e.metaKey) {
                            e.preventDefault();
                            window.location.href = '/visits/new';
                        }
                        break;
                    case 'l':
                    case 'L':
                        if (!e.ctrlKey && !e.metaKey) {
                            e.preventDefault();
                            window.location.href = '/visits';
                        }
                        break;
                    case 'Escape':
                        // Close any open modals
                        const openModals = document.querySelectorAll('.modal.show');
                        openModals.forEach(modal => {
                            const bsModal = bootstrap.Modal.getInstance(modal);
                            if (bsModal) bsModal.hide();
                        });
                        break;
                }
            });
        }

        /**
         * Show keyboard shortcuts help
         */
        showKeyboardHelp() {
            const helpModal = document.createElement('div');
            helpModal.className = 'modal fade';
            helpModal.innerHTML = `
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-keyboard"></i> Keyboard Shortcuts
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <h6><i class="fas fa-arrows-alt-h"></i> Navigation</h6>
                                    <ul class="list-unstyled">
                                        <li><kbd>←</kbd> Previous month</li>
                                        <li><kbd>→</kbd> Next month</li>
                                        <li><kbd>T</kbd> Go to today</li>
                                    </ul>
                                </div>
                                <div class="col-md-6">
                                    <h6><i class="fas fa-bolt"></i> Actions</h6>
                                    <ul class="list-unstyled">
                                        <li><kbd>N</kbd> New visit</li>
                                        <li><kbd>L</kbd> List view</li>
                                        <li><kbd>H</kbd> Show this help</li>
                                        <li><kbd>Esc</kbd> Close modals</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            document.body.appendChild(helpModal);
            const modal = new bootstrap.Modal(helpModal);
            modal.show();

            // Remove modal from DOM when hidden
            helpModal.addEventListener('hidden.bs.modal', () => {
                document.body.removeChild(helpModal);
            });
        }

        /**
         * Setup visit count indicators
         */
        setupVisitCountIndicators() {
            const calendarDays = document.querySelectorAll('.calendar-day');
            
            calendarDays.forEach(day => {
                const visits = day.querySelectorAll('.visit-item');
                const visitCount = visits.length;

                if (visitCount > 0) {
                    // Add visit count badge
                    let countBadge = day.querySelector('.visit-count-badge');
                    if (!countBadge) {
                        countBadge = document.createElement('div');
                        countBadge.className = 'visit-count-badge';
                        day.appendChild(countBadge);
                    }

                    countBadge.textContent = visitCount;
                    countBadge.title = `${visitCount} visit${visitCount !== 1 ? 's' : ''} scheduled`;

                    // Add different styles based on visit count
                    if (visitCount >= 5) {
                        countBadge.classList.add('high-count');
                    } else if (visitCount >= 3) {
                        countBadge.classList.add('medium-count');
                    } else {
                        countBadge.classList.add('low-count');
                    }

                    // Check for emergency visits
                    const emergencyVisits = day.querySelectorAll('.visit-item.emergency');
                    if (emergencyVisits.length > 0) {
                        day.classList.add('has-emergency');
                        countBadge.classList.add('has-emergency');
                    }
                }
            });
        }

        /**
         * Setup loading states for better UX
         */
        setupLoadingStates() {
            // Intercept navigation functions to add loading states
            const originalNavigateMonth = window.navigateMonth;
            const originalGoToToday = window.goToToday;
            const originalFilterByVeterinarian = window.filterByVeterinarian;

            if (originalNavigateMonth) {
                window.navigateMonth = (direction) => {
                    if (this.isLoading) return;
                    this.showLoadingState();
                    originalNavigateMonth(direction);
                };
            }

            if (originalGoToToday) {
                window.goToToday = () => {
                    if (this.isLoading) return;
                    this.showLoadingState();
                    originalGoToToday();
                };
            }

            if (originalFilterByVeterinarian) {
                window.filterByVeterinarian = () => {
                    if (this.isLoading) return;
                    this.showLoadingState();
                    originalFilterByVeterinarian();
                };
            }
        }

        /**
         * Show loading state
         */
        showLoadingState() {
            this.isLoading = true;
            
            const calendarGrid = document.querySelector('.calendar-grid');
            if (calendarGrid) {
                calendarGrid.style.opacity = '0.6';
                calendarGrid.style.pointerEvents = 'none';
            }

            // Add loading overlay
            const loadingOverlay = document.createElement('div');
            loadingOverlay.className = 'calendar-loading-overlay';
            loadingOverlay.innerHTML = `
                <div class="text-center">
                    <div class="loading-spinner"></div>
                    <p class="mt-2">Loading calendar...</p>
                </div>
            `;

            const calendarContainer = document.querySelector('.calendar-container');
            if (calendarContainer) {
                calendarContainer.style.position = 'relative';
                calendarContainer.appendChild(loadingOverlay);
            }
        }

        /**
         * Setup accessibility enhancements
         */
        setupAccessibility() {
            // Add ARIA labels to calendar days
            const calendarDays = document.querySelectorAll('.calendar-day');
            calendarDays.forEach(day => {
                const dayNumber = day.querySelector('.calendar-day-number');
                const visits = day.querySelectorAll('.visit-item');
                
                if (dayNumber) {
                    const dateString = day.dataset.dateString;
                    const visitCount = visits.length;
                    
                    let ariaLabel = `${dayNumber.textContent}`;
                    if (dateString) {
                        const date = new Date(dateString);
                        ariaLabel = date.toLocaleDateString('en-US', { 
                            weekday: 'long', 
                            month: 'long', 
                            day: 'numeric' 
                        });
                    }
                    
                    if (visitCount > 0) {
                        ariaLabel += `, ${visitCount} visit${visitCount !== 1 ? 's' : ''} scheduled`;
                    } else {
                        ariaLabel += ', no visits scheduled';
                    }
                    
                    day.setAttribute('aria-label', ariaLabel);
                    day.setAttribute('role', 'button');
                    day.setAttribute('tabindex', '0');
                }
            });

            // Add keyboard navigation for calendar days
            calendarDays.forEach(day => {
                day.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        day.click();
                    }
                });
            });

            // Add focus management
            this.setupFocusManagement();
        }

        /**
         * Setup focus management for keyboard navigation
         */
        setupFocusManagement() {
            const calendarDays = document.querySelectorAll('.calendar-day');
            let currentFocusIndex = -1;

            document.addEventListener('keydown', (e) => {
                if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT') return;

                const focusableDay = document.querySelector('.calendar-day:focus');
                if (focusableDay) {
                    currentFocusIndex = Array.from(calendarDays).indexOf(focusableDay);
                }

                switch(e.key) {
                    case 'ArrowUp':
                        e.preventDefault();
                        this.moveFocus(calendarDays, currentFocusIndex, -7);
                        break;
                    case 'ArrowDown':
                        e.preventDefault();
                        this.moveFocus(calendarDays, currentFocusIndex, 7);
                        break;
                    case 'ArrowLeft':
                        if (!e.shiftKey) {
                            e.preventDefault();
                            this.moveFocus(calendarDays, currentFocusIndex, -1);
                        }
                        break;
                    case 'ArrowRight':
                        if (!e.shiftKey) {
                            e.preventDefault();
                            this.moveFocus(calendarDays, currentFocusIndex, 1);
                        }
                        break;
                    case 'Home':
                        e.preventDefault();
                        this.moveFocus(calendarDays, currentFocusIndex, -currentFocusIndex);
                        break;
                    case 'End':
                        e.preventDefault();
                        this.moveFocus(calendarDays, currentFocusIndex, calendarDays.length - 1 - currentFocusIndex);
                        break;
                }
            });
        }

        /**
         * Move focus to a different calendar day
         * @param {NodeList} days - Calendar day elements
         * @param {number} currentIndex - Current focus index
         * @param {number} offset - Offset to move
         */
        moveFocus(days, currentIndex, offset) {
            const newIndex = Math.max(0, Math.min(days.length - 1, currentIndex + offset));
            if (days[newIndex]) {
                days[newIndex].focus();
            }
        }

        /**
         * Setup performance optimizations
         */
        setupPerformanceOptimizations() {
            // Lazy load visit details
            const visitItems = document.querySelectorAll('.visit-item');
            visitItems.forEach(item => {
                item.addEventListener('mouseenter', this.preloadVisitDetails.bind(this), { once: true });
            });

            // Optimize scroll performance
            let scrollTimeout;
            window.addEventListener('scroll', () => {
                clearTimeout(scrollTimeout);
                scrollTimeout = setTimeout(() => {
                    this.handleScroll();
                }, 100);
            }, { passive: true });
        }

        /**
         * Preload visit details for better UX
         * @param {Event} event - Mouse enter event
         */
        preloadVisitDetails(event) {
            const visitId = event.target.dataset.visitId;
            if (visitId && !event.target.dataset.preloaded) {
                fetch(`/visits/${visitId}/quick`)
                    .then(response => response.text())
                    .then(html => {
                        // Cache the response
                        event.target.dataset.preloaded = 'true';
                        event.target.dataset.cachedHtml = html;
                    })
                    .catch(error => {
                        console.warn('Failed to preload visit details:', error);
                    });
            }
        }

        /**
         * Handle scroll events for performance
         */
        handleScroll() {
            // Add scroll-based optimizations here if needed
            // For example, lazy loading of off-screen elements
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            new CalendarEnhancementManager();
        });
    } else {
        new CalendarEnhancementManager();
    }

})();