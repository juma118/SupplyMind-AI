.PHONY: infra-up infra-down full-up full-down build test seed logs down clean

## Start only infrastructure (Postgres, Kafka, Keycloak) - run each service locally for fast iteration
infra-up:
	docker compose up -d postgres redpanda keycloak

infra-down:
	docker compose stop postgres redpanda keycloak

## Build and run every service + the web app as containers (one command, no local toolchain needed)
full-up:
	docker compose --profile full up --build -d

full-down:
	docker compose --profile full down

## Package every backend service (skips tests - use `make test` separately)
build:
	cd services/core-service && mvn -q package -DskipTests
	cd services/operations-service && mvn -q package -DskipTests
	cd services/shipment-service && mvn -q package -DskipTests
	cd services/ai-operations-service && mvn -q package -DskipTests
	cd services/api-gateway && mvn -q package -DskipTests
	cd apps/web && npm ci && npx ng build --configuration production

test:
	cd services/core-service && mvn -q test
	cd services/operations-service && mvn -q test
	cd services/shipment-service && mvn -q test
	cd services/ai-operations-service && mvn -q test
	cd services/api-gateway && mvn -q test

## Seed demo master data (warehouses/products/suppliers) into a running Core Service
seed:
	bash scripts/seed-data.sh

## Tail logs for every full-profile container
logs:
	docker compose --profile full logs -f

## Stop everything (infra + full profile, if running)
down:
	docker compose --profile full down
	docker compose stop postgres redpanda keycloak

## Remove containers and volumes - DESTROYS local Postgres data
clean:
	docker compose --profile full down -v
