#!/bin/bash
# Jenkins Installation and Configuration Script for Pet Clinic CI/CD Pipeline
# This script installs Jenkins with required plugins and basic security configuration

set -e

# Configuration
JENKINS_HOME="/var/lib/jenkins"
JENKINS_USER="jenkins"
JENKINS_PORT="8080"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin123"  # In production, use AWS Secrets Manager

# Log function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting Jenkins installation and configuration..."

# Update system
log "Updating system packages..."
yum update -y

# Install Java 11 (required for Jenkins)
log "Installing Java 11..."
yum install -y java-11-amazon-corretto-headless

# Add Jenkins repository
log "Adding Jenkins repository..."
wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key

# Install Jenkins
log "Installing Jenkins..."
yum install -y jenkins

# Install Git (required for source code management)
log "Installing Git..."
yum install -y git

# Install Docker (for containerized builds)
log "Installing Docker..."
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -a -G docker jenkins

# Install AWS CLI v2
log "Installing AWS CLI v2..."
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install
rm -rf aws awscliv2.zip

# Configure Jenkins
log "Configuring Jenkins..."

# Start Jenkins service
systemctl start jenkins
systemctl enable jenkins

# Wait for Jenkins to start
log "Waiting for Jenkins to start..."
sleep 30

# Get initial admin password
INITIAL_PASSWORD=$(cat /var/lib/jenkins/secrets/initialAdminPassword)
log "Initial admin password retrieved"

# Install Jenkins CLI
log "Setting up Jenkins CLI..."
wget http://localhost:8080/jnlpJars/jenkins-cli.jar -O /opt/jenkins-cli.jar

# Function to run Jenkins CLI commands
jenkins_cli() {
    java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ -auth admin:${INITIAL_PASSWORD} "$@"
}

# Wait for Jenkins to be fully ready
log "Waiting for Jenkins to be fully ready..."
for i in {1..30}; do
    if curl -s http://localhost:8080/login > /dev/null; then
        log "Jenkins is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        log "ERROR: Jenkins failed to start properly"
        exit 1
    fi
    sleep 10
done

# Skip initial setup wizard
log "Configuring Jenkins to skip setup wizard..."
echo 'jenkins.install.runSetupWizard=false' >> /var/lib/jenkins/jenkins.install.UpgradeWizard.state
echo 'jenkins.install.runSetupWizard=false' >> /var/lib/jenkins/jenkins.install.InstallUtil.lastExecVersion

# Install essential plugins
log "Installing essential Jenkins plugins..."
PLUGINS=(
    "git"
    "github"
    "pipeline-stage-view"
    "workflow-aggregator"
    "blueocean"
    "aws-credentials"
    "ec2"
    "s3"
    "thinBackup"
    "build-timeout"
    "timestamper"
    "ws-cleanup"
    "ant"
    "gradle"
    "maven-plugin"
    "junit"
    "jacoco"
    "sonar"
    "email-ext"
    "slack"
)

# Create plugins installation script
cat > /tmp/install-plugins.groovy << 'EOF'
import jenkins.model.*
import hudson.model.*
import hudson.security.*
import hudson.util.*
import jenkins.security.s2m.*

def instance = Jenkins.getInstance()

// Install plugins
def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()

// Update plugin data
uc.updateAllSites()

// Install plugins
def plugins = [
    "git",
    "github", 
    "pipeline-stage-view",
    "workflow-aggregator",
    "blueocean",
    "aws-credentials",
    "ec2",
    "s3",
    "thinBackup",
    "build-timeout",
    "timestamper",
    "ws-cleanup",
    "ant",
    "gradle",
    "maven-plugin",
    "junit",
    "jacoco",
    "sonar",
    "email-ext",
    "slack"
]

plugins.each { pluginName ->
    if (!pm.getPlugin(pluginName)) {
        def plugin = uc.getPlugin(pluginName)
        if (plugin) {
            plugin.deploy(true)
            println "Installing plugin: ${pluginName}"
        }
    }
}

// Save configuration
instance.save()
EOF

# Execute plugin installation
log "Executing plugin installation..."
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ -auth admin:${INITIAL_PASSWORD} groovy = < /tmp/install-plugins.groovy

# Restart Jenkins to load plugins
log "Restarting Jenkins to load plugins..."
systemctl restart jenkins
sleep 60

# Configure security
log "Configuring Jenkins security..."
cat > /tmp/configure-security.groovy << EOF
import jenkins.model.*
import hudson.security.*
import hudson.security.csrf.DefaultCrumbIssuer
import jenkins.security.s2m.AdminWhitelistRule

def instance = Jenkins.getInstance()

// Enable CSRF protection
instance.setCrumbIssuer(new DefaultCrumbIssuer(true))

// Configure security realm (local user database)
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("${ADMIN_USER}", "${ADMIN_PASSWORD}")
instance.setSecurityRealm(hudsonRealm)

// Configure authorization strategy (logged-in users can do anything)
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

// Disable agent-to-master security for simplicity (not recommended for production)
instance.getInjector().getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false)

// Save configuration
instance.save()

println "Security configuration completed"
EOF

# Execute security configuration
log "Executing security configuration..."
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ -auth admin:${INITIAL_PASSWORD} groovy = < /tmp/configure-security.groovy

# Configure global tools
log "Configuring global tools..."
cat > /tmp/configure-tools.groovy << 'EOF'
import jenkins.model.*
import hudson.model.*
import hudson.tools.*
import hudson.plugins.git.*
import hudson.tasks.Maven.*

def instance = Jenkins.getInstance()

// Configure Git
def gitInstallation = new GitTool("Default", "/usr/bin/git", [])
def gitDescriptor = instance.getDescriptor(GitTool.class)
gitDescriptor.setInstallations(gitInstallation)
gitDescriptor.save()

// Configure Maven
def mavenInstallation = new MavenInstallation("Maven-3.8.6", "/opt/maven", [])
def mavenDescriptor = instance.getDescriptor(MavenInstallation.class)
mavenDescriptor.setInstallations(mavenInstallation)
mavenDescriptor.save()

// Configure JDK
def jdkInstallation = new JDK("Java-11", "/usr/lib/jvm/java-11-amazon-corretto")
def jdkDescriptor = instance.getDescriptor(JDK.class)
jdkDescriptor.setInstallations(jdkInstallation)
jdkDescriptor.save()

instance.save()
println "Global tools configuration completed"
EOF

# Execute tools configuration
log "Executing tools configuration..."
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ -auth admin:${ADMIN_USER}:${ADMIN_PASSWORD} groovy = < /tmp/configure-tools.groovy

# Configure system settings
log "Configuring system settings..."
cat > /tmp/configure-system.groovy << 'EOF'
import jenkins.model.*
import hudson.model.*

def instance = Jenkins.getInstance()

// Set Jenkins URL (will be updated with actual ALB DNS)
instance.setRootUrl("http://localhost:8080/")

// Set system admin email
def locationConfig = JenkinsLocationConfiguration.get()
locationConfig.setAdminAddress("admin@petclinic.local")
locationConfig.save()

// Configure number of executors
instance.setNumExecutors(2)

// Set quiet period
instance.setQuietPeriod(5)

// Set SCM checkout retry count
instance.setScmCheckoutRetryCount(3)

instance.save()
println "System configuration completed"
EOF

# Execute system configuration
log "Executing system configuration..."
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ -auth admin:${ADMIN_USER}:${ADMIN_PASSWORD} groovy = < /tmp/configure-system.groovy

# Configure backup
log "Configuring backup settings..."
cat > /tmp/configure-backup.groovy << 'EOF'
import jenkins.model.*
import org.jvnet.hudson.plugins.thinbackup.*

def instance = Jenkins.getInstance()

// Configure ThinBackup plugin
def thinBackupDescriptor = instance.getDescriptor(ThinBackupPeriodicWork.class)
if (thinBackupDescriptor) {
    def config = new ThinBackupPluginImpl()
    config.setBackupPath("/var/lib/jenkins/backups")
    config.setNrMaxStoredFull(30)  // Keep 30 full backups
    config.setSchedule("H 2 * * *")  // Daily at 2 AM
    config.setBackupBuildResults(true)
    config.setBackupBuildArchive(false)
    config.setBackupUserContents(true)
    config.setBackupPluginArchives(true)
    config.setBackupConfigHistory(true)
    
    thinBackupDescriptor.configure(null, config)
    thinBackupDescriptor.save()
    
    println "Backup configuration completed"
}
EOF

# Execute backup configuration
log "Executing backup configuration..."
java -jar /opt/jenkins-cli.jar -s http://localhost:8080/ -auth admin:${ADMIN_USER}:${ADMIN_PASSWORD} groovy = < /tmp/configure-backup.groovy

# Create backup directory
log "Creating backup directory..."
mkdir -p /var/lib/jenkins/backups
chown jenkins:jenkins /var/lib/jenkins/backups

# Configure firewall (if firewalld is running)
if systemctl is-active --quiet firewalld; then
    log "Configuring firewall..."
    firewall-cmd --permanent --add-port=8080/tcp
    firewall-cmd --reload
fi

# Clean up temporary files
log "Cleaning up temporary files..."
rm -f /tmp/install-plugins.groovy
rm -f /tmp/configure-security.groovy
rm -f /tmp/configure-tools.groovy
rm -f /tmp/configure-system.groovy
rm -f /tmp/configure-backup.groovy

# Final restart to ensure all configurations are loaded
log "Final Jenkins restart..."
systemctl restart jenkins

# Wait for Jenkins to be ready
log "Waiting for Jenkins to be ready..."
sleep 60

# Verify installation
log "Verifying Jenkins installation..."
if curl -s http://localhost:8080/login > /dev/null; then
    log "✓ Jenkins is accessible"
else
    log "✗ Jenkins is not accessible"
    exit 1
fi

# Display installation summary
log "Jenkins installation completed successfully!"
log "Jenkins URL: http://localhost:8080"
log "Admin username: ${ADMIN_USER}"
log "Admin password: ${ADMIN_PASSWORD}"
log "Jenkins home: ${JENKINS_HOME}"
log "Backup directory: ${JENKINS_HOME}/backups"

# Create Jenkins info file
cat > /opt/jenkins-info.txt << EOF
Jenkins Installation Information
================================
Installation Date: $(date)
Jenkins URL: http://localhost:8080
Admin Username: ${ADMIN_USER}
Admin Password: ${ADMIN_PASSWORD}
Jenkins Home: ${JENKINS_HOME}
Backup Directory: ${JENKINS_HOME}/backups

Installed Plugins:
- Git
- GitHub
- Pipeline
- Blue Ocean
- AWS Credentials
- EC2
- S3
- ThinBackup
- Build Timeout
- Timestamper
- Workspace Cleanup
- Maven
- JUnit
- JaCoCo
- SonarQube
- Email Extension
- Slack

Service Management:
- Start: systemctl start jenkins
- Stop: systemctl stop jenkins
- Restart: systemctl restart jenkins
- Status: systemctl status jenkins
- Logs: journalctl -u jenkins -f

Configuration Files:
- Main config: ${JENKINS_HOME}/config.xml
- Plugin configs: ${JENKINS_HOME}/
- CLI tool: /opt/jenkins-cli.jar
EOF

log "Installation information saved to /opt/jenkins-info.txt"
log "Jenkins installation and configuration completed successfully!"