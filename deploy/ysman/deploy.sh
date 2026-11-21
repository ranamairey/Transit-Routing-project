#!/usr/bin/env bash
# Builds the rider app and admin console for their ysman sub-paths and ships them, with the API
# source, to /srv/apps/syrian-transit on the server, then rebuilds and restarts the containers.
# Needs the "shisha" SSH host (see ~/.ssh/config). The server's .env is created once by hand
# (see docs/DEPLOY.md) and is never overwritten here.
# Map tiles: by default the MapTiler key from frontend/.env.local is used, which only works once
# the server's address is listed among the key's allowed origins. TILES=osm uses free
# OpenStreetMap tiles instead.
set -euo pipefail
cd "$(dirname "$0")/../.."

HOST="${DEPLOY_HOST:-shisha}"
DIR="${DEPLOY_DIR:-/srv/apps/syrian-transit}"
ORIGIN="${DEPLOY_ORIGIN:-https://ysman.dyndns.info}"
OUT="$(mktemp -d)"
if [ "${TILES:-maptiler}" = "osm" ]; then export VITE_TILE_URL= VITE_TILE_URL_DARK=; fi
trap 'rm -rf "$OUT"' EXIT

echo "==> Building the rider app (/transit/)"
(cd frontend && VITE_BASE=/transit/ VITE_API_BASE=/transit VITE_ADMIN_URL="$ORIGIN/transit-admin/" VITE_PUBLIC_URL="$ORIGIN/transit" \
  npx vite build --outDir "$OUT/web/rider" --emptyOutDir --logLevel warn)

echo "==> Building the admin console (/transit-admin/)"
(cd admin-console && VITE_BASE=/transit-admin/ VITE_API_BASE=/transit \
  npx vite build --outDir "$OUT/web/admin" --emptyOutDir --logLevel warn)

echo "==> Uploading to $HOST:$DIR"
ssh "$HOST" "mkdir -p '$DIR/backend' '$DIR/web'"
rsync -az --delete Dockerfile pom.xml mvnw .mvn src "$HOST:$DIR/backend/"
rsync -az --delete "$OUT/web/" "$HOST:$DIR/web/"
rsync -az deploy/ysman/docker-compose.yml deploy/ysman/nginx.conf "$HOST:$DIR/"

echo "==> Rebuilding and restarting"
ssh "$HOST" "cd '$DIR' && test -f .env || { echo 'Missing $DIR/.env (see docs/DEPLOY.md)'; exit 1; }
  docker compose up -d --build && docker image prune -f >/dev/null"

echo "==> Done: $ORIGIN/transit/  and  $ORIGIN/transit-admin/"
