from data_handler.traincontinuousdata import continuous_data_to_csv
from data_handler.trainstationdata import train_stations_to_csv
from data_handler.trainstationdata import train_stations_to_db
from data_handler.cycle_handler import cycle_stations_to_db
from data_handler.bus_handler import trip_updates_to_db
#from data_handler.bus_handler import fetch_gtfs_trip_updates_raw


def main():
    print("Hello from data-handler!")
    #train_stations_to_csv(r"irishrail_stations.csv")
    #continious_data_to_csv(r"irishrail_train_history.csv")
    train_stations_to_db()
    cycle_stations_to_db()
    #fetch_gtfs_trip_updates_raw()
    trip_updates_to_db()


if __name__ == "__main__":
    main()
