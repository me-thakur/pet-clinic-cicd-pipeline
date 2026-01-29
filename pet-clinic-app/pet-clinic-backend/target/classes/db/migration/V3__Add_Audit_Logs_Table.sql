-- Migration script to add audit_logs table for audit logging functionality
-- Validates: Requirements 9.2

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for better query performance
CREATE INDEX idx_audit_logs_username ON audit_logs(username);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource);
CREATE INDEX idx_audit_logs_resource_id ON audit_logs(resource_id);
CREATE INDEX idx_audit_logs_resource_composite ON audit_logs(resource, resource_id);

-- Comments are included in the migration script for documentation
-- H2 database does not support ALTER TABLE COMMENT syntax
-- Column descriptions:
-- username: Username of the user performing the action
-- timestamp: Timestamp when the action occurred  
-- action: Type of action performed (CREATE, READ, UPDATE, DELETE, etc.)
-- resource: Type of resource accessed (User, Pet, Visit, etc.)
-- resource_id: ID of the specific resource accessed
-- details: Additional details about the action
-- ip_address: IP address of the user (supports IPv4 and IPv6)
-- user_agent: User agent string from the request