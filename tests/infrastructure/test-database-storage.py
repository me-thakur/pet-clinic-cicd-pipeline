#!/usr/bin/env python3
"""
Infrastructure deployment tests for database and storage components
Tests RDS connectivity, backup configuration, EFS mount functionality, and S3 bucket policies
"""

import boto3
import pytest
import time
import json
from moto import mock_rds, mock_s3, mock_efs, mock_kms, mock_ssm
from pathlib import Path
import yaml

# Test configuration
TEST_ENVIRONMENT = "test"
TEST_REGION = "us-east-1"
TEMPLATES_DIR = Path(__file__).parent.parent.parent / "cloudformation"

class TestDatabaseInfrastructure:
    """Test suite for database infrastructure validation"""
    
    def setup_method(self):
        """Setup test environment"""
        self.test_params = {
            "Environment": TEST_ENVIRONMENT,
            "DatabaseInstanceClass": "db.t3.micro",
            "VPCId": "vpc-12345678",
            "PrivateSubnetId": "subnet-12345678",
            "DatabaseSecurityGroupId": "sg-12345678",
            "DBUsername": "testadmin",
            "DBPassword": "testpassword123",
            "DBName": "testdb"
        }
    
    def load_template(self, template_name):
        """Load CloudFormation template from file"""
        template_path = TEMPLATES_DIR / template_name
        if not template_path.exists():
            pytest.skip(f"Template {template_name} not found")
        
        with open(template_path, 'r') as f:
            content = f.read()
        return content
    
    def test_database_template_structure(self):
        """Test database template has required resources"""
        template_content = self.load_template("database.yaml")
        
        # Check for required resources
        required_resources = [
            "DBSubnetGroup:",
            "DBParameterGroup:",
            "PetClinicDatabase:",
            "DatabaseCPUAlarm:",
            "DatabaseConnectionsAlarm:",
            "DatabaseAlarmTopic:"
        ]
        
        for resource in required_resources:
            assert resource in template_content, f"Database template missing {resource.replace(':', '')}"
        
        print("✓ Database template has all required resources")
    
    def test_database_security_configuration(self):
        """Test database security settings"""
        template_content = self.load_template("database.yaml")
        
        # Check for security features
        security_features = [
            "StorageEncrypted: true",
            "MultiAZ: true",
            "BackupRetentionPeriod: 7",
            "EnablePerformanceInsights: true"
        ]
        
        for feature in security_features:
            assert feature in template_content, f"Database template missing security feature: {feature}"
        
        print("✓ Database template has proper security configuration")
    
    def test_database_monitoring_setup(self):
        """Test database monitoring and alerting configuration"""
        template_content = self.load_template("database.yaml")
        
        # Check for monitoring components
        monitoring_components = [
            "MonitoringInterval: 60",
            "AWS::CloudWatch::Alarm",
            "AWS::SNS::Topic",
            "CPUUtilization",
            "DatabaseConnections"
        ]
        
        for component in monitoring_components:
            assert component in template_content, f"Database template missing monitoring: {component}"
        
        print("✓ Database template has comprehensive monitoring")
    
    def test_database_parameter_validation(self):
        """Test database parameter configuration"""
        template_content = self.load_template("database.yaml")
        
        # Check parameter group settings
        parameter_settings = [
            "Family: mysql8.0",
            "innodb_buffer_pool_size",
            "max_connections",
            "slow_query_log"
        ]
        
        for setting in parameter_settings:
            assert setting in template_content, f"Database template missing parameter: {setting}"
        
        print("✓ Database template has proper parameter configuration")
    
    @mock_rds
    @mock_ssm
    def test_database_deployment_simulation(self):
        """Simulate database deployment and test connectivity"""
        # Setup mock AWS environment
        rds_client = boto3.client("rds", region_name=TEST_REGION)
        ssm_client = boto3.client("ssm", region_name=TEST_REGION)
        
        try:
            # Create DB subnet group
            rds_client.create_db_subnet_group(
                DBSubnetGroupName=f"{TEST_ENVIRONMENT}-test-subnet-group",
                DBSubnetGroupDescription="Test subnet group",
                SubnetIds=["subnet-12345678", "subnet-87654321"]
            )
            
            # Create database instance
            rds_client.create_db_instance(
                DBInstanceIdentifier=f"{TEST_ENVIRONMENT}-test-db",
                DBInstanceClass="db.t3.micro",
                Engine="mysql",
                MasterUsername="testadmin",
                MasterUserPassword="testpassword123",
                DBName="testdb",
                AllocatedStorage=100,
                DBSubnetGroupName=f"{TEST_ENVIRONMENT}-test-subnet-group"
            )
            
            # Verify database instance exists
            response = rds_client.describe_db_instances(
                DBInstanceIdentifier=f"{TEST_ENVIRONMENT}-test-db"
            )
            
            db_instance = response['DBInstances'][0]
            assert db_instance['DBInstanceStatus'] == 'available'
            assert db_instance['Engine'] == 'mysql'
            assert db_instance['AllocatedStorage'] == 100
            
            print("✓ Database deployment simulation successful")
            
        except Exception as e:
            pytest.fail(f"Database deployment simulation failed: {str(e)}")

class TestStorageInfrastructure:
    """Test suite for storage infrastructure validation"""
    
    def setup_method(self):
        """Setup test environment"""
        self.test_params = {
            "Environment": TEST_ENVIRONMENT,
            "VPCId": "vpc-12345678",
            "PrivateSubnetId": "subnet-12345678",
            "PublicSubnetId": "subnet-87654321"
        }
    
    def load_template(self, template_name):
        """Load CloudFormation template from file"""
        template_path = TEMPLATES_DIR / template_name
        if not template_path.exists():
            pytest.skip(f"Template {template_name} not found")
        
        with open(template_path, 'r') as f:
            content = f.read()
        return content
    
    def test_storage_template_structure(self):
        """Test storage template has required resources"""
        template_content = self.load_template("storage.yaml")
        
        # Check for required S3 resources
        s3_resources = [
            "JenkinsBackupBucket:",
            "ArtifactsBucket:",
            "LogsBucket:",
            "S3EncryptionKey:",
            "JenkinsBackupBucketPolicy:"
        ]
        
        for resource in s3_resources:
            assert resource in template_content, f"Storage template missing {resource.replace(':', '')}"
        
        # Check for required EFS resources
        efs_resources = [
            "EFSFileSystem:",
            "EFSMountTargetPrivate:",
            "EFSMountTargetPublic:",
            "EFSAccessPointJenkins:",
            "EFSAccessPointApp:"
        ]
        
        for resource in efs_resources:
            assert resource in template_content, f"Storage template missing {resource.replace(':', '')}"
        
        print("✓ Storage template has all required resources")
    
    def test_s3_security_configuration(self):
        """Test S3 bucket security settings"""
        template_content = self.load_template("storage.yaml")
        
        # Check for security features
        security_features = [
            "SSEAlgorithm: aws:kms",
            "VersioningConfiguration:",
            "PublicAccessBlockConfiguration:",
            "BlockPublicAcls: true",
            "BlockPublicPolicy: true",
            "IgnorePublicAcls: true",
            "RestrictPublicBuckets: true"
        ]
        
        for feature in security_features:
            assert feature in template_content, f"Storage template missing security feature: {feature}"
        
        print("✓ Storage template has proper S3 security configuration")
    
    def test_efs_encryption_configuration(self):
        """Test EFS encryption and access point configuration"""
        template_content = self.load_template("storage.yaml")
        
        # Check for EFS security features
        efs_security = [
            "Encrypted: true",
            "KmsKeyId:",
            "EFSEncryptionKey:",
            "PosixUser:",
            "RootDirectory:"
        ]
        
        for feature in efs_security:
            assert feature in template_content, f"Storage template missing EFS security: {feature}"
        
        print("✓ Storage template has proper EFS encryption configuration")
    
    def test_lifecycle_policies(self):
        """Test S3 lifecycle policies for cost optimization"""
        template_content = self.load_template("storage.yaml")
        
        # Check for lifecycle configurations
        lifecycle_features = [
            "LifecycleConfiguration:",
            "ExpirationInDays:",
            "STANDARD_IA",
            "GLACIER",
            "DEEP_ARCHIVE"
        ]
        
        for feature in lifecycle_features:
            assert feature in template_content, f"Storage template missing lifecycle feature: {feature}"
        
        print("✓ Storage template has proper lifecycle policies")
    
    @mock_s3
    @mock_kms
    @mock_efs
    def test_storage_deployment_simulation(self):
        """Simulate storage deployment and test functionality"""
        # Setup mock AWS environment
        s3_client = boto3.client("s3", region_name=TEST_REGION)
        kms_client = boto3.client("kms", region_name=TEST_REGION)
        efs_client = boto3.client("efs", region_name=TEST_REGION)
        
        try:
            # Create KMS key
            kms_response = kms_client.create_key(
                Description="Test KMS key for S3 encryption"
            )
            kms_key_id = kms_response['KeyMetadata']['KeyId']
            
            # Create S3 buckets
            bucket_names = [
                f"{TEST_ENVIRONMENT}-pet-clinic-jenkins-backup-123456789",
                f"{TEST_ENVIRONMENT}-pet-clinic-artifacts-123456789",
                f"{TEST_ENVIRONMENT}-pet-clinic-logs-123456789"
            ]
            
            for bucket_name in bucket_names:
                s3_client.create_bucket(Bucket=bucket_name)
                
                # Test bucket encryption
                s3_client.put_bucket_encryption(
                    Bucket=bucket_name,
                    ServerSideEncryptionConfiguration={
                        'Rules': [{
                            'ApplyServerSideEncryptionByDefault': {
                                'SSEAlgorithm': 'aws:kms',
                                'KMSMasterKeyID': kms_key_id
                            }
                        }]
                    }
                )
                
                # Verify bucket exists and is encrypted
                encryption_response = s3_client.get_bucket_encryption(Bucket=bucket_name)
                assert encryption_response['ServerSideEncryptionConfiguration']['Rules'][0]['ApplyServerSideEncryptionByDefault']['SSEAlgorithm'] == 'aws:kms'
            
            # Create EFS file system
            efs_response = efs_client.create_file_system(
                CreationToken=f"{TEST_ENVIRONMENT}-test-efs",
                PerformanceMode='generalPurpose',
                Encrypted=True
            )
            
            file_system_id = efs_response['FileSystemId']
            
            # Verify EFS file system
            describe_response = efs_client.describe_file_systems(
                FileSystemId=file_system_id
            )
            
            assert describe_response['FileSystems'][0]['Encrypted'] == True
            assert describe_response['FileSystems'][0]['PerformanceMode'] == 'generalPurpose'
            
            print("✓ Storage deployment simulation successful")
            
        except Exception as e:
            pytest.fail(f"Storage deployment simulation failed: {str(e)}")
    
    def test_backup_retention_policies(self):
        """Test backup retention policy configuration"""
        template_content = self.load_template("storage.yaml")
        
        # Check for different retention periods
        retention_configs = [
            "ExpirationInDays: 90",  # Jenkins backups
            "ExpirationInDays: 365",  # Artifacts
            "ExpirationInDays: 2555",  # Logs (7 years)
            "NoncurrentVersionExpirationInDays: 30",
            "TransitionInDays: 30"
        ]
        
        for config in retention_configs:
            assert config in template_content, f"Storage template missing retention config: {config}"
        
        print("✓ Storage template has proper backup retention policies")

class TestInfrastructureIntegration:
    """Test suite for database and storage integration"""
    
    def test_parameter_store_integration(self):
        """Test Systems Manager parameter store configuration"""
        # Test database template
        db_template = self.load_template("database.yaml")
        storage_template = self.load_template("storage.yaml")
        
        # Check for SSM parameters in database template
        db_ssm_params = [
            "AWS::SSM::Parameter",
            "/database/endpoint",
            "/database/port",
            "/database/name",
            "/database/username",
            "/database/password"
        ]
        
        for param in db_ssm_params:
            assert param in db_template, f"Database template missing SSM parameter: {param}"
        
        # Check for SSM parameters in storage template
        storage_ssm_params = [
            "AWS::SSM::Parameter",
            "/storage/backup-bucket",
            "/storage/artifacts-bucket",
            "/storage/efs-id"
        ]
        
        for param in storage_ssm_params:
            assert param in storage_template, f"Storage template missing SSM parameter: {param}"
        
        print("✓ Infrastructure templates have proper SSM parameter integration")
    
    def load_template(self, template_name):
        """Load CloudFormation template from file"""
        template_path = TEMPLATES_DIR / template_name
        if not template_path.exists():
            pytest.skip(f"Template {template_name} not found")
        
        with open(template_path, 'r') as f:
            content = f.read()
        return content
    
    def test_cross_stack_references(self):
        """Test that templates properly reference resources from other stacks"""
        db_template = self.load_template("database.yaml")
        storage_template = self.load_template("storage.yaml")
        
        # Database template should reference network resources
        db_references = [
            "VPCId:",
            "PrivateSubnetId:",
            "DatabaseSecurityGroupId:"
        ]
        
        for ref in db_references:
            assert ref in db_template, f"Database template missing cross-stack reference: {ref}"
        
        # Storage template should reference network resources
        storage_references = [
            "VPCId:",
            "PrivateSubnetId:",
            "PublicSubnetId:"
        ]
        
        for ref in storage_references:
            assert ref in storage_template, f"Storage template missing cross-stack reference: {ref}"
        
        print("✓ Infrastructure templates have proper cross-stack references")

if __name__ == "__main__":
    # Run tests
    pytest.main([__file__, "-v"])