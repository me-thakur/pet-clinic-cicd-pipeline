"""
Custom exceptions for MySQL privilege configuration.

This module defines specific exception classes for different types of errors
that can occur during MySQL privilege configuration and management.
"""

from typing import List, Optional, Dict, Any


class MySQLPrivilegeError(Exception):
    """Base exception for MySQL privilege-related errors."""
    
    def __init__(
        self, 
        message: str, 
        mysql_error_code: Optional[int] = None,
        resolution_steps: Optional[List[str]] = None
    ):
        super().__init__(message)
        self.mysql_error_code = mysql_error_code
        self.resolution_steps = resolution_steps or []
        
    def add_resolution_step(self, step: str) -> None:
        """Add a resolution step to help users fix the issue."""
        self.resolution_steps.append(step)


class ConfigurationError(MySQLPrivilegeError):
    """Exception raised when MySQL configuration fails."""
    
    def __init__(
        self, 
        message: str, 
        failed_settings: Optional[Dict[str, Any]] = None,
        **kwargs
    ):
        super().__init__(message, **kwargs)
        self.failed_settings = failed_settings or {}


class EnvironmentDetectionError(MySQLPrivilegeError):
    """Exception raised when environment detection fails."""
    
    def __init__(
        self, 
        message: str, 
        detected_indicators: Optional[Dict[str, Any]] = None,
        **kwargs
    ):
        super().__init__(message, **kwargs)
        self.detected_indicators = detected_indicators or {}


class ScriptExecutionError(MySQLPrivilegeError):
    """Exception raised when database script execution fails."""
    
    def __init__(
        self, 
        message: str, 
        failed_statement: Optional[str] = None,
        statement_index: Optional[int] = None,
        **kwargs
    ):
        super().__init__(message, **kwargs)
        self.failed_statement = failed_statement
        self.statement_index = statement_index


class PrivilegeValidationError(MySQLPrivilegeError):
    """Exception raised when privilege validation fails."""
    
    def __init__(
        self, 
        message: str, 
        missing_privileges: Optional[List[str]] = None,
        current_grants: Optional[List[str]] = None,
        **kwargs
    ):
        super().__init__(message, **kwargs)
        self.missing_privileges = missing_privileges or []
        self.current_grants = current_grants or []


class SuperPrivilegeRequiredError(MySQLPrivilegeError):
    """Exception raised when SUPER privilege is required but not available (ERROR 1419)."""
    
    def __init__(
        self, 
        message: str = "You do not have the SUPER privilege and binary logging is enabled",
        operation: Optional[str] = None,
        **kwargs
    ):
        super().__init__(message, mysql_error_code=1419, **kwargs)
        self.operation = operation
        
        # Add standard resolution steps for ERROR 1419
        self.add_resolution_step("Set log_bin_trust_function_creators = 1 in MySQL configuration")
        self.add_resolution_step("Or grant SUPER privilege to the database user")
        self.add_resolution_step("Or disable binary logging (not recommended for production)")


class BinaryLoggingError(MySQLPrivilegeError):
    """Exception raised when binary logging configuration issues occur."""
    
    def __init__(
        self, 
        message: str,
        current_setting: Optional[bool] = None,
        **kwargs
    ):
        super().__init__(message, **kwargs)
        self.current_setting = current_setting


class ConnectionError(MySQLPrivilegeError):
    """Exception raised when MySQL connection fails."""
    
    def __init__(
        self, 
        message: str,
        host: Optional[str] = None,
        port: Optional[int] = None,
        database: Optional[str] = None,
        **kwargs
    ):
        super().__init__(message, **kwargs)
        self.host = host
        self.port = port
        self.database = database