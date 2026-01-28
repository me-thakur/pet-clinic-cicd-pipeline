"""
Unit tests for ScriptExecutor class.

Tests SQL script parsing, validation, DEFINER clause handling, retry logic,
and environment-specific script preparation functionality.
"""

import pytest
from unittest.mock import Mock, patch, MagicMock
import time

from mysql_privilege_config.executors.script_executor import ScriptExecutor
from mysql_privilege_config.core.models import (
    MySQLConfiguration, ExecutionOptions, Environment, ValidationResult,
    ExecutionResult, EnvironmentProfile
)
from mysql_privilege_config.core.exceptions import (
    ScriptExecutionError, SuperPrivilegeRequiredError, ConnectionError
)


class TestScriptExecutor:
    """Test cases for ScriptExecutor class."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.config = MySQLConfiguration(
            host="localhost",
            port=3306,
            database="test_db",
            username="test_user",
            password="test_pass",
            retry_attempts=3,
            timeout_seconds=30
        )
        self.executor = ScriptExecutor(self.config)
    
    def test_init_with_config(self):
        """Test ScriptExecutor initialization with configuration."""
        executor = ScriptExecutor(self.config)
        assert executor.config == self.config
        assert executor.logger is not None
    
    def test_init_without_config(self):
        """Test ScriptExecutor initialization without configuration."""
        executor = ScriptExecutor()
        assert executor.config is not None
        assert executor.config.host == "localhost"
        assert executor.config.port == 3306
    
    def test_execute_script_empty_script(self):
        """Test execute_script with empty script raises error."""
        with pytest.raises(ScriptExecutionError, match="Empty script provided"):
            self.executor.execute_script("")
        
        with pytest.raises(ScriptExecutionError, match="Empty script provided"):
            self.executor.execute_script("   ")
    
    @patch('mysql_privilege_config.executors.script_executor.MYSQL_CONNECTOR_AVAILABLE', False)
    def test_execute_script_no_mysql_connector(self):
        """Test execute_script without MySQL connector raises error."""
        executor = ScriptExecutor(self.config)
        
        # Should return failed result, not raise exception
        result = executor.execute_script("SELECT 1")
        assert not result.success
        assert any("MySQL connector not available" in error for error in result.errors)
    
    def test_validate_script_empty(self):
        """Test validate_script with empty script."""
        result = self.executor.validate_script("")
        assert not result.is_valid
        assert "Empty script provided" in result.syntax_errors[0]
        
        result = self.executor.validate_script("   ")
        assert not result.is_valid
        assert "Empty script provided" in result.syntax_errors[0]
    
    def test_validate_script_basic_select(self):
        """Test validate_script with basic SELECT statement."""
        script = "SELECT * FROM users"
        result = self.executor.validate_script(script)
        
        assert result.is_valid
        assert len(result.syntax_errors) == 0
        assert len(result.sql_statements) == 1
    
    def test_validate_script_function_creation(self):
        """Test validate_script with function creation."""
        script = """
        CREATE FUNCTION test_func(x INT) RETURNS INT
        DETERMINISTIC
        BEGIN
            RETURN x * 2;
        END
        """
        result = self.executor.validate_script(script)
        
        # Should be valid now with improved syntax validation
        assert result.is_valid
        assert any("function/procedure creation" in warning for warning in result.privilege_warnings)
    
    def test_validate_script_with_definer(self):
        """Test validate_script with DEFINER clause."""
        script = """
        CREATE DEFINER = 'user'@'localhost' FUNCTION test_func(x INT) RETURNS INT
        DETERMINISTIC
        BEGIN
            RETURN x * 2;
        END
        """
        result = self.executor.validate_script(script)
        
        # Should be valid now with improved syntax validation
        assert result.is_valid
        assert any("DEFINER clauses" in warning for warning in result.privilege_warnings)
    
    def test_validate_script_security_concerns(self):
        """Test validate_script identifies security concerns."""
        script = "DROP DATABASE test_db; GRANT ALL PRIVILEGES ON *.* TO 'user'@'%'"
        result = self.executor.validate_script(script)
        
        assert any("database/schema drop" in concern for concern in result.security_concerns)
        assert any("broad privilege grants" in concern for concern in result.security_concerns)
    
    def test_validate_script_syntax_errors(self):
        """Test validate_script identifies syntax errors."""
        script = "SELECT * FROM users WHERE (id = 1"  # Missing closing parenthesis
        result = self.executor.validate_script(script)
        
        assert not result.is_valid
        assert any("Unbalanced parentheses" in error for error in result.syntax_errors)
    
    def test_prepare_script_local_development(self):
        """Test prepare_script for local development environment."""
        script = "CREATE FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END"
        
        prepared = self.executor.prepare_script(script, Environment.LOCAL_DEVELOPMENT)
        
        # Local development should have minimal modifications
        assert "CREATE FUNCTION" in prepared
        # Should not add DEFINER in local development
        assert "DEFINER" not in prepared
    
    def test_prepare_script_ci_cd(self):
        """Test prepare_script for CI/CD environment."""
        script = "CREATE FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END"
        
        prepared = self.executor.prepare_script(script, Environment.CI_CD)
        
        # CI/CD should add DEFINER clauses
        assert "DEFINER = CURRENT_USER" in prepared
        assert "START TRANSACTION" in prepared
        assert "COMMIT" in prepared
    
    def test_prepare_script_production(self):
        """Test prepare_script for production environment."""
        script = "CREATE FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END"
        
        prepared = self.executor.prepare_script(script, Environment.PRODUCTION)
        
        # Production should add DEFINER clauses and error handling
        assert "DEFINER = CURRENT_USER" in prepared
        assert "START TRANSACTION" in prepared
        assert "sql_mode" in prepared
    
    def test_add_definer_clauses(self):
        """Test _add_definer_clauses method."""
        script = """
        CREATE FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END;
        CREATE PROCEDURE test_proc() BEGIN SELECT 1; END;
        CREATE TRIGGER test_trigger BEFORE INSERT ON test_table FOR EACH ROW BEGIN END;
        """
        
        result = self.executor._add_definer_clauses(script)
        
        assert result.count("DEFINER = CURRENT_USER") == 3
        assert "CREATE DEFINER = CURRENT_USER FUNCTION" in result
        assert "CREATE DEFINER = CURRENT_USER PROCEDURE" in result
        assert "CREATE DEFINER = CURRENT_USER TRIGGER" in result
    
    def test_add_definer_clauses_existing_definer(self):
        """Test _add_definer_clauses doesn't duplicate existing DEFINER."""
        script = "CREATE DEFINER = 'user'@'localhost' FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END"
        
        result = self.executor._add_definer_clauses(script)
        
        # Should not add another DEFINER clause
        assert result.count("DEFINER") == 1
        assert "'user'@'localhost'" in result
    
    def test_wrap_in_transaction(self):
        """Test _wrap_in_transaction method."""
        script = "INSERT INTO test_table VALUES (1, 'test')"
        
        result = self.executor._wrap_in_transaction(script)
        
        assert result.startswith("START TRANSACTION")
        assert result.endswith("COMMIT;")
        assert script in result
    
    def test_add_error_handling(self):
        """Test _add_error_handling method."""
        script = "SELECT * FROM test_table"
        
        result = self.executor._add_error_handling(script)
        
        assert "@old_sql_mode" in result
        assert "SET sql_mode" in result
        assert script in result
    
    def test_parse_script_statements(self):
        """Test _parse_script_statements method."""
        script = """
        SELECT * FROM users;
        INSERT INTO users (name) VALUES ('test');
        -- This is a comment
        UPDATE users SET name = 'updated' WHERE id = 1;
        """
        
        statements = self.executor._parse_script_statements(script)
        
        assert len(statements) == 3
        assert "SELECT * FROM users" in statements[0]
        assert "INSERT INTO users" in statements[1]
        assert "UPDATE users" in statements[2]
    
    def test_clean_sql_script(self):
        """Test _clean_sql_script method."""
        script = """
        -- Single line comment
        SELECT * FROM users; /* Multi-line
        comment */
        INSERT INTO test VALUES (1);
        """
        
        cleaned = self.executor._clean_sql_script(script)
        
        assert "-- Single line comment" not in cleaned
        assert "Multi-line comment" not in cleaned
        assert "SELECT * FROM users" in cleaned
        assert "INSERT INTO test" in cleaned
    
    def test_apply_security_context_production(self):
        """Test _apply_security_context for production environment."""
        script = "CREATE DEFINER = 'root'@'%' FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END"
        
        result = self.executor._apply_security_context(script, Environment.PRODUCTION)
        
        # Should replace root@% with CURRENT_USER in production
        assert "root@%" not in result
        assert "DEFINER = CURRENT_USER" in result
    
    def test_apply_privilege_workarounds(self):
        """Test _apply_privilege_workarounds method."""
        script = """
        CREATE FUNCTION test_func(x INT) RETURNS INT
        BEGIN RETURN x * 2; END;
        CREATE PROCEDURE test_proc()
        BEGIN SELECT 1; END;
        """
        
        result = self.executor._apply_privilege_workarounds(script)
        
        assert "DEFINER = CURRENT_USER" in result
        assert "SQL SECURITY DEFINER" in result
    
    def test_calculate_retry_delay(self):
        """Test _calculate_retry_delay method."""
        from mysql_privilege_config.core.models import RetryStrategy
        
        # No retry
        delay = self.executor._calculate_retry_delay(1, RetryStrategy.NO_RETRY)
        assert delay == 0
        
        # Linear backoff
        delay = self.executor._calculate_retry_delay(3, RetryStrategy.LINEAR_BACKOFF)
        assert delay == 3
        
        # Exponential backoff
        delay = self.executor._calculate_retry_delay(3, RetryStrategy.EXPONENTIAL_BACKOFF)
        assert delay == 8  # 2^3
        
        # Exponential backoff with cap
        delay = self.executor._calculate_retry_delay(10, RetryStrategy.EXPONENTIAL_BACKOFF)
        assert delay == 60  # Capped at 60 seconds
    
    def test_is_transient_error(self):
        """Test _is_transient_error method."""
        # Transient errors
        assert self.executor._is_transient_error(Exception("Connection timeout"))
        assert self.executor._is_transient_error(Exception("Lock wait timeout exceeded"))
        assert self.executor._is_transient_error(Exception("MySQL server has gone away"))
        
        # Non-transient errors
        assert not self.executor._is_transient_error(Exception("Syntax error"))
        assert not self.executor._is_transient_error(Exception("Access denied"))
    
    @patch('mysql_privilege_config.executors.script_executor.mysql.connector.connect')
    def test_execute_script_success(self, mock_connect):
        """Test successful script execution."""
        # Mock database connection and cursor
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_cursor.rowcount = 1
        mock_connect.return_value = mock_connection
        
        script = "INSERT INTO test_table VALUES (1, 'test')"
        options = ExecutionOptions(validate_before_execution=False)
        
        # Mock time with a cycle to provide infinite values
        from itertools import cycle
        with patch('time.time', side_effect=cycle([0, 0.001])):  # Infinite cycle of time values
            result = self.executor.execute_script(script, options)
        
        assert result.success
        assert result.affected_rows == 1
        assert result.execution_time_ms >= 0  # Allow 0 or positive values
        mock_connection.start_transaction.assert_called_once()
        mock_connection.commit.assert_called_once()
    
    @patch('mysql_privilege_config.executors.script_executor.mysql.connector.connect')
    def test_execute_script_super_privilege_error(self, mock_connect):
        """Test script execution with SUPER privilege error (ERROR 1419)."""
        # Mock database connection and cursor
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_connect.return_value = mock_connection
        
        # Mock MySQL error 1419
        from mysql_privilege_config.executors.script_executor import MySQLError
        mysql_error = MySQLError("You do not have the SUPER privilege")
        mysql_error.errno = 1419
        mock_cursor.execute.side_effect = mysql_error
        
        script = "CREATE FUNCTION test_func() RETURNS INT BEGIN RETURN 1; END"
        options = ExecutionOptions(
            validate_before_execution=False,
            retry_on_privilege_error=True,
            apply_definer_workarounds=True
        )
        
        result = self.executor.execute_script(script, options)
        
        assert not result.success
        assert any("privilege" in error.lower() for error in result.errors)
        mock_connection.rollback.assert_called()
    
    @patch('mysql_privilege_config.executors.script_executor.mysql.connector.connect')
    def test_execute_script_with_validation(self, mock_connect):
        """Test script execution with validation enabled."""
        # Mock database connection and cursor
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_cursor.rowcount = 0
        mock_connect.return_value = mock_connection
        
        script = "SELECT * FROM test_table"
        options = ExecutionOptions(validate_before_execution=True)
        
        result = self.executor.execute_script(script, options)
        
        assert result.success
        mock_connection.start_transaction.assert_called_once()
        mock_connection.commit.assert_called_once()
    
    @patch('mysql_privilege_config.executors.script_executor.mysql.connector.connect')
    def test_execute_script_validation_failure(self, mock_connect):
        """Test script execution with validation failure."""
        script = "INVALID SQL STATEMENT"
        options = ExecutionOptions(validate_before_execution=True)
        
        result = self.executor.execute_script(script, options)
        
        assert not result.success
        assert any("validation error" in error.lower() for error in result.errors)
        # Should not attempt database connection if validation fails
        mock_connect.assert_not_called()
    
    def test_prepare_script_empty(self):
        """Test prepare_script with empty script."""
        result = self.executor.prepare_script("", Environment.LOCAL_DEVELOPMENT)
        assert result == ""
        
        result = self.executor.prepare_script("   ", Environment.CI_CD)
        assert result == "   "
    
    def test_get_environment_profile(self):
        """Test _get_environment_profile method."""
        profile = self.executor._get_environment_profile(Environment.LOCAL_DEVELOPMENT)
        assert isinstance(profile, EnvironmentProfile)
        assert profile.name == Environment.LOCAL_DEVELOPMENT
        
        profile = self.executor._get_environment_profile(Environment.PRODUCTION)
        assert isinstance(profile, EnvironmentProfile)
        assert profile.name == Environment.PRODUCTION


class TestScriptExecutorIntegration:
    """Integration tests for ScriptExecutor with mocked MySQL connections."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.config = MySQLConfiguration(
            host="localhost",
            port=3306,
            database="test_db",
            username="test_user",
            password="test_pass"
        )
        self.executor = ScriptExecutor(self.config)
    
    @patch('mysql_privilege_config.executors.script_executor.mysql.connector.connect')
    def test_full_script_execution_workflow(self, mock_connect):
        """Test complete script execution workflow."""
        # Mock database connection and cursor
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_cursor.rowcount = 2
        mock_connect.return_value = mock_connection
        
        script = """
        CREATE FUNCTION test_func(x INT) RETURNS INT
        DETERMINISTIC
        BEGIN
            RETURN x * 2;
        END;
        
        INSERT INTO test_table VALUES (1, 'test');
        """
        
        options = ExecutionOptions(
            environment=Environment.CI_CD,
            validate_before_execution=True,
            retry_on_privilege_error=True,
            apply_definer_workarounds=True,
            log_sql_statements=True
        )
        
        # Mock time with a cycle to provide infinite values
        from itertools import cycle
        with patch('time.time', side_effect=cycle([0, 0.001])):  # Infinite cycle of time values
            result = self.executor.execute_script(script, options)
        
        assert result.success
        assert result.affected_rows >= 2  # At least 2 rows (could be more due to environment preparation)
        assert result.execution_time_ms >= 0
        assert len(result.sql_statements) > 0
        
        # Verify transaction handling
        mock_connection.start_transaction.assert_called_once()
        mock_connection.commit.assert_called_once()
        
        # Verify cursor was closed
        mock_cursor.close.assert_called()
    
    @patch('mysql_privilege_config.executors.script_executor.mysql.connector.connect')
    def test_retry_logic_with_recovery(self, mock_connect):
        """Test retry logic that eventually succeeds."""
        # Mock database connection and cursor
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_connect.return_value = mock_connection
        
        # First call fails, second succeeds
        from mysql_privilege_config.executors.script_executor import MySQLError
        mysql_error = MySQLError("Lock wait timeout exceeded")
        mock_cursor.execute.side_effect = [mysql_error, None]
        mock_cursor.rowcount = 1
        
        script = "INSERT INTO test_table VALUES (1, 'test')"
        options = ExecutionOptions(
            validate_before_execution=False,
            retry_on_privilege_error=True
        )
        
        # Mock sleep and time with infinite cycles
        from itertools import cycle
        with patch('time.sleep'), patch('time.time', side_effect=cycle([0, 0.001])):
            result = self.executor.execute_script(script, options)
        
        assert result.success
        assert any("attempt 2" in warning.lower() for warning in result.warnings)
        
        # Should have been called twice (first failure, then success)
        assert mock_cursor.execute.call_count == 2