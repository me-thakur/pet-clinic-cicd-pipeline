# Logging Configuration for Pet Clinic CI/CD Pipeline

This directory contains comprehensive logging configuration for the Pet Clinic CI/CD Pipeline system, including application logs, system logs, Jenkins logs, and centralized log management.

## Overview

The logging system provides:

- **Centralized Logging**: All logs are collected and managed centrally
- **Structured Logging**: JSON format for production environments
- **Log Rotation**: Automatic log rotation and retention policies
- **Real-time Monitoring**: Automated monitoring and alerting
- **CloudWatch Integration**: AWS CloudWatch Logs integration for cloud environments
- **Security Logging**: Dedicated security event logging and monitoring

## Components

### 1. Application Logging (`logback-spring.xml`)

Spring Boot application logging configuration with:
- Multiple appenders (console, file, JSON, async)
- Environment-specific configurations
- Separate log files for different concerns (application, error, security, database)
- Automatic log rotation and compression
- Performance-optimized async logging

**Log Files:**
- `/var/log/petclinic/application.log` - General application logs
- `/var/log/petclinic/error.log` - Error-level logs only
- `/var/log/petclinic/security.log` - Security-related events
- `/var/log/petclinic/database.log` - Database operations
- `/var/log/petclinic/application.json` - Structured JSON logs (production)

### 2. Jenkins Logging (`jenkins-logging.properties`)

Jenkins CI/CD system logging configuration:
- Build and deployment logging
- Pipeline execution logs
- Plugin and system logs
- SCM integration logs
- Backup operation logs

**Log Files:**
- `/var/log/jenkins/jenkins.log` - Main Jenkins application log
- `/var/log/jenkins/jenkins-backup-sync.log` - Backup synchronization logs
- `/var/log/jenkins/jenkins-disaster-recovery.log` - Disaster recovery logs

### 3. System Logging (`rsyslog-petclinic.conf`)

System-level logging configuration:
- Centralized syslog collection
- Component-specific log routing
- Security event filtering
- Error log aggregation

### 4. CloudWatch Integration (`cloudwatch-logs-config.json`)

AWS CloudWatch Logs configuration:
- Automatic log shipping to CloudWatch
- Multiple log groups for different components
- Configurable retention periods
- System metrics collection

**CloudWatch Log Groups:**
- `/aws/petclinic/application` - Application logs
- `/aws/petclinic/errors` - Error logs
- `/aws/petclinic/security` - Security logs
- `/aws/jenkins/application` - Jenkins logs
- `/aws/jenkins/backup` - Backup operation logs
- `/aws/system/syslog` - System logs

### 5. Log Monitoring (`petclinic-log-monitor.sh`)

Automated log monitoring and alerting:
- Error rate monitoring
- Security event detection
- Automated alerting via email, SNS, and syslog
- Configurable thresholds and patterns

## Installation

### Automatic Setup

Run the setup script as root:

```bash
sudo ./setup-logging.sh
```

This script will:
1. Create necessary log directories
2. Configure rsyslog
3. Set up log rotation
4. Install CloudWatch agent (on AWS EC2)
5. Configure Jenkins logging
6. Set up application logging
7. Create monitoring scripts
8. Test the configuration

### Manual Setup

#### 1. Create Log Directories

```bash
sudo mkdir -p /var/log/petclinic
sudo mkdir -p /var/log/jenkins
sudo chmod 755 /var/log/petclinic /var/log/jenkins
```

#### 2. Configure Rsyslog

```bash
sudo cp rsyslog-petclinic.conf /etc/rsyslog.d/49-petclinic.conf
sudo systemctl restart rsyslog
```

#### 3. Configure Log Rotation

```bash
sudo cp logrotate-petclinic /etc/logrotate.d/petclinic
sudo cp logrotate-jenkins /etc/logrotate.d/jenkins-custom
```

#### 4. Configure Application Logging

```bash
# Copy logback configuration to application classpath
sudo mkdir -p /opt/petclinic/config
sudo cp logback-spring.xml /opt/petclinic/config/

# Set environment variables
echo "LOG_PATH=/var/log/petclinic" | sudo tee -a /etc/environment
echo "LOGGING_LEVEL_ROOT=INFO" | sudo tee -a /etc/environment
```

#### 5. Configure Jenkins Logging

```bash
# Copy Jenkins logging properties
sudo cp jenkins-logging.properties $JENKINS_HOME/logging.properties
sudo chown jenkins:jenkins $JENKINS_HOME/logging.properties

# Add to Jenkins startup parameters
echo 'JAVA_ARGS="$JAVA_ARGS -Djava.util.logging.config.file=$JENKINS_HOME/logging.properties"' | sudo tee -a /etc/default/jenkins
```

#### 6. Install CloudWatch Agent (AWS EC2 only)

```bash
# Download and install CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U amazon-cloudwatch-agent.rpm

# Configure and start agent
sudo cp cloudwatch-logs-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config -m ec2 \
    -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s
```

## Configuration

### Environment Variables

Set the following environment variables for optimal logging:

```bash
# Application logging
export LOG_PATH="/var/log/petclinic"
export LOGGING_LEVEL_ROOT="INFO"
export LOGGING_LEVEL_COM_PETCLINIC="DEBUG"

# Jenkins logging
export JENKINS_HOME="/var/lib/jenkins"

# Monitoring and alerting
export ALERT_EMAIL="admin@petclinic.local"
export SNS_TOPIC_ARN="arn:aws:sns:us-east-1:123456789012:petclinic-alerts"
```

### Spring Boot Application Properties

Add to `application.yml`:

```yaml
logging:
  config: classpath:logback-spring.xml
  level:
    com.petclinic: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  file:
    name: /var/log/petclinic/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## Log Levels

### Application Logs
- **TRACE**: Detailed debugging information
- **DEBUG**: Development debugging information
- **INFO**: General application flow information
- **WARN**: Warning messages for potential issues
- **ERROR**: Error messages for application failures
- **FATAL**: Critical errors that may cause application shutdown

### Jenkins Logs
- **FINEST/TRACE**: Detailed debugging
- **FINE/DEBUG**: Debug information
- **INFO**: General information
- **WARNING**: Warning messages
- **SEVERE/ERROR**: Error messages

## Log Retention

### Local Log Retention
- **Application logs**: 30 days
- **Error logs**: 60 days
- **Security logs**: 90 days
- **Jenkins logs**: 30 days
- **Backup logs**: 60 days
- **System logs**: 14 days

### CloudWatch Log Retention
- **Application logs**: 30 days
- **Error logs**: 60 days
- **Security logs**: 90 days
- **Jenkins logs**: 30 days
- **System logs**: 14 days

## Monitoring and Alerting

### Automated Monitoring

The log monitoring script (`petclinic-log-monitor.sh`) runs every 5 minutes and monitors for:

- **Error Patterns**: ERROR, FATAL, Exception, OutOfMemoryError
- **Security Patterns**: authentication failed, unauthorized access
- **High Error Rates**: More than 10 errors in 5 minutes

### Alert Channels

1. **Email**: Configured via `ALERT_EMAIL` environment variable
2. **SNS**: AWS SNS notifications via `SNS_TOPIC_ARN`
3. **Syslog**: Critical alerts logged to system log
4. **Slack**: Integration via webhook URL (optional)

### Custom Monitoring

Add custom monitoring patterns by modifying the monitoring script:

```bash
# Add custom error patterns
ERROR_PATTERNS+=("CustomError" "BusinessLogicException")

# Add custom security patterns
SECURITY_PATTERNS+=("privilege escalation" "data breach")
```

## Troubleshooting

### Common Issues

#### Logs Not Being Created

1. **Check permissions**:
   ```bash
   ls -la /var/log/petclinic/
   ls -la /var/log/jenkins/
   ```

2. **Check rsyslog configuration**:
   ```bash
   sudo rsyslogd -N1  # Test configuration
   sudo systemctl status rsyslog
   ```

3. **Check application configuration**:
   ```bash
   # Verify logback configuration is loaded
   grep -i "logback" /var/log/petclinic/application.log
   ```

#### CloudWatch Logs Not Shipping

1. **Check CloudWatch agent status**:
   ```bash
   sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
       -m ec2 -c default -a query
   ```

2. **Check IAM permissions**:
   - Ensure EC2 instance has CloudWatch Logs permissions
   - Verify log groups exist in CloudWatch console

3. **Check agent logs**:
   ```bash
   sudo tail -f /opt/aws/amazon-cloudwatch-agent/logs/amazon-cloudwatch-agent.log
   ```

#### High Log Volume

1. **Adjust log levels**:
   ```bash
   # Reduce verbosity in production
   export LOGGING_LEVEL_ROOT="WARN"
   export LOGGING_LEVEL_COM_PETCLINIC="INFO"
   ```

2. **Configure log sampling**:
   - Implement log sampling for high-frequency events
   - Use async appenders for better performance

3. **Increase log rotation frequency**:
   ```bash
   # Edit logrotate configuration
   sudo vim /etc/logrotate.d/petclinic
   ```

### Log Analysis

#### Useful Commands

```bash
# View real-time application logs
tail -f /var/log/petclinic/application.log

# Search for errors in the last hour
find /var/log/petclinic -name "*.log" -mmin -60 -exec grep -H "ERROR" {} \;

# Count error occurrences
grep -c "ERROR" /var/log/petclinic/application.log

# View security events
tail -f /var/log/petclinic/security.log

# Monitor Jenkins build logs
tail -f /var/log/jenkins/jenkins.log | grep -i "build"

# Check system resource usage from logs
grep -i "memory\|cpu\|disk" /var/log/syslog
```

#### Log Analysis Tools

- **ELK Stack**: Elasticsearch, Logstash, Kibana for advanced log analysis
- **Splunk**: Commercial log analysis platform
- **AWS CloudWatch Insights**: Query and analyze CloudWatch logs
- **Grafana**: Visualization and dashboards for log metrics

## Security Considerations

### Log Security

1. **File Permissions**: Restrict access to sensitive log files
2. **Log Encryption**: Encrypt logs in transit and at rest
3. **Access Control**: Implement proper access controls for log files
4. **Audit Logging**: Maintain audit trails for log access

### Sensitive Data

- **PII Filtering**: Ensure no personally identifiable information in logs
- **Credential Masking**: Mask passwords and API keys in logs
- **Data Retention**: Follow data retention policies and regulations
- **Secure Transmission**: Use encrypted channels for log shipping

## Performance Optimization

### Application Performance

1. **Async Logging**: Use async appenders for better performance
2. **Log Level Optimization**: Use appropriate log levels for production
3. **Structured Logging**: Use JSON format for better parsing performance
4. **Log Sampling**: Implement sampling for high-frequency events

### System Performance

1. **Log Rotation**: Configure appropriate rotation policies
2. **Compression**: Enable log compression to save disk space
3. **Remote Shipping**: Ship logs to remote systems to reduce local I/O
4. **Monitoring**: Monitor log system performance and resource usage

## Integration

### CI/CD Pipeline Integration

The logging system integrates with the CI/CD pipeline by:

1. **Build Logs**: Capturing all build and deployment logs
2. **Test Results**: Logging test execution and results
3. **Deployment Tracking**: Tracking deployment status and issues
4. **Performance Metrics**: Collecting performance data during deployments

### Monitoring System Integration

- **Prometheus**: Metrics collection from log data
- **Grafana**: Visualization of log-based metrics
- **AlertManager**: Alert routing and management
- **PagerDuty**: Incident management integration

## Maintenance

### Regular Tasks

1. **Log Review**: Regular review of error and security logs
2. **Retention Policy**: Ensure log retention policies are followed
3. **Performance Monitoring**: Monitor logging system performance
4. **Configuration Updates**: Keep logging configurations up to date

### Backup and Recovery

1. **Log Backups**: Regular backups of critical log data
2. **Configuration Backups**: Backup logging configurations
3. **Recovery Testing**: Test log recovery procedures
4. **Documentation**: Maintain up-to-date documentation

## Support

For logging system support:

- **Documentation**: Refer to this README and component documentation
- **Logs**: Check system logs for logging system issues
- **Monitoring**: Use monitoring dashboards to identify issues
- **Support Team**: Contact the DevOps team for assistance

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2023-12-01 | 1.0 | Initial logging configuration |
| 2023-12-15 | 1.1 | Added CloudWatch integration |
| 2024-01-01 | 1.2 | Enhanced monitoring and alerting |