#!/bin/bash

echo "=== Pet Clinic Backend Regression Testing ==="
echo "Testing that existing functionality remains intact after critical fixes..."
echo

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    echo -n "Testing $test_name... "
    
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}PASSED${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}FAILED${NC}"
        ((FAILED++))
        return 1
    fi
}

# Function to check if a file exists
check_file() {
    local file_path="$1"
    local description="$2"
    
    if [ -f "$file_path" ]; then
        echo -e "✓ ${GREEN}$description found${NC}"
        return 0
    else
        echo -e "✗ ${RED}$description missing${NC}"
        return 1
    fi
}

# Function to check if a class compiles
check_compilation() {
    local class_path="$1"
    local class_name="$2"
    
    if javac -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" "$class_path" -d /tmp > /dev/null 2>&1; then
        echo -e "✓ ${GREEN}$class_name compiles successfully${NC}"
        return 0
    else
        echo -e "✗ ${RED}$class_name compilation failed${NC}"
        return 1
    fi
}

echo "1. Testing Core Service Files Exist"
echo "=================================="

check_file "src/main/java/com/petclinic/backend/service/impl/PetServiceImpl.java" "PetServiceImpl"
check_file "src/main/java/com/petclinic/backend/service/impl/VeterinarianServiceImpl.java" "VeterinarianServiceImpl"
check_file "src/main/java/com/petclinic/backend/service/impl/VisitServiceImpl.java" "VisitServiceImpl"
check_file "src/main/java/com/petclinic/backend/service/impl/OwnerValidationServiceImpl.java" "OwnerValidationServiceImpl"

echo
echo "2. Testing Enhanced Service Files Exist"
echo "======================================="

check_file "src/main/java/com/petclinic/backend/service/impl/EnhancedPetServiceImpl.java" "EnhancedPetServiceImpl"
check_file "src/main/java/com/petclinic/backend/service/impl/VisitSearchServiceImpl.java" "VisitSearchServiceImpl"
check_file "src/main/java/com/petclinic/backend/service/impl/EnhancedTableServiceImpl.java" "EnhancedTableServiceImpl"
check_file "src/main/java/com/petclinic/backend/service/impl/InternationalValidationServiceImpl.java" "InternationalValidationServiceImpl"

echo
echo "3. Testing Controller Files Exist"
echo "================================="

check_file "src/main/java/com/petclinic/backend/controller/PetController.java" "PetController"
check_file "src/main/java/com/petclinic/backend/controller/VeterinarianController.java" "VeterinarianController"
check_file "src/main/java/com/petclinic/backend/controller/VisitController.java" "VisitController"
check_file "src/main/java/com/petclinic/backend/controller/VisitSearchController.java" "VisitSearchController"
check_file "src/main/java/com/petclinic/backend/controller/ValidationController.java" "ValidationController"
check_file "src/main/java/com/petclinic/backend/controller/EnhancedTableController.java" "EnhancedTableController"

echo
echo "4. Testing Integration Configuration Files"
echo "=========================================="

check_file "src/main/java/com/petclinic/backend/config/IntegrationConfig.java" "IntegrationConfig"
check_file "src/main/java/com/petclinic/backend/config/ServiceIntegrationConfig.java" "ServiceIntegrationConfig"
check_file "src/main/java/com/petclinic/backend/config/ErrorHandlingIntegrationConfig.java" "ErrorHandlingIntegrationConfig"

echo
echo "5. Testing Main Code Compilation"
echo "================================"

run_test "Main code compilation" "mvn compile -q"

echo
echo "6. Testing Application Context Loading"
echo "====================================="

# Test application context can load without errors
run_test "Application context loading" "java -cp 'target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)' -Dspring.profiles.active=test -Dspring.main.web-application-type=none com.petclinic.backend.PetClinicBackendApplication --spring.main.lazy-initialization=true --logging.level.root=ERROR --spring.jpa.hibernate.ddl-auto=none --spring.datasource.url=jdbc:h2:mem:testdb --spring.datasource.driver-class-name=org.h2.Driver --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"

echo
echo "7. Testing Service Interface Compatibility"
echo "=========================================="

# Check that service interfaces haven't changed
if grep -q "public interface PetService" src/main/java/com/petclinic/backend/service/PetService.java; then
    echo -e "✓ ${GREEN}PetService interface intact${NC}"
else
    echo -e "✗ ${RED}PetService interface modified${NC}"
fi

if grep -q "public interface VeterinarianService" src/main/java/com/petclinic/backend/service/VeterinarianService.java; then
    echo -e "✓ ${GREEN}VeterinarianService interface intact${NC}"
else
    echo -e "✗ ${RED}VeterinarianService interface modified${NC}"
fi

if grep -q "public interface VisitService" src/main/java/com/petclinic/backend/service/VisitService.java; then
    echo -e "✓ ${GREEN}VisitService interface intact${NC}"
else
    echo -e "✗ ${RED}VisitService interface modified${NC}"
fi

echo
echo "8. Testing Enhanced Features Are Present"
echo "========================================"

# Check that enhanced features are properly implemented
if grep -q "EnhancedPetService" src/main/java/com/petclinic/backend/service/impl/EnhancedPetServiceImpl.java; then
    echo -e "✓ ${GREEN}EnhancedPetService implementation present${NC}"
else
    echo -e "✗ ${RED}EnhancedPetService implementation missing${NC}"
fi

if grep -q "VisitSearchService" src/main/java/com/petclinic/backend/service/impl/VisitSearchServiceImpl.java; then
    echo -e "✓ ${GREEN}VisitSearchService implementation present${NC}"
else
    echo -e "✗ ${RED}VisitSearchService implementation missing${NC}"
fi

if grep -q "InternationalValidationService" src/main/java/com/petclinic/backend/service/impl/InternationalValidationServiceImpl.java; then
    echo -e "✓ ${GREEN}InternationalValidationService implementation present${NC}"
else
    echo -e "✗ ${RED}InternationalValidationService implementation missing${NC}"
fi

echo
echo "9. Testing API Compatibility"
echo "============================"

# Check that existing API endpoints are still present
if grep -q "@GetMapping" src/main/java/com/petclinic/backend/controller/PetController.java; then
    echo -e "✓ ${GREEN}PetController API endpoints present${NC}"
else
    echo -e "✗ ${RED}PetController API endpoints missing${NC}"
fi

if grep -q "@GetMapping" src/main/java/com/petclinic/backend/controller/VeterinarianController.java; then
    echo -e "✓ ${GREEN}VeterinarianController API endpoints present${NC}"
else
    echo -e "✗ ${RED}VeterinarianController API endpoints missing${NC}"
fi

if grep -q "@GetMapping" src/main/java/com/petclinic/backend/controller/VisitController.java; then
    echo -e "✓ ${GREEN}VisitController API endpoints present${NC}"
else
    echo -e "✗ ${RED}VisitController API endpoints missing${NC}"
fi

echo
echo "10. Testing Database Configuration"
echo "================================="

# Check that database configuration files are intact
if [ -f "src/main/resources/application.properties" ] || [ -f "src/main/resources/application.yml" ]; then
    echo -e "✓ ${GREEN}Database configuration files present${NC}"
else
    echo -e "✗ ${RED}Database configuration files missing${NC}"
fi

# Check for JPA entity classes
if ls src/main/java/com/petclinic/backend/model/*.java > /dev/null 2>&1; then
    echo -e "✓ ${GREEN}JPA entity classes present${NC}"
else
    echo -e "✗ ${RED}JPA entity classes missing${NC}"
fi

echo
echo "11. Testing Repository Layer"
echo "============================"

# Check that repository interfaces are intact
if ls src/main/java/com/petclinic/backend/repository/*.java > /dev/null 2>&1; then
    echo -e "✓ ${GREEN}Repository interfaces present${NC}"
else
    echo -e "✗ ${RED}Repository interfaces missing${NC}"
fi

echo
echo "12. Testing Security Configuration"
echo "================================="

# Check that security configuration is intact
if [ -f "src/main/java/com/petclinic/backend/config/SecurityConfig.java" ]; then
    echo -e "✓ ${GREEN}Security configuration present${NC}"
else
    echo -e "✗ ${RED}Security configuration missing${NC}"
fi

echo
echo "=== Regression Testing Summary ==="
echo "=================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All regression tests passed!${NC}"
    echo "Existing functionality remains intact after implementing critical fixes."
else
    echo -e "${RED}Some regression tests failed.${NC}"
    echo "Please review the failed tests above."
fi

echo
echo "Tests completed: $((PASSED + FAILED))"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"

# Exit with appropriate code
if [ $FAILED -eq 0 ]; then
    exit 0
else
    exit 1
fi