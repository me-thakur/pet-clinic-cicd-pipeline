# Pet Clinic CI/CD Pipeline - Troubleshooting Guide

This guide provides solutions to common issues encountered when deploying, configuring, and operating the Pet Clinic CI/CD Pipeline system.

## Table of Contents

1. [Infrastructure Issues](#infrastructure-issues)
2. [Application Issues](#application-issues)
3. [Jenkins Issues](#jenkins-issues)
4. [Database Issues](#database-issues)
5. [Network Issues](#network-issues)
6. [Security Issues](#security-issues)
7. [Performance Issues](#performance-issues)
8. [Monitoring Issues](#monitoring-issues)
9. [Backup and Recovery Issues](#backup-and-recovery-issues)
10. [General Debugging](#general-debugging)

## Infrastructure Issues

### CloudFormation Stack Creation Failed

#### Symptoms
- Stack creation fails with `CREATE_FAILED` status
- Resources show error messages in stack events
- Nested stacks fail to create

#### Common Causes and Solutions

**1. IAM Permissions Issues**
```bash
# Check IAM permissions
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:user/deployer \
  --action-names cloudformation:CreateStack \
  --resource-arns "*"

# Solution: Add missing permissions
aws iam attach-user-policy \
  --user-name deployer \
  --policy-arn arn:aws:iam::aws:policy/PowerUserAccess
```

**2. Resource Limits Exceeded**
```bash
# Check VPC limits
aws ec2 describe-account-attributes \
  --attribute-names max-instances

# Check EIP limits
aws ec2 describe-addresses

# Solution: Request limit increase through AWS Support
aws support create-case \
  --subject "Increase VPC limit" \
  --service-code "amazon-vpc" \
  --category-code "service-limit-increase"
```

**3. S3 Bucket Name Conflicts**
```bash
# Error: Bucket name already exists
# Solution: Use unique bucket names with random suffix
RANDOM_SUFFIX=$(openssl rand -hex 4)
sed -i "s/petclinic-backup-bucket/petclinic-backup-bucket-$RANDOM_SUFFIX/g" cloudformation/storage.yaml
```

**4. Availability Zone Issues**
```bash
# Check available AZs
aws ec2 describe-availability-zones \
  --query 'AvailabilityZones[?State==`available`].ZoneName'

# Solution: Update template with available AZs
# Modify cloudformation/network.yaml to use available zones
```

#### Debugging Steps
```bash
# 1. Check stack events
aws cloudformation describe-stack-events \
  --stack-name petclinic-production \
  --query 'StackEvents[?ResourceStatus==`CREATE_FAILED`]' \
  --output table

# 2. Check nested stack failures
aws cloudformation list-stacks \
  --stack-status-filter CREATE_FAILED \
  --query 'StackSummaries[?contains(StackName, `petclinic`)].{Name:StackName,Status:StackStatus,Reason:StackStatusReason}'

# 3. Validate templates
aws cloudformation validate-template \
  --template-body file://cloudformation/master-stack.yaml

# 4. Check resource dependencies
aws cloudformation describe-stack-resources \
  --stack-name petclinic-production \
  --query 'StackResources[?ResourceStatus==`CREATE_FAILED`]'
```

### EC2 Instance Launch Failures

#### Symptoms
- Auto Scaling Group shows instances in `Pending` state
- Instances fail to pass health checks
- User data scripts fail to execute

#### Solutions

**1. Instance Launch Issues**
```bash
# Check instance status
aws ec2 describe-instances \
  --filters "Name=tag:Project,Values=petclinic" \
  --query 'Reservations[].Instances[].{ID:InstanceId,State:State.Name,Status:StatusChecks}'

# Check system logs
aws ec2 get-console-output --instance-id i-1234567890abcdef0

# Solution: Fix user data script or AMI issues
```

**2. Security Group Issues**
```bash
# Test connectivity
aws ec2 describe-security-groups \
  --group-ids sg-1234567890abcdef0 \
  --query 'SecurityGroups[].IpPermissions'

# Solution: Update security group rules
aws ec2 authorize-security-group-ingress \
  --group-id sg-1234567890abcdef0 \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0
```

**3. EFS Mount Issues**
```bash
# Check EFS mount targets
aws efs describe-mount-targets \
  --file-system-id fs-1234567890abcdef0

# Test EFS connectivity from instance
sudo mount -t efs fs-1234567890abcdef0:/ /mnt/efs

# Solution: Fix security groups or mount helper
sudo yum install -y amazon-efs-utils
```

## Application Issues

### Application Won't Start

#### Symptoms
- Spring Boot application fails to start
- Port binding errors
- Database connection failures

#### Solutions

**1. Port Conflicts**
```bash
# Check what's using the port
sudo netstat -tlnp | grep :8080
sudo lsof -i :8080

# Solution: Kill conflicting process or change port
sudo kill -9 $(sudo lsof -t -i:8080)
```

**2. Java Version Issues**
```bash
# Check Java version
java -version
javac -version

# Solution: Install correct Java version
sudo yum install -y java-11-openjdk-devel
sudo alternatives --config java
```

**3. Memory Issues**
```bash
# Check available memory
free -h
cat /proc/meminfo

# Solution: Increase JVM heap size
export JAVA_OPTS="-Xmx1024m -Xms512m"
```

**4. Configuration Issues**
```bash
# Check application properties
cat /opt/petclinic/application.yml

# Check environment variables
env | grep SPRING

# Solution: Fix configuration
sudo systemctl edit petclinic-backend
# Add:
# [Service]
# Environment="SPRING_PROFILES_ACTIVE=production"
# Environment="DB_HOST=your-db-endpoint"
```

### Database Connection Issues

#### Symptoms
- Application logs show database connection errors
- Connection timeouts
- Authentication failures

#### Solutions

**1. Network Connectivity**
```bash
# Test database connectivity
telnet your-db-endpoint 3306
nc -zv your-db-endpoint 3306

# Check security groups
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=*database*" \
  --query 'SecurityGroups[].IpPermissions'
```

**2. Authentication Issues**
```bash
# Test database login
mysql -h your-db-endpoint -u petclinic -p

# Check user permissions
mysql -h your-db-endpoint -u root -p -e "SELECT User, Host FROM mysql.user WHERE User='petclinic';"

# Solution: Reset password or recreate user
mysql -h your-db-endpoint -u root -p << 'EOF'
DROP USER IF EXISTS 'petclinic'@'%';
CREATE USER 'petclinic'@'%' IDENTIFIED BY 'new-password';
GRANT ALL PRIVILEGES ON petclinic.* TO 'petclinic'@'%';
FLUSH PRIVILEGES;
EOF
```

**3. Connection Pool Issues**
```bash
# Check connection pool configuration
grep -r "datasource" /opt/petclinic/application.yml

# Solution: Adjust connection pool settings
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

## Jenkins Issues

### Jenkins Won't Start

#### Symptoms
- Jenkins service fails to start
- Port 8080 not accessible
- Jenkins logs show errors

#### Solutions

**1. Service Issues**
```bash
# Check Jenkins service status
sudo systemctl status jenkins

# Check Jenkins logs
sudo journalctl -u jenkins -f

# Restart Jenkins
sudo systemctl restart jenkins
```

**2. Java Issues**
```bash
# Check Java installation
which java
java -version

# Check Jenkins Java configuration
sudo cat /etc/sysconfig/jenkins | grep JAVA

# Solution: Fix Java path
sudo sed -i 's|JAVA_HOME=.*|JAVA_HOME=/usr/lib/jvm/java-11-openjdk|' /etc/sysconfig/jenkins
```

**3. Permission Issues**
```bash
# Check Jenkins home directory permissions
ls -la /var/lib/jenkins/

# Fix permissions
sudo chown -R jenkins:jenkins /var/lib/jenkins/
sudo chmod -R 755 /var/lib/jenkins/
```

**4. Port Conflicts**
```bash
# Check if port 8080 is in use
sudo netstat -tlnp | grep :8080

# Change Jenkins port
sudo sed -i 's/JENKINS_PORT="8080"/JENKINS_PORT="8082"/' /etc/sysconfig/jenkins
sudo systemctl restart jenkins
```

### Jenkins Build Failures

#### Symptoms
- Builds fail with compilation errors
- Git checkout failures
- Plugin issues

#### Solutions

**1. Git Issues**
```bash
# Check Git configuration
git config --list

# Test Git connectivity
git ls-remote https://github.com/your-org/pet-clinic-cicd-pipeline.git

# Solution: Fix Git credentials or SSH keys
ssh-keygen -t rsa -b 4096 -C "jenkins@yourserver.com"
# Add public key to GitHub
```

**2. Maven Issues**
```bash
# Check Maven installation
mvn -version

# Check Maven settings
cat ~/.m2/settings.xml

# Solution: Install Maven or fix configuration
sudo yum install -y maven
```

**3. Plugin Issues**
```bash
# Check installed plugins
curl -u admin:password http://jenkins-url/pluginManager/api/json?depth=1

# Update plugins
curl -X POST -u admin:password http://jenkins-url/updateCenter/safeRestart
```

### Pipeline Configuration Issues

#### Symptoms
- Jenkinsfile syntax errors
- Pipeline steps fail
- Environment variables not set

#### Solutions

**1. Jenkinsfile Syntax**
```groovy
// Validate Jenkinsfile syntax
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
    }
}
```

**2. Environment Variables**
```bash
# Check environment in Jenkins
echo $PATH
echo $JAVA_HOME
echo $MAVEN_HOME

# Solution: Set in Jenkins global configuration or Jenkinsfile
environment {
    JAVA_HOME = '/usr/lib/jvm/java-11-openjdk'
    MAVEN_HOME = '/usr/share/maven'
    PATH = "${MAVEN_HOME}/bin:${PATH}"
}
```

## Database Issues

### MySQL Privilege Configuration Issues

#### Symptoms
- ERROR 1419: You do not have the SUPER privilege and binary logging is enabled
- Function creation failures in different environments
- Privilege validation errors

#### Solutions

**1. Environment Detection Issues**
```bash
# Check current environment detection
mysql-privilege-config detect-environment

# Manually set environment if detection fails
export MYSQL_PRIVILEGE_ENVIRONMENT=production
mysql-privilege-config configure --environment production
```

**2. Function Creation Errors (ERROR 1419)**
```bash
# Check current log_bin_trust_function_creators setting
mysql -h $DB_HOST -u $DB_USER -p -e "SELECT @@log_bin_trust_function_creators;"

# For development/CI environments (enable)
mysql-privilege-config apply-security --environment development

# For production (use DEFINER with proper privileges)
mysql-privilege-config apply-security --environment production --strict
```

**3. Privilege Validation Failures**
```bash
# Run comprehensive privilege validation
mysql-privilege-config validate --environment production --verbose

# Check specific user privileges
mysql -h $DB_HOST -u root -p -e "SHOW GRANTS FOR 'petclinic'@'%';"

# Fix privilege issues
mysql-privilege-config fix-privileges --user petclinic --environment production
```

**4. Configuration Template Issues**
```bash
# Validate configuration templates
mysql-privilege-config validate-config --template local-development
mysql-privilege-config validate-config --template ci
mysql-privilege-config validate-config --template production

# Apply correct template
mysql-privilege-config apply-template --template production --host $DB_HOST
```

#### Debugging MySQL Privilege Configuration
```bash
# Enable debug logging
export MYSQL_PRIVILEGE_LOG_LEVEL=DEBUG
mysql-privilege-config --debug validate

# Check configuration status
mysql-privilege-config status --environment production

# Test function creation capability
mysql-privilege-config test-functions --environment production
```

### RDS Connection Problems

#### Symptoms
- Cannot connect to RDS instance
- Connection timeouts
- SSL/TLS errors

#### Solutions

**1. Security Group Configuration**
```bash
# Check RDS security groups
aws rds describe-db-instances \
  --db-instance-identifier petclinic-db \
  --query 'DBInstances[0].VpcSecurityGroups'

# Update security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-database \
  --protocol tcp \
  --port 3306 \
  --source-group sg-application
```

**2. Parameter Group Issues**
```bash
# Check parameter group
aws rds describe-db-parameters \
  --db-parameter-group-name petclinic-params

# Modify parameters if needed
aws rds modify-db-parameter-group \
  --db-parameter-group-name petclinic-params \
  --parameters ParameterName=max_connections,ParameterValue=200,ApplyMethod=immediate
```

**3. SSL Configuration**
```bash
# Download RDS CA certificate
wget https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem

# Update connection string
jdbc:mysql://endpoint:3306/petclinic?useSSL=true&serverSslCert=rds-ca-2019-root.pem
```

### Database Performance Issues

#### Symptoms
- Slow query performance
- High CPU utilization
- Connection pool exhaustion

#### Solutions

**1. Query Optimization**
```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- Check slow queries
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

-- Add indexes
CREATE INDEX idx_owners_lastname ON owners(last_name);
CREATE INDEX idx_pets_owner_id ON pets(owner_id);
```

**2. Connection Pool Tuning**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

**3. RDS Performance Insights**
```bash
# Enable Performance Insights
aws rds modify-db-instance \
  --db-instance-identifier petclinic-db \
  --enable-performance-insights \
  --performance-insights-retention-period 7
```

## Network Issues

### Load Balancer Issues

#### Symptoms
- 502/503 errors from load balancer
- Health check failures
- Uneven traffic distribution

#### Solutions

**1. Health Check Configuration**
```bash
# Check target group health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/petclinic-frontend/1234567890123456

# Update health check settings
aws elbv2 modify-target-group \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/petclinic-frontend/1234567890123456 \
  --health-check-path /health \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 5
```

**2. Security Group Issues**
```bash
# Check ALB security group
aws ec2 describe-security-groups \
  --group-ids sg-alb \
  --query 'SecurityGroups[].IpPermissions'

# Allow traffic from ALB to targets
aws ec2 authorize-security-group-ingress \
  --group-id sg-application \
  --protocol tcp \
  --port 8080 \
  --source-group sg-alb
```

### VPC Connectivity Issues

#### Symptoms
- Cannot reach internet from private subnets
- Inter-subnet communication failures
- DNS resolution issues

#### Solutions

**1. NAT Gateway Issues**
```bash
# Check NAT Gateway status
aws ec2 describe-nat-gateways \
  --filter "Name=vpc-id,Values=vpc-12345678"

# Check route tables
aws ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=vpc-12345678" \
  --query 'RouteTables[].{ID:RouteTableId,Routes:Routes}'

# Fix routing
aws ec2 create-route \
  --route-table-id rtb-12345678 \
  --destination-cidr-block 0.0.0.0/0 \
  --nat-gateway-id nat-12345678
```

**2. DNS Issues**
```bash
# Check VPC DNS settings
aws ec2 describe-vpc-attribute \
  --vpc-id vpc-12345678 \
  --attribute enableDnsHostnames

# Enable DNS resolution
aws ec2 modify-vpc-attribute \
  --vpc-id vpc-12345678 \
  --enable-dns-hostnames
```

## Security Issues

### SSL/TLS Certificate Issues

#### Symptoms
- HTTPS not working
- Certificate validation errors
- Mixed content warnings

#### Solutions

**1. ACM Certificate Issues**
```bash
# Check certificate status
aws acm list-certificates \
  --query 'CertificateSummaryList[?DomainName==`yourdomain.com`]'

# Request new certificate
aws acm request-certificate \
  --domain-name yourdomain.com \
  --subject-alternative-names "*.yourdomain.com" \
  --validation-method DNS
```

**2. Load Balancer HTTPS Configuration**
```bash
# Add HTTPS listener
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/petclinic-alb/1234567890123456 \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012 \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/petclinic-frontend/1234567890123456
```

### IAM Permission Issues

#### Symptoms
- Access denied errors
- Services cannot assume roles
- Cross-service communication failures

#### Solutions

**1. Role Trust Relationships**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

**2. Policy Attachments**
```bash
# Check attached policies
aws iam list-attached-role-policies --role-name petclinic-ec2-role

# Attach missing policy
aws iam attach-role-policy \
  --role-name petclinic-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

## Performance Issues

### High CPU Utilization

#### Symptoms
- EC2 instances showing high CPU
- Application response times slow
- Auto Scaling triggering frequently

#### Solutions

**1. Application Profiling**
```bash
# Check Java process CPU usage
top -p $(pgrep java)

# Generate thread dump
jstack $(pgrep java) > thread-dump.txt

# Analyze heap usage
jmap -histo $(pgrep java)
```

**2. JVM Tuning**
```bash
# Optimize JVM settings
export JAVA_OPTS="-Xmx2048m -Xms1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Enable JVM monitoring
export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
```

**3. Auto Scaling Optimization**
```bash
# Update scaling policies
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name petclinic-backend-asg \
  --policy-name scale-out \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "TargetValue": 60.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ASGAverageCPUUtilization"
    }
  }'
```

### Memory Issues

#### Symptoms
- Out of memory errors
- Frequent garbage collection
- Application crashes

#### Solutions

**1. Memory Analysis**
```bash
# Check memory usage
free -h
cat /proc/meminfo

# Generate heap dump
jmap -dump:format=b,file=heap-dump.hprof $(pgrep java)

# Analyze with Eclipse MAT or similar tool
```

**2. Memory Optimization**
```bash
# Increase heap size
export JAVA_OPTS="-Xmx4096m -Xms2048m"

# Optimize garbage collection
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:G1HeapRegionSize=16m"
```

## Monitoring Issues

### CloudWatch Metrics Missing

#### Symptoms
- No metrics appearing in CloudWatch
- Alarms not triggering
- Dashboard showing no data

#### Solutions

**1. CloudWatch Agent Issues**
```bash
# Check agent status
sudo systemctl status amazon-cloudwatch-agent

# Check agent configuration
sudo cat /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# Restart agent
sudo systemctl restart amazon-cloudwatch-agent
```

**2. IAM Permissions**
```bash
# Check CloudWatch permissions
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:role/CloudWatchAgentServerRole \
  --action-names cloudwatch:PutMetricData \
  --resource-arns "*"
```

### Log Aggregation Issues

#### Symptoms
- Logs not appearing in CloudWatch Logs
- Log rotation issues
- Disk space problems

#### Solutions

**1. CloudWatch Logs Agent**
```bash
# Check logs agent
sudo systemctl status awslogsd

# Check configuration
sudo cat /etc/awslogs/awslogs.conf

# Test log shipping
sudo tail -f /var/log/awslogs.log
```

**2. Log Rotation**
```bash
# Configure logrotate
sudo cat > /etc/logrotate.d/petclinic << 'EOF'
/opt/petclinic/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 644 petclinic petclinic
}
EOF
```

## Backup and Recovery Issues

### Backup Failures

#### Symptoms
- Jenkins backups not created
- S3 sync failures
- RDS snapshots failing

#### Solutions

**1. Jenkins Backup Issues**
```bash
# Check ThinBackup configuration
sudo cat /var/lib/jenkins/thinBackup.xml

# Manual backup test
sudo -u jenkins java -jar /var/lib/jenkins/plugins/thinBackup/WEB-INF/lib/thinBackup.jar

# Check backup logs
sudo tail -f /var/lib/jenkins/logs/thinBackup.log
```

**2. S3 Sync Issues**
```bash
# Test S3 connectivity
aws s3 ls s3://your-backup-bucket/

# Check IAM permissions
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:role/jenkins-role \
  --action-names s3:PutObject,s3:GetObject \
  --resource-arns "arn:aws:s3:::your-backup-bucket/*"

# Manual sync test
aws s3 sync /var/lib/jenkins/backup/ s3://your-backup-bucket/jenkins/
```

### Recovery Issues

#### Symptoms
- Restore procedures fail
- Data corruption
- Service unavailability during recovery

#### Solutions

**1. Database Recovery**
```bash
# Test restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier petclinic-db-restore-test \
  --db-snapshot-identifier petclinic-db-snapshot-20231201

# Verify data integrity
mysql -h restored-endpoint -u petclinic -p -e "SELECT COUNT(*) FROM owners;"
```

**2. Jenkins Recovery**
```bash
# Test Jenkins restore
sudo systemctl stop jenkins
sudo mv /var/lib/jenkins /var/lib/jenkins.backup
sudo mkdir /var/lib/jenkins
sudo chown jenkins:jenkins /var/lib/jenkins

# Restore from backup
aws s3 sync s3://your-backup-bucket/jenkins/latest/ /var/lib/jenkins/
sudo chown -R jenkins:jenkins /var/lib/jenkins/
sudo systemctl start jenkins
```

## General Debugging

### Systematic Debugging Approach

#### 1. Gather Information
```bash
# System information
uname -a
cat /etc/os-release
df -h
free -h
uptime

# Service status
sudo systemctl status jenkins
sudo systemctl status petclinic-backend
sudo systemctl status petclinic-frontend

# Network connectivity
curl -I http://localhost:8080
curl -I http://localhost:8081
telnet db-endpoint 3306
```

#### 2. Check Logs
```bash
# System logs
sudo journalctl -f

# Application logs
sudo tail -f /opt/petclinic/logs/backend.log
sudo tail -f /opt/petclinic/logs/frontend.log

# Jenkins logs
sudo tail -f /var/log/jenkins/jenkins.log

# Database logs (if accessible)
sudo tail -f /var/log/mysql/error.log
```

#### 3. Test Components Individually
```bash
# Test database connectivity
mysql -h db-endpoint -u petclinic -p -e "SELECT 1"

# Test application endpoints
curl http://localhost:8081/api/health
curl http://localhost:8080/health

# Test Jenkins
curl http://localhost:8080/jenkins/login
```

### Common Debugging Commands

```bash
# Process monitoring
ps aux | grep java
ps aux | grep jenkins

# Network monitoring
netstat -tlnp
ss -tlnp

# Disk usage
du -sh /var/lib/jenkins/
du -sh /opt/petclinic/

# Memory usage
cat /proc/meminfo
free -h

# CPU usage
top
htop

# File permissions
ls -la /var/lib/jenkins/
ls -la /opt/petclinic/

# Environment variables
env | grep JAVA
env | grep SPRING
```

### Getting Help

When troubleshooting issues:

1. **Check this guide first** for common solutions
2. **Gather relevant logs** and error messages
3. **Document the exact steps** that led to the issue
4. **Test in isolation** to identify the root cause
5. **Check AWS service health** at https://status.aws.amazon.com/
6. **Consult AWS documentation** for service-specific issues
7. **Open GitHub issues** for application-specific problems
8. **Contact AWS Support** for infrastructure issues

### Emergency Procedures

#### System Recovery
```bash
# Stop all services
sudo systemctl stop jenkins
sudo systemctl stop petclinic-backend
sudo systemctl stop petclinic-frontend

# Check system resources
df -h
free -h

# Restart services one by one
sudo systemctl start petclinic-backend
sudo systemctl start petclinic-frontend
sudo systemctl start jenkins

# Verify functionality
./scripts/health-check.sh
```

#### Rollback Procedures
```bash
# Application rollback
./deployment-scripts/rollback.sh

# Infrastructure rollback
aws cloudformation cancel-update-stack --stack-name petclinic-production

# Database rollback (if needed)
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier petclinic-db \
  --db-snapshot-identifier petclinic-db-snapshot-before-change
```

This troubleshooting guide covers the most common issues you may encounter. For issues not covered here, follow the systematic debugging approach and consult the relevant AWS documentation or open a support case.