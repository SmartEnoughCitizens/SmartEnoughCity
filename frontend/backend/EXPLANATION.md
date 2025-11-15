# Spring Boot Backend - Detailed Code Explanation

This document provides a comprehensive explanation of how the Sustainable City Management Backend works.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Application Flow](#application-flow)
3. [Security & Authentication](#security--authentication)
4. [Core Components](#core-components)
5. [Data Models](#data-models)
6. [API Controllers](#api-controllers)
7. [In-Memory Data Storage](#in-memory-data-storage)
8. [Request/Response Flow](#requestresponse-flow)

---

## Architecture Overview

The backend follows a **layered architecture** pattern:

```
┌─────────────────────────────────────┐
│         REST Controllers            │  ← HTTP requests/responses
├─────────────────────────────────────┤
│      Service Layer (Business)       │  ← Business logic
├─────────────────────────────────────┤
│     Data Layer (In-Memory Maps)     │  ← Data storage
├─────────────────────────────────────┤
│    Security Layer (JWT Filter)      │  ← Authentication
└─────────────────────────────────────┘
```

### Key Design Decisions:

1. **In-Memory Storage**: Using `ConcurrentHashMap` instead of a database for simplicity and speed
2. **JWT Authentication**: Stateless authentication for scalability
3. **CORS Support**: Configured to work with React frontend on localhost:3000
4. **Mock Data**: Pre-seeded data for immediate testing

---

## Application Flow

### 1. Application Startup

**File**: `CityManagementApplication.java`

```java
@SpringBootApplication
public class CityManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityManagementApplication.class, args);
    }
}
```

**What happens:**
1. Spring Boot scans the package `com.city.management` for components
2. Auto-configures beans (controllers, services, security)
3. Starts embedded Tomcat server on port 8080
4. `UserService.initUsers()` runs (marked with `@PostConstruct`) to seed test users
5. `MockDataService` constructor initializes mock data

---

## Security & Authentication

### JWT (JSON Web Token) Flow

```
┌──────────┐                ┌──────────┐                ┌──────────┐
│  Client  │                │  Backend │                │   User   │
│          │                │          │                │  Store   │
└────┬─────┘                └────┬─────┘                └────┬─────┘
     │                           │                           │
     │  POST /api/auth/login     │                           │
     ├──────────────────────────>│                           │
     │  {username, password}     │                           │
     │                           │  Validate credentials     │
     │                           ├──────────────────────────>│
     │                           │                           │
     │                           │  User found              │
     │                           │<──────────────────────────┤
     │                           │                           │
     │                           │  Generate JWT token       │
     │                           │  (signed with secret)     │
     │                           │                           │
     │  Response: {token, user}  │                           │
     │<──────────────────────────┤                           │
     │                           │                           │
     │  GET /api/dashboard/stats │                           │
     │  Authorization: Bearer <token>                        │
     ├──────────────────────────>│                           │
     │                           │                           │
     │                           │  Validate JWT             │
     │                           │  Extract username & role  │
     │                           │                           │
     │  Response: {stats...}     │                           │
     │<──────────────────────────┤                           │
```

### Security Components

#### 1. JwtUtil (Token Management)

**File**: `util/JwtUtil.java`

```java
@Component
public class JwtUtil {
    // Generate token with username and role
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    // Validate token
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
```

**Key methods:**
- `generateToken()`: Creates JWT with username as subject and role as claim
- `validateToken()`: Checks if token is valid and not expired
- `extractUsername()`: Extracts username from token
- `extractRole()`: Extracts user role from token claims

**How it works:**
1. Token is signed using HMAC-SHA256 algorithm with a secret key
2. Token contains: header (algorithm), payload (claims), signature
3. Expiration is set to 24 hours (86400000 ms)
4. Secret key is loaded from `application.properties`

#### 2. JwtAuthenticationFilter

**File**: `config/JwtAuthenticationFilter.java`

This filter runs **before every request** to validate JWT tokens.

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain chain) {
        // 1. Extract token from Authorization header
        String jwt = authorizationHeader.substring(7); // Remove "Bearer "

        // 2. Extract username from token
        username = jwtUtil.extractUsername(jwt);

        // 3. Validate token
        if (jwtUtil.validateToken(jwt, username)) {
            // 4. Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 5. Continue filter chain
        chain.doFilter(request, response);
    }
}
```

**Flow:**
1. Request comes in with `Authorization: Bearer <token>` header
2. Filter extracts token from header
3. Validates token using `JwtUtil`
4. If valid, sets authentication in Spring Security context
5. Controller can access authenticated user via `Authentication` parameter

#### 3. SecurityConfig

**File**: `config/SecurityConfig.java`

Configures Spring Security with JWT and CORS.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Public endpoints
                .anyRequest().authenticated()                 // All others require auth
            )
            .addFilterBefore(jwtAuthenticationFilter,
                           UsernamePasswordAuthenticationFilter.class);
    }
}
```

**Key configurations:**
- **CSRF disabled**: Not needed for stateless JWT authentication
- **Stateless sessions**: No server-side session storage
- **Public endpoints**: `/api/auth/**` accessible without token
- **JWT filter**: Runs before Spring's default authentication filter
- **CORS**: Allows requests from `http://localhost:3000`

---

## Core Components

### 1. UserService

**File**: `service/UserService.java`

Manages user data using in-memory storage.

```java
@Service
public class UserService {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    @PostConstruct
    public void initUsers() {
        // Pre-seed test users
        createUser(1L, "admin", "admin123", ..., ROLE_GOVERNMENT_ADMIN);
        // ... more users
    }
}
```

**Why two maps?**
- `users`: Fast lookup by ID (O(1))
- `usersByUsername`: Fast lookup by username for login (O(1))

**Key methods:**
- `findByUsername()`: Used during login
- `validatePassword()`: Uses BCrypt to compare hashed passwords
- `createUser()`: Creates new user with auto-generated ID
- `updateUser()`: Updates existing user fields
- `deleteUser()`: Removes user from both maps

**Password hashing:**
```java
@Autowired
private PasswordEncoder passwordEncoder;

// Store hashed password
user.setPassword(passwordEncoder.encode(password));

// Validate password
public boolean validatePassword(User user, String password) {
    return passwordEncoder.matches(password, user.getPassword());
}
```

BCrypt automatically generates salt and stores it with the hash.

### 2. AuthService

**File**: `service/AuthService.java`

Handles authentication logic.

```java
@Service
public class AuthService {
    public Optional<JwtResponse> login(String username, String password) {
        // 1. Find user
        Optional<User> userOpt = userService.findByUsername(username);

        // 2. Validate password
        if (!userService.validatePassword(user, password)) {
            return Optional.empty();
        }

        // 3. Generate JWT token
        String token = jwtUtil.generateToken(username, role);

        // 4. Return response with token and user data
        return Optional.of(new JwtResponse(token, userDTO));
    }
}
```

**Registration flow:**
```java
public Optional<JwtResponse> registerCityManager(RegisterRequest request) {
    // 1. Check if username exists
    if (userService.findByUsername(request.getUsername()).isPresent()) {
        return Optional.empty();
    }

    // 2. Create user with specific role
    User user = userService.createUser(..., Role.ROLE_CITY_MANAGER);

    // 3. Generate token and return
    String token = jwtUtil.generateToken(...);
    return Optional.of(new JwtResponse(token, userDTO));
}
```

### 3. MockDataService

**File**: `service/MockDataService.java`

Provides realistic mock data for testing.

```java
@Service
public class MockDataService {
    private final Map<Long, Simulation> simulations = new ConcurrentHashMap<>();
    private final Map<Long, Notification> notifications = new ConcurrentHashMap<>();

    public MockDataService() {
        initNotifications();
        initSimulations();
    }
}
```

**Key features:**
- **Random data generation**: Uses `Random` for realistic variation
- **Time-series data**: Generates 24-hour data points
- **Async simulation**: Simulates long-running tasks with threads
- **Thread-safe**: Uses `ConcurrentHashMap` for concurrent access

**Example - Indicator data generation:**
```java
public IndicatorData getIndicatorData(String mode) {
    // Create summary with random values
    summary.put("totalCount", new Random().nextInt(1000) + 500);

    // Generate 24-hour time series
    for (int i = 0; i < 24; i++) {
        timeSeries.add(new TimeSeriesData(
            LocalDateTime.now().minusHours(24 - i).format(formatter),
            50 + new Random().nextDouble() * 50,
            metadata
        ));
    }

    return new IndicatorData(mode, summary, timeSeries, metrics);
}
```

**Async simulation:**
```java
public Simulation runSimulation(Map<String, Object> params, String username) {
    // Create simulation with "running" status
    Simulation simulation = new Simulation();
    simulation.setStatus("running");

    // Simulate async processing
    new Thread(() -> {
        Thread.sleep(2000);  // Simulate 2-second processing
        simulation.setStatus("completed");
        simulation.setResults(generatedResults);
    }).start();

    return simulation;  // Return immediately
}
```

---

## Data Models

### User Model

**File**: `model/User.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;      // Stored as BCrypt hash
    private String email;
    private String name;
    private Role role;            // Enum: ROLE_GOVERNMENT_ADMIN, etc.
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
```

**Lombok annotations:**
- `@Data`: Generates getters, setters, toString, equals, hashCode
- `@NoArgsConstructor`: Generates no-argument constructor
- `@AllArgsConstructor`: Generates all-arguments constructor

### Role Enum

```java
public enum Role {
    ROLE_GOVERNMENT_ADMIN,
    ROLE_CITY_MANAGER,
    ROLE_SERVICE_PROVIDER_ADMIN,
    ROLE_SERVICE_PROVIDER_USER
}
```

Roles are used for authorization (could be extended with method security).

### JwtResponse

```java
@Data
public class JwtResponse {
    private String token;
    private UserDTO user;

    @Data
    public static class UserDTO {
        private Long id;
        private String username;
        private String email;
        private String name;
        private String role;
        private boolean active;

        public static UserDTO fromUser(User user) {
            // Convert User to UserDTO (excludes password)
        }
    }
}
```

**Why UserDTO?**
- Excludes sensitive data (password) from response
- Converts Role enum to String for JSON
- Separation of internal model from API response

---

## API Controllers

### AuthController

**File**: `controller/AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<JwtResponse> response = authService.login(...);

        if (response.isEmpty()) {
            return ResponseEntity.status(401)
                .body(new ErrorResponse("Invalid username or password"));
        }

        return ResponseEntity.ok(response.get());
    }
}
```

**Annotations explained:**
- `@RestController`: Combines `@Controller` + `@ResponseBody`
- `@RequestMapping("/api/auth")`: Base path for all endpoints in this controller
- `@PostMapping("/login")`: Maps POST requests to `/api/auth/login`
- `@RequestBody`: Deserializes JSON request body to Java object

**Response handling:**
- Success: `ResponseEntity.ok(data)` → HTTP 200 with JSON body
- Failure: `ResponseEntity.status(401).body(error)` → HTTP 401 with error message

### DashboardController

```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(mockDataService.getDashboardStats());
    }
}
```

**Flow:**
1. Client sends: `GET /api/dashboard/stats` with `Authorization: Bearer <token>`
2. `JwtAuthenticationFilter` validates token
3. Spring routes request to `DashboardController.getStats()`
4. Controller calls `MockDataService.getDashboardStats()`
5. Service returns mock data
6. Spring serializes data to JSON
7. Response sent to client

### SimulationController

```java
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    @PostMapping("/run")
    public ResponseEntity<Simulation> runSimulation(
            @RequestBody Map<String, Object> parameters,
            Authentication authentication) {

        String username = authentication.getName();  // Get authenticated user
        Simulation simulation = mockDataService.runSimulation(parameters, username);
        return ResponseEntity.ok(simulation);
    }
}
```

**Authentication parameter:**
- `Authentication authentication`: Automatically injected by Spring Security
- Contains authenticated user's details
- `authentication.getName()`: Returns username from JWT token

---

## In-Memory Data Storage

### Why ConcurrentHashMap?

```java
private final Map<Long, User> users = new ConcurrentHashMap<>();
```

**Benefits:**
1. **Thread-safe**: Multiple requests can access simultaneously
2. **No locking on reads**: Better performance than `Collections.synchronizedMap`
3. **Segment locking**: Only locks portion of map on writes
4. **No database overhead**: Perfect for testing/prototyping

### Data Initialization

```java
@Service
public class UserService {
    @PostConstruct  // Runs after dependency injection
    public void initUsers() {
        createUser(1L, "admin", "admin123", ..., ROLE_GOVERNMENT_ADMIN);
        // ... more users
    }
}
```

**@PostConstruct lifecycle:**
1. Spring creates `UserService` bean
2. Injects dependencies (`@Autowired` fields)
3. Runs `@PostConstruct` method
4. Bean is ready for use

### Atomic ID Generation

```java
private final AtomicLong idGenerator = new AtomicLong(5);

public User createUser(...) {
    Long id = idGenerator.getAndIncrement();  // Thread-safe increment
    // ... create user with ID
}
```

**AtomicLong** ensures thread-safe ID generation without synchronization.

---

## Request/Response Flow

### Complete Example: Login Request

**1. Client sends request:**
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**2. Spring processes request:**
```
Request → Tomcat → DispatcherServlet → SecurityFilterChain
```

**3. Security filter chain:**
```
JwtAuthenticationFilter (skipped - public endpoint)
   ↓
SecurityConfig allows /api/auth/** without authentication
   ↓
Routes to AuthController.login()
```

**4. Controller execution:**
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    // Spring deserializes JSON to LoginRequest object
    // loginRequest.getUsername() = "admin"
    // loginRequest.getPassword() = "admin123"

    Optional<JwtResponse> response = authService.login(
        loginRequest.getUsername(),
        loginRequest.getPassword()
    );

    return ResponseEntity.ok(response.get());
}
```

**5. Service execution:**
```java
public Optional<JwtResponse> login(String username, String password) {
    // Find user in ConcurrentHashMap
    Optional<User> userOpt = userService.findByUsername("admin");

    // Validate password using BCrypt
    boolean valid = passwordEncoder.matches("admin123", user.getPassword());

    // Generate JWT token
    String token = jwtUtil.generateToken("admin", "ROLE_GOVERNMENT_ADMIN");

    // Create response
    return Optional.of(new JwtResponse(token, userDTO));
}
```

**6. JWT generation:**
```java
public String generateToken(String username, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", role);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)          // Subject: "admin"
        .setIssuedAt(new Date())       // iat: current timestamp
        .setExpiration(new Date(...))  // exp: 24 hours from now
        .signWith(secretKey, HS256)    // Sign with secret
        .compact();
}
```

**7. Response sent:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiUk9MRV9HT1ZFUk5NRU5UX0FETUlOIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3MDUzMTg4MDAsImV4cCI6MTcwNTQwNTIwMH0.signature",
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

### Authenticated Request Flow

**1. Client sends request with token:**
```bash
GET http://localhost:8080/api/dashboard/stats
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**2. JwtAuthenticationFilter executes:**
```java
protected void doFilterInternal(...) {
    // Extract token
    String jwt = request.getHeader("Authorization").substring(7);

    // Extract username from token
    String username = jwtUtil.extractUsername(jwt);  // "admin"

    // Validate token
    if (jwtUtil.validateToken(jwt, username)) {
        // Extract role
        String role = jwtUtil.extractRole(jwt);  // "ROLE_GOVERNMENT_ADMIN"

        // Create authentication object
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(role))
            );

        // Set in security context
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    // Continue to controller
    chain.doFilter(request, response);
}
```

**3. Controller receives authenticated request:**
```java
@GetMapping("/stats")
public ResponseEntity<DashboardStats> getStats() {
    // Authentication is already validated
    // User details available via SecurityContextHolder

    return ResponseEntity.ok(mockDataService.getDashboardStats());
}
```

---

## Key Concepts Summary

### 1. Dependency Injection
```java
@Autowired
private UserService userService;  // Spring injects instance
```
Spring creates and manages bean instances, injecting them where needed.

### 2. Bean Lifecycle
```
Constructor → @Autowired injection → @PostConstruct → Bean ready
```

### 3. Spring Security Filter Chain
```
Request → JwtAuthenticationFilter → SecurityConfig rules → Controller
```

### 4. Password Security
- **Never store plain-text passwords**
- Use BCrypt (includes salt, slow by design)
- Hash during registration, compare during login

### 5. Stateless Authentication
- **No server-side sessions**
- JWT contains all needed info
- Each request validated independently
- Scales horizontally (no shared session storage)

### 6. CORS (Cross-Origin Resource Sharing)
```java
configuration.setAllowedOrigins(List.of("http://localhost:3000"));
```
Allows frontend on different port to call API.

---

## Error Handling

### Authentication Errors
```java
if (response.isEmpty()) {
    return ResponseEntity.status(401)
        .body(new ErrorResponse("Invalid username or password"));
}
```
Returns HTTP 401 Unauthorized with error message.

### Resource Not Found
```java
if (result.isEmpty()) {
    return ResponseEntity.notFound().build();  // HTTP 404
}
```

### Validation Errors
```java
if (userService.findByUsername(username).isPresent()) {
    return ResponseEntity.status(400)
        .body(new ErrorResponse("Username already exists"));
}
```
Returns HTTP 400 Bad Request.

---

## Testing the Backend

### 1. Start the application
```bash
mvn spring-boot:run
```

### 2. Test login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 3. Use token for authenticated requests
```bash
TOKEN="<paste-token-here>"

curl -X GET http://localhost:8080/api/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"
```

---

## Conclusion

This backend demonstrates:
- ✅ JWT-based authentication
- ✅ Spring Security configuration
- ✅ RESTful API design
- ✅ In-memory data storage
- ✅ CORS support
- ✅ Password hashing
- ✅ Layered architecture
- ✅ Dependency injection
- ✅ Mock data generation

It's designed as a **lightweight testing backend** - not for production use, but perfect for frontend development and testing!
