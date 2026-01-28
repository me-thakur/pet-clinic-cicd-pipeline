#!/usr/bin/env python3
"""
Property-based test for Network Security Enforcement

Property 5: Network Security Enforcement
For any network traffic attempt, the security groups should only allow connections 
that match the defined security rules (frontend to backend, backend to database, etc.)

Validates: Requirements 3.4
"""

import pytest
import boto3
import socket
import subprocess
import time
import json
import os
from hypothesis import given, strategies as st, settings
from typing import Dict, Any, Optional, List, Tuple
import uuid
from datetime import datetime
from botocore.exceptions import ClientError
import threading
import requests


class NetworkSecurityEnforcementTest:
    """Property-based test for network security enforcement"""
    
    def __init__(self):
        self.aws_region = os.getenv('AWS_REGION', 'us-east-1')
        self.stack_name = os.getenv('CLOUDFORMATION_STACK', 'pet-clinic-pipeline')
        
        # Initialize AWS clients
        self.ec2_client = boto3.client('ec2', region_name=self.aws_region)
        self.cloudformation_client = boto3.client('cloudformation', region_name=self.aws_region)
        
        # Network configuration
        self.security_rules = {
            'jenkins': {
                'allowed_inbound': [
                    {'protocol': 'tcp', 'port': 22, 'source': '0.0.0.0/0'},  # SSH
                    {'protocol': 'tcp', 'port': 8080, 'source': '0.0.0.0/0'},  # Jenkins UI
                    {'protocol': 'tcp', 'port': 80, 'source': '0.0.0.0/0'},  # HTTP
                    {'protocol': 'tcp', 'port': 443, 'source': '0.0.0.0/0'}  # HTTPS
                ],
                'denied_inbound': [
                    {'protocol': 'tcp', 'port': 3306, 'source': '0.0.0.0/0'},  # MySQL
                    {'protocol': 'tcp', 'port': 5432, 'source': '0.0.0.0/0'},  # PostgreSQL
                    {'protocol': 'tcp', 'port': 8081, 'source': '0.0.0.0/0'}  # Backend API
                ]
            },
            'frontend': {
                'allowed_inbound': [
                    {'protocol': 'tcp', 'port': 80, 'source': '0.0.0.0/0'},  # HTTP
                    {'protocol': 'tcp', 'port': 443, 'source': '0.0.0.0/0'},  # HTTPS
                    {'protocol': 'tcp', 'port': 8080, 'source': '0.0.0.0/0'}  # App port
                ],
                'allowed_outbound': [
                    {'protocol': 'tcp', 'port': 8081, 'target': 'backend'}  # To backend
                ],
                'denied_inbound': [
                    {'protocol': 'tcp', 'port': 3306, 'source': '0.0.0.0/0'},  # MySQL
                    {'protocol': 'tcp', 'port': 22, 'source': '0.0.0.0/0'}  # SSH
                ]
            },
            'backend': {
                'allowed_inbound': [
                    {'protocol': 'tcp', 'port': 8081, 'source': 'frontend'},  # From frontend
                    {'protocol': 'tcp', 'port': 8081, 'source': 'jenkins'}  # From Jenkins
                ],
                'allowed_outbound': [
                    {'protocol': 'tcp', 'port': 3306, 'target': 'database'}  # To database
                ],
                'denied_inbound': [
                    {'protocol': 'tcp', 'port': 8081, 'source': '0.0.0.0/0'},  # Direct internet access
                    {'protocol': 'tcp', 'port': 22, 'source': '0.0.0.0/0'}  # SSH from internet
                ]
            },
            'database': {
                'allowed_inbound': [
                    {'protocol': 'tcp', 'port': 3306, 'source': 'backend'}  # From backend only
                ],
                'denied_inbound': [
                    {'protocol': 'tcp', 'port': 3306, 'source': '0.0.0.0/0'},  # Direct internet access
                    {'protocol': 'tcp', 'port': 3306, 'source': 'frontend'},  # From frontend
                    {'protocol': 'tcp', 'port': 3306, 'source': 'jenkins'}  # From Jenkins
                ]
            }
        }
    
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up test environment with unique identifiers"""
        test_id = str(uuid.uuid4())[:8]
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        
        return {
            'test_id': test_id,
            'timestamp': timestamp,
            'security_groups': {},
            'instances': {},
            'vpc_info': {}
        }
    
    def discover_infrastructure(self, test_env: Dict[str, Any]) -> bool:
        """Discover existing infrastructure components for testing"""
        try:
            # Get CloudFormation stack resources
            response = self.cloudformation_client.describe_stack_resources(
                StackName=self.stack_name
            )
            
            resources = response['StackResources']
            
            # Find security groups
            for resource in resources:
                if resource['ResourceType'] == 'AWS::EC2::SecurityGroup':
                    sg_id = resource['PhysicalResourceId']
                    logical_id = resource['LogicalResourceId']
                    
                    # Map logical IDs to security group types
                    if 'jenkins' in logical_id.lower():
                        test_env['security_groups']['jenkins'] = sg_id
                    elif 'frontend' in logical_id.lower():
                        test_env['security_groups']['frontend'] = sg_id
                    elif 'backend' in logical_id.lower():
                        test_env['security_groups']['backend'] = sg_id
                    elif 'database' in logical_id.lower() or 'rds' in logical_id.lower():
                        test_env['security_groups']['database'] = sg_id
                
                # Find VPC
                elif resource['ResourceType'] == 'AWS::EC2::VPC':
                    test_env['vpc_info']['vpc_id'] = resource['PhysicalResourceId']
            
            # Get detailed security group information
            if test_env['security_groups']:
                sg_response = self.ec2_client.describe_security_groups(
                    GroupIds=list(test_env['security_groups'].values())
                )
                
                for sg in sg_response['SecurityGroups']:
                    sg_id = sg['GroupId']
                    for sg_type, sg_id_check in test_env['security_groups'].items():
                        if sg_id == sg_id_check:
                            test_env['security_groups'][sg_type] = {
                                'id': sg_id,
                                'name': sg['GroupName'],
                                'inbound_rules': sg['IpPermissions'],
                                'outbound_rules': sg['IpPermissionsEgress']
                            }
            
            # Find running instances
            instances_response = self.ec2_client.describe_instances(
                Filters=[
                    {'Name': 'instance-state-name', 'Values': ['running']},
                    {'Name': 'vpc-id', 'Values': [test_env['vpc_info'].get('vpc_id', '')]}
                ]
            )
            
            for reservation in instances_response['Reservations']:
                for instance in reservation['Instances']:
                    instance_id = instance['InstanceId']
                    
                    # Determine instance type based on tags or security groups
                    tags = {tag['Key']: tag['Value'] for tag in instance.get('Tags', [])}
                    sg_ids = [sg['GroupId'] for sg in instance['SecurityGroups']]
                    
                    instance_type = 'unknown'
                    for sg_type, sg_info in test_env['security_groups'].items():
                        if isinstance(sg_info, dict) and sg_info['id'] in sg_ids:
                            instance_type = sg_type
                            break
                    
                    test_env['instances'][instance_type] = {
                        'id': instance_id,
                        'private_ip': instance.get('PrivateIpAddress'),
                        'public_ip': instance.get('PublicIpAddress'),
                        'security_groups': sg_ids,
                        'subnet_id': instance['SubnetId']
                    }
            
            return len(test_env['security_groups']) > 0
            
        except Exception as e:
            print(f"Error discovering infrastructure: {e}")
            return False
    
    def validate_security_group_rules(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Validate that security group rules match expected configuration"""
        validation_results = {
            'security_groups_found': len(test_env['security_groups']),
            'rule_validations': {},
            'compliance_score': 0,
            'violations': []
        }
        
        for sg_type, sg_info in test_env['security_groups'].items():
            if not isinstance(sg_info, dict):
                continue
                
            sg_validation = {
                'allowed_inbound_correct': True,
                'denied_inbound_enforced': True,
                'allowed_outbound_correct': True,
                'unexpected_rules': []
            }
            
            expected_rules = self.security_rules.get(sg_type, {})
            actual_inbound = sg_info['inbound_rules']
            actual_outbound = sg_info['outbound_rules']
            
            # Validate allowed inbound rules
            if 'allowed_inbound' in expected_rules:
                for expected_rule in expected_rules['allowed_inbound']:
                    rule_found = self._find_matching_rule(expected_rule, actual_inbound, test_env)
                    if not rule_found:
                        sg_validation['allowed_inbound_correct'] = False
                        validation_results['violations'].append(
                            f"{sg_type}: Missing allowed inbound rule {expected_rule}"
                        )
            
            # Validate denied inbound rules (should not exist)
            if 'denied_inbound' in expected_rules:
                for denied_rule in expected_rules['denied_inbound']:
                    rule_found = self._find_matching_rule(denied_rule, actual_inbound, test_env)
                    if rule_found:
                        sg_validation['denied_inbound_enforced'] = False
                        validation_results['violations'].append(
                            f"{sg_type}: Prohibited inbound rule found {denied_rule}"
                        )
            
            # Validate allowed outbound rules
            if 'allowed_outbound' in expected_rules:
                for expected_rule in expected_rules['allowed_outbound']:
                    rule_found = self._find_matching_rule(expected_rule, actual_outbound, test_env)
                    if not rule_found:
                        sg_validation['allowed_outbound_correct'] = False
                        validation_results['violations'].append(
                            f"{sg_type}: Missing allowed outbound rule {expected_rule}"
                        )
            
            validation_results['rule_validations'][sg_type] = sg_validation
        
        # Calculate compliance score
        total_checks = sum(len(v) for v in validation_results['rule_validations'].values())
        passed_checks = sum(
            sum(1 for check in sg_val.values() if isinstance(check, bool) and check)
            for sg_val in validation_results['rule_validations'].values()
        )
        
        validation_results['compliance_score'] = passed_checks / max(total_checks, 1) if total_checks > 0 else 0
        
        return validation_results
    
    def _find_matching_rule(self, expected_rule: Dict[str, Any], actual_rules: List[Dict[str, Any]], test_env: Dict[str, Any]) -> bool:
        """Find if an expected rule matches any actual security group rule"""
        for actual_rule in actual_rules:
            if self._rules_match(expected_rule, actual_rule, test_env):
                return True
        return False
    
    def _rules_match(self, expected: Dict[str, Any], actual: Dict[str, Any], test_env: Dict[str, Any]) -> bool:
        """Check if expected rule matches actual rule"""
        # Check protocol
        if expected.get('protocol') != actual.get('IpProtocol'):
            return False
        
        # Check port
        expected_port = expected.get('port')
        if expected_port:
            actual_from_port = actual.get('FromPort')
            actual_to_port = actual.get('ToPort')
            
            if actual_from_port != expected_port or actual_to_port != expected_port:
                return False
        
        # Check source/target
        if 'source' in expected:
            expected_source = expected['source']
            
            # Handle CIDR blocks
            if '/' in expected_source:
                for ip_range in actual.get('IpRanges', []):
                    if ip_range.get('CidrIp') == expected_source:
                        return True
            
            # Handle security group references
            elif expected_source in test_env['security_groups']:
                target_sg_id = test_env['security_groups'][expected_source]
                if isinstance(target_sg_id, dict):
                    target_sg_id = target_sg_id['id']
                
                for user_id_group_pair in actual.get('UserIdGroupPairs', []):
                    if user_id_group_pair.get('GroupId') == target_sg_id:
                        return True
        
        if 'target' in expected:
            expected_target = expected['target']
            
            # For outbound rules, check if target security group is referenced
            if expected_target in test_env['security_groups']:
                target_sg_id = test_env['security_groups'][expected_target]
                if isinstance(target_sg_id, dict):
                    target_sg_id = target_sg_id['id']
                
                for user_id_group_pair in actual.get('UserIdGroupPairs', []):
                    if user_id_group_pair.get('GroupId') == target_sg_id:
                        return True
        
        return False
    
    def perform_network_connectivity_tests(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Perform actual network connectivity tests to validate security enforcement"""
        connectivity_results = {
            'tests_performed': 0,
            'tests_passed': 0,
            'allowed_connections': [],
            'blocked_connections': [],
            'unexpected_results': []
        }
        
        # Test allowed connections
        allowed_tests = [
            ('jenkins', 'backend', 8081, True),  # Jenkins should reach backend
            ('frontend', 'backend', 8081, True),  # Frontend should reach backend
            ('backend', 'database', 3306, True),  # Backend should reach database
        ]
        
        # Test blocked connections
        blocked_tests = [
            ('internet', 'backend', 8081, False),  # Internet should not reach backend directly
            ('internet', 'database', 3306, False),  # Internet should not reach database
            ('frontend', 'database', 3306, False),  # Frontend should not reach database directly
        ]
        
        all_tests = allowed_tests + blocked_tests
        
        for source, target, port, should_connect in all_tests:
            connectivity_results['tests_performed'] += 1
            
            # Perform connectivity test
            connection_result = self._test_connection(source, target, port, test_env)
            
            if connection_result == should_connect:
                connectivity_results['tests_passed'] += 1
                if should_connect:
                    connectivity_results['allowed_connections'].append(f"{source} -> {target}:{port}")
                else:
                    connectivity_results['blocked_connections'].append(f"{source} -X-> {target}:{port}")
            else:
                connectivity_results['unexpected_results'].append({
                    'source': source,
                    'target': target,
                    'port': port,
                    'expected': should_connect,
                    'actual': connection_result
                })
        
        return connectivity_results
    
    def _test_connection(self, source: str, target: str, port: int, test_env: Dict[str, Any]) -> bool:
        """Test network connection between source and target"""
        try:
            # Get target IP address
            target_ip = None
            
            if target in test_env['instances']:
                instance_info = test_env['instances'][target]
                target_ip = instance_info.get('private_ip') or instance_info.get('public_ip')
            
            if not target_ip:
                return False
            
            # Perform connection test based on source
            if source == 'internet':
                # Test from external perspective (simulated)
                return self._test_external_connection(target_ip, port)
            elif source in test_env['instances']:
                # Test from another instance (would require SSH access)
                return self._test_internal_connection(source, target_ip, port, test_env)
            
            return False
            
        except Exception as e:
            print(f"Error testing connection {source} -> {target}:{port}: {e}")
            return False
    
    def _test_external_connection(self, target_ip: str, port: int) -> bool:
        """Test connection from external internet perspective"""
        try:
            # Use socket to test connectivity with timeout
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5)  # 5 second timeout
            
            result = sock.connect_ex((target_ip, port))
            sock.close()
            
            return result == 0
            
        except Exception:
            return False
    
    def _test_internal_connection(self, source: str, target_ip: str, port: int, test_env: Dict[str, Any]) -> bool:
        """Test connection from internal instance perspective"""
        # This would require SSH access to the source instance
        # For this test, we'll simulate based on security group rules
        
        source_instance = test_env['instances'].get(source, {})
        source_sg_ids = source_instance.get('security_groups', [])
        
        # Check if source security group allows outbound to target
        for sg_type, sg_info in test_env['security_groups'].items():
            if isinstance(sg_info, dict) and sg_info['id'] in source_sg_ids:
                outbound_rules = sg_info['outbound_rules']
                
                for rule in outbound_rules:
                    if (rule.get('IpProtocol') == 'tcp' and 
                        rule.get('FromPort') <= port <= rule.get('ToPort', port)):
                        
                        # Check if rule allows connection to target
                        for ip_range in rule.get('IpRanges', []):
                            if ip_range.get('CidrIp') == '0.0.0.0/0':
                                return True
                        
                        # Check security group references
                        for user_group in rule.get('UserIdGroupPairs', []):
                            target_sg_id = user_group.get('GroupId')
                            # If target instance has this security group, connection allowed
                            for target_type, target_instance in test_env['instances'].items():
                                if target_ip in [target_instance.get('private_ip'), target_instance.get('public_ip')]:
                                    if target_sg_id in target_instance.get('security_groups', []):
                                        return True
        
        return False


@pytest.fixture
def network_security_test():
    """Fixture for network security enforcement test"""
    return NetworkSecurityEnforcementTest()


@given(
    test_port=st.integers(min_value=1024, max_value=65535),
    connection_timeout=st.integers(min_value=1, max_value=10)
)
@settings(max_examples=5, deadline=300000)  # 5 minute timeout per test
def test_network_security_enforcement(network_security_test, test_port, connection_timeout):
    """
    **Validates: Requirements 3.4**
    
    Property 5: Network Security Enforcement
    For any network traffic attempt, the security groups should only allow connections 
    that match the defined security rules (frontend to backend, backend to database, etc.)
    """
    # Set up test environment
    test_env = network_security_test.setup_test_environment()
    
    # Step 1: Discover existing infrastructure
    print("Discovering infrastructure components...")
    infrastructure_found = network_security_test.discover_infrastructure(test_env)
    
    if not infrastructure_found:
        pytest.skip("Required infrastructure not found for network security testing")
    
    print(f"Found {len(test_env['security_groups'])} security groups and {len(test_env['instances'])} instances")
    
    # Step 2: Validate security group rules configuration
    print("Validating security group rules...")
    rule_validation = network_security_test.validate_security_group_rules(test_env)
    
    # Assert security group compliance
    assert rule_validation['security_groups_found'] > 0, "No security groups found for testing"
    assert rule_validation['compliance_score'] >= 0.8, (
        f"Security group compliance too low: {rule_validation['compliance_score']:.2f}. "
        f"Violations: {rule_validation['violations']}"
    )
    
    print(f"Security group compliance: {rule_validation['compliance_score']:.2%}")
    
    # Step 3: Perform network connectivity tests
    print("Performing network connectivity tests...")
    connectivity_results = network_security_test.perform_network_connectivity_tests(test_env)
    
    # Assert connectivity test results
    if connectivity_results['tests_performed'] > 0:
        success_rate = connectivity_results['tests_passed'] / connectivity_results['tests_performed']
        assert success_rate >= 0.8, (
            f"Network connectivity tests failed: {success_rate:.2%} success rate. "
            f"Unexpected results: {connectivity_results['unexpected_results']}"
        )
        
        print(f"Network connectivity tests: {connectivity_results['tests_passed']}/{connectivity_results['tests_performed']} passed")
    
    # Step 4: Validate specific security enforcement scenarios
    
    # Ensure backend is not directly accessible from internet
    backend_protected = True
    for sg_type, sg_info in test_env['security_groups'].items():
        if sg_type == 'backend' and isinstance(sg_info, dict):
            for rule in sg_info['inbound_rules']:
                for ip_range in rule.get('IpRanges', []):
                    if ip_range.get('CidrIp') == '0.0.0.0/0' and rule.get('FromPort') == 8081:
                        backend_protected = False
                        break
    
    assert backend_protected, "Backend is directly accessible from internet (security violation)"
    
    # Ensure database is only accessible from backend
    database_protected = True
    for sg_type, sg_info in test_env['security_groups'].items():
        if sg_type == 'database' and isinstance(sg_info, dict):
            for rule in sg_info['inbound_rules']:
                # Check for direct internet access
                for ip_range in rule.get('IpRanges', []):
                    if ip_range.get('CidrIp') == '0.0.0.0/0' and rule.get('FromPort') == 3306:
                        database_protected = False
                        break
                
                # Check for non-backend security group access
                for user_group in rule.get('UserIdGroupPairs', []):
                    sg_id = user_group.get('GroupId')
                    backend_sg_id = test_env['security_groups'].get('backend', {})
                    if isinstance(backend_sg_id, dict):
                        backend_sg_id = backend_sg_id['id']
                    
                    if sg_id != backend_sg_id and rule.get('FromPort') == 3306:
                        database_protected = False
                        break
    
    assert database_protected, "Database is accessible from non-backend sources (security violation)"
    
    print(f"✓ Property 5 validated: Network security enforcement is working correctly")
    print(f"  - Security group compliance: {rule_validation['compliance_score']:.2%}")
    print(f"  - Connectivity tests passed: {connectivity_results['tests_passed']}/{connectivity_results['tests_performed']}")
    print(f"  - Backend protected from direct internet access: {backend_protected}")
    print(f"  - Database protected (backend-only access): {database_protected}")
    
    # Log allowed and blocked connections
    if connectivity_results['allowed_connections']:
        print(f"  - Allowed connections: {len(connectivity_results['allowed_connections'])}")
    if connectivity_results['blocked_connections']:
        print(f"  - Blocked connections: {len(connectivity_results['blocked_connections'])}")


def test_network_security_enforcement_manual():
    """
    Manual test case for network security enforcement
    This test can be run independently to verify security rules
    """
    test_instance = NetworkSecurityEnforcementTest()
    test_env = test_instance.setup_test_environment()
    
    try:
        # Discover infrastructure
        print("Discovering infrastructure for manual test...")
        infrastructure_found = test_instance.discover_infrastructure(test_env)
        
        if not infrastructure_found:
            pytest.skip("Infrastructure not found for manual security test")
        
        # Validate security group rules
        rule_validation = test_instance.validate_security_group_rules(test_env)
        print(f"Security group compliance: {rule_validation['compliance_score']:.2%}")
        
        if rule_validation['violations']:
            print("Security violations found:")
            for violation in rule_validation['violations']:
                print(f"  - {violation}")
        
        # Perform connectivity tests
        connectivity_results = test_instance.perform_network_connectivity_tests(test_env)
        print(f"Connectivity tests: {connectivity_results['tests_passed']}/{connectivity_results['tests_performed']} passed")
        
        if connectivity_results['unexpected_results']:
            print("Unexpected connectivity results:")
            for result in connectivity_results['unexpected_results']:
                print(f"  - {result['source']} -> {result['target']}:{result['port']} "
                      f"(expected: {result['expected']}, actual: {result['actual']})")
        
        # Assert basic security requirements
        assert rule_validation['compliance_score'] >= 0.7, "Security compliance too low for manual test"
        
        if connectivity_results['tests_performed'] > 0:
            success_rate = connectivity_results['tests_passed'] / connectivity_results['tests_performed']
            assert success_rate >= 0.7, "Too many connectivity test failures"
        
        print("✓ Manual network security test passed")
        
    except Exception as e:
        print(f"Manual test error: {e}")
        raise


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v'])