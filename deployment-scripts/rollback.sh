#!/bin/bash
set -e

# Pet Clinic Rollback Script
# This script rolls back to the previous version of the application

# Configuration
APP_TYPE="${1:-both}"  # backend, frontend, or both
DEPLOY_DIR="/opt/pet-clinic"
BACKUP_DIR="/opt/pet-clinic/backups"
SERVICE_USER="petclinic"
LOG_DIR="/var/log/pet-clinic"

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

# Validate app type parameter
if [[ "$APP_TYPE" != "backend" && "$APP_TYPE" != "frontend" && "$APP_TYPE" != "both" ]]; then
    error "Invalid app type. Use: backend, frontend, or both"
fi

log "Starting rollback for: $APP_TYPE"

# Function to rollback backend
rollback_backend() {
    log "Rolling back backend application"
    
    # Find latest backup
    local latest_backup
    latest_backup=$(ls -t "$BACKUP_DIR"/pet-clinic-backend-backup-*.jar 2>/dev/null | head -1)
    
    if [[ -z "$latest_backup" ]]; then
        error "No backend backup found for rollback"
    fi
    
    log "Found backup: $(basename "$latest_backup")"
    
    # Stop current backend
    log "Stopping backend service"
    if systemctl is-active --quiet pet-clinic-backend; then
        sudo systemctl stop pet-clinic-backend
    fi
    
    # Backup current version before rollback
    if [[ -f "$DEPLOY_DIR/current.jar" ]]; then
        local rollback_backup="$BACKUP_DIR/pet-clinic-backend-pre-rollback-$(date +%Y%m%d-%H%M%S).jar"
        cp "$DEPLOY_DIR/current.jar" "$rollback_backup"
        log "Current version backed up as: $(basename "$rollback_backup")"
    fi
    
    # Restore from backup
    cp "$latest_backup" "$DEPLOY_DIR/current.jar"
    chown "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_DIR/current.jar"
    
    # Start backend service
    log "Starting backend service"
    sudo systemctl start pet-clinic-backend
    
    # Wait for service to be ready
    local health_url="http://localhost:8080/api/actuator/health"
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "$health_url" > /dev/null 2>&1; then
            log "Backend rollback completed successfully"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            error "Backend failed to start after rollback"
        fi
        
        log "Attempt $attempt/$max_attempts: Waiting for backend to be ready..."
        sleep 10
        ((attempt++))
    done
}

# Function to rollback frontend
rollback_frontend() {
    log "Rolling back frontend application"
    
    # Find latest backup
    local latest_backup
    latest_backup=$(ls -t "$BACKUP_DIR"/pet-clinic-frontend-backup-*.jar 2>/dev/null | head -1)
    
    if [[ -z "$latest_backup" ]]; then
        error "No frontend backup found for rollback"
    fi
    
    log "Found backup: $(basename "$latest_backup")"
    
    # Stop current frontend
    log "Stopping frontend service"
    if systemctl is-active --quiet pet-clinic-frontend; then
        sudo systemctl stop pet-clinic-frontend
    fi
    
    # Backup current version before rollback
    if [[ -f "$DEPLOY_DIR/frontend-current.jar" ]]; then
        local rollback_backup="$BACKUP_DIR/pet-clinic-frontend-pre-rollback-$(date +%Y%m%d-%H%M%S).jar"
        cp "$DEPLOY_DIR/frontend-current.jar" "$rollback_backup"
        log "Current version backed up as: $(basename "$rollback_backup")"
    fi
    
    # Restore from backup
    cp "$latest_backup" "$DEPLOY_DIR/frontend-current.jar"
    chown "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_DIR/frontend-current.jar"
    
    # Start frontend service
    log "Starting frontend service"
    sudo systemctl start pet-clinic-frontend
    
    # Wait for service to be ready
    local health_url="http://localhost:8081/actuator/health"
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "$health_url" > /dev/null 2>&1; then
            log "Frontend rollback completed successfully"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            error "Frontend failed to start after rollback"
        fi
        
        log "Attempt $attempt/$max_attempts: Waiting for frontend to be ready..."
        sleep 10
        ((attempt++))
    done
}

# Execute rollback based on app type
case "$APP_TYPE" in
    "backend")
        rollback_backend
        ;;
    "frontend")
        rollback_frontend
        ;;
    "both")
        rollback_backend
        rollback_frontend
        ;;
esac

# Run health check after rollback
log "Running health check after rollback"
if [[ -f "$DEPLOY_DIR/../deployment-scripts/health-check.sh" ]]; then
    bash "$DEPLOY_DIR/../deployment-scripts/health-check.sh" || warn "Health check reported issues after rollback"
else
    warn "Health check script not found, skipping post-rollback verification"
fi

log "✓ Rollback completed for: $APP_TYPE"
log "Check application logs for any issues: $LOG_DIR/"