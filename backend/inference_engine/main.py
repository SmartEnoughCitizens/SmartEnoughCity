import time 
import os 
from dotenv import load_dotenv
from db_connection import get_db_connection, test_connection
from recommender import generate_recommendations
import requests

load_dotenv() 

## Fetching the data from the database.
def fetch_data():
    """"Fetches the required data from the database for generating recommendations."""
    conn = get_db_connection()
    cur = conn.cursor()

    schema = os.getenv('DB_SCHEMA', 'external_data')
    cur.execute("SELECT first_name, last_name FROM external_data.users WHERE is_active = true;")
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return rows 

# Function to send recommendation via an API
def send_recommendation_api(first_name, last_name, recommendations):
    """Send the generated recommendations to the API."""
    api_url = os.getenv('RECOMMENDATION_API_URL')  # The API URL should be set in your .env
    payload = {
        'first_name': first_name,
        'last_name': last_name,
        'recommendations': recommendations
    }

    try:
        response = requests.post(api_url, json=payload)
        if response.status_code == 200:
            print(f"Successfully sent recommendations for {first_name} {last_name}")
        else:
            print(f"Failed to send recommendations for {first_name} {last_name}. Status Code: {response.status_code}")
    except Exception as e:
        print(f"Error sending recommendation to API: {e}")



def process_recommendations():
    """Processes each first name and last name and generates the recommendations."""
    print("Recommendation Engine Started...")
    # Test connection first
    if not test_connection():
        print("❌ Cannot connect to database. Exiting...")
        return 
    
    while True:
        try:
            data_rows = fetch_data()
            for row in data_rows:
                first_name = row['first_name']
                last_name = row['last_name']
                recommendations = generate_recommendations(first_name, last_name)
                print(f"Recommendations for {first_name} {last_name}: {recommendations}")
                # Send recommendations to the API
                send_recommendation_api(first_name, last_name, recommendations)
            
            print("Recommendation created for the users")
            time.sleep(20)
        except Exception as e:
            print(f"Error in processing loop: {e}")
            time.sleep(15)

if __name__ == "__main__":
    process_recommendations()

            
