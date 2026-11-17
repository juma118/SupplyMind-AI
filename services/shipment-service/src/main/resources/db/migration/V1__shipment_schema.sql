CREATE TABLE shipments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    shipment_code   VARCHAR(64) NOT NULL,
    carrier         VARCHAR(120),
    origin          VARCHAR(120),
    destination_warehouse VARCHAR(64) NOT NULL,
    product_sku     VARCHAR(64) NOT NULL,
    quantity        INTEGER NOT NULL,
    avg_daily_demand NUMERIC(10, 2),
    status          VARCHAR(32) NOT NULL,
    eta             TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_shipment_tenant_code UNIQUE (tenant_id, shipment_code)
);

CREATE INDEX idx_shipments_tenant ON shipments (tenant_id);
CREATE INDEX idx_shipments_status ON shipments (tenant_id, status);

CREATE TABLE shipment_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    shipment_id     UUID NOT NULL REFERENCES shipments (id),
    event_type      VARCHAR(32) NOT NULL,
    description     TEXT,
    delay_hours     INTEGER,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shipment_events_shipment ON shipment_events (shipment_id);
