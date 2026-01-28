#!/bin/bash
set -e

# Jenkins Backup Test Script
# This script tests the ThinBackup plugin configuration and backup functionality

# Configuration
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
BACKUP_DIR="${JENKINS_HOME}/backups"
JENKINS_URL="${JENKINS_URL:-http://localhost:8080}"
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_TOKEN="${JENKINS_TOKEN}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

success() {
    echo -e "${GREEN}✓ $1${NC}"
}

failure() {
    echo -e "${RED}✗ $1${NC}"
}

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
TEST_RESULTS=()

add_test_result() {
    local test_name="$1"
    local result="$2"
    local details="$3"
    
    TEST_RESULTS+=("$test_name|$result|$details")
    if [[ "$result" == "PASS" ]]; then
        ((TESTS_PASSED++))
        success "$test_name"
    else
        ((TESTS_FAILED++))
        failure "$test_name: $details"
    fi
}

# Test 1: Verify backup directory structure
test_backup_directory_structure() {
    info "Testing backup directory structure"
    
    if [[ -d "$BACKUP_DIR" ]]; then
        add_test_result "Backup Directory Exists" "PASS" "Directory found at $BACKUP_DIR"
        
        # Check permissions
        if [[ -w "$BACKUP_DIR" ]]; then
            add_test_result "Backup Directory Writable" "PASS" "Directory is writable"
        else
            add_test_result "Backup Directory Writable" "FAIL" "Directory is not writable"
        fi
        
        # Check subdirectories
        for subdir in full diff archive; do
            if [[ -d "$BACKUP_DIR/$subdir" ]]; then
                add_test_result "Backup Subdirectory $subdir" "PASS" "Subdirectory exists"
            else
                add_test_result "Backup Subdirectory $subdir" "FAIL" "Subdirectory missing"
            fi
        done
    else
        add_test_result "Backup Directory Exists" "FAIL" "Directory not found at $BACKUP_DIR"
    fi
}

# Test 2: Verify ThinBackup plugin configuration
test_thinbackup_configuration() {
    info "Testing ThinBackup plugin configuration"
    
    local config_file="$JENKINS_HOME/org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml"
    
    if [[ -f "$config_file" ]]; then
        add_test_result "ThinBackup Config File" "PASS" "Configuration file exists"
        
        # Check key configuration values
        if grep -q "<backupPath>$BACKUP_DIR</backupPath>" "$config_file"; then
            add_test_result "Backup Path Configuration" "PASS" "Correct backup path configured"
        else
            add_test_result "Backup Path Configuration" "FAIL" "Incorrect or missing backup path"
        fi
        
        if grep -q "<fullBackupSchedule>0 2 \* \* \*</fullBackupSchedule>" "$config_file"; then
            add_test_result "Full Backup Schedule" "PASS" "Daily backup at 2 AM configured"
        else
            add_test_result "Full Backup Schedule" "FAIL" "Backup schedule not configured correctly"
        fi
        
        if grep -q "<nrMaxStoredFull>30</nrMaxStoredFull>" "$config_file"; then
            add_test_result "Backup Retention Policy" "PASS" "30 backup retention configured"
        else
            add_test_result "Backup Retention Policy" "FAIL" "Backup retention not configured correctly"
        fi
    else
        add_test_result "ThinBackup Config File" "FAIL" "Configuration file not found"
    fi
}

# Test 3: Test backup scripts
test_backup_scripts() {
    info "Testing backup management scripts"
    
    local scripts=("backup-retention.sh" "backup-monitor.sh" "verify-backup.sh")
    
    for script in "${scripts[@]}"; do
        local script_path="$JENKINS_HOME/$script"
        if [[ -f "$script_path" && -x "$script_path" ]]; then
            add_test_result "Script $script" "PASS" "Script exists and is executable"
        else
            add_test_result "Script $script" "FAIL" "Script missing or not executable"
        fi
    done
}

# Test 4: Test cron jobs
test_cron_jobs() {
    info "Testing backup cron jobs"
    
    local cron_files=("/etc/cron.d/jenkins-backup-retention" "/etc/cron.d/jenkins-backup-monitor")
    
    for cron_file in "${cron_files[@]}"; do
        if [[ -f "$cron_file" ]]; then
            add_test_result "Cron Job $(basename "$cron_file")" "PASS" "Cron job configured"
        else
            add_test_result "Cron Job $(basename "$cron_file")" "FAIL" "Cron job not configured"
        fi
    done
}

# Test 5: Test systemd timer
test_systemd_timer() {
    info "Testing systemd backup timer"
    
    if systemctl is-enabled jenkins-backup-monitor.timer >/dev/null 2>&1; then
        add_test_result "Systemd Timer Enabled" "PASS" "Timer is enabled"
        
        if systemctl is-active jenkins-backup-monitor.timer >/dev/null 2>&1; then
            add_test_result "Systemd Timer Active" "PASS" "Timer is running"
        else
            add_test_result "Systemd Timer Active" "FAIL" "Timer is not running"
        fi
    else
        add_test_result "Systemd Timer Enabled" "FAIL" "Timer is not enabled"
    fi
}

# Test 6: Test Jenkins connectivity (if credentials provided)
test_jenkins_connectivity() {
    info "Testing Jenkins connectivity"
    
    if [[ -n "$JENKINS_TOKEN" ]]; then
        local response
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -u "$JENKINS_USER:$JENKINS_TOKEN" "$JENKINS_URL/api/json" 2>/dev/null || echo "HTTPSTATUS:000")
        local status_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
        
        if [[ "$status_code" == "200" ]]; then
            add_test_result "Jenkins API Access" "PASS" "Successfully connected to Jenkins API"
        else
            add_test_result "Jenkins API Access" "FAIL" "Failed to connect to Jenkins API (HTTP $status_code)"
        fi
    else
        add_test_result "Jenkins API Access" "SKIP" "No Jenkins token provided"
    fi
}

# Test 7: Trigger manual backup (if Jenkins is accessible)
test_manual_backup() {
    info "Testing manual backup trigger"
    
    if [[ -n "$JENKINS_TOKEN" ]]; then
        # Try to trigger a backup via Jenkins CLI or API
        local backup_trigger_url="$JENKINS_URL/plugin/thinbackup/backupnow"
        local response
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -u "$JENKINS_USER:$JENKINS_TOKEN" "$backup_trigger_url" 2>/dev/null || echo "HTTPSTATUS:000")
        local status_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
        
        if [[ "$status_code" == "200" || "$status_code" == "302" ]]; then
            add_test_result "Manual Backup Trigger" "PASS" "Backup trigger successful"
            
            # Wait a bit and check for new backup files
            sleep 10
            local recent_backup=$(find "$BACKUP_DIR" -name "FULL-*" -newermt "2 minutes ago" | head -1)
            if [[ -n "$recent_backup" ]]; then
                add_test_result "Manual Backup Creation" "PASS" "Backup file created: $(basename "$recent_backup")"
            else
                add_test_result "Manual Backup Creation" "WARN" "No new backup file detected (may take longer)"
            fi
        else
            add_test_result "Manual Backup Trigger" "FAIL" "Failed to trigger backup (HTTP $status_code)"
        fi
    else
        add_test_result "Manual Backup Trigger" "SKIP" "No Jenkins token provided"
    fi
}

# Test 8: Verify backup monitoring
test_backup_monitoring() {
    info "Testing backup monitoring functionality"
    
    if [[ -x "$JENKINS_HOME/backup-monitor.sh" ]]; then
        # Run backup monitor script
        if "$JENKINS_HOME/backup-monitor.sh" >/dev/null 2>&1; then
            add_test_result "Backup Monitor Execution" "PASS" "Monitor script executed successfully"
        else
            add_test_result "Backup Monitor Execution" "FAIL" "Monitor script failed to execute"
        fi
        
        # Check if log file was created
        if [[ -f "/var/log/jenkins/backup-monitor.log" ]]; then
            add_test_result "Backup Monitor Logging" "PASS" "Monitor log file created"
        else
            add_test_result "Backup Monitor Logging" "WARN" "Monitor log file not found"
        fi
    else
        add_test_result "Backup Monitor Execution" "FAIL" "Monitor script not found or not executable"
    fi
}

# Main test execution
main() {
    log "Starting Jenkins ThinBackup configuration tests"
    
    test_backup_directory_structure
    test_thinbackup_configuration
    test_backup_scripts
    test_cron_jobs
    test_systemd_timer
    test_jenkins_connectivity
    test_manual_backup
    test_backup_monitoring
    
    # Generate test report
    log "Test Results Summary"
    echo "=================================="
    printf "%-35s %-6s %s\n" "Test" "Result" "Details"
    echo "=================================="
    
    for result in "${TEST_RESULTS[@]}"; do
        IFS='|' read -r name status details <<< "$result"
        
        case "$status" in
            "PASS") printf "%-35s ${GREEN}%-6s${NC} %s\n" "$name" "$status" "$details" ;;
            "FAIL") printf "%-35s ${RED}%-6s${NC} %s\n" "$name" "$status" "$details" ;;
            "WARN") printf "%-35s ${YELLOW}%-6s${NC} %s\n" "$name" "$status" "$details" ;;
            "SKIP") printf "%-35s ${BLUE}%-6s${NC} %s\n" "$name" "$status" "$details" ;;
        esac
    done
    
    echo "=================================="
    log "Tests completed: $TESTS_PASSED passed, $TESTS_FAILED failed"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        success "All critical tests passed! Jenkins ThinBackup is properly configured."
        exit 0
    else
        error "$TESTS_FAILED test(s) failed. Please review the configuration."
        exit 1
    fi
}

# Check if required tools are available
if ! command -v curl &> /dev/null; then
    warn "curl not found, Jenkins connectivity tests will be skipped"
fi

# Run main test suite
main "$@"