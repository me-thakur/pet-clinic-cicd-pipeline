# Security Testing Suite for Pet Clinic CI/CD Pipeline

This directory contains comprehensive security validation and penetration testing suites for the Pet Clinic CI/CD Pipeline.

## Test Coverage

### Security Validation (`test-security-validation.py`)

Comprehensive security validation covering:

1. **Authentication Mechanisms**
   - Jenkins authentication security
   - Frontend authentication enforcement
   - Backend API authentication
   - Default credential testing
   - Authentication bypass detection

2. **Network Security**
   - Port scanning and open port analysis
   - SSL/TLS configuration validation
   - Certificate verification
   - Firewall rule effectiveness
   - Security group compliance

3. **Injection Vulnerability Testing**
   - SQL injection detection
   - Cross-Site Scripting (XSS) testing
   - Input validation verification
   - Error message analysis

4. **Secret Management**
   - AWS Secrets Manager usage
   - Hardcoded secret detection
   - Encryption at rest validation
   - Configuration security review

### Penetration Testing (`test-penetration-testing.py`)

Simulated attack scenarios including:

1. **Authentication Bypass Attacks**
   - Direct URL access attempts
   - HTTP method bypass testing
   - Header manipulation attacks
   - Session fixation attempts

2. **Brute Force Attacks**
   - Password brute forcing
   - Rate limiting validation
   - Account lockout testing
   - Common credential testing

3. **Injection Attacks**
   - Directory traversal attacks
   - Command injection attempts
   - XXE (XML External Entity) attacks
   - Path manipulation testing

4. **Cross-Site Request Forgery (CSRF)**
   - CSRF token validation
   - State-changing operation protection
   - API endpoint CSRF testing

5. **Session Management Attacks**
   - Session fixation testing
   - Cookie security validation
   - Session timeout verification

## Environment Configuration

### Required Environment Variables

```bash
# Application URLs
export JENKINS_URL="http://your-jenkins-server:8080"
export FRONTEND_URL="http://your-frontend-server:8080"
export BACKEND_URL="http://your-backend-server:8081"

# Authentication Credentials
export JENKINS_USER="admin"
export JENKINS_TOKEN="your-jenkins-api-token"
export TEST_USERNAME="testuser"
export TEST_PASSWORD="testpass"

# AWS Configuration
export AWS_REGION="us-east-1"
export CLOUDFORMATION_STACK="pet-clinic-pipeline"

# Optional Test Configuration
export S3_ARTIFACTS_BUCKET="pet-clinic-artifacts"
export TEST_REPO="your-org/pet-clinic-repo"
```

### AWS Permissions Required

The security tests require the following AWS permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeInstances",
                "ec2:DescribeSecurityGroups",
                "rds:DescribeDBInstances",
                "s3:ListBucket",
                "s3:GetObject",
                "secretsmanager:ListSecrets",
                "secretsmanager:DescribeSecret",
                "cloudformation:DescribeStacks",
                "cloudformation:DescribeStackResources"
            ],
            "Resource": "*"
        }
    ]
}
```

## Running the Tests

### Prerequisites

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Configure AWS credentials:
```bash
aws configure
# or set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
```

3. Set environment variables as described above

### Execute Security Tests

Run all security tests:
```bash
pytest test-security-validation.py test-penetration-testing.py -v
```

Run security validation only:
```bash
pytest test-security-validation.py -v
```

Run penetration testing only:
```bash
pytest test-penetration-testing.py -v
```

Run focused authentication tests:
```bash
pytest test-security-validation.py::test_authentication_enforcement_focused -v
pytest test-penetration-testing.py::test_focused_authentication_attacks -v
```

### Test Execution Time

- **Security Validation**: 5-10 minutes
- **Penetration Testing**: 10-15 minutes
- **Combined Suite**: 15-25 minutes

## Test Results Interpretation

### Security Validation Results

The security validation test provides:

- **Component Scores**: Individual scores for authentication, network security, injection protection, and secret management
- **Overall Security Score**: Weighted average of all component scores
- **Compliance Status**: Pass/fail status for key security requirements
- **Findings**: Detailed security observations and recommendations

### Penetration Testing Results

The penetration testing suite provides:

- **Attack Success Rate**: Percentage of successful attacks out of total attempts
- **Vulnerability Count**: Number of security vulnerabilities discovered
- **Risk Assessment**: Overall risk level (LOW/MEDIUM/HIGH)
- **Vulnerability Breakdown**: Categorized vulnerability counts
- **Security Recommendations**: Actionable security improvements

### Success Criteria

Tests pass when:
- Overall security score ≥ 70%
- No critical vulnerabilities found
- Attack success rate ≤ 5%
- Authentication mechanisms properly enforced
- Network security controls effective
- No injection vulnerabilities detected
- Secret management practices secure

## Security Test Categories

### 🟢 Low Risk Findings
- Missing security headers (non-critical)
- Minor configuration improvements
- Informational security observations

### 🟡 Medium Risk Findings
- Weak security configurations
- Missing security controls (non-critical paths)
- Potential information disclosure

### 🔴 High Risk Findings
- Authentication bypass vulnerabilities
- Injection vulnerabilities (SQL, XSS, Command)
- Sensitive data exposure
- Critical security misconfigurations

## Common Issues and Solutions

### Issue: Connection Timeouts
**Cause**: Network connectivity or firewall blocking
**Solution**: 
- Verify application URLs are accessible
- Check network connectivity
- Ensure security groups allow test traffic

### Issue: Authentication Failures
**Cause**: Invalid credentials or authentication changes
**Solution**:
- Verify Jenkins token is valid and has API access
- Check application authentication requirements
- Update test credentials if needed

### Issue: AWS Permission Errors
**Cause**: Insufficient AWS permissions
**Solution**:
- Verify AWS credentials are configured
- Ensure IAM permissions include required actions
- Check resource access permissions

### Issue: False Positives
**Cause**: Test environment differences or expected security measures
**Solution**:
- Review test findings for context
- Adjust test expectations for environment
- Document expected security configurations

## Customizing Security Tests

### Adding New Attack Vectors

To add new penetration tests:

1. Add payload patterns to the respective class
2. Create test method following naming convention
3. Update vulnerability counting logic
4. Add findings to the report generation

### Modifying Security Thresholds

Security score thresholds can be adjusted in the test assertions:

```python
# Example: Lower authentication score threshold
assert auth_results['overall_score'] >= 0.5  # Changed from 0.6
```

### Environment-Specific Configurations

For different environments, create environment-specific configuration:

```python
# Example: Development environment with relaxed security
if os.getenv('ENVIRONMENT') == 'development':
    # Allow lower security scores for dev environment
    min_security_score = 0.5
else:
    min_security_score = 0.7
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Security Testing
on:
  schedule:
    - cron: '0 2 * * 0'  # Weekly security tests
  workflow_dispatch:

jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      - name: Install dependencies
        run: pip install -r tests/security/requirements.txt
      - name: Run security validation
        env:
          JENKINS_URL: ${{ secrets.JENKINS_URL }}
          JENKINS_TOKEN: ${{ secrets.JENKINS_TOKEN }}
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        run: pytest tests/security/test-security-validation.py -v
      - name: Run penetration tests
        env:
          JENKINS_URL: ${{ secrets.JENKINS_URL }}
          FRONTEND_URL: ${{ secrets.FRONTEND_URL }}
          BACKEND_URL: ${{ secrets.BACKEND_URL }}
        run: pytest tests/security/test-penetration-testing.py -v
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Security Testing') {
            steps {
                script {
                    sh '''
                        cd tests/security
                        pip install -r requirements.txt
                        pytest test-security-validation.py -v --junitxml=security-results.xml
                        pytest test-penetration-testing.py -v --junitxml=pentest-results.xml
                    '''
                }
            }
            post {
                always {
                    junit 'tests/security/*-results.xml'
                }
            }
        }
    }
}
```

## Security Best Practices

Based on the security tests, follow these best practices:

1. **Authentication**
   - Implement strong authentication mechanisms
   - Use multi-factor authentication where possible
   - Regularly rotate credentials and tokens
   - Implement account lockout policies

2. **Network Security**
   - Use HTTPS for all communications
   - Implement proper firewall rules
   - Regularly review security group configurations
   - Monitor network traffic for anomalies

3. **Input Validation**
   - Validate all user inputs
   - Use parameterized queries for database access
   - Implement proper output encoding
   - Sanitize file uploads and paths

4. **Secret Management**
   - Use AWS Secrets Manager or similar services
   - Never hardcode secrets in configuration
   - Implement encryption at rest and in transit
   - Regularly rotate secrets and keys

5. **Session Management**
   - Use secure session tokens
   - Implement proper session timeout
   - Use HttpOnly and Secure cookie flags
   - Prevent session fixation attacks

## Reporting Security Issues

If security tests identify critical vulnerabilities:

1. **Immediate Actions**
   - Document the vulnerability details
   - Assess the potential impact
   - Implement temporary mitigations if possible

2. **Remediation Process**
   - Create security incident tickets
   - Prioritize fixes based on risk level
   - Test fixes in development environment
   - Deploy fixes following change management process

3. **Verification**
   - Re-run security tests after fixes
   - Perform additional manual testing
   - Update security documentation
   - Schedule regular security reviews

## Contributing

When contributing to security tests:

1. Follow responsible disclosure practices
2. Test in isolated environments only
3. Document new test cases thoroughly
4. Update this README with new test descriptions
5. Ensure tests are non-destructive and reversible