# Pet Clinic Management System - API Reference

## Overview

The Pet Clinic Management System provides a comprehensive REST API for managing veterinary clinic operations. This document provides detailed information about all available endpoints, request/response formats, authentication, and usage examples.

## Base Information

- **Base URL**: `http://localhost:9090/api/v1`
- **API Version**: v1
- **Content Type**: `application/json`
- **Authentication**: JWT Bearer Token
- **Documentation**: Available at `/swagger-ui.html`

## Authentication

### Login Endpoint

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["ADMIN"],
  "expiresIn": 3600
}
```

### Using Authentication

Include the JWT token in the Authorization header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Pet Management API

### Get All Pets

```http
GET /api/pets?page=0&size=10&sort=name,asc
```

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)
- `sort` (optional): Sort criteria (default: id,asc)
- `search` (optional): Search term for name or breed

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Buddy",
      "species": "Dog",
      "breed": "Golden Retriever",
      "birthDate": "2020-05-15",
      "owner": {
        "id": 1,
        "firstName": "John",
        "lastName": "Doe",
        "telephone": "555-1234"
      },
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### Get Pet by ID

```http
GET /api/pets/{id}
```

**Response:**
```json
{
  "id": 1,
  "name": "Buddy",
  "species": "Dog",
  "breed": "Golden Retriever",
  "birthDate": "2020-05-15",
  "owner": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "telephone": "555-1234",
    "address": "123 Main St",
    "city": "Springfield"
  },
  "visits": [
    {
      "id": 1,
      "visitDate": "2024-01-20T14:00:00Z",
      "visitType": "CHECKUP",
      "diagnosis": "Healthy",
      "veterinarian": {
        "id": 1,
        "firstName": "Dr. Sarah",
        "lastName": "Johnson"
      }
    }
  ],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### Create Pet

```http
POST /api/pets
Content-Type: application/json

{
  "name": "Max",
  "species": "Dog",
  "breed": "Labrador",
  "birthDate": "2021-03-10",
  "ownerId": 1
}
```

**Response:**
```json
{
  "id": 2,
  "name": "Max",
  "species": "Dog",
  "breed": "Labrador",
  "birthDate": "2021-03-10",
  "owner": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe"
  },
  "createdAt": "2024-01-30T15:45:00Z",
  "updatedAt": "2024-01-30T15:45:00Z"
}
```

### Update Pet

```http
PUT /api/pets/{id}
Content-Type: application/json

{
  "name": "Max Updated",
  "species": "Dog",
  "breed": "Labrador Mix",
  "birthDate": "2021-03-10",
  "ownerId": 1
}
```

### Delete Pet

```http
DELETE /api/pets/{id}
```

**Response:** `204 No Content`

### Search Pets

```http
GET /api/pets/search?q=golden&species=Dog&ownerId=1
```

**Query Parameters:**
- `q`: Search term (searches name and breed)
- `species`: Filter by species
- `ownerId`: Filter by owner ID
- `breed`: Filter by breed

## Visit Management API

### Get All Visits

```http
GET /api/visits?page=0&size=10&sort=visitDate,desc
```

**Query Parameters:**
- `page`, `size`, `sort`: Pagination parameters
- `petId`: Filter by pet ID
- `veterinarianId`: Filter by veterinarian ID
- `visitType`: Filter by visit type
- `startDate`: Filter visits after date (ISO format)
- `endDate`: Filter visits before date (ISO format)

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "visitDate": "2024-01-20T14:00:00Z",
      "visitType": "CHECKUP",
      "diagnosis": "Healthy dog, no issues found",
      "treatment": "Routine examination completed",
      "notes": "Patient was cooperative during examination",
      "pet": {
        "id": 1,
        "name": "Buddy",
        "species": "Dog"
      },
      "veterinarian": {
        "id": 1,
        "firstName": "Dr. Sarah",
        "lastName": "Johnson",
        "specialties": ["GENERAL_PRACTICE"]
      },
      "duration": 30,
      "status": "COMPLETED",
      "createdAt": "2024-01-20T14:00:00Z",
      "updatedAt": "2024-01-20T14:30:00Z"
    }
  ],
  "totalElements": 15,
  "totalPages": 2
}
```

### Schedule Visit

```http
POST /api/visits
Content-Type: application/json

{
  "petId": 1,
  "veterinarianId": 1,
  "visitDate": "2024-02-15T10:00:00Z",
  "visitType": "VACCINATION",
  "notes": "Annual vaccination appointment"
}
```

### Complete Visit

```http
PUT /api/visits/{id}/complete
Content-Type: application/json

{
  "diagnosis": "Administered annual vaccines",
  "treatment": "Rabies and DHPP vaccines given",
  "notes": "Patient tolerated vaccines well. Next vaccination due in 1 year.",
  "prescriptions": [
    {
      "medication": "Heartworm preventative",
      "dosage": "1 tablet monthly",
      "duration": "12 months"
    }
  ]
}
```

### Get Visit Schedule

```http
GET /api/visits/schedule?date=2024-02-15&veterinarianId=1
```

**Response:**
```json
{
  "date": "2024-02-15",
  "veterinarian": {
    "id": 1,
    "firstName": "Dr. Sarah",
    "lastName": "Johnson"
  },
  "appointments": [
    {
      "id": 5,
      "time": "09:00:00",
      "duration": 30,
      "pet": {
        "id": 2,
        "name": "Whiskers",
        "species": "Cat"
      },
      "visitType": "CHECKUP",
      "status": "SCHEDULED"
    },
    {
      "id": 6,
      "time": "10:00:00",
      "duration": 45,
      "pet": {
        "id": 1,
        "name": "Buddy",
        "species": "Dog"
      },
      "visitType": "VACCINATION",
      "status": "SCHEDULED"
    }
  ],
  "totalAppointments": 2,
  "availableSlots": [
    "11:00:00",
    "14:00:00",
    "15:30:00"
  ]
}
```

## Veterinarian Management API

### Get All Veterinarians

```http
GET /api/veterinarians?page=0&size=10
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Dr. Sarah",
      "lastName": "Johnson",
      "licenseNumber": "VET-2024-001",
      "email": "sarah.johnson@petclinic.com",
      "phone": "555-0101",
      "specialties": [
        {
          "id": 1,
          "name": "GENERAL_PRACTICE",
          "description": "General veterinary practice"
        },
        {
          "id": 2,
          "name": "SURGERY",
          "description": "Surgical procedures"
        }
      ],
      "workingHours": {
        "monday": "08:00-17:00",
        "tuesday": "08:00-17:00",
        "wednesday": "08:00-17:00",
        "thursday": "08:00-17:00",
        "friday": "08:00-17:00"
      },
      "active": true,
      "createdAt": "2024-01-01T08:00:00Z"
    }
  ]
}
```

### Create Veterinarian

```http
POST /api/veterinarians
Content-Type: application/json

{
  "firstName": "Dr. Michael",
  "lastName": "Smith",
  "licenseNumber": "VET-2024-002",
  "email": "michael.smith@petclinic.com",
  "phone": "555-0102",
  "specialtyIds": [1, 3],
  "workingHours": {
    "monday": "09:00-18:00",
    "tuesday": "09:00-18:00",
    "wednesday": "09:00-18:00",
    "thursday": "09:00-18:00",
    "friday": "09:00-16:00"
  }
}
```

### Get Veterinarian Availability

```http
GET /api/veterinarians/{id}/availability?date=2024-02-15
```

**Response:**
```json
{
  "veterinarianId": 1,
  "date": "2024-02-15",
  "workingHours": "08:00-17:00",
  "scheduledAppointments": [
    {
      "startTime": "09:00:00",
      "endTime": "09:30:00",
      "visitId": 5
    },
    {
      "startTime": "10:00:00",
      "endTime": "10:45:00",
      "visitId": 6
    }
  ],
  "availableSlots": [
    {
      "startTime": "08:00:00",
      "endTime": "09:00:00",
      "duration": 60
    },
    {
      "startTime": "09:30:00",
      "endTime": "10:00:00",
      "duration": 30
    },
    {
      "startTime": "10:45:00",
      "endTime": "17:00:00",
      "duration": 375
    }
  ],
  "totalAvailableMinutes": 465,
  "utilizationPercentage": 22.5
}
```

## Search API

### Global Search

```http
GET /api/search?q=golden&entities=pets,owners,visits
```

**Query Parameters:**
- `q`: Search query
- `entities`: Comma-separated list of entities to search (pets, owners, visits, veterinarians)
- `page`, `size`: Pagination parameters

**Response:**
```json
{
  "query": "golden",
  "totalResults": 8,
  "results": {
    "pets": {
      "count": 3,
      "items": [
        {
          "id": 1,
          "name": "Buddy",
          "breed": "Golden Retriever",
          "owner": "John Doe",
          "highlights": ["Golden Retriever"]
        }
      ]
    },
    "owners": {
      "count": 1,
      "items": [
        {
          "id": 5,
          "firstName": "Golden",
          "lastName": "Smith",
          "telephone": "555-0199",
          "highlights": ["Golden"]
        }
      ]
    },
    "visits": {
      "count": 4,
      "items": [
        {
          "id": 12,
          "visitDate": "2024-01-15T14:00:00Z",
          "petName": "Buddy",
          "diagnosis": "Golden Retriever showing signs of hip dysplasia",
          "highlights": ["Golden Retriever"]
        }
      ]
    }
  },
  "executionTime": 45
}
```

### Advanced Filter

```http
POST /api/search/filter
Content-Type: application/json

{
  "entityType": "pets",
  "filters": [
    {
      "field": "species",
      "operator": "equals",
      "value": "Dog"
    },
    {
      "field": "birthDate",
      "operator": "between",
      "value": ["2020-01-01", "2022-12-31"]
    },
    {
      "field": "owner.city",
      "operator": "equals",
      "value": "Springfield"
    }
  ],
  "sort": [
    {
      "field": "name",
      "direction": "asc"
    }
  ],
  "page": 0,
  "size": 20
}
```

## Reporting API

### Dashboard Metrics

```http
GET /api/reports/dashboard?startDate=2024-01-01&endDate=2024-01-31
```

**Response:**
```json
{
  "period": {
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
  },
  "metrics": {
    "totalVisits": 156,
    "totalRevenue": 15600.00,
    "activePets": 89,
    "newPetRegistrations": 12,
    "averageVisitsPerDay": 5.03,
    "veterinarianUtilization": {
      "average": 78.5,
      "byVeterinarian": [
        {
          "veterinarianId": 1,
          "name": "Dr. Sarah Johnson",
          "utilization": 85.2,
          "totalVisits": 67
        },
        {
          "veterinarianId": 2,
          "name": "Dr. Michael Smith",
          "utilization": 71.8,
          "totalVisits": 89
        }
      ]
    },
    "visitTypeDistribution": {
      "CHECKUP": 45,
      "VACCINATION": 32,
      "SURGERY": 8,
      "EMERGENCY": 15,
      "FOLLOW_UP": 56
    },
    "speciesDistribution": {
      "Dog": 98,
      "Cat": 45,
      "Bird": 8,
      "Other": 5
    }
  },
  "trends": {
    "visitTrend": "increasing",
    "revenueTrend": "stable",
    "registrationTrend": "increasing"
  }
}
```

### Visit Statistics Report

```http
GET /api/reports/visits?startDate=2024-01-01&endDate=2024-01-31&veterinarianId=1&format=json
```

**Query Parameters:**
- `startDate`, `endDate`: Date range
- `veterinarianId` (optional): Filter by veterinarian
- `visitType` (optional): Filter by visit type
- `format`: Response format (json, pdf, csv)

### Revenue Report

```http
GET /api/reports/revenue?startDate=2024-01-01&endDate=2024-01-31&groupBy=month
```

**Response:**
```json
{
  "period": {
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
  },
  "totalRevenue": 15600.00,
  "groupBy": "month",
  "data": [
    {
      "period": "2024-01",
      "revenue": 15600.00,
      "visitCount": 156,
      "averageRevenuePerVisit": 100.00
    }
  ],
  "breakdown": {
    "byVeterinarian": [
      {
        "veterinarianId": 1,
        "name": "Dr. Sarah Johnson",
        "revenue": 6700.00,
        "visitCount": 67
      }
    ],
    "byVisitType": [
      {
        "visitType": "CHECKUP",
        "revenue": 4500.00,
        "visitCount": 45,
        "averagePrice": 100.00
      }
    ]
  }
}
```

### Export Report

```http
GET /api/reports/visits/export?startDate=2024-01-01&endDate=2024-01-31&format=pdf
```

**Response:** Binary file download (PDF or CSV)

## Owner Management API

### Get All Owners

```http
GET /api/owners?page=0&size=10&search=john
```

### Create Owner

```http
POST /api/owners
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith",
  "address": "456 Oak Ave",
  "city": "Springfield",
  "telephone": "555-0123"
}
```

### Get Owner with Pets

```http
GET /api/owners/{id}/pets
```

**Response:**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "address": "123 Main St",
  "city": "Springfield",
  "telephone": "555-1234",
  "pets": [
    {
      "id": 1,
      "name": "Buddy",
      "species": "Dog",
      "breed": "Golden Retriever",
      "birthDate": "2020-05-15",
      "lastVisit": "2024-01-20T14:00:00Z"
    },
    {
      "id": 2,
      "name": "Whiskers",
      "species": "Cat",
      "breed": "Persian",
      "birthDate": "2019-08-10",
      "lastVisit": "2024-01-18T11:00:00Z"
    }
  ],
  "totalPets": 2,
  "createdAt": "2023-12-01T10:00:00Z"
}
```

## Error Handling

### Standard Error Response

```json
{
  "timestamp": "2024-01-30T15:45:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'name': must not be blank",
  "path": "/api/pets",
  "details": {
    "field": "name",
    "rejectedValue": "",
    "code": "NotBlank"
  }
}
```

### Common HTTP Status Codes

- **200 OK**: Successful GET, PUT requests
- **201 Created**: Successful POST requests
- **204 No Content**: Successful DELETE requests
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource conflict (e.g., scheduling conflict)
- **422 Unprocessable Entity**: Business rule violation
- **500 Internal Server Error**: Server error

### Validation Errors

```json
{
  "timestamp": "2024-01-30T15:45:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Multiple validation errors occurred",
  "path": "/api/pets",
  "validationErrors": [
    {
      "field": "name",
      "message": "Pet name is required",
      "rejectedValue": null
    },
    {
      "field": "birthDate",
      "message": "Birth date cannot be in the future",
      "rejectedValue": "2025-01-01"
    }
  ]
}
```

## Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Rate Limit**: 1000 requests per hour per user
- **Headers**: 
  - `X-RateLimit-Limit`: Maximum requests per hour
  - `X-RateLimit-Remaining`: Remaining requests in current window
  - `X-RateLimit-Reset`: Time when rate limit resets

When rate limit is exceeded:

```json
{
  "timestamp": "2024-01-30T15:45:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 15 minutes.",
  "retryAfter": 900
}
```

## API Versioning

The API supports versioning through URL path:

- **Current Version**: `/api/v1/`
- **Version Header**: `Accept: application/vnd.petclinic.v1+json`
- **Deprecation**: Deprecated versions include `Deprecation` header

## Pagination

All list endpoints support pagination:

**Request:**
```http
GET /api/pets?page=0&size=10&sort=name,asc
```

**Response includes pagination metadata:**
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false,
  "numberOfElements": 10
}
```

## WebSocket API (Real-time Updates)

### Connect to WebSocket

```javascript
const socket = new WebSocket('ws://localhost:9090/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': 'Bearer ' + token
}, function(frame) {
  console.log('Connected: ' + frame);
});
```

### Subscribe to Updates

```javascript
// Subscribe to visit updates
stompClient.subscribe('/topic/visits', function(message) {
  const visit = JSON.parse(message.body);
  console.log('Visit updated:', visit);
});

// Subscribe to schedule changes
stompClient.subscribe('/topic/schedule', function(message) {
  const schedule = JSON.parse(message.body);
  console.log('Schedule updated:', schedule);
});
```

## SDK Examples

### JavaScript/Node.js

```javascript
const PetClinicAPI = require('pet-clinic-api-client');

const client = new PetClinicAPI({
  baseURL: 'http://localhost:9090/api/v1',
  token: 'your-jwt-token'
});

// Get all pets
const pets = await client.pets.getAll({ page: 0, size: 10 });

// Create a new pet
const newPet = await client.pets.create({
  name: 'Buddy',
  species: 'Dog',
  breed: 'Golden Retriever',
  birthDate: '2020-05-15',
  ownerId: 1
});

// Schedule a visit
const visit = await client.visits.schedule({
  petId: 1,
  veterinarianId: 1,
  visitDate: '2024-02-15T10:00:00Z',
  visitType: 'CHECKUP'
});
```

### Python

```python
from pet_clinic_client import PetClinicClient

client = PetClinicClient(
    base_url='http://localhost:9090/api/v1',
    token='your-jwt-token'
)

# Get all pets
pets = client.pets.get_all(page=0, size=10)

# Create a new pet
new_pet = client.pets.create({
    'name': 'Buddy',
    'species': 'Dog',
    'breed': 'Golden Retriever',
    'birth_date': '2020-05-15',
    'owner_id': 1
})

# Search pets
results = client.search.global_search('golden retriever')
```

## Testing the API

### Using curl

```bash
# Login
curl -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# Get pets (with token)
curl -X GET http://localhost:9090/api/v1/pets \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Create a pet
curl -X POST http://localhost:9090/api/v1/pets \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Max",
    "species": "Dog",
    "breed": "Labrador",
    "birthDate": "2021-03-10",
    "ownerId": 1
  }'
```

### Using Postman

1. Import the OpenAPI specification from `/api-docs`
2. Set up environment variables for base URL and token
3. Use the pre-configured requests for testing

## Support and Documentation

- **Interactive Documentation**: `/swagger-ui.html`
- **OpenAPI Specification**: `/api-docs`
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`

For additional support, please refer to the main documentation or contact the development team.

---

**API Version**: 1.0  
**Last Updated**: January 30, 2026  
**Maintained By**: Pet Clinic Development Team