#!/bin/bash
set -e

# Pet Clinic Backend Deployment Script
# This script deploys the backend application to EC2 instances

# Configuration
APP_NAME="pet-clinic-backend"
APP_VERSION="${BUILD_NUMBER:-latest}"
DEPLOY_DIR="/opt/pet-clinic"
SERVICE_USER="petclinic"
LOG_DIR="/var/log/pet-clinic"
BACKUP_DIR="/opt/pet-clinic/backups"

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

# Validate required environment variables
required_vars=("DB_HOST" "DB_NAME" "DB_USER" "DB_PASSWORD" "AWS_REGION")
for var in "${required_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        error "Required environment variable $var is not set"
    fi
done

log "Starting deployment of $APP_NAME version $APP_VERSION"

# Create necessary directories
log "Creating deployment directories"
sudo mkdir -p "$DEPLOY_DIR" "$LOG_DIR" "$BACKUP_DIR"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_DIR" "$LOG_DIR" "$BACKUP_DIR"

# Download application artifact from S3
log "Downloading application artifact from S3"
ARTIFACT_URL="s3://pet-clinic-artifacts/backend/${APP_NAME}-${APP_VERSION}.jar"
aws s3 cp "$ARTIFACT_URL" "$DEPLOY_DIR/${APP_NAME}-${APP_VERSION}.jar" || error "Failed to download artifact"

# Backup current version if it exists
if [[ -f "$DEPLOY_DIR/current.jar" ]]; then
    log "Backing up current version"
    BACKUP_NAME="${APP_NAME}-backup-$(date +%Y%m%d-%H%M%S).jar"
    cp "$DEPLOY_DIR/current.jar" "$BACKUP_DIR/$BACKUP_NAME"
    
    # Keep only last 5 backups
    cd "$BACKUP_DIR"
    ls -t ${APP_NAME}-backup-*.jar | tail -n +6 | xargs -r rm
fi

# Stop current application if running
log "Stopping current application"
if systemctl is-active --quiet pet-clinic-backend; then
    sudo systemctl stop pet-clinic-backend
    log "Application stopped"
else
    log "Application was not running"
fi

# Update symlink to new version
log "Updating application symlink"
ln -sf "$DEPLOY_DIR/${APP_NAME}-${APP_VERSION}.jar" "$DEPLOY_DIR/current.jar"

# Update application configuration
log "Updating application configuration"
cat > "$DEPLOY_DIR/application.yml" << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/${DB_NAME}?useSSL=true&serverTimezone=UTC
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

logging:
  level:
    com.petclinic: INFO
    org.springframework.security: DEBUG
  file:
    name: ${LOG_DIR}/backend.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

aws:
  region: ${AWS_REGION}
EOF

# Set proper permissions
sudo chown "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_DIR/application.yml"
sudo chmod 640 "$DEPLOY_DIR/application.yml"

# Run database migrations
log "Running database migrations"
cd "$DEPLOY_DIR"
java -jar current.jar --spring.flyway.migrate=true --spring.profiles.active=migration || error "Database migration failed"

# Start application
log "Starting application"
sudo systemctl start pet-clinic-backend || error "Failed to start application"

# Wait for application to be ready
log "Waiting for application to be ready"
HEALTH_URL="http://localhost:8080/api/actuator/health"
MAX_ATTEMPTS=30
ATTEMPT=1

while [[ $ATTEMPT -le $MAX_ATTEMPTS ]]; do
    if curl -f -s "$HEALTH_URL" > /dev/null 2>&1; then
        log "Application is healthy and ready"
        break
    fi
    
    if [[ $ATTEMPT -eq $MAX_ATTEMPTS ]]; then
        error "Application failed to become healthy within timeout"
    fi
    
    log "Attempt $ATTEMPT/$MAX_ATTEMPTS: Application not ready yet, waiting..."
    sleep 10
    ((ATTEMPT++))
done

# Verify deployment
log "Verifying deployment"
HEALTH_RESPONSE=$(curl -s "$HEALTH_URL")
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    log "✓ Health check passed"
else
    error "Health check failed: $HEALTH_RESPONSE"
fi

# Test database connectivity
log "Testing database connectivity"
DB_HEALTH_URL="http://localhost:8080/api/actuator/health/db"
DB_RESPONSE=$(curl -s "$DB_HEALTH_URL")
if echo "$DB_RESPONSE" | grep -q '"status":"UP"'; then
    log "✓ Database connectivity verified"
else
    warn "Database health check returned: $DB_RESPONSE"
fi

# Enable service for auto-start
sudo systemctl enable pet-clinic-backend

log "✓ Backend deployment completed successfully"
log "Application version: $APP_VERSION"
log "Health endpoint: $HEALTH_URL"
log "Logs location: $LOG_DIR/backend.log"