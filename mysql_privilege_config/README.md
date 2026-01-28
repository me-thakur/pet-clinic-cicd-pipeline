# MySQL Privilege Configuration

A comprehensive Python package for managing MySQL SUPER privilege issues in CI/CD pipelines. This package addresses the common ERROR 1419 that occurs when binary logging is enabled and database users lack SUPER privileges for creating functions, procedures, or triggers.

## Features

- **Environment-aware configuration**: Automatically detects and applies appropriate MySQL configuration for local development, CI/CD, and production environments
- **Privilege validation**: Comprehensive checking of MySQL user privileges and system configuration
- **Script execution**: Safe execution of database scripts with privilege handling and error recovery
- **Error handling**: Detailed error messages with actionable resolution steps
- **Property-based testing**: Extensive test coverage using Hypothesis for correctness validation

## Quick Start

```python
from mysql_privilege_config import DatabaseConfigManager, Environment

# Create configuration manager
config_manager = DatabaseConfigManager()

# Detect environment and apply configuration
env = config_manager.detect_environment()
result = config_manager.apply_configuration(env)

if result.success:
    print("MySQL configuration applied successfully!")
else:
    print("Configuration failed:", result.errors)
```

## Architecture

The package follows a layered architecture with clear separation of concerns:

- **Core Models**: Data structures and enums (`mysql_privilege_config.core`)
- **Environment Detection**: Automatic environment identification (`mysql_privilege_config.detectors`)
- **Privilege Validation**: MySQL privilege checking (`mysql_privilege_config.validators`)
- **Configuration Management**: Central orchestration (`mysql_privilege_config.managers`)
- **Script Execution**: Database script handling (`mysql_privilege_config.executors`)
- **Utilities**: Logging and error handling (`mysql_privilege_config.utils`)

## Environment Support

### Local Development
- Permissive configuration for ease of development
- `log_bin_trust_function_creators = ON`
- Binary logging typically disabled

### CI/CD
- Balanced security with automation requirements
- Automated privilege configuration
- Comprehensive error handling and retry logic

### Production
- Restrictive security configuration
- Manual privilege grants preferred
- Detailed audit logging

## Installation

```bash
pip install mysql-privilege-config
```

For development:
```bash
pip install mysql-privilege-config[dev]
```

## Testing

The package includes comprehensive test coverage:

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=mysql_privilege_config

# Run property-based tests only
pytest -m property
```

## Requirements

- Python 3.8+
- MySQL 5.7+ or 8.0+
- mysql-connector-python or PyMySQL

## License

MIT License - see LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## Support

For issues and questions:
- Check the documentation
- Review error messages and resolution steps
- Open an issue on GitHub