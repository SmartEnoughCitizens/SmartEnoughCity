from data_handler.traincontinuousdata import continuous_data_to_csv
from data_handler.trainstationdata import train_stations_to_csv
from data_handler.trainstationdata import train_stations_to_db
from data_handler.cycle_handler import cycle_stations_to_db
from data_handler.bus_handler import trip_updates_to_db
from data_handler.luas_handler import luas_stops_to_db  
from data_handler.luas_handler import luas_forecasts_to_db

def main():
    print("Hello from data-handler!")
    #train_stations_to_csv(r"irishrail_stations.csv")
    #continious_data_to_csv(r"irishrail_train_history.csv")
    train_stations_to_db()
    cycle_stations_to_db()
    trip_updates_to_db()
    luas_stops_to_db()
    luas_forecasts_to_db()


if __name__ == "__main__":
    main()
