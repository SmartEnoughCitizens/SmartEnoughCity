        


CREATE TABLE IF NOT EXISTS external_data.train_stations (
    id SERIAL PRIMARY KEY,
    station_code TEXT,
    station_desc TEXT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    station_types TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS external_data.cycle_stations (
    station_id TEXT PRIMARY KEY,
    name TEXT,
    address TEXT,
    capacity INTEGER,
    num_bikes_available INTEGER,
    num_docks_available INTEGER,
    is_installed BOOLEAN,
    is_renting BOOLEAN,
    is_returning BOOLEAN,
    last_reported BIGINT,
    last_reported_dt TEXT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION
);

DROP TABLE external_data.gtfs_trip_updates;

CREATE TABLE external_data.bus_trip_updates (
    id BIGSERIAL PRIMARY KEY,
    entity_id TEXT,
    trip_id TEXT,
    route_id TEXT,
    start_time TEXT,
    start_date TEXT,
    stop_sequence TEXT,
    stop_id TEXT,
    arrival_delay BIGINT,
    departure_delay BIGINT,
    schedule_relationship TEXT,
    UNIQUE (entity_id, stop_sequence, stop_id)
);


