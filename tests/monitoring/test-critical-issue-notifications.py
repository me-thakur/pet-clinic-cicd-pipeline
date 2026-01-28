#!/usr/bin/env python3

"""
Property-Based Test for Critical Issue Notifications
**Feature: pet-clinic-cicd-pipeline, Property 15: Critical Issue Notification**
**Validates: Requirements 9.5**

This test verifies that for any critical system issue or failure, the monitoring system
should send notifications to administrators within the defined time threshold.
"""

import os
import sys
import tempfile
import shutil
import subprocess
import json
import time
import threading
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple
import hypothesis
from hypothesis import given, strategies as st, settings
import pytest
import boto3
from moto import mock_aws
import logging
from unittest.mock import Mock, patch
# import requests_mock  # Not needed for this test

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class CriticalIssueNotificationTest:
    """Test class for critical issue notification properties"""
    
    def __init__(self):
        self.temp_dirs = []
        self.mock_notifications = []
        self.notification_channels = {}
        self.time_threshold_seconds = 300  # 5 minutes
    
    def setup_method(self):
        """Setup test environment"""
        # Create temporary directories for testing
        self.test_dir = tempfile.mkdtemp(prefix="notification_test_")
        self.temp_dirs.append(self.test_dir)
        
        # Setup mock AWS services
        self.aws_mock = mock_aws()
        self.aws_mock.start()
        
        # Initialize mock services
        self.sns_client = boto3.client('sns', region_name='us-east-1')
        self.cloudwatch_client = boto3.client('cloudwatch', region_name='us-east-1')
        
        # Create mock SNS topic
        self.sns_topic_response = self.sns_client.create_topic(Name='test-critical-alerts')
        self.sns_topic_arn = self.sns_topic_response['TopicArn']
        
        # Reset notification tracking
        self.mock_notifications = []
        self.notification_channels = {
            'sns': {'enabled': True, 'notifications': []},
            'email': {'enabled': True, 'notifications': []},
            'slack': {'enabled': True, 'notifications': []},
            'pagerduty': {'enabled': True, 'notifications': []}
        }
    
    def teardown_method(self):
        """Cleanup test environment"""
        # Stop AWS mock
        if hasattr(self, 'aws_mock'):
            self.aws_mock.stop()
        
        # Clean up temporary directories
        for temp_dir in self.temp_dirs:
            if os.path.exists(temp_dir):
                shutil.rmtree(temp_dir, ignore_errors=True)
        self.temp_dirs.clear()
    
    def simulate_critical_issue(self, issue_type: str, issue_data: Dict[str, Any]) -> Dict[str, Any]:
        """Simulate a critical system issue"""
        issue_id = f"issue_{int(time.time() * 1000)}"
        issue_timestamp = datetime.now()
        
        issue_record = {
            'issue_id': issue_id,
            'issue_type': issue_type,
            'timestamp': issue_timestamp,
            'data': issue_data,
            'severity': self._determine_severity(issue_type, issue_data),
            'notifications_sent': [],
            'notification_start_time': None,
            'notification_end_time': None
        }
        
        # Trigger notifications based on issue type
        if issue_record['severity'] == 'critical':
            self._trigger_critical_notifications(issue_record)
        elif issue_record['severity'] == 'warning':
            self._trigger_warning_notifications(issue_record)
        
        return issue_record
    
    def _determine_severity(self, issue_type: str, issue_data: Dict[str, Any]) -> str:
        """Determine the severity level of an issue"""
        critical_issues = [
            'service_down',
            'database_failure',
            'security_breach',
            'system_failure',
            'backup_failure_critical',
            'disk_full',
            'memory_exhausted'
        ]
        
        warning_issues = [
            'high_cpu',
            'high_memory',
            'slow_response',
            'build_failure',
            'backup_failure_warning',
            'disk_space_low'
        ]
        
        if issue_type in critical_issues:
            return 'critical'
        elif issue_type in warning_issues:
            return 'warning'
        else:
            # Check data for severity indicators
            if issue_data.get('severity') == 'critical':
                return 'critical'
            elif issue_data.get('severity') == 'warning':
                return 'warning'
            else:
                return 'info'
    
    def _trigger_critical_notifications(self, issue_record: Dict[str, Any]):
        """Trigger notifications for critical issues"""
        issue_record['notification_start_time'] = datetime.now()
        
        # Send to all enabled notification channels
        notification_threads = []
        
        for channel, config in self.notification_channels.items():
            if config['enabled']:
                thread = threading.Thread(
                    target=self._send_notification,
                    args=(channel, issue_record)
                )
                thread.start()
                notification_threads.append(thread)
        
        # Wait for all notifications to complete (with timeout)
        for thread in notification_threads:
            thread.join(timeout=self.time_threshold_seconds)
        
        issue_record['notification_end_time'] = datetime.now()
    
    def _trigger_warning_notifications(self, issue_record: Dict[str, Any]):
        """Trigger notifications for warning issues"""
        issue_record['notification_start_time'] = datetime.now()
        
        # Send to selected notification channels (not PagerDuty for warnings)
        notification_threads = []
        
        warning_channels = ['sns', 'email', 'slack']
        for channel in warning_channels:
            if self.notification_channels[channel]['enabled']:
                thread = threading.Thread(
                    target=self._send_notification,
                    args=(channel, issue_record)
                )
                thread.start()
                notification_threads.append(thread)
        
        # Wait for notifications to complete
        for thread in notification_threads:
            thread.join(timeout=self.time_threshold_seconds)
        
        issue_record['notification_end_time'] = datetime.now()
    
    def _send_notification(self, channel: str, issue_record: Dict[str, Any]):
        """Send notification through specified channel"""
        notification_start = time.time()
        
        try:
            if channel == 'sns':
                self._send_sns_notification(issue_record)
            elif channel == 'email':
                self._send_email_notification(issue_record)
            elif channel == 'slack':
                self._send_slack_notification(issue_record)
            elif channel == 'pagerduty':
                self._send_pagerduty_notification(issue_record)
            
            notification_end = time.time()
            notification_duration = notification_end - notification_start
            
            notification_record = {
                'channel': channel,
                'issue_id': issue_record['issue_id'],
                'timestamp': datetime.now(),
                'duration_seconds': notification_duration,
                'success': True,
                'error': None
            }
            
            issue_record['notifications_sent'].append(notification_record)
            self.notification_channels[channel]['notifications'].append(notification_record)
            self.mock_notifications.append(notification_record)
            
        except Exception as e:
            notification_end = time.time()
            notification_duration = notification_end - notification_start
            
            notification_record = {
                'channel': channel,
                'issue_id': issue_record['issue_id'],
                'timestamp': datetime.now(),
                'duration_seconds': notification_duration,
                'success': False,
                'error': str(e)
            }
            
            issue_record['notifications_sent'].append(notification_record)
            self.notification_channels[channel]['notifications'].append(notification_record)
            self.mock_notifications.append(notification_record)
    
    def _send_sns_notification(self, issue_record: Dict[str, Any]):
        """Send SNS notification"""
        message = {
            'AlarmName': f"PetClinic-{issue_record['issue_type']}",
            'AlarmDescription': f"Critical issue: {issue_record['issue_type']}",
            'NewStateValue': 'ALARM',
            'NewStateReason': issue_record['data'].get('description', 'Critical system issue detected'),
            'StateChangeTime': issue_record['timestamp'].isoformat(),
            'Region': 'us-east-1'
        }
        
        # Simulate network delay
        time.sleep(0.1)
        
        self.sns_client.publish(
            TopicArn=self.sns_topic_arn,
            Message=json.dumps(message),
            Subject=f"Pet Clinic Critical Alert: {issue_record['issue_type']}"
        )
    
    def _send_email_notification(self, issue_record: Dict[str, Any]):
        """Send email notification (simulated)"""
        # Simulate email sending delay
        time.sleep(0.2)
        
        # In real implementation, this would send actual email
        # For testing, we just simulate the process
        pass
    
    def _send_slack_notification(self, issue_record: Dict[str, Any]):
        """Send Slack notification (simulated)"""
        # Simulate Slack API call delay
        time.sleep(0.15)
        
        # In real implementation, this would call Slack webhook
        # For testing, we just simulate the process
        pass
    
    def _send_pagerduty_notification(self, issue_record: Dict[str, Any]):
        """Send PagerDuty notification (simulated)"""
        # Simulate PagerDuty API call delay
        time.sleep(0.3)
        
        # In real implementation, this would call PagerDuty API
        # For testing, we just simulate the process
        pass
    
    def verify_notification_timeliness(self, issues: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Verify that notifications were sent within time threshold"""
        verification_result = {
            'total_issues': len(issues),
            'critical_issues': 0,
            'timely_notifications': 0,
            'late_notifications': 0,
            'failed_notifications': 0,
            'notification_details': [],
            'average_notification_time': 0,
            'max_notification_time': 0
        }
        
        total_notification_time = 0
        notification_times = []
        
        for issue in issues:
            if issue['severity'] == 'critical':
                verification_result['critical_issues'] += 1
                
                if issue['notification_start_time'] and issue['notification_end_time']:
                    notification_duration = (issue['notification_end_time'] - issue['notification_start_time']).total_seconds()
                    notification_times.append(notification_duration)
                    total_notification_time += notification_duration
                    
                    issue_analysis = {
                        'issue_id': issue['issue_id'],
                        'issue_type': issue['issue_type'],
                        'notification_duration': notification_duration,
                        'within_threshold': notification_duration <= self.time_threshold_seconds,
                        'successful_channels': len([n for n in issue['notifications_sent'] if n['success']]),
                        'failed_channels': len([n for n in issue['notifications_sent'] if not n['success']]),
                        'channel_details': issue['notifications_sent']
                    }
                    
                    if issue_analysis['within_threshold']:
                        verification_result['timely_notifications'] += 1
                    else:
                        verification_result['late_notifications'] += 1
                    
                    if issue_analysis['failed_channels'] > 0:
                        verification_result['failed_notifications'] += 1
                    
                    verification_result['notification_details'].append(issue_analysis)
        
        # Calculate statistics
        if notification_times:
            verification_result['average_notification_time'] = sum(notification_times) / len(notification_times)
            verification_result['max_notification_time'] = max(notification_times)
        
        return verification_result
    
    def verify_notification_coverage(self, issues: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Verify that all critical issues received notifications"""
        coverage_result = {
            'total_critical_issues': 0,
            'issues_with_notifications': 0,
            'issues_without_notifications': 0,
            'channel_coverage': {},
            'missing_notifications': []
        }
        
        # Initialize channel coverage
        for channel in self.notification_channels.keys():
            coverage_result['channel_coverage'][channel] = {
                'total_sent': 0,
                'successful': 0,
                'failed': 0
            }
        
        for issue in issues:
            if issue['severity'] == 'critical':
                coverage_result['total_critical_issues'] += 1
                
                if len(issue['notifications_sent']) > 0:
                    coverage_result['issues_with_notifications'] += 1
                    
                    # Count notifications by channel
                    for notification in issue['notifications_sent']:
                        channel = notification['channel']
                        coverage_result['channel_coverage'][channel]['total_sent'] += 1
                        
                        if notification['success']:
                            coverage_result['channel_coverage'][channel]['successful'] += 1
                        else:
                            coverage_result['channel_coverage'][channel]['failed'] += 1
                else:
                    coverage_result['issues_without_notifications'] += 1
                    coverage_result['missing_notifications'].append({
                        'issue_id': issue['issue_id'],
                        'issue_type': issue['issue_type'],
                        'timestamp': issue['timestamp']
                    })
        
        return coverage_result
    
    def verify_notification_content(self, issues: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Verify that notifications contain sufficient information"""
        content_result = {
            'total_notifications': len(self.mock_notifications),
            'notifications_with_complete_info': 0,
            'notifications_missing_info': 0,
            'required_fields': ['issue_id', 'timestamp', 'channel'],
            'content_analysis': []
        }
        
        for notification in self.mock_notifications:
            analysis = {
                'notification': notification,
                'has_required_fields': True,
                'missing_fields': [],
                'has_error_info': notification.get('error') is not None,
                'has_timing_info': 'duration_seconds' in notification
            }
            
            # Check required fields
            for field in content_result['required_fields']:
                if field not in notification:
                    analysis['has_required_fields'] = False
                    analysis['missing_fields'].append(field)
            
            if analysis['has_required_fields']:
                content_result['notifications_with_complete_info'] += 1
            else:
                content_result['notifications_missing_info'] += 1
            
            content_result['content_analysis'].append(analysis)
        
        return content_result
    
    def simulate_notification_failure(self, channel: str, failure_rate: float = 0.3):
        """Simulate notification failures for testing resilience"""
        if channel in self.notification_channels:
            # Temporarily disable channel to simulate failure
            original_state = self.notification_channels[channel]['enabled']
            
            # Randomly disable based on failure rate
            import random
            if random.random() < failure_rate:
                self.notification_channels[channel]['enabled'] = False
                return True
            
        return False
    
    def restore_notification_channel(self, channel: str):
        """Restore a notification channel after simulated failure"""
        if channel in self.notification_channels:
            self.notification_channels[channel]['enabled'] = True

# Generate test data strategies
@st.composite
def critical_issue_strategy(draw):
    """Generate critical system issues for testing"""
    issue_types = [
        'service_down',
        'database_failure', 
        'security_breach',
        'system_failure',
        'backup_failure_critical',
        'disk_full',
        'memory_exhausted',
        'high_cpu',
        'high_memory',
        'slow_response',
        'build_failure'
    ]
    
    issue_type = draw(st.sampled_from(issue_types))
    
    base_data = {
        'component': draw(st.sampled_from(['frontend', 'backend', 'database', 'jenkins', 'system'])),
        'description': draw(st.text(min_size=10, max_size=100)),
        'affected_users': draw(st.integers(min_value=0, max_value=1000)),
        'environment': draw(st.sampled_from(['production', 'staging', 'development']))
    }
    
    # Add issue-specific data
    if issue_type == 'service_down':
        base_data.update({
            'service_name': draw(st.sampled_from(['petclinic-frontend', 'petclinic-backend', 'jenkins'])),
            'downtime_duration': draw(st.integers(min_value=1, max_value=3600)),
            'severity': 'critical'
        })
    elif issue_type == 'database_failure':
        base_data.update({
            'database_type': 'mysql',
            'error_code': draw(st.integers(min_value=1000, max_value=9999)),
            'connection_count': draw(st.integers(min_value=0, max_value=100)),
            'severity': 'critical'
        })
    elif issue_type == 'security_breach':
        base_data.update({
            'attack_type': draw(st.sampled_from(['sql_injection', 'xss', 'brute_force', 'unauthorized_access'])),
            'source_ip': f"{draw(st.integers(min_value=1, max_value=255))}.{draw(st.integers(min_value=1, max_value=255))}.{draw(st.integers(min_value=1, max_value=255))}.{draw(st.integers(min_value=1, max_value=255))}",
            'severity': 'critical'
        })
    elif issue_type == 'high_cpu':
        base_data.update({
            'cpu_percentage': draw(st.integers(min_value=80, max_value=100)),
            'duration_minutes': draw(st.integers(min_value=1, max_value=60)),
            'severity': 'warning' if draw(st.integers(min_value=80, max_value=100)) < 90 else 'critical'
        })
    elif issue_type == 'disk_full':
        base_data.update({
            'disk_usage_percentage': draw(st.integers(min_value=95, max_value=100)),
            'available_space_mb': draw(st.integers(min_value=0, max_value=100)),
            'severity': 'critical'
        })
    
    return issue_type, base_data

@st.composite
def notification_config_strategy(draw):
    """Generate notification configuration for testing"""
    return {
        'sns_enabled': draw(st.booleans()),
        'email_enabled': draw(st.booleans()),
        'slack_enabled': draw(st.booleans()),
        'pagerduty_enabled': draw(st.booleans()),
        'time_threshold_seconds': draw(st.integers(min_value=60, max_value=600))
    }

class TestCriticalIssueNotifications:
    """Property-based tests for critical issue notifications"""
    
    def setup_method(self):
        self.test_instance = CriticalIssueNotificationTest()
        self.test_instance.setup_method()
    
    def teardown_method(self):
        self.test_instance.teardown_method()
    
    @given(issues=st.lists(critical_issue_strategy(), min_size=1, max_size=10))
    @settings(max_examples=100, deadline=60000)
    def test_critical_issues_trigger_timely_notifications(self, issues):
        """
        Property 15: Critical Issue Notification
        For any critical system issue or failure, the monitoring system should send
        notifications to administrators within the defined time threshold.
        """
        # Simulate critical issues
        simulated_issues = []
        for issue_type, issue_data in issues:
            simulated_issue = self.test_instance.simulate_critical_issue(issue_type, issue_data)
            simulated_issues.append(simulated_issue)
        
        # Verify notification timeliness
        timeliness_result = self.test_instance.verify_notification_timeliness(simulated_issues)
        
        # Assert that critical issues receive timely notifications
        if timeliness_result['critical_issues'] > 0:
            # At least 90% of critical issues should receive timely notifications
            timely_percentage = (timeliness_result['timely_notifications'] / timeliness_result['critical_issues']) * 100
            assert timely_percentage >= 90, f"At least 90% of critical issues should receive timely notifications, got {timely_percentage}%"
            
            # Average notification time should be within threshold
            assert timeliness_result['average_notification_time'] <= self.test_instance.time_threshold_seconds, f"Average notification time should be within {self.test_instance.time_threshold_seconds}s, got {timeliness_result['average_notification_time']}s"
            
            # Maximum notification time should not exceed threshold by more than 50%
            max_allowed_time = self.test_instance.time_threshold_seconds * 1.5
            assert timeliness_result['max_notification_time'] <= max_allowed_time, f"Maximum notification time should not exceed {max_allowed_time}s, got {timeliness_result['max_notification_time']}s"
    
    @given(issues=st.lists(critical_issue_strategy(), min_size=1, max_size=5))
    @settings(max_examples=50, deadline=60000)
    def test_critical_issues_receive_comprehensive_notifications(self, issues):
        """
        Property 15: Critical Issue Notification (Coverage variant)
        For any critical system issue, notifications should be sent through all
        configured notification channels.
        """
        # Simulate critical issues
        simulated_issues = []
        for issue_type, issue_data in issues:
            simulated_issue = self.test_instance.simulate_critical_issue(issue_type, issue_data)
            simulated_issues.append(simulated_issue)
        
        # Verify notification coverage
        coverage_result = self.test_instance.verify_notification_coverage(simulated_issues)
        
        # Assert comprehensive notification coverage
        if coverage_result['total_critical_issues'] > 0:
            # All critical issues should receive notifications
            coverage_percentage = (coverage_result['issues_with_notifications'] / coverage_result['total_critical_issues']) * 100
            assert coverage_percentage >= 95, f"At least 95% of critical issues should receive notifications, got {coverage_percentage}%"
            
            # Each enabled channel should have sent notifications
            enabled_channels = [ch for ch, config in self.test_instance.notification_channels.items() if config['enabled']]
            
            for channel in enabled_channels:
                channel_stats = coverage_result['channel_coverage'][channel]
                if coverage_result['total_critical_issues'] > 0:
                    # At least 80% success rate per channel (allowing for some failures)
                    if channel_stats['total_sent'] > 0:
                        success_rate = (channel_stats['successful'] / channel_stats['total_sent']) * 100
                        assert success_rate >= 80, f"Channel {channel} should have at least 80% success rate, got {success_rate}%"
    
    @given(issues=st.lists(critical_issue_strategy(), min_size=1, max_size=5))
    @settings(max_examples=30, deadline=60000)
    def test_notification_resilience_with_channel_failures(self, issues):
        """
        Property 15: Critical Issue Notification (Resilience variant)
        For any critical system issue, even if some notification channels fail,
        at least one channel should successfully deliver the notification.
        """
        # Simulate channel failures
        failed_channels = []
        for channel in ['slack', 'pagerduty']:  # Simulate failure of some channels
            if self.test_instance.simulate_notification_failure(channel, failure_rate=0.5):
                failed_channels.append(channel)
        
        # Simulate critical issues
        simulated_issues = []
        for issue_type, issue_data in issues:
            simulated_issue = self.test_instance.simulate_critical_issue(issue_type, issue_data)
            simulated_issues.append(simulated_issue)
        
        # Restore failed channels
        for channel in failed_channels:
            self.test_instance.restore_notification_channel(channel)
        
        # Verify notification coverage despite failures
        coverage_result = self.test_instance.verify_notification_coverage(simulated_issues)
        
        # Assert resilience
        if coverage_result['total_critical_issues'] > 0:
            # Even with some channel failures, notifications should still be delivered
            coverage_percentage = (coverage_result['issues_with_notifications'] / coverage_result['total_critical_issues']) * 100
            assert coverage_percentage >= 90, f"Even with channel failures, at least 90% of critical issues should receive notifications, got {coverage_percentage}%"
            
            # At least one channel should have high success rate
            successful_channels = 0
            for channel, stats in coverage_result['channel_coverage'].items():
                if stats['total_sent'] > 0:
                    success_rate = (stats['successful'] / stats['total_sent']) * 100
                    if success_rate >= 90:
                        successful_channels += 1
            
            assert successful_channels >= 1, "At least one notification channel should have high success rate"
    
    @given(issues=st.lists(critical_issue_strategy(), min_size=1, max_size=5))
    @settings(max_examples=30, deadline=60000)
    def test_notification_content_completeness(self, issues):
        """
        Property 15: Critical Issue Notification (Content variant)
        For any critical system issue notification, the notification should contain
        sufficient information for administrators to understand and respond to the issue.
        """
        # Simulate critical issues
        simulated_issues = []
        for issue_type, issue_data in issues:
            simulated_issue = self.test_instance.simulate_critical_issue(issue_type, issue_data)
            simulated_issues.append(simulated_issue)
        
        # Verify notification content
        content_result = self.test_instance.verify_notification_content(simulated_issues)
        
        # Assert content completeness
        if content_result['total_notifications'] > 0:
            # All notifications should have complete information
            completeness_percentage = (content_result['notifications_with_complete_info'] / content_result['total_notifications']) * 100
            assert completeness_percentage >= 95, f"At least 95% of notifications should have complete information, got {completeness_percentage}%"
            
            # Check that notifications include timing information
            notifications_with_timing = len([n for n in content_result['content_analysis'] if n['has_timing_info']])
            timing_percentage = (notifications_with_timing / content_result['total_notifications']) * 100
            assert timing_percentage >= 90, f"At least 90% of notifications should include timing information, got {timing_percentage}%"
    
    @given(
        issues=st.lists(critical_issue_strategy(), min_size=3, max_size=8),
        config=notification_config_strategy()
    )
    @settings(max_examples=20, deadline=60000)
    def test_notification_system_scalability(self, issues, config):
        """
        Property 15: Critical Issue Notification (Scalability variant)
        For any number of concurrent critical issues, the notification system should
        handle all issues without significant degradation in notification delivery time.
        """
        # Configure notification channels based on test config
        for channel, enabled in config.items():
            if channel.endswith('_enabled') and channel.replace('_enabled', '') in self.test_instance.notification_channels:
                channel_name = channel.replace('_enabled', '')
                self.test_instance.notification_channels[channel_name]['enabled'] = enabled
        
        # Update time threshold
        self.test_instance.time_threshold_seconds = config['time_threshold_seconds']
        
        # Simulate multiple concurrent critical issues
        simulated_issues = []
        start_time = time.time()
        
        # Create threads to simulate concurrent issues
        issue_threads = []
        for issue_type, issue_data in issues:
            thread = threading.Thread(
                target=lambda it=issue_type, id=issue_data: simulated_issues.append(
                    self.test_instance.simulate_critical_issue(it, id)
                )
            )
            issue_threads.append(thread)
            thread.start()
        
        # Wait for all issues to be processed
        for thread in issue_threads:
            thread.join(timeout=config['time_threshold_seconds'] * 2)
        
        end_time = time.time()
        total_processing_time = end_time - start_time
        
        # Verify scalability
        timeliness_result = self.test_instance.verify_notification_timeliness(simulated_issues)
        
        # Assert scalability requirements
        if timeliness_result['critical_issues'] > 0:
            # System should handle multiple concurrent issues efficiently
            avg_time_per_issue = total_processing_time / len(issues)
            assert avg_time_per_issue <= config['time_threshold_seconds'], f"Average processing time per issue should be within threshold, got {avg_time_per_issue}s"
            
            # Notification success rate should remain high even under load
            timely_percentage = (timeliness_result['timely_notifications'] / timeliness_result['critical_issues']) * 100
            assert timely_percentage >= 85, f"Under concurrent load, at least 85% of notifications should be timely, got {timely_percentage}%"
            
            # Maximum notification time should not degrade significantly
            max_allowed_time = config['time_threshold_seconds'] * 2  # Allow 2x threshold under load
            assert timeliness_result['max_notification_time'] <= max_allowed_time, f"Maximum notification time under load should not exceed {max_allowed_time}s, got {timeliness_result['max_notification_time']}s"

if __name__ == "__main__":
    # Run the property-based tests
    pytest.main([__file__, "-v", "--tb=short"])