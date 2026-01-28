"""
Logging configuration for MySQL privilege configuration system.

This module provides centralized logging configuration with appropriate
formatters and handlers for different environments and use cases.
"""

import logging
import logging.handlers
import sys
from pathlib import Path
from typing import Optional, Dict, Any
from ..core.models import Environment, LogLevel


# Default logging configuration
DEFAULT_LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
DEFAULT_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

# Environment-specific log levels
ENVIRONMENT_LOG_LEVELS = {
    Environment.LOCAL_DEVELOPMENT: logging.DEBUG,
    Environment.CI_CD: logging.INFO,
    Environment.PRODUCTION: logging.WARNING,
}


def setup_logging(
    environment: Environment = Environment.LOCAL_DEVELOPMENT,
    log_file: Optional[str] = None,
    log_level: Optional[LogLevel] = None,
    enable_console: bool = True,
    log_format: Optional[str] = None,
    date_format: Optional[str] = None,
) -> None:
    """
    Set up logging configuration for the MySQL privilege configuration system.
    
    Args:
        environment: Deployment environment (affects default log level)
        log_file: Optional path to log file
        log_level: Override default log level for environment
        enable_console: Whether to enable console logging
        log_format: Custom log format string
        date_format: Custom date format string
    """
    # Determine log level
    if log_level:
        level = _convert_log_level(log_level)
    else:
        level = ENVIRONMENT_LOG_LEVELS.get(environment, logging.INFO)
    
    # Use custom formats or defaults
    fmt = log_format or DEFAULT_LOG_FORMAT
    date_fmt = date_format or DEFAULT_DATE_FORMAT
    
    # Create formatter
    formatter = logging.Formatter(fmt=fmt, datefmt=date_fmt)
    
    # Get root logger and clear existing handlers
    root_logger = logging.getLogger("mysql_privilege_config")
    root_logger.handlers.clear()
    root_logger.setLevel(level)
    
    # Add console handler if enabled
    if enable_console:
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setFormatter(formatter)
        console_handler.setLevel(level)
        root_logger.addHandler(console_handler)
    
    # Add file handler if log file specified
    if log_file:
        # Ensure log directory exists
        log_path = Path(log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Create rotating file handler
        file_handler = logging.handlers.RotatingFileHandler(
            log_file,
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=5,
            encoding='utf-8'
        )
        file_handler.setFormatter(formatter)
        file_handler.setLevel(level)
        root_logger.addHandler(file_handler)
    
    # Log the configuration
    logger = get_logger(__name__)
    logger.info(f"Logging configured for environment: {environment.value}")
    logger.info(f"Log level: {logging.getLevelName(level)}")
    if log_file:
        logger.info(f"Log file: {log_file}")


def get_logger(name: str) -> logging.Logger:
    """
    Get a logger instance for the specified module.
    
    Args:
        name: Logger name (typically __name__ from calling module)
        
    Returns:
        Configured logger instance
    """
    # Ensure the logger is under our hierarchy
    if not name.startswith("mysql_privilege_config"):
        name = f"mysql_privilege_config.{name}"
    
    return logging.getLogger(name)


def _convert_log_level(log_level: LogLevel) -> int:
    """Convert LogLevel enum to logging module level."""
    level_mapping = {
        LogLevel.DEBUG: logging.DEBUG,
        LogLevel.INFO: logging.INFO,
        LogLevel.WARNING: logging.WARNING,
        LogLevel.ERROR: logging.ERROR,
        LogLevel.CRITICAL: logging.CRITICAL,
    }
    return level_mapping.get(log_level, logging.INFO)


def configure_mysql_connector_logging(level: int = logging.WARNING) -> None:
    """
    Configure logging for MySQL connector to reduce noise.
    
    Args:
        level: Log level for MySQL connector (default: WARNING)
    """
    mysql_loggers = [
        "mysql.connector",
        "mysql.connector.connection",
        "mysql.connector.cursor",
        "mysql.connector.pooling",
    ]
    
    for logger_name in mysql_loggers:
        logger = logging.getLogger(logger_name)
        logger.setLevel(level)


def log_privilege_operation(
    logger: logging.Logger,
    operation: str,
    success: bool,
    details: Optional[Dict[str, Any]] = None,
) -> None:
    """
    Log privilege-related operations with consistent formatting.
    
    Args:
        logger: Logger instance to use
        operation: Description of the operation
        success: Whether the operation succeeded
        details: Optional additional details to log
    """
    status = "SUCCESS" if success else "FAILED"
    message = f"Privilege operation {status}: {operation}"
    
    if details:
        detail_str = ", ".join(f"{k}={v}" for k, v in details.items())
        message += f" ({detail_str})"
    
    if success:
        logger.info(message)
    else:
        logger.error(message)


def log_configuration_change(
    logger: logging.Logger,
    setting: str,
    old_value: Any,
    new_value: Any,
    environment: Environment,
) -> None:
    """
    Log MySQL configuration changes with consistent formatting.
    
    Args:
        logger: Logger instance to use
        setting: Name of the configuration setting
        old_value: Previous value
        new_value: New value
        environment: Environment where change occurred
    """
    logger.info(
        f"Configuration change in {environment.value}: "
        f"{setting} changed from '{old_value}' to '{new_value}'"
    )