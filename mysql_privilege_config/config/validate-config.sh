#!/bin/bash
# MySQL Configuration Validation Script Wrapper
# This script provides a convenient way to validate MySQL configuration

set -e

# Default values
HOST="localhost"
PORT="3306"
USER="root"
PASSWORD=""
ENVIRONMENT=""
OUTPUT="text"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to show usage
show_usage() {
    cat << EOF
MySQL Configuration Validation Script

Usage: $0 [OPTIONS]

OPTIONS:
    -h, --host HOST         MySQL host (default: localhost)
    -P, --port PORT         MySQL port (default: 3306)
    -u, --user USER         MySQL user (default: root)
    -p, --password PASS     MySQL password (default: empty)
    -e, --environment ENV   Target environment: local, ci, production
                           (auto-detected if not specified)
    -o, --output FORMAT     Output format: text, json (default: text)
    --help                  Show this help message

EXAMPLES:
    # Validate local development setup
    $0 --environment local

    # Validate CI/CD setup with custom connection
    $0 --host mysql-ci --user ci_user --password ci_pass --environment ci

    # Validate production with JSON output
    $0 --host prod-mysql --environment production --output json

    # Auto-detect environment
    $0

ENVIRONMENT DETECTION:
    The script can automatically detect the environment based on:
    - CI environment variables (CI=true, GITHUB_ACTIONS, etc.)
    - Container indicators (Docker, Kubernetes)
    - Default to local development if no indicators found

VALIDATION CHECKS:
    - log_bin_trust_function_creators setting
    - Binary logging configuration
    - User privileges (SUPER, CREATE ROUTINE)
    - Function creation capability
    - Environment-specific security settings
    - SSL/TLS configuration (production)
    - Connection limits and timeouts

EXIT CODES:
    0 - Validation passed
    1 - Validation failed or connection error
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            HOST="$2"
            shift 2
            ;;
        -P|--port)
            PORT="$2"
            shift 2
            ;;
        -u|--user)
            USER="$2"
            shift 2
            ;;
        -p|--password)
            PASSWORD="$2"
            shift 2
            ;;
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_color $RED "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/validate-config.py"

# Check if Python script exists
if [[ ! -f "$PYTHON_SCRIPT" ]]; then
    print_color $RED "Error: Python validation script not found at $PYTHON_SCRIPT"
    exit 1
fi

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    print_color $RED "Error: Python 3 is required but not installed"
    exit 1
fi

# Build Python command
PYTHON_CMD="python3 $PYTHON_SCRIPT --host $HOST --port $PORT --user $USER --output $OUTPUT"

if [[ -n "$PASSWORD" ]]; then
    PYTHON_CMD="$PYTHON_CMD --password $PASSWORD"
fi

if [[ -n "$ENVIRONMENT" ]]; then
    PYTHON_CMD="$PYTHON_CMD --environment $ENVIRONMENT"
fi

# Print connection info (without password)
print_color $BLUE "🔍 Validating MySQL configuration..."
print_color $BLUE "   Host: $HOST:$PORT"
print_color $BLUE "   User: $USER"
if [[ -n "$ENVIRONMENT" ]]; then
    print_color $BLUE "   Environment: $ENVIRONMENT"
else
    print_color $BLUE "   Environment: auto-detect"
fi

# Run the Python validation script
if eval "$PYTHON_CMD"; then
    print_color $GREEN "✅ Configuration validation completed successfully"
    exit 0
else
    exit_code=$?
    print_color $RED "❌ Configuration validation failed"
    
    # Provide helpful error messages based on common issues
    case $exit_code in
        1)
            print_color $YELLOW "💡 Common solutions:"
            print_color $YELLOW "   - Check MySQL connection parameters"
            print_color $YELLOW "   - Verify log_bin_trust_function_creators setting"
            print_color $YELLOW "   - Ensure binary logging is properly configured"
            print_color $YELLOW "   - Review user privileges"
            ;;
        *)
            print_color $YELLOW "💡 Check the error messages above for specific issues"
            ;;
    esac
    
    exit $exit_code
fi