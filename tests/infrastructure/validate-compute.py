#!/usr/bin/env python3
"""
Simple validation script for compute infrastructure template
"""

import re
import sys

def validate_compute_template():
    """Validate the compute CloudFormation template"""
    try:
        with open('../../cloudformation/compute.yaml', 'r') as f:
            content = f.read()
        
        print("✓ Template file readable")
        
        # Check for required sections
        required_sections = ['AWSTemplateFormatVersion', 'Description', 'Parameters', 'Resources', 'Outputs']
        for section in required_sections:
            if section + ':' in content:
                print(f"✓ {section} section found")
            else:
                print(f"✗ {section} section missing")
                return False
        
        # Check for required resources
        required_resources = [
            'ApplicationLoadBalancer',
            'FrontendTargetGroup', 
            'FrontendAutoScalingGroup',
            'BackendAutoScalingGroup',
            'JenkinsInstance'
        ]
        
        for resource in required_resources:
            if resource + ':' in content:
                print(f"✓ Resource {resource} found")
            else:
                print(f"✗ Resource {resource} missing")
                return False
        
        # Check for EFS mounting in user data
        if 'amazon-efs-utils' in content and 'mount' in content:
            print("✓ EFS mounting configuration found")
        else:
            print("✗ EFS mounting configuration missing")
            return False
        
        # Check for Java/Maven installation references
        if 'install-java.sh' in content and 'install-maven.sh' in content:
            print("✓ Java/Maven installation scripts referenced")
        else:
            print("✗ Java/Maven installation scripts not referenced")
            return False
        
        # Check for health check configuration
        if 'HealthCheckPath' in content and '/actuator/health' in content:
            print("✓ Health check configuration found")
        else:
            print("✗ Health check configuration missing")
            return False
        
        # Check for Auto Scaling configuration
        if 'MinSize' in content and 'MaxSize' in content and 'DesiredCapacity' in content:
            print("✓ Auto Scaling configuration found")
        else:
            print("✗ Auto Scaling configuration missing")
            return False
        
        print("\n✓ All validation checks passed!")
        return True
        
    except Exception as e:
        print(f"✗ Validation failed: {e}")
        return False

if __name__ == '__main__':
    success = validate_compute_template()
    sys.exit(0 if success else 1)