# Pet Clinic CI/CD Pipeline - Local Development Guide

This guide provides comprehensive instructions for setting up and running the Pet Clinic CI/CD Pipeline system locally for development and testing purposes.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Local Infrastructure](#local-infrastructure)
4. [Application Development](#application-development)
5. [Testing](#testing)
6. [Debugging](#debugging)
7. [Development Workflow](#development-workflow)

## Prerequisites

### Required Software

#### Core Development Tools
```bash
# Java Development Kit 11
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk

# macOS (using Homebrew)
brew install openjdk@11

# Windows (using Chocolatey)
choco install openjdk11

# Verify installation
java -version
javac -version
```

#### Build Tools
```bash
# Maven 3.6+
# Ubuntu/Debian
sudo apt install maven

# macOS
brew install maven

# Windows
choco install maven

# Verify installation
mvn -version
```

#### Database
```bash
# MySQL 8.0
# Ubuntu/Debian
sudo apt install mysql-server mysql-client

# macOS
brew install mysql

# Windows
choco install mysql

# Start MySQL service
# Ubuntu/Debian
sudo systemctl start mysql
sudo systemctl enable mysql

# macOS
brew services start mysql

# Windows
net start mysql80
```

#### Development Environment
```bash
# Git
sudo apt install git  # Ubuntu/Debian
brew install git      # macOS
choco install git     # Windows

# Node.js (for frontend tooling)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs  # Ubuntu/Debian

brew install node               # macOS
choco install nodejs           # Windows

# Docker (optional, for containerized development)
# Ubuntu/Debian
sudo apt install docker.io docker-compose
sudo usermod -aG docker $USER

# macOS
brew install docker docker-compose

# Windows
choco install docker-desktop
```

### IDE Setup

#### IntelliJ IDEA (Recommended)
```bash
# Download from https://www.jetbrains.com/idea/
# Or install via package manager

# Ubuntu (Snap)
sudo snap install intellij-idea-community --classic

# macOS
brew install --cask intellij-idea-ce

# Windows
choco install intellijidea-community
```

**Required Plugins**:
- Spring Boot
- Maven Helper
- Database Navigator
- Git Integration
- Thymeleaf

#### VS Code Alternative
```bash
# Install VS Code
# Ubuntu
sudo snap install code --classic

# macOS
brew install --cask visual-studio-code

# Windows
choco install vscode
```

**Required Extensions**:
- Extension Pack for Java
- Spring Boot Extension Pack
- MySQL
- GitLens
- Thunder Client (for API testing)

## Environment Setup

### 1. Clone Repository

```bash
# Clone the repository
git clone https://github.com/your-org/pet-clinic-cicd-pipeline.git
cd pet-clinic-cicd-pipeline

# Create development branch
git checkout -b feature/local-development
```

### 2. Database Setup

#### Create Local Database
```bash
# Connect to MySQL
mysql -u root -p

# Create database and user
CREATE DATABASE petclinic_dev;
CREATE DATABASE petclinic_test;

CREATE USER 'petclinic'@'localhost' IDENTIFIED BY 'petclinic123';
GRANT ALL PRIVILEGES ON petclinic_dev.* TO 'petclinic'@'localhost';
GRANT ALL PRIVILEGES ON petclinic_test.* TO 'petclinic'@'localhost';
FLUSH PRIVILEGES;

# Verify connection
mysql -u petclinic -ppetclinic123 -e "SELECT 1"
```

#### Initialize Schema
```bash
# Navigate to backend module
cd pet-clinic-app/pet-clinic-backend

# Run schema initialization
mysql -u petclinic -ppetclinic123 petclinic_dev < src/main/resources/schema.sql
mysql -u petclinic -ppetclinic123 petclinic_dev < src/main/resources/data.sql
```

#### Setup MySQL Privilege Configuration
```bash
# Install MySQL privilege configuration system
pip install -e mysql_privilege_config/

# Configure for local development environment
mysql-privilege-config configure --environment local \
  --host localhost \
  --user petclinic \
  --password petclinic123

# Apply local development security settings
mysql-privilege-config apply-security --environment local

# Validate configuration
mysql-privilege-config validate --environment local

# Test function creation (should work in local environment)
mysql-privilege-config test-functions --environment local
```

### 3. Application Configuration

#### Backend Configuration
Create `pet-clinic-app/pet-clinic-backend/src/main/resources/application-dev.yml`:

```yaml
# Development configuration
spring:
  profiles:
    active: dev
  
  datasource:
    url: jdbc:mysql://localhost:3306/petclinic_dev?useSSL=false&serverTimezone=UTC
    username: petclinic
    password: petclinic123
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
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
      org.hibernate.type.descriptor.sql.BasicBinder: TRACE

server:
  port: 8081
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
```

#### Frontend Configuration
Create `pet-clinic-app/pet-clinic-frontend/src/main/resources/application-dev.yml`:

```yaml
# Development configuration
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
  port: 8080

# Backend service URL
petclinic:
  backend:
    url: http://localhost:8081/api

logging:
  level:
    com.petclinic: DEBUG
    org.springframework.web: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 4. Environment Variables

Create `.env` file in project root:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=petclinic_dev
DB_USERNAME=petclinic
DB_PASSWORD=petclinic123

# Application Configuration
BACKEND_PORT=8081
FRONTEND_PORT=8080
PROFILE=dev

# Testing Configuration
TEST_DB_NAME=petclinic_test

# MySQL Privilege Configuration
MYSQL_PRIVILEGE_ENVIRONMENT=local
MYSQL_PRIVILEGE_HOST=localhost
MYSQL_PRIVILEGE_USER=petclinic
MYSQL_PRIVILEGE_PASSWORD=petclinic123
MYSQL_PRIVILEGE_SECURITY_LEVEL=permissive

# Development Tools
MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"
JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## Local Infrastructure

### 1. Database Management

#### Using Docker (Recommended)
```bash
# Create docker-compose.yml for local development
cat > docker-compose.dev.yml << EOF
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: petclinic-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: petclinic_dev
      MYSQL_USER: petclinic
      MYSQL_PASSWORD: petclinic123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./pet-clinic-app/pet-clinic-backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/1-schema.sql
      - ./pet-clinic-app/pet-clinic-backend/src/main/resources/data.sql:/docker-entrypoint-initdb.d/2-data.sql
    command: --default-authentication-plugin=mysql_native_password

volumes:
  mysql_data:
EOF

# Start database
docker-compose -f docker-compose.dev.yml up -d mysql

# Check database status
docker-compose -f docker-compose.dev.yml ps
```

#### Database Management Tools
```bash
# Install MySQL Workbench (GUI)
# Ubuntu
sudo snap install mysql-workbench-community

# macOS
brew install --cask mysql-workbench

# Windows
choco install mysql.workbench

# Or use command line tools
mysql -u petclinic -ppetclinic123 -h localhost petclinic_dev
```

### 2. Local Jenkins (Optional)

```bash
# Run Jenkins in Docker for local CI/CD testing
docker run -d \
  --name jenkins-local \
  -p 8082:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts

# Get initial admin password
docker exec jenkins-local cat /var/jenkins_home/secrets/initialAdminPassword

# Access Jenkins at http://localhost:8082
```

## Application Development

### 1. Build and Run

#### Backend Service
```bash
# Navigate to backend directory
cd pet-clinic-app/pet-clinic-backend

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run application in development mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or run with debugging enabled
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Application will be available at http://localhost:8081/api
```

#### Frontend Service
```bash
# Navigate to frontend directory
cd pet-clinic-app/pet-clinic-frontend

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run application in development mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Application will be available at http://localhost:8080
```

#### Run Both Services
```bash
# From project root, create a startup script
cat > scripts/start-local.sh << 'EOF'
#!/bin/bash

# Start database if using Docker
docker-compose -f docker-compose.dev.yml up -d mysql

# Wait for database to be ready
echo "Waiting for database to be ready..."
until docker exec petclinic-mysql mysqladmin ping -h"localhost" --silent; do
    sleep 1
done

# Start backend in background
echo "Starting backend service..."
cd pet-clinic-app/pet-clinic-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../../logs/backend.log 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend to start..."
until curl -f http://localhost:8081/api/health > /dev/null 2>&1; do
    sleep 2
done

# Start frontend
echo "Starting frontend service..."
cd ../pet-clinic-frontend
mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../../logs/frontend.log 2>&1 &
FRONTEND_PID=$!

echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "Backend: http://localhost:8081/api"
echo "Frontend: http://localhost:8080"
echo "Logs: logs/backend.log, logs/frontend.log"
EOF

chmod +x scripts/start-local.sh

# Create logs directory
mkdir -p logs

# Start services
./scripts/start-local.sh
```

### 2. Development Workflow

#### Hot Reload Setup
```bash
# Add Spring Boot DevTools dependency to both modules
# Already included in pom.xml for development

# For IntelliJ IDEA:
# 1. Enable "Build project automatically" in Settings > Build > Compiler
# 2. Enable "Allow auto-make to start even if developed application is currently running"
# 3. Use Ctrl+F9 to trigger recompilation

# For VS Code:
# Install "Spring Boot Dashboard" extension
# Use Ctrl+Shift+P > "Spring Boot: Reload Apps"
```

#### Live Reload for Frontend
```bash
# Frontend changes will be automatically reloaded due to Thymeleaf configuration
# CSS/JS changes require browser refresh

# For automatic browser refresh, add LiveReload
# Add to frontend pom.xml:
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 3. API Development

#### Testing REST Endpoints
```bash
# Install HTTPie for command-line API testing
pip install httpie

# Test backend endpoints
http GET localhost:8081/api/health
http GET localhost:8081/api/owners
http POST localhost:8081/api/owners firstName=John lastName=Doe address="123 Main St" city=Springfield telephone=555-1234

# Or use curl
curl -X GET http://localhost:8081/api/owners
curl -X POST http://localhost:8081/api/owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","address":"123 Main St","city":"Springfield","telephone":"555-1234"}'
```

#### API Documentation
```bash
# Add Swagger/OpenAPI documentation
# Add to backend pom.xml:
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.6.14</version>
</dependency>

# Access API documentation at:
# http://localhost:8081/api/swagger-ui.html
# http://localhost:8081/api/v3/api-docs
```

## Testing

### 1. Unit Testing

#### Run Unit Tests
```bash
# Run all tests
mvn test

# Run tests for specific module
cd pet-clinic-app/pet-clinic-backend
mvn test

# Run specific test class
mvn test -Dtest=OwnerControllerTest

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

#### Property-Based Testing
```bash
# Property-based tests use jqwik framework
# Run property tests specifically
mvn test -Dtest="*Properties*"

# Configure jqwik in src/test/resources/junit-platform.properties
jqwik.tries.default=100
jqwik.max.discard.ratio=5
jqwik.reporting.only-failures=true
```

### 2. Integration Testing

#### Database Integration Tests
```bash
# Use test database
export SPRING_PROFILES_ACTIVE=test

# Run integration tests
mvn test -Dtest="*IntegrationTest"

# Or use Maven profile
mvn test -Pintegration-tests
```

#### End-to-End Testing
```bash
# Install Selenium WebDriver for browser testing
mvn dependency:get -Dartifact=org.seleniumhq.selenium:selenium-java:4.11.0

# Run E2E tests (requires both services running)
mvn test -Dtest="*E2ETest"
```

### 3. Test Data Management

#### Test Database Setup
```sql
-- Create test-specific data
INSERT INTO owners (first_name, last_name, address, city, telephone) VALUES
('Test', 'Owner1', '123 Test St', 'Test City', '555-0001'),
('Test', 'Owner2', '456 Test Ave', 'Test Town', '555-0002');

INSERT INTO pets (name, birth_date, type, owner_id) VALUES
('TestPet1', '2020-01-01', 'Dog', 1),
('TestPet2', '2021-06-15', 'Cat', 2);
```

#### Test Data Cleanup
```bash
# Create cleanup script
cat > scripts/cleanup-test-data.sh << 'EOF'
#!/bin/bash
mysql -u petclinic -ppetclinic123 petclinic_test << SQL
DELETE FROM visits WHERE pet_id IN (SELECT id FROM pets WHERE name LIKE 'Test%');
DELETE FROM pets WHERE name LIKE 'Test%';
DELETE FROM owners WHERE first_name = 'Test';
SQL
EOF

chmod +x scripts/cleanup-test-data.sh
```

## Debugging

### 1. Application Debugging

#### IntelliJ IDEA Debug Configuration
```
1. Create new "Remote JVM Debug" configuration
2. Set Host: localhost
3. Set Port: 5005
4. Set module classpath: pet-clinic-backend or pet-clinic-frontend
5. Start application with debug flags
6. Attach debugger
```

#### VS Code Debug Configuration
Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Backend",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        },
        {
            "type": "java",
            "name": "Debug Frontend",
            "request": "attach",
            "hostName": "localhost",
            "port": 5006
        }
    ]
}
```

### 2. Database Debugging

#### Query Logging
```yaml
# Add to application-dev.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.jdbc.core: DEBUG
```

#### Database Profiling
```sql
-- Enable MySQL query logging
SET GLOBAL general_log = 'ON';
SET GLOBAL general_log_file = '/tmp/mysql-query.log';

-- Monitor slow queries
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
SET GLOBAL slow_query_log_file = '/tmp/mysql-slow.log';
```

### 3. Performance Profiling

#### JVM Profiling
```bash
# Add JVM profiling flags
export JAVA_OPTS="-XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=profile.jfr"

# Or use JProfiler/YourKit for detailed profiling
```

#### Application Metrics
```bash
# Access actuator endpoints
curl http://localhost:8081/api/actuator/metrics
curl http://localhost:8081/api/actuator/health
curl http://localhost:8081/api/actuator/info

# Custom metrics endpoint
curl http://localhost:8081/api/actuator/metrics/jvm.memory.used
```

## Development Workflow

### 1. Feature Development

#### Branch Strategy
```bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes and commit
git add .
git commit -m "Add new feature"

# Push and create pull request
git push origin feature/new-feature
```

#### Code Quality Checks
```bash
# Run code formatting
mvn spotless:apply

# Run static analysis
mvn spotbugs:check

# Run security checks
mvn org.owasp:dependency-check-maven:check

# Run all quality checks
mvn clean verify
```

### 2. Testing Workflow

#### Test-Driven Development
```bash
# 1. Write failing test
# 2. Run test to confirm it fails
mvn test -Dtest=NewFeatureTest

# 3. Implement minimum code to pass
# 4. Run test to confirm it passes
mvn test -Dtest=NewFeatureTest

# 5. Refactor and repeat
```

#### Continuous Testing
```bash
# Use Maven Surefire plugin for continuous testing
mvn surefire:test -Dsurefire.rerunFailingTestsCount=2

# Or use IDE continuous testing features
```

### 3. Local CI/CD Simulation

#### Pre-commit Hooks
```bash
# Install pre-commit hooks
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
echo "Running pre-commit checks..."

# Run tests
mvn test
if [ $? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi

# Run code quality checks
mvn spotless:check
if [ $? -ne 0 ]; then
    echo "Code formatting issues. Run 'mvn spotless:apply' and try again."
    exit 1
fi

echo "Pre-commit checks passed."
EOF

chmod +x .git/hooks/pre-commit
```

#### Local Pipeline Simulation
```bash
# Create local pipeline script
cat > scripts/local-pipeline.sh << 'EOF'
#!/bin/bash
set -e

echo "=== Local CI/CD Pipeline Simulation ==="

echo "1. Code Quality Checks..."
mvn spotless:check
mvn spotbugs:check

echo "2. Unit Tests..."
mvn test

echo "3. Integration Tests..."
mvn test -Pintegration-tests

echo "4. Build Application..."
mvn clean package -DskipTests

echo "5. Security Scan..."
mvn org.owasp:dependency-check-maven:check

echo "6. Deploy to Local..."
./scripts/deploy-local.sh

echo "7. Health Check..."
curl -f http://localhost:8080/health
curl -f http://localhost:8081/api/health

echo "=== Pipeline Completed Successfully ==="
EOF

chmod +x scripts/local-pipeline.sh
```

### 4. Troubleshooting Common Issues

#### Port Conflicts
```bash
# Check what's using ports
lsof -i :8080
lsof -i :8081
lsof -i :3306

# Kill processes if needed
kill -9 $(lsof -t -i:8080)
```

#### Database Connection Issues
```bash
# Test database connectivity
mysql -u petclinic -ppetclinic123 -h localhost -e "SELECT 1"

# Check MySQL service status
systemctl status mysql  # Linux
brew services list | grep mysql  # macOS

# Test MySQL privilege configuration
mysql-privilege-config validate --environment local --verbose

# Fix privilege issues if any
mysql-privilege-config fix-privileges --user petclinic --environment local
```

#### Memory Issues
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"

# Monitor JVM memory usage
jstat -gc -t $(jps | grep Application | cut -d' ' -f1) 5s
```

This local development setup provides a comprehensive environment for developing, testing, and debugging the Pet Clinic CI/CD Pipeline application.