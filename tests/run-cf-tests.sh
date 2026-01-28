#!/bin/bash
# CloudFormation template validation test runner

set -e

echo "Setting up test environment..."

# Install Python dependencies
pip install -r requirements.txt

echo "Running CloudFormation template validation tests..."

# Run template syntax validation
echo "1. Testing template syntax and structure..."
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_master_stack_template_syntax -v
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_network_stack_template_syntax -v
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_iam_stack_template_syntax -v

# Run parameter validation
echo "2. Testing parameter definitions..."
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_master_stack_parameters -v

# Run dependency validation
echo "3. Testing nested stack dependencies..."
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_nested_stack_dependencies -v

# Run security validation
echo "4. Testing security configurations..."
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_security_groups_configuration -v
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_iam_policies_least_privilege -v

# Run naming convention tests
echo "5. Testing naming conventions..."
python -m pytest cloudformation/test-templates.py::TestCloudFormationTemplates::test_resource_naming_conventions -v

# Run all tests together
echo "6. Running complete test suite..."
python -m pytest cloudformation/test-templates.py -v

echo "CloudFormation template validation tests completed!"

# Optional: Run cfn-lint for additional validation
if command -v cfn-lint &> /dev/null; then
    echo "Running cfn-lint validation..."
    cfn-lint ../cloudformation/*.yaml
else
    echo "cfn-lint not found, skipping additional validation"
fi