CREATE TABLE IF NOT EXISTS job_status_history (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id       UUID        NOT NULL,
    from_status  VARCHAR(30),
    to_status    VARCHAR(30) NOT NULL,
    changed_by   UUID,
    reason       TEXT,
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_status_history_job_id ON job_status_history(job_id);
