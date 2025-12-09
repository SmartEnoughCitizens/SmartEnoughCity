# Disruption Management Engine - Implementation Documentation

## Overview

The Disruption Management Engine is the central module for monitoring and responding to transport disruptions in real-time. This document describes the skeleton implementation that has been created.

## Architecture

### Layer Structure

```
Controller Layer (API Endpoints)
    ↓
Facade Layer (Business Logic Orchestration)
    ↓
Service Layer (Specialized Business Services)
    ↓
Repository Layer (Data Persistence)
```

## Components

### 1. Entity Layer

#### `Disruption.java`
The main entity representing a disruption incident with comprehensive fields:
- **Classification**: Type, severity, status
- **Location**: GPS coordinates, affected area
- **Transport Info**: Affected modes, routes, stops
- **Timing**: Detection time, start/end times, resolution time
- **Solutions**: Alternative routes, recommendations
- **Source**: Data source, reference IDs
- **Context**: Event details, construction info, delay duration

### 2. DTO Layer

#### `DisruptionDetectionRequest.java`
**Purpose**: Receives disruption detection data from Python data handler service.

**Key Fields**:
- Disruption type and severity
- Location data (lat/lon, affected area)
- Affected transport modes, routes, stops
- Timing information
- Source details
- Additional context (events, construction, traffic)

**Usage**: Python service sends this payload to `/api/v1/disruptions/detect`

#### `AlternativeRoute.java`
**Purpose**: Represents a single alternative route option.

**Key Fields**:
- Route identification
- Transport modes and stops
- Metrics (duration, distance, cost)
- Quality scores (comfort, reliability, crowding)
- Priority and recommendation flags

#### `DisruptionSolution.java`
**Purpose**: Compiled solution ready for notification.

**Key Fields**:
- Primary and secondary recommendations
- Step-by-step instructions
- Impact estimation
- Affected user groups
- Notification readiness flag

### 3. Service Layer

#### `ThresholdDetectionService.java`
**Responsibilities**:
- Validate if disruption meets threshold criteria
- Calculate severity levels
- Determine if immediate action is required
- Estimate number of affected travelers

**Key Methods**:
- `meetsThreshold()` - Validate threshold criteria
- `calculateSeverity()` - Determine severity level
- `requiresImmediateAction()` - Check urgency
- `estimateAffectedTravelers()` - Impact estimation

#### `AlternativeRoutingService.java`
**Responsibilities**:
- Calculate alternative routes for disruptions
- Evaluate route quality and metrics
- Score and rank alternatives
- Support multi-modal routing

**Key Methods**:
- `calculateAlternativeRoutes()` - Main routing logic
- `calculateRoutesForPoints()` - Point-to-point routing
- `evaluateRoute()` - Score individual routes
- `findBestAlternative()` - Select top option

#### `SolutionCompilationService.java`
**Responsibilities**:
- Compile solutions from alternative routes
- Prioritize and rank options
- Generate user-friendly guidance
- Identify affected user groups

**Key Methods**:
- `compileSolution()` - Create complete solution
- `prioritizeRoutes()` - Rank alternatives
- `selectPrimaryRecommendation()` - Choose best option
- `generateActionSummary()` - Create user summary
- `generateInstructions()` - Step-by-step guidance

#### `NotificationCoordinationService.java`
**Responsibilities**:
- Coordinate with Notification module
- Format solutions for delivery
- Handle different notification types
- Track delivery status

**Key Methods**:
- `sendToNotificationHandler()` - Send solution to notifications
- `sendUrgentNotification()` - Handle critical disruptions
- `sendDisruptionUpdate()` - Send status updates
- `sendResolutionNotification()` - Notify resolution

#### `IncidentLoggingService.java`
**Responsibilities**:
- Log all disruption events
- Track performance metrics
- Support audit and optimization
- Generate analytics reports

**Key Methods**:
- `logDisruptionDetected()` - Log new disruption
- `logRouteCalculation()` - Track calculation performance
- `logSolutionCompilation()` - Record solution details
- `logNotificationSent()` - Track notifications
- `logDisruptionResolved()` - Record resolution
- `generateAnalyticsReport()` - Create analysis reports

### 4. Facade Layer

#### `DisruptionFacade.java`
**Purpose**: Orchestrates the complete disruption management workflow.

**Main Workflow** (`handleDisruptionDetection`):
1. Validate threshold criteria
2. Create disruption record
3. Calculate alternative routes
4. Compile solution
5. Send to notification handler
6. Log incident

**Key Methods**:
- `handleDisruptionDetection()` - Main entry point for Python service
- `processDisruption()` - Process disruption and generate solution
- `resolveDisruption()` - Mark disruption as resolved
- `getActiveDisruptions()` - Query active incidents
- Standard CRUD operations

### 5. Controller Layer

#### `DisruptionController.java`
**Purpose**: REST API endpoints for disruption management.

**Key Endpoints**:

##### Disruption Detection (Python Service)
```
POST /api/v1/disruptions/detect
Body: DisruptionDetectionRequest
Returns: DisruptionSolution (201) or NO_CONTENT (204) if threshold not met
```

##### Query Endpoints
```
GET  /api/v1/disruptions/active           - Get active disruptions
GET  /api/v1/disruptions/severity/{level} - Filter by severity
GET  /api/v1/disruptions/area/{area}      - Filter by area
GET  /api/v1/disruptions/{id}/logs        - Get incident logs
```

##### Management Endpoints
```
POST /api/v1/disruptions/{id}/resolve     - Resolve disruption
```

##### Standard CRUD
```
GET    /api/v1/disruptions     - List all
GET    /api/v1/disruptions/{id} - Get by ID
POST   /api/v1/disruptions     - Create
PUT    /api/v1/disruptions/{id} - Update
DELETE /api/v1/disruptions/{id} - Delete
```

## Integration Flow

### Python Data Handler → Disruption Engine

```
┌─────────────────────┐
│ Python Data Handler │
│  (External Service) │
└──────────┬──────────┘
           │
           │ 1. Detects disruption in data stream
           │    (delay, congestion, event, etc.)
           │
           ↓
    POST /api/v1/disruptions/detect
    {
      disruptionType: "DELAY",
      severity: "HIGH",
      affectedRoutes: ["Bus15", "Bus27"],
      ...
    }
           │
           ↓
┌──────────────────────┐
│ DisruptionController │
│  detectDisruption()  │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│  DisruptionFacade    │
│ handleDisruption...()│
└──────────┬───────────┘
           │
           ├─→ ThresholdDetectionService (validate)
           ├─→ AlternativeRoutingService (calculate routes)
           ├─→ SolutionCompilationService (compile solution)
           ├─→ NotificationCoordinationService (send to users)
           └─→ IncidentLoggingService (log everything)
           │
           ↓
    Returns DisruptionSolution
    {
      primaryRecommendation: {...},
      actionSummary: "Take Metro Line 2...",
      ...
    }
```

## TODO: Implementation Steps

The skeleton is complete. To make it functional, implement the following in order:

### Phase 1: Core Infrastructure
1. **Database Setup**
   - Create JPA entity annotations for `Disruption`
   - Implement `DisruptionRepository` with Spring Data JPA
   - Add custom queries for filtering (active, by severity, by area)

2. **Configuration**
   - Create threshold configuration (application.yml or database)
   - Define severity levels and criteria
   - Configure notification channels

### Phase 2: Routing Logic
3. **Alternative Routing Service**
   - Integrate with routing algorithm library (e.g., GraphHopper, OSRM)
   - Implement multi-modal routing
   - Add route evaluation and scoring logic
   - Integrate with transport data APIs

4. **Solution Compilation**
   - Implement prioritization algorithms
   - Create user guidance templates
   - Add natural language generation for instructions

### Phase 3: Integration
5. **Notification Integration**
   - Create NotificationFacade interface
   - Implement notification formatting
   - Set up notification channels (push, email, SMS)

6. **Recommendation Integration**
   - Link with RecommendationFacade
   - Use ML models for route suggestions
   - Integrate historical preference data

### Phase 4: Persistence and Analytics
7. **Logging and Analytics**
   - Implement incident log repository
   - Create performance metrics tracking
   - Build analytics dashboard
   - Generate optimization reports

### Phase 5: Testing and Optimization
8. **Testing**
   - Unit tests for all services
   - Integration tests for complete workflow
   - Load testing for Python service integration
   - Mock disruption scenarios

9. **Optimization**
   - Cache frequently accessed data
   - Optimize routing calculations
   - Implement async processing for non-critical tasks
   - Add rate limiting for external API calls

## Python Service Integration Guide

### Required Payload Format

```python
import requests
import json
from datetime import datetime

def report_disruption(disruption_data):
    """
    Send disruption to Java backend when detected in data stream
    """
    endpoint = "http://localhost:8080/api/v1/disruptions/detect"
    
    payload = {
        "disruptionType": "DELAY",  # DELAY, CANCELLATION, CONGESTION, CONSTRUCTION, EVENT, ACCIDENT
        "severity": "HIGH",          # LOW, MEDIUM, HIGH, CRITICAL
        "description": "Significant delay on Bus Route 15 due to traffic accident",
        
        # Location
        "latitude": 52.3676,
        "longitude": 4.9041,
        "affectedArea": "City Center",
        
        # Transport info
        "affectedTransportModes": ["BUS"],
        "affectedRoutes": ["Bus15", "Bus27"],
        "affectedStops": ["Central Station", "Dam Square"],
        
        # Timing
        "detectedAt": datetime.now().isoformat(),
        "estimatedStartTime": datetime.now().isoformat(),
        "estimatedEndTime": None,  # If unknown
        "delayMinutes": 30,
        
        # Source
        "dataSource": "REAL_TIME_API",
        "sourceReferenceId": "incident-12345",
        
        # Additional context
        "additionalNotes": "Heavy traffic on main road"
    }
    
    try:
        response = requests.post(endpoint, json=payload)
        
        if response.status_code == 201:
            solution = response.json()
            print(f"Disruption processed. Solution generated: {solution['actionSummary']}")
            return solution
        elif response.status_code == 204:
            print("Disruption below threshold, not processed")
            return None
        else:
            print(f"Error: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"Failed to report disruption: {e}")
        return None
```

### When to Call the Endpoint

The Python service should call `/api/v1/disruptions/detect` when:

1. **Transport Delays**: Delay > 10 minutes detected
2. **Traffic Congestion**: Congestion level exceeds threshold
3. **Service Cancellations**: Line/route cancelled
4. **Events**: Large event affecting transport
5. **Construction**: New construction work detected
6. **Accidents**: Incident affecting routes

## Notes on Lint Warnings

The IDE is showing "non-project file" warnings. These are benign and will resolve when:
1. The project is properly built with Gradle
2. The IDE reindexes the project
3. Dependencies are properly resolved

The package declarations are correct (`com.trinity.hermes.DisruptionManagement.*`).

## Summary

This implementation provides a complete skeleton for the Disruption Management Engine with:

✅ Complete data model for disruptions  
✅ DTO for Python service integration  
✅ Specialized service classes for each responsibility  
✅ Orchestration logic in facade layer  
✅ REST API endpoints  
✅ Logging and analytics hooks  
✅ Notification coordination  
✅ Clear TODOs for implementation  

The architecture supports the full workflow from detection to notification and is ready for implementation of the business logic.
