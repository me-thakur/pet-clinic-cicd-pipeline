# Pet Clinic Management System - Project Flowchart

## Document Information

- **Document Version**: 2.0
- **Last Updated**: January 30, 2026
- **Status**: Current
- **Purpose**: Visual representation of system workflows and processes

## System Architecture Flowchart

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Browser]
        MOB[Mobile Device]
        API_CLIENT[API Client]
    end

    subgraph "Frontend Layer"
        FE[Pet Clinic Frontend<br/>Spring Boot + Thymeleaf]
        STATIC[Static Resources<br/>CSS, JS, Images]
    end

    subgraph "Backend Layer"
        BE[Pet Clinic Backend<br/>Spring Boot REST API]
        AUTH[Authentication Service]
        CACHE[Cache Layer<br/>Caffeine]
    end

    subgraph "Service Layer"
        PET_SVC[Pet Service]
        VISIT_SVC[Visit Service]
        VET_SVC[Veterinarian Service]
        SEARCH_SVC[Search Service]
        REPORT_SVC[Report Service]
        AUDIT_SVC[Audit Service]
    end

    subgraph "Data Layer"
        DB[(MySQL Database)]
        FLYWAY[Flyway Migrations]
        BACKUP[Backup System]
    end

    subgraph "External Systems"
        SMTP[Email Service]
        MONITOR[Monitoring<br/>CloudWatch]
        LOGS[Logging System]
    end

    WEB --> FE
    MOB --> FE
    API_CLIENT --> BE
    FE --> BE
    FE --> STATIC
    
    BE --> AUTH
    BE --> CACHE
    BE --> PET_SVC
    BE --> VISIT_SVC
    BE --> VET_SVC
    BE --> SEARCH_SVC
    BE --> REPORT_SVC
    BE --> AUDIT_SVC
    
    PET_SVC --> DB
    VISIT_SVC --> DB
    VET_SVC --> DB
    SEARCH_SVC --> DB
    REPORT_SVC --> DB
    AUDIT_SVC --> DB
    
    FLYWAY --> DB
    DB --> BACKUP
    
    BE --> SMTP
    BE --> MONITOR
    BE --> LOGS
```

## User Journey Flowcharts

### 1. Pet Registration Workflow

```mermaid
flowchart TD
    START([User Starts Pet Registration])
    LOGIN{User Logged In?}
    AUTH_PAGE[Redirect to Login]
    PET_FORM[Display Pet Registration Form]
    VALIDATE{Form Valid?}
    OWNER_SELECT[Select/Create Owner]
    OWNER_EXISTS{Owner Exists?}
    CREATE_OWNER[Create New Owner]
    SAVE_PET[Save Pet to Database]
    AUDIT_LOG[Log Pet Creation]
    SUCCESS[Display Success Message]
    ERROR[Display Error Message]
    END([End])

    START --> LOGIN
    LOGIN -->|No| AUTH_PAGE
    LOGIN -->|Yes| PET_FORM
    AUTH_PAGE --> PET_FORM
    PET_FORM --> VALIDATE
    VALIDATE -->|No| ERROR
    VALIDATE -->|Yes| OWNER_SELECT
    OWNER_SELECT --> OWNER_EXISTS
    OWNER_EXISTS -->|No| CREATE_OWNER
    OWNER_EXISTS -->|Yes| SAVE_PET
    CREATE_OWNER --> SAVE_PET
    SAVE_PET --> AUDIT_LOG
    AUDIT_LOG --> SUCCESS
    ERROR --> PET_FORM
    SUCCESS --> END
```

### 2. Visit Scheduling Workflow

```mermaid
flowchart TD
    START([User Initiates Visit Scheduling])
    SELECT_PET[Select Pet from List]
    PET_DETAILS[Display Pet Details]
    SCHEDULE_FORM[Show Scheduling Form]
    SELECT_VET[Select Veterinarian]
    CHECK_SPECIALTY{Specialty Match?}
    SELECT_DATE[Choose Date & Time]
    CHECK_CONFLICT{Scheduling Conflict?}
    RESOLVE_CONFLICT[Show Alternative Times]
    CONFIRM_VISIT[Confirm Visit Details]
    SAVE_VISIT[Save Visit to Database]
    SEND_REMINDER[Schedule Email Reminder]
    AUDIT_LOG[Log Visit Creation]
    SUCCESS[Display Confirmation]
    ERROR[Display Error Message]
    END([End])

    START --> SELECT_PET
    SELECT_PET --> PET_DETAILS
    PET_DETAILS --> SCHEDULE_FORM
    SCHEDULE_FORM --> SELECT_VET
    SELECT_VET --> CHECK_SPECIALTY
    CHECK_SPECIALTY -->|No| ERROR
    CHECK_SPECIALTY -->|Yes| SELECT_DATE
    SELECT_DATE --> CHECK_CONFLICT
    CHECK_CONFLICT -->|Yes| RESOLVE_CONFLICT
    CHECK_CONFLICT -->|No| CONFIRM_VISIT
    RESOLVE_CONFLICT --> SELECT_DATE
    CONFIRM_VISIT --> SAVE_VISIT
    SAVE_VISIT --> SEND_REMINDER
    SEND_REMINDER --> AUDIT_LOG
    AUDIT_LOG --> SUCCESS
    ERROR --> SCHEDULE_FORM
    SUCCESS --> END
```

### 3. Search and Filter Workflow

```mermaid
flowchart TD
    START([User Initiates Search])
    SEARCH_INPUT[Enter Search Query]
    SEARCH_TYPE{Search Type?}
    GLOBAL_SEARCH[Global Search Across All Entities]
    FILTERED_SEARCH[Apply Filters]
    EXECUTE_SEARCH[Execute Database Query]
    CACHE_CHECK{Results in Cache?}
    FETCH_CACHE[Retrieve from Cache]
    QUERY_DB[Query Database]
    PROCESS_RESULTS[Process and Highlight Results]
    CACHE_RESULTS[Cache Results]
    DISPLAY_RESULTS[Display Paginated Results]
    EXPORT_OPTION{Export Requested?}
    GENERATE_EXPORT[Generate PDF/CSV Export]
    SAVE_HISTORY[Save to Search History]
    END([End])

    START --> SEARCH_INPUT
    SEARCH_INPUT --> SEARCH_TYPE
    SEARCH_TYPE -->|Global| GLOBAL_SEARCH
    SEARCH_TYPE -->|Filtered| FILTERED_SEARCH
    GLOBAL_SEARCH --> EXECUTE_SEARCH
    FILTERED_SEARCH --> EXECUTE_SEARCH
    EXECUTE_SEARCH --> CACHE_CHECK
    CACHE_CHECK -->|Yes| FETCH_CACHE
    CACHE_CHECK -->|No| QUERY_DB
    FETCH_CACHE --> DISPLAY_RESULTS
    QUERY_DB --> PROCESS_RESULTS
    PROCESS_RESULTS --> CACHE_RESULTS
    CACHE_RESULTS --> DISPLAY_RESULTS
    DISPLAY_RESULTS --> EXPORT_OPTION
    EXPORT_OPTION -->|Yes| GENERATE_EXPORT
    EXPORT_OPTION -->|No| SAVE_HISTORY
    GENERATE_EXPORT --> SAVE_HISTORY
    SAVE_HISTORY --> END
```

## Data Flow Diagrams

### 1. Pet Management Data Flow

```mermaid
flowchart LR
    subgraph "User Interface"
        UI[Pet Management UI]
    end

    subgraph "API Layer"
        CONTROLLER[Pet Controller]
        VALIDATOR[Input Validator]
    end

    subgraph "Business Logic"
        SERVICE[Pet Service]
        BUSINESS_RULES[Business Rules Engine]
    end

    subgraph "Data Access"
        REPOSITORY[Pet Repository]
        CACHE[Cache Manager]
    end

    subgraph "Data Storage"
        DATABASE[(MySQL Database)]
        AUDIT_LOG[(Audit Log)]
    end

    UI -->|Pet Data| CONTROLLER
    CONTROLLER -->|Validate| VALIDATOR
    VALIDATOR -->|Valid Data| SERVICE
    SERVICE -->|Apply Rules| BUSINESS_RULES
    BUSINESS_RULES -->|Processed Data| REPOSITORY
    REPOSITORY -->|Check Cache| CACHE
    CACHE -->|Cache Miss| DATABASE
    DATABASE -->|Pet Record| REPOSITORY
    REPOSITORY -->|Audit Trail| AUDIT_LOG
    REPOSITORY -->|Response| SERVICE
    SERVICE -->|Result| CONTROLLER
    CONTROLLER -->|JSON Response| UI
```

### 2. Authentication and Authorization Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant AuthController
    participant AuthService
    participant UserRepository
    participant Database
    participant AuditService

    User->>Frontend: Login Request
    Frontend->>AuthController: POST /api/auth/login
    AuthController->>AuthService: authenticate(credentials)
    AuthService->>UserRepository: findByUsername()
    UserRepository->>Database: SELECT user query
    Database-->>UserRepository: User record
    UserRepository-->>AuthService: User entity
    AuthService->>AuthService: validatePassword()
    AuthService->>AuthService: generateJWT()
    AuthService->>AuditService: logLoginAttempt()
    AuditService->>Database: INSERT audit record
    AuthService-->>AuthController: JWT token
    AuthController-->>Frontend: Authentication response
    Frontend-->>User: Login success/failure
```

### 3. Visit Scheduling Process Flow

```mermaid
stateDiagram-v2
    [*] --> PetSelection
    PetSelection --> VeterinarianSelection : Pet Selected
    VeterinarianSelection --> DateTimeSelection : Vet Selected
    DateTimeSelection --> ConflictCheck : Date/Time Chosen
    ConflictCheck --> SchedulingError : Conflict Found
    ConflictCheck --> VisitConfirmation : No Conflict
    SchedulingError --> DateTimeSelection : Choose Different Time
    VisitConfirmation --> VisitScheduled : Confirmed
    VisitScheduled --> ReminderScheduled : Visit Saved
    ReminderScheduled --> [*]
    
    state ConflictCheck {
        [*] --> CheckVetAvailability
        CheckVetAvailability --> CheckRoomAvailability
        CheckRoomAvailability --> ValidateBusinessHours
        ValidateBusinessHours --> [*]
    }
```

## System Integration Flowchart

```mermaid
graph TB
    subgraph "External Integrations"
        EMAIL[Email Service<br/>SMTP]
        MONITOR[Monitoring<br/>CloudWatch]
        BACKUP[Backup Service<br/>S3]
    end

    subgraph "Pet Clinic System"
        subgraph "Frontend Services"
            WEB_UI[Web Interface]
            MOBILE_UI[Mobile Interface]
        end

        subgraph "API Gateway"
            API_GW[API Gateway<br/>Rate Limiting & Auth]
        end

        subgraph "Core Services"
            PET_API[Pet Management API]
            VISIT_API[Visit Management API]
            VET_API[Veterinarian API]
            SEARCH_API[Search API]
            REPORT_API[Reporting API]
            AUTH_API[Authentication API]
        end

        subgraph "Data Services"
            CACHE_SVC[Cache Service]
            DB_SVC[Database Service]
            AUDIT_SVC[Audit Service]
        end

        subgraph "Infrastructure"
            DB[(Primary Database)]
            CACHE_STORE[(Cache Store)]
            FILE_STORE[(File Storage)]
        end
    end

    WEB_UI --> API_GW
    MOBILE_UI --> API_GW
    
    API_GW --> PET_API
    API_GW --> VISIT_API
    API_GW --> VET_API
    API_GW --> SEARCH_API
    API_GW --> REPORT_API
    API_GW --> AUTH_API
    
    PET_API --> CACHE_SVC
    VISIT_API --> CACHE_SVC
    VET_API --> CACHE_SVC
    SEARCH_API --> CACHE_SVC
    REPORT_API --> CACHE_SVC
    
    CACHE_SVC --> DB_SVC
    DB_SVC --> DB
    CACHE_SVC --> CACHE_STORE
    
    PET_API --> AUDIT_SVC
    VISIT_API --> AUDIT_SVC
    VET_API --> AUDIT_SVC
    AUTH_API --> AUDIT_SVC
    
    AUDIT_SVC --> DB
    
    REPORT_API --> FILE_STORE
    
    AUTH_API --> EMAIL
    VISIT_API --> EMAIL
    
    DB_SVC --> BACKUP
    CACHE_SVC --> MONITOR
    API_GW --> MONITOR
```

## Security Flow Diagram

```mermaid
flowchart TD
    subgraph "Security Layers"
        subgraph "Network Security"
            HTTPS[HTTPS/TLS Encryption]
            FIREWALL[Firewall Rules]
        end

        subgraph "Application Security"
            JWT[JWT Authentication]
            RBAC[Role-Based Access Control]
            INPUT_VAL[Input Validation]
            CSRF[CSRF Protection]
        end

        subgraph "Data Security"
            ENCRYPT[Data Encryption at Rest]
            AUDIT[Audit Logging]
            BACKUP_SEC[Secure Backups]
        end
    end

    subgraph "User Roles"
        ADMIN[Administrator]
        VET[Veterinarian]
        STAFF[Staff Member]
    end

    subgraph "Protected Resources"
        PET_DATA[Pet Records]
        VISIT_DATA[Visit Records]
        VET_DATA[Veterinarian Data]
        REPORTS[Reports & Analytics]
        SYSTEM_CONFIG[System Configuration]
    end

    ADMIN --> RBAC
    VET --> RBAC
    STAFF --> RBAC
    
    RBAC --> JWT
    JWT --> INPUT_VAL
    INPUT_VAL --> CSRF
    
    HTTPS --> RBAC
    FIREWALL --> HTTPS
    
    RBAC --> PET_DATA
    RBAC --> VISIT_DATA
    RBAC --> VET_DATA
    RBAC --> REPORTS
    RBAC --> SYSTEM_CONFIG
    
    PET_DATA --> ENCRYPT
    VISIT_DATA --> ENCRYPT
    VET_DATA --> ENCRYPT
    
    ENCRYPT --> AUDIT
    AUDIT --> BACKUP_SEC
```

## Performance Optimization Flow

```mermaid
flowchart TD
    REQUEST[Incoming Request]
    CACHE_CHECK{Cache Hit?}
    CACHE_RETURN[Return Cached Data]
    DB_QUERY[Database Query]
    OPTIMIZE{Query Optimized?}
    INDEX_CHECK[Check Indexes]
    QUERY_PLAN[Analyze Query Plan]
    EXECUTE_QUERY[Execute Optimized Query]
    CACHE_STORE[Store in Cache]
    RETURN_RESPONSE[Return Response]
    MONITOR[Performance Monitoring]

    REQUEST --> CACHE_CHECK
    CACHE_CHECK -->|Yes| CACHE_RETURN
    CACHE_CHECK -->|No| DB_QUERY
    DB_QUERY --> OPTIMIZE
    OPTIMIZE -->|No| INDEX_CHECK
    OPTIMIZE -->|Yes| EXECUTE_QUERY
    INDEX_CHECK --> QUERY_PLAN
    QUERY_PLAN --> EXECUTE_QUERY
    EXECUTE_QUERY --> CACHE_STORE
    CACHE_STORE --> RETURN_RESPONSE
    CACHE_RETURN --> RETURN_RESPONSE
    RETURN_RESPONSE --> MONITOR
    MONITOR --> REQUEST
```

## Error Handling and Recovery Flow

```mermaid
flowchart TD
    ERROR[Error Occurs]
    ERROR_TYPE{Error Type?}
    
    VALIDATION_ERROR[Validation Error]
    BUSINESS_ERROR[Business Logic Error]
    SYSTEM_ERROR[System Error]
    DATABASE_ERROR[Database Error]
    
    LOG_ERROR[Log Error Details]
    AUDIT_ERROR[Audit Error Event]
    
    USER_MESSAGE[Generate User-Friendly Message]
    SYSTEM_ALERT[Send System Alert]
    
    RETRY{Retryable?}
    RETRY_LOGIC[Execute Retry Logic]
    FALLBACK[Execute Fallback Logic]
    
    RECOVERY{Recovery Possible?}
    AUTO_RECOVERY[Automatic Recovery]
    MANUAL_INTERVENTION[Manual Intervention Required]
    
    RESPONSE[Return Error Response]
    
    ERROR --> ERROR_TYPE
    
    ERROR_TYPE -->|Validation| VALIDATION_ERROR
    ERROR_TYPE -->|Business| BUSINESS_ERROR
    ERROR_TYPE -->|System| SYSTEM_ERROR
    ERROR_TYPE -->|Database| DATABASE_ERROR
    
    VALIDATION_ERROR --> LOG_ERROR
    BUSINESS_ERROR --> LOG_ERROR
    SYSTEM_ERROR --> LOG_ERROR
    DATABASE_ERROR --> LOG_ERROR
    
    LOG_ERROR --> AUDIT_ERROR
    AUDIT_ERROR --> USER_MESSAGE
    
    SYSTEM_ERROR --> SYSTEM_ALERT
    DATABASE_ERROR --> SYSTEM_ALERT
    
    SYSTEM_ALERT --> RETRY
    RETRY -->|Yes| RETRY_LOGIC
    RETRY -->|No| FALLBACK
    
    RETRY_LOGIC --> RECOVERY
    FALLBACK --> RECOVERY
    
    RECOVERY -->|Yes| AUTO_RECOVERY
    RECOVERY -->|No| MANUAL_INTERVENTION
    
    USER_MESSAGE --> RESPONSE
    AUTO_RECOVERY --> RESPONSE
    MANUAL_INTERVENTION --> RESPONSE
```

## Deployment and CI/CD Flow

```mermaid
flowchart LR
    subgraph "Development"
        DEV[Developer]
        GIT[Git Repository]
        PR[Pull Request]
    end

    subgraph "CI/CD Pipeline"
        BUILD[Build & Test]
        QUALITY[Quality Gates]
        SECURITY[Security Scan]
        PACKAGE[Package Application]
    end

    subgraph "Deployment Environments"
        DEV_ENV[Development]
        STAGING[Staging]
        PROD[Production]
    end

    subgraph "Monitoring"
        HEALTH[Health Checks]
        METRICS[Performance Metrics]
        ALERTS[Alerting System]
    end

    DEV --> GIT
    GIT --> PR
    PR --> BUILD
    BUILD --> QUALITY
    QUALITY --> SECURITY
    SECURITY --> PACKAGE
    
    PACKAGE --> DEV_ENV
    DEV_ENV --> STAGING
    STAGING --> PROD
    
    DEV_ENV --> HEALTH
    STAGING --> HEALTH
    PROD --> HEALTH
    
    HEALTH --> METRICS
    METRICS --> ALERTS
```

## Database Schema Relationships

```mermaid
erDiagram
    OWNERS ||--o{ PETS : owns
    PETS ||--o{ VISITS : has
    VETERINARIANS ||--o{ VISITS : conducts
    VETERINARIANS ||--o{ VET_SPECIALTIES : has
    SPECIALTIES ||--o{ VET_SPECIALTIES : defines
    USERS ||--o{ AUDIT_LOGS : generates
    VISITS ||--o{ VISIT_NOTES : contains

    OWNERS {
        bigint id PK
        string first_name
        string last_name
        string address
        string city
        string telephone
        timestamp created_at
        timestamp updated_at
    }

    PETS {
        bigint id PK
        string name
        string species
        string breed
        date birth_date
        bigint owner_id FK
        timestamp created_at
        timestamp updated_at
    }

    VISITS {
        bigint id PK
        datetime visit_date
        string visit_type
        string diagnosis
        string treatment
        text notes
        bigint pet_id FK
        bigint veterinarian_id FK
        timestamp created_at
        timestamp updated_at
    }

    VETERINARIANS {
        bigint id PK
        string first_name
        string last_name
        string license_number
        string email
        string phone
        timestamp created_at
        timestamp updated_at
    }

    SPECIALTIES {
        bigint id PK
        string name
        string description
    }

    VET_SPECIALTIES {
        bigint veterinarian_id FK
        bigint specialty_id FK
    }

    USERS {
        bigint id PK
        string username
        string password_hash
        string email
        string role
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    AUDIT_LOGS {
        bigint id PK
        string action
        string entity_type
        bigint entity_id
        bigint user_id FK
        text details
        timestamp created_at
    }
```

## Conclusion

This flowchart documentation provides comprehensive visual representations of the Pet Clinic Management System's workflows, processes, and architecture. These diagrams serve as:

1. **System Understanding**: Clear visualization of how components interact
2. **Development Guide**: Reference for developers implementing features
3. **Troubleshooting Aid**: Visual guide for diagnosing issues
4. **Documentation**: Comprehensive system documentation
5. **Training Material**: Visual aids for user and developer training

The flowcharts are designed to be:
- **Comprehensive**: Covering all major system processes
- **Clear**: Easy to understand and follow
- **Maintainable**: Can be updated as the system evolves
- **Practical**: Useful for both technical and non-technical stakeholders

---

**Document Maintained By**: Development Team  
**Review Cycle**: Updated with major system changes  
**Format**: Mermaid diagrams for version control compatibility