#!/bin/bash

# Jenkins Disaster Recovery Automation Script
# This script automates the complete disaster recovery process for Jenkins
# Requirements: 7.4

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESTORE_SCRIPT="$SCRIPT_DIR/s3-backup-restore.sh"
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
S3_BUCKET="${S3_BACKUP_BUCKET:-pet-clinic-jenkins-backups}"
AWS_REGION="${AWS_REGION:-us-east-1}"
LOG_FILE="/var/log/jenkins-disaster-recovery.log"
RECOVERY_PLAN_FILE="$SCRIPT_DIR/recovery-plan.json"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    send_notification "DISASTER_RECOVERY_FAILED" "$1"
    exit 1
}

# Send notification
send_notification() {
    local event_type="$1"
    local message="$2"
    
    # Send SNS notification if topic is configured
    if [[ -n "${SNS_TOPIC_ARN:-}" ]]; then
        aws sns publish \
            --topic-arn "$SNS_TOPIC_ARN" \
            --subject "Jenkins Disaster Recovery - $event_type" \
            --message "$message" \
            --region "$AWS_REGION" || log "Failed to send SNS notification"
    fi
    
    # Send Slack notification if webhook is configured
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"Jenkins Disaster Recovery - $event_type: $message\"}" \
            "$SLACK_WEBHOOK_URL" || log "Failed to send Slack notification"
    fi
}

# Check system prerequisites
check_system_prerequisites() {
    log "Checking system prerequisites for disaster recovery..."
    
    # Check if running as root or with sudo
    if [[ $EUID -ne 0 ]] && ! sudo -n true 2>/dev/null; then
        error_exit "This script requires root privileges or passwordless sudo"
    fi
    
    # Check if AWS CLI is installed and configured
    if ! command -v aws &> /dev/null; then
        error_exit "AWS CLI is not installed"
    fi
    
    if ! aws sts get-caller-identity &> /dev/null; then
        error_exit "AWS credentials not configured or invalid"
    fi
    
    # Check if restore script exists
    if [[ ! -f "$RESTORE_SCRIPT" ]]; then
        error_exit "Restore script not found: $RESTORE_SCRIPT"
    fi
    
    # Check if Jenkins is installed
    if ! command -v jenkins &> /dev/null && ! systemctl list-unit-files | grep -q jenkins; then
        log "Warning: Jenkins not found, will need to install during recovery"
    fi
    
    log "System prerequisites check completed"
}

# Install Jenkins if not present
install_jenkins() {
    log "Installing Jenkins..."
    
    # Update system packages
    sudo apt-get update -y || sudo yum update -y || error_exit "Failed to update system packages"
    
    # Install Java if not present
    if ! command -v java &> /dev/null; then
        log "Installing Java..."
        sudo apt-get install -y openjdk-11-jdk || sudo yum install -y java-11-openjdk-devel || error_exit "Failed to install Java"
    fi
    
    # Install Jenkins
    if command -v apt-get &> /dev/null; then
        # Ubuntu/Debian
        wget -q -O - https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo apt-key add -
        sudo sh -c 'echo deb https://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
        sudo apt-get update -y
        sudo apt-get install -y jenkins
    elif command -v yum &> /dev/null; then
        # RHEL/CentOS/Amazon Linux
        sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
        sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
        sudo yum install -y jenkins
    else
        error_exit "Unsupported operating system for automatic Jenkins installation"
    fi
    
    # Create Jenkins user and directories
    sudo mkdir -p "$JENKINS_HOME"
    sudo chown -R jenkins:jenkins "$JENKINS_HOME"
    
    log "Jenkins installation completed"
}

# Verify Jenkins installation
verify_jenkins_installation() {
    log "Verifying Jenkins installation..."
    
    # Check if Jenkins service exists
    if ! systemctl list-unit-files | grep -q jenkins; then
        error_exit "Jenkins service not found after installation"
    fi
    
    # Check if Jenkins home directory exists
    if [[ ! -d "$JENKINS_HOME" ]]; then
        error_exit "Jenkins home directory not found: $JENKINS_HOME"
    fi
    
    # Check if Jenkins user exists
    if ! id jenkins &> /dev/null; then
        error_exit "Jenkins user not found"
    fi
    
    log "Jenkins installation verification completed"
}

# Create recovery plan
create_recovery_plan() {
    log "Creating disaster recovery plan..."
    
    local recovery_plan='{
        "recovery_steps": [
            {
                "step": 1,
                "description": "System prerequisites check",
                "status": "pending",
                "timestamp": null
            },
            {
                "step": 2,
                "description": "Jenkins installation verification",
                "status": "pending",
                "timestamp": null
            },
            {
                "step": 3,
                "description": "Backup restoration from S3",
                "status": "pending",
                "timestamp": null
            },
            {
                "step": 4,
                "description": "Jenkins service startup",
                "status": "pending",
                "timestamp": null
            },
            {
                "step": 5,
                "description": "Post-recovery validation",
                "status": "pending",
                "timestamp": null
            }
        ],
        "recovery_start_time": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
        "recovery_end_time": null,
        "recovery_status": "in_progress"
    }'
    
    echo "$recovery_plan" > "$RECOVERY_PLAN_FILE"
    log "Recovery plan created: $RECOVERY_PLAN_FILE"
}

# Update recovery plan step
update_recovery_step() {
    local step_number="$1"
    local status="$2"
    
    if [[ -f "$RECOVERY_PLAN_FILE" ]]; then
        python3 -c "
import json
import sys
from datetime import datetime

with open('$RECOVERY_PLAN_FILE', 'r') as f:
    plan = json.load(f)

for step in plan['recovery_steps']:
    if step['step'] == $step_number:
        step['status'] = '$status'
        step['timestamp'] = datetime.utcnow().isoformat() + 'Z'
        break

with open('$RECOVERY_PLAN_FILE', 'w') as f:
    json.dump(plan, f, indent=2)
" || log "Failed to update recovery plan"
    fi
}

# Complete recovery plan
complete_recovery_plan() {
    local final_status="$1"
    
    if [[ -f "$RECOVERY_PLAN_FILE" ]]; then
        python3 -c "
import json
from datetime import datetime

with open('$RECOVERY_PLAN_FILE', 'r') as f:
    plan = json.load(f)

plan['recovery_status'] = '$final_status'
plan['recovery_end_time'] = datetime.utcnow().isoformat() + 'Z'

with open('$RECOVERY_PLAN_FILE', 'w') as f:
    json.dump(plan, f, indent=2)
" || log "Failed to complete recovery plan"
    fi
}

# Validate recovered Jenkins
validate_recovery() {
    log "Validating Jenkins recovery..."
    
    # Wait for Jenkins to start
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if systemctl is-active --quiet jenkins; then
            log "Jenkins service is running"
            break
        fi
        
        log "Waiting for Jenkins to start (attempt $attempt/$max_attempts)..."
        sleep 10
        ((attempt++))
    done
    
    if [[ $attempt -gt $max_attempts ]]; then
        error_exit "Jenkins failed to start after recovery"
    fi
    
    # Wait for Jenkins web interface to be available
    attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|403"; then
            log "Jenkins web interface is accessible"
            break
        fi
        
        log "Waiting for Jenkins web interface (attempt $attempt/$max_attempts)..."
        sleep 10
        ((attempt++))
    done
    
    if [[ $attempt -gt $max_attempts ]]; then
        error_exit "Jenkins web interface not accessible after recovery"
    fi
    
    # Check if jobs were restored
    local job_count
    job_count=$(find "$JENKINS_HOME/jobs" -maxdepth 1 -type d | wc -l)
    log "Found $job_count job directories in Jenkins home"
    
    # Check if users were restored
    local user_count
    user_count=$(find "$JENKINS_HOME/users" -maxdepth 1 -type d | wc -l)
    log "Found $user_count user directories in Jenkins home"
    
    log "Jenkins recovery validation completed successfully"
}

# Generate recovery report
generate_recovery_report() {
    log "Generating disaster recovery report..."
    
    local report_file="/tmp/jenkins-recovery-report-$(date +%Y%m%d-%H%M%S).json"
    
    local report='{
        "recovery_timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
        "jenkins_home": "'$JENKINS_HOME'",
        "s3_bucket": "'$S3_BUCKET'",
        "aws_region": "'$AWS_REGION'",
        "system_info": {
            "hostname": "'$(hostname)'",
            "os": "'$(uname -s)'",
            "kernel": "'$(uname -r)'",
            "architecture": "'$(uname -m)'"
        },
        "jenkins_info": {
            "service_status": "'$(systemctl is-active jenkins 2>/dev/null || echo 'unknown')'",
            "home_directory_size": "'$(du -sh "$JENKINS_HOME" 2>/dev/null | cut -f1 || echo 'unknown')'",
            "job_count": '$(find "$JENKINS_HOME/jobs" -maxdepth 1 -type d 2>/dev/null | wc -l || echo 0)',
            "user_count": '$(find "$JENKINS_HOME/users" -maxdepth 1 -type d 2>/dev/null | wc -l || echo 0)'
        }
    }'
    
    echo "$report" > "$report_file"
    log "Recovery report generated: $report_file"
    
    # Upload report to S3 if possible
    if aws s3 cp "$report_file" "s3://$S3_BUCKET/recovery-reports/" --region "$AWS_REGION" 2>/dev/null; then
        log "Recovery report uploaded to S3"
    else
        log "Failed to upload recovery report to S3"
    fi
}

# Main disaster recovery process
main() {
    local backup_key=""
    local skip_validation=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -b|--backup)
                backup_key="$2"
                shift 2
                ;;
            --skip-validation)
                skip_validation=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  -b, --backup KEY     Restore specific backup by S3 key"
                echo "  --skip-validation    Skip post-recovery validation"
                echo "  -h, --help          Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting Jenkins disaster recovery process"
    send_notification "DISASTER_RECOVERY_STARTED" "Jenkins disaster recovery process initiated"
    
    # Create recovery plan
    create_recovery_plan
    
    # Step 1: Check system prerequisites
    update_recovery_step 1 "in_progress"
    check_system_prerequisites
    update_recovery_step 1 "completed"
    
    # Step 2: Verify or install Jenkins
    update_recovery_step 2 "in_progress"
    if ! systemctl list-unit-files | grep -q jenkins; then
        install_jenkins
    fi
    verify_jenkins_installation
    update_recovery_step 2 "completed"
    
    # Step 3: Restore from backup
    update_recovery_step 3 "in_progress"
    if [[ -n "$backup_key" ]]; then
        log "Restoring from specific backup: $backup_key"
        if ! "$RESTORE_SCRIPT" --backup "$backup_key"; then
            update_recovery_step 3 "failed"
            error_exit "Failed to restore from backup: $backup_key"
        fi
    else
        log "Restoring from latest backup"
        if ! "$RESTORE_SCRIPT"; then
            update_recovery_step 3 "failed"
            error_exit "Failed to restore from latest backup"
        fi
    fi
    update_recovery_step 3 "completed"
    
    # Step 4: Start Jenkins service (already done by restore script)
    update_recovery_step 4 "completed"
    
    # Step 5: Validate recovery
    if [[ "$skip_validation" != true ]]; then
        update_recovery_step 5 "in_progress"
        validate_recovery
        update_recovery_step 5 "completed"
    else
        update_recovery_step 5 "skipped"
    fi
    
    # Complete recovery plan
    complete_recovery_plan "completed"
    
    # Generate recovery report
    generate_recovery_report
    
    log "Jenkins disaster recovery completed successfully"
    send_notification "DISASTER_RECOVERY_COMPLETED" "Jenkins disaster recovery completed successfully"
}

# Execute main function
main "$@"