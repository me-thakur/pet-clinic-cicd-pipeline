#!/bin/bash

# S3 Backup Synchronization Script for Jenkins
# This script uploads Jenkins backups to S3 bucket with encryption and integrity checking
# Requirements: 7.1, 7.2, 7.3

set -euo pipefail

# Configuration
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
BACKUP_DIR="${JENKINS_HOME}/backup"
S3_BUCKET="${S3_BACKUP_BUCKET:-pet-clinic-jenkins-backups}"
AWS_REGION="${AWS_REGION:-us-east-1}"
LOG_FILE="/var/log/jenkins-backup-sync.log"
ENCRYPTION_KEY_ID="${KMS_KEY_ID:-alias/jenkins-backup-key}"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
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
    
    # Check if backup directory exists
    if [[ ! -d "$BACKUP_DIR" ]]; then
        error_exit "Backup directory does not exist: $BACKUP_DIR"
    fi
    
    # Check if S3 bucket exists
    if ! aws s3 ls "s3://$S3_BUCKET" &> /dev/null; then
        error_exit "S3 bucket does not exist or is not accessible: $S3_BUCKET"
    fi
    
    log "Prerequisites check passed"
}

# Calculate file checksum for integrity verification
calculate_checksum() {
    local file="$1"
    sha256sum "$file" | cut -d' ' -f1
}

# Upload backup file to S3 with encryption
upload_backup() {
    local backup_file="$1"
    local s3_key="$2"
    local checksum="$3"
    
    log "Uploading $backup_file to s3://$S3_BUCKET/$s3_key"
    
    # Upload with server-side encryption
    if aws s3 cp "$backup_file" "s3://$S3_BUCKET/$s3_key" \
        --server-side-encryption aws:kms \
        --ssekms-key-id "$ENCRYPTION_KEY_ID" \
        --metadata "checksum=$checksum,upload-time=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        --region "$AWS_REGION"; then
        log "Successfully uploaded $backup_file"
        return 0
    else
        log "Failed to upload $backup_file"
        return 1
    fi
}

# Verify uploaded backup integrity
verify_backup_integrity() {
    local s3_key="$1"
    local expected_checksum="$2"
    
    log "Verifying integrity of s3://$S3_BUCKET/$s3_key"
    
    # Download file to temporary location for verification
    local temp_file="/tmp/backup_verify_$(basename "$s3_key")"
    
    if aws s3 cp "s3://$S3_BUCKET/$s3_key" "$temp_file" --region "$AWS_REGION"; then
        local actual_checksum
        actual_checksum=$(calculate_checksum "$temp_file")
        
        if [[ "$actual_checksum" == "$expected_checksum" ]]; then
            log "Integrity verification passed for $s3_key"
            rm -f "$temp_file"
            return 0
        else
            log "Integrity verification failed for $s3_key (expected: $expected_checksum, actual: $actual_checksum)"
            rm -f "$temp_file"
            return 1
        fi
    else
        log "Failed to download $s3_key for verification"
        return 1
    fi
}

# Sync all backup files to S3
sync_backups() {
    log "Starting backup synchronization to S3..."
    
    local upload_count=0
    local error_count=0
    
    # Find all backup files (ThinBackup creates .zip files)
    while IFS= read -r -d '' backup_file; do
        local filename
        filename=$(basename "$backup_file")
        local backup_date
        backup_date=$(date -r "$backup_file" +%Y/%m/%d)
        local s3_key="jenkins-backups/$backup_date/$filename"
        
        # Calculate checksum before upload
        local checksum
        checksum=$(calculate_checksum "$backup_file")
        
        # Check if file already exists in S3 with same checksum
        if aws s3api head-object --bucket "$S3_BUCKET" --key "$s3_key" --region "$AWS_REGION" &> /dev/null; then
            local existing_checksum
            existing_checksum=$(aws s3api head-object --bucket "$S3_BUCKET" --key "$s3_key" --region "$AWS_REGION" --query 'Metadata.checksum' --output text 2>/dev/null || echo "")
            
            if [[ "$existing_checksum" == "$checksum" ]]; then
                log "Backup $filename already exists in S3 with same checksum, skipping"
                continue
            fi
        fi
        
        # Upload backup file
        if upload_backup "$backup_file" "$s3_key" "$checksum"; then
            # Verify integrity after upload
            if verify_backup_integrity "$s3_key" "$checksum"; then
                ((upload_count++))
                log "Successfully synchronized $filename"
            else
                ((error_count++))
                log "Integrity verification failed for $filename"
            fi
        else
            ((error_count++))
            log "Failed to upload $filename"
        fi
        
    done < <(find "$BACKUP_DIR" -name "*.zip" -type f -print0)
    
    log "Backup synchronization completed: $upload_count uploaded, $error_count errors"
    
    if [[ $error_count -gt 0 ]]; then
        return 1
    fi
    
    return 0
}

# Clean up old local backups based on retention policy
cleanup_local_backups() {
    log "Cleaning up old local backups..."
    
    # Keep last 7 days of local backups (ThinBackup handles its own retention)
    find "$BACKUP_DIR" -name "*.zip" -type f -mtime +7 -delete
    
    log "Local backup cleanup completed"
}

# Send notification on completion
send_notification() {
    local status="$1"
    local message="$2"
    
    # Send SNS notification if topic is configured
    if [[ -n "${SNS_TOPIC_ARN:-}" ]]; then
        aws sns publish \
            --topic-arn "$SNS_TOPIC_ARN" \
            --subject "Jenkins Backup Sync - $status" \
            --message "$message" \
            --region "$AWS_REGION" || log "Failed to send SNS notification"
    fi
    
    # Log to CloudWatch if log group is configured
    if [[ -n "${CLOUDWATCH_LOG_GROUP:-}" ]]; then
        aws logs put-log-events \
            --log-group-name "$CLOUDWATCH_LOG_GROUP" \
            --log-stream-name "jenkins-backup-sync" \
            --log-events timestamp=$(date +%s000),message="$message" \
            --region "$AWS_REGION" || log "Failed to send CloudWatch log"
    fi
}

# Main execution
main() {
    log "Starting Jenkins backup synchronization to S3"
    
    # Check prerequisites
    check_prerequisites
    
    # Sync backups to S3
    if sync_backups; then
        cleanup_local_backups
        local success_message="Jenkins backup synchronization completed successfully"
        log "$success_message"
        send_notification "SUCCESS" "$success_message"
        exit 0
    else
        local error_message="Jenkins backup synchronization completed with errors"
        log "$error_message"
        send_notification "ERROR" "$error_message"
        exit 1
    fi
}

# Execute main function
main "$@"