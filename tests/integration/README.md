# Integration Tests for Pet Clinic CI/CD Pipeline

This directory contains comprehensive integration tests that validate the complete CI/CD pipeline functionality from end-to-end.

## Test Coverage

### End-to-End Pipeline Test (`test-end-to-end-pipeline.py`)

This comprehensive test suite validates:

1. **Infrastructure Health Verification**
   - CloudFormation stack status
   - EC2 instances running state
   - RDS database availability
   - S3 backup bucket accessibility
   - Jenkins server responsiveness

2. **Complete CI/CD Pipeline Workflow**
   - Code push triggers Jenkins build
   - Build and test execution
   - Deployment to target environment
   - Health checks and monitoring
   - Status reporting back to GitHub

3. **Pet Clinic Application Functionality**
   - Owner management (create, search, view)
   - Pet management (create, view details)
   - Visit management (create, track)
   - Authentication and authorization
   - API endpoint functionality

4. **Backup and Restore Procedures**
   - Backup existence in S3
   - Backup integrity verification
   - Restore capability validation

5. **Rollback Scenarios**
   - Automatic rollback on deployment failure
   - Rollback mechanism verification

## Environment Configuration

The tests require the following environment variables:

### Jenkins Configuration
```bash
export JENKINS_URL="http://your-jenkins-server:8080"
export JENKINS_USER="admin"
export JENKINS_TOKEN="your-jenkins-api-token"
```

### GitHub Configuration
```bash
export GITHUB_TOKEN="your-github-personal-access-token"
export TEST_REPO="your-org/pet-clinic-repo"
```

### AWS Configuration
```bash
export AWS_REGION="us-east-1"
export CLOUDFORMATION_STACK="pet-clinic-pipeline"
export S3_BACKUP_BUCKET="pet-clinic-jenkins-backups"
```

### Application Configuration
```bash
export FRONTEND_URL="http://your-frontend-server:8080"
export BACKEND_URL="http://your-backend-server:8081"
```

## Running the Tests

### Prerequisites

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Configure AWS credentials:
```bash
aws configure
# or set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables
```

3. Set up environment variables as described above

### Execute Tests

Run all integration tests:
```bash
pytest test-end-to-end-pipeline.py -v
```

Run specific test:
```bash
pytest test-end-to-end-pipeline.py::test_complete_cicd_pipeline_workflow -v
```

Run with detailed output:
```bash
pytest test-end-to-end-pipeline.py -v -s --tb=long
```

### Test Execution Time

- **Complete E2E Test**: 15-30 minutes (includes full pipeline execution)
- **Rollback Test**: 5-10 minutes
- **Infrastructure Health Check**: 1-2 minutes

## Test Artifacts

The tests create temporary artifacts during execution:

1. **GitHub Repository Changes**
   - Test Java classes
   - Test SQL data files
   - Updated application properties
   - All artifacts are automatically cleaned up after test completion

2. **Application Test Data**
   - Test owners, pets, and visits
   - Cleaned up via API calls after test completion

3. **Jenkins Build History**
   - Test builds remain in Jenkins history for audit purposes
   - Can be manually cleaned up if needed

## Troubleshooting

### Common Issues

1. **Jenkins Connection Timeout**
   - Verify Jenkins URL and credentials
   - Check network connectivity
   - Ensure Jenkins is running and accessible

2. **GitHub API Rate Limiting**
   - Use a GitHub personal access token with appropriate permissions
   - Consider using a dedicated test repository

3. **AWS Permission Errors**
   - Verify AWS credentials have necessary permissions
   - Check CloudFormation, EC2, RDS, and S3 access

4. **Application Health Check Failures**
   - Verify application URLs are correct
   - Check if applications are deployed and running
   - Review application logs for errors

### Debug Mode

Enable debug logging:
```bash
export PYTEST_CURRENT_TEST=1
pytest test-end-to-end-pipeline.py -v -s --log-cli-level=DEBUG
```

### Manual Verification

If automated tests fail, you can manually verify components:

1. **Check Jenkins**: Visit Jenkins URL and verify job status
2. **Check Application**: Access frontend URL and test functionality
3. **Check AWS Resources**: Use AWS Console to verify resource status
4. **Check GitHub**: Verify webhook configuration and commit status

## Test Results Interpretation

### Success Criteria

A successful test run should show:
- ✅ All infrastructure components healthy
- ✅ Pipeline triggered and completed successfully
- ✅ Application functionality tests passed
- ✅ Backup system operational

### Failure Analysis

If tests fail, check:
1. **Infrastructure Issues**: CloudFormation stack status, resource availability
2. **Pipeline Issues**: Jenkins job configuration, build logs
3. **Application Issues**: Deployment status, application logs
4. **Network Issues**: Security groups, connectivity between components

## Continuous Integration

These integration tests can be incorporated into CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
name: Integration Tests
on:
  schedule:
    - cron: '0 2 * * *'  # Run daily at 2 AM
  workflow_dispatch:

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      - name: Install dependencies
        run: pip install -r tests/integration/requirements.txt
      - name: Run integration tests
        env:
          JENKINS_URL: ${{ secrets.JENKINS_URL }}
          JENKINS_TOKEN: ${{ secrets.JENKINS_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: pytest tests/integration/ -v
```

## Contributing

When adding new integration tests:

1. Follow the existing test structure and patterns
2. Include proper cleanup in `finally` blocks
3. Add appropriate assertions and error messages
4. Update this README with new test descriptions
5. Ensure tests are idempotent and can run multiple times

## Security Considerations

- Never commit credentials or tokens to version control
- Use environment variables for all sensitive configuration
- Ensure test cleanup removes any sensitive test data
- Review test logs before sharing to avoid credential exposure