#!/bin/bash

# Pet Clinic CI/CD Pipeline - Infrastructure Deployment Script
# This script deploys only the infrastructure components

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
STACK_NAME="${STACK_NAME:-petclinic-production}"
REGION="${AWS_REGION:-us-east-1}"
ENVIRONMENT="${ENVIRONMENT:-production}"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites for infrastructure deployment..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    fi
    
    # Check required parameters
    if [[ -z "$DB_PASSWORD" ]]; then
        print_error "DB_PASSWORD environment variable is required."
        print_error "Set it with: export DB_PASSWORD='your-secure-password'"
        exit 1
    fi
    
    if [[ -z "$JENKINS_ADMIN_PASSWORD" ]]; then
        print_error "JENKINS_ADMIN_PASSWORD environment variable is required."
        print_error "Set it with: export JENKINS_ADMIN_PASSWORD='your-secure-password'"
        exit 1
    fi
    
    print_success "Prerequisites check completed"
}

# Function to validate templates
validate_templates() {
    print_status "Validating CloudFormation templates..."
    
    local templates=(
        "cloudformation/master-stack.yaml"
        "cloudformation/network.yaml"
        "cloudformation/iam.yaml"
        "cloudformation/database.yaml"
        "cloudformation/storage.yaml"
        "cloudformation/compute.yaml"
    )
    
    for template in "${templates[@]}"; do
        if [[ -f "$template" ]]; then
            print_status "Validating $(basename "$template")..."
            if ! aws cloudformation validate-template \
                --template-body file://"$template" \
                --region "$REGION" > /dev/null 2>&1; then
                print_error "Template validation failed for $template"
                exit 1
            fi
        else
            print_error "Template not found: $template"
            exit 1
        fi
    done
    
    print_success "All templates validated successfully"
}

# Function to create parameters file
create_parameters() {
    print_status "Creating deployment parameters..."
    
    # Generate random suffix for unique resource names
    RANDOM_SUFFIX=$(openssl rand -hex 4)
    
    # Create parameters file
    cat > /tmp/infrastructure-parameters.json << EOF
[
    {
        "ParameterKey": "Environment",
        "ParameterValue": "$ENVIRONMENT"
    },
    {
        "ParameterKey": "InstanceType",
        "ParameterValue": "${INSTANCE_TYPE:-t3.medium}"
    },
    {
        "ParameterKey": "DBInstanceClass",
        "ParameterValue": "${DB_INSTANCE_CLASS:-db.t3.micro}"
    },
    {
        "ParameterKey": "DBPassword",
        "ParameterValue": "$DB_PASSWORD"
    },
    {
        "ParameterKey": "JenkinsAdminPassword",
        "ParameterValue": "$JENKINS_ADMIN_PASSWORD"
    },
    {
        "ParameterKey": "RandomSuffix",
        "ParameterValue": "$RANDOM_SUFFIX"
    },
    {
        "ParameterKey": "KeyPairName",
        "ParameterValue": "${KEY_PAIR_NAME:-}"
    },
    {
        "ParameterKey": "AllowedCIDR",
        "ParameterValue": "${ALLOWED_CIDR:-0.0.0.0/0}"
    }
]
EOF
    
    print_success "Parameters file created with random suffix: $RANDOM_SUFFIX"
}

# Function to check if stack exists
stack_exists() {
    aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" &> /dev/null
}

# Function to get stack status
get_stack_status() {
    aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].StackStatus' \
        --output text 2>/dev/null || echo "DOES_NOT_EXIST"
}

# Function to deploy stack
deploy_stack() {
    local stack_status
    stack_status=$(get_stack_status)
    
    case "$stack_status" in
        "DOES_NOT_EXIST")
            print_status "Creating new infrastructure stack..."
            aws cloudformation create-stack \
                --stack-name "$STACK_NAME" \
                --template-body file://cloudformation/master-stack.yaml \
                --parameters file:///tmp/infrastructure-parameters.json \
                --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
                --region "$REGION" \
                --tags Key=Environment,Value="$ENVIRONMENT" \
                       Key=Project,Value=petclinic \
                       Key=ManagedBy,Value=cloudformation
            
            print_status "Waiting for stack creation to complete (this may take 15-20 minutes)..."
            if aws cloudformation wait stack-create-complete \
                --stack-name "$STACK_NAME" \
                --region "$REGION"; then
                print_success "Stack creation completed successfully"
            else
                print_error "Stack creation failed"
                show_stack_events
                exit 1
            fi
            ;;
            
        "CREATE_COMPLETE"|"UPDATE_COMPLETE")
            print_warning "Stack already exists and is in a stable state"
            read -p "Do you want to update the stack? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_status "Updating existing infrastructure stack..."
                if aws cloudformation update-stack \
                    --stack-name "$STACK_NAME" \
                    --template-body file://cloudformation/master-stack.yaml \
                    --parameters file:///tmp/infrastructure-parameters.json \
                    --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
                    --region "$REGION" 2>/dev/null; then
                    
                    print_status "Waiting for stack update to complete..."
                    if aws cloudformation wait stack-update-complete \
                        --stack-name "$STACK_NAME" \
                        --region "$REGION"; then
                        print_success "Stack update completed successfully"
                    else
                        print_error "Stack update failed"
                        show_stack_events
                        exit 1
                    fi
                else
                    print_warning "No updates to perform or update failed"
                fi
            else
                print_status "Skipping stack update"
            fi
            ;;
            
        "CREATE_IN_PROGRESS"|"UPDATE_IN_PROGRESS")
            print_warning "Stack is currently being modified. Please wait for it to complete."
            exit 1
            ;;
            
        "CREATE_FAILED"|"UPDATE_FAILED"|"ROLLBACK_COMPLETE"|"ROLLBACK_FAILED")
            print_error "Stack is in a failed state: $stack_status"
            print_error "Please delete the stack and try again, or check the stack events for details"
            show_stack_events
            exit 1
            ;;
            
        *)
            print_error "Unknown stack status: $stack_status"
            exit 1
            ;;
    esac
}

# Function to show stack events for debugging
show_stack_events() {
    print_status "Recent stack events:"
    aws cloudformation describe-stack-events \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'StackEvents[0:10].{Time:Timestamp,Status:ResourceStatus,Type:ResourceType,Reason:ResourceStatusReason}' \
        --output table 2>/dev/null || true
}

# Function to get and display stack outputs
get_stack_outputs() {
    print_status "Retrieving stack outputs..."
    
    local outputs
    outputs=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs' \
        --output json 2>/dev/null)
    
    if [[ "$outputs" != "null" && "$outputs" != "[]" ]]; then
        echo -e "\n${GREEN}Stack Outputs:${NC}"
        echo "$outputs" | jq -r '.[] | "\(.OutputKey): \(.OutputValue)"' | while read -r line; do
            echo -e "${BLUE}  $line${NC}"
        done
        echo
    else
        print_warning "No stack outputs available"
    fi
}

# Function to verify infrastructure
verify_infrastructure() {
    print_status "Verifying infrastructure deployment..."
    
    # Check VPC
    local vpc_id
    vpc_id=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`VPCId`].OutputValue' \
        --output text 2>/dev/null)
    
    if [[ -n "$vpc_id" && "$vpc_id" != "None" ]]; then
        print_success "VPC created: $vpc_id"
    else
        print_warning "VPC ID not found in outputs"
    fi
    
    # Check RDS
    local db_endpoint
    db_endpoint=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
        --output text 2>/dev/null)
    
    if [[ -n "$db_endpoint" && "$db_endpoint" != "None" ]]; then
        print_success "Database created: $db_endpoint"
    else
        print_warning "Database endpoint not found in outputs"
    fi
    
    # Check Jenkins URL
    local jenkins_url
    jenkins_url=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
        --output text 2>/dev/null)
    
    if [[ -n "$jenkins_url" && "$jenkins_url" != "None" ]]; then
        print_success "Jenkins server will be available at: $jenkins_url"
    else
        print_warning "Jenkins URL not found in outputs"
    fi
    
    # Check Application Load Balancer
    local alb_url
    alb_url=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`ApplicationURL`].OutputValue' \
        --output text 2>/dev/null)
    
    if [[ -n "$alb_url" && "$alb_url" != "None" ]]; then
        print_success "Application Load Balancer: $alb_url"
    else
        print_warning "Application URL not found in outputs"
    fi
}

# Function to run infrastructure tests
run_infrastructure_tests() {
    print_status "Running infrastructure validation tests..."
    
    if [[ -f "tests/cloudformation/test-templates.py" ]]; then
        if command -v python3 &> /dev/null; then
            print_status "Running CloudFormation template tests..."
            cd tests/cloudformation
            python3 test-templates.py
            cd ../..
            print_success "CloudFormation tests passed"
        else
            print_warning "Python3 not available, skipping template tests"
        fi
    else
        print_warning "Infrastructure tests not found, skipping"
    fi
}

# Function to display next steps
display_next_steps() {
    echo
    print_success "=== INFRASTRUCTURE DEPLOYMENT COMPLETED ==="
    echo
    echo -e "${BLUE}Stack Name:${NC} $STACK_NAME"
    echo -e "${BLUE}Region:${NC} $REGION"
    echo -e "${BLUE}Environment:${NC} $ENVIRONMENT"
    echo
    
    # Get key outputs for display
    local jenkins_url alb_url db_endpoint
    jenkins_url=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
        --output text 2>/dev/null)
    
    alb_url=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`ApplicationURL`].OutputValue' \
        --output text 2>/dev/null)
    
    db_endpoint=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
        --output text 2>/dev/null)
    
    echo -e "${GREEN}Next Steps:${NC}"
    echo "1. Wait for all services to initialize (5-10 minutes)"
    echo
    echo "2. Access Jenkins at: $jenkins_url"
    echo "   - Initial setup may take a few minutes"
    echo "   - Username: admin"
    echo "   - Password: $JENKINS_ADMIN_PASSWORD"
    echo
    echo "3. Build and deploy the application:"
    echo "   ./scripts/deploy-application.sh"
    echo
    echo "4. Configure monitoring and alerting:"
    echo "   ./scripts/setup-monitoring.sh"
    echo
    echo "5. Set up backup system:"
    echo "   ./scripts/setup-backup.sh"
    echo
    echo -e "${YELLOW}Important URLs:${NC}"
    echo "- Jenkins: $jenkins_url"
    echo "- Application: $alb_url (after app deployment)"
    echo "- Database: $db_endpoint"
    echo
    echo -e "${YELLOW}Monitoring:${NC}"
    echo "- CloudFormation Console: https://console.aws.amazon.com/cloudformation/"
    echo "- EC2 Console: https://console.aws.amazon.com/ec2/"
    echo "- RDS Console: https://console.aws.amazon.com/rds/"
    echo
}

# Function to cleanup temporary files
cleanup() {
    rm -f /tmp/infrastructure-parameters.json
}

# Main function
main() {
    echo -e "${BLUE}===========================================${NC}"
    echo -e "${BLUE}Pet Clinic Infrastructure Deployment${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo
    
    # Set trap for cleanup
    trap cleanup EXIT
    
    # Run deployment steps
    check_prerequisites
    validate_templates
    create_parameters
    deploy_stack
    get_stack_outputs
    verify_infrastructure
    run_infrastructure_tests
    display_next_steps
    
    print_success "Infrastructure deployment completed successfully!"
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi