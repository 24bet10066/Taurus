CREATE TABLE IF NOT EXISTS job_technicians (
    job_id         UUID        NOT NULL,
    technician_id  UUID        NOT NULL,
    role           VARCHAR(20) NOT NULL,
    assigned_at    TIMESTAMPTZ,
    PRIMARY KEY (job_id, technician_id)
);

CREATE INDEX idx_job_technicians_technician_id ON job_technicians(technician_id);
