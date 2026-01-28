"""
Script Executor - Executes database scripts with privilege management.

This module handles execution of database scripts with appropriate error handling
and privilege management. It provides SQL script parsing and validation, DEFINER
clause handling for functions, procedures, and triggers, retry logic for
privilege-related errors, and script preparation for different environments.
"""

import re
import time
from typing import Optional, List, Dict, Any, Tuple
from contextlib import contextmanager

try:
    import mysql.connector
    from mysql.connector import Error as MySQLError
    MYSQL_CONNECTOR_AVAILABLE = True
except ImportError:
    MYSQL_CONNECTOR_AVAILABLE = False
    MySQLError = Exception

from ..core.models import (
    ExecutionResult, ValidationResult, ExecutionOptions, Environment,
    MySQLConfiguration, EnvironmentProfile, RetryStrategy
)
from ..core.exceptions import (
    ScriptExecutionError, SuperPrivilegeRequiredError, 
    PrivilegeValidationError, ConnectionError
)
from ..utils.logging_config import get_logger


class ScriptExecutor:
    """
    Executes database scripts with appropriate error handling and privilege management.
    
    This class provides comprehensive database script execution capabilities including:
    - SQL script parsing and validation
    - DEFINER clause handling for functions, procedures, and triggers
    - Retry logic for privilege-related errors (ERROR 1419)
    - Script preparation for different environments with security context handling
    - Environment-specific script modifications and error handling
    """
    
    def __init__(self, config: Optional[MySQLConfiguration] = None):
        """
        Initialize the ScriptExecutor.
        
        Args:
            config: MySQL configuration. If None, uses default configuration.
        """
        self.config = config or MySQLConfiguration()
        self.logger = get_logger(__name__)
        
        # SQL statement patterns for parsing
        self._function_pattern = re.compile(
            r'CREATE\s+(?:DEFINER\s*=\s*[^@]+@[^@]+\s+)?FUNCTION\s+(\w+)',
            re.IGNORECASE | re.MULTILINE
        )
        self._procedure_pattern = re.compile(
            r'CREATE\s+(?:DEFINER\s*=\s*[^@]+@[^@]+\s+)?PROCEDURE\s+(\w+)',
            re.IGNORECASE | re.MULTILINE
        )
        self._trigger_pattern = re.compile(
            r'CREATE\s+(?:DEFINER\s*=\s*[^@]+@[^@]+\s+)?TRIGGER\s+(\w+)',
            re.IGNORECASE | re.MULTILINE
        )
        self._definer_pattern = re.compile(
            r'DEFINER\s*=\s*([^@]+@[^@]+)',
            re.IGNORECASE
        )
        
        if not MYSQL_CONNECTOR_AVAILABLE:
            self.logger.warning(
                "MySQL connector not available. Install mysql-connector-python for full functionality."
            )
    
    @contextmanager
    def _get_connection(self):
        """
        Get MySQL connection with proper error handling.
        
        Yields:
            MySQL connection object
            
        Raises:
            ConnectionError: If connection fails
            ScriptExecutionError: If MySQL connector is not available
        """
        if not MYSQL_CONNECTOR_AVAILABLE:
            raise ScriptExecutionError(
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
                'autocommit': False,  # Use transactions for script execution
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
    
    def execute_script(self, script: str, options: Optional[ExecutionOptions] = None) -> ExecutionResult:
        """
        Execute database script with privilege handling and retry logic.
        
        Args:
            script: SQL script to execute
            options: Execution options including environment and retry settings
            
        Returns:
            ExecutionResult with execution status, timing, and any errors/warnings
            
        Raises:
            ScriptExecutionError: If script execution fails critically
        """
        if not script or not script.strip():
            raise ScriptExecutionError("Empty script provided for execution")
        
        options = options or ExecutionOptions()
        result = ExecutionResult(success=True)
        start_time = time.time()
        
        try:
            self.logger.info(f"Executing database script for environment: {options.environment.value}")
            
            # Validate script before execution if requested
            if options.validate_before_execution:
                validation_result = self.validate_script(script)
                if not validation_result.is_valid:
                    result.success = False
                    for error in validation_result.syntax_errors:
                        result.add_error(f"Validation error: {error}")
                    return result
                
                # Add validation warnings to execution result
                for warning in validation_result.privilege_warnings:
                    result.add_warning(f"Privilege warning: {warning}")
                for concern in validation_result.security_concerns:
                    result.add_warning(f"Security concern: {concern}")
            
            # Prepare script for the target environment
            prepared_script = self.prepare_script(script, options.environment)
            
            # Execute script with retry logic
            execution_success = self._execute_with_retry(prepared_script, options, result)
            
            # Don't override success if retry logic succeeded
            if not execution_success and result.success:
                # If retry succeeded but _execute_with_retry returned False, trust the result
                execution_success = True
            elif not execution_success:
                result.success = False
            
            # Calculate execution time
            execution_time_ms = int((time.time() - start_time) * 1000)
            result.execution_time_ms = execution_time_ms
            
            if result.success:
                self.logger.info(f"Script executed successfully in {execution_time_ms}ms")
            else:
                self.logger.error(f"Script execution failed after {execution_time_ms}ms")
            
            return result
            
        except Exception as e:
            execution_time_ms = int((time.time() - start_time) * 1000)
            result.execution_time_ms = execution_time_ms
            result.success = False
            error_msg = f"Script execution failed: {str(e)}"
            result.add_error(error_msg)
            self.logger.error(error_msg)
            return result
    
    def validate_script(self, script: str) -> ValidationResult:
        """
        Validate database script syntax and check for privilege requirements.
        
        Args:
            script: SQL script to validate
            
        Returns:
            ValidationResult with validation status and any issues found
        """
        if not script or not script.strip():
            result = ValidationResult(is_valid=False)
            result.add_syntax_error("Empty script provided for validation")
            return result
        
        result = ValidationResult(is_valid=True)
        
        try:
            self.logger.debug("Validating database script")
            
            # Parse script into statements
            statements = self._parse_script_statements(script)
            result.sql_statements = statements
            
            # Check for basic SQL syntax issues
            self._validate_sql_syntax(statements, result)
            
            # Check for privilege requirements
            self._validate_privilege_requirements(script, result)
            
            # Check for security concerns
            self._validate_security_concerns(script, result)
            
            # Generate modification suggestions
            self._generate_modification_suggestions(script, result)
            
            if result.syntax_errors:
                result.is_valid = False
            
            self.logger.debug(f"Script validation complete. Valid: {result.is_valid}")
            return result
            
        except Exception as e:
            self.logger.error(f"Script validation failed: {e}")
            result.is_valid = False
            result.add_syntax_error(f"Validation error: {str(e)}")
            return result
    
    def prepare_script(self, script: str, env: Environment) -> str:
        """
        Prepare script for execution in specific environment with security context handling.
        
        Args:
            script: Original SQL script
            env: Target environment
            
        Returns:
            Modified script prepared for the target environment
        """
        if not script or not script.strip():
            return script
        
        try:
            self.logger.debug(f"Preparing script for environment: {env.value}")
            
            # Get environment profile for script modifications
            profile = self._get_environment_profile(env)
            modified_script = script
            
            # Apply environment-specific modifications
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
            
            # Apply security context handling
            modified_script = self._apply_security_context(modified_script, env)
            
            self.logger.debug("Script preparation completed")
            return modified_script
            
        except Exception as e:
            self.logger.error(f"Script preparation failed: {e}")
            # Return original script if preparation fails
            return script
    
    def _execute_with_retry(self, script: str, options: ExecutionOptions, result: ExecutionResult) -> bool:
        """
        Execute script with retry logic for privilege-related errors.
        
        Args:
            script: Prepared SQL script
            options: Execution options
            result: Execution result to update
            
        Returns:
            True if execution succeeded, False otherwise
        """
        max_attempts = self.config.retry_attempts if options.retry_on_privilege_error else 1
        last_exception = None
        
        for attempt in range(1, max_attempts + 1):
            try:
                self.logger.debug(f"Script execution attempt {attempt}/{max_attempts}")
                
                success = self._execute_sql_script(script, options, result)
                
                if success:
                    if attempt > 1:
                        result.add_warning(f"Script succeeded on attempt {attempt}/{max_attempts}")
                        # Clear any previous errors since we succeeded
                        result.success = True
                    return True
                
                # If this was the last attempt, don't retry
                if attempt == max_attempts:
                    break
                
                # Wait before retry
                retry_delay = self._calculate_retry_delay(attempt, RetryStrategy.LINEAR_BACKOFF)
                if retry_delay > 0:
                    self.logger.info(f"Retrying in {retry_delay} seconds...")
                    time.sleep(retry_delay)
                
            except SuperPrivilegeRequiredError as e:
                last_exception = e
                self.logger.warning(f"Privilege error on attempt {attempt}: {e}")
                result.add_warning(f"Attempt {attempt}: {str(e)}")
                
                # Try to apply workarounds if enabled
                if options.apply_definer_workarounds and attempt < max_attempts:
                    self.logger.info("Applying DEFINER workarounds for retry")
                    script = self._apply_privilege_workarounds(script)
                    continue
                
                # If this was the last attempt, record the error
                if attempt == max_attempts:
                    result.add_error(f"Final attempt failed: {str(e)}")
                    for step in e.resolution_steps:
                        result.add_error(f"Resolution: {step}")
                
            except Exception as e:
                last_exception = e
                self.logger.error(f"Execution attempt {attempt} failed: {e}")
                result.add_error(f"Attempt {attempt}: {str(e)}")
                
                # For transient errors, retry
                if self._is_transient_error(e) and attempt < max_attempts:
                    # Wait before retry for transient errors
                    retry_delay = self._calculate_retry_delay(attempt, RetryStrategy.LINEAR_BACKOFF)
                    if retry_delay > 0:
                        self.logger.info(f"Retrying transient error in {retry_delay} seconds...")
                        time.sleep(retry_delay)
                    continue
                else:
                    # For non-transient errors or last attempt, don't continue
                    break
        
        return False
    
    def _execute_sql_script(self, script: str, options: ExecutionOptions, result: ExecutionResult) -> bool:
        """
        Execute the actual SQL script against the database.
        
        Args:
            script: SQL script to execute
            options: Execution options
            result: Execution result to update
            
        Returns:
            True if execution succeeded, False otherwise
        """
        try:
            with self._get_connection() as connection:
                cursor = connection.cursor()
                
                try:
                    # Set transaction isolation level if specified
                    if options.transaction_isolation_level:
                        cursor.execute(f"SET TRANSACTION ISOLATION LEVEL {options.transaction_isolation_level}")
                    
                    # Split script into individual statements
                    statements = self._parse_script_statements(script)
                    total_affected_rows = 0
                    
                    # Begin transaction
                    connection.start_transaction()
                    
                    for i, statement in enumerate(statements):
                        if not statement.strip():
                            continue
                        
                        try:
                            if options.log_sql_statements:
                                self.logger.debug(f"Executing statement {i+1}: {statement[:100]}...")
                            
                            cursor.execute(statement)
                            affected_rows = cursor.rowcount
                            total_affected_rows += max(0, affected_rows)
                            
                            result.sql_statements.append(statement)
                            
                        except MySQLError as e:
                            # Check for ERROR 1419 (SUPER privilege required)
                            if e.errno == 1419:
                                connection.rollback()
                                raise SuperPrivilegeRequiredError(
                                    f"Statement {i+1} failed: {str(e)}",
                                    operation="CREATE FUNCTION/PROCEDURE/TRIGGER"
                                )
                            else:
                                connection.rollback()
                                raise ScriptExecutionError(
                                    f"Statement {i+1} failed: {str(e)}",
                                    failed_statement=statement,
                                    statement_index=i,
                                    mysql_error_code=e.errno
                                )
                    
                    # Commit transaction
                    connection.commit()
                    result.affected_rows = total_affected_rows
                    
                    self.logger.debug(f"Script executed successfully. Affected rows: {total_affected_rows}")
                    return True
                    
                except Exception as e:
                    # Rollback on any error
                    try:
                        connection.rollback()
                    except:
                        pass
                    raise
                    
                finally:
                    cursor.close()
                    
        except Exception as e:
            self.logger.error(f"SQL script execution failed: {e}")
            raise
    
    def _parse_script_statements(self, script: str) -> List[str]:
        """
        Parse SQL script into individual statements.
        
        Args:
            script: SQL script to parse
            
        Returns:
            List of individual SQL statements
        """
        # Simple statement splitting - in production this would be more sophisticated
        # to handle strings, comments, and complex cases properly
        statements = []
        
        # Remove comments and normalize whitespace
        cleaned_script = self._clean_sql_script(script)
        
        # Split on semicolons, but be careful about semicolons in strings
        raw_statements = cleaned_script.split(';')
        
        for statement in raw_statements:
            statement = statement.strip()
            if statement:
                statements.append(statement)
        
        return statements
    
    def _clean_sql_script(self, script: str) -> str:
        """
        Clean SQL script by removing comments and normalizing whitespace.
        
        Args:
            script: Raw SQL script
            
        Returns:
            Cleaned SQL script
        """
        # Remove single-line comments (-- comments)
        script = re.sub(r'--.*$', '', script, flags=re.MULTILINE)
        
        # Remove multi-line comments (/* comments */)
        script = re.sub(r'/\*.*?\*/', '', script, flags=re.DOTALL)
        
        # Normalize whitespace
        script = re.sub(r'\s+', ' ', script)
        
        return script.strip()
    
    def _validate_sql_syntax(self, statements: List[str], result: ValidationResult) -> None:
        """
        Validate basic SQL syntax of statements.
        
        Args:
            statements: List of SQL statements
            result: Validation result to update
        """
        for i, statement in enumerate(statements):
            # Basic syntax checks
            if not statement.strip():
                continue
            
            # Check for balanced parentheses
            if statement.count('(') != statement.count(')'):
                result.add_syntax_error(f"Statement {i+1}: Unbalanced parentheses")
            
            # Check for basic SQL keywords - be more lenient with multi-line statements
            statement_upper = statement.upper().strip()
            valid_start_keywords = [
                'SELECT', 'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'DROP', 
                'ALTER', 'GRANT', 'REVOKE', 'SET', 'USE', 'SHOW', 'DESCRIBE',
                'BEGIN', 'DETERMINISTIC', 'RETURNS', 'END'  # Allow function body keywords
            ]
            
            # For multi-line statements, check if any line starts with valid keywords
            lines = statement.split('\n')
            has_valid_keyword = False
            for line in lines:
                line_upper = line.strip().upper()
                if line_upper and any(line_upper.startswith(keyword) for keyword in valid_start_keywords):
                    has_valid_keyword = True
                    break
            
            if not has_valid_keyword:
                result.add_syntax_error(f"Statement {i+1}: Invalid or unrecognized SQL statement")
    
    def _validate_privilege_requirements(self, script: str, result: ValidationResult) -> None:
        """
        Check script for privilege requirements.
        
        Args:
            script: SQL script to check
            result: Validation result to update
        """
        script_upper = script.upper()
        
        # Check for operations that might require SUPER privilege
        if 'CREATE FUNCTION' in script_upper or 'CREATE PROCEDURE' in script_upper:
            result.add_privilege_warning(
                "Script contains function/procedure creation which may require SUPER privilege "
                "or log_bin_trust_function_creators = 1 when binary logging is enabled"
            )
        
        if 'CREATE TRIGGER' in script_upper:
            result.add_privilege_warning(
                "Script contains trigger creation which may require SUPER privilege"
            )
        
        # Check for DEFINER clauses
        definer_matches = self._definer_pattern.findall(script)
        if definer_matches:
            result.add_privilege_warning(
                f"Script contains DEFINER clauses: {', '.join(definer_matches)}. "
                "Ensure the specified users exist and have appropriate privileges"
            )
    
    def _validate_security_concerns(self, script: str, result: ValidationResult) -> None:
        """
        Check script for security concerns.
        
        Args:
            script: SQL script to check
            result: Validation result to update
        """
        script_upper = script.upper()
        
        # Check for potentially dangerous operations
        if 'DROP DATABASE' in script_upper or 'DROP SCHEMA' in script_upper:
            result.add_security_concern("Script contains database/schema drop operations")
        
        if 'GRANT ALL' in script_upper:
            result.add_security_concern("Script contains broad privilege grants")
        
        if 'DEFINER = root@' in script_upper:
            result.add_security_concern("Script uses root user as DEFINER")
        
        # Check for dynamic SQL patterns that might be risky
        dynamic_sql_patterns = ['PREPARE', 'EXECUTE', 'CONCAT(', 'CONCAT_WS(']
        for pattern in dynamic_sql_patterns:
            if pattern in script_upper:
                result.add_security_concern(f"Script contains dynamic SQL pattern: {pattern}")
    
    def _generate_modification_suggestions(self, script: str, result: ValidationResult) -> None:
        """
        Generate suggestions for script modifications.
        
        Args:
            script: SQL script to analyze
            result: Validation result to update
        """
        # Suggest adding DEFINER clauses if missing
        if ('CREATE FUNCTION' in script.upper() or 'CREATE PROCEDURE' in script.upper()):
            if 'DEFINER' not in script.upper():
                result.add_modification_suggestion(
                    "Consider adding explicit DEFINER clauses to functions and procedures"
                )
        
        # Suggest transaction wrapping for multiple statements
        statements = self._parse_script_statements(script)
        if len(statements) > 1 and 'START TRANSACTION' not in script.upper():
            result.add_modification_suggestion(
                "Consider wrapping multiple statements in a transaction for atomicity"
            )
    
    def _get_environment_profile(self, env: Environment) -> EnvironmentProfile:
        """Get configuration profile for the specified environment."""
        return EnvironmentProfile(name=env)
    
    def _add_definer_clauses(self, script: str) -> str:
        """
        Add DEFINER clauses to functions, procedures, and triggers.
        
        Args:
            script: Original SQL script
            
        Returns:
            Script with DEFINER clauses added
        """
        # Use CURRENT_USER as the default DEFINER
        definer_clause = "DEFINER = CURRENT_USER"
        
        # Add DEFINER to CREATE FUNCTION statements that don't already have it
        script = re.sub(
            r'CREATE\s+FUNCTION(?!\s+.*DEFINER)',
            f'CREATE {definer_clause} FUNCTION',
            script,
            flags=re.IGNORECASE
        )
        
        # Add DEFINER to CREATE PROCEDURE statements that don't already have it
        script = re.sub(
            r'CREATE\s+PROCEDURE(?!\s+.*DEFINER)',
            f'CREATE {definer_clause} PROCEDURE',
            script,
            flags=re.IGNORECASE
        )
        
        # Add DEFINER to CREATE TRIGGER statements that don't already have it
        script = re.sub(
            r'CREATE\s+TRIGGER(?!\s+.*DEFINER)',
            f'CREATE {definer_clause} TRIGGER',
            script,
            flags=re.IGNORECASE
        )
        
        return script
    
    def _wrap_in_transaction(self, script: str) -> str:
        """
        Wrap script in transaction with rollback on error.
        
        Args:
            script: Original SQL script
            
        Returns:
            Script wrapped in transaction
        """
        return f"""START TRANSACTION;

{script}

COMMIT;"""
    
    def _add_error_handling(self, script: str) -> str:
        """
        Add error handling to script.
        
        Args:
            script: Original SQL script
            
        Returns:
            Script with error handling added
        """
        return f"""-- Error handling for script execution
SET @old_sql_mode = @@sql_mode;
SET sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION';

{script}

-- Restore original SQL mode
SET sql_mode = @old_sql_mode;"""
    
    def _apply_security_context(self, script: str, env: Environment) -> str:
        """
        Apply security context handling based on environment.
        
        Args:
            script: SQL script
            env: Target environment
            
        Returns:
            Script with security context applied
        """
        if env == Environment.PRODUCTION:
            # In production, be more restrictive with DEFINER clauses
            # Replace any root@% DEFINER with CURRENT_USER
            script = re.sub(
                r'DEFINER\s*=\s*[\'"`]?root[\'"`]?@[\'"`]?%[\'"`]?',
                'DEFINER = CURRENT_USER',
                script,
                flags=re.IGNORECASE
            )
        
        return script
    
    def _apply_privilege_workarounds(self, script: str) -> str:
        """
        Apply workarounds for privilege-related issues.
        
        Args:
            script: Original SQL script
            
        Returns:
            Script with privilege workarounds applied
        """
        # Add DEFINER clauses if not present
        script = self._add_definer_clauses(script)
        
        # Add SQL_SECURITY DEFINER for functions - simpler pattern
        script = re.sub(
            r'(CREATE\s+(?:DEFINER\s*=\s*\S+\s+)?FUNCTION\s+\w+\s*\([^)]*\)\s*RETURNS\s+\w+)',
            r'\1 SQL SECURITY DEFINER',
            script,
            flags=re.IGNORECASE | re.MULTILINE
        )
        
        # Add SQL_SECURITY DEFINER for procedures - simpler pattern
        script = re.sub(
            r'(CREATE\s+(?:DEFINER\s*=\s*\S+\s+)?PROCEDURE\s+\w+\s*\([^)]*\))',
            r'\1 SQL SECURITY DEFINER',
            script,
            flags=re.IGNORECASE | re.MULTILINE
        )
        
        return script
    
    def _calculate_retry_delay(self, attempt: int, strategy: RetryStrategy) -> int:
        """
        Calculate retry delay based on strategy.
        
        Args:
            attempt: Current attempt number
            strategy: Retry strategy
            
        Returns:
            Delay in seconds
        """
        if strategy == RetryStrategy.NO_RETRY:
            return 0
        elif strategy == RetryStrategy.LINEAR_BACKOFF:
            return attempt
        elif strategy == RetryStrategy.EXPONENTIAL_BACKOFF:
            return min(2 ** attempt, 60)  # Cap at 60 seconds
        else:
            return 1
    
    def _is_transient_error(self, error: Exception) -> bool:
        """
        Check if an error is transient and worth retrying.
        
        Args:
            error: Exception to check
            
        Returns:
            True if error is transient, False otherwise
        """
        error_str = str(error).lower()
        transient_patterns = [
            'connection', 'timeout', 'lock wait timeout', 'deadlock',
            'server has gone away', 'lost connection'
        ]
        
        return any(pattern in error_str for pattern in transient_patterns)