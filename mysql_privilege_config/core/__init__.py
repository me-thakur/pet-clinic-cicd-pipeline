"""
Core module containing data models, enums, and base classes.
"""

from .models import (
    Environment,
    SecurityLevel,
    MySQLConfiguration,
    ConfigurationResult,
    PrivilegeReport,
    ExecutionResult,
    ValidationResult,
    ExecutionOptions,
    EnvironmentProfile,
    ErrorResolution,
    RetryStrategy,
)

from .exceptions import (
    MySQLPrivilegeError,
    ConfigurationError,
    EnvironmentDetectionError,
    ScriptExecutionError,
    PrivilegeValidationError,
)

__all__ = [
    "Environment",
    "SecurityLevel",
    "MySQLConfiguration", 
    "ConfigurationResult",
    "PrivilegeReport",
    "ExecutionResult",
    "ValidationResult",
    "ExecutionOptions",
    "EnvironmentProfile",
    "ErrorResolution",
    "RetryStrategy",
    "MySQLPrivilegeError",
    "ConfigurationError",
    "EnvironmentDetectionError",
    "ScriptExecutionError",
    "PrivilegeValidationError",
]