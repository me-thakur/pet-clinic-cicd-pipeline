-v
docker-compose -f docker-compose-ci.yml up -d
```

For detailed documentation, see MYSQL-PRIVILEGE-CONFIGURATION.mdnable it:
SET GLOBAL log_bin_trust_function_creators = 1;

-- Make permanent in my.cnf:
log_bin_trust_function_creators = 1
```

### Configuration Not Applied
```bash
# Check MySQL is reading config file
mysql --help --verbose | grep -A 1 "Default options"

# Restart MySQL after config changes
sudo systemctl restart mysql
```

### Docker Container Issues
```bash
# Check container logs
docker-compose -f docker-compose-ci.yml logs mysql-ci

# Restart with clean state
docker-compose -f docker-compose-ci.yml down 
### CI/CD (Balanced)
- `log_bin_trust_function_creators = 1` ✅
- Automated configuration via Docker
- Health checks and monitoring
- Ephemeral database instances

### Production (Restrictive)
- `log_bin_trust_function_creators = 0` (most secure)
- SSL/TLS encryption required
- Comprehensive audit logging
- Strict privilege controls

## Troubleshooting Common Issues

### ERROR 1419 Still Occurs
```sql
-- Check setting
SELECT @@log_bin_trust_function_creators;

-- If 0, ese64 32)" \
  --namespace=production

# 2. Deploy
kubectl apply -f mysql_privilege_config/config/kubernetes-configmap.yaml

# 3. Validate
kubectl port-forward service/mysql-privilege-production 3306:3306 --namespace=production &
./validate-config.sh --host localhost --environment production
```

## Configuration Templates

### Development (Permissive)
- `log_bin_trust_function_creators = 1` ✅
- `bind_address = 127.0.0.1` (localhost only)
- Enhanced logging for debugging
- Relaxed security for development ease
ge_config/config/validate-config.sh --environment local
```

### CI/CD with Docker
```bash
# 1. Start CI environment
cd mysql_privilege_config/config
docker-compose -f docker-compose-ci.yml up -d

# 2. Validate
./validate-config.sh --host localhost --user ci_user --password ci_password --environment ci
```

### Production with Kubernetes
```bash
# 1. Create namespace and secrets
kubectl create namespace production
kubectl create secret generic mysql-credentials \
  --from-literal=root-password="$(openssl rand -ba
# 1. Copy configuration
cp mysql_privilege_config/config/local-development.cnf /usr/local/etc/my.cnf

# 2. Restart MySQL
brew services restart mysql  # macOS
sudo systemctl restart mysql  # Linux

# 3. Validate
./mysql_privilee Configuration - Deployment Setup Guide

## Quick Start by Environment

### Local Development
```bash# MySQL Privileg