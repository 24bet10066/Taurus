CREATE TABLE IF NOT EXISTS credit_transactions (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    technician_id   UUID          NOT NULL,
    type            VARCHAR(10)   NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    balance_after   DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(20),
    reference_id    UUID,
    notes           VARCHAR(500),
    recorded_by     VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_credit_transactions_technician_created
    ON credit_transactions(technician_id, created_at DESC);
