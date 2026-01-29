#!/bin/bash
echo "=== Container Status ==="
for container in petclinic-mysql jenkins-local; do
    if docker ps -a --format "table {{.Names}}\t{{.Status}}" | grep -q "$container"; then
        status=$(docker inspect -f '{{.State.Status}}' "$container")
        echo "$container: $status"
        if [ "$status" = "running" ]; then
            case $container in
                "petclinic-mysql")
                    echo "  MySQL: http://localhost:3306"
                    echo "  Database: petclinic_dev"
                    echo "  User: petclinic / petclinic123"
                    ;;
                "jenkins-local")
                    echo "  Jenkins: http://localhost:8082"
                    if docker exec jenkins-local test -f /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null; then
                        password=$(docker exec jenkins-local cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null)
                        echo "  Admin Password: $password"
                    fi
                    ;;
            esac
        fi
    else
        echo "$container: not found"
    fi
done

echo ""
echo "=== Quick Actions ==="
echo "Start all containers: docker start petclinic-mysql jenkins-local"
echo "Stop all containers:  docker stop jenkins-local petclinic-mysql"
echo "View logs:           docker logs <container-name>"