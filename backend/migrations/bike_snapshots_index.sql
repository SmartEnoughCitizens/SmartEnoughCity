-- Speed up LATERAL latest-snapshot lookup in alternative transport query.
-- The LATERAL JOIN fetches the most recent snapshot per station (ORDER BY timestamp DESC LIMIT 1);
-- without this index Postgres does a full sequential scan of the snapshots table for each station.
CREATE INDEX IF NOT EXISTS idx_bike_snapshots_station_time
    ON external_data.dublin_bikes_station_snapshots (station_id, timestamp DESC);
