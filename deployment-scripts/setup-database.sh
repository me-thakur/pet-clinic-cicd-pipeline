#!/bin/bash
set -e

# Pet Clinic Database Setup Script
# This script sets up the database schema and initial data

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-petclinic}"
DB_USER="${DB_USER:-petclinic}"
DB_PASSWORD="${DB_PASSWORD}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# Validate required environment variables
if [[ -z "$DB_PASSWORD" ]]; then
    error "DB_PASSWORD environment variable is required"
fi

if [[ -z "$DB_ROOT_PASSWORD" ]]; then
    error "DB_ROOT_PASSWORD environment variable is required"
fi

log "Setting up Pet Clinic database"

# Test database connectivity
log "Testing database connectivity"
mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$DB_ROOT_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1 || error "Cannot connect to database server"

# Create database if it doesn't exist
log "Creating database if it doesn't exist"
mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$DB_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Create user if it doesn't exist
log "Creating database user if it doesn't exist"
mysql -h "$DB_HOST" -P "$DB_PORT" -u root -p"$DB_ROOT_PASSWORD" << EOF
CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'%';
FLUSH PRIVILEGES;
EOF

# Create initial schema
log "Creating initial database schema"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" << 'EOF'
-- Pet Clinic Database Schema

-- Create owners table
CREATE TABLE IF NOT EXISTS owners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    telephone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owners_last_name (last_name),
    INDEX idx_owners_email (email)
);

-- Create veterinarians table
CREATE TABLE IF NOT EXISTS veterinarians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    specialties VARCHAR(255),
    license_number VARCHAR(50) UNIQUE,
    phone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vets_license (license_number),
    INDEX idx_vets_specialties (specialties)
);

-- Create pets table
CREATE TABLE IF NOT EXISTS pets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    birth_date DATE,
    type VARCHAR(50) NOT NULL,
    breed VARCHAR(100),
    color VARCHAR(50),
    weight DECIMAL(5,2),
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE,
    INDEX idx_pets_owner (owner_id),
    INDEX idx_pets_type (type),
    INDEX idx_pets_name (name)
);

-- Create visits table
CREATE TABLE IF NOT EXISTS visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_date DATE NOT NULL,
    description TEXT,
    diagnosis VARCHAR(500),
    treatment VARCHAR(500),
    cost DECIMAL(10,2),
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE,
    FOREIGN KEY (veterinarian_id) REFERENCES veterinarians(id),
    INDEX idx_visits_pet (pet_id),
    INDEX idx_visits_vet (veterinarian_id),
    INDEX idx_visits_date (visit_date)
);

-- Create flyway schema history table for migration tracking
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank),
    INDEX flyway_schema_history_s_idx (success)
);
EOF

# Insert sample data
log "Inserting sample data"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" << 'EOF'
-- Sample data for Pet Clinic

-- Insert sample owners
INSERT IGNORE INTO owners (id, first_name, last_name, address, city, telephone, email) VALUES
(1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023', 'george.franklin@email.com'),
(2, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749', 'betty.davis@email.com'),
(3, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763', 'eduardo.rodriguez@email.com'),
(4, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198', 'harold.davis@email.com'),
(5, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765', 'peter.mctavish@email.com');

-- Insert sample veterinarians
INSERT IGNORE INTO veterinarians (id, first_name, last_name, specialties, license_number, phone, email) VALUES
(1, 'James', 'Carter', 'Surgery, Radiology', 'VET001', '6085551234', 'james.carter@petclinic.com'),
(2, 'Helen', 'Leary', 'Dentistry', 'VET002', '6085555678', 'helen.leary@petclinic.com'),
(3, 'Linda', 'Douglas', 'Surgery, Dermatology', 'VET003', '6085559012', 'linda.douglas@petclinic.com'),
(4, 'Rafael', 'Ortega', 'Surgery', 'VET004', '6085553456', 'rafael.ortega@petclinic.com'),
(5, 'Henry', 'Stevens', 'Radiology', 'VET005', '6085557890', 'henry.stevens@petclinic.com');

-- Insert sample pets
INSERT IGNORE INTO pets (id, name, birth_date, type, breed, color, weight, owner_id) VALUES
(1, 'Leo', '2010-09-07', 'Cat', 'Persian', 'Orange', 4.2, 1),
(2, 'Basil', '2012-08-06', 'Hamster', 'Golden', 'Brown', 0.3, 2),
(3, 'Rosy', '2011-04-17', 'Dog', 'Golden Retriever', 'Golden', 25.5, 3),
(4, 'Jewel', '2010-03-07', 'Dog', 'Border Collie', 'Black and White', 18.7, 3),
(5, 'Iggy', '2010-11-30', 'Lizard', 'Iguana', 'Green', 2.1, 4),
(6, 'George', '2010-01-20', 'Snake', 'Python', 'Brown', 3.8, 5),
(7, 'Samantha', '2012-09-04', 'Cat', 'Siamese', 'Gray', 3.9, 5),
(8, 'Max', '2012-09-04', 'Dog', 'Beagle', 'Tricolor', 12.3, 5);

-- Insert sample visits
INSERT IGNORE INTO visits (id, visit_date, description, diagnosis, treatment, cost, pet_id, veterinarian_id) VALUES
(1, '2013-01-01', 'Rabies shot', 'Routine vaccination', 'Rabies vaccine administered', 45.00, 7, 1),
(2, '2013-01-02', 'Spayed', 'Routine spaying procedure', 'Ovariohysterectomy performed', 150.00, 8, 2),
(3, '2013-01-03', 'Neutered', 'Routine neutering procedure', 'Castration performed', 120.00, 8, 2),
(4, '2013-01-04', 'Broken rib', 'Fractured rib from accident', 'Pain medication and rest', 200.00, 7, 1);
EOF

# Verify database setup
log "Verifying database setup"
OWNER_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM owners;")
PET_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM pets;")
VET_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM veterinarians;")
VISIT_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM visits;")

log "✓ Database setup completed successfully"
log "  - Owners: $OWNER_COUNT"
log "  - Pets: $PET_COUNT"
log "  - Veterinarians: $VET_COUNT"
log "  - Visits: $VISIT_COUNT"

# Test application user permissions
log "Testing application user permissions"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT 'Database access verified' as status;" || error "Application user cannot access database"

log "✓ Database setup and verification completed successfully"