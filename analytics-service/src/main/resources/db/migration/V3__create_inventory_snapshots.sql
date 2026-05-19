CREATE TABLE IF NOT EXISTS inventory_snapshots (
    snapshot_date        DATE         NOT NULL,
    part_id              UUID         NOT NULL,
    part_name            VARCHAR(255),
    stock_level          INT          NOT NULL DEFAULT 0,
    parts_sold_this_week INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (snapshot_date, part_id)
);

CREATE INDEX idx_inv_snap_date ON inventory_snapshots (snapshot_date DESC);
