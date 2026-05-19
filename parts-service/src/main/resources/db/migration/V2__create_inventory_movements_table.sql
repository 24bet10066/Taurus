CREATE TABLE IF NOT EXISTS inventory_movements (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    part_id        UUID          NOT NULL,
    movement_type  VARCHAR(30)   NOT NULL,
    quantity       INT           NOT NULL,
    stock_after    INT           NOT NULL,
    unit_price     DECIMAL(10,2),
    total_value    DECIMAL(10,2),
    reference_id   UUID,
    recorded_by    VARCHAR(100),
    notes          TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_movements_part_id    ON inventory_movements(part_id);
CREATE INDEX idx_inventory_movements_created_at ON inventory_movements(created_at DESC);
