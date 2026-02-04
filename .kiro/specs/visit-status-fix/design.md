# Design Document: Visit Status Fix

## Overview

This design addresses the critical synchronization issue between the Pet Clinic frontend and backend systems regarding visit completion status. The backend correctly calculates completion status using the `isCompleted()` method (diagnosis + treatment present), but the frontend maintains a separate `completed` boolean field that becomes out of sync.

The solution involves:
1. Modifying the backend to include computed completion status in API responses
2. Updating the frontend to use backend-computed status instead of maintaining separate state
3. Removing manual completion controls from forms
4. Ensuring consistent status display across all frontend views

## Architecture

### Current Architecture Issues

```mermaid
graph TD
    A[Frontend Visit Form] --> B[Frontend Visit Model]
    B --> C[VisitService.updateVisit]
    C --> D[Backend API PUT /visits/{id}]
    D --> E[Backend Visit Entity]
    E --> F[isCompleted() method]
    
    G[Frontend Display] --> H[Frontend completed field]
    
    F -.-> I[Not returned to frontend]
    H -.-> J[Out of sync with backend]
    
    style I fill:#ffcccc
    style J fill:#ffcccc
```

### Target Architecture

```mermaid
graph TD
    A[Frontend Visit Form] --> B[Frontend Visit Model]
    B --> C[VisitService.updateVisit]
    C --> D[Backend API PUT /visits/{id}]
    D --> E[Backend Visit Entity]
    E --> F[isCompleted() method]
    F --> G[JSON Response with computed status]
    G --> H[Frontend Display]
    
    style G fill:#ccffcc
    style H fill:#ccffcc
```

## Components and Interfaces

### Backend Changes

#### 1. Visit Entity JSON Serialization
- **Component**: `com.petclinic.backend.model.Visit`
- **Change**: Add `@JsonProperty("completed")` annotation to expose `isCompleted()` result
- **Interface**: JSON serialization includes computed completion status

#### 2. Visit Controller Response Enhancement
- **Component**: `com.petclinic.backend.controller.VisitController`
- **Method**: `updateVisit(Long id, Map<String, Object> visitData)`
- **Change**: Ensure response includes complete visit object with computed status
- **Interface**: PUT /api/visits/{id} returns Visit with completion status

#### 3. Visit Service Update Logic
- **Component**: `com.petclinic.backend.service.impl.VisitServiceImpl`
- **Method**: `updateVisitFields(Visit existingVisit, Visit updatedVisit)`
- **Change**: Ensure completion status is recalculated after field updates
- **Interface**: Internal service method maintains consistency

### Frontend Changes

#### 1. Visit Model Simplification
- **Component**: `com.petclinic.frontend.model.Visit`
- **Change**: Remove `completed` field, add computed getter that reads from backend response
- **Interface**: Model reflects backend state, not separate frontend state

#### 2. Visit Service Response Handling
- **Component**: `com.petclinic.frontend.service.VisitService`
- **Method**: `updateVisit(Long id, Visit visit)`
- **Change**: Process backend response to extract computed completion status
- **Interface**: Service returns Visit with backend-computed status

#### 3. Visit Form Template Updates
- **Component**: Visit edit form templates
- **Change**: Remove manual completion checkbox, add read-only status display
- **Interface**: Form shows automatic status based on diagnosis/treatment

#### 4. Visit List Display Updates
- **Component**: Visit list templates and controllers
- **Change**: Display status from backend-computed field
- **Interface**: Consistent status display across all views

## Data Models

### Backend Visit Entity (Enhanced)
```java
@Entity
public class Visit extends BaseEntity {
    // ... existing fields ...
    
    @JsonProperty("completed")
    public boolean isCompleted() {
        return diagnosis != null && !diagnosis.trim().isEmpty() &&
               treatment != null && !treatment.trim().isEmpty();
    }
}
```

### Frontend Visit Model (Simplified)
```java
public class Visit {
    // Remove: private Boolean completed = false;
    
    // ... existing fields ...
    
    // Transient field populated from backend response
    @JsonProperty("completed")
    private Boolean completedFromBackend;
    
    public Boolean getCompleted() {
        return completedFromBackend != null ? completedFromBackend : false;
    }
    
    public void setCompleted(Boolean completed) {
        this.completedFromBackend = completed;
    }
}
```

### API Response Format
```json
{
    "id": 123,
    "visitDate": "2024-01-15T10:30:00",
    "diagnosis": "Routine checkup completed",
    "treatment": "Vaccinations administered",
    "cost": 75.00,
    "completed": true,
    "pet": { "id": 456 },
    "veterinarian": { "id": 789 }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Visit Completion Logic Consistency
*For any* visit with diagnosis and treatment fields, the completion status should be "Completed" if and only if both fields contain non-empty, non-whitespace text, and "Pending" otherwise
**Validates: Requirements 1.1, 1.2, 6.2**

### Property 2: Backend API Response Completeness
*For any* visit operation (create, update, retrieve), the backend API response should include a "completed" field that accurately reflects the isCompleted() method result
**Validates: Requirements 1.3, 1.4, 3.1, 3.2, 3.3, 3.4**

### Property 3: Frontend-Backend Status Synchronization
*For any* visit displayed in the frontend, the completion status shown should match the backend's computed completion status and never rely on separate frontend state
**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

### Property 4: Form Behavior Consistency
*For any* visit edit form interaction, the displayed completion status should automatically reflect the current diagnosis and treatment field values according to the completion logic
**Validates: Requirements 4.2, 4.3, 4.4**

### Property 5: Data Consistency Validation
*For any* set of existing visits, the system should be able to validate and recalculate completion status to ensure consistency with diagnosis and treatment data
**Validates: Requirements 5.1, 5.2, 5.3**

### Property 6: Error Handling Robustness
*For any* visit update operation that encounters errors or corrupted data, the system should maintain data consistency and default to "Pending" status when completion cannot be determined
**Validates: Requirements 6.1, 6.3**

## Error Handling

### Backend Error Scenarios
1. **Null/Missing Fields**: When diagnosis or treatment fields are null, treat as empty for completion logic
2. **Serialization Errors**: If JSON serialization fails, ensure graceful degradation with default "Pending" status
3. **Database Constraints**: Handle database constraint violations during visit updates
4. **Concurrent Updates**: Use optimistic locking to prevent race conditions in status calculation

### Frontend Error Scenarios
1. **API Communication Failures**: Display cached status with error indicator when backend is unavailable
2. **Invalid Backend Responses**: Default to "Pending" status if backend response is malformed
3. **Form Validation Errors**: Maintain consistent status display even when form validation fails
4. **Network Timeouts**: Provide user feedback and retry mechanisms for failed updates

## Testing Strategy

### Dual Testing Approach
The system requires both unit testing and property-based testing for comprehensive coverage:

**Unit Tests** focus on:
- Specific examples of completion logic (empty fields, whitespace-only fields, valid combinations)
- API endpoint response formats and error conditions
- Frontend form behavior with known input combinations
- Integration points between frontend and backend services

**Property-Based Tests** focus on:
- Universal completion logic across all possible diagnosis/treatment combinations
- API response consistency across different visit data sets
- Frontend-backend synchronization with randomized visit data
- Error handling robustness with various failure scenarios

### Property-Based Testing Configuration
- **Library**: Use JUnit 5 with jqwik for Java property-based testing
- **Iterations**: Minimum 100 iterations per property test
- **Test Tags**: Each property test must reference its design document property
- **Tag Format**: **Feature: visit-status-fix, Property {number}: {property_text}**

### Test Data Generation
- **Visit Generator**: Create visits with random diagnosis/treatment combinations including edge cases
- **Field Generator**: Generate strings with various whitespace patterns, empty strings, and null values
- **API Response Generator**: Create realistic API responses with different completion status scenarios
- **Form Input Generator**: Generate form input combinations to test frontend behavior

### Integration Testing
- **End-to-End Flows**: Test complete visit update flows from frontend form to backend persistence
- **API Contract Testing**: Verify API responses match expected schemas with completion status
- **Database Consistency**: Test that database state matches computed completion status
- **Cross-Browser Testing**: Ensure frontend status display works consistently across browsers

### Performance Considerations
- **Completion Calculation**: Ensure isCompleted() method performance is acceptable for large visit datasets
- **API Response Size**: Monitor impact of adding completion status to all API responses
- **Frontend Rendering**: Test that status updates don't cause unnecessary re-renders
- **Database Queries**: Optimize queries that filter by completion status