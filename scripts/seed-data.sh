#!/usr/bin/env bash
# Seeds minimal demo master data so the primary demo scenario (README §2) can be run
# immediately: a warehouse pair with uneven stock, a product, and two suppliers with
# different reliability. Idempotent-ish: re-running will fail individual POSTs with 409s
# for data that already exists, which is safe to ignore.
set -euo pipefail

CORE_URL="${CORE_SERVICE_BASE_URL:-http://localhost:8081}"
OPS_URL="${OPERATIONS_SERVICE_BASE_URL:-http://localhost:8082}"
TENANT="${1:-tenant-demo}"

echo "Seeding tenant '$TENANT' via Core Service ($CORE_URL) and Operations Service ($OPS_URL)..."

curl -sf -X POST "$CORE_URL/api/v1/warehouses" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"code":"WH-A","name":"Columbus DC","city":"Columbus","country":"US","capacityUnits":50000}' \
  && echo "  created WH-A" || echo "  WH-A already exists, skipping"

curl -sf -X POST "$CORE_URL/api/v1/warehouses" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"code":"WH-C","name":"Reno DC","city":"Reno","country":"US","capacityUnits":50000}' \
  && echo "  created WH-C" || echo "  WH-C already exists, skipping"

curl -sf -X POST "$CORE_URL/api/v1/products" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"sku":"WID-2200","name":"Widget Pro","unitCost":8.50,"reorderPoint":1500}' \
  && echo "  created product WID-2200" || echo "  WID-2200 already exists, skipping"

curl -sf -X POST "$CORE_URL/api/v1/suppliers" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"name":"Nova Components","category":"Electronics","onTimeRate":0.982,"avgLeadTimeDays":4}' \
  && echo "  created supplier Nova Components" || echo "  Nova Components already exists, skipping"

curl -sf -X POST "$CORE_URL/api/v1/suppliers" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"name":"PacWest Supply","category":"Packaging","onTimeRate":0.714,"avgLeadTimeDays":6}' \
  && echo "  created supplier PacWest Supply" || echo "  PacWest Supply already exists, skipping"

echo "Seeding inventory: WH-A near reorder point, WH-C with surplus (transfer candidate)..."
curl -sf -X POST "$OPS_URL/api/v1/inventory/adjustments" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"warehouseCode":"WH-A","productSku":"WID-2200","quantityDelta":2400,"reason":"seed"}' \
  && echo "  WH-A stocked to 2400" || echo "  WH-A adjustment failed (may already be seeded)"

curl -sf -X POST "$OPS_URL/api/v1/inventory/adjustments" -H "Content-Type: application/json" -H "X-Tenant-Id: $TENANT" \
  -d '{"warehouseCode":"WH-C","productSku":"WID-2200","quantityDelta":3800,"reason":"seed"}' \
  && echo "  WH-C stocked to 3800" || echo "  WH-C adjustment failed (may already be seeded)"

echo ""
echo "Seed complete. Try the primary demo scenario:"
echo "  curl -X POST $OPS_URL/api/v1/inventory/adjustments ... (or)"
echo "  curl -X POST \${AI_OPS_URL:-http://localhost:8084}/api/v1/investigations/shipment-delay \\"
echo "    -H 'Content-Type: application/json' -H \"X-Tenant-Id: $TENANT\" \\"
echo "    -d '{\"shipmentId\":\"SHP-DEMO-1\",\"productSku\":\"WID-2200\",\"delayHours\":72,\"warehouseCode\":\"WH-A\",\"currentStockUnits\":2400,\"avgDailyDemand\":1200.0}'"
