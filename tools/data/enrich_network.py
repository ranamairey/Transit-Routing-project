#!/usr/bin/env python3
"""
Builds docs/data/damascus-network.json from the hand-curated docs/data/damascus-network.base.json by adding:

  1. every named place in Damascus / Rif Dimashq from OpenStreetMap (neighbourhoods, villages, squares,
     bus stops, garages, hospitals, universities, landmarks) so riders can search any destination;
  2. road-following geometry for every line (OSRM driving profile through the line's ordered stops);
  3. hail-and-ride boarding points: servis pick up and drop off anywhere on their road, so known places
     lying on a line's path are added to that line as intermediate stops, in travel order.

Usage:  python3 tools/data/enrich_network.py            (uses cached downloads in tools/data/.cache)
        python3 tools/data/enrich_network.py --refresh  (re-downloads OSM places and line geometry)

Data © OpenStreetMap contributors (ODbL). Requests are cached and paced (OSRM ≤ 1 req/s).
"""
import hashlib
import json
import math
import os
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BASE = os.path.join(ROOT, "docs/data/damascus-network.base.json")
OUT = os.path.join(ROOT, "docs/data/damascus-network.json")
RES = os.path.join(ROOT, "src/main/resources/data/damascus-network.json")
CACHE = os.path.join(ROOT, "tools/data/.cache")
UA = "SmartSyrianTransit-data/1.0 (+https://github.com/ranamairey/Transit-Routing-project)"
OVERPASS = ["https://lz4.overpass-api.de/api/interpreter", "https://overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter"]
OSRM = os.environ.get("OSRM_DRIVING_URL", "https://router.project-osrm.org").rstrip("/")
BBOX = (33.30, 36.00, 33.75, 36.60)
DAMASCUS_CENTER = (33.5130, 36.2920)
REFRESH = "--refresh" in sys.argv

# Hail-and-ride snapping distance (metres from the line's road) by place type.
SNAP = {"stop": 120, "landmark": 150, "area": 350}
MIN_GAP_ALONG = 90  # don't add a boarding point within this distance (along the line) of another stop


# ---------------------------------------------------------------- helpers
def normalize(text):
    if not text:
        return ""
    s = unicodedata.normalize("NFKC", text).lower().strip()
    s = re.sub(r"[ً-ْٰـ]", "", s)
    s = re.sub(r"[أإآٱ]", "ا", s).replace("ة", "ه")
    s = re.sub(r"[ىی]", "ي", s)
    s = re.sub(r"[ءئؤ]", "", s)
    s = re.sub(r"(^|\s)ال", r"\1", s)
    s = re.sub(r"[^\w\s]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def has_arabic(text):
    return bool(text) and bool(re.search(r"[؀-ۿ]", text))


def http(url, data=None, timeout=180):
    """curl instead of urllib: some system Pythons ship an old TLS stack that fails the handshake."""
    import subprocess
    cmd = ["curl", "-sS", "-f", "-m", str(timeout), "-A", UA, url]
    if data is not None:
        cmd[1:1] = ["--data-binary", "@-"]
    out = subprocess.run(cmd, input=data, capture_output=True, check=True)
    return out.stdout.decode("utf-8")


def cached(name, fetch):
    path = os.path.join(CACHE, name)
    if os.path.exists(path) and not REFRESH:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    value = fetch()
    os.makedirs(CACHE, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(value, f, ensure_ascii=False)
    return value


def overpass(query):
    last = None
    for attempt in range(6):
        url = OVERPASS[attempt % len(OVERPASS)]
        try:
            body = http(url, data=urllib.parse.urlencode({"data": query}).encode())
            return json.loads(body)
        except Exception as e:  # busy / 429 / 504
            last = e
            time.sleep(8 + attempt * 6)
    raise RuntimeError(f"Overpass failed: {last}")


def meters(a, b):
    lat1, lng1 = a
    lat2, lng2 = b
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lng2 - lng1)
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * 6371000 * math.asin(math.sqrt(h))


KX = 111320 * math.cos(math.radians(33.5))
KY = 110574


def xy(p):
    return (p[1] * KX, p[0] * KY)


def project(point, line_xy, cum):
    """Distance from point to polyline (m) and position along it (m)."""
    px, py = xy(point)
    best, along = float("inf"), 0.0
    for i in range(1, len(line_xy)):
        ax, ay = line_xy[i - 1]
        bx, by = line_xy[i]
        dx, dy = bx - ax, by - ay
        l2 = dx * dx + dy * dy
        t = 0 if l2 == 0 else max(0, min(1, ((px - ax) * dx + (py - ay) * dy) / l2))
        cx, cy = ax + t * dx, ay + t * dy
        d = math.hypot(px - cx, py - cy)
        if d < best:
            best, along = d, cum[i - 1] + t * (cum[i] - cum[i - 1])
    return best, along


def simplify(points, tol):
    """Douglas-Peucker on lat/lng points with tolerance in metres."""
    if len(points) < 3:
        return points
    pts = [xy(p) for p in points]
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        s, e = stack.pop()
        ax, ay = pts[s]
        bx, by = pts[e]
        dx, dy = bx - ax, by - ay
        l2 = dx * dx + dy * dy
        idx, dmax = -1, 0
        for i in range(s + 1, e):
            px, py = pts[i]
            t = 0 if l2 == 0 else max(0, min(1, ((px - ax) * dx + (py - ay) * dy) / l2))
            d = math.hypot(px - (ax + t * dx), py - (ay + t * dy))
            if d > dmax:
                idx, dmax = i, d
        if dmax > tol and idx > 0:
            keep[idx] = True
            stack += [(s, idx), (idx, e)]
    return [p for p, k in zip(points, keep) if k]


# ---------------------------------------------------------------- 1. OSM places
PLACES_QUERY = f"""
[out:json][timeout:180];
(
  node["place"~"^(neighbourhood|suburb|quarter|village|town|hamlet|city_block|square)$"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  relation["boundary"="administrative"]["admin_level"~"^(8|9|10)$"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["amenity"~"^(bus_station|hospital|university|college|marketplace)$"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["highway"="bus_stop"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["public_transport"~"^(platform|station)$"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["place"="square"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["tourism"~"^(attraction|museum)$"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["historic"~"^(monument|city_gate|castle)$"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["shop"="mall"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
  nwr["junction"]["name"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
);
out center tags;
"""

GOV_QUERY = """
[out:json][timeout:120];
relation["boundary"="administrative"]["admin_level"="4"]["name:en"~"^Damascus"];
out geom;
"""

PREFIXES = ("حي ", "بلدية ", "ناحية ", "منطقة ", "مدينة ", "قرية ", "بلدة ")
GENERIC = {normalize(x) for x in ["موقف", "موقف باص", "موقف الباص", "محطة", "محطة باص", "كراج", "Bus Stop", "bus stop", "دوار", "ساحة", "مشفى", "جامعة"]}


def classify(tags):
    """(stationKind, snap class, priority) — lower priority wins on duplicate names."""
    if tags.get("amenity") == "bus_station":
        return ("GARAGE" if "كراج" in tags.get("name", "") else "TERMINAL"), "stop", 0
    if tags.get("highway") == "bus_stop" or tags.get("public_transport") in ("platform", "station"):
        return "STOP", "stop", 1
    if tags.get("place") == "square" or "junction" in tags:
        return "POINT", "landmark", 2
    if tags.get("place") in ("neighbourhood", "suburb", "quarter", "city_block") or tags.get("boundary") == "administrative":
        return "POINT", "area", 3
    if tags.get("place") in ("village", "town", "hamlet"):
        return "POINT", "area", 4
    return "POINT", "landmark", 5


def damascus_governorate_segments():
    def fetch():
        data = overpass(GOV_QUERY)
        segs = []
        for el in data["elements"]:
            name_en = el.get("tags", {}).get("name:en", "")
            if "Rif" in name_en or "Countryside" in name_en:
                continue
            for m in el.get("members", []):
                if m.get("role") == "outer" and m.get("geometry"):
                    g = [(p["lat"], p["lon"]) for p in m["geometry"]]
                    segs += [(g[i], g[i + 1]) for i in range(len(g) - 1)]
        return segs
    return [tuple(map(tuple, s)) for s in cached("damascus_governorate.json", fetch)]


def inside(point, segs):
    if not segs:
        return meters(point, DAMASCUS_CENTER) < 7000
    lat, lng = point
    hit = False
    for (a_lat, a_lng), (b_lat, b_lng) in segs:
        if (a_lat > lat) != (b_lat > lat):
            x = a_lng + (lat - a_lat) * (b_lng - a_lng) / (b_lat - a_lat)
            if x > lng:
                hit = not hit
    return hit


def osm_places(taken_names, gov_segs):
    data = cached("osm_places.json", lambda: overpass(PLACES_QUERY))
    found = {}
    for el in data["elements"]:
        tags = el.get("tags", {})
        raw = tags.get("name:ar") or tags.get("name")
        if not has_arabic(raw):
            continue
        lat = el.get("lat") or el.get("center", {}).get("lat")
        lng = el.get("lon") or el.get("center", {}).get("lon")
        if lat is None or not (BBOX[0] <= lat <= BBOX[2] and BBOX[1] <= lng <= BBOX[3]):
            continue
        name = raw.strip()
        aliases = []
        for p in PREFIXES:
            if name.startswith(p) and len(name) > len(p) + 2:
                aliases.append(name)
                name = name[len(p):].strip()
                break
        key = normalize(name)
        if len(key) < 2 or key in GENERIC or key in taken_names:
            continue
        kind, snap, prio = classify(tags)
        for k in ("alt_name", "old_name", "alt_name:ar", "name"):
            v = tags.get(k)
            if v and v != name and has_arabic(v) and v not in aliases:
                aliases.append(v)
        item = {
            "name": name,
            "nameEn": tags.get("name:en") or None,
            "aliases": "، ".join(aliases) or None,
            "stationKind": kind,
            "governorate": "DAMASCUS" if inside((lat, lng), gov_segs) else "RIF_DIMASHQ",
            "lat": round(lat, 6),
            "lng": round(lng, 6),
            "source": f"osm:{el['type']}/{el['id']}",
            "confidence": "high" if snap == "stop" else "medium",
            "_snap": snap,
            "_prio": prio,
        }
        if key not in found or prio < found[key]["_prio"]:
            found[key] = item
    return list(found.values())


# ---------------------------------------------------------------- 2. road geometry
def osrm_route(points):
    coords = ";".join(f"{lng:.6f},{lat:.6f}" for lat, lng in points)
    key = hashlib.sha1(coords.encode()).hexdigest()[:16]

    def fetch():
        time.sleep(1.1)
        url = f"{OSRM}/route/v1/driving/{coords}?overview=full&geometries=geojson&continue_straight=false"
        for attempt in range(4):
            try:
                d = json.loads(http(url, timeout=60))
                if d.get("code") == "Ok":
                    r = d["routes"][0]
                    return {"distance": r["distance"], "coords": [[c[1], c[0]] for c in r["geometry"]["coordinates"]]}
                return {"error": d.get("code")}
            except Exception as e:
                time.sleep(3 + attempt * 3)
                err = str(e)
        raise RuntimeError(f"OSRM failed: {err}")  # not cached; next run retries
    try:
        return cached(f"osrm_{key}.json", fetch)
    except RuntimeError as e:
        return {"error": str(e)}


# ---------------------------------------------------------------- main
def main():
    with open(BASE, encoding="utf-8") as f:
        base = json.load(f)
    stations = base["stations"]
    routes = base["routes"]

    taken = set()
    for s in stations:
        taken.add(normalize(s["name"]))
        for a in re.split(r"[،,;/]", s.get("aliases") or ""):
            if a.strip():
                taken.add(normalize(a))

    print("Fetching Damascus governorate boundary…")
    gov = damascus_governorate_segments()
    print(f"  {len(gov)} boundary segments")
    print("Fetching OpenStreetMap places…")
    places = osm_places(taken, gov)
    print(f"  {len(places)} new places")

    by_name = {s["name"]: s for s in stations}
    snap_of = {s["name"]: ("stop" if s["stationKind"] in ("STOP", "GARAGE", "TERMINAL", "HUB", "TRANSFER_HUB") else "landmark") for s in stations}
    for p in places:
        by_name[p["name"]] = p
        snap_of[p["name"]] = p["_snap"]
    candidates = [(n, (s["lat"], s["lng"])) for n, s in by_name.items()]

    print("Building road geometry and boarding points…")
    stats = {"osrm": 0, "osm": 0, "straight": 0, "inferred": 0}
    for r in routes:
        stops = [(by_name[n]["lat"], by_name[n]["lng"]) for n in r["stations"]]
        straight = sum(meters(stops[i - 1], stops[i]) for i in range(1, len(stops)))
        if r.get("polyline"):
            line = [(p["lat"], p["lng"]) for p in r["polyline"]]
            stats["osm"] += 1
        else:
            res = osrm_route(stops)
            if "coords" in res and res["distance"] > straight * 1.9 and len(stops) >= 2:
                # one-way streets can force long detours: route stop to stop and use a straight hop
                # wherever a single hop is still unrealistic
                coords, total = [], 0.0
                for i in range(1, len(stops)):
                    hop = meters(stops[i - 1], stops[i])
                    seg = osrm_route([stops[i - 1], stops[i]])
                    if "coords" in seg and seg["distance"] <= max(hop * 2.2, hop + 400):
                        part, total = seg["coords"], total + seg["distance"]
                    else:
                        part, total = [list(stops[i - 1]), list(stops[i])], total + hop
                    coords += part if not coords else part[1:]
                res = {"distance": total, "coords": coords}
                r["source"] = (r.get("source") or "") + " | per-stop road routing"
            if "coords" in res and res["distance"] <= max(straight * 3.0, straight + 3000):
                line = simplify([tuple(c) for c in res["coords"]], 8)
                tol = 8
                while len(line) > 220:
                    tol *= 1.5
                    line = simplify([tuple(c) for c in res["coords"]], tol)
                r["polyline"] = [{"lat": round(a, 6), "lng": round(b, 6)} for a, b in line]
                stats["osrm"] += 1
                r["source"] = (r.get("source") or "") + " | road geometry: OSRM driving over OSM"
            else:
                line = stops
                stats["straight"] += 1
                sys.stderr.write(f"  ! {r['code']}: no usable road geometry ({res.get('error') or 'detour too long'})\n")

        # hail-and-ride boarding points along the road
        line_xy = [xy(p) for p in line]
        cum = [0.0]
        for i in range(1, len(line)):
            cum.append(cum[-1] + math.hypot(line_xy[i][0] - line_xy[i - 1][0], line_xy[i][1] - line_xy[i - 1][1]))
        existing = []
        for n in r["stations"]:
            _, along = project((by_name[n]["lat"], by_name[n]["lng"]), line_xy, cum)
            existing.append((along, n))
        if any(existing[i][0] < existing[i - 1][0] - 50 for i in range(1, len(existing))):
            continue  # stop order does not follow the geometry; leave the curated stop list untouched
        lats = [p[0] for p in line]
        lngs = [p[1] for p in line]
        pad = 0.004
        added = []
        for name, pt in candidates:
            if name in r["stations"]:
                continue
            if not (min(lats) - pad <= pt[0] <= max(lats) + pad and min(lngs) - pad <= pt[1] <= max(lngs) + pad):
                continue
            dist, along = project(pt, line_xy, cum)
            if dist > SNAP[snap_of[name]] or along < existing[0][0] + 40 or along > existing[-1][0] - 40:
                continue
            added.append((along, dist, name))
        added.sort(key=lambda t: (t[0], t[1]))
        merged = list(existing)
        for along, dist, name in added:
            if all(abs(along - a) >= MIN_GAP_ALONG for a, _ in merged):
                merged.append((along, name))
        merged.sort(key=lambda t: t[0])
        new_count = len(merged) - len(existing)
        if new_count:
            r["stations"] = [n for _, n in merged]
            r["inferredStops"] = new_count
            stats["inferred"] += new_count

    # keep every curated station and every OSM place (places stay searchable even when no line passes)
    out_stations = list(stations) + [{k: v for k, v in p.items() if not k.startswith("_")} for p in places]
    for s in out_stations:
        if s.get("nameEn") is None:
            s.pop("nameEn", None)
        if s.get("aliases") is None:
            s.pop("aliases", None)
    used = {n for r in routes for n in r["stations"]}

    # validation
    names = [s["name"] for s in out_stations]
    assert len(names) == len(set(names)), "duplicate station names"
    codes = [r["code"] for r in routes]
    assert len(codes) == len(set(codes)), "duplicate route codes"
    for r in routes:
        assert len(r["stations"]) >= 2 and all(n in by_name for n in r["stations"]), r["code"]
    for s in out_stations:
        assert 33.2 <= s["lat"] <= 33.8 and 36.0 <= s["lng"] <= 36.6, s["name"]

    out = {"stations": out_stations, "routes": routes}
    for path in (OUT, RES):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False, indent=1)
    stops = sorted(len(r["stations"]) for r in routes)
    print(f"Stations: {len(out_stations)} ({len(used)} on a line, {len(out_stations) - len(used)} searchable places)")
    print(f"Routes: {len(routes)} | geometry: {stats['osm']} OSM relations, {stats['osrm']} OSRM roads, {stats['straight']} straight")
    print(f"Boarding points added along lines: {stats['inferred']} | stops per line min/median/max: {stops[0]}/{stops[len(stops)//2]}/{stops[-1]}")


if __name__ == "__main__":
    main()
