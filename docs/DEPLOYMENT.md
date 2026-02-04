# Pet Clinic CI/CD Pipeline - Deployment Guide

This comprehensive guide provides step-by-step instructions for deploying the Pet Clinic CI/CD Pipeline system on AWS.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Deployment](#quick-deployment)
3. [Step-by-Step Deployment](#step-by-step-deployment)
4. [Configuration](#configuration)
5. [Verification](#verification)
6. [Troubleshooting](#troubleshooting)

## Prerequisites

### AWS Account Setup

1. **AWS Account**: Active AWS account with billing enabled
2. **IAM Permissions**: Administrator access or specific permissions for:
   - CloudFormation (full access)
   - EC2 (full access)
   - RDS (full access)
   - S3 (full access)
   - IAM (full access)
   - EFS (full access)
   - CloudWatch (full access)

3. **AWS CLI Configuration**:
```bash
# Install AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configure credentials
aws configure
# AWS Access Key ID: [Your Access Key]
# AWS Secret Access Key: [Your Secret Key]
# Default region name: us-east-1
# Default output format: json
```

### Local Development Environment

1. **Required Software**:
   - Git 2.20+
   - Java 11 (OpenJDK recommended)
   - Maven 3.6+
   - Python 3.8+ (for testing scripts)

2. **Installation Commands**:
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install git openjdk-11-jdk maven python3 python3-pip

# macOS (using Homebrew)
brew install git openjdk@11 maven python3

# Verify installations
java -version
mvn -version
git --version
python3 --version
```

### GitHub Setup

1. **GitHub Personal Access Token**:
   - Go to GitHub Settings > Developer settings > Personal access tokens
   - Generate token with `repo`, `admin:repo_hook`, and `user` scopes
   - Save token securely

2. **SSH Key Setup** (optional but recommended):
```bash
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
cat ~/.ssh/id_rsa.pub
# Add the public key to your GitHub account
```

## Quick Deployment

For rapid deployment with default settings:

```bash
# 1. Clone the repository
git clone https://github.com/your-org/pet-clinic-cicd-pipeline.git
cd pet-clinic-cicd-pipeline

# 2. Set environment variables
export AWS_REGION="us-east-1"
export GITHUB_TOKEN="your-github-token"
export JENKINS_ADMIN_PASSWORD="SecurePassword123!"
export DB_PASSWORD="DatabasePassword123!"

# 3. Run quick deployment script
chmod +x scripts/quick-deploy.sh
./scripts/quick-deploy.sh

# 4. Get Jenkins URL
aws cloudformation describe-stacks \
  --stack-name pet-clinic-master \
  --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
  --output text
```

**Deployment Time**: Approximately 15-20 minutes

## Step-by-Step Deployment

### Phase 1: Infrastructure Deployment

#### Step 1: Deploy Master Stack

```bash
# Create the master CloudFormation stack
aws cloudformation create-stack \
  --stack-name pet-clinic-master \
  --template-body file://cloudformation/master-stack.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=production \
    ParameterKey=InstanceType,ParameterValue=t3.medium \
    ParameterKey=DBPassword,ParameterValue=YourSecurePassword123! \
    ParameterKey=JenkinsAdminPassword,ParameterValue=JenkinsPassword123! \
  --capabilities CAPABILITY_IAM \
  --region us-east-1

# Monitor deployment progress
aws cloudformation wait stack-create-complete \
  --stack-name pet-clinic-master \
  --region us-east-1

# Check stack status
aws cloudformation describe-stacks \
  --stack-name pet-clinic-master \
  --query 'Stacks[0].StackStatus' \
  --output text
```

#### Step 2: Verify Infrastructure Components

```bash
# Check all nested stacks
aws cloudformation list-stacks \
  --stack-status-filter CREATE_COMPLETE \
  --query 'StackSummaries[?contains(StackName, `pet-clinic`)].{Name:StackName,Status:StackStatus}' \
  --output table

# Get important outputs
aws cloudformation describe-stacks \
  --stack-name pet-clinic-master \
  --query 'Stacks[0].Outputs' \
  --output table
```

### Phase 2: Application Setup

#### Step 3: Configure Jenkins

```bash
# Get Jenkins URL and initial admin password
JENKINS_URL=$(aws cloudformation describe-stacks \
  --stack-name pet-clinic-master \
  --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
  --output text)

echo "Jenkins URL: $JENKINS_URL"
echo "Admin Password: JenkinsPassword123!"

# Wait for Jenkins to be ready
echo "Waiting for Jenkins to start..."
until curl -s "$JENKINS_URL/login" > /dev/null; do
  echo "Jenkins not ready yet, waiting 30 seconds..."
  sleep 30
done
echo "Jenkins is ready!"
```

#### Step 4: Setup GitHub Integration

```bash
# Configure GitHub webhooks
export JENKINS_URL="$JENKINS_URL"
export GITHUB_REPO="your-org/pet-clinic-cicd-pipeline"

# Run webhook setup script
chmod +x jenkins-pipelines/setup-webhooks.sh
./jenkins-pipelines/setup-webhooks.sh
```

#### Step 5: Configure MySQL Privilege System

```bash
# Install MySQL privilege configuration system
pip install -e mysql_privilege_config/

# Configure for production environment
mysql-privilege-config configure --environment production \
  --host $DB_HOST \
  --user petclinic \
  --password $DB_PASSWORD

# Validate MySQL privilege configuration
mysql-privilege-config validate --environment production

# Apply security configurations
mysql-privilege-config apply-security --environment production
```

#### Step 7: Monitoring and Security Setup

```bash
# Build and deploy the application
cd pet-clinic-app

# Build the application
mvn clean package -DskipTests

# Deploy backend
chmod +x ../deployment-scripts/deploy-backend.sh
../deployment-scripts/deploy-backend.sh

# Deploy frontend
chmod +x ../deployment-scripts/deploy-frontend.sh
../deployment-scripts/deploy-frontend.sh

# Verify deployment
chmod +x ../deployment-scripts/health-check.sh
../deployment-scripts/health-check.sh
```

### Phase 3: Monitoring and Security Setup

#### Step 8: Configure Monitoring

```bash
# Setup CloudWatch monitoring
chmod +x monitoring-config/setup-monitoring.sh
./monitoring-config/setup-monitoring.sh

# Deploy monitoring stacks
aws cloudformation create-stack \
  --stack-name pet-clinic-monitoring \
  --template-body file://monitoring-config/cloudwatch-alarms.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=production \
  --region us-east-1
```

#### Step 9: Configure Security

```bash
# Setup SSL certificates
chmod +x security-config/ssl-certificate-setup.sh
./security-config/ssl-certificate-setup.sh

# Apply security hardening
chmod +x security-config/security-hardening.sh
./security-config/security-hardening.sh

# Configure secrets management
chmod +x security-config/secrets-management.sh
./security-config/secrets-management.sh
```

#### Step 10: Setup Backup System

```bash
# Configure Jenkins backups
chmod +x jenkins-config/setup-thinbackup.sh
./jenkins-config/setup-thinbackup.sh

# Setup S3 backup synchronization
chmod +x jenkins-config/s3-backup-sync.sh
./jenkins-config/s3-backup-sync.sh

# Test backup system
chmod +x jenkins-config/backup-test.sh
./jenkins-config/backup-test.sh
```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```bash
# AWS Configuration
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012

# GitHub Configuration
GITHUB_TOKEN=your-github-token
GITHUB_REPO=your-org/pet-clinic-cicd-pipeline

# Jenkins Configuration
JENKINS_ADMIN_PASSWORD=SecurePassword123!
JENKINS_URL=http://your-jenkins-url

# Database Configuration
DB_PASSWORD=DatabasePassword123!
DB_HOST=your-rds-endpoint
DB_NAME=petclinic

# MySQL Privilege Configuration
MYSQL_PRIVILEGE_ENVIRONMENT=production
MYSQL_PRIVILEGE_HOST=your-rds-endpoint
MYSQL_PRIVILEGE_USER=petclinic
MYSQL_PRIVILEGE_PASSWORD=DatabasePassword123!
MYSQL_PRIVILEGE_SECURITY_LEVEL=restrictive

# Application Configuration
APP_ENV=production
LOG_LEVEL=INFO

# Monitoring Configuration
SLACK_WEBHOOK_URL=https://hooks.slack.com/your-webhook
EMAIL_NOTIFICATIONS=admin@yourcompany.com
```

### Custom Parameters

Modify CloudFormation parameters in `cloudformation/master-stack.yaml`:

```yaml
Parameters:
  Environment:
    Type: String
    Default: production
    AllowedValues: [development, staging, production]
  
  InstanceType:
    Type: String
    Default: t3.medium
    AllowedValues: [t3.small, t3.medium, t3.large]
  
  DBInstanceClass:
    Type: String
    Default: db.t3.micro
    AllowedValues: [db.t3.micro, db.t3.small, db.t3.medium]
```

## Verification

### Infrastructure Verification

```bash
# Check all CloudFormation stacks
aws cloudformation list-stacks \
  --stack-status-filter CREATE_COMPLETE UPDATE_COMPLETE \
  --query 'StackSummaries[?contains(StackName, `pet-clinic`)].{Name:StackName,Status:StackStatus}' \
  --output table

# Verify EC2 instances
aws ec2 describe-instances \
  --filters "Name=tag:Project,Values=pet-clinic" \
  --query 'Reservations[].Instances[].{ID:InstanceId,State:State.Name,Type:InstanceType}' \
  --output table

# Check RDS instance
aws rds describe-db-instances \
  --query 'DBInstances[?contains(DBInstanceIdentifier, `pet-clinic`)].{ID:DBInstanceIdentifier,Status:DBInstanceStatus,Engine:Engine}' \
  --output table
```

### Application Verification

```bash
# Get application URLs
ALB_URL=$(aws cloudformation describe-stacks \
  --stack-name pet-clinic-master \
  --query 'Stacks[0].Outputs[?OutputKey==`ApplicationURL`].OutputValue' \
  --output text)

# Test application endpoints
curl -f "$ALB_URL/health" || echo "Health check failed"
curl -f "$ALB_URL/api/owners" || echo "API check failed"
curl -f "$ALB_URL/" || echo "Frontend check failed"

# Run comprehensive health check
./deployment-scripts/health-check.sh
```

### Jenkins Verification

```bash
# Check Jenkins status
curl -f "$JENKINS_URL/login" || echo "Jenkins not accessible"

# Verify Jenkins jobs
curl -u admin:$JENKINS_ADMIN_PASSWORD \
  "$JENKINS_URL/api/json?tree=jobs[name,color]" | \
  python3 -m json.tool
```

### Security Verification

```bash
# Run security tests
cd tests/security
python3 -m pytest test-security-validation.py -v

# Check SSL certificate
echo | openssl s_client -connect $ALB_URL:443 -servername $ALB_URL 2>/dev/null | \
  openssl x509 -noout -dates
```

## Troubleshooting

### Common Issues

#### 1. CloudFormation Stack Creation Failed

**Symptoms**: Stack creation fails with resource errors

**Solutions**:
```bash
# Check stack events
aws cloudformation describe-stack-events \
  --stack-name pet-clinic-master \
  --query 'StackEvents[?ResourceStatus==`CREATE_FAILED`]' \
  --output table

# Common fixes:
# - Check IAM permissions
# - Verify parameter values
# - Check resource limits (VPC limits, EIP limits)
# - Ensure unique S3 bucket names
```

#### 2. Jenkins Not Accessible

**Symptoms**: Cannot access Jenkins URL

**Solutions**:
```bash
# Check security group rules
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=*jenkins*" \
  --query 'SecurityGroups[].IpPermissions'

# Check instance status
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=*jenkins*" \
  --query 'Reservations[].Instances[].[InstanceId,State.Name,PublicIpAddress]'

# SSH into Jenkins instance to check logs
ssh -i your-key.pem ec2-user@jenkins-ip
sudo journalctl -u jenkins -f
```

#### 3. Application Deployment Failed

**Symptoms**: Application not responding or deployment errors

**Solutions**:
```bash
# Check application logs
ssh -i your-key.pem ec2-user@app-instance-ip
sudo journalctl -u petclinic-backend -f
sudo journalctl -u petclinic-frontend -f

# Check database connectivity
mysql -h $DB_HOST -u petclinic -p$DB_PASSWORD -e "SELECT 1"

# Restart services
sudo systemctl restart petclinic-backend
sudo systemctl restart petclinic-frontend
```

#### 4. Database Connection Issues

**Symptoms**: Application cannot connect to database

**Solutions**:
```bash
# Check RDS status
aws rds describe-db-instances \
  --db-instance-identifier pet-clinic-db \
  --query 'DBInstances[0].DBInstanceStatus'

# Check security group rules
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=*database*" \
  --query 'SecurityGroups[].IpPermissions'

# Test connection from application server
telnet $DB_HOST 3306
```

### Getting Help

1. **Check Logs**: Always start by checking relevant logs
2. **AWS Console**: Use AWS Console for visual debugging
3. **CloudFormation Events**: Check stack events for detailed error messages
4. **GitHub Issues**: Report bugs and get community help
5. **AWS Support**: For AWS-specific issues

### Rollback Procedures

If deployment fails and you need to rollback:

```bash
# Rollback CloudFormation stack
aws cloudformation cancel-update-stack --stack-name pet-clinic-master

# Or delete and recreate
aws cloudformation delete-stack --stack-name pet-clinic-master
aws cloudformation wait stack-delete-complete --stack-name pet-clinic-master

# Clean up resources that might not be deleted automatically
# (S3 buckets, EFS file systems, etc.)
```

## Next Steps

After successful deployment:

1. **Configure Monitoring**: Set up custom dashboards and alerts
2. **Security Hardening**: Apply additional security measures
3. **Performance Tuning**: Optimize application and infrastructure
4. **Backup Testing**: Regularly test backup and restore procedures
5. **Documentation**: Update documentation with environment-specific details

For more detailed information, see:
- [Architecture Guide](ARCHITECTURE.md)
- [Local Development Guide](LOCAL-DEVELOPMENT.md)
- [Troubleshooting Guide](TROUBLESHOOTING.md)