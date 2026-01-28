# Jenkins Pipeline Configuration for Pet Clinic CI/CD

This directory contains Jenkins pipeline configurations and related scripts for the Pet Clinic CI/CD Pipeline project.

## Files Overview

### 1. Jenkinsfile-backend
Complete CI/CD pipeline for the Pet Clinic Backend application.

**Pipeline Stages:**
- **Checkout**: Source code retrieval from Git
- **Build**: Maven compilation and packaging
- **Test**: Unit tests, integration tests, and property-based tests
- **Quality Analysis**: SonarQube analysis and security scanning
- **Package**: JAR creation and deployment package preparation
- **Deploy**: Deployment to Auto Scaling Groups
- **Health Check**: Application health verification and rollback

**Features:**
- Parallel test execution
- Quality gates with SonarQube
- OWASP dependency checking
- AWS deployment automation
- Automatic rollback on health check failure
- Slack and email notifications
- Build artifact archiving

### 2. Jenkinsfile-frontend
Complete CI/CD pipeline for the Pet Clinic Frontend application.

**Pipeline Stages:**
- **Checkout**: Source code retrieval from Git
- **Build**: Maven compilation with Thymeleaf templates
- **Test**: Unit tests, integration tests, and security tests
- **Quality Analysis**: SonarQube analysis, security scanning, and template validation
- **Package**: JAR creation with environment-specific configuration
- **Deploy**: Blue-green or rolling deployment strategies
- **Health Check**: Load balancer health checks and smoke tests

**Features:**
- Blue-green deployment support
- Template validation for Thymeleaf
- Load balancer integration
- Target group health monitoring
- Smoke testing for critical user journeys
- Integration with backend services

### 3. setup-webhooks.sh
Automated GitHub webhook configuration script.

**Features:**
- GitHub API integration
- Webhook creation and testing
- Jenkins job creation
- Multi-repository support
- Error handling and validation

## Pipeline Configuration

### Environment Variables

Both pipelines use the following environment variables:

#### Application Configuration
- `APP_NAME`: Application name (pet-clinic-backend/frontend)
- `APP_VERSION`: Build number for versioning
- `MAVEN_OPTS`: Maven JVM options
- `JAVA_HOME`: Java installation path

#### AWS Configuration
- `AWS_DEFAULT_REGION`: AWS region for deployment
- `AWS_ACCOUNT_ID`: AWS account identifier
- `STAGING_ASG`: Staging Auto Scaling Group name
- `PROD_ASG`: Production Auto Scaling Group name

#### Database Configuration (Backend)
- `DB_HOST`: Database host endpoint
- `DB_NAME`: Database name
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

#### Service Configuration (Frontend)
- `BACKEND_SERVICE_URL`: Backend service endpoint
- `STAGING_ALB_TG`: Staging target group ARN
- `PROD_ALB_TG`: Production target group ARN

#### Notification Configuration
- `SLACK_CHANNEL`: Slack channel for notifications
- `EMAIL_RECIPIENTS`: Email addresses for notifications

### Pipeline Parameters

Both pipelines support the following parameters:

- **ENVIRONMENT**: Target deployment environment (dev/staging/prod)
- **SKIP_TESTS**: Skip unit and integration tests
- **DEPLOY_ONLY**: Skip build and deploy existing artifact
- **ARTIFACT_VERSION**: Specific artifact version for deployment

Frontend pipeline additional parameters:
- **BLUE_GREEN_DEPLOYMENT**: Use blue-green deployment strategy

### Credentials Configuration

The following credentials must be configured in Jenkins:

#### AWS Credentials
- `aws-account-id`: AWS account ID
- `aws-credentials`: AWS access key and secret

#### Database Credentials
- `db-host`: Database endpoint
- `db-username`: Database username
- `db-password`: Database password

#### Service Credentials
- `backend-service-url`: Backend service URL

#### GitHub Credentials
- `github-ssh`: SSH key for GitHub access
- GitHub webhook token (for webhook setup)

## Setup Instructions

### 1. Prerequisites

Ensure the following are installed and configured:
- Jenkins with required plugins (see jenkins-config/plugins.txt)
- AWS CLI configured with appropriate permissions
- GitHub repository access
- SonarQube server (optional)

### 2. Jenkins Configuration

1. **Install Required Plugins**:
   ```bash
   # Use the plugin list from jenkins-config/plugins.txt
   java -jar jenkins-cli.jar install-plugin < jenkins-config/plugins.txt
   ```

2. **Configure Global Tools**:
   - Java 11 (Amazon Corretto)
   - Maven 3.8.6
   - Git

3. **Set Up Credentials**:
   - Add AWS credentials
   - Add database credentials
   - Add GitHub SSH key
   - Add service URLs

### 3. Repository Setup

1. **Copy Jenkinsfiles to Repositories**:
   ```bash
   # Copy to backend repository root
   cp Jenkinsfile-backend /path/to/pet-clinic-backend/Jenkinsfile
   
   # Copy to frontend repository root
   cp Jenkinsfile-frontend /path/to/pet-clinic-frontend/Jenkinsfile
   ```

2. **Configure GitHub Webhooks**:
   ```bash
   # Set environment variables
   export JENKINS_URL="http://your-jenkins-server:8080"
   export JENKINS_USER="admin"
   export JENKINS_TOKEN="your-jenkins-token"
   export GITHUB_TOKEN="your-github-token"
   export GITHUB_ORG="your-github-org"
   
   # Run webhook setup
   ./setup-webhooks.sh
   ```

### 4. Pipeline Testing

1. **Manual Trigger**:
   - Go to Jenkins dashboard
   - Navigate to job (pet-clinic-backend or pet-clinic-frontend)
   - Click "Build with Parameters"
   - Select environment and options
   - Click "Build"

2. **Automatic Trigger**:
   - Push code to GitHub repository
   - Webhook should trigger build automatically
   - Check Jenkins for new build

## Deployment Strategies

### Rolling Deployment (Default)
- Deploys to instances one by one
- Removes instance from load balancer during deployment
- Re-registers instance after successful deployment
- Minimizes downtime but slower deployment

### Blue-Green Deployment (Frontend)
- Creates new instances with updated application
- Switches traffic to new instances
- Keeps old instances for quick rollback
- Zero downtime but requires more resources

## Monitoring and Notifications

### Build Notifications
- **Slack**: Real-time notifications to configured channel
- **Email**: Detailed build reports to administrators
- **Jenkins UI**: Build status and logs

### Health Monitoring
- **Application Health**: `/actuator/health` endpoint checks
- **Load Balancer**: Target group health monitoring
- **Infrastructure**: Auto Scaling Group instance health

### Rollback Procedures
- Automatic rollback on health check failure
- Manual rollback through Jenkins parameters
- Blue-green deployment instant rollback

## Troubleshooting

### Common Issues

1. **Build Failures**:
   - Check Maven dependencies and versions
   - Verify Java version compatibility
   - Review test failures in Jenkins UI

2. **Deployment Failures**:
   - Verify AWS credentials and permissions
   - Check Auto Scaling Group configuration
   - Review instance logs in CloudWatch

3. **Health Check Failures**:
   - Verify application startup time
   - Check database connectivity
   - Review application logs

4. **Webhook Issues**:
   - Verify GitHub token permissions
   - Check Jenkins webhook URL accessibility
   - Review GitHub webhook delivery logs

### Debugging Steps

1. **Check Jenkins Logs**:
   ```bash
   # View Jenkins system logs
   tail -f /var/log/jenkins/jenkins.log
   
   # View build console output in Jenkins UI
   ```

2. **Check Application Logs**:
   ```bash
   # On EC2 instances
   sudo journalctl -u petclinic-backend -f
   sudo journalctl -u petclinic-frontend -f
   ```

3. **Check AWS Resources**:
   ```bash
   # Check Auto Scaling Group
   aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names your-asg
   
   # Check target group health
   aws elbv2 describe-target-health --target-group-arn your-target-group-arn
   ```

## Security Considerations

### Pipeline Security
- Use Jenkins credentials for sensitive data
- Implement approval processes for production deployments
- Audit pipeline changes and access

### Deployment Security
- Use IAM roles instead of access keys
- Encrypt sensitive data in transit and at rest
- Implement network security groups

### Code Security
- OWASP dependency checking
- SonarQube security analysis
- Container image scanning (if using Docker)

## Performance Optimization

### Build Performance
- Use Maven local repository caching
- Implement parallel test execution
- Optimize Docker layer caching

### Deployment Performance
- Use blue-green deployment for zero downtime
- Implement health check optimization
- Configure appropriate timeout values

## Maintenance

### Regular Tasks
- Update Jenkins plugins monthly
- Review and update pipeline configurations
- Monitor build performance metrics
- Clean up old build artifacts

### Backup and Recovery
- Jenkins configuration backup
- Pipeline definition version control
- Credential backup and rotation

## Integration Points

### External Services
- **GitHub**: Source code management and webhooks
- **AWS**: Infrastructure and deployment target
- **SonarQube**: Code quality analysis
- **Slack**: Team notifications
- **Email**: Administrative notifications

### Internal Services
- **Backend API**: Service integration testing
- **Database**: Connection and migration testing
- **Load Balancer**: Health check integration

## Metrics and Reporting

### Build Metrics
- Build success/failure rates
- Build duration trends
- Test coverage reports
- Quality gate compliance

### Deployment Metrics
- Deployment frequency
- Lead time for changes
- Mean time to recovery
- Change failure rate

These metrics align with DORA (DevOps Research and Assessment) key performance indicators for measuring DevOps effectiveness.