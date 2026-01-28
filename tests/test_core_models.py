"""
Unit tests for core data models and enums.

This module tests the fundamental data structures used throughout the
MySQL privilege configuration system.
"""

import pytest
from dataclasses import asdict
from mysql_privilege_config.core.models import (
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
    LogLevel,
)


class TestEnvironmentEnum:
    """Test Environment enum values and behavior."""
    
    def test_environment_values(self):
        """Test that Environment enum has expected values."""
        assert Environment.LOCAL_DEVELOPMENT.value == "local"
        assert Environment.CI_CD.value == "ci"
        assert Environment.PRODUCTION.value == "production"
    
    def test_environment_count(self):
        """Test that Environment enum has exactly 3 values."""
        assert len(Environment) == 3


class TestSecurityLevelEnum:
    """Test SecurityLevel enum values and behavior."""
    
    def test_security_level_values(self):
        """Test that SecurityLevel enum has expected values."""
        assert SecurityLevel.PERMISSIVE.value == "permissive"
        assert SecurityLevel.BALANCED.value == "balanced"
        assert SecurityLevel.RESTRICTIVE.value == "restrictive"
    
    def test_security_level_count(self):
        """Test that SecurityLevel enum has exactly 3 values."""
        assert len(SecurityLevel) == 3


class TestMySQLConfiguration:
    """Test MySQLConfiguration dataclass."""
    
    def test_default_configuration(self):
        """Test default MySQL configuration values."""
        config = MySQLConfiguration()
        
        assert config.log_bin_trust_function_creators is False
        assert config.binary_logging is True
        assert config.environment == Environment.LOCAL_DEVELOPMENT
        assert config.security_level == SecurityLevel.PERMISSIVE
        assert config.host == "localhost"
        assert config.port == 3306
        assert config.database == ""
        assert config.username == ""
        assert config.password == ""
        assert config.retry_attempts == 3
        assert config.timeout_seconds == 30
        assert config.allowed_definer_users == []
        assert config.restricted_operations == []
    
    def test_custom_configuration(self):
        """Test MySQL configuration with custom values."""
        config = MySQLConfiguration(
            host="mysql.example.com",
            port=3307,
            database="myapp",
            username="appuser",
            password="secret",
            environment=Environment.PRODUCTION,
            security_level=SecurityLevel.RESTRICTIVE,
            log_bin_trust_function_creators=True,
            binary_logging=False,
            retry_attempts=5,
            timeout_seconds=60,
            allowed_definer_users=["appuser", "admin"],
            restricted_operations=["DROP", "TRUNCATE"],
        )
        
        assert config.host == "mysql.example.com"
        assert config.port == 3307
        assert config.database == "myapp"
        assert config.username == "appuser"
        assert config.password == "secret"
        assert config.environment == Environment.PRODUCTION
        assert config.security_level == SecurityLevel.RESTRICTIVE
        assert config.log_bin_trust_function_creators is True
        assert config.binary_logging is False
        assert config.retry_attempts == 5
        assert config.timeout_seconds == 60
        assert config.allowed_definer_users == ["appuser", "admin"]
        assert config.restricted_operations == ["DROP", "TRUNCATE"]
    
    def test_to_dict_conversion(self):
        """Test conversion of configuration to dictionary."""
        config = MySQLConfiguration(
            host="testhost",
            port=3306,
            database="testdb",
            username="testuser",
            environment=Environment.CI_CD,
            security_level=SecurityLevel.BALANCED,
        )
        
        config_dict = config.to_dict()
        
        assert isinstance(config_dict, dict)
        assert config_dict["host"] == "testhost"
        assert config_dict["port"] == 3306
        assert config_dict["database"] == "testdb"
        assert config_dict["username"] == "testuser"
        assert config_dict["environment"] == "ci"
        assert config_dict["security_level"] == "balanced"
        assert "password" not in config_dict  # Password should not be in dict


class TestConfigurationResult:
    """Test ConfigurationResult dataclass."""
    
    def test_default_configuration_result(self):
        """Test default ConfigurationResult values."""
        result = ConfigurationResult(success=True)
        
        assert result.success is True
        assert result.applied_settings == {}
        assert result.warnings == []
        assert result.errors == []
    
    def test_add_warning(self):
        """Test adding warnings to configuration result."""
        result = ConfigurationResult(success=True)
        result.add_warning("Test warning")
        
        assert len(result.warnings) == 1
        assert result.warnings[0] == "Test warning"
        assert result.success is True  # Warnings don't affect success
    
    def test_add_error(self):
        """Test adding errors to configuration result."""
        result = ConfigurationResult(success=True)
        result.add_error("Test error")
        
        assert len(result.errors) == 1
        assert result.errors[0] == "Test error"
        assert result.success is False  # Errors set success to False


class TestPrivilegeReport:
    """Test PrivilegeReport dataclass."""
    
    def test_default_privilege_report(self):
        """Test default PrivilegeReport values."""
        report = PrivilegeReport()
        
        assert report.has_super_privilege is False
        assert report.has_create_routine is False
        assert report.binary_logging_enabled is False
        assert report.trust_function_creators_enabled is False
        assert report.recommended_actions == []
        assert report.mysql_version == ""
        assert report.user_grants == []
    
    def test_add_recommendation(self):
        """Test adding recommendations to privilege report."""
        report = PrivilegeReport()
        report.add_recommendation("Enable log_bin_trust_function_creators")
        
        assert len(report.recommended_actions) == 1
        assert report.recommended_actions[0] == "Enable log_bin_trust_function_creators"


class TestExecutionResult:
    """Test ExecutionResult dataclass."""
    
    def test_default_execution_result(self):
        """Test default ExecutionResult values."""
        result = ExecutionResult(success=True)
        
        assert result.success is True
        assert result.affected_rows == 0
        assert result.execution_time_ms == 0
        assert result.warnings == []
        assert result.errors == []
        assert result.sql_statements == []
    
    def test_add_warning_and_error(self):
        """Test adding warnings and errors to execution result."""
        result = ExecutionResult(success=True)
        result.add_warning("Test warning")
        result.add_error("Test error")
        
        assert len(result.warnings) == 1
        assert len(result.errors) == 1
        assert result.success is False  # Errors set success to False


class TestValidationResult:
    """Test ValidationResult dataclass."""
    
    def test_default_validation_result(self):
        """Test default ValidationResult values."""
        result = ValidationResult(is_valid=True)
        
        assert result.is_valid is True
        assert result.syntax_errors == []
        assert result.privilege_warnings == []
        assert result.security_concerns == []
        assert result.suggested_modifications == []
    
    def test_add_syntax_error(self):
        """Test adding syntax errors to validation result."""
        result = ValidationResult(is_valid=True)
        result.add_syntax_error("Missing semicolon")
        
        assert len(result.syntax_errors) == 1
        assert result.syntax_errors[0] == "Missing semicolon"
        assert result.is_valid is False  # Syntax errors set is_valid to False
    
    def test_add_various_items(self):
        """Test adding various types of items to validation result."""
        result = ValidationResult(is_valid=True)
        result.add_privilege_warning("May need SUPER privilege")
        result.add_security_concern("Function is non-deterministic")
        result.add_modification_suggestion("Add DEFINER clause")
        
        assert len(result.privilege_warnings) == 1
        assert len(result.security_concerns) == 1
        assert len(result.suggested_modifications) == 1
        assert result.is_valid is True  # These don't affect validity


class TestEnvironmentProfile:
    """Test EnvironmentProfile dataclass."""
    
    def test_local_development_profile(self):
        """Test EnvironmentProfile for local development."""
        profile = EnvironmentProfile(name=Environment.LOCAL_DEVELOPMENT)
        
        assert profile.name == Environment.LOCAL_DEVELOPMENT
        assert profile.mysql_config["log_bin_trust_function_creators"] is True
        assert profile.mysql_config["binary_logging"] is False
        assert profile.security_constraints["allow_super_privilege"] is True
        assert profile.security_constraints["require_definer_validation"] is False
        assert profile.script_modifications["add_definer_clauses"] is False
    
    def test_ci_cd_profile(self):
        """Test EnvironmentProfile for CI/CD."""
        profile = EnvironmentProfile(name=Environment.CI_CD)
        
        assert profile.name == Environment.CI_CD
        assert profile.mysql_config["log_bin_trust_function_creators"] is True
        assert profile.mysql_config["binary_logging"] is True
        assert profile.security_constraints["allow_super_privilege"] is False
        assert profile.security_constraints["require_definer_validation"] is True
        assert profile.script_modifications["add_definer_clauses"] is True
    
    def test_production_profile(self):
        """Test EnvironmentProfile for production."""
        profile = EnvironmentProfile(name=Environment.PRODUCTION)
        
        assert profile.name == Environment.PRODUCTION
        assert profile.mysql_config["log_bin_trust_function_creators"] is False
        assert profile.mysql_config["binary_logging"] is True
        assert profile.security_constraints["allow_super_privilege"] is False
        assert profile.security_constraints["require_definer_validation"] is True
        assert profile.script_modifications["add_definer_clauses"] is True
        assert profile.security_constraints["allow_dynamic_sql"] is False


class TestErrorResolution:
    """Test ErrorResolution dataclass."""
    
    def test_default_error_resolution(self):
        """Test default ErrorResolution values."""
        resolution = ErrorResolution(can_retry=True)
        
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.NO_RETRY
        assert resolution.user_instructions == []
        assert resolution.log_level == LogLevel.ERROR
        assert resolution.max_retry_attempts == 3
        assert resolution.retry_delay_seconds == 1
    
    def test_add_instruction(self):
        """Test adding instructions to error resolution."""
        resolution = ErrorResolution(can_retry=True)
        resolution.add_instruction("Check MySQL configuration")
        
        assert len(resolution.user_instructions) == 1
        assert resolution.user_instructions[0] == "Check MySQL configuration"


class TestEnumValues:
    """Test various enum values and completeness."""
    
    def test_retry_strategy_values(self):
        """Test RetryStrategy enum values."""
        assert RetryStrategy.NO_RETRY.value == "no_retry"
        assert RetryStrategy.LINEAR_BACKOFF.value == "linear_backoff"
        assert RetryStrategy.EXPONENTIAL_BACKOFF.value == "exponential_backoff"
    
    def test_log_level_values(self):
        """Test LogLevel enum values."""
        assert LogLevel.DEBUG.value == "debug"
        assert LogLevel.INFO.value == "info"
        assert LogLevel.WARNING.value == "warning"
        assert LogLevel.ERROR.value == "error"
        assert LogLevel.CRITICAL.value == "critical"