ALTER TABLE ai_recommendations
    ADD COLUMN action_type VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN action_product_sku VARCHAR(64),
    ADD COLUMN action_quantity INTEGER,
    ADD COLUMN action_source_warehouse VARCHAR(64),
    ADD COLUMN action_dest_warehouse VARCHAR(64),
    ADD COLUMN action_supplier_id VARCHAR(64),
    ADD COLUMN action_unit_price NUMERIC(14, 2);
