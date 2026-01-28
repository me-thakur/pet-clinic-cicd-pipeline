#!/bin/bash

# Setup Monitoring and Alerting Script
# This script configures comprehensive monitoring and alerting for Pet Clinic CI/CD Pipeline
# Requirements: 9.4, 9.5

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${ENVIRONMENT:-production}"
AWS_REGION="${AWS_REGION:-us-east-1}"
ALERT_EMAIL="${ALERT_EMAIL:-admin@petclinic.local}"
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"
PAGERDUTY_INTEGRATION_KEY="${PAGERDUTY_INTEGRATION_KEY:-}"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if AWS CLI is installed and configured
    if ! command -v aws &> /dev/null; then
        error_exit "AWS CLI is not installed"
    fi
    
    if ! aws sts get-caller-identity &> /dev/null; then
        error_exit "AWS credentials not configured or invalid"
    fi
    
    # Check if running on EC2
    if ! curl -s --max-time 3 http://169.254.169.254/latest/meta-data/instance-id &>/dev/null; then
        log "WARNING: Not running on EC2, some features may not work"
    fi
    
    # Check required parameters
    if [[ -z "$ALERT_EMAIL" ]]; then
        error_exit "ALERT_EMAIL environment variable is required"
    fi
    
    log "Prerequisites check completed"
}

# Deploy SNS notifications
deploy_sns_notifications() {
    log "Deploying SNS notifications..."
    
    local stack_name="${ENVIRONMENT}-petclinic-sns-notifications"
    local template_file="$SCRIPT_DIR/sns-notifications.yaml"
    
    if [[ ! -f "$template_file" ]]; then
        error_exit "SNS notifications template not found: $template_file"
    fi
    
    local parameters=(
        "ParameterKey=Environment,ParameterValue=$ENVIRONMENT"
        "ParameterKey=AlertEmail,ParameterValue=$ALERT_EMAIL"
    )
    
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        parameters+=("ParameterKey=SlackWebhookUrl,ParameterValue=$SLACK_WEBHOOK_URL")
    fi
    
    if [[ -n "$PAGERDUTY_INTEGRATION_KEY" ]]; then
        parameters+=("ParameterKey=PagerDutyIntegrationKey,ParameterValue=$PAGERDUTY_INTEGRATION_KEY")
    fi
    
    # Deploy or update stack
    if aws cloudformation describe-stacks --stack-name "$stack_name" --region "$AWS_REGION" &>/dev/null; then
        log "Updating existing SNS notifications stack..."
        aws cloudformation update-stack \
            --stack-name "$stack_name" \
            --template-body "file://$template_file" \
            --parameters "${parameters[@]}" \
            --capabilities CAPABILITY_IAM \
            --region "$AWS_REGION"
    else
        log "Creating new SNS notifications stack..."
        aws cloudformation create-stack \
            --stack-name "$stack_name" \
            --template-body "file://$template_file" \
            --parameters "${parameters[@]}" \
            --capabilities CAPABILITY_IAM \
            --region "$AWS_REGION"
    fi
    
    # Wait for stack completion
    log "Waiting for SNS notifications stack to complete..."
    aws cloudformation wait stack-create-complete \
        --stack-name "$stack_name" \
        --region "$AWS_REGION" || \
    aws cloudformation wait stack-update-complete \
        --stack-name "$stack_name" \
        --region "$AWS_REGION"
    
    # Get SNS topic ARNs
    SNS_CRITICAL_TOPIC_ARN=$(aws cloudformation describe-stacks \
        --stack-name "$stack_name" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`CriticalAlertsTopicArn`].OutputValue' \
        --output text)
    
    log "SNS notifications deployed successfully"
    log "Critical alerts topic ARN: $SNS_CRITICAL_TOPIC_ARN"
}

# Deploy CloudWatch alarms
deploy_cloudwatch_alarms() {
    log "Deploying CloudWatch alarms..."
    
    local stack_name="${ENVIRONMENT}-petclinic-cloudwatch-alarms"
    local template_file="$SCRIPT_DIR/cloudwatch-alarms.yaml"
    
    if [[ ! -f "$template_file" ]]; then
        error_exit "CloudWatch alarms template not found: $template_file"
    fi
    
    # Get instance ID
    local instance_id
    if instance_id=$(curl -s --max-time 3 http://169.254.169.254/latest/meta-data/instance-id 2>/dev/null); then
        log "Using EC2 instance ID: $instance_id"
    else
        log "WARNING: Could not get EC2 instance ID, using placeholder"
        instance_id="i-placeholder"
    fi
    
    # Get RDS instance ID (assuming it follows naming convention)
    local rds_instance_id="${ENVIRONMENT}-petclinic-database"
    
    local parameters=(
        "ParameterKey=Environment,ParameterValue=$ENVIRONMENT"
        "ParameterKey=SNSTopicArn,ParameterValue=$SNS_CRITICAL_TOPIC_ARN"
        "ParameterKey=InstanceId,ParameterValue=$instance_id"
        "ParameterKey=RDSInstanceId,ParameterValue=$rds_instance_id"
    )
    
    # Deploy or update stack
    if aws cloudformation describe-stacks --stack-name "$stack_name" --region "$AWS_REGION" &>/dev/null; then
        log "Updating existing CloudWatch alarms stack..."
        aws cloudformation update-stack \
            --stack-name "$stack_name" \
            --template-body "file://$template_file" \
            --parameters "${parameters[@]}" \
            --region "$AWS_REGION"
    else
        log "Creating new CloudWatch alarms stack..."
        aws cloudformation create-stack \
            --stack-name "$stack_name" \
            --template-body "file://$template_file" \
            --parameters "${parameters[@]}" \
            --region "$AWS_REGION"
    fi
    
    # Wait for stack completion
    log "Waiting for CloudWatch alarms stack to complete..."
    aws cloudformation wait stack-create-complete \
        --stack-name "$stack_name" \
        --region "$AWS_REGION" || \
    aws cloudformation wait stack-update-complete \
        --stack-name "$stack_name" \
        --region "$AWS_REGION"
    
    log "CloudWatch alarms deployed successfully"
}

# Deploy CloudWatch dashboard
deploy_cloudwatch_dashboard() {
    log "Deploying CloudWatch dashboard..."
    
    local stack_name="${ENVIRONMENT}-petclinic-cloudwatch-dashboard"
    local template_file="$SCRIPT_DIR/cloudwatch-dashboard.yaml"
    
    if [[ ! -f "$template_file" ]]; then
        error_exit "CloudWatch dashboard template not found: $template_file"
    fi
    
    # Get instance ID
    local instance_id
    if instance_id=$(curl -s --max-time 3 http://169.254.169.254/latest/meta-data/instance-id 2>/dev/null); then
        log "Using EC2 instance ID: $instance_id"
    else
        log "WARNING: Could not get EC2 instance ID, using placeholder"
        instance_id="i-placeholder"
    fi
    
    # Get RDS instance ID
    local rds_instance_id="${ENVIRONMENT}-petclinic-database"
    
    local parameters=(
        "ParameterKey=Environment,ParameterValue=$ENVIRONMENT"
        "ParameterKey=InstanceId,ParameterValue=$instance_id"
        "ParameterKey=RDSInstanceId,ParameterValue=$rds_instance_id"
    )
    
    # Deploy or update stack
    if aws cloudformation describe-stacks --stack-name "$stack_name" --region "$AWS_REGION" &>/dev/null; then
        log "Updating existing CloudWatch dashboard stack..."
        aws cloudformation update-stack \
            --stack-name "$stack_name" \
            --template-body "file://$template_file" \
            --parameters "${parameters[@]}" \
            --region "$AWS_REGION"
    else
        log "Creating new CloudWatch dashboard stack..."
        aws cloudformation create-stack \
            --stack-name "$stack_name" \
            --template-body "file://$template_file" \
            --parameters "${parameters[@]}" \
            --region "$AWS_REGION"
    fi
    
    # Wait for stack completion
    log "Waiting for CloudWatch dashboard stack to complete..."
    aws cloudformation wait stack-create-complete \
        --stack-name "$stack_name" \
        --region "$AWS_REGION" || \
    aws cloudformation wait stack-update-complete \
        --stack-name "$stack_name" \
        --region "$AWS_REGION"
    
    # Get dashboard URL
    DASHBOARD_URL=$(aws cloudformation describe-stacks \
        --stack-name "$stack_name" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`DashboardUrl`].OutputValue' \
        --output text)
    
    log "CloudWatch dashboard deployed successfully"
    log "Dashboard URL: $DASHBOARD_URL"
}

# Install custom metrics publisher
install_custom_metrics_publisher() {
    log "Installing custom metrics publisher..."
    
    local metrics_script="$SCRIPT_DIR/custom-metrics-publisher.py"
    local install_dir="/opt/petclinic-monitoring"
    local service_file="/etc/systemd/system/petclinic-metrics.service"
    
    if [[ ! -f "$metrics_script" ]]; then
        error_exit "Custom metrics publisher script not found: $metrics_script"
    fi
    
    # Create installation directory
    sudo mkdir -p "$install_dir"
    
    # Copy script
    sudo cp "$metrics_script" "$install_dir/"
    sudo chmod +x "$install_dir/custom-metrics-publisher.py"
    
    # Install Python dependencies
    sudo pip3 install boto3 requests || log "WARNING: Failed to install Python dependencies"
    
    # Create systemd service
    sudo tee "$service_file" > /dev/null << EOF
[Unit]
Description=Pet Clinic Custom Metrics Publisher
After=network.target
Wants=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$install_dir
ExecStart=/usr/bin/python3 $install_dir/custom-metrics-publisher.py --daemon --environment $ENVIRONMENT --region $AWS_REGION
Restart=always
RestartSec=60
Environment=JENKINS_URL=http://localhost:8080
Environment=APP_URL=http://localhost:8080
Environment=AWS_DEFAULT_REGION=$AWS_REGION

[Install]
WantedBy=multi-user.target
EOF
    
    # Enable and start service
    sudo systemctl daemon-reload
    sudo systemctl enable petclinic-metrics.service
    sudo systemctl start petclinic-metrics.service
    
    log "Custom metrics publisher installed and started"
}

# Configure log-based metrics
configure_log_metrics() {
    log "Configuring log-based metrics..."
    
    # Create CloudWatch log metric filters
    local log_groups=(
        "/aws/petclinic/application"
        "/aws/petclinic/errors"
        "/aws/jenkins/application"
    )
    
    for log_group in "${log_groups[@]}"; do
        # Check if log group exists
        if aws logs describe-log-groups --log-group-name-prefix "$log_group" --region "$AWS_REGION" | grep -q "$log_group"; then
            log "Creating metric filters for log group: $log_group"
            
            # Error count metric filter
            aws logs put-metric-filter \
                --log-group-name "$log_group" \
                --filter-name "${log_group//\//-}-error-count" \
                --filter-pattern "ERROR" \
                --metric-transformations \
                    metricName=ErrorCount,metricNamespace=PetClinic/Logs,metricValue=1 \
                --region "$AWS_REGION" || log "WARNING: Failed to create error count filter for $log_group"
            
            # Warning count metric filter
            aws logs put-metric-filter \
                --log-group-name "$log_group" \
                --filter-name "${log_group//\//-}-warning-count" \
                --filter-pattern "WARN" \
                --metric-transformations \
                    metricName=WarningCount,metricNamespace=PetClinic/Logs,metricValue=1 \
                --region "$AWS_REGION" || log "WARNING: Failed to create warning count filter for $log_group"
        else
            log "WARNING: Log group $log_group does not exist, skipping metric filters"
        fi
    done
    
    log "Log-based metrics configured"
}

# Test monitoring setup
test_monitoring() {
    log "Testing monitoring setup..."
    
    # Test custom metrics publisher
    if systemctl is-active --quiet petclinic-metrics.service; then
        log "✓ Custom metrics publisher service is running"
    else
        log "✗ Custom metrics publisher service is not running"
    fi
    
    # Test CloudWatch alarms
    local alarm_count
    alarm_count=$(aws cloudwatch describe-alarms \
        --alarm-name-prefix "${ENVIRONMENT}-PetClinic" \
        --region "$AWS_REGION" \
        --query 'MetricAlarms | length(@)' \
        --output text)
    
    if [[ "$alarm_count" -gt 0 ]]; then
        log "✓ Found $alarm_count CloudWatch alarms"
    else
        log "✗ No CloudWatch alarms found"
    fi
    
    # Test SNS topic
    if aws sns get-topic-attributes --topic-arn "$SNS_CRITICAL_TOPIC_ARN" --region "$AWS_REGION" &>/dev/null; then
        log "✓ SNS critical alerts topic is accessible"
    else
        log "✗ SNS critical alerts topic is not accessible"
    fi
    
    # Send test metric
    log "Sending test metric..."
    python3 "$SCRIPT_DIR/custom-metrics-publisher.py" --environment "$ENVIRONMENT" --region "$AWS_REGION" || log "WARNING: Failed to send test metric"
    
    log "Monitoring setup test completed"
}

# Create monitoring documentation
create_documentation() {
    log "Creating monitoring documentation..."
    
    local doc_file="/opt/petclinic-monitoring/README.md"
    
    sudo tee "$doc_file" > /dev/null << EOF
# Pet Clinic Monitoring Setup

## Overview
This document describes the monitoring and alerting setup for the Pet Clinic CI/CD Pipeline.

## Components

### CloudWatch Alarms
- **Environment**: $ENVIRONMENT
- **Region**: $AWS_REGION
- **Total Alarms**: $(aws cloudwatch describe-alarms --alarm-name-prefix "${ENVIRONMENT}-PetClinic" --region "$AWS_REGION" --query 'MetricAlarms | length(@)' --output text 2>/dev/null || echo "Unknown")

### SNS Notifications
- **Critical Alerts Topic**: $SNS_CRITICAL_TOPIC_ARN
- **Alert Email**: $ALERT_EMAIL
- **Slack Integration**: $([ -n "$SLACK_WEBHOOK_URL" ] && echo "Enabled" || echo "Disabled")
- **PagerDuty Integration**: $([ -n "$PAGERDUTY_INTEGRATION_KEY" ] && echo "Enabled" || echo "Disabled")

### CloudWatch Dashboard
- **Dashboard URL**: $DASHBOARD_URL

### Custom Metrics
- **Service**: petclinic-metrics.service
- **Status**: $(systemctl is-active petclinic-metrics.service 2>/dev/null || echo "Unknown")
- **Log File**: /var/log/petclinic-metrics.log

## Maintenance

### Restart Custom Metrics Service
\`\`\`bash
sudo systemctl restart petclinic-metrics.service
\`\`\`

### View Service Logs
\`\`\`bash
sudo journalctl -u petclinic-metrics.service -f
\`\`\`

### Test Monitoring
\`\`\`bash
$SCRIPT_DIR/setup-monitoring.sh --test-only
\`\`\`

## Troubleshooting

### Common Issues
1. **Custom metrics not appearing**: Check service status and logs
2. **Alarms not triggering**: Verify metric data is being published
3. **Notifications not received**: Check SNS topic subscriptions

### Support
- Check CloudWatch console for metrics and alarms
- Review service logs for errors
- Verify AWS credentials and permissions

Generated on: $(date)
EOF
    
    log "Monitoring documentation created: $doc_file"
}

# Main execution
main() {
    local test_only=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --test-only)
                test_only=true
                shift
                ;;
            --environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --region)
                AWS_REGION="$2"
                shift 2
                ;;
            --alert-email)
                ALERT_EMAIL="$2"
                shift 2
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --test-only          Only run monitoring tests"
                echo "  --environment ENV    Set environment name"
                echo "  --region REGION      Set AWS region"
                echo "  --alert-email EMAIL  Set alert email address"
                echo "  -h, --help          Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting monitoring and alerting setup for Pet Clinic CI/CD Pipeline"
    log "Environment: $ENVIRONMENT"
    log "AWS Region: $AWS_REGION"
    log "Alert Email: $ALERT_EMAIL"
    
    check_prerequisites
    
    if [[ "$test_only" == true ]]; then
        test_monitoring
        exit 0
    fi
    
    # Deploy monitoring components
    deploy_sns_notifications
    deploy_cloudwatch_alarms
    deploy_cloudwatch_dashboard
    install_custom_metrics_publisher
    configure_log_metrics
    
    # Test setup
    test_monitoring
    
    # Create documentation
    create_documentation
    
    log "Monitoring and alerting setup completed successfully"
    log ""
    log "Next steps:"
    log "1. Access the CloudWatch dashboard: $DASHBOARD_URL"
    log "2. Confirm email subscription for alerts"
    log "3. Test alert notifications"
    log "4. Review monitoring documentation: /opt/petclinic-monitoring/README.md"
}

# Execute main function
main "$@"