"""
Unit tests for ErrorHandler class.

This module tests the comprehensive error handling functionality including
ERROR 1419 detection, configuration error handling, environment detection
error fallbacks, and user-friendly error message generation.
"""

import pytest
import time
from unittest.mock import Mock, patch, MagicMock
from hypothesis import given, strategies as st

from mysql_privilege_config.core.models import (
    Environment,
    ErrorResolution,
    RetryStrategy,
    LogLevel,
)
from mysql_privilege_config.core.exceptions import (
    SuperPrivilegeRequiredError,
    ConfigurationError,
    EnvironmentDetectionError,
    ScriptExecutionError,
    PrivilegeValidationError,
    BinaryLoggingError,
    ConnectionError,
)
from mysql_privilege_config.utils.error_handler import ErrorHandler, create_error_handler


class TestErrorHandler:
    """Test cases for ErrorHandler class."""
    
    def test_init_with_default_environment(self):
        """Test ErrorHandler initialization with default environment."""
        handler = ErrorHandler()
        assert handler.environment == Environment.LOCAL_DEVELOPMENT
        assert handler.logger is not None
        assert len(handler._error_handlers) == 7
    
    def test_init_with_specific_environment(self):
        """Test ErrorHandler initialization with specific environment."""
        handler = ErrorHandler(Environment.PRODUCTION)
        assert handler.environment == Environment.PRODUCTION
    
    def test_handle_super_privilege_error_local_dev(self):
        """Test handling ERROR 1419 in local development environment."""
        handler = ErrorHandler(Environment.LOCAL_DEVELOPMENT)
        error = SuperPrivilegeRequiredError("SUPER privilege required")
        
        resolution = handler.handle_error(error)
        
        assert isinstance(resolution, ErrorResolution)
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.NO_RETRY
        assert resolution.log_level == LogLevel.ERROR
        assert len(resolution.user_instructions) == 3
        assert "log_bin_trust_function_creators" in resolution.user_instructions[0]
        assert "my.cnf" in resolution.user_instructions[1]
        assert "Restart MySQL" in resolution.user_instructions[2]
    
    def test_handle_super_privilege_error_ci_cd(self):
        """Test handling ERROR 1419 in CI/CD environment."""
        handler = ErrorHandler(Environment.CI_CD)
        error = SuperPrivilegeRequiredError("SUPER privilege required")
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is True
        assert len(resolution.user_instructions) == 3
        assert "CI environment" in resolution.user_instructions[0]
        assert "Docker Compose" in resolution.user_instructions[1]
        assert "container" in resolution.user_instructions[2]
    
    def test_handle_super_privilege_error_production(self):
        """Test handling ERROR 1419 in production environment."""
        handler = ErrorHandler(Environment.PRODUCTION)
        error = SuperPrivilegeRequiredError("SUPER privilege required")
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is True
        assert len(resolution.user_instructions) == 3
        assert "CREATE ROUTINE privilege" in resolution.user_instructions[0]
        assert "security implications" in resolution.user_instructions[1]
        assert "security team" in resolution.user_instructions[2]
    
    def test_handle_configuration_error(self):
        """Test handling configuration errors."""
        handler = ErrorHandler()
        failed_settings = {"log_bin_trust_function_creators": "ON"}
        error = ConfigurationError("Configuration failed", failed_settings=failed_settings)
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.LINEAR_BACKOFF
        assert resolution.max_retry_attempts == 2
        assert len(resolution.user_instructions) >= 3
        assert "MySQL server is running" in resolution.user_instructions[0]
        assert "sufficient privileges" in resolution.user_instructions[1]
        assert "MySQL version" in resolution.user_instructions[2]
        assert "log_bin_trust_function_creators" in resolution.user_instructions[3]
    
    def test_handle_environment_detection_error(self):
        """Test handling environment detection errors."""
        handler = ErrorHandler()
        error = EnvironmentDetectionError("Cannot detect environment")
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is False
        assert resolution.log_level == LogLevel.WARNING
        assert len(resolution.user_instructions) == 4
        assert "--environment parameter" in resolution.user_instructions[0]
        assert "environment variables" in resolution.user_instructions[1]
        assert "containerization indicators" in resolution.user_instructions[2]
        assert "Default to CI" in resolution.user_instructions[3]
    
    def test_handle_script_execution_error(self):
        """Test handling script execution errors."""
        handler = ErrorHandler()
        error = ScriptExecutionError(
            "Script execution failed",
            failed_statement="CREATE FUNCTION test() RETURNS INT RETURN 1;",
        )
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.LINEAR_BACKOFF
        assert resolution.max_retry_attempts == 2
        assert len(resolution.user_instructions) == 4
        assert "SQL syntax" in resolution.user_instructions[0]
        assert "referenced objects" in resolution.user_instructions[1]
        assert "necessary privileges" in resolution.user_instructions[2]
        assert "CREATE FUNCTION test()" in resolution.user_instructions[3]
    
    def test_handle_privilege_validation_error(self):
        """Test handling privilege validation errors."""
        handler = ErrorHandler()
        error = PrivilegeValidationError(
            "Privilege validation failed",
            missing_privileges=["SUPER", "CREATE ROUTINE"],
        )
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is False
        assert resolution.log_level == LogLevel.ERROR
        assert len(resolution.user_instructions) == 3
        assert "Grant necessary privileges" in resolution.user_instructions[0]
        assert "database administrator" in resolution.user_instructions[1]
        assert "SUPER, CREATE ROUTINE" in resolution.user_instructions[2]
    
    def test_handle_binary_logging_error(self):
        """Test handling binary logging errors."""
        handler = ErrorHandler()
        error = BinaryLoggingError("Binary logging configuration error")
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.NO_RETRY
        assert resolution.log_level == LogLevel.WARNING
        assert len(resolution.user_instructions) == 3
        assert "binary logging configuration" in resolution.user_instructions[0]
        assert "log_bin system variable" in resolution.user_instructions[1]
        assert "disabling binary logging" in resolution.user_instructions[2]
    
    def test_handle_connection_error(self):
        """Test handling connection errors."""
        handler = ErrorHandler()
        error = ConnectionError("Connection failed", host="localhost", port=3306)
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.EXPONENTIAL_BACKOFF
        assert resolution.max_retry_attempts == 3
        assert resolution.retry_delay_seconds == 2
        assert len(resolution.user_instructions) == 4
        assert "MySQL server is running" in resolution.user_instructions[0]
        assert "network connectivity" in resolution.user_instructions[1]
        assert "connection parameters" in resolution.user_instructions[2]
        assert "localhost:3306" in resolution.user_instructions[3]
    
    def test_handle_generic_error(self):
        """Test handling generic errors not covered by specific handlers."""
        handler = ErrorHandler()
        error = ValueError("Some generic error")
        
        resolution = handler.handle_error(error)
        
        assert resolution.can_retry is False
        assert resolution.log_level == LogLevel.ERROR
        assert len(resolution.user_instructions) == 3
        assert "error message" in resolution.user_instructions[0]
        assert "MySQL server logs" in resolution.user_instructions[1]
        assert "documentation" in resolution.user_instructions[2]
    
    def test_execute_with_retry_success_first_attempt(self):
        """Test successful operation on first attempt."""
        handler = ErrorHandler()
        operation = Mock(return_value="success")
        
        result = handler.execute_with_retry(operation, "test operation")
        
        assert result == "success"
        operation.assert_called_once()
    
    def test_execute_with_retry_success_after_retries(self):
        """Test successful operation after retries."""
        handler = ErrorHandler()
        operation = Mock(side_effect=[
            SuperPrivilegeRequiredError("First failure"),
            SuperPrivilegeRequiredError("Second failure"),
            "success"
        ])
        
        result = handler.execute_with_retry(operation, "test operation", max_attempts=3)
        
        assert result == "success"
        assert operation.call_count == 3
    
    def test_execute_with_retry_non_retryable_error(self):
        """Test operation with non-retryable error."""
        handler = ErrorHandler()
        error = PrivilegeValidationError("Non-retryable error")
        operation = Mock(side_effect=error)
        
        with pytest.raises(PrivilegeValidationError):
            handler.execute_with_retry(operation, "test operation", max_attempts=3)
        
        operation.assert_called_once()  # Should not retry
    
    def test_execute_with_retry_all_attempts_fail(self):
        """Test operation that fails all retry attempts."""
        handler = ErrorHandler()
        error = SuperPrivilegeRequiredError("Persistent error")
        operation = Mock(side_effect=error)
        
        with pytest.raises(SuperPrivilegeRequiredError):
            handler.execute_with_retry(operation, "test operation", max_attempts=2)
        
        assert operation.call_count == 2
    
    @patch('time.sleep')
    def test_execute_with_retry_delay_calculation(self, mock_sleep):
        """Test retry delay calculation for different strategies."""
        handler = ErrorHandler()
        operation = Mock(side_effect=[
            SuperPrivilegeRequiredError("Error"),
            "success"
        ])
        
        # Test linear backoff
        handler.execute_with_retry(
            operation, "test operation", 
            max_attempts=2, 
            retry_strategy=RetryStrategy.LINEAR_BACKOFF
        )
        mock_sleep.assert_called_with(1.0)  # First retry delay
        
        # Reset mock
        mock_sleep.reset_mock()
        operation.side_effect = [SuperPrivilegeRequiredError("Error"), "success"]
        
        # Test exponential backoff
        handler.execute_with_retry(
            operation, "test operation", 
            max_attempts=2, 
            retry_strategy=RetryStrategy.EXPONENTIAL_BACKOFF
        )
        mock_sleep.assert_called_with(1.0)  # 2^0 = 1
    
    def test_calculate_retry_delay(self):
        """Test retry delay calculation for different strategies."""
        handler = ErrorHandler()
        
        # Test no retry
        assert handler._calculate_retry_delay(0, RetryStrategy.NO_RETRY) == 0.0
        
        # Test linear backoff
        assert handler._calculate_retry_delay(0, RetryStrategy.LINEAR_BACKOFF) == 1.0
        assert handler._calculate_retry_delay(1, RetryStrategy.LINEAR_BACKOFF) == 2.0
        assert handler._calculate_retry_delay(2, RetryStrategy.LINEAR_BACKOFF) == 3.0
        
        # Test exponential backoff
        assert handler._calculate_retry_delay(0, RetryStrategy.EXPONENTIAL_BACKOFF) == 1.0
        assert handler._calculate_retry_delay(1, RetryStrategy.EXPONENTIAL_BACKOFF) == 2.0
        assert handler._calculate_retry_delay(2, RetryStrategy.EXPONENTIAL_BACKOFF) == 4.0
    
    @patch('mysql_privilege_config.utils.error_handler.get_logger')
    def test_log_error_and_resolution(self, mock_get_logger):
        """Test error and resolution logging."""
        mock_logger = Mock()
        mock_get_logger.return_value = mock_logger
        
        handler = ErrorHandler()
        handler.logger = mock_logger
        
        error = SuperPrivilegeRequiredError("Test error")
        resolution = ErrorResolution(
            can_retry=True,
            log_level=LogLevel.ERROR,
            user_instructions=["Step 1", "Step 2"]
        )
        
        handler._log_error_and_resolution(error, resolution)
        
        # Verify logging calls
        assert mock_logger.log.call_count >= 3  # Error + resolution steps
        
        # Check that error was logged
        error_call = mock_logger.log.call_args_list[0]
        assert "SuperPrivilegeRequiredError" in str(error_call)
        
        # Check that resolution steps were logged
        resolution_calls = [call for call in mock_logger.log.call_args_list if "Step" in str(call)]
        assert len(resolution_calls) >= 2


class TestErrorHandlerFactory:
    """Test cases for error handler factory function."""
    
    def test_create_error_handler_local_dev(self):
        """Test creating error handler for local development."""
        handler = create_error_handler(Environment.LOCAL_DEVELOPMENT)
        assert isinstance(handler, ErrorHandler)
        assert handler.environment == Environment.LOCAL_DEVELOPMENT
    
    def test_create_error_handler_ci_cd(self):
        """Test creating error handler for CI/CD."""
        handler = create_error_handler(Environment.CI_CD)
        assert isinstance(handler, ErrorHandler)
        assert handler.environment == Environment.CI_CD
    
    def test_create_error_handler_production(self):
        """Test creating error handler for production."""
        handler = create_error_handler(Environment.PRODUCTION)
        assert isinstance(handler, ErrorHandler)
        assert handler.environment == Environment.PRODUCTION


# Property-based tests using Hypothesis
class TestErrorHandlerProperties:
    """Property-based tests for ErrorHandler class."""
    
    @given(st.sampled_from(Environment))
    def test_error_handler_initialization_property(self, environment):
        """Property: ErrorHandler should initialize correctly for any environment."""
        handler = ErrorHandler(environment)
        assert handler.environment == environment
        assert handler.logger is not None
        assert len(handler._error_handlers) > 0
    
    @given(
        st.sampled_from(Environment),
        st.text(min_size=1, max_size=100),
    )
    def test_super_privilege_error_handling_property(self, environment, error_message):
        """Property: SuperPrivilegeRequiredError should always be handled with appropriate resolution."""
        handler = ErrorHandler(environment)
        error = SuperPrivilegeRequiredError(error_message)
        
        resolution = handler.handle_error(error)
        
        assert isinstance(resolution, ErrorResolution)
        assert resolution.can_retry is True
        assert resolution.log_level == LogLevel.ERROR
        assert len(resolution.user_instructions) > 0
        
        # Environment-specific checks
        if environment == Environment.LOCAL_DEVELOPMENT:
            assert any("my.cnf" in instruction for instruction in resolution.user_instructions)
        elif environment == Environment.CI_CD:
            assert any("Docker" in instruction or "CI" in instruction for instruction in resolution.user_instructions)
        else:  # Production
            assert any("security" in instruction.lower() for instruction in resolution.user_instructions)
    
    @given(
        st.text(min_size=1, max_size=100),
        st.dictionaries(st.text(min_size=1, max_size=20), st.text(min_size=1, max_size=50), min_size=0, max_size=5)
    )
    def test_configuration_error_handling_property(self, error_message, failed_settings):
        """Property: ConfigurationError should always be handled with retry capability."""
        handler = ErrorHandler()
        error = ConfigurationError(error_message, failed_settings=failed_settings)
        
        resolution = handler.handle_error(error)
        
        assert isinstance(resolution, ErrorResolution)
        assert resolution.can_retry is True
        assert resolution.retry_strategy == RetryStrategy.LINEAR_BACKOFF
        assert resolution.max_retry_attempts == 2
        assert len(resolution.user_instructions) >= 3
        
        # If there are failed settings, they should be mentioned
        if failed_settings:
            settings_mentioned = any(
                any(setting in instruction for setting in failed_settings.keys())
                for instruction in resolution.user_instructions
            )
            assert settings_mentioned
    
    @given(st.integers(min_value=0, max_value=10))
    def test_retry_delay_calculation_property(self, attempt):
        """Property: Retry delay calculation should be consistent and non-negative."""
        handler = ErrorHandler()
        
        # Test all retry strategies
        for strategy in RetryStrategy:
            delay = handler._calculate_retry_delay(attempt, strategy)
            assert delay >= 0.0
            
            if strategy == RetryStrategy.NO_RETRY:
                assert delay == 0.0
            elif strategy == RetryStrategy.LINEAR_BACKOFF:
                assert delay == 1.0 + attempt
            elif strategy == RetryStrategy.EXPONENTIAL_BACKOFF:
                assert delay == 2.0 ** attempt
    
    @given(
        st.integers(min_value=1, max_value=5),
        st.sampled_from(RetryStrategy)
    )
    def test_execute_with_retry_attempts_property(self, max_attempts, retry_strategy):
        """Property: execute_with_retry should respect max_attempts parameter."""
        with patch('time.sleep'):  # Mock sleep to avoid delays in property tests
            handler = ErrorHandler()
            operation = Mock(side_effect=SuperPrivilegeRequiredError("Persistent error"))
            
            with pytest.raises(SuperPrivilegeRequiredError):
                handler.execute_with_retry(
                    operation, 
                    "test operation", 
                    max_attempts=max_attempts,
                    retry_strategy=retry_strategy
                )
            
            assert operation.call_count == max_attempts