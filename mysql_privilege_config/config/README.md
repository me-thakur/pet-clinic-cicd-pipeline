# MySQL Configuration Templates

This directory contains MySQL configuration templates for different environments, designed to address MySQL SUPER privilege issues while maintaining appropriate security levels for each deployment context.

## Overview

The configuration templates solve the common ERROR 1419 that occurs when creating functions, procedures, or triggers in MySQL with binary logging enabled but without SUPER privileges. The solution uses the `log_bin_trust_function_creators` system variable with environment-appropriate security settings.

## Configuration Files

### Local Development
- **`local-development.cnf`** - Main configuration for local development
- **`security/local-development-security.cnf`** - Security settings for development

**Key Features:**
- `log_bin_trust_function_creators = 1` (allows function creation)
- Permissive settings for development ease
- Local-only network access (127.0.0.1)
- Enhanced logging for debugging
- Relaxed password policies

### CI/CD Environment
- **`docker-compose-ci.yml`** - Docker Compose setup for CI/CD
- **`mysql-ci.cnf`** - MySQL configuration for CI containers
- **`security/ci-security.cnf`** - Security settings for CI/CD
- **`init-scripts/`** - Database initialization scripts

**Key Features:**
- `log_bin_trust_function_creators = 1` (enables automated testing)
- Container-optimized settings
- Health checks and monitoring
- Automated user creation for testing
- Balanced security for CI environment

### Production Environment
- **`kubernetes-configmap.yaml`** - Kubernetes ConfigMap with production config
- **`security/production-security.cnf`** - Maximum security settings

**Key Features:**
- `log_bin_trust_function_creators = 0` (most secure, requires SUPER privilege)
- Alternative relaxed configuration available
- SSL/TLS encryption required
- Strict password policies
- Comprehensive audit logging
- Resource limits and monitoring

## Usage Instructions

### Local Development Setup

1. **Copy the configuration file:**
   ```bash
   cp mysql_privilege_config/config/local-development.cnf /usr/local/etc/my.cnf
   # Or on Linux: /etc/mysql/my.cnf
   ```

2. **Restart MySQL:**
   ```bash
   brew services restart mysql  # macOS with Homebrew
   # Or: sudo systemctl restart mysql  # Linux
   ```

3. **Verify the setting:**
   ```sql
   SELECT @@log_bin_trust_function_creators;
   ```

### CI/CD Docker Setup

1. **Use the Docker Compose configuration:**
   ```bash
   cd mysql_privilege_config/config
   docker-compose -f docker-compose-ci.yml up -d
   ```

2. **Wait for health check:**
   ```bash
   docker-compose -f docker-compose-ci.yml ps
   ```

3. **Run your tests:**
   ```bash
   docker-compose -f docker-compose-ci.yml exec app-test python -m pytest
   ```

### Kubernetes Production Deployment

1. **Create the namespace:**
   ```bash
   kubectl create namespace production
   ```

2. **Apply the configuration:**
   ```bash
   kubectl apply -f mysql_privilege_config/config/kubernetes-configmap.yaml
   ```

3. **Update passwords in the secret:**
   ```bash
   # Generate base64 encoded passwords
   echo -n "your_secure_password" | base64
   
   # Edit the secret in the YAML file with actual passwords
   kubectl apply -f mysql_privilege_config/config/kubernetes-configmap.yaml
   ```

## Security Considerations

### log_bin_trust_function_creators Setting

This system variable controls whether MySQL allows creation of functions, procedures, and triggers without SUPER privileges when binary logging is enabled.

**Setting = 1 (Permissive):**
- ✅ Allows function creation without SUPER privileges
- ✅ Resolves ERROR 1419 in CI/CD pipelines
- ⚠️ May allow creation of non-deterministic functions
- ⚠️ Could cause replication inconsistencies if not carefully managed

**Setting = 0 (Restrictive):**
- ✅ Maximum security - requires SUPER privilege
- ✅ Prevents unauthorized function creation
- ❌ Requires SUPER privileges for application users
- ❌ May break CI/CD pipelines without privilege grants

### Environment-Specific Recommendations

#### Local Development
- Use `log_bin_trust_function_creators = 1`
- Enable comprehensive logging for debugging
- Allow local file operations for data import/export
- Use relaxed password policies for convenience

#### CI/CD
- Use `log_bin_trust_function_creators = 1`
- Create dedicated test users with minimal required privileges
- Enable health checks and monitoring
- Use container isolation for security
- Rotate passwords regularly

#### Production
- **Recommended:** `log_bin_trust_function_creators = 0` with SUPER privileges for specific users
- **Alternative:** `log_bin_trust_function_creators = 1` with strict monitoring and auditing
- Require SSL/TLS encryption
- Implement strong password policies
- Enable comprehensive audit logging
- Regular security reviews and privilege audits

## Testing the Configuration

### Verify Function Creation

```sql
-- Test function creation (should work with log_bin_trust_function_creators = 1)
DELIMITER $$
CREATE FUNCTION test_function(x INT) 
RETURNS INT 
DETERMINISTIC 
READS SQL DATA
BEGIN 
    RETURN x * 2; 
END$$
DELIMITER ;

-- Test the function
SELECT test_function(5); -- Should return 10

-- Clean up
DROP FUNCTION test_function;
```

### Check Configuration Status

```sql
-- Verify key settings
SELECT @@log_bin_trust_function_creators as function_creators;
SELECT @@log_bin as binary_logging;
SELECT @@binlog_format as binlog_format;

-- Check user privileges
SHOW GRANTS FOR CURRENT_USER();

-- List created routines
SELECT ROUTINE_NAME, ROUTINE_TYPE, DEFINER 
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE ROUTINE_SCHEMA = DATABASE();
```

## Troubleshooting

### Common Issues

1. **ERROR 1419: You do not have the SUPER privilege and binary logging is enabled**
   - **Solution:** Set `log_bin_trust_function_creators = 1` or grant SUPER privilege
   - **Check:** `SELECT @@log_bin_trust_function_creators;`

2. **Configuration not taking effect**
   - **Solution:** Restart MySQL server after configuration changes
   - **Check:** Verify configuration file location and syntax

3. **Docker container fails to start**
   - **Solution:** Check Docker logs for configuration errors
   - **Command:** `docker-compose logs mysql-ci`

4. **Kubernetes pod fails to start**
   - **Solution:** Check ConfigMap and Secret configurations
   - **Commands:** 
     ```bash
     kubectl describe pod mysql-privilege-production-0
     kubectl logs mysql-privilege-production-0
     ```

### Validation Commands

```bash
# Test MySQL connection
mysql -h localhost -u root -p -e "SELECT 1"

# Check MySQL error log
tail -f /var/log/mysql/error.log

# Verify Docker container health
docker-compose ps
docker-compose logs mysql-ci

# Check Kubernetes resources
kubectl get pods -n production
kubectl describe configmap mysql-privilege-config -n production
```

## Best Practices

1. **Environment Isolation:** Use different configurations for each environment
2. **Principle of Least Privilege:** Grant only necessary privileges to application users
3. **Regular Audits:** Review user privileges and created database objects regularly
4. **Monitoring:** Monitor slow queries and error logs for security issues
5. **Backup and Recovery:** Ensure binary logs are included in backup strategies
6. **SSL/TLS:** Use encrypted connections in production environments
7. **Password Management:** Use strong passwords and rotate them regularly

## References

- [MySQL 8.0 Reference Manual - Binary Logging](https://dev.mysql.com/doc/refman/8.0/en/binary-log.html)
- [MySQL 8.0 Reference Manual - Stored Program Binary Logging](https://dev.mysql.com/doc/refman/8.0/en/stored-programs-logging.html)
- [MySQL 8.0 Reference Manual - Server System Variables](https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_log_bin_trust_function_creators)

## Support

For issues related to these configurations:
1. Check the troubleshooting section above
2. Review MySQL error logs
3. Verify environment-specific requirements
4. Consult the MySQL documentation for your specific version