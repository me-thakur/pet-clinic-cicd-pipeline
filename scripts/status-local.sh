#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Get the project root directory (parent of scripts directory)
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to project root directory
cd "$PROJECT_ROOT"

echo "=== Pet Clinic Local Development Status ==="
echo "Working directory: $(pwd)"
echo

# Check Docker containers
echo "Docker Containers:"
echo "=================="
for container in petclinic-mysql jenkins-local; do
    if docker ps -a --format "table {{.Names}}\t{{.Status}}" | grep -q "$container"; then
        status=$(docker inspect -f '{{.State.Status}}' "$container")
        echo "$container: $status"
        if [ "$status" = "running" ]; then
            case $container in
                "petclinic-mysql")
                    echo "  MySQL: http://localhost:3306"
                    # Test MySQL connection
                    if docker exec petclinic-mysql mysqladmin ping -h"localhost" --silent 2>/dev/null; then
                        echo "  ✅ MySQL connection: OK"
                    else
                        echo "  ❌ MySQL connection: FAILED"
                    fi
                    ;;
                "jenkins-local")
                    echo "  Jenkins: http://localhost:8082"
                    # Test Jenkins connection
                    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8082 | grep -q "200\|403"; then
                        echo "  ✅ Jenkins web interface: OK"
                    else
                        echo "  ❌ Jenkins web interface: FAILED"
                    fi
                    ;;
            esac
        fi
    else
        echo "$container: not found"
    fi
done

echo
echo "Spring Boot Applications:"
echo "========================"

# Check backend service
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/actuator/health | grep -q "200"; then
    echo "Backend (port 8081): ✅ RUNNING"
    echo "  API: http://localhost:8081/api"
    echo "  Health: http://localhost:8081/api/actuator/health"
else
    echo "Backend (port 8081): ❌ NOT RUNNING"
fi

# Check frontend service
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|302"; then
    echo "Frontend (port 8080): ✅ RUNNING"
    echo "  Application: http://localhost:8080"
else
    echo "Frontend (port 8080): ❌ NOT RUNNING"
fi

echo
echo "Process Information:"
echo "==================="
# Check for Spring Boot processes
SPRING_PROCESSES=$(pgrep -f "spring-boot:run" | wc -l)
if [ "$SPRING_PROCESSES" -gt 0 ]; then
    echo "Spring Boot processes running: $SPRING_PROCESSES"
    pgrep -f "spring-boot:run" | while read pid; do
        echo "  PID: $pid"
    done
else
    echo "No Spring Boot processes found"
fi

echo
echo "Log Files:"
echo "=========="
if [ -d "$PROJECT_ROOT/logs" ]; then
    echo "Log directory: $PROJECT_ROOT/logs/"
    for log in "$PROJECT_ROOT/logs"/*.log; do
        if [ -f "$log" ]; then
            size=$(du -h "$log" | cut -f1)
            echo "  $(basename "$log"): $size"
        fi
    done
else
    echo "No logs directory found"
fi

echo
echo "Quick Actions:"
echo "=============="
echo "Start services: ./scripts/start-local.sh"
echo "Stop services:  ./scripts/stop-local.sh"
echo "View logs:      tail -f logs/backend.log logs/frontend.log"