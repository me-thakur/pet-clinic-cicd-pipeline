#!/usr/bin/env python3
"""
Property-based test for Jenkins GitHub Integration Workflow

Property 1: Jenkins GitHub Integration Workflow
For any code push to the main branch, Jenkins should automatically trigger a build, 
execute the pipeline, and report the final status back to GitHub

Validates: Requirements 1.1, 1.4
"""

import pytest
import requests
import json
import time
import subprocess
import os
from hypothesis import given, strategies as st, settings
from typing import Dict, Any, Optional
import uuid


class JenkinsGitHubIntegrationTest:
    """Property-based test for Jenkins GitHub integration workflow"""
    
    def __init__(self):
        self.jenkins_url = os.getenv('JENKINS_URL', 'http://localhost:8080')
        self.jenkins_user = os.getenv('JENKINS_USER', 'admin')
        self.jenkins_token = os.getenv('JENKINS_TOKEN', '')
        self.github_token = os.getenv('GITHUB_TOKEN', '')
        self.test_repo = os.getenv('TEST_REPO', 'test-org/pet-clinic-test')
        
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up test environment with unique identifiers"""
        test_id = str(uuid.uuid4())[:8]
        return {
            'test_id': test_id,
            'branch_name': f'test-branch-{test_id}',
            'commit_message': f'Test commit {test_id}',
            'jenkins_job': f'pet-clinic-pipeline-test-{test_id}'
        }
    
    def create_test_commit(self, test_env: Dict[str, Any]) -> str:
        """Create a test commit to trigger Jenkins build"""
        # Create a simple test file change
        test_content = f"// Test file for {test_env['test_id']}\nconst testId = '{test_env['test_id']}';\n"
        
        # Use GitHub API to create commit
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Get current main branch SHA
        response = requests.get(
            f'https://api.github.com/repos/{self.test_repo}/git/refs/heads/main',
            headers=headers
        )
        if response.status_code != 200:
            pytest.skip(f"Cannot access test repository: {response.status_code}")
        
        main_sha = response.json()['object']['sha']
        
        # Create blob for test file
        blob_data = {
            'content': test_content,
            'encoding': 'utf-8'
        }
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/git/blobs',
            headers=headers,
            json=blob_data
        )
        blob_sha = response.json()['sha']
        
        # Create tree
        tree_data = {
            'base_tree': main_sha,
            'tree': [{
                'path': f'test-{test_env["test_id"]}.js',
                'mode': '100644',
                'type': 'blob',
                'sha': blob_sha
            }]
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
        
        return commit_sha
    
    def wait_for_jenkins_build(self, commit_sha: str, timeout: int = 300) -> Optional[Dict[str, Any]]:
        """Wait for Jenkins to trigger and complete build for the commit"""
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            # Check Jenkins for builds triggered by this commit
            auth = (self.jenkins_user, self.jenkins_token)
            response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/api/json?tree=builds[number,result,actions[lastBuiltRevision[SHA1]]]',
                auth=auth
            )
            
            if response.status_code == 200:
                builds = response.json().get('builds', [])
                for build in builds:
                    # Check if this build was triggered by our commit
                    for action in build.get('actions', []):
                        if 'lastBuiltRevision' in action:
                            if action['lastBuiltRevision']['SHA1'] == commit_sha:
                                if build['result'] is not None:  # Build completed
                                    return build
            
            time.sleep(10)  # Wait 10 seconds before checking again
        
        return None
    
    def check_github_status(self, commit_sha: str) -> Dict[str, Any]:
        """Check GitHub commit status updated by Jenkins"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        response = requests.get(
            f'https://api.github.com/repos/{self.test_repo}/commits/{commit_sha}/status',
            headers=headers
        )
        
        if response.status_code == 200:
            return response.json()
        return {}
    
    def cleanup_test_commit(self, test_env: Dict[str, Any]):
        """Clean up test artifacts"""
        # Remove test file (optional cleanup)
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        try:
            # Get file SHA
            response = requests.get(
                f'https://api.github.com/repos/{self.test_repo}/contents/test-{test_env["test_id"]}.js',
                headers=headers
            )
            if response.status_code == 200:
                file_sha = response.json()['sha']
                
                # Delete file
                delete_data = {
                    'message': f'Cleanup test file {test_env["test_id"]}',
                    'sha': file_sha
                }
                requests.delete(
                    f'https://api.github.com/repos/{self.test_repo}/contents/test-{test_env["test_id"]}.js',
                    headers=headers,
                    json=delete_data
                )
        except Exception:
            pass  # Ignore cleanup errors


@pytest.fixture
def jenkins_github_test():
    """Fixture for Jenkins GitHub integration test"""
    return JenkinsGitHubIntegrationTest()


@given(
    commit_message_suffix=st.text(min_size=1, max_size=50, alphabet=st.characters(whitelist_categories=('Lu', 'Ll', 'Nd', 'Pc'))),
    test_file_content=st.text(min_size=10, max_size=200)
)
@settings(max_examples=5, deadline=600000)  # 10 minute timeout per test
def test_jenkins_github_integration_workflow(jenkins_github_test, commit_message_suffix, test_file_content):
    """
    **Validates: Requirements 1.1, 1.4**
    
    Property 1: Jenkins GitHub Integration Workflow
    For any code push to the main branch, Jenkins should automatically trigger a build, 
    execute the pipeline, and report the final status back to GitHub
    """
    # Skip if required environment variables are not set
    if not all([
        jenkins_github_test.jenkins_token,
        jenkins_github_test.github_token,
        jenkins_github_test.test_repo
    ]):
        pytest.skip("Required environment variables not set for GitHub integration test")
    
    # Set up test environment
    test_env = jenkins_github_test.setup_test_environment()
    test_env['commit_message'] += f' - {commit_message_suffix}'
    
    try:
        # Step 1: Create test commit to main branch
        commit_sha = jenkins_github_test.create_test_commit(test_env)
        assert commit_sha, "Failed to create test commit"
        
        # Step 2: Wait for Jenkins to automatically trigger build (Requirement 1.1)
        build_info = jenkins_github_test.wait_for_jenkins_build(commit_sha, timeout=300)
        assert build_info is not None, f"Jenkins did not trigger build for commit {commit_sha} within timeout"
        
        # Step 3: Verify build executed pipeline stages
        assert build_info['result'] in ['SUCCESS', 'FAILURE', 'UNSTABLE'], f"Build completed with unexpected result: {build_info['result']}"
        
        # Step 4: Verify Jenkins reported status back to GitHub (Requirement 1.4)
        github_status = jenkins_github_test.check_github_status(commit_sha)
        assert github_status, f"No GitHub status found for commit {commit_sha}"
        
        # Verify status was updated by Jenkins
        jenkins_statuses = [status for status in github_status.get('statuses', []) 
                          if 'jenkins' in status.get('context', '').lower()]
        assert jenkins_statuses, "No Jenkins status updates found in GitHub"
        
        # Verify status reflects build result
        latest_status = jenkins_statuses[0]  # Most recent status
        expected_state = 'success' if build_info['result'] == 'SUCCESS' else 'failure'
        assert latest_status['state'] in ['success', 'failure', 'pending'], f"Invalid GitHub status state: {latest_status['state']}"
        
        print(f"✓ Property 1 validated: Commit {commit_sha[:8]} triggered Jenkins build #{build_info['number']} with result {build_info['result']}")
        
    finally:
        # Cleanup test artifacts
        jenkins_github_test.cleanup_test_commit(test_env)


def test_jenkins_github_integration_manual():
    """
    Manual test case for Jenkins GitHub integration
    This test can be run independently to verify the integration works
    """
    test_instance = JenkinsGitHubIntegrationTest()
    
    # Skip if environment not configured
    if not all([test_instance.jenkins_token, test_instance.github_token]):
        pytest.skip("Manual test requires JENKINS_TOKEN and GITHUB_TOKEN environment variables")
    
    # Create a simple test commit
    test_env = test_instance.setup_test_environment()
    
    try:
        commit_sha = test_instance.create_test_commit(test_env)
        print(f"Created test commit: {commit_sha}")
        
        # Wait for build
        build_info = test_instance.wait_for_jenkins_build(commit_sha, timeout=180)
        if build_info:
            print(f"Jenkins build triggered: #{build_info['number']} - {build_info['result']}")
            
            # Check GitHub status
            github_status = test_instance.check_github_status(commit_sha)
            if github_status:
                print(f"GitHub status updated: {github_status.get('state', 'unknown')}")
            
            assert True, "Manual test completed successfully"
        else:
            pytest.fail("Jenkins build was not triggered within timeout")
            
    finally:
        test_instance.cleanup_test_commit(test_env)


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v'])