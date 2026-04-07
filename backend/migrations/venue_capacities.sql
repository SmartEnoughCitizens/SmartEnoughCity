-- venue_capacities.sql
-- Manually set capacity for all known venues.
-- Safe to re-run: keyed on ticketmaster_id (unique) so duplicates are handled correctly.
-- Capacity is intentionally excluded from the data_handler upsert,
-- so these values are preserved across normal data refreshes.
--
-- After running this, the disruption detection service uses venue.capacity >= 1000
-- (LARGE_EVENT_VENUE_CAPACITY_THRESHOLD) with four severity tiers:
--   CRITICAL >= 15000  (Aviva, Croke Park, 3Arena, Marlay Park, etc.)
--   HIGH     >=  5000  (RDS, Tallaght Stadium, Convention Centre, etc.)
--   MEDIUM   >=  2500  (Bord Gáis, Knocknarea Arena, etc.)
--   LOW      >=  1000  (Vicar Street, 3Olympia, Gaiety, Ambassador, etc.)
--
-- Run with:
--   psql -h <host> -U app_owner -d smart_enough_city -f venue_capacities.sql

-- ── Arklow ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ91775H7';   -- The Arklow Bay Hotel
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ9177XKf';   -- The Arklow Bay Hotel (Wicklow)

-- ── Athenry ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917Avif';   -- Raheen Woods Hotel

-- ── Athlone ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ91779EV';   -- Ozone Nightclub
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917Avm0';   -- Shamrock Lodge Hotel

-- ── Aughrim ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 6500  WHERE ticketmaster_id = 'KovZ917APpe';   -- Echelon Park Aughrim

-- ── Ballinasloe ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 850   WHERE ticketmaster_id = 'KovZ917Adrf';   -- Shearwater Hotel

-- ── Ballinderreen ──────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AY_K';   -- Ballinderreen GAA Club

-- ── Bantry ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 700   WHERE ticketmaster_id = 'KovZ9177Ttf';   -- Live By The Bay

-- ── Carlow ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 11000 WHERE ticketmaster_id = 'KovZ917APb7';   -- Netwatch Dr. Cullen Park
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ9177Wlf';   -- Woodford Dolmen Hotel

-- ── Carrick-on-Shannon ─────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZpZAnEvvA';  -- The Landmark Hotel

-- ── Castleblayney ──────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 10000 WHERE ticketmaster_id = 'KovZ917APrw';   -- St. Mary's GAA

-- ── Clonmel ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AIE0';   -- Talbot Hotel
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ91775Hf';   -- Clonmel Park Hotel

-- ── Co. Cavan ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 25030 WHERE ticketmaster_id = 'KovZ9177T1f';   -- Kingspan Breffni
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ9177Xjf';   -- Slieve Russell Hotel
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917AZ1V';   -- Hotel Kilmore

-- ── Co. Clare ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 1650  WHERE ticketmaster_id = 'KovZ9177XIV';   -- Treacy's West County Hotel
UPDATE external_data.venues SET capacity = 20100 WHERE ticketmaster_id = 'KovZ9177TSf';   -- Zimmer Biomet Pairc Chiosog

-- ── Co. Donegal ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ9177XqV';   -- Abbey Hotel
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AZY7';   -- Jacksons Hotel
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ9177Tl7';   -- Mount Errigal Hotel (Donegal)
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917AZ5V';   -- McGrorys Of Culdaff

-- ── Co. Dublin ─────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917A2Z0';   -- The Purty Kitchen
UPDATE external_data.venues SET capacity = 20000 WHERE ticketmaster_id = 'KovZ9177Xt7';   -- Malahide Castle

-- ── Co. Kildare ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 800   WHERE ticketmaster_id = 'KovZ917Avj7';   -- Keadeen Hotel
UPDATE external_data.venues SET capacity = 15000 WHERE ticketmaster_id = 'KovZ9177Tmf';   -- Cedral St. Conleth's Park

-- ── Co. Laois ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ9177TY0';   -- Midlands Park Hotel
UPDATE external_data.venues SET capacity = 80000 WHERE ticketmaster_id = 'KovZpZAt6nkA';  -- Stradbally Hall (Electric Picnic)
UPDATE external_data.venues SET capacity = 240   WHERE ticketmaster_id = 'KovZ9177TG0';   -- The Dunamaise Arts Centre
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917ASWK';   -- Ballykilcavan Brewery

-- ── Co. Leitrim ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ9177Ta0';   -- Landmark Hotel

-- ── Co. Louth ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ9177Tv7';   -- Fairways Hotel
UPDATE external_data.venues SET capacity = 1250  WHERE ticketmaster_id = 'KovZ917AZ5f';   -- The TLT

-- ── Co. Mayo ───────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 4000  WHERE ticketmaster_id = 'KovZ9177Tm0';   -- TF Royal, Castlebar

-- ── Co. Meath ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AZaV';   -- Headfort Arms Hotel
UPDATE external_data.venues SET capacity = 80000 WHERE ticketmaster_id = 'KovZ9177WBV';   -- Slane Castle

-- ── Co. Monaghan ───────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 1500  WHERE ticketmaster_id = 'KovZ9177T57';   -- Hillgrove Hotel

-- ── Co. Offaly ─────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ9177TYf';   -- Bridge House Hotel
UPDATE external_data.venues SET capacity = 18000 WHERE ticketmaster_id = 'KovZ9177TaV';   -- Glenisk O'Connor Park
UPDATE external_data.venues SET capacity = 474   WHERE ticketmaster_id = 'KovZ9177Wy7';   -- Tullamore Court Hotel

-- ── Co. Tipperary ──────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AV2A';   -- Cashel Rugby Club
UPDATE external_data.venues SET capacity = 45690 WHERE ticketmaster_id = 'KovZ9177WwV';   -- FBD Semple Stadium
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917ARyy';   -- The Ragg

-- ── Co. Waterford ──────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 15000 WHERE ticketmaster_id = 'KovZ9177X_0';   -- Cappoquin Logistics Fraher Field

-- ── Co. Westmeath ──────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 1200  WHERE ticketmaster_id = 'KovZ9177XP7';   -- Mullingar Park Hotel
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ917AV2f';   -- Cathedral Of Christ The King Mullingar

-- ── Co. Wexford ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 700   WHERE ticketmaster_id = 'KovZ9177Xyf';   -- Riverside Park Hotel

-- ── Co. Wicklow ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AOOO';   -- Russborough House
UPDATE external_data.venues SET capacity = 3200  WHERE ticketmaster_id = 'KovZ9177XUV';   -- Carlisle Grounds
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AsOz';   -- The Avon Resort

-- ── Cork ───────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ9177Xq0';   -- Clayton Hotel Silver Springs
UPDATE external_data.venues SET capacity = 1189  WHERE ticketmaster_id = 'KovZ9177WnV';   -- Cork City Hall
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ9177Wl7';   -- Cork Opera House
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ9177Ti7';   -- Cyprus Avenue
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ9177Xi7';   -- Live At The Marquee
UPDATE external_data.venues SET capacity = 45300 WHERE ticketmaster_id = 'KovZ9177Tc0';   -- SuperValu Pairc Ui Chaoimh
UPDATE external_data.venues SET capacity = 150   WHERE ticketmaster_id = 'KovZ917ARIH';   -- Wavelength
UPDATE external_data.venues SET capacity = 8000  WHERE ticketmaster_id = 'KovZ9177Xy0';   -- Virgin Media Park

-- ── Dalkey ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917AEXI';   -- The Queens Bar

-- ── Donegal ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 450   WHERE ticketmaster_id = 'KovZ917AZY7';   -- Abbey Hotel Donegal Town
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AxnZ';   -- The Central Hotel

-- ── Donnybrook ─────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 6000  WHERE ticketmaster_id = 'KovZ9177TbV';   -- Energia Park

-- ── Drogheda ───────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917AEXw';   -- McHugh's
UPDATE external_data.venues SET capacity = 800   WHERE ticketmaster_id = 'KovZ9177227';   -- The Lourdes Church

-- ── Dublin ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 13000 WHERE ticketmaster_id = 'KovZ9177WYV';   -- 3Arena
UPDATE external_data.venues SET capacity = 13000 WHERE ticketmaster_id = 'Za5ju3rKuqZDvSkAObKhc1-BlkmkyRJaU1'; -- 3Arena (duplicate entry)
UPDATE external_data.venues SET capacity = 1600  WHERE ticketmaster_id = 'KovZ9177TI0';   -- 3Olympia Theatre
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917Av40';   -- Abbey Tavern
UPDATE external_data.venues SET capacity = 250   WHERE ticketmaster_id = 'KovZ9177Wc7';   -- Academy 2
UPDATE external_data.venues SET capacity = 1300  WHERE ticketmaster_id = 'KovZ9177Tef';   -- Ambassador Theatre
UPDATE external_data.venues SET capacity = 51700 WHERE ticketmaster_id = 'KovZ9177Tn7';   -- Aviva Stadium
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917A2OV';   -- BELLOBAR
UPDATE external_data.venues SET capacity = 250   WHERE ticketmaster_id = 'KovZ91774_0';   -- Bloody Mary's
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917At_Q';   -- Bonnington Hotel
UPDATE external_data.venues SET capacity = 2111  WHERE ticketmaster_id = 'KovZ917AZa7';   -- Bord Gais Energy Theatre
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917ASTC';   -- Clonsilla Inn
UPDATE external_data.venues SET capacity = 4000  WHERE ticketmaster_id = 'KovZ917AiQi';   -- Collins Barracks
UPDATE external_data.venues SET capacity = 82300 WHERE ticketmaster_id = 'KovZ9177XKV';   -- Croke Park
UPDATE external_data.venues SET capacity = 150   WHERE ticketmaster_id = 'KovZ917ASyL';   -- Curveball
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AJcx';   -- Fairview Park
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917AeXV';   -- Fitzpatrick Castle Hotel
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917APbU';   -- GAA Generic Venue
UPDATE external_data.venues SET capacity = 1145  WHERE ticketmaster_id = 'KovZ9177XT0';   -- Gaiety Theatre
UPDATE external_data.venues SET capacity = 250   WHERE ticketmaster_id = 'KovZ9177XtV';   -- Green Room at The Academy
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917AmyF';   -- Hyde Dublin
UPDATE external_data.venues SET capacity = 3000  WHERE ticketmaster_id = 'KovZ9177WHV';   -- IMMA
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ9177Ty0';   -- Iveagh Gardens
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917AOs7';   -- Lost Lane
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917AV11';   -- Louis Fitzgerald Hotel
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AV39';   -- Marine Hotel
UPDATE external_data.venues SET capacity = 40000 WHERE ticketmaster_id = 'KovZ9177TJ0';   -- Marlay Park
UPDATE external_data.venues SET capacity = 2080  WHERE ticketmaster_id = 'KovZ9177TZf';   -- National Stadium
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ9177TM0';   -- Opium
UPDATE external_data.venues SET capacity = 15000 WHERE ticketmaster_id = 'KovZ9177Tnf';   -- Parnell Park
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AYrs';   -- Peacocks Bar & Lounge
UPDATE external_data.venues SET capacity = 60000 WHERE ticketmaster_id = 'KovZ9177TgV';   -- Phoenix Park
UPDATE external_data.venues SET capacity = 22000 WHERE ticketmaster_id = 'KovZ9177XQV';   -- RDS (Royal Dublin Society)
UPDATE external_data.venues SET capacity = 18500 WHERE ticketmaster_id = 'KovZ9177TAf';   -- RDS Arena
UPDATE external_data.venues SET capacity = 36000 WHERE ticketmaster_id = 'KovZ917AOCi';   -- St Annes Park
UPDATE external_data.venues SET capacity = 10000 WHERE ticketmaster_id = 'KovZ9177Xxf';   -- Tallaght Stadium
UPDATE external_data.venues SET capacity = 650   WHERE ticketmaster_id = 'KovZ9177TF7';   -- The Academy
UPDATE external_data.venues SET capacity = 550   WHERE ticketmaster_id = 'KovZ9177TlV';   -- The Button Factory
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AVu3';   -- The Cock Tavern
UPDATE external_data.venues SET capacity = 150   WHERE ticketmaster_id = 'KovZ917Am0e';   -- The Coliemore
UPDATE external_data.venues SET capacity = 8000  WHERE ticketmaster_id = 'KovZ917AZu0';   -- The Convention Centre Dublin
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917ASDy';   -- The Goat Bar & Grill
UPDATE external_data.venues SET capacity = 650   WHERE ticketmaster_id = 'KovZ9177Wn0';   -- The Grand Social
UPDATE external_data.venues SET capacity = 1935  WHERE ticketmaster_id = 'KovZ9177TCf';   -- The Helix
UPDATE external_data.venues SET capacity = 100   WHERE ticketmaster_id = 'KovZ917ARXy';   -- The Ruby Sessions
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917ACif';   -- The Sound House
UPDATE external_data.venues SET capacity = 350   WHERE ticketmaster_id = 'KovZ9177Tm7';   -- The Sugar Club
UPDATE external_data.venues SET capacity = 1935  WHERE ticketmaster_id = 'KovZ917AYnW';   -- The Theatre at the Helix
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917AZs0';   -- The Unitarian Church
UPDATE external_data.venues SET capacity = 170   WHERE ticketmaster_id = 'KovZ917AJzr';   -- The Workmans Cellar
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AZq7';   -- The Workmans Club
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AZIV';   -- Trinity College
UPDATE external_data.venues SET capacity = 120   WHERE ticketmaster_id = 'KovZ917AZd7';   -- Upstairs At Whelans
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ917Avlf';   -- Various Venues
UPDATE external_data.venues SET capacity = 1500  WHERE ticketmaster_id = 'KovZ9177TZ0';   -- Vicar Street
UPDATE external_data.venues SET capacity = 450   WHERE ticketmaster_id = 'KovZ9177Tlf';   -- Whelans
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917A2Z0';   -- The Purty Kitchen (Co Dublin)
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917AVu3';   -- The Cock Tavern
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917AV11';   -- Louis Fitzgerald Hotel

-- ── Dún Laoghaire ──────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917At9x';   -- Walters

-- ── Galway ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ9177WM0';   -- Black Box
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AYGm';   -- Clifden Showgrounds
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ9177WDf';   -- Festival Big Top
UPDATE external_data.venues SET capacity = 1200  WHERE ticketmaster_id = 'KovZ9177XYf';   -- LEISURELAND
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917Avv7';   -- Monroes
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ9177Txf';   -- Roisin Dubh
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ9177XQ7';   -- ST. Nicholas' College Church
UPDATE external_data.venues SET capacity = 3000  WHERE ticketmaster_id = 'KovZ917AVk4';   -- The Crescendo Tent
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917Adj7';   -- Town Hall Theatre
UPDATE external_data.venues SET capacity = 26197 WHERE ticketmaster_id = 'KovZ917AZd0';   -- Pearse Stadium

-- ── Gorey ──────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ917A_l7';   -- Gorey Little Theatre

-- ── Kerry ──────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ91779J7';   -- Ballyroe Heights Hotel
UPDATE external_data.venues SET capacity = 750   WHERE ticketmaster_id = 'KovZ91779R0';   -- INEC Acoustic Club
UPDATE external_data.venues SET capacity = 4000  WHERE ticketmaster_id = 'KovZ9177Te0';   -- Gleneagle Arena

-- ── Kildare ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 2000  WHERE ticketmaster_id = 'KovZ917AZKf';   -- Goffs
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AVfA';   -- Palmerstown Estate House
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AeMf';   -- Westgrove Hotel

-- ── Kilkenny ───────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ9177XyV';   -- Hotel Kilkenny
UPDATE external_data.venues SET capacity = 3000  WHERE ticketmaster_id = 'KovZ917AVf0';   -- Live At Castlemills
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ9177TxV';   -- Set Theatre
UPDATE external_data.venues SET capacity = 1939  WHERE ticketmaster_id = 'KovZ9177WB7';   -- The Hub At Cillin Hill
UPDATE external_data.venues SET capacity = 27000 WHERE ticketmaster_id = 'KovZ9177TA7';   -- UPMC Nowlan Park

-- ── Killeagh ───────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZ917ARH1';   -- Joe's Farm Crisps

-- ── Letterkenny ────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 1000  WHERE ticketmaster_id = 'KovZpZAnEllA';  -- Mount Errigal Hotel

-- ── Limerick ───────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 120   WHERE ticketmaster_id = 'KovZ917Ad57';   -- Kasbah Social Club
UPDATE external_data.venues SET capacity = 2000  WHERE ticketmaster_id = 'KovZ917Ad-7';   -- King John's Castle
UPDATE external_data.venues SET capacity = 510   WHERE ticketmaster_id = 'KovZ917Ade0';   -- Lime Tree Theatre
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AZtf';   -- Live At The Big Top
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AdA7';   -- Live at the Docklands
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917AQ-7';   -- Longcourt House Hotel
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917Aixp';   -- Radisson Blu Hotel Limerick
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ9177XOV';   -- Dolans Warehouse
UPDATE external_data.venues SET capacity = 44023 WHERE ticketmaster_id = 'KovZ9177X37';   -- TUS Gaelic Grounds
UPDATE external_data.venues SET capacity = 26000 WHERE ticketmaster_id = 'KovZ9177XEV';   -- Thomond Park Stadium
UPDATE external_data.venues SET capacity = 1038  WHERE ticketmaster_id = 'KovZ9177WS0';   -- University Concert Hall
UPDATE external_data.venues SET capacity = 200   WHERE ticketmaster_id = 'KovZ9177X30';   -- Upstairs at Dolans

-- ── Longford ───────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 10000 WHERE ticketmaster_id = 'KovZ9177TEf';   -- Pearse Park

-- ── Mallow ─────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 10000 WHERE ticketmaster_id = 'KovZ917APQQ';   -- Mallow GAA

-- ── Meath ──────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ917AZlf';   -- CityNorth Hotel

-- ── Mullingar ──────────────────────────────────────────────────────────────────
-- (already covered under Co. Westmeath above)

-- ── Portlaoise ─────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ9177X_f';   -- Kavanagh's

-- ── Raphoe / Portlaw / Rathkeale ───────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AQWV';   -- Curraghmore House
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AI0j';   -- Oakfield Park
UPDATE external_data.venues SET capacity = 10000 WHERE ticketmaster_id = 'KovZ917AIGU';   -- Mick Neville Park

-- ── Roscommon ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 16000 WHERE ticketmaster_id = 'KovZ9177TzV';   -- King & Moffatt Dr. Hyde Park

-- ── Saint Kevin / Harcourt Street ──────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ91772FV';   -- Bond Nightclub

-- ── Sligo ──────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 2200  WHERE ticketmaster_id = 'KovZ917AZt0';   -- Knocknarea Arena
UPDATE external_data.venues SET capacity = 15000 WHERE ticketmaster_id = 'KovZ9177TcV';   -- Markievicz Park
UPDATE external_data.venues SET capacity = 600   WHERE ticketmaster_id = 'KovZ9177Wgf';   -- Radisson Blu Hotel Sligo
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ9177T5V';   -- Sligo Park Hotel

-- ── Smithfield ─────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 150   WHERE ticketmaster_id = 'KovZ917Aeq7';   -- The Cobblestone

-- ── Waterford ──────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917ARgF';   -- Bank Lane
UPDATE external_data.venues SET capacity = 8000  WHERE ticketmaster_id = 'KovZ917Av30';   -- RSC Waterford
UPDATE external_data.venues SET capacity = 650   WHERE ticketmaster_id = 'KovZ917AeMV';   -- Theatre Royal
UPDATE external_data.venues SET capacity = 400   WHERE ticketmaster_id = 'KovZ917A8mV';   -- Tower Hotel

-- ── Wexford ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 15000 WHERE ticketmaster_id = 'KovZ9177Wmf';   -- Chadwicks Wexford Park
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AQrf';   -- Crown Live
UPDATE external_data.venues SET capacity = 3000  WHERE ticketmaster_id = 'KovZ917AmVj';   -- Live at The Breakwater
UPDATE external_data.venues SET capacity = 774   WHERE ticketmaster_id = 'KovZ917Ad4f';   -- National Opera House
UPDATE external_data.venues SET capacity = 500   WHERE ticketmaster_id = 'KovZ917AvZV';   -- Wexford Spiegeltent Festival

-- ── Wicklow ────────────────────────────────────────────────────────────────────
UPDATE external_data.venues SET capacity = 5000  WHERE ticketmaster_id = 'KovZ917AIjI';   -- Glendalough Estate
UPDATE external_data.venues SET capacity = 300   WHERE ticketmaster_id = 'KovZ917AsOz';   -- The Avon Resort

-- ── Verify ─────────────────────────────────────────────────────────────────────

SELECT
    v.name,
    v.city,
    v.capacity,
    COUNT(e.id) AS upcoming_events,
    CASE
        WHEN v.capacity >= 15000 THEN 'CRITICAL'
        WHEN v.capacity >= 5000  THEN 'HIGH'
        WHEN v.capacity >= 2500  THEN 'MEDIUM'
        WHEN v.capacity >= 1000  THEN 'LOW'
        ELSE 'below threshold'
    END AS disruption_severity
FROM external_data.venues v
LEFT JOIN external_data.events e
    ON e.venue_id = v.id AND e.event_date >= CURRENT_DATE
WHERE v.capacity IS NOT NULL
GROUP BY v.name, v.city, v.capacity
ORDER BY v.capacity DESC;
