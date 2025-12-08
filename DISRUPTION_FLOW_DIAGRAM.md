# Disruption Management Engine - Flow Diagram

## Complete System Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL DATA SOURCES                            │
│  (Bus API, Tram API, Train API, Traffic API, Events API, etc.)         │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                     PYTHON DATA HANDLER SERVICE                          │
│                                                                          │
│  • Collects data from all external APIs                                │
│  • Cleans and normalizes data                                           │
│  • MONITORS FOR DISRUPTIONS:                                            │
│    - Transport delays                                                    │
│    - Traffic congestion                                                  │
│    - Service cancellations                                              │
│    - Construction work                                                   │
│    - Events affecting transport                                         │
│                                                                          │
│  IF disruption detected:                                                │
│    → Prepare DisruptionDetectionRequest payload                         │
│    → POST to /api/v1/disruptions/detect                                 │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    DISRUPTION MANAGEMENT ENGINE                          │
│                      (Java Spring Boot)                                  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ CONTROLLER LAYER                                               │    │
│  │ DisruptionController.detectDisruption()                        │    │
│  │ - Receives POST /api/v1/disruptions/detect                     │    │
│  └──────────────────────────┬─────────────────────────────────────┘    │
│                             ↓                                            │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ FACADE LAYER                                                   │    │
│  │ DisruptionFacade.handleDisruptionDetection()                   │    │
│  │                                                                 │    │
│  │ WORKFLOW:                                                       │    │
│  │ 1. Validate threshold                                           │    │
│  │ 2. Create disruption record                                     │    │
│  │ 3. Process disruption                                           │    │
│  │ 4. Send to notification                                         │    │
│  │ 5. Log incident                                                 │    │
│  └──────────────────────────┬─────────────────────────────────────┘    │
│                             ↓                                            │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ SERVICE LAYER                                                   │    │
│  │                                                                 │    │
│  │ Step 1: ThresholdDetectionService                              │    │
│  │         ├─ meetsThreshold()                                     │    │
│  │         ├─ calculateSeverity()                                  │    │
│  │         └─ requiresImmediateAction()                            │    │
│  │                                                                 │    │
│  │ Step 2: AlternativeRoutingService                              │    │
│  │         ├─ calculateAlternativeRoutes()                         │    │
│  │         ├─ evaluateRoute()                                      │    │
│  │         └─ findBestAlternative()                                │    │
│  │                                                                 │    │
│  │ Step 3: SolutionCompilationService                             │    │
│  │         ├─ compileSolution()                                    │    │
│  │         ├─ prioritizeRoutes()                                   │    │
│  │         ├─ selectPrimaryRecommendation()                        │    │
│  │         ├─ generateActionSummary()                              │    │
│  │         └─ generateInstructions()                               │    │
│  │                                                                 │    │
│  │ Step 4: NotificationCoordinationService                        │    │
│  │         └─ sendToNotificationHandler()                          │    │
│  │                                                                 │    │
│  │ Step 5: IncidentLoggingService                                 │    │
│  │         ├─ logDisruptionDetected()                              │    │
│  │         ├─ logRouteCalculation()                                │    │
│  │         ├─ logSolutionCompilation()                             │    │
│  │         └─ logNotificationSent()                                │    │
│  └────────────────────────────────────────────────────────────────┘    │
└───────────┬──────────────────────────────────────┬──────────────────────┘
            │                                      │
            ↓                                      ↓
┌────────────────────────┐         ┌────────────────────────────────┐
│  NOTIFICATION MODULE   │         │   INCIDENT LOG DATABASE        │
│                        │         │                                │
│ • Sends alerts to users│         │ • Stores all disruption events │
│ • Push notifications   │         │ • Performance metrics          │
│ • Email/SMS            │         │ • Analytics data               │
│ • In-app messages      │         │ • Audit trail                  │
└────────────────────────┘         └────────────────────────────────┘


## Data Flow Example: Bus Delay

1. Python Service: "Bus 15 delayed by 20 minutes"
   ↓
2. POST to /api/v1/disruptions/detect
   {
     "disruptionType": "DELAY",
     "severity": "MEDIUM",
     "affectedRoutes": ["Bus15"],
     "delayMinutes": 20,
     ...
   }
   ↓
3. Threshold Check: ✓ Meets criteria (delay > 10 min)
   ↓
4. Calculate Alternatives:
   - Option 1: Metro Line 2 + Bus 27 (25 min)
   - Option 2: Tram 5 (30 min)
   - Option 3: Walk to Metro Station (35 min)
   ↓
5. Compile Solution:
   Primary: Metro Line 2 + Bus 27
   Summary: "Take Metro Line 2 instead of Bus 15"
   Instructions: ["Walk to Central Station", "Take Metro Line 2...", ...]
   ↓
6. Send to Notification Module:
   {
     "users": [affected users on Bus 15 route],
     "message": "Bus 15 delayed. Take Metro Line 2 instead.",
     "alternatives": [...]
   }
   ↓
7. Log Everything:
   - Detection time: 14:23:15
   - Processing time: 230ms
   - Alternatives calculated: 3
   - Users notified: 47
```

## API Endpoint Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                     DISRUPTION API ENDPOINTS                     │
└─────────────────────────────────────────────────────────────────┘

FOR PYTHON DATA HANDLER:
  POST   /api/v1/disruptions/detect
         → Main entry point for disruption detection
         → Returns: DisruptionSolution (201) or No Content (204)

FOR DASHBOARD/MONITORING:
  GET    /api/v1/disruptions/active
         → Get all active disruptions
         
  GET    /api/v1/disruptions/severity/{level}
         → Filter by severity (LOW/MEDIUM/HIGH/CRITICAL)
         
  GET    /api/v1/disruptions/area/{area}
         → Filter by affected area
         
  GET    /api/v1/disruptions/{id}/logs
         → Get incident logs for a disruption

FOR MANAGEMENT:
  POST   /api/v1/disruptions/{id}/resolve
         → Mark disruption as resolved
         → Sends resolution notification to users

STANDARD CRUD:
  GET    /api/v1/disruptions
  GET    /api/v1/disruptions/{id}
  POST   /api/v1/disruptions
  PUT    /api/v1/disruptions/{id}
  DELETE /api/v1/disruptions/{id}
```

## Service Responsibilities Matrix

```
┌──────────────────────────────┬─────────────────────────────────────────┐
│ SERVICE                      │ RESPONSIBILITIES                         │
├──────────────────────────────┼─────────────────────────────────────────┤
│ ThresholdDetectionService    │ • Validate disruption meets criteria    │
│                              │ • Calculate severity levels              │
│                              │ • Determine urgency                      │
│                              │ • Estimate impact                        │
├──────────────────────────────┼─────────────────────────────────────────┤
│ AlternativeRoutingService    │ • Calculate alternative routes           │
│                              │ • Evaluate route quality                 │
│                              │ • Score and rank options                 │
│                              │ • Support multi-modal routing            │
├──────────────────────────────┼─────────────────────────────────────────┤
│ SolutionCompilationService   │ • Compile complete solutions             │
│                              │ • Prioritize alternatives                │
│                              │ • Generate user guidance                 │
│                              │ • Create step-by-step instructions       │
├──────────────────────────────┼─────────────────────────────────────────┤
│ NotificationCoordinationSvc  │ • Send to notification module            │
│                              │ • Format for different channels          │
│                              │ • Handle urgent notifications            │
│                              │ • Send updates and resolutions           │
├──────────────────────────────┼─────────────────────────────────────────┤
│ IncidentLoggingService       │ • Log all disruption events              │
│                              │ • Track performance metrics              │
│                              │ • Generate analytics reports             │
│                              │ • Support audit and optimization         │
└──────────────────────────────┴─────────────────────────────────────────┘
```
