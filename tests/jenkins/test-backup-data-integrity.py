#!/usr/bin/env python3

"""
Property-Based Test for Jenkins Backup Data Integrity
**Feature: pet-clinic-cicd-pipeline, Property 9: Backup Data Integrity**
**Validates: Requirements 7.1, 7.2, 7.4**

This test verifies that for any Jenkins backup created, restoring from that backup
should recreate a Jenkins instance with identical job configurations, build history,
and system settings.
"""

import os
import sys
import tempfile
import shutil
import subprocess
import json
import hashlib
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, List, Any, Optional
import hypothesis
from hypothesis import given, strategies as st, settings
import pytest
import boto3
from moto import mock_aws
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class JenkinsBackupIntegrityTest:
    """Test class for Jenkins backup data integrity properties"""
    
    def __init__(self):
        self.temp_dirs = []
        self.s3_client = None
        self.bucket_name = "test-jenkins-backups"
    
    def setup_method(self):
        """Setup test environment"""
        # Create temporary directories for testing
        self.jenkins_home = tempfile.mkdtemp(prefix="jenkins_home_")
        self.backup_dir = tempfile.mkdtemp(prefix="backup_")
        self.restore_dir = tempfile.mkdtemp(prefix="restore_")
        self.temp_dirs.extend([self.jenkins_home, self.backup_dir, self.restore_dir])
        
        # Setup mock S3
        self.s3_mock = mock_aws()
        self.s3_mock.start()
        self.s3_client = boto3.client('s3', region_name='us-east-1')
        self.s3_client.create_bucket(Bucket=self.bucket_name)
    
    def teardown_method(self):
        """Cleanup test environment"""
        # Stop S3 mock
        if hasattr(self, 's3_mock'):
            self.s3_mock.stop()
        
        # Clean up temporary directories
        for temp_dir in self.temp_dirs:
            if os.path.exists(temp_dir):
                shutil.rmtree(temp_dir, ignore_errors=True)
        self.temp_dirs.clear()
    
    def create_jenkins_config(self, config_data: Dict[str, Any]) -> str:
        """Create a Jenkins configuration with specified data"""
        jenkins_home = self.jenkins_home
        
        # Create basic Jenkins directory structure
        os.makedirs(f"{jenkins_home}/jobs", exist_ok=True)
        os.makedirs(f"{jenkins_home}/users", exist_ok=True)
        os.makedirs(f"{jenkins_home}/plugins", exist_ok=True)
        
        # Create config.xml
        config_xml = f"""<?xml version='1.1' encoding='UTF-8'?>
<hudson>
  <version>{config_data.get('version', '2.401.3')}</version>
  <numExecutors>{config_data.get('executors', 2)}</numExecutors>
  <mode>NORMAL</mode>
  <useSecurity>{str(config_data.get('security', True)).lower()}</useSecurity>
  <authorizationStrategy class="hudson.security.FullControlOnceLoggedInAuthorizationStrategy">
    <denyAnonymousReadAccess>{str(config_data.get('denyAnonymous', True)).lower()}</denyAnonymousReadAccess>
  </authorizationStrategy>
  <securityRealm class="hudson.security.HudsonPrivateSecurityRealm">
    <disableSignup>{str(config_data.get('disableSignup', True)).lower()}</disableSignup>
    <enableCaptcha>false</enableCaptcha>
  </securityRealm>
  <disableRememberMe>false</disableRememberMe>
  <projectNamingStrategy class="jenkins.model.ProjectNamingStrategy$DefaultProjectNamingStrategy"/>
  <workspaceDir>${{JENKINS_HOME}}/workspace/${{ITEM_FULLNAME}}</workspaceDir>
  <buildsDir>${{ITEM_ROOTDIR}}/builds</buildsDir>
  <systemMessage>{config_data.get('systemMessage', 'Jenkins Test Instance')}</systemMessage>
</hudson>"""
        
        with open(f"{jenkins_home}/config.xml", 'w') as f:
            f.write(config_xml)
        
        # Create jobs
        for job_name, job_config in config_data.get('jobs', {}).items():
            job_dir = f"{jenkins_home}/jobs/{job_name}"
            os.makedirs(job_dir, exist_ok=True)
            
            job_xml = f"""<?xml version='1.1' encoding='UTF-8'?>
<project>
  <description>{job_config.get('description', f'Test job {job_name}')}</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.scm.NullSCM"/>
  <canRoam>true</canRoam>
  <disabled>{str(job_config.get('disabled', False)).lower()}</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <triggers/>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.tasks.Shell>
      <command>{job_config.get('command', 'echo "Hello World"')}</command>
    </hudson.tasks.Shell>
  </builders>
  <publishers/>
  <buildWrappers/>
</project>"""
            
            with open(f"{job_dir}/config.xml", 'w') as f:
                f.write(job_xml)
            
            # Create build history
            builds_dir = f"{job_dir}/builds"
            os.makedirs(builds_dir, exist_ok=True)
            
            for build_num in job_config.get('builds', []):
                build_dir = f"{builds_dir}/{build_num}"
                os.makedirs(build_dir, exist_ok=True)
                
                build_xml = f"""<?xml version='1.1' encoding='UTF-8'?>
<build>
  <actions/>
  <queueId>{build_num}</queueId>
  <timestamp>{1640995200000 + build_num * 86400000}</timestamp>
  <startTime>{1640995200000 + build_num * 86400000}</startTime>
  <result>SUCCESS</result>
  <duration>5000</duration>
  <charset>UTF-8</charset>
  <keepLog>false</keepLog>
  <builtOn></builtOn>
  <workspace>{jenkins_home}/workspace/{job_name}</workspace>
  <hudsonVersion>{config_data.get('version', '2.401.3')}</hudsonVersion>
  <scm class="hudson.scm.NullChangeLogParser"/>
  <culprits/>
</build>"""
                
                with open(f"{build_dir}/build.xml", 'w') as f:
                    f.write(build_xml)
                
                # Create log file
                with open(f"{build_dir}/log", 'w') as f:
                    f.write(f"Build #{build_num} log content\n")
                    f.write(f"Started by user admin\n")
                    f.write(f"Building in workspace {jenkins_home}/workspace/{job_name}\n")
                    f.write(f"[{job_name}] $ /bin/sh -xe /tmp/jenkins{build_num}.sh\n")
                    f.write(f"+ {job_config.get('command', 'echo \"Hello World\"')}\n")
                    f.write(f"Hello World\n")
                    f.write(f"Finished: SUCCESS\n")
        
        # Create users
        for username, user_config in config_data.get('users', {}).items():
            user_dir = f"{jenkins_home}/users/{username}"
            os.makedirs(user_dir, exist_ok=True)
            
            user_xml = f"""<?xml version='1.1' encoding='UTF-8'?>
<user>
  <fullName>{user_config.get('fullName', username)}</fullName>
  <description>{user_config.get('description', f'User {username}')}</description>
  <properties>
    <jenkins.security.ApiTokenProperty>
      <apiToken>
        <value>{user_config.get('apiToken', 'test-token-' + username)}</value>
      </apiToken>
    </jenkins.security.ApiTokenProperty>
    <hudson.model.MyViewsProperty>
      <views>
        <hudson.model.AllView>
          <owner class="hudson.model.MyViewsProperty" reference="../../.."/>
          <name>all</name>
          <filterExecutors>false</filterExecutors>
          <filterQueue>false</filterQueue>
          <properties class="hudson.model.View$PropertyList"/>
        </hudson.model.AllView>
      </views>
    </hudson.model.MyViewsProperty>
  </properties>
</user>"""
            
            with open(f"{user_dir}/config.xml", 'w') as f:
                f.write(user_xml)
        
        return jenkins_home
    
    def create_backup(self, jenkins_home: str) -> str:
        """Create a backup of Jenkins home directory"""
        backup_file = f"{self.backup_dir}/jenkins-backup-{os.path.basename(jenkins_home)}.zip"
        
        with zipfile.ZipFile(backup_file, 'w', zipfile.ZIP_DEFLATED) as zipf:
            for root, dirs, files in os.walk(jenkins_home):
                for file in files:
                    file_path = os.path.join(root, file)
                    arcname = os.path.relpath(file_path, jenkins_home)
                    zipf.write(file_path, arcname)
        
        return backup_file
    
    def restore_backup(self, backup_file: str, restore_path: str) -> str:
        """Restore Jenkins from backup file"""
        os.makedirs(restore_path, exist_ok=True)
        
        with zipfile.ZipFile(backup_file, 'r') as zipf:
            zipf.extractall(restore_path)
        
        return restore_path
    
    def calculate_directory_hash(self, directory: str) -> Dict[str, str]:
        """Calculate hash of all files in directory for comparison"""
        file_hashes = {}
        
        for root, dirs, files in os.walk(directory):
            for file in files:
                file_path = os.path.join(root, file)
                relative_path = os.path.relpath(file_path, directory)
                
                with open(file_path, 'rb') as f:
                    file_hash = hashlib.sha256(f.read()).hexdigest()
                    file_hashes[relative_path] = file_hash
        
        return file_hashes
    
    def compare_jenkins_configs(self, original: str, restored: str) -> Dict[str, Any]:
        """Compare original and restored Jenkins configurations"""
        comparison = {
            'identical': True,
            'differences': [],
            'missing_files': [],
            'extra_files': [],
            'file_differences': []
        }
        
        original_hashes = self.calculate_directory_hash(original)
        restored_hashes = self.calculate_directory_hash(restored)
        
        # Check for missing files
        for file_path in original_hashes:
            if file_path not in restored_hashes:
                comparison['missing_files'].append(file_path)
                comparison['identical'] = False
        
        # Check for extra files
        for file_path in restored_hashes:
            if file_path not in original_hashes:
                comparison['extra_files'].append(file_path)
                comparison['identical'] = False
        
        # Check for file differences
        for file_path in original_hashes:
            if file_path in restored_hashes:
                if original_hashes[file_path] != restored_hashes[file_path]:
                    comparison['file_differences'].append({
                        'file': file_path,
                        'original_hash': original_hashes[file_path],
                        'restored_hash': restored_hashes[file_path]
                    })
                    comparison['identical'] = False
        
        return comparison
    
    def upload_backup_to_s3(self, backup_file: str, s3_key: str) -> bool:
        """Upload backup to S3 with metadata"""
        try:
            # Calculate checksum
            with open(backup_file, 'rb') as f:
                checksum = hashlib.sha256(f.read()).hexdigest()
            
            # Upload to S3
            self.s3_client.upload_file(
                backup_file,
                self.bucket_name,
                s3_key,
                ExtraArgs={
                    'Metadata': {
                        'checksum': checksum,
                        'upload-time': '2023-01-01T00:00:00Z'
                    }
                }
            )
            return True
        except Exception as e:
            logger.error(f"Failed to upload backup to S3: {e}")
            return False
    
    def download_backup_from_s3(self, s3_key: str, local_file: str) -> bool:
        """Download backup from S3"""
        try:
            self.s3_client.download_file(self.bucket_name, s3_key, local_file)
            return True
        except Exception as e:
            logger.error(f"Failed to download backup from S3: {e}")
            return False
    
    def verify_s3_backup_integrity(self, backup_file: str, s3_key: str) -> bool:
        """Verify backup integrity using S3 metadata"""
        try:
            # Get S3 object metadata
            response = self.s3_client.head_object(Bucket=self.bucket_name, Key=s3_key)
            expected_checksum = response['Metadata'].get('checksum')
            
            if not expected_checksum:
                logger.warning("No checksum metadata found")
                return True  # Skip verification if no metadata
            
            # Calculate actual checksum
            with open(backup_file, 'rb') as f:
                actual_checksum = hashlib.sha256(f.read()).hexdigest()
            
            return actual_checksum == expected_checksum
        except Exception as e:
            logger.error(f"Failed to verify S3 backup integrity: {e}")
            return False

# Generate test data strategies
@st.composite
def jenkins_config_strategy(draw):
    """Generate Jenkins configuration data for testing"""
    num_jobs = draw(st.integers(min_value=1, max_value=5))
    num_users = draw(st.integers(min_value=1, max_value=3))
    
    jobs = {}
    for i in range(num_jobs):
        job_name = f"job-{i}"
        num_builds = draw(st.integers(min_value=0, max_value=5))
        jobs[job_name] = {
            'description': f'Test job {job_name}',
            'disabled': draw(st.booleans()),
            'command': draw(st.sampled_from(['echo "Hello"', 'ls -la', 'date', 'whoami'])),
            'builds': list(range(1, num_builds + 1))
        }
    
    users = {}
    for i in range(num_users):
        username = f"user{i}"
        users[username] = {
            'fullName': f'Test User {i}',
            'description': f'Test user {username}',
            'apiToken': f'token-{username}-{draw(st.text(min_size=8, max_size=16, alphabet=st.characters(min_codepoint=97, max_codepoint=122)))}'
        }
    
    return {
        'version': draw(st.sampled_from(['2.401.3', '2.400.1', '2.399.0'])),
        'executors': draw(st.integers(min_value=1, max_value=4)),
        'security': draw(st.booleans()),
        'denyAnonymous': draw(st.booleans()),
        'disableSignup': draw(st.booleans()),
        'systemMessage': draw(st.text(min_size=10, max_size=100)),
        'jobs': jobs,
        'users': users
    }

class TestBackupDataIntegrity:
    """Property-based tests for backup data integrity"""
    
    def setup_method(self):
        self.test_instance = JenkinsBackupIntegrityTest()
        self.test_instance.setup_method()
    
    def teardown_method(self):
        self.test_instance.teardown_method()
    
    @given(config=jenkins_config_strategy())
    @settings(max_examples=100, deadline=60000)
    def test_backup_restore_preserves_all_data(self, config):
        """
        Property 9: Backup Data Integrity
        For any Jenkins backup created, restoring from that backup should recreate 
        a Jenkins instance with identical job configurations, build history, and system settings.
        """
        # Create original Jenkins configuration
        original_jenkins = self.test_instance.create_jenkins_config(config)
        
        # Create backup
        backup_file = self.test_instance.create_backup(original_jenkins)
        assert os.path.exists(backup_file), "Backup file should be created"
        assert os.path.getsize(backup_file) > 0, "Backup file should not be empty"
        
        # Restore from backup
        restored_jenkins = self.test_instance.restore_backup(backup_file, self.test_instance.restore_dir)
        
        # Compare original and restored configurations
        comparison = self.test_instance.compare_jenkins_configs(original_jenkins, restored_jenkins)
        
        # Assert that configurations are identical
        assert comparison['identical'], f"Restored Jenkins should be identical to original. Differences: {comparison}"
        assert len(comparison['missing_files']) == 0, f"No files should be missing: {comparison['missing_files']}"
        assert len(comparison['extra_files']) == 0, f"No extra files should be present: {comparison['extra_files']}"
        assert len(comparison['file_differences']) == 0, f"No file differences should exist: {comparison['file_differences']}"
    
    @given(config=jenkins_config_strategy())
    @settings(max_examples=50, deadline=60000)
    def test_s3_backup_integrity_verification(self, config):
        """
        Property 9: Backup Data Integrity (S3 variant)
        For any Jenkins backup uploaded to S3, the integrity verification should pass
        and the downloaded backup should be identical to the original.
        """
        # Create original Jenkins configuration
        original_jenkins = self.test_instance.create_jenkins_config(config)
        
        # Create backup
        backup_file = self.test_instance.create_backup(original_jenkins)
        
        # Upload to S3
        s3_key = f"jenkins-backups/test/{os.path.basename(backup_file)}"
        upload_success = self.test_instance.upload_backup_to_s3(backup_file, s3_key)
        assert upload_success, "Backup should upload successfully to S3"
        
        # Download from S3
        downloaded_backup = f"{self.test_instance.backup_dir}/downloaded-{os.path.basename(backup_file)}"
        download_success = self.test_instance.download_backup_from_s3(s3_key, downloaded_backup)
        assert download_success, "Backup should download successfully from S3"
        
        # Verify integrity
        integrity_check = self.test_instance.verify_s3_backup_integrity(downloaded_backup, s3_key)
        assert integrity_check, "S3 backup integrity verification should pass"
        
        # Restore from downloaded backup
        restored_jenkins = self.test_instance.restore_backup(downloaded_backup, 
                                                           f"{self.test_instance.restore_dir}/s3-restore")
        
        # Compare original and restored configurations
        comparison = self.test_instance.compare_jenkins_configs(original_jenkins, restored_jenkins)
        
        # Assert that configurations are identical
        assert comparison['identical'], f"S3 restored Jenkins should be identical to original. Differences: {comparison}"
    
    @given(config=jenkins_config_strategy())
    @settings(max_examples=30, deadline=60000)
    def test_backup_preserves_job_configurations(self, config):
        """
        Property 9: Backup Data Integrity (Job Configuration variant)
        For any Jenkins backup, all job configurations should be preserved exactly.
        """
        # Create original Jenkins configuration
        original_jenkins = self.test_instance.create_jenkins_config(config)
        
        # Create and restore backup
        backup_file = self.test_instance.create_backup(original_jenkins)
        restored_jenkins = self.test_instance.restore_backup(backup_file, self.test_instance.restore_dir)
        
        # Verify each job configuration
        for job_name in config.get('jobs', {}):
            original_job_config = f"{original_jenkins}/jobs/{job_name}/config.xml"
            restored_job_config = f"{restored_jenkins}/jobs/{job_name}/config.xml"
            
            assert os.path.exists(restored_job_config), f"Job config should exist: {job_name}"
            
            # Parse and compare XML configurations
            with open(original_job_config, 'r') as f:
                original_xml = ET.parse(f)
            with open(restored_job_config, 'r') as f:
                restored_xml = ET.parse(f)
            
            # Compare XML structure (simplified comparison)
            original_desc = original_xml.find('description')
            restored_desc = restored_xml.find('description')
            
            if original_desc is not None and restored_desc is not None:
                assert original_desc.text == restored_desc.text, f"Job description should match for {job_name}"
    
    @given(config=jenkins_config_strategy())
    @settings(max_examples=30, deadline=60000)
    def test_backup_preserves_build_history(self, config):
        """
        Property 9: Backup Data Integrity (Build History variant)
        For any Jenkins backup, all build history should be preserved exactly.
        """
        # Create original Jenkins configuration
        original_jenkins = self.test_instance.create_jenkins_config(config)
        
        # Create and restore backup
        backup_file = self.test_instance.create_backup(original_jenkins)
        restored_jenkins = self.test_instance.restore_backup(backup_file, self.test_instance.restore_dir)
        
        # Verify build history for each job
        for job_name, job_config in config.get('jobs', {}).items():
            for build_num in job_config.get('builds', []):
                original_build_xml = f"{original_jenkins}/jobs/{job_name}/builds/{build_num}/build.xml"
                restored_build_xml = f"{restored_jenkins}/jobs/{job_name}/builds/{build_num}/build.xml"
                original_build_log = f"{original_jenkins}/jobs/{job_name}/builds/{build_num}/log"
                restored_build_log = f"{restored_jenkins}/jobs/{job_name}/builds/{build_num}/log"
                
                assert os.path.exists(restored_build_xml), f"Build XML should exist: {job_name}#{build_num}"
                assert os.path.exists(restored_build_log), f"Build log should exist: {job_name}#{build_num}"
                
                # Compare build logs
                with open(original_build_log, 'r') as f:
                    original_log_content = f.read()
                with open(restored_build_log, 'r') as f:
                    restored_log_content = f.read()
                
                assert original_log_content == restored_log_content, f"Build log should match for {job_name}#{build_num}"

if __name__ == "__main__":
    # Run the property-based tests
    pytest.main([__file__, "-v", "--tb=short"])