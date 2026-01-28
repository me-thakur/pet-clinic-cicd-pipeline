#!/bin/bash

# S3 Backup Restore Script for Jenkins
# This script downloads and restores Jenkins backups from S3 bucket
# Requirements: 7.4

set -euo pipefail

# Configuration
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
BACKUP_DIR="${JENKINS_HOME}/backup"
S3_BUCKET="${S3_BACKUP_BUCKET:-pet-clinic-jenkins-backups}"
AWS_REGION="${AWS_REGION:-us-east-1}"
LOG_FILE="/var/log/jenkins-backup-restore.log"
TEMP_DIR="/tmp/jenkins-restore"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    cleanup_temp_files
    exit 1
}

# Cleanup temporary files
cleanup_temp_files() {
    if [[ -d "$TEMP_DIR" ]]; then
        rm -rf "$TEMP_DIR"
        log "Cleaned up temporary files"
    fi
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if AWS CLI is installed
    if ! command -v aws &> /dev/null; then
        error_exit "AWS CLI is not installed"
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        error_exit "AWS credentials not configured or invalid"
    fi
    
    # Check if S3 bucket exists
    if ! aws s3 ls "s3://$S3_BUCKET" &> /dev/null; then
        error_exit "S3 bucket does not exist or is not accessible: $S3_BUCKET"
    fi
    
    # Create temporary directory
    mkdir -p "$TEMP_DIR"
    
    log "Prerequisites check passed"
}

# List available backups in S3
list_available_backups() {
    log "Listing available backups in S3..."
    
    aws s3 ls "s3://$S3_BUCKET/jenkins-backups/" --recursive --region "$AWS_REGION" | \
        grep "\.zip$" | \
        sort -k1,2 -r | \
        head -20
}

# Download backup from S3
download_backup() {
    local s3_key="$1"
    local local_file="$2"
    
    log "Downloading backup from s3://$S3_BUCKET/$s3_key"
    
    if aws s3 cp "s3://$S3_BUCKET/$s3_key" "$local_file" --region "$AWS_REGION"; then
        log "Successfully downloaded backup to $local_file"
        return 0
    else
        log "Failed to download backup from S3"
        return 1
    fi
}

# Verify backup integrity
verify_backup_integrity() {
    local backup_file="$1"
    local s3_key="$2"
    
    log "Verifying backup integrity..."
    
    # Get expected checksum from S3 metadata
    local expected_checksum
    expected_checksum=$(aws s3api head-object --bucket "$S3_BUCKET" --key "$s3_key" --region "$AWS_REGION" --query 'Metadata.checksum' --output text 2>/dev/null || echo "")
    
    if [[ -z "$expected_checksum" || "$expected_checksum" == "None" ]]; then
        log "Warning: No checksum metadata found for backup, skipping integrity check"
        return 0
    fi
    
    # Calculate actual checksum
    local actual_checksum
    actual_checksum=$(sha256sum "$backup_file" | cut -d' ' -f1)
    
    if [[ "$actual_checksum" == "$expected_checksum" ]]; then
        log "Backup integrity verification passed"
        return 0
    else
        log "Backup integrity verification failed (expected: $expected_checksum, actual: $actual_checksum)"
        return 1
    fi
}

# Stop Jenkins service
stop_jenkins() {
    log "Stopping Jenkins service..."
    
    if systemctl is-active --quiet jenkins; then
        sudo systemctl stop jenkins
        log "Jenkins service stopped"
    else
        log "Jenkins service is not running"
    fi
}

# Start Jenkins service
start_jenkins() {
    log "Starting Jenkins service..."
    
    sudo systemctl start jenkins
    
    # Wait for Jenkins to start
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if systemctl is-active --quiet jenkins; then
            log "Jenkins service started successfully"
            return 0
        fi
        
        log "Waiting for Jenkins to start (attempt $attempt/$max_attempts)..."
        sleep 10
        ((attempt++))
    done
    
    error_exit "Jenkins failed to start after $max_attempts attempts"
}

# Backup current Jenkins home
backup_current_jenkins() {
    local backup_name="jenkins-pre-restore-$(date +%Y%m%d-%H%M%S).tar.gz"
    local backup_path="/tmp/$backup_name"
    
    log "Creating backup of current Jenkins home..."
    
    if tar -czf "$backup_path" -C "$(dirname "$JENKINS_HOME")" "$(basename "$JENKINS_HOME")"; then
        log "Current Jenkins home backed up to $backup_path"
        echo "$backup_path"
    else
        error_exit "Failed to backup current Jenkins home"
    fi
}

# Restore Jenkins from backup
restore_jenkins() {
    local backup_file="$1"
    
    log "Restoring Jenkins from backup: $backup_file"
    
    # Create backup directory if it doesn't exist
    mkdir -p "$BACKUP_DIR"
    
    # Extract backup to temporary location
    local extract_dir="$TEMP_DIR/extract"
    mkdir -p "$extract_dir"
    
    if unzip -q "$backup_file" -d "$extract_dir"; then
        log "Backup extracted successfully"
    else
        error_exit "Failed to extract backup file"
    fi
    
    # Find the backup content (ThinBackup creates dated folders)
    local backup_content
    backup_content=$(find "$extract_dir" -type d -name "FULL-*" | head -1)
    
    if [[ -z "$backup_content" ]]; then
        error_exit "Could not find backup content in extracted files"
    fi
    
    log "Found backup content in: $backup_content"
    
    # Stop Jenkins before restore
    stop_jenkins
    
    # Backup current Jenkins home
    local current_backup
    current_backup=$(backup_current_jenkins)
    
    # Clear Jenkins home (except logs and workspace)
    log "Clearing Jenkins home directory..."
    find "$JENKINS_HOME" -mindepth 1 -maxdepth 1 \
        ! -name "logs" \
        ! -name "workspace" \
        ! -name "backup" \
        -exec rm -rf {} +
    
    # Restore from backup
    log "Restoring Jenkins configuration and data..."
    if cp -r "$backup_content"/* "$JENKINS_HOME/"; then
        log "Jenkins data restored successfully"
    else
        error_exit "Failed to restore Jenkins data"
    fi
    
    # Fix permissions
    sudo chown -R jenkins:jenkins "$JENKINS_HOME"
    sudo chmod -R 755 "$JENKINS_HOME"
    
    # Start Jenkins
    start_jenkins
    
    log "Jenkins restore completed successfully"
    log "Previous Jenkins home backed up to: $current_backup"
}

# Interactive backup selection
select_backup_interactively() {
    log "Available backups:"
    
    local backups
    mapfile -t backups < <(aws s3 ls "s3://$S3_BUCKET/jenkins-backups/" --recursive --region "$AWS_REGION" | \
        grep "\.zip$" | \
        sort -k1,2 -r | \
        head -20 | \
        awk '{print $4}')
    
    if [[ ${#backups[@]} -eq 0 ]]; then
        error_exit "No backups found in S3 bucket"
    fi
    
    local i=1
    for backup in "${backups[@]}"; do
        local backup_name
        backup_name=$(basename "$backup")
        local backup_date
        backup_date=$(echo "$backup" | grep -o '[0-9]\{4\}/[0-9]\{2\}/[0-9]\{2\}' || echo "unknown")
        echo "$i) $backup_name (Date: $backup_date)"
        ((i++))
    done
    
    echo -n "Select backup to restore (1-${#backups[@]}): "
    read -r selection
    
    if [[ "$selection" =~ ^[0-9]+$ ]] && [[ "$selection" -ge 1 ]] && [[ "$selection" -le ${#backups[@]} ]]; then
        echo "${backups[$((selection-1))]}"
    else
        error_exit "Invalid selection"
    fi
}

# Main execution
main() {
    local backup_key=""
    local interactive=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -b|--backup)
                backup_key="$2"
                shift 2
                ;;
            -i|--interactive)
                interactive=true
                shift
                ;;
            -l|--list)
                check_prerequisites
                list_available_backups
                exit 0
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  -b, --backup KEY     Restore specific backup by S3 key"
                echo "  -i, --interactive    Interactive backup selection"
                echo "  -l, --list          List available backups"
                echo "  -h, --help          Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting Jenkins backup restore from S3"
    
    # Check prerequisites
    check_prerequisites
    
    # Select backup
    if [[ "$interactive" == true ]]; then
        backup_key=$(select_backup_interactively)
    elif [[ -z "$backup_key" ]]; then
        # Use latest backup
        backup_key=$(aws s3 ls "s3://$S3_BUCKET/jenkins-backups/" --recursive --region "$AWS_REGION" | \
            grep "\.zip$" | \
            sort -k1,2 -r | \
            head -1 | \
            awk '{print $4}')
        
        if [[ -z "$backup_key" ]]; then
            error_exit "No backups found in S3 bucket"
        fi
        
        log "Using latest backup: $backup_key"
    fi
    
    # Download backup
    local backup_file="$TEMP_DIR/$(basename "$backup_key")"
    if ! download_backup "$backup_key" "$backup_file"; then
        error_exit "Failed to download backup"
    fi
    
    # Verify backup integrity
    if ! verify_backup_integrity "$backup_file" "$backup_key"; then
        error_exit "Backup integrity verification failed"
    fi
    
    # Confirm restore operation
    echo "WARNING: This will replace the current Jenkins installation with the backup."
    echo "Backup to restore: $backup_key"
    echo -n "Are you sure you want to continue? (yes/no): "
    read -r confirmation
    
    if [[ "$confirmation" != "yes" ]]; then
        log "Restore operation cancelled by user"
        cleanup_temp_files
        exit 0
    fi
    
    # Restore Jenkins
    restore_jenkins "$backup_file"
    
    # Cleanup
    cleanup_temp_files
    
    log "Jenkins restore completed successfully"
}

# Trap to ensure cleanup on exit
trap cleanup_temp_files EXIT

# Execute main function
main "$@"