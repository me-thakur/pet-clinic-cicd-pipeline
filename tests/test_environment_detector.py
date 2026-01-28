"""
Unit tests for EnvironmentDetector class.

Tests specific CI environment variables, containerization detection edge cases,
and environment detection logic as specified in task 2.3.
"""

import os
import pytest
from unittest.mock import patch, mock_open, MagicMock
from pathlib import Path

from mysql_privilege_config.detectors.environment_detector import EnvironmentDetector
from mysql_privilege_config.core.models import Environment
from mysql_privilege_config.core.exceptions import EnvironmentDetectionError


class TestEnvironmentDetector:
    """Test cases for EnvironmentDetector class."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.detector = EnvironmentDetector()
        # Clear cache between tests
        self.detector._environment_cache = None
    
    def test_init(self):
        """Test EnvironmentDetector initialization."""
        detector = EnvironmentDetector()
        assert detector.logger is not None
        assert detector._environment_cache is None
    
    @pytest.mark.parametrize("ci_var", [
        'CI', 'CONTINUOUS_INTEGRATION', 'GITHUB_ACTIONS', 'JENKINS_URL',
        'JENKINS_HOME', 'GITLAB_CI', 'CIRCLECI', 'TRAVIS', 'BUILDKITE',
        'AZURE_PIPELINES', 'TEAMCITY_VERSION', 'BAMBOO_BUILD_NUMBER',
        'CODEBUILD_BUILD_ID', 'TF_BUILD', 'DRONE', 'SEMAPHORE'
    ])
    def test_ci_environment_detection(self, ci_var):
        """Test CI environment detection with various CI variables."""
        with patch.dict(os.environ, {ci_var: 'true'}, clear=False):
            assert self.detector.is_ci_environment() is True
            assert self.detector.detect_environment() == Environment.CI_CD
    
    def test_ci_environment_false_values(self):
        """Test CI environment detection with false values."""
        false_values = ['false', '0', 'no', 'off', 'False', 'NO', 'OFF']
        
        for false_value in false_values:
            with patch.dict(os.environ, {'CI': false_value}, clear=True):
                assert self.detector.is_ci_environment() is False
    
    def test_ci_environment_no_variables(self):
        """Test CI environment detection with no CI variables."""
        # Use empty environment
        with patch.dict(os.environ, {}, clear=True):
            assert self.detector.is_ci_environment() is False
    
    @pytest.mark.parametrize("prod_var,prod_value", [
        ('NODE_ENV', 'production'),
        ('ENVIRONMENT', 'production'),
        ('ENV', 'production'),
        ('STAGE', 'production'),
        ('DEPLOYMENT_STAGE', 'production'),
        ('APP_ENV', 'production'),
        ('RAILS_ENV', 'production'),
    ])
    def test_production_environment_detection(self, prod_var, prod_value):
        """Test production environment detection with various production variables."""
        with patch.dict(os.environ, {prod_var: prod_value}, clear=True):
            assert self.detector.detect_environment() == Environment.PRODUCTION
    
    def test_production_environment_django_settings(self):
        """Test production environment detection with Django settings."""
        django_settings = [
            'myapp.settings.production',
            'project.settings.production_config',
            'settings.production'
        ]
        
        for setting in django_settings:
            with patch.dict(os.environ, {'DJANGO_SETTINGS_MODULE': setting}, clear=True):
                assert self.detector.detect_environment() == Environment.PRODUCTION
    
    def test_local_development_default(self):
        """Test default to local development environment."""
        # Use empty environment to ensure no CI or production indicators
        with patch.dict(os.environ, {}, clear=True):
            assert self.detector.detect_environment() == Environment.LOCAL_DEVELOPMENT
    
    def test_containerization_detection_dockerenv(self):
        """Test containerization detection via .dockerenv file."""
        with patch('pathlib.Path.exists') as mock_exists:
            mock_exists.return_value = True
            assert self.detector.is_containerized() is True
    
    def test_containerization_detection_env_vars(self):
        """Test containerization detection via environment variables."""
        container_vars = [
            'KUBERNETES_SERVICE_HOST',
            'KUBERNETES_SERVICE_PORT', 
            'DOCKER_CONTAINER',
            'CONTAINER'
        ]
        
        for var in container_vars:
            with patch.dict(os.environ, {var: 'true'}, clear=True):
                assert self.detector.is_containerized() is True
    
    @patch('platform.system')
    def test_containerization_detection_linux_cgroup(self, mock_platform):
        """Test containerization detection via Linux cgroup."""
        mock_platform.return_value = 'Linux'
        
        cgroup_content = """
        12:perf_event:/docker/1234567890abcdef
        11:net_cls,net_prio:/docker/1234567890abcdef
        10:freezer:/docker/1234567890abcdef
        """
        
        with patch('pathlib.Path.exists', return_value=True), \
             patch('pathlib.Path.read_text', return_value=cgroup_content):
            assert self.detector.is_containerized() is True
    
    @patch('platform.system')
    def test_containerization_detection_kubernetes_cgroup(self, mock_platform):
        """Test containerization detection via Kubernetes cgroup."""
        mock_platform.return_value = 'Linux'
        
        cgroup_content = """
        12:perf_event:/kubepods/burstable/pod123/container456
        11:net_cls,net_prio:/kubepods/burstable/pod123/container456
        """
        
        with patch('pathlib.Path.exists', return_value=True), \
             patch('pathlib.Path.read_text', return_value=cgroup_content):
            assert self.detector.is_containerized() is True
    
    def test_containerization_detection_no_indicators(self):
        """Test containerization detection with no indicators."""
        with patch('pathlib.Path.exists', return_value=False), \
             patch.dict(os.environ, {}, clear=True):
            assert self.detector.is_containerized() is False
    
    def test_get_environment_variables(self):
        """Test getting relevant environment variables."""
        test_vars = {
            'CI': 'true',
            'NODE_ENV': 'development',
            'KUBERNETES_SERVICE_HOST': '10.0.0.1',
            'PATH': '/usr/bin:/bin',
            'HOME': '/home/user'
        }
        
        with patch.dict(os.environ, test_vars, clear=True):
            env_vars = self.detector.get_environment_variables()
            
            # Should include the test variables that are set
            assert 'CI' in env_vars
            assert env_vars['CI'] == 'true'
            assert 'NODE_ENV' in env_vars
            assert env_vars['NODE_ENV'] == 'development'
            assert 'PATH' in env_vars
            assert 'HOME' in env_vars
    
    def test_environment_caching(self):
        """Test that environment detection is cached."""
        with patch.dict(os.environ, {'CI': 'true'}, clear=True):
            # First call should detect CI
            env1 = self.detector.detect_environment()
            assert env1 == Environment.CI_CD
            
            # Clear CI variable
            with patch.dict(os.environ, {}, clear=True):
                # Second call should return cached result
                env2 = self.detector.detect_environment()
                assert env2 == Environment.CI_CD  # Still cached
    
    def test_detection_priority_ci_over_production(self):
        """Test that CI detection takes priority over production."""
        with patch.dict(os.environ, {
            'CI': 'true',
            'NODE_ENV': 'production'
        }, clear=True):
            assert self.detector.detect_environment() == Environment.CI_CD
    
    def test_get_detection_summary(self):
        """Test getting comprehensive detection summary."""
        with patch.dict(os.environ, {'CI': 'true'}, clear=True):
            summary = self.detector.get_detection_summary()
            
            assert 'detected_environment' in summary
            assert summary['detected_environment'] == 'ci'
            assert 'is_ci' in summary
            assert summary['is_ci'] is True
            assert 'is_containerized' in summary
            assert 'platform' in summary
            assert 'python_version' in summary
            assert 'container_indicators' in summary
            assert 'ci_variables_found' in summary
            assert 'CI' in summary['ci_variables_found']
    
    def test_detection_summary_with_error(self):
        """Test detection summary when detection fails."""
        with patch.object(self.detector, 'detect_environment', side_effect=Exception("Test error")):
            summary = self.detector.get_detection_summary()
            
            assert 'error' in summary
            assert 'detection_failed' in summary
            assert summary['detection_failed'] is True
    
    def test_container_indicators_diagnostic(self):
        """Test container indicators diagnostic information."""
        with patch('pathlib.Path.exists', return_value=True), \
             patch.dict(os.environ, {'KUBERNETES_SERVICE_HOST': '10.0.0.1'}, clear=True):
            
            indicators = self.detector._get_container_indicators()
            
            assert 'file_/.dockerenv' in indicators
            assert indicators['file_/.dockerenv'] is True
            assert 'env_KUBERNETES_SERVICE_HOST' in indicators
            assert indicators['env_KUBERNETES_SERVICE_HOST'] is True
    
    @patch('platform.system')
    def test_linux_container_detection_non_linux(self, mock_platform):
        """Test Linux container detection on non-Linux systems."""
        mock_platform.return_value = 'Windows'
        assert self.detector._is_linux_container() is False
        
        mock_platform.return_value = 'Darwin'
        assert self.detector._is_linux_container() is False
    
    def test_environment_detection_error_handling(self):
        """Test error handling in environment detection."""
        with patch.object(self.detector, 'get_environment_variables', side_effect=Exception("Test error")):
            with pytest.raises(EnvironmentDetectionError) as exc_info:
                self.detector.detect_environment()
            
            assert "Failed to detect environment" in str(exc_info.value)
            assert exc_info.value.detected_indicators is not None
    
    def test_containerization_error_handling(self):
        """Test error handling in containerization detection."""
        with patch('pathlib.Path.exists', side_effect=Exception("File system error")):
            # Should not raise exception, just return False and log warning
            result = self.detector.is_containerized()
            assert result is False
    
    def test_ci_environment_error_handling(self):
        """Test error handling in CI environment detection."""
        with patch('os.getenv', side_effect=Exception("Environment error")):
            # Should not raise exception, just return False and log warning
            result = self.detector.is_ci_environment()
            assert result is False
    
    def test_production_environment_case_insensitive(self):
        """Test production environment detection is case insensitive."""
        with patch.dict(os.environ, {'NODE_ENV': 'PRODUCTION'}, clear=True):
            assert self.detector.detect_environment() == Environment.PRODUCTION
        
        with patch.dict(os.environ, {'NODE_ENV': 'Production'}, clear=True):
            assert self.detector.detect_environment() == Environment.PRODUCTION