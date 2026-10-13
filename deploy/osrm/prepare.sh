#!/usr/bin/env bash
# One-time preparation of the OSRM pedestrian graph for Syria (≈ 60 MB download, a few minutes of processing).
# Usage: ./deploy/osrm/prepare.sh   (requires Docker)
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p data
if [ ! -f data/syria-latest.osm.pbf ]; then
  curl -L -o data/syria-latest.osm.pbf https://download.geofabrik.de/asia/syria-latest.osm.pbf
fi
IMG=osrm/osrm-backend:latest
docker run --rm -t -v "$PWD/data:/data" $IMG osrm-extract -p /opt/foot.lua /data/syria-latest.osm.pbf
docker run --rm -t -v "$PWD/data:/data" $IMG osrm-partition /data/syria-latest.osrm
docker run --rm -t -v "$PWD/data:/data" $IMG osrm-customize /data/syria-latest.osrm
echo "Done. Start with: docker compose --profile osrm up -d"
