"""
Unit tests for PrivilegeValidator class.

This module tests the MySQL privilege validation functionality, including
privilege checking, binary logging status, and privilege report generation.
"""

import pytest
from unittest.mock import Mock, MagicMock, patch, call
from mysql_privilege_config.validators.privilege_validator import PrivilegeValidator
from mysql_privilege_config.core.models import (
    MySQLConfiguration,
    Environment,
    SecurityLevel,
    PrivilegeReport
)
from mysql_privilege_config.core.exceptions import (
    PrivilegeValidationError,
    ConnectionError,
    SuperPrivilegeRequiredError
)


class TestPrivilegeValidator:
    """Test cases for PrivilegeValidator class."""
    
    def test_init_with_default_config(self):
        """Test PrivilegeValidator initialization with default configuration."""
        validator = PrivilegeValidator()
        assert validator.config is not None
        assert isinstance(validator.config, MySQLConfiguration)
        assert validator.logger is not None
    
    def test_init_with_custom_config(self, sample_mysql_config):
        """Test PrivilegeValidator initialization with custom configuration."""
        validator = PrivilegeValidator(sample_mysql_config)
        assert validator.config == sample_mysql_config
        assert validator.config.host == "localhost"
        assert validator.config.port == 3306
    
    @patch('mysql.connector.connect')
    def test_get_connection_success(self, mock_connect, sample_mysql_config):
        """Test successful MySQL connection."""
        mock_connection = MagicMock()
        mock_connection.is_connected.return_value = True
        mock_connect.return_value = mock_connection
        
        validator = PrivilegeValidator(sample_mysql_config)
        
        with validator._get_connection() as connection:
            assert connection == mock_connection
            mock_connect.assert_called_once()
            
        mock_connection.close.assert_called_once()
    
    @patch('mysql.connector.connect')
    def test_get_connection_failure(self, mock_connect, sample_mysql_config):
        """Test MySQL connection failure."""
        mock_connect.side_effect = Exception("Connection failed")
        
        validator = PrivilegeValidator(sample_mysql_config)
        
        with pytest.raises(ConnectionError) as exc_info:
            with validator._get_connection():
                pass
        
        assert "Connection failed" in str(exc_info.value)
    
    @patch('mysql.connector.connect')
    def test_get_connection_not_connected(self, mock_connect, sample_mysql_config):
        """Test MySQL connection that fails to establish."""
        mock_connection = MagicMock()
        mock_connection.is_connected.return_value = False
        mock_connect.return_value = mock_connection
        
        validator = PrivilegeValidator(sample_mysql_config)
        
        with pytest.raises(ConnectionError) as exc_info:
            with validator._get_connection():
                pass
        
        assert "Failed to establish connection" in str(exc_info.value)
    
    @patch('mysql_privilege_config.validators.privilege_validator.MYSQL_CONNECTOR_AVAILABLE', False)
    def test_get_connection_no_connector(self, sample_mysql_config):
        """Test connection attempt when MySQL connector is not available."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with pytest.raises(PrivilegeValidationError) as exc_info:
            with validator._get_connection():
                pass
        
        assert "MySQL connector not available" in str(exc_info.value)
    
    def test_execute_query_success(self, sample_mysql_config):
        """Test successful query execution."""
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [('result1',), ('result2',)]
        mock_connection.cursor.return_value = mock_cursor
        
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_get_connection') as mock_get_conn:
            mock_get_conn.return_value.__enter__.return_value = mock_connection
            
            results = validator._execute_query("SELECT * FROM test")
            
            assert results == [('result1',), ('result2',)]
            mock_cursor.execute.assert_called_once_with("SELECT * FROM test", None)
            mock_cursor.fetchall.assert_called_once()
            mock_cursor.close.assert_called_once()
    
    def test_execute_query_with_params(self, sample_mysql_config):
        """Test query execution with parameters."""
        mock_connection = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [('result',)]
        mock_connection.cursor.return_value = mock_cursor
        
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_get_connection') as mock_get_conn:
            mock_get_conn.return_value.__enter__.return_value = mock_connection
            
            results = validator._execute_query("SELECT * FROM test WHERE id = %s", (1,))
            
            assert results == [('result',)]
            mock_cursor.execute.assert_called_once_with("SELECT * FROM test WHERE id = %s", (1,))
    
    def test_check_super_privilege_granted(self, sample_mysql_config):
        """Test SUPER privilege check when privilege is granted."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock grants that include SUPER privilege
        grants = [
            ("GRANT USAGE ON *.* TO 'test_user'@'localhost'",),
            ("GRANT SUPER ON *.* TO 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator.check_super_privilege()
            assert result is True
    
    def test_check_super_privilege_all_privileges(self, sample_mysql_config):
        """Test SUPER privilege check when ALL PRIVILEGES is granted."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock grants that include ALL PRIVILEGES
        grants = [
            ("GRANT ALL PRIVILEGES ON *.* TO 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator.check_super_privilege()
            assert result is True
    
    def test_check_super_privilege_not_granted(self, sample_mysql_config):
        """Test SUPER privilege check when privilege is not granted."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock grants without SUPER privilege
        grants = [
            ("GRANT USAGE ON *.* TO 'test_user'@'localhost'",),
            ("GRANT SELECT, INSERT ON test_db.* TO 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator.check_super_privilege()
            assert result is False
    
    def test_check_create_routine_privilege_granted(self, sample_mysql_config):
        """Test CREATE ROUTINE privilege check when privilege is granted."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock grants that include CREATE ROUTINE privilege
        grants = [
            ("GRANT USAGE ON *.* TO 'test_user'@'localhost'",),
            ("GRANT CREATE ROUTINE ON test_db.* TO 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator.check_create_routine_privilege()
            assert result is True
    
    def test_check_create_routine_privilege_not_granted(self, sample_mysql_config):
        """Test CREATE ROUTINE privilege check when privilege is not granted."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock grants without CREATE ROUTINE privilege
        grants = [
            ("GRANT USAGE ON *.* TO 'test_user'@'localhost'",),
            ("GRANT SELECT, INSERT ON test_db.* TO 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator.check_create_routine_privilege()
            assert result is False
    
    def test_check_binary_logging_enabled(self, sample_mysql_config):
        """Test binary logging status check when enabled."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock system variable showing binary logging enabled
        result = [("log_bin", "ON")]
        
        with patch.object(validator, '_execute_query', return_value=result):
            status = validator.check_binary_logging_status()
            assert status is True
    
    def test_check_binary_logging_disabled(self, sample_mysql_config):
        """Test binary logging status check when disabled."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock system variable showing binary logging disabled
        result = [("log_bin", "OFF")]
        
        with patch.object(validator, '_execute_query', return_value=result):
            status = validator.check_binary_logging_status()
            assert status is False
    
    def test_check_binary_logging_no_result(self, sample_mysql_config):
        """Test binary logging status check when no result is returned."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_execute_query', return_value=[]):
            status = validator.check_binary_logging_status()
            assert status is False
    
    def test_check_trust_function_creators_enabled(self, sample_mysql_config):
        """Test trust function creators check when enabled."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock system variable showing trust function creators enabled
        result = [("log_bin_trust_function_creators", "ON")]
        
        with patch.object(validator, '_execute_query', return_value=result):
            status = validator.check_trust_function_creators()
            assert status is True
    
    def test_check_trust_function_creators_disabled(self, sample_mysql_config):
        """Test trust function creators check when disabled."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock system variable showing trust function creators disabled
        result = [("log_bin_trust_function_creators", "OFF")]
        
        with patch.object(validator, '_execute_query', return_value=result):
            status = validator.check_trust_function_creators()
            assert status is False
    
    def test_get_mysql_version(self, sample_mysql_config):
        """Test MySQL version retrieval."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock version query result
        result = [("8.0.35",)]
        
        with patch.object(validator, '_execute_query', return_value=result):
            version = validator._get_mysql_version()
            assert version == "8.0.35"
    
    def test_get_mysql_version_failure(self, sample_mysql_config):
        """Test MySQL version retrieval when query fails."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_execute_query', side_effect=Exception("Query failed")):
            version = validator._get_mysql_version()
            assert version == "Unknown"
    
    def test_get_current_user_grants(self, sample_mysql_config):
        """Test current user grants retrieval."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock grants query result
        grants = [
            ("GRANT USAGE ON *.* TO 'test_user'@'localhost'",),
            ("GRANT CREATE ROUTINE ON test_db.* TO 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator._get_current_user_grants()
            expected = [
                "GRANT USAGE ON *.* TO 'test_user'@'localhost'",
                "GRANT CREATE ROUTINE ON test_db.* TO 'test_user'@'localhost'"
            ]
            assert result == expected
    
    def test_generate_recommendations_error_1419_scenario(self, sample_mysql_config):
        """Test recommendation generation for ERROR 1419 scenario."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Create report with ERROR 1419 conditions
        report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=True,
            binary_logging_enabled=True,
            trust_function_creators_enabled=False
        )
        
        recommendations = validator._generate_recommendations(report)
        
        assert any("log_bin_trust_function_creators" in rec for rec in recommendations)
        assert any("configuration file" in rec for rec in recommendations)
    
    def test_generate_recommendations_missing_create_routine(self, sample_mysql_config):
        """Test recommendation generation for missing CREATE ROUTINE privilege."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Create report with missing CREATE ROUTINE
        report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=False,
            binary_logging_enabled=False,
            trust_function_creators_enabled=False
        )
        
        recommendations = validator._generate_recommendations(report)
        
        assert any("CREATE ROUTINE" in rec for rec in recommendations)
        assert any("GRANT CREATE ROUTINE" in rec for rec in recommendations)
    
    def test_generate_privilege_report_success(self, sample_mysql_config):
        """Test successful privilege report generation."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock all the individual check methods
        with patch.object(validator, 'check_super_privilege', return_value=False), \
             patch.object(validator, 'check_create_routine_privilege', return_value=True), \
             patch.object(validator, 'check_binary_logging_status', return_value=True), \
             patch.object(validator, 'check_trust_function_creators', return_value=False), \
             patch.object(validator, '_get_mysql_version', return_value="8.0.35"), \
             patch.object(validator, '_get_current_user_grants', return_value=["GRANT USAGE ON *.*"]):
            
            report = validator.generate_privilege_report()
            
            assert isinstance(report, PrivilegeReport)
            assert report.has_super_privilege is False
            assert report.has_create_routine is True
            assert report.binary_logging_enabled is True
            assert report.trust_function_creators_enabled is False
            assert report.mysql_version == "8.0.35"
            assert len(report.recommended_actions) > 0
    
    def test_validate_function_creation_capability_success(self, sample_mysql_config):
        """Test function creation capability validation when conditions are met."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock a report that allows function creation
        report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=True,
            binary_logging_enabled=True,
            trust_function_creators_enabled=True  # This allows function creation
        )
        
        with patch.object(validator, 'generate_privilege_report', return_value=report):
            result = validator.validate_function_creation_capability()
            assert result is True
    
    def test_validate_function_creation_capability_error_1419(self, sample_mysql_config):
        """Test function creation capability validation when ERROR 1419 conditions exist."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock a report with ERROR 1419 conditions
        report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=True,
            binary_logging_enabled=True,
            trust_function_creators_enabled=False  # This causes ERROR 1419
        )
        
        with patch.object(validator, 'generate_privilege_report', return_value=report):
            with pytest.raises(SuperPrivilegeRequiredError) as exc_info:
                validator.validate_function_creation_capability()
            
            assert "ERROR 1419" in str(exc_info.value)
            assert exc_info.value.mysql_error_code == 1419
    
    def test_validate_function_creation_capability_missing_create_routine(self, sample_mysql_config):
        """Test function creation capability validation when CREATE ROUTINE is missing."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock a report missing CREATE ROUTINE privilege
        report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=False,  # Missing CREATE ROUTINE
            binary_logging_enabled=False,
            trust_function_creators_enabled=False
        )
        
        with patch.object(validator, 'generate_privilege_report', return_value=report):
            result = validator.validate_function_creation_capability()
            assert result is False


class TestPrivilegeValidatorEdgeCases:
    """Test edge cases and error conditions for PrivilegeValidator."""
    
    def test_privilege_check_query_failure(self, sample_mysql_config):
        """Test privilege check when query execution fails."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_execute_query', side_effect=Exception("Query failed")):
            with pytest.raises(PrivilegeValidationError):
                validator.check_super_privilege()
    
    def test_binary_logging_check_query_failure(self, sample_mysql_config):
        """Test binary logging check when query execution fails."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_execute_query', side_effect=Exception("Query failed")):
            with pytest.raises(PrivilegeValidationError):
                validator.check_binary_logging_status()
    
    def test_trust_function_creators_check_query_failure(self, sample_mysql_config):
        """Test trust function creators check when query execution fails."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, '_execute_query', side_effect=Exception("Query failed")):
            with pytest.raises(PrivilegeValidationError):
                validator.check_trust_function_creators()
    
    def test_generate_privilege_report_failure(self, sample_mysql_config):
        """Test privilege report generation when checks fail."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        with patch.object(validator, 'check_super_privilege', side_effect=Exception("Check failed")):
            with pytest.raises(PrivilegeValidationError):
                validator.generate_privilege_report()
    
    def test_case_insensitive_privilege_detection(self, sample_mysql_config):
        """Test that privilege detection is case insensitive."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Test with lowercase grants
        grants = [
            ("grant usage on *.* to 'test_user'@'localhost'",),
            ("grant super on *.* to 'test_user'@'localhost'",),
        ]
        
        with patch.object(validator, '_execute_query', return_value=grants):
            result = validator.check_super_privilege()
            assert result is True
    
    def test_binary_logging_various_values(self, sample_mysql_config):
        """Test binary logging detection with various value formats."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Test different ways MySQL might return "enabled"
        test_cases = [
            ("ON", True),
            ("on", True),
            ("1", True),
            ("TRUE", True),
            ("OFF", False),
            ("off", False),
            ("0", False),
            ("FALSE", False),
        ]
        
        for value, expected in test_cases:
            result = [("log_bin", value)]
            with patch.object(validator, '_execute_query', return_value=result):
                status = validator.check_binary_logging_status()
                assert status == expected, f"Failed for value: {value}"


class TestPrivilegeValidatorIntegration:
    """Integration-style tests for PrivilegeValidator."""
    
    def test_complete_privilege_validation_workflow(self, sample_mysql_config):
        """Test complete privilege validation workflow."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Mock all dependencies for a complete workflow
        with patch.object(validator, 'check_super_privilege', return_value=True), \
             patch.object(validator, 'check_create_routine_privilege', return_value=True), \
             patch.object(validator, 'check_binary_logging_status', return_value=True), \
             patch.object(validator, 'check_trust_function_creators', return_value=True), \
             patch.object(validator, '_get_mysql_version', return_value="8.0.35"), \
             patch.object(validator, '_get_current_user_grants', return_value=["GRANT ALL PRIVILEGES ON *.*"]):
            
            # Generate report
            report = validator.generate_privilege_report()
            
            # Validate function creation capability
            can_create = validator.validate_function_creation_capability()
            
            # Assertions
            assert report.has_super_privilege is True
            assert report.has_create_routine is True
            assert report.binary_logging_enabled is True
            assert report.trust_function_creators_enabled is True
            assert can_create is True
            assert "8.0.35" in report.mysql_version
    
    def test_error_1419_detection_and_handling(self, sample_mysql_config):
        """Test ERROR 1419 detection and proper exception handling."""
        validator = PrivilegeValidator(sample_mysql_config)
        
        # Set up ERROR 1419 conditions
        with patch.object(validator, 'check_super_privilege', return_value=False), \
             patch.object(validator, 'check_create_routine_privilege', return_value=True), \
             patch.object(validator, 'check_binary_logging_status', return_value=True), \
             patch.object(validator, 'check_trust_function_creators', return_value=False), \
             patch.object(validator, '_get_mysql_version', return_value="8.0.35"), \
             patch.object(validator, '_get_current_user_grants', return_value=["GRANT USAGE ON *.*"]):
            
            # Generate report should succeed
            report = validator.generate_privilege_report()
            assert len(report.recommended_actions) > 0
            
            # Function creation validation should raise ERROR 1419
            with pytest.raises(SuperPrivilegeRequiredError) as exc_info:
                validator.validate_function_creation_capability()
            
            assert exc_info.value.mysql_error_code == 1419
            assert "log_bin_trust_function_creators" in exc_info.value.resolution_steps[0]