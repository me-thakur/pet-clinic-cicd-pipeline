#!/usr/bin/env python3
"""
MySQL Configuration Validation Script

This script validates MySQL configuration for privilege management across different environments.
It checks the key settings that resolve ERROR 1419 and verifies environment-appropriate security.

Requirements: 1.4, 2.1, 2.2, 2.3, 5.1, 5.2
"""

import sys
import os
import argparse
import mysql.connector
from mysql.connector import Error
from typing import Dict, List, Tuple, Optional
import json
from datetime import datetime

# Add the parent directory to the path to import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core.models import Environment, SecurityLevel
from detectors.environment_detector import EnvironmentDetector
from validators.privilege_validator import PrivilegeValidator


class ConfigurationValidator:
    """Validates MySQL configuration for different environments."""
    
    def __init__(self, host: str = 'localhost', port: int = 3306, 
                 user: str = 'root', password: str = ''):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.connection = None
        self.environment_detector = EnvironmentDetector()
        
    def connect(self) -> bool:
        """Establish connection to MySQL server."""
        try:
            self.connection = mysql.connector.connect(
                host=self.host,
                port=self.port,
                user=self.user,
                password=self.password,
                autocommit=True
            )
            return True
        except Error as e:
            print(f"❌ Failed to connect to MySQL: {e}")
            return False
    
    def disconnect(self):
        """Close MySQL connection."""
        if self.connection and self.connection.is_connected():
            self.connection.close()
    
    def get_system_variable(self, variable_name: str) -> Optional[str]:
        """Get MySQL system variable value."""
        try:
            cursor = self.connection.cursor()
            cursor.execute(f"SELECT @@{variable_name}")
            result = cursor.fetchone()
            cursor.close()
            return str(result[0]) if result else None
        except Error as e:
            print(f"⚠️  Warning: Could not get {variable_name}: {e}")
            return None
    
    def check_privilege(self, privilege: str) -> bool:
        """Check if current user has specific privilege."""
        try:
            cursor = self.connection.cursor()
            cursor.execute("SHOW GRANTS FOR CURRENT_USER()")
            grants = cursor.fetchall()
            cursor.close()
            
            grant_text = ' '.join([grant[0] for grant in grants]).upper()
            return privilege.upper() in grant_text or 'ALL PRIVILEGES' in grant_text
        except Error as e:
            print(f"⚠️  Warning: Could not check {privilege} privilege: {e}")
            return False
    
    def test_function_creation(self) -> bool:
        """Test if functions can be created without SUPER privileges."""
        try:
            cursor = self.connection.cursor()
            
            # Create a test database if it doesn't exist
            cursor.execute("CREATE DATABASE IF NOT EXISTS config_test")
            cursor.execute("USE config_test")
            
            # Try to create a simple deterministic function
            test_function = """
            CREATE FUNCTION IF NOT EXISTS test_privilege_function(x INT) 
            RETURNS INT 
            DETERMINISTIC 
            NO SQL
            COMMENT 'Test function for privilege validation'
            BEGIN 
                RETURN x * 2; 
            END
            """
            
            cursor.execute("DROP FUNCTION IF EXISTS test_privilege_function")
            cursor.execute(test_function)
            
            # Test the function
            cursor.execute("SELECT test_privilege_function(5)")
            result = cursor.fetchone()
            
            # Clean up
            cursor.execute("DROP FUNCTION test_privilege_function")
            cursor.execute("DROP DATABASE config_test")
            cursor.close()
            
            return result and result[0] == 10
            
        except Error as e:
            if "1419" in str(e):  # ERROR 1419: SUPER privilege required
                return False
            print(f"⚠️  Warning: Function creation test failed: {e}")
            return False
    
    def validate_environment_config(self, environment: Environment) -> Dict[str, any]:
        """Validate configuration for specific environment."""
        results = {
            'environment': environment.value,
            'timestamp': datetime.now().isoformat(),
            'checks': {},
            'recommendations': [],
            'security_level': 'unknown'
        }
        
        # Core privilege settings
        log_bin_trust = self.get_system_variable('log_bin_trust_function_creators')
        binary_logging = self.get_system_variable('log_bin')
        binlog_format = self.get_system_variable('binlog_format')
        
        results['checks']['log_bin_trust_function_creators'] = {
            'value': log_bin_trust,
            'status': 'unknown'
        }
        
        results['checks']['binary_logging'] = {
            'value': binary_logging,
            'status': 'unknown'
        }
        
        results['checks']['binlog_format'] = {
            'value': binlog_format,
            'status': 'unknown'
        }
        
        # Check privileges
        has_super = self.check_privilege('SUPER')
        has_create_routine = self.check_privilege('CREATE ROUTINE')
        
        results['checks']['super_privilege'] = {
            'value': has_super,
            'status': 'info'
        }
        
        results['checks']['create_routine_privilege'] = {
            'value': has_create_routine,
            'status': 'info'
        }
        
        # Test function creation
        can_create_functions = self.test_function_creation()
        results['checks']['function_creation'] = {
            'value': can_create_functions,
            'status': 'unknown'
        }
        
        # Environment-specific validation
        if environment == Environment.LOCAL_DEVELOPMENT:
            results.update(self._validate_development_config(results['checks']))
        elif environment == Environment.CI_CD:
            results.update(self._validate_ci_config(results['checks']))
        elif environment == Environment.PRODUCTION:
            results.update(self._validate_production_config(results['checks']))
        
        return results
    
    def _validate_development_config(self, checks: Dict) -> Dict[str, any]:
        """Validate local development configuration."""
        recommendations = []
        security_level = SecurityLevel.PERMISSIVE.value
        
        # Development should allow function creation
        if checks['log_bin_trust_function_creators']['value'] == '1':
            checks['log_bin_trust_function_creators']['status'] = 'pass'
        else:
            checks['log_bin_trust_function_creators']['status'] = 'fail'
            recommendations.append(
                "Set log_bin_trust_function_creators = 1 for development ease"
            )
        
        # Function creation should work
        if checks['function_creation']['value']:
            checks['function_creation']['status'] = 'pass'
        else:
            checks['function_creation']['status'] = 'fail'
            recommendations.append(
                "Function creation failed - check log_bin_trust_function_creators setting"
            )
        
        # Check additional development settings
        bind_address = self.get_system_variable('bind_address')
        if bind_address == '127.0.0.1':
            checks['network_security'] = {'value': bind_address, 'status': 'pass'}
        else:
            checks['network_security'] = {'value': bind_address, 'status': 'warning'}
            recommendations.append(
                "Consider binding to 127.0.0.1 for development security"
            )
        
        return {
            'security_level': security_level,
            'recommendations': recommendations
        }
    
    def _validate_ci_config(self, checks: Dict) -> Dict[str, any]:
        """Validate CI/CD configuration."""
        recommendations = []
        security_level = SecurityLevel.BALANCED.value
        
        # CI should allow function creation for testing
        if checks['log_bin_trust_function_creators']['value'] == '1':
            checks['log_bin_trust_function_creators']['status'] = 'pass'
        else:
            checks['log_bin_trust_function_creators']['status'] = 'fail'
            recommendations.append(
                "Set log_bin_trust_function_creators = 1 for CI/CD testing"
            )
        
        # Function creation should work in CI
        if checks['function_creation']['value']:
            checks['function_creation']['status'] = 'pass'
        else:
            checks['function_creation']['status'] = 'fail'
            recommendations.append(
                "Function creation failed - CI tests may fail"
            )
        
        # Binary logging should be enabled for production consistency
        if checks['binary_logging']['value'] == 'ON':
            checks['binary_logging']['status'] = 'pass'
        else:
            checks['binary_logging']['status'] = 'warning'
            recommendations.append(
                "Enable binary logging to match production environment"
            )
        
        # Check CI-specific settings
        max_connections = self.get_system_variable('max_connections')
        if max_connections and int(max_connections) >= 300:
            checks['connection_limit'] = {'value': max_connections, 'status': 'pass'}
        else:
            checks['connection_limit'] = {'value': max_connections, 'status': 'warning'}
            recommendations.append(
                "Consider increasing max_connections for CI workloads"
            )
        
        return {
            'security_level': security_level,
            'recommendations': recommendations
        }
    
    def _validate_production_config(self, checks: Dict) -> Dict[str, any]:
        """Validate production configuration."""
        recommendations = []
        security_level = SecurityLevel.RESTRICTIVE.value
        
        # Production should be restrictive by default
        if checks['log_bin_trust_function_creators']['value'] == '0':
            checks['log_bin_trust_function_creators']['status'] = 'pass'
            if not checks['super_privilege']['value']:
                recommendations.append(
                    "SUPER privilege required for function creation with current settings"
                )
        elif checks['log_bin_trust_function_creators']['value'] == '1':
            checks['log_bin_trust_function_creators']['status'] = 'warning'
            recommendations.append(
                "log_bin_trust_function_creators = 1 reduces security - ensure proper monitoring"
            )
        else:
            checks['log_bin_trust_function_creators']['status'] = 'fail'
            recommendations.append(
                "Configure log_bin_trust_function_creators appropriately for production"
            )
        
        # Binary logging must be enabled in production
        if checks['binary_logging']['value'] == 'ON':
            checks['binary_logging']['status'] = 'pass'
        else:
            checks['binary_logging']['status'] = 'fail'
            recommendations.append(
                "Enable binary logging for production replication and recovery"
            )
        
        # Check production security settings
        ssl_status = self.get_system_variable('have_ssl')
        if ssl_status == 'YES':
            checks['ssl_support'] = {'value': ssl_status, 'status': 'pass'}
        else:
            checks['ssl_support'] = {'value': ssl_status, 'status': 'warning'}
            recommendations.append(
                "Enable SSL/TLS support for production security"
            )
        
        # Check if secure transport is required
        secure_transport = self.get_system_variable('require_secure_transport')
        if secure_transport == 'ON':
            checks['secure_transport'] = {'value': secure_transport, 'status': 'pass'}
        else:
            checks['secure_transport'] = {'value': secure_transport, 'status': 'warning'}
            recommendations.append(
                "Consider requiring secure transport for production"
            )
        
        return {
            'security_level': security_level,
            'recommendations': recommendations
        }
    
    def print_results(self, results: Dict[str, any]):
        """Print validation results in a readable format."""
        print(f"\n🔍 MySQL Configuration Validation Report")
        print(f"Environment: {results['environment'].upper()}")
        print(f"Security Level: {results['security_level'].upper()}")
        print(f"Timestamp: {results['timestamp']}")
        print("=" * 60)
        
        # Print check results
        for check_name, check_data in results['checks'].items():
            status_icon = {
                'pass': '✅',
                'fail': '❌',
                'warning': '⚠️',
                'info': 'ℹ️',
                'unknown': '❓'
            }.get(check_data['status'], '❓')
            
            print(f"{status_icon} {check_name.replace('_', ' ').title()}: {check_data['value']}")
        
        # Print recommendations
        if results['recommendations']:
            print(f"\n📋 Recommendations:")
            for i, rec in enumerate(results['recommendations'], 1):
                print(f"   {i}. {rec}")
        else:
            print(f"\n✅ No recommendations - configuration looks good!")
        
        print("=" * 60)


def main():
    """Main function to run configuration validation."""
    parser = argparse.ArgumentParser(
        description='Validate MySQL configuration for privilege management'
    )
    parser.add_argument('--host', default='localhost', help='MySQL host')
    parser.add_argument('--port', type=int, default=3306, help='MySQL port')
    parser.add_argument('--user', default='root', help='MySQL user')
    parser.add_argument('--password', default='', help='MySQL password')
    parser.add_argument('--environment', 
                       choices=['local', 'ci', 'production'],
                       help='Target environment (auto-detected if not specified)')
    parser.add_argument('--output', choices=['text', 'json'], default='text',
                       help='Output format')
    
    args = parser.parse_args()
    
    # Create validator
    validator = ConfigurationValidator(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password
    )
    
    # Connect to MySQL
    if not validator.connect():
        sys.exit(1)
    
    try:
        # Determine environment
        if args.environment:
            env_map = {
                'local': Environment.LOCAL_DEVELOPMENT,
                'ci': Environment.CI_CD,
                'production': Environment.PRODUCTION
            }
            environment = env_map[args.environment]
        else:
            # Auto-detect environment
            environment = validator.environment_detector.detect_environment()
            print(f"🔍 Auto-detected environment: {environment.value}")
        
        # Validate configuration
        results = validator.validate_environment_config(environment)
        
        # Output results
        if args.output == 'json':
            print(json.dumps(results, indent=2))
        else:
            validator.print_results(results)
        
        # Exit with appropriate code
        failed_checks = [
            check for check in results['checks'].values() 
            if check['status'] == 'fail'
        ]
        
        if failed_checks:
            print(f"\n❌ Validation failed with {len(failed_checks)} critical issues")
            sys.exit(1)
        else:
            print(f"\n✅ Configuration validation completed successfully")
            sys.exit(0)
    
    finally:
        validator.disconnect()


if __name__ == '__main__':
    main()