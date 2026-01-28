#!/bin/bash
# Health Check Script for Pet Clinic CI/CD Pipeline
# This script performs comprehensive health checks on deployed applications

set -e

# Configuration
BACKEND_PORT="8081"
FRONTEND_PORT="8080"
TIMEOUT=30
MAX_RETRIES=5

# Log function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Usage function
usage() {
    echo "Usage: $0 [backend|frontend|all] [--timeout=seconds] [--retries=count]"
    echo "  backend|frontend|all: Application(s) to check"
    echo "  --timeout: HTTP request timeout in seconds (default: 30)"
    echo "  --retries: Maximum number of retries (default: 5)"
    exit 1
}

# Parse arguments
APP_TYPE="all"
while [[ $# -gt 0 ]]; do
    case $1 in
        backend|frontend|all)
            APP_TYPE="$1"
            shift
            ;;
        --timeout=*)
            TIMEOUT="${1#*=}"
            shift
            ;;
        --retries=*)
            MAX_RETRIES="${1#*=}"
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            log "ERROR: Unknown option $1"
            usage
            ;;
    esac
done

# Health check function
check_health() {
    local app_name="$1"
    local port="$2"
    local health_url="http://localhost:${port}/actuator/health"
    
    log "Checking health of $app_name on port $port..."
    
    # Check if service is running
    if ! systemctl is-active --quiet "petclinic-${app_name}"; then
        log "ERROR: Service petclinic-${app_name} is not running"
        return 1
    fi
    
    # Check if port is listening
    if ! netstat -tuln | grep -q ":${port} "; then
        log "ERROR: Port $port is not listening"
        return 1
    fi
    
    # Perform HTTP health check with retries
    local retry_count=0
    while [ $retry_count -lt $MAX_RETRIES ]; do
        log "Health check attempt $((retry_count + 1))/$MAX_RETRIES for $app_name..."
        
        if curl -f -s --connect-timeout "$TIMEOUT" "$health_url" > /tmp/health_response.json; then
            local status=$(cat /tmp/health_response.json | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            if [ "$status" = "UP" ]; then
                log "✓ $app_name is healthy (status: $status)"
                
                # Additional checks
                check_application_metrics "$app_name" "$port"
                check_database_connectivity "$app_name" "$port"
                
                return 0
            else
                log "⚠ $app_name status: $status"
            fi
        else
            log "✗ Health check failed for $app_name (attempt $((retry_count + 1)))"
        fi
        
        retry_count=$((retry_count + 1))
        if [ $retry_count -lt $MAX_RETRIES ]; then
            sleep 10
        fi
    done
    
    log "ERROR: Health check failed for $app_name after $MAX_RETRIES attempts"
    
    # Show service logs for debugging
    log "Recent logs for petclinic-${app_name}:"
    journalctl -u "petclinic-${app_name}" --no-pager -n 20
    
    return 1
}

# Check application metrics
check_application_metrics() {
    local app_name="$1"
    local port="$2"
    local metrics_url="http://localhost:${port}/actuator/metrics"
    
    log "Checking metrics for $app_name..."
    
    if curl -f -s --connect-timeout 10 "$metrics_url" > /tmp/metrics_response.json; then
        local metric_count=$(cat /tmp/metrics_response.json | grep -o '"name":"[^"]*"' | wc -l)
        log "✓ Metrics endpoint accessible ($metric_count metrics available)"
        
        # Check specific metrics
        if curl -f -s --connect-timeout 10 "${metrics_url}/jvm.memory.used" > /dev/null; then
            log "✓ JVM metrics available"
        fi
        
        if curl -f -s --connect-timeout 10 "${metrics_url}/http.server.requests" > /dev/null; then
            log "✓ HTTP request metrics available"
        fi
    else
        log "⚠ Metrics endpoint not accessible for $app_name"
    fi
}

# Check database connectivity (for backend only)
check_database_connectivity() {
    local app_name="$1"
    local port="$2"
    
    if [ "$app_name" = "backend" ]; then
        log "Checking database connectivity for $app_name..."
        
        # Try to access a simple endpoint that requires database
        local db_check_url="http://localhost:${port}/api/owners"
        
        if curl -f -s --connect-timeout 10 "$db_check_url" > /dev/null; then
            log "✓ Database connectivity verified"
        else
            log "⚠ Database connectivity check failed"
        fi
    fi
}

# System resource check
check_system_resources() {
    log "Checking system resources..."
    
    # Memory usage
    local mem_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
    log "Memory usage: ${mem_usage}%"
    
    # Disk usage
    local disk_usage=$(df -h / | awk 'NR==2{printf "%s", $5}')
    log "Disk usage: $disk_usage"
    
    # CPU load
    local cpu_load=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
    log "CPU load (1min): $cpu_load"
    
    # Check if resources are within acceptable limits
    if (( $(echo "$mem_usage > 90" | bc -l) )); then
        log "⚠ High memory usage: ${mem_usage}%"
    fi
    
    local disk_usage_num=$(echo "$disk_usage" | sed 's/%//')
    if [ "$disk_usage_num" -gt 90 ]; then
        log "⚠ High disk usage: $disk_usage"
    fi
}

# Main execution
log "Starting health check for $APP_TYPE..."

# Check system resources
check_system_resources

# Perform health checks based on app type
overall_status=0

case $APP_TYPE in
    "backend")
        if ! check_health "backend" "$BACKEND_PORT"; then
            overall_status=1
        fi
        ;;
    "frontend")
        if ! check_health "frontend" "$FRONTEND_PORT"; then
            overall_status=1
        fi
        ;;
    "all")
        if ! check_health "backend" "$BACKEND_PORT"; then
            overall_status=1
        fi
        if ! check_health "frontend" "$FRONTEND_PORT"; then
            overall_status=1
        fi
        ;;
esac

# Clean up temporary files
rm -f /tmp/health_response.json /tmp/metrics_response.json

# Final status
if [ $overall_status -eq 0 ]; then
    log "✓ All health checks passed successfully!"
else
    log "✗ Some health checks failed!"
fi

exit $overall_status