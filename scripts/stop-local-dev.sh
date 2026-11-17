#!/usr/bin/env bash
# Stops processes bound to the ports local-dev.sh started (core/operations/shipment/
# ai-operations/gateway/web). Does not touch Postgres/Kafka/Keycloak containers - use
# `make infra-down` or `docker compose stop postgres redpanda keycloak` for those.
set -euo pipefail

PORTS=(8081 8082 8083 8084 8085 4200)

for port in "${PORTS[@]}"; do
  if command -v netstat >/dev/null 2>&1 && uname -s | grep -qi mingw; then
    # Windows (Git Bash)
    pid=$(netstat -ano | grep ":$port " | grep LISTENING | head -1 | awk '{print $NF}')
    if [ -n "${pid:-}" ]; then
      echo "Stopping process on :$port (pid $pid)"
      taskkill //PID "$pid" //F || true
    fi
  else
    # macOS / Linux
    pid=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    if [ -n "${pid:-}" ]; then
      echo "Stopping process on :$port (pid $pid)"
      kill -9 "$pid" || true
    fi
  fi
done

echo "Done. Infra containers (Postgres, Kafka) are left running - stop with 'make infra-down' if needed."
