# Pet Clinic Application Integration Guide

## Overview

This document describes how all components of the Pet Clinic Management System are wired together to create a cohesive, fully-functional application. The system consists of a Spring Boot backend API and a Thymeleaf-based frontend web application.

## Architecture Overview

```
┌─────────────────┐    HTTP/REST    ┌─────────────────┐
│   Frontend      │ ──────────────► │   Backend       │
│   (Port 8080)   │                 │   (Port 9090)   │
│                 │                 │                 │
│ - Thymeleaf     │                 │ - REST API      │
│ - Spring MVC    │                 │ - Spring Data   │
│ - WebClient     │                 │ - H2/MySQL      │
│ - Bootstrap UI  │                 │ - JWT Auth      │
└─────────────────┘                 └─────────────────┘
```

## Component Integration

### 1. Frontend-Backend Communication

#### WebClient Configuration
The frontend uses Spring WebFlux WebClient to communicate with the backend:

**File:** `pet-clinic-frontend/src/main/java/com/petclinic/frontend/config/WebClientConfig.java`

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
            .baseUrl(backendUrl + apiPath)  // http://localhost:9090/api
            .build();
}
```

**Configuration:** `pet-clinic-frontend/src/main/resources/application.yml`

```yaml
pet-clinic:
  backend:
    url: http://localhost:9090
    api-path: /api
```

#### Service Layer Integration
Frontend services use WebClient to make HTTP requests to backend endpoints:

**Example:** `VeterinarianService.java`
```java
public Mono<Veterinarian> getVeterinarianById(Long id) {
    return webClient.get()
            .uri("/veterinarians/{id}", id)
            .retrieve()
            .bodyToMono(Veterinarian.class);
}
```

### 2. Authentication and Security

#### Backend Security (JWT-based)
- **File:** `pet-clinic-backend/src/main/java/com/petclinic/backend/config/SecurityConfig.java`
- **Features:**
  - JWT token authentication for API endpoints
  - Role-based access control (ADMIN, VET, STAFF)
  - CORS configuration for frontend access
  - Method-level security annotations

#### Frontend Security (Session-based)
- **File:** `pet-clinic-frontend/src/main/java/com/petclinic/frontend/config/SecurityConfig.java`
- **Features:**
  - Form-based authentication
  - In-memory user store for demo purposes
  - Session management
  - Role-based access control

#### Authentication Flow
1. User logs into frontend with username/password
2. Frontend authenticates user against its own user store
3. Frontend makes API calls to backend (backend has separate authentication)
4. Backend validates JWT tokens for API access

### 3. Error Handling Integration

#### Backend Error Handling
- **File:** `pet-clinic-backend/src/main/java/com/petclinic/backend/exception/GlobalExceptionHandler.java`
- **Features:**
  - Standardized error response format
  - HTTP status code mapping
  - Detailed error logging
  - API versioning support

#### Frontend Error Handling
- **File:** `pet-clinic-frontend/src/main/java/com/petclinic/frontend/exception/FrontendExceptionHandler.java`
- **Features:**
  - WebClient exception handling
  - User-friendly error pages
  - Backend service unavailable handling
  - Automatic retry mechanisms

### 4. Data Flow Integration

#### Entity Mapping
Frontend and backend have corresponding model classes:

**Backend Entity:** `Pet.java` (JPA Entity)
```java
@Entity
@Table(name = "pets")
public class Pet {
    @Id @GeneratedValue
    private Long id;
    // ... JPA annotations and relationships
}
```

**Frontend Model:** `Pet.java` (POJO)
```java
public class Pet {
    private Long id;
    // ... validation annotations for forms
}
```

#### Service Integration Pattern
```
Frontend Controller → Frontend Service → WebClient → Backend Controller → Backend Service → Repository → Database
```

### 5. Configuration Integration

#### Port Configuration
- **Backend:** Port 9090 (`application.yml`)
- **Frontend:** Port 8080 (`application.yml`)
- **Database:** H2 in-memory (dev), MySQL (prod)

#### CORS Configuration
Backend allows requests from frontend origin:
```yaml
pet-clinic:
  cors:
    allowed-origins: http://localhost:8080
```

#### Profile-based Configuration
Both applications support multiple profiles:
- `dev`: Development with H2 database
- `staging`: Staging with MySQL
- `prod`: Production with MySQL and SSL

### 6. API Integration

#### REST Endpoints
Backend exposes RESTful APIs that frontend consumes:

| Entity | Backend Endpoint | Frontend Service Method |
|--------|------------------|------------------------|
| Pets | `GET /api/pets` | `petService.getAllPets()` |
| Veterinarians | `GET /api/veterinarians` | `veterinarianService.getAllVeterinarians()` |
| Visits | `POST /api/visits` | `visitService.scheduleVisit()` |
| Reports | `GET /api/reports/visits` | `reportService.getVisitReport()` |

#### Pagination Integration
Backend returns paginated responses that frontend handles:
```java
// Backend
@GetMapping
public Page<Pet> getAllPets(Pageable pageable) { ... }

// Frontend
public Mono<Page<Pet>> getAllPets(Pageable pageable) {
    return webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/pets")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(this::convertToPetPage);
}
```

### 7. Testing Integration

#### Integration Tests
- **Backend:** `SystemIntegrationTest.java` - Tests complete API workflows
- **Frontend:** `FrontendBackendIntegrationTest.java` - Tests service communication

#### Validation Script
- **File:** `validate-integration.sh`
- **Purpose:** Validates configuration and runtime integration
- **Usage:** `./validate-integration.sh`

## Startup and Deployment

### Local Development
1. **Start Backend:**
   ```bash
   cd pet-clinic-app/pet-clinic-backend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **Start Frontend:**
   ```bash
   cd pet-clinic-app/pet-clinic-frontend
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Or use the startup script:**
   ```bash
   ./scripts/start-local.sh
   ```

### Access Points
- **Frontend Application:** http://localhost:8080
- **Backend API:** http://localhost:9090/api
- **API Documentation:** http://localhost:9090/swagger-ui.html
- **H2 Console:** http://localhost:9090/h2-console

### Demo Credentials
- **Admin:** username: `admin`, password: `admin123`
- **Veterinarian:** username: `vet`, password: `vet123`
- **Staff:** username: `staff`, password: `staff123`

## Key Integration Points

### 1. Configuration Consistency
- Backend port (9090) matches frontend backend URL configuration
- CORS origins match frontend URL
- API paths are consistent between frontend and backend

### 2. Error Handling Chain
- Backend returns standardized error responses
- Frontend handles WebClient exceptions
- User-friendly error pages with retry mechanisms
- Automatic service unavailable detection

### 3. Security Integration
- Frontend handles user authentication and session management
- Backend handles API authentication with JWT tokens
- Role-based access control on both layers
- CORS properly configured for cross-origin requests

### 4. Data Consistency
- Model classes are compatible between frontend and backend
- Validation rules are consistent
- Date/time handling is standardized
- Pagination parameters match

### 5. Monitoring and Health Checks
- Both applications expose actuator endpoints
- Health checks validate service availability
- Metrics collection for monitoring
- Logging integration for troubleshooting

## Troubleshooting

### Common Issues

1. **Frontend can't connect to backend**
   - Check backend is running on port 9090
   - Verify CORS configuration
   - Check firewall settings

2. **Authentication issues**
   - Verify user credentials
   - Check JWT token configuration
   - Validate session timeout settings

3. **API errors**
   - Check backend logs for detailed error messages
   - Verify request format and parameters
   - Check API documentation for correct usage

### Validation Commands
```bash
# Check configuration
./pet-clinic-app/validate-integration.sh --skip-runtime-checks

# Check running services
curl http://localhost:9090/actuator/health
curl http://localhost:8080/actuator/health

# Test API connectivity
curl -X OPTIONS -H "Origin: http://localhost:8080" http://localhost:9090/api/pets
```

## Conclusion

The Pet Clinic application demonstrates a well-integrated multi-tier architecture with:
- Clean separation of concerns between frontend and backend
- Robust error handling and user experience
- Comprehensive security implementation
- Scalable and maintainable code structure
- Thorough testing and validation

All components are properly wired together to provide a seamless user experience while maintaining system reliability and security.