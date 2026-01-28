#!/bin/bash
# Jenkins Environment Configuration for Pet Clinic CI/CD Pipeline
# This script sets up environment variables for Jenkins

# Jenkins Configuration
export JENKINS_HOME="/var/lib/jenkins"
export JENKINS_USER="jenkins"
export JENKINS_GROUP="jenkins"
export JENKINS_PORT="8080"
export JENKINS_PREFIX="/jenkins"
export JENKINS_LOG="/var/log/jenkins/jenkins.log"

# Java Configuration
export JAVA_HOME="/usr/lib/jvm/java-11-amazon-corretto"
export JAVA_OPTS="-Djava.awt.headless=true \
    -Djenkins.install.runSetupWizard=false \
    -Dcasc.jenkins.config=/var/lib/jenkins/jenkins-casc.yaml \
    -Dhudson.security.csrf.DefaultCrumbIssuer.EXCLUDE_SESSION_ID=true \
    -Dhudson.model.DirectoryBrowserSupport.CSP=\"default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';\" \
    -Xms512m \
    -Xmx2048m \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+DisableExplicitGC \
    -XX:+UnlockExperimentalVMOptions \
    -XX:G1NewSizePercent=20 \
    -XX:+UnlockDiagnosticVMOptions \
    -XX:G1SummarizeRSetStatsPeriod=1"

# Jenkins CLI Configuration
export JENKINS_CLI_JAR="/opt/jenkins-cli.jar"
export JENKINS_URL="http://localhost:8080"

# Tool Paths
export MAVEN_HOME="/opt/maven"
export MAVEN_OPTS="-Xms256m -Xmx512m"
export GIT_HOME="/usr/bin/git"

# AWS Configuration (will be set by IAM roles in production)
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
export AWS_REGION="${AWS_REGION:-us-east-1}"

# Docker Configuration
export DOCKER_HOST="unix:///var/run/docker.sock"

# Build Configuration
export BUILD_NUMBER="${BUILD_NUMBER:-1}"
export BUILD_ID="${BUILD_ID:-unknown}"
export JOB_NAME="${JOB_NAME:-manual}"
export WORKSPACE="${WORKSPACE:-/tmp}"

# Application Configuration
export ENVIRONMENT="${ENVIRONMENT:-dev}"
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-petclinic}"

# Notification Configuration
export SLACK_CHANNEL="${SLACK_CHANNEL:-#ci-cd}"
export EMAIL_RECIPIENTS="${EMAIL_RECIPIENTS:-admin@petclinic.local}"

# Security Configuration
export JENKINS_ADMIN_PASSWORD="${JENKINS_ADMIN_PASSWORD:-admin123}"
export JENKINS_DEV_PASSWORD="${JENKINS_DEV_PASSWORD:-dev123}"
export JENKINS_VIEWER_PASSWORD="${JENKINS_VIEWER_PASSWORD:-view123}"

# Backup Configuration
export BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
export BACKUP_S3_BUCKET="${BACKUP_S3_BUCKET:-petclinic-jenkins-backups}"

# Logging Configuration
export LOG_LEVEL="${LOG_LEVEL:-INFO}"
export LOG_MAX_SIZE="${LOG_MAX_SIZE:-100MB}"
export LOG_MAX_FILES="${LOG_MAX_FILES:-10}"

# Performance Configuration
export JENKINS_EXECUTORS="${JENKINS_EXECUTORS:-2}"
export JENKINS_QUIET_PERIOD="${JENKINS_QUIET_PERIOD:-5}"
export JENKINS_SCM_RETRY_COUNT="${JENKINS_SCM_RETRY_COUNT:-3}"

# Update PATH
export PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"

# Function to validate environment
validate_environment() {
    echo "Validating Jenkins environment..."
    
    # Check Java
    if [ ! -f "${JAVA_HOME}/bin/java" ]; then
        echo "ERROR: Java not found at ${JAVA_HOME}/bin/java"
        return 1
    fi
    
    # Check Maven
    if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
        echo "ERROR: Maven not found at ${MAVEN_HOME}/bin/mvn"
        return 1
    fi
    
    # Check Git
    if [ ! -f "${GIT_HOME}" ]; then
        echo "ERROR: Git not found at ${GIT_HOME}"
        return 1
    fi
    
    # Check Jenkins home directory
    if [ ! -d "${JENKINS_HOME}" ]; then
        echo "ERROR: Jenkins home directory not found at ${JENKINS_HOME}"
        return 1
    fi
    
    # Check Docker socket
    if [ ! -S "/var/run/docker.sock" ]; then
        echo "WARNING: Docker socket not found at /var/run/docker.sock"
    fi
    
    echo "Environment validation completed successfully"
    return 0
}

# Function to display environment info
show_environment() {
    echo "Jenkins Environment Configuration"
    echo "================================"
    echo "JENKINS_HOME: ${JENKINS_HOME}"
    echo "JENKINS_URL: ${JENKINS_URL}"
    echo "JAVA_HOME: ${JAVA_HOME}"
    echo "MAVEN_HOME: ${MAVEN_HOME}"
    echo "GIT_HOME: ${GIT_HOME}"
    echo "AWS_REGION: ${AWS_REGION}"
    echo "ENVIRONMENT: ${ENVIRONMENT}"
    echo "LOG_LEVEL: ${LOG_LEVEL}"
    echo "================================"
}

# Export functions
export -f validate_environment
export -f show_environment

# Auto-validate if script is sourced
if [ "${BASH_SOURCE[0]}" != "${0}" ]; then
    validate_environment
fi