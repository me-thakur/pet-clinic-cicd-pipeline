# Pet Clinic AWS Cloud Deployment Guide

This comprehensive guide covers deploying the Pet Clinic application and CI/CD pipeline to AWS Cloud infrastructure.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Infrastructure Architecture](#infrastructure-architecture)
4. [Pre-Deployment Setup](#pre-deployment-setup)
5. [Infrastructure Deployment](#infrastructure-deployment)
6. [Application Deployment](#application-deployment)
7. [CI/CD Pipeline Setup](#cicd-pipeline-setup)
8. [Monitoring and Alerting](#monitoring-and-alerting)
9. [Security Configuration](#security-configuration)
10. [Backup and Disaster Recovery](#backup-and-disaster-recovery)
11. [Troubleshooting](#troubleshooting)
12. [Cost Optimization](#cost-optimization)
13. [Maintenance](#maintenance)

## Overview

The Pet Clinic application is deployed on AWS using a comprehensive infrastructure-as-code approach with:

- **Multi-tier architecture** with separate frontend and backend services
- **Auto-scaling** EC2 instances with Application Load Balancer
- **RDS MySQL** database with Multi-AZ deployment
- **Jenkins CI/CD pipeline** for automated deployments
- **Comprehensive monitoring** with CloudWatch and custom metrics
- **Security hardening** with IAM roles, security groups, and encryption
- **Automated backups** and disaster recovery procedures

## Prerequisites

### AWS Account Requirements

1. **AWS Account** with administrative access
2. **AWS CLI v2** installed and configured
3. **Sufficient service limits** for the following resources:
   - VPCs: 1
   - EC2 instances: 5-10 (depending on environment)
   - RDS instances: 1-2
   - Application Load Balancers: 1
   - S3 buckets: 3
   - IAM roles: 5

### Local Development Environment

```bash
# Required tools
aws --version          # AWS CLI v2.0+
git --version         # Git 2.0+
java --version        # Java 11+
mvn --version         # Maven 3.6+
docker --version      # Docker 20.0+ (optional)
```

### Required Permissions

Your AWS user/role needs the following permissions:
- CloudFormation: Full access
- EC2: Full access
- RDS: Full access
- S3: Full access
- IAM: Full access
- VPC: Full access
- CloudWatch: Full access
- Systems Manager: Parameter access

## Infrastructure Architecture

### Network Architecture

```
Internet Gateway
       |
   Public Subnet (10.0.1.0/24)
   ├── Jenkins Server
   ├── NAT Gateway
   └── Application Load Balancer
       |
   Private Subnet (10.0.2.0/24)
   ├── Frontend Auto Scaling Group (2-3 instances)
   ├── Backend Auto Scaling Group (2-3 instances)
   └── RDS MySQL (Multi-AZ)
```

### Component Overview

| Component | Purpose | High Availability |
|-----------|---------|-------------------|
| VPC | Network isolation | Single AZ |
| Public Subnet | Internet-facing resources | Single AZ |
| Private Subnet | Application and database | Single AZ |
| Application Load Balancer | Traffic distribution | Multi-AZ capable |
| Auto Scaling Groups | Application scaling | Multi-AZ capable |
| RDS MySQL | Database | Multi-AZ |
| S3 Buckets | Artifacts, backups, logs | Multi-AZ |
| EFS | Shared installation scripts | Multi-AZ |

## Pre-Deployment Setup

### 1. Clone Repository

```bash
git clone <repository-url>
cd pet-clinic-cicd
```

### 2. Configure AWS Credentials

```bash
aws configure
# Enter your AWS Access Key ID, Secret Access Key, Region, and Output format
```

### 3. Set Environment Variables

```bash
# Required environment variables
export ENVIRONMENT="production"  # or "staging", "dev"
export AWS_REGION="us-east-1"
export DB_PASSWORD="your-secure-database-password"
export JENKINS_ADMIN_PASSWORD="your-secure-jenkins-password"

# Optional environment variables
export INSTANCE_TYPE="t3.medium"
export DB_INSTANCE_CLASS="db.t3.micro"
export KEY_PAIR_NAME="your-ec2-key-pair"  # For SSH access
export ALLOWED_CIDR="0.0.0.0/0"  # Restrict in production
```

### 4. Create EC2 Key Pair (Optional)

```bash
# Create key pair for SSH access
aws ec2 create-key-pair \
    --key-name pet-clinic-key \
    --query 'KeyMaterial' \
    --output text > ~/.ssh/pet-clinic-key.pem

chmod 400 ~/.ssh/pet-clinic-key.pem
export KEY_PAIR_NAME="pet-clinic-key"
```

## Infrastructure Deployment

### 1. Validate CloudFormation Templates

```bash
# Validate all templates
./scripts/deploy-infrastructure.sh --validate-only
```

### 2. Deploy Infrastructure Stack

```bash
# Deploy complete infrastructure
./scripts/deploy-infrastructure.sh
```

The deployment process includes:

1. **IAM Stack**: Roles and policies for EC2 instances
2. **Network Stack**: VPC, subnets, security groups, NAT gateway
3. **Storage Stack**: S3 buckets, EFS file system
4. **Database Stack**: RDS MySQL with Multi-AZ
5. **Compute Stack**: EC2 instances, Auto Scaling Groups, Load Balancer

### 3. Monitor Deployment Progress

```bash
# Check stack status
aws cloudformation describe-stacks \
    --stack-name petclinic-production \
    --query 'Stacks[0].StackStatus'

# View stack events
aws cloudformation describe-stack-events \
    --stack-name petclinic-production \
    --query 'StackEvents[0:10].{Time:Timestamp,Status:ResourceStatus,Type:ResourceType}'
```

### 4. Retrieve Infrastructure Outputs

```bash
# Get all stack outputs
aws cloudformation describe-stacks \
    --stack-name petclinic-production \
    --query 'Stacks[0].Outputs'
```

Key outputs include:
- **JenkinsURL**: Jenkins server access URL
- **ApplicationURL**: Application Load Balancer URL
- **DatabaseEndpoint**: RDS database endpoint
- **VPCId**: VPC identifier
- **BackupBucket**: S3 bucket for backups

## Application Deployment

### 1. Access Jenkins Server

```bash
# Get Jenkins URL from stack outputs
JENKINS_URL=$(aws cloudformation describe-stacks \
    --stack-name petclinic-production \
    --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
    --output text)

echo "Jenkins URL: $JENKINS_URL"
```

Access Jenkins at the provided URL:
- **Username**: `admin`
- **Password**: Value of `JENKINS_ADMIN_PASSWORD`

### 2. Configure Jenkins

Jenkins is pre-configured with:
- **Jenkins Configuration as Code (JCasC)**
- **Pre-installed plugins** for Maven, Git, AWS
- **Automated job creation** for frontend and backend
- **Security configuration** with role-based access

### 3. Set Up GitHub Integration

1. **Add GitHub SSH Key** to Jenkins credentials
2. **Configure webhook** in GitHub repository
3. **Update repository URLs** in Jenkins jobs

```bash
# Generate SSH key for GitHub
ssh-keygen -t rsa -b 4096 -C "jenkins@petclinic.local" -f ~/.ssh/jenkins_github
```

### 4. Deploy Applications

#### Backend Deployment

```bash
# Set environment variables for backend
export DB_HOST=$(aws cloudformation describe-stacks \
    --stack-name petclinic-production \
    --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
    --output text)

export DB_NAME="petclinicdb"
export DB_USER="petclinicadmin"
export AWS_REGION="us-east-1"

# Run backend deployment script
./deployment-scripts/deploy-backend.sh
```

#### Frontend Deployment

```bash
# Set environment variables for frontend
export BACKEND_URL="http://internal-backend-alb.amazonaws.com"
export AWS_REGION="us-east-1"

# Run frontend deployment script
./deployment-scripts/deploy-frontend.sh
```

### 5. Verify Application Deployment

```bash
# Check application health
curl -f http://<application-url>/actuator/health

# Check backend API
curl -f http://<application-url>/api/actuator/health

# Test database connectivity
curl -f http://<application-url>/api/actuator/health/db
```

## CI/CD Pipeline Setup

### 1. Jenkins Pipeline Configuration

The Jenkins server includes pre-configured pipelines:

#### Backend Pipeline (`Jenkinsfile-backend`)
- **Source Code Checkout** from GitHub
- **Maven Build** with dependency resolution
- **Unit Tests** execution with JUnit
- **Property-Based Tests** with jqwik
- **Code Quality Analysis** with SonarQube
- **Security Scanning** with OWASP dependency check
- **Docker Image Build** (optional)
- **Deployment** to staging/production
- **Health Check Verification**
- **Rollback** on deployment failure

#### Frontend Pipeline (`Jenkinsfile-frontend`)
- **Source Code Checkout** from GitHub
- **Maven Build** with Thymeleaf templates
- **Unit Tests** execution
- **Integration Tests** with Selenium
- **Static Analysis** with ESLint/SonarQube
- **Deployment** to staging/production
- **UI Health Check**
- **Rollback** on deployment failure

### 2. Automated Deployment Triggers

- **Push to main branch**: Triggers production deployment
- **Pull Request**: Triggers staging deployment and testing
- **Scheduled builds**: Nightly builds for dependency updates
- **Manual triggers**: For hotfixes and emergency deployments

### 3. Pipeline Monitoring

```bash
# View Jenkins logs
sudo journalctl -u jenkins -f

# Check pipeline status via API
curl -u admin:$JENKINS_ADMIN_PASSWORD \
    "$JENKINS_URL/job/pet-clinic/job/pet-clinic-backend/lastBuild/api/json"
```

## Monitoring and Alerting

### 1. Deploy Monitoring Stack

```bash
# Set up comprehensive monitoring
export ALERT_EMAIL="admin@yourcompany.com"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."  # Optional
export PAGERDUTY_INTEGRATION_KEY="your-pagerduty-key"  # Optional

./monitoring-config/setup-monitoring.sh
```

### 2. CloudWatch Dashboards

The monitoring setup creates dashboards for:

- **Infrastructure Metrics**: CPU, memory, disk, network
- **Application Metrics**: Response times, error rates, throughput
- **Database Metrics**: Connections, CPU, storage, replication lag
- **Jenkins Metrics**: Build success rates, queue length, executor usage

### 3. Alerting Configuration

Automated alerts are configured for:

| Metric | Threshold | Action |
|--------|-----------|--------|
| EC2 CPU Utilization | > 80% | Email + Slack |
| RDS CPU Utilization | > 80% | Email + PagerDuty |
| Application Error Rate | > 5% | Email + Slack |
| Database Connections | > 80% | Email |
| Disk Space | > 85% | Email |
| Jenkins Build Failures | > 3 consecutive | Email + Slack |

### 4. Custom Metrics

The deployment includes a custom metrics publisher that tracks:
- Application-specific business metrics
- Custom performance indicators
- Jenkins pipeline metrics
- Security event metrics

## Security Configuration

### 1. Network Security

- **VPC Isolation**: All resources in private VPC
- **Security Groups**: Restrictive inbound/outbound rules
- **NACLs**: Additional network-level protection
- **NAT Gateway**: Secure outbound internet access for private subnets

### 2. Data Encryption

- **RDS Encryption**: Database encrypted at rest
- **S3 Encryption**: All buckets encrypted with KMS
- **EFS Encryption**: File system encrypted in transit and at rest
- **SSL/TLS**: HTTPS enforced for all web traffic

### 3. Access Control

- **IAM Roles**: Least privilege access for all services
- **Instance Profiles**: No hardcoded credentials
- **Systems Manager**: Secure parameter storage
- **Jenkins Security**: Role-based access control

### 4. Security Monitoring

```bash
# Enable AWS Config for compliance monitoring
aws configservice put-configuration-recorder \
    --configuration-recorder name=default,roleARN=arn:aws:iam::account:role/config-role

# Enable CloudTrail for audit logging
aws cloudtrail create-trail \
    --name petclinic-audit-trail \
    --s3-bucket-name petclinic-audit-logs
```

## Backup and Disaster Recovery

### 1. Automated Backups

#### Database Backups
- **RDS Automated Backups**: 7-day retention
- **RDS Snapshots**: Weekly manual snapshots
- **Cross-region replication**: For disaster recovery

#### Application Backups
- **Jenkins Configuration**: Daily backups to S3
- **Application Artifacts**: Versioned storage in S3
- **EFS Snapshots**: Daily file system backups

### 2. Backup Verification

```bash
# Test database backup restoration
./jenkins-config/backup-test.sh

# Verify S3 backup integrity
aws s3api head-object \
    --bucket petclinic-production-backups \
    --key jenkins/backup-$(date +%Y%m%d).tar.gz
```

### 3. Disaster Recovery Procedures

#### RTO/RPO Targets
- **Recovery Time Objective (RTO)**: 4 hours
- **Recovery Point Objective (RPO)**: 1 hour

#### Recovery Steps
1. **Assess Impact**: Determine scope of outage
2. **Activate DR Plan**: Notify stakeholders
3. **Restore Infrastructure**: Deploy to alternate region
4. **Restore Data**: From latest backups
5. **Validate System**: Run health checks
6. **Switch Traffic**: Update DNS/load balancer
7. **Monitor**: Ensure stable operation

### 4. Backup Testing Schedule

- **Weekly**: Database backup restoration test
- **Monthly**: Full disaster recovery drill
- **Quarterly**: Cross-region failover test

## Troubleshooting

### Common Issues and Solutions

#### 1. Infrastructure Deployment Failures

```bash
# Check CloudFormation events
aws cloudformation describe-stack-events \
    --stack-name petclinic-production \
    --query 'StackEvents[?ResourceStatus==`CREATE_FAILED`]'

# Common solutions:
# - Check service limits
# - Verify IAM permissions
# - Ensure unique resource names
# - Check parameter values
```

#### 2. Application Deployment Issues

```bash
# Check application logs
sudo tail -f /var/log/pet-clinic/backend.log
sudo tail -f /var/log/pet-clinic/frontend.log

# Check service status
sudo systemctl status pet-clinic-backend
sudo systemctl status pet-clinic-frontend

# Common solutions:
# - Verify database connectivity
# - Check environment variables
# - Ensure proper file permissions
# - Validate configuration files
```

#### 3. Database Connection Problems

```bash
# Test database connectivity
mysql -h $DB_HOST -u $DB_USER -p$DB_PASSWORD -e "SELECT 1"

# Check RDS status
aws rds describe-db-instances \
    --db-instance-identifier petclinic-production-db

# Common solutions:
# - Verify security group rules
# - Check database credentials
# - Ensure database is in available state
# - Validate connection string
```

#### 4. Jenkins Issues

```bash
# Check Jenkins service
sudo systemctl status jenkins

# View Jenkins logs
sudo journalctl -u jenkins -f

# Check disk space
df -h /var/lib/jenkins

# Common solutions:
# - Restart Jenkins service
# - Clear old build artifacts
# - Check plugin compatibility
# - Verify Java version
```

### Monitoring and Debugging Tools

```bash
# System monitoring
htop                    # Process monitoring
iotop                   # I/O monitoring
netstat -tulpn         # Network connections
ss -tulpn              # Socket statistics

# Application monitoring
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:9090/actuator/metrics

# AWS CLI debugging
aws logs tail /aws/petclinic/application --follow
aws cloudwatch get-metric-statistics --namespace AWS/EC2 --metric-name CPUUtilization
```

## Cost Optimization

### 1. Right-Sizing Resources

#### Current Resource Allocation
- **Jenkins Server**: t3.medium (2 vCPU, 4 GB RAM)
- **Application Servers**: t3.small (1 vCPU, 2 GB RAM)
- **Database**: db.t3.micro (1 vCPU, 1 GB RAM)

#### Optimization Recommendations

```bash
# Monitor resource utilization
aws cloudwatch get-metric-statistics \
    --namespace AWS/EC2 \
    --metric-name CPUUtilization \
    --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 3600 \
    --statistics Average

# Consider downsizing if average CPU < 30%
# Consider upsizing if average CPU > 70%
```

### 2. Storage Optimization

```bash
# S3 lifecycle policies
aws s3api put-bucket-lifecycle-configuration \
    --bucket petclinic-production-backups \
    --lifecycle-configuration file://s3-lifecycle.json

# EBS volume optimization
aws ec2 describe-volumes \
    --filters Name=attachment.instance-id,Values=i-1234567890abcdef0 \
    --query 'Volumes[0].{Size:Size,VolumeType:VolumeType,Iops:Iops}'
```

### 3. Reserved Instances

For production workloads, consider:
- **1-year Reserved Instances** for predictable workloads
- **Savings Plans** for flexible compute usage
- **Spot Instances** for non-critical batch processing

### 4. Cost Monitoring

```bash
# Set up billing alerts
aws budgets create-budget \
    --account-id $(aws sts get-caller-identity --query Account --output text) \
    --budget file://budget-config.json

# Monitor costs by service
aws ce get-cost-and-usage \
    --time-period Start=2024-01-01,End=2024-01-31 \
    --granularity MONTHLY \
    --metrics BlendedCost \
    --group-by Type=DIMENSION,Key=SERVICE
```

### Estimated Monthly Costs (us-east-1)

| Service | Configuration | Estimated Cost |
|---------|---------------|----------------|
| EC2 Instances | 1x t3.medium + 4x t3.small | $85-120 |
| RDS MySQL | db.t3.micro Multi-AZ | $25-35 |
| Application Load Balancer | 1 ALB | $20-25 |
| S3 Storage | 100GB with lifecycle | $5-10 |
| Data Transfer | Moderate usage | $10-20 |
| CloudWatch | Standard monitoring | $5-15 |
| **Total** | | **$150-225/month** |

## Maintenance

### 1. Regular Maintenance Tasks

#### Weekly Tasks
- Review CloudWatch alarms and metrics
- Check backup completion status
- Update security patches on EC2 instances
- Review Jenkins build success rates

#### Monthly Tasks
- Rotate access keys and passwords
- Review and optimize costs
- Update application dependencies
- Test disaster recovery procedures

#### Quarterly Tasks
- Security audit and penetration testing
- Performance optimization review
- Capacity planning assessment
- Documentation updates

### 2. Automated Maintenance

```bash
# Set up automated patching
aws ssm create-maintenance-window \
    --name "petclinic-maintenance-window" \
    --schedule "cron(0 2 ? * SUN *)" \
    --duration 4 \
    --cutoff 1

# Configure automatic backups
aws rds modify-db-instance \
    --db-instance-identifier petclinic-production-db \
    --backup-retention-period 7 \
    --preferred-backup-window "03:00-04:00"
```

### 3. Monitoring Maintenance

```bash
# Check system health
./scripts/health-check.sh

# Review logs for errors
sudo grep -i error /var/log/pet-clinic/*.log | tail -20

# Monitor disk usage
df -h | grep -E "(80%|90%|100%)"

# Check service status
systemctl status pet-clinic-backend pet-clinic-frontend jenkins
```

### 4. Update Procedures

#### Application Updates
1. **Test in staging environment**
2. **Create database backup**
3. **Deploy during maintenance window**
4. **Verify health checks**
5. **Monitor for issues**
6. **Rollback if necessary**

#### Infrastructure Updates
1. **Update CloudFormation templates**
2. **Validate templates**
3. **Deploy to staging first**
4. **Schedule production update**
5. **Monitor deployment**
6. **Verify all services**

---

## Support and Resources

### Documentation Links
- [AWS CloudFormation Documentation](https://docs.aws.amazon.com/cloudformation/)
- [Amazon RDS User Guide](https://docs.aws.amazon.com/rds/)
- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

### Emergency Contacts
- **Infrastructure Issues**: AWS Support
- **Application Issues**: Development Team
- **Security Issues**: Security Team
- **Database Issues**: DBA Team

### Useful Commands Reference

```bash
# Quick status check
aws cloudformation describe-stacks --stack-name petclinic-production --query 'Stacks[0].StackStatus'

# Application health check
curl -f http://$(aws cloudformation describe-stacks --stack-name petclinic-production --query 'Stacks[0].Outputs[?OutputKey==`ApplicationURL`].OutputValue' --output text)/actuator/health

# Database connection test
mysql -h $(aws cloudformation describe-stacks --stack-name petclinic-production --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' --output text) -u petclinicadmin -p

# Jenkins status
curl -u admin:$JENKINS_ADMIN_PASSWORD $(aws cloudformation describe-stacks --stack-name petclinic-production --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' --output text)/api/json
```

This comprehensive guide provides everything needed to successfully deploy and maintain the Pet Clinic application on AWS Cloud infrastructure.