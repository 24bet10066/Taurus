CREATE TABLE IF NOT EXISTS technician_performance (
    technician_id  UUID NOT NULL,
    metric_month   DATE NOT NULL,   -- first day of the month
    total_jobs     INT  NOT NULL DEFAULT 0,
    completed_jobs INT  NOT NULL DEFAULT 0,
    total_revenue  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    avg_job_minutes INT,
    PRIMARY KEY (technician_id, metric_month)
);

CREATE INDEX idx_tech_perf_month ON technician_performance (metric_month DESC);
