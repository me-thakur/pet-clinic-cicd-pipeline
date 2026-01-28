#!/bin/bash

# Pet Clinic CI/CD Pipeline - Local Development Setup Script
# This script sets up the complete local development environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-rootpassword}"
DB_PASSWORD="${DB_PASSWORD:-petclinic123}"
BACKEND_PORT="${BACKEND_PORT:-8081}"
FRONTEND_PORT="${FRONTEND_PORT:-8080}"

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

# Function to detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if command -v apt-get &> /dev/null; then
            OS="ubuntu"
        elif command -v yum &> /dev/null; then
            OS="centos"
        else
            OS="linux"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        OS="windows"
    else
        OS="unknown"
    fi
    
    print_status "Detected OS: $OS"
}

# Function to check if command exists
command_exists() {
    command -v "$1" &> /dev/null
}

# Function to install Java
install_java() {
    if command_exists java; then
        local java_version
        java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1-2)
        if [[ "$java_version" == "11"* ]] || [[ "$java_version" == "1.8"* ]]; then
            print_success "Java $java_version is already installed"
            return 0
        else
            print_warning "Java $java_version found, but Java 11 is recommended"
        fi
    fi
    
    print_status "Installing Java 11..."
    case "$OS" in
        "ubuntu")
            sudo apt update
            sudo apt install -y openjdk-11-jdk
            ;;
        "centos")
            sudo yum install -y java-11-openjdk-devel
            ;;
        "macos")
            if command_exists brew; then
                brew install openjdk@11
                echo 'export PATH="/usr/local/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
                echo 'export PATH="/usr/local/opt/openjdk@11/bin:$PATH"' >> ~/.bash_profile
            else
                print_error "Homebrew not found. Please install Java 11 manually."
                return 1
            fi
            ;;
        *)
            print_error "Unsupported OS for automatic Java installation"
            return 1
            ;;
    esac
    
    print_success "Java 11 installed successfully"
}

# Function to install Maven
install_maven() {
    if command_exists mvn; then
        print_success "Maven is already installed"
        return 0
    fi
    
    print_status "Installing Maven..."
    case "$OS" in
        "ubuntu")
            sudo apt install -y maven
            ;;
        "centos")
            sudo yum install -y maven
            ;;
        "macos")
            if command_exists brew; then
                brew install maven
            else
                print_error "Homebrew not found. Please install Maven manually."
                return 1
            fi
            ;;
        *)
            print_error "Unsupported OS for automatic Maven installation"
            return 1
            ;;
    esac
    
    print_success "Maven installed successfully"
}

# Function to install MySQL
install_mysql() {
    if command_exists mysql; then
        print_success "MySQL is already installed"
        return 0
    fi
    
    print_status "Installing MySQL..."
    case "$OS" in
        "ubuntu")
            sudo apt install -y mysql-server mysql-client
            sudo systemctl start mysql
            sudo systemctl enable mysql
            ;;
        "centos")
            sudo yum install -y mysql-server mysql
            sudo systemctl start mysqld
            sudo systemctl enable mysqld
            ;;
        "macos")
            if command_exists brew; then
                brew install mysql
                brew services start mysql
            else
                print_error "Homebrew not found. Please install MySQL manually."
                return 1
            fi
            ;;
        *)
            print_error "Unsupported OS for automatic MySQL installation"
            return 1
            ;;
    esac
    
    print_success "MySQL installed successfully"
}

# Function to install Docker (optional)
install_docker() {
    if command_exists docker; then
        print_success "Docker is already installed"
        return 0
    fi
    
    read -p "Do you want to install Docker for containerized development? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Skipping Docker installation"
        return 0
    fi
    
    print_status "Installing Docker..."
    case "$OS" in
        "ubuntu")
            sudo apt update
            sudo apt install -y docker.io docker-compose
            sudo usermod -aG docker "$USER"
            sudo systemctl start docker
            sudo systemctl enable docker
            ;;
        "centos")
            sudo yum install -y docker docker-compose
            sudo usermod -aG docker "$USER"
            sudo systemctl start docker
            sudo systemctl enable docker
            ;;
        "macos")
            if command_exists brew; then
                brew install --cask docker
                print_warning "Please start Docker Desktop manually"
            else
                print_error "Homebrew not found. Please install Docker Desktop manually."
                return 1
            fi
            ;;
        *)
            print_error "Unsupported OS for automatic Docker installation"
            return 1
            ;;
    esac
    
    print_success "Docker installed successfully"
    print_warning "You may need to log out and back in for Docker group membership to take effect"
}

# Function to setup database
setup_database() {
    print_status "Setting up local database..."
    
    # Check if we should use Docker
    if command_exists docker && docker ps &> /dev/null; then
        setup_database_docker
    else
        setup_database_native
    fi
}

# Function to setup database with Docker
setup_database_docker() {
    print_status "Setting up database with Docker..."
    
    # Create docker-compose file
    cat > "$PROJECT_DIR/docker-compose.dev.yml" << EOF
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: petclinic-mysql-dev
    environment:
      MYSQL_ROOT_PASSWORD: $MYSQL_ROOT_PASSWORD
      MYSQL_DATABASE: petclinic_dev
      MYSQL_USER: petclinic
      MYSQL_PASSWORD: $DB_PASSWORD
    ports:
      - "3306:3306"
    volumes:
      - mysql_dev_data:/var/lib/mysql
      - ./pet-clinic-app/pet-clinic-backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/1-schema.sql:ro
      - ./pet-clinic-app/pet-clinic-backend/src/main/resources/data.sql:/docker-entrypoint-initdb.d/2-data.sql:ro
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

volumes:
  mysql_dev_data:
EOF
    
    # Start database
    cd "$PROJECT_DIR"
    docker-compose -f docker-compose.dev.yml up -d mysql
    
    # Wait for database to be ready
    print_status "Waiting for database to be ready..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec petclinic-mysql-dev mysqladmin ping -h localhost --silent; then
            print_success "Database is ready!"
            break
        fi
        
        print_status "Attempt $attempt/$max_attempts: Database not ready yet, waiting 5 seconds..."
        sleep 5
        ((attempt++))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        print_error "Database failed to start within expected time"
        return 1
    fi
    
    print_success "Database setup completed with Docker"
}

# Function to setup database natively
setup_database_native() {
    print_status "Setting up database natively..."
    
    # Secure MySQL installation (basic setup)
    if [[ "$OS" == "ubuntu" || "$OS" == "centos" ]]; then
        # Set root password if not set
        mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '$MYSQL_ROOT_PASSWORD';" 2>/dev/null || true
    fi
    
    # Create databases and user
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" << EOF
CREATE DATABASE IF NOT EXISTS petclinic_dev;
CREATE DATABASE IF NOT EXISTS petclinic_test;

CREATE USER IF NOT EXISTS 'petclinic'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON petclinic_dev.* TO 'petclinic'@'localhost';
GRANT ALL PRIVILEGES ON petclinic_test.* TO 'petclinic'@'localhost';
FLUSH PRIVILEGES;
EOF
    
    # Initialize schema
    if [[ -f "$PROJECT_DIR/pet-clinic-app/pet-clinic-backend/src/main/resources/schema.sql" ]]; then
        mysql -u petclinic -p"$DB_PASSWORD" petclinic_dev < "$PROJECT_DIR/pet-clinic-app/pet-clinic-backend/src/main/resources/schema.sql"
        mysql -u petclinic -p"$DB_PASSWORD" petclinic_dev < "$PROJECT_DIR/pet-clinic-app/pet-clinic-backend/src/main/resources/data.sql"
    fi
    
    print_success "Database setup completed natively"
}

# Function to create application configuration
create_app_config() {
    print_status "Creating application configuration files..."
    
    # Backend configuration
    mkdir -p "$PROJECT_DIR/pet-clinic-app/pet-clinic-backend/src/main/resources"
    cat > "$PROJECT_DIR/pet-clinic-app/pet-clinic-backend/src/main/resources/application-dev.yml" << EOF
spring:
  profiles:
    active: dev
  
  datasource:
    url: jdbc:mysql://localhost:3306/petclinic_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: petclinic
    password: $DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
  
  logging:
    level:
      com.petclinic: DEBUG
      org.springframework.web: DEBUG
      org.hibernate.SQL: DEBUG

server:
  port: $BACKEND_PORT
  servlet:
    context-path: /api

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
EOF
    
    # Frontend configuration
    mkdir -p "$PROJECT_DIR/pet-clinic-app/pet-clinic-frontend/src/main/resources"
    cat > "$PROJECT_DIR/pet-clinic-app/pet-clinic-frontend/src/main/resources/application-dev.yml" << EOF
spring:
  profiles:
    active: dev
  
  thymeleaf:
    cache: false
    mode: HTML
    encoding: UTF-8
    prefix: classpath:/templates/
    suffix: .html
  
  web:
    resources:
      cache:
        period: 0

server:
  port: $FRONTEND_PORT

petclinic:
  backend:
    url: http://localhost:$BACKEND_PORT/api

logging:
  level:
    com.petclinic: DEBUG
    org.springframework.web: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
EOF
    
    print_success "Application configuration files created"
}

# Function to create startup scripts
create_startup_scripts() {
    print_status "Creating startup scripts..."
    
    mkdir -p "$PROJECT_DIR/scripts"
    mkdir -p "$PROJECT_DIR/logs"
    
    # Backend startup script
    cat > "$PROJECT_DIR/scripts/start-backend.sh" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")/../pet-clinic-app/pet-clinic-backend"
echo "Starting backend service..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../../logs/backend.log 2>&1 &
echo $! > ../../logs/backend.pid
echo "Backend started with PID $(cat ../../logs/backend.pid)"
echo "Logs: logs/backend.log"
echo "URL: http://localhost:8081/api"
EOF
    
    # Frontend startup script
    cat > "$PROJECT_DIR/scripts/start-frontend.sh" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")/../pet-clinic-app/pet-clinic-frontend"
echo "Starting frontend service..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../../logs/frontend.log 2>&1 &
echo $! > ../../logs/frontend.pid
echo "Frontend started with PID $(cat ../../logs/frontend.pid)"
echo "Logs: logs/frontend.log"
echo "URL: http://localhost:8080"
EOF
    
    # Combined startup script
    cat > "$PROJECT_DIR/scripts/start-all.sh" << 'EOF'
#!/bin/bash

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

echo "Starting Pet Clinic development environment..."

# Start database if using Docker
if command -v docker &> /dev/null && [[ -f "docker-compose.dev.yml" ]]; then
    echo "Starting database..."
    docker-compose -f docker-compose.dev.yml up -d mysql
    
    # Wait for database
    echo "Waiting for database to be ready..."
    until docker exec petclinic-mysql-dev mysqladmin ping -h localhost --silent; do
        sleep 2
    done
    echo "Database is ready!"
fi

# Start backend
echo "Starting backend..."
./scripts/start-backend.sh

# Wait for backend to start
echo "Waiting for backend to start..."
until curl -f http://localhost:8081/api/health > /dev/null 2>&1; do
    sleep 2
done
echo "Backend is ready!"

# Start frontend
echo "Starting frontend..."
./scripts/start-frontend.sh

echo ""
echo "=== Pet Clinic Development Environment Started ==="
echo "Frontend: http://localhost:8080"
echo "Backend API: http://localhost:8081/api"
echo "Backend Health: http://localhost:8081/api/health"
echo "Frontend Health: http://localhost:8080/actuator/health"
echo ""
echo "Logs:"
echo "  Backend: logs/backend.log"
echo "  Frontend: logs/frontend.log"
echo ""
echo "To stop services: ./scripts/stop-all.sh"
EOF
    
    # Stop script
    cat > "$PROJECT_DIR/scripts/stop-all.sh" << 'EOF'
#!/bin/bash

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

echo "Stopping Pet Clinic development environment..."

# Stop backend
if [[ -f "logs/backend.pid" ]]; then
    PID=$(cat logs/backend.pid)
    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping backend (PID: $PID)..."
        kill "$PID"
        rm logs/backend.pid
    fi
fi

# Stop frontend
if [[ -f "logs/frontend.pid" ]]; then
    PID=$(cat logs/frontend.pid)
    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping frontend (PID: $PID)..."
        kill "$PID"
        rm logs/frontend.pid
    fi
fi

# Stop database if using Docker
if command -v docker &> /dev/null && [[ -f "docker-compose.dev.yml" ]]; then
    echo "Stopping database..."
    docker-compose -f docker-compose.dev.yml down
fi

echo "All services stopped"
EOF
    
    # Make scripts executable
    chmod +x "$PROJECT_DIR/scripts/start-backend.sh"
    chmod +x "$PROJECT_DIR/scripts/start-frontend.sh"
    chmod +x "$PROJECT_DIR/scripts/start-all.sh"
    chmod +x "$PROJECT_DIR/scripts/stop-all.sh"
    
    print_success "Startup scripts created"
}

# Function to create environment file
create_env_file() {
    print_status "Creating environment configuration..."
    
    cat > "$PROJECT_DIR/.env" << EOF
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=petclinic_dev
DB_USERNAME=petclinic
DB_PASSWORD=$DB_PASSWORD

# Application Configuration
BACKEND_PORT=$BACKEND_PORT
FRONTEND_PORT=$FRONTEND_PORT
PROFILE=dev

# Testing Configuration
TEST_DB_NAME=petclinic_test

# Development Tools
MAVEN_OPTS=-Xmx1024m
JAVA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
EOF
    
    print_success "Environment file created"
}

# Function to build application
build_application() {
    print_status "Building application..."
    
    cd "$PROJECT_DIR/pet-clinic-app"
    
    # Clean and compile
    mvn clean compile -q
    
    # Run tests
    print_status "Running tests..."
    mvn test -q
    
    print_success "Application built and tested successfully"
}

# Function to verify setup
verify_setup() {
    print_status "Verifying setup..."
    
    # Check Java
    if command_exists java; then
        local java_version
        java_version=$(java -version 2>&1 | head -n1)
        print_success "Java: $java_version"
    else
        print_error "Java not found"
        return 1
    fi
    
    # Check Maven
    if command_exists mvn; then
        local maven_version
        maven_version=$(mvn -version | head -n1)
        print_success "Maven: $maven_version"
    else
        print_error "Maven not found"
        return 1
    fi
    
    # Check database
    if command_exists docker && docker ps | grep -q petclinic-mysql-dev; then
        print_success "Database: Running in Docker"
    elif command_exists mysql; then
        if mysql -u petclinic -p"$DB_PASSWORD" -e "SELECT 1" petclinic_dev &> /dev/null; then
            print_success "Database: Connected successfully"
        else
            print_warning "Database: Connection failed"
        fi
    else
        print_warning "Database: Not verified"
    fi
    
    print_success "Setup verification completed"
}

# Function to display next steps
display_next_steps() {
    echo
    print_success "=== LOCAL DEVELOPMENT SETUP COMPLETED ==="
    echo
    echo -e "${GREEN}Quick Start:${NC}"
    echo "1. Start all services:"
    echo "   ./scripts/start-all.sh"
    echo
    echo "2. Access the application:"
    echo "   - Frontend: http://localhost:$FRONTEND_PORT"
    echo "   - Backend API: http://localhost:$BACKEND_PORT/api"
    echo
    echo "3. Stop all services:"
    echo "   ./scripts/stop-all.sh"
    echo
    echo -e "${GREEN}Development Workflow:${NC}"
    echo "1. Make code changes"
    echo "2. Application will auto-reload (Spring Boot DevTools)"
    echo "3. Run tests: mvn test"
    echo "4. Check logs in logs/ directory"
    echo
    echo -e "${GREEN}Database Access:${NC}"
    if command_exists docker && docker ps | grep -q petclinic-mysql-dev; then
        echo "- Docker: docker exec -it petclinic-mysql-dev mysql -u petclinic -p petclinic_dev"
    fi
    echo "- Native: mysql -u petclinic -p$DB_PASSWORD petclinic_dev"
    echo
    echo -e "${GREEN}Useful Commands:${NC}"
    echo "- Build: mvn clean package"
    echo "- Test: mvn test"
    echo "- Debug: Add -Dspring-boot.run.jvmArguments=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\""
    echo
    echo -e "${YELLOW}Configuration Files:${NC}"
    echo "- Backend: pet-clinic-app/pet-clinic-backend/src/main/resources/application-dev.yml"
    echo "- Frontend: pet-clinic-app/pet-clinic-frontend/src/main/resources/application-dev.yml"
    echo "- Environment: .env"
    echo
}

# Main function
main() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}Pet Clinic Local Development Setup${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo
    
    detect_os
    
    print_status "Installing required software..."
    install_java
    install_maven
    install_mysql
    install_docker
    
    print_status "Setting up development environment..."
    setup_database
    create_app_config
    create_startup_scripts
    create_env_file
    
    print_status "Building and testing application..."
    build_application
    
    verify_setup
    display_next_steps
    
    print_success "Local development setup completed successfully!"
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi