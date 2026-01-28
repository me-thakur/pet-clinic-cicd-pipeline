# EFS Installation Scripts

This directory contains installation and deployment scripts for the Pet Clinic CI/CD Pipeline. These scripts are designed to be stored on Amazon EFS and accessed by EC2 instances during bootstrap.

## Scripts Overview

### 1. install-java.sh
Installs Amazon Corretto 11 JDK on Amazon Linux 2 instances.

**Features:**
- Installs both headless and development versions of Java 11
- Sets up JAVA_HOME environment variable
- Creates system-wide Java alternatives
- Configures JVM options for Pet Clinic applications

**Usage:**
```bash
sudo ./install-java.sh
```

### 2. install-maven.sh
Installs Apache Maven 3.8.6 for building Java applications.

**Features:**
- Downloads and installs Maven 3.8.6
- Sets up MAVEN_HOME environment variable
- Creates custom Maven settings with profiles
- Configures local repository location

**Usage:**
```bash
sudo ./install-maven.sh
```

### 3. deploy-app.sh
Deploys Pet Clinic applications (backend or frontend) as systemd services.

**Features:**
- Creates application user and directories
- Configures Spring Boot applications
- Sets up systemd services with auto-restart
- Performs health checks after deployment
- Supports multiple environments (dev, staging, prod)

**Usage:**
```bash
sudo ./deploy-app.sh [backend|frontend] <jar-file> [environment]
```

**Examples:**
```bash
# Deploy backend application in dev environment
sudo ./deploy-app.sh backend /tmp/pet-clinic-backend-1.0.0.jar dev

# Deploy frontend application in production
sudo ./deploy-app.sh frontend /tmp/pet-clinic-frontend-1.0.0.jar prod
```

### 4. health-check.sh
Comprehensive health checking for deployed applications.

**Features:**
- Checks service status and port availability
- Performs HTTP health checks with retries
- Validates application metrics endpoints
- Checks database connectivity (backend only)
- Monitors system resources (memory, disk, CPU)

**Usage:**
```bash
./health-check.sh [backend|frontend|all] [--timeout=seconds] [--retries=count]
```

**Examples:**
```bash
# Check all applications
./health-check.sh all

# Check only backend with custom timeout
./health-check.sh backend --timeout=60 --retries=3
```

## Deployment to EFS

To deploy these scripts to EFS:

1. Mount the EFS file system on an EC2 instance:
```bash
sudo mkdir -p /mnt/efs
sudo mount -t efs fs-xxxxxx:/ /mnt/efs
```

2. Copy scripts to EFS:
```bash
sudo cp -r efs-scripts/* /mnt/efs/
sudo chmod +x /mnt/efs/*.sh
```

3. Verify scripts are accessible:
```bash
ls -la /mnt/efs/
```

## Environment Variables

The scripts use the following environment variables:

- `ENVIRONMENT`: Application environment (dev, staging, prod)
- `DB_HOST`: Database host (for application configuration)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JAVA_HOME`: Java installation directory
- `MAVEN_HOME`: Maven installation directory

## Application Ports

- **Backend**: Port 8081
- **Frontend**: Port 8080

## Log Locations

- Application logs: `/var/log/petclinic/`
- System logs: `journalctl -u petclinic-[backend|frontend]`

## Service Management

After deployment, applications can be managed using systemctl:

```bash
# Start/stop services
sudo systemctl start petclinic-backend
sudo systemctl stop petclinic-frontend

# Check service status
sudo systemctl status petclinic-backend

# View logs
sudo journalctl -u petclinic-backend -f
```

## Troubleshooting

1. **Java not found**: Ensure install-java.sh completed successfully
2. **Maven not found**: Ensure install-maven.sh completed successfully and source /etc/profile
3. **Application won't start**: Check logs with `journalctl -u petclinic-[app]`
4. **Health check fails**: Verify database connectivity and application configuration
5. **Port conflicts**: Ensure no other services are using ports 8080/8081

## Security Considerations

- Scripts should be run with appropriate privileges (sudo for installation)
- Database credentials should be managed securely (AWS Secrets Manager recommended)
- EFS should be configured with appropriate access controls
- Applications run as non-root user (petclinic)

## Integration with CloudFormation

These scripts are referenced in the compute.yaml CloudFormation template:

```yaml
UserData:
  Fn::Base64: !Sub |
    #!/bin/bash
    # Mount EFS
    mkdir -p /mnt/efs
    mount -t efs ${EfsFileSystemId}:/ /mnt/efs
    
    # Run installation scripts
    if [ -f /mnt/efs/install-java.sh ]; then
      chmod +x /mnt/efs/install-java.sh
      /mnt/efs/install-java.sh
    fi
    
    if [ -f /mnt/efs/install-maven.sh ]; then
      chmod +x /mnt/efs/install-maven.sh
      /mnt/efs/install-maven.sh
    fi
```