#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Get the project root directory (parent of scripts directory)
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to project root directory
cd "$PROJECT_ROOT"

echo "Starting Pet Clinic services from: $(pwd)"

# Start database if using Docker
docker-compose -f pet-clinic-app/pet-clinic-backend/docker-compose.dev.yml up -d mysql

# Wait for database to be ready
echo "Waiting for database to be ready..."
until docker exec petclinic-mysql mysqladmin ping -h"localhost" --silent; do
    sleep 1
done

# Create logs directory if it doesn't exist
mkdir -p logs

# Start backend in background
echo "Starting backend service..."
cd "$PROJECT_ROOT/pet-clinic-app/pet-clinic-backend"
mvn spring-boot:run -Dspring-boot.run.profiles=dev > "$PROJECT_ROOT/logs/backend.log" 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend to start..."
until curl -f http://localhost:8081/api/actuator/health > /dev/null 2>&1; do
    sleep 2
done

# Start frontend
echo "Starting frontend service..."
cd "$PROJECT_ROOT/pet-clinic-app/pet-clinic-frontend"
mvn spring-boot:run -Dspring-boot.run.profiles=dev > "$PROJECT_ROOT/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!

echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "Backend: http://localhost:8081/api"
echo "Frontend: http://localhost:8080"
echo "Logs: logs/backend.log, logs/frontend.log"

# Wait for frontend to start
echo "Waiting for frontend to start..."
until curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|302"; do
    sleep 2
done

echo "All services started successfully!"
echo "Pet Clinic Application: http://localhost:8080"
echo "Backend API: http://localhost:8081/api"