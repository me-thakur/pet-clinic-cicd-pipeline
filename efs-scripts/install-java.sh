#!/bin/bash
# Java 11 Installation Script for Pet Clinic CI/CD Pipeline
# This script installs Amazon Corretto 11 JDK on Amazon Linux 2

set -e

# Log function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting Java 11 installation..."

# Update system packages
log "Updating system packages..."
yum update -y

# Install Amazon Corretto 11
log "Installing Amazon Corretto 11..."
yum install -y java-11-amazon-corretto-headless java-11-amazon-corretto-devel

# Set JAVA_HOME environment variable
log "Setting JAVA_HOME environment variable..."
echo 'export JAVA_HOME=/usr/lib/jvm/java-11-amazon-corretto' >> /etc/profile
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile

# Create symlink for system-wide access
log "Creating system-wide Java symlinks..."
alternatives --install /usr/bin/java java /usr/lib/jvm/java-11-amazon-corretto/bin/java 1
alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-11-amazon-corretto/bin/javac 1

# Verify installation
log "Verifying Java installation..."
source /etc/profile
java -version
javac -version

# Create Java configuration for applications
log "Creating Java configuration..."
mkdir -p /opt/petclinic/config
cat > /opt/petclinic/config/java.conf << EOF
# Java Configuration for Pet Clinic Applications
JAVA_HOME=/usr/lib/jvm/java-11-amazon-corretto
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"
JVM_ARGS="-Dspring.profiles.active=\${ENVIRONMENT:-dev}"
EOF

log "Java 11 installation completed successfully!"
log "JAVA_HOME: $JAVA_HOME"
log "Java version: $(java -version 2>&1 | head -n 1)"