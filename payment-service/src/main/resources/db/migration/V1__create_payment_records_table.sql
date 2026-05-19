CREATE TABLE payment_records (
    id                    UUID PRIMARY KEY,
    job_id                UUID         NOT NULL,
    amount                NUMERIC(10, 2) NOT NULL,
    payment_method        VARCHAR(20)  NOT NULL,
    razorpay_payment_id   VARCHAR(100),
    razorpay_order_id     VARCHAR(100),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    collected_by          UUID,
    customer_phone        VARCHAR(15),
    notes                 TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_records_job    ON payment_records (job_id);
CREATE INDEX idx_payment_records_status ON payment_records (status);
CREATE INDEX idx_payment_records_date   ON payment_records (created_at);
