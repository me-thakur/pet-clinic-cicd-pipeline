"""
Unit tests for logging configuration.

This module tests the logging setup and configuration functionality
for the MySQL privilege configuration system.
"""

import pytest
import logging
import tempfile
from pathlib import Path
from unittest.mock import patch, MagicMock

from mysql_privilege_config.utils.logging_config import (
    setup_logging,
    get_logger,
    configure_mysql_connector_logging,
    log_privilege_operation,
    log_configuration_change,
    _convert_log_level,
    ENVIRONMENT_LOG_LEVELS,
)
from mysql_privilege_config.core.models import Environment, LogLevel


class TestSetupLogging:
    """Test logging setup functionality."""
    
    def test_setup_logging_default(self):
        """Test setup_logging with default parameters."""
        setup_logging()
        
        logger = get_logger("test_module")
        assert logger.level <= logging.DEBUG  # Should be set to appropriate level
        assert len(logger.handlers) == 0  # Logger itself doesn't have handlers, parent does
        
        # Test that root logger has handlers
        root_logger = logging.getLogger("mysql_privilege_config")
        assert len(root_logger.handlers) >= 1  # Should have console handler
    
    def test_setup_logging_with_environment(self):
        """Test setup_logging with different environments."""
        for env in Environment:
            setup_logging(environment=env)
            root_logger = logging.getLogger("mysql_privilege_config")
            expected_level = ENVIRONMENT_LOG_LEVELS.get(env, logging.INFO)
            assert root_logger.level == expected_level
    
    def test_setup_logging_with_log_file(self):
        """Test setup_logging with log file."""
        with tempfile.TemporaryDirectory() as temp_dir:
            log_file = Path(temp_dir) / "test.log"
            setup_logging(log_file=str(log_file))
            
            root_logger = logging.getLogger("mysql_privilege_config")
            
            # Should have both console and file handlers
            handler_types = [type(h).__name__ for h in root_logger.handlers]
            assert "StreamHandler" in handler_types
            assert "RotatingFileHandler" in handler_types
            
            # Log file should be created
            assert log_file.exists()
    
    def test_setup_logging_no_console(self):
        """Test setup_logging with console disabled."""
        setup_logging(enable_console=False)
        
        root_logger = logging.getLogger("mysql_privilege_config")
        handler_types = [type(h).__name__ for h in root_logger.handlers]
        assert "StreamHandler" not in handler_types
    
    def test_setup_logging_custom_format(self):
        """Test setup_logging with custom format."""
        custom_format = "%(name)s - %(levelname)s - %(message)s"
        setup_logging(log_format=custom_format)
        
        root_logger = logging.getLogger("mysql_privilege_config")
        if root_logger.handlers:
            formatter = root_logger.handlers[0].formatter
            assert formatter.format(logging.LogRecord(
                name="test", level=logging.INFO, pathname="", lineno=0,
                msg="test", args=(), exc_info=None
            )).startswith("test - INFO - test")


class TestGetLogger:
    """Test logger retrieval functionality."""
    
    def test_get_logger_basic(self):
        """Test basic logger retrieval."""
        logger = get_logger("test_module")
        
        assert isinstance(logger, logging.Logger)
        assert logger.name == "mysql_privilege_config.test_module"
    
    def test_get_logger_with_full_name(self):
        """Test logger retrieval with full module name."""
        logger = get_logger("mysql_privilege_config.test_module")
        
        assert isinstance(logger, logging.Logger)
        assert logger.name == "mysql_privilege_config.test_module"
    
    def test_get_logger_hierarchy(self):
        """Test that loggers maintain proper hierarchy."""
        parent_logger = get_logger("parent")
        child_logger = get_logger("parent.child")
        
        assert parent_logger.name == "mysql_privilege_config.parent"
        assert child_logger.name == "mysql_privilege_config.parent.child"
        assert child_logger.parent == parent_logger


class TestConvertLogLevel:
    """Test log level conversion functionality."""
    
    def test_convert_all_log_levels(self):
        """Test conversion of all LogLevel enum values."""
        conversions = {
            LogLevel.DEBUG: logging.DEBUG,
            LogLevel.INFO: logging.INFO,
            LogLevel.WARNING: logging.WARNING,
            LogLevel.ERROR: logging.ERROR,
            LogLevel.CRITICAL: logging.CRITICAL,
        }
        
        for log_level, expected in conversions.items():
            assert _convert_log_level(log_level) == expected
    
    def test_convert_invalid_log_level(self):
        """Test conversion with invalid log level returns default."""
        # Create a mock LogLevel that's not in the mapping
        mock_level = MagicMock()
        mock_level.name = "INVALID"
        
        result = _convert_log_level(mock_level)
        assert result == logging.INFO  # Default fallback


class TestConfigureMySQLConnectorLogging:
    """Test MySQL connector logging configuration."""
    
    def test_configure_mysql_connector_logging_default(self):
        """Test MySQL connector logging with default level."""
        configure_mysql_connector_logging()
        
        # Check that MySQL connector loggers are configured
        mysql_loggers = [
            "mysql.connector",
            "mysql.connector.connection",
            "mysql.connector.cursor",
            "mysql.connector.pooling",
        ]
        
        for logger_name in mysql_loggers:
            logger = logging.getLogger(logger_name)
            assert logger.level == logging.WARNING
    
    def test_configure_mysql_connector_logging_custom_level(self):
        """Test MySQL connector logging with custom level."""
        configure_mysql_connector_logging(level=logging.ERROR)
        
        logger = logging.getLogger("mysql.connector")
        assert logger.level == logging.ERROR


class TestLogPrivilegeOperation:
    """Test privilege operation logging functionality."""
    
    def test_log_privilege_operation_success(self):
        """Test logging successful privilege operation."""
        logger = MagicMock()
        
        log_privilege_operation(logger, "Test operation", True)
        
        logger.info.assert_called_once()
        call_args = logger.info.call_args[0][0]
        assert "SUCCESS" in call_args
        assert "Test operation" in call_args
    
    def test_log_privilege_operation_failure(self):
        """Test logging failed privilege operation."""
        logger = MagicMock()
        
        log_privilege_operation(logger, "Test operation", False)
        
        logger.error.assert_called_once()
        call_args = logger.error.call_args[0][0]
        assert "FAILED" in call_args
        assert "Test operation" in call_args
    
    def test_log_privilege_operation_with_details(self):
        """Test logging privilege operation with details."""
        logger = MagicMock()
        details = {"user": "testuser", "privilege": "SUPER"}
        
        log_privilege_operation(logger, "Grant privilege", True, details)
        
        logger.info.assert_called_once()
        call_args = logger.info.call_args[0][0]
        assert "Grant privilege" in call_args
        assert "user=testuser" in call_args
        assert "privilege=SUPER" in call_args


class TestLogConfigurationChange:
    """Test configuration change logging functionality."""
    
    def test_log_configuration_change(self):
        """Test logging configuration changes."""
        logger = MagicMock()
        
        log_configuration_change(
            logger, 
            "log_bin_trust_function_creators", 
            False, 
            True, 
            Environment.CI_CD
        )
        
        logger.info.assert_called_once()
        call_args = logger.info.call_args[0][0]
        assert "log_bin_trust_function_creators" in call_args
        assert "False" in call_args
        assert "True" in call_args
        assert "ci" in call_args


class TestEnvironmentLogLevels:
    """Test environment-specific log levels."""
    
    def test_environment_log_levels_complete(self):
        """Test that all environments have log levels defined."""
        for env in Environment:
            assert env in ENVIRONMENT_LOG_LEVELS
    
    def test_environment_log_levels_appropriate(self):
        """Test that log levels are appropriate for environments."""
        assert ENVIRONMENT_LOG_LEVELS[Environment.LOCAL_DEVELOPMENT] == logging.DEBUG
        assert ENVIRONMENT_LOG_LEVELS[Environment.CI_CD] == logging.INFO
        assert ENVIRONMENT_LOG_LEVELS[Environment.PRODUCTION] == logging.WARNING
        
        # Production should be most restrictive (highest level)
        prod_level = ENVIRONMENT_LOG_LEVELS[Environment.PRODUCTION]
        ci_level = ENVIRONMENT_LOG_LEVELS[Environment.CI_CD]
        dev_level = ENVIRONMENT_LOG_LEVELS[Environment.LOCAL_DEVELOPMENT]
        
        assert prod_level >= ci_level >= dev_level


class TestLoggingIntegration:
    """Test logging integration scenarios."""
    
    def test_logging_after_setup(self):
        """Test that logging works correctly after setup."""
        setup_logging(environment=Environment.LOCAL_DEVELOPMENT, enable_console=False)
        
        logger = get_logger("test_integration")
        
        # These should not raise exceptions
        logger.debug("Debug message")
        logger.info("Info message")
        logger.warning("Warning message")
        logger.error("Error message")
        logger.critical("Critical message")
    
    def test_multiple_setup_calls(self):
        """Test that multiple setup calls work correctly."""
        # First setup
        setup_logging(environment=Environment.LOCAL_DEVELOPMENT)
        logger1 = get_logger("test1")
        
        # Second setup should clear previous handlers
        setup_logging(environment=Environment.PRODUCTION)
        logger2 = get_logger("test2")
        
        # Both loggers should work
        logger1.info("Test message 1")
        logger2.info("Test message 2")
        
        # Root logger should have handlers from second setup
        root_logger = logging.getLogger("mysql_privilege_config")
        assert root_logger.level == ENVIRONMENT_LOG_LEVELS[Environment.PRODUCTION]