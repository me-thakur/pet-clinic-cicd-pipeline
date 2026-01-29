#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Get the project root directory (parent of scripts directory)
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to project root directory
cd "$PROJECT_ROOT"

echo "Stopping Pet Clinic services from: $(pwd)"

# Stop Spring Boot applications
echo "Stopping Spring Boot applications..."
pkill -f "spring-boot:run"
pkill -f "pet-clinic"

# Stop Docker containers
echo "Stopping Docker containers..."
docker-compose -f pet-clinic-app/pet-clinic-backend/docker-compose.dev.yml down

echo "All services stopped."
echo "Note: To remove data volumes, run:"
echo "docker-compose -f pet-clinic-app/pet-clinic-backend/docker-compose.dev.yml down -v"