-- MySQL User Creation Script for CI/CD Environment
-- This script creates users with appropriate privileges for testing
-- Requirements: 2.2, 3.1, 3.2, 3.3

-- Create application user for testing
CREATE USER IF NOT EXISTS 'test_user'@'%' IDENTIFIED BY 'test_password';

-- Grant privileges needed for function, procedure, and trigger creation
-- These privileges work with log_bin_trust_function_creators = 1
GRANT SELECT, INSERT, UPDATE, DELETE ON testdb.* TO 'test_user'@'%';
GRANT CREATE, DROP, ALTER ON testdb.* TO 'test_user'@'%';
GRANT CREATE ROUTINE, ALTER ROUTINE ON testdb.* TO 'test_user'@'%';
GRANT EXECUTE ON testdb.* TO 'test_user'@'%';
GRANT TRIGGER ON testdb.* TO 'test_user'@'%';

-- Create a user with minimal privileges to test privilege validation
CREATE USER IF NOT EXISTS 'limited_user'@'%' IDENTIFIED BY 'limited_password';
GRANT SELECT ON testdb.* TO 'limited_user'@'%';

-- Create a user with SUPER privilege for comparison testing
CREATE USER IF NOT EXISTS 'super_user'@'%' IDENTIFIED BY 'super_password';
GRANT ALL PRIVILEGES ON *.* TO 'super_user'@'%' WITH GRANT OPTION;

-- Flush privileges to ensure changes take effect
FLUSH PRIVILEGES;

-- Display current privilege settings for verification
SELECT 'Current MySQL privilege settings:' as info;
SELECT @@log_bin_trust_function_creators as log_bin_trust_function_creators;
SELECT @@log_bin as binary_logging_enabled;
SELECT @@binlog_format as binlog_format;

-- Show created users
SELECT 'Created users:' as info;
SELECT user, host FROM mysql.user WHERE user IN ('test_user', 'limited_user', 'super_user');

-- Test function creation capability
SELECT 'Testing function creation capability...' as info;