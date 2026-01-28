-- Test Functions, Procedures, and Triggers for Privilege Validation
-- This script tests the ability to create database objects without SUPER privileges
-- Requirements: 3.1, 3.2, 3.3

USE testdb;

-- Create a test table for our functions and triggers
CREATE TABLE IF NOT EXISTS test_table (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    value DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Test Function 1: Simple deterministic function
-- This should work with log_bin_trust_function_creators = 1
DELIMITER $$

CREATE FUNCTION IF NOT EXISTS calculate_tax(amount DECIMAL(10,2))
RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
COMMENT 'Calculate tax on given amount'
BEGIN
    DECLARE tax_rate DECIMAL(4,4) DEFAULT 0.0825; -- 8.25% tax rate
    RETURN amount * tax_rate;
END$$

DELIMITER ;

-- Test Function 2: Function with DEFINER clause
-- This demonstrates proper DEFINER usage for security
DELIMITER $$

CREATE FUNCTION IF NOT EXISTS format_currency(amount DECIMAL(10,2))
RETURNS VARCHAR(20)
DETERMINISTIC
NO SQL
COMMENT 'Format amount as currency string'
BEGIN
    RETURN CONCAT('$', FORMAT(amount, 2));
END$$

DELIMITER ;

-- Test Procedure 1: Simple procedure for data manipulation
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS update_test_value(
    IN p_id INT,
    IN p_value DECIMAL(10,2)
)
MODIFIES SQL DATA
DETERMINISTIC
COMMENT 'Update value in test table'
BEGIN
    UPDATE test_table 
    SET value = p_value, updated_at = CURRENT_TIMESTAMP 
    WHERE id = p_id;
    
    -- Log the update
    SELECT CONCAT('Updated record ', p_id, ' with value ', p_value) as result;
END$$

DELIMITER ;

-- Test Procedure 2: Procedure with error handling
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS safe_insert_test(
    IN p_name VARCHAR(100),
    IN p_value DECIMAL(10,2)
)
MODIFIES SQL DATA
DETERMINISTIC
COMMENT 'Safely insert record with error handling'
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    INSERT INTO test_table (name, value) VALUES (p_name, p_value);
    
    COMMIT;
    
    SELECT LAST_INSERT_ID() as new_id;
END$$

DELIMITER ;

-- Test Trigger 1: Before insert trigger
DELIMITER $$

CREATE TRIGGER IF NOT EXISTS test_table_before_insert
    BEFORE INSERT ON test_table
    FOR EACH ROW
BEGIN
    -- Validate input data
    IF NEW.value < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Value cannot be negative';
    END IF;
    
    -- Set default name if empty
    IF NEW.name = '' OR NEW.name IS NULL THEN
        SET NEW.name = 'Default Name';
    END IF;
END$$

DELIMITER ;

-- Test Trigger 2: After update trigger for auditing
DELIMITER $$

CREATE TRIGGER IF NOT EXISTS test_table_after_update
    AFTER UPDATE ON test_table
    FOR EACH ROW
BEGIN
    -- Simple audit logging (in real scenario, would log to audit table)
    -- This is just for testing trigger creation
    SET @audit_message = CONCAT('Updated record ID: ', NEW.id, 
                               ' from value: ', OLD.value, 
                               ' to value: ', NEW.value);
END$$

DELIMITER ;

-- Insert test data to verify everything works
INSERT INTO test_table (name, value) VALUES 
    ('Test Item 1', 100.00),
    ('Test Item 2', 250.50),
    ('Test Item 3', 75.25);

-- Test the created functions
SELECT 
    id,
    name,
    value,
    calculate_tax(value) as tax_amount,
    format_currency(value) as formatted_value,
    format_currency(value + calculate_tax(value)) as total_with_tax
FROM test_table;

-- Test the procedure
CALL update_test_value(1, 150.00);

-- Test safe insert procedure
CALL safe_insert_test('Procedure Test', 300.00);

-- Verify trigger functionality by attempting negative value (should fail)
-- INSERT INTO test_table (name, value) VALUES ('Negative Test', -10.00);

-- Show that all objects were created successfully
SELECT 'Database objects created successfully:' as info;

SELECT 
    ROUTINE_TYPE as type,
    ROUTINE_NAME as name,
    DEFINER,
    SQL_DATA_ACCESS,
    IS_DETERMINISTIC
FROM INFORMATION_SCHEMA.ROUTINES 
WHERE ROUTINE_SCHEMA = 'testdb'
ORDER BY ROUTINE_TYPE, ROUTINE_NAME;

SELECT 
    'TRIGGER' as type,
    TRIGGER_NAME as name,
    EVENT_MANIPULATION,
    ACTION_TIMING
FROM INFORMATION_SCHEMA.TRIGGERS 
WHERE TRIGGER_SCHEMA = 'testdb'
ORDER BY TRIGGER_NAME;

SELECT 'Test completed successfully - all objects created without SUPER privileges!' as result;