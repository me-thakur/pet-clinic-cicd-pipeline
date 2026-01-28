"""
Unit tests for DatabaseConfigManager class.

Tests the central orchestrator for MySQL privilege configuration,
including environment detection integration, privilege validation,
configuration application, and rollback capabilities.
"""

import pytest
from unittest.mock import Mock, patch, MagicMock, call
from contextlib import contextmanager

from mysql_privilege_config.managers.config_manager import DatabaseConfigManager
from mysql_privilege_config.core.models import (
    Environment, MySQLConfiguration, ConfigurationResult, 
    PrivilegeReport, SecurityLevel, EnvironmentProfile
)
from mysql_privilege_config.core.exceptions import (
    ConfigurationError, PrivilegeValidationError, 
    SuperPrivilegeRequiredError, ConnectionError
)


class TestDatabaseConfigManager:
    """Test cases for DatabaseConfigManager class."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.config = MySQLConfiguration(
            host="localhost",
            port=3306,
            database="test_db",
            username="test_user",
            password="test_pass"
        )
        self.manager = DatabaseConfigManager(self.config)
    
    def test_init_with_config(self):
        """Test initialization with provided configuration."""
        manager = DatabaseConfigManager(self.config)
        assert manager.config == self.config
        assert manager.environment_detector is not None
        assert manager.privilege_validator is not None
        assert manager._detected_environment is None
        assert manager._current_profile is None
        assert manager._original_settings == {}
    
    def test_init_without_config(self):
        """Test initialization with default configuration."""
        manager = DatabaseConfigManager()
        assert isinstance(manager.config, MySQLConfiguration)
        assert manager.config.host == "localhost"
        assert manager.config.port == 3306
    
    @patch('mysql_privilege_config.managers.config_manager.EnvironmentDetector')
    def test_detect_environment_success(self, mock_detector_class):
        """Test successful environment detection."""
        # Setup mock
        mock_detector = Mock()
        mock_detector.detect_environment.return_value = Environment.CI_CD
        mock_detector_class.return_value = mock_detector
        
        # Create manager with mocked detector
        manager = DatabaseConfigManager(self.config)
        manager.environment_detector = mock_detector
        
        # Test detection
        result = manager.detect_environment()
        
        assert result == Environment.CI_CD
        assert manager._detected_environment == Environment.CI_CD
        assert manager.config.environment == Environment.CI_CD
        assert manager.config.security_level == SecurityLevel.BALANCED
        mock_detector.detect_environment.assert_called_once()
    
    @patch('mysql_privilege_config.managers.config_manager.EnvironmentDetector')
    def test_detect_environment_cached(self, mock_detector_class):
        """Test that environment detection is cached."""
        # Setup mock
        mock_detector = Mock()
        mock_detector.detect_environment.return_value = Environment.PRODUCTION
        mock_detector_class.return_value = mock_detector
        
        # Create manager with mocked detector
        manager = DatabaseConfigManager(self.config)
        manager.environment_detector = mock_detector
        
        # Test multiple calls
        result1 = manager.detect_environment()
        result2 = manager.detect_environment()
        
        assert result1 == Environment.PRODUCTION
        assert result2 == Environment.PRODUCTION
        # Should only call detector once due to caching
        mock_detector.detect_environment.assert_called_once()
    
    @patch('mysql_privilege_config.managers.config_manager.EnvironmentDetector')
    def test_detect_environment_failure(self, mock_detector_class):
        """Test environment detection failure handling."""
        # Setup mock to raise exception
        mock_detector = Mock()
        mock_detector.detect_environment.side_effect = Exception("Detection failed")
        mock_detector_class.return_value = mock_detector
        
        # Create manager with mocked detector
        manager = DatabaseConfigManager(self.config)
        manager.environment_detector = mock_detector
        
        # Test detection failure
        with pytest.raises(ConfigurationError) as exc_info:
            manager.detect_environment()
        
        assert "Failed to detect environment" in str(exc_info.value)
    
    @patch('mysql_privilege_config.managers.config_manager.mysql.connector.connect')
    def test_apply_configuration_success(self, mock_connect):
        """Test successful configuration application."""
        # Setup mock connection
        mock_connection = Mock()
        mock_cursor = Mock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_connect.return_value = mock_connection
        
        # Mock environment detection
        with patch.object(self.manager, 'detect_environment', return_value=Environment.LOCAL_DEVELOPMENT):
            with patch.object(self.manager, '_store_original_settings'):
                with patch.object(self.manager, '_validate_applied_configuration', return_value=True):
                    result = self.manager.apply_configuration()
        
        assert result.success is True
        assert "environment" in result.applied_settings
        assert result.applied_settings["environment"] == "local"
    
    @patch('mysql_privilege_config.managers.config_manager.mysql.connector.connect')
    def test_apply_configuration_with_specific_environment(self, mock_connect):
        """Test configuration application with specific environment."""
        # Setup mock connection
        mock_connection = Mock()
        mock_cursor = Mock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_connect.return_value = mock_connection
        
        # Test with specific environment
        with patch.object(self.manager, '_store_original_settings'):
            with patch.object(self.manager, '_validate_applied_configuration', return_value=True):
                result = self.manager.apply_configuration(Environment.PRODUCTION)
        
        assert result.success is True
        assert result.applied_settings["environment"] == "production"
    
    @patch('mysql_privilege_config.managers.config_manager.mysql.connector.connect')
    def test_apply_configuration_failure_with_rollback(self, mock_connect):
        """Test configuration application failure with rollback."""
        # Setup mock connection to fail
        mock_connect.side_effect = Exception("Connection failed")
        
        # Setup original settings for rollback
        self.manager._original_settings = {"log_bin_trust_function_creators": "OFF"}
        
        with patch.object(self.manager, 'detect_environment', return_value=Environment.CI_CD):
            with patch.object(self.manager, '_store_original_settings'):
                with patch.object(self.manager, '_attempt_rollback') as mock_rollback:
                    result = self.manager.apply_configuration()
        
        assert result.success is False
        assert len(result.errors) > 0
        mock_rollback.assert_called_once()
    
    @patch('mysql_privilege_config.managers.config_manager.PrivilegeValidator')
    def test_validate_privileges_success(self, mock_validator_class):
        """Test successful privilege validation."""
        # Setup mock validator
        mock_validator = Mock()
        mock_report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=True,
            binary_logging_enabled=True,
            trust_function_creators_enabled=True
        )
        mock_validator.generate_privilege_report.return_value = mock_report
        mock_validator.validate_function_creation_capability.return_value = True
        mock_validator_class.return_value = mock_validator
        
        # Create manager with mocked validator
        manager = DatabaseConfigManager(self.config)
        manager.privilege_validator = mock_validator
        
        # Test validation
        result = manager.validate_privileges()
        
        assert result is True
        mock_validator.generate_privilege_report.assert_called_once()
        mock_validator.validate_function_creation_capability.assert_called_once()
    
    @patch('mysql_privilege_config.managers.config_manager.PrivilegeValidator')
    def test_validate_privileges_super_privilege_required(self, mock_validator_class):
        """Test privilege validation with SUPER privilege required error."""
        # Setup mock validator to raise SuperPrivilegeRequiredError
        mock_validator = Mock()
        mock_validator.generate_privilege_report.return_value = PrivilegeReport()
        mock_validator.validate_function_creation_capability.side_effect = SuperPrivilegeRequiredError(
            "SUPER privilege required", operation="CREATE FUNCTION"
        )
        mock_validator_class.return_value = mock_validator
        
        # Create manager with mocked validator
        manager = DatabaseConfigManager(self.config)
        manager.privilege_validator = mock_validator
        
        # Test validation
        result = manager.validate_privileges()
        
        assert result is False
    
    @patch('mysql_privilege_config.managers.config_manager.PrivilegeValidator')
    def test_validate_privileges_failure(self, mock_validator_class):
        """Test privilege validation failure."""
        # Setup mock validator to raise exception
        mock_validator = Mock()
        mock_validator.generate_privilege_report.side_effect = Exception("Validation failed")
        mock_validator_class.return_value = mock_validator
        
        # Create manager with mocked validator
        manager = DatabaseConfigManager(self.config)
        manager.privilege_validator = mock_validator
        
        # Test validation failure
        with pytest.raises(PrivilegeValidationError):
            manager.validate_privileges()
    
    def test_execute_script_success(self):
        """Test successful script execution."""
        test_script = "CREATE FUNCTION test_func() RETURNS INT RETURN 1;"
        
        with patch.object(self.manager, 'validate_privileges', return_value=True):
            with patch.object(self.manager, 'detect_environment', return_value=Environment.LOCAL_DEVELOPMENT):
                with patch.object(self.manager, '_execute_sql_script', return_value=True):
                    result = self.manager.execute_script(test_script)
        
        assert result is True
    
    def test_execute_script_with_privilege_warning(self):
        """Test script execution with privilege validation warning."""
        test_script = "CREATE FUNCTION test_func() RETURNS INT RETURN 1;"
        
        with patch.object(self.manager, 'validate_privileges', return_value=False):
            with patch.object(self.manager, 'detect_environment', return_value=Environment.CI_CD):
                with patch.object(self.manager, '_execute_sql_script', return_value=True):
                    result = self.manager.execute_script(test_script)
        
        assert result is True
    
    def test_execute_script_failure(self):
        """Test script execution failure."""
        test_script = "INVALID SQL;"
        
        with patch.object(self.manager, 'validate_privileges', return_value=True):
            with patch.object(self.manager, 'detect_environment', return_value=Environment.PRODUCTION):
                with patch.object(self.manager, '_execute_sql_script', return_value=False):
                    result = self.manager.execute_script(test_script)
        
        assert result is False
    
    def test_execute_script_exception(self):
        """Test script execution with exception."""
        test_script = "CREATE FUNCTION test_func() RETURNS INT RETURN 1;"
        
        with patch.object(self.manager, 'validate_privileges', side_effect=Exception("Validation error")):
            with pytest.raises(ConfigurationError) as exc_info:
                self.manager.execute_script(test_script)
            
            assert "Script execution failed" in str(exc_info.value)
    
    @patch('mysql_privilege_config.managers.config_manager.PrivilegeValidator')
    def test_get_privilege_report(self, mock_validator_class):
        """Test getting privilege report."""
        # Setup mock validator
        mock_validator = Mock()
        mock_report = PrivilegeReport(has_super_privilege=True)
        mock_validator.generate_privilege_report.return_value = mock_report
        mock_validator_class.return_value = mock_validator
        
        # Create manager with mocked validator
        manager = DatabaseConfigManager(self.config)
        manager.privilege_validator = mock_validator
        
        # Test getting report
        result = manager.get_privilege_report()
        
        assert result == mock_report
        mock_validator.generate_privilege_report.assert_called_once()
    
    @patch('mysql_privilege_config.managers.config_manager.mysql.connector.connect')
    def test_rollback_configuration_success(self, mock_connect):
        """Test successful configuration rollback."""
        # Setup mock connection
        mock_connection = Mock()
        mock_cursor = Mock()
        mock_connection.cursor.return_value = mock_cursor
        mock_connection.is_connected.return_value = True
        mock_connect.return_value = mock_cursor
        
        # Setup original settings
        self.manager._original_settings = {
            "log_bin_trust_function_creators": "OFF",
            "innodb_flush_log_at_trx_commit": "1"
        }
        
        # Test rollback
        with patch.object(self.manager, '_apply_mysql_setting', return_value=True):
            result = self.manager.rollback_configuration()
        
        assert result.success is True
        assert len(self.manager._original_settings) == 0
    
    def test_rollback_configuration_no_original_settings(self):
        """Test rollback when no original settings are stored."""
        result = self.manager.rollback_configuration()
        
        assert result.success is True
        assert len(result.warnings) == 1
        assert "No original settings stored" in result.warnings[0]
    
    def test_get_security_level_for_environment(self):
        """Test security level mapping for different environments."""
        assert self.manager._get_security_level_for_environment(Environment.LOCAL_DEVELOPMENT) == SecurityLevel.PERMISSIVE
        assert self.manager._get_security_level_for_environment(Environment.CI_CD) == SecurityLevel.BALANCED
        assert self.manager._get_security_level_for_environment(Environment.PRODUCTION) == SecurityLevel.RESTRICTIVE
    
    def test_get_environment_profile(self):
        """Test getting environment profile."""
        profile = self.manager._get_environment_profile(Environment.CI_CD)
        
        assert isinstance(profile, EnvironmentProfile)
        assert profile.name == Environment.CI_CD
        assert "log_bin_trust_function_creators" in profile.mysql_config
        assert "allow_super_privilege" in profile.security_constraints
        assert "add_definer_clauses" in profile.script_modifications
    
    def test_prepare_script_for_environment_local(self):
        """Test script preparation for local development environment."""
        script = "CREATE FUNCTION test_func() RETURNS INT RETURN 1;"
        profile = EnvironmentProfile(Environment.LOCAL_DEVELOPMENT)
        
        result = self.manager._prepare_script_for_environment(script, profile)
        
        # Local development should not modify the script much
        assert "CREATE FUNCTION test_func()" in result
    
    def test_prepare_script_for_environment_production(self):
        """Test script preparation for production environment."""
        script = "CREATE FUNCTION test_func() RETURNS INT RETURN 1;"
        profile = EnvironmentProfile(Environment.PRODUCTION)
        
        result = self.manager._prepare_script_for_environment(script, profile)
        
        # Production should add DEFINER, transactions, and error handling
        assert "DEFINER = CURRENT_USER" in result
        assert "START TRANSACTION" in result
        assert "sql_mode" in result
    
    def test_add_definer_clauses(self):
        """Test adding DEFINER clauses to SQL statements."""
        script = """
        CREATE FUNCTION test_func() RETURNS INT RETURN 1;
        CREATE PROCEDURE test_proc() BEGIN SELECT 1; END;
        """
        
        result = self.manager._add_definer_clauses(script)
        
        assert "CREATE DEFINER = CURRENT_USER FUNCTION" in result
        assert "CREATE DEFINER = CURRENT_USER PROCEDURE" in result
    
    def test_wrap_in_transaction(self):
        """Test wrapping script in transaction."""
        script = "INSERT INTO test_table VALUES (1);"
        
        result = self.manager._wrap_in_transaction(script)
        
        assert "START TRANSACTION;" in result
        assert "COMMIT;" in result
        assert script in result
    
    def test_add_error_handling(self):
        """Test adding error handling to script."""
        script = "SELECT * FROM test_table;"
        
        result = self.manager._add_error_handling(script)
        
        assert "SET @old_sql_mode" in result
        assert "STRICT_TRANS_TABLES" in result
        assert script in result
        assert "SET sql_mode = @old_sql_mode" in result


class TestDatabaseConfigManagerIntegration:
    """Integration tests for DatabaseConfigManager with real components."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.config = MySQLConfiguration(
            host="localhost",
            port=3306,
            database="test_db",
            username="test_user",
            password="test_pass"
        )
    
    def test_manager_with_real_environment_detector(self):
        """Test manager with real EnvironmentDetector."""
        manager = DatabaseConfigManager(self.config)
        
        # Should not raise exception
        env = manager.detect_environment()
        assert isinstance(env, Environment)
    
    def test_manager_without_mysql_connector(self):
        """Test manager behavior when MySQL connector is not available."""
        with patch('mysql_privilege_config.managers.config_manager.MYSQL_CONNECTOR_AVAILABLE', False):
            manager = DatabaseConfigManager(self.config)
            
            # Should initialize without error but log warning
            assert manager.config == self.config
            
            # Should raise ConfigurationError when trying to use MySQL connection features
            with pytest.raises(ConfigurationError) as exc_info:
                with manager._get_connection():
                    pass
            
            assert "MySQL connector not available" in str(exc_info.value)