"""
Command-line interface for MySQL privilege configuration.

This module provides CLI commands for environment detection, configuration,
privilege validation, and script execution with comprehensive error handling
and user-friendly output.
"""

import sys
import argparse
import json
import os
from pathlib import Path
from typing import Optional, Dict, Any, List
from contextlib import contextmanager

from .core.models import (
    Environment, MySQLConfiguration, SecurityLevel,
    ExecutionOptions, PrivilegeReport, ConfigurationResult
)
from .core.exceptions import (
    MySQLPrivilegeError, ConfigurationError, EnvironmentDetectionError,
    SuperPrivilegeRequiredError, ConnectionError
)
from .detectors.environment_detector import EnvironmentDetector
from .managers.config_manager import DatabaseConfigManager
from .validators.privilege_validator import PrivilegeValidator
from .executors.script_executor import ScriptExecutor
from .utils.error_handler import ErrorHandler
from .utils.logging_config import get_logger, setup_logging


class MySQLPrivilegeCLI:
    """
    Command-line interface for MySQL privilege configuration management.
    
    Provides commands for:
    - Environment detection and configuration
    - Privilege validation and reporting
    - Script execution with privilege handling
    - Configuration validation and troubleshooting
    """
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.config: Optional[MySQLConfiguration] = None
        self.error_handler: Optional[ErrorHandler] = None
    
    def create_parser(self) -> argparse.ArgumentParser:
        """Create the main argument parser with all subcommands."""
        parser = argparse.ArgumentParser(
            prog='mysql-privilege-config',
            description='MySQL Privilege Configuration Management Tool',
            formatter_class=argparse.RawDescriptionHelpFormatter,
            epilog="""
Examples:
  # Detect current environment
  mysql-privilege-config detect-env
  
  # Validate privileges for current user
  mysql-privilege-config validate --host localhost --user myuser
  
  # Apply configuration for CI environment
  mysql-privilege-config configure --environment ci --host mysql-ci
  
  # Execute script with privilege handling
  mysql-privilege-config execute --script schema.sql --environment production
  
  # Generate privilege report
  mysql-privilege-config report --output json --host prod-mysql
  
  # Troubleshoot privilege issues
  mysql-privilege-config troubleshoot --host localhost --user testuser
            """
        )
        
        # Global options
        parser.add_argument(
            '--host', default='localhost',
            help='MySQL host (default: localhost)'
        )
        parser.add_argument(
            '--port', type=int, default=3306,
            help='MySQL port (default: 3306)'
        )
        parser.add_argument(
            '--user', default='root',
            help='MySQL user (default: root)'
        )
        parser.add_argument(
            '--password', default='',
            help='MySQL password (use MYSQL_PASSWORD env var for security)'
        )
        parser.add_argument(
            '--database', default='',
            help='MySQL database name'
        )
        parser.add_argument(
            '--output', choices=['text', 'json'], default='text',
            help='Output format (default: text)'
        )
        parser.add_argument(
            '--verbose', '-v', action='store_true',
            help='Enable verbose logging'
        )
        parser.add_argument(
            '--quiet', '-q', action='store_true',
            help='Suppress non-error output'
        )
        
        # Create subparsers for commands
        subparsers = parser.add_subparsers(
            dest='command',
            help='Available commands',
            metavar='COMMAND'
        )
        
        # Environment detection command
        detect_parser = subparsers.add_parser(
            'detect-env',
            help='Detect current deployment environment',
            description='Automatically detect whether running in local, CI/CD, or production environment'
        )
        detect_parser.add_argument(
            '--summary', action='store_true',
            help='Show detailed detection summary'
        )
        
        # Configuration command
        config_parser = subparsers.add_parser(
            'configure',
            help='Apply MySQL configuration for environment',
            description='Apply environment-specific MySQL configuration settings'
        )
        config_parser.add_argument(
            '--environment', choices=['local', 'ci', 'production'],
            help='Target environment (auto-detected if not specified)'
        )
        config_parser.add_argument(
            '--dry-run', action='store_true',
            help='Show what would be configured without applying changes'
        )
        config_parser.add_argument(
            '--rollback', action='store_true',
            help='Rollback to previous configuration'
        )
        
        # Privilege validation command
        validate_parser = subparsers.add_parser(
            'validate',
            help='Validate MySQL user privileges',
            description='Check current user privileges and system configuration'
        )
        validate_parser.add_argument(
            '--check-functions', action='store_true',
            help='Test function creation capability'
        )
        
        # Script execution command
        execute_parser = subparsers.add_parser(
            'execute',
            help='Execute database script with privilege handling',
            description='Execute SQL script with automatic privilege management and error handling'
        )
        execute_parser.add_argument(
            '--script', required=True,
            help='Path to SQL script file'
        )
        execute_parser.add_argument(
            '--environment', choices=['local', 'ci', 'production'],
            help='Target environment for script modifications'
        )
        execute_parser.add_argument(
            '--validate-only', action='store_true',
            help='Only validate script without executing'
        )
        execute_parser.add_argument(
            '--no-retry', action='store_true',
            help='Disable retry on privilege errors'
        )
        execute_parser.add_argument(
            '--no-definer-workarounds', action='store_true',
            help='Disable automatic DEFINER clause workarounds'
        )
        
        # Privilege report command
        report_parser = subparsers.add_parser(
            'report',
            help='Generate comprehensive privilege report',
            description='Generate detailed report of current privileges and recommendations'
        )
        report_parser.add_argument(
            '--include-grants', action='store_true',
            help='Include detailed grant information'
        )
        
        # Troubleshooting command
        troubleshoot_parser = subparsers.add_parser(
            'troubleshoot',
            help='Troubleshoot privilege and configuration issues',
            description='Diagnose common privilege issues and provide resolution steps'
        )
        troubleshoot_parser.add_argument(
            '--error-code', type=int,
            help='MySQL error code to troubleshoot (e.g., 1419)'
        )
        troubleshoot_parser.add_argument(
            '--test-function-creation', action='store_true',
            help='Test function creation and diagnose issues'
        )
        
        return parser
    
    def setup_configuration(self, args: argparse.Namespace) -> MySQLConfiguration:
        """Setup MySQL configuration from command line arguments."""
        # Get password from environment variable if not provided
        password = args.password or os.getenv('MYSQL_PASSWORD', '')
        
        config = MySQLConfiguration(
            host=args.host,
            port=args.port,
            username=args.user,
            password=password,
            database=args.database,
        )
        
        return config
    
    def setup_logging(self, args: argparse.Namespace) -> None:
        """Setup logging based on command line arguments."""
        from .core.models import LogLevel
        
        if args.quiet:
            log_level = LogLevel.ERROR
        elif args.verbose:
            log_level = LogLevel.DEBUG
        else:
            log_level = LogLevel.INFO
        
        # Try to detect environment for logging setup
        try:
            detector = EnvironmentDetector()
            environment = detector.detect_environment()
        except Exception:
            environment = Environment.LOCAL_DEVELOPMENT
        
        setup_logging(environment=environment, log_level=log_level)
    
    @contextmanager
    def error_handling(self, operation_name: str):
        """Context manager for consistent error handling across commands."""
        try:
            yield
        except KeyboardInterrupt:
            self.logger.info(f"{operation_name} interrupted by user")
            sys.exit(130)  # Standard exit code for SIGINT
        except MySQLPrivilegeError as e:
            self.logger.error(f"{operation_name} failed: {e}")
            if self.error_handler:
                resolution = self.error_handler.handle_error(e)
                self._print_error_resolution(resolution)
            sys.exit(1)
        except Exception as e:
            self.logger.error(f"{operation_name} failed with unexpected error: {e}")
            if self.error_handler:
                resolution = self.error_handler.handle_error(e)
                self._print_error_resolution(resolution)
            sys.exit(1)
    
    def _print_error_resolution(self, resolution) -> None:
        """Print error resolution steps to the user."""
        if resolution.user_instructions:
            print("\nResolution steps:")
            for i, instruction in enumerate(resolution.user_instructions, 1):
                print(f"  {i}. {instruction}")
    
    def _output_result(self, data: Any, output_format: str) -> None:
        """Output result in the specified format."""
        if output_format == 'json':
            if hasattr(data, 'to_dict'):
                print(json.dumps(data.to_dict(), indent=2))
            elif isinstance(data, dict):
                print(json.dumps(data, indent=2))
            else:
                print(json.dumps(str(data), indent=2))
        else:
            print(data)
    
    def cmd_detect_env(self, args: argparse.Namespace) -> None:
        """Handle environment detection command."""
        with self.error_handling("Environment detection"):
            detector = EnvironmentDetector()
            
            if args.summary:
                summary = detector.get_detection_summary()
                self._output_result(summary, args.output)
            else:
                environment = detector.detect_environment()
                
                if args.output == 'json':
                    result = {
                        'environment': environment.value,
                        'is_ci': detector.is_ci_environment(),
                        'is_containerized': detector.is_containerized()
                    }
                    self._output_result(result, args.output)
                else:
                    print(f"Detected environment: {environment.value}")
                    print(f"CI/CD environment: {'Yes' if detector.is_ci_environment() else 'No'}")
                    print(f"Containerized: {'Yes' if detector.is_containerized() else 'No'}")
    
    def cmd_configure(self, args: argparse.Namespace) -> None:
        """Handle configuration command."""
        with self.error_handling("Configuration"):
            config_manager = DatabaseConfigManager(self.config)
            
            # Handle rollback
            if args.rollback:
                result = config_manager.rollback_configuration()
                if args.output == 'json':
                    self._output_result(result, args.output)
                else:
                    if result.success:
                        print("Configuration rolled back successfully")
                    else:
                        print("Configuration rollback failed:")
                        for error in result.errors:
                            print(f"  - {error}")
                return
            
            # Determine target environment
            if args.environment:
                env_map = {
                    'local': Environment.LOCAL_DEVELOPMENT,
                    'ci': Environment.CI_CD,
                    'production': Environment.PRODUCTION
                }
                target_env = env_map[args.environment]
            else:
                target_env = config_manager.detect_environment()
            
            if args.dry_run:
                # Show what would be configured
                profile = config_manager._get_environment_profile(target_env)
                
                if args.output == 'json':
                    dry_run_result = {
                        'target_environment': target_env.value,
                        'mysql_config': profile.mysql_config,
                        'security_constraints': profile.security_constraints,
                        'script_modifications': profile.script_modifications
                    }
                    self._output_result(dry_run_result, args.output)
                else:
                    print(f"Would configure for environment: {target_env.value}")
                    print("\nMySQL configuration changes:")
                    for key, value in profile.mysql_config.items():
                        print(f"  {key}: {value}")
                    print("\nSecurity constraints:")
                    for key, value in profile.security_constraints.items():
                        print(f"  {key}: {value}")
            else:
                # Apply configuration
                result = config_manager.apply_configuration(target_env)
                
                if args.output == 'json':
                    self._output_result(result, args.output)
                else:
                    if result.success:
                        print(f"Configuration applied successfully for {target_env.value} environment")
                        if result.applied_settings:
                            print("\nApplied settings:")
                            for key, value in result.applied_settings.items():
                                print(f"  {key}: {value}")
                    else:
                        print("Configuration failed:")
                        for error in result.errors:
                            print(f"  - {error}")
                    
                    if result.warnings:
                        print("\nWarnings:")
                        for warning in result.warnings:
                            print(f"  - {warning}")
    
    def cmd_validate(self, args: argparse.Namespace) -> None:
        """Handle privilege validation command."""
        with self.error_handling("Privilege validation"):
            validator = PrivilegeValidator(self.config)
            
            if args.check_functions:
                # Test function creation capability
                try:
                    can_create = validator.validate_function_creation_capability()
                    
                    if args.output == 'json':
                        result = {'can_create_functions': can_create}
                        self._output_result(result, args.output)
                    else:
                        status = "Yes" if can_create else "No"
                        print(f"Can create functions: {status}")
                        
                        if not can_create:
                            print("\nFunction creation may fail due to privilege restrictions.")
                            print("Run 'mysql-privilege-config troubleshoot --test-function-creation' for details.")
                
                except SuperPrivilegeRequiredError as e:
                    if args.output == 'json':
                        result = {
                            'can_create_functions': False,
                            'error': str(e),
                            'resolution_steps': e.resolution_steps
                        }
                        self._output_result(result, args.output)
                    else:
                        print("Function creation will fail:")
                        print(f"  {e}")
                        print("\nResolution steps:")
                        for step in e.resolution_steps:
                            print(f"  - {step}")
            else:
                # General privilege validation - generate report
                report = validator.generate_privilege_report()
                
                # Determine if there are privilege issues
                has_issues = (
                    (report.binary_logging_enabled and 
                     not report.has_super_privilege and 
                     not report.trust_function_creators_enabled) or
                    not report.has_create_routine
                )
                
                if args.output == 'json':
                    result = {
                        'privileges_valid': not has_issues,
                        'has_super_privilege': report.has_super_privilege,
                        'has_create_routine': report.has_create_routine,
                        'binary_logging_enabled': report.binary_logging_enabled,
                        'trust_function_creators_enabled': report.trust_function_creators_enabled
                    }
                    self._output_result(result, args.output)
                else:
                    status = "Valid" if not has_issues else "Issues found"
                    print(f"Privilege validation: {status}")
                    
                    if has_issues:
                        print("\nRun 'mysql-privilege-config report' for detailed analysis.")
    
    def cmd_execute(self, args: argparse.Namespace) -> None:
        """Handle script execution command."""
        with self.error_handling("Script execution"):
            # Check if script file exists
            script_path = Path(args.script)
            if not script_path.exists():
                raise FileNotFoundError(f"Script file not found: {args.script}")
            
            # Read script content
            script_content = script_path.read_text(encoding='utf-8')
            
            # Setup execution options
            if args.environment:
                env_map = {
                    'local': Environment.LOCAL_DEVELOPMENT,
                    'ci': Environment.CI_CD,
                    'production': Environment.PRODUCTION
                }
                target_env = env_map[args.environment]
            else:
                detector = EnvironmentDetector()
                target_env = detector.detect_environment()
            
            options = ExecutionOptions(
                environment=target_env,
                retry_on_privilege_error=not args.no_retry,
                apply_definer_workarounds=not args.no_definer_workarounds,
                validate_before_execution=True,
                log_sql_statements=args.verbose
            )
            
            executor = ScriptExecutor(self.config)
            
            if args.validate_only:
                # Only validate the script
                validation_result = executor.validate_script(script_content)
                
                if args.output == 'json':
                    self._output_result(validation_result, args.output)
                else:
                    status = "Valid" if validation_result.is_valid else "Invalid"
                    print(f"Script validation: {status}")
                    
                    if validation_result.syntax_errors:
                        print("\nSyntax errors:")
                        for error in validation_result.syntax_errors:
                            print(f"  - {error}")
                    
                    if validation_result.privilege_warnings:
                        print("\nPrivilege warnings:")
                        for warning in validation_result.privilege_warnings:
                            print(f"  - {warning}")
                    
                    if validation_result.security_concerns:
                        print("\nSecurity concerns:")
                        for concern in validation_result.security_concerns:
                            print(f"  - {concern}")
                    
                    if validation_result.suggested_modifications:
                        print("\nSuggested modifications:")
                        for suggestion in validation_result.suggested_modifications:
                            print(f"  - {suggestion}")
            else:
                # Execute the script
                execution_result = executor.execute_script(script_content, options)
                
                if args.output == 'json':
                    self._output_result(execution_result, args.output)
                else:
                    status = "Success" if execution_result.success else "Failed"
                    print(f"Script execution: {status}")
                    print(f"Execution time: {execution_result.execution_time_ms}ms")
                    print(f"Affected rows: {execution_result.affected_rows}")
                    
                    if execution_result.warnings:
                        print("\nWarnings:")
                        for warning in execution_result.warnings:
                            print(f"  - {warning}")
                    
                    if execution_result.errors:
                        print("\nErrors:")
                        for error in execution_result.errors:
                            print(f"  - {error}")
    
    def cmd_report(self, args: argparse.Namespace) -> None:
        """Handle privilege report command."""
        with self.error_handling("Privilege report generation"):
            validator = PrivilegeValidator(self.config)
            report = validator.generate_privilege_report()
            
            if args.output == 'json':
                report_dict = {
                    'has_super_privilege': report.has_super_privilege,
                    'has_create_routine': report.has_create_routine,
                    'binary_logging_enabled': report.binary_logging_enabled,
                    'trust_function_creators_enabled': report.trust_function_creators_enabled,
                    'mysql_version': report.mysql_version,
                    'recommended_actions': report.recommended_actions
                }
                
                if args.include_grants:
                    report_dict['user_grants'] = report.user_grants
                
                self._output_result(report_dict, args.output)
            else:
                print("MySQL Privilege Report")
                print("=" * 50)
                print(f"MySQL Version: {report.mysql_version}")
                print(f"SUPER Privilege: {'Yes' if report.has_super_privilege else 'No'}")
                print(f"CREATE ROUTINE Privilege: {'Yes' if report.has_create_routine else 'No'}")
                print(f"Binary Logging Enabled: {'Yes' if report.binary_logging_enabled else 'No'}")
                print(f"Trust Function Creators: {'Yes' if report.trust_function_creators_enabled else 'No'}")
                
                if report.recommended_actions:
                    print("\nRecommended Actions:")
                    for i, action in enumerate(report.recommended_actions, 1):
                        print(f"  {i}. {action}")
                
                if args.include_grants and report.user_grants:
                    print("\nUser Grants:")
                    for grant in report.user_grants:
                        print(f"  {grant}")
    
    def cmd_troubleshoot(self, args: argparse.Namespace) -> None:
        """Handle troubleshooting command."""
        with self.error_handling("Troubleshooting"):
            config_manager = DatabaseConfigManager(self.config)
            validator = PrivilegeValidator(self.config)
            
            if args.error_code:
                # Troubleshoot specific error code
                if args.error_code == 1419:
                    print("Troubleshooting ERROR 1419 (SUPER privilege required)")
                    print("=" * 60)
                    
                    # Check current configuration
                    report = validator.generate_privilege_report()
                    
                    print(f"Binary logging enabled: {'Yes' if report.binary_logging_enabled else 'No'}")
                    print(f"SUPER privilege: {'Yes' if report.has_super_privilege else 'No'}")
                    print(f"Trust function creators: {'Yes' if report.trust_function_creators_enabled else 'No'}")
                    
                    # Determine if ERROR 1419 would occur
                    if (report.binary_logging_enabled and 
                        not report.has_super_privilege and 
                        not report.trust_function_creators_enabled):
                        print("\n❌ ERROR 1419 will occur with current configuration")
                        print("\nResolution options:")
                        print("  1. Enable log_bin_trust_function_creators = 1")
                        print("  2. Grant SUPER privilege to user (not recommended)")
                        print("  3. Disable binary logging (not recommended for production)")
                    else:
                        print("\n✅ ERROR 1419 should not occur with current configuration")
                else:
                    print(f"Troubleshooting for error code {args.error_code} not implemented")
            
            elif args.test_function_creation:
                # Test function creation capability
                print("Testing function creation capability")
                print("=" * 40)
                
                try:
                    can_create = validator.validate_function_creation_capability()
                    if can_create:
                        print("✅ Function creation should work")
                    else:
                        print("❌ Function creation may fail")
                        
                        # Get detailed report
                        report = validator.generate_privilege_report()
                        print("\nCurrent configuration:")
                        print(f"  Binary logging: {'Enabled' if report.binary_logging_enabled else 'Disabled'}")
                        print(f"  SUPER privilege: {'Yes' if report.has_super_privilege else 'No'}")
                        print(f"  CREATE ROUTINE privilege: {'Yes' if report.has_create_routine else 'No'}")
                        print(f"  Trust function creators: {'Enabled' if report.trust_function_creators_enabled else 'Disabled'}")
                        
                        if report.recommended_actions:
                            print("\nRecommended actions:")
                            for i, action in enumerate(report.recommended_actions, 1):
                                print(f"  {i}. {action}")
                
                except SuperPrivilegeRequiredError as e:
                    print("❌ Function creation will fail:")
                    print(f"  {e}")
                    print("\nResolution steps:")
                    for step in e.resolution_steps:
                        print(f"  - {step}")
            
            else:
                # General troubleshooting
                print("MySQL Privilege Configuration Troubleshooting")
                print("=" * 50)
                
                # Environment detection
                detector = EnvironmentDetector()
                environment = detector.detect_environment()
                print(f"Detected environment: {environment.value}")
                
                # Privilege validation
                try:
                    report = validator.generate_privilege_report()
                    print(f"MySQL version: {report.mysql_version}")
                    print(f"Connection: ✅ Successful")
                    
                    # Check for common issues
                    issues = []
                    
                    if (report.binary_logging_enabled and 
                        not report.has_super_privilege and 
                        not report.trust_function_creators_enabled):
                        issues.append("ERROR 1419 will occur when creating functions")
                    
                    if not report.has_create_routine:
                        issues.append("Missing CREATE ROUTINE privilege")
                    
                    if issues:
                        print("\n❌ Issues found:")
                        for issue in issues:
                            print(f"  - {issue}")
                        
                        if report.recommended_actions:
                            print("\nRecommended actions:")
                            for i, action in enumerate(report.recommended_actions, 1):
                                print(f"  {i}. {action}")
                    else:
                        print("\n✅ No privilege issues detected")
                
                except Exception as e:
                    print(f"❌ Connection failed: {e}")
                    print("\nCheck connection parameters and MySQL server status")
    
    def run(self, args: Optional[List[str]] = None) -> int:
        """Run the CLI application."""
        parser = self.create_parser()
        parsed_args = parser.parse_args(args)
        
        # Setup logging
        self.setup_logging(parsed_args)
        
        # Setup configuration
        self.config = self.setup_configuration(parsed_args)
        
        # Setup error handler
        try:
            detector = EnvironmentDetector()
            environment = detector.detect_environment()
            self.error_handler = ErrorHandler(environment)
        except Exception:
            # Fallback to default environment if detection fails
            self.error_handler = ErrorHandler(Environment.LOCAL_DEVELOPMENT)
        
        # Handle commands
        if not parsed_args.command:
            parser.print_help()
            return 0
        
        command_handlers = {
            'detect-env': self.cmd_detect_env,
            'configure': self.cmd_configure,
            'validate': self.cmd_validate,
            'execute': self.cmd_execute,
            'report': self.cmd_report,
            'troubleshoot': self.cmd_troubleshoot,
        }
        
        handler = command_handlers.get(parsed_args.command)
        if handler:
            handler(parsed_args)
            return 0
        else:
            print(f"Unknown command: {parsed_args.command}")
            parser.print_help()
            return 1


def main() -> int:
    """
    Main entry point for the CLI application.
    
    Returns:
        Exit code (0 for success, non-zero for failure)
    """
    cli = MySQLPrivilegeCLI()
    return cli.run()


if __name__ == "__main__":
    sys.exit(main())