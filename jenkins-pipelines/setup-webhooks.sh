#!/bin/bash
# GitHub Webhook Setup Script for Pet Clinic CI/CD Pipeline
# This script configures GitHub webhooks for automatic build triggers

set -e

# Configuration
JENKINS_URL="${JENKINS_URL:-http://localhost:8080}"
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_TOKEN="${JENKINS_TOKEN:-admin123}"
GITHUB_TOKEN="${GITHUB_TOKEN}"
GITHUB_ORG="${GITHUB_ORG:-your-org}"

# Repositories to configure
REPOSITORIES=(
    "pet-clinic-backend"
    "pet-clinic-frontend"
)

# Log function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    if [ -z "$GITHUB_TOKEN" ]; then
        log "ERROR: GITHUB_TOKEN environment variable is required"
        exit 1
    fi
    
    if ! command -v curl &> /dev/null; then
        log "ERROR: curl is required but not installed"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        log "ERROR: jq is required but not installed"
        exit 1
    fi
    
    log "Prerequisites check passed"
}

# Test Jenkins connectivity
test_jenkins_connectivity() {
    log "Testing Jenkins connectivity..."
    
    local response=$(curl -s -w "%{http_code}" -o /dev/null \
        -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
        "${JENKINS_URL}/api/json")
    
    if [ "$response" != "200" ]; then
        log "ERROR: Cannot connect to Jenkins at ${JENKINS_URL}"
        log "HTTP response code: $response"
        exit 1
    fi
    
    log "Jenkins connectivity test passed"
}

# Test GitHub connectivity
test_github_connectivity() {
    log "Testing GitHub connectivity..."
    
    local response=$(curl -s -w "%{http_code}" -o /dev/null \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        "https://api.github.com/user")
    
    if [ "$response" != "200" ]; then
        log "ERROR: Cannot connect to GitHub API"
        log "HTTP response code: $response"
        log "Please check your GITHUB_TOKEN"
        exit 1
    fi
    
    log "GitHub connectivity test passed"
}

# Create webhook for repository
create_webhook() {
    local repo="$1"
    local webhook_url="${JENKINS_URL}/github-webhook/"
    
    log "Creating webhook for repository: ${GITHUB_ORG}/${repo}"
    
    # Check if webhook already exists
    local existing_webhooks=$(curl -s \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        "https://api.github.com/repos/${GITHUB_ORG}/${repo}/hooks")
    
    local webhook_exists=$(echo "$existing_webhooks" | jq -r --arg url "$webhook_url" '.[] | select(.config.url == $url) | .id')
    
    if [ -n "$webhook_exists" ]; then
        log "Webhook already exists for ${repo} (ID: $webhook_exists)"
        return 0
    fi
    
    # Create webhook payload
    local webhook_payload=$(cat << EOF
{
  "name": "web",
  "active": true,
  "events": [
    "push",
    "pull_request",
    "create",
    "delete"
  ],
  "config": {
    "url": "${webhook_url}",
    "content_type": "json",
    "insecure_ssl": "0"
  }
}
EOF
)
    
    # Create webhook
    local response=$(curl -s -w "%{http_code}" \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$webhook_payload" \
        "https://api.github.com/repos/${GITHUB_ORG}/${repo}/hooks")
    
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "201" ]; then
        local webhook_id=$(echo "$response_body" | jq -r '.id')
        log "✓ Webhook created successfully for ${repo} (ID: $webhook_id)"
    else
        log "✗ Failed to create webhook for ${repo}"
        log "HTTP response code: $http_code"
        log "Response: $response_body"
        return 1
    fi
}

# Test webhook
test_webhook() {
    local repo="$1"
    
    log "Testing webhook for repository: ${GITHUB_ORG}/${repo}"
    
    # Get webhook ID
    local webhooks=$(curl -s \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        "https://api.github.com/repos/${GITHUB_ORG}/${repo}/hooks")
    
    local webhook_url="${JENKINS_URL}/github-webhook/"
    local webhook_id=$(echo "$webhooks" | jq -r --arg url "$webhook_url" '.[] | select(.config.url == $url) | .id')
    
    if [ -z "$webhook_id" ] || [ "$webhook_id" = "null" ]; then
        log "✗ No webhook found for ${repo}"
        return 1
    fi
    
    # Test webhook
    local response=$(curl -s -w "%{http_code}" \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        -X POST \
        "https://api.github.com/repos/${GITHUB_ORG}/${repo}/hooks/${webhook_id}/tests")
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" = "204" ]; then
        log "✓ Webhook test successful for ${repo}"
    else
        log "⚠ Webhook test failed for ${repo} (HTTP: $http_code)"
        log "This might be normal if Jenkins is not publicly accessible"
    fi
}

# Configure Jenkins GitHub plugin
configure_jenkins_github() {
    log "Configuring Jenkins GitHub plugin..."
    
    # Create GitHub server configuration
    local github_config=$(cat << 'EOF'
import jenkins.model.*
import org.jenkinsci.plugins.github.*
import org.jenkinsci.plugins.github.config.*

def instance = Jenkins.getInstance()
def descriptor = instance.getDescriptor(GitHubPlugin.class)

// Create GitHub server config
def githubConfig = new GitHubServerConfig("https://api.github.com")
githubConfig.setName("GitHub")
githubConfig.setManageHooks(true)

// Set configuration
descriptor.setConfigs([githubConfig])
descriptor.save()

println "GitHub plugin configured successfully"
EOF
)
    
    # Execute configuration
    echo "$github_config" | curl -s \
        -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
        -H "Content-Type: text/plain" \
        -d @- \
        "${JENKINS_URL}/scriptText" > /dev/null
    
    log "✓ Jenkins GitHub plugin configured"
}

# Create Jenkins jobs
create_jenkins_jobs() {
    log "Creating Jenkins jobs..."
    
    for repo in "${REPOSITORIES[@]}"; do
        log "Creating job for ${repo}..."
        
        # Determine Jenkinsfile path
        local jenkinsfile_path="Jenkinsfile"
        if [ "$repo" = "pet-clinic-backend" ]; then
            jenkinsfile_path="jenkins-pipelines/Jenkinsfile-backend"
        elif [ "$repo" = "pet-clinic-frontend" ]; then
            jenkinsfile_path="jenkins-pipelines/Jenkinsfile-frontend"
        fi
        
        # Create job XML configuration
        local job_xml=$(cat << EOF
<?xml version='1.1' encoding='UTF-8'?>
<org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject plugin="workflow-multibranch">
  <actions/>
  <description>CI/CD pipeline for ${repo}</description>
  <properties>
    <org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig plugin="pipeline-model-definition">
      <dockerLabel></dockerLabel>
      <registry plugin="docker-commons"/>
    </org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig>
  </properties>
  <folderViews class="jenkins.branch.MultiBranchProjectViewHolder" plugin="branch-api">
    <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
  </folderViews>
  <healthMetrics>
    <com.cloudbees.hudson.plugins.folder.health.WorstChildHealthMetric plugin="cloudbees-folder">
      <nonRecursive>false</nonRecursive>
    </com.cloudbees.hudson.plugins.folder.health.WorstChildHealthMetric>
  </healthMetrics>
  <icon class="jenkins.branch.MetadataActionFolderIcon" plugin="branch-api">
    <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
  </icon>
  <orphanedItemStrategy class="com.cloudbees.hudson.plugins.folder.computed.DefaultOrphanedItemStrategy" plugin="cloudbees-folder">
    <pruneDeadBranches>true</pruneDeadBranches>
    <daysToKeep>7</daysToKeep>
    <numToKeep>10</numToKeep>
  </orphanedItemStrategy>
  <triggers>
    <com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger plugin="cloudbees-folder">
      <spec>H H * * *</spec>
      <interval>86400000</interval>
    </com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger>
  </triggers>
  <disabled>false</disabled>
  <sources class="jenkins.branch.MultiBranchProject\$BranchSourceList" plugin="branch-api">
    <data>
      <jenkins.branch.BranchSource>
        <source class="org.jenkinsci.plugins.github_branch_source.GitHubSCMSource" plugin="github-branch-source">
          <id>${repo}-source</id>
          <repoOwner>${GITHUB_ORG}</repoOwner>
          <repository>${repo}</repository>
          <traits>
            <org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait>
              <strategyId>1</strategyId>
            </org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait>
            <org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait>
              <strategyId>2</strategyId>
            </org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait>
            <org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait>
              <strategyId>2</strategyId>
              <trust class="org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait\$TrustPermission"/>
            </org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait>
          </traits>
        </source>
        <strategy class="jenkins.branch.DefaultBranchPropertyStrategy">
          <properties class="empty-list"/>
        </strategy>
      </jenkins.branch.BranchSource>
    </data>
    <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
  </sources>
  <factory class="org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory">
    <owner class="org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject" reference="../.."/>
    <scriptPath>${jenkinsfile_path}</scriptPath>
  </factory>
</org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject>
EOF
)
        
        # Create job
        local response=$(curl -s -w "%{http_code}" \
            -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
            -H "Content-Type: application/xml" \
            -d "$job_xml" \
            "${JENKINS_URL}/createItem?name=${repo}")
        
        local http_code=$(echo "$response" | tail -n1)
        
        if [ "$http_code" = "200" ]; then
            log "✓ Job created successfully for ${repo}"
        else
            log "⚠ Job creation response for ${repo}: HTTP $http_code"
            log "Job might already exist or there was an error"
        fi
    done
}

# Main execution
main() {
    log "Starting GitHub webhook setup for Pet Clinic CI/CD Pipeline..."
    
    # Check prerequisites
    check_prerequisites
    
    # Test connectivity
    test_jenkins_connectivity
    test_github_connectivity
    
    # Configure Jenkins
    configure_jenkins_github
    
    # Create Jenkins jobs
    create_jenkins_jobs
    
    # Create webhooks for each repository
    for repo in "${REPOSITORIES[@]}"; do
        create_webhook "$repo"
        test_webhook "$repo"
    done
    
    log "GitHub webhook setup completed successfully!"
    log ""
    log "Summary:"
    log "- Jenkins URL: ${JENKINS_URL}"
    log "- GitHub Organization: ${GITHUB_ORG}"
    log "- Repositories configured: ${REPOSITORIES[*]}"
    log ""
    log "Next steps:"
    log "1. Verify Jenkins jobs are created: ${JENKINS_URL}/job/"
    log "2. Test webhook by pushing to any configured repository"
    log "3. Check Jenkins build history for automatic triggers"
}

# Execute main function
main "$@"