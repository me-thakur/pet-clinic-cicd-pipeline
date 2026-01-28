#!/bin/bash

# Pet Clinic CI/CD Pipeline - Test Runner Script
# This script runs various types of tests for the project

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_RESULTS_DIR="$PROJECT_DIR/test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

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

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS] [TEST_CATEGORY]"
    echo
    echo "Test Categories:"
    echo "  unit              Run unit tests only"
    echo "  integration       Run integration tests only"
    echo "  property-based    Run property-based tests only"
    echo "  security          Run security tests only"
    echo "  infrastructure    Run infrastructure tests only"
    echo "  all               Run all tests (default)"
    echo
    echo "Options:"
    echo "  -h, --help        Show this help message"
    echo "  -v, --verbose     Enable verbose output"
    echo "  -f, --fast        Run tests with reduced iterations for faster execution"
    echo "  -c, --coverage    Generate code coverage reports"
    echo "  -r, --report      Generate detailed test reports"
    echo "  --parallel        Run tests in parallel where possible"
    echo "  --skip-build      Skip building the application before tests"
    echo
    echo "Examples:"
    echo "  $0                    # Run all tests"
    echo "  $0 unit               # Run only unit tests"
    echo "  $0 --fast property-based  # Run property tests with reduced iterations"
    echo "  $0 -c -r all          # Run all tests with coverage and reports"
}

# Function to parse command line arguments
parse_args() {
    VERBOSE=false
    FAST=false
    COVERAGE=false
    REPORT=false
    PARALLEL=false
    SKIP_BUILD=false
    TEST_CATEGORY="all"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -f|--fast)
                FAST=true
                shift
                ;;
            -c|--coverage)
                COVERAGE=true
                shift
                ;;
            -r|--report)
                REPORT=true
                shift
                ;;
            --parallel)
                PARALLEL=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            unit|integration|property-based|security|infrastructure|all)
                TEST_CATEGORY="$1"
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

# Function to setup test environment
setup_test_environment() {
    print_status "Setting up test environment..."
    
    # Create test results directory
    mkdir -p "$TEST_RESULTS_DIR"
    
    # Set environment variables for testing
    export SPRING_PROFILES_ACTIVE=test
    export TEST_RESULTS_DIR="$TEST_RESULTS_DIR"
    
    if [[ "$FAST" == "true" ]]; then
        export JQWIK_TRIES_DEFAULT=3
        export HYPOTHESIS_MAX_EXAMPLES=10
        print_status "Fast mode enabled: Reduced test iterations"
    fi
    
    if [[ "$VERBOSE" == "true" ]]; then
        export MAVEN_OPTS="$MAVEN_OPTS -X"
        print_status "Verbose mode enabled"
    fi
    
    print_success "Test environment setup completed"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking test prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed"
        exit 1
    fi
    
    # Check Python for infrastructure tests
    if ! command -v python3 &> /dev/null; then
        print_warning "Python3 not found. Infrastructure tests may not run."
    fi
    
    # Check database connectivity for integration tests
    if [[ "$TEST_CATEGORY" == "integration" || "$TEST_CATEGORY" == "all" ]]; then
        if command -v docker &> /dev/null && docker ps | grep -q petclinic-mysql; then
            print_success "Database available via Docker"
        elif command -v mysql &> /dev/null; then
            if mysql -u petclinic -ppetclinic123 -e "SELECT 1" petclinic_test &> /dev/null; then
                print_success "Database connection verified"
            else
                print_warning "Database connection failed. Integration tests may fail."
            fi
        else
            print_warning "No database found. Integration tests may fail."
        fi
    fi
    
    print_success "Prerequisites check completed"
}

# Function to build application
build_application() {
    if [[ "$SKIP_BUILD" == "true" ]]; then
        print_status "Skipping application build"
        return 0
    fi
    
    print_status "Building application..."
    
    cd "$PROJECT_DIR/pet-clinic-app"
    
    local maven_args="-q"
    if [[ "$VERBOSE" == "true" ]]; then
        maven_args=""
    fi
    
    if [[ "$PARALLEL" == "true" ]]; then
        maven_args="$maven_args -T 1C"
    fi
    
    mvn clean compile $maven_args
    
    print_success "Application build completed"
}

# Function to run unit tests
run_unit_tests() {
    print_status "Running unit tests..."
    
    cd "$PROJECT_DIR/pet-clinic-app"
    
    local maven_args="test"
    local test_pattern="*Test"
    
    if [[ "$VERBOSE" == "true" ]]; then
        maven_args="$maven_args -X"
    else
        maven_args="$maven_args -q"
    fi
    
    if [[ "$COVERAGE" == "true" ]]; then
        maven_args="$maven_args jacoco:report"
    fi
    
    if [[ "$PARALLEL" == "true" ]]; then
        maven_args="$maven_args -T 1C"
    fi
    
    # Run unit tests
    mvn $maven_args -Dtest="$test_pattern"
    
    # Copy results
    if [[ "$REPORT" == "true" ]]; then
        mkdir -p "$TEST_RESULTS_DIR/unit"
        find . -name "surefire-reports" -type d -exec cp -r {} "$TEST_RESULTS_DIR/unit/" \; 2>/dev/null || true
        
        if [[ "$COVERAGE" == "true" ]]; then
            find . -name "jacoco.exec" -exec cp {} "$TEST_RESULTS_DIR/unit/" \; 2>/dev/null || true
            find . -path "*/site/jacoco/*" -name "*.html" -exec cp -r {} "$TEST_RESULTS_DIR/unit/" \; 2>/dev/null || true
        fi
    fi
    
    print_success "Unit tests completed"
}

# Function to run integration tests
run_integration_tests() {
    print_status "Running integration tests..."
    
    cd "$PROJECT_DIR/pet-clinic-app"
    
    local maven_args="test"
    local test_pattern="*IntegrationTest,*IT"
    
    if [[ "$VERBOSE" == "true" ]]; then
        maven_args="$maven_args -X"
    else
        maven_args="$maven_args -q"
    fi
    
    if [[ "$PARALLEL" == "true" ]]; then
        maven_args="$maven_args -T 1C"
    fi
    
    # Set integration test profile
    export SPRING_PROFILES_ACTIVE=test
    
    # Run integration tests
    mvn $maven_args -Dtest="$test_pattern" -Pintegration-tests
    
    # Copy results
    if [[ "$REPORT" == "true" ]]; then
        mkdir -p "$TEST_RESULTS_DIR/integration"
        find . -name "surefire-reports" -type d -exec cp -r {} "$TEST_RESULTS_DIR/integration/" \; 2>/dev/null || true
    fi
    
    print_success "Integration tests completed"
}

# Function to run property-based tests
run_property_based_tests() {
    print_status "Running property-based tests..."
    
    cd "$PROJECT_DIR/pet-clinic-app"
    
    local maven_args="test"
    local test_pattern="*Properties,*PropertyTest"
    
    if [[ "$VERBOSE" == "true" ]]; then
        maven_args="$maven_args -X"
    else
        maven_args="$maven_args -q"
    fi
    
    if [[ "$FAST" == "true" ]]; then
        # Set reduced iterations for faster execution
        export JQWIK_TRIES_DEFAULT=3
        maven_args="$maven_args -Djqwik.tries.default=3"
    fi
    
    # Run property-based tests
    mvn $maven_args -Dtest="$test_pattern"
    
    # Copy results
    if [[ "$REPORT" == "true" ]]; then
        mkdir -p "$TEST_RESULTS_DIR/property-based"
        find . -name "surefire-reports" -type d -exec cp -r {} "$TEST_RESULTS_DIR/property-based/" \; 2>/dev/null || true
    fi
    
    print_success "Property-based tests completed"
}

# Function to run security tests
run_security_tests() {
    print_status "Running security tests..."
    
    if [[ ! -d "$PROJECT_DIR/tests/security" ]]; then
        print_warning "Security tests directory not found, skipping"
        return 0
    fi
    
    cd "$PROJECT_DIR/tests/security"
    
    # Check if Python requirements are installed
    if [[ -f "requirements.txt" ]]; then
        if command -v pip3 &> /dev/null; then
            pip3 install -r requirements.txt -q
        else
            print_warning "pip3 not found, security test dependencies may not be installed"
        fi
    fi
    
    # Run security tests
    if command -v python3 &> /dev/null; then
        local python_args=""
        if [[ "$VERBOSE" == "true" ]]; then
            python_args="-v"
        fi
        
        python3 -m pytest $python_args test-security-validation.py test-penetration-testing.py
        
        # Copy results
        if [[ "$REPORT" == "true" ]]; then
            mkdir -p "$TEST_RESULTS_DIR/security"
            find . -name "*.xml" -o -name "*.json" -o -name "*.html" | xargs -I {} cp {} "$TEST_RESULTS_DIR/security/" 2>/dev/null || true
        fi
    else
        print_warning "Python3 not available, skipping security tests"
    fi
    
    print_success "Security tests completed"
}

# Function to run infrastructure tests
run_infrastructure_tests() {
    print_status "Running infrastructure tests..."
    
    if [[ ! -d "$PROJECT_DIR/tests/infrastructure" ]]; then
        print_warning "Infrastructure tests directory not found, skipping"
        return 0
    fi
    
    cd "$PROJECT_DIR/tests/infrastructure"
    
    # Check if Python requirements are installed
    if [[ -f "../requirements.txt" ]]; then
        if command -v pip3 &> /dev/null; then
            pip3 install -r ../requirements.txt -q
        else
            print_warning "pip3 not found, infrastructure test dependencies may not be installed"
        fi
    fi
    
    # Run infrastructure tests
    if command -v python3 &> /dev/null; then
        local python_args=""
        if [[ "$VERBOSE" == "true" ]]; then
            python_args="-v"
        fi
        
        if [[ "$FAST" == "true" ]]; then
            export HYPOTHESIS_MAX_EXAMPLES=10
        fi
        
        python3 -m pytest $python_args test-*.py
        
        # Copy results
        if [[ "$REPORT" == "true" ]]; then
            mkdir -p "$TEST_RESULTS_DIR/infrastructure"
            find . -name "*.xml" -o -name "*.json" -o -name "*.html" | xargs -I {} cp {} "$TEST_RESULTS_DIR/infrastructure/" 2>/dev/null || true
        fi
    else
        print_warning "Python3 not available, skipping infrastructure tests"
    fi
    
    print_success "Infrastructure tests completed"
}

# Function to run Jenkins tests
run_jenkins_tests() {
    print_status "Running Jenkins tests..."
    
    if [[ ! -d "$PROJECT_DIR/tests/jenkins" ]]; then
        print_warning "Jenkins tests directory not found, skipping"
        return 0
    fi
    
    cd "$PROJECT_DIR/tests/jenkins"
    
    # Check if Python requirements are installed
    if [[ -f "requirements.txt" ]]; then
        if command -v pip3 &> /dev/null; then
            pip3 install -r requirements.txt -q
        else
            print_warning "pip3 not found, Jenkins test dependencies may not be installed"
        fi
    fi
    
    # Run Jenkins tests
    if command -v python3 &> /dev/null; then
        local python_args=""
        if [[ "$VERBOSE" == "true" ]]; then
            python_args="-v"
        fi
        
        if [[ "$FAST" == "true" ]]; then
            export HYPOTHESIS_MAX_EXAMPLES=10
        fi
        
        python3 -m pytest $python_args test-*.py
        
        # Copy results
        if [[ "$REPORT" == "true" ]]; then
            mkdir -p "$TEST_RESULTS_DIR/jenkins"
            find . -name "*.xml" -o -name "*.json" -o -name "*.html" | xargs -I {} cp {} "$TEST_RESULTS_DIR/jenkins/" 2>/dev/null || true
        fi
    else
        print_warning "Python3 not available, skipping Jenkins tests"
    fi
    
    print_success "Jenkins tests completed"
}

# Function to generate test report
generate_test_report() {
    if [[ "$REPORT" != "true" ]]; then
        return 0
    fi
    
    print_status "Generating test report..."
    
    local report_file="$TEST_RESULTS_DIR/test-report-$TIMESTAMP.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Pet Clinic Test Report - $TIMESTAMP</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { background-color: #d4edda; border-color: #c3e6cb; }
        .warning { background-color: #fff3cd; border-color: #ffeaa7; }
        .error { background-color: #f8d7da; border-color: #f5c6cb; }
        .code { background-color: #f8f9fa; padding: 10px; border-radius: 3px; font-family: monospace; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Pet Clinic CI/CD Pipeline Test Report</h1>
        <p><strong>Generated:</strong> $(date)</p>
        <p><strong>Test Category:</strong> $TEST_CATEGORY</p>
        <p><strong>Fast Mode:</strong> $FAST</p>
        <p><strong>Coverage:</strong> $COVERAGE</p>
    </div>
EOF
    
    # Add test results sections
    for test_type in unit integration property-based security infrastructure jenkins; do
        if [[ -d "$TEST_RESULTS_DIR/$test_type" ]]; then
            echo "<div class=\"section success\">" >> "$report_file"
            echo "<h2>$test_type Tests</h2>" >> "$report_file"
            echo "<p>Results available in: $TEST_RESULTS_DIR/$test_type</p>" >> "$report_file"
            echo "</div>" >> "$report_file"
        fi
    done
    
    cat >> "$report_file" << EOF
    <div class="section">
        <h2>Test Execution Summary</h2>
        <div class="code">
            Test Category: $TEST_CATEGORY<br>
            Fast Mode: $FAST<br>
            Coverage: $COVERAGE<br>
            Parallel: $PARALLEL<br>
            Results Directory: $TEST_RESULTS_DIR<br>
        </div>
    </div>
</body>
</html>
EOF
    
    print_success "Test report generated: $report_file"
}

# Function to display test summary
display_test_summary() {
    echo
    print_success "=== TEST EXECUTION COMPLETED ==="
    echo
    echo -e "${BLUE}Test Category:${NC} $TEST_CATEGORY"
    echo -e "${BLUE}Fast Mode:${NC} $FAST"
    echo -e "${BLUE}Coverage:${NC} $COVERAGE"
    echo -e "${BLUE}Reports:${NC} $REPORT"
    echo -e "${BLUE}Parallel:${NC} $PARALLEL"
    echo
    
    if [[ "$REPORT" == "true" ]]; then
        echo -e "${GREEN}Test Results:${NC}"
        echo "  Directory: $TEST_RESULTS_DIR"
        if [[ -f "$TEST_RESULTS_DIR/test-report-$TIMESTAMP.html" ]]; then
            echo "  Report: $TEST_RESULTS_DIR/test-report-$TIMESTAMP.html"
        fi
        echo
    fi
    
    if [[ "$COVERAGE" == "true" ]]; then
        echo -e "${GREEN}Coverage Reports:${NC}"
        find "$PROJECT_DIR" -name "jacoco.exec" -o -path "*/site/jacoco/index.html" | head -5 | while read -r file; do
            echo "  $file"
        done
        echo
    fi
    
    echo -e "${GREEN}Next Steps:${NC}"
    echo "1. Review test results and fix any failures"
    echo "2. Check coverage reports if generated"
    echo "3. Run specific test categories if needed"
    echo "4. Integrate tests into CI/CD pipeline"
    echo
}

# Main function
main() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}Pet Clinic Test Runner${NC}"
    echo -e "${BLUE}================================${NC}"
    echo
    
    parse_args "$@"
    setup_test_environment
    check_prerequisites
    build_application
    
    # Run tests based on category
    case "$TEST_CATEGORY" in
        "unit")
            run_unit_tests
            ;;
        "integration")
            run_integration_tests
            ;;
        "property-based")
            run_property_based_tests
            ;;
        "security")
            run_security_tests
            ;;
        "infrastructure")
            run_infrastructure_tests
            ;;
        "all")
            run_unit_tests
            run_integration_tests
            run_property_based_tests
            run_security_tests
            run_infrastructure_tests
            run_jenkins_tests
            ;;
        *)
            print_error "Unknown test category: $TEST_CATEGORY"
            show_usage
            exit 1
            ;;
    esac
    
    generate_test_report
    display_test_summary
    
    print_success "Test execution completed successfully!"
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi