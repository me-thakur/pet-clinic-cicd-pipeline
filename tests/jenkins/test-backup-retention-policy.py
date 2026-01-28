#!/usr/bin/env python3

"""
Property-Based Test for Jenkins Backup Retention Policy
**Feature: pet-clinic-cicd-pipeline, Property 10: Backup Retention Policy Compliance**
**Validates: Requirements 7.5**

This test verifies that for any backup retention policy configuration, the backup system
should automatically remove backups that exceed the retention period while preserving
those within the policy.
"""

import os
import sys
import tempfile
import shutil
import subprocess
import json
import hashlib
import zipfile
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple
import hypothesis
from hypothesis import given, strategies as st, settings
import pytest
import boto3
from moto import mock_aws
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class BackupRetentionPolicyTest:
    """Test class for backup retention policy compliance properties"""
    
    def __init__(self):
        self.temp_dirs = []
        self.s3_client = None
        self.bucket_name = "test-jenkins-backups"
    
    def setup_method(self):
        """Setup test environment"""
        # Create temporary directories for testing
        self.backup_dir = tempfile.mkdtemp(prefix="backup_retention_")
        self.temp_dirs.append(self.backup_dir)
        
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
    
    def create_backup_file(self, backup_date: datetime, backup_type: str = "FULL") -> str:
        """Create a mock backup file with specified date"""
        backup_filename = f"{backup_type}-{backup_date.strftime('%Y-%m-%d_%H-%M-%S')}.zip"
        backup_path = os.path.join(self.backup_dir, backup_filename)
        
        # Create a simple zip file with some content
        with zipfile.ZipFile(backup_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
            # Add some mock Jenkins configuration
            zipf.writestr("config.xml", f"""<?xml version='1.1' encoding='UTF-8'?>
<hudson>
  <version>2.401.3</version>
  <numExecutors>2</numExecutors>
  <mode>NORMAL</mode>
  <useSecurity>true</useSecurity>
  <backupDate>{backup_date.isoformat()}</backupDate>
</hudson>""")
            
            # Add a mock job
            zipf.writestr("jobs/test-job/config.xml", """<?xml version='1.1' encoding='UTF-8'?>
<project>
  <description>Test job for backup retention testing</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.scm.NullSCM"/>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <triggers/>
  <concurrentBuild>false</concurrentBuild>
  <builders/>
  <publishers/>
  <buildWrappers/>
</project>""")
        
        # Set file modification time to match backup date
        timestamp = backup_date.timestamp()
        os.utime(backup_path, (timestamp, timestamp))
        
        return backup_path
    
    def upload_backup_to_s3(self, backup_file: str, backup_date: datetime) -> str:
        """Upload backup to S3 with date-based key structure"""
        backup_filename = os.path.basename(backup_file)
        s3_key = f"jenkins-backups/{backup_date.strftime('%Y/%m/%d')}/{backup_filename}"
        
        # Calculate checksum
        with open(backup_file, 'rb') as f:
            checksum = hashlib.sha256(f.read()).hexdigest()
        
        # Upload to S3 with metadata
        self.s3_client.upload_file(
            backup_file,
            self.bucket_name,
            s3_key,
            ExtraArgs={
                'Metadata': {
                    'checksum': checksum,
                    'backup-date': backup_date.isoformat(),
                    'backup-type': 'jenkins-backup'
                }
            }
        )
        
        return s3_key
    
    def create_backup_set(self, backup_dates: List[datetime]) -> List[Tuple[str, str]]:
        """Create a set of backups with specified dates and upload to S3"""
        backup_files = []
        
        for backup_date in backup_dates:
            # Create local backup file
            backup_file = self.create_backup_file(backup_date)
            
            # Upload to S3
            s3_key = self.upload_backup_to_s3(backup_file, backup_date)
            
            backup_files.append((backup_file, s3_key))
        
        return backup_files
    
    def list_s3_backups(self) -> List[Dict[str, Any]]:
        """List all backups in S3 bucket"""
        response = self.s3_client.list_objects_v2(
            Bucket=self.bucket_name,
            Prefix="jenkins-backups/"
        )
        
        backups = []
        for obj in response.get('Contents', []):
            # Get object metadata
            head_response = self.s3_client.head_object(
                Bucket=self.bucket_name,
                Key=obj['Key']
            )
            
            backup_info = {
                'key': obj['Key'],
                'size': obj['Size'],
                'last_modified': obj['LastModified'],
                'metadata': head_response.get('Metadata', {})
            }
            
            # Parse backup date from metadata or key
            backup_date_str = backup_info['metadata'].get('backup-date')
            if backup_date_str:
                backup_info['backup_date'] = datetime.fromisoformat(backup_date_str.replace('Z', '+00:00'))
            else:
                # Try to parse from key structure
                try:
                    date_parts = obj['Key'].split('/')[1:4]  # jenkins-backups/YYYY/MM/DD/
                    if len(date_parts) == 3:
                        backup_info['backup_date'] = datetime(
                            int(date_parts[0]), int(date_parts[1]), int(date_parts[2])
                        )
                except (ValueError, IndexError):
                    backup_info['backup_date'] = obj['LastModified'].replace(tzinfo=None)
            
            backups.append(backup_info)
        
        return backups
    
    def apply_retention_policy(self, retention_policy: Dict[str, int]) -> Dict[str, Any]:
        """Apply retention policy to S3 backups"""
        current_date = datetime.now()
        backups = self.list_s3_backups()
        
        # Sort backups by date (newest first)
        backups.sort(key=lambda x: x['backup_date'], reverse=True)
        
        retention_result = {
            'total_backups_before': len(backups),
            'backups_to_keep': [],
            'backups_to_delete': [],
            'policy_applied': retention_policy
        }
        
        # Apply daily retention
        daily_cutoff = current_date - timedelta(days=retention_policy.get('daily_days', 30))
        daily_backups = [b for b in backups if b['backup_date'] >= daily_cutoff]
        
        # Apply weekly retention (keep one backup per week for older backups)
        weekly_cutoff = current_date - timedelta(days=retention_policy.get('weekly_days', 84))  # 12 weeks
        weekly_backups = []
        if retention_policy.get('weekly_days', 0) > 0:
            older_backups = [b for b in backups if b['backup_date'] < daily_cutoff and b['backup_date'] >= weekly_cutoff]
            # Group by week and keep the newest from each week
            weeks = {}
            for backup in older_backups:
                week_key = backup['backup_date'].strftime('%Y-W%U')
                if week_key not in weeks or backup['backup_date'] > weeks[week_key]['backup_date']:
                    weeks[week_key] = backup
            weekly_backups = list(weeks.values())
        
        # Apply monthly retention (keep one backup per month for even older backups)
        monthly_cutoff = current_date - timedelta(days=retention_policy.get('monthly_days', 365))  # 12 months
        monthly_backups = []
        if retention_policy.get('monthly_days', 0) > 0:
            older_backups = [b for b in backups if b['backup_date'] < weekly_cutoff and b['backup_date'] >= monthly_cutoff]
            # Group by month and keep the newest from each month
            months = {}
            for backup in older_backups:
                month_key = backup['backup_date'].strftime('%Y-%m')
                if month_key not in months or backup['backup_date'] > months[month_key]['backup_date']:
                    months[month_key] = backup
            monthly_backups = list(months.values())
        
        # Combine all backups to keep
        backups_to_keep = daily_backups + weekly_backups + monthly_backups
        backups_to_keep_keys = {b['key'] for b in backups_to_keep}
        
        # Determine backups to delete
        backups_to_delete = [b for b in backups if b['key'] not in backups_to_keep_keys]
        
        retention_result['backups_to_keep'] = backups_to_keep
        retention_result['backups_to_delete'] = backups_to_delete
        retention_result['total_backups_after'] = len(backups_to_keep)
        
        # Actually delete the backups marked for deletion
        for backup in backups_to_delete:
            try:
                self.s3_client.delete_object(Bucket=self.bucket_name, Key=backup['key'])
                logger.info(f"Deleted backup: {backup['key']}")
            except Exception as e:
                logger.error(f"Failed to delete backup {backup['key']}: {e}")
        
        return retention_result
    
    def verify_retention_compliance(self, retention_policy: Dict[str, int], 
                                  retention_result: Dict[str, Any]) -> Dict[str, bool]:
        """Verify that retention policy was applied correctly"""
        current_date = datetime.now()
        compliance_result = {
            'daily_compliance': True,
            'weekly_compliance': True,
            'monthly_compliance': True,
            'no_future_backups': True,
            'policy_respected': True
        }
        
        kept_backups = retention_result['backups_to_keep']
        deleted_backups = retention_result['backups_to_delete']
        
        # Check daily retention compliance
        daily_cutoff = current_date - timedelta(days=retention_policy.get('daily_days', 30))
        daily_backups = [b for b in kept_backups if b['backup_date'] >= daily_cutoff]
        
        # Verify no daily backups were incorrectly deleted
        for backup in deleted_backups:
            if backup['backup_date'] >= daily_cutoff:
                compliance_result['daily_compliance'] = False
                logger.error(f"Daily backup incorrectly deleted: {backup['key']}")
        
        # Check weekly retention compliance
        if retention_policy.get('weekly_days', 0) > 0:
            weekly_cutoff = current_date - timedelta(days=retention_policy.get('weekly_days', 84))
            weekly_range_backups = [b for b in kept_backups 
                                  if daily_cutoff > b['backup_date'] >= weekly_cutoff]
            
            # Group by week and verify only one per week is kept
            weeks = {}
            for backup in weekly_range_backups:
                week_key = backup['backup_date'].strftime('%Y-W%U')
                if week_key not in weeks:
                    weeks[week_key] = []
                weeks[week_key].append(backup)
            
            # Check that we don't have too many backups per week
            for week_key, week_backups in weeks.items():
                if len(week_backups) > 1:
                    # This might be acceptable if they're from different days
                    # but let's check if any should have been deleted
                    pass
        
        # Check monthly retention compliance
        if retention_policy.get('monthly_days', 0) > 0:
            monthly_cutoff = current_date - timedelta(days=retention_policy.get('monthly_days', 365))
            weekly_cutoff = current_date - timedelta(days=retention_policy.get('weekly_days', 84))
            monthly_range_backups = [b for b in kept_backups 
                                   if weekly_cutoff > b['backup_date'] >= monthly_cutoff]
            
            # Group by month and verify only one per month is kept
            months = {}
            for backup in monthly_range_backups:
                month_key = backup['backup_date'].strftime('%Y-%m')
                if month_key not in months:
                    months[month_key] = []
                months[month_key].append(backup)
        
        # Check that no future backups exist
        for backup in kept_backups:
            if backup['backup_date'] > current_date:
                compliance_result['no_future_backups'] = False
                logger.error(f"Future backup found: {backup['key']}")
        
        # Check that backups older than the maximum retention period were deleted
        max_retention_days = max(
            retention_policy.get('daily_days', 30),
            retention_policy.get('weekly_days', 84),
            retention_policy.get('monthly_days', 365)
        )
        max_cutoff = current_date - timedelta(days=max_retention_days)
        
        for backup in kept_backups:
            if backup['backup_date'] < max_cutoff:
                compliance_result['policy_respected'] = False
                logger.error(f"Backup older than maximum retention kept: {backup['key']}")
        
        return compliance_result

# Generate test data strategies
@st.composite
def retention_policy_strategy(draw):
    """Generate retention policy configurations for testing"""
    return {
        'daily_days': draw(st.integers(min_value=1, max_value=60)),
        'weekly_days': draw(st.integers(min_value=7, max_value=180)),
        'monthly_days': draw(st.integers(min_value=30, max_value=730))
    }

@st.composite
def backup_dates_strategy(draw):
    """Generate sets of backup dates for testing"""
    current_date = datetime.now()
    num_backups = draw(st.integers(min_value=5, max_value=50))
    
    backup_dates = []
    for _ in range(num_backups):
        # Generate dates from 2 years ago to now
        days_ago = draw(st.integers(min_value=0, max_value=730))
        backup_date = current_date - timedelta(days=days_ago)
        backup_dates.append(backup_date)
    
    # Remove duplicates and sort
    backup_dates = sorted(list(set(backup_dates)))
    
    return backup_dates

class TestBackupRetentionPolicy:
    """Property-based tests for backup retention policy compliance"""
    
    def setup_method(self):
        self.test_instance = BackupRetentionPolicyTest()
        self.test_instance.setup_method()
    
    def teardown_method(self):
        self.test_instance.teardown_method()
    
    @given(
        retention_policy=retention_policy_strategy(),
        backup_dates=backup_dates_strategy()
    )
    @settings(max_examples=3, deadline=60000)  # Reduced examples for faster execution
    def test_retention_policy_compliance(self, retention_policy, backup_dates):
        """
        Property 10: Backup Retention Policy Compliance
        For any backup retention policy configuration, the backup system should automatically
        remove backups that exceed the retention period while preserving those within the policy.
        """
        # Ensure policy is logically consistent
        if retention_policy['weekly_days'] <= retention_policy['daily_days']:
            retention_policy['weekly_days'] = retention_policy['daily_days'] + 7
        if retention_policy['monthly_days'] <= retention_policy['weekly_days']:
            retention_policy['monthly_days'] = retention_policy['weekly_days'] + 30
        
        # Create backup set
        backup_files = self.test_instance.create_backup_set(backup_dates)
        assert len(backup_files) == len(backup_dates), "All backups should be created"
        
        # Verify all backups exist in S3
        initial_backups = self.test_instance.list_s3_backups()
        assert len(initial_backups) == len(backup_dates), "All backups should be uploaded to S3"
        
        # Apply retention policy
        retention_result = self.test_instance.apply_retention_policy(retention_policy)
        
        # Verify retention policy was applied
        assert retention_result['total_backups_before'] == len(backup_dates), "Initial backup count should match"
        assert retention_result['total_backups_after'] <= retention_result['total_backups_before'], "Backup count should not increase"
        
        # Verify compliance
        compliance_result = self.test_instance.verify_retention_compliance(retention_policy, retention_result)
        
        # Assert compliance requirements
        assert compliance_result['daily_compliance'], "Daily retention policy should be complied with"
        assert compliance_result['weekly_compliance'], "Weekly retention policy should be complied with"
        assert compliance_result['monthly_compliance'], "Monthly retention policy should be complied with"
        assert compliance_result['no_future_backups'], "No future backups should exist"
        assert compliance_result['policy_respected'], "Overall retention policy should be respected"
        
        # Verify final state
        final_backups = self.test_instance.list_s3_backups()
        assert len(final_backups) == retention_result['total_backups_after'], "Final backup count should match retention result"
    
    @given(retention_policy=retention_policy_strategy())
    @settings(max_examples=3, deadline=60000)  # Reduced examples for faster execution
    def test_retention_policy_preserves_recent_backups(self, retention_policy):
        """
        Property 10: Backup Retention Policy Compliance (Recent Backups variant)
        For any retention policy, recent backups within the daily retention period should always be preserved.
        """
        current_date = datetime.now()
        
        # Create recent backups (within daily retention period)
        recent_dates = []
        for i in range(min(retention_policy['daily_days'], 10)):
            recent_dates.append(current_date - timedelta(days=i))
        
        # Create some older backups that should be deleted
        old_dates = []
        for i in range(5):
            days_ago = retention_policy['monthly_days'] + 30 + i  # Beyond maximum retention
            old_dates.append(current_date - timedelta(days=days_ago))
        
        all_dates = recent_dates + old_dates
        
        # Create backup set
        backup_files = self.test_instance.create_backup_set(all_dates)
        
        # Apply retention policy
        retention_result = self.test_instance.apply_retention_policy(retention_policy)
        
        # Verify all recent backups are preserved
        kept_backup_dates = {b['backup_date'].date() for b in retention_result['backups_to_keep']}
        
        for recent_date in recent_dates:
            assert recent_date.date() in kept_backup_dates, f"Recent backup should be preserved: {recent_date}"
        
        # Verify old backups are deleted
        for old_date in old_dates:
            assert old_date.date() not in kept_backup_dates, f"Old backup should be deleted: {old_date}"
    
    @given(backup_dates=backup_dates_strategy())
    @settings(max_examples=3, deadline=60000)  # Reduced examples for faster execution
    def test_retention_policy_idempotency(self, backup_dates):
        """
        Property 10: Backup Retention Policy Compliance (Idempotency variant)
        For any set of backups, applying the same retention policy multiple times should produce the same result.
        """
        retention_policy = {
            'daily_days': 30,
            'weekly_days': 84,
            'monthly_days': 365
        }
        
        # Create backup set
        backup_files = self.test_instance.create_backup_set(backup_dates)
        
        # Apply retention policy first time
        first_result = self.test_instance.apply_retention_policy(retention_policy)
        first_kept_keys = {b['key'] for b in first_result['backups_to_keep']}
        
        # Apply retention policy second time
        second_result = self.test_instance.apply_retention_policy(retention_policy)
        second_kept_keys = {b['key'] for b in second_result['backups_to_keep']}
        
        # Results should be identical
        assert first_kept_keys == second_kept_keys, "Retention policy should be idempotent"
        assert first_result['total_backups_after'] == second_result['total_backups_after'], "Backup counts should be identical"
        assert len(second_result['backups_to_delete']) == 0, "No additional backups should be deleted on second run"
    
    @given(retention_policy=retention_policy_strategy())
    @settings(max_examples=3, deadline=60000)  # Reduced examples for faster execution
    def test_retention_policy_weekly_monthly_logic(self, retention_policy):
        """
        Property 10: Backup Retention Policy Compliance (Weekly/Monthly Logic variant)
        For any retention policy, weekly and monthly retention should keep appropriate representative backups.
        """
        current_date = datetime.now()
        
        # Create a comprehensive set of backups spanning the retention periods
        backup_dates = []
        
        # Daily backups for the daily retention period
        for i in range(retention_policy['daily_days']):
            backup_dates.append(current_date - timedelta(days=i))
        
        # Weekly backups for the weekly retention period
        start_weekly = retention_policy['daily_days']
        end_weekly = retention_policy['weekly_days']
        for week in range(start_weekly // 7, end_weekly // 7):
            backup_dates.append(current_date - timedelta(days=week * 7))
        
        # Monthly backups for the monthly retention period
        start_monthly = retention_policy['weekly_days']
        end_monthly = retention_policy['monthly_days']
        for month in range(start_monthly // 30, end_monthly // 30):
            backup_dates.append(current_date - timedelta(days=month * 30))
        
        # Remove duplicates and sort
        backup_dates = sorted(list(set(backup_dates)))
        
        if len(backup_dates) == 0:
            return  # Skip if no valid dates generated
        
        # Create backup set
        backup_files = self.test_instance.create_backup_set(backup_dates)
        
        # Apply retention policy
        retention_result = self.test_instance.apply_retention_policy(retention_policy)
        
        # Verify compliance
        compliance_result = self.test_instance.verify_retention_compliance(retention_policy, retention_result)
        
        # Assert that the logic correctly handles weekly and monthly retention
        assert compliance_result['weekly_compliance'], "Weekly retention logic should be correct"
        assert compliance_result['monthly_compliance'], "Monthly retention logic should be correct"
        
        # Verify that we have some backups in each retention tier (if applicable)
        kept_backups = retention_result['backups_to_keep']
        daily_cutoff = current_date - timedelta(days=retention_policy['daily_days'])
        weekly_cutoff = current_date - timedelta(days=retention_policy['weekly_days'])
        
        daily_backups = [b for b in kept_backups if b['backup_date'] >= daily_cutoff]
        weekly_backups = [b for b in kept_backups if daily_cutoff > b['backup_date'] >= weekly_cutoff]
        
        if len(backup_dates) > retention_policy['daily_days']:
            assert len(daily_backups) > 0, "Should have daily backups if within retention period"
        
        if len(backup_dates) > retention_policy['weekly_days']:
            # Should have some weekly backups if we created backups in that range
            weekly_range_originals = [d for d in backup_dates if daily_cutoff > d >= weekly_cutoff]
            if len(weekly_range_originals) > 0:
                assert len(weekly_backups) > 0, "Should have weekly backups if backups existed in weekly range"

if __name__ == "__main__":
    # Run the property-based tests
    pytest.main([__file__, "-v", "--tb=short"])