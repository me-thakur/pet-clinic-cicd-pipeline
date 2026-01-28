"""
Database Configuration Manager - Central orchestrator for MySQL privilege configuration.

This module serves as the main entry point for coordinating MySQL privilege configuration 
across different environments. It integrates with EnvironmentDetector and PrivilegeValidator
to provide comprehensive configuration management with validation and rollback capabilities.
"""

from typing import Optional, Dict, Any, List
from contextlib import contextmanager

try:
    import mysql.connector
    from mysql.connector import Error as MySQLError
    MYSQL_CONNECTOR_AVAILABLE = True
except ImportError:
    MYSQL_CONNECTOR_AVAILABLE = False
    MySQLError = Exception

from ..core.models import (
    Environment, MySQLConfiguration, ConfigurationResult, 
    EnvironmentProfile, PrivilegeReport, SecurityLevel
)
from ..core.exceptions import (
    ConfigurationError, ConnectionError, PrivilegeValidationError,
    SuperPrivilegeRequiredError, BinaryLoggingError
)
from ..detectors.environment_detector import EnvironmentDetector
from ..validators.privilege_validator import PrivilegeValidator
from ..utils.logging_config import get_logger


class DatabaseConfigManager:
    """
    Central component that orchestrates MySQL privilege configuration across different environments.
    
    This class coordinates environment detection, privilege validation, and configuration application
    to ensure MySQL functions, procedures, and triggers can be created without SUPER privilege issues.
    It provides environment-specific configuration profiles and comprehensive error handling.
    """
    
    def __init__(self, config: Optional[MySQLConfiguration] = None):
        """
        Initialize the DatabaseConfigManager.
        
        Args:
            config: MySQL configuration. If None, uses default configuration.
        """
        self.config = config or MySQLConfiguration()
        self.logger = get_logger(__name__)
        
        # Initialize component dependencies
        self.environment_detector = EnvironmentDetector()
        self.privilege_validator = PrivilegeValidator(self.config)
        
        # Cache for environment and configuration state
        self._detected_environment: Optional[Environment] = None
        self._current_profile: Optional[EnvironmentProfile] = None
        self._original_settings: Dict[str, Any] = {}
        
        if not MYSQL_CONNECTOR_AVAILABLE:
            self.logger.warning(
                "MySQL connector not available. Install mysql-connector-python for full functionality."
            )
    
    def detect_environment(self) -> Environment:
        """
        Detect current deployment environment using EnvironmentDetector.
        
        Returns:
            Environment: The detected environment (CI_CD, PRODUCTION, or LOCAL_DEVELOPMENT)
            
        Raises:
            ConfigurationError: If environment detection fails
        """
        try:
            if self._detected_environment is None:
                self.logger.info("Detecting deployment environment")
                self._detected_environment = self.environment_detector.detect_environment()
                
                # Update configuration with detected environment
                self.config.environment = self._detected_environment
                self.config.security_level = self._get_security_level_for_environment(
                    self._detected_environment
                )
                
                self.logger.info(f"Environment detected: {self._detected_environment.value}")
            
            return self._detected_environment
            
        except Exception as e:
            error_msg = f"Failed to detect environment: {str(e)}"
            self.logger.error(error_msg)
            raise ConfigurationError(error_msg)
    
    def apply_configuration(self, env: Optional[Environment] = None) -> ConfigurationResult:
        """
        Apply MySQL configuration for the specified environment.
        
        Args:
            env: Target environment. If None, uses detected environment.
            
        Returns:
            ConfigurationResult: Result of configuration application
            
        Raises:
            ConfigurationError: If configuration application fails
        """
        result = ConfigurationResult(success=True)
        
        try:
            # Use provided environment or detect current one
            target_env = env or self.detect_environment()
            self.logger.info(f"Applying configuration for environment: {target_env.value}")
            
            # Get environment profile
            profile = self._get_environment_profile(target_env)
            self._current_profile = profile
            
            # Store original settings for potential rollback
            self._store_original_settings()
            
            # Apply MySQL system variable configurations
            mysql_settings = profile.mysql_config
            for setting_name, setting_value in mysql_settings.items():
                if setting_name == "additional_settings":
                    # Handle additional settings dictionary
                    for add_setting, add_value in setting_value.items():
                        success = self._apply_mysql_setting(add_setting, add_value, result)
                        if not success:
                            result.success = False
                else:
                    success = self._apply_mysql_setting(setting_name, setting_value, result)
                    if not success:
                        result.success = False
            
            # Validate configuration was applied successfully
            if result.success:
                validation_success = self._validate_applied_configuration(profile, result)
                if not validation_success:
                    result.success = False
                    result.add_error("Configuration validation failed after application")
            
            # Log final result
            if result.success:
                self.logger.info(f"Configuration applied successfully for {target_env.value}")
                result.applied_settings["environment"] = target_env.value
                result.applied_settings["security_level"] = profile.name.value
            else:
                self.logger.error(f"Configuration application failed for {target_env.value}")
                # Attempt rollback if we have original settings
                if self._original_settings:
                    self._attempt_rollback(result)
            
            return result
            
        except Exception as e:
            error_msg = f"Failed to apply configuration: {str(e)}"
            self.logger.error(error_msg)
            result.success = False
            result.add_error(error_msg)
            
            # Attempt rollback on critical failure
            if self._original_settings:
                self._attempt_rollback(result)
            
            return result
    
    def validate_privileges(self) -> bool:
        """
        Validate that required privileges are available using PrivilegeValidator.
        
        Returns:
            bool: True if all required privileges are available, False otherwise
            
        Raises:
            PrivilegeValidationError: If privilege validation fails
        """
        try:
            self.logger.info("Validating MySQL privileges")
            
            # Generate comprehensive privilege report
            report = self.privilege_validator.generate_privilege_report()
            
            # Check function creation capability
            can_create_functions = self.privilege_validator.validate_function_creation_capability()
            
            # Log privilege status
            self.logger.info(
                f"Privilege validation complete. "
                f"SUPER: {report.has_super_privilege}, "
                f"CREATE ROUTINE: {report.has_create_routine}, "
                f"Binary logging: {report.binary_logging_enabled}, "
                f"Trust function creators: {report.trust_function_creators_enabled}, "
                f"Can create functions: {can_create_functions}"
            )
            
            # Provide recommendations if needed
            if report.recommended_actions:
                self.logger.info("Privilege recommendations:")
                for action in report.recommended_actions:
                    self.logger.info(f"  - {action}")
            
            return can_create_functions
            
        except SuperPrivilegeRequiredError as e:
            self.logger.warning(f"Privilege validation failed: {e}")
            return False
        except Exception as e:
            error_msg = f"Privilege validation error: {str(e)}"
            self.logger.error(error_msg)
            raise PrivilegeValidationError(error_msg)
    
    def execute_script(self, script: str) -> bool:
        """
        Execute database script with privilege handling and environment-appropriate modifications.
        
        Args:
            script: SQL script to execute
            
        Returns:
            bool: True if script executed successfully, False otherwise
            
        Raises:
            ConfigurationError: If script execution fails critically
        """
        try:
            self.logger.info("Executing database script with privilege handling")
            
            # Validate privileges before execution
            if not self.validate_privileges():
                self.logger.warning("Privilege validation failed, but attempting script execution")
            
            # Get current environment profile for script modifications
            env = self.detect_environment()
            profile = self._get_environment_profile(env)
            
            # Apply environment-specific script modifications
            modified_script = self._prepare_script_for_environment(script, profile)
            
            # Execute the script
            success = self._execute_sql_script(modified_script)
            
            if success:
                self.logger.info("Database script executed successfully")
            else:
                self.logger.error("Database script execution failed")
            
            return success
            
        except Exception as e:
            error_msg = f"Script execution failed: {str(e)}"
            self.logger.error(error_msg)
            raise ConfigurationError(error_msg)
    
    def get_privilege_report(self) -> PrivilegeReport:
        """
        Get comprehensive privilege report from PrivilegeValidator.
        
        Returns:
            PrivilegeReport: Current privilege status and recommendations
        """
        return self.privilege_validator.generate_privilege_report()
    
    def rollback_configuration(self) -> ConfigurationResult:
        """
        Rollback MySQL configuration to original settings.
        
        Returns:
            ConfigurationResult: Result of rollback operation
        """
        result = ConfigurationResult(success=True)
        
        if not self._original_settings:
            result.add_warning("No original settings stored for rollback")
            return result
        
        try:
            self.logger.info("Rolling back MySQL configuration to original settings")
            
            for setting_name, original_value in self._original_settings.items():
                success = self._apply_mysql_setting(setting_name, original_value, result)
                if not success:
                    result.success = False
            
            if result.success:
                self.logger.info("Configuration rollback completed successfully")
                self._original_settings.clear()
            else:
                self.logger.error("Configuration rollback failed")
            
            return result
            
        except Exception as e:
            error_msg = f"Configuration rollback failed: {str(e)}"
            self.logger.error(error_msg)
            result.success = False
            result.add_error(error_msg)
            return result
    
    def _get_security_level_for_environment(self, env: Environment) -> SecurityLevel:
        """Get appropriate security level for environment."""
        if env == Environment.LOCAL_DEVELOPMENT:
            return SecurityLevel.PERMISSIVE
        elif env == Environment.CI_CD:
            return SecurityLevel.BALANCED
        else:  # PRODUCTION
            return SecurityLevel.RESTRICTIVE
    
    def _get_environment_profile(self, env: Environment) -> EnvironmentProfile:
        """Get configuration profile for the specified environment."""
        return EnvironmentProfile(name=env)
    
    @contextmanager
    def _get_connection(self):
        """Get MySQL connection with proper error handling."""
        if not MYSQL_CONNECTOR_AVAILABLE:
            raise ConfigurationError(
                "MySQL connector not available. Install mysql-connector-python to use this feature."
            )
        
        connection = None
        try:
            connection_config = {
                'host': self.config.host,
                'port': self.config.port,
                'database': self.config.database,
                'user': self.config.username,
                'password': self.config.password,
                'autocommit': True,
                'connection_timeout': self.config.timeout_seconds,
                'charset': 'utf8mb4',
                'collation': 'utf8mb4_unicode_ci'
            }
            
            # Remove empty values
            connection_config = {k: v for k, v in connection_config.items() if v}
            
            connection = mysql.connector.connect(**connection_config)
            
            if not connection.is_connected():
                raise ConnectionError(
                    f"Failed to establish connection to MySQL server",
                    host=self.config.host,
                    port=self.config.port
                )
            
            yield connection
            
        except MySQLError as e:
            error_msg = f"MySQL connection error: {e}"
            self.logger.error(error_msg)
            raise ConnectionError(error_msg)
        finally:
            if connection and connection.is_connected():
                connection.close()
    
    def _store_original_settings(self) -> None:
        """Store original MySQL settings for potential rollback."""
        try:
            settings_to_store = [
                'log_bin_trust_function_creators',
                'log_bin',
                'innodb_flush_log_at_trx_commit'
            ]
            
            with self._get_connection() as connection:
                cursor = connection.cursor()
                try:
                    for setting in settings_to_store:
                        cursor.execute("SHOW VARIABLES LIKE %s", (setting,))
                        result = cursor.fetchone()
                        if result:
                            self._original_settings[setting] = result[1]
                            self.logger.debug(f"Stored original setting: {setting} = {result[1]}")
                finally:
                    cursor.close()
                    
        except Exception as e:
            self.logger.warning(f"Could not store original settings: {e}")
    
    def _apply_mysql_setting(self, setting_name: str, setting_value: Any, result: ConfigurationResult) -> bool:
        """Apply a single MySQL system variable setting."""
        try:
            # Convert boolean values to MySQL format
            if isinstance(setting_value, bool):
                mysql_value = "ON" if setting_value else "OFF"
            else:
                mysql_value = str(setting_value)
            
            with self._get_connection() as connection:
                cursor = connection.cursor()
                try:
                    # Use SET GLOBAL for system variables
                    query = f"SET GLOBAL {setting_name} = %s"
                    cursor.execute(query, (mysql_value,))
                    
                    result.applied_settings[setting_name] = mysql_value
                    self.logger.debug(f"Applied setting: {setting_name} = {mysql_value}")
                    return True
                    
                except MySQLError as e:
                    if "read only" in str(e).lower() or "readonly" in str(e).lower():
                        result.add_warning(f"Setting {setting_name} is read-only and cannot be changed at runtime")
                        self.logger.warning(f"Read-only setting {setting_name}: {e}")
                        return True  # Not a failure, just a limitation
                    else:
                        result.add_error(f"Failed to set {setting_name}: {e}")
                        self.logger.error(f"Failed to set {setting_name}: {e}")
                        return False
                finally:
                    cursor.close()
                    
        except Exception as e:
            result.add_error(f"Error applying setting {setting_name}: {e}")
            self.logger.error(f"Error applying setting {setting_name}: {e}")
            return False
    
    def _validate_applied_configuration(self, profile: EnvironmentProfile, result: ConfigurationResult) -> bool:
        """Validate that configuration was applied successfully."""
        try:
            validation_success = True
            
            # Check key settings that should be applied
            mysql_config = profile.mysql_config
            
            with self._get_connection() as connection:
                cursor = connection.cursor()
                try:
                    for setting_name, expected_value in mysql_config.items():
                        if setting_name == "additional_settings":
                            continue  # Skip nested dictionary
                        
                        cursor.execute("SHOW VARIABLES LIKE %s", (setting_name,))
                        result_row = cursor.fetchone()
                        
                        if result_row:
                            actual_value = result_row[1]
                            
                            # Convert expected boolean to MySQL format for comparison
                            if isinstance(expected_value, bool):
                                expected_mysql = "ON" if expected_value else "OFF"
                            else:
                                expected_mysql = str(expected_value)
                            
                            if actual_value.upper() != expected_mysql.upper():
                                result.add_warning(
                                    f"Setting {setting_name} validation failed: "
                                    f"expected {expected_mysql}, got {actual_value}"
                                )
                                validation_success = False
                            else:
                                self.logger.debug(f"Validated setting: {setting_name} = {actual_value}")
                        else:
                            result.add_warning(f"Could not validate setting: {setting_name}")
                            
                finally:
                    cursor.close()
            
            return validation_success
            
        except Exception as e:
            result.add_error(f"Configuration validation error: {e}")
            self.logger.error(f"Configuration validation error: {e}")
            return False
    
    def _attempt_rollback(self, result: ConfigurationResult) -> None:
        """Attempt to rollback configuration changes."""
        try:
            self.logger.warning("Attempting configuration rollback due to failure")
            rollback_result = self.rollback_configuration()
            
            if rollback_result.success:
                result.add_warning("Configuration rolled back successfully after failure")
            else:
                result.add_error("Configuration rollback also failed")
                for error in rollback_result.errors:
                    result.add_error(f"Rollback error: {error}")
                    
        except Exception as e:
            result.add_error(f"Rollback attempt failed: {e}")
            self.logger.error(f"Rollback attempt failed: {e}")
    
    def _prepare_script_for_environment(self, script: str, profile: EnvironmentProfile) -> str:
        """Prepare SQL script with environment-specific modifications."""
        modified_script = script
        script_mods = profile.script_modifications
        
        # Add DEFINER clauses if required
        if script_mods.get("add_definer_clauses", False):
            modified_script = self._add_definer_clauses(modified_script)
        
        # Wrap in transactions if required
        if script_mods.get("wrap_in_transactions", False):
            modified_script = self._wrap_in_transaction(modified_script)
        
        # Add error handling if required
        if script_mods.get("add_error_handling", False):
            modified_script = self._add_error_handling(modified_script)
        
        return modified_script
    
    def _add_definer_clauses(self, script: str) -> str:
        """Add DEFINER clauses to functions and procedures."""
        # Simple implementation - in production this would be more sophisticated
        definer_clause = f"DEFINER = CURRENT_USER"
        
        # Add DEFINER to CREATE FUNCTION statements
        script = script.replace(
            "CREATE FUNCTION",
            f"CREATE {definer_clause} FUNCTION"
        )
        
        # Add DEFINER to CREATE PROCEDURE statements
        script = script.replace(
            "CREATE PROCEDURE",
            f"CREATE {definer_clause} PROCEDURE"
        )
        
        return script
    
    def _wrap_in_transaction(self, script: str) -> str:
        """Wrap script in transaction with rollback on error."""
        return f"""
START TRANSACTION;
{script}
COMMIT;
"""
    
    def _add_error_handling(self, script: str) -> str:
        """Add basic error handling to script."""
        return f"""
-- Error handling for script execution
SET @old_sql_mode = @@sql_mode;
SET sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION';

{script}

-- Restore original SQL mode
SET sql_mode = @old_sql_mode;
"""
    
    def _execute_sql_script(self, script: str) -> bool:
        """Execute SQL script with proper error handling."""
        try:
            with self._get_connection() as connection:
                cursor = connection.cursor()
                try:
                    # Split script into individual statements
                    statements = [stmt.strip() for stmt in script.split(';') if stmt.strip()]
                    
                    for statement in statements:
                        if statement:
                            cursor.execute(statement)
                            self.logger.debug(f"Executed statement: {statement[:100]}...")
                    
                    return True
                    
                except MySQLError as e:
                    self.logger.error(f"SQL execution error: {e}")
                    return False
                finally:
                    cursor.close()
                    
        except Exception as e:
            self.logger.error(f"Script execution error: {e}")
            return False