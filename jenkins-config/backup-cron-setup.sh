#!/bin/bash

# Setup cron job for automated S3 backup synchronization
# This script configures automated backup synchronization to S3

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SYNC_SCRIPT="$SCRIPT_DIR/s3-backup-sync.sh"
CRON_USER="jenkins"
LOG_FILE="/var/log/jenkins-backup-cron-setup.log"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Check if script exists and is executable
check_sync_script() {
    if [[ ! -f "$SYNC_SCRIPT" ]]; then
        error_exit "Sync script not found: $SYNC_SCRIPT"
    fi
    
    if [[ ! -x "$SYNC_SCRIPT" ]]; then
        log "Making sync script executable..."
        chmod +x "$SYNC_SCRIPT"
    fi
}

# Setup cron job
setup_cron_job() {
    log "Setting up cron job for backup synchronization..."
    
    # Create cron job entry
    local cron_entry="0 2 * * * $SYNC_SCRIPT >> /var/log/jenkins-backup-sync.log 2>&1"
    
    # Add cron job for jenkins user
    (crontab -u "$CRON_USER" -l 2>/dev/null || echo "") | grep -v "$SYNC_SCRIPT" | {
        cat
        echo "$cron_entry"
    } | crontab -u "$CRON_USER" -
    
    log "Cron job added for user $CRON_USER: $cron_entry"
}

# Verify cron job
verify_cron_job() {
    log "Verifying cron job installation..."
    
    if crontab -u "$CRON_USER" -l | grep -q "$SYNC_SCRIPT"; then
        log "Cron job verified successfully"
        crontab -u "$CRON_USER" -l | grep "$SYNC_SCRIPT"
    else
        error_exit "Cron job verification failed"
    fi
}

# Main execution
main() {
    log "Setting up automated backup synchronization cron job"
    
    check_sync_script
    setup_cron_job
    verify_cron_job
    
    log "Backup synchronization cron job setup completed"
    log "Backups will be synchronized to S3 daily at 2:00 AM"
}

# Execute main function
main "$@"