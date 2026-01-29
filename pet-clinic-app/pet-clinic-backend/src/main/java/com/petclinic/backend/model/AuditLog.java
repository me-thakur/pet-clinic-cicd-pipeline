package com.petclinic.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry for tracking sensitive data access
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @NotBlank
    @Column(name = "username", nullable = false)
    private String username;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @NotBlank
    @Column(name = "action", nullable = false)
    private String action;

    @NotBlank
    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    // Constructors
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String username, String action, String resource, Long resourceId) {
        this();
        this.username = username;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
    }

    public AuditLog(String username, String action, String resource, Long resourceId, String details) {
        this(username, action, resource, resourceId);
        this.details = details;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + getId() +
                ", username='" + username + '\'' +
                ", timestamp=" + timestamp +
                ", action='" + action + '\'' +
                ", resource='" + resource + '\'' +
                ", resourceId=" + resourceId +
                ", details='" + details + '\'' +
                '}';
    }
}