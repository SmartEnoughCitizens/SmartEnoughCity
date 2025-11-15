# Sustainable City Management Backend

A lightweight Java Spring Boot backend for testing the Sustainable City Management System frontend. This backend provides JWT-based authentication and mock REST API endpoints with in-memory data storage.

## Features

- JWT-based authentication
- CORS support for http://localhost:3000
- Pre-seeded test users with different roles
- Mock data for all endpoints (no database required)
- RESTful API endpoints for dashboard, indicators, users, simulations, and notifications
- In-memory data storage for quick testing

## Requirements

- Java 17 or higher
- Maven 3.6+

## Quick Start

### 1. Clone the repository

```bash
cd backend
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## Test User Credentials

The backend comes pre-seeded with 4 test users:

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ROLE_GOVERNMENT_ADMIN |
| manager | manager123 | ROLE_CITY_MANAGER |
| provider_admin | provider123 | ROLE_SERVICE_PROVIDER_ADMIN |
| provider_user | user123 | ROLE_SERVICE_PROVIDER_USER |

## API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication Endpoints

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@city.gov",
    "name": "admin",
    "role": "ROLE_GOVERNMENT_ADMIN",
    "active": true
  }
}
```

#### Register City Manager
```bash
POST /api/auth/register/city-manager
Content-Type: application/json

{
  "username": "newmanager",
  "password": "password123",
  "email": "manager@city.gov",
  "name": "New Manager"
}
```

#### Register Service Provider Admin
```bash
POST /api/auth/register/service-provider-admin
Content-Type: application/json

{
  "username": "newprovider",
  "password": "password123",
  "email": "provider@city.gov",
  "name": "New Provider"
}
```

### Dashboard Endpoints

#### Get Dashboard Stats
```bash
GET /api/dashboard/stats
Authorization: Bearer <token>

Response:
{
  "totalUsers": 245,
  "activeSimulations": 12,
  "totalIncidents": 34,
  "systemHealth": 98.5,
  "recentMetrics": [...]
}
```

#### Get Dashboard Overview
```bash
GET /api/dashboard/overview
Authorization: Bearer <token>

Response:
{
  "summary": {...},
  "charts": [...],
  "alerts": [...]
}
```

### Indicator Endpoints

#### Get Indicator Data
```bash
GET /api/indicators/{mode}
Authorization: Bearer <token>

# Valid modes: car, cycle, bus, train, tram, pedestrian, events, construction

Example:
GET /api/indicators/car

Response:
{
  "mode": "car",
  "summary": {...},
  "timeSeries": [...],
  "metrics": {...}
}
```

### User Management Endpoints

#### Get All Users
```bash
GET /api/users
Authorization: Bearer <token>

Response:
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@city.gov",
    "name": "admin",
    "role": "ROLE_GOVERNMENT_ADMIN",
    "active": true
  },
  ...
]
```

#### Create Service Provider User
```bash
POST /api/users/service-provider
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@provider.com",
  "name": "New User"
}
```

#### Update User
```bash
PUT /api/users/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "updated@city.gov",
  "name": "Updated Name",
  "role": "ROLE_SERVICE_PROVIDER_USER",
  "active": true
}
```

#### Delete User
```bash
DELETE /api/users/{id}
Authorization: Bearer <token>
```

### Simulation Endpoints

#### Get All Simulations
```bash
GET /api/simulations
Authorization: Bearer <token>

Response:
[
  {
    "id": 1,
    "name": "Morning Traffic Analysis",
    "type": "traffic",
    "status": "completed",
    "parameters": {...},
    "results": {...},
    "createdAt": "2024-01-15T10:30:00",
    "completedAt": "2024-01-15T11:30:00",
    "createdBy": "admin"
  },
  ...
]
```

#### Run Simulation
```bash
POST /api/simulations/run
Authorization: Bearer <token>
Content-Type: application/json

{
  "type": "traffic",
  "duration": 60,
  "trafficDensity": "high"
}

Response:
{
  "id": 2,
  "name": "Simulation 2",
  "type": "traffic",
  "status": "running",
  ...
}
```

#### Delete Simulation
```bash
DELETE /api/simulations/{id}
Authorization: Bearer <token>
```

### Notification Endpoints

#### Get All Notifications
```bash
GET /api/notifications
Authorization: Bearer <token>

Response:
[
  {
    "id": 1,
    "title": "System Update",
    "message": "System maintenance scheduled for tonight",
    "type": "info",
    "read": false,
    "createdAt": "2024-01-15T10:00:00",
    "userId": "admin"
  },
  ...
]
```

#### Mark Notification as Read
```bash
PUT /api/notifications/{id}/read
Authorization: Bearer <token>
```

#### Mark All Notifications as Read
```bash
PUT /api/notifications/mark-all-read
Authorization: Bearer <token>
```

### System Endpoints

#### Get System Status
```bash
GET /api/system/status
Authorization: Bearer <token>

Response:
{
  "status": "healthy",
  "version": "1.0.0",
  "uptime": 12345,
  "services": {
    "database": {"status": "healthy", ...},
    "cache": {"status": "healthy", ...},
    ...
  }
}
```

#### Get System Health (Public)
```bash
GET /api/system/health

Response:
{
  "status": "UP",
  "timestamp": "2024-01-15T10:00:00",
  "uptime": 12345,
  "details": {...}
}
```

## Example curl Commands

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Get Dashboard Stats (with authentication)
```bash
# First, save the token from login response
TOKEN="your-jwt-token-here"

curl -X GET http://localhost:8080/api/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"
```

### Get Indicator Data
```bash
curl -X GET http://localhost:8080/api/indicators/car \
  -H "Authorization: Bearer $TOKEN"
```

### Run Simulation
```bash
curl -X POST http://localhost:8080/api/simulations/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"traffic","duration":60,"trafficDensity":"high"}'
```

## Project Structure

```
backend/
├── src/
│   └── main/
│       ├── java/com/city/management/
│       │   ├── CityManagementApplication.java    # Main application class
│       │   ├── config/                            # Security & JWT configuration
│       │   │   ├── SecurityConfig.java
│       │   │   └── JwtAuthenticationFilter.java
│       │   ├── controller/                        # REST controllers
│       │   │   ├── AuthController.java
│       │   │   ├── DashboardController.java
│       │   │   ├── IndicatorController.java
│       │   │   ├── UserController.java
│       │   │   ├── SimulationController.java
│       │   │   ├── NotificationController.java
│       │   │   └── SystemController.java
│       │   ├── model/                             # Data models
│       │   │   ├── User.java
│       │   │   ├── Role.java
│       │   │   ├── LoginRequest.java
│       │   │   ├── JwtResponse.java
│       │   │   └── ...
│       │   ├── service/                           # Business logic
│       │   │   ├── AuthService.java
│       │   │   ├── UserService.java
│       │   │   └── MockDataService.java
│       │   └── util/                              # Utilities
│       │       └── JwtUtil.java
│       └── resources/
│           └── application.properties             # Application configuration
├── pom.xml                                        # Maven dependencies
└── README.md                                      # This file
```

## Configuration

The application can be configured via `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# JWT settings
jwt.secret=<your-secret-key>
jwt.expiration=86400000  # 24 hours in milliseconds
```

## Technologies Used

- **Spring Boot 3.2.0** - Application framework
- **Spring Security** - Authentication & authorization
- **JWT (JSON Web Tokens)** - Token-based authentication
- **Lombok** - Reduce boilerplate code
- **Maven** - Dependency management & build tool

## Development Notes

- This is a **mock backend** for testing purposes only
- All data is stored in-memory and will be reset on application restart
- No real database is used - perfect for quick frontend testing
- CORS is configured to allow requests from `http://localhost:3000`
- JWT tokens expire after 24 hours

## Troubleshooting

### Port already in use
If port 8080 is already in use, change it in `application.properties`:
```properties
server.port=8081
```

### CORS errors
Make sure your frontend is running on `http://localhost:3000`. If using a different port, update `SecurityConfig.java`:
```java
configuration.setAllowedOrigins(List.of("http://localhost:YOUR_PORT"));
```

### JWT token errors
If you get authentication errors, make sure:
1. You're including the `Authorization: Bearer <token>` header
2. The token hasn't expired
3. The token was obtained from the `/api/auth/login` endpoint

## License

This project is created for educational purposes as part of the ASD Group Assignment.
