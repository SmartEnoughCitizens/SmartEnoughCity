"""EV charging demand estimation and prediction."""

import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression

housing_data = pd.read_csv(r"data\Household_data.csv")

# Filtering the data to remove unnecessary datapoints
housing_data = housing_data[housing_data["Statistic Label"] == "Private households"]
housing_data = housing_data[
    housing_data["Type of Accommodation"] != "Caravan/Mobile home"
]
housing_data = housing_data[housing_data["Type of Accommodation"] != "Total"]

# Groupin the data by electroal Division and Type of Accommmodation
grouped_housing_data = (
    housing_data.groupby(["CSO Electoral Divisions 2022", "Type of Accommodation"])[
        "VALUE"
    ]
    .sum()
    .reset_index()
)
total_houses = grouped_housing_data["VALUE"].sum()

# Group by 'CSO Electoral Divisions 2022' and 'Type of Accommodation', then sum 'VALUE'
grouped_data = (
    grouped_housing_data.groupby(
        ["CSO Electoral Divisions 2022", "Type of Accommodation"]
    )["VALUE"]
    .sum()
    .unstack(fill_value=0)
)


vehicles_data = pd.read_csv(r"data\vehicle_counts.csv")
## Filtering only for Electric Vechicles.
vehicles_data = vehicles_data[
    (vehicles_data["Taxation Class"] == "All Private Cars")
    & (vehicles_data["Type of Fuel"] == "Electric")
]
total_evs = vehicles_data["VALUE"].sum()

# Calculate the total sum for each area
grouped_data["Total"] = grouped_data.sum(axis=1)

# Calculate 'Registered for Area' by applying the formula
grouped_data["Registered_ev"] = round(
    (
        (
            grouped_data["Bed-Sit"] * 0.3
            + grouped_data["Flat/Apartment"] * 0.8
            + grouped_data["House/Bungalow"] * 1
        )
        / total_houses
    )
    * total_evs
)

# Calculate the percentage for each accommodation type
for accommodation_type in ["Bed-Sit", "Flat/Apartment", "House/Bungalow"]:
    grouped_data[f"{accommodation_type}_Percentage"] = (
        grouped_data[accommodation_type] / grouped_data["Total"]
    ) * 100

# Reset index to get a flat DataFrame
grouped_data = grouped_data.reset_index()

# Calculatin the percentag Home Charge percentage of the area
grouped_data["home_charge_percentage"] = (
    (grouped_data["House/Bungalow_Percentage"] * 0.70)
    + (grouped_data["Flat/Apartment_Percentage"] * 0.40)
    + (grouped_data["Bed-Sit_Percentage"] * 0.40)
) / 100

# Calculating the frequency of charing requried
grouped_data["charge_frequency"] = 0.17 * (1 - grouped_data["home_charge_percentage"])

# Charging demand. i.e. no of stations that is required to meet the demand.
grouped_data["charging_demand"] = round(
    (grouped_data["Registered_ev"] * grouped_data["charge_frequency"]) / 4
)

print(grouped_data)
grouped_data.to_csv(r"data\charging_demand.csv", index=False)

## Storing Recommendations

areas = grouped_data[(grouped_data["charging_demand"] >= 20)]
charging_recommendation = areas["CSO Electoral Divisions 2022"]

# Map generation now handled via ev_router API endpoints
# (/api/v1/ev/charging-stations, /api/v1/ev/charging-demand, /api/v1/ev/areas-geojson)

## Predicting Increase in EV - Vehicles
df = pd.read_csv(r"data\vehicle_counts.csv")
## Filtering only for Electric Vechicles.
df = df[
    (df["Taxation Class"] == "All Private Cars")
    & (df["Type of Fuel"] == "Electric")
    & (df["Year"] >= 2020)
]
# Define independent (X) and dependent (y) variables
X = df["Year"].values.reshape(-1, 1)
y = df["VALUE"]

# Create and fit the model
model = LinearRegression()
model.fit(X, y)

# Predict for the next year (2026)
next_year = np.array([[2026]])  # Predict for the year 2026
predicted_ev_count = model.predict(next_year)


## Data to send to the Frontend
print(f"Predicted number of EVs for 2026: {round(predicted_ev_count[0])}")

EV_increase = round(predicted_ev_count[0])
count_2025 = df[df["Year"] == 2025]["VALUE"].values[0]

# Calculate the percentage increase in EV based on the prediction.
percentage_increase = round(((EV_increase - count_2025) / count_2025) * 100, 2)
print(f"An increase of {percentage_increase} when compared to last year")

print(
    f"The charging demand in these areas are high {charging_recommendation} the city should consider adding EV charging stations in these areas."
)
