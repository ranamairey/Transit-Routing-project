#!/usr/bin/env bash
# Starts the backend with variables from .env (MONGO_URI, MONGO_DB, JWT_SECRET, ...)
set -euo pipefail
cd "$(dirname "$0")"
if [ -f .env ]; then
  set -a; . ./.env; set +a
fi
exec ./mvnw -q spring-boot:run "$@"
