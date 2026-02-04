# Design Document: Critical Fixes and Enhancements

## Overview

This design document outlines the technical approach for resolving 13 critical issues in the pet clinic application. The fixes address fundamental problems with error handling, search functionality, validation services, report exports, multi-delete operations, table sorting, filtering, calendar views, global search, UI visibility, and form functionality.

## Architecture Overview

### System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Layer                           │
├─────────────────────────────────────────────────────────────────┤
│  Error Handler  │  Search UI  │  Table Components  │  Forms     │
│  - Specific     │  - Global   │  - Enhanced Tables │  - Owner   │
│    Messages     │  - Filters  │  - Multi-select    │  - Pet     │
│  - Recovery     │  - Calendar │  - Server Sorting  │  - Validation│
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  Controllers with Enhanced Error Handling and Validation        │
│  - Owner Controller    │  - Pet Controller                      │
│  - Visit Controller    │  - Veterinarian Controller             │
│  - Search Controller   │  - Report Controller (Admin)           │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                             │
├─────────────────────────────────────────────────────────────────┤
│  Enhanced Services with Fallback Mechanisms                     │
│  - Validation Service  │  - Search Service                      │
│  - Export Service      │  - Enhanced Table Service              │
│  - Calendar Service    │  - Multi-Delete Service                │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Data Access Layer                           │
├─────────────────────────────────────────────────────────────────┤
│  Repositories with Optimized Queries and Bulk Operations        │
│  - Owner Repository    │  - Pet Repository                      │
│  - Visit Repository    │  - Veterinarian Repository             │
│  - Search Repository   │  - Audit Repository                    │
└─────────────────────────────────────────────────────────────────┘
```

## Component Design

### 1. Error Handling System

#### Enhanced Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .errorCode("VALIDATION_FAILED")
                .message("Please correct the following fields:")
                .fieldErrors(ex.getFieldErrors())
                .suggestions(generateSuggestions(ex))
                .build());
    }
    
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(503)
            .body(ErrorResponse.builder()
                .errorCode("SERVICE_UNAVAILABLE")
                .message("Service temporarily unavailable: " + ex.getServiceName())
                .retryAfter(ex.getRetryAfter())
                .fallbackOptions(ex.getFallbackOptions())
                .build());
    }
}
```

#### Frontend Error Handler
```javascript
class ErrorHandler {
    handleError(error, context) {
        const errorType = this.categorizeError(error);
        
        switch (errorType) {
            case 'VALIDATION':
                return this.handleValidationError(error, context);
            case 'SERVICE_UNAVAILABLE':
                return this.handleServiceUnavailable(error, context);
            case 'NETWORK':
                return this.handleNetworkError(error, context);
            default:
                return this.handleGenericError(error, context);
        }
    }
    
    handleValidationError(error, context) {
        // Display field-specific errors
        error.fieldErrors.forEach(fieldError => {
            this.displayFieldError(fieldError.field, fieldError.message);
        });
        
        return { handled: true, fallback: false };
    }
}
```

### 2. Senior Pet Search System

#### Senior Pet Controller
```java
@RestController
@RequestMapping("/api/pets/senior")
public class SeniorPetController {
    
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<SeniorPetInfo>> searchSeniorPets(
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String healthCondition,
            Pageable pageable) {
        
        SeniorPetSearchCriteria criteria = SeniorPetSearchCriteria.builder()
            .minAge(minAge)
            .maxAge(maxAge)
            .species(species)
            .healthCondition(healthCondition)
            .build();
            
        Page<SeniorPetInfo> results = seniorPetService.searchSeniorPets(criteria, pageable);
        return ResponseEntity.ok(PagedResponse.of(results));
    }
}
```

#### Senior Pet Service
```java
@Service
public class SeniorPetService {
    
    private final Map<String, Integer> speciesAgeThresholds = Map.of(
        "Dog", 7,
        "Cat", 7,
        "Bird", 5,
        "Rabbit", 5
    );
    
    public Page<SeniorPetInfo> searchSeniorPets(SeniorPetSearchCriteria criteria, Pageable pageable) {
        Specification<Pet> spec = Specification.where(null);
        
        // Apply age-based filtering with species-specific thresholds
        if (criteria.getSpecies() != null) {
            Integer threshold = speciesAgeThresholds.getOrDefault(criteria.getSpecies(), 7);
            spec = spec.and(PetSpecifications.olderThan(threshold));
            spec = spec.and(PetSpecifications.hasSpecies(criteria.getSpecies()));
        }
        
        return petRepository.findAll(spec, pageable)
            .map(this::toSeniorPetInfo);
    }
}
```

### 3. Enhanced Validation System

#### Validation Service with Fallback
```java
@Service
public class EnhancedValidationService {
    
    private final ValidationServiceClient validationClient;
    private final ClientSideValidator clientSideValidator;
    private final CircuitBreaker circuitBreaker;
    
    public ValidationResult validateOwner(Owner owner) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                return validationClient.validateOwner(owner);
            } catch (ServiceUnavailableException ex) {
                log.warn("Validation service unavailable, falling back to client-side validation");
                ValidationResult result = clientSideValidator.validateOwner(owner);
                result.addWarning("Full validation temporarily unavailable. Basic validation performed.");
                return result;
            }
        });
    }
}
```

#### International Postal Code Validator
```java
@Component
public class InternationalPostalCodeValidator {
    
    private final Map<String, Pattern> countryPatterns = Map.of(
        "US", Pattern.compile("^\\d{5}(-\\d{4})?$"),
        "CA", Pattern.compile("^[A-Z]\\d[A-Z] \\d[A-Z]\\d$"),
        "UK", Pattern.compile("^[A-Z]{1,2}\\d[A-Z\\d]? \\d[A-Z]{2}$"),
        "DE", Pattern.compile("^\\d{5}$"),
        "FR", Pattern.compile("^\\d{5}$"),
        "AU", Pattern.compile("^\\d{4}$"),
        "JP", Pattern.compile("^\\d{3}-\\d{4}$")
    );
    
    public ValidationResult validatePostalCode(String postalCode, String country) {
        if (StringUtils.isEmpty(postalCode)) {
            return ValidationResult.valid(); // Optional field
        }
        
        Pattern pattern = countryPatterns.get(country);
        if (pattern == null) {
            // Fallback validation for unknown countries
            return validateGenericPostalCode(postalCode);
        }
        
        if (pattern.matcher(postalCode).matches()) {
            return ValidationResult.valid();
        } else {
            return ValidationResult.invalid(
                "Invalid postal code format for " + country + 
                ". Expected format: " + getFormatExample(country)
            );
        }
    }
}
```

### 4. Report Export System

#### Enhanced Report Export Service
```java
@Service
public class EnhancedReportExportService {
    
    @Async
    public CompletableFuture<ExportResult> exportReport(ExportRequest request) {
        try {
            validateExportPermissions(request);
            
            ExportProgress progress = createProgressTracker(request);
            
            byte[] data = generateReportData(request, progress);
            String filename = generateFilename(request);
            
            ExportResult result = ExportResult.builder()
                .data(data)
                .filename(filename)
                .contentType(getContentType(request.getFormat()))
                .build();
                
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception ex) {
            log.error("Report export failed", ex);
            throw new ReportExportException("Export failed: " + ex.getMessage(), ex);
        }
    }
    
    private void validateExportPermissions(ExportRequest request) {
        if (!securityService.hasRole("ADMIN")) {
            throw new AccessDeniedException("Report export requires admin privileges");
        }
    }
}
```

### 5. Multi-Delete System

#### Bulk Operations Controller
```java
@RestController
public class BulkOperationsController {
    
    @DeleteMapping("/api/{entityType}/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BulkOperationResult> bulkDelete(
            @PathVariable String entityType,
            @RequestBody BulkDeleteRequest request) {
        
        try {
            validateBulkDeleteRequest(request);
            
            BulkOperationResult result = bulkOperationService.performBulkDelete(
                entityType, request.getIds(), request.getConfirmationToken()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (BulkOperationException ex) {
            return ResponseEntity.badRequest()
                .body(BulkOperationResult.failed(ex.getMessage()));
        }
    }
}
```

#### Bulk Operation Service
```java
@Service
@Transactional
public class BulkOperationService {
    
    public BulkOperationResult performBulkDelete(String entityType, List<Long> ids, String confirmationToken) {
        validateConfirmationToken(confirmationToken);
        
        BulkOperationResult.Builder resultBuilder = BulkOperationResult.builder()
            .entityType(entityType)
            .totalRequested(ids.size());
        
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long id : ids) {
            try {
                deleteEntity(entityType, id);
                successCount++;
            } catch (Exception ex) {
                errors.add("Failed to delete " + entityType + " with ID " + id + ": " + ex.getMessage());
            }
        }
        
        return resultBuilder
            .successCount(successCount)
            .errors(errors)
            .build();
    }
}
```

### 6. Server-Side Sorting System

#### Enhanced Table Service
```java
@Service
public class EnhancedTableService<T> {
    
    public PagedResponse<T> findWithSortAndFilter(
            String entityType, 
            Pageable pageable, 
            SortMetadata sortMetadata, 
            List<FilterCriteria> filters) {
        
        try {
            // Apply sorting at database level
            Sort sort = buildSort(sortMetadata);
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            // Apply filters
            Specification<T> spec = buildSpecification(filters);
            
            Page<T> results = getRepository(entityType).findAll(spec, sortedPageable);
            
            return PagedResponse.<T>builder()
                .content(results.getContent())
                .page(PageInfo.from(results))
                .sortMetadata(sortMetadata)
                .activeFilters(filters)
                .build();
                
        } catch (Exception ex) {
            log.error("Server-side sorting failed for {}", entityType, ex);
            throw new TableOperationException("Sorting failed", ex);
        }
    }
}
```

### 7. Enhanced Search System

#### Global Search Service
```java
@Service
public class GlobalSearchService {
    
    private final List<EntitySearchProvider> searchProviders;
    
    public SearchResultSummary globalSearch(String query, int page, int size) {
        List<CompletableFuture<EntitySearchResult>> futures = searchProviders.stream()
            .map(provider -> CompletableFuture.supplyAsync(() -> 
                provider.search(query, page, size)))
            .collect(Collectors.toList());
        
        List<EntitySearchResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        return SearchResultSummary.builder()
            .query(query)
            .results(combineResults(results))
            .entityCounts(calculateEntityCounts(results))
            .totalResults(calculateTotalResults(results))
            .executionTimeMs(System.currentTimeMillis() - startTime)
            .build();
    }
}
```

#### Visit Search with Filters
```java
@RestController
@RequestMapping("/api/visits/search")
public class VisitSearchController {
    
    @GetMapping("/by-pet")
    public ResponseEntity<Page<Visit>> searchByPet(
            @RequestParam Long petId,
            Pageable pageable) {
        
        Page<Visit> results = visitService.findByPetId(petId, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/by-veterinarian")
    public ResponseEntity<Page<Visit>> searchByVeterinarian(
            @RequestParam Long veterinarianId,
            Pageable pageable) {
        
        Page<Visit> results = visitService.findByVeterinarianId(veterinarianId, pageable);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/by-status")
    public ResponseEntity<Page<Visit>> searchByStatus(
            @RequestParam String status,
            Pageable pageable) {
        
        VisitStatus visitStatus = VisitStatus.valueOf(status.toUpperCase());
        Page<Visit> results = visitService.findByStatus(visitStatus, pageable);
        return ResponseEntity.ok(results);
    }
}
```

### 8. Calendar View System

#### Calendar Service
```java
@Service
public class CalendarService {
    
    public CalendarView getCalendarView(YearMonth yearMonth, Long veterinarianId) {
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        List<Visit> visits = visitRepository.findByDateRangeAndVeterinarian(
            startOfMonth, endOfMonth, veterinarianId
        );
        
        List<CalendarDay> calendarDays = generateCalendarDays(yearMonth, visits);
        
        return CalendarView.builder()
            .yearMonth(yearMonth)
            .days(calendarDays)
            .veterinarianId(veterinarianId)
            .build();
    }
    
    private List<CalendarDay> generateCalendarDays(YearMonth yearMonth, List<Visit> visits) {
        Map<LocalDate, List<Visit>> visitsByDate = visits.stream()
            .collect(Collectors.groupingBy(visit -> visit.getVisitDate().toLocalDate()));
        
        List<CalendarDay> days = new ArrayList<>();
        LocalDate startDate = yearMonth.atDay(1).with(DayOfWeek.MONDAY);
        
        for (int i = 0; i < 42; i++) { // 6 weeks
            LocalDate date = startDate.plusDays(i);
            List<Visit> dayVisits = visitsByDate.getOrDefault(date, Collections.emptyList());
            
            days.add(CalendarDay.builder()
                .date(date)
                .visits(dayVisits)
                .isCurrentMonth(date.getMonth() == yearMonth.getMonth())
                .isToday(date.equals(LocalDate.now()))
                .build());
        }
        
        return days;
    }
}
```

## Frontend Architecture

### Enhanced Table Component
```javascript
class EnhancedTable {
    constructor(tableElement, options) {
        this.table = tableElement;
        this.options = {
            entityType: options.entityType,
            enableServerSideSort: true,
            enableMultiSelect: true,
            enableFiltering: true,
            ...options
        };
        
        this.sortController = new SortController(this);
        this.filterController = new FilterController(this);
        this.bulkController = new BulkController(this);
        this.errorHandler = new ErrorHandler(this);
        
        this.init();
    }
    
    async applySortAsync(column, direction, addToExisting = false) {
        try {
            this.showLoadingState();
            
            if (this.options.enableServerSideSort) {
                await this.sortController.applyServerSideSort(column, direction, addToExisting);
            } else {
                await this.sortController.applyClientSideSort(column, direction);
            }
            
        } catch (error) {
            await this.errorHandler.handleSortError(error, { column, direction });
        } finally {
            this.hideLoadingState();
        }
    }
}
```

### Multi-Select and Bulk Operations
```javascript
class BulkController {
    constructor(enhancedTable) {
        this.enhancedTable = enhancedTable;
        this.selectedItems = new Set();
        this.setupBulkControls();
    }
    
    async bulkDeleteAsync() {
        if (this.selectedItems.size === 0) {
            throw new Error('No items selected for deletion');
        }
        
        const confirmed = await this.showConfirmationDialog();
        if (!confirmed) {
            return { cancelled: true };
        }
        
        try {
            const response = await fetch(`/api/${this.enhancedTable.options.entityType}/bulk`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ids: Array.from(this.selectedItems),
                    confirmationToken: this.generateConfirmationToken()
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showSuccessMessage(`Successfully deleted ${result.successCount} items`);
                this.enhancedTable.refresh();
                this.clearSelection();
            } else {
                this.showErrorMessage(`Bulk delete partially failed: ${result.errors.join(', ')}`);
            }
            
            return result;
            
        } catch (error) {
            this.showErrorMessage('Bulk delete failed: ' + error.message);
            throw error;
        }
    }
}
```

## Data Models

### Error Response Model
```java
@Data
@Builder
public class ErrorResponse {
    private String errorCode;
    private String message;
    private List<FieldError> fieldErrors;
    private List<String> suggestions;
    private String retryAfter;
    private List<String> fallbackOptions;
    private String timestamp;
    private String path;
}

@Data
@Builder
public class FieldError {
    private String field;
    private String message;
    private String rejectedValue;
    private List<String> suggestions;
}
```

### Search Models
```java
@Data
@Builder
public class SearchResultSummary {
    private String query;
    private List<SearchResult> results;
    private Map<String, Integer> entityCounts;
    private int totalResults;
    private long executionTimeMs;
    private int currentPage;
    private int totalPages;
}

@Data
@Builder
public class SearchResult {
    private String entityType;
    private Long entityId;
    private String title;
    private String description;
    private Map<String, String> metadata;
    private String url;
    private String editUrl;
    private double relevanceScore;
}
```

### Bulk Operation Models
```java
@Data
@Builder
public class BulkOperationResult {
    private boolean success;
    private String entityType;
    private int totalRequested;
    private int successCount;
    private List<String> errors;
    private String operationId;
    private long executionTimeMs;
}

@Data
@Builder
public class BulkDeleteRequest {
    private List<Long> ids;
    private String confirmationToken;
    private String entityType;
}
```

## Security Considerations

### Access Control
- Admin-only access for report exports
- Role-based permissions for bulk operations
- Audit logging for sensitive operations
- CSRF protection for state-changing operations

### Data Protection
- Sanitize error messages to prevent information leakage
- Validate all input parameters
- Use parameterized queries to prevent SQL injection
- Implement rate limiting for search operations

## Performance Optimizations

### Database Optimizations
- Add indexes for commonly sorted columns
- Optimize queries for server-side sorting
- Use pagination to limit result sets
- Implement query result caching

### Frontend Optimizations
- Debounce search inputs
- Use virtual scrolling for large tables
- Implement progressive loading for calendar views
- Cache filter options and search suggestions

## Testing Strategy

### Property-Based Tests
Each major component will have property-based tests to verify:
- Error handling consistency
- Search result accuracy
- Sorting correctness
- Data integrity during bulk operations

### Integration Tests
- End-to-end workflows for all fixed functionality
- Error scenario testing
- Performance testing under load
- Security testing for access controls

### User Acceptance Testing
- Verify all 13 critical issues are resolved
- Test error messages are specific and actionable
- Confirm all UI components are properly visible
- Validate search and filter functionality works as expected

## Deployment Strategy

### Phased Rollout
1. **Phase 1**: Error handling and validation fixes
2. **Phase 2**: Search and filtering enhancements
3. **Phase 3**: Table operations and bulk functionality
4. **Phase 4**: UI fixes and calendar improvements
5. **Phase 5**: Performance optimizations and final testing

### Rollback Plan
- Feature flags for new functionality
- Database migration rollback scripts
- Frontend asset versioning
- Monitoring and alerting for critical issues

## Monitoring and Observability

### Metrics
- Error rates by component
- Search performance metrics
- Table operation response times
- Bulk operation success rates

### Logging
- Structured logging for all operations
- Error context preservation
- Performance timing information
- User action audit trails

### Alerting
- High error rates
- Performance degradation
- Service unavailability
- Security violations