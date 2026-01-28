#!/bin/bash

# SSL Certificate Setup Script for Pet Clinic CI/CD Pipeline
# This script configures SSL/TLS certificates for HTTPS communication
# Requirements: 10.3, 10.4

set -euo pipefail

# Configuration
DOMAIN_NAME="${DOMAIN_NAME:-petclinic.local}"
CERT_DIR="/etc/ssl/petclinic"
NGINX_CONF_DIR="/etc/nginx/sites-available"
NGINX_ENABLED_DIR="/etc/nginx/sites-enabled"
ACME_DIR="/var/www/html/.well-known/acme-challenge"
LOG_FILE="/var/log/ssl-setup.log"

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

# Install required packages
install_dependencies() {
    log "Installing SSL certificate dependencies..."
    
    # Update package list
    apt-get update
    
    # Install required packages
    apt-get install -y \
        nginx \
        certbot \
        python3-certbot-nginx \
        openssl \
        curl \
        wget
    
    log "Dependencies installed successfully"
}

# Create certificate directories
create_cert_directories() {
    log "Creating certificate directories..."
    
    mkdir -p "$CERT_DIR"
    mkdir -p "$ACME_DIR"
    mkdir -p "$NGINX_CONF_DIR"
    mkdir -p "$NGINX_ENABLED_DIR"
    
    # Set proper permissions
    chmod 755 "$CERT_DIR"
    chmod 755 "$ACME_DIR"
    
    log "Certificate directories created"
}

# Generate self-signed certificate for development/testing
generate_self_signed_cert() {
    log "Generating self-signed certificate for $DOMAIN_NAME..."
    
    local cert_file="$CERT_DIR/$DOMAIN_NAME.crt"
    local key_file="$CERT_DIR/$DOMAIN_NAME.key"
    local csr_file="$CERT_DIR/$DOMAIN_NAME.csr"
    
    # Generate private key
    openssl genrsa -out "$key_file" 2048
    
    # Generate certificate signing request
    openssl req -new -key "$key_file" -out "$csr_file" -subj "/C=US/ST=State/L=City/O=PetClinic/OU=IT/CN=$DOMAIN_NAME"
    
    # Generate self-signed certificate
    openssl x509 -req -days 365 -in "$csr_file" -signkey "$key_file" -out "$cert_file"
    
    # Set proper permissions
    chmod 600 "$key_file"
    chmod 644 "$cert_file"
    
    # Clean up CSR file
    rm -f "$csr_file"
    
    log "Self-signed certificate generated: $cert_file"
}

# Obtain Let's Encrypt certificate
obtain_letsencrypt_cert() {
    log "Obtaining Let's Encrypt certificate for $DOMAIN_NAME..."
    
    # Check if domain is publicly accessible
    if ! curl -s --max-time 10 "http://$DOMAIN_NAME" &>/dev/null; then
        log "WARNING: Domain $DOMAIN_NAME is not publicly accessible, using self-signed certificate"
        generate_self_signed_cert
        return
    fi
    
    # Stop nginx temporarily
    systemctl stop nginx || true
    
    # Obtain certificate
    certbot certonly \
        --standalone \
        --non-interactive \
        --agree-tos \
        --email "admin@$DOMAIN_NAME" \
        --domains "$DOMAIN_NAME" \
        --cert-path "$CERT_DIR/$DOMAIN_NAME.crt" \
        --key-path "$CERT_DIR/$DOMAIN_NAME.key"
    
    if [[ $? -eq 0 ]]; then
        log "Let's Encrypt certificate obtained successfully"
        
        # Copy certificates to our directory
        cp "/etc/letsencrypt/live/$DOMAIN_NAME/fullchain.pem" "$CERT_DIR/$DOMAIN_NAME.crt"
        cp "/etc/letsencrypt/live/$DOMAIN_NAME/privkey.pem" "$CERT_DIR/$DOMAIN_NAME.key"
        
        # Set proper permissions
        chmod 600 "$CERT_DIR/$DOMAIN_NAME.key"
        chmod 644 "$CERT_DIR/$DOMAIN_NAME.crt"
    else
        log "Failed to obtain Let's Encrypt certificate, falling back to self-signed"
        generate_self_signed_cert
    fi
}

# Configure Nginx for HTTPS
configure_nginx_https() {
    log "Configuring Nginx for HTTPS..."
    
    local nginx_conf="$NGINX_CONF_DIR/petclinic-https"
    
    # Create Nginx configuration for HTTPS
    cat > "$nginx_conf" << EOF
# Pet Clinic HTTPS Configuration
server {
    listen 80;
    server_name $DOMAIN_NAME;
    
    # Redirect HTTP to HTTPS
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name $DOMAIN_NAME;
    
    # SSL Configuration
    ssl_certificate $CERT_DIR/$DOMAIN_NAME.crt;
    ssl_certificate_key $CERT_DIR/$DOMAIN_NAME.key;
    
    # SSL Security Settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_session_tickets off;
    
    # HSTS (HTTP Strict Transport Security)
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
    
    # Security Headers
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self';" always;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;
    
    # Frontend Application
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Port \$server_port;
        
        # Security headers for proxied content
        proxy_hide_header X-Powered-By;
        proxy_hide_header Server;
        
        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8081/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Port \$server_port;
        
        # CORS headers for API
        add_header Access-Control-Allow-Origin "https://$DOMAIN_NAME" always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With" always;
        
        # Handle preflight requests
        if (\$request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin "https://$DOMAIN_NAME";
            add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
            add_header Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With";
            add_header Access-Control-Max-Age 1728000;
            add_header Content-Type "text/plain; charset=utf-8";
            add_header Content-Length 0;
            return 204;
        }
    }
    
    # Jenkins (if accessible via web)
    location /jenkins/ {
        proxy_pass http://localhost:8080/jenkins/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # Jenkins-specific headers
        proxy_set_header X-Forwarded-Port \$server_port;
        proxy_set_header X-Forwarded-Host \$host;
        
        # Authentication required for Jenkins
        auth_basic "Jenkins Access";
        auth_basic_user_file /etc/nginx/.htpasswd;
    }
    
    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
    
    # Let's Encrypt challenge
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }
    
    # Security: Block access to sensitive files
    location ~ /\\.ht {
        deny all;
    }
    
    location ~ /\\.(git|svn) {
        deny all;
    }
    
    # Logging
    access_log /var/log/nginx/petclinic-access.log;
    error_log /var/log/nginx/petclinic-error.log;
}
EOF
    
    # Enable the site
    ln -sf "$nginx_conf" "$NGINX_ENABLED_DIR/"
    
    # Remove default site if it exists
    rm -f "$NGINX_ENABLED_DIR/default"
    
    log "Nginx HTTPS configuration created"
}

# Create HTTP basic auth for Jenkins
create_jenkins_auth() {
    log "Creating HTTP basic authentication for Jenkins..."
    
    local htpasswd_file="/etc/nginx/.htpasswd"
    local jenkins_user="${JENKINS_USER:-admin}"
    local jenkins_password="${JENKINS_PASSWORD:-$(openssl rand -base64 12)}"
    
    # Install htpasswd utility
    apt-get install -y apache2-utils
    
    # Create htpasswd file
    htpasswd -cb "$htpasswd_file" "$jenkins_user" "$jenkins_password"
    
    # Set proper permissions
    chmod 600 "$htpasswd_file"
    chown www-data:www-data "$htpasswd_file"
    
    log "Jenkins authentication created for user: $jenkins_user"
    log "Jenkins password: $jenkins_password"
    
    # Save credentials to secure location
    echo "Jenkins Web Access Credentials:" > /root/jenkins-web-credentials.txt
    echo "Username: $jenkins_user" >> /root/jenkins-web-credentials.txt
    echo "Password: $jenkins_password" >> /root/jenkins-web-credentials.txt
    chmod 600 /root/jenkins-web-credentials.txt
}

# Configure SSL certificate auto-renewal
configure_cert_renewal() {
    log "Configuring SSL certificate auto-renewal..."
    
    # Create renewal script
    cat > /usr/local/bin/renew-ssl-cert.sh << 'EOF'
#!/bin/bash

# SSL Certificate Renewal Script
LOG_FILE="/var/log/ssl-renewal.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "Starting SSL certificate renewal check..."

# Renew certificates
certbot renew --quiet --no-self-upgrade

if [[ $? -eq 0 ]]; then
    log "Certificate renewal check completed successfully"
    
    # Reload nginx if certificates were renewed
    if systemctl is-active --quiet nginx; then
        systemctl reload nginx
        log "Nginx reloaded with renewed certificates"
    fi
else
    log "Certificate renewal check failed"
fi
EOF
    
    chmod +x /usr/local/bin/renew-ssl-cert.sh
    
    # Create cron job for automatic renewal
    cat > /etc/cron.d/ssl-cert-renewal << 'EOF'
# SSL Certificate Auto-Renewal
# Runs twice daily at random times to avoid rate limiting
0 2,14 * * * root /usr/local/bin/renew-ssl-cert.sh
EOF
    
    log "SSL certificate auto-renewal configured"
}

# Configure firewall for HTTPS
configure_firewall() {
    log "Configuring firewall for HTTPS..."
    
    # Install and configure UFW
    apt-get install -y ufw
    
    # Reset UFW to defaults
    ufw --force reset
    
    # Default policies
    ufw default deny incoming
    ufw default allow outgoing
    
    # Allow SSH
    ufw allow ssh
    
    # Allow HTTP and HTTPS
    ufw allow 80/tcp
    ufw allow 443/tcp
    
    # Allow specific application ports (if needed)
    # ufw allow 8080/tcp  # Application port (internal only)
    # ufw allow 8081/tcp  # API port (internal only)
    
    # Enable UFW
    ufw --force enable
    
    log "Firewall configured for HTTPS"
}

# Test SSL configuration
test_ssl_configuration() {
    log "Testing SSL configuration..."
    
    # Test Nginx configuration
    nginx -t
    if [[ $? -ne 0 ]]; then
        error_exit "Nginx configuration test failed"
    fi
    
    # Restart Nginx
    systemctl restart nginx
    
    # Wait for Nginx to start
    sleep 5
    
    # Test HTTPS connection
    if curl -k -s --max-time 10 "https://$DOMAIN_NAME/health" | grep -q "healthy"; then
        log "✓ HTTPS health check passed"
    else
        log "✗ HTTPS health check failed"
    fi
    
    # Test HTTP to HTTPS redirect
    if curl -s --max-time 10 -I "http://$DOMAIN_NAME" | grep -q "301"; then
        log "✓ HTTP to HTTPS redirect working"
    else
        log "✗ HTTP to HTTPS redirect not working"
    fi
    
    # Test SSL certificate
    local cert_info
    cert_info=$(openssl s_client -connect "$DOMAIN_NAME:443" -servername "$DOMAIN_NAME" </dev/null 2>/dev/null | openssl x509 -noout -subject 2>/dev/null)
    
    if [[ -n "$cert_info" ]]; then
        log "✓ SSL certificate is valid: $cert_info"
    else
        log "✗ SSL certificate validation failed"
    fi
    
    log "SSL configuration testing completed"
}

# Generate security report
generate_security_report() {
    log "Generating security configuration report..."
    
    local report_file="/root/petclinic-security-report.txt"
    
    cat > "$report_file" << EOF
Pet Clinic Security Configuration Report
Generated: $(date)

SSL/TLS Configuration:
- Domain: $DOMAIN_NAME
- Certificate: $CERT_DIR/$DOMAIN_NAME.crt
- Private Key: $CERT_DIR/$DOMAIN_NAME.key
- Certificate Type: $(if [[ -f "/etc/letsencrypt/live/$DOMAIN_NAME/fullchain.pem" ]]; then echo "Let's Encrypt"; else echo "Self-Signed"; fi)

Nginx Configuration:
- HTTPS Port: 443
- HTTP Redirect: Enabled
- Security Headers: Enabled
- HSTS: Enabled
- OCSP Stapling: Enabled

Security Features:
- Firewall: $(ufw status | head -1)
- HTTP Basic Auth: Enabled for Jenkins
- SSL Protocols: TLSv1.2, TLSv1.3
- Strong Ciphers: Enabled
- Session Security: Configured

Auto-Renewal:
- Cron Job: Configured
- Renewal Script: /usr/local/bin/renew-ssl-cert.sh

Access Credentials:
- Jenkins Web Access: /root/jenkins-web-credentials.txt

Log Files:
- SSL Setup: $LOG_FILE
- SSL Renewal: /var/log/ssl-renewal.log
- Nginx Access: /var/log/nginx/petclinic-access.log
- Nginx Error: /var/log/nginx/petclinic-error.log

Security Recommendations:
1. Regularly update SSL certificates
2. Monitor security logs for suspicious activity
3. Keep Nginx and system packages updated
4. Review and update security headers periodically
5. Implement additional authentication for sensitive endpoints

Next Steps:
1. Update DNS records to point to this server
2. Test HTTPS access from external networks
3. Configure monitoring for SSL certificate expiration
4. Set up log monitoring and alerting
EOF
    
    chmod 600 "$report_file"
    
    log "Security report generated: $report_file"
}

# Main execution
main() {
    local use_letsencrypt=false
    local skip_firewall=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --domain)
                DOMAIN_NAME="$2"
                shift 2
                ;;
            --letsencrypt)
                use_letsencrypt=true
                shift
                ;;
            --skip-firewall)
                skip_firewall=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --domain DOMAIN      Set domain name (default: petclinic.local)"
                echo "  --letsencrypt        Use Let's Encrypt certificate"
                echo "  --skip-firewall      Skip firewall configuration"
                echo "  -h, --help          Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting SSL/TLS and security hardening setup"
    log "Domain: $DOMAIN_NAME"
    log "Let's Encrypt: $use_letsencrypt"
    
    check_root
    install_dependencies
    create_cert_directories
    
    if [[ "$use_letsencrypt" == true ]]; then
        obtain_letsencrypt_cert
    else
        generate_self_signed_cert
    fi
    
    configure_nginx_https
    create_jenkins_auth
    configure_cert_renewal
    
    if [[ "$skip_firewall" != true ]]; then
        configure_firewall
    fi
    
    test_ssl_configuration
    generate_security_report
    
    log "SSL/TLS and security hardening setup completed successfully"
    log ""
    log "Important Information:"
    log "- HTTPS URL: https://$DOMAIN_NAME"
    log "- Jenkins Web Access: https://$DOMAIN_NAME/jenkins/"
    log "- Jenkins Credentials: /root/jenkins-web-credentials.txt"
    log "- Security Report: /root/petclinic-security-report.txt"
    log ""
    log "Next Steps:"
    log "1. Update DNS records to point to this server"
    log "2. Test HTTPS access from external networks"
    log "3. Configure application to use HTTPS URLs"
    log "4. Set up monitoring for SSL certificate expiration"
}

# Execute main function
main "$@"