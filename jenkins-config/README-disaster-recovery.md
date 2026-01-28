# Jenkins Disaster Recovery Documentation

This document provides comprehensive instructions for Jenkins disaster recovery procedures using the automated scripts in this directory.

## Overview

The disaster recovery system consists of several components:

1. **Backup System**: Automated daily backups to S3 with encryption and integrity checking
2. **Restore Scripts**: Automated restoration from S3 backups
3. **Disaster Recovery**: Complete disaster recovery automation
4. **Validation**: Post-recovery validation and reporting

## Scripts Overview

### Core Scripts

- `s3-backup-sync.sh` - Synchronizes Jenkins backups to S3 with encryption
- `s3-backup-restore.sh` - Restores Jenkins from S3 backups
- `disaster-recovery.sh` - Complete disaster recovery automation
- `recovery-validation.sh` - Validates recovered Jenkins installation
- `backup-cron-setup.sh` - Sets up automated backup synchronization

### Configuration Scripts

- `setup-thinbackup.sh` - Configures ThinBackup plugin
- `thinbackup-config.xml` - ThinBackup plugin configuration
- `backup-test.sh` - Tests backup functionality

## Prerequisites

### System Requirements

- Linux system (Ubuntu/Debian or RHEL/CentOS/Amazon Linux)
- Root or sudo access
- AWS CLI installed and configured
- Internet connectivity
- Sufficient disk space for backups and restoration

### AWS Requirements

- S3 bucket for backup storage
- IAM role/user with appropriate permissions:
  - S3: GetObject, PutObject, DeleteObject, ListBucket
  - KMS: Encrypt, Decrypt (if using KMS encryption)
  - SNS: Publish (for notifications, optional)
  - CloudWatch: PutLogEvents (for logging, optional)

### Environment Variables

Set the following environment variables for optimal operation:

```bash
export JENKINS_HOME="/var/lib/jenkins"
export S3_BACKUP_BUCKET="your-jenkins-backup-bucket"
export AWS_REGION="us-east-1"
export KMS_KEY_ID="alias/jenkins-backup-key"
export SNS_TOPIC_ARN="arn:aws:sns:us-east-1:123456789012:jenkins-alerts"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
export CLOUDWATCH_LOG_GROUP="/aws/jenkins/backup"
```

## Disaster Recovery Procedures

### Scenario 1: Complete System Loss

When the entire Jenkins server is lost and needs to be rebuilt from scratch:

1. **Launch new EC2 instance** with appropriate specifications
2. **Install basic dependencies**:
   ```bash
   sudo apt-get update && sudo apt-get install -y curl wget unzip python3 awscli
   ```
3. **Configure AWS credentials**:
   ```bash
   aws configure
   ```
4. **Download recovery scripts**:
   ```bash
   wget https://your-scripts-location/disaster-recovery.sh
   chmod +x disaster-recovery.sh
   ```
5. **Run disaster recovery**:
   ```bash
   sudo ./disaster-recovery.sh
   ```

### Scenario 2: Jenkins Data Corruption

When Jenkins is installed but data is corrupted:

1. **Stop Jenkins service**:
   ```bash
   sudo systemctl stop jenkins
   ```
2. **Run restore script**:
   ```bash
   ./s3-backup-restore.sh --interactive
   ```
3. **Validate recovery**:
   ```bash
   ./recovery-validation.sh
   ```

### Scenario 3: Restore Specific Backup

To restore from a specific backup instead of the latest:

1. **List available backups**:
   ```bash
   ./s3-backup-restore.sh --list
   ```
2. **Restore specific backup**:
   ```bash
   ./s3-backup-restore.sh --backup "jenkins-backups/2023/12/01/backup-file.zip"
   ```

## Backup Management

### Manual Backup

To create an immediate backup:

```bash
# Trigger ThinBackup manually
curl -X POST http://localhost:8080/thinBackup/backupManual

# Sync to S3
./s3-backup-sync.sh
```

### Backup Verification

To verify backup integrity:

```bash
# Test backup creation and sync
./backup-test.sh

# Verify S3 backups
aws s3 ls s3://your-backup-bucket/jenkins-backups/ --recursive
```

### Backup Retention

The system implements a three-tier retention policy:
- **Daily backups**: 30 days
- **Weekly backups**: 12 weeks
- **Monthly backups**: 12 months

## Recovery Validation

After any recovery operation, run the validation script:

```bash
./recovery-validation.sh
```

This script validates:
- Jenkins service status
- Web interface accessibility
- Configuration file integrity
- Job configurations
- User accounts
- Plugin status
- Build history
- System configuration

## Monitoring and Alerting

### SNS Notifications

Configure SNS topic for backup and recovery notifications:

```bash
aws sns create-topic --name jenkins-backup-alerts
aws sns subscribe --topic-arn arn:aws:sns:us-east-1:123456789012:jenkins-backup-alerts \
    --protocol email --notification-endpoint your-email@example.com
```

### CloudWatch Logging

Enable CloudWatch logging for backup operations:

```bash
aws logs create-log-group --log-group-name /aws/jenkins/backup
```

### Slack Integration

Configure Slack webhook for real-time notifications:

1. Create Slack app and webhook URL
2. Set `SLACK_WEBHOOK_URL` environment variable
3. Notifications will be sent automatically during backup/recovery operations

## Troubleshooting

### Common Issues

#### Backup Sync Failures

**Problem**: S3 sync fails with permission errors
**Solution**: 
```bash
# Check AWS credentials
aws sts get-caller-identity

# Verify S3 bucket permissions
aws s3 ls s3://your-backup-bucket/
```

#### Recovery Failures

**Problem**: Jenkins fails to start after recovery
**Solution**:
```bash
# Check Jenkins logs
sudo journalctl -u jenkins -f

# Verify file permissions
sudo chown -R jenkins:jenkins /var/lib/jenkins
```

#### Validation Failures

**Problem**: Validation script reports configuration issues
**Solution**:
```bash
# Check specific validation results
cat /tmp/jenkins-validation-report-*.json | jq '.issues_found'

# Re-run recovery with different backup
./s3-backup-restore.sh --interactive
```

### Log Locations

- Backup sync logs: `/var/log/jenkins-backup-sync.log`
- Restore logs: `/var/log/jenkins-backup-restore.log`
- Disaster recovery logs: `/var/log/jenkins-disaster-recovery.log`
- Validation logs: `/var/log/jenkins-recovery-validation.log`
- Jenkins system logs: `/var/log/jenkins/jenkins.log`

## Testing Procedures

### Regular Testing

Perform disaster recovery testing monthly:

1. **Create test environment**
2. **Run disaster recovery simulation**:
   ```bash
   ./disaster-recovery.sh --backup "specific-test-backup"
   ```
3. **Validate recovery**:
   ```bash
   ./recovery-validation.sh
   ```
4. **Document results and issues**

### Backup Testing

Test backup integrity weekly:

```bash
# Run backup test
./backup-test.sh

# Verify backup can be restored
./s3-backup-restore.sh --backup "latest-test-backup" --skip-confirmation
```

## Security Considerations

### Encryption

- All backups are encrypted in transit using HTTPS
- S3 server-side encryption with KMS keys
- Local backup files should be on encrypted storage

### Access Control

- Limit access to backup scripts and S3 bucket
- Use IAM roles instead of access keys when possible
- Regularly rotate credentials

### Network Security

- Ensure secure communication channels
- Use VPC endpoints for S3 access when possible
- Monitor network traffic for anomalies

## Recovery Time Objectives (RTO)

Expected recovery times:
- **Complete system rebuild**: 30-60 minutes
- **Data restoration only**: 10-20 minutes
- **Validation and testing**: 10-15 minutes

## Recovery Point Objectives (RPO)

Data loss expectations:
- **Daily backups**: Maximum 24 hours of data loss
- **Real-time sync**: Maximum 1 hour of data loss (if configured)

## Contact Information

For disaster recovery support:
- **Primary Contact**: DevOps Team (devops@company.com)
- **Secondary Contact**: System Administrator (sysadmin@company.com)
- **Emergency Contact**: On-call Engineer (oncall@company.com)

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2023-12-01 | 1.0 | Initial disaster recovery documentation |
| 2023-12-15 | 1.1 | Added validation procedures and troubleshooting |
| 2024-01-01 | 1.2 | Enhanced security considerations and testing procedures |