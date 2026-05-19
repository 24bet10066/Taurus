CREATE TABLE customers (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(15)  NOT NULL UNIQUE,
    email           VARCHAR(100),
    address         TEXT,
    city            VARCHAR(50),
    pincode         VARCHAR(10),
    total_spent     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    job_count       INTEGER        NOT NULL DEFAULT 0,
    last_service_date TIMESTAMP WITH TIME ZONE,
    next_service_due  DATE,
    active          BOOLEAN        NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_phone  ON customers (phone);
CREATE INDEX idx_customers_active ON customers (active) WHERE active = true;
