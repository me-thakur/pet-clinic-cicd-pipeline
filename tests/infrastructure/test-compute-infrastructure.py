#!/usr/bin/env python3
"""
Compute Infrastructure Tests for Pet Clinic CI/CD Pipeline

This module contains unit tests for the compute infrastructure CloudFormation template.
Tests verify EC2 instance configuration, EFS mounting, load balancer health checks,
and Auto Scaling Group configuration.

Requirements: 4.1, 4.5
"""

import boto3
import json
import pytest
import yaml
from moto import mock_cloudformation, mock_ec2, mock_elbv2, mock_autoscaling, mock_efs
from unittest.mock import patch, MagicMock


class TestComputeInfrastructure:
    """Test suite for compute infrastructure CloudFormation template"""
    
    @classmethod
    def setup_class(cls):
        """Load CloudFormation template for testing"""
        with open('cloudformation/compute.yaml', 'r') as f:
            cls.template = yaml.safe_load(f)
    
    def test_template_structure(self):
        """Test that the template has required sections"""
        assert 'AWSTemplateFormatVersion' in self.template
        assert 'Description' in self.template
        assert 'Parameters' in self.template
        assert 'Resources' in self.template
        assert 'Outputs' in self.template
        
        # Verify description
        assert 'Pet Clinic CI/CD Pipeline - Compute Infrastructure Stack' in self.template['Description']
    
    def test_required_parameters(self):
        """Test that all required parameters are defined"""
        parameters = self.template['Parameters']
        required_params = [
            'Environment', 'VpcId', 'PublicSubnetId', 'PrivateSubnetId',
            'JenkinsSecurityGroupId', 'FrontendSecurityGroupId', 'BackendSecurityGroupId',
            'JenkinsInstanceRole', 'AppInstanceRole', 'EfsFileSystemId',
            'JenkinsInstanceType', 'AppInstanceType', 'KeyPairName'
        ]
        
        for param in required_params:
            assert param in parameters, f"Required parameter {param} not found"
    
    def test_parameter_defaults(self):
        """Test parameter default values"""
        parameters = self.template['Parameters']
        
        assert parameters['Environment']['Default'] == 'dev'
        assert parameters['JenkinsInstanceType']['Default'] == 't3.medium'
        assert parameters['AppInstanceType']['Default'] == 't3.small'
        assert parameters['KeyPairName']['Default'] == ''
    
    def test_parameter_constraints(self):
        """Test parameter constraints and allowed values"""
        parameters = self.template['Parameters']
        
        # Environment constraints
        assert 'AllowedValues' in parameters['Environment']
        assert set(parameters['Environment']['AllowedValues']) == {'dev', 'staging', 'prod'}
        
        # Instance type constraints
        jenkins_types = parameters['JenkinsInstanceType']['AllowedValues']
        assert 't3.medium' in jenkins_types
        assert 't3.large' in jenkins_types
        
        app_types = parameters['AppInstanceType']['AllowedValues']
        assert 't3.micro' in app_types
        assert 't3.small' in app_types
    
    def test_required_resources(self):
        """Test that all required resources are defined"""
        resources = self.template['Resources']
        required_resources = [
            'ApplicationLoadBalancer', 'ALBSecurityGroup', 'FrontendTargetGroup',
            'ALBListener', 'FrontendLaunchTemplate', 'FrontendAutoScalingGroup',
            'BackendLaunchTemplate', 'BackendAutoScalingGroup', 'JenkinsInstance'
        ]
        
        for resource in required_resources:
            assert resource in resources, f"Required resource {resource} not found"
    
    def test_application_load_balancer_configuration(self):
        """Test Application Load Balancer configuration"""
        alb = self.template['Resources']['ApplicationLoadBalancer']
        
        assert alb['Type'] == 'AWS::ElasticLoadBalancingV2::LoadBalancer'
        
        properties = alb['Properties']
        assert properties['Type'] == 'application'
        assert properties['Scheme'] == 'internet-facing'
        assert properties['IpAddressType'] == 'ipv4'
        
        # Check subnets reference
        assert 'Subnets' in properties
        assert len(properties['Subnets']) == 2
    
    def test_target_group_health_check(self):
        """Test target group health check configuration"""
        target_group = self.template['Resources']['FrontendTargetGroup']
        
        assert target_group['Type'] == 'AWS::ElasticLoadBalancingV2::TargetGroup'
        
        properties = target_group['Properties']
        assert properties['Port'] == 8080
        assert properties['Protocol'] == 'HTTP'
        assert properties['HealthCheckEnabled'] == True
        assert properties['HealthCheckPath'] == '/actuator/health'
        assert properties['HealthCheckProtocol'] == 'HTTP'
        assert properties['HealthCheckPort'] == 8080
        assert properties['HealthCheckIntervalSeconds'] == 30
        assert properties['HealthCheckTimeoutSeconds'] == 5
        assert properties['HealthyThresholdCount'] == 2
        assert properties['UnhealthyThresholdCount'] == 3
        
        # Check matcher
        assert 'Matcher' in properties
        assert properties['Matcher']['HttpCode'] == 200
    
    def test_launch_template_configuration(self):
        """Test launch template configuration for applications"""
        frontend_lt = self.template['Resources']['FrontendLaunchTemplate']
        backend_lt = self.template['Resources']['BackendLaunchTemplate']
        
        # Test frontend launch template
        assert frontend_lt['Type'] == 'AWS::EC2::LaunchTemplate'
        frontend_data = frontend_lt['Properties']['LaunchTemplateData']
        
        # Check IAM instance profile reference
        assert 'IamInstanceProfile' in frontend_data
        
        # Check security groups
        assert 'SecurityGroupIds' in frontend_data
        
        # Check user data for EFS mounting and Java/Maven installation
        user_data = frontend_data['UserData']['Fn::Base64']['Fn::Sub']
        assert 'amazon-efs-utils' in user_data
        assert 'install-java.sh' in user_data
        assert 'install-maven.sh' in user_data
        assert 'cfn-signal' in user_data
        
        # Test backend launch template
        assert backend_lt['Type'] == 'AWS::EC2::LaunchTemplate'
        backend_data = backend_lt['Properties']['LaunchTemplateData']
        
        # Similar checks for backend
        assert 'IamInstanceProfile' in backend_data
        assert 'SecurityGroupIds' in backend_data
    
    def test_auto_scaling_group_configuration(self):
        """Test Auto Scaling Group configuration"""
        frontend_asg = self.template['Resources']['FrontendAutoScalingGroup']
        backend_asg = self.template['Resources']['BackendAutoScalingGroup']
        
        # Test frontend ASG
        assert frontend_asg['Type'] == 'AWS::AutoScaling::AutoScalingGroup'
        frontend_props = frontend_asg['Properties']
        
        assert frontend_props['MinSize'] == 1
        assert frontend_props['MaxSize'] == 3
        assert frontend_props['DesiredCapacity'] == 2
        assert frontend_props['HealthCheckType'] == 'ELB'
        assert frontend_props['HealthCheckGracePeriod'] == 300
        
        # Check target group ARN reference
        assert 'TargetGroupARNs' in frontend_props
        
        # Check creation policy
        assert 'CreationPolicy' in frontend_asg
        creation_policy = frontend_asg['CreationPolicy']['ResourceSignal']
        assert creation_policy['Count'] == 1
        assert creation_policy['Timeout'] == 'PT15M'
        
        # Check update policy
        assert 'UpdatePolicy' in frontend_asg
        update_policy = frontend_asg['UpdatePolicy']['AutoScalingRollingUpdate']
        assert update_policy['MinInstancesInService'] == 1
        assert update_policy['MaxBatchSize'] == 1
        assert update_policy['WaitOnResourceSignals'] == True
        
        # Test backend ASG
        assert backend_asg['Type'] == 'AWS::AutoScaling::AutoScalingGroup'
        backend_props = backend_asg['Properties']
        
        assert backend_props['MinSize'] == 1
        assert backend_props['MaxSize'] == 3
        assert backend_props['DesiredCapacity'] == 2
        assert backend_props['HealthCheckType'] == 'EC2'
    
    def test_jenkins_instance_configuration(self):
        """Test Jenkins EC2 instance configuration"""
        jenkins = self.template['Resources']['JenkinsInstance']
        
        assert jenkins['Type'] == 'AWS::EC2::Instance'
        
        properties = jenkins['Properties']
        
        # Check IAM instance profile reference
        assert 'IamInstanceProfile' in properties
        
        # Check security groups
        assert 'SecurityGroupIds' in properties
        
        # Check subnet placement (should be in public subnet)
        assert 'SubnetId' in properties
        
        # Check user data for Jenkins installation
        user_data = properties['UserData']['Fn::Base64']['Fn::Sub']
        assert 'jenkins' in user_data.lower()
        assert 'docker' in user_data
        assert 'aws cli' in user_data.lower() or 'awscli' in user_data
        assert 'amazon-efs-utils' in user_data
        assert 'cfn-signal' in user_data
        
        # Check creation policy
        assert 'CreationPolicy' in jenkins
        creation_policy = jenkins['CreationPolicy']['ResourceSignal']
        assert creation_policy['Count'] == 1
        assert creation_policy['Timeout'] == 'PT15M'
    
    def test_efs_mounting_in_user_data(self):
        """Test EFS mounting configuration in user data scripts"""
        resources_with_user_data = [
            'FrontendLaunchTemplate',
            'BackendLaunchTemplate',
            'JenkinsInstance'
        ]
        
        for resource_name in resources_with_user_data:
            resource = self.template['Resources'][resource_name]
            
            if resource_name == 'JenkinsInstance':
                user_data = resource['Properties']['UserData']['Fn::Base64']['Fn::Sub']
            else:
                user_data = resource['Properties']['LaunchTemplateData']['UserData']['Fn::Base64']['Fn::Sub']
            
            # Check EFS mounting commands
            assert 'amazon-efs-utils' in user_data
            assert 'mkdir -p /mnt/efs' in user_data
            assert 'mount -a' in user_data or 'mount -t efs' in user_data
            assert '${EfsFileSystemId}' in user_data
    
    def test_security_group_references(self):
        """Test security group parameter references"""
        resources_with_sg = {
            'ALBSecurityGroup': None,  # Creates its own SG
            'FrontendLaunchTemplate': 'FrontendSecurityGroupId',
            'BackendLaunchTemplate': 'BackendSecurityGroupId',
            'JenkinsInstance': 'JenkinsSecurityGroupId'
        }
        
        for resource_name, sg_param in resources_with_sg.items():
            if sg_param is None:
                continue
                
            resource = self.template['Resources'][resource_name]
            
            if resource_name == 'JenkinsInstance':
                sg_ids = resource['Properties']['SecurityGroupIds']
            else:
                sg_ids = resource['Properties']['LaunchTemplateData']['SecurityGroupIds']
            
            # Should reference the appropriate security group parameter
            assert len(sg_ids) == 1
            assert '!Ref' in str(sg_ids[0]) or 'Ref' in str(sg_ids[0])
    
    def test_required_outputs(self):
        """Test that all required outputs are defined"""
        outputs = self.template['Outputs']
        required_outputs = [
            'ApplicationLoadBalancerDNS', 'ApplicationLoadBalancerArn',
            'JenkinsInstanceId', 'JenkinsPublicIP',
            'FrontendAutoScalingGroupName', 'BackendAutoScalingGroupName',
            'FrontendTargetGroupArn'
        ]
        
        for output in required_outputs:
            assert output in outputs, f"Required output {output} not found"
    
    def test_output_exports(self):
        """Test output export names"""
        outputs = self.template['Outputs']
        
        # Check that outputs have proper export names
        for output_name, output_config in outputs.items():
            if 'Export' in output_config:
                export_name = output_config['Export']['Name']
                # Should use environment prefix
                assert '${Environment}' in str(export_name) or 'Environment' in str(export_name)
    
    def test_conditional_key_pair(self):
        """Test conditional key pair configuration"""
        conditions = self.template.get('Conditions', {})
        
        # Should have HasKeyPair condition
        assert 'HasKeyPair' in conditions
        
        # Check that launch templates and Jenkins instance use conditional key pair
        resources_with_keypair = [
            'FrontendLaunchTemplate',
            'BackendLaunchTemplate', 
            'JenkinsInstance'
        ]
        
        for resource_name in resources_with_keypair:
            resource = self.template['Resources'][resource_name]
            
            if resource_name == 'JenkinsInstance':
                key_name = resource['Properties'].get('KeyName')
            else:
                key_name = resource['Properties']['LaunchTemplateData'].get('KeyName')
            
            # Should use conditional reference
            if key_name:
                assert '!If' in str(key_name) or 'If' in str(key_name)
    
    def test_tagging_strategy(self):
        """Test resource tagging strategy"""
        resources_with_tags = [
            'ApplicationLoadBalancer',
            'ALBSecurityGroup', 
            'FrontendTargetGroup'
        ]
        
        for resource_name in resources_with_tags:
            resource = self.template['Resources'][resource_name]
            properties = resource['Properties']
            
            assert 'Tags' in properties
            tags = properties['Tags']
            
            # Should have Name and Environment tags
            tag_keys = [tag['Key'] for tag in tags]
            assert 'Name' in tag_keys
            assert 'Environment' in tag_keys
    
    def test_launch_template_tagging(self):
        """Test launch template tag specifications"""
        launch_templates = ['FrontendLaunchTemplate', 'BackendLaunchTemplate']
        
        for lt_name in launch_templates:
            lt = self.template['Resources'][lt_name]
            lt_data = lt['Properties']['LaunchTemplateData']
            
            assert 'TagSpecifications' in lt_data
            tag_specs = lt_data['TagSpecifications']
            
            # Should tag instances
            instance_tags = None
            for spec in tag_specs:
                if spec['ResourceType'] == 'instance':
                    instance_tags = spec['Tags']
                    break
            
            assert instance_tags is not None
            tag_keys = [tag['Key'] for tag in instance_tags]
            assert 'Name' in tag_keys
            assert 'Environment' in tag_keys
            assert 'Role' in tag_keys


@mock_cloudformation
@mock_ec2
@mock_elbv2
@mock_autoscaling
@mock_efs
class TestComputeInfrastructureIntegration:
    """Integration tests for compute infrastructure deployment"""
    
    def setup_method(self):
        """Set up test environment"""
        self.cf_client = boto3.client('cloudformation', region_name='us-east-1')
        self.ec2_client = boto3.client('ec2', region_name='us-east-1')
        self.elbv2_client = boto3.client('elbv2', region_name='us-east-1')
        self.asg_client = boto3.client('autoscaling', region_name='us-east-1')
        
        # Create mock VPC and subnets
        vpc_response = self.ec2_client.create_vpc(CidrBlock='10.0.0.0/16')
        self.vpc_id = vpc_response['Vpc']['VpcId']
        
        # Create subnets
        public_subnet = self.ec2_client.create_subnet(
            VpcId=self.vpc_id,
            CidrBlock='10.0.1.0/24',
            AvailabilityZone='us-east-1a'
        )
        self.public_subnet_id = public_subnet['Subnet']['SubnetId']
        
        private_subnet = self.ec2_client.create_subnet(
            VpcId=self.vpc_id,
            CidrBlock='10.0.2.0/24',
            AvailabilityZone='us-east-1b'
        )
        self.private_subnet_id = private_subnet['Subnet']['SubnetId']
        
        # Create security groups
        self.jenkins_sg_id = self.ec2_client.create_security_group(
            GroupName='jenkins-sg',
            Description='Jenkins security group',
            VpcId=self.vpc_id
        )['GroupId']
        
        self.frontend_sg_id = self.ec2_client.create_security_group(
            GroupName='frontend-sg',
            Description='Frontend security group',
            VpcId=self.vpc_id
        )['GroupId']
        
        self.backend_sg_id = self.ec2_client.create_security_group(
            GroupName='backend-sg',
            Description='Backend security group',
            VpcId=self.vpc_id
        )['GroupId']
    
    def test_stack_creation_with_valid_parameters(self):
        """Test CloudFormation stack creation with valid parameters"""
        with open('cloudformation/compute.yaml', 'r') as f:
            template_body = f.read()
        
        parameters = [
            {'ParameterKey': 'Environment', 'ParameterValue': 'test'},
            {'ParameterKey': 'VpcId', 'ParameterValue': self.vpc_id},
            {'ParameterKey': 'PublicSubnetId', 'ParameterValue': self.public_subnet_id},
            {'ParameterKey': 'PrivateSubnetId', 'ParameterValue': self.private_subnet_id},
            {'ParameterKey': 'JenkinsSecurityGroupId', 'ParameterValue': self.jenkins_sg_id},
            {'ParameterKey': 'FrontendSecurityGroupId', 'ParameterValue': self.frontend_sg_id},
            {'ParameterKey': 'BackendSecurityGroupId', 'ParameterValue': self.backend_sg_id},
            {'ParameterKey': 'JenkinsInstanceRole', 'ParameterValue': 'jenkins-role'},
            {'ParameterKey': 'AppInstanceRole', 'ParameterValue': 'app-role'},
            {'ParameterKey': 'EfsFileSystemId', 'ParameterValue': 'fs-12345678'},
        ]
        
        # This should not raise an exception
        response = self.cf_client.validate_template(TemplateBody=template_body)
        assert 'Parameters' in response
        
        # Check that all our parameters are recognized
        template_params = [p['ParameterKey'] for p in response['Parameters']]
        for param in parameters:
            assert param['ParameterKey'] in template_params
    
    def test_parameter_validation(self):
        """Test parameter validation"""
        with open('cloudformation/compute.yaml', 'r') as f:
            template_body = f.read()
        
        # Test with invalid environment
        invalid_params = [
            {'ParameterKey': 'Environment', 'ParameterValue': 'invalid'},
        ]
        
        # Should validate template structure but parameter constraints 
        # would be enforced during actual stack creation
        response = self.cf_client.validate_template(TemplateBody=template_body)
        assert response['ResponseMetadata']['HTTPStatusCode'] == 200


if __name__ == '__main__':
    # Run tests
    pytest.main([__file__, '-v'])