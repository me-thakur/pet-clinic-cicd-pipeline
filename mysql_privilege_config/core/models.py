"""
Core data models and enums for MySQL privilege configuration.

This module defines the fundamental data structures used throughout the
MySQL privilege configuration system, including environment types,
security levels, and configuration schemas.
"""

from enum import Enum
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Any
import logging


class Environment(Enum):
    """Deployment environment types."""
    LOCAL_DEVELOPMENT = "local"
    CI_CD = "ci"
    PRODUCTION = "production"


class SecurityLevel(Enum):
    """Security constraint levels for different environments."""
    PERMISSIVE = "permissive"    # Local development
    BALANCED = "balanced"        # CI/CD
    RESTRICTIVE = "restrictive"  # Production


class RetryStrategy(Enum):
    """Retry strategies for error handling."""
    NO_RETRY = "no_retry"
    LINEAR_BACKOFF = "linear_backoff"
    EXPONENTIAL_BACKOFF = "exponential_backoff"


class LogLevel(Enum):
    """Logging levels for error handling."""
    DEBUG = "debug"
    INFO = "info"
    WARNING = "warning"
    ERROR = "error"
    CRITICAL = "critical"


@dataclass
class MySQLConfiguration:
    """MySQL configuration settings for privilege management."""
    
    # Core privilege settings
    log_bin_trust_function_creators: bool = False
    binary_logging: bool = True
    
    # Environment-specific settings
    environment: Environment = Environment.LOCAL_DEVELOPMENT
    security_level: SecurityLevel = SecurityLevel.PERMISSIVE
    
    # Connection settings
    host: str = "localhost"
    port: int = 3306
    database: str = ""
    username: str = ""
    password: str = ""
    
    # Script execution settings
    retry_attempts: int = 3
    timeout_seconds: int = 30
    
    # Security constraints
    allowed_definer_users: List[str] = field(default_factory=list)
    restricted_operations: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert configuration to dictionary."""
        return {
            "log_bin_trust_function_creators": self.log_bin_trust_function_creators,
            "binary_logging": self.binary_logging,
            "environment": self.environment.value,
            "security_level": self.security_level.value,
            "host": self.host,
            "port": self.port,
            "database": self.database,
            "username": self.username,
            "retry_attempts": self.retry_attempts,
            "timeout_seconds": self.timeout_seconds,
            "allowed_definer_users": self.allowed_definer_users,
            "restricted_operations": self.restricted_operations,
        }


@dataclass
class ConfigurationResult:
    """Result of applying MySQL configuration."""
    success: bool
    applied_settings: Dict[str, Any] = field(default_factory=dict)
    warnings: List[str] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)
    
    def add_warning(self, message: str) -> None:
        """Add a warning message."""
        self.warnings.append(message)
        
    def add_error(self, message: str) -> None:
        """Add an error message."""
        self.errors.append(message)
        self.success = False


@dataclass
class PrivilegeReport:
    """Report of MySQL user privileges and system configuration."""
    has_super_privilege: bool = False
    has_create_routine: bool = False
    binary_logging_enabled: bool = False
    trust_function_creators_enabled: bool = False
    recommended_actions: List[str] = field(default_factory=list)
    mysql_version: str = ""
    user_grants: List[str] = field(default_factory=list)
    
    def add_recommendation(self, action: str) -> None:
        """Add a recommended action."""
        self.recommended_actions.append(action)


@dataclass
class ExecutionResult:
    """Result of executing a database script."""
    success: bool
    affected_rows: int = 0
    execution_time_ms: int = 0
    warnings: List[str] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)
    sql_statements: List[str] = field(default_factory=list)
    
    def add_warning(self, message: str) -> None:
        """Add a warning message."""
        self.warnings.append(message)
        
    def add_error(self, message: str) -> None:
        """Add an error message."""
        self.errors.append(message)
        self.success = False


@dataclass
class ValidationResult:
    """Result of validating a database script."""
    is_valid: bool
    syntax_errors: List[str] = field(default_factory=list)
    privilege_warnings: List[str] = field(default_factory=list)
    security_concerns: List[str] = field(default_factory=list)
    suggested_modifications: List[str] = field(default_factory=list)
    
    def add_syntax_error(self, error: str) -> None:
        """Add a syntax error."""
        self.syntax_errors.append(error)
        self.is_valid = False
        
    def add_privilege_warning(self, warning: str) -> None:
        """Add a privilege warning."""
        self.privilege_warnings.append(warning)
        
    def add_security_concern(self, concern: str) -> None:
        """Add a security concern."""
        self.security_concerns.append(concern)
        
    def add_modification_suggestion(self, suggestion: str) -> None:
        """Add a suggested modification."""
        self.suggested_modifications.append(suggestion)


@dataclass
class ExecutionOptions:
    """Options for script execution."""
    environment: Environment = Environment.LOCAL_DEVELOPMENT
    retry_on_privilege_error: bool = True
    apply_definer_workarounds: bool = True
    validate_before_execution: bool = True
    log_sql_statements: bool = False
    transaction_isolation_level: Optional[str] = None


@dataclass
class EnvironmentProfile:
    """Configuration profile for a specific environment."""
    name: Environment
    mysql_config: Dict[str, Any] = field(default_factory=dict)
    security_constraints: Dict[str, bool] = field(default_factory=dict)
    script_modifications: Dict[str, bool] = field(default_factory=dict)
    
    def __post_init__(self):
        """Initialize default values based on environment."""
        if not self.mysql_config:
            self.mysql_config = self._get_default_mysql_config()
        if not self.security_constraints:
            self.security_constraints = self._get_default_security_constraints()
        if not self.script_modifications:
            self.script_modifications = self._get_default_script_modifications()
    
    def _get_default_mysql_config(self) -> Dict[str, Any]:
        """Get default MySQL configuration for this environment."""
        if self.name == Environment.LOCAL_DEVELOPMENT:
            return {
                "log_bin_trust_function_creators": True,
                "binary_logging": False,
                "additional_settings": {}
            }
        elif self.name == Environment.CI_CD:
            return {
                "log_bin_trust_function_creators": True,
                "binary_logging": True,
                "additional_settings": {"innodb_flush_log_at_trx_commit": 2}
            }
        else:  # PRODUCTION
            return {
                "log_bin_trust_function_creators": False,
                "binary_logging": True,
                "additional_settings": {"innodb_flush_log_at_trx_commit": 1}
            }
    
    def _get_default_security_constraints(self) -> Dict[str, bool]:
        """Get default security constraints for this environment."""
        if self.name == Environment.LOCAL_DEVELOPMENT:
            return {
                "allow_super_privilege": True,
                "require_definer_validation": False,
                "allow_dynamic_sql": True
            }
        elif self.name == Environment.CI_CD:
            return {
                "allow_super_privilege": False,
                "require_definer_validation": True,
                "allow_dynamic_sql": True
            }
        else:  # PRODUCTION
            return {
                "allow_super_privilege": False,
                "require_definer_validation": True,
                "allow_dynamic_sql": False
            }
    
    def _get_default_script_modifications(self) -> Dict[str, bool]:
        """Get default script modifications for this environment."""
        if self.name == Environment.LOCAL_DEVELOPMENT:
            return {
                "add_definer_clauses": False,
                "wrap_in_transactions": False,
                "add_error_handling": False
            }
        elif self.name == Environment.CI_CD:
            return {
                "add_definer_clauses": True,
                "wrap_in_transactions": True,
                "add_error_handling": True
            }
        else:  # PRODUCTION
            return {
                "add_definer_clauses": True,
                "wrap_in_transactions": True,
                "add_error_handling": True
            }


@dataclass
class ErrorResolution:
    """Resolution strategy for handling errors."""
    can_retry: bool
    retry_strategy: RetryStrategy = RetryStrategy.NO_RETRY
    user_instructions: List[str] = field(default_factory=list)
    log_level: LogLevel = LogLevel.ERROR
    max_retry_attempts: int = 3
    retry_delay_seconds: int = 1
    
    def add_instruction(self, instruction: str) -> None:
        """Add a user instruction."""
        self.user_instructions.append(instruction)