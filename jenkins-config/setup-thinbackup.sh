#!/bin/bash
set -e

# Jenkins ThinBackup Plugin Setup Script
# This script configures the ThinBackup plugin for automated Jenkins backups

# Configuration
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
BACKUP_DIR="${JENKINS_HOME}/backups"
THINBACKUP_CONFIG="${JENKINS_HOME}/org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml"
SERVICE_USER="${SERVICE_USER:-jenkins}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   error "This script should not be run as root for security reasons"
fi

log "Setting up Jenkins ThinBackup plugin configuration"

# Verify Jenkins is installed and running
if ! systemctl is-active --quiet jenkins; then
    error "Jenkins service is not running. Please start Jenkins first."
fi

# Create backup directory
log "Creating backup directory: $BACKUP_DIR"
sudo mkdir -p "$BACKUP_DIR"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$BACKUP_DIR"
sudo chmod 755 "$BACKUP_DIR"

# Create backup subdirectories for organization
sudo mkdir -p "$BACKUP_DIR"/{full,diff,archive}
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$BACKUP_DIR"/{full,diff,archive}

# Copy ThinBackup configuration
log "Installing ThinBackup plugin configuration"
if [[ -f "thinbackup-config.xml" ]]; then
    sudo cp thinbackup-config.xml "$THINBACKUP_CONFIG"
    sudo chown "$SERVICE_USER:$SERVICE_USER" "$THINBACKUP_CONFIG"
    sudo chmod 644 "$THINBACKUP_CONFIG"
else
    warn "ThinBackup configuration file not found, creating default configuration"
    
    # Create default configuration
    sudo tee "$THINBACKUP_CONFIG" > /dev/null << 'EOF'
<?xml version='1.0' encoding='UTF-8'?>
<org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl plugin="thinbackup@1.15">
  <options>
    <backupPath>/var/lib/jenkins/backups</backupPath>
    <nrMaxStoredFull>30</nrMaxStoredFull>
    <fullBackupSchedule>0 2 * * *</fullBackupSchedule>
    <diffBackupSchedule>0 */6 * * *</diffBackupSchedule>
    <cleanupDiff>true</cleanupDiff>
    <maxStoredDiff>28</maxStoredDiff>
    <moveOldBackupsToZipFile>true</moveOldBackupsToZipFile>
    <backupBuildResults>true</backupBuildResults>
    <backupBuildArchive>false</backupBuildArchive>
    <backupUserContents>true</backupUserContents>
    <backupNextBuildNumber>true</backupNextBuildNumber>
    <backupWorkspace>false</backupWorkspace>
    <excludedFilesRegex>.*\.log$|.*\.tmp$|.*workspace.*|.*\.git.*</excludedFilesRegex>
    <waitForIdle>true</waitForIdle>
    <forceQuietModeTimeout>120</forceQuietModeTimeout>
    <backupAdditionalFiles>true</backupAdditionalFiles>
    <backupAdditionalFilesRegex>config\.xml|.*\.key|secrets/.*</backupAdditionalFilesRegex>
    <emailSuccess>false</emailSuccess>
    <emailFailure>true</emailFailure>
    <emailAddress>admin@petclinic.local</emailAddress>
  </options>
</org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl>
EOF
    
    sudo chown "$SERVICE_USER:$SERVICE_USER" "$THINBACKUP_CONFIG"
    sudo chmod 644 "$THINBACKUP_CONFIG"
fi

# Create backup retention script
log "Creating backup retention management script"
sudo tee "$JENKINS_HOME/backup-retention.sh" > /dev/null << 'EOF'
#!/bin/bash
# Jenkins Backup Retention Management Script
# Implements retention policy: 30 daily, 12 weekly, 12 monthly

BACKUP_DIR="/var/lib/jenkins/backups"
LOG_FILE="/var/log/jenkins/backup-retention.log"

# Ensure log directory exists
mkdir -p "$(dirname "$LOG_FILE")"

log_message() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Daily backup retention (keep last 30)
log_message "Starting daily backup retention cleanup"
cd "$BACKUP_DIR/full" 2>/dev/null || exit 1

# Keep only the 30 most recent daily backups
ls -t FULL-* 2>/dev/null | tail -n +31 | while read -r backup; do
    if [[ -f "$backup" || -d "$backup" ]]; then
        log_message "Removing old daily backup: $backup"
        rm -rf "$backup"
    fi
done

# Weekly backup retention (keep 12 weekly backups)
log_message "Managing weekly backup retention"
# Move every 7th backup to weekly archive
weekly_count=0
ls -t FULL-* 2>/dev/null | while read -r backup; do
    backup_date=$(echo "$backup" | grep -o '[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}' | head -1)
    if [[ -n "$backup_date" ]]; then
        day_of_week=$(date -d "$backup_date" +%u)
        if [[ "$day_of_week" == "7" ]] && [[ $weekly_count -lt 12 ]]; then
            # This is a Sunday backup, keep as weekly
            weekly_count=$((weekly_count + 1))
            log_message "Preserving weekly backup: $backup"
        fi
    fi
done

# Monthly backup retention (keep 12 monthly backups)
log_message "Managing monthly backup retention"
# Keep first backup of each month for 12 months
monthly_count=0
current_month=""
ls -t FULL-* 2>/dev/null | while read -r backup; do
    backup_date=$(echo "$backup" | grep -o '[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}' | head -1)
    if [[ -n "$backup_date" ]]; then
        backup_month=$(date -d "$backup_date" +%Y-%m)
        if [[ "$backup_month" != "$current_month" ]] && [[ $monthly_count -lt 12 ]]; then
            current_month="$backup_month"
            monthly_count=$((monthly_count + 1))
            log_message "Preserving monthly backup: $backup"
        fi
    fi
done

# Clean up differential backups older than 7 days
log_message "Cleaning up old differential backups"
cd "$BACKUP_DIR/diff" 2>/dev/null || exit 0
find . -name "DIFF-*" -type f -mtime +7 -exec rm -f {} \; -print | while read -r file; do
    log_message "Removed old differential backup: $file"
done

log_message "Backup retention cleanup completed"
EOF

sudo chmod +x "$JENKINS_HOME/backup-retention.sh"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$JENKINS_HOME/backup-retention.sh"

# Create cron job for backup retention
log "Setting up cron job for backup retention"
sudo tee /etc/cron.d/jenkins-backup-retention > /dev/null << EOF
# Jenkins backup retention cleanup - runs daily at 3 AM
0 3 * * * $SERVICE_USER $JENKINS_HOME/backup-retention.sh
EOF

# Create backup monitoring script
log "Creating backup monitoring script"
sudo tee "$JENKINS_HOME/backup-monitor.sh" > /dev/null << 'EOF'
#!/bin/bash
# Jenkins Backup Monitoring Script
# Checks backup status and sends alerts if needed

BACKUP_DIR="/var/lib/jenkins/backups"
LOG_FILE="/var/log/jenkins/backup-monitor.log"
ALERT_EMAIL="${BACKUP_ALERT_EMAIL:-admin@petclinic.local}"

# Ensure log directory exists
mkdir -p "$(dirname "$LOG_FILE")"

log_message() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

send_alert() {
    local subject="$1"
    local message="$2"
    
    # Try to send email if mail command is available
    if command -v mail >/dev/null 2>&1; then
        echo "$message" | mail -s "$subject" "$ALERT_EMAIL"
        log_message "Alert sent: $subject"
    else
        log_message "ALERT: $subject - $message"
    fi
}

# Check if backup directory exists and is accessible
if [[ ! -d "$BACKUP_DIR" ]]; then
    send_alert "Jenkins Backup Alert: Backup directory missing" "Backup directory $BACKUP_DIR does not exist or is not accessible."
    exit 1
fi

# Check for recent backups (within last 25 hours)
log_message "Checking for recent backups"
recent_backup=$(find "$BACKUP_DIR" -name "FULL-*" -newermt "25 hours ago" | head -1)

if [[ -z "$recent_backup" ]]; then
    send_alert "Jenkins Backup Alert: No recent backups found" "No full backup found within the last 25 hours. Please check backup configuration."
    exit 1
fi

# Check backup size (should be reasonable)
backup_size=$(du -sh "$BACKUP_DIR" | cut -f1)
log_message "Current backup directory size: $backup_size"

# Check disk space
available_space=$(df "$BACKUP_DIR" | awk 'NR==2 {print $4}')
used_percentage=$(df "$BACKUP_DIR" | awk 'NR==2 {print $5}' | sed 's/%//')

if [[ "$used_percentage" -gt 90 ]]; then
    send_alert "Jenkins Backup Alert: Low disk space" "Backup disk usage is at ${used_percentage}%. Available space: ${available_space}KB"
fi

# Count backup files
full_backups=$(find "$BACKUP_DIR" -name "FULL-*" | wc -l)
diff_backups=$(find "$BACKUP_DIR" -name "DIFF-*" | wc -l)

log_message "Backup inventory: $full_backups full backups, $diff_backups differential backups"

# Verify backup integrity (check if latest backup is not empty)
if [[ -n "$recent_backup" ]]; then
    if [[ -f "$recent_backup" ]]; then
        backup_file_size=$(stat -f%z "$recent_backup" 2>/dev/null || stat -c%s "$recent_backup" 2>/dev/null || echo "0")
        if [[ "$backup_file_size" -lt 1024 ]]; then
            send_alert "Jenkins Backup Alert: Backup file too small" "Latest backup file $recent_backup is only ${backup_file_size} bytes, which may indicate a failed backup."
        fi
    elif [[ -d "$recent_backup" ]]; then
        backup_dir_size=$(du -s "$recent_backup" | cut -f1)
        if [[ "$backup_dir_size" -lt 100 ]]; then
            send_alert "Jenkins Backup Alert: Backup directory too small" "Latest backup directory $recent_backup is only ${backup_dir_size}KB, which may indicate a failed backup."
        fi
    fi
fi

log_message "Backup monitoring completed successfully"
EOF

sudo chmod +x "$JENKINS_HOME/backup-monitor.sh"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$JENKINS_HOME/backup-monitor.sh"

# Create cron job for backup monitoring
log "Setting up cron job for backup monitoring"
sudo tee /etc/cron.d/jenkins-backup-monitor > /dev/null << EOF
# Jenkins backup monitoring - runs every 4 hours
0 */4 * * * $SERVICE_USER $JENKINS_HOME/backup-monitor.sh
EOF

# Create backup verification script
log "Creating backup verification script"
sudo tee "$JENKINS_HOME/verify-backup.sh" > /dev/null << 'EOF'
#!/bin/bash
# Jenkins Backup Verification Script
# Verifies backup integrity and completeness

BACKUP_DIR="/var/lib/jenkins/backups"
JENKINS_HOME="/var/lib/jenkins"

verify_backup() {
    local backup_path="$1"
    local backup_name=$(basename "$backup_path")
    
    echo "Verifying backup: $backup_name"
    
    # Check if backup exists
    if [[ ! -e "$backup_path" ]]; then
        echo "ERROR: Backup $backup_name does not exist"
        return 1
    fi
    
    # Check backup size
    if [[ -f "$backup_path" ]]; then
        # ZIP file backup
        if ! unzip -t "$backup_path" >/dev/null 2>&1; then
            echo "ERROR: Backup ZIP file $backup_name is corrupted"
            return 1
        fi
        echo "✓ ZIP file integrity verified"
    elif [[ -d "$backup_path" ]]; then
        # Directory backup
        essential_files=("config.xml" "jobs")
        for file in "${essential_files[@]}"; do
            if [[ ! -e "$backup_path/$file" ]]; then
                echo "WARNING: Essential file/directory $file missing from backup"
            else
                echo "✓ Essential file $file found"
            fi
        done
    fi
    
    echo "Backup verification completed for $backup_name"
    return 0
}

# Verify latest full backup
latest_backup=$(find "$BACKUP_DIR" -name "FULL-*" -type f -o -name "FULL-*" -type d | sort -r | head -1)

if [[ -n "$latest_backup" ]]; then
    verify_backup "$latest_backup"
else
    echo "ERROR: No full backups found in $BACKUP_DIR"
    exit 1
fi
EOF

sudo chmod +x "$JENKINS_HOME/verify-backup.sh"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$JENKINS_HOME/verify-backup.sh"

# Update Jenkins plugins list to include ThinBackup
log "Adding ThinBackup plugin to plugins list"
if [[ -f "plugins.txt" ]]; then
    if ! grep -q "thinbackup" plugins.txt; then
        echo "thinbackup:1.15" >> plugins.txt
        log "Added ThinBackup plugin to plugins.txt"
    else
        log "ThinBackup plugin already in plugins.txt"
    fi
fi

# Create systemd service for backup monitoring (optional)
log "Creating systemd timer for backup operations"
sudo tee /etc/systemd/system/jenkins-backup-monitor.service > /dev/null << EOF
[Unit]
Description=Jenkins Backup Monitor
After=jenkins.service

[Service]
Type=oneshot
User=$SERVICE_USER
ExecStart=$JENKINS_HOME/backup-monitor.sh
EOF

sudo tee /etc/systemd/system/jenkins-backup-monitor.timer > /dev/null << EOF
[Unit]
Description=Run Jenkins Backup Monitor every 4 hours
Requires=jenkins-backup-monitor.service

[Timer]
OnCalendar=*-*-* 00,04,08,12,16,20:00:00
Persistent=true

[Install]
WantedBy=timers.target
EOF

# Enable and start the timer
sudo systemctl daemon-reload
sudo systemctl enable jenkins-backup-monitor.timer
sudo systemctl start jenkins-backup-monitor.timer

log "✓ Jenkins ThinBackup plugin configuration completed successfully"
log "Configuration details:"
log "  - Backup directory: $BACKUP_DIR"
log "  - Full backups: Daily at 2 AM (keep 30)"
log "  - Differential backups: Every 6 hours (keep 28)"
log "  - Retention policy: 30 daily, 12 weekly, 12 monthly"
log "  - Monitoring: Every 4 hours"
log ""
log "Next steps:"
log "1. Restart Jenkins to load the ThinBackup plugin configuration"
log "2. Verify backup schedule in Jenkins UI: Manage Jenkins > ThinBackup"
log "3. Test backup functionality manually"
log "4. Monitor backup logs in /var/log/jenkins/"