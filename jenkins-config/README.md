# Jenkins Configuration for Pet Clinic CI/CD Pipeline

This directory contains Jenkins installation and configuration files for the Pet Clinic CI/CD Pipeline project.

## Files Overview

### 1. install-jenkins.sh
Complete Jenkins installation and configuration script for Amazon Linux 2.

**Features:**
- Installs Jenkins with Java 11, Git, Docker, and AWS CLI
- Configures essential plugins automatically
- Sets up basic security with admin user
- Configures global tools (Git, Maven, JDK)
- Sets up backup configuration
- Creates system service configuration

**Usage:**
```bash
sudo ./install-jenkins.sh
```

### 2. jenkins-casc.yaml
Jenkins Configuration as Code (JCasC) file for declarative configuration.

**Features:**
- Security realm and authorization strategy
- Global tool configurations
- Credential management
- Plugin configurations
- Job DSL for creating initial jobs
- Backup settings

**Usage:**
Place in `/var/lib/jenkins/jenkins-casc.yaml` and restart Jenkins.

### 3. plugins.txt
List of essential Jenkins plugins for the CI/CD pipeline.

**Categories:**
- Core Pipeline plugins
- Blue Ocean UI
- Source code management (Git, GitHub)
- Build tools (Maven, Gradle)
- Testing and quality (JUnit, JaCoCo, SonarQube)
- AWS integration
- Docker support
- Backup and maintenance
- Notifications (Email, Slack)
- Security plugins

**Usage:**
```bash
# Install plugins using Jenkins CLI
java -jar jenkins-cli.jar -s http://localhost:8080/ install-plugin < plugins.txt
```

### 4. jenkins.service
Systemd service configuration for Jenkins.

**Features:**
- Optimized JVM settings for performance
- Security hardening
- Resource limits
- Automatic restart configuration
- Proper logging setup

**Usage:**
```bash
sudo cp jenkins.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable jenkins
sudo systemctl start jenkins
```

### 5. jenkins-env.sh
Environment configuration script with all necessary variables.

**Features:**
- Java and Maven configuration
- AWS settings
- Build environment variables
- Security credentials
- Logging configuration
- Environment validation functions

**Usage:**
```bash
source jenkins-env.sh
validate_environment
show_environment
```

## Installation Process

### 1. Automated Installation
Run the complete installation script:
```bash
sudo ./install-jenkins.sh
```

### 2. Manual Installation Steps

#### Step 1: Prepare Environment
```bash
# Source environment variables
source jenkins-env.sh

# Validate environment
validate_environment
```

#### Step 2: Install Jenkins
```bash
# Update system
sudo yum update -y

# Install Java 11
sudo yum install -y java-11-amazon-corretto-headless

# Add Jenkins repository
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key

# Install Jenkins
sudo yum install -y jenkins
```

#### Step 3: Configure Service
```bash
# Copy service configuration
sudo cp jenkins.service /etc/systemd/system/

# Copy JCasC configuration
sudo cp jenkins-casc.yaml /var/lib/jenkins/
sudo chown jenkins:jenkins /var/lib/jenkins/jenkins-casc.yaml

# Reload systemd and start Jenkins
sudo systemctl daemon-reload
sudo systemctl enable jenkins
sudo systemctl start jenkins
```

#### Step 4: Install Plugins
```bash
# Wait for Jenkins to start
sleep 60

# Install plugins
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ install-plugin < plugins.txt

# Restart Jenkins
sudo systemctl restart jenkins
```

## Configuration Details

### Security Configuration
- **Authentication**: Local user database with role-based access
- **Authorization**: Role-based strategy with admin, developer, and viewer roles
- **CSRF Protection**: Enabled with crumb issuer
- **Content Security Policy**: Configured for security

### User Roles
- **Admin**: Full administrative access
- **Developer**: Build and job management permissions
- **Viewer**: Read-only access to jobs and builds

### Global Tools
- **Java**: Amazon Corretto 11 at `/usr/lib/jvm/java-11-amazon-corretto`
- **Maven**: Version 3.8.6 at `/opt/maven`
- **Git**: System Git at `/usr/bin/git`

### Backup Configuration
- **Location**: `/var/lib/jenkins/backups`
- **Schedule**: Daily at 2 AM
- **Retention**: 30 full backups
- **Content**: Configurations, job definitions, user content

### AWS Integration
- **Credentials**: Managed through AWS IAM roles
- **Services**: EC2, S3, CloudFormation integration
- **Deployment**: Automated deployment to EC2 instances

## Post-Installation Tasks

### 1. Access Jenkins
- URL: `http://your-server:8080`
- Username: `admin`
- Password: `admin123` (change in production)

### 2. Configure GitHub Integration
1. Go to "Manage Jenkins" → "Configure System"
2. Add GitHub server configuration
3. Set up webhook URL: `http://your-server:8080/github-webhook/`

### 3. Configure AWS Credentials
1. Go to "Manage Jenkins" → "Manage Credentials"
2. Add AWS credentials or configure IAM roles
3. Test connectivity to AWS services

### 4. Set up Notifications
1. Configure email settings in "Manage Jenkins" → "Configure System"
2. Set up Slack integration with webhook URL
3. Test notification delivery

### 5. Create Initial Jobs
Jobs are automatically created through Job DSL:
- `pet-clinic/pet-clinic-backend`
- `pet-clinic/pet-clinic-frontend`

## Monitoring and Maintenance

### Service Management
```bash
# Check status
sudo systemctl status jenkins

# View logs
sudo journalctl -u jenkins -f

# Restart service
sudo systemctl restart jenkins
```

### Backup Management
```bash
# Manual backup
curl -X POST http://localhost:8080/thinBackup/backupManual

# List backups
ls -la /var/lib/jenkins/backups/

# Restore from backup
# Use Jenkins UI: Manage Jenkins → ThinBackup → Restore
```

### Plugin Management
```bash
# List installed plugins
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ list-plugins

# Update plugins
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ install-plugin plugin-name
```

## Troubleshooting

### Common Issues

1. **Jenkins won't start**
   - Check Java installation: `java -version`
   - Check disk space: `df -h`
   - Check logs: `journalctl -u jenkins`

2. **Plugin installation fails**
   - Check internet connectivity
   - Verify Jenkins update center
   - Try manual plugin installation

3. **Build failures**
   - Check tool configurations
   - Verify credentials
   - Check workspace permissions

4. **Performance issues**
   - Monitor memory usage
   - Adjust JVM settings in service file
   - Check executor configuration

### Log Locations
- **Jenkins logs**: `/var/log/jenkins/jenkins.log`
- **System logs**: `journalctl -u jenkins`
- **Build logs**: Available in Jenkins UI

### Support Resources
- Jenkins Documentation: https://www.jenkins.io/doc/
- Plugin Documentation: https://plugins.jenkins.io/
- Configuration as Code: https://github.com/jenkinsci/configuration-as-code-plugin

## Security Best Practices

1. **Change default passwords** immediately after installation
2. **Use HTTPS** in production environments
3. **Configure firewall** to restrict access to port 8080
4. **Regular updates** of Jenkins and plugins
5. **Backup encryption** for sensitive data
6. **Audit logs** for security monitoring
7. **Principle of least privilege** for user permissions

## Production Considerations

1. **Load Balancer**: Configure ALB health checks for `/login`
2. **SSL/TLS**: Terminate SSL at load balancer or configure in Jenkins
3. **Scaling**: Use Jenkins agents for distributed builds
4. **Monitoring**: Integrate with CloudWatch for metrics
5. **Secrets Management**: Use AWS Secrets Manager for sensitive data
6. **High Availability**: Consider Jenkins clustering for critical environments

## ThinBackup Configuration

The ThinBackup plugin is configured for automated Jenkins backups with the following settings:

### Backup Schedule
- **Full Backups**: Daily at 2:00 AM
- **Differential Backups**: Every 6 hours
- **Backup Location**: `/var/lib/jenkins/backups`

### Retention Policy
- **Daily Backups**: Keep 30 most recent
- **Weekly Backups**: Keep 12 weekly backups (Sundays)
- **Monthly Backups**: Keep 12 monthly backups (first of month)
- **Differential Backups**: Clean up after 7 days

### Setup ThinBackup

1. **Configure ThinBackup plugin:**
   ```bash
   ./setup-thinbackup.sh
   ```

2. **Test backup configuration:**
   ```bash
   ./backup-test.sh
   ```

### Backup Management Scripts

The setup creates several management scripts:
- **`backup-retention.sh`** - Implements retention policy (30/12/12)
- **`backup-monitor.sh`** - Monitors backup health and disk space
- **`verify-backup.sh`** - Verifies backup integrity and completeness

### Backup Monitoring
- **Health Checks**: Every 4 hours via cron job
- **Retention Cleanup**: Daily at 3:00 AM
- **Email Alerts**: On backup failures
- **Disk Space Monitoring**: Alerts when >90% full

### What Gets Backed Up
- ✅ Jenkins configuration files
- ✅ Job configurations and build history
- ✅ User content and credentials
- ✅ Plugin configurations
- ✅ Build results and artifacts (configurable)
- ❌ Workspaces (excluded for space efficiency)
- ❌ Log files and temporary files

### Backup Recovery

To restore Jenkins from backup:

1. **Stop Jenkins service**
   ```bash
   sudo systemctl stop jenkins
   ```

2. **Restore from backup**
   ```bash
   cd /var/lib/jenkins
   sudo -u jenkins tar -xzf backups/FULL-2024-01-15_02-00.tar.gz
   ```

3. **Start Jenkins service**
   ```bash
   sudo systemctl start jenkins
   ```

### Backup Troubleshooting

1. **Check backup directory permissions**
   ```bash
   sudo chown -R jenkins:jenkins /var/lib/jenkins/backups
   sudo chmod 755 /var/lib/jenkins/backups
   ```

2. **Verify ThinBackup plugin configuration**
   ```bash
   cat /var/lib/jenkins/org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl.xml
   ```

3. **Check backup monitoring logs**
   ```bash
   tail -f /var/log/jenkins/backup-monitor.log
   ```

4. **Verify cron jobs**
   ```bash
   sudo crontab -l -u jenkins
   systemctl status jenkins-backup-monitor.timer
   ```