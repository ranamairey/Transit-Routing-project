# Damascus transit network import file

`damascus-network.json` is a real-data seed for the backend's bulk network import. It was
compiled on 13 Oct 2026 from the five research notes in
`research_notes/Damascus transit real data/` plus live OpenStreetMap pulls (Overpass API,
OSM base 2026-10-13). The file follows the shape accepted by `NetworkImportRequest`
(`stations[]` + `routes[]`); the extra per-item fields `source` and `confidence` are
documentation only and are ignored by the importer.

## Contents

| | count |
|---|---|
| Stations | **135** (Damascus 77, Rif Dimashq 58) |
| – by kind | 7 GARAGE, 2 HUB, 5 TRANSFER_HUB, 69 TERMINAL, 52 STOP |
| – by confidence | 89 high, 39 medium, 7 low |
| Routes | **83** (Damascus 35, Rif Dimashq 48) |
| – by vehicle | 70 SERVIS (microbus), 13 BUS |
| – by confidence | 4 high, 15 medium, 64 low |
| Routes with a real OSM polyline | **7** (`DAM-MEZ-03`, `DAM-SUM-01`, `DAM-BRZ-01`, `DAM-BUS-05`, `DAM-BUS-08`, `RIF-ART-01`, `RIF-AIR-01`) – all other routes have no `polyline`, so the app draws straight lines between stops |
| Fares | all in **new Syrian pounds** (post-Jan-2026 redenomination, 1 new = 100 old); 35–80 per ride |

Route code prefixes: `DAM-…` city servis lines by hub (MEZ Mezzeh, SUM Sumariyah, DWL Duwayla'a,
KFS Kafr Sousa, TDM Tadamon, DFS Daf al-Shawk, ZHR Zahira, QDM Qadam, MHJ Muhajireen, RKN Rukn
al-Din, ARN al-Areen, DMR Dummar, BRZ Barzeh, QBN Qaboun, JBR Jobar, MDN Midan) and `DAM-BUS-nn`
city buses; `RIF-…` Rif Dimashq lines by Damascus garage/corridor (JRM Jaramana, GHT Eastern
Ghouta via Abbasiyeen, TAL al-Tall, QUD Qudsaya, HAM al-Hameh, WZN Wazzan garage / Wadi Barada,
SUM Sumariyah garage, ART Jdeidat Artouz–Artouz–Qatana corridor, MDM Muadamiyah, DRY Darayya /
Sahnaya, SZ Sayyida Zaynab garage (Karaj al-Sitt), KSW Kiswah, SBN Sbeineh, AIR airport bus).

Station order in each route follows the line name (first-named terminus → last-named terminus),
e.g. `جديدة عرطوز - برامكة` lists Jdeidat Artouz first and Baramkeh last.

## How the data was built

1. **Line inventory** – the Damascus Governorate fuel/GPS decisions of 2022–2023 (SANA
   2022-11-03, Al-Watan/Nabd 2023-05-23, SANA 2023-06-06, Enab Baladi 2023-10-03), the 2024–2026
   press on relocated garages and fares, the two Wikiroutes stop sequences (2021), and the OSM
   route relations. Only lines named in those sources are included.
2. **Coordinates** – OSM features only: garages/stations (`amenity=bus_station`), named `موقف`
   bus stops, `place=*` nodes, `admin_level=10` "حي …" boundary centroids, squares and named POIs.
   Where the real stop is unmapped, the nearest named OSM feature is used and the station's
   `source` starts with `PROXY:` (13 stations, see below). No coordinate was invented.
3. **Polylines** – member ways of the OSM route relations were fetched with Overpass and the
   path between the two termini was extracted by shortest path over the way graph (relation
   20148903 carries both carriageways; small breaks ≤400 m between member ways were bridged),
   then Douglas–Peucker-simplified to ≤150 points. `DAM-SUM-01` uses relation 4113584
   (Sumariyah → Thawra St) plus a straight extension to Abbasiyeen.
4. **Fares** – Sept 2026 committee tables (buses 40, microbuses 40–70 new SYP by line); where a
   line-specific Sept 2026 driver-reported value exists it is used (e.g. مزة جبل–كراجات 50,
   ميدان–برزة 60, دويلعة/كفرسوسة/مهاجرين–صناعة 45). Otherwise: 40 for buses, 45 for short city
   lines, 50–55 for long city lines, and for Rif lines the last official old-SYP figure
   converted ÷100 and scaled for the Sept 2026 rise (e.g. Jdeidat Artouz–Baramkeh 60,
   Muadamiyah–Baramkeh 45, Wadi Barada lines 50–70). The basis is written in each route's `source`.

## Confidence legend

| value | stations | routes |
|---|---|---|
| **high** | exact OSM feature for the stop/garage/square | ordered stops from an OSM relation or a published stop sequence (Wikiroutes), line confirmed 2025–2026 |
| **medium** | neighbourhood centroid, or a named POI next to the stop | line named in an official/press source with a stated corridor; intermediate stops partly inferred |
| **low** | proxy feature (garage not mapped) or termini not geolocated | only endpoints known (intermediates inferred from the hub geography), line last attested before 2024, or fare estimated |

Proxy stations (coordinate is a nearby named OSM feature, not the stop itself): كراج جسر الوزان
(mosque on طريق بيروت, Dummar), كراج السيدة عائشة (quarter centroid), كراج نهر عيشة (named office),
ساحة الأشمر (mosque), العفيف (mosque), إشارة المدفع (park), الكناني (mosque), دف الشوك (pharmacy),
ركن الدين - الشيخ خالد (cemetery), جديدة الشيباني (bakery), ضاحية الفردوس (mosque), جديدة الفضل
(mosque), مخيم الحسينية (park). Also note: المزة 86 uses the quarter centroid for both the
"خزان" and "مدرسة" branch termini (merged into one route `DAM-MEZ-02`).

## Lines known by name but not included

Dropped for lack of coordinates or a usable terminus:

- **الدوار الشمالي**, **الدوار الجنوبي** – line names only; no route or endpoint description in any source.
- **وادي المشاريع – جسر الرئيس** – وادي المشاريع not found in OSM/Nominatim.
- **مخيم فلسطين – مخيم اليرموك – كراجات – برامكة** ("فلسطين – الكراجات", still running Sept 2026) – مخيم فلسطين and باب المخيم not found in OSM/Nominatim.
- **كفرسوسة مهايني – الفحامة** – المهايني stop not located (only a fuel station/hospital of that name in Midan).
- **مساكن برزة – شارع الثورة** (plain) and **مزة – البولمان** – treated as the same lines as `DAM-BRZ-01` and `DAM-MEZ-03`.
- **الزبداني / سرغايا / مضايا, بلودان, يبرود, النبك, الضمير, عدرا, الهامة (servis via جمرايا)** – no Damascus garage stated in 2024–2026 sources (Sarghaya is also outside the bbox).
- **السومرية – كراجات العباسيين** (Rif Dimashq, GlobalSy) and **جرمانا – البرامكة** (SANA) – snippet-only / unverified.
- **خان أرنبة / القنيطرة – السومرية** – outside Rif Dimashq and the bbox.
- **مخيم الحسينية local line**, Artouz municipal lines (عرطوز–قطنا, –خان الشيح, –كوكب, –المعضمية; كوكب–مرانة), **داريا–صحنايا** local, **بدا – جسر الرئيس** (2020 bus) – omitted as stale or Rif-internal; the Artouz–Sumariyah and Jdeidat Artouz–Sumariyah municipal lines are kept.
- 2017 taxi-servis lines (أوتستراد المزة–الهمك, الدويلعة–المواساة, جرمانا–المواساة, مشروع دمر–فيكتوريا, كفرسوسة–الجمارك) – no evidence they still run.
- Intermediate stops omitted inside kept routes: البحصة, نبيل نفيسة, الجبة, زين العابدين (جادات سلمية line); خورشيد, عادلة بيهم, الباشكاتب, موقف نورا, حديقة البرامكة, سانا as a separate stop (مهاجرين–صناعة); دوار التربية (ميدان–برزة); الذيابية (السيدة زينب line); دوار الكنيسة, عقدة 17 نيسان (حي العرين line); شارع العابد (الزاهرة–عش الورور bus).

Other caveats: the March 2026 decision to clear servis from مهاجرين–صناعة, سومرية–كراجات and باب
توما–جسر الحرية for electric buses had not been enforced by Sept 2026, so those lines are kept as
servis. The move of كراج الست to شارع الثلاثين (from 29 Mar 2026) is unconfirmed, so south-Rif lines
still end at كراجات الست. From 1 Jul 2026 the القابون/مساكن برزة/برزة البلد servis stops were to move
from شارع الثورة to under جسر الحرية; the file keeps شارع الثورة as the terminus.

## Sources

Official / press (Arabic): SANA 2022-11-03 (30-line fuel decision), Al-Watan via Nabd
2023-05-23 and SANA 2023-06-06 (GPS line lists), Enab Baladi 2023-10-03, Damascus Governorate
Telegram (posts 24989, 26670, 27053), Athr Press (2019-06-30, 2023-11-02, 2024-09-01,
2024-09-21), Syria Steps 2020-02-15, SANA 2020-09-01 (Tall/Bada buses), Al-Watan 2015-06-20,
2024-08-01, 2025-02-08, 2026-02-27, Enab Baladi 748369 / 757082 / 758398 / 775716 / 785030 /
785500 / 824978, SANA 2228387 / 2425353 / 2602722, Syria TV 2025-05-17 and 2025-12-22, North
Press 2025-12-23, Al-Modon 2026-01-31, Economist-sy 2026-09-10, Kassioun 2026-09-20, Qamishly
2025-11-24, eSyria 2014-11, Wikiroutes routes 76515 and 76516. Full URLs are in each item's
`source` field and in the research notes.

Geodata: OpenStreetMap via Overpass API (`lz4.overpass-api.de`, `overpass-api.de`,
`z.overpass-api.de`; custom User-Agent `SmartSyrianTransit-research/1.0`), route relations
4108101, 4108338, 4110300, 4110558, 4110592, 4111623, 4111655, 4113584, 4113624, 14469961,
14995426, 20148903; Nominatim for دوار البطيخة. The 46-landmark table in
`open_geodata_sources.md` and the garage GPS in `rif_dimashq_lines_garages.md` were re-fetched
and cross-checked live.

## Licence

Station coordinates and polylines are derived from OpenStreetMap data,
**© OpenStreetMap contributors, Open Database License (ODbL) 1.0**
(https://www.openstreetmap.org/copyright). If this file (or the stations/routes database built
from it) is redistributed, keep this attribution and apply ODbL share-alike to the derived
database. Line names, fares and route descriptions are facts compiled from the cited news and
official sources.

## Import

- API: `POST /api/v1/admin/network/import` with header `Authorization: Bearer <admin JWT>` and
  the file as the JSON body, e.g.
  `curl -X POST http://localhost:8080/api/v1/admin/network/import -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" --data-binary @docs/data/damascus-network.json`
- Admin console: **Import / Export** (Data) page → choose the file or paste its contents → Import.

The import is an upsert: stations match on `name` + `governorate`, routes on `code` (else on
`name`), so re-importing an edited file updates in place. Route stops reference stations by
exact `name`; the response lists warnings for any unresolved station. Re-run the validation
after editing: valid JSON, every route station exists, no duplicate station names or route
codes, all coordinates within lat 33.2–33.8 / lng 36.0–36.6, ≥2 stations per route.
