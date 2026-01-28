#!/bin/bash

# Pet Clinic CI/CD Pipeline - Quick Deployment Script
# This script provides a one-command deployment of the entire system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
STACK_NAME="petclinic-production"
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
    print_status "Checking prerequisites..."
    
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
    
    # Check required environment variables
    if [[ -z "$GITHUB_TOKEN" ]]; then
        print_error "GITHUB_TOKEN environment variable is required."
        exit 1
    fi
    
    if [[ -z "$JENKINS_ADMIN_PASSWORD" ]]; then
        print_error "JENKINS_ADMIN_PASSWORD environment variable is required."
        exit 1
    fi
    
    if [[ -z "$DB_PASSWORD" ]]; then
        print_error "DB_PASSWORD environment variable is required."
        exit 1
    fi
    
    # Check Java and Maven
    if ! command -v java &> /dev/null; then
        print_warning "Java is not installed locally. This is needed for building the application."
    fi
    
    if ! command -v mvn &> /dev/null; then
        print_warning "Maven is not installed locally. This is needed for building the application."
    fi
    
    print_success "Prerequisites check completed"
}

# Function to validate CloudFormation templates
validate_templates() {
    print_status "Validating CloudFormation templates..."
    
    for template in cloudformation/*.yaml; do
        if [[ -f "$template" ]]; then
            print_status "Validating $(basename "$template")..."
            aws cloudformation validate-template \
                --template-body file://"$template" \
                --region "$REGION" > /dev/null
        fi
    done
    
    print_success "All templates validated successfully"
}

# Function to create parameter file
create_parameters() {
    print_status "Creating deployment parameters..."
    
    # Generate random suffix for unique resource names
    RANDOM_SUFFIX=$(openssl rand -hex 4)
    
    cat > /tmp/deployment-parameters.json << EOF
[
    {
        "ParameterKey": "Environment",
        "ParameterValue": "$ENVIRONMENT"
    },
    {
        "ParameterKey": "InstanceType",
        "ParameterValue": "t3.medium"
    },
    {
        "ParameterKey": "DBInstanceClass",
        "ParameterValue": "db.t3.micro"
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
    }
]
EOF
    
    print_success "Parameters file created"
}

# Function to deploy infrastructure
deploy_infrastructure() {
    print_status "Deploying infrastructure stack..."
    
    # Check if stack already exists
    if aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" &> /dev/null; then
        print_warning "Stack $STACK_NAME already exists. Updating..."
        aws cloudformation update-stack \
            --stack-name "$STACK_NAME" \
            --template-body file://cloudformation/master-stack.yaml \
            --parameters file:///tmp/deployment-parameters.json \
            --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
            --region "$REGION"
        
        print_status "Waiting for stack update to complete..."
        aws cloudformation wait stack-update-complete \
            --stack-name "$STACK_NAME" \
            --region "$REGION"
    else
        print_status "Creating new stack..."
        aws cloudformation create-stack \
            --stack-name "$STACK_NAME" \
            --template-body file://cloudformation/master-stack.yaml \
            --parameters file:///tmp/deployment-parameters.json \
            --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
            --region "$REGION" \
            --tags Key=Environment,Value="$ENVIRONMENT" Key=Project,Value=petclinic
        
        print_status "Waiting for stack creation to complete (this may take 15-20 minutes)..."
        aws cloudformation wait stack-create-complete \
            --stack-name "$STACK_NAME" \
            --region "$REGION"
    fi
    
    print_success "Infrastructure deployment completed"
}

# Function to get stack outputs
get_stack_outputs() {
    print_status "Retrieving stack outputs..."
    
    # Get important outputs
    JENKINS_URL=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' \
        --output text)
    
    ALB_URL=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`ApplicationURL`].OutputValue' \
        --output text)
    
    DB_ENDPOINT=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
        --output text)
    
    ARTIFACT_BUCKET=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`ArtifactBucket`].OutputValue' \
        --output text)
    
    # Export for use in other functions
    export JENKINS_URL ALB_URL DB_ENDPOINT ARTIFACT_BUCKET
    
    print_success "Stack outputs retrieved"
}

# Function to build application
build_application() {
    print_status "Building application..."
    
    if command -v mvn &> /dev/null && command -v java &> /dev/null; then
        cd pet-clinic-app
        
        print_status "Running Maven build..."
        mvn clean package -DskipTests -q
        
        print_status "Uploading artifacts to S3..."
        aws s3 cp pet-clinic-backend/target/pet-clinic-backend-1.0.0.jar \
            s3://"$ARTIFACT_BUCKET"/artifacts/backend/ --region "$REGION"
        
        aws s3 cp pet-clinic-frontend/target/pet-clinic-frontend-1.0.0.jar \
            s3://"$ARTIFACT_BUCKET"/artifacts/frontend/ --region "$REGION"
        
        cd ..
        print_success "Application built and uploaded"
    else
        print_warning "Maven or Java not available locally. Skipping application build."
        print_warning "You'll need to build and deploy the application manually later."
    fi
}

# Function to wait for Jenkins
wait_for_jenkins() {
    print_status "Waiting for Jenkins to be ready..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$JENKINS_URL/login" > /dev/null 2>&1; then
            print_success "Jenkins is ready!"
            return 0
        fi
        
        print_status "Attempt $attempt/$max_attempts: Jenkins not ready yet, waiting 30 seconds..."
        sleep 30
        ((attempt++))
    done
    
    print_error "Jenkins failed to start within expected time"
    return 1
}

# Function to configure GitHub webhooks
configure_webhooks() {
    print_status "Configuring GitHub webhooks..."
    
    if [[ -n "$GITHUB_TOKEN" ]] && [[ -f "jenkins-pipelines/setup-webhooks.sh" ]]; then
        export JENKINS_URL
        export GITHUB_REPO="${GITHUB_REPO:-your-org/pet-clinic-cicd-pipeline}"
        
        chmod +x jenkins-pipelines/setup-webhooks.sh
        ./jenkins-pipelines/setup-webhooks.sh
        
        print_success "GitHub webhooks configured"
    else
        print_warning "GitHub token not available or webhook script not found. Skipping webhook configuration."
    fi
}

# Function to run health checks
run_health_checks() {
    print_status "Running health checks..."
    
    # Check Jenkins
    if curl -f "$JENKINS_URL/login" > /dev/null 2>&1; then
        print_success "Jenkins health check passed"
    else
        print_warning "Jenkins health check failed"
    fi
    
    # Check application (may not be deployed yet)
    if curl -f "$ALB_URL" > /dev/null 2>&1; then
        print_success "Application health check passed"
    else
        print_warning "Application health check failed (this is expected if application isn't deployed yet)"
    fi
    
    # Check database connectivity (from a bastion or instance with access)
    print_status "Database endpoint: $DB_ENDPOINT"
}

# Function to display deployment summary
display_summary() {
    print_success "=== DEPLOYMENT COMPLETED ==="
    echo
    echo -e "${BLUE}Stack Name:${NC} $STACK_NAME"
    echo -e "${BLUE}Region:${NC} $REGION"
    echo -e "${BLUE}Environment:${NC} $ENVIRONMENT"
    echo
    echo -e "${BLUE}Jenkins URL:${NC} $JENKINS_URL"
    echo -e "${BLUE}Application URL:${NC} $ALB_URL"
    echo -e "${BLUE}Database Endpoint:${NC} $DB_ENDPOINT"
    echo
    echo -e "${GREEN}Next Steps:${NC}"
    echo "1. Access Jenkins at: $JENKINS_URL"
    echo "   - Username: admin"
    echo "   - Password: $JENKINS_ADMIN_PASSWORD"
    echo
    echo "2. Configure Jenkins jobs and run initial deployment"
    echo
    echo "3. Access application at: $ALB_URL (once deployed)"
    echo
    echo "4. Monitor deployment progress in AWS CloudFormation console"
    echo
    echo -e "${YELLOW}Note:${NC} The application needs to be deployed through Jenkins pipeline"
    echo "or manually using the deployment scripts in the deployment-scripts/ directory."
    echo
}

# Function to cleanup on error
cleanup_on_error() {
    print_error "Deployment failed. Cleaning up temporary files..."
    rm -f /tmp/deployment-parameters.json
    exit 1
}

# Main deployment function
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Pet Clinic CI/CD Pipeline Quick Deploy${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo
    
    # Set trap for cleanup on error
    trap cleanup_on_error ERR
    
    # Run deployment steps
    check_prerequisites
    validate_templates
    create_parameters
    deploy_infrastructure
    get_stack_outputs
    build_application
    wait_for_jenkins
    configure_webhooks
    run_health_checks
    display_summary
    
    # Cleanup
    rm -f /tmp/deployment-parameters.json
    
    print_success "Quick deployment completed successfully!"
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi