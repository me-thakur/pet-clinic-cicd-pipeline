#!/bin/bash
set -e

# Pet Clinic Health Check Script
# This script performs comprehensive health checks on the deployed application

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080/api}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:8081}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-petclinic}"
DB_USER="${DB_USER:-petclinic}"
DB_PASSWORD="${DB_PASSWORD}"

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

# Health check results
HEALTH_CHECKS=()
FAILED_CHECKS=0

add_check_result() {
    local check_name="$1"
    local result="$2"
    local details="$3"
    
    HEALTH_CHECKS+=("$check_name|$result|$details")
    if [[ "$result" == "FAIL" ]]; then
        ((FAILED_CHECKS++))
    fi
}

# Check if service is running
check_service_status() {
    local service_name="$1"
    info "Checking $service_name service status"
    
    if systemctl is-active --quiet "$service_name"; then
        success "$service_name is running"
        add_check_result "$service_name Service" "PASS" "Service is active"
        return 0
    else
        failure "$service_name is not running"
        add_check_result "$service_name Service" "FAIL" "Service is not active"
        return 1
    fi
}

# Check HTTP endpoint
check_http_endpoint() {
    local name="$1"
    local url="$2"
    local expected_status="${3:-200}"
    
    info "Checking $name endpoint: $url"
    
    local response
    local status_code
    
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" "$url" 2>/dev/null || echo "HTTPSTATUS:000")
    status_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    if [[ "$status_code" == "$expected_status" ]]; then
        success "$name endpoint is healthy (HTTP $status_code)"
        add_check_result "$name Endpoint" "PASS" "HTTP $status_code"
        return 0
    else
        failure "$name endpoint failed (HTTP $status_code)"
        add_check_result "$name Endpoint" "FAIL" "HTTP $status_code"
        return 1
    fi
}

# Check JSON API endpoint
check_json_endpoint() {
    local name="$1"
    local url="$2"
    local expected_field="$3"
    local expected_value="$4"
    
    info "Checking $name JSON endpoint: $url"
    
    local response
    response=$(curl -s "$url" 2>/dev/null || echo '{}')
    
    if echo "$response" | jq -e ".$expected_field" > /dev/null 2>&1; then
        local actual_value
        actual_value=$(echo "$response" | jq -r ".$expected_field")
        
        if [[ "$actual_value" == "$expected_value" ]]; then
            success "$name JSON endpoint is healthy ($expected_field: $actual_value)"
            add_check_result "$name JSON API" "PASS" "$expected_field: $actual_value"
            return 0
        else
            failure "$name JSON endpoint returned unexpected value ($expected_field: $actual_value, expected: $expected_value)"
            add_check_result "$name JSON API" "FAIL" "$expected_field: $actual_value (expected: $expected_value)"
            return 1
        fi
    else
        failure "$name JSON endpoint failed or returned invalid JSON"
        add_check_result "$name JSON API" "FAIL" "Invalid JSON response or missing field"
        return 1
    fi
}

# Check database connectivity
check_database() {
    info "Checking database connectivity"
    
    if [[ -z "$DB_PASSWORD" ]]; then
        warn "DB_PASSWORD not set, skipping database check"
        add_check_result "Database" "SKIP" "DB_PASSWORD not provided"
        return 0
    fi
    
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT 1;" > /dev/null 2>&1; then
        success "Database is accessible"
        
        # Check table counts
        local owner_count
        local pet_count
        owner_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM owners;" 2>/dev/null || echo "0")
        pet_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM pets;" 2>/dev/null || echo "0")
        
        add_check_result "Database" "PASS" "Connected, $owner_count owners, $pet_count pets"
        return 0
    else
        failure "Database is not accessible"
        add_check_result "Database" "FAIL" "Connection failed"
        return 1
    fi
}

# Check application logs for errors
check_application_logs() {
    local app_name="$1"
    local log_file="$2"
    
    info "Checking $app_name application logs"
    
    if [[ ! -f "$log_file" ]]; then
        warn "$app_name log file not found: $log_file"
        add_check_result "$app_name Logs" "SKIP" "Log file not found"
        return 0
    fi
    
    # Check for recent errors (last 100 lines)
    local error_count
    error_count=$(tail -100 "$log_file" | grep -i "error\|exception\|failed" | wc -l)
    
    if [[ "$error_count" -eq 0 ]]; then
        success "$app_name logs show no recent errors"
        add_check_result "$app_name Logs" "PASS" "No recent errors"
        return 0
    else
        warn "$app_name logs show $error_count recent errors"
        add_check_result "$app_name Logs" "WARN" "$error_count recent errors"
        return 1
    fi
}

# Check disk space
check_disk_space() {
    info "Checking disk space"
    
    local usage
    usage=$(df /opt/pet-clinic | awk 'NR==2 {print $5}' | sed 's/%//')
    
    if [[ "$usage" -lt 80 ]]; then
        success "Disk space is adequate ($usage% used)"
        add_check_result "Disk Space" "PASS" "$usage% used"
        return 0
    elif [[ "$usage" -lt 90 ]]; then
        warn "Disk space is getting low ($usage% used)"
        add_check_result "Disk Space" "WARN" "$usage% used"
        return 1
    else
        failure "Disk space is critically low ($usage% used)"
        add_check_result "Disk Space" "FAIL" "$usage% used"
        return 1
    fi
}

# Check memory usage
check_memory_usage() {
    info "Checking memory usage"
    
    local mem_usage
    mem_usage=$(free | awk 'NR==2{printf "%.0f", $3*100/$2}')
    
    if [[ "$mem_usage" -lt 80 ]]; then
        success "Memory usage is normal ($mem_usage% used)"
        add_check_result "Memory Usage" "PASS" "$mem_usage% used"
        return 0
    elif [[ "$mem_usage" -lt 90 ]]; then
        warn "Memory usage is high ($mem_usage% used)"
        add_check_result "Memory Usage" "WARN" "$mem_usage% used"
        return 1
    else
        failure "Memory usage is critically high ($mem_usage% used)"
        add_check_result "Memory Usage" "FAIL" "$mem_usage% used"
        return 1
    fi
}

# Main health check execution
main() {
    log "Starting Pet Clinic health check"
    
    # Service status checks
    check_service_status "pet-clinic-backend"
    check_service_status "pet-clinic-frontend"
    
    # HTTP endpoint checks
    check_http_endpoint "Backend Health" "$BACKEND_URL/actuator/health"
    check_http_endpoint "Frontend Health" "$FRONTEND_URL/actuator/health"
    check_http_endpoint "Frontend Web" "$FRONTEND_URL/"
    
    # JSON API checks
    check_json_endpoint "Backend Health Status" "$BACKEND_URL/actuator/health" "status" "UP"
    check_json_endpoint "Frontend Health Status" "$FRONTEND_URL/actuator/health" "status" "UP"
    
    # Database check
    check_database
    
    # Log checks
    check_application_logs "Backend" "/var/log/pet-clinic/backend.log"
    check_application_logs "Frontend" "/var/log/pet-clinic/frontend.log"
    
    # System resource checks
    check_disk_space
    check_memory_usage
    
    # Generate health report
    log "Health Check Summary"
    echo "=================================="
    printf "%-25s %-6s %s\n" "Check" "Result" "Details"
    echo "=================================="
    
    for check in "${HEALTH_CHECKS[@]}"; do
        IFS='|' read -r name result details <<< "$check"
        
        case "$result" in
            "PASS") printf "%-25s ${GREEN}%-6s${NC} %s\n" "$name" "$result" "$details" ;;
            "WARN") printf "%-25s ${YELLOW}%-6s${NC} %s\n" "$name" "$result" "$details" ;;
            "FAIL") printf "%-25s ${RED}%-6s${NC} %s\n" "$name" "$result" "$details" ;;
            "SKIP") printf "%-25s ${BLUE}%-6s${NC} %s\n" "$name" "$result" "$details" ;;
        esac
    done
    
    echo "=================================="
    
    if [[ "$FAILED_CHECKS" -eq 0 ]]; then
        success "All health checks passed!"
        log "Pet Clinic application is healthy and ready"
        exit 0
    else
        failure "$FAILED_CHECKS health check(s) failed"
        error "Pet Clinic application has health issues"
        exit 1
    fi
}

# Check if jq is available for JSON parsing
if ! command -v jq &> /dev/null; then
    warn "jq not found, installing..."
    sudo yum install -y jq || sudo apt-get install -y jq || warn "Could not install jq, JSON checks may fail"
fi

# Run main health check
main "$@"