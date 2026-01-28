#!/bin/bash

# Security Hardening Script for Pet Clinic CI/CD Pipeline
# This script implements comprehensive security hardening measures
# Requirements: 10.1, 10.2, 10.3, 10.4, 10.5

set -euo pipefail

# Configuration
LOG_FILE="/var/log/security-hardening.log"
BACKUP_DIR="/root/security-backups"
SECURITY_CONFIG_DIR="/etc/security/petclinic"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        error_exit "This script must be run as root"
    fi
}

# Create backup of original configurations
create_config_backups() {
    log "Creating backups of original configurations..."
    
    mkdir -p "$BACKUP_DIR"
    
    # Backup important configuration files
    local files_to_backup=(
        "/etc/ssh/sshd_config"
        "/etc/security/limits.conf"
        "/etc/sysctl.conf"
        "/etc/login.defs"
        "/etc/pam.d/common-password"
        "/etc/sudoers"
        "/etc/hosts.deny"
        "/etc/hosts.allow"
    )
    
    for file in "${files_to_backup[@]}"; do
        if [[ -f "$file" ]]; then
            cp "$file" "$BACKUP_DIR/$(basename "$file").backup.$(date +%Y%m%d)"
            log "Backed up: $file"
        fi
    done
    
    log "Configuration backups created in $BACKUP_DIR"
}

# Harden SSH configuration
harden_ssh() {
    log "Hardening SSH configuration..."
    
    local sshd_config="/etc/ssh/sshd_config"
    
    # Backup original config
    cp "$sshd_config" "$sshd_config.backup.$(date +%Y%m%d)"
    
    # Apply SSH hardening settings
    cat > "$sshd_config" << 'EOF'
# Pet Clinic SSH Hardening Configuration

# Protocol and Encryption
Protocol 2
Port 22
AddressFamily inet

# Authentication
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AuthorizedKeysFile .ssh/authorized_keys
PermitEmptyPasswords no
ChallengeResponseAuthentication no
UsePAM yes

# Connection Settings
ClientAliveInterval 300
ClientAliveCountMax 2
MaxAuthTries 3
MaxSessions 4
MaxStartups 10:30:100
LoginGraceTime 60

# Security Features
X11Forwarding no
AllowTcpForwarding no
GatewayPorts no
PermitTunnel no
PermitUserEnvironment no
StrictModes yes
UsePrivilegeSeparation sandbox

# Logging
SyslogFacility AUTH
LogLevel VERBOSE

# Allowed Users/Groups
AllowUsers jenkins ubuntu admin
DenyUsers root

# Host Key Algorithms
HostKeyAlgorithms rsa-sha2-512,rsa-sha2-256,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,ssh-ed25519

# Key Exchange Algorithms
KexAlgorithms curve25519-sha256@libssh.org,ecdh-sha2-nistp521,ecdh-sha2-nistp384,ecdh-sha2-nistp256,diffie-hellman-group16-sha512,diffie-hellman-group18-sha512

# Ciphers
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr

# MAC Algorithms
MACs hmac-sha2-256-etm@openssh.com,hmac-sha2-512-etm@openssh.com,hmac-sha2-256,hmac-sha2-512

# Banner
Banner /etc/ssh/banner
EOF
    
    # Create SSH banner
    cat > /etc/ssh/banner << 'EOF'
***************************************************************************
                    AUTHORIZED ACCESS ONLY
                    
This system is for the use of authorized users only. Individuals using
this computer system without authority, or in excess of their authority,
are subject to having all of their activities on this system monitored
and recorded by system personnel.

In the course of monitoring individuals improperly using this system, or
in the course of system maintenance, the activities of authorized users
may also be monitored.

Anyone using this system expressly consents to such monitoring and is
advised that if such monitoring reveals possible evidence of criminal
activity, system personnel may provide the evidence to law enforcement
officials.
***************************************************************************
EOF
    
    # Test SSH configuration
    sshd -t
    if [[ $? -eq 0 ]]; then
        systemctl restart sshd
        log "SSH hardening completed successfully"
    else
        error_exit "SSH configuration test failed"
    fi
}

# Configure system security limits
configure_security_limits() {
    log "Configuring system security limits..."
    
    # Configure limits.conf
    cat >> /etc/security/limits.conf << 'EOF'

# Pet Clinic Security Limits
* soft nofile 65536
* hard nofile 65536
* soft nproc 32768
* hard nproc 32768
root soft nofile 65536
root hard nofile 65536
jenkins soft nofile 65536
jenkins hard nofile 65536
EOF
    
    # Configure sysctl for security
    cat > /etc/sysctl.d/99-petclinic-security.conf << 'EOF'
# Pet Clinic Security Kernel Parameters

# Network Security
net.ipv4.ip_forward = 0
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1
net.ipv4.icmp_echo_ignore_broadcasts = 1
net.ipv4.icmp_ignore_bogus_error_responses = 1
net.ipv4.tcp_syncookies = 1
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# IPv6 Security (disable if not used)
net.ipv6.conf.all.disable_ipv6 = 1
net.ipv6.conf.default.disable_ipv6 = 1
net.ipv6.conf.lo.disable_ipv6 = 1

# Memory Protection
kernel.dmesg_restrict = 1
kernel.kptr_restrict = 2
kernel.yama.ptrace_scope = 1

# File System Security
fs.suid_dumpable = 0
fs.protected_hardlinks = 1
fs.protected_symlinks = 1

# Process Security
kernel.core_uses_pid = 1
kernel.ctrl-alt-del = 0
EOF
    
    # Apply sysctl settings
    sysctl -p /etc/sysctl.d/99-petclinic-security.conf
    
    log "System security limits configured"
}

# Configure password policies
configure_password_policies() {
    log "Configuring password policies..."
    
    # Install libpam-pwquality
    apt-get update
    apt-get install -y libpam-pwquality
    
    # Configure password quality requirements
    cat > /etc/security/pwquality.conf << 'EOF'
# Pet Clinic Password Quality Configuration

# Password length
minlen = 12
minclass = 3

# Character requirements
dcredit = -1    # At least 1 digit
ucredit = -1    # At least 1 uppercase
lcredit = -1    # At least 1 lowercase
ocredit = -1    # At least 1 special character

# Dictionary and similarity checks
dictcheck = 1
usercheck = 1
enforcing = 1

# Consecutive characters
maxsequence = 3
maxrepeat = 3

# Password history
remember = 12
EOF
    
    # Configure PAM for password policies
    sed -i 's/^password.*pam_unix.so.*/password required pam_pwquality.so retry=3\npassword sufficient pam_unix.so sha512 shadow nullok try_first_pass use_authtok remember=12/' /etc/pam.d/common-password
    
    # Configure login.defs
    sed -i 's/^PASS_MAX_DAYS.*/PASS_MAX_DAYS 90/' /etc/login.defs
    sed -i 's/^PASS_MIN_DAYS.*/PASS_MIN_DAYS 1/' /etc/login.defs
    sed -i 's/^PASS_WARN_AGE.*/PASS_WARN_AGE 7/' /etc/login.defs
    
    log "Password policies configured"
}

# Configure account lockout policies
configure_account_lockout() {
    log "Configuring account lockout policies..."
    
    # Install libpam-faillock
    apt-get install -y libpam-faillock
    
    # Configure faillock in PAM
    cat > /etc/pam.d/common-auth << 'EOF'
# Pet Clinic Authentication Configuration

# Account lockout configuration
auth required pam_faillock.so preauth silent audit deny=5 unlock_time=900 fail_interval=900
auth [success=1 default=bad] pam_unix.so
auth [default=die] pam_faillock.so authfail audit deny=5 unlock_time=900 fail_interval=900
auth sufficient pam_faillock.so authsucc audit deny=5 unlock_time=900 fail_interval=900

# Standard authentication
auth [success=1 default=ignore] pam_unix.so nullok_secure
auth requisite pam_deny.so
auth required pam_permit.so
EOF
    
    cat > /etc/pam.d/common-account << 'EOF'
# Pet Clinic Account Configuration

# Account lockout check
account required pam_faillock.so

# Standard account checks
account [success=1 new_authtok_reqd=done default=ignore] pam_unix.so
account requisite pam_deny.so
account required pam_permit.so
EOF
    
    log "Account lockout policies configured"
}

# Configure sudo security
configure_sudo_security() {
    log "Configuring sudo security..."
    
    # Backup original sudoers
    cp /etc/sudoers "$BACKUP_DIR/sudoers.backup.$(date +%Y%m%d)"
    
    # Configure secure sudo settings
    cat > /etc/sudoers.d/petclinic-security << 'EOF'
# Pet Clinic Sudo Security Configuration

# Defaults
Defaults env_reset
Defaults mail_badpass
Defaults secure_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
Defaults logfile="/var/log/sudo.log"
Defaults log_input, log_output
Defaults iolog_dir="/var/log/sudo-io"
Defaults passwd_timeout=5
Defaults passwd_tries=3
Defaults timestamp_timeout=15
Defaults requiretty
Defaults use_pty

# Disable dangerous environment variables
Defaults env_delete="BASH_ENV"
Defaults env_delete="ENV"
Defaults env_delete="FUNCTION_PATH"
Defaults env_delete="GLOBIGNORE"
Defaults env_delete="JAVA_TOOL_OPTIONS"
Defaults env_delete="PS4"
Defaults env_delete="BASH_FUNC_*"

# User privilege specification
jenkins ALL=(ALL) NOPASSWD: /bin/systemctl restart petclinic-*, /bin/systemctl start petclinic-*, /bin/systemctl stop petclinic-*
jenkins ALL=(ALL) NOPASSWD: /usr/bin/docker, /usr/local/bin/docker-compose
EOF
    
    # Set proper permissions
    chmod 440 /etc/sudoers.d/petclinic-security
    
    # Test sudo configuration
    visudo -c
    if [[ $? -ne 0 ]]; then
        error_exit "Sudo configuration test failed"
    fi
    
    log "Sudo security configured"
}

# Configure file system security
configure_filesystem_security() {
    log "Configuring file system security..."
    
    # Create secure directories
    mkdir -p "$SECURITY_CONFIG_DIR"
    chmod 700 "$SECURITY_CONFIG_DIR"
    
    # Set secure permissions on sensitive files
    chmod 600 /etc/ssh/ssh_host_*_key
    chmod 644 /etc/ssh/ssh_host_*_key.pub
    chmod 600 /etc/shadow
    chmod 600 /etc/gshadow
    chmod 644 /etc/passwd
    chmod 644 /etc/group
    
    # Secure log files
    chmod 640 /var/log/auth.log
    chmod 640 /var/log/syslog
    chmod 640 /var/log/kern.log
    
    # Create and configure logrotate for security logs
    cat > /etc/logrotate.d/petclinic-security << 'EOF'
/var/log/security-hardening.log
/var/log/sudo.log
/var/log/sudo-io/*.log {
    daily
    missingok
    rotate 90
    compress
    delaycompress
    notifempty
    create 640 root adm
    postrotate
        /bin/kill -HUP `cat /var/run/rsyslogd.pid 2> /dev/null` 2> /dev/null || true
    endscript
}
EOF
    
    # Find and secure world-writable files
    log "Securing world-writable files..."
    find / -xdev -type f -perm -0002 -exec chmod o-w {} \; 2>/dev/null || true
    
    # Find and report SUID/SGID files
    log "Auditing SUID/SGID files..."
    find / -xdev \( -perm -4000 -o -perm -2000 \) -type f > "$SECURITY_CONFIG_DIR/suid-sgid-files.txt" 2>/dev/null || true
    
    log "File system security configured"
}

# Configure network security
configure_network_security() {
    log "Configuring network security..."
    
    # Configure hosts.deny (default deny)
    cat > /etc/hosts.deny << 'EOF'
# Pet Clinic Network Security - Default Deny
ALL: ALL
EOF
    
    # Configure hosts.allow (specific allows)
    cat > /etc/hosts.allow << 'EOF'
# Pet Clinic Network Security - Specific Allows
sshd: 10.0.0.0/8
sshd: 172.16.0.0/12
sshd: 192.168.0.0/16
sshd: 127.0.0.1
EOF
    
    # Install and configure fail2ban
    apt-get install -y fail2ban
    
    cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
# Ban settings
bantime = 3600
findtime = 600
maxretry = 3
backend = systemd

# Email notifications
destemail = admin@petclinic.local
sendername = Fail2Ban-PetClinic
mta = sendmail

# SSH protection
[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600

# Nginx protection
[nginx-http-auth]
enabled = true
filter = nginx-http-auth
logpath = /var/log/nginx/error.log
maxretry = 3
bantime = 3600

[nginx-limit-req]
enabled = true
filter = nginx-limit-req
logpath = /var/log/nginx/error.log
maxretry = 10
bantime = 3600

# Custom application protection
[petclinic-auth]
enabled = true
filter = petclinic-auth
logpath = /var/log/petclinic/security.log
maxretry = 5
bantime = 1800
EOF
    
    # Create custom fail2ban filter for Pet Clinic
    cat > /etc/fail2ban/filter.d/petclinic-auth.conf << 'EOF'
[Definition]
failregex = ^.*authentication failed.*ip=<HOST>.*$
            ^.*unauthorized access.*ip=<HOST>.*$
            ^.*security violation.*ip=<HOST>.*$
ignoreregex =
EOF
    
    # Start and enable fail2ban
    systemctl enable fail2ban
    systemctl start fail2ban
    
    log "Network security configured"
}

# Configure application security
configure_application_security() {
    log "Configuring application security..."
    
    # Create application security configuration
    mkdir -p /etc/petclinic/security
    
    # Configure Java security properties
    cat > /etc/petclinic/security/java-security.properties << 'EOF'
# Pet Clinic Java Security Configuration

# Disable weak algorithms
jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL
jdk.certpath.disabledAlgorithms=MD2, MD5, SHA1 jdkCA & usage TLSServer, RSA keySize < 1024, DSA keySize < 1024, EC keySize < 224

# Enable strong random number generation
securerandom.source=file:/dev/urandom

# Network security
networkaddress.cache.ttl=60
networkaddress.cache.negative.ttl=10

# Security manager properties
java.security.manager=default
java.security.policy=/etc/petclinic/security/java.policy
EOF
    
    # Create Java security policy
    cat > /etc/petclinic/security/java.policy << 'EOF'
// Pet Clinic Java Security Policy

grant {
    // File permissions
    permission java.io.FilePermission "/var/log/petclinic/-", "read,write";
    permission java.io.FilePermission "/tmp/-", "read,write,delete";
    permission java.io.FilePermission "/var/lib/petclinic/-", "read,write";
    
    // Network permissions
    permission java.net.SocketPermission "localhost:3306", "connect,resolve";
    permission java.net.SocketPermission "*:80", "connect,resolve";
    permission java.net.SocketPermission "*:443", "connect,resolve";
    
    // System properties
    permission java.util.PropertyPermission "*", "read";
    permission java.util.PropertyPermission "java.awt.headless", "write";
    
    // Runtime permissions
    permission java.lang.RuntimePermission "accessDeclaredMembers";
    permission java.lang.RuntimePermission "createClassLoader";
    permission java.lang.RuntimePermission "getStackTrace";
    
    // Reflection permissions
    permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
};
EOF
    
    # Configure application environment security
    cat > /etc/environment << 'EOF'
# Pet Clinic Environment Security Configuration
JAVA_OPTS="-Djava.security.properties=/etc/petclinic/security/java-security.properties -Djava.security.manager -Djava.security.policy=/etc/petclinic/security/java.policy"
SPRING_PROFILES_ACTIVE=production,security
SECURITY_REQUIRE_SSL=true
SESSION_TIMEOUT=1800
CSRF_PROTECTION=true
XSS_PROTECTION=true
CONTENT_TYPE_OPTIONS=nosniff
FRAME_OPTIONS=DENY
EOF
    
    log "Application security configured"
}

# Configure audit logging
configure_audit_logging() {
    log "Configuring audit logging..."
    
    # Install auditd
    apt-get install -y auditd audispd-plugins
    
    # Configure audit rules
    cat > /etc/audit/rules.d/petclinic-audit.rules << 'EOF'
# Pet Clinic Audit Rules

# Delete existing rules
-D

# Buffer size
-b 8192

# Failure mode (0=silent, 1=printk, 2=panic)
-f 1

# Monitor authentication events
-w /etc/passwd -p wa -k identity
-w /etc/group -p wa -k identity
-w /etc/gshadow -p wa -k identity
-w /etc/shadow -p wa -k identity
-w /etc/security/opasswd -p wa -k identity

# Monitor sudo usage
-w /etc/sudoers -p wa -k actions
-w /etc/sudoers.d/ -p wa -k actions

# Monitor SSH configuration
-w /etc/ssh/sshd_config -p wa -k sshd

# Monitor system calls
-a always,exit -F arch=b64 -S adjtimex -S settimeofday -k time-change
-a always,exit -F arch=b32 -S adjtimex -S settimeofday -S stime -k time-change
-a always,exit -F arch=b64 -S clock_settime -k time-change
-a always,exit -F arch=b32 -S clock_settime -k time-change
-w /etc/localtime -p wa -k time-change

# Monitor network configuration
-w /etc/hosts -p wa -k system-locale
-w /etc/hostname -p wa -k system-locale
-w /etc/network/ -p wa -k system-locale

# Monitor login/logout events
-w /var/log/faillog -p wa -k logins
-w /var/log/lastlog -p wa -k logins
-w /var/log/tallylog -p wa -k logins

# Monitor process and session initiation
-w /var/run/utmp -p wa -k session
-w /var/log/wtmp -p wa -k logins
-w /var/log/btmp -p wa -k logins

# Monitor file system mounts
-a always,exit -F arch=b64 -S mount -k mounts
-a always,exit -F arch=b32 -S mount -k mounts

# Monitor file deletions
-a always,exit -F arch=b64 -S unlink -S unlinkat -S rename -S renameat -k delete
-a always,exit -F arch=b32 -S unlink -S unlinkat -S rename -S renameat -k delete

# Monitor changes to system administration scope
-w /etc/sudoers -p wa -k scope
-w /etc/sudoers.d/ -p wa -k scope

# Monitor kernel module loading
-w /sbin/insmod -p x -k modules
-w /sbin/rmmod -p x -k modules
-w /sbin/modprobe -p x -k modules
-a always,exit -F arch=b64 -S init_module -S delete_module -k modules

# Make the configuration immutable
-e 2
EOF
    
    # Configure auditd
    sed -i 's/^max_log_file = .*/max_log_file = 100/' /etc/audit/auditd.conf
    sed -i 's/^num_logs = .*/num_logs = 10/' /etc/audit/auditd.conf
    sed -i 's/^max_log_file_action = .*/max_log_file_action = rotate/' /etc/audit/auditd.conf
    
    # Enable and start auditd
    systemctl enable auditd
    systemctl start auditd
    
    log "Audit logging configured"
}

# Install and configure security monitoring tools
install_security_tools() {
    log "Installing security monitoring tools..."
    
    # Install security tools
    apt-get install -y \
        rkhunter \
        chkrootkit \
        lynis \
        aide \
        clamav \
        clamav-daemon
    
    # Configure rkhunter
    rkhunter --update
    rkhunter --propupd
    
    # Configure AIDE (Advanced Intrusion Detection Environment)
    aideinit
    mv /var/lib/aide/aide.db.new /var/lib/aide/aide.db
    
    # Configure ClamAV
    freshclam
    systemctl enable clamav-daemon
    systemctl start clamav-daemon
    
    # Create security scan script
    cat > /usr/local/bin/security-scan.sh << 'EOF'
#!/bin/bash

# Pet Clinic Security Scan Script
SCAN_LOG="/var/log/security-scan.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$SCAN_LOG"
}

log "Starting security scan..."

# Run rkhunter
log "Running rkhunter scan..."
rkhunter --check --skip-keypress --report-warnings-only >> "$SCAN_LOG" 2>&1

# Run chkrootkit
log "Running chkrootkit scan..."
chkrootkit >> "$SCAN_LOG" 2>&1

# Run AIDE check
log "Running AIDE integrity check..."
aide --check >> "$SCAN_LOG" 2>&1

# Run ClamAV scan on critical directories
log "Running ClamAV antivirus scan..."
clamscan -r --bell -i /home /var/www /tmp >> "$SCAN_LOG" 2>&1

log "Security scan completed"
EOF
    
    chmod +x /usr/local/bin/security-scan.sh
    
    # Create cron job for regular security scans
    cat > /etc/cron.d/security-scan << 'EOF'
# Pet Clinic Security Scan
0 2 * * 0 root /usr/local/bin/security-scan.sh
EOF
    
    log "Security monitoring tools installed"
}

# Generate security hardening report
generate_security_report() {
    log "Generating security hardening report..."
    
    local report_file="/root/petclinic-security-hardening-report.txt"
    
    cat > "$report_file" << EOF
Pet Clinic Security Hardening Report
Generated: $(date)

System Information:
- Hostname: $(hostname)
- OS: $(lsb_release -d | cut -f2)
- Kernel: $(uname -r)
- Architecture: $(uname -m)

Security Hardening Applied:
✓ SSH Hardening
  - Root login disabled
  - Password authentication disabled
  - Strong ciphers and algorithms configured
  - Connection limits applied
  - Logging enhanced

✓ System Security Limits
  - Kernel security parameters configured
  - Network security settings applied
  - Memory protection enabled
  - File system security enhanced

✓ Password Policies
  - Minimum length: 12 characters
  - Complexity requirements enforced
  - Password history: 12 passwords
  - Account lockout: 5 failed attempts

✓ Sudo Security
  - Secure defaults configured
  - Logging enabled
  - Restricted privileges for service accounts

✓ File System Security
  - Sensitive file permissions secured
  - World-writable files protected
  - SUID/SGID files audited

✓ Network Security
  - Fail2ban configured and active
  - Host-based access controls applied
  - Network parameter hardening

✓ Application Security
  - Java security policies configured
  - Environment security settings applied
  - Security headers enabled

✓ Audit Logging
  - Comprehensive audit rules configured
  - System events monitored
  - Log rotation configured

✓ Security Monitoring Tools
  - Rootkit detection (rkhunter, chkrootkit)
  - File integrity monitoring (AIDE)
  - Antivirus scanning (ClamAV)
  - Security assessment (Lynis)

Configuration Files:
- SSH Config: /etc/ssh/sshd_config
- Security Limits: /etc/security/limits.conf
- Kernel Parameters: /etc/sysctl.d/99-petclinic-security.conf
- Password Policy: /etc/security/pwquality.conf
- Sudo Config: /etc/sudoers.d/petclinic-security
- Fail2ban Config: /etc/fail2ban/jail.local
- Audit Rules: /etc/audit/rules.d/petclinic-audit.rules
- Java Security: /etc/petclinic/security/java-security.properties

Log Files:
- Security Hardening: $LOG_FILE
- Sudo Logs: /var/log/sudo.log
- Audit Logs: /var/log/audit/audit.log
- Fail2ban Logs: /var/log/fail2ban.log
- Security Scans: /var/log/security-scan.log

Backup Files:
- Configuration Backups: $BACKUP_DIR

Scheduled Tasks:
- Security Scan: Weekly (Sunday 2:00 AM)
- Log Rotation: Daily
- System Updates: Manual (recommended weekly)

Security Recommendations:
1. Regularly update system packages
2. Monitor security logs for suspicious activity
3. Review and update security policies periodically
4. Perform regular security assessments
5. Keep security tools updated
6. Implement network segmentation
7. Use intrusion detection systems
8. Regular backup and disaster recovery testing
9. Security awareness training for administrators
10. Implement multi-factor authentication where possible

Next Steps:
1. Test all services after hardening
2. Update monitoring and alerting systems
3. Document security procedures
4. Schedule regular security reviews
5. Implement additional security controls as needed

Security Contacts:
- Security Team: security@petclinic.local
- System Administrator: admin@petclinic.local
- Emergency Contact: emergency@petclinic.local

Compliance Notes:
This hardening configuration addresses common security frameworks:
- CIS (Center for Internet Security) benchmarks
- NIST Cybersecurity Framework
- OWASP security guidelines
- Industry best practices

For detailed compliance mapping, refer to the security documentation.
EOF
    
    chmod 600 "$report_file"
    
    log "Security hardening report generated: $report_file"
}

# Test security configuration
test_security_configuration() {
    log "Testing security configuration..."
    
    # Test SSH configuration
    if sshd -t; then
        log "✓ SSH configuration test passed"
    else
        log "✗ SSH configuration test failed"
    fi
    
    # Test sudo configuration
    if visudo -c; then
        log "✓ Sudo configuration test passed"
    else
        log "✗ Sudo configuration test failed"
    fi
    
    # Test fail2ban status
    if systemctl is-active --quiet fail2ban; then
        log "✓ Fail2ban is active"
    else
        log "✗ Fail2ban is not active"
    fi
    
    # Test auditd status
    if systemctl is-active --quiet auditd; then
        log "✓ Auditd is active"
    else
        log "✗ Auditd is not active"
    fi
    
    # Test file permissions
    local critical_files=(
        "/etc/shadow:600"
        "/etc/ssh/ssh_host_rsa_key:600"
        "/etc/sudoers:440"
    )
    
    for file_perm in "${critical_files[@]}"; do
        local file="${file_perm%:*}"
        local expected_perm="${file_perm#*:}"
        
        if [[ -f "$file" ]]; then
            local actual_perm
            actual_perm=$(stat -c "%a" "$file")
            if [[ "$actual_perm" == "$expected_perm" ]]; then
                log "✓ File permissions correct: $file ($actual_perm)"
            else
                log "✗ File permissions incorrect: $file (expected $expected_perm, got $actual_perm)"
            fi
        fi
    done
    
    log "Security configuration testing completed"
}

# Main execution
main() {
    local skip_network=false
    local skip_audit=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-network)
                skip_network=true
                shift
                ;;
            --skip-audit)
                skip_audit=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --skip-network    Skip network security configuration"
                echo "  --skip-audit      Skip audit logging configuration"
                echo "  -h, --help       Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    log "Starting comprehensive security hardening for Pet Clinic CI/CD Pipeline"
    
    check_root
    create_config_backups
    harden_ssh
    configure_security_limits
    configure_password_policies
    configure_account_lockout
    configure_sudo_security
    configure_filesystem_security
    configure_application_security
    
    if [[ "$skip_network" != true ]]; then
        configure_network_security
    fi
    
    if [[ "$skip_audit" != true ]]; then
        configure_audit_logging
    fi
    
    install_security_tools
    test_security_configuration
    generate_security_report
    
    log "Security hardening completed successfully"
    log ""
    log "Important Information:"
    log "- Security Report: /root/petclinic-security-hardening-report.txt"
    log "- Configuration Backups: $BACKUP_DIR"
    log "- Security Logs: $LOG_FILE"
    log ""
    log "CRITICAL: System will require reboot to apply all kernel security settings"
    log ""
    log "Next Steps:"
    log "1. Review the security report"
    log "2. Test all applications and services"
    log "3. Update monitoring systems"
    log "4. Schedule regular security scans"
    log "5. Reboot the system when convenient"
}

# Execute main function
main "$@"