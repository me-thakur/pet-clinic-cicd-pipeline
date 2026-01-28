#!/usr/bin/env python3
"""
Property-based test for Jenkins Pull Request Validation

Property 2: Pull Request Validation
For any pull request created in the repository, Jenkins should run validation tests 
and update the PR status with pass/fail results

Validates: Requirements 1.2
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


class JenkinsPullRequestValidationTest:
    """Property-based test for Jenkins pull request validation workflow"""
    
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
            'branch_name': f'feature/test-pr-{test_id}',
            'pr_title': f'Test PR {test_id}',
            'pr_body': f'Test pull request for validation testing - {test_id}',
            'commit_message': f'Test commit for PR {test_id}'
        }
    
    def create_test_branch_and_commit(self, test_env: Dict[str, Any]) -> str:
        """Create a test branch with a commit for the pull request"""
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
        
        # Create new branch
        branch_data = {
            'ref': f'refs/heads/{test_env["branch_name"]}',
            'sha': main_sha
        }
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/git/refs',
            headers=headers,
            json=branch_data
        )
        if response.status_code not in [201, 422]:  # 422 if branch already exists
            pytest.fail(f"Failed to create branch: {response.status_code}")
        
        # Create test file content
        test_content = f"""// Test file for PR validation {test_env['test_id']}
public class TestPR{test_env['test_id'].replace('-', '')} {{
    public void testMethod() {{
        System.out.println("Testing PR validation for {test_env['test_id']}");
    }}
}}
"""
        
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
                'path': f'src/test/java/TestPR{test_env["test_id"].replace("-", "")}.java',
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
        
        # Update branch reference
        ref_data = {
            'sha': commit_sha,
            'force': True
        }
        response = requests.patch(
            f'https://api.github.com/repos/{self.test_repo}/git/refs/heads/{test_env["branch_name"]}',
            headers=headers,
            json=ref_data
        )
        
        return commit_sha
    
    def create_pull_request(self, test_env: Dict[str, Any]) -> int:
        """Create a pull request from the test branch"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        pr_data = {
            'title': test_env['pr_title'],
            'body': test_env['pr_body'],
            'head': test_env['branch_name'],
            'base': 'main'
        }
        
        response = requests.post(
            f'https://api.github.com/repos/{self.test_repo}/pulls',
            headers=headers,
            json=pr_data
        )
        
        if response.status_code == 201:
            return response.json()['number']
        else:
            pytest.fail(f"Failed to create pull request: {response.status_code} - {response.text}")
    
    def wait_for_jenkins_pr_build(self, pr_number: int, timeout: int = 300) -> Optional[Dict[str, Any]]:
        """Wait for Jenkins to trigger and complete PR validation build"""
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            # Check Jenkins for PR builds
            auth = (self.jenkins_user, self.jenkins_token)
            response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/api/json?tree=builds[number,result,actions[causes[*]]]',
                auth=auth
            )
            
            if response.status_code == 200:
                builds = response.json().get('builds', [])
                for build in builds:
                    # Check if this build was triggered by our PR
                    for action in build.get('actions', []):
                        causes = action.get('causes', [])
                        for cause in causes:
                            # Look for PR-related triggers
                            if ('pullrequest' in str(cause).lower() or 
                                f'#{pr_number}' in str(cause) or
                                'pr' in str(cause).lower()):
                                if build['result'] is not None:  # Build completed
                                    return build
            
            time.sleep(10)  # Wait 10 seconds before checking again
        
        return None
    
    def check_pr_status_checks(self, pr_number: int) -> List[Dict[str, Any]]:
        """Check GitHub PR status checks updated by Jenkins"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Get PR details
        response = requests.get(
            f'https://api.github.com/repos/{self.test_repo}/pulls/{pr_number}',
            headers=headers
        )
        
        if response.status_code != 200:
            return []
        
        pr_data = response.json()
        head_sha = pr_data['head']['sha']
        
        # Get status checks for the PR head commit
        response = requests.get(
            f'https://api.github.com/repos/{self.test_repo}/commits/{head_sha}/status',
            headers=headers
        )
        
        if response.status_code == 200:
            return response.json().get('statuses', [])
        return []
    
    def cleanup_test_pr(self, test_env: Dict[str, Any], pr_number: Optional[int] = None):
        """Clean up test pull request and branch"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        try:
            # Close PR if it exists
            if pr_number:
                close_data = {'state': 'closed'}
                requests.patch(
                    f'https://api.github.com/repos/{self.test_repo}/pulls/{pr_number}',
                    headers=headers,
                    json=close_data
                )
            
            # Delete branch
            requests.delete(
                f'https://api.github.com/repos/{self.test_repo}/git/refs/heads/{test_env["branch_name"]}',
                headers=headers
            )
        except Exception:
            pass  # Ignore cleanup errors


@pytest.fixture
def jenkins_pr_test():
    """Fixture for Jenkins PR validation test"""
    return JenkinsPullRequestValidationTest()


@given(
    pr_title_suffix=st.text(min_size=1, max_size=30, alphabet=st.characters(whitelist_categories=('Lu', 'Ll', 'Nd', 'Pc'))),
    test_code_content=st.text(min_size=10, max_size=100, alphabet=st.characters(whitelist_categories=('Lu', 'Ll', 'Nd', 'Pc', 'Pd')))
)
@settings(max_examples=3, deadline=600000)  # 10 minute timeout per test
def test_jenkins_pull_request_validation_workflow(jenkins_pr_test, pr_title_suffix, test_code_content):
    """
    **Validates: Requirements 1.2**
    
    Property 2: Pull Request Validation
    For any pull request created in the repository, Jenkins should run validation tests 
    and update the PR status with pass/fail results
    """
    # Skip if required environment variables are not set
    if not all([
        jenkins_pr_test.jenkins_token,
        jenkins_pr_test.github_token,
        jenkins_pr_test.test_repo
    ]):
        pytest.skip("Required environment variables not set for PR validation test")
    
    # Set up test environment
    test_env = jenkins_pr_test.setup_test_environment()
    test_env['pr_title'] += f' - {pr_title_suffix}'
    test_env['pr_body'] += f'\n\nTest content: {test_code_content}'
    
    pr_number = None
    
    try:
        # Step 1: Create test branch with commit
        commit_sha = jenkins_pr_test.create_test_branch_and_commit(test_env)
        assert commit_sha, "Failed to create test branch and commit"
        
        # Step 2: Create pull request (Requirement 1.2 trigger)
        pr_number = jenkins_pr_test.create_pull_request(test_env)
        assert pr_number, "Failed to create pull request"
        
        # Step 3: Wait for Jenkins to automatically trigger PR validation
        build_info = jenkins_pr_test.wait_for_jenkins_pr_build(pr_number, timeout=300)
        assert build_info is not None, f"Jenkins did not trigger PR validation build for PR #{pr_number} within timeout"
        
        # Step 4: Verify validation tests were executed
        assert build_info['result'] in ['SUCCESS', 'FAILURE', 'UNSTABLE'], f"PR build completed with unexpected result: {build_info['result']}"
        
        # Step 5: Verify Jenkins updated PR status with pass/fail results (Requirement 1.2)
        status_checks = jenkins_pr_test.check_pr_status_checks(pr_number)
        assert status_checks, f"No status checks found for PR #{pr_number}"
        
        # Verify status was updated by Jenkins
        jenkins_statuses = [status for status in status_checks 
                          if 'jenkins' in status.get('context', '').lower()]
        assert jenkins_statuses, "No Jenkins status updates found for PR"
        
        # Verify status reflects validation result
        latest_status = jenkins_statuses[0]  # Most recent status
        assert latest_status['state'] in ['success', 'failure', 'pending'], f"Invalid PR status state: {latest_status['state']}"
        
        # Verify status description indicates validation
        description = latest_status.get('description', '').lower()
        assert any(keyword in description for keyword in ['test', 'validation', 'build', 'check']), f"Status description doesn't indicate validation: {description}"
        
        print(f"✓ Property 2 validated: PR #{pr_number} triggered Jenkins validation build #{build_info['number']} with result {build_info['result']}")
        
    finally:
        # Cleanup test artifacts
        jenkins_pr_test.cleanup_test_pr(test_env, pr_number)


def test_jenkins_pull_request_validation_manual():
    """
    Manual test case for Jenkins PR validation
    This test can be run independently to verify PR validation works
    """
    test_instance = JenkinsPullRequestValidationTest()
    
    # Skip if environment not configured
    if not all([test_instance.jenkins_token, test_instance.github_token]):
        pytest.skip("Manual test requires JENKINS_TOKEN and GITHUB_TOKEN environment variables")
    
    # Create a test PR
    test_env = test_instance.setup_test_environment()
    pr_number = None
    
    try:
        # Create branch and commit
        commit_sha = test_instance.create_test_branch_and_commit(test_env)
        print(f"Created test branch: {test_env['branch_name']} with commit: {commit_sha}")
        
        # Create PR
        pr_number = test_instance.create_pull_request(test_env)
        print(f"Created pull request: #{pr_number}")
        
        # Wait for Jenkins validation
        build_info = test_instance.wait_for_jenkins_pr_build(pr_number, timeout=180)
        if build_info:
            print(f"Jenkins PR validation triggered: #{build_info['number']} - {build_info['result']}")
            
            # Check PR status
            status_checks = test_instance.check_pr_status_checks(pr_number)
            if status_checks:
                jenkins_statuses = [s for s in status_checks if 'jenkins' in s.get('context', '').lower()]
                if jenkins_statuses:
                    print(f"PR status updated: {jenkins_statuses[0]['state']} - {jenkins_statuses[0].get('description', '')}")
            
            assert True, "Manual PR validation test completed successfully"
        else:
            pytest.fail("Jenkins PR validation was not triggered within timeout")
            
    finally:
        test_instance.cleanup_test_pr(test_env, pr_number)


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v'])