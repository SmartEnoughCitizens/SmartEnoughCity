import requests
import os

folder_id = "1rit5LjXJiknktSpWMip60qkuaEZ0Nsnt"
download_dir = "static_data"

os.makedirs(download_dir, exist_ok=True)

# Ordnerinhalt über öffentliche HTML-Seite scrapen
url = f"https://drive.google.com/drive/folders/{folder_id}"
response = requests.get(url)

# File-IDs aus dem HTML extrahieren
import re
file_ids = re.findall(r'"([a-zA-Z0-9_-]{33})"', response.text)
file_ids = list(set(file_ids))  # Duplikate entfernen

print(f"{len(file_ids)} Dateien gefunden.")

for file_id in file_ids:
    download_url = f"https://drive.google.com/uc?export=download&id={file_id}"
    r = requests.get(download_url, allow_redirects=True)
    
    # Dateiname aus Header lesen
    filename = r.headers.get('Content-Disposition', f'{file_id}.bin')
    filename = re.findall(r'filename="(.+)"', filename)
    filename = filename[0] if filename else f"{file_id}.bin"
    
    filepath = os.path.join(download_dir, filename)
    with open(filepath, 'wb') as f:
        f.write(r.content)
    print(f"Heruntergeladen: {filename}")

print("Fertig!")