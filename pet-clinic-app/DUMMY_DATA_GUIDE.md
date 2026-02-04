# Pet Clinic Dummy Data Guide

## Overview

This guide explains the dummy data system implemented for the Pet Clinic application, including automated seeding, manual generation, and data management capabilities.

## Dummy Data Sources

### 1. Migration-Based Data (V10__Add_Comprehensive_Dummy_Data.sql)

**Location**: `src/main/resources/db/migration/V10__Add_Comprehensive_Dummy_Data.sql`

**Content**:
- 20 veterinarians with diverse specialties
- 50 pet owners with realistic contact information
- 70+ pets (dogs, cats, and exotic animals)
- 25+ visits with various scenarios (emergency, routine, surgical)
- 15+ appointments (past and future)
- Vaccination records
- Medical records with detailed findings
- System settings and audit logs

**Features**:
- Celebrity and fictional character names for memorable testing
- Diverse pet breeds and species
- Realistic medical histories and conditions
- Various visit types and costs
- Complete vaccination schedules

### 2. Programmatic Data Seeding (DataSeedingService)

**Location**: `src/main/java/com/petclinic/backend/service/DataSeedingService.java`

**Capabilities**:
- Automatic data generation on application startup
- Configurable data volumes
- Realistic random data generation
- Respects existing data (skip if data exists)

**Configuration Properties**:
```yaml
pet-clinic:
  data-seeding:
    enabled: true                    # Enable/disable automatic seeding
    skip-if-data-exists: true       # Skip if database already has data
    owner-count: 15                 # Number of owners to generate
    max-pets-per-owner: 3          # Maximum pets per owner
    max-visits-per-pet: 5          # Maximum visits per pet
```

### 3. Quick Test Data (generate-test-data.sql)

**Location**: `generate-test-data.sql`

**Purpose**: Minimal dataset for immediate testing
**Content**:
- 5 test owners
- 8 test pets
- 5 visits (completed, in-progress, scheduled)
- 3 appointments
- Sample vaccinations and medical records

## Data Management API

### Admin Endpoints (Development/Staging Only)

**Base URL**: `/api/admin/data-seeding`

#### 1. Seed Database
```http
POST /api/admin/data-seeding/seed
Authorization: Bearer <admin-jwt-token>
```

**Response**:
```json
{
  "success": true,
  "message": "Database seeded successfully",
  "data": {
    "before": {
      "owners": 10,
      "pets": 15,
      "visits": 8
    },
    "after": {
      "owners": 25,
      "pets": 45,
      "visits": 33
    }
  }
}
```

#### 2. Get Statistics
```http
GET /api/admin/data-seeding/statistics
Authorization: Bearer <admin-jwt-token>
```

#### 3. Clear Dummy Data
```http
DELETE /api/admin/data-seeding/clear
Authorization: Bearer <admin-jwt-token>
```

#### 4. Reset Database
```http
POST /api/admin/data-seeding/reset
Authorization: Bearer <admin-jwt-token>
```

## Data Categories

### Pet Owners
- **Realistic Names**: Mix of common and celebrity names
- **Complete Addresses**: Street, city, state, zip code
- **Contact Information**: Phone numbers and email addresses
- **Geographic Diversity**: Multiple cities and states

### Pets
- **Species Variety**: Dogs, cats, birds, reptiles, fish, rabbits, hamsters
- **Breed Diversity**: 20+ dog breeds, 20+ cat breeds, exotic species
- **Age Range**: Puppies/kittens to senior pets (15+ years)
- **Medical Histories**: Various conditions and health statuses

### Veterinarians
- **Specializations**: 20 different specialties
- **Multiple Specialties**: Each vet can have 1-2 specializations
- **Contact Information**: Professional email addresses and phone numbers

### Visits and Appointments
- **Visit Types**: Emergency, routine, surgical, dental, behavioral
- **Time Distribution**: Past year of visits, future appointments
- **Cost Variation**: $15 (nail trim) to $2500 (major surgery)
- **Status Variety**: Completed, in-progress, scheduled, confirmed

### Medical Records
- **Detailed Findings**: Comprehensive examination notes
- **Treatment Plans**: Specific medications and procedures
- **Follow-up Care**: Recommendations and next steps
- **Professional Language**: Veterinary terminology and format

## Usage Scenarios

### 1. Development Testing
```bash
# Start application with automatic seeding
export SPRING_PROFILES_ACTIVE=dev
export JWT_SECRET=your-development-jwt-secret
mvn spring-boot:run
```

### 2. Manual Data Generation
```bash
# Load quick test data
mysql -u username -p petclinicdb < generate-test-data.sql

# Or use the API endpoint
curl -X POST http://localhost:9090/api/admin/data-seeding/seed \
  -H "Authorization: Bearer <admin-token>"
```

### 3. Demo Environment Setup
```bash
# Configure for demo with extensive data
export pet-clinic.data-seeding.owner-count=50
export pet-clinic.data-seeding.max-pets-per-owner=4
export pet-clinic.data-seeding.max-visits-per-pet=8
```

### 4. Testing Specific Scenarios
```sql
-- Find pets with specific conditions
SELECT p.name, p.species, p.medical_history, o.first_name, o.last_name
FROM pets p
JOIN owners o ON p.owner_id = o.id
WHERE p.medical_history LIKE '%arthritis%';

-- Find upcoming appointments
SELECT a.appointment_date, a.appointment_time, p.name as pet_name, 
       v.first_name as vet_name, a.reason
FROM appointments a
JOIN pets p ON a.pet_id = p.id
JOIN veterinarians v ON a.vet_id = v.id
WHERE a.appointment_date >= CURDATE()
ORDER BY a.appointment_date, a.appointment_time;

-- Find overdue vaccinations
SELECT p.name, v.vaccine_name, v.next_due_date, o.first_name, o.last_name
FROM vaccinations v
JOIN pets p ON v.pet_id = p.id
JOIN owners o ON p.owner_id = o.id
WHERE v.next_due_date < CURDATE();
```

## Data Relationships

### Owner → Pets (1:Many)
- Each owner can have multiple pets
- Pets belong to exactly one owner
- Realistic distribution: 1-4 pets per owner

### Pet → Visits (1:Many)
- Each pet can have multiple visits
- Visits belong to exactly one pet
- Historical data spanning the past year

### Veterinarian → Visits (1:Many)
- Each vet can handle multiple visits
- Visits assigned to one primary vet
- Specialization matching for complex cases

### Visit → Medical Records (1:Many)
- Each visit can have multiple medical records
- Records provide detailed documentation
- Different record types: examination, surgery, emergency

## Data Quality Features

### Realistic Constraints
- Pet ages appropriate for species
- Visit costs realistic for procedures
- Appointment times during business hours
- Vaccination schedules following veterinary standards

### Data Integrity
- Foreign key relationships maintained
- No orphaned records
- Consistent data formats
- Proper date sequencing

### Testing Scenarios
- Emergency cases for testing urgent workflows
- Routine visits for standard operations
- Complex medical histories for advanced features
- Various appointment statuses for scheduling tests

## Customization

### Adding New Data Types
1. Update the migration file with new categories
2. Extend DataSeedingService with new generation methods
3. Add configuration properties for new data volumes
4. Update the API endpoints to handle new data types

### Modifying Data Volumes
```yaml
# application.yml
pet-clinic:
  data-seeding:
    enabled: true
    owner-count: 100        # Increase for stress testing
    max-pets-per-owner: 5   # More pets per owner
    max-visits-per-pet: 10  # More visit history
```

### Custom Data Sets
```sql
-- Create custom test scenarios
INSERT INTO owners (first_name, last_name, email, telephone) VALUES
('Test', 'User', 'test@example.com', '555-TEST');

INSERT INTO pets (name, species, breed, owner_id) VALUES
('TestPet', 'Dog', 'Test Breed', LAST_INSERT_ID());
```

## Troubleshooting

### Common Issues

1. **Seeding Fails on Startup**
   - Check database connection
   - Verify Flyway migrations completed
   - Ensure JWT_SECRET is configured

2. **Duplicate Data Errors**
   - Set `skip-if-data-exists: true`
   - Clear existing data before seeding
   - Check unique constraints

3. **Performance Issues**
   - Reduce data volumes for development
   - Use indexes on frequently queried columns
   - Consider pagination for large datasets

### Debugging
```bash
# Enable debug logging for data seeding
logging.level.com.petclinic.backend.service.DataSeedingService=DEBUG

# Check seeding status
curl http://localhost:9090/api/admin/data-seeding/statistics
```

## Best Practices

1. **Development**: Use moderate data volumes (15-25 owners)
2. **Testing**: Use consistent test data for reproducible tests
3. **Demo**: Use extensive data with interesting scenarios
4. **Production**: Never enable data seeding in production

## Security Considerations

- Data seeding endpoints only available in dev/staging profiles
- Admin authentication required for all seeding operations
- No sensitive real data in dummy datasets
- Clear separation between test and production data

## Maintenance

### Regular Tasks
1. Update dummy data to reflect new features
2. Add new test scenarios as application evolves
3. Maintain realistic data relationships
4. Update documentation when adding new data types

### Version Control
- Migration files are versioned and immutable
- Service code changes are tracked in Git
- Configuration changes documented in release notes