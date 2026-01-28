#!/usr/bin/env python3
"""
CloudFormation template validation tests for Pet Clinic CI/CD Pipeline
Tests template syntax, parameter validation, and nested stack dependencies
"""

import json
import yaml
import boto3
import pytest
from pathlib import Path
from moto import mock_cloudformation, mock_s3, mock_ec2, mock_iam
import os

# Test configuration
TEMPLATES_DIR = Path(__file__).parent.parent.parent / "cloudformation"
TEST_ENVIRONMENT = "test"
TEST_REGION = "us-east-1"

class TestCloudFormationTemplates:
    """Test suite for CloudFormation template validation"""
    
    def setup_method(self):
        """Setup test environment"""
        self.templates_dir = TEMPLATES_DIR
        self.test_params = {
            "Environment": TEST_ENVIRONMENT,
            "InstanceType": "t3.medium",
            "DatabaseInstanceClass": "db.t3.micro",
            "KeyPairName": "test-keypair",
            "AdminCIDR": "10.0.0.0/8",
            "TemplateBaseURL": "https://s3.amazonaws.com/test-bucket"
        }
    
    def load_template(self, template_name):
        """Load CloudFormation template from file"""
        template_path = self.templates_dir / template_name
        if not template_path.exists():
            pytest.skip(f"Template {template_name} not found")
        
        with open(template_path, 'r') as f:
            return yaml.safe_load(f)
    
    def test_master_stack_template_syntax(self):
        """Test master stack template has valid YAML syntax"""
        template = self.load_template("master-stack.yaml")
        
        # Verify required sections exist
        assert "AWSTemplateFormatVersion" in template
        assert "Description" in template
        assert "Parameters" in template
        assert "Resources" in template
        assert "Outputs" in template
        
        # Verify template format version
        assert template["AWSTemplateFormatVersion"] == "2010-09-09"
    
    def test_network_stack_template_syntax(self):
        """Test network stack template has valid YAML syntax"""
        template = self.load_template("network.yaml")
        
        # Verify required sections exist
        assert "AWSTemplateFormatVersion" in template
        assert "Resources" in template
        assert "Outputs" in template
        
        # Verify key resources exist
        resources = template["Resources"]
        assert "VPC" in resources
        assert "PublicSubnet" in resources
        assert "PrivateSubnet" in resources
        assert "InternetGateway" in resources
        assert "NATGateway" in resources
    
    def test_iam_stack_template_syntax(self):
        """Test IAM stack template has valid YAML syntax"""
        template = self.load_template("iam.yaml")
        
        # Verify required sections exist
        assert "AWSTemplateFormatVersion" in template
        assert "Resources" in resources
        assert "Outputs" in template
        
        # Verify key IAM resources exist
        resources = template["Resources"]
        assert "JenkinsInstanceRole" in resources
        assert "AppInstanceRole" in resources
        assert "JenkinsInstanceProfile" in resources
        assert "AppInstanceProfile" in resources
    
    def test_master_stack_parameters(self):
        """Test master stack parameters are properly defined"""
        template = self.load_template("master-stack.yaml")
        parameters = template["Parameters"]
        
        # Verify required parameters exist
        required_params = [
            "Environment", "InstanceType", "DatabaseInstanceClass",
            "KeyPairName", "AdminCIDR", "TemplateBaseURL"
        ]
        
        for param in required_params:
            assert param in parameters, f"Required parameter {param} missing"
            assert "Type" in parameters[param], f"Parameter {param} missing Type"
        
        # Verify parameter constraints
        assert parameters["Environment"]["AllowedValues"] == ["dev", "staging", "prod"]
        assert parameters["InstanceType"]["AllowedValues"] == ["t3.small", "t3.medium", "t3.large"]
    
    def test_nested_stack_dependencies(self):
        """Test nested stack dependencies are correctly defined"""
        template = self.load_template("master-stack.yaml")
        resources = template["Resources"]
        
        # Verify nested stacks exist
        nested_stacks = ["IAMStack", "NetworkStack", "StorageStack", "DatabaseStack", "ComputeStack"]
        for stack in nested_stacks:
            assert stack in resources, f"Nested stack {stack} missing"
            assert resources[stack]["Type"] == "AWS::CloudFormation::Stack"
        
        # Verify dependencies
        assert "DependsOn" not in resources["IAMStack"], "IAM stack should not depend on others"
        assert "DependsOn" not in resources["NetworkStack"], "Network stack should not depend on others"
        
        storage_deps = resources["StorageStack"].get("DependsOn", [])
        assert "NetworkStack" in storage_deps, "Storage stack should depend on Network stack"
        
        compute_deps = resources["ComputeStack"].get("DependsOn", [])
        expected_deps = ["NetworkStack", "IAMStack", "StorageStack"]
        for dep in expected_deps:
            assert dep in compute_deps, f"Compute stack should depend on {dep}"
    
    def test_stack_outputs(self):
        """Test stack outputs are properly defined"""
        template = self.load_template("master-stack.yaml")
        outputs = template["Outputs"]
        
        # Verify required outputs exist
        required_outputs = [
            "VPCId", "PublicSubnetId", "PrivateSubnetId",
            "JenkinsServerInstanceId", "JenkinsServerPublicIP",
            "DatabaseEndpoint", "S3BackupBucket", "EFSFileSystemId"
        ]
        
        for output in required_outputs:
            assert output in outputs, f"Required output {output} missing"
            assert "Description" in outputs[output], f"Output {output} missing description"
            assert "Value" in outputs[output], f"Output {output} missing value"
    
    def test_security_groups_configuration(self):
        """Test security groups are properly configured"""
        template = self.load_template("network.yaml")
        resources = template["Resources"]
        
        # Verify security groups exist
        security_groups = [
            "JenkinsSecurityGroup", "FrontendSecurityGroup",
            "BackendSecurityGroup", "DatabaseSecurityGroup", "EFSSecurityGroup"
        ]
        
        for sg in security_groups:
            assert sg in resources, f"Security group {sg} missing"
            sg_resource = resources[sg]
            assert sg_resource["Type"] == "AWS::EC2::SecurityGroup"
            assert "SecurityGroupIngress" in sg_resource["Properties"]
    
    def test_iam_policies_least_privilege(self):
        """Test IAM policies follow least privilege principle"""
        template = self.load_template("iam.yaml")
        resources = template["Resources"]
        
        # Check Jenkins instance policy
        jenkins_policy = resources["JenkinsInstancePolicy"]["Properties"]["PolicyDocument"]
        statements = jenkins_policy["Statement"]
        
        # Verify no wildcard resources for sensitive actions
        for statement in statements:
            if statement["Effect"] == "Allow":
                actions = statement.get("Action", [])
                resources_list = statement.get("Resource", [])
                
                # Check for overly permissive actions
                dangerous_actions = ["*", "iam:*", "ec2:*"]
                for action in actions:
                    assert action not in dangerous_actions, f"Overly permissive action: {action}"
    
    @mock_cloudformation
    @mock_s3
    @mock_ec2
    @mock_iam
    def test_template_validation_with_aws(self):
        """Test template validation using AWS CloudFormation service"""
        # Setup mock AWS environment
        cf_client = boto3.client("cloudformation", region_name=TEST_REGION)
        s3_client = boto3.client("s3", region_name=TEST_REGION)
        ec2_client = boto3.client("ec2", region_name=TEST_REGION)
        
        # Create test S3 bucket for templates
        s3_client.create_bucket(Bucket="test-bucket")
        
        # Create test VPC and key pair for validation
        vpc_response = ec2_client.create_vpc(CidrBlock="10.0.0.0/16")
        ec2_client.create_key_pair(KeyName="test-keypair")
        
        # Load and validate master template
        template = self.load_template("master-stack.yaml")
        template_body = yaml.dump(template)
        
        try:
            # Validate template syntax with CloudFormation
            response = cf_client.validate_template(TemplateBody=template_body)
            
            # Verify validation response
            assert "Parameters" in response
            assert "Description" in response
            
            # Check parameters match our expectations
            cf_params = {p["ParameterKey"]: p for p in response["Parameters"]}
            for param_name in self.test_params.keys():
                if param_name != "TemplateBaseURL":  # Skip URL param for mock test
                    assert param_name in cf_params, f"Parameter {param_name} not found in validation"
        
        except Exception as e:
            pytest.fail(f"Template validation failed: {str(e)}")
    
    def test_resource_naming_conventions(self):
        """Test resources follow consistent naming conventions"""
        templates = ["master-stack.yaml", "network.yaml", "iam.yaml"]
        
        for template_name in templates:
            template = self.load_template(template_name)
            resources = template.get("Resources", {})
            
            for resource_name, resource_config in resources.items():
                # Resource names should be PascalCase
                assert resource_name[0].isupper(), f"Resource {resource_name} should start with uppercase"
                assert "_" not in resource_name, f"Resource {resource_name} should not contain underscores"
                
                # Resources should have proper tags where applicable
                properties = resource_config.get("Properties", {})
                if resource_config["Type"] in ["AWS::EC2::VPC", "AWS::EC2::Subnet", "AWS::EC2::SecurityGroup"]:
                    tags = properties.get("Tags", [])
                    tag_keys = [tag["Key"] for tag in tags]
                    assert "Environment" in tag_keys, f"Resource {resource_name} missing Environment tag"
    
    def test_template_descriptions(self):
        """Test all templates have meaningful descriptions"""
        templates = ["master-stack.yaml", "network.yaml", "iam.yaml"]
        
        for template_name in templates:
            template = self.load_template(template_name)
            
            assert "Description" in template, f"Template {template_name} missing description"
            description = template["Description"]
            assert len(description) > 20, f"Template {template_name} description too short"
            assert "Pet Clinic" in description, f"Template {template_name} description should mention Pet Clinic"

if __name__ == "__main__":
    # Run tests
    pytest.main([__file__, "-v"])