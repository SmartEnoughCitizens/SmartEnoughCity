# Dublin Bikes Data Handler Implementation Guide
## Data Source Mapping to Use Cases
**Project:** Dublin Bikes Management System  
**Date:** January 2026  
**Status:** Data Source Specification  
---
## Executive Summary
This document describes all required data sources, their schemas, and how they map to database tables needed to support your Dublin Bikes use cases. The system integrates real-time and historical bike availability data with cycle count metrics and geographic information.
**Data sources:** 3 primary sources  
**Database tables:** 8 core tables + 2 supporting  
**Update frequency:** Real-time (1 min) + historical aggregation (daily)
---
## Data Sources Overview
| Source | Type | Update Freq | Purpose | Use Cases |
|--------|------|-------------|---------|-----------|
| **JCDecaux API (Real-time)** | REST API / GBFS | 1 minute | Station availability snapshot | All use cases requiring live status |
| **Historical Station Data (CSV)** | CSV Downloads | Monthly archive | 60-90 day historical patterns | Demand analysis, trend detection, forecasting |
| **Dublin Cycle Counts (CSV)** | CSV Downloads | Annual + updates | General cycling intensity by location/hour | Validation, volume context, corridor identification |

---
## 1. DATA SOURCE: JCDecaux Dublin Bikes Real-Time API
### Source Details
**API Type:** GBFS (General Bikeshare Feed Specification) v3.0 + Legacy JCDecaux format  
**Endpoints:**
- GBFS Root: `https://api.cyclocity.fr/contracts/dublin/gbfs/gbfs.json`
- Station Status (real-time): `https://api.cyclocity.fr/contracts/dublin/gbfs/station_status.json`
- Station Information (static): `https://api.cyclocity.fr/contracts/dublin/gbfs/station_information.json`
**Authentication:** API key required (register at https://developer.jcdecaux.com/)  
**Contract name:** `dublin`  
**Format:** JSON  
**Latency:** ≤5 minutes  
**Update Interval:** Every minute
### Response Schema
#### station_status.json
**Response wrapper:**
```
{
  "last_updated": timestamp (RFC3339),
  "ttl": integer (0 for real-time data),
  "version": "3.0",
  "data": {
    "stations": [...]
  }
}
```
**Each station object contains:**
| Field | Type | Required | Maps To Table | Description |
|-------|------|----------|----------------|-------------|
| `station_id` | string (ID) | Yes | `station_snapshots.station_id` | Unique station identifier |
| `num_bikes_available` | integer | Yes | `station_snapshots.available_bikes` | Functional bikes ready for rental |
| `num_docks_available` | integer | Yes | `station_snapshots.available_docks` | Empty dock points for returns |
| `num_bikes_disabled` | integer | Optional | `station_snapshots.disabled_bikes` | Out-of-service bikes at station |
| `num_docks_disabled` | integer | Optional | `station_snapshots.disabled_docks` | Out-of-service docks |
| `is_installed` | boolean | Yes | `station_snapshots.is_installed` | Equipment physically installed? |
| `is_renting` | boolean | Yes | `station_snapshots.is_renting` | Rentals currently enabled? |
| `is_returning` | boolean | Yes | `station_snapshots.is_returning` | Returns currently enabled? |
| `last_reported` | timestamp | Yes | `station_snapshots.last_reported` | Station's last status report time |

**Example station record:**
```json
{
  "station_id": "1",
  "num_bikes_available": 8,
  "num_docks_available": 2,
  "num_bikes_disabled": 0,
  "num_docks_disabled": 0,
  "is_installed": true,
  "is_renting": true,
  "is_returning": true,
  "last_reported": "2026-01-22T17:30:00+00:00"
}
```
#### station_information.json
**Each station information record contains:**
| Field | Type | Required | Maps To Table | Description |
|-------|------|----------|----------------|-------------|
| `station_id` | string (ID) | Yes | `stations.station_id` | Unique identifier (PRIMARY KEY) |
| `name` | string | Yes | `stations.name` | Station display name |
| `short_name` | string | Optional | `stations.short_name` | Abbreviated station name |
| `address` | string | Optional | `stations.address` | Station street address |
| `lat` | float | Yes | `stations.latitude` | WGS84 latitude (6 decimal places) |
| `lon` | float | Yes | `stations.longitude` | WGS84 longitude (6 decimal places) |
| `capacity` | integer | Yes | `stations.capacity` | Total dock stands available |
| `region_id` | string | Optional | `stations.region_id` | Geographic region identifier |

**Example station information record:**
```json
{
  "station_id": "1",
  "name": "Mary Street",
  "short_name": "001",
  "address": "Mary Street, Dublin 1",
  "lat": 53.349316,
  "lon": -6.262876,
  "capacity": 30,
  "region_id": "dublin_central"
}
```
---
## 2. DATA SOURCE: Dublin Bikes Historical Data (CSV Archives)
### Source Details
**URL Pattern:** `https://data.smartdublin.ie/dataset/33ec9fe2-4957-4e9a-ab55-c5e917c7a9ab/resource/[resource_id]/download/dublin-bikes_station_status_[YYYYMM].csv`
**Latest Archive (July 2025):** `https://data.smartdublin.ie/dataset/33ec9fe2-4957-4e9a-ab55-c5e917c7a9ab/resource/5b5afbcc-61e0-48d0-870e-a22df2ea4793/download/dublin-bikes_station_status_072025.csv`
**Available:** Monthly archives 2023-present  
**Format:** CSV  
**Frequency:** Updated monthly (last day of month)  
**Size:** ~20-80 MB per month (~600k rows)
### CSV Schema
**Column names (case-sensitive in GBFS format, mapped from legacy format):**
| CSV Column | Type | Example | Maps To Table | Notes |
|-----------|------|---------|----------------|-------|
| `system_id` | text | "dublin" | `station_history.system_id` | Always "dublin" |
| `last_reported` | timestamp | "2025-07-15T14:30:00+00:00" | `station_history.timestamp` | When station sent this status |
| `station_id` | integer | 1, 2, 3... | `station_history.station_id` (FK) | Links to `stations` table |
| `num_bikes_available` | integer | 8 | `station_history.available_bikes` | Bikes ready for rental |
| `num_docks_available` | integer | 2 | `station_history.available_docks` | Empty docks for returns |
| `is_installed` | text | "true" / "false" | `station_history.is_installed` | Note: string in CSV, convert to boolean |
| `is_renting` | text | "true" / "false" | `station_history.is_renting` | String in CSV, convert to boolean |
| `is_returning` | text | "true" / "false" | `station_history.is_returning` | String in CSV, convert to boolean |
| `name` | text | "Mary Street" | *(ignored, use `stations.name`)* | Station name (redundant) |
| `short_name` | text | "001" | *(ignored, use `stations.short_name`)* | Short name (redundant) |
| `address` | text | "Mary Street, Dublin 1" | *(ignored, use `stations.address`)* | Address (redundant) |
| `lat` | float | 53.349316 | *(ignored, use `stations.latitude`)* | Latitude (redundant) |
| `lon` | float | -6.262876 | *(ignored, use `stations.longitude`)* | Longitude (redundant) |
| `region_id` | text | "dublin_central" | *(ignored, use `stations.region_id`)* | Region (redundant) |
| `capacity` | integer | 30 | *(ignored, use `stations.capacity`)* | Capacity (redundant) |

**Sample CSV rows:**
```csv
system_id,last_reported,station_id,num_bikes_available,num_docks_available,is_installed,is_renting,is_returning,name,short_name,address,lat,lon,region_id,capacity
dublin,2025-07-15T14:30:00+00:00,1,8,2,true,true,true,Mary Street,001,Mary Street Dublin 1,53.349316,-6.262876,dublin_central,30
dublin,2025-07-15T14:35:00+00:00,2,5,10,true,true,true,Stoneybatter,002,Stoneybatter Dublin 7,53.356,-6.289,dublin_north,15
dublin,2025-07-15T14:40:00+00:00,1,9,1,true,true,true,Mary Street,001,Mary Street Dublin 1,53.349316,-6.262876,dublin_central,30
```
---
## 3. DATA SOURCE: Dublin City Cycle Counts
### Source Details
**Base URL:** `https://data.smartdublin.ie/dataset/dublin-city-centre-cycle-counts`  
**Data CSV:** Retrieved via manual download or API query  
**Format:** CSV (annual + monthly partitions)  
**Frequency:** Annual updates + periodic refresh  
**Coverage:** 8+ fixed counter locations across Dublin city centre
### CSV Schema
**Counter Locations File** (`Dublin City Cycle Counter Locations`):
| Column | Type | Example | Maps To Table | Notes |
|--------|------|---------|----------------|-------|
| `location_id` | text | "north_strand_nb" | `cycle_counters.location_id` | Unique counter identifier |
| `location_name` | text | "North Strand Road (North-bound)" | `cycle_counters.name` | Display name with direction |
| `latitude` | float | 53.350 | `cycle_counters.latitude` | WGS84 latitude |
| `longitude` | float | -6.250 | `cycle_counters.longitude` | WGS84 longitude |
| `road_name` | text | "North Strand Road" | `cycle_counters.road_name` | Primary road identifier |
| `direction` | text | "NB" / "SB" / "EB" / "WB" | `cycle_counters.direction` | Traffic direction |

**Cycle Counts Data File** (`[Year] Cycle Counts Dublin City`):
| Column | Type | Example | Maps To Table | Notes |
|--------|------|---------|----------------|-------|
| `location_id` | text | "north_strand_nb" | `cycle_counts.location_id` (FK) | Links to `cycle_counters` |
| `date` | date | "2025-07-15" | `cycle_counts.date` | Date of count |
| `hour` | integer | 0-23 | `cycle_counts.hour` | Hour of day (00:00-23:00) |
| `bike_count` | integer | 127 | `cycle_counts.count` | Number of cyclists in that hour |

**Sample counter locations:**
```csv
location_id,location_name,latitude,longitude,road_name,direction
north_strand_nb,North Strand Road (North-bound),53.35043,-6.25098,North Strand Road,NB
south_circular_rd_sb,South Circular Road (South-bound),53.33405,-6.28641,South Circular Road,SB
drumcondra_rd_eb,Drumcondra Road (East-bound),53.36155,-6.24850,Drumcondra Road,EB
```
**Sample cycle count data:**
```csv
location_id,date,hour,bike_count
north_strand_nb,2025-07-15,8,142
north_strand_nb,2025-07-15,9,89
north_strand_nb,2025-07-15,17,156
south_circular_rd_sb,2025-07-15,8,67
south_circular_rd_sb,2025-07-15,9,51
```
---
## Database Schema Specification
### Table 1: `stations` (Static Station Metadata)
**Purpose:** Master reference for all Dublin Bikes stations  
**Source:** JCDecaux station_information.json  
**Update Frequency:** Monthly (as new stations added/removed)  
**Cardinality:** ~110 stations
```sql
CREATE TABLE stations (
  station_id INTEGER PRIMARY KEY,
  system_id TEXT NOT NULL DEFAULT 'dublin',
  name TEXT NOT NULL,
  short_name TEXT,
  address TEXT,
  latitude DECIMAL(10, 6) NOT NULL,
  longitude DECIMAL(10, 6) NOT NULL,
  capacity INTEGER NOT NULL,
  region_id TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**Used by use cases:** All  
**Sample row:**
```
station_id=1 | name="Mary Street" | latitude=53.349316 | longitude=-6.262876 | capacity=30
```
---
### Table 2: `station_snapshots` (Real-Time Station Status)
**Purpose:** Latest real-time availability at each station  
**Source:** JCDecaux station_status.json (API, 1-minute refresh)  
**Update Frequency:** Every 1 minute  
**Cardinality:** ~110 rows (replaced entirely each poll)
```sql
CREATE TABLE station_snapshots (
  id BIGSERIAL PRIMARY KEY,
  station_id INTEGER NOT NULL REFERENCES stations(station_id),
  timestamp TIMESTAMP NOT NULL,
  last_reported TIMESTAMP NOT NULL,
  available_bikes INTEGER NOT NULL,
  available_docks INTEGER NOT NULL,
  disabled_bikes INTEGER DEFAULT 0,
  disabled_docks INTEGER DEFAULT 0,
  is_installed BOOLEAN NOT NULL,
  is_renting BOOLEAN NOT NULL,
  is_returning BOOLEAN NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_station_timestamp (station_id, timestamp DESC)
);
```
**Used by use cases:**
- Display Cycle Stations and Usage (live availability)
- Analyze station (nearly) full/empty times (current state)
- Detect low-access areas (live vs historical)
**Sample row:**
```
station_id=1 | timestamp=2026-01-22T17:30:00Z | available_bikes=8 | available_docks=2 | is_renting=true | is_returning=true
```
---
### Table 3: `station_history` (Historical Station Status)
**Purpose:** Archive of station snapshots for trend analysis  
**Source:** CSV archives (monthly downloads)  
**Update Frequency:** Daily batch import of monthly CSV (or continuous append from API)  
**Cardinality:** ~60 million rows (110 stations × ~15,000 snapshots/month × 36 months)
```sql
CREATE TABLE station_history (
  id BIGSERIAL PRIMARY KEY,
  station_id INTEGER NOT NULL REFERENCES stations(station_id),
  timestamp TIMESTAMP NOT NULL,
  last_reported TIMESTAMP NOT NULL,
  available_bikes INTEGER NOT NULL,
  available_docks INTEGER NOT NULL,
  is_installed BOOLEAN NOT NULL,
  is_renting BOOLEAN NOT NULL,
  is_returning BOOLEAN NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_station_history_time (station_id, timestamp DESC),
  INDEX idx_station_history_date (DATE(timestamp)),
  CONSTRAINT unique_station_time UNIQUE(station_id, timestamp)
);
```
**Used by use cases:**
- Demand Analysis: 60-90 day demand metrics
- Analyze station (nearly) full/empty times: historical patterns
- Origin-Destination Heatmaps: flow inference from availability changes
- Detect low-access areas: persistent underutilization
**Sample rows:**
```
station_id=1, timestamp=2025-07-15T08:00:00Z, available_bikes=8, available_docks=2
station_id=1, timestamp=2025-07-15T08:05:00Z, available_bikes=6, available_docks=4
station_id=1, timestamp=2025-07-15T08:10:00Z, available_bikes=5, available_docks=5
```
---
### Table 4: `station_demand_metrics` (Pre-computed Demand Summaries)
**Purpose:** Cache aggregated demand metrics for dashboard performance  
**Source:** Computed from `station_history` (daily batch)  
**Update Frequency:** Daily (end-of-day)  
**Cardinality:** ~110 rows per day
```sql
CREATE TABLE station_demand_metrics (
  id BIGSERIAL PRIMARY KEY,
  station_id INTEGER NOT NULL REFERENCES stations(station_id),
  metric_date DATE NOT NULL,
  hour_of_day INTEGER,  -- NULL = daily aggregate, 0-23 = hourly
  avg_bikes_available DECIMAL(10, 2),
  avg_docks_available DECIMAL(10, 2),
  max_bikes DECIMAL(10, 2),
  min_bikes DECIMAL(10, 2),
  occupancy_rate DECIMAL(5, 2),  -- percent
  checkout_rate DECIMAL(10, 2),  -- inferred from diffs
  return_rate DECIMAL(10, 2),    -- inferred from diffs
  stockout_events INTEGER,       -- count of times bikes=0
  fullness_events INTEGER,       -- count of times docks=0
  peak_checkout_hour INTEGER,
  peak_return_hour INTEGER,
  demand_type VARCHAR(50),       -- 'high', 'medium', 'low', 'commute', 'tourist', 'residential'
  growth_rate_mom DECIMAL(10, 2),  -- month-over-month % change
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_demand_station_date (station_id, metric_date DESC),
  CONSTRAINT unique_station_metric UNIQUE(station_id, metric_date, hour_of_day)
);
```
**Used by use cases:**
- Demand Analysis: demand profiles by station
- Detect low-access areas: identify sustained underutilization
- Simulate changes: baseline metrics for scenario comparison
- Display recommendations: growth alerts
**Sample row:**
```
station_id=1 | metric_date=2026-01-22 | hour_of_day=8 | avg_bikes_available=4.5 | occupancy_rate=85 | demand_type='commute' | growth_rate_mom=12.5
```
---
### Table 5: `origin_destination_flows` (Inferred Trip Patterns)
**Purpose:** Station-pair demand patterns inferred from availability changes  
**Source:** Computed from `station_history` (daily aggregation)  
**Update Frequency:** Daily batch  
**Cardinality:** ~5,000-10,000 OD pairs (depending on frequency threshold)
```sql
CREATE TABLE origin_destination_flows (
  id BIGSERIAL PRIMARY KEY,
  origin_station_id INTEGER NOT NULL REFERENCES stations(station_id),
  destination_station_id INTEGER NOT NULL REFERENCES stations(station_id),
  metric_date DATE NOT NULL,
  hour_of_day INTEGER,  -- NULL = daily aggregate, 0-23 = hourly
  inferred_trips INTEGER,  -- count of inferred trips A->B
  confidence_score DECIMAL(5, 2),  -- 0-100% confidence in inference
  distance_km DECIMAL(10, 2),  -- great-circle distance
  avg_time_minutes INTEGER,  -- estimated travel time
  trip_rate_per_hour DECIMAL(10, 2),
  frequency_category VARCHAR(50),  -- 'frequent', 'moderate', 'rare'
  day_of_week INTEGER,  -- 0-6 (Mon-Sun)
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_origin_dest_date (origin_station_id, destination_station_id, metric_date DESC),
  INDEX idx_destination_date (destination_station_id, metric_date DESC),
  CONSTRAINT unique_od_pair UNIQUE(origin_station_id, destination_station_id, metric_date, hour_of_day, day_of_week)
);
```
**Used by use cases:**
- Origin-Destination Heatmaps: populate route heatmap
- Detect rebalancing opportunities: imbalanced corridors
- Simulate changes: forecast impact on top OD pairs
**Sample rows:**
```
origin_station_id=1 | destination_station_id=5 | metric_date=2026-01-22 | hour_of_day=8 | inferred_trips=12 | frequency_category='frequent'
origin_station_id=5 | destination_station_id=1 | metric_date=2026-01-22 | hour_of_day=17 | inferred_trips=15 | frequency_category='frequent'
```
---
### Table 6: `cycle_counters` (Fixed Counter Locations)
**Purpose:** Master reference for fixed bicycle counters  
**Source:** Dublin City Cycle Counts CSV  
**Update Frequency:** As new counters installed  
**Cardinality:** ~8-12 counter locations
```sql
CREATE TABLE cycle_counters (
  location_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,  -- e.g., "North Strand Road (North-bound)"
  latitude DECIMAL(10, 6) NOT NULL,
  longitude DECIMAL(10, 6) NOT NULL,
  road_name TEXT,
  direction VARCHAR(2),  -- NB, SB, EB, WB
  nearest_station_id INTEGER REFERENCES stations(station_id),
  distance_to_station_km DECIMAL(10, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**Used by use cases:**
- Detect low-access areas: volume context for underserved zones
- Demand Analysis: validate inferred demand
- Display heatmaps: overlay general cycling intensity
**Sample row:**
```
location_id='north_strand_nb' | name='North Strand Road (North-bound)' | latitude=53.35043 | longitude=-6.25098 | direction='NB'
```
---
### Table 7: `cycle_counts` (Hourly Cycle Volume)
**Purpose:** Historical hourly bicycle counts from fixed counters  
**Source:** Dublin City Cycle Counts CSV  
**Update Frequency:** Annually (with ad-hoc updates)  
**Cardinality:** ~70k-100k rows per year per counter (~1M total)
```sql
CREATE TABLE cycle_counts (
  id BIGSERIAL PRIMARY KEY,
  location_id TEXT NOT NULL REFERENCES cycle_counters(location_id),
  date DATE NOT NULL,
  hour INTEGER NOT NULL CHECK (hour >= 0 AND hour <= 23),
  bike_count INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_location_datetime (location_id, date, hour),
  INDEX idx_datetime (date, hour),
  CONSTRAINT unique_location_time UNIQUE(location_id, date, hour)
);
```
**Used by use cases:**
- Demand Analysis: volume context for validation
- Detect low-access areas: footfall pattern analysis
- Simulate changes: external demand signals
**Sample rows:**
```
location_id='north_strand_nb' | date=2025-07-15 | hour=8 | bike_count=142
location_id='north_strand_nb' | date=2025-07-15 | hour=17 | bike_count=156
```
---
### Table 8: `demand_analysis_segments` (Station Demand Classifications)
**Purpose:** Classification of stations by demand pattern  
**Source:** Computed from `station_demand_metrics` (weekly batch)  
**Update Frequency:** Weekly  
**Cardinality:** ~110 rows
```sql
CREATE TABLE demand_analysis_segments (
  id BIGSERIAL PRIMARY KEY,
  station_id INTEGER NOT NULL UNIQUE REFERENCES stations(station_id),
  segment_type VARCHAR(50) NOT NULL,  -- 'high_demand', 'commute', 'tourist', 'residential', 'low_demand', 'mixed'
  confidence_score DECIMAL(5, 2),  -- 0-100
  peak_checkout_hour INTEGER,
  peak_return_hour INTEGER,
  avg_daily_trips INTEGER,
  growth_trend VARCHAR(50),  -- 'growing', 'declining', 'stable'
  underutilization_score DECIMAL(5, 2),  -- 0-100 (higher = more underused)
  last_analyzed DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**Used by use cases:**
- Demand Analysis: identify commute vs tourist vs residential patterns
- Detect low-access areas: mark underutilized stations
- Recommendations: data-driven priority for rebalancing/expansion
**Sample rows:**
```
station_id=1 | segment_type='commute' | growth_trend='growing' | avg_daily_trips=250
station_id=45 | segment_type='residential' | segment_type='low_demand' | underutilization_score=78
```
---
## Supporting Tables (Derived/Operational)
### Table 9: `recommendations` (Generated System Recommendations)
**Purpose:** Audit trail of system-generated recommendations  
**Scope:** Add/remove bikes, add/remove stations, rebalancing suggestions  
```sql
CREATE TABLE recommendations (
  id BIGSERIAL PRIMARY KEY,
  station_id INTEGER REFERENCES stations(station_id),
  recommendation_type VARCHAR(100),  -- 'add_bikes', 'remove_bikes', 'add_station', 'remove_station', 'rebalance'
  priority VARCHAR(50),  -- 'critical', 'high', 'medium', 'low'
  reason TEXT,
  suggested_action TEXT,
  metric_basis JSONB,  -- supporting metrics that triggered recommendation
  status VARCHAR(50) DEFAULT 'pending',  -- 'pending', 'approved', 'executed', 'rejected'
  approved_at TIMESTAMP,
  executed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_status_created (status, created_at DESC)
);
```
---
### Table 10: `simulation_scenarios` (What-if Analysis Results)
**Purpose:** Cache simulation results for scenario comparison  
**Scope:** Impact of bike/station changes on demand
```sql
CREATE TABLE simulation_scenarios (
  id BIGSERIAL PRIMARY KEY,
  scenario_name TEXT NOT NULL,
  description TEXT,
  base_scenario_id INTEGER REFERENCES simulation_scenarios(id),
  changes JSONB,  -- {"station_1": {"add_bikes": 10}, "station_45": {"remove_station": true}}
  projected_availability_change DECIMAL(10, 2),  -- avg % change
  projected_demand_impact DECIMAL(10, 2),  -- estimated % change in trip volume
  affected_stations INTEGER[],
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(100)
);
```
---
## Data Ingestion Pipelines
### Pipeline 1: Real-Time API Ingestion (Every 1 minute)
```
JCDecaux API (station_status.json)
    ↓
Parse JSON response
    ↓
INSERT INTO station_snapshots (with upsert/replace)
    ↓
UPDATE station_demand_metrics (if hour has changed, queue aggregation)
    ↓
TRIGGER infer_od_flows() if significant availability changes detected
```
**Implementation:** Python script with REST client, run on scheduler every 60 seconds
---
### Pipeline 2: Historical Data Import (Daily or On-Demand)
```
Download monthly CSV from data.smartdublin.ie (if new month available)
    ↓
Validate CSV schema (detect format changes)
    ↓
Transform: convert string booleans to actual booleans, parse timestamps
    ↓
BULK INSERT into station_history (skip duplicates if re-running)
    ↓
Aggregate: INSERT/UPDATE station_demand_metrics for past 7 days
    ↓
Compute: UPDATE demand_analysis_segments
    ↓
Detect: INSERT new recommendations if thresholds triggered
```
**Implementation:** Python Pandas script, scheduled daily at 02:00 UTC
---
### Pipeline 3: Cycle Count Data Import (Monthly/As-Available)
```
Download cycle counts CSV from data.gov.ie / data.smartdublin.ie
    ↓
Validate schema (location_id, date, hour, bike_count)
    ↓
INSERT INTO cycle_counts (bulk insert, skip if exists)
    ↓
Validate: Compare with station_history for outliers/gaps
    ↓
Log: Record data import metadata (source, rows, date range)
```
**Implementation:** Python script, on-demand trigger when new data published
---
## Use Case to Data Source Mapping Matrix
| Use Case | Required Tables | Data Sources | Update Freq | Latency |
|----------|-----------------|--------------|-------------|---------|
| Display Cycle Stations & Usage | `stations`, `station_snapshots` | JCDecaux API (RT) | 1 min | <2 min |
| Demand Analysis (60-90 days) | `stations`, `station_history`, `station_demand_metrics`, `demand_analysis_segments` | CSV archives + API | Daily agg | <24 hrs |
| Analyze station full/empty times | `station_history`, `station_demand_metrics` | CSV archives + API | Daily agg | <24 hrs |
| Forecast upcoming risk windows | `station_history`, `station_demand_metrics` | CSV archives + API | Daily agg | <24 hrs |
| Detect low-access areas | `stations`, `station_history`, `cycle_counters`, `cycle_counts`, `demand_analysis_segments` | CSV + cycle counts CSV | Daily agg | <24 hrs |
| Origin-Destination Heatmaps | `stations`, `origin_destination_flows` | Inferred from `station_history` | Daily agg | <24 hrs |
| Display recommendations | `stations`, `station_demand_metrics`, `recommendations`, `cycle_counters` | All above | Daily agg | <24 hrs |
| Simulate scenario impacts | `stations`, `station_demand_metrics`, `origin_destination_flows`, `simulation_scenarios` | Historical data | On-demand | Real-time |

---
## Data Quality & Validation Rules
### API Response Validation
```python
def validate_station_status(record):
    assert record['station_id'] > 0, "Invalid station ID"
    assert record['available_bikes'] >= 0, "Negative bike count"
    assert record['available_docks'] >= 0, "Negative dock count"
    assert record['available_bikes'] <= station['capacity'], "Bikes exceed capacity"
    assert isinstance(record['is_installed'], bool), "is_installed must be boolean"
    assert record['last_reported'] is not None, "last_reported required"
    return True
```
### CSV Import Validation
```python
def validate_csv_row(row):
    assert pd.notna(row['station_id']), "station_id is null"
    assert pd.notna(row['num_bikes_available']), "num_bikes_available is null"
    assert row['num_bikes_available'] >= 0, "Negative bike count"
    assert row['is_installed'] in ['true', 'false'], "is_installed invalid"
    assert pd.notna(row['last_reported']), "last_reported is null"
    return True
```
### OD Flow Inference Rules
```python
def infer_od_pairs(station_history_window):
    # For each 5-10 minute window:
    # If station_A.bikes drops by N and station_B.docks drops by N (within 10 min)
    # Record as inferred trip with confidence based on proximity and probability
    # Filter: only count if confidence > 60% or geographic distance < 500m
    confidence = calculate_proximity_probability(station_a, station_b, time_delta)
    if confidence > 0.6:
        insert_flow(origin=a, dest=b, trips=n, confidence=confidence)
```
---
## Data Governance
### Data Ownership
| Table | Owner | Update Authority | Access Level |
|-------|-------|------------------|--------------|
| `stations` | JCDecaux (via API) | Sync monthly | Public |
| `station_snapshots` | JCDecaux API | Automated every 1 min | Public |
| `station_history` | JCDecaux (archived) | Automated daily batch | Public |
| `station_demand_metrics` | System (computed) | Automated daily | Internal |
| `origin_destination_flows` | System (inferred) | Automated daily | Internal |
| `cycle_counters` | DCC / Smart Dublin | Manual sync | Public |
| `cycle_counts` | DCC / Smart Dublin | Manual sync (annual) | Public |
| `demand_analysis_segments` | System (computed) | Automated weekly | Internal |
| `recommendations` | System + City Manager | System triggers, manager approves | Internal |
| `simulation_scenarios` | City Manager | Manual creation | Internal |

### Data Retention
- **`station_snapshots`**: Keep 30 days (rolling window for real-time analysis)
- **`station_history`**: Keep 36+ months for trend analysis and ML features
- **`recommendations`**: Keep 2 years for audit trail
- **`simulation_scenarios`**: Keep indefinitely (historical what-if analysis)
### Privacy & Compliance
- All data is **aggregate** (per station, per hour) — no individual user/trip tracking
- Comply with GDPR and Irish Data Protection Act (no personal data)
- License: Dublin Bikes data is CC-BY (Creative Commons Attribution)
---
## Implementation Checklist
- [ ] Set up database schema (create all 10 tables with indexes)
- [ ] Implement JCDecaux API authentication (obtain API key)
- [ ] Build real-time data ingestion pipeline (1-min interval API polling)
- [ ] Build CSV import pipeline (monthly archive download & ingestion)
- [ ] Build cycle count CSV import (annual/as-needed)
- [ ] Implement data validation & error handling
- [ ] Build aggregation jobs (daily demand metrics, weekly segmentation)
- [ ] Build OD flow inference engine
- [ ] Build recommendation trigger logic
- [ ] Build dashboard datasource queries
- [ ] Set up monitoring & alerting (API failures, data gaps, quality)
- [ ] Document API schemas & changes (GBFS version tracking)
- [ ] Plan data backup strategy (nightly snapshots)
---
## Appendix: Mapping Examples
### Example 1: "Display Cycle Stations and Usage"
**Data flow:**
```
JCDecaux API station_status.json
    → station_snapshots table (current state)
    + stations table (metadata)
    → Dashboard query: SELECT stations.*, station_snapshots.* WHERE timestamp = NOW()
```
### Example 2: "Demand Analysis of Cycle Stations"
**Data flow:**
```
JCDecaux CSV archives (last 90 days)
    → station_history table (hourly snapshots)
    → Aggregate hourly/daily/weekly into station_demand_metrics
    → Classify via demand_analysis_segments
    → Dashboard: hourly demand curves, peak hours, growth rates, segment labels
```
### Example 3: "Origin-Destination Heatmap"
**Data flow:**
```
station_history (daily snapshots with 5-min intervals)
    → Infer A→B pairs (when A.bikes↓ and B.docks↓ near-simultaneous)
    → origin_destination_flows table (frequency, confidence, distance)
    → Dashboard: draw weighted lines between stations, shade by traffic
```
### Example 4: "Detect Low-Access Areas"
**Data flow:**
```
station_history (90-day window)
    + cycle_counts (hourly volumes)
    + stations (lat/lon)
    → Compute coverage gaps (>800m from nearest station)
    + Compute availability risk (<20% bikes for 40% of peak hours)
    → demand_analysis_segments marks low_demand
    + cycle_counts shows high footfall nearby
    → Recommendation: "Add station near North Strand (high foot traffic, 1km from Mary Street)"
```
---
**Document Version:** 1.0  
**Last Updated:** January 26, 2026  
**Next Review:** February 26, 2026
 