CREATE TABLE IF NOT EXISTS spare_parts (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)  NOT NULL,
    sku             VARCHAR(100)  UNIQUE,
    category        VARCHAR(50),
    appliance_type  VARCHAR(50),
    brand           VARCHAR(100),
    is_oem          BOOLEAN       NOT NULL DEFAULT false,
    buy_price       DECIMAL(10,2) NOT NULL,
    sell_price      DECIMAL(10,2) NOT NULL,
    internal_price  DECIMAL(10,2) NOT NULL,
    current_stock   INT           NOT NULL DEFAULT 0,
    min_stock       INT           NOT NULL DEFAULT 1,
    location        VARCHAR(50),
    is_fast_moving  BOOLEAN       NOT NULL DEFAULT false,
    is_active       BOOLEAN       NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_spare_parts_appliance_type ON spare_parts(appliance_type);
CREATE INDEX idx_spare_parts_sku            ON spare_parts(sku);
CREATE INDEX idx_spare_parts_is_active      ON spare_parts(is_active);
