"""
Unit tests for core exception classes.

This module tests the custom exception classes used throughout the
MySQL privilege configuration system.
"""

import pytest
from mysql_privilege_config.core.exceptions import (
    MySQLPrivilegeError,
    ConfigurationError,
    EnvironmentDetectionError,
    ScriptExecutionError,
    PrivilegeValidationError,
    SuperPrivilegeRequiredError,
    BinaryLoggingError,
    ConnectionError,
)


class TestMySQLPrivilegeError:
    """Test base MySQLPrivilegeError exception."""
    
    def test_basic_exception(self):
        """Test basic exception creation and properties."""
        error = MySQLPrivilegeError("Test error message")
        
        assert str(error) == "Test error message"
        assert error.mysql_error_code is None
        assert error.resolution_steps == []
    
    def test_exception_with_error_code(self):
        """Test exception with MySQL error code."""
        error = MySQLPrivilegeError("Test error", mysql_error_code=1419)
        
        assert str(error) == "Test error"
        assert error.mysql_error_code == 1419
        assert error.resolution_steps == []
    
    def test_exception_with_resolution_steps(self):
        """Test exception with resolution steps."""
        steps = ["Step 1", "Step 2"]
        error = MySQLPrivilegeError("Test error", resolution_steps=steps)
        
        assert str(error) == "Test error"
        assert error.resolution_steps == steps
    
    def test_add_resolution_step(self):
        """Test adding resolution steps to exception."""
        error = MySQLPrivilegeError("Test error")
        error.add_resolution_step("New step")
        
        assert len(error.resolution_steps) == 1
        assert error.resolution_steps[0] == "New step"
        
        error.add_resolution_step("Another step")
        assert len(error.resolution_steps) == 2
        assert error.resolution_steps[1] == "Another step"


class TestConfigurationError:
    """Test ConfigurationError exception."""
    
    def test_basic_configuration_error(self):
        """Test basic configuration error."""
        error = ConfigurationError("Configuration failed")
        
        assert str(error) == "Configuration failed"
        assert error.failed_settings == {}
        assert error.mysql_error_code is None
    
    def test_configuration_error_with_failed_settings(self):
        """Test configuration error with failed settings."""
        failed_settings = {"log_bin_trust_function_creators": True, "binary_logging": False}
        error = ConfigurationError("Configuration failed", failed_settings=failed_settings)
        
        assert str(error) == "Configuration failed"
        assert error.failed_settings == failed_settings
    
    def test_configuration_error_inheritance(self):
        """Test that ConfigurationError inherits from MySQLPrivilegeError."""
        error = ConfigurationError("Test error")
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, ConfigurationError)


class TestEnvironmentDetectionError:
    """Test EnvironmentDetectionError exception."""
    
    def test_basic_environment_detection_error(self):
        """Test basic environment detection error."""
        error = EnvironmentDetectionError("Environment detection failed")
        
        assert str(error) == "Environment detection failed"
        assert error.detected_indicators == {}
    
    def test_environment_detection_error_with_indicators(self):
        """Test environment detection error with detected indicators."""
        indicators = {"CI": "true", "GITHUB_ACTIONS": "false"}
        error = EnvironmentDetectionError("Detection failed", detected_indicators=indicators)
        
        assert str(error) == "Detection failed"
        assert error.detected_indicators == indicators
    
    def test_environment_detection_error_inheritance(self):
        """Test that EnvironmentDetectionError inherits from MySQLPrivilegeError."""
        error = EnvironmentDetectionError("Test error")
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, EnvironmentDetectionError)


class TestScriptExecutionError:
    """Test ScriptExecutionError exception."""
    
    def test_basic_script_execution_error(self):
        """Test basic script execution error."""
        error = ScriptExecutionError("Script execution failed")
        
        assert str(error) == "Script execution failed"
        assert error.failed_statement is None
        assert error.statement_index is None
    
    def test_script_execution_error_with_statement(self):
        """Test script execution error with failed statement."""
        statement = "CREATE FUNCTION test() RETURNS INT RETURN 1;"
        error = ScriptExecutionError("Execution failed", failed_statement=statement, statement_index=2)
        
        assert str(error) == "Execution failed"
        assert error.failed_statement == statement
        assert error.statement_index == 2
    
    def test_script_execution_error_inheritance(self):
        """Test that ScriptExecutionError inherits from MySQLPrivilegeError."""
        error = ScriptExecutionError("Test error")
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, ScriptExecutionError)


class TestPrivilegeValidationError:
    """Test PrivilegeValidationError exception."""
    
    def test_basic_privilege_validation_error(self):
        """Test basic privilege validation error."""
        error = PrivilegeValidationError("Privilege validation failed")
        
        assert str(error) == "Privilege validation failed"
        assert error.missing_privileges == []
        assert error.current_grants == []
    
    def test_privilege_validation_error_with_privileges(self):
        """Test privilege validation error with privilege information."""
        missing = ["SUPER", "CREATE ROUTINE"]
        grants = ["USAGE ON *.*", "SELECT ON test.*"]
        error = PrivilegeValidationError(
            "Validation failed", 
            missing_privileges=missing, 
            current_grants=grants
        )
        
        assert str(error) == "Validation failed"
        assert error.missing_privileges == missing
        assert error.current_grants == grants
    
    def test_privilege_validation_error_inheritance(self):
        """Test that PrivilegeValidationError inherits from MySQLPrivilegeError."""
        error = PrivilegeValidationError("Test error")
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, PrivilegeValidationError)


class TestSuperPrivilegeRequiredError:
    """Test SuperPrivilegeRequiredError exception (ERROR 1419)."""
    
    def test_default_super_privilege_error(self):
        """Test default super privilege error."""
        error = SuperPrivilegeRequiredError()
        
        assert "SUPER privilege" in str(error)
        assert "binary logging" in str(error)
        assert error.mysql_error_code == 1419
        assert error.operation is None
        assert len(error.resolution_steps) > 0
        assert any("log_bin_trust_function_creators" in step for step in error.resolution_steps)
    
    def test_super_privilege_error_with_operation(self):
        """Test super privilege error with specific operation."""
        error = SuperPrivilegeRequiredError(operation="CREATE FUNCTION")
        
        assert error.operation == "CREATE FUNCTION"
        assert error.mysql_error_code == 1419
    
    def test_super_privilege_error_with_custom_message(self):
        """Test super privilege error with custom message."""
        custom_message = "Custom SUPER privilege error"
        error = SuperPrivilegeRequiredError(message=custom_message)
        
        assert str(error) == custom_message
        assert error.mysql_error_code == 1419
    
    def test_super_privilege_error_resolution_steps(self):
        """Test that super privilege error has appropriate resolution steps."""
        error = SuperPrivilegeRequiredError()
        
        assert len(error.resolution_steps) >= 3
        steps_text = " ".join(error.resolution_steps)
        assert "log_bin_trust_function_creators" in steps_text
        assert "SUPER privilege" in steps_text
        assert "binary logging" in steps_text
    
    def test_super_privilege_error_inheritance(self):
        """Test that SuperPrivilegeRequiredError inherits from MySQLPrivilegeError."""
        error = SuperPrivilegeRequiredError()
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, SuperPrivilegeRequiredError)


class TestBinaryLoggingError:
    """Test BinaryLoggingError exception."""
    
    def test_basic_binary_logging_error(self):
        """Test basic binary logging error."""
        error = BinaryLoggingError("Binary logging error")
        
        assert str(error) == "Binary logging error"
        assert error.current_setting is None
    
    def test_binary_logging_error_with_setting(self):
        """Test binary logging error with current setting."""
        error = BinaryLoggingError("Binary logging error", current_setting=True)
        
        assert str(error) == "Binary logging error"
        assert error.current_setting is True
    
    def test_binary_logging_error_inheritance(self):
        """Test that BinaryLoggingError inherits from MySQLPrivilegeError."""
        error = BinaryLoggingError("Test error")
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, BinaryLoggingError)


class TestConnectionError:
    """Test ConnectionError exception."""
    
    def test_basic_connection_error(self):
        """Test basic connection error."""
        error = ConnectionError("Connection failed")
        
        assert str(error) == "Connection failed"
        assert error.host is None
        assert error.port is None
        assert error.database is None
    
    def test_connection_error_with_details(self):
        """Test connection error with connection details."""
        error = ConnectionError(
            "Connection failed", 
            host="localhost", 
            port=3306, 
            database="testdb"
        )
        
        assert str(error) == "Connection failed"
        assert error.host == "localhost"
        assert error.port == 3306
        assert error.database == "testdb"
    
    def test_connection_error_inheritance(self):
        """Test that ConnectionError inherits from MySQLPrivilegeError."""
        error = ConnectionError("Test error")
        assert isinstance(error, MySQLPrivilegeError)
        assert isinstance(error, ConnectionError)


class TestExceptionHierarchy:
    """Test exception inheritance hierarchy."""
    
    def test_all_exceptions_inherit_from_base(self):
        """Test that all custom exceptions inherit from MySQLPrivilegeError."""
        exceptions = [
            ConfigurationError("test"),
            EnvironmentDetectionError("test"),
            ScriptExecutionError("test"),
            PrivilegeValidationError("test"),
            SuperPrivilegeRequiredError(),
            BinaryLoggingError("test"),
            ConnectionError("test"),
        ]
        
        for exc in exceptions:
            assert isinstance(exc, MySQLPrivilegeError)
            assert isinstance(exc, Exception)
    
    def test_exception_with_all_parameters(self):
        """Test exception creation with all possible parameters."""
        error = MySQLPrivilegeError(
            "Test message",
            mysql_error_code=1419,
            resolution_steps=["Step 1", "Step 2"]
        )
        
        assert str(error) == "Test message"
        assert error.mysql_error_code == 1419
        assert error.resolution_steps == ["Step 1", "Step 2"]