"""
Pytest configuration and shared fixtures for MySQL privilege configuration tests.

This module provides common test fixtures, configuration, and utilities
used across all test modules.
"""

import pytest
import logging
from typing import Dict, Any, Optional
from unittest.mock import Mock, MagicMock
from hypothesis import settings, Verbosity

from mysql_privilege_config.core.models import (
    Environment,
    SecurityLevel,
    MySQLConfiguration,
    PrivilegeReport,
    ConfigurationResult,
    ExecutionResult,
    ValidationResult,
)
from mysql_privilege_config.utils.logging_config import setup_logging


# Configure Hypothesis for property-based testing
settings.register_profile("default", max_examples=100, verbosity=Verbosity.normal)
settings.register_profile("ci", max_examples=200, verbosity=Verbosity.verbose)
settings.register_profile("dev", max_examples=50, verbosity=Verbosity.quiet)

# Load the appropriate profile based on environment
settings.load_profile("default")


@pytest.fixture(scope="session", autouse=True)
def configure_logging():
    """Configure logging for test sessions."""
    setup_logging(
        environment=Environment.LOCAL_DEVELOPMENT,
        enable_console=False,  # Reduce noise during testing
        log_level=None,  # Use default for environment
    )


@pytest.fixture
def mock_mysql_connection():
    """Provide a mock MySQL connection for testing."""
    connection = MagicMock()
    cursor = MagicMock()
    connection.cursor.return_value = cursor
    connection.is_connected.return_value = True
    return connection


@pytest.fixture
def sample_mysql_config():
    """Provide a sample MySQL configuration for testing."""
    return MySQLConfiguration(
        host="localhost",
        port=3306,
        database="test_db",
        username="test_user",
        password="test_pass",
        environment=Environment.LOCAL_DEVELOPMENT,
        security_level=SecurityLevel.PERMISSIVE,
        log_bin_trust_function_creators=True,
        binary_logging=False,
    )


@pytest.fixture
def sample_privilege_report():
    """Provide a sample privilege report for testing."""
    return PrivilegeReport(
        has_super_privilege=False,
        has_create_routine=True,
        binary_logging_enabled=True,
        trust_function_creators_enabled=False,
        mysql_version="8.0.35",
        user_grants=["USAGE ON *.*", "CREATE ROUTINE ON test_db.*"],
        recommended_actions=["Enable log_bin_trust_function_creators"],
    )


@pytest.fixture
def sample_configuration_result():
    """Provide a sample configuration result for testing."""
    result = ConfigurationResult(success=True)
    result.applied_settings = {
        "log_bin_trust_function_creators": True,
        "binary_logging": True,
    }
    result.warnings = ["Binary logging enabled - monitor replication"]
    return result


@pytest.fixture
def sample_execution_result():
    """Provide a sample execution result for testing."""
    result = ExecutionResult(success=True)
    result.affected_rows = 1
    result.execution_time_ms = 150
    result.sql_statements = ["CREATE FUNCTION test_func() RETURNS INT RETURN 1;"]
    return result


@pytest.fixture
def sample_validation_result():
    """Provide a sample validation result for testing."""
    result = ValidationResult(is_valid=True)
    result.privilege_warnings = ["Function creation may require SUPER privilege"]
    result.suggested_modifications = ["Add DEFINER clause to function definition"]
    return result


@pytest.fixture(params=[Environment.LOCAL_DEVELOPMENT, Environment.CI_CD, Environment.PRODUCTION])
def all_environments(request):
    """Parametrized fixture providing all environment types."""
    return request.param


@pytest.fixture(params=[SecurityLevel.PERMISSIVE, SecurityLevel.BALANCED, SecurityLevel.RESTRICTIVE])
def all_security_levels(request):
    """Parametrized fixture providing all security levels."""
    return request.param


@pytest.fixture
def environment_variables():
    """Provide sample environment variables for testing."""
    return {
        "CI": "true",
        "GITHUB_ACTIONS": "true",
        "MYSQL_HOST": "localhost",
        "MYSQL_PORT": "3306",
        "MYSQL_DATABASE": "test_db",
        "MYSQL_USER": "test_user",
        "MYSQL_PASSWORD": "test_pass",
    }


@pytest.fixture
def mysql_error_1419():
    """Provide a mock MySQL ERROR 1419 (SUPER privilege required)."""
    error = Mock()
    error.errno = 1419
    error.msg = "You do not have the SUPER privilege and binary logging is enabled"
    error.sqlstate = "HY000"
    return error


@pytest.fixture
def sample_sql_scripts():
    """Provide sample SQL scripts for testing."""
    return {
        "simple_function": """
            CREATE FUNCTION test_func(x INT) RETURNS INT
            DETERMINISTIC
            READS SQL DATA
            RETURN x * 2;
        """,
        "function_with_definer": """
            CREATE DEFINER=`test_user`@`localhost` FUNCTION test_func_definer(x INT) RETURNS INT
            DETERMINISTIC
            READS SQL DATA
            RETURN x * 2;
        """,
        "procedure": """
            CREATE PROCEDURE test_proc(IN x INT, OUT result INT)
            DETERMINISTIC
            READS SQL DATA
            BEGIN
                SET result = x * 2;
            END;
        """,
        "trigger": """
            CREATE TRIGGER test_trigger
            BEFORE INSERT ON test_table
            FOR EACH ROW
            BEGIN
                SET NEW.created_at = NOW();
            END;
        """,
        "invalid_syntax": """
            CREATE FUNCTION invalid_func(
            RETURN x * 2;
        """,
    }


class MockMySQLConnector:
    """Mock MySQL connector for testing without actual database connection."""
    
    def __init__(self, should_fail: bool = False, error_code: Optional[int] = None):
        self.should_fail = should_fail
        self.error_code = error_code
        self.connected = True
        
    def connect(self, **kwargs):
        if self.should_fail:
            raise Exception(f"Connection failed: {self.error_code}")
        return self
        
    def cursor(self):
        return MockCursor(self.should_fail, self.error_code)
        
    def is_connected(self):
        return self.connected
        
    def close(self):
        self.connected = False


class MockCursor:
    """Mock MySQL cursor for testing."""
    
    def __init__(self, should_fail: bool = False, error_code: Optional[int] = None):
        self.should_fail = should_fail
        self.error_code = error_code
        self.results = []
        
    def execute(self, query, params=None):
        if self.should_fail and self.error_code:
            error = Mock()
            error.errno = self.error_code
            error.msg = f"MySQL Error {self.error_code}"
            raise error
            
    def fetchall(self):
        return self.results
        
    def fetchone(self):
        return self.results[0] if self.results else None
        
    def close(self):
        pass


@pytest.fixture
def mock_mysql_connector():
    """Provide a mock MySQL connector for testing."""
    return MockMySQLConnector()


@pytest.fixture
def mock_failing_mysql_connector():
    """Provide a mock MySQL connector that fails with ERROR 1419."""
    return MockMySQLConnector(should_fail=True, error_code=1419)


# Utility functions for tests
def assert_configuration_valid(config: MySQLConfiguration) -> None:
    """Assert that a MySQL configuration is valid."""
    assert isinstance(config.environment, Environment)
    assert isinstance(config.security_level, SecurityLevel)
    assert isinstance(config.host, str)
    assert isinstance(config.port, int)
    assert 1 <= config.port <= 65535
    assert config.retry_attempts >= 0
    assert config.timeout_seconds > 0


def assert_privilege_report_valid(report: PrivilegeReport) -> None:
    """Assert that a privilege report is valid."""
    assert isinstance(report.has_super_privilege, bool)
    assert isinstance(report.has_create_routine, bool)
    assert isinstance(report.binary_logging_enabled, bool)
    assert isinstance(report.trust_function_creators_enabled, bool)
    assert isinstance(report.recommended_actions, list)
    assert isinstance(report.user_grants, list)