"""
Error handling framework for MySQL privilege configuration.

This module provides comprehensive error handling with resolution strategies,
retry logic, and user-friendly error messages for common MySQL privilege issues.
"""

import time
import logging
from typing import Dict, List, Optional, Callable, Any, Type
from ..core.models import ErrorResolution, RetryStrategy, LogLevel, Environment
from ..core.exceptions import (
    MySQLPrivilegeError,
    SuperPrivilegeRequiredError,
    ConfigurationError,
    EnvironmentDetectionError,
    ScriptExecutionError,
    PrivilegeValidationError,
    BinaryLoggingError,
    ConnectionError,
)
from .logging_config import get_logger


class ErrorHandler:
    """
    Comprehensive error handler for MySQL privilege configuration operations.
    
    Provides error categorization, resolution strategies, retry logic,
    and user-friendly error messages with actionable resolution steps.
    """
    
    def __init__(self, environment: Environment = Environment.LOCAL_DEVELOPMENT):
        self.environment = environment
        self.logger = get_logger(__name__)
        self._error_handlers: Dict[Type[Exception], Callable] = {
            SuperPrivilegeRequiredError: self._handle_super_privilege_error,
            ConfigurationError: self._handle_configuration_error,
            EnvironmentDetectionError: self._handle_environment_detection_error,
            ScriptExecutionError: self._handle_script_execution_error,
            PrivilegeValidationError: self._handle_privilege_validation_error,
            BinaryLoggingError: self._handle_binary_logging_error,
            ConnectionError: self._handle_connection_error,
        }
    
    def handle_error(self, error: Exception) -> ErrorResolution:
        """
        Handle an error and return resolution strategy.
        
        Args:
            error: The exception that occurred
            
        Returns:
            ErrorResolution with retry strategy and user instructions
        """
        error_type = type(error)
        
        # Find the most specific handler
        handler = None
        for exc_type, handler_func in self._error_handlers.items():
            if isinstance(error, exc_type):
                handler = handler_func
                break
        
        if handler:
            resolution = handler(error)
        else:
            resolution = self._handle_generic_error(error)
        
        # Log the error and resolution
        self._log_error_and_resolution(error, resolution)
        
        return resolution
    
    def execute_with_retry(
        self,
        operation: Callable[[], Any],
        operation_name: str,
        max_attempts: int = 3,
        retry_strategy: RetryStrategy = RetryStrategy.LINEAR_BACKOFF,
    ) -> Any:
        """
        Execute an operation with retry logic.
        
        Args:
            operation: Function to execute
            operation_name: Description of the operation for logging
            max_attempts: Maximum number of retry attempts
            retry_strategy: Strategy for retry delays
            
        Returns:
            Result of the operation
            
        Raises:
            The last exception if all retries fail
        """
        last_exception = None
        
        for attempt in range(max_attempts):
            try:
                self.logger.debug(f"Executing {operation_name} (attempt {attempt + 1}/{max_attempts})")
                result = operation()
                
                if attempt > 0:
                    self.logger.info(f"{operation_name} succeeded after {attempt + 1} attempts")
                
                return result
                
            except Exception as e:
                last_exception = e
                
                if attempt < max_attempts - 1:  # Not the last attempt
                    resolution = self.handle_error(e)
                    
                    if resolution.can_retry:
                        delay = self._calculate_retry_delay(attempt, retry_strategy)
                        self.logger.warning(
                            f"{operation_name} failed (attempt {attempt + 1}/{max_attempts}): {e}. "
                            f"Retrying in {delay} seconds..."
                        )
                        time.sleep(delay)
                    else:
                        self.logger.error(f"{operation_name} failed with non-retryable error: {e}")
                        break
                else:
                    self.logger.error(f"{operation_name} failed after {max_attempts} attempts: {e}")
        
        # All retries failed
        if last_exception:
            raise last_exception
    
    def _handle_super_privilege_error(self, error: SuperPrivilegeRequiredError) -> ErrorResolution:
        """Handle ERROR 1419 - SUPER privilege required."""
        resolution = ErrorResolution(
            can_retry=True,
            retry_strategy=RetryStrategy.NO_RETRY,  # Fix configuration first
            log_level=LogLevel.ERROR,
        )
        
        if self.environment == Environment.LOCAL_DEVELOPMENT:
            resolution.add_instruction("Enable log_bin_trust_function_creators in your local MySQL configuration")
            resolution.add_instruction("Add 'log_bin_trust_function_creators = 1' to your my.cnf file")
            resolution.add_instruction("Restart MySQL server to apply the configuration")
        elif self.environment == Environment.CI_CD:
            resolution.add_instruction("Configure CI environment to set log_bin_trust_function_creators = 1")
            resolution.add_instruction("Update Docker Compose or CI configuration files")
            resolution.add_instruction("Ensure MySQL container starts with proper configuration")
        else:  # Production
            resolution.add_instruction("Consider granting CREATE ROUTINE privilege instead of SUPER")
            resolution.add_instruction("Review security implications before enabling log_bin_trust_function_creators")
            resolution.add_instruction("Consult with security team for production privilege changes")
        
        return resolution
    
    def _handle_configuration_error(self, error: ConfigurationError) -> ErrorResolution:
        """Handle MySQL configuration errors."""
        resolution = ErrorResolution(
            can_retry=True,
            retry_strategy=RetryStrategy.LINEAR_BACKOFF,
            log_level=LogLevel.ERROR,
            max_retry_attempts=2,
        )
        
        resolution.add_instruction("Verify MySQL server is running and accessible")
        resolution.add_instruction("Check that user has sufficient privileges to modify system variables")
        resolution.add_instruction("Ensure MySQL version supports the configuration settings")
        
        if hasattr(error, 'failed_settings') and error.failed_settings:
            failed_settings = ", ".join(error.failed_settings.keys())
            resolution.add_instruction(f"Review failed settings: {failed_settings}")
        
        return resolution
    
    def _handle_environment_detection_error(self, error: EnvironmentDetectionError) -> ErrorResolution:
        """Handle environment detection errors."""
        resolution = ErrorResolution(
            can_retry=False,  # Environment detection doesn't usually benefit from retries
            log_level=LogLevel.WARNING,
        )
        
        resolution.add_instruction("Manually specify the environment using --environment parameter")
        resolution.add_instruction("Check environment variables (CI, GITHUB_ACTIONS, etc.)")
        resolution.add_instruction("Verify containerization indicators if running in containers")
        resolution.add_instruction("Default to CI environment if uncertain")
        
        return resolution
    
    def _handle_script_execution_error(self, error: ScriptExecutionError) -> ErrorResolution:
        """Handle database script execution errors."""
        resolution = ErrorResolution(
            can_retry=True,
            retry_strategy=RetryStrategy.LINEAR_BACKOFF,
            log_level=LogLevel.ERROR,
            max_retry_attempts=2,
        )
        
        resolution.add_instruction("Review SQL syntax in the failed statement")
        resolution.add_instruction("Check that all referenced objects exist")
        resolution.add_instruction("Verify user has necessary privileges for the operation")
        
        if hasattr(error, 'failed_statement') and error.failed_statement:
            resolution.add_instruction(f"Failed statement: {error.failed_statement[:100]}...")
        
        return resolution
    
    def _handle_privilege_validation_error(self, error: PrivilegeValidationError) -> ErrorResolution:
        """Handle privilege validation errors."""
        resolution = ErrorResolution(
            can_retry=False,  # Privilege issues need manual intervention
            log_level=LogLevel.ERROR,
        )
        
        resolution.add_instruction("Grant necessary privileges to the database user")
        resolution.add_instruction("Contact database administrator for privilege escalation")
        
        if hasattr(error, 'missing_privileges') and error.missing_privileges:
            missing = ", ".join(error.missing_privileges)
            resolution.add_instruction(f"Missing privileges: {missing}")
        
        return resolution
    
    def _handle_binary_logging_error(self, error: BinaryLoggingError) -> ErrorResolution:
        """Handle binary logging configuration errors."""
        resolution = ErrorResolution(
            can_retry=True,
            retry_strategy=RetryStrategy.NO_RETRY,
            log_level=LogLevel.WARNING,
        )
        
        resolution.add_instruction("Check MySQL binary logging configuration")
        resolution.add_instruction("Verify log_bin system variable setting")
        resolution.add_instruction("Consider implications of disabling binary logging")
        
        return resolution
    
    def _handle_connection_error(self, error: ConnectionError) -> ErrorResolution:
        """Handle MySQL connection errors."""
        resolution = ErrorResolution(
            can_retry=True,
            retry_strategy=RetryStrategy.EXPONENTIAL_BACKOFF,
            log_level=LogLevel.ERROR,
            max_retry_attempts=3,
            retry_delay_seconds=2,
        )
        
        resolution.add_instruction("Verify MySQL server is running")
        resolution.add_instruction("Check network connectivity to MySQL server")
        resolution.add_instruction("Verify connection parameters (host, port, credentials)")
        
        if hasattr(error, 'host') and error.host:
            resolution.add_instruction(f"Target host: {error.host}:{getattr(error, 'port', 3306)}")
        
        return resolution
    
    def _handle_generic_error(self, error: Exception) -> ErrorResolution:
        """Handle generic errors not covered by specific handlers."""
        resolution = ErrorResolution(
            can_retry=False,
            log_level=LogLevel.ERROR,
        )
        
        resolution.add_instruction("Review the error message for specific details")
        resolution.add_instruction("Check MySQL server logs for additional information")
        resolution.add_instruction("Consult documentation for the specific error")
        
        return resolution
    
    def _calculate_retry_delay(self, attempt: int, strategy: RetryStrategy) -> float:
        """Calculate delay before retry based on strategy."""
        if strategy == RetryStrategy.NO_RETRY:
            return 0.0
        elif strategy == RetryStrategy.LINEAR_BACKOFF:
            return 1.0 + attempt
        elif strategy == RetryStrategy.EXPONENTIAL_BACKOFF:
            return 2.0 ** attempt
        else:
            return 1.0
    
    def _log_error_and_resolution(self, error: Exception, resolution: ErrorResolution) -> None:
        """Log error details and resolution strategy."""
        log_level = getattr(logging, resolution.log_level.value.upper())
        
        self.logger.log(log_level, f"Error occurred: {type(error).__name__}: {error}")
        
        if resolution.user_instructions:
            self.logger.log(log_level, "Resolution steps:")
            for i, instruction in enumerate(resolution.user_instructions, 1):
                self.logger.log(log_level, f"  {i}. {instruction}")
        
        if resolution.can_retry:
            self.logger.log(log_level, f"Retry strategy: {resolution.retry_strategy.value}")
            self.logger.log(log_level, f"Max retry attempts: {resolution.max_retry_attempts}")


def create_error_handler(environment: Environment) -> ErrorHandler:
    """
    Factory function to create an error handler for the specified environment.
    
    Args:
        environment: Target environment
        
    Returns:
        Configured ErrorHandler instance
    """
    return ErrorHandler(environment)