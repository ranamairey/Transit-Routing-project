#!/usr/bin/env bash
# Configures MapTiler map tiles for the rider app, the Android build and the admin console.
# Usage: ./tools/set-map-key.sh YOUR_MAPTILER_KEY
# Writes git-ignored .env.local files; restart `npm run dev` (or rebuild) afterwards.
set -euo pipefail
KEY="${1:?usage: $0 YOUR_MAPTILER_KEY}"
cd "$(dirname "$0")/.."
for app in frontend admin-console; do
  f="$app/.env.local"
  touch "$f"
  grep -v '^VITE_TILE_URL' "$f" > "$f.tmp" || true
  {
    cat "$f.tmp"
    echo "VITE_TILE_URL=https://api.maptiler.com/maps/streets-v2/256/{z}/{x}/{y}.png?key=$KEY"
    echo "VITE_TILE_URL_DARK=https://api.maptiler.com/maps/streets-v2-dark/256/{z}/{x}/{y}.png?key=$KEY"
  } > "$f"
  rm -f "$f.tmp"
  echo "wrote $f"
done
code=$(curl -s -o /dev/null -w "%{http_code}" -e "http://localhost:5173/" "https://api.maptiler.com/maps/streets-v2/256/12/2460/1642.png?key=$KEY")
echo "MapTiler test tile (as localhost): HTTP $code (200 = key works; 403 = key wrong or localhost not in allowed origins)"
