CREATE TABLE technicians (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(100) NOT NULL,
    phone                 VARCHAR(15)  NOT NULL UNIQUE,
    email                 VARCHAR(100),
    type                  VARCHAR(20)  NOT NULL,          -- HIRED | FREELANCE
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | INACTIVE | SUSPENDED
    skills                TEXT         NOT NULL DEFAULT '[]',     -- JSON array of ApplianceType strings
    city                  VARCHAR(50),
    pincode               VARCHAR(10),
    active_jobs           INTEGER      NOT NULL DEFAULT 0,
    total_jobs_completed  INTEGER      NOT NULL DEFAULT 0,
    total_parts_purchased NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_parts_paid      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    parts_order_count     INTEGER      NOT NULL DEFAULT 0,
    trust_score           NUMERIC(5, 4) NOT NULL DEFAULT 0.5000,
    payment_reliability   NUMERIC(5, 4) NOT NULL DEFAULT 0.5000,
    order_frequency       NUMERIC(5, 4) NOT NULL DEFAULT 0.5000,
    tenure_score          NUMERIC(5, 4) NOT NULL DEFAULT 0.5000,
    volume_score          NUMERIC(5, 4) NOT NULL DEFAULT 0.5000,
    credit_limit          NUMERIC(10, 2) NOT NULL DEFAULT 500.00,
    last_trust_computed   TIMESTAMP WITH TIME ZONE,
    approved              BOOLEAN NOT NULL DEFAULT false,
    active                BOOLEAN NOT NULL DEFAULT true,
    onboarded_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_technicians_type   ON technicians (type);
CREATE INDEX idx_technicians_status ON technicians (status) WHERE active = true;
CREATE INDEX idx_technicians_phone  ON technicians (phone);
