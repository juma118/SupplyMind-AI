#!/usr/bin/env bash
# Starts infrastructure (Postgres, Kafka, Keycloak) in Docker, then each backend service,
# the Gateway, and the Angular app locally (mvn / npm) for fast edit-reload cycles - the
# alternative to `make full-up`, which containerizes everything instead (slower rebuilds,
# closer to prod).
#
# Use this when actively developing; use full-up for a one-command demo/deploy run.
set -euo pipefail
cd "$(dirname "$0")/.."

LOG_DIR="${TMPDIR:-/tmp}/supplymind-logs"
mkdir -p "$LOG_DIR"

echo "Starting infrastructure (Postgres, Kafka, Keycloak)..."
docker compose up -d postgres redpanda keycloak

echo "Waiting for Postgres..."
for i in $(seq 1 30); do
  if docker exec supplymind-postgres pg_isready -U supplymind >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Waiting for Keycloak to import the supplymind realm..."
for i in $(seq 1 40); do
  if curl -s http://localhost:8180/realms/supplymind/.well-known/openid-configuration 2>/dev/null | grep -q issuer; then
    break
  fi
  sleep 3
done

start_service() {
  local name="$1" dir="$2" cmd="$3" port="$4"
  echo "Starting $name on :$port (log: $LOG_DIR/$name.log)..."
  (cd "$dir" && eval "$cmd") > "$LOG_DIR/$name.log" 2>&1 &
  disown
}

start_service core-service services/core-service "mvn -q spring-boot:run" 8081
start_service operations-service services/operations-service "mvn -q spring-boot:run" 8082
start_service shipment-service services/shipment-service "mvn -q quarkus:dev" 8083
start_service ai-operations-service services/ai-operations-service "mvn -q spring-boot:run" 8084
start_service api-gateway services/api-gateway "mvn -q spring-boot:run" 8085
start_service web apps/web "npx ng serve --host 0.0.0.0 --port 4200" 4200

echo ""
echo "All services starting in the background. Logs: $LOG_DIR/<service>.log"
echo "Health checks:"
echo "  curl http://localhost:8081/actuator/health   # core-service"
echo "  curl http://localhost:8082/actuator/health   # operations-service"
echo "  curl http://localhost:8083/q/health          # shipment-service"
echo "  curl http://localhost:8084/actuator/health   # ai-operations-service"
echo "  curl http://localhost:8085/actuator/health   # api-gateway"
echo "  open http://localhost:4200                   # web (redirects to Keycloak login)"
echo ""
echo "Demo users (Keycloak realm 'supplymind', all password 'password'):"
echo "  j.alvarez     - supply-chain-manager"
echo "  admin.demo    - platform-admin"
echo "  p.chen        - procurement-manager"
echo "  auditor.demo  - auditor"
echo "Keycloak admin console: http://localhost:8180 (admin/admin)"
echo ""
echo "Run scripts/seed-data.sh once everything is up to load demo master data."
echo "Run scripts/stop-local-dev.sh to stop everything started by this script."
