#!/usr/bin/env python3
"""
Property-based test for Infrastructure Update Idempotency

Property 4: Infrastructure Update Idempotency
For any CloudFormation stack update operation, running the same update multiple times 
should produce the same infrastructure state without data loss

Validates: Requirements 2.4
"""

import pytest
import boto3
import json
import time
import hashlib
from hypothesis import given, strategies as st, settings
from typing import Dict, Any, Optional, List
import uuid
from datetime import datetime
from botocore.exceptions import ClientError


class InfrastructureUpdateIdempotencyTest:
    """Property-based test for infrastructure update idempotency"""
    
    def __init__(self):
        self.aws_region = os.getenv('AWS_REGION', 'us-east-1')
        self.stack_name = os.getenv('CLOUDFORMATION_STACK', 'pet-clinic-pipeline')
        self.test_stack_prefix = 'pet-clinic-idempotency-test'
        
        # Initialize AWS clients
        self.cloudformation_client = boto3.client('cloudformation', region_name=self.aws_region)
        self.ec2_client = boto3.client('ec2', region_name=self.aws_region)
        self.rds_client = boto3.client('rds', region_name=self.aws_region)
        self.s3_client = boto3.client('s3', region_name=self.aws_region)
        
    def setup_test_environment(self) -> Dict[str, Any]:
        """Set up test environment with unique identifiers"""
        test_id = str(uuid.uuid4())[:8]
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        
        return {
            'test_id': test_id,
            'timestamp': timestamp,
            'test_stack_name': f'{self.test_stack_prefix}-{test_id}',
            'original_state_hash': None,
            'update_iterations': []
        }
    
    def get_stack_template(self) -> str:
        """Get a simplified CloudFormation template for testing idempotency"""
        return json.dumps({
            "AWSTemplateFormatVersion": "2010-09-09",
            "Description": "Idempotency test stack for Pet Clinic CI/CD Pipeline",
            "Parameters": {
                "Environment": {
                    "Type": "String",
                    "Default": "test",
                    "AllowedValues": ["test", "dev", "staging", "prod"]
                },
                "InstanceType": {
                    "Type": "String",
                    "Default": "t3.micro",
                    "AllowedValues": ["t3.micro", "t3.small", "t3.medium"]
                },
                "TestParameter": {
                    "Type": "String",
                    "Default": "initial-value",
                    "Description": "Parameter for testing idempotency"
                }
            },
            "Resources": {
                "TestVPC": {
                    "Type": "AWS::EC2::VPC",
                    "Properties": {
                        "CidrBlock": "10.0.0.0/16",
                        "EnableDnsHostnames": True,
                        "EnableDnsSupport": True,
                        "Tags": [
                            {"Key": "Name", "Value": {"Ref": "AWS::StackName"}},
                            {"Key": "Environment", "Value": {"Ref": "Environment"}},
                            {"Key": "TestParameter", "Value": {"Ref": "TestParameter"}}
                        ]
                    }
                },
                "TestSubnet": {
                    "Type": "AWS::EC2::Subnet",
                    "Properties": {
                        "VpcId": {"Ref": "TestVPC"},
                        "CidrBlock": "10.0.1.0/24",
                        "AvailabilityZone": {"Fn::Select": [0, {"Fn::GetAZs": ""}]},
                        "Tags": [
                            {"Key": "Name", "Value": {"Fn::Sub": "${AWS::StackName}-subnet"}},
                            {"Key": "Environment", "Value": {"Ref": "Environment"}}
                        ]
                    }
                },
                "TestSecurityGroup": {
                    "Type": "AWS::EC2::SecurityGroup",
                    "Properties": {
                        "GroupDescription": "Test security group for idempotency testing",
                        "VpcId": {"Ref": "TestVPC"},
                        "SecurityGroupIngress": [
                            {
                                "IpProtocol": "tcp",
                                "FromPort": 80,
                                "ToPort": 80,
                                "CidrIp": "0.0.0.0/0"
                            }
                        ],
                        "Tags": [
                            {"Key": "Name", "Value": {"Fn::Sub": "${AWS::StackName}-sg"}},
                            {"Key": "Environment", "Value": {"Ref": "Environment"}}
                        ]
                    }
                }
            },
            "Outputs": {
                "VPCId": {
                    "Description": "VPC ID",
                    "Value": {"Ref": "TestVPC"},
                    "Export": {"Name": {"Fn::Sub": "${AWS::StackName}-VPC-ID"}}
                },
                "SubnetId": {
                    "Description": "Subnet ID",
                    "Value": {"Ref": "TestSubnet"},
                    "Export": {"Name": {"Fn::Sub": "${AWS::StackName}-Subnet-ID"}}
                },
                "SecurityGroupId": {
                    "Description": "Security Group ID",
                    "Value": {"Ref": "TestSecurityGroup"},
                    "Export": {"Name": {"Fn::Sub": "${AWS::StackName}-SG-ID"}}
                }
            }
        })
    
    def create_initial_stack(self, test_env: Dict[str, Any], parameters: Dict[str, str]) -> bool:
        """Create the initial CloudFormation stack"""
        try:
            template_body = self.get_stack_template()
            
            # Convert parameters to CloudFormation format
            cf_parameters = [
                {'ParameterKey': key, 'ParameterValue': value}
                for key, value in parameters.items()
            ]
            
            response = self.cloudformation_client.create_stack(
                StackName=test_env['test_stack_name'],
                TemplateBody=template_body,
                Parameters=cf_parameters,
                Capabilities=['CAPABILITY_IAM'],
                Tags=[
                    {'Key': 'TestId', 'Value': test_env['test_id']},
                    {'Key': 'Purpose', 'Value': 'IdempotencyTest'},
                    {'Key': 'Timestamp', 'Value': test_env['timestamp']}
                ]
            )
            
            # Wait for stack creation to complete
            waiter = self.cloudformation_client.get_waiter('stack_create_complete')
            waiter.wait(
                StackName=test_env['test_stack_name'],
                WaiterConfig={'Delay': 15, 'MaxAttempts': 40}  # 10 minutes max
            )
            
            return True
            
        except Exception as e:
            print(f"Error creating initial stack: {e}")
            return False
    
    def capture_stack_state(self, test_env: Dict[str, Any]) -> Dict[str, Any]:
        """Capture the current state of the CloudFormation stack"""
        try:
            # Get stack description
            stack_response = self.cloudformation_client.describe_stacks(
                StackName=test_env['test_stack_name']
            )
            stack = stack_response['Stacks'][0]
            
            # Get stack resources
            resources_response = self.cloudformation_client.describe_stack_resources(
                StackName=test_env['test_stack_name']
            )
            resources = resources_response['StackResources']
            
            # Get stack outputs
            outputs = stack.get('Outputs', [])
            
            # Create state snapshot
            state = {
                'stack_status': stack['StackStatus'],
                'parameters': {param['ParameterKey']: param['ParameterValue'] 
                             for param in stack.get('Parameters', [])},
                'outputs': {output['OutputKey']: output['OutputValue'] 
                           for output in outputs},
                'resources': {
                    resource['LogicalResourceId']: {
                        'type': resource['ResourceType'],
                        'status': resource['ResourceStatus'],
                        'physical_id': resource.get('PhysicalResourceId', '')
                    }
                    for resource in resources
                },
                'tags': {tag['Key']: tag['Value'] for tag in stack.get('Tags', [])},
                'creation_time': stack['CreationTime'].isoformat(),
                'last_updated_time': stack.get('LastUpdatedTime', stack['CreationTime']).isoformat()
            }
            
            # Calculate state hash for comparison
            state_json = json.dumps(state, sort_keys=True, default=str)
            state_hash = hashlib.sha256(state_json.encode('utf-8')).hexdigest()
            state['state_hash'] = state_hash
            
            return state
            
        except Exception as e:
            print(f"Error capturing stack state: {e}")
            return {}
    
    def perform_stack_update(self, test_env: Dict[str, Any], parameters: Dict[str, str], iteration: int) -> Dict[str, Any]:
        """Perform a CloudFormation stack update operation"""
        update_result = {
            'iteration': iteration,
            'update_attempted': False,
            'update_completed': False,
            'no_changes_detected': False,
            'error_occurred': False,
            'error_message': None,
            'duration': 0
        }
        
        start_time = time.time()
        
        try:
            template_body = self.get_stack_template()
            
            # Convert parameters to CloudFormation format
            cf_parameters = [
                {'ParameterKey': key, 'ParameterValue': value}
                for key, value in parameters.items()
            ]
            
            # Attempt stack update
            response = self.cloudformation_client.update_stack(
                StackName=test_env['test_stack_name'],
                TemplateBody=template_body,
                Parameters=cf_parameters,
                Capabilities=['CAPABILITY_IAM']
            )
            
            update_result['update_attempted'] = True
            
            # Wait for update to complete
            waiter = self.cloudformation_client.get_waiter('stack_update_complete')
            waiter.wait(
                StackName=test_env['test_stack_name'],
                WaiterConfig={'Delay': 15, 'MaxAttempts': 40}  # 10 minutes max
            )
            
            update_result['update_completed'] = True
            
        except ClientError as e:
            error_code = e.response['Error']['Code']
            error_message = e.response['Error']['Message']
            
            if error_code == 'ValidationError' and 'No updates are to be performed' in error_message:
                # This is expected for idempotent operations
                update_result['no_changes_detected'] = True
                update_result['update_completed'] = True
            else:
                update_result['error_occurred'] = True
                update_result['error_message'] = f"{error_code}: {error_message}"
                
        except Exception as e:
            update_result['error_occurred'] = True
            update_result['error_message'] = str(e)
        
        update_result['duration'] = time.time() - start_time
        return update_result
    
    def verify_idempotency(self, test_env: Dict[str, Any], states: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Verify that multiple update operations produced idempotent results"""
        verification = {
            'states_captured': len(states),
            'all_states_identical': False,
            'resource_ids_consistent': False,
            'outputs_consistent': False,
            'parameters_consistent': False,
            'state_hashes': [],
            'differences_found': []
        }
        
        if len(states) < 2:
            return verification
        
        # Extract state hashes
        verification['state_hashes'] = [state.get('state_hash', '') for state in states]
        
        # Compare all states for idempotency
        reference_state = states[0]
        all_identical = True
        
        for i, state in enumerate(states[1:], 1):
            # Compare resource physical IDs (should remain the same)
            for resource_id, resource_info in reference_state['resources'].items():
                if resource_id in state['resources']:
                    ref_physical_id = resource_info['physical_id']
                    curr_physical_id = state['resources'][resource_id]['physical_id']
                    
                    if ref_physical_id != curr_physical_id:
                        all_identical = False
                        verification['differences_found'].append(
                            f"Resource {resource_id} physical ID changed: {ref_physical_id} -> {curr_physical_id}"
                        )
            
            # Compare outputs (should remain the same)
            for output_key, output_value in reference_state['outputs'].items():
                if output_key in state['outputs']:
                    if output_value != state['outputs'][output_key]:
                        all_identical = False
                        verification['differences_found'].append(
                            f"Output {output_key} changed: {output_value} -> {state['outputs'][output_key]}"
                        )
            
            # Compare parameters (should remain the same for idempotent updates)
            for param_key, param_value in reference_state['parameters'].items():
                if param_key in state['parameters']:
                    if param_value != state['parameters'][param_key]:
                        all_identical = False
                        verification['differences_found'].append(
                            f"Parameter {param_key} changed: {param_value} -> {state['parameters'][param_key]}"
                        )
        
        verification['all_states_identical'] = all_identical
        verification['resource_ids_consistent'] = len([d for d in verification['differences_found'] if 'physical ID' in d]) == 0
        verification['outputs_consistent'] = len([d for d in verification['differences_found'] if 'Output' in d]) == 0
        verification['parameters_consistent'] = len([d for d in verification['differences_found'] if 'Parameter' in d]) == 0
        
        return verification
    
    def cleanup_test_stack(self, test_env: Dict[str, Any]):
        """Clean up the test CloudFormation stack"""
        try:
            self.cloudformation_client.delete_stack(
                StackName=test_env['test_stack_name']
            )
            
            # Wait for deletion to complete
            waiter = self.cloudformation_client.get_waiter('stack_delete_complete')
            waiter.wait(
                StackName=test_env['test_stack_name'],
                WaiterConfig={'Delay': 15, 'MaxAttempts': 40}  # 10 minutes max
            )
            
        except Exception as e:
            print(f"Error cleaning up test stack: {e}")


@pytest.fixture
def infrastructure_idempotency_test():
    """Fixture for infrastructure update idempotency test"""
    return InfrastructureUpdateIdempotencyTest()


@given(
    environment=st.sampled_from(['test', 'dev']),
    instance_type=st.sampled_from(['t3.micro', 't3.small']),
    test_parameter=st.text(min_size=5, max_size=20, alphabet=st.characters(whitelist_categories=('Lu', 'Ll', 'Nd', 'Pd')))
)
@settings(max_examples=3, deadline=1800000)  # 30 minute timeout per test
def test_infrastructure_update_idempotency(infrastructure_idempotency_test, environment, instance_type, test_parameter):
    """
    **Validates: Requirements 2.4**
    
    Property 4: Infrastructure Update Idempotency
    For any CloudFormation stack update operation, running the same update multiple times 
    should produce the same infrastructure state without data loss
    """
    # Set up test environment
    test_env = infrastructure_idempotency_test.setup_test_environment()
    
    # Define stack parameters
    parameters = {
        'Environment': environment,
        'InstanceType': instance_type,
        'TestParameter': test_parameter
    }
    
    try:
        # Step 1: Create initial CloudFormation stack
        print(f"Creating initial stack {test_env['test_stack_name']} with parameters: {parameters}")
        stack_created = infrastructure_idempotency_test.create_initial_stack(test_env, parameters)
        assert stack_created, "Failed to create initial CloudFormation stack"
        
        # Step 2: Capture initial state
        initial_state = infrastructure_idempotency_test.capture_stack_state(test_env)
        assert initial_state, "Failed to capture initial stack state"
        test_env['original_state_hash'] = initial_state.get('state_hash')
        
        print(f"Initial stack state captured with hash: {test_env['original_state_hash'][:8]}")
        
        # Step 3: Perform multiple identical update operations (idempotency test)
        states = [initial_state]
        update_results = []
        
        for iteration in range(1, 4):  # Perform 3 identical updates
            print(f"Performing update iteration {iteration}...")
            
            # Perform the same update operation
            update_result = infrastructure_idempotency_test.perform_stack_update(
                test_env, parameters, iteration
            )
            update_results.append(update_result)
            
            # Capture state after update
            post_update_state = infrastructure_idempotency_test.capture_stack_state(test_env)
            if post_update_state:
                states.append(post_update_state)
            
            # Verify update completed successfully (either with changes or no changes detected)
            assert update_result['update_completed'], (
                f"Update iteration {iteration} failed: {update_result.get('error_message', 'Unknown error')}"
            )
            
            print(f"Update iteration {iteration} completed in {update_result['duration']:.1f}s")
            if update_result['no_changes_detected']:
                print(f"  - No changes detected (expected for idempotent operation)")
        
        # Step 4: Verify idempotency across all states
        verification = infrastructure_idempotency_test.verify_idempotency(test_env, states)
        
        # Assert idempotency requirements
        assert verification['states_captured'] >= 2, "Insufficient states captured for idempotency verification"
        
        # Core idempotency assertions
        assert verification['resource_ids_consistent'], (
            f"Resource IDs not consistent across updates: {verification['differences_found']}"
        )
        
        assert verification['outputs_consistent'], (
            f"Stack outputs not consistent across updates: {verification['differences_found']}"
        )
        
        assert verification['parameters_consistent'], (
            f"Stack parameters not consistent across updates: {verification['differences_found']}"
        )
        
        # Verify no data loss occurred
        final_state = states[-1]
        assert len(final_state['resources']) == len(initial_state['resources']), "Resources were lost during updates"
        assert len(final_state['outputs']) == len(initial_state['outputs']), "Outputs were lost during updates"
        
        print(f"✓ Property 4 validated: Infrastructure updates are idempotent")
        print(f"  - {len(states)} states captured and verified")
        print(f"  - {len(update_results)} update operations performed")
        print(f"  - Resource IDs consistent: {verification['resource_ids_consistent']}")
        print(f"  - Outputs consistent: {verification['outputs_consistent']}")
        print(f"  - Parameters consistent: {verification['parameters_consistent']}")
        
        # Log update operation results
        no_change_updates = sum(1 for result in update_results if result['no_changes_detected'])
        print(f"  - {no_change_updates}/{len(update_results)} updates detected no changes (expected for idempotency)")
        
    finally:
        # Cleanup test stack
        print("Cleaning up test stack...")
        infrastructure_idempotency_test.cleanup_test_stack(test_env)


def test_infrastructure_update_idempotency_manual():
    """
    Manual test case for infrastructure update idempotency
    This test can be run independently to verify idempotency behavior
    """
    test_instance = InfrastructureUpdateIdempotencyTest()
    test_env = test_instance.setup_test_environment()
    
    # Use fixed parameters for manual testing
    parameters = {
        'Environment': 'test',
        'InstanceType': 't3.micro',
        'TestParameter': 'manual-test-value'
    }
    
    try:
        # Create initial stack
        print("Creating manual test stack...")
        stack_created = test_instance.create_initial_stack(test_env, parameters)
        assert stack_created, "Failed to create manual test stack"
        
        # Capture initial state
        initial_state = test_instance.capture_stack_state(test_env)
        print(f"Initial state hash: {initial_state.get('state_hash', 'N/A')[:8]}")
        
        # Perform two identical updates
        states = [initial_state]
        for i in range(1, 3):
            print(f"Performing manual update {i}...")
            update_result = test_instance.perform_stack_update(test_env, parameters, i)
            
            if update_result['update_completed']:
                post_state = test_instance.capture_stack_state(test_env)
                states.append(post_state)
                print(f"Update {i} completed, state hash: {post_state.get('state_hash', 'N/A')[:8]}")
            else:
                print(f"Update {i} failed: {update_result.get('error_message', 'Unknown')}")
        
        # Verify idempotency
        verification = test_instance.verify_idempotency(test_env, states)
        print(f"Idempotency verification: {verification['all_states_identical']}")
        
        if verification['differences_found']:
            print("Differences found:")
            for diff in verification['differences_found']:
                print(f"  - {diff}")
        
        assert verification['resource_ids_consistent'], "Manual test failed - resource IDs not consistent"
        assert verification['outputs_consistent'], "Manual test failed - outputs not consistent"
        
    finally:
        test_instance.cleanup_test_stack(test_env)


if __name__ == '__main__':
    # Run the tests
    import sys
    import os
    pytest.main([sys.argv[0], '-v'])