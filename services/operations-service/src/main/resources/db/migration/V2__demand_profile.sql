-- README §6: ROP = (Average Daily Demand x Lead Time) + Safety Stock. These three inputs
-- have to live somewhere persistent per (tenant, warehouse, product) for that formula to
-- be computed from real data rather than a one-off request parameter; reorder_point itself
-- already existed but nothing ever wrote to it before this.
ALTER TABLE inventory_balances
    ADD COLUMN avg_daily_demand NUMERIC(10, 2),
    ADD COLUMN lead_time_days INTEGER,
    ADD COLUMN safety_stock INTEGER;
