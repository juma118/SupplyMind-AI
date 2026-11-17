CREATE TABLE inventory_balances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    warehouse_id    VARCHAR(64) NOT NULL,
    product_id      VARCHAR(64) NOT NULL,
    available_units INTEGER NOT NULL DEFAULT 0,
    reserved_units  INTEGER NOT NULL DEFAULT 0,
    incoming_units  INTEGER NOT NULL DEFAULT 0,
    damaged_units   INTEGER NOT NULL DEFAULT 0,
    reorder_point   INTEGER,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_balance UNIQUE (tenant_id, warehouse_id, product_id)
);

CREATE INDEX idx_inventory_balances_tenant ON inventory_balances (tenant_id);
CREATE INDEX idx_inventory_balances_product ON inventory_balances (tenant_id, product_id);

CREATE TABLE inventory_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    warehouse_id    VARCHAR(64) NOT NULL,
    product_id      VARCHAR(64) NOT NULL,
    movement_type   VARCHAR(32) NOT NULL,
    quantity        INTEGER NOT NULL,
    reference_type  VARCHAR(64),
    reference_id    VARCHAR(120),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_movements_tenant ON inventory_movements (tenant_id);
CREATE INDEX idx_inventory_movements_wh_product ON inventory_movements (tenant_id, warehouse_id, product_id);

CREATE TABLE purchase_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    supplier_id     VARCHAR(64) NOT NULL,
    warehouse_id    VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    total_value     NUMERIC(14, 2),
    source_type     VARCHAR(32),
    source_id       VARCHAR(120),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_purchase_orders_tenant ON purchase_orders (tenant_id);

CREATE TABLE purchase_order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders (id),
    product_id      VARCHAR(64) NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_po_items_po ON purchase_order_items (purchase_order_id);
