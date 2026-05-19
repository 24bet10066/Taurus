-- Fix: new technicians get credit_limit = 0 (must be explicitly enabled by admin)
ALTER TABLE technician_credit ALTER COLUMN credit_limit SET DEFAULT 0;

-- Extend type column to support LIMIT_CHANGE (was VARCHAR(10), LIMIT_CHANGE = 12 chars)
ALTER TABLE credit_transactions ALTER COLUMN type TYPE VARCHAR(20);

-- Add overdue tracking columns
ALTER TABLE technician_credit
    ADD COLUMN IF NOT EXISTS is_overdue    BOOLEAN    NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS overdue_since TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_technician_credit_overdue
    ON technician_credit (is_overdue) WHERE is_overdue = TRUE;
