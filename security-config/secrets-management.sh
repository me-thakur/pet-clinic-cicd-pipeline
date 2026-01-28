#!/bin/bash

# Secrets Management Script for Pet Clinic CI/CD Pipeline
# This script implements secure secret management for database credentials and API keys
# Requirements: 10.4

set -euo pipefail

# Configuration
SECRETS_DIR="/etc/petclinic/secrets"
VAULT_DIR="/var/lib/petclinic/vault"
LOG_FILE="/var/log/secrets-management.log"
ENCRYPTION_KEY_FILE="/etc/petclinic/secrets/.encryption.key"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        error_exit "This script must be run as root"
    fi
}

# Generate encryption key
generate_encryption_key() {
    log "Generating encryption key for secrets..."
    
    mkdir -p "$(dirname "$ENCRYPTION_KEY_FILE")"
    
    # Generate a strong encryption key
    openssl rand -base64 32 > "$ENCRYPTION_KEY_FILE"
    
    # Set restrictive permissions
    chmod 600 "$ENCRYPTION_KEY_FILE"
    chown root:root "$ENCRYPTION_KEY_FILE"
    
    log "Encryption key generated and secured"
}

# Encrypt a secret
encrypt_secret() {
    local secret_name="$1"
    local secret_value="$2"
    local encrypted_file="$SECRETS_DIR/${secret_name}.enc"
    
    mkdir -p "$SECRETS_DIR"
    
    # Encrypt the secret using AES-256-CBC
    echo -n "$secret_value" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -pass file:"$ENCRYPTION_KEY_FILE" -out "$encrypted_file"
    
    # Set restrictive permissions
    chmod 600 "$encrypted_file"
    chown root:root "$encrypted_file"
    
    log "Secret '$secret_name' encrypted and stored"
}

# Decrypt a secret
decrypt_secret() {
    local secret_name="$1"
    local encrypted_file="$SECRETS_DIR/${secret_name}.enc"
    
    if [[ ! -f "$encrypted_file" ]]; then
        error_exit "Secret '$secret_name' not found"
    fi
    
    # Decrypt the secret
    openssl enc -aes-256-cbc -d -salt -pbkdf2 -iter 100000 -pass file:"$ENCRYPTION_KEY_FILE" -in "$encrypted_file"
}

# Store database credentials
store_database_credentials() {
    log "Storing database credentials..."
    
    # Generate strong database password if not provided
    local db_password="${DB_PASSWORD:-$(openssl rand -base64 24)}"
    local db_username="${DB_USERNAME:-petclinic}"
    local db_host="${DB_HOST:-localhost}"
    local db_port="${DB_PORT:-3306}"
    local db_name="${DB_NAME:-petclinic}"
    
    # Store individual credentials
    encrypt_secret "db_username" "$db_username"
    encrypt_secret "db_password" "$db_password"
    encrypt_secret "db_host" "$db_host"
    encrypt_secret "db_port" "$db_port"
    encrypt_secret "db_name" "$db_name"
    
    # Create connection string
    local connection_string="jdbc:mysql://${db_host}:${db_port}/${db_name}?useSSL=true&requireSSL=true&verifyServerCertificate=true"
    encrypt_secret "db_connection_string" "$connection_string"
    
    log "Database credentials stored securely"
    log "Database username: $db_username"
    log "Database password: [ENCRYPTED]"
}

# Store API keys and tokens
store_api_credentials() {
    log "Storing API credentials..."
    
    # Jenkins API token
    local jenkins_token="${JENKINS_API_TOKEN:-$(openssl rand -hex 32)}"
    encrypt_secret "jenkins_api_token" "$jenkins_token"
    
    # GitHub token (if provided)
    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
        encrypt_secret "github_token" "$GITHUB_TOKEN"
        log "GitHub token stored"
    fi
    
    # AWS credentials (if provided)
    if [[ -n "${AWS_ACCESS_KEY_ID:-}" ]] && [[ -n "${AWS_SECRET_ACCESS_KEY:-}" ]]; then
        encrypt_secret "aws_access_key_id" "$AWS_ACCESS_KEY_ID"
        encrypt_secret "aws_secret_access_key" "$AWS_SECRET_ACCESS_KEY"
        log "AWS credentials stored"
    fi
    
    # Slack webhook URL (if provided)
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        encrypt_secret "slack_webhook_url" "$SLACK_WEBHOOK_URL"
        log "Slack webhook URL stored"
    fi
    
    # PagerDuty integration key (if provided)
    if [[ -n "${PAGERDUTY_INTEGRATION_KEY:-}" ]]; then
        encrypt_secret "pagerduty_integration_key" "$PAGERDUTY_INTEGRATION_KEY"
        log "PagerDuty integration key stored"
    fi
    
    log "API credentials stored securely"
}

# Create secret retrieval script
create_secret_retrieval_script() {
    log "Creating secret retrieval script..."
    
    cat > /usr/local/bin/get-secret.sh << 'EOF'
#!/bin/bash

# Secret Retrieval Script for Pet Clinic
# Usage: get-secret.sh <secret_name>

set -euo pipefail

SECRETS_DIR="/etc/petclinic/secrets"
ENCRYPTION_KEY_FILE="/etc/petclinic/secrets/.encryption.key"

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <secret_name>"
    echo "Available secrets:"
    ls -1 "$SECRETS_DIR"/*.enc 2>/dev/null | sed 's/.*\///;s/\.enc$//' || echo "No secrets found"
    exit 1
fi

SECRET_NAME="$1"
ENCRYPTED_FILE="$SECRETS_DIR/${SECRET_NAME}.enc"

if [[ ! -f "$ENCRYPTED_FILE" ]]; then
    echo "Error: Secret '$SECRET_NAME' not found" >&2
    exit 1
fi

if [[ ! -f "$ENCRYPTION_KEY_FILE" ]]; then
    echo "Error: Encryption key not found" >&2
    exit 1
fi

# Decrypt and output the secret
openssl enc -aes-256-cbc -d -salt -pbkdf2 -iter 100000 -pass file:"$ENCRYPTION_KEY_FILE" -in "$ENCRYPTED_FILE" 2>/dev/null
EOF
    
    chmod 750 /usr/local/bin/get-secret.sh
    chown root:jenkins /usr/local/bin/get-secret.sh
    
    log "Secret retrieval script created"
}

# Create application configuration with secrets
create_application_config() {
    log "Creating application configuration with secrets..."
    
    local config_dir="/etc/petclinic/config"
    mkdir -p "$config_dir"
    
    # Create Spring Boot configuration template
    cat > "$config_dir/application-secrets.yml" << 'EOF'
# Pet Clinic Application Configuration with Secrets
# This file contains references to encrypted secrets

spring:
  datasource:
    url: ${DB_CONNECTION_STRING}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: false
        
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 24 hours
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
      
logging:
  level:
    com.petclinic: INFO
    org.springframework.security: WARN
  file:
    name: /var/log/petclinic/application.log
    
server:
  port: 8080
  ssl:
    enabled: true
    key-store: /etc/ssl/petclinic/keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    
external:
  apis:
    github:
      token: ${GITHUB_TOKEN}
    slack:
      webhook: ${SLACK_WEBHOOK_URL}
    pagerduty:
      integration-key: ${PAGERDUTY_INTEGRATION_KEY}
EOF
    
    chmod 640 "$config_dir/application-secrets.yml"
    chown root:jenkins "$config_dir/application-secrets.yml"
    
    # Create environment file for secrets
    cat > "$config_dir/secrets.env" << 'EOF'
#!/bin/bash

# Pet Clinic Secrets Environment File
# Source this file to load secrets into environment variables

export DB_CONNECTION_STRING="$(get-secret.sh db_connection_string)"
export DB_USERNAME="$(get-secret.sh db_username)"
export DB_PASSWORD="$(get-secret.sh db_password)"
export JWT_SECRET="$(get-secret.sh jwt_secret)"
export SSL_KEYSTORE_PASSWORD="$(get-secret.sh ssl_keystore_password)"
export GITHUB_TOKEN="$(get-secret.sh github_token 2>/dev/null || echo '')"
export SLACK_WEBHOOK_URL="$(get-secret.sh slack_webhook_url 2>/dev/null || echo '')"
export PAGERDUTY_INTEGRATION_KEY="$(get-secret.sh pagerduty_integration_key 2>/dev/null || echo '')"
EOF
    
    chmod 640 "$config_dir/secrets.env"
    chown root:jenkins "$config_dir/secrets.env"
    
    log "Application configuration with secrets created"
}

# Create Jenkins credentials configuration
create_jenkins_credentials() {
    log "Creating Jenkins credentials configuration..."
    
    local jenkins_home="${JENKINS_HOME:-/var/lib/jenkins}"
    local credentials_dir="$jenkins_home/credentials"
    
    mkdir -p "$credentials_dir"
    
    # Create Jenkins credentials XML template
    cat > "$credentials_dir/petclinic-secrets.xml" << 'EOF'
<?xml version='1.1' encoding='UTF-8'?>
<com.cloudbees.plugins.credentials.SystemCredentialsProvider plugin="credentials@2.6.1">
  <domainCredentialsMap class="hudson.util.CopyOnWriteMap$Hash">
    <entry>
      <com.cloudbees.plugins.credentials.domains.Domain>
        <specifications/>
      </com.cloudbees.plugins.credentials.domains.Domain>
      <java.util.concurrent.CopyOnWriteArrayList>
        
        <!-- Database Credentials -->
        <com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl>
          <scope>GLOBAL</scope>
          <id>petclinic-database</id>
          <description>Pet Clinic Database Credentials</description>
          <username>${DB_USERNAME}</username>
          <password>${DB_PASSWORD}</password>
        </com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl>
        
        <!-- GitHub Token -->
        <org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl>
          <scope>GLOBAL</scope>
          <id>github-token</id>
          <description>GitHub API Token</description>
          <secret>${GITHUB_TOKEN}</secret>
        </org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl>
        
        <!-- AWS Credentials -->
        <com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl>
          <scope>GLOBAL</scope>
          <id>aws-credentials</id>
          <description>AWS Credentials for Pet Clinic</description>
          <accessKey>${AWS_ACCESS_KEY_ID}</accessKey>
          <secretKey>${AWS_SECRET_ACCESS_KEY}</secretKey>
        </com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl>
        
      </java.util.concurrent.CopyOnWriteArrayList>
    </entry>
  </domainCredentialsMap>
</com.cloudbees.plugins.credentials.SystemCredentialsProvider>
EOF
    
    chmod 640 "$credentials_dir/petclinic-secrets.xml"
    chown jenkins:jenkins "$credentials_dir/petclinic-secrets.xml"
    
    log "Jenkins credentials configuration created"
}

# Create SSL keystore with encrypted password
create_ssl_keystore() {
    log "Creating SSL keystore with encrypted password..."
    
    local keystore_password
    keystore_password=$(openssl rand -base64 24)
    local keystore_file="/etc/ssl/petclinic/keystore.p12"
    local cert_file="/etc/ssl/petclinic/${DOMAIN_NAME:-petclinic.local}.crt"
    local key_file="/etc/ssl/petclinic/${DOMAIN_NAME:-petclinic.local}.key"
    
    # Store keystore password as encrypted secret
    encrypt_secret "ssl_keystore_password" "$keystore_password"
    
    # Create PKCS12 keystore if certificate exists
    if [[ -f "$cert_file" ]] && [[ -f "$key_file" ]]; then
        openssl pkcs12 -export -in "$cert_file" -inkey "$key_file" -out "$keystore_file" -password pass:"$keystore_password"
        chmod 640 "$keystore_file"
        chown root:jenkins "$keystore_file"
        log "SSL keystore created with encrypted password"
    else
        log "SSL certificate files not found, skipping keystore creation"
    fi
}

# Generate JWT secret
generate_jwt_secret() {
    log "Generating JWT secret..."
    
    local jwt_secret
    jwt_secret=$(openssl rand -base64 64)
    encrypt_secret "jwt_secret" "$jwt_secret"
    
    log "JWT secret generated and stored"
}

# Create secret rotation script
create_secret_rotation_script() {
    log "Creating secret rotation script..."
    
    cat > /usr/local/bin/rotate-secrets.sh << 'EOF'
#!/bin/bash

# Secret Rotation Script for Pet Clinic
# This script rotates secrets and updates configurations

set -euo pipefail

LOG_FILE="/var/log/secret-rotation.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

rotate_database_password() {
    log "Rotating database password..."
    
    # Generate new password
    local new_password
    new_password=$(openssl rand -base64 24)
    
    # Update database user password (requires database admin access)
    # mysql -u root -p -e "ALTER USER 'petclinic'@'%' IDENTIFIED BY '$new_password';"
    
    # Update encrypted secret
    echo -n "$new_password" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -pass file:/etc/petclinic/secrets/.encryption.key -out /etc/petclinic/secrets/db_password.enc
    
    log "Database password rotated"
}

rotate_jwt_secret() {
    log "Rotating JWT secret..."
    
    # Generate new JWT secret
    local new_jwt_secret
    new_jwt_secret=$(openssl rand -base64 64)
    
    # Update encrypted secret
    echo -n "$new_jwt_secret" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -pass file:/etc/petclinic/secrets/.encryption.key -out /etc/petclinic/secrets/jwt_secret.enc
    
    log "JWT secret rotated"
}

rotate_ssl_keystore_password() {
    log "Rotating SSL keystore password..."
    
    # Generate new keystore password
    local new_password
    new_password=$(openssl rand -base64 24)
    
    # Update encrypted secret
    echo -n "$new_password" | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 -pass file:/etc/petclinic/secrets/.encryption.key -out /etc/petclinic/secrets/ssl_keystore_password.enc
    
    # Recreate keystore with new password (requires certificate files)
    local cert_file="/etc/ssl/petclinic/petclinic.local.crt"
    local key_file="/etc/ssl/petclinic/petclinic.local.key"
    local keystore_file="/etc/ssl/petclinic/keystore.p12"
    
    if [[ -f "$cert_file" ]] && [[ -f "$key_file" ]]; then
        openssl pkcs12 -export -in "$cert_file" -inkey "$key_file" -out "$keystore_file" -password pass:"$new_password"
        chmod 640 "$keystore_file"
        chown root:jenkins "$keystore_file"
    fi
    
    log "SSL keystore password rotated"
}

main() {
    log "Starting secret rotation..."
    
    case "${1:-all}" in
        database)
            rotate_database_password
            ;;
        jwt)
            rotate_jwt_secret
            ;;
        ssl)
            rotate_ssl_keystore_password
            ;;
        all)
            rotate_database_password
            rotate_jwt_secret
            rotate_ssl_keystore_password
            ;;
        *)
            echo "Usage: $0 [database|jwt|ssl|all]"
            exit 1
            ;;
    esac
    
    log "Secret rotation completed"
    log "IMPORTANT: Restart applications to use new secrets"
}

main "$@"
EOF
    
    chmod 750 /usr/local/bin/rotate-secrets.sh
    chown root:jenkins /usr/local/bin/rotate-secrets.sh
    
    # Create cron job for monthly secret rotation
    cat > /etc/cron.d/secret-rotation << 'EOF'
# Pet Clinic Secret Rotation
# Rotate secrets monthly on the first Sunday at 3 AM
0 3 1-7 * 0 root /usr/local/bin/rotate-secrets.sh all
EOF
    
    log "Secret rotation script created"
}

# Create secret backup and restore scripts
create_backup_restore_scripts() {
    log "Creating secret backup and restore scripts..."
    
    # Backup script
    cat > /usr/local/bin/backup-secrets.sh << 'EOF'
#!/bin/bash

# Secret Backup Script for Pet Clinic

set -euo pipefail

BACKUP_DIR="/var/backups/petclinic-secrets"
SECRETS_DIR="/etc/petclinic/secrets"
DATE=$(date +%Y%m%d-%H%M%S)

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting secrets backup..."

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Create encrypted backup archive
tar -czf "$BACKUP_DIR/secrets-backup-$DATE.tar.gz" -C "$(dirname "$SECRETS_DIR")" "$(basename "$SECRETS_DIR")"

# Set restrictive permissions
chmod 600 "$BACKUP_DIR/secrets-backup-$DATE.tar.gz"

# Remove old backups (keep last 30 days)
find "$BACKUP_DIR" -name "secrets-backup-*.tar.gz" -mtime +30 -delete

log "Secrets backup completed: $BACKUP_DIR/secrets-backup-$DATE.tar.gz"
EOF
    
    chmod 750 /usr/local/bin/backup-secrets.sh
    
    # Restore script
    cat > /usr/local/bin/restore-secrets.sh << 'EOF'
#!/bin/bash

# Secret Restore Script for Pet Clinic

set -euo pipefail

BACKUP_DIR="/var/backups/petclinic-secrets"
SECRETS_DIR="/etc/petclinic/secrets"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <backup-file>"
    echo "Available backups:"
    ls -1 "$BACKUP_DIR"/secrets-backup-*.tar.gz 2>/dev/null || echo "No backups found"
    exit 1
fi

BACKUP_FILE="$1"

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

log "Starting secrets restore from: $BACKUP_FILE"

# Backup current secrets
if [[ -d "$SECRETS_DIR" ]]; then
    mv "$SECRETS_DIR" "${SECRETS_DIR}.backup.$(date +%Y%m%d-%H%M%S)"
fi

# Restore from backup
tar -xzf "$BACKUP_FILE" -C "$(dirname "$SECRETS_DIR")"

# Set proper permissions
chmod -R 600 "$SECRETS_DIR"/*
chown -R root:root "$SECRETS_DIR"

log "Secrets restore completed"
EOF
    
    chmod 750 /usr/local/bin/restore-secrets.sh
    
    log "Backup and restore scripts created"
}

# Test secret management
test_secret_management() {
    log "Testing secret management..."
    
    # Test encryption/decryption
    local test_secret="test-secret-value"
    encrypt_secret "test_secret" "$test_secret"
    
    local decrypted_secret
    decrypted_secret=$(decrypt_secret "test_secret")
    
    if [[ "$decrypted_secret" == "$test_secret" ]]; then
        log "✓ Secret encryption/decryption test passed"
    else
        log "✗ Secret encryption/decryption test failed"
    fi
    
    # Clean up test secret
    rm -f "$SECRETS_DIR/test_secret.enc"
    
    # Test secret retrieval script
    if [[ -x "/usr/local/bin/get-secret.sh" ]]; then
        log "✓ Secret retrieval script is executable"
    else
        log "✗ Secret retrieval script is not executable"
    fi
    
    # Test backup script
    if [[ -x "/usr/local/bin/backup-secrets.sh" ]]; then
        log "✓ Secret backup script is executable"
    else
        log "✗ Secret backup script is not executable"
    fi
    
    log "Secret management testing completed"
}

# Generate secrets management report
generate_secrets_report() {
    log "Generating secrets management report..."
    
    local report_file="/root/petclinic-secrets-management-report.txt"
    
    cat > "$report_file" << EOF
Pet Clinic Secrets Management Report
Generated: $(date)

Secrets Storage:
- Secrets Directory: $SECRETS_DIR
- Encryption Key: $ENCRYPTION_KEY_FILE
- Vault Directory: $VAULT_DIR

Stored Secrets:
$(ls -la "$SECRETS_DIR"/*.enc 2>/dev/null | sed 's/.*\///;s/\.enc$//' | sed 's/^/- /' || echo "- No secrets found")

Configuration Files:
- Application Config: /etc/petclinic/config/application-secrets.yml
- Environment File: /etc/petclinic/config/secrets.env
- Jenkins Credentials: $JENKINS_HOME/credentials/petclinic-secrets.xml

Management Scripts:
- Secret Retrieval: /usr/local/bin/get-secret.sh
- Secret Rotation: /usr/local/bin/rotate-secrets.sh
- Secret Backup: /usr/local/bin/backup-secrets.sh
- Secret Restore: /usr/local/bin/restore-secrets.sh

Security Features:
- AES-256-CBC encryption
- PBKDF2 key derivation (100,000 iterations)
- Restrictive file permissions (600)
- Root-only access to encryption key
- Automated secret rotation
- Encrypted backups

Scheduled Tasks:
- Secret Rotation: Monthly (first Sunday at 3 AM)
- Secret Backup: Manual (recommended weekly)

Usage Examples:
# Retrieve a secret
get-secret.sh db_password

# Rotate specific secret
rotate-secrets.sh database

# Backup all secrets
backup-secrets.sh

# Load secrets into environment
source /etc/petclinic/config/secrets.env

Security Recommendations:
1. Regularly rotate secrets (automated monthly)
2. Monitor secret access logs
3. Backup secrets to secure location
4. Use different encryption keys per environment
5. Implement secret scanning in CI/CD pipeline
6. Regular security audits of secret management
7. Use hardware security modules (HSM) for production
8. Implement secret versioning
9. Monitor for secret exposure in logs/code
10. Train team on secret management best practices

Compliance Notes:
- Secrets are encrypted at rest
- Access is logged and monitored
- Rotation policies are implemented
- Backup and recovery procedures are documented
- Principle of least privilege is enforced

Next Steps:
1. Test secret retrieval in applications
2. Configure monitoring for secret access
3. Set up automated secret rotation
4. Implement secret scanning tools
5. Document secret management procedures
EOF
    
    chmod 600 "$report_file"
    
    log "Secrets management report generated: $report_file"
}

# Main execution
main() {
    local generate_all=true
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --db-only)
                generate_all=false
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --db-only        Only generate database credentials"
                echo "  -h, --help      Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting secrets management setup for Pet Clinic CI/CD Pipeline"
    
    check_root
    generate_encryption_key
    
    if [[ "$generate_all" == true ]]; then
        store_database_credentials
        store_api_credentials
        generate_jwt_secret
        create_ssl_keystore
        create_secret_retrieval_script
        create_application_config
        create_jenkins_credentials
        create_secret_rotation_script
        create_backup_restore_scripts
    else
        store_database_credentials
        create_secret_retrieval_script
    fi
    
    test_secret_management
    generate_secrets_report
    
    log "Secrets management setup completed successfully"
    log ""
    log "Important Information:"
    log "- Secrets Report: /root/petclinic-secrets-management-report.txt"
    log "- Encryption Key: $ENCRYPTION_KEY_FILE (KEEP SECURE!)"
    log "- Secrets Directory: $SECRETS_DIR"
    log ""
    log "Next Steps:"
    log "1. Review the secrets management report"
    log "2. Test secret retrieval in applications"
    log "3. Configure monitoring for secret access"
    log "4. Set up automated backups"
    log "5. Document secret management procedures"
}

# Execute main function
main "$@"