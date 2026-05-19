CREATE TABLE IF NOT EXISTS technician_credit (
    technician_id     UUID          PRIMARY KEY,
    technician_name   VARCHAR(255)  NOT NULL,
    technician_phone  VARCHAR(15),
    credit_limit      DECIMAL(10,2) NOT NULL DEFAULT 1000,
    current_balance   DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_purchased   DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_paid        DECIMAL(10,2) NOT NULL DEFAULT 0,
    last_purchase_at  TIMESTAMPTZ,
    last_payment_at   TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ
);
