#!/usr/bin/env bash
# Builds (or refreshes) the walking-route graph on the ysman server from the latest Syria OpenStreetMap
# extract, then restarts the OSRM container. Run from a workstation: ./deploy/ysman/prepare-osrm.sh
set -euo pipefail
HOST="${DEPLOY_HOST:-shisha}"
DIR="${DEPLOY_DIR:-/srv/apps/syrian-transit}"
IMG=ghcr.io/project-osrm/osrm-backend:v6.0.0
ssh "$HOST" "set -e; mkdir -p '$DIR/osrm'; cd '$DIR/osrm'
  curl -fsSL -o syria-latest.osm.pbf https://download.geofabrik.de/asia/syria-latest.osm.pbf
  docker run --rm -v '$DIR/osrm:/data' $IMG osrm-extract -p /opt/foot.lua /data/syria-latest.osm.pbf
  docker run --rm -v '$DIR/osrm:/data' $IMG osrm-partition /data/syria-latest.osrm
  docker run --rm -v '$DIR/osrm:/data' $IMG osrm-customize /data/syria-latest.osrm
  cd '$DIR' && docker compose up -d syrian-transit-osrm && docker restart syrian-transit-osrm"
echo "Walking graph refreshed."
