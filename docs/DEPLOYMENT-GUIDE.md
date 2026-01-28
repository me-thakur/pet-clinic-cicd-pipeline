# MySQL Privilege Configuration - Deployment Guide

## Environment-Specific Setup

### Local Development
```bash
# Copy configuration template
cp mysql_privilege_config/config/local-development.cnf /usr/local/etc/my.cnf

# Restart MySQL service
brew services restart mysql  # macOS
sudo systemctl restart mysql  # Linux

# Validate configuration
./mysql_privilege_config/config/validate-config.sh --environment local
```

### CI/CD Environment
```bash
# Start Docker Compose environment
cd mysql_privilege_config/config
docker-compose -f docker-compose-ci.yml up -d

# Validate CI configuration
./validate-config.sh --host localhost --user ci_user --environment ci
```

### Production Environment
```bash
# Create Kubernetes namespace
kubectl create namespace production

# Create secure credentials
kubectl create secret generic mysql-credentials \
  --from-literal=root-password="$(openssl rand -base64 32)" \
  --namespace=production

# Deploy configuration
kubectl apply -f mysql_privilege_config/config/kubernetes-configmap.yaml

# Validate production setup
kubectl port-forward service/mysql-privilege-production 3306:3306 --namespace=production &
./validate-config.sh --host localhost --environment production
```

## Security Configuration Summary

| Environment | log_bin_trust_function_creators | Security Level | Use Case |
|-------------|--------------------------------|----------------|----------|
| Development | 1 (ON) | Permissive | Local development ease |
| CI/CD | 1 (ON) | Balanced | Automated testing |
| Production | 0 (OFF) | Restrictive | Maximum security |

## Quick Troubleshooting

### ERROR 1419 Resolution
```sql
-- Check current setting
SELECT @@log_bin_trust_function_creators;

-- Enable if needed (development/CI)
SET GLOBAL log_bin_trust_function_creators = 1;
```

### Configuration Validation
```bash
# Run environment-specific validation
./validate-config.sh --environment [local|ci|production]

# Test function creation
mysql -e "CREATE FUNCTION test_func(x INT) RETURNS INT DETERMINISTIC BEGIN RETURN x*2; END;"
mysql -e "SELECT test_func(5); DROP FUNCTION test_func;"
```

For comprehensive documentation, see MYSQL-PRIVILEGE-CONFIGURATION.md