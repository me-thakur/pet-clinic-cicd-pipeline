package com.petclinic.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

/**
 * Integration Test Suite
 * 
 * This test suite runs all comprehensive integration tests for the Pet Clinic application.
 * It validates complete user workflows, cross-module functionality, data consistency,
 * and performance requirements.
 * 
 * Test Categories:
 * - SystemIntegrationTest: Basic system integration and API functionality
 * - CompleteWorkflowIntegrationTest: End-to-end user workflows
 * - CrossModuleIntegrationTest: Frontend-backend integration and data consistency
 * - PerformanceIntegrationTest: Performance and scalability validation
 * 
 * **Validates: Requirements All**
 */
@DisplayName("Pet Clinic Integration Test Suite")
public class IntegrationTestSuite {
    
    @Nested
    @DisplayName("System Integration Tests")
    class SystemIntegrationTests extends SystemIntegrationTest {
        // Inherits all tests from SystemIntegrationTest
    }
    
    @Nested
    @DisplayName("Complete Workflow Tests")
    class CompleteWorkflowTests extends CompleteWorkflowIntegrationTest {
        // Inherits all tests from CompleteWorkflowIntegrationTest
    }
    
    @Nested
    @DisplayName("Cross Module Integration Tests")
    class CrossModuleTests extends CrossModuleIntegrationTest {
        // Inherits all tests from CrossModuleIntegrationTest
    }
    
    @Nested
    @DisplayName("Performance Integration Tests")
    class PerformanceTests extends EnhancedPerformanceTest {
        // Inherits all tests from EnhancedPerformanceTest
    }
}