#!/bin/bash

# Setup Logging Configuration Script
# This script configures comprehensive logging for the Pet Clinic CI/CD Pipeline
# Requirements: 9.1, 9.2

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_BASE_DIR="/var/log"
PETCLINIC_LOG_DIR="$LOG_BASE_DIR/petclinic"
JENKINS_LOG_DIR="$LOG_BASE_DIR/jenkins"
RSYSLOG_CONF_DIR="/etc/rsyslog.d"
LOGROTATE_CONF_DIR="/etc/logrotate.d"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
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

# Create log directories
create_log_directories() {
    log "Creating log directories..."
    
    # Create Pet Clinic log directories
    mkdir -p "$PETCLINIC_LOG_DIR"
    mkdir -p "$JENKINS_LOG_DIR"
    
    # Set proper permissions
    chown -R jenkins:jenkins "$JENKINS_LOG_DIR" 2>/dev/null || log "Jenkins user not found, skipping ownership change"
    chmod 755 "$PETCLINIC_LOG_DIR"
    chmod 755 "$JENKINS_LOG_DIR"
    
    log "Log directories created successfully"
}

# Configure rsyslog
configure_rsyslog() {
    log "Configuring rsyslog..."
    
    # Copy rsyslog configuration
    if [[ -f "$SCRIPT_DIR/rsyslog-petclinic.conf" ]]; then
        cp "$SCRIPT_DIR/rsyslog-petclinic.conf" "$RSYSLOG_CONF_DIR/49-petclinic.conf"
        log "Rsyslog configuration installed"
    else
        error_exit "Rsyslog configuration file not found"
    fi
    
    # Restart rsyslog service
    systemctl restart rsyslog
    log "Rsyslog service restarted"
}

# Configure logrotate
configure_logrotate() {
    log "Configuring logrotate..."
    
    # Create logrotate configuration for Pet Clinic logs
    cat > "$LOGROTATE_CONF_DIR/petclinic" << 'EOF'
/var/log/petclinic/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        /bin/kill -HUP `cat /var/run/rsyslogd.pid 2> /dev/null` 2> /dev/null || true
    endscript
}

/var/log/petclinic/security.log {
    daily
    missingok
    rotate 90
    compress
    delaycompress
    notifempty
    create 600 root root
    postrotate
        /bin/kill -HUP `cat /var/run/rsyslogd.pid 2> /dev/null` 2> /dev/null || true
    endscript
}

/var/log/petclinic/error.log {
    daily
    missingok
    rotate 60
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        /bin/kill -HUP `cat /var/run/rsyslogd.pid 2> /dev/null` 2> /dev/null || true
    endscript
}
EOF

    # Create logrotate configuration for Jenkins logs
    cat > "$LOGROTATE_CONF_DIR/jenkins-custom" << 'EOF'
/var/log/jenkins/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 jenkins jenkins
    postrotate
        /bin/kill -HUP `cat /var/run/rsyslogd.pid 2> /dev/null` 2> /dev/null || true
    endscript
}

/var/log/jenkins/jenkins-backup-*.log {
    daily
    missingok
    rotate 60
    compress
    delaycompress
    notifempty
    create 644 jenkins jenkins
}

/var/log/jenkins/jenkins-disaster-recovery.log {
    daily
    missingok
    rotate 90
    compress
    delaycompress
    notifempty
    create 644 jenkins jenkins
}
EOF

    log "Logrotate configuration installed"
}

# Install CloudWatch Logs agent
install_cloudwatch_logs() {
    log "Installing CloudWatch Logs agent..."
    
    # Check if running on AWS EC2
    if curl -s --max-time 3 http://169.254.169.254/latest/meta-data/instance-id &>/dev/null; then
        log "Running on AWS EC2, installing CloudWatch agent"
        
        # Download and install CloudWatch agent
        if ! command -v amazon-cloudwatch-agent-ctl &> /dev/null; then
            wget -q https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
            rpm -U ./amazon-cloudwatch-agent.rpm || yum install -y ./amazon-cloudwatch-agent.rpm
            rm -f ./amazon-cloudwatch-agent.rpm
        fi
        
        # Configure CloudWatch agent
        if [[ -f "$SCRIPT_DIR/cloudwatch-logs-config.json" ]]; then
            cp "$SCRIPT_DIR/cloudwatch-logs-config.json" /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
            
            # Start CloudWatch agent
            /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
                -a fetch-config \
                -m ec2 \
                -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
                -s
            
            log "CloudWatch Logs agent configured and started"
        else
            log "CloudWatch configuration file not found, skipping agent configuration"
        fi
    else
        log "Not running on AWS EC2, skipping CloudWatch agent installation"
    fi
}

# Configure Jenkins logging
configure_jenkins_logging() {
    log "Configuring Jenkins logging..."
    
    local jenkins_home="${JENKINS_HOME:-/var/lib/jenkins}"
    
    if [[ -d "$jenkins_home" ]]; then
        # Copy Jenkins logging configuration
        if [[ -f "$SCRIPT_DIR/jenkins-logging.properties" ]]; then
            cp "$SCRIPT_DIR/jenkins-logging.properties" "$jenkins_home/logging.properties"
            chown jenkins:jenkins "$jenkins_home/logging.properties" 2>/dev/null || log "Jenkins user not found"
            log "Jenkins logging configuration installed"
        fi
        
        # Configure Jenkins to use the logging properties
        local jenkins_config="/etc/default/jenkins"
        if [[ -f "$jenkins_config" ]]; then
            if ! grep -q "java.util.logging.config.file" "$jenkins_config"; then
                echo 'JAVA_ARGS="$JAVA_ARGS -Djava.util.logging.config.file=$JENKINS_HOME/logging.properties"' >> "$jenkins_config"
                log "Jenkins logging configuration added to startup parameters"
            fi
        fi
    else
        log "Jenkins home directory not found, skipping Jenkins logging configuration"
    fi
}

# Configure application logging
configure_application_logging() {
    log "Configuring application logging..."
    
    # Copy logback configuration to application resources
    local app_resources_dir="/opt/petclinic/config"
    mkdir -p "$app_resources_dir"
    
    if [[ -f "$SCRIPT_DIR/logback-spring.xml" ]]; then
        cp "$SCRIPT_DIR/logback-spring.xml" "$app_resources_dir/"
        log "Application logging configuration installed"
    fi
    
    # Set environment variables for logging
    cat > /etc/environment << 'EOF'
LOG_PATH=/var/log/petclinic
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_PETCLINIC=DEBUG
LOGGING_FILE_NAME=/var/log/petclinic/application.log
EOF
    
    log "Application logging environment configured"
}

# Create log monitoring script
create_log_monitoring() {
    log "Creating log monitoring script..."
    
    cat > /usr/local/bin/petclinic-log-monitor.sh << 'EOF'
#!/bin/bash

# Pet Clinic Log Monitoring Script
# This script monitors log files for critical issues and sends alerts

LOG_DIRS=("/var/log/petclinic" "/var/log/jenkins")
ERROR_PATTERNS=("ERROR" "FATAL" "Exception" "OutOfMemoryError" "StackOverflowError")
SECURITY_PATTERNS=("authentication failed" "unauthorized access" "security violation")
ALERT_EMAIL="${ALERT_EMAIL:-admin@petclinic.local}"
ALERT_THRESHOLD=10

# Function to send alert
send_alert() {
    local subject="$1"
    local message="$2"
    
    # Send email alert if mail is configured
    if command -v mail &> /dev/null; then
        echo "$message" | mail -s "$subject" "$ALERT_EMAIL"
    fi
    
    # Send to syslog
    logger -p local0.crit "PETCLINIC_ALERT: $subject - $message"
    
    # Send SNS notification if configured
    if [[ -n "${SNS_TOPIC_ARN:-}" ]] && command -v aws &> /dev/null; then
        aws sns publish --topic-arn "$SNS_TOPIC_ARN" --subject "$subject" --message "$message"
    fi
}

# Monitor for errors
monitor_errors() {
    local error_count=0
    
    for log_dir in "${LOG_DIRS[@]}"; do
        if [[ -d "$log_dir" ]]; then
            for pattern in "${ERROR_PATTERNS[@]}"; do
                local count
                count=$(find "$log_dir" -name "*.log" -mmin -5 -exec grep -c "$pattern" {} + 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
                error_count=$((error_count + count))
            done
        fi
    done
    
    if [[ $error_count -gt $ALERT_THRESHOLD ]]; then
        send_alert "High Error Rate Detected" "Found $error_count errors in the last 5 minutes"
    fi
}

# Monitor for security issues
monitor_security() {
    for log_dir in "${LOG_DIRS[@]}"; do
        if [[ -d "$log_dir" ]]; then
            for pattern in "${SECURITY_PATTERNS[@]}"; do
                local matches
                matches=$(find "$log_dir" -name "*.log" -mmin -5 -exec grep -l "$pattern" {} + 2>/dev/null)
                if [[ -n "$matches" ]]; then
                    send_alert "Security Alert" "Security pattern '$pattern' detected in logs: $matches"
                fi
            done
        fi
    done
}

# Main monitoring loop
main() {
    monitor_errors
    monitor_security
}

main "$@"
EOF

    chmod +x /usr/local/bin/petclinic-log-monitor.sh
    
    # Create cron job for log monitoring
    cat > /etc/cron.d/petclinic-log-monitor << 'EOF'
# Pet Clinic Log Monitoring
*/5 * * * * root /usr/local/bin/petclinic-log-monitor.sh
EOF
    
    log "Log monitoring script created and scheduled"
}

# Test logging configuration
test_logging() {
    log "Testing logging configuration..."
    
    # Test application logging
    logger -p local0.info "PETCLINIC_TEST: Application logging test"
    
    # Test Jenkins logging
    logger -p local1.info "JENKINS_TEST: Jenkins logging test"
    
    # Test error logging
    logger -p local0.err "PETCLINIC_TEST: Error logging test"
    
    # Test security logging
    logger -p auth.info "SECURITY_TEST: Security logging test"
    
    # Wait a moment for logs to be written
    sleep 2
    
    # Check if logs were created
    local test_passed=true
    
    if [[ ! -f "$PETCLINIC_LOG_DIR/application.log" ]] && [[ ! -f "/var/log/syslog" ]]; then
        log "WARNING: Application logs not found"
        test_passed=false
    fi
    
    if [[ ! -f "$PETCLINIC_LOG_DIR/security.log" ]] && [[ ! -f "/var/log/auth.log" ]]; then
        log "WARNING: Security logs not found"
        test_passed=false
    fi
    
    if [[ "$test_passed" == true ]]; then
        log "Logging configuration test passed"
    else
        log "WARNING: Some logging tests failed, check configuration"
    fi
}

# Main execution
main() {
    log "Starting logging configuration setup..."
    
    check_root
    create_log_directories
    configure_rsyslog
    configure_logrotate
    configure_jenkins_logging
    configure_application_logging
    install_cloudwatch_logs
    create_log_monitoring
    test_logging
    
    log "Logging configuration setup completed successfully"
    log "Log directories:"
    log "  - Pet Clinic logs: $PETCLINIC_LOG_DIR"
    log "  - Jenkins logs: $JENKINS_LOG_DIR"
    log "  - System logs: /var/log/syslog"
    log ""
    log "To view logs in real-time:"
    log "  tail -f $PETCLINIC_LOG_DIR/application.log"
    log "  tail -f $JENKINS_LOG_DIR/jenkins.log"
    log "  tail -f /var/log/syslog"
}

# Execute main function
main "$@"