CREATE TABLE IF NOT EXISTS jobs (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id       UUID         NOT NULL,
    customer_name     VARCHAR(200),
    customer_phone    VARCHAR(20),
    appliance_id      UUID,
    appliance_type    VARCHAR(30),
    brand             VARCHAR(100),
    source            VARCHAR(20)  NOT NULL,
    issue_description TEXT         NOT NULL,
    priority          VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',
    status            VARCHAR(30)  NOT NULL DEFAULT 'REQUESTED',
    assigned_at       TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    cancelled_at      TIMESTAMPTZ,
    estimated_charge  DECIMAL(10,2),
    actual_charge     DECIMAL(10,2),
    labor_charge      DECIMAL(10,2),
    parts_charge      DECIMAL(10,2),
    payment_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_method    VARCHAR(20),
    payment_ref       VARCHAR(100),
    area              VARCHAR(100),
    customer_notes    TEXT,
    technician_notes  TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_jobs_status      ON jobs(status);
CREATE INDEX idx_jobs_customer_id ON jobs(customer_id);
CREATE INDEX idx_jobs_created_at  ON jobs(created_at DESC);
