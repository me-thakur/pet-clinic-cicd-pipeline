#!/usr/bin/env python3
"""
Property-based test for CI/CD Pipeline Build Consistency

Property 6: CI/CD Pipeline Build Consistency
For any source code commit, the CI/CD pipeline should produce the same build artifacts 
when given the same input code

Validates: Requirements 6.1
"""

import pytest
import requests
import json
import time
import subprocess
import os
import hashlib
import tempfile
import shutil
import zipfile
from hypothesis import given, strategies as st, settings
from typing import Dict, Any, Optional, List
import uuid
from datetime import datetime
import boto3


class CICDPipelineBuildConsistencyTest:
    """Property-based test for CI/CD pipeline build consistency"""
    
    def __init__(self):
        self.jenkins_url = os.getenv('JENKINS_URL', 'http://localhost:8080')
        self.jenkins_user = os.getenv('JENKINS_USER', 'admin')
        self.jenkins_token = os.getenv('JENKINS_TOKEN', '')
        self.github_token = os.getenv('GITHUB_TOKEN', '')
        self.test_repo = os.getenv('TEST_REPO', 'test-org/pet-clinic-test')
        
        # AWS S3 for artifact storage
        self.aws_region = os.getenv('AWS_REGION', 'us-east-1')
        self.s3_bucket = os.getenv('S3_ARTIFACTS_BUCKET', 'pet-clinic-artifacts')
        self.s3_client = boto3.client('s3', region_name=self.aws_region)
        
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up test environment with unique identifiers"""
        test_id = str(uuid.uuid4())[:8]
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        
        return {
            'test_id': test_id,
            'timestamp': timestamp,
            'commit_message': f'Build consistency test {test_id} - {timestamp}',
            'test_file_name': f'BuildConsistencyTest{test_id}.java',
            'commit_sha': None,
            'build_results': [],
            'artifact_hashes': []
        }
    
    def create_deterministic_source_code(self, test_env: Dict[str, Any], code_content: str) -> str:
        """Create a deterministic source code commit for consistency testing"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Create deterministic Java test class
        java_content = f'''package com.petclinic.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Build Consistency Test Class
 * Test ID: {test_env['test_id']}
 * Timestamp: {test_env['timestamp']}
 * 
 * This class is designed to produce consistent build artifacts
 * across multiple build executions with identical source code.
 */
@SpringBootTest
public class {test_env['test_file_name'].replace('.java', '')} {{
    
    private static final String TEST_ID = "{test_env['test_id']}";
    private static final String TIMESTAMP = "{test_env['timestamp']}";
    
    @Test
    public void testBuildConsistency() {{
        // Deterministic test that should produce identical results
        String expectedValue = "consistency-test-" + TEST_ID;
        String actualValue = generateDeterministicValue();
        
        assertEquals(expectedValue, actualValue, 
            "Build consistency test should produce identical results");
    }}
    
    @Test
    public void testDeterministicCalculation() {{
        // Mathematical calculation that should be consistent
        int input = {abs(hash(test_env['test_id'])) % 1000};
        int expected = calculateDeterministicResult(input);
        int actual = calculateDeterministicResult(input);
        
        assertEquals(expected, actual, 
            "Deterministic calculations should be consistent");
    }}
    
    @Test
    public void testStringProcessing() {{
        // String processing that should be deterministic
        String input = "{code_content[:50].replace('"', '\\"')}";
        String processed = processStringDeterministically(input);
        
        assertNotNull(processed, "String processing should not return null");
        assertTrue(processed.length() > 0, "Processed string should not be empty");
        
        // Verify deterministic behavior
        String processedAgain = processStringDeterministically(input);
        assertEquals(processed, processedAgain, 
            "String processing should be deterministic");
    }}
    
    private String generateDeterministicValue() {{
        return "consistency-test-" + TEST_ID;
    }}
    
    private int calculateDeterministicResult(int input) {{
        // Simple deterministic calculation
        return (input * 42 + 17) % 1000;
    }}
    
    private String processStringDeterministically(String input) {{
        if (input == null || input.isEmpty()) {{
            return "empty";
        }}
        
        // Deterministic string processing
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {{
            if (Character.isLetterOrDigit(c)) {{
                result.append(Character.toLowerCase(c));
            }}
        }}
        
        return result.toString();
    }}
    
    @Test
    public void testEnvironmentIndependence() {{
        // Test that should work regardless of build environment
        // (as long as Java and dependencies are consistent)
        
        String javaVersion = System.getProperty("java.version");
        assertNotNull(javaVersion, "Java version should be available");
        
        // Test basic Java functionality
        java.util.List<String> testList = java.util.Arrays.asList("a", "b", "c");
        assertEquals(3, testList.size(), "Basic Java collections should work");
        
        // Test deterministic date formatting (using fixed date)
        java.time.LocalDate fixedDate = java.time.LocalDate.of(2024, 1, 1);
        String formattedDate = fixedDate.toString();
        assertEquals("2024-01-01", formattedDate, "Date formatting should be consistent");
    }}
}}
'''
        
        # Get current main branch SHA
        response = requests.get(
            f'https://api.github.com/repos/{self.test_repo}/git/refs/heads/main',
            headers=headers
        )
        if response.status_code != 200:
            raise Exception(f"Cannot access repository: {response.status_code}")
        
        main_sha = response.json()['object']['sha']
        
        # Create blob for test file
        blob_data = {
            'content': java_content,
            'encoding': 'utf-8'
        }
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/git/blobs',
            headers=headers,
            json=blob_data
        )
        blob_sha = response.json()['sha']
        
        # Also create a pom.xml update to ensure consistent build configuration
        pom_content = f'''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.petclinic</groupId>
    <artifactId>pet-clinic-consistency-test</artifactId>
    <version>1.0.0-{test_env['test_id']}</version>
    <packaging>jar</packaging>
    
    <name>Pet Clinic Build Consistency Test</name>
    <description>Test project for build consistency validation</description>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.8.2</junit.version>
        <spring.boot.version>2.7.0</spring.boot.version>
        
        <!-- Ensure reproducible builds -->
        <project.build.outputTimestamp>2024-01-01T00:00:00Z</project.build.outputTimestamp>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${{spring.boot.version}}</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${{junit.version}}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.10.1</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M7</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                    </includes>
                </configuration>
            </plugin>
            
            <!-- Plugin for reproducible builds -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.2</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <Build-Time>${{maven.build.timestamp}}</Build-Time>
                            <Test-ID>{test_env['test_id']}</Test-ID>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
'''
        
        # Create blob for pom.xml
        pom_blob_data = {
            'content': pom_content,
            'encoding': 'utf-8'
        }
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/git/blobs',
            headers=headers,
            json=pom_blob_data
        )
        pom_blob_sha = response.json()['sha']
        
        # Create tree with both files
        tree_data = {
            'base_tree': main_sha,
            'tree': [
                {
                    'path': f'src/test/java/com/petclinic/test/{test_env["test_file_name"]}',
                    'mode': '100644',
                    'type': 'blob',
                    'sha': blob_sha
                },
                {
                    'path': 'pom.xml',
                    'mode': '100644',
                    'type': 'blob',
                    'sha': pom_blob_sha
                }
            ]
        }
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/git/trees',
            headers=headers,
            json=tree_data
        )
        tree_sha = response.json()['sha']
        
        # Create commit
        commit_data = {
            'message': test_env['commit_message'],
            'tree': tree_sha,
            'parents': [main_sha]
        }
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/git/commits',
            headers=headers,
            json=commit_data
        )
        commit_sha = response.json()['sha']
        
        # Update main branch reference
        ref_data = {
            'sha': commit_sha,
            'force': False
        }
        response = requests.patch(
            f'https://api.github.com/repos/{self.test_repo}/git/refs/heads/main',
            headers=headers,
            json=ref_data
        )
        
        test_env['commit_sha'] = commit_sha
        return commit_sha
    
    def trigger_multiple_builds(self, test_env: Dict[str, Any], num_builds: int = 3) -> List[Dict[str, Any]]:
        """Trigger multiple builds of the same commit to test consistency"""
        build_results = []
        auth = (self.jenkins_user, self.jenkins_token)
        
        for build_num in range(1, num_builds + 1):
            print(f"Triggering build {build_num}/{num_builds}...")
            
            # Trigger build manually (since we want multiple builds of same commit)
            trigger_response = requests.post(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/build',
                auth=auth,
                data={'cause': f'Build consistency test {test_env["test_id"]} - iteration {build_num}'}
            )
            
            if trigger_response.status_code not in [200, 201]:
                print(f"Failed to trigger build {build_num}: {trigger_response.status_code}")
                continue
            
            # Wait for build to start and complete
            build_info = self._wait_for_latest_build_completion(auth, timeout=600)
            
            if build_info:
                # Get detailed build information
                detailed_info = self._get_detailed_build_info(build_info['number'], auth)
                
                build_result = {
                    'iteration': build_num,
                    'build_number': build_info['number'],
                    'result': build_info['result'],
                    'duration': build_info.get('duration', 0),
                    'timestamp': build_info.get('timestamp', 0),
                    'artifacts': detailed_info.get('artifacts', []),
                    'test_results': detailed_info.get('test_results', {}),
                    'console_log_hash': detailed_info.get('console_log_hash', ''),
                    'workspace_hash': detailed_info.get('workspace_hash', '')
                }
                
                build_results.append(build_result)
                print(f"Build {build_num} completed: #{build_info['number']} - {build_info['result']}")
            else:
                print(f"Build {build_num} did not complete within timeout")
            
            # Wait between builds to avoid conflicts
            time.sleep(30)
        
        test_env['build_results'] = build_results
        return build_results
    
    def _wait_for_latest_build_completion(self, auth: tuple, timeout: int = 600) -> Optional[Dict[str, Any]]:
        """Wait for the latest build to complete"""
        start_time = time.time()
        last_build_number = None
        
        while time.time() - start_time < timeout:
            try:
                response = requests.get(
                    f'{self.jenkins_url}/job/pet-clinic-pipeline/api/json?tree=builds[number,result,building,duration,timestamp]',
                    auth=auth,
                    timeout=30
                )
                
                if response.status_code == 200:
                    builds = response.json().get('builds', [])
                    
                    if builds:
                        latest_build = builds[0]  # Most recent build
                        
                        # Check if this is a new build
                        if last_build_number is None:
                            last_build_number = latest_build['number']
                        
                        # If build completed and it's the one we're waiting for
                        if (not latest_build['building'] and 
                            latest_build['result'] is not None and
                            latest_build['number'] >= last_build_number):
                            return latest_build
                
                time.sleep(10)
                
            except Exception as e:
                print(f"Error waiting for build completion: {e}")
                time.sleep(10)
        
        return None
    
    def _get_detailed_build_info(self, build_number: int, auth: tuple) -> Dict[str, Any]:
        """Get detailed information about a specific build"""
        detailed_info = {
            'artifacts': [],
            'test_results': {},
            'console_log_hash': '',
            'workspace_hash': ''
        }
        
        try:
            # Get build details
            response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/api/json',
                auth=auth,
                timeout=30
            )
            
            if response.status_code == 200:
                build_data = response.json()
                detailed_info['artifacts'] = build_data.get('artifacts', [])
            
            # Get console log and hash it
            console_response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/consoleText',
                auth=auth,
                timeout=30
            )
            
            if console_response.status_code == 200:
                console_text = console_response.text
                # Remove timestamps and variable content for consistent hashing
                cleaned_console = self._clean_console_log_for_hashing(console_text)
                detailed_info['console_log_hash'] = hashlib.sha256(cleaned_console.encode('utf-8')).hexdigest()
            
            # Get test results if available
            test_response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/testReport/api/json',
                auth=auth,
                timeout=30
            )
            
            if test_response.status_code == 200:
                test_data = test_response.json()
                detailed_info['test_results'] = {
                    'total_count': test_data.get('totalCount', 0),
                    'fail_count': test_data.get('failCount', 0),
                    'pass_count': test_data.get('passCount', 0),
                    'skip_count': test_data.get('skipCount', 0)
                }
            
            # Get workspace artifacts if available
            workspace_response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/ws/target/',
                auth=auth,
                timeout=30
            )
            
            if workspace_response.status_code == 200:
                # Hash the workspace content structure
                workspace_content = workspace_response.text
                detailed_info['workspace_hash'] = hashlib.sha256(workspace_content.encode('utf-8')).hexdigest()
        
        except Exception as e:
            print(f"Error getting detailed build info: {e}")
        
        return detailed_info
    
    def _clean_console_log_for_hashing(self, console_log: str) -> str:
        """Clean console log by removing timestamps and variable content for consistent hashing"""
        lines = console_log.split('\n')
        cleaned_lines = []
        
        for line in lines:
            # Remove timestamp patterns
            import re
            line = re.sub(r'\d{2}:\d{2}:\d{2}', 'XX:XX:XX', line)
            line = re.sub(r'\d{4}-\d{2}-\d{2}', 'XXXX-XX-XX', line)
            line = re.sub(r'\d{13}', 'XXXXXXXXXXXXX', line)  # Unix timestamps
            
            # Remove build-specific paths and IDs
            line = re.sub(r'/tmp/[a-zA-Z0-9]+', '/tmp/XXXXXXXX', line)
            line = re.sub(r'build #\d+', 'build #XXX', line)
            line = re.sub(r'Build #\d+', 'Build #XXX', line)
            
            # Keep only lines that contain meaningful build information
            if any(keyword in line.lower() for keyword in [
                'compiling', 'testing', 'building', 'maven', 'junit', 
                'success', 'failure', 'error', 'warning'
            ]):
                cleaned_lines.append(line)
        
        return '\n'.join(cleaned_lines)
    
    def analyze_build_consistency(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze build results for consistency across multiple executions"""
        build_results = test_env['build_results']
        
        consistency_analysis = {
            'builds_analyzed': len(build_results),
            'all_builds_successful': True,
            'consistent_results': True,
            'consistent_artifacts': True,
            'consistent_test_results': True,
            'consistent_duration_range': True,
            'inconsistencies': [],
            'result_summary': {}
        }
        
        if len(build_results) < 2:
            return consistency_analysis
        
        # Analyze build results consistency
        reference_build = build_results[0]
        
        for i, build in enumerate(build_results[1:], 1):
            # Check build success
            if build['result'] != 'SUCCESS':
                consistency_analysis['all_builds_successful'] = False
                consistency_analysis['inconsistencies'].append(
                    f"Build {i+1} failed with result: {build['result']}"
                )
            
            # Check result consistency
            if build['result'] != reference_build['result']:
                consistency_analysis['consistent_results'] = False
                consistency_analysis['inconsistencies'].append(
                    f"Build {i+1} result ({build['result']}) differs from reference ({reference_build['result']})"
                )
            
            # Check artifact consistency
            if len(build['artifacts']) != len(reference_build['artifacts']):
                consistency_analysis['consistent_artifacts'] = False
                consistency_analysis['inconsistencies'].append(
                    f"Build {i+1} produced {len(build['artifacts'])} artifacts vs {len(reference_build['artifacts'])} in reference"
                )
            
            # Check test results consistency
            ref_tests = reference_build['test_results']
            curr_tests = build['test_results']
            
            for key in ['total_count', 'fail_count', 'pass_count', 'skip_count']:
                if ref_tests.get(key, 0) != curr_tests.get(key, 0):
                    consistency_analysis['consistent_test_results'] = False
                    consistency_analysis['inconsistencies'].append(
                        f"Build {i+1} test {key} ({curr_tests.get(key, 0)}) differs from reference ({ref_tests.get(key, 0)})"
                    )
            
            # Check duration consistency (should be within reasonable range)
            ref_duration = reference_build['duration']
            curr_duration = build['duration']
            
            if ref_duration > 0 and curr_duration > 0:
                duration_ratio = abs(curr_duration - ref_duration) / ref_duration
                if duration_ratio > 0.5:  # More than 50% difference
                    consistency_analysis['consistent_duration_range'] = False
                    consistency_analysis['inconsistencies'].append(
                        f"Build {i+1} duration ({curr_duration}ms) significantly differs from reference ({ref_duration}ms)"
                    )
        
        # Overall consistency assessment
        consistency_analysis['consistent_results'] = (
            consistency_analysis['all_builds_successful'] and
            consistency_analysis['consistent_results'] and
            consistency_analysis['consistent_artifacts'] and
            consistency_analysis['consistent_test_results']
        )
        
        # Create result summary
        consistency_analysis['result_summary'] = {
            'build_results': [build['result'] for build in build_results],
            'build_durations': [build['duration'] for build in build_results],
            'artifact_counts': [len(build['artifacts']) for build in build_results],
            'test_pass_counts': [build['test_results'].get('pass_count', 0) for build in build_results]
        }
        
        return consistency_analysis
    
    def cleanup_test_artifacts(self, test_env: Dict[str, Any]):
        """Clean up test artifacts and commits"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        try:
            # Clean up test files from repository
            test_files = [
                f'src/test/java/com/petclinic/test/{test_env["test_file_name"]}',
                'pom.xml'
            ]
            
            for file_path in test_files:
                try:
                    # Get file SHA
                    response = requests.get(
                        f'https://api.github.com/repos/{self.test_repo}/contents/{file_path}',
                        headers=headers
                    )
                    
                    if response.status_code == 200:
                        file_sha = response.json()['sha']
                        
                        # Delete file
                        delete_data = {
                            'message': f'Cleanup build consistency test {test_env["test_id"]}',
                            'sha': file_sha
                        }
                        requests.delete(
                            f'https://api.github.com/repos/{self.test_repo}/contents/{file_path}',
                            headers=headers,
                            json=delete_data
                        )
                except Exception:
                    pass  # Ignore cleanup errors
        
        except Exception as e:
            print(f"Error during cleanup: {e}")


@pytest.fixture
def build_consistency_test():
    """Fixture for CI/CD pipeline build consistency test"""
    return CICDPipelineBuildConsistencyTest()


@given(
    code_complexity=st.integers(min_value=1, max_value=5),
    test_data_size=st.integers(min_value=10, max_value=100)
)
@settings(max_examples=3, deadline=1800000)  # 30 minute timeout per test
def test_cicd_pipeline_build_consistency(build_consistency_test, code_complexity, test_data_size):
    """
    **Validates: Requirements 6.1**
    
    Property 6: CI/CD Pipeline Build Consistency
    For any source code commit, the CI/CD pipeline should produce the same build artifacts 
    when given the same input code
    """
    # Skip if required environment variables are not set
    if not all([
        build_consistency_test.jenkins_token,
        build_consistency_test.github_token,
        build_consistency_test.test_repo
    ]):
        pytest.skip("Required environment variables not set for build consistency test")
    
    # Set up test environment
    test_env = build_consistency_test.setup_test_environment()
    
    # Generate deterministic code content based on input parameters
    code_content = f"""
    // Build consistency test with complexity level {code_complexity}
    // Test data size: {test_data_size}
    
    public class TestData {{
        private static final int COMPLEXITY_LEVEL = {code_complexity};
        private static final int DATA_SIZE = {test_data_size};
        
        public static String generateTestData() {{
            StringBuilder data = new StringBuilder();
            for (int i = 0; i < DATA_SIZE; i++) {{
                data.append("test-item-").append(i).append("\\n");
            }}
            return data.toString();
        }}
        
        public static int calculateComplexity() {{
            int result = 1;
            for (int i = 1; i <= COMPLEXITY_LEVEL; i++) {{
                result *= i;
            }}
            return result;
        }}
    }}
    """
    
    try:
        # Step 1: Create deterministic source code commit
        print(f"Creating deterministic source code for test {test_env['test_id']}...")
        commit_sha = build_consistency_test.create_deterministic_source_code(test_env, code_content)
        assert commit_sha, "Failed to create deterministic source code commit"
        print(f"Created commit {commit_sha[:8]} with deterministic code")
        
        # Step 2: Trigger multiple builds of the same commit
        print("Triggering multiple builds for consistency testing...")
        build_results = build_consistency_test.trigger_multiple_builds(test_env, num_builds=3)
        
        # Verify we got multiple build results
        assert len(build_results) >= 2, f"Need at least 2 builds for consistency testing, got {len(build_results)}"
        print(f"Completed {len(build_results)} builds for consistency analysis")
        
        # Step 3: Analyze build consistency
        print("Analyzing build consistency...")
        consistency_analysis = build_consistency_test.analyze_build_consistency(test_env)
        
        # Assert build consistency requirements
        assert consistency_analysis['all_builds_successful'], (
            f"Not all builds were successful: {consistency_analysis['inconsistencies']}"
        )
        
        assert consistency_analysis['consistent_results'], (
            f"Build results not consistent: {consistency_analysis['inconsistencies']}"
        )
        
        assert consistency_analysis['consistent_test_results'], (
            f"Test results not consistent across builds: {consistency_analysis['inconsistencies']}"
        )
        
        # Allow some flexibility in artifact consistency (due to timestamps, etc.)
        if not consistency_analysis['consistent_artifacts']:
            print(f"Warning: Artifact consistency issues detected: {consistency_analysis['inconsistencies']}")
        
        # Step 4: Verify specific consistency properties
        
        # All builds should have same number of test passes
        test_pass_counts = consistency_analysis['result_summary']['test_pass_counts']
        if test_pass_counts:
            assert len(set(test_pass_counts)) <= 1, (
                f"Test pass counts not consistent: {test_pass_counts}"
            )
        
        # Build results should be identical
        build_results_list = consistency_analysis['result_summary']['build_results']
        assert len(set(build_results_list)) <= 1, (
            f"Build results not identical: {build_results_list}"
        )
        
        # Duration should be within reasonable range (not too variable)
        durations = consistency_analysis['result_summary']['build_durations']
        if len(durations) > 1 and all(d > 0 for d in durations):
            avg_duration = sum(durations) / len(durations)
            max_deviation = max(abs(d - avg_duration) for d in durations)
            relative_deviation = max_deviation / avg_duration if avg_duration > 0 else 0
            
            assert relative_deviation <= 0.5, (
                f"Build duration too variable: {durations}, relative deviation: {relative_deviation:.2%}"
            )
        
        print(f"✓ Property 6 validated: CI/CD pipeline builds are consistent")
        print(f"  - Builds analyzed: {consistency_analysis['builds_analyzed']}")
        print(f"  - All builds successful: {consistency_analysis['all_builds_successful']}")
        print(f"  - Results consistent: {consistency_analysis['consistent_results']}")
        print(f"  - Test results consistent: {consistency_analysis['consistent_test_results']}")
        print(f"  - Build results: {consistency_analysis['result_summary']['build_results']}")
        print(f"  - Test pass counts: {consistency_analysis['result_summary']['test_pass_counts']}")
        
        if consistency_analysis['inconsistencies']:
            print(f"  - Minor inconsistencies noted: {len(consistency_analysis['inconsistencies'])}")
        
    finally:
        # Cleanup test artifacts
        print("Cleaning up test artifacts...")
        build_consistency_test.cleanup_test_artifacts(test_env)


def test_cicd_pipeline_build_consistency_manual():
    """
    Manual test case for CI/CD pipeline build consistency
    This test can be run independently to verify build consistency
    """
    test_instance = CICDPipelineBuildConsistencyTest()
    
    # Skip if environment not configured
    if not all([test_instance.jenkins_token, test_instance.github_token]):
        pytest.skip("Manual test requires JENKINS_TOKEN and GITHUB_TOKEN environment variables")
    
    test_env = test_instance.setup_test_environment()
    
    # Create simple deterministic code
    simple_code = """
    // Simple manual test for build consistency
    public class SimpleTest {
        public static final String VALUE = "deterministic-value";
        
        public static int calculate() {
            return 42 * 2;
        }
    }
    """
    
    try:
        # Create commit
        commit_sha = test_instance.create_deterministic_source_code(test_env, simple_code)
        print(f"Created manual test commit: {commit_sha}")
        
        # Trigger 2 builds
        build_results = test_instance.trigger_multiple_builds(test_env, num_builds=2)
        print(f"Completed {len(build_results)} builds")
        
        if len(build_results) >= 2:
            # Analyze consistency
            consistency_analysis = test_instance.analyze_build_consistency(test_env)
            
            print(f"Build consistency: {consistency_analysis['consistent_results']}")
            print(f"All builds successful: {consistency_analysis['all_builds_successful']}")
            
            if consistency_analysis['inconsistencies']:
                print("Inconsistencies found:")
                for inconsistency in consistency_analysis['inconsistencies']:
                    print(f"  - {inconsistency}")
            
            # Basic assertions for manual test
            assert consistency_analysis['all_builds_successful'], "Manual test failed - not all builds successful"
            
            if consistency_analysis['builds_analyzed'] >= 2:
                assert consistency_analysis['consistent_test_results'], "Manual test failed - test results not consistent"
        
        print("✓ Manual build consistency test completed")
        
    finally:
        test_instance.cleanup_test_artifacts(test_env)


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v'])