# Implementation Plan: Visit Status Fix

## Overview

This implementation plan addresses the visit status synchronization bug by modifying both backend and frontend components to ensure consistent completion status calculation and display. The approach focuses on making the backend the single source of truth for completion status while updating the frontend to consume and display this computed status.

## Tasks

- [x] 1. Enhance Backend Visit Entity JSON Serialization
  - Add @JsonProperty annotation to expose isCompleted() method result in JSON responses
  - Ensure the computed completion status is included in all Visit entity serializations
  - _Requirements: 3.4, 1.4_

- [ ]* 1.1 Write property test for Visit entity JSON serialization
  - **Property 2: Backend API Response Completeness**
  - **Validates: Requirements 3.4, 1.4**

- [x] 2. Update Backend Visit Controller Response Handling
  - [x] 2.1 Modify updateVisit method to ensure complete response object
    - Verify that PUT /api/visits/{id} returns complete Visit object with computed status
    - Test that response includes the "completed" field matching isCompleted() result
    - _Requirements: 3.1, 1.3_
  
  - [x] 2.2 Verify GET endpoints include completion status
    - Ensure GET /api/visits/{id} includes computed completion status
    - Verify visit list endpoints return status for all visits
    - _Requirements: 3.2, 3.3_

- [ ]* 2.3 Write property test for API response completeness
  - **Property 2: Backend API Response Completeness**
  - **Validates: Requirements 3.1, 3.2, 3.3**

- [x] 3. Checkpoint - Backend API Testing
  - Ensure all backend tests pass, verify API responses include completion status
  - Test with various diagnosis/treatment combinations to confirm status calculation

- [x] 4. Update Frontend Visit Model
  - [x] 4.1 Remove separate completed field from Visit model
    - Remove the Boolean completed field that causes synchronization issues
    - Add transient field to store backend-computed completion status
    - _Requirements: 2.3_
  
  - [x] 4.2 Update Visit model getters/setters
    - Modify getCompleted() to return backend-computed status
    - Update setCompleted() to store backend response value
    - _Requirements: 2.1, 2.3_

- [ ]* 4.3 Write property test for frontend model synchronization
  - **Property 3: Frontend-Backend Status Synchronization**
  - **Validates: Requirements 2.1, 2.3**

- [x] 5. Update Frontend Visit Service
  - [x] 5.1 Enhance updateVisit method response handling
    - Process backend response to extract computed completion status
    - Ensure frontend Visit objects reflect backend-computed status
    - _Requirements: 2.2_
  
  - [x] 5.2 Update visit retrieval methods
    - Modify getVisitById and other retrieval methods to handle completion status
    - Ensure consistent status handling across all service methods
    - _Requirements: 2.1, 2.4_

- [ ]* 5.3 Write property test for service response handling
  - **Property 3: Frontend-Backend Status Synchronization**
  - **Validates: Requirements 2.2, 2.4**

- [x] 6. Update Visit Form Templates and Controllers
  - [x] 6.1 Remove manual completion checkbox from edit forms
    - Remove "Visit Completed" checkbox from visit edit templates
    - Update form validation to not include manual completion field
    - _Requirements: 4.1_
  
  - [x] 6.2 Add read-only completion status display
    - Add visual indicator showing current completion status
    - Display status based on current diagnosis and treatment values
    - _Requirements: 4.2, 4.4_
  
  - [x] 6.3 Update form submission handling
    - Modify form controllers to not send manual completion data
    - Ensure form updates display correct status from backend response
    - _Requirements: 4.3_

- [ ]* 6.4 Write property test for form behavior
  - **Property 4: Form Behavior Consistency**
  - **Validates: Requirements 4.2, 4.3, 4.4**

- [x] 7. Update Visit List Display Components
  - [x] 7.1 Modify visit list templates
    - Update visit list views to display backend-computed completion status
    - Ensure consistent status display across all list views
    - _Requirements: 2.4_
  
  - [x] 7.2 Update visit detail views
    - Modify visit detail pages to show computed completion status
    - Remove any manual completion status controls from detail views
    - _Requirements: 2.1_

- [ ]* 7.3 Write unit tests for display components
  - Test visit list and detail view status display
  - Verify status consistency across different views
  - _Requirements: 2.1, 2.4_

- [x] 8. Implement Data Consistency Validation
  - [x] 8.1 Create visit status validation utility
    - Implement method to validate completion status consistency
    - Add logging for any inconsistencies found
    - _Requirements: 5.1, 5.2_
  
  - [x] 8.2 Add batch recalculation functionality
    - Create administrative function to recalculate all visit statuses
    - Ensure recalculation uses the same logic as isCompleted() method
    - _Requirements: 5.3_

- [ ]* 8.3 Write property test for data consistency validation
  - **Property 5: Data Consistency Validation**
  - **Validates: Requirements 5.1, 5.2, 5.3**

- [x] 9. Enhance Error Handling
  - [x] 9.1 Add backend error handling for completion status
    - Handle null/missing diagnosis and treatment fields gracefully
    - Ensure default "Pending" status for corrupted data
    - _Requirements: 6.1, 6.3_
  
  - [x] 9.2 Add frontend error handling for API failures
    - Handle cases where backend completion status is unavailable
    - Provide fallback display when API communication fails
    - _Requirements: 6.1_

- [ ]* 9.3 Write property test for error handling robustness
  - **Property 6: Error Handling Robustness**
  - **Validates: Requirements 6.1, 6.3**

- [-] 10. Integration Testing and Validation
  - [x] 10.1 Create end-to-end integration tests
    - Test complete visit update flow from frontend to backend
    - Verify status synchronization across the entire system
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [x] 10.2 Test completion logic edge cases
    - Test whitespace-only diagnosis and treatment fields
    - Verify empty field handling and null value processing
    - _Requirements: 6.2_

- [ ]* 10.3 Write comprehensive property test for completion logic
  - **Property 1: Visit Completion Logic Consistency**
  - **Validates: Requirements 1.1, 1.2, 6.2**

- [x] 11. Final Checkpoint - System Integration Testing
  - Ensure all tests pass across backend and frontend
  - Verify visit status displays correctly in all UI components
  - Test with various visit data combinations to confirm fix is complete

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation of the fix
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Focus on backend changes first, then frontend synchronization
- Integration tests ensure the complete fix works end-to-end