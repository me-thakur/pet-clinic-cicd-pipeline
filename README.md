# Pet Clinic CI/CD Pipeline

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/your-org/pet-clinic-cicd-pipeline)
[![Security](https://img.shields.io/badge/security-hardened-blue)](https://github.com/your-org/pet-clinic-cicd-pipeline)
[![AWS](https://img.shields.io/badge/AWS-ready-orange)](https://aws.amazon.com/)
[![Jenkins](https://img.shields.io/badge/Jenkins-automated-red)](https://jenkins.io/)

A comprehensive, enterprise-grade CI/CD pipeline for deploying a pet clinic management system on AWS using Jenkins, CloudFormation, and modern DevOps practices.

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   GitHub Repo   │───▶│  Jenkins Server  │───▶│   AWS Cloud     │
│                 │    │                  │    │                 │
│ • Source Code   │    │ • CI/CD Pipeline │    │ • EC2 Instances │
│ • Jenkinsfiles  │    │ • Automated      │    │ • RDS Database  │
│ • Tests         │    │   Testing        │    │ • S3 Storage    │
└─────────────────┘    │ • Deployments    │    │ • CloudWatch    │
                       └──────────────────┘    └─────────────────┘
```

### Key Components

- **🏥 Pet Clinic Application**: Java Spring Boot frontend and backend
- **🔧 Jenkins CI/CD**: Automated build, test, and deployment pipeline
- **☁️ AWS Infrastructure**: CloudFormation-managed cloud resources
- **🗄️ MySQL Privilege Configuration**: Automated database security management system
- **🔒 Security**: HTTPS, secrets management, and hardening
- **📊 Monitoring**: CloudWatch metrics, logging, and alerting
- **💾 Backup**: Automated backup and disaster recovery

## 🚀 Quick Start

### Prerequisites

- AWS Account with appropriate permissions
- GitHub repository access
- Local development environment (Java 11, Maven, Git)

### 1-Minute Setup

```bash
# Clone the repository
git clone https://github.com/your-org/pet-clinic-cicd-pipeline.git
cd pet-clinic-cicd-pipeline

# Configure AWS credentials
aws configure

# Set environment variables
export GITHUB_TOKEN="your-github-token"
export JENKINS_ADMIN_PASSWORD="secure-password"
export DB_PASSWORD="database-password"

# Install MySQL privilege configuration system
pip install -e mysql_privilege_config/

# Deploy infrastructure
./scripts/deploy-infrastructure.sh

# Access Jenkins
echo "Jenkins URL: http://$(aws cloudformation describe-stacks --stack-name pet-clinic-master --query 'Stacks[0].Outputs[?OutputKey==`JenkinsURL`].OutputValue' --output text)"
```

## 📋 Features

### ✅ Complete CI/CD Pipeline
- **Automated Builds**: Triggered by GitHub webhooks
- **Comprehensive Testing**: Unit tests, integration tests, property-based tests
- **Multi-Environment**: Development, staging, and production deployments
- **Rollback Capability**: Automatic rollback on deployment failures

### ✅ Enterprise Infrastructure
- **High Availability**: Multi-AZ deployment with auto-scaling
- **Database Security**: Automated MySQL privilege management across environments
- **Security**: VPC isolation, security groups, encrypted storage
- **Monitoring**: CloudWatch dashboards, alarms, and notifications
- **Backup**: Automated backups with retention policies

### ✅ Production-Ready Application
- **Pet Management**: Complete CRUD operations for pets, owners, visits
- **Veterinarian System**: Appointment scheduling and medical records
- **Search & Filter**: Advanced search capabilities
- **Authentication**: Secure user authentication and authorization

### ✅ DevOps Excellence
- **Infrastructure as Code**: CloudFormation templates
- **Configuration as Code**: Jenkins JCasC
- **Automated Testing**: 15+ property-based tests
- **Security Hardening**: CIS benchmarks compliance

## 📁 Project Structure

```
pet-clinic-cicd-pipeline/
├── 📁 cloudformation/          # AWS CloudFormation templates
│   ├── master-stack.yaml       # Main orchestration template
│   ├── network.yaml            # VPC and networking
│   ├── compute.yaml            # EC2 and load balancers
│   ├── database.yaml           # RDS configuration
│   ├── storage.yaml            # S3 and EFS setup
│   └── iam.yaml                # IAM roles and policies
├── 📁 mysql_privilege_config/  # MySQL privilege management system
│   ├── cli.py                  # Command-line interface
│   ├── core/                   # Core models and exceptions
│   ├── detectors/              # Environment detection
│   ├── managers/               # Configuration management
│   ├── validators/             # Privilege validation
│   ├── executors/              # Script execution
│   ├── utils/                  # Utilities and error handling
│   └── config/                 # Environment configurations
├── 📁 jenkins-config/          # Jenkins configuration
│   ├── jenkins-casc.yaml       # Configuration as Code
│   ├── plugins.txt             # Required plugins
│   ├── install-jenkins.sh      # Installation script
│   └── backup-*.sh             # Backup management
├── 📁 jenkins-pipelines/       # CI/CD pipeline definitions
│   ├── Jenkinsfile-backend     # Backend pipeline
│   ├── Jenkinsfile-frontend    # Frontend pipeline
│   └── setup-webhooks.sh       # GitHub integration
├── 📁 pet-clinic-app/          # Application source code
│   ├── pet-clinic-backend/     # Spring Boot backend
│   ├── pet-clinic-frontend/    # Spring Boot frontend
│   └── pom.xml                 # Maven parent POM
├── 📁 deployment-scripts/      # Deployment automation
│   ├── deploy-backend.sh       # Backend deployment
│   ├── deploy-frontend.sh      # Frontend deployment
│   ├── health-check.sh         # Health validation
│   └── rollback.sh             # Rollback procedures
├── 📁 monitoring-config/       # Monitoring and alerting
│   ├── cloudwatch-alarms.yaml  # CloudWatch alarms
│   ├── cloudwatch-dashboard.yaml # Monitoring dashboard
│   └── setup-monitoring.sh     # Monitoring setup
├── 📁 security-config/         # Security hardening
│   ├── ssl-certificate-setup.sh # HTTPS configuration
│   ├── security-hardening.sh   # System hardening
│   └── secrets-management.sh   # Secrets management
├── 📁 tests/                   # Comprehensive test suite
│   ├── jenkins/                # Jenkins pipeline tests
│   ├── infrastructure/         # Infrastructure tests
│   ├── integration/            # End-to-end tests
│   └── security/               # Security validation
├── 📁 docs/                    # Documentation
│   ├── DEPLOYMENT.md           # Deployment guide
│   ├── ARCHITECTURE.md         # Architecture details
│   ├── MYSQL-PRIVILEGE-CONFIGURATION.md # MySQL security guide
│   ├── SECURITY-TROUBLESHOOTING.md # Security troubleshooting
│   └── TROUBLESHOOTING.md      # Common issues
└── 📄 README.md                # This file
```

## 🛠️ Technology Stack

### Infrastructure & DevOps
- **Cloud Platform**: AWS (EC2, RDS, S3, CloudWatch, EFS)
- **Infrastructure as Code**: CloudFormation
- **CI/CD**: Jenkins with Blue Ocean
- **Configuration Management**: Jenkins Configuration as Code (JCasC)
- **Monitoring**: CloudWatch, SNS, custom metrics

### Application Stack
- **Backend**: Java 11, Spring Boot, Spring Data JPA
- **Frontend**: Spring Boot, Thymeleaf, Bootstrap
- **Database**: MySQL 8.0 (AWS RDS) with automated privilege management
- **Build Tool**: Maven
- **Testing**: JUnit, jqwik (property-based testing)

### Security & Operations
- **Security**: HTTPS/TLS, AWS IAM, security groups, MySQL privilege management
- **Secrets Management**: AWS Systems Manager Parameter Store
- **Database Security**: Environment-specific privilege configurations
- **Backup**: Jenkins ThinBackup, S3 synchronization
- **Logging**: Logback, CloudWatch Logs
- **Alerting**: SNS, email, Slack integration

## 📖 Documentation

- **[Deployment Guide](docs/DEPLOYMENT.md)** - Complete deployment instructions
- **[Architecture Guide](docs/ARCHITECTURE.md)** - Detailed system architecture
- **[MySQL Privilege Configuration](docs/MYSQL-PRIVILEGE-CONFIGURATION.md)** - Database security management
- **[Local Development](docs/LOCAL-DEVELOPMENT.md)** - Local setup and development
- **[Cloud Deployment](docs/CLOUD-DEPLOYMENT.md)** - AWS cloud deployment
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions
- **[Security Troubleshooting](docs/SECURITY-TROUBLESHOOTING.md)** - MySQL security issues
- **[API Documentation](docs/API.md)** - REST API reference
- **[Security Guide](docs/SECURITY.md)** - Security best practices

## 🚀 Deployment Options

### Option 1: Quick Cloud Deployment
```bash
# Deploy everything to AWS
./scripts/quick-deploy.sh
```

### Option 2: Local Development
```bash
# Run locally for development
./scripts/local-setup.sh
```

### Option 3: Step-by-Step Deployment
```bash
# Infrastructure first
./scripts/deploy-infrastructure.sh

# Application deployment
./scripts/deploy-application.sh

# Configure monitoring
./scripts/setup-monitoring.sh
```

## 🗄️ MySQL Privilege Configuration

The system includes an automated MySQL privilege management component that ensures proper database security across different environments.

### Environment-Specific Security

| Environment | Security Level | log_bin_trust_function_creators | Use Case |
|-------------|----------------|--------------------------------|----------|
| **Local Development** | Permissive | ON | Rapid development and testing |
| **CI/CD** | Balanced | ON | Automated testing with security |
| **Production** | Restrictive | OFF | Maximum security compliance |

### Key Features

- **🔍 Environment Detection**: Automatically identifies deployment environment
- **⚙️ Configuration Management**: Template-based security configurations
- **✅ Privilege Validation**: Validates MySQL privileges and reports issues
- **🔧 Script Execution**: Executes database scripts with retry logic
- **🚨 Error Handling**: Comprehensive error resolution strategies

### Quick Usage

```bash
# Install the MySQL privilege configuration system
pip install -e mysql_privilege_config/

# Configure for your environment
mysql-privilege-config configure --environment production \
  --host your-db-host \
  --user petclinic \
  --password your-password

# Validate configuration
mysql-privilege-config validate --environment production

# Apply security settings
mysql-privilege-config apply-security --environment production
```

### CLI Commands

```bash
# Environment detection
mysql-privilege-config detect-environment

# Configuration management
mysql-privilege-config configure --environment [local|ci|production]

# Privilege validation
mysql-privilege-config validate --environment production --verbose

# Security application
mysql-privilege-config apply-security --environment production

# Function testing
mysql-privilege-config test-functions --environment local

# Status checking
mysql-privilege-config status --environment production
```

## 🧪 Testing

The project includes comprehensive testing at multiple levels:

### Property-Based Testing
- **15 Property Tests** validating system correctness
- **Automated Test Generation** with Hypothesis/jqwik
- **Statistical Confidence** with configurable iterations

### Integration Testing
- **End-to-End Pipeline Tests**
- **Infrastructure Validation**
- **Security Penetration Testing**

### Run Tests
```bash
# Run all tests
./scripts/run-tests.sh

# Run specific test categories
./scripts/run-tests.sh --category property-based
./scripts/run-tests.sh --category integration
./scripts/run-tests.sh --category security
```

## 📊 Monitoring & Observability

### CloudWatch Integration
- **Infrastructure Metrics**: CPU, memory, disk, network
- **Application Metrics**: Response times, error rates, throughput
- **Custom Metrics**: Business KPIs and operational metrics

### Alerting
- **Multi-Channel Notifications**: Email, Slack, PagerDuty
- **Escalation Policies**: Tiered alerting based on severity
- **Automated Recovery**: Self-healing for common issues

### Dashboards
- **Operational Dashboard**: Real-time system health
- **Business Dashboard**: Pet clinic operational metrics
- **Security Dashboard**: Security events and compliance

## 🔒 Security

### Security Features
- **Network Security**: VPC isolation, security groups, NACLs
- **Database Security**: Automated MySQL privilege management and validation
- **Data Encryption**: At rest and in transit
- **Access Control**: IAM roles, least privilege principle
- **Secrets Management**: Encrypted parameter store
- **Security Monitoring**: CloudTrail, GuardDuty integration

### Compliance
- **CIS Benchmarks**: System hardening compliance
- **OWASP Top 10**: Web application security
- **AWS Security Best Practices**: Cloud security compliance

## 💾 Backup & Disaster Recovery

### Automated Backups
- **Jenkins Configuration**: Daily automated backups
- **Database Backups**: RDS automated backups with point-in-time recovery
- **Application Data**: S3 cross-region replication
- **Retention Policies**: 30 daily, 12 weekly, 12 monthly

### Disaster Recovery
- **RTO**: Recovery Time Objective < 4 hours
- **RPO**: Recovery Point Objective < 1 hour
- **Automated Recovery**: Scripts for rapid restoration
- **Testing**: Regular DR testing procedures

## 🤝 Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Development Guidelines
- Follow Java coding standards
- Write comprehensive tests
- Update documentation
- Ensure security compliance

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Getting Help
- **Documentation**: Check the [docs/](docs/) directory
- **Issues**: Open a GitHub issue
- **Discussions**: Use GitHub Discussions for questions

### Common Issues
- **AWS Permissions**: Ensure proper IAM permissions
- **Jenkins Plugins**: Check plugin compatibility
- **Network Connectivity**: Verify security group rules
- **MySQL Privileges**: Use `mysql-privilege-config validate` for database issues

## 🏆 Acknowledgments

- **Spring Boot Team** for the excellent framework
- **Jenkins Community** for the robust CI/CD platform
- **AWS** for reliable cloud infrastructure
- **Open Source Community** for the amazing tools and libraries

## 📈 Project Status

- ✅ **Infrastructure**: Complete and tested
- ✅ **Application**: Fully functional pet clinic system
- ✅ **CI/CD Pipeline**: Automated build, test, deploy
- ✅ **MySQL Privilege Management**: Automated database security across environments
- ✅ **Security**: Hardened and compliant
- ✅ **Monitoring**: Comprehensive observability
- ✅ **Documentation**: Complete guides and references

---

**Built with ❤️ for the DevOps community**

For detailed deployment instructions, see [DEPLOYMENT.md](docs/DEPLOYMENT.md).