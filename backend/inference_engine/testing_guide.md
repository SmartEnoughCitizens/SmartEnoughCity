# 🧪 Simplified Testing Guide - Inference Engine

## Prerequisites
- Docker and Docker Compose installed
- Terminal/Command Prompt
- (Optional) Postman installed

---

## Quick Start with Docker

### Step 1: Navigate to Project Directory
```bash
cd /path/to/your/project/backend
```

### Step 2: Build and Start Services
```bash
# Build the inference engine
docker-compose build inference-engine

# Start the service
docker-compose up inference-engine

# OR run in detached mode (background)
docker-compose up -d inference-engine
```

### Step 3: Check Logs
```bash
# Watch logs in real-time
docker-compose logs -f inference-engine

# You should see:
# ✓ INFO:     Started server process
# ✓ INFO:     Uvicorn running on http://0.0.0.0:8000
```

---

## Testing the API

### Test 1: Root Endpoint (Verify Service is Running)
```bash
curl http://localhost:8000/
```

**Expected Response:**
```json
{
  "service": "Recommendation Engine API",
  "version": "1.0.0",
  "status": "running",
  "endpoints": {
    "generate_recommendation": "POST /recommendations/generate",
    "mock_data_engine": "GET /mock/data-engine?data_indicator=bus|car|train",
    "mock_notification": "POST /mock/notification"
  },
  "documentation": {
    "swagger_ui": "/docs",
    "redoc": "/redoc"
  }
}
```

---

### Test 2: Mock Data Engine (Test Different Transport Types)

#### Test with Bus Data
```bash
curl "http://localhost:8000/mock/data-engine?data_indicator=bus"
```

**Expected Response:**
```json
{
  "data": {
    "transport_type": "bus",
    "available_routes": ["Route 101", "Route 202", "Route 303"],
    "congestion_level": 0.6,
    "avg_time": 25,
    "stops_count": 12,
    "next_arrival": "5 minutes",
    "fare": "$2.50"
  },
  "metadata": {
    "timestamp": "2024-11-25T10:30:00.123456",
    "data_source": "Mock Data Analysis Engine",
    "version": "1.0"
  }
}
```

#### Test with Car Data
```bash
curl "http://localhost:8000/mock/data-engine?data_indicator=car"
```

**Expected Response:**
```json
{
  "data": {
    "transport_type": "car",
    "available_routes": ["Highway A", "Main Street", "Park Avenue"],
    "congestion_level": 0.8,
    "avg_time": 18,
    "parking_availability": "limited",
    "traffic_status": "heavy",
    "estimated_cost": "$5.00"
  },
  "metadata": {
    "timestamp": "2024-11-25T10:30:00.123456",
    "data_source": "Mock Data Analysis Engine",
    "version": "1.0"
  }
}
```

#### Test with Train Data
```bash
curl "http://localhost:8000/mock/data-engine?data_indicator=train"
```

**Expected Response:**
```json
{
  "data": {
    "transport_type": "train",
    "available_routes": ["Red Line", "Blue Line", "Green Line"],
    "congestion_level": 0.3,
    "avg_time": 15,
    "next_departure": "10 minutes",
    "platform": "Platform 3",
    "fare": "$3.75"
  },
  "metadata": {
    "timestamp": "2024-11-25T10:30:00.123456",
    "data_source": "Mock Data Analysis Engine",
    "version": "1.0"
  }
}
```

---

### Test 3: Generate Recommendation (Main API)

#### Scenario A: Bus Recommendation
```bash
curl -X POST http://localhost:8000/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "john_123",
    "context": {
      "data_indicator": "bus",
      "current_location": "downtown",
      "destination": "university"
    }
  }'
```

**Expected Response:**
```json
{
  "recommendation_id": "rec_john_123_1732532400",
  "status": "completed",
  "message": "Recommendation generated and notification sent",
  "timestamp": "2024-11-25T10:30:00.123456"
}
```

#### Scenario B: Car Recommendation
```bash
curl -X POST http://localhost:8000/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "alice_456",
    "context": {
      "data_indicator": "car",
      "current_location": "home",
      "destination": "office"
    }
  }'
```

#### Scenario C: Train Recommendation
```bash
curl -X POST http://localhost:8000/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "bob_789",
    "context": {
      "data_indicator": "train",
      "departure_time": "09:00 AM"
    }
  }'
```

#### Scenario D: Default (No data_indicator specified - defaults to bus)
```bash
curl -X POST http://localhost:8000/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "charlie_999",
    "context": {
      "current_location": "downtown"
    }
  }'
```

---

### Understanding the Logs

When you run a recommendation, watch the logs:

```bash
docker-compose logs -f inference-engine
```

**You'll see this workflow:**
```
🎯 Received recommendation request for user: john_123
🚀 Starting recommendation workflow for user: john_123
📝 Recommendation ID: rec_john_123_1732532400
📊 Step 1/3: Fetching bus data
📡 Fetching data from: http://inference-engine:8000/mock/data-engine
🧪 Mock Data Engine called for data_indicator: bus
✅ Successfully fetched bus data
🤖 Step 2/3: Generating recommendation for user john_123
🤖 Generating recommendations using ML model...
✅ Recommendations generated for bus
📎 Added context to recommendation
📧 Step 3/3: Sending notification for user john_123
📡 Sending notification to: http://inference-engine:8000/mock/notification
🧪 Mock Notification Handler called
📧 Notification would be sent to user: john_123
📋 Recommendation: {...}
✅ Notification sent for user john_123
✅ Recommendation workflow completed: rec_john_123_1732532400
```

---

## Testing with Postman

### Setup Collection

Create a Postman collection with these requests:

#### Request 1: Root (Check Service)
- **Method:** GET
- **URL:** `http://localhost:8000/`

#### Request 2: Mock Data Engine - Bus
- **Method:** GET
- **URL:** `http://localhost:8000/mock/data-engine?data_indicator=bus`

#### Request 3: Mock Data Engine - Car
- **Method:** GET
- **URL:** `http://localhost:8000/mock/data-engine?data_indicator=car`

#### Request 4: Mock Data Engine - Train
- **Method:** GET
- **URL:** `http://localhost:8000/mock/data-engine?data_indicator=train`

#### Request 5: Generate Bus Recommendation
- **Method:** POST
- **URL:** `http://localhost:8000/recommendations/generate`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "user_id": "postman_user_123",
  "context": {
    "data_indicator": "bus",
    "current_location": "downtown",
    "destination": "airport"
  }
}
```

#### Request 6: Generate Car Recommendation
- **Method:** POST
- **URL:** `http://localhost:8000/recommendations/generate`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "user_id": "postman_user_456",
  "context": {
    "data_indicator": "car",
    "current_location": "suburb",
    "destination": "city_center"
  }
}
```

#### Request 7: Generate Train Recommendation
- **Method:** POST
- **URL:** `http://localhost:8000/recommendations/generate`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "user_id": "postman_user_789",
  "context": {
    "data_indicator": "train",
    "departure_station": "Central",
    "arrival_station": "North"
  }
}
```

---

## Interactive API Documentation

### FastAPI Swagger UI
Open in browser: **http://localhost:8000/docs**

1. You'll see all available endpoints
2. Click on `POST /recommendations/generate`
3. Click "Try it out"
4. Enter request body:
```json
{
  "user_id": "swagger_test_user",
  "context": {
    "data_indicator": "bus"
  }
}
```
5. Click "Execute"
6. View the response below

### Alternative Documentation
Open in browser: **http://localhost:8000/redoc**

---

## Quick Test Script

Save this as `test_inference_engine.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8000"

echo "🧪 Testing Inference Engine API..."
echo ""

echo "1️⃣ Checking service status..."
curl -s $BASE_URL/ | jq '.'
echo ""

echo "2️⃣ Testing Mock Data Engine - Bus..."
curl -s "$BASE_URL/mock/data-engine?data_indicator=bus" | jq '.data'
echo ""

echo "3️⃣ Testing Mock Data Engine - Car..."
curl -s "$BASE_URL/mock/data-engine?data_indicator=car" | jq '.data'
echo ""

echo "4️⃣ Testing Mock Data Engine - Train..."
curl -s "$BASE_URL/mock/data-engine?data_indicator=train" | jq '.data'
echo ""

echo "5️⃣ Generating Bus Recommendation..."
curl -s -X POST $BASE_URL/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user_bus",
    "context": {
      "data_indicator": "bus",
      "location": "downtown"
    }
  }' | jq '.'
echo ""

echo "6️⃣ Generating Car Recommendation..."
curl -s -X POST $BASE_URL/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user_car",
    "context": {
      "data_indicator": "car",
      "location": "suburb"
    }
  }' | jq '.'
echo ""

echo "7️⃣ Generating Train Recommendation..."
curl -s -X POST $BASE_URL/recommendations/generate \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user_train",
    "context": {
      "data_indicator": "train",
      "station": "central"
    }
  }' | jq '.'
echo ""

echo "✅ All tests complete!"
```

**Run with:**
```bash
chmod +x test_inference_engine.sh
./test_inference_engine.sh
```

---

## Troubleshooting

### Issue 1: Port Already in Use
```bash
# Kill process on port 8000
lsof -ti:8000 | xargs kill -9

# OR change port in compose.yaml to 8001
```

### Issue 2: Container Won't Start
```bash
# Check logs
docker-compose logs inference-engine

# Rebuild
docker-compose build --no-cache inference-engine
docker-compose up inference-engine
```

### Issue 3: Can't Connect
```bash
# Verify service is running
docker-compose ps

# Check if container is healthy
docker ps

# Restart service
docker-compose restart inference-engine
```

---

## Stop Services

```bash
# Stop service
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## API Summary

| Endpoint | Method | Parameters | Purpose |
|----------|--------|-----------|---------|
| `/` | GET | - | Service info |
| `/recommendations/generate` | POST | user_id, context (with data_indicator) | Generate recommendation |
| `/mock/data-engine` | GET | data_indicator (bus/car/train) | Mock data source |
| `/mock/notification` | POST | payload | Mock notification |
| `/docs` | GET | - | Interactive API docs |

---

## Next Steps

1. ✅ Test all three transport types (bus, car, train)
2. ✅ Verify logs show complete workflow
3. ✅ Test with different user_ids and contexts
4. ⏭️ Replace mock endpoints with real APIs when ready
5. ⏭️ Integrate actual ML model
6. ⏭️ Add database persistence

---

**🎉 Your simplified API is ready for testing!**