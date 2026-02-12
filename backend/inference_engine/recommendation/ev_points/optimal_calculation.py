import pandas as pd
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# --- 0. Read data ---
ev_stations = pd.read_csv(
    os.path.join(BASE_DIR, "data", "ESB-EV-charge-point-locations.csv"),
    encoding='latin1'
)

households = pd.read_csv(
    os.path.join(BASE_DIR, "data", "Household_data.csv"),
    encoding='utf-8-sig'
)

car_counts = pd.read_csv(
    os.path.join(BASE_DIR, "data", "combined_scats_with_locations.csv"),
    encoding='utf-8-sig'
)

# --- Prepare household data ---
households.columns = households.columns.str.strip()
households = households[
    (households['Statistic Label'] == 'Private households') &
    (households['Type of Accommodation'] != 'Caravan/Mobile home')
]

# Extract region from "CSO Electoral Divisions 2022"
households['Region'] = households['CSO Electoral Divisions 2022'].str.split(',').str[0]

# --- 1. Average car counts per region from SCATS ---
regional_monthly = (
    car_counts
    .groupby(['Final_Region', 'Year', 'Month'])['Sum_Volume']
    .sum()
    .reset_index()
)

regional_car_counts = (
    regional_monthly
    .groupby('Final_Region')['Sum_Volume']
    .mean()  # Average monthly car counts per region
    .reset_index(name='Avg_Car_Count')
)

print("\nAverage car counts per region:\n", regional_car_counts)

# --- 2. Household proportion per region ---
total_households = households['VALUE'].sum()
households['Household_Proportion'] = households['VALUE'] / total_households

# --- 3. Distribute EVs based on car counts and household proportions ---
EV_ADOPTION_RATE = 0.10  # assume 10% of cars are EVs
households = households.merge(
    regional_car_counts,
    left_on='Region',
    right_on='Final_Region',
    how='left'
)

# Estimated number of EVs in each household cluster
households['Estimated_EVs'] = households['Household_Proportion'] * households['Avg_Car_Count'] * EV_ADOPTION_RATE

# --- 4. Calculate charging station needs ---
STATIONS_PER_EV = 50  # 1 station per 50 EVs
households['Stations_Needed'] = households['Estimated_EVs'] / STATIONS_PER_EV

existing_stations = len(ev_stations)
total_needed = households['Stations_Needed'].sum()
new_stations = total_needed - existing_stations

print(f"\nExisting charging stations: {existing_stations}")
print(f"Total charging stations needed: {int(total_needed)}")
print(f"New charging stations to install: {int(new_stations)}")

# --- Optional: Output per region ---
regional_summary = households.groupby('Region')['Stations_Needed'].sum().reset_index()
print("\nCharging station needs per region:\n", regional_summary)
