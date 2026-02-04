# Pet Clinic Configuration Guide

## Overview

This document provides comprehensive configuration guidance for the Pet Clinic application, covering environment setup, security configuration, and deployment options.

## Environment Configuration

### Required Environment Variables

The following environment variables must be configured for production deployment:

#### Database Configuration
```bash
DB_HOST=your_database_host
DB_PORT=3306
DB_NAME=petclinicdb
DB_USERNAME=petclinicadmin
DB_PASSWORD=your_secure_password
```

#### Security Configuration (REQUIRED)
```bash
# JWT Secret - MUST be configured, no default provided
JWT_SECRET=your_jwt_secret_key_at_least_256_bits_long_for_security_purposes
JWT_EXPIRATION=3600000  # 1 hour in milliseconds

# Session timeout
SESSION_TIMEOUT=1800000  # 30 minutes in milliseconds
```

#### CORS Configuration
```bash
# Comma-separated list of allowed origins
CORS_ALLOWED_ORIGINS=http://localhost:8080,https://your-frontend-domain.com
```

#### Backend URL Configuration
```bash
# Frontend needs to know backend URL
BACKEND_URL=http://localhost:9090  # or https://your-backend-domain.com
```

### Environment Setup

1. Copy the environment template:
   ```bash
   cp .env.template .env
   ```

2. Edit `.env` file with your specific values

3. Source the environment file (for local development):
   ```bash
   source .env
   ```

## Profile Configuration

The application supports three profiles:

### Development Profile (`dev`)
- Uses H2 in-memory database
- Flyway migrations enabled
- Debug logging enabled
- HTTPS disabled
- Compression disabled

### Staging Profile (`staging`)
- Uses MySQL database
- Flyway migrations enabled
- Info level logging
- HTTPS optional

### Production Profile (`prod`)
- Uses MySQL database with SSL
- Flyway migrations enabled
- Warn level logging
- HTTPS enabled (requires SSL certificates)
- Compression enabled

## Security Configuration

### JWT Configuration

JWT tokens are used for authentication. The JWT secret MUST be configured via environment variable:

```yaml
security:
  jwt:
    secret: ${JWT_SECRET}  # No default - must be explicitly configured
    expiration: 86400000   # 24 hours
```

**Important**: Never use default JWT secrets in production. Generate a secure random key:

```bash
# Generate a secure JWT secret
openssl rand -base64 64
```

### CORS Configuration

CORS is configured to restrict origins to specific domains:

```yaml
pet-clinic:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:8080}
```

All controllers use this configuration instead of wildcard (`*`) origins.

### HTTPS Configuration (Production)

For production deployment, configure HTTPS:

```yaml
server:
  ssl:
    enabled: ${app.https.enabled:true}
    key-store: ${app.https.keystore.path}
    key-store-password: ${app.https.keystore.password}
    key-store-type: ${app.https.keystore.type}
    key-alias: ${app.https.keystore.alias}
```

## Database Configuration

### Development (H2)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:petclinic
    driver-class-name: org.h2.Driver
    username: sa
    password: 
```

### Production (MySQL)
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?useSSL=true
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Flyway Migrations

Flyway is enabled in all profiles to ensure consistent schema:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

## Application Ports

- **Frontend**: 8080 (all profiles)
- **Backend**: 9090 (dev/staging), 8443 (prod with HTTPS)

## Caching Configuration

Caffeine cache is configured for performance:

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30m
    cache-names:
      - pets
      - veterinarians
      - visits
      - owners
      - searchResults
      - reports
```

## Monitoring Configuration

Actuator endpoints are configured for monitoring:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Access monitoring endpoints:
- Health: `http://localhost:9090/actuator/health`
- Metrics: `http://localhost:9090/actuator/metrics`
- Prometheus: `http://localhost:9090/actuator/prometheus`

## API Documentation

Swagger UI is available at:
- Development: `http://localhost:9090/swagger-ui.html`
- API Docs: `http://localhost:9090/api-docs`

## Logging Configuration

### Development
- Application: DEBUG level
- Spring Security: DEBUG level
- SQL queries: Enabled with formatting

### Production
- Application: WARN level
- Spring Security: WARN level
- Log file: `/var/log/pet-clinic-backend.log`

## Troubleshooting

### Common Issues

1. **JWT Secret Not Configured**
   - Error: Application fails to start
   - Solution: Set `JWT_SECRET` environment variable

2. **Database Connection Failed**
   - Error: Cannot connect to MySQL
   - Solution: Verify `DB_*` environment variables

3. **CORS Errors**
   - Error: Frontend cannot access backend
   - Solution: Update `CORS_ALLOWED_ORIGINS` to include frontend URL

4. **Flyway Migration Failed**
   - Error: Schema validation failed
   - Solution: Ensure database schema matches migrations

### Validation

To validate your configuration:

1. Check environment variables:
   ```bash
   env | grep -E "(DB_|JWT_|CORS_|BACKEND_)"
   ```

2. Test database connection:
   ```bash
   mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME
   ```

3. Verify JWT secret length:
   ```bash
   echo -n "$JWT_SECRET" | wc -c  # Should be at least 32 characters
   ```

## Best Practices

1. **Never commit `.env` files** - Use `.env.template` for documentation
2. **Use strong JWT secrets** - Generate with `openssl rand -base64 64`
3. **Restrict CORS origins** - Never use `*` in production
4. **Enable HTTPS in production** - Use valid SSL certificates
5. **Monitor application health** - Use actuator endpoints
6. **Regular backups** - Configure database backups
7. **Log rotation** - Configure log file rotation in production