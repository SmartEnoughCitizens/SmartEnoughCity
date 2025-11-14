-- Create a sample table in external_data schema
CREATE TABLE IF NOT EXISTS external_data.users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(200),
    role VARCHAR(50),
    installed_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Insert dummy data
INSERT INTO external_data.users (first_name, last_name, role, is_active) VALUES
    ('Mukul', 'Khare', 'Backend', true),
    ('Rakesh', 'Laksman', 'Backend', true),
    ('Nadine', 'Dinchen', 'Data Handler', true),
    ('John', 'Ionn', 'Mobile App', false),
    ('Mihail', 'S', 'Database Creator', true);