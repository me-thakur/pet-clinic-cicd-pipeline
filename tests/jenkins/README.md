# Jenkins Property-Based Tests

This directory contains property-based tests for validating Jenkins CI/CD pipeline correctness properties.

## Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Set environment variables:
```bash
export JENKINS_URL="http://your-jenkins-server:8080"
export JENKINS_USER="admin"
export JENKINS_TOKEN="your-jenkins-api-token"
export GITHUB_TOKEN="your-github-personal-access-token"
export TEST_REPO="your-org/your-test-repo"

# For deployment health and rollback tests
export BACKEND_URL="http://localhost:8080/api"
export FRONTEND_URL="http://localhost:8081"
export DB_HOST="your-db-host"
export DB_USER="petclinic"
export DB_PASSWORD="your-db-password"
export DB_NAME="petclinic"
export DEPLOY_DIR="/opt/pet-clinic"
export BACKUP_DIR="/opt/pet-clinic/backups"
export HEALTH_CHECK_SCRIPT="./deployment-scripts/health-check.sh"
export ROLLBACK_SCRIPT="./deployment-scripts/rollback.sh"
```

## Running Tests

### Property 1: Jenkins GitHub Integration Workflow

Tests that code pushes to main branch automatically trigger Jenkins builds and report status back to GitHub.

```bash
# Run property-based test
python -m pytest tests/jenkins/test-github-integration.py::test_jenkins_github_integration_workflow -v

# Run manual test case
python -m pytest tests/jenkins/test-github-integration.py::test_jenkins_github_integration_manual -v
```

### Property 2: Pull Request Validation

Tests that pull requests trigger Jenkins validation and update PR status.

```bash
# Run property-based test
python -m pytest tests/jenkins/test-pull-request-validation.py::test_jenkins_pull_request_validation_workflow -v

# Run manual test case
python -m pytest tests/jenkins/test-pull-request-validation.py::test_jenkins_pull_request_validation_manual -v
```

### Property 7: Deployment Health Validation

Tests that deployments perform comprehensive health checks before completion.

```bash
# Run property-based test
python -m pytest tests/jenkins/test-deployment-health-validation.py::test_deployment_health_validation_property -v

# Run manual test case
python -m pytest tests/jenkins/test-deployment-health-validation.py::test_deployment_health_validation_manual -v

# Test health check component coverage
python -m pytest tests/jenkins/test-deployment-health-validation.py::test_health_check_component_coverage -v
```

### Property 8: Automatic Rollback on Failure

Tests that failed deployments trigger automatic rollback to previous working version.

```bash
# Run property-based test
python -m pytest tests/jenkins/test-automatic-rollback.py::test_automatic_rollback_on_failure_property -v

# Run manual test case
python -m pytest tests/jenkins/test-automatic-rollback.py::test_automatic_rollback_manual -v

# Test rollback script availability
python -m pytest tests/jenkins/test-automatic-rollback.py::test_rollback_script_availability -v
```

### Run All Jenkins Tests

```bash
# Run all Jenkins property-based tests
python -m pytest tests/jenkins/ -v

# Run with specific markers
python -m pytest tests/jenkins/ -v -k "manual"  # Only manual tests
python -m pytest tests/jenkins/ -v -k "property"  # Only property tests
```

## Environment Variables

### Required for All Tests
- `JENKINS_URL`: URL of your Jenkins server (default: http://localhost:8080)
- `JENKINS_USER`: Jenkins username (default: admin)
- `JENKINS_TOKEN`: Jenkins API token for authentication

### Required for GitHub Integration Tests
- `GITHUB_TOKEN`: GitHub personal access token with repo permissions
- `TEST_REPO`: GitHub repository in format "owner/repo" for testing

### Required for Deployment Tests
- `BACKEND_URL`: Backend application URL (default: http://localhost:8080/api)
- `FRONTEND_URL`: Frontend application URL (default: http://localhost:8081)
- `DB_HOST`: Database host (default: localhost)
- `DB_USER`: Database username (default: petclinic)
- `DB_PASSWORD`: Database password
- `DB_NAME`: Database name (default: petclinic)
- `DEPLOY_DIR`: Application deployment directory (default: /opt/pet-clinic)
- `BACKUP_DIR`: Backup files directory (default: /opt/pet-clinic/backups)
- `HEALTH_CHECK_SCRIPT`: Path to health check script
- `ROLLBACK_SCRIPT`: Path to rollback script

## Test Properties

The tests validate the following correctness properties:

1. **Jenkins GitHub Integration Workflow**: For any code push to the main branch, Jenkins should automatically trigger a build, execute the pipeline, and report the final status back to GitHub

2. **Pull Request Validation**: For any pull request created in the repository, Jenkins should run validation tests and update the PR status with pass/fail results

3. **Deployment Health Validation**: For any successful deployment, the health check process should verify that all deployed components are responding correctly before marking the deployment as complete

4. **Automatic Rollback on Failure**: For any deployment that fails health checks or encounters errors, the system should automatically rollback to the previous working version

## Notes

- Tests create temporary commits and clean them up automatically
- Property-based tests run with reduced examples due to time constraints
- Manual tests provide simple verification of functionality
- Tests require configured Jenkins server with GitHub integration
- Deployment tests require running pet clinic application instances
- Some tests may be skipped if required services are not available