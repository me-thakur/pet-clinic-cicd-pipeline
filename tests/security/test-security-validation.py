#!/usr/bin/env python3
"""
Security Validation and Penetration Testing Suite

This comprehensive security test suite validates authentication, authorization, 
network security, encryption, and secret management across the Pet Clinic CI/CD Pipeline.

Validates: Requirements 10.1, 10.2, 10.3, 10.4
"""

import pytest
import requests
import socket
import ssl
import subprocess
import json
import time
import os
import boto3
import base64
from typing import Dict, Any, List, Optional, Tuple
from urllib.parse import urlparse
import uuid
from datetime import datetime
from botocore.exceptions import ClientError
import threading
import hashlib


class SecurityValidationTest:
    """Comprehensive security validation and penetration testing"""
    
    def __init__(self):
        # Application endpoints
        self.jenkins_url = os.getenv('JENKINS_URL', 'http://localhost:8080')
        self.frontend_url = os.getenv('FRONTEND_URL', 'http://localhost:8080')
        self.backend_url = os.getenv('BACKEND_URL', 'http://localhost:8081')
        
        # Credentials for testing
        self.jenkins_user = os.getenv('JENKINS_USER', 'admin')
        self.jenkins_token = os.getenv('JENKINS_TOKEN', '')
        self.test_username = os.getenv('TEST_USERNAME', 'testuser')
        self.test_password = os.getenv('TEST_PASSWORD', 'testpass')
        
        # AWS configuration
        self.aws_region = os.getenv('AWS_REGION', 'us-east-1')
        self.stack_name = os.getenv('CLOUDFORMATION_STACK', 'pet-clinic-pipeline')
        
        # Initialize AWS clients
        self.ec2_client = boto3.client('ec2', region_name=self.aws_region)
        self.rds_client = boto3.client('rds', region_name=self.aws_region)
        self.s3_client = boto3.client('s3', region_name=self.aws_region)
        self.secrets_client = boto3.client('secretsmanager', region_name=self.aws_region)
        
        # Security test configuration
        self.common_ports = [21, 22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 3306, 5432, 8080, 8081]
        self.sensitive_endpoints = [
            '/admin', '/management', '/actuator', '/health', '/metrics',
            '/env', '/configprops', '/dump', '/trace', '/jolokia'
        ]
        self.sql_injection_payloads = [
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' UNION SELECT * FROM information_schema.tables --",
            "admin'--",
            "' OR 1=1#"
        ]
        self.xss_payloads = [
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "';alert('XSS');//"
        ]
    
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up security test environment"""
        test_id = str(uuid.uuid4())[:8]
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        
        return {
            'test_id': test_id,
            'timestamp': timestamp,
            'discovered_endpoints': [],
            'security_findings': [],
            'vulnerability_count': 0
        }
    
    def test_authentication_mechanisms(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test authentication mechanisms across all components"""
        auth_results = {
            'jenkins_auth': {'tested': False, 'secure': False, 'findings': []},
            'frontend_auth': {'tested': False, 'secure': False, 'findings': []},
            'backend_auth': {'tested': False, 'secure': False, 'findings': []},
            'overall_score': 0
        }
        
        # Test Jenkins authentication
        print("Testing Jenkins authentication...")
        auth_results['jenkins_auth'] = self._test_jenkins_authentication()
        
        # Test Frontend authentication
        print("Testing Frontend authentication...")
        auth_results['frontend_auth'] = self._test_frontend_authentication()
        
        # Test Backend authentication
        print("Testing Backend authentication...")
        auth_results['backend_auth'] = self._test_backend_authentication()
        
        # Calculate overall security score
        components = [auth_results['jenkins_auth'], auth_results['frontend_auth'], auth_results['backend_auth']]
        tested_components = [c for c in components if c['tested']]
        secure_components = [c for c in tested_components if c['secure']]
        
        auth_results['overall_score'] = len(secure_components) / max(len(tested_components), 1)
        
        return auth_results
    
    def _test_jenkins_authentication(self) -> Dict[str, Any]:
        """Test Jenkins authentication security"""
        result = {'tested': False, 'secure': False, 'findings': []}
        
        try:
            # Test unauthenticated access
            response = requests.get(f'{self.jenkins_url}/api/json', timeout=10)
            result['tested'] = True
            
            if response.status_code == 403:
                result['secure'] = True
                result['findings'].append("✓ Jenkins requires authentication for API access")
            elif response.status_code == 200:
                result['findings'].append("⚠ Jenkins API accessible without authentication")
            
            # Test with invalid credentials
            invalid_response = requests.get(
                f'{self.jenkins_url}/api/json',
                auth=('invalid', 'credentials'),
                timeout=10
            )
            
            if invalid_response.status_code == 401:
                result['findings'].append("✓ Jenkins rejects invalid credentials")
            else:
                result['findings'].append("⚠ Jenkins authentication bypass possible")
                result['secure'] = False
            
            # Test with valid credentials if available
            if self.jenkins_token:
                valid_response = requests.get(
                    f'{self.jenkins_url}/api/json',
                    auth=(self.jenkins_user, self.jenkins_token),
                    timeout=10
                )
                
                if valid_response.status_code == 200:
                    result['findings'].append("✓ Jenkins accepts valid credentials")
                else:
                    result['findings'].append("⚠ Jenkins valid credentials not working")
            
            # Test for default credentials
            default_creds = [('admin', 'admin'), ('jenkins', 'jenkins'), ('admin', '')]
            for username, password in default_creds:
                default_response = requests.get(
                    f'{self.jenkins_url}/api/json',
                    auth=(username, password),
                    timeout=5
                )
                
                if default_response.status_code == 200:
                    result['findings'].append(f"🚨 Jenkins accessible with default credentials: {username}/{password}")
                    result['secure'] = False
        
        except Exception as e:
            result['findings'].append(f"Error testing Jenkins authentication: {e}")
        
        return result
    
    def _test_frontend_authentication(self) -> Dict[str, Any]:
        """Test Frontend authentication security"""
        result = {'tested': False, 'secure': False, 'findings': []}
        
        try:
            # Test login page accessibility
            login_response = requests.get(f'{self.frontend_url}/login', timeout=10)
            result['tested'] = True
            
            if login_response.status_code == 200:
                result['findings'].append("✓ Login page accessible")
                
                # Check for HTTPS redirect
                if login_response.url.startswith('https://'):
                    result['findings'].append("✓ Login redirects to HTTPS")
                    result['secure'] = True
                else:
                    result['findings'].append("⚠ Login page not using HTTPS")
            
            # Test protected resource access
            protected_response = requests.get(f'{self.frontend_url}/owners', timeout=10)
            
            if protected_response.status_code in [401, 403]:
                result['findings'].append("✓ Protected resources require authentication")
                result['secure'] = True
            elif protected_response.status_code == 302:
                if 'login' in protected_response.headers.get('Location', '').lower():
                    result['findings'].append("✓ Protected resources redirect to login")
                    result['secure'] = True
                else:
                    result['findings'].append("⚠ Unexpected redirect for protected resource")
            else:
                result['findings'].append("⚠ Protected resources accessible without authentication")
                result['secure'] = False
            
            # Test for session management
            session = requests.Session()
            session_response = session.get(f'{self.frontend_url}/', timeout=10)
            
            if 'Set-Cookie' in session_response.headers:
                cookies = session_response.headers['Set-Cookie']
                if 'HttpOnly' in cookies:
                    result['findings'].append("✓ Cookies have HttpOnly flag")
                else:
                    result['findings'].append("⚠ Cookies missing HttpOnly flag")
                
                if 'Secure' in cookies:
                    result['findings'].append("✓ Cookies have Secure flag")
                else:
                    result['findings'].append("⚠ Cookies missing Secure flag")
        
        except Exception as e:
            result['findings'].append(f"Error testing frontend authentication: {e}")
        
        return result
    
    def _test_backend_authentication(self) -> Dict[str, Any]:
        """Test Backend API authentication security"""
        result = {'tested': False, 'secure': False, 'findings': []}
        
        try:
            # Test API endpoints without authentication
            api_endpoints = ['/api/owners', '/api/pets', '/api/visits', '/api/vets']
            
            for endpoint in api_endpoints:
                response = requests.get(f'{self.backend_url}{endpoint}', timeout=10)
                result['tested'] = True
                
                if response.status_code in [401, 403]:
                    result['findings'].append(f"✓ {endpoint} requires authentication")
                    result['secure'] = True
                elif response.status_code == 200:
                    result['findings'].append(f"⚠ {endpoint} accessible without authentication")
                    result['secure'] = False
                    break
            
            # Test actuator endpoints
            actuator_endpoints = ['/actuator/health', '/actuator/info', '/actuator/metrics']
            
            for endpoint in actuator_endpoints:
                response = requests.get(f'{self.backend_url}{endpoint}', timeout=10)
                
                if response.status_code == 200:
                    # Health endpoint might be public, others should be protected
                    if endpoint == '/actuator/health':
                        result['findings'].append("ℹ Health endpoint is public (acceptable)")
                    else:
                        result['findings'].append(f"⚠ {endpoint} accessible without authentication")
                        result['secure'] = False
        
        except Exception as e:
            result['findings'].append(f"Error testing backend authentication: {e}")
        
        return result
    
    def test_network_security(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test network security and access controls"""
        network_results = {
            'port_scan_results': {},
            'ssl_tls_validation': {},
            'firewall_effectiveness': {},
            'security_score': 0,
            'vulnerabilities': []
        }
        
        print("Performing network security tests...")
        
        # Test port scanning
        network_results['port_scan_results'] = self._perform_port_scan()
        
        # Test SSL/TLS configuration
        network_results['ssl_tls_validation'] = self._test_ssl_tls_configuration()
        
        # Test firewall effectiveness
        network_results['firewall_effectiveness'] = self._test_firewall_rules()
        
        # Calculate security score
        scores = []
        if network_results['port_scan_results'].get('secure_ports', 0) > 0:
            total_ports = network_results['port_scan_results'].get('total_scanned', 1)
            secure_ports = network_results['port_scan_results'].get('secure_ports', 0)
            scores.append(secure_ports / total_ports)
        
        if network_results['ssl_tls_validation'].get('total_tested', 0) > 0:
            ssl_score = network_results['ssl_tls_validation'].get('secure_endpoints', 0) / network_results['ssl_tls_validation'].get('total_tested', 1)
            scores.append(ssl_score)
        
        network_results['security_score'] = sum(scores) / max(len(scores), 1)
        
        return network_results
    
    def _perform_port_scan(self) -> Dict[str, Any]:
        """Perform port scanning to identify open ports"""
        scan_results = {
            'total_scanned': 0,
            'open_ports': [],
            'closed_ports': [],
            'secure_ports': 0,
            'findings': []
        }
        
        # Get target IPs from infrastructure
        target_ips = self._get_target_ips()
        
        for target_ip in target_ips[:2]:  # Limit to 2 IPs for faster testing
            print(f"Scanning ports on {target_ip}...")
            
            for port in self.common_ports:
                scan_results['total_scanned'] += 1
                
                try:
                    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    sock.settimeout(2)
                    result = sock.connect_ex((target_ip, port))
                    sock.close()
                    
                    if result == 0:
                        scan_results['open_ports'].append(f"{target_ip}:{port}")
                        
                        # Check if this is an expected open port
                        if port in [80, 443, 8080, 8081, 22]:
                            scan_results['findings'].append(f"✓ Expected port {port} open on {target_ip}")
                            scan_results['secure_ports'] += 1
                        else:
                            scan_results['findings'].append(f"⚠ Unexpected port {port} open on {target_ip}")
                    else:
                        scan_results['closed_ports'].append(f"{target_ip}:{port}")
                        scan_results['secure_ports'] += 1
                
                except Exception as e:
                    scan_results['findings'].append(f"Error scanning {target_ip}:{port}: {e}")
        
        return scan_results
    
    def _get_target_ips(self) -> List[str]:
        """Get target IP addresses from infrastructure"""
        target_ips = []
        
        try:
            # Parse IPs from configured URLs
            for url in [self.jenkins_url, self.frontend_url, self.backend_url]:
                parsed = urlparse(url)
                if parsed.hostname and parsed.hostname not in ['localhost', '127.0.0.1']:
                    target_ips.append(parsed.hostname)
            
            # Get IPs from EC2 instances if available
            try:
                response = self.ec2_client.describe_instances(
                    Filters=[
                        {'Name': 'instance-state-name', 'Values': ['running']},
                        {'Name': 'tag:Project', 'Values': ['pet-clinic']}
                    ]
                )
                
                for reservation in response['Reservations']:
                    for instance in reservation['Instances']:
                        if instance.get('PublicIpAddress'):
                            target_ips.append(instance['PublicIpAddress'])
            except Exception:
                pass  # Ignore AWS errors
        
        except Exception as e:
            print(f"Error getting target IPs: {e}")
        
        # Remove duplicates and add localhost as fallback
        target_ips = list(set(target_ips))
        if not target_ips:
            target_ips = ['127.0.0.1']
        
        return target_ips
    
    def _test_ssl_tls_configuration(self) -> Dict[str, Any]:
        """Test SSL/TLS configuration security"""
        ssl_results = {
            'total_tested': 0,
            'secure_endpoints': 0,
            'findings': [],
            'certificate_details': {}
        }
        
        # Test HTTPS endpoints
        https_urls = []
        for url in [self.jenkins_url, self.frontend_url, self.backend_url]:
            if url.startswith('https://'):
                https_urls.append(url)
            else:
                # Try HTTPS version
                https_url = url.replace('http://', 'https://')
                https_urls.append(https_url)
        
        for url in https_urls:
            ssl_results['total_tested'] += 1
            
            try:
                parsed = urlparse(url)
                hostname = parsed.hostname
                port = parsed.port or 443
                
                # Test SSL connection
                context = ssl.create_default_context()
                
                with socket.create_connection((hostname, port), timeout=10) as sock:
                    with context.wrap_socket(sock, server_hostname=hostname) as ssock:
                        cert = ssock.getpeercert()
                        
                        ssl_results['secure_endpoints'] += 1
                        ssl_results['findings'].append(f"✓ SSL/TLS connection successful to {hostname}:{port}")
                        
                        # Store certificate details
                        ssl_results['certificate_details'][url] = {
                            'subject': dict(x[0] for x in cert['subject']),
                            'issuer': dict(x[0] for x in cert['issuer']),
                            'version': cert['version'],
                            'not_after': cert['notAfter']
                        }
                        
                        # Check certificate validity
                        subject = dict(x[0] for x in cert['subject'])
                        if subject.get('commonName') == hostname:
                            ssl_results['findings'].append(f"✓ Certificate CN matches hostname for {hostname}")
                        else:
                            ssl_results['findings'].append(f"⚠ Certificate CN mismatch for {hostname}")
            
            except ssl.SSLError as e:
                ssl_results['findings'].append(f"⚠ SSL error for {url}: {e}")
            except Exception as e:
                ssl_results['findings'].append(f"⚠ Connection error for {url}: {e}")
        
        return ssl_results
    
    def _test_firewall_rules(self) -> Dict[str, Any]:
        """Test firewall rules and security group effectiveness"""
        firewall_results = {
            'security_groups_tested': 0,
            'rules_validated': 0,
            'violations_found': 0,
            'findings': []
        }
        
        try:
            # Get security groups from CloudFormation stack
            response = self.ec2_client.describe_security_groups(
                Filters=[
                    {'Name': 'group-name', 'Values': ['*pet-clinic*', '*jenkins*', '*frontend*', '*backend*']}
                ]
            )
            
            for sg in response['SecurityGroups']:
                firewall_results['security_groups_tested'] += 1
                sg_name = sg['GroupName']
                
                # Check inbound rules
                for rule in sg['IpPermissions']:
                    firewall_results['rules_validated'] += 1
                    
                    # Check for overly permissive rules
                    for ip_range in rule.get('IpRanges', []):
                        if ip_range.get('CidrIp') == '0.0.0.0/0':
                            port_range = f"{rule.get('FromPort', 'N/A')}-{rule.get('ToPort', 'N/A')}"
                            
                            # Allow common web ports
                            if rule.get('FromPort') in [80, 443]:
                                firewall_results['findings'].append(f"✓ {sg_name}: Web ports {port_range} open to internet")
                            elif rule.get('FromPort') == 22:
                                firewall_results['findings'].append(f"⚠ {sg_name}: SSH port 22 open to internet")
                                firewall_results['violations_found'] += 1
                            elif rule.get('FromPort') in [3306, 5432]:
                                firewall_results['findings'].append(f"🚨 {sg_name}: Database port {port_range} open to internet")
                                firewall_results['violations_found'] += 1
                            else:
                                firewall_results['findings'].append(f"⚠ {sg_name}: Port {port_range} open to internet")
        
        except Exception as e:
            firewall_results['findings'].append(f"Error testing firewall rules: {e}")
        
        return firewall_results
    
    def test_injection_vulnerabilities(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test for SQL injection and XSS vulnerabilities"""
        injection_results = {
            'sql_injection_tests': 0,
            'sql_vulnerabilities': 0,
            'xss_tests': 0,
            'xss_vulnerabilities': 0,
            'findings': []
        }
        
        print("Testing for injection vulnerabilities...")
        
        # Test SQL injection
        injection_results.update(self._test_sql_injection())
        
        # Test XSS vulnerabilities
        injection_results.update(self._test_xss_vulnerabilities())
        
        return injection_results
    
    def _test_sql_injection(self) -> Dict[str, Any]:
        """Test for SQL injection vulnerabilities"""
        sql_results = {
            'sql_injection_tests': 0,
            'sql_vulnerabilities': 0,
            'findings': []
        }
        
        # Test endpoints that might be vulnerable
        test_endpoints = [
            f'{self.backend_url}/api/owners/search',
            f'{self.backend_url}/api/pets/search',
            f'{self.frontend_url}/owners/find'
        ]
        
        for endpoint in test_endpoints:
            for payload in self.sql_injection_payloads:
                sql_results['sql_injection_tests'] += 1
                
                try:
                    # Test GET parameter injection
                    response = requests.get(
                        endpoint,
                        params={'lastName': payload, 'name': payload},
                        timeout=10
                    )
                    
                    # Look for SQL error messages or unexpected behavior
                    if response.status_code == 500:
                        if any(error in response.text.lower() for error in ['sql', 'mysql', 'postgresql', 'database']):
                            sql_results['sql_vulnerabilities'] += 1
                            sql_results['findings'].append(f"🚨 Potential SQL injection in {endpoint} with payload: {payload}")
                    elif response.status_code == 200 and len(response.text) > 10000:
                        # Unusually large response might indicate successful injection
                        sql_results['findings'].append(f"⚠ Suspicious response size from {endpoint} with payload: {payload}")
                    
                    # Test POST data injection
                    if endpoint.endswith('/search'):
                        post_response = requests.post(
                            endpoint,
                            json={'query': payload, 'filter': payload},
                            timeout=10
                        )
                        
                        if post_response.status_code == 500:
                            if any(error in post_response.text.lower() for error in ['sql', 'mysql', 'postgresql']):
                                sql_results['sql_vulnerabilities'] += 1
                                sql_results['findings'].append(f"🚨 Potential SQL injection in POST {endpoint}")
                
                except Exception as e:
                    sql_results['findings'].append(f"Error testing SQL injection on {endpoint}: {e}")
        
        if sql_results['sql_vulnerabilities'] == 0 and sql_results['sql_injection_tests'] > 0:
            sql_results['findings'].append("✓ No SQL injection vulnerabilities detected")
        
        return sql_results
    
    def _test_xss_vulnerabilities(self) -> Dict[str, Any]:
        """Test for Cross-Site Scripting (XSS) vulnerabilities"""
        xss_results = {
            'xss_tests': 0,
            'xss_vulnerabilities': 0,
            'findings': []
        }
        
        # Test endpoints that display user input
        test_endpoints = [
            f'{self.frontend_url}/owners/new',
            f'{self.frontend_url}/pets/new',
            f'{self.backend_url}/api/owners'
        ]
        
        for endpoint in test_endpoints:
            for payload in self.xss_payloads:
                xss_results['xss_tests'] += 1
                
                try:
                    # Test XSS in form data
                    form_data = {
                        'firstName': payload,
                        'lastName': payload,
                        'name': payload,
                        'description': payload
                    }
                    
                    response = requests.post(endpoint, data=form_data, timeout=10)
                    
                    # Check if payload is reflected in response
                    if payload in response.text and response.headers.get('content-type', '').startswith('text/html'):
                        xss_results['xss_vulnerabilities'] += 1
                        xss_results['findings'].append(f"🚨 Potential XSS vulnerability in {endpoint}")
                    
                    # Test XSS in URL parameters
                    get_response = requests.get(
                        endpoint,
                        params={'search': payload, 'filter': payload},
                        timeout=10
                    )
                    
                    if payload in get_response.text and 'text/html' in get_response.headers.get('content-type', ''):
                        xss_results['xss_vulnerabilities'] += 1
                        xss_results['findings'].append(f"🚨 Potential XSS in URL parameters for {endpoint}")
                
                except Exception as e:
                    xss_results['findings'].append(f"Error testing XSS on {endpoint}: {e}")
        
        if xss_results['xss_vulnerabilities'] == 0 and xss_results['xss_tests'] > 0:
            xss_results['findings'].append("✓ No XSS vulnerabilities detected")
        
        return xss_results
    
    def test_secret_management(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test secret management and encryption practices"""
        secret_results = {
            'secrets_tested': 0,
            'secure_secrets': 0,
            'encryption_validated': False,
            'findings': []
        }
        
        print("Testing secret management...")
        
        # Test AWS Secrets Manager usage
        try:
            response = self.secrets_client.list_secrets()
            
            for secret in response.get('SecretList', []):
                secret_results['secrets_tested'] += 1
                secret_name = secret['Name']
                
                if 'pet-clinic' in secret_name.lower():
                    secret_results['findings'].append(f"✓ Found pet clinic secret: {secret_name}")
                    
                    # Check encryption
                    if secret.get('KmsKeyId'):
                        secret_results['secure_secrets'] += 1
                        secret_results['findings'].append(f"✓ Secret {secret_name} uses KMS encryption")
                    else:
                        secret_results['findings'].append(f"⚠ Secret {secret_name} not using KMS encryption")
        
        except Exception as e:
            secret_results['findings'].append(f"Error testing secrets: {e}")
        
        # Test for hardcoded secrets in configuration
        config_endpoints = [
            f'{self.jenkins_url}/configure',
            f'{self.backend_url}/actuator/env',
            f'{self.backend_url}/actuator/configprops'
        ]
        
        for endpoint in config_endpoints:
            try:
                response = requests.get(endpoint, timeout=10)
                
                if response.status_code == 200:
                    content = response.text.lower()
                    
                    # Look for potential hardcoded secrets
                    secret_patterns = ['password=', 'secret=', 'key=', 'token=', 'api_key=']
                    
                    for pattern in secret_patterns:
                        if pattern in content:
                            # Check if it's actually a hardcoded value (not a placeholder)
                            lines = [line for line in content.split('\n') if pattern in line]
                            for line in lines[:3]:  # Check first 3 matches
                                if not any(placeholder in line for placeholder in ['${', '***', 'xxx', 'placeholder']):
                                    secret_results['findings'].append(f"⚠ Potential hardcoded secret in {endpoint}")
                                    break
            
            except Exception as e:
                secret_results['findings'].append(f"Error checking {endpoint} for secrets: {e}")
        
        # Test database connection encryption
        try:
            response = self.rds_client.describe_db_instances()
            
            for db_instance in response['DBInstances']:
                if 'pet-clinic' in db_instance['DBInstanceIdentifier']:
                    secret_results['secrets_tested'] += 1
                    
                    if db_instance.get('StorageEncrypted', False):
                        secret_results['secure_secrets'] += 1
                        secret_results['findings'].append("✓ RDS instance uses encryption at rest")
                        secret_results['encryption_validated'] = True
                    else:
                        secret_results['findings'].append("⚠ RDS instance not encrypted at rest")
        
        except Exception as e:
            secret_results['findings'].append(f"Error checking RDS encryption: {e}")
        
        return secret_results
    
    def generate_security_report(self, test_env: Dict[str, Any], all_results: Dict[str, Any]) -> Dict[str, Any]:
        """Generate comprehensive security report"""
        report = {
            'test_summary': {
                'test_id': test_env['test_id'],
                'timestamp': test_env['timestamp'],
                'total_tests': 0,
                'vulnerabilities_found': 0,
                'overall_security_score': 0
            },
            'component_scores': {},
            'critical_findings': [],
            'recommendations': [],
            'compliance_status': {}
        }
        
        # Calculate component scores
        auth_score = all_results.get('authentication', {}).get('overall_score', 0)
        network_score = all_results.get('network_security', {}).get('security_score', 0)
        injection_score = 1.0  # Start with perfect score
        secret_score = 0
        
        # Adjust injection score based on vulnerabilities
        injection_results = all_results.get('injection_vulnerabilities', {})
        total_injection_tests = injection_results.get('sql_injection_tests', 0) + injection_results.get('xss_tests', 0)
        total_vulnerabilities = injection_results.get('sql_vulnerabilities', 0) + injection_results.get('xss_vulnerabilities', 0)
        
        if total_injection_tests > 0:
            injection_score = 1.0 - (total_vulnerabilities / total_injection_tests)
        
        # Calculate secret management score
        secret_results = all_results.get('secret_management', {})
        if secret_results.get('secrets_tested', 0) > 0:
            secret_score = secret_results.get('secure_secrets', 0) / secret_results.get('secrets_tested', 1)
        
        report['component_scores'] = {
            'authentication': auth_score,
            'network_security': network_score,
            'injection_protection': injection_score,
            'secret_management': secret_score
        }
        
        # Calculate overall security score
        scores = list(report['component_scores'].values())
        report['test_summary']['overall_security_score'] = sum(scores) / max(len(scores), 1)
        
        # Collect critical findings
        for category, results in all_results.items():
            if isinstance(results, dict) and 'findings' in results:
                for finding in results['findings']:
                    if finding.startswith('🚨'):
                        report['critical_findings'].append(finding)
                        report['test_summary']['vulnerabilities_found'] += 1
        
        # Generate recommendations
        if auth_score < 0.8:
            report['recommendations'].append("Strengthen authentication mechanisms across all components")
        
        if network_score < 0.8:
            report['recommendations'].append("Review and tighten network security controls")
        
        if injection_score < 1.0:
            report['recommendations'].append("Implement input validation and parameterized queries")
        
        if secret_score < 0.8:
            report['recommendations'].append("Improve secret management practices and encryption")
        
        # Compliance status
        report['compliance_status'] = {
            'authentication_required': auth_score >= 0.8,
            'https_enforced': network_score >= 0.7,
            'injection_protected': injection_score >= 0.9,
            'secrets_encrypted': secret_score >= 0.8
        }
        
        return report


@pytest.fixture
def security_test():
    """Fixture for security validation test"""
    return SecurityValidationTest()


def test_comprehensive_security_validation(security_test):
    """
    Comprehensive security validation and penetration testing
    
    This test validates:
    - Authentication and authorization mechanisms
    - Network security and access controls  
    - Protection against injection attacks
    - Encryption and secret management
    
    Validates: Requirements 10.1, 10.2, 10.3, 10.4
    """
    # Set up test environment
    test_env = security_test.setup_test_environment()
    print(f"Starting comprehensive security validation {test_env['test_id']}")
    
    all_results = {}
    
    try:
        # Test 1: Authentication mechanisms
        print("\n=== Testing Authentication Mechanisms ===")
        auth_results = security_test.test_authentication_mechanisms(test_env)
        all_results['authentication'] = auth_results
        
        # Assert authentication requirements
        assert auth_results['overall_score'] >= 0.6, (
            f"Authentication security too low: {auth_results['overall_score']:.2%}. "
            f"Issues found in components."
        )
        
        print(f"Authentication security score: {auth_results['overall_score']:.2%}")
        
        # Test 2: Network security
        print("\n=== Testing Network Security ===")
        network_results = security_test.test_network_security(test_env)
        all_results['network_security'] = network_results
        
        # Assert network security requirements
        assert network_results['security_score'] >= 0.7, (
            f"Network security score too low: {network_results['security_score']:.2%}. "
            f"Vulnerabilities: {network_results.get('vulnerabilities', [])}"
        )
        
        print(f"Network security score: {network_results['security_score']:.2%}")
        
        # Test 3: Injection vulnerabilities
        print("\n=== Testing Injection Vulnerabilities ===")
        injection_results = security_test.test_injection_vulnerabilities(test_env)
        all_results['injection_vulnerabilities'] = injection_results
        
        # Assert injection protection
        total_vulnerabilities = injection_results.get('sql_vulnerabilities', 0) + injection_results.get('xss_vulnerabilities', 0)
        assert total_vulnerabilities == 0, (
            f"Injection vulnerabilities found: {total_vulnerabilities} "
            f"(SQL: {injection_results.get('sql_vulnerabilities', 0)}, "
            f"XSS: {injection_results.get('xss_vulnerabilities', 0)})"
        )
        
        print(f"Injection tests: {injection_results.get('sql_injection_tests', 0)} SQL, {injection_results.get('xss_tests', 0)} XSS")
        
        # Test 4: Secret management
        print("\n=== Testing Secret Management ===")
        secret_results = security_test.test_secret_management(test_env)
        all_results['secret_management'] = secret_results
        
        # Assert secret management (more lenient for test environment)
        if secret_results.get('secrets_tested', 0) > 0:
            secret_score = secret_results.get('secure_secrets', 0) / secret_results.get('secrets_tested', 1)
            assert secret_score >= 0.5, (
                f"Secret management score too low: {secret_score:.2%}"
            )
            print(f"Secret management score: {secret_score:.2%}")
        else:
            print("No secrets found to test (acceptable for test environment)")
        
        # Generate comprehensive security report
        print("\n=== Generating Security Report ===")
        security_report = security_test.generate_security_report(test_env, all_results)
        
        # Assert overall security posture
        overall_score = security_report['test_summary']['overall_security_score']
        assert overall_score >= 0.7, (
            f"Overall security score too low: {overall_score:.2%}. "
            f"Critical findings: {len(security_report['critical_findings'])}"
        )
        
        # Log security validation results
        print(f"\n✓ Comprehensive security validation completed")
        print(f"  - Overall security score: {overall_score:.2%}")
        print(f"  - Authentication score: {security_report['component_scores']['authentication']:.2%}")
        print(f"  - Network security score: {security_report['component_scores']['network_security']:.2%}")
        print(f"  - Injection protection score: {security_report['component_scores']['injection_protection']:.2%}")
        print(f"  - Secret management score: {security_report['component_scores']['secret_management']:.2%}")
        print(f"  - Critical vulnerabilities found: {security_report['test_summary']['vulnerabilities_found']}")
        
        # Log compliance status
        compliance = security_report['compliance_status']
        print(f"  - Authentication required: {'✓' if compliance['authentication_required'] else '✗'}")
        print(f"  - HTTPS enforced: {'✓' if compliance['https_enforced'] else '✗'}")
        print(f"  - Injection protected: {'✓' if compliance['injection_protected'] else '✗'}")
        print(f"  - Secrets encrypted: {'✓' if compliance['secrets_encrypted'] else '✗'}")
        
        # Log recommendations if any
        if security_report['recommendations']:
            print(f"  - Recommendations: {len(security_report['recommendations'])}")
            for rec in security_report['recommendations'][:3]:  # Show first 3
                print(f"    • {rec}")
        
        # Final assertion on critical vulnerabilities
        assert security_report['test_summary']['vulnerabilities_found'] == 0, (
            f"Critical security vulnerabilities found: {security_report['critical_findings']}"
        )
        
    except Exception as e:
        print(f"Security validation error: {e}")
        raise


def test_authentication_enforcement_focused():
    """
    Focused test for authentication enforcement
    Validates: Requirements 10.1, 10.2
    """
    test_instance = SecurityValidationTest()
    test_env = test_instance.setup_test_environment()
    
    # Test authentication mechanisms
    auth_results = test_instance.test_authentication_mechanisms(test_env)
    
    print("Authentication test results:")
    for component, result in auth_results.items():
        if isinstance(result, dict) and 'findings' in result:
            print(f"\n{component}:")
            for finding in result['findings']:
                print(f"  {finding}")
    
    # Assert basic authentication requirements
    assert auth_results['overall_score'] > 0, "No authentication mechanisms found"
    
    # Check that at least one component has secure authentication
    secure_components = sum(1 for comp in ['jenkins_auth', 'frontend_auth', 'backend_auth'] 
                          if auth_results.get(comp, {}).get('secure', False))
    assert secure_components > 0, "No components have secure authentication"
    
    print(f"\n✓ Authentication enforcement test completed")
    print(f"  - Overall score: {auth_results['overall_score']:.2%}")
    print(f"  - Secure components: {secure_components}/3")


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v', '--tb=short'])