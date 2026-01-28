# MySQL Privilege Configuration - Security Troubleshooting Guide

## Table of Contents

1. [Security Incident Response](#security-incident-response)
2. [Privilege Escalation Detection](#privilege-escalation-detection)
3. [Function Security Analysis](#function-security-analysis)
4. [Binary Log Security](#binary-log-security)
5. [Replication Security Issues](#replication-security-issues)
6. [SSL/TLS Troubleshooting](#ssltls-troubleshooting)
7. [Audit Log Analysis](#audit-log-analysis)
8. [Emergency Procedures](#emergency-procedures)

## Security Incident Response

### Suspected Privilege Escalation

**Immediate Actions:**
1. **Isolate the affected system**
2. **Preserve evidence** (logs, binary logs, configuration files)
3. **Assess the scope** of potential compromise
4. **Notify security team** and stakeholders

**Investigation Steps:**

```sql
-- 1. Check for recent privilege changes
SELECT 
    User, 
    Host, 
    Super_priv, 
    Grant_priv,
    Password_last_changed,
    Account_locked
FROM mysql.user 
WHERE Password_last_changed >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY Password_last_changed DESC;

-- 2. Review recently created functions/procedures
SELECT 
    ROUTINE_SCHEMA,
    ROUTINE_NAME,
    ROUTINE_TYPE,
    DEFINER,
    CREATED,
    LAST_ALTERED,
    ROUTINE_DEFINITION
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE CREATED >= DATE_SUB(NOW(), INTERVAL 7 DAY)
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
ORDER BY CREATED DESC;

-- 3. Check for suspicious DEFINER usage
SELECT 
    ROUTINE_NAME,
    DEFINER,
    ROUTINE_DEFINITION
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE DEFINER LIKE 'root@%' 
OR DEFINER NOT LIKE '%@localhost'
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys');
```

**Evidence Collection:**

```bash
#!/bin/bash
# security-evidence-collection.sh

INCIDENT_DIR="/tmp/mysql-security-incident-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$INCIDENT_DIR"

echo "Collecting MySQL security evidence to $INCIDENT_DIR"

# 1. Current configuration
mysql -u root -p -e "SHOW VARIABLES" > "$INCIDENT_DIR/mysql-variables.txt"
mysql -u root -p -e "SHOW GLOBAL STATUS" > "$INCIDENT_DIR/mysql-status.txt"

# 2. User privileges
mysql -u root -p -e "SELECT * FROM mysql.user" > "$INCIDENT_DIR/mysql-users.txt"
mysql -u root -p -e "SELECT * FROM mysql.db" > "$INCIDENT_DIR/mysql-db-privileges.txt"

# 3. Recent routines
mysql -u root -p -e "
SELECT * FROM INFORMATION_SCHEMA.ROUTINES 
WHERE CREATED >= DATE_SUB(NOW(), INTERVAL 30 DAY)
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
" > "$INCIDENT_DIR/recent-routines.txt"

# 4. Binary logs
mysql -u root -p -e "SHOW BINARY LOGS" > "$INCIDENT_DIR/binary-logs.txt"
mysql -u root -p -e "SHOW MASTER STATUS" > "$INCIDENT_DIR/master-status.txt"

# 5. Process list
mysql -u root -p -e "SHOW FULL PROCESSLIST" > "$INCIDENT_DIR/processlist.txt"

# 6. Error logs
cp /var/log/mysql/error.log "$INCIDENT_DIR/" 2>/dev/null || echo "Error log not accessible"

# 7. Configuration files
cp /etc/mysql/my.cnf "$INCIDENT_DIR/" 2>/dev/null || echo "my.cnf not found in /etc/mysql/"
cp /usr/local/etc/my.cnf "$INCIDENT_DIR/" 2>/dev/null || echo "my.cnf not found in /usr/local/etc/"

# 8. System information
uname -a > "$INCIDENT_DIR/system-info.txt"
ps aux | grep mysql > "$INCIDENT_DIR/mysql-processes.txt"
netstat -tlnp | grep 3306 > "$INCIDENT_DIR/mysql-network.txt"

echo "Evidence collection completed in $INCIDENT_DIR"
echo "Archive and secure this directory for forensic analysis"
```

### Unauthorized Function Creation

**Detection Queries:**

```sql
-- Functions created in the last 24 hours
SELECT 
    ROUTINE_SCHEMA,
    ROUTINE_NAME,
    DEFINER,
    CREATED,
    ROUTINE_DEFINITION
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE CREATED >= DATE_SUB(NOW(), INTERVAL 1 DAY)
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys');

-- Functions with suspicious characteristics
SELECT 
    ROUTINE_NAME,
    DEFINER,
    ROUTINE_DEFINITION
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE (
    ROUTINE_DEFINITION LIKE '%SYSTEM%' OR
    ROUTINE_DEFINITION LIKE '%SHELL%' OR
    ROUTINE_DEFINITION LIKE '%EXEC%' OR
    ROUTINE_DEFINITION LIKE '%FILE%' OR
    ROUTINE_DEFINITION LIKE '%LOAD_FILE%' OR
    ROUTINE_DEFINITION LIKE '%INTO OUTFILE%'
)
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys');
```

**Immediate Response:**

```sql
-- Disable suspicious functions (don't drop immediately - preserve evidence)
-- Note: MySQL doesn't have DISABLE FUNCTION, so consider renaming or access control

-- Review and document suspicious functions before removal
SELECT 
    CONCAT('-- SUSPICIOUS FUNCTION: ', ROUTINE_NAME, '\n',
           '-- DEFINER: ', DEFINER, '\n',
           '-- CREATED: ', CREATED, '\n',
           '-- DEFINITION:\n', ROUTINE_DEFINITION, '\n\n') as documentation
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE ROUTINE_NAME IN ('suspicious_function_name');

-- After documentation, remove if confirmed malicious
-- DROP FUNCTION suspicious_function_name;
```

## Privilege Escalation Detection

### Monitoring Privilege Changes

**Real-time Monitoring Script:**

```python
#!/usr/bin/env python3
# privilege-monitor.py

import mysql.connector
import time
import json
import logging
from datetime import datetime

class PrivilegeMonitor:
    def __init__(self, host='localhost', user='monitor', password='monitor_pass'):
        self.connection_config = {
            'host': host,
            'user': user,
            'password': password,
            'database': 'mysql'
        }
        self.last_check = {}
        self.setup_logging()
    
    def setup_logging(self):
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler('/var/log/mysql-privilege-monitor.log'),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)
    
    def get_current_privileges(self):
        """Get current state of user privileges."""
        connection = mysql.connector.connect(**self.connection_config)
        cursor = connection.cursor(dictionary=True)
        
        cursor.execute("""
            SELECT 
                User, 
                Host, 
                Super_priv, 
                Grant_priv,
                Create_routine_priv,
                Password_last_changed
            FROM user
        """)
        
        privileges = cursor.fetchall()
        cursor.close()
        connection.close()
        
        return {f"{p['User']}@{p['Host']}": p for p in privileges}
    
    def detect_changes(self):
        """Detect privilege changes since last check."""
        current_privileges = self.get_current_privileges()
        changes = []
        
        if not self.last_check:
            self.last_check = current_privileges
            return changes
        
        # Check for new users
        for user_host, privileges in current_privileges.items():
            if user_host not in self.last_check:
                changes.append({
                    'type': 'USER_ADDED',
                    'user': user_host,
                    'privileges': privileges,
                    'timestamp': datetime.now().isoformat()
                })
        
        # Check for privilege changes
        for user_host, old_privileges in self.last_check.items():
            if user_host in current_privileges:
                new_privileges = current_privileges[user_host]
                
                # Check for privilege escalation
                if (old_privileges['Super_priv'] == 'N' and 
                    new_privileges['Super_priv'] == 'Y'):
                    changes.append({
                        'type': 'SUPER_PRIVILEGE_GRANTED',
                        'user': user_host,
                        'timestamp': datetime.now().isoformat(),
                        'severity': 'HIGH'
                    })
                
                if (old_privileges['Grant_priv'] == 'N' and 
                    new_privileges['Grant_priv'] == 'Y'):
                    changes.append({
                        'type': 'GRANT_PRIVILEGE_GRANTED',
                        'user': user_host,
                        'timestamp': datetime.now().isoformat(),
                        'severity': 'HIGH'
                    })
        
        # Check for removed users
        for user_host in self.last_check:
            if user_host not in current_privileges:
                changes.append({
                    'type': 'USER_REMOVED',
                    'user': user_host,
                    'timestamp': datetime.now().isoformat()
                })
        
        self.last_check = current_privileges
        return changes
    
    def monitor(self, interval=60):
        """Continuous monitoring loop."""
        self.logger.info("Starting privilege monitoring...")
        
        while True:
            try:
                changes = self.detect_changes()
                
                for change in changes:
                    if change.get('severity') == 'HIGH':
                        self.logger.error(f"SECURITY ALERT: {change}")
                        self.send_alert(change)
                    else:
                        self.logger.info(f"Privilege change detected: {change}")
                
                time.sleep(interval)
                
            except Exception as e:
                self.logger.error(f"Monitoring error: {e}")
                time.sleep(interval)
    
    def send_alert(self, change):
        """Send security alert (implement based on your alerting system)."""
        # Implement email, Slack, or other alerting mechanism
        pass

if __name__ == "__main__":
    monitor = PrivilegeMonitor()
    monitor.monitor()
```

### Automated Privilege Validation

```bash
#!/bin/bash
# validate-privileges.sh

# Expected privilege configuration
declare -A EXPECTED_SUPER_USERS=(
    ["root@localhost"]="Y"
    ["root@127.0.0.1"]="Y"
    ["root@::1"]="Y"
)

declare -A EXPECTED_GRANT_USERS=(
    ["root@localhost"]="Y"
    ["root@127.0.0.1"]="Y"
    ["root@::1"]="Y"
)

# Check current privileges
mysql -u root -p -e "
SELECT 
    CONCAT(User, '@', Host) as user_host,
    Super_priv,
    Grant_priv
FROM mysql.user 
WHERE Super_priv = 'Y' OR Grant_priv = 'Y'
" --skip-column-names | while read user_host super_priv grant_priv; do
    
    # Check SUPER privilege
    if [ "$super_priv" = "Y" ]; then
        if [[ ! ${EXPECTED_SUPER_USERS[$user_host]} ]]; then
            echo "ALERT: Unexpected SUPER privilege for $user_host"
        fi
    fi
    
    # Check GRANT privilege
    if [ "$grant_priv" = "Y" ]; then
        if [[ ! ${EXPECTED_GRANT_USERS[$user_host]} ]]; then
            echo "ALERT: Unexpected GRANT privilege for $user_host"
        fi
    fi
done
```

## Function Security Analysis

### Analyzing Function Security

**Dangerous Pattern Detection:**

```sql
-- Functions with potentially dangerous operations
SELECT 
    ROUTINE_SCHEMA,
    ROUTINE_NAME,
    DEFINER,
    ROUTINE_DEFINITION,
    CASE 
        WHEN ROUTINE_DEFINITION REGEXP '(SYSTEM|SHELL|EXEC|LOAD_FILE|INTO OUTFILE|INTO DUMPFILE)' THEN 'HIGH'
        WHEN ROUTINE_DEFINITION REGEXP '(CONCAT|PREPARE|EXECUTE)' THEN 'MEDIUM'
        WHEN DEFINER LIKE 'root@%' THEN 'MEDIUM'
        ELSE 'LOW'
    END as risk_level
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
ORDER BY 
    CASE 
        WHEN ROUTINE_DEFINITION REGEXP '(SYSTEM|SHELL|EXEC|LOAD_FILE|INTO OUTFILE|INTO DUMPFILE)' THEN 1
        WHEN ROUTINE_DEFINITION REGEXP '(CONCAT|PREPARE|EXECUTE)' THEN 2
        WHEN DEFINER LIKE 'root@%' THEN 3
        ELSE 4
    END;
```

**Function Determinism Analysis:**

```sql
-- Check for non-deterministic functions (potential replication issues)
SELECT 
    ROUTINE_SCHEMA,
    ROUTINE_NAME,
    IS_DETERMINISTIC,
    ROUTINE_DEFINITION
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE IS_DETERMINISTIC = 'NO'
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
AND @@log_bin_trust_function_creators = 1;
```

**DEFINER Analysis:**

```sql
-- Analyze DEFINER usage patterns
SELECT 
    DEFINER,
    COUNT(*) as function_count,
    GROUP_CONCAT(ROUTINE_NAME) as functions
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
GROUP BY DEFINER
ORDER BY function_count DESC;

-- Check for privilege escalation via DEFINER
SELECT 
    r.ROUTINE_NAME,
    r.DEFINER,
    u.Super_priv,
    u.Grant_priv
FROM INFORMATION_SCHEMA.ROUTINES r
LEFT JOIN mysql.user u ON SUBSTRING_INDEX(r.DEFINER, '@', 1) = u.User 
    AND SUBSTRING_INDEX(r.DEFINER, '@', -1) = u.Host
WHERE r.ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
AND (u.Super_priv = 'Y' OR u.Grant_priv = 'Y');
```

### Function Security Audit Script

```python
#!/usr/bin/env python3
# function-security-audit.py

import mysql.connector
import re
import json
from datetime import datetime

class FunctionSecurityAuditor:
    def __init__(self, host='localhost', user='audit', password='audit_pass'):
        self.connection_config = {
            'host': host,
            'user': user,
            'password': password
        }
        
        # Define security patterns
        self.high_risk_patterns = [
            r'SYSTEM\s*\(',
            r'SHELL\s*\(',
            r'EXEC\s*\(',
            r'LOAD_FILE\s*\(',
            r'INTO\s+OUTFILE',
            r'INTO\s+DUMPFILE'
        ]
        
        self.medium_risk_patterns = [
            r'CONCAT\s*\(',
            r'PREPARE\s+',
            r'EXECUTE\s+',
            r'@@\w+',  # System variables
            r'INFORMATION_SCHEMA'
        ]
    
    def analyze_function_security(self, routine_definition, definer):
        """Analyze a function for security risks."""
        risks = []
        
        # Check for high-risk patterns
        for pattern in self.high_risk_patterns:
            if re.search(pattern, routine_definition, re.IGNORECASE):
                risks.append({
                    'level': 'HIGH',
                    'pattern': pattern,
                    'description': 'Potentially dangerous system operation'
                })
        
        # Check for medium-risk patterns
        for pattern in self.medium_risk_patterns:
            if re.search(pattern, routine_definition, re.IGNORECASE):
                risks.append({
                    'level': 'MEDIUM',
                    'pattern': pattern,
                    'description': 'Dynamic SQL or system access'
                })
        
        # Check DEFINER risks
        if 'root@' in definer:
            risks.append({
                'level': 'MEDIUM',
                'pattern': 'root DEFINER',
                'description': 'Function executes with root privileges'
            })
        
        return risks
    
    def audit_all_functions(self):
        """Audit all user-defined functions."""
        connection = mysql.connector.connect(**self.connection_config)
        cursor = connection.cursor(dictionary=True)
        
        cursor.execute("""
            SELECT 
                ROUTINE_SCHEMA,
                ROUTINE_NAME,
                ROUTINE_TYPE,
                DEFINER,
                IS_DETERMINISTIC,
                CREATED,
                ROUTINE_DEFINITION
            FROM INFORMATION_SCHEMA.ROUTINES 
            WHERE ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
            ORDER BY CREATED DESC
        """)
        
        routines = cursor.fetchall()
        cursor.close()
        connection.close()
        
        audit_results = {
            'timestamp': datetime.now().isoformat(),
            'total_routines': len(routines),
            'high_risk': [],
            'medium_risk': [],
            'low_risk': []
        }
        
        for routine in routines:
            risks = self.analyze_function_security(
                routine['ROUTINE_DEFINITION'], 
                routine['DEFINER']
            )
            
            routine_info = {
                'schema': routine['ROUTINE_SCHEMA'],
                'name': routine['ROUTINE_NAME'],
                'type': routine['ROUTINE_TYPE'],
                'definer': routine['DEFINER'],
                'deterministic': routine['IS_DETERMINISTIC'],
                'created': routine['CREATED'].isoformat() if routine['CREATED'] else None,
                'risks': risks
            }
            
            # Categorize by highest risk level
            max_risk = 'LOW'
            for risk in risks:
                if risk['level'] == 'HIGH':
                    max_risk = 'HIGH'
                    break
                elif risk['level'] == 'MEDIUM':
                    max_risk = 'MEDIUM'
            
            audit_results[f'{max_risk.lower()}_risk'].append(routine_info)
        
        return audit_results
    
    def generate_report(self, audit_results):
        """Generate human-readable audit report."""
        report = f"""
MySQL Function Security Audit Report
Generated: {audit_results['timestamp']}
Total Routines Analyzed: {audit_results['total_routines']}

HIGH RISK FUNCTIONS ({len(audit_results['high_risk'])}):
"""
        
        for func in audit_results['high_risk']:
            report += f"\n  {func['schema']}.{func['name']} ({func['type']})\n"
            report += f"    DEFINER: {func['definer']}\n"
            report += f"    CREATED: {func['created']}\n"
            report += f"    RISKS:\n"
            for risk in func['risks']:
                report += f"      - {risk['level']}: {risk['description']}\n"
        
        report += f"\nMEDIUM RISK FUNCTIONS ({len(audit_results['medium_risk'])}):\n"
        
        for func in audit_results['medium_risk']:
            report += f"\n  {func['schema']}.{func['name']} ({func['type']})\n"
            report += f"    DEFINER: {func['definer']}\n"
            for risk in func['risks']:
                if risk['level'] == 'MEDIUM':
                    report += f"    - {risk['description']}\n"
        
        report += f"\nLOW RISK FUNCTIONS: {len(audit_results['low_risk'])}\n"
        
        return report

if __name__ == "__main__":
    auditor = FunctionSecurityAuditor()
    results = auditor.audit_all_functions()
    
    # Save JSON results
    with open(f"function-audit-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json", 'w') as f:
        json.dump(results, f, indent=2, default=str)
    
    # Print report
    print(auditor.generate_report(results))
```

## Binary Log Security

### Binary Log Analysis for Security

**Suspicious Activity Detection:**

```sql
-- Note: These queries require binary log analysis tools like mysqlbinlog

-- Check for privilege-related changes in binary logs
-- Run this on binary log files:
-- mysqlbinlog mysql-bin.000001 | grep -i "grant\|revoke\|create user\|drop user"

-- Check for function creation in binary logs:
-- mysqlbinlog mysql-bin.000001 | grep -i "create function\|create procedure\|create trigger"
```

**Binary Log Security Script:**

```bash
#!/bin/bash
# binlog-security-analysis.sh

BINLOG_DIR="/var/lib/mysql"
ANALYSIS_DIR="/tmp/binlog-analysis-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$ANALYSIS_DIR"

echo "Analyzing binary logs for security events..."

# Get list of binary logs
mysql -u root -p -e "SHOW BINARY LOGS" --skip-column-names | while read binlog size; do
    echo "Analyzing $binlog..."
    
    # Extract privilege-related events
    mysqlbinlog "$BINLOG_DIR/$binlog" | grep -i "grant\|revoke\|create user\|drop user\|alter user" > "$ANALYSIS_DIR/${binlog}-privileges.txt"
    
    # Extract function/procedure creation
    mysqlbinlog "$BINLOG_DIR/$binlog" | grep -i "create function\|create procedure\|create trigger\|drop function\|drop procedure\|drop trigger" > "$ANALYSIS_DIR/${binlog}-routines.txt"
    
    # Extract suspicious patterns
    mysqlbinlog "$BINLOG_DIR/$binlog" | grep -i "system\|shell\|exec\|load_file\|outfile" > "$ANALYSIS_DIR/${binlog}-suspicious.txt"
done

echo "Binary log analysis completed in $ANALYSIS_DIR"

# Generate summary report
cat > "$ANALYSIS_DIR/summary.txt" << EOF
Binary Log Security Analysis Summary
Generated: $(date)

Privilege Changes:
$(find "$ANALYSIS_DIR" -name "*-privileges.txt" -exec wc -l {} \; | awk '{sum+=$1} END {print sum " events"}')

Routine Changes:
$(find "$ANALYSIS_DIR" -name "*-routines.txt" -exec wc -l {} \; | awk '{sum+=$1} END {print sum " events"}')

Suspicious Patterns:
$(find "$ANALYSIS_DIR" -name "*-suspicious.txt" -exec wc -l {} \; | awk '{sum+=$1} END {print sum " events"}')

Review individual files for detailed analysis.
EOF

cat "$ANALYSIS_DIR/summary.txt"
```

### Binary Log Integrity Verification

```bash
#!/bin/bash
# binlog-integrity-check.sh

echo "Checking binary log integrity..."

# Check binary log index consistency
mysql -u root -p -e "SHOW BINARY LOGS" --skip-column-names | while read binlog size; do
    if [ ! -f "/var/lib/mysql/$binlog" ]; then
        echo "ERROR: Binary log file $binlog is missing but listed in index"
    else
        actual_size=$(stat -c%s "/var/lib/mysql/$binlog")
        if [ "$actual_size" != "$size" ]; then
            echo "WARNING: Size mismatch for $binlog (index: $size, actual: $actual_size)"
        fi
    fi
done

# Check for binary log corruption
mysql -u root -p -e "SHOW BINARY LOGS" --skip-column-names | while read binlog size; do
    echo "Checking $binlog for corruption..."
    if ! mysqlbinlog --verify-binlog-checksum "/var/lib/mysql/$binlog" > /dev/null 2>&1; then
        echo "ERROR: Binary log $binlog appears to be corrupted"
    fi
done

echo "Binary log integrity check completed"
```

## Replication Security Issues

### Replication Security Monitoring

**Master-Slave Consistency Check:**

```sql
-- On Master: Check for non-deterministic functions
SELECT 
    ROUTINE_SCHEMA,
    ROUTINE_NAME,
    IS_DETERMINISTIC,
    DEFINER
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE IS_DETERMINISTIC = 'NO'
AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys');

-- On Slave: Check replication status
SHOW SLAVE STATUS\G

-- Check for replication errors
SELECT * FROM performance_schema.replication_applier_status_by_worker 
WHERE LAST_ERROR_NUMBER != 0;
```

**Replication Security Audit:**

```python
#!/usr/bin/env python3
# replication-security-audit.py

import mysql.connector
from datetime import datetime

class ReplicationSecurityAuditor:
    def __init__(self, master_config, slave_configs):
        self.master_config = master_config
        self.slave_configs = slave_configs
    
    def check_master_security(self):
        """Check master server security configuration."""
        connection = mysql.connector.connect(**self.master_config)
        cursor = connection.cursor(dictionary=True)
        
        issues = []
        
        # Check log_bin_trust_function_creators
        cursor.execute("SELECT @@log_bin_trust_function_creators as setting")
        trust_setting = cursor.fetchone()['setting']
        
        if trust_setting == 1:
            # Check for non-deterministic functions
            cursor.execute("""
                SELECT COUNT(*) as count FROM INFORMATION_SCHEMA.ROUTINES 
                WHERE IS_DETERMINISTIC = 'NO'
                AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
            """)
            non_det_count = cursor.fetchone()['count']
            
            if non_det_count > 0:
                issues.append({
                    'severity': 'HIGH',
                    'issue': f'{non_det_count} non-deterministic functions with log_bin_trust_function_creators enabled',
                    'recommendation': 'Review functions for determinism or disable log_bin_trust_function_creators'
                })
        
        cursor.close()
        connection.close()
        return issues
    
    def check_slave_security(self, slave_config):
        """Check slave server security configuration."""
        connection = mysql.connector.connect(**slave_config)
        cursor = connection.cursor(dictionary=True)
        
        issues = []
        
        # Check replication status
        cursor.execute("SHOW SLAVE STATUS")
        slave_status = cursor.fetchone()
        
        if slave_status:
            if slave_status['Last_Error']:
                issues.append({
                    'severity': 'HIGH',
                    'issue': f"Replication error: {slave_status['Last_Error']}",
                    'recommendation': 'Investigate and resolve replication error'
                })
            
            if slave_status['Seconds_Behind_Master'] and slave_status['Seconds_Behind_Master'] > 300:
                issues.append({
                    'severity': 'MEDIUM',
                    'issue': f"Slave lag: {slave_status['Seconds_Behind_Master']} seconds",
                    'recommendation': 'Investigate replication performance'
                })
        
        cursor.close()
        connection.close()
        return issues
    
    def audit_replication_security(self):
        """Perform comprehensive replication security audit."""
        audit_results = {
            'timestamp': datetime.now().isoformat(),
            'master_issues': [],
            'slave_issues': {}
        }
        
        # Audit master
        try:
            audit_results['master_issues'] = self.check_master_security()
        except Exception as e:
            audit_results['master_issues'] = [{
                'severity': 'HIGH',
                'issue': f'Failed to audit master: {e}',
                'recommendation': 'Check master connectivity and permissions'
            }]
        
        # Audit slaves
        for i, slave_config in enumerate(self.slave_configs):
            try:
                audit_results['slave_issues'][f'slave_{i}'] = self.check_slave_security(slave_config)
            except Exception as e:
                audit_results['slave_issues'][f'slave_{i}'] = [{
                    'severity': 'HIGH',
                    'issue': f'Failed to audit slave: {e}',
                    'recommendation': 'Check slave connectivity and permissions'
                }]
        
        return audit_results

# Example usage
if __name__ == "__main__":
    master_config = {
        'host': 'master.example.com',
        'user': 'audit',
        'password': 'audit_pass'
    }
    
    slave_configs = [
        {
            'host': 'slave1.example.com',
            'user': 'audit',
            'password': 'audit_pass'
        },
        {
            'host': 'slave2.example.com',
            'user': 'audit',
            'password': 'audit_pass'
        }
    ]
    
    auditor = ReplicationSecurityAuditor(master_config, slave_configs)
    results = auditor.audit_replication_security()
    
    print(json.dumps(results, indent=2, default=str))
```

## SSL/TLS Troubleshooting

### SSL Configuration Validation

```sql
-- Check SSL support and configuration
SELECT 
    @@have_ssl as ssl_support,
    @@ssl_ca as ssl_ca,
    @@ssl_cert as ssl_cert,
    @@ssl_key as ssl_key,
    @@require_secure_transport as require_secure_transport;

-- Check current connection encryption
SHOW STATUS LIKE 'Ssl%';

-- Check user SSL requirements
SELECT User, Host, ssl_type, ssl_cipher, x509_issuer, x509_subject 
FROM mysql.user 
WHERE ssl_type != '' OR ssl_cipher != '' OR x509_issuer != '' OR x509_subject != '';
```

### SSL Certificate Validation Script

```bash
#!/bin/bash
# ssl-certificate-validation.sh

SSL_DIR="/etc/mysql/ssl"
MYSQL_USER="root"

echo "Validating MySQL SSL certificates..."

# Check if SSL files exist
for file in ca.pem server-cert.pem server-key.pem; do
    if [ ! -f "$SSL_DIR/$file" ]; then
        echo "ERROR: SSL file $SSL_DIR/$file not found"
        exit 1
    fi
done

# Check certificate validity
echo "Checking certificate validity..."
openssl x509 -in "$SSL_DIR/server-cert.pem" -text -noout | grep -A 2 "Validity"

# Check certificate expiration
EXPIRY_DATE=$(openssl x509 -in "$SSL_DIR/server-cert.pem" -enddate -noout | cut -d= -f2)
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( (EXPIRY_EPOCH - CURRENT_EPOCH) / 86400 ))

if [ $DAYS_UNTIL_EXPIRY -lt 30 ]; then
    echo "WARNING: SSL certificate expires in $DAYS_UNTIL_EXPIRY days"
elif [ $DAYS_UNTIL_EXPIRY -lt 0 ]; then
    echo "ERROR: SSL certificate has expired"
    exit 1
else
    echo "SSL certificate is valid for $DAYS_UNTIL_EXPIRY more days"
fi

# Test SSL connection
echo "Testing SSL connection..."
mysql -u $MYSQL_USER -p --ssl-mode=REQUIRED -e "SHOW STATUS LIKE 'Ssl_cipher'" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "SSL connection test: PASSED"
else
    echo "SSL connection test: FAILED"
    exit 1
fi

echo "SSL validation completed successfully"
```

## Audit Log Analysis

### Comprehensive Audit Log Analysis

```python
#!/usr/bin/env python3
# audit-log-analyzer.py

import re
import json
from datetime import datetime, timedelta
from collections import defaultdict

class AuditLogAnalyzer:
    def __init__(self, log_file_path):
        self.log_file_path = log_file_path
        self.security_patterns = {
            'privilege_escalation': [
                r'GRANT\s+.*\s+TO\s+',
                r'REVOKE\s+.*\s+FROM\s+',
                r'CREATE\s+USER\s+',
                r'DROP\s+USER\s+',
                r'ALTER\s+USER\s+'
            ],
            'function_creation': [
                r'CREATE\s+FUNCTION\s+',
                r'CREATE\s+PROCEDURE\s+',
                r'CREATE\s+TRIGGER\s+',
                r'DROP\s+FUNCTION\s+',
                r'DROP\s+PROCEDURE\s+',
                r'DROP\s+TRIGGER\s+'
            ],
            'suspicious_activity': [
                r'LOAD_FILE\s*\(',
                r'INTO\s+OUTFILE\s+',
                r'SYSTEM\s*\(',
                r'SHELL\s*\(',
                r'@@\w+'
            ],
            'failed_authentication': [
                r'Access denied for user',
                r'authentication failure',
                r'login failed'
            ]
        }
    
    def parse_log_entry(self, line):
        """Parse a single log entry."""
        # Basic log parsing - adjust based on your log format
        timestamp_match = re.match(r'(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})', line)
        if timestamp_match:
            timestamp = datetime.fromisoformat(timestamp_match.group(1))
            return {
                'timestamp': timestamp,
                'raw_line': line,
                'content': line[timestamp_match.end():].strip()
            }
        return None
    
    def analyze_security_events(self, time_window_hours=24):
        """Analyze security events in the specified time window."""
        cutoff_time = datetime.now() - timedelta(hours=time_window_hours)
        security_events = defaultdict(list)
        
        with open(self.log_file_path, 'r') as f:
            for line in f:
                entry = self.parse_log_entry(line)
                if not entry or entry['timestamp'] < cutoff_time:
                    continue
                
                # Check for security patterns
                for category, patterns in self.security_patterns.items():
                    for pattern in patterns:
                        if re.search(pattern, entry['content'], re.IGNORECASE):
                            security_events[category].append({
                                'timestamp': entry['timestamp'].isoformat(),
                                'pattern': pattern,
                                'content': entry['content'][:200]  # Truncate for readability
                            })
        
        return dict(security_events)
    
    def generate_security_report(self, events):
        """Generate a security report from analyzed events."""
        report = f"""
MySQL Security Audit Log Analysis
Generated: {datetime.now().isoformat()}
Analysis Period: Last 24 hours

SUMMARY:
"""
        
        total_events = sum(len(events[category]) for category in events)
        report += f"Total Security Events: {total_events}\n\n"
        
        for category, event_list in events.items():
            if event_list:
                report += f"{category.upper().replace('_', ' ')} ({len(event_list)} events):\n"
                
                # Show recent events
                for event in event_list[-5:]:  # Last 5 events
                    report += f"  {event['timestamp']}: {event['content']}\n"
                
                if len(event_list) > 5:
                    report += f"  ... and {len(event_list) - 5} more events\n"
                
                report += "\n"
        
        return report
    
    def detect_anomalies(self, events):
        """Detect anomalous patterns in security events."""
        anomalies = []
        
        # Check for privilege escalation spikes
        privilege_events = events.get('privilege_escalation', [])
        if len(privilege_events) > 10:  # Threshold
            anomalies.append({
                'type': 'privilege_escalation_spike',
                'severity': 'HIGH',
                'description': f'Unusual number of privilege changes: {len(privilege_events)}',
                'events': privilege_events
            })
        
        # Check for failed authentication patterns
        failed_auth = events.get('failed_authentication', [])
        if len(failed_auth) > 50:  # Threshold
            anomalies.append({
                'type': 'brute_force_attempt',
                'severity': 'HIGH',
                'description': f'Possible brute force attack: {len(failed_auth)} failed attempts',
                'events': failed_auth[-10:]  # Last 10 attempts
            })
        
        # Check for suspicious activity
        suspicious = events.get('suspicious_activity', [])
        if suspicious:
            anomalies.append({
                'type': 'suspicious_activity_detected',
                'severity': 'MEDIUM',
                'description': f'Suspicious database operations detected: {len(suspicious)}',
                'events': suspicious
            })
        
        return anomalies

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) != 2:
        print("Usage: python3 audit-log-analyzer.py <log_file_path>")
        sys.exit(1)
    
    analyzer = AuditLogAnalyzer(sys.argv[1])
    events = analyzer.analyze_security_events()
    
    # Generate report
    report = analyzer.generate_security_report(events)
    print(report)
    
    # Detect anomalies
    anomalies = analyzer.detect_anomalies(events)
    if anomalies:
        print("ANOMALIES DETECTED:")
        for anomaly in anomalies:
            print(f"  {anomaly['severity']}: {anomaly['description']}")
    
    # Save detailed results
    with open(f"security-audit-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json", 'w') as f:
        json.dump({
            'events': events,
            'anomalies': anomalies,
            'timestamp': datetime.now().isoformat()
        }, f, indent=2, default=str)
```

## Emergency Procedures

### Security Incident Response Playbook

#### Immediate Response (0-15 minutes)

1. **Isolate the System**:
   ```bash
   # Block all connections except from localhost
   mysql -u root -p -e "SET GLOBAL max_connections = 1"
   
   # Or stop MySQL service entirely
   sudo systemctl stop mysql
   ```

2. **Preserve Evidence**:
   ```bash
   # Create incident directory
   INCIDENT_DIR="/tmp/mysql-incident-$(date +%Y%m%d-%H%M%S)"
   mkdir -p "$INCIDENT_DIR"
   
   # Copy critical files
   cp /var/log/mysql/*.log "$INCIDENT_DIR/"
   cp /etc/mysql/my.cnf "$INCIDENT_DIR/"
   
   # Dump current state
   mysqldump --all-databases --routines --triggers > "$INCIDENT_DIR/full-dump.sql"
   ```

3. **Assess Damage**:
   ```sql
   -- Check for unauthorized changes
   SELECT * FROM mysql.user WHERE Password_last_changed >= DATE_SUB(NOW(), INTERVAL 1 HOUR);
   
   -- Check recent function creation
   SELECT * FROM INFORMATION_SCHEMA.ROUTINES 
   WHERE CREATED >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
   AND ROUTINE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys');
   ```

#### Short-term Response (15 minutes - 1 hour)

1. **Secure the System**:
   ```sql
   -- Disable log_bin_trust_function_creators if enabled
   SET GLOBAL log_bin_trust_function_creators = 0;
   
   -- Lock suspicious accounts
   ALTER USER 'suspicious_user'@'%' ACCOUNT LOCK;
   
   -- Revoke dangerous privileges
   REVOKE SUPER ON *.* FROM 'compromised_user'@'%';
   ```

2. **Remove Malicious Objects**:
   ```sql
   -- Document before removal
   SELECT CONCAT('-- MALICIOUS FUNCTION: ', ROUTINE_NAME, '\n', ROUTINE_DEFINITION) 
   FROM INFORMATION_SCHEMA.ROUTINES 
   WHERE ROUTINE_NAME = 'malicious_function';
   
   -- Remove after documentation
   DROP FUNCTION IF EXISTS malicious_function;
   ```

3. **Implement Monitoring**:
   ```bash
   # Start continuous monitoring
   nohup python3 privilege-monitor.py > /var/log/privilege-monitor.log 2>&1 &
   ```

#### Long-term Response (1+ hours)

1. **Full Security Audit**:
   ```bash
   # Run comprehensive security audit
   python3 function-security-audit.py > security-audit-post-incident.txt
   
   # Analyze binary logs
   ./binlog-security-analysis.sh
   ```

2. **System Hardening**:
   ```sql
   -- Implement stricter security
   SET GLOBAL log_bin_trust_function_creators = 0;
   SET GLOBAL require_secure_transport = ON;
   
   -- Review all user privileges
   SELECT User, Host, Super_priv, Grant_priv FROM mysql.user;
   ```

3. **Recovery Validation**:
   ```bash
   # Validate system integrity
   ./validate-config.sh --environment production
   
   # Test application functionality
   python3 -m pytest tests/integration/ -v
   ```

### Emergency Contact Information

```bash
# emergency-contacts.sh
cat << EOF
MySQL Security Emergency Contacts

Primary DBA: John Doe <john.doe@company.com> +1-555-0101
Security Team: security@company.com +1-555-0102
Infrastructure Team: infra@company.com +1-555-0103

Escalation Procedures:
1. Notify Primary DBA immediately
2. If no response in 15 minutes, contact Security Team
3. For critical incidents, contact Infrastructure Team

Incident Reporting:
- Create ticket in ServiceNow
- Send email to security@company.com
- Update incident status every 30 minutes

Recovery Procedures:
- Follow documented recovery playbook
- Validate all changes before production deployment
- Conduct post-incident review within 24 hours
EOF
```

### Automated Emergency Response

```python
#!/usr/bin/env python3
# emergency-response.py

import mysql.connector
import smtplib
import logging
from email.mime.text import MIMEText
from datetime import datetime

class EmergencyResponse:
    def __init__(self, mysql_config, email_config):
        self.mysql_config = mysql_config
        self.email_config = email_config
        self.logger = logging.getLogger(__name__)
    
    def emergency_lockdown(self):
        """Emergency lockdown procedure."""
        try:
            connection = mysql.connector.connect(**self.mysql_config)
            cursor = connection.cursor()
            
            # Reduce max connections to minimum
            cursor.execute("SET GLOBAL max_connections = 10")
            
            # Disable log_bin_trust_function_creators
            cursor.execute("SET GLOBAL log_bin_trust_function_creators = 0")
            
            # Log the action
            self.logger.critical("Emergency lockdown activated")
            
            cursor.close()
            connection.close()
            
            return True
            
        except Exception as e:
            self.logger.error(f"Emergency lockdown failed: {e}")
            return False
    
    def send_emergency_alert(self, message):
        """Send emergency alert email."""
        try:
            msg = MIMEText(f"""
MYSQL SECURITY EMERGENCY ALERT

Timestamp: {datetime.now().isoformat()}
Message: {message}

Immediate action required. Check MySQL security logs and system status.

This is an automated alert from the MySQL security monitoring system.
""")
            
            msg['Subject'] = 'URGENT: MySQL Security Emergency'
            msg['From'] = self.email_config['from']
            msg['To'] = ', '.join(self.email_config['to'])
            
            # Send email (configure SMTP settings)
            # smtp_server.send_message(msg)
            
            self.logger.critical(f"Emergency alert sent: {message}")
            
        except Exception as e:
            self.logger.error(f"Failed to send emergency alert: {e}")
    
    def trigger_emergency_response(self, threat_level, details):
        """Trigger appropriate emergency response based on threat level."""
        if threat_level == 'CRITICAL':
            # Full lockdown
            if self.emergency_lockdown():
                self.send_emergency_alert(f"Critical threat detected. System locked down. Details: {details}")
            else:
                self.send_emergency_alert(f"Critical threat detected. LOCKDOWN FAILED. Manual intervention required. Details: {details}")
        
        elif threat_level == 'HIGH':
            # Alert only, no automatic lockdown
            self.send_emergency_alert(f"High-level security threat detected. Review required. Details: {details}")
        
        elif threat_level == 'MEDIUM':
            # Log and monitor
            self.logger.warning(f"Medium-level security event: {details}")

# Example usage
if __name__ == "__main__":
    mysql_config = {
        'host': 'localhost',
        'user': 'emergency',
        'password': 'emergency_pass'
    }
    
    email_config = {
        'from': 'mysql-security@company.com',
        'to': ['dba@company.com', 'security@company.com']
    }
    
    emergency = EmergencyResponse(mysql_config, email_config)
    
    # Example: Trigger emergency response for critical threat
    emergency.trigger_emergency_response('CRITICAL', 'Unauthorized SUPER privilege grant detected')
```

---

This security troubleshooting guide provides comprehensive procedures for detecting, analyzing, and responding to security incidents in MySQL privilege configuration environments. Regular practice of these procedures and maintenance of the monitoring systems is essential for effective security management.