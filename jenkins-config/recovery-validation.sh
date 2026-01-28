#!/bin/bash

# Jenkins Recovery Validation Script
# This script validates that Jenkins has been properly restored after disaster recovery
# Requirements: 7.4

set -euo pipefail

# Configuration
JENKINS_HOME="${JENKINS_HOME:-/var/lib/jenkins}"
JENKINS_URL="${JENKINS_URL:-http://localhost:8080}"
LOG_FILE="/var/log/jenkins-recovery-validation.log"
VALIDATION_REPORT="/tmp/jenkins-validation-report-$(date +%Y%m%d-%H%M%S).json"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Initialize validation report
init_validation_report() {
    local report='{
        "validation_timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
        "jenkins_home": "'$JENKINS_HOME'",
        "jenkins_url": "'$JENKINS_URL'",
        "validation_results": {
            "service_status": null,
            "web_interface": null,
            "configuration_files": null,
            "job_configurations": null,
            "user_accounts": null,
            "plugin_status": null,
            "build_history": null,
            "system_configuration": null
        },
        "overall_status": "in_progress",
        "issues_found": [],
        "recommendations": []
    }'
    
    echo "$report" > "$VALIDATION_REPORT"
}

# Update validation report
update_validation_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    python3 -c "
import json
import sys

try:
    with open('$VALIDATION_REPORT', 'r') as f:
        report = json.load(f)
    
    report['validation_results']['$test_name'] = {
        'status': '$status',
        'details': '$details',
        'timestamp': '$(date -u +%Y-%m-%dT%H:%M:%SZ)'
    }
    
    with open('$VALIDATION_REPORT', 'w') as f:
        json.dump(report, f, indent=2)
except Exception as e:
    print(f'Failed to update validation report: {e}', file=sys.stderr)
" || log "Failed to update validation report for $test_name"
}

# Add issue to report
add_issue() {
    local severity="$1"
    local description="$2"
    local recommendation="$3"
    
    python3 -c "
import json
import sys

try:
    with open('$VALIDATION_REPORT', 'r') as f:
        report = json.load(f)
    
    issue = {
        'severity': '$severity',
        'description': '$description',
        'timestamp': '$(date -u +%Y-%m-%dT%H:%M:%SZ)'
    }
    
    report['issues_found'].append(issue)
    
    if '$recommendation':
        report['recommendations'].append('$recommendation')
    
    with open('$VALIDATION_REPORT', 'w') as f:
        json.dump(report, f, indent=2)
except Exception as e:
    print(f'Failed to add issue to report: {e}', file=sys.stderr)
" || log "Failed to add issue to validation report"
}

# Finalize validation report
finalize_validation_report() {
    local overall_status="$1"
    
    python3 -c "
import json
import sys

try:
    with open('$VALIDATION_REPORT', 'r') as f:
        report = json.load(f)
    
    report['overall_status'] = '$overall_status'
    report['validation_completed'] = '$(date -u +%Y-%m-%dT%H:%M:%SZ)'
    
    with open('$VALIDATION_REPORT', 'w') as f:
        json.dump(report, f, indent=2)
except Exception as e:
    print(f'Failed to finalize validation report: {e}', file=sys.stderr)
" || log "Failed to finalize validation report"
}

# Test Jenkins service status
test_service_status() {
    log "Testing Jenkins service status..."
    
    if systemctl is-active --quiet jenkins; then
        log "✓ Jenkins service is running"
        update_validation_result "service_status" "pass" "Jenkins service is active and running"
        return 0
    else
        log "✗ Jenkins service is not running"
        update_validation_result "service_status" "fail" "Jenkins service is not active"
        add_issue "critical" "Jenkins service is not running" "Start Jenkins service with: sudo systemctl start jenkins"
        return 1
    fi
}

# Test Jenkins web interface
test_web_interface() {
    log "Testing Jenkins web interface accessibility..."
    
    local max_attempts=10
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        local http_code
        http_code=$(curl -s -o /dev/null -w "%{http_code}" "$JENKINS_URL" 2>/dev/null || echo "000")
        
        if [[ "$http_code" =~ ^(200|403)$ ]]; then
            log "✓ Jenkins web interface is accessible (HTTP $http_code)"
            update_validation_result "web_interface" "pass" "Jenkins web interface accessible with HTTP code $http_code"
            return 0
        fi
        
        log "Attempt $attempt/$max_attempts: Jenkins web interface returned HTTP $http_code"
        sleep 5
        ((attempt++))
    done
    
    log "✗ Jenkins web interface is not accessible"
    update_validation_result "web_interface" "fail" "Jenkins web interface not accessible after $max_attempts attempts"
    add_issue "critical" "Jenkins web interface not accessible" "Check Jenkins service logs and network configuration"
    return 1
}

# Test configuration files
test_configuration_files() {
    log "Testing Jenkins configuration files..."
    
    local config_files=(
        "$JENKINS_HOME/config.xml"
        "$JENKINS_HOME/hudson.model.UpdateCenter.xml"
    )
    
    local missing_files=()
    local valid_files=0
    
    for config_file in "${config_files[@]}"; do
        if [[ -f "$config_file" ]]; then
            if [[ -s "$config_file" ]]; then
                log "✓ Configuration file exists and is not empty: $config_file"
                ((valid_files++))
            else
                log "⚠ Configuration file exists but is empty: $config_file"
                add_issue "warning" "Empty configuration file: $config_file" "Review and restore configuration file content"
            fi
        else
            log "✗ Configuration file missing: $config_file"
            missing_files+=("$config_file")
        fi
    done
    
    if [[ ${#missing_files[@]} -eq 0 ]]; then
        update_validation_result "configuration_files" "pass" "$valid_files configuration files validated successfully"
        return 0
    else
        update_validation_result "configuration_files" "fail" "${#missing_files[@]} configuration files missing"
        for missing_file in "${missing_files[@]}"; do
            add_issue "critical" "Missing configuration file: $missing_file" "Restore configuration file from backup"
        done
        return 1
    fi
}

# Test job configurations
test_job_configurations() {
    log "Testing Jenkins job configurations..."
    
    local jobs_dir="$JENKINS_HOME/jobs"
    
    if [[ ! -d "$jobs_dir" ]]; then
        log "✗ Jobs directory does not exist: $jobs_dir"
        update_validation_result "job_configurations" "fail" "Jobs directory missing"
        add_issue "critical" "Jobs directory missing" "Restore jobs directory from backup"
        return 1
    fi
    
    local job_count=0
    local valid_jobs=0
    local invalid_jobs=()
    
    for job_dir in "$jobs_dir"/*; do
        if [[ -d "$job_dir" ]]; then
            ((job_count++))
            local job_name
            job_name=$(basename "$job_dir")
            local config_xml="$job_dir/config.xml"
            
            if [[ -f "$config_xml" ]] && [[ -s "$config_xml" ]]; then
                # Basic XML validation
                if xmllint --noout "$config_xml" 2>/dev/null; then
                    log "✓ Job configuration valid: $job_name"
                    ((valid_jobs++))
                else
                    log "✗ Job configuration invalid XML: $job_name"
                    invalid_jobs+=("$job_name")
                fi
            else
                log "✗ Job configuration missing or empty: $job_name"
                invalid_jobs+=("$job_name")
            fi
        fi
    done
    
    if [[ $job_count -eq 0 ]]; then
        log "⚠ No jobs found in Jenkins"
        update_validation_result "job_configurations" "warning" "No jobs found"
        add_issue "warning" "No jobs found" "Verify if jobs should exist or restore from backup"
        return 0
    elif [[ ${#invalid_jobs[@]} -eq 0 ]]; then
        log "✓ All $job_count job configurations are valid"
        update_validation_result "job_configurations" "pass" "$valid_jobs of $job_count jobs validated successfully"
        return 0
    else
        log "✗ ${#invalid_jobs[@]} of $job_count job configurations are invalid"
        update_validation_result "job_configurations" "fail" "${#invalid_jobs[@]} invalid job configurations found"
        for invalid_job in "${invalid_jobs[@]}"; do
            add_issue "high" "Invalid job configuration: $invalid_job" "Restore job configuration from backup"
        done
        return 1
    fi
}

# Test user accounts
test_user_accounts() {
    log "Testing Jenkins user accounts..."
    
    local users_dir="$JENKINS_HOME/users"
    
    if [[ ! -d "$users_dir" ]]; then
        log "⚠ Users directory does not exist: $users_dir"
        update_validation_result "user_accounts" "warning" "Users directory missing"
        add_issue "warning" "Users directory missing" "Create users directory or restore from backup if users existed"
        return 0
    fi
    
    local user_count=0
    local valid_users=0
    local invalid_users=()
    
    for user_dir in "$users_dir"/*; do
        if [[ -d "$user_dir" ]]; then
            ((user_count++))
            local username
            username=$(basename "$user_dir")
            local config_xml="$user_dir/config.xml"
            
            if [[ -f "$config_xml" ]] && [[ -s "$config_xml" ]]; then
                if xmllint --noout "$config_xml" 2>/dev/null; then
                    log "✓ User configuration valid: $username"
                    ((valid_users++))
                else
                    log "✗ User configuration invalid XML: $username"
                    invalid_users+=("$username")
                fi
            else
                log "✗ User configuration missing or empty: $username"
                invalid_users+=("$username")
            fi
        fi
    done
    
    if [[ $user_count -eq 0 ]]; then
        log "⚠ No users found in Jenkins"
        update_validation_result "user_accounts" "warning" "No users found"
        add_issue "warning" "No users found" "Create admin user or restore users from backup"
        return 0
    elif [[ ${#invalid_users[@]} -eq 0 ]]; then
        log "✓ All $user_count user accounts are valid"
        update_validation_result "user_accounts" "pass" "$valid_users of $user_count users validated successfully"
        return 0
    else
        log "✗ ${#invalid_users[@]} of $user_count user accounts are invalid"
        update_validation_result "user_accounts" "fail" "${#invalid_users[@]} invalid user accounts found"
        for invalid_user in "${invalid_users[@]}"; do
            add_issue "medium" "Invalid user account: $invalid_user" "Restore user configuration from backup"
        done
        return 1
    fi
}

# Test plugin status
test_plugin_status() {
    log "Testing Jenkins plugin status..."
    
    local plugins_dir="$JENKINS_HOME/plugins"
    
    if [[ ! -d "$plugins_dir" ]]; then
        log "✗ Plugins directory does not exist: $plugins_dir"
        update_validation_result "plugin_status" "fail" "Plugins directory missing"
        add_issue "critical" "Plugins directory missing" "Restore plugins directory from backup"
        return 1
    fi
    
    local plugin_count
    plugin_count=$(find "$plugins_dir" -name "*.jpi" -o -name "*.hpi" | wc -l)
    
    if [[ $plugin_count -eq 0 ]]; then
        log "⚠ No plugins found in Jenkins"
        update_validation_result "plugin_status" "warning" "No plugins found"
        add_issue "warning" "No plugins found" "Install required plugins or restore from backup"
        return 0
    else
        log "✓ Found $plugin_count plugins in Jenkins"
        update_validation_result "plugin_status" "pass" "$plugin_count plugins found"
        return 0
    fi
}

# Test build history
test_build_history() {
    log "Testing Jenkins build history..."
    
    local jobs_dir="$JENKINS_HOME/jobs"
    local total_builds=0
    local jobs_with_builds=0
    
    if [[ ! -d "$jobs_dir" ]]; then
        log "⚠ Jobs directory does not exist, skipping build history test"
        update_validation_result "build_history" "warning" "Jobs directory missing"
        return 0
    fi
    
    for job_dir in "$jobs_dir"/*; do
        if [[ -d "$job_dir" ]]; then
            local builds_dir="$job_dir/builds"
            if [[ -d "$builds_dir" ]]; then
                local build_count
                build_count=$(find "$builds_dir" -maxdepth 1 -type d | wc -l)
                if [[ $build_count -gt 1 ]]; then  # Subtract 1 for the builds directory itself
                    ((jobs_with_builds++))
                    total_builds=$((total_builds + build_count - 1))
                fi
            fi
        fi
    done
    
    if [[ $total_builds -eq 0 ]]; then
        log "⚠ No build history found"
        update_validation_result "build_history" "warning" "No build history found"
        add_issue "info" "No build history found" "This is normal for a fresh installation"
        return 0
    else
        log "✓ Found $total_builds builds across $jobs_with_builds jobs"
        update_validation_result "build_history" "pass" "$total_builds builds found across $jobs_with_builds jobs"
        return 0
    fi
}

# Test system configuration
test_system_configuration() {
    log "Testing Jenkins system configuration..."
    
    local config_xml="$JENKINS_HOME/config.xml"
    
    if [[ ! -f "$config_xml" ]]; then
        log "✗ Main configuration file missing: $config_xml"
        update_validation_result "system_configuration" "fail" "Main configuration file missing"
        add_issue "critical" "Main configuration file missing" "Restore config.xml from backup"
        return 1
    fi
    
    # Check if config.xml is valid XML
    if ! xmllint --noout "$config_xml" 2>/dev/null; then
        log "✗ Main configuration file is invalid XML"
        update_validation_result "system_configuration" "fail" "Main configuration file is invalid XML"
        add_issue "critical" "Invalid main configuration file" "Restore valid config.xml from backup"
        return 1
    fi
    
    # Check for essential configuration elements
    local essential_elements=(
        "hudson"
        "version"
        "numExecutors"
    )
    
    local missing_elements=()
    
    for element in "${essential_elements[@]}"; do
        if ! xmllint --xpath "//$element" "$config_xml" &>/dev/null; then
            missing_elements+=("$element")
        fi
    done
    
    if [[ ${#missing_elements[@]} -eq 0 ]]; then
        log "✓ System configuration is valid"
        update_validation_result "system_configuration" "pass" "System configuration validated successfully"
        return 0
    else
        log "✗ System configuration missing essential elements: ${missing_elements[*]}"
        update_validation_result "system_configuration" "fail" "Missing essential configuration elements"
        for element in "${missing_elements[@]}"; do
            add_issue "high" "Missing configuration element: $element" "Restore complete configuration from backup"
        done
        return 1
    fi
}

# Run all validation tests
run_all_tests() {
    log "Starting comprehensive Jenkins recovery validation..."
    
    local test_results=()
    
    # Run all tests
    test_service_status && test_results+=("service_status:pass") || test_results+=("service_status:fail")
    test_web_interface && test_results+=("web_interface:pass") || test_results+=("web_interface:fail")
    test_configuration_files && test_results+=("configuration_files:pass") || test_results+=("configuration_files:fail")
    test_job_configurations && test_results+=("job_configurations:pass") || test_results+=("job_configurations:fail")
    test_user_accounts && test_results+=("user_accounts:pass") || test_results+=("user_accounts:fail")
    test_plugin_status && test_results+=("plugin_status:pass") || test_results+=("plugin_status:fail")
    test_build_history && test_results+=("build_history:pass") || test_results+=("build_history:fail")
    test_system_configuration && test_results+=("system_configuration:pass") || test_results+=("system_configuration:fail")
    
    # Count results
    local pass_count=0
    local fail_count=0
    local warning_count=0
    
    for result in "${test_results[@]}"; do
        case "$result" in
            *:pass) ((pass_count++)) ;;
            *:fail) ((fail_count++)) ;;
            *:warning) ((warning_count++)) ;;
        esac
    done
    
    log "Validation completed: $pass_count passed, $fail_count failed, $warning_count warnings"
    
    # Determine overall status
    local overall_status
    if [[ $fail_count -eq 0 ]]; then
        if [[ $warning_count -eq 0 ]]; then
            overall_status="pass"
            log "✓ All validation tests passed"
        else
            overall_status="pass_with_warnings"
            log "⚠ All critical tests passed, but there are warnings"
        fi
    else
        overall_status="fail"
        log "✗ $fail_count validation tests failed"
    fi
    
    finalize_validation_report "$overall_status"
    
    return $fail_count
}

# Display validation report
display_report() {
    log "Validation report saved to: $VALIDATION_REPORT"
    
    if command -v jq &> /dev/null; then
        echo "=== VALIDATION REPORT SUMMARY ==="
        jq -r '
            "Overall Status: " + .overall_status,
            "Validation Time: " + .validation_timestamp,
            "",
            "Test Results:",
            (.validation_results | to_entries[] | "  " + .key + ": " + .value.status),
            "",
            "Issues Found: " + (.issues_found | length | tostring),
            (.issues_found[] | "  [" + .severity + "] " + .description)
        ' "$VALIDATION_REPORT"
    else
        log "Install 'jq' to see formatted validation report"
        log "Raw report available at: $VALIDATION_REPORT"
    fi
}

# Main execution
main() {
    local show_report=true
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --no-report)
                show_report=false
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --no-report    Don't display the validation report"
                echo "  -h, --help     Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting Jenkins recovery validation"
    
    # Initialize validation report
    init_validation_report
    
    # Run all validation tests
    if run_all_tests; then
        log "Jenkins recovery validation completed successfully"
        exit_code=0
    else
        log "Jenkins recovery validation completed with failures"
        exit_code=1
    fi
    
    # Display report if requested
    if [[ "$show_report" == true ]]; then
        display_report
    fi
    
    exit $exit_code
}

# Execute main function
main "$@"