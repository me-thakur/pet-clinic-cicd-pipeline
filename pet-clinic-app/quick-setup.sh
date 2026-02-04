#!/bin/bash

# Quick Setup Script for Pet Clinic Dummy Data
# This script sets up the Pet Clinic application with dummy data for immediate testing

set -e

echo "🏥 Pet Clinic Quick Setup Script"
echo "================================"

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: Please run this script from the pet-clinic-app directory"
    exit 1
fi

# Set environment variables for development
echo "📝 Setting up environment variables..."
export SPRING_PROFILES_ACTIVE=dev
export JWT_SECRET="pet-clinic-development-jwt-secret-key-for-testing-purposes-only-do-not-use-in-production"
export CORS_ALLOWED_ORIGINS="http://localhost:8080,http://localhost:3000"
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=petclinicdb
export DB_USERNAME=petclinicadmin
export DB_PASSWORD=password

echo "✅ Environment variables configured"

# Create .env file for persistence
echo "📄 Creating .env file..."
cat > .env << EOF
# Pet Clinic Development Environment Configuration
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=pet-clinic-development-jwt-secret-key-for-testing-purposes-only-do-not-use-in-production
CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000
DB_HOST=localhost
DB_PORT=3306
DB_NAME=petclinicdb
DB_USERNAME=petclinicadmin
DB_PASSWORD=password
SESSION_TIMEOUT=1800000
BACKEND_URL=http://localhost:9090
EOF

echo "✅ .env file created"

# Check if MySQL is running (optional)
echo "🔍 Checking MySQL connection..."
if command -v mysql &> /dev/null; then
    if mysql -h$DB_HOST -P$DB_PORT -u$DB_USERNAME -p$DB_PASSWORD -e "SELECT 1;" &> /dev/null; then
        echo "✅ MySQL connection successful"
        
        # Load quick test data
        echo "📊 Loading quick test data..."
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USERNAME -p$DB_PASSWORD $DB_NAME < generate-test-data.sql
        echo "✅ Quick test data loaded"
    else
        echo "⚠️  MySQL connection failed - will use H2 in-memory database"
        echo "   To use MySQL, ensure it's running and credentials are correct"
    fi
else
    echo "⚠️  MySQL client not found - will use H2 in-memory database"
fi

# Build the application
echo "🔨 Building application..."
if mvn clean compile -q; then
    echo "✅ Application built successfully"
else
    echo "❌ Build failed - please check for compilation errors"
    exit 1
fi

# Start the backend
echo "🚀 Starting Pet Clinic Backend..."
echo "   Backend will be available at: http://localhost:9090"
echo "   Swagger UI will be available at: http://localhost:9090/swagger-ui.html"
echo "   H2 Console (if using H2): http://localhost:9090/h2-console"
echo ""
echo "📋 Test Data Summary:"
echo "   - 5 test owners with contact information"
echo "   - 8 test pets (dogs and cats)"
echo "   - 5 visits (completed, in-progress, scheduled)"
echo "   - 3 upcoming appointments"
echo "   - Sample vaccinations and medical records"
echo ""
echo "🔑 Default Admin Credentials:"
echo "   Username: admin"
echo "   Password: admin123"
echo ""
echo "📖 API Documentation: http://localhost:9090/swagger-ui.html"
echo "🔧 Data Seeding API: http://localhost:9090/api/admin/data-seeding/statistics"
echo ""
echo "Press Ctrl+C to stop the application"
echo "================================"

# Start the backend application
cd pet-clinic-backend
mvn spring-boot:run