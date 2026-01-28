"""
Tests for the MySQL privilege configuration CLI.

This module tests the command-line interface functionality including
argument parsing, command execution, and error handling.
"""

import pytest
import sys
import json
from unittest.mock import Mock, patch, MagicMock
from io import StringIO
from pathlib import Path

from mysql_privilege_config.cli import MySQLPrivilegeCLI
from mysql_privilege_config.core.models import (
    Environment, PrivilegeReport, ConfigurationResult, ExecutionResult, ValidationResult
)
from mysql_privilege_config.core.exceptions import SuperPrivilegeRequiredError


class TestMySQLPrivilegeCLI:
    """Test cases for the MySQL privilege configuration CLI."""
    
    def setup_method(self):
        """Setup test fixtures."""
        self.cli = MySQLPrivilegeCLI()
    
    def test_create_parser(self):
        """Test that the argument parser is created correctly."""
        parser = self.cli.create_parser()
        
        # Test that parser exists and has expected attributes
        assert parser is not None
        assert parser.prog == 'mysql-privilege-config'
        
        # Test parsing help
        with pytest.raises(SystemExit):
            parser.parse_args(['--help'])
    
    def test_setup_configuration(self):
        """Test configuration setup from command line arguments."""
        # Mock arguments
        args = Mock()
        args.host = 'testhost'
        args.port = 3307
        args.user = 'testuser'
        args.password = 'testpass'
        args.database = 'testdb'
        
        config = self.cli.setup_configuration(args)
        
        assert config.host == 'testhost'
        assert config.port == 3307
        assert config.username == 'testuser'
        assert config.password == 'testpass'
        assert config.database == 'testdb'
    
    def test_setup_configuration_with_env_password(self):
        """Test configuration setup with password from environment."""
        args = Mock()
        args.host = 'localhost'
        args.port = 3306
        args.user = 'root'
        args.password = ''
        args.database = ''
        
        with patch.dict('os.environ', {'MYSQL_PASSWORD': 'env_password'}):
            config = self.cli.setup_configuration(args)
            assert config.password == 'env_password'
    
    @patch('mysql_privilege_config.cli.EnvironmentDetector')
    def test_cmd_detect_env_basic(self, mock_detector_class):
        """Test basic environment detection command."""
        # Setup mocks
        mock_detector = Mock()
        mock_detector.detect_environment.return_value = Environment.CI_CD
        mock_detector.is_ci_environment.return_value = True
        mock_detector.is_containerized.return_value = False
        mock_detector_class.return_value = mock_detector
        
        # Mock arguments
        args = Mock()
        args.summary = False
        args.output = 'text'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_detect_env(args)
            
            # Verify output
            mock_print.assert_any_call("Detected environment: ci")
            mock_print.assert_any_call("CI/CD environment: Yes")
            mock_print.assert_any_call("Containerized: No")
    
    @patch('mysql_privilege_config.cli.EnvironmentDetector')
    def test_cmd_detect_env_json_output(self, mock_detector_class):
        """Test environment detection with JSON output."""
        # Setup mocks
        mock_detector = Mock()
        mock_detector.detect_environment.return_value = Environment.LOCAL_DEVELOPMENT
        mock_detector.is_ci_environment.return_value = False
        mock_detector.is_containerized.return_value = True
        mock_detector_class.return_value = mock_detector
        
        # Mock arguments
        args = Mock()
        args.summary = False
        args.output = 'json'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_detect_env(args)
            
            # Verify JSON output was printed
            mock_print.assert_called_once()
            output = mock_print.call_args[0][0]
            result = json.loads(output)
            
            assert result['environment'] == 'local'
            assert result['is_ci'] is False
            assert result['is_containerized'] is True
    
    @patch('mysql_privilege_config.cli.DatabaseConfigManager')
    def test_cmd_configure_dry_run(self, mock_manager_class):
        """Test configuration command with dry run."""
        # Setup mocks
        mock_manager = Mock()
        mock_profile = Mock()
        mock_profile.mysql_config = {'log_bin_trust_function_creators': True}
        mock_profile.security_constraints = {'allow_super_privilege': False}
        mock_manager._get_environment_profile.return_value = mock_profile
        mock_manager_class.return_value = mock_manager
        
        # Setup CLI config
        self.cli.config = Mock()
        
        # Mock arguments
        args = Mock()
        args.environment = 'ci'
        args.dry_run = True
        args.rollback = False
        args.output = 'text'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_configure(args)
            
            # Verify dry run output
            mock_print.assert_any_call("Would configure for environment: ci")
    
    @patch('mysql_privilege_config.cli.PrivilegeValidator')
    def test_cmd_validate_check_functions_success(self, mock_validator_class):
        """Test privilege validation with function creation check - success case."""
        # Setup mocks
        mock_validator = Mock()
        mock_validator.validate_function_creation_capability.return_value = True
        mock_validator_class.return_value = mock_validator
        
        # Setup CLI config
        self.cli.config = Mock()
        
        # Mock arguments
        args = Mock()
        args.check_functions = True
        args.output = 'text'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_validate(args)
            
            # Verify output
            mock_print.assert_any_call("Can create functions: Yes")
    
    @patch('mysql_privilege_config.cli.PrivilegeValidator')
    def test_cmd_validate_check_functions_failure(self, mock_validator_class):
        """Test privilege validation with function creation check - failure case."""
        # Setup mocks
        mock_validator = Mock()
        mock_validator.validate_function_creation_capability.side_effect = SuperPrivilegeRequiredError(
            "SUPER privilege required",
            operation="CREATE FUNCTION"
        )
        mock_validator_class.return_value = mock_validator
        
        # Setup CLI config
        self.cli.config = Mock()
        
        # Mock arguments
        args = Mock()
        args.check_functions = True
        args.output = 'text'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_validate(args)
            
            # Verify error output
            mock_print.assert_any_call("Function creation will fail:")
    
    @patch('mysql_privilege_config.cli.ScriptExecutor')
    @patch('mysql_privilege_config.cli.EnvironmentDetector')
    @patch('mysql_privilege_config.cli.Path')
    def test_cmd_execute_validate_only(self, mock_path_class, mock_detector_class, mock_executor_class):
        """Test script execution with validate-only option."""
        # Setup mocks
        mock_path = Mock()
        mock_path.exists.return_value = True
        mock_path.read_text.return_value = "CREATE FUNCTION test() RETURNS INT RETURN 1;"
        mock_path_class.return_value = mock_path
        
        mock_detector = Mock()
        mock_detector.detect_environment.return_value = Environment.LOCAL_DEVELOPMENT
        mock_detector_class.return_value = mock_detector
        
        mock_executor = Mock()
        mock_validation_result = ValidationResult(is_valid=True)
        mock_executor.validate_script.return_value = mock_validation_result
        mock_executor_class.return_value = mock_executor
        
        # Setup CLI config
        self.cli.config = Mock()
        
        # Mock arguments
        args = Mock()
        args.script = 'test.sql'
        args.environment = None
        args.validate_only = True
        args.no_retry = False
        args.no_definer_workarounds = False
        args.verbose = False
        args.output = 'text'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_execute(args)
            
            # Verify validation was called
            mock_executor.validate_script.assert_called_once()
            mock_print.assert_any_call("Script validation: Valid")
    
    @patch('mysql_privilege_config.cli.PrivilegeValidator')
    def test_cmd_report_basic(self, mock_validator_class):
        """Test privilege report generation."""
        # Setup mocks
        mock_validator = Mock()
        mock_report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=True,
            binary_logging_enabled=True,
            trust_function_creators_enabled=True,
            mysql_version="8.0.35"
        )
        mock_report.recommended_actions = ["Enable log_bin_trust_function_creators"]
        mock_validator.generate_privilege_report.return_value = mock_report
        mock_validator_class.return_value = mock_validator
        
        # Setup CLI config
        self.cli.config = Mock()
        
        # Mock arguments
        args = Mock()
        args.include_grants = False
        args.output = 'text'
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_report(args)
            
            # Verify report output
            mock_print.assert_any_call("MySQL Privilege Report")
            mock_print.assert_any_call("MySQL Version: 8.0.35")
    
    @patch('mysql_privilege_config.cli.PrivilegeValidator')
    def test_cmd_troubleshoot_error_1419(self, mock_validator_class):
        """Test troubleshooting for ERROR 1419."""
        # Setup mocks
        mock_validator = Mock()
        mock_report = PrivilegeReport(
            has_super_privilege=False,
            has_create_routine=True,
            binary_logging_enabled=True,
            trust_function_creators_enabled=False
        )
        mock_validator.generate_privilege_report.return_value = mock_report
        mock_validator_class.return_value = mock_validator
        
        # Setup CLI config
        self.cli.config = Mock()
        
        # Mock arguments
        args = Mock()
        args.error_code = 1419
        args.test_function_creation = False
        
        # Capture output
        with patch('builtins.print') as mock_print:
            self.cli.cmd_troubleshoot(args)
            
            # Verify troubleshooting output
            mock_print.assert_any_call("Troubleshooting ERROR 1419 (SUPER privilege required)")
            mock_print.assert_any_call("\n❌ ERROR 1419 will occur with current configuration")
    
    def test_run_no_command(self):
        """Test running CLI with no command shows help."""
        with patch('sys.argv', ['mysql-privilege-config']):
            with patch.object(self.cli, 'setup_logging'):
                with patch.object(self.cli, 'setup_configuration'):
                    with patch('mysql_privilege_config.cli.EnvironmentDetector'):
                        # Mock parser to avoid actual help output
                        mock_parser = Mock()
                        mock_parser.parse_args.return_value = Mock(command=None)
                        mock_parser.print_help = Mock()
                        
                        with patch.object(self.cli, 'create_parser', return_value=mock_parser):
                            result = self.cli.run([])
                            
                            assert result == 0
                            mock_parser.print_help.assert_called_once()
    
    def test_run_unknown_command(self):
        """Test running CLI with unknown command."""
        with patch('builtins.print') as mock_print:
            with patch.object(self.cli, 'setup_logging'):
                with patch.object(self.cli, 'setup_configuration'):
                    with patch('mysql_privilege_config.cli.EnvironmentDetector'):
                        # Mock parser
                        mock_parser = Mock()
                        mock_args = Mock(command='unknown-command')
                        mock_parser.parse_args.return_value = mock_args
                        mock_parser.print_help = Mock()
                        
                        with patch.object(self.cli, 'create_parser', return_value=mock_parser):
                            result = self.cli.run(['unknown-command'])
                            
                            assert result == 1
                            mock_print.assert_any_call("Unknown command: unknown-command")
                            mock_parser.print_help.assert_called_once()


def test_main_function():
    """Test the main function entry point."""
    with patch('mysql_privilege_config.cli.MySQLPrivilegeCLI') as mock_cli_class:
        mock_cli = Mock()
        mock_cli.run.return_value = 0
        mock_cli_class.return_value = mock_cli
        
        from mysql_privilege_config.cli import main
        result = main()
        
        assert result == 0
        mock_cli.run.assert_called_once()


if __name__ == '__main__':
    pytest.main([__file__, '-v'])