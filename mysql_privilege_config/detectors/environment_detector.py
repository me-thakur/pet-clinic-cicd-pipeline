"""
Environment Detector - Identifies deployment environment for appropriate configuration.

This module detects whether the system is running in local development, CI/CD, 
or production environment by examining environment variables, containerization 
indicators, and other system characteristics.
"""

import os
import platform
from pathlib import Path
from typing import Dict, Optional, Set
from ..core.models import Environment
from ..core.exceptions import EnvironmentDetectionError
from ..utils.logging_config import get_logger


class EnvironmentDetector:
    """
    Identifies the current deployment environment to determine appropriate configuration strategy.
    
    Detection logic:
    1. CI/CD: Checks for common CI environment variables
    2. Containerization: Looks for Docker/Kubernetes indicators
    3. Production: Checks for production-specific markers
    4. Default: Falls back to local development
    """
    
    # Common CI/CD environment variables
    CI_ENVIRONMENT_VARIABLES = {
        'CI',                    # Generic CI indicator
        'CONTINUOUS_INTEGRATION', # Generic CI indicator
        'GITHUB_ACTIONS',        # GitHub Actions
        'JENKINS_URL',           # Jenkins
        'JENKINS_HOME',          # Jenkins
        'GITLAB_CI',             # GitLab CI
        'CIRCLECI',              # CircleCI
        'TRAVIS',                # Travis CI
        'BUILDKITE',             # Buildkite
        'AZURE_PIPELINES',       # Azure DevOps
        'TEAMCITY_VERSION',      # TeamCity
        'BAMBOO_BUILD_NUMBER',   # Bamboo
        'CODEBUILD_BUILD_ID',    # AWS CodeBuild
        'TF_BUILD',              # Azure DevOps (alternative)
        'DRONE',                 # Drone CI
        'SEMAPHORE',             # Semaphore CI
    }
    
    # Production environment indicators
    PRODUCTION_ENVIRONMENT_VARIABLES = {
        'NODE_ENV': 'production',
        'ENVIRONMENT': 'production',
        'ENV': 'production',
        'STAGE': 'production',
        'DEPLOYMENT_STAGE': 'production',
        'APP_ENV': 'production',
        'RAILS_ENV': 'production',
        'DJANGO_SETTINGS_MODULE': lambda x: 'production' in x.lower(),
    }
    
    # Container indicators
    CONTAINER_INDICATORS = {
        'files': [
            '/.dockerenv',           # Docker container indicator
            '/proc/1/cgroup',        # Container cgroup info
        ],
        'env_vars': {
            'KUBERNETES_SERVICE_HOST',  # Kubernetes pod
            'KUBERNETES_SERVICE_PORT',  # Kubernetes pod
            'DOCKER_CONTAINER',         # Docker container
            'CONTAINER',                # Generic container
            'HOSTNAME',                 # Often set in containers
        }
    }
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self._environment_cache: Optional[Environment] = None
    
    def detect_environment(self) -> Environment:
        """
        Detect current deployment environment.
        
        Returns:
            Environment: The detected environment (CI_CD, PRODUCTION, or LOCAL_DEVELOPMENT)
            
        Raises:
            EnvironmentDetectionError: If detection fails critically
        """
        if self._environment_cache is not None:
            return self._environment_cache
            
        try:
            self.logger.debug("Starting environment detection")
            
            # Get all relevant environment variables
            env_vars = self.get_environment_variables()
            
            # Check for CI/CD environment first (highest priority)
            if self.is_ci_environment():
                self.logger.info("Detected CI/CD environment")
                self._environment_cache = Environment.CI_CD
                return Environment.CI_CD
            
            # Check for production environment
            if self._is_production_environment(env_vars):
                self.logger.info("Detected production environment")
                self._environment_cache = Environment.PRODUCTION
                return Environment.PRODUCTION
            
            # Default to local development
            self.logger.info("Defaulting to local development environment")
            self._environment_cache = Environment.LOCAL_DEVELOPMENT
            return Environment.LOCAL_DEVELOPMENT
            
        except Exception as e:
            error_msg = f"Failed to detect environment: {str(e)}"
            self.logger.error(error_msg)
            
            # Collect diagnostic information
            detected_indicators = {
                'ci_vars_found': [var for var in self.CI_ENVIRONMENT_VARIABLES if os.getenv(var)],
                'container_indicators': self._get_container_indicators(),
                'platform': platform.system(),
                'python_version': platform.python_version(),
            }
            
            raise EnvironmentDetectionError(
                error_msg,
                detected_indicators=detected_indicators
            )
    
    def get_environment_variables(self) -> Dict[str, str]:
        """
        Get relevant environment variables for environment detection.
        
        Returns:
            Dict[str, str]: Dictionary of relevant environment variables
        """
        relevant_vars = set()
        
        # Add CI environment variables
        relevant_vars.update(self.CI_ENVIRONMENT_VARIABLES)
        
        # Add production environment variables
        relevant_vars.update(self.PRODUCTION_ENVIRONMENT_VARIABLES.keys())
        
        # Add container environment variables
        relevant_vars.update(self.CONTAINER_INDICATORS['env_vars'])
        
        # Add some common system variables
        relevant_vars.update({
            'PATH', 'HOME', 'USER', 'HOSTNAME', 'PWD',
            'SHELL', 'TERM', 'LANG', 'LC_ALL'
        })
        
        # Get values for all relevant variables
        env_vars = {}
        for var in relevant_vars:
            value = os.getenv(var)
            if value is not None:
                env_vars[var] = value
        
        self.logger.debug(f"Found {len(env_vars)} relevant environment variables")
        return env_vars
    
    def is_containerized(self) -> bool:
        """
        Check if running in containerized environment (Docker, Kubernetes).
        
        Returns:
            bool: True if running in a container, False otherwise
        """
        try:
            # Check for container indicator files
            for file_path in self.CONTAINER_INDICATORS['files']:
                if Path(file_path).exists():
                    self.logger.debug(f"Found container indicator file: {file_path}")
                    return True
            
            # Check for container environment variables
            for env_var in self.CONTAINER_INDICATORS['env_vars']:
                if os.getenv(env_var):
                    self.logger.debug(f"Found container indicator env var: {env_var}")
                    return True
            
            # Check cgroup information (Linux containers)
            if self._is_linux_container():
                return True
            
            return False
            
        except Exception as e:
            self.logger.warning(f"Error checking containerization: {e}")
            return False
    
    def is_ci_environment(self) -> bool:
        """
        Check if running in CI/CD environment.
        
        Returns:
            bool: True if running in CI/CD, False otherwise
        """
        try:
            # Check for any CI environment variables
            for ci_var in self.CI_ENVIRONMENT_VARIABLES:
                value = os.getenv(ci_var)
                if value:
                    # Some CI variables might be set to 'false' or '0'
                    if value.lower() not in ('false', '0', 'no', 'off'):
                        self.logger.debug(f"Found CI indicator: {ci_var}={value}")
                        return True
            
            return False
            
        except Exception as e:
            self.logger.warning(f"Error checking CI environment: {e}")
            return False
    
    def _is_production_environment(self, env_vars: Dict[str, str]) -> bool:
        """
        Check if running in production environment.
        
        Args:
            env_vars: Dictionary of environment variables
            
        Returns:
            bool: True if production environment detected
        """
        try:
            for var_name, expected_value in self.PRODUCTION_ENVIRONMENT_VARIABLES.items():
                actual_value = env_vars.get(var_name)
                if actual_value:
                    if callable(expected_value):
                        # Handle lambda functions for complex checks
                        if expected_value(actual_value):
                            self.logger.debug(f"Production indicator: {var_name}={actual_value}")
                            return True
                    elif actual_value.lower() == expected_value:
                        self.logger.debug(f"Production indicator: {var_name}={actual_value}")
                        return True
            
            return False
            
        except Exception as e:
            self.logger.warning(f"Error checking production environment: {e}")
            return False
    
    def _is_linux_container(self) -> bool:
        """
        Check if running in a Linux container by examining cgroup information.
        
        Returns:
            bool: True if Linux container detected
        """
        try:
            if platform.system() != 'Linux':
                return False
            
            cgroup_path = Path('/proc/1/cgroup')
            if cgroup_path.exists():
                content = cgroup_path.read_text()
                # Look for container indicators in cgroup
                container_indicators = ['docker', 'kubepods', 'containerd', 'lxc']
                for indicator in container_indicators:
                    if indicator in content.lower():
                        self.logger.debug(f"Found container indicator in cgroup: {indicator}")
                        return True
            
            return False
            
        except Exception as e:
            self.logger.debug(f"Could not check Linux container status: {e}")
            return False
    
    def _get_container_indicators(self) -> Dict[str, bool]:
        """
        Get detailed container indicator information for diagnostics.
        
        Returns:
            Dict[str, bool]: Container indicators and their status
        """
        indicators = {}
        
        # Check files
        for file_path in self.CONTAINER_INDICATORS['files']:
            indicators[f"file_{file_path}"] = Path(file_path).exists()
        
        # Check environment variables
        for env_var in self.CONTAINER_INDICATORS['env_vars']:
            indicators[f"env_{env_var}"] = bool(os.getenv(env_var))
        
        # Check Linux container
        indicators['linux_container'] = self._is_linux_container()
        
        return indicators
    
    def get_detection_summary(self) -> Dict[str, any]:
        """
        Get a comprehensive summary of environment detection results.
        
        Returns:
            Dict[str, any]: Summary of detection results
        """
        try:
            environment = self.detect_environment()
            
            return {
                'detected_environment': environment.value,
                'is_ci': self.is_ci_environment(),
                'is_containerized': self.is_containerized(),
                'platform': platform.system(),
                'python_version': platform.python_version(),
                'container_indicators': self._get_container_indicators(),
                'ci_variables_found': [
                    var for var in self.CI_ENVIRONMENT_VARIABLES 
                    if os.getenv(var)
                ],
                'production_variables_found': [
                    var for var in self.PRODUCTION_ENVIRONMENT_VARIABLES.keys()
                    if os.getenv(var)
                ]
            }
            
        except Exception as e:
            return {
                'error': str(e),
                'detection_failed': True
            }