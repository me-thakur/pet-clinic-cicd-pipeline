#!/bin/bash
# Maven Installation Script for Pet Clinic CI/CD Pipeline
# This script installs Apache Maven 3.8.6 on Amazon Linux 2

set -e

# Configuration
MAVEN_VERSION="3.8.6"
MAVEN_HOME="/opt/maven"
MAVEN_ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}"

# Log function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting Maven ${MAVEN_VERSION} installation..."

# Ensure Java is installed
if ! command -v java &> /dev/null; then
    log "ERROR: Java is not installed. Please install Java first."
    exit 1
fi

# Create Maven directory
log "Creating Maven installation directory..."
mkdir -p /opt

# Download Maven
log "Downloading Maven ${MAVEN_VERSION}..."
cd /opt
if [ ! -f "${MAVEN_ARCHIVE}" ]; then
    wget "${MAVEN_URL}"
    if [ $? -ne 0 ]; then
        log "ERROR: Failed to download Maven"
        exit 1
    fi
else
    log "Maven archive already exists, skipping download"
fi

# Extract Maven
log "Extracting Maven..."
tar xzf "${MAVEN_ARCHIVE}"
if [ $? -ne 0 ]; then
    log "ERROR: Failed to extract Maven"
    exit 1
fi

# Create symlink
log "Creating Maven symlink..."
if [ -L "${MAVEN_HOME}" ]; then
    rm "${MAVEN_HOME}"
fi
ln -s "apache-maven-${MAVEN_VERSION}" maven

# Set environment variables
log "Setting Maven environment variables..."
cat > /etc/profile.d/maven.sh << EOF
# Maven Environment Variables
export MAVEN_HOME=${MAVEN_HOME}
export PATH=\${MAVEN_HOME}/bin:\${PATH}
EOF

# Make the script executable
chmod +x /etc/profile.d/maven.sh

# Source the environment
source /etc/profile.d/maven.sh

# Create Maven configuration directory
log "Creating Maven configuration..."
mkdir -p /opt/petclinic/config
cat > /opt/petclinic/config/maven.conf << EOF
# Maven Configuration for Pet Clinic Applications
MAVEN_HOME=${MAVEN_HOME}
MAVEN_OPTS="-Xms256m -Xmx512m"
MAVEN_PROFILES="\${ENVIRONMENT:-dev}"
EOF

# Create custom Maven settings
log "Creating Maven settings..."
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  
  <localRepository>/opt/maven-repo</localRepository>
  
  <profiles>
    <profile>
      <id>dev</id>
      <properties>
        <spring.profiles.active>dev</spring.profiles.active>
      </properties>
    </profile>
    <profile>
      <id>staging</id>
      <properties>
        <spring.profiles.active>staging</spring.profiles.active>
      </properties>
    </profile>
    <profile>
      <id>prod</id>
      <properties>
        <spring.profiles.active>prod</spring.profiles.active>
      </properties>
    </profile>
  </profiles>
  
  <activeProfiles>
    <activeProfile>dev</activeProfile>
  </activeProfiles>
</settings>
EOF

# Create Maven repository directory
mkdir -p /opt/maven-repo
chmod 755 /opt/maven-repo

# Clean up
log "Cleaning up installation files..."
rm -f "${MAVEN_ARCHIVE}"

# Verify installation
log "Verifying Maven installation..."
mvn -version

log "Maven ${MAVEN_VERSION} installation completed successfully!"
log "MAVEN_HOME: ${MAVEN_HOME}"
log "Maven version: $(mvn -version | head -n 1)"