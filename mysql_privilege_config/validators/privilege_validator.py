"""
Privilege Validator - Validates MySQL user privileges and system configuration.

This module validates MySQL user privileges and system configuration before
executing database operations, providing comprehensive privilege reports and
recommendations for resolving privilege-related issues.
"""

import re
from typing import Optional, List, Dict, Any, Tuple
from contextlib import contextmanager

try:
    import mysql.connector
    from mysql.connector import Error as MySQLError
    MYSQL_CONNECTOR_AVAILABLE = True
except ImportError:
    MYSQL_CONNECTOR_AVAILABLE = False
    MySQLError = Exception

from ..core.models import PrivilegeReport, MySQLConfiguration
from ..core.exceptions import (
    PrivilegeValidationError,
    ConnectionError,
    SuperPrivilegeRequiredError,
    BinaryLoggingError
)
from ..utils.logging_config import get_logger


class PrivilegeValidator:
    """
    Validates MySQL user privileges and system configuration before executing database operations.
    
    This class provides comprehensive privilege checking, including SUPER privilege validation,
    CREATE ROUTINE privilege verification, binary logging status checks, and trust function
    creators setting validation. It generates detailed privilege reports with recommendations
    for resolving privilege-related issues.
    """
    
    def __init__(self, config: Optional[MySQLConfiguration] = None):
        """
        Initialize the PrivilegeValidator.
        
        Args:
            config: MySQL configuration. If None, uses default configuration.
        """
        self.config = config or MySQLConfiguration()
        self.logger = get_logger(__name__)
        self._connection = None
        
        if not MYSQL_CONNECTOR_AVAILABLE:
            self.logger.warning(
                "MySQL connector not available. Install mysql-connector-python for full functionality."
            )
    
    @contextmanager
    def _get_connection(self):
        """
        Get a MySQL connection with proper error handling and cleanup.
        
        Yields:
            MySQL connection object
            
        Raises:
            ConnectionError: If connection fails
            PrivilegeValidationError: If MySQL connector is not available
        """
        if not MYSQL_CONNECTOR_AVAILABLE:
            raise PrivilegeValidationError(
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
            
            # Remove empty values to use MySQL defaults
            connection_config = {k: v for k, v in connection_config.items() if v}
            
            self.logger.debug(f"Connecting to MySQL at {self.config.host}:{self.config.port}")
            connection = mysql.connector.connect(**connection_config)
            
            if not connection.is_connected():
                raise ConnectionError(
                    f"Failed to establish connection to MySQL server at {self.config.host}:{self.config.port}",
                    host=self.config.host,
                    port=self.config.port,
                    database=self.config.database
                )
            
            yield connection
            
        except MySQLError as e:
            error_msg = f"MySQL connection error: {e}"
            self.logger.error(error_msg)
            raise ConnectionError(
                error_msg,
                host=self.config.host,
                port=self.config.port,
                database=self.config.database
            )
        except Exception as e:
            error_msg = f"Unexpected connection error: {e}"
            self.logger.error(error_msg)
            raise ConnectionError(error_msg)
        finally:
            if connection and connection.is_connected():
                connection.close()
                self.logger.debug("MySQL connection closed")
    
    def _execute_query(self, query: str, params: Optional[Tuple] = None) -> List[Tuple]:
        """
        Execute a SQL query and return results.
        
        Args:
            query: SQL query to execute
            params: Query parameters
            
        Returns:
            List of result tuples
            
        Raises:
            PrivilegeValidationError: If query execution fails
        """
        try:
            with self._get_connection() as connection:
                cursor = connection.cursor()
                try:
                    cursor.execute(query, params)
                    results = cursor.fetchall()
                    self.logger.debug(f"Query executed successfully: {query[:100]}...")
                    return results
                finally:
                    cursor.close()
        except MySQLError as e:
            error_msg = f"Query execution failed: {e}"
            self.logger.error(error_msg)
            raise PrivilegeValidationError(error_msg, mysql_error_code=getattr(e, 'errno', None))
        except Exception as e:
            error_msg = f"Unexpected query error: {e}"
            self.logger.error(error_msg)
            raise PrivilegeValidationError(error_msg)
    
    def check_super_privilege(self) -> bool:
        """
        Check if the current user has SUPER privilege.
        
        Returns:
            True if user has SUPER privilege, False otherwise
            
        Raises:
            PrivilegeValidationError: If privilege check fails
        """
        try:
            # Query to check current user's grants
            grants = self._execute_query("SHOW GRANTS FOR CURRENT_USER()")
            
            # Check if any grant includes SUPER privilege
            for grant_row in grants:
                grant_statement = grant_row[0].upper()
                if 'GRANT ALL PRIVILEGES' in grant_statement or 'SUPER' in grant_statement:
                    self.logger.debug("SUPER privilege detected in grants")
                    return True
            
            self.logger.debug("SUPER privilege not found in grants")
            return False
            
        except Exception as e:
            self.logger.error(f"Failed to check SUPER privilege: {e}")
            raise PrivilegeValidationError(f"Failed to check SUPER privilege: {e}")
    
    def check_create_routine_privilege(self) -> bool:
        """
        Check if the current user has CREATE ROUTINE privilege.
        
        Returns:
            True if user has CREATE ROUTINE privilege, False otherwise
            
        Raises:
            PrivilegeValidationError: If privilege check fails
        """
        try:
            # Query to check current user's grants
            grants = self._execute_query("SHOW GRANTS FOR CURRENT_USER()")
            
            # Check if any grant includes CREATE ROUTINE privilege
            for grant_row in grants:
                grant_statement = grant_row[0].upper()
                if ('GRANT ALL PRIVILEGES' in grant_statement or 
                    'CREATE ROUTINE' in grant_statement):
                    self.logger.debug("CREATE ROUTINE privilege detected in grants")
                    return True
            
            self.logger.debug("CREATE ROUTINE privilege not found in grants")
            return False
            
        except Exception as e:
            self.logger.error(f"Failed to check CREATE ROUTINE privilege: {e}")
            raise PrivilegeValidationError(f"Failed to check CREATE ROUTINE privilege: {e}")
    
    def check_binary_logging_status(self) -> bool:
        """
        Check if binary logging is enabled on the MySQL server.
        
        Returns:
            True if binary logging is enabled, False otherwise
            
        Raises:
            PrivilegeValidationError: If binary logging status check fails
        """
        try:
            # Check log_bin system variable
            result = self._execute_query("SHOW VARIABLES LIKE 'log_bin'")
            
            if not result:
                self.logger.warning("Could not determine binary logging status")
                return False
            
            log_bin_value = result[0][1].upper()
            is_enabled = log_bin_value in ('ON', '1', 'TRUE')
            
            self.logger.debug(f"Binary logging status: {log_bin_value} (enabled: {is_enabled})")
            return is_enabled
            
        except Exception as e:
            self.logger.error(f"Failed to check binary logging status: {e}")
            raise PrivilegeValidationError(f"Failed to check binary logging status: {e}")
    
    def check_trust_function_creators(self) -> bool:
        """
        Check if log_bin_trust_function_creators is enabled.
        
        Returns:
            True if log_bin_trust_function_creators is enabled, False otherwise
            
        Raises:
            PrivilegeValidationError: If trust function creators check fails
        """
        try:
            # Check log_bin_trust_function_creators system variable
            result = self._execute_query("SHOW VARIABLES LIKE 'log_bin_trust_function_creators'")
            
            if not result:
                self.logger.warning("Could not determine log_bin_trust_function_creators status")
                return False
            
            trust_value = result[0][1].upper()
            is_enabled = trust_value in ('ON', '1', 'TRUE')
            
            self.logger.debug(f"log_bin_trust_function_creators status: {trust_value} (enabled: {is_enabled})")
            return is_enabled
            
        except Exception as e:
            self.logger.error(f"Failed to check log_bin_trust_function_creators: {e}")
            raise PrivilegeValidationError(f"Failed to check log_bin_trust_function_creators: {e}")
    
    def _get_mysql_version(self) -> str:
        """
        Get the MySQL server version.
        
        Returns:
            MySQL version string
        """
        try:
            result = self._execute_query("SELECT VERSION()")
            if result:
                version = result[0][0]
                self.logger.debug(f"MySQL version: {version}")
                return version
            return "Unknown"
        except Exception as e:
            self.logger.warning(f"Could not determine MySQL version: {e}")
            return "Unknown"
    
    def _get_current_user_grants(self) -> List[str]:
        """
        Get all grants for the current user.
        
        Returns:
            List of grant statements
        """
        try:
            grants = self._execute_query("SHOW GRANTS FOR CURRENT_USER()")
            grant_statements = [grant[0] for grant in grants]
            self.logger.debug(f"Retrieved {len(grant_statements)} grant statements")
            return grant_statements
        except Exception as e:
            self.logger.warning(f"Could not retrieve user grants: {e}")
            return []
    
    def _generate_recommendations(self, report: PrivilegeReport) -> List[str]:
        """
        Generate recommendations based on privilege report.
        
        Args:
            report: Current privilege report
            
        Returns:
            List of recommended actions
        """
        recommendations = []
        
        # Check for ERROR 1419 scenario
        if (report.binary_logging_enabled and 
            not report.has_super_privilege and 
            not report.trust_function_creators_enabled):
            recommendations.append(
                "Enable log_bin_trust_function_creators to allow function creation without SUPER privilege"
            )
            recommendations.append(
                "Add 'log_bin_trust_function_creators = 1' to MySQL configuration file"
            )
        
        # Check for missing CREATE ROUTINE privilege
        if not report.has_create_routine:
            recommendations.append(
                "Grant CREATE ROUTINE privilege to allow function and procedure creation"
            )
            recommendations.append(
                f"Execute: GRANT CREATE ROUTINE ON {self.config.database or '*'}.* TO CURRENT_USER()"
            )
        
        # Security recommendations
        if report.has_super_privilege:
            recommendations.append(
                "Consider using log_bin_trust_function_creators instead of SUPER privilege for better security"
            )
        
        # Environment-specific recommendations
        if self.config.environment.value == "production":
            if report.trust_function_creators_enabled:
                recommendations.append(
                    "Review function determinism in production environment with trust_function_creators enabled"
                )
        
        return recommendations
    
    def generate_privilege_report(self) -> PrivilegeReport:
        """
        Generate a comprehensive privilege report with current status and recommendations.
        
        Returns:
            PrivilegeReport with current privilege status and recommendations
            
        Raises:
            PrivilegeValidationError: If report generation fails
        """
        try:
            self.logger.info("Generating comprehensive privilege report")
            
            # Check all privilege aspects
            has_super = self.check_super_privilege()
            has_create_routine = self.check_create_routine_privilege()
            binary_logging_enabled = self.check_binary_logging_status()
            trust_function_creators = self.check_trust_function_creators()
            
            # Get additional information
            mysql_version = self._get_mysql_version()
            user_grants = self._get_current_user_grants()
            
            # Create privilege report
            report = PrivilegeReport(
                has_super_privilege=has_super,
                has_create_routine=has_create_routine,
                binary_logging_enabled=binary_logging_enabled,
                trust_function_creators_enabled=trust_function_creators,
                mysql_version=mysql_version,
                user_grants=user_grants
            )
            
            # Generate recommendations
            recommendations = self._generate_recommendations(report)
            report.recommended_actions = recommendations
            
            self.logger.info(f"Privilege report generated successfully. "
                           f"SUPER: {has_super}, CREATE ROUTINE: {has_create_routine}, "
                           f"Binary logging: {binary_logging_enabled}, "
                           f"Trust function creators: {trust_function_creators}")
            
            return report
            
        except Exception as e:
            error_msg = f"Failed to generate privilege report: {e}"
            self.logger.error(error_msg)
            raise PrivilegeValidationError(error_msg)
    
    def validate_function_creation_capability(self) -> bool:
        """
        Validate if the current configuration allows function creation.
        
        Returns:
            True if function creation should work, False otherwise
            
        Raises:
            SuperPrivilegeRequiredError: If ERROR 1419 conditions are detected
        """
        try:
            report = self.generate_privilege_report()
            
            # Check for ERROR 1419 conditions
            if (report.binary_logging_enabled and 
                not report.has_super_privilege and 
                not report.trust_function_creators_enabled):
                
                raise SuperPrivilegeRequiredError(
                    "Function creation will fail due to ERROR 1419: "
                    "Binary logging is enabled but user lacks SUPER privilege and "
                    "log_bin_trust_function_creators is disabled",
                    operation="CREATE FUNCTION"
                )
            
            # Check for CREATE ROUTINE privilege
            if not report.has_create_routine:
                self.logger.warning("CREATE ROUTINE privilege missing - function creation may fail")
                return False
            
            return True
            
        except SuperPrivilegeRequiredError:
            raise
        except Exception as e:
            self.logger.error(f"Function creation capability validation failed: {e}")
            return False