# Pet Clinic CI/CD Pipeline - Cloud Deployment Guide

This guide provides detailed instructions for deploying the Pet Clinic CI/CD Pipeline system to AWS cloud infrastructure in production environments.

## Table of Contents

1. [Pre-Deployment Planning](#pre-deployment-planning)
2. [AWS Account Preparation](#aws-account-preparation)
3. [Infrastructure Deployment](#infrastructure-deployment)
4. [Application Deployment](#application-deployment)
5. [Post-Deployment Configuration](#post-deployment-configuration)
6. [Production Considerations](#production-considerations)
7. [Monitoring and Maintenance](#monitoring-and-maintenance)
8. [Disaster Recovery](#disaster-recovery)

## Pre-Deployment Planning

### 1. Architecture Review

Before deployment, review the target architecture:

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS Production Environment               │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  Availability   │  │  Availability   │  │    Management   │ │
│  │    Zone A       │  │    Zone B       │  │     Zone        │ │
│  │                 │  │                 │  │                 │ │
│  │ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │ │
│  │ │   Public    │ │  │ │   Public    │ │  │ │   Jenkins   │ │ │
│  │ │   Subnet    │ │  │ │   Subnet    │ │  │ │   Server    │ │ │
│  │ │             │ │  │ │             │ │  │ │             │ │ │
│  │ │ • ALB       │ │  │ │ • ALB       │ │  │ │ • CI/CD     │ │ │
│  │ │ • NAT GW    │ │  │ │ • NAT GW    │ │  │ │ • Backups   │ │ │
│  │ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │ │
│  │                 │  │                 │  │                 │ │
│  │ ┌─────────────┐ │  │ ┌─────────────┐ │  │                 │ │
│  │ │   Private   │ │  │ │   Private   │ │  │                 │ │
│  │ │   Subnet    │ │  │ │   Subnet    │ │  │                 │ │
│  │ │             │ │  │ │             │ │  │                 │ │
│  │ │ • App Tier  │ │  │ │ • App Tier  │ │  │                 │ │
│  │ │ • EFS Mount │ │  │ │ • EFS Mount │ │  │                 │ │
│  │ └─────────────┘ │  │ └─────────────┘ │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  │ ┌─────────────┐ │  │ ┌─────────────┐ │  │                 │ │
│  │ │  Database   │ │  │ │  Database   │ │  │                 │ │
│  │ │   Subnet    │ │  │ │   Subnet    │ │  │                 │ │
│  │ │             │ │  │ │             │ │  │                 │ │
│  │ │ • RDS       │ │  │ │ • RDS       │ │  │                 │ │
│  │ │   Primary   │ │  │ │   Standby   │ │  │                 │ │
│  │ └─────────────┘ │  │ └─────────────┘ │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Capacity Planning

#### Compute Resources
| Environment | Component | Instance Type | Min | Max | Target CPU |
|-------------|-----------|---------------|-----|-----|------------|
| Production | Jenkins | t3.large | 1 | 1 | N/A |
| Production | Frontend | t3.medium | 2 | 8 | 70% |
| Production | Backend | t3.medium | 2 | 8 | 70% |
| Production | Database | db.t3.small | 1 | 1 | N/A |

#### Storage Requirements
| Component | Type | Size | IOPS | Backup |
|-----------|------|------|------|--------|
| Jenkins | EBS gp3 | 100 GB | 3000 | Daily |
| Application | EBS gp3 | 50 GB | 3000 | AMI |
| Database | RDS gp3 | 100 GB | 3000 | 7 days |
| Backups | S3 | Unlimited | N/A | Cross-region |

### 3. Cost Estimation

```bash
# Use AWS Pricing Calculator
# https://calculator.aws/

# Estimated monthly costs (us-east-1):
# - EC2 instances: $150-300
# - RDS database: $50-100
# - Load balancer: $25
# - S3 storage: $10-20
# - Data transfer: $10-30
# - CloudWatch: $10-20
# Total: ~$255-495/month
```

## AWS Account Preparation

### 1. Account Setup

#### Root Account Security
```bash
# 1. Enable MFA on root account
# 2. Create IAM admin user (don't use root for daily operations)
# 3. Set up billing alerts
# 4. Enable CloudTrail
# 5. Configure AWS Config
```

#### Service Limits
```bash
# Check and request limit increases if needed
aws service-quotas get-service-quota \
  --service-code ec2 \
  --quota-code L-1216C47A  # Running On-Demand instances

aws service-quotas get-service-quota \
  --service-code rds \
  --quota-code L-7B6409FD  # DB instances

# Request increases through AWS Support if needed
```

### 2. IAM Setup

#### Create Deployment User
```bash
# Create IAM user for deployment
aws iam create-user --user-name petclinic-deployer

# Create and attach policy
cat > deployment-policy.json << 'EOF'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cloudformation:*",
                "ec2:*",
                "rds:*",
                "s3:*",
                "iam:*",
                "efs:*",
                "elasticloadbalancing:*",
                "autoscaling:*",
                "cloudwatch:*",
                "logs:*",
                "sns:*",
                "ssm:*"
            ],
            "Resource": "*"
        }
    ]
}
EOF

aws iam create-policy \
  --policy-name PetClinicDeploymentPolicy \
  --policy-document file://deployment-policy.json

aws iam attach-user-policy \
  --user-name petclinic-deployer \
  --policy-arn arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):policy/PetClinicDeploymentPolicy

# Create access keys
aws iam create-access-key --user-name petclinic-deployer
```

### 3. Network Planning

#### VPC Design
```bash
# Plan IP addressing
# VPC CIDR: 10.0.0.0/16
# Public Subnet AZ-A: 10.0.1.0/24
# Public Subnet AZ-B: 10.0.2.0/24
# Private Subnet AZ-A: 10.0.10.0/24
# Private Subnet AZ-B: 10.0.20.0/24
# Database Subnet AZ-A: 10.0.100.0/24
# Database Subnet AZ-B: 10.0.200.0/24
```

#### DNS Planning
```bash
# Register domain or use existing
# Plan subdomain structure:
# - app.yourdomain.com (Application)
# - jenkins.yourdomain.com (Jenkins)
# - api.yourdomain.com (API)
```

## Infrastructure Deployment

### 1. Pre-Deployment Validation

#### Validate Templates
```bash
# Validate all CloudFormation templates
for template in cloudformation/*.yaml; do
    echo "Validating $template..."
    aws cloudformation validate-template \
      --template-body file://$template
done

# Run template linting
pip install cfn-lint
cfn-lint cloudformation/*.yaml
```

#### Parameter Validation
```bash
# Create parameter file for production
cat > cloudformation/production-parameters.json << 'EOF'
[
    {
        "ParameterKey": "Environment",
        "ParameterValue": "production"
    },
    {
        "ParameterKey": "InstanceType",
        "ParameterValue": "t3.medium"
    },
    {
        "ParameterKey": "DBInstanceClass",
        "ParameterValue": "db.t3.small"
    },
    {
        "ParameterKey": "DBPassword",
        "ParameterValue": "REPLACE_WITH_SECURE_PASSWORD"
    },
    {
        "ParameterKey": "JenkinsAdminPassword",
        "ParameterValue": "REPLACE_WITH_SECURE_PASSWORD"
    },
    {
        "ParameterKey": "KeyPairName",
        "ParameterValue": "your-key-pair"
    },
    {
        "ParameterKey": "AllowedCIDR",
        "ParameterValue": "0.0.0.0/0"
    }
]
EOF
```

### 2. Staged Deployment

#### Phase 1: Core Infrastructure
```bash
# Deploy master stack
aws cloudformation create-stack \
  --stack-name petclinic-production \
  --template-body file://cloudformation/master-stack.yaml \
  --parameters file://cloudformation/production-parameters.json \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
  --region us-east-1 \
  --tags Key=Environment,Value=production Key=Project,Value=petclinic

# Monitor deployment
aws cloudformation wait stack-create-complete \
  --stack-name petclinic-production \
  --region us-east-1

# Check stack status
aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].StackStatus' \
  --output text
```

#### Phase 2: Verify Infrastructure
```bash
# Get stack outputs
aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].Outputs' \
  --output table

# Test connectivity
JENKINS_URL=$(aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
  --output text)

ALB_URL=$(aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].Outputs[?OutputKey==`ApplicationURL`].OutputValue' \
  --output text)

echo "Jenkins URL: $JENKINS_URL"
echo "Application URL: $ALB_URL"

# Test endpoints
curl -I $JENKINS_URL
curl -I $ALB_URL
```

### 3. Infrastructure Validation

#### Security Validation
```bash
# Check security groups
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=*petclinic*" \
  --query 'SecurityGroups[].{Name:GroupName,Rules:IpPermissions}' \
  --output table

# Verify encryption
aws rds describe-db-instances \
  --query 'DBInstances[?contains(DBInstanceIdentifier, `petclinic`)].{ID:DBInstanceIdentifier,Encrypted:StorageEncrypted}'

aws s3api get-bucket-encryption \
  --bucket $(aws cloudformation describe-stacks \
    --stack-name petclinic-production \
    --query 'Stacks[0].Outputs[?OutputKey==`BackupBucket`].OutputValue' \
    --output text)
```

#### Network Validation
```bash
# Test network connectivity
VPC_ID=$(aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].Outputs[?OutputKey==`VPCId`].OutputValue' \
  --output text)

# Check route tables
aws ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'RouteTables[].{ID:RouteTableId,Routes:Routes}' \
  --output table

# Verify NAT Gateway connectivity
aws ec2 describe-nat-gateways \
  --filter "Name=vpc-id,Values=$VPC_ID" \
  --query 'NatGateways[].{ID:NatGatewayId,State:State,SubnetId:SubnetId}'
```

## Application Deployment

### 1. Jenkins Configuration

#### Initial Setup
```bash
# Wait for Jenkins to be ready
echo "Waiting for Jenkins to start..."
until curl -s $JENKINS_URL/login > /dev/null; do
    echo "Jenkins not ready yet, waiting 30 seconds..."
    sleep 30
done

# Get initial admin password
JENKINS_INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=*jenkins*" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text)

# SSH into Jenkins instance to get password
aws ssm start-session --target $JENKINS_INSTANCE_ID
# Once connected: sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

#### Configure Jenkins
```bash
# Upload Jenkins configuration
scp -i your-key.pem jenkins-config/jenkins-casc.yaml ec2-user@$JENKINS_URL:/tmp/

# SSH and apply configuration
ssh -i your-key.pem ec2-user@$JENKINS_URL << 'EOF'
sudo cp /tmp/jenkins-casc.yaml /var/lib/jenkins/
sudo chown jenkins:jenkins /var/lib/jenkins/jenkins-casc.yaml
sudo systemctl restart jenkins
EOF
```

### 2. Application Build and Deploy

#### Build Application
```bash
# Build application locally first
cd pet-clinic-app
mvn clean package -DskipTests

# Upload artifacts to S3
ARTIFACT_BUCKET=$(aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].Outputs[?OutputKey==`ArtifactBucket`].OutputValue' \
  --output text)

aws s3 cp pet-clinic-backend/target/pet-clinic-backend-1.0.0.jar \
  s3://$ARTIFACT_BUCKET/artifacts/backend/

aws s3 cp pet-clinic-frontend/target/pet-clinic-frontend-1.0.0.jar \
  s3://$ARTIFACT_BUCKET/artifacts/frontend/
```

#### Deploy to EC2 Instances
```bash
# Get application instance IDs
BACKEND_INSTANCES=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=*backend*" "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text)

FRONTEND_INSTANCES=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=*frontend*" "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text)

# Deploy backend
for instance in $BACKEND_INSTANCES; do
    echo "Deploying backend to $instance"
    aws ssm send-command \
      --instance-ids $instance \
      --document-name "AWS-RunShellScript" \
      --parameters 'commands=[
        "sudo systemctl stop petclinic-backend",
        "aws s3 cp s3://'$ARTIFACT_BUCKET'/artifacts/backend/pet-clinic-backend-1.0.0.jar /opt/petclinic/",
        "sudo systemctl start petclinic-backend"
      ]'
done

# Deploy frontend
for instance in $FRONTEND_INSTANCES; do
    echo "Deploying frontend to $instance"
    aws ssm send-command \
      --instance-ids $instance \
      --document-name "AWS-RunShellScript" \
      --parameters 'commands=[
        "sudo systemctl stop petclinic-frontend",
        "aws s3 cp s3://'$ARTIFACT_BUCKET'/artifacts/frontend/pet-clinic-frontend-1.0.0.jar /opt/petclinic/",
        "sudo systemctl start petclinic-frontend"
      ]'
done
```

### 3. Database Setup

#### Initialize Database
```bash
# Get RDS endpoint
DB_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
  --output text)

# Connect through bastion host or VPN
# Initialize schema
mysql -h $DB_ENDPOINT -u petclinic -p << 'EOF'
CREATE DATABASE IF NOT EXISTS petclinic;
USE petclinic;

-- Run schema creation scripts
SOURCE pet-clinic-app/pet-clinic-backend/src/main/resources/schema.sql;
SOURCE pet-clinic-app/pet-clinic-backend/src/main/resources/data.sql;
EOF
```

## Post-Deployment Configuration

### 1. SSL/TLS Configuration

#### Request SSL Certificate
```bash
# Request certificate through ACM
aws acm request-certificate \
  --domain-name yourdomain.com \
  --subject-alternative-names "*.yourdomain.com" \
  --validation-method DNS \
  --region us-east-1

# Get certificate ARN
CERT_ARN=$(aws acm list-certificates \
  --query 'CertificateSummaryList[?DomainName==`yourdomain.com`].CertificateArn' \
  --output text)

# Update ALB to use HTTPS
aws elbv2 create-listener \
  --load-balancer-arn $(aws elbv2 describe-load-balancers \
    --names petclinic-alb \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text) \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=$CERT_ARN \
  --default-actions Type=forward,TargetGroupArn=$(aws elbv2 describe-target-groups \
    --names petclinic-frontend-tg \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)
```

### 2. DNS Configuration

#### Route 53 Setup
```bash
# Create hosted zone (if not exists)
aws route53 create-hosted-zone \
  --name yourdomain.com \
  --caller-reference $(date +%s)

# Get hosted zone ID
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
  --query 'HostedZones[?Name==`yourdomain.com.`].Id' \
  --output text | cut -d'/' -f3)

# Create DNS records
cat > dns-records.json << EOF
{
    "Changes": [
        {
            "Action": "CREATE",
            "ResourceRecordSet": {
                "Name": "app.yourdomain.com",
                "Type": "A",
                "AliasTarget": {
                    "DNSName": "$ALB_URL",
                    "EvaluateTargetHealth": false,
                    "HostedZoneId": "Z35SXDOTRQ7X7K"
                }
            }
        },
        {
            "Action": "CREATE",
            "ResourceRecordSet": {
                "Name": "jenkins.yourdomain.com",
                "Type": "A",
                "TTL": 300,
                "ResourceRecords": [
                    {
                        "Value": "$(aws ec2 describe-instances \
                          --instance-ids $JENKINS_INSTANCE_ID \
                          --query 'Reservations[0].Instances[0].PublicIpAddress' \
                          --output text)"
                    }
                ]
            }
        }
    ]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://dns-records.json
```

### 3. Monitoring Setup

#### CloudWatch Dashboards
```bash
# Deploy monitoring stack
aws cloudformation create-stack \
  --stack-name petclinic-monitoring \
  --template-body file://monitoring-config/cloudwatch-dashboard.yaml \
  --parameters ParameterKey=Environment,ParameterValue=production \
  --region us-east-1

# Deploy alarms
aws cloudformation create-stack \
  --stack-name petclinic-alarms \
  --template-body file://monitoring-config/cloudwatch-alarms.yaml \
  --parameters ParameterKey=Environment,ParameterValue=production \
  --region us-east-1
```

#### Log Aggregation
```bash
# Configure CloudWatch Logs agent on instances
for instance in $BACKEND_INSTANCES $FRONTEND_INSTANCES; do
    aws ssm send-command \
      --instance-ids $instance \
      --document-name "AWS-RunShellScript" \
      --parameters 'commands=[
        "sudo yum install -y awslogs",
        "sudo systemctl enable awslogsd",
        "sudo systemctl start awslogsd"
      ]'
done
```

## Production Considerations

### 1. Security Hardening

#### Network Security
```bash
# Update security groups for production
# Remove SSH access from 0.0.0.0/0
aws ec2 authorize-security-group-ingress \
  --group-id sg-jenkins \
  --protocol tcp \
  --port 22 \
  --source-group sg-bastion

# Enable VPC Flow Logs
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids $VPC_ID \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-group-name VPCFlowLogs
```

#### Secrets Management
```bash
# Store secrets in Parameter Store
aws ssm put-parameter \
  --name "/petclinic/production/db-password" \
  --value "your-secure-db-password" \
  --type "SecureString"

aws ssm put-parameter \
  --name "/petclinic/production/jenkins-admin-password" \
  --value "your-secure-jenkins-password" \
  --type "SecureString"
```

### 2. Backup Configuration

#### Automated Backups
```bash
# Configure RDS backups
aws rds modify-db-instance \
  --db-instance-identifier petclinic-db \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "sun:04:00-sun:05:00"

# Setup Jenkins backup
ssh -i your-key.pem ec2-user@$JENKINS_URL << 'EOF'
sudo /opt/jenkins-config/setup-thinbackup.sh
sudo /opt/jenkins-config/s3-backup-sync.sh
EOF
```

### 3. Performance Optimization

#### Auto Scaling Configuration
```bash
# Update Auto Scaling policies
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name petclinic-backend-asg \
  --policy-name scale-out \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ASGAverageCPUUtilization"
    }
  }'
```

## Monitoring and Maintenance

### 1. Health Monitoring

#### Application Health Checks
```bash
# Create health check script
cat > scripts/health-check-production.sh << 'EOF'
#!/bin/bash

echo "=== Production Health Check ==="

# Check application endpoints
echo "Checking application health..."
curl -f https://app.yourdomain.com/health || echo "Frontend health check failed"
curl -f https://app.yourdomain.com/api/health || echo "Backend health check failed"

# Check Jenkins
echo "Checking Jenkins..."
curl -f https://jenkins.yourdomain.com/login || echo "Jenkins health check failed"

# Check database connectivity
echo "Checking database..."
mysql -h $DB_ENDPOINT -u petclinic -p$DB_PASSWORD -e "SELECT 1" || echo "Database check failed"

# Check AWS resources
echo "Checking AWS resources..."
aws cloudformation describe-stacks \
  --stack-name petclinic-production \
  --query 'Stacks[0].StackStatus' \
  --output text

echo "=== Health Check Complete ==="
EOF

chmod +x scripts/health-check-production.sh
```

### 2. Log Analysis

#### Centralized Logging
```bash
# Query CloudWatch Logs
aws logs describe-log-groups \
  --log-group-name-prefix "/aws/ec2/petclinic"

# Search for errors
aws logs filter-log-events \
  --log-group-name "/aws/ec2/petclinic/application" \
  --filter-pattern "ERROR" \
  --start-time $(date -d "1 hour ago" +%s)000
```

### 3. Performance Monitoring

#### Metrics Analysis
```bash
# Get CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name TargetResponseTime \
  --dimensions Name=LoadBalancer,Value=app/petclinic-alb/1234567890abcdef \
  --statistics Average \
  --start-time $(date -d "1 hour ago" --iso-8601) \
  --end-time $(date --iso-8601) \
  --period 300
```

## Disaster Recovery

### 1. Backup Verification

#### Test Restore Procedures
```bash
# Test database restore
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier petclinic-db-test \
  --db-snapshot-identifier petclinic-db-snapshot-$(date +%Y%m%d) \
  --db-instance-class db.t3.micro

# Test Jenkins restore
./jenkins-config/disaster-recovery.sh test
```

### 2. Failover Procedures

#### Multi-Region Setup
```bash
# Deploy to secondary region
aws cloudformation create-stack \
  --stack-name petclinic-dr \
  --template-body file://cloudformation/master-stack.yaml \
  --parameters file://cloudformation/dr-parameters.json \
  --capabilities CAPABILITY_IAM \
  --region us-west-2
```

### 3. Recovery Testing

#### Regular DR Drills
```bash
# Schedule monthly DR tests
cat > scripts/dr-test.sh << 'EOF'
#!/bin/bash
echo "Starting DR test..."

# Test backup restore
./jenkins-config/disaster-recovery.sh

# Test application deployment
./deployment-scripts/deploy-backend.sh
./deployment-scripts/deploy-frontend.sh

# Verify functionality
./scripts/health-check-production.sh

echo "DR test complete"
EOF

chmod +x scripts/dr-test.sh

# Add to cron for monthly execution
echo "0 2 1 * * /path/to/scripts/dr-test.sh" | crontab -
```

This cloud deployment guide provides comprehensive instructions for deploying and maintaining the Pet Clinic CI/CD Pipeline in a production AWS environment with proper security, monitoring, and disaster recovery capabilities.