import pandas as pd
import numpy as np 
import matplotlib.pyplot as plt 
import folium
import json
import branca.colormap as cm
from sklearn.linear_model import LinearRegression 

housing_data = pd.read_csv(r"data\Household_data.csv")

#Filtering the data to remove unnecessary datapoints
housing_data = housing_data[housing_data['Statistic Label'] == 'Private households'] 
housing_data = housing_data[housing_data['Type of Accommodation'] != 'Caravan/Mobile home'] 
housing_data = housing_data[housing_data['Type of Accommodation'] != 'Total'] 

#Groupin the data by electroal Division and Type of Accommmodation
grouped_housing_data = housing_data.groupby(['CSO Electoral Divisions 2022', 'Type of Accommodation'])['VALUE'].sum().reset_index()
total_houses = grouped_housing_data['VALUE'].sum() 

# Group by 'CSO Electoral Divisions 2022' and 'Type of Accommodation', then sum 'VALUE'
grouped_data = grouped_housing_data.groupby(['CSO Electoral Divisions 2022', 'Type of Accommodation'])['VALUE'].sum().unstack(fill_value=0)


vehicles_data = pd.read_csv(r"data\vehicle_counts.csv")
## Filtering only for Electric Vechicles. 
vehicles_data = vehicles_data[(vehicles_data['Taxation Class'] == 'All Private Cars') & (vehicles_data['Type of Fuel'] == 'Electric')]
total_EVs = vehicles_data['VALUE'].sum()

# Calculate the total sum for each area
grouped_data['Total'] = grouped_data.sum(axis=1)

# Calculate 'Registered for Area' by applying the formula
grouped_data['Registered_ev'] = round(((grouped_data['Bed-Sit']*0.3 + grouped_data['Flat/Apartment']*0.8 + grouped_data['House/Bungalow']*1) / total_houses)*total_EVs)

# Calculate the percentage for each accommodation type
for accommodation_type in ['Bed-Sit', 'Flat/Apartment', 'House/Bungalow']:
    grouped_data[f'{accommodation_type}_Percentage'] = (grouped_data[accommodation_type] / grouped_data['Total'])*100

# Reset index to get a flat DataFrame
grouped_data = grouped_data.reset_index()

#Calculatin the percentag Home Charge percentage of the area 
grouped_data['home_charge_percentage'] = ((grouped_data['House/Bungalow_Percentage']*0.70) + (grouped_data['Flat/Apartment_Percentage']*0.40) + (grouped_data['Bed-Sit_Percentage']*0.40))/100

#Calculating the frequency of charing requried 
grouped_data['charge_frequency'] = 0.17 *(1 - grouped_data['home_charge_percentage'])

# Charging demand. i.e. no of stations that is required to meet the demand. 
grouped_data['charging_demand'] = round((grouped_data['Registered_ev'] * grouped_data['charge_frequency'])/ 4)

print(grouped_data)
grouped_data.to_csv(r"data\charging_demand.csv", index=False)  

## Storing Recommendations

areas = grouped_data[(grouped_data['charging_demand'] >= 20)]
charging_recommendation = areas['CSO Electoral Divisions 2022']

## Plotting the Map for the Charging Stations and Charging Demand.ArithmeticError
# ── CONFIGURATION ──────────────────────────────────────────────────────
GEOJSON_PATH = r"data\location_data.geojson"
CSV_PATH     = r"data\charging_demand.csv"
STATIONS_PATH  = r"data\charging_point_location.csv" 
OUTPUT_FILE  = "dublin_ev_map.html"
# ──────────────────────────────────────────────────────────────────────

# Load GeoJSON
with open(GEOJSON_PATH, "r", encoding="utf-8") as f:
    geojson = json.load(f)

# Load CSV
df = pd.read_csv(CSV_PATH, usecols=["CSO Electoral Divisions 2022", "charging_demand","Registered_ev"])

# Build lookup: normalise both sides to lowercase for matching
# CSV names look like "Arran Quay A, Dublin City"
# We'll match on just the division part (before the comma) to avoid county suffix issues
def normalise(name):
    return name.strip().lower()

demand_lookup = {}
for _, row in df.iterrows():
    full_name = row["CSO Electoral Divisions 2022"]
    division  = full_name.split(",")[0].strip()   # e.g. "Arran Quay A"
    demand_lookup[normalise(division)] = {"charging_demand": row["charging_demand"], "Registered_ev": row["Registered_ev"]
    }

# Dublin counties in the GeoJSON
DUBLIN_COUNTIES = {"DUBLIN CITY", "DÚN LAOGHAIRE-RATHDOWN", "FINGAL", "SOUTH DUBLIN"}

# Filter to Dublin only and inject charging_demand
dublin_features = []
unmatched = []

for feature in geojson["features"]:
    props   = feature["properties"]
    county  = (props.get("COUNTY_ENGLISH") or "").upper()
    if county not in DUBLIN_COUNTIES:
        continue

    ed_name = props.get("ED_ENGLISH", "")           # e.g. "ARRAN QUAY A"
    key     = normalise(ed_name)
    demand_data = demand_lookup.get(key, {})

    props["display_name"]    = ed_name.title()       # "Arran Quay A"
    props["charging_demand"] = demand_data.get("charging_demand")
    props["Registered_ev"] = demand_data.get("Registered_ev")   

    if demand_data.get("charging_demand") is None:
        unmatched.append(ed_name)

    dublin_features.append(feature)

geojson["features"] = dublin_features

print(f"Dublin divisions found: {len(dublin_features)}")
print(f"Matched: {len(dublin_features) - len(unmatched)}")
print(f"Unmatched ({len(unmatched)}): {unmatched[:10]}")

# Colour scale
demands  = df["charging_demand"]
colormap = cm.LinearColormap(
    colors=["#0d1f3c", "#0a7a8e", "#7ec84a", "#f0e030", "#e05020", "#b01010"],
    vmin=demands.min(),
    vmax=demands.max(),
    caption="EV Charging Demand"
)

# Build map
m = folium.Map(location=[53.35, -6.28], zoom_start=11, tiles="OpenStreetMap")

folium.GeoJson(
    geojson,
    name="Charging Demand",
    style_function=lambda feature: {
        "fillColor":   colormap(feature["properties"]["charging_demand"])
                       if feature["properties"]["charging_demand"] is not None
                       else "#1c2535",
        "color":       "#111111",
        "weight":      0.8,
        "fillOpacity": 0.75,
    },
    tooltip=folium.GeoJsonTooltip(
        fields=["display_name", "charging_demand","Registered_ev"],
        aliases=["Division", "Charging Demand","Registered EVs"],
        sticky=True,
    ),
).add_to(m)

colormap.add_to(m)

# ── CHARGING STATIONS (lightning bolt icons) ─────────────────────────
stations = pd.read_csv(STATIONS_PATH, encoding="latin-1")
stations = stations[stations['County'] == 'Dublin'] 

station_layer = folium.FeatureGroup(name="Charging Stations").add_to(m)

for _, row in stations.iterrows():
    folium.Marker(
        location=[row["Latitude"], row["Longitude"]],
        icon=folium.DivIcon(
            html='''
                <div style="
                    font-size: 18px;
                    line-height: 1;
                    filter: drop-shadow(0 0 4px #facc15);
                ">⚡</div>
            ''',
            icon_size=(22, 22),
            icon_anchor=(11, 11),
        ),
        tooltip="Charging Station",
    ).add_to(station_layer)

# Layer control — toggle demand/stations on and off
folium.LayerControl().add_to(m)
m.save(OUTPUT_FILE)
print(f"\nMap saved to {OUTPUT_FILE}")


## Predicting Increase in EV - Vehicles
df = pd.read_csv(r"data\vehicle_counts.csv")
## Filtering only for Electric Vechicles. 
df = df[(df['Taxation Class'] == 'All Private Cars') & (df['Type of Fuel'] == 'Electric') & (df['Year'] >= 2020) ]
# Define independent (X) and dependent (y) variables
X = df['Year'].values.reshape(-1, 1)
y = df['VALUE']

# Create and fit the model
model = LinearRegression()
model.fit(X, y)

# Predict for the next year (2026)
next_year = np.array([[2026]])  # Predict for the year 2026
predicted_ev_count = model.predict(next_year)


## Data to send to the Frontend 
print(f"Predicted number of EVs for 2026: {round(predicted_ev_count[0])}")

EV_increase = round(predicted_ev_count[0]) 
count_2025 = df[df['Year'] == 2025]['VALUE'].values[0]

#Calculate the percentage increase in EV based on the prediction.
percentage_increase = round(((EV_increase - count_2025) /count_2025)*100,2)
print(f"An increase of {percentage_increase} when compared to last year")

print(f"The charging demand in these areas are high {charging_recommendation} the city should consider adding EV charging stations in these areas.")

