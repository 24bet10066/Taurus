CREATE TABLE daily_summary (
    summary_date    DATE         PRIMARY KEY,
    total_revenue   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_jobs      INTEGER        NOT NULL DEFAULT 0,
    cash_revenue    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    online_revenue  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    parts_revenue   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    labor_revenue   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
