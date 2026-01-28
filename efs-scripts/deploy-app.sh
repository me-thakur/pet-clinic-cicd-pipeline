#!/bin/bash
# Application Deployment Script for Pet Clinic CI/CD Pipeline
# This script deploys the Pet Clinic application to EC2 instances

set -e

# Configuration
APP_USER="petclinic"
APP_HOME="/home/${APP_USER}/app"
APP_NAME="pet-clinic"
BACKEND_PORT="8081"
FRONTEND_PORT="8080"
LOG_DIR="/var/log/petclinic"

# Log function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Usage function
usage() {
    echo "Usage: $0 [backend|frontend] <jar-file> [environment]"
    echo "  backend|frontend: Application type to deploy"
    echo "  jar-file: Path to the JAR file to deploy"
    echo "  environment: Environment (dev|staging|prod) - optional, defaults to dev"
    exit 1
}

# Check arguments
if [ $# -lt 2 ]; then
    usage
fi

APP_TYPE="$1"
JAR_FILE="$2"
ENVIRONMENT="${3:-dev}"

# Validate app type
if [ "$APP_TYPE" != "backend" ] && [ "$APP_TYPE" != "frontend" ]; then
    log "ERROR: Invalid application type. Must be 'backend' or 'frontend'"
    usage
fi

# Validate JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    log "ERROR: JAR file not found: $JAR_FILE"
    exit 1
fi

log "Starting deployment of $APP_TYPE application..."
log "JAR file: $JAR_FILE"
log "Environment: $ENVIRONMENT"

# Create application user if it doesn't exist
if ! id "$APP_USER" &>/dev/null; then
    log "Creating application user: $APP_USER"
    useradd -m -s /bin/bash "$APP_USER"
fi

# Create application directories
log "Creating application directories..."
mkdir -p "$APP_HOME"
mkdir -p "$LOG_DIR"
chown -R "$APP_USER:$APP_USER" "$APP_HOME"
chown -R "$APP_USER:$APP_USER" "$LOG_DIR"

# Set port based on application type
if [ "$APP_TYPE" = "backend" ]; then
    APP_PORT="$BACKEND_PORT"
else
    APP_PORT="$FRONTEND_PORT"
fi

# Stop existing application
log "Stopping existing $APP_TYPE application..."
if systemctl is-active --quiet "petclinic-$APP_TYPE"; then
    systemctl stop "petclinic-$APP_TYPE"
fi

# Copy JAR file
log "Copying JAR file to application directory..."
cp "$JAR_FILE" "$APP_HOME/${APP_NAME}-${APP_TYPE}.jar"
chown "$APP_USER:$APP_USER" "$APP_HOME/${APP_NAME}-${APP_TYPE}.jar"

# Create application configuration
log "Creating application configuration..."
cat > "$APP_HOME/application-${ENVIRONMENT}.yml" << EOF
server:
  port: ${APP_PORT}

spring:
  profiles:
    active: ${ENVIRONMENT}
  
  datasource:
    url: jdbc:mysql://\${DB_HOST:localhost}:3306/petclinic
    username: \${DB_USERNAME:petclinic}
    password: \${DB_PASSWORD:petclinic}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

logging:
  level:
    com.petclinic: INFO
  file:
    name: ${LOG_DIR}/${APP_TYPE}.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
EOF

chown "$APP_USER:$APP_USER" "$APP_HOME/application-${ENVIRONMENT}.yml"

# Create systemd service
log "Creating systemd service for $APP_TYPE..."
cat > "/etc/systemd/system/petclinic-${APP_TYPE}.service" << EOF
[Unit]
Description=Pet Clinic ${APP_TYPE^} Application
After=network.target

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_HOME}
ExecStart=/usr/bin/java -jar ${APP_HOME}/${APP_NAME}-${APP_TYPE}.jar --spring.config.location=${APP_HOME}/application-${ENVIRONMENT}.yml
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=petclinic-${APP_TYPE}

Environment=JAVA_HOME=/usr/lib/jvm/java-11-amazon-corretto
Environment=SPRING_PROFILES_ACTIVE=${ENVIRONMENT}
Environment=SERVER_PORT=${APP_PORT}

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and start service
log "Starting $APP_TYPE service..."
systemctl daemon-reload
systemctl enable "petclinic-${APP_TYPE}"
systemctl start "petclinic-${APP_TYPE}"

# Wait for application to start
log "Waiting for application to start..."
sleep 30

# Health check
log "Performing health check..."
for i in {1..10}; do
    if curl -f "http://localhost:${APP_PORT}/actuator/health" &>/dev/null; then
        log "Health check passed!"
        break
    fi
    if [ $i -eq 10 ]; then
        log "ERROR: Health check failed after 10 attempts"
        systemctl status "petclinic-${APP_TYPE}"
        exit 1
    fi
    log "Health check attempt $i failed, retrying in 10 seconds..."
    sleep 10
done

# Display service status
log "Service status:"
systemctl status "petclinic-${APP_TYPE}" --no-pager

log "$APP_TYPE deployment completed successfully!"
log "Application is running on port $APP_PORT"
log "Logs are available at: $LOG_DIR/${APP_TYPE}.log"
log "Service name: petclinic-${APP_TYPE}"