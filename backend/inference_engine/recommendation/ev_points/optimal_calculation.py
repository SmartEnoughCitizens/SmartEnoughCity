import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
import glob
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
print("Files in data folder:", os.listdir(os.path.join(BASE_DIR, "data")))

# --- 0. Read base data ---
ev_stations = pd.read_csv(
    os.path.join(BASE_DIR, "data", "ESB-EV-charge-point-locations.csv"),
    encoding='latin1'
)
households = pd.read_csv(
    os.path.join(BASE_DIR, "data", "Household_data.csv"),
    encoding='latin1'
)
dlr_sites = pd.read_csv(
    os.path.join(BASE_DIR, "data", "dlr_scats_sites-1.csv"),
    encoding='latin1'
)
dcc_sites = pd.read_csv(
    os.path.join(BASE_DIR, "data", "dcc_traffic_signals_20221130.csv"),
    encoding='latin1'
)

print("DCC site columns:", dcc_sites.columns.tolist())

# --- 1. Aggregate SCATS files ---
scats_folder = os.path.join(BASE_DIR, "data", "scats")
all_files = glob.glob(os.path.join(scats_folder, "*.csv"))

regional_sums = []

for file in all_files:
    print(f"\nProcessing {file} ...")

    cols = ['End_Time', 'Site', 'Sum_Volume']
    for chunk in pd.read_csv(file, usecols=cols, encoding='utf-8-sig', chunksize=100_000):
        
       

        chunk['End_Time'] = pd.to_datetime(chunk['End_Time'], format="%Y%m%d%H%M%S", errors='coerce')
        chunk = chunk.dropna(subset=['End_Time'])
        chunk['Year'] = chunk['End_Time'].dt.year
        chunk['Month'] = chunk['End_Time'].dt.month

        agg = chunk.groupby(['Site', 'Year', 'Month'])['Sum_Volume'].sum().reset_index()
        
        
        regional_sums.append(agg)

ev_registered = pd.concat(regional_sums, ignore_index=True)
print("\nCombined SCATS data head:\n", ev_registered.head())
print("\nCombined SCATS data:\n", ev_registered)

print("\nSCATS data types:\n", ev_registered[ev_registered["Site"]== 3].sum())

print("SCATS combined description:\n", ev_registered.describe())
print("SCATS unique Sites:", ev_registered['Site'].nunique())


ev_registered.groupby('Site', 'Year', 'Month')['Sum_Volume'].sum().sort_values(ascending=False)

print("debuuug:" + ev_registered)


# --- 2. Site mappings ---
dlr_sites = dlr_sites[['Site_ID','Location','Lat','Long']].rename(columns={'Site_ID':'Site'})
dcc_sites = dcc_sites[['SiteID','Site_Description_Cap','Region','Lat','Long']].rename(columns={'SiteID':'Site','Site_Description_Cap':'Location'})

ev_registered = ev_registered.merge(dlr_sites, on='Site', how='left')
ev_registered = ev_registered.merge(dcc_sites, on='Site', how='left', suffixes=('_DLR','_DCC'))

print("\nAfter merging site info:")
print(ev_registered.head())
print("Null counts:\n", ev_registered.isnull().sum())

ev_registered['Final_Location'] = ev_registered['Location_DLR'].combine_first(ev_registered['Location_DCC'])
ev_registered['Final_Lat'] = ev_registered['Lat_DLR'].combine_first(ev_registered['Lat_DCC'])
ev_registered['Final_Long'] = ev_registered['Long_DLR'].combine_first(ev_registered['Long_DCC'])
ev_registered['Final_Region'] = ev_registered['Region']

# --- 3. Aggregate by Region & Year ---
regional_ev = ev_registered.groupby(['Final_Region','Year'])['Sum_Volume'].sum().reset_index()
print("\nRegional EV aggregation head:\n", regional_ev.head())

# --- 4. Forecast EVs for 2026 per region ---
future_year = 2026
predictions = []

for region in regional_ev['Final_Region'].unique():
    region_data = regional_ev[regional_ev['Final_Region'] == region].sort_values('Year')
    print(f"\nRegion: {region}, data:\n", region_data)
    
    if len(region_data) < 2:
        predicted = region_data['Sum_Volume'].iloc[-1]
    else:
        X = region_data['Year'].values.reshape(-1,1)
        y = region_data['Sum_Volume'].values
        print("X:", X.flatten(), "y:", y)
        model = LinearRegression()
        model.fit(X, y)
        predicted = model.predict(np.array([[future_year]]))[0]
        print(f"Predicted EVs for {region} in {future_year}: {predicted}")
    
    predictions.append({'Region': region, 'Predicted_EVs_2026': predicted})

pred_df = pd.DataFrame(predictions)
print("\nEV forecast for 2026 per region:\n", pred_df)

# --- 5. Estimate EVs per household division ---
total_households = households['VALUE'].sum()
households['EV_proportion'] = households['VALUE'] / total_households
total_EVs_2026 = pred_df['Predicted_EVs_2026'].sum()
households['Estimated_EVs_2026'] = households['EV_proportion'] * total_EVs_2026
households['Stations_Needed'] = households['Estimated_EVs_2026'] / 50

existing_stations = len(ev_stations)
total_needed = households['Stations_Needed'].sum()
new_stations = total_needed - existing_stations

print(f"\nTotal new charging stations required (based on households): {int(new_stations)}")
