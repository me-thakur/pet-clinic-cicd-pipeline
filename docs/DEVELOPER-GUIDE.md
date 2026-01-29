# Pet Clinic Management System - Developer Guide

## Overview

This guide provides comprehensive information for developers working on the Pet Clinic Management System. It covers development setup, coding standards, architecture patterns, testing strategies, and contribution guidelines.

## Table of Contents

1. [Development Environment Setup](#development-environment-setup)
2. [Project Structure](#project-structure)
3. [Architecture Overview](#architecture-overview)
4. [Development Workflow](#development-workflow)
5. [Coding Standards](#coding-standards)
6. [Testing Guidelines](#testing-guidelines)
7. [API Development](#api-development)
8. [Database Development](#database-development)
9. [Frontend Development](#frontend-development)
10. [Security Guidelines](#security-guidelines)
11. [Performance Optimization](#performance-optimization)
12. [Debugging and Troubleshooting](#debugging-and-troubleshooting)
13. [Contributing Guidelines](#contributing-guidelines)

## Development Environment Setup

### Prerequisites

- **Java**: OpenJDK 11 or higher
- **Maven**: 3.6.0 or higher
- **MySQL**: 8.0 or higher (or H2 for testing)
- **Git**: Latest version
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Quick Setup

```bash
# Clone the repository
git clone https://github.com/your-org/pet-clinic-management-system.git
cd pet-clinic-management-system

# Set up development database (optional - H2 is used by default)
mysql -u root -p -e "CREATE DATABASE petclinic_dev;"
mysql -u root -p -e "CREATE USER 'petclinic'@'localhost' IDENTIFIED BY 'password';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON petclinic_dev.* TO 'petclinic'@'localhost';"

# Build the project
cd pet-clinic-app
mvn clean install

# Run the backend
mvn spring-boot:run -pl pet-clinic-backend -Dspring.profiles.active=dev

# Run the frontend (in another terminal)
mvn spring-boot:run -pl pet-clinic-frontend -Dspring.profiles.active=dev
```

### IDE Configuration

#### IntelliJ IDEA Setup

1. **Import Project**
   - File → Open → Select `pet-clinic-app/pom.xml`
   - Import as Maven project

2. **Configure Code Style**
   - File → Settings → Editor → Code Style → Java
   - Import scheme from `docs/intellij-code-style.xml`

3. **Enable Annotation Processing**
   - File → Settings → Build → Compiler → Annotation Processors
   - Check "Enable annotation processing"

4. **Configure Run Configurations**
   - Run → Edit Configurations
   - Add Spring Boot configurations for backend and frontend

#### VS Code Setup

1. **Install Extensions**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - MySQL extension

2. **Configure Settings**
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.compile.nullAnalysis.mode": "automatic",
     "spring-boot.ls.problem.application-properties.enabled": true
   }
   ```

## Project Structure

```
pet-clinic-app/
├── pet-clinic-backend/          # Spring Boot backend application
│   ├── src/main/java/
│   │   └── com/petclinic/backend/
│   │       ├── PetClinicBackendApplication.java
│   │       ├── config/          # Configuration classes
│   │       ├── controller/      # REST API controllers
│   │       ├── dto/            # Data Transfer Objects
│   │       ├── exception/      # Exception handling
│   │       ├── model/          # JPA entities
│   │       ├── repository/     # Data access layer
│   │       └── service/        # Business logic layer
│   ├── src/main/resources/
│   │   ├── db/migration/       # Flyway database migrations
│   │   ├── application.yml     # Main configuration
│   │   └── application-*.yml   # Environment-specific configs
│   └── src/test/java/          # Test classes
├── pet-clinic-frontend/         # Spring Boot frontend application
│   ├── src/main/java/
│   │   └── com/petclinic/frontend/
│   ├── src/main/resources/
│   │   ├── static/             # CSS, JS, images
│   │   └── templates/          # Thymeleaf templates
│   └── src/test/java/
└── pom.xml                     # Parent Maven POM
```
## Architecture Overview

### Layered Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────┐
│           Presentation Layer        │
│  (Controllers, DTOs, Exception      │
│   Handlers, Web Configuration)      │
├─────────────────────────────────────┤
│            Service Layer            │
│   (Business Logic, Validation,      │
│    Transaction Management)          │
├─────────────────────────────────────┤
│          Repository Layer           │
│    (Data Access, JPA Repositories,  │
│     Custom Queries)                 │
├─────────────────────────────────────┤
│            Domain Layer             │
│     (Entities, Value Objects,       │
│      Domain Services)               │
└─────────────────────────────────────┘
```

### Key Design Patterns

1. **Repository Pattern**: Data access abstraction
2. **Service Layer Pattern**: Business logic encapsulation
3. **DTO Pattern**: Data transfer between layers
4. **Builder Pattern**: Complex object construction
5. **Strategy Pattern**: Algorithm selection (e.g., search strategies)
6. **Observer Pattern**: Event handling and notifications

### Core Components

#### Entities (Domain Layer)
- `Owner`: Pet owner information
- `Pet`: Pet details and medical history
- `Visit`: Veterinary appointments and treatments
- `Veterinarian`: Veterinary professional profiles
- `User`: System user accounts
- `AuditLog`: System activity tracking

#### Services (Business Layer)
- `PetService`: Pet management operations
- `VisitService`: Appointment scheduling and management
- `VeterinarianService`: Veterinarian profile management
- `SearchService`: Global search functionality
- `ReportService`: Analytics and reporting
- `AuditService`: Activity logging

#### Controllers (Presentation Layer)
- `PetController`: Pet management endpoints
- `VisitController`: Visit scheduling endpoints
- `VeterinarianController`: Veterinarian management endpoints
- `SearchController`: Search and filtering endpoints
- `ReportController`: Reporting endpoints
- `AuthController`: Authentication endpoints

## Development Workflow

### Git Workflow

We follow the **Git Flow** branching model:

```
main (production-ready code)
├── develop (integration branch)
│   ├── feature/pet-management
│   ├── feature/visit-scheduling
│   └── feature/reporting
├── release/v2.0.0
└── hotfix/critical-bug-fix
```

### Branch Naming Conventions

- **Feature branches**: `feature/short-description`
- **Bug fix branches**: `bugfix/issue-number-description`
- **Hotfix branches**: `hotfix/critical-issue-description`
- **Release branches**: `release/version-number`

### Commit Message Format

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(pets): add pet search functionality

Implement global search across pet records with filtering
by species, breed, and owner information.

Closes #123
```

### Pull Request Process

1. **Create Feature Branch**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/your-feature-name
   ```

2. **Develop and Test**
   ```bash
   # Make changes
   git add .
   git commit -m "feat(scope): description"
   
   # Run tests
   mvn test
   ```

3. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   # Create pull request on GitHub
   ```

4. **Code Review Process**
   - Automated tests must pass
   - At least one code review approval
   - No merge conflicts
   - Documentation updated if needed

## Coding Standards

### Java Coding Standards

#### Naming Conventions

```java
// Classes: PascalCase
public class PetService {
    
    // Constants: UPPER_SNAKE_CASE
    private static final String DEFAULT_SPECIES = "UNKNOWN";
    
    // Variables and methods: camelCase
    private PetRepository petRepository;
    
    public Pet findPetById(Long petId) {
        return petRepository.findById(petId)
            .orElseThrow(() -> new EntityNotFoundException("Pet not found"));
    }
}
```

#### Code Organization

```java
@Service
@Transactional
@Slf4j
public class PetServiceImpl implements PetService {
    
    // 1. Static fields
    private static final int MAX_PETS_PER_OWNER = 10;
    
    // 2. Instance fields
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final AuditService auditService;
    
    // 3. Constructor
    public PetServiceImpl(PetRepository petRepository,
                         OwnerRepository ownerRepository,
                         AuditService auditService) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.auditService = auditService;
    }
    
    // 4. Public methods
    @Override
    public Pet createPet(CreatePetRequest request) {
        validatePetCreation(request);
        
        Pet pet = Pet.builder()
            .name(request.getName())
            .species(request.getSpecies())
            .breed(request.getBreed())
            .birthDate(request.getBirthDate())
            .owner(findOwnerById(request.getOwnerId()))
            .build();
            
        Pet savedPet = petRepository.save(pet);
        auditService.logPetCreation(savedPet);
        
        return savedPet;
    }
    
    // 5. Private methods
    private void validatePetCreation(CreatePetRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Pet name is required");
        }
        
        if (request.getBirthDate() != null && request.getBirthDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Birth date cannot be in the future");
        }
    }
    
    private Owner findOwnerById(Long ownerId) {
        return ownerRepository.findById(ownerId)
            .orElseThrow(() -> new EntityNotFoundException("Owner not found: " + ownerId));
    }
}
```

#### Exception Handling

```java
// Custom exceptions
public class PetClinicException extends RuntimeException {
    public PetClinicException(String message) {
        super(message);
    }
    
    public PetClinicException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Global exception handler
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .build();
    }
    
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message(ex.getMessage())
            .build();
    }
}
```

### Documentation Standards

#### JavaDoc Comments

```java
/**
 * Service for managing pet records and related operations.
 * 
 * <p>This service provides comprehensive pet management functionality including
 * creation, updates, search, and deletion of pet records. It ensures data
 * integrity and business rule validation.</p>
 * 
 * @author Pet Clinic Development Team
 * @version 2.0
 * @since 1.0
 */
@Service
public class PetServiceImpl implements PetService {
    
    /**
     * Creates a new pet record with the provided information.
     * 
     * <p>This method validates the pet information, checks business rules
     * (such as maximum pets per owner), and creates the pet record in the
     * database. An audit log entry is created for tracking purposes.</p>
     * 
     * @param request the pet creation request containing pet details
     * @return the created pet with generated ID and timestamps
     * @throws ValidationException if the request data is invalid
     * @throws EntityNotFoundException if the specified owner doesn't exist
     * @throws BusinessRuleException if business rules are violated
     */
    @Override
    public Pet createPet(CreatePetRequest request) {
        // Implementation
    }
}
```

## Testing Guidelines

### Testing Strategy

We follow a comprehensive testing approach:

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Property-Based Tests**: Test universal properties with generated data
4. **End-to-End Tests**: Test complete user workflows

### Unit Testing

#### Service Layer Tests

```java
@ExtendWith(MockitoExtension.class)
class PetServiceImplTest {
    
    @Mock
    private PetRepository petRepository;
    
    @Mock
    private OwnerRepository ownerRepository;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private PetServiceImpl petService;
    
    @Test
    @DisplayName("Should create pet successfully with valid data")
    void shouldCreatePetSuccessfully() {
        // Given
        CreatePetRequest request = CreatePetRequest.builder()
            .name("Buddy")
            .species("Dog")
            .breed("Golden Retriever")
            .birthDate(LocalDate.of(2020, 5, 15))
            .ownerId(1L)
            .build();
            
        Owner owner = Owner.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .build();
            
        Pet expectedPet = Pet.builder()
            .id(1L)
            .name("Buddy")
            .species("Dog")
            .breed("Golden Retriever")
            .birthDate(LocalDate.of(2020, 5, 15))
            .owner(owner)
            .build();
        
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(petRepository.save(any(Pet.class))).thenReturn(expectedPet);
        
        // When
        Pet result = petService.createPet(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Buddy");
        assertThat(result.getSpecies()).isEqualTo("Dog");
        assertThat(result.getOwner()).isEqualTo(owner);
        
        verify(petRepository).save(any(Pet.class));
        verify(auditService).logPetCreation(result);
    }
    
    @Test
    @DisplayName("Should throw exception when owner not found")
    void shouldThrowExceptionWhenOwnerNotFound() {
        // Given
        CreatePetRequest request = CreatePetRequest.builder()
            .name("Buddy")
            .ownerId(999L)
            .build();
            
        when(ownerRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> petService.createPet(request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Owner not found: 999");
            
        verify(petRepository, never()).save(any(Pet.class));
    }
}
```

#### Controller Tests

```java
@WebMvcTest(PetController.class)
class PetControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PetService petService;
    
    @Test
    @DisplayName("Should return pet when valid ID provided")
    void shouldReturnPetWhenValidIdProvided() throws Exception {
        // Given
        Pet pet = Pet.builder()
            .id(1L)
            .name("Buddy")
            .species("Dog")
            .build();
            
        when(petService.findById(1L)).thenReturn(pet);
        
        // When & Then
        mockMvc.perform(get("/api/pets/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Buddy"))
                .andExpect(jsonPath("$.species").value("Dog"));
    }
    
    @Test
    @DisplayName("Should return 404 when pet not found")
    void shouldReturn404WhenPetNotFound() throws Exception {
        // Given
        when(petService.findById(999L))
            .thenThrow(new EntityNotFoundException("Pet not found"));
        
        // When & Then
        mockMvc.perform(get("/api/pets/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
```

### Property-Based Testing

```java
@PropertyTest
class PetEntityPropertiesTest {
    
    @Property
    @DisplayName("Pet creation should always result in valid entity")
    void petCreationShouldAlwaysResultInValidEntity(
            @ForAll @StringLength(min = 1, max = 50) String name,
            @ForAll @StringLength(min = 1, max = 30) String species,
            @ForAll @StringLength(max = 50) String breed) {
        
        // Given
        Owner owner = Owner.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Owner")
            .build();
        
        // When
        Pet pet = Pet.builder()
            .name(name)
            .species(species)
            .breed(breed)
            .owner(owner)
            .build();
        
        // Then
        assertThat(pet.getName()).isEqualTo(name);
        assertThat(pet.getSpecies()).isEqualTo(species);
        assertThat(pet.getBreed()).isEqualTo(breed);
        assertThat(pet.getOwner()).isEqualTo(owner);
        assertThat(pet.getCreatedAt()).isNull(); // Not set until persistence
    }
    
    @Property
    @DisplayName("Pet search should return consistent results")
    void petSearchShouldReturnConsistentResults(
            @ForAll @StringLength(min = 1, max = 20) String searchTerm) {
        
        // This property test would verify that search results are
        // consistent and follow expected patterns
        assume(!searchTerm.trim().isEmpty());
        
        // Implementation would test actual search functionality
        // with various search terms
    }
}
```

### Integration Testing

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class PetServiceIntegrationTest {
    
    @Autowired
    private PetService petService;
    
    @Autowired
    private OwnerRepository ownerRepository;
    
    @Autowired
    private PetRepository petRepository;
    
    @Test
    @DisplayName("Should create pet and persist to database")
    void shouldCreatePetAndPersistToDatabase() {
        // Given
        Owner owner = Owner.builder()
            .firstName("John")
            .lastName("Doe")
            .address("123 Main St")
            .city("Springfield")
            .telephone("555-1234")
            .build();
        Owner savedOwner = ownerRepository.save(owner);
        
        CreatePetRequest request = CreatePetRequest.builder()
            .name("Buddy")
            .species("Dog")
            .breed("Golden Retriever")
            .birthDate(LocalDate.of(2020, 5, 15))
            .ownerId(savedOwner.getId())
            .build();
        
        // When
        Pet createdPet = petService.createPet(request);
        
        // Then
        assertThat(createdPet.getId()).isNotNull();
        
        Optional<Pet> retrievedPet = petRepository.findById(createdPet.getId());
        assertThat(retrievedPet).isPresent();
        assertThat(retrievedPet.get().getName()).isEqualTo("Buddy");
        assertThat(retrievedPet.get().getOwner().getId()).isEqualTo(savedOwner.getId());
    }
}
```

### Test Configuration

#### Test Properties

Create `src/test/resources/application-test.properties`:

```properties
# H2 in-memory database for testing
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA configuration for testing
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Disable Flyway for tests
spring.flyway.enabled=false

# Logging configuration
logging.level.com.petclinic=DEBUG
logging.level.org.springframework.web=DEBUG
```

#### Test Data Builders

```java
public class TestDataBuilder {
    
    public static Owner.OwnerBuilder defaultOwner() {
        return Owner.builder()
            .firstName("John")
            .lastName("Doe")
            .address("123 Main St")
            .city("Springfield")
            .telephone("555-1234");
    }
    
    public static Pet.PetBuilder defaultPet() {
        return Pet.builder()
            .name("Buddy")
            .species("Dog")
            .breed("Golden Retriever")
            .birthDate(LocalDate.of(2020, 5, 15));
    }
    
    public static Visit.VisitBuilder defaultVisit() {
        return Visit.builder()
            .visitDate(LocalDateTime.now().plusDays(1))
            .visitType(VisitType.CHECKUP)
            .diagnosis("Healthy")
            .treatment("Routine examination");
    }
}
```

## API Development

### REST API Design Principles

1. **RESTful URLs**: Use nouns, not verbs
2. **HTTP Methods**: Use appropriate HTTP methods
3. **Status Codes**: Return meaningful HTTP status codes
4. **Consistent Response Format**: Standardize response structure
5. **Versioning**: Support API versioning
6. **Documentation**: Comprehensive API documentation

### Controller Implementation

```java
@RestController
@RequestMapping("/api/v1/pets")
@Validated
@Slf4j
public class PetController {
    
    private final PetService petService;
    
    public PetController(PetService petService) {
        this.petService = petService;
    }
    
    @GetMapping
    @Operation(summary = "Get all pets", description = "Retrieve a paginated list of pets")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pets"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PagedResponse<PetResponse>> getAllPets(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(required = false) String search) {
        
        log.info("Getting pets - page: {}, size: {}, sort: {}, search: {}", 
                page, size, sort, search);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Pet> pets = petService.findAll(pageable, search);
        
        PagedResponse<PetResponse> response = PagedResponse.<PetResponse>builder()
            .content(pets.getContent().stream()
                .map(PetResponse::from)
                .collect(Collectors.toList()))
            .page(pets.getNumber())
            .size(pets.getSize())
            .totalElements(pets.getTotalElements())
            .totalPages(pets.getTotalPages())
            .first(pets.isFirst())
            .last(pets.isLast())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "Create a new pet", description = "Create a new pet record")
    public ResponseEntity<PetResponse> createPet(
            @Valid @RequestBody CreatePetRequest request) {
        
        log.info("Creating pet: {}", request.getName());
        
        Pet createdPet = petService.createPet(request);
        PetResponse response = PetResponse.from(createdPet);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/pets/" + createdPet.getId()))
            .body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get pet by ID", description = "Retrieve a specific pet by its ID")
    public ResponseEntity<PetResponse> getPetById(
            @PathVariable @Positive Long id) {
        
        log.info("Getting pet by ID: {}", id);
        
        Pet pet = petService.findById(id);
        PetResponse response = PetResponse.from(pet);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update pet", description = "Update an existing pet record")
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdatePetRequest request) {
        
        log.info("Updating pet ID: {}", id);
        
        Pet updatedPet = petService.updatePet(id, request);
        PetResponse response = PetResponse.from(updatedPet);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pet", description = "Delete a pet record")
    public ResponseEntity<Void> deletePet(@PathVariable @Positive Long id) {
        log.info("Deleting pet ID: {}", id);
        
        petService.deleteById(id);
        
        return ResponseEntity.noContent().build();
    }
}
```

### DTO Design

```java
// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePetRequest {
    
    @NotBlank(message = "Pet name is required")
    @Size(max = 50, message = "Pet name must not exceed 50 characters")
    private String name;
    
    @NotBlank(message = "Species is required")
    @Size(max = 30, message = "Species must not exceed 30 characters")
    private String species;
    
    @Size(max = 50, message = "Breed must not exceed 50 characters")
    private String breed;
    
    @PastOrPresent(message = "Birth date cannot be in the future")
    private LocalDate birthDate;
    
    @NotNull(message = "Owner ID is required")
    @Positive(message = "Owner ID must be positive")
    private Long ownerId;
}

// Response DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponse {
    
    private Long id;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthDate;
    private OwnerSummary owner;
    private List<VisitSummary> recentVisits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static PetResponse from(Pet pet) {
        return PetResponse.builder()
            .id(pet.getId())
            .name(pet.getName())
            .species(pet.getSpecies())
            .breed(pet.getBreed())
            .birthDate(pet.getBirthDate())
            .owner(OwnerSummary.from(pet.getOwner()))
            .recentVisits(pet.getVisits().stream()
                .sorted(Comparator.comparing(Visit::getVisitDate).reversed())
                .limit(5)
                .map(VisitSummary::from)
                .collect(Collectors.toList()))
            .createdAt(pet.getCreatedAt())
            .updatedAt(pet.getUpdatedAt())
            .build();
    }
}
```

### OpenAPI Documentation

```java
@OpenAPIDefinition(
    info = @Info(
        title = "Pet Clinic Management API",
        version = "2.0",
        description = "Comprehensive API for managing veterinary clinic operations",
        contact = @Contact(
            name = "Pet Clinic Development Team",
            email = "dev@petclinic.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:9090", description = "Development server"),
        @Server(url = "https://api.petclinic.com", description = "Production server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class OpenApiConfig {
    
    @Bean
    public GroupedOpenApi petManagementApi() {
        return GroupedOpenApi.builder()
            .group("pet-management")
            .displayName("Pet Management APIs")
            .pathsToMatch("/api/v1/pets/**")
            .build();
    }
    
    @Bean
    public GroupedOpenApi visitManagementApi() {
        return GroupedOpenApi.builder()
            .group("visit-management")
            .displayName("Visit Management APIs")
            .pathsToMatch("/api/v1/visits/**")
            .build();
    }
}
```

This completes the first part of the Developer Guide. The file is getting quite long, so I'll continue with the remaining sections in the next part.