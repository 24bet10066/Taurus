CREATE TABLE IF NOT EXISTS parts_sales (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    technician_id    UUID          NOT NULL,
    technician_name  VARCHAR(255),
    items            JSONB         NOT NULL,
    total_amount     DECIMAL(10,2) NOT NULL,
    payment_method   VARCHAR(20)   NOT NULL,
    credit_used      DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parts_sales_technician_id ON parts_sales(technician_id);
CREATE INDEX idx_parts_sales_created_at    ON parts_sales(created_at DESC);
