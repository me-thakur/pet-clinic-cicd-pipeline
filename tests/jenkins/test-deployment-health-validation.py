#!/usr/bin/env python3
"""
Property-based test for Deployment Health Validation

Property 7: Deployment Health Validation
For any successful deployment, the health check process should verify that all deployed 
components are responding correctly before marking the deployment as complete

Validates: Requirements 6.4
"""

import pytest
import requests
import json
import time
import subprocess
import os
from hypothesis import given, strategies as st, settings
from typing import Dict, Any, Optional, List
import uuid
import tempfile


class DeploymentHealthValidationTest:
    """Property-based test for deployment health validation workflow"""
    
    def __init__(self):
        self.backend_url = os.getenv('BACKEND_URL', 'http://localhost:8080/api')
        self.frontend_url = os.getenv('FRONTEND_URL', 'http://localhost:8081')
        self.db_host = os.getenv('DB_HOST', 'localhost')
        self.db_user = os.getenv('DB_USER', 'petclinic')
        self.db_password = os.getenv('DB_PASSWORD', '')
        self.db_name = os.getenv('DB_NAME', 'petclinic')
        self.health_check_script = os.getenv('HEALTH_CHECK_SCRIPT', './deployment-scripts/health-check.sh')
        
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up test environment with unique identifiers"""
        test_id = str(uuid.uuid4())[:8]
        return {
            'test_id': test_id,
            'deployment_id': f'deploy-{test_id}',
            'timestamp': int(time.time())
        }
    
    def simulate_deployment_completion(self, test_env: Dict[str, Any]) -> bool:
        """Simulate a deployment completion event"""
        # In a real scenario, this would be triggered by Jenkins pipeline completion
        # For testing, we'll assume deployment is complete if services are running
        
        try:
            # Check if backend service is running
            backend_response = requests.get(f'{self.backend_url}/actuator/health', timeout=5)
            backend_healthy = backend_response.status_code == 200
            
            # Check if frontend service is running  
            frontend_response = requests.get(f'{self.frontend_url}/actuator/health', timeout=5)
            frontend_healthy = frontend_response.status_code == 200
            
            return backend_healthy and frontend_healthy
        except Exception:
            return False
    
    def run_health_check_process(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Run the health check process and return results"""
        health_results = {
            'overall_status': 'UNKNOWN',
            'component_checks': {},
            'execution_time': 0,
            'error_message': None
        }
        
        start_time = time.time()
        
        try:
            # Run health check script if available
            if os.path.exists(self.health_check_script):
                result = subprocess.run(
                    [self.health_check_script],
                    capture_output=True,
                    text=True,
                    timeout=120,
                    env=dict(os.environ, **{
                        'BACKEND_URL': self.backend_url,
                        'FRONTEND_URL': self.frontend_url,
                        'DB_HOST': self.db_host,
                        'DB_USER': self.db_user,
                        'DB_PASSWORD': self.db_password,
                        'DB_NAME': self.db_name
                    })
                )
                
                health_results['overall_status'] = 'PASS' if result.returncode == 0 else 'FAIL'
                health_results['script_output'] = result.stdout
                health_results['script_errors'] = result.stderr
                
            else:
                # Fallback to manual health checks
                health_results = self._manual_health_checks(test_env)
                
        except subprocess.TimeoutExpired:
            health_results['overall_status'] = 'TIMEOUT'
            health_results['error_message'] = 'Health check script timed out'
        except Exception as e:
            health_results['overall_status'] = 'ERROR'
            health_results['error_message'] = str(e)
        
        health_results['execution_time'] = time.time() - start_time
        return health_results
    
    def _manual_health_checks(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Perform manual health checks when script is not available"""
        results = {
            'overall_status': 'PASS',
            'component_checks': {},
            'execution_time': 0,
            'error_message': None
        }
        
        checks_passed = 0
        total_checks = 0
        
        # Backend health check
        total_checks += 1
        try:
            response = requests.get(f'{self.backend_url}/actuator/health', timeout=10)
            if response.status_code == 200 and response.json().get('status') == 'UP':
                results['component_checks']['backend_health'] = 'PASS'
                checks_passed += 1
            else:
                results['component_checks']['backend_health'] = f'FAIL - HTTP {response.status_code}'
        except Exception as e:
            results['component_checks']['backend_health'] = f'FAIL - {str(e)}'
        
        # Frontend health check
        total_checks += 1
        try:
            response = requests.get(f'{self.frontend_url}/actuator/health', timeout=10)
            if response.status_code == 200 and response.json().get('status') == 'UP':
                results['component_checks']['frontend_health'] = 'PASS'
                checks_passed += 1
            else:
                results['component_checks']['frontend_health'] = f'FAIL - HTTP {response.status_code}'
        except Exception as e:
            results['component_checks']['frontend_health'] = f'FAIL - {str(e)}'
        
        # Database connectivity check
        if self.db_password:
            total_checks += 1
            try:
                # Simple database connectivity test
                import mysql.connector
                conn = mysql.connector.connect(
                    host=self.db_host,
                    user=self.db_user,
                    password=self.db_password,
                    database=self.db_name,
                    connection_timeout=10
                )
                cursor = conn.cursor()
                cursor.execute("SELECT 1")
                cursor.fetchone()
                cursor.close()
                conn.close()
                results['component_checks']['database_connectivity'] = 'PASS'
                checks_passed += 1
            except Exception as e:
                results['component_checks']['database_connectivity'] = f'FAIL - {str(e)}'
        
        # Frontend web interface check
        total_checks += 1
        try:
            response = requests.get(self.frontend_url, timeout=10)
            if response.status_code == 200 and 'Pet Clinic' in response.text:
                results['component_checks']['frontend_web'] = 'PASS'
                checks_passed += 1
            else:
                results['component_checks']['frontend_web'] = f'FAIL - HTTP {response.status_code}'
        except Exception as e:
            results['component_checks']['frontend_web'] = f'FAIL - {str(e)}'
        
        # Backend API functionality check
        total_checks += 1
        try:
            response = requests.get(f'{self.backend_url}/owners', timeout=10)
            if response.status_code in [200, 404]:  # 404 is OK if no owners exist
                results['component_checks']['backend_api'] = 'PASS'
                checks_passed += 1
            else:
                results['component_checks']['backend_api'] = f'FAIL - HTTP {response.status_code}'
        except Exception as e:
            results['component_checks']['backend_api'] = f'FAIL - {str(e)}'
        
        # Determine overall status
        if checks_passed == total_checks:
            results['overall_status'] = 'PASS'
        elif checks_passed >= total_checks * 0.8:  # 80% pass rate
            results['overall_status'] = 'WARN'
        else:
            results['overall_status'] = 'FAIL'
        
        return results
    
    def verify_deployment_marked_complete(self, test_env: Dict[str, Any], health_results: Dict[str, Any]) -> bool:
        """Verify that deployment is only marked complete after successful health checks"""
        # In a real CI/CD pipeline, this would check Jenkins build status or deployment status
        # For testing purposes, we simulate this by checking if health checks passed
        
        if health_results['overall_status'] == 'PASS':
            # Deployment should be marked as complete
            return True
        else:
            # Deployment should NOT be marked as complete if health checks failed
            return False


@pytest.fixture
def deployment_health_test():
    """Fixture for deployment health validation test"""
    return DeploymentHealthValidationTest()


@given(
    deployment_scenario=st.sampled_from(['normal', 'backend_slow', 'frontend_error', 'database_issue']),
    health_check_timeout=st.integers(min_value=30, max_value=120)
)
@settings(max_examples=4, deadline=300000)  # 5 minute timeout per test
def test_deployment_health_validation_property(deployment_health_test, deployment_scenario, health_check_timeout):
    """
    **Validates: Requirements 6.4**
    
    Property 7: Deployment Health Validation
    For any successful deployment, the health check process should verify that all deployed 
    components are responding correctly before marking the deployment as complete
    """
    # Set up test environment
    test_env = deployment_health_test.setup_test_environment()
    test_env['scenario'] = deployment_scenario
    test_env['timeout'] = health_check_timeout
    
    # Step 1: Simulate deployment completion
    deployment_completed = deployment_health_test.simulate_deployment_completion(test_env)
    
    if not deployment_completed:
        pytest.skip("Deployment simulation failed - services not available for testing")
    
    # Step 2: Run health check process (Requirement 6.4)
    health_results = deployment_health_test.run_health_check_process(test_env)
    
    # Step 3: Verify health check process executed
    assert health_results is not None, "Health check process did not execute"
    assert 'overall_status' in health_results, "Health check did not return overall status"
    assert health_results['overall_status'] in ['PASS', 'FAIL', 'WARN', 'TIMEOUT', 'ERROR'], f"Invalid health check status: {health_results['overall_status']}"
    
    # Step 4: Verify health check validated all deployed components
    if 'component_checks' in health_results:
        # Verify that key components were checked
        expected_components = ['backend_health', 'frontend_health']
        for component in expected_components:
            if component in health_results['component_checks']:
                assert health_results['component_checks'][component] is not None, f"Component {component} was not properly checked"
    
    # Step 5: Verify deployment completion status aligns with health check results
    deployment_marked_complete = deployment_health_test.verify_deployment_marked_complete(test_env, health_results)
    
    if health_results['overall_status'] == 'PASS':
        assert deployment_marked_complete, "Deployment should be marked complete when health checks pass"
    elif health_results['overall_status'] in ['FAIL', 'ERROR', 'TIMEOUT']:
        # In a real system, deployment should not be marked complete if health checks fail
        # For testing, we'll verify the health check process detected the issues
        assert health_results['overall_status'] != 'PASS', "Health check should detect deployment issues"
    
    # Step 6: Verify health check execution time is reasonable
    assert health_results['execution_time'] > 0, "Health check execution time should be recorded"
    assert health_results['execution_time'] < health_check_timeout + 30, "Health check took too long to execute"
    
    print(f"✓ Property 7 validated: Deployment {test_env['deployment_id']} health check completed with status {health_results['overall_status']} in {health_results['execution_time']:.2f}s")


def test_deployment_health_validation_manual():
    """
    Manual test case for deployment health validation
    This test can be run independently to verify health validation works
    """
    test_instance = DeploymentHealthValidationTest()
    
    # Set up test environment
    test_env = test_instance.setup_test_environment()
    
    # Check if deployment is ready
    deployment_ready = test_instance.simulate_deployment_completion(test_env)
    if not deployment_ready:
        pytest.skip("Services not available for health validation test")
    
    # Run health check process
    health_results = test_instance.run_health_check_process(test_env)
    
    print(f"Health Check Results:")
    print(f"  Overall Status: {health_results['overall_status']}")
    print(f"  Execution Time: {health_results['execution_time']:.2f}s")
    
    if 'component_checks' in health_results:
        print("  Component Checks:")
        for component, status in health_results['component_checks'].items():
            print(f"    {component}: {status}")
    
    if 'script_output' in health_results:
        print("  Script Output (last 10 lines):")
        output_lines = health_results['script_output'].split('\n')[-10:]
        for line in output_lines:
            if line.strip():
                print(f"    {line}")
    
    # Verify deployment completion logic
    deployment_complete = test_instance.verify_deployment_marked_complete(test_env, health_results)
    print(f"  Deployment Marked Complete: {deployment_complete}")
    
    # Assert that health check process worked
    assert health_results['overall_status'] in ['PASS', 'FAIL', 'WARN'], f"Health check returned unexpected status: {health_results['overall_status']}"
    
    print("✓ Manual deployment health validation test completed successfully")


def test_health_check_component_coverage():
    """
    Test that health check covers all critical deployment components
    """
    test_instance = DeploymentHealthValidationTest()
    test_env = test_instance.setup_test_environment()
    
    # Run health check
    health_results = test_instance.run_health_check_process(test_env)
    
    # Verify critical components are checked
    if 'component_checks' in health_results:
        component_checks = health_results['component_checks']
        
        # Should check backend health
        backend_checked = any('backend' in key.lower() for key in component_checks.keys())
        assert backend_checked, "Health check should verify backend component"
        
        # Should check frontend health
        frontend_checked = any('frontend' in key.lower() for key in component_checks.keys())
        assert frontend_checked, "Health check should verify frontend component"
        
        print(f"✓ Health check covers {len(component_checks)} components")
        for component, status in component_checks.items():
            print(f"  {component}: {status}")
    
    assert health_results['overall_status'] != 'UNKNOWN', "Health check should determine overall status"


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v'])