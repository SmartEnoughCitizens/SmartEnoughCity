import pandas as pd
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# traffic_count = pd.read_csv(
#     os.path.join(BASE_DIR, "data", "combined_scats_with_locations.csv"),
#     encoding='utf-8-sig'
# )

car_counts1 = pd.read_csv(
    os.path.join(BASE_DIR, "data", "carscounts2017-2025.csv"),
    encoding="utf-8-sig"
)


car_counts2 = pd.read_csv(
    os.path.join(BASE_DIR, "data", "carscount2014-15.csv"),  # dein zweiter Dateiname
    encoding="utf-8-sig"
)

car_counts3 = pd.read_csv(
    os.path.join(BASE_DIR, "data", "carscount1998-2013.csv"),  # dein zweiter Dateiname
    encoding="utf-8-sig"
)


car_counts = pd.concat([car_counts1, car_counts2, car_counts3], ignore_index=True)


car_counts["VALUE"] = pd.to_numeric(car_counts["VALUE"], errors="coerce")


dublin_data = car_counts[
    car_counts["Licensing Authority"].str.contains("Dublin", case=False, na=False)
]

yearly_sum = (
    dublin_data
    .groupby("Year")["VALUE"]
    .sum()
    .reset_index()
    .rename(columns={"VALUE": "Total_Value"})
)

emission_by_year = (
    dublin_data
    .groupby(["Year", "Emission Band"])["VALUE"]
    .sum()
    .reset_index()
)

emission_pivot = (
    emission_by_year
    .pivot(index="Year", columns="Emission Band", values="VALUE")
    .fillna(0)
    .reset_index()
)

final_df = yearly_sum.merge(emission_pivot, on="Year", how="left")

yearly_sum["Cumulative_Total"] = yearly_sum["Total_Value"].cumsum()

emission_pivot_cum = emission_pivot.copy()
band_columns = [col for col in emission_pivot_cum.columns if col != "Year"]

for col in band_columns:
    emission_pivot_cum[col + "_cumulative"] = emission_pivot_cum[col].cumsum()

final_df = yearly_sum.merge(emission_pivot_cum, on="Year", how="left")


percent_per_year = emission_pivot.copy()
band_columns = [col for col in percent_per_year.columns if col != "Year"]

for col in band_columns:
    percent_per_year[col + "_percent"] = (percent_per_year[col] / yearly_sum["Total_Value"]) * 100

percent_total = emission_pivot.copy()
for col in band_columns:
    percent_total[col + "_total_percent"] = (percent_total[col].cumsum() / yearly_sum["Total_Value"].cumsum()) * 100

final_df = final_df.merge(percent_per_year[[ "Year"] + [col + "_percent" for col in band_columns]], on="Year")
final_df = final_df.merge(percent_total[["Year"] + [col + "_total_percent" for col in band_columns]], on="Year")



print(final_df)



output_path = os.path.join(BASE_DIR, "data", "dublin_car_summary.csv")

final_df.to_csv(output_path, index=False)

print(f"File saved to: {output_path}")
