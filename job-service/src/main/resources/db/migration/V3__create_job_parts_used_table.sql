CREATE TABLE IF NOT EXISTS job_parts_used (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id     UUID         NOT NULL,
    part_id    UUID         NOT NULL,
    part_name  VARCHAR(200),
    quantity   INT          NOT NULL,
    unit_cost  DECIMAL(10,2) NOT NULL,
    source     VARCHAR(20)  NOT NULL DEFAULT 'SHOP',
    added_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_parts_used_job_id ON job_parts_used(job_id);
