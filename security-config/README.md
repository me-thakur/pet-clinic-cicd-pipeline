# Security Configuration for Pet Clinic CI/CD Pipeline

This directory contains comprehensive security hardening and HTTPS configuration for the Pet Clinic CI/CD Pipeline system.

## Overview

The security system provides:

- **HTTPS/TLS Configuration**: SSL certificate management and secure web communications
- **System Hardening**: Comprehensive security hardening measures
- **Secrets Management**: Secure storage and management of sensitive credentials
- **Access Control**: Authentication and authorization mechanisms
- **Security Monitoring**: Intrusion detection and security auditing
- **Compliance**: Industry standard security practices and frameworks

## Components

### 1. SSL Certificate Setup (`ssl-certificate-setup.sh`)

Configures HTTPS and SSL/TLS certificates:
- **Let's Encrypt Integration**: Automatic certificate provisioning and renewal
- **Self-Signed Certificates**: Development and testing certificate generation
- **Nginx HTTPS Configuration**: Secure reverse proxy with security headers
- **Security Headers**: HSTS, CSP, XSS protection, and more
- **Certificate Auto-Renewal**: Automated certificate renewal with cron jobs

### 2. Security Hardening (`security-hardening.sh`)

Comprehensive system security hardening:
- **SSH Hardening**: Secure SSH configuration with strong algorithms
- **System Security Limits**: Kernel parameters and security settings
- **Password Policies**: Strong password requirements and account lockout
- **Sudo Security**: Secure privilege escalation configuration
- **File System Security**: Proper permissions and access controls
- **Network Security**: Fail2ban, host-based access controls
- **Audit Logging**: Comprehensive system event auditing
- **Security Tools**: Rootkit detection, file integrity monitoring

### 3. Secrets Management (`secrets-management.sh`)

Secure credential and secret management:
- **Encryption**: AES-256-CBC encryption for all secrets
- **Key Management**: Secure encryption key generation and storage
- **Database Credentials**: Encrypted database connection information
- **API Keys**: Secure storage of third-party API credentials
- **SSL Certificates**: Encrypted keystore password management
- **Secret Rotation**: Automated secret rotation policies
- **Backup/Restore**: Secure backup and recovery procedures

## Installation

### Prerequisites

- Ubuntu/Debian or RHEL/CentOS/Amazon Linux
- Root or sudo access
- Internet connectivity for package installation
- Domain name (for Let's Encrypt certificates)

### Quick Setup

Run all security configurations:

```bash
# Set environment variables
export DOMAIN_NAME="petclinic.yourdomain.com"
export DB_PASSWORD="your-secure-db-password"
export JENKINS_API_TOKEN="your-jenkins-token"
export GITHUB_TOKEN="your-github-token"
export SLACK_WEBHOOK_URL="your-slack-webhook"

# Run security setup scripts
sudo ./security-hardening.sh
sudo ./ssl-certificate-setup.sh --domain $DOMAIN_NAME --letsencrypt
sudo ./secrets-management.sh
```

### Individual Component Setup

#### SSL Certificate Setup

```bash
# For production with Let's Encrypt
sudo ./ssl-certificate-setup.sh --domain petclinic.yourdomain.com --letsencrypt

# For development with self-signed certificate
sudo ./ssl-certificate-setup.sh --domain petclinic.local
```

#### Security Hardening

```bash
# Full security hardening
sudo ./security-hardening.sh

# Skip network security (if using external firewall)
sudo ./security-hardening.sh --skip-network

# Skip audit logging (if using external SIEM)
sudo ./security-hardening.sh --skip-audit
```

#### Secrets Management

```bash
# Full secrets management setup
sudo ./secrets-management.sh

# Database credentials only
sudo ./secrets-management.sh --db-only
```

## Configuration

### Environment Variables

Set these environment variables before running setup scripts:

```bash
# SSL Configuration
export DOMAIN_NAME="petclinic.yourdomain.com"
export JENKINS_USER="admin"
export JENKINS_PASSWORD="secure-password"

# Database Configuration
export DB_USERNAME="petclinic"
export DB_PASSWORD="secure-db-password"
export DB_HOST="localhost"
export DB_PORT="3306"
export DB_NAME="petclinic"

# API Credentials
export JENKINS_API_TOKEN="your-jenkins-api-token"
export GITHUB_TOKEN="your-github-personal-access-token"
export AWS_ACCESS_KEY_ID="your-aws-access-key"
export AWS_SECRET_ACCESS_KEY="your-aws-secret-key"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
export PAGERDUTY_INTEGRATION_KEY="your-pagerduty-integration-key"
```

### SSL/TLS Configuration

#### Certificate Types

**Let's Encrypt (Production)**:
- Automatic certificate provisioning
- 90-day validity with auto-renewal
- Requires publicly accessible domain
- Free and trusted by all browsers

**Self-Signed (Development/Testing)**:
- Generated locally
- 365-day validity
- Not trusted by browsers (requires exception)
- Suitable for internal testing

#### Security Headers

The Nginx configuration includes comprehensive security headers:

```nginx
# HSTS (HTTP Strict Transport Security)
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;

# Security Headers
add_header X-Frame-Options DENY always;
add_header X-Content-Type-Options nosniff always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self';" always;
```

### Security Hardening Configuration

#### SSH Hardening

Key security improvements:
- Root login disabled
- Password authentication disabled
- Strong encryption algorithms only
- Connection limits and timeouts
- Enhanced logging

#### System Security

Kernel security parameters:
- Network security settings
- Memory protection
- File system security
- Process security controls

#### Password Policies

Strong password requirements:
- Minimum 12 characters
- Mixed case, numbers, special characters
- Password history (12 passwords)
- Account lockout after 5 failed attempts

### Secrets Management Configuration

#### Encryption

All secrets are encrypted using:
- **Algorithm**: AES-256-CBC
- **Key Derivation**: PBKDF2 with 100,000 iterations
- **Salt**: Random salt for each encryption
- **Key Storage**: Separate encryption key file

#### Secret Types

**Database Credentials**:
- Username, password, host, port, database name
- Connection string with SSL parameters

**API Credentials**:
- Jenkins API token
- GitHub personal access token
- AWS access keys
- Slack webhook URL
- PagerDuty integration key

**SSL Certificates**:
- Keystore passwords
- Certificate passphrases

## Usage

### SSL Certificate Management

#### Check Certificate Status

```bash
# Check certificate expiration
openssl x509 -in /etc/ssl/petclinic/petclinic.local.crt -noout -dates

# Test SSL connection
openssl s_client -connect petclinic.local:443 -servername petclinic.local
```

#### Manual Certificate Renewal

```bash
# Renew Let's Encrypt certificate
sudo certbot renew

# Reload Nginx with new certificate
sudo systemctl reload nginx
```

### Secret Management

#### Retrieve Secrets

```bash
# Get database password
get-secret.sh db_password

# Get GitHub token
get-secret.sh github_token

# List available secrets
get-secret.sh
```

#### Rotate Secrets

```bash
# Rotate all secrets
sudo rotate-secrets.sh all

# Rotate specific secret
sudo rotate-secrets.sh database
sudo rotate-secrets.sh jwt
sudo rotate-secrets.sh ssl
```

#### Backup and Restore

```bash
# Backup secrets
sudo backup-secrets.sh

# List available backups
ls -la /var/backups/petclinic-secrets/

# Restore from backup
sudo restore-secrets.sh /var/backups/petclinic-secrets/secrets-backup-20231201-120000.tar.gz
```

### Security Monitoring

#### Check Security Status

```bash
# Check fail2ban status
sudo fail2ban-client status

# Check audit logs
sudo ausearch -m avc -ts recent

# Run security scan
sudo /usr/local/bin/security-scan.sh
```

#### View Security Logs

```bash
# SSH authentication logs
sudo tail -f /var/log/auth.log

# Fail2ban logs
sudo tail -f /var/log/fail2ban.log

# Audit logs
sudo tail -f /var/log/audit/audit.log

# Security hardening logs
sudo tail -f /var/log/security-hardening.log
```

## Security Features

### Network Security

- **Firewall**: UFW configured with minimal open ports
- **Fail2ban**: Intrusion prevention system
- **Host-based Access Control**: TCP wrappers configuration
- **Network Parameters**: Kernel-level network security

### Access Control

- **SSH Key Authentication**: Password authentication disabled
- **Multi-factor Authentication**: Ready for implementation
- **Role-based Access**: Sudo configuration with least privilege
- **Session Management**: Secure session handling

### Data Protection

- **Encryption at Rest**: All secrets encrypted with strong algorithms
- **Encryption in Transit**: HTTPS/TLS for all web communications
- **Key Management**: Secure key generation and storage
- **Data Integrity**: File integrity monitoring with AIDE

### Monitoring and Auditing

- **System Auditing**: Comprehensive audit trail with auditd
- **Intrusion Detection**: Rootkit and malware detection
- **Log Management**: Centralized logging with rotation
- **Security Scanning**: Regular automated security scans

## Compliance

### Security Frameworks

The configuration addresses requirements from:

- **CIS Benchmarks**: Center for Internet Security guidelines
- **NIST Cybersecurity Framework**: Risk management framework
- **OWASP**: Web application security guidelines
- **PCI DSS**: Payment card industry standards (where applicable)
- **SOC 2**: Service organization controls

### Compliance Features

- **Access Controls**: Authentication and authorization
- **Data Encryption**: At rest and in transit
- **Audit Logging**: Comprehensive activity logging
- **Vulnerability Management**: Regular security scanning
- **Incident Response**: Monitoring and alerting capabilities

## Troubleshooting

### Common Issues

#### SSL Certificate Issues

**Problem**: Certificate not trusted by browsers
**Solution**: 
```bash
# Check certificate chain
openssl s_client -connect petclinic.local:443 -showcerts

# Verify certificate configuration
sudo nginx -t
```

**Problem**: Certificate renewal fails
**Solution**:
```bash
# Check certbot logs
sudo tail -f /var/log/letsencrypt/letsencrypt.log

# Manual renewal with verbose output
sudo certbot renew --dry-run --verbose
```

#### Secret Access Issues

**Problem**: Cannot decrypt secrets
**Solution**:
```bash
# Check encryption key permissions
ls -la /etc/petclinic/secrets/.encryption.key

# Verify secret file integrity
file /etc/petclinic/secrets/*.enc
```

**Problem**: Application cannot access secrets
**Solution**:
```bash
# Check file permissions
ls -la /etc/petclinic/secrets/
ls -la /usr/local/bin/get-secret.sh

# Test secret retrieval
sudo -u jenkins get-secret.sh db_password
```

#### Security Hardening Issues

**Problem**: SSH connection refused
**Solution**:
```bash
# Check SSH configuration
sudo sshd -t

# Check SSH service status
sudo systemctl status sshd

# Review SSH logs
sudo tail -f /var/log/auth.log
```

**Problem**: Application fails to start after hardening
**Solution**:
```bash
# Check system limits
ulimit -a

# Review application logs
sudo tail -f /var/log/petclinic/application.log

# Check SELinux/AppArmor status
sudo getenforce  # SELinux
sudo aa-status   # AppArmor
```

### Log Locations

- **SSL Setup**: `/var/log/ssl-setup.log`
- **Security Hardening**: `/var/log/security-hardening.log`
- **Secrets Management**: `/var/log/secrets-management.log`
- **Nginx Access**: `/var/log/nginx/petclinic-access.log`
- **Nginx Error**: `/var/log/nginx/petclinic-error.log`
- **SSH Authentication**: `/var/log/auth.log`
- **Fail2ban**: `/var/log/fail2ban.log`
- **Audit**: `/var/log/audit/audit.log`
- **Security Scans**: `/var/log/security-scan.log`

### Useful Commands

```bash
# Check SSL certificate expiration
openssl x509 -in /etc/ssl/petclinic/petclinic.local.crt -noout -dates

# Test HTTPS connection
curl -I https://petclinic.local/health

# Check fail2ban status
sudo fail2ban-client status sshd

# View recent audit events
sudo ausearch -ts recent

# Check system security status
sudo lynis audit system

# Test secret encryption/decryption
echo "test" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -pass file:/etc/petclinic/secrets/.encryption.key | openssl enc -aes-256-cbc -d -salt -pbkdf2 -iter 100000 -pass file:/etc/petclinic/secrets/.encryption.key
```

## Maintenance

### Regular Tasks

#### Daily
- Monitor security logs for anomalies
- Check system resource usage
- Verify backup completion

#### Weekly
- Review fail2ban reports
- Update system packages
- Run security scans

#### Monthly
- Rotate secrets (automated)
- Review access logs
- Update security configurations
- Test disaster recovery procedures

#### Quarterly
- Security assessment with Lynis
- Review and update security policies
- Penetration testing
- Compliance audit

### Updates and Patches

#### System Updates

```bash
# Update system packages
sudo apt update && sudo apt upgrade -y  # Ubuntu/Debian
sudo yum update -y                      # RHEL/CentOS

# Update security tools
sudo rkhunter --update
sudo freshclam  # ClamAV signatures
```

#### Configuration Updates

```bash
# Update SSL configuration
sudo ./ssl-certificate-setup.sh --domain petclinic.local

# Update security hardening
sudo ./security-hardening.sh

# Regenerate secrets
sudo rotate-secrets.sh all
```

## Security Contacts

- **Security Team**: security@petclinic.local
- **System Administrator**: admin@petclinic.local
- **Emergency Contact**: emergency@petclinic.local
- **Compliance Officer**: compliance@petclinic.local

## Documentation

### Security Reports

After running the setup scripts, detailed reports are generated:

- **SSL Configuration**: `/root/petclinic-security-report.txt`
- **Security Hardening**: `/root/petclinic-security-hardening-report.txt`
- **Secrets Management**: `/root/petclinic-secrets-management-report.txt`

### Additional Resources

- [OWASP Security Guidelines](https://owasp.org/)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Nginx Security Guide](https://nginx.org/en/docs/http/securing_http.html)

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2023-12-01 | 1.0 | Initial security configuration |
| 2023-12-15 | 1.1 | Added secrets management |
| 2024-01-01 | 1.2 | Enhanced SSL configuration and monitoring |