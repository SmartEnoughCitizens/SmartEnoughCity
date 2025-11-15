        


        CREATE TABLE IF NOT EXISTS external_data.train_stations (
            id SERIAL PRIMARY KEY,
            station_code TEXT,
            station_desc TEXT,
            lat DOUBLE PRECISION,
            lon DOUBLE PRECISION,
            station_types TEXT,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
        );

