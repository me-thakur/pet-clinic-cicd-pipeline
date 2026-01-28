# Pet Clinic Deployment Scripts

This directory contains automation scripts for deploying and managing the Pet Clinic application on AWS EC2 instances.

## Scripts Overview

### Core Deployment Scripts

- **`setup-database.sh`** - Sets up MySQL database schema and initial data
- **`deploy-backend.sh`** - Deploys the backend Spring Boot application
- **`deploy-frontend.sh`** - Deploys the frontend web application
- **`health-check.sh`** - Comprehensive health checks for all components
- **`rollback.sh`** - Rolls back to previous application version

## Prerequisites

1. **System Requirements:**
   - Linux-based EC2 instance (Amazon Linux 2 or Ubuntu)
   - Java 11 installed
   - MySQL client installed
   - AWS CLI configured
   - curl and jq utilities

2. **Environment Variables:**
   ```bash
   # Database configuration
   export DB_HOST="your-rds-endpoint"
   export DB_NAME="petclinic"
   export DB_USER="petclinic"
   export DB_PASSWORD="your-db-password"
   export DB_ROOT_PASSWORD="your-root-password"
   
   # Application configuration
   export BACKEND_URL="http://internal-alb-endpoint/api"
   export AWS_REGION="us-east-1"
   export BUILD_NUMBER="1.0.0"
   
   # GitHub OAuth (optional)
   export GITHUB_CLIENT_ID="your-client-id"
   export GITHUB_CLIENT_SECRET="your-client-secret"
   ```

3. **Service User:**
   ```bash
   sudo useradd -r -s /bin/false petclinic
   ```

4. **Systemd Service Files:**
   Create service files for both applications (see examples below)

## Usage

### Initial Setup

1. **Make scripts executable:**
   ```bash
   chmod +x *.sh
   ```

2. **Set up database:**
   ```bash
   ./setup-database.sh
   ```

3. **Deploy applications:**
   ```bash
   ./deploy-backend.sh
   ./deploy-frontend.sh
   ```

4. **Verify deployment:**
   ```bash
   ./health-check.sh
   ```

### Ongoing Operations

- **Health Check:** `./health-check.sh`
- **Rollback Backend:** `./rollback.sh backend`
- **Rollback Frontend:** `./rollback.sh frontend`
- **Rollback Both:** `./rollback.sh both`

## Systemd Service Files

### Backend Service (`/etc/systemd/system/pet-clinic-backend.service`)

```ini
[Unit]
Description=Pet Clinic Backend Application
After=network.target

[Service]
Type=simple
User=petclinic
Group=petclinic
WorkingDirectory=/opt/pet-clinic
ExecStart=/usr/bin/java -jar -Xmx512m -Xms256m current.jar --spring.config.location=file:./application.yml
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=pet-clinic-backend

[Install]
WantedBy=multi-user.target
```

### Frontend Service (`/etc/systemd/system/pet-clinic-frontend.service`)

```ini
[Unit]
Description=Pet Clinic Frontend Application
After=network.target pet-clinic-backend.service

[Service]
Type=simple
User=petclinic
Group=petclinic
WorkingDirectory=/opt/pet-clinic
ExecStart=/usr/bin/java -jar -Xmx256m -Xms128m frontend-current.jar --spring.config.location=file:./frontend-application.yml
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=pet-clinic-frontend

[Install]
WantedBy=multi-user.target
```

## Directory Structure

```
/opt/pet-clinic/
├── current.jar                    # Backend application symlink
├── frontend-current.jar           # Frontend application symlink
├── application.yml                # Backend configuration
├── frontend-application.yml       # Frontend configuration
├── pet-clinic-backend-1.0.0.jar  # Backend version files
├── pet-clinic-frontend-1.0.0.jar # Frontend version files
└── backups/                       # Application backups
    ├── pet-clinic-backend-backup-*.jar
    └── pet-clinic-frontend-backup-*.jar

/var/log/pet-clinic/
├── backend.log                    # Backend application logs
└── frontend.log                   # Frontend application logs
```

## Health Check Details

The health check script validates:

- **Service Status:** Backend and frontend systemd services
- **HTTP Endpoints:** Application health endpoints
- **JSON APIs:** Structured health responses
- **Database:** Connectivity and data integrity
- **Logs:** Recent error analysis
- **System Resources:** Disk space and memory usage

### Health Check Exit Codes

- `0` - All checks passed
- `1` - One or more checks failed

## Rollback Process

The rollback script:

1. Stops the current application
2. Backs up the current version
3. Restores from the latest backup
4. Starts the application
5. Verifies the rollback was successful

## Troubleshooting

### Common Issues

1. **Permission Errors:**
   ```bash
   sudo chown -R petclinic:petclinic /opt/pet-clinic
   sudo chmod +x /opt/pet-clinic/*.sh
   ```

2. **Database Connection:**
   ```bash
   mysql -h $DB_HOST -u $DB_USER -p$DB_PASSWORD -e "SELECT 1;"
   ```

3. **Service Logs:**
   ```bash
   journalctl -u pet-clinic-backend -f
   journalctl -u pet-clinic-frontend -f
   ```

4. **Application Logs:**
   ```bash
   tail -f /var/log/pet-clinic/backend.log
   tail -f /var/log/pet-clinic/frontend.log
   ```

### Health Check Failures

- Check service status: `systemctl status pet-clinic-backend`
- Verify configuration files exist and are readable
- Ensure database is accessible
- Check disk space and memory usage
- Review application logs for errors

## Security Considerations

- Scripts should not be run as root
- Database passwords should be stored securely
- Application runs as dedicated service user
- Configuration files have restricted permissions (640)
- Backups are retained with proper cleanup

## Integration with Jenkins

These scripts are designed to be called from Jenkins pipelines:

```groovy
stage('Deploy Backend') {
    steps {
        sh './deployment-scripts/deploy-backend.sh'
    }
}

stage('Health Check') {
    steps {
        sh './deployment-scripts/health-check.sh'
    }
}
```