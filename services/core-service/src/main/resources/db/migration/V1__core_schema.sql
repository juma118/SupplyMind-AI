CREATE TABLE warehouses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    code            VARCHAR(32) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    city            VARCHAR(120),
    country         VARCHAR(120),
    capacity_units  INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_warehouse_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    sku             VARCHAR(64) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    unit_cost       NUMERIC(14, 2),
    reorder_point   INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE TABLE suppliers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(120),
    on_time_rate         NUMERIC(5, 4),
    avg_lead_time_days  INTEGER,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_warehouses_tenant ON warehouses (tenant_id);
CREATE INDEX idx_products_tenant ON products (tenant_id);
CREATE INDEX idx_suppliers_tenant ON suppliers (tenant_id);

CREATE TABLE supplier_products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    supplier_id     UUID NOT NULL REFERENCES suppliers (id),
    product_id      UUID NOT NULL REFERENCES products (id),
    unit_price      NUMERIC(14, 2),
    lead_time_days  INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_supplier_product UNIQUE (tenant_id, supplier_id, product_id)
);

CREATE INDEX idx_supplier_products_tenant ON supplier_products (tenant_id);
