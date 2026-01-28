"""
MySQL Privilege Configuration Package

This package provides a comprehensive solution for managing MySQL SUPER privilege
issues in CI/CD pipelines by implementing environment-specific configuration
management for database operations.

The package addresses ERROR 1419 that occurs when binary logging is enabled
and database users lack SUPER privileges for creating functions, procedures,
or triggers.
"""

__version__ = "1.0.0"
__author__ = "MySQL Privilege Configuration Team"

from .core.models import Environment, SecurityLevel, MySQLConfiguration
from .core.exceptions import MySQLPrivilegeError, ConfigurationError
from .managers.config_manager import DatabaseConfigManager
from .detectors.environment_detector import EnvironmentDetector
from .validators.privilege_validator import PrivilegeValidator
from .executors.script_executor import ScriptExecutor

__all__ = [
    "Environment",
    "SecurityLevel", 
    "MySQLConfiguration",
    "MySQLPrivilegeError",
    "ConfigurationError",
    "DatabaseConfigManager",
    "EnvironmentDetector",
    "PrivilegeValidator",
    "ScriptExecutor",
]