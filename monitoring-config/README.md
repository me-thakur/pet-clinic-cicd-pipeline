# Monitoring and Alerting Configuration for Pet Clinic CI/CD Pipeline

This directory contains comprehensive monitoring and alerting configuration for the Pet Clinic CI/CD Pipeline system.

## Overview

The monitoring system provides:

- **CloudWatch Alarms**: Automated alerting for infrastructure and application metrics
- **CloudWatch Dashboard**: Centralized visualization of system health and performance
- **SNS Notifications**: Multi-channel alert delivery (email, Slack, PagerDuty)
- **Custom Metrics**: Application-specific metrics collection and publishing
- **Log-based Monitoring**: Automated monitoring of log patterns and events

## Components

### 1. CloudWatch Alarms (`cloudwatch-alarms.yaml`)

Comprehensive alarm configuration covering:
- **EC2 Instance Monitoring**: CPU, memory, disk space, status checks
- **RDS Database Monitoring**: CPU, connections, storage, latency
- **Application Monitoring**: Error rates, response times, service status
- **Jenkins Monitoring**: Service status, build failures, queue length
- **Security Monitoring**: Failed logins, security events
- **Backup Monitoring**: Backup failures, backup age

### 2. CloudWatch Dashboard (`cloudwatch-dashboard.yaml`)

Interactive dashboard with:
- Real-time system metrics visualization
- Application performance monitoring
- Database health indicators
- Security event tracking
- Recent error log analysis
- Key performance indicators (KPIs)

### 3. SNS Notifications (`sns-notifications.yaml`)

Multi-channel notification system:
- **Email Notifications**: Direct email alerts to administrators
- **Slack Integration**: Real-time notifications to Slack channels
- **PagerDuty Integration**: Critical incident management
- **Lambda Functions**: Custom notification processing

### 4. Custom Metrics Publisher (`custom-metrics-publisher.py`)

Python service that collects and publishes:
- Application health and performance metrics
- Jenkins CI/CD pipeline metrics
- Security event metrics
- Backup operation metrics
- Custom business logic metrics

## Installation

### Automatic Setup

Run the setup script with appropriate parameters:

```bash
export ENVIRONMENT="production"
export AWS_REGION="us-east-1"
export ALERT_EMAIL="admin@petclinic.local"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
export PAGERDUTY_INTEGRATION_KEY="your-pagerduty-integration-key"

sudo ./setup-monitoring.sh
```

### Manual Setup

#### 1. Deploy SNS Notifications

```bash
aws cloudformation create-stack \
    --stack-name production-petclinic-sns-notifications \
    --template-body file://sns-notifications.yaml \
    --parameters \
        ParameterKey=Environment,ParameterValue=production \
        ParameterKey=AlertEmail,ParameterValue=admin@petclinic.local \
    --capabilities CAPABILITY_IAM \
    --region us-east-1
```

#### 2. Deploy CloudWatch Alarms

```bash
aws cloudformation create-stack \
    --stack-name production-petclinic-cloudwatch-alarms \
    --template-body file://cloudwatch-alarms.yaml \
    --parameters \
        ParameterKey=Environment,ParameterValue=production \
        ParameterKey=SNSTopicArn,ParameterValue=arn:aws:sns:us-east-1:123456789012:production-PetClinic-CriticalAlerts \
        ParameterKey=InstanceId,ParameterValue=i-1234567890abcdef0 \
        ParameterKey=RDSInstanceId,ParameterValue=production-petclinic-database \
    --region us-east-1
```

#### 3. Deploy CloudWatch Dashboard

```bash
aws cloudformation create-stack \
    --stack-name production-petclinic-cloudwatch-dashboard \
    --template-body file://cloudwatch-dashboard.yaml \
    --parameters \
        ParameterKey=Environment,ParameterValue=production \
        ParameterKey=InstanceId,ParameterValue=i-1234567890abcdef0 \
        ParameterKey=RDSInstanceId,ParameterValue=production-petclinic-database \
    --region us-east-1
```

#### 4. Install Custom Metrics Publisher

```bash
# Install dependencies
sudo pip3 install boto3 requests

# Copy script
sudo mkdir -p /opt/petclinic-monitoring
sudo cp custom-metrics-publisher.py /opt/petclinic-monitoring/
sudo chmod +x /opt/petclinic-monitoring/custom-metrics-publisher.py

# Create systemd service
sudo tee /etc/systemd/system/petclinic-metrics.service > /dev/null << 'EOF'
[Unit]
Description=Pet Clinic Custom Metrics Publisher
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/bin/python3 /opt/petclinic-monitoring/custom-metrics-publisher.py --daemon --environment production
Restart=always
RestartSec=60

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable petclinic-metrics.service
sudo systemctl start petclinic-metrics.service
```

## Configuration

### Environment Variables

Set the following environment variables for optimal monitoring:

```bash
# AWS Configuration
export AWS_REGION="us-east-1"
export AWS_DEFAULT_REGION="us-east-1"

# Application Configuration
export ENVIRONMENT="production"
export JENKINS_URL="http://localhost:8080"
export APP_URL="http://localhost:8080"

# Notification Configuration
export ALERT_EMAIL="admin@petclinic.local"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
export PAGERDUTY_INTEGRATION_KEY="your-pagerduty-integration-key"

# Custom Metrics Configuration
export JENKINS_USER="admin"
export JENKINS_API_TOKEN="your-jenkins-api-token"
```

### Alarm Thresholds

Default alarm thresholds can be customized by modifying the CloudFormation templates:

- **CPU Utilization**: 80% (2 consecutive periods)
- **Memory Utilization**: 85% (2 consecutive periods)
- **Disk Space**: 90% (1 period)
- **Database CPU**: 75% (2 consecutive periods)
- **Application Error Rate**: 10 errors (2 consecutive periods)
- **Response Time**: 2000ms (2 consecutive periods)

### Notification Channels

#### Email Notifications
- Configured automatically during SNS stack deployment
- Requires email confirmation after deployment

#### Slack Integration
- Requires Slack webhook URL
- Supports rich message formatting with alarm details
- Color-coded messages based on alarm state

#### PagerDuty Integration
- Requires PagerDuty integration key
- Automatic incident creation and resolution
- Supports incident deduplication

## Monitoring Metrics

### Infrastructure Metrics

#### EC2 Instance
- `CPUUtilization`: CPU usage percentage
- `mem_used_percent`: Memory usage percentage
- `disk_used_percent`: Disk space usage percentage
- `StatusCheckFailed_Instance`: Instance status check failures
- `StatusCheckFailed_System`: System status check failures

#### RDS Database
- `CPUUtilization`: Database CPU usage
- `DatabaseConnections`: Active database connections
- `FreeStorageSpace`: Available storage space
- `ReadLatency`: Database read latency
- `WriteLatency`: Database write latency

### Application Metrics

#### Pet Clinic Application
- `ServiceStatus`: Application service availability
- `ErrorCount`: Number of application errors
- `ResponseTime`: Average response time
- `RequestCount`: Total request count
- `HealthCheckStatus`: Application health check status

#### Jenkins CI/CD
- `ServiceStatus`: Jenkins service availability
- `BuildFailureRate`: Percentage of failed builds
- `QueueLength`: Number of jobs in queue
- `JobsSuccess`: Number of successful jobs
- `JobsFailed`: Number of failed jobs

### Security Metrics
- `SecurityEvents`: Number of security events
- `FailedLoginAttempts`: Failed authentication attempts

### Backup Metrics
- `BackupFailures`: Number of backup failures
- `LastBackupAge`: Age of last successful backup
- `BackupSize`: Size of latest backup

## Dashboard Access

### CloudWatch Dashboard
Access the main monitoring dashboard at:
```
https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=production-PetClinic-Dashboard
```

### Key Dashboard Sections
1. **System Overview**: EC2 and RDS resource utilization
2. **Application Performance**: Response times and error rates
3. **Jenkins Metrics**: Build status and queue information
4. **Security Monitoring**: Authentication and security events
5. **Backup Status**: Backup operations and health
6. **Recent Errors**: Latest error log entries

## Alerting

### Alert Severity Levels

#### Critical Alerts
- System failures (EC2, RDS status checks)
- Application unavailability
- Security breaches
- Backup failures

#### Warning Alerts
- High resource utilization
- Performance degradation
- Build failures
- Disk space warnings

#### Info Alerts
- Successful deployments
- Backup completions
- System maintenance notifications

### Alert Response

#### Immediate Response Required
- Service unavailability
- Database failures
- Security incidents
- Critical resource exhaustion

#### Investigation Required
- Performance degradation
- High error rates
- Build failures
- Resource warnings

## Troubleshooting

### Common Issues

#### Custom Metrics Not Appearing
1. Check service status: `sudo systemctl status petclinic-metrics.service`
2. Review service logs: `sudo journalctl -u petclinic-metrics.service -f`
3. Verify AWS credentials and permissions
4. Check network connectivity to AWS services

#### Alarms Not Triggering
1. Verify metric data is being published to CloudWatch
2. Check alarm configuration and thresholds
3. Ensure SNS topic subscriptions are confirmed
4. Review CloudWatch alarm history

#### Notifications Not Received
1. Confirm email subscription to SNS topics
2. Check Slack webhook URL configuration
3. Verify PagerDuty integration key
4. Review Lambda function logs for notification services

#### Dashboard Not Loading
1. Verify CloudWatch dashboard exists
2. Check IAM permissions for dashboard access
3. Ensure metrics are being published
4. Review browser console for errors

### Log Locations

- **Custom Metrics Service**: `sudo journalctl -u petclinic-metrics.service`
- **CloudWatch Agent**: `/opt/aws/amazon-cloudwatch-agent/logs/`
- **Application Logs**: `/var/log/petclinic/`
- **Jenkins Logs**: `/var/log/jenkins/`
- **System Logs**: `/var/log/syslog`

### Useful Commands

```bash
# Check custom metrics service
sudo systemctl status petclinic-metrics.service

# View recent metrics
aws cloudwatch get-metric-statistics \
    --namespace PetClinic/Application \
    --metric-name ErrorCount \
    --start-time 2023-01-01T00:00:00Z \
    --end-time 2023-01-01T01:00:00Z \
    --period 300 \
    --statistics Sum

# List active alarms
aws cloudwatch describe-alarms \
    --state-value ALARM \
    --alarm-name-prefix production-PetClinic

# Test SNS notification
aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:production-PetClinic-CriticalAlerts \
    --message "Test notification from Pet Clinic monitoring"
```

## Maintenance

### Regular Tasks

#### Daily
- Review dashboard for anomalies
- Check alarm status
- Verify backup completion

#### Weekly
- Review metric trends
- Update alarm thresholds if needed
- Test notification channels

#### Monthly
- Review and update monitoring configuration
- Analyze performance trends
- Update documentation

### Updates and Changes

#### Adding New Metrics
1. Update `custom-metrics-publisher.py`
2. Add corresponding CloudWatch alarms
3. Update dashboard configuration
4. Test new metrics and alarms

#### Modifying Thresholds
1. Update CloudFormation templates
2. Deploy stack updates
3. Test alarm behavior
4. Document changes

#### Adding Notification Channels
1. Update SNS configuration
2. Add Lambda functions for new channels
3. Test notification delivery
4. Update documentation

## Security Considerations

### IAM Permissions
- Use least privilege principle for monitoring roles
- Regularly review and audit permissions
- Use IAM roles instead of access keys where possible

### Data Protection
- Encrypt SNS topics and CloudWatch logs
- Secure webhook URLs and API keys
- Monitor access to monitoring systems

### Access Control
- Restrict dashboard access to authorized users
- Use MFA for administrative access
- Log and monitor configuration changes

## Cost Optimization

### CloudWatch Costs
- Monitor CloudWatch usage and costs
- Optimize metric retention periods
- Use metric filters efficiently
- Consider custom metrics usage

### Notification Costs
- Optimize SNS message frequency
- Use appropriate notification channels
- Implement notification throttling for high-frequency events

## Support and Documentation

### Additional Resources
- [AWS CloudWatch Documentation](https://docs.aws.amazon.com/cloudwatch/)
- [SNS Documentation](https://docs.aws.amazon.com/sns/)
- [CloudFormation Documentation](https://docs.aws.amazon.com/cloudformation/)

### Support Contacts
- **Primary**: DevOps Team (devops@petclinic.local)
- **Secondary**: System Administrator (admin@petclinic.local)
- **Emergency**: On-call Engineer (oncall@petclinic.local)

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2023-12-01 | 1.0 | Initial monitoring configuration |
| 2023-12-15 | 1.1 | Added custom metrics and dashboard |
| 2024-01-01 | 1.2 | Enhanced alerting and notification system |