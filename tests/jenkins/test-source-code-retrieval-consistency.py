#!/usr/bin/env python3
"""
Property-based test for Source Code Retrieval Consistency

Property 3: Source Code Retrieval Consistency
For any build execution, Jenkins should clone the exact source code from the repository 
that corresponds to the triggering commit

Validates: Requirements 1.5
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
from hypothesis import given, strategies as st, settings
from typing import Dict, Any, Optional, List
import uuid
import boto3
from datetime import datetime


class SourceCodeRetrievalConsistencyTest:
    """Property-based test for source code retrieval consistency"""
    
    def __init__(self):
        self.jenkins_url = os.getenv('JENKINS_URL', 'http://localhost:8080')
        self.jenkins_user = os.getenv('JENKINS_USER', 'admin')
        self.jenkins_token = os.getenv('JENKINS_TOKEN', '')
        self.github_token = os.getenv('GITHUB_TOKEN', '')
        self.test_repo = os.getenv('TEST_REPO', 'test-org/pet-clinic-test')
        
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up test environment with unique identifiers"""
        test_id = str(uuid.uuid4())[:8]
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        
        return {
            'test_id': test_id,
            'timestamp': timestamp,
            'commit_message': f'Source consistency test {test_id} - {timestamp}',
            'test_file_name': f'consistency-test-{test_id}.txt',
            'test_content_hash': None,
            'commit_sha': None
        }
    
    def create_test_commit_with_content(self, test_env: Dict[str, Any], file_content: str) -> str:
        """Create a test commit with specific content and return commit SHA"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Calculate content hash for verification
        content_hash = hashlib.sha256(file_content.encode('utf-8')).hexdigest()
        test_env['test_content_hash'] = content_hash
        
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
            'content': file_content,
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
                'path': test_env['test_file_name'],
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
        
        test_env['commit_sha'] = commit_sha
        return commit_sha
    
    def wait_for_jenkins_build_completion(self, commit_sha: str, timeout: int = 300) -> Optional[Dict[str, Any]]:
        """Wait for Jenkins build to complete and return build information"""
        start_time = time.time()
        auth = (self.jenkins_user, self.jenkins_token)
        
        while time.time() - start_time < timeout:
            try:
                response = requests.get(
                    f'{self.jenkins_url}/job/pet-clinic-pipeline/api/json?tree=builds[number,result,building,actions[lastBuiltRevision[SHA1]]]',
                    auth=auth,
                    timeout=30
                )
                
                if response.status_code == 200:
                    builds = response.json().get('builds', [])
                    
                    for build in builds:
                        # Find build for our commit
                        for action in build.get('actions', []):
                            if 'lastBuiltRevision' in action:
                                if action['lastBuiltRevision']['SHA1'] == commit_sha:
                                    if not build['building'] and build['result'] is not None:
                                        return build
                
                time.sleep(10)
                
            except Exception as e:
                print(f"Error waiting for build: {e}")
                time.sleep(10)
        
        return None
    
    def verify_jenkins_workspace_content(self, build_number: int, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Verify that Jenkins workspace contains the exact content from the commit"""
        auth = (self.jenkins_user, self.jenkins_token)
        verification_result = {
            'workspace_accessible': False,
            'test_file_exists': False,
            'content_matches': False,
            'commit_sha_matches': False,
            'retrieved_content_hash': None,
            'workspace_commit_sha': None
        }
        
        try:
            # Get workspace content via Jenkins API
            workspace_response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/ws/{test_env["test_file_name"]}/*view*/',
                auth=auth,
                timeout=30
            )
            
            if workspace_response.status_code == 200:
                verification_result['workspace_accessible'] = True
                verification_result['test_file_exists'] = True
                
                # Get the actual file content
                file_content_response = requests.get(
                    f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/ws/{test_env["test_file_name"]}',
                    auth=auth,
                    timeout=30
                )
                
                if file_content_response.status_code == 200:
                    retrieved_content = file_content_response.text
                    retrieved_hash = hashlib.sha256(retrieved_content.encode('utf-8')).hexdigest()
                    verification_result['retrieved_content_hash'] = retrieved_hash
                    
                    # Compare content hashes
                    verification_result['content_matches'] = (
                        retrieved_hash == test_env['test_content_hash']
                    )
            
            # Get build details to verify commit SHA
            build_details_response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/api/json',
                auth=auth,
                timeout=30
            )
            
            if build_details_response.status_code == 200:
                build_data = build_details_response.json()
                
                # Extract commit SHA from build actions
                for action in build_data.get('actions', []):
                    if 'lastBuiltRevision' in action:
                        workspace_commit_sha = action['lastBuiltRevision']['SHA1']
                        verification_result['workspace_commit_sha'] = workspace_commit_sha
                        verification_result['commit_sha_matches'] = (
                            workspace_commit_sha == test_env['commit_sha']
                        )
                        break
        
        except Exception as e:
            print(f"Error verifying workspace content: {e}")
        
        return verification_result
    
    def verify_git_repository_state(self, build_number: int, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Verify the git repository state in Jenkins workspace matches the expected commit"""
        auth = (self.jenkins_user, self.jenkins_token)
        git_verification = {
            'git_log_accessible': False,
            'commit_in_history': False,
            'head_points_to_commit': False,
            'working_directory_clean': False
        }
        
        try:
            # Execute git commands in Jenkins workspace via build console
            # This is a simulation - in practice, you'd need to access the actual workspace
            
            # Get build console output to check git operations
            console_response = requests.get(
                f'{self.jenkins_url}/job/pet-clinic-pipeline/{build_number}/consoleText',
                auth=auth,
                timeout=30
            )
            
            if console_response.status_code == 200:
                console_output = console_response.text
                
                # Look for git checkout operations in console output
                if f'Checking out Revision {test_env["commit_sha"]}' in console_output:
                    git_verification['git_log_accessible'] = True
                    git_verification['commit_in_history'] = True
                    git_verification['head_points_to_commit'] = True
                
                # Look for clean checkout indicators
                if 'Finished: SUCCESS' in console_output and 'ERROR' not in console_output:
                    git_verification['working_directory_clean'] = True
        
        except Exception as e:
            print(f"Error verifying git repository state: {e}")
        
        return git_verification
    
    def cleanup_test_commit(self, test_env: Dict[str, Any]):
        """Clean up test commit and artifacts"""
        headers = {
            'Authorization': f'token {self.github_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        try:
            # Get file SHA for deletion
            response = requests.get(
                f'https://api.github.com/repos/{self.test_repo}/contents/{test_env["test_file_name"]}',
                headers=headers
            )
            
            if response.status_code == 200:
                file_sha = response.json()['sha']
                
                # Delete test file
                delete_data = {
                    'message': f'Cleanup consistency test {test_env["test_id"]}',
                    'sha': file_sha
                }
                requests.delete(
                    f'https://api.github.com/repos/{self.test_repo}/contents/{test_env["test_file_name"]}',
                    headers=headers,
                    json=delete_data
                )
        
        except Exception:
            pass  # Ignore cleanup errors


@pytest.fixture
def source_consistency_test():
    """Fixture for source code retrieval consistency test"""
    return SourceCodeRetrievalConsistencyTest()


@given(
    file_content=st.text(min_size=50, max_size=1000, alphabet=st.characters(whitelist_categories=('Lu', 'Ll', 'Nd', 'Pc', 'Pd', 'Ps', 'Pe', 'Po'))),
    content_suffix=st.text(min_size=10, max_size=100, alphabet=st.characters(whitelist_categories=('Lu', 'Ll', 'Nd')))
)
@settings(max_examples=5, deadline=600000)  # 10 minute timeout per test
def test_source_code_retrieval_consistency(source_consistency_test, file_content, content_suffix):
    """
    **Validates: Requirements 1.5**
    
    Property 3: Source Code Retrieval Consistency
    For any build execution, Jenkins should clone the exact source code from the repository 
    that corresponds to the triggering commit
    """
    # Skip if required environment variables are not set
    if not all([
        source_consistency_test.jenkins_token,
        source_consistency_test.github_token,
        source_consistency_test.test_repo
    ]):
        pytest.skip("Required environment variables not set for source consistency test")
    
    # Set up test environment
    test_env = source_consistency_test.setup_test_environment()
    
    # Create unique test content
    unique_content = f"""# Source Code Retrieval Consistency Test
# Test ID: {test_env['test_id']}
# Timestamp: {test_env['timestamp']}
# Content Hash Verification Test

{file_content}

# Unique suffix for this test iteration
{content_suffix}

# End of test file
"""
    
    try:
        # Step 1: Create commit with specific content
        commit_sha = source_consistency_test.create_test_commit_with_content(test_env, unique_content)
        assert commit_sha, "Failed to create test commit"
        print(f"Created test commit {commit_sha[:8]} with content hash {test_env['test_content_hash'][:8]}")
        
        # Step 2: Wait for Jenkins to build the commit
        build_info = source_consistency_test.wait_for_jenkins_build_completion(commit_sha, timeout=300)
        assert build_info is not None, f"Jenkins build not completed for commit {commit_sha} within timeout"
        
        build_number = build_info['number']
        print(f"Jenkins build #{build_number} completed with result: {build_info['result']}")
        
        # Step 3: Verify Jenkins workspace contains exact content from commit
        workspace_verification = source_consistency_test.verify_jenkins_workspace_content(build_number, test_env)
        
        # Assert workspace accessibility and file existence
        assert workspace_verification['workspace_accessible'], "Jenkins workspace not accessible"
        assert workspace_verification['test_file_exists'], f"Test file {test_env['test_file_name']} not found in workspace"
        
        # Assert content consistency (core property validation)
        assert workspace_verification['content_matches'], (
            f"Content mismatch: expected hash {test_env['test_content_hash'][:8]}, "
            f"got {workspace_verification['retrieved_content_hash'][:8] if workspace_verification['retrieved_content_hash'] else 'None'}"
        )
        
        # Assert commit SHA consistency
        assert workspace_verification['commit_sha_matches'], (
            f"Commit SHA mismatch: expected {test_env['commit_sha'][:8]}, "
            f"got {workspace_verification['workspace_commit_sha'][:8] if workspace_verification['workspace_commit_sha'] else 'None'}"
        )
        
        # Step 4: Verify git repository state in workspace
        git_verification = source_consistency_test.verify_git_repository_state(build_number, test_env)
        
        # Assert git repository consistency
        assert git_verification['commit_in_history'], f"Commit {commit_sha[:8]} not found in git history"
        assert git_verification['head_points_to_commit'], f"Git HEAD does not point to commit {commit_sha[:8]}"
        
        print(f"✓ Property 3 validated: Jenkins retrieved exact source code for commit {commit_sha[:8]}")
        print(f"  - Content hash verified: {test_env['test_content_hash'][:8]}")
        print(f"  - Commit SHA verified: {commit_sha[:8]}")
        print(f"  - Git repository state consistent")
        
    finally:
        # Cleanup test artifacts
        source_consistency_test.cleanup_test_commit(test_env)


def test_source_code_retrieval_consistency_manual():
    """
    Manual test case for source code retrieval consistency
    This test can be run independently to verify the consistency mechanism
    """
    test_instance = SourceCodeRetrievalConsistencyTest()
    
    # Skip if environment not configured
    if not all([test_instance.jenkins_token, test_instance.github_token]):
        pytest.skip("Manual test requires JENKINS_TOKEN and GITHUB_TOKEN environment variables")
    
    test_env = test_instance.setup_test_environment()
    
    # Create deterministic test content
    test_content = f"""# Manual Source Consistency Test
# Test ID: {test_env['test_id']}
# This is a manual test to verify source code retrieval consistency

def test_function():
    return "consistency_test_{test_env['test_id']}"

# Unique identifier: {test_env['timestamp']}
"""
    
    try:
        commit_sha = test_instance.create_test_commit_with_content(test_env, test_content)
        print(f"Created manual test commit: {commit_sha}")
        
        # Wait for build
        build_info = test_instance.wait_for_jenkins_build_completion(commit_sha, timeout=180)
        if build_info:
            print(f"Build completed: #{build_info['number']} - {build_info['result']}")
            
            # Verify workspace content
            workspace_verification = test_instance.verify_jenkins_workspace_content(
                build_info['number'], test_env
            )
            
            if workspace_verification['content_matches']:
                print("✓ Content consistency verified")
            else:
                print("✗ Content consistency failed")
            
            if workspace_verification['commit_sha_matches']:
                print("✓ Commit SHA consistency verified")
            else:
                print("✗ Commit SHA consistency failed")
            
            assert workspace_verification['content_matches'], "Manual test failed - content mismatch"
            assert workspace_verification['commit_sha_matches'], "Manual test failed - commit SHA mismatch"
        else:
            pytest.fail("Manual test failed - build not completed within timeout")
    
    finally:
        test_instance.cleanup_test_commit(test_env)


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v'])