from backend.data_handler.src.data_handler.traincontinuousdata import continuous_data_to_csv
from data_handler.trainstationdata import train_stations_to_csv
from data_handler.trainstationdata import train_stations_to_db



def main():
    print("Hello from data-handler!")
    #train_stations_to_csv(r"irishrail_stations.csv")
    #continious_data_to_csv(r"irishrail_train_history.csv")
    train_stations_to_db()

if __name__ == "__main__":
    main()
