# Pet Clinic Management System - Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the Pet Clinic Management System in various environments, from local development to production deployment on cloud platforms.

## Prerequisites

### System Requirements

#### Minimum Requirements
- **CPU**: 2 cores
- **RAM**: 4GB
- **Storage**: 20GB available space
- **Network**: Stable internet connection

#### Recommended Requirements (Production)
- **CPU**: 4+ cores
- **RAM**: 8GB+
- **Storage**: 50GB+ SSD
- **Network**: High-speed internet with low latency

### Software Dependencies

#### Required Software
- **Java**: OpenJDK 11 or higher
- **Maven**: 3.6.0 or higher
- **MySQL**: 8.0 or higher
- **Git**: Latest version

#### Optional Software
- **Docker**: 20.10+ (for containerized deployment)
- **Docker Compose**: 1.29+ (for multi-container setup)
- **Nginx**: Latest (for reverse proxy)
- **SSL Certificate**: For HTTPS in production

## Environment Setup

### 1. Local Development Environment

#### Quick Setup Script

```bash
#!/bin/bash
# Local development setup script

echo "Setting up Pet Clinic Management System - Local Development"

# Check Java installation
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install OpenJDK 11 or higher."
    exit 1
fi

# Check Maven installation
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven 3.6.0 or higher."
    exit 1
fi

# Clone repository (if not already cloned)
if [ ! -d "pet-clinic-management-system" ]; then
    git clone https://github.com/your-org/pet-clinic-management-system.git
    cd pet-clinic-management-system
else
    cd pet-clinic-management-system
    git pull origin main
fi

# Set up MySQL database
echo "Setting up MySQL database..."
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS petclinic;
CREATE USER IF NOT EXISTS 'petclinic'@'localhost' IDENTIFIED BY 'petclinic123';
GRANT ALL PRIVILEGES ON petclinic.* TO 'petclinic'@'localhost';
FLUSH PRIVILEGES;
EOF

# Copy configuration files
cp pet-clinic-app/pet-clinic-backend/src/main/resources/application-dev.yml.example \
   pet-clinic-app/pet-clinic-backend/src/main/resources/application-dev.yml

# Build the application
echo "Building the application..."
cd pet-clinic-app
mvn clean install -DskipTests

echo "Setup complete! You can now run the application with:"
echo "mvn spring-boot:run -pl pet-clinic-backend -Dspring.profiles.active=dev"
```

#### Manual Setup Steps

1. **Install Java 11+**
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install openjdk-11-jdk
   
   # macOS (using Homebrew)
   brew install openjdk@11
   
   # Verify installation
   java -version
   ```

2. **Install Maven**
   ```bash
   # Ubuntu/Debian
   sudo apt install maven
   
   # macOS (using Homebrew)
   brew install maven
   
   # Verify installation
   mvn -version
   ```

3. **Install MySQL**
   ```bash
   # Ubuntu/Debian
   sudo apt install mysql-server
   
   # macOS (using Homebrew)
   brew install mysql
   
   # Start MySQL service
   sudo systemctl start mysql  # Linux
   brew services start mysql   # macOS
   ```

4. **Configure Database**
   ```sql
   -- Connect to MySQL as root
   mysql -u root -p
   
   -- Create database and user
   CREATE DATABASE petclinic;
   CREATE USER 'petclinic'@'localhost' IDENTIFIED BY 'petclinic123';
   GRANT ALL PRIVILEGES ON petclinic.* TO 'petclinic'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   ```

5. **Configure Application**
   ```bash
   # Navigate to project directory
   cd pet-clinic-app/pet-clinic-backend/src/main/resources
   
   # Copy and edit configuration
   cp application-dev.yml.example application-dev.yml
   
   # Edit database connection settings
   nano application-dev.yml
   ```

   Update the database configuration:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/petclinic
       username: petclinic
       password: petclinic123
   ```

6. **Build and Run**
   ```bash
   # Build the application
   cd pet-clinic-app
   mvn clean install
   
   # Run the backend
   mvn spring-boot:run -pl pet-clinic-backend -Dspring.profiles.active=dev
   
   # In another terminal, run the frontend (optional)
   mvn spring-boot:run -pl pet-clinic-frontend -Dspring.profiles.active=dev
   ```

### 2. Docker Development Environment

#### Docker Compose Setup

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: petclinic-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: petclinic
      MYSQL_USER: petclinic
      MYSQL_PASSWORD: petclinic123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - petclinic-network

  backend:
    build:
      context: ./pet-clinic-app/pet-clinic-backend
      dockerfile: Dockerfile
    container_name: petclinic-backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/petclinic
      SPRING_DATASOURCE_USERNAME: petclinic
      SPRING_DATASOURCE_PASSWORD: petclinic123
    ports:
      - "9090:9090"
    depends_on:
      - mysql
    networks:
      - petclinic-network
    volumes:
      - ./logs:/app/logs

  frontend:
    build:
      context: ./pet-clinic-app/pet-clinic-frontend
      dockerfile: Dockerfile
    container_name: petclinic-frontend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      BACKEND_URL: http://backend:9090
    ports:
      - "8080:8080"
    depends_on:
      - backend
    networks:
      - petclinic-network

volumes:
  mysql_data:

networks:
  petclinic-network:
    driver: bridge
```

#### Backend Dockerfile

Create `pet-clinic-app/pet-clinic-backend/Dockerfile`:

```dockerfile
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy Maven dependencies (for better caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests

# Copy built JAR
RUN cp target/*.jar app.jar

# Expose port
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9090/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Run with Docker

```bash
# Build and start all services
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Production Deployment

### 1. Server Preparation

#### System Updates and Security

```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Install security updates
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades

# Configure firewall
sudo ufw enable
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 9090/tcp  # Application port

# Install fail2ban for SSH protection
sudo apt install fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

#### Install Required Software

```bash
# Install Java 11
sudo apt install openjdk-11-jdk

# Install MySQL
sudo apt install mysql-server
sudo mysql_secure_installation

# Install Nginx (reverse proxy)
sudo apt install nginx
sudo systemctl enable nginx
sudo systemctl start nginx

# Install certbot for SSL certificates
sudo apt install certbot python3-certbot-nginx
```

### 2. Database Setup (Production)

#### MySQL Configuration

```bash
# Connect to MySQL
sudo mysql -u root -p

# Create production database
CREATE DATABASE petclinic_prod;
CREATE USER 'petclinic_prod'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON petclinic_prod.* TO 'petclinic_prod'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### MySQL Optimization

Edit `/etc/mysql/mysql.conf.d/mysqld.cnf`:

```ini
[mysqld]
# Performance tuning
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT

# Connection settings
max_connections = 200
connect_timeout = 10
wait_timeout = 600
interactive_timeout = 600

# Query cache
query_cache_type = 1
query_cache_size = 128M
query_cache_limit = 2M

# Logging
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

Restart MySQL:
```bash
sudo systemctl restart mysql
```

### 3. Application Deployment

#### Create Application User

```bash
# Create dedicated user for the application
sudo useradd -r -m -U -d /opt/petclinic -s /bin/bash petclinic
sudo passwd petclinic

# Create application directories
sudo mkdir -p /opt/petclinic/{app,logs,config,backups}
sudo chown -R petclinic:petclinic /opt/petclinic
```

#### Deploy Application

```bash
# Switch to application user
sudo su - petclinic

# Clone repository
git clone https://github.com/your-org/pet-clinic-management-system.git
cd pet-clinic-management-system

# Build application
cd pet-clinic-app
mvn clean package -DskipTests

# Copy JAR to application directory
cp pet-clinic-backend/target/*.jar /opt/petclinic/app/petclinic-backend.jar
cp pet-clinic-frontend/target/*.jar /opt/petclinic/app/petclinic-frontend.jar
```

#### Production Configuration

Create `/opt/petclinic/config/application-prod.yml`:

```yaml
spring:
  profiles:
    active: prod
  
  datasource:
    url: jdbc:mysql://localhost:3306/petclinic_prod?useSSL=true&requireSSL=true
    username: petclinic_prod
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 9090
  servlet:
    context-path: /api

logging:
  level:
    com.petclinic: INFO
    org.springframework.security: WARN
  file:
    name: /opt/petclinic/logs/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

# Application-specific configuration
app:
  encryption:
    key: ${ENCRYPTION_KEY}
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600000
  session:
    timeout: 1800000
```

#### Environment Variables

Create `/opt/petclinic/config/environment`:

```bash
# Database configuration
export DB_PASSWORD="your_strong_database_password"

# Security configuration
export JWT_SECRET="your_jwt_secret_key_at_least_256_bits_long"
export ENCRYPTION_KEY="your_32_character_encryption_key"

# Application configuration
export SPRING_PROFILES_ACTIVE="prod"
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 4. Systemd Service Configuration

#### Backend Service

Create `/etc/systemd/system/petclinic-backend.service`:

```ini
[Unit]
Description=Pet Clinic Backend Service
After=network.target mysql.service
Requires=mysql.service

[Service]
Type=simple
User=petclinic
Group=petclinic
WorkingDirectory=/opt/petclinic
EnvironmentFile=/opt/petclinic/config/environment
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/petclinic/app/petclinic-backend.jar --spring.config.location=/opt/petclinic/config/application-prod.yml
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=petclinic-backend

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/petclinic/logs

[Install]
WantedBy=multi-user.target
```

#### Frontend Service

Create `/etc/systemd/system/petclinic-frontend.service`:

```ini
[Unit]
Description=Pet Clinic Frontend Service
After=network.target petclinic-backend.service
Requires=petclinic-backend.service

[Service]
Type=simple
User=petclinic
Group=petclinic
WorkingDirectory=/opt/petclinic
EnvironmentFile=/opt/petclinic/config/environment
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/petclinic/app/petclinic-frontend.jar --spring.config.location=/opt/petclinic/config/application-prod.yml --server.port=8080
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=petclinic-frontend

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/petclinic/logs

[Install]
WantedBy=multi-user.target
```

#### Enable and Start Services

```bash
# Reload systemd configuration
sudo systemctl daemon-reload

# Enable services to start on boot
sudo systemctl enable petclinic-backend
sudo systemctl enable petclinic-frontend

# Start services
sudo systemctl start petclinic-backend
sudo systemctl start petclinic-frontend

# Check service status
sudo systemctl status petclinic-backend
sudo systemctl status petclinic-frontend

# View logs
sudo journalctl -u petclinic-backend -f
sudo journalctl -u petclinic-frontend -f
```

### 5. Nginx Reverse Proxy Configuration

#### Main Configuration

Create `/etc/nginx/sites-available/petclinic`:

```nginx
# Rate limiting
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=web:10m rate=30r/s;

# Upstream servers
upstream backend {
    server 127.0.0.1:9090;
    keepalive 32;
}

upstream frontend {
    server 127.0.0.1:8080;
    keepalive 32;
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS server
server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL configuration
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    # Modern configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Referrer-Policy "strict-origin-when-cross-origin";

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/xml+rss application/json;

    # API endpoints
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # WebSocket support
    location /ws {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Frontend application
    location / {
        limit_req zone=web burst=50 nodelay;
        
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Static files caching
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
        proxy_pass http://frontend;
    }

    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass http://backend/api/actuator/health;
    }
}
```

#### Enable Site

```bash
# Enable the site
sudo ln -s /etc/nginx/sites-available/petclinic /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

### 6. SSL Certificate Setup

#### Using Let's Encrypt

```bash
# Install certbot
sudo apt install certbot python3-certbot-nginx

# Obtain SSL certificate
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# Test automatic renewal
sudo certbot renew --dry-run

# Set up automatic renewal
sudo crontab -e
# Add this line:
# 0 12 * * * /usr/bin/certbot renew --quiet
```

### 7. Monitoring and Logging

#### Log Rotation

Create `/etc/logrotate.d/petclinic`:

```
/opt/petclinic/logs/*.log {
    daily
    missingok
    rotate 52
    compress
    delaycompress
    notifempty
    create 644 petclinic petclinic
    postrotate
        systemctl reload petclinic-backend
        systemctl reload petclinic-frontend
    endscript
}
```

#### Health Check Script

Create `/opt/petclinic/scripts/health-check.sh`:

```bash
#!/bin/bash

# Health check script for Pet Clinic application

BACKEND_URL="http://localhost:9090/api/actuator/health"
FRONTEND_URL="http://localhost:8080/actuator/health"
LOG_FILE="/opt/petclinic/logs/health-check.log"

check_service() {
    local service_name=$1
    local url=$2
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" --max-time 10)
    
    if [ "$response" = "200" ]; then
        echo "$(date): $service_name is healthy" >> "$LOG_FILE"
        return 0
    else
        echo "$(date): $service_name is unhealthy (HTTP $response)" >> "$LOG_FILE"
        return 1
    fi
}

# Check backend
if ! check_service "Backend" "$BACKEND_URL"; then
    systemctl restart petclinic-backend
    echo "$(date): Restarted backend service" >> "$LOG_FILE"
fi

# Check frontend
if ! check_service "Frontend" "$FRONTEND_URL"; then
    systemctl restart petclinic-frontend
    echo "$(date): Restarted frontend service" >> "$LOG_FILE"
fi
```

Make it executable and add to cron:

```bash
chmod +x /opt/petclinic/scripts/health-check.sh

# Add to crontab
sudo crontab -e
# Add this line to run every 5 minutes:
# */5 * * * * /opt/petclinic/scripts/health-check.sh
```

## Cloud Deployment (AWS)

### 1. EC2 Instance Setup

#### Launch Instance

```bash
# Create EC2 instance using AWS CLI
aws ec2 run-instances \
    --image-id ami-0c02fb55956c7d316 \
    --count 1 \
    --instance-type t3.medium \
    --key-name your-key-pair \
    --security-group-ids sg-xxxxxxxxx \
    --subnet-id subnet-xxxxxxxxx \
    --user-data file://user-data.sh \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=PetClinic-Production}]'
```

#### User Data Script

Create `user-data.sh`:

```bash
#!/bin/bash
yum update -y
yum install -y java-11-openjdk-devel mysql nginx

# Install application
mkdir -p /opt/petclinic
cd /opt/petclinic
wget https://github.com/your-org/pet-clinic-management-system/releases/latest/download/petclinic-backend.jar
wget https://github.com/your-org/pet-clinic-management-system/releases/latest/download/petclinic-frontend.jar

# Configure and start services
systemctl enable petclinic-backend
systemctl enable petclinic-frontend
systemctl enable nginx
systemctl start petclinic-backend
systemctl start petclinic-frontend
systemctl start nginx
```

### 2. RDS Database Setup

```bash
# Create RDS MySQL instance
aws rds create-db-instance \
    --db-instance-identifier petclinic-prod \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --engine-version 8.0.35 \
    --master-username admin \
    --master-user-password YourStrongPassword \
    --allocated-storage 20 \
    --storage-type gp2 \
    --vpc-security-group-ids sg-xxxxxxxxx \
    --db-subnet-group-name your-db-subnet-group \
    --backup-retention-period 7 \
    --multi-az \
    --storage-encrypted
```

### 3. Load Balancer Setup

```bash
# Create Application Load Balancer
aws elbv2 create-load-balancer \
    --name petclinic-alb \
    --subnets subnet-xxxxxxxx subnet-yyyyyyyy \
    --security-groups sg-xxxxxxxxx \
    --scheme internet-facing \
    --type application \
    --ip-address-type ipv4
```

## Backup and Recovery

### 1. Database Backup

#### Automated Backup Script

Create `/opt/petclinic/scripts/backup-database.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/opt/petclinic/backups"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="petclinic_prod"
DB_USER="petclinic_prod"
DB_PASSWORD="your_password"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Create database backup
mysqldump -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" > "$BACKUP_DIR/petclinic_backup_$DATE.sql"

# Compress backup
gzip "$BACKUP_DIR/petclinic_backup_$DATE.sql"

# Remove backups older than 30 days
find "$BACKUP_DIR" -name "petclinic_backup_*.sql.gz" -mtime +30 -delete

echo "Database backup completed: petclinic_backup_$DATE.sql.gz"
```

#### Schedule Backup

```bash
# Add to crontab for daily backups at 2 AM
sudo crontab -e
# Add this line:
# 0 2 * * * /opt/petclinic/scripts/backup-database.sh
```

### 2. Application Backup

```bash
#!/bin/bash
# Application backup script

BACKUP_DIR="/opt/petclinic/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Backup configuration files
tar -czf "$BACKUP_DIR/config_backup_$DATE.tar.gz" /opt/petclinic/config/

# Backup logs (last 7 days)
find /opt/petclinic/logs -name "*.log" -mtime -7 -exec tar -czf "$BACKUP_DIR/logs_backup_$DATE.tar.gz" {} +

echo "Application backup completed"
```

## Troubleshooting

### Common Issues

#### 1. Application Won't Start

```bash
# Check service status
sudo systemctl status petclinic-backend
sudo systemctl status petclinic-frontend

# Check logs
sudo journalctl -u petclinic-backend -f
sudo journalctl -u petclinic-frontend -f

# Check application logs
tail -f /opt/petclinic/logs/application.log
```

#### 2. Database Connection Issues

```bash
# Test database connection
mysql -u petclinic_prod -p -h localhost petclinic_prod

# Check MySQL status
sudo systemctl status mysql

# Check MySQL logs
sudo tail -f /var/log/mysql/error.log
```

#### 3. High Memory Usage

```bash
# Check memory usage
free -h
ps aux --sort=-%mem | head

# Adjust JVM settings in environment file
export JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"

# Restart services
sudo systemctl restart petclinic-backend
sudo systemctl restart petclinic-frontend
```

#### 4. SSL Certificate Issues

```bash
# Check certificate status
sudo certbot certificates

# Renew certificate manually
sudo certbot renew

# Test Nginx configuration
sudo nginx -t

# Check SSL configuration
openssl s_client -connect your-domain.com:443
```

### Performance Tuning

#### JVM Tuning

```bash
# Production JVM settings
export JAVA_OPTS="-Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseJVMCICompiler \
    -XX:+PrintGC \
    -XX:+PrintGCDetails \
    -Xloggc:/opt/petclinic/logs/gc.log"
```

#### MySQL Tuning

```sql
-- Check current settings
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'max_connections';

-- Monitor performance
SHOW PROCESSLIST;
SHOW ENGINE INNODB STATUS;
```

## Security Checklist

### Pre-Deployment Security

- [ ] Update all system packages
- [ ] Configure firewall (UFW/iptables)
- [ ] Install and configure fail2ban
- [ ] Set up SSL certificates
- [ ] Configure secure database passwords
- [ ] Enable audit logging
- [ ] Set up intrusion detection
- [ ] Configure backup encryption

### Post-Deployment Security

- [ ] Regular security updates
- [ ] Monitor security logs
- [ ] Perform security scans
- [ ] Review access logs
- [ ] Update SSL certificates
- [ ] Backup verification
- [ ] Incident response plan
- [ ] Security training for staff

## Maintenance

### Regular Maintenance Tasks

#### Daily
- [ ] Check application health
- [ ] Review error logs
- [ ] Monitor disk space
- [ ] Verify backups

#### Weekly
- [ ] Update system packages
- [ ] Review security logs
- [ ] Performance monitoring
- [ ] Database maintenance

#### Monthly
- [ ] Security audit
- [ ] Backup testing
- [ ] Performance tuning
- [ ] Documentation updates

### Maintenance Scripts

Create `/opt/petclinic/scripts/maintenance.sh`:

```bash
#!/bin/bash

echo "Starting maintenance tasks..."

# Update system packages
sudo apt update && sudo apt upgrade -y

# Clean up old logs
find /opt/petclinic/logs -name "*.log.*" -mtime +30 -delete

# Optimize database
mysql -u petclinic_prod -p petclinic_prod -e "OPTIMIZE TABLE pets, visits, veterinarians, owners;"

# Check disk space
df -h

# Check service status
systemctl status petclinic-backend petclinic-frontend mysql nginx

echo "Maintenance tasks completed"
```

## Support and Documentation

For additional support and detailed documentation:

- **Application Logs**: `/opt/petclinic/logs/`
- **System Logs**: `/var/log/`
- **Configuration**: `/opt/petclinic/config/`
- **Health Checks**: `https://your-domain.com/health`
- **API Documentation**: `https://your-domain.com/api/swagger-ui.html`

---

**Document Version**: 2.0  
**Last Updated**: January 30, 2026  
**Maintained By**: Pet Clinic Development Team