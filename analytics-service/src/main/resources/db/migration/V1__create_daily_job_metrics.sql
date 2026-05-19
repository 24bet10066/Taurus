CREATE TABLE IF NOT EXISTS daily_job_metrics (
    metric_date    DATE        NOT NULL,
    appliance_type VARCHAR(50) NOT NULL,
    area           VARCHAR(100) NOT NULL,
    total_jobs     INT         NOT NULL DEFAULT 0,
    completed_jobs INT         NOT NULL DEFAULT 0,
    cancelled_jobs INT         NOT NULL DEFAULT 0,
    total_revenue  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    avg_job_value  DECIMAL(10, 2),
    PRIMARY KEY (metric_date, appliance_type, area)
);

CREATE INDEX idx_daily_metrics_date ON daily_job_metrics (metric_date DESC);
