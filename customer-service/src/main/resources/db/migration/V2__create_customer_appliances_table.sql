CREATE TABLE customer_appliances (
    id              UUID PRIMARY KEY,
    customer_id     UUID         NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    appliance_type  VARCHAR(30)  NOT NULL,
    brand           VARCHAR(50),
    model           VARCHAR(100),
    serial_number   VARCHAR(100),
    purchase_date   DATE,
    amc_start_date  DATE,
    amc_end_date    DATE,
    next_service_due DATE,
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appliances_customer     ON customer_appliances (customer_id);
CREATE INDEX idx_appliances_amc_end      ON customer_appliances (amc_end_date)    WHERE amc_end_date IS NOT NULL;
CREATE INDEX idx_appliances_service_due  ON customer_appliances (next_service_due) WHERE next_service_due IS NOT NULL;
