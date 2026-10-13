# OSRM pedestrian routing

Gives the app real walking distances, times and street geometry for walking legs (transfers, first/last mile).
Without it the backend falls back to straight-line walking automatically.

1. `./deploy/osrm/prepare.sh` — downloads the Syria OpenStreetMap extract and builds the foot-profile graph into `deploy/osrm/data/` (git-ignored).
2. `docker compose --profile osrm up -d` — starts the `osrm` container and the backend with `OSRM_URL=http://osrm:5000`.
3. Local development without Docker: run OSRM anywhere and set `OSRM_URL=http://localhost:5000` in `.env`.

Refresh the data every few months by deleting `deploy/osrm/data/` and running step 1 again.
Map data © OpenStreetMap contributors (ODbL).
