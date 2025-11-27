from data_handler.train.traincontinuousdata import continuous_data_to_csv
from data_handler.train.trainstationdata import train_stations_to_csv, train_stations_to_db
from data_handler.settings.database_settings import get_db_settings
from data_handler.settings.data_sources_settings import get_data_sources_settings


def main():
    # Load settings from environment
    db_settings = get_db_settings()
    sources_settings = get_data_sources_settings()
    
    print("Hello from data-handler!")
    print(f"Connected to database: {db_settings.name} at {db_settings.host}:{db_settings.port}")
    print("\nData sources enabled:")
    print(f"  - Cycle data: {sources_settings.enable_cycle_data}")
    print(f"  - Car data: {sources_settings.enable_car_data}")
    print(f"  - Bus data: {sources_settings.enable_bus_data}")
    print(f"  - Train data: {sources_settings.enable_train_data}")
    print(f"  - Tram data: {sources_settings.enable_tram_data}")
    print(f"  - Construction data: {sources_settings.enable_construction_data}\n")
    
    # Process data sources based on enabled toggles
    if sources_settings.enable_train_data:
        print("Processing train data...")
        # train_stations_to_csv(r"irishrail_stations.csv")
        # continious_data_to_csv(r"irishrail_train_history.csv")
        train_stations_to_db()
    
    if sources_settings.enable_cycle_data:
        print("Processing cycle data...")
        # Add cycle data processing here
    
    if sources_settings.enable_car_data:
        print("Processing car data...")
        # Add car data processing here
    
    if sources_settings.enable_bus_data:
        print("Processing bus data...")
        # Add bus data processing here
    
    if sources_settings.enable_tram_data:
        print("Processing tram data...")
        # Add tram data processing here
    
    if sources_settings.enable_construction_data:
        print("Processing construction data...")
        # Add construction data processing here

if __name__ == "__main__":
    main()
