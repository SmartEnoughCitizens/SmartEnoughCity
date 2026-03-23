"""Visualize emission band data over time."""

from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd

# Path
BASE_DIR = Path(__file__).parent
output_path = BASE_DIR / "data" / "dublin_car_summary.csv"

# Load data
df = pd.read_csv(output_path)
df = df.sort_values("Year")

# Bands
bands = [
    "Band  A",
    "Band  B",
    "Band  C",
    "Band  D",
    "Band  E",
    "Band  F",
    "Band  G",
    "Not available",
]

# ----------------------------
# 1️⃣ Absolute Values Plot
# ----------------------------

plt.figure()

for band in bands:
    plt.plot(df["Year"], df[band], label=band)

plt.title("All Bands - Absolute Values Over Time")
plt.xlabel("Year")
plt.ylabel("Number of Vehicles")
plt.legend()
plt.show()


# Load
df = pd.read_csv(output_path)
df = df.sort_values("Year")

# Plot total cars
plt.figure()
plt.plot(df["Year"], df["Total_Value"])
plt.title("Total Number of Cars Over Time")
plt.xlabel("Year")
plt.ylabel("Total Cars")
plt.show()


df = pd.read_csv(output_path)
df = df.sort_values("Year")

# Plot total cars
plt.figure()
plt.plot(df["Year"], df["Cumulative_Total"])
plt.title("Total Number of Cars Over Time")
plt.xlabel("Year")
plt.ylabel("Cars per Year")
plt.show()
